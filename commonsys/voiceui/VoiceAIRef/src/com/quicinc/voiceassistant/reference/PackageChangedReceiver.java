/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.quicinc.voiceassistant.reference;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import com.quicinc.voiceassistant.reference.controller.SoundModelFilesManager;
import com.quicinc.voiceassistant.reference.util.LogUtils;
import com.quicinc.voice.assist.sdk.utility.AbstractConnector;
import com.quicinc.voice.assist.sdk.voiceactivation.VoiceActivationConnector;

public class PackageChangedReceiver extends BroadcastReceiver {
    private static final String TAG = PackageChangedReceiver.class.getSimpleName();
    private static final String KEY_EXTRA_STATE ="EXTRA_STATE";
    private static final String KEY_EXTRA_UID ="EXTRA_UID";
    private static final String DATA_CLEARED = "DATA_CLEARED";

    @Override
    public void onReceive(Context context, Intent intent) {
        String extraState = intent.getStringExtra(KEY_EXTRA_STATE);
        int extraUid = intent.getIntExtra(KEY_EXTRA_UID, 0);
        int uid = context.getApplicationInfo().uid;
        LogUtils.d(TAG, "extraState = " + extraState + " extraUid = " + extraUid
                + " Uid = " + uid);
        if (DATA_CLEARED.equals(extraState) && extraUid == uid) {
            VoiceActivationConnector connector = new VoiceActivationConnector(context.getApplicationContext());
            connector.connect(new AbstractConnector.ServiceConnectListener() {
                @Override
                public void onServiceConnected() {
                    SoundModelFilesManager.getInstance(context.getApplicationContext()).releaseRecognitions(connector);
                    connector.disconnect();
                }

                @Override
                public void onServiceDisConnected(ComponentName name) {

                }
            });
        }
    }
}
