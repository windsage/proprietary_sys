/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.CallFailCause;
import vendor.qti.hardware.radio.ims.SipErrorInfo;

@VintfStability
parcelable CallFailCauseResponse {
    CallFailCause failCause = CallFailCause.INVALID;
    byte[] errorInfo;
    String networkErrorString;
    boolean hasErrorDetails;
    SipErrorInfo errorDetails;
}

