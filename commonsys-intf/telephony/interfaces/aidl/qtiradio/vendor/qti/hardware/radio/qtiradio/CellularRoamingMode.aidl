/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.qtiradio;

@VintfStability
@Backing(type="int")
enum CellularRoamingMode {
    INVALID = -1,   // Cellular roaming invalid
    DISABLED = 0,   // Cellular roaming disabled
    ENABLED = 1,    // Cellular roaming enabled
}