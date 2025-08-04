/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.uim_remote_client;

@VintfStability
@Backing(type="int")
enum UimRemoteClientRsp {
    UIM_REMOTE_ERR_SUCCESS = 0,
    UIM_REMOTE_ERR_GENERIC_FAILURE = 1,
    UIM_REMOTE_ERR_NOT_SUPPORTED = 2,
    UIM_REMOTE_ERR_INVALID_PARAMETER = 3,
}
