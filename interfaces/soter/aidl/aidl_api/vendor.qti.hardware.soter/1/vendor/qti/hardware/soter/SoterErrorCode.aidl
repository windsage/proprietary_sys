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

package vendor.qti.hardware.soter;
@Backing(type="int") @VintfStability
enum SoterErrorCode {
  SOTER_ERROR_OK = 0,
  SOTER_ERROR_ATTK_IS_VALID = (-1) /* -1 */,
  SOTER_ERROR_ATTK_NOT_EXIST = (-2) /* -2 */,
  SOTER_ERROR_ATTK_DIGEST_NOT_MATCH = (-3) /* -3 */,
  SOTER_ERROR_ATTK_DIGEST_NOT_READY = (-4) /* -4 */,
  SOTER_ERROR_ASK_NOT_READY = (-5) /* -5 */,
  SOTER_ERROR_AUTH_KEY_NOT_READY = (-6) /* -6 */,
  SOTER_ERROR_SESSION_OUT_OF_TIME = (-7) /* -7 */,
  SOTER_ERROR_NO_AUTH_KEY_MATCHED = (-8) /* -8 */,
  SOTER_ERROR_IS_AUTHING = (-9) /* -9 */,
  SOTER_ERROR_OTHERS = (-10) /* -10 */,
  SOTER_ERROR_MEMORY_ALLOCATION_FAILED = (-11) /* -11 */,
  SOTER_ERROR_SOTER_NOT_ENABLED = (-12) /* -12 */,
  SOTER_ERROR_ATTK_NOT_PROVISIONED = (-13) /* -13 */,
  SOTER_SECURITY_STATE_FAILURE = (-14) /* -14 */,
  SOTER_ERROR_INVALID_TAG = (-15) /* -15 */,
  SOTER_ERROR_INVALID_ARGUMENT = (-16) /* -16 */,
  SOTER_ERROR_UNSUPPORTED_KEY_SIZE = (-17) /* -17 */,
  SOTER_ERROR_SECURE_HW_COMMUNICATION_FAILED = (-18) /* -18 */,
  SOTER_ERROR_ATTK_ALREADY_PROVISIONED = (-20) /* -20 */,
  SOTER_RPMB_NOT_PROVISIONED = (-21) /* -21 */,
  SOTER_ERROR_INSUFFICIENT_BUFFER_SPACE = (-22) /* -22 */,
  SOTER_ERROR_UNSUPPORTED_DIGEST = (-23) /* -23 */,
  SOTER_ERROR_UNSUPPORTED_PADDING_MODE = (-24) /* -24 */,
  SOTER_ERROR_INVALID_KEY_BLOB = (-25) /* -25 */,
  SOTER_ERROR_VERIFICATION_FAILED = (-26) /* -26 */,
  SOTER_ERROR_INVALID_AUTHORIZATION_TIMEOUT = (-27) /* -27 */,
  SOTER_ERROR_KEY_EXPORT_OPTIONS_INVALID = (-28) /* -28 */,
  SOTER_ERROR_UNEXPECTED_NULL_POINTER = (-29) /* -29 */,
  SOTER_WRAPPERERROR_UNKNOWN = (-200) /* -200 */,
  SOTER_ERROR_UID_NULL = (-201) /* -201 */,
  SOTER_ERROR_KNAME_NULL = (-202) /* -202 */,
  SOTER_ERROR_CHALLENGE_NULL = (-203) /* -203 */,
  SOTER_ERROR_OPERATEID_NULL = (-204) /* -204 */,
  SOTER_ERROR_UNKNOWN_ERROR = (-1000) /* -1000 */,
}
