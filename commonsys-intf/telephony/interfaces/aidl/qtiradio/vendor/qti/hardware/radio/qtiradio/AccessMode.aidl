/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.qtiradio;

@VintfStability
@Backing(type="int")
enum AccessMode {
    /*
     * Invalid Access Mode
     */
    INVALID = 0,

    /**
     * PLMN Access Mode
     */
    PLMN = 1,

    /**
     * SNPN Access Mode
     */
    SNPN = 2,
}
