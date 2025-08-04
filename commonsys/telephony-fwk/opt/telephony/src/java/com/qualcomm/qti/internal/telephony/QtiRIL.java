/*
 * Copyright (c) 2017, 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.internal.telephony;

import static android.telephony.TelephonyManager.HAL_SERVICE_NETWORK;
import static android.telephony.TelephonyManager.HAL_SERVICE_RADIO;

import static com.android.internal.telephony.RILConstants.*;
import static com.android.internal.util.Preconditions.checkNotNull;

import android.annotation.NonNull;
import android.content.Context;
import android.hardware.radio.V1_0.CdmaSmsMessage;
import android.hardware.radio.V1_0.IRadio;
import android.hardware.radio.V1_0.RadioError;
import android.hardware.radio.V1_0.RadioResponseInfo;
import android.hardware.radio.V1_0.SendSmsResult;
import android.hidl.manager.V1_0.IServiceManager;
import android.hidl.manager.V1_0.IServiceNotification;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.HwBinder;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.WorkSource;
import android.provider.Settings;
import android.telephony.NetworkScanRequest;
import android.telephony.Rlog;

import com.android.internal.telephony.RIL;
import com.android.internal.telephony.RILRequest;
import com.android.internal.telephony.RILUtils;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.flags.FeatureFlags;
import com.android.internal.telephony.NetworkScanResult;
import com.android.internal.telephony.OperatorInfo;
import com.android.internal.telephony.SmsResponse;
import com.android.internal.telephony.nano.TelephonyProto.SmsSession;
import android.telephony.ImsiEncryptionInfo;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;

import com.qualcomm.qti.internal.telephony.QtiCarrierInfoManager.QtiCarrierInfoResponse;
import org.codeaurora.telephony.utils.EnhancedRadioCapabilityResponse;
import org.codeaurora.internal.NrConfig;
import org.codeaurora.telephony.utils.RILConstants;
import vendor.qti.hardware.radio.qtiradio.V1_0.IQtiRadio;
import vendor.qti.hardware.radio.qtiradio.V1_0.QtiRadioResponseInfo;
import vendor.qti.hardware.radio.qtiradio.V1_0.QtiRadioError;
import vendor.qti.hardware.radio.qtiradio.V2_0.IQtiRadioResponse;
import vendor.qti.hardware.radio.qtiradio.V2_0.IQtiRadioIndication;
import com.qualcomm.qti.internal.telephony.QtiCarrierInfoManager.QtiCarrierInfoResponse;

/**
 * QtiRIL extends RIL.
 *
 * {@hide}
 */
public final class QtiRIL extends RIL {
    static final String TAG = "QTIRILJ";
    static final String[] QTI_HIDL_SERVICE_NAME = {"slot1", "slot2", "slot3"};
    QtiRadioResponse mQtiRadioResponse;
    QtiRadioIndication mQtiRadioIndication;

    vendor.qti.hardware.radio.qtiradio.V2_5.IQtiRadioResponse.Stub mClientRadioResponseCb;
    vendor.qti.hardware.radio.qtiradio.V2_2.IQtiRadioIndication.Stub mClientRadioIndicationCb;

    private IQtiRadio mQtiRadio;
    int mQtiPhoneId = 0;
    final QtiRadioProxyDeathRecipient mDeathRecipient;
    final AtomicLong mQtiRadioProxyCookie = new AtomicLong(0);
    private QtiCarrierInfoResponse mQtiCarrierInfoResponse;
    private static final String PROPERTY_CAG_SNPN = "persist.vendor.cag_snpn";
    private static final String NETWORK_ACCESS_MODE = "access_mode";
    private int mCagSnpnFeatureStatus = -1;
    private static final int ACCESS_MODE_PLMN = 1;

    // Notification object used to listen to the start of the IQtiRadio
    private final QtiRadioServiceNotification mServiceNotification =
        new QtiRadioServiceNotification();

    final class QtiRadioProxyDeathRecipient implements HwBinder.DeathRecipient {
        @Override
        public void serviceDied(long cookie) {
            // Deal with service going away
            Rlog.d(TAG, "serviceDied");
            resetServiceAndRequestList();
        }
    }

    private void resetServiceAndRequestList() {
        resetProxyAndRequestList();
        mQtiRadio = null;
        mQtiRadioResponse = null;
        mQtiRadioIndication = null;
        mQtiRadioProxyCookie.incrementAndGet();
    }

    public void notifyNetworkScanResult(NetworkScanResult nsr){
      mRilNetworkScanResultRegistrants.notifyRegistrants(
                    new AsyncResult(null, nsr, null));
    }

    /**
     * Returns true if we are connected to IQtiRadio service, false otherwise.
     */
    private boolean isQtiRadioServiceConnected() {
        return (mQtiRadio != null);
    }

    /**
     * Class that implements the service notification which gets called once the
     * service with fully qualified name fqName has started
     */
    final class QtiRadioServiceNotification extends IServiceNotification.Stub {
        /**
         * Callback that gets called when the service has registered
         * @param fqName - Fully qualified name of the service
         * @param name - Name of the service
         * @param preexisting - if the registration is preexisting
         */
        @Override
        public void onRegistration(String fqName, String name, boolean preexisting) {
            Rlog.d(TAG, "QtiRadio interface service started " + fqName + " " + name +
                " preexisting =" + preexisting);
            if (!isQtiRadioServiceConnected()) {
                initQtiRadio();
            }
        }
    }

