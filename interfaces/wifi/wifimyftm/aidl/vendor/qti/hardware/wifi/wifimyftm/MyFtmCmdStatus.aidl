/**
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.wifi.wifimyftm;

/**
 * wifimyftm  status codes.
 */
@VintfStability
@Backing(type="int")
enum MyFtmCmdStatus {
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
