 /*
  * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
  * All rights reserved.
  * Confidential and Proprietary - Qualcomm Technologies, Inc.
  *
  * Not a Contribution.
  *
  * Copyright (C) 2006 The Android Open Source Project
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  *
  */

package org.codeaurora.ims;

import android.hidl.manager.V1_0.IServiceManager;
import android.hidl.manager.V1_0.IServiceNotification;

import android.graphics.Point;
import android.location.Address;
import android.os.HwBinder;
import android.os.RemoteException;
import android.telephony.ims.ImsException;

import java.util.ArrayList;
import java.util.List;
import java.util.Collection;

import com.qualcomm.ims.utils.Log;

import vendor.qti.hardware.radio.ims.V1_0.*;
import vendor.qti.hardware.radio.ims.V1_2.ImsSmsMessage;
import vendor.qti.hardware.radio.ims.V1_4.MultiIdentityLineInfoHal;

/*
 * Class that sends requests to IImsRadio HIDL interface
 */

public class ImsRadioHidl implements IImsRadio {

    static final String LOG_TAG = "ImsRadioHidl";
    private IImsRadioResponse mResponse;
    private IImsRadioIndication mIndication;
    private Integer mPhoneId;
    // The death recepient object which gets notified when IImsRadio service dies.
    private ImsRadioDeathRecipient mDeathRecipient;

    private String mServiceName;

    // Notification object used to listen to the start of the IImsRadio
    private final ImsRadioServiceNotification mServiceNotification =
            new ImsRadioServiceNotification();

    // Synchronization object of HAL interfaces.
    private final Object mHalSync = new Object();

    /* Object of the type ImsRadioResponse which is registered with the IImsRadio
     * service for all callbacks to be routed back */
    private vendor.qti.hardware.radio.ims.V1_0.IImsRadioResponse mImsRadioResponse;

    /*Used by Sms over Ims functions*/
    private volatile vendor.qti.hardware.radio.ims.V1_2.IImsRadio mImsRadioV12;

    private volatile vendor.qti.hardware.radio.ims.V1_3.IImsRadio mImsRadioV13;
    private volatile vendor.qti.hardware.radio.ims.V1_4.IImsRadio mImsRadioV14;
    private volatile vendor.qti.hardware.radio.ims.V1_5.IImsRadio mImsRadioV15;
    private volatile vendor.qti.hardware.radio.ims.V1_6.IImsRadio mImsRadioV16;
    private volatile vendor.qti.hardware.radio.ims.V1_7.IImsRadio mImsRadioV17;
    private volatile vendor.qti.hardware.radio.ims.V1_8.IImsRadio mImsRadioV18;
    private volatile vendor.qti.hardware.radio.ims.V1_9.IImsRadio mImsRadioV19;

    /* Object of the type ImsRadioIndication which is registered with the IImsRadio
     * service for all unsolicited messages to be sent*/
    private vendor.qti.hardware.radio.ims.V1_0.IImsRadioIndication mImsRadioIndication;

    /*  Instance of the IImsRadio interface */
    private volatile vendor.qti.hardware.radio.ims.V1_0.IImsRadio mImsRadio;
    static final String IIMS_RADIO_SERVICE_NAME = "imsradio";

    private static final int STATUS_INTERROGATE = 2;

    private boolean mIsDisposed = false;

    public ImsRadioHidl(IImsRadioResponse response, IImsRadioIndication
            indication, int phoneId) {
        mPhoneId = phoneId;
        mResponse = response;
        mIndication = indication;
        mServiceName = IIMS_RADIO_SERVICE_NAME + mPhoneId;
        mDeathRecipient = new ImsRadioDeathRecipient();
        // register for ImsRadio service notification
        registerForImsRadioServiceNotification();
    }

    private void notifyServiceUp() {
        mIndication.onServiceUp();
    }

    private void notifyServiceDown() {
        mIndication.onServiceDown();
    }

    /**
     * Class that implements the binder death recipient to be notified when
     * IImsRadio service dies.
     */
    final class ImsRadioDeathRecipient implements HwBinder.DeathRecipient {
        /**
         * Callback that gets called when the service has died
         */
        @Override
        public void serviceDied(long cookie) {
            Log.e(this, " IImsRadio Died");
            resetServiceAndRequestList();
        }
    }

    private synchronized void resetServiceAndRequestList() {
        notifyServiceDown();
        resetHalInterfaces();
    }

