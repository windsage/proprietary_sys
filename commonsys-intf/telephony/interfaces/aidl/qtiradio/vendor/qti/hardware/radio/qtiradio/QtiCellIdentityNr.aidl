/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.qtiradio;

import vendor.qti.hardware.radio.qtiradio.CellIdentityNr;

import vendor.qti.hardware.radio.qtiradio.CagInfo;
import vendor.qti.hardware.radio.qtiradio.SnpnInfo;

@VintfStability
@JavaDerive(toString=true)
parcelable QtiCellIdentityNr {

    /**
     * CellIdentityNr super implementation.
     */
    @nullable CellIdentityNr cNr;

    /**
     * SNPN information
     */
    @nullable SnpnInfo snpnInfo;

    /**
     * CAG Cells Information
     */
    @nullable CagInfo cagInfo;

}
