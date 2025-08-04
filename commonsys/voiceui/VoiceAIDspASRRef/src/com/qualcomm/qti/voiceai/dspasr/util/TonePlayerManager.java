/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.voiceai.dspasr.util;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

import com.qualcomm.qti.voiceai.dspasr.R;


public class TonePlayerManager implements SoundPool.OnLoadCompleteListener {

    public static final int SUCCESS_TONE_INDEX = 0;
    private SoundPool mSoundPool;
    private static final int MAX_STREAMS = 8;
    private static final int[] RES_IDS = {
            R.raw.succeed,
            R.raw.fail_m};
    private final int[] mSoundIdArray;
    private final boolean[] mSoundIdReadyArray;
    private final Runnable[] mPendingPlaybackTasks = new Runnable[RES_IDS.length];

    public TonePlayerManager(Context context) {
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build();
        mSoundPool = new SoundPool.Builder()
                .setMaxStreams(MAX_STREAMS)
                .setAudioAttributes(attributes)
                .build();
        mSoundPool.setOnLoadCompleteListener(this);
        mSoundIdArray = new int[RES_IDS.length];
        mSoundIdReadyArray = new boolean[RES_IDS.length];
        for (int i = 0; i < RES_IDS.length; i++) {
            mSoundIdArray[i] = mSoundPool.load(context, RES_IDS[i], 1);
            mSoundIdReadyArray[i] = false;
        }
    }

    @Override
    public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
        if (status != 0) {
            return;
        }
        for (int i = 0; i < mSoundIdArray.length; i++) {
            if (sampleId == mSoundIdArray[i]) {
                mSoundIdReadyArray[i] = true;
                break;
            }
        }
        for (int i = 0; i < mPendingPlaybackTasks.length; i++) {
            if (sampleId == mSoundIdArray[i] && null != mPendingPlaybackTasks[i]) {
                mPendingPlaybackTasks[i].run();
                break;
            }
        }
    }

    public void playMayWait(final int resId) {
        if (mSoundIdReadyArray[resId]){
            mSoundPool.play(mSoundIdArray[resId],
                    0.5f, 0.5f, 0, 0, 1f);
        } else {
            mPendingPlaybackTasks[resId] = () -> mSoundPool.play(mSoundIdArray[resId],
                    0.5f, 0.5f, 0, 0, 1f);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        release();
        super.finalize();
    }

    private void release() {
        if (mSoundPool != null) {
            mSoundPool.release();
            mSoundPool = null;
        }
    }
}
