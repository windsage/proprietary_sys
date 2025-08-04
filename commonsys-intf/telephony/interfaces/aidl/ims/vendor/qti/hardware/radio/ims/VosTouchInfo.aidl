/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.Coordinate2D;

/**
 * VosTouchInfo used in video online service call for touch event.
 */
@VintfStability
parcelable VosTouchInfo {
    Coordinate2D touch;
    /**
     * Milliseconds
     */
    int touchDuration;
}

