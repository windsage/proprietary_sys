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

import static com.android.bluetooth.Utils.enforceBluetoothPermission;
import static com.android.bluetooth.Utils.enforceBluetoothPrivilegedPermission;
import static android.Manifest.permission.BLUETOOTH_CONNECT;

import android.bluetooth.BluetoothLeAudio;
import android.bluetooth.BluetoothCodecConfig;
import android.bluetooth.BluetoothCodecStatus;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.IBluetoothVcp;

import android.bluetooth.BluetoothLeAudioCodecConfig;
import android.bluetooth.BluetoothLeAudioCodecStatus;

import android.content.Intent;
import android.content.AttributionSource;
import android.os.Binder;
import android.os.HandlerThread;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Log;

import com.android.bluetooth.a2dp.A2dpService;
import com.android.bluetooth.btservice.AdapterService;
import com.android.bluetooth.btservice.ProfileService;
import com.android.bluetooth.acm.AcmService;
import com.android.bluetooth.Utils;
import com.android.bluetooth.apm.MediaAudio;
import com.android.bluetooth.apm.CallAudio;
import com.android.bluetooth.apm.MediaAudio.MediaDevice;
import com.android.bluetooth.le_audio.LeAudioService;
import com.android.bluetooth.btservice.ActiveDeviceManager;
<<<<<<< HEAD   (ee9fe1 Merge "ACM: disconnect VCP until ACM get disconnected in QTI)
=======
import com.android.bluetooth.hfp.HeadsetService;
import com.android.bluetooth.cc.CCService;
>>>>>>> CHANGE (719141 CC: Handling of In-band ringtone for CSIP devices(2/2).)

import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class StreamAudioService extends ProfileService {
    private static final boolean DBG = true;
    private static final String TAG = "APM: StreamAudioService:";
    public static final int LE_AUDIO_UNICAST = BluetoothProfile.VCP;

    public static final String CoordinatedAudioServiceName = "com.android.bluetooth.acm.AcmService";
    public static final int COORDINATED_AUDIO_UNICAST = AcmService.ACM_AUDIO_UNICAST;

    private static StreamAudioService sStreamAudioService;
    private ActiveDeviceManagerService mActiveDeviceManager;
    private LeAudioService mLeAudioService;

    private MediaAudio mMediaAudio;
    private CallAudio mCallAudio;
    private VolumeManager mVolumeManager;
    private ApmNativeInterface mApmNative;
    private final Object mVolumeManagerLock = new Object();
    @Override
    protected void create() {
        Log.i(TAG, "create()");
    }

    private static final int BAP       = 0x01;
    private static final int GCP       = 0x02;
    private static final int WMCP      = 0x04;
    private static final int VMCP      = 0x08;
    private static final int BAP_CALL  = 0x10;
    private static final int GCP_VBC   = 0x20;
    private static final int HAP_LE    = 0x30;

    private static final int MEDIA_CONTEXT = 1;
    private static final int VOICE_CONTEXT = 2;

    private static final int METADATA_UNSPECIFIED    = 0x0000;
    private static final int METADATA_CONVERSATIONAL = 0x0002;
    private static final int METADATA_MEDIA          = 0x0004;
    private static final int METADATA_GAME           = 0x0008;
    private static final int METADATA_INSTRUCTIONAL  = 0x0010;
    private static final int METADATA_LIVE           = 0x0040;
    private static final int METADATA_SOUNDEFFECTS   = 0x0080;
    private static final int METADATA_NOTIFICATIONS  = 0x0100;
    private static final int METADATA_RINGTONE       = 0x0200;
    private static final int METADATA_ALERTS         = 0x0400;
    private static final int METADATA_EMERGENCYALARM = 0x0800;

    private final Map<BluetoothDevice, Integer> mPrevStateMap = new HashMap<>();
    public boolean isLeStateBroadcasted = false;
    //private boolean isSetLeMedia = true;

    private final Map<BluetoothDevice, Integer> mLeProfilesStableStateMap = new HashMap<>();
    private int LE_V_PROF_CONNECTED = 0x1;
    private int LE_M_PROF_CONNECTED = 0x2;
    private int LE_VM_PROF_CONNECTED = 0x3;
    private int LE_VM_PROF_DISCONNECTED = 0x0;

    private final Map<BluetoothDevice, Integer> mLeProfilesInterStateMap = new HashMap<>();
    private int LE_V_PROF_CONNECTING = 0x1;
    private int LE_M_PROF_CONNECTING = 0x2;
    private int LE_VM_PROF_CONNECTING = 0x3;
    private int LE_VM_PROF_DISCONNECTING = 0x0;

    private static int PREFE_PROF_NONE       = 0;
    private static int PREFE_PROF_TMAP_MEDIA = 1;
    private static int PREFE_PROF_TMAP_CALL  = 2;
    private boolean mIsSasPtsEnabled = false;
    private boolean mIsPtsAc9iEnabled = false;
    private int mCurrentContextType = 0;
    private boolean mLowLatencyMode = false;
    private boolean isDualModeSupported = false;
    private boolean isDualModeEnabled = false;
    private static final String DUAL_MODE_TRANSPORT_AVAILABLE =
              "persist.vendor.qcom.bluetooth.dualmode_transport_support";
    private static final String PREFER_LE_AUDIO_ONLY_MODE =
              "persist.bluetooth.prefer_le_audio_only_mode";

    @Override
    protected boolean start() {
        if(sStreamAudioService != null) {
            Log.i(TAG, "StreamAudioService already started");
            return true;
        }
        Log.i(TAG, "start()");

        mActiveDeviceManager = ActiveDeviceManagerService.get(this);
        ApmConst.setQtiLeAudioEnabled(mActiveDeviceManager.isQcLeaEnabled());
        ApmConst.setAospLeaEnabled(mActiveDeviceManager.isAospLeaEnabled());

        ApmConstIntf.init();

        setStreamAudioService(this);

        mMediaAudio = MediaAudio.init(this);
        mCallAudio = CallAudio.init(this);

        DeviceProfileMap dpm = DeviceProfileMap.getDeviceProfileMapInstance();
        dpm.init(this);
        synchronized (mVolumeManagerLock) {
            mVolumeManager = VolumeManager.init(this);
        }
        mPrevStateMap.clear();
        mLeProfilesInterStateMap.clear();
        mLeProfilesStableStateMap.clear();

        mIsSasPtsEnabled =
             SystemProperties.getBoolean("persist.vendor.service.bt.leaudio.pts", false);
        Log.d(TAG, "mIsSasPtsEnabled: " + mIsSasPtsEnabled);

        mIsPtsAc9iEnabled =
             SystemProperties.getBoolean("persist.vendor.service.bt.leaudio.ptsAc9i", false);
        Log.d(TAG, "mIsSasPtsEnabled: " + mIsPtsAc9iEnabled);

        isDualModeSupported =
               SystemProperties.getBoolean(DUAL_MODE_TRANSPORT_AVAILABLE, false);
        if (isDualModeSupported) {
           String mIsDualModePropSet;
           mIsDualModePropSet =
                SystemProperties.get("persist.bluetooth.enable_dual_mode_audio", "");
           if (mIsDualModePropSet == "true") {
              Log.d(TAG, "mIsDualModePropSet is set to " + mIsDualModePropSet);
              SystemProperties.set(PREFER_LE_AUDIO_ONLY_MODE, "false");
              isDualModeEnabled = true;
           } else {
              Log.d(TAG, "Dual Mode transport is disabled");
           }
        }

        Log.i(TAG, "start() complete");
        return true;
    }

    @Override
    protected boolean stop() {
        Log.w(TAG, "stop() called");
        if (sStreamAudioService == null) {
            Log.w(TAG, "stop() called before start()");
            return true;
        }

        if (mActiveDeviceManager != null) {
            mActiveDeviceManager.disable();
            mActiveDeviceManager.cleanup();
        }

        DeviceProfileMap dMap = DeviceProfileMap.getDeviceProfileMapInstance();
        dMap.cleanup();
        mMediaAudio.cleanup();
        mCallAudio.cleanup();
        mPrevStateMap.clear();
        mLeProfilesInterStateMap.clear();
        mLeProfilesStableStateMap.clear();
        mCurrentContextType = 0;
        mLowLatencyMode = false;
        return true;
    }

    @Override
    protected void cleanup() {
        Log.i(TAG, "cleanup()");
        synchronized (mVolumeManagerLock) {
            if (mVolumeManager != null)
                mVolumeManager.cleanup();
            mVolumeManager = null;
        }
        setStreamAudioService(null);
    }

    public boolean isDualModeAudioEnabled() {
        Log.i(TAG, "Dual mode support is: " + isDualModeEnabled);
        return isDualModeEnabled;
    }

    public boolean connectLeStream(BluetoothDevice device, int profile) {
        AcmService mAcmService = AcmService.getAcmService();
        int mContext = getContext(profile);

        if(mContext == 0) {
            Log.e(TAG, "No valid context for profiles passed");
            return false;
        }

        if(mAcmService == null) {
            Log.e(TAG, "connectLeStream: mAcmService is null");
            return false;
        }

        Log.e(TAG, "profile: " + profile);
        return mAcmService.connect(device, mContext, getAcmProfileID(profile), /*MEDIA_CONTEXT*/
                                                     getAcmPreferredProfile(device));
    }

    public boolean disconnectLeStream(BluetoothDevice device,
                                      boolean callAudio, boolean mediaAudio) {
        AcmService mAcmService = AcmService.getAcmService();
        Log.i(TAG, "disconnectLeStream(): callAudio: " + callAudio +
                                       ", mediaAudio: " + mediaAudio);
        if(mAcmService == null) {
            Log.e(TAG, "disconnectLeStream: mAcmService is null");
            return false;
        }
        if(callAudio && mediaAudio)
            return mAcmService.disconnect(device, VOICE_CONTEXT | MEDIA_CONTEXT);
        else if(mediaAudio)
            return mAcmService.disconnect(device, MEDIA_CONTEXT);
        else if(callAudio)
            return mAcmService.disconnect(device, VOICE_CONTEXT);

        return false;
    }

    public boolean startStream(BluetoothDevice device) {
        AcmService mAcmService = AcmService.getAcmService();
        if(mAcmService == null) {
            Log.e(TAG, "startStream: mAcmService is null");
            return false;
        }
        return mAcmService.StartStream(device, VOICE_CONTEXT);
    }

    public boolean stopStream(BluetoothDevice device) {
        AcmService mAcmService = AcmService.getAcmService();
        if(mAcmService == null) {
            Log.e(TAG, "stopStream: mAcmService is null");
            return false;
        }
        return mAcmService.StopStream(device, VOICE_CONTEXT);
    }

    public int setActiveDevice(BluetoothDevice device, int profile, boolean playReq) {
        AcmService mAcmService = AcmService.getAcmService();
        if (mAcmService == null && device == null) {
            Log.w(TAG, ": device is null, fake success.");
            return mActiveDeviceManager.SHO_SUCCESS;
        }
        if(mAcmService == null) {
            Log.e(TAG, "setActiveDevice: mAcmService is null");
            return mActiveDeviceManager.SHO_FAILED;
        }

        if(ApmConst.AudioProfiles.BAP_MEDIA == profile) {
            return mAcmService.setActiveDevice(device, MEDIA_CONTEXT, BAP, playReq);
        } else if(ApmConst.AudioProfiles.BAP_GCP == profile){
            return mAcmService.setActiveDevice(device, MEDIA_CONTEXT, GCP, playReq);
        } else if(ApmConst.AudioProfiles.BAP_RECORDING == profile){
            return mAcmService.setActiveDevice(device, MEDIA_CONTEXT, WMCP, playReq);
        } else if(ApmConst.AudioProfiles.BAP_GCP_VBC == profile){
            return mAcmService.setActiveDevice(device, MEDIA_CONTEXT, GCP_VBC, playReq);
        } else {
            return mAcmService.setActiveDevice(device, VOICE_CONTEXT, BAP_CALL, playReq);
            //return mAcmService.setActiveDevice(device, MEDIA_CONTEXT, BAP, playReq);
        }
    }

    public void updateDeviceProfileType(BluetoothDevice device, int profile) {
        AcmService mAcmService = AcmService.getAcmService();
        if (mAcmService == null ||  device == null) {
            Log.w(TAG, ": AcmService or device is null, fake success.");
            return;
        }
        mAcmService.updateActiveProfile(device, MEDIA_CONTEXT, BAP);
    }
    public void setCodecConfig(BluetoothDevice device, String codecID, int channelMode) {
        AcmService mAcmService = AcmService.getAcmService();
        if(mAcmService != null)
            mAcmService.ChangeCodecConfigPreference(device, codecID);
    }

    public BluetoothDevice getDeviceGroup(BluetoothDevice device){
        AcmService mAcmService = AcmService.getAcmService();
        if(mAcmService == null) {
            Log.e(TAG, "getDeviceGroup: mAcmService is null");
            return null;
        }
        return mAcmService.onlyGetGroupDevice(device);
    }

    public List<BluetoothDevice> getGroupMembers(BluetoothDevice groupAddress) {
        AcmService mAcmService = AcmService.getAcmService();
        return (mAcmService != null) ? mAcmService.getGroupMembers(groupAddress) : null;
    }

    public boolean isLowLatencyModeEnabled() {
        Log.d(TAG, " mLowLatencyMode: " + mLowLatencyMode);
        return mLowLatencyMode;
    }

    public void resetLowLatencyMode() {
        mLowLatencyMode = false;
    }

    public void setLatencyMode(boolean isLowLatency) {
        Log.d(TAG, "setLatencyMode: " + isLowLatency +
              " mCurrentContextType: " + mCurrentContextType);
        int profile = mActiveDeviceManager.getActiveProfile(ApmConst.AudioFeatures.MEDIA_AUDIO);
        if (profile != ApmConst.AudioProfiles.A2DP) {
            Log.e(TAG, " setLatencyMode called for non-A2DP profile");
            return;
        }

        mLowLatencyMode = isLowLatency;
        MediaAudio mMediaAudio = MediaAudio.get();
        BluetoothDevice device = mActiveDeviceManager.getActiveDevice(
                                    ApmConst.AudioFeatures.MEDIA_AUDIO);
        if (isLowLatency) {
            if (device != null) {
                Log.d(TAG, "enableGamingMode");
                mMediaAudio.enableGamingMode(device, mCurrentContextType);
            }
        } else {
            if (mCurrentContextType != METADATA_GAME) {
                if (device != null) {
                    Log.d(TAG, "disableGamingMode");
                    mMediaAudio.disableGamingMode(device, mCurrentContextType);
                }
            }
        }
    }

    public void setMetadataContext(int context_type) {
        Log.w(TAG, "setMetadataContextType: " + context_type);
        MediaAudio mMediaAudio = MediaAudio.get();
        BluetoothDevice device = mActiveDeviceManager.getActiveDevice(
                                    ApmConst.AudioFeatures.MEDIA_AUDIO);
        mCurrentContextType = context_type;
        switch(context_type) {
            case METADATA_CONVERSATIONAL:
                Log.w(TAG, "Conversational context type received");
                if (device != null && !mLowLatencyMode) {
                    mMediaAudio.disableGamingMode(device, context_type);
                }
                break;
            case METADATA_MEDIA:
                Log.w(TAG, "Media context type received");
                if (device != null && !mLowLatencyMode) {
                    mMediaAudio.disableGamingMode(device, context_type);
                }
                break;
            case METADATA_GAME:
                Log.w(TAG, "Game context type received");
                if (device != null) {
                    mMediaAudio.enableGamingMode(device, context_type);
                }
                break;
            case METADATA_LIVE:
                Log.w(TAG, "SRec context type received");
                if (device != null) {
                    //mMediaAudio.enableRecordingMode(device, context_type);
                }
                break;
            case METADATA_SOUNDEFFECTS:
                Log.w(TAG, "Sonification context type received, it can be either of" +
                           "Media, Conversational, Live or Game");
                break;
            case METADATA_NOTIFICATIONS:
                Log.w(TAG, "Notification context type received");
                break;
            case METADATA_RINGTONE:
                Log.w(TAG, "Ringtone context type received");
                break;
            case METADATA_ALERTS:
                Log.w(TAG, "Alert context type received");
                break;
            case METADATA_EMERGENCYALARM:
                Log.w(TAG, "Emergency Alarm context type received");
                break;
            default:
                Log.w(TAG, "No context type received");
                break;
        }
    }

    public void onConnectionStateChange(BluetoothDevice device, int state,
                                        int audioType, boolean primeDevice) {
        Log.w(TAG, "onConnectionStateChange: state:" + state +
                   " for device " + device + " audioType: " + audioType +
                   " primeDevice: " + primeDevice);
        MediaAudio mMediaAudio = MediaAudio.get();
        CallAudio mCallAudio = CallAudio.get();
        int profile = ApmConst.AudioProfiles.NONE;

        boolean isCsipDevice = false;
        BluetoothDevice grpDevice = getDeviceGroup(device);
        Log.w(TAG, "onConnectionStateChange: grpDevice:" + grpDevice);
        if (grpDevice != null) {
            isCsipDevice = (device != null) &&
                  grpDevice.getAddress().contains(ApmConst.groupAddress);
        }
        Log.w(TAG, "onConnectionStateChange: isCsipDevice:" + isCsipDevice);

        if (audioType == ApmConst.AudioFeatures.CALL_AUDIO) {
            profile = ApmConst.AudioProfiles.BAP_CALL;
            if(isCsipDevice) {
                mCallAudio.onConnStateChange(device, state,
                                             ApmConst.AudioProfiles.BAP_CALL,
                                             primeDevice);
            } else {
                mCallAudio.onConnStateChange(device, state,
                                             ApmConst.AudioProfiles.BAP_CALL);
            }
        } else if (audioType == ApmConst.AudioFeatures.MEDIA_AUDIO) {
            profile = ApmConst.AudioProfiles.BAP_MEDIA;

            if (isCsipDevice) {
                mMediaAudio.onConnStateChange(device, state,
                                              ApmConst.AudioProfiles.BAP_MEDIA,
                                              primeDevice);
            } else {
                mMediaAudio.onConnStateChange(device, state,
                                              ApmConst.AudioProfiles.BAP_MEDIA);
            }
        }

        if (!ApmConst.getQtiLeAudioEnabled()) {
            int prevState = BluetoothProfile.STATE_DISCONNECTED;
            int leProfilesInterState = 0x0;
            int leProfilesStableState = 0x0;
            if (mPrevStateMap.containsKey(device)) {
                prevState = mPrevStateMap.get(device);
            }
            if (mLeProfilesInterStateMap.containsKey(device)) {
                leProfilesInterState = mLeProfilesInterStateMap.get(device);
            }
            if (mLeProfilesStableStateMap.containsKey(device)) {
                leProfilesStableState = mLeProfilesStableStateMap.get(device);
            }

            Log.w(TAG, "onConnectionStateChange: prevState:" + prevState +
                       ", profile: " + profile + ", mIsSasPtsEnabled: " + mIsSasPtsEnabled);

            if (profile == ApmConst.AudioProfiles.BAP_MEDIA) {
                if (state == BluetoothProfile.STATE_CONNECTING) {
                    Log.w(TAG, "onConnectionStateChange: Media into connecting state");
                    leProfilesInterState |= LE_M_PROF_CONNECTING;
                } else if (state == BluetoothProfile.STATE_DISCONNECTING) {
                    Log.w(TAG, "onConnectionStateChange: Media into disconnecting state");
                    leProfilesInterState &= ~LE_M_PROF_CONNECTING;
                } else if (state == BluetoothProfile.STATE_CONNECTED) {
                    Log.w(TAG, "onConnectionStateChange: Media got connected");
                    leProfilesStableState |= LE_M_PROF_CONNECTED;
                } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.w(TAG, "onConnectionStateChange: Media got disconnected");
                    leProfilesStableState &= ~LE_M_PROF_CONNECTED;
                }
            }

            if (profile == ApmConst.AudioProfiles.BAP_CALL) {
                if (state == BluetoothProfile.STATE_CONNECTING) {
                    Log.w(TAG, "onConnectionStateChange: Voice into connecting state");
                    leProfilesInterState |= LE_V_PROF_CONNECTING;
                } else if (state == BluetoothProfile.STATE_DISCONNECTING) {
                    Log.w(TAG, "onConnectionStateChange: Voice into disconnecting state");
                    leProfilesInterState &= ~LE_V_PROF_CONNECTING;
                } else if (state == BluetoothProfile.STATE_CONNECTED) {
                    Log.w(TAG, "onConnectionStateChange: Voice got connected");
                    leProfilesStableState |= LE_V_PROF_CONNECTED;
                } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.w(TAG, "onConnectionStateChange: Voice got disconnected");
                    leProfilesStableState &= ~LE_V_PROF_CONNECTED;
                }
            }

            Log.w(TAG, "onConnectionStateChange: "+
                       " leProfilesInterState:" + leProfilesInterState +
                       ", leProfilesStableState:" + leProfilesStableState);

            DeviceProfileMap dMap = DeviceProfileMap.getDeviceProfileMapInstance();
            if (dMap == null) {
                Log.w(TAG, "onConnectionStateChange: dmap is null, return.");
                return;
            }

            int MediaProfileID =
               dMap.getSupportedProfile(device, ApmConst.AudioFeatures.MEDIA_AUDIO);
            int VoiceProfileID =
               dMap.getSupportedProfile(device, ApmConst.AudioFeatures.CALL_AUDIO);
            Log.w(TAG, "onConnectionStateChange: "+
                       " MediaProfileID:" + MediaProfileID +
                       ", VoiceProfileID:" + VoiceProfileID);

            if (prevState != state &&
                state == BluetoothProfile.STATE_CONNECTING &&
                leProfilesInterState == LE_VM_PROF_CONNECTING) {
                Log.w(TAG, "onConnectionStateChange: both BAP Media and Voice" +
                                                             " into connecting");
                lebroadcastConnStateChange(device,
                                     BluetoothProfile.STATE_DISCONNECTED, state);
                prevState = state;
            } else if (prevState != state &&
                       state == BluetoothProfile.STATE_DISCONNECTING &&
                       leProfilesInterState == LE_VM_PROF_DISCONNECTING) {
                Log.w(TAG, "onConnectionStateChange: both BAP Media and Voice" +
                           " into disconnecting, don't broadcast");
                //lebroadcastConnStateChange(device, prevState, state);
                //prevState = state;
            } else if (prevState != state &&
                       state == BluetoothProfile.STATE_CONNECTED &&
                       leProfilesStableState == LE_VM_PROF_CONNECTED) {
                Log.w(TAG, "onConnectionStateChange: both BAP Media and Voice" +
                                                                " got connected");
                lebroadcastConnStateChange(device, prevState, state);

                prevState = state;
            } else if (prevState != state &&
                      state == BluetoothProfile.STATE_DISCONNECTED &&
                      leProfilesStableState == LE_VM_PROF_DISCONNECTED) {
                Log.w(TAG, "onConnectionStateChange: both BAP Media and Voice" +
                                                               " got disconnected");
                lebroadcastConnStateChange(device, prevState, state);

                prevState = state;
            } else if ((((ApmConst.AudioProfiles.BAP_CALL & VoiceProfileID) !=
                         ApmConst.AudioProfiles.BAP_CALL) || mIsSasPtsEnabled) &&
                       prevState != state) {
                if (state == BluetoothProfile.STATE_CONNECTING &&
                    leProfilesInterState == LE_M_PROF_CONNECTING) {
                    Log.w(TAG, "onConnectionStateChange: BAP Media connecting");
                } else if (state == BluetoothProfile.STATE_CONNECTED &&
                           leProfilesStableState == LE_M_PROF_CONNECTED) {
                    Log.w(TAG, "onConnectionStateChange: BAP Media connected");
                } else if (state == BluetoothProfile.STATE_DISCONNECTING &&
                           leProfilesInterState == LE_VM_PROF_DISCONNECTING) {
                    Log.w(TAG, "onConnectionStateChange: BAP Media disconnecting");
                } else if (state == BluetoothProfile.STATE_DISCONNECTED &&
                           leProfilesInterState == LE_VM_PROF_DISCONNECTED) {
                    Log.w(TAG, "onConnectionStateChange: BAP Media disconnected");
                }
                lebroadcastConnStateChange(device, prevState, state);
                prevState = state;
            } else if ((((ApmConst.AudioProfiles.BAP_MEDIA & MediaProfileID) !=
                         ApmConst.AudioProfiles.BAP_MEDIA) || mIsSasPtsEnabled) &&
                       prevState != state) {
                if (state == BluetoothProfile.STATE_CONNECTING &&
                    leProfilesInterState == LE_V_PROF_CONNECTING) {
                    Log.w(TAG, "onConnectionStateChange: BAP Call connecting");
                } else if (state == BluetoothProfile.STATE_CONNECTED &&
                           leProfilesStableState == LE_V_PROF_CONNECTED) {
                    Log.w(TAG, "onConnectionStateChange: BAP Call connected");
                } else if (state == BluetoothProfile.STATE_DISCONNECTING &&
                           leProfilesInterState == LE_VM_PROF_DISCONNECTING) {
                    Log.w(TAG, "onConnectionStateChange: BAP Call disconnecting");
                } else if (state == BluetoothProfile.STATE_DISCONNECTED &&
                           leProfilesInterState == LE_VM_PROF_DISCONNECTED) {
                    Log.w(TAG, "onConnectionStateChange: BAP Call disconnected");
                }
                lebroadcastConnStateChange(device, prevState, state);
                prevState = state;
            } else {
                Log.w(TAG, "onConnectionStateChange: Either of the profile" +
                                                         " not in proper state");
            }
            Log.w(TAG, "onConnectionStateChange: " +
                       " prevState: " + prevState +
                       ", leProfilesInterState: " + leProfilesInterState +
                       ", leProfilesStableState: " + leProfilesStableState);
            mPrevStateMap.put(device, prevState);
            mLeProfilesInterStateMap.put(device, leProfilesInterState);
            mLeProfilesStableStateMap.put(device, leProfilesStableState);
        }
        return;
    }

    public void lebroadcastConnStateChange(BluetoothDevice device,
                                              int prevState, int newState) {
        //A2dpService mA2dpService = A2dpService.getA2dpService();
        LeAudioService mLeAudioService = LeAudioService.getLeAudioService();
        ActiveDeviceManager deviceManager =
                           AdapterService.getAdapterService().getActiveDeviceManager();
        if (mLeAudioService != null) {
            Log.d(TAG, "lebroadcastConnStateChange: " + prevState +
                       "->" + newState + " for device " + device);
            mLeAudioService.connectionStateChanged(device, prevState, newState);
            deviceManager.onLeDeviceConnStateChange(device, newState, prevState);
            Intent intent = new Intent(BluetoothLeAudio.ACTION_LE_AUDIO_CONNECTION_STATE_CHANGED);
            intent.putExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, prevState);
            intent.putExtra(BluetoothProfile.EXTRA_STATE, newState);
            intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
            intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT
                            | Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND);
            mLeAudioService.sendBroadcast(intent, BLUETOOTH_CONNECT,
                 Utils.getTempAllowlistBroadcastOptions());
        }
        CCService ccService = CCService.getCCService();
        if (ccService != null) {
            ccService.onAcmConnectionStateChanged(device, prevState, newState);
        }
    }

    public void onStreamStateChange(BluetoothDevice device, int state, int audioType) {
        MediaAudio mMediaAudio = MediaAudio.get();
        CallAudio mCallAudio = CallAudio.get();
        if(audioType == ApmConst.AudioFeatures.MEDIA_AUDIO)
            mMediaAudio.onStreamStateChange(device, state);
        else if(audioType == ApmConst.AudioFeatures.CALL_AUDIO)
             mCallAudio.onAudioStateChange(device, state);
    }

    public void onActiveDeviceChange(BluetoothDevice device, int audioType) {
        if (mActiveDeviceManager != null)
            mActiveDeviceManager.onActiveDeviceChange(device, audioType);
    }

    public void onLeCodecConfigChange(BluetoothDevice device,
                                      BluetoothLeAudioCodecStatus codecStatus,
                                      int audioType) {
        Log.w(TAG, "onMediaLeCodecConfigChange(): device: " + device +
                   ", codecstatus " + codecStatus);
        LeAudioService leservice = LeAudioService.getLeAudioService();
        if (leservice != null) {
            Log.w(TAG, "Trigger callback ");
            leservice.onLeCodecConfigChange(device, codecStatus, ApmConst.AudioProfiles.BAP_MEDIA);
        }
    }

    public void onMediaCodecConfigChange(BluetoothDevice device, BluetoothCodecStatus codecStatus, int audioType) {
        MediaAudio mMediaAudio = MediaAudio.get();
        mMediaAudio.onCodecConfigChange(device, codecStatus, ApmConst.AudioProfiles.BAP_MEDIA);
    }

    public void onMediaCodecConfigChange(BluetoothDevice device, BluetoothCodecStatus codecStatus, int audioType, boolean updateAudio) {
        MediaAudio mMediaAudio = MediaAudio.get();
        mMediaAudio.onCodecConfigChange(device, codecStatus, ApmConst.AudioProfiles.BAP_MEDIA, updateAudio);
    }

    public void setCallAudioParam(String param) {
        CallAudio mCallAudio = CallAudio.get();
        mCallAudio.setAudioParam(param);
    }

    public void setCallAudioOn(boolean on) {
        CallAudio mCallAudio = CallAudio.get();
        mCallAudio.setBluetoothScoOn(on);
    }

    public int getVcpConnState(BluetoothDevice device) {
        synchronized (mVolumeManagerLock) {
            if (mVolumeManager == null)
                return BluetoothProfile.STATE_DISCONNECTED;
            return mVolumeManager.getConnectionState(device);
        }
    }

    public int getConnectionMode(BluetoothDevice device) {
        synchronized (mVolumeManagerLock) {
            if (mVolumeManager == null)
                return BluetoothProfile.STATE_DISCONNECTED;
            return mVolumeManager.getConnectionMode(device);
        }
    }

    public void setAbsoluteVolume(BluetoothDevice device, int volume) {
        synchronized (mVolumeManagerLock) {
            if (mVolumeManager != null)
                mVolumeManager.updateBroadcastVolume(device, volume);
        }
    }

    public int getAbsoluteVolume(BluetoothDevice device) {
        synchronized (mVolumeManagerLock) {
            if (mVolumeManager == null)
                return 7;
            return mVolumeManager.getBassVolume(device);
        }
    }

    public void setMute(BluetoothDevice device, boolean muteStatus) {
        synchronized (mVolumeManagerLock) {
            if (mVolumeManager != null)
                mVolumeManager.setMute(device, muteStatus);
        }
    }

    public boolean isMute(BluetoothDevice device) {
        synchronized (mVolumeManagerLock) {
            if (mVolumeManager == null)
                return false;
            return mVolumeManager.getMuteStatus(device);
        }
    }

    boolean setActiveProfile(BluetoothDevice device, int audioType, int profile) {
        MediaAudio mMediaAudio = MediaAudio.get();
        CallAudio mCallAudio = CallAudio.get();
        if(audioType == ApmConst.AudioFeatures.MEDIA_AUDIO)
            return mMediaAudio.setActiveProfile(device, profile);
        else if(audioType == ApmConst.AudioFeatures.CALL_AUDIO)
             return mCallAudio.setActiveProfile(device, profile);
        return false;
    }

    int getActiveProfile(int audioType) {
        return mActiveDeviceManager.getActiveProfile(audioType);
    }

    private int getContext(int profileID) {
        int context = 0;
        Log.i(TAG, "mIsPtsAc9iEnabled: " + mIsPtsAc9iEnabled);
        if((DeviceProfileMap.getLeMediaProfiles() & profileID) > 0) {
            context = (context|MEDIA_CONTEXT);
        }

        if (!mIsPtsAc9iEnabled) {
            if ((DeviceProfileMap.getLeCallProfiles() & profileID) > 0) {
                context = (context|VOICE_CONTEXT);
            }
        }
        return context;
    }

    private int getAcmProfileID (int ProfileID) {
        int AcmProfileID = 0;
        Log.i(TAG, "mIsPtsAc9iEnabled: " + mIsPtsAc9iEnabled);
        if (!mIsPtsAc9iEnabled) {
            if((ApmConst.AudioProfiles.BAP_MEDIA & ProfileID) ==
                                           ApmConst.AudioProfiles.BAP_MEDIA) {
                AcmProfileID = BAP;
            }

            if((ApmConst.AudioProfiles.BAP_CALL & ProfileID) ==
                                           ApmConst.AudioProfiles.BAP_CALL) {
                AcmProfileID = AcmProfileID | BAP_CALL;
            }

            if ((ApmConst.AudioProfiles.HAP_LE & ProfileID) ==
                                           ApmConst.AudioProfiles.HAP_LE) {
                AcmProfileID = AcmProfileID | HAP_LE;
            }

            if((ApmConst.AudioProfiles.BAP_GCP & ProfileID) ==
                                           ApmConst.AudioProfiles.BAP_GCP) {
                AcmProfileID = AcmProfileID | GCP;
            }
        }

        if((ApmConst.AudioProfiles.BAP_RECORDING & ProfileID) ==
                                  ApmConst.AudioProfiles.BAP_RECORDING) {
            AcmProfileID = AcmProfileID | WMCP;
        } else if(!mIsPtsAc9iEnabled &&
                  ((ApmConst.AudioProfiles.BAP_GCP_VBC & ProfileID) ==
                                  ApmConst.AudioProfiles.BAP_GCP_VBC)) {
            AcmProfileID = AcmProfileID | GCP_VBC;
        }

        return AcmProfileID;
    }

    public int getAcmPreferredProfile(BluetoothDevice device) {
        boolean isAospLeAudioEnabled = ApmConstIntf.getAospLeaEnabled();
        Log.i(TAG, "isAospLeAudioEnabled: " + isAospLeAudioEnabled);
        boolean mTmapEnabled =
          SystemProperties.getBoolean("persist.vendor.qcom.bluetooth.enable_tmap_profile", false);
        Log.d(TAG, "Is Tmap Profile enabled = " + mTmapEnabled);

        int prefAcmProf = 0; //This variable decides whether to go with BAP/TMAP

        //for hearing Aid
        if (isAospLeAudioEnabled) {
            DeviceProfileMap dMap = DeviceProfileMap.getDeviceProfileMapInstance();
            int supported_prfiles = dMap.getAllSupportedProfile(device);

            if ((supported_prfiles & ApmConst.AudioProfiles.HAP_LE) ==
                                                   ApmConst.AudioProfiles.HAP_LE) {
                Log.d(TAG, "preferred profile is HAP_LE");
                prefAcmProf = prefAcmProf|HAP_LE;
                return prefAcmProf;
            }
        }

        if (isAospLeAudioEnabled && mTmapEnabled) {
            DeviceProfileMap dMap = DeviceProfileMap.getDeviceProfileMapInstance();
            int supported_prfiles = dMap.getAllSupportedProfile(device);

            if ((supported_prfiles & ApmConst.AudioProfiles.TMAP_MEDIA) ==
                                                   ApmConst.AudioProfiles.TMAP_MEDIA) {
                prefAcmProf = prefAcmProf|PREFE_PROF_TMAP_MEDIA;
            }

            if ((supported_prfiles & ApmConst.AudioProfiles.TMAP_CALL) ==
                                                   ApmConst.AudioProfiles.TMAP_CALL) {
                prefAcmProf = prefAcmProf|PREFE_PROF_TMAP_CALL;
            }
        }
        Log.d(TAG, "getAcmPreferredProfile(): device: " + device +
                   ", prefAcmProf: " + prefAcmProf);

        return prefAcmProf;
    }

    @Override
    protected IProfileServiceBinder initBinder() {
        return new LeAudioUnicastBinder(this);
    }

    private static class LeAudioUnicastBinder extends IBluetoothVcp.Stub implements IProfileServiceBinder {
        StreamAudioService mService;

        private StreamAudioService getService() {
            if (mService != null && mService.isAvailable()) {
                return mService;
            }
            return null;
        }

        LeAudioUnicastBinder(StreamAudioService service) {
            mService = service;
        }

        @Override
        public void cleanup() {
        }

        @Override
        public int getConnectionState(BluetoothDevice device, AttributionSource source) {
            if (DBG) {
                Log.d(TAG, "getConnectionState(): device: " + device);
            }
            StreamAudioService service = getService();
            if(service == null || !Utils.checkConnectPermissionForDataDelivery(
                    service, source, "getConnectionState")) {
                return BluetoothProfile.STATE_DISCONNECTED;
            }
            return service.getVcpConnState(device);
        }

        @Override
        public int getConnectionMode(BluetoothDevice device, AttributionSource source) {
            if (DBG) {
                Log.d(TAG, "getConnectionMode(): device: " + device);
            }
            StreamAudioService service = getService();
            if(service == null || !Utils.checkConnectPermissionForDataDelivery(
                    service, source, "getConnectionMode")) {
                return 0;
            }
            return service.getConnectionMode(device);
        }

        @Override
        public void setAbsoluteVolume(BluetoothDevice device, int volume, AttributionSource source) {
            if (DBG) {
                Log.d(TAG, "setAbsoluteVolume(): device: " + device + " volume: " + volume);
            }
            StreamAudioService service = getService();
            if(service == null || !Utils.checkConnectPermissionForDataDelivery(
                    service, source, "setAbsoluteVolume")) {
                return;
            }
            service.setAbsoluteVolume(device, volume);
        }

        @Override
        public int getAbsoluteVolume(BluetoothDevice device, AttributionSource source) {
            if (DBG) {
                Log.d(TAG, "getAbsoluteVolume(): device: " + device);
            }
            StreamAudioService service = getService();
            if(service == null || !Utils.checkConnectPermissionForDataDelivery(
                    service, source, "getAbsoluteVolume")) {
                return 7;
            }
            return service.getAbsoluteVolume(device);
        }

        @Override
        public void setMute (BluetoothDevice device, boolean enableMute, AttributionSource source) {
            if (DBG) {
                Log.d(TAG, "setMute(): device: " + device);
            }
            StreamAudioService service = getService();
            if(service == null || !Utils.checkConnectPermissionForDataDelivery(
                    service, source, "setMute")) {
                return;
            }
            service.setMute(device, enableMute);
        }

        @Override
        public boolean isMute(BluetoothDevice device, AttributionSource source) {
            if (DBG) {
                Log.d(TAG, "isMute(): device: " + device);
            }
            StreamAudioService service = getService();
            if(service == null || !Utils.checkConnectPermissionForDataDelivery(
                    service, source, "isMute")) {
                return false;
            }
            return service.isMute(device);
        }

        public boolean setActiveProfile(BluetoothDevice device, int audioType, int profile,
                                       AttributionSource source) {
            StreamAudioService service = getService();
            if((service != null || !Utils.checkConnectPermissionForDataDelivery(
                service, source,"setActiveProfile")) && device != null) {
                return mService.setActiveProfile(device, audioType, profile);
            }
            return false;
        }

        public int getActiveProfile(int audioType, AttributionSource source) {
            StreamAudioService service = getService();
            if(service != null || !Utils.checkConnectPermissionForDataDelivery(
                    service, source,"getActiveProfile")) {
                return mService.getActiveProfile(audioType);
            }
            return -1;
        }
    }

    public static StreamAudioService getStreamAudioService() {
        return sStreamAudioService;
    }

    private static synchronized void setStreamAudioService(StreamAudioService instance) {
        if (DBG) {
            Log.d(TAG, "setStreamAudioService(): set to: " + instance);
        }
        sStreamAudioService = instance;
    }
}
