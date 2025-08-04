/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package org.codeaurora.ims;

import android.telephony.ims.ImsReasonInfo;

import com.qualcomm.ims.utils.Log;

import java.util.ArrayList;
import java.util.Arrays;

import org.codeaurora.ims.sms.SmsResponse;

import vendor.qti.hardware.radio.ims.CallForwardInfo;
import vendor.qti.hardware.radio.ims.CallForwardStatusInfo;
import vendor.qti.hardware.radio.ims.CallWaitingInfo;
import vendor.qti.hardware.radio.ims.ClipProvisionStatus;
import vendor.qti.hardware.radio.ims.ClirInfo;
import vendor.qti.hardware.radio.ims.ColrInfo;
import vendor.qti.hardware.radio.ims.ConfigInfo;
import vendor.qti.hardware.radio.ims.ConfigFailureCause;
import vendor.qti.hardware.radio.ims.ErrorCode;
import vendor.qti.hardware.radio.ims.ImsSubConfigInfo;
import vendor.qti.hardware.radio.ims.IpPresentation;
import vendor.qti.hardware.radio.ims.MultiSimVoiceCapability;
import vendor.qti.hardware.radio.ims.RegistrationInfo;
import vendor.qti.hardware.radio.ims.Result;
import vendor.qti.hardware.radio.ims.ServiceClassStatus;
import vendor.qti.hardware.radio.ims.ServiceStatusInfo;
import vendor.qti.hardware.radio.ims.SipErrorInfo;
import vendor.qti.hardware.radio.ims.SmsSendFailureReason;
import vendor.qti.hardware.radio.ims.SmsSendResponse;
import vendor.qti.hardware.radio.ims.SmsSendStatus;
import vendor.qti.hardware.radio.ims.SuppServiceStatus;

/* This class handles AIDL responses, converts objects from AIDL->Telephony java type
 * and forwards the response to ImsSenderRxr
 */
public class ImsRadioResponseAidl extends vendor.qti.hardware.radio.ims.IImsRadioResponse.Stub {
    private IImsRadioResponse mImsRadioResponse;
    private int mPhoneId;
    private final String mLogSuffix;

    public ImsRadioResponseAidl(IImsRadioResponse respCallback, int phoneId) {
        mImsRadioResponse = respCallback;
        mPhoneId = phoneId;
        mLogSuffix ="[SUB" + mPhoneId + "]";
    }

    private void log(String msg) {
        Log.i(this, msg + mLogSuffix);
    }

    @Override
    public final int getInterfaceVersion() {
        return vendor.qti.hardware.radio.ims.IImsRadioResponse.VERSION;
    }

    @Override
    public String getInterfaceHash() {
        return vendor.qti.hardware.radio.ims.IImsRadioResponse.HASH;
    }

    @Override
    public void dialResponse(int token, int errorCode) {
        log("Dial response received");
        mImsRadioResponse.onDialResponse(token, StableAidlErrorCode.toErrorCode(errorCode));
    }

    @Override
    public void answerResponse(int token, int errorCode) {
        log("Answer response received");
        mImsRadioResponse.onAnswerResponse(token, StableAidlErrorCode.toErrorCode(errorCode));
    }

    @Override
    public void hangupResponse(int token, int errorCode) {
        log("Hangup response received");
        mImsRadioResponse.onHangupResponse(token, StableAidlErrorCode.toErrorCode(errorCode));
    }

    @Override
    public void requestRegistrationChangeResponse(int token, int errorCode) {
        log("Registration change response received");
        mImsRadioResponse.onRequestRegistrationChangeResponse(token,
                StableAidlErrorCode.toErrorCode(errorCode));
    }

    @Override
    public void queryServiceStatusResponse(int token, int errorCode,
                                           ServiceStatusInfo[] srvStatusList) {
        log("QueryServiceStatusResponse received");
        ArrayList<ServiceStatus> ret = StableAidl.toServiceStatus(srvStatusList);
        mImsRadioResponse.onQueryServiceStatusResponse(token,
                StableAidlErrorCode.toErrorCode(errorCode), ret);
    }

