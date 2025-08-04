/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.atcmdfwd;

@VintfStability
@Backing(type="int")
enum AtCmdResult {
    ATCMD_RESULT_ERROR = 0,
    ATCMD_RESULT_OK    = 1,
    ATCMD_RESULT_OTHER = 2,
}
