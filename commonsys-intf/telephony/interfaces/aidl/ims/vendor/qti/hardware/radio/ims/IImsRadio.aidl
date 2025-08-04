/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.AddressInfo;
import vendor.qti.hardware.radio.ims.AnswerRequest;
import vendor.qti.hardware.radio.ims.CallComposerDialRequest;
import vendor.qti.hardware.radio.ims.CallForwardInfo;
import vendor.qti.hardware.radio.ims.CallModifyInfo;
import vendor.qti.hardware.radio.ims.CallWaitingInfo;
import vendor.qti.hardware.radio.ims.CallType;
import vendor.qti.hardware.radio.ims.ClirInfo;
import vendor.qti.hardware.radio.ims.ColrInfo;
import vendor.qti.hardware.radio.ims.ConfigInfo;
import vendor.qti.hardware.radio.ims.DeflectRequestInfo;
import vendor.qti.hardware.radio.ims.DialRequest;
import vendor.qti.hardware.radio.ims.DtmfInfo;
import vendor.qti.hardware.radio.ims.EmergencyCallRoute;
import vendor.qti.hardware.radio.ims.EmergencyDialRequest;
import vendor.qti.hardware.radio.ims.ExplicitCallTransferInfo;
import vendor.qti.hardware.radio.ims.FacilityType;
import vendor.qti.hardware.radio.ims.GeoLocationInfo;
import vendor.qti.hardware.radio.ims.HangupRequestInfo;
import vendor.qti.hardware.radio.ims.IImsRadioIndication;
import vendor.qti.hardware.radio.ims.IImsRadioResponse;
import vendor.qti.hardware.radio.ims.AcknowledgeSmsInfo;
import vendor.qti.hardware.radio.ims.AcknowledgeSmsReportInfo;
import vendor.qti.hardware.radio.ims.IpPresentation;
import vendor.qti.hardware.radio.ims.MediaConfig;
import vendor.qti.hardware.radio.ims.MultiIdentityLineInfo;
import vendor.qti.hardware.radio.ims.RegState;
import vendor.qti.hardware.radio.ims.RttMode;
import vendor.qti.hardware.radio.ims.ServiceClassStatus;
import vendor.qti.hardware.radio.ims.ServiceStatusInfo;
import vendor.qti.hardware.radio.ims.SmsSendRequest;
import vendor.qti.hardware.radio.ims.SuppServiceStatusRequest;
import vendor.qti.hardware.radio.ims.TtyInfo;
import vendor.qti.hardware.radio.ims.VosActionInfo;
import vendor.qti.hardware.radio.ims.ConferenceAbortReasonInfo;

@VintfStability
interface IImsRadio {

    /**
     * Set callback that has response functions for the requests send from ImsService
     *
     * @param imsRadioResponse Object containing the call backs for requests like DIAL, etc.
     * @param imsRadioIndication Object containing the call backs for UNSOLs
     */
    void setCallback(in IImsRadioResponse imsRadioResponse,
        in IImsRadioIndication imsRadioIndication);

    /**
     * MessageId.REQUEST_DIAL
     *
     * @param token to match request/response. Responses must include the same token as requests.
     * @param DailRequest - the parcelable containing the dial request params like address, clir
     * call details, etc.
     *
     */
    oneway void dial(in int token, in DialRequest dialRequest);

    /**
     * MessageId.REQUEST_ADD_PARTICIPANT
     *
     * @param token to match request/response. Responses must include the same token as requests.
     * @param DailRequest - the parcelable containing the dial request params like address, clir
     * call details, etc.
     *
     */
    oneway void addParticipant(in int token, in DialRequest dialRequest);

    /**
     * ImsQmiIF.REQUEST_IMS_REGISTRATION_STATE
     *
     * @param token Id to match request/response. Response must include same token.
     *
     */
    oneway void getImsRegistrationState(in int token);

    /**
     * ImsQmiIF.REQUEST_ANSWER
     *
     * @param token Id to match request/response. Response must include same token.
     * @param answerRequest - the parcelable containing the answer request params like callType,
     * IpPresentation, Rtt Mode
     *
     */
    oneway void answer(in int token, in AnswerRequest answerRequest);

    /**
     * MessageId.REQUEST_HANGUP
     *
     * @param token Id to match request/response. Response must include same token.
     * @param hangup Payload for the hangup request. It consists of connection index,
     *               is_multiparty, connetion URI, conference Id, fail cause.
     *
     */
    oneway void hangup(in int token, in HangupRequestInfo hangup);

