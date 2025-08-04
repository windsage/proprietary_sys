/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.uim_remote_client;

@VintfStability
@Backing(type="int")
enum UimRemoteClientEventStatusType {
    UIM_REMOTE_STATUS_CONN_UNAVAILABLE = 0,
    UIM_REMOTE_STATUS_CONN_AVAILABLE = 1,
    UIM_REMOTE_STATUS_CARD_INSERTED = 2,
    UIM_REMOTE_STATUS_CARD_REMOVED = 3,
    UIM_REMOTE_STATUS_CARD_ERROR = 4,
    UIM_REMOTE_STATUS_CARD_RESET = 5,
    UIM_REMOTE_STATUS_CARD_WAKEUP = 6,
}
