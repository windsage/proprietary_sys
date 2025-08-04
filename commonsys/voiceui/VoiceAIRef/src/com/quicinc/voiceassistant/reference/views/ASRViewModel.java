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

public class ASRViewModel extends ViewModel {
    private static final String TAG = ASRViewModel.class.getSimpleName();
    private final MutableLiveData<Boolean> mMicPermissionGranted = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> mASRListening = new MutableLiveData<>(false);
    private final MutableLiveData<String> mASRResult = new MutableLiveData<>();
    public void setASRListening(Boolean listening) {
        if (listening.equals(getASRListening().getValue())) return;
        Log.d(TAG, "set listening: " + listening );
        mASRListening.setValue(listening);
    }
    public LiveData<Boolean> getASRListening() {
        return mASRListening;
    }

    public void setASRResult(String asrResult) {
        Log.d(TAG, "set asrResult: " + asrResult );
        mASRResult.setValue(asrResult);
    }

    public LiveData<String> getASRResult() {
        return mASRResult;
    }

    public void setMicPermissionGranted(boolean granted) {
        mMicPermissionGranted.setValue(granted);
    }

    public LiveData<Boolean> getIsMicPermissionGranted(){
        return mMicPermissionGranted;
    }
}
