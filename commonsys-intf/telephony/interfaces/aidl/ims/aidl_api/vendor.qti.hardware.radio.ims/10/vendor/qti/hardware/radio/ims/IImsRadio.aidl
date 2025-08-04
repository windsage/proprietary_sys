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
interface IImsRadio {
  void setCallback(in vendor.qti.hardware.radio.ims.IImsRadioResponse imsRadioResponse, in vendor.qti.hardware.radio.ims.IImsRadioIndication imsRadioIndication);
  oneway void dial(in int token, in vendor.qti.hardware.radio.ims.DialRequest dialRequest);
  oneway void addParticipant(in int token, in vendor.qti.hardware.radio.ims.DialRequest dialRequest);
  oneway void getImsRegistrationState(in int token);
  oneway void answer(in int token, in vendor.qti.hardware.radio.ims.AnswerRequest answerRequest);
  oneway void hangup(in int token, in vendor.qti.hardware.radio.ims.HangupRequestInfo hangup);
  oneway void requestRegistrationChange(in int token, in vendor.qti.hardware.radio.ims.RegState state);
  oneway void queryServiceStatus(in int token);
  oneway void setServiceStatus(in int token, in vendor.qti.hardware.radio.ims.ServiceStatusInfo[] srvStatusInfoList);
  oneway void hold(in int token, in int callId);
  oneway void resume(in int token, in int callId);
  oneway void setConfig(in int token, in vendor.qti.hardware.radio.ims.ConfigInfo config);
  oneway void getConfig(in int token, in vendor.qti.hardware.radio.ims.ConfigInfo config);
  oneway void conference(in int token);
  oneway void getClip(in int token);
  oneway void getClir(in int token);
  oneway void setClir(in int token, in vendor.qti.hardware.radio.ims.ClirInfo clirInfo);
  oneway void getColr(in int token);
  oneway void setColr(in int token, in vendor.qti.hardware.radio.ims.ColrInfo colrInfo);
  oneway void exitEmergencyCallbackMode(in int token);
  oneway void sendDtmf(in int token, in vendor.qti.hardware.radio.ims.DtmfInfo dtmfInfo);
  oneway void startDtmf(in int token, in vendor.qti.hardware.radio.ims.DtmfInfo dtmfInfo);
  oneway void stopDtmf(in int token);
  oneway void setUiTtyMode(in int token, in vendor.qti.hardware.radio.ims.TtyInfo ttyInfo);
  oneway void modifyCallInitiate(in int token, in vendor.qti.hardware.radio.ims.CallModifyInfo callModifyInfo);
  oneway void modifyCallConfirm(in int token, in vendor.qti.hardware.radio.ims.CallModifyInfo callModifyInfo);
  oneway void queryCallForwardStatus(in int token, in vendor.qti.hardware.radio.ims.CallForwardInfo callForwardInfo);
  oneway void setCallForwardStatus(in int token, in vendor.qti.hardware.radio.ims.CallForwardInfo callForwardInfo);
  oneway void getCallWaiting(in int token, in int serviceClass);
  oneway void setCallWaiting(in int token, in vendor.qti.hardware.radio.ims.CallWaitingInfo callWaitingInfo);
  oneway void setSuppServiceNotification(in int token, in vendor.qti.hardware.radio.ims.ServiceClassStatus status);
  oneway void explicitCallTransfer(in int token, in vendor.qti.hardware.radio.ims.ExplicitCallTransferInfo ectInfo);
  oneway void suppServiceStatus(in int token, in vendor.qti.hardware.radio.ims.SuppServiceStatusRequest suppServiceStatusRequest);
  oneway void getRtpStatistics(in int token);
  oneway void getRtpErrorStatistics(in int token);
  oneway void deflectCall(in int token, in vendor.qti.hardware.radio.ims.DeflectRequestInfo deflectRequestInfo);
  oneway void sendGeolocationInfo(in int token, in vendor.qti.hardware.radio.ims.GeoLocationInfo geoLocationInfo);
  oneway void getImsSubConfig(in int token);
  oneway void sendRttMessage(in int token, in String message);
  oneway void cancelModifyCall(in int token, in int callId);
  oneway void sendSms(in int token, in vendor.qti.hardware.radio.ims.SmsSendRequest smsRequest);
  oneway void acknowledgeSms(int token, in vendor.qti.hardware.radio.ims.AcknowledgeSmsInfo smsInfo);
  oneway void acknowledgeSmsReport(in int token, in vendor.qti.hardware.radio.ims.AcknowledgeSmsReportInfo smsReportInfo);
  String getSmsFormat();
  oneway void registerMultiIdentityLines(in int token, in vendor.qti.hardware.radio.ims.MultiIdentityLineInfo[] info);
  oneway void queryVirtualLineInfo(in int token, in String msisdn);
  oneway void emergencyDial(in int token, in vendor.qti.hardware.radio.ims.EmergencyDialRequest dialRequest);
  oneway void sendUssd(in int token, in String ussd);
  oneway void cancelPendingUssd(in int token);
  oneway void callComposerDial(in int token, in vendor.qti.hardware.radio.ims.CallComposerDialRequest dialRequest);
  oneway void sendSipDtmf(in int token, in String requestCode);
  oneway void setMediaConfiguration(in int token, in vendor.qti.hardware.radio.ims.MediaConfig config);
  oneway void queryMultiSimVoiceCapability(in int token);
  oneway void exitSmsCallBackMode(in int token);
}
