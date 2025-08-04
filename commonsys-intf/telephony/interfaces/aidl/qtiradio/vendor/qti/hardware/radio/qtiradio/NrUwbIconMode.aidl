/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.qtiradio;

@VintfStability
@Backing(type="int")
enum NrUwbIconMode {
    // These values refer to RRC (Radio Resource Control) states
    NONE               = 0,
    CONNECTED          = 1,
    IDLE               = 2,
    CONNECTED_AND_IDLE = 3,
}