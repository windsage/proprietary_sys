/*
 * Copyright (c) 2021-2023 Qualcomm Technologies, Inc.
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

package vendor.qti.hardware.radio.qtiradio;
@VintfStability
interface IQtiRadio {
  oneway void setCallbacks(in vendor.qti.hardware.radio.qtiradio.IQtiRadioResponse responseCallback, in vendor.qti.hardware.radio.qtiradio.IQtiRadioIndication indicationCallback);
  oneway void queryNrIconType(in int serial);
  oneway void enableEndc(in int serial, in boolean enable);
  oneway void queryEndcStatus(in int serial);
  String getPropertyValue(in String prop, in String def);
  oneway void setNrConfig(in int serial, in vendor.qti.hardware.radio.qtiradio.NrConfig config);
  oneway void queryNrConfig(in int serial);
  oneway void getQtiRadioCapability(in int serial);
  oneway void getCallForwardStatus(in int serial, in vendor.qti.hardware.radio.qtiradio.CallForwardInfo callForwardInfo);
  oneway void getFacilityLockForApp(in int serial, in vendor.qti.hardware.radio.qtiradio.FacilityLockInfo facilityLockInfo);
  oneway void getImei(in int serial);
  oneway void getDdsSwitchCapability(in int serial);
  oneway void sendUserPreferenceForDataDuringVoiceCall(in int serial, in boolean userPreference);
  boolean isEpdgOverCellularDataSupported();
  oneway void setNrUltraWidebandIconConfig(in int serial, in int sib2Value, in @nullable vendor.qti.hardware.radio.qtiradio.NrUwbIconBandInfo saBandInfo, in @nullable vendor.qti.hardware.radio.qtiradio.NrUwbIconBandInfo nsaBandInfo, in @nullable List<vendor.qti.hardware.radio.qtiradio.NrUwbIconRefreshTime> refreshTime, in @nullable vendor.qti.hardware.radio.qtiradio.NrUwbIconBandwidthInfo minAggregateBwInfo);
  oneway void getNetworkSelectionMode(in int serial);
  oneway void setNetworkSelectionModeAutomatic(in int serial, in vendor.qti.hardware.radio.qtiradio.AccessMode mode);
  oneway void setNetworkSelectionModeManual(in int serial, in vendor.qti.hardware.radio.qtiradio.SetNetworkSelectionMode setNetworkSelectionMode);
  oneway void startNetworkScan(in int serial, in vendor.qti.hardware.radio.qtiradio.QtiNetworkScanRequest request);
  oneway void stopNetworkScan(in int serial);
  vendor.qti.hardware.radio.qtiradio.CiwlanConfig getCiwlanConfig();
  vendor.qti.hardware.radio.qtiradio.PersoUnlockStatus getSimPersoUnlockStatus();
}
