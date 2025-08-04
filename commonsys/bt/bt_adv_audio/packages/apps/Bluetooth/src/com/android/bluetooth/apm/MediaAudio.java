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
 **************************************************************************/

package com.android.bluetooth.apm;

import static android.Manifest.permission.BLUETOOTH_CONNECT;
import static android.bluetooth.IBluetoothLeAudio.LE_AUDIO_GROUP_ID_INVALID;

import android.annotation.RequiresPermission;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothCodecConfig;
import android.bluetooth.BluetoothCodecStatus;
import android.bluetooth.BluetoothLeAudioCodecStatus;
import android.bluetooth.BluetoothLeAudioCodecConfig;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import com.android.bluetooth.Utils;
import android.bluetooth.BluetoothUuid;
import android.bluetooth.IBluetoothA2dp;
import android.bluetooth.BluetoothAdapter;

import com.android.bluetooth.btservice.AdapterService;
import com.android.bluetooth.btservice.ActiveDeviceManager;
import com.android.bluetooth.btservice.ProfileService;
import com.android.bluetooth.a2dp.A2dpService;
import com.android.bluetooth.apm.ActiveDeviceManagerService;
import com.android.bluetooth.apm.ApmConst;
import com.android.bluetooth.broadcast.BroadcastService;
import com.android.bluetooth.le_audio.LeAudioService;
import com.android.bluetooth.acm.AcmService;
import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.os.ParcelUuid;
import android.media.AudioManager;
import android.media.BluetoothProfileConnectionInfo;
import android.util.Log;
import android.util.StatsLog;
import com.android.bluetooth.hfp.HeadsetService;
import com.android.bluetooth.CsipWrapper;
import com.android.bluetooth.cc.CCService;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Arrays;

import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;

public class MediaAudio {
    private static MediaAudio sMediaAudio;
    private AdapterService mAdapterService;
//    private BapBroadcastService mBapBroadcastService;
    private ActiveDeviceManagerService mActiveDeviceManager;
    private Context mContext;
    BapBroadcastManager mBapBroadcastManager;
    Map<String, MediaDevice> mMediaDevices;

    final ArrayList <String> supported_codec = new ArrayList<String>( List.of(
        "LC3"
    ));

    private BroadcastReceiver mCodecConfigReceiver;
    private BroadcastReceiver mQosConfigReceiver;
    public static final String BLUETOOTH_PERM = android.Manifest.permission.BLUETOOTH;
    public static final String BLUETOOTH_PRIVILEGED =
            android.Manifest.permission.BLUETOOTH_PRIVILEGED;
    public static final String BLUETOOTH_ADMIN_PERM = android.Manifest.permission.BLUETOOTH_ADMIN;
    public static final String ACTION_UPDATE_CODEC_CONFIG =
                "qti.intent.bluetooth.action.UPDATE_CODEC_CONFIG";
    public static final String CODEC_ID =
                "qti.bluetooth.extra.CODEC_ID";
    public static final String CODEC_CONFIG =
                "qti.bluetooth.extra.CODEC_CONFIG";
    public static final String CHANNEL_MODE =
                "qti.bluetooth.extra.CHANNEL_MODE";
    public static final String ACTION_UPDATE_QOS_CONFIG =
                "qti.intent.bluetooth.action.UPDATE_QOS_CONFIG";
    public static final String QOS_CONFIG =
                "qti.bluetooth.extra.QOS_CONFIG";
    private static final int MAX_DEVICES = 200;
    public static final String TAG = "APM: MediaAudio";
    public static final boolean DBG = true;

    public static final int INVALID_SET_ID = 0x10;

    //Only SERC(Media Rx
    private static final long AUDIO_RECORDING_MASK = 0x00030000;
    private static final long AUDIO_RECORDING_OFF = 0x00010000;
    private static final long AUDIO_RECORDING_ON = 0x00020000;

    //Only Gaming Tx
    private static final long GAMING_OFF = 0x00001000;
    private static final long GAMING_ON = 0x00002000;
    private static final long GAMING_MODE_MASK = 0x00007000;
    private static final long APTX_ULL = 0x00005000;

    //Gaming Tx + Rx(VBC)
    private static final long GAME_AUDIO_RECORDING_MASK = 0x000C0000;
    private static final long GAME_AUDIO_RECORDING_OFF  = 0x00040000;
    private static final long GAME_AUDIO_RECORDING_ON   = 0x00080000;

    private static boolean mIsRecordingEnabled;
    private static final int PROFILE_UNKNOWN = -1;
    private static boolean mIsGamingVbcEnabledDefault;
    private static boolean mIsSetactiveLeMedia;
    private static boolean mIsCacheGamingOff;
    private static BluetoothDevice mCacheGamingDevice;
    private static boolean mLeaOnly = false;
    private static boolean  mIsMediaAudioPtsEnabled = false;
    private static boolean mGamingEnabled = false;
    private static boolean mPtsTmapConfBandC = false;
    private static boolean mIgnoreALSLLtoHQ = true;

    private static int mGamingMode     = 0;
    private static int GAME_NONE       = 0;
    private static int GAME_TX_MODE    = 1;
    private static int GAME_RX_MODE    = 2;
    private static int GAME_TX_RX_MODE = 3;

    private final int FWK_SOURCE_CODEC_TYPE_APTX_ADAPTIVE_R4 = 2;
    private final int FWK_SOURCE_CODEC_TYPE_DEFAULT = 3;
    private AudioManager mAudioManager;

    private MediaAudio(Context context) {
        Log.i(TAG, "initialization");

        mContext = context;
        mMediaDevices = new ConcurrentHashMap<String, MediaDevice>();
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            Objects.requireNonNull(mAudioManager,
                               "AudioManager cannot be null when A2dpService starts");

        mAdapterService = AdapterService.getAdapterService();
        mActiveDeviceManager = ActiveDeviceManagerService.get();

        mBapBroadcastManager = new BapBroadcastManager();

        IntentFilter codecFilter = new IntentFilter();
        codecFilter.addAction(ACTION_UPDATE_CODEC_CONFIG);
        mCodecConfigReceiver = new LeCodecConfig();
        context.registerReceiver(mCodecConfigReceiver, codecFilter, Context.RECEIVER_EXPORTED);

        IntentFilter qosFilter = new IntentFilter();
        qosFilter.addAction(ACTION_UPDATE_QOS_CONFIG);
        mQosConfigReceiver = new QosConfigReceiver();
        context.registerReceiver(mQosConfigReceiver, qosFilter, Context.RECEIVER_EXPORTED);

        mIsRecordingEnabled =
            SystemProperties.getBoolean("persist.vendor.service.bt.recording_supported", false);
        mIsGamingVbcEnabledDefault =
            SystemProperties.getBoolean("persist.vendor.service.bt.default_gaming_vbc_enabled",
                                         false);

        mIgnoreALSLLtoHQ =
            SystemProperties.getBoolean("persist.vendor.service.bt.ignore_als_ll_to_hq", true);

        mIsMediaAudioPtsEnabled =
                   SystemProperties.getBoolean("persist.vendor.service.bt.leaudio.pts", false);
        mPtsTmapConfBandC =
                   SystemProperties.getBoolean("persist.vendor.btstack.pts_tmap_conf_B_and_C",
                                                false);

        /* Use property to connect to LEA only with dual mode devices. 
         * Once dual connection comes in, this property will be set to false
         */ 
        mLeaOnly = SystemProperties.getBoolean("persist.vendor.service.bt.lea_only", true);
        mIsCacheGamingOff = false;
        mCacheGamingDevice = null;
        //2 Setup Codec Config here
    }

    public static MediaAudio init(Context context) {
        if(sMediaAudio == null) {
            sMediaAudio = new MediaAudio(context);
            MediaAudioIntf.init(sMediaAudio);
        }
        return sMediaAudio;
    }

    public static MediaAudio get() {
        return sMediaAudio;
    }

    protected void cleanup() {
        if (mCodecConfigReceiver != null) {
          mContext.unregisterReceiver(mCodecConfigReceiver);
        }
        if (mQosConfigReceiver != null) {
          mContext.unregisterReceiver(mQosConfigReceiver);
        }
        mCodecConfigReceiver = null;
        mQosConfigReceiver = null;
        mMediaDevices.clear();
    }

    public boolean connect(BluetoothDevice device) {
        return connect (device, false, false);
    }

    public boolean connect(BluetoothDevice device, Boolean allProfile) {
        return connect (device, allProfile, false);
    }

    public boolean autoConnect(BluetoothDevice device) {
        Log.e(TAG, "autoConnect: " + device);
        return connect(device, false, true);
    }

