/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.CallType;
import vendor.qti.hardware.radio.ims.CallDomain;
import vendor.qti.hardware.radio.ims.RttMode;
import vendor.qti.hardware.radio.ims.ServiceStatusInfo;
import vendor.qti.hardware.radio.ims.ThreeDimensionalVideoType;

/**
 * CallDetails to store information like call type, call domain etc.
 * Telephony/Lower layers will process CallDetails based on individual default parameters.
 */
@VintfStability
parcelable CallDetails {
    CallType callType = CallType.UNKNOWN;
    CallDomain callDomain = CallDomain.INVALID;
    String[] extras;
    ServiceStatusInfo[] localAbility;
    ServiceStatusInfo[] peerAbility;
    int callSubstate = -1;
    int mediaId = -1;
    int causeCode = -1;
    RttMode rttMode = RttMode.INVALID;
    String sipAlternateUri;
    boolean isVosSupported;
    boolean isCrbtCall;
    boolean isVisualizedVoiceCall;
    ThreeDimensionalVideoType threeDimensionalVideoType = ThreeDimensionalVideoType.NONE;
}
