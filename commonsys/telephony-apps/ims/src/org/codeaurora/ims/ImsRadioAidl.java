/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package org.codeaurora.ims;

import android.content.Context;
import android.graphics.Point;
import android.location.Address;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.ims.ImsException;
import android.telephony.TelephonyManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.qualcomm.ims.utils.Log;

import vendor.qti.hardware.radio.ims.AcknowledgeSmsInfo;
import vendor.qti.hardware.radio.ims.AcknowledgeSmsReportInfo;
import vendor.qti.hardware.radio.ims.AnswerRequest;
import vendor.qti.hardware.radio.ims.CallForwardInfo;
import vendor.qti.hardware.radio.ims.CallWaitingInfo;
import vendor.qti.hardware.radio.ims.ClirInfo;
import vendor.qti.hardware.radio.ims.ColrInfo;
import vendor.qti.hardware.radio.ims.ConferenceAbortReasonInfo;
import vendor.qti.hardware.radio.ims.ConfigInfo;
import vendor.qti.hardware.radio.ims.DeflectRequestInfo;
import vendor.qti.hardware.radio.ims.DialRequest;
import vendor.qti.hardware.radio.ims.DtmfInfo;
import vendor.qti.hardware.radio.ims.EmergencyDialRequest;
import vendor.qti.hardware.radio.ims.ExplicitCallTransferInfo;
import vendor.qti.hardware.radio.ims.GeoLocationInfo;
import vendor.qti.hardware.radio.ims.HangupRequestInfo;
import vendor.qti.hardware.radio.ims.MediaConfig;
import vendor.qti.hardware.radio.ims.ServiceClassStatus;
import vendor.qti.hardware.radio.ims.ServiceStatusInfo;
import vendor.qti.hardware.radio.ims.SmsSendRequest;
import vendor.qti.hardware.radio.ims.SuppServiceStatusRequest;
import vendor.qti.hardware.radio.ims.TtyInfo;
/*
 * Class that sends requests to IImsRadio AIDL interface
 */

public class ImsRadioAidl implements IImsRadio {

    static final String LOG_TAG = "ImsRadioAidl";
    private IImsRadioResponse mResponse;
    private IImsRadioIndication mIndication;
    private int mPhoneId;
    // The death recepient object which gets notified when IImsRadio service dies.
    private ImsRadioDeathRecipient mDeathRecipient;

    private final String mServiceInstance;

    private IBinder mBinder;
    // Synchronization object of HAL interfaces.
    private final Object mLock = new Object();

    /* Object of the type ImsRadioResponse which is registered with the IImsRadio
     * service for all callbacks to be routed back */
    private vendor.qti.hardware.radio.ims.IImsRadioResponse mImsRadioResponse;

    /* Object of the type ImsRadioIndication which is registered with the IImsRadio
     * service for all unsolicited messages to be sent*/
    private vendor.qti.hardware.radio.ims.IImsRadioIndication mImsRadioIndication;

    /*  Instance of the IImsRadio interface */
    private volatile vendor.qti.hardware.radio.ims.IImsRadio mImsRadio;
    private static final String IIMS_RADIO_SERVICE_NAME =
            "vendor.qti.hardware.radio.ims.IImsRadio/imsradio";
    private final String mLogSuffix;

    private static final int STATUS_INTERROGATE = 2;

    private boolean mIsDisposed = false;

    private final Context mContext;
    private final TelephonyManager mTm;

    // DSDS transition feature is supported from MPSS.DE.5 and up
    private static final int DSDS_TRANSITION_SUPPORTED_MODEM_VERSION = 5;
    private static final String DSDS_TRANSITION_SUPPORTED_MODEM = "MPSS.DE.";

    public ImsRadioAidl(IImsRadioResponse response, IImsRadioIndication
            indication, int phoneId, Context context) {
        mPhoneId = phoneId;
        mResponse = response;
        mIndication = indication;
        mServiceInstance = IIMS_RADIO_SERVICE_NAME + mPhoneId;
        mLogSuffix ="[SUB" + mPhoneId + "]";
        mDeathRecipient = new ImsRadioDeathRecipient();
        mContext = context;
        mTm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        initImsRadio();
    }

