/*==============================================================================
 *  @file ISigma_miracast.aidl
 *
 *  @par DESCRIPTION:
 *       AIDL HAL API Defination
 *
 *
 * Copyright (c) 2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
  * Confidential and Proprietary - Qualcomm Technologies, Inc.
 ==============================================================================*/// FIXME: license file, or use the -l option to generate the files with the header.
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

package vendor.qti.hardware.sigma_miracast_aidl;
@VintfStability
interface ISigma_miracast {
  void connect_go_start_wfd(in String cmd_string, in String peer_ip, in int rtsp_port, in int dev_type, in String session_id, out int[] status, out String[] rtsp_session_id);
  int dev_exec_action(in String cmd);
  int dev_send_frame(in String cmd);
  int sta_generate_event(in String cmd_string);
  void sta_preset_testparameters(in String cmd, out int[] status, out String[] respBuf);
  int sta_reset_default(in String cmd);
  void start_wfd_connection(in String cmd_string, in String peer_ip, in int rtsp_port, in int dev_type, in String session_id, out int[] status, out String[] rtsp_session_id);
}
