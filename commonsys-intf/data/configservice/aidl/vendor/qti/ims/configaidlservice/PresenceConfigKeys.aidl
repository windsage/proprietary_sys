/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.ims.configaidlservice;

/**
 * Properties that can be set using PRESENCE_CONFIG
 */
@VintfStability
@Backing(type="int")
enum PresenceConfigKeys {
    /**
     * This key expects uint32_t value
     */
    PUBLISH_TIMER_KEY = 100,
    /**
     * This key expects uint32_t value
     */
    PUBLISH_EXTENDED_TIMER_KEY,
    /**
     * This key expects uint32_t value
     */
    PUBLISH_SRC_THROTTLE_TIMER_KEY,
    /**
     * This key expects uint32_t value
     */
    PUBLISH_ERROR_RECOVERY_TIMER_KEY,
    /**
     * This key expects uint32_t value
     */
    LIST_SUBSCRIPTION_EXPIRY_KEY,
    /**
     * This key expects uint32_t value
     */
    CAPABILITES_CACHE_EXPIRY_KEY,
    /**
     * This key expects uint32_t value
     */
    AVAILABILITY_CACHE_EXPIRY_KEY,
    /**
     * This key expects uint32_t value
     */
    CAPABILITY_POLL_INTERVAL_KEY,
    /**
     * This key expects uint32_t value
     */
    MAX_ENTIES_IN_LIST_SUBSCRIBE_KEY,
    /**
     * This key expects uint8_t value, expects 0 or 1
     */
    CAPABILITY_DISCOVERY_ENABLED_KEY,
    /**
     * This key expects uint8_t value, expects 0 or 1
     */
    GZIP_ENABLED_KEY,
    /**
     * This key expects string value
     */
    USER_AGENT_KEY,
}
