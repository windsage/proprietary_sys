/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.CallType;
import vendor.qti.hardware.radio.ims.IpPresentation;
import vendor.qti.hardware.radio.ims.RttMode;

@VintfStability
parcelable AnswerRequest {
    CallType callType = CallType.UNKNOWN;
    IpPresentation presentation = IpPresentation.INVALID;
    RttMode mode = RttMode.INVALID;
}

