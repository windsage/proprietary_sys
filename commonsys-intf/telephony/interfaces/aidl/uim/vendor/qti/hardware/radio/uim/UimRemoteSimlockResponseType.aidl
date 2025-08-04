/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.uim;

@VintfStability
@Backing(type="int")
enum UimRemoteSimlockResponseType {
    UIM_REMOTE_SIMLOCK_RESP_SUCCESS = 0,
    UIM_REMOTE_SIMLOCK_RESP_FAILURE = 1,
    UIM_REMOTE_SIMLOCK_RESP_GET_TIME_FAILED = 2,
}