    private void resetHalInterfaces() {
        Log.d(this, "resetHalInterfaces: Resetting HAL interfaces.");
        vendor.qti.hardware.radio.ims.V1_0.IImsRadio imsRadio = imsRadioV10();
        if (imsRadio != null) {
            try {
                imsRadio.unlinkToDeath(mDeathRecipient);
            } catch (Exception ex) {}
        }
        synchronized (mHalSync) {
            mImsRadio = null;
            mImsRadioV12 = null;
            mImsRadioV13 = null;
            mImsRadioV14 = null;
            mImsRadioV15 = null;
            mImsRadioV16 = null;
            mImsRadioV17 = null;
            mImsRadioV18 = null;
            mImsRadioV19 = null;
            mImsRadioResponse = null;
            mImsRadioIndication = null;

        }
    }

    /**
     * Class that implements the service notification which gets called once the
     * service with fully qualified name fqName has started
     */
    final class ImsRadioServiceNotification extends IServiceNotification.Stub {
        /**
         * Callback that gets called when the service has registered
         * @param fqName - Fully qualified name of the service
         * @param name - Name of the service
         * @param preexisting - if the registration is preexisting
         */
        @Override
        public void onRegistration(String fqName, String name, boolean preexisting) {
            Log.v(this, "Ims Radio interface service started " + fqName + " " + name +
                    " preexisting =" + preexisting);
            synchronized(mHalSync) {
                if (mIsDisposed) {
                    Log.i(this, "onRegistration: already disposed. Exit");
                    return;
                }
            }
            initImsRadio();
        }
    }

    /**
     * Register for notification when the
     * vendor.qti.hardware.radio.ims@1.0::IImsRadio is registered
     */
    private void registerForImsRadioServiceNotification() {
        try {
            final boolean ret = IServiceManager.getService()
                    .registerForNotifications("vendor.qti.hardware.radio.ims@1.0::IImsRadio",
                            mServiceName, mServiceNotification);
            if (!ret) {
                Log.e(this, "Failed to register for service start notifications");
            }
        } catch (RemoteException ex) {
            Log.e(this, "Failed to register for service start notifications. Exception " + ex);
        }
    }

    private vendor.qti.hardware.radio.ims.V1_0.IImsRadio imsRadioV10() {
        vendor.qti.hardware.radio.ims.V1_0.IImsRadio imsRadio = null;
        synchronized (mHalSync) {
            imsRadio = mImsRadio;
        }
        return imsRadio;
    }

    private vendor.qti.hardware.radio.ims.V1_2.IImsRadio imsRadioV12() {
        vendor.qti.hardware.radio.ims.V1_2.IImsRadio imsRadio = null;
        synchronized (mHalSync) {
            imsRadio = mImsRadioV12;
        }
        return imsRadio;
    }

    private vendor.qti.hardware.radio.ims.V1_3.IImsRadio imsRadioV13() {
        vendor.qti.hardware.radio.ims.V1_3.IImsRadio imsRadio = null;
        synchronized (mHalSync) {
            imsRadio = mImsRadioV13;
        }
        return imsRadio;
    }

    private vendor.qti.hardware.radio.ims.V1_4.IImsRadio imsRadioV14() {
        vendor.qti.hardware.radio.ims.V1_4.IImsRadio imsRadio = null;
        synchronized (mHalSync) {
            imsRadio = mImsRadioV14;
        }
        return imsRadio;
    }

    private vendor.qti.hardware.radio.ims.V1_5.IImsRadio imsRadioV15() {
        vendor.qti.hardware.radio.ims.V1_5.IImsRadio imsRadio = null;
        synchronized (mHalSync) {
            imsRadio = mImsRadioV15;
        }
        return imsRadio;
    }

    private vendor.qti.hardware.radio.ims.V1_6.IImsRadio imsRadioV16() {
        vendor.qti.hardware.radio.ims.V1_6.IImsRadio imsRadio = null;
        synchronized (mHalSync) {
            imsRadio = mImsRadioV16;
        }
        return imsRadio;
    }

    private vendor.qti.hardware.radio.ims.V1_7.IImsRadio imsRadioV17() {
        vendor.qti.hardware.radio.ims.V1_7.IImsRadio imsRadio = null;
        synchronized (mHalSync) {
            imsRadio = mImsRadioV17;
        }
        return imsRadio;
    }

    private vendor.qti.hardware.radio.ims.V1_8.IImsRadio imsRadioV18() {
        vendor.qti.hardware.radio.ims.V1_8.IImsRadio imsRadio = null;
        synchronized (mHalSync) {
            imsRadio = mImsRadioV18;
        }
        return imsRadio;
    }

    private vendor.qti.hardware.radio.ims.V1_9.IImsRadio imsRadioV19() {
        vendor.qti.hardware.radio.ims.V1_9.IImsRadio imsRadio = null;
        synchronized (mHalSync) {
            imsRadio = mImsRadioV19;
        }
        return imsRadio;
    }

