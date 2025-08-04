/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
*/
/******************************************************************************
 *  Copyright (c) 2020, The Linux Foundation. All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are
 *  met:
 *      * Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *      * Redistributions in binary form must reproduce the above
 *        copyright notice, this list of conditions and the following
 *        disclaimer in the documentation and/or other materials provided
 *        with the distribution.
 *      * Neither the name of The Linux Foundation nor the names of its
 *        contributors may be used to endorse or promote products derived
 *        from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 *  ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 *  BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 *  BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 *  WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 *  OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 *  IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *****************************************************************************/

package com.android.bluetooth.acm;

import static android.Manifest.permission.BLUETOOTH_CONNECT;

import static com.android.bluetooth.Utils.enforceBluetoothPrivilegedPermission;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothA2dp.OptionalCodecsPreferenceStatus;
import android.bluetooth.BluetoothA2dp.OptionalCodecsSupportStatus;
import android.bluetooth.BluetoothCodecConfig;
import android.bluetooth.BluetoothCodecStatus;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothUuid;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.HandlerThread;
import android.os.Handler;
import android.os.Binder;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;
import android.os.Message;
import android.bluetooth.BluetoothGroupCallback;
import com.android.bluetooth.groupclient.GroupService;
import android.bluetooth.DeviceGroup;
import android.bluetooth.BluetoothDeviceGroup;
import com.android.bluetooth.apm.ActiveDeviceManagerService;
import com.android.bluetooth.apm.CallAudio;
import com.android.bluetooth.apm.VolumeManager;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import android.os.SystemProperties;
import com.android.bluetooth.BluetoothMetricsProto;
import com.android.bluetooth.BluetoothStatsLog;
import com.android.bluetooth.CsipWrapper;
import com.android.bluetooth.Utils;
import android.bluetooth.BluetoothAdapter;
import com.android.bluetooth.apm.ApmConst;
import com.android.bluetooth.btservice.AdapterService;
import com.android.bluetooth.btservice.MetricsLogger;
import com.android.bluetooth.btservice.ProfileService;
import com.android.bluetooth.btservice.ServiceFactory;
import com.android.bluetooth.csip.CsipSetCoordinatorService;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import com.android.bluetooth.vcp.VcpController;
import com.android.bluetooth.apm.StreamAudioService;
import com.android.bluetooth.apm.DeviceProfileMap;
import android.bluetooth.BluetoothLeAudioCodecStatus;
import android.bluetooth.BluetoothLeAudioCodecConfig;
import com.android.bluetooth.hfp.HeadsetService;

/**
 * Provides Bluetooth ACM profile, as a service in the Bluetooth application.
 * @hide
 */
public class AcmService extends ProfileService {
    private static final boolean DBG = true;
    private static final String TAG = "AcmService";
    private String mAcmName;
    public static final int ACM_AUDIO_UNICAST = 38;
    public static final int INVALID_SET_ID = 0x10;
    public static final ParcelUuid CAP_UUID =
                ParcelUuid.fromString("00001853-0000-1000-8000-00805F9B34FB");
    private static AcmService sAcmService;
    private BluetoothAdapter mAdapter;
    private AdapterService mAdapterService;
    private HandlerThread mStateMachinesThread;
    private static final int LOCK_RELEASED = 0;                    // (LOCK Released successfully)
    private static final int LOCK_RELEASED_TIMEOUT = 1;          // (LOCK Released by timeout)
    private static final int ALL_LOCKS_ACQUIRED = 2;              // (LOCK Acquired for all requested set members)
    private static final int SOME_LOCKS_ACQUIRED_REASON_TIMEOUT = 3; // (Request timeout for some set members)
    private static final int SOME_LOCKS_ACQUIRED_REASON_DISC = 4;   // (Some of the set members were disconnected)
    private static final int LOCK_DENIED = 5;                     // (Denied by one of the set members)
    private static final int INVALID_REQUEST_PARAMS = 6;         // (Upper layer provided invalid parameters)
    private static final int LOCK_RELEASE_NOT_ALLOWED = 7;           // (Response from remote (PTS))
    private static final int INVALID_VALUE = 8;
    @VisibleForTesting
    AcmNativeInterface mAcmNativeInterface;
    @VisibleForTesting
    ServiceFactory mFactory = new ServiceFactory();

    static final int CONTEXT_TYPE_UNKNOWN = 0;
    static final int CONTEXT_TYPE_MUSIC = 1;
    static final int CONTEXT_TYPE_VOICE = 2;
    static final int CONTEXT_TYPE_MUSIC_VOICE = 3;
    static final int CONTEXT_TYPE_BROADCAST_AUDIO = 6;
    private static final int VOICE_AUDIO_CONNECTION_DELAY_DEFAULT = 100;

    private AcmCodecConfig mAcmCodecConfig;
    private final Object mAudioManagerLock = new Object();
    private final Object mBtLeaLock = new Object();
    private final Object mBtAcmLock = new Object();
    private final Object mActiveDeviceLock = new Object();
    private String mLeaChannelMode = "stereo";
    private AudioManager mAudioManager;
    @GuardedBy("mStateMachines")
    private BluetoothDevice mGroupBdAddress = null;
    private BluetoothDevice mActiveDevice = null;
    private BluetoothDevice mActiveDeviceVoice = null;
    private int mActiveDeviceProfile = 0;
    private int mActiveDeviceVoiceProfile = 0;
    private final ConcurrentMap<BluetoothDevice, AcmStateMachine> mStateMachines =
            new ConcurrentHashMap<>();
    private ConcurrentHashMap<BluetoothDevice, BluetoothAcmDevice> mAcmDevices =
                     new ConcurrentHashMap<BluetoothDevice, BluetoothAcmDevice>();

    // Upper limit of all ACM devices: Bonded or Connected
    private static final int MAX_ACM_STATE_MACHINES = 50;
    // Upper limit of all ACM devices that are Connected or Connecting
    private int mMaxConnectedAudioDevices = 1;
    CsipManager mCsipManager = null;
    boolean mIsCsipRegistered = false;
    boolean mShoPend = false;
    boolean mVoiceShoPend = false;
    //volume
    private int mAudioStreamMax;
    private int mActiveDeviceLocalMediaVol;
    private int mActiveDeviceLocalVoiceVol;
    private boolean mActiveDeviceIsMuted;
    private static final int VCP_MAX_VOL = 255;
    private VcpController mVcpController;

    private BroadcastReceiver mBondStateChangedReceiver;
    private final ReentrantReadWriteLock mAcmNativeInterfaceLock = new ReentrantReadWriteLock();
    public int mCsipAppId = -1;

    private static final int SET_EBMONO_CFG = 1;
    private static final int SET_EBSTEREO_CFG = 2;
    private static final int MESSAGE_DELAYED_START_VOICE_STREAM = 3;

    private static final int MonoCfg_Timeout = 3000;
    private static final int StereoCfg_Timeout = 3000;

    private static final int DELAY_START_VOICE_STREAM_MS = 4000;
    private CsipWrapper mCsipWrapper = null;
    public boolean mIsPtsDualConnReq = false;

    private Handler mHandler = new Handler() {
       @Override
       public void handleMessage(Message msg)
       {
         synchronized(mBtLeaLock) {
           switch (msg.what) {
               case SET_EBMONO_CFG:
                   Log.d(TAG, "setparameters to Mono");
                   synchronized (mAudioManagerLock) {
                        if(mAudioManager != null)
                           mAudioManager.setParameters("LEAMono=true");
                   }
                   mLeaChannelMode = "mono";
                   break;
               case SET_EBSTEREO_CFG:
                   Log.d(TAG, "setparameters to stereo");
                   synchronized (mAudioManagerLock) {
                       if(mAudioManager != null)
                           mAudioManager.setParameters("LEAMono=false");
                   }
                   mLeaChannelMode = "stereo";
                   break;
               case MESSAGE_DELAYED_START_VOICE_STREAM:
                   BluetoothDevice grpDevice = (BluetoothDevice)msg.obj;
                   int contextType = msg.arg1;
                   Log.d(TAG, "MESSAGE_DELAYED_START_VOICE_STREAM, grpDevice=" + grpDevice +
                           ", contextType=" + contextType);
                   Log.d(TAG, "timeout to wait for suspending, continue to start voice stream");
                   startStreamWithoutSync(grpDevice, contextType);
                   break;
              default:
                   break;
           }
         }
       }
    };

    @Override
    protected void create() {
        Log.i(TAG, "create()");
    }

