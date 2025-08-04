/**
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.powersavemode;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import com.qualcomm.qti.powersavemode.modes.PowerSaveModeManager;

public class SystemEventReceiver extends BroadcastReceiver {

    private static final String TAG = SystemEventReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Context appContext = context.getApplicationContext();
        Log.d(TAG, "onReceive: action = " + action);

        if (Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)) {
            SharedPreferenceUtils.saveSessionHandle(appContext, -1);
            boolean isSupport = PowerSaveModeManager.getInstance().isPowerSaveModeSupport();
            int hintType = SharedPreferenceUtils.getPowerSaveModeHintType(appContext);
            if (isSupport && hintType != -1) {
                tryTurnOnPowerSaveMode(appContext, hintType);
            }
            enablePowerSaveModeActivity(appContext, isSupport);
        }
    }

    private void tryTurnOnPowerSaveMode(Context appContext, int hintType) {
        PowerSaveModeManager modeManager = PowerSaveModeManager.getInstance();

        int savedHandle = SharedPreferenceUtils.getSessionHandle(appContext);
        if (savedHandle != -1) {
            modeManager.turnOffPowerSaveMode(savedHandle);
        }

        int handle = modeManager.turnOnPowerSaveMode(appContext.getPackageName(), hintType);
        if (handle != -1) {
            SharedPreferenceUtils.saveSessionHandle(appContext, handle);
        }
    }

    private void enablePowerSaveModeActivity(Context context, boolean enable) {
        Log.d(TAG, "enablePowerSaveModeActivity: enable = " + enable);
        PackageManager pm = context.getPackageManager();
        ComponentName name = new ComponentName(context, PowerSaveModeActivity.class);
        if (enable) {
            pm.setComponentEnabledSetting(name, PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
        } else {
            // kill the app when power save mode feature is not supported
            pm.setComponentEnabledSetting(name,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0);
        }
    }
}