    /**
     * Initialize the instance of IImsRadio. Get the service and register the callback object
     * to be called for the responses to the solicited and unsolicited requests.
     */
    private void initImsRadio() {
        try {
            vendor.qti.hardware.radio.ims.V1_0.IImsRadio imsRadio =
                    vendor.qti.hardware.radio.ims.V1_0.IImsRadio.getService(mServiceName);
            if (imsRadio == null) {
                resetHalInterfaces();
                Log.e(this, "initImsRadio: imsRadio is null.");
                return;
            }

            Log.i(this, "initImsRadio: imsRadioV10 availability: " +
                    toAvailability(imsRadio != null));

            vendor.qti.hardware.radio.ims.V1_1.IImsRadio imsRadioV11 =
                    vendor.qti.hardware.radio.ims.V1_1.IImsRadio.castFrom(imsRadio);
            Log.i(this, "initImsRadio: imsRadioV11 availability: " + toAvailability(imsRadioV11
                    != null));

            vendor.qti.hardware.radio.ims.V1_2.IImsRadio imsRadioV12 =
                    vendor.qti.hardware.radio.ims.V1_2.IImsRadio.castFrom(imsRadio);
            Log.i(this, "initImsRadio: imsRadioV12 availability: " + toAvailability(imsRadioV12
                    != null));

            vendor.qti.hardware.radio.ims.V1_3.IImsRadio imsRadioV13 =
                    vendor.qti.hardware.radio.ims.V1_3.IImsRadio.castFrom(imsRadio);
            Log.i(this, "initImsRadio: imsRadioV13 availability: " + toAvailability(imsRadioV13
                    != null));

            vendor.qti.hardware.radio.ims.V1_4.IImsRadio imsRadioV14 =
                    vendor.qti.hardware.radio.ims.V1_4.IImsRadio.castFrom(imsRadio);
            Log.i(this, "initImsRadio: imsRadioV14 availability: " + toAvailability(imsRadioV14
                    != null));

            vendor.qti.hardware.radio.ims.V1_5.IImsRadio imsRadioV15 =
                    vendor.qti.hardware.radio.ims.V1_5.IImsRadio.castFrom(imsRadio);
            Log.i(this, "initImsRadio: imsRadioV15 availability: " + toAvailability(imsRadioV15
                    != null));

            vendor.qti.hardware.radio.ims.V1_6.IImsRadio imsRadioV16 =
                    vendor.qti.hardware.radio.ims.V1_6.IImsRadio.castFrom(imsRadio);
            Log.i(this, "initImsRadio: imsRadioV16 availability: " + toAvailability(imsRadioV16
                    != null));

            vendor.qti.hardware.radio.ims.V1_7.IImsRadio imsRadioV17 =
                    vendor.qti.hardware.radio.ims.V1_7.IImsRadio.castFrom(imsRadio);
            Log.i(this, "initImsRadio: imsRadioV17 availability: " + toAvailability(imsRadioV17
                    != null));

            vendor.qti.hardware.radio.ims.V1_8.IImsRadio imsRadioV18 =
                    vendor.qti.hardware.radio.ims.V1_8.IImsRadio.castFrom(imsRadio);
            Log.i(this, "initImsRadio: imsRadioV18 availability: " + toAvailability(imsRadioV18
                    != null));

            vendor.qti.hardware.radio.ims.V1_9.IImsRadio imsRadioV19 =
                    vendor.qti.hardware.radio.ims.V1_9.IImsRadio.castFrom(imsRadio);
            Log.i(this, "initImsRadio: imsRadioV19 availability: " + toAvailability(imsRadioV19
                    != null));

            ImsRadioResponseHidl imsRadioResponse = new ImsRadioResponseHidl(mResponse);
            ImsRadioIndicationHidl imsRadioIndication = new ImsRadioIndicationHidl
                    (mIndication);

            synchronized (mHalSync) {
                mImsRadioResponse = imsRadioResponse;
                mImsRadioIndication = imsRadioIndication;
                imsRadio.setCallback(mImsRadioResponse, mImsRadioIndication);
                imsRadio.linkToDeath(mDeathRecipient, 0 /* Not Used */);
                mImsRadio = imsRadio;
                mImsRadioV12 = imsRadioV12;
                mImsRadioV13 = imsRadioV13;
                mImsRadioV14 = imsRadioV14;
                mImsRadioV15 = imsRadioV15;
                mImsRadioV16 = imsRadioV16;
                mImsRadioV17 = imsRadioV17;
                mImsRadioV18 = imsRadioV18;
                mImsRadioV19 = imsRadioV19;
            }
            notifyServiceUp();

        } catch (Exception ex) {
            Log.e(this, "initImsRadio: Exception: " + ex);
            resetServiceAndRequestList();
        }
    }

