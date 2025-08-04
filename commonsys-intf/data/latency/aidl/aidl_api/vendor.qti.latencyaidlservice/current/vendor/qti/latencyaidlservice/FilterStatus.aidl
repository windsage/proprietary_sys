/******************************************************************************  
Copyright (c) 2023 Qualcomm Technologies, Inc.  
All Rights Reserved.  
Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/
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

package vendor.qti.latencyaidlservice;
@Backing(type="long") @VintfStability
enum FilterStatus {
  FILTER_INACTIVE = 0,
  FILTER_ACTIVE = 1,
  FILTER_INACTIVITY_TIMEOUT = 2,
  FILTER_DELETED = 3,
  ERROR_DUPLICATE_FILTER = 4,
  ERROR_FILTER_LIMIT_REACHED = 5,
  ERROR_INTERNAL = 6,
  ERROR_INVALID_ARGS = 7,
  ERROR_IP_TYPE_MISMATCH = 8,
  ERROR_INACTIVITY_TIMEOUT_INVALID = 9,
  ERROR_DST_PORT_INVALID = 10,
  ERROR_SRC_PORT_INVALID = 11,
  ERROR_DST_IP_INVALID = 12,
  ERROR_SRC_IP_INVALID = 13,
}