    /**
     * ImsQmiIF.REQUEST_IMS_REG_STATE_CHANGE
     *
     * @param token Id to match request/response. Response must include same token.
     * @param state New registration state requested.
     *
     */
    oneway void requestRegistrationChange(in int token, in RegState state);

    /**
     * ImsQmiIF.REQUEST_IMS_QUERY_SERVICE_STATUS
     *
     * @param token Id to match request/response. Response must include same token.
     *
     */
    oneway void queryServiceStatus(in int token);

    /**
     * MessageId.REQUEST_IMS_SET_SERVICE_STATUS
     *
     * @param token Id to match request/response. Response must include same token.
     * @param srvStatusInfo List of service statuses
     *
     */
    oneway void setServiceStatus(in int token, in ServiceStatusInfo[] srvStatusInfoList);

    /**
     * ImsQmiIF.REQUEST_HOLD
     *
     * @param token Id to match request/response. Response must include same token.
     * @param callId Id of the call to be held.
     *
     */
    oneway void hold(in int token, in int callId);

    /**
     * ImsQmiIF.REQUEST_RESUME
     *
     * @param token Id to match request/response. Response must include same token.
     * @param callId Id of the call to be resumed.
     *
     */
    oneway void resume(in int token, in int callId);

    /**
     * MessageId.REQUEST_SET_IMS_CONFIG
     *
     * @param token ID to match request/response. Response must include same token.
     * @param config Config to be set. Contains item and value.
     *
     */
    oneway void setConfig(in int token, in ConfigInfo config);

    /**
     * ImsQmiIF.REQUEST_GET_IMS_CONFIG
     *
     * @param token Id to match request/response. Response must include same token.
     * @param config Config requested. Contains item and value.
     *
     */
    oneway void getConfig(in int token, in ConfigInfo config);

    /**
     * ImsQmiIF.REQUEST_CONFERENCE
     *
     * @param token Id to match request/response. Response must include same token.
     *
     */
    oneway void conference(in int token);

    /**
     * ImsQmiIF.REQUEST_QUERY_CLIP
     *
     * @param token Id to match request/response. Response must include same token.
     *
     */
    oneway void getClip(in int token);

    /**
     * ImsQmiIF.REQUEST_QUERY_CLIR
     *
     * @param token Id to match request/response. Response must include same token.
     *
     */
    oneway void getClir(in int token);

    /**
     * ImsQmiIF.REQUEST_SET_CLIR
     *
     * @param token Id to match request/response. Response must include same token.
     * @param clirInfo CLIR info to set
     *
     */
    oneway void setClir(in int token, in ClirInfo clirInfo);

    /**
     * ImsQmiIF.REQUEST_GET_COLR
     *
     * @param token Id to match request/response. Response must include same token.
     *
     */
    oneway void getColr(in int token);

    /**
     * MessageId.REQUEST_SET_COLR
     *
     * @param token Id to match request/response. Response must include same token.
     * @param colrInfo COLR Info - IP presentation and error details to be set
     *
     */
    oneway void setColr(in int token, in ColrInfo colrInfo);

    /**
     * ImsQmiIF.REQUEST_EXIT_EMERGENCY_CALLBACK_MODE
     *
     * @param token Id to match request/response. Response must include same token.
     *
     */
    oneway void exitEmergencyCallbackMode(in int token);

    /**
     * ImsQmiIF.REQUEST_DTMF
     *
     * @param token Id to match request/response. Response must include same token.
     * @param dtmfInfo DTMF tones to send
     *
     */
    oneway void sendDtmf(in int token, in DtmfInfo dtmfInfo);

    /**
     * ImsQmiIF.REQUEST_DTMF_START
     *
     * @param token Id to match request/response. Response must include same token.
     * @param dtmfInfo DTMF tones to send
     *
     */
    oneway void startDtmf(in int token, in DtmfInfo dtmfInfo);

    /**
     * ImsQmiIF.REQUEST_DTMF_STOP
     *
     * @param token Id to match request/response. Response must include same token.
     *
     */
    oneway void stopDtmf(in int token);

    /**
     * ImsQmiIF.REQUEST_SEND_UI_TTY_MODE
     *
     * @param token Id to match request/response. Response must include same token.
     * @param ttyInfo TTY info to set having TTY mode and additional info as bytes
     *
     */
    oneway void setUiTtyMode(in int token, in TtyInfo ttyInfo);

