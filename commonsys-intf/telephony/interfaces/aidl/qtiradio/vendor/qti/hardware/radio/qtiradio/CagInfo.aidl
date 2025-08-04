/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.qtiradio;

@VintfStability
@JavaDerive(toString=true)
parcelable CagInfo {

    /**
     * Common Access Group Name corresponding to CAG Cell.
     */
    String cagName;

    /**
     * CAG ID of CAG Cell.
     */
    long cagId;

    /**
     * Indicates if PLMN is CAG only access.
     */
    boolean cagOnlyAccess;

    /**
     * Indicates the presence of [PLMN, CAG_ID] combination in the CAG info list.
     */
    boolean cagInAllowedList;
}