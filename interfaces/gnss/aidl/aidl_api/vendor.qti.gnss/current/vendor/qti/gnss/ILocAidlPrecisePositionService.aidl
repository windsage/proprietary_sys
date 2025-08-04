/*
*  Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
*  All rights reserved.
*  Confidential and Proprietary - Qualcomm Technologies, Inc.
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

package vendor.qti.gnss;
@VintfStability
interface ILocAidlPrecisePositionService {
  int setCallback(in vendor.qti.gnss.ILocAidlPrecisePositionServiceCallback callback);
  void startSession(in int id, in long tbfMsec, in vendor.qti.gnss.ILocAidlPrecisePositionService.LocAidlPrecisePositionType preciseType, in vendor.qti.gnss.ILocAidlPrecisePositionService.LocAidlPrecisePositionCorrectionType correctionType);
  void stopSession(in int id);
  @Backing(type="int") @VintfStability
  enum LocAidlPrecisePositionCorrectionType {
    TYPE_DEFAULT = 1,
    TYPE_RTCM = 2,
    TYPE_3GPP = 3,
  }
  @Backing(type="int") @VintfStability
  enum LocAidlPrecisePositionType {
    UNKNOWN = 0,
    MLP = 1,
    DLP = 2,
    MLP_WOCS = 3,
  }
}