    /**
     * Register for notification when the com.qualcomm.qti.qtiradio@1.0::IQtiRadio is registered
     */
    private void registerForQtiRadioServiceNotification() {
        try {
            final boolean ret = IServiceManager.getService()
                    .registerForNotifications("vendor.qti.hardware.radio.qtiradio@1.0::IQtiRadio",
                    QTI_HIDL_SERVICE_NAME[mQtiPhoneId], mServiceNotification);
            if (!ret) {
                Rlog.e(TAG, "Failed to register for service start notifications");
            }
        } catch (RemoteException ex) {
            Rlog.e(TAG, "Failed to register for service start notifications. Exception " + ex);
        }
    }

    /**
     * Initialize the instance of IQtiRadio. Get the service and register the callback object
     * to be called for the responses to the solicited and unsolicited requests.
     */
    private synchronized void initQtiRadio() {
        try {
            mQtiRadio = IQtiRadio.getService(QTI_HIDL_SERVICE_NAME[mQtiPhoneId]);
            if (mQtiRadio == null) {
                Rlog.e(TAG, "initQtiRadio: mQtiRadio is null. Return");
                return;
            }
            Rlog.d(TAG, "initQtiRadio: mQtiRadio" + mQtiRadio);
            mQtiRadio.linkToDeath(mDeathRecipient,
                    mQtiRadioProxyCookie.incrementAndGet());
            mQtiRadioResponse = new QtiRadioResponse(this);
            mQtiRadioIndication = new QtiRadioIndication(mQtiPhoneId);
        } catch (Exception ex) {
            Rlog.e(TAG, "initQtiRadio: Exception: " + ex);
            resetServiceAndRequestList();
        }
    }
    //***** Constructors

    public QtiRIL(Context context, int preferredNetworkType, int cdmaSubscription,
            @NonNull FeatureFlags flags) {
        this(context, preferredNetworkType, cdmaSubscription, null, flags);
    }

    public QtiRIL(Context context, int preferredNetworkType, int cdmaSubscription,
            Integer instanceId, @NonNull FeatureFlags flags) {
        super(context, preferredNetworkType, cdmaSubscription, instanceId, flags);
        mQtiPhoneId = instanceId;
        Rlog.d(TAG, "QtiRIL");
        mDeathRecipient = new QtiRadioProxyDeathRecipient();
        registerForQtiRadioServiceNotification();
    }

    /** Returns a IQtiRadio instance or null if the service is not available. */
    public IQtiRadio getQtiRadioProxy(Message result) {
        if (!mIsCellularSupported) {
            Rlog.d(TAG, "getQtiRadioProxy: Not calling getService(): wifi-only");
            if (result != null) {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(RADIO_NOT_AVAILABLE));
                result.sendToTarget();
            }
            return null;
        }

        if (mQtiRadio == null) {

            Rlog.d(TAG, "getQtiRadioProxy: mRadioProxy == null");
            if (result != null) {
                AsyncResult.forMessage(result, null,
                        CommandException.fromRilErrno(RADIO_NOT_AVAILABLE));
                result.sendToTarget();
            }
        }

