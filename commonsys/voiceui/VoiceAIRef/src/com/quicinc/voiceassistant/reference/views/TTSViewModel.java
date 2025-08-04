/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.quicinc.voiceassistant.reference.views;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class TTSViewModel extends ViewModel {
    private final MutableLiveData<Boolean> mTTSPlaying = new MutableLiveData<Boolean>();
    private final MutableLiveData<String> mTextPendingToSpeech = new MutableLiveData<>();
    public void setTTSPlaying(Boolean item) {
        if (item.equals(getTTSPlaying().getValue())) return;
        Log.d("TTS", "setTTSPlaying: " + item);
        mTTSPlaying.setValue(item);
    }
    public LiveData<Boolean> getTTSPlaying() {
        return mTTSPlaying;
    }

    public void setTextPendingToSpeech(String asrResult) {
        mTextPendingToSpeech.setValue(asrResult);
    }

    public LiveData<String> getTextPendingToSpeech() {
        return mTextPendingToSpeech;
    }
}
