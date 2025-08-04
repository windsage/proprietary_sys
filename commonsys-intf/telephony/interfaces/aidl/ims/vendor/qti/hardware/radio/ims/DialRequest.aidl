/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.IpPresentation;
import vendor.qti.hardware.radio.ims.CallDetails;
import vendor.qti.hardware.radio.ims.ClirMode;
import vendor.qti.hardware.radio.ims.MultiIdentityLineInfo;
import vendor.qti.hardware.radio.ims.RedialInfo;

/**
 * DialRequest is used to initiate VoLTE/VT call.
 * Lower layers will process DialRequest based on
 *         DialRequest#address is not empty for normal calls.
 *         isConferenceUri is true.
 */
@VintfStability
parcelable DialRequest {
    String address;
    ClirMode clirMode = ClirMode.INVALID;
    CallDetails callDetails;
    boolean isConferenceUri;
    boolean isCallPull;
    boolean isEncrypted;
    /* MultiIdentityLineInfo is used to support MultiIdentity line registration */
    MultiIdentityLineInfo multiLineInfo;
    /*
     * redialInfo will be sent when lower layers end the call asking
     * Android Telephony to redial. Decision to redial the call or not
     * is dependent on specific call fail causes that are agreed
     * with modem.
     */
    RedialInfo redialInfo;
}