    // Implementation methods for IImsRadio java interface
    @Override
    public boolean isFeatureSupported(int feature) {
        switch (feature) {
            case Feature.SMS:
                return imsRadioV12() != null;
            case Feature.EMERGENCY_DIAL:
                return imsRadioV15() != null;
            case Feature.CONSOLIDATED_SET_SERVICE_STATUS:
            case Feature.CALL_COMPOSER_DIAL:
            case Feature.USSD:
                return imsRadioV16() != null;
            case Feature.CRS:
            case Feature.SIP_DTMF:
                return imsRadioV17() != null;
            case Feature.CONFERENCE_CALL_STATE_COMPLETED:
                return imsRadioV18() != null;
            case Feature.VIDEO_ONLINE_SERVICE:
                return imsRadioV19() != null;
            default:
                return false;
        }
    }

    public boolean isAlive() {
        return (imsRadioV10() != null);
    }

    @Override
    public void dispose() {
        synchronized (mHalSync) {
            mIsDisposed = true;
        }
        resetHalInterfaces();
    }

    @Override
    public void addParticipant(int token, String address, int clirMode,
                               CallDetails callDetails) throws RemoteException {
        vendor.qti.hardware.radio.ims.V1_4.DialRequest dialRequest =
                ImsRadioUtils.buildDialRequest(address,
                clirMode, callDetails, false /*isEncrypted*/);
        vendor.qti.hardware.radio.ims.V1_4.IImsRadio imsRadio = imsRadioV14();
        if (imsRadio != null) {
            imsRadio.addParticipant_1_4(token, dialRequest);
        } else {
            Log.w(this, "mImsRadio V1.4 is null. invoking mImsRadio.dial()");
            imsRadioV10().addParticipant(token, ImsRadioUtils.migrateFromDialRequestV14(
                    dialRequest));
        }
        Log.v(this, " addParticipant: token = " + token +
                " address = " + dialRequest.address + " callType = " +
                dialRequest.callDetails.callType + " callDomain = " +
                dialRequest.callDetails.callDomain + " isConferenceUri = " +
                dialRequest.isConferenceUri + "isCallPull =" + dialRequest.isCallPull +
                " isEncrypted = " + dialRequest.isEncrypted);
    }

    @Override
    public void dial(int token, String address, EmergencyCallInfo eInfo,
                     int clirMode, CallDetails callDetails, boolean isEncrypted,
                     CallComposerInfo ccInfo, RedialInfo redialInfo) throws RemoteException {
        vendor.qti.hardware.radio.ims.V1_4.DialRequest dialRequest =
                ImsRadioUtils.buildDialRequest(address, clirMode, callDetails, isEncrypted,
                redialInfo);
        if (imsRadioV16() != null) {
            vendor.qti.hardware.radio.ims.V1_6.DialRequest dialRequestV16 =
                    ImsRadioUtilsV16.buildDialRequest(
                    address, clirMode, callDetails, isEncrypted, redialInfo);
            if (ccInfo != null) {
                log("Dialing call composer call v1.6");
                vendor.qti.hardware.radio.ims.V1_6.CallComposerInfo composerInfo =
                        ImsRadioUtilsV16.buildCallComposerInfoHal(ccInfo);
                imsRadioV16().callComposerDial(token, dialRequestV16, composerInfo);
            } else if (eInfo != null) {
                int categories = ImsRadioUtilsV15.mapEmergencyServiceCategoryToHal(
                        eInfo.getEmergencyServiceCategories());
                int route = ImsRadioUtilsV15.mapEmergencyCallRoutingToHal(
                        eInfo.getEmergencyCallRouting());
                ArrayList<String> urns = new ArrayList<>(eInfo.getEmergencyUrns());
                log("emergencyDial v1.6");
                imsRadioV16().emergencyDial_1_6(token, dialRequestV16, categories,
                        urns, route, eInfo.hasKnownUserIntentEmergency(),
                        eInfo.isEmergencyCallTesting());
            } else {
                log("dial v1.6");
                imsRadioV16().dial_1_6(token, dialRequestV16);
            }
        } else if (eInfo != null && imsRadioV15() != null) {
            int categories = ImsRadioUtilsV15.mapEmergencyServiceCategoryToHal(
                    eInfo.getEmergencyServiceCategories());
            int route = ImsRadioUtilsV15.mapEmergencyCallRoutingToHal(
                    eInfo.getEmergencyCallRouting());
            ArrayList<String> urns = new ArrayList<>(eInfo.getEmergencyUrns());
            log("emergencyDial v1.5");
            imsRadioV15().emergencyDial(token, dialRequest, categories,
                    urns, route, eInfo.hasKnownUserIntentEmergency(),
                    eInfo.isEmergencyCallTesting());
        } else {
            vendor.qti.hardware.radio.ims.V1_4.IImsRadio imsRadio = imsRadioV14();
            if (imsRadio != null) {
                log("dial v1.4");
                imsRadio.dial_1_4(token, dialRequest);
            } else {
                log("dial v1.0");
                imsRadioV10().dial(token, ImsRadioUtils.migrateFromDialRequestV14(
                        dialRequest));
            }
        }
        Log.v(this, "dial: address = " + dialRequest.address + "callType =" +
                dialRequest.callDetails.callType + "callDomain =" +
                dialRequest.callDetails.callDomain + "isConferenceUri =" +
                dialRequest.isConferenceUri + "isCallPull =" + dialRequest.isCallPull +
                "isEncrypted =" + dialRequest.isEncrypted +
                "rttMode =" + dialRequest.callDetails.rttMode);
    }

