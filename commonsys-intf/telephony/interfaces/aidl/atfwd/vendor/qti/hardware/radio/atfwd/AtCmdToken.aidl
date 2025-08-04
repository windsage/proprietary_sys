/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.atfwd;

@VintfStability
parcelable AtCmdToken {
    /** count of the number of tokens received as parameters to an AT command */
    int numberOfItems;

    /** list of tokens */
    String[] items;
}
