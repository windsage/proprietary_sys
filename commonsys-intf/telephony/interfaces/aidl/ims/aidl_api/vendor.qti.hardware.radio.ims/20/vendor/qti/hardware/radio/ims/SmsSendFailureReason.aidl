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
  INVALID,
  NONE,
  GENERIC_FAILURE,
  RADIO_OFF,
  NULL_PDU,
  NO_SERVICE,
  LIMIT_EXCEEDED,
  SHORT_CODE_NOT_ALLOWED,
  SHORT_CODE_NEVER_ALLOWED,
  FDN_CHECK_FAILURE,
  RADIO_NOT_AVAILABLE,
  NETWORK_REJECT,
  INVALID_ARGUMENTS,
  INVALID_STATE,
  NO_MEMORY,
  INVALID_SMS_FORMAT,
  SYSTEM_ERROR,
  MODEM_ERROR,
  NETWORK_ERROR,
  ENCODING_ERROR,
  INVALID_SMSC_ADDRESS,
  OPERATION_NOT_ALLOWED,
  INTERNAL_ERROR,
  NO_RESOURCES,
  CANCELLED,
  REQUEST_NOT_SUPPORTED,
}
