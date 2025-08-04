/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.voiceai.usecase.voicecall.player;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.HandlerThread;

import com.qualcomm.qti.voiceai.usecase.voicecall.Constants;
import com.qualcomm.qti.voiceai.usecase.voicecall.QLog;
import com.qualcomm.qti.voiceai.usecase.voicecall.tts.SynthesisAudioItem;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Player {
    private Context mContext;
    private String mTag;
    private MediaPlayer mMediaPlayer;
    private MediaPlayer.OnCompletionListener mOnCompletionListener;
    private MediaPlayer.OnErrorListener mOnErrorListener;
    private AtomicInteger mReadyCount = new AtomicInteger(0);
    private LinkedBlockingQueue<SynthesisAudioItem> mWaitingPlaybackItemQueue = new LinkedBlockingQueue<>();
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private AtomicBoolean mIsPlaying = new AtomicBoolean(false);
    private AudioManager mAudioManager;

    public Player(Context context, String tag) {
        mContext = context;
        mAudioManager = context.getSystemService(AudioManager.class);
        mTag = tag;
        mMediaPlayer = new MediaPlayer();
        mOnCompletionListener = new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mReadyCount.decrementAndGet();
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        playNext();
                    }
                });
            }
        };

        mOnErrorListener = new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                mIsPlaying.set(false);
                mMediaPlayer.stop();
                return false;
            }
        };
        mMediaPlayer.setOnCompletionListener(mOnCompletionListener);
        mMediaPlayer.setOnErrorListener(mOnErrorListener);
        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mIsPlaying.set(true);
                mMediaPlayer.start();
            }
        });

        createHandler();
    }

    private void updateAttribute() {
        if(Constants.TX.equals(mTag)) {
            enableTelephonyRouting();
        } else {
            AudioAttributes audioAttributes = new AudioAttributes.Builder().
                    setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION).
                    setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build();
            mMediaPlayer.setAudioAttributes(audioAttributes);
        }
    }

    private void enableTelephonyRouting() {
        AudioDeviceInfo[] devices = mAudioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
        for (AudioDeviceInfo device : devices) {
            if ( device.getType() == AudioDeviceInfo.TYPE_TELEPHONY ){
                boolean res = mMediaPlayer.setPreferredDevice(device);
                QLog.VC_PlaybackLogD(mTag + " available device: " + device.getId() +  ", res=" + res);
                break;
            }
        }
    }

    private void createHandler() {
        mHandlerThread = new HandlerThread(mTag + "_player");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    private void playNext() {
        mMediaPlayer.reset();
        if (mReadyCount.get() > 0 ) {
            SynthesisAudioItem item = null;
            try {
                item = mWaitingPlaybackItemQueue.take();
                if(item != null) {
                    try (FileInputStream input = new FileInputStream(item.getSynthesisFile())) {
                        mMediaPlayer.setDataSource(input.getFD());
                        QLog.VC_PlaybackLogD(mTag + "playNext()");
                        mMediaPlayer.prepareAsync();
                        updateAttribute();
                    } catch (IOException e) {
                        QLog.VC_PlaybackLogE("playNext() error", e);
                    }
                }
            } catch (InterruptedException e) {
                QLog.VC_PlaybackLogE("playNext() error when take item", e);
            }
        } else {
            mIsPlaying.set(false);
        }
    }

    public void enqueue(SynthesisAudioItem item) {
        try {
            mWaitingPlaybackItemQueue.put(item);
        } catch (InterruptedException e) {
            QLog.VC_PlaybackLogE("enqueue item " + item.toString() + " error", e);
        }
    }

    public void dequeue(SynthesisAudioItem item) {
        boolean success = mWaitingPlaybackItemQueue.remove(item);
        if(!success) {
            QLog.VC_PlaybackLogE(mTag + " remove item failed");
        }
    }

    public void play() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                QLog.VC_PlaybackLogD(mTag + " play()");
                if(!mIsPlaying.get() && mReadyCount.get() == 0) {
                    mReadyCount.incrementAndGet();
                    playNext();
                } else {
                    mReadyCount.incrementAndGet();
                }
            }
        }, 200);
    }

    public void release() {
        mMediaPlayer.release();
        mHandlerThread.quitSafely();
        mWaitingPlaybackItemQueue.clear();
    }
}
