/*
 * Copyright (c) 2021, 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.RadioTechType;
import vendor.qti.hardware.radio.ims.SmsSendStatus;
import vendor.qti.hardware.radio.ims.SmsSendFailureReason;

/**
 * Data structure to store sms related information when send sms
 * response is sent from lower layers.
 */
@VintfStability
parcelable SmsSendResponse {
    int msgRef = -1;
    SmsSendStatus smsStatus = SmsSendStatus.INVALID;
    SmsSendFailureReason reason = SmsSendFailureReason.INVALID;
    int networkErrorCode = -1;
    int transportErrorCode = -1;
    RadioTechType radioTech = RadioTechType.INVALID;
}
