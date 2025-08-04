/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.ims.configaidlservice;

/*
 * Autoconfig trigger reason types
 */
@VintfStability
@Backing(type="int")
enum AutoConfigTriggerReason {
    AUTOCONFIG_UNSPECIFIED = -1,
    /*
     * reason not supported/unspecified
     */
    AUTOCONFIG_DEFAULT = 0,
    /*
     * default reason code
     */
    AUTOCONFIG_INVALID_TOKEN = 1,
    /*
     * Token becomes invalid
     */
    AUTOCONFIG_INVALID_CREDENTIAL = 2,
    /*
     * Username/password/URL becomes invalid
     */
    AUTOCONFIG_CLIENT_CHANGE = 3,
    /*
     * RCS client has been changed
     */
    AUTOCONFIG_DEVICE_UPGRADE = 4,
    /*
     * Device has been upgraded
     */
    AUTOCONFIG_FACTORY_RESET = 5,
}
