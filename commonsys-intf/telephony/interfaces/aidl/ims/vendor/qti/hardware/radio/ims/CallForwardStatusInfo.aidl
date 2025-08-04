/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.CallForwardStatus;
import vendor.qti.hardware.radio.ims.SipErrorInfo;

/**
 * Data structure comtaining status and error details
 * for all call forwarding requests.
 */
@VintfStability
parcelable CallForwardStatusInfo {
    SipErrorInfo errorDetails;
    CallForwardStatus[] status;
}

