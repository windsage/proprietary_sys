/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.voiceai.usecase.voicecall.whisperwrapper;

import java.io.File;
import java.io.InputStream;
import java.util.List;

public interface IWhisperEngine {
    void init(WhisperEngine.IInitResultCallback resultCallback);
    void start(InputStream audioStream);
    void stop();
    void deInit();
    void setListener(IResultListener listener);
    void enableContinuousTranscription(boolean enabled);
    boolean continuousTranscriptionEnabled();
    void setVadLenFuture(int value);
    int getVadLenFuture();
    void setVadLenHangover(int value);
    int getVadLenHangover();
    void setVadThreshold(int value);
    int getVadThreshold();
    void setVadCmdEndDetectionThreshold(int value);
    int getVadCmdEndDetectionThreshold();
    void setTranslationEnabled(boolean enabled);
    boolean getTranslationEnabled();
    void setLanguage(String language);
    List<String> getLanguages();
    void enablePartialTranscriptions(boolean enabled);
    boolean partialTranscriptionsEnabled();
    void setMinBufferMs(int ms);
    int getMinBufferMs();
    void suppressNonSpeech(boolean suppress);
    boolean getSuppressNonSpeech();
    void showProgressIndicators(boolean show);
    boolean getShowProgressIndicators();
    String getVersion();
    String[] getSupportedLanguageCodes();
    String[] getSupportedLanguages();
    int getAILogLevel();
    void setAILogLevel(int level);
    void setDebugDir(File dir);
    File getDebugDir();
    boolean isDebugEnabled();
    void enableDebug(boolean enabled);
}
