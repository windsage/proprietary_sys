/**
 * Copyright (c) 2018, Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qti.xdivert;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.telephony.TelephonyManager;


public class XDivertService extends Service {

    private XDivertUtility mXDivertUtility;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mXDivertUtility);
    }

    @Override
    public int onStartCommand(Intent intent, int id, int startId) {
        mXDivertUtility = XDivertUtility.init(this);
        IntentFilter filter = new IntentFilter(TelephonyManager
                .ACTION_SIM_APPLICATION_STATE_CHANGED);
        filter.addAction(TelephonyManager.ACTION_MULTI_SIM_CONFIG_CHANGED);
        this.registerReceiver(mXDivertUtility, filter);
        return START_STICKY;
    }
}

