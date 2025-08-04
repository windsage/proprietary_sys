/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.atfwd;

import vendor.qti.hardware.radio.atfwd.AtCmdResult;

@VintfStability
parcelable AtCmdResponse {
    AtCmdResult result;
    String response;
}
