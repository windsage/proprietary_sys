/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.qtiradio;

import vendor.qti.hardware.radio.qtiradio.NrUwbIconMode;

@VintfStability
@JavaDerive(equals = true)
parcelable NrUwbIconBandwidthInfo {
    // Whether the minimum aggregate bandwidth will be considered for showing the 5G UWB icon
    boolean enabled;
    // One of NONE, CONNECTED, IDLE, or CONNECTED_AND_IDLE
    NrUwbIconMode mode;
    // The minimum aggregate bandwidth needed to show the 5G UWB icon
    int bandwidth;
}
