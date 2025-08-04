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
 * Bluetooth ActiveDeviceManagerServiceLegacy. There is one instance each for
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
import com.android.bluetooth.btservice.AdapterService;
import com.android.bluetooth.btservice.ActiveDeviceManager;
import com.android.bluetooth.btservice.ProfileService;
import com.android.bluetooth.a2dp.A2dpService;
import com.android.bluetooth.hearingaid.HearingAidService;
import com.android.bluetooth.hfp.HeadsetService;
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

public class ActiveDeviceManagerServiceLegacy extends ActiveDeviceManagerService {
    private static final boolean DBG = true;
    private static final String TAG = "APM: ActiveDeviceManagerServiceLegacy";
    private static ActiveDeviceManagerServiceLegacy sActiveDeviceManager = null;
    private AudioManager mAudioManager;
    private static final HandlerThread[] thread = new HandlerThread[AudioType.SIZE];
    private static final ShoStateMachine[] sm = new ShoStateMachine[AudioType.SIZE];
    private ApmNativeInterface apmNative;
    private Context mContext;
    private boolean txStreamSuspended = false;
    private final Lock lock = new ReentrantLock();
    private final Condition mediaHandoffComplete = lock.newCondition();
    private final Condition voiceHandoffComplete = lock.newCondition();
    private boolean mBroadcastStrategy = false;
    AudioProductStrategy strategy_notification;
    AudioProductStrategy strategy_ring;
    private boolean leaActiveDevUpdateSent = false;
    private boolean mIsAdmPtsEnabled = false;
    /*static class Event {
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
        static final int RETRY_ACTIVATE = 11;
        static final int STOP_SM = 0;
    }*/

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
    private ActiveDeviceManagerServiceLegacy (Context context) {
        mContext = context;
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            Objects.requireNonNull(mAudioManager,
                               "AudioManager cannot be null when ActiveDeviceManagerServiceLegacy starts");
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
        Log.d(TAG, "mIsAdmPtsEnabled: " + mIsAdmPtsEnabled);

        Log.d(TAG, "APM Init start");
        apmNative = ApmNativeInterface.getInstance();
        apmNative.init();
        Log.d(TAG, "APM Init complete");
    }

    public static ActiveDeviceManagerServiceLegacy get(Context context) {
        if(sActiveDeviceManager == null) {
            sActiveDeviceManager = new ActiveDeviceManagerServiceLegacy(context);
            ActiveDeviceManagerServiceIntf.init(sActiveDeviceManager);
        }
        return sActiveDeviceManager;
    }

    public static ActiveDeviceManagerServiceLegacy get() {
        return sActiveDeviceManager;
    }

    public boolean setActiveDevice(BluetoothDevice device, Integer mAudioType,
                                   Boolean isUIReq, Boolean playReq) {
        Log.d(TAG, "setActiveDevice(" + device + ") audioType: " + mAudioType +
                          " isUIReq: " + isUIReq + " playReq: " + playReq  );
        boolean isCallActive = false;
        if(ApmConst.AudioFeatures.CALL_AUDIO == mAudioType) {
            CallAudio mCallAudio = CallAudio.get();
            isCallActive = mCallAudio.isAudioOn();
        }

        int activeProfile = getActiveProfile(mAudioType);
        BluetoothDevice activeAbsoluteDevice = getActiveAbsoluteDevice(mAudioType);
        StreamAudioService streamAudioService = StreamAudioService.getStreamAudioService();
        BluetoothDevice activeAbsolutegroupDevice = null;
        BluetoothDevice groupDevice = null;
        if (streamAudioService != null) {
            activeAbsolutegroupDevice = streamAudioService.getDeviceGroup(activeAbsoluteDevice);
            groupDevice = streamAudioService.getDeviceGroup(device);
        }


        Log.d(TAG, "setActiveDevice(): activeProfile: " + activeProfile +
                   ", activeAbsoluteDevice: " + activeAbsoluteDevice +
                   ", activeAbsolutegroupDevice: " + activeAbsolutegroupDevice +
                   ", groupDevice: " + groupDevice);
        if((device != null) && !device.equals(activeAbsoluteDevice) &&
           (groupDevice != null) && groupDevice.equals(activeAbsolutegroupDevice)) {
           Log.d(TAG, "setActiveDevice(): Check for active ongoing use-case.");
           if (ApmConst.AudioProfiles.BAP_RECORDING == activeProfile) {
               enableRecording(device);
               return true;
           } else if (ApmConst.AudioProfiles.BAP_GCP == activeProfile) {
               enableGaming(device);
               return true;
           } else if (ApmConst.AudioProfiles.BAP_GCP_VBC == activeProfile) {
               enableGamingVbc(device);
               return true;
           }
        }

        synchronized(sm[mAudioType]) {
            sm[mAudioType].mSHOQueue.device = device;
            sm[mAudioType].mSHOQueue.isUIReq = isUIReq;
            sm[mAudioType].mSHOQueue.PlayReq = (playReq || isCallActive);
            sm[mAudioType].mSHOQueue.isBroadcast = false;
            sm[mAudioType].mSHOQueue.isRecordingMode = false;
            sm[mAudioType].mSHOQueue.isGamingMode = false;
            sm[mAudioType].mSHOQueue.isGamingVbcMode = false;
            sm[mAudioType].mSHOQueue.stopBroadcasting = false;
            Handler mHandler = sm[mAudioType].getHandler();
            if (isStableState(mAudioType) &&
                ((mHandler != null &&
                mHandler.hasMessages(Event.REMOVE_DEVICE)) ||
                sm[mAudioType].mMsgProcessing == Event.REMOVE_DEVICE)) {
                sm[mAudioType].mSHOQueue.isQueueUpdated = true;
                Log.d(TAG,"SetActive while deactivating, queue updated");
            }
        }

        if(device != null) {
            sm[mAudioType].sendMessage(Event.SET_ACTIVE);
        } else {
            if (ApmConst.getQtiLeAudioEnabled() &&
                mAudioType == AudioType.MEDIA &&
                sm[mAudioType].mState == sm[mAudioType].mBroadcasting) {
                Log.d(TAG, "LE Broadcast is active, ignore REMOVE_DEVICE");
            } else {
                sm[mAudioType].sendMessage(Event.REMOVE_DEVICE);
            }
        }
        return isUIReq;
    }

    public boolean setActiveDevice(BluetoothDevice device,
                                   Integer mAudioType, Boolean isUIReq) {
        Log.d(TAG, "setActiveDevice(" + device + ") audioType: " + mAudioType +
                                                  " isUIReq: " + isUIReq);
        return setActiveDevice(device, mAudioType, isUIReq, false);
    }

    public boolean setActiveDevice(BluetoothDevice device, Integer mAudioType) {
        Log.d(TAG, "setActiveDevice(" + device + ") audioType: " + mAudioType);
        return setActiveDevice(device, mAudioType, false, false);
    }

    public boolean setActiveDeviceBlocking(BluetoothDevice device, Integer mAudioType) {
        Log.d(TAG, "setActiveDeviceBlocking: Enter");
        if(ApmConst.AudioFeatures.CALL_AUDIO == mAudioType ||
           (ApmConst.getAospLeaEnabled() &&
            (ApmConst.AudioFeatures.MEDIA_AUDIO == mAudioType))) {
            sm[mAudioType].mWaitForLock = true;
            setActiveDevice(device, mAudioType, false, false);
            try {
                if (sm[mAudioType].mWaitForLock) {
                    lock.lock();
                    Log.d(TAG, "setActiveDeviceBlocking: acquired lock waiting");
                    voiceHandoffComplete.await();
                }
            } catch (InterruptedException e) {
                Log.d(TAG, "setActiveDeviceBlocking: Unblocked because of exception: " + e);
            } finally {
                Log.d(TAG, "setActiveDeviceBlocking: unlock");
                lock.unlock();
            }
        }
        Log.d(TAG, "setActiveDeviceBlocking: Exit");
        return true;
    }
    public boolean setActiveDeviceBlocking(BluetoothDevice device, Integer mAudioType, Boolean uiReq) {
        Log.d(TAG, "setActiveDeviceBlocking with uireq: Enter");
        if(ApmConst.AudioFeatures.CALL_AUDIO == mAudioType ||
           (ApmConst.getAospLeaEnabled() &&
            (ApmConst.AudioFeatures.MEDIA_AUDIO == mAudioType))) {
            sm[mAudioType].mWaitForLock = true;
            setActiveDevice(device, mAudioType, uiReq, false);
            try {
                if (sm[mAudioType].mWaitForLock) {
                    lock.lock();
                    Log.d(TAG, "setActiveDeviceBlocking with uireq:: acquired lock waiting");
                    voiceHandoffComplete.await();
                }
            } catch (InterruptedException e) {
                Log.d(TAG, "setActiveDeviceBlocking with uireq:: Unblocked because of exception: " + e);
            } finally {
                Log.d(TAG, "setActiveDeviceBlocking with uireq:: unlock");
                lock.unlock();
            }
        }
        Log.d(TAG, "setActiveDeviceBlocking with uireq:: Exit");
        return true;
    }

    public BluetoothDevice getQueuedDevice(Integer mAudioType) {
        return sm[mAudioType].mSHOQueue.device;
    }

    public boolean removeActiveDevice(Integer mAudioType, Boolean forceStopAudio) {
        sm[mAudioType].mSHOQueue.forceStopAudio = forceStopAudio;
        setActiveDevice(null, mAudioType, false);
        return true;
    }

    public BluetoothDevice getActiveAbsoluteDevice(Integer mAudioType) {
        Log.d(TAG, "getActiveAbsoluteDevice(): device: " +
                                     sm[mAudioType].Current.absoluteDevice);
        return sm[mAudioType].Current.absoluteDevice;
    }

    public BluetoothDevice getActiveDevice(Integer mAudioType) {
        Log.d(TAG, "getActiveDevice(): device: " + sm[mAudioType].Current.Device);
        return sm[mAudioType].Current.Device;
    }

    public int getActiveProfile(Integer mAudioType) {
        if(sm[mAudioType].mState == sm[mAudioType].mBroadcasting) {
            // Use Current.Profile here
            return ApmConst.AudioProfiles.BROADCAST_LE;
        } else if(sm[mAudioType].mState != sm[mAudioType].mIdle) {
            Log.d(TAG, "getActiveProfile returning profile " + sm[mAudioType].Current.Profile);
            return sm[mAudioType].Current.Profile;
        }
        return ApmConst.AudioProfiles.NONE;
    }

    public boolean onActiveDeviceChange(BluetoothDevice device, Integer mAudioType) {
        return onActiveDeviceChange(device, mAudioType, ApmConst.AudioProfiles.NONE);
    }

    public boolean onActiveDeviceChange(BluetoothDevice device, Integer mAudioType, Integer mProfile) {
        if (device != null || mProfile == ApmConst.AudioProfiles.BROADCAST_LE) {
            DeviceProfileCombo mDeviceProfileCombo = new DeviceProfileCombo(device, mProfile);
            sm[mAudioType].sendMessage(Event.ACTIVE_DEVICE_CHANGE, mDeviceProfileCombo);
        }
        else
            sm[mAudioType].sendMessage(Event.DEVICE_REMOVED);
        return true;
    }

    public boolean isQcLeaEnabled() {
        Log.d(TAG, "isQcLeaEnabled()");
        return apmNative.isQcLeaEnabled();
    }

    public boolean isAospLeaEnabled() {
        Log.d(TAG, "isAospLeaEnabled()");
        return apmNative.isAospLeaEnabled();
    }
    public boolean enableBroadcast(BluetoothDevice device) {
        synchronized(sm[AudioType.MEDIA]) {
            sm[AudioType.MEDIA].mSHOQueue.device = device;
            sm[AudioType.MEDIA].mSHOQueue.isBroadcast = true;
            sm[AudioType.MEDIA].mSHOQueue.PlayReq = false;
            sm[AudioType.MEDIA].mSHOQueue.stopBroadcasting = false;
        }
        sm[AudioType.MEDIA].sendMessage(Event.SET_ACTIVE);
        return true;
    }

    public boolean disableBroadcast() {
        Log.d(TAG, "disableBroadcast");
        synchronized(sm[AudioType.MEDIA]) {
            sm[AudioType.MEDIA].mSHOQueue.isBroadcast = false;
            sm[AudioType.MEDIA].mSHOQueue.PlayReq = false;
            sm[AudioType.MEDIA].mSHOQueue.stopBroadcasting = true;
        }
        if (sm[AudioType.MEDIA].mState == sm[AudioType.MEDIA].mBroadcasting) {
            sm[AudioType.MEDIA].sendMessage(Event.REMOVE_DEVICE);
        }
        return true;
    }

    public boolean enableGaming(BluetoothDevice device) {
        if (sm[AudioType.MEDIA].mState == sm[AudioType.MEDIA].mGamingMode &&
                        device.equals(getActiveAbsoluteDevice(AudioType.MEDIA))) {
            Log.d(TAG, "Device already in Gaming Mode");
            return true;
        }

        Log.d(TAG, "enableGaming(): Gaming Tx only. ");

        synchronized(sm[AudioType.MEDIA]) {
            sm[AudioType.MEDIA].mSHOQueue.device = device;
            sm[AudioType.MEDIA].mSHOQueue.isBroadcast = false;
            sm[AudioType.MEDIA].mSHOQueue.isGamingMode = true;
            sm[AudioType.MEDIA].mSHOQueue.PlayReq = false;
        }
        sm[AudioType.MEDIA].sendMessage(Event.SET_ACTIVE);
        return true;
    }

    public boolean disableGaming(BluetoothDevice device) {
        if (sm[AudioType.MEDIA].mState != sm[AudioType.MEDIA].mGamingMode) {
            Log.e(TAG, "Gaming Mode not active");
            return true;
        }
       if (sm[AudioType.MEDIA].mState == sm[AudioType.MEDIA].mGamingMode &&
               !device.equals(getActiveAbsoluteDevice(AudioType.MEDIA))) {
            Log.e(TAG, "Gaming Mode not active for target device " + device);
            return true;
        }
        Log.d(TAG, "disableGaming(): Gaming Tx or both Tx/Rx");

        /*MediaAudio mMediaAudio = MediaAudio.get();
        if(mMediaAudio != null && mMediaAudio.isA2dpPlaying(device)) {
            Log.w(TAG, "Gaming Stream is Active");
            return false;
        }*/

        synchronized(sm[AudioType.MEDIA]) {
            sm[AudioType.MEDIA].mSHOQueue.device = device;
            sm[AudioType.MEDIA].mSHOQueue.isGamingMode = false;
            sm[AudioType.MEDIA].mSHOQueue.isGamingVbcMode = false;
            sm[AudioType.MEDIA].mSHOQueue.PlayReq = false;
            sm[AudioType.MEDIA].mSHOQueue.isUIReq = true;
        }

        //sm[AudioType.MEDIA].sendMessageDelayed(Event.SET_ACTIVE, DEACTIVATE_TRY_DELAY);
        sm[AudioType.MEDIA].sendMessage(Event.SET_ACTIVE);
        return true;
    }

    public boolean enableGamingVbc(BluetoothDevice device) {
        Log.d(TAG, "enableGamingVbc() ");
        if (sm[AudioType.MEDIA].mState == sm[AudioType.MEDIA].mGamingMode &&
             sm[AudioType.MEDIA].mIsGcpVbcConnectionUpdated == true &&
                         device.equals(getActiveDevice(AudioType.MEDIA))) {
             Log.d(TAG, "Device already in Gaming VBC Mode");
             return true;
        }

        synchronized(sm[AudioType.MEDIA]) {
            sm[AudioType.MEDIA].mSHOQueue.device = device;
            sm[AudioType.MEDIA].mSHOQueue.isBroadcast = false;
            sm[AudioType.MEDIA].mSHOQueue.isGamingMode = true;
            sm[AudioType.MEDIA].mSHOQueue.isGamingVbcMode = true;
            sm[AudioType.MEDIA].mSHOQueue.PlayReq = false;
        }
        sm[AudioType.MEDIA].sendMessage(Event.SET_ACTIVE);
        return true;
    }

    public boolean disableGamingVbc(BluetoothDevice device) {
        if (sm[AudioType.MEDIA].mState != sm[AudioType.MEDIA].mGamingMode) {
            Log.e(TAG, "disableGamingVbc(): Gaming Mode not active");
            return true;
        } else {
            Log.d(TAG, "disableGamingVbc(): wait for disableGaming()" + device);
            return true;
        }
    }

