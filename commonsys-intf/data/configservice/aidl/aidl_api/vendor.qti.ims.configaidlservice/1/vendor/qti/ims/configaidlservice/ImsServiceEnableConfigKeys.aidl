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
enum ImsServiceEnableConfigKeys {
  VOLTE_ENABLED_KEY = 400,
  VIDEOTELEPHONY_ENABLED_KEY,
  MOBILE_DATA_ENABLED_KEY,
  WIFI_CALLING_ENABLED_KEY,
  WIFI_CALLING_IN_ROAMING_ENABLED_KEY,
  IMS_SERVICE_ENABLED_KEY,
  UT_ENABLED_KEY,
  SMS_ENABLED_KEY,
  DAN_ENABLED_KEY,
  USSD_ENABLED_KEY,
  MWI_ENABLED_KEY,
  PRESENCE_ENABLED_KEY,
  AUTOCONFIG_ENABLED_KEY,
  XDM_CLIENT_ENABLED_KEY,
  RCS_MESSAGING_ENABLED_KEY,
  CALL_MODE_PREF_ROAM_ENABLED_KEY,
  RTT_ENABLED_KEY,
  CARRIER_CONFIG_ENABLED_KEY,
  WIFI_PROVISIONING_ID_KEY,
  CALL_MODE_PREFERENCE_KEY,
  CALL_MODE_ROAM_PREFERENCE_KEY,
  SERVICE_MASK_BY_NETWORK_ENABLED_KEY,
  OPTIONS_ENABLED_KEY,
  CALL_COMPOSER_ENABLED_KEY,
}
