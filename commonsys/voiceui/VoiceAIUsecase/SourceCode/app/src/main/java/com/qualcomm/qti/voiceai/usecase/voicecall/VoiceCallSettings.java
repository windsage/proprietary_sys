/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.voiceai.usecase.voicecall;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

import com.qualcomm.qti.voiceai.usecase.Facade;

import java.util.ArrayList;

public final class VoiceCallSettings {
    private static final ArrayList<String> SUPPORTED_LANGUAGES = new ArrayList<>();
    private static final String DEFAULT_RX_LANGUAGE = "English";
    private static final String DEFAULT_TX_LANGUAGE = "Chinese";

    static {
        SUPPORTED_LANGUAGES.add("English");
        SUPPORTED_LANGUAGES.add("Chinese");
        SUPPORTED_LANGUAGES.add("French");
        SUPPORTED_LANGUAGES.add("Spanish");
        SUPPORTED_LANGUAGES.add("German");
        SUPPORTED_LANGUAGES.add("Italian");
        SUPPORTED_LANGUAGES.add("Korean");
    }

    private static final String SHARED_PREFERENCE_NAME = "voice_call_settings";
    public static final String KEY_ENABLE_FEATURE = "enable_feature";
    public static final String KEY_MY_VOICE_LANGUAGE = "my_voice_language";
    public static final String KEY_OTHERS_VOICE_LANGUAGE = "others_voice_language";
    public static final String KEY_MUTE_MY_VOICE = "mute_my_voice";
    public static final String KEY_MUTE_OTHERS_VOICE = "mute_others_voice";


    private final static SharedPreferences mSharedPreferences =
            Facade.getApplicationContext().getSharedPreferences(SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);

    public static void registerDataChangeListener(OnSharedPreferenceChangeListener listener) {
        mSharedPreferences.registerOnSharedPreferenceChangeListener(listener);
    }

    public static void unRegisterDataChangeListener(OnSharedPreferenceChangeListener listener) {
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(listener);
    }

    public static ArrayList<String> getSupportedLanguages() {
        return SUPPORTED_LANGUAGES;
    }

    private static void putString(String key, String value) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    private static String getString(String key, String defValue) {
        return mSharedPreferences.getString(key, defValue);
    }

    private static void putBoolean(String key, boolean value) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    private static boolean getBoolean(String key, boolean defValue) {
        return mSharedPreferences.getBoolean(key, defValue);
    }

    public static void setVoiceCallTranslationEnabled(boolean enabled) {
        putBoolean(KEY_ENABLE_FEATURE, enabled);
    }

    public static boolean getVoiceCallTranslationEnabled() {
        return getBoolean(KEY_ENABLE_FEATURE, false);
    }

    public static void saveTxLanguage(String language) {
        putString(KEY_MY_VOICE_LANGUAGE, language);
    }

    public static String getTxLanguage() {
        return getString(KEY_MY_VOICE_LANGUAGE, DEFAULT_TX_LANGUAGE);
    }

    public static void saveRxLanguage(String language) {
        putString(KEY_OTHERS_VOICE_LANGUAGE, language);
    }

    public static String getRxLanguage() {
        return getString(KEY_OTHERS_VOICE_LANGUAGE, DEFAULT_RX_LANGUAGE);
    }

    public static void saveMyVoiceMute(boolean enabled) {
        putBoolean(KEY_MUTE_MY_VOICE, enabled);
    }

    public static boolean getMyVoiceMute() {
        return getBoolean(KEY_MUTE_MY_VOICE, false);
    }

    public static void saveOtherPersonVoiceMute(boolean enabled) {
        putBoolean(KEY_MUTE_OTHERS_VOICE, enabled);
    }

    public static boolean getOtherPersonVoiceMute() {
        return getBoolean(KEY_MUTE_OTHERS_VOICE, false);
    }
}
