/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.voiceai.dspasr.data;

import android.content.Context;
import android.content.SharedPreferences;

public class Settings {
    private static final String SHARED_PREFERENCES_NAME = "enpu_asr_para";
    private static final String KEY_ENABLE_CONTINUOUS_TRANSCRIPTION = "enable_continuous_transcription";
    private static final String KEY_ENABLE_PARTIAL_TRANSCRIPTION = "enable_partial_transcription";
    private static final String KEY_ENABLE_LOW_POWER_BUFFER_MODE = "enable_low_power_buffer_mode";
    private static final String KEY_ASR_LANGUAGE = "asr_language";
    private static final String KEY_TRANSLATION_ENABLED = "asr_translation";
    private static final String KEY_ASR_TIMEOUT = "asr_timeout";

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

    public static void setContinuousTranscriptionEnabled(Context context, boolean enable) {
        putBoolean(context, KEY_ENABLE_CONTINUOUS_TRANSCRIPTION, enable);
    }

    public static boolean getContinuousTranscriptionEnabled(Context context) {
        return getBoolean(context, KEY_ENABLE_CONTINUOUS_TRANSCRIPTION, false);
    }

    public static void setPartialTranscriptionEnabled(Context context, boolean enable) {
        putBoolean(context, KEY_ENABLE_PARTIAL_TRANSCRIPTION, enable);
    }

    public static boolean getPartialTranscriptionEnabled(Context context) {
        return getBoolean(context, KEY_ENABLE_PARTIAL_TRANSCRIPTION, true);
    }
    public static void setLowPowerBufferModeEnabled(Context context, boolean enable) {
        putBoolean(context, KEY_ENABLE_LOW_POWER_BUFFER_MODE, enable);
    }

    public static boolean getLowPowerBufferModeEnabled(Context context) {
        return getBoolean(context, KEY_ENABLE_LOW_POWER_BUFFER_MODE, false);
    }

    public static String getASRLanguage(Context context) {
        return getString(context, KEY_ASR_LANGUAGE, "EN_US");
    }

    public static void setASRLanguage(Context context, String val) {
        putString(context, KEY_ASR_LANGUAGE, val);
    }

    public static String getASRTimeout(Context context) {
        return getString(context, KEY_ASR_TIMEOUT, "5000");
    }

    public static void setASRTimeout(Context context, String val) {
        putString(context, KEY_ASR_TIMEOUT, val);
    }

    public static void setASRTranslationEnabled(Context context, boolean enabled) {
        putBoolean(context, KEY_TRANSLATION_ENABLED, enabled);
    }

    public static boolean getASRTranslationEnabled(Context context) {
        return getBoolean(context, KEY_TRANSLATION_ENABLED, false);
    }
}
