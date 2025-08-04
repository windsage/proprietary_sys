/*
 * Copyright (c) 2021, 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
*/
/*
 * Copyright (c) 2020, The Linux Foundation. All rights reserved.
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
 **************************************************************************

/**
 * Bluetooth ActiveDeviceManagerService. There is one instance each for
 *  voice and media profile management..
 *  - "Idle" and "Active" are steady states.
 *  - "Activating" and "Deactivating" are transient states until the
 *     SHO / Deactivation is completed.
 *
 *
 *                               (Idle)
 *                             |        ^
 *                   SetActive |        | Removed
 *                             V        |
 *                 (Activating)          (Deactivating)
 *                             |        ^
 *                 Activated   |        | setActive(NULL) / removeDevice
 *                             V        |
 *                      (Active / Broadcasting)
 *
 *
 */

package com.android.bluetooth.apm;

import static android.Manifest.permission.BLUETOOTH_CONNECT;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothLeAudio;
import com.android.bluetooth.Utils;
/*import com.android.bluetooth.btservice.AdapterService;
import com.android.bluetooth.btservice.ActiveDeviceManager;
import com.android.bluetooth.btservice.ProfileService;
import com.android.bluetooth.a2dp.A2dpService;
import com.android.bluetooth.hearingaid.HearingAidService;
import com.android.bluetooth.hfp.HeadsetService;*/
import com.android.bluetooth.mcp.McpService;
import com.android.bluetooth.broadcast.BroadcastService;
import com.android.bluetooth.cc.CCService;
import com.android.bluetooth.acm.AcmService;
import com.android.bluetooth.le_audio.LeAudioService;

import android.content.Intent;
import android.content.Context;
import android.os.Looper;
import android.os.Message;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Parcel;
import android.os.UserHandle;
import android.os.SystemProperties;
import android.util.Log;
import android.media.AudioManager;
import android.media.BluetoothProfileConnectionInfo;
import android.media.AudioDeviceAttributes;
import com.android.bluetooth.BluetoothStatsLog;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Boolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.lang.Integer;
import java.util.Scanner;
import java.util.Objects;
import android.media.audiopolicy.AudioProductStrategy;
import android.media.AudioDeviceAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioSystem;
import java.util.List;

public abstract class ActiveDeviceManagerService {
    private static final boolean DBG = true;
    private static final String TAG = "APM: ActiveDeviceManagerService";
    private static ActiveDeviceManagerService sActiveDeviceManager = null;
    private Context mContext;
    protected AudioManager mAudioManager;
    /*private static final HandlerThread[] thread = new HandlerThread[AudioType.SIZE];
    private static final ShoStateMachine[] sm = new ShoStateMachine[AudioType.SIZE];
    private ApmNativeInterface apmNative;
    private boolean txStreamSuspended = false;
    private final Lock lock = new ReentrantLock();
    private final Condition mediaHandoffComplete = lock.newCondition();
    private final Condition voiceHandoffComplete = lock.newCondition();
    private boolean mBroadcastStrategy = false;
    AudioProductStrategy strategy_notification;
    AudioProductStrategy strategy_ring;
    private boolean leaActiveDevUpdateSent = false;*/
    protected boolean mIsAdmPtsEnabled = false;

    static class Event {
        static final int SET_ACTIVE = 1;
        static final int ACTIVE_DEVICE_CHANGE = 2;
        static final int REMOVE_DEVICE = 3;
        static final int DEVICE_REMOVED = 4;
        static final int ACTIVATE_TIMEOUT = 5;
        static final int DEACTIVATE_TIMEOUT = 6;
        static final int SUSPEND_RECORDING = 7;
        static final int RESUME_RECORDING = 8;
        static final int RETRY_DEACTIVATE = 9;
        static final int RETRY_DISABLE_RECORDING = 10;
<<<<<<< HEAD   (c0a072 Merge "Xpan: Reset Low power Timer" into bt-sys.lnx.15.0)
        static final int RETRY_MEDIA_MODE_SWITCH = 11;
=======
        static final int RETRY_ACTIVATE = 11;
        static final int SET_ACTIVE_DELAYED = 12;
>>>>>>> CHANGE (43f7dc retry to activate when failed to switch active device)
        static final int STOP_SM = 0;
        static final int NONE = -1;
    }

    public static final int SHO_SUCCESS = 0;
    public static final int SHO_PENDING = 1;
    public static final int SHO_FAILED = 2;
    public static final int ALREADY_ACTIVE = 3;


