/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.quicinc.voice.assist.sdk.voiceactivation;

import android.content.ComponentName;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.quicinc.voice.activation.aidl.IResultCallback;
import com.quicinc.voice.activation.aidl.IWakewordToggleService;
import com.quicinc.voice.assist.sdk.utility.Constants;
import com.quicinc.voice.assist.sdk.utility.DataUtils;
import com.quicinc.voice.assist.sdk.utility.IOperationCallback;
import com.quicinc.voice.assist.sdk.utility.UIThreadExecutor;

import java.lang.ref.WeakReference;

/**
 * A public class which contains all the Wakeword recognition enable/disable API.
 */
public class VoiceActivationManager {
    private final static String TAG = VoiceActivationManager.class.getSimpleName();

    private static final String SOUND_MODEL_FILE_NAME = "SoundModel.fileName";
    private static final String SOUND_MODEL_USER_NAME ="SoundModel.userId";
    private static final String SOUND_MODEL_LOCALE ="SoundModel.locale";
    private static final String KEY_CLIENT_INFO_TYPE = "ClientInfo.type";
    private static final String KEY_CLIENT_INFO_PACKAGE_NAME = "ClientInfo.packageName";
    private static final String KEY_CLIENT_INFO_CLASS_NAME = "ClientInfo.className";
    private static final String KEY_SOUND_MODEL_UUID = "SoundModelInfo.UUID";

    public static void enableWakewordRecognition(VoiceActivationConnector connector,
                                                 SoundModel soundModel,
                                                 ComponentName recognitionService,
                                                 WeakReference<IOperationCallback<Bundle,
                                                         VoiceActivationFailedReason>> callbackRef) {
        IOperationCallback<Bundle, VoiceActivationFailedReason> callback = callbackRef.get();
        if (callback == null) return;
        IWakewordToggleService wakewordToggleService = checkAndGetWakewordService(connector);
        if (wakewordToggleService == null) {
            UIThreadExecutor
                    .failed(callbackRef, VoiceActivationFailedReason.VOICE_ACTIVATION_NOT_CONNECTED);
            return;
        }
        if (!isSoundModelValid(soundModel)) {
            UIThreadExecutor
                    .failed(callbackRef, VoiceActivationFailedReason.VOICE_ACTIVATION_INVALID_PARAM);
            return;
        }
        Bundle bundle = buildWakewordInfo(soundModel, recognitionService);
        ParcelFileDescriptor pfd = DataUtils.openSoundModel(soundModel.getSoundModelPath());
        if (pfd == null) {
            UIThreadExecutor
                    .failed(callbackRef, VoiceActivationFailedReason.VOICE_ACTIVATION_FILE_NOT_EXIST);
            return;
        }
        try {
            Log.d(TAG, "enable wakeword recognition");
            wakewordToggleService
                    .enableWakewordRecognition(bundle, pfd, new IResultCallback.Stub() {
                        @Override
                        public void onSuccess(Bundle returnValues) {
                            Log.d(TAG, "enable wakeword recognition success");
                            UIThreadExecutor.success(callbackRef, returnValues);
                        }

                        @Override
                        public void onFailure(Bundle params) {
                            if (params == null) params = Bundle.EMPTY;
                            UIThreadExecutor.failed(callbackRef,
                                    VoiceActivationFailedReason.getFailedReason(
                                            params.getInt(Constants.KEY_ERROR_CODE)));
                            Log.d(TAG, "enable wakeword recognition failed");
                        }
                    });
        } catch (RemoteException e) {
            DataUtils.closeFD(pfd);
            callback.onFailure(VoiceActivationFailedReason.VOICE_ACTIVATION_NOT_CONNECTED);
            Log.e(TAG,
                    "call enableWakewordRecognition error, " + e.getLocalizedMessage());
        }

    }