    @Override
    public void setServiceStatusResponse(int token, int errorCode) {
        log("SetServiceStatus response received");
        mImsRadioResponse.onSetServiceStatusResponse(token,
                StableAidlErrorCode.toErrorCode(errorCode));
    }

    @Override
    public void holdResponse(int token, int errorCode, SipErrorInfo sipError) {
        log("Hold response received.");
        ImsReasonInfo imsReasonInfo = StableAidl.toSipError(sipError);
        mImsRadioResponse.onHoldResponse(token,
                StableAidlErrorCode.toErrorCode(errorCode), imsReasonInfo);
    }

    @Override
    public void resumeResponse(int token, int errorCode, SipErrorInfo sipError) {
        log("Resume response received.");
        ImsReasonInfo imsReasonInfo = StableAidl.toSipError(sipError);
        mImsRadioResponse.onResumeResponse(token,
                StableAidlErrorCode.toErrorCode(errorCode), imsReasonInfo);
    }

    @Override
    public void setConfigResponse(int token, int errorCode, ConfigInfo config) {
        log("Set config response received");
        Object ret = StableAidl.toConfigObject(config);
        mImsRadioResponse.onSetConfigResponse(token,
                StableAidlErrorCode.toErrorCode(errorCode), ret);
    }

    @Override
    public void getConfigResponse(int token, int errorCode, ConfigInfo config) {
        log("Get config response received");
        Object ret = StableAidl.toConfigObject(config);
        mImsRadioResponse.onGetConfigResponse(token,
                StableAidlErrorCode.toErrorCode(errorCode), ret);
    }

    @Override
    public void getImsRegistrationStateResponse(int token, int errorCode,
                                        RegistrationInfo registration) {
        log("getImsRegistrationStateResponse received");
        ImsRegistrationInfo regMessage = StableAidl.toImsRegistration(registration);
        mImsRadioResponse.onGetRegistrationResponse(token,
                StableAidlErrorCode.toErrorCode(errorCode), regMessage);
    }

    @Override
    public void suppServiceStatusResponse(int token, int errorCode,
                                          SuppServiceStatus suppServiceStatus) {
        log("Supp Service status response received" +
                " status:" + suppServiceStatus.status +
                " provisionStatus:" + suppServiceStatus.provisionStatus +
                " facilityType:" + suppServiceStatus.facilityType +
                " failureCause:" + suppServiceStatus.failureCause +
                " errorCode:" + suppServiceStatus.errorDetails.errorCode +
                " isPasswordRequired:" + suppServiceStatus.isPasswordRequired);
        SuppSvcResponse suppSvcResponse = StableAidl.toSuppSvcResponse(suppServiceStatus);
        mImsRadioResponse.onSuppServiceStatusResponse(token,
                StableAidlErrorCode.toErrorCode(errorCode), suppSvcResponse);
    }

    @Override
    public void conferenceResponse(int token, int errorCode, SipErrorInfo errorInfo) {
        log("conference response received.");
        ImsReasonInfo imsReasonInfo = StableAidl.toSipError(errorInfo);
        mImsRadioResponse.onConferenceResponse(token,
                StableAidlErrorCode.toErrorCode(errorCode), imsReasonInfo);
    }

    @Override
    public void getClipResponse(int token, int errorCode,
                                ClipProvisionStatus clipProvisionStatus) {
        log("Get clip response received");
        SuppService clipProvStatus = StableAidl.toSuppService(clipProvisionStatus);
        mImsRadioResponse.onGetClipResponse(token,
                StableAidlErrorCode.toErrorCode(errorCode), clipProvStatus);
    }

