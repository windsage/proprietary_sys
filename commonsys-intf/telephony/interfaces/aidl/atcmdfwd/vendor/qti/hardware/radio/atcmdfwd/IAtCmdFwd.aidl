/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.atcmdfwd;

import vendor.qti.hardware.radio.atcmdfwd.AtCmd;
import vendor.qti.hardware.radio.atcmdfwd.AtCmdResponse;

@VintfStability
interface IAtCmdFwd {

    /*
     * Process AtCmd
     *
     * @param cmd is Atcmd to be handled
     *
     * @return resp is AtCmdResponse object with result and string
     *         result ATCMD_RESULT_OK on success
     *                ATCMD_RESULT_ERROR with error strings
     *         response has error string message
     */
    AtCmdResponse processAtCmd(in AtCmd cmd);
}
