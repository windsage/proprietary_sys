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
  INVALID,
  CFU,
  CF_BUSY,
  CF_NO_REPLY,
  CF_NOT_REACHABLE,
  CF_ALL,
  CF_ALL_CONDITIONAL,
  CFUT,
  CLIP,
  CLIR,
  COLP,
  COLR,
  CNAP,
  WAIT,
  BAOC,
  BAOIC,
  BAOIC_EXC_HOME,
  BAIC,
  BAIC_ROAMING,
  ALL_BARRING,
  OUTGOING_BARRING,
  INCOMING_BARRING,
  INCOMING_BARRING_DN,
  INCOMING_BARRING_ANONYMOUS,
}
