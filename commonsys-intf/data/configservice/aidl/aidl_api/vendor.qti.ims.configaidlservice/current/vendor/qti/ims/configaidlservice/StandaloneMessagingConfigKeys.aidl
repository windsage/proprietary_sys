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

package vendor.qti.ims.configaidlservice;
@Backing(type="int") @VintfStability
enum StandaloneMessagingConfigKeys {
  DEFAULT_SMS_APP_KEY = 300,
  DEFAULT_VVM_APP_KEY,
  AUTO_CONFIG_USER_AGENT_KEY,
  XDM_CLIENT_USER_AGENT_KEY,
  CLIENT_VENDOR_KEY,
  CLIENT_VERSION_KEY,
  TERMINAL_VENDOR_KEY,
  TERMINAL_MODEL_KEY,
  TERMINAL_SW_VERSION_KEY,
  RCS_VERSION_KEY,
  PROVISIONING_VERSION_KEY,
  FRIENDLY_DEVICE_NAME_KEY,
  RCS_PROFILE_KEY,
  BOT_VERSION_KEY,
  APP_ID_KEY = 314,
}
