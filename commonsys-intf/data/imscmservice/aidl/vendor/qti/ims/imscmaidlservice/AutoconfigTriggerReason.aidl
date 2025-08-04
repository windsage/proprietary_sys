/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/
package vendor.qti.ims.imscmaidlservice;

/*
 * Autoconfig trigger reason types
 */
@VintfStability
@Backing(type="int")
enum AutoconfigTriggerReason {
    AUTOCONFIG_USER_REQUEST = 0,
    /*
     * Due to user request after version -2 received
     */
    AUTOCONFIG_REFRESH_TOKEN = 1,
    /*
     * Refresh Token
     */
    AUTOCONFIG_INVALID_TOKEN = 2,
    /*
     * Token becomes invalid
     */
    AUTOCONFIG_INVALID_CREDENTIAL = 3,
    /*
     * Username/password/URL becomes invalid
     */
    AUTOCONFIG_CLIENT_CHANGE = 4,
    /*
     * RCS client has been changed
     */
    AUTOCONFIG_DEVICE_UPGRADE = 5,
    /*
     * Device has been upgraded
     */
    AUTOCONFIG_FACTORY_RESET = 6,
}
