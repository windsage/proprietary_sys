/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.CallForwardInfo;
import vendor.qti.hardware.radio.ims.CallForwardStatusInfo;
import vendor.qti.hardware.radio.ims.CallWaitingInfo;
import vendor.qti.hardware.radio.ims.ClipProvisionStatus;
import vendor.qti.hardware.radio.ims.ClirInfo;
import vendor.qti.hardware.radio.ims.ColrInfo;
import vendor.qti.hardware.radio.ims.ConfigInfo;
import vendor.qti.hardware.radio.ims.ErrorCode;
import vendor.qti.hardware.radio.ims.ImsSubConfigInfo;
import vendor.qti.hardware.radio.ims.MultiSimVoiceCapability;
import vendor.qti.hardware.radio.ims.RegistrationInfo;
import vendor.qti.hardware.radio.ims.ServiceClassStatus;
import vendor.qti.hardware.radio.ims.ServiceStatusInfo;
import vendor.qti.hardware.radio.ims.SipErrorInfo;
import vendor.qti.hardware.radio.ims.SmsSendResponse;
import vendor.qti.hardware.radio.ims.SuppServiceStatus;

@VintfStability
interface IImsRadioResponse {
    /**
     *
     * Response to ImsQmiIF.REQUEST_DIAL
     *
     * @param token to match request/response. Response must include same token as request.
     * @param errorCode - errorCode of type ImsQmiIF.Error returned from RIL.
     *
     */
    oneway void dialResponse(in int token, in ErrorCode errorCode);

    /**
     *
     * Response to ImsQmiIF.REQUEST_ANSWER
     *
     * @param token Id to match request/response. Response must include same token.
     * @param errorCode - errorCode of type ImsQmiIF.Error returned from RIL.
     *
     */
    oneway void answerResponse(in int token, in ErrorCode errorCode);

    /**
     *
     * Response to ImsQmiIF.REQUEST_HANGUP
     *
     * @param token Id to match request/response. Response must include same token.
     * @param errorCode - errorCode of type ImsQmiIF.Error returned from RIL.
     *
     */
    oneway void hangupResponse(in int token, in ErrorCode errorCode);

    /**
     *
     * Response to ImsQmiIF.REQUEST_IMS_REG_STATE_CHANGE
     *
     * @param token Id to match request/response. Response must include same token.
     * @param errorCode - errorCode of type ImsQmiIF.Error returned from RIL.
     *
     */
    oneway void requestRegistrationChangeResponse(in int token, in ErrorCode errorCode);

    /**
     *
     * Response to ImsQmiIF.REQUEST_QUERY_SERVICE_STATUS
     *
     * @param token Id to match request/response. Response must include same token.
     * @param errorCode - errorCode of type ImsQmiIF.Error returned from RIL.
     * @param srvStatusList List of service statuses.
     *
     */
    oneway void queryServiceStatusResponse(in int token, in ErrorCode errorCode,
            in ServiceStatusInfo[] srvStatusList);

    /**
     *
     * Response to ImsQmiIF.REQUEST_SET_SERVICE_STATUS
     *
     * @param token Id to match request/response. Response must include same token.
     * @param errorCode - errorCode of type ImsQmiIF.Error returned from RIL.
     *
     */
    oneway void setServiceStatusResponse(in int token, in ErrorCode errorCode);

    /**
     *
     * Response to ImsQmiIF.REQUEST_HOLD
     *
     * @param token Id to match request/response. Response must include same token.
     * @param errorCode - errorCode of type ImsQmiIF.Error returned from RIL.
     * @param sipError Sip error from the network.
     *
     */
    oneway void holdResponse(in int token, in ErrorCode errorCode, in SipErrorInfo sipError);

    /**
     *
     * Response to ImsQmiIF.REQUEST_RESUME
     *
     * @param token Id to match request/response. Response must include same token.
     * @param errorCode - errorCode of type ImsQmiIF.Error returned from RIL.
     * @param sipError Sip error from the network.
     *
     */
    oneway void resumeResponse(in int token, in ErrorCode errorCode, in SipErrorInfo sipError);

