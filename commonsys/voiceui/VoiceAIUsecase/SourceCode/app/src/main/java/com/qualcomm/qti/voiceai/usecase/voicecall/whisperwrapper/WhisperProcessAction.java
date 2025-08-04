/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.voiceai.usecase.voicecall.whisperwrapper;

import com.qualcomm.qti.voice.assist.whisper.sdk.Whisper;

import java.io.InputStream;
import java.util.concurrent.RecursiveAction;

public class WhisperProcessAction extends RecursiveAction {

    private Whisper mWhisper;
    private InputStream mAudioStream;

    public WhisperProcessAction(Whisper whisper, InputStream audioStream) {
        mWhisper = whisper;
        mAudioStream = audioStream;
    }

    @Override
    protected void compute() {
        mWhisper.start(mAudioStream);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return super.cancel(mayInterruptIfRunning);
    }
}