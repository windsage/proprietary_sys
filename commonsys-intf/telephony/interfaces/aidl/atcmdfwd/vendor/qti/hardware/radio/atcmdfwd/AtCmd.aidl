/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.atcmdfwd;

import vendor.qti.hardware.radio.atcmdfwd.AtCmdToken;

@VintfStability
parcelable AtCmd {
    String name;
    int opCode;
    AtCmdToken token;
}
