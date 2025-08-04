/*
 * Copyright (c) 2021-2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.AutoCallRejectionInfo;
import vendor.qti.hardware.radio.ims.AutoCallRejectionInfo2;
import vendor.qti.hardware.radio.ims.CallComposerAutoRejectionInfo;
import vendor.qti.hardware.radio.ims.CallComposerInfo;
import vendor.qti.hardware.radio.ims.CallInfo;
import vendor.qti.hardware.radio.ims.CallModifyInfo;
import vendor.qti.hardware.radio.ims.ConferenceInfo;
import vendor.qti.hardware.radio.ims.CiWlanNotificationInfo;
import vendor.qti.hardware.radio.ims.DtmfInfo;
import vendor.qti.hardware.radio.ims.EmergencyCallBackMode;
import vendor.qti.hardware.radio.ims.ErrorCode;
import vendor.qti.hardware.radio.ims.GeoLocationDataStatus;
import vendor.qti.hardware.radio.ims.HandoverInfo;
import vendor.qti.hardware.radio.ims.ImsSubConfigInfo;
import vendor.qti.hardware.radio.ims.IncomingSms;
import vendor.qti.hardware.radio.ims.MessageWaitingIndication;
import vendor.qti.hardware.radio.ims.MultiIdentityLineInfo;
import vendor.qti.hardware.radio.ims.MultiSimVoiceCapability;
import vendor.qti.hardware.radio.ims.ParticipantStatusInfo;
import vendor.qti.hardware.radio.ims.PreAlertingCallInfo;
import vendor.qti.hardware.radio.ims.RadioState;
import vendor.qti.hardware.radio.ims.RegistrationBlockStatusInfo;
import vendor.qti.hardware.radio.ims.RegistrationInfo;
import vendor.qti.hardware.radio.ims.ServiceStatusInfo;
import vendor.qti.hardware.radio.ims.SipErrorInfo;
import vendor.qti.hardware.radio.ims.SmsCallBackMode;
import vendor.qti.hardware.radio.ims.SmsSendStatusReport;
import vendor.qti.hardware.radio.ims.SrtpEncryptionInfo;
import vendor.qti.hardware.radio.ims.StkCcUnsolSsResult;
import vendor.qti.hardware.radio.ims.SuppServiceNotification;
import vendor.qti.hardware.radio.ims.SystemServiceDomain;
import vendor.qti.hardware.radio.ims.ToneOperation;
import vendor.qti.hardware.radio.ims.TtyInfo;
import vendor.qti.hardware.radio.ims.UssdModeType;
import vendor.qti.hardware.radio.ims.ViceInfo;
import vendor.qti.hardware.radio.ims.VoiceInfo;
import vendor.qti.hardware.radio.ims.VoWiFiCallQuality;

@VintfStability
interface IImsRadioIndication {
    /**
     * ImsQmiIF.UNSOL_RESPONSE_CALL_STATE_CHANGED
     *
     * @param callList List of calls and their details.
     *
     */
    oneway void onCallStateChanged(in CallInfo[] callList);

    /**
     * ImsQmiIF.UNSOL_CALL_RING
     *
     */
    oneway void onRing();

    /**
     * ImsQmiIF.UNSOL_RINGBACK_TONE
     *
     * @param tone Start or stop tone.
     *
     */
    oneway void onRingbackTone(in ToneOperation tone);

    /**
     * ImsQmiIF.UNSOL_RESPONSE_IMS_NETWORK_STATE_CHANGED
     *
     * @param registration IMS registration state.
     *
     */
    oneway void onRegistrationChanged(in RegistrationInfo registration);

    /**
     * ImsQmiIF.UNSOL_RESPONSE_HANDOVER
     *
     * @param handover Handover details containing type, source, target
     *         and extra information about handover.
     *
     */
    oneway void onHandover(in HandoverInfo handover);

    /**
     *
     * Response to ImsQmiIF.UNSOL_SRV_STATUS_UPDATE
     *
     * @param srvStatusList List of service statuses.
     *
     */
    oneway void onServiceStatusChanged(in ServiceStatusInfo[] srvStatusList);

    /**
     * ImsQmiIF.UNSOL_RADIO_STATE_CHANGED
     *
     * @param radioState Radio state.
     *
     */
    oneway void onRadioStateChanged(in RadioState radioState);

    /**
     * ImsQmiIF.UNSOL_EMERGENCY_CALLBACK_MODE_CHANGED
     *
     */
    oneway void onEmergencyCallBackModeChanged(in EmergencyCallBackMode mode);

    /**
     * ImsQmiIF.UNSOL_TTY_NOTIFICATION
     *
     * @param ttyNotify TTY info having TTY mode and additional info as bytes
     *
     */
    oneway void onTtyNotification(in TtyInfo ttyInfo);

    /**
     * ImsQmiIF.UNSOL_REFRESH_CONF_INFO
     *
     * @param conferenceInfo Conference info having conference uri and call state
     *
     */
    oneway void onRefreshConferenceInfo(in ConferenceInfo conferenceInfo);

    /**
     * ImsQmiIF.UNSOL_REFRESH_VICE_INFO
     *
     * @param viceInfo having VICE info URI
     *
     */
    oneway void onRefreshViceInfo(in ViceInfo viceInfo);

    /**
     * ImsQmiIF.UNSOL_MODIFY_CALL
     *
     * @param CallModifyInfo, call modify information
     */
    oneway void onModifyCall(in CallModifyInfo callModifyInfo);

    /**
     * ImsQmiIF.UNSOL_SUPP_SVC_NOTIFICATION
     *
     * @param suppServiceNotification details of the supplementary service notification.
     *
     */
    oneway void onSuppServiceNotification(in SuppServiceNotification suppServiceNotification);

    /**
     * ImsQmiIF.UNSOL_MWI
     *
     * @param messageWaitingIndication details of messages waiting
     *
     */
    oneway void onMessageWaiting(in MessageWaitingIndication messageWaitingIndication);

    /**
     * ImsQmiIF.UNSOL_REQUEST_GEOLOCATION
     *
     * @param Location coordinates for reverse-geocoding
     *
     */
    oneway void onGeolocationInfoRequested(in double lat, in double lon);

    /**
     * ImsQmiIF.UNSOL_IMS_SUB_CONFIG_CHANGED
     *
     * @param config IMS stack configuration on the modem.
     */
    oneway void onImsSubConfigChanged(in ImsSubConfigInfo config);

    /**
     * ImsQmiIF.UNSOL_PARTICIPANT_STATUS_INFO
     *
     * @param ParticipantStatusInfo Participant status information.
     */
    oneway void onParticipantStatusInfo(in ParticipantStatusInfo participantStatusInfo);

    /**
     * ImsQmiIF.UNSOL_RESPONSE_REGISTRATION_BLOCK_STATUS
     *
     * @param BlockStatusOnWwan Details of the registration block status on Wwan technology.
     * @param BlockStatusOnWlan Details of the registration block status on Wlan technology.
     *
     */
    oneway void onRegistrationBlockStatus(in RegistrationBlockStatusInfo blockStatusInfo);

    /**
     * ImsQmiIF.UNSOL_RESPONSE_RTT_MSG_RECEIVED
     *
     * @param message - RTT text message
     *
     */
    oneway void onRttMessageReceived(in String message);

    /**
     * ImsQmiIF.UNSOL_VOWIFI_CALL_QUALITY
     *
     * @param VoWiFiCallQuality VoWiFi call quality information
     */
    oneway void onVoWiFiCallQuality(in VoWiFiCallQuality voWiFiCallQualityInfo);

    /**
     * ImsQmiIF.UNSOL_ON_SS
     * Indicates when Supplementary service(SS) response is received when DIAL/USSD/SS is changed
     * to SS by call control.
     *
     * @param StkCcUnsolSsResult Details of SS request and response information.
     */
    oneway void onSupplementaryServiceIndication(in StkCcUnsolSsResult ss);

    /**
     *
     * MessageId.UNSOL_IMS_SMS_STATUS_REPORT
     * Indicates the status report of the sent message.
     *
     * @param smsStatusReport ImsSmsSendStatusReport as defined in types.hal
     *
     */
    oneway void onSmsSendStatusReport(in SmsSendStatusReport smsStatusReport);

    /**
     *
     * MessageId.UNSOL_IMS_SMS_INCOMING_SMS
     * Indicates the incoming sms over ims.
     *
     * @param imsSms ImsSmsMessage as defined in types.hal
     *
     */
    oneway void onIncomingSms(in IncomingSms imsSms);

    /**
     * MessageId.UNSOL_VOPS_CHANGED
     *
     * @param isVopsEnabled indicates if Vops is enabled
     */
    oneway void onVopsChanged(in boolean isVopsEnabled);


    /**
     * MessageId.UNSOL_AUTO_CALL_REJECTION_IND
     * Indicates the auto rejected incoming call and reason
     *
     * @param autoCallRejectionInfo
     * Deprecated use onIncomingCallAutoRejected2
     */
    oneway void onIncomingCallAutoRejected(in AutoCallRejectionInfo autoCallRejectionInfo);

    /**
     *
     * MessageId.UNSOL_VOICE_INFO
     * Sends updates for the RTT voice info which indicates whether there is speech or silence
     * from remote user
     *
     * @param voiceInfo VoiceInfo
     *
     */
    oneway void onVoiceInfoChanged(in VoiceInfo voiceInfo);

    /**
     * MessageId.UNSOL_MULTI_IDENTITY_REGISTRATION_STATUS_CHANGE
     *
     * @param info List of all the lines and their registation status
     *
     */
    oneway void onMultiIdentityRegistrationStatusChange(in MultiIdentityLineInfo[] info);

   /**
     * MessageId.ims_MsgId_UNSOL_MULTI_IDENTITY_INFO_PENDING
     *
     * Indication to ATEL that modem needs information about the
     * MultiIdentity Lines.
     *
     */
    oneway void onMultiIdentityInfoPending();

   /**
     * MessageId.UNSOL_MODEM_SUPPORTS_WFC_ROAMING_MODE
     * Indicates if modem supports WFC roaming mode configuration
     *
     * @param wfcRoamingConfigurationSupport
     */
    oneway void onModemSupportsWfcRoamingModeConfiguration(
            in boolean wfcRoamingConfigurationSupport);

    /**
     * MessageId.UNSOL_USSD_FAILED
     * Indicates if ussd message failed over IMS
     *
     * @param type
     * @param errorDetails
     */
    oneway void onUssdMessageFailed(in UssdModeType type, in SipErrorInfo errorDetails);

    /**
     * MessageId.UNSOL_USSD_RECEIVED
     *
     * @param type, indicates ussd mode type.
     * @param msg, message string in UTF-8, if applicable.
     * @param errorDetails, from the network.
     *
     */
    oneway void onUssdReceived(in UssdModeType type, in String msg, in SipErrorInfo errorDetails);

    /**
     * MessageId.UNSOL_CALL_COMPOSER_INFO_AVAILABLE
     *
     * @param callComposerInfo Collection of information related to pre-call. It
     *         only is populated and sent when pre-alerting state is received
     * Deprecated use onPreAlertingCallInfoAvailable
     *
     */
     oneway void onCallComposerInfoAvailable(in CallComposerInfo info);

     /**
      * MessageId.UNSOL_AUTO_CALL_COMPOSER_CALL_REJECTION_IND
      * Indicates the auto rejected incoming call and reason with call composer elements
      *
      * @param autoCallRejectionInfo
      * @param callComposerInfo
      * Deprecated use onIncomingCallAutoRejected2
      */
    oneway void onIncomingCallComposerCallAutoRejected(
        in CallComposerAutoRejectionInfo autoRejectionInfo);

    /**
     * MessageId.UNSOL_RETRIEVE_GEO_LOCATION_DATA_STATUS
     *
     * This indication will be received when modem needs location details to register over WiFi
     * and modem not able to get the location details due to GPS errors.
     *
     * @param GeoLocationDataStatus list of errors received to retrieve the geo location details.
     *
     */
    oneway void onRetrievingGeoLocationDataStatus(in GeoLocationDataStatus geoLocationDataStatus);

    /**
    * MessageId.UNSOL_SIP_DTMF_RECEIVED
    *
    * Indicate the SIP DTMF config string that parsed from xml by modem.
    * @param configCode, SIP DTMF string parsed by modem.
    *
    */
   oneway void onSipDtmfReceived(in String configCode);

    /**
     * MessageId.UNSOL_SERVICE_DOMAIN_CHANGED
     *
     * This indication will be received when modem sends NAS system info.
     *
     * @param domain indicates the UE's service domain.
     *
     */
    oneway void onServiceDomainChanged(in SystemServiceDomain domain);

    /**
     * MessageId.UNSOL_SCBM_UPDATE_IND
     *
     * This indication will be received when modem enters/leaves Sms Callback mode.
     * Modem enters SCBM for certain carriers after an emergency SMS is sent by user.
     *
     * @param mode indicates the state of modem whether its in SCBM or not.
     *
     */
    oneway void onSmsCallBackModeChanged(in SmsCallBackMode mode);

    /**
     *
     * MessageId.UNSOL_CONFERENCE_CALL_STATE_COMPLETED
     *
     */
    oneway void onConferenceCallStateCompleted();

    /**
     * MessageId.UNSOL_INCOMING_DTMF_START
     *
     * This indication will be received when modem sends incoming dtmf start event
     * after call is established.
     *
     * @param dtmfInfo DTMF tones received.
     *
     */
    oneway void onIncomingDtmfStart(in DtmfInfo dtmfInfo);

    /**
     * MessageId.UNSOL_INCOMING_DTMF_STOP
     *
     * This indication will be received when modem sends incoming dtmf stop event
     * after call is established.
     *
     * @param dtmfInfo DTMF tones received.
     *
     */
    oneway void onIncomingDtmfStop(in DtmfInfo dtmfInfo);

    /**
     * MessageId.UNSOL_MULTI_SIM_VOICE_CAPABILITY_CHANGED
     *
     * This indication will be received when modem changes the multi sim voice capability based
     * on RAT where IMS is registered.
     *
     */
    oneway void onMultiSimVoiceCapabilityChanged(in MultiSimVoiceCapability voiceCapability);

    /**
     * MessageId.UNSOL_PRE_ALERTING_CALL_INFO_AVAILABLE
     *
     * @param info Collection of information related to pre-call. It
     *         only is populated and sent when pre-alerting state is received
     *
     */
    oneway void onPreAlertingCallInfoAvailable(in PreAlertingCallInfo info);

    /**
     * MessageId.UNSOL_INCOMING_CALL_AUTO_REJECTED
     * Indicates the auto rejected incoming call and reason with call composer and eCnam elements
     *
     * @param autoCallRejectionInfo consits of AutoCallRejectionInfo, CallComposerInfo and
     *        EcnamInfo.
     *
     */
    oneway void onIncomingCallAutoRejected2(in AutoCallRejectionInfo2 autoCallRejectionInfo);

    /**
     * MessageId.UNSOL_C_IWLAN_NOTIFICATION
     * Indicates type of C_IWLAN notification
     *
     * @param type indicates type of notification
     *
     */
    oneway void onCiWlanNotification(in CiWlanNotificationInfo type);

    /**
     * MessageId.UNSOL_SRTP_ENCRYPTION_STATUS_CHANGED
     * Indicates type of SRTP encryption category.
     *
     * @param info consits of callId, categories.
     *
     */
    oneway void onSrtpEncryptionStatusChanged(in SrtpEncryptionInfo info);
}
