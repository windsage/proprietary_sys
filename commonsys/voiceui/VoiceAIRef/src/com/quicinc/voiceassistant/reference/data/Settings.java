/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.quicinc.voiceassistant.reference.data;

import android.content.Context;
import android.content.SharedPreferences;

public class Settings {
    private static final String SHARED_PREFERENCES_NAME = "qva_client";
    private static final String KEY_ENABLE_UV = "enable_uv";
    private static final String KEY_ASR_LANGUAGE = "asr_language";
    private static final String KEY_TRANSLATION_ENABLED = "asr_translation";

    private static void putString(Context context, String key, String value) {
        SharedPreferences.Editor editor = context.getSharedPreferences(SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE).edit();
        editor.putString(key, value);
        editor.apply();
    }

    private static String getString(Context context, String key, String defValue) {
        SharedPreferences sp = context.getSharedPreferences(SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE);
        return sp.getString(key, defValue);
    }

    private static void putBoolean(Context context, String key, boolean value) {
        SharedPreferences.Editor editor = context.getSharedPreferences(SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE).edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    private static boolean getBoolean(Context context, String key, boolean defValue) {
        SharedPreferences sp = context.getSharedPreferences(SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE);
        return sp.getBoolean(key, defValue);
    }

    public static void setUVEnabled(Context context, boolean enable) {
        putBoolean(context, KEY_ENABLE_UV, enable);
    }

    public static boolean getUVEnabled(Context context) {
        return getBoolean(context, KEY_ENABLE_UV, false);
    }

    public static String getASRLanguage(Context context) {
        return getString(context, KEY_ASR_LANGUAGE, "English");
    }

    public static void setASRLanguage(Context context, String val) {
        putString(context, KEY_ASR_LANGUAGE, val);
    }

    public static void setASRTranslationEnabled(Context context, boolean enabled) {
        putBoolean(context, KEY_TRANSLATION_ENABLED, enabled);
    }

    public static boolean getASRTranslationEnabled(Context context) {
        return getBoolean(context, KEY_TRANSLATION_ENABLED, false);
    }
}
