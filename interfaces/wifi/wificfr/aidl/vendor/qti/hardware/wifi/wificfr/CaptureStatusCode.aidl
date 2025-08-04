/**
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.wifi.wificfr;

/**
 * Enum values indicating the status of CSI capture operation(CSI start/stop).
 */
@VintfStability
@Backing(type="int")
enum CaptureStatusCode {
    /**
     * Command executed successfully
     */
    SUCCESS,
    /**
     * Unknown failure occured.
     */
    FAILURE_UNKNOWN,
}