    /**
     *
     * Response to ImsQmiIF.REQUEST_SET_IMS_CONFIG
     *
     * @param token Id to match request/response. Response must include same token.
     * @param errorCode - errorCode of type ImsQmiIF.Error returned from RIL.
     * @param config Config that was set. Contains item and value.
     *
     */
    oneway void setConfigResponse(in int token, in ErrorCode errorCode, in ConfigInfo config);

    /**
     *
     * Response to ImsQmiIF.REQUEST_GET_IMS_CONFIG
     *
     * @param token Id to match request/response. Response must include same token.
     * @param errorCode - errorCode of type ImsQmiIF.Error returned from RIL.
     * @param config Config that was requested. Contains item and value.
     *
     */
    oneway void getConfigResponse(in int token, in ErrorCode errorCode, in ConfigInfo config);

    /**
     *
     * Response to ImsQmiIF.REQUEST_GET_IMS_REGISTRATION_STATE
     *
     * @param token Id to match request/response. Response must include same token.
     * @param errorCode - errorCode of type ImsQmiIF.Error returned from RIL.
     * @param registration IMS registration state.
     *
     */
    oneway void getImsRegistrationStateResponse(in int token, in ErrorCode errorCode,
            in RegistrationInfo registration);

    /**
     *
     * Response to Supplementary service requests -
     *             ImsQmiIF.REQUEST_SET_CALL_FORWARD_STATUS
     *             ImsQmiIF.REQUEST_SET_CALL_WAITING
     *             ImsQmiIF.REQUEST_SUPP_SVC_STATUS
     *             ImsQmiIF.REQUEST_SET_COLR
     *
     * @param token Id to match request/response. Response must include same token.
     * @param errorCode - errorCode of type ImsQmiIF.Error returned from RIL.
     * @param suppServiceStatus supplementary service query status for call forwarding,
     * call waiting, etc. Has service class type enabled/disabled, facility type, error code, etc.
     *
     */
    oneway void suppServiceStatusResponse(in int token, in ErrorCode errorCode,
        in SuppServiceStatus suppServiceStatus);

    /**
     *
     * Response to ImsQmiIF.REQUEST_CONFERENCE
     *
     * @param token Id to match request/response. Response must include same token.
     * @param errorCode - errorCode of type ImsQmiIF.Error returned from RIL.
     * @param sipError Sip error from the network.
     *
     */
    oneway void conferenceResponse(in int token, in ErrorCode errorCode, in SipErrorInfo errorInfo);

   /**
     * Response to ImsQmiIF.REQUEST_QUERY_CLIP
     *
     * @param token Id to match request/response. Response must include same token.
     * @param errorCode - errorCode of type ImsQmiIF.Error returned from RIL.
     * @param clipProvisionStatus CLIP info having status and error details
     *
     */
    oneway void getClipResponse(in int token, in ErrorCode errorCode,
            in ClipProvisionStatus clipProvisionStatus);

    /**
     * Response to ImsQmiIF.REQUEST_GET_CLIR
     *
     * @param token Id to match request/response. Response must include same token.
     * @param errorCode - errorCode of type ImsQmiIF.Error returned from RIL.
     * @param clirInfo info
     * @param hasClirInfo - true or false if clirInfo has valid value or not
     *
     */
    oneway void getClirResponse(in int token, in ErrorCode errorCode, in ClirInfo clirInfo,
        in boolean hasClirInfo);

    /**
     * Response to ImsQmiIF.REQUEST_SET_CLIR
     *
     * @param token Id to match request/response. Response must include same token.
     * @param errorCode - errorCode of type ImsQmiIF.Error returned from RIL.
     *
     */
    oneway void setClirResponse(in int token, in ErrorCode errorCode);

    /**
     * Response to ImsQmiIF.REQUEST_QUERY_COLR
     *
     * @param token Id to match request/response. Response must include same token.
     * @param errorCode - errorCode of type ImsQmiIF.Error returned from RIL.
     * @param colrInfo COLR Info - IP presentation and error details
     *
     */
    oneway void getColrResponse(in int token, in ErrorCode errorCode, in ColrInfo colrInfo);

