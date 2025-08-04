/* ==============================================================================
 * SamplwInfo.aidl
 *
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
============================================================================== */


package vendor.qti.hardware.wifidisplaysession_aidl;

import android.hardware.common.NativeHandle;

@VintfStability
parcelable SampleInfo {
    long nTimeStamp;
    long nSampleId;
    android.hardware.common.NativeHandle nBufHandle;
    int nHeight;
    int nWidth;
    int nStride;
    long nFrameNo;
    long nArrivalTime;
}
