/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.voiceai.usecase.voicecall.tts;

import android.content.Context;
import android.media.AudioAttributes;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import com.qualcomm.qti.voiceai.usecase.voicecall.Constants;
import com.qualcomm.qti.voiceai.usecase.voicecall.QLog;
import com.qualcomm.qti.voiceai.usecase.voicecall.player.Player;

import java.util.Locale;

public class TtsManager {
    private final Context mContext;
    private final String mTag;
    private final Player mPlayer;
    private final TextToSpeech mTextToSpeech;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private final UtteranceProgressListener mUtteranceProgressListener =
            new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    QLog.VC_TTSLogD("onStart utteranceId=" + utteranceId);
                }

                @Override
                public void onDone(String utteranceId) {
                    QLog.VC_TTSLogD("onDone utteranceId=" + utteranceId);
                    play();
                }

                @Override
                public void onError(String utteranceId) {
                    QLog.VC_TTSLogE("onError utteranceId=" + utteranceId);
                }
            };
    public TtsManager(Context context, String tag) {
        mContext = context;
        mTag = tag;
        TextToSpeech.OnInitListener onInitListener = new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    mTextToSpeech.setOnUtteranceProgressListener(mUtteranceProgressListener);
                }
            }
        };
        mTextToSpeech = new TextToSpeech(context, onInitListener);
        mPlayer = new Player(context, tag);
        createHandler();
    }

    public void setLanguage(String language) {
        mTextToSpeech.setLanguage(Locale.forLanguageTag(language));
    }

    public void enqueue(String text) {
        mHandler.post(() -> {
            String tagName = getTagName();
            SynthesisAudioItem item = new SynthesisAudioItem(mContext, tagName, text);
            synthesisToFile(item);
            mPlayer.enqueue(item);
        });
    }

    private void synthesisToFile(SynthesisAudioItem item) {
        Bundle params = new Bundle();
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, item.getTag());
        mTextToSpeech.synthesizeToFile(item.getText(), params, item.getSynthesisFile(),
                item.getTag());
    }

    private String getTagName() {
        return Constants.getCurrentTimestamp() + "_" + mTag;
    }

    private void play() {
        mPlayer.play();
    }

    public void stop() {
        try {
            mTextToSpeech.stop();
        } catch (Exception e){
            QLog.VCLogE(mTag, "exception occurs when TextToSpeech stop", e);
        }
    }

    public void shutdown() {
        try {
            mTextToSpeech.shutdown();
        } catch (Exception e){
            QLog.VCLogE(mTag, "exception occurs when TextToSpeech shutdown", e);
        }
        mPlayer.release();
        mHandlerThread.quitSafely();
    }

    private void createHandler() {
        if (mHandler == null) {
            mHandlerThread = new HandlerThread(mTag + "_voice_call_tts_thread");
            mHandlerThread.start();
            mHandler = new Handler(mHandlerThread.getLooper());
        }
    }

}
