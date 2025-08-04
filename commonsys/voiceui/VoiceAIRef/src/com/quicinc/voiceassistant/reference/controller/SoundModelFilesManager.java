/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.quicinc.voiceassistant.reference.controller;

import static com.quicinc.voice.assist.sdk.voiceactivation.VoiceActivationFailedReason.VOICE_ACTIVATION_INVALID_PARAM;

import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;

import com.quicinc.voice.assist.sdk.multimodal.MultiModalRecognitionConnector;
import com.quicinc.voiceassistant.reference.ClientApplication;
import com.quicinc.voiceassistant.reference.data.SmModel;
import com.quicinc.voiceassistant.reference.util.FileUtils;
import com.quicinc.voiceassistant.reference.util.LogUtils;
import com.quicinc.voiceassistant.reference.util.SoundModelFileNameUtils;
import com.quicinc.voice.assist.sdk.utility.AbstractConnector;
import com.quicinc.voice.assist.sdk.utility.IOperationCallback;
import com.quicinc.voice.assist.sdk.voiceactivation.SoundModel;
import com.quicinc.voice.assist.sdk.voiceactivation.VoiceActivationConnector;
import com.quicinc.voice.assist.sdk.voiceactivation.VoiceActivationFailedReason;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public final class SoundModelFilesManager implements
        SoundModelFilesSyncTask.FilesSyncCallback,
        SoundModelsLoaderTask.LoaderCallback {
    public static final int DELAY_WAITING_CONNECTED = 2000;
    static final Handler mMainHandler = new Handler();
    private static final String TAG = SoundModelFilesManager.class.getSimpleName();
    private static SoundModelFilesManager sInstance;
    private final Context mContext;
    private final List<SoundModelsChangeCallback> mCallbacks = new ArrayList<>();
    private final List<SmModel> mSmModels = new ArrayList<>();
    private final Handler mWorkHandler;
    private final ComponentName mRecognitionService;

    /*ui thread*/
    @Override
    public void onSoundModelsSyncCompleted(List<List<String>> upgradeList) {
        if (upgradeList.size() > 0) {
            LogUtils.e(TAG, "onSoundModelsSyncCompleted");
            upgradeSoundModels(upgradeList);
        }
        handleLoadSoundModelsInternal();
    }

    /*ui thread*/
    @Override
    public void onSoundModelsLoaded(List<SmModel> smModels) {
        handleNotifySoundModelsLoaded(smModels);
    }

    public interface SoundModelsChangeCallback {
        void onSoundModelsLoaded();
    }

    public static SoundModelFilesManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new SoundModelFilesManager(context);
        }
        return sInstance;
    }

    private SoundModelFilesManager(Context context) {
        mContext = context;
        mRecognitionService = new ComponentName(context,
                "com.qualcomm.qti.voice.assist.client.RecognitionService");

        HandlerThread handlerThread = new HandlerThread("qva_client");
        handlerThread.start();
        mWorkHandler = new Handler(handlerThread.getLooper());
    }


    public void enableRecognition(VoiceActivationConnector connector, SmModel smModel,
                                  WeakReference<IOperationCallback<Bundle,
                                          VoiceActivationFailedReason>> callback) {
        if (smModel != null) {
            if (isRecognitionEnabled(connector, smModel)) {
                if (callback.get() != null) {
                    callback.get().onSuccess(new Bundle());
                }
                LogUtils.d(TAG, "this sound model has been enabled");
            } else {
                connector.enableWakewordRecognition(smModel.convertToSoundModel(),
                        mRecognitionService, callback);
            }
        } else {
            if (callback.get() != null) {
                callback.get().onFailure(VOICE_ACTIVATION_INVALID_PARAM);
            }
        }
    }

    public void disableRecognition(WeakReference<IOperationCallback<Bundle,
            VoiceActivationFailedReason>> callback) {
        VoiceActivationConnector connector = new VoiceActivationConnector(mContext);
        connector.connect(new AbstractConnector.ServiceConnectListener() {
            @Override
            public void onServiceConnected() {
                if (mSmModels.size() > 0) {
                    disableRecognition(connector, mSmModels.get(0), callback);
                }
                connector.disconnect();
            }

            @Override
            public void onServiceDisConnected(ComponentName name) {

            }
        });
    }

    public void releaseRecognitions(VoiceActivationConnector connector) {
        if (connector.getWakewordToggleService() != null) {
            mReleaseRecognitionTask.run();
        } else {
            mMainHandler.postDelayed(mReleaseRecognitionTask, DELAY_WAITING_CONNECTED);
        }
    }

    public boolean isRecognitionEnabled(VoiceActivationConnector connector, SmModel smModel) {
        return smModel != null && connector.isWakewordRecognitionEnabled(
                smModel.convertToSoundModel());
    }

    public boolean isRecognitionEnabled(VoiceActivationConnector connector) {
        if (mSmModels.size() > 0) {
            return isRecognitionEnabled(connector, mSmModels.get(0));
        } else {
            return false;
        }
    }

    public void enableMultiModalRecognition(MultiModalRecognitionConnector connector,
                                            SmModel smModel, WeakReference<IOperationCallback<Bundle,
                                            VoiceActivationFailedReason>> callback) {
        if (isMultiModalRecognitionEnabled(connector)) {
            if (callback.get() != null) {
                callback.get().onSuccess(new Bundle());
            }
            LogUtils.d(TAG, "liftAndTalk Recognition has been enabled");
        } else {
            SoundModel soundModel = null;
            if (smModel != null) {
                soundModel = smModel.convertToSoundModel();
            }
            connector.enableRecognition(soundModel,
                    mRecognitionService, callback);
        }
    }

    public void disableMultiModalRecognition(WeakReference<IOperationCallback<Bundle,
            VoiceActivationFailedReason>> callback) {
        MultiModalRecognitionConnector connector = new MultiModalRecognitionConnector(mContext);
        connector.connect(new AbstractConnector.ServiceConnectListener() {
            @Override
            public void onServiceConnected() {
                disableMultiModalRecognition(connector, null, callback);
                connector.disconnect();
            }

            @Override
            public void onServiceDisConnected(ComponentName name) {

            }
        });
    }

    public void disableMultiModalRecognition(MultiModalRecognitionConnector connector,
                                             SmModel smModel,
                                             WeakReference<IOperationCallback<Bundle,
                                                     VoiceActivationFailedReason>> callback) {
        if (isMultiModalRecognitionEnabled(connector)) {
            SoundModel soundModel = null;
            if (smModel != null) {
                soundModel = smModel.convertToSoundModel();
            }
            connector.disableRecognition(soundModel, callback);
        } else {
            LogUtils.d(TAG, "liftAndTalk Recognition has been disabled");
            if (callback.get() != null) {
                callback.get().onSuccess(new Bundle());
            }
        }
    }
    public boolean isMultiModalRecognitionEnabled(MultiModalRecognitionConnector connector) {
        return connector.isRecognitionEnabled(null);

    }

    public void initialize() {
        mWorkHandler.post(new SoundModelFilesSyncTask(mContext, this));
    }

    public void addSoundModelsChangeCallback(SoundModelsChangeCallback callback) {
        if (!mCallbacks.contains(callback)) {
            mCallbacks.add(callback);
        }
    }

    public void removeSoundModelsChangeCallback(SoundModelsChangeCallback callback) {
        mCallbacks.remove(callback);
    }

    public List<SmModel> getAccessibleSoundModels() {
        return mSmModels;
    }

    private void handleLoadSoundModelsInternal() {
        mWorkHandler.post(new SoundModelsLoaderTask(this));
    }

    private void handleNotifySoundModelsLoaded(final List<SmModel> smModels) {
        mSmModels.clear();
        mSmModels.addAll(smModels);
        notifySoundModelsLoaded();
    }

    private void notifySoundModelsLoaded() {
        for (SoundModelsChangeCallback callback : mCallbacks) {
            callback.onSoundModelsLoaded();
        }
    }

    public void disableRecognition(VoiceActivationConnector connector, SmModel smModel,
                                   WeakReference<IOperationCallback<Bundle,
                                           VoiceActivationFailedReason>> callback) {
        if (smModel != null) {
            if (isRecognitionEnabled(connector, smModel)) {
                connector.disableWakewordRecognition(smModel.convertToSoundModel(), callback);
            } else {
                LogUtils.d(TAG, "this sound model has been disabled");
                if (callback.get() != null) {
                    callback.get().onSuccess(new Bundle());
                }
            }
        } else {
            connector.disableWakewordRecognition(null, callback);
        }

    }

    private final Runnable mReleaseRecognitionTask = () -> disableRecognition(
            new WeakReference<>(
                    new IOperationCallback<Bundle, VoiceActivationFailedReason>() {
                        @Override
                        public void onSuccess(Bundle bundle) {
                            LogUtils.d(TAG, "releaseRecognitions onSuccess");
                        }

                        @Override
                        public void onFailure(VoiceActivationFailedReason reason) {
                            LogUtils.e(TAG, "releaseRecognitions onFailure reason " + reason);
                        }
                    }));

    private void upgradeSoundModels(List<List<String>> upgradeList) {
        for (List<String> list : upgradeList) {
            String[] files = list.toArray(new String[0]);
            Arrays.sort(files,
                    Comparator.comparingLong(SoundModelFileNameUtils::parseSoundModelVersion));
            for (String file : files) {
                SoundModel soundModel = new SoundModel(file, null,
                        FileUtils.getLocaleFromFileName(file));
                VoiceActivationConnector connector = new VoiceActivationConnector(mContext);
                connector.connect(new AbstractConnector.ServiceConnectListener() {
                    @Override
                    public void onServiceConnected() {
                        if (connector.isWakewordRecognitionEnabled(soundModel)) {
                            releaseRecognitions(connector);
                            LogUtils.d(TAG, "mEnabledSoundModelFile = " + file);
                        }
                    }

                    @Override
                    public void onServiceDisConnected(ComponentName name) {

                    }
                });

            }
            for (int i = 0; i < files.length - 1; i++) {
                LogUtils.d(TAG, "deleteFile " + files[i]);
                FileUtils.deleteFile(new File(ClientApplication.getInstance().getAppFilesDir(),
                        files[i]));
            }
        }
    }
}
