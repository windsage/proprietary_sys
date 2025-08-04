/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.telephonyservice;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.List;

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

/**
 * This class interacts with AudioManager to set/get audio parameters
 * and send audio server up/down indication to lower layers.
 */
public class AudioController implements IAudioController {

    private static final String TAG = "AudioController";
    public static final int AUDIO_STATUS_UNKNOWN = -1;
    public static final int AUDIO_STATUS_OK = 0;
    public static final int AUDIO_GENERIC_FAILURE = 1;
    public static final int AUDIO_STATUS_SERVER_DIED = 2;
    private List<IAudioControllerCallback> mAudioCallbacks = null;
    private AudioServerStateCallback mServerStateCallback = new AudioServerStateCallback();
    private final AudioManager mAudioManager;

    public AudioController(Context context) throws Exception{
        // Creating callback for audio server status
        Log.d(TAG, "registering audio server state callback");
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (mAudioManager == null) {
            throw new Exception("AudioController(): mAudioManager is null.");
        }
        Executor exec = context.getMainExecutor();
        mAudioManager.setAudioServerStateCallback(exec, mServerStateCallback);
    }

    private class AudioServerStateCallback extends AudioManager.AudioServerStateCallback {
        @Override
        public void onAudioServerDown() {
            notifyAudioState(AUDIO_STATUS_SERVER_DIED);
        }
        @Override
        public void onAudioServerUp() {
            notifyAudioState(AUDIO_STATUS_OK);
        }
    }

    /**
    * The function is called by lower layers (RIL) to configure audio.
    * Note that the function is called from a binder thread and hence
    * all member variables accessed in this function must be synchronized.
    * @param keyValuePairs Key-value pairs in following format:
    *   key1=value1;key2=value2;...
    */
    @Override
    public int setParameters(String keyValuePairs) {
        Log.d(TAG, "setParameters with: " + keyValuePairs);
        try {
            if (mAudioManager.isAudioServerRunning()) {
                mAudioManager.setParameters(keyValuePairs);
                return AUDIO_STATUS_OK;
            }
        } catch (Throwable ex) {
            Log.e(TAG, " setParameters - Exception: " + ex);
        }
        return AUDIO_GENERIC_FAILURE;
    }

    /**
     * The function to query audio parameters from AudioManager.
     * This API is not used currently and only added for completeness.
     *
     * Note that the function is called from a binder thread and hence
     * all member variables accessed in this function must be
     * synchronized.
     * @param key list of key parameters
     * @return list of parameters key value pairs in following format:
     *    key1=value1;key2=value2;...
     */
    @Override
    public String getParameters(String key) {
        Log.d(TAG, "getParameters for key: " + key);
        try {
            if (mAudioManager.isAudioServerRunning()) {
                return mAudioManager.getParameters(key);
            }
        } catch (Throwable ex) {
            Log.e(TAG, " getParameters - Exception: " + ex);
        }
        return "";
    }

    public void dispose() {
        // unregister audio server state callback
        Log.d(TAG, "unregistering audio server state callback");
        mAudioManager.clearAudioServerStateCallback();
        mAudioCallbacks = null;
    }

    /**
     * Updates audio callbacks in Audio Controller.
     */
    public void updateAudioCallbacks(List<IAudioControllerCallback> audioCallbacks) {
        Log.d(TAG, "updateAudioCallbacks");
        mAudioCallbacks = new CopyOnWriteArrayList<>(audioCallbacks);
        notifyAudioState(getAudioServerState());
    }

    /**
     * Notifies Audio State via HAL to RIL.
     */
    private void notifyAudioState (int state) {
        Log.d(TAG, "notifyAudioState - state: " + state);
        if (mAudioCallbacks == null) {
            Log.d(TAG, "notifyAudioState: mAudioCallbacks is null");
            return;
        }
        for (IAudioControllerCallback audio: mAudioCallbacks) {
            audio.onAudioStatusChanged(state);
        }
    }

    /**
     * Gets current Audio Server state.
     */
    private int getAudioServerState() {
        return mAudioManager.isAudioServerRunning() ? AUDIO_STATUS_OK : AUDIO_GENERIC_FAILURE;
    }
}
