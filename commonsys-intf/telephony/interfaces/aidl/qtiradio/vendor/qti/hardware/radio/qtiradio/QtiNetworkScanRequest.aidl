/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.qtiradio;

import vendor.qti.hardware.radio.qtiradio.NetworkScanRequest;

import vendor.qti.hardware.radio.qtiradio.AccessMode;
import vendor.qti.hardware.radio.qtiradio.SearchType;

@VintfStability
@JavaDerive(toString=true)
parcelable QtiNetworkScanRequest {

    /**
     * NetworkScanRequest super implementation.
     */
    NetworkScanRequest nsr;

    /**
     * AccessMode can be PLMN or SNPN
     */
    AccessMode accessMode;

    /**
     * searchType can be PLMN_ONLY or PLMN_AND_CAG
     */
    SearchType searchType;
}
