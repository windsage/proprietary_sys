/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.qtiradio;

import vendor.qti.hardware.radio.qtiradio.NrUwbIconRefreshTimerType;

@VintfStability
@JavaDerive(equals = true)
parcelable NrUwbIconRefreshTime {
    // One of SCG_TO_MCG, IDLE_TO_CONNECT, or IDLE
    NrUwbIconRefreshTimerType timerType;
    // The timer value upon the expiration of which the 5G icon will be refreshed
    int timeValue;
}
