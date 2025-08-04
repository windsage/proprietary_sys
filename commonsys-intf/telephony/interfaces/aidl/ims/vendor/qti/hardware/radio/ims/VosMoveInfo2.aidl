/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.Coordinate2D;

/**
 * VosMoveInfo2 used in video online service call for move events.
 */
@VintfStability
parcelable VosMoveInfo2 {
    Coordinate2D point;
    //Index for the move event.
    int index;
    //Timestamp for the move event.
    long timestamp;
    //Slide duration in milliseconds.
    int duration;
}

