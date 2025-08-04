/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.uim;

@VintfStability
@Backing(type="byte")
enum UimGbaStatus {
    /**
     * Success
     */
    UIM_GBA_SUCCESS = 0x00,
    /**
     * Failure
     */
    UIM_GBA_GENERIC_FAILURE = 0x1,
}
