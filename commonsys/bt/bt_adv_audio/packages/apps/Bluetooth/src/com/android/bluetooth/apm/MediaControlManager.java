/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
*/
/*
 * Copyright (c) 2020-2021, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
       * Redistributions of source code must retain the above copyright
         notice, this list of conditions and the following disclaimer.
       * Redistributions in binary form must reproduce the above
         copyright notice, this list of conditions and the following
         disclaimer in the documentation and/or other materials provided
         with the distribution.
       * Neither the name of The Linux Foundation nor the names of its
         contributors may be used to endorse or promote products derived
         from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 **************************************************************************/

package com.android.bluetooth.apm;

import android.os.Binder;
import android.os.HandlerThread;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Log;
import com.android.internal.util.ArrayUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.AudioAttributes;
import android.media.AudioPlaybackConfiguration;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.MediaSession.QueueItem;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.util.Log;
import android.util.StatsLog;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;

import com.android.bluetooth.avrcp.MediaController;
import com.android.bluetooth.apm.StreamAudioService;
import com.android.bluetooth.mcp.McpService;

public class MediaControlManager {
    private static final boolean DBG = true;
    private static final String TAG = "APM: MediaControlManager";

    static MediaControlManager mMediaControlManager = null;

    PlaybackCallback mPlaybackCallbackCb;
    //MediaControlCallback mMediaControlCallbackCb;
    BroadcastReceiver mMediaControlReceiver;
    private static Context mContext;
    //private AudioManager mAudioManager;
    private Handler mHandler;
    private McpService mMcpService;
    public static final String MusicPlayerControlServiceName = "com.android.bluetooth.mcp.McpService";
    public static final int MUSIC_PLAYER_CONTROL = McpService.MUSIC_PLAYER_CONTROL;
    private MediaControlManager () {
        mPlaybackCallbackCb =  new PlaybackCallback();
        //mMediaControlCallbackCb = new MediaControlCallback();
        mMediaControlReceiver = new MediaControlReceiver();
    }

    public static MediaControlManager get() {
        Log.v(TAG, "get");
        return mMediaControlManager;
    }

    public static void make(Context context) {
        if(mMediaControlManager == null) {
            mMediaControlManager = new MediaControlManager();
            mMediaControlManager.init(context);
            MediaControlManagerIntf.init(mMediaControlManager);
            Log.v(TAG, "init, New mMediaControlManager instance");
        }
    }

    public void init(Context context) {
        mContext = context;
        /*mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        HandlerThread thread = new HandlerThread("MediaControlThread");
        Looper looper = thread.getLooper();
        mHandler = new Handler(looper);
        mAudioManager.registerAudioPlaybackCallback(mPlaybackCallbackCb,
                        mHandler);*/

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        mContext.registerReceiver(mMediaControlReceiver, filter);
        Log.v(TAG, "init done");
    }

    public void cleanup() {
        if(mMediaControlReceiver != null) {
          mContext.unregisterReceiver(mMediaControlReceiver);
        }
        mMediaControlReceiver = null;
    }

    private void updateCurrentPlayer (int playerId, int browseId) {
    }

    void handlePassthroughCmd(int op, int state) {
    }

    private class PlaybackCallback extends AudioManager.AudioPlaybackCallback {
        @Override
        public void onPlaybackConfigChanged(List<AudioPlaybackConfiguration> configs) {
            super.onPlaybackConfigChanged(configs);

            /*Update Playback config*/
        }
    }


    public void onMetadataChanged(MediaMetadata metadata) {
        /*Update Metadata change*/
        Log.v(TAG, "onMetadataChanged");
        mMcpService = McpService.getMcpService();
        if (mMcpService != null) {
           mMcpService.updateMetaData(metadata);
        }
    }

    public synchronized void onPlaybackStateChanged(PlaybackState state) {
        /*Update Playback State*/
        Log.v(TAG, "onPlaybackStateChanged");
        mMcpService = McpService.getMcpService();
        if (mMcpService != null) {
           mMcpService.updatePlaybackState(state);
        }

    }

    public synchronized void onPackageChanged(String packageName) {
        Log.v(TAG, "onPackageChanged");
        mMcpService = McpService.getMcpService();
        boolean removed = false;
        if (packageName == null)
            removed = true;
        if (mMcpService != null) {
           mMcpService.updatePlayerName(packageName, removed);
        }
    }
    public void onSessionDestroyed(String packageName) {
        Log.v(TAG, "onSessionDestroyed");
        mMcpService = McpService.getMcpService();
        if (mMcpService != null) {
           mMcpService.updatePlayerName(packageName, true);
        }
    }

    public void onQueueChanged(List<MediaSession.QueueItem> queue) {

    }

    private class MediaControlReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String packageName = null;
            String action = intent.getAction();
            boolean removed = false;
            Log.v(TAG, "action " + action);
            if(action == null)
                return;

            switch(action) {
                case Intent.ACTION_PACKAGE_REMOVED:
                    packageName = intent.getData().getSchemeSpecificPart();
                    /*handle package removed*/
                    removed = true;
                    break;
                case Intent.ACTION_PACKAGE_ADDED:
                    packageName = intent.getData().getSchemeSpecificPart();
                    /*handle package added*/
                    break;
                case Intent.ACTION_PACKAGE_CHANGED:
                    packageName = intent.getData().getSchemeSpecificPart();
                    /*handle package changed*/
                    break;
                default :
                    break;
            }
            mMcpService = McpService.getMcpService();
            if (mMcpService != null) {
                mMcpService.updatePlayerName(packageName, removed);
            }
        }
    }
}
