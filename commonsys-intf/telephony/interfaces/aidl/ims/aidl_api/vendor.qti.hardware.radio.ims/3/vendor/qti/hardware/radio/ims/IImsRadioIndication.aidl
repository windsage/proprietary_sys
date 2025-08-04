/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
///////////////////////////////////////////////////////////////////////////////
// THIS FILE IS IMMUTABLE. DO NOT EDIT IN ANY CASE.                          //
///////////////////////////////////////////////////////////////////////////////

// This file is a snapshot of an AIDL file. Do not edit it manually. There are
// two cases:
// 1). this is a frozen version file - do not edit this in any case.
// 2). this is a 'current' file. If you make a backwards compatible change to
//     the interface (from the latest frozen version), the build system will
//     prompt you to update this file with `m <name>-update-api`.
//
// You must not make a backward incompatible change to any AIDL file built
// with the aidl_interface module type with versions property set. The module
// type is used to build AIDL files in a way that they can be used across
// independently updatable components of the system. If a device is shipped
// with such a backward incompatible change, it has a high risk of breaking
// later when a module using the interface is updated, e.g., Mainline modules.

package vendor.qti.hardware.radio.ims;
@VintfStability
interface IImsRadioIndication {
  oneway void onCallStateChanged(in vendor.qti.hardware.radio.ims.CallInfo[] callList);
  oneway void onRing();
  oneway void onRingbackTone(in vendor.qti.hardware.radio.ims.ToneOperation tone);
  oneway void onRegistrationChanged(in vendor.qti.hardware.radio.ims.RegistrationInfo registration);
  oneway void onHandover(in vendor.qti.hardware.radio.ims.HandoverInfo handover);
  oneway void onServiceStatusChanged(in vendor.qti.hardware.radio.ims.ServiceStatusInfo[] srvStatusList);
  oneway void onRadioStateChanged(in vendor.qti.hardware.radio.ims.RadioState radioState);
  oneway void onEmergencyCallBackModeChanged(in vendor.qti.hardware.radio.ims.EmergencyCallBackMode mode);
  oneway void onTtyNotification(in vendor.qti.hardware.radio.ims.TtyInfo ttyInfo);
  oneway void onRefreshConferenceInfo(in vendor.qti.hardware.radio.ims.ConferenceInfo conferenceInfo);
  oneway void onRefreshViceInfo(in vendor.qti.hardware.radio.ims.ViceInfo viceInfo);
  oneway void onModifyCall(in vendor.qti.hardware.radio.ims.CallModifyInfo callModifyInfo);
  oneway void onSuppServiceNotification(in vendor.qti.hardware.radio.ims.SuppServiceNotification suppServiceNotification);
  oneway void onMessageWaiting(in vendor.qti.hardware.radio.ims.MessageWaitingIndication messageWaitingIndication);
  oneway void onGeolocationInfoRequested(in double lat, in double lon);
  oneway void onImsSubConfigChanged(in vendor.qti.hardware.radio.ims.ImsSubConfigInfo config);
  oneway void onParticipantStatusInfo(in vendor.qti.hardware.radio.ims.ParticipantStatusInfo participantStatusInfo);
  oneway void onRegistrationBlockStatus(in vendor.qti.hardware.radio.ims.RegistrationBlockStatusInfo blockStatusInfo);
  oneway void onRttMessageReceived(in String message);
  oneway void onVoWiFiCallQuality(in vendor.qti.hardware.radio.ims.VoWiFiCallQuality voWiFiCallQualityInfo);
  oneway void onSupplementaryServiceIndication(in vendor.qti.hardware.radio.ims.StkCcUnsolSsResult ss);
  oneway void onSmsSendStatusReport(in vendor.qti.hardware.radio.ims.SmsSendStatusReport smsStatusReport);
  oneway void onIncomingSms(in vendor.qti.hardware.radio.ims.IncomingSms imsSms);
  oneway void onVopsChanged(in boolean isVopsEnabled);
  oneway void onIncomingCallAutoRejected(in vendor.qti.hardware.radio.ims.AutoCallRejectionInfo autoCallRejectionInfo);
  oneway void onVoiceInfoChanged(in vendor.qti.hardware.radio.ims.VoiceInfo voiceInfo);
  oneway void onMultiIdentityRegistrationStatusChange(in vendor.qti.hardware.radio.ims.MultiIdentityLineInfo[] info);
  oneway void onMultiIdentityInfoPending();
  oneway void onModemSupportsWfcRoamingModeConfiguration(in boolean wfcRoamingConfigurationSupport);
  oneway void onUssdMessageFailed(in vendor.qti.hardware.radio.ims.UssdModeType type, in vendor.qti.hardware.radio.ims.SipErrorInfo errorDetails);
  oneway void onUssdReceived(in vendor.qti.hardware.radio.ims.UssdModeType type, in String msg, in vendor.qti.hardware.radio.ims.SipErrorInfo errorDetails);
  oneway void onCallComposerInfoAvailable(in vendor.qti.hardware.radio.ims.CallComposerInfo info);
  oneway void onIncomingCallComposerCallAutoRejected(in vendor.qti.hardware.radio.ims.CallComposerAutoRejectionInfo autoRejectionInfo);
  oneway void onRetrievingGeoLocationDataStatus(in vendor.qti.hardware.radio.ims.GeoLocationDataStatus geoLocationDataStatus);
  oneway void onSipDtmfReceived(in String configCode);
  oneway void onServiceDomainChanged(in vendor.qti.hardware.radio.ims.SystemServiceDomain domain);
  oneway void onSmsCallBackModeChanged(in vendor.qti.hardware.radio.ims.SmsCallBackMode mode);
  oneway void onConferenceCallStateCompleted();
  oneway void onIncomingDtmfStart(in vendor.qti.hardware.radio.ims.DtmfInfo dtmfInfo);
  oneway void onIncomingDtmfStop(in vendor.qti.hardware.radio.ims.DtmfInfo dtmfInfo);
  oneway void onMultiSimVoiceCapabilityChanged(in vendor.qti.hardware.radio.ims.MultiSimVoiceCapability voiceCapability);
}
