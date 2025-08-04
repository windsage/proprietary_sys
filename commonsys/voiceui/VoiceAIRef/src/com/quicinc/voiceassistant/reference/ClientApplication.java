/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.quicinc.voiceassistant.reference;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.widget.Toast;

import com.quicinc.voiceassistant.reference.controller.SoundModelFilesManager;
import com.quicinc.voiceassistant.reference.util.AppPermissionUtils;
import com.quicinc.voice.assist.sdk.utility.AbstractConnector;
import com.quicinc.voice.assist.sdk.VoiceAssist;
import com.quicinc.voice.assist.sdk.voiceactivation.VoiceActivationConnector;

import java.util.ArrayList;
import java.util.List;

public class ClientApplication extends Application {

    private static ClientApplication sInstance;
    private static List<Activity> mActivities = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = ClientApplication.this;
        VoiceAssist.initialize(getApplicationContext(), getAppFilesDir());
        releaseRecognitionsIfNoPermission();
    }

    public static ClientApplication getInstance() {
        return sInstance;
    }

    public String getAppFilesDir() {
        return getFilesDir().getAbsolutePath();
    }

    public void addActivityInstance(Activity activity) {
        if (!mActivities.contains(activity)) {
            mActivities.add(activity);
        }
    }

    public void removeActivityInstance(Activity activity) {
        mActivities.remove(activity);
    }

    public void finishActivities() {
        Toast.makeText(getApplicationContext(), getString(R.string.permission_deny_tips),
                Toast.LENGTH_SHORT).show();
        for (Activity activity : mActivities) {
            activity.finish();
        }
    }

    private void releaseRecognitionsIfNoPermission() {
        if (AppPermissionUtils.isRuntimePermissionsGranted(this)) {
            return;
        }
        VoiceActivationConnector connector = new VoiceActivationConnector(this);
        connector.connect(new AbstractConnector.ServiceConnectListener() {
            @Override
            public void onServiceConnected() {
                SoundModelFilesManager.getInstance(getApplicationContext()).releaseRecognitions(connector);
                connector.disconnect();
            }

            @Override
            public void onServiceDisConnected(ComponentName name) {

            }
        });

    }
}