    @Override
    public void sendUssd(int token, String address) throws RemoteException {
        imsRadioV16().sendUssd(token, address);
    }

    @Override
    public void cancelPendingUssd(int token) throws RemoteException {
        imsRadioV16().cancelPendingUssd(token);
    }

    @Override
    public void answer(int token, int callType, int ipPresentation, int rttMode)
            throws RemoteException {
        imsRadioV10().answer(token, ImsRadioUtils.callTypeToHal(callType),
                ImsRadioUtilsV15.mapTirPresentationToIpPresentation(ipPresentation),
                ImsRadioUtils.rttModeToHal(rttMode));
    }

    @Override
    public void deflectCall(int token, int index, String number) throws RemoteException {
        DeflectRequestInfo deflectRequestInfo = new DeflectRequestInfo();
        deflectRequestInfo.connIndex = index;
        deflectRequestInfo.number = number;
        imsRadioV10().deflectCall(token, deflectRequestInfo);
    }

    @Override
    public void sendSms(int token, int messageRef, String format, String smsc,
                        boolean isRetry, byte[] pdu) throws RemoteException {
        ImsSmsMessage imsSms = ImsRadioUtils.buildImsSms(messageRef, format,
                smsc, isRetry, pdu);
        imsRadioV12().sendImsSms(token, imsSms);
    }

    @Override
    public void acknowledgeSms(int token, int messageRef, int deliverStatus)
            throws RemoteException {
        int deliverStatusInfo = ImsRadioUtils.imsSmsDeliverStatusToHidl(deliverStatus);
        vendor.qti.hardware.radio.ims.V1_5.IImsRadio imsRadioV15 = imsRadioV15();
        if (imsRadioV15 != null) {
            imsRadioV15().acknowledgeSms_1_5(token, messageRef, deliverStatusInfo);
        } else {
            Log.w(this, "ImsRadioV15 is null. Invoking ImsRadioV12.acknowledgeSms()");
            imsRadioV12().acknowledgeSms(token, messageRef, deliverStatusInfo);
        }
    }

    @Override
    public void acknowledgeSmsReport(int token, int messageRef,
                                     int statusReportStatus) throws RemoteException {
        int statusReportStatusInfo = ImsRadioUtils.imsSmsStatusReportStatusToHidl(
                statusReportStatus);
        imsRadioV12().acknowledgeSmsReport(token, messageRef, statusReportStatusInfo);
    }

    @Override
    public String getSmsFormat() throws RemoteException{
        return imsRadioV12().getSmsFormat();
    }

    @Override
    public void sendGeolocationInfo(int token, double lat, double lon,
                                    Address address) throws RemoteException {
        vendor.qti.hardware.radio.ims.V1_2.AddressInfo addressInfo =
                ImsRadioUtils.getHidlAddressInfo(lat, lon, address);
        vendor.qti.hardware.radio.ims.V1_2.IImsRadio imsRadioV12 = imsRadioV12();
        if (imsRadioV12 != null) {
            imsRadioV12.sendGeolocationInfo_1_2(token, lat, lon, addressInfo);
        } else {
            imsRadioV10().sendGeolocationInfo(token, lat, lon,
                    ImsRadioUtils.migrateAddressToV10(addressInfo));
        }
    }

    @Override
    public void hangup(int token, int connectionId, String userUri, String confUri,
                       boolean mpty, int failCause, String errorInfo) throws RemoteException {
        vendor.qti.hardware.radio.ims.V1_3.HangupRequestInfo hangup =
                ImsRadioUtils.buildHangupRequest(connectionId, userUri, confUri, mpty, failCause,
                errorInfo);

        vendor.qti.hardware.radio.ims.V1_3.IImsRadio imsRadioV13 = imsRadioV13();
        if (imsRadioV13 != null) {
            imsRadioV13.hangup_1_3(token, hangup);
        } else {
            Log.w(this, "ImsRadioV13 is null. Invoking ImsRadioV10.hangup()");
            imsRadioV10().hangup(token, ImsRadioUtils.migrateHangupRequestInfoFromV13(hangup));
        }
    }