    private void log(String msg) {
        Log.i(this, msg + mLogSuffix);
    }

    private void logv(String msg) {
        Log.v(this, msg + mLogSuffix);
    }

    private void loge(String msg) {
        Log.e(this, msg + mLogSuffix);
    }

    /**
     * Class that implements the binder death recipient to be notified when
     * IImsRadio service dies.
     */
    final class ImsRadioDeathRecipient implements IBinder.DeathRecipient {
        /**
         * Callback that gets called when the service has died
         */
        @Override
        public void binderDied() {
            loge(" IImsRadio Died");
            resetService();
            initImsRadio();
        }
    }

    private void resetService() {
        log("IImsRadio service down: Resetting HAL interfaces.");
        mIndication.onServiceDown();
        IBinder binder = null;
        synchronized (mLock) {
            mImsRadio = null;
            mImsRadioResponse = null;
            mImsRadioIndication = null;
            binder = mBinder;
            mBinder = null;
        }
        if (binder != null) {
            try {
                boolean result = binder.unlinkToDeath(mDeathRecipient, 0 /* Not used */);
            } catch (Exception ex) {
                loge("IImsRadio binder is null" + ex);
            }
        }
    }

    private vendor.qti.hardware.radio.ims.IImsRadio imsRadio() {
        vendor.qti.hardware.radio.ims.IImsRadio imsRadio = null;
        synchronized (mLock) {
            imsRadio = mImsRadio;
        }
        return imsRadio;
    }

    /**
     * Initialize the instance of IImsRadio AIDL. Get the service and register the
     * callback object to be called for the responses to the solicited and
     * unsolicited requests.
     */
    private void initImsRadio() {
        try {
            log("initImsRadio: connect to IImsRadio Stable AIDL");
            IBinder binder = Binder.allowBlocking(
                    ServiceManager.waitForDeclaredService(mServiceInstance));
            if (binder == null) {
                loge("initImsRadio: Stable AIDL is NOT used.");
                return;
            }
            vendor.qti.hardware.radio.ims.IImsRadio imsRadio = vendor.qti.hardware.
                    radio.ims.IImsRadio.Stub.asInterface(binder);
            if (imsRadio == null) {
                loge("initImsRadio: mAidlInstance is null");
                return;
            }
            log("initImsRadio: imsRadio availability: " +
                    toAvailability(imsRadio != null));

            ImsRadioResponseAidl imsRadioResponse = new ImsRadioResponseAidl(mResponse, mPhoneId);
            ImsRadioIndicationAidl imsRadioIndication = new ImsRadioIndicationAidl
                    (mIndication, mPhoneId);

            synchronized (mLock) {
                if (mIsDisposed) {
                    log("initImsRadio: already disposed. exiting.");
                    return;
                }
                mBinder = binder;
                mImsRadioResponse = imsRadioResponse;
                mImsRadioIndication = imsRadioIndication;
                imsRadio.setCallback(mImsRadioResponse, mImsRadioIndication);
                mImsRadio = imsRadio;
            }
            binder.linkToDeath(mDeathRecipient, 0 /* Not Used */);
            mIndication.onServiceUp();

        } catch (Exception ex) {
            loge( "initImsRadio: Exception: " + ex);
            resetService();
        }
    }

    public static boolean isAidlAvailable(int phoneId) {
        try {
            return ServiceManager.isDeclared(IIMS_RADIO_SERVICE_NAME + phoneId);
        } catch (SecurityException e) {
            // Security exception will be thrown if sepolicy for AIDL is not present
            Log.e("ImsRadioAidl", "Security exception while call into AIDL");
        }
        return false;
    }

