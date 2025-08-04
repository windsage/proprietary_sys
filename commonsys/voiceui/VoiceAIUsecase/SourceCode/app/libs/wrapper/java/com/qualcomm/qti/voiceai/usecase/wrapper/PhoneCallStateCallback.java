/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries. All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.voiceai.usecase.wrapper;

import android.content.Context;
import android.telephony.TelephonyCallback;

import android.telephony.TelephonyCallback.PreciseCallStateListener;
import android.telephony.PreciseCallState;

final public class PhoneCallStateCallback extends TelephonyCallback
        implements PreciseCallStateListener {

    public static final int PRECISE_CALL_STATE_NOT_VALID =      -1;
    public static final int PRECISE_CALL_STATE_IDLE =           0;
    public static final int PRECISE_CALL_STATE_ACTIVE =         1;
    public static final int PRECISE_CALL_STATE_HOLDING =        2;
    public static final int PRECISE_CALL_STATE_DIALING =        3;
    public static final int PRECISE_CALL_STATE_ALERTING =       4;
    public static final int PRECISE_CALL_STATE_INCOMING =       5;
    public static final int PRECISE_CALL_STATE_WAITING =        6;
    public static final int PRECISE_CALL_STATE_DISCONNECTED =   7;
    public static final int PRECISE_CALL_STATE_DISCONNECTING =  8;

    private final Context mContext;
    private final PhoneCallStateCallbackWrapper.Listener mListener;

    public PhoneCallStateCallback(Context context, PhoneCallStateCallbackWrapper.Listener listener) {
        mContext = context;
        mListener = listener;
    }

    @Override
    public void onPreciseCallStateChanged(PreciseCallState callState) {
        int ringingState = callState.getRingingCallState();
        int foregroundState = callState.getForegroundCallState();
        int state = ringingState | foregroundState;
        switch (state) {
            case PreciseCallState.PRECISE_CALL_STATE_IDLE:
                onIdle();
                break;
            case PreciseCallState.PRECISE_CALL_STATE_INCOMING:
                onRinging();
                break;
            case PreciseCallState.PRECISE_CALL_STATE_ALERTING:
                onAlerting();
                break;
            case PreciseCallState.PRECISE_CALL_STATE_ACTIVE:
                onActive();
                break;
            default:
                sendState(state);
                break;
        }
    }

    private void onIdle() {
        if(mListener != null) {
            mListener.onIdle();
        }
    }

    private void onRinging() {
        if(mListener != null) {
            mListener.onRinging();
        }
    }

    private void onAlerting() {
        if(mListener != null) {
            mListener.onAlerting();
        }
    }

    private void onActive() {
        if(mListener != null) {
            mListener.onActive();
        }
    }

    private void sendState(int state) {
        if(mListener != null) {
            mListener.onStateChange(state);
        }
    }
}
