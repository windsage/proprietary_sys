/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.SmsReportStatus;

/**
 * Data structure containing acknowledge sms Information such as
 * the message reference and setting status report.
 */
@VintfStability
parcelable AcknowledgeSmsReportInfo {
    int messageRef = -1;
    SmsReportStatus smsReportStatus = SmsReportStatus.INVALID;
}

