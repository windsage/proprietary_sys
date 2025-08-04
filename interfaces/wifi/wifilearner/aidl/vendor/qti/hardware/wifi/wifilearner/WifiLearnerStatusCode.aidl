/**
 * Copyright (c) 2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.wifi.wifilearner;

@VintfStability
@Backing(type="int")
enum WifiLearnerStatusCode {
    /**
     * No errors.
     */
    SUCCESS,
    /**
     * Unknown failure occured.
     */
    FAILURE_UNKNOWN,
    /**
     * One of the incoming args is invalid.
     */
    FAILURE_ARGS_INVALID,
}
