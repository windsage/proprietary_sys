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

package vendor.qti.hardware.radio.qtiradio;
@VintfStability
interface IQtiRadioResponse {
  oneway void onNrIconTypeResponse(in int serial, in int errorCode, in vendor.qti.hardware.radio.qtiradio.NrIconType iconType);
  oneway void onEnableEndcResponse(in int serial, in int errorCode, in vendor.qti.hardware.radio.qtiradio.Status status);
  oneway void onEndcStatusResponse(in int serial, in int errorCode, in vendor.qti.hardware.radio.qtiradio.EndcStatus endcStatus);
  oneway void setNrConfigResponse(in int serial, in int errorCode, in vendor.qti.hardware.radio.qtiradio.Status status);
  oneway void onNrConfigResponse(in int serial, in int errorCode, in vendor.qti.hardware.radio.qtiradio.NrConfig config);
  oneway void getQtiRadioCapabilityResponse(in int serial, in int errorCode, in vendor.qti.hardware.radio.qtiradio.RadioAccessFamily raf);
  oneway void getCallForwardStatusResponse(in int serial, in int errorCode, in vendor.qti.hardware.radio.qtiradio.CallForwardInfo[] callForwardInfoList);
  oneway void getFacilityLockForAppResponse(in int serial, in int errorCode, in int response);
  oneway void getImeiResponse(in int serial, in int errorCode, in vendor.qti.hardware.radio.qtiradio.ImeiInfo info);
  oneway void getDdsSwitchCapabilityResponse(in int serial, in int errorCode, in boolean support);
  oneway void sendUserPreferenceForDataDuringVoiceCallResponse(in int serial, in int errorCode);
}
