/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.qtiradio;


import vendor.qti.hardware.radio.qtiradio.CellConnectionStatus;
import vendor.qti.hardware.radio.qtiradio.QtiCellInfoRatSpecificInfo;

@VintfStability
@JavaDerive()
parcelable QtiCellInfo {

    /**
     * True if this cell is registered false if not registered.
     */
    boolean registered;

    /**
     * Connection status for the cell.
     */
    CellConnectionStatus connectionStatus;

    /**
     * Cell information corresponding to each RAT.
     */
    QtiCellInfoRatSpecificInfo ratSpecificInfo;
}
