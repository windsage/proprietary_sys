/******************************************************************************  
Copyright (c) 2023 Qualcomm Technologies, Inc.  
All Rights Reserved.  
Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/
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

package vendor.qti.latencyaidlservice;
@VintfStability
interface ILinkLatencyService {
  vendor.qti.latencyaidlservice.StatusCode addFilter(in vendor.qti.latencyaidlservice.Filter filter, out vendor.qti.latencyaidlservice.AddFilterId addfilterid);
  vendor.qti.latencyaidlservice.StatusCode close();
  vendor.qti.latencyaidlservice.StatusCode deleteAllFilters();
  vendor.qti.latencyaidlservice.StatusCode deleteFilter(in int filterId);
  oneway void enableHighPerformanceGaming(in boolean isEnable);
  vendor.qti.latencyaidlservice.FilterInfo[] getFilters();
  vendor.qti.latencyaidlservice.StatusCode prioritizeDefaultDataSubscription(in boolean isEnabled);
  oneway void setCallback(in vendor.qti.latencyaidlservice.ILinkLatencyIndication callback);
  vendor.qti.latencyaidlservice.StatusCode setLevel(in vendor.qti.latencyaidlservice.Config params, out vendor.qti.latencyaidlservice.SetLevelArguments setlevelarguments);
  vendor.qti.latencyaidlservice.StatusCode updateOODStatus(in int filterId, in vendor.qti.latencyaidlservice.OodStatus ood);
  vendor.qti.latencyaidlservice.StatusCode updateOodForDDS(in vendor.qti.latencyaidlservice.OodStatus ood);
  vendor.qti.latencyaidlservice.StatusCode updatePdcpDiscardTimer(in int filterId, in long timer);
  vendor.qti.latencyaidlservice.StatusCode updatePdcpDiscardTimerForDDS(in long timerValue);
  vendor.qti.latencyaidlservice.StatusCode updatelinkLatencyLevel(in int filterId, in vendor.qti.latencyaidlservice.Level uplink_level, in vendor.qti.latencyaidlservice.Level downlink_level);
  vendor.qti.latencyaidlservice.StatusCode prioritizeDataPerSubscription(in vendor.qti.latencyaidlservice.SlotId slotId, in boolean isEnabled);
}
