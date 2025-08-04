/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.Coordinate2D;

/**
 * VosMoveInfo used in video online service call for move event.
 */
@VintfStability
parcelable VosMoveInfo {
    Coordinate2D start;
    Coordinate2D end;
}

