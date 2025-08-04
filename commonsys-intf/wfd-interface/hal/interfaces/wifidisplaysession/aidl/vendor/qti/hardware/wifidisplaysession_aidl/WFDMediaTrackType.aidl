/* ==============================================================================
 * WFDMediaTrackType.aidl
 *
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
============================================================================== */


package vendor.qti.hardware.wifidisplaysession_aidl;

@VintfStability
@Backing(type="int")
enum WFDMediaTrackType {
    WFD_SESSION_AUDIO_TRACK,
    WFD_SESSION_VIDEO_TRACK,
    WFD_SESSION_IMAGE_TRACK,
}
