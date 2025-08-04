/**
 * Copyright (c) 2013,2017,2022-2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.atfwd2;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.IHwBinder;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Log;

public class AtFwdService extends Service {

    private static final String TAG = "AtFwdService2";
    private static AtFwdAidlClient mAtFwdAidlClient;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate method");
        try {
            mAtFwdAidlClient = new AtFwdAidlClient(this);
        } catch (Throwable e) {
            Log.e(TAG, "Starting AtCmdFwd Service", e);
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "AtFwdService Destroyed Successfully...");
        super.onDestroy();
    }
}
