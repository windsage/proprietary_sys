/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.voiceai.usecase.voicecall.whisperwrapper;

import android.os.Bundle;

public interface IResultListener {
    //need to keep the same as in WhisperResponseListener, since we do not map the error code
    // Error Codes
    int NO_ERROR = 0;
    int UNKNOWN_ERROR = -1;
    int NOT_INITIALIZED_ERROR = -2;
    int INVALID_PARAMETERS_ERROR = -3;
    int NO_SPEECH_TIMEOUT = -4;

    // Transcription Status Codes
    int VALID_TRANSCRIPTION = 0;
    int INVALID_SPEECH_TOKEN = 1;
    int INVALID_LANGUAGE_TOKEN = 2;
    int PROGRESS_INDICATOR = 3;
    void onTranscription(String transcription, String language, boolean finalized, int code, Bundle extra);
    void onError(int errorCode, Exception ex);
    void onRecordingStopped();
    void onSpeechStart();
    void onSpeechEnd();
}
