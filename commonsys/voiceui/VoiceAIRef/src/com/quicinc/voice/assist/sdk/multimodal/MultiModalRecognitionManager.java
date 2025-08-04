/*
 * Copyright (c) 2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.quicinc.voice.assist.sdk.multimodal;

import android.content.ComponentName;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import com.quicinc.voice.activation.aidl.IResultCallback;
import com.quicinc.voice.activation.aidl.IMultiModalRecognitionService;
import com.quicinc.voice.assist.sdk.utility.Constants;
import com.quicinc.voice.assist.sdk.utility.DataUtils;
import com.quicinc.voice.assist.sdk.utility.IOperationCallback;
import com.quicinc.voice.assist.sdk.utility.UIThreadExecutor;
import com.quicinc.voice.assist.sdk.voiceactivation.SoundModel;
import com.quicinc.voice.assist.sdk.voiceactivation.VoiceActivationFailedReason;

import java.lang.ref.WeakReference;

/**
 * A public class which contains all the Multi Modal recognition enable/disable API.
 */
public class MultiModalRecognitionManager {
    private final static String TAG = MultiModalRecognitionManager.class.getSimpleName();
    private static final String SOUND_MODEL_IS_LIFT_AND_TALK = "SoundModel.isLiftAndTalk";
    private static final String KEY_CLIENT_INFO_PACKAGE_NAME = "ClientInfo.packageName";
    private static final String KEY_CLIENT_INFO_CLASS_NAME = "ClientInfo.className";
    private static final String KEY_CLIENT_INFO_FOR_LLM_CLIENT = "ClientInfo.forLLMClient";

    public static void enableRecognition(MultiModalRecognitionConnector connector,
                                         SoundModel soundModel,
                                         ComponentName recognitionService,
                                         WeakReference<IOperationCallback<Bundle,
                                                 VoiceActivationFailedReason>> callbackRef) {
        IOperationCallback<Bundle, VoiceActivationFailedReason> callback = callbackRef.get();
        if (callback == null) return;
        IMultiModalRecognitionService multiModalRecognitionService =
                checkAndGetMultiModalRecognitionService(connector);
        if (multiModalRecognitionService == null) {
            Log.d(TAG, "enable recognition failed service not exist!");
            UIThreadExecutor
                    .failed(callback, VoiceActivationFailedReason.VOICE_ACTIVATION_NOT_CONNECTED);
            return;
        }
        Bundle bundle = buildModelInfo(soundModel, recognitionService);
        ParcelFileDescriptor pfd = null;
        if (soundModel != null) {
            DataUtils.openSoundModel(soundModel.getSoundModelPath());
            if (pfd == null) {
                UIThreadExecutor
                        .failed(callback, VoiceActivationFailedReason.VOICE_ACTIVATION_FILE_NOT_EXIST);
                return;
            }
        }
        try {
            Log.d(TAG, "enable recognition");
            multiModalRecognitionService
                    .enableRecognition(null, bundle, new IResultCallback.Stub() {
                        @Override
                        public void onSuccess(Bundle returnValues) {
                            Log.d(TAG, "enable recognition success");
                            UIThreadExecutor.success(callbackRef, returnValues);
                        }

                        @Override
                        public void onFailure(Bundle params) {
                            if (params == null) params = Bundle.EMPTY;
                            UIThreadExecutor.failed(callbackRef,
                                    VoiceActivationFailedReason.getFailedReason(
                                            params.getInt(Constants.KEY_ERROR_CODE)));
                            Log.d(TAG, "enable recognition failed");
                        }
                    });
        } catch (RemoteException e) {
            DataUtils.closeFD(pfd);
            callback.onFailure(VoiceActivationFailedReason.VOICE_ACTIVATION_NOT_CONNECTED);
            Log.e(TAG,
                    "call enableRecognition error, " + e.getLocalizedMessage());
        }
    }

    public static void disableRecognition(MultiModalRecognitionConnector connector,
                                   SoundModel soundModel,
                                   WeakReference<IOperationCallback<Bundle,
                                           VoiceActivationFailedReason>> callbackRef) {
        IOperationCallback<Bundle, VoiceActivationFailedReason> callback = callbackRef.get();
        if (callback == null) return;
        IMultiModalRecognitionService multiModalRecognitionService =
                checkAndGetMultiModalRecognitionService(connector);
        if (multiModalRecognitionService == null) {
            UIThreadExecutor
                    .failed(callback, VoiceActivationFailedReason.VOICE_ACTIVATION_NOT_CONNECTED);
            return;
        }

        Bundle bundle = createParams(soundModel);
        try {
            Log.d(TAG, "disable recognition");
            multiModalRecognitionService.disableRecognition(bundle, new IResultCallback.Stub() {
                @Override
                public void onSuccess(Bundle returnValues) {
                    Log.d(TAG,
                            "disable recognition success");
                    UIThreadExecutor.success(callbackRef, returnValues);
                }

                @Override
                public void onFailure(Bundle params) {
                    if (params == null) params = Bundle.EMPTY;
                    UIThreadExecutor.failed(callbackRef,
                            VoiceActivationFailedReason.getFailedReason(
                                    params.getInt(Constants.KEY_ERROR_CODE)));
                    Log.d(TAG,
                            "disable recognition failed");
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "call disableRecognition error, " +
                    e.getLocalizedMessage());
            UIThreadExecutor
                    .failed(callback, VoiceActivationFailedReason.VOICE_ACTIVATION_NOT_CONNECTED);
        }
    }

    public static boolean isRecognitionEnabled(MultiModalRecognitionConnector connector,
                                               SoundModel soundModel) {
        IMultiModalRecognitionService multiModalRecognitionService =
                checkAndGetMultiModalRecognitionService(connector);
        if (multiModalRecognitionService != null) {
            Bundle bundle = createParams(soundModel);
            try {
                return multiModalRecognitionService.isRecognitionEnabled(bundle);
            } catch (Exception e) {
                Log.e(TAG, "call isRecognitionEnabled error, " + e.getLocalizedMessage());
            }
        }
        return false;
    }

    private static Bundle createParams(SoundModel soundModel) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(SOUND_MODEL_IS_LIFT_AND_TALK, true);
        return bundle;
    }

    private static IMultiModalRecognitionService checkAndGetMultiModalRecognitionService(
            MultiModalRecognitionConnector connector) {
        if (connector != null) {
            return connector.getMultiModalRecognitionService();
        }
        return null;
    }

    private static Bundle buildModelInfo(SoundModel soundModel,
                                         ComponentName recognitionService) {
        Bundle bundle = createParams(soundModel);
        if (soundModel != null && soundModel.getBundle() != null) {
            bundle.putAll(soundModel.getBundle());
        }
        bundle.putString(KEY_CLIENT_INFO_PACKAGE_NAME, recognitionService.getPackageName());
        bundle.putString(KEY_CLIENT_INFO_CLASS_NAME, recognitionService.getClassName());
        bundle.putBoolean(KEY_CLIENT_INFO_FOR_LLM_CLIENT, true);
        return bundle;
    }
}