    /**
     * ImsQmiIF.REQUEST_EXIT_EMERGENCY_CALLBACK_MODE
     *
     * @param token Id to match request/response. Response must include same token.
     * @param errorCode - errorCode of type ImsQmiIF.Error returned from RIL.
     *
     */
    oneway void exitEmergencyCallbackModeResponse(in int token, in ErrorCode errorCode);

    /**
     * Response to ImsQmiIF.REQUEST_DTMF
     *
     * @param token Id to match request/response. Response must include same token.
     * @param errorCode - errorCode of type ImsQmiIF.Error returned from RIL.
     *
     */
    oneway void sendDtmfResponse(in int token, in ErrorCode errorCode);

    /**
     * Response to ImsQmiIF.REQUEST_DTMF_START
     *
     * @param token Id to match request/response. Response must include same token.
     * @param errorCode - errorCode of type ImsQmiIF.Error returned from RIL.
     *
     */
    oneway void startDtmfResponse(in int token, in ErrorCode errorCode);

    /**
     * Response to ImsQmiIF.REQUEST_DTMF_STOP
     *
     * @param token Id to match request/response. Response must include same token.
     * @param errorCode - errorCode of type ImsQmiIF.Error returned from RIL.
     *
     */
    oneway void stopDtmfResponse(in int token, in ErrorCode errorCode);

    /**
     * Response to ImsQmiIF.REQUEST_SEND_UI_TTY_MODE
     *
     * @param token Id to match request/response. Response must include same token.
     * @param errorCode - errorCode of type ImsQmiIF.Error returned from RIL
     *
     */
     oneway void setUiTTYModeResponse(in int token, in ErrorCode errorCode);

    /**
     * Response to ImsQmiIF.REQUEST_MODIFY_CALL_INITIATE
     *
     * @param token Id to match request/response. Response must include same token.
     * @param errorCode - errorCode of type ImsQmiIF.Error returned from RIL.
     *
     */
    oneway void modifyCallInitiateResponse(in int token, in ErrorCode errorCode);

    /**
     *
     * Response to ImsQmiIF.REQUEST_MODIFY_CALL_CONFIRM
     *
     * @param token Id to match request/response. Response must include same token.
     * @param errorCode - errorCode of type ImsQmiIF.Error returned from RIL.
     *
     */
    oneway void modifyCallConfirmResponse(in int token, in ErrorCode errorCode);

    /**
     *
     * Response to ImsQmiIF.REQUEST_QUERY_CALL_FORWARD_STATUS
     *
     * @param token Id to match request/response. Response must include same token.
     * @param errorCode - errorCode of type ImsQmiIF.Error returned from RIL.
     * @param callInfoForwardInfoList list of call forward status information for different
     *         service classes.
     * @param errorDetails Sip error information if any.
     *
     */
    oneway void queryCallForwardStatusResponse(in int token, in ErrorCode errorCode,
            in CallForwardInfo[] callForwardInfoList, in SipErrorInfo errorDetails);

    /**
     *
     * Response to ImsQmiIF.REQUEST_QUERY_CALL_WAITING
     *
     * @param token Id to match request/response. Response must include same token.
     * @param errorCode - errorCode of type ImsQmiIF.Error returned from RIL.
     * @param callWaitingInfo - parcelable containing parameters for call waiting
     * @param errorDetails Sip error information if any.
     *
     */
    oneway void getCallWaitingResponse(in int token, in ErrorCode errorCode,
            in CallWaitingInfo callWaitingInfo, in SipErrorInfo errorDetails);

    /**
     * Response to ImsQmiIF.REQUEST_EXPLICIT_CALL_TRANSFER
     *
     * @param token Id to match request/response. Response must include same token.
     * @param errorCode - errorCode of type ImsQmiIF.Error returned from RIL.
     * @param errorInfo Sip error information if any
     */
    oneway void explicitCallTransferResponse(in int token, in ErrorCode errorCode,
            in SipErrorInfo errorInfo);

    /**
     *
     * Response to ImsQmiIF.REQUEST_SET_SUPP_SVC_NOTIFICATION
     *
     * @param token Id to match request/response. Response must include same token.
     * @param errorCode - errorCode of type ImsQmiIF.Error returned from RIL.
     * @param status Service class status.
     *
     */
    oneway void setSuppServiceNotificationResponse(in int token, in ErrorCode errorCode,
            in ServiceClassStatus serviceStatus);