    /**
     * ImsQmiIF.REQUEST_MODIFY_CALL_INITIATE
     *
     * @param token Id to match request/response. Response must include same token.
     * @param CallModifyInfo new call modify type requested
     */
    oneway void modifyCallInitiate(in int token, in CallModifyInfo callModifyInfo);

    /**
     * ImsQmiIF.REQUEST_MODIFY_CALL_CONFIRM
     *
     * @param token Id to match request/response. Response must include same token.
     * @param CallModifyInfo new call modify type confirmed
     */
    oneway void modifyCallConfirm(in int token, in CallModifyInfo callModifyInfo);

    /**
     * ImsQmiIF.REQUEST_QUERY_CALL_FORWARD_STATUS
     *
     * @param token Id to match request/response. Response must include same token.
     * @param callForwardInfo Call forward query details.
     *
     */
    oneway void queryCallForwardStatus(in int token, in CallForwardInfo callForwardInfo);

    /**
     * ImsQmiIF.REQUEST_SET_CALL_FORWARD_STATUS
     *
     * @param token Id to match request/response. Response must include same token.
     * @param callForwardInfo Set call forward request details.
     *
     */
    oneway void setCallForwardStatus(in int token, in CallForwardInfo callForwardInfo);

    /**
     * ImsQmiIF.REQUEST_QUERY_CALL_WAITING
     *
     * @param token Id to match request/response. Response must include same token.
     * @param serviceClass serviceClass on which the call waiting is set.
     *
     */
    oneway void getCallWaiting(in int token, in int serviceClass);

    /**
     * ImsQmiIF.REQUEST_SET_CALL_WAITING
     *
     * @param token Id to match request/response. Response must include same token.
     * @param callWaitingInfo - the parcelable containing info such as service class on which
     * the call waiting is set and service class status
     *
     */
    oneway void setCallWaiting(in int token, in CallWaitingInfo callWaitingInfo);

    /**
     * ImsQmiIF.REQUEST_SET_SUPP_SVC_NOTIFICATION
     *
     * @param token Id to match request/response. Response must include same token.
     * @param status Service class status.
     *
     */
    oneway void setSuppServiceNotification(in int token, in ServiceClassStatus status);

    /**
     * ImsQmiIF.REQUEST_EXPLICIT_CALL_TRANSFER
     *
     * @param token Id to match request/response. Response must include same token.
     * @param ectInfo Payload for the call transfer request.
     *
     */
    oneway void explicitCallTransfer(in int token, in ExplicitCallTransferInfo ectInfo);

    /**
     * MessageId.REQUEST_SUPP_SVC_STATUS
     *
     * @param token Id to match request/response. Response must include same token.
     * @param suppServiceStatusRequest - parcelable containing following supp service request info:
     * operationType, call barring operation [@link SuppSvcResponse.ACTIVATE/DEACTIVATE]
     * facilityType, call barring operation [@Link SuppSvcResponse.FACILITY_*]
     * cbNumListInfo, this will have ICB Number list and Service class information.
     * password, Password to activate/deactivate the call barring.
     */
    oneway void suppServiceStatus(in int token,
            in SuppServiceStatusRequest suppServiceStatusRequest);

    /**
     * ImsQmiIF.REQUEST_GET_RTP_STATISTICS
     *
     * @param token Id to match request/response. Response must include same token.
     *
     */
    oneway void getRtpStatistics(in int token);

    /**
     * ImsQmiIF.REQUEST_GET_RTP_ERROR_STATISTICS
     *
     * @param token Id to match request/response. Response must include same token.
     *
     */
    oneway void getRtpErrorStatistics(in int token);

    /**
     * ImsQmiIF.REQUEST_DEFLECT_CALL
     *
     * @param token Id to match request/response. Response must include same token.
     * @param deflectRequestInfo Payload for the call deflect request.
     *
     */
    oneway void deflectCall(in int token, in DeflectRequestInfo deflectRequestInfo);

    /**
     * MessageId.REQUEST_SEND_GEOLOCATION_INFO
     *
     * @param token to match request/response. Responses must include the same token as requests.
     * @param geoLocationInfo - the parcelable containing:
     * Latitude location coordinate
     * Longitude location coordinate
     * addesssInfo - the parcelable containing the reverse-geocoded address information.
     *
     */
    oneway void sendGeolocationInfo(in int token, in GeoLocationInfo geoLocationInfo);

