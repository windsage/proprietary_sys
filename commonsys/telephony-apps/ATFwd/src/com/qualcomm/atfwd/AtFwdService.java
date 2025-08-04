/*
 * Copyright (c) 2013, 2017, 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.atfwd;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.IHwBinder;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.Pair;

public class AtFwdService extends Service {

    private static final String TAG = "AtFwdService";
    private static AtCmdFwd mAtCmdFwdHidl;
    private static AtCmdFwdAidl mAtCmdFwdAidl;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate method");
        try {
            if (isBoardApiLevelMatched()) {
                mAtCmdFwdAidl = new AtCmdFwdAidl(this);
                ServiceManager.addService(
                        "vendor.qti.hardware.radio.atcmdfwd.IAtCmdFwd/AtCmdFwdAidl",
                        mAtCmdFwdAidl);
            } else {
                mAtCmdFwdHidl = new AtCmdFwd(this);
                mAtCmdFwdHidl.registerAsService("AtCmdFwdService");
            }
        } catch (Throwable e) {
            Log.e(TAG, "Starting AtCmdFwd Service", e);
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "AtFwdService Destroyed Successfully...");
        super.onDestroy();
    }

    private int makeRadioVersion(int major, int minor) {
        if (major < 0 || minor < 0) return 0;
        return major * 100 + minor;
    }

    private boolean isAidlHalAvailable() {
        final int RADIO_HAL_VERSION_1_6 = makeRadioVersion(1, 6); // Radio HAL Version S

        TelephonyManager telephonyManager = getSystemService(TelephonyManager.class);
        if (telephonyManager == null) {
            Log.e(TAG, "TelephonyManger is NULL");
            return false;
        }
        Pair<Integer, Integer> radioVersion = telephonyManager.getRadioHalVersion();
        int halVersion = makeRadioVersion(radioVersion.first, radioVersion.second);

        Log.d(TAG, "isAidlHalAvailable: halVersion = " + halVersion);
        if (halVersion > RADIO_HAL_VERSION_1_6) {
            return true;
        }
        return false;
    }

    /**
     * Finds the board api level or product api level by reading systemproperty.
     * Returns true if one of the api level is 33 Android-T.
     */
    boolean isBoardApiLevelMatched() {
        int apiLevel = SystemProperties.getInt("ro.board.api_level", 0);
        Log.d(TAG, "isBoardApiLevelMatched: apiLevel= " + apiLevel);
        if ((apiLevel > Build.VERSION_CODES.S_V2) ||
                ((apiLevel == 0) && isAidlHalAvailable())) {
            Log.d(TAG, "isBoardApiLevelMatched: true...");
            return true;
        }
        return false;
    }
}