    @Override
    protected boolean start() {
        Log.i(TAG, "start()");
        String propValue;

        if (sAcmService != null) {
            Log.w(TAG, "AcmService is already running");
            return true;
        }

        // Step 1: Get AdapterService, AcmNativeInterface.
        // None of them can be null.
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mAdapterService = Objects.requireNonNull(AdapterService.getAdapterService(),
                "AdapterService cannot be null when AcmService starts");
        mAcmNativeInterface = Objects.requireNonNull(AcmNativeInterface.getInstance(),
                "AcmNativeInterface cannot be null when AcmService starts");

        mAdapterService = Objects.requireNonNull(AdapterService.getAdapterService(),
                "AdapterService cannot be null when StreamAudioService starts");

        Log.i(TAG, "mAdapterService.isHostAdvAudioUnicastFeatureSupported() returned "
                  + mAdapterService.isHostAdvAudioUnicastFeatureSupported());
        Log.i(TAG, "mAdapterService.isAdvUnicastAudioFeatEnabled() returned "
                  + mAdapterService.isAdvUnicastAudioFeatEnabled());

        // SOC supports unicast, host supports unicast
        if (mAdapterService.isHostAdvAudioUnicastFeatureSupported() &&
            mAdapterService.isAdvUnicastAudioFeatEnabled()) {

            Log.i(TAG, "SOC supports unicast, host supports unicast");
            // set properties only if they are not set to allow user enable/disable
            // the features explicitly
            propValue = SystemProperties.get("persist.vendor.service.bt.bap.enable_ucast");

            if (propValue == null || propValue.length() == 0 || !propValue.equals("false")) {
                SystemProperties.set("persist.vendor.service.bt.bap.enable_ucast", "true");
            } else {
                Log.i(TAG, "persist.vendor.service.bt.bap.enable_ucast is already set to "
                        + propValue);
            }

            Log.i(TAG, "mAdapterService.isHostAdvAudioStereoRecordingFeatureSupported() returned "
                  + mAdapterService.isHostAdvAudioStereoRecordingFeatureSupported());

            // SOC supports unicast, host supports unicast and stereo recording
            if (mAdapterService.isHostAdvAudioStereoRecordingFeatureSupported()) {
                Log.i(TAG, "SOC supports unicast, host supports unicast, stereo recording");

                propValue = SystemProperties.get("persist.vendor.service.bt.recording_supported");
                if (propValue == null || propValue.length() == 0 || !propValue.equals("false")) {
                    SystemProperties.set("persist.vendor.service.bt.recording_supported", "true");
                    Log.i(TAG, "persist.vendor.service.bt.recording_supported set to true");
                } else {
                    Log.i(TAG, "persist.vendor.service.bt.recording_supported is already set to "
                           + propValue);
                }
            }

            Log.i(TAG, "mAdapterService.isHostQHSFeatureSupported() returned "
                   + mAdapterService.isHostQHSFeatureSupported());

            // SOC supports unicast, host supports unicast and QHS
            if (mAdapterService.isHostQHSFeatureSupported()) {
                Log.i(TAG, "SOC supports unicast, host supports unicast, QHS");
                // set properties only if they are not set to allow user enable/disable
                // the features explicitly
                propValue = SystemProperties.get("persist.vendor.btstack.qhs_enable");

                if (propValue == null || propValue.length() == 0 || !propValue.equals("false")) {
                    SystemProperties.set("persist.vendor.btstack.qhs_enable", "true");
                } else {
                    Log.i(TAG, "persist.vendor.service.bt.bap.enable_ucast is already set to "
                          + propValue);
                }
            }

            Log.i(TAG, "isHostAdvAudioLC3QFeatureSupported(): "
                    + mAdapterService.isHostAdvAudioLC3QFeatureSupported());

            // SOC supports unicast, host supports unicast and LC3Q
            if (mAdapterService.isHostAdvAudioLC3QFeatureSupported()) {
                Log.i(TAG, "SOC supports unicast, host supports unicast, LC3Q");
                // set properties only if they are not set to allow user enable/disable
                // the features explicitly
                propValue = SystemProperties.get("persist.vendor.service.bt.is_lc3q_supported");

                if (propValue == null || propValue.length() == 0 || !propValue.equals("false")) {
                    SystemProperties.set("persist.vendor.service.bt.is_lc3q_supported", "true");
                } else {
                    Log.i(TAG, "persist.vendor.service.bt.is_lc3q_supported is already set to "
                           + propValue);
                }
            }
        }

        // Step 2: Get maximum number of connected audio devices
        mMaxConnectedAudioDevices = mAdapterService.getMaxConnectedAudioDevices();
        Log.i(TAG, "Max connected audio devices set to " + mMaxConnectedAudioDevices);


        String LeaChannelMode = SystemProperties.get("persist.vendor.btstack.Lea.defaultchannelmode");
        if (!LeaChannelMode.isEmpty() && "mono".equals(LeaChannelMode)) {
            mLeaChannelMode = "mono";
        }
        Log.d(TAG, "Default LEA ChannelMode: " + LeaChannelMode);
        // Step 3: Start handler thread for state machines
        mStateMachines.clear();
        mStateMachinesThread = new HandlerThread("AcmService.StateMachines");
        mStateMachinesThread.start();

        // Step 4: Setup codec config
        mAcmCodecConfig = new AcmCodecConfig(this, mAcmNativeInterface);

        if (mAdapterService.isAdvUnicastAudioFeatEnabled()) {
          Log.d(TAG, "Initialize AcmNativeInterface");
          // Step 5: Initialize native interface
          mAcmNativeInterface.init(mMaxConnectedAudioDevices,
                                   mAcmCodecConfig.codecConfigPriorities());
        }

        // Step 6: Setup broadcast receivers
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        mBondStateChangedReceiver = new BondStateChangedReceiver();
        registerReceiver(mBondStateChangedReceiver, filter, Context.RECEIVER_EXPORTED);
        synchronized (mAudioManagerLock) {
             mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
             Objects.requireNonNull(mAudioManager,
                                "AudioManager cannot be null when AcmService starts");
             mAudioStreamMax = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        }
        // Step 7: Mark service as started
        setAcmService(this);
        mCsipWrapper = CsipWrapper.getInstance();

        AcmServIntf.init(sAcmService);

        //step 8: Register CSIP module
        mCsipManager = new CsipManager();

        //step 9: Get Vcp Controller
        mVcpController = VcpController.make(this);
        Objects.requireNonNull(mVcpController, "mVcpController cannot be null when AcmService starts");
        mIsPtsDualConnReq =
            SystemProperties.getBoolean("persist.vendor.service.bt.acm.dualconnect", false);
        return true;
    }

    @Override
    protected boolean stop() {
        Log.i(TAG, "stop()");
        if (sAcmService == null) {
            Log.w(TAG, "stop() called before start()");
            return true;
        }
       // step 8: suspend music or voice streaming
        setActiveDeviceAcm(null, CONTEXT_TYPE_MUSIC, mActiveDeviceProfile);
        setActiveDeviceAcm(null, CONTEXT_TYPE_VOICE, mActiveDeviceVoiceProfile);
        // Step 7: do quit Vcp Controller
        if (mVcpController != null) {
            mVcpController.doQuit();
        }

        // Step 6: Mark service as stopped
        setAcmService(null);
        try {
            unregisterReceiver(mBondStateChangedReceiver);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, e.getMessage());
        }

        mBondStateChangedReceiver = null;

        // Step 5: Clear codec config
        mAcmCodecConfig = null;

        // Step 4: Destroy state machines and stop handler thread
        synchronized (mStateMachines) {
            for (AcmStateMachine sm : mStateMachines.values()) {
                sm.doQuit();
                sm.cleanup();
            }
            mStateMachines.clear();
        }
        if (mStateMachinesThread != null) {
            mStateMachinesThread.quitSafely();
        }
        mStateMachinesThread = null;

        // Step 3: Reset maximum number of connected audio devices
        mMaxConnectedAudioDevices = 1;

        // Step 2: Cleanup native interface
        if(mAcmNativeInterface != null) {
            mAcmNativeInterface.cleanup();
        }

        // Step 1: Clear AdapterService, AcmNativeInterface, AudioManager
        mAcmNativeInterface = null;
        mAdapterService = null;
        if (mAcmDevices != null)
            mAcmDevices.clear();

        if (mCsipManager != null) {
            mCsipManager.unregisterCsip();
        }
        mCsipManager = null;

