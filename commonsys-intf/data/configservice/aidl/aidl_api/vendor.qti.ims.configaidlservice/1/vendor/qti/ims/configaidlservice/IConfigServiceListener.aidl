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
interface IConfigServiceListener {
  oneway void onAutoConfigErrorSipResponse(in vendor.qti.ims.configaidlservice.AutoConfigResponse acsResponse);
  oneway void onAutoConfigurationReceived(in vendor.qti.ims.configaidlservice.AutoConfig acsConfig);
  oneway void onCommandStatus(in vendor.qti.ims.configaidlservice.RequestStatus status, in int userData);
  oneway void onGetSettingsResponse(in vendor.qti.ims.configaidlservice.RequestStatus status, in vendor.qti.ims.configaidlservice.SettingsData cbdata, in int userData);
  oneway void onGetUpdatedSettings(in vendor.qti.ims.configaidlservice.SettingsData cbdata);
  oneway void onRcsServiceStatusUpdate(in boolean isRcsEnabled);
  oneway void onReconfigNeeded();
  oneway void onTokenFetchRequest(in int requestId, in vendor.qti.ims.configaidlservice.TokenType tokenType, in vendor.qti.ims.configaidlservice.TokenRequestReason reqReason);
  oneway void onUceStatusUpdate(in vendor.qti.ims.configaidlservice.UceCapabilityInfo capinfo);
  oneway void onUserAgentReceived(in String userAgent, in vendor.qti.ims.configaidlservice.AppType appType);
}