        return mQtiRadio;
    }

    public void setImsiEncryptionResponseCallback(QtiCarrierInfoResponse qtiCarrierInfo) {
        mQtiCarrierInfoResponse = qtiCarrierInfo;
    }

    public void enable5g(int serial) throws RemoteException {
        vendor.qti.hardware.radio.qtiradio.V2_0.IQtiRadio radioProxy2_0 = vendor.qti.hardware
                .radio.qtiradio.V2_0.IQtiRadio.castFrom(mQtiRadio);
        if (radioProxy2_0 != null) {
            radioProxy2_0.enable5g(serial);
        } else {
            throw new RemoteException("API not available!");
        }
    }

    public void disable5g(int serial) throws RemoteException {
        vendor.qti.hardware.radio.qtiradio.V2_0.IQtiRadio radioProxy2_0 = vendor.qti.hardware
                .radio.qtiradio.V2_0.IQtiRadio.castFrom(mQtiRadio);
        if (radioProxy2_0 != null) {
            radioProxy2_0.disable5g(serial);
        } else {
            throw new RemoteException("API not available!");
        }
    }

    public void enable5gOnly(int serial) throws RemoteException {
        vendor.qti.hardware.radio.qtiradio.V2_0.IQtiRadio radioProxy2_0 = vendor.qti.hardware
                .radio.qtiradio.V2_0.IQtiRadio.castFrom(mQtiRadio);
        if (radioProxy2_0 != null) {
            radioProxy2_0.enable5gOnly(serial);
        } else {
            throw new RemoteException("API not available!");
        }
    }

    public void query5gStatus(int serial) throws RemoteException {
        vendor.qti.hardware.radio.qtiradio.V2_0.IQtiRadio radioProxy2_0 = vendor.qti.hardware
                .radio.qtiradio.V2_0.IQtiRadio.castFrom(mQtiRadio);
        if (radioProxy2_0 != null) {
            radioProxy2_0.query5gStatus(serial);
        } else {
            throw new RemoteException("API not available!");
        }
    }

    public void queryNrDcParam(int serial) throws RemoteException {
        vendor.qti.hardware.radio.qtiradio.V2_0.IQtiRadio radioProxy2_0 = vendor.qti.hardware
                .radio.qtiradio.V2_0.IQtiRadio.castFrom(mQtiRadio);
        if (radioProxy2_0 != null) {
            radioProxy2_0.queryNrDcParam(serial);
        } else {
            throw new RemoteException("API not available!");
        }
    }

    public void queryNrBearerAllocation(int serial) throws RemoteException {
        vendor.qti.hardware.radio.qtiradio.V2_0.IQtiRadio radioProxy2_0 = vendor.qti.hardware
                .radio.qtiradio.V2_0.IQtiRadio.castFrom(mQtiRadio);
        if (radioProxy2_0 != null) {
            radioProxy2_0.queryNrBearerAllocation(serial);
        } else {
            throw new RemoteException("API not available!");
        }
    }

    public void queryNrSignalStrength(int serial) throws RemoteException {
        vendor.qti.hardware.radio.qtiradio.V2_0.IQtiRadio radioProxy2_0 = vendor.qti.hardware
                .radio.qtiradio.V2_0.IQtiRadio.castFrom(mQtiRadio);
        if (radioProxy2_0 != null) {
            radioProxy2_0.queryNrSignalStrength(serial);
        } else {
            throw new RemoteException("API not available!");
        }
    }

    public void queryUpperLayerIndInfo(int serial) throws RemoteException {
        vendor.qti.hardware.radio.qtiradio.V2_1.IQtiRadio radioProxy2_1 = vendor.qti.hardware
                .radio.qtiradio.V2_1.IQtiRadio.castFrom(mQtiRadio);
        if (radioProxy2_1 != null) {
            radioProxy2_1.queryUpperLayerIndInfo(serial);
        } else {
            throw new RemoteException("API not available!");
        }
    }


    public void queryNrIconType(int serial) throws RemoteException {
        vendor.qti.hardware.radio.qtiradio.V2_2.IQtiRadio radioProxy2_2 = vendor.qti.hardware
                .radio.qtiradio.V2_2.IQtiRadio.castFrom(mQtiRadio);
        if (radioProxy2_2 != null) {
            radioProxy2_2.queryNrIconType(serial);
        } else {
            throw new RemoteException("API not available!");
        }
    }


    public void query5gConfigInfo(int serial) throws RemoteException {
        vendor.qti.hardware.radio.qtiradio.V2_1.IQtiRadio radioProxy2_1 = vendor.qti.hardware
                .radio.qtiradio.V2_1.IQtiRadio.castFrom(mQtiRadio);
        if (radioProxy2_1 != null) {
            radioProxy2_1.query5gConfigInfo(serial);
        } else {
            throw new RemoteException("API not available!");
        }
    }

    public void setNrConfig(int serial, NrConfig config) throws RemoteException {
        vendor.qti.hardware.radio.qtiradio.V2_5.IQtiRadio radioProxy2_5 = vendor.qti.hardware
                .radio.qtiradio.V2_5.IQtiRadio.castFrom(mQtiRadio);
        if (radioProxy2_5 != null) {
            Rlog.d(TAG,"setNrConfig: serial = " + serial + " config= " + config.get());
            radioProxy2_5.setNrConfig(serial, config.get());
        } else {
            throw new RemoteException("API not available!");
        }
    }

    public void queryNrConfig(int serial) throws RemoteException {
        vendor.qti.hardware.radio.qtiradio.V2_5.IQtiRadio radioProxy2_5 = vendor.qti.hardware
                .radio.qtiradio.V2_5.IQtiRadio.castFrom(mQtiRadio);
        if (radioProxy2_5 != null) {
            Rlog.d(TAG,"queryNrConfig: serial = " + serial);
            radioProxy2_5.queryNrConfig(serial);
        } else {
            throw new RemoteException("API not available!");
        }
    }

    public int getPropertyValueInt(String property, int def) throws RemoteException {
        int propVal = def;
        vendor.qti.hardware.radio.qtiradio.V2_3.IQtiRadio radioProxy2_3 = vendor.qti.hardware
                .radio.qtiradio.V2_3.IQtiRadio.castFrom(mQtiRadio);
        if (radioProxy2_3 != null) {
            try {
                propVal = radioProxy2_3.getPropertyValueInt(property, def);
            } catch (RemoteException ex) {
                throw new RemoteException("API Error");
            }
        } else {
            Rlog.e(TAG, "getPropertyValueInt HAL API not available");
            propVal = SystemProperties.getInt(property, def);
        }

        return propVal;
    }

    public boolean getPropertyValueBool(String property, boolean def) throws RemoteException {
        boolean propVal = def;
        vendor.qti.hardware.radio.qtiradio.V2_3.IQtiRadio radioProxy2_3 = vendor.qti.hardware
                .radio.qtiradio.V2_3.IQtiRadio.castFrom(mQtiRadio);
        if (radioProxy2_3 != null) {
            try {
                propVal = radioProxy2_3.getPropertyValueBool(property, def);
            } catch (RemoteException ex) {
                throw new RemoteException("API Error");
            }
        } else {
            Rlog.e(TAG, "getPropertyValueBool HAL API not available");
            propVal = SystemProperties.getBoolean(property, def);
        }

        return propVal;
    }

    public String getPropertyValueString(String property, String def) throws RemoteException {
        String propVal = def;
        vendor.qti.hardware.radio.qtiradio.V2_3.IQtiRadio radioProxy2_3 = vendor.qti.hardware
                .radio.qtiradio.V2_3.IQtiRadio.castFrom(mQtiRadio);
        if (radioProxy2_3 != null) {
            try {
                propVal = radioProxy2_3.getPropertyValueString(property, def);
            } catch (RemoteException ex) {
                throw new RemoteException("API Error");
            }
        } else {
            Rlog.e(TAG, "getPropertyValueString HAL API not available");
            propVal = SystemProperties.get(property, def);
        }

        return propVal;
    }

    public void enableEndc(int serial, boolean enable) throws RemoteException {
        vendor.qti.hardware.radio.qtiradio.V2_3.IQtiRadio radioProxy2_3 = vendor.qti.hardware
                .radio.qtiradio.V2_3.IQtiRadio.castFrom(mQtiRadio);
        if (radioProxy2_3 != null) {
            radioProxy2_3.enableEndc(serial, enable);
        } else {
            throw new RemoteException("API not available!");
        }
    }

    public void queryEndcStatus(int serial) throws RemoteException {
        vendor.qti.hardware.radio.qtiradio.V2_3.IQtiRadio radioProxy2_3 = vendor.qti.hardware
                .radio.qtiradio.V2_3.IQtiRadio.castFrom(mQtiRadio);
        if (radioProxy2_3 != null) {
            radioProxy2_3.queryEndcStatus(serial);
        } else {
            throw new RemoteException("API not available!");
        }
    }

    public void setCarrierInfoForImsiEncryption(int serial, ImsiEncryptionInfo imsiEncryptionInfo) {
        Rlog.d(TAG, "setCarrierInfoForImsiEncryption,  serial=" +serial);
        QtiPhoneUtils.getInstance().setCarrierInfoForImsiEncryption(mQtiPhoneId,
                imsiEncryptionInfo, mQtiCarrierInfoResponse);
    }

    public void startNetworkScan(NetworkScanRequest networkScanRequest, Message result) {
        Rlog.d(TAG, "startNetworkScan");
        if(isCagSnpnEnabled()) {
            QtiPhoneUtils.getInstance().startNetworkScan(mQtiPhoneId, networkScanRequest,
                    result);
        } else {
            super.startNetworkScan(networkScanRequest,
                    result);
        }
    }

    public void stopNetworkScan(Message result) {
        Rlog.d(TAG, "stopNetworkScan");
        if(isCagSnpnEnabled()) {
            QtiPhoneUtils.getInstance().stopNetworkScan(mQtiPhoneId, result);
        } else {
            super.stopNetworkScan(result);
        }
    }

    public void getEnhancedRadioCapability(Message response) {
        Rlog.d(TAG, "getEnhancedRadioCapability, response=" + response);
        boolean exception = false;
        boolean ret = false;
        try {
            ret = QtiPhoneUtils.getInstance().getQtiRadioCapability(mQtiPhoneId, response);
        } catch (RuntimeException | RemoteException e) {
            Rlog.d(TAG, "getEnhancedRadioCapability, exception");
            e.printStackTrace();
            exception = true;
        } finally {
            if (exception || !ret) {
                Rlog.d(TAG, "getEnhancedRadioCapability request failed");
                if (response != null) {
                    AsyncResult.forMessage(response, null,
                                        CommandException.fromRilErrno(RADIO_NOT_AVAILABLE));
                    response.sendToTarget();
                }
            }
        }
    }

    private String convertNullToEmptyString(String string) {
        return string != null ? string : "";
    }

    private String addRanToOperatorNumeric(String operatorNumeric, int ran) {
        if (operatorNumeric == null) return "";
        return operatorNumeric + "+" + ran;
    }

    @Override
    public void setNetworkSelectionModeManual(String operatorNumeric, int ran, Message result) {
        if (getHalVersion(HAL_SERVICE_RADIO).greaterOrEqual(RADIO_HAL_VERSION_1_5)) {
            // For IRadio HAL >= 1.5, invoke the superclass method.
            super.setNetworkSelectionModeManual(operatorNumeric, ran, result);
        } else {
            // For IRadio HAL < 1.5, append the RAN to the operator numeric to provide
            // PLMN+RAN support for legacy targets.
            IRadio radioProxy = getRadioProxy();
            if (radioProxy != null) {
                RILRequest rr = obtainRequest(RIL_REQUEST_SET_NETWORK_SELECTION_MANUAL, result,
                        mRILDefaultWorkSource);
                try {
                    int halRan = RILUtils.convertToHalRadioAccessNetworks(ran);
                    String operatorNumericWithRan =
                            addRanToOperatorNumeric(operatorNumeric, halRan);
                    Rlog.d(TAG, rr.serialString() + "> " + RILUtils.requestToString(rr.getRequest())
                            + " operatorNumeric = " + operatorNumericWithRan
                            + (" [PHONE" + mQtiPhoneId + "]"));
                    radioProxy.setNetworkSelectionModeManual(rr.getSerial(),
                            operatorNumericWithRan);
                } catch (RemoteException | RuntimeException e) {
                    handleRadioProxyExceptionForRR(HAL_SERVICE_NETWORK,
                            "setNetworkSelectionModeManual", e);
                }
            }
        }
    }

    public void setNetworkSelectionModeManual(OperatorInfo network, Message result) {
        Rlog.d(TAG, "setNetworkSelectionModeManual");
        if(isCagSnpnEnabled()) {
            QtiPhoneUtils.getInstance().setNetworkSelectionModeManual(mQtiPhoneId, network, result);
        } else {
            setNetworkSelectionModeManual(network.getOperatorNumeric(), network.getRan(), result);
        }
    }

    public void setNetworkSelectionModeAutomatic(Message result) {
        Rlog.d(TAG, "setNetworkSelectionModeAutomatic");
        if(isCagSnpnEnabled()) {
            QtiPhoneUtils.getInstance().setNetworkSelectionModeAutomatic(mQtiPhoneId,
                    getAccessMode(), result);
        } else {
            super.setNetworkSelectionModeAutomatic(result);
        }
    }

    @Override
    public void sendCdmaSms(byte[] pdu, Message result) {
        // If HAL version 1.5 is supported use the functionality from super class
        if (canToggleUiccApplicationsEnablement()) {
            super.sendCdmaSms(pdu, result);
            return;
        }

        sendCdmaSms(pdu, result, false);
    }

    @Override
    public void sendCdmaSMSExpectMore(byte[] pdu, Message result) {
        // If HAL version 1.5 is supported use the functionality from super class
        if (canToggleUiccApplicationsEnablement()) {
            super.sendCdmaSMSExpectMore(pdu, result);
            return;
        }
        sendCdmaSms(pdu, result, true);
    }

    private void sendCdmaSms(byte[] pdu, Message result, boolean expectMore) {
        Rlog.d(TAG, "sendCdmaSms,  expectMore=" +expectMore);

        boolean ret = QtiPhoneUtils.getInstance().sendCdmaSms(mQtiPhoneId, pdu, result, expectMore);
        if (ret == false) {
            Rlog.d(TAG, "fall back to default sendCdmaSms");
            super.sendCdmaSms(pdu, result);
        }
    }

    /* package */
    Message qtiGetMessageFromRequest (Object request) {
        return getMessageFromRequest (request);
    }

    /* package */
    Object qtiProcessResponse(RadioResponseInfo responseInfo) {
        return processResponse(responseInfo);
    }

    void qtiProcessResponseDone(Object ret, RadioResponseInfo responseInfo, Object str) {
        processResponseDone(ret, responseInfo, str);
    }

    public void setCallbacks(vendor.qti.hardware.radio.qtiradio.V2_5.IQtiRadioResponse.Stub
            qtiRadioResponse,
            vendor.qti.hardware.radio.qtiradio.V2_2.IQtiRadioIndication.Stub
            qtiRadioIndication) {
        mClientRadioResponseCb = qtiRadioResponse;
        mClientRadioIndicationCb = qtiRadioIndication;
    }

    public boolean isCagSnpnEnabled() {
        if (mCagSnpnFeatureStatus == -1) {
            mCagSnpnFeatureStatus = QtiPhoneUtils.getInstance().getPropertyValueBool(
                    PROPERTY_CAG_SNPN, false) ? 1 : 0;
            Rlog.d(TAG, "isCagSnpnEnable, mCagSnpnFeatureStatus = " + mCagSnpnFeatureStatus);
        }
        return mCagSnpnFeatureStatus == 1 ? true : false;
    }

    public int getAccessMode() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                NETWORK_ACCESS_MODE + mQtiPhoneId, ACCESS_MODE_PLMN);
    }

    public class QtiRadioResponse extends
            vendor.qti.hardware.radio.qtiradio.V2_6.IQtiRadioResponse.Stub {

        QtiRIL mRil;
        static final String QTI_RILJ_LOG_TAG = "QtiRadioResponse";

        public QtiRadioResponse(QtiRIL ril) {
            mRil = ril;
        }

        /**
         * Helper function to send response msg
         * @param msg Response message to be sent
         * @param ret Return object to be included in the response message
         */
        void sendMessageResponse(Message msg, Object ret) {
            if (msg != null) {
                AsyncResult.forMessage(msg, ret, null);
                msg.sendToTarget();
            }
        }

        RadioResponseInfo toRadioResponseInfo (
                QtiRadioResponseInfo qtiResponseInfo) {
            RadioResponseInfo responseInfo = new RadioResponseInfo();
            responseInfo.type = qtiResponseInfo.type;
            responseInfo.serial = qtiResponseInfo.serial;
            responseInfo.error = qtiResponseInfo.error;
            return responseInfo;
        }

        private void responseString(RadioResponseInfo responseInfo, String str) {
            Object request = mRil.qtiProcessResponse(responseInfo);
            Message result = mRil.qtiGetMessageFromRequest(request);
            if (result != null) {
                if (responseInfo.error == QtiRadioError.NONE) {
                    sendMessageResponse(result, str);
                }
                mRil.qtiProcessResponseDone(request, responseInfo, str);
            }
        }

        /**
         * @param responseInfo Response info struct containing response type, serial no. and error
         * @param atr String containing the ATR
         */
        @Override
        public void getAtrResponse(QtiRadioResponseInfo qtiResponseInfo, String atr) {
            Rlog.d(QTI_RILJ_LOG_TAG, "getAtrResponse");
            RadioResponseInfo responseInfo= toRadioResponseInfo(
                    qtiResponseInfo);
            responseString(responseInfo, atr);
        }
        @Override
        public void onEnable5gResponse(int serial, int errorCode, int status) {
            Rlog.d(TAG,"onEnable5gResponse: serial = " + serial + " errorCode = " + errorCode +
                    " " + "status = " + status);
            if (mClientRadioResponseCb != null) {
                try {
                    mClientRadioResponseCb.onEnable5gResponse(serial, errorCode, status);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onDisable5gResponse(int serial, int errorCode, int status) {
            Rlog.d(TAG, "onDisable5gResponse: serial = " + serial + " errorCode = " + errorCode +
                    " " + "status = " + status);
            if (mClientRadioResponseCb != null) {
                try {
                    mClientRadioResponseCb.onDisable5gResponse(serial, errorCode, status);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onEnable5gOnlyResponse(int serial, int errorCode, int status) {
            Rlog.d(TAG, "onEnable5gOnlyResponse: serial = " + serial + " errorCode = " + errorCode +
                    "" + " " + "status = " + status);
            if (mClientRadioResponseCb != null) {
                try {
                    mClientRadioResponseCb.onEnable5gOnlyResponse(serial, errorCode, status);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void on5gStatusResponse(int serial, int errorCode, int enabled) {
            Rlog.d(TAG, "on5gStatusResponse: serial = " + serial + " errorCode = " + errorCode +
                    " " + "enabled = " + enabled);
            if (mClientRadioResponseCb != null) {
                try {
                    mClientRadioResponseCb.on5gStatusResponse(serial, errorCode, enabled);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onNrDcParamResponse(int serial, int errorCode,
                vendor.qti.hardware.radio.qtiradio.V2_0.DcParam dcParam) {
            Rlog.d(TAG, "onNrDcParamResponse: serial = " + serial + " errorCode = "
                    + errorCode + " dcParam = " + dcParam);
            if (mClientRadioResponseCb != null) {
                try {
                    mClientRadioResponseCb.onNrDcParamResponse(serial, errorCode, dcParam);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onNrBearerAllocationResponse_2_1(int serial, int errorCode, int bearerStatus) {
            Rlog.d(TAG, "onNrBearerAllocationResponse_2_1: serial = " + serial + " errorCode = " +
                    errorCode + " bearerStatus = " + bearerStatus);
            if (mClientRadioResponseCb != null) {
                try {
                    mClientRadioResponseCb.onNrBearerAllocationResponse_2_1(serial, errorCode,
                            bearerStatus);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onNrBearerAllocationResponse(int serial, int errorCode, int bearerStatus) {
            Rlog.d(TAG, "onNrBearerAllocationResponse: serial = " + serial + " errorCode = " +
                    errorCode + " bearerStatus = " + bearerStatus);
            if (mClientRadioResponseCb != null) {
                try {
                    mClientRadioResponseCb.onNrBearerAllocationResponse(serial, errorCode,
                            bearerStatus);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onUpperLayerIndInfoResponse(int serial, int errorCode,
                vendor.qti.hardware.radio.qtiradio.V2_1.UpperLayerIndInfo uliInfo){
            Rlog.d(TAG, "UpperLayerIndInfoResponse: serial = " + serial + " errorCode = "
                    + errorCode + " UpperLayerIndInfo = " + uliInfo);
            if (mClientRadioResponseCb != null) {
                try {
                    mClientRadioResponseCb.onUpperLayerIndInfoResponse(serial, errorCode,
                            uliInfo);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void on5gConfigInfoResponse(int serial, int errorCode, int confType){
            Rlog.d(TAG, "on5gConfigInfoResponse: serial = " + serial + " errorCode = "
                    + errorCode + " ConfigType = " + confType);
            if (mClientRadioResponseCb != null) {
                try {
                    mClientRadioResponseCb.on5gConfigInfoResponse(serial, errorCode,
                            confType);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onSignalStrengthResponse(int serial, int errorCode,
                vendor.qti.hardware.radio.qtiradio.V2_0.SignalStrength signalStrength) {
            Rlog.d(TAG, "onSignalStrengthResponse: serial = " + serial + " errorCode = " +
                    errorCode + " signalStrength = " + signalStrength);
            if (mClientRadioResponseCb != null) {
                try {
                    mClientRadioResponseCb.onSignalStrengthResponse(serial, errorCode,
                            signalStrength);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onNrIconTypeResponse(int serial, int errorCode, int iconType){
            Rlog.d(TAG, "onNrIconTypeResponse: serial = " + serial + " errorCode = "
                    + errorCode + " iconType = " + iconType);
            if (mClientRadioResponseCb != null) {
                try {
                    mClientRadioResponseCb.onNrIconTypeResponse(serial, errorCode,
                            iconType);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onEnableEndcResponse(int serial, int errorCode, int status) {
            Rlog.d(TAG,"onEnableEndcResponse: serial = " + serial + " errorCode = " + errorCode +
                    " " + "status = " + status);
            if (mClientRadioResponseCb != null) {
                try {
                    mClientRadioResponseCb.onEnableEndcResponse(serial, errorCode, status);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onEndcStatusResponse(int serial, int errorCode, int endcStatus) {
            Rlog.d(TAG,"onEndcStatusResponse: serial = " + serial + " errorCode = " + errorCode +
                    " " + "endcStatus = " + endcStatus);
            if (mClientRadioResponseCb != null) {
                try {
                    mClientRadioResponseCb.onEndcStatusResponse(serial, errorCode, endcStatus);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void setNrConfigResponse(int serial, int errorCode, int status) {
            Rlog.d(TAG,"setNrConfigResponse: serial = " + serial + " errorCode = " + errorCode +
                    " status = " + status);
            if (mClientRadioResponseCb != null) {
                try {
                    mClientRadioResponseCb.setNrConfigResponse(serial, errorCode, status);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onNrConfigResponse(int serial, int errorCode, int nrConfig) {
            Rlog.d(TAG, "onNrConfigResponse: serial = " + serial + " errorCode = " + errorCode +
                    " nrConfig = " + nrConfig);
            if (mClientRadioResponseCb != null) {
                try {
                    mClientRadioResponseCb.onNrConfigResponse(serial, errorCode, nrConfig);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void setCarrierInfoForImsiEncryptionResponse(QtiRadioResponseInfo info) {
            Rlog.d(TAG,"setCarrierInfoForImsiEncryptionResponse: serial = " + info.serial +
                    " errorCode = " + info.error);
        }

        private void responseSms(RadioResponseInfo responseInfo, SendSmsResult sms) {
            Object request = mRil.qtiProcessResponse(responseInfo);
            if (request != null) {
                Message result = mRil.qtiGetMessageFromRequest(request);
                SmsResponse ret = new SmsResponse(sms.messageRef, sms.ackPDU, sms.errorCode);
                if (responseInfo.error == RadioError.NONE) {
                    sendMessageResponse(result, ret);
                }
                mRil.qtiProcessResponseDone(request, responseInfo, ret);
            }
        }

        /**
         * @param responseInfo Response info struct containing response type, serial no. and error
         * @param sms Sms result struct as defined by SendSmsResult in types.hal
         */
        @Override
        public void sendCdmaSmsResponse(QtiRadioResponseInfo qtiResponseInfo, SendSmsResult sms) {
            Rlog.d(TAG, "sendCdmaSmsResponse");
            RadioResponseInfo responseInfo= toRadioResponseInfo(
                    qtiResponseInfo);
            Rlog.d(QTI_RILJ_LOG_TAG, "["+ qtiResponseInfo.serial+ "] < RIL_REQUEST_CDMA_SEND_SMS ");
            responseSms(responseInfo, sms);
        }

        private int convertToQtiNetworkTypeBitMask(int raf) {
            int networkTypeRaf = 0;

            if ((raf & vendor.qti.hardware.radio.qtiradio.V2_6.RadioAccessFamily.GSM) != 0) {
                networkTypeRaf |= EnhancedRadioCapabilityResponse.NETWORK_TYPE_BITMASK_GSM;
            }
            if ((raf & vendor.qti.hardware.radio.qtiradio.V2_6.RadioAccessFamily.GPRS) != 0) {
                networkTypeRaf |= EnhancedRadioCapabilityResponse.NETWORK_TYPE_BITMASK_GPRS;
            }
            if ((raf & vendor.qti.hardware.radio.qtiradio.V2_6.RadioAccessFamily.EDGE) != 0) {
                networkTypeRaf |= EnhancedRadioCapabilityResponse.NETWORK_TYPE_BITMASK_EDGE;
            }
            // convert both IS95A/IS95B to CDMA as network mode doesn't support CDMA
            if ((raf & vendor.qti.hardware.radio.qtiradio.V2_6.RadioAccessFamily.IS95A) != 0) {
                networkTypeRaf |= EnhancedRadioCapabilityResponse.NETWORK_TYPE_BITMASK_CDMA;
            }
            if ((raf & vendor.qti.hardware.radio.qtiradio.V2_6.RadioAccessFamily.IS95B) != 0) {
                networkTypeRaf |= EnhancedRadioCapabilityResponse.NETWORK_TYPE_BITMASK_CDMA;
            }
            if ((raf & vendor.qti.hardware.radio.qtiradio.V2_6.RadioAccessFamily.ONE_X_RTT) != 0) {
                networkTypeRaf |= EnhancedRadioCapabilityResponse.NETWORK_TYPE_BITMASK_1xRTT;
            }
            if ((raf & vendor.qti.hardware.radio.qtiradio.V2_6.RadioAccessFamily.EVDO_0) != 0) {
                networkTypeRaf |= EnhancedRadioCapabilityResponse.NETWORK_TYPE_BITMASK_EVDO_0;
            }
            if ((raf & vendor.qti.hardware.radio.qtiradio.V2_6.RadioAccessFamily.EVDO_A) != 0) {
                networkTypeRaf |= EnhancedRadioCapabilityResponse.NETWORK_TYPE_BITMASK_EVDO_A;
            }
            if ((raf & vendor.qti.hardware.radio.qtiradio.V2_6.RadioAccessFamily.EVDO_B) != 0) {
                networkTypeRaf |= EnhancedRadioCapabilityResponse.NETWORK_TYPE_BITMASK_EVDO_B;
            }
            if ((raf & vendor.qti.hardware.radio.qtiradio.V2_6.RadioAccessFamily.EHRPD) != 0) {
                networkTypeRaf |= EnhancedRadioCapabilityResponse.NETWORK_TYPE_BITMASK_EHRPD;
            }
            if ((raf & vendor.qti.hardware.radio.qtiradio.V2_6.RadioAccessFamily.HSUPA) != 0) {
                networkTypeRaf |= EnhancedRadioCapabilityResponse.NETWORK_TYPE_BITMASK_HSUPA;
            }
            if ((raf & vendor.qti.hardware.radio.qtiradio.V2_6.RadioAccessFamily.HSDPA) != 0) {
                networkTypeRaf |= EnhancedRadioCapabilityResponse.NETWORK_TYPE_BITMASK_HSDPA;
            }
            if ((raf & vendor.qti.hardware.radio.qtiradio.V2_6.RadioAccessFamily.HSPA) != 0) {
                networkTypeRaf |= EnhancedRadioCapabilityResponse.NETWORK_TYPE_BITMASK_HSPA;
            }
            if ((raf & vendor.qti.hardware.radio.qtiradio.V2_6.RadioAccessFamily.HSPAP) != 0) {
                networkTypeRaf |= EnhancedRadioCapabilityResponse.NETWORK_TYPE_BITMASK_HSPAP;
            }
            if ((raf & vendor.qti.hardware.radio.qtiradio.V2_6.RadioAccessFamily.UMTS) != 0) {
                networkTypeRaf |= EnhancedRadioCapabilityResponse.NETWORK_TYPE_BITMASK_UMTS;
            }
            if ((raf & vendor.qti.hardware.radio.qtiradio.V2_6.RadioAccessFamily.TD_SCDMA) != 0) {
                networkTypeRaf |= EnhancedRadioCapabilityResponse.NETWORK_TYPE_BITMASK_TD_SCDMA;
            }
            if ((raf & vendor.qti.hardware.radio.qtiradio.V2_6.RadioAccessFamily.LTE) != 0) {
                networkTypeRaf |= EnhancedRadioCapabilityResponse.NETWORK_TYPE_BITMASK_LTE;
            }
            if ((raf & vendor.qti.hardware.radio.qtiradio.V2_6.RadioAccessFamily.LTE_CA) != 0) {
                networkTypeRaf |= EnhancedRadioCapabilityResponse.NETWORK_TYPE_BITMASK_LTE_CA;
            }
            if ((raf & vendor.qti.hardware.radio.qtiradio.V2_6.RadioAccessFamily.NR_NSA) != 0) {
                networkTypeRaf |= EnhancedRadioCapabilityResponse.NETWORK_TYPE_BITMASK_NR_NSA;
            }
            if ((raf & vendor.qti.hardware.radio.qtiradio.V2_6.RadioAccessFamily.NR_SA) != 0) {
                networkTypeRaf |= EnhancedRadioCapabilityResponse.NETWORK_TYPE_BITMASK_NR_SA;
            }
            return (networkTypeRaf == 0) ? EnhancedRadioCapabilityResponse.
                    NETWORK_TYPE_UNKNOWN : networkTypeRaf;
        }

        /**
         * Response to IQtiRadio.getQtiRadioCapability
         * @param responseInfo Response info struct containing response type, serial no. and error
         * @param raf Radio Access Family 32-bit bitmap of
         * vendor.qti.hardware.radio.qtiradio.V2_6.RadioAccessFamily
         */
        @Override
        public void getQtiRadioCapabilityResponse(RadioResponseInfo responseInfo,int raf) {
            RILRequest rr = mRil.processResponse(responseInfo);
            Rlog.d(TAG, "getQtiRadioCapabilityResponse:" + raf);
            int ret = 0;
            if(rr != null) {
                ret = convertToQtiNetworkTypeBitMask(raf);
            }
            if (responseInfo.error == QtiRadioError.NONE) {
                sendMessageResponse(rr.getResult(), ret);
            }
            processResponseDone(rr, responseInfo, ret);
        }
    }

    public class QtiRadioIndication extends
            vendor.qti.hardware.radio.qtiradio.V2_2.IQtiRadioIndication.Stub {
        static final String TAG = "QtiRadioIndication";

        int mSlotId;

        public QtiRadioIndication(int slotId) {
            Rlog.d(TAG, "[" + slotId + "]Constructor: ");
            mSlotId = slotId;
        }

        @Override
        public void on5gStatusChange(int enableStatus) {
            Rlog.d(TAG, "on5gStatusChange: slotId = " + mSlotId);
            if (mClientRadioIndicationCb != null) {
                try {
                    mClientRadioIndicationCb.on5gStatusChange(enableStatus);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onNrDcParamChange(
                vendor.qti.hardware.radio.qtiradio.V2_0.DcParam dcParam) {
            Rlog.d(TAG, "onNrDcParamChange: slotId = " + mSlotId);
            if (mClientRadioIndicationCb != null) {
                try {
                    mClientRadioIndicationCb.onNrDcParamChange(dcParam);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onNrBearerAllocationChange_2_1(int bearerStatus) {
            Rlog.d(TAG, "onNrBearerAllocationChange_2_1: slotId = " + mSlotId);
            if (mClientRadioIndicationCb != null) {
                try {
                    mClientRadioIndicationCb.onNrBearerAllocationChange_2_1(bearerStatus);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onNrBearerAllocationChange(int bearerStatus) {
            Rlog.d(TAG, "onNrBearerAllocationChange: slotId = " + mSlotId);
            if (mClientRadioIndicationCb != null) {
                try {
                    mClientRadioIndicationCb.onNrBearerAllocationChange(bearerStatus);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onUpperLayerIndInfoChange(
                vendor.qti.hardware.radio.qtiradio.V2_1.UpperLayerIndInfo uliInfo){
            Rlog.d(TAG, "onUpperLayerIndInfoChange: UpperLayerIndInfo = " + uliInfo);
            if (mClientRadioIndicationCb != null) {
                try {
                    mClientRadioIndicationCb.onUpperLayerIndInfoChange(uliInfo);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void on5gConfigInfoChange(int confType){
            Rlog.d(TAG, "on5gConfigInfoChange: ConfigType = " + confType);
            if (mClientRadioIndicationCb != null) {
                try {
                    mClientRadioIndicationCb.on5gConfigInfoChange(confType);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onNrIconTypeChange(int iconType){
            Rlog.d(TAG, "onNrIconTypeChange: iconType = " + iconType);
            if (mClientRadioIndicationCb != null) {
                try {
                    mClientRadioIndicationCb.onNrIconTypeChange(iconType);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onSignalStrengthChange(
                vendor.qti.hardware.radio.qtiradio.V2_0.SignalStrength signalStrength) {
            Rlog.d(TAG, "onSignalStrengthChange: slotId = " + mSlotId);
            if (mClientRadioIndicationCb != null) {
                try {
                    mClientRadioIndicationCb.onSignalStrengthChange(signalStrength);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void qtiRadioIndication(int value) {
            Rlog.d(TAG, "qtiRadioIndication: NOP!!");
        }

    }
}
