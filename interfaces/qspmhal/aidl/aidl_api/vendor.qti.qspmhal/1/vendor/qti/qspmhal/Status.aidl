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

package vendor.qti.qspmhal;
@Backing(type="int") @VintfStability
enum Status {
  SUCCESS = 0,
  FAIL = 1,
  MEMORY_ALLOCATION_FAILED = 2,
  MEMORY_MAPPING_FAILED = 3,
  UNSUPPORTED_PROF_FILE_SIZE = 4,
  CPU_PROFILE_NOT_FOUND = 5,
  GPU_PROFILE_NOT_FOUND = 6,
  GPU_PROFILE_READ_ERROR = 7,
  CPU_PROFILE_READ_ERROR = 8,
  PID_ERROR = 9,
  DELETE_ALL_FAILED = 10,
  UPDATE_FAILED = 11,
  DELETE_FAILED = 12,
  UNKOWN_COMMAND = 13,
}