    @Override
    public void queryServiceStatus(int token) throws RemoteException {
        imsRadioV10().queryServiceStatus(token);
    }

    @Override
    public void setServiceStatus(int token, List<CapabilityStatus> capabilityStatusList,
                                 int restrictCause) throws RemoteException {
        if (imsRadioV16() != null) {
            ArrayList<vendor.qti.hardware.radio.ims.V1_6.ServiceStatusInfo> serviceStatusInfoList =
                    ImsRadioUtils.buildServiceStatusInfoList(capabilityStatusList, restrictCause);

            imsRadioV16().setServiceStatus_1_6(token, serviceStatusInfoList);
        } else {
            CapabilityStatus capabilityStatus = capabilityStatusList.get(0);
            vendor.qti.hardware.radio.ims.V1_6.ServiceStatusInfo serviceStatusInfo =
                    ImsRadioUtils.buildServiceStatusInfo(
                    ImsRadioUtils.mapCapabilityToSrvType(capabilityStatus.getCapability()),
                    ImsRadioUtils.mapRadioTechToHidlRadioTech(capabilityStatus.getRadioTech()),
                    ImsRadioUtils.mapValueToServiceStatus(capabilityStatus.getStatus()),
                    restrictCause);
            imsRadioV10().setServiceStatus(token, ImsRadioUtils.migrateServiceStatusInfoFromV16(
                    serviceStatusInfo));
        }
    }

    @Override
    public void getImsRegistrationState(int token) throws RemoteException {
        imsRadioV10().getImsRegistrationState(token);
    }

    @Override
    public void requestRegistrationChange(int token, int imsRegState) throws RemoteException {
        imsRadioV10().requestRegistrationChange(token,
                ImsRadioUtils.regStateToHal(imsRegState));
    }

    @Override
    public void modifyCallInitiate(int token, CallModify callModify) throws RemoteException {
        imsRadioV10().modifyCallInitiate(token,
                ImsRadioUtils.buildCallModifyInfo(callModify));
    }

    @Override
    public void cancelModifyCall(int token, int callId) throws RemoteException {
        imsRadioV10().cancelModifyCall(token, callId);
    }

    @Override
    public void modifyCallConfirm(int token, CallModify callModify) throws RemoteException {
        imsRadioV10().modifyCallConfirm(token,
                ImsRadioUtils.buildCallModifyInfo(callModify));
    }

    @Override
    public void hold(int token, int callId) throws RemoteException {
        imsRadioV10().hold(token, callId);
    }

    @Override
    public void resume(int token, int callId) throws RemoteException {
        imsRadioV10().resume(token, callId);
    }

    @Override
    public void conference(int token) throws RemoteException {
        imsRadioV10().conference(token);
    }

    @Override
    public void explicitCallTransfer(int token, int srcCallId, int type, String number,
                                    int destCallId) throws RemoteException {
        ExplicitCallTransferInfo ectInfo = ImsRadioUtils.buildExplicitCallTransferInfo(srcCallId,
                type, number, destCallId);
        imsRadioV10().explicitCallTransfer(token, ectInfo);
    }

    @Override
    public void setConfig(int token, int item, boolean boolValue,
                          int intValue, String strValue, int errorCause)
                          throws RemoteException, ImsException {
        vendor.qti.hardware.radio.ims.V1_8.IImsRadio imsRadioV18 = imsRadioV18();
        vendor.qti.hardware.radio.ims.V1_8.ConfigInfo configInfoV18 = ImsRadioUtilsV18.
                buildConfigInfo(item, boolValue, intValue, strValue, errorCause);
        Log.i(this, "setConfig: item:" + configInfoV18.item + " boolValue:" + boolValue +
                " intValue:" + intValue + " strValue:" + strValue + " errorCause:" + errorCause);
        if (imsRadioV18 != null) {
            imsRadioV18.setConfig_1_8(token, configInfoV18);
        } else {
            vendor.qti.hardware.radio.ims.V1_6.ConfigInfo configInfoV16 = ImsRadioUtilsV16.
                    buildConfigInfo(item, boolValue, intValue, strValue, errorCause);
            if (imsRadioV16() != null) {
                imsRadioV16().setConfig_1_6(token, configInfoV16);
            } else {
                vendor.qti.hardware.radio.ims.V1_5.ConfigInfo configInfoV15 =
                        ImsRadioUtilsV15.migrateConfigInfoFromV16(configInfoV16);
                if (imsRadioV15() != null) {
                    imsRadioV15().setConfig_1_5(token, configInfoV15);
                } else {
                    imsRadioV10().setConfig(token, ImsRadioUtils.
                            migrateConfigInfoFromV15(configInfoV15));
                }
            }
        }
    }

