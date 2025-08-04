/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

@VintfStability
@Backing(type="int")
enum GeoLocationDataStatus {
    /*
    * Default value.
    */
    INVALID,
    /*
     * Received when all previously reported
     * geo location issues are resolved.
     */
    RESOLVED,
    /*
     * Received when time out to get the Longitude and Latitude
     * from GPS engine.
     */
    TIMEOUT,
    /*
     * Received when Telephony is unable to provide the relevant
     * information (ex: country code) when sending geo location information
     * with sendGeolocationInfo_1_2.
     */
    NO_CIVIC_ADDRESS,
    /*
     * Received if GPS is disabled from UI.
     */
    ENGINE_LOCK,
}
