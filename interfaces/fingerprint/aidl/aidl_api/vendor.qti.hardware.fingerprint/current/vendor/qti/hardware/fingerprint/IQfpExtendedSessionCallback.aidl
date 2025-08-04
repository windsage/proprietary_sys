/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
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

package vendor.qti.hardware.fingerprint;
@VintfStability
interface IQfpExtendedSessionCallback {
  void onCaptureImage(in int LUTUsed, in int currSystemLUT, in int numImages, in byte[] imagesData);
  void onCaptureDebugData(in vendor.qti.hardware.fingerprint.CaptureDebugData[] data);
  void onError(in int error);
  void onStatus(in int status, in byte[] extension);
  void onAirImageCapture(in int status, in int flags, in byte[] data);
  void onHrmCapture(in int info, in byte[] data);
  void onTieUntie(in int reqType, in byte[] data);
  void onEnumerateTieList(in byte[] fingeprintIds);
  void onQfsSetting(in int reqType, in byte[] data);
  void onForceSense(in int cmd, in int status, in int stage, in byte[] data);
  void onStylus(in byte[] data);
}