    // Implementation methods for IImsRadio java interface
    @Override
    public boolean isFeatureSupported(int feature) {
        switch (feature) {
            case Feature.SMS:
            case Feature.EMERGENCY_DIAL:
            case Feature.CONSOLIDATED_SET_SERVICE_STATUS:
            case Feature.CALL_COMPOSER_DIAL:
            case Feature.USSD:
            case Feature.CRS:
            case Feature.SIP_DTMF:
            case Feature.CONFERENCE_CALL_STATE_COMPLETED:
                return imsRadio() != null;
            case Feature.SET_MEDIA_CONFIG:
                //Supports this from Stable AIDL version 2
                return StableAidl.isVersionSupported(imsRadio(), 2);
            case Feature.MULTI_SIM_VOICE_CAPABILITY:
                //Supports this from Stable AIDL version 3
                return StableAidl.isVersionSupported(imsRadio(), 3);
            case Feature.EXIT_SCBM:
                //Supports this from Stable AIDL version 4
                return StableAidl.isVersionSupported(imsRadio(), 4);
            case Feature.B2C_ENRICHED_CALLING:
                //Supports this from Stable AIDL version 7
                return StableAidl.isVersionSupported(imsRadio(), 7);
            case Feature.DATA_CHANNEL:
                //Supports this from Stable AIDL version 8
                return StableAidl.isVersionSupported(imsRadio(), 8);
            case Feature.VIDEO_ONLINE_SERVICE:
                //Supports this from Stable AIDL version 11
                return StableAidl.isVersionSupported(imsRadio(), 11);
            case Feature.DSDS_TRANSITION:
                //Supported from Stable AIDL version 12 and MPSS.DE.5+
                return isDsdsTransitionSupported();
            default:
                return false;
        }
    }

    private boolean isDsdsTransitionSupported() {
        if (!StableAidl.isVersionSupported(imsRadio(), 12)) {
            return false;
        }
        String basebandVersion = mTm.getBasebandVersion();
        Log.d(this, "BasebandVersion string: " + basebandVersion);
        if (basebandVersion.isEmpty()) {
            return false;
        }
        // splits on the first digit ex: MPSS.DE.5.0-01532-LANAI_GEN_TEST-1
        // result: MPSS.DE.5
        String[] output = basebandVersion.split("(?<=\\d)");
        if (output.length <= 0) {
            return false;
        }
        int value = Character.getNumericValue(output[0].charAt(output[0].length() -1));
        return containsModemVersion(output[0], DSDS_TRANSITION_SUPPORTED_MODEM) &&
                isSupportedModemVersion(value, DSDS_TRANSITION_SUPPORTED_MODEM_VERSION);
    }

    // Helper function to check if it is the expected modem type
    private boolean containsModemVersion(String modemBaseband, String modemType) {
        return modemBaseband.contains(modemType);
    }

    // Helper function to check if supported on the modem version
    private boolean isSupportedModemVersion(int modemVersion, int supportedModemVersion) {
        return modemVersion >= supportedModemVersion;
    }

    public boolean isAlive() {
        return imsRadio() != null;
    }

    @Override
    public void dispose() {
        synchronized(mLock) {
            mIsDisposed = true;
        }
        resetService();
    }

    @Override
    public void addParticipant(int token, String address, int clirMode,
                               CallDetails callDetails) throws RemoteException {
        DialRequest dialRequest = StableAidl.fromDialRequest(address, clirMode, callDetails,
                false /*isEncrypted*/, null /*redialInfo*/);
        imsRadio().addParticipant(token, dialRequest);
        logv("addParticipant: token = " + token + " address = " + dialRequest.address +
                " callType = " + dialRequest.callDetails.callType + " callDomain = " +
                dialRequest.callDetails.callDomain + " isConferenceUri = " +
                dialRequest.isConferenceUri + "isCallPull =" + dialRequest.isCallPull +
                " isEncrypted = " + dialRequest.isEncrypted);
    }

    @Override
    public void dial(int token, String address, EmergencyCallInfo eInfo,
                     int clirMode, CallDetails callDetails, boolean isEncrypted,
                     CallComposerInfo ccInfo, RedialInfo redialInfo)
                     throws RemoteException {

        DialRequest dialRequest = StableAidl.fromDialRequest(address, clirMode, callDetails,
                isEncrypted, redialInfo);
        if (eInfo != null) {
            EmergencyDialRequest emergencyDialRequest = StableAidl.fromEmergencyDialRequest(
                    eInfo, address, clirMode, callDetails, isEncrypted, redialInfo);
            imsRadio().emergencyDial(token, emergencyDialRequest);
        } else if (ccInfo != null) {
            vendor.qti.hardware.radio.ims.CallComposerDialRequest composerDialRequest =
                    StableAidl.fromCallComposerDialRequest(ccInfo, address, clirMode, callDetails,
                    isEncrypted, redialInfo);
            imsRadio().callComposerDial(token, composerDialRequest);
        } else {
            imsRadio().dial(token, dialRequest);
        }
        logv("dial: address = " + dialRequest.address + "callType =" +
                dialRequest.callDetails.callType + "callDomain =" +
                dialRequest.callDetails.callDomain + "isConferenceUri =" +
                dialRequest.isConferenceUri + "isCallPull =" + dialRequest.isCallPull +
                "isEncrypted =" + dialRequest.isEncrypted +
                "rttMode =" + dialRequest.callDetails.rttMode);
    }

