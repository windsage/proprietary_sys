/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.voiceai.usecase.wrapper;

import android.content.Context;

public class PhoneCallStateCallbackWrapper {
    public interface Listener {

        default void onStateChange(int state) {
        }

        void onIdle();

        void onRinging();

        void onAlerting();

        void onActive();
    }

    PhoneCallStateCallback mCallback;

    public PhoneCallStateCallbackWrapper(Context context,Listener listener) {
        mCallback = new PhoneCallStateCallback(context, listener);
    }
}
