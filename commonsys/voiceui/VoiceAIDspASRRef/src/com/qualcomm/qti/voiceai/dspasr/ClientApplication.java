/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.voiceai.dspasr;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.widget.Toast;

import com.qualcomm.qti.voiceai.dspasr.util.AppPermissionUtils;

import java.util.ArrayList;
import java.util.List;

public class ClientApplication extends Application {

    private static ClientApplication sInstance;
    private static List<Activity> mActivities = new ArrayList<>();
    private ASRTTSActivity.MyHandler handler;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = ClientApplication.this;

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


    public void setHandler(ASRTTSActivity.MyHandler handler) {
        this.handler = handler;
    }

    public ASRTTSActivity.MyHandler getHandler() {
        return handler;
    }
}