    @Override
    public void sendUssd(int token, String address) throws RemoteException {
        imsRadio().sendUssd(token, address);
    }

    @Override
    public void cancelPendingUssd(int token) throws RemoteException {
        imsRadio().cancelPendingUssd(token);
    }

    @Override
    public void answer(int token, int callType, int ipPresentation, int rttMode)
                       throws RemoteException {
        AnswerRequest answerRequest = StableAidl.fromAnswerRequest(callType, ipPresentation,
                rttMode);
        imsRadio().answer(token, answerRequest);
    }

    @Override
    public void deflectCall(int token, int index, String number)
                            throws RemoteException {
        DeflectRequestInfo deflectRequestInfo = StableAidl.fromDeflectCall(index, number);
        imsRadio().deflectCall(token, deflectRequestInfo);
    }

    @Override
    public void sendSms(int token, int messageRef, String format, String smsc,
                        boolean isRetry, byte[] pdu) throws RemoteException {
        SmsSendRequest smsRequest = StableAidl.fromSmsRequest(messageRef, format,
                smsc, isRetry, pdu);
        imsRadio().sendSms(token, smsRequest);
    }

    @Override
    public void acknowledgeSms(int token, int messageRef, int deliverStatus)
                               throws RemoteException {
        AcknowledgeSmsInfo smsInfo = StableAidl.fromAcknowledgeSms(messageRef,deliverStatus);
        imsRadio().acknowledgeSms(token, smsInfo);
    }

    @Override
    public void acknowledgeSmsReport(int token, int messageRef,
            int statusReportStatus) throws RemoteException {
        AcknowledgeSmsReportInfo smsReportInfo = StableAidl.fromAcknowledgeSmsReport(
                messageRef, statusReportStatus);
        imsRadio().acknowledgeSmsReport(token, smsReportInfo);
    }

    @Override
    public String getSmsFormat() throws RemoteException{
        return imsRadio().getSmsFormat();
    }

    @Override
    public void sendGeolocationInfo(int token, double lat, double lon,
                                    Address address) throws RemoteException {
        GeoLocationInfo geoLocationInfo = StableAidl.fromGeoLocation(lat, lon, address);
        imsRadio().sendGeolocationInfo(token, geoLocationInfo);
    }

    @Override
    public void hangup(int token, int connectionId, String userUri, String confUri,
            boolean mpty, int failCause, String errorInfo) throws RemoteException {
        HangupRequestInfo hangup = StableAidl.fromHangup(connectionId, userUri, confUri, mpty,
                failCause, errorInfo);
        imsRadio().hangup(token, hangup);
    }

    @Override
    public void queryServiceStatus(int token) throws RemoteException {
        imsRadio().queryServiceStatus(token);
    }

    @Override
    public void setServiceStatus(int token, List<CapabilityStatus> capabilityStatusList,
                                 int restrictCause) throws RemoteException {
        ServiceStatusInfo[] svcstatusInfo = StableAidl.fromServiceStatusInfoList(
                capabilityStatusList, restrictCause);
        imsRadio().setServiceStatus(token, svcstatusInfo);
    }

    @Override
    public void getImsRegistrationState(int token) throws RemoteException {
        imsRadio().getImsRegistrationState(token);
    }

    @Override
    public void requestRegistrationChange(int token, int imsRegState)
                                          throws RemoteException {
        imsRadio().requestRegistrationChange(token, StableAidl.fromRegState(imsRegState));
    }

    @Override
    public void modifyCallInitiate(int token, CallModify callModify)
            throws RemoteException {
        imsRadio().modifyCallInitiate(token, StableAidl.fromCallModify(callModify));
    }

