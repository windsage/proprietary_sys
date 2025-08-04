/**
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.powersavemode;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferenceUtils {
    private final static String SHARED_PREFERENCES_NAME = "power_save_mode_prefs";
    private static final String POWER_SAVE_MODE_HINT_TYPE = "power_save_mode_hint_type";
    private static final String SESSION_HANDLE = "session_handle";

    private static SharedPreferences getSharedPreferences(Context context) {
        final Context storageContext = context.createDeviceProtectedStorageContext();
        return storageContext.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public static int getPowerSaveModeHintType(Context context) {
        SharedPreferences prefs = getSharedPreferences(context);
        return prefs.getInt(POWER_SAVE_MODE_HINT_TYPE, -1);
    }

    public static void savePowerSaveModeHintType(Context context, int hint) {
        SharedPreferences prefs = getSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(POWER_SAVE_MODE_HINT_TYPE, hint);
        editor.apply();
    }

    public static int getSessionHandle(Context context) {
        SharedPreferences prefs = getSharedPreferences(context);
        return prefs.getInt(SESSION_HANDLE, -1);
    }

    public static void saveSessionHandle(Context context, int handle){
        SharedPreferences prefs = getSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(SESSION_HANDLE, handle);
        editor.apply();
    }
}

