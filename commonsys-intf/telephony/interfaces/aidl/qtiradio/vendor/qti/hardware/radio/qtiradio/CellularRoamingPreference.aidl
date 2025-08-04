/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.qtiradio;

import vendor.qti.hardware.radio.qtiradio.CellularRoamingMode;

@VintfStability
@JavaDerive(toString=true)
parcelable CellularRoamingPreference {
    /**
     * The international cellular roaming preference
     */
    CellularRoamingMode internationalCellularRoaming;

    /**
     * The domestic cellular roaming preference
     */
    CellularRoamingMode domesticCellularRoaming;
}