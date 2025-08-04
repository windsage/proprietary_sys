/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.Size;

@VintfStability
parcelable MediaConfig {
    Size maxAvcCodecResolution;
    Size maxHevcCodecResolution;
    Size screenSize;
}
