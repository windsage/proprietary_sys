/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.ims.configaidlservice;

@VintfStability
@Backing(type="int")
enum StandaloneMessagingConfigKeys {
    /**
     * This key expects uint8_t value
     */
    DEFAULT_SMS_APP_KEY = 300,
    /**
     * This key expects uint8_t value
     */
    DEFAULT_VVM_APP_KEY,
    /**
     * This key expects string value
     */
    AUTO_CONFIG_USER_AGENT_KEY,
    /**
     * This key expects string value
     */
    XDM_CLIENT_USER_AGENT_KEY,
    /**
     * This key expects string value
     */
    CLIENT_VENDOR_KEY,
    /**
     * This key expects string value
     */
    CLIENT_VERSION_KEY,
    /**
     * This key expects string value
     */
    TERMINAL_VENDOR_KEY,
    /**
     * This key expects string value
     */
    TERMINAL_MODEL_KEY,
    /**
     * This key expects string value
     */
    TERMINAL_SW_VERSION_KEY,
    /**
     * This key expects string value
     */
    RCS_VERSION_KEY,
    /**
     * This key expects string value
     */
    PROVISIONING_VERSION_KEY,
    /**
     * This key expects string value
     */
    FRIENDLY_DEVICE_NAME_KEY,
    /**
     * This key expects string value
     */
    RCS_PROFILE_KEY,
    /**
     * This key expects string value
     */
    BOT_VERSION_KEY,
    /**
     * This key expects string value
     *
     *
     * Comma separated list of AppID values each corresponding to an application client supports.
     * If not set, the default is ap2002
     */
    APP_ID_KEY = 314,
}
