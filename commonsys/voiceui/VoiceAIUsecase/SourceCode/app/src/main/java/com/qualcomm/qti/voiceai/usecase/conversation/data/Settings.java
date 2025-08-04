/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.voiceai.usecase.conversation.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

public class Settings {
    private static final String SHARED_PREFERENCES_NAME = "conversation_recording_settings";
    private static final String KEY_TRANSCRIPTION_MODE = "transcription_mode";
    private static final String KEY_TRANSCRIPTION_LANGUAGE = "transcription_language";
    private static final String KEY_TRANSLATION_LANGUAGE = "translation_language";

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

    private static void putStringSet(Context context, String key, HashSet<String> value) {
        SharedPreferences.Editor editor = context.getSharedPreferences(SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE).edit();
        editor.putStringSet(key, null);
        editor.commit();
        editor.putStringSet(key, value);
        editor.apply();
    }

    private static HashSet<String> getStringSet(Context context, String key, String defValue) {
        SharedPreferences sp = context.getSharedPreferences(SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE);
        return (HashSet<String>) sp.getStringSet(key, new HashSet<String>(0));
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

    private static void putInt(Context context, String key, int value) {
        SharedPreferences.Editor editor = context.getSharedPreferences(SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE).edit();
        editor.putInt(key, value);
        editor.apply();
    }

    private static int getInt(Context context, String key, int defValue) {
        SharedPreferences sp = context.getSharedPreferences(SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE);
        return sp.getInt(key, defValue);
    }

    public static void setRealtimeModeEnabled(Context context, boolean enable) {
        putBoolean(context, KEY_TRANSCRIPTION_MODE, enable);
    }

    public static boolean getRealtimeModeEnabled(Context context) {
        return getBoolean(context, KEY_TRANSCRIPTION_MODE, true);
    }

    public static int getTranscriptionLanguage(Context context) {
        return getInt(context, KEY_TRANSCRIPTION_LANGUAGE, 0);
    }

    public static void setTranscriptionLanguage(Context context, int val) {
        putInt(context, KEY_TRANSCRIPTION_LANGUAGE, val);
    }

    public static void setTranslationLanguage(Context context, String val) {
        putString(context, KEY_TRANSLATION_LANGUAGE, val);
    }

    public static String getTranslationLanguage(Context context) {
        return getString(context, KEY_TRANSLATION_LANGUAGE, null);
    }
    public static void setTranslationLanguages(Context context, HashSet<String> val) {
        Log.d("setTranslationLanguages", "val = " + val);
        putStringSet(context, KEY_TRANSLATION_LANGUAGE, val);
    }

    public static HashSet<String>  getTranslationLanguages(Context context) {
        Log.d("getTranslationLanguages", "val = " + getStringSet(context, KEY_TRANSLATION_LANGUAGE, null));
        return getStringSet(context, KEY_TRANSLATION_LANGUAGE, null);
    }
}
