/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.voiceai.usecase.voicecall.whisperwrapper;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;

import com.qualcomm.qti.voice.assist.whisper.sdk.Whisper;
import com.qualcomm.qti.voice.assist.whisper.sdk.WhisperResponseListener;

import com.qualcomm.qti.voiceai.usecase.R;
import com.qualcomm.qti.voiceai.usecase.voicecall.QLog;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class WhisperEngine implements IWhisperEngine {
    private static final String TAG = WhisperEngine.class.getSimpleName();
    public static final String DNN_MODEL_FILE = "speech_float.eai";
    public static final String VOCAB_FILE = "vocab.bin";
    public static final String ENCODER_FILE = "encoder_model_htp.bin";
    public static final String DECODER_FILE = "decoder_model_htp.bin";
    private static final String WHISPER_MODEL_SUB_PATH = "ASR_binary/Whisper";
    public static final int LOG_LEVEL_FATAL = 0;
    public static final int LOG_LEVEL_ERROR = 1;
    public static final int LOG_LEVEL_WARNING = 2;
    public static final int LOG_LEVEL_INFO = 3;
    public static final int LOG_LEVEL_VERBOSE = 4;
    public static final int DEFAULT_AI_LOG_LEVEL = 0;

    public enum State {IDLE, INIT, START}

    private State mState = State.IDLE;
    private final Context mContext;
    private IResultListener mResultListener;
    private final Whisper mWhisper;
    private WhisperProcessAction mWhisperAction;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private final String mAdspPath;
    private final String mDnnModelPath;
    private final String mVocabPath;
    private final String mEncoderPath;
    private final String mDecoderPath;
    private final String mTag;

    private WhisperResponseListener mWhisperListener = new WhisperResponseListener() {
        @Override
        public void onTranscription(
                String transcription, String language, boolean finalized, int code, Bundle extra) {
            if (mResultListener != null) {
                mResultListener.onTranscription(transcription, language, finalized, code, extra);
            }
        }

        @Override
        public void onError(int errorCode, Exception ex) {
            if (mResultListener != null) {
                mResultListener.onError(errorCode, ex);
            }
        }

        @Override
        public void onRecordingStopped() {
            if (mResultListener != null) {
                mResultListener.onRecordingStopped();
            }
        }

        @Override
        public void onSpeechStart() {
            if (mResultListener != null) {
                mResultListener.onSpeechStart();
            }
        }

        @Override
        public void onSpeechEnd() {
            if (mResultListener != null) {
                mResultListener.onSpeechEnd();
            }
        }
    };

    public interface IInitResultCallback {
        void onSuccess();
        void onFailure(int code, String reason);
    }

    public WhisperEngine(Context context, String tag) {
        mContext = context;
        mTag = tag;
        mWhisper = Whisper.newInstance();
        mWhisper.setListener(mWhisperListener);
        mHandler = createHandler();
        String modelPath = mContext.getResources().getString(R.string.model_path);
        mAdspPath = modelPath + File.separator + WHISPER_MODEL_SUB_PATH;
        mDnnModelPath = mAdspPath + "/" + DNN_MODEL_FILE;
        mVocabPath = mAdspPath + "/" + VOCAB_FILE;
        mEncoderPath = mAdspPath + "/" + ENCODER_FILE;
        mDecoderPath = mAdspPath + "/" + DECODER_FILE;
    }

    @Override
    public void init(IInitResultCallback resultCallback) {
        if (State.IDLE == mState) {
            if (mHandler != null) mHandler.post(()->initEngine(resultCallback));
        } else {
            if (resultCallback != null) resultCallback.onSuccess();
            QLog.VC_ASRLogI(mTag + " asr engine has been already initialized");
        }
    }

    @Override
    public void start(InputStream audioStream) {
        if (mHandler != null) mHandler.post(()->startEngine(audioStream));
    }

    @Override
    public void stop() {
        if (mHandler != null) mHandler.post(this::stopEngine);
    }

    @Override
    public void deInit() {
        if (mHandler != null) mHandler.post(this::deInitEngine);
    }

    @Override
    public void setListener(IResultListener listener) {
        mResultListener = listener;
    }

    @Override
    public int getAILogLevel() {
        return mWhisper.getAILogLevel();
    }

    @Override
    public void setAILogLevel(int level) {
        mWhisper.setAILogLevel(level);
    }

    @Override
    public void setDebugDir(File dir) {
        mWhisper.setDebugDir(dir);
    }

    @Override
    public File getDebugDir() {
        return mWhisper.getDebugDir();
    }

    @Override
    public boolean isDebugEnabled() {
        return mWhisper.isDebugEnabled();
    }

    @Override
    public void enableDebug(boolean enabled) {
        mWhisper.enableDebug(enabled);
    }

    @Override
    public void enableContinuousTranscription(boolean enabled) {
        mWhisper.enableContinuousTranscription(enabled);
    }

    @Override
    public boolean continuousTranscriptionEnabled() {
        return mWhisper.continuousTranscriptionEnabled();
    }

    @Override
    public void setVadLenFuture(int value) {
        mWhisper.setVadLenFuture(value);
    }

    @Override
    public int getVadLenFuture() {
        return mWhisper.getVadLenFuture();
    }

    @Override
    public void setVadLenHangover(int value) {
        mWhisper.setVadLenHangover(value);
    }

    @Override
    public int getVadLenHangover() {
        return mWhisper.getVadLenHangover();
    }

    @Override
    public void setVadThreshold(int value) {
        mWhisper.setVadThreshold(value);
    }

    @Override
    public int getVadThreshold() {
        return mWhisper.getVadThreshold();
    }

    @Override
    public void setVadCmdEndDetectionThreshold(int value) {
        mWhisper.setVadCmdEndDetectionThreshold(value);
    }

    @Override
    public int getVadCmdEndDetectionThreshold() {
        return mWhisper.getVadCmdEndDetectionThreshold();
    }

    @Override
    public void setTranslationEnabled(boolean enabled) {
        mWhisper.setTranslationEnabled(enabled);
    }

    @Override
    public boolean getTranslationEnabled() {
        return mWhisper.getTranslationEnabled();
    }

    @Override
    public void setLanguage(String language) {
        mWhisper.setLanguageCodes(getLanguageCode(language));
    }

    @Override
    public List<String> getLanguages() {
        List<String> languages = new ArrayList<>();
        List<String> codes = mWhisper.getLanguageCodes();
        for (String code: codes) {
            String lang = getLanguage(code);
            if(lang != null) languages.add(lang);
        }
        return languages;
    }

    private String getLanguageCode(String language) {
        if (null != language) {
            String[] supportedLanguages = getSupportedLanguages();
            String[] supportedLanguageCodes = getSupportedLanguageCodes();
            for (int i=0; i < supportedLanguages.length; i++) {
                if (supportedLanguages[i].equalsIgnoreCase(language)) {
                    return supportedLanguageCodes[i];
                }
            }
        }
        return null;
    }

    private String getLanguage(String languagecode) {
        if (null != languagecode) {
            String[] supportedLanguages = getSupportedLanguages();
            String[] supportedLanguageCodes = getSupportedLanguageCodes();
            for (int i=0; i < supportedLanguageCodes.length; i++) {
                if (supportedLanguageCodes[i].equalsIgnoreCase(languagecode)) {
                    return supportedLanguages[i];
                }
            }
        }
        return null;
    }

    @Override
    public void enablePartialTranscriptions(boolean enabled) {
        mWhisper.enablePartialTranscriptions(enabled);
    }

    @Override
    public boolean partialTranscriptionsEnabled() {
        return mWhisper.partialTranscriptionsEnabled();
    }

    public void setNoStartSpeechTimeout(int value) {
        mWhisper.setNoStartSpeechTimeout(value);
    }


    @Override
    public void setMinBufferMs(int ms) {
        mWhisper.setMinBufferMs(ms);
    }

    @Override
    public int getMinBufferMs() {
        return mWhisper.getMinBufferMs();
    }

    @Override
    public void suppressNonSpeech(boolean suppress) {
        mWhisper.suppressNonSpeech(suppress);
    }

    @Override
    public boolean getSuppressNonSpeech() {
        return mWhisper.getSuppressNonSpeech();
    }

    @Override
    public void showProgressIndicators(boolean show) {
        mWhisper.showProgressIndicators(show);
    }

    @Override
    public boolean getShowProgressIndicators() {
        return mWhisper.getShowProgressIndicators();
    }

    @Override
    public String getVersion() {
        return mWhisper.getVersion();
    }

    @Override
    public String[] getSupportedLanguageCodes() {
        return mWhisper.getSupportedLanguageCodes();
    }

    @Override
    public String[] getSupportedLanguages() {
        return mWhisper.getSupportedLanguages();
    }

    private Handler createHandler() {
        mHandlerThread = new HandlerThread(mTag + "_asr_engine");
        mHandlerThread.start();

        return new Handler(mHandlerThread.getLooper());
    }

    private void initEngine(IInitResultCallback resultCallback) {
        if (!isPlatformSupport()) {
            if (resultCallback != null){
                resultCallback.onFailure(Whisper.UNKNOWN, "NOT_SUPPORT");
            }
            QLog.VC_ASRLogI(mTag + " asr init failed, current platform not support");
            return;
        }
        QLog.VC_ASRLogD(mTag + " initEngine state=" + mState);
        if (State.IDLE == mState) {
            int initCode = mWhisper.init(mAdspPath, mDnnModelPath,
                                            mVocabPath, mEncoderPath, mDecoderPath);
            if (Whisper.NO_ERROR == initCode) {
                QLog.VC_ASRLogD(mTag + " asr engine init success");
                if (resultCallback != null) resultCallback.onSuccess();
                mState = State.INIT;
                setVadLenHangover(120);
            } else {
                if (resultCallback != null) resultCallback.onFailure(initCode,
                        (initCode == Whisper.INVALID_PARAMS ? "INVALID_PARAMS" : "UNKNOWN"));
                QLog.VC_ASRLogE(mTag + " asr engine init failed : " + initCode);
            }
        } else if (State.INIT == mState) {
            if (resultCallback != null) resultCallback.onSuccess();
            QLog.VC_ASRLogI(mTag + " asr engine has been already initialized");
        } else {
            QLog.VC_ASRLogI(mTag + " asr init failed, current state is " + mState);
        }
    }

    private void startEngine(InputStream audioStream) {
        QLog.VC_ASRLogD(mTag + " startEngine state=" + mState + ",audioStream=" + audioStream);
        QLog.VC_ASRLogD(mTag + " startEngine whisper config= " + mWhisper.toString());
        if (State.INIT == mState) {
            mWhisperAction = new WhisperProcessAction(mWhisper, audioStream);
            mWhisperAction.fork();
            mState = State.START;
        } else {
            QLog.VC_ASRLogI(mTag + " asr start failed, current state is " + mState);
        }
    }

    private void stopEngine() {
        QLog.VC_ASRLogD(mTag + " stopEngine state=" + mState);
        if (mState == State.START) {
            mWhisperAction.cancel(true);
            mWhisper.stop();
            mState = State.INIT;
        } else {
            QLog.VC_ASRLogI(mTag + " asr stop failed, current state is " + mState);
        }
    }

    private void deInitEngine() {
        QLog.VC_ASRLogD(mTag + " deInitEngine state=" + mState);
        if (mState == State.INIT) {
            mWhisper.deInit();
            mHandlerThread.quitSafely();
            mState = State.IDLE;
        } else {
            QLog.VC_ASRLogI(mTag + " asr deInit failed, current state is " + mState);
        }
    }

    private boolean isPlatformSupport(){
        return true;
    }
}