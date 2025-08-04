/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

/**
 * CallLocation used in call composer call.
 * Lower layers will process CallLocation if CallLocation#radius is > -1.
 */
@VintfStability
parcelable CallLocation {
    float radius = -1f;
    double latitude;
    double longitude;
}

