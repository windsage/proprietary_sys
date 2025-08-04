/* Copyright (c) 2016, 2021 Qualcomm Technologies, Inc.
   All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.ims.vt;

import android.content.Context;
import android.media.MediaActionSound;
import org.codeaurora.ims.ImsCallUtils;
import com.qualcomm.ims.utils.Log;
import com.qualcomm.ims.utils.Config;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

/**
 * Helper/wrapper class for playing sounds on various camera events, e.g. start/stop recording.
 * This class is not thread safe and its APIs shall be called from the same thread.
 *
 * @See MediaActionSound
 */
public class CameraActionSoundHelper {
    MediaActionSound mActionSound;
    Context mContext;
    private CompletableFuture<Void> mCompletableFuture;

    /**
     * Initializes <code> MediaActionSound</code> based on the R.config.config_enable_camera_sound
     * config. If the flag is not set calls to the member functions results in no-op.
     */
    CameraActionSoundHelper(Context context) {
        if (context == null) {
            Log.v(this, "Ctor: Context is null");
            return;
        }
        mContext = context;
    }

    /**
     * Loads media resources.
     *
     * @See MediaActionSound#load
     */
    public void open() {
        if (!Config.isConfigEnabled(mContext,
            org.codeaurora.ims.R.bool.config_enable_camera_sound)) {
            return;
        }

        if (mActionSound == null) {
            mActionSound = new MediaActionSound();
        }

        if (mCompletableFuture == null) {
            Log.v(this, "open: Loading media files.");
            mCompletableFuture =
                CompletableFuture.runAsync(() -> {
                    mActionSound.load(MediaActionSound.START_VIDEO_RECORDING);
                    mActionSound.load(MediaActionSound.STOP_VIDEO_RECORDING);
                });
        }
    }

    /**
     * Releases media resources.
     *
     * @See MediaActionSound#release
     */
    public void close() {
        if (mActionSound == null) {
            return;
        }

        if (mCompletableFuture != null && !mCompletableFuture.isDone()) {
            try {
                mCompletableFuture.cancel(true);
            } catch (CancellationException e) {
                Log.i(this, "exception: " + e.getMessage());
            }
        }
        mCompletableFuture = null;

        Log.v(this, "close: Releasing resources");
        mActionSound.release();
        mActionSound = null;
    }

    /**
     * Plays video recording started sound.
     *
     * @See MediaActionSound#START_VIDEO_RECORDING
     */
    public void onRecordingStarted() {
        if (mActionSound == null || !isSoundActionLoaded()) {
            return;
        }

        Log.v(this, "onRecordingStarted:");
        mActionSound.play(MediaActionSound.START_VIDEO_RECORDING);
    }

    /**
     * Plays video recording stopped sound.
     *
     * @See MediaActionSound#STOP_VIDEO_RECORDING
     */
    public void onRecordingStopped() {
        if (mActionSound == null || !isSoundActionLoaded()) {
            return;
        }

        Log.v(this, "onRecordingStopped:");
        mActionSound.play(MediaActionSound.STOP_VIDEO_RECORDING);
    }

    private boolean isSoundActionLoaded() {
        return mCompletableFuture != null && mCompletableFuture.isDone()
            && !mCompletableFuture.isCompletedExceptionally()
            && !mCompletableFuture.isCancelled();
    }
}
