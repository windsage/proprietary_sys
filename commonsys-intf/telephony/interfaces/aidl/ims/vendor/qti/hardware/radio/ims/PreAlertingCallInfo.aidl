/*
 * Copyright (c) 2021-2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.CallComposerInfo;
import vendor.qti.hardware.radio.ims.EcnamInfo;

/**
 * PreAlertingCallInfo is used to indicate pre alerting call information.
 * Telephony/Lower layers will process CallComposerInfo/EcnamInfo based on non null and
 * individual parameters, subject if not empty, imageUri if not empty etc.
 */
@VintfStability
parcelable PreAlertingCallInfo {
    int callId;
    @nullable
    CallComposerInfo callComposerInfo;
    @nullable
    EcnamInfo ecnamInfo;
    int modemCallId;
    boolean isDcCall = false;
}
