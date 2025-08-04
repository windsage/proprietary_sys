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
enum SmsSendFailureReason {
  INVALID = 0,
  NONE = 1,
  GENERIC_FAILURE = 2,
  RADIO_OFF = 3,
  NULL_PDU = 4,
  NO_SERVICE = 5,
  LIMIT_EXCEEDED = 6,
  SHORT_CODE_NOT_ALLOWED = 7,
  SHORT_CODE_NEVER_ALLOWED = 8,
  FDN_CHECK_FAILURE = 9,
  RADIO_NOT_AVAILABLE = 10,
  NETWORK_REJECT = 11,
  INVALID_ARGUMENTS = 12,
  INVALID_STATE = 13,
  NO_MEMORY = 14,
  INVALID_SMS_FORMAT = 15,
  SYSTEM_ERROR = 16,
  MODEM_ERROR = 17,
  NETWORK_ERROR = 18,
  ENCODING_ERROR = 19,
  INVALID_SMSC_ADDRESS = 20,
  OPERATION_NOT_ALLOWED = 21,
  INTERNAL_ERROR = 22,
  NO_RESOURCES = 23,
  CANCELLED = 24,
  REQUEST_NOT_SUPPORTED = 25,
}
