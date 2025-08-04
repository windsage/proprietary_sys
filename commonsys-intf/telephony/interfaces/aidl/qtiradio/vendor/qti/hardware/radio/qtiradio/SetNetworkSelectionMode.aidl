/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.qtiradio;


import vendor.qti.hardware.radio.qtiradio.AccessNetwork;
import vendor.qti.hardware.radio.qtiradio.AccessMode;
import vendor.qti.hardware.radio.qtiradio.CagInfo;


@VintfStability
@JavaDerive()
parcelable SetNetworkSelectionMode {

    /**
     * Operator Numeric : MccMnc
     */
    String operatorNumeric;

    /**
     * RAT
     */
    AccessNetwork ran;

    /**
     * Current AccessMode {PLMN/SNPN/INVALID}
     */
    AccessMode accessMode;

    /**
     * CAG Cells Information
     */
    @nullable CagInfo cagInfo;

    /**
     * SNPN Network Id.
     */
    byte[] snpnNid;
}