    @Override
    public void getClirResponse(int token, int errorCode, ClirInfo clirInfo,
                                boolean hasClirInfo) {
        log("Get clir response received");
        int[] response = hasClirInfo ? StableAidl.toClirArray(clirInfo) : null;
        mImsRadioResponse.onGetClirResponse(token,
                StableAidlErrorCode.toErrorCode(errorCode), response);
    }

    @Override
    public void setClirResponse(int token, int errorCode) {
        log("Set clir response received  error " + errorCode);
        mImsRadioResponse.onSetClirResponse(token, StableAidlErrorCode.toErrorCode(errorCode));
    }

    @Override
    public void getColrResponse(int token, int errorCode, ColrInfo colrInfo) {
        log("getColr response received");
        SuppService colrValue = StableAidl.toSuppService(colrInfo);
        mImsRadioResponse.onGetColrResponse(token,
                StableAidlErrorCode.toErrorCode(errorCode), colrValue);
    }

    @Override
    public void exitEmergencyCallbackModeResponse(int token, int errorCode) {
        log("Exit Emergency Callback response received error " + errorCode);
        mImsRadioResponse.onExitEmergencyCallbackModeResponse(token,
                StableAidlErrorCode.toErrorCode(errorCode));
    }

    @Override
    public void sendDtmfResponse(int token, int errorCode) {
        log("Send Dtmf response received error " + errorCode);
        mImsRadioResponse.onSendDtmfResponse(token, StableAidlErrorCode.toErrorCode(errorCode));
    }

    @Override
    public void startDtmfResponse(int token, int errorCode) {
        log("Start Dtmf response received error " + errorCode);
        mImsRadioResponse.onStartDtmfResponse(token, StableAidlErrorCode.toErrorCode(errorCode));
    }

    @Override
    public void stopDtmfResponse(int token, int errorCode) {
        log("Stop Dtmf response received error " + errorCode);
        mImsRadioResponse.onStopDtmfResponse(token, StableAidlErrorCode.toErrorCode(errorCode));
    }

    @Override
    public void setUiTTYModeResponse(int token, int errorCode) {
        log("Set Ui TTY mode response received error " + errorCode);
        mImsRadioResponse.onSetUiTTYModeResponse(token,
                StableAidlErrorCode.toErrorCode(errorCode));
    }

    @Override
    public void modifyCallInitiateResponse(int token, int errorCode) {
        log("Modify call initiate response received");
        mImsRadioResponse.onModifyCallInitiateResponse(token,
                StableAidlErrorCode.toErrorCode(errorCode));
    }

    @Override
    public void modifyCallConfirmResponse(int token, int errorCode) {
        log("Modify call confirm response received");
        mImsRadioResponse.onModifyCallConfirmResponse(token,
                StableAidlErrorCode.toErrorCode(errorCode));
    }

    @Override
    public void queryCallForwardStatusResponse(int token, int errorCode,
            CallForwardInfo[] callForwardInfoList, SipErrorInfo errorDetails) {
        log("Query call forward status response received");
        ImsCallForwardTimerInfo cfTimerInfo[] = StableAidl.
                toImsCallForwardTimerInfo(callForwardInfoList);
        mImsRadioResponse.onQueryCallForwardStatusResponse(token,
                StableAidlErrorCode.toErrorCode(errorCode), cfTimerInfo);
    }

    @Override
    public void getCallWaitingResponse(int token, int errorCode,
            CallWaitingInfo callWaitingInfo, SipErrorInfo errorDetails) {
        log("Get call waiting response received");
        int[] response = StableAidl.toCallWaitingArray(callWaitingInfo);
        mImsRadioResponse.onGetCallWaitingResponse(token,
                StableAidlErrorCode.toErrorCode(errorCode), response);
    }

    @Override
    public void explicitCallTransferResponse(int token, int errorCode,
                                             SipErrorInfo errorInfo) {
        log("Explicit call transfer response received");
        ImsReasonInfo imsReasonInfo = StableAidl.toSipError(errorInfo);
        mImsRadioResponse.onExplicitCallTransferResponse(token,
                StableAidlErrorCode.toErrorCode(errorCode), imsReasonInfo);
    }

