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
interface IImsRadioResponse {
  oneway void dialResponse(in int token, in vendor.qti.hardware.radio.ims.ErrorCode errorCode);
  oneway void answerResponse(in int token, in vendor.qti.hardware.radio.ims.ErrorCode errorCode);
  oneway void hangupResponse(in int token, in vendor.qti.hardware.radio.ims.ErrorCode errorCode);
  oneway void requestRegistrationChangeResponse(in int token, in vendor.qti.hardware.radio.ims.ErrorCode errorCode);
  oneway void queryServiceStatusResponse(in int token, in vendor.qti.hardware.radio.ims.ErrorCode errorCode, in vendor.qti.hardware.radio.ims.ServiceStatusInfo[] srvStatusList);
  oneway void setServiceStatusResponse(in int token, in vendor.qti.hardware.radio.ims.ErrorCode errorCode);
  oneway void holdResponse(in int token, in vendor.qti.hardware.radio.ims.ErrorCode errorCode, in vendor.qti.hardware.radio.ims.SipErrorInfo sipError);
  oneway void resumeResponse(in int token, in vendor.qti.hardware.radio.ims.ErrorCode errorCode, in vendor.qti.hardware.radio.ims.SipErrorInfo sipError);
  oneway void setConfigResponse(in int token, in vendor.qti.hardware.radio.ims.ErrorCode errorCode, in vendor.qti.hardware.radio.ims.ConfigInfo config);
  oneway void getConfigResponse(in int token, in vendor.qti.hardware.radio.ims.ErrorCode errorCode, in vendor.qti.hardware.radio.ims.ConfigInfo config);
  oneway void getImsRegistrationStateResponse(in int token, in vendor.qti.hardware.radio.ims.ErrorCode errorCode, in vendor.qti.hardware.radio.ims.RegistrationInfo registration);
  oneway void suppServiceStatusResponse(in int token, in vendor.qti.hardware.radio.ims.ErrorCode errorCode, in vendor.qti.hardware.radio.ims.SuppServiceStatus suppServiceStatus);
  oneway void conferenceResponse(in int token, in vendor.qti.hardware.radio.ims.ErrorCode errorCode, in vendor.qti.hardware.radio.ims.SipErrorInfo errorInfo);
  oneway void getClipResponse(in int token, in vendor.qti.hardware.radio.ims.ErrorCode errorCode, in vendor.qti.hardware.radio.ims.ClipProvisionStatus clipProvisionStatus);
  oneway void getClirResponse(in int token, in vendor.qti.hardware.radio.ims.ErrorCode errorCode, in vendor.qti.hardware.radio.ims.ClirInfo clirInfo, in boolean hasClirInfo);
  oneway void setClirResponse(in int token, in vendor.qti.hardware.radio.ims.ErrorCode errorCode);
  oneway void getColrResponse(in int token, in vendor.qti.hardware.radio.ims.ErrorCode errorCode, in vendor.qti.hardware.radio.ims.ColrInfo colrInfo);
  oneway void exitEmergencyCallbackModeResponse(in int token, in vendor.qti.hardware.radio.ims.ErrorCode errorCode);
  oneway void sendDtmfResponse(in int token, in vendor.qti.hardware.radio.ims.ErrorCode errorCode);
  oneway void startDtmfResponse(in int token, in vendor.qti.hardware.radio.ims.ErrorCode errorCode);
  oneway void stopDtmfResponse(in int token, in vendor.qti.hardware.radio.ims.ErrorCode errorCode);
  oneway void setUiTTYModeResponse(in int token, in vendor.qti.hardware.radio.ims.ErrorCode errorCode);
  oneway void modifyCallInitiateResponse(in int token, in vendor.qti.hardware.radio.ims.ErrorCode errorCode);
  oneway void modifyCallConfirmResponse(in int token, in vendor.qti.hardware.radio.ims.ErrorCode errorCode);
  oneway void queryCallForwardStatusResponse(in int token, in vendor.qti.hardware.radio.ims.ErrorCode errorCode, in vendor.qti.hardware.radio.ims.CallForwardInfo[] callForwardInfoList, in vendor.qti.hardware.radio.ims.SipErrorInfo errorDetails);
  oneway void getCallWaitingResponse(in int token, in vendor.qti.hardware.radio.ims.ErrorCode errorCode, in vendor.qti.hardware.radio.ims.CallWaitingInfo callWaitingInfo, in vendor.qti.hardware.radio.ims.SipErrorInfo errorDetails);
  oneway void explicitCallTransferResponse(in int token, in vendor.qti.hardware.radio.ims.ErrorCode errorCode, in vendor.qti.hardware.radio.ims.SipErrorInfo errorInfo);
  oneway void setSuppServiceNotificationResponse(in int token, in vendor.qti.hardware.radio.ims.ErrorCode errorCode, in vendor.qti.hardware.radio.ims.ServiceClassStatus serviceStatus);
  oneway void getRtpStatisticsResponse(in int token, in vendor.qti.hardware.radio.ims.ErrorCode errorCode, in long packetCount);
  oneway void getRtpErrorStatisticsResponse(in int token, in vendor.qti.hardware.radio.ims.ErrorCode errorCode, in long packetErrorCount);
  oneway void addParticipantResponse(in int token, in vendor.qti.hardware.radio.ims.ErrorCode errorCode);
  oneway void deflectCallResponse(in int token, in vendor.qti.hardware.radio.ims.ErrorCode errorCode);
  oneway void sendGeolocationInfoResponse(in int token, in vendor.qti.hardware.radio.ims.ErrorCode errorCode);
  oneway void getImsSubConfigResponse(in int token, in vendor.qti.hardware.radio.ims.ErrorCode errorCode, in vendor.qti.hardware.radio.ims.ImsSubConfigInfo subConfigInfo);
  oneway void sendRttMessageResponse(in int token, in vendor.qti.hardware.radio.ims.ErrorCode errorCode);
  oneway void cancelModifyCallResponse(in int token, in vendor.qti.hardware.radio.ims.ErrorCode errorCode);
  oneway void sendSmsResponse(in int token, in vendor.qti.hardware.radio.ims.SmsSendResponse smsResponse);
  oneway void registerMultiIdentityLinesResponse(in int token, in vendor.qti.hardware.radio.ims.ErrorCode errorCode);
  oneway void queryVirtualLineInfoResponse(in int token, in String msisdn, in String[] virtualLineInfo);
  oneway void setCallForwardStatusResponse(in int token, in vendor.qti.hardware.radio.ims.ErrorCode errorCode, in vendor.qti.hardware.radio.ims.CallForwardStatusInfo callForwardStatus);
  oneway void sendUssdResponse(in int token, in vendor.qti.hardware.radio.ims.ErrorCode errorCode, in vendor.qti.hardware.radio.ims.SipErrorInfo errorDetails);
  oneway void cancelPendingUssdResponse(in int token, in vendor.qti.hardware.radio.ims.ErrorCode errorCode, in vendor.qti.hardware.radio.ims.SipErrorInfo errorDetails);
  oneway void sendSipDtmfResponse(in int token, in vendor.qti.hardware.radio.ims.ErrorCode errorCode);
  oneway void setMediaConfigurationResponse(in int token, in vendor.qti.hardware.radio.ims.ErrorCode errorCode);
}