    static final int RETRY_LIMIT = 4;
    static final int RETRY_DEACTIVATE_LIMIT = 40;
    static final int ACTIVATE_TIMEOUT_DELAY = 3000;
    static final int DEACTIVATE_TIMEOUT_DELAY = 2000;
    static final int DEACTIVATE_TRY_DELAY = 50;
    static final int DEACTIVATE_RECORDING_TRY_DELAY = 200;
    static final int ACTIVATE_RETRY_DELAY = 200;
    private static final int INVALID_SET_ID = 0x10;

    /*private ActiveDeviceManagerService (Context context) {
        mContext = context;
        /*mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            Objects.requireNonNull(mAudioManager,
                               "AudioManager cannot be null when ActiveDeviceManagerService starts");
        mBroadcastStrategy = SystemProperties.getBoolean("persist.vendor.service.bt.ba_strategy", false);
        thread[AudioType.MEDIA] = new HandlerThread("ActiveDeviceManager.MediaThread");
        thread[AudioType.MEDIA].start();
        Looper mediaLooper = thread[AudioType.MEDIA].getLooper();
        sm[AudioType.MEDIA] = new ShoStateMachine(AudioType.MEDIA, mediaLooper);

        thread[AudioType.VOICE] = new HandlerThread("ActiveDeviceManager.VoiceThread");
        thread[AudioType.VOICE].start();
        Looper voiceLooper = thread[AudioType.VOICE].getLooper();
        sm[AudioType.VOICE] = new ShoStateMachine(AudioType.VOICE, voiceLooper);

        mIsAdmPtsEnabled =
             SystemProperties.getBoolean("persist.vendor.service.bt.leaudio.pts", false);

        Log.d(TAG, "mIsAdmPtsEnabled: " + mIsAdmPtsEnabled);*/

        /*Log.d(TAG, "APM Init start");
        apmNative = ApmNativeInterface.getInstance();
        apmNative.init();
        Log.d(TAG, "APM Init complete");
    }*/

    public static ActiveDeviceManagerService get(Context context) {
        //if(sActiveDeviceManager == null) {
        sActiveDeviceManager = ActiveDeviceManagerServiceLegacy.get(context);
            //ActiveDeviceManagerServiceIntf.init(sActiveDeviceManager);
            //sActiveDeviceManager = new ActiveDeviceManagerServiceLegacy(context);
        //}
        return sActiveDeviceManager;
    }

    public static ActiveDeviceManagerService get() {
        return sActiveDeviceManager;
    }

    abstract public boolean setActiveDevice(BluetoothDevice device, Integer mAudioType,
                                   Boolean isUIReq, Boolean playReq);

    abstract public boolean setActiveDevice(BluetoothDevice device,
                                   Integer mAudioType, Boolean isUIReq);

    abstract public boolean setActiveDevice(BluetoothDevice device, Integer mAudioType);

    abstract public boolean setActiveDeviceBlocking(BluetoothDevice device, Integer mAudioType);

    abstract public boolean setActiveDeviceBlocking(BluetoothDevice device, Integer mAudioType, Boolean uiReq);

    abstract public BluetoothDevice getQueuedDevice(Integer mAudioType);

    abstract public boolean removeActiveDevice(Integer mAudioType, Boolean forceStopAudio);

    abstract public BluetoothDevice getActiveAbsoluteDevice(Integer mAudioType);

    abstract public BluetoothDevice getActiveDevice(Integer mAudioType);

    abstract public int getActiveProfile(Integer mAudioType);

    abstract public boolean onActiveDeviceChange(BluetoothDevice device, Integer mAudioType);

    abstract public boolean onActiveDeviceChange(BluetoothDevice device, Integer mAudioType, Integer mProfile);

    abstract public boolean isQcLeaEnabled();

    abstract public boolean isAospLeaEnabled();

    abstract public boolean enableBroadcast(BluetoothDevice device);

    abstract public boolean disableBroadcast();

    abstract public boolean enableGaming(BluetoothDevice device);

    abstract public boolean disableGaming(BluetoothDevice device);

    abstract public boolean enableGamingVbc(BluetoothDevice device);

    abstract public boolean disableGamingVbc(BluetoothDevice device);

    abstract public boolean enableRecording(BluetoothDevice device);

    abstract public boolean disableRecording(BluetoothDevice device);

