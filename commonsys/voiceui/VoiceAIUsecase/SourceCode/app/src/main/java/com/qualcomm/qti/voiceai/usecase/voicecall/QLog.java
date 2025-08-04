/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.voiceai.usecase.voicecall;

import android.util.Log;

public class QLog {

    private final static String TAG_VOICECALL = "VoiceCall";
    public static void VCLogD(final String sub, final String message) {
        Log.d(TAG_VOICECALL + "_" + sub, message);
    }

    public static void VCLogI(final String sub, final String message) {
        Log.i(TAG_VOICECALL + "_" + sub, message);
    }

    public static void VCLogE(final String sub, final String message) {
        Log.e(TAG_VOICECALL + "_" + sub, message);
    }

    public static void VCLogE(final String sub, final String message, Exception e) {
        Log.e(TAG_VOICECALL + "_" + sub, message, e);
    }

    public static void VC_PhoneCallLogD(final String message) {
        VCLogD("phonecallstate", message);
    }

    public static void VC_PhoneCallLogI(final String message) {
        VCLogI("phonecallstate", message);
    }

    public static void VC_PhoneCallLogE(final String message) {
        VCLogE("phonecallstate", message);
    }

    public static void VC_RecordingLogD(final String message) {
        VCLogD("recording", message);
    }

    public static void VC_RecordingLogI(final String message) {
        VCLogI("recording", message);
    }

    public static void VC_RecordingLogE(final String message) {
        VCLogE("recording", message);
    }

    public static void VC_ASRLogD(final String message) {
        VCLogD("asr", message);
    }

    public static void VC_ASRLogI(final String message) {
        VCLogI("asr", message);
    }

    public static void VC_ASRLogE(final String message) {
        VCLogE("asr", message);
    }

    public static void VC_TranslationLogD(final String message) {
        VCLogD("translation", message);
    }

    public static void VC_TranslationLogI(final String message) {
        VCLogI("translation", message);
    }

    public static void VC_TranslationLogE(final String message) {
        VCLogE("translation", message);
    }

    public static void VC_TTSLogD(final String message) {
        VCLogD("tts", message);
    }

    public static void VC_TTSLogI(final String message) {
        VCLogI("tts", message);
    }

    public static void VC_TTSLogE(final String message) {
        VCLogE("tts", message);
    }
    public static void VC_PlaybackLogD(final String message) {
        VCLogD("playback", message);
    }

    public static void VC_PlaybackLogI(final String message) {
        VCLogI("playback", message);
    }

    public static void VC_PlaybackLogE(final String message) {
        VCLogE("playback", message);
    }

    public static void VC_PlaybackLogE(final String message, Exception e) {
        VCLogE("playback", message, e);
    }

}
