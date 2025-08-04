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
@Backing(type="int") @VintfStability
enum Codec {
  INVALID = 0,
  QCELP13K = 1,
  EVRC = 2,
  EVRC_B = 3,
  EVRC_WB = 4,
  EVRC_NW = 5,
  AMR_NB = 6,
  AMR_WB = 7,
  GSM_EFR = 8,
  GSM_FR = 9,
  GSM_HR = 10,
  G711U = 11,
  G723 = 12,
  G711A = 13,
  G722 = 14,
  G711AB = 15,
  G729 = 16,
  EVS_NB = 17,
  EVS_WB = 18,
  EVS_SWB = 19,
  EVS_FB = 20,
}
