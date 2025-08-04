/* ==============================================================================
 * AudioInfo.aidl
 *
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
============================================================================== */



package vendor.qti.hardware.wifidisplaysession_aidl;

@VintfStability
parcelable AudioInfo {
    int nSampleRate;
    int nSamplesPerFrame;
    int nBitsPerSample;
    int nChannels;
}
