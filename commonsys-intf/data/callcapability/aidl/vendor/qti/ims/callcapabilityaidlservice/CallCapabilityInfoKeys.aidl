/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.ims.callcapabilityaidlservice;

@VintfStability
@Backing(type="int")
enum CallCapabilityInfoKeys {
    /**
     * This key expects uint8_t value, expects 0 or 1
     */
    PEER_CALL_COMPOSER_CAPABILITY_SUPPORTED_KEY = 1,
    /**
     * This key expects uint8_t value, expects 0 or 1
     */
    RTT_CAPABILITY_SUPPORTED_KEY,
    /**
     * This key expects uint8_t value, expects 0 or 1
     */
    CONF_CAPABILITY_SUPPORTED_KEY,
    /**
     * This key expects string value, expects "TX","RX","BOTH"
     */
    VIDEO_CAPABILITY_SUPPORTED_KEY,
    /**
     * This key expects string value, expects "TX","RX","BOTH"
     */
    AUDIO_CAPABILITY_SUPPORTED_KEY,
}
