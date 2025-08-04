/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
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

package vendor.qti.hardware.radio.lpa;
@Backing(type="int") @VintfStability
enum UimLpaUserEventId {
  UIM_LPA_UNKNOWN_EVENT_ID = 0,
  UIM_LPA_ADD_PROFILE = 1,
  UIM_LPA_ENABLE_PROFILE = 2,
  UIM_LPA_DISABLE_PROFILE = 3,
  UIM_LPA_DELETE_PROFILE = 4,
  UIM_LPA_EUICC_MEMORY_RESET = 5,
  UIM_LPA_GET_PROFILE = 6,
  UIM_LPA_UPDATE_NICKNAME = 7,
  UIM_LPA_GET_EID = 8,
  UIM_LPA_USER_CONSENT = 9,
  UIM_LPA_SRV_ADDR_OPERATION = 10,
  UIM_LPA_CONFIRM_CODE = 11,
  UIM_LPA_EUICC_INFO2 = 12,
}
