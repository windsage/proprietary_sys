/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
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

package vendor.qti.hardware.radio.qtiradio;
@VintfStability
interface IQtiRadioIndication {
  oneway void onNrIconTypeChange(in vendor.qti.hardware.radio.qtiradio.NrIconType iconType);
  oneway void onNrConfigChange(in vendor.qti.hardware.radio.qtiradio.NrConfig config);
  oneway void onImeiChange(in vendor.qti.hardware.radio.qtiradio.ImeiInfo info);
  oneway void onDdsSwitchCapabilityChange();
  oneway void onDdsSwitchCriteriaChange(in boolean telephonyDdsSwitch);
  oneway void onDdsSwitchRecommendation(in int recommendedSlotId);
  oneway void onDataDeactivateDelayTime(in long delayTimeMilliSecs);
  oneway void onEpdgOverCellularDataSupported(in boolean support);
  oneway void onMcfgRefresh(in vendor.qti.hardware.radio.qtiradio.McfgRefreshState refreshState, in int slotId);
  oneway void networkScanResult(in vendor.qti.hardware.radio.qtiradio.QtiNetworkScanResult result);
  oneway void onSimPersoUnlockStatusChange(in vendor.qti.hardware.radio.qtiradio.PersoUnlockStatus status);
  oneway void onCiwlanAvailable(in boolean ciwlanAvailable);
  oneway void onCiwlanConfigChange(in vendor.qti.hardware.radio.qtiradio.CiwlanConfig ciwlanConfig);
  oneway void onNrIconChange(in vendor.qti.hardware.radio.qtiradio.NrIcon icon);
  oneway void onGetAllEsimProfilesReq(in int referenceNum);
  oneway void onEnableProfileReq(in int referenceNum, in String iccId);
  oneway void onDisableProfileReq(in int referenceNum, in String iccId);
}
