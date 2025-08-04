/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.SmsDeliverStatus;

/**
 * Data structure containing acknowledge sms Information such as
 * the message reference and status of delivering the message.
 */
@VintfStability
parcelable AcknowledgeSmsInfo {
    int messageRef = -1;
    SmsDeliverStatus smsDeliverStatus = SmsDeliverStatus.INVALID;
}