    private boolean connect(BluetoothDevice device, boolean allProfile, boolean autoConnect) {
        Log.e(TAG, "connect: " + device + " allProfile: " + allProfile +
                                          " autoConnect: " + autoConnect);

        boolean isAospLeAudioEnabled = ApmConstIntf.getAospLeaEnabled();
        Log.i(TAG, "isAospLeAudioEnabled: " + isAospLeAudioEnabled);

        if (!isAospLeAudioEnabled &&
            getConnectionPolicy(device) == BluetoothProfile.CONNECTION_POLICY_FORBIDDEN) {
            Log.e(TAG, "Cannot connect to " + device + " : CONNECTION_POLICY_FORBIDDEN");
            return false;
        }

        DeviceProfileMap dMap = DeviceProfileMap.getDeviceProfileMapInstance();
        if (dMap == null)
            return false;

        int profileID = dMap.getSupportedProfile(device, ApmConst.AudioFeatures.MEDIA_AUDIO);
        if (profileID == ApmConst.AudioProfiles.NONE) {
            if (mLeaOnly) {
                Log.d(TAG, "Device does not support media service, populate UUID from AdapterService");
                ParcelUuid[] featureUuids = mAdapterService.getRemoteUuids(device);
                dMap.handleDeviceUuids(device, featureUuids);
                profileID = dMap.getSupportedProfile(device, ApmConst.AudioFeatures.MEDIA_AUDIO);
                if (profileID == ApmConst.AudioProfiles.NONE && !allProfile) {
                    Log.e(TAG, "Cannot connect to " + device + ". No adv audio uuids present.");
                    return false;
                }
            } else {
                Log.e(TAG, "Cannot connect to " + device + ". Device does not support media service.");
                return false;
            }
        }
        int peer_supported_profiles = dMap.getAllSupportedProfile(device);
        boolean is_peer_support_recording =
                ((peer_supported_profiles & ApmConst.AudioProfiles.BAP_RECORDING) != 0);
        MediaDevice mMediaDevice = mMediaDevices.get(device.getAddress());
        if(mMediaDevice == null) {
            if(mMediaDevices.size() >= MAX_DEVICES)
                return false;
            mMediaDevice = new MediaDevice(device, profileID);
            mMediaDevices.put(device.getAddress(), mMediaDevice);
        }

        if (!isAospLeAudioEnabled &&
            mMediaDevice.profileConnStatus[MediaDevice.A2DP_STREAM] !=
                                               BluetoothProfile.STATE_CONNECTED) {
            if((ApmConst.AudioProfiles.A2DP & profileID) == ApmConst.AudioProfiles.A2DP) {
              A2dpService service = A2dpService.getA2dpService();
              if(service != null) {
                 service.connectA2dp(device);
             }
          }
        }

        if (mMediaDevice.profileConnStatus[MediaDevice.LE_STREAM] ==
                                          BluetoothProfile.STATE_CONNECTED) {
           Log.i(TAG, "LE stream already connected");
           return false;
        }

        BluetoothDevice groupDevice = device;
        StreamAudioService mStreamService =
                                StreamAudioService.getStreamAudioService();

        if (mStreamService != null &&
            (ApmConst.AudioProfiles.HAP_LE & profileID) ==
                                         ApmConst.AudioProfiles.HAP_LE) {
            int defaultMediaProfile = ApmConst.AudioProfiles.BAP_MEDIA;

            /*int defaultMusicProfile = dMap.getProfile(device, ApmConst.AudioFeatures.MEDIA_AUDIO);
            if((ApmConst.AudioProfiles.A2DP & defaultMusicProfile) == ApmConst.AudioProfiles.A2DP) {
                Log.i(TAG, "A2DP is default profile for Music, configure BAP for Gaming");
                defaultMediaProfile = ApmConst.AudioProfiles.BAP_GCP;
            }*/

            Log.i(TAG, "Recording profile enabled: " + mIsRecordingEnabled);
            if (mIsRecordingEnabled) {
                int recordingProfile =
                          dMap.getProfile(device, ApmConst.AudioFeatures.STEREO_RECORDING);
                Log.i(TAG, "recordingProfile: " + recordingProfile);

                Log.d(TAG, "mIsMediaAudioPtsEnabled: " + mIsMediaAudioPtsEnabled);
                if (mIsMediaAudioPtsEnabled) {
                    Log.d(TAG, "Forcing recording profile is there: ");
                    recordingProfile = ApmConst.AudioProfiles.BAP_RECORDING;
                }
                //Added support for recording profile
                Log.i(TAG, "force added recording and VBC profile.");
                recordingProfile = ApmConst.AudioProfiles.BAP_RECORDING;
                defaultMediaProfile = defaultMediaProfile | ApmConst.AudioProfiles.BAP_GCP_VBC;
                defaultMediaProfile = defaultMediaProfile | recordingProfile;

                if (recordingProfile == 0) {
                    int vbcProfile = dMap.getProfile(device, ApmConst.AudioFeatures.GCP_VBC);
                    Log.i(TAG, "vbcProfile: " + vbcProfile);
                    if (vbcProfile == ApmConst.AudioProfiles.BAP_GCP_VBC) {
                        Log.i(TAG, "Add vbc profile to LE connect request: " + vbcProfile);
                        defaultMediaProfile = defaultMediaProfile | vbcProfile;
                    } else {
                        Log.i(TAG, "No support for recording as well as vbc.");
                    }
                } else {
                    Log.i(TAG, "Add Recording profile to LE connect request: " + recordingProfile);
                    defaultMediaProfile = defaultMediaProfile | recordingProfile;
                }
            } else {
                //For Moselle
                Log.i(TAG, "Host not having the support of SREC, check for is remote supports vbc");
                int vbcProfile = dMap.getProfile(device, ApmConst.AudioFeatures.GCP_VBC);
                Log.i(TAG, "vbcProfile: " + vbcProfile);
                if (vbcProfile == ApmConst.AudioProfiles.BAP_GCP_VBC) {
                    Log.i(TAG, "Add vbc profile to LE connect request: " + vbcProfile);
                    defaultMediaProfile = defaultMediaProfile | vbcProfile;
                } else {
                    Log.i(TAG, "No remote support for vbc.");
                }
            }
            mStreamService.connectLeStream(groupDevice, defaultMediaProfile | ApmConst.AudioProfiles.BAP_CALL);
        } else if (mStreamService != null) {
            int defaultMediaProfile = ApmConst.AudioProfiles.NONE;
            if ((ApmConst.AudioProfiles.BAP_MEDIA & profileID) ==
                                         ApmConst.AudioProfiles.BAP_MEDIA) {
                defaultMediaProfile = ApmConst.AudioProfiles.BAP_MEDIA;

                /* handle common conect of call and media audio */
                if(autoConnect) {
                    groupDevice = mStreamService.getDeviceGroup(device);
                    Log.i(TAG, "Auto Connect Request. Connecting group: " + groupDevice);
                }

                /*int defaultMusicProfile = dMap.getProfile(device, ApmConst.AudioFeatures.MEDIA_AUDIO);
                if((ApmConst.AudioProfiles.A2DP & defaultMusicProfile) == ApmConst.AudioProfiles.A2DP) {
                    Log.i(TAG, "A2DP is default profile for Music, configure BAP for Gaming");
                    defaultMediaProfile = ApmConst.AudioProfiles.BAP_GCP;
                }*/

                Log.i(TAG, "Recording profile enabled: " + mIsRecordingEnabled);
                if (mIsRecordingEnabled) {
                    int recordingProfile =
                              dMap.getProfile(device, ApmConst.AudioFeatures.STEREO_RECORDING);
                    Log.i(TAG, "recordingProfile: " + recordingProfile);

                    Log.d(TAG, "mIsMediaAudioPtsEnabled: " + mIsMediaAudioPtsEnabled);
                    if (mIsMediaAudioPtsEnabled) {
                        Log.d(TAG, "Forcing recording profile is there: ");
                        recordingProfile = ApmConst.AudioProfiles.BAP_RECORDING;
                    }

                    if (recordingProfile == 0) {
                        int vbcProfile = dMap.getProfile(device, ApmConst.AudioFeatures.GCP_VBC);
                        Log.i(TAG, "vbcProfile: " + vbcProfile);
                        if (vbcProfile == ApmConst.AudioProfiles.BAP_GCP_VBC) {
                            Log.i(TAG, "Add vbc profile to LE connect request: " + vbcProfile);
                            defaultMediaProfile = defaultMediaProfile | vbcProfile;
                        } else {
                            Log.i(TAG, "No support for recording as well as vbc.");
                        }
                    } else {
                        Log.i(TAG, "Add Recording profile to LE connect request: " + recordingProfile);
                        defaultMediaProfile = defaultMediaProfile | recordingProfile;
                    }
                } else {
                    //For Moselle
                    Log.i(TAG, "Host not having the support of SREC, check for is remote supports vbc");
                    int vbcProfile = dMap.getProfile(device, ApmConst.AudioFeatures.GCP_VBC);
                    Log.i(TAG, "vbcProfile: " + vbcProfile);
                    if (vbcProfile == ApmConst.AudioProfiles.BAP_GCP_VBC) {
                        Log.i(TAG, "Add vbc profile to LE connect request: " + vbcProfile);
                        defaultMediaProfile = defaultMediaProfile | vbcProfile;
                    } else {
                        Log.i(TAG, "No remote support for vbc.");
                    }
                }
            }

            if (allProfile) {
                int callProfileID =
                    dMap.getSupportedProfile(device, ApmConst.AudioFeatures.CALL_AUDIO);
                if (((callProfileID & ApmConst.AudioProfiles.BAP_CALL) ==
                                                    ApmConst.AudioProfiles.BAP_CALL) ||
                   ((callProfileID & ApmConst.AudioProfiles.TMAP_CALL) ==
                                                    ApmConst.AudioProfiles.TMAP_CALL))  {
                    Log.i(TAG, "Add BAP_CALL to LE connect request");
                    if (mPtsTmapConfBandC) {
                        mStreamService.connectLeStream(groupDevice,
                                                ApmConst.AudioProfiles.BAP_CALL);
                    } else {
                        mStreamService.connectLeStream(groupDevice,
                           defaultMediaProfile | ApmConst.AudioProfiles.BAP_CALL);
                    }
                } else {
                    mStreamService.connectLeStream(groupDevice, defaultMediaProfile);
                }
            } else {
                mStreamService.connectLeStream(groupDevice, defaultMediaProfile);
            }
        }
        return true;
    }

    public boolean disconnect(BluetoothDevice device) {
        Log.i(TAG, "Disconnect: " + device);
        return disconnect(device, false);
    }

    public boolean disconnect(BluetoothDevice device, Boolean allProfile) {
        Log.i(TAG, " disconnect: " + device + ", allProfile: " + allProfile);
        MediaDevice mMediaDevice = null;

        if(device == null)
            return false;

        mMediaDevice = mMediaDevices.get(device.getAddress());
        if(mMediaDevice == null) {
            Log.e(TAG, "Ignore: Device " + device + " not present in list");
            return false;
        }

        Log.i(TAG, " disconnect: A2dp Connectionstatus: " +
                          mMediaDevice.profileConnStatus[MediaDevice.A2DP_STREAM]);

        boolean isAospLeAudioEnabled = ApmConstIntf.getAospLeaEnabled();
        Log.i(TAG, "isAospLeAudioEnabled: " + isAospLeAudioEnabled);
        if (!isAospLeAudioEnabled &&
            mMediaDevice.profileConnStatus[MediaDevice.A2DP_STREAM] !=
                                           BluetoothProfile.STATE_DISCONNECTED) {
            A2dpService service = A2dpService.getA2dpService();
            if(service != null) {
                service.disconnectA2dp(device);
            }
        }

        Log.i(TAG, " disconnect: LeMediaAudio Connectionstatus: " +
                          mMediaDevice.profileConnStatus[MediaDevice.LE_STREAM]);
        if (mMediaDevice.profileConnStatus[MediaDevice.LE_STREAM] !=
                                          BluetoothProfile.STATE_DISCONNECTED) {
            StreamAudioService service = StreamAudioService.getStreamAudioService();
            if(service != null) {
                DeviceProfileMap dMap = DeviceProfileMap.getDeviceProfileMapInstance();
                if (!dMap.isProfileConnected(device, ApmConst.AudioProfiles.BAP_CALL)) {
                    Log.d(TAG,"BAP_CALL not connected");
                    allProfile = false;
                }
                service.disconnectLeStream(device, allProfile, true);
            }
        }

        return true;
    }

    public List<BluetoothDevice> getConnectedDevices() {
        Log.i(TAG, "getConnectedDevices: ");
        if(mMediaDevices.size() == 0) {
            return new ArrayList<>(0);
        }

        List<BluetoothDevice> connectedDevices = new ArrayList<>();
        for(MediaDevice mMediaDevice : mMediaDevices.values()) {
            if(mMediaDevice.deviceConnStatus == BluetoothProfile.STATE_CONNECTED) {
                connectedDevices.add(mMediaDevice.mDevice);
            }
        }
        return connectedDevices;
    }

    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
        Log.i(TAG, "getDevicesMatchingConnectionStates: ");
        List<BluetoothDevice> devices = new ArrayList<>();
        if (states == null) {
            return devices;
        }

        BluetoothDevice [] bondedDevices = null;
        bondedDevices = mAdapterService.getBondedDevices();
        if(bondedDevices == null) {
            return devices;
        }