    @Override
    public void cancelModifyCall(int token, int callId) throws RemoteException {
        imsRadio().cancelModifyCall(token, callId);
    }

    @Override
    public void modifyCallConfirm(int token, CallModify callModify)
            throws RemoteException {
        imsRadio().modifyCallConfirm(token, StableAidl.fromCallModify(callModify));
    }

    @Override
    public void hold(int token, int callId) throws RemoteException {
        imsRadio().hold(token, callId);
    }

    @Override
    public void resume(int token, int callId) throws RemoteException {
        imsRadio().resume(token, callId);
    }

    @Override
    public void conference(int token) throws RemoteException {
        imsRadio().conference(token);
    }

    @Override
    public void explicitCallTransfer(int token, int srcCallId, int type, String number,
                                     int destCallId) throws RemoteException {
        ExplicitCallTransferInfo ectInfo = StableAidl.fromEctInfo(srcCallId, type,
                number, destCallId);
        imsRadio().explicitCallTransfer(token, ectInfo);
    }

    @Override
    public void setConfig(int token, int item, boolean boolValue,
            int intValue, String strValue, int errorCause)
            throws RemoteException, ImsException {
        ConfigInfo config = StableAidl.fromImsConfig(item, boolValue,
                intValue, strValue, errorCause);
        imsRadio().setConfig(token, config);
    }

    @Override
    public void getConfig(int token, int item, boolean boolValue,
            int intValue, String strValue, int errorCause) throws RemoteException {
        ConfigInfo config = StableAidl.fromImsConfig(item, boolValue,
                intValue, strValue, errorCause);
        imsRadio().getConfig(token, config);
    }

    @Override
    public void sendDtmf(int token, int callId, char c) throws RemoteException {
        DtmfInfo dtmfValue = StableAidl.fromDtmf(callId, c);
        imsRadio().sendDtmf(token, dtmfValue);
    }

    @Override
    public void startDtmf(int token, int callId, char c) throws RemoteException {
        DtmfInfo dtmfValue = StableAidl.fromDtmf(callId, c);
        imsRadio().startDtmf(token, dtmfValue);
    }

    @Override
    public void stopDtmf(int token, int callId) throws RemoteException {
        imsRadio().stopDtmf(token);
    }

    @Override
    public void setSuppServiceNotification(int token, boolean enable)
            throws RemoteException {
        imsRadio().setSuppServiceNotification(token,
                enable ? ServiceClassStatus.ENABLED : ServiceClassStatus.DISABLED);
    }

    @Override
    public void getClir(int token) throws RemoteException {
        imsRadio().getClir(token);
    }

    @Override
    public void setClir(int token, int clirMode) throws RemoteException {
        ClirInfo clirValue = StableAidl.fromClir(clirMode);
        imsRadio().setClir(token, clirValue);
    }

    @Override
    public void getCallWaiting(int token, int serviceClass) throws RemoteException {
        imsRadio().getCallWaiting(token, serviceClass);
    }

    @Override
    public void setCallWaiting(int token, boolean enable, int serviceClass)
            throws RemoteException {
        CallWaitingInfo callWaitingInfo = StableAidl.fromCallWaiting(enable, serviceClass);
        imsRadio().setCallWaiting(token, callWaitingInfo);
    }

    @Override
    public void setCallForwardStatus(int token, int startHour, int startMinute,
                                     int endHour, int endMinute, int action,
                                     int cfReason, int serviceClass, String number,
                                     int timeSeconds) throws RemoteException {
        CallForwardInfo cfInfo = StableAidl.fromImsCallForwardTimerInfo(cfReason, serviceClass,
              number, action, timeSeconds, startHour, startMinute, endHour, endMinute);
        imsRadio().setCallForwardStatus(token, cfInfo);
    }

    @Override
    public void queryCallForwardStatus(int token, int cfReason, int serviceClass,
                                       String number, boolean expectMore) throws RemoteException {
        CallForwardInfo cfInfo = StableAidl.fromImsCallForwardTimerInfo(cfReason,
                serviceClass, number, STATUS_INTERROGATE, 0, expectMore);
        imsRadio().queryCallForwardStatus(token, cfInfo);
    }

