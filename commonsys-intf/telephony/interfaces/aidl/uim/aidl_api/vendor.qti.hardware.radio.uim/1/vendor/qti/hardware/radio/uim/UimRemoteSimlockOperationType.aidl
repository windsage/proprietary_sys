/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
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

package vendor.qti.hardware.radio.uim;
@Backing(type="int") @VintfStability
enum UimRemoteSimlockOperationType {
  UIM_REMOTE_SIMLOCK_GENERATE_ENCRYPTED_KEY = 0,
  UIM_REMOTE_SIMLOCK_PROCESS_SIMLOCK_DATA = 1,
  UIM_REMOTE_SIMLOCK_GENERATE_HMAC = 2,
  UIM_REMOTE_SIMLOCK_GET_MAX_SUPPORTED_VERSION = 3,
  UIM_REMOTE_SIMLOCK_GET_STATUS = 4,
  UIM_REMOTE_SIMLOCK_GENERATE_BLOB_REQUEST = 5,
  UIM_REMOTE_SIMLOCK_UNLOCK_TIMER_START = 6,
  UIM_REMOTE_SIMLOCK_UNLOCK_TIMER_STOP = 7,
}