    public boolean enableRecording(BluetoothDevice device) {
        Log.d(TAG, "enableRecording: " + device);

        MediaAudio mMediaAudio = MediaAudio.get();
        if(ApmConst.getQtiLeAudioEnabled() && txStreamSuspended == false) {
            Log.d(TAG, "Set A2dpSuspended=true");
            mAudioManager.setParameters("A2dpSuspended=true");
            txStreamSuspended = true;
        }

        synchronized(sm[AudioType.MEDIA]) {
            sm[AudioType.MEDIA].mSHOQueue.device = device;
            sm[AudioType.MEDIA].mSHOQueue.isBroadcast = false;
            sm[AudioType.MEDIA].mSHOQueue.isGamingMode = false;
            sm[AudioType.MEDIA].mSHOQueue.isGamingVbcMode = false;
            sm[AudioType.MEDIA].mSHOQueue.isRecordingMode = true;
            sm[AudioType.MEDIA].mSHOQueue.PlayReq = false;
            sm[AudioType.MEDIA].mSHOQueue.isUIReq = true;
        }
        sm[AudioType.MEDIA].sendMessage(Event.SET_ACTIVE);
        return true;
    }

    public boolean disableRecording(BluetoothDevice device) {
        Log.d(TAG, "disableRecording: " + device);

        synchronized(sm[AudioType.MEDIA]) {
            sm[AudioType.MEDIA].mSHOQueue.device = device;
            sm[AudioType.MEDIA].mSHOQueue.isRecordingMode = false;
            sm[AudioType.MEDIA].mSHOQueue.stopRecording = true;
            sm[AudioType.MEDIA].mSHOQueue.PlayReq = false;
        }

        if (sm[AudioType.MEDIA].mState == sm[AudioType.MEDIA].mRecordingMode) {
            sm[AudioType.MEDIA].sendMessage(Event.SET_ACTIVE);
        }
        return true;
    }

    public boolean suspendRecording(Boolean suspend) {
        Log.d(TAG, "suspendRecording: " + suspend);

        if (ApmConst.getQtiLeAudioEnabled() &&
            sm[AudioType.MEDIA].mState == sm[AudioType.MEDIA].mRecordingMode) {
          if(suspend) {
            sm[AudioType.MEDIA].sendMessage(Event.SUSPEND_RECORDING);
          } else {
            sm[AudioType.MEDIA].sendMessage(Event.RESUME_RECORDING);
          }
        }
        return true;
    }

    public boolean isGamingActive(BluetoothDevice device) {
        boolean isGamingActive =
            (sm[AudioType.MEDIA].mState == sm[AudioType.MEDIA].mGamingMode);
        Log.d(TAG, "isGamingActive(): " + isGamingActive);
        return isGamingActive;
    }

    public boolean isRecordingActive(BluetoothDevice device) {
        boolean isRecordingActive =
            (sm[AudioType.MEDIA].mState == sm[AudioType.MEDIA].mRecordingMode);
        Log.d(TAG, "isRecordingActive(): "  + isRecordingActive);
        return isRecordingActive;
    }

    public boolean isStableState(Integer mAudioType) {
        State state = sm[mAudioType].mState;
        boolean isStableState =
                    (!(sm[mAudioType].mActivating == state ||
                       sm[mAudioType].mDeactivating == state));
        Log.d(TAG, "isStableState(): "  + isStableState);
        return isStableState;
    }

    public boolean isUpdatePending(int mAudioType) {
        boolean isUpdatePending = sm[mAudioType].updatePending;
        Log.d(TAG, "Audio type: " + mAudioType + ", isUpdatePending: "  + isUpdatePending);
        return isUpdatePending;
    }

    public boolean isUpdatedPreviouselyToMM(int mAudioType) {
        boolean updatedPreviouselyToMM = sm[mAudioType].updatedPreviouselyToMM;
        Log.d(TAG, "Audio type: " + mAudioType +
                   ", updatedPreviouselyToMM: "  + updatedPreviouselyToMM);
        return updatedPreviouselyToMM;
    }
		/*No Action required in Legacy Handling*/

    public void resetUpdatedPreviouselyToMM(int mAudioType) {
        Log.d(TAG, "Audio type: " + mAudioType +
                   ", updatedPreviouselyToMM: "  + sm[mAudioType].updatedPreviouselyToMM);
        if (sm[mAudioType].updatedPreviouselyToMM) {
            sm[mAudioType].updatedPreviouselyToMM = false;
        }
    }

    private void broadcastLeActiveDeviceChange(BluetoothDevice device) {
        if (DBG) {
            Log.d(TAG, "broadcastLeActiveDeviceChange(" + device + ")");
        }

        Intent intent = new Intent(BluetoothLeAudio.ACTION_LE_AUDIO_ACTIVE_DEVICE_CHANGED);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT
                        | Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND);

