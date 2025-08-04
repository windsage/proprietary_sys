/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.qtiradio;

@VintfStability
@Backing(type="int")
enum NrConfig {
    NR_CONFIG_INVALID = -1,
    NR_CONFIG_COMBINED_SA_NSA = 0,
    NR_CONFIG_NSA = 1,
    NR_CONFIG_SA= 2,
}