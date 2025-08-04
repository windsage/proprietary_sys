/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.voiceai.usecase.voicecall.tts;

import static com.qualcomm.qti.voiceai.usecase.voicecall.Constants.RECORDING_ROOT_PATH;
import static com.qualcomm.qti.voiceai.usecase.voicecall.Constants.WAV_SUFFIX;

import android.content.Context;

import com.qualcomm.qti.voiceai.usecase.voicecall.Constants;
import com.qualcomm.qti.voiceai.usecase.voicecall.QLog;

import java.io.File;
import java.io.IOException;

public class SynthesisAudioItem {

    private final Context mContext;
    private final String mTag;
    private final String mText;
    private File mSynthesisFile;
    public SynthesisAudioItem(Context context, String tag, String text) {
        mContext = context;
        mTag = tag;
        mText = text;
        createSynthesisFile();
    }

    public String getText() {
        return mText;
    }

    public String getTag() {
        return mTag;
    }

    public File getSynthesisFile() {
        return mSynthesisFile;
    }

    private void createSynthesisFile() {
        String synthesisFile = "tts_" + mTag + WAV_SUFFIX;
        QLog.VC_TTSLogD("createSynthesisFile " + synthesisFile);
        File outPath = new File(mContext.getFilesDir(), RECORDING_ROOT_PATH + "/"
                + Constants.getVoiceCallStartTime() + "/tts/");
        if (!outPath.exists()) {
            outPath.mkdirs();
        }
        //tts_20240521_010500222_TX.wav
        mSynthesisFile = new File(outPath, synthesisFile);
        if (!mSynthesisFile.exists()) {
            try {
                mSynthesisFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String toString() {
        return "SynthesisAudioItem[" +
                "tag= " + mTag
                + ",text=" + mText
                + ",synthesisFile=" + mSynthesisFile.getAbsolutePath()
                + "]";
    }
}
