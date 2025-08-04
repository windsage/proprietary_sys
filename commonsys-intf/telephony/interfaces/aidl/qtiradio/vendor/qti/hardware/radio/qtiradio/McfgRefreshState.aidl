/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.qtiradio;

@VintfStability
@Backing(type="int")
enum McfgRefreshState  {
    START           = 0,     //  Refresh start event
    COMPLETE        = 1,     //  Refresh complete event
    CLIENT_REFRESH  = 2,     //  Client refresh event
}
