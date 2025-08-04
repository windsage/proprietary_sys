/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.Result;
import vendor.qti.hardware.radio.ims.SipErrorInfo;

@VintfStability
parcelable CallForwardStatus {
    int reason;
    /*
     * CFU, CFB, CFNRc, CFNRy
     */
    Result status = Result.INVALID;
    SipErrorInfo errorDetails;
}

