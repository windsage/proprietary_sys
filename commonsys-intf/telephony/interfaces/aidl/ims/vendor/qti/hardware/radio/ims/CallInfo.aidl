/*
 * Copyright (c) 2021-2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.AudioQuality;
import vendor.qti.hardware.radio.ims.CallDetails;
import vendor.qti.hardware.radio.ims.CallFailCauseResponse;
import vendor.qti.hardware.radio.ims.CallProgressInfo;
import vendor.qti.hardware.radio.ims.CallState;
import vendor.qti.hardware.radio.ims.CrsData;
import vendor.qti.hardware.radio.ims.MsimAdditionalCallInfo;
import vendor.qti.hardware.radio.ims.MultiIdentityLineInfo;
import vendor.qti.hardware.radio.ims.TirMode;
import vendor.qti.hardware.radio.ims.VerstatInfo;

@VintfStability
parcelable CallInfo {
    CallState state = CallState.INVALID;
    /*
     * Initialize integer with default value as Integer.MAX_VALUE.
     * Currently not able to initialize due to built-in constants are not supported in stable AIDL.
     * Ref: Google bug 185505930.
     */
    int index;
    int toa;
    boolean isMpty;
    boolean isMT;
    MultiIdentityLineInfo mtMultiLineInfo;
    int als;
    boolean isVoice;
    boolean isVoicePrivacy;
    String number;
    int numberPresentation;
    String name;
    int namePresentation;
    CallDetails callDetails;
    CallFailCauseResponse failCause;
    boolean isEncrypted;
    boolean isCalledPartyRinging;
    String historyInfo;
    boolean isVideoConfSupported;
    VerstatInfo verstatInfo;
    TirMode tirMode = TirMode.INVALID;
    /*
     * True if the message is intended for preliminary resource allocation
     * only and shall not be visible to the end user.
     * Default: False
     */
    boolean isPreparatory;
    CrsData crsData;
    //Call progress info for MO calls during alerting stage.
    CallProgressInfo callProgInfo;
    /*
     * Diversion information to report call forward info will be valid
     * only for incoming calls.
     */
    String  diversionInfo;
    // Additional call info to report additional call fail cause when concurrent calls are
    // not possible.
    @nullable
    MsimAdditionalCallInfo additionalCallInfo;
    // Indicated audio quality, e.g., HD, HD+, of the call.
    @nullable
    AudioQuality audioQuality;
    int modemCallId;
    // Set by RIL when the call is an emergency call.
    boolean isEmergency;
    // Set during incoming call only if the caller has filled this field.
    @nullable
    String callReason;
}