    @Override
    public void getConfig(int token, int item, boolean boolValue,
                          int intValue, String strValue, int errorCause) throws RemoteException {
        vendor.qti.hardware.radio.ims.V1_8.IImsRadio imsRadioV18 = imsRadioV18();
        vendor.qti.hardware.radio.ims.V1_8.ConfigInfo configInfoV18 = ImsRadioUtilsV18.
                buildConfigInfo(item, boolValue, intValue, strValue, errorCause);
        Log.i(this, "getConfig: item:" + configInfoV18.item + " boolValue:" + boolValue +
                " intValue:" + intValue + " strValue:" + strValue + " errorCause:" + errorCause);
        if (imsRadioV18 != null) {
            imsRadioV18.getConfig_1_8(token, configInfoV18);
        } else {
            ConfigInfo configInfo = ImsRadioUtils.buildConfigInfo(item, boolValue, intValue,
                    strValue, errorCause);
            imsRadioV10().getConfig(token, configInfo);
        }
    }

    @Override
    public void sendDtmf(int token, int callId, char c) throws RemoteException {
        DtmfInfo dtmfValue = new DtmfInfo();
        dtmfValue.dtmf = Character.toString(c);
        imsRadioV10().sendDtmf(token, dtmfValue);
    }

    @Override
    public void startDtmf(int token, int callId, char c) throws RemoteException {
        DtmfInfo dtmfValue = new DtmfInfo();
        dtmfValue.dtmf = Character.toString(c);
        imsRadioV10().startDtmf(token, dtmfValue);
    }

    @Override
    public void stopDtmf(int token, int callId) throws RemoteException {
        imsRadioV10().stopDtmf(token);
    }

    @Override
    public void setSuppServiceNotification(int token, boolean enable) throws RemoteException {
        imsRadioV10().setSuppServiceNotification(token,
                enable ? ServiceClassStatus.ENABLED : ServiceClassStatus.DISABLED);
    }

    @Override
    public void getClir(int token) throws RemoteException {
        imsRadioV10().getClir(token);
    }

    @Override
    public void setClir(int token, int clirMode) throws RemoteException {
        ClirInfo clirValue = new ClirInfo();
        clirValue.paramN = clirMode;
        imsRadioV10().setClir(token, clirValue);
    }

    @Override
    public void getCallWaiting(int token, int serviceClass) throws RemoteException {
        imsRadioV10().getCallWaiting(token, serviceClass);
    }

    @Override
    public void setCallWaiting(int token, boolean enable, int serviceClass) throws RemoteException {
        imsRadioV10().setCallWaiting(token,
                enable ? ServiceClassStatus.ENABLED : ServiceClassStatus.DISABLED, serviceClass);
    }

    @Override
    public void setCallForwardStatus(int token, int startHour, int startMinute,
                                     int endHour, int endMinute, int action,
                                     int cfReason, int serviceClass, String number,
                                     int timeSeconds) throws RemoteException {
        vendor.qti.hardware.radio.ims.V1_0.CallForwardInfo cfInfo =
                ImsRadioUtils.buildCallForwardInfo(cfReason, serviceClass, number,
                action, timeSeconds);
        if (startHour != Integer.MAX_VALUE && startMinute != Integer.MAX_VALUE) {
            cfInfo.hasCallFwdTimerStart = true;
            ImsRadioUtils.buildCallFwdTimerInfo(cfInfo.callFwdTimerStart, startHour, startMinute);
        }

        if (endHour != Integer.MAX_VALUE && endMinute != Integer.MAX_VALUE) {
            cfInfo.hasCallFwdTimerEnd = true;
            ImsRadioUtils.buildCallFwdTimerInfo(cfInfo.callFwdTimerEnd, endHour, endMinute);
        }
        imsRadioV10().setCallForwardStatus(token, cfInfo);
    }

    @Override
    public void queryCallForwardStatus(int token, int cfReason, int serviceClass,
                                       String number, boolean expectMore) throws RemoteException {
        vendor.qti.hardware.radio.ims.V1_0.CallForwardInfo cfInfo =
                ImsRadioUtils.buildCallForwardInfo(cfReason, serviceClass, number,
                STATUS_INTERROGATE, 0);
        imsRadioV10().queryCallForwardStatus(token, cfInfo);
    }

