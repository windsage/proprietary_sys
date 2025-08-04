/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.voiceai.usecase.voicecall;

import android.media.AudioRecord;

import java.io.IOException;
import java.io.InputStream;

public class VoiceCallRecordingInputStream extends InputStream {

    private AudioRecord mAudioRecord;
    private final VoiceCallRecordingStreamSaver mStreamSaver;
    private final String mTag;
    private final Object mRecordingStateLock = new Object();

    public VoiceCallRecordingInputStream(AudioRecord audioRecord,
                                         VoiceCallRecordingStreamSaver saver, String tag) {
        mAudioRecord = audioRecord;
        mStreamSaver = saver;
        mTag = tag;
    }

    @Override
    public int read() throws IOException {
        throw new UnsupportedOperationException("This operation is not supported!");
    }

    @Override
    public int read(byte[] buffer, int off, int len) throws IOException {
        if (mAudioRecord != null) {
            int read = mAudioRecord.read(buffer, off, len);
            mStreamSaver.onAppRecording(buffer, off, read);
            return read;
        } else {
            return 0;
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (mRecordingStateLock) {
            if (mAudioRecord != null) {
                if (mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    QLog.VC_RecordingLogD(mTag + " VoiceCallRecordingInputStream stop");
                    mAudioRecord.stop();
                    mAudioRecord.release();
                    mAudioRecord = null;
                }
            }
        }
    }
}
