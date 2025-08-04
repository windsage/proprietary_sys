/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.atfwd;

@VintfStability
@Backing(type="int")
enum AtCmdResult {
    ATCMD_RESULT_ERROR = 0,
    ATCMD_RESULT_OK    = 1,
    ATCMD_RESULT_OTHER = 2,
}
