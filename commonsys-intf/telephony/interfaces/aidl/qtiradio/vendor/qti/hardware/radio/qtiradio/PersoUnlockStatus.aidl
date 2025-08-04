/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.qtiradio;

@VintfStability
@Backing(type="int")
enum PersoUnlockStatus {
    UNKNOWN = 0,
    TEMPORARY_UNLOCKED  = 1,
    PERMANENT_UNLOCKED  = 2,
}