    /**
     * ImsQmiIF.REQUEST_GET_IMS_SUB_CONFIG
     *
     * @param token Id to match request/response. Response must include same token.
     *
     */
    oneway void getImsSubConfig(in int token);

    /**
     * ImsQmiIF.REQUEST_SEND_RTT_MSG
     *
     * @param token Id to match request/response. Response must include same token.
     * @param  message Rtt text message
     *
     */
    oneway void sendRttMessage(in int token, in String message);

    /**
     * ImsQmiIF.REQUEST_CANCEL_MODIFY_CALL
     *
     * @param token Id to match request/response. Response must include same token.
     * @param callId Id of the call to be cancelled modify.
     *
     */
    oneway void cancelModifyCall(in int token, in int callId);

    /**
     *
     * MessageId.REQUEST_SEND_IMS_SMS
     *
     * @param token Id to match request/response. Response must include same token.
     * @param smsRequest - the parcelable containing the SmsSendRequest information
     *
     * Response function is IImsRadioResponse.SmsSendResponse()
     *
     */
    oneway void sendSms(in int token, in SmsSendRequest smsRequest);

     /**
     * MessageId.REQUEST_ACKNOWLEDGE_IMS_SMS
     *
     * @param token Id to match request/response. Response must include same token.
     * @param smsResult - the parcelable containing message reference and SMS deliver status.
     *
     */
    oneway void acknowledgeSms(int token, in AcknowledgeSmsInfo smsInfo);

     /**
     * MessageId.REQUEST_ACKNOWLEDGE_IMS_SMS_REPORT
     *
     * @param token Id to match request/response. Response must include same token.
     * @param smsReportResult - the parcelable containing message reference and sms status
     * report result.
     *
     */
    oneway void acknowledgeSmsReport(in int token, in AcknowledgeSmsReportInfo smsReportInfo);

     /**
     *
     * MessageId.REQUEST_IMS_SMS_FORMAT
     *
     * @return the format of the message. Valid values are {SmsMessage#FORMAT_3GPP} and
     * {SmsMessage#FORMAT_3GPP2}.
     *
     */
    String getSmsFormat();

    /**
     * MessageId.REQUEST_REGISTER_MULTI_IDENTITY_LINES
     *
     * @param token Id to match request/response. Response must include same token.
     * @param info List of MultiIdentityLineInfo.
     */
    oneway void registerMultiIdentityLines(in int token, in MultiIdentityLineInfo[] info);

    /**
     * MessageId.REQUEST_QUERY_VIRTUAL_LINE_INFO
     *
     * @param token Id to match request/response. Response must include same token.
     * @param msisdn - msisdn of the line for which the virtual line info is being queried.
     */
    oneway void queryVirtualLineInfo(in int token, in String msisdn);

    /**
     * MessageId.REQUEST_EMERGENCY_DIAL
     *
     * Initiate emergency voice call, with zero or more emergency service category(s), zero or
     * more emergency Uniform Resource Names (URN), and routing information for handling the call.
     * IMS uses this request to make its emergency call instead of using IImsRadio.dial
     * if the 'address' in the 'dialRequest' field is identified as an emergency number by Android.
     *
     * Some countries or carriers require some emergency numbers that must be handled with normal
     * call routing or emergency routing. If the 'routing' field is specified as
     * EmergencyCallRoute#NORMAL, the implementation must use normal call routing to
     * handle the call; if it is specified as EmergencyNumberRoute#EMERGENCY, the
     * implementation must use emergency routing to handle the call; if it is
     * EmergencyNumberRoute#UNKNOWN, Android does not know how to handle the call.
     *
     * If the dialed emergency number does not have a specified emergency service category, the
     * 'categories' field is set to EmergencyServiceCategory#UNSPECIFIED; if the dialed
     * emergency number does not have specified emergency Uniform Resource Names, the 'urns' field
     * is set to an empty list. If the underlying technology used to request emergency services
     * does not support the emergency service category or emergency uniform resource names, the
     * field 'categories' or 'urns' may be ignored.
     *
     * If 'isTesting' is true, this request is for testing purpose, and must not be sent to a real
     * emergency service; otherwise it's for a real emergency call request.
     *
     * Reference: 3gpp 22.101, Section 10 - Emergency Calls;
     *            3gpp 23.167, Section 6 - Functional description;
     *            3gpp 24.503, Section 5.1.6.8.1 - General;
     *            RFC 5031
     *
     * @param token to match request/response. Responses must include the same token as requests.
     * @param dialRequest - the parcelable containing the dial request params like address, clir
     *     call details, etc. and emergency parameters such as:
     * categories - the Emergency Service Category(s) of the call.
     * urns - the emergency Uniform Resource Names (URN)
     * route - the emergency call routing information.
     * hasKnownUserIntentEmergency - indicate if user's intent for the emergency call
     *     is known.
     * isTesting - to represent real or test emergency call.
     *
     * Response function is IImsRadioResponse.dialResponse()
     *
     */
    oneway void emergencyDial(in int token, in EmergencyDialRequest dialRequest);

