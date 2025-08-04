/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.ClipStatus;
import vendor.qti.hardware.radio.ims.SipErrorInfo;

/**
 * Data structure to indicate status of clip provisions.
 * Telephony will process ClipProvisionStatus if ClipProvisionStatus#ClipStatus
 * is not ClipStatus#INVALID.
 */
@VintfStability
parcelable ClipProvisionStatus {
    ClipStatus clipStatus = ClipStatus.INVALID;
    SipErrorInfo errorDetails;
}

