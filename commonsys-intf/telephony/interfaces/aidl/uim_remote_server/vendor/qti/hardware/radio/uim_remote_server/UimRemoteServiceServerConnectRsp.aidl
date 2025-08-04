/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.uim_remote_server;

@VintfStability
@Backing(type="int")
enum UimRemoteServiceServerConnectRsp {
    UIM_REMOTE_SERVICE_SERVER_SUCCESS = 0,
    UIM_REMOTE_SERVICE_SERVER_CONNECT_FAILURE = 1,
    UIM_REMOTE_SERVICE_SERVER_MSG_SIZE_TOO_LARGE = 2,
    UIM_REMOTE_SERVICE_SERVER_MSG_SIZE_TOO_SMALL = 3,
    UIM_REMOTE_SERVICE_SERVER_CONNECT_OK_CALL_ONGOING = 4,
}
