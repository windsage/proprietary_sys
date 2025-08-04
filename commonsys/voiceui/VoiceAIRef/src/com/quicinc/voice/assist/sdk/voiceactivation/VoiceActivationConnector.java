/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.quicinc.voice.assist.sdk.voiceactivation;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.quicinc.voice.activation.aidl.IWakewordToggleService;
import com.quicinc.voice.assist.sdk.utility.AbstractConnector;
import com.quicinc.voice.assist.sdk.utility.Constants;
import com.quicinc.voice.assist.sdk.utility.IOperationCallback;

import java.lang.ref.WeakReference;

public class VoiceActivationConnector extends AbstractConnector {
    public final static String WAKEWORD_TOGGLE_SERVICE_NAME =
            "com.quicinc.voice.activation.engineservice.WakewordToggleService";
    private final static Intent WAKEWORD_TOGGLE_SERVICE_INTENT =
            new Intent().setClassName(Constants.QVA_PACKAGE_NAME, WAKEWORD_TOGGLE_SERVICE_NAME);

    public VoiceActivationConnector(Context context) {
        super(context, WAKEWORD_TOGGLE_SERVICE_INTENT);
    }

    public IWakewordToggleService getWakewordToggleService() {
        return IWakewordToggleService.Stub.asInterface(getBinder());
    }

    public void enableWakewordRecognition( SoundModel soundModel,
                                          ComponentName recognitionService,
                                           WeakReference<IOperationCallback<Bundle,
                                                   VoiceActivationFailedReason>> callback) {
        VoiceActivationManager
                .enableWakewordRecognition(this, soundModel,
                        recognitionService, callback);
    }

    public void disableWakewordRecognition( SoundModel soundModel,
                                            WeakReference<IOperationCallback<Bundle,
                                                    VoiceActivationFailedReason>> callback) {
        VoiceActivationManager
                .disableWakewordRecognition(this, soundModel, callback);
    }

    public boolean isWakewordRecognitionEnabled(SoundModel soundModel) {
        return VoiceActivationManager
                .isWakewordRecognitionEnabled(this, soundModel);
    }
}
