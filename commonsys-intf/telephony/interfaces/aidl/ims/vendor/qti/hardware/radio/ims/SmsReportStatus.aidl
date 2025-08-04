/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

@VintfStability
@Backing(type="int")
enum SmsReportStatus {
    /**
     * Default value
     */
    INVALID,
    /**
     * Status Report was set successfully.
     */
    OK,
    /**
     * Error while setting status report
     */
    ERROR,
}
