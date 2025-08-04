/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.ims.configaidlservice;

/**
 * Properties that can be set using IMS_SERVICE_ENABLE_CONFIG
 */
@VintfStability
@Backing(type="int")
enum ImsServiceEnableConfigKeys {
    /**
     * This key expects uint8_t value, expects 0 or 1
     */
    VOLTE_ENABLED_KEY = 400,
    /**
     * This key expects uint8_t value, expects 0 or 1
     */
    VIDEOTELEPHONY_ENABLED_KEY,
    /**
     * This key expects uint8_t value, expects 0 or 1
     */
    MOBILE_DATA_ENABLED_KEY,
    /**
     * This key expects uint8_t value, expects 0 or 1
     */
    WIFI_CALLING_ENABLED_KEY,
    /**
     * This key expects uint8_t value, expects 0 or 1
     */
    WIFI_CALLING_IN_ROAMING_ENABLED_KEY,
    /**
     * This key expects uint8_t value, expects 0 or 1
     */
    IMS_SERVICE_ENABLED_KEY,
    /**
     * This key expects uint8_t value, expects 0 or 1
     */
    UT_ENABLED_KEY,
    /**
     * This key expects uint8_t value, expects 0 or 1
     */
    SMS_ENABLED_KEY,
    /**
     * This key expects uint8_t value, expects 0 or 1
     */
    DAN_ENABLED_KEY,
    /**
     * This key expects uint8_t value, expects 0 or 1
     */
    USSD_ENABLED_KEY,
    /**
     * This key expects uint8_t value, expects 0 or 1
     */
    MWI_ENABLED_KEY,
    /**
     * This key expects uint8_t value, expects 0 or 1
     */
    PRESENCE_ENABLED_KEY,
    /**
     * This key expects uint8_t value, expects 0 or 1
     */
    AUTOCONFIG_ENABLED_KEY,
    /**
     * This key expects uint8_t value, expects 0 or 1
     */
    XDM_CLIENT_ENABLED_KEY,
    /**
     * This key expects uint8_t value, expects 0 or 1
     */
    RCS_MESSAGING_ENABLED_KEY,
    /**
     * This key expects uint8_t value, expects 0 or 1
     */
    CALL_MODE_PREF_ROAM_ENABLED_KEY,
    /**
     * This key expects uint8_t value, expects 0 or 1
     */
    RTT_ENABLED_KEY,
    /**
     * This key expects uint8_t value, expects 0 or 1
     */
    CARRIER_CONFIG_ENABLED_KEY,
    /**
     * This key expects string value
     */
    WIFI_PROVISIONING_ID_KEY,
    /**
     * This key expects uint32_t value
     */
    CALL_MODE_PREFERENCE_KEY,
    /**
     * This key expects uint32_t value
     */
    CALL_MODE_ROAM_PREFERENCE_KEY,
    /**
     * This key expects uint64_t value
     */
    SERVICE_MASK_BY_NETWORK_ENABLED_KEY,
    /**
     * This key expects uint8_t value, expects 0 or 1
     */
    OPTIONS_ENABLED_KEY,
    /**
     * This key expects uint8_t value, expects 0 or 1
     */
    CALL_COMPOSER_ENABLED_KEY,
}
