/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.voiceai.usecase.voicecall;

import static com.qualcomm.qti.voiceai.usecase.voicecall.Constants.PCM_SUFFIX;
import static com.qualcomm.qti.voiceai.usecase.voicecall.Constants.RECORDING_ROOT_PATH;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class VoiceCallRecordingStreamSaver {
    private final Context mContext;
    private DataOutputStream mOutputStream;
    private final String mTag;

    public VoiceCallRecordingStreamSaver(Context context, String tag) {
        mContext = context;
        mTag = tag;
    }

    private boolean isNotDebugMode() {
        return !Constants.isDebugMode();
    }

    public void onAppRecordingStart() {
        if (isNotDebugMode()) {
            return;
        }
        File outPath = new File(getPrivateFilesRootPath());
        if (!outPath.exists()) {
            outPath.mkdirs();
        }
        //asr_20240521_010500222_TX.pcm
        String timestamp = Constants.getCurrentTimestamp();
        File pcmFile = new File(outPath, "asr_" + timestamp + "_" + mTag + PCM_SUFFIX);
        try {
            if (!pcmFile.exists()) {
                pcmFile.createNewFile();
            }
            BufferedOutputStream buffer =
                    new BufferedOutputStream(Files.newOutputStream(pcmFile.toPath()));
            mOutputStream = new DataOutputStream(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onAppRecording(byte[] bytes, int start, int length) {
        if (isNotDebugMode()) {
            return;
        }
        try {
            if (mOutputStream != null) {
                mOutputStream.write(bytes, start, length);
            } else {
                QLog.VC_RecordingLogE("stream saver output stream null");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onAppRecordingStop() {
        if (isNotDebugMode()) {
            return;
        }
        if (mOutputStream != null) {
            try {
                mOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String getPrivateFilesRootPath() {
        return mContext.getFilesDir() + "/" + RECORDING_ROOT_PATH + "/"
                + Constants.getVoiceCallStartTime() + "/asr/";
    }
}