    /**
     * MessageId.REQUEST_USSD
     *
     * @param token Id to match request/response. Response must include same token.
     * @param ussd, string containing the USSD request.
     *
     */
    oneway void sendUssd(in int token, in String ussd);

    /**
     * MessageId.REQUEST_CANCEL_USSD
     *
     * @param token Id to match request/response. Response must include same token.
     *
     */
    oneway void cancelPendingUssd(in int token);

    /**
     *
     * MessageId.REQUEST_CALL_COMPOSER_DIAL
     *
     * @param token Id to match request/response. Response must include same token.
     * @param dialRequest - the struct containing the dial request params like address,
     *     clir, call details, etc. and pre-call (aka call composer) information,
     *     including subject, image, location, and priority.
     *
     * Response function is IImsRadioResponse.dialResponse()
     *
     */
    oneway void callComposerDial(in int token, in CallComposerDialRequest dialRequest);

    /**
     * MessageId.REQUEST_SIP_DTMF
     *
     * Sends a SIP DTMF string to Modem, Modem will pack it into xml and send to n/w.
     * @param token Id to match request/response. Response must include same token.
     * @param requestCode, string containing the sip dtmf request, for example “1*001#" .
     *
     */
    oneway void sendSipDtmf(in int token, in String requestCode);

    /**
     * MessageId.REQUEST_SET_MEDIA_CONFIG
     *
     * @param token ID to match request/response. Response must include same token.
     * @param config, Config to be set. Contains items and values.
     *
     */
    oneway void setMediaConfiguration(in int token, in MediaConfig config);

    /**
     * MessageId.REQUEST_QUERY_MULTI_SIM_VOICE_CAPABILITY
     *
     * @param token Id to match request/response. Response must include same token.
     *
     */
    oneway void queryMultiSimVoiceCapability(in int token);

    /**
     * MessageId.REQUEST_EXIT_SMS_CALLBACK_MODE
     *
     * @param token Id to match request/response. Response must include same token.
     *
     */
    oneway void exitSmsCallBackMode(in int token);

    /**
     * MessageId.REQUEST_SEND_VOS_SUPPORT_STATUS
     *
     * Sends isVosSupported to Modem, Modem will pack it into xml and send to n/w.
     * @param token Id to match request/response. Response must include same token.
     * @param isVosSupported informs device supports video online service.
     *
     */
    oneway void sendVosSupportStatus(in int token, in boolean isVosSupported);

    /**
     * MessageId.REQUEST_SEND_VOS_ACTION_INFO
     *
     * Sends vosActionInfo object to Modem, Modem will pack it into xml and send to n/w.
     * @param token Id to match request/response. Response must include same token.
     * @param vosActionInfo - the struct containing the vos action request params like
     *     vosMoveInfo, vosTouchInfo.
     *
     */
    oneway void sendVosActionInfo(in int token, in VosActionInfo vosActionInfo);

    /**
     * MessageId.REQUEST_ENABLE_GLASSES_FREE_3D_VIDEO
     *
     * Sends enable glasses free 3d video request to Modem.
     * @param token Id to match request/response. Response must include same token.
     * @param enableGlassesFree3dVideo informs device supports glasses free 3d video,
     * modem will use this flag to negotiate with network during MO call setup.
     *
     */
    oneway void setGlassesFree3dVideoCapability(in int token, in boolean enableGlassesFree3dVideo);

    /**
     * MessageId.REQUEST_ABORT_CONFERENCE
     *
     * Sends abort conference request to modem.
     * @param token Id to match request/response. Response must include same token.
     * @param abortReason informs modem about the reason for abort.
     *
     */
    oneway void abortConference(in int token, in ConferenceAbortReasonInfo abortReason);
}
