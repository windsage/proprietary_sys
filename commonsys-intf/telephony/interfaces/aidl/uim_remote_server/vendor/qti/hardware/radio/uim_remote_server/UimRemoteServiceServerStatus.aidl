/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.uim_remote_server;

@VintfStability
@Backing(type="int")
enum UimRemoteServiceServerStatus {
    UIM_REMOTE_SERVICE_SERVER_UNKNOWN_ERROR = 0,
    UIM_REMOTE_SERVICE_SERVER_CARD_RESET = 1,
    UIM_REMOTE_SERVICE_SERVER_CARD_NOT_ACCESSIBLE = 2,
    UIM_REMOTE_SERVICE_SERVER_CARD_REMOVED = 3,
    UIM_REMOTE_SERVICE_SERVER_CARD_INSERTED = 4,
    UIM_REMOTE_SERVICE_SERVER_RECOVERED = 5,
}
