/**
 * Copyright (c) 2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.wifi.wifilearner;

@VintfStability
@Backing(type="int")
enum ChannelSurveyInfoMask {
    SURVEY_HAS_NOISE = 1 << 0,
    SURVEY_HAS_CHAN_TIME = 1 << 1,
    SURVEY_HAS_CHAN_TIME_BUSY = 1 << 2,
    SURVEY_HAS_CHAN_TIME_EXT_BUSY = 1 << 3,
    SURVEY_HAS_CHAN_TIME_RX = 1 << 4,
    SURVEY_HAS_CHAN_TIME_TX = 1 << 5,
    SURVEY_HAS_CHAN_TIME_SCAN = 1 << 6,
}
