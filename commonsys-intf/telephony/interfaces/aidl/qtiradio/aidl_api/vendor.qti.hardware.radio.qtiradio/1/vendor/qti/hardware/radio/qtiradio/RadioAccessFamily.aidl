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
@Backing(type="int") @VintfStability
enum RadioAccessFamily {
  UNKNOWN = 1,
  GPRS = 2,
  EDGE = 4,
  UMTS = 8,
  IS95A = 16,
  IS95B = 32,
  ONE_X_RTT = 64,
  EVDO_0 = 128,
  EVDO_A = 256,
  HSDPA = 512,
  HSUPA = 1024,
  HSPA = 2048,
  EVDO_B = 4096,
  EHRPD = 8192,
  LTE = 16384,
  HSPAP = 32768,
  GSM = 65536,
  TD_SCDMA = 131072,
  LTE_CA = 524288,
  NR_NSA = 1048576,
  NR_SA = 2097152,
}
