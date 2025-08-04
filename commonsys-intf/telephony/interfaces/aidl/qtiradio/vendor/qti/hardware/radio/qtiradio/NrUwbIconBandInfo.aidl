/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.qtiradio;

import vendor.qti.hardware.radio.qtiradio.NrUwbIconMode;

@VintfStability
@JavaDerive(equals = true)
parcelable NrUwbIconBandInfo {
    // Whether 5G UWB icon is enabled for this band
    boolean enabled;
    // One of NONE, CONNECTED, IDLE, or CONNECTED_AND_IDLE
    NrUwbIconMode mode;
    // The bands array
    int[] bands;
}
