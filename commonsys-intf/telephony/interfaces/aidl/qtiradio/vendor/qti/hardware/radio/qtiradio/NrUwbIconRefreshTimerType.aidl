/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.qtiradio;

@VintfStability
@Backing(type="int")
enum NrUwbIconRefreshTimerType {
    // These values refer to the types of network events and RRC (Radio Resource Control)
    // transitions
    SCG_TO_MCG      = 0,
    IDLE_TO_CONNECT = 1,
    IDLE            = 2,
}