    @Override
    public void setSuppServiceNotificationResponse(int token, int errorCode,
                                                   int serviceStatus) {
        log("Set supp service notification response received");
        mImsRadioResponse.onSetSuppServiceNotificationResponse(token,
                StableAidlErrorCode.toErrorCode(errorCode), serviceStatus);
    }

    @Override
    public void getRtpStatisticsResponse(int token, int errorCode,
                                         long packetCount) {
        log("Get Rtp Statistics response received packetCount = " +
                packetCount + " error " + errorCode);
        mImsRadioResponse.onGetRtpStatisticsResponse(token,
                StableAidlErrorCode.toErrorCode(errorCode), packetCount);
    }

    @Override
    public void getRtpErrorStatisticsResponse(int token, int errorCode,
                                              long packetErrorCount) {
        log("Get Rtp Error Statistics response received packetErrorCount = " +
                packetErrorCount + " error " + errorCode);
        mImsRadioResponse.onGetRtpStatisticsResponse(token,
                StableAidlErrorCode.toErrorCode(errorCode), packetErrorCount);
    }

    @Override
    public void addParticipantResponse(int token, int errorCode) {
        log("Add Participant response received. errorCode: " + errorCode);
        mImsRadioResponse.onAddParticipantResponse(token,
                StableAidlErrorCode.toErrorCode(errorCode));
    }

    @Override
    public void deflectCallResponse(int token, int errorCode) {
        log("Deflect call response received");
        mImsRadioResponse.onDeflectCallResponse(token,
                StableAidlErrorCode.toErrorCode(errorCode));
    }

    @Override
    public void sendGeolocationInfoResponse(int token, int errorCode) {
        log("Send geolocation response received");
        mImsRadioResponse.onSendGeolocationInfoResponse(token,
                StableAidlErrorCode.toErrorCode(errorCode));
    }

    @Override
    public void getImsSubConfigResponse(int token, int errorCode,
                                        ImsSubConfigInfo subConfigInfo) {
        log("Subconfig response received");
        ImsSubConfigDetails subConfigDetails = StableAidl.toImsSubconfigDetails(subConfigInfo);
        mImsRadioResponse.onGetImsSubConfigResponse(token,
                StableAidlErrorCode.toErrorCode(errorCode), subConfigDetails);
    }

    @Override
    public void sendRttMessageResponse(int token, int errorCode) {
        log("Send Rtt Message response received. errorCode: " + errorCode);
        mImsRadioResponse.onSendRttMessageResponse(token,
                StableAidlErrorCode.toErrorCode(errorCode));
    }

    @Override
    public void cancelModifyCallResponse(int token, int errorCode) {
        log("Cancel modify call response received.");
        mImsRadioResponse.onCancelModifyCallResponse(token,
                StableAidlErrorCode.toErrorCode(errorCode));
    }

    @Override
    public void sendSmsResponse(int token, SmsSendResponse smsResponse) {
        log("Ims sms response received");
        if(smsResponse.smsStatus == SmsSendStatus.INVALID ||
                smsResponse.reason == SmsSendFailureReason.INVALID) {
            log("Status or reason invalid.");
            return;
        }
        SmsResponse response = StableAidl.toSmsResponse(
                smsResponse.msgRef, smsResponse.smsStatus, smsResponse.reason,
                smsResponse.networkErrorCode, smsResponse.radioTech);
        mImsRadioResponse.onSendImsSmsResponse(token, response);
    }

    @Override
    public void registerMultiIdentityLinesResponse(int token, int errorCode) {
        log("registerMultiIdentityLines Response received. errorCode : " + errorCode);
        mImsRadioResponse.onRegisterMultiIdentityLinesResponse(token,
                StableAidlErrorCode.toErrorCode(errorCode));
    }

