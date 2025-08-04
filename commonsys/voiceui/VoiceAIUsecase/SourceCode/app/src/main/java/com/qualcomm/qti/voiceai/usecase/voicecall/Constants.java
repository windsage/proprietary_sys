/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.voiceai.usecase.voicecall;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.qualcomm.qti.voiceai.usecase.wrapper.QSystemProperties;
public class Constants {


    public static final int AUDIO_SOURCE = 1;
    public static final int BITS_PER_SAMPLE = 16;
    public static final int AUDIO_SAMPLE_RATE = 16000;
    public static final int BUFFER_SIZE = 2 * 32 * 1000;
    public final static String RECORDING_ROOT_PATH = "VoiceCallRecording";
    public final static String WAV_SUFFIX = ".wav";
    public final static String PCM_SUFFIX = ".pcm";
    public final static String TX = "TX";
    public final static String RX = "RX";
    public final static String KEY_ENABLE_FFECNS_RECORD = "translate_record";
    public final static String KEY_MUTE_RX_VOICE = "voice_translation_rx_mute";
    private static String mVoiceCallStartTime;
    public static void markVoiceCallStartTime() {
        mVoiceCallStartTime = getCurrentTimestamp();
    }

    public static void markVoiceCallStartTime(String startTime) {
        mVoiceCallStartTime = startTime;
    }

    public static String getVoiceCallStartTime() {
        return mVoiceCallStartTime;
    }

    public static void cleanVoiceCallStartTime() {
        mVoiceCallStartTime = "NULL";
    }

    public static String getCurrentTimestamp() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmssSSS",
                Locale.getDefault());
        return dateFormat.format(new Date());
    }

    public static boolean isDebugMode() {
        return "1".equals(QSystemProperties.get("persist.voiceai.dump"));
    }

}