    @Override
    public void getClip(int token) throws RemoteException {
        imsRadioV10().getClip(token);
    }

    @Override
    public void setUiTtyMode(int token, int uiTtyMode) throws RemoteException {
        TtyInfo info = new TtyInfo();
        info.mode = ImsRadioUtils.ttyModeToHal(uiTtyMode);
        imsRadioV10().setUiTtyMode(token, info);
    }

    @Override
    public void exitEmergencyCallbackMode(int token) throws RemoteException {
        imsRadioV10().exitEmergencyCallbackMode(token);
    }

    @Override
    public void suppServiceStatus(int token, int operationType, int facility,
                                  String[] inCbNumList, String password,
                                  int serviceClass, boolean expectMore) throws RemoteException {
        CbNumListInfo cbNumListInfo = ImsRadioUtils.buildCbNumListInfo(inCbNumList, serviceClass);
        vendor.qti.hardware.radio.ims.V1_6.IImsRadio imsRadio = imsRadioV16();
        if (imsRadio != null) {
            imsRadio.suppServiceStatus_1_6(token, operationType,
                    ImsRadioUtils.facilityTypeToHal(facility), cbNumListInfo,
                    password != null ? password : "");
        } else {
            imsRadioV10().suppServiceStatus(token, operationType,
                    ImsRadioUtils.facilityTypeToHal(facility), cbNumListInfo);
        }
    }

    @Override
    public void getColr(int token) throws RemoteException {
        imsRadioV10().getColr(token);
    }

    @Override
    public void setColr(int token, int presentationValue) throws RemoteException {
        ColrInfo colrValue = new ColrInfo();
        colrValue.presentation = ImsRadioUtils.ipPresentationToHal(presentationValue);
        imsRadioV10().setColr(token, colrValue);
    }

    @Override
    public void getRtpStatistics(int token) throws RemoteException {
        imsRadioV10().getRtpStatistics(token);
    }

    @Override
    public void getRtpErrorStatistics(int token) throws RemoteException {
        imsRadioV10().getRtpErrorStatistics(token);
    }

    @Override
    public void getImsSubConfig(int token) throws RemoteException {
        imsRadioV10().getImsSubConfig(token);
    }

    @Override
    public void sendRttMessage(int token, String message) throws RemoteException {
        imsRadioV10().sendRttMessage(token, message);
    }

    @Override
    public void queryVirtualLineInfo(int token, String msisdn) throws RemoteException {
        imsRadioV14().queryVirtualLineInfo(token, msisdn);
    }

    @Override
    public void registerMultiIdentityLines(int token, Collection
            <MultiIdentityLineInfo> linesInfo) throws RemoteException {
        ArrayList<MultiIdentityLineInfoHal> halLinesInfo = new ArrayList<>();
        for (MultiIdentityLineInfo line : linesInfo) {
            MultiIdentityLineInfoHal lineInfo =  ImsRadioUtilsV14.
                    toMultiIdentityLineInfoHal(line);
            halLinesInfo.add(lineInfo);
        }
        imsRadioV14().registerMultiIdentityLines(token, halLinesInfo);
    }

    @Override
    public void sendSipDtmf(int token, String requestCode) throws RemoteException {
        imsRadioV17().sendSipDtmf(token, requestCode);
    }

    @Override
    public void setMediaConfiguration(int token, Point screenSize, Point avcSize,
            Point hevcSize) throws RemoteException {
        fail();
    }

    @Override
    public void queryMultiSimVoiceCapability(int token) throws RemoteException {
        fail();
    }

    @Override
    public void exitSmsCallBackMode(int token) throws RemoteException {
        fail();
    }

    @Override
    public void sendVosSupportStatus(int token, boolean isVosSupported) throws RemoteException {
        imsRadioV19().sendVosSupportStatus(token, isVosSupported);
    }

    @Override
    public void sendVosActionInfo(int token, VosActionInfo vosActionInfo) throws RemoteException {
        imsRadioV19().sendVosActionInfo(token, ImsRadioUtils.fromVosActionInfo(vosActionInfo));
    }

    @Override
    public void setGlassesFree3dVideoCapability(int token, boolean enable3dVideo)
            throws RemoteException {
        fail();
    }

    @Override
    public void abortConference(int token, int conferenceAbortReason)
            throws RemoteException {
        fail();
    }

    private void fail() throws RemoteException {
        throw new RemoteException("HIDL does not support this API");
    }

    public void log(String msg) {
        Log.i(this, msg + "[SUB" + mPhoneId + "]");
    }

    public void logv(String msg) {
        Log.v(this, msg + "[SUB" + mPhoneId + "]");
    }
}