    /**
      * ImsQmiIF.REQUEST_GET_RTP_STATISTICS
      *
      * @param token Id to match request/response. Response must include same token.
      * @param errorCode - errorCode of type ImsQmiIF.Error returned from RIL.
      * @param packetCount total number of packets sent or received in sample duration.
      *
      */
    oneway void getRtpStatisticsResponse(in int token, in ErrorCode errorCode,
            in long packetCount);

    /**
     * ImsQmiIF.REQUEST_GET_RTP_ERROR_STATISTICS
     *
     * @param token Id to match request/response. Response must include same token.
     * @param errorCode - errorCode of type ImsQmiIF.Error returned from RIL.
     * @param packetErrorCount total number of packets errors encountered in sample duration.
     *
     */
    oneway void getRtpErrorStatisticsResponse(in int token, in ErrorCode errorCode,
            in long packetErrorCount);

    /**
     *
     * Response to ImsQmiIF.REQUEST_ADD_PARTICIPANT
     *
     * @param token to match request/response. Response must include same token as request.
     * @param errorCode - errorCode of type ImsQmiIF.Error returned from RIL.
     *
     */
    oneway void addParticipantResponse(in int token, in ErrorCode errorCode);

    /**
     * Response to ImsQmiIF.REQUEST_DEFLECT_CALL
     *
     * @param token Id to match request/response. Response must include same token.
     * @param errorCode - errorCode of type ImsQmiIF.Error returned from RIL.
     *
     */
    oneway void deflectCallResponse(in int token, in ErrorCode errorCode);

    /**
     * Response to ImsQmiIF.REQUEST_SEND_GEOLOCATION_INFO
     *
     * @param token Id to match request/response. Response must include same token.
     * @param errorCode - errorCode of type ImsQmiIF.Error returned from RIL.
     *
     */
    oneway void sendGeolocationInfoResponse(in int token, in ErrorCode errorCode);

    /**
     * Response to ImsQmiIF.REQUEST_GET_IMS_SUB_CONFIG
     *
     * @param token Id to match request/response. Response must include same token.
     * @param errorCode - errorCode of type ImsQmiIF.Error returned from RIL.
     * @param ImsSubConfigInfo, provided information of Ims stack static support
     *
     */
    oneway void getImsSubConfigResponse(in int token, in ErrorCode errorCode,
            in ImsSubConfigInfo subConfigInfo);

    /**
     * Response to ImsQmiIF.REQUEST_SEND_RTT_MSG
     *
     * @param token to match request/response. Response must include same token as request.
     * @param errorCode - errorCode of type ImsQmiIF.Error returned from RIL.
     *
     */
    oneway void sendRttMessageResponse(in int token, in ErrorCode errorCode);

    /**
     * Response to ImsQmiIF.REQUEST_CANCEL_MODIFY_CALL
     *
     * @param token Id to match request/response. Response must include same token.
     * @param errorCode - errorCode of type ImsQmiIF.Error returned from RIL.
     *
     */
    oneway void cancelModifyCallResponse(in int token, in ErrorCode errorCode);

    /**
     *
     * Response to MessageId.REQUEST_SEND_IMS_SMS.
     * Pass the result of the sent message.
     *
     * @param token Id to match request/response. Response must include same token.
     * @param msgeRef the message reference.
     * @param smsStatus status result of sending the sms. Valid values are defined
     * in types.hal.
     * @param reason reason in case status is failure. Valid values are defined in
     * types.hal.
     *
     */
    oneway void sendSmsResponse(in int token, in SmsSendResponse smsResponse);

    /**
     * Response to MessageId.REQUEST_REGISTER_MULTI_IDENTITY_LINES
     *
     * @param token Id to match request/response. Response must include same token.
     * @param errorCode - errorCode of type ImsErrorCode.Error returned from RIL.
     */
    oneway void registerMultiIdentityLinesResponse(in int token, in ErrorCode errorCode);