    @Override
    public void getClip(int token) throws RemoteException {
        imsRadio().getClip(token);
    }

    @Override
    public void setUiTtyMode(int token, int uiTtyMode) throws RemoteException {
        TtyInfo info = StableAidl.fromTty(uiTtyMode);
        imsRadio().setUiTtyMode(token, info);
    }

    @Override
    public void exitEmergencyCallbackMode(int token) throws RemoteException {
        imsRadio().exitEmergencyCallbackMode(token);
    }

    @Override
    public void suppServiceStatus(int token, int operationType, int facility,
                                  String[] inCbNumList, String password,
                                  int serviceClass, boolean expectMore) throws RemoteException {
        SuppServiceStatusRequest suppServiceStatusRequest = StableAidl.fromSuppServiceStatus(
                operationType, facility, inCbNumList, password, serviceClass, expectMore);
        imsRadio().suppServiceStatus(token, suppServiceStatusRequest);
    }

    @Override
    public void getColr(int token) throws RemoteException {
        imsRadio().getColr(token);
    }

    @Override
    public void setColr(int token, int presentationValue) throws RemoteException {
        ColrInfo colrValue = StableAidl.fromColrValue(presentationValue);
        imsRadio().setColr(token, colrValue);
    }

    @Override
    public void getRtpStatistics(int token) throws RemoteException {
        imsRadio().getRtpStatistics(token);
    }

    @Override
    public void getRtpErrorStatistics(int token) throws RemoteException {
        imsRadio().getRtpErrorStatistics(token);
    }

    @Override
    public void getImsSubConfig(int token) throws RemoteException {
        imsRadio().getImsSubConfig(token);
    }

    @Override
    public void sendRttMessage(int token, String message) throws RemoteException {
        imsRadio().sendRttMessage(token, message);
    }

    @Override
    public void queryVirtualLineInfo(int token, String msisdn) throws RemoteException {
        imsRadio().queryVirtualLineInfo(token, msisdn);
    }

    @Override
    public void registerMultiIdentityLines(int token, Collection
            <MultiIdentityLineInfo> linesInfo) throws RemoteException {
        vendor.qti.hardware.radio.ims.MultiIdentityLineInfo[] halLinesInfo =
                StableAidl.fromMultiIdentityLineInfoList(linesInfo);
        imsRadio().registerMultiIdentityLines(token, halLinesInfo);
    }

    @Override
    public void sendSipDtmf(int token, String requestCode) throws RemoteException {
        imsRadio().sendSipDtmf(token, requestCode);
    }

    @Override
    public void setMediaConfiguration(int token, Point screenSize, Point avcSize,
            Point hevcSize) throws RemoteException {
        MediaConfig mediaConfig = StableAidl.fromImsMediaConfig(screenSize, avcSize, hevcSize);
        imsRadio().setMediaConfiguration(token, mediaConfig);
    }

    @Override
    public void queryMultiSimVoiceCapability(int token) throws RemoteException {
        imsRadio().queryMultiSimVoiceCapability(token);
    }

    @Override
    public void exitSmsCallBackMode(int token) throws RemoteException {
        imsRadio().exitSmsCallBackMode(token);
    }

    @Override
    public void sendVosSupportStatus(int token, boolean isVosSupported) throws RemoteException {
        imsRadio().sendVosSupportStatus(token, isVosSupported);
    }

    @Override
    public void sendVosActionInfo(int token, VosActionInfo vosActionInfo) throws RemoteException {
        imsRadio().sendVosActionInfo(token, StableAidl.fromVosActionInfo(vosActionInfo));
    }

    @Override
    public void setGlassesFree3dVideoCapability(int token, boolean enable3dVideo)
            throws RemoteException {
        imsRadio().setGlassesFree3dVideoCapability(token, enable3dVideo);
    }

    @Override
    public void abortConference(int token, int conferenceAbortReason) throws RemoteException {
        ConferenceAbortReasonInfo conferenceAbortReasonInfo =
                StableAidl.fromConferenceAbortReasonInfo(conferenceAbortReason);
        imsRadio().abortConference(token, conferenceAbortReasonInfo);
    }
}
