/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries. All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.voiceai.usecase;

import android.app.Application;
import android.util.Log;

public final class UsecaseApplication extends Application {

    private static final String TAG = UsecaseApplication.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "UsecaseApplication onCreate()");
        Facade.register(getApplicationContext());
        Facade.getTranslationManager().downloadAllModels();
    }
}
