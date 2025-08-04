/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.quicinc.voiceassistant.reference;

import android.content.Context;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.quicinc.voice.assist.sdk.multimodal.MultiModalRecognitionConnector;
import com.quicinc.voiceassistant.reference.controller.SoundModelFilesManager;
import com.quicinc.voiceassistant.reference.data.SmModel;
import com.quicinc.voice.assist.sdk.utility.IOperationCallback;
import com.quicinc.voice.assist.sdk.voiceactivation.VoiceActivationConnector;
import com.quicinc.voice.assist.sdk.voiceactivation.VoiceActivationFailedReason;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public abstract class ActivationActivity extends AppCompatActivity
        implements SoundModelFilesManager.SoundModelsChangeCallback {
    static final String TAG = ActivationActivity.class.getSimpleName();
    List<SmModel> mSmModels = new ArrayList<>();
    Context mContext;
    SoundModelFilesManager mSmFilesManager;

    VoiceActivationConnector mVoiceActivationConnector;
    MultiModalRecognitionConnector mMultiModalRecognitionConnector;
    boolean isVoiceActivationServiceConnected;
    boolean isMultiModalRecognitionConnected;
    protected void registerSoundModelsChangeCallback() {
        mContext = ActivationActivity.this;
        mSmFilesManager = SoundModelFilesManager.getInstance(getApplicationContext());
        mSmFilesManager.addSoundModelsChangeCallback(this);
        mSmModels = mSmFilesManager.getAccessibleSoundModels();
    }

    protected void unregisterSoundModelsChangeCallback() {
        if (mSmFilesManager != null) {
            mSmFilesManager.removeSoundModelsChangeCallback(this);
        }
    }

    @Override
    public void onSoundModelsLoaded() {
        mSmModels = mSmFilesManager.getAccessibleSoundModels();
        notifyDataSetChanged();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
    }

    protected abstract void notifyDataSetChanged();

    protected void toggleRecognition(boolean enable, WeakReference<IOperationCallback
            <Bundle, VoiceActivationFailedReason>> callback) {
        if (enable) {
            mSmFilesManager.enableRecognition(mVoiceActivationConnector,
                    mSmModels.get(0), callback);
        } else {
            mSmFilesManager.disableRecognition(callback);
        }
    }

    protected void toggleMultiModalRecognition(boolean enable, WeakReference<IOperationCallback
            <Bundle, VoiceActivationFailedReason>> callback) {
        if (enable) {
            mSmFilesManager.enableMultiModalRecognition(mMultiModalRecognitionConnector,
                    null, callback);
        } else {
            mSmFilesManager.disableMultiModalRecognition(callback);
        }
    }
}
