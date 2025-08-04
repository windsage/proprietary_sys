/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.quicinc.voice.assist.sdk.voiceactivation;

/**
 * An enumeration of failed reason when voice activation.
 */
public enum VoiceActivationFailedReason {
    /**
     * Used to indicate unknown error occurred when voice activation.
     */
    VOICE_ACTIVATION_UNKNOWN(-1),
    /**
     * Used to indicate service is not connected when voice activation.
     */
    VOICE_ACTIVATION_NOT_CONNECTED(-2),
    /**
     * Used to indicate error occurred when voice activation.
     */
    VOICE_ACTIVATION_ERROR(-3),
    /**
     * Used to indicate the QVA application not granted permission when voice activation.
     */
    VOICE_ACTIVATION_PERMISSION_DENIED(-4),
    /**
     * Used to indicate service not init occurred when voice activation.
     */
    VOICE_ACTIVATION_NOT_INIT(-5),
    /**
     * Used to indicate the value is not correct.
     */
    VOICE_ACTIVATION_BAD_VALUE(-6),
    /**
     * Used to indicate dead object when voice activation.
     */
    VOICE_ACTIVATION_DEAD_OBJECT(-7),
    /**
     * Used to indicate the previous operation is invalid when voice activation.
     */
    VOICE_ACTIVATION_INVALID_OPERATION(-8),
    /**
     * Used to indicate the parameter is invalid when voice activation.
     */
    VOICE_ACTIVATION_INVALID_PARAM(-9),
    /**
     * Used to indicate file not exist when voice activation.
     */
    VOICE_ACTIVATION_FILE_NOT_EXIST(-10),
    /**
     * Used to indicate file not exist when voice activation.
     */
    VOICE_ACTIVATION_WRONG_STATUS(-11);

    final int mErrorCode;

    VoiceActivationFailedReason(int errorCode) {
        mErrorCode = errorCode;
    }

    public static VoiceActivationFailedReason getFailedReason(int errorCode) {
        for (VoiceActivationFailedReason error : VoiceActivationFailedReason.values()) {
            if (errorCode == error.mErrorCode) {
                return error;
            }
        }
        return VOICE_ACTIVATION_UNKNOWN;
    }
}