        return true;
    }

    @Override
    protected void cleanup() {
        Log.i(TAG, "cleanup()");
    }

    @Override
    protected IProfileServiceBinder initBinder() {
        return new AcmBinder(this);
    }

    private class BluetoothAcmDevice {
        private BluetoothDevice mGrpDevice; // group bd address
        private int mState;
        private int msetID;

        BluetoothAcmDevice(BluetoothDevice device, int state, int setID) {
            mGrpDevice = device;
            mState = state;
            msetID = setID;
        }
    }

    private BluetoothDevice getAddressFromString(String address) {
        return mAdapter.getRemoteDevice(address);
    }

    public BluetoothDevice makeGroupBdAddress(BluetoothDevice device, int state, int setid) {
        Log.i(TAG, " Set id : " + setid + " Num of connected acm devices: " + mAcmDevices.size());
        boolean setIdMatched = false;
        if (setid == INVALID_SET_ID) {
            Log.d(TAG, "Device is not part of any group");
            BluetoothAcmDevice acmDevice = new BluetoothAcmDevice(device, state, setid);
            mAcmDevices.put(device, acmDevice);
            mGroupBdAddress = acmDevice.mGrpDevice;
            return mGroupBdAddress;
        }
       // BluetoothDevice bdaddr = null;
        if (mAcmDevices == null) {
            Log.d(TAG, "Hash Map is NULL");
            return mGroupBdAddress;
        }
        if (mAcmDevices.containsKey(device)) {
            Log.d(TAG, "Device is available in Hash Map");
            BluetoothAcmDevice acmDevice = mAcmDevices.get(device);
            if (acmDevice != null) {
                mGroupBdAddress = acmDevice.mGrpDevice;
            }
            return mGroupBdAddress;
        }
        if (mAcmDevices.size() != 0) {
            for (BluetoothDevice dm : mAcmDevices.keySet()) {
                BluetoothAcmDevice d = mAcmDevices.get(dm);
                if ((d != null) && (d.msetID == setid)) {
                    setIdMatched = true;
                    Log.d(TAG, "Device is part of same set ID");
                    BluetoothAcmDevice acmDevice = new BluetoothAcmDevice(d.mGrpDevice, state, setid);
                    mAcmDevices.put(device, acmDevice);
                    mGroupBdAddress = acmDevice.mGrpDevice;
                    break;
                }
            }
        }
        if (!setIdMatched) {
            Log.d(TAG, "create new group or device is not part of existing set ID");
            String address = "9E:8B:00:00:00:0";
            BluetoothDevice bdaddr = getAddressFromString(address + setid);
            BluetoothAcmDevice acmDevice = new BluetoothAcmDevice(bdaddr, state, setid);
            mAcmDevices.put(device, acmDevice);
            mGroupBdAddress = bdaddr;
        }
        return mGroupBdAddress;
    }

    public void handleAcmDeviceStateChange(BluetoothDevice device, int state, int setid) {
        Log.d(TAG, "handleAcmDeviceStateChange: device: " + device + ", state: " + state
                + " Set id : " + setid);
        Log.i(TAG, " Num of connected ACM devices: " + mAcmDevices.size());
        boolean update = false;
        if (device == null || mAcmDevices.size() == 0)
            return;
        BluetoothAcmDevice acmDevice = mAcmDevices.get(device);
        //check if current active group address is same as this device group address
        if (acmDevice != null && mGroupBdAddress != acmDevice.mGrpDevice) {
            Log.d(TAG, "Inactive device is disconnected");
            update = true;
        }
        int bondState = mAdapterService.getBondState(device);
        if (state == BluetoothProfile.STATE_DISCONNECTED &&
                bondState == BluetoothDevice.BOND_NONE ) {
            Log.d(TAG, "Remove Device from hash map");
            mAcmDevices.remove(device);
        } else if (acmDevice != null) {
            acmDevice.mState = state;
            Log.d(TAG, "Update state");
        }
        for (BluetoothDevice dm : mAcmDevices.keySet()) {
            BluetoothAcmDevice d = mAcmDevices.get(dm);
            if ((d !=null) && (d.msetID == setid)) {
                if (d.mState == BluetoothProfile.STATE_CONNECTED) {
                    update = true;
                    Log.d(TAG, "Atleast one member is connected");
                    break;
                }
            }
        }
        if (!update) {
            /*if (!mAcmNativeInterface.setActiveDevice(null, 0)) {//send unknown context type
                Log.w(TAG, "setActiveDevice(null): Cannot remove active device in native layer");
            }*/
        }
    }

    public static synchronized AcmService getAcmService() {
        if (sAcmService == null) {
            Log.w(TAG, "getAcmService(): service is null");
            return null;
        }
        if (!sAcmService.isAvailable()) {
            Log.w(TAG, "getAcmService(): service is not available");
            return null;
        }
        return sAcmService;
    }

    private static synchronized void setAcmService(AcmService instance) {
        if (DBG) {
            Log.d(TAG, "setAcmService(): set to: " + instance);
        }
        sAcmService = instance;
    }

    //From LeAudioService this connect would come.
    public boolean connect(BluetoothDevice device) {
        Log.d(TAG, "connect(): " + device);
        DeviceProfileMap dMap = DeviceProfileMap.getDeviceProfileMapInstance();
        if (dMap == null)
            return false;

        int peer_supported_profiles = dMap.getAllSupportedProfile(device);
        boolean is_peer_support_recording =
                ((peer_supported_profiles & ApmConst.AudioProfiles.BAP_RECORDING) != 0);

        int profileID = dMap.getSupportedProfile(device,
                                                 ApmConst.AudioFeatures.MEDIA_AUDIO);
        if (profileID == ApmConst.AudioProfiles.NONE) {
            Log.e(TAG, "Can Not connect to " + device +
                       ". Device does not support media service.");
            return false;
        }

        BluetoothDevice groupDevice = device;
        StreamAudioService mStreamService = StreamAudioService.getStreamAudioService();

        if (mStreamService != null &&
            (ApmConst.AudioProfiles.BAP_MEDIA & profileID) ==
                                    ApmConst.AudioProfiles.BAP_MEDIA) {
            int defaultMediaProfile = ApmConst.AudioProfiles.BAP_MEDIA;

            /* handle common conect of call and media audio */
            if(/*autoConnect*/ false) {
                groupDevice = mStreamService.getDeviceGroup(device);
                Log.i(TAG, "Auto Connect Request. Connecting group: " + groupDevice);
            }

            boolean mIsRecordingEnabled = SystemProperties.getBoolean(
                              "persist.vendor.service.bt.recording_supported", false);

            if(mIsRecordingEnabled) {
                int recordingProfile =
                       dMap.getProfile(device, ApmConst.AudioFeatures.STEREO_RECORDING);
                Log.i(TAG, "Add Recording profile to LE connect request: " + recordingProfile);
                defaultMediaProfile = defaultMediaProfile | recordingProfile;
            }

            if(/*allProfile*/ true) {
                int callProfileID =
                     dMap.getSupportedProfile(device, ApmConst.AudioFeatures.CALL_AUDIO);
                if((callProfileID & ApmConst.AudioProfiles.BAP_CALL) ==
                                                    ApmConst.AudioProfiles.BAP_CALL) {
                    Log.i(TAG, "Add BAP_CALL to LE connect request");
                    mStreamService.connectLeStream(groupDevice,
                        defaultMediaProfile | ApmConst.AudioProfiles.BAP_CALL);
                } else {
                    mStreamService.connectLeStream(groupDevice, defaultMediaProfile);
                }
            } else {
                mStreamService.connectLeStream(groupDevice, defaultMediaProfile);
            }
        }
        return true;
    }

    public boolean connect(BluetoothDevice device, int contextType,
                           int profileType, int preferredContext) {

        if (DBG) {
            Log.d(TAG, "connect(): " + device + " contextType: " + contextType
                    + " profileType: " + profileType + " preferredContext: " + preferredContext);
        }
        if ((device != null) && device.getAddress().contains("9E:8B:00:00:00")) {
            Log.d(TAG, "Connect request for group");
            byte[] addrByte = Utils.getByteAddress(device);
            int set_id = addrByte[5];
            List<BluetoothDevice> d = mCsipManager.getSetMembers(set_id);
            if (d == null) {
                Log.d(TAG, "No set member found");
                return false;
            }
            Iterator<BluetoothDevice> members = d.iterator();
            if (members != null) {
                while (members.hasNext()) {
                    BluetoothDevice addr = members.next();
                    Log.d(TAG, "connect member: " + addr);
                    synchronized (mStateMachines) {
                        if (!connectionAllowedCheckMaxDevices(addr)) {
                            // when mMaxConnectedAudioDevices is one, disconnect current device first.
                            if (mMaxConnectedAudioDevices == 1) {
                                List<BluetoothDevice> sinks = getDevicesMatchingConnectionStates(
                                        new int[] {BluetoothProfile.STATE_CONNECTED,
                                                BluetoothProfile.STATE_CONNECTING,
                                                BluetoothProfile.STATE_DISCONNECTING});
                                for (BluetoothDevice sink : sinks) {
                                    if (sink.equals(addr)) {
                                        Log.w(TAG, "Connecting to device " + addr + " : disconnect skipped");
                                        continue;
                                    }
                                    disconnect(sink, contextType);
                                }
                            } else {
                                Log.e(TAG, "Cannot connect to " + addr + " : too many connected devices");
                                return false;
                            }
                        }
                        AcmStateMachine smConnect = getOrCreateStateMachine(addr);
                        if (smConnect == null) {
                            Log.e(TAG, "Cannot connect to " + addr + " : no state machine");
                            return false;
                        }
                        Message msg = smConnect.obtainMessage(AcmStateMachine.CONNECT);
                        msg.obj = preferredContext;
                        msg.arg1 = contextType;
                        msg.arg2 = profileType;
                        smConnect.sendMessage(msg);
                    }
                }
            }
            return true;
        }
        synchronized (mStateMachines) {
            if (!connectionAllowedCheckMaxDevices(device)) {
                // when mMaxConnectedAudioDevices is one, disconnect current device first.
                if (mMaxConnectedAudioDevices == 1) {
                    List<BluetoothDevice> sinks = getDevicesMatchingConnectionStates(
                            new int[] {BluetoothProfile.STATE_CONNECTED,
                                    BluetoothProfile.STATE_CONNECTING,
                                    BluetoothProfile.STATE_DISCONNECTING});
                    for (BluetoothDevice sink : sinks) {
                        if (sink.equals(device)) {
                            Log.w(TAG, "Connecting to device " + device + " : disconnect skipped");
                            continue;
                        }
                        disconnect(sink, contextType);
                    }
                } else {
                    Log.e(TAG, "Cannot connect to " + device + " : too many connected devices");
                    return false;
                }
            }
            AcmStateMachine smConnect = getOrCreateStateMachine(device);
            if (smConnect == null) {
                Log.e(TAG, "Cannot connect to " + device + " : no state machine");
                return false;
            }
            Message msg = smConnect.obtainMessage(AcmStateMachine.CONNECT);
            msg.obj = preferredContext;
            msg.arg1 = contextType;
            msg.arg2 = profileType;
            smConnect.sendMessage(msg);
            return true;
        }
    }

    public boolean disconnect(BluetoothDevice device) {
        Log.i(TAG, "Disconnect: " + device);
        //MediaDevice mMediaDevice = null;

        if (device == null) {
            Log.i(TAG, "Disconnect: device is null, return");
            return false;
        }

         StreamAudioService service = StreamAudioService.getStreamAudioService();
         if(service != null) {
             //DeviceProfileMap dMap = DeviceProfileMap.getDeviceProfileMapInstance();
             //Below code not required, but keeping
             //To-do
             /*if (!dMap.isProfileConnected(device, ApmConst.AudioProfiles.BAP_CALL)) {
                 Log.d(TAG,"BAP_CALL not connected");
                 allProfile = false;
             }*/
             service.disconnectLeStream(device, true, true);
         }
         return true;
    }

    /**
     * Disconnects Acm for the remote bluetooth device
     *
     * @param device is the device with which we would like to disconnect acm
     * @return true if profile disconnected, false if device not connected over acm
     */
    public boolean disconnect(BluetoothDevice device, int contextType) {

        if (DBG) {
            Log.d(TAG, "disconnect(): " + device);
        }

        if ((device != null) && device.getAddress().contains("9E:8B:00:00:00")) {
            Log.d(TAG, "Disonnect request for group");
            byte[] addrByte = Utils.getByteAddress(device);
            int set_id = addrByte[5];
            List<BluetoothDevice> d = mCsipManager.getSetMembers(set_id);
            if (d == null) {
                Log.d(TAG, "No set member found");
                return false;
            }
            Iterator<BluetoothDevice> members = d.iterator();
            if (members != null) {
                while (members.hasNext()) {
                    BluetoothDevice addr = members.next();
                    Log.d(TAG, "disconnect member: " + device);
                    synchronized (mStateMachines) {
                        AcmStateMachine sm = mStateMachines.get(addr);
                        if (sm == null) {
                            Log.e(TAG, "Ignored disconnect request for " + addr + " : no state machine");
                            return false;
                        }
                        Message msg = sm.obtainMessage(AcmStateMachine.DISCONNECT);
                        msg.obj = contextType;
                        sm.sendMessage(msg);
                    }
                }
                return true;
            }
        }
        synchronized (mStateMachines) {
            AcmStateMachine sm = mStateMachines.get(device);
            if (sm == null) {
                Log.e(TAG, "Ignored disconnect request for " + device + " : no state machine");
                return false;
            }
            Message msg = sm.obtainMessage(AcmStateMachine.DISCONNECT);
            msg.obj = contextType;
            sm.sendMessage(msg);
            return true;
        }
    }

    public List<BluetoothDevice> getGroupMembers (BluetoothDevice groupDevice) {
        if (groupDevice == null) {
            Log.e(TAG, "getGroupMembers: groupDevice is null. Return.");
            return null;
        }
        if (!groupDevice.getAddress().contains("9E:8B:00:00:00")) {
            Log.e(TAG, "getGroupMembers: Not a group address. Return.");
            List<BluetoothDevice> devices = new ArrayList<>();
            devices.add(groupDevice);
            return devices;
        }

        byte[] addrByte = Utils.getByteAddress(groupDevice);
        int set_id = addrByte[5];
        List<BluetoothDevice> devices = mCsipManager.getSetMembers(set_id);
        return devices;
    }

    public List<BluetoothDevice> getConnectedDevices() {

        synchronized (mStateMachines) {
            List<BluetoothDevice> devices = new ArrayList<>();
            for (AcmStateMachine sm : mStateMachines.values()) {
                if (sm.isConnected()) {
                    devices.add(sm.getDevice());
                }
            }
            return devices;
        }
    }

    //check if it can be a list ?
    public BluetoothDevice getCsipLockRequestedDevice() {

        synchronized (mStateMachines) {
            BluetoothDevice device = null;
            for (AcmStateMachine sm : mStateMachines.values()) {
                if (sm.isCsipLockRequested()) {
                    device = sm.getDevice();
                }
            }
            return device;
        }
    }

    public boolean IsLockSupportAvailable(BluetoothDevice device) {
        boolean isLockSupported = false;
        /*int setId = mSetCoordinator.getRemoteSetId(device, ACM_UUID);
        DeviceGroup set = mSetCoordinator.getDeviceGroup(setId);
        isLockSupported = set.mLockSupport;*/
        //isLockSupported = mAdapterService.isCsipLockSupport(device);
        Log.d(TAG, "Exclusive Access SupportAvaible for:" + device + "returns " + isLockSupported);
        return isLockSupported;
    }

    /**
     * Check whether can connect to a peer device.
     * The check considers the maximum number of connected peers.
     *
     * @param device the peer device to connect to
     * @return true if connection is allowed, otherwise false
     */
    private boolean connectionAllowedCheckMaxDevices(BluetoothDevice device) {
        int connected = 0;
        // Count devices that are in the process of connecting or already connected
        synchronized (mStateMachines) {
            for (AcmStateMachine sm : mStateMachines.values()) {
                switch (sm.getConnectionState()) {
                    case BluetoothProfile.STATE_CONNECTING:
                    case BluetoothProfile.STATE_CONNECTED:
                        if (Objects.equals(device, sm.getDevice())) {
                            return true;    // Already connected or accounted for
                        }
                        connected++;
                        break;
                    default:
                        break;
                }
            }
        }
        return (connected < mMaxConnectedAudioDevices);
    }

    List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {

        List<BluetoothDevice> devices = new ArrayList<>();
        if (states == null) {
            return devices;
        }
        final BluetoothDevice[] bondedDevices = mAdapterService.getBondedDevices();
        if (bondedDevices == null) {
            return devices;
        }
        synchronized (mStateMachines) {
            for (BluetoothDevice device : bondedDevices) {
                /*if (!ArrayUtils.contains(mAdapterService.getRemoteUuids(device),
                                                 BluetoothUuid.ACM_SINK)) {
                    continue;
                }*/
                int connectionState = BluetoothProfile.STATE_DISCONNECTED;
                AcmStateMachine sm = mStateMachines.get(device);
                if (sm != null) {
                    connectionState = sm.getConnectionState();
                }
                for (int state : states) {
                    if (connectionState == state) {
                        devices.add(device);
                        break;
                    }
                }
            }
            return devices;
        }
    }

    /**
     * Get the list of devices that have state machines.
     *
     * @return the list of devices that have state machines
     */
    @VisibleForTesting
    List<BluetoothDevice> getDevices() {
        List<BluetoothDevice> devices = new ArrayList<>();
        synchronized (mStateMachines) {
            for (AcmStateMachine sm : mStateMachines.values()) {
                devices.add(sm.getDevice());
            }
            return devices;
        }
    }

    public int getConnectionState(BluetoothDevice device) {
        synchronized (mStateMachines) {
            AcmStateMachine sm = mStateMachines.get(device);
            if (sm == null) {
                return BluetoothProfile.STATE_DISCONNECTED;
            }
            return sm.getConnectionState();
        }
    }

    public int getCsipConnectionState(BluetoothDevice device) {
        synchronized (mStateMachines) {
            AcmStateMachine sm = mStateMachines.get(device);
            if (sm == null) {
                return BluetoothProfile.STATE_DISCONNECTED;
            }
            return sm.getCsipConnectionState();
        }
    }

    public int getMusicConnectionState(BluetoothDevice device) {
        synchronized (mStateMachines) {
            AcmStateMachine sm = mStateMachines.get(device);
            if (sm == null) {
                return BluetoothProfile.STATE_DISCONNECTED;
            }
            return sm.getMusicConnectionState();
        }
    }

    // Handle messages from native (JNI) to Java
    void messageFromNative(AcmStackEvent stackEvent) {
        Objects.requireNonNull(stackEvent.device,
                               "Device should never be null, event: " + stackEvent);
        synchronized (mStateMachines) {
            BluetoothDevice device = stackEvent.device;
            AcmStateMachine sm = mStateMachines.get(device);
            if (sm == null) {
                if (stackEvent.type == AcmStackEvent.EVENT_TYPE_CONNECTION_STATE_CHANGED) {
                    switch (stackEvent.valueInt1) {
                        case AcmStackEvent.CONNECTION_STATE_CONNECTED:
                        case AcmStackEvent.CONNECTION_STATE_CONNECTING:
                            // Create a new state machine only when connecting to a device
                            if (!connectionAllowedCheckMaxDevices(device)) {
                                Log.e(TAG, "Cannot connect to " + device
                                        + " : too many connected devices");
                                return;
                            }
                            sm = getOrCreateStateMachine(device);
                            break;
                        default:
                            break;
                    }
                }
            }
            if (sm == null) {
                Log.e(TAG, "Cannot process stack event: no state machine: " + stackEvent);
                return;
            }
            sm.sendMessage(AcmStateMachine.STACK_EVENT, stackEvent);
        }
    }

    private AcmStateMachine getOrCreateStateMachine(BluetoothDevice device) {
        if (device == null) {
            Log.e(TAG, "getOrCreateStateMachine failed: device cannot be null");
            return null;
        }
        synchronized (mStateMachines) {
            AcmStateMachine sm = mStateMachines.get(device);
            if (sm != null) {
                return sm;
            }
            // Limit the maximum number of state machines to avoid DoS attack
            if (mStateMachines.size() >= MAX_ACM_STATE_MACHINES) {
                Log.e(TAG, "Maximum number of ACM state machines reached: "
                        + MAX_ACM_STATE_MACHINES);
                return null;
            }
            if (DBG) {
                Log.d(TAG, "Creating a new state machine for " + device);
            }
            sm = AcmStateMachine.make(device, this, mAcmNativeInterface,
                                      mStateMachinesThread.getLooper());
            mStateMachines.put(device, sm);
            return sm;
        }
    }

    private class BondStateChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction())) {
                return;
            }
            int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
                                           BluetoothDevice.ERROR);
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            Objects.requireNonNull(device, "ACTION_BOND_STATE_CHANGED with no EXTRA_DEVICE");
            bondStateChanged(device, state);
        }
    }

    /**
     * Process a change in the bonding state for a device.
     *
     * @param device the device whose bonding state has changed
     * @param bondState the new bond state for the device. Possible values are:
     * {@link BluetoothDevice#BOND_NONE},
     * {@link BluetoothDevice#BOND_BONDING},
     * {@link BluetoothDevice#BOND_BONDED}.
     */
    @VisibleForTesting
    public void bondStateChanged(BluetoothDevice device, Integer bondState) {
        if (DBG) {
            Log.d(TAG, "Bond state changed for device: " + device +
                       " bondState: " + bondState);
        }
        // Remove state machine if the bonding for a device is removed
        if (bondState != BluetoothDevice.BOND_NONE) {
            return;
        }

        BluetoothDevice grpDevice = onlyGetGroupDevice(device);
        if(!device.equals(grpDevice)) {
            byte[] addrByte = Utils.getByteAddress(grpDevice);
            int set_id = addrByte[5];
            if(isLastMemberUnbonded(device, set_id)) {
                VolumeManager volumeManager = VolumeManager.get();
                volumeManager.handleDeviceUnbond(grpDevice);
                DeviceProfileMap dMap = DeviceProfileMap.getDeviceProfileMapInstance();
                if (dMap != null) {
                  dMap.handleDeviceUnbond(grpDevice);
                }
            }
        }

        synchronized (mStateMachines) {
            AcmStateMachine sm = mStateMachines.get(device);
            if (sm == null) {
                Log.e(TAG, "sm is null, for device: " + device);
                if (mAcmDevices.containsKey(device)) {
                    Log.d(TAG, "Remove Device from hash map");
                    mAcmDevices.remove(device);
                }
                return;
            }
            Log.d(TAG, " Connection state: " + sm.getConnectionState() +
                       " , for device: " + device);
            if (sm.getConnectionState() != BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, " Connection state is not disconnected, return");
                return;
            }
        }
        removeStateMachine(device);
    }

    private void removeStateMachine(BluetoothDevice device) {
        synchronized (mStateMachines) {
            AcmStateMachine sm = mStateMachines.get(device);
            if (sm == null) {
                Log.w(TAG, "removeStateMachine: device " + device
                        + " does not have a state machine");
                return;
            }
            Log.i(TAG, "removeStateMachine: removing state machine for device: " + device);
            sm.doQuit();
            sm.cleanup();
            mStateMachines.remove(device);
            Log.d(TAG, "Remove Device from hash map");
            mAcmDevices.remove(device);
        }
    }

    void updateLeaChannelMode(int state, BluetoothDevice device) {
        BluetoothDevice peerLeaDevice = null;
        peerLeaDevice = getLeaPeerDevice(device);
        if (peerLeaDevice == null) {
            Log.d(TAG, "updateLeaChannelMode: peer device is NULL");
            return;
        }
        Log.d(TAG, "LeaChannelMode: " + mLeaChannelMode + "state: " + state);
        synchronized(mBtLeaLock) {
            if ("mono".equals(mLeaChannelMode)) {
                if ((state == BluetoothA2dp.STATE_PLAYING) && (peerLeaDevice!= null)
                     && peerLeaDevice.isConnected() && isAcmPlayingMusic(peerLeaDevice)) {
                    Log.d(TAG, "updateLeaChannelMode: send delay message to set stereo ");
                    Message msg = mHandler.obtainMessage(SET_EBSTEREO_CFG);
                    mHandler.sendMessageDelayed(msg, StereoCfg_Timeout);
                } else if (state == BluetoothA2dp.STATE_PLAYING) {
                    Log.d(TAG, "updateLeaChannelMode: setparameters to Mono");
                    synchronized (mAudioManagerLock) {
                        if (mAudioManager != null) {
                            Log.d(TAG, "updateLeaChannelMode: Acquired mVariableLock");
                            mAudioManager.setParameters("LEAMono=true");
                        }
                    }
                    Log.d(TAG, "updateLeaChannelMode: Released mVariableLock");
                }
                if ((state == BluetoothA2dp.STATE_NOT_PLAYING) &&
                       isAcmPlayingMusic(peerLeaDevice)) {
                    if (mHandler.hasMessages(StereoCfg_Timeout)) {
                        Log.d(TAG, "updateLeaChannelMode:remove delay message for stereo");
                        mHandler.removeMessages(StereoCfg_Timeout);
                    }
                }
            } else if ("stereo".equals(mLeaChannelMode)) {
                if ((state == BluetoothA2dp.STATE_PLAYING) &&
                     (getConnectionState(peerLeaDevice) != BluetoothProfile.STATE_CONNECTED
                     || !isAcmPlayingMusic(peerLeaDevice))) {
                    Log.d(TAG, "updateLeaChannelMode: send delay message to set mono");
                    Message msg = mHandler.obtainMessage(SET_EBMONO_CFG);
                    mHandler.sendMessageDelayed(msg, MonoCfg_Timeout);
                }
                if ((state == BluetoothA2dp.STATE_PLAYING) && isAcmPlayingMusic(peerLeaDevice)) {
                    if (mHandler.hasMessages(SET_EBMONO_CFG)) {
                        Log.d(TAG, "updateLeaChannelMode: remove delay message to set mono");
                        mHandler.removeMessages(SET_EBMONO_CFG);
                    }
                }
                if ((state == BluetoothA2dp.STATE_NOT_PLAYING) && isAcmPlayingMusic(peerLeaDevice)) {
                    Log.d(TAG, "setparameters to Mono");
                    synchronized (mAudioManagerLock) {
                        if (mAudioManager != null)
                            mAudioManager.setParameters("LEAMono=true");
                    }
                    mLeaChannelMode = "mono";
                }
            }
        }
    }

    private BluetoothDevice getLeaPeerDevice(BluetoothDevice device) {
        synchronized (mStateMachines) {
            AcmStateMachine sm = mStateMachines.get(device);
            if (sm == null) {
                return null;
            }
            return sm.getPeerDevice();
        }
    }

    public boolean isPeerDeviceConnectedForContext(BluetoothDevice device, int setid, int contextType) {
        boolean isConnected = false;
        Log.d(TAG, "isPeerDeviceConnectedForContext: device:" + device + " setid:" + setid + " context:" + contextType);
        if (mAcmDevices.size() != 0) {
            for (BluetoothDevice dm : mAcmDevices.keySet()) {
                BluetoothAcmDevice d = mAcmDevices.get(dm);
                if (d == null)
                   continue;
                Log.d(TAG, "isPeerDeviceConnectedForContext(): d.msetID: " + d.msetID +
                           " d.mState: " + d.mState);
                if ((d.msetID == setid) && !Objects.equals(dm, device)) {
                    if (d.mState == BluetoothProfile.STATE_CONNECTED) {
                        AcmStateMachine sm = getOrCreateStateMachine(dm);
                        if (sm == null) {
                            Log.e(TAG, "AcmStateMachine for peer is null");
                            continue;
                        }
                        if ((contextType == CONTEXT_TYPE_MUSIC &&
                              sm.getMusicConnectionState() == BluetoothProfile.STATE_CONNECTED) ||
                              (contextType == CONTEXT_TYPE_VOICE &&
                              sm.getVoiceConnectionState() == BluetoothProfile.STATE_CONNECTED)) {
                            isConnected = true;
                            Log.d(TAG, "At least one member is in connected state for context: " + contextType);
                            break;
                        }
                    }
                }
            }
        }
        return isConnected;
    }

    public boolean isPeerDeviceConnected(BluetoothDevice device, int setid) {
        boolean isConnected = false;
        Log.d(TAG, "isPeerDeviceConnected(): device: " + device + " setid: " + setid);
        if (setid == INVALID_SET_ID)
          return isConnected;
        if (mAcmDevices.size() != 0) {
            for (BluetoothDevice dm : mAcmDevices.keySet()) {
                BluetoothAcmDevice d = mAcmDevices.get(dm);
                if (d == null)
                   continue;
                Log.d(TAG, "isPeerDeviceConnected(): d.msetID: " + d.msetID +
                           " d.mState: " + d.mState);
                if ((d.msetID == setid) && !Objects.equals(dm, device)) {
                    if (d.mState == BluetoothProfile.STATE_CONNECTED) {
                        isConnected = true;
                        Log.d(TAG, "At least one member is in connected state");
                        break;
                    }
                }
            }
        }
        return isConnected;
    }

    public boolean isPeerDeviceConnecting(BluetoothDevice device, int setid) {
        boolean isConnecting = false;
        Log.d(TAG, "isPeerDeviceConnecting(): device: " + device + " setid: " + setid);
        if (mAcmDevices.size() != 0) {
            for (BluetoothDevice dm : mAcmDevices.keySet()) {
                BluetoothAcmDevice d = mAcmDevices.get(dm);
                if (d == null)
                  continue;
                Log.d(TAG, "isPeerDeviceConnecting(): d.msetID: " + d.msetID +
                           " d.mState: " + d.mState);
                if ((d.msetID == setid) && !Objects.equals(dm, device)) {
                    if (d.mState == BluetoothProfile.STATE_CONNECTING) {
                        isConnecting = true;
                        Log.d(TAG, "At least one member is in Connecting state");
                        break;
                    }
                }
            }
        }
        return isConnecting;
    }

    public boolean isPeerDeviceStreaming(BluetoothDevice device, int setid, int contextType) {
        boolean isStreaming = false;
        if (mAcmDevices.size() != 0) {
            for (BluetoothDevice dm : mAcmDevices.keySet()) {
                BluetoothAcmDevice d = mAcmDevices.get(dm);
                if ((d != null) && (d.msetID == setid) && !Objects.equals(dm, device)) {
                    if (d.mState == BluetoothProfile.STATE_CONNECTED) {
                        synchronized (mBtAcmLock) {
                            AcmStateMachine sm = mStateMachines.get(dm);
                            if (sm == null) {
                                return false;
                            }
                            if ((contextType == CONTEXT_TYPE_MUSIC && sm.isMusicPlaying()) ||
                                    (contextType == CONTEXT_TYPE_VOICE && sm.isVoicePlaying())) {
                                isStreaming = true;
                                Log.d(TAG, "At least one member is streaming, context type: " + contextType);
                                break;
                            }
                        }
                    }
                }
            }
        }
        return isStreaming;
    }

    public boolean isLastMemberUnbonded(BluetoothDevice device, int setid) {
        boolean isLastMember = true;
        Log.d(TAG, "isLastMemberUnbonded(): device: " + device + " setid: " + setid);
        if (mAcmDevices.size() != 0) {
            for (BluetoothDevice dm : mAcmDevices.keySet()) {
                BluetoothAcmDevice d = mAcmDevices.get(dm);
                if (d == null)
                  continue;
                Log.d(TAG, "isLastMemberUnbonded(): d.msetID: " + d.msetID +
                           " d.mState: " + d.mState);
                if ((d.msetID == setid) && !Objects.equals(dm, device) &&
                        mAdapterService.getBondState(dm) == BluetoothDevice.BOND_BONDED) {
                    isLastMember = false;
                    Log.d(TAG, "At least another member is in bonded state");
                    break;
                }
            }
        }
        return isLastMember;
    }

    public boolean isShoPendingStop() {
        Log.d(TAG, "isShoPendingStop " + mShoPend);
        return mShoPend;
    }

    public void resetShoPendingStop() {
        mShoPend = false;
    }

    public boolean isVoiceShoPendingStop() {
        Log.d(TAG, "isVoiceShoPendingStop " + mVoiceShoPend);
        return mVoiceShoPend;
    }

    public void resetVoiceShoPendingStop() {
        Log.d(TAG, "resetVoiceShoPendingStop " + mVoiceShoPend);
        mVoiceShoPend = false;
        if (ApmConst.getAospLeaEnabled()) {
            CallAudio mCallAudio = CallAudio.get();
            if (mCallAudio != null && mCallAudio.isVoipLeaWarEnabled() &&
                    mCallAudio.isVirtualCallStarted()) {
                Log.d(TAG, "stopScoUsingVirtualVoiceCall for SHO");
                mCallAudio.stopScoUsingVirtualVoiceCall();
            }
        }
    }

    public BluetoothDevice getVoiceActiveDevice() {
        return mActiveDeviceVoice;
    }

    public void removePeersFromBgWl(BluetoothDevice device, int setid) {
        synchronized (mStateMachines) {
            BluetoothDevice d = null;
            List<BluetoothDevice> members = mCsipManager.getSetMembers(setid);
            if (members == null) {
                Log.d(TAG, "No set member found");
                return;
            }
            Iterator<BluetoothDevice> i = members.iterator();
            if (i != null) {
                while (i.hasNext()) {
                    d = i.next();
                    if (!(Objects.equals(d, device))) {
                        Log.d(TAG, "Device: " + d);
                        AcmStateMachine sm = mStateMachines.get(d);
                        if (sm == null) {
                            return;
                        }
                        sm.removeDevicefromBgWL();
                    }
                }
            }
        }
    }

    public boolean isAcmPlayingMusic(BluetoothDevice device) {
        if (DBG) {
            Log.d(TAG, "isAcmPlayingMusic(" + device + ")");
        }
        if (device == null) return false;
        synchronized (mBtAcmLock) {
            AcmStateMachine sm = mStateMachines.get(device);
            if (sm == null) {
                return false;
            }
            return sm.isMusicPlaying();
        }
    }

    public boolean isAcmPlayingVoice(BluetoothDevice device) {
        if (DBG) {
            Log.d(TAG, "isAcmPlayingVoice(" + device + ")");
        }
        if (device == null) return false;
        synchronized (mBtAcmLock) {
            AcmStateMachine sm = mStateMachines.get(device);
            if (sm == null) {
                return false;
            }
            return sm.isVoicePlaying();
        }
    }

    public boolean isAcmCallActive() {
        if (DBG) {
            Log.d(TAG, "isAcmCallActive()");
        }
        HeadsetService mHeadsetService = HeadsetService.getHeadsetService();
        CallAudio mCallAudio = CallAudio.get();
        if (mHeadsetService != null && mCallAudio != null) {
            return mHeadsetService.isScoOrCallActive() ||
                    (mCallAudio.isVoipLeaWarEnabled() &&
                    mCallAudio.isVirtualCallStarted());
        }
        return false;
    }

    public BluetoothDevice getGroup(BluetoothDevice device) {
        Log.d(TAG, "Get group address for (" + device + ")");
        if (device == null)
            return null;

        if (device.getAddress().contains("9E:8B:00:00:00")) {
            Log.d(TAG, "Called for group address");
            return device;
        }
        BluetoothDevice dm = null;
        int Id = mCsipManager.getCsipSetId(device, null /*ACM_UUID*/);

        if (mAcmDevices != null) {
            Log.d(TAG, "Hash Map is not NULL");
            BluetoothAcmDevice d = mAcmDevices.get(device);
            if (d != null && Id == d.msetID) {
                dm = d.mGrpDevice;
            } else if (d != null) {
                Log.d(TAG, "Remove Device from hash map, SetId didn't match with map");
                mAcmDevices.remove(device);
            }
        }
        if (dm == null) {
            if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                Log.d(TAG, "getGroup: device is not bonded");
                return device;
            } else {
                Log.d(TAG, "Group address is NULL, make New");
                dm = makeGroupBdAddress(device, BluetoothProfile.STATE_DISCONNECTED, Id);
            }
        }
        return dm;
    }

    public void updateActiveProfile(BluetoothDevice device, int contextType, int profileType) {
        Log.d(TAG, "updateActiveProfile: " + device + " contextType: " + contextType + " profileType: " + profileType +
              " mActiveDeviceProfile: " + mActiveDeviceProfile);
        mActiveDeviceProfile = profileType;
    }
    public int setActiveDevice(BluetoothDevice device, int contextType, int profileType, boolean playReq) {

        Log.d(TAG, "setActiveDevice: " + device + " contextType: " + contextType + " profileType: " + profileType +
                " play req: " + playReq + " mActiveDeviceProfile: " + mActiveDeviceProfile+ " mActiveDeviceVoiceProfile: " + mActiveDeviceVoiceProfile);

        if (Objects.equals(device, mActiveDevice) && contextType == CONTEXT_TYPE_MUSIC && (mActiveDeviceProfile == profileType)) {
            Log.e(TAG, "setActiveDevice(" + device + "): already set to active for media and profileType same as active profile");
            return ActiveDeviceManagerService.ALREADY_ACTIVE;
        }
        if (Objects.equals(device, mActiveDeviceVoice) && contextType == CONTEXT_TYPE_VOICE && (mActiveDeviceVoiceProfile == profileType)) {
            Log.e(TAG, "setActiveDevice(" + device + "): already set to active for voice and profileType same as active profile");
            return ActiveDeviceManagerService.ALREADY_ACTIVE;
        }
        if (contextType == CONTEXT_TYPE_MUSIC) {
            mShoPend = false;
            if (!Objects.equals(device, mActiveDevice)) {
                if (mActiveDevice != null && mActiveDevice.getAddress().contains("9E:8B:00:00:00")) {
                    byte[] addrByte = Utils.getByteAddress(mActiveDevice);
                    int set_id = addrByte[5];
                    List<BluetoothDevice> members = mCsipManager.getSetMembers(set_id);
                    if (members == null) {
                        Log.d(TAG, "No set member found");
                    } else {
                        Iterator<BluetoothDevice> i = members.iterator();
                        if (i != null) {
                            while (i.hasNext()) {
                                BluetoothDevice addr = i.next();
                                Log.d(TAG, "isAcmPlayingMusic(addr) " + isAcmPlayingMusic(addr));
                                if (isAcmPlayingMusic(addr)) {
                                    mShoPend = true;
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    Log.d(TAG, "TWM active device");
                    mShoPend = isAcmPlayingMusic(mActiveDevice);
                }
            }
            Log.d(TAG, "mShoPend " + mShoPend);
        } else if (contextType == CONTEXT_TYPE_VOICE) {
            mVoiceShoPend = false;
            if (!Objects.equals(device, mActiveDeviceVoice)) {
                if (mActiveDeviceVoice != null && mActiveDeviceVoice.getAddress().contains("9E:8B:00:00:00")) {
                    byte[] addrByte = Utils.getByteAddress(mActiveDeviceVoice);
                    int set_id = addrByte[5];
                    List<BluetoothDevice> members = mCsipManager.getSetMembers(set_id);
                    if (members == null) {
                        Log.d(TAG, "No set member found");
                    } else {
                        Iterator<BluetoothDevice> i = members.iterator();
                        if (i != null) {
                            while (i.hasNext()) {
                                BluetoothDevice addr = i.next();
                                Log.d(TAG, "isAcmPlayingVoice(addr) " + isAcmPlayingVoice(addr));
                                if (isAcmPlayingVoice(addr)) {
                                    mVoiceShoPend = true;
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    Log.d(TAG, "TWM active device");
                    mVoiceShoPend = isAcmPlayingVoice(mActiveDeviceVoice);
                }
            }
            Log.d(TAG, "mVoiceShoPend " + mVoiceShoPend);
        }
        Log.d(TAG, "old mActiveDevice: " + mActiveDevice + " & old mActiveDeviceVoice: " + mActiveDeviceVoice);

        if (setActiveDeviceAcm(device, contextType, profileType)) {
            if (contextType == CONTEXT_TYPE_MUSIC) {
              mActiveDevice = device;
              mActiveDeviceProfile = profileType;
            } else if (contextType == CONTEXT_TYPE_VOICE) {
              if (ApmConst.getAospLeaEnabled()) {
                if (!mVoiceShoPend || (device != null &&
                    mActiveDeviceVoice != null)) {
                  CallAudio mCallAudio = CallAudio.get();
                  if (mCallAudio != null && mCallAudio.isVoipLeaWarEnabled() &&
                      mCallAudio.isVirtualCallStarted()) {
                    Log.d(TAG, "stopScoUsingVirtualVoiceCall for SHO");
                    mCallAudio.stopScoUsingVirtualVoiceCall();
                  }
                }
              }
              mActiveDeviceVoice = device;
              mActiveDeviceVoiceProfile = profileType;
            }
            Log.d(TAG, "new mActiveDevice: " + mActiveDevice + " & new mActiveDeviceVoice: " + mActiveDeviceVoice);
            if(!playReq) {
               if ((mShoPend && contextType == CONTEXT_TYPE_MUSIC) ||
                       (mVoiceShoPend && contextType == CONTEXT_TYPE_VOICE)) {
                   Log.d(TAG, "setActiveDevice(" + device + "): returns with status " + ActiveDeviceManagerService.SHO_PENDING);
                   return ActiveDeviceManagerService.SHO_PENDING;
               } else {
                   Log.d(TAG, "setActiveDevice(" + device + "): returns with status " + ActiveDeviceManagerService.SHO_SUCCESS);
                   return ActiveDeviceManagerService.SHO_SUCCESS;
               }
            } else {
                Log.d(TAG, "setActiveDevice(" + device + "): returns with status " + ActiveDeviceManagerService.SHO_PENDING);
                return ActiveDeviceManagerService.SHO_PENDING;
            }
        }
        Log.d(TAG, "setActiveDevice(" + device + "): returns with status " + ActiveDeviceManagerService.SHO_FAILED);
        if (contextType == CONTEXT_TYPE_MUSIC) {
            Log.d(TAG, "reset mShoPend ");
            mShoPend = false;
        } else if (contextType == CONTEXT_TYPE_VOICE) {
            Log.d(TAG, "reset mVoiceShoPend ");
            mVoiceShoPend = false;
        }
        return ActiveDeviceManagerService.SHO_FAILED;
    }

    private boolean setActiveDeviceAcm(BluetoothDevice device, int contextType, int profileType) {
        synchronized(mActiveDeviceLock) {
            Log.d(TAG, "setActiveDeviceAcm: " + device);
            try {
                mAcmNativeInterfaceLock.readLock().lock();
                if (mAcmNativeInterface != null && !mAcmNativeInterface.setActiveDevice(device, profileType)) {
                    Log.e(TAG, "setActiveDevice(" + device + "): Cannot set as active in native layer");
                    return false;
                }
            } finally {
                mAcmNativeInterfaceLock.readLock().unlock();
            }
            Log.d(TAG, "setActiveDeviceAcm(" + device + "): returns true");
        }
        return true;
    }

    public void setCodecConfigPreference(BluetoothDevice device,
                                          BluetoothCodecConfig codecConfig, int contextType) {

        if (DBG) {
            Log.d(TAG, "setCodecConfigPreference(" + device + "): "
                    + Objects.toString(codecConfig));
        }
        mAcmCodecConfig.setCodecConfigPreference(device, codecConfig, contextType);
    }

    public void ChangeCodecConfigPreference(BluetoothDevice device,
                                            String mesg) {

        if (DBG) {
            Log.d(TAG, "ChangeCodecConfigPreference " + device + "string: " + mesg);
        }
        if (device == null)
            return;
        mAcmName = mesg;
        synchronized (mStateMachines) {
            if (mAcmDevices.size() != 0) {
                for (BluetoothDevice dm : mAcmDevices.keySet()) {
                    BluetoothAcmDevice d = mAcmDevices.get(dm);
                    if (d == null)
                       continue;
                    if (Objects.equals(device, d.mGrpDevice)) {
                        if (d.mState == BluetoothProfile.STATE_CONNECTED) {
                            AcmStateMachine sm = getOrCreateStateMachine(dm);
                            if (sm == null) {
                                //TODO:handle this case
                                continue;
                            }
                            Message msg = sm.obtainMessage(AcmStateMachine.CODEC_CONFIG_CHANGED);
                            msg.obj = d.msetID;
                            sm.sendMessage(msg);
                        }
                    }
                }
            }
        }
    }

    public BluetoothDevice onlyGetGroupDevice(BluetoothDevice device) {
        Log.d(TAG, "onlyGetGroupDevice for (" + device + ")");
        if (device == null)
            return null;
        if (device.getAddress().contains("9E:8B:00:00:00")) {
            Log.d(TAG, "onlyGetGroupDevice, already a group device");
            return device;
        }

        if (mAcmDevices != null) {
            BluetoothAcmDevice d = mAcmDevices.get(device);
            if (d != null) {
                Log.d(TAG, "onlyGetGroupDevice, d.mGrpDevice=" + d.mGrpDevice);
                return d.mGrpDevice;
            }
        }

        int setid = mCsipManager.getCsipSetId(device, null /*ACM_UUID*/); //TODO: UUID what to set ?
        if (setid == INVALID_SET_ID) {
            Log.d(TAG, "onlyGetGroupDevice, device is not part of any group");;
            return device;
        } else {
            String address = "9E:8B:00:00:00:0";
            BluetoothDevice dev = getAddressFromString(address + setid);
            Log.d(TAG, "onlyGetGroupDevice, new grpDevice=" + dev);
            return dev;
        }
    }

    public void onStreamStateChange(BluetoothDevice device, boolean playing, int contextType) {
        BluetoothDevice grpDevice = onlyGetGroupDevice(device);

        Log.d(TAG, "onStreamStateChange(" + device + "), grpDevice=" +
                grpDevice + ", playing=" + playing + ", contextType=" + contextType);

        if (contextType == CONTEXT_TYPE_MUSIC) {
            if (!playing &&
                    mHandler.hasEqualMessages(MESSAGE_DELAYED_START_VOICE_STREAM, grpDevice) &&
                    !isAnyGroupMemberPlayingMusic(grpDevice)) {
                mHandler.removeEqualMessages(MESSAGE_DELAYED_START_VOICE_STREAM, grpDevice);
                Log.d(TAG, "onStreamStateChange(" + device + "), grpDevice=" +
                        grpDevice + ", start delayed voice stream");
                startStreamWithoutSync(grpDevice, CONTEXT_TYPE_VOICE);
            }
        }
    }

    public boolean isAnyGroupMemberPlayingMusic(BluetoothDevice device) {
        boolean isMusicPlaying = false;

        if (device == null)
            return false;

        BluetoothDevice grpDevice = onlyGetGroupDevice(device);

        for (BluetoothDevice dm : mAcmDevices.keySet()) {
            BluetoothAcmDevice d = mAcmDevices.get(dm);
            if (d == null)
                continue;
            if (!Objects.equals(grpDevice, d.mGrpDevice))
                continue;

            synchronized (mBtAcmLock) {
                AcmStateMachine sm = mStateMachines.get(dm);
                if (sm == null) {
                    continue;
                }

                if (sm.isMusicPlaying()) {
                    isMusicPlaying = true;
                    break;
                }
            }
        }

        if (DBG) {
            Log.d(TAG, "isAnyGroupMemberPlayingMusic(" + device + "), grpDevice=" + grpDevice +
                    ", isMusicPlaying=" + isMusicPlaying);
        }
        return isMusicPlaying;
    }

    public boolean StartStream(BluetoothDevice device, int contextType) {
        if (DBG) {
            Log.d(TAG, "startStream for " + device + " context type: " + contextType);
        }
        if (device == null)
            return false;

        if (contextType == CONTEXT_TYPE_VOICE && isAnyGroupMemberPlayingMusic(device)) {
            BluetoothDevice grpDevice = onlyGetGroupDevice(device);
            if (mHandler.hasEqualMessages(MESSAGE_DELAYED_START_VOICE_STREAM, grpDevice)) {
                Log.d(TAG, "startStream: pending to start voice stream, grpDevice=" + grpDevice);
                return true;
            } else {
                Log.d(TAG, "startStream: delay to start voice stream, grpDevice=" + grpDevice);
                Message m = mHandler.obtainMessage(MESSAGE_DELAYED_START_VOICE_STREAM, grpDevice);
                m.arg1 = contextType;
                mHandler.sendMessageDelayed(m, DELAY_START_VOICE_STREAM_MS);
                return true;
            }
        }

        return startStreamWithoutSync(device, contextType);
    }

    private boolean startStreamWithoutSync(BluetoothDevice device, int contextType) {
        if (DBG) {
            Log.d(TAG, "startStreamWithoutSync for " + device + " context type: " + contextType);
        }
        int connDelay = SystemProperties.getInt("persist.vendor.bluetooth.audioconnect.delay",
                 VOICE_AUDIO_CONNECTION_DELAY_DEFAULT);
        if (device == null)
            return false;
        synchronized (mStateMachines) {
            if (mAcmDevices.size() != 0) {
                for (BluetoothDevice dm : mAcmDevices.keySet()) {
                    BluetoothAcmDevice d = mAcmDevices.get(dm);
                    if (d == null)
                      continue;
                    if (Objects.equals(device, d.mGrpDevice)) {
                        if (d.mState == BluetoothProfile.STATE_CONNECTED) {
                            AcmStateMachine sm = getOrCreateStateMachine(dm);
                            if (sm == null) {
                                //TODO:handle this case
                                continue;
                            }
                            Message msg = sm.obtainMessage(AcmStateMachine.START_STREAM);
                            msg.obj = contextType;
                            if (contextType == CONTEXT_TYPE_VOICE) {
                                Log.i(TAG, "voice stream triggering after " + connDelay + "secs");
                                sm.sendMessageDelayed(msg, connDelay);
                            } else {
                                sm.sendMessage(msg);
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    public boolean StopStream(BluetoothDevice device, int contextType) {
        if (DBG) {
            Log.d(TAG, "stopStream for " + device+ " context type: " + contextType);
        }
        if (device == null)
            return false;

        if (contextType == CONTEXT_TYPE_VOICE) {
            BluetoothDevice grpDevice = onlyGetGroupDevice(device);
            if (mHandler.hasEqualMessages(MESSAGE_DELAYED_START_VOICE_STREAM, grpDevice)) {
                Log.d(TAG, "StopStream(" + device +
                        "), stop the pending voice stream, grpDevice=" + grpDevice);
                mHandler.removeEqualMessages(MESSAGE_DELAYED_START_VOICE_STREAM, grpDevice);
                return true;
            }
        }

        synchronized (mStateMachines) {
            if (mAcmDevices.size() != 0) {
                for (BluetoothDevice dm : mAcmDevices.keySet()) {
                    BluetoothAcmDevice d = mAcmDevices.get(dm);
                    if (d == null)
                       continue;
                    if (Objects.equals(device, d.mGrpDevice)) {
                        if (d.mState == BluetoothProfile.STATE_CONNECTED) {
                            AcmStateMachine sm = getOrCreateStateMachine(dm);
                            if (sm == null) {
                                //TODO:handle this case
                                continue;
                            }
                            Message msg = sm.obtainMessage(AcmStateMachine.STOP_STREAM);
                            msg.obj = contextType;
                            sm.sendMessage(msg);
                        }
                    }
                }
            }
        }
        return true;
    }

    public void setAbsoluteVolume(BluetoothDevice grpAddr, int volumeLevel, int audioType) {
        //Convert volume level to VCP level before sending.
        int vcpVolume = convertToVcpVolume(volumeLevel, audioType);
        Log.d(TAG, "AudioVolumeLevel " + volumeLevel + " vcpVolume "  + vcpVolume);
        if (grpAddr == null) {
            Log.e(TAG, "setAbsoluteVolume: grpAddr is null.");
            return;
        }

        if (audioType == ApmConst.AudioFeatures.MEDIA_AUDIO) {
            Log.d(TAG, "mActiveDevice " + mActiveDevice + " grpAddr "  + grpAddr
                    + " audioType " + audioType);
            if (!grpAddr.equals(mActiveDevice)) {
                Log.e(TAG, "Ignore setAbsoluteVolume for inactive device");
                return;
            }
        } else if (audioType == ApmConst.AudioFeatures.CALL_AUDIO) {
            Log.d(TAG, "mActiveDeviceVoice " + mActiveDeviceVoice + " grpAddr "  + grpAddr
                    + " audioType " + audioType);
            if (!grpAddr.equals(mActiveDeviceVoice)) {
                Log.e(TAG, "Ignore setAbsoluteVolume for inactive device");
                return;
            }
        } else if (audioType == ApmConst.AudioFeatures.BROADCAST_AUDIO) {
            Log.d(TAG, "No active device, grpAddr " + grpAddr + " is broadcast device or group");
        } else {
            Log.e(TAG, "Invalid audio type for set volume, ignore this request");
            return;
        }

        if (audioType == ApmConst.AudioFeatures.MEDIA_AUDIO) {
            mActiveDeviceLocalMediaVol = volumeLevel;
        } else if (audioType == ApmConst.AudioFeatures.CALL_AUDIO) {
            mActiveDeviceLocalVoiceVol = volumeLevel;
        }

        if (grpAddr.getAddress().contains("9E:8B:00:00:00")) {
            Log.d(TAG, "setAbsoluteVolume for group addr, send abs vol to all members");
            byte[] addrByte = Utils.getByteAddress(grpAddr);
            int set_id = addrByte[5];
            List<BluetoothDevice> members = mCsipManager.getSetMembers(set_id);
            if (members == null) {
                Log.d(TAG, "No set member found");
                return;
            }
            Iterator<BluetoothDevice> i = members.iterator();
            if (i != null) {
              while (i.hasNext()) {
                BluetoothDevice addr = i.next();
                Log.d(TAG, "send vol to member: " + addr);
                mVcpController.setAbsoluteVolume(addr, vcpVolume, audioType);
              }
            }
        } else {
            Log.d(TAG, "setAbsoluteVolume for single addr, send abs vol to " + grpAddr);
            mVcpController.setAbsoluteVolume(grpAddr, vcpVolume, audioType);
        }
    }

    public void setMute(BluetoothDevice grpAddr, boolean enableMute) {
        if (mActiveDevice == null) {
            Log.d(TAG, "No active device set, this grpAddr " + grpAddr + " is broadcast group");
        } else {
            Log.d(TAG, "mActiveDevice " + mActiveDevice + " grpAddr "  + grpAddr);
        }
        if (grpAddr == null) {
            Log.e(TAG, "grpAddr is null");
            return;
        }

        if (grpAddr.getAddress().contains("9E:8B:00:00:00")) {
            Log.d(TAG, "setMute for group addr, send mute/unmute to all members");
            byte[] addrByte = Utils.getByteAddress(grpAddr);
            int set_id = addrByte[5];
            List<BluetoothDevice> members = mCsipManager.getSetMembers(set_id);
            if (members == null) {
                Log.d(TAG, "No set member found");
                return;
            }
            Iterator<BluetoothDevice> i = members.iterator();
            if (i != null) {
              while (i.hasNext()) {
                BluetoothDevice addr = i.next();
                Log.d(TAG, "send setMute to member: " + addr);
                mVcpController.setMute(addr, enableMute);
              }
            }
        } else {
            Log.d(TAG, "setMute for single addr, send mute/unmute to " + grpAddr);
            mVcpController.setMute(grpAddr, enableMute);
        }
    }

    public void onVolumeStateChanged(BluetoothDevice device, int vol, int audioType) {
        VolumeManager service = VolumeManager.get();
        //Get or Make group address
        BluetoothDevice grpDev = getGroup(device);

        if (audioType == ApmConst.AudioFeatures.BROADCAST_AUDIO) {
            Log.d(TAG, "volume notification for Broadcast audio");
        } else if (audioType == ApmConst.AudioFeatures.MEDIA_AUDIO) {
            Log.d(TAG, "volume notification for Media audio");
        } else if (audioType == ApmConst.AudioFeatures.CALL_AUDIO) {
            Log.d(TAG, "volume notification for Call audio");
        } else {
            // remote volume change case
            Log.d(TAG, "Volume change from remote: " + device +  " vcp vol " + vol);
            audioType = service.getActiveAudioType(device);
        }

        //Convert vol
        int audioVolume = convertToAudioStreamVolume(vol, audioType);
        Log.d(TAG, "vcp vol " + vol + " audioVolume " + audioVolume);

        if (audioType == ApmConst.AudioFeatures.BROADCAST_AUDIO) {
            Log.d(TAG, "update volume to APM for Broadcast audio");
            service.onVolumeChange(grpDev, audioVolume, audioType);
        } else if (audioType == ApmConst.AudioFeatures.MEDIA_AUDIO) {
            //Check if new vol is diff than active group vol
            Log.d(TAG, "new vol: " + audioVolume + " mActiveDeviceLocalMediaVol: " + mActiveDeviceLocalMediaVol);
            if (mActiveDeviceLocalMediaVol != audioVolume) {
                Log.d(TAG, "new vol is different than mActiveDeviceLocalMediaVol update APM");
                service.onVolumeChange(grpDev, audioVolume, audioType);
                mActiveDeviceLocalMediaVol = audioVolume;
            } else {
                Log.d(TAG, "local active media vol same as device new vol, ignore sending to APM");
            }
        } else if (audioType == ApmConst.AudioFeatures.CALL_AUDIO) {
            Log.d(TAG, "new vol: " + audioVolume + " mActiveDeviceLocalVoiceVol: " + mActiveDeviceLocalVoiceVol);
            if (mActiveDeviceLocalVoiceVol != audioVolume) {
                Log.d(TAG, "new vol is different than mActiveDeviceLocalVoiceVol update APM");
                service.onVolumeChange(grpDev, audioVolume, audioType);
                mActiveDeviceLocalVoiceVol = audioVolume;
            } else {
                Log.d(TAG, "local active call vol same as device new vol, ignore sending to APM");
            }
        } else {
            Log.d(TAG, "No audio is streaming and is inactive device, ignore sending to APM");
        }

        //Check if this device is group device, then take lock (ignored for now) and update vol to other members as well.
        applyVolToOtherMembers(device, mCsipManager.getCsipSetId(device, null /*ACM_UUID*/), vol, audioType);
    }

    public void onMuteStatusChanged(BluetoothDevice device, boolean isMuted) {
        VolumeManager service = VolumeManager.get();
        //Get or Make group address
        BluetoothDevice grpDev = getGroup(device);
        //default context type
        int c_type = CONTEXT_TYPE_UNKNOWN;
        boolean mPtsTest = SystemProperties.getBoolean("persist.vendor.service.bt.vcp_controller.pts", false);

        Log.d(TAG, "isMuted " + isMuted + " mActiveDeviceIsMuted " + mActiveDeviceIsMuted);

        if (!mVcpController.isBroadcastDevice(device) && ((mActiveDevice == null) || (mActiveDevice != grpDev)) && !mPtsTest) {
            Log.d(TAG, "unicast-mode, either Active device null or muteStatus changed for inactive device -- return");
            return;
        }

        if ((mActiveDevice == null) || (mActiveDevice != grpDev)) {
            Log.d(TAG, "No Active device or device is not active, seems in Broadcast mode");
            c_type = CONTEXT_TYPE_BROADCAST_AUDIO;
        } else {
            Log.d(TAG, "mActiveDevice " + mActiveDevice + " grpDev "  + grpDev + " device " + device);
            c_type = CONTEXT_TYPE_MUSIC;
        }

        //Check if new mute state is diff than active group mute state
        if (mActiveDeviceIsMuted != isMuted) {
            Log.d(TAG, "new mute state is different than mActiveDeviceIsMuted update APM");
            service.onMuteStatusChange(grpDev, isMuted, CONTEXT_TYPE_MUSIC);
            mActiveDeviceIsMuted = isMuted;
        } else {
            Log.d(TAG, "local active device mute state same as device new mute state, ignore sending to APM, but send to other members");
        }

        //Check if this device is group device, then take lock(ignored for now) and update mute state to other members as well.
        applyMuteStateToOtherMembers(device, mCsipManager.getCsipSetId(device, null /*ACM_UUID*/), isMuted);
    }

    public void setAbsVolSupport(BluetoothDevice device, boolean isAbsVolSupported, int initialVol) {
        VolumeManager service = VolumeManager.get();
        //Get or Make group address
        BluetoothDevice grpDev = getGroup(device);
        if (grpDev == null) {
            Log.d(TAG, "Group not created, return");
            return;
        }
        if ((mVcpController.getConnectionState(device) == BluetoothProfile.STATE_CONNECTED) && isAbsVolSupported) {
            if (!isVcpPeerDeviceConnected(device, mCsipManager.getCsipSetId(device, null /*ACM_UUID*/))) {
                Log.d(TAG, "VCP Peer device not connected, this is 1st member, update support to APM ");
                //get current vol from VCP and send to APM during connection.
                Log.d(TAG, "VCP initialVol " + initialVol + " when connected device " + device);
                Log.d(TAG, "Update APM with connection vol " + convertToAudioStreamVolume(
                        initialVol, ApmConst.AudioFeatures.MEDIA_AUDIO));
                service.setAbsoluteVolumeSupport(grpDev, isAbsVolSupported,
                        convertToAudioStreamVolume(initialVol, ApmConst.AudioFeatures.MEDIA_AUDIO),
                        ApmConst.AudioProfiles.VCP);

                //get current mute state from VCP and sent to APM during connection.
                Log.d(TAG, "VCP mute state " + mVcpController.isMute(device) + " when connected device " + device);
                service.onMuteStatusChange(grpDev, mVcpController.isMute(device), CONTEXT_TYPE_MUSIC);
            } else {
                Log.d(TAG, "VCP Peer device connected, this is not 1st member, skip update to APM");
            }
        } else if ((mVcpController.getConnectionState(device) == BluetoothProfile.STATE_DISCONNECTED) && !isAbsVolSupported) {
            if (isVcpPeerDeviceConnected(device, mCsipManager.getCsipSetId(device, null /*ACM_UUID*/))) {
                Log.d(TAG, "VCP Peer device connected, this is not last member, skip update to APM ");
            } else {
                Log.d(TAG, "VCP Peer device not connected, this is last member, update to APM");
                service.setAbsoluteVolumeSupport(grpDev, isAbsVolSupported, ApmConst.AudioProfiles.VCP);
            }
        }
    }

    public boolean isVcpPeerDeviceConnected(BluetoothDevice device, int setid) {
      boolean isVcpPeerConnected = false;
      if (setid == INVALID_SET_ID)
          return isVcpPeerConnected;
      List<BluetoothDevice> members = mCsipManager.getSetMembers(setid);
      if (members == null) {
          Log.d(TAG, "No set member found");
          return isVcpPeerConnected;
      }
      Iterator<BluetoothDevice> i = members.iterator();
      if (i != null) {
          while (i.hasNext()) {
              BluetoothDevice addr = i.next();
              if (!Objects.equals(addr, device) && (mVcpController.getConnectionState(addr) == BluetoothProfile.STATE_CONNECTED)) {
                  isVcpPeerConnected = true;
                  Log.d(TAG, "At least one other vcp member is in connected state");
                  break;
              }
          }
      }
      return isVcpPeerConnected;
    }

    private int convertToAudioStreamVolume(int volume, int audioType) {
        if (ApmConst.getAospLeaEnabled()) {
            VolumeManager volumeManager = VolumeManager.get();
            if (volumeManager != null) {
                mAudioStreamMax = volumeManager.getAudioStreamMaxVolume(audioType);
            }
        }
        // Rescale volume to match AudioSystem's volume
        return (int) Math.round((double) volume * mAudioStreamMax / VCP_MAX_VOL);
    }

    private int convertToVcpVolume(int volume, int audioType) {
        if (ApmConst.getAospLeaEnabled()) {
            VolumeManager volumeManager = VolumeManager.get();
            if (volumeManager != null) {
                mAudioStreamMax = volumeManager.getAudioStreamMaxVolume(audioType);
            }
        }
        return (int) Math.ceil((double) volume * VCP_MAX_VOL / mAudioStreamMax);
    }

    private void applyVolToOtherMembers(BluetoothDevice device, int setid, int volume, int audioType) {
        if (setid == INVALID_SET_ID)
            return;
        List<BluetoothDevice> members = mCsipManager.getSetMembers(setid);
        if (members == null) {
            Log.d(TAG, "No set member found");
            return;
        }
        Iterator<BluetoothDevice> i = members.iterator();
        if (i != null) {
            while (i.hasNext()) {
                BluetoothDevice addr = i.next();
                if (!Objects.equals(addr, device)) {
                    Log.d(TAG, "send vol changed to addr " + addr);
                    mVcpController.setAbsoluteVolume(addr, volume, audioType);
                }
            }
        }
    }

    private void applyMuteStateToOtherMembers(BluetoothDevice device, int setid, boolean muteState) {
        if (setid == INVALID_SET_ID)
            return;
        List<BluetoothDevice> members = mCsipManager.getSetMembers(setid);
        if (members == null) {
            Log.d(TAG, "No set member found");
            return;
        }
        Iterator<BluetoothDevice> i = members.iterator();
        if (i != null) {
            while (i.hasNext()) {
                BluetoothDevice addr = i.next();
                if (!Objects.equals(addr, device)) {
                    Log.d(TAG, "send mute state to addr " + addr);
                    mVcpController.setMute(addr, muteState);
                }
            }
        }
    }

    public int getVcpConnState(BluetoothDevice device) {
        return mVcpController.getConnectionState(device);
    }

    public int getVcpConnMode(BluetoothDevice device) {
        return mVcpController.getConnectionMode(device);
    }

    public boolean isVcpMute(BluetoothDevice device) {
        return mVcpController.isMute(device);
    }

    public String getAcmName() {
        return mAcmName;
    }

    public BluetoothLeAudioCodecStatus getLeAudioCodecStatus(BluetoothDevice device) {
        if (DBG) {
            Log.d(TAG, "getLeAudioCodecStatus(" + device + ")");
        }
        synchronized (mBtAcmLock) {
            AcmStateMachine sm = mStateMachines.get(device);
            if (sm == null) {
                return null;
            }
            return sm.getLeAudioCodecStatus();
        }
    }

    public void hfpCallBapMediaSync(boolean isBapMediaSuspend) {
        Log.d(TAG, "hfpCallBapMediaSync(): isBapMediaSuspend: " + isBapMediaSuspend);
        mAcmNativeInterface.hfpCallBapMediaSync(isBapMediaSuspend);
    }

    private static class AcmBinder extends Binder implements IProfileServiceBinder {
        AcmBinder(AcmService service) {
        }
        @Override
        public void cleanup() {
        }
    }

    private BluetoothGroupCallback mBluetoothGroupCallback = new BluetoothGroupCallback() {
        public void onGroupClientAppRegistered(int status, int appId) {
            Log.d(TAG, "onGroupClientAppRegistered, status: " + status + "appId: " + appId);
            if (status == 0) {
                mCsipManager.updateAppId(appId);
                mIsCsipRegistered = true;
            } else {
                Log.e(TAG, "DeviceGroup registeration failed, status:" + status);
            }
        }

        public void onConnectionStateChanged (int state, BluetoothDevice device) {
            Log.d(TAG, "onConnectionStateChanged: Device: " + device + "state: " + state);
            //notify statemachine about device connection state
            synchronized (mStateMachines) {
                  AcmStateMachine sm = getOrCreateStateMachine(device);
                  Message msg = sm.obtainMessage(AcmStateMachine.CSIP_CONNECTION_STATE_CHANGED);
                  msg.obj = state;
                  sm.sendMessage(msg);
            }
        }

        public void onExclusiveAccessChanged (int setId, int value, int status,
                                                   List<BluetoothDevice> devices) {
            Log.d(TAG, "onExclusiveAccessChanged: setId" + setId + devices + "status:" + status);

            //notify the statemachine about CSIP Lock status
            switch (status) {
                case ALL_LOCKS_ACQUIRED: {
                    synchronized (mStateMachines) {
                        for (BluetoothDevice device : devices) {
                            AcmStateMachine sm = getOrCreateStateMachine(device);
                            if (sm == null) {
                                //TODO:handle this case
                                continue;
                            }
                            Message msg = sm.obtainMessage(AcmStateMachine.CSIP_LOCK_STATUS_LOCKED);
                            msg.obj = setId;
                            msg.arg1 = value;
                            sm.sendMessage(msg);
                        }
                    }
                    break;
                }

                case SOME_LOCKS_ACQUIRED_REASON_TIMEOUT: //check if need to handle separately
                case SOME_LOCKS_ACQUIRED_REASON_DISC: {
                    BluetoothDevice mDevice = getCsipLockRequestedDevice();
                    if (devices.contains(mDevice)) {
                        synchronized (mStateMachines) {
                            for (BluetoothDevice device : devices) {
                                AcmStateMachine sm = getOrCreateStateMachine(device);
                                if (sm == null) {
                                    //TODO:handle this case
                                    continue;
                                }
                                Message msg = sm.obtainMessage(AcmStateMachine.CSIP_LOCK_STATUS_PARTIAL_LOCK);
                                msg.obj = setId;
                                msg.arg1 = value;
                                sm.sendMessage(msg);
                            }
                        }
                    } else {
                        Log.d(TAG, "Exclusive access requested device is not present in list, release access for all devices");
                        mCsipManager.setLock(setId, devices, mCsipManager.UNLOCK);
                    }
                } break;

                case LOCK_DENIED: {
                    synchronized (mStateMachines) {
                        for (BluetoothDevice device : devices) {
                            AcmStateMachine sm = getOrCreateStateMachine(device);
                            if (sm == null) {
                                //TODO:handle this case
                                continue;
                            }
                            Message msg = sm.obtainMessage(AcmStateMachine.CSIP_LOCK_STATUS_DENIED);
                            msg.obj = setId;
                            msg.arg1 = value;
                            sm.sendMessage(msg);
                        }
                    }
                } break;

                case LOCK_RELEASED: {
                    synchronized (mStateMachines) {
                        for (BluetoothDevice device : devices) {
                            AcmStateMachine sm = getOrCreateStateMachine(device);
                            if (sm == null) {
                                //TODO:handle this case
                                continue;
                            }
                            Message msg = sm.obtainMessage(AcmStateMachine.CSIP_LOCK_STATUS_RELEASED);
                            msg.obj = setId;
                            msg.arg1 = value;
                            sm.sendMessage(msg);
                        }
                    }
                } break;

                case LOCK_RELEASED_TIMEOUT: {
                    synchronized (mStateMachines) {
                        for (BluetoothDevice device : devices) {
                            AcmStateMachine sm = getOrCreateStateMachine(device);
                            if (sm == null) {
                                //TODO:handle this case
                                continue;
                            }
                            Message msg = sm.obtainMessage(AcmStateMachine.CSIP_LOCK_STATUS_RELEASED);
                            msg.obj = setId;
                            msg.arg1 = value;
                            sm.sendMessage(msg);
                        }
                    }
                } break;

                /*case INVALID_REQUEST_PARAMS: {
                    synchronized (mStateMachines) {
                        for (BluetoothDevice device : devices) {
                            AcmStateMachine sm = getOrCreateStateMachine(device);
                            if (sm == null) {
                                //TODO:handle this case
                                continue;
                            }
                            Message msg = sm.obtainMessage(AcmStateMachine.CSIP_LOCK_STATUS_INVALID_PARAM);
                            msg.obj = setId;
                            msg.arg1 = value;
                            sm.sendMessage(msg);
                        }
                    }
                } break;*/

                /*case LOCK_RELEASE_NOT_ALLOWED: {
                    synchronized (mStateMachines) {
                        for (BluetoothDevice device : devices) {
                            AcmStateMachine sm = getOrCreateStateMachine(device);
                            if (sm == null) {
                                //TODO:handle this case
                                continue;
                            }
                            Message msg = sm.obtainMessage(AcmStateMachine.CSIP_LOCK_STATUS_RELEASE);
                            msg.obj = setId;
                            msg.arg1 = value;
                            sm.sendMessage(msg);
                        }
                    }
                } break;*/

                case INVALID_VALUE:
                    break;
            }
        }

        //public void onSetMemberFound (int setId, BluetoothDevice device) {    }

        //public void onSetDiscoveryStatusChanged (int setId, int status, int reason) {    }

        //public void onLockAvailable (int setId, BluetoothDevice device) {    }

        //public void onNewSetFound (int setId,  BluetoothDevice device, UUID uuid) {     }

        //public void onLockStatusFetched (int setId, int lockStatus) {    }
    };

    public CsipManager getCsipManager(){
        return mCsipManager;
    }

    class CsipManager {
        public int LOCK = 0;
        public int UNLOCK = 0;

        int mCsipAppid;

        CsipManager() {
            LOCK = BluetoothDeviceGroup.ACCESS_GRANTED;
            UNLOCK = BluetoothDeviceGroup.ACCESS_RELEASED;

            registerCsip();
        }

        CsipManager(BluetoothGroupCallback callbacks) {
            LOCK = BluetoothDeviceGroup.ACCESS_GRANTED;
            UNLOCK = BluetoothDeviceGroup.ACCESS_RELEASED;

            registerCsip(callbacks);
        }

        void updateAppId(int id) {
            mCsipAppid = id;
        }

        int getAppId() {
            return mCsipAppid;
        }

        void registerCsip() {
            mCsipWrapper.registerGroupClientModule(mBluetoothGroupCallback);
        }

        void registerCsip(BluetoothGroupCallback callbacks) {
            mCsipWrapper.registerGroupClientModule(callbacks);
        }

        void unregisterCsip() {
            mCsipWrapper.unregisterGroupClientModule(mCsipAppid);
        }

        void connectCsip(BluetoothDevice device) {
            mCsipWrapper.connect(mCsipAppid, device);
        }

        void disconnectCsip(BluetoothDevice device) {
            mCsipWrapper.disconnect(mCsipAppid, device);
        }

        void setLock(int setId, List<BluetoothDevice> devices, int lockVal) {
            mCsipWrapper.setLockValue(mCsipAppid, setId, devices, lockVal);
        }

        DeviceGroup getCsipSet(int setId) {
            return mCsipWrapper.getCoordinatedSet(setId);
        }

        List<BluetoothDevice> getSetMembers(int setId) {
            DeviceGroup set = getCsipSet(setId);
            if(set != null)
                return set.getDeviceGroupMembers();
            return null;
        }

        int getCsipSetId(BluetoothDevice device, ParcelUuid uuid) {
            if (mCsipWrapper != null &&
                mCsipWrapper.checkIncludingServiceForDeviceGroup(device, CAP_UUID)) {
                uuid = CAP_UUID;
            }
            return mCsipWrapper.getRemoteDeviceGroupId(device, uuid);
            //return -1;
        }
    }
}
