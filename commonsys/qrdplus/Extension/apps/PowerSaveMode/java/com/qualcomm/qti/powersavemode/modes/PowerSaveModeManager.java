/**
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.powersavemode.modes;

import android.os.SystemProperties;
import android.util.BoostFramework;
import android.util.Log;

public class PowerSaveModeManager {
    private static final String TAG = PowerSaveModeManager.class.getSimpleName();
    private static final String PROP_POWER_SAVE_MODE_SUPPORT
            = "ro.vendor.power.tuning.support";
    private static final int POWER_SAVE_MODE_HINT = 0x000015E0;
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);
    private static final PowerSaveModeManager sInstance = new PowerSaveModeManager();

    private BoostFramework mBoostFramework;

    public static PowerSaveModeManager getInstance() {
        return sInstance;
    }

    private PowerSaveModeManager() {
        mBoostFramework = new BoostFramework();
    }

    public int turnOnPowerSaveMode(String packageName, int hintType) {
        int handle = -1;
        if (DEBUG) {
            Log.d(TAG, "turnOnPowerSaveMode: hint = " + hintType);
        }
        if (hintType > 0) {
            handle = mBoostFramework.perfHint(POWER_SAVE_MODE_HINT, packageName,
                    Integer.MAX_VALUE, hintType);
        }
        if (DEBUG) {
            Log.d(TAG, "turnOnPowerSaveMode: turn on handle = " + handle);
        }
        return handle;
    }

    public int turnOffPowerSaveMode(int handle) {
        int ret = -1;
        if (DEBUG) {
            Log.d(TAG, "turnOffPowerSaveMode: handle = " + handle);
        }
        ret = mBoostFramework.perfLockReleaseHandler(handle);
        if (DEBUG) {
            Log.d(TAG, "turnOffPowerSaveMode: return ret = " + ret);
        }
        return ret;
    }

    public boolean isPowerSaveModeSupport() {
        return SystemProperties.getBoolean(PROP_POWER_SAVE_MODE_SUPPORT, false);
    }
}
