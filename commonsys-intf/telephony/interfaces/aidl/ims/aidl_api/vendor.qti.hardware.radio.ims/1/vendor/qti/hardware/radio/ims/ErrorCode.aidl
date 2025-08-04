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
  INVALID = 0,
  SUCCESS = 1,
  RADIO_NOT_AVAILABLE = 2,
  GENERIC_FAILURE = 3,
  PASSWORD_INCORRECT = 4,
  REQUEST_NOT_SUPPORTED = 5,
  CANCELLED = 6,
  NO_MEMORY = 7,
  UNUSED = 8,
  INVALID_PARAMETER = 9,
  REJECTED_BY_REMOTE = 10,
  IMS_DEREGISTERED = 11,
  NETWORK_NOT_SUPPORTED = 12,
  HOLD_RESUME_FAILED = 13,
  HOLD_RESUME_CANCELED = 14,
  REINVITE_COLLISION = 15,
  FDN_CHECK_FAILURE = 16,
  SS_MODIFIED_TO_DIAL = 17,
  SS_MODIFIED_TO_USSD = 18,
  SS_MODIFIED_TO_SS = 19,
  SS_MODIFIED_TO_DIAL_VIDEO = 20,
  DIAL_MODIFIED_TO_USSD = 21,
  DIAL_MODIFIED_TO_SS = 22,
  DIAL_MODIFIED_TO_DIAL = 23,
  DIAL_MODIFIED_TO_DIAL_VIDEO = 24,
  DIAL_VIDEO_MODIFIED_TO_USSD = 25,
  DIAL_VIDEO_MODIFIED_TO_SS = 26,
  DIAL_VIDEO_MODIFIED_TO_DIAL = 27,
  DIAL_VIDEO_MODIFIED_TO_DIAL_VIDEO = 28,
  USSD_CS_FALLBACK = 29,
}
