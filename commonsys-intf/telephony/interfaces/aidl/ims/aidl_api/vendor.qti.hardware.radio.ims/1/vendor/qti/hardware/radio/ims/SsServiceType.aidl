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
enum SsServiceType {
  INVALID = 0,
  CFU = 1,
  CF_BUSY = 2,
  CF_NO_REPLY = 3,
  CF_NOT_REACHABLE = 4,
  CF_ALL = 5,
  CF_ALL_CONDITIONAL = 6,
  CFUT = 7,
  CLIP = 8,
  CLIR = 9,
  COLP = 10,
  COLR = 11,
  CNAP = 12,
  WAIT = 13,
  BAOC = 14,
  BAOIC = 15,
  BAOIC_EXC_HOME = 16,
  BAIC = 17,
  BAIC_ROAMING = 18,
  ALL_BARRING = 19,
  OUTGOING_BARRING = 20,
  INCOMING_BARRING = 21,
  INCOMING_BARRING_DN = 22,
  INCOMING_BARRING_ANONYMOUS = 23,
}
