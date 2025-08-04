/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.TtyMode;

/**
 * Data structure containing TTY mode and additional info as bytes.
 * Lower layers will process TtyInfo if TtyInfo#TtyMode
 * is not TtyMode#TTY_MODE_INVALID.
 */
@VintfStability
parcelable TtyInfo {
    /*
     * Tty Mode
     */
    TtyMode mode = TtyMode.INVALID;
    byte[] userData;
}

