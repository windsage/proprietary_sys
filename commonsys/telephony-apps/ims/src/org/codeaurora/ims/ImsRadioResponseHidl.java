/*
  * Copyright (c) 2015, 2017-2021 Qualcomm Technologies, Inc.
  * All Rights Reserved.
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

import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.stub.ImsSmsImplBase;

import com.qualcomm.ims.utils.Log;
import java.util.ArrayList;
import org.codeaurora.ims.sms.SmsResponse;
import vendor.qti.hardware.radio.ims.V1_0.*;

/* This class handles HIDL responses, converts objects from HIDL->Telephony java type
 * and forwards the response to ImsSenderRxr
 */

class ImsRadioResponseHidl extends
        vendor.qti.hardware.radio.ims.V1_9.IImsRadioResponse.Stub {

    private IImsRadioResponse mImsRadioResponse;

    public ImsRadioResponseHidl(IImsRadioResponse respCallback) {
        mImsRadioResponse = respCallback;
    }

    /**
     * Callback for getting the response to the DIAL request sent to the RIL via the
     * IImsRadio interface
     * @param token to match request/response. Response must include same token as request
     * @param errorCode of type ImsErrorCode.Error send back from RIL for the dial request
     *
     */
    @Override
    public void dialResponse(int token, int errorCode) {
        Log.i(this, "Dial response received");
        mImsRadioResponse.onDialResponse(token, errorCode);
    }

    /**
     * Callback for getting the response to the sendsms request sent to the RIL via the
     * IImsRadio interface
     * @param token to match request/response. Response must include same token as request
     * @param messageRef reference of the sent message.
     * @param smsStatusResult result of sending the sms.
     * @param reason reason in case status is failure.
     * @param networkErrorCode 3GPP error code from network.
     * @param transportErrorCode 3GPP2 transport layer error code.
     */
    @Override
    public void sendImsSmsResponse_1_8(int token, int messageRef, int smsStatusResult,
            int reason, int networkErrorCode, int transportErrorCode) {
        Log.i(this, "Ims sms response received");
        SmsResponse response = ImsRadioUtilsV18.imsSmsResponsefromHidl(messageRef,
                smsStatusResult, reason, networkErrorCode);
        mImsRadioResponse.onSendImsSmsResponse(token, response);
    }

    /**
     * Callback for getting the response to the sendsms request sent to the RIL via the
     * IImsRadio interface
     * @param token to match request/response. Response must include same token as request
     * @param messageRef reference of the sent message.
     * @param smsStatusResult to check status of the sent message..
     */
    @Override
    public void sendImsSmsResponse_1_5(int token, int messageRef, int smsStatusResult,
                                       int reason) {
        sendImsSmsResponse_1_8( token, messageRef, smsStatusResult, reason,
        ImsSmsImplBase.RESULT_NO_NETWORK_ERROR, ImsSmsImplBase.RESULT_NO_NETWORK_ERROR);
    }

    /**
     * Callback for getting the response to the sendsms request sent to the RIL via the
     * IImsRadio interface
     * @param token to match request/response. Response must include same token as request
     * @param messageRef reference of the sent message.
     * @param smsStatusResult to check status of the sent message..
     */
    @Override
    public void sendImsSmsResponse(int token, int messageRef, int smsStatusResult,
            int reason) {
        sendImsSmsResponse_1_5( token, messageRef, smsStatusResult, reason);
    }

    @Override
    public void setConfigResponse(int token, int errorCode, ConfigInfo config) {
        Log.i(this, "setConfigResponse()");
        setConfigResponse_1_6(token, errorCode,
                ImsRadioUtilsV16.migrateConfigInfoFrom(config));
    }

    @Override
    public void setConfigResponse_1_6(int token, int errorCode,
            vendor.qti.hardware.radio.ims.V1_6.ConfigInfo config) {
        Log.i(this, "setConfigResponse_1_6()");
        setConfigResponse_1_8(token, errorCode, ImsRadioUtilsV18.migrateConfigInfoFromV16(config));
    }

    @Override
    public void setConfigResponse_1_8(int token, int errorCode,
            vendor.qti.hardware.radio.ims.V1_8.ConfigInfo config) {
        Log.i(this, "setConfigResponse_1_8()");
        Object ret = processConfigResponse(config);
        mImsRadioResponse.onSetConfigResponse(token, errorCode, ret);
    }

    @Override
    public void getConfigResponse(int token, int errorCode, ConfigInfo config) {
        Log.i(this, "getConfigResponse()");
        vendor.qti.hardware.radio.ims.V1_6.ConfigInfo configV16 =
                ImsRadioUtilsV16.migrateConfigInfoFrom(config);
        getConfigResponse_1_8(token, errorCode,
                ImsRadioUtilsV18.migrateConfigInfoFromV16(configV16));
    }

    @Override
    public void getConfigResponse_1_8(int token, int errorCode,
            vendor.qti.hardware.radio.ims.V1_8.ConfigInfo config) {
        Log.i(this, "getConfigResponse_1_8()");
        Object ret = processConfigResponse(config);
        mImsRadioResponse.onGetConfigResponse(token, errorCode, ret);
    }

    private Object processConfigResponse(vendor.qti.hardware.radio.ims.V1_8.ConfigInfo configInfo) {
         if (configInfo.hasBoolValue) {
             return configInfo.boolValue;
         } else if (configInfo.intValue != Integer.MAX_VALUE) {
             return configInfo.intValue;
         } else if (configInfo.stringValue != null) {
             return configInfo.stringValue;
         } else if (configInfo.errorCause != ConfigFailureCause.CONFIG_FAILURE_INVALID) {
             return ImsRadioUtils.configFailureCauseFromHal(configInfo.errorCause);
         }
         return ImsRadioUtilsV18.configInfoFromHal(configInfo);
    }

    @Override
    public void queryServiceStatusResponse(int token, int errorCode,
            ArrayList<ServiceStatusInfo> serviceStatusInfoList) {
        Log.i(this, "queryServiceStatusResponse()");
        queryServiceStatusResponse_1_6(token, errorCode,
                ImsRadioUtilsV16.migrateServiceStatusInfo(serviceStatusInfoList));
    }

    @Override
    public void queryServiceStatusResponse_1_6(int token, int errorCode,
            ArrayList<vendor.qti.hardware.radio.ims.V1_6.ServiceStatusInfo> infoList) {
        Log.i(this, "queryServiceStatusResponse_1_6()");

        ArrayList<ServiceStatus> ret = ImsRadioUtils.handleSrvStatus(infoList);
        mImsRadioResponse.onQueryServiceStatusResponse(token, errorCode, ret);
    }

    @Override
    public void setServiceStatusResponse(int token, int errorCode) {
        Log.i(this, "SetServiceStatus response received.");
        mImsRadioResponse.onSetServiceStatusResponse(token, errorCode);
    }

    @Override
    public void resumeResponse(int token, int errorCode, SipErrorInfo errorInfo) {
        Log.i(this, "Resume response received.");
        ImsReasonInfo imsReasonInfo = ImsRadioUtils.sipErrorFromHal(errorInfo);
        mImsRadioResponse.onResumeResponse(token, errorCode, imsReasonInfo);
    }

    @Override
    public void holdResponse(int token, int errorCode, SipErrorInfo errorInfo) {
        Log.i(this, "Hold response received.");
        ImsReasonInfo imsReasonInfo = ImsRadioUtils.sipErrorFromHal(errorInfo);
        mImsRadioResponse.onHoldResponse(token, errorCode, imsReasonInfo);
    }

    @Override
    public void hangupResponse(int token, int errorCode) {
        Log.i(this, "Got hangup response from ImsRadio.");
        mImsRadioResponse.onHangupResponse(token, errorCode);
    }

    @Override
    public void answerResponse(int token, int errorCode) {
        Log.i(this, "Got answer response from ImsRadio.");
        mImsRadioResponse.onAnswerResponse(token, errorCode);
    }

    @Override
    public void requestRegistrationChangeResponse(int token, int errorCode) {
        Log.i(this, "Got registration change response from ImsRadio.");
        mImsRadioResponse.onRequestRegistrationChangeResponse(token, errorCode);
    }

    @Override
    public void getRegistrationResponse(int token, int errorCode,
                                        RegistrationInfo registration) {
        Log.i(this, "getRegistrationResponse()");
        getRegistrationResponse_1_6(token, errorCode,
                ImsRadioUtilsV16.migrateRegistrationInfo(registration));
    }

    @Override
    public void getRegistrationResponse_1_6(int token, int errorCode,
            vendor.qti.hardware.radio.ims.V1_6.RegistrationInfo registration) {
        Log.i(this, "getRegistrationResponse_1_6()");
        ImsRegistrationInfo regMessage = null;
        if (registration != null) {
            regMessage = ImsRadioUtils.registrationFromHal(registration);
        }
        mImsRadioResponse.onGetRegistrationResponse(token, errorCode, regMessage);
    }

    @Override
    public void suppServiceStatusResponse_1_3(int token, int errorCode,
            vendor.qti.hardware.radio.ims.V1_3.SuppServiceStatus suppServiceStatus) {
        Log.i(this, "suppServiceStatusResponse_1_3() " +
                " status:" + suppServiceStatus.status +
                " provisionStatus:" + suppServiceStatus.provisionStatus +
                " facilityType:" + suppServiceStatus.facilityType +
                " failureCause:" + suppServiceStatus.failureCause +
                " errorCode:" + suppServiceStatus.errorDetails.errorCode);

        SuppSvcResponse suppSvcResponse =
                ImsRadioUtils.suppSvcStatusResponseFromHal(suppServiceStatus);
        mImsRadioResponse.onSuppServiceStatusResponse(token, errorCode, suppSvcResponse);
    }

    @Override
    public void suppServiceStatusResponse(int token, int errorCode,
                                          SuppServiceStatus suppServiceStatus) {
        vendor.qti.hardware.radio.ims.V1_3.SuppServiceStatus status =
                ImsRadioUtilsV13.migrateSuppServiceStatusFromV10(suppServiceStatus);
        suppServiceStatusResponse_1_3(token, errorCode, status);
    }

    @Override
    public void conferenceResponse(int token, int errorCode, SipErrorInfo errorInfo) {
        Log.i(this, "conference response received.");
        ImsReasonInfo imsReasonInfo = ImsRadioUtils.sipErrorFromHal(errorInfo);
        mImsRadioResponse.onConferenceResponse(token, errorCode, imsReasonInfo);
    }

    @Override
    public void getClipResponse(int token, int errorCode,
                                ClipProvisionStatus clipProvisionStatus) {
        SuppService clipProvStatus = new SuppService();

        final int clipStatus = clipProvisionStatus.clipStatus;
        if (clipStatus != ClipStatus.INVALID) {
            clipProvStatus.setStatus(ImsRadioUtils.clipStatusFromHal(
                    clipProvisionStatus.clipStatus));
            Log.i(this, "getClipResponse from ImsRadio. Clipstatus " + clipProvisionStatus);
        }

        if (clipProvisionStatus.hasErrorDetails) {
            clipProvStatus.setErrorDetails(ImsRadioUtils.sipErrorFromHal(
                    clipProvisionStatus.errorDetails));
            Log.i(this, "getClipResponse from ImsRadio. Errorcode " +
                    clipProvisionStatus.errorDetails.errorCode + "String " +
                    clipProvisionStatus.errorDetails.errorString);
        }
        mImsRadioResponse.onGetClipResponse(token, errorCode, clipProvStatus);
    }

    @Override
    public void getClirResponse(int token, int errorCode, ClirInfo clirInfo,
                                boolean hasClirInfo) {
        int[] response = null;

        if (hasClirInfo) {
            response = new int[ImsCallUtils.CLIR_RESPONSE_LENGTH];
            if (clirInfo.paramN != Integer.MAX_VALUE) {
                response[0] = clirInfo.paramN;
            }
            if (clirInfo.paramM != Integer.MAX_VALUE) {
                response[1] = clirInfo.paramM;
            }
            Log.i(this, "getClirResponse from ImsRadio. param_m - " + clirInfo.paramM +
                    "param_n - " + clirInfo.paramN);
        }
        mImsRadioResponse.onGetClirResponse(token, errorCode, response);
    }

    @Override
    public void queryCallForwardStatusResponse(int token, int errorCode,
            ArrayList<vendor.qti.hardware.radio.ims.V1_0.CallForwardInfo>
            callForwardInfoList, SipErrorInfo errorDetails) {
        ImsCallForwardTimerInfo cfTimerInfo[] = ImsRadioUtils.
                buildCFStatusResponseFromHal(callForwardInfoList);
        mImsRadioResponse.onQueryCallForwardStatusResponse(token, errorCode, cfTimerInfo);
    }

    @Override
    public void getCallWaitingResponse(int token, int errorCode, int inServiceStatus,
            int serviceClass, SipErrorInfo errorDetails) {
        int[] response = null;

        if (inServiceStatus != ServiceClassStatus.INVALID) {
            int outServiceStatus = ImsRadioUtils.serviceClassStatusFromHal(inServiceStatus);
            if (outServiceStatus == SuppSvcResponse.DISABLED) {
                response = new int[1];
                response[0] = SuppSvcResponse.DISABLED;
            } else {
                response = new int[2];
                response[0] = SuppSvcResponse.ENABLED;
                response[1] = serviceClass;
            }
        }
        mImsRadioResponse.onGetCallWaitingResponse(token, errorCode, response);
    }

    @Override
    public void setClirResponse(int token, int errorCode) {
        Log.i(this, "Got setClirResponse from ImsRadio. error " + errorCode);
        mImsRadioResponse.onSetClirResponse(token, errorCode);
    }

    @Override
    public void getColrResponse_1_3(int token, int errorCode,
            vendor.qti.hardware.radio.ims.V1_3.ColrInfo colrInfo) {

        SuppService colrValue = new SuppService();

        colrValue.setServiceClassStatus(
                ImsRadioUtils.serviceClassStatusFromHal(colrInfo.status));
        colrValue.setProvisionStatus(ImsRadioUtils.serviceClassProvisionStatusFromHal
                (colrInfo.provisionStatus));
        colrValue.setStatus(ImsRadioUtils.serviceClassStatusFromHal(colrInfo.status));

        final int presentation = ImsRadioUtils.ipPresentationFromHal(colrInfo.presentation);
        if (presentation != IpPresentation.IP_PRESENTATION_INVALID) {
            colrValue.setPresentation(presentation);
            Log.i(this, "getColrResponse from ImsRadio. presentation " + presentation);
        }

        if (colrInfo.hasErrorDetails) {
            colrValue.setErrorDetails(ImsRadioUtils.sipErrorFromHal(colrInfo.errorDetails));
            Log.i(this, "getColrResponse from ImsRadio. errorcode " +
                    colrInfo.errorDetails.errorCode + "string " +
                    colrInfo.errorDetails.errorString);
        }
        mImsRadioResponse.onGetColrResponse(token, errorCode, colrValue);
    }

    @Override
    public void getColrResponse(int token, int errorCode, ColrInfo colrInfo) {
        vendor.qti.hardware.radio.ims.V1_3.ColrInfo ci =
                ImsRadioUtilsV13.migrateColrInfoFromV10(colrInfo);
        getColrResponse_1_3(token, errorCode, ci);
    }

    @Override
    public void exitEmergencyCallbackModeResponse(int token, int errorCode) {
        Log.i(this, "Got exitEmergencyCallbackModeResponse from ImsRadio error "
                + errorCode);
        mImsRadioResponse.onExitEmergencyCallbackModeResponse(token, errorCode);
    }

    @Override
    public void sendDtmfResponse(int token, int errorCode) {
        Log.i(this, "Got sendDtmfResponse from ImsRadio error " + errorCode);
        mImsRadioResponse.onSendDtmfResponse(token, errorCode);
    }

    @Override
    public void startDtmfResponse(int token, int errorCode) {
        Log.i(this, "Got startDtmfResponse from ImsRadio error " + errorCode);
        mImsRadioResponse.onStartDtmfResponse(token, errorCode);
    }

    @Override
    public void stopDtmfResponse(int token, int errorCode) {
        Log.i(this, "Got stopDtmfResponse from ImsRadio error " + errorCode);
        mImsRadioResponse.onStopDtmfResponse(token, errorCode);
    }

    @Override
    public void setUiTTYModeResponse(int token, int errorCode) {
        Log.i(this, "Got setUiTTYModeResponse from ImsRadio error " + errorCode);
        mImsRadioResponse.onSetUiTTYModeResponse(token, errorCode);
    }

    @Override
    public void modifyCallInitiateResponse(int token, int errorCode) {
        Log.i(this, "Got modify call initiate response from ImsRadio.");
        mImsRadioResponse.onModifyCallInitiateResponse(token, errorCode);
    }

    @Override
    public void cancelModifyCallResponse(int token, int errorCode) {
        Log.i(this, "Got cancel modify call response from ImsRadio.");
        mImsRadioResponse.onCancelModifyCallResponse(token, errorCode);
    }

    @Override
    public void modifyCallConfirmResponse(int token, int errorCode) {
        Log.i(this, "Got modify call confirm response from ImsRadio.");
        mImsRadioResponse.onModifyCallConfirmResponse(token, errorCode);
    }

    @Override
    public void explicitCallTransferResponse(int token, int errorCode) {
        Log.i(this, "explicitCallTransferResponse()");
        mImsRadioResponse.onExplicitCallTransferResponse(token, errorCode, null /*SipErrorInfo*/);
    }

    @Override
    public void explicitCallTransferResponse_1_8(int token, int errorCode, SipErrorInfo errorInfo) {
        Log.i(this, "explicitCallTransferResponse_1_8()");
        //TODO
    }

    @Override
    public void setSuppServiceNotificationResponse(int token, int errorCode,
                                                   int serviceStatusClass) {
        Log.i(this, "Got set supp service notification response from ImsRadio.");
        mImsRadioResponse.onSetSuppServiceNotificationResponse(token, errorCode,
                serviceStatusClass);
    }

    @Override
    public void getRtpStatisticsResponse(int token, int errorCode,
                                         long packetCount) {
        Log.i(this, "Got getRtpStatisticsResponse from ImsRadio packetCount = " +
                packetCount + " error " + errorCode);
        mImsRadioResponse.onGetRtpStatisticsResponse(token, errorCode, packetCount);
    }

    @Override
    public void getRtpErrorStatisticsResponse(int token, int errorCode,
                                              long packetErrorCount) {
        Log.i(this, "Got getRtpErrorStatisticsResponse from ImsRadio packetErrorCount = " +
                packetErrorCount + " error " + errorCode);
        mImsRadioResponse.onGetRtpErrorStatisticsResponse(token, errorCode, packetErrorCount);
    }

    @Override
    public void addParticipantResponse(int token, int errorCode) {
        // TODO: Map proto error codes to IImsRadio error codes to be used by the interface.
        // Change usage of errors of type ImsErrorCode.Error to some proprietary error code
        // and return that to clients.
        Log.i(this, "Add Participant response received. errorCode: " + errorCode);
        mImsRadioResponse.onAddParticipantResponse(token, errorCode);
    }

    @Override
    public void deflectCallResponse(int token, int errorCode) {
        Log.i(this, "Got deflect call response from ImsRadio.");
        mImsRadioResponse.onDeflectCallResponse(token,errorCode);
    }

    @Override
    public void sendGeolocationInfoResponse(int token, int errorCode) {
        Log.i(this, "Received GeoLocationInfo response from ImsRadio.");
        mImsRadioResponse.onSendGeolocationInfoResponse(token, errorCode);
    }

    @Override
    public void getImsSubConfigResponse(int token, int errorCode,
                                        ImsSubConfigInfo subConfigInfo) {
        Log.i(this, "Received Subconfig response from ImsRadio.");
        ImsSubConfigDetails subConfigDetails = ImsRadioUtils.imsSubconfigFromHal(subConfigInfo);
        mImsRadioResponse.onGetImsSubConfigResponse(token, errorCode, subConfigDetails);
    }

    /**
     * Callback for getting the response to the USSD request sent to the RIL via the
     * IImsRadio interface
     * @param token to match request/response. Response must include same token as request
     * @param errorCode of type ImsErrorCode.Error send back from RIL for the ussd request
     * @param errorDetails related to SIP messaging from the network.
     *
     */
    @Override
    public void sendUssdResponse(int token, int errorCode, SipErrorInfo errorInfo) {
        Log.i(this, "Send USSD response received.");
        ImsReasonInfo imsReasonInfo = ImsRadioUtils.sipErrorFromHal(errorInfo);
        mImsRadioResponse.onSendUssdResponse(token, errorCode, imsReasonInfo);
    }

    @Override
    public void sendSipDtmfResponse(int token, int errorCode) {
        Log.i(this, "Received Send SIP DTMF response from ImsRadio.");
        mImsRadioResponse.onSendSipDtmfResponse(token, errorCode);
    }

    /**
     * Callback for getting the response to the cancel pending USSD request sent to
     * the RIL via the IImsRadio interface
     * @param token to match request/response. Response must include same token as request
     * @param errorCode of type ImsErrorCode.Error send back from RIL for the ussd request
     * @param errorDetails related to SIP messaging from the network.
     *
     */
    @Override
    public void cancelPendingUssdResponse(int token, int errorCode,
                                          SipErrorInfo errorInfo) {
        Log.i(this, "Cancel pending USSD response received.");
        ImsReasonInfo imsReasonInfo = ImsRadioUtils.sipErrorFromHal(errorInfo);
        mImsRadioResponse.onSendUssdResponse(token, errorCode, imsReasonInfo);
    }

    @Override
    public void sendRttMessageResponse(int token, int errorCode) {
        Log.i(this, "Send Rtt Message response received. errorCode: " + errorCode);
        mImsRadioResponse.onSendRttMessageResponse(token, errorCode);
    }

    @Override
    public void registerMultiIdentityLinesResponse(int token, int errorCode) {
        Log.i(this, "registerMultiIdentityLines Response received. errorCode : "
                + errorCode);
        mImsRadioResponse.onRegisterMultiIdentityLinesResponse(token, errorCode);
    }

    @Override
    public void queryVirtualLineInfoResponse(int token, String msisdn,
            ArrayList<String> virtualLineInfo) {
        VirtualLineInfo virtualInfo = new VirtualLineInfo(msisdn, virtualLineInfo);
        Log.i(this, "queryVirtualLineInfoResponse :: " + virtualInfo);
        mImsRadioResponse.onQueryVirtualLineInfoResponse(token, msisdn, virtualInfo);
    }

    @Override
    public void setCallForwardStatusResponse(int token, int errorCode,
            vendor.qti.hardware.radio.ims.V1_5.CallForwardStatusInfo callForwardStatusInfo) {
        Log.i(this, "set call forward response received.");
        CallForwardStatusInfo cfStatusInfo = ImsRadioUtilsV15.callForwardStatusInfoFromHal(
                callForwardStatusInfo);
        mImsRadioResponse.onSetCallForwardStatusResponse(token, errorCode, cfStatusInfo);
    }

    @Override
    public void sendVosSupportStatusResponse(int token, int errorCode) {
        Log.i(this, "Send VOS support status response received");
        mImsRadioResponse.onSendVosSupportStatusResponse(token, errorCode);
    }

    @Override
    public void sendVosActionInfoResponse(int token, int errorCode) {
        Log.i(this, "Send VOS action info response received");
        mImsRadioResponse.onSendVosActionInfoResponse(token, errorCode);
    }
}
