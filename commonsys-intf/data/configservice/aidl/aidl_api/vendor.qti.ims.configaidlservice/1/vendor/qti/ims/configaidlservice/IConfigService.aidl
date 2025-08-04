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
@VintfStability
interface IConfigService {
  int deregisterForSettingsChange(in int userData);
  int getAcsConfiguration(in int userData);
  int getRcsServiceStatus(out vendor.qti.ims.configaidlservice.RcsStatus rcsStatus);
  int getSettingsValue(in vendor.qti.ims.configaidlservice.SettingsId settingsId, in int userData);
  int getUceStatus(out vendor.qti.ims.configaidlservice.UceCapabilityInfo uceCapInfo);
  int registerForSettingsChange(in int userData);
  int setAppToken(in String token, in int userData);
  int setConfig(in vendor.qti.ims.configaidlservice.ConfigData configData, in int userData);
  int setSettingsValue(in vendor.qti.ims.configaidlservice.SettingsData settingsData, in int userData);
  int triggerAcsRequest(in vendor.qti.ims.configaidlservice.AutoConfigTriggerReason autoConfigReasonType, in int userData);
  int updateTokenFetchStatus(in int requestId, in vendor.qti.ims.configaidlservice.TokenType tokenType, in vendor.qti.ims.configaidlservice.StatusCode status, in int userData);
  int getUserAgent(in vendor.qti.ims.configaidlservice.AppType appType);
  int setSmsVersion(in String version);
}
