/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
*/
/*
 * Copyright (c) 2020, The Linux Foundation. All rights reserved.

 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.

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
 */

package com.android.bluetooth.broadcast;

import static android.Manifest.permission.BLUETOOTH_CONNECT;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothBroadcast;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothCodecConfig;
import android.bluetooth.BluetoothCodecStatus;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeAudioCodecConfigMetadata;
import android.bluetooth.BluetoothLeAudioContentMetadata;
import android.bluetooth.BluetoothLeBroadcastChannel;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastSettings;
import android.bluetooth.BluetoothLeBroadcastSubgroup;
import android.bluetooth.BluetoothLeBroadcastSubgroupSettings;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothStatusCodes;
import android.bluetooth.BluetoothUuid;
import android.bluetooth.IBluetoothBroadcast;
import android.bluetooth.IBluetoothLeBroadcastCallback;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.HandlerThread;
import android.util.Log;
import android.util.StatsLog;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import android.content.AttributionSource;
import com.android.bluetooth.Utils;

import android.os.Handler;
import android.os.Message;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;

import com.android.bluetooth.BluetoothMetricsProto;
import com.android.bluetooth.Utils;
import com.android.bluetooth.avrcp.Avrcp;
import com.android.bluetooth.avrcp.Avrcp_ext;
import com.android.bluetooth.avrcp.AvrcpTargetService;
import com.android.bluetooth.a2dp.A2dpService;
import com.android.bluetooth.btservice.AdapterService;
import com.android.bluetooth.btservice.MetricsLogger;
import com.android.bluetooth.btservice.ProfileService;
import com.android.bluetooth.btservice.ServiceFactory;
import com.android.bluetooth.ba.BATService;
import com.android.bluetooth.gatt.GattService;
import com.android.bluetooth.lebroadcast.LeBroadcastServIntf;
import com.android.bluetooth.mcp.McpService;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.bluetooth.hfp.HeadsetService;
import com.android.modules.utils.SynchronousResultReceiver;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.AdvertisingSet;
import android.bluetooth.le.AdvertisingSetCallback;
import android.bluetooth.le.AdvertisingSetParameters;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.PeriodicAdvertisingParameters;
import android.media.MediaMetadata;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.UUID;
import java.util.HashMap;
import android.os.ParcelUuid;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import com.android.bluetooth.bc.BCService;
import com.android.bluetooth.apm.ActiveDeviceManagerService;
import com.android.bluetooth.apm.ApmConst;
import com.android.bluetooth.apm.DeviceProfileMap;
import com.android.bluetooth.apm.MediaAudio;
import com.android.bluetooth.R;
import android.graphics.Color;
import android.net.Uri;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.NotificationChannel;
/**
 * Provides Bluetooth Broadcast profile, as a service in the Bluetooth application.
 * @hide
 */
public class BroadcastService extends ProfileService {
    private static final boolean DBG = true;
    private static final boolean VDBG = true;
    private static final String TAG = "BroadcastService";
    private final Object mBroadcastLock = new Object();
    private static BroadcastService sBroadcastService;
    private Context mContext = null;
    @VisibleForTesting
    BroadcastNativeInterface mBroadcastNativeInterface;
    @VisibleForTesting
    ServiceFactory mFactory = new ServiceFactory();
    private AudioManager mAudioManager;
    int mBroadcastState = BluetoothBroadcast.STATE_DISABLED;
    int mBroadcastAudioState = BluetoothBroadcast.STATE_NOT_PLAYING;
    private String mEncryptionString;
    private byte[] mEncKey = new byte[16];
    private byte [] BigBroadcastCode = new byte [16];
    private byte [] mBroadcastID = new byte[3];
    private final int mBroadcastIdLength = 3;
    private boolean mEncryptionEnabled = true;
    private boolean mPartialSimulcast = false;//dual quality simulcast
    private boolean mEncKeyRefreshed = false;
    private int mEncryptionLength =16;
    private int mDefaultEncryptionLength = 16;
    private int [] bis_handles;
    private int mBIGHandle = -1;
    private int mNumBises = -1;
    private int mNumSubGrps = 1;
    private int mPD = 0;
    private boolean goingDown = false;
    private boolean mIsAdvertising = false;
    private BroadcastMessageHandler mHandler;
    private AdvertisingSetCallback mCallback;
    private AdvertisingSet mAdvertisingSet;
    List <BisInfo> mBisInfo;
    Map<Integer, MetadataLtv>mMetaInfo = Collections.synchronizedMap(new HashMap<>());;
    private String mAdvAddress;
    private int mAdvAddressType;
    private BluetoothLeAdvertiser mAdvertiser;
    private BluetoothCodecStatus mCodecStatus;
    private BluetoothCodecConfig mCodecConfig;
    private BluetoothCodecConfig mHapCodecConfig;
    private BroadcastCodecConfig mBroadcastCodecConfig;
    private BroadcastAdvertiser mBroadcastAdvertiser;
    private int mBroadcastConfigSettings;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBroadcastDevice = null;
    private boolean mBroadcastDeviceIsActive = false;
    TrackMetadata mTrackMetadata;
    private String mBroadcastAddress = "FA:CE:FA:CE:FA:CE";
    ActiveDeviceManagerService mActiveDeviceManager;
    public static UUID BROADCAST_AUDIO_UUID = UUID.fromString("00001852-0000-1000-8000-00805F9B34FB");
    public static UUID BASIC_AUDIO_UUID = UUID.fromString("00001851-0000-1000-8000-00805F9B34FB");
    public static UUID PUBLIC_BROADCAST_AUDIO_UUID = UUID.fromString("00001856-0000-1000-8000-00805F9B34FB");
    private BroadcastBase mBroadcastBase;
    private MediaAudio mMediaAudio;
    private boolean new_codec_id = false;
    private static int mSecPhy = 1;
    private static int mTxPowerLevel = 1;
    private static int mPaInt;
    private static int mPreferPaInt;
    private boolean mNewVersion = false;
    private boolean mBAIdleTimeout = false;
    private static int BROADCAST_INACTIVITY_TIMEOUT_MS = 900000;//15min
    private static final int mMinutesInMS = 60000;
    List <String> broadcast_supported_config = new ArrayList<String>(List.of("16_2", "24_2", "48_1", "48_2", "48_3", "48_4", "48_5", "48_6"));
    private static final String BROADCAST_NOTIFICATION_ID = "broadcast_notification";
    private NotificationManager mNotificationManager;
    int mNOTIFICATION_ID = -1;
    private boolean mBANotification = false;
    private int mCcidNum;
    private int mStreamingAudioContext;
    private static final String ACTION_UPDATE_BROADCAST_METADATA =
                 "qti.intent.bluetooth.action.UPDATE_BROADCAST_METADATA";
    private static final String METADATA_AUDIO_CONTEXT =
                 "qti.bluetooth.extra.METADATA_AUDIO_CONTEXT";
    private static final String METADATA_CCID =
               "qti.bluetooth.extra.METADATA_CCID";
    private static final int MSG_ENABLE_BROADCAST = 1;
    private static final int MSG_DISABLE_BROADCAST = 2;
    private static final int MSG_SET_ENCRYPTION_KEY = 3;
    private static final int MSG_GET_ENCRYPTION_KEY = 4;
    private static final int MSG_SET_BROADCAST_ACTIVE = 5;
    private static final int MSG_UPDATE_BROADCAST_ADV_SET = 6;
    private static final int MSG_ADV_DATA_SET = 7;
    private static final int MSG_SET_AUDIO_PATH = 8;
    private static final int MSG_RESET_ENCRYPTION_FLAG_TIMEOUT = 9;
    private static final int MSG_FROM_NATIVE_CODEC_STATE = 10;
    private static final int MSG_FROM_NATIVE_BROADCAST_STATE = 11;
    private static final int MSG_FROM_NATIVE_ENCRYPTION_KEY = 12;
    private static final int MSG_FROM_NATIVE_BROADCAST_AUDIO_STATE = 13;
    private static final int MSG_FROM_NATIVE_SETUP_BIG = 14;
    private static final int MSG_UPDATE_BROADCAST_STATE = 15;
    private static final int MSG_FROM_NATIVE_BROADCAST_ID = 16;
    private static final int MSG_BROADCAST_INACTIVE_TIMEOUT = 17;
    private static final int MSG_SET_BROADCAST_CODE = 18;
    private static final int MAX_NUMBEROF_BROADCAST = 1;
    private static final int BROADCAST_CODE_LENGTH_MIN = 4;
    private static final int BROADCAST_CODE_LENGTH_MAX = 16;
    private int mPbsAudioQuality = 0;
    private static final int PBS_STD_QUALITY = 1;
    private static final int PBS_HIGH_QUALITY = 2;
    private static final int PBP_META_DATA_LENGTH = 3;
    private static final int PBP_META_DATA_TYPE = 3;
    private static final int BAP_DEFAULT_PA_INTERVAL = 360;
    private static final int BAP_CIG_CALCULATOR_PA_INTERVAL = 288;
    private static final int PBP_DEFAULT_PA_INTERVAL = 120;
    private static final int PBP_CIG_CALCULATOR_PA_INTERVAL = 96;

    private RemoteCallbackList<IBluetoothLeBroadcastCallback>
            mBroadcastCallbacks;
    private BluetoothLeAudioContentMetadata mAudioContentMetadata;
    private BluetoothLeAudioContentMetadata mPublicMetadata;
    private final Map<Integer, Integer>
            mBroadcastIdMap = new HashMap<>();
    private final Map<Integer, BluetoothLeBroadcastMetadata>
            mBroadcastMetadataMap = new HashMap<>();
    private final Map<Integer, AdvertisingSet>
            mAdvertisingSets = Collections.synchronizedMap(new HashMap<>());
    private final Object mCallbackNotifyLock = new Object();
    private String mBroadcastName;
    private boolean mIsPublicBroadcast;

    @Override
    protected IProfileServiceBinder initBinder() {
        return new BluetoothBroadcastBinder(this);
    }

    @Override
    protected void create() {
        Log.i(TAG, "create()");
    }

