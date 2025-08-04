/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.qtiradio;

import vendor.qti.hardware.radio.qtiradio.QtiCellInfo;

@VintfStability
@JavaDerive(toString=true)
parcelable QtiNetworkScanResult {

    /**
     * The result contains a part of the scan results.
     */
    const int SCAN_STATUS_PARTIAL = 1;

    /**
     * The result contains the last part of the scan results.
     */
    const int SCAN_STATUS_COMPLETE = 2;

    /**
     * The status of the scan.
     * Values are SCAN_STATUS_
     */
    int status;

    /**
     * The error code of the incremental result.
     */
    int error;

    /**
     * List of network information as QtiCellInfo.
     */
    QtiCellInfo[] networkInfos;

}
