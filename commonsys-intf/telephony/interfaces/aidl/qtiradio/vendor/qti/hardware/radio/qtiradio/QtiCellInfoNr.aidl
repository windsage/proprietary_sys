/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.qtiradio;

import vendor.qti.hardware.radio.qtiradio.NrSignalStrength;

import vendor.qti.hardware.radio.qtiradio.QtiCellIdentityNr;

@VintfStability
@JavaDerive(toString=true)
parcelable QtiCellInfoNr {

    /**
     * Information representing NR 5G cell.
     */
    QtiCellIdentityNr cellIdentityNr;

    /**
     * SignalStrength parameters for NR.
     */
    NrSignalStrength signalStrengthNr;

}
