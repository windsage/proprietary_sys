/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

@VintfStability
@Backing(type="int")
enum FacilityType {
    INVALID,
    /*
     * Calling Line Identification Presentation
     */
    CLIP,
    /*
     * Connected Line Identification Presentation
     */
    COLP,
    /*
     * Bar All Outgoing Calls
     */
    BAOC,
    /*
     * Bar All Outgoing International Calls
     */
    BAOIC,
    /*
     * Bar all Outgoing International Calls except those
     * directed to home PLMN country
     */
    BAOICxH,
    /*
     * Bar All Incoming Calls
     */
    BAIC,
    /*
     * Bar All Incoming Calls when Roaming outside
     * the home PLMN country
     */
    BAICr,
    /*
     * Bar All incoming & outgoing Calls
     */
    BA_ALL,
    /*
     * Bar All Outgoing Calls
     */
    BA_MO,
    /*
     * Bar All Incming Calls
     */
    BA_MT,
    /*
     * Bar Incoming Calls from specific DN
     */
    BS_MT,
    /*
     * Bar All Incoming Calls from Anonymous numbers
     */
    BAICa,
}
