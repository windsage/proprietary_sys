/**
 * Copyright (c) 2013,2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.atfwd2;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Log;

public class AtFwdAutoboot extends BroadcastReceiver {

    private static final String TAG = "AtFwd2 AutoBoot";
    String action = "android.intent.action.BOOT_COMPLETED";
    static final String ATFWD_CLIENT_SERVICE_NAME =
            "vendor.qti.hardware.radio.atfwd.IAtFwd/AtFwdAidl";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (action.equals(intent.getAction())) {
            try {
                if (isBoardApiLevelMatched() &&
                            ServiceManager.isDeclared(ATFWD_CLIENT_SERVICE_NAME)) {
                    ComponentName comp = new ComponentName(context.getPackageName(),
                            AtFwdService.class.getName());
                    ComponentName service = context.startService(new Intent().setComponent(comp));
                    if (service == null) {
                        Log.e(TAG, "Could not start service: " + comp.toString());
                    } else {
                        Log.e(TAG, "AtFwd auto boot started successfully: " + comp.toString());
                    }
                } else {
                    Log.e(TAG, "IAtFwd service is not declared.");
                }
            } catch (SecurityException ex) {
                Log.e(TAG, "Caught security exception while querying for declared service." + ex);
                return;
            }
        } else {
            Log.e(TAG, "Received Intent: " + intent.toString());
        }
    }

    private boolean isBoardApiLevelMatched() {
        int apiLevel = SystemProperties.getInt("ro.board.api_level", 0);
        int apiProdLevel = SystemProperties.getInt("ro.product.first_api_level", 0);
        Log.d(TAG, "isBoardApiLevelMatched: apiLevel= " + apiLevel +
                " ProdLevel= " + apiProdLevel);
        if (apiLevel >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
                ((apiLevel == 0) && (apiProdLevel >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE))) {
            Log.d(TAG, "isBoardApiLevelMatched: true...");
            return true;
        }
        return false;
    }
}
