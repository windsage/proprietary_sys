/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/
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

package vendor.qti.hardware.data.iwlandata;
@VintfStability
interface IIWlan {
  oneway void deactivateDataCall(in int serial, in int cid, in android.hardware.radio.data.DataRequestReason reason);
  oneway void getAllQualifiedNetworks(in int serial);
  oneway void getDataCallList(in int serial);
  oneway void getDataRegistrationState(in int serial);
  oneway void iwlanDisabled();
  oneway void responseAcknowledgement();
  void setResponseFunctions(in vendor.qti.hardware.data.iwlandata.IIWlanResponse dataResponse, in vendor.qti.hardware.data.iwlandata.IIWlanIndication dataIndication);
  oneway void setupDataCall(in int serial, in android.hardware.radio.AccessNetwork accessNetwork, in android.hardware.radio.data.DataProfileInfo dataProfileInfo, in boolean roamingAllowed, in android.hardware.radio.data.DataRequestReason reason, in android.hardware.radio.data.LinkAddress[] addresses, in String[] dnses, in int pduSessionId, in @nullable android.hardware.radio.data.SliceInfo sliceInfo, in boolean matchAllRuleAllowed);
}
