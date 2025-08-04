/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.qtiradio;

import vendor.qti.hardware.radio.qtiradio.CellInfoCdma;
import vendor.qti.hardware.radio.qtiradio.CellInfoGsm;
import vendor.qti.hardware.radio.qtiradio.CellInfoLte;
import vendor.qti.hardware.radio.qtiradio.CellInfoTdscdma;
import vendor.qti.hardware.radio.qtiradio.CellInfoWcdma;

import vendor.qti.hardware.radio.qtiradio.QtiCellInfoNr;

@VintfStability
@JavaDerive(toString=true)
union QtiCellInfoRatSpecificInfo {
    /**
     * 3gpp CellInfo types.
     */
    CellInfoGsm gsm;
    CellInfoWcdma wcdma;
    CellInfoTdscdma tdscdma;
    CellInfoLte lte;
    QtiCellInfoNr nr;
    /**
     * 3gpp2 CellInfo types;
     */
    CellInfoCdma cdma;
}