    @Override
    public void queryVirtualLineInfoResponse(int token, String msisdn,
                                             String[] virtualLineInfo) {
        VirtualLineInfo virtualInfo = new VirtualLineInfo(msisdn,
                new ArrayList<String>(Arrays.asList(virtualLineInfo)));
        log("queryVirtualLineInfoResponse :: " + virtualInfo);
        mImsRadioResponse.onQueryVirtualLineInfoResponse(token, msisdn, virtualInfo);
    }

    @Override
    public void setCallForwardStatusResponse(int token, int errorCode,
            vendor.qti.hardware.radio.ims.CallForwardStatusInfo callForwardStatus) {
        log("Set call forward status response received");
        org.codeaurora.ims.CallForwardStatusInfo cfStatusInfo = StableAidl.
               toCallForwardStatusInfo(callForwardStatus);
        mImsRadioResponse.onSetCallForwardStatusResponse(token,
                StableAidlErrorCode.toErrorCode(errorCode), cfStatusInfo);
    }

    @Override
    public void sendUssdResponse(int token, int errorCode,
                                 SipErrorInfo errorDetails) {
        log("Send USSD response received.");
        ImsReasonInfo imsReasonInfo = StableAidl.toSipError(errorDetails);
        mImsRadioResponse.onSendUssdResponse(token,
                StableAidlErrorCode.toErrorCode(errorCode), imsReasonInfo);
    }

    @Override
    public void cancelPendingUssdResponse(int token, int errorCode,
                                          SipErrorInfo errorDetails) {
        log("Cancel pending USSD response received.");
        ImsReasonInfo imsReasonInfo = StableAidl.toSipError(errorDetails);
        mImsRadioResponse.onSendUssdResponse(token,
                StableAidlErrorCode.toErrorCode(errorCode), imsReasonInfo);
    }

    @Override
    public void sendSipDtmfResponse(int token, int errorCode) {
        log("Send sip dtmf response received");
        mImsRadioResponse.onSendSipDtmfResponse(token, StableAidlErrorCode.toErrorCode(errorCode));
    }

    @Override
    public void setMediaConfigurationResponse(int token, int errorCode) {
        log("Set media configuration response received");
        mImsRadioResponse.onSetMediaConfigurationResponse(
                token, StableAidlErrorCode.toErrorCode(errorCode));
    }

    @Override
    public void queryMultiSimVoiceCapabilityResponse(int token, int errorCode,
            int voiceCapability) {
        log("query multi sim voice capability.");
        mImsRadioResponse.onQueryMultiSimVoiceCapabilityResponse(token,
                StableAidlErrorCode.toErrorCode(errorCode),
                StableAidl.toMultiSimVoiceCapability(voiceCapability));
    }

    @Override
    public void exitSmsCallBackModeResponse(int token, int errorCode) {
        log("exit SCBM");
        mImsRadioResponse.exitSmsCallBackModeResponse(token,
                StableAidlErrorCode.toErrorCode(errorCode));
    }

    @Override
    public void sendVosSupportStatusResponse(int token, int errorCode) {
        log("Send VOS support status response received");
        mImsRadioResponse.onSendVosSupportStatusResponse(token,
                StableAidlErrorCode.toErrorCode(errorCode));
    }

    @Override
    public void sendVosActionInfoResponse(int token, int errorCode) {
        log("Send VOS action info response received");
        mImsRadioResponse.onSendVosActionInfoResponse(token,
                StableAidlErrorCode.toErrorCode(errorCode));
    }

    @Override
    public void setGlassesFree3dVideoCapabilityResponse(int token, int errorCode) {
        log("Set glasses free 3d video capability response received");
        mImsRadioResponse.onSetGlassesFree3dVideoCapabilityResponse(token,
                 StableAidlErrorCode.toErrorCode(errorCode));
    }

    @Override
    public void abortConferenceResponse(int token, int errorCode) {
        log("abort conference response received.");
        mImsRadioResponse.onAbortConferenceResponse(token,
                StableAidlErrorCode.toErrorCode(errorCode));
    }
}