    abstract public boolean suspendRecording(Boolean suspend);

    abstract public boolean isGamingActive(BluetoothDevice device);

    abstract public boolean isRecordingActive(BluetoothDevice device);

    abstract public boolean isStableState(Integer mAudioType);

    abstract public boolean isUpdatePending(int mAudioType);

    abstract public boolean isUpdatedPreviouselyToMM(int mAudioType);

    abstract public void resetUpdatedPreviouselyToMM(int mAudioType);

    private BluetoothProfileConnectionInfo getBroadcastProfile(boolean suppressNoisyIntent, int volume) {
        Log.d(TAG, "Create LeAudioBroadcast ProfileConnectionInfo");
        Parcel parcel = Parcel.obtain();
        parcel.writeInt(BluetoothProfile.LE_AUDIO_BROADCAST);
        parcel.writeBoolean(suppressNoisyIntent);
        parcel.writeInt(volume);
        parcel.writeBoolean(true /* mIsLeOutput */);
        parcel.setDataPosition(0);

        return BluetoothProfileConnectionInfo.CREATOR.createFromParcel(parcel);
    }

    private BluetoothProfileConnectionInfo getLeAudioOutputProfile(boolean suppressNoisyIntent,
            int volume) {
        Log.d(TAG, "getLeAudioOutputProfile, suppressNoisy: " + suppressNoisyIntent + ", volume: " + volume);
        Parcel parcel = Parcel.obtain();
        parcel.writeInt(BluetoothProfile.LE_AUDIO);
        parcel.writeBoolean(suppressNoisyIntent);
        parcel.writeInt(volume);
        parcel.writeBoolean(true /* isLeOutput */);
        parcel.setDataPosition(0);

        BluetoothProfileConnectionInfo profileInfo =
                BluetoothProfileConnectionInfo.CREATOR.createFromParcel(parcel);
        parcel.recycle();
        return profileInfo;
    }

    public void disable() {
        /*Log.d(TAG, "disable() called");
        sm[AudioType.MEDIA].sendMessage(Event.STOP_SM);
        sm[AudioType.VOICE].sendMessage(Event.STOP_SM);*/
    }

    public void cleanup() {
        /*Log.d(TAG, "cleanup() called");
        sm[AudioType.VOICE].doQuit();
        sm[AudioType.MEDIA].doQuit();
        thread[AudioType.VOICE].quitSafely();
        thread[AudioType.MEDIA].quitSafely();
        thread[AudioType.VOICE] = null;
        thread[AudioType.MEDIA] = null;
        sActiveDeviceManager = null;*/
    }

    private class DeviceProfileCombo {
        BluetoothDevice Device;
        BluetoothDevice absoluteDevice;
        int Profile;

        DeviceProfileCombo(BluetoothDevice mDevice, int mProfile) {
            Device = mDevice;
            Profile = mProfile;
        }

        DeviceProfileCombo() {
            Device = null;
            Profile = ApmConst.AudioProfiles.NONE;
        }
    }

    static class AudioType {
        public static final int VOICE = ApmConst.AudioFeatures.CALL_AUDIO;
        public static final int MEDIA = ApmConst.AudioFeatures.MEDIA_AUDIO;

        public static int SIZE = 2;
    }

    private class SHOReq {
        BluetoothDevice device;
        boolean PlayReq;
        int retryCount;
        boolean isBroadcast;
        boolean isGamingMode;
        boolean isGamingVbcMode;
        boolean isRecordingMode;
        boolean stopRecording;
        boolean stopBroadcasting;
        boolean isUIReq;
        boolean forceStopAudio;
        boolean isQueueUpdated;

        void copy(SHOReq src) {
            device = src.device;
            PlayReq = src.PlayReq;
            retryCount = src.retryCount;
            isBroadcast = src.isBroadcast;
            isGamingMode = src.isGamingMode;
            isGamingVbcMode = src.isGamingVbcMode;
            isRecordingMode = src.isRecordingMode;
            isUIReq = src.isUIReq;
            forceStopAudio = src.forceStopAudio;
            stopRecording = src.stopRecording;
        }

        void reset() {
            device = null;
            PlayReq = false;
            retryCount = 0;
            isBroadcast = false;
            isGamingMode = false;
            isGamingVbcMode = false;
            isRecordingMode = false;
            isUIReq = false;
            forceStopAudio = false;
            stopRecording = false;
        }
    }
}
