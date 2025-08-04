/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.uim_remote_client;

@VintfStability
@Backing(type="int")
enum UimRemoteClientErrorCauseType {
    UIM_REMOTE_CARD_ERROR_UNKNOWN = 0,
    UIM_REMOTE_CARD_ERROR_NO_LINK_EST = 1,
    UIM_REMOTE_CARD_ERROR_CMD_TIMEOUT = 2,
    UIM_REMOTE_CARD_ERROR_POWER_DOWN = 3,
    UIM_REMOTE_CARD_ERROR_POWER_DOWN_TELECOM = 4,
}
