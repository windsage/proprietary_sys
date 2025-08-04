/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.telephonyservice;
import android.util.Log;

/**
 * Default HAL class that is invoked when no IQcRilAudio HAL is available.
 * Typical use case for this when the target does not support Telephony/RIL.
 */
public class QcRilAudioNotSupportedHal implements IAudioControllerCallback {

    private final String TAG = "QcRilAudioNotSupportedHal";

    private void fail() {
        Log.e(TAG, "Radio is not supported");
    }

    // Implementation of IImsRadio java interface where all methods throw an exception
    @Override
    public void onAudioStatusChanged(int status) {
        fail();
    }

    @Override
    public void onDispose() {
        fail();
    }
}
