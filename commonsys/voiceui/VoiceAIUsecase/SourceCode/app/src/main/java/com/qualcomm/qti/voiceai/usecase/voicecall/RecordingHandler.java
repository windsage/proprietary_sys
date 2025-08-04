/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.voiceai.usecase.voicecall;

import static android.media.MediaRecorder.AudioSource.MIC;
import static android.media.MediaRecorder.AudioSource.VOICE_DOWNLINK;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.os.Handler;
import android.os.HandlerThread;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

public class RecordingHandler {

    private final String mTag;
    private final Context mContext;
    IRecordingListener mListener;
    private final Handler mHandler;
    private HandlerThread mHandlerThread;
    private VoiceCallRecordingInputStream mInputStream;
    private VoiceCallRecordingStreamSaver mStreamSaver;
    private final AtomicBoolean mRecording = new AtomicBoolean(false);
    private AudioRecord mAudioRecord;
    private AudioManager mAudioManager;
    public RecordingHandler(String tag, Context context, IRecordingListener listener) {
        mTag = tag;
        mContext = context;
        mListener = listener;
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mHandler = createHandler(tag);
    }

    interface IRecordingListener {
        void onRecordingStart(InputStream inputStream);
        void onRecordingStop();
    }

    private Handler createHandler(String tag) {
        mHandlerThread = new HandlerThread(tag + "_voice_call_recording_thread");
        mHandlerThread.start();
        return new Handler(mHandlerThread.getLooper());
    }

    public void startRecording() {
        if (mRecording.compareAndSet(false, true)) {
            mHandler.post(this::startRecordingInner);
        }
    }

    private void startRecordingInner() {
        if (mContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            if (mStreamSaver == null) {
                mStreamSaver = new VoiceCallRecordingStreamSaver(mContext, mTag);
            }
            mStreamSaver.onAppRecordingStart();
            enableFFECNSRecord(true);
            int audioSource = Constants.TX.equals(mTag) ? MIC : VOICE_DOWNLINK;
            mAudioRecord = new AudioRecord(audioSource, Constants.AUDIO_SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_FRONT, AudioFormat.ENCODING_PCM_16BIT,
                    Constants.BUFFER_SIZE);
            QLog.VC_RecordingLogD(mTag + " RecordingHandler startRecording");
            mAudioRecord.startRecording();
            mInputStream = new VoiceCallRecordingInputStream(mAudioRecord, mStreamSaver, mTag);
            mListener.onRecordingStart(mInputStream);
        }
    }

    public void stopRecording() {
        if (mRecording.compareAndSet(true, false)) {
            mHandler.post(this::stopRecordingInner);
        }
    }

    public void shutdown() {
        mHandlerThread.quitSafely();
    }

    private void stopRecordingInner() {
        QLog.VC_RecordingLogD(mTag + " RecordingHandler stopRecording");
        enableFFECNSRecord(false);
        if (mStreamSaver != null) {
            mStreamSaver.onAppRecordingStop();
            mStreamSaver = null;
        }
        if (mInputStream != null) {
            try {
                mInputStream.close();
                mInputStream = null;
                mAudioRecord = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void enableFFECNSRecord(boolean enable) {
        if (Constants.TX.equals(mTag)) {
            String value = enable ? "true" : "false";
            QLog.VC_RecordingLogD("enableFFECNSRecord setParameters("
                    + Constants.KEY_ENABLE_FFECNS_RECORD + "=" + value + ")");
            mAudioManager.setParameters(Constants.KEY_ENABLE_FFECNS_RECORD + "=" + value);
            QLog.VC_RecordingLogD("enableFFECNSRecord setParameters done");
        }
    }
}
