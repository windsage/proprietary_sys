/* ==============================================================================
 * VideoInfo.aidl
 *
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
============================================================================== */


package vendor.qti.hardware.wifidisplaysession_aidl;

import vendor.qti.hardware.wifidisplaysession_aidl.VideoColorFormatType;

@VintfStability
parcelable VideoInfo {
    int nHeight;
    int nWidth;
    int nFrameRate;
    int nMinBuffersReq;
    int nCanSkipFrames;
    int nMaxFrameSkipIntervalMs;
    int nIDRIntervalMs;
    VideoColorFormatType eColorFmt;
    boolean bEnableUBWC;
}
