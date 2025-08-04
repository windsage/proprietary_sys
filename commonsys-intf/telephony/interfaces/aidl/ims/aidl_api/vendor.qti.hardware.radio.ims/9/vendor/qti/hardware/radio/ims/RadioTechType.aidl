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
enum RadioTechType {
  INVALID = 0,
  ANY = 1,
  UNKNOWN = 2,
  GPRS = 3,
  EDGE = 4,
  UMTS = 5,
  IS95A = 6,
  IS95B = 7,
  RTT_1X = 8,
  EVDO_0 = 9,
  EVDO_A = 10,
  HSDPA = 11,
  HSUPA = 12,
  HSPA = 13,
  EVDO_B = 14,
  EHRPD = 15,
  LTE = 16,
  HSPAP = 17,
  GSM = 18,
  TD_SCDMA = 19,
  WIFI = 20,
  IWLAN = 21,
  NR5G = 22,
  C_IWLAN = 23,
}
