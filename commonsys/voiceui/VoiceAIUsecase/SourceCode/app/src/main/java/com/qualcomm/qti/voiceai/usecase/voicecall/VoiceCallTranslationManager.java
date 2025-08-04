/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.voiceai.usecase.voicecall;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Handler;
import android.os.HandlerThread;

public class VoiceCallTranslationManager {

    private static final String TAG = VoiceCallTranslationManager.class.getSimpleName();
    enum STATE {IDLE, INIT, START}

    private STATE mState;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private final Context mContext;
    private final LinkRouteManager mTxUplinkRouteManager;
    private SharedPreferences.OnSharedPreferenceChangeListener mDataChangeListener;
    private AudioManager mAudioManager;
    public VoiceCallTranslationManager(Context context) {
        mState = STATE.IDLE;
        mContext = context;
        mTxUplinkRouteManager = new TxUplinkRouteManager(context);
        mAudioManager = context.getSystemService(AudioManager.class);
        createHandler();
    }

    private void createHandler() {
        if (mHandler == null) {
            mHandlerThread = new HandlerThread("voice_call_manager_thread");
            mHandlerThread.start();
            mHandler = new Handler(mHandlerThread.getLooper());
        }
    }

    public void init() {
        mHandler.post(() -> {
            QLog.VCLogD(TAG, "init state=" + mState);
            if (mState == STATE.IDLE) {
                mTxUplinkRouteManager.init();
                RxDownlinkRouteService.init(mContext);
                setLanguage();
                registerDataChangeListener();
                mState = STATE.INIT;
            }
        });

    }

    public void start() {
        if (mState == STATE.START) return;
        if (mState == STATE.IDLE) {
            init();
        }
        mHandler.post(() -> {
            QLog.VCLogD(TAG, "init start=" + mState);
            if (mState == STATE.INIT) {
                Constants.markVoiceCallStartTime();
                mTxUplinkRouteManager.start();
                RxDownlinkRouteService.start(mContext, Constants.getVoiceCallStartTime());
                muteVoiceIfNeeded();
                mState = STATE.START;
            }
        });
    }

    public void stop() {
        mHandler.post(() -> {
            QLog.VCLogD(TAG, "stop state=" + mState);
            if (mState == STATE.START) {
                Constants.cleanVoiceCallStartTime();
                mTxUplinkRouteManager.stop();
                RxDownlinkRouteService.stop(mContext);
                unMuteAllVoiceWhenStopped();
                mState = STATE.INIT;
            }
        });
    }

    public void deinit() {
        mHandler.post(() -> {
            QLog.VCLogD(TAG, "deinit state=" + mState);
            if (mState == STATE.INIT) {
                mTxUplinkRouteManager.deinit();
                RxDownlinkRouteService.deinit(mContext);
                unRegisterDataChangeListener();
                mState = STATE.IDLE;
            }
        });
    }

    public void clearThread() {
        mHandlerThread.quitSafely();
    }

    private void setLanguage() {
        String txLanguage = VoiceCallSettings.getTxLanguage();
        String rxLanguage = VoiceCallSettings.getRxLanguage();
        mTxUplinkRouteManager.setASRLanguage(txLanguage);
        mTxUplinkRouteManager.setTranslationLanguage(rxLanguage);
        RxDownlinkRouteService.setLanguage(mContext, rxLanguage, txLanguage);
    }

    private void registerDataChangeListener() {
        if (mDataChangeListener == null) {
            mDataChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
                    if (key.equals(VoiceCallSettings.KEY_MY_VOICE_LANGUAGE)
                            || key.equals(VoiceCallSettings.KEY_OTHERS_VOICE_LANGUAGE)) {
                        setLanguage();
                    }
                    if (mState == STATE.START) {
                        if (key.equals(VoiceCallSettings.KEY_MUTE_MY_VOICE)) {
                            boolean muteMyVoice = VoiceCallSettings.getMyVoiceMute();
                            muteMyOriginVoice(muteMyVoice);
                        }
                        if (key.equals(VoiceCallSettings.KEY_MUTE_OTHERS_VOICE)) {
                            boolean muteOtherVoice = VoiceCallSettings.getOtherPersonVoiceMute();
                            muteOthersOriginVoice(muteOtherVoice);
                        }
                    }
                }
            };
        }
        VoiceCallSettings.registerDataChangeListener(mDataChangeListener);
    }

    private void unRegisterDataChangeListener() {
        if (mDataChangeListener != null) {
            VoiceCallSettings.unRegisterDataChangeListener(mDataChangeListener);
        }
    }

    private void muteVoiceIfNeeded() {
        boolean muteMyVoice = VoiceCallSettings.getMyVoiceMute();
        if(muteMyVoice) {
            muteMyOriginVoice(true);
        }
        boolean muteOtherVoice = VoiceCallSettings.getOtherPersonVoiceMute();
        if(muteOtherVoice) {
            muteOthersOriginVoice(true);
        }
    }

    private void unMuteAllVoiceWhenStopped() {
        muteMyOriginVoice(false);
        muteOthersOriginVoice(false);
    }


    private void muteMyOriginVoice(boolean mute) {
        QLog.VCLogD(TAG ,"muteMyOriginVoice " + mute);
        mAudioManager.setMicrophoneMute(mute);
    }

    private void muteOthersOriginVoice(boolean mute) {
        String value = mute ? "true" : "false";
        QLog.VCLogD(TAG, "muteOthersOriginVoice setParameters("
                + Constants.KEY_MUTE_RX_VOICE + "=" + value + ")");
        mAudioManager.setParameters(Constants.KEY_MUTE_RX_VOICE + "=" + value);
        QLog.VCLogD(TAG, "muteOthersOriginVoice setParameters done");
    }
}
