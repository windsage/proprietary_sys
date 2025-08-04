/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.uim_remote_server;

@VintfStability
@Backing(type="int")
enum UimRemoteServiceServerDisconnectType {
    UIM_REMOTE_SERVICE_SERVER_DISCONNECT_GRACEFUL = 0,
    UIM_REMOTE_SERVICE_SERVER_DISCONNECT_IMMEDIATE = 1,
}
