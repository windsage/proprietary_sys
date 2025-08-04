/* ==============================================================================
 * VideoColorFormatType.aidl
 *
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
============================================================================== */


package vendor.qti.hardware.wifidisplaysession_aidl;

@VintfStability
@Backing(type="int")
enum VideoColorFormatType {
    WFD_SESSION_VIDEO_FORMAT_YCbCr,
    WFD_SESSION_VIDEO_FORMAT_ARGB32,
}
