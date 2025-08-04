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
enum ErrorCode {
  INVALID,
  SUCCESS,
  RADIO_NOT_AVAILABLE,
  GENERIC_FAILURE,
  PASSWORD_INCORRECT,
  REQUEST_NOT_SUPPORTED,
  CANCELLED,
  NO_MEMORY,
  UNUSED,
  INVALID_PARAMETER,
  REJECTED_BY_REMOTE,
  IMS_DEREGISTERED,
  NETWORK_NOT_SUPPORTED,
  HOLD_RESUME_FAILED,
  HOLD_RESUME_CANCELED,
  REINVITE_COLLISION,
  FDN_CHECK_FAILURE,
  SS_MODIFIED_TO_DIAL,
  SS_MODIFIED_TO_USSD,
  SS_MODIFIED_TO_SS,
  SS_MODIFIED_TO_DIAL_VIDEO,
  DIAL_MODIFIED_TO_USSD,
  DIAL_MODIFIED_TO_SS,
  DIAL_MODIFIED_TO_DIAL,
  DIAL_MODIFIED_TO_DIAL_VIDEO,
  DIAL_VIDEO_MODIFIED_TO_USSD,
  DIAL_VIDEO_MODIFIED_TO_SS,
  DIAL_VIDEO_MODIFIED_TO_DIAL,
  DIAL_VIDEO_MODIFIED_TO_DIAL_VIDEO,
  USSD_CS_FALLBACK,
  CF_SERVICE_NOT_REGISTERED,
  RIL_INTERNAL_NO_MEMORY,
  RIL_INTERNAL_INVALID_STATE,
  RIL_INTERNAL_INVALID_ARGUMENTS,
  RIL_INTERNAL_GENERIC_FAILURE,
}
