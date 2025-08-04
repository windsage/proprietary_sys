/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.atfwd;

import vendor.qti.hardware.radio.atfwd.AtCmdToken;

@VintfStability
parcelable AtCmd {
    /** name of the AT command, e.g., +CFUN */
    String name;

    /** operation code */
    int opCode;

    /** parameters for the current AT command */
    AtCmdToken token;
}