        for (BluetoothDevice device : bondedDevices) {
            MediaDevice mMediaDevice;
            int state = BluetoothProfile.STATE_DISCONNECTED;

            DeviceProfileMap dMap = DeviceProfileMap.getDeviceProfileMapInstance();
            if(dMap == null) {
                return new ArrayList<>(0);
            }
            if(dMap.getProfile(device, ApmConst.AudioFeatures.MEDIA_AUDIO) == ApmConst.AudioProfiles.NONE) {
                continue;
            }

            mMediaDevice = mMediaDevices.get(device.getAddress());
            if(mMediaDevice != null)
                state = mMediaDevice.deviceConnStatus;

            for(int s: states) {
                if(s == state) {
                    devices.add(device);
                    break;
                }
            }
        }
        return devices;
    }

    public int getConnectionState(BluetoothDevice device) {
        if(device == null)
            return BluetoothProfile.STATE_DISCONNECTED;

        MediaDevice mMediaDevice = mMediaDevices.get(device.getAddress());
        if(mMediaDevice != null) {
            Log.d(TAG,"getConnectionState(" + device + "): " +
                                         mMediaDevice.deviceConnStatus);
            return mMediaDevice.deviceConnStatus;
        }

        return BluetoothProfile.STATE_DISCONNECTED;
    }

    public int getPriority(BluetoothDevice device) {
        if(mAdapterService != null) {
            return mAdapterService.getDatabase()
                .getProfileConnectionPolicy(device, BluetoothProfile.A2DP);
        }
        return BluetoothProfile.PRIORITY_UNDEFINED;
    }

    public boolean isA2dpPlaying(BluetoothDevice device) {
        if(device == null)
            return false;

        MediaDevice mMediaDevice = mMediaDevices.get(device.getAddress());
        if(mMediaDevice != null) {
            Log.i(TAG, "isA2dpPlaying: " + mMediaDevice.streamStatus);
            return (mMediaDevice.streamStatus == BluetoothA2dp.STATE_PLAYING);
        }

        return false;
    }

    public BluetoothCodecStatus getCodecStatus(BluetoothDevice device) {
        Log.i(TAG, "getCodecStatus: for device: " + device);
        if(device == null)
            return null;

        if (mBapBroadcastManager.isBapBroadcastActive() &&
               (device.equals(mBapBroadcastManager.getBroadcastDevice()) ||
               mAdapterService.isAdvAudioDevice(device))) {
            return mBapBroadcastManager.getCodecStatus();
        }

        ActiveDeviceManagerService mActiveDeviceManager = ActiveDeviceManagerService.get();
        int profile = mActiveDeviceManager.getActiveProfile(ApmConst.AudioFeatures.MEDIA_AUDIO);
        if(profile != ApmConst.AudioProfiles.NONE && profile != ApmConst.AudioProfiles.A2DP) {
            StreamAudioService service = StreamAudioService.getStreamAudioService();
            if(service != null) {
                device = service.getDeviceGroup(device);
            }
        }

        if (device == null)
            return null;

        MediaDevice mMediaDevice = mMediaDevices.get(device.getAddress());
        Log.i(TAG, "getCodecStatus: for mMediaDevice: " + mMediaDevice);
        if(mMediaDevice == null)
            return null;

        Log.i(TAG, "getCodecStatus: " + mMediaDevice.mCodecStatus);
        return mMediaDevice.mCodecStatus;
    }

    public BluetoothLeAudioCodecStatus getLeAudioCodecStatus(Integer groupId) {
        Log.i(TAG, "getLeAudioCodecStatus: for groupId: " + groupId);

        if (groupId == LE_AUDIO_GROUP_ID_INVALID) {
            Log.e(TAG, "request came for invalid groupId return null");
            return null;
        }

        MediaDevice mMediaDevice = null;
        LeAudioService mLeService = LeAudioService.getLeAudioService();
        BluetoothDevice device = (mLeService != null) ?
                mLeService.getConnectedGroupLeadDevice(groupId) : null;
        Log.i(TAG, "getLeAudioCodecStatus: groupId: " + groupId + " device: " + device);
        if (device != null) {
            mMediaDevice = mMediaDevices.get(device.getAddress());
        }

        Log.i(TAG, "getLeAudioCodecStatus: for mMediaDevice: " + mMediaDevice);
        if(mMediaDevice == null) {
            Log.i(TAG, "getLeAudioCodecStatus: MediaDevice is null, return");
            return null;
        }

        AcmService mAcmService = AcmService.getAcmService();
        if (mAcmService != null) {
            mMediaDevice.mLeAudioCodecStatus = mAcmService.getLeAudioCodecStatus(device);
        } else {
            mMediaDevice.mLeAudioCodecStatus = null;
        }

        Log.i(TAG, "getLeAudioCodecStatus: " + mMediaDevice.mLeAudioCodecStatus);
        return mMediaDevice.mLeAudioCodecStatus;
    }

    public static int booleanToInt(boolean value) {
        return value ? 1 : 0;
    }

    public void enableGamingMode(BluetoothDevice mDevice, int mContext) {
        BluetoothDevice device = mDevice;
        A2dpService service = A2dpService.getA2dpService();
        BluetoothCodecStatus codecStatus = null;
        BluetoothCodecConfig codecConfig = null;
        int profile = mActiveDeviceManager.getActiveProfile(ApmConst.AudioFeatures.MEDIA_AUDIO);
        mGamingEnabled = true;
        //setGamingMode(booleanToInt(mGamingEnabled));
        if (profile == ApmConst.AudioProfiles.A2DP) {
            Log.i(TAG, "enableGamingMode: for A2DP");
            service.setGamingMode(device, true);
        } else if (profile == ApmConst.AudioProfiles.BAP_GCP) {
            Log.i(TAG, "enableGamingMode: for LE");
            //mActiveDeviceManager.enableGaming(device);
        }
    }

    public void disableGamingMode(BluetoothDevice mDevice, int mContext) {
        BluetoothDevice device = mDevice;
        A2dpService service = A2dpService.getA2dpService();
        BluetoothCodecStatus codecStatus = null;
        BluetoothCodecConfig codecConfig = null;
        int profile = mActiveDeviceManager.getActiveProfile(ApmConst.AudioFeatures.MEDIA_AUDIO);
        mGamingEnabled = false;
        //setGamingMode(booleanToInt(mGamingEnabled));
        if (profile == ApmConst.AudioProfiles.A2DP) {
            Log.i(TAG, "disableGamingMode: for A2DP");
            service.setGamingMode(device, false);
        } else if (profile == ApmConst.AudioProfiles.BAP_MEDIA ||
                 profile == ApmConst.AudioProfiles.BAP_CALL ||
                 profile == ApmConst.AudioProfiles.TMAP_MEDIA ||
                 profile == ApmConst.AudioProfiles.TMAP_CALL) {
            Log.i(TAG, "disableGamingMode: for LE");
            //mActiveDeviceManager.disableGaming(device);
        }
    }

    public void setCodecConfigPreference(BluetoothDevice mDevice,
                                             BluetoothCodecConfig codecConfig) {
        BluetoothDevice device = mDevice;

        Log.i(TAG, "setCodecConfigPreference: " + codecConfig);
        if(device == null) {
            if(mActiveDeviceManager != null) {
                device = mActiveDeviceManager.getActiveDevice(
                                         ApmConst.AudioFeatures.MEDIA_AUDIO);
            }
        }

        if (device == null) {
            Log.e(TAG, "setCodecConfigPreference: device id null, return");
            return;
        }

        if (codecConfig == null) {
            Log.e(TAG, "setCodecConfigPreference: Codec config can't be null");
            return;
        }
        long cs4 = codecConfig.getCodecSpecific4();

        if (mActiveDeviceManager.getActiveProfile(ApmConst.AudioFeatures.MEDIA_AUDIO) ==
                                                      ApmConst.AudioProfiles.BROADCAST_LE) {
            AcmService mAcmService = AcmService.getAcmService();
            BluetoothDevice mPrevDevice =
                mActiveDeviceManager.getQueuedDevice(ApmConst.AudioFeatures.MEDIA_AUDIO);

            if (mPrevDevice != null && mAcmService != null) {
                if ((cs4 & AUDIO_RECORDING_MASK) == AUDIO_RECORDING_ON) {
                    if (mAcmService.getConnectionState(mPrevDevice) ==
                                              BluetoothProfile.STATE_CONNECTED) {
                        device = mPrevDevice;
                        Log.d(TAG,"Recording request, switch device to " + device);
                    } else {
                        Log.d(TAG,"Not DUMO device, ignore recording request");
                        return;
                    }
                }
            } else if ((cs4 & GAMING_MODE_MASK) == GAMING_ON) {
                Log.d(TAG, "Ignore gaming mode request when broadcast is active");
                return;
            }
        }

        DeviceProfileMap dMap = DeviceProfileMap.getDeviceProfileMapInstance();
        int supported_prfiles = dMap.getAllSupportedProfile(device);

        MediaDevice mMediaDevice = mMediaDevices.get(device.getAddress());
        boolean peer_supports_recording =
                ((supported_prfiles & ApmConst.AudioProfiles.BAP_RECORDING) != 0);

        int profileIndex =
              mMediaDevice.getProfileIndex(ApmConst.AudioProfiles.BAP_MEDIA);
        int profileIndexA2dp =
              mMediaDevice.getProfileIndex(ApmConst.AudioProfiles.A2DP);
        boolean is_peer_connected_for_recording =
                    (mMediaDevice.profileConnStatus[profileIndex] ==
                                       BluetoothProfile.STATE_CONNECTED);
        boolean is_peer_connected_for_a2dp =
                    (mMediaDevice.profileConnStatus[profileIndexA2dp] ==
                                          BluetoothProfile.STATE_CONNECTED);

        Log.i(TAG, "is_peer_connected_for_recording: " +
                    is_peer_connected_for_recording +
                   ", is_peer_connected_for_a2dp: " +
                   is_peer_connected_for_a2dp);
        Log.i(TAG, " mGamingMode: " + mGamingMode);

        CallAudio mCallAudio = CallAudio.get();
        boolean isInCall =
                  mCallAudio != null && mCallAudio.isVoiceOrCallActive();

        boolean isBapConnected =
                 (mMediaDevice.profileConnStatus[mMediaDevice.LE_STREAM] ==
                                           BluetoothProfile.STATE_CONNECTED);

        Log.i(TAG, "is_bap_connected " + isBapConnected +
                   ", call in progress: " + isInCall);

        // TODO : check the FM related rx activity
        if (mActiveDeviceManager != null &&
            peer_supports_recording && mIsRecordingEnabled &&
            is_peer_connected_for_recording) {
            if ((cs4 & AUDIO_RECORDING_MASK) == AUDIO_RECORDING_ON &&
                (cs4 & GAME_AUDIO_RECORDING_MASK) != GAME_AUDIO_RECORDING_ON) {
                if(!isInCall &&
                   !mActiveDeviceManager.isRecordingActive(device) &&
                   !mActiveDeviceManager.isGamingActive(device) &&
                   (mGamingMode == GAME_NONE)) {
                  mActiveDeviceManager.enableRecording(device);
                }
            } else if ((cs4 & AUDIO_RECORDING_MASK) == AUDIO_RECORDING_OFF) {
                if(mActiveDeviceManager.isRecordingActive(device)) {
                  mActiveDeviceManager.disableRecording(device);
                }
            }
        }

        int profileID = dMap.getProfile(device, ApmConst.AudioFeatures.MEDIA_AUDIO);

        boolean peer_supports_gaming =
                    ((supported_prfiles & ApmConst.AudioProfiles.BAP_GCP) != 0);

        Log.i(TAG, "peer_supports_gaming: " + peer_supports_gaming);

        if (isBapConnected && peer_supports_gaming && ((cs4 & APTX_ULL) != APTX_ULL)) {
            //long mGamingStatus = 0;
            long mGamingStatus = (cs4 & GAMING_MODE_MASK);
            long mGamingVbcStatus = (cs4 & GAME_AUDIO_RECORDING_MASK);
            mIsCacheGamingOff = false;
            mCacheGamingDevice = null;
            boolean is_game_vbc_off = false;

            Log.i(TAG, " mGamingStatus: " + mGamingStatus +
                       ", mGamingVbcStatus: " + mGamingVbcStatus);

            boolean peer_supports_gaming_vbc =
                    ((supported_prfiles & ApmConst.AudioProfiles.BAP_GCP_VBC) != 0);

            Log.i(TAG, " mIsGamingVbcEnabledDefault: " + mIsGamingVbcEnabledDefault +
                       ", peer_supports_gaming_vbc: " + peer_supports_gaming_vbc);

            if (!mIsGamingVbcEnabledDefault) {// default prop set to false
                if ((mGamingVbcStatus & GAME_AUDIO_RECORDING_ON) > 0 && !isInCall) {
                    if (!peer_supports_gaming_vbc) {
                        // When ALS detects VBC on, but remote doesn't support vbc,
                        // then enable Gaming Tx path only.
                        Log.w(TAG, "Remote not supports VBC, Turning On Gaming Tx Mode");
                        mGamingMode = GAME_TX_MODE;
                        mActiveDeviceManager.enableGaming(device);
                        return;
                    } else {
                        // When ALS detects VBC on, and remote support vbc,
                        // then enable both, Gaming Tx and Rx path.
                        Log.w(TAG, "Remote supports VBC,Turning On Gaming Vbc Mode");
                        mGamingMode = GAME_TX_RX_MODE;
                        mActiveDeviceManager.enableGamingVbc(device);
                        return;
                    }
                } else if ((mGamingStatus & GAMING_ON) > 0 && !isInCall) {
                    // When ALS detects Only Gaming Tx
                    // then enable Gaming Tx path only.
                    Log.w(TAG, "Turning On Gaming Tx Mode, mGamingMode: " + mGamingMode);
                    mGamingMode = GAME_TX_MODE;
                    mActiveDeviceManager.enableGaming(device);
                    return;
                } else if ((mGamingVbcStatus & GAME_AUDIO_RECORDING_OFF) > 0) {
                    // When ALS detects VBC off,
                    // then reset Rx Mode and check for both gaming Tx and Rx off.
                    // If both are off, then trigger disable gaming.
                    Log.w(TAG, "Reset Gaming Rx Mode, mGamingMode: " + mGamingMode);
                    mGamingMode &= ~GAME_RX_MODE;

                    //Below Check is needed due to ALS introduced caching logic
                    //to detect vbc. In this case when Game recording off comes,
                    //need to check for Gaming Tx also, beacuse it would have been
                    //set already off.
                    if ((mGamingStatus & GAMING_OFF) > 0) {
                        Log.w(TAG, "Reset Gaming Tx Mode, mGamingMode: " + mGamingMode);
                        mGamingMode &= ~GAME_TX_MODE;
                        is_game_vbc_off = true;
                    }
                    Log.w(TAG, "mGamingMode: " + mGamingMode +
                               ", is_game_vbc_off: " + is_game_vbc_off);
                }
            } else {// default prop set to true
                if (((mGamingVbcStatus & GAME_AUDIO_RECORDING_ON) > 0 && !isInCall)) {
                    if (peer_supports_gaming_vbc) {
                        Log.w(TAG, "Turning On Gaming Vbc Mode");
                        mActiveDeviceManager.enableGamingVbc(device);
                        return;
                    } else {
                        Log.w(TAG, "Fallback to Gaming Tx mode, " +
                                   "when remote don't have vbc support");
                        mActiveDeviceManager.enableGaming(device);
                        return;
                    }
                }
            }

            if (((mGamingStatus & GAMING_OFF) > 0) || is_game_vbc_off) {
                // When ALS detects Gaming off,
                // then reset Tx Mode and check for both gaming Tx and Rx off.
                // If both are off, then trigger disable gaming.
                if (mActiveDeviceManager.isGamingActive(device)) {
                    if (!isInCall ||
                        (mActiveDeviceManager.getActiveProfile(ApmConst.AudioFeatures.CALL_AUDIO) ==
                                                                     ApmConst.AudioProfiles.HFP) ||
                        (mActiveDeviceManager.getActiveProfile(ApmConst.AudioFeatures.CALL_AUDIO) ==
                                                                     ApmConst.AudioProfiles.NONE)) {
                        Log.w(TAG, "Reset Gaming Tx Mode, mGamingMode: " + mGamingMode);
                        mGamingMode &= ~GAME_TX_MODE;
                        if (mGamingMode == GAME_NONE) {
                            Log.w(TAG, "Turning Off Gaming Tx or Tx/Rx Mode");
                            mActiveDeviceManager.disableGaming(device);
                        }
                    } else {
                        Log.w(TAG, "Cache the GAMING OFF as call is active, device " + device);
                        mIsCacheGamingOff = true;
                        mCacheGamingDevice = device;
                    }
                    return;
                }
            }
        }

        if(ApmConst.AudioProfiles.A2DP == profileID) {
            if(codecConfig.getCodecType() ==
                  BluetoothCodecConfig.SOURCE_CODEC_TYPE_LC3) {
              return;
            }
            A2dpService service = A2dpService.getA2dpService();
            if(service != null) {
                service.setCodecConfigPreferenceA2dp(device, codecConfig);
                return;
            }
        }
    }

    public void setLeAudioCodecConfigPreference(Integer groupId,
                             BluetoothLeAudioCodecConfig inputCodecConfig,
                             BluetoothLeAudioCodecConfig outputCodecConfig) {

        BluetoothDevice device = null;
        if (mActiveDeviceManager != null) {
            Log.d(TAG, "setLeAudioCodecConfigPreference: get currect Active device for LE Audio profile");
            device =
                mActiveDeviceManager.getActiveAbsoluteDevice(ApmConst.AudioFeatures.MEDIA_AUDIO);
        }

        if (device == null) {
            Log.e(TAG, "setLeAudioCodecConfigPreference: device id null, return");
            return;
        }

        if (groupId == LE_AUDIO_GROUP_ID_INVALID) {
            Log.e(TAG, "request came for invalid groupId return");
            return;
        }

        if (inputCodecConfig == null && outputCodecConfig == null) {
            Log.e(TAG, "setLeAudioCodecConfigPreference: Both in and out coedec config are null, return");
            return;
        }

/*
        // TO-DO :Uncomment this snippet based on future Google impl ??
        LeAudioService mLeService = LeAudioService.getLeAudioService();
        BluetoothDevice lead_device = (mLeService != null) ?
                mLeService.getConnectedGroupLeadDevice(groupId) : null;
        Log.i(TAG, "getLeAudioCodecStatus: groupId: " + groupId + " device: " + lead_device);
        if (device != lead_device) {
            Log.e(TAG, "setLeAudioCodecConfigPreference: request came for non-active group ignore");
            return;
        }
*/

        long inputCS4 = inputCodecConfig.getCodecSpecific4();
        long outputCS4 = outputCodecConfig.getCodecSpecific4();

        if (mActiveDeviceManager.getActiveProfile(ApmConst.AudioFeatures.MEDIA_AUDIO) ==
                                                      ApmConst.AudioProfiles.BROADCAST_LE) {
            AcmService mAcmService = AcmService.getAcmService();
            BluetoothDevice mPrevDevice =
                mActiveDeviceManager.getQueuedDevice(ApmConst.AudioFeatures.MEDIA_AUDIO);

            if (mPrevDevice != null && mAcmService != null) {
                if ((inputCS4 & AUDIO_RECORDING_MASK) == AUDIO_RECORDING_ON) {
                    if (mAcmService.getConnectionState(mPrevDevice) ==
                                              BluetoothProfile.STATE_CONNECTED) {
                        device = mPrevDevice;
                        Log.d(TAG,"Recording request, switch device to " + device);
                    } else {
                        Log.d(TAG,"Not DUMO device, ignore recording request");
                        return;
                    }
                }
            } else if ((outputCS4 & GAMING_MODE_MASK) == GAMING_ON) {
                Log.d(TAG, "Ignore gaming mode request when broadcast is active");
                return;
            }
        }

        int req_codec_type = outputCodecConfig.getCodecType();
        int req_sampling_freq = outputCodecConfig.getSampleRate();
        int frame_duration = 0; // TO-DO Add later
        int octects_per_frame = 0; // TO-DO Add later
        String codec_name;
            if (req_codec_type == BluetoothLeAudioCodecConfig.SOURCE_CODEC_TYPE_APTX_ADAPTIVE_LE)
              codec_name = "APTXLE_";
            else if (req_codec_type == FWK_SOURCE_CODEC_TYPE_APTX_ADAPTIVE_R4)/*Temp. changes*/
              codec_name = "AAR4_";
            else if (req_codec_type == FWK_SOURCE_CODEC_TYPE_DEFAULT)/*Temp. changes*/
              codec_name = "DEFAULT_";
            else
              codec_name = "LC3_";
        String sample_freq = SampleRateToString(req_sampling_freq);
        String dummy_octets = "_0";
        String dummy_frameduration = "_0";
        // Request config is combo of codectype_freq_octects_framedur
        StringBuilder sb_config = (new StringBuilder())
                                   .append(codec_name)
                                   .append(sample_freq)
                                   .append(dummy_octets)
                                   .append(dummy_frameduration);
        String requested_config = sb_config.toString();
        Log.w(TAG, " Requested config : " + requested_config);

        DeviceProfileMap dMap = DeviceProfileMap.getDeviceProfileMapInstance();
        int supported_prfiles = dMap.getAllSupportedProfile(device);

        MediaDevice mMediaDevice = mMediaDevices.get(device.getAddress());
        boolean peer_supports_recording =
                ((supported_prfiles & ApmConst.AudioProfiles.BAP_RECORDING) != 0);

        Log.i(TAG, "peer_supports_recording: " + peer_supports_recording);

        int profileIndex =
              mMediaDevice.getProfileIndex(ApmConst.AudioProfiles.BAP_MEDIA);
        int profileIndexA2dp =
              mMediaDevice.getProfileIndex(ApmConst.AudioProfiles.A2DP);
        boolean is_peer_connected_for_recording =
                    (mMediaDevice.profileConnStatus[profileIndex] ==
                                       BluetoothProfile.STATE_CONNECTED);
        boolean is_peer_connected_for_a2dp =
                    (mMediaDevice.profileConnStatus[profileIndexA2dp] ==
                                          BluetoothProfile.STATE_CONNECTED);

        Log.i(TAG, "is_peer_connected_for_recording: " +
                    is_peer_connected_for_recording +
                   ", is_peer_connected_for_a2dp: " +
                   is_peer_connected_for_a2dp);
        Log.i(TAG, " mGamingMode: " + mGamingMode);

        CallAudio mCallAudio = CallAudio.get();
        boolean isInCall =
                  mCallAudio != null && mCallAudio.isVoiceOrCallActive();

        boolean isBapConnected =
                 (mMediaDevice.profileConnStatus[mMediaDevice.LE_STREAM] ==
                                           BluetoothProfile.STATE_CONNECTED);

        Log.i(TAG, "is_bap_connected " + isBapConnected +
                   ", call in progress: " + isInCall);

        // TODO : check the FM related rx activity
        if (mActiveDeviceManager != null &&
            /*peer_supports_recording &&*/ mIsRecordingEnabled &&
            is_peer_connected_for_recording && inputCS4 != 0) {
            long mStereoRecordingStatus = (inputCS4 & AUDIO_RECORDING_MASK);
            VolumeManager mVolumeManager = VolumeManager.get();
            if (mVolumeManager.getBluetoothContextualVolumeStream() ==
                  mAudioManager.STREAM_VOICE_CALL) {
                isInCall = true;
            }
            Log.i(TAG, "mStereoRecordingStatus: " + mStereoRecordingStatus);
            if ((inputCS4 & AUDIO_RECORDING_MASK) == AUDIO_RECORDING_ON &&
                (inputCS4 & GAME_AUDIO_RECORDING_MASK) != GAME_AUDIO_RECORDING_ON) {
                if(!isInCall &&
                   !mActiveDeviceManager.isRecordingActive(device) &&
                   // !mActiveDeviceManager.isGamingActive(device) &&
                   (mGamingMode == GAME_NONE)) {
                  mActiveDeviceManager.enableRecording(device);
                } else {
                    Log.d(TAG,"Ignore recording request, voice/voip call active");
                }
            } else if ((inputCS4 & AUDIO_RECORDING_MASK) == AUDIO_RECORDING_OFF) {
                if(mActiveDeviceManager.isRecordingActive(device)) {
                  mActiveDeviceManager.disableRecording(device);
                }
            }
        }

        int profileID = dMap.getProfile(device, ApmConst.AudioFeatures.MEDIA_AUDIO);

        boolean peer_supports_gaming =
                    ((supported_prfiles & ApmConst.AudioProfiles.BAP_GCP) != 0);

        Log.i(TAG, "peer_supports_gaming: " + peer_supports_gaming);

        if (isBapConnected && outputCS4 != 0 &&/*peer_supports_gaming &&*/
          ((outputCS4 & APTX_ULL) != APTX_ULL)) {
            long mGamingStatus = (outputCS4 & GAMING_MODE_MASK);
            long mGamingVbcStatus = (inputCS4 & GAME_AUDIO_RECORDING_MASK);

            Log.i(TAG, " mGamingStatus: " + mGamingStatus +
                       ", mGamingVbcStatus: " + mGamingVbcStatus);

            boolean peer_supports_gaming_vbc =
                    ((supported_prfiles & ApmConst.AudioProfiles.BAP_GCP_VBC) != 0);

            Log.i(TAG, " mIsGamingVbcEnabledDefault: " + mIsGamingVbcEnabledDefault +
                       ", peer_supports_gaming_vbc: " + peer_supports_gaming_vbc);

            if (!mIsGamingVbcEnabledDefault) {// default prop set to false
                if ((mGamingVbcStatus & GAME_AUDIO_RECORDING_ON) > 0 && !isInCall) {
                    if (!peer_supports_gaming_vbc) {
                        // When ALS detects VBC on, but remote doesn't support vbc,
                        // then enable Gaming Tx path only.
                        Log.w(TAG, "Remote not supports VBC, Turning On Gaming Tx Mode");
                        if ((mGamingMode & GAME_TX_MODE) == GAME_TX_MODE) {
                            Log.w(TAG, "enableGaming is already called");
                            return;
                        }
                        mGamingMode = GAME_TX_MODE;
                        mActiveDeviceManager.enableGaming(device);
                        return;
                    } else {
                        // When ALS detects VBC on, and remote support vbc,
                        // then enable both, Gaming Tx and Rx path.
                        Log.w(TAG, "Remote supports VBC,Turning On Gaming Vbc Mode");
                        if ((mGamingMode & GAME_TX_RX_MODE) == GAME_TX_RX_MODE) {
                          Log.w(TAG, "enableGamingVbc is already called");
                          return;
                        }
                        mGamingMode = GAME_TX_RX_MODE;
                        mActiveDeviceManager.enableGamingVbc(device);
                        return;
                    }
                } else if ((mGamingStatus & GAMING_ON) > 0 && !isInCall) {
                    // When ALS detects Only Gaming Tx
                    // then enable Gaming Tx path only.
                    Log.w(TAG, "Turning On Gaming Tx Mode, mGamingMode: " + mGamingMode);
                    if ((mGamingMode & GAME_TX_MODE) == GAME_TX_MODE) {
                        Log.w(TAG, "enableGaming is already called");
                        return;
                    }
                    mGamingMode = GAME_TX_MODE;
                    mActiveDeviceManager.enableGaming(device);
                    return;
                } else if (((mGamingVbcStatus & GAME_AUDIO_RECORDING_OFF) > 0) &&
                   (req_codec_type == BluetoothLeAudioCodecConfig.SOURCE_CODEC_TYPE_LC3)) {
                    // When ALS detects VBC off,
                    // then reset Rx Mode and check for both gaming Tx and Rx off.
                    // If both are off, then trigger disable gaming.
                    Log.w(TAG, "Reset Gaming Rx Mode, mGamingMode: " + mGamingMode);
                    mGamingMode &= ~GAME_RX_MODE;
                    if (mGamingMode == GAME_NONE) {
                        if(mIgnoreALSLLtoHQ) {
                            Log.w(TAG, "Not turning Off Gaming Tx/Rx Mode");
                        } else {
                            Log.w(TAG, "Turning Off Gaming Tx/Rx Mode");
                            mActiveDeviceManager.disableGaming(device);
                        }
                        return;
                    }
                }
            } else {// default prop set to true
                if (((mGamingVbcStatus & GAME_AUDIO_RECORDING_ON) > 0 && !isInCall)) {
                    if (peer_supports_gaming_vbc) {
                        Log.w(TAG, "Turning On Gaming Vbc Mode");
                        mActiveDeviceManager.enableGamingVbc(device);
                        return;
                    } else {
                        Log.w(TAG, "Fallback to Gaming Tx mode, " +
                                   "when remote don't have vbc support");
                        mActiveDeviceManager.enableGaming(device);
                        return;
                    }
                }
            }

            if (((mGamingStatus & GAMING_OFF) > 0) &&
                (req_codec_type == BluetoothLeAudioCodecConfig.SOURCE_CODEC_TYPE_LC3)) {
                // When ALS detects Gaming off,
                // then reset Tx Mode and check for both gaming Tx and Rx off.
                // If both are off, then trigger disable gaming.
                if (mActiveDeviceManager.isGamingActive(device)) {
                    Log.w(TAG, "Reset Gaming Tx Mode, mGamingMode: " + mGamingMode);
                    mGamingMode &= ~GAME_TX_MODE;
                    if (mGamingMode == GAME_NONE) {
                        if(mIgnoreALSLLtoHQ) {
                            Log.w(TAG, "Not turning Off Gaming Tx or Tx/Rx Mode");
                        } else {
                            Log.w(TAG, "Turning Off Gaming Tx or Tx/Rx Mode");
                            mActiveDeviceManager.disableGaming(device);
                        }
                    }
                    return;
                }
            }
        }
        if (isInCall) {
          LeAudioService mLeaService = LeAudioService.getLeAudioService();
          BluetoothDevice mDevice = (mLeaService != null) ?
                          mLeaService.getConnectedGroupLeadDevice(groupId) : null;
          BluetoothDevice mVoiceActiveDevice =
                  mActiveDeviceManager.getActiveAbsoluteDevice(ApmConst.AudioFeatures.CALL_AUDIO);
          if (Objects.equals(mVoiceActiveDevice, mDevice)) {
              Log.d(TAG, "Call active ignore codec switch");
              return;
          }
        }
        // For now only allow 48k/96k AptX freq switch
        StreamAudioService mStreamService = StreamAudioService.getStreamAudioService();
        boolean is_csip_device = false;
        if (mStreamService.getDeviceGroup(device) != null) {
            is_csip_device = ((device != null) &&
                (mStreamService.getDeviceGroup(device).getAddress().contains("9E:8B:00:00:00")));
        }
        Log.w(TAG, "Req cfg for " + ((is_csip_device == true) ? "csip" : "non-csip") + " device");
        Log.w(TAG, "Requested config string from ALS/Dev UI is " + requested_config);

        // TO-DO: Generalise condition for DEV-UI updates
        if ((req_codec_type == BluetoothLeAudioCodecConfig.SOURCE_CODEC_TYPE_APTX_ADAPTIVE_LE)
            && (is_csip_device == false) &&
            ((req_sampling_freq == BluetoothLeAudioCodecConfig.SAMPLE_RATE_96000) ||
            (req_sampling_freq == BluetoothLeAudioCodecConfig.SAMPLE_RATE_48000))) {
            Log.w(TAG, "Trigger freq switch for AptX LE codec for sample rate " +
                      SampleRateToString(req_sampling_freq));
            mStreamService.setCodecConfig(device, requested_config, 0);
        } else if ((req_codec_type == FWK_SOURCE_CODEC_TYPE_APTX_ADAPTIVE_R4)/*Temp. changes*/
            && (is_csip_device == false)) {
            Log.w(TAG, "AAR4 Config R4 Codec");
            mAdapterService.getDatabase().setAAR4Status(device, true);
            mStreamService.setCodecConfig(device, requested_config, 0);
        } else if ((req_codec_type == FWK_SOURCE_CODEC_TYPE_DEFAULT)/*Temp. changes*/
            && (is_csip_device == false)) {
            Log.w(TAG, "AAR4 Config Default Codec");
            mAdapterService.getDatabase().setAAR4Status(device, false);
            mStreamService.setCodecConfig(device, requested_config, 0);
        }
    }

    public void handleCacheGamingOff(BluetoothDevice device) {
        Log.i(TAG, "handleCacheGamingOff: mIsCacheGamingOff is " + mIsCacheGamingOff);
        ActiveDeviceManagerService mActiveDeviceManager = ActiveDeviceManagerService.get();
        if (mIsCacheGamingOff && mCacheGamingDevice !=null)  {
            // Called when hang up call.
            // then reset Tx Mode and check for both gaming Tx and Rx off.
            //If both are off, then trigger disable gaming.
            if (mActiveDeviceManager != null) {
                if (mActiveDeviceManager.isGamingActive(mCacheGamingDevice)) {
                    Log.w(TAG, "Reset Gaming Tx Mode, mGamingMode: " + mGamingMode);
                    mGamingMode &= ~GAME_TX_MODE;
                    mIsCacheGamingOff = false;
                    if (mGamingMode == GAME_NONE) {
                        Log.w(TAG, "Turning Off Gaming Tx or Tx/Rx Mode , device " + mCacheGamingDevice);
                        mActiveDeviceManager.disableGaming(mCacheGamingDevice);
                    }
                 mCacheGamingDevice = null;
                }
            }
        }
        return;
    }

    public boolean isGamingVbcEnabledDefault() {
        Log.i(TAG, " mIsGamingVbcEnabledDefault: " + mIsGamingVbcEnabledDefault);
        return mIsGamingVbcEnabledDefault;
    }

    public void enableOptionalCodecs(BluetoothDevice device) {
        Log.i(TAG, "enableOptionalCodecs: ");

        BluetoothCodecStatus mCodecStatus = null;

        if (device == null) {
            if(mActiveDeviceManager != null) {
                device = mActiveDeviceManager.getActiveDevice(ApmConst.AudioFeatures.MEDIA_AUDIO);
            }
        }
        if (device == null) {
            Log.e(TAG, "enableOptionalCodecs: Invalid device");
            return;
        }

        if (getSupportsOptionalCodecs(device) != BluetoothA2dp.OPTIONAL_CODECS_SUPPORTED) {
            Log.e(TAG, "enableOptionalCodecs: No optional codecs");
            return;
        }

        MediaDevice mMediaDevice = mMediaDevices.get(device.getAddress());

        A2dpService service = A2dpService.getA2dpService();
        if(service != null) {
            int profileIndex = mMediaDevice.getProfileIndex(ApmConst.AudioProfiles.A2DP);
            mCodecStatus = mMediaDevice.mProfileCodecStatus[profileIndex];
            if(mCodecStatus != null) {
                service.enableOptionalCodecsA2dp(device, mCodecStatus);
            }
        }
        // 2 Should implement common codec handling when
        //vendor codecs is introduced in LE Audio
    }

    public void disableOptionalCodecs(BluetoothDevice device) {
        Log.i(TAG, "disableOptionalCodecs: ");
        BluetoothCodecStatus mCodecStatus = null;
        if (device == null) {
            if(mActiveDeviceManager != null) {
                device = mActiveDeviceManager.getActiveDevice(ApmConst.AudioFeatures.MEDIA_AUDIO);
            }
        }
        if (device == null) {
            Log.e(TAG, "disableOptionalCodecs: Invalid device");
            return;
        }

        if (getSupportsOptionalCodecs(device) != BluetoothA2dp.OPTIONAL_CODECS_SUPPORTED) {
            Log.e(TAG, "disableOptionalCodecs: No optional codecs");
            return;
        }

        MediaDevice mMediaDevice = mMediaDevices.get(device.getAddress());

        A2dpService service = A2dpService.getA2dpService();
        if(service != null) {
            int profileIndex = mMediaDevice.getProfileIndex(ApmConst.AudioProfiles.A2DP);
            mCodecStatus = mMediaDevice.mProfileCodecStatus[profileIndex];
            if(mCodecStatus != null) {
                service.disableOptionalCodecsA2dp(device, mCodecStatus.getCodecConfig());
            }
        }
        // 2 Should implement common codec handling when
        //vendor codecs is introduced in LE Audio
    }

    public int getSupportsOptionalCodecs(BluetoothDevice device) {
        if(mAdapterService != null)
            return mAdapterService.getDatabase().getA2dpSupportsOptionalCodecs(device);
        return BluetoothA2dp.OPTIONAL_CODECS_NOT_SUPPORTED;
    }

    public int supportsOptionalCodecs(BluetoothDevice device) {
        BluetoothCodecStatus codecStatus = getCodecStatus(device);
        if (codecStatus == null) {
            Log.w(TAG, "supportsOptionalCodecs: unkown codec");
            return BluetoothA2dp.OPTIONAL_CODECS_SUPPORT_UNKNOWN;
        }
        if(codecStatus.getCodecConfig().getCodecType() ==
                        BluetoothCodecConfig.SOURCE_CODEC_TYPE_LC3)
        {
            Log.w(TAG, "LE Audio active, HD Audio Option Disabled");
            return BluetoothA2dp.OPTIONAL_CODECS_NOT_SUPPORTED;
        }
        if(mAdapterService.isTwsPlusDevice(device)) {
            Log.w(TAG, "Disable optional codec support for TWS+ device");
            return BluetoothA2dp.OPTIONAL_CODECS_NOT_SUPPORTED;
        }
        return getSupportsOptionalCodecs(device);
    }

    public int getOptionalCodecsEnabled(BluetoothDevice device) {
        if(mAdapterService != null)
            return mAdapterService.getDatabase().getA2dpOptionalCodecsEnabled(device);
        return BluetoothA2dp.OPTIONAL_CODECS_PREF_UNKNOWN;
    }

    public void setOptionalCodecsEnabled(BluetoothDevice device, Integer value) {
        Log.i(TAG, "setOptionalCodecsEnabled: " + value);
        if (value != BluetoothA2dp.OPTIONAL_CODECS_PREF_UNKNOWN
                && value != BluetoothA2dp.OPTIONAL_CODECS_PREF_DISABLED
                && value != BluetoothA2dp.OPTIONAL_CODECS_PREF_ENABLED) {
            Log.w(TAG, "Unexpected value passed to setOptionalCodecsEnabled:" + value);
            return;
        }

        if(mAdapterService != null)
            mAdapterService.getDatabase().setA2dpOptionalCodecsEnabled(device, value);
    }

    public int getConnectionPolicy(BluetoothDevice device) {
        if(mAdapterService != null)
            return mAdapterService.getDatabase()
                .getProfileConnectionPolicy(device, BluetoothProfile.A2DP);
        return BluetoothProfile.CONNECTION_POLICY_UNKNOWN;
    }

    @RequiresPermission(android.Manifest.permission.BLUETOOTH_PRIVILEGED)
    public boolean setConnectionPolicy(BluetoothDevice device, Integer connectionPolicy) {
        mContext.enforceCallingOrSelfPermission(BLUETOOTH_PRIVILEGED,
                "Need BLUETOOTH_PRIVILEGED permission");
        if (DBG) {
            Log.d(TAG, "Saved connectionPolicy " + device + " = " + connectionPolicy);
        }
        boolean setSuccessfully;
        setSuccessfully = mAdapterService.getDatabase()
                .setProfileConnectionPolicy(device, BluetoothProfile.A2DP, connectionPolicy);

        if (setSuccessfully && connectionPolicy == BluetoothProfile.CONNECTION_POLICY_ALLOWED) {
            connect(device);
        } else if (setSuccessfully
                && connectionPolicy == BluetoothProfile.CONNECTION_POLICY_FORBIDDEN) {
            disconnect(device);
        }
        return setSuccessfully;
    }

    public boolean setSilenceMode(BluetoothDevice device, Boolean silence) {
        if (DBG) {
            Log.d(TAG, "setSilenceMode(" + device + "): " + silence);
        }
        BluetoothDevice mActiveDevice = mActiveDeviceManager.getActiveDevice(ApmConst.AudioFeatures.MEDIA_AUDIO);
        if (silence && Objects.equals(mActiveDevice, device)) {
            mActiveDeviceManager.removeActiveDevice(ApmConst.AudioFeatures.MEDIA_AUDIO, true);
        } else if (!silence && null ==
                mActiveDeviceManager.getActiveDevice(ApmConst.AudioFeatures.MEDIA_AUDIO)) {
            // Set the device as the active device if currently no active device.
            mActiveDeviceManager.setActiveDevice(device, ApmConst.AudioFeatures.MEDIA_AUDIO, false);
        }
        return true;
    }

    public void setGamingMode(int mode) {
        if (DBG) {
            Log.d(TAG, "setGamingMode: " + mode + ", mGamingMode: " + mGamingMode);
        }
        mGamingMode = mode;
        return;
    }

    public int getProfileConnectionState(BluetoothDevice device, int profile) {
        MediaDevice mMediaDevice = mMediaDevices.get(device.getAddress());
        if(mMediaDevice == null)
            return BluetoothProfile.STATE_DISCONNECTED;

        return mMediaDevice.profileConnStatus[mMediaDevice.getProfileIndex(profile)];
    }

    boolean setActiveProfile(BluetoothDevice device, int profile) {
        if (device == null) {
            return false;
        }

        if(getProfileConnectionState(device, profile) == BluetoothProfile.STATE_DISCONNECTED) {
            return false;
        }

        int volProfile = (profile == ApmConst.AudioProfiles.A2DP) ?
                        ApmConst.AudioProfiles.AVRCP : ApmConst.AudioProfiles.VCP;
        DeviceProfileMap dMap = DeviceProfileMap.getDeviceProfileMapInstance();
        dMap.setActiveProfile(device, ApmConst.AudioFeatures.MEDIA_AUDIO, profile);
        dMap.setActiveProfile(device, ApmConst.AudioFeatures.MEDIA_VOLUME_CONTROL, volProfile);

        StreamAudioService streamAudioService = StreamAudioService.getStreamAudioService();
        BluetoothDevice groupDevice = streamAudioService.getDeviceGroup(device);
        if(!device.equals(groupDevice)) {
            dMap.setActiveProfile(groupDevice, ApmConst.AudioFeatures.MEDIA_AUDIO, profile);
            dMap.setActiveProfile(groupDevice, ApmConst.AudioFeatures.MEDIA_VOLUME_CONTROL, volProfile);
        }

        mActiveDeviceManager.setActiveDevice(device, ApmConst.AudioFeatures.MEDIA_AUDIO, false);
        return true;
    }

    public void onConnStateChange(BluetoothDevice device, Integer state, Integer profile) {
        Log.d(TAG, "onConnStateChange: profile: " + profile +
                   " state: " + state + " for device " + device);
        if(device == null)
            return;
        MediaDevice mMediaDevice = mMediaDevices.get(device.getAddress());
        StreamAudioService mStreamAudioService = StreamAudioService.getStreamAudioService();
        BluetoothDevice groupDevice = mStreamAudioService.getDeviceGroup(device);

        Log.d(TAG, "onConnStateChange: mMediaDevice: " + mMediaDevice);

        if(mMediaDevice == null) {
            if(state == BluetoothProfile.STATE_DISCONNECTED)
                return;
            if(mMediaDevices.size() >= MAX_DEVICES) {
                Log.d(TAG, "onConnStateChange: reached Max devices");
                return;
            }
            mMediaDevice = new MediaDevice(device, profile, state);
            mMediaDevices.put(device.getAddress(), mMediaDevice);
            if (!ApmConst.getQtiLeAudioEnabled()) {
                Log.d(TAG, "onConnStateChange: Don't broadcast here for AOSP LeAudio Media Profile");
            } else {
                broadcastConnStateChange(device,
                                         BluetoothProfile.STATE_DISCONNECTED, state);
            }
            return;
        }

        int profileIndex = mMediaDevice.getProfileIndex(profile);
        int prevState = mMediaDevice.deviceConnStatus;
        Log.w(TAG, "onConnStateChange: prevState: " + prevState);
        if(mMediaDevice.profileConnStatus[profileIndex] == state) {
            Log.w(TAG, "Profile already in state: " + state + ". Return");
            return;
        }
        mMediaDevice.profileConnStatus[profileIndex] = state;

        DeviceProfileMap dMap = DeviceProfileMap.getDeviceProfileMapInstance();
        if(state == BluetoothProfile.STATE_CONNECTED) {
            dMap.profileConnectionUpdate(device,
                                         ApmConst.AudioFeatures.MEDIA_AUDIO,
                                         profile, true);
            if (ApmConst.getAospLeaEnabled() &&
                    profile == ApmConst.AudioProfiles.A2DP) {
                Log.d(TAG, "onConnStateChange: Don's refresh codec here for A2dp in AOSP LeAudio");
            } else {
                refreshCurrentCodec(device, PROFILE_UNKNOWN);
            }
            if(!device.equals(groupDevice)) {
                /*Update group connection state at DPM*/
                dMap.profileConnectionUpdate(groupDevice, ApmConst.AudioFeatures.MEDIA_AUDIO, profile, true);
            }
        } else if(state == BluetoothProfile.STATE_DISCONNECTED) {
            dMap.profileConnectionUpdate(device,
                                         ApmConst.AudioFeatures.MEDIA_AUDIO,
                                         profile, false);

            if((!device.equals(groupDevice)) && ApmConst.AudioProfiles.A2DP == profile) {
                /*Update group connection state at DPM*/
                dMap.profileConnectionUpdate(groupDevice, ApmConst.AudioFeatures.MEDIA_AUDIO, profile, false);
            }
        }

        if ((profileIndex+1)%2 == 0) {
            Log.w(TAG, " otherProfileConnection: LE");
        } else {
            Log.w(TAG, " otherProfileConnection: A2DP");
        }

        int otherProfileConnectionState =
                              mMediaDevice.profileConnStatus[(profileIndex+1)%2];
        Log.w(TAG, " otherProfileConnectionState: " + otherProfileConnectionState);

        if (!ApmConst.getQtiLeAudioEnabled()) {
            if (profile == ApmConst.AudioProfiles.BAP_MEDIA) {
                if (state == BluetoothProfile.STATE_DISCONNECTED) {
                    mMediaDevice.deviceConnStatus = state;
                }
            }
            Log.w(TAG, "AOSP LE Media profile enabled, return ");
            return;
        }

        switch(otherProfileConnectionState) {
        /*Send Broadcast based on state of other profile*/
            case BluetoothProfile.STATE_DISCONNECTED:
                Log.w(TAG, "otherProfile is disconnected");
                broadcastConnStateChange(device, prevState, state);
                mMediaDevice.deviceConnStatus = state;
                if(state == BluetoothProfile.STATE_CONNECTED && ApmConst.getQtiLeAudioEnabled()) {
                    int supportedProfiles = dMap.getSupportedProfile(device,
                                                    ApmConst.AudioFeatures.MEDIA_AUDIO);
                    if(profile == ApmConst.AudioProfiles.A2DP &&
                            (supportedProfiles & ApmConst.AudioProfiles.BAP_MEDIA) ==
                                                    ApmConst.AudioProfiles.BAP_MEDIA) {
                        Log.w(TAG, "Connect LE Media after A2DP auto connect from remote");
                        StreamAudioService mStreamService = StreamAudioService.
                                                                    getStreamAudioService();
                        if(mStreamService != null) {
                            mStreamService.connectLeStream(groupDevice,
                                                    ApmConst.AudioProfiles.BAP_MEDIA);
                        }
                    } else {
                        ActiveDeviceManagerService mActiveDeviceManager = ActiveDeviceManagerService.get();
                        if(mActiveDeviceManager != null) {
                            mActiveDeviceManager.setActiveDevice(device,
                                                    ApmConst.AudioFeatures.MEDIA_AUDIO);
                        }
                    }
                }
                break;
            case BluetoothProfile.STATE_CONNECTING:
                Log.w(TAG, "otherProfile is connecting");
                if(state == BluetoothProfile.STATE_CONNECTED) {
                    broadcastConnStateChange(device, prevState, state);
                    mMediaDevice.deviceConnStatus = state;
                } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                    ActiveDeviceManagerService mActiveDeviceManager =
                            ActiveDeviceManagerService.get();
                    if (mActiveDeviceManager != null) {
                        BluetoothDevice currentActiveDevice =
                            mActiveDeviceManager.getActiveAbsoluteDevice(ApmConst.AudioFeatures.MEDIA_AUDIO);
                        if (device.equals(currentActiveDevice)) {
                            Log.w(TAG, "onConnStateChange: "+
                                       "Trigger Media SHO to null.");
                            mActiveDeviceManager.setActiveDevice(null,
                                                      ApmConst.AudioFeatures.MEDIA_AUDIO);
                        }
                    }
                }
                break;
            case BluetoothProfile.STATE_DISCONNECTING:
                Log.w(TAG, "otherProfile is disconnecting");
                if(state == BluetoothProfile.STATE_CONNECTING ||
                        state == BluetoothProfile.STATE_CONNECTED) {
                    broadcastConnStateChange(device, prevState, state);
                    mMediaDevice.deviceConnStatus = state;
                }
                break;
            case BluetoothProfile.STATE_CONNECTED:
                Log.w(TAG, "otherProfile is connected");
                ActiveDeviceManagerService mActiveDeviceManager =
                        ActiveDeviceManagerService.get();
                if(mActiveDeviceManager == null) {
                    break;
                }

                BluetoothDevice mActiveDevice = mActiveDeviceManager
                            .getActiveDevice(ApmConst.AudioFeatures.MEDIA_AUDIO);

                if(mAdapterService.getState() == BluetoothAdapter.STATE_ON &&
                            ((state == BluetoothProfile.STATE_CONNECTED) ||
                            (state == BluetoothProfile.STATE_DISCONNECTED &&
                            device.equals(mActiveDevice)))) {
                    Log.w(TAG, "onConnStateChange: "+
                               "Trigger Media handoff for Device: " + device);
                    mActiveDeviceManager.setActiveDevice(device,
                                              ApmConst.AudioFeatures.MEDIA_AUDIO);
                }
                break;
        }

        if (profileIndex == mMediaDevice.LE_STREAM &&
             state == BluetoothProfile.STATE_DISCONNECTED) {
            mMediaDevice.mProfileCodecStatus[profileIndex] = null;
        }
    }

    public void onConnStateChange(BluetoothDevice device, int state,
                                  int profile, boolean isFirstMember) {
        Log.w(TAG, "onConnStateChange: state:" + state + " for device " + device +
                   " new group: " + isFirstMember);
        if((state == BluetoothProfile.STATE_CONNECTED ||
            state == BluetoothProfile.STATE_CONNECTING) &&
            isFirstMember) {
            StreamAudioService mStreamAudioService = StreamAudioService.getStreamAudioService();
            BluetoothDevice groupDevice = mStreamAudioService.getDeviceGroup(device);
            Log.w(TAG, "onConnStateChange: groupDevice:" + groupDevice);
            if(groupDevice != null) {
                MediaDevice mMediaDevice = mMediaDevices.get(groupDevice.getAddress());
                Log.w(TAG, "onConnStateChange: mMediaDevice:" + mMediaDevice);
                if(mMediaDevice == null) {
                    mMediaDevice = new MediaDevice(groupDevice, profile,
                                                   BluetoothProfile.STATE_CONNECTED);
                    mMediaDevices.put(groupDevice.getAddress(), mMediaDevice);
                } else {
                    int profileIndex = mMediaDevice.getProfileIndex(profile);
                    mMediaDevice.profileConnStatus[profileIndex] =
                                                    BluetoothProfile.STATE_CONNECTED;
                    mMediaDevice.deviceConnStatus = state;
                }
            }
        } else if(isFirstMember && (state == BluetoothProfile.STATE_DISCONNECTING ||
                        state == BluetoothProfile.STATE_DISCONNECTED)) {
            StreamAudioService mStreamAudioService =
                                       StreamAudioService.getStreamAudioService();
            BluetoothDevice groupDevice = mStreamAudioService.getDeviceGroup(device);
            MediaDevice mMediaDevice = (groupDevice == null) ?
                                        null : mMediaDevices.get(groupDevice.getAddress());
            int prevState = BluetoothProfile.STATE_CONNECTED;
            Log.w(TAG, "onConnStateChange: mMediaDevice: " + mMediaDevice);
            if(mMediaDevice != null) {
                prevState = mMediaDevice.deviceConnStatus;
                int profileIndex = mMediaDevice.getProfileIndex(profile);
                mMediaDevice.profileConnStatus[profileIndex] = state;
                mMediaDevice.deviceConnStatus = state;
                Log.w(TAG, "onConnStateChange: device: " + groupDevice +
                           " state = " +  mMediaDevice.deviceConnStatus);
            }
            ActiveDeviceManager mDeviceManager =
                           AdapterService.getAdapterService().getActiveDeviceManager();
            mDeviceManager.onDeviceConnStateChange(groupDevice, state, prevState,
                            ApmConst.AudioFeatures.MEDIA_AUDIO);
            if(!device.equals(groupDevice)) {
                /*Update group connection state at DPM*/
                DeviceProfileMap dMap = DeviceProfileMap.getDeviceProfileMapInstance();
                dMap.profileConnectionUpdate(groupDevice, ApmConst.AudioFeatures.MEDIA_AUDIO, profile, false);
            }
        }
        onConnStateChange(device, state, profile);
    }

    public int getGroupConnState(BluetoothDevice device) {
        StreamAudioService mStreamAudioService = StreamAudioService.getStreamAudioService();
        BluetoothDevice groupDevice = mStreamAudioService.getDeviceGroup(device);
        if(groupDevice != null) {
            MediaDevice mMediaDevice = mMediaDevices.get(groupDevice.getAddress());
            if(mMediaDevice != null) {
                Log.w(TAG, "getGroupConnState: device: " + groupDevice + " state = " +  mMediaDevice.deviceConnStatus);
                int connState = mMediaDevice.deviceConnStatus;
                return connState;
            }
        }
        return BluetoothProfile.STATE_DISCONNECTED;
    }

    public void onStreamStateChange(BluetoothDevice device, Integer streamStatus) {
        int prevStatus;
        Log.d(TAG, "onStreamStateChange: mGamingEnabled: " + mGamingEnabled);
        if (streamStatus == BluetoothA2dp.STATE_NOT_PLAYING && mGamingEnabled == true) {
            Log.d(TAG, "onStreamStateChange: Disabling Gaming Mode");
            mGamingEnabled = false;
        }
        A2dpService mA2dpService = A2dpService.getA2dpService();
        if(mA2dpService != null && !mA2dpService.isQtiLeAudioEnabled()) {
            return;
        }

        if(device == null)
            return;
        MediaDevice mMediaDevice = mMediaDevices.get(device.getAddress());
        if(mMediaDevice == null) {
            return;
        }

        if(mMediaDevice.streamStatus == streamStatus)
            return;

        prevStatus = mMediaDevice.streamStatus;
        mMediaDevice.streamStatus = streamStatus;
        Log.d(TAG, "onStreamStateChange update to volume manager");
        VolumeManager mVolumeManager = VolumeManager.get();
        mVolumeManager.updateStreamState(device, streamStatus, ApmConst.AudioFeatures.MEDIA_AUDIO);
        CCService mCCService = CCService.getCCService();
        if (mCCService != null) {
            mCCService.onUnicastStateUpdate(streamStatus);
        }
        broadcastStreamState(device, prevStatus, streamStatus);
    }

    protected BluetoothCodecStatus convergeCodecConfig(MediaDevice mMediaDevice, Integer profile) {
        BluetoothCodecStatus A2dpCodecStatus = mMediaDevice.mProfileCodecStatus[MediaDevice.A2DP_STREAM];
        BluetoothCodecStatus BapCodecStatus = mMediaDevice.mProfileCodecStatus[MediaDevice.LE_STREAM];
        BluetoothCodecStatus mCodecStatus = null;

        if(A2dpCodecStatus == null ||
           mMediaDevice.profileConnStatus[MediaDevice.A2DP_STREAM] !=
                                       BluetoothProfile.STATE_CONNECTED) {
            return BapCodecStatus;
        }

        if(BapCodecStatus == null ||
           mMediaDevice.profileConnStatus[MediaDevice.LE_STREAM] !=
                                       BluetoothProfile.STATE_CONNECTED) {
            return A2dpCodecStatus;
        }

        ActiveDeviceManagerService mActiveDeviceManager = ActiveDeviceManagerService.get();
        int mActiveProfile = profile;
        if (mActiveProfile == -1)
            mActiveProfile = mActiveDeviceManager.getActiveProfile(ApmConst.AudioFeatures.MEDIA_AUDIO);
        int mActiveProfileIndex = mMediaDevice.getProfileIndex(mActiveProfile);
        BluetoothCodecConfig mCodecConfig = mMediaDevice.mProfileCodecStatus[mActiveProfileIndex].getCodecConfig();

        Log.d(TAG, "convergeCodecConfig: mActiveProfile: "
                   + mActiveProfile + ", mActiveProfileIndex: " +  mActiveProfileIndex);

        BluetoothCodecConfig[] mCodecsLocalCapabilities = new BluetoothCodecConfig[
                            A2dpCodecStatus.getCodecsLocalCapabilities().size() +
                            BapCodecStatus.getCodecsLocalCapabilities().size()];
        System.arraycopy(A2dpCodecStatus.getCodecsLocalCapabilities().toArray(new BluetoothCodecConfig[0]), 0, mCodecsLocalCapabilities, 0,
                         A2dpCodecStatus.getCodecsLocalCapabilities().size());
        System.arraycopy(BapCodecStatus.getCodecsLocalCapabilities().toArray(new BluetoothCodecConfig[0]), 0, mCodecsLocalCapabilities,
                         A2dpCodecStatus.getCodecsLocalCapabilities().size(),
                         BapCodecStatus.getCodecsLocalCapabilities().size());

        BluetoothCodecConfig[] mCodecsSelectableCapabilities = new BluetoothCodecConfig[
                            A2dpCodecStatus.getCodecsSelectableCapabilities().size() +
                            BapCodecStatus.getCodecsSelectableCapabilities().size()];
        System.arraycopy(A2dpCodecStatus.getCodecsSelectableCapabilities().toArray(new BluetoothCodecConfig[0]), 0, mCodecsSelectableCapabilities, 0,
                         A2dpCodecStatus.getCodecsSelectableCapabilities().size());
        System.arraycopy(BapCodecStatus.getCodecsSelectableCapabilities().toArray(new BluetoothCodecConfig[0]), 0, mCodecsSelectableCapabilities,
                         A2dpCodecStatus.getCodecsSelectableCapabilities().size(),
                         BapCodecStatus.getCodecsSelectableCapabilities().size());

        mCodecStatus = new BluetoothCodecStatus(mCodecConfig,
                Arrays.asList(mCodecsLocalCapabilities), Arrays.asList(mCodecsSelectableCapabilities));
        return mCodecStatus;
    }

    public void onCodecConfigChange(BluetoothDevice device, BluetoothCodecStatus mCodecStatus, Integer profile) {
        onCodecConfigChange(device, mCodecStatus, profile, true);
    }

    protected void refreshCurrentCodec(BluetoothDevice device, Integer profile) {
        MediaDevice mMediaDevice = mMediaDevices.get(device.getAddress());
        if(mMediaDevice == null) {
            return;
        }
        Log.d(TAG, "refreshCurrentCodec: for profile " + profile);
        mMediaDevice.mCodecStatus = convergeCodecConfig(mMediaDevice, profile);

        Log.d(TAG, "refreshCurrentCodec: " + device + ", " + mMediaDevice.mCodecStatus);

        broadcastCodecStatus(device, mMediaDevice.mCodecStatus);
    }

    public void onCodecConfigChange(BluetoothDevice device,
            BluetoothCodecStatus codecStatus, Integer profile, Boolean updateAudio) {
        Log.w(TAG, "onCodecConfigChange: for profile:" + profile + " for device "
                + device + " update audio: " + updateAudio + " with status " + codecStatus);
        if(device == null || codecStatus == null)
            return;

        if (profile != ApmConst.AudioProfiles.NONE && profile != ApmConst.AudioProfiles.A2DP) {
            StreamAudioService service = StreamAudioService.getStreamAudioService();
            if(service != null) {
                device = service.getDeviceGroup(device);
            }
        }

        if (device == null) {
            Log.d(TAG,"onCodecConfigChange: group device is null, return");
            return;
        }

        MediaDevice mMediaDevice = mMediaDevices.get(device.getAddress());
        BluetoothCodecStatus prevCodecStatus = null;

        if (mMediaDevice == null && profile == ApmConst.AudioProfiles.BROADCAST_LE) {
            Log.d(TAG,"LE Broadcast codec change");
        } else if(mMediaDevice == null) {
            Log.e(TAG, "No entry in Device Profile map for device: " + device);
            return;
        }

        if (mMediaDevice != null) {
            int profileIndex = mMediaDevice.getProfileIndex(profile);
            Log.d(TAG, "profileIndex: " + profileIndex);

            if(codecStatus.equals(mMediaDevice.mProfileCodecStatus[profileIndex])) {
                Log.w(TAG, "onCodecConfigChange: Codec already updated for the device and profile");
                return;
            }

            mMediaDevice.mProfileCodecStatus[profileIndex] = codecStatus;
            prevCodecStatus = mMediaDevice.mCodecStatus;

            /* Check the codec status for alternate Media profile for this device */
            if(mMediaDevice.mProfileCodecStatus[(profileIndex+1)%2] != null) {
                mMediaDevice.mCodecStatus = convergeCodecConfig(mMediaDevice, PROFILE_UNKNOWN);
            } else {
                mMediaDevice.mCodecStatus = codecStatus;
            }

            Log.w(TAG, "BroadCasting codecstatus " + mMediaDevice.mCodecStatus +
                                                              " for device: " + device);
            broadcastCodecStatus(device, mMediaDevice.mCodecStatus);
        }

        if (prevCodecStatus != null && mMediaDevice != null) {
            if (prevCodecStatus.getCodecConfig().equals(
                                    mMediaDevice.mCodecStatus.getCodecConfig())) {
                Log.d(TAG, "Previous and current codec config are same. Return");
                return;
            }
        }

        ActiveDeviceManagerService mActiveDeviceManager = ActiveDeviceManagerService.get();
        if (mActiveDeviceManager != null &&
            (!mActiveDeviceManager.isStableState(ApmConst.AudioFeatures.MEDIA_AUDIO) ||
              mActiveDeviceManager.isUpdatePending(ApmConst.AudioFeatures.MEDIA_AUDIO))) {
            Log.d(TAG, "SHO under progress. MM Audio will be updated after SHO completes");
            return;
        }

        if (updateAudio &&
            ((ApmConst.getQtiLeAudioEnabled() &&
              device.equals(mActiveDeviceManager.getActiveDevice(
                                         ApmConst.AudioFeatures.MEDIA_AUDIO))) ||
              ((!ApmConst.getQtiLeAudioEnabled() &&
                device.equals(mActiveDeviceManager.getActiveDevice(
                                         ApmConst.AudioFeatures.MEDIA_AUDIO)) &&
                device.equals(mActiveDeviceManager.getActiveDevice(
                                         ApmConst.AudioFeatures.CALL_AUDIO)))))) {
            VolumeManager mVolumeManager = VolumeManager.get();
            int currentVolume =
                   mVolumeManager.getActiveVolume(ApmConst.AudioFeatures.MEDIA_AUDIO);
            if (profile == ApmConst.AudioProfiles.BROADCAST_LE) {
                currentVolume = 15;
            }

            if (mAudioManager != null) {
                BluetoothDevice groupDevice = device;
                if(profile == ApmConst.AudioProfiles.BAP_MEDIA) {
                    StreamAudioService mStreamAudioService =
                                         StreamAudioService.getStreamAudioService();
                    groupDevice = mStreamAudioService.getDeviceGroup(device);
                }
                Log.d(TAG, "onCodecConfigChange: Calling handleBluetoothActiveDeviceChanged");

                /*if (!ApmConst.getQtiLeAudioEnabled() &&
                    (mActiveDeviceManager.getActiveProfile(ApmConst.AudioFeatures.MEDIA_AUDIO) ==
                                                      ApmConst.AudioProfiles.BAP_MEDIA) &&
                    (mActiveDeviceManager.getActiveProfile(ApmConst.AudioFeatures.CALL_AUDIO) ==
                                                      ApmConst.AudioProfiles.BAP_CALL)) {
                    Log.d(TAG, "onCodecConfigChange: Update MM with In and out BLE device");
                    mAudioManager.handleBluetoothActiveDeviceChanged(groupDevice,
                      groupDevice, BluetoothProfileConnectionInfo.createLeAudioInfo(true, true));

                    mAudioManager.handleBluetoothActiveDeviceChanged(groupDevice,
                      groupDevice, BluetoothProfileConnectionInfo.createLeAudioInfo(true, false));
                } else */
                if (ApmConst.getQtiLeAudioEnabled()) {
                    Log.d(TAG, "onCodecConfigChange: Update MM with A2dpd device");
                    mAudioManager.handleBluetoothActiveDeviceChanged(groupDevice,
                      groupDevice, BluetoothProfileConnectionInfo.createA2dpInfo(true, currentVolume));
                }
            }
        }
    }

    private void broadcastConnStateChange(BluetoothDevice device,
                                          int prevState, int newState) {
      A2dpService mA2dpService = A2dpService.getA2dpService();
      if (mA2dpService != null) {
        Log.d(TAG, "Broadcast Conn State Change: " + prevState +
                   "->" + newState + " for device " + device);
        Intent intent = new Intent(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        intent.putExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, prevState);
        intent.putExtra(BluetoothProfile.EXTRA_STATE, newState);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT
                        | Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND);
        mA2dpService.sendBroadcast(intent, BLUETOOTH_CONNECT,
             Utils.getTempAllowlistBroadcastOptions());
      }
    }

    private void broadcastStreamState(BluetoothDevice device, int prevStatus, int streamStatus) {
      A2dpService mA2dpService = A2dpService.getA2dpService();
      if (mA2dpService != null) {
        Intent intent = new Intent(BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
        intent.putExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, prevStatus);
        intent.putExtra(BluetoothProfile.EXTRA_STATE, streamStatus);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
        mA2dpService.sendBroadcast(intent, BLUETOOTH_CONNECT,
             Utils.getTempAllowlistBroadcastOptions());
      }
    }

    private void broadcastCodecStatus (BluetoothDevice device, BluetoothCodecStatus mCodecStatus) {
      A2dpService mA2dpService = A2dpService.getA2dpService();
      if (mA2dpService != null) {
        Intent intent = new Intent(BluetoothA2dp.ACTION_CODEC_CONFIG_CHANGED);
        intent.putExtra(BluetoothCodecStatus.EXTRA_CODEC_STATUS, mCodecStatus);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT
                        | Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND);
        mA2dpService.sendBroadcast(intent, BLUETOOTH_CONNECT,
             Utils.getTempAllowlistBroadcastOptions());
      }
    }

    public boolean isValidCodec (String mCodec) {
        return supported_codec.contains(mCodec);
    }

    public AudioManager getAudioManager() {
        return mAudioManager;
    }

    class MediaDevice {
        BluetoothDevice mDevice;
        int[] profileConnStatus = new int[2];
        int deviceConnStatus;
        int streamStatus;
        private BluetoothLeAudioCodecStatus mLeAudioCodecStatus;
        private BluetoothCodecStatus mCodecStatus;
        private BluetoothCodecStatus[] mProfileCodecStatus = new BluetoothCodecStatus[2];

        public static final int A2DP_STREAM = 0;
        public static final int LE_STREAM = 1;

        MediaDevice(BluetoothDevice device, int profile, int state) {
            profileConnStatus[A2DP_STREAM] = BluetoothProfile.STATE_DISCONNECTED;
            profileConnStatus[LE_STREAM] = BluetoothProfile.STATE_DISCONNECTED;
            mDevice = device;
            if((profile & ApmConst.AudioProfiles.A2DP) != ApmConst.AudioProfiles.NONE) {
                profileConnStatus[A2DP_STREAM] = state;
            }
            if((profile & (ApmConst.AudioProfiles.TMAP_MEDIA | ApmConst.AudioProfiles.BAP_MEDIA)) !=
                    ApmConst.AudioProfiles.NONE) {
                profileConnStatus[LE_STREAM] = state;
            }
            deviceConnStatus = state;
            streamStatus = BluetoothA2dp.STATE_NOT_PLAYING;
        }

        MediaDevice(BluetoothDevice device, int profile) {
            this(device, profile, BluetoothProfile.STATE_DISCONNECTED);
        }

        public int getProfileIndex(int profile) {
            if(profile == ApmConst.AudioProfiles.A2DP)
                return A2DP_STREAM;
            else
                return LE_STREAM;
        }
    }

    private class LeCodecConfig extends BroadcastReceiver {
        /*am broadcast -a qti.intent.bluetooth.action.UPDATE_CODEC_CONFIG --es
            qti.bluetooth.extra.CODEC_ID "LC3" --es qti.bluetooth.extra.CODEC_CONFIG "<ID>"*/

        ArrayList <String> supported_codec_config = new ArrayList<String>( List.of(
        /* config ID      Sampling Freq    Octets/Frame  */
            "8_1",  /*          8               26       */
            "8_2",  /*          8               30       */
            "16_1", /*          16              30       */
            "16_2", /*          16              40       */
            "24_1", /*          24              45       */
            "24_2", /*          24              60       */
            "32_1", /*          32              60       */
            "32_2", /*          32              80       */
            "441_1",/*          44.1            98       */
            "441_2",/*          44.1            130      */
            "48_1", /*          48              75       */
            "48_2", /*          48              100      */
            "48_3", /*          48              90       */
            "48_4", /*          48              120      */
            "48_5", /*          48              117      */
            "48_6", /*          48              155      */
            "GCP_TX",
            "GCP_TX_RX"
        ));

        Map <String, Integer> channel_mode = Map.of(
            "NONE", 0,
            "MONO", 1,
            "STEREO", 2
        );

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!ACTION_UPDATE_CODEC_CONFIG.equals(intent.getAction())) {
                return;
            }
            String mCodecId = intent.getStringExtra(CODEC_ID);
            if(mCodecId == null || !isValidCodec(mCodecId)) {
                Log.w(TAG, "Invalid Codec " + mCodecId);
                return;
            }
            String mCodecConfig = intent.getStringExtra(CODEC_CONFIG);
            if(mCodecConfig == null || !isValidCodecConfig(mCodecConfig)) {
                Log.w(TAG, "Invalid Codec Config " + mCodecConfig);
                return;
            }

            int mChannelMode = BluetoothCodecConfig.CHANNEL_MODE_NONE;
            String chMode = intent.getStringExtra(CHANNEL_MODE);
            if(chMode != null && channel_mode.containsKey(chMode)) {
                mChannelMode = channel_mode.get(chMode);
            }

            ActiveDeviceManagerService mActiveDeviceManager
                = ActiveDeviceManagerService.get();
            int profile = mActiveDeviceManager.getActiveProfile(ApmConst.AudioFeatures.MEDIA_AUDIO);
            if(profile == ApmConst.AudioProfiles.BROADCAST_LE) {
                /*Update Broadcast module here*/
                mBapBroadcastManager.setCodecPreference(mCodecConfig, mChannelMode);
            } else if (profile == ApmConst.AudioProfiles.BAP_MEDIA) {
                BluetoothDevice device = mActiveDeviceManager.getActiveDevice(ApmConst.AudioFeatures.MEDIA_AUDIO);
                StreamAudioService service = StreamAudioService.getStreamAudioService();
                service.setCodecConfig(device, mCodecConfig, mChannelMode);
            }
            Log.i(TAG, "Codec Config Request: Codec Name: " + mCodecId + " Config ID: "
                    + mCodecConfig + " mChannelMode: " + mChannelMode + " for profile: " + profile);
        }

        boolean isValidCodecConfig (String mCodecConfig) {
            return supported_codec_config.contains(mCodecConfig);
        }
    }

    private class QosConfigReceiver extends BroadcastReceiver {
        /*am broadcast -a qti.intent.bluetooth.action.UPDATE_QOS_CONFIG --es
            qti.bluetooth.extra.CODEC_ID "LC3" --es qti.bluetooth.extra.QOS_CONFIG "<ID>"*/
        boolean enable = false;

        ArrayList <String> supported_Qos_config = new ArrayList<String>( List.of(
            "8_1_1",
            "8_2_1",
            "16_1_1",
            "16_2_1",
            "24_1_1",
            "24_2_1",
            "32_1_1",
            "32_2_1",
            "441_1_1",
            "441_2_1",
            "48_1_1",
            "48_2_1",
            "48_3_1",
            "48_4_1",
            "48_5_1",
            "48_6_1",

            "8_1_2",
            "8_2_2",
            "16_1_2",
            "16_2_2",
            "24_1_2",
            "24_2_2",
            "32_1_2",
            "32_2_2",
            "441_1_2",
            "441_2_2",
            "48_1_2",
            "48_2_2",
            "48_3_2",
            "48_4_2",
            "48_5_2",
            "48_6_2"
        ));

        @Override
        public void onReceive(Context context, Intent intent) {
            if(!enable)
                return;
            if (!ACTION_UPDATE_QOS_CONFIG.equals(intent.getAction())) {
                return;
            }

            String mCodecId = intent.getStringExtra(CODEC_ID);
            if(mCodecId == null || !isValidCodec(mCodecId)) {
                Log.w(TAG, "Invalid Codec " + mCodecId);
                return;
            }
            String mQosConfig = intent.getStringExtra(QOS_CONFIG);
            if(mQosConfig == null || !isValidQosConfig(mQosConfig)) {
                Log.w(TAG, "Invalid QosConfig " + mQosConfig);
                return;
            }

            ActiveDeviceManagerService mActiveDeviceManager
                = ActiveDeviceManagerService.get();
            int profile = mActiveDeviceManager.getActiveProfile(ApmConst.AudioFeatures.MEDIA_AUDIO);
            if(profile == ApmConst.AudioProfiles.BROADCAST_LE) {
                /*Update Broadcast module here*/
            } else if (profile == ApmConst.AudioProfiles.BAP_MEDIA) {
                /*Update ACM here*/
            }
            Log.i(TAG, "New Qos Config ID: " + mQosConfig + " for profile: " + profile);
        }

        boolean isValidQosConfig(String mQosConfig) {
            return supported_Qos_config.contains(mQosConfig);
        }
    }

    class BapBroadcastManager {
        void setCodecPreference(String codecConfig, int channelMode) {
            BroadcastService mBroadcastService = BroadcastService.getBroadcastService();
            if(mBroadcastService != null) {
                mBroadcastService.setCodecPreference(codecConfig, channelMode);
            }
        }

        BluetoothCodecStatus getCodecStatus() {
            BroadcastService mBroadcastService = BroadcastService.getBroadcastService();
            if(mBroadcastService != null) {
                return mBroadcastService.getCodecStatus();
            }
            return null;
        }

        boolean isBapBroadcastActive() {
            BroadcastService mBroadcastService = BroadcastService.getBroadcastService();
            if(mBroadcastService != null) {
                return mBroadcastService.isBroadcastActive();
            }
            return false;
        }

        BluetoothDevice getBroadcastDevice() {
            BroadcastService mBroadcastService = BroadcastService.getBroadcastService();
            if(mBroadcastService != null) {
                return mBroadcastService.getBroadcastDevice();
            }
            return null;
        }
    }

    String SampleRateToString(int sample_rate) {
      switch (sample_rate) {
        case BluetoothLeAudioCodecConfig.SAMPLE_RATE_8000:
          return "8000";
        case BluetoothLeAudioCodecConfig.SAMPLE_RATE_16000:
          return "16000";
        case BluetoothLeAudioCodecConfig.SAMPLE_RATE_24000:
          return "24000";
        case BluetoothLeAudioCodecConfig.SAMPLE_RATE_32000:
          return "32000";
        case BluetoothLeAudioCodecConfig.SAMPLE_RATE_44100:
          return "44100";
        case BluetoothLeAudioCodecConfig.SAMPLE_RATE_48000:
          return "48000";
        case BluetoothLeAudioCodecConfig.SAMPLE_RATE_96000:
          return "96000";
        default:
          return "48000";
      }
    }
}