        //A2dpService mA2dpService = A2dpService.getA2dpService();
        LeAudioService mLeAudioService = LeAudioService.getLeAudioService();
        if(mLeAudioService == null) {
            Log.e(TAG, "Le Audio Service not ready");
            return;
        }
        mLeAudioService.sendBroadcastAsUser(intent, UserHandle.ALL,
                                            BLUETOOTH_CONNECT,
                         Utils.getTempAllowlistBroadcastOptions());
    }

    private void broadcastActiveDeviceChange(BluetoothDevice device, int mAudioType) {
        if (DBG) {
            Log.d(TAG, "broadcastActiveDeviceChange(" + device + ")");
        }

        Intent intent;
        if (mAudioType == AudioType.MEDIA) {
            intent = new Intent(BluetoothA2dp.ACTION_ACTIVE_DEVICE_CHANGED);
        } else {
            intent = new Intent(BluetoothHeadset.ACTION_ACTIVE_DEVICE_CHANGED);
        }

        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT
                        | Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND);
        if(mAudioType == AudioType.MEDIA) {
            A2dpService mA2dpService = A2dpService.getA2dpService();
            if(mA2dpService == null) {
                Log.e(TAG, "A2dp Service not ready");
                return;
            }
            mA2dpService.sendBroadcast(intent, BLUETOOTH_CONNECT,
                                       Utils.getTempAllowlistBroadcastOptions());
        } else {
            HeadsetService mHeadsetService = HeadsetService.getHeadsetService();
            if(mHeadsetService == null) {
                Log.e(TAG, "Headset Service not ready");
                return;
            }
            mHeadsetService.sendBroadcastAsUser(intent, UserHandle.ALL, BLUETOOTH_CONNECT,
                   Utils.getTempAllowlistBroadcastOptions());
        }
    }

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
        Log.d(TAG, "disable() called");
        sm[AudioType.MEDIA].sendMessage(Event.STOP_SM);
        sm[AudioType.VOICE].sendMessage(Event.STOP_SM);
    }

    public void cleanup() {
        Log.d(TAG, "cleanup() called");
        if (sm[AudioType.VOICE] != null) {
            sm[AudioType.VOICE].doQuit();
        }

        if (sm[AudioType.MEDIA] != null) {
            sm[AudioType.MEDIA].doQuit();
        }

        if (thread[AudioType.VOICE] != null) {
            thread[AudioType.VOICE].quitSafely();
            thread[AudioType.VOICE] = null;
        }

        if (thread[AudioType.MEDIA] != null) {
            thread[AudioType.MEDIA].quitSafely();
            thread[AudioType.MEDIA] = null;
        }

        sActiveDeviceManager = null;
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

    private final class ShoStateMachine extends StateMachine {
        private static final boolean DBG = true;
        private static final String TAG = "APM: ActiveDeviceManagerServiceLegacy";

        static final int IDLE = 0;
        static final int ACTIVATING = 1;
        static final int ACTIVE = 2;
        static final int DEACTIVATING = 3;
        static final int BROADCAST_ACTIVE = 4;
        static final int GAMING_ACTIVE = 5;
        static final int RECORDING_ACTIVE = 6;

        private Idle mIdle;
        private Activating mActivating;
        private Active mActive;
        private Deactivating mDeactivating;
        private Broadcasting mBroadcasting;
        private Gaming mGamingMode;
        private Recording mRecordingMode;

        private DeviceProfileCombo Current;
        private DeviceProfileCombo Target;
        private SHOReq mTargetSHO;
        private SHOReq mSHOQueue;
        private boolean mWaitForLock;
        private DeviceProfileMap dpm;

        private int mAudioType;
        private State mState;
        private State mPrevState = null;
        private State mPrevStableState = null;
        boolean mIsGcpVbcConnectionUpdated = false;
        private BluetoothDevice mPrevActiveDevice;
        private BluetoothDevice mPrevActiveUcastDevice;
        private int mPrevActiveProfile = ApmConst.AudioProfiles.NONE;
        private int mPrevActiveUcastProfile = ApmConst.AudioProfiles.NONE;
        boolean enabled;
        boolean updatePending = false;
        boolean mBroadcastActive = false;
        boolean updatedPreviouselyToMM = false;
        boolean updateAbsoluteDevicePending = false;
        boolean mRecordingSuspended = false;
        boolean mdoQuitSetLeActiveDeviceToAudio = false;
        private int mMsgProcessing = Event.NONE;
        private String sAudioType;

        ShoStateMachine (int audioType, Looper looper) {
            super(TAG, looper);
            setDbg(DBG);

            mIdle = new Idle();
            mActivating = new Activating();
            mActive = new Active();
            mDeactivating = new Deactivating();
            mBroadcasting = new Broadcasting();
            mGamingMode = new Gaming();
            mRecordingMode = new Recording();

            Current = new DeviceProfileCombo();
            Target = new DeviceProfileCombo();
            mSHOQueue = new SHOReq();
            mTargetSHO = new SHOReq();

            addState(mIdle);
            addState(mActivating);
            addState(mActive);
            addState(mDeactivating);
            addState(mBroadcasting);
            addState(mGamingMode);
            addState(mRecordingMode);

            mAudioType = audioType;
            if(mAudioType == AudioType.MEDIA)
                sAudioType = new String("MEDIA");
            else if(mAudioType == AudioType.VOICE)
                sAudioType = new String("VOICE");

            enabled =  true;
            setInitialState(mIdle);
            start();
        }

        public void doQuit () {
            Log.i(TAG, "Stopping SHO StateMachine for " + mAudioType);
            if (ApmConst.getAospLeaEnabled() &&
                (sm[mAudioType].mState == sm[mAudioType].mActive ||
                 sm[mAudioType].mState == sm[mAudioType].mDeactivating)) {
                if ((mAudioType == AudioType.MEDIA &&
                    sm[mAudioType].Current.Profile != ApmConst.AudioProfiles.HAP_BREDR &&
                    sm[mAudioType].Current.Profile != ApmConst.AudioProfiles.A2DP) ||
                    (mAudioType == AudioType.VOICE &&
                     sm[mAudioType].Current.Profile != ApmConst.AudioProfiles.HFP)) {
                    mdoQuitSetLeActiveDeviceToAudio = true;
                    CallAudio mCallAudio = CallAudio.get();
                    if(mCallAudio != null && mCallAudio.isVoipLeaWarEnabled()) {
                        broadcastActiveDeviceChange (null, AudioType.VOICE);
                    }
                    broadcastLeActiveDeviceChange (null);
                    mAudioManager.handleBluetoothActiveDeviceChanged(null, sm[mAudioType].Current.Device,
                            BluetoothProfileConnectionInfo.createLeAudioInfo(false, true));
                    mAudioManager.handleBluetoothActiveDeviceChanged(null, sm[mAudioType].Current.Device,
                            BluetoothProfileConnectionInfo.createLeAudioInfo(false, false));
                    Log.d(TAG, "doQuit update le active device to audio");
                }
            }
            sAudioType = null;
            quitNow();
        }

       /* public void cleanUp {

        }*/

        private String messageWhatToString(int msg) {
            switch (msg) {
                case Event.SET_ACTIVE:
                    return "SET ACTIVE";
                case Event.ACTIVE_DEVICE_CHANGE:
                    return "ACTIVE DEVICE CHANGED";
                case Event.REMOVE_DEVICE:
                    return "REMOVE DEVICE";
                case Event.DEVICE_REMOVED:
                    return "REMOVED";
                case Event.ACTIVATE_TIMEOUT:
                    return "SET ACTIVE TIMEOUT";
                case Event.DEACTIVATE_TIMEOUT:
                    return "REMOVE DEVICE TIMEOUT";
                case Event.STOP_SM:
                    return "STOP STATE MACHINE";
                default:
                    break;
            }
            return Integer.toString(msg);
        }

        int startSho(BluetoothDevice device, int profile) {
            MediaAudio mMediaAudio = MediaAudio.get();
            int ret = SHO_FAILED;
            StreamAudioService streamAudioService;
            AcmService mAcmService = AcmService.getAcmService();
            Log.e(TAG, " startSho() for device: " + device + ", for profile: " + profile);
            switch (profile) {
                case ApmConst.AudioProfiles.A2DP:
                    A2dpService a2dpService = A2dpService.getA2dpService();
                    if(a2dpService != null) {
                        // pass play status here
                        DeviceProfileMap dpm = DeviceProfileMap.getDeviceProfileMapInstance();
                        int supportedProfiles = dpm.getSupportedProfile(device, ApmConst.AudioFeatures.MEDIA_AUDIO);
                        if(device != null && (supportedProfiles & ApmConst.AudioProfiles.A2DP)
                                                == ApmConst.AudioProfiles.NONE) {
                            Log.d(TAG, "Device does not support A2dp. Try Group Members");
                            streamAudioService = StreamAudioService.getStreamAudioService();
                            List<BluetoothDevice> groupMembers =
                                      streamAudioService.getGroupMembers(streamAudioService.getDeviceGroup(device));

                            if (groupMembers != null) {
                                for (BluetoothDevice memberDevice: groupMembers) {
                                    supportedProfiles = dpm.getSupportedProfile(memberDevice, ApmConst.AudioFeatures.MEDIA_AUDIO);
                                    if((supportedProfiles & ApmConst.AudioProfiles.A2DP) == ApmConst.AudioProfiles.A2DP) {
                                        device = memberDevice;
                                    }
                                }
                            } else {
                                Log.d(TAG, "startSho: groupMembers is null");
                            }
                        }
                        ret = a2dpService.setActiveDevice(device, false);
                    } else {
                        ret = SHO_SUCCESS;
                    }
                    break;
                case ApmConst.AudioProfiles.HFP:
                    HeadsetService headsetService = HeadsetService.getHeadsetService();
                    if(headsetService != null) {
                        DeviceProfileMap dpm = DeviceProfileMap.getDeviceProfileMapInstance();
                        int supportedProfiles = dpm.getSupportedProfile(device, ApmConst.AudioFeatures.CALL_AUDIO);
                        if(device != null && (supportedProfiles & ApmConst.AudioProfiles.HFP)
                                                == ApmConst.AudioProfiles.NONE) {
                            Log.d(TAG, "Device does not support HFP. Try Group Members");
                            streamAudioService = StreamAudioService.getStreamAudioService();
                            List<BluetoothDevice> groupMembers =
                                      streamAudioService.getGroupMembers(streamAudioService.getDeviceGroup(device));

                            if (groupMembers != null) {
                                for (BluetoothDevice memberDevice: groupMembers) {
                                    supportedProfiles = dpm.getSupportedProfile(memberDevice, ApmConst.AudioFeatures.CALL_AUDIO);
                                    if((supportedProfiles & ApmConst.AudioProfiles.HFP) == ApmConst.AudioProfiles.HFP) {
                                        device = memberDevice;
                                    }
                                }
                            } else {
                                Log.d(TAG, "startSho: groupMembers is null");
                            }
                        }

                        ret = headsetService.setActiveDeviceHF(device);
                    } else {
                        ret = SHO_SUCCESS;
                    }
                    break;
                case ApmConst.AudioProfiles.TMAP_MEDIA:
                case ApmConst.AudioProfiles.BAP_MEDIA:
                    streamAudioService = StreamAudioService.getStreamAudioService();
                    if (streamAudioService == null &&
                        mAcmService == null && device == null) {
                        Log.w(TAG, "startSho(): streamAudioService, mAcmService and " +
                                   " device are null, fake success.");
                        return SHO_SUCCESS;
                    }
                    ret = streamAudioService.setActiveDevice(device, ApmConst.AudioProfiles.BAP_MEDIA, false);
                    if (ret == ActiveDeviceManagerServiceLegacy.ALREADY_ACTIVE) {
                      ret = SHO_SUCCESS;
                    }
                    break;
                case ApmConst.AudioProfiles.BAP_RECORDING:
                    streamAudioService = StreamAudioService.getStreamAudioService();
                    if (streamAudioService == null &&
                        mAcmService == null && device == null) {
                        Log.w(TAG, "startSho(): streamAudioService, mAcmService and " +
                                   " device are null, fake success.");
                        return SHO_SUCCESS;
                    }
                    ret = streamAudioService.setActiveDevice(device, ApmConst.AudioProfiles.BAP_RECORDING, false);
                    if (ret == ActiveDeviceManagerServiceLegacy.ALREADY_ACTIVE) {
                      ret = SHO_SUCCESS;
                    }
                    break;
                case ApmConst.AudioProfiles.TMAP_CALL:
                case ApmConst.AudioProfiles.BAP_CALL:
                    streamAudioService = StreamAudioService.getStreamAudioService();
                    if (streamAudioService == null &&
                        mAcmService == null && device == null) {
                        Log.w(TAG, "startSho(): streamAudioService, mAcmService and " +
                                   " device are null, fake success.");
                        return SHO_SUCCESS;
                    }
                    ret = streamAudioService.setActiveDevice(device, profile, false);
                    break;
                case ApmConst.AudioProfiles.BAP_GCP:
                    streamAudioService = StreamAudioService.getStreamAudioService();
                    if (streamAudioService == null &&
                        mAcmService == null && device == null) {
                        Log.w(TAG, "startSho(): streamAudioService, mAcmService and " +
                                   " device are null, fake success.");
                        return SHO_SUCCESS;
                    }
                    ret = streamAudioService.setActiveDevice(device,
                                                             ApmConst.AudioProfiles.BAP_GCP,
                                                             false);
                    if (ret == ActiveDeviceManagerServiceLegacy.ALREADY_ACTIVE) {
                        ret = SHO_SUCCESS;
                    }
                    break;
                case ApmConst.AudioProfiles.BAP_GCP_VBC:
                    streamAudioService = StreamAudioService.getStreamAudioService();
                    if (streamAudioService == null &&
                        mAcmService == null && device == null) {
                        Log.w(TAG, "startSho(): streamAudioService, mAcmService and " +
                                   " device are null, fake success.");
                        return SHO_SUCCESS;
                    }
                    Log.e(TAG, " startSho() for device: " + device + ", triggr VBC");
                    ret = streamAudioService.setActiveDevice(device,
                                                             ApmConst.AudioProfiles.BAP_GCP_VBC,
                                                             false);
                    if (ret == ActiveDeviceManagerServiceLegacy.ALREADY_ACTIVE) {
                      ret = SHO_SUCCESS;
                    }
                    break;
                case ApmConst.AudioProfiles.BROADCAST_LE:
                    //ret = SHO_SUCCESS;//broadcastService.setActiveDevice();
                    BroadcastService mBroadcastService = BroadcastService.getBroadcastService();
                    if (mBroadcastService != null)
                        ret = mBroadcastService.setActiveDevice(device);

                    Log.d(TAG, " startSho() for Broadcast ret: " + ret);
                    if (!ApmConst.getQtiLeAudioEnabled()) {
                        if (ret == SHO_SUCCESS && device == null) {
                            boolean stopAudio = false;
                            if (mState == mDeactivating ||
                                    AdapterService.getAdapterService().getState() != BluetoothAdapter.STATE_ON) {
                                stopAudio = true;
                                if (mPrevActiveUcastDevice != null) {
                                    // To avoid race condition for BLE unicast and BLE broadcast disconnected
                                    // events sending to MM-Audio simutaneously while BT turning off
                                    Log.d(TAG, "BT is turning off, Ignore updating Broadcast inactive to MM-Audio");
                                    break;
                                }
                            } else {
                                if (mPrevActiveUcastDevice != null && (!mPrevActiveUcastDevice.equals(Target.Device) ||
                                        !isSameProfile(mPrevActiveUcastProfile, Target.Profile, mAudioType))) {
                                    Log.d(TAG, "notify MM-audio BCAST/UCAST active change until enter ACTIVE state");
                                    break;
                                }
                            }
                            Log.d(TAG, " Update broadcast device inactive to MM Audio, stopAudio " + stopAudio);
                            mAudioManager.handleBluetoothActiveDeviceChanged(
                                    null, Current.Device, getBroadcastProfile(!stopAudio, -1));
                                    //BluetoothProfileConnectionInfo.createLeAudioBroadcastInfo(!stopAudio));
                        }
                    }
                    break;
                case ApmConst.AudioProfiles.HAP_BREDR:
                    HearingAidService hearingAidService = HearingAidService.getHearingAidService();
                    if (hearingAidService != null) {
                        ret = hearingAidService.setActiveDevice(device) ?  SHO_SUCCESS : SHO_FAILED;
                    } else {
                        ret = SHO_SUCCESS;
                    }
                    break;
            }
            return ret;
        }

        int getConnectionState(BluetoothDevice device, int profile) {
            Log.d(TAG,"getConnectionState: " + device.getAddress() + " for profile: " + profile);
            int state = BluetoothProfile.STATE_DISCONNECTED;
            if (profile == ApmConst.AudioProfiles.A2DP) {
                A2dpService a2dpService = A2dpService.getA2dpService();
                if (a2dpService != null) {
                    state = a2dpService.getConnectionState(device);
                } else {
                    state = BluetoothProfile.STATE_DISCONNECTED;
                }
            } else {
                AcmService mAcmService = AcmService.getAcmService();
                if (device.getAddress().contains(ApmConst.groupAddress)) {
                    Log.d(TAG,"getConnectionState: device: " + device + " is a group address");
                    StreamAudioService mStreamAudioService =
                                         StreamAudioService.getStreamAudioService();
                    List<BluetoothDevice> groupMembers = (mStreamAudioService != null) ?
                                           mStreamAudioService.getGroupMembers(device) : null;
                    if (groupMembers != null) {
                        Log.d(TAG,"getConnectionState: Fetch connection state of groupmembers.");
                        for (BluetoothDevice memberDevice: groupMembers) {
                             device = memberDevice;
                             if (mAcmService != null) {
                                 state = mAcmService.getConnectionState(device);
                                 Log.d(TAG,"getConnectionState: device: " + device +
                                           ", state:" + state);
                                 if (state == BluetoothProfile.STATE_CONNECTED) {
                                     Log.d(TAG,"getConnectionState: Atleast one member is connected.");
                                     break;
                                 }
                             } else {
                                 state = BluetoothProfile.STATE_DISCONNECTED;
                             }
                        }
                    }
                } else {
                    if (mAcmService != null) {
                        Log.d(TAG,"getConnectionState from ACM");
                        state = mAcmService.getConnectionState(device);
                    } else {
                        state = BluetoothProfile.STATE_DISCONNECTED;
                    }
                }
            }
            Log.d(TAG,"getConnectionState: " + state);
            return state;
        }

        class Idle extends State {
            @Override
            public void enter() {
                synchronized (this) {
                    mState = mIdle;
                    mMsgProcessing = Event.NONE;
                    leaActiveDevUpdateSent = false;
                    log(" Idle leaActiveDevUpdateSent reset " + sAudioType);

                }
                Current.Device = null;
                Current.Profile = ApmConst.AudioProfiles.NONE;
                //2 Update dependent profiles
                log("mdoQuitSetLeActiveDeviceToAudio: " + mdoQuitSetLeActiveDeviceToAudio);
                if(mPrevState != null && mPrevActiveDevice != null) {
                    if (mAudioType == AudioType.MEDIA &&
                        mPrevActiveProfile != ApmConst.AudioProfiles.HAP_BREDR) {
                        MediaAudio mMediaAudio = MediaAudio.get();
                        boolean stopAudio = mTargetSHO.forceStopAudio ||
                                       (getConnectionState(mPrevActiveDevice, mPrevActiveProfile)
                                         != BluetoothProfile.STATE_CONNECTED);
                        /*TODO: Add profile check here*/
                        if(ApmConst.getQtiLeAudioEnabled()) {
                            broadcastActiveDeviceChange (null, mAudioType);
                            if(mAudioManager != null) {
                                log("De-Activate Device " + mPrevActiveDevice + " stopAudio: " + stopAudio);
                                mAudioManager.handleBluetoothActiveDeviceChanged(null, mPrevActiveDevice,
                                    BluetoothProfileConnectionInfo.createA2dpInfo(!stopAudio, -1));
                            }
                        } else {
                            if (mPrevActiveProfile == ApmConst.AudioProfiles.A2DP) {
                                broadcastActiveDeviceChange (null, mAudioType);
                                log("De-Activate Device " + mPrevActiveDevice + " stopAudio: " + stopAudio);
                                mAudioManager.handleBluetoothActiveDeviceChanged(null, mPrevActiveDevice,
                                        BluetoothProfileConnectionInfo.createA2dpInfo(!stopAudio, -1));
                            } else if (!mdoQuitSetLeActiveDeviceToAudio) {
                                broadcastLeActiveDeviceChange (null);

                                mAudioManager.handleBluetoothActiveDeviceChanged(null, mPrevActiveDevice,
                                        BluetoothProfileConnectionInfo.createLeAudioInfo(!stopAudio, true));

                                mAudioManager.handleBluetoothActiveDeviceChanged(null, mPrevActiveDevice,
                                        BluetoothProfileConnectionInfo.createLeAudioInfo(!stopAudio, false));
                            }
                        }
                    } else if(mAudioType == AudioType.VOICE) {
                        if(ApmConst.getQtiLeAudioEnabled()) {
                            broadcastActiveDeviceChange (null, mAudioType);
                            CallAudio mCallAudio = CallAudio.get();
                            if(mCallAudio != null && mCallAudio.isVoiceOrCallActive()) {
                                mCallAudio.stopScoUsingVirtualVoiceCall();
                            }
                        } else {
                            if (mPrevActiveProfile != ApmConst.AudioProfiles.HFP &&
                                !mdoQuitSetLeActiveDeviceToAudio) {
                                CallAudio mCallAudio = CallAudio.get();
                                if(mCallAudio != null && mCallAudio.isVoipLeaWarEnabled()) {
                                    broadcastActiveDeviceChange (null, mAudioType);
                                }
                                broadcastLeActiveDeviceChange (null);
                                boolean stopAudio = mTargetSHO.forceStopAudio ||
                                               (getConnectionState(mPrevActiveDevice, mPrevActiveProfile)
                                                 != BluetoothProfile.STATE_CONNECTED);

                                mAudioManager.handleBluetoothActiveDeviceChanged(null, mPrevActiveDevice,
                                        BluetoothProfileConnectionInfo.createLeAudioInfo(!stopAudio, true));

                                mAudioManager.handleBluetoothActiveDeviceChanged(null, mPrevActiveDevice,
                                        BluetoothProfileConnectionInfo.createLeAudioInfo(!stopAudio, false));
                            } else if (mPrevActiveProfile == ApmConst.AudioProfiles.HFP) {
                                log("Previous active profile is hfp, broadcast active device change");
                                broadcastActiveDeviceChange (null, mAudioType);
                            }
                        }
                    }

                    VolumeManager mVolumeManager = VolumeManager.get();
                    if(mVolumeManager != null) {
                        mVolumeManager.onActiveDeviceChange(Current.Device, mAudioType);
                    }
                }

                if(txStreamSuspended && mAudioType == AudioType.MEDIA) {
                    mAudioManager.setParameters("A2dpSuspended=false");
                    txStreamSuspended = false;
                }
                if (mBroadcastStrategy && mAudioType == AudioType.MEDIA) {
                    List<AudioProductStrategy> audioProductStrategies =
                                             AudioProductStrategy.getAudioProductStrategies();
                    Log.d(TAG,"audioProductStrategies size: " + audioProductStrategies.size());
                    for (final AudioProductStrategy aps : audioProductStrategies) {
                        if (aps.getAudioAttributesForLegacyStreamType(AudioSystem.STREAM_NOTIFICATION) != null) {
                            strategy_notification = aps;
                        } else if(aps.getAudioAttributesForLegacyStreamType(AudioSystem.STREAM_RING) != null) {
                            strategy_ring = aps;
                        }
                    }
                    if (strategy_notification != null && strategy_ring != null) {
                        mAudioManager.removePreferredDeviceForStrategy(strategy_notification);
                        mAudioManager.removePreferredDeviceForStrategy(strategy_ring);
                    }
                }
                log("mPrevActiveProfile: " + mPrevActiveProfile);

                    lock.lock();
                    voiceHandoffComplete.signal();
                mWaitForLock = false;
                    lock.unlock();
                    Log.d(TAG, "unlock by signal");

                mPrevActiveProfile = ApmConst.AudioProfiles.NONE;
                if (!enabled) {
                    log("state machine stopped");
                }
            }

            @Override
            public void exit() {
                mPrevState = mIdle;
                mPrevStableState = mIdle;
                mPrevActiveDevice = null;
                mPrevActiveUcastDevice = null;
                mPrevActiveProfile = ApmConst.AudioProfiles.NONE;
            }

            @Override
            public boolean processMessage(Message message) {
                log("Idle: Process Message (" + mAudioType + "): "
                        + messageWhatToString(message.what));
                if(!enabled) {
                    log("State Machine not running. Returning");
                    return NOT_HANDLED;
                }
                mMsgProcessing = message.what;
                switch(message.what) {
                    case Event.SET_ACTIVE:
                        if(mSHOQueue.device == null) {
                            Log.w(TAG, "Invalid request");
                            break;
                        }
                        transitionTo(mActivating);
                        break;

                    case Event.ACTIVE_DEVICE_CHANGE:
                        /* Might move to active here*/
                    case Event.REMOVE_DEVICE:
                        lock.lock();
                        voiceHandoffComplete.signal();
                        mWaitForLock = false;
                        lock.unlock();
                        log("Idle: Process Message Ignored");
                        break;
                    case Event.STOP_SM:
                        enabled = false;
                        log("state machine stopped");
                        break;
                    default:
                        return NOT_HANDLED;
                }
                return HANDLED;
            }
        }

        class Activating extends State {
            int ret;
            @Override
            public void enter() {
                synchronized (this) {
                    mState = mActivating;
                    updatePending = true;

                    Target.Device = mSHOQueue.device;
                    Target.absoluteDevice = Target.Device;
                    mTargetSHO.copy(mSHOQueue);
                    mSHOQueue.reset();
                    Log.w(TAG, "Activating " + sAudioType + " Device: " + Target.Device);
                }

                dpm = DeviceProfileMap.getDeviceProfileMapInstance();
                StreamAudioService streamAudioService = StreamAudioService.getStreamAudioService();
                BluetoothDevice groupDevice = streamAudioService.getDeviceGroup(Target.Device);

                if(Target.Device == null || groupDevice == null) {
                    Log.e(TAG, "Target/Group Device is null, Returning");
                    transitionTo(mPrevState);
                    updatePending = false;
                    return;
                }

                if (mTargetSHO.isBroadcast) {
                    Target.Profile = dpm.getProfile(Target.Device, ApmConst.AudioFeatures.BROADCAST_AUDIO);
                    mSHOQueue.device = Current.absoluteDevice;
                    mSHOQueue.isUIReq = false;
                } else if (mTargetSHO.isRecordingMode) {
                    //mSHOQueue.device = Current.Device;
                    Target.Profile = ApmConst.AudioProfiles.BAP_RECORDING;
                    mSHOQueue.isUIReq = false;
                } else if (mTargetSHO.isGamingMode) {
                    if (mTargetSHO.isGamingVbcMode) {
                        Target.Profile = ApmConst.AudioProfiles.BAP_GCP_VBC;
                    } else {
                        /*Only single profile supports gaming Mode*/
                        int mConnProf = streamAudioService.getAcmPreferredProfile(Target.Device);
                          Target.Profile = ApmConst.AudioProfiles.BAP_GCP;
                    }
                } else {
                    Target.Profile = dpm.getProfile(Target.Device, mAudioType);
                    if(!Target.Device.equals(groupDevice)) {
                        int preferredProfile = dpm.getProfile(groupDevice, mAudioType);
                        if(preferredProfile > ApmConst.AudioProfiles.NONE) {
                            Target.Profile = preferredProfile;
                        }
                    }
                }

                if(Target.Profile == ApmConst.AudioProfiles.BAP_CALL ||
                        Target.Profile == ApmConst.AudioProfiles.BAP_MEDIA ||
                        Target.Profile == ApmConst.AudioProfiles.BAP_RECORDING ||
                        Target.Profile == ApmConst.AudioProfiles.BAP_GCP||
                        Target.Profile == ApmConst.AudioProfiles.BAP_GCP_VBC) {
                    Target.Device = groupDevice;
                }

                Log.w(TAG, "Activating(): Target profile:  " + Target.Profile +
                           ", Current Profile: " + Current.Profile);

                if (Target.Device.equals(Current.Device) &&
                   Target.Profile == Current.Profile) {
                    Log.d(TAG,"Target Device: " + Target.Device +
                              " and Profile: " + Target.Profile +
                                               " already active");
                    transitionTo(mPrevState);
                    updatePending = false;
                    updatedPreviouselyToMM = true;
                    mBroadcastActive = true;
                    Log.d(TAG,"Target absoluteDevice: " + Target.absoluteDevice +
                              " Current absoluteDevice " + Current.absoluteDevice);
                    if (Target.absoluteDevice != null &&
                             !Target.absoluteDevice.equals(Current.absoluteDevice) &&
                             getConnectionState(Current.absoluteDevice, Current.Profile) !=
                             BluetoothProfile.STATE_CONNECTED) {
                        Log.d(TAG, "Need update absolute device from " + Current.absoluteDevice +
                                " to " + Target.absoluteDevice);
                        Current.absoluteDevice = Target.absoluteDevice;
                        updateAbsoluteDevicePending = true;
                    }
                    return;
                }

                if(!ApmConst.getQtiLeAudioEnabled() && mAudioType == AudioType.MEDIA) {
                    // BLE Unicast -> Broadcast
                    if (isLeUnicastToBroadcast(Current.Profile, Target.Profile, mAudioType)) {
                        Log.e(TAG, "Broadcast is activated while unicast active, ignore set unicast inactive");
                        ActivateDevice(Target, mTargetSHO, Current.Profile);
                        return;
                    // BLE Broadcast (Unicast Active Previously) -> A2DP/HAP
                    } else if (Current.Profile == ApmConst.AudioProfiles.BROADCAST_LE &&
                            mPrevActiveUcastDevice != null &&
                            !isSameProfile(mPrevActiveUcastProfile, Target.Profile, mAudioType)) {
                        Log.e(TAG, "Switch active to non-BLE profile from Broadsting");
                        // set broadcast inactive first
                        ret = startSho(null, ApmConst.AudioProfiles.BROADCAST_LE);

                        Log.e(TAG, "Activating(): Set Bcast in-active SHO status, ret: " + ret);
                        if (ret != SHO_SUCCESS) {
                            transitionTo(mPrevState);
                            updatePending = false;
                            return;
                        }
                        Log.e(TAG, "Broadcast is inactive, restore previous LE unicast device and profile");
                        Current.Profile = mPrevActiveUcastProfile;
                        Current.Device = mPrevActiveUcastDevice;
                    }
                }

                Log.w(TAG, "Activating(): Current.Device:  " + Current.Device);

                if(Current.Device == null ||
                   isSameProfile(Current.Profile, Target.Profile, mAudioType)) {
                    /* Single Step SHO*/
                    Log.e(TAG, "Single step SHO");
                    ActivateDevice(Target, mTargetSHO, Current.Profile);
                } else {
                    /*Multi Step SHO*/
                    Log.e(TAG, "Multi step SHO");
                    ret = startSho(null, Current.Profile);

                    Log.e(TAG, "Activating(): First step SHO status, ret: " + ret);
                    if(SHO_PENDING == ret) {
                        sendMessageDelayed(Event.DEACTIVATE_TIMEOUT, DEACTIVATE_TIMEOUT_DELAY);
                    } else if(ret == SHO_FAILED) {
                        mTargetSHO.retryCount = 1;
                        sendMessageDelayed(Event.RETRY_DEACTIVATE, DEACTIVATE_TRY_DELAY);
                    } else if(SHO_SUCCESS == ret) {
                        mPrevState = mIdle;
                        Current.Device = null;
                        Current.absoluteDevice = null;
                        ActivateDevice(Target, mTargetSHO, Current.Profile);
                    }
                }
            }

            @Override
            public void exit() {
                removeMessages(Event.ACTIVATE_TIMEOUT);
                mPrevState = mActivating;
            }

            @Override
            public boolean processMessage(Message message) {
                log("Activating: Process Message (" + mAudioType + "): "
                        + messageWhatToString(message.what));

                switch(message.what) {
                    case Event.SET_ACTIVE:
                        log("New SHO request while handling previous. Add to queue");
                        removeDeferredMessages(Event.REMOVE_DEVICE);
                        removeDeferredMessages(Event.SET_ACTIVE);
                        deferMessage(message);
                        break;

                    case Event.ACTIVE_DEVICE_CHANGE:
                        DeviceProfileCombo mDeviceProfileCombo = (DeviceProfileCombo)message.obj;
                        removeMessages(Event.ACTIVATE_TIMEOUT);
                        if (Target.Profile == ApmConst.AudioProfiles.BAP_GCP
                                    && Target.Profile == mDeviceProfileCombo.Profile) {
                            Current.Device = Target.Device;
                            Current.Profile = Target.Profile;
                            transitionTo(mGamingMode);
                        } else if (Target.Profile == ApmConst.AudioProfiles.BROADCAST_LE
                                    && Target.Profile == mDeviceProfileCombo.Profile) {
                            Current.Device = Target.Device;
                            Current.Profile = Target.Profile;
                            transitionTo(mBroadcasting);
                        } else if(Target.Device != null && Target.Device.equals(mDeviceProfileCombo.Device)) {
                            Current.Device = mDeviceProfileCombo.Device;
                            Current.Profile = Target.Profile;
                            Current.absoluteDevice = Target.absoluteDevice;
                            transitionTo(mActive);
                        }
                        break;

                    case Event.REMOVE_DEVICE:
                        removeDeferredMessages(Event.REMOVE_DEVICE);
                        deferMessage(message);
                        break;

                    case Event.DEVICE_REMOVED:
                        mPrevState = mIdle;
                        Current.Device = null;
                        removeMessages(Event.DEACTIVATE_TIMEOUT);
                        ActivateDevice(Target, mTargetSHO, Current.Profile);
                        break;

                    case Event.RETRY_DEACTIVATE:
                        ret = startSho(null, Current.Profile);
                        Log.d(TAG, "Activating(): RETRY_DEACTIVATE, sho status ret: " + ret);
                        if(SHO_PENDING == ret) {
                            mTargetSHO.retryCount = 0;
                            sendMessageDelayed(Event.DEACTIVATE_TIMEOUT, DEACTIVATE_TIMEOUT_DELAY);
                        } else if(ret == SHO_FAILED) {
                            if(mTargetSHO.retryCount >= RETRY_DEACTIVATE_LIMIT) {
                                updatePending = false;
                                transitionTo(mPrevState);
                            } else {
                                mTargetSHO.retryCount++;
                                sendMessageDelayed(Event.RETRY_DEACTIVATE, DEACTIVATE_TRY_DELAY);
                            }
                        } else if(SHO_SUCCESS == ret) {
                            mTargetSHO.retryCount = 0;
                            mPrevState = mIdle;
                            Current.Device = null;
                            ActivateDevice(Target, mTargetSHO, Current.Profile);
                        }
                        break;

                    case Event.ACTIVATE_TIMEOUT: {
                        CallAudio mCallAudio = CallAudio.get();
                        if (mCallAudio != null && mCallAudio.isVoipLeaWarEnabled() &&
                            (Target.Profile == ApmConst.AudioProfiles.TMAP_MEDIA ||
                             Target.Profile == ApmConst.AudioProfiles.BAP_MEDIA ||
                             Target.Profile == ApmConst.AudioProfiles.BAP_RECORDING ||
                             Target.Profile == ApmConst.AudioProfiles.TMAP_CALL ||
                             Target.Profile == ApmConst.AudioProfiles.BAP_CALL ||
                             Target.Profile == ApmConst.AudioProfiles.BAP_GCP ||
                             Target.Profile == ApmConst.AudioProfiles.BAP_GCP_VBC)) {
                            Log.d(TAG, "Back to previous active device since ACTIVATE_TIMEOUT");
                            StreamAudioService mStreamAudioService =
                                               StreamAudioService.getStreamAudioService();
                            if (mStreamAudioService != null)
                                mStreamAudioService.setActiveDevice(Current.Device, Target.Profile, false);
                        }
                        transitionTo(mPrevState);
                        break;
                    }
                    case Event.DEACTIVATE_TIMEOUT:
                        transitionTo(mPrevState);
                        break;
                    case Event.RETRY_DISABLE_RECORDING:
                        Log.d(TAG, "RETRY_DISABLE_RECORDING");
                        ret = startSho(mTargetSHO.device,ApmConst.AudioProfiles.BAP_MEDIA);

                        Log.d(TAG, "Activating(): RETRY_DISABLE_RECORDING, sho status ret: " + ret);
                        if (ret == SHO_FAILED) {
                          if(mTargetSHO.retryCount >= RETRY_LIMIT) {
                            transitionTo(mPrevState);
                          } else {
                            mTargetSHO.retryCount++;
                            sendMessageDelayed(Event.RETRY_DISABLE_RECORDING, DEACTIVATE_RECORDING_TRY_DELAY);
                          }
                        } else if (ret == SHO_SUCCESS){
                            Current.Device = Target.Device;
                            Current.absoluteDevice = Target.absoluteDevice;
                            Current.Profile = Target.Profile;
                            transitionTo(mActive);
                        }
                        break;
                    case Event.RETRY_ACTIVATE:
                        Log.d(TAG, "RETRY_ACTIVATE, audioType=" + mAudioType);
                        CallAudio mCallAudio = CallAudio.get();
                        boolean isInCall =
                            mCallAudio != null && mCallAudio.isVoiceOrCallActive();
                        if (isInCall && mAudioType != AudioType.VOICE) {
                            Log.d(TAG,"call active ignore mode switch retry");
                            updatePending = false;
                            transitionTo(mPrevState);
                            break;
                        }
                        ret = startSho(Target.Device, Target.Profile);
                        if (ret == SHO_FAILED) {
                            Log.d(TAG, "startSho returns SHO_FAILED for " + mTargetSHO.retryCount +
                                                       "th time");
                            if (mTargetSHO.retryCount >= RETRY_LIMIT) {
                                transitionTo(mPrevState);
                                break;
                            } else {
                                mTargetSHO.retryCount++;
                                sendMessageDelayed(Event.RETRY_ACTIVATE, ACTIVATE_RETRY_DELAY);
                                break;
                            }
                        } else if (ret == SHO_SUCCESS) {
                            Log.d(TAG, "RETRY_ACTIVATE_SUCESS,target profile: " + Target.Profile);
                            Current.Device = Target.Device;
                            Current.absoluteDevice = Target.absoluteDevice;
                            Current.Profile = Target.Profile;

                            if (mTargetSHO.isBroadcast) {
                                transitionTo(mBroadcasting);
                            } else if (mTargetSHO.isGamingMode) {
                                transitionTo(mGamingMode);
                            } else if (mTargetSHO.isRecordingMode) {
                                transitionTo(mRecordingMode);
                            } else {
                                if (ApmConst.getAospLeaEnabled()) {
                                    sendActiveDeviceUpdate(mAudioType, Current);
                                    updatePending = false;
                                }
                                transitionTo(mActive);
                            }
                        }
                        break;
                    case Event.STOP_SM:
                        deferMessage(message);
                        break;

                    default:
                        return NOT_HANDLED;
                }
                return HANDLED;
            }

            void ActivateDevice(DeviceProfileCombo mTarget, SHOReq mTargetSHO, int currentProfile) {
                CallAudio mCallAudio = CallAudio.get();
                boolean isInCall =
                       mCallAudio != null && mCallAudio.isVoiceOrCallActive();
                Log.d(TAG, "ActivateDevice(): Target Device: " + mTarget.Device +
                           ", Current.Device: " + Current.Device +
                           ", profile: " + currentProfile + ", isInCall: " + isInCall);
                if (ApmConst.getAospLeaEnabled() && isInCall &&
                    Current.Device != null && Current.Device.equals(mTarget.Device) &&
                    mTarget.Profile == ApmConst.AudioProfiles.BAP_MEDIA &&
                    (currentProfile == ApmConst.AudioProfiles.BAP_GCP ||
                     currentProfile == ApmConst.AudioProfiles.BAP_RECORDING ||
                     currentProfile == ApmConst.AudioProfiles.BAP_GCP_VBC)) {
                    // This would ensure shostatemachine should move to proper state.
                    Log.d(TAG, " Fake success as updateMetadata would take care from MM ");
                    StreamAudioService streamAudioService = StreamAudioService.getStreamAudioService();
                    streamAudioService.updateDeviceProfileType(Current.Device, mTarget.Profile);
                    ret = SHO_SUCCESS;
                } else {
                    ret = startSho(mTarget.Device, mTarget.Profile);
                }

                Log.d(TAG, "ActivateDevice(): SHO status ret: " + ret);
                if(SHO_PENDING == ret) {
                    if (ApmConst.getQtiLeAudioEnabled()) {
                        Current.Device = mTarget.Device;
                        Current.absoluteDevice = mTarget.absoluteDevice;
                        Current.Profile = mTarget.Profile;
                    }
                    if(mAudioType == AudioType.MEDIA) {
                        if(ApmConst.getQtiLeAudioEnabled()) {
                            sendActiveDeviceMediaUpdate(Current);
                        } else {
                            Log.d(TAG,"Dont send sendActiveDeviceUpdate when sho is pending");
                        }
                    }
                    sendMessageDelayed(Event.ACTIVATE_TIMEOUT, ACTIVATE_TIMEOUT_DELAY);
                } else if(ret == SHO_FAILED) {
                    if (mState == mBroadcasting) {
                        mTargetSHO.forceStopAudio = true;
                        Log.d(TAG,"Previous state was broadcasting, moving to idle");
                    }
                    if (mPrevState == mRecordingMode &&
                      Current.Device != null && Current.Device.equals(mTarget.Device)) {
                      sendMessageDelayed(Event.RETRY_DISABLE_RECORDING, DEACTIVATE_RECORDING_TRY_DELAY);
                    } else if (Current.Device != null && Current.Device.equals(mTarget.Device) &&
                        (mTarget.Profile == ApmConst.AudioProfiles.BAP_GCP ||
                        mTarget.Profile == ApmConst.AudioProfiles.BAP_GCP_VBC ||
                        mTarget.Profile == ApmConst.AudioProfiles.BAP_RECORDING)) {
                        sendMessageDelayed(Event.RETRY_ACTIVATE, ACTIVATE_RETRY_DELAY);
                    } else {
                      Log.d(TAG,"SHO Failed for: " + mTarget.Device + " and "
                                          + mTarget.Profile + ", Retrying..");
                      sendMessageDelayed(Event.RETRY_ACTIVATE, ACTIVATE_RETRY_DELAY);
                    }
                } else if(SHO_SUCCESS == ret) {
                    Current.Device = mTarget.Device;
                    Current.absoluteDevice = mTarget.absoluteDevice;
                    Current.Profile = mTarget.Profile;
                    if(mTargetSHO.isBroadcast) {
                        transitionTo(mBroadcasting);
                    } else if (mTargetSHO.isGamingMode) {
                        transitionTo(mGamingMode);
                    } else if (mTargetSHO.isRecordingMode) {
                        transitionTo(mRecordingMode);
                    } else {
                        if(ApmConst.getAospLeaEnabled()) {
                          sendActiveDeviceUpdate(mAudioType, Current);
                          updatePending = false;
                        }
                        transitionTo(mActive);
                    }
                } else if(ALREADY_ACTIVE == ret) {
                    transitionTo(mActive);
                }
            }
        }

        class Deactivating extends State {
            int ret;
            private State mNextState = null;
            @Override
            public void enter() {
                synchronized (this) {
                    Log.d(TAG, "Enter Deactivating(): mNextState: " + mNextState);
                    mNextState = null;
                    mState = mDeactivating;
                    if (mPrevState == mBroadcasting) {
                        mPrevState = mIdle;
                    }
                    Target.Device = null;
                    Target.Profile = Current.Profile;
                    mTargetSHO.copy(mSHOQueue);
                    if (!mSHOQueue.isQueueUpdated) {
                        mSHOQueue.reset();
                    }
                    else {
                        Log.d(TAG, "mSHOQueue is updated, dont reset");
                    }
                }
                ret = startSho(Target.Device, Target.Profile);
                Log.d(TAG, "Deactivating(): ret: " + ret);
                if (SHO_SUCCESS == ret) {
                    McpService mMcpService = McpService.getMcpService();
                    if (mMcpService != null) {
                        mMcpService.SetActiveDevices(Target.Device, ApmConst.AudioProfiles.MCP);
                        Log.d(TAG, "Deactivating: SetActiveDevices called to MCP," +
                                   " Target.Device: " + Target.Device);
                    }
                    mNextState = mIdle;
                    transitionTo(mIdle);
                } else if (SHO_PENDING == ret) {
                    sendMessageDelayed(Event.DEACTIVATE_TIMEOUT, DEACTIVATE_TIMEOUT_DELAY);
                } else {
                    mTargetSHO.retryCount = 1;
                    sendMessageDelayed(Event.RETRY_DEACTIVATE, DEACTIVATE_TRY_DELAY);
                }
            }

            @Override
            public void exit() {
                removeMessages(Event.DEACTIVATE_TIMEOUT);
                mPrevState = mDeactivating;
                if (!ApmConst.getQtiLeAudioEnabled() &&
                        mPrevStableState == mBroadcasting) {
                    Log.d(TAG, "Set previous active unicast device and profile while Deactivating from Broadcast");
                    mPrevActiveDevice = mPrevActiveUcastDevice;
                    mPrevActiveProfile = mPrevActiveUcastProfile;
                } else {
                    mPrevActiveDevice = Current.Device;
                    mPrevActiveProfile = Current.Profile;
                }
                //When next is idle then only make it as null.
                if (mNextState == mIdle) {
                  Log.d(TAG, "Exit Deactivating: NextState is idle. " );
                  Current.absoluteDevice = null;
                  Current.Device = null;
                  Current.Profile = ApmConst.AudioProfiles.NONE; // Add profile value here
                }
            }

            @Override
            public boolean processMessage(Message message) {
                log("Deactivating: Process Message (" + mAudioType + "): "
                        + messageWhatToString(message.what));

                switch(message.what) {
                    case Event.SET_ACTIVE:
                        log("New SHO request while handling previous. Add to queue");
                        removeDeferredMessages(Event.SET_ACTIVE);
                        deferMessage(message);
                        break;

                    case Event.ACTIVE_DEVICE_CHANGE:
                        break;

                    case Event.REMOVE_DEVICE:
                        break;

                    case Event.DEVICE_REMOVED:
                        removeMessages(Event.DEACTIVATE_TIMEOUT);
                        mNextState = mIdle;
                        transitionTo(mIdle);
                        break;

                    case Event.RETRY_DEACTIVATE:
                        Log.d(TAG, "Deactivating(): Current.Profile: " + Current.Profile);
                        ret = startSho(null, Current.Profile);

                        Log.d(TAG, "Deactivating(): RETRY_DEACTIVATE, sho status ret: " + ret);
                        if (SHO_PENDING == ret) {
                            mTargetSHO.retryCount = 0;
                            sendMessageDelayed(Event.DEACTIVATE_TIMEOUT, DEACTIVATE_TIMEOUT_DELAY);
                        } else if (ret == SHO_FAILED) {
                            Log.d(TAG, "Deactivating(): RETRY_DEACTIVATE, mTargetSHO.retryCount: "
                                                                    + mTargetSHO.retryCount);
                            if(mTargetSHO.retryCount >= RETRY_DEACTIVATE_LIMIT) {
                                updatePending = false;
                                mNextState = mPrevState;
                                transitionTo(mPrevState);
                            } else {
                                mTargetSHO.retryCount++;
                                sendMessageDelayed(Event.RETRY_DEACTIVATE, DEACTIVATE_TRY_DELAY);
                            }
                        } else if(SHO_SUCCESS == ret) {
                            mTargetSHO.retryCount = 0;
                            mNextState = mIdle;
                            transitionTo(mIdle);
                        }
                        break;

                    case Event.DEACTIVATE_TIMEOUT:
                        transitionTo(mPrevState);

                    case Event.STOP_SM:
                        deferMessage(message);
                        break;

                    default:
                        return NOT_HANDLED;
                }
                return HANDLED;
            }
        }

        void sendActiveDeviceUpdate (int mAudioType, DeviceProfileCombo Current) {
            Log.d(TAG, "sendActiveDeviceUpdate(): mAudioType: " + mAudioType +
                                                ", Current.Profile: " + Current.Profile +
                                                ", Current.Device: " + Current.Device);
            DeviceProfileMap dpm = DeviceProfileMap.getDeviceProfileMapInstance();
            if (dpm == null) {
                Log.w(TAG, "sendActiveDeviceUpdate: dpm is null, return.");
                return;
            }
            int MediaProfID =
               dpm.getSupportedProfile(Current.Device, ApmConst.AudioFeatures.MEDIA_AUDIO);
            int VoiceProfID =
               dpm.getSupportedProfile(Current.Device, ApmConst.AudioFeatures.CALL_AUDIO);
            Log.d(TAG, "sendActiveDeviceUpdate: "+ " MediaProfID:" + MediaProfID +
                       ", VoiceProfID:" + VoiceProfID);
            switch (mAudioType) {
                case AudioType.MEDIA:
                    if(Current.Profile == ApmConst.AudioProfiles.A2DP ||
                            Current.Profile == ApmConst.AudioProfiles.HAP_BREDR) {
                        sendActiveDeviceMediaUpdate(Current);
                    } else {
                        int activeProfile = getActiveProfile(AudioType.VOICE);
                        BluetoothDevice device = getActiveDevice(AudioType.VOICE);
                        Log.d(TAG, "sendActiveDeviceUpdate(): activeProfile: " + activeProfile +
                                   ", device: " + device + ", mIsAdmPtsEnabled: " + mIsAdmPtsEnabled);
                        if((((ApmConst.AudioProfiles.BAP_CALL & VoiceProfID) !=
                             ApmConst.AudioProfiles.BAP_CALL) || mIsAdmPtsEnabled) ||
                             (ApmConst.AudioProfiles.BAP_CALL == activeProfile &&
                              Current.Device.equals(device))) {
                            if (mPrevStableState == mBroadcasting &&
                                    Current.Device.equals(mPrevActiveUcastDevice)) {
                                Log.d(TAG, "sendActiveDeviceUpdate(): Back to previous " +
                                           "unicast active state from Broadcasting");
                            } else {
                                sendActiveDeviceLeUpdate(Current);
                            }
                        }
                    }
                    break;
                case AudioType.VOICE:
                    if(Current.Profile == ApmConst.AudioProfiles.HFP ||
                            Current.Profile == ApmConst.AudioProfiles.HAP_BREDR) {
                        sendActiveDeviceVoiceUpdate(Current);
                    } else {
                        int activeProfile = getActiveProfile(AudioType.MEDIA);
                        BluetoothDevice device = getActiveDevice(AudioType.MEDIA);
                        Log.d(TAG, "sendActiveDeviceUpdate(): activeProfile: " + activeProfile +
                                   ", device: " + device + ", mIsAdmPtsEnabled: " + mIsAdmPtsEnabled);
                        if((((ApmConst.AudioProfiles.BAP_MEDIA & MediaProfID) !=
                              ApmConst.AudioProfiles.BAP_MEDIA) || mIsAdmPtsEnabled) ||
                           (ApmConst.AudioProfiles.BAP_MEDIA == activeProfile &&
                            Current.Device.equals(device)) ||
                           (ApmConst.AudioProfiles.BROADCAST_LE == activeProfile &&
                            ApmConst.AudioProfiles.BAP_CALL == Current.Profile)) {
                             sendActiveDeviceLeUpdate(Current);
                        }
                    }
                    break;
            }
        }

        void sendActiveAbsoluteDeviceUpdate (int mAudioType, DeviceProfileCombo Current) {
            Log.d(TAG, "sendActiveAbsoluteDeviceUpdate(): mAudioType: " + mAudioType +
                                                ", Current.Profile: " + Current.Profile +
                                                ", Current.absoluteDevice: " + Current.absoluteDevice);

            CCService ccService = CCService.getCCService();
            ActiveDeviceManager mDeviceManager =
                       AdapterService.getAdapterService().getActiveDeviceManager();
            if (ApmConst.getQtiLeAudioEnabled()) {
                if (mAudioType == AudioType.MEDIA &&
                        Current.Profile == ApmConst.AudioProfiles.BAP_MEDIA) {
                    broadcastActiveDeviceChange (Current.absoluteDevice, AudioType.MEDIA);
                } else if (mAudioType == AudioType.VOICE &&
                        Current.Profile == ApmConst.AudioProfiles.BAP_CALL) {
                    broadcastActiveDeviceChange (Current.absoluteDevice, AudioType.VOICE);
                    if (ccService != null) {
                        ccService.setActiveDevice(Current.absoluteDevice);
                    }
                }
            } else if (ApmConst.getAospLeaEnabled()) {
                if (mAudioType == AudioType.MEDIA &&
                        Current.Profile == ApmConst.AudioProfiles.BAP_MEDIA) {
                    int activeProfile = getActiveProfile(AudioType.VOICE);
                    BluetoothDevice activeAbsoluteDevice = getActiveAbsoluteDevice(AudioType.VOICE);
                    Log.d(TAG, "sendActiveAbsoluteDeviceUpdate(): activeProfile: " + activeProfile +
                                            ", activeAbsolutedevice: " + activeAbsoluteDevice);
                    if(ApmConst.AudioProfiles.BAP_CALL == activeProfile &&
                            Current.absoluteDevice.equals(activeAbsoluteDevice)) {
                        broadcastLeActiveDeviceChange(Current.absoluteDevice);
                        mDeviceManager.onLeActiveDeviceChange(Current.absoluteDevice);
                        if (ccService != null) {
                            ccService.setActiveDevice(Current.absoluteDevice);
                        }
                    }
                } else if (mAudioType == AudioType.VOICE &&
                        Current.Profile == ApmConst.AudioProfiles.BAP_CALL) {
                    int activeProfile = getActiveProfile(AudioType.MEDIA);
                    BluetoothDevice activeAbsoluteDevice = getActiveAbsoluteDevice(AudioType.MEDIA);
                    CallAudio mCallAudio = CallAudio.get();
                    if(mCallAudio != null && mCallAudio.isVoipLeaWarEnabled()) {
                        broadcastActiveDeviceChange (Current.absoluteDevice, AudioType.VOICE);
                    }
                    Log.d(TAG, "sendActiveAbsoluteDeviceUpdate(): activeProfile: " + activeProfile +
                                            ", activeAbsolutedevice: " + activeAbsoluteDevice);
                    if(ApmConst.AudioProfiles.BAP_MEDIA == activeProfile &&
                            Current.absoluteDevice.equals(activeAbsoluteDevice)) {
                        broadcastLeActiveDeviceChange(Current.absoluteDevice);
                        mDeviceManager.onLeActiveDeviceChange(Current.absoluteDevice);
                        if (ccService != null) {
                            ccService.setActiveDevice(Current.absoluteDevice);
                        }
                    }
                }
            }
            updateAbsoluteDevicePending = false;
        }

        class Active extends State {
            int ret;
            @Override
            public void enter() {
                synchronized (this) {
                    mMsgProcessing = Event.NONE;
                    mState = mActive;
                }
                Log.d(TAG, "Active(): mAudioType: " + mAudioType +
                           ", Current.Profile: " + Current.Profile +
                           ", Current.Device: " + Current.Device +
                           ", updatePending: " + updatePending +
                           ", updateAbsoluteDevicePending: " + updateAbsoluteDevicePending);
                if(updatePending) {
                    if(mAudioType == AudioType.MEDIA) {
                        if(ApmConst.getQtiLeAudioEnabled()) {
                            sendActiveDeviceMediaUpdate(Current);
                        } else {
                            sendActiveDeviceUpdate(mAudioType, Current);
                        }
                    }
                    else if(mAudioType == AudioType.VOICE) {
                        if(ApmConst.getQtiLeAudioEnabled()) {
                            sendActiveDeviceVoiceUpdate(Current);
                        } else {
                            sendActiveDeviceUpdate(mAudioType, Current);
                        }
                    }
                } else if (updateAbsoluteDevicePending) {
                    sendActiveAbsoluteDeviceUpdate(mAudioType, Current);
                } else if (mBroadcastActive){
                    mBroadcastActive = false;
                    checkAndBroadcastActiveDevice(Current.Device, Current.Profile);
                }

                if(txStreamSuspended && mAudioType == AudioType.MEDIA) {
                    if (mSHOQueue.device != null && mSHOQueue.isRecordingMode) {
                        Log.w(TAG, "enableRecording request is pending in SHO queue, keep txStreamSuspended");
                    } else {
                        Log.d(TAG, "Set A2dpSuspended=false");
                        mAudioManager.setParameters("A2dpSuspended=false");
                        txStreamSuspended = false;
                    }
                } else if (sm[mAudioType].mWaitForLock && (mAudioType == AudioType.VOICE ||
                           (ApmConst.getAospLeaEnabled() &&
                            mAudioType == AudioType.MEDIA))) {
                    lock.lock();
                        sm[mAudioType].mWaitForLock = false;
                    voiceHandoffComplete.signal();
                    lock.unlock();
                    Log.d(TAG, "Voice Active: unlock by signal");
                }
            }

            @Override
            public void exit() {
            //2 update dependent profiles
                mPrevState = mActive;
                mPrevStableState = mActive;
                mPrevActiveDevice = Current.Device;
                mPrevActiveProfile = Current.Profile;
                if (Current.Profile == ApmConst.AudioProfiles.BAP_MEDIA ||
                        Current.Profile == ApmConst.AudioProfiles.TMAP_MEDIA ||
                        Current.Profile == ApmConst.AudioProfiles.BAP_CALL ||
                        Current.Profile == ApmConst.AudioProfiles.TMAP_CALL) {
                    mPrevActiveUcastDevice = Current.Device;
                    mPrevActiveUcastProfile = Current.Profile;
                } else {
                    mPrevActiveUcastDevice = null;
                    mPrevActiveUcastProfile = ApmConst.AudioProfiles.NONE;
                }
                VolumeManager mVolumeManager = VolumeManager.get();
                if(mVolumeManager != null) {
                    mVolumeManager.saveVolume(mAudioType);
                    if (ApmConst.getAospLeaEnabled()) {
                        if (getActiveProfile(AudioType.MEDIA) ==
                                ApmConst.AudioProfiles.BROADCAST_LE) {
                            Log.d(TAG, "Saving media volume also while broadcasting");
                            mVolumeManager.saveVolume(AudioType.MEDIA);
                        }
                    }
                }
            }

            @Override
            public boolean processMessage(Message message) {
                log("Active: Process Message (" + mAudioType + "): "
                        + messageWhatToString(message.what));
                mMsgProcessing = message.what;
                switch(message.what) {
                    case Event.SET_ACTIVE:
                        if(mSHOQueue.device == null) {
                            Log.w(TAG, "Invalid request");
                            break;
                        }
                        transitionTo(mActivating);
                        break;

                    case Event.ACTIVE_DEVICE_CHANGE:
                        // might have to handle
                        break;

                    case Event.REMOVE_DEVICE:
                        transitionTo(mDeactivating);
                        break;

                    case Event.DEVICE_REMOVED:
                        //might have to handle
                        transitionTo(mIdle);
                        break;

                    case Event.STOP_SM:
                        transitionTo(mDeactivating);
                        enabled = false;
                        break;

                    default:
                        return NOT_HANDLED;
                }
                return HANDLED;
            }
        }

        class Gaming extends State {
            int ret;
            @Override
            public void enter() {
                synchronized (this) {
                    mMsgProcessing = Event.NONE;
                    mState = mGamingMode;
                }
                if(updatePending) {
                   Log.e(TAG, ": Gaming Enter(): profile: " + Current.Profile +
                              ", mIsGcpVbcConnectionUpdated: " + mIsGcpVbcConnectionUpdated);
                   if (Current.Profile == ApmConst.AudioProfiles.BAP_GCP_VBC &&
                       !mIsGcpVbcConnectionUpdated) {
                     mIsGcpVbcConnectionUpdated = true;
                   }
                   sendActiveDeviceGamingUpdate(Current, false);
                } else if (updateAbsoluteDevicePending) {
                    sendActiveAbsoluteDeviceUpdate(mAudioType, Current);
                }
                if(txStreamSuspended) {
                    mAudioManager.setParameters("A2dpSuspended=false");
                    txStreamSuspended = false;
                }
                if (mPrevState == mActivating && mWaitForLock) {
                    lock.lock();
                    voiceHandoffComplete.signal();
                    mWaitForLock = false;
                    lock.unlock();
                    Log.d(TAG, "Gaming Active: unlock by signal");
                }
            }

            @Override
            public void exit() {
            //2 update dependent profiles
                mPrevState = mGamingMode;
                mPrevStableState = mGamingMode;
                mPrevActiveDevice = Current.Device;
                mPrevActiveProfile = Current.Profile;
                mPrevActiveUcastDevice = Current.Device;
                mPrevActiveUcastProfile = Current.Profile;
                VolumeManager mVolumeManager = VolumeManager.get();
                if(mVolumeManager != null) {
                    mVolumeManager.saveVolume(mAudioType);
                }

                //VBC disconnection update to MM.
                Log.e(TAG, ": Gaming Exit(): for device: " + Current.Device +
                           ", mIsGcpVbcConnectionUpdated: " + mIsGcpVbcConnectionUpdated);

                MediaAudio mMediaAudio = MediaAudio.get();
                if (mMediaAudio != null) {
                    Log.d(TAG, "reset mGamingMode in MediaAudio");
                    mMediaAudio.setGamingMode(0);
                }

                if (mAudioManager != null && mIsGcpVbcConnectionUpdated) {
                    /*if (!ApmConst.getQtiLeAudioEnabled()) {
                        mAudioManager.handleBluetoothActiveDeviceChanged(
                                Current.Device, mPrevActiveDevice,
                                BluetoothProfileConnectionInfo.createLeAudioInfo(true, false));
                    } else */
                    if (ApmConst.getQtiLeAudioEnabled()) {
                        mAudioManager.handleBluetoothActiveDeviceChanged(
                               null, mPrevActiveDevice,
                               BluetoothProfileConnectionInfo.createA2dpSinkInfo(-1));
                    }
                    //After update reset the flag.
                    mIsGcpVbcConnectionUpdated = false;
                }
            }

            @Override
            public boolean processMessage(Message message) {
                log("Gaming: Process Message (" + mAudioType + "): "
                        + messageWhatToString(message.what));
                mMsgProcessing = message.what;
                switch(message.what) {
                    case Event.SET_ACTIVE:
                        if(mSHOQueue.device == null) {
                            Log.w(TAG, "Invalid request");
                            break;
                        }

                        Log.w(TAG, "isGamingVbcMode: " + mSHOQueue.isGamingVbcMode +
                                   ", mIsGcpVbcConnectionUpdated: " + mIsGcpVbcConnectionUpdated);
                        if (!Current.absoluteDevice.equals(mSHOQueue.device)) {
                            LeAudioService mLeAudioService = LeAudioService.getLeAudioService();
                            StreamAudioService streamAudioService = StreamAudioService.getStreamAudioService();
                            if (mLeAudioService != null && streamAudioService != null) {
                                BluetoothDevice groupDevice = streamAudioService.getDeviceGroup(mSHOQueue.device);
                                int shoGroupId = mLeAudioService.getGroupId(mSHOQueue.device);
                                if (shoGroupId < INVALID_SET_ID && Current.Device.equals(groupDevice)) {
                                    Log.d(TAG, "Same group made active");
                                    mSHOQueue.isGamingMode = true;
                                    mSHOQueue.isGamingVbcMode =
                                       (Current.Profile == ApmConst.AudioProfiles.BAP_GCP_VBC);
                                }
                            }
                        }
                        if (Current.Device.equals(mSHOQueue.device) &&
                            mSHOQueue.isGamingVbcMode) {
                            if (mIsGcpVbcConnectionUpdated) {
                                Log.w(TAG, "Already Updated VBC connection");
                                break;
                            }

                            int ret_val;
                            ret_val = startSho(Current.Device, ApmConst.AudioProfiles.BAP_GCP_VBC);
                            Log.d(TAG, "Gaming(): sho to vbc ret_val: " + ret_val);

                            if (ret_val == SHO_SUCCESS && mAudioManager != null) {
                                Current.Profile = ApmConst.AudioProfiles.BAP_GCP_VBC;
                                sendActiveDeviceGamingUpdate(Current, true);
                                mIsGcpVbcConnectionUpdated = true;
                                break;
                            } else {
                              Log.w(TAG, "SHO failed for VBC profile, return");
                              break;
                            }
                        } else {
                            if(!mSHOQueue.isUIReq && Current.Device.equals(mSHOQueue.device)) {
                                Log.w(TAG, "Spurious request for same device. Ignore");
                                mSHOQueue.reset();
                                lock.lock();
                                voiceHandoffComplete.signal();
                                mWaitForLock = false;
                                lock.unlock();
                                Log.d(TAG, "unlock by signal");
                                break;
                            }
                        }

                        /*MediaAudio mMediaAudio = MediaAudio.get();
                        if(mMediaAudio != null && mMediaAudio.isA2dpPlaying(mSHOQueue.device)) {
                            if(!(mSHOQueue.isBroadcast || mSHOQueue.isRecordingMode)) {
                                Log.w(TAG, "Gaming streaming is on");
                                break;
                            }
                        }*/
                        transitionTo(mActivating);
                        break;

                    case Event.ACTIVE_DEVICE_CHANGE:
                        // might have to handle
                        break;

                    case Event.REMOVE_DEVICE:
                        transitionTo(mDeactivating);
                        break;

                    case Event.DEVICE_REMOVED:
                        //might have to handle
                        break;

                    case Event.STOP_SM:
                        transitionTo(mDeactivating);
                        enabled = false;
                        break;

                    default:
                        return NOT_HANDLED;
                }
                return HANDLED;
            }
        }

        class Broadcasting extends State {
            int ret;
            @Override
            public void enter() {
                synchronized (this) {
                    mMsgProcessing = Event.NONE;
                    mState = mBroadcasting;
                }

                if (updatePending) {
                    if (!ApmConst.getQtiLeAudioEnabled()) {
                        VolumeManager mVolumeManager = VolumeManager.get();
                        int deviceVolume = 7;
                        if(mVolumeManager != null && mPrevActiveDevice != null) {
                            deviceVolume = mVolumeManager.getSavedVolume(mPrevActiveDevice, ApmConst.AudioFeatures.MEDIA_AUDIO);
                            Log.d(TAG, "get saved device volume: " + deviceVolume);
                        }
                        if (mPrevActiveDevice != null &&
                                mPrevActiveUcastDevice == null) {
                            Log.d(TAG,"Update A2dp device inactive to MM Audio");
                            broadcastActiveDeviceChange(null, AudioType.MEDIA);
                            mAudioManager.handleBluetoothActiveDeviceChanged(null, mPrevActiveDevice,
                                    BluetoothProfileConnectionInfo.createA2dpInfo(true, -1));
                            if(mVolumeManager != null) {
                                mVolumeManager.onActiveDeviceChange(null,
                                                ApmConst.AudioFeatures.MEDIA_AUDIO);
                            }
                        }
                        Log.d(TAG,"Update broadcast device active to MM Audio");
                        mAudioManager.handleBluetoothActiveDeviceChanged(
                                Current.Device, null, getBroadcastProfile(true, deviceVolume));
                                //BluetoothProfileConnectionInfo.createLeAudioBroadcastInfo(true));
                    } else {
                        apmNative.activeDeviceUpdate(Current.Device, Current.Profile, mAudioType);
                        broadcastActiveDeviceChange(Current.Device, AudioType.MEDIA);
                        mAudioManager.setDeviceVolumeBehavior(new AudioDeviceAttributes(
                        AudioDeviceAttributes.ROLE_OUTPUT, AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                        Current.Device.getAddress()), AudioManager.DEVICE_VOLUME_BEHAVIOR_VARIABLE);
                        // Update active device to null in VolumeManager while enter broadcasting state
                        VolumeManager mVolumeManager = VolumeManager.get();
                        if(mVolumeManager != null) {
                            mVolumeManager.onActiveDeviceChange(null,
                                                                ApmConst.AudioFeatures.MEDIA_AUDIO);
                        }
                        int rememberedVolume = 15;

                        mAudioManager.handleBluetoothActiveDeviceChanged(
                                Current.Device, mPrevActiveDevice,
                                BluetoothProfileConnectionInfo.createA2dpInfo(true, rememberedVolume));
                    }
                    updatePending = false;
                }

                if (mBroadcastStrategy) {
                    List<AudioProductStrategy> audioProductStrategies =
                                         AudioProductStrategy.getAudioProductStrategies();
                    Log.d(TAG,"audioProductStrategies size: " + audioProductStrategies.size());
                    for (final AudioProductStrategy aps : audioProductStrategies) {
                        if (aps.getAudioAttributesForLegacyStreamType(AudioSystem.STREAM_NOTIFICATION) !=  null) {
                            strategy_notification = aps;
                        } else if(aps.getAudioAttributesForLegacyStreamType(AudioSystem.STREAM_RING) !=  null) {
                            strategy_ring = aps;
                        }
                    }
                    if (strategy_notification != null && strategy_ring != null) {
                        AudioDeviceAttributes device = new AudioDeviceAttributes(AudioDeviceAttributes.ROLE_OUTPUT,
                                                                                AudioSystem.DEVICE_OUT_SPEAKER, "");
                        mAudioManager.setPreferredDeviceForStrategy(strategy_notification, device);
                        mAudioManager.setPreferredDeviceForStrategy(strategy_ring, device);
                    } else {
                        Log.d(TAG,"getAudioProductStrategy returned null");
                    }
                }
                if(txStreamSuspended) {
                    mAudioManager.setParameters("A2dpSuspended=false");
                    txStreamSuspended = false;
                }
            }

            @Override
            public void exit() {
                mPrevState = mBroadcasting;
                mPrevStableState = mBroadcasting;
                mPrevActiveDevice = Current.Device;
                mPrevActiveProfile = Current.Profile;
                if (mBroadcastStrategy && strategy_notification != null && strategy_ring != null) {
                    mAudioManager.removePreferredDeviceForStrategy(strategy_notification);
                    mAudioManager.removePreferredDeviceForStrategy(strategy_ring);
                }
            }

            @Override
            public boolean processMessage(Message message) {
                log("Broadcasting: Process Message (" + mAudioType + "): "
                        + messageWhatToString(message.what));
                mMsgProcessing = message.what;
                switch(message.what) {
                    case Event.SET_ACTIVE:
                        if (ApmConstIntf.getQtiLeAudioEnabled()) {
                            if(mSHOQueue.isUIReq)
                                transitionTo(mActivating);
                        } else if (ApmConst.getAospLeaEnabled()) {
                            dpm = DeviceProfileMap.getDeviceProfileMapInstance();
                            int targetProfile = dpm.getProfile(mSHOQueue.device, mAudioType);
                            Log.d(TAG, "Target Profile: " + targetProfile);
                            boolean sendUpdate = false;
                            if (targetProfile == ApmConst.AudioProfiles.BAP_MEDIA ||
                                    targetProfile == ApmConst.AudioProfiles.TMAP_MEDIA) {
                                Log.d(TAG, "mSHOQueue.device: " + mSHOQueue.device +
                                      ", isGamingVbcMode: " + mSHOQueue.isGamingVbcMode +
                                      ", isRecordingMode: " + mSHOQueue.isRecordingMode +
                                      ", mPrevActiveUcastProfile: " + mPrevActiveUcastProfile +
                                      ", mPrevActiveUcastDevice: " + mPrevActiveUcastDevice);
                                StreamAudioService streamAudioService =
                                                   StreamAudioService.getStreamAudioService();
                                BluetoothDevice btDevice = null;
                                if (mSHOQueue != null) {
                                    btDevice = streamAudioService.getDeviceGroup(mSHOQueue.device);
                                }
                                Log.d(TAG, "dev: " + btDevice);
                                if (btDevice != null && !mSHOQueue.isGamingMode &&
                                        !mSHOQueue.isGamingVbcMode && !mSHOQueue.isRecordingMode &&
                                    !btDevice.equals(mPrevActiveUcastDevice)) {
                                    Log.d(TAG, "startSho for uncast device " + btDevice);
                                    ret = startSho(btDevice, targetProfile);
                                    if (SHO_SUCCESS == ret) {
                                        Log.d(TAG, "SHO success for new unicast device");
                                        mPrevActiveUcastProfile = targetProfile;
                                        mPrevActiveUcastDevice = btDevice;
                                        Target.Device = btDevice;
                                        Target.absoluteDevice = mSHOQueue.device;
                                        Target.Profile = targetProfile;
                                        sendUpdate = true;
                                    } else {
                                        Log.w(TAG, "SHO failed for new unicast device, return");
                                    }
                                }
                                if (sendUpdate) {
                                    sendActiveDeviceUpdate(mAudioType,Target);
                                }
                                if (mWaitForLock) {
                                    lock.lock();
                                    mWaitForLock = false;
                                    voiceHandoffComplete.signal();
                                    lock.unlock();
                                    Log.d(TAG, "Unlock SetActiveDeviceBlocking");
                                }
                            } else {
                                if(mSHOQueue.isUIReq) {
                                    transitionTo(mActivating);
                                }
                            }
                        }
                        break;

                    case Event.ACTIVE_DEVICE_CHANGE:
                        break;

                    case Event.REMOVE_DEVICE:
                        if (ApmConstIntf.getQtiLeAudioEnabled()) {
                            if(mSHOQueue.device == null) {
                                transitionTo(mDeactivating);
                            } else {
                                transitionTo(mActivating);
                            }
                        } else if (ApmConst.getAospLeaEnabled()) {
                            if(mSHOQueue.device == null) {
                                if (mSHOQueue.stopBroadcasting) {
                                    transitionTo(mDeactivating);
                                } else {
                                    // Previous active unicast device is removed/disconnected while broadcasting
                                    if (mPrevActiveUcastDevice != null) {
                                        Log.d(TAG, "Broadcasting, Deactivate unicast device: " + mPrevActiveUcastDevice);
                                        ret = startSho(null, mPrevActiveUcastProfile);
                                        Log.d(TAG, "Broadcasting(): startSho ret: " + ret);
                                        if (SHO_SUCCESS == ret) {
                                            Log.d(TAG, "Broadcasting, Deactivate unicast device success");
                                            mPrevActiveUcastDevice = null;
                                            mPrevActiveUcastProfile = ApmConst.AudioProfiles.NONE;
                                        } else if (SHO_PENDING == ret) {
                                            Log.d(TAG, "Broadcasting, Deactivate unicast device pending");
                                            sendMessageDelayed(Event.DEACTIVATE_TIMEOUT, DEACTIVATE_TIMEOUT_DELAY);
                                        } else {
                                            Log.d(TAG, "Broadcasting(): startSho failed");
                                        }
                                    }
                                    if (mPrevActiveUcastDevice == null || ret == SHO_SUCCESS) {
                                        lock.lock();
                                        voiceHandoffComplete.signal();
                                        mWaitForLock = false;
                                        lock.unlock();
                                        Log.d(TAG, "Unlock SetActiveDeviceBlocking");
                                    }
                                }
                            } else {
                                transitionTo(mActivating);
                            }
                        }
                        break;

                    case Event.DEVICE_REMOVED:
                        if(!ApmConstIntf.getQtiLeAudioEnabled()) {
                            Log.d(TAG, "Broadcasting, unicast active device is removed");
                            removeMessages(Event.DEACTIVATE_TIMEOUT);
                            mPrevActiveUcastDevice = null;
                            mPrevActiveUcastProfile = ApmConst.AudioProfiles.NONE;
                            lock.lock();
                            voiceHandoffComplete.signal();
                            mWaitForLock = false;
                            lock.unlock();
                            Log.d(TAG, "Unlock SetActiveDeviceBlocking");
                        }
                        break;

                    case Event.DEACTIVATE_TIMEOUT:
                        if(!ApmConstIntf.getQtiLeAudioEnabled()) {
                            Log.d(TAG, "Broadcasting, Deactivate unicast device timeout");
                        }
                        lock.lock();
                        voiceHandoffComplete.signal();
                        mWaitForLock = false;
                        lock.unlock();
                        Log.d(TAG, "Unlock SetActiveDeviceBlocking");
                        break;

                    case Event.STOP_SM:
                        transitionTo(mDeactivating);
                        enabled = false;
                        break;

                    default:
                        return NOT_HANDLED;
                }
                return HANDLED;
            }
        }

        class Recording extends State {
            int ret;
            @Override
            public void enter() {
                synchronized (this) {
                    mMsgProcessing = Event.NONE;
                    mState = mRecordingMode;
                }
                sendActiveDeviceRecordingUpdate(Current);
                updatePending = false;
                mRecordingSuspended = false;
            }

            @Override
            public void exit() {
                mPrevState = mRecordingMode;
                mPrevStableState = mRecordingMode;
                mPrevActiveDevice = Current.Device;
                mPrevActiveProfile = Current.Profile;
                mPrevActiveUcastDevice = Current.Device;
                mPrevActiveUcastProfile = Current.Profile;
                HeadsetService hfpService = HeadsetService.getHeadsetService();

                /*if (!ApmConst.getQtiLeAudioEnabled()) {
                    mAudioManager.handleBluetoothActiveDeviceChanged(
                           Current.Device, mPrevActiveDevice,
                           BluetoothProfileConnectionInfo.createLeAudioInfo(true, false));
                } else */
                if (ApmConst.getQtiLeAudioEnabled()) {
                    mAudioManager.handleBluetoothActiveDeviceChanged(
                               null, mPrevActiveDevice,
                               BluetoothProfileConnectionInfo.createA2dpSinkInfo(-1));
                }

                if(mRecordingSuspended) {
                  mAudioManager.setParameters("A2dpCaptureSuspend=false");
                  mRecordingSuspended = false;
                }
                CallAudio mCallAudio = CallAudio.get();
                boolean isInCall = mCallAudio != null &&
                                   mCallAudio.isVoiceOrCallActive();
                if(isInCall) {
                  Log.d(TAG, " reset txStreamSuspended as call is active" );
                  txStreamSuspended = false;
                }
                VolumeManager mVolumeManager = VolumeManager.get();
                if(mVolumeManager != null) {
                    mVolumeManager.saveVolume(mAudioType);
                }
            }

            @Override
            public boolean processMessage(Message message) {
                log("Recording: Process Message (" + mAudioType + "): "
                        + messageWhatToString(message.what));
                mMsgProcessing = message.what;
                switch(message.what) {
                    case Event.SET_ACTIVE:
                        if(mSHOQueue.device == null) {
                            Log.w(TAG, "Invalid request");
                            break;
                        } else {
                            Log.w(TAG, "Recording: Checking whether it is for same device:" +
                                       " isRecordingMode:" + mSHOQueue.isRecordingMode);
                            if (mSHOQueue.isRecordingMode &&
                                Current.Device.equals(mSHOQueue.device)) {
                                Log.w(TAG, "Recording: Spurious request for same device. Ignore");
                                mSHOQueue.reset();
                                break;
                            }  else if (!mSHOQueue.isUIReq &&
                                        !mSHOQueue.PlayReq &&
                                        !mSHOQueue.stopRecording &&
                                Current.Device.equals(mSHOQueue.device)) {
                                Log.w(TAG, " don't activate media while Recording:");
                                mSHOQueue.reset();
                                lock.lock();
                                voiceHandoffComplete.signal();
                                mWaitForLock = false;
                                lock.unlock();Log.d(TAG, "unlock by signal");
                                break;
                            } else {
                                transitionTo(mActivating);
                            }
                        }
                        break;

                    case Event.ACTIVE_DEVICE_CHANGE:
                        break;

                    case Event.REMOVE_DEVICE:
                        transitionTo(mDeactivating);
                        break;

                    case Event.DEVICE_REMOVED:
                        //might have to handle
                        break;
                    case Event.SUSPEND_RECORDING: {
                        if(mRecordingSuspended) break;
                        mAudioManager.setParameters("A2dpCaptureSuspend=true");
                        mRecordingSuspended = true;
                    } break;

                    case Event.RESUME_RECORDING: {
                        if(!mRecordingSuspended) break;
                        mAudioManager.setParameters("A2dpCaptureSuspend=false");
                        mRecordingSuspended = false;
                    } break;
                    case Event.STOP_SM:
                        transitionTo(mDeactivating);
                        enabled = false;
                        break;

                    default:
                        return NOT_HANDLED;
                }
                return HANDLED;
            }
        }
        void checkAndBroadcastActiveDevice(BluetoothDevice device, int Profile) {
            CallAudio mCallAudio = CallAudio.get();
            boolean isInCall =
                   mCallAudio != null && mCallAudio.isVoiceOrCallActive();
            if (!isInCall) return;
            if (Profile == ApmConst.AudioProfiles.HAP_BREDR ||
                Profile == ApmConst.AudioProfiles.A2DP||
                Profile == ApmConst.AudioProfiles.HFP) return;
            List<AudioDeviceInfo> devices = mAudioManager.getAvailableCommunicationDevices();
            Log.d(TAG,"audio devices size: " + devices.size());
            if (devices.size() > 0) {
                for (AudioDeviceInfo dev : devices) {
                    if (dev.getType() == AudioDeviceInfo.TYPE_BLE_HEADSET) {
                        if (device.getAddress().equals(dev.getAddress())) {
                            Log.d(TAG, "Device connection already updated, " +
                                                            "broadcast le active devcie");
                            broadcastLeActiveDeviceChange(Current.absoluteDevice);
                        }
                    }
                }
            }
        }

        void sendActiveDeviceLeUpdate(DeviceProfileCombo Current) {
            BluetoothDevice mPrevADevice = mPrevActiveDevice;

            Log.d(TAG, "sendActiveDeviceLeUpdate: mPrevActiveDevice: " +
                       mPrevActiveDevice + ", Current.Device: " + Current.Device +
                       ", Current.Profile: " + Current.Profile +
                       "mIsAdmPtsEnabled: " + mIsAdmPtsEnabled);

            if (Current.Device.equals(mPrevActiveDevice)) {
                Log.d(TAG, "sendActiveDeviceLeUpdate: previous LE-A "+
                       " and current LE-A device are same.");
                mPrevADevice = null;
            }

            if(Current.Profile == ApmConst.AudioProfiles.HAP_BREDR) {
                if(mPrevActiveDevice != null) {
                    broadcastActiveDeviceChange (null, AudioType.MEDIA );
                    ActiveDeviceManager mDeviceManager = AdapterService.getAdapterService().getActiveDeviceManager();
                    mDeviceManager.onActiveDeviceChange(null, ApmConst.AudioFeatures.MEDIA_AUDIO);
                    mAudioManager.handleBluetoothActiveDeviceChanged(Current.Device, mPrevActiveDevice,
                           BluetoothProfileConnectionInfo.createHearingAidInfo(true));
                }
                return;
            }
            apmNative.activeDeviceUpdate(Current.Device, Current.Profile, AudioType.MEDIA);

            MediaAudio mMediaAudio = MediaAudio.get();
            mMediaAudio.refreshCurrentCodec(Current.Device, Current.Profile);

            broadcastActiveDeviceChange (null, AudioType.MEDIA);
            CallAudio mCallAudio = CallAudio.get();
            if(mCallAudio != null && mCallAudio.isVoipLeaWarEnabled()) {
                broadcastActiveDeviceChange (Current.absoluteDevice, AudioType.VOICE);
            } else {
                broadcastActiveDeviceChange (null, AudioType.VOICE);
            }
            // broadcast le active device change is sent after audio device
            // added callback received

            //2 Update dependent profiles
            VolumeManager mVolumeManager = VolumeManager.get();
            if(mVolumeManager != null) {
                mVolumeManager.onActiveDeviceChange(Current.Device,
                                                    ApmConst.AudioFeatures.MEDIA_AUDIO);
                mVolumeManager.onActiveDeviceChange(Current.Device,
                                                    ApmConst.AudioFeatures.CALL_AUDIO);
            }

            McpService mMcpService = McpService.getMcpService();
            if (mMcpService != null) {
                mMcpService.SetActiveDevices(Current.Device, ApmConst.AudioProfiles.MCP);
                Log.d(TAG, "SetActiveDevices called to MCP, Current.Device: " + Current.Device +
                           " Current.absoluteDevice: " + Current.absoluteDevice);
            }


            CCService ccService = CCService.getCCService();
            if (ccService != null) {
                ccService.setActiveDevice(Current.absoluteDevice);
            }

            /*if(mTargetSHO.PlayReq) {
                CallAudio mCallAudio = CallAudio.get();
                mCallAudio.connectAudio();
            }*/

            if (mAudioManager != null) {
                boolean isInCall =
                       mCallAudio != null && mCallAudio.isVoiceOrCallActive();
                if (mPrevActiveUcastDevice == null && isInCall &&
                    mPrevActiveDevice != null) {
                    List<AudioDeviceInfo> devices = mAudioManager.getAvailableCommunicationDevices();
                    Log.d(TAG,"audio devices size: " + devices.size());
                    if (devices.size() > 0) {
                        for (AudioDeviceInfo device : devices) {
                            if (device.getType() == AudioDeviceInfo.TYPE_BLE_HEADSET) {
                                if (Current.Device.getAddress().equals(device.getAddress())) {
                                    Log.d(TAG, "Device connection already updated, " +
                                                                    "broadcast le active devcie");
                                    broadcastLeActiveDeviceChange(Current.absoluteDevice);
                                    //mAudioManager.setParameters("LEASuspended=false");
                                    HeadsetService service = HeadsetService.getHeadsetService();
                                    if (service != null) {
                                        service.getHfpA2DPSyncInterface().releaseLeAudio();
                                    }
                                    updatePending = false;
                                    leaActiveDevUpdateSent = true;
                                    updatedPreviouselyToMM = true;
                                    return;
                                }
                            }
                        }
                    }
                }
                lock.lock();
                mWaitForLock = false;
                voiceHandoffComplete.signal();
                lock.unlock();
                Log.d(TAG, "unlock setActiveDeviceBlocking");
                int deviceVolume = 7;
                if (mVolumeManager != null && Current.Device != null) {
                    if (mVolumeManager.getBluetoothContextualVolumeStream() == AudioManager.STREAM_VOICE_CALL) {
                        deviceVolume = mVolumeManager.getSavedVolume(Current.Device, ApmConst.AudioFeatures.CALL_AUDIO);
                        Log.d(TAG, "get saved device volume: " + deviceVolume + " for voice stream");
                    } else {
                        deviceVolume = mVolumeManager.getSavedVolume(Current.Device, ApmConst.AudioFeatures.MEDIA_AUDIO);
                        Log.d(TAG, "get saved device volume: " + deviceVolume + " for media stream");
                    }
                }
                Log.d(TAG, "sendActiveDeviceLeUpdate: LEA IN & OUT devices ");

                if (mPrevStableState == mBroadcasting && mPrevActiveUcastDevice != null) {
                    mAudioManager.handleBluetoothActiveDeviceChanged(Current.Device, mPrevActiveUcastDevice,
                               getLeAudioOutputProfile(true, deviceVolume));

                    mAudioManager.handleBluetoothActiveDeviceChanged(Current.Device, mPrevActiveUcastDevice,
                               BluetoothProfileConnectionInfo.createLeAudioInfo(true, false));

                    mAudioManager.handleBluetoothActiveDeviceChanged(null, mPrevActiveDevice,
                                getBroadcastProfile(true, -1));
                                //BluetoothProfileConnectionInfo.createLeAudioBroadcastInfo(true));
                } else {
                    DeviceProfileMap dpm = DeviceProfileMap.getDeviceProfileMapInstance();
                    if (dpm == null) {
                        Log.w(TAG, "sendActiveDeviceLeUpdate: dpm is null, return.");
                        return;
                    }
                    int MediaProf =
                       dpm.getSupportedProfile(Current.Device, ApmConst.AudioFeatures.MEDIA_AUDIO);
                    int VoiceProf =
                       dpm.getSupportedProfile(Current.Device, ApmConst.AudioFeatures.CALL_AUDIO);
                    Log.d(TAG, "sendActiveDeviceLeUpdate: "+ " MediaProf:" + MediaProf +
                               ", VoiceProf:" + VoiceProf);
                    if (mIsAdmPtsEnabled ||
                        ((ApmConst.AudioProfiles.BAP_CALL & VoiceProf) !=
                             ApmConst.AudioProfiles.BAP_CALL) ||
                        ((ApmConst.AudioProfiles.BAP_MEDIA & MediaProf) !=
                                                       ApmConst.AudioProfiles.BAP_MEDIA)) {
                        Log.d(TAG, "sendActiveDeviceLeUpdate: Updating Out device only for PTS");
                        mAudioManager.handleBluetoothActiveDeviceChanged(Current.Device, mPrevADevice,
                                   getLeAudioOutputProfile(true, deviceVolume));

                        if (true /*((ApmConst.AudioProfiles.BAP_MEDIA & MediaProf) !=
                              ApmConst.AudioProfiles.BAP_MEDIA) &&
                            Current.Profile == ApmConst.AudioProfiles.BAP_CALL ||
                            Current.Profile == ApmConst.AudioProfiles.TMAP_CALL ||
                            Current.Profile == ApmConst.AudioProfiles.BAP_RECORDING ||
                            Current.Profile == ApmConst.AudioProfiles.BAP_GCP_VBC*/) {
                            Log.d(TAG, "sendActiveDeviceLeUpdate: Updating In device also for PTS");
                            mAudioManager.handleBluetoothActiveDeviceChanged(Current.Device, mPrevADevice,
                                   BluetoothProfileConnectionInfo.createLeAudioInfo(true, false));
                        }
                    } else {
                        mPrevADevice = mPrevActiveUcastDevice != null?mPrevActiveDevice:null;
                        mAudioManager.handleBluetoothActiveDeviceChanged(Current.Device, mPrevADevice,
                                   getLeAudioOutputProfile(true, deviceVolume));

                        mAudioManager.handleBluetoothActiveDeviceChanged(Current.Device, mPrevADevice,
                                   BluetoothProfileConnectionInfo.createLeAudioInfo(true, false));
                    }
                }
                if (mPrevActiveUcastProfile == ApmConst.AudioProfiles.NONE) {
                    mAudioManager.handleBluetoothActiveDeviceChanged(null, mPrevActiveDevice,
                                        BluetoothProfileConnectionInfo.createA2dpInfo(true, -1));
                }
                Log.d(TAG, "leaActiveDevUpdateSent set to true" );
                leaActiveDevUpdateSent = true;
            }
            updatePending = false;
        }

        void sendActiveDeviceMediaUpdate(DeviceProfileCombo Current) {
            Log.d(TAG, "sendActiveDeviceMediaUpdate: mPrevActiveDevice: " +
                       mPrevActiveDevice + ", Current.Device: " + Current.Device +
                       ", Current.Profile: " + Current.Profile);
            StreamAudioService streamAudioService;
            if(Current.Profile == ApmConst.AudioProfiles.HAP_BREDR) {
                if (ApmConst.getQtiLeAudioEnabled()) {
                    if(mPrevActiveDevice != null) {
                        broadcastActiveDeviceChange (null, AudioType.MEDIA );
                        ActiveDeviceManager mDeviceManager = AdapterService.getAdapterService().getActiveDeviceManager();
                        mDeviceManager.onActiveDeviceChange(null, ApmConst.AudioFeatures.MEDIA_AUDIO);
                        mAudioManager.handleBluetoothActiveDeviceChanged(null, mPrevActiveDevice,
                               BluetoothProfileConnectionInfo.createA2dpInfo(true, -1));
                    }
                } else if (ApmConst.getAospLeaEnabled()) {
                    // AOSP LE Audio Enabled
                    if (mPrevActiveUcastDevice != null) {
                        broadcastLeActiveDeviceChange (null);
                        if (mPrevStableState == mBroadcasting) {
                            // Broadcasting -> HAP Active
                            Log.d(TAG, "Broadcasting -> HAP Active, Notify MM-Audio Bcast and Ucast Inactive");
                            mAudioManager.handleBluetoothActiveDeviceChanged(null, mPrevActiveUcastDevice,
                                    BluetoothProfileConnectionInfo.createLeAudioInfo(true, true));

                            mAudioManager.handleBluetoothActiveDeviceChanged(null, mPrevActiveUcastDevice,
                                    BluetoothProfileConnectionInfo.createLeAudioInfo(true, false));

                            mAudioManager.handleBluetoothActiveDeviceChanged(null, mPrevActiveDevice,
                                    getBroadcastProfile(true, -1));
                                    //BluetoothProfileConnectionInfo.createLeAudioBroadcastInfo(true));
                        } else {
                            // LE Unicast Active -> HAP Active
                            Log.d(TAG, "LE-Unicast -> HAP active, Notify MM-Audio Ucast Inactive");
                            mAudioManager.handleBluetoothActiveDeviceChanged(null, mPrevActiveUcastDevice,
                                    BluetoothProfileConnectionInfo.createLeAudioInfo(true, true));

                            mAudioManager.handleBluetoothActiveDeviceChanged(null, mPrevActiveUcastDevice,
                                    BluetoothProfileConnectionInfo.createLeAudioInfo(true, false));
                        }
                    } else if (mPrevActiveDevice != null) {
                        // A2DP Active -> HAP Active
                        Log.d(TAG, "A2DP -> HAP Ative, Notify MM-Audio A2DP Inactive");
                        broadcastActiveDeviceChange (null, AudioType.MEDIA);
                        ActiveDeviceManager mDeviceManager = AdapterService.getAdapterService().getActiveDeviceManager();
                        mDeviceManager.onActiveDeviceChange(null, ApmConst.AudioFeatures.MEDIA_AUDIO);
                        mAudioManager.handleBluetoothActiveDeviceChanged(null, mPrevActiveDevice,
                                BluetoothProfileConnectionInfo.createA2dpInfo(true, -1));
                    }
                }
                return;
            }
            apmNative.activeDeviceUpdate(Current.Device, Current.Profile, AudioType.MEDIA);

            MediaAudio mMediaAudio = MediaAudio.get();
            mMediaAudio.refreshCurrentCodec(Current.Device, Current.Profile);

            if (mPrevActiveUcastDevice != null) {
                broadcastLeActiveDeviceChange (null);
            }
            broadcastActiveDeviceChange (Current.absoluteDevice, AudioType.MEDIA);
            ActiveDeviceManager mDeviceManager = AdapterService.getAdapterService().getActiveDeviceManager();
            mDeviceManager.onActiveDeviceChange(Current.Device, ApmConst.AudioFeatures.MEDIA_AUDIO);

            streamAudioService = StreamAudioService.getStreamAudioService();
            if (streamAudioService != null) {
                Log.d(TAG, " sendActiveDeviceMediaUpdate, resetLowLatencyMode");
                streamAudioService.resetLowLatencyMode();
            }
            //2 Update dependent profiles
            VolumeManager mVolumeManager = VolumeManager.get();

            McpService mMcpService = McpService.getMcpService();
            if (mMcpService != null) {
                mMcpService.SetActiveDevices(Current.Device, ApmConst.AudioProfiles.MCP);
                Log.d(TAG, " SetActiveDevices called to MCP, Current.Device: " + Current.Device +
                           " Current.absoluteDevice: " + Current.absoluteDevice);
            }
            int deviceVolume = 7;
            if(mVolumeManager != null && Current.Device != null) {
                deviceVolume = mVolumeManager.getSavedVolume(Current.Device, ApmConst.AudioFeatures.MEDIA_AUDIO);
                Log.d(TAG, "get saved device volume: " + deviceVolume);
            }
            lock.lock();
            mWaitForLock = false;
            voiceHandoffComplete.signal();
            lock.unlock();
            Log.d(TAG, "Unlock SetActiveDeviceBlocking");
            Log.d(TAG, "mPrevActiveProfile is " + mPrevActiveProfile);
            if(mAudioManager != null) {
                if (ApmConst.getAospLeaEnabled()) {
                    if (mPrevActiveUcastDevice != null) {
                        Log.d(TAG, "LEA to A2DP SHO");
                        mAudioManager.handleBluetoothActiveDeviceChanged(Current.Device, null,
                                   BluetoothProfileConnectionInfo.createA2dpInfo(true, deviceVolume));
                    } else if (mPrevStableState == mBroadcasting) {
                        Log.d(TAG, "Broadcasting to A2DP SHO");
                        mAudioManager.handleBluetoothActiveDeviceChanged(Current.Device, null,
                                   BluetoothProfileConnectionInfo.createA2dpInfo(true, deviceVolume));
                    } else if (mPrevActiveProfile == ApmConst.AudioProfiles.A2DP) {
                        Log.d(TAG, "A2DP to A2DP SHO");
                        mAudioManager.handleBluetoothActiveDeviceChanged(Current.Device, mPrevActiveDevice,
                                   BluetoothProfileConnectionInfo.createA2dpInfo(true, deviceVolume));
                    } else {
                        Log.d(TAG, "Non-A2DP to A2DP SHO");
                        mAudioManager.handleBluetoothActiveDeviceChanged(Current.Device, null,
                                   BluetoothProfileConnectionInfo.createA2dpInfo(true, deviceVolume));
                    }
                } else {
                    mAudioManager.handleBluetoothActiveDeviceChanged(Current.Device, mPrevActiveDevice,
                               BluetoothProfileConnectionInfo.createA2dpInfo(true, deviceVolume));
                }
                if (mPrevStableState == mBroadcasting && mPrevActiveUcastDevice != null) {
                    Log.d(TAG, "Broadcasting to non-BLE profile switch, notify MM-audio Bcast and Ucast inactive");
                    mAudioManager.handleBluetoothActiveDeviceChanged(null, mPrevActiveUcastDevice,
                                BluetoothProfileConnectionInfo.createLeAudioInfo(true, true));
                    mAudioManager.handleBluetoothActiveDeviceChanged(null, mPrevActiveUcastDevice,
                                BluetoothProfileConnectionInfo.createLeAudioInfo(true, false));
                    mAudioManager.handleBluetoothActiveDeviceChanged(null, mPrevActiveDevice,
                                getBroadcastProfile(true, -1));
                                //BluetoothProfileConnectionInfo.createLeAudioBroadcastInfo(true));
                } else if (mPrevActiveUcastDevice != null){
                    mAudioManager.handleBluetoothActiveDeviceChanged(null, mPrevActiveDevice,
                                BluetoothProfileConnectionInfo.createLeAudioInfo(true, true));
                    mAudioManager.handleBluetoothActiveDeviceChanged(null, mPrevActiveDevice,
                                BluetoothProfileConnectionInfo.createLeAudioInfo(true, false));
                }
                Log.d(TAG, "leaActiveDevUpdateSent reset " );
                leaActiveDevUpdateSent = false;
            }

            if(mVolumeManager != null) {
                mVolumeManager.onActiveDeviceChange(Current.Device,
                                                    ApmConst.AudioFeatures.MEDIA_AUDIO);
            }
            updatePending = false;
        }

        void sendActiveDeviceGamingUpdate(DeviceProfileCombo Current, boolean notifyMMonly) {
            Log.d(TAG, " sendActiveDeviceGamingUpdate(), notifyMMonly: " + notifyMMonly);
            apmNative.activeDeviceUpdate(Current.Device, Current.Profile, AudioType.MEDIA);

            MediaAudio mMediaAudio = MediaAudio.get();
            mMediaAudio.refreshCurrentCodec(Current.Device, Current.Profile);

            if (!notifyMMonly) {
                ActiveDeviceManager mDeviceManager =
                           AdapterService.getAdapterService().getActiveDeviceManager();
                if (!ApmConst.getQtiLeAudioEnabled() &&
                    (Current.Profile == ApmConst.AudioProfiles.BAP_GCP ||
                     Current.Profile == ApmConst.AudioProfiles.BAP_GCP_VBC)) {
                    broadcastLeActiveDeviceChange(Current.absoluteDevice);
                    mDeviceManager.onLeActiveDeviceChange(Current.absoluteDevice);
                } else {
                    broadcastActiveDeviceChange (Current.absoluteDevice, AudioType.MEDIA);
                    mDeviceManager.onActiveDeviceChange(Current.Device,
                                             ApmConst.AudioFeatures.MEDIA_AUDIO);
                }
                //mDeviceManager.onActiveDeviceChange(Current.Device,
                //                                    ApmConst.AudioFeatures.MEDIA_AUDIO);
            }

            //2 Update dependent profiles
            VolumeManager mVolumeManager = VolumeManager.get();
            int deviceVolume = 7;
            if(mVolumeManager != null) {
                mVolumeManager.onActiveDeviceChange(Current.Device,
                                                    ApmConst.AudioFeatures.MEDIA_AUDIO);
                deviceVolume =
                    mVolumeManager.getActiveVolume(ApmConst.AudioFeatures.MEDIA_AUDIO);
            }

            if (mAudioManager != null) {
                /*Add back channel call here*/

                Log.e(TAG, " sendActiveDeviceGamingUpdate(), for profile: " + Current.Profile);

                /*if (!ApmConst.getQtiLeAudioEnabled()) {
                    if (Current.Profile == ApmConst.AudioProfiles.BAP_GCP_VBC) {
                        mAudioManager.handleBluetoothActiveDeviceChanged(
                               Current.Device, mPrevActiveDevice,
                               BluetoothProfileConnectionInfo.createLeAudioInfo(true, true));
                        mAudioManager.handleBluetoothActiveDeviceChanged(
                               Current.Device, mPrevActiveDevice,
                               BluetoothProfileConnectionInfo.createLeAudioInfo(true, false));
                    } else {
                        mAudioManager.handleBluetoothActiveDeviceChanged(
                               Current.Device, mPrevActiveDevice,
                               BluetoothProfileConnectionInfo.createLeAudioInfo(true, true));
                    }
                } else*/
                if (ApmConst.getQtiLeAudioEnabled()) {
                    if (Current.Profile == ApmConst.AudioProfiles.BAP_GCP_VBC) {
                        mAudioManager.handleBluetoothActiveDeviceChanged(
                               Current.Device, mPrevActiveDevice,
                               BluetoothProfileConnectionInfo.createA2dpInfo(true, deviceVolume));
                        mAudioManager.handleBluetoothActiveDeviceChanged(
                               Current.Device, null,
                               BluetoothProfileConnectionInfo.createA2dpSinkInfo(deviceVolume));
                    } else {
                        mAudioManager.handleBluetoothActiveDeviceChanged(
                               Current.Device, mPrevActiveDevice,
                               BluetoothProfileConnectionInfo.createA2dpInfo(true, deviceVolume));
                    }
                }
            }
            updatePending = false;
        }

        void sendActiveDeviceVoiceUpdate(DeviceProfileCombo Current) {
            Log.d(TAG, "sendActiveDeviceVoiceUpdate: Current.Profile: " + Current.Profile +
                       ", mPrevActiveUcastDevice: " + mPrevActiveUcastDevice +
                       ", mPrevActiveUcastDevice: " + mPrevActiveDevice);
            if(Current.Profile == ApmConst.AudioProfiles.HAP_BREDR) {
                if(mPrevActiveDevice != null) {
                    broadcastActiveDeviceChange (null, AudioType.VOICE);
                    ActiveDeviceManager mDeviceManager = AdapterService.getAdapterService().getActiveDeviceManager();
                    mDeviceManager.onActiveDeviceChange(null, ApmConst.AudioFeatures.CALL_AUDIO);
                }
                return;
            }

            broadcastActiveDeviceChange (Current.absoluteDevice, AudioType.VOICE);
            if (!ApmConst.getQtiLeAudioEnabled()) {
                if ((sm[AudioType.MEDIA].mState == sm[AudioType.MEDIA].mBroadcasting)
                        && (mPrevActiveUcastDevice != null)) {
                    // BLE unicast active -> Enable Broadcasting  -> Legacy Headset
                    Log.d(TAG, "Update LE active device change while broadcast is active and LE voice is inactive");
                    broadcastLeActiveDeviceChange (null);

                    mAudioManager.handleBluetoothActiveDeviceChanged(null, mPrevActiveUcastDevice,
                            BluetoothProfileConnectionInfo.createLeAudioInfo(true, true));

                    mAudioManager.handleBluetoothActiveDeviceChanged(null, mPrevActiveUcastDevice,
                            BluetoothProfileConnectionInfo.createLeAudioInfo(true, false));
                }
            }

            ActiveDeviceManager mDeviceManager = AdapterService.getAdapterService().getActiveDeviceManager();
            mDeviceManager.onActiveDeviceChange(Current.Device, ApmConst.AudioFeatures.CALL_AUDIO);
            VolumeManager mVolumeManager = VolumeManager.get();
            if(mVolumeManager != null) {
                mVolumeManager.onActiveDeviceChange(Current.Device,
                                                    ApmConst.AudioFeatures.CALL_AUDIO);
            }
            CCService ccService = CCService.getCCService();
            if (ccService != null) {
                ccService.setActiveDevice(Current.absoluteDevice);
            }

            if(mTargetSHO.PlayReq) {
                CallAudio mCallAudio = CallAudio.get();
                mCallAudio.connectAudio();
            }

            updatePending = false;
        }

        void sendActiveDeviceRecordingUpdate(DeviceProfileCombo Current) {
            apmNative.activeDeviceUpdate(Current.Device, Current.Profile, AudioType.MEDIA);

            Log.d(TAG, "sendActiveDeviceRecordingUpdate: mPrevActiveDevice: "
                         + mPrevActiveDevice + ", Current.Device: " + Current.Device);
            MediaAudio mMediaAudio = MediaAudio.get();
            mMediaAudio.refreshCurrentCodec(Current.Device, Current.Profile);

            if (mAudioManager != null) {
                /*if (!ApmConst.getQtiLeAudioEnabled()) {
                   mAudioManager.handleBluetoothActiveDeviceChanged(
                           Current.Device, mPrevActiveDevice,
                           BluetoothProfileConnectionInfo.createLeAudioInfo(false, false));
                } else*/
                if (ApmConst.getQtiLeAudioEnabled()) {
                    mAudioManager.handleBluetoothActiveDeviceChanged(
                               Current.Device, null,
                               BluetoothProfileConnectionInfo.createA2dpSinkInfo(-1));
                }
            }
            updatePending = false;
        }

        boolean isSameProfile (int p1, int p2, int audioType) {
            if(p1 == p2) {
                return true;
            }

            if(audioType == AudioType.MEDIA) {
                int leMediaMask = ApmConst.AudioProfiles.TMAP_MEDIA |
                                  ApmConst.AudioProfiles.BAP_MEDIA |
                                  ApmConst.AudioProfiles.BAP_RECORDING |
                                  ApmConst.AudioProfiles.BAP_GCP|
                                  ApmConst.AudioProfiles.BAP_GCP_VBC;
                if((leMediaMask & p1) > 0 && (leMediaMask & p2) > 0) {
                    return true;
                }
            } else if(audioType == AudioType.VOICE) {
                int leVoiceMask =
                  ApmConst.AudioProfiles.TMAP_CALL | ApmConst.AudioProfiles.BAP_CALL;
                if((leVoiceMask & p1) > 0 && (leVoiceMask & p2) > 0) {
                    return true;
                }
            }

            return false;
        }

        boolean isLeUnicastToBroadcast(int p1, int p2, int audioType) {
            if (audioType == AudioType.MEDIA) {
                int leMediaMask = ApmConst.AudioProfiles.TMAP_MEDIA |
                                  ApmConst.AudioProfiles.BAP_MEDIA |
                                  ApmConst.AudioProfiles.BAP_RECORDING |
                                  ApmConst.AudioProfiles.BAP_GCP|
                                  ApmConst.AudioProfiles.BAP_GCP_VBC;
                if((leMediaMask & p1) > 0 && (ApmConst.AudioProfiles.BROADCAST_LE & p2) > 0) {
                    Log.d(TAG, "Media audio switch from LE Unicast to Broadcast");
                    return true;
                }
            }
            return false;
        }
    }

    /*static class AudioType {
        public static final int VOICE = ApmConst.AudioFeatures.CALL_AUDIO;
        public static final int MEDIA = ApmConst.AudioFeatures.MEDIA_AUDIO;

        public static int SIZE = 2;
    }*/

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
