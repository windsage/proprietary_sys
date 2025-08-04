/* ==============================================================================
 * ImageInfo.aidl
 *
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
============================================================================== */


package vendor.qti.hardware.wifidisplaysession_aidl;

@VintfStability
parcelable ImageInfo {
    int nHeight;
    int nWidth;
    boolean bSecure;
    int nMaxOverlaySupport;
}