    public static void disableWakewordRecognition(VoiceActivationConnector connector,
                                                  SoundModel soundModel,
                                                  WeakReference<IOperationCallback<Bundle,
                                                          VoiceActivationFailedReason>> callbackRef) {
        IOperationCallback<Bundle, VoiceActivationFailedReason> callback = callbackRef.get();
        if (callback == null) return;
        IWakewordToggleService wakewordToggleService = checkAndGetWakewordService(connector);
        if (wakewordToggleService == null) {
            UIThreadExecutor
                    .failed(callbackRef, VoiceActivationFailedReason.VOICE_ACTIVATION_NOT_CONNECTED);
            return;
        }

        Bundle bundle = null;
        if (isSoundModelValid(soundModel)) {
            bundle = createParams(soundModel);
        }
        try {
            Log.d(TAG, "disable wakeword recognition");
            wakewordToggleService.disableWakewordRecognition(bundle, new IResultCallback.Stub() {
                @Override
                public void onSuccess(Bundle returnValues) {
                    Log.d(TAG,
                            "disable wakeword recognition success");
                    UIThreadExecutor.success(callbackRef, returnValues);
                }

                @Override
                public void onFailure(Bundle params) {
                    if (params == null) params = Bundle.EMPTY;
                    UIThreadExecutor.failed(callbackRef,
                            VoiceActivationFailedReason.getFailedReason(params.getInt(
                                    Constants.KEY_ERROR_CODE)));
                    Log.d(TAG,
                            "disable wakeword recognition failed");
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "call disableWakewordRecognition error, " +
                    e.getLocalizedMessage());
            UIThreadExecutor
                    .failed(callbackRef, VoiceActivationFailedReason.VOICE_ACTIVATION_NOT_CONNECTED);
        }
    }

    public static boolean isWakewordRecognitionEnabled(VoiceActivationConnector connector,
                                                       SoundModel soundModel) {
        IWakewordToggleService wakewordToggleService = checkAndGetWakewordService(connector);
        if (wakewordToggleService != null && isSoundModelValid(soundModel)) {
            Bundle bundle = createParams(soundModel);
            try {
                return wakewordToggleService.isWakewordRecognitionEnabled(bundle);
            } catch (Exception e) {
                Log.e(TAG, "call isWakewordRecognitionEnabled error, " + e.getLocalizedMessage());
            }
        }
        return false;
    }

    private static Bundle createParams(SoundModel soundModel) {
        Bundle bundle = new Bundle();
        bundle.putString(SOUND_MODEL_FILE_NAME, soundModel.getFileName());
        bundle.putString(SOUND_MODEL_USER_NAME, soundModel.getUserId());
        bundle.putString(SOUND_MODEL_LOCALE, soundModel.getLocale());
        return bundle;
    }
    private static IWakewordToggleService checkAndGetWakewordService(
            VoiceActivationConnector connector) {
        if (connector != null) {
            return connector.getWakewordToggleService();
        }
        return null;
    }

    private static Bundle buildWakewordInfo(SoundModel soundModel,
                                            ComponentName recognitionService) {
        Bundle bundle = createParams(soundModel);
        if (soundModel.getBundle() != null) bundle.putAll(soundModel.getBundle());
        bundle.putString(KEY_CLIENT_INFO_PACKAGE_NAME, recognitionService.getPackageName());
        bundle.putString(KEY_CLIENT_INFO_CLASS_NAME, recognitionService.getClassName());
        bundle.putBoolean("ClientInfo.forLLMClient", true);
        return bundle;
    }

    private static boolean isSoundModelValid(SoundModel soundModel) {
        if (soundModel != null && !TextUtils.isEmpty(soundModel.getFileName())) {
            String filename = soundModel.getFileName();
            return filename.endsWith(Constants.SUFFIX_PRESET_SOUND_MODEL)
                    || filename.endsWith(Constants.SUFFIX_TRAINED_SOUND_MODEL);
        }
        return false;
    }
}