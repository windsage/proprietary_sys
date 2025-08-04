/* ==============================================================================
 * MediaInfo.aidl
 *
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
============================================================================== */


package vendor.qti.hardware.wifidisplaysession_aidl;

import vendor.qti.hardware.wifidisplaysession_aidl.WFDMediaTrackType;
import vendor.qti.hardware.wifidisplaysession_aidl.AudioInfo;
import vendor.qti.hardware.wifidisplaysession_aidl.VideoInfo;

@VintfStability
parcelable MediaInfo {
    @VintfStability
    parcelable TrackInfo {
        AudioInfo sAudioInfo;
        VideoInfo sVideoInfo;
    }
    WFDMediaTrackType eTrackType;
    TrackInfo sInfo;
}
