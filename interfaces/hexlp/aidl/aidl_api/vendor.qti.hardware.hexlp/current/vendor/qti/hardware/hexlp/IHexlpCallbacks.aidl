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

package vendor.qti.hardware.hexlp;
@VintfStability
interface IHexlpCallbacks {
  oneway void OnDrainDone(vendor.qti.hardware.hexlp.HexlpError drain_rsp);
  oneway void OnError(in vendor.qti.hardware.hexlp.HexlpError reported_err);
  oneway void OnFlushDone(vendor.qti.hardware.hexlp.HexlpError flush_rsp);
  oneway void OnReconfigDone(in vendor.qti.hardware.hexlp.HexlpError reconfig_err, in vendor.qti.hardware.hexlp.HexlpSessionResponse reconfig_rsp);
  oneway void OnInputBufferDone(in vendor.qti.hardware.hexlp.HexlpReturnedBufferInfo[] hexlp_ibd_info);
  oneway void OnInputBuffersDone(in vendor.qti.hardware.hexlp.HexlpReturnedBufferInfo[] hexlp_ibds_info);
  oneway void OnOutputBufferDone(in vendor.qti.hardware.hexlp.HexlpOBDInfo hexlp_obd_info);
  oneway void OnOutputBuffersDone(in vendor.qti.hardware.hexlp.HexlpOBDInfo[] hexlp_obds_info);
  oneway void OnSPPCustomResponse(in vendor.qti.hardware.hexlp.HexlpCustomElements custom_rsps);
}
