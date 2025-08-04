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
interface IHexlp {
  vendor.qti.hardware.hexlp.HexlpError LoadExtensions(in String[] preload_extensions, out int[] failure_indexes);
  vendor.qti.hardware.hexlp.HexlpError UnloadExtensions(in String[] unload_extensions, out int[] failure_indexes);
  vendor.qti.hardware.hexlp.HexlpError CreateSession(in vendor.qti.hardware.hexlp.HexlpCreateSessionParams create_session_params, in @nullable vendor.qti.hardware.hexlp.HexlpCustomElements custom_ctrls);
  vendor.qti.hardware.hexlp.HexlpError DestroySession();
  vendor.qti.hardware.hexlp.HexlpError OpenSession(in vendor.qti.hardware.hexlp.HexlpSessionDescriptionParams open_session_params, in vendor.qti.hardware.hexlp.HexlpCustomElements custom_ctrls, out vendor.qti.hardware.hexlp.HexlpSessionResponse open_session_rsps);
  vendor.qti.hardware.hexlp.HexlpError CloseSession();
  vendor.qti.hardware.hexlp.HexlpError SendProcessingParameters(in vendor.qti.hardware.hexlp.HexlpSPPBuffer spp_buf, in @nullable vendor.qti.hardware.hexlp.HexlpCustomElements custom_ctrls);
  vendor.qti.hardware.hexlp.HexlpError SendMultipleProcessingParameters(in vendor.qti.hardware.hexlp.HexlpSPPBuffer[] spp_bufs, in @nullable vendor.qti.hardware.hexlp.HexlpCustomElements custom_ctrls);
  vendor.qti.hardware.hexlp.HexlpError ReconfigureSession(in vendor.qti.hardware.hexlp.HexlpSessionDescriptionParams reconfig_session_params, in @nullable vendor.qti.hardware.hexlp.HexlpCustomElements custom_ctrls);
  vendor.qti.hardware.hexlp.HexlpError FlushSession();
  vendor.qti.hardware.hexlp.HexlpError DrainSession();
  vendor.qti.hardware.hexlp.HexlpError PreMapBuffers(in vendor.qti.hardware.hexlp.HexlpMemBuffer[] pre_mapped_bufs, out int[] failure_cookies);
  vendor.qti.hardware.hexlp.HexlpError UnMapBuffers(in int[] unmapped_buf_cookies, out int[] failure_cookies);
}
