/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.uim_remote_server;

@VintfStability
@Backing(type="int")
enum UimRemoteServiceServerResultCode {
    UIM_REMOTE_SERVICE_SERVER_SUCCESS = 0,
    UIM_REMOTE_SERVICE_SERVER_GENERIC_FAILURE = 1,
    UIM_REMOTE_SERVICE_SERVER_CARD_NOT_ACCESSSIBLE = 2,
    UIM_REMOTE_SERVICE_SERVER_CARD_ALREADY_POWERED_OFF = 3,
    UIM_REMOTE_SERVICE_SERVER_CARD_REMOVED = 4,
    UIM_REMOTE_SERVICE_SERVER_CARD_ALREADY_POWERED_ON = 5,
    UIM_REMOTE_SERVICE_SERVER_DATA_NOT_AVAILABLE = 6,
    UIM_REMOTE_SERVICE_SERVER_NOT_SUPPORTED = 7,
}
