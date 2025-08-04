/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.qtiradio;

@VintfStability
@Backing(type="int")
enum SearchType {
    /*
     * Invalid search type
     */
    INVALID = 0,

    /**
     * PLMN and CAG search type
     */
    PLMN_AND_CAG = 1,

    /**
     * PLMN only search type
     */
    PLMN_ONLY = 2,
}
