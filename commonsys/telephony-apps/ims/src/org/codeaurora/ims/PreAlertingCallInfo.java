/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package org.codeaurora.ims;

/* This class is responsible to cache the callcomposer and ecnam information received as part of
 * prealerting indication.
 */

public final class PreAlertingCallInfo {
    // Call Id
    private int mCallId;
    // Call composer information
    CallComposerInfo mCcInfo;
    // ecnam information
    EcnamInfo mEcnamInfo;
    //Data channel call information
    private int mModemCallId;
    private boolean mIsDcCall;

    public PreAlertingCallInfo() {}

    public PreAlertingCallInfo(int id, CallComposerInfo ccInfo) {
        this(id, ccInfo, null, -1, false);
    }

    public PreAlertingCallInfo(int id, EcnamInfo ecnamInfo) {
        this(id, null, ecnamInfo, -1, false);
    }

    public PreAlertingCallInfo(int id, int modemCallId, boolean isDcCall) {
        this(id, null, null, modemCallId, isDcCall);
    }

    public PreAlertingCallInfo(int id, CallComposerInfo ccInfo, EcnamInfo ecnamInfo,
            int modemCallId, boolean isDcCal) {
        mCallId = id;
        mCcInfo = ccInfo;
        mEcnamInfo = ecnamInfo;
        mModemCallId = modemCallId;
        mIsDcCall = isDcCal;
    }

    /**
     * Method used to return callId.
     */
    public int getCallId() {
        return mCallId;
    }

    /**
     * Method used to return CallComposer Information.
     */
    public CallComposerInfo getCallComposerInfo() {
        return mCcInfo;
    }

    /**
     * Method used to return Ecnam Information.
     */
    public EcnamInfo getEcnamInfo() {
        return mEcnamInfo;
    }

    /**
     * Method used to return Data Channel Call Information - modemCallId.
     */
    public int getModemCallId() {
        return mModemCallId;
    }
    /**
     * Method used to return Data Channel Call Information - isDcCall.
     */
    public boolean getIsDcCall() {
        return mIsDcCall;
    }

    public String toString() {
        return "PreAlertingCallInfo CallId: " + mCallId + " CallComposerInfo: " + mCcInfo +
                " EcnamInfo: " + mEcnamInfo + " ModemCallId: "+ mModemCallId +
                " IsDcCall: " + mIsDcCall;
    }
}
