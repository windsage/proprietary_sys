/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.voiceai.usecase.conversation.ui;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ConversationViewModel extends ViewModel {
    private static final String TAG = ConversationViewModel.class.getSimpleName();
    private final MutableLiveData<Boolean> mMicPermissionGranted = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> mASRListening = new MutableLiveData<>(false);
    private final MutableLiveData<AsrResult> mASRResult = new MutableLiveData<>();
    public void setASRListening(Boolean listening) {
        if (listening.equals(getASRListening().getValue())) return;
        Log.d(TAG, "set listening: " + listening );
        mASRListening.setValue(listening);
    }
    public LiveData<Boolean> getASRListening() {
        return mASRListening;
    }

    public void setASRResult(AsrResult asrResult) {
        Log.d(TAG, "set asrResult:" + asrResult.getResult() + " isFinal:" + asrResult.isFinal());
        mASRResult.setValue(asrResult);
    }

    public LiveData<AsrResult> getASRResult() {
        return mASRResult;
    }

    public void setMicPermissionGranted(boolean granted) {
        mMicPermissionGranted.setValue(granted);
    }

    public LiveData<Boolean> getIsMicPermissionGranted(){
        return mMicPermissionGranted;
    }

    static class AsrResult {

        private final String mResult;
        private final boolean isFinal;

        AsrResult(boolean isFinal, String result) {
            this.isFinal = isFinal;
            mResult = result;
        }

        public boolean isFinal() {
            return isFinal;
        }

        public String getResult() {
            return mResult;
        }

        @Override
        public String toString() {
            return getResult();
        }
    }
}
