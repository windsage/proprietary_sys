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
parcelable Filter {
  vendor.qti.latencyaidlservice.IpAddress srcIp;
  char srcPort;
  vendor.qti.latencyaidlservice.IpAddress dstIp;
  char dstPort;
  vendor.qti.latencyaidlservice.Protocol protocol;
  vendor.qti.latencyaidlservice.FlowMark mark;
  vendor.qti.latencyaidlservice.Level uplink_level;
  vendor.qti.latencyaidlservice.Level downlink_level;
  long pdcp_timer;
  vendor.qti.latencyaidlservice.OodStatus ood;
}
