/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.voiceai.usecase.voicecall;

import static com.qualcomm.qti.voiceai.usecase.voicecall.Constants.RECORDING_ROOT_PATH;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import com.qualcomm.qti.voiceai.usecase.Facade;
import com.qualcomm.qti.voiceai.usecase.translation.TranslateProgressListener;
import com.qualcomm.qti.voiceai.usecase.translation.TranslationManager;
import com.qualcomm.qti.voiceai.usecase.translation.TranslatorSupportedLanguage;
import com.qualcomm.qti.voiceai.usecase.translation.TranslatorWrapper;
import com.qualcomm.qti.voiceai.usecase.voicecall.tts.TtsManager;
import com.qualcomm.qti.voiceai.usecase.voicecall.whisperwrapper.IResultListener;
import com.qualcomm.qti.voiceai.usecase.voicecall.whisperwrapper.WhisperEngine;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;

public abstract class LinkRouteManager {
    private final static String TAG = LinkRouteManager.class.getSimpleName();
    private final Context mContext;
    private final String mTag;
    private RecordingHandler mRecordingHandler;
    private RecordingHandler.IRecordingListener mRecordingListener;
    private WhisperEngine mWhisperEngine;
    private IResultListener mResultListener;
    private TranslationManager mTranslationManager;
    private TranslateProgressListener mTranslationProgressListener;
    private final HashMap<String, TranslatorWrapper> mTranslatorCacheMap = new HashMap<>();
    private TtsManager mTtsManager;
    private String mASRLanguage;
    private String mTranslationLanguage;

    enum State {IDLE, INIT, START}
    private State mState = State.IDLE;

    public LinkRouteManager(final Context context, final String tag) {
        mContext = context;
        mTag = tag;
    }

    public void init() {
        if (mState == State.IDLE) {
            initRecordingHandler();
            initWhisperEngine();
            initTranslationManager();
            initTtsManager();
            mState = State.INIT;
        } else {
            QLog.VCLogD(TAG, "ignore init request, current state is " + mState.name());
        }
    }

    private void initRecordingHandler() {
        mRecordingListener = new RecordingHandler.IRecordingListener() {
            @Override
            public void onRecordingStart(InputStream inputStream) {
                QLog.VC_RecordingLogD(mTag + " onRecordingStart");
                enableWhisperDebug(Constants.isDebugMode());
                mWhisperEngine.setLanguage(getASRLanguage());
                mWhisperEngine.start(inputStream);
            }

            @Override
            public void onRecordingStop() {
                QLog.VC_RecordingLogD(mTag + " onRecordingStop");
            }
        };
        mRecordingHandler = new RecordingHandler(mTag, mContext, mRecordingListener);
    }

    private void enableWhisperDebug(boolean enable) {
        File file = enable ? new File(mContext.getFilesDir() + "/" + RECORDING_ROOT_PATH
                + "/" + Constants.getVoiceCallStartTime() + "/asr/whisper" + mTag + "/") : null;
        mWhisperEngine.setDebugDir(file);
        mWhisperEngine.enableDebug(enable);
    }

    private void initWhisperEngine() {
        mResultListener = new IResultListener() {
            @Override
            public void onTranscription(String transcription, String language, boolean finalized,
                                        int code, Bundle extra) {
                QLog.VC_ASRLogD(mTag + " onTranscription transcription=" + transcription +
                        ",language=" + language + ", finalized=" + finalized + ", code=" + code
                        + ",extra=" + extra);
                if (finalized && transcription != null && !TextUtils.isEmpty(transcription.trim())) {
                    startTranslation(language, transcription);
                }
            }

            @Override
            public void onError(int errorCode, Exception ex) {
                QLog.VC_ASRLogE(mTag + " " + errorCode);
            }

            @Override
            public void onRecordingStopped() {
                QLog.VC_ASRLogD(mTag + " onRecordingStopped");
            }

            @Override
            public void onSpeechStart() {
                QLog.VC_ASRLogD(mTag + " onSpeechStart");
            }

            @Override
            public void onSpeechEnd() {
                QLog.VC_ASRLogD(mTag + " onSpeechEnd");
            }
        };
        mWhisperEngine = new WhisperEngine(mContext, mTag);
        mWhisperEngine.setListener(mResultListener);
        mWhisperEngine.enableContinuousTranscription(true);
        mWhisperEngine.setNoStartSpeechTimeout(Integer.MAX_VALUE);
        mWhisperEngine.init(null);
    }