    @Override
    protected boolean start() {
        Log.i(TAG, "start()");
        if (sBroadcastService != null) {
            Log.w(TAG, "Broadcastervice is already running");
            return true;
        }
        if (mHandler != null)
            mHandler = null;
        mBroadcastNativeInterface = Objects.requireNonNull(mBroadcastNativeInterface.getInstance(),
            "BroadcastNativeInterface cannot be null when BroadcastService starts");
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            Objects.requireNonNull(mAudioManager,
            "AudioManager cannot be null when A2dpService starts");
        HandlerThread thread = new HandlerThread("BroadcastHandler");
        mPbsAudioQuality = SystemProperties.getInt("persist.vendor.btstack.pbsaudioquality", 0);
        if (is_pbs_with_sq_audio_enabled()) {
            mBroadcastConfigSettings = 2;
        } else if (is_pbs_with_hq_audio_enabled()) {
            mBroadcastConfigSettings = 4;
        } else {
            mBroadcastConfigSettings = SystemProperties.getInt("persist.vendor.btstack.bap_ba_setting", 4);
        }
        mBroadcastCodecConfig = new BroadcastCodecConfig();
        String PartialSimulcast = SystemProperties.get("persist.vendor.btstack.partial_simulcast");
        if (!PartialSimulcast.isEmpty() && "true".equals(PartialSimulcast)) {
            mPartialSimulcast = true;
            mNumSubGrps = 2;
            mNumBises = 4;
            //mHapCodecConfig = new BroadcastCodecConfig(mPartialSimulcast);
        }
        String mNewCodecId = SystemProperties.get("persist.vendor.btstack.new_lc3_id");
        if (mNewCodecId.isEmpty() || "true".equals(mNewCodecId) ||
            "6".equals(mNewCodecId)) {
            new_codec_id = true;
        }
        /* Property to set seconday advertising phy to 1M or 2M. 2M is selected by default
         * if propety is not set
         */
        mSecPhy = SystemProperties.getInt("persist.vendor.btstack.secphy", 2);
        mTxPowerLevel = SystemProperties.getInt("persist.vendor.service.bt.txpower", 9);
        mPD = SystemProperties.getInt("persist.vendor.service.bt.presentation_delay", 40);
        mPreferPaInt = SystemProperties.getInt("persist.vendor.btstack.pa_interval", 0);
        mNewVersion = SystemProperties.getBoolean("persist.vendor.service.bt.new_ba_version", true);
        mBAIdleTimeout = SystemProperties.getBoolean("persist.vendor.service.bt.ba_idle_timeout", false);
        if (mBAIdleTimeout) {
            int idle_timeout = SystemProperties.getInt("persist.vendor.service.bt.ba_idle_timeout_value", 15);
            BROADCAST_INACTIVITY_TIMEOUT_MS = idle_timeout * mMinutesInMS;
        }
        int offload_mode = 1; //offload
        mBroadcastNativeInterface.init(1, mCodecConfig,offload_mode);
        thread.start();
        mContext = this;
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        mContext.registerReceiver(mBroadcastReceiver, filter, Context.RECEIVER_EXPORTED);
        IntentFilter metadataFilter = new IntentFilter();
        metadataFilter.addAction(ACTION_UPDATE_BROADCAST_METADATA);
        mContext.registerReceiver(mBroadcastMetadataReceiver, metadataFilter, Context.RECEIVER_EXPORTED);
        Looper looper = thread.getLooper();
        mHandler = new BroadcastMessageHandler(looper);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBroadcastBase = new BroadcastBase();
        mBisInfo = new ArrayList<>();
        mCcidNum = 1; // Signle Ccid by default
        mStreamingAudioContext = 0x0004; // Media Audio context
        //mBroadcastAdvertiser = new BroadcastAdvertiser();
        setBroadcastService(this);
        LeBroadcastServIntf.init(sBroadcastService);
        mBroadcastDevice = mAdapter.getRemoteDevice(mBroadcastAddress);
        mTrackMetadata = new TrackMetadata(null);

        mActiveDeviceManager = ActiveDeviceManagerService.get(this);
        DeviceProfileMap dpm = DeviceProfileMap.getDeviceProfileMapInstance();
        dpm.profileConnectionUpdate(mBroadcastDevice, ApmConst.AudioFeatures.BROADCAST_AUDIO, ApmConst.AudioProfiles.BROADCAST_LE, true);
        mBANotification = SystemProperties.getBoolean("persist.vendor.service.bt.ba_notification", false);
        if (mBANotification) {
            mNotificationManager = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel mChannel = new NotificationChannel(BROADCAST_NOTIFICATION_ID,
                                           this.getString(R.string.broadcast_notification_name),
                                           NotificationManager.IMPORTANCE_DEFAULT);
            mChannel.setDescription(this.getString(R.string.bluetooth_broadcast_status));
            mChannel.enableLights(true);
            mChannel.enableVibration(true);
            mChannel.setSound(Uri.EMPTY, Notification.AUDIO_ATTRIBUTES_DEFAULT);
            mChannel.setLightColor(Color.GREEN);
            mNotificationManager.createNotificationChannel(mChannel);
        }
        mBroadcastCallbacks = new RemoteCallbackList<IBluetoothLeBroadcastCallback>();
        mAdvertisingSets.clear();
        mBroadcastIdMap.clear();
        mBroadcastMetadataMap.clear();
        return true;
    }
    private boolean is_pbs_with_sq_audio_enabled() {
        boolean status = false;
        if (mPbsAudioQuality == PBS_STD_QUALITY) {
            status = true;
        }
        return status;
    }
    private boolean is_pbs_with_hq_audio_enabled() {
        boolean status = false;
        if (mPbsAudioQuality == PBS_HIGH_QUALITY) {
            status = true;
        }
        return status;
    }
    public boolean is_pbs_enabled() {
        boolean status = false;
        if (is_pbs_with_sq_audio_enabled() || is_pbs_with_hq_audio_enabled()) {
            status = true;
        }
        return status;
    }
    private void initialize_advertiser() {
        Log.d(TAG,"initalize_advertiser");
        mBroadcastAdvertiser = new BroadcastAdvertiser();
        GetEncryptionKeyFromNative();
    }
    private void startAdvTest() {
        //Log.d(TAG,"startAdvTest!!!");
        boolean ba_test = SystemProperties.getBoolean("persist.vendor.btstack.batest",false);
        if (ba_test) {
            Log.d(TAG,"startAdvTest!!!");
            EnableBroadcast(null);
        }
    }
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
               int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,BluetoothAdapter.ERROR);
               Log.d(TAG,"action: " + action + " state: " + state);
               if (state == BluetoothAdapter.STATE_ON) {
                   initialize_advertiser();
                   startAdvTest();
               } else if (state == BluetoothAdapter.STATE_TURNING_OFF) {
                   if (sBroadcastService != null)
                       cleanup_broadcast();
               }
            }
        }
    };

    private BroadcastReceiver mBroadcastMetadataReceiver = new BroadcastReceiver() {
        /*am broadcast -a qti.intent.bluetooth.action.UPDATE_BROADCAST_METADATA
            --es qti.bluetooth.extra.METADATA_AUDIO_CONTEXT "UNSPECIFIED/CONVERSATIONAL/MEDIA/GAME""
            --es qti.bluetooth.extra.METADATA_CCID "0/1/2"*/

         Map <String, Integer> audio_context_map = Map.of(
             "UNSPECIFIED", 1,
             "CONVERSATIONAL", 2,
             "MEDIA", 4,
             "GAME", 8
         );

         List <String> supported_ccid_num = new ArrayList<String>(List.of("0", "1", "2"));

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (!action.equals(ACTION_UPDATE_BROADCAST_METADATA)) {
                return;
            }

            String audioContext = intent.getStringExtra(METADATA_AUDIO_CONTEXT);
            if(audioContext != null && audio_context_map.containsKey(audioContext)) {
                mStreamingAudioContext = audio_context_map.get(audioContext);
            }

            String ccid = intent.getStringExtra(METADATA_CCID);
            if (supported_ccid_num.contains(ccid)) {
                mCcidNum = supported_ccid_num.indexOf(ccid);
            }
            Log.i(TAG,"Metadata update request: mStreamingAudioContext: " + mStreamingAudioContext
                        + " mCcidNum: " + mCcidNum);

            if (mBroadcastState == BluetoothBroadcast.STATE_ENABLED ||
                    mBroadcastState == BluetoothBroadcast.STATE_STREAMING) {
                mBroadcastBase.populateBase();
                mBroadcastAdvertiser.updatePAwithBase();
            } else {
                Log.w(TAG, "Broadcast is disabled while update metadata");
            }
        }
    };

    @Override
    protected boolean stop() {
        Log.i(TAG, "stop()");
        if (sBroadcastService == null) {
            Log.w(TAG, "stop() called before start()");
            return true;
        }
        if (mBroadcastCallbacks != null) {
            try {
                mBroadcastCallbacks.kill();
            } catch (NoSuchElementException e) {
                Log.e(TAG, "Exception: unlinkToDeath");
            }
            mBroadcastCallbacks = null;
        }
        mHandler.removeMessages(MSG_BROADCAST_INACTIVE_TIMEOUT);
        notifyBroadcastEnabled(false);
        if (mIsAdvertising) {
           mBroadcastAdvertiser.stopBroadcastAdvertising();
           notifyOnBroadcastStopped(mAdvertisingSet.getAdvertiserId(),
                   BluetoothStatusCodes.REASON_LOCAL_APP_REQUEST);
        }
        mContext.unregisterReceiver(mBroadcastReceiver);
        mContext.unregisterReceiver(mBroadcastMetadataReceiver);
        mBroadcastNativeInterface = null;
        mAudioManager = null;
        mIsAdvertising = false;
        Looper looper = mHandler.getLooper();
        if (looper != null) {
            looper.quit();
        }
        setBroadcastService(null);
        if (mNotificationManager != null )
            mNotificationManager.deleteNotificationChannel(BROADCAST_NOTIFICATION_ID);
        mBroadcastIdMap.clear();
        mBroadcastMetadataMap.clear();
        mAdvertisingSets.clear();
        return true;
    }

    @Override
    protected void cleanup() {
        Log.i(TAG, "cleanup()");
    }
    public static synchronized BroadcastService getBroadcastService() {
        if (sBroadcastService == null) {
            Log.w(TAG, "getBroadcastService(): service is null");
            return null;
        }
        if (!sBroadcastService.isAvailable()) {
            Log.w(TAG, "getBroadcastService(): service is not available");
            return null;
        }
        return sBroadcastService;
    }

    /** Handles Broadcast messages. */
    private final class BroadcastMessageHandler extends Handler {
        private BroadcastMessageHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            Log.v(TAG, "BroadcastMessageHandler: received message=" + msg.what);
            int prev_state;
            switch (msg.what) {
                case MSG_ENABLE_BROADCAST:
                    //int prev_state;
                    synchronized (mBroadcastLock) {
                        if (VDBG) {
                            Log.i(TAG, "Setting broadcast state to ENABLING");
                        }
                        prev_state = mBroadcastState;
                        mBroadcastState = BluetoothBroadcast.STATE_ENABLING;
                    }
                    broadcastState(mBroadcastState, prev_state);
                    mBroadcastNativeInterface.enableBroadcast(mCodecConfig);
                  break;
                case MSG_DISABLE_BROADCAST:
                case MSG_BROADCAST_INACTIVE_TIMEOUT:
                    //int prev_state;
                    goingDown = true;
                    if (!mIsAdvertising) {
                        Log.e(TAG, "Broadcast is not advertising");
                        break;
                    }
                    synchronized(mBroadcastLock) {
                        if (VDBG) {
                            Log.i(TAG,"Disabling broadcast, setting state to DISABLING");
                        }
                        prev_state = mBroadcastState;
                        mBroadcastState = BluetoothBroadcast.STATE_DISABLING;
                    }
                    broadcastState(mBroadcastState, prev_state);
                    mBroadcastNativeInterface.disableBroadcast(mAdvertisingSet.getAdvertiserId());
                    //mBroadcastAdvertiser.stopBroadcastAdvertising();
                  break;
                case MSG_SET_ENCRYPTION_KEY:
                    //int length = msg.arg1;
                    mBroadcastNativeInterface.SetEncryptionKey(mEncryptionEnabled, mEncryptionLength);
                    if (mEncryptionLength == 0) {
                        for(int i = 0; i < mDefaultEncryptionLength; i++) {
                            BigBroadcastCode[i] = 0x00;
                        }
                        broadcastEncryptionkeySet();
                    }
                  break;
                case MSG_GET_ENCRYPTION_KEY: {
                    mEncryptionString = mBroadcastNativeInterface.GetEncryptionKey();
                    if (mEncryptionString == null) {
                        Log.e(TAG,"MSG_GET_ENCRYPTION_KEY: mEncryptionString null");
                        for (int i = 0; i < mDefaultEncryptionLength; i++) {
                             BigBroadcastCode[i] = 0x00;
                        }
                        break;
                    }
                    mEncKey= mEncryptionString.getBytes();
                    Log.i(TAG, "mEncryptionString: " + mEncryptionString);
                    System.arraycopy(mEncKey, 0, BigBroadcastCode, 0, mEncKey.length);
                    if (mEncKey.length < mDefaultEncryptionLength) {
                        for (int i = mEncKey.length; i < mDefaultEncryptionLength; i++) {
                            BigBroadcastCode[i] = 0x00;
                        }
                    }
                    for (int i = 0;i < mDefaultEncryptionLength/2; i++) {
                        byte temp = BigBroadcastCode[i];
                        BigBroadcastCode[i] = BigBroadcastCode[(mDefaultEncryptionLength -1) - i];
                        BigBroadcastCode[(mDefaultEncryptionLength -1) - i] = temp;
                    }
                    for (int i = 0; i < 16; i++) {
                        Log.i(TAG,"BigBroadcastCode["+ i + "] = " + BigBroadcastCode[i]);
                    }
                    //TODO: Stub to test encryption key creation, to be removed
                    //Log.i(TAG,"calling setencryptionkey");
                    //mBroadcastNativeInterface.SetEncryptionKey(4);
                    broadcastEncryptionkeySet();
                  }
                  break;
                case MSG_UPDATE_BROADCAST_ADV_SET:
                  break;
                case MSG_SET_BROADCAST_ACTIVE:
                  // Call native layer to set broadcast active
                    //mBroadcastNativeInterface.setActiveDevice(true, mAdvertisingSet.getAdvertiserId());
                    //setActiveDevice(mBroadcastDevice);
                    notifyBroadcastEnabled(true);
                  break;
                case MSG_RESET_ENCRYPTION_FLAG_TIMEOUT:
                    Log.i(TAG,"Setting mEncKeyRefreshed to false");
                    mEncKeyRefreshed = false;
                  break;
                case MSG_FROM_NATIVE_BROADCAST_STATE:
                    int nativeBroadcastState = msg.arg1;
                    synchronized(mBroadcastLock) {
                          prev_state = mBroadcastState;
                          if (nativeBroadcastState == BluetoothBroadcast.STATE_DISABLED &&
                                  mIsAdvertising) {
                              Log.i(TAG, "Keep Broadcast on DISABLNG state if still in advertising");
                              mBroadcastState = BluetoothBroadcast.STATE_DISABLING;
                          } else {
                              mBroadcastState = nativeBroadcastState;
                          }
                          if (VDBG) {
                              Log.i(TAG,"Prevous state: " + prev_state +
                                      " native Broadcast state: " + nativeBroadcastState +
                                      " New broadcast state: " + mBroadcastState);
                          }
                    }
                    if (nativeBroadcastState == BluetoothBroadcast.STATE_DISABLED) {
                        if (goingDown) {
                            notifyBroadcastEnabled(false);
                        }
                        mBIGHandle = -1;
                        mBroadcastAdvertiser.stopBroadcastAdvertising();
                        break;
                    } else if ((prev_state == BluetoothBroadcast.STATE_DISABLED ||
                            prev_state == BluetoothBroadcast.STATE_ENABLING) &&
                            mBroadcastState == BluetoothBroadcast.STATE_ENABLED) {
                        Log.d(TAG, "Broadcast started");
                        notifyBroadcastStarted(mAdvertisingSet.getAdvertiserId(),
                                BluetoothStatusCodes.REASON_LOCAL_APP_REQUEST);
                        BluetoothLeBroadcastMetadata md = getBroadcastMetadata(
                                mAdvertisingSet.getAdvertiserId());
                        if (md != null) {
                            notifyBroadcastMetadataChanged(mAdvertisingSet.getAdvertiserId(), md);
                            mBroadcastMetadataMap.put(mAdvertisingSet.getAdvertiserId(), md);
                        } else {
                            Log.e(TAG, "Metadata is null");
                        }
                    }
                    if (prev_state != mBroadcastState)
                        broadcastState(mBroadcastState, prev_state);
                    if (mBAIdleTimeout)
                        start_broadcast_idle_timeout();
                  break;
                case MSG_ADV_DATA_SET:
                    synchronized (mBroadcastLock) {
                        if (VDBG) {
                            Log.i(TAG, "Setting broadcast state to ENABLING");
                        }
                        prev_state = mBroadcastState;
                        mBroadcastState = BluetoothBroadcast.STATE_ENABLED;
                    }
                    broadcastState(mBroadcastState, prev_state);
                  break;
                case MSG_SET_AUDIO_PATH:
                  //mBroadcastNativeInterface.SetupAudioPath(true,mAdvertisingSet.getAdvertiserId(),mBIGHandle,mNumBises,bis_handles);
                  break;
                case MSG_FROM_NATIVE_CODEC_STATE:
                    mCodecStatus = (BluetoothCodecStatus)msg.obj;
                    if (IsCodecConfigChanged(mCodecStatus.getCodecConfig())) {
                        mBroadcastCodecConfig.updateBroadcastCodecConfig(mCodecStatus.getCodecConfig());
                        mBroadcastBase.populateBase();
                        mBroadcastAdvertiser.updatePAwithBase();
                    }
                    broadcastCodecConfig(mCodecStatus);
                    mMediaAudio = MediaAudio.get();
                    mMediaAudio.onCodecConfigChange(mBroadcastDevice, mCodecStatus, ApmConst.AudioProfiles.BROADCAST_LE);
                  break;
                case MSG_FROM_NATIVE_ENCRYPTION_KEY: {
                    mEncryptionString = (String)msg.obj;
                    Log.d(TAG,"mEncryptionString: " + mEncryptionString);
                    mEncKey= mEncryptionString.getBytes();
                    System.arraycopy(mEncKey, 0, BigBroadcastCode, 0, mEncKey.length);
                    if (mEncKey.length < mDefaultEncryptionLength) {
                        for (int i = mEncKey.length; i < mDefaultEncryptionLength; i++) {
                            BigBroadcastCode[i] = 0x00;
                        }
                    }
                    for (int i = 0; i < mEncKey.length; i++) {
                        Log.d(TAG,"mEnc[" + i +"] = " + mEncKey[i]);
                    }
                    for (int i = 0;i < mDefaultEncryptionLength/2; i++) {
                        byte temp = BigBroadcastCode[i];
                        BigBroadcastCode[i] = BigBroadcastCode[(mDefaultEncryptionLength - 1) - i];
                        BigBroadcastCode[(mDefaultEncryptionLength - 1) - i] = temp;
                    }
                    //Broadcast encyption key set
                    broadcastEncryptionkeySet();
                  }
                  break;
                case MSG_FROM_NATIVE_SETUP_BIG:
                    int setup = msg.arg1;
                    boolean set = (setup == 1);
                    if (set) {
                        Log.d(TAG, "BIG created: " + mBIGHandle + "with no of bises: " + mNumBises);
                        mNumBises = mNumBises * mNumSubGrps;
                        mBroadcastBase.populateBase();
                        mBroadcastAdvertiser.updatePAwithBase();
                    } else {
                        Log.d(TAG, "BIG terminated");
                        mBIGHandle = -1;
                        //Clean up mBisInfo List
                        mBisInfo.clear();
                    }
                    break;
                case MSG_FROM_NATIVE_BROADCAST_AUDIO_STATE:
                    int prevState = mBroadcastAudioState;
                    mBroadcastAudioState = msg.arg1;
                    if (prevState != mBroadcastAudioState)
                        broadcastAudioState(mBroadcastAudioState, prevState);
                    break;
                case MSG_FROM_NATIVE_BROADCAST_ID:
                   if (mBroadcastAdvertiser != null) {
                       Log.d(TAG, "Broadcast state: " + mBroadcastState);
                       if (mBroadcastState == BluetoothBroadcast.STATE_ENABLING) {
                           mBroadcastAdvertiser.startBroadcastAdvertising();
                       }
                   } else {
                       Log.e(TAG,"Did not receive adatper state change intent, turning off Broadcast");
                       prev_state = mBroadcastState;
                       mBroadcastState = BluetoothBroadcast.STATE_DISABLED;
                       broadcastState(mBroadcastState, prev_state);
                       notifyBroadcastStartFailed(BluetoothStatusCodes.ERROR_LOCAL_NOT_ENOUGH_RESOURCES);
                   }
                   break;
                case MSG_UPDATE_BROADCAST_STATE:
                    prev_state = msg.arg1;
                    mBroadcastState = BluetoothBroadcast.STATE_DISABLED;
                    Log.d(TAG,"MSG_UPDATE_BROADCAST_STATE");
                    broadcastState(mBroadcastState, prev_state);
                    notifyOnBroadcastStopped(mAdvertisingSet.getAdvertiserId(),
                            BluetoothStatusCodes.REASON_LOCAL_APP_REQUEST);
                    mBroadcastMetadataMap.remove(mAdvertisingSet.getAdvertiserId());
                    mBroadcastIdMap.remove(mAdvertisingSet.getAdvertiserId());;
                    break;
                case MSG_SET_BROADCAST_CODE:
                    if (mBroadcastNativeInterface != null) {
                        mBroadcastNativeInterface.SetBroadcastCode((byte[]) msg.obj);
                    }
                    break;
                default:
                  Log.e(TAG,"unknown message msg.what = " + msg.what);
                  break;
            }
            Log.d(TAG,"Exit handleMessage");
        }
    }

    private void start_broadcast_idle_timeout() {
        synchronized(mBroadcastLock) {
            if (mBroadcastState == BluetoothBroadcast.STATE_ENABLED) {
                Log.d(TAG,"start_broadcast_idle_timeout: start idle timeout");
                Message msg = mHandler.obtainMessage(MSG_BROADCAST_INACTIVE_TIMEOUT);
                mHandler.sendMessageDelayed(msg,BROADCAST_INACTIVITY_TIMEOUT_MS);
            } else if (mBroadcastState == BluetoothBroadcast.STATE_STREAMING) {
                if (mAudioManager.isMusicActive()) {
                    Log.d(TAG,"start_broadcast_idle_timeout: remove idle timeout");
                    mHandler.removeMessages(MSG_BROADCAST_INACTIVE_TIMEOUT);
                }
            } else if (mBroadcastState == BluetoothBroadcast.STATE_DISABLING ||
                mBroadcastState == BluetoothBroadcast.STATE_DISABLED) {
                mHandler.removeMessages(MSG_BROADCAST_INACTIVE_TIMEOUT);
            }
        }
    }

    private void updateBroadcastStateToHfp(int state) {
        if (DBG) {
            Log.d(TAG,"updateBroadcastStateToHfp");
        }
        HeadsetService hfpService = HeadsetService.getHeadsetService();
        if (hfpService != null) {
            hfpService.updateBroadcastState(state);
        }
    }

    private void updateBroadcastStateToAssist(int state) {
        if (DBG) {
            Log.d(TAG,"updateBroadcastStateToAssist");
        }
        BCService bcService = BCService.getBCService();
        if (bcService != null) {
            bcService.onBroadcastStateChanged(state);
        }
    }

    public void onPASyncedToBroadcastReceiver (BluetoothDevice dev) {
        if (mHandler.hasMessages(MSG_SET_BROADCAST_ACTIVE)) {
            mHandler.removeMessages(MSG_SET_BROADCAST_ACTIVE);
            notifyBroadcastEnabled(true);
        }
    }

    private void broadcastState(int state, int prev_state) {
        if (DBG) {
            Log.d(TAG, "Broadcasting broadcastState: " + state);
        }
        Intent intent = new Intent(BluetoothBroadcast.ACTION_BROADCAST_STATE_CHANGED);
        intent.putExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, prev_state);
        intent.putExtra(BluetoothProfile.EXTRA_STATE, state);
        sendBroadcast(intent, BLUETOOTH_CONNECT, Utils.getTempAllowlistBroadcastOptions());
        updateBroadcastStateToHfp(state);
        updateBroadcastStateToAssist(state);
    }
    private void broadcastCodecConfig(BluetoothCodecStatus codecStatus) {
        if (DBG) {
            Log.d(TAG, "Broacasting broadcastCodecConfig" + codecStatus);
        }
        Intent intent = new Intent(BluetoothA2dp.ACTION_CODEC_CONFIG_CHANGED);
        intent.putExtra(BluetoothCodecStatus.EXTRA_CODEC_STATUS, codecStatus);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, mBroadcastDevice);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT
                        | Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND);
        //sendBroadcast(intent, BLUETOOTH_CONNECT);
    }

    private void broadcastEncryptionkeySet() {
        if (DBG) {
            Log.d(TAG, "broadcastEncryptionkeySet");
        }
        Intent intent = new Intent(BluetoothBroadcast.ACTION_BROADCAST_ENCRYPTION_KEY_GENERATED);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT
                        | Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND);
        sendBroadcast(intent, BLUETOOTH_CONNECT, Utils.getTempAllowlistBroadcastOptions());
    }

    private void broadcastAudioState(int newState, int prevState) {
        Log.d(TAG, "broadcastAudioState: State:" + audioStateToString(prevState)
                + "->" + audioStateToString(newState));
        Intent intent = new Intent(BluetoothBroadcast.ACTION_BROADCAST_AUDIO_STATE_CHANGED);
        intent.putExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, prevState);
        intent.putExtra(BluetoothProfile.EXTRA_STATE, newState);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
        sendBroadcast(intent, BLUETOOTH_CONNECT, Utils.getTempAllowlistBroadcastOptions());
        if (newState == BluetoothBroadcast.STATE_PLAYING) {
            notifyPlaybackStarted(mAdvertisingSet.getAdvertiserId(),
                    BluetoothStatusCodes.REASON_LOCAL_STACK_REQUEST);
        } else if (newState == BluetoothBroadcast.STATE_NOT_PLAYING) {
            notifyPlaybackStopped(mAdvertisingSet.getAdvertiserId(),
                    BluetoothStatusCodes.REASON_LOCAL_STACK_REQUEST);
        }
    }

    private static String audioStateToString(int state) {
        switch (state) {
            case BluetoothBroadcast.STATE_PLAYING:
                return "PLAYING";
            case BluetoothBroadcast.STATE_NOT_PLAYING:
                return "NOT_PLAYING";
            default:
                break;
        }
        return Integer.toString(state);
    }
    private boolean IsCodecConfigChanged(BluetoothCodecConfig config) {
        return (mCodecConfig.getSampleRate() != config.getSampleRate() ||
                mCodecConfig.getChannelMode() != config.getChannelMode() ||
                mCodecConfig.getCodecSpecific1() != config.getCodecSpecific1() ||
                mCodecConfig.getCodecSpecific2() != config.getCodecSpecific2());
    }
    private boolean isCodecValid(BluetoothCodecConfig mCodecConfig) {
        if (mCodecConfig.getCodecType() != BluetoothCodecConfig.SOURCE_CODEC_TYPE_LC3) {
            return false;
        }
        return true;
    }

    private boolean isCodecConfigValid(String config_id) {
        if (broadcast_supported_config.contains(config_id)) {
            Log.d(TAG,"isCodecConfigValid: config supported");
            return true;
        }
        Log.d(TAG,"isCodecConfigValid: config not supported");
        return false;
    }

    private boolean isEncrytionLengthValid(int enc_length) {
        if (enc_length == 4 || enc_length == 16) {
            return true;
        }
        return false;
    }

    private BluetoothCodecConfig buildCodecConfig(String config_id, int channel) {
        //BluetoothCodecConfig cc;
        int index = broadcast_supported_config.indexOf(config_id);
        int sr;
        long codecspecific1, codecspecific2;
        String isMono = SystemProperties.get("persist.vendor.btstack.enable.broadcast_mono");
        Log.d(TAG,"buildCodecConfig:" + config_id + " index: " + index);
        switch(index) {
            case 0: //16_2
                sr = BluetoothCodecConfig.SAMPLE_RATE_16000;
                //ch_mode = BluetoothCodecConfig.CHANNEL_MODE_STEREO;
                codecspecific1 = 1001;//32kbps
                codecspecific2 = 1;
                break;
            case 1: //24_2
                sr = BluetoothCodecConfig.SAMPLE_RATE_24000;
                //ch_mode = BluetoothCodecConfig.CHANNEL_MODE_STEREO;
                codecspecific1 = 1002;//48kbps
                codecspecific2 = 1;
                break;
            case 2: //48_1
                sr = BluetoothCodecConfig.SAMPLE_RATE_48000;
                //ch_mode = BluetoothCodecConfig.CHANNEL_MODE_STEREO;
                codecspecific1 = 1004;//80kbps
                codecspecific2 = 0;
                break;
            case 3: //48_2
                sr = BluetoothCodecConfig.SAMPLE_RATE_48000;
                //ch_mode = BluetoothCodecConfig.CHANNEL_MODE_STEREO;
                codecspecific1 = 1004;//80kbps
                codecspecific2 = 1;
                break;
            case 4: //48_3
                sr = BluetoothCodecConfig.SAMPLE_RATE_48000;
                //ch_mode = BluetoothCodecConfig.CHANNEL_MODE_STEREO;
                codecspecific1 = 1006;//96kbps
                codecspecific2 = 0;
                break;
            case 5: //48_4
                sr = BluetoothCodecConfig.SAMPLE_RATE_48000;
                //ch_mode = BluetoothCodecConfig.CHANNEL_MODE_STEREO;
                codecspecific1 = 1006;//96kbps
                codecspecific2 = 1;
                break;
            case 6: //48_5
                sr = BluetoothCodecConfig.SAMPLE_RATE_48000;
                //ch_mode = BluetoothCodecConfig.CHANNEL_MODE_STEREO;
                codecspecific1 = 1007;//124kbps
                codecspecific2 = 0;
                break;
            case 7: //48_6
                sr = BluetoothCodecConfig.SAMPLE_RATE_48000;
                //ch_mode = BluetoothCodecConfig.CHANNEL_MODE_STEREO;
                codecspecific1 = 1007;//124kbps
                codecspecific2 = 1;
                break;

            default:
                sr = BluetoothCodecConfig.SAMPLE_RATE_48000;
                //ch_mode = BluetoothCodecConfig.CHANNEL_MODE_STEREO;
                codecspecific1 = 1007;//80kbps
                codecspecific2 = 1;
                break;
            }
        //if (isMono.isEmpty() || isMono.equals("mono")) {
        //    ch_mode = BluetoothCodecConfig.CHANNEL_MODE_MONO;
        //}
        BluetoothCodecConfig cc = new BluetoothCodecConfig(BluetoothCodecConfig.SOURCE_CODEC_TYPE_LC3,
                                      BluetoothCodecConfig.CODEC_PRIORITY_DEFAULT,
                                      sr, BluetoothCodecConfig.BITS_PER_SAMPLE_24,
                                      channel, codecspecific1, codecspecific2, 0, 0);
        return cc;
    }
    private static synchronized void setBroadcastService(BroadcastService instance) {
        if (DBG) {
            Log.d(TAG, "setBroadcastService(): set to: " + instance);
        }
        sBroadcastService = instance;
    }

    private void cleanup_broadcast() {
        if (DBG) Log.d (TAG, "cleanup_broadcast");
        mHandler.removeMessages(MSG_BROADCAST_INACTIVE_TIMEOUT);
        synchronized (mBroadcastLock) {
            if (mIsAdvertising) {
                if (mBroadcastNativeInterface != null)
                    mBroadcastNativeInterface.disableBroadcast(mAdvertisingSet.getAdvertiserId());
                mBroadcastAdvertiser.stopBroadcastAdvertising();
                int prev_state = mBroadcastState;
                mBroadcastState = BluetoothBroadcast.STATE_DISABLED;
                broadcastState(mBroadcastState, prev_state);
            }
        }
    }
    public boolean EnableBroadcast(String packageName) {
        if (DBG) Log.d (TAG, "EnableBroadcast");
        synchronized (mBroadcastLock) {
            if (mBroadcastState != BluetoothBroadcast.STATE_DISABLED) {
                return false;
            }
            mBroadcastState = BluetoothBroadcast.STATE_ENABLING;
        }
        Message msg = mHandler.obtainMessage(MSG_ENABLE_BROADCAST);
        mHandler.sendMessage(msg);
        return true;
    }
    public boolean DisableBroadcast(String packageName) {
        if (DBG) Log.d (TAG, "DisableBroadcast: state " + mBroadcastState);
        synchronized (mBroadcastLock) {
            if (mBroadcastState == BluetoothBroadcast.STATE_DISABLING ||
                mBroadcastState == BluetoothBroadcast.STATE_DISABLED) {
                return true;
            } else if (mBroadcastState != BluetoothBroadcast.STATE_ENABLED &&
                mBroadcastState != BluetoothBroadcast.STATE_STREAMING) {
                Log.d(TAG,"Broadcast is not enabled yet");
                return false;
            }
            mBroadcastState = BluetoothBroadcast.STATE_DISABLING;
        }
        mHandler.removeMessages(MSG_BROADCAST_INACTIVE_TIMEOUT);
        Message msg = mHandler.obtainMessage(MSG_DISABLE_BROADCAST);
        mHandler.sendMessage(msg);
        return true;
    }
    public boolean SetEncryption(boolean enable, int enc_len,
                           boolean use_existing, String packageName) {
        if (DBG) Log.d (TAG,"SetEncryption");

        mEncryptionEnabled = enable;
        if (enable) {
            if (!isEncrytionLengthValid(enc_len)) {
                if (DBG) Log.d (TAG,"SetEncryption: invalid encrytion length requested");
                return false;
            }
        } else {
            Log.d(TAG,"Selected unencrypted");
            enc_len = 0;
        }
        if (!use_existing) {
            Log.d (TAG,"Generate new ecrytpion key of lenght = " + enc_len);
            mEncryptionLength = enc_len;
            if (mBroadcastState == BluetoothBroadcast.STATE_ENABLED ||
                mBroadcastState == BluetoothBroadcast.STATE_STREAMING) {
                mEncKeyRefreshed = true;
                Message msg = mHandler.obtainMessage(MSG_RESET_ENCRYPTION_FLAG_TIMEOUT);
                mHandler.sendMessageDelayed(msg, 1000);
            }
            Message msg = mHandler.obtainMessage(MSG_SET_ENCRYPTION_KEY);
            mHandler.sendMessage(msg);
        }
        return true;
    }

    public byte[] GetEncryptionKey(String packageName) {
        if (DBG) Log.d (TAG,"GetBroadcastEncryptionKey: package name = " + packageName);

        return BigBroadcastCode;
    }

    public int GetBroadcastStatus(String packageName) {
        if (DBG) Log.d (TAG,"GetBroadcastStatus: state = " + mBroadcastState + " package name = " + packageName);
        return mBroadcastState;
    }

    public boolean isBroadcastActive() {
        if (mBroadcastDeviceIsActive == false) {
            Log.d (TAG,"isBroadcastActive: Broadcast is turned to off");
            return false;
        }
        if (DBG) Log.d (TAG,"isBroadcastActive: " + mBroadcastState);
        return ((mBroadcastState == BluetoothBroadcast.STATE_ENABLED) ||
               (mBroadcastState == BluetoothBroadcast.STATE_STREAMING));
    }

    public BluetoothDevice getBroadcastDevice() {
        if (DBG) Log.d (TAG,"getBroadcastDevice");
        return mBroadcastDevice;
    }

    public String getBroadcastAddress() {
        if (DBG) Log.d (TAG,"getBroadcastAddress");
        return mBroadcastAddress;
    }

    public byte[] getBroadcastId() {
        Log.d(TAG,"getBroadcastId: " + mBroadcastID);
        return mBroadcastID;
    }

    public String getProgramInfo() {
        String programInfo = null;
        if (mAudioContentMetadata != null) {
            return mAudioContentMetadata.getProgramInfo();
        }
        return programInfo;
    }

    public String getBroadcastName() {
        Log.d(TAG, "getBroadcastName: " + mBroadcastName);
        return mBroadcastName;
    }

    public byte[] getPublicBroadcastServiceData() {
        return mBroadcastAdvertiser.getPublicBroadcastServiceData();
    }

    public boolean isBroadcastStreamingEncrypted() {
        return mEncryptionEnabled;
    }

    public boolean isBroadcastStreaming() {
        return (mBroadcastState == BluetoothBroadcast.STATE_STREAMING);
    }

    public String BroadcastGetAdvAddress() {
        if (DBG) Log.d (TAG,"BroadcastGetAdvAddress: " + mAdvAddress);
        return mAdvAddress;
    }

    public int getNumSubGroups() {
        if (DBG) Log.d (TAG,"getNumSubGroups: " + mNumSubGrps);
        return mNumSubGrps;
    }

    public int BroadcastGetAdvAddrType() {
        return mAdvAddressType;
    }

    public int BroadcatGetAdvHandle() {
        //check if advertising
        return mAdvertisingSet.getAdvertiserId();
    }

    public int BroadcastGetAdvInterval() {
        return mPaInt;
    }
    public List<BisInfo> BroadcastGetBisInfo() {
        if (isBroadcastStreaming()) {
            return mBisInfo;
        }
        Log.d(TAG,"BroadcastGetBisInfo: Broadcast is not active");
        return mBisInfo;
    }

    public Map<Integer, MetadataLtv> BroadcastGetMetaInfo() {
        if (isBroadcastStreaming()) {
            return mMetaInfo;
        }
        Log.d(TAG,"BroadcastGetMetaInfo: Broadcast is not active");
        return mMetaInfo;
    }
    public byte[] BroadcastGetMetadata() {
        if (isBroadcastStreaming()) {
            return mBroadcastBase.getMetadataContext();
        }
        Log.d(TAG,"BroadcastGetMetadata: Broadcast is not active");
        return mBroadcastBase.getMetadataContext();
    }
    public int getPresentationDelay() {
        return mBroadcastBase.getPresentationDelay();
    }
    public void setCodecPreference(String config_id, int ch_mode) {
        if (isCodecConfigValid(config_id)) {
            setCodecPreference(buildCodecConfig(config_id, ch_mode));
        }
    }
    public void setCodecPreference(BluetoothCodecConfig newConfig) {
        if (DBG) Log.d (TAG, "setCodecPreference");
        if (newConfig.getCodecType() != BluetoothCodecConfig.SOURCE_CODEC_TYPE_LC3) {
            Log.e(TAG, "setCodecPreference: Invalid codec for broadcast mode: " + newConfig.getCodecType());
            return;
        }
        //mBroadcastCodecConfig.updateCodecConfig(newConfig);
        if (mBroadcastState != BluetoothBroadcast.STATE_DISABLED)
            mBroadcastNativeInterface.setCodecConfigPreference(mAdvertisingSet.getAdvertiserId(),newConfig);
    }

    public void GetEncryptionKeyFromNative() {
        Log.e(TAG,"GetEncryptionKeyFromNative");
        Message msg = mHandler.obtainMessage(MSG_GET_ENCRYPTION_KEY);
        mHandler.sendMessage(msg);
    }
    private void setup_isodatapath(int adv_id, int big_handle,int num_bises, int[] bises) {
    }
    /* LE HAP broadcast hooks */
    public boolean startHAPBroadcast() {
        if (isBroadcastActive()) {
        //TODO: update codec config with HAP HQ mode
        //Terminate BIG if created
        //Notify codec config change to stack
        //Create BIG and update BASE
        } else {
        //TODO: update codec config with HAP HQ mode
        //Start Adv
        //Existing encryption key will be used for HAP as only music streaming is supported
        //Announcement content type will not be covered
        }
        return true;
    }
    public boolean stopHAPBroadcast() {
        //TODO: DisableAudioPath
        //Terminate BIG
        //update state to disabling
        //stop Adv
        //reset codec config to default config
        return true;
    }
    public void removeActiveDevice() {
        if (DBG) Log.d (TAG,"removeActiveDevice");
        //int [] bis_handles = {-1, -1};
        if (mBroadcastDeviceIsActive == false) {
            Log.d (TAG,"removeActiveDevice: mBADeviceIsActive is false, already removed");
            return;
        }
        mBroadcastDeviceIsActive = false;
        synchronized (mBroadcastLock) {
            if (mIsAdvertising &&
               (mBroadcastState == BluetoothBroadcast.STATE_ENABLED ||
                mBroadcastState == BluetoothBroadcast.STATE_STREAMING)) {
                mBroadcastNativeInterface.disableBroadcast(mAdvertisingSet.getAdvertiserId());
                //mBroadcastAdvertiser.stopBroadcastAdvertising();
            }
            if (!mBroadcastNativeInterface.setActiveDevice(false, mAdvertisingSet.getAdvertiserId())) {
                Log.d(TAG,"SetActiveNative failed");
            }
        }
        if (mNotificationManager != null && mNOTIFICATION_ID != -1) {
            mNotificationManager.cancel(mNOTIFICATION_ID);
            mNOTIFICATION_ID = -1;
        }
    }

    public BluetoothCodecStatus getCodecStatus() {
        if (DBG) Log.d (TAG,"getCodecStatus");
        BluetoothCodecConfig[] mBroadcastCodecConfig = {mCodecConfig};
        return (new BluetoothCodecStatus(mCodecConfig, Arrays.asList(mBroadcastCodecConfig),
                                                     Arrays.asList(mBroadcastCodecConfig)));
    }
    public int setActiveDevice(BluetoothDevice device) {
        if (DBG) Log.d (TAG,"setActiveDevice");
        if (device == null) {
            removeActiveDevice();
            return ActiveDeviceManagerService.SHO_SUCCESS;
        }
        if (!Objects.equals(device, mBroadcastDevice)) {
            Log.d(TAG,"setActiveDevice: Not a Broadcast device");
            return ActiveDeviceManagerService.SHO_FAILED;
        }
        if (!mBroadcastNativeInterface.setActiveDevice(true, mAdvertisingSet.getAdvertiserId())) {
            Log.d(TAG,"SetActiveNative failed");
            return ActiveDeviceManagerService.SHO_FAILED;
        }
        mBroadcastDeviceIsActive = true;

        return ActiveDeviceManagerService.SHO_SUCCESS;
    }

    private void notifyBroadcastStarted(Integer instanceId, int reason) {
        if (!mBroadcastIdMap.containsKey(instanceId)) {
            Log.d(TAG, "Unknown broadcast id for instance " + instanceId);
            return;
        }
        Integer broadcastId = mBroadcastIdMap.get(instanceId);
        if (mBroadcastCallbacks != null) {
            synchronized (mCallbackNotifyLock) {
                final int n = mBroadcastCallbacks.beginBroadcast();
                for (int i = 0; i < n; i++) {
                    final IBluetoothLeBroadcastCallback callback =
                            mBroadcastCallbacks.getBroadcastItem(i);
                    try {
                        Log.d(TAG, "Calling onBroadcastStarted: " + i);
                        callback.onBroadcastStarted(reason, broadcastId);
                    } catch (RemoteException e) {
                        Log.e(TAG, "Stack:" + Log.getStackTraceString(e));
                    }
                }
                mBroadcastCallbacks.finishBroadcast();
            }
        }
    }

    private void notifyBroadcastStartFailed(int reason) {
        if (mBroadcastCallbacks != null) {
            synchronized (mCallbackNotifyLock) {
                final int n = mBroadcastCallbacks.beginBroadcast();
                for (int i = 0; i < n; i++) {
                    final IBluetoothLeBroadcastCallback callback =
                            mBroadcastCallbacks.getBroadcastItem(i);
                    try {
                        Log.d(TAG, "Calling onBroadcastStartFailed: " + i);
                        callback.onBroadcastStartFailed(reason);
                    } catch (RemoteException e) {
                        Log.e(TAG, "Stack:" + Log.getStackTraceString(e));
                    }
                }
                mBroadcastCallbacks.finishBroadcast();
            }
        }
    }

    private void notifyOnBroadcastStopped(Integer instanceId, int reason) {
        if (!mBroadcastIdMap.containsKey(instanceId)) {
            Log.d(TAG, "Unknown broadcast id for instance " + instanceId);
            return;
        }
        Integer broadcastId = mBroadcastIdMap.get(instanceId);
        if (mBroadcastCallbacks != null) {
            synchronized (mCallbackNotifyLock) {
                final int n = mBroadcastCallbacks.beginBroadcast();
                for (int i = 0; i < n; i++) {
                    final IBluetoothLeBroadcastCallback callback =
                            mBroadcastCallbacks.getBroadcastItem(i);
                    try {
                        Log.d(TAG, "Calling onBroadcastStopped: " + i);
                        callback.onBroadcastStopped(reason, broadcastId);
                    } catch (RemoteException e) {
                        Log.e(TAG, "Stack:" + Log.getStackTraceString(e));
                    }
                }
                mBroadcastCallbacks.finishBroadcast();
            }
        }
    }

    private void notifyOnBroadcastStopFailed(int reason) {
        if (mBroadcastCallbacks != null) {
            synchronized (mCallbackNotifyLock) {
                final int n = mBroadcastCallbacks.beginBroadcast();
                for (int i = 0; i < n; i++) {
                    final IBluetoothLeBroadcastCallback callback =
                            mBroadcastCallbacks.getBroadcastItem(i);
                    try {
                        Log.d(TAG, "Calling onBroadcastStopFailed: " + i);
                        callback.onBroadcastStopFailed(reason);
                    } catch (RemoteException e) {
                        Log.e(TAG, "Stack:" + Log.getStackTraceString(e));
                    }
                }
                mBroadcastCallbacks.finishBroadcast();
            }
        }
    }

    private void notifyPlaybackStarted(Integer instanceId, int reason) {
        if (!mBroadcastIdMap.containsKey(instanceId)) {
            Log.d(TAG, "Unknown broadcast id for instance " + instanceId);
            return;
        }
        Integer broadcastId = mBroadcastIdMap.get(instanceId);
        if (mBroadcastCallbacks != null) {
            synchronized (mCallbackNotifyLock) {
                final int n = mBroadcastCallbacks.beginBroadcast();
                for (int i = 0; i < n; i++) {
                    final IBluetoothLeBroadcastCallback callback =
                            mBroadcastCallbacks.getBroadcastItem(i);
                    try {
                        Log.d(TAG, "Calling onPlaybackStarted: " + i);
                        callback.onPlaybackStarted(reason, broadcastId);
                    } catch (RemoteException e) {
                        Log.e(TAG, "Stack:" + Log.getStackTraceString(e));
                    }
                }
                mBroadcastCallbacks.finishBroadcast();
            }
        }
    }

    private void notifyPlaybackStopped(Integer instanceId, int reason) {
        if (!mBroadcastIdMap.containsKey(instanceId)) {
            Log.d(TAG, "Unknown broadcast id for instance " + instanceId);
            return;
        }
        Integer broadcastId = mBroadcastIdMap.get(instanceId);
        if (mBroadcastCallbacks != null) {
            synchronized (mCallbackNotifyLock) {
                final int n = mBroadcastCallbacks.beginBroadcast();
                for (int i = 0; i < n; i++) {
                    final IBluetoothLeBroadcastCallback callback =
                            mBroadcastCallbacks.getBroadcastItem(i);
                    try {
                        Log.d(TAG, "Calling onPlaybackStopped: " + i);
                        callback.onPlaybackStopped(reason, broadcastId);
                    } catch (RemoteException e) {
                        Log.e(TAG, "Stack:" + Log.getStackTraceString(e));
                    }
                }
                mBroadcastCallbacks.finishBroadcast();
            }
        }
    }

    private void notifyBroadcastUpdated(Integer instanceId, int reason) {
        if (!mBroadcastIdMap.containsKey(instanceId)) {
            Log.d(TAG, "Unknown broadcast id for instance " + instanceId);
            return;
        }
        Integer broadcastId = mBroadcastIdMap.get(instanceId);
        if (mBroadcastCallbacks != null) {
            synchronized (mCallbackNotifyLock) {
                final int n = mBroadcastCallbacks.beginBroadcast();
                for (int i = 0; i < n; i++) {
                    final IBluetoothLeBroadcastCallback callback =
                            mBroadcastCallbacks.getBroadcastItem(i);
                    try {
                        Log.d(TAG, "Calling onBroadcastUpdated: " + i);
                        callback.onBroadcastUpdated(reason, broadcastId);
                    } catch (RemoteException e) {
                        Log.e(TAG, "Stack:" + Log.getStackTraceString(e));
                    }
                }
                mBroadcastCallbacks.finishBroadcast();
            }
        }
    }

    private void notifyBroadcastUpdateFailed(Integer instanceId, int reason) {
        if (!mBroadcastIdMap.containsKey(instanceId)) {
            Log.d(TAG, "Unknown broadcast id for instance " + instanceId);
            return;
        }
        Integer broadcastId = mBroadcastIdMap.get(instanceId);
        if (mBroadcastCallbacks != null) {
            synchronized (mCallbackNotifyLock) {
                final int n = mBroadcastCallbacks.beginBroadcast();
                for (int i = 0; i < n; i++) {
                    final IBluetoothLeBroadcastCallback callback =
                            mBroadcastCallbacks.getBroadcastItem(i);
                    try {
                        Log.d(TAG, "Calling onBroadcastUpdateFailed: " + i);
                        callback.onBroadcastUpdateFailed(reason, broadcastId);
                    } catch (RemoteException e) {
                        Log.e(TAG, "Stack:" + Log.getStackTraceString(e));
                    }
                }
                mBroadcastCallbacks.finishBroadcast();
            }
        }
    }

    private void notifyBroadcastMetadataChanged(Integer instanceId,
            BluetoothLeBroadcastMetadata metadata) {
        Log.d(TAG, "notifyBroadcastMetadataChanged for instance " + instanceId);
        if (!mBroadcastIdMap.containsKey(instanceId)) {
            Log.d(TAG, "Unknown broadcast id for instance " + instanceId);
            return;
        }
        Integer broadcastId = mBroadcastIdMap.get(instanceId);
        if (mBroadcastCallbacks != null) {
            synchronized (mCallbackNotifyLock) {
                final int n = mBroadcastCallbacks.beginBroadcast();
                for (int i = 0; i < n; i++) {
                    final IBluetoothLeBroadcastCallback callback =
                            mBroadcastCallbacks.getBroadcastItem(i);
                    try {
                        Log.d(TAG, "Calling onBroadcastMetadataChanged: " + i);
                        callback.onBroadcastMetadataChanged(broadcastId, metadata);
                    } catch (RemoteException e) {
                        Log.e(TAG, "Stack:" + Log.getStackTraceString(e));
                    }
                }
                mBroadcastCallbacks.finishBroadcast();
            }
        }
    }

    private int chooseBestPAInterval() {
        int defaultPaInt = is_pbs_enabled() ? PBP_DEFAULT_PA_INTERVAL : BAP_DEFAULT_PA_INTERVAL;
        int cigCalculatorPaInt = is_pbs_enabled() ? PBP_CIG_CALCULATOR_PA_INTERVAL : BAP_CIG_CALCULATOR_PA_INTERVAL;

        // Use 360ms/120ms PA interval if ISO CIG Parameter calculator is enabled
        // Use 450ms/150ms PA interval if ISO CIG Parameter calculator is disabled
        AdapterService adapterService = AdapterService.getAdapterService();
        if (adapterService.isISOCIGParameterCalculator()) {
            Log.w(TAG, "ISO CIG Parameter calculator is enabled, PA interval: " + cigCalculatorPaInt);
            return cigCalculatorPaInt;
        } else {
            Log.d(TAG, "ISO CIG Parameter calculator is disabled, PA interval: " + defaultPaInt);
            return defaultPaInt;
        }
    }

    public void registerLeBroadcastCallback(IBluetoothLeBroadcastCallback callback) {
        if (mBroadcastCallbacks != null) {
            mBroadcastCallbacks.register(callback);
        }
    }

    public void unregisterLeBroadcastCallback(IBluetoothLeBroadcastCallback callback) {
        if (mBroadcastCallbacks != null) {
            mBroadcastCallbacks.unregister(callback);
        }
    }
    private boolean isAllZeros(byte[] broadcastCode) {
        for (byte a : broadcastCode) {
            if (a != 0) return false;
        }
        return true;
    }
    private boolean setBroadcastCode(byte[] broadcastCode) {
        if (broadcastCode == null || broadcastCode.length == 0 ||
           isAllZeros(broadcastCode)) {
            Log.d(TAG, "setBroadcastCode: code is null or length is zero");
            return SetEncryption(false, 0, false, null);
        }
        Log.d(TAG, "setBroadcastCode: length = " + broadcastCode.length);
        mEncryptionEnabled = true;
        Message msg = mHandler.obtainMessage(MSG_SET_BROADCAST_CODE);
        msg.obj = broadcastCode;
        mHandler.sendMessage(msg);
        return true;
    }

    public void startBroadcast(BluetoothLeBroadcastSettings broadcastSettings) {
        synchronized (mBroadcastLock) {
            if (mBroadcastState != BluetoothBroadcast.STATE_DISABLED) {
                Log.d(TAG, "startBroadcast: not in disabled state");
                notifyBroadcastStartFailed(BluetoothStatusCodes.ERROR_UNKNOWN);
                return;
            }
        }
        byte[] broadcastCode = broadcastSettings.getBroadcastCode();
        if (broadcastCode != null) {
            Log.d(TAG, "startBroadcast: code = " + Arrays.toString(broadcastCode));
            if (broadcastCode.length != 0 && broadcastCode.length < BROADCAST_CODE_LENGTH_MIN ||
                    broadcastCode.length > BROADCAST_CODE_LENGTH_MAX) {
                Log.e(TAG, "Invalid broadcast code length: " + broadcastCode.length);
                notifyBroadcastStartFailed(BluetoothStatusCodes.ERROR_LE_BROADCAST_INVALID_CODE);
                return;
            }
        }
        List<BluetoothLeBroadcastSubgroupSettings> settings =
                broadcastSettings.getSubgroupSettings();
        if (settings == null || settings.size() < 1) {
            Log.d(TAG, "subgroup settings is invalid");
            return;
        }
        BluetoothLeBroadcastSubgroupSettings subgroupSettings =
                settings.get(0);
        BluetoothLeAudioContentMetadata contentMetadata =
                subgroupSettings.getContentMetadata();
        if (contentMetadata == null) {
            Log.d(TAG, "contentMetadata cannot be null");
            return;
        }
        mBroadcastName = broadcastSettings.getBroadcastName();
        mIsPublicBroadcast = broadcastSettings.isPublicBroadcast();
        Log.d(TAG, "mBroadcastName: " + mBroadcastName +
                " mIsPublicBroadcast: " + mIsPublicBroadcast);
        mAudioContentMetadata = contentMetadata;
        setBroadcastCode(broadcastCode);
        if (mIsPublicBroadcast == true) {
            mPublicMetadata = broadcastSettings.getPublicBroadcastMetadata();
            int preferredQuality = subgroupSettings.getPreferredQuality();
            Log.d(TAG, "preferredQuality: " + preferredQuality);
            if (preferredQuality == BluetoothLeBroadcastSubgroupSettings.QUALITY_STANDARD) {
                mPbsAudioQuality = PBS_STD_QUALITY;
            } else if (preferredQuality == BluetoothLeBroadcastSubgroupSettings.QUALITY_HIGH) {
                mPbsAudioQuality = PBS_HIGH_QUALITY;
            }
        }
        if (is_pbs_with_sq_audio_enabled()) {
            mBroadcastConfigSettings = 2;
        } else if (is_pbs_with_hq_audio_enabled()) {
            mBroadcastConfigSettings = 4;
        } else {
            mBroadcastConfigSettings = SystemProperties.getInt("persist.vendor.btstack.bap_ba_setting", 4);
        }
        mBroadcastCodecConfig = new BroadcastCodecConfig();
        Log.d(TAG, "mPbsAudioQuality = " + mPbsAudioQuality +
                " mBroadcastConfigSettings = " + mBroadcastConfigSettings);
        if (EnableBroadcast(null) == false) {
            Log.d(TAG, "startBroadcast fail.");
            notifyBroadcastStartFailed(
                    BluetoothStatusCodes.ERROR_LOCAL_NOT_ENOUGH_RESOURCES);
        }
    }

    public void stopBroadcast(int broadcastId) {
        synchronized (mBroadcastLock) {
            if (mBroadcastState != BluetoothBroadcast.STATE_ENABLED &&
                    mBroadcastState != BluetoothBroadcast.STATE_STREAMING) {
                Log.d(TAG, "stopBroadcast: not in enabled or streaming state");
                notifyOnBroadcastStopFailed(BluetoothStatusCodes.ERROR_UNKNOWN);
                return;
            }
        }
        Optional<Integer> instanceId = mBroadcastIdMap.entrySet()
                .stream()
                .filter(entry -> Objects.equals(entry.getValue(), broadcastId))
                .map(Map.Entry::getKey)
                .findFirst();
        if (!instanceId.isPresent()) {
            Log.d(TAG, "Unknown broadcast ID " + broadcastId);
            return;
        }
        DisableBroadcast(null);
        mAudioContentMetadata = null;
        mBroadcastCodecConfig = null;
        mBroadcastName = null;
        mIsPublicBroadcast = false;
        mPublicMetadata = null;
    }

    public void updateBroadcast(int broadcastId,
                                BluetoothLeBroadcastSettings broadcastSettings) {
        Optional<Integer> instanceId = mBroadcastIdMap.entrySet()
                .stream()
                .filter(entry -> Objects.equals(entry.getValue(), broadcastId))
                .map(Map.Entry::getKey)
                .findFirst();
        if (!instanceId.isPresent()) {
            Log.d(TAG, "Unknown broadcast ID " + broadcastId);
            return;
        }
        List<BluetoothLeBroadcastSubgroupSettings> settings =
                broadcastSettings.getSubgroupSettings();
        if (settings == null || settings.size() < 1) {
            Log.d(TAG, "subgroup settings is invalid");
            return;
        }
        BluetoothLeAudioContentMetadata contentMetadata = settings.get(0).getContentMetadata();
        if (contentMetadata == null) {
            Log.d(TAG, "contentMetadata cannot be null");
            return;
        }
        mBroadcastName = broadcastSettings.getBroadcastName();
        // Currently assume mIsPublicBroadcast can't be updated.
        // mIsPublicBroadcast = broadcastSettings.isPublicBroadcast();
        mAudioContentMetadata = contentMetadata;
        if (mIsPublicBroadcast) {
            mPublicMetadata = broadcastSettings.getPublicBroadcastMetadata();
        }
        mBroadcastBase.populateBase();
        mBroadcastAdvertiser.updatePAwithBase();
        notifyBroadcastUpdated(mAdvertisingSet.getAdvertiserId(),
                BluetoothStatusCodes.REASON_LOCAL_APP_REQUEST);
        BluetoothLeBroadcastMetadata md = getBroadcastMetadata(
                mAdvertisingSet.getAdvertiserId());
        if (md != null) {
            mBroadcastMetadataMap.put(mAdvertisingSet.getAdvertiserId(), md);
            notifyBroadcastMetadataChanged(mAdvertisingSet.getAdvertiserId(), md);
        } else {
            Log.e(TAG, "Metadata is null");
        }
    }

    public boolean isPlaying(int broadcastId) {
        Optional<Integer> instanceId = mBroadcastIdMap.entrySet()
                .stream()
                .filter(entry -> Objects.equals(entry.getValue(), broadcastId))
                .map(Map.Entry::getKey)
                .findFirst();
        if (!instanceId.isPresent()) {
            Log.d(TAG, "Unknown broadcast ID " + broadcastId);
            return false;
        }
        return mBroadcastAudioState == BluetoothBroadcast.STATE_PLAYING;
    }

    public List<BluetoothLeBroadcastMetadata>
            getAllBroadcastMetadata() {
        Log.d(TAG, "getAllBroadcastMetadata: length = " + mBroadcastMetadataMap.values().size());
        return new ArrayList<BluetoothLeBroadcastMetadata>(mBroadcastMetadataMap.values());
    }

    public int getMaximumNumberOfBroadcasts() {
        return MAX_NUMBEROF_BROADCAST;
    }

    public int getMaximumStreamsPerBroadcast() {
        /* TODO: This is currently fixed to 1 */
        return 1;
    }

    public int getMaximumSubgroupsPerBroadcast() {
        /* TODO: This is currently fixed to 1 */
        return 1;
    }

    private byte[] reverse(byte[] code) {
        int len = code.length;
        byte[] codecopy = new byte[len];
        System.arraycopy(code, 0, codecopy, 0, len);
        for(int i = 0; i < len/2; i++) {
            byte c = codecopy[i];
            codecopy[i] = codecopy[len - i - 1];
            codecopy[len - i - 1] = c;
        }
        int i = len - 1;
        while(i >= 0 && codecopy[i] == 0)
            i--;
        return Arrays.copyOf(codecopy, i + 1);
    }

    private BluetoothLeBroadcastMetadata getBroadcastMetadata(int advId) {
        BluetoothDevice address = BluetoothAdapter.getDefaultAdapter().
                getRemoteDevice(BluetoothAdapter.getDefaultAdapter().getAddress());
        int addressType = BluetoothDevice.ADDRESS_TYPE_PUBLIC;
        BCService bcService = BCService.getBCService();
        if (bcService != null) {
            address = bcService.getLocalBroadcastSource();
            addressType = bcService.getLocalBroadcastSourceType();
        }
        if (mBroadcastIdMap.get(advId) == null) {
            Log.e(TAG, "Cannot get broadcast Id");
            return null;
        }
        Log.d (TAG,"getBroadcastMetadata id: " + mBroadcastIdMap.get(advId) +
                ", address: " + address +
                ", address type: " + addressType);
        int preferredQuality = BluetoothLeBroadcastMetadata.AUDIO_CONFIG_QUALITY_NONE;
        if (is_pbs_with_sq_audio_enabled()) {
            preferredQuality = BluetoothLeBroadcastMetadata.AUDIO_CONFIG_QUALITY_STANDARD;
        } else if (is_pbs_with_hq_audio_enabled()) {
            preferredQuality = BluetoothLeBroadcastMetadata.AUDIO_CONFIG_QUALITY_HIGH;
        }
        BluetoothLeBroadcastMetadata.Builder metaData = new BluetoothLeBroadcastMetadata.Builder()
                .setSourceDevice(address, addressType)
                .setSourceAdvertisingSid(advId)
                .setBroadcastId(mBroadcastIdMap.get(advId))
                .setPaSyncInterval(mPaInt)
                .setEncrypted(mEncryptionEnabled)
                .setBroadcastCode(reverse(BigBroadcastCode))
                .setPublicBroadcast(mIsPublicBroadcast)
                .setPublicBroadcastMetadata(mPublicMetadata)
                .setAudioConfigQuality(preferredQuality)
                .setBroadcastName(mBroadcastName)
                .setPresentationDelayMicros(mBroadcastBase.getPresentationDelay());
        for(BluetoothLeBroadcastSubgroup subGroup: mBroadcastBase.getSubGroups()) {
            metaData.addSubgroup(subGroup);
        }
        return metaData.build();
    }

    public void notifyBroadcastEnabled(boolean enabled) {
        if (DBG) Log.d (TAG,"notifyBroadcastEnabled: " + enabled);
        ActiveDeviceManagerService activeDeviceManager = ActiveDeviceManagerService.get();
        if(activeDeviceManager == null) {
            Log.e(TAG,"ActiveDeviceManagerService not started. Return");
            return;
        }
        if (enabled) {
            activeDeviceManager.enableBroadcast(mBroadcastDevice);

            if (mNotificationManager != null ) {
                mNOTIFICATION_ID = android.R.drawable.stat_sys_data_bluetooth;
                Notification notification =
                    new Notification.Builder(this, BROADCAST_NOTIFICATION_ID)
                    .setContentTitle(this.getString(R.string.broadcast_notification_name))
                    .setContentText(this.getString(R.string.bluetooth_broadcast_status))
                    .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
                    .setChannelId(BROADCAST_NOTIFICATION_ID)
                    .build();
                mNotificationManager.notify(mNOTIFICATION_ID, notification);
            } else {
                Log.d(TAG,"mNotificationManager is null");
            }
        }
        else {
            activeDeviceManager.disableBroadcast();
            if (mNotificationManager != null && mNOTIFICATION_ID != -1) {
                mNotificationManager.cancel(mNOTIFICATION_ID);
                mNOTIFICATION_ID = -1;
            }
        }
    }

    public void updateMetadataFromAvrcp(MediaMetadata data) {
        if (DBG) Log.d (TAG,"updateMetadataFromAvrcp");
        mTrackMetadata = new TrackMetadata(data);
    }
    public void messageFromNative(BroadcastStackEvent event) {
        if (DBG) Log.d (TAG,"messageFromNative: event " + event);
        switch(event.type) {
            case BroadcastStackEvent.EVENT_TYPE_BROADCAST_STATE_CHANGED:
                {
                    Message msg =
                        mHandler.obtainMessage(MSG_FROM_NATIVE_BROADCAST_STATE,
                                               event.valueInt, event.advHandle);
                    mHandler.sendMessage(msg);
                }
                break;
            case BroadcastStackEvent.EVENT_TYPE_BROADCAST_AUDIO_STATE_CHANGED:
                {
                    Message msg =
                        mHandler.obtainMessage(MSG_FROM_NATIVE_BROADCAST_AUDIO_STATE,
                                               event.valueInt, event.advHandle);
                    mHandler.sendMessage(msg);
                }
                break;
            case BroadcastStackEvent.EVENT_TYPE_CODEC_CONFIG_CHANGED:
                {
                    Message msg =
                        mHandler.obtainMessage(MSG_FROM_NATIVE_CODEC_STATE);
                    msg.obj = event.codecStatus;
                    mHandler.sendMessage(msg);
                }
                break;
            case BroadcastStackEvent.EVENT_TYPE_ENC_KEY_GENERATED:
                {
                    Message msg =
                        mHandler.obtainMessage(MSG_FROM_NATIVE_ENCRYPTION_KEY);
                    msg.obj = event.key;
                    mHandler.sendMessage(msg);
                }
                break;
            case BroadcastStackEvent.EVENT_TYPE_SETUP_BIG:
                {
                    mBIGHandle =  event.bigHandle;
                    if (event.valueInt == 1)
                        mNumBises = event.NumBises;
                    Message msg =
                        mHandler.obtainMessage(MSG_FROM_NATIVE_SETUP_BIG,event.valueInt, event.advHandle);
                    mHandler.sendMessage(msg);
                }
                break;
            case BroadcastStackEvent.EVENT_TYPE_BROADCAST_ID_GENERATED:
                {
                    Message msg =
                        mHandler.obtainMessage(MSG_FROM_NATIVE_BROADCAST_ID);
                    for (int i = 0; i < mBroadcastIdLength; i++) {
                         mBroadcastID[i] = (byte)event.BroadcastId[i];
                         Log.d(TAG,"mBroadcastID["+i+"]" + " = " + mBroadcastID[i]);
                    }
                    mHandler.sendMessage(msg);
                }
                break;
            default:
              Log.e (TAG,"messageFromNative: Invalid");
        }
    }
    class TrackMetadata {
     private String title;
     private String artistName;
     private String albumName;
     private String genre;
     private long playingTimeMs;

         public TrackMetadata(MediaMetadata data) {
             if (data == null) return;
             artistName = stringOrBlank(data.getString(MediaMetadata.METADATA_KEY_ARTIST));
             albumName = stringOrBlank(data.getString(MediaMetadata.METADATA_KEY_ALBUM));
             title = data.getString(MediaMetadata.METADATA_KEY_TITLE);
             genre = stringOrBlank(data.getString(MediaMetadata.METADATA_KEY_GENRE));
             playingTimeMs = data.getLong(MediaMetadata.METADATA_KEY_DURATION);
         }
         private String stringOrBlank(String s) {
            return s == null ? new String() : s;
         }
    }
    class BroadcastAdvertiser {
      public BroadcastAdvertiser() {
          Log.i(TAG,"BroadcastAdvertiser");
          mCallback = new BroadcastAdvertiserCallback();
          mAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
          if (mAdvertiser == null) {
              Log.e(TAG, "BroadcastAdvertiser: mAdvertiser is null");
          }
      }
      public void startBroadcastAdvertising() {
          Log.i(TAG,"startBroadcastAdvertising");
          if (mAdvertiser == null) {
              Log.e(TAG,"startBroadcastAdvertising: Advertiser is null");
              int prev_state = mBroadcastState;
              mBroadcastState = BluetoothBroadcast.STATE_DISABLED;
              broadcastState(mBroadcastState, prev_state);
              notifyBroadcastStartFailed(BluetoothStatusCodes.ERROR_LOCAL_NOT_ENOUGH_RESOURCES);
              return;
          }
          AdvertisingSetParameters.Builder adv_param =
                         new AdvertisingSetParameters.Builder();
          adv_param.setLegacyMode(false);
          adv_param.setConnectable(false);
          adv_param.setScannable(false);
          adv_param.setInterval(AdvertisingSetParameters.INTERVAL_MIN); //100msec
          adv_param.setTxPowerLevel(mTxPowerLevel);
          adv_param.setPrimaryPhy(1);
          adv_param.setSecondaryPhy(mSecPhy);
          AdvertiseData AdvData;
          if (is_pbs_enabled()) {
                AdvData = new AdvertiseData.Builder()
                                           .setIncludeDeviceName(true)
                                           .addServiceData(new ParcelUuid(BROADCAST_AUDIO_UUID), mBroadcastID)
                                           .setIncludePublicBroadcastDeviceName(true, mBroadcastName)
                                           .addServiceData(new ParcelUuid(PUBLIC_BROADCAST_AUDIO_UUID), populate_public_broadcast_service_data())
                                           .build();
                // If public broadcast profile is enabled then periodic advertising interval must be less than or equal to 120ms.
          } else {
                AdvData = new AdvertiseData.Builder()
                                           .setIncludeDeviceName(true)
                                           .addServiceData(new ParcelUuid(BROADCAST_AUDIO_UUID), mBroadcastID).build();
          }
          if (mPreferPaInt > 0) {
              mPaInt = mPreferPaInt;
          } else {
              mPaInt = chooseBestPAInterval();
          }
          Log.i(TAG, "PA interval is " + mPaInt);

          PeriodicAdvertisingParameters.Builder periodic_param = new PeriodicAdvertisingParameters.Builder();
          periodic_param.setIncludeTxPower(true);
          periodic_param.setInterval(mPaInt);
          AdvertiseData PeriodicData = new AdvertiseData.Builder().addServiceData(new ParcelUuid(BASIC_AUDIO_UUID),
                  new byte[0]).build();
          Log.i(TAG,"Calling startAdvertisingSet");
          mAdvertiser.startAdvertisingSet(adv_param.build(), AdvData, null, periodic_param.build(), PeriodicData, 0, 0, mCallback);
      }
      public void stopBroadcastAdvertising() {
          Log.i(TAG,"stopBroadcastAdvertising");
          if (mAdvertiser != null)
              mAdvertiser.stopAdvertisingSet(mCallback);
      }

      public void updatePAwithBase() {
          Log.i(TAG,"updatePAwithBase");
          AdvertiseData PeriodicData = new AdvertiseData.Builder().addServiceData(new ParcelUuid(BASIC_AUDIO_UUID), mBroadcastBase.getBroadcastBaseInfo()).build();
          mAdvertisingSet.setPeriodicAdvertisingData(PeriodicData);
      }

      private byte[] populate_public_broadcast_service_data() {
          int is_stream_encrypted = (mEncKey.length > 0)? 0x01:0x00;
          int is_std_quality_audio = 0x02;
          int is_high_quality_audio = 0x04;
          byte[] public_broadcast_stream_features =  new byte[1];
          if (is_pbs_with_sq_audio_enabled()) {
              Log.i(TAG,"PBS SQ_AUDIO");
              public_broadcast_stream_features = mBroadcastBase.intTobyteArray((is_stream_encrypted|is_std_quality_audio), 1);
          } else {
              Log.i(TAG,"PBS HQ_AUDIO");
              public_broadcast_stream_features = mBroadcastBase.intTobyteArray((is_stream_encrypted|is_high_quality_audio), 1);
          }
          ByteArrayOutputStream ByteStr = new ByteArrayOutputStream();
          ByteStr.write(public_broadcast_stream_features, 0, public_broadcast_stream_features.length);
          byte[] media_context_meta_data_length =  new byte[1];
          if (mPublicMetadata != null) {
              byte[] rawBytes = mPublicMetadata.getRawMetadata();
              media_context_meta_data_length = mBroadcastBase.intTobyteArray(rawBytes.length, 1);
              ByteStr.write(media_context_meta_data_length, 0, media_context_meta_data_length.length);
              ByteStr.write(rawBytes, 0, rawBytes.length);
          } else {
              byte [] media_context_meta_data = {PBP_META_DATA_LENGTH, PBP_META_DATA_TYPE, (byte)0x04, (byte)0x00};
              media_context_meta_data_length = mBroadcastBase.intTobyteArray(media_context_meta_data.length, 1);
              ByteStr.write(media_context_meta_data_length, 0, media_context_meta_data_length.length);
              ByteStr.write(media_context_meta_data, 0, media_context_meta_data.length);
          }
          return ByteStr.toByteArray();
      }

      public byte[] getPublicBroadcastServiceData() {
          if (!is_pbs_enabled()) {
              return null;
          }
          return populate_public_broadcast_service_data();
      }
    }

    private class BroadcastAdvertiserCallback extends AdvertisingSetCallback {
        @Override
        public void onAdvertisingSetStarted(AdvertisingSet advertisingSet, int txPower,
                                         int status) {
            Log.i(TAG, "onAdvertisingSetStarted status " + status
                 + " advertisingSet: " + advertisingSet + " txPower " + txPower);
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG,"Failed to start Broadcast Advertisement");
                int prev_state = mBroadcastState;
                mBroadcastState = BluetoothBroadcast.STATE_DISABLED;
                broadcastState(mBroadcastState,prev_state);
                notifyBroadcastStartFailed(BluetoothStatusCodes.ERROR_LOCAL_NOT_ENOUGH_RESOURCES);
            }
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mBroadcastIdMap.put(advertisingSet.getAdvertiserId(), new BigInteger(1, reverse(mBroadcastID)).intValue());
                mAdvertisingSets.put(advertisingSet.getAdvertiserId(), advertisingSet);
                mAdvertisingSet = advertisingSet;
                mIsAdvertising = true;
                Log.i(TAG,"onAdvertisingSetStarted: adv_id = " + advertisingSet.getAdvertiserId() + "copied id = " + mAdvertisingSet.getAdvertiserId());
                mAdvertisingSet.getOwnAddress();
                int msgDelay = 20;
                if (mHandler.hasMessages(MSG_RESET_ENCRYPTION_FLAG_TIMEOUT)) {
                    msgDelay = 600;
                } else {
                    BCService bcService = BCService.getBCService();
                    if (bcService != null && bcService.isAutoLocalSourceAddForActiveDeviceEnabled(null)) {
                        //delay MM indication by 3 sec
                        msgDelay = 3000;
                    }
                }
                Message msg =
                        mHandler.obtainMessage(MSG_SET_BROADCAST_ACTIVE);
                Log.i(TAG, "MSG_SET_BROADCAST_ACTIVE: delay " + msgDelay);
                mHandler.sendMessageDelayed(msg, msgDelay);
                int mChMode = mCodecConfig.getChannelMode();
                switch (mChMode) {
                    case BluetoothCodecConfig.CHANNEL_MODE_MONO:
                    case BluetoothCodecConfig.CHANNEL_MODE_JOINT_STEREO:
                        mNumBises = 1 * mNumSubGrps;
                        break;
                    case BluetoothCodecConfig.CHANNEL_MODE_STEREO:
                        mNumBises = 2 * mNumSubGrps;
                        break;
                    default:
                        Log.e(TAG,"channel mode unknown");
                }
                mBroadcastBase.populateBase();
                mBroadcastAdvertiser.updatePAwithBase();
            }
        }

        @Override
        public void onAdvertisingSetStopped(AdvertisingSet advertisingSet) {
            Log.i(TAG, "onAdvertisingSetStopped advertisingSet: " + advertisingSet);
            mIsAdvertising = false;
            mAdvertisingSets.remove(advertisingSet.getAdvertiserId());
            int prev_state = mBroadcastState;
            if (!goingDown && mBroadcastDeviceIsActive) {
                Log.d(TAG,"onAdvertisingSetStopped: Unexpected Broadcast turn off");
                notifyBroadcastEnabled(false);
            }
            if (goingDown) {
                Message msg = mHandler.obtainMessage(MSG_UPDATE_BROADCAST_STATE,
                               BluetoothBroadcast.STATE_DISABLING);
                mHandler.sendMessageDelayed(msg,300);
                mBroadcastState = BluetoothBroadcast.STATE_DISABLING;
                goingDown = false;
            } else {
                mBroadcastState = BluetoothBroadcast.STATE_DISABLED;
                broadcastState(mBroadcastState, prev_state);
                notifyOnBroadcastStopped(mAdvertisingSet.getAdvertiserId(),
                        BluetoothStatusCodes.REASON_LOCAL_APP_REQUEST);
                mBroadcastMetadataMap.remove(mAdvertisingSet.getAdvertiserId());
                mBroadcastIdMap.remove(advertisingSet.getAdvertiserId());
            }
        }

        @Override
        public void onAdvertisingEnabled(AdvertisingSet advertisingSet, boolean enable,
                                         int status) {
            Log.i(TAG, "onAdvertisingEnabled advertisingSet: " + advertisingSet
                    + " status " + status + " enable: " + enable);
        }

        @Override
        public void onAdvertisingDataSet(AdvertisingSet advertisingSet, int status) {
            Log.i(TAG, "onAdvertisingDataSet advertisingSet: " + advertisingSet
                    + " status " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "onAdvertisingDataSet: Base Info updated");
            }
        }
        @Override
        public void onAdvertisingParametersUpdated(AdvertisingSet advertisingSet,
                                                   int txPower, int status) {
            Log.i(TAG, "onAdvertisingParametersUpdated  advertisingSet: " + advertisingSet
                    + " status " + status  + " txPower " + txPower);
        }

        @Override
        public void onOwnAddressRead(AdvertisingSet advertisingSet, int addressType,
                                     String address) {
            Log.i(TAG, "onOwnAddressRead advertisingSet: " + advertisingSet
                    + " address " + address + " addressType " + addressType);
            mAdvAddress = address;
            mAdvAddressType = addressType;
        }

    }
    class BroadcastBase {
        private final int LC3_SAMPLE_RATE_8000 = 0x01;
        private final int LC3_SAMPLE_RATE_16000 = 0x02;
        private final int LC3_SAMPLE_RATE_24000 = 0x03;
        private final int LC3_SAMPLE_RATE_32000 = 0x04;
        private final int LC3_SAMPLE_RATE_44100 = 0x05;
        private final int LC3_SAMPLE_RATE_48000 = 0x06;

        int presentationDelay = 0x009C40;
        byte [] mPresentationDelay = new byte[3];
        byte [] mCodecId = new byte[5];
        byte [] mCodecSpecificLength = new byte[1];
        byte [] mCodecSpecificSampleRate = new byte[3];
        byte [] mCodecSpecificFrameDuration = new byte[3];
        byte [] mCodecSpecificAudioLocation = new byte[6];
        byte [] mCodecSpecificOctetsPerFrame = new byte[3];
        byte [] mCodecSpecificBlocksPerSdu = new byte[3];
        byte [] mCodecSpecificLengthL2 = new byte[1];
        byte [] mCodecSpecificSampleRateL2 = new byte[3];
        byte [] mCodecSpecificFrameDurationL2 = new byte[3];
        byte [] mCodecSpecificAudioLocationL2 = new byte[6];
        byte [] mCodecSpecificOctetsPerFrameL2 = new byte[3];
        byte [] mCodecSpecificBlocksPerSduL2 = new byte[3];
        byte [] mMetadataLength = new byte[1];
        byte [] mMetadataContext = new byte[3];
        byte [] mNumSubgroups = new byte[1];
        byte [] mL2CodecID = new byte[1];
        byte [] mL2CodecSpecificLength = new byte[1];
        byte [] mL2mMetadataLength = new byte[1];
        byte [] mL2NumBises = new byte[1];
        byte [] mL2BisIndices = new byte[2];
        byte [] mL3BisIndex = new byte[1];
        byte [] mL3CodecSpecificLength = new byte[1];
        byte [] mL3CodecSpecificAudioLocation = new byte[6];
        byte mSampleRateLength = 2;
        byte mSampleRateType = 0x01;
        byte mFrameDurationLength = 2;
        byte mFrameDurationType = 0x02;
        byte mFrameDuration_7_5 = 0x00;//7.5 msec
        byte mFrameDuration_10 = 0x01;//10msec
        byte mAudioLocationLength = 5;
        byte mAudioLocationType = 0x03;
        byte mAudioLocationLeft = 0x01;
        byte mAudioLocationRight = 0x02;
        byte mAudioLocationCentre = 0x04;
        byte mOctetsPerFrameLength = 3;
        byte mOctestPerFrameType = 0x04;
        byte mBlocksPerSduLength = 2;
        byte mBlocksPerSduType = 0x05;
        long LC3_CODEC_ID_OLD = 0x0000000001;
        long LC3_CODEC_ID = 0x0000000006;
        byte mCodecConfigLength = 0x10; //to be changed
        byte mMediaContextType = 0x10;
        byte mStreamingAudioContextType = 0x02;
        byte mCcidListType = 0x05;
        byte [] BroadcastBaseArray = null;

        //Metadata AD type
        //Metadata
      public BroadcastBase() {
          //mccid = 0;
          //int presentationDelay = 0x000014;
          if (mPD == 20) {
              Log.d(TAG,"Presentation Delay is set to 20msec");
              presentationDelay = 0x004E20;
          }
          if (mNewVersion) {
              mPresentationDelay = intTobyteArray(presentationDelay, 3);
              mNumSubgroups[0] = (byte)mNumSubGrps;
          } else {
              mPresentationDelay = intTobyteArray(presentationDelay, 3);
              if (new_codec_id) {
                  mCodecId = longTobyteArray(LC3_CODEC_ID,5);
              } else {
                  mCodecId = longTobyteArray(LC3_CODEC_ID_OLD,5);
              }
              mCodecSpecificLength[0] = mCodecConfigLength;
              mCodecSpecificSampleRate = updateSampleRate();
              mCodecSpecificFrameDuration = updateFrameDuration();
              mCodecSpecificAudioLocation = updateAudioLocation(0);
              mCodecSpecificOctetsPerFrame = updateOctetsPerFrame();
              mMetadataLength[0] = (byte)0x03;
              int index = 0;
              mMetadataContext[index++] = (byte)0x02; //length
              mMetadataContext[index++] = (byte)mMediaContextType; //Type
              mMetadataContext[index++] = (byte)0x01; //Value Music
              mNumSubgroups[0] = (byte)mNumSubGrps; // only one set of broadcast is supported.
          }
      }
      public byte [] getBroadcastBaseInfo() {
          return BroadcastBaseArray;
      }
      public void updateBIGhandle(int handle) {
          mBIGHandle = handle;
      }

      public byte[] getMetadataContext() {
          return mMetadataContext;
      }

      public int getNumSubGroups() {
          return mNumSubgroups[0];
      }

      public BluetoothLeBroadcastSubgroup[] getSubGroups() {
          BluetoothLeBroadcastSubgroup[] subGroups = new BluetoothLeBroadcastSubgroup[mNumSubGrps];
          if (mNewVersion) {
              ByteArrayOutputStream ByteStr = new ByteArrayOutputStream();
              for (int i = 0; i < mNumSubGrps; i ++) {
                  BluetoothLeBroadcastSubgroup.Builder subGroup =
                          new BluetoothLeBroadcastSubgroup.Builder();
                  subGroup.setCodecId(new_codec_id ? LC3_CODEC_ID : LC3_CODEC_ID_OLD);
                  ByteStr.reset();
                  if (mPartialSimulcast && (i < mNumSubGrps / 2)) {
                      ByteStr.write(mCodecSpecificSampleRate, 0, mCodecSpecificSampleRate.length);
                      ByteStr.write(mCodecSpecificFrameDuration, 0, mCodecSpecificFrameDuration.length);
                      ByteStr.write(mCodecSpecificAudioLocation, 0, mCodecSpecificAudioLocation.length);
                      ByteStr.write(mCodecSpecificOctetsPerFrame, 0, mCodecSpecificOctetsPerFrame.length);
                      ByteStr.write(mCodecSpecificBlocksPerSdu, 0, mCodecSpecificBlocksPerSdu.length);
                  } else {
                      ByteStr.write(mCodecSpecificSampleRateL2, 0, mCodecSpecificSampleRateL2.length);
                      ByteStr.write(mCodecSpecificFrameDurationL2, 0, mCodecSpecificFrameDurationL2.length);
                      ByteStr.write(mCodecSpecificAudioLocationL2, 0, mCodecSpecificAudioLocationL2.length);
                      ByteStr.write(mCodecSpecificOctetsPerFrameL2, 0, mCodecSpecificOctetsPerFrameL2.length);
                      ByteStr.write(mCodecSpecificBlocksPerSduL2, 0, mCodecSpecificBlocksPerSduL2.length);
                  }
                  subGroup.setCodecSpecificConfig(BluetoothLeAudioCodecConfigMetadata.
                          fromRawBytes(ByteStr.toByteArray()));
                  subGroup.setContentMetadata(BluetoothLeAudioContentMetadata.fromRawBytes(updateMetadata()));
                  int bisPerGroup = calculateBisPerGroup();
                  for (int j =0; j < bisPerGroup; j ++) {
                      BluetoothLeBroadcastChannel.Builder channel = new BluetoothLeBroadcastChannel.Builder();
                      channel.setSelected(false);
                      channel.setChannelIndex(1 + bisPerGroup * i + j);
                      byte [] config = updateAudioLocation(j+1);
                      channel.setCodecMetadata(BluetoothLeAudioCodecConfigMetadata.fromRawBytes(config));
                      subGroup.addChannel(channel.build());
                  }
                  subGroups[i] = subGroup.build();
              }
          }
          return subGroups;
      }

      public int getPresentationDelay() {
          return presentationDelay;
      }

      public byte [] updateMetadataContext() {
          Log.i(TAG, "updateMetadataContext: mStreamingAudioContext is " + mStreamingAudioContext);
          byte [] audioContextArray = intTobyteArray(mStreamingAudioContext, 2);
          byte ltv [] = {3, mStreamingAudioContextType, (byte)audioContextArray[0], (byte)audioContextArray[1]};
          return ltv;
      }

      public byte [] updateMetdadataCcid() {
          McpService mcpService = McpService.getMcpService();
          int ccid = 0;
          if (mcpService != null) {
              ccid = mcpService.getControlContentID();
          }
          Log.i(TAG, "mCcidNum: " + mCcidNum + " ccid: " + ccid);

          if (mCcidNum == 0) {
              Log.i(TAG, "Ccid is None");
              return null;
          } else if (mCcidNum == 1) {
              byte [] ltv = {2, mCcidListType, (byte)ccid};
              return ltv;
          } else if (mCcidNum == 2) {
              // Multiple CCID case, used for test only
              int ccid2 = ccid + 1;
              byte [] ltv = {3, mCcidListType, (byte)ccid, (byte)ccid2};
              return ltv;
          }

          Log.i(TAG, "Invalid Ccid num: " + mCcidNum);
          return null;
      }

      public byte [] updateMetadata() {
          byte [] contextLtv = updateMetadataContext();
          byte [] ccidListLtv = updateMetdadataCcid();
          int metadataLength = 0;
          boolean isContextLtvPresent = false;
          boolean isCcidListLtvPresent = false;

          if (mAudioContentMetadata != null) {
              int currentPos = 0;
              byte[] rawBytes = mAudioContentMetadata.getRawMetadata();
              // check duplicate LTV items
              while (currentPos < rawBytes.length) {
                  int length = rawBytes[currentPos] & 0xFF;
                  if (length == 0) {
                      break;
                  }
                  currentPos++;
                  if (currentPos >= rawBytes.length) {
                      break;
                  }
                  int type = rawBytes[currentPos] & 0xFF;
                  currentPos++;
                  if (currentPos >= rawBytes.length) {
                      break;
                  }
                  int dataLength = length - 1;
                  if (rawBytes.length - currentPos < dataLength) {
                      break;
                  }
                  if (type == mStreamingAudioContextType) {
                      Log.d(TAG, "Audio context LTV is already present");
                      isContextLtvPresent = true;
                  } else if (type == mCcidListType) {
                      Log.d(TAG, "CCID list LTV is already present");
                      isCcidListLtvPresent = true;
                  }
                  currentPos += dataLength;
              }
          }

          if (isContextLtvPresent != true && contextLtv != null) {
              metadataLength += contextLtv.length;
          }
          if (isCcidListLtvPresent != true && ccidListLtv != null) {
              metadataLength += ccidListLtv.length;
          }
          if (mAudioContentMetadata != null) {
              metadataLength += mAudioContentMetadata.getRawMetadata().length;
          }
          byte [] metadata = new byte[metadataLength];
          int dstPos = 0;

          if (isContextLtvPresent != true && contextLtv != null) {
              System.arraycopy(contextLtv, 0, metadata, dstPos, contextLtv.length);
              dstPos += contextLtv.length;
          }
          if (isCcidListLtvPresent != true && ccidListLtv != null) {
              System.arraycopy(ccidListLtv, 0, metadata, dstPos, ccidListLtv.length);
              dstPos += ccidListLtv.length;
          }
          if (mAudioContentMetadata != null) {
              System.arraycopy(mAudioContentMetadata.getRawMetadata(), 0,
                      metadata, dstPos, mAudioContentMetadata.getRawMetadata().length);
          }
          return metadata;
      }

      public byte [] updateSampleRate() {
          int SR = mCodecConfig.getSampleRate();
          byte bytevalue;
          switch (SR) {
              case BluetoothCodecConfig.SAMPLE_RATE_48000:
                if (mNewVersion) {
                    bytevalue = (byte)0x08;
                } else {
                    bytevalue = (byte)0x06;
                }
                break;
              case BluetoothCodecConfig.SAMPLE_RATE_44100:
                if (mNewVersion) {
                    bytevalue = (byte)0x07;
                } else {
                    bytevalue = (byte)0x05;
                }
                break;
              case BluetoothCodecConfig.SAMPLE_RATE_32000:
                if (mNewVersion) {
                    bytevalue = (byte)0x06;
                } else {
                    bytevalue = (byte)0x04;
                }
                break;
              case BluetoothCodecConfig.SAMPLE_RATE_24000:
                if (mNewVersion) {
                    bytevalue = (byte)0x05;
                } else {
                    bytevalue = (byte)0x03;
                }
                break;
              case BluetoothCodecConfig.SAMPLE_RATE_16000:
                if (mNewVersion) {
                    bytevalue = (byte)0x03;
                } else {
                    bytevalue = (byte)0x02;
                }
                break;
              case BluetoothCodecConfig.SAMPLE_RATE_8000:
                bytevalue = (byte)0x01;
                break;
              default:
                if (mNewVersion) {
                    bytevalue = (byte)0x08;
                } else {
                    bytevalue = (byte)0x06;
                }
          }
          byte [] ltv = {mSampleRateLength, mSampleRateType, bytevalue};
          return ltv;
      }
      public byte[] updateOctetsPerFrame() {
          long bitrate = (int) mCodecConfig.getCodecSpecific1();
          long frameDuration = (int) mCodecConfig.getCodecSpecific2();
          byte bytevalue;
          //Update OctetsPerFrame based on frame duration
          switch ((int)bitrate) {
              case 1001:
                if (frameDuration == 0) { //7.5msec
                   bytevalue = (byte)30;
                } else { //10msec
                   bytevalue = (byte)40;
                }
                break;
              case 1002:
                if (frameDuration == 0) {
                    bytevalue = (byte)45;
                } else {
                    bytevalue = (byte)60;
                }
                break;
              case 1004:
                if (frameDuration == 0) {
                    bytevalue = (byte)75;
                } else {
                    bytevalue = (byte)100;
                }
                break;
              case 1006:
                if (frameDuration == 0) {
                    bytevalue = (byte)90;
                } else {
                    bytevalue = (byte)120;
                }
                break;
              case 1007:
                if (frameDuration == 0) {
                    bytevalue = (byte)117;
                } else {
                    bytevalue = (byte)155;
                }
                break;
              default:
                bytevalue = (byte)100;
          }
          Log.d(TAG,"updateOctetsPerFrame: " + bytevalue);
          byte [] ltv = {mOctetsPerFrameLength, mOctestPerFrameType, bytevalue, 0x00};
          return ltv;
      }
      private byte[] updateBlocksPerSdu() {
          byte[] ltv = {mBlocksPerSduLength, mBlocksPerSduType,0x01};
          return ltv;
      }

      public byte [] updateHAPSampleRate() {
          int SR = mCodecConfig.getSampleRate();
          byte bytevalue;
          switch (SR) {
              case BluetoothCodecConfig.SAMPLE_RATE_16000:
                bytevalue = (byte)0x02;
                break;
              case BluetoothCodecConfig.SAMPLE_RATE_24000:
                bytevalue = (byte)0x03;
              default:
                bytevalue = (byte)0x02;
          }
          byte[] ltv = {mSampleRateLength, mSampleRateType, bytevalue};
          return ltv;
      }
      public byte [] updateHapOctetsPerFrame() {
          long bitrate = mCodecConfig.getCodecSpecific1();
          long frameDuration = (int) mCodecConfig.getCodecSpecific2();
          byte bytevalue;
          //Update OctetsPerFrame based on frame duration
          switch((int)bitrate) {
              case 1001:
                if (frameDuration == 0) { //7.5msec
                   bytevalue = (byte)30;
                } else { //10msec
                   bytevalue = (byte)40;
                }
                break;
              case 1002:
                if (frameDuration == 0) {
                    bytevalue = (byte)45;
                } else {
                    bytevalue = (byte)60;
                }
                break;
              default:
                bytevalue = (byte)40;
          }
          byte [] ltv = {mOctetsPerFrameLength, mOctestPerFrameType, bytevalue, 0x00};
          return ltv;
      }
      public byte [] updateAudioLocation(int bis_index) {
          int ch_mode = mCodecConfig.getChannelMode();
          byte ch = 0;
          if (bis_index == 0) {
              // stereo
              if (ch_mode == BluetoothCodecConfig.CHANNEL_MODE_STEREO ||
                  ch_mode == BluetoothCodecConfig.CHANNEL_MODE_JOINT_STEREO)
                  ch = (byte)0x03;
              else if (ch_mode == BluetoothCodecConfig.CHANNEL_MODE_MONO)
                  ch = (byte)0x00;
          } else {
              if (ch_mode == BluetoothCodecConfig.CHANNEL_MODE_STEREO) {
                  int bises = (mNumBises/((int)mNumSubgroups[0]));
                  ch = (byte)(mAudioLocationRight - (bis_index % bises));
              } else if (ch_mode == BluetoothCodecConfig.CHANNEL_MODE_JOINT_STEREO) {
                  ch = (byte)0x03;
              } else if (ch_mode == BluetoothCodecConfig.CHANNEL_MODE_MONO) {
                  ch = (byte)0x00;
              }
          }
          byte [] loc = {mAudioLocationLength, mAudioLocationType, ch, 0x00, 0x00, 0x00};
          return loc;
      }
      public byte[] updateFrameDuration() {
          byte mFD = mFrameDuration_10;
          if (mCodecConfig.getCodecSpecific2() == 0) {
              Log.d(TAG,"updateFrameDuration: 7.5msec");
              mFD = mFrameDuration_7_5;
          } else {
              Log.d(TAG,"updateFrameDuration: 10 msec");
          }
          byte[] ltv = {mFrameDurationLength,mFrameDurationType,mFD};
          return ltv;
      }
      public byte[] intTobyteArray(int intValue, int bytelen) {
          byte [] val = new byte[bytelen];
          for (int i = 0; i < bytelen; i++) {
              val[(bytelen - 1) -i] = (byte)((intValue >> (8 *(bytelen - (i + 1)))) & 0x000000FF);
          }
          return val;
      }
      public byte [] longTobyteArray(long longValue, int bytelen) {
          byte [] val = new byte[bytelen];
          for (int i = 0; i < bytelen; i++) {
              val[(bytelen - 1) -i] = (byte)((longValue >> (8 *(bytelen - (i + 1)))) & 0x00000000000000FF);
          }
          return val;
      }
      public int calculateBisPerGroup() {
          int mChMode = mCodecConfig.getChannelMode();
          int numbis = 2;
          switch (mChMode) {
              case BluetoothCodecConfig.CHANNEL_MODE_MONO:
              case BluetoothCodecConfig.CHANNEL_MODE_JOINT_STEREO:
                  Log.d(TAG,"BisPerGroup is 1");
                  numbis = 1;
                  break;
              case BluetoothCodecConfig.CHANNEL_MODE_STEREO:
                  Log.d(TAG,"BisPerGroup is 2");
                  numbis = 2;
                  break;
              default:
                  Log.e(TAG,"channel mode unknown");
          }
          return numbis;
      }
      public void populateBase() {
          if (DBG) Log.d(TAG,"populateBase");
          mBisInfo.clear();
          byte [] baseL1 = populate_level1_base();
          byte [] baseL2 = populate_level2_base();
          ByteArrayOutputStream ByteStr = new ByteArrayOutputStream();
          ByteStr.write(baseL1, 0, baseL1.length);
          ByteStr.write(baseL2, 0, baseL2.length);
          if (!mNewVersion) {
              byte [] baseL3 = populate_level3_base();
              ByteStr.write(baseL3, 0, baseL3.length);
          }
          BroadcastBaseArray = ByteStr.toByteArray();
      }
      private byte [] populate_level1_base() {
          ByteArrayOutputStream ByteStr = new ByteArrayOutputStream();
          if (mNewVersion) {
              mPresentationDelay = intTobyteArray(presentationDelay, 3);
              mNumSubgroups[0] = (byte)mNumSubGrps;//calculate based on num bises and channel mode
              ByteStr.write(mPresentationDelay, 0, mPresentationDelay.length);
              ByteStr.write(mNumSubgroups, 0, mNumSubgroups.length);
          } else {
              mPresentationDelay = intTobyteArray(presentationDelay, 3);
              if (new_codec_id) {
                  mCodecId = longTobyteArray(LC3_CODEC_ID,5);
              } else {
                  mCodecId = longTobyteArray(LC3_CODEC_ID_OLD,5);
              }
              mCodecSpecificLength[0] = mCodecConfigLength;
              mCodecSpecificSampleRate = updateSampleRate();
              mCodecSpecificFrameDuration = updateFrameDuration();
              mCodecSpecificAudioLocation = updateAudioLocation(0);
              mCodecSpecificOctetsPerFrame = updateOctetsPerFrame();
              mMetadataLength[0] = (byte)0x03;
              byte [] mediacontext = {2, mMediaContextType, (byte)0x01};
              mNumSubgroups[0] = (byte)mNumSubGrps;//calculate based on num bises and channel mode

              ByteStr.write(mPresentationDelay, 0, mPresentationDelay.length);
              ByteStr.write(mCodecId, 0, mCodecId.length);
              ByteStr.write(mCodecSpecificLength, 0, mCodecSpecificLength.length);
              ByteStr.write(mCodecSpecificSampleRate, 0, mCodecSpecificSampleRate.length);
              ByteStr.write(mCodecSpecificFrameDuration, 0, mCodecSpecificFrameDuration.length);
              ByteStr.write(mCodecSpecificAudioLocation, 0, mCodecSpecificAudioLocation.length);
              ByteStr.write(mCodecSpecificOctetsPerFrame, 0, mCodecSpecificOctetsPerFrame.length);
              ByteStr.write(mMetadataLength, 0, mMetadataLength.length);
              ByteStr.write(mMetadataContext, 0, mMetadataContext.length);
              ByteStr.write(mNumSubgroups, 0, mNumSubgroups.length);
          }
          return ByteStr.toByteArray();
      }
      private byte [] populate_level2_base() {
          Log.d(TAG,"populate_level2_base, subgroup = " + mNumSubgroups[0]);
          ByteArrayOutputStream ByteStr = new ByteArrayOutputStream();
          byte [] metalength = new byte[1];
          int bisPerGroup = calculateBisPerGroup();//mNumBises/mNumSubGrps;
          byte [] numBises = new byte[1];
          numBises = intTobyteArray(bisPerGroup,1);
          byte [] bisInd = new byte[bisPerGroup];
          if (mNewVersion) {
              byte[] mcid = new byte[1];
              if (new_codec_id) {
                  mcid = longTobyteArray(LC3_CODEC_ID,5);
              } else {
                  mcid = longTobyteArray(LC3_CODEC_ID_OLD,5);
              }
              byte [] metadata = updateMetadata();
              mMetadataLength[0] = (byte)metadata.length;
              int codecConfigLength = 0x13;
              mCodecSpecificLength = intTobyteArray(codecConfigLength, 1);
              for (int i = 0; i < mNumSubgroups[0]; i++) {
                  if (mPartialSimulcast) {
                      if (i < (mNumSubgroups[0] / 2)) {
                      //High quality
                          ByteStr.write(numBises, 0, numBises.length);
                          ByteStr.write(mcid, 0, mcid.length);
                          mCodecSpecificSampleRate = updateSampleRate();
                          mCodecSpecificFrameDuration = updateFrameDuration();
                          mCodecSpecificAudioLocation = updateAudioLocation(0);
                          mCodecSpecificOctetsPerFrame = updateOctetsPerFrame();
                          mCodecSpecificBlocksPerSdu= updateBlocksPerSdu();
                          ByteStr.write(mCodecSpecificLength, 0, mCodecSpecificLength.length);
                          ByteStr.write(mCodecSpecificSampleRate, 0, mCodecSpecificSampleRate.length);
                          ByteStr.write(mCodecSpecificFrameDuration, 0, mCodecSpecificFrameDuration.length);
                          ByteStr.write(mCodecSpecificAudioLocation, 0, mCodecSpecificAudioLocation.length);
                          ByteStr.write(mCodecSpecificOctetsPerFrame, 0, mCodecSpecificOctetsPerFrame.length);
                          ByteStr.write(mCodecSpecificBlocksPerSdu, 0, mCodecSpecificBlocksPerSdu.length);
                          ByteStr.write(mMetadataLength, 0, mMetadataLength.length);
                          ByteStr.write(metadata, 0, metadata.length);
                          byte[] level3 = populate_level3_new_base(i, mcid, mCodecSpecificSampleRate,
                                                                   mCodecSpecificFrameDuration,
                                                                   mCodecSpecificOctetsPerFrame,
                                                                   mCodecSpecificBlocksPerSdu,
                                                                   metadata);
                          ByteStr.write(level3, 0, level3.length);
                          mMetaInfo.put(i,new MetadataLtv(metadata));
                      } else {
                      //Low quality
                          ByteStr.write(numBises, 0, numBises.length);
                          ByteStr.write(mcid, 0, mcid.length);
                          mCodecSpecificSampleRateL2= updateHAPSampleRate();
                          mCodecSpecificFrameDurationL2= updateFrameDuration();
                          mCodecSpecificAudioLocationL2= updateAudioLocation(0);
                          mCodecSpecificOctetsPerFrameL2= updateHapOctetsPerFrame();
                          mCodecSpecificBlocksPerSduL2= updateBlocksPerSdu();
                          ByteStr.write(mCodecSpecificLength, 0, mCodecSpecificLength.length);
                          ByteStr.write(mCodecSpecificSampleRateL2, 0, mCodecSpecificSampleRateL2.length);
                          ByteStr.write(mCodecSpecificFrameDurationL2, 0, mCodecSpecificFrameDurationL2.length);
                          ByteStr.write(mCodecSpecificAudioLocationL2, 0, mCodecSpecificAudioLocationL2.length);
                          ByteStr.write(mCodecSpecificOctetsPerFrameL2, 0, mCodecSpecificOctetsPerFrameL2.length);
                          ByteStr.write(mCodecSpecificBlocksPerSduL2, 0, mCodecSpecificBlocksPerSduL2.length);
                          ByteStr.write(mMetadataLength, 0, mMetadataLength.length);
                          ByteStr.write(metadata, 0, metadata.length);
                          byte[] level3 = populate_level3_new_base(i, mcid, mCodecSpecificSampleRateL2,
                                                                   mCodecSpecificFrameDurationL2,
                                                                   mCodecSpecificOctetsPerFrameL2,
                                                                   mCodecSpecificBlocksPerSduL2,
                                                                   metadata);
                          ByteStr.write(level3, 0, level3.length);
                          mMetaInfo.put(i,new MetadataLtv(metadata));
                      }
                  } else {
                      ByteStr.write(numBises, 0, numBises.length);
                      ByteStr.write(mcid, 0, mcid.length);
                      mCodecSpecificLengthL2 = intTobyteArray(codecConfigLength, 1);
                      mCodecSpecificSampleRateL2 = updateSampleRate();
                      mCodecSpecificFrameDurationL2 = updateFrameDuration();
                      mCodecSpecificAudioLocationL2 = updateAudioLocation(0);
                      mCodecSpecificOctetsPerFrameL2 = updateOctetsPerFrame();
                      mCodecSpecificBlocksPerSduL2 = updateBlocksPerSdu();

                      ByteStr.write(mCodecSpecificLengthL2, 0, mCodecSpecificLengthL2.length);
                      ByteStr.write(mCodecSpecificSampleRateL2, 0, mCodecSpecificSampleRateL2.length);
                      ByteStr.write(mCodecSpecificFrameDurationL2, 0, mCodecSpecificFrameDurationL2.length);
                      ByteStr.write(mCodecSpecificAudioLocationL2, 0, mCodecSpecificAudioLocationL2.length);
                      ByteStr.write(mCodecSpecificOctetsPerFrameL2, 0, mCodecSpecificOctetsPerFrameL2.length);
                      ByteStr.write(mCodecSpecificBlocksPerSduL2, 0, mCodecSpecificBlocksPerSduL2.length);
                      ByteStr.write(mMetadataLength, 0, mMetadataLength.length);
                      ByteStr.write(metadata, 0, metadata.length);
                      byte[] level3 = populate_level3_new_base(0, mcid, mCodecSpecificSampleRateL2,
                                                               mCodecSpecificFrameDurationL2,
                                                               mCodecSpecificOctetsPerFrameL2,
                                                               mCodecSpecificBlocksPerSduL2,
                                                               metadata);
                      ByteStr.write(level3, 0, level3.length);
                      mMetaInfo.put(i,new MetadataLtv(metadata));
                  }
              }
          } else {
              for (int i = 0; i < mNumSubgroups[0]; i++) {
                  if (mPartialSimulcast) {
                      if (i < (mNumSubgroups[0] / 2)) {
                          //High quality
                          byte[] mcid = new byte[1];
                          mcid = intTobyteArray(0xFE,1);
                          mL2CodecSpecificLength = intTobyteArray(0,1);//(byte) 0;
                          ByteStr.write(mcid, 0, mcid.length);
                          ByteStr.write(mL2CodecSpecificLength, 0, mL2CodecSpecificLength.length);
                      } else {
                          //Low quality
                          byte[] mcid = new byte[5];
                          if (new_codec_id) {
                              mcid = longTobyteArray(LC3_CODEC_ID,5);
                          } else {
                              mcid = longTobyteArray(LC3_CODEC_ID_OLD,5);
                          }
                          mCodecSpecificLengthL2 = intTobyteArray(mCodecConfigLength, 1);
                          mCodecSpecificSampleRateL2 = updateHAPSampleRate();
                          mCodecSpecificFrameDurationL2 = updateFrameDuration();
                          mCodecSpecificAudioLocationL2 = updateAudioLocation(0);
                          mCodecSpecificOctetsPerFrameL2 = updateHapOctetsPerFrame();
                          ByteStr.write(mcid, 0, mcid.length);
                          ByteStr.write(mL2CodecSpecificLength, 0, mL2CodecSpecificLength.length);
                          ByteStr.write(mCodecSpecificSampleRateL2, 0, mCodecSpecificSampleRateL2.length);
                          ByteStr.write(mCodecSpecificFrameDurationL2, 0, mCodecSpecificFrameDurationL2.length);
                          ByteStr.write(mCodecSpecificAudioLocationL2, 0, mCodecSpecificAudioLocationL2.length);
                          ByteStr.write(mCodecSpecificOctetsPerFrameL2, 0, mCodecSpecificOctetsPerFrameL2.length);
                      }
                      metalength = intTobyteArray(0, 1);//(byte)0;
                      for (int j = 0; j < bisPerGroup;j++) {
                          bisInd[j] = (byte)(1 + (bisPerGroup * i) + j);
                      }
                      ByteStr.write(metalength, 0, metalength.length);
                      ByteStr.write(numBises, 0, numBises.length);
                      ByteStr.write(bisInd, 0, bisInd.length);
                  } else {
                      byte [] mcid = new byte[1];
                      mcid = intTobyteArray(0xFE,1);//(byte)0xFE;
                      mL2CodecSpecificLength = intTobyteArray(0,1);//(byte)0;
                      metalength = intTobyteArray(0,1);//(byte)0;
                      for (int j = 0; j < bisPerGroup;j++) {
                          bisInd[j] = (byte)(1 + (bisPerGroup * i) + j);
                      }
                      ByteStr.write(mcid, 0, mcid.length);
                      ByteStr.write(mL2CodecSpecificLength, 0, mL2CodecSpecificLength.length);
                      ByteStr.write(metalength, 0, metalength.length);
                      ByteStr.write(numBises, 0, numBises.length);
                      ByteStr.write(bisInd, 0, bisInd.length);
                  }
              }
          }
          return ByteStr.toByteArray();
      }
      private byte[] populate_level3_base() {
          ByteArrayOutputStream ByteStr = new ByteArrayOutputStream();
          for (int i = 0; i < mNumBises; i++) {
              byte[] index = new byte[1];
              byte [] configlength = new byte[1];
              index[0] = (byte)(1 + i); //fetch from mAdvertisingSet
              configlength[0] = (byte)6;
              byte [] config = updateAudioLocation(i+1);
              ByteStr.write(index, 0, index.length);
              ByteStr.write(configlength,0, configlength.length);
              ByteStr.write(config, 0, config.length);
              mBisInfo.add(new BisInfo((int)index[0], mCodecId, mCodecSpecificSampleRate, mCodecSpecificFrameDuration,
                              config, mCodecSpecificOctetsPerFrame, mMetadataContext));
          }
          return ByteStr.toByteArray();
      }
      private byte[] populate_level3_new_base(int subGroupId, byte[] codecId, byte[] SampleRate,
                                               byte[] frameDuration, byte[] octetsPerFrame, byte[] BlocksPerSdu,
                                               byte[] mMetadata) {
          ByteArrayOutputStream ByteStr = new ByteArrayOutputStream();
          int bisPerGroup = calculateBisPerGroup();
          for (int i = 0; i < bisPerGroup; i++) {
              byte[] index = new byte[1];
              byte [] configlength = new byte[1];
              index[0] = (byte)(1 + i + (bisPerGroup * subGroupId));
              configlength[0] = (byte)6;
              byte [] config = updateAudioLocation(i+1);
              ByteStr.write(index, 0, index.length);
              ByteStr.write(configlength,0, configlength.length);
              ByteStr.write(config, 0, config.length);
              mBisInfo.add(new BisInfo((int)index[0], codecId, SampleRate, frameDuration, config,
                           octetsPerFrame, BlocksPerSdu, mMetadata, subGroupId));
          }
          return ByteStr.toByteArray();
      }
    }
    public class BisInfo {
        public int BisIndex;
        public byte [] mCodecId = new byte[5];
        public CodecConfigLtv BisCodecConfig;
        public MetadataLtv BisMetadata;
        public int mSubGroupId;
        public BisInfo(int index, byte[] codecId, byte[] CodecSpecificSampleRate, byte[] CodecSpecificFrameDuration,
                       byte[] CodecSpecificAudioLocation, byte[] CodecSpecificOctetsPerFrame, byte[] metadata) {
            BisIndex = index;
            mCodecId = codecId;
            BisCodecConfig = new CodecConfigLtv(CodecSpecificSampleRate, CodecSpecificFrameDuration,
                                            CodecSpecificAudioLocation, CodecSpecificOctetsPerFrame);
            BisMetadata = new MetadataLtv(metadata);
            mSubGroupId = -1;
        }
        public BisInfo (int index, byte[] codecId, byte[] CodecSpecificSampleRate, byte[] CodecSpecificFrameDuration,
                        byte[] CodecSpecificAudioLocation, byte[] CodecSpecificOctetsPerFrame,
                        byte[] CodecSpecificBlocksPerSdu, byte[] metadata, int subGroupId) {
             BisIndex = index;
             mCodecId = codecId;
             BisCodecConfig = new CodecConfigLtv(CodecSpecificSampleRate, CodecSpecificFrameDuration,
                                             CodecSpecificAudioLocation, CodecSpecificBlocksPerSdu,
                                             CodecSpecificOctetsPerFrame);
             BisMetadata = new MetadataLtv(metadata);
             mSubGroupId = subGroupId;
        }
    }
    public class CodecConfigLtv{
        byte [] mCodecSpecificSampleRate;
        byte [] mCodecSpecificFrameDuration;
        byte [] mCodecSpecificAudioLocation;
        byte [] mCodecSpecificOctetsPerFrame;
        byte [] mCodecSpecificBlocksPerSdu;
        public CodecConfigLtv(byte[] CodecSpecificSampleRate,
                              byte[] CodecSpecificFrameDuration,
                              byte[] CodecSpecificAudioLocation,
                              byte[] CodecSpecificOctetsPerFrame) {
            mCodecSpecificSampleRate = CodecSpecificSampleRate;
            mCodecSpecificFrameDuration = CodecSpecificFrameDuration;
            mCodecSpecificAudioLocation = CodecSpecificAudioLocation;
            mCodecSpecificOctetsPerFrame = CodecSpecificOctetsPerFrame;
        }
        public CodecConfigLtv(byte[] CodecSpecificSampleRate,
                              byte[] CodecSpecificFrameDuration,
                              byte[] CodecSpecificAudioLocation,
                              byte[] CodecSpecificOctetsPerFrame,
                              byte [] CodecSpecificBlocksPerSdu) {
            mCodecSpecificSampleRate = CodecSpecificSampleRate;
            mCodecSpecificFrameDuration = CodecSpecificFrameDuration;
            mCodecSpecificAudioLocation = CodecSpecificAudioLocation;
            mCodecSpecificOctetsPerFrame = CodecSpecificOctetsPerFrame;
            mCodecSpecificBlocksPerSdu = CodecSpecificBlocksPerSdu;
        }
        public byte[] getByteArray() {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try {
                outputStream.write(mCodecSpecificSampleRate);
                outputStream.write(mCodecSpecificFrameDuration);
                outputStream.write(mCodecSpecificAudioLocation);
                outputStream.write(mCodecSpecificOctetsPerFrame);
                if (mNewVersion) {
                    outputStream.write(mCodecSpecificBlocksPerSdu);
                }
            } catch (IOException e) {
                Log.e(TAG, "getBytes: ioexception caught!" + e);
                return null;
            }
            return outputStream.toByteArray( );
        }
    }
    public class MetadataLtv {
        byte[] mRawMetadata;
        public MetadataLtv(byte[] metadata) {
            mRawMetadata = metadata;
        }
        public byte[] getByteArray() {
            return mRawMetadata;
        }
    }
    class BroadcastCodecConfig {
        public BroadcastCodecConfig() {
        //Default configuration
            int sr, ch_mode;
            long codecspecific1;
            switch(mBroadcastConfigSettings) {
                case 1:
                    sr = BluetoothCodecConfig.SAMPLE_RATE_16000;
                    ch_mode = BluetoothCodecConfig.CHANNEL_MODE_MONO;
                    codecspecific1 = 1001;//32kbps
                    break;
                case 2:
                    sr = BluetoothCodecConfig.SAMPLE_RATE_16000;
                    ch_mode = BluetoothCodecConfig.CHANNEL_MODE_STEREO;
                    codecspecific1 = 1001;//32kbps
                    break;
                case 3:
                    sr = BluetoothCodecConfig.SAMPLE_RATE_48000;
                    ch_mode = BluetoothCodecConfig.CHANNEL_MODE_MONO;
                    codecspecific1 = 1004;//80kbps
                    break;
                case 4:
                    sr = BluetoothCodecConfig.SAMPLE_RATE_48000;
                    ch_mode = BluetoothCodecConfig.CHANNEL_MODE_STEREO;
                    codecspecific1 = 1004;//80kbps
                    break;
                case 5:
                    sr = BluetoothCodecConfig.SAMPLE_RATE_48000;
                    ch_mode = BluetoothCodecConfig.CHANNEL_MODE_MONO;
                    codecspecific1 = 1006;//96kbps
                    break;
                case 6:
                    sr = BluetoothCodecConfig.SAMPLE_RATE_48000;
                    ch_mode = BluetoothCodecConfig.CHANNEL_MODE_STEREO;
                    codecspecific1 = 1006;//96
                    break;
                case 7:
                    sr = BluetoothCodecConfig.SAMPLE_RATE_48000;
                    ch_mode = BluetoothCodecConfig.CHANNEL_MODE_MONO;
                    codecspecific1 = 1007;//124
                    break;
                case 8:
                default:
                    sr = BluetoothCodecConfig.SAMPLE_RATE_48000;
                    ch_mode = BluetoothCodecConfig.CHANNEL_MODE_STEREO;
                    codecspecific1 = 1007;
                    break;

            }
            mCodecConfig = new BluetoothCodecConfig(BluetoothCodecConfig.SOURCE_CODEC_TYPE_LC3,
                                                    BluetoothCodecConfig.CODEC_PRIORITY_DEFAULT,
                                                    sr, BluetoothCodecConfig.BITS_PER_SAMPLE_24,
                                                    ch_mode, codecspecific1, 1, 0, 0);
            if (mPartialSimulcast) {
                mHapCodecConfig = new BluetoothCodecConfig(BluetoothCodecConfig.SOURCE_CODEC_TYPE_LC3,
                                                        BluetoothCodecConfig.CODEC_PRIORITY_DEFAULT,
                                                        BluetoothCodecConfig.SAMPLE_RATE_16000,
                                                        BluetoothCodecConfig.BITS_PER_SAMPLE_24,
                                                        BluetoothCodecConfig.CHANNEL_MODE_STEREO,
                                                        1000, 1, 0, 0);

            }
        }
        public void updateBroadcastCodecConfig(BluetoothCodecConfig newConfig) {
            if (DBG) Log.d(TAG, "updateBroadcastCodecConfig: " + newConfig);
            mCodecConfig = newConfig;
            int mChMode = mCodecConfig.getChannelMode();
            switch (mChMode) {
                case BluetoothCodecConfig.CHANNEL_MODE_MONO:
                case BluetoothCodecConfig.CHANNEL_MODE_JOINT_STEREO:
                    mNumBises = 1 * mNumSubGrps;
                    break;
                case BluetoothCodecConfig.CHANNEL_MODE_STEREO:
                    mNumBises = 2 * mNumSubGrps;
                    break;
                default:
                    Log.e(TAG,"channel mode unknown");
            }
        }
    }

    /**
     * Binder object: must be a static class or memory leak may occur.
     */
    @VisibleForTesting
    static class BluetoothBroadcastBinder extends IBluetoothBroadcast.Stub
            implements IProfileServiceBinder {
        private BroadcastService mService;

        private BroadcastService getService() {
            if (!Utils.checkCallerIsSystemOrActiveUser(TAG)) {
                return null;
            }

            if (mService != null && mService.isAvailable()) {
                return mService;
            }
            return null;
        }

        BluetoothBroadcastBinder(BroadcastService svc) {
            mService = svc;
        }

        @Override
        public void cleanup() {
            mService = null;
        }
        @Override
        public boolean SetBroadcast(boolean enable, String packageName, AttributionSource source) {
            BroadcastService service = getService();
            if (service == null || !Utils.checkAdvertisePermissionForDataDelivery(
                                   service, source, TAG)) {
                return false;
            }
            if (enable) {
                return service.EnableBroadcast(packageName);
            }
            else {
                return service.DisableBroadcast(packageName);
            }
            //return false;
        }

        @Override
        public boolean SetEncryption(boolean enable, int enc_len, boolean use_existing,
                         String packageName, AttributionSource source) {
            BroadcastService service = getService();
            if (service == null || !Utils.checkAdvertisePermissionForDataDelivery(
                                   service, source, TAG)) {
                return false;
            }
            return service.SetEncryption(enable, enc_len, use_existing, packageName);
        }

        @Override
        public byte[] GetEncryptionKey(String packageName, AttributionSource source) {
            BroadcastService service = getService();
            if (service == null || !Utils.checkAdvertisePermissionForDataDelivery(
                                   service, source, TAG)) {
                return null;
            }
            return service.GetEncryptionKey(packageName);
        }
        @Override
        public int GetBroadcastStatus(String packageName, AttributionSource source) {
            BroadcastService service = getService();
            if (service == null || !Utils.checkAdvertisePermissionForDataDelivery(
                                   service, source, TAG)) {
                return BluetoothBroadcast.STATE_DISABLED;
            }
            return service.GetBroadcastStatus(packageName);
        }
    }
}
