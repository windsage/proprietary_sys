/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
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
 **************************************************************************/

package com.android.bluetooth.apm;

import android.bluetooth.BleBroadcastAudioScanAssistManager;
import android.bluetooth.BleBroadcastSourceInfo;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.DeviceGroup;
import com.android.bluetooth.btservice.AdapterService;
import com.android.bluetooth.a2dp.A2dpService;
import com.android.bluetooth.avrcp.Avrcp_ext;
import com.android.bluetooth.acm.AcmService;
import com.android.bluetooth.bc.BCService;
import com.android.bluetooth.groupclient.GroupService;
import com.android.bluetooth.hfp.HeadsetService;
import com.android.bluetooth.CsipWrapper;
import com.android.bluetooth.Utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioDeviceAttributes;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Log;

import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class VolumeManager {
    public static final String TAG = "APM: VolumeManager";
    private static VolumeManager mVolumeManager = null;
    private AdapterService mAdapterService;
    private DeviceVolume mMedia;
    private DeviceVolume mCall;
    private DeviceVolume mBroadcast;
    private DeviceProfileMap dpm;
    private MediaAudio mMediaAudio;
    private CallAudio mCallAudio;
    private int mMediaAudioStreamMax;
    private int mCallAudioStreamMax;
    private int mCallAudioStreamVolume;
    private static Context mContext;
    private VolumeManagerMessageHandler mHandler;
    private boolean mPtsTest = false;
    BroadcastReceiver mVolumeManagerReceiver;
    Map<String, Integer> AbsVolumeSupport;
    private int mAudioMode = AudioManager.MODE_INVALID;
    private final AudioDeviceChangedCallback mAudioDeviceChangedCallback =
            new AudioDeviceChangedCallback();

    public static final String CALL_VOLUME_MAP = "bluetooth_call_volume_map";
    public static final String MEDIA_VOLUME_MAP = "bluetooth_media_volume_map";
    public static final String BROADCAST_VOLUME_MAP = "bluetooth_broadcast_volume_map";
    public final String ACTION_SHUTDOWN = "android.intent.action.ACTION_SHUTDOWN";
    public final String ACTION_POWER_OFF = "android.intent.action.QUICKBOOT_POWEROFF";
    private static final int SET_AUDIO_CALL_VOLUME_DELAY_DEFAULT = 1000;
    private static final int VCP_MAX_VOL = 255;

    // Volume Manager handler messages
    public static final int MESSAGE_SET_CALL_VOLUME_TO_AUDIO_TIMEOUT = 1;
    public static final int MESSAGE_SET_LE_AUDIO_VOLUME = 2;

    private VolumeManager() {
        mAdapterService = Objects.requireNonNull(AdapterService.getAdapterService(),
                "AdapterService cannot be null when VolumeManager starts");

        mCall = new DeviceVolume(mContext, CALL_VOLUME_MAP);
        mMedia = new DeviceVolume(mContext, MEDIA_VOLUME_MAP);
        mBroadcast = new DeviceVolume(mContext, BROADCAST_VOLUME_MAP);

        dpm = DeviceProfileMap.getDeviceProfileMapInstance();
        mMediaAudio = MediaAudio.get();
        mCallAudio = CallAudio.get();

        mMediaAudioStreamMax = mMediaAudio.getAudioManager().getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        mMedia.setSafeVolume(mMediaAudioStreamMax/2);
        mBroadcast.setSafeVolume(mMediaAudioStreamMax/2);

        if (ApmConst.getQtiLeAudioEnabled()) {
            mCallAudioStreamMax = mCallAudio.getAudioManager().getStreamMaxVolume(AudioManager.STREAM_BLUETOOTH_SCO);
            mCall.setSafeVolume(mCallAudioStreamMax/2);
        } else {
            // Align with volume scales and default volume level in MM-audio side in AOSP LeAudio
            mCallAudioStreamMax = mCallAudio.getAudioManager().getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);
            mCall.setSafeVolume(mCallAudioStreamMax/2 + 1);
        }

        AbsVolumeSupport = new ConcurrentHashMap<String, Integer>();

        mVolumeManagerReceiver = new VolumeManagerReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(AudioManager.VOLUME_CHANGED_ACTION);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(ACTION_SHUTDOWN);
        filter.addAction(ACTION_POWER_OFF);
        filter.addAction(BleBroadcastAudioScanAssistManager.ACTION_BROADCAST_SOURCE_INFO);
        mContext.registerReceiver(mVolumeManagerReceiver, filter, Context.RECEIVER_EXPORTED);

        mMediaAudio.getAudioManager().registerAudioDeviceCallback(
                mAudioDeviceChangedCallback, null);

        HandlerThread thread = new HandlerThread("VolumeManagerHandler");
        thread.start();
        Looper looper = thread.getLooper();
        mHandler = new VolumeManagerMessageHandler(looper);
        mBluetoothOnModeChangedListener = new BluetoothOnModeChangedListener();
        mMediaAudio.getAudioManager().addOnModeChangedListener(
                 Executors.newSingleThreadExecutor(), mBluetoothOnModeChangedListener);
        //sync with current audio mode when constructed
        mAudioMode = mMediaAudio.getAudioManager().getMode();
        mCallAudioStreamVolume = -1;
        mPtsTest = SystemProperties.getBoolean("persist.vendor.service.bt.vcp_controller.pts", false);
    }

    public static VolumeManager init (Context context) {
        mContext = context;

        if(mVolumeManager == null) {
            mVolumeManager = new VolumeManager();
            VolumeManagerIntf.init(mVolumeManager);
        }

        return mVolumeManager;
    }

    public void cleanup() {
        Log.i(TAG, "cleanup");
        handleDeviceShutdown();
        synchronized (mVolumeManager) {
            mCall = null;
            mMedia = null;
            mBroadcast = null;
            mContext.unregisterReceiver(mVolumeManagerReceiver);
            mVolumeManagerReceiver = null;
            AbsVolumeSupport.clear();
            AbsVolumeSupport = null;
            mVolumeManager = null;
            mMediaAudio.getAudioManager().unregisterAudioDeviceCallback(
                    mAudioDeviceChangedCallback);
            if (mBluetoothOnModeChangedListener != null) {
                mMediaAudio.getAudioManager().removeOnModeChangedListener(mBluetoothOnModeChangedListener);
                mBluetoothOnModeChangedListener = null;
            }
        }
    }

    public static VolumeManager get() {
        return mVolumeManager;
    }

    private class AudioDeviceChangedCallback extends AudioDeviceCallback {
        @Override
        public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
            if (addedDevices == null)
                return;
            Log.d(TAG, "onAudioDevicesAdded: size=" + addedDevices.length);

            for (AudioDeviceInfo deviceInfo : addedDevices) {
                Log.d(TAG, "onAudioDevicesAdded: address=" +
                        deviceInfo.getAddress() + ", type=" + deviceInfo.getType());
                String addr = deviceInfo.getAddress();
                if (!BluetoothAdapter.checkBluetoothAddress(addr))
                    continue;

                if (mMedia != null && mMedia.mDevice != null &&
                        deviceInfo.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP) {
                    BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(addr);
                    AcmService mAcmService = AcmService.getAcmService();
                    BluetoothDevice mGroupDevice;
                    if(mAcmService != null) {
                        mGroupDevice = mAcmService.getGroup(device);
                    } else {
                        mGroupDevice = device;
                    }
                    if (mGroupDevice != null && mGroupDevice.equals(mMedia.mDevice)) {
                        int absVolSupportProfiles = AbsVolumeSupport.getOrDefault(mGroupDevice.getAddress(), 0);
                        boolean isAbsSupported = absVolSupportProfiles != 0;
                        Log.i(TAG, "isAbsoluteVolumeSupport:  " + isAbsSupported);
                        mMediaAudio.getAudioManager().setDeviceVolumeBehavior(new AudioDeviceAttributes(
                            AudioDeviceAttributes.ROLE_OUTPUT, AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, addr),
                            isAbsSupported ? AudioManager.DEVICE_VOLUME_BEHAVIOR_ABSOLUTE :
                            AudioManager.DEVICE_VOLUME_BEHAVIOR_VARIABLE);
                        break;
                    }
                }
             }
        }

        @Override
        public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
            Log.d(TAG, "onAudioDevicesRemoved: size=" + removedDevices.length);

            for (AudioDeviceInfo deviceInfo : removedDevices) {
                Log.d(TAG, "onAudioDevicesRemoved: address=" +
                        deviceInfo.getAddress() + ", type=" + deviceInfo.getType());
                String addr = deviceInfo.getAddress();
                if (!BluetoothAdapter.checkBluetoothAddress(addr))
                    continue;

                if (mMedia != null && mMedia.mDevice != null
                        && mMedia.mProfile == ApmConst.AudioProfiles.VCP
                        && deviceInfo.getType() == AudioDeviceInfo.TYPE_BLE_BROADCAST) {
                    Log.i(TAG, "restore volume for LE media device:  " + mMedia.mDevice + " volume " + mMedia.mVolume);
                    mMediaAudio.getAudioManager().setStreamVolume(AudioManager.STREAM_MUSIC, mMedia.mVolume,
                            AudioManager.FLAG_BLUETOOTH_ABS_VOLUME);
                    break;
                }
            }
        }
    }

    class BluetoothOnModeChangedListener implements AudioManager.OnModeChangedListener {
        @Override
        public void onModeChanged(int mode) {
            Log.d(TAG, "onModeChanged mode = " + mode);
            mAudioMode = mode;
        }
    }

    private BluetoothOnModeChangedListener mBluetoothOnModeChangedListener;

     /** Handles Volume Manager messages. */
     private final class VolumeManagerMessageHandler extends Handler {
         private VolumeManagerMessageHandler(Looper looper) {
             super(looper);
         }

         @Override
         public void handleMessage(Message msg) {
             Log.v(TAG, "VolumeManagerMessageHandler: received message=" + msg.what);

             switch (msg.what) {
                 case MESSAGE_SET_CALL_VOLUME_TO_AUDIO_TIMEOUT:
                     int volume = mCall.mVolume;
                     mCallAudio.getAudioManager().setStreamVolume(AudioManager.STREAM_BLUETOOTH_SCO, volume, 0);
                     break;
                 case MESSAGE_SET_LE_AUDIO_VOLUME:
                 {
                     Log.v(TAG, "MESSAGE_SET_LE_AUDIO_VOLUME: " + msg.arg1);
                     AcmService mAcmService = AcmService.getAcmService();
                     int streamType = getBluetoothContextualVolumeStream();
                     int audioType = (streamType == AudioManager.STREAM_VOICE_CALL) ?
                                 ApmConst.AudioFeatures.CALL_AUDIO : ApmConst.AudioFeatures.MEDIA_AUDIO;
                     int vol = converToAudioVolume(audioType, msg.arg1);

                     if (audioType == ApmConst.AudioFeatures.CALL_AUDIO) {
                         if(mCall.mDevice == null) {
                             Log.e (TAG, "setLeAudioVolume: No Device Active for Voice. Ignore");
                             return;
                         }
                         if (mPtsTest && vol == mCall.mVolume) {
                             Log.e (TAG, "Ignore set same volume to VCP for pts test");
                             return;
                         }
                        mCall.updateVolume(vol);
                        if(mMedia != null && mAcmService != null) {
                             Log.i (TAG, "setLeAudioVolume: Updating new volume to VCP: " + vol);
                             mAcmService.setAbsoluteVolume(mMedia.mDevice, vol, ApmConst.AudioFeatures.CALL_AUDIO);
                         }
                     } else {
                         if(mMedia == null) {
                             Log.e (TAG, "setLeAudioVolume: mMedia is null");
                             return;
                         }
                         if(mMedia.mDevice == null) {
                             Log.e (TAG, "setLeAudioVolume: No Device Active for Media. Ignore");
                             return;
                         }
                         if (mPtsTest && vol == mMedia.mVolume) {
                             Log.e (TAG, "Ignore set same volume to VCP for pts test");
                             return;
                         }
                         boolean isStreamMute = mMediaAudio.getAudioManager().isStreamMute(streamType);
                         boolean isVcpMute = getMuteStatus(mMedia.mDevice);
                         Log.d (TAG, "setLeAudioVolume: stream mute: " + isStreamMute + " vcp mute: " + isVcpMute);
                         if (isStreamMute && !isVcpMute && vol != 0) {
                             Log.w (TAG, "setLeAudioVolume: unmute stream if set non-zero LE volume");
                             int flag = AudioManager.FLAG_BLUETOOTH_ABS_VOLUME;
                             mMediaAudio.getAudioManager().adjustStreamVolume(streamType, AudioManager.ADJUST_UNMUTE, flag);
                         }

                         mMedia.updateVolume(vol);
                         if(mAcmService != null) {
                             Log.i (TAG, "setLeAudioVolume: Updating new volume to VCP: " + vol);
                             mAcmService.setAbsoluteVolume(mMedia.mDevice, vol, ApmConst.AudioFeatures.MEDIA_AUDIO);
                         }
                     }
                     break;
                 }
                 default:
                     break;
             }
        }
    }

    private DeviceVolume VolumeType(int mAudioType) {
        if(ApmConst.AudioFeatures.CALL_AUDIO == mAudioType) {
            return mCall;
        } else if(ApmConst.AudioFeatures.MEDIA_AUDIO == mAudioType) {
            return mMedia;
        } else if(ApmConst.AudioFeatures.BROADCAST_AUDIO == mAudioType) {
            return mBroadcast;
        }
        return null;
    }

    public int getConnectionMode(BluetoothDevice device) {
        AcmService mAcmService = AcmService.getAcmService();
        if(mAcmService == null) {
            return -1;
        }
        return mAcmService.getVcpConnMode(device);
    }

    public void setLeAudioVolume(Integer volume) {
        Log.e (TAG, "setLeAudioVolume:  " + volume);
        Message msg = mHandler.obtainMessage(MESSAGE_SET_LE_AUDIO_VOLUME, volume, 0);
        mHandler.sendMessage(msg);
    }

    public void setMediaAbsoluteVolume (Integer volume) {
        if(mMedia == null) {
            Log.e (TAG, "setMediaAbsoluteVolume: mMedia is null");
            return;
        }
        if(mMedia.mDevice == null) {
            Log.e (TAG, "setMediaAbsoluteVolume: No Device Active for Media. Ignore");
            return;
        }
        mMedia.updateVolume(volume);

        if(ApmConst.AudioProfiles.AVRCP == mMedia.mProfile) {
            Avrcp_ext mAvrcp = Avrcp_ext.get();
            if(mAvrcp != null) {
                Log.i (TAG, "setMediaAbsoluteVolume: Updating new volume to AVRCP: " + volume);
                mAvrcp.setAbsoluteVolume(volume);
            }
        } else if(ApmConst.AudioProfiles.VCP == mMedia.mProfile) {
            AcmService mAcmService = AcmService.getAcmService();
            if(mAcmService != null) {
                Log.i (TAG, "setMediaAbsoluteVolume: Updating new volume to VCP: " + volume);
                mMedia.updateVolume(volume);
                mAcmService.setAbsoluteVolume(mMedia.mDevice, volume, ApmConst.AudioFeatures.MEDIA_AUDIO);
            }
        }
    }

    public void updateMediaStreamVolume (Integer volume) {
        if(mMedia == null) {
            Log.e (TAG, "updateMediaStreamVolume: mMedia is null");
            return;
        }
        if(mMedia.mDevice == null) {
            Log.e (TAG, "updateMediaStreamVolume: No Device Active for Media. Ignore");
            return;
        }

        if(ApmConst.getQtiLeAudioEnabled() && mMedia.mSupportAbsoluteVolume) {
            /* Ignore: Will update volume via API call */
            return;
        }
        Log.d (TAG, "updateMediaStreamVolume: " + volume);
        mMedia.updateVolume(volume);
    }

    public void updateBroadcastVolume (BluetoothDevice device, int volume) {
        int callAudioState = mCallAudio.getAudioState(device);
        boolean isCall = (callAudioState == BluetoothHeadset.STATE_AUDIO_CONNECTING ||
                 callAudioState == BluetoothHeadset.STATE_AUDIO_CONNECTED);
        if (isCall && !mPtsTest) {
            Log.e(TAG, "Call in progress, ignore volume change");
            return;
        }

        mBroadcast.updateVolume(device, volume);
        AcmService mAcmService = AcmService.getAcmService();
        if (mAcmService != null) {
            BluetoothDevice mGroupDevice = mAcmService.getGroup(device);
            mAcmService.setAbsoluteVolume(mGroupDevice, volume, ApmConst.AudioFeatures.BROADCAST_AUDIO);
            mBroadcast.updateVolume(mGroupDevice, volume);
        }
    }

    public void setMute(BluetoothDevice device, boolean muteStatus) {
        AcmService mAcmService = AcmService.getAcmService();
        if (mAcmService != null) {
            BluetoothDevice mGroupDevice = mAcmService.getGroup(device);
            mAcmService.setMute(mGroupDevice, muteStatus);
        }
    }

    public void restoreCallVolume (Integer volume) {
        if(mCall.mDevice == null) {
            Log.e (TAG, "restoreCallVolume: No Device Active for Call. Ignore");
            return;
        }

        if(ApmConst.AudioProfiles.HFP == mCall.mProfile) {
            // Ignore restoring call volume for HFP case
            Log.w (TAG, "restoreCallVolume: Ignore restore call volume for HFP");
        } else if(ApmConst.AudioProfiles.VCP == mCall.mProfile) {
            AcmService mAcmService = AcmService.getAcmService();
            if(mAcmService != null) {
                Log.i (TAG, "restoreCallVolume: Updating new volume to VCP: " + volume);
                mCall.updateVolume(volume);
                mAcmService.setAbsoluteVolume(mCall.mDevice, volume, ApmConst.AudioFeatures.CALL_AUDIO);
            }

            if (!ApmConst.getQtiLeAudioEnabled()) {
                Log.w(TAG, "AOSP LE Call profile enabled, return ");
                return;
            }

            // Restore call volume to MM-Audio also
            Log.i (TAG, "mCallAudioStreamVolume: " + mCallAudioStreamVolume + " volume: " + volume);
            if (mCallAudioStreamVolume != volume) {
                Log.i (TAG, "restoreCallVolume: update restored volume to MM " + volume);
                if (mHandler.hasMessages(MESSAGE_SET_CALL_VOLUME_TO_AUDIO_TIMEOUT)) {
                    Log.i (TAG, "restoreCallVolume: MESSAGE_SET_CALL_VOLUME_TO_AUDIO_TIMEOUT is already in looper");
                } else {
                    /* Call Volume can be updated to MM-Audio successfully after audio has
                       applied voice stream to BT_SCO device. Need add an delay to ensure
                       updating call volume to Audio properly.
                    */
                    int delay = SystemProperties.getInt("persist.vendor.btstack.setcallvolume.delay",
                            SET_AUDIO_CALL_VOLUME_DELAY_DEFAULT);
                    Message msg = mHandler.obtainMessage(MESSAGE_SET_CALL_VOLUME_TO_AUDIO_TIMEOUT);
                    mHandler.sendMessageDelayed(msg, delay);
                }
            }
        }
    }

    public void setCallVolume (Intent intent) {
        if(mCall.mDevice == null) {
            Log.e (TAG, "setCallVolume: No Device Active for Call. Ignore");
            return;
        }

        int volume = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_VALUE, 0);
        if(ApmConst.AudioProfiles.HFP == mCall.mProfile) {
            Log.i (TAG, "setCallVolume: Updating new volume to HFP: " + volume);
            HeadsetService headsetService = HeadsetService.getHeadsetService();
            headsetService.setIntentScoVolume(intent);
        } else if(ApmConst.AudioProfiles.VCP == mCall.mProfile) {
            if (!ApmConst.getQtiLeAudioEnabled() &&
                    !(mCallAudio != null && mCallAudio.isVoipLeaWarEnabled())) {
                Log.w(TAG, "AOSP LE profile enabled, return ");
                return;
            }

            Log.i (TAG, "setCallVolume: mCall volume: " + mCall.mVolume + ", volume: " + volume);
            // Avoid updating same call volume after remote volume change
            if (volume == mCall.mVolume) {
                Log.w (TAG, "setCallVolume: Ignore updating same call volume to remote");
                return;
            }
            AcmService mAcmService = AcmService.getAcmService();
            if(mAcmService != null) {
                Log.i (TAG, "setCallVolume: Updating new volume to VCP: " + volume);
                mCall.updateVolume(volume);
                mAcmService.setAbsoluteVolume(mCall.mDevice, volume, ApmConst.AudioFeatures.CALL_AUDIO);
            }
        }
    }

    public int getConnectionState(BluetoothDevice device) {
        AcmService mAcmService = AcmService.getAcmService();
        if (mAcmService != null)
            return mAcmService.getVcpConnState(device);
        else
            return BluetoothProfile.STATE_DISCONNECTED;
    }

    public void onConnStateChange(BluetoothDevice device, Integer state, Integer profile) {
        Log.d (TAG, "onConnStateChange: state: " + state + " Profile: " + profile);
        if (device == null) {
            Log.e (TAG, "onConnStateChange: device is null. Ignore");
            return;
        }

        AcmService mAcmService = AcmService.getAcmService();
        BluetoothDevice mGroupDevice;
        if(mAcmService != null) {
            mGroupDevice = mAcmService.getGroup(device);
        } else {
            mGroupDevice = device;
        }

        if (state == BluetoothProfile.STATE_CONNECTED) {
            if (profile == ApmConst.AudioProfiles.HFP)
                dpm.profileConnectionUpdate(mGroupDevice, ApmConst.AudioFeatures.CALL_VOLUME_CONTROL, profile, true);
            else if (profile == ApmConst.AudioProfiles.AVRCP)
                dpm.profileConnectionUpdate(mGroupDevice, ApmConst.AudioFeatures.MEDIA_VOLUME_CONTROL, profile, true);
        } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
            if (profile == ApmConst.AudioProfiles.HFP)
                dpm.profileConnectionUpdate(mGroupDevice, ApmConst.AudioFeatures.CALL_VOLUME_CONTROL, profile, false);
            else if (profile == ApmConst.AudioProfiles.AVRCP)
                dpm.profileConnectionUpdate(mGroupDevice, ApmConst.AudioFeatures.MEDIA_VOLUME_CONTROL, profile, false);
        }

        if (mMedia != null && mGroupDevice.equals(mMedia.mDevice)) {
            mMedia.mProfile =
                    dpm.getProfile(mGroupDevice, ApmConst.AudioFeatures.MEDIA_VOLUME_CONTROL);
        }
        if (mGroupDevice.equals(mCall.mDevice)) {
            mCall.mProfile =
                    dpm.getProfile(mGroupDevice, ApmConst.AudioFeatures.CALL_VOLUME_CONTROL);
        }

        if (mPtsTest) {
            Log.w(TAG, "Skip volume restore for pts test: " + device);
            return;
        }

        if (state == BluetoothProfile.STATE_CONNECTED) {
            int audioType = getActiveAudioType(device);
            if (mMedia != null && ApmConst.AudioFeatures.MEDIA_AUDIO == audioType && mMedia.mProfile == profile) {
                Log.d (TAG, "onConnStateChange: Media is streaming or active, update media volume");
                setMediaAbsoluteVolume(mMedia.mVolume);
            } else if (ApmConst.AudioFeatures.CALL_AUDIO == audioType &&
                    mCall.mProfile == profile) {
                Log.d (TAG, "onConnStateChange: Call is streaming, update call volume");
                restoreCallVolume(mCall.mVolume);
            } else if (ApmConst.AudioFeatures.BROADCAST_AUDIO == audioType) {
                Log.d (TAG, "onConnStateChange: Broadcast is streaming, update broadcast volume");
                if (ApmConst.getQtiLeAudioEnabled()) {
                    updateBroadcastVolume(device, getBassVolume(device));
                } else {
                    Log.d(TAG, "No broadcast volume restore for AOSP LE Audio");
                }
            }
        }
    }

    int converToAudioVolume(int audioType, int vcpVolume) {
        int audioMaxVolume = (audioType == ApmConst.AudioFeatures.CALL_AUDIO)
                ? mCallAudioStreamMax : mMediaAudioStreamMax;
        int audioMinVolume = 0;

        int audioVolume = (int) Math.floor(
                (double) vcpVolume * (audioMaxVolume - audioMinVolume) / VCP_MAX_VOL);
        Log.d (TAG, "convertToAudioVolume: " + audioVolume + " audioType " + audioType);
        return audioVolume;
    }

    public int getBluetoothContextualVolumeStream() {
        switch (mAudioMode) {
            case AudioManager.MODE_IN_COMMUNICATION:
            case AudioManager.MODE_IN_CALL:
                Log.d(TAG, "getBluetoothContextualVolumeStream return voice");
                return AudioManager.STREAM_VOICE_CALL;
            case AudioManager.MODE_NORMAL:
            default:
                // other conditions will influence the stream type choice, read on...
                break;
        }
        Log.d(TAG, "getBluetoothContextualVolumeStream return media");
        return AudioManager.STREAM_MUSIC;
    }

    public int getAudioStreamMaxVolume(int audioType) {
        int audioMaxVolume = (audioType == ApmConst.AudioFeatures.CALL_AUDIO)
                ? mCallAudioStreamMax : mMediaAudioStreamMax;
        return audioMaxVolume;
    }

    public void onVolumeChange(Integer volume, Integer audioType, Boolean showUI) {
        int flag = showUI ? AudioManager.FLAG_SHOW_UI : 0;
        if (ApmConst.getQtiLeAudioEnabled()) {
            if(audioType == ApmConst.AudioFeatures.CALL_AUDIO){
                mCall.updateVolume(volume);
                mCallAudio.getAudioManager().setStreamVolume(AudioManager.STREAM_BLUETOOTH_SCO,
                        volume, flag);
            } else if(audioType == ApmConst.AudioFeatures.MEDIA_AUDIO) {
                if(mMedia != null)
                    mMedia.updateVolume(volume);
                mMediaAudio.getAudioManager().setStreamVolume(AudioManager.STREAM_MUSIC, volume,
                        flag | AudioManager.FLAG_BLUETOOTH_ABS_VOLUME);
            }
        } else {
            if(audioType == ApmConst.AudioFeatures.CALL_AUDIO){
                mCall.updateVolume(volume);
                mCallAudio.getAudioManager().setStreamVolume(AudioManager.STREAM_VOICE_CALL,
                        volume, flag | AudioManager.FLAG_BLUETOOTH_ABS_VOLUME);
            } else if(audioType == ApmConst.AudioFeatures.MEDIA_AUDIO) {
                if(mMedia != null)
                    mMedia.updateVolume(volume);
                mMediaAudio.getAudioManager().setStreamVolume(AudioManager.STREAM_MUSIC,
                        volume, flag | AudioManager.FLAG_BLUETOOTH_ABS_VOLUME);
            }
        }
        Log.d (TAG, "onVolumeChange: " + volume + " audioType " + audioType + " showUI " + showUI);
    }

    public void onVolumeChange(BluetoothDevice device, Integer volume, Integer audioType) {
        if ((VolumeType(audioType) == mCall && device.equals(mCall.mDevice)) ||
            (VolumeType(audioType) == mMedia && mMedia != null && device.equals(mMedia.mDevice))) {
            onVolumeChange(volume, audioType, true);
        } else {
            mBroadcast.updateVolume(device, volume);
        }
    }

    public void onMuteStatusChange(BluetoothDevice device, boolean isMute, int audioType) {
    }


    public void onActiveDeviceChange(BluetoothDevice device, int audioType) {
        if(device == null) {
            synchronized(mVolumeManager) {
                if(VolumeType(audioType) != null)
                    VolumeType(audioType).reset();
            }
        } else {
            int mProfile = dpm.getProfile(device, audioType == ApmConst.AudioFeatures.CALL_AUDIO?
                    ApmConst.AudioFeatures.CALL_VOLUME_CONTROL:ApmConst.AudioFeatures.MEDIA_VOLUME_CONTROL);
            DeviceVolume mDeviceVolume = VolumeType(audioType);
            Log.i(TAG, "ActiveDeviceChange: device: " + mDeviceVolume.mDevice + ". AudioType: " + audioType);
            AcmService mAcmService = AcmService.getAcmService();
            BluetoothDevice mGroupDevice;
            if(mAcmService != null) {
                mGroupDevice = mAcmService.getGroup(device);
            } else {
                mGroupDevice = device;
            }
            BluetoothDevice prevActiveDevice = mDeviceVolume.mDevice;
            mDeviceVolume.updateDevice(mGroupDevice, mProfile);
            if(mDeviceVolume.equals(mMedia)) {
                int mAbsVolSupportProfiles = AbsVolumeSupport.getOrDefault(mGroupDevice.getAddress(), 0);
                boolean isAbsSupported = (mAbsVolSupportProfiles != 0) ? true : false;
                Log.i(TAG, "isAbsoluteVolumeSupport:  " + isAbsSupported);
                mDeviceVolume.mSupportAbsoluteVolume = isAbsSupported;

                boolean isPrevA2dp = prevActiveDevice != null &&
                        dpm.getProfile(prevActiveDevice, ApmConst.AudioFeatures.MEDIA_AUDIO) == ApmConst.AudioProfiles.A2DP;
                Log.i(TAG, "onActiveDeviceChange: prevActiveDevice=" + prevActiveDevice +
                        ", isPrevA2dp=" + isPrevA2dp);

                ActiveDeviceManagerService activeDeviceManager =
                        ActiveDeviceManagerService.get(mContext);
                if (ApmConst.getQtiLeAudioEnabled() || isPrevA2dp &&
                        activeDeviceManager.getActiveProfile(ApmConst.AudioFeatures.MEDIA_AUDIO) ==
                        ApmConst.AudioProfiles.A2DP) {
                    mMediaAudio.getAudioManager().setDeviceVolumeBehavior(new AudioDeviceAttributes(
                            AudioDeviceAttributes.ROLE_OUTPUT, AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                            device.getAddress()),
                            isAbsSupported ? AudioManager.DEVICE_VOLUME_BEHAVIOR_ABSOLUTE :
                            AudioManager.DEVICE_VOLUME_BEHAVIOR_VARIABLE);
                } else {
                    Log.i(TAG, "Not set absVolumeSupported for BLE device in AOSP LeAudio or set in AudioDeviceChangedCallback");
                }

                Log.i(TAG, "ActiveDeviceChange: Profile: " + mProfile + ". New Volume: " + mDeviceVolume.mVolume);
                if (mPtsTest) {
                    Log.w(TAG, "Skip volume restore for pts test: " + device);
                    return;
                }

                if (ApmConst.getQtiLeAudioEnabled()) {
                    if (!isBroadcastAudioSynced(device) ||
                        (mMediaAudio.isA2dpPlaying(device) && mMediaAudio.getAudioManager().isMusicActive())) {
                        setMediaAbsoluteVolume(mDeviceVolume.mVolume);
                    }
                } else {
                    Log.i(TAG, "Not restore volume for BLE device in AOSP LeAudio");
                }
            }
        }
    }

    public void updateStreamState(BluetoothDevice device, Integer streamState, Integer audioType) {
        boolean isMusicActive = false;
        if (device == null) {
            Log.e (TAG, "updateStreamState: device is null. Ignore");
            return;
        }
        if (audioType == ApmConst.AudioFeatures.MEDIA_AUDIO &&
            streamState == BluetoothA2dp.STATE_PLAYING) {
            isMusicActive = mMediaAudio.getAudioManager().isMusicActive();
        }
        Log.d(TAG, "updateStreamState, device: " + device + " type: " + audioType
                + " streamState: " + streamState + " isMusicActive: " + isMusicActive);

        AcmService mAcmService = AcmService.getAcmService();
        BluetoothDevice mGroupDevice;
        if(mAcmService != null) {
            mGroupDevice = mAcmService.getGroup(device);
        } else {
            mGroupDevice = device;
        }

        if (mPtsTest) {
            Log.w(TAG, "Skip volume restore for pts test: " + device);
            return;
        }

        if ((audioType == ApmConst.AudioFeatures.MEDIA_AUDIO &&
                streamState == BluetoothA2dp.STATE_NOT_PLAYING) ||
                (audioType == ApmConst.AudioFeatures.CALL_AUDIO &&
                streamState == BluetoothHeadset.STATE_AUDIO_DISCONNECTED)) {
            if (isBroadcastAudioSynced(device)) {
                if (ApmConst.getQtiLeAudioEnabled()) {
                    handleBroadcastAudioSynced(device);
                } else {
                    Log.d(TAG, "No broadcast volume restore for AOSP LE Audio");
                }
            }
        } else if (audioType == ApmConst.AudioFeatures.MEDIA_AUDIO &&
                streamState == BluetoothA2dp.STATE_PLAYING && isMusicActive) {
            if (mMedia != null && mGroupDevice.equals(mMedia.mDevice)) {
                Log.d(TAG, "Restore volume for A2dp streaming");
                setMediaAbsoluteVolume(mMedia.mVolume);
            }
        } else if (audioType == ApmConst.AudioFeatures.CALL_AUDIO &&
                streamState == BluetoothHeadset.STATE_AUDIO_CONNECTED) {
            if (mGroupDevice.equals(mCall.mDevice)) {
                Log.d(TAG, "Restore volume for call");
                restoreCallVolume(mCall.mVolume);
            }
        }
    }

    public int getActiveAudioType(BluetoothDevice device) {
        if (!ApmConst.getQtiLeAudioEnabled()) {
            int streamType = getBluetoothContextualVolumeStream();
            if (streamType == AudioManager.STREAM_VOICE_CALL) {
                return ApmConst.AudioFeatures.CALL_AUDIO;
            }
            return ApmConst.AudioFeatures.MEDIA_AUDIO;
        }

        int callAudioState = mCallAudio.getAudioState(device);
        boolean isCall = (callAudioState == BluetoothHeadset.STATE_AUDIO_CONNECTING ||
                 callAudioState == BluetoothHeadset.STATE_AUDIO_CONNECTED);
        int audioType = -1;

        if (device == null) {
            Log.e (TAG, "getActiveAudioType: device is null. Ignore");
            return audioType;
        }

        AcmService mAcmService = AcmService.getAcmService();
        BluetoothDevice mGroupDevice;
        if(mAcmService != null) {
            mGroupDevice = mAcmService.getGroup(device);
        } else {
            mGroupDevice = device;
        }

        if (mMediaAudio.isA2dpPlaying(device) &&
                mMediaAudio.getAudioManager().isMusicActive()) {
            if (mMedia != null && mGroupDevice.equals(mMedia.mDevice)) {
                Log.d(TAG, "Active Media audio is streaming");
                audioType = ApmConst.AudioFeatures.MEDIA_AUDIO;
            }
        } else if (isCall) {
            if (mGroupDevice.equals(mCall.mDevice)) {
                Log.d(TAG, "Active Call audio is streaming");
                audioType = ApmConst.AudioFeatures.CALL_AUDIO;
            }
        } else if (isBroadcastAudioSynced(device)) {
            Log.d(TAG, "Broadcast audio is streaming");
            audioType = ApmConst.AudioFeatures.BROADCAST_AUDIO;
        } else {
            Log.d(TAG, "None of audio is streaming");
            ActiveDeviceManagerService activeDeviceManager =
                    ActiveDeviceManagerService.get(mContext);
            BluetoothDevice activeDevice =
                    activeDeviceManager.getActiveDevice(ApmConst.AudioFeatures.MEDIA_AUDIO);
            if (mMedia != null && mGroupDevice.equals(mMedia.mDevice) && mGroupDevice.equals(activeDevice)) {
                Log.d(TAG, "Peer is Media active, set for media type by default");
                audioType = ApmConst.AudioFeatures.MEDIA_AUDIO;
            } else {
                Log.d(TAG, "Inactive peer, unknow audio type");
            }
        }

        Log.d(TAG, "getActiveAudioType: ret " + audioType);
        return audioType;
    }

    /*Should be called by AVRCP and VCP after every connection*/
    public void setAbsoluteVolumeSupport(BluetoothDevice device, Boolean isSupported,
            Integer initVol, Integer profile) {
        setAbsoluteVolumeSupport(device, isSupported, profile);
    }

    public void setAbsoluteVolumeSupport(BluetoothDevice device, Boolean isSupported,
            Integer profile) {
        Log.i(TAG, "setAbsoluteVolumeSupport device " + device + " profile " + profile
                + " isSupported " + isSupported);
        if(device == null)
            return;

        AcmService mAcmService = AcmService.getAcmService();
        BluetoothDevice mGroupDevice;
        if(mAcmService != null) {
            mGroupDevice = mAcmService.getGroup(device);
        } else {
            mGroupDevice = device;
        }

        int mProfile = dpm.getProfile(device, ApmConst.AudioFeatures.MEDIA_VOLUME_CONTROL);
        int mAbsVolSupportProfiles = AbsVolumeSupport.getOrDefault(mGroupDevice.getAddress(), 0);
        if (isSupported) {
            mAbsVolSupportProfiles = mAbsVolSupportProfiles | profile;
        } else {
            mAbsVolSupportProfiles = mAbsVolSupportProfiles & ~profile;
        }

        if(mMedia != null && mGroupDevice.equals(mMedia.mDevice)) {
            boolean isAbsSupported = (mAbsVolSupportProfiles != 0) ? true : false;
            Log.i(TAG, "Update abs volume support:  " + isAbsSupported);
            mMedia.mSupportAbsoluteVolume = isAbsSupported;

            ActiveDeviceManagerService activeDeviceManager =
                    ActiveDeviceManagerService.get(mContext);
            if (ApmConst.getQtiLeAudioEnabled() ||
                    activeDeviceManager.getActiveProfile(ApmConst.AudioFeatures.MEDIA_AUDIO)
                    == ApmConst.AudioProfiles.A2DP) {
                mMediaAudio.getAudioManager().setDeviceVolumeBehavior(new AudioDeviceAttributes(
                        AudioDeviceAttributes.ROLE_OUTPUT, AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                        device.getAddress()),
                        isAbsSupported ? AudioManager.DEVICE_VOLUME_BEHAVIOR_ABSOLUTE
                        : AudioManager.DEVICE_VOLUME_BEHAVIOR_VARIABLE);
            } else {
                Log.i(TAG, "Not set absVolumeSupported for BLE device in AOSP LeAudio");
            }

            if(mMedia.mProfile == ApmConst.AudioProfiles.NONE) {
                mMedia.mProfile = mProfile;
                Log.i(TAG, "setAbsoluteVolumeSupport: Profile: " + mMedia.mProfile);
            }
        }
        Log.i(TAG, "Update AbsVolumeSupport map for device: " + mGroupDevice);
        AbsVolumeSupport.put(mGroupDevice.getAddress(), mAbsVolSupportProfiles);
    }

    public void saveVolume(Integer audioType) {
        DeviceVolume mDeviceVolume = VolumeType(audioType);
        Log.i(TAG, "saveVolume(): mDeviceVolume: " + mDeviceVolume);
        if (mDeviceVolume != null) {
            mDeviceVolume.saveVolume();
        }
    }

    public int getSavedVolume(BluetoothDevice device, Integer audioType) {
        AcmService mAcmService = AcmService.getAcmService();
        BluetoothDevice mGroupDevice;
        if(mAcmService != null) {
            mGroupDevice = mAcmService.getGroup(device);
        } else {
            mGroupDevice = device;
        }

        DeviceVolume mDeviceVolume = VolumeType(audioType);
        Log.i(TAG, "getSavedVolume(): mDeviceVolume: " + mDeviceVolume);
        int volume = -1;
        if (mDeviceVolume != null) {
            volume = mDeviceVolume.getSavedVolume(mGroupDevice);
        }
        Log.i(TAG, "getSavedVolume: " + mGroupDevice + " volume: " + volume);
        return volume;
    }

    public int getActiveVolume(Integer audioType) {
        DeviceVolume mDeviceVolume = VolumeType(audioType);
        Log.i(TAG, "getActiveVolume(): mDeviceVolume: " + mDeviceVolume);
        if (mDeviceVolume != null) {
            return mDeviceVolume.mVolume;
        }
        return -1;
    }

    public int getBassVolume(BluetoothDevice device) {
        AcmService mAcmService = AcmService.getAcmService();
        BluetoothDevice mGroupDevice;
        if(mAcmService != null) {
            mGroupDevice = mAcmService.getGroup(device);
        } else {
            mGroupDevice = device;
        }
        int volume = mBroadcast.getVolume(mGroupDevice);
        Log.i(TAG, "getBassVolume: " + device + " volume: " + volume);
        return volume;
    }

    public boolean getMuteStatus(BluetoothDevice device) {
        AcmService mAcmService = AcmService.getAcmService();
        if(mAcmService == null) {
            return false;
        }
        return mAcmService.isVcpMute(device);
    }

    boolean isBroadcastAudioSynced(BluetoothDevice device) {
        BCService mBCService = BCService.getBCService();
        if (mBCService == null || device == null) return false;
        List<BleBroadcastSourceInfo> srcInfos =
                mBCService.getAllBroadcastSourceInformation(device);
        if (srcInfos == null || srcInfos.size() == 0) {
            Log.e(TAG, "source Infos not available");
            return false;
        }

        for (int i=0; i<srcInfos.size(); i++) {
            if (srcInfos.get(i).getAudioSyncState() ==
                    BleBroadcastSourceInfo.BROADCAST_ASSIST_AUDIO_SYNC_STATE_SYNCHRONIZED) {
                Log.d(TAG, "Remote synced audio to broadcast source");
                return true;
            }
        }
        return false;
    }

    void handleBroadcastAudioSynced(BluetoothDevice device) {
        if (device == null) {
            return;
        }

        AcmService mAcmService = AcmService.getAcmService();
        BluetoothDevice mGroupDevice;
        if(mAcmService != null) {
            mGroupDevice = mAcmService.getGroup(device);
        } else {
            mGroupDevice = device;
        }

        int callAudioState = mCallAudio.getAudioState(device);
        boolean isCall = (callAudioState == BluetoothHeadset.STATE_AUDIO_CONNECTING ||
                callAudioState == BluetoothHeadset.STATE_AUDIO_CONNECTED);

        if (mMedia != null && mGroupDevice.equals(mMedia.mDevice) && mMediaAudio.isA2dpPlaying(device)) {
            Log.d (TAG, "Active media device and streaming, not restore broadcast volume");
        } else if (mGroupDevice.equals(mCall.mDevice) && isCall) {
            Log.d (TAG, "Active call device and in call, not restore broadcast volume");
        } else {
            Log.d (TAG, "Restore broadcast volume while remote synced audio");
            updateBroadcastVolume(device, getBassVolume(device));
        }
    }

    public void handleDeviceUnbond(BluetoothDevice device) {
        if (device == null) {
            Log.i(TAG, "handleDeviceUnbond for null");
            return;
        }
        Log.i(TAG, "handleDeviceUnbond for device: " + device);
        if(device.equals(mCall.mDevice)) {
            mCall.reset();
        }
        if(mMedia != null && device.equals(mMedia.mDevice)) {
            mMedia.reset();
        }

        mCall.removeDevice(device);
        if(mMedia != null)
            mMedia.removeDevice(device);
        mBroadcast.removeDevice(device);
    }

    void handleDeviceShutdown() {
        Log.i(TAG, "handleDeviceShutdown Save Volume start");
        if(mCall.mDevice != null) {
            mCall.saveVolume();
            mCall.reset();
        }
        if(mMedia != null && mMedia.mDevice != null) {
            mMedia.saveVolume();
            mMedia.reset();
        }
        mBroadcast.saveVolume();
        Log.i(TAG, "handleDeviceShutdown Save Volume end");
    }

    class DeviceVolume {
        BluetoothDevice mDevice;
        int mVolume;
        int mProfile;
        boolean mSupportAbsoluteVolume;
        Map<String, Integer> mBassVolMap;

        Context mContext;
        private String mAudioTypeStr;
        public String mVolumeMap;
        private int mSafeVol;

        DeviceVolume(Context context, String map) {
            this.reset();
            mContext = context;
            mVolumeMap = map;
            mSupportAbsoluteVolume = false;

            if(map == "bluetooth_call_volume_map") {
                 mAudioTypeStr = "Call";
            }
            else if(map == "bluetooth_media_volume_map") {
                mAudioTypeStr = "Media";
            }
            else {
                mAudioTypeStr = "Broadcast";
                mBassVolMap = new ConcurrentHashMap<String, Integer>();
            }

            Map<String, ?> allKeys = getVolumeMap().getAll();
            SharedPreferences.Editor pref = getVolumeMap().edit();
            for (Map.Entry<String, ?> entry : allKeys.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                BluetoothDevice d = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(key);

                if (d.getAddress().contains(ApmConst.groupAddress)) {
                    Log.d(TAG, "Volume map with group address");
                    byte[] addrByte = Utils.getByteAddress(d);
                    int set_id = addrByte[5];
                    CsipWrapper csipWrapper = CsipWrapper.getInstance();
                    DeviceGroup cSet = csipWrapper.getCoordinatedSet(set_id);
                    if (cSet != null) {
                        if (value instanceof Integer &&
                                cSet != null && cSet.getDeviceGroupMembers().size() != 0) {
                            if (mAudioTypeStr.equals("Broadcast")) {
                                mBassVolMap.put(key, (Integer) value);
                                Log.w(TAG, "address " + key + " from the broadcast volume map volume :" + value);
                            }
                        } else {
                            Log.w(TAG, "Removing " + key + " from the " + mAudioTypeStr + " volume map");
                            pref.remove(key);
                        }
                    } else {
                        Log.d(TAG, "Skip if cSet is null");
                    }
                } else {
                    Log.d(TAG, "Volume map with public address");
                    if (value instanceof Integer && mAdapterService.getBondState(d) == BluetoothDevice.BOND_BONDED) {
                        if (mAudioTypeStr.equals("Broadcast")) {
                            mBassVolMap.put(key, (Integer) value);
                            Log.w(TAG, "address " + key + " from the broadcast volume map volume :" + value);
                        }
                    } else {
                        Log.w(TAG, "Removing " + key + " from the " + mAudioTypeStr + " volume map");
                        pref.remove(key);
                    }
                }
            }
            pref.apply();
        }

        void setSafeVolume(int safeVol) {
            Log.i (TAG, "Safe volume " + safeVol + " for Audio Type " + mAudioTypeStr);
            mSafeVol = safeVol;
        }

        void updateDevice (BluetoothDevice device, int profile) {
            mDevice = device;
            mProfile = profile;

            mVolume = getSavedVolume(device);
            Log.i (TAG, "New " + mAudioTypeStr + " device: " + mDevice + " Vol: " + mVolume);
        }

        int getSavedVolume (BluetoothDevice device) {
            int mSavedVolume;
            SharedPreferences pref = getVolumeMap();
            mSavedVolume = pref.getInt(device.getAddress(), mSafeVol);
            return mSavedVolume;
        }

        void updateVolume (int volume) {
            mVolume = volume;
        }

        void updateVolume (BluetoothDevice device, int volume) {
            if(mAudioTypeStr.equals("Broadcast")) {
                Log.i(TAG, "updateVolume, device " + device + " volume: " + volume);
                mBassVolMap.put(device.getAddress(), volume);
            }
        }
        int getVolume(BluetoothDevice device) {
            if(device == null) {
                Log.e (TAG, "Null Device passed");
                return mSafeVol;
            }
            if(mAudioTypeStr.equals("Broadcast")) {
                if(mBassVolMap.containsKey(device.getAddress())) {
                    return mBassVolMap.getOrDefault(device.getAddress(), mSafeVol);
                } else {
                    int mSavedVolume = getSavedVolume(device);
                    mBassVolMap.put(device.getAddress(), mSavedVolume);
                    Log.i(TAG, "get saved volume, device " + device + " volume: " + mSavedVolume);
                    return mSavedVolume;
                }
            }
            return mSafeVol;
        }
        private SharedPreferences getVolumeMap() {
            return mContext.getSharedPreferences(mVolumeMap, Context.MODE_PRIVATE);
        }

        public synchronized void saveVolume() {
            if(mAudioTypeStr.equals("Broadcast")) {
                saveBroadcastVolume();
                return;
            }

            if(mDevice == null) {
                Log.e (TAG, "saveVolume: No Device Active for " + mAudioTypeStr + ". Ignore");
                return;
            }

            SharedPreferences.Editor pref = getVolumeMap().edit();
            pref.putInt(mDevice.getAddress(), mVolume);
            pref.apply();
            Log.i (TAG, "Saved " + mAudioTypeStr + " Volume: " + mVolume + " for device: " + mDevice);
        }

        public void saveBroadcastVolume() {
            SharedPreferences.Editor pref = getVolumeMap().edit();
            for(Map.Entry<String, Integer> itr : mBassVolMap.entrySet()) {
                pref.putInt(itr.getKey(), itr.getValue());
            }
            pref.apply();
        }

        public void saveVolume(BluetoothDevice device) {
            if(device == null) {
                Log.e (TAG, "Null Device passed");
                return;
            }
            if(mAudioTypeStr.equals("Broadcast")) {
                int mVol = mBassVolMap.getOrDefault(device.getAddress(), mSafeVol);
                SharedPreferences.Editor pref = getVolumeMap().edit();
                pref.putInt(device.getAddress(), mVol);
                pref.apply();
            }
        }

        void removeDevice(BluetoothDevice device) {
            if(mAudioTypeStr.equals("Broadcast")) {
                Log.i (TAG, "Remove device " + device + " from broadcast volume map ");
                mBassVolMap.remove(device.getAddress());
            }
            SharedPreferences.Editor pref = getVolumeMap().edit();
            pref.remove(device.getAddress());
            pref.apply();
        }

        synchronized void reset () {
            Log.i (TAG, "Reset " + mAudioTypeStr + " Device: " + mDevice);
            mDevice = null;
            mVolume = mSafeVol;
            mProfile = ApmConst.AudioProfiles.NONE;
            mSupportAbsoluteVolume = false;
        }
    }

    private class VolumeManagerReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action == null)
                return;

            switch(action) {
                case AudioManager.VOLUME_CHANGED_ACTION:
                    int streamType = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_TYPE, -1);
                    int volumeValue = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_VALUE, 0);

                    if(streamType == AudioManager.STREAM_BLUETOOTH_SCO) {
                        Log.d(TAG, "Call volume changed to " + volumeValue);
                        mCallAudioStreamVolume = volumeValue;
                        if(mHandler.hasMessages(MESSAGE_SET_CALL_VOLUME_TO_AUDIO_TIMEOUT)) {
                            Log.d(TAG, "Remove MESSAGE_SET_CALL_VOLUME_TO_AUDIO_TIMEOUT if call volume changed by user");
                            mHandler.removeMessages(MESSAGE_SET_CALL_VOLUME_TO_AUDIO_TIMEOUT);
                        }
                        setCallVolume(intent);
                    } else if (streamType == AudioManager.STREAM_MUSIC) {
                        Log.d(TAG, "Media volume changed to " + volumeValue);
                        updateMediaStreamVolume(volumeValue);
                    }
                    break;

                case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                    int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
                            BluetoothDevice.ERROR);
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if(device == null)
                        return;

                    Log.d(TAG, "Bond state changed for device: " + device + " state: " + state);
                    if (state == BluetoothDevice.BOND_NONE) {
                        DeviceProfileMap dMap = DeviceProfileMap.getDeviceProfileMapInstance();
                        if (dMap != null) {
                            dMap.handleDeviceUnbond(device);
                        }
                        handleDeviceUnbond(device);
                    }
                    break;

                case BleBroadcastAudioScanAssistManager.ACTION_BROADCAST_SOURCE_INFO:
                    BleBroadcastSourceInfo sourceInfo = intent.getParcelableExtra(
                                      BleBroadcastSourceInfo.EXTRA_SOURCE_INFO);
                    device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    if (device == null || sourceInfo == null) {
                        Log.w (TAG, "Bluetooth Device or Source info is null");
                        break;
                    }

                    if (sourceInfo.getAudioSyncState() ==
                            BleBroadcastSourceInfo.BROADCAST_ASSIST_AUDIO_SYNC_STATE_SYNCHRONIZED) {
                        if (ApmConst.getQtiLeAudioEnabled()) {
                            handleBroadcastAudioSynced(device);
                        } else {
                            Log.d(TAG, "No broadcast volume restore for AOSP LE Audio");
                        }
                    }
                    break;

                case ACTION_SHUTDOWN:
                case ACTION_POWER_OFF:
                    handleDeviceShutdown();
                    break;
            }
        }
    }
}
