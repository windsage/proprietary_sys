/* ==============================================================================
 * WFDRuntimeCommands.aidl
 *
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
============================================================================== */


package vendor.qti.hardware.wifidisplaysession_aidl;

@VintfStability
@Backing(type="int")
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
