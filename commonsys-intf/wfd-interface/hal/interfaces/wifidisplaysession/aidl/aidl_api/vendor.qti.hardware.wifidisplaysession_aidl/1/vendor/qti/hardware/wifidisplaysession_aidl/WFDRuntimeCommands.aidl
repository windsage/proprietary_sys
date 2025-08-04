/* ==============================================================================
 * WFDRuntimeCommands.aidl
 *
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
============================================================================== */
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

package vendor.qti.hardware.wifidisplaysession_aidl;
@Backing(type="int") @VintfStability
enum WFDRuntimeCommands {
  WFD_SESSION_CMD_OPEN_AUDIO_PROXY,
  WFD_SESSION_CMD_CLOSE_AUDIO_PROXY,
  WFD_SESSION_CMD_ENABLE_BITRATE_ADAPT,
  WFD_SESSION_CMD_DISABLE_BITRATE_ADAPT,
  WFD_SESSION_CMD_BLANK_REMOTE_DISPLAY,
  WFD_SESSION_CMD_ENABLE_STREAMING_FEATURE,
  WFD_SESSION_CMD_DISABLE_STREAMING_FEATURE,
  WFD_SESSION_CMD_DISABLE_AUDIO,
  WFD_SESSION_CMD_ENABLE_AUDIO,
  WFD_SESSION_CMD_DISABLE_VIDEO,
  WFD_SESSION_CMD_ENABLE_VIDEO,
  WFD_SESSION_CMD_INVALID,
}