    /**
     * Response to MessageId.REQUEST_QUERY_VIRTUAL_LINE_INFO
     *
     * @param token Id to match request/response. Response must include same token.
     * @param msisdn - msisdn of the line for which info is being queried.
     * @param virtualLineInfo - the virtual line info of the msisdn
     */
    oneway void queryVirtualLineInfoResponse(in int token, in String msisdn,
            in String[] virtualLineInfo);

    /**
     *
     * Response to MessageId.REQUEST_SET_CALL_FORWARD_STATUS
     *
     * @param token Id to match request/response. Response must include same token.
     * @param errorCode - errorCode of type ImsErrorCode.Error returned from RIL.
     * @param CallForwardStatus - status and error details for all call forwarding requests
     *        (CFB, CFU, CFNRy, CFNRc, CFUT, CF_ALL and CF_ALL_CONDITIONAL)
     *
     */
     oneway void setCallForwardStatusResponse(in int token, in ErrorCode errorCode,
             in CallForwardStatusInfo callForwardStatus);

    /**
     * Response to MessageId.REQUEST_USSD
     *
     * @param token Id to match request/response. Response must include same token.
     * @param errorCode, RIL specific error code.
     * @param errorDetails, from the network.
     *
     */
    oneway void sendUssdResponse(in int token, in ErrorCode errorCode,
            in SipErrorInfo errorDetails);

    /**
     * Response to MessageId.REQUEST_CANCEL_USSD
     *
     * @param token Id to match request/response. Response must include same token.
     * @param errorCode, RIL specific error code.
     * @param errorDetails, from the network.
     *
     */
    oneway void cancelPendingUssdResponse(in int token, in ErrorCode errorCode,
            in SipErrorInfo errorDetails);


    /**
     * Response to MessageId.REQUEST_SIP_DTMF
     *
     * @param token Id to match request/response. Response must include same token.
     * @param errorCode, RIL specific error code.
     *
     */
    oneway void sendSipDtmfResponse(in int token, in ErrorCode errorCode);

    /**
     *
     * Response to MessageId.REQUEST_SET_MEDIA_CONFIG
     *
     * @param token Id to match request/response. Response must include same token.
     * @param errorCode, RIL specific error code.
     *
     */
    oneway void setMediaConfigurationResponse(in int token, in ErrorCode errorCode);

    /**
     * MessageId.REQUEST_GET_MULTI_SIM_VOICE_CAPABILITY
     *
     * @param token Id to match request/response. Response must include same token.
     * @param errorCode - errorCode of type ImsQmiIF.Error returned from RIL.
     * @param voiceCapability, provides multi sim voice capability information.
     *
     */
    oneway void queryMultiSimVoiceCapabilityResponse(in int token, in ErrorCode errorCode,
            in MultiSimVoiceCapability voiceCapability);

    /**
     * Response to MessageId.REQUEST_EXIT_SMS_CALLBACK_MODE
     *
     * @param token Id to match request/response. Response must include same token.
     * @param errorCode, RIL specific error code.
     *
     */
    oneway void exitSmsCallBackModeResponse(in int token, in ErrorCode errorCode);

    /**
     * Response to MessageId.REQUEST_SEND_VOS_SUPPORT_STATUS
     *
     * @param token Id to match request/response. Response must include same token.
     * @param errorCode, RIL specific error code.
     *
     */
    oneway void sendVosSupportStatusResponse(in int token, in ErrorCode errorCode);

    /**
     * Response to MessageId.REQUEST_SEND_VOS_ACTION_INFO
     *
     * @param token Id to match request/response. Response must include same token.
     * @param errorCode, RIL specific error code.
     *
     */
    oneway void sendVosActionInfoResponse(in int token, in ErrorCode errorCode);

     /**
     * Response to MessageId.REQUEST_ENABLE_GLASSES_FREE_3D_VIDEO
     *
     * @param token Id to match request/response. Response must include same token.
     * @param errorCode, RIL specific error code.
     *
     */
    oneway void setGlassesFree3dVideoCapabilityResponse(in int token, in ErrorCode errorCode);

     /**
     * Response to MessageId.REQUEST_ABORT_CONFERENCE
     *
     * @param token Id to match request/response. Response must include same token.
     * @param errorCode, RIL specific error code.
     *
     */
    oneway void abortConferenceResponse(in int token, in ErrorCode errorCode);
}
