/*
 * Copyright (c) 2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.quicinc.voice.assist.sdk.multimodal;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.quicinc.voice.activation.aidl.IMultiModalRecognitionService;
import com.quicinc.voice.assist.sdk.utility.AbstractConnector;
import com.quicinc.voice.assist.sdk.utility.Constants;
import com.quicinc.voice.assist.sdk.utility.IOperationCallback;
import com.quicinc.voice.assist.sdk.voiceactivation.SoundModel;
import com.quicinc.voice.assist.sdk.voiceactivation.VoiceActivationFailedReason;
import com.quicinc.voice.assist.sdk.voiceactivation.VoiceActivationManager;

import java.lang.ref.WeakReference;

public class MultiModalRecognitionConnector extends AbstractConnector {
    public final static String MULTI_MODAL_RECOGNITION_SERVICE_NAME =
            "com.quicinc.voice.activation.multimodal.MultiModalRecognitionService";
    private final static Intent MULTI_MODAL_RECOGNITION_SERVICE_INTENT =
            new Intent().setClassName(Constants.QVA_PACKAGE_NAME,
                    MULTI_MODAL_RECOGNITION_SERVICE_NAME);
    public MultiModalRecognitionConnector(Context context) {
        super(context, MULTI_MODAL_RECOGNITION_SERVICE_INTENT);
    }

    public IMultiModalRecognitionService getMultiModalRecognitionService() {
        return IMultiModalRecognitionService.Stub.asInterface(getBinder());
    }

    public void enableRecognition(SoundModel soundModel,
                                  ComponentName recognitionService, WeakReference<
                                  IOperationCallback<Bundle,
                                          VoiceActivationFailedReason>> callback) {
        MultiModalRecognitionManager
                .enableRecognition(this, soundModel,
                        recognitionService, callback);
    }

    public void disableRecognition(SoundModel soundModel,
                                   WeakReference<IOperationCallback<Bundle,
                                           VoiceActivationFailedReason>> callback) {
        MultiModalRecognitionManager
                .disableRecognition(this, soundModel, callback);
    }

    public boolean isRecognitionEnabled(SoundModel soundModel) {
        return MultiModalRecognitionManager
                .isRecognitionEnabled(this, soundModel);
    }
}
