/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio;

import vendor.qti.hardware.radio.RadioError;

@VintfStability
parcelable RadioResponseInfo {
    /**
     * Serial number of the request
     */
    int serial;

    /**
     * Response error
     */
    RadioError error;
}