    private void startTranslation(String language, String text) {
        QLog.VC_TranslationLogD(mTag + " startTranslation language=" + language + ",text=" + text);
        TranslatorSupportedLanguage source = TranslatorSupportedLanguage.convert(language);
        String translationLang = getTranslationLanguage();
        TranslatorSupportedLanguage target = TranslatorSupportedLanguage.convert(translationLang);
        //key:zh_en
        String key = translatorWrapperKey(source, target);
        if (!mTranslatorCacheMap.containsKey(key)) {
            TranslatorWrapper wrapper = mTranslationManager.createTranslator(source, target);
            mTranslatorCacheMap.put(key, wrapper);
        }
        TranslatorWrapper wrapper = mTranslatorCacheMap.get(key);
        //translationId TX_zh_en_20240521_170723222
        String translationId = getTranslationId(mTag, key);
        QLog.VC_TranslationLogD(mTag + " startTranslation translationId=" + translationId);
        mTranslationManager.translate(wrapper, mTranslationProgressListener, translationId, text);
    }

    private void initTranslationManager() {
        mTranslationManager = Facade.getTranslationManager();
        mTranslationProgressListener = new TranslateProgressListener() {
            @Override
            public void onDone(String translationId, String result) {
                QLog.VC_TranslationLogD(mTag + " onDone translationId=" + translationId
                        + ",result=" + result);
                if(result != null && TextUtils.isEmpty(result.trim())) return;
                String language = getTargetLanguageFromTranslationId(translationId);
                mTtsManager.setLanguage(language);
                mTtsManager.enqueue(result);
            }

            @Override
            public void onError(String translationId) {
                QLog.VC_TranslationLogD(mTag + " onError translationId=" + translationId);
            }
        };
    }

    private String translatorWrapperKey(TranslatorSupportedLanguage source,
                                        TranslatorSupportedLanguage target) {
        return source.getLanguage() + "_" + target.getLanguage();
    }

    private String getTranslationId(String tag, String source_target) {
        String time = Constants.getCurrentTimestamp();
        return tag + "_" + source_target + "_" + time;
    }

    private String getTargetLanguageFromTranslationId(String translationId) {
        String[] splits = translationId.split("_");
        return splits[2];
    }

    private void initTtsManager() {
        mTtsManager = new TtsManager(mContext, mTag);
    }


    public void start() {
        if (mState == State.INIT) {
            mRecordingHandler.startRecording();
            mState = State.START;
        } else {
            QLog.VCLogD(TAG, "ignore start request, current state is " + mState.name());
        }
    }

    public void stop() {
        if (mState == State.START) {
            mRecordingHandler.stopRecording();
            mWhisperEngine.stop();
            releaseTranslators();
            mTtsManager.stop();
            mState = State.INIT;
        } else {
            QLog.VCLogD(TAG, "ignore stop request, current state is " + mState.name());
        }
    }

    private void releaseTranslators() {
        for (TranslatorWrapper wrapper : mTranslatorCacheMap.values()) {
            mTranslationManager.releaseTranslator(wrapper);
        }
        mTranslatorCacheMap.clear();
    }

    public void deinit() {
        if (mState == State.INIT) {
            mWhisperEngine.deInit();
            mTtsManager.shutdown();
            mRecordingHandler.shutdown();
            clearDump();
            mState = State.IDLE;
        } else {
            QLog.VCLogD(TAG, "ignore deinit request, current state is " + mState.name());
        }
    }

    private void clearDump() {
        if (!Constants.isDebugMode()) {
            Path path = Path.of(mContext.getFilesDir().getAbsolutePath(), RECORDING_ROOT_PATH);
            try {
                Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                QLog.VCLogI("cleardump", e.getMessage());
            }
        }
    }

    public String getASRLanguage() {
        return mASRLanguage;
    }

    public void setASRLanguage(String language) {
        if (TextUtils.equals(mASRLanguage, language)) {
            return;
        }
        QLog.VC_ASRLogD(mTag + " current asr language " + mASRLanguage + ",change to " + language);
        mASRLanguage = language;
        if (mWhisperEngine != null) {
            mWhisperEngine.setLanguage(mASRLanguage);
        }
    }

    public String getTranslationLanguage() {
        return mTranslationLanguage;
    }

    public void setTranslationLanguage(String language) {
        if (TextUtils.equals(mTranslationLanguage, language)) {
            return;
        }
        QLog.VC_ASRLogD(mTag + " current translation language " + mTranslationLanguage + ",change to " + language);
        mTranslationLanguage = language;
    }
}


