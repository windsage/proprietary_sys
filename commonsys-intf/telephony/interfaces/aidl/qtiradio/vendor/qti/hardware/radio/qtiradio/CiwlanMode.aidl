/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.qtiradio;

@VintfStability
@Backing(type="int")
enum CiwlanMode {
    // The C_IWLAN mode is a static configuration and will not change after boot
    INVALID = -1,
    ONLY = 0,
    PREFERRED = 1,
}