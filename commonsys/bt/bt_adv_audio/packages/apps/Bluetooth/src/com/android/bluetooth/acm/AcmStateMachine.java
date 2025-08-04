/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
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

import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothCodecConfig;
import android.bluetooth.BluetoothCodecStatus;
import android.bluetooth.BluetoothLeAudioCodecStatus;
import android.bluetooth.BluetoothLeAudioCodecConfig;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import com.android.internal.util.IState;
import com.android.bluetooth.BluetoothStatsLog;
import com.android.bluetooth.btservice.ProfileService;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import java.util.UUID;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Scanner;
import android.os.SystemProperties;
import com.android.bluetooth.btservice.AdapterService;
import java.util.Objects;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import com.android.bluetooth.apm.ApmConst;
import com.android.bluetooth.apm.StreamAudioService;
import com.android.bluetooth.vcp.VcpController;
import android.bluetooth.BluetoothVcp;
import com.android.bluetooth.apm.ActiveDeviceManagerService;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

final class AcmStateMachine extends StateMachine {
    private static final boolean DBG = true;
    private static final String TAG = "AcmStateMachine";
    private String  mReconfig = "";
    public UUID uuid;
    static final int CONNECT = 1;
    static final int DISCONNECT = 2;
    static final int CSIP_CONNECTION_STATE_CHANGED = 3;
    static final int CSIP_LOCK_STATUS_LOCKED = 4;
    static final int CSIP_LOCK_STATUS_PARTIAL_LOCK = 5;
    static final int CSIP_LOCK_STATUS_DENIED = 6;
    static final int CSIP_LOCK_STATUS_RELEASED = 7;
    static final int CODEC_CONFIG_CHANGED = 8;
    static final int GATT_CONNECTION_STATE_CHANGED = 9;
    static final int GATT_CONNECTION_TIMEOUT = 10;
    static final int START_STREAM = 11;
    static final int STOP_STREAM = 12;
    static final int START_STREAM_REQ = 13;
    @VisibleForTesting
    static final int STACK_EVENT = 101;
    private static final int CONNECT_TIMEOUT = 201;
    private static final int CSIP_LOCK_RELEASE_TIMEOUT = 301;
    private static final int CSIP_LOCK_TIMEOUT = 401;
    private static final int LE_AUDIO_AVAILABLE_LICENSED = 0x00000300;
    private static final int QHS_SUPPORT_AVAILABLE = 0x00000800;
    static final int GATT_CONNECTION_TIMEOUT_MS = 30000;
    static final int GATT_PEER_CONN_TIMEOUT = 0x08;
    static final int GATT_CONN_TERMINATED_REMOTE_USER_DISC = 0x13;
    static final int GATT_CONN_TERMINATED_REMOTE_PWR_OFF = 0x15;
    static final int GATT_CONN_FAILED_ESTABLISHED = 0x3E;
    static final int GATT_DISCONNECT_PEER = 22;

    @VisibleForTesting
    static int sConnectTimeoutMs = 30000;        // 30s
    static int sCsipLockReleaseTimeoutMs = 5000;  //TODO
    static int sCsipLockTimeoutMs = 5000;

    private final int VOICE_STREAM_START_RETRY_COUNT = 3;
    private int numRetry = 0;

    private final int ACM_MAX_BYTES = 100;

    private final int CS_PARAM_NUM_BITS  = 8;
    private final int CS_PARAM_IND_MASK  = 0xff;
    private final int CS_PARAM_1ST_INDEX = 0x00;
    private final int CS_PARAM_2ND_INDEX = 0x01;
    private final int CS_PARAM_3RD_INDEX = 0x02;
    private final int CS_PARAM_4TH_INDEX = 0x03;
    private final int CS_PARAM_5TH_INDEX = 0x04;
    private final int CS_PARAM_6TH_INDEX = 0x05;
    private final int CS_PARAM_7TH_INDEX = 0x06;
    private final int CS_PARAM_8TH_INDEX = 0x07;

    private final int CODEC_TYPE_LC3Q    = 0x10;

    private final int APTX_LEA_CODEC     = 0x06;
    private final int APTX_R4_CODEC     = 0x07;

    private final int FWK_SOURCE_CODEC_TYPE_APTX_ADAPTIVE_R4 = 2;
    private final int FWK_SAMPLE_RATE_192000 = 0x400 ;
    private static final String CODEC_NAME = "Codec";
    private static final String STREAM_MAP = "StreamMap";
    private static final String FRAME_DURATION = "FrameDuration";
    private static final String SDU_BLOCK = "Blocks_forSDU";
    private static final String RXCONFIG_INDX = "rxconfig_index";
    private static final String TXCONFIG_INDX = "txconfig_index";
    private static final String VERSION = "version";
    private static final String VENDOR_META_DATA = "vendor";
    private static final String BLE_AUDIO = "bt_ble";
    private static final String BLE_AUDIO_FEATURE_ON = "on";
    private static final String BLE_AUDIO_FEATURE_OFF = "off";
    private Disconnected mDisconnected;
    private Connecting mConnecting;
    private Disconnecting mDisconnecting;
    private Connected mConnected;
    private Streaming mStreaming;
    private int mConnectionState = BluetoothProfile.STATE_DISCONNECTED;
    private int mLastConnectionState = -1;
    private int mMusicConnectionState = BluetoothProfile.STATE_DISCONNECTED;
    private int mLastMusicConnectionState = -1;
    private int mVoiceConnectionState = BluetoothProfile.STATE_DISCONNECTED;
    private int mLastVoiceConnectionState = -1;

    private AcmService mAcmService;

    private AcmNativeInterface mAcmNativeInterface;
    private BluetoothGatt mBluetoothGatt;
    private final BluetoothDevice mDevice;
    private BluetoothDevice mGroupAddress;
    private boolean mIsMusicPlaying = false;
    private boolean mIsVoicePlaying = false;
    private VcpController mVcpController;
    private BluetoothCodecStatus mMusicCodecStatus;
    private BluetoothCodecStatus mVoiceCodecStatus;
    public BluetoothLeAudioCodecStatus mLeAudioCodecStatus;

    @Retention(RetentionPolicy.SOURCE)
    public @interface SourceCodecType {}

    @Retention(RetentionPolicy.SOURCE)
    public @interface CodecPriority {}

    @Retention(RetentionPolicy.SOURCE)
    public @interface ChannelCount {}

    @Retention(RetentionPolicy.SOURCE)
    public @interface FrameDuration {}

    static final int CONTEXT_TYPE_UNKNOWN = 0;
    static final int CONTEXT_TYPE_MUSIC = 1;
    static final int CONTEXT_TYPE_VOICE = 2;
    static final int CONTEXT_TYPE_MUSIC_VOICE = 3;

    static final int CONTENT_TYPE_COVERSATIONAL = 2;
    static final int CONTENT_TYPE_MEDIA = 4;

    private int mContextTypeToDisconnect = CONTEXT_TYPE_UNKNOWN;
    private int mCurrentContextType = CONTEXT_TYPE_UNKNOWN;
    private int mProfileType = 0;
    private int mPreferredContext = CONTEXT_TYPE_UNKNOWN;

    private int mCurrentContextType_temp = CONTEXT_TYPE_UNKNOWN;
    private int mProfileType_temp = 0;
    private int mPreferredContext_temp = CONTEXT_TYPE_UNKNOWN;

    private boolean IsDisconnectRequested = false;
    private boolean IsReconfigRequested = false;
    private int mCsipConnectionState = BluetoothProfile.STATE_DISCONNECTED;
    private boolean mDeviceLocked = false;
    private boolean mCsipLockRequested = false;
    private boolean mIsDeviceWhitelisted = false;
    private boolean mIsBgConnection = false;
    private int mSetId;
    private boolean mAcmMTUChangeRequested = false;
    private boolean mConnUpdateRequested = false;
    private int cached_state;
    private boolean mIsUpdateProfDisConnection = false;
    private boolean mPtsDualConnReq = false;
    private int mDualRetryCount = 0;

    private final Object mBluetoothGattLock = new Object();

    AcmStateMachine(BluetoothDevice device, AcmService acmService,
                     AcmNativeInterface acmNativeInterface, Looper looper) {
        super(TAG, looper);
        setDbg(DBG);
        mDevice = device;
        mAcmService = acmService;
        mAcmNativeInterface = acmNativeInterface;

        mDisconnected = new Disconnected();
        mConnecting = new Connecting();
        mDisconnecting = new Disconnecting();
        mConnected = new Connected();
        mStreaming = new Streaming();

        addState(mDisconnected);
        addState(mConnecting);
        addState(mDisconnecting);
        addState(mConnected);
        addState(mStreaming);

        mCurrentContextType = CONTEXT_TYPE_UNKNOWN;
        mDeviceLocked = false;
        IsDisconnectRequested = false;
        IsReconfigRequested = false;
        mIsDeviceWhitelisted = false;
        setInitialState(mDisconnected);
        mSetId = -1;
        cached_state = -1;
        mAcmMTUChangeRequested = false;
        mIsUpdateProfDisConnection = false;
        if (mAcmService != null) {
          mPtsDualConnReq = mAcmService.mIsPtsDualConnReq;
        } else {
          mPtsDualConnReq = false;
        }

        synchronized (mBluetoothGattLock) {
          if (mBluetoothGatt != null) {
              Log.e(TAG, "disconnect gatt");
              mBluetoothGatt.disconnect();
              mBluetoothGatt.close();
              mBluetoothGatt = null;
          }
        }
    }

    static AcmStateMachine make(BluetoothDevice device, AcmService acmService,
                                  AcmNativeInterface acmNativeInterface, Looper looper) {
        Log.i(TAG, "make acm for device " + device);
        AcmStateMachine acmSm = new AcmStateMachine(device, acmService, acmNativeInterface,
                                                    looper);
        acmSm.start();
        return acmSm;
    }

    public void doQuit() {
        log("doQuit for device " + mDevice);
        if (mIsMusicPlaying) {
            // Stop if audio is still playing
            log("doQuit: stopped Music playing " + mDevice);
            mIsMusicPlaying = false;
            updateStreamState(mDevice, BluetoothA2dp.STATE_NOT_PLAYING, CONTEXT_TYPE_MUSIC);
        }
        if (mIsVoicePlaying) {
            log("doQuit: stopped voice streaming " + mDevice);
            mIsVoicePlaying = false;
            updateStreamState(mDevice, BluetoothA2dp.STATE_NOT_PLAYING, ApmConst.AudioFeatures.CALL_AUDIO);
            if (!mAcmService.isPeerDeviceStreaming(mDevice, mSetId, CONTEXT_TYPE_VOICE)) {
                Log.i(TAG, "doQuit: last peer voice stream stopped " + mDevice);
                setCallAudioOn(false);
                setVoiceParameters(false);
            }
        }
        quitNow();
    }

    public void cleanup() {
        log("cleanup for device " + mDevice);
        synchronized (mBluetoothGattLock) {
          if (mBluetoothGatt != null) {
              Log.e(TAG, "disconnect gatt");
              mBluetoothGatt.disconnect();
              mBluetoothGatt.close();
              mBluetoothGatt = null;
          }
        }
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i(TAG, "onConnectionStateChange: Status: " + status + " newState: " + newState);
            if (status == BluetoothGatt.GATT_SUCCESS  || status == GATT_DISCONNECT_PEER) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    synchronized (mBluetoothGattLock) {
                      if (mBluetoothGatt != null)
                        mBluetoothGatt.requestMtu(ACM_MAX_BYTES);
                    }
                    mAcmMTUChangeRequested = true;
                    cached_state = newState;
                } else {
                    if (mAcmMTUChangeRequested == true) {
                        mAcmMTUChangeRequested = false;
                    }

                    if (cached_state != -1) {
                        cached_state = -1;
                    }

                    Message m = obtainMessage(GATT_CONNECTION_STATE_CHANGED);
                    m.obj = newState;
                    sendMessage(m);
                }
            } else if (status == GATT_PEER_CONN_TIMEOUT ||
                       status == GATT_CONN_FAILED_ESTABLISHED ||
                       status == GATT_CONN_TERMINATED_REMOTE_PWR_OFF ||
                       status == GATT_CONN_TERMINATED_REMOTE_USER_DISC) {
                BluetoothDevice target = gatt.getDevice();
                Log.i(TAG, "[ACM] remote side disconnection, for device add in BG WL: " +target);
                synchronized (mBluetoothGattLock) {
                  if (mBluetoothGatt != null) {
                    mBluetoothGatt.close();
                  }
                  mBluetoothGatt = target.connectGatt(mAcmService, true, mGattCallback,
                          BluetoothDevice.TRANSPORT_LE, false, (BluetoothDevice.PHY_LE_1M_MASK |
                          BluetoothDevice.PHY_LE_2M_MASK | BluetoothDevice.PHY_LE_CODED_MASK),
                          null, true);
                }
                mIsDeviceWhitelisted = true;
            } else {
                synchronized (mBluetoothGattLock) {
                  if (mBluetoothGatt != null) {
                    mBluetoothGatt.close();
                    mBluetoothGatt = null;
                    Log.i(TAG, "[ACM] Failed to connect GATT server.");
                  }
                }
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            log("onMtuChanged: mtu: " + mtu +
                " mAcmMTUChangeRequested: " + mAcmMTUChangeRequested +
                " cached_state: " + cached_state);
            if (mAcmMTUChangeRequested == true) {
                Log.d(TAG, "onMtuChanged: requesting connection update");
                if (!mBluetoothGatt.requestLeConnectionUpdate(24, 24, 0, 1000, 0, 0)) {
                    if (cached_state != -1) {
                        Message m = obtainMessage(GATT_CONNECTION_STATE_CHANGED);
                        m.obj = cached_state;
                        sendMessage(m);
                        mAcmMTUChangeRequested = false;
                        cached_state = -1;
                    }
                }
                mAcmMTUChangeRequested = false;
                mConnUpdateRequested = true;
            } else {
                log("onMtuChanged: Remote initiated trigger");
                //Do nothing
            }
        }

        @Override
        public void  onConnectionUpdated(BluetoothGatt gatt, int interval, int latency, int timeout,
            int status) {
            log("onConnectionUpdated: interval = " + interval);
            if (mConnUpdateRequested) {
                if (cached_state != -1) {
                    Message m = obtainMessage(GATT_CONNECTION_STATE_CHANGED);
                    m.obj = cached_state;
                    sendMessage(m);
                    mConnUpdateRequested = false;
                    cached_state = -1;
                }
            }
        }
    };

    @VisibleForTesting
    class Disconnected extends State {
        @Override
        public void enter() {
            if (ApmConst.getQtiLeAudioEnabled()) {
                //Disconnect unicast VCP 1st
                mVcpController = VcpController.getVcpController();
                if (mVcpController != null) {
                    Log.d(TAG, "Disconnect VCP for " + mDevice);
                    mVcpController.disconnect(mDevice, BluetoothVcp.MODE_UNICAST);
                } else {
                    Log.d(TAG, "mVcpController is null");
                }
            }

            Message currentMessage = getCurrentMessage();
            Log.i(TAG, "Enter Disconnected(" + mDevice + "): " + (currentMessage == null ? "null"
                    : messageWhatToString(currentMessage.what)));
            synchronized (this) {
                mConnectionState = BluetoothProfile.STATE_DISCONNECTED;
                mMusicConnectionState = BluetoothProfile.STATE_DISCONNECTED;
                mVoiceConnectionState = BluetoothProfile.STATE_DISCONNECTED;
            }
            removeDeferredMessages(DISCONNECT);
            removeMessages(CSIP_LOCK_RELEASE_TIMEOUT);
            removeMessages(GATT_CONNECTION_TIMEOUT);
            if (mLastMusicConnectionState != -1) {
                // Don't broadcast during startup
                if (mIsMusicPlaying) {
                    Log.i(TAG, "Disconnected: stop music streaming: " + mDevice);
                    mIsMusicPlaying = false;
                    updateStreamState(mDevice, BluetoothA2dp.STATE_NOT_PLAYING, CONTEXT_TYPE_MUSIC);
                }
                if (mLastVoiceConnectionState != -1) {
                    if (mIsVoicePlaying) {
                        Log.i(TAG, "Disconnected: stop voice streaming: " + mDevice);
                        mIsVoicePlaying = false;
                        updateStreamState(mDevice, BluetoothA2dp.STATE_NOT_PLAYING, ApmConst.AudioFeatures.CALL_AUDIO);
                        if (!mAcmService.isPeerDeviceStreaming(mDevice, mSetId, CONTEXT_TYPE_VOICE)) {
                            Log.i(TAG, "Disconnected: last peer voice stream stopped " + mDevice);
                            setCallAudioOn(false);
                            setVoiceParameters(false);
                        }
                    }
                }
                // ensure when one device is disconnected, need to update the channel mode.
                mAcmService.updateLeaChannelMode(BluetoothA2dp.STATE_NOT_PLAYING, mDevice);
                //Unlock, device state changed from connected to disconnected

                Log.i(TAG, " mIsUpdateProfDisConnection: " + mIsUpdateProfDisConnection);
                if (mDevice.getBondState() == BluetoothDevice.BOND_NONE ||
                    mIsUpdateProfDisConnection) {
                    Log.i(TAG, "Device is unpaired/mIsUpdateProfDisConnection has been set," +
                                           " update disconnected to APM for " + mDevice);
                    mIsUpdateProfDisConnection = false;
                    if (mAcmService.isPeerDeviceConnectedForContext(mDevice, mSetId, CONTEXT_TYPE_MUSIC)) {
                        Log.d(TAG, "Fellow device media is already connected, not last member ");
                        updateConnectionState(mDevice, mMusicConnectionState, CONTEXT_TYPE_MUSIC, false);
                    } else {
                        Log.d(TAG, "Last member to disconnect media");
                        updateConnectionState(mDevice, mMusicConnectionState, CONTEXT_TYPE_MUSIC, true);
                    }
                    if (mAcmService.isPeerDeviceConnectedForContext(mDevice, mSetId, CONTEXT_TYPE_VOICE)) {
                        Log.d(TAG, "Fellow device voice is already connected, not last member ");
                        if (mLastVoiceConnectionState != -1)
                            updateConnectionState(mDevice, mVoiceConnectionState, ApmConst.AudioFeatures.CALL_AUDIO, false);
                    } else {
                        Log.d(TAG, "Last member to disconnect voice");
                        if (mLastVoiceConnectionState != -1)
                            updateConnectionState(mDevice, mVoiceConnectionState, ApmConst.AudioFeatures.CALL_AUDIO, true);
                    }
                    mDeviceLocked = false;
                    //assuming disconnected state we are moving hence update AcmDevice hash map
                    mAcmService.handleAcmDeviceStateChange(mDevice, mConnectionState, mSetId);
                } else if (mDeviceLocked) {
                    Log.i(TAG, "Access RELEASED for device " + mDevice);
                    List<BluetoothDevice> members = new ArrayList<BluetoothDevice>();
                    members.add(mDevice);
                    mAcmService.getCsipManager().setLock(mSetId, members, mAcmService.getCsipManager().UNLOCK);
                    mDeviceLocked = false;
                } else if (mCsipConnectionState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i(TAG, "Device access is already RELEASED, Go for DeviceGroup Disconnect " + mDevice);
                    mAcmService.getCsipManager().disconnectCsip(mDevice);
                } else {
                    synchronized (mBluetoothGattLock) {
                      if (mBluetoothGatt != null && !mIsDeviceWhitelisted) {
                          if (mAcmService.isPeerDeviceConnected(mDevice,mSetId)) {
                              //add to whitelist
                              Log.d(TAG, "Fellow device connected, add to BG WL");
                              addDeviceToBgWL();
                          } else {
                              Log.d(TAG, "remove other devices from BG WL");
                              mAcmService.removePeersFromBgWl(mDevice, mSetId);
                              Log.i(TAG, "Go for GATT Disconnect " + mDevice);
                              mBluetoothGatt.disconnect();
                          }
                      } else {
                          Log.d(TAG, "mBluetoothGatt is NULL or device is in BG WL ");
                          if (mAcmService.isPeerDeviceConnectedForContext(mDevice, mSetId, CONTEXT_TYPE_MUSIC)) {
                              Log.d(TAG, "Fellow device media is already connected, not last member ");
                              updateConnectionState(mDevice, mMusicConnectionState, CONTEXT_TYPE_MUSIC, false);
                          } else {
                              Log.d(TAG, "Last member to disconnect media");
                              updateConnectionState(mDevice, mMusicConnectionState, CONTEXT_TYPE_MUSIC, true);
                          }
                          if (mAcmService.isPeerDeviceConnectedForContext(mDevice, mSetId, CONTEXT_TYPE_VOICE)) {
                              Log.d(TAG, "Fellow device voice is already connected, not last member ");
                              if (mLastVoiceConnectionState != -1)
                                  updateConnectionState(mDevice, mVoiceConnectionState, ApmConst.AudioFeatures.CALL_AUDIO, false);
                          } else {
                              Log.d(TAG, "Last member to disconnect voice");
                              if (mLastVoiceConnectionState != -1)
                                  updateConnectionState(mDevice, mVoiceConnectionState, ApmConst.AudioFeatures.CALL_AUDIO, true);
                          }
                          if (!mAcmService.isPeerDeviceConnected(mDevice, mSetId)) {
                              Log.d(TAG, "Last member to disconnect");
                              if (!mIsDeviceWhitelisted) {
                                  mAcmService.removePeersFromBgWl(mDevice, mSetId);
                                  Log.d(TAG, "remove other devices from BG WL");
                              }
                          }
                          //assuming disconnected state we are moving hence update AcmDevice hash map
                          mAcmService.handleAcmDeviceStateChange(mDevice, mConnectionState, mSetId);
                      }
                    }
                }
            }
        }

        @Override
        public void exit() {
            Message currentMessage = getCurrentMessage();
            log("Exit Disconnected(" + mDevice + "): " + (currentMessage == null ? "null"
                    : messageWhatToString(currentMessage.what)));
            mConnectionState = BluetoothProfile.STATE_DISCONNECTED;
            mLastMusicConnectionState = BluetoothProfile.STATE_DISCONNECTED;
            mLastVoiceConnectionState = BluetoothProfile.STATE_DISCONNECTED;
        }

        @Override
        public boolean processMessage(Message message) {
            log("Disconnected process message(" + mDevice + "): "
                    + messageWhatToString(message.what));

            switch (message.what) {
                case CONNECT: {
                    mCurrentContextType = message.arg1;
                    mProfileType = message.arg2;
                    mPreferredContext = (int)message.obj;
                    Log.d(TAG, "mPtsDualConnReq: " + mPtsDualConnReq);
                    if (mPtsDualConnReq) {
                       mCurrentContextType_temp = mCurrentContextType;
                       mProfileType_temp = mProfileType;
                       mPreferredContext_temp = mPreferredContext;
                    }
                    Log.i(TAG, "Connecting " + contextTypeToString(mCurrentContextType) + " to " + mDevice);
                    if (mAcmService.IsLockSupportAvailable(mDevice)) {
                        Log.d(TAG, "Exclusive Access support available, go for GATT connect");
                        //if lock support available then go for CSIP connect
                        synchronized (mBluetoothGattLock) {
                          if (mBluetoothGatt != null) {
                              mBluetoothGatt.close();
                          }
                          mBluetoothGatt = mDevice.connectGatt(mAcmService, false, mGattCallback,
                                  BluetoothDevice.TRANSPORT_LE, false, (BluetoothDevice.PHY_LE_1M_MASK |
                                  BluetoothDevice.PHY_LE_2M_MASK | BluetoothDevice.PHY_LE_CODED_MASK),
                                  null, true);
                        }

                        sendMessageDelayed(GATT_CONNECTION_TIMEOUT, GATT_CONNECTION_TIMEOUT_MS);
                        transitionTo(mConnecting);
                        break;
                    } else {
                        Log.d(TAG, "Exclusive Access support not available, go for GATT connect");
                        if (!mIsBgConnection) {
                            synchronized (mBluetoothGattLock) {
                              if (mBluetoothGatt != null) {
                                  mBluetoothGatt.close();
                              }
                              mBluetoothGatt = mDevice.connectGatt(mAcmService, false, mGattCallback,
                                      BluetoothDevice.TRANSPORT_LE, false, (BluetoothDevice.PHY_LE_1M_MASK |
                                      BluetoothDevice.PHY_LE_2M_MASK | BluetoothDevice.PHY_LE_CODED_MASK),
                                      null, true);
                            }

                            sendMessageDelayed(GATT_CONNECTION_TIMEOUT, GATT_CONNECTION_TIMEOUT_MS);
                        } else {
                            mIsBgConnection = false;
                            Log.d(TAG, "Incoming connection due to BG WL, resetting incoming connection flag");
                            if (!mAcmNativeInterface.connectAcm(mDevice, message.arg1, message.arg2, (int)message.obj)) {
                                Log.e(TAG, "Disconnected: error connecting to " + mDevice);
                                break;
                            }
                        }
                        transitionTo(mConnecting);
                        /*if (!mAcmNativeInterface.connectAcm(mDevice, message.arg1, message.arg2, (int)message.obj)) {
                            Log.e(TAG, "Disconnected: error connecting to " + mDevice);
                            break;
                        }
                        transitionTo(mConnecting);*/
                    }
                } break;

                case GATT_CONNECTION_STATE_CHANGED: {
                    removeMessages(GATT_CONNECTION_TIMEOUT);
                    int st = (int)message.obj;
                    if (st == BluetoothProfile.STATE_DISCONNECTED) {
                        Log.d(TAG, "GATT Disconnected");
                        mIsDeviceWhitelisted = false;
                        if (mCurrentContextType == CONTEXT_TYPE_MUSIC) {
                          if (mAcmService.isPeerDeviceConnectedForContext(mDevice, mSetId, CONTEXT_TYPE_MUSIC)) {
                              Log.d(TAG, "Fellow device is already connected for media, not last member ");
                              updateConnectionState(mDevice, mMusicConnectionState, CONTEXT_TYPE_MUSIC, false);
                          } else {
                              Log.d(TAG, "Last member to disconnect MEDIA");
                              updateConnectionState(mDevice, mMusicConnectionState, CONTEXT_TYPE_MUSIC, true);
                          }
                        } else if (mCurrentContextType == CONTEXT_TYPE_VOICE) {
                          if (mAcmService.isPeerDeviceConnectedForContext(mDevice, mSetId, CONTEXT_TYPE_VOICE)) {
                              Log.d(TAG, "Fellow device is already connected for voice, not last member ");
                              updateConnectionState(mDevice, mVoiceConnectionState, ApmConst.AudioFeatures.CALL_AUDIO, false);
                          } else {
                              Log.d(TAG, "Last member to disconnect VOICE");
                              updateConnectionState(mDevice, mVoiceConnectionState, ApmConst.AudioFeatures.CALL_AUDIO, true);
                          }
                        } else if (mCurrentContextType == CONTEXT_TYPE_MUSIC_VOICE) {
                          if (mAcmService.isPeerDeviceConnectedForContext(mDevice, mSetId, CONTEXT_TYPE_MUSIC)) {
                              Log.d(TAG, "Fellow device is already connected for media, not last member ");
                              updateConnectionState(mDevice, mMusicConnectionState, CONTEXT_TYPE_MUSIC, false);
                          } else {
                              Log.d(TAG, "Last member to disconnect MEDIA");
                              updateConnectionState(mDevice, mMusicConnectionState, CONTEXT_TYPE_MUSIC, true);
                          }
                          if (mAcmService.isPeerDeviceConnectedForContext(mDevice, mSetId, CONTEXT_TYPE_VOICE)) {
                              Log.d(TAG, "Fellow device is already connected for voice, not last member ");
                              updateConnectionState(mDevice, mVoiceConnectionState, ApmConst.AudioFeatures.CALL_AUDIO, false);
                          } else {
                              Log.d(TAG, "Last member to disconnect VOICE");
                              updateConnectionState(mDevice, mVoiceConnectionState, ApmConst.AudioFeatures.CALL_AUDIO, true);
                          }
                        }
                        //assuming disconneted state we are moving hence update AcmDevice hash map
                        mAcmService.handleAcmDeviceStateChange(mDevice, BluetoothProfile.STATE_DISCONNECTED, mSetId);
                        synchronized (mBluetoothGattLock) {
                          if (mBluetoothGatt != null) {
                              mBluetoothGatt.close();
                          }
                        }
                    } else if (st == BluetoothProfile.STATE_CONNECTED) {
                        mIsDeviceWhitelisted = false;
                        mIsBgConnection = true;
                        Log.d(TAG, "GATT connected from background, go for profile connection");
                        AdapterService mAdapterService = AdapterService.getAdapterService();
                        if (mAdapterService != null && !mAdapterService.connectAllEnabledProfiles(mDevice)) {
                            Log.e(TAG, "Disconnected: error connecting to " + mDevice);
                            break;
                        }
                        break;
                    }
                } break;

                case DISCONNECT:
                    Log.w(TAG, "Disconnected: DISCONNECT ignored: " + mDevice);
                    break;

                case GATT_CONNECTION_TIMEOUT: {
                    Log.d(TAG, "GATT connection Timeout");
                    break;
                }

                case CSIP_CONNECTION_STATE_CHANGED:
                    int state = (int)message.obj;
                    if (state == BluetoothProfile.STATE_DISCONNECTED)
                        mCsipConnectionState = BluetoothProfile.STATE_DISCONNECTED;
                    synchronized (mBluetoothGattLock) {
                      if (mBluetoothGatt != null)
                          mBluetoothGatt.disconnect();
                    }
                    break;

                case CSIP_LOCK_STATUS_RELEASED: {
                    removeMessages(CSIP_LOCK_RELEASE_TIMEOUT);
                    //disconnect CSIP
                    int value = (int)message.arg1;
                    Log.d(TAG, "Exclusive Access state changed:" + value);
                    if (value == mAcmService.getCsipManager().UNLOCK) {
                        if (mCsipConnectionState == BluetoothProfile.STATE_CONNECTED) {
                           mAcmService.getCsipManager().disconnectCsip(mDevice);
                        }
                    }
                    mCsipLockRequested = false;
                    mDeviceLocked = false;
                    break;
                }

                case STACK_EVENT:
                    AcmStackEvent event = (AcmStackEvent) message.obj;
                    log("Disconnected: stack event: " + event);
                    if (!mDevice.equals(event.device)) {
                        Log.wtf(TAG, "Device(" + mDevice + "): event mismatch: " + event);
                    }
                    switch (event.type) {
                        case AcmStackEvent.EVENT_TYPE_CONNECTION_STATE_CHANGED:
                            processConnectionEvent(event.valueInt1, event.valueInt2);
                            break;
                        case AcmStackEvent.EVENT_TYPE_CODEC_CONFIG_CHANGED:
                            processCodecConfigEvent(event.codecStatus, event.valueInt2);
                            break;
                        default:
                            Log.e(TAG, "Disconnected: ignoring stack event: " + event);
                            break;
                    }
                    break;
                default:
                    return NOT_HANDLED;
            }
            return HANDLED;
        }

        // in Disconnected state
        private void processConnectionEvent(int state, int contextType) {
            switch (state) {
                case AcmStackEvent.CONNECTION_STATE_DISCONNECTED:
                    Log.w(TAG, "Ignore ACM DISCONNECTED event: " + mDevice);
                    break;
                case AcmStackEvent.CONNECTION_STATE_CONNECTING:{
                        // Reject the connection and stay in Disconnected state itself
                        Log.w(TAG, "Incoming A2DP Connecting request rejected: " + mDevice);
                        mAcmNativeInterface.disconnectAcm(mDevice, contextType);
                    }
                    break;
                case AcmStackEvent.CONNECTION_STATE_CONNECTED: {//Shouldn't come
                    Log.w(TAG, "ACM Connected from Disconnected state: " + mDevice);
                    // Reject the connection and stay in Disconnected state itself
                    Log.w(TAG, "Incoming ACM Connected request rejected: " + mDevice);
                    mAcmNativeInterface.disconnectAcm(mDevice, contextType);
                    }
                    break;
                case AcmStackEvent.CONNECTION_STATE_DISCONNECTING:
                    Log.w(TAG, "Ignore ACM DISCONNECTING event: " + mDevice);
                    break;
                default:
                    Log.e(TAG, "Incorrect state: " + state + " device: " + mDevice);
                    break;
            }
        }
    }

    @VisibleForTesting
    class Connecting extends State {
        @Override
        public void enter() {
            if (ApmConst.getQtiLeAudioEnabled()) {
                //Connect unicast VCP 1st
                mVcpController = VcpController.getVcpController();
                if (mVcpController != null) {
                    Log.d(TAG, "Connect VCP for " + mDevice);
                    mVcpController.connect(mDevice, BluetoothVcp.MODE_UNICAST);
                } else {
                    Log.d(TAG, "mVcpController is null");
                }
            }

            Message currentMessage = getCurrentMessage();
            Log.i(TAG, "Enter Connecting(" + mDevice + "): " + (currentMessage == null ? "null"
                    : messageWhatToString(currentMessage.what)));
            Log.d(TAG, "mPtsDualConnReq: " + mPtsDualConnReq);
            if ((mPtsDualConnReq) && (mDualRetryCount == 0)) {
                Log.d(TAG, "sending dummy connect message");
                Message msg =  obtainMessage(CONNECT);
                msg.arg1 = mCurrentContextType_temp;
                msg.arg2 = mProfileType_temp;
                msg.obj = mPreferredContext_temp;
                sendMessageDelayed(msg, 10000);
                mDualRetryCount = 1;
            } else {
               sendMessageDelayed(CONNECT_TIMEOUT, sConnectTimeoutMs);
            }
            //this context type here is global context type, if not based on current
            //and prev states per context, form this contextType here
            synchronized (this) {
                mConnectionState = BluetoothProfile.STATE_CONNECTING;
            }
            mSetId = mAcmService.getCsipManager().getCsipSetId(mDevice, null /*ACM_UUID*/); //TODO: UUID what to set ?
            mGroupAddress = mAcmService.getGroup(mDevice);
            Log.d(TAG, "Group bd address  " + mGroupAddress);
            mAcmService.handleAcmDeviceStateChange(mDevice, mConnectionState, mSetId);
            if (mCurrentContextType == CONTEXT_TYPE_MUSIC) {
              mMusicConnectionState = BluetoothProfile.STATE_CONNECTING;
              if (mAcmService.isPeerDeviceConnectedForContext(mDevice, mSetId, CONTEXT_TYPE_MUSIC)) {
                  Log.d(TAG, "Fellow device is already connected for media, not first member ");
                  updateConnectionState(mDevice, mMusicConnectionState, CONTEXT_TYPE_MUSIC, false);
              } else {
                  Log.d(TAG, "First member of group to connect MUSIC");
                  updateConnectionState(mDevice, mMusicConnectionState, CONTEXT_TYPE_MUSIC, true);
              }
            } else if (mCurrentContextType == CONTEXT_TYPE_VOICE) {
              mVoiceConnectionState = BluetoothProfile.STATE_CONNECTING;
              if (mAcmService.isPeerDeviceConnectedForContext(mDevice, mSetId, CONTEXT_TYPE_VOICE)) {
                  Log.d(TAG, "Fellow device is already connected for voice, not first member ");
                  updateConnectionState(mDevice, mVoiceConnectionState, ApmConst.AudioFeatures.CALL_AUDIO, false);
              } else {
                  Log.d(TAG, "First member of group to connect VOICE");
                  updateConnectionState(mDevice, mVoiceConnectionState, ApmConst.AudioFeatures.CALL_AUDIO, true);
              }
            } else if (mCurrentContextType == CONTEXT_TYPE_MUSIC_VOICE) {
              mMusicConnectionState = BluetoothProfile.STATE_CONNECTING;
              mVoiceConnectionState = BluetoothProfile.STATE_CONNECTING;
              if (mAcmService.isPeerDeviceConnectedForContext(mDevice, mSetId, CONTEXT_TYPE_MUSIC)) {
                  Log.d(TAG, "Fellow device is already connected for media, not first member ");
                  updateConnectionState(mDevice, mMusicConnectionState, CONTEXT_TYPE_MUSIC, false);
              } else {
                  Log.d(TAG, "First member of group to connect MUSIC");
                  updateConnectionState(mDevice, mMusicConnectionState, CONTEXT_TYPE_MUSIC, true);
              }
              if (mAcmService.isPeerDeviceConnectedForContext(mDevice, mSetId, CONTEXT_TYPE_VOICE)) {
                  Log.d(TAG, "Fellow device is already connected for voice, not first member ");
                  updateConnectionState(mDevice, mVoiceConnectionState, ApmConst.AudioFeatures.CALL_AUDIO, false);
              } else {
                  Log.d(TAG, "First member of group to connect VOICE");
                  updateConnectionState(mDevice, mVoiceConnectionState, ApmConst.AudioFeatures.CALL_AUDIO, true);
              }
            }
        }

        @Override
        public void exit() {
            Message currentMessage = getCurrentMessage();
            log("Exit Connecting(" + mDevice + "): " + (currentMessage == null ? "null"
                    : messageWhatToString(currentMessage.what)));
            if (mCurrentContextType == CONTEXT_TYPE_MUSIC) {
              mLastMusicConnectionState = BluetoothProfile.STATE_CONNECTING;
            } else if (mCurrentContextType == CONTEXT_TYPE_VOICE) {
              mLastVoiceConnectionState = BluetoothProfile.STATE_CONNECTING;
            } else if (mCurrentContextType == CONTEXT_TYPE_MUSIC_VOICE) {
              mLastMusicConnectionState = BluetoothProfile.STATE_CONNECTING;
              mLastVoiceConnectionState = BluetoothProfile.STATE_CONNECTING;
            }
            removeMessages(CONNECT_TIMEOUT);
        }

        @Override
        public boolean processMessage(Message message) {
            log("Connecting process message(" + mDevice + "): "
                    + messageWhatToString(message.what));

            switch (message.what) {
                case CONNECT:
                    deferMessage(message); // Do we need to defer this ? It shouldn't have come.
                    break;
                case CONNECT_TIMEOUT: {
                    Log.w(TAG, "Connecting connection timeout: " + mDevice);
                    //check if CSIP is connected
                    if (mCsipConnectionState == BluetoothProfile.STATE_CONNECTED)
                        mAcmService.getCsipManager().disconnectCsip(mDevice);
                    mAcmNativeInterface.disconnectAcm(mDevice, mCurrentContextType);
                    AcmStackEvent event =
                            new AcmStackEvent(AcmStackEvent.EVENT_TYPE_CONNECTION_STATE_CHANGED);
                    event.device = mDevice;
                    event.valueInt1 = AcmStackEvent.CONNECTION_STATE_DISCONNECTED;
                    event.valueInt2 = mCurrentContextType;
                    sendMessage(STACK_EVENT, event);
                    break;
                }
                case DISCONNECT: {
                    deferMessage(message);
                    break;
                }

                case GATT_CONNECTION_STATE_CHANGED: {
                    removeMessages(GATT_CONNECTION_TIMEOUT);
                    int st = (int)message.obj;
                    if (st == BluetoothProfile.STATE_CONNECTED) {
                        mIsDeviceWhitelisted = false;
                        Log.d(TAG, "GATT connected, go for connect");
                        if (!mAcmNativeInterface.connectAcm(mDevice, mCurrentContextType, mProfileType, mPreferredContext)) {
                            Log.e(TAG, "Disconnected: error connecting to " + mDevice);
                            break;
                        }
                    } else if (st == BluetoothProfile.STATE_DISCONNECTED) {
                        Log.d(TAG, "GATT Disconnected");
                        mIsDeviceWhitelisted = false;
                        updateConnectionState(mDevice, mMusicConnectionState, CONTEXT_TYPE_MUSIC, false);
                        updateConnectionState(mDevice, mVoiceConnectionState, ApmConst.AudioFeatures.CALL_AUDIO, false);
                        transitionTo(mDisconnected);
                    }
                } break;

                case GATT_CONNECTION_TIMEOUT: {
                    Log.d(TAG, "GATT connection Timeout");
                    if (mAcmService.isPeerDeviceConnected(mDevice, mSetId)) {
                        Log.d(TAG, "Peer device is connected, add to BG WL");
                        mIsDeviceWhitelisted = true;
                        synchronized (mBluetoothGattLock) {
                          if (mBluetoothGatt != null) {
                              mBluetoothGatt.close();
                          }

                          mBluetoothGatt = mDevice.connectGatt(mAcmService, true, mGattCallback,
                                  BluetoothDevice.TRANSPORT_LE, false, (BluetoothDevice.PHY_LE_1M_MASK |
                                  BluetoothDevice.PHY_LE_2M_MASK | BluetoothDevice.PHY_LE_CODED_MASK),
                                  null, true);
                        }

                    } else {
                        Log.d(TAG, "No member is in connected state, do not add in BG WL");
                        mIsDeviceWhitelisted = false;
                        synchronized (mBluetoothGattLock) {
                          if (mBluetoothGatt != null) {
                              Log.e(TAG, "Disconnect gatt and make gatt instance null.");
                              mBluetoothGatt.disconnect();
                              mBluetoothGatt.close();
                              mBluetoothGatt = null;
                          }
                        }
                    }
                    removeDeferredMessages(CONNECT);
                    transitionTo(mDisconnected);
                    break;
                }

                case CSIP_CONNECTION_STATE_CHANGED: {
                    int state = (int)message.obj;
                    if (state == BluetoothProfile.STATE_CONNECTED) {
                        Log.e(TAG, "DeviceGroup Connected to  " + mDevice);
                        mCsipConnectionState = BluetoothProfile.STATE_CONNECTED;
                        mSetId = mAcmService.getCsipManager().getCsipSetId(mDevice, null /*ACM_UUID*/); //TODO: UUID what to set ?
                        Iterator<BluetoothDevice> i = mAcmService.getCsipManager().getSetMembers(mSetId).iterator();
                        List<BluetoothDevice> members = new ArrayList<BluetoothDevice>();
                        if (i != null) {
                            while (i.hasNext()) {
                                BluetoothDevice device = i.next();
                                if (mAcmService.getCsipConnectionState(device) == BluetoothProfile.STATE_CONNECTED) {
                                    members.add(device);
                                }
                            }
                        }
                        //setlockvalue takes device list
                        mCsipLockRequested = true;
                        mAcmService.getCsipManager().setLock(mSetId, members, mAcmService.getCsipManager().LOCK);
                    } else {
                        Log.e(TAG, "DeviceGroup Connection failed to  " + mDevice);
                        mCsipConnectionState = BluetoothProfile.STATE_DISCONNECTED;
                        mCsipLockRequested = false;
                        mDeviceLocked = false;
                        transitionTo(mDisconnected);
                    }
                    break;
                }

                case CSIP_LOCK_STATUS_LOCKED: {
                    mCsipLockRequested = false;
                    int value = (int)message.arg1;
                    Log.d(TAG, "Exclusive Access state changed:" + value);
                    int setId = (int)message.obj;
                    int st = mAcmService.getCsipConnectionState(mDevice);
                    if ((mDeviceLocked && st == BluetoothProfile.STATE_CONNECTED)) {
                        Log.w(TAG, "Device access is already granted and DeviceGroup is in connected state");
                        break;
                    }
                    if (value == mAcmService.getCsipManager().LOCK) {
                        mDeviceLocked = true;
                        if (!mAcmNativeInterface.connectAcm(mDevice, mCurrentContextType, mProfileType, mPreferredContext)) {
                            Log.e(TAG, "Disconnected: error connecting to " + mDevice);
                            mAcmService.getCsipManager().disconnectCsip(mDevice);
                            transitionTo(mDisconnected);
                            break;
                        }
                    } else {
                        mDeviceLocked = false;
                        Log.w(TAG, "Exclusive Access failed to " + setId);
                        transitionTo(mDisconnected);
                    }
                    break;
                }

                case CSIP_LOCK_STATUS_PARTIAL_LOCK: {
                    //check if requested lock is from this device
                    if (mCsipLockRequested) {
                        int value = (int)message.arg1;
                        Log.d(TAG, "Exclusive Access state changed:" + value);
                        int setId = (int)message.obj;
                        int st = mAcmService.getCsipConnectionState(mDevice);
                        if (mDeviceLocked && st == BluetoothProfile.STATE_CONNECTED) {
                            Log.w(TAG, "Device access is already granted and DeviceGroup is in connected state");
                            break;
                        }
                        if (value == mAcmService.getCsipManager().LOCK) {
                            mDeviceLocked = true;
                            if (!mAcmNativeInterface.connectAcm(mDevice, mCurrentContextType, mProfileType, mPreferredContext)) {
                                Log.e(TAG, "Disconnected: error connecting to " + mDevice);
                                mAcmService.getCsipManager().disconnectCsip(mDevice);
                                transitionTo(mDisconnected);
                                break;
                            }
                        } else {
                            mDeviceLocked = false;
                            Log.w(TAG, "Exclusive Access failed to " + setId);
                            transitionTo(mDisconnected);
                        }
                        mCsipLockRequested = false;
                    } else {
                        //lock is not requested from this device TODO: check if
                        Log.d(TAG, "Exclusive Access is not requested from this device: " + mDevice);
                        mDeviceLocked = true;
                    }
                }
                break;

                case CSIP_LOCK_RELEASE_TIMEOUT:
                    //TODO: lock release individual ?
                    Log.d(TAG, "Exclusive Access timeout to " + mDevice);
                    List<BluetoothDevice> set = new ArrayList<BluetoothDevice>();
                    set.add(mDevice);
                    mAcmService.getCsipManager().setLock(mSetId, set, mAcmService.getCsipManager().UNLOCK);
                    mDeviceLocked = false;
                    break;

                case CSIP_LOCK_STATUS_DENIED:
                    //lock denied fail connection
                    Log.e(TAG, "DeviceGroup Connection failed to  " + mDevice);
                    mCsipLockRequested = false;
                    mDeviceLocked = false;
                    break;

                case CSIP_LOCK_STATUS_RELEASED:
                    removeMessages(CSIP_LOCK_RELEASE_TIMEOUT);
                    mCsipLockRequested = false;
                    mDeviceLocked = false;
                    break;

                case STACK_EVENT:
                    AcmStackEvent event = (AcmStackEvent) message.obj;
                    log("Connecting: stack event: " + event);
                    if (!mDevice.equals(event.device)) {
                        Log.wtf(TAG, "Device(" + mDevice + "): event mismatch: " + event);
                    }
                    switch (event.type) {
                        case AcmStackEvent.EVENT_TYPE_CONNECTION_STATE_CHANGED:
                            processConnectionEvent(event.valueInt1, event.valueInt2);
                            break;
                        case AcmStackEvent.EVENT_TYPE_CODEC_CONFIG_CHANGED:
                            processCodecConfigEvent(event.codecStatus, event.valueInt2);
                            break;
                        case AcmStackEvent.EVENT_TYPE_AUDIO_STATE_CHANGED:
                            break;
                        default:
                            Log.e(TAG, "Connecting: ignoring stack event: " + event);
                            break;
                    }
                    break;
                default:
                    return NOT_HANDLED;
            }
            return HANDLED;
        }

        // in Connecting state
        private void processConnectionEvent(int state, int contextType) {
            IState smState;
            switch (state) {
                case AcmStackEvent.CONNECTION_STATE_DISCONNECTED: {
                    smState = mDisconnected;
                    Log.w(TAG, "Connecting device disconnected: " + mDevice);
                    mCurrentContextType &= ~contextType;
                    Log.i(TAG, "mCurrentContextType now is " + contextTypeToString(mCurrentContextType));
                    if ((mMusicConnectionState == BluetoothProfile.STATE_CONNECTING) &&
                        (mVoiceConnectionState == BluetoothProfile.STATE_CONNECTING)) {
                        if (contextType != CONTEXT_TYPE_MUSIC_VOICE) {
                            /*only 1/2 contexts are being disconnected, remain in connecting state but broadcast the connection state*/
                            smState = mConnecting;
                        }
                    }
                    processTransitionContextState(mConnecting, mDisconnected, contextType);
                    if (contextType == CONTEXT_TYPE_MUSIC) {
                        if (mAcmService.isPeerDeviceConnectedForContext(mDevice, mSetId, CONTEXT_TYPE_MUSIC)) {
                            Log.d(TAG, "Fellow device is already connected, update MUSIC");
                            updateConnectionState(mDevice, mMusicConnectionState, CONTEXT_TYPE_MUSIC, false);
                        } else {
                            Log.d(TAG, "First member of group to disconnect MUSIC");
                            updateConnectionState(mDevice, mMusicConnectionState, CONTEXT_TYPE_MUSIC, true);
                        }
                    } else if (contextType == CONTEXT_TYPE_VOICE) {
                        if (mAcmService.isPeerDeviceConnectedForContext(mDevice, mSetId, CONTEXT_TYPE_VOICE)) {
                            Log.d(TAG, "Fellow device is already connected, update VOICE");
                            updateConnectionState(mDevice, mVoiceConnectionState, ApmConst.AudioFeatures.CALL_AUDIO, false);
                        } else {
                            Log.d(TAG, "First member of group to disconnect VOICE");
                            updateConnectionState(mDevice, mVoiceConnectionState, ApmConst.AudioFeatures.CALL_AUDIO, true);
                        }
                    }
                    if ((mMusicConnectionState == BluetoothProfile.STATE_DISCONNECTED) &&
                        (mVoiceConnectionState == BluetoothProfile.STATE_DISCONNECTED)) {
                        transitionTo(smState);
                    }
                } break;

                case AcmStackEvent.CONNECTION_STATE_CONNECTED: {
                    // start lock release timer  TODO:when CSIP support is available
                    //sendMessageDelayed(CSIP_LOCK_RELEASE_TIMEOUT, sCsipLockReleaseTimeoutMs);
                    if (contextType == CONTEXT_TYPE_MUSIC) {
                        mMusicConnectionState = BluetoothProfile.STATE_CONNECTED;
                        if (mAcmService.isPeerDeviceConnectedForContext(mDevice, mSetId, CONTEXT_TYPE_MUSIC)) {
                            Log.d(TAG, "Fellow device is already connected, update MUSIC");
                            updateConnectionState(mDevice, mMusicConnectionState, CONTEXT_TYPE_MUSIC, false);
                        } else {
                            Log.d(TAG, "First member of group to connect MUSIC");
                            updateConnectionState(mDevice, mMusicConnectionState, CONTEXT_TYPE_MUSIC, true);
                        }
                    } else if (contextType == CONTEXT_TYPE_VOICE) {
                        mVoiceConnectionState = BluetoothProfile.STATE_CONNECTED;
                        if (mAcmService.isPeerDeviceConnectedForContext(mDevice, mSetId, CONTEXT_TYPE_VOICE)) {
                            Log.d(TAG, "Fellow device is already connected, update VOICE");
                            updateConnectionState(mDevice, mVoiceConnectionState, ApmConst.AudioFeatures.CALL_AUDIO, false);
                        } else {
                            Log.d(TAG, "First member of group to connect VOICE");
                            updateConnectionState(mDevice, mVoiceConnectionState, ApmConst.AudioFeatures.CALL_AUDIO, true);
                        }
                    }
                    transitionTo(mConnected);
                } break;

                case AcmStackEvent.CONNECTION_STATE_CONNECTING:
                    // Ignored - probably an event that the outgoing connection was initiated
                    break;
                case AcmStackEvent.CONNECTION_STATE_DISCONNECTING: {
                    Log.w(TAG, "Connecting device disconnecting: " + mDevice);
                    transitionTo(mDisconnecting);
                } break;

                default:
                    Log.e(TAG, "Incorrect event: " + state);
                    break;
            }
        }
    }

    @VisibleForTesting
    class Disconnecting extends State {
        @Override
        public void enter() {
            Message currentMessage = getCurrentMessage();
            Log.i(TAG, "Enter Disconnecting(" + mDevice + "): " + (currentMessage == null ? "null"
                    : messageWhatToString(currentMessage.what)));
            sendMessageDelayed(CONNECT_TIMEOUT, sConnectTimeoutMs);
            synchronized (this) {
                mConnectionState = BluetoothProfile.STATE_DISCONNECTING;
            }
            mAcmService.handleAcmDeviceStateChange(mDevice, mConnectionState, mSetId);
            if (mContextTypeToDisconnect == CONTEXT_TYPE_MUSIC && mMusicConnectionState != BluetoothProfile.STATE_DISCONNECTED) {
              mMusicConnectionState = BluetoothProfile.STATE_DISCONNECTING;
              if (mAcmService.isPeerDeviceConnectedForContext(mDevice, mSetId, CONTEXT_TYPE_MUSIC)) {
                Log.d(TAG, "Fellow device is already connected, update MEDIA");
                updateConnectionState(mDevice, mMusicConnectionState, CONTEXT_TYPE_MUSIC, false);
              } else {
                Log.d(TAG, "Last member to disconnect, update MEDIA");
                updateConnectionState(mDevice, mMusicConnectionState, CONTEXT_TYPE_MUSIC, true);
              }
            } else if (mContextTypeToDisconnect == CONTEXT_TYPE_VOICE && mVoiceConnectionState != BluetoothProfile.STATE_DISCONNECTED) {
              mVoiceConnectionState = BluetoothProfile.STATE_DISCONNECTING;
              if (mAcmService.isPeerDeviceConnectedForContext(mDevice, mSetId, CONTEXT_TYPE_VOICE)) {
                Log.d(TAG, "Fellow device is already connected, update VOICE ");
                updateConnectionState(mDevice, mVoiceConnectionState, ApmConst.AudioFeatures.CALL_AUDIO, false);
              } else {
                Log.d(TAG, "Last member to disconnect, update VOICE");
                updateConnectionState(mDevice, mVoiceConnectionState, ApmConst.AudioFeatures.CALL_AUDIO, true);
              }
            } else if (mContextTypeToDisconnect == CONTEXT_TYPE_MUSIC_VOICE && mMusicConnectionState != BluetoothProfile.STATE_DISCONNECTED
                       && mVoiceConnectionState != BluetoothProfile.STATE_DISCONNECTED) {
              mMusicConnectionState = BluetoothProfile.STATE_DISCONNECTING;
              mVoiceConnectionState = BluetoothProfile.STATE_DISCONNECTING;
              if (mAcmService.isPeerDeviceConnectedForContext(mDevice, mSetId, CONTEXT_TYPE_MUSIC)) {
                Log.d(TAG, "Fellow device is already connected, update MEDIA");
                updateConnectionState(mDevice, mMusicConnectionState, CONTEXT_TYPE_MUSIC, false);
              } else {
                Log.d(TAG, "Last member to disconnect, update MEDIA");
                updateConnectionState(mDevice, mMusicConnectionState, CONTEXT_TYPE_MUSIC, true);
              }
              if (mAcmService.isPeerDeviceConnectedForContext(mDevice, mSetId, CONTEXT_TYPE_VOICE)) {
                Log.d(TAG, "Fellow device is already connected, update VOICE");
                updateConnectionState(mDevice, mVoiceConnectionState, ApmConst.AudioFeatures.CALL_AUDIO, false);
              } else {
                Log.d(TAG, "Last member to disconnect, update VOICE");
                updateConnectionState(mDevice, mVoiceConnectionState, ApmConst.AudioFeatures.CALL_AUDIO, true);
              }
            }
        }

        @Override
        public void exit() {
            Message currentMessage = getCurrentMessage();
            log("Exit Disconnecting(" + mDevice + "): " + (currentMessage == null ? "null"
                    : messageWhatToString(currentMessage.what)));
            mLastConnectionState = BluetoothProfile.STATE_DISCONNECTING;
            removeMessages(CONNECT_TIMEOUT);
        }

        @Override
        public boolean processMessage(Message message) {
            log("Disconnecting process message(" + mDevice + "): "
                    + messageWhatToString(message.what));

            switch (message.what) {
                case CONNECT:
                    deferMessage(message);
                    break;
                case CONNECT_TIMEOUT: {
                    Log.w(TAG, "Disconnecting connection timeout: " + mDevice);
                    mAcmNativeInterface.disconnectAcm(mDevice, mCurrentContextType);
                    AcmStackEvent event =
                            new AcmStackEvent(AcmStackEvent.EVENT_TYPE_CONNECTION_STATE_CHANGED);
                    event.device = mDevice;
                    event.valueInt1 = AcmStackEvent.CONNECTION_STATE_DISCONNECTED;
                    event.valueInt2 = mCurrentContextType;
                    sendMessage(STACK_EVENT, event);
                    break;
                }
                case DISCONNECT:
                    deferMessage(message);
                    break;
                case GATT_CONNECTION_STATE_CHANGED:
                    deferMessage(message);
                    break;
                case STACK_EVENT:
                    AcmStackEvent event = (AcmStackEvent) message.obj;
                    log("Disconnecting: stack event: " + event);
                    if (!mDevice.equals(event.device)) {
                        Log.wtf(TAG, "Device(" + mDevice + "): event mismatch: " + event);
                    }
                    switch (event.type) {
                        case AcmStackEvent.EVENT_TYPE_CONNECTION_STATE_CHANGED:
                            processConnectionEvent(event.valueInt1, event.valueInt2);
                            break;
                        case AcmStackEvent.EVENT_TYPE_CODEC_CONFIG_CHANGED:
                            processCodecConfigEvent(event.codecStatus, event.valueInt2);
                            break;
                        case AcmStackEvent.EVENT_TYPE_AUDIO_STATE_CHANGED:
                        default:
                            Log.e(TAG, "Disconnecting: ignoring stack event: " + event);
                            break;
                    }
                    break;

                default:
                    return NOT_HANDLED;
            }
            return HANDLED;
        }

        // in Disconnecting state
        private void processConnectionEvent(int event, int contextType) {
            IState smState;
            switch (event) {
                case AcmStackEvent.CONNECTION_STATE_DISCONNECTED: {
                    Log.w(TAG, "Disconnecting device disconnected " + mDevice);
                    mCurrentContextType &= ~contextType;
                    Log.i(TAG, "mCurrentContextType now is " + contextTypeToString(mCurrentContextType));
                    processTransitionContextState(mDisconnecting, mDisconnected, contextType);
                    if (contextType == CONTEXT_TYPE_MUSIC) {
                        if (mAcmService.isPeerDeviceConnectedForContext(mDevice, mSetId, CONTEXT_TYPE_MUSIC)) {
                            Log.d(TAG, "Fellow device is already connected, update MUSIC");
                            updateConnectionState(mDevice, mMusicConnectionState, CONTEXT_TYPE_MUSIC, false);
                        } else {
                            Log.d(TAG, "Last member to disconnect, update MUSIC");
                            updateConnectionState(mDevice, mMusicConnectionState, CONTEXT_TYPE_MUSIC, true);
                        }
                    } else if (contextType == CONTEXT_TYPE_VOICE) {
                        if (mAcmService.isPeerDeviceConnectedForContext(mDevice, mSetId, CONTEXT_TYPE_VOICE)) {
                            Log.d(TAG, "Fellow device is already connected, update VOICE");
                            updateConnectionState(mDevice, mVoiceConnectionState, ApmConst.AudioFeatures.CALL_AUDIO, false);
                        } else {
                            Log.d(TAG, "Last member to disconnect, update VOICE");
                            updateConnectionState(mDevice, mVoiceConnectionState, ApmConst.AudioFeatures.CALL_AUDIO, true);
                        }
                    }
                    if (mMusicConnectionState == BluetoothProfile.STATE_DISCONNECTED &&
                        mVoiceConnectionState == BluetoothProfile.STATE_DISCONNECTED) {
                        transitionTo(mDisconnected);
                    }
                    if ((mCurrentContextType == CONTEXT_TYPE_VOICE) &&
                        (mVoiceConnectionState == BluetoothProfile.STATE_CONNECTED) ||
                        (mCurrentContextType == CONTEXT_TYPE_MUSIC) &&
                        (mMusicConnectionState == BluetoothProfile.STATE_CONNECTED)) {
                        Log.d(TAG, "Context " + contextTypeToString(mCurrentContextType) +
                            " still connected");
                        transitionTo(mConnected);
                    }
                } break;

                case AcmStackEvent.CONNECTION_STATE_CONNECTED: {
                    //TODO:sendMessageDelayed(CSIP_LOCK_RELEASE_TIMEOUT, sCsipLockReleaseTimeoutMs);
                    // Reject the connection and stay in Disconnecting state
                    if ((mMusicConnectionState == BluetoothProfile.STATE_DISCONNECTING) &&
                        (contextType == CONTEXT_TYPE_MUSIC) ||
                        (mVoiceConnectionState == BluetoothProfile.STATE_DISCONNECTING) &&
                        (contextType == CONTEXT_TYPE_VOICE)) {
                        Log.w(TAG, "Incoming ACM Connected request rejected: " + mDevice);
                        mAcmNativeInterface.disconnectAcm(mDevice, contextType);
                        break;
                    }
                    if (contextType == CONTEXT_TYPE_MUSIC) {
                        mMusicConnectionState = BluetoothProfile.STATE_CONNECTED;
                        if (mAcmService.isPeerDeviceConnectedForContext(mDevice, mSetId,
                            CONTEXT_TYPE_MUSIC)) {
                            Log.d(TAG, "Fellow device is already connected, update MUSIC");
                            updateConnectionState(mDevice, mMusicConnectionState,
                                    CONTEXT_TYPE_MUSIC, false);
                        } else {
                            Log.d(TAG, "First member of group to connect MUSIC");
                            updateConnectionState(mDevice, mMusicConnectionState,
                                    CONTEXT_TYPE_MUSIC, true);
                        }
                    } else if (contextType == CONTEXT_TYPE_VOICE) {
                        mVoiceConnectionState = BluetoothProfile.STATE_CONNECTED;
                        if (mAcmService.isPeerDeviceConnectedForContext(mDevice, mSetId,
                                            CONTEXT_TYPE_VOICE)) {
                            Log.d(TAG, "Fellow device is already connected, update VOICE");
                            updateConnectionState(mDevice, mVoiceConnectionState,
                                    ApmConst.AudioFeatures.CALL_AUDIO, false);
                        } else {
                            Log.d(TAG, "First member of group to connect VOICE");
                            updateConnectionState(mDevice, mVoiceConnectionState,
                                    ApmConst.AudioFeatures.CALL_AUDIO, true);
                        }
                    }
                } break;

                case AcmStackEvent.CONNECTION_STATE_DISCONNECTING:
                    if (contextType == CONTEXT_TYPE_MUSIC)
                        updateConnectionState(mDevice, BluetoothProfile.STATE_DISCONNECTING, CONTEXT_TYPE_MUSIC, false);
                    else if (contextType == CONTEXT_TYPE_VOICE)
                        updateConnectionState(mDevice, BluetoothProfile.STATE_DISCONNECTING, CONTEXT_TYPE_VOICE, false);
                    else {
                        updateConnectionState(mDevice, BluetoothProfile.STATE_DISCONNECTING, CONTEXT_TYPE_MUSIC, false);
                        updateConnectionState(mDevice, BluetoothProfile.STATE_DISCONNECTING, CONTEXT_TYPE_VOICE, false);
                    }
                    Log.d(TAG, "Updating disconnecting state to APM " + mDevice + "contextType " + contextType);
                    IsDisconnectRequested = false;
                    // We are already disconnecting, do nothing
                    break;
                default:
                    Log.e(TAG, "Incorrect event: " + event);
                    break;
            }
        }
    }

    @VisibleForTesting
    class Connected extends State {
        @Override
        public void enter() {
            Message currentMessage = getCurrentMessage();
            Log.i(TAG, "Enter Connected(" + mDevice + "): " + (currentMessage == null ? "null"
                    : messageWhatToString(currentMessage.what)));
            synchronized (this) {
                mConnectionState = BluetoothProfile.STATE_CONNECTED;
            }
            mAcmService.handleAcmDeviceStateChange(mDevice, mConnectionState, mSetId);
        }

        @Override
        public void exit() {
            Message currentMessage = getCurrentMessage();
            log("Exit Connected(" + mDevice + "): " + (currentMessage == null ? "null"
                    : messageWhatToString(currentMessage.what)));
            mLastConnectionState = BluetoothProfile.STATE_CONNECTED;
            numRetry = 0;
        }

        @Override
        public boolean processMessage(Message message) {
            log("Connected process message(" + mDevice + "): " +
                                           messageWhatToString(message.what));

            switch (message.what) {
                case CONNECT: {
                    if (message.arg1 == CONTEXT_TYPE_MUSIC &&
                        mMusicConnectionState != BluetoothProfile.STATE_DISCONNECTED) {
                        Log.d(TAG, "Ignore as media are connected/connecting");
                        break;
                    }

                    if (message.arg1 == CONTEXT_TYPE_VOICE &&
                        mVoiceConnectionState != BluetoothProfile.STATE_DISCONNECTED) {
                        Log.d(TAG, "Ignore as voice are connected/connecting");
                        break;
                    }

                    if (message.arg1 == CONTEXT_TYPE_MUSIC_VOICE &&
                        mVoiceConnectionState != BluetoothProfile.STATE_DISCONNECTED &&
                        mMusicConnectionState != BluetoothProfile.STATE_DISCONNECTED) {
                        Log.d(TAG, "Ignore as media and voice are connected/connecting");
                        break;
                    }

                    mCurrentContextType |= message.arg1;
                    Log.i(TAG, "mCurrentContextType now is " +
                          contextTypeToString(mCurrentContextType));
                    mProfileType = message.arg2;
                    mPreferredContext = (int)message.obj;
                    Log.i(TAG, "Connecting " + contextTypeToString(message.arg1) +
                               " to " + mDevice);

                   if (message.arg1 == CONTEXT_TYPE_MUSIC_VOICE) {
                       Log.d(TAG, "State:- Connected, event:- CONNECT: "+
                               "mVoiceConnectionState: " + mVoiceConnectionState +
                               ", mMusicConnectionState" + mMusicConnectionState);
                       if (mVoiceConnectionState == BluetoothProfile.STATE_CONNECTED) {
                           message.arg1 -= CONTEXT_TYPE_VOICE;
                       } else if (mMusicConnectionState == BluetoothProfile.STATE_CONNECTED) {
                           message.arg1 -= CONTEXT_TYPE_MUSIC;
                       }
                   }
                   Log.i(TAG, "Connecting " + contextTypeToString(message.arg1) +
                               " to " + mDevice + " after re-check connected profiles");

                    mSetId = mAcmService.getCsipManager().getCsipSetId(mDevice, null);
                    mGroupAddress = mAcmService.getGroup(mDevice);
                    Log.d(TAG, "State:- Connected, event:- CONNECT: "+
                               "Group bd address  " + mGroupAddress);

                    if (message.arg1 == CONTEXT_TYPE_MUSIC) {
                        mMusicConnectionState = BluetoothProfile.STATE_CONNECTING;
                        if (mAcmService.isPeerDeviceConnectedForContext(mDevice, mSetId, CONTEXT_TYPE_MUSIC)) {
                            Log.d(TAG, "State:- Connected, event:- CONNECT: "+
                                       "Fellow device is already connected, not first member ");
                            updateConnectionState(mDevice, mMusicConnectionState,
                                    CONTEXT_TYPE_MUSIC, false);
                        } else {
                            Log.d(TAG, "State:- Connected, event:- CONNECT: "+
                                       "First member of group to connect MUSIC");
                            updateConnectionState(mDevice, mMusicConnectionState,
                                    CONTEXT_TYPE_MUSIC, true);
                        }
                    } else if (message.arg1 == CONTEXT_TYPE_VOICE) {
                        mVoiceConnectionState = BluetoothProfile.STATE_CONNECTING;
                        if (mAcmService.isPeerDeviceConnectedForContext(mDevice, mSetId, CONTEXT_TYPE_VOICE)) {
                            Log.d(TAG, "State:- Connected, event:- CONNECT: "+
                                       "Fellow device is already connected, not first member ");
                            updateConnectionState(mDevice, mVoiceConnectionState,
                                    ApmConst.AudioFeatures.CALL_AUDIO, false);
                        } else {
                            Log.d(TAG, "State:- Connected, event:- CONNECT: "+
                                       "First member of group to connect VOICE");
                            updateConnectionState(mDevice, mVoiceConnectionState,
                                    ApmConst.AudioFeatures.CALL_AUDIO, true);
                        }
                    } else if (message.arg1 == CONTEXT_TYPE_MUSIC_VOICE) {
                        mMusicConnectionState = BluetoothProfile.STATE_CONNECTING;
                        mVoiceConnectionState = BluetoothProfile.STATE_CONNECTING;
                        if (mAcmService.isPeerDeviceConnectedForContext(mDevice, mSetId, CONTEXT_TYPE_MUSIC)) {
                            Log.d(TAG, "State:- Connected, event:- CONNECT: "+
                                       "Fellow device is already connected, not first member ");
                            updateConnectionState(mDevice, mMusicConnectionState,
                                    CONTEXT_TYPE_MUSIC, false);
                        } else {
                            Log.d(TAG, "State:- Connected, event:- CONNECT: "+
                                       "First member of group to connect MUSIC");
                            updateConnectionState(mDevice, mMusicConnectionState,
                                    CONTEXT_TYPE_MUSIC, true);
                        }
                        if (mAcmService.isPeerDeviceConnectedForContext(mDevice, mSetId, CONTEXT_TYPE_VOICE)) {
                            Log.d(TAG, "State:- Connected, event:- CONNECT: "+
                                       "Fellow device is already connected, not first member ");
                            updateConnectionState(mDevice, mVoiceConnectionState,
                                    ApmConst.AudioFeatures.CALL_AUDIO, false);
                        } else {
                            Log.d(TAG, "State:- Connected, event:- CONNECT: "+
                                       "First member of group to connect VOICE");
                            updateConnectionState(mDevice, mVoiceConnectionState,
                                    ApmConst.AudioFeatures.CALL_AUDIO, true);
                        }
                    }

                    if (mAcmService.IsLockSupportAvailable(mDevice)) {
                        Log.d(TAG, "Exclusive Access support available, gatt should already be connected");
                        //if lock support available then go for CSIP connect
                        //mBluetoothGatt = mDevice.connectGatt(mAcmService, false, mGattCallback, BluetoothDevice.TRANSPORT_LE, 7);
                        //sendMessageDelayed(GATT_CONNECTION_TIMEOUT, GATT_CONNECTION_TIMEOUT_MS);
                        //transitionTo(mConnecting);
                        //break;
                    } else {
                        Log.d(TAG, "Exclusive Access support not available, gatt should already be connected");
                        //mBluetoothGatt = mDevice.connectGatt(mAcmService, false, mGattCallback, BluetoothDevice.TRANSPORT_LE, 7);
                        //sendMessageDelayed(GATT_CONNECTION_TIMEOUT, GATT_CONNECTION_TIMEOUT_MS);
                        //transitionTo(mConnecting);
                    }

                    if (!mAcmNativeInterface.connectAcm(mDevice, message.arg1,
                                                        message.arg2, (int)message.obj)) {
                        Log.e(TAG, "Disconnected: error connecting to " +
                                                  mDevice + " remain in connected");
                    }
                } break;

                case DISCONNECT: {//disconnect request goes individual
                    IsDisconnectRequested = true;
                    mIsDeviceWhitelisted = false;
                    mContextTypeToDisconnect = (int)message.obj;
                    IState state = mDisconnecting;
                    boolean disconnected_flag = false;
                    //check if disconnect is for individual context type
                    Log.i(TAG, "Disconnecting " + contextTypeToString(mContextTypeToDisconnect) +  " from " + mDevice);
                    if (((mContextTypeToDisconnect == CONTEXT_TYPE_MUSIC) &&
                        (mMusicConnectionState != BluetoothProfile.STATE_CONNECTED)) ||
                        ((mContextTypeToDisconnect == CONTEXT_TYPE_VOICE) &&
                        (mVoiceConnectionState != BluetoothProfile.STATE_CONNECTED))) {
                        Log.i(TAG, "Defering until Media & Voice both are connected");
                        deferMessage(message);
                        break;
                    }

                    if (!mAcmNativeInterface.disconnectAcm(mDevice, mContextTypeToDisconnect)) {
                        Log.e(TAG, "error disconnecting " + contextTypeToString(mContextTypeToDisconnect) +  " from " + mDevice);
                        transitionTo(mDisconnected);
                        disconnected_flag = true;
                    }
                    if ((mMusicConnectionState == BluetoothProfile.STATE_CONNECTED) &&
                        (mVoiceConnectionState == BluetoothProfile.STATE_CONNECTED)) {
                        if (mContextTypeToDisconnect != CONTEXT_TYPE_MUSIC_VOICE) {
                            /*only 1/2 contexts are being disconnected,
                            remain in connected state but broadcast the connection state*/
                            state = mConnected;
                        }
                    }
                    processTransitionContextState(mConnected, (disconnected_flag ? mDisconnected : mDisconnecting), mContextTypeToDisconnect);
                    if (state == mConnected) {
                        if (mContextTypeToDisconnect == CONTEXT_TYPE_MUSIC) {
                            if (mAcmService.isPeerDeviceConnectedForContext(mDevice, mSetId, CONTEXT_TYPE_MUSIC)) {
                                Log.d(TAG, "Fellow device is already connected, update MUSIC");
                                updateConnectionState(mDevice, mMusicConnectionState, CONTEXT_TYPE_MUSIC, false);
                            } else {
                                Log.d(TAG, "Last member to disconnect, update MUSIC");
                                updateConnectionState(mDevice, mMusicConnectionState, CONTEXT_TYPE_MUSIC, true);
                            }
                        } else if (mContextTypeToDisconnect == CONTEXT_TYPE_VOICE) {
                            if (mAcmService.isPeerDeviceConnectedForContext(mDevice, mSetId, CONTEXT_TYPE_VOICE)) {
                                Log.d(TAG, "Fellow device is already connected, update VOICE");
                                updateConnectionState(mDevice, mVoiceConnectionState, ApmConst.AudioFeatures.CALL_AUDIO, false);
                            } else {
                                Log.d(TAG, "Last member to disconnect, update VOICE");
                                updateConnectionState(mDevice, mVoiceConnectionState, ApmConst.AudioFeatures.CALL_AUDIO, true);
                            }
                        }
                    } else {
                        transitionTo(state);
                    }
                    /*mSetId = mSetCoordinator.getRemoteDeviceSetId(mDevice, null); // TODO: UUID ?
                    List<BluetoothDevice> members = new ArrayList<BluetoothDevice>();
                    members.add(mDevice);
                    //setlockvalue takes device list
                    mCsipLockRequested = true;
                    mDeviceLocked = false;
                    mSetCoordinator.setLockValue(mAcmService.mCsipAppId, mSetId, members, BluetoothCsip.LOCK);*/
                } break;

                case CSIP_LOCK_STATUS_LOCKED: {
                    mCsipLockRequested = false;
                    int value = (int)message.arg1;
                    int setId = (int)message.obj;
                    int st = mAcmService.getCsipConnectionState(mDevice);
                    Log.d(TAG, "Exclusive Access state changed:" + value);
                    if (value == mAcmService.getCsipManager().LOCK) {
                        mDeviceLocked = true;
                        if (IsDisconnectRequested) {
                            Log.i(TAG, "Disconnecting " + contextTypeToString(mContextTypeToDisconnect) +  " from " + mDevice);
                            if (!mAcmNativeInterface.disconnectAcm(mDevice, mContextTypeToDisconnect)) { // this context Type is passed in disconnect api from APM
                                Log.e(TAG, "error disconnecting " + contextTypeToString(mContextTypeToDisconnect) +  " from " + mDevice);
                                transitionTo(mDisconnected);
                            }
                            transitionTo(mDisconnecting);
                        } else if (IsReconfigRequested) {
                            Log.w(TAG, "Reconfig requested Exclusive Access");
                            if (!mAcmNativeInterface.ChangeCodecConfigPreference(mDevice, mReconfig)) { // this context Type is passed in disconnect api from APM
                                Log.e(TAG, "reconfig error " + mDevice);
                                break;
                            }
                        }
                    }
                } break;

                case CSIP_CONNECTION_STATE_CHANGED:
                    int state = (int)message.obj;
                    if (state == BluetoothProfile.STATE_DISCONNECTED)
                        mCsipConnectionState = BluetoothProfile.STATE_DISCONNECTED;
                    break;

                case CSIP_LOCK_RELEASE_TIMEOUT:
                    //lock release individual ?
                    Log.d(TAG, "Exclusive Access timeout to " + mDevice);
                    List<BluetoothDevice> devices = new ArrayList<BluetoothDevice>();
                    devices.add(mDevice);
                    mAcmService.getCsipManager().setLock(mSetId, devices, mAcmService.getCsipManager().UNLOCK);
                    mDeviceLocked = false;
                    break;

                case CSIP_LOCK_STATUS_RELEASED:
                    // ignore disconnect CSIP
                    removeMessages(CSIP_LOCK_RELEASE_TIMEOUT);
                    mDeviceLocked = false;
                    break;

                case CODEC_CONFIG_CHANGED: {
                    IsReconfigRequested = true;
                    mReconfig = mAcmService.getAcmName();
                    if (!mAcmNativeInterface.ChangeCodecConfigPreference(mDevice, mReconfig)) {
                        Log.e(TAG, "reconfig error " + mDevice);
                        break;
                    }
                    /*int setId = (int)message.obj;
                    int st = mAcmService.getCsipConnectionState(mDevice);
                    if ((mDeviceLocked && st == BluetoothProfile.STATE_CONNECTED)) {
                        Log.w(TAG, "Device access is already granted and DeviceGroup is in connected state");
                        if (!mAcmNativeInterface.ChangeCodecConfigPreference(mDevice, mReconfig)) {
                            Log.e(TAG, "reconfig error " + mDevice);
                            break;
                        }
                        break;
                    }
                    //mSetId = mSetCoordinator.getRemoteDeviceSetId(mDevice, null ); //TODO: UUID what to set ?
                    List<BluetoothDevice> members = new ArrayList<BluetoothDevice>();
                    Iterator<BluetoothDevice> i = mAcmService.getCsipManager().getSetMembers(mSetId).iterator();
                    if (i != null) {
                        while (i.hasNext()) {
                            BluetoothDevice device = i.next();
                            if (mAcmService.getCsipConnectionState(device) == BluetoothProfile.STATE_CONNECTED) {
                                members.add(device);
                            }
                        }
                    }
                    mAcmService.getCsipManager().setLock(mSetId, members, mAcmService.getCsipManager().LOCK);*/
                } break;

                case START_STREAM: {
                    int value = (int)message.obj;
                    if (!mAcmNativeInterface.startStream(mDevice, value)) {
                        Log.e(TAG, "start stream error " + mDevice + ": numRetry: " + numRetry);
                        if (numRetry < VOICE_STREAM_START_RETRY_COUNT) {
                            sendMessage(obtainMessage(START_STREAM_REQ, value));
                            numRetry ++;
                        } else {
                            numRetry = 0;
                        }
                        break;
                    } else {
                        numRetry = 0;
                    }
                    /*int setId = (int)message.obj;
                    int st = mAcmService.getCsipConnectionState(mDevice);
                    if ((mDeviceLocked && st == BluetoothProfile.STATE_CONNECTED)) {
                        Log.w(TAG, "Device access is already granted and DeviceGroup is in connected state");
                        if (!mAcmNativeInterface.ChangeCodecConfigPreference(mDevice, mReconfig)) {
                            Log.e(TAG, "reconfig error " + mDevice);
                            break;
                        }
                        break;
                    }
                    //mSetId = mSetCoordinator.getRemoteDeviceSetId(mDevice, null ); //TODO: UUID what to set ?
                    List<BluetoothDevice> members = new ArrayList<BluetoothDevice>();
                    Iterator<BluetoothDevice> i = mAcmService.getCsipManager().getSetMembers(mSetId).iterator();
                    if (i != null) {
                        while (i.hasNext()) {
                            BluetoothDevice device = i.next();
                            if (mAcmService.getCsipConnectionState(device) == BluetoothProfile.STATE_CONNECTED) {
                                members.add(device);
                            }
                        }
                    }
                    mAcmService.getCsipManager().setLock(mSetId, members, mAcmService.getCsipManager().LOCK);*/
                } break;

               case STOP_STREAM: {
                    int value = (int)message.obj;
                    deferMessage(message);
                    Log.wtf(TAG, "Defer STOP stream as start_stream is still in progress");
                } break;

                case START_STREAM_REQ: {
                    if (!mAcmService.isPeerDeviceStreaming(mDevice, mSetId, CONTEXT_TYPE_MUSIC)) {
                        mAcmService.StartStream(mGroupAddress, CONTEXT_TYPE_VOICE);
                    }
                } break;

                case STACK_EVENT:
                    AcmStackEvent event = (AcmStackEvent) message.obj;
                    log("Connected: stack event: " + event);
                    if (!mDevice.equals(event.device)) {
                        Log.wtf(TAG, "Device(" + mDevice + "): event mismatch: " + event);
                    }
                    switch (event.type) {
                        case AcmStackEvent.EVENT_TYPE_CONNECTION_STATE_CHANGED:
                            processConnectionEvent(event.valueInt1, event.valueInt2);
                            break;
                        case AcmStackEvent.EVENT_TYPE_AUDIO_STATE_CHANGED:
                            processAudioStateEvent(event.valueInt1, event.valueInt2);
                            break;
                        case AcmStackEvent.EVENT_TYPE_CODEC_CONFIG_CHANGED:
                            processCodecConfigEvent(event.codecStatus, event.valueInt2);
                            break;
                        case AcmStackEvent.EVENT_TYPE_META_DATA_CHANGED:
                            processMetaDataEvent(event.valueInt2);
                            break;
                        default:
                            Log.e(TAG, "Connected: ignoring stack event: " + event);
                            break;
                    }
                    break;

                case GATT_CONNECTION_STATE_CHANGED: {
                    Log.e(TAG, "Connection state as disconnected " + mDevice);
                    removeMessages(GATT_CONNECTION_TIMEOUT);
                    int st = (int)message.obj;
                    if (st == BluetoothProfile.STATE_DISCONNECTED) {
                        Log.d(TAG, " GATT Disconnected");
                        mIsDeviceWhitelisted = false;
                        mIsUpdateProfDisConnection = true;
                        transitionTo(mDisconnected);
                    } break;
                }

                default:
                    return NOT_HANDLED;
            }
            return HANDLED;
        }

        // in Connected state
        private void processConnectionEvent(int event, int contextType) {
            IState smState;
            switch (event) {
                case AcmStackEvent.CONNECTION_STATE_DISCONNECTED: {
                  mCurrentContextType &= ~contextType;
                  Log.i(TAG, "mCurrentContextType now is " + contextTypeToString(mCurrentContextType));
                  processTransitionContextState(mDisconnecting, mDisconnected, contextType);
                  if (contextType == CONTEXT_TYPE_MUSIC) {
                      if (mAcmService.isPeerDeviceConnectedForContext(mDevice, mSetId, CONTEXT_TYPE_MUSIC)) {
                          Log.d(TAG, "Fellow device is already connected, update MUSIC");
                          updateConnectionState(mDevice, mMusicConnectionState, CONTEXT_TYPE_MUSIC, false);
                      } else {
                          Log.d(TAG, "Last member to disconnect, update MUSIC");
                          updateConnectionState(mDevice, mMusicConnectionState, CONTEXT_TYPE_MUSIC, true);
                      }
                  } else if (contextType == CONTEXT_TYPE_VOICE) {
                      if (mAcmService.isPeerDeviceConnectedForContext(mDevice, mSetId, CONTEXT_TYPE_VOICE)) {
                          Log.d(TAG, "Fellow device is already connected, update VOICE");
                          updateConnectionState(mDevice, mVoiceConnectionState, ApmConst.AudioFeatures.CALL_AUDIO, false);
                      } else {
                          Log.d(TAG, "Last member to disconnect, update VOICE");
                          updateConnectionState(mDevice, mVoiceConnectionState, ApmConst.AudioFeatures.CALL_AUDIO, true);
                      }
                  }
                  if (mMusicConnectionState == BluetoothProfile.STATE_DISCONNECTED &&
                          mVoiceConnectionState == BluetoothProfile.STATE_DISCONNECTED) {
                      transitionTo(mDisconnected);
                  }
                } break;

                case AcmStackEvent.CONNECTION_STATE_CONNECTED: {
                    //TODO:sendMessageDelayed(CSIP_LOCK_RELEASE_TIMEOUT, sCsipLockReleaseTimeoutMs);
                    Log.w(TAG, "ACM CONNECTED event for device: " + mDevice + " context type: " + contextTypeToString(contextType));
                    if (contextType == CONTEXT_TYPE_MUSIC) {
                        mMusicConnectionState = BluetoothProfile.STATE_CONNECTED;
                        if (mAcmService.isPeerDeviceConnectedForContext(mDevice, mSetId, CONTEXT_TYPE_MUSIC)) {
                            Log.d(TAG, "Fellow device is already connected, update MUSIC");
                            updateConnectionState(mDevice, mMusicConnectionState, CONTEXT_TYPE_MUSIC, false);
                        } else {
                            Log.d(TAG, "First member of group to connect MUSIC");
                            updateConnectionState(mDevice, mMusicConnectionState, CONTEXT_TYPE_MUSIC, true);
                        }
                    } else if (contextType == CONTEXT_TYPE_VOICE) {
                        mVoiceConnectionState = BluetoothProfile.STATE_CONNECTED;
                        if (mAcmService.isPeerDeviceConnectedForContext(mDevice, mSetId, CONTEXT_TYPE_VOICE)) {
                            Log.d(TAG, "Fellow device is already connected, update VOICE");
                            updateConnectionState(mDevice, mVoiceConnectionState, ApmConst.AudioFeatures.CALL_AUDIO, false);
                        } else {
                            Log.d(TAG, "First member of group to connect VOICE");
                            updateConnectionState(mDevice, mVoiceConnectionState, ApmConst.AudioFeatures.CALL_AUDIO, true);
                        }
                    }

                } break;

                case AcmStackEvent.CONNECTION_STATE_CONNECTING: {
                    Log.w(TAG, "Ignore ACM CONNECTED event: " + mDevice);
                } break;

                case AcmStackEvent.CONNECTION_STATE_DISCONNECTING: {
                    if ((contextType == CONTEXT_TYPE_MUSIC) &&
                        (mMusicConnectionState == BluetoothProfile.STATE_DISCONNECTING)) {
                        Log.w(TAG, "Ignore Disconnecting for media - already disconnecting");
                    } else if ((contextType == CONTEXT_TYPE_VOICE) &&
                               (mVoiceConnectionState == BluetoothProfile.STATE_DISCONNECTING)) {
                        Log.w(TAG, "Ignore Disconnecting for voice - already disconnecting");
                    } else {
                        smState = mDisconnecting;
                        Log.w(TAG, "Connected device disconnecting: " + mDevice);
                        if ((mMusicConnectionState == BluetoothProfile.STATE_CONNECTED) &&
                            (mVoiceConnectionState == BluetoothProfile.STATE_CONNECTED)) {
                            if (contextType != CONTEXT_TYPE_MUSIC_VOICE) {
                                /*only 1/2 contexts are being disconnected,
                                remain in connecting state but broadcast the connection state*/
                                smState = mConnected;
                            }
                        }
                        processTransitionContextState(mConnected, mDisconnecting, contextType);
                        if (smState == mConnected) {
                            if (contextType == CONTEXT_TYPE_MUSIC) {
                                updateConnectionState(mDevice, mMusicConnectionState, CONTEXT_TYPE_MUSIC, false);
                            } else if (contextType == CONTEXT_TYPE_VOICE) {
                                updateConnectionState(mDevice, mVoiceConnectionState, ApmConst.AudioFeatures.CALL_AUDIO, false);
                            }
                        } else {
                            transitionTo(smState);
                        }
                    }
                } break;

                default:
                    Log.e(TAG, "Connection State Device: " + mDevice + " bad event: " + event);
                    break;
            }
        }


        // in Connected state
        private void processAudioStateEvent(int state,  int contextType) {
            Log.i(TAG, "Connected: processAudioStateEvent: state: " + state + " mIsMusicPlaying: " + mIsMusicPlaying);
            switch (state) {
                case AcmStackEvent.AUDIO_STATE_STARTED: {
                    if (contextType == CONTEXT_TYPE_MUSIC)
                      mIsMusicPlaying = true;
                    else if (contextType == CONTEXT_TYPE_VOICE)
                      mIsVoicePlaying = true;
                    transitionTo(mStreaming);
                } break;
                case AcmStackEvent.AUDIO_STATE_REMOTE_SUSPEND:
                case AcmStackEvent.AUDIO_STATE_STOPPED: {
                    synchronized (this) {
                        if (contextType == CONTEXT_TYPE_MUSIC) {
                            if (mIsMusicPlaying) {
                                Log.i(TAG, "Connected: stopped media playing: " + mDevice);
                                mIsMusicPlaying = false;
                                mAcmService.onStreamStateChange(mDevice, false, CONTEXT_TYPE_MUSIC);
                                updateStreamState(mDevice, BluetoothA2dp.STATE_NOT_PLAYING, ApmConst.AudioFeatures.MEDIA_AUDIO);
                                if (mAcmService.isShoPendingStop()) {
                                    Log.i(TAG, "SHO was pending earlier, complete now");
                                    mAcmService.resetShoPendingStop();
                                    StreamAudioService service = StreamAudioService.getStreamAudioService();
                                    if (service != null) {
                                        service.onActiveDeviceChange(null, ApmConst.AudioFeatures.MEDIA_AUDIO);
                                    }
                                }
                            }
                        } else if (contextType == CONTEXT_TYPE_VOICE) {
                            if (mIsVoicePlaying) {
                                Log.i(TAG, "Connected: stopped voice playing: " + mDevice);
                                mIsVoicePlaying = false;
                                mAcmService.onStreamStateChange(mDevice, false, CONTEXT_TYPE_VOICE);
                                updateStreamState(mDevice, BluetoothA2dp.STATE_NOT_PLAYING, ApmConst.AudioFeatures.CALL_AUDIO);
                                if (!mAcmService.isPeerDeviceStreaming(mDevice, mSetId, CONTEXT_TYPE_VOICE)) {
                                    Log.i(TAG, "Connected: last peer voice stream stopped " + mDevice);
                                    setCallAudioOn(false);
                                    setVoiceParameters(false);
                                }
                                if (mAcmService.isVoiceShoPendingStop()) {
                                    Log.i(TAG, "Voice SHO was pending earlier, complete now");
                                    mAcmService.resetVoiceShoPendingStop();
                                    StreamAudioService service = StreamAudioService.getStreamAudioService();
                                    if (service != null) {
                                        service.onActiveDeviceChange(mAcmService.getVoiceActiveDevice(),
                                                ApmConst.AudioFeatures.CALL_AUDIO);
                                    }
                                }
                            }
                        }
                    }
                } break;
                default:
                    Log.e(TAG, "Audio State Device: " + mDevice + " bad state: " + state);
                    break;
            }
        }

        // in Connected state
        private void processMetaDataEvent(int contextType) {
            Log.i(TAG, "Connected: processMetaDataEvent: contextType: " + contextType +
                                                         ", mDevice: " + mDevice);
            Integer mAudioType = -1;
            if (contextType == CONTENT_TYPE_COVERSATIONAL) {
                mAudioType = ApmConst.AudioFeatures.CALL_AUDIO;
            } else if (contextType == CONTENT_TYPE_MEDIA) {
                mAudioType = ApmConst.AudioFeatures.MEDIA_AUDIO;
            }
            ActiveDeviceManagerService mActiveDeviceManager = ActiveDeviceManagerService.get();
            mActiveDeviceManager.setActiveDevice(mDevice, mAudioType);
        }

        private boolean isCallActive() {
            Log.i(TAG, "Connected: getCallStateInfo ");
            return mAcmService.isAcmCallActive();
        }
    }


    @VisibleForTesting
    class Streaming extends State {
        @Override
        public void enter() {
            Message currentMessage = getCurrentMessage();
            Log.i(TAG, "Enter Streaming(" + mDevice + "): " + (currentMessage == null ? "null"
                    : messageWhatToString(currentMessage.what)));

            if ((mMusicConnectionState == BluetoothProfile.STATE_CONNECTED)) {
                removeDeferredMessages(CONNECT);
            }
            if (mIsMusicPlaying) {
                Log.i(TAG, "start playing media: " + mDevice);
                mAcmService.onStreamStateChange(mDevice, true, CONTEXT_TYPE_MUSIC);
                updateStreamState(mDevice, BluetoothA2dp.STATE_PLAYING, ApmConst.AudioFeatures.MEDIA_AUDIO);
                mAcmService.updateLeaChannelMode(BluetoothA2dp.STATE_PLAYING, mDevice);
            } else if (mIsVoicePlaying) {
                Log.i(TAG, "start playing voice: " + mDevice);
                mAcmService.onStreamStateChange(mDevice, true, CONTEXT_TYPE_VOICE);
                updateStreamState(mDevice, BluetoothHeadset.STATE_AUDIO_CONNECTED, ApmConst.AudioFeatures.CALL_AUDIO);
                setVoiceParameters(true);
                setCallAudioOn(true);
            }
        }

        @Override
        public void exit() {
            Message currentMessage = getCurrentMessage();
            log("Exit Streaming(" + mDevice + "): " + (currentMessage == null ? "null"
                    : messageWhatToString(currentMessage.what)));
            synchronized (this) {
                if (mIsMusicPlaying) {
                    mAcmService.onStreamStateChange(mDevice, false, CONTEXT_TYPE_MUSIC);
                    updateStreamState(mDevice, BluetoothA2dp.STATE_NOT_PLAYING, ApmConst.AudioFeatures.MEDIA_AUDIO);
                    if (mAcmService.isShoPendingStop()) {
                        Log.i(TAG, "Exit Streaming: SHO was pending earlier, complete now");
                        mAcmService.resetShoPendingStop();
                        StreamAudioService service = StreamAudioService.getStreamAudioService();
                        if (service != null) {
                            service.onActiveDeviceChange(null, ApmConst.AudioFeatures.MEDIA_AUDIO);
                        }
                    }
                } else if (mIsVoicePlaying) {
                    mAcmService.onStreamStateChange(mDevice, false, CONTEXT_TYPE_VOICE);
                    updateStreamState(mDevice, BluetoothHeadset.STATE_AUDIO_DISCONNECTED, ApmConst.AudioFeatures.CALL_AUDIO);
                    if (!mAcmService.isPeerDeviceStreaming(mDevice, mSetId, CONTEXT_TYPE_VOICE)) {
                        Log.i(TAG, "Exit Streaming: last peer voice stream stopped " + mDevice);
                        setCallAudioOn(false);
                        setVoiceParameters(false);
                    }
                    if (mAcmService.isVoiceShoPendingStop()) {
                        Log.i(TAG, "Voice SHO was pending earlier, complete now");
                        mAcmService.resetVoiceShoPendingStop();
                        StreamAudioService service = StreamAudioService.getStreamAudioService();
                        if (service != null) {
                            service.onActiveDeviceChange(mAcmService.getVoiceActiveDevice(), ApmConst.AudioFeatures.CALL_AUDIO);
                        }
                    }
                }
                mIsMusicPlaying = false;
                mIsVoicePlaying = false;
            }
        }

        @Override
        public boolean processMessage(Message message) {
            log("Streaming process message(" + mDevice + "): " + messageWhatToString(message.what));

            switch (message.what) {
                case CONNECT: {
                    if (message.arg1 == CONTEXT_TYPE_MUSIC && mMusicConnectionState != BluetoothProfile.STATE_DISCONNECTED)
                        break;
                    if (message.arg1 == CONTEXT_TYPE_VOICE && mVoiceConnectionState != BluetoothProfile.STATE_DISCONNECTED)
                        break;
                    if (message.arg1 == CONTEXT_TYPE_MUSIC_VOICE && mVoiceConnectionState != BluetoothProfile.STATE_DISCONNECTED
                         && mMusicConnectionState != BluetoothProfile.STATE_DISCONNECTED)
                        break;
                    mCurrentContextType |= message.arg1;
                    Log.i(TAG, "mCurrentContextType now is " + contextTypeToString(mCurrentContextType));
                    mProfileType = message.arg2;
                    mPreferredContext = (int)message.obj;
                    Log.i(TAG, "Connecting " + contextTypeToString(message.arg1) + " to " + mDevice);
                    if (mAcmService.IsLockSupportAvailable(mDevice)) {
                        Log.d(TAG, "Exclusive Access support available, gatt should already be connected");
                        //if lock support available then go for CSIP connect
                        //mBluetoothGatt = mDevice.connectGatt(mAcmService, false, mGattCallback, BluetoothDevice.TRANSPORT_LE, 7);
                        //sendMessageDelayed(GATT_CONNECTION_TIMEOUT, GATT_CONNECTION_TIMEOUT_MS);
                        //transitionTo(mConnecting);
                        //break;
                    } else {
                        Log.d(TAG, "Exclusive Access support not available, gatt should already be connected");
                        //mBluetoothGatt = mDevice.connectGatt(mAcmService, false, mGattCallback, BluetoothDevice.TRANSPORT_LE, 7);
                        //sendMessageDelayed(GATT_CONNECTION_TIMEOUT, GATT_CONNECTION_TIMEOUT_MS);
                        //transitionTo(mConnecting);
                    }
                    if (!mAcmNativeInterface.connectAcm(mDevice, message.arg1, message.arg2, (int)message.obj)) {
                        Log.e(TAG, "Disconnected: error connecting to " + mDevice + " remain in streaming");
                    }
                } break;

                case DISCONNECT: {//disconnect request goes individual
                    IsDisconnectRequested = true;
                    mIsDeviceWhitelisted = false;
                    mContextTypeToDisconnect = (int)message.obj;
                    IState state = mDisconnecting;
                    boolean disconnected_flag = false;
                    //check if disconnect is for individual context type
                    Log.i(TAG, "Disconnecting " + contextTypeToString(mContextTypeToDisconnect) +  " from " + mDevice);
                    if (!mAcmNativeInterface.disconnectAcm(mDevice, mContextTypeToDisconnect)) {
                        Log.e(TAG, "error disconnecting " + contextTypeToString(mContextTypeToDisconnect) +  " from " + mDevice);
                        transitionTo(mDisconnected);
                        disconnected_flag = true;
                    }
                    if ((mMusicConnectionState == BluetoothProfile.STATE_CONNECTED)
                          && (mVoiceConnectionState == BluetoothProfile.STATE_CONNECTED)) {
                        if (mContextTypeToDisconnect != CONTEXT_TYPE_MUSIC_VOICE) {
                            /*only 1/2 contexts are being disconnected,
                            remain in connected state but broadcast the connection state*/
                            if ((mIsMusicPlaying && (mContextTypeToDisconnect == CONTEXT_TYPE_MUSIC)) ||
                                    (mIsVoicePlaying && (mContextTypeToDisconnect == CONTEXT_TYPE_VOICE))) {
                                Log.d(TAG, "Current streaming audio context: " + mContextTypeToDisconnect + " is disconnected: " + mDevice );
                                state = mConnected;
                            } else {
                                Log.d(TAG, "Non-streaming audio context: " + mContextTypeToDisconnect + " is disconnected: " + mDevice );
                                state = mStreaming;
                            }
                        }
                    }
                    processTransitionContextState(mConnected, (disconnected_flag ? mDisconnected : mDisconnecting), mContextTypeToDisconnect);
                    if (state == mConnected || state == mStreaming) {
                        if (mContextTypeToDisconnect == CONTEXT_TYPE_MUSIC) {
                            if (mAcmService.isPeerDeviceConnectedForContext(mDevice, mSetId, CONTEXT_TYPE_MUSIC)) {
                                Log.d(TAG, "Fellow device is already connected, update MUSIC");
                                updateConnectionState(mDevice, mMusicConnectionState, CONTEXT_TYPE_MUSIC, false);
                            } else {
                                Log.d(TAG, "Last member to disconnect, update MUSIC");
                                updateConnectionState(mDevice, mMusicConnectionState, CONTEXT_TYPE_MUSIC, true);
                            }
                        } else if (mContextTypeToDisconnect == CONTEXT_TYPE_VOICE) {
                            if (mAcmService.isPeerDeviceConnectedForContext(mDevice, mSetId, CONTEXT_TYPE_VOICE)) {
                                Log.d(TAG, "Fellow device is already connected, update VOICE");
                                updateConnectionState(mDevice, mVoiceConnectionState, ApmConst.AudioFeatures.CALL_AUDIO, false);
                            } else {
                                Log.d(TAG, "Last member to disconnect, update VOICE");
                                updateConnectionState(mDevice, mVoiceConnectionState, ApmConst.AudioFeatures.CALL_AUDIO, true);
                            }
                        }
                        if (state != mStreaming) {
                            transitionTo(state);
                        }
                    } else {
                        transitionTo(state);
                    }
                    /*mSetId = mSetCoordinator.getRemoteDeviceSetId(mDevice, null); // TODO: UUID ?
                    List<BluetoothDevice> members = new ArrayList<BluetoothDevice>();
                    members.add(mDevice);
                    //setlockvalue takes device list
                    mCsipLockRequested = true;
                    mDeviceLocked = false;
                    mSetCoordinator.setLockValue(mAcmService.mCsipAppId, mSetId, members, BluetoothCsip.LOCK);*/
                } break;

                case CSIP_LOCK_STATUS_LOCKED: {
                    mCsipLockRequested = false;
                    int value = (int)message.arg1;
                    int setId = (int)message.obj;
                    int st = mAcmService.getCsipConnectionState(mDevice);
                    Log.d(TAG, "Exclusive Access state changed:" + value);
                    if (value == mAcmService.getCsipManager().LOCK) {
                        mDeviceLocked = true;
                        if (IsDisconnectRequested) {
                            Log.i(TAG, "Disconnecting " + contextTypeToString(mContextTypeToDisconnect) +  " from " + mDevice);
                            if (!mAcmNativeInterface.disconnectAcm(mDevice, mContextTypeToDisconnect)) { // this context Type is passed in disconnect api from APM
                                Log.e(TAG, "error disconnecting " + contextTypeToString(mContextTypeToDisconnect) +  " from " + mDevice);
                                transitionTo(mDisconnected);
                            }
                            transitionTo(mDisconnecting);
                        } else if (IsReconfigRequested) {
                            Log.w(TAG, "Reconfig requested Exclusive Access");
                            if (!mAcmNativeInterface.ChangeCodecConfigPreference(mDevice, mReconfig)) { // this context Type is passed in disconnect api from APM
                                Log.e(TAG, "reconfig error " + mDevice);
                                break;
                            }
                        }
                    }
                } break;

                case CSIP_CONNECTION_STATE_CHANGED:
                    int state = (int)message.obj;
                    if (state == BluetoothProfile.STATE_DISCONNECTED)
                        mCsipConnectionState = BluetoothProfile.STATE_DISCONNECTED;
                    break;

                case CSIP_LOCK_RELEASE_TIMEOUT:
                    //lock release individual ?
                    Log.d(TAG, "Exclusive Access timeout to " + mDevice);
                    List<BluetoothDevice> devices = new ArrayList<BluetoothDevice>();
                    devices.add(mDevice);
                    mAcmService.getCsipManager().setLock(mSetId, devices, mAcmService.getCsipManager().UNLOCK);
                    mDeviceLocked = false;
                    break;

                case CSIP_LOCK_STATUS_RELEASED:
                    // ignore disconnect CSIP
                    removeMessages(CSIP_LOCK_RELEASE_TIMEOUT);
                    mDeviceLocked = false;
                    break;

                case CODEC_CONFIG_CHANGED: {
                    IsReconfigRequested = true;
                    mReconfig = mAcmService.getAcmName();
                    if (!mAcmNativeInterface.ChangeCodecConfigPreference(mDevice, mReconfig)) {
                        Log.e(TAG, "reconfig error " + mDevice);
                        break;
                    }
                    /*int setId = (int)message.obj;
                    int st = mAcmService.getCsipConnectionState(mDevice);
                    if ((mDeviceLocked && st == BluetoothProfile.STATE_CONNECTED)) {
                        Log.w(TAG, "Device is already acquired and DeviceGroup is in connected state");
                        if (!mAcmNativeInterface.ChangeCodecConfigPreference(mDevice, mReconfig)) {
                            Log.e(TAG, "reconfig error " + mDevice);
                            break;
                        }
                        break;
                    }
                    //mSetId = mSetCoordinator.getRemoteDeviceSetId(mDevice, null); //TODO: UUID what to set ?
                    List<BluetoothDevice> members = new ArrayList<BluetoothDevice>();
                    Iterator<BluetoothDevice> i = mAcmService.getCsipManager().getSetMembers(setId).iterator();
                    if (i != null) {
                        while (i.hasNext()) {
                            BluetoothDevice device = i.next();
                            if (mAcmService.getCsipConnectionState(device) == BluetoothProfile.STATE_CONNECTED) {
                                members.add(device);
                            }
                        }
                    }
                    mAcmService.getCsipManager().setLock(setId,
                                members, mAcmService.getCsipManager().LOCK);*/
                } break;

                case STOP_STREAM: {
                    int value = (int)message.obj;
                    if (!mAcmNativeInterface.stopStream(mDevice, value)) {
                        Log.e(TAG, "start stream error " + mDevice);
                        break;
                    }
                    /*int setId = (int)message.obj;
                    int st = mAcmService.getCsipConnectionState(mDevice);
                    if ((mDeviceLocked && st == BluetoothProfile.STATE_CONNECTED)) {
                        Log.w(TAG, "Device access is already granted and DeviceGroup is in connected state");
                        if (!mAcmNativeInterface.ChangeCodecConfigPreference(mDevice, mReconfig)) {
                            Log.e(TAG, "reconfig error " + mDevice);
                            break;
                        }
                        break;
                    }
                    //mSetId = mSetCoordinator.getRemoteDeviceSetId(mDevice, null ); //TODO: UUID what to set ?
                    List<BluetoothDevice> members = new ArrayList<BluetoothDevice>();
                    Iterator<BluetoothDevice> i = mAcmService.getCsipManager().getSetMembers(mSetId).iterator();
                    if (i != null) {
                        while (i.hasNext()) {
                            BluetoothDevice device = i.next();
                            if (mAcmService.getCsipConnectionState(device) == BluetoothProfile.STATE_CONNECTED) {
                                members.add(device);
                            }
                        }
                    }
                    mAcmService.getCsipManager().setLock(mSetId, members, mAcmService.getCsipManager().LOCK);*/
                } break;

                case START_STREAM:
                    int value = (int)message.obj;
                    if (value == CONTEXT_TYPE_VOICE && mIsMusicPlaying) {
                        deferMessage(obtainMessage(START_STREAM_REQ, value));
                        Log.wtf(TAG, "Defer START request for voice context");
                    }
                    break;

                case STACK_EVENT:
                    AcmStackEvent event = (AcmStackEvent) message.obj;
                    log("Streaming: stack event: " + event);
                    if (!mDevice.equals(event.device)) {
                        Log.wtf(TAG, "Device(" + mDevice + "): event mismatch: " + event);
                    }
                    switch (event.type) {
                        case AcmStackEvent.EVENT_TYPE_CONNECTION_STATE_CHANGED:
                            processConnectionEvent(event.valueInt1, event.valueInt2);
                            break;
                        case AcmStackEvent.EVENT_TYPE_AUDIO_STATE_CHANGED:
                            processAudioStateEvent(event.valueInt1, event.valueInt2);
                            break;
                        case AcmStackEvent.EVENT_TYPE_CODEC_CONFIG_CHANGED:
                            processCodecConfigEvent(event.codecStatus, event.valueInt2);
                            break;
                        case AcmStackEvent.EVENT_TYPE_META_DATA_CHANGED:
                            processMetaDataEvent(event.valueInt2);
                            break;
                        default:
                            Log.e(TAG, "Streaming: ignoring stack event: " + event);
                            break;
                    }
                    break;
                    default:
                        return NOT_HANDLED;
            }
            return HANDLED;
        }

        // in Streaming state
        private void processConnectionEvent(int event, int contextType) {
            IState smState;
            switch (event) {
                case AcmStackEvent.CONNECTION_STATE_DISCONNECTED: {
                    if ((contextType == CONTEXT_TYPE_MUSIC) &&
                        (mMusicConnectionState == BluetoothProfile.STATE_DISCONNECTED)) {
                        Log.w(TAG, "Ignore Disconnected for media - already disconnected");
                    } else if ((contextType == CONTEXT_TYPE_VOICE) &&
                               (mVoiceConnectionState == BluetoothProfile.STATE_DISCONNECTED)) {
                        Log.w(TAG, "Ignore Disconnected for voice - already disconnected");
                    } else {
                        //TODO: sendMessageDelayed(CSIP_LOCK_RELEASE_TIMEOUT, sCsipLockReleaseTimeoutMs);
                        smState = mDisconnected;
                        Log.w(TAG, "Streaming device disconnected: " + mDevice);
                        mCurrentContextType &= ~contextType;
                        Log.i(TAG, "mCurrentContextType now is " + contextTypeToString(mCurrentContextType));
                        processTransitionContextState(mDisconnecting, mDisconnected, contextType);
                        if (mMusicConnectionState == BluetoothProfile.STATE_CONNECTED ||
                            mVoiceConnectionState == BluetoothProfile.STATE_CONNECTED) {
                            if ((mIsMusicPlaying && contextType == CONTEXT_TYPE_MUSIC) ||
                                    (mIsVoicePlaying && contextType == CONTEXT_TYPE_VOICE)) {
                                Log.d(TAG, "Current streaming audio context: " + contextType + " is disconnected: " + mDevice);
                                smState = mConnected;
                            } else {
                                Log.d(TAG, "Non-streaming audio context: " + contextType + " is disconnected: " + mDevice);
                                smState = mStreaming;
                            }
                        }
                        if (smState == mConnected || smState == mStreaming) {
                            if (contextType == CONTEXT_TYPE_MUSIC) {
                                if (mAcmService.isPeerDeviceConnectedForContext(mDevice, mSetId, CONTEXT_TYPE_MUSIC)) {
                                    Log.d(TAG, "Fellow device is already connected, update MUSIC");
                                    updateConnectionState(mDevice, mMusicConnectionState, CONTEXT_TYPE_MUSIC, false);
                                } else {
                                    Log.d(TAG, "Last member to disconnect, update MUSIC");
                                    updateConnectionState(mDevice, mMusicConnectionState, CONTEXT_TYPE_MUSIC, true);
                                }
                            } else if (contextType == CONTEXT_TYPE_VOICE) {
                                if (mAcmService.isPeerDeviceConnectedForContext(mDevice, mSetId, CONTEXT_TYPE_VOICE)) {
                                    Log.d(TAG, "Fellow device is already connected, update VOICE");
                                    updateConnectionState(mDevice, mVoiceConnectionState, ApmConst.AudioFeatures.CALL_AUDIO, false);
                                } else {
                                    Log.d(TAG, "Last member to disconnect, update VOICE");
                                    updateConnectionState(mDevice, mVoiceConnectionState, ApmConst.AudioFeatures.CALL_AUDIO, true);
                                }
                            }
                            if (smState != mStreaming) {
                                transitionTo(smState);
                            }
                        } else {
                            transitionTo(smState);
                        }
                    }
                } break;

                case AcmStackEvent.CONNECTION_STATE_CONNECTED: {
                    //TODO:sendMessageDelayed(CSIP_LOCK_RELEASE_TIMEOUT, sCsipLockReleaseTimeoutMs);
                    Log.w(TAG, "ACM CONNECTED event for device: " + mDevice + " context type: " + contextTypeToString(contextType));
                    if (contextType == CONTEXT_TYPE_MUSIC) {
                        mMusicConnectionState = BluetoothProfile.STATE_CONNECTED;
                        if (mAcmService.isPeerDeviceConnectedForContext(mDevice, mSetId, CONTEXT_TYPE_MUSIC)) {
                            Log.d(TAG, "Fellow device is already connected, update MUSIC");
                            updateConnectionState(mDevice, mMusicConnectionState, CONTEXT_TYPE_MUSIC, false);
                        } else {
                            Log.d(TAG, "First member of group to connect MUSIC");
                            updateConnectionState(mDevice, mMusicConnectionState, CONTEXT_TYPE_MUSIC, true);
                        }
                    } else if (contextType == CONTEXT_TYPE_VOICE) {
                        mVoiceConnectionState = BluetoothProfile.STATE_CONNECTED;
                        if (mAcmService.isPeerDeviceConnectedForContext(mDevice, mSetId, CONTEXT_TYPE_VOICE)) {
                            Log.d(TAG, "Fellow device is already connected, update VOICE");
                            updateConnectionState(mDevice, mVoiceConnectionState, ApmConst.AudioFeatures.CALL_AUDIO, false);
                        } else {
                            Log.d(TAG, "First member of group to connect VOICE");
                            updateConnectionState(mDevice, mVoiceConnectionState, ApmConst.AudioFeatures.CALL_AUDIO, true);
                        }
                    }
                } break;

                case AcmStackEvent.CONNECTION_STATE_CONNECTING: {
                    Log.w(TAG, "ACM CONNECTING event: " + mDevice);
                } break;

                case AcmStackEvent.CONNECTION_STATE_DISCONNECTING: {
                    if ((contextType == CONTEXT_TYPE_MUSIC) &&
                        (mMusicConnectionState == BluetoothProfile.STATE_DISCONNECTING)) {
                        Log.w(TAG, "Ignore Disconnecting for media - already disconnecting");
                    } else if ((contextType == CONTEXT_TYPE_VOICE) &&
                               (mVoiceConnectionState == BluetoothProfile.STATE_DISCONNECTING)) {
                        Log.w(TAG, "Ignore Disconnecting for voice - already disconnecting");
                    } else {
                        smState = mDisconnecting;
                        Log.w(TAG, "Streaming device disconnecting: " + mDevice);
                        if ((mMusicConnectionState == BluetoothProfile.STATE_CONNECTED) &&
                            (mVoiceConnectionState == BluetoothProfile.STATE_CONNECTED)) {
                            if (contextType != CONTEXT_TYPE_MUSIC_VOICE) {
                                /*only 1/2 contexts are being disconnected,
                                remain in connecting state but broadcast the connection state*/
                                if ((mIsMusicPlaying && (contextType == CONTEXT_TYPE_MUSIC)) ||
                                        (mIsVoicePlaying && (contextType == CONTEXT_TYPE_VOICE))) {
                                    Log.d(TAG, "Current streaming audio context: " + contextType + " is disconnected: " + mDevice);
                                    smState = mConnected;
                                } else {
                                    Log.d(TAG, "Non-streaming audio context: " + contextType + " is disconnected: " + mDevice);
                                    smState = mStreaming;
                                }
                            }
                        }
                        processTransitionContextState(mConnected, mDisconnecting, contextType);
                        if (smState == mConnected || smState == mStreaming) {
                            if (contextType == CONTEXT_TYPE_MUSIC) {
                                updateConnectionState(mDevice, mMusicConnectionState, CONTEXT_TYPE_MUSIC, false);
                            } else if (contextType == CONTEXT_TYPE_VOICE) {
                                updateConnectionState(mDevice, mVoiceConnectionState, ApmConst.AudioFeatures.CALL_AUDIO, false);
                            }
                            if (smState != mStreaming) {
                                transitionTo(smState);
                            }
                        } else {
                            transitionTo(smState);
                        }
                    }
                } break;

                default:
                    Log.e(TAG, "Connection State Device: " + mDevice + " bad event: " + event);
                    break;
            }
        }

        // in Streaming state
        private void processAudioStateEvent(int state,  int contextType) {
            Log.i(TAG, "Streaming: processAudioStateEvent: state: " + state + "mIsMusicPlaying: " + mIsMusicPlaying);
            switch (state) {
                case AcmStackEvent.AUDIO_STATE_STARTED:
                    Log.i(TAG, "Streaming: already started: " + mDevice);
                    break;
                case AcmStackEvent.AUDIO_STATE_REMOTE_SUSPEND:
                case AcmStackEvent.AUDIO_STATE_STOPPED:
                    synchronized (this) {
                        if (contextType == CONTEXT_TYPE_MUSIC) {
                            if (mIsMusicPlaying) {
                                Log.i(TAG, "Streaming: stopped media playing: " + mDevice);
                                mIsMusicPlaying = false;
                                mAcmService.onStreamStateChange(mDevice, false, CONTEXT_TYPE_MUSIC);
                                updateStreamState(mDevice, BluetoothA2dp.STATE_NOT_PLAYING, CONTEXT_TYPE_MUSIC);
                                if (mAcmService.isShoPendingStop()) {
                                    Log.i(TAG, "Streaming: SHO was pending earlier, complete now");
                                    mAcmService.resetShoPendingStop();
                                    StreamAudioService service = StreamAudioService.getStreamAudioService();
                                    if (service != null) {
                                        service.onActiveDeviceChange(null, ApmConst.AudioFeatures.MEDIA_AUDIO);
                                    }
                                }
                                transitionTo(mConnected);
                            }
                        }
                        if (contextType == CONTEXT_TYPE_VOICE) {
                            if (mIsVoicePlaying) {
                                Log.i(TAG, "Streaming: stopped voice playing: " + mDevice);
                                mIsVoicePlaying = false;
                                mAcmService.onStreamStateChange(mDevice, false, CONTEXT_TYPE_VOICE);
                                updateStreamState(mDevice, BluetoothHeadset.STATE_AUDIO_DISCONNECTED, ApmConst.AudioFeatures.CALL_AUDIO);
                                if (!mAcmService.isPeerDeviceStreaming(mDevice, mSetId, CONTEXT_TYPE_VOICE)) {
                                    Log.i(TAG, "Streaming: last peer voice stream stopped " + mDevice);
                                    setCallAudioOn(false);
                                    setVoiceParameters(false);
                                }
                                if (mAcmService.isVoiceShoPendingStop()) {
                                    Log.i(TAG, "Voice SHO was pending earlier, complete now");
                                    mAcmService.resetVoiceShoPendingStop();
                                    StreamAudioService service = StreamAudioService.getStreamAudioService();
                                    if (service != null) {
                                        service.onActiveDeviceChange(mAcmService.getVoiceActiveDevice(), ApmConst.AudioFeatures.CALL_AUDIO);
                                    }
                                }
                                transitionTo(mConnected);
                            }
                        }
                    }
                    break;
                default:
                    Log.e(TAG, "Audio State Device: " + mDevice + " bad state: " + state);
                    break;
            }
        }

        private void processMetaDataEvent(int contextType) {
            Log.i(TAG, "Connected: processMetaDataEvent: contextType: " + contextType +
                                                         ", mDevice: " + mDevice);
            Integer mAudioType = -1;
            if (contextType == CONTENT_TYPE_COVERSATIONAL) {
                mAudioType = ApmConst.AudioFeatures.CALL_AUDIO;
            } else if (contextType == CONTENT_TYPE_MEDIA) {
                mAudioType = ApmConst.AudioFeatures.MEDIA_AUDIO;
            }
            ActiveDeviceManagerService mActiveDeviceManager = ActiveDeviceManagerService.get();
            mActiveDeviceManager.setActiveDevice(mDevice, mAudioType);
        }
    }

    private void updateConnectionState(BluetoothDevice device, int state,
            int audioType, boolean primeDevice) {
        StreamAudioService service = StreamAudioService.getStreamAudioService();
        if (service == null) {
            Log.d(TAG, "Stream audio service is null");
            return;
        }
        service.onConnectionStateChange(device, state, audioType, primeDevice);
    }

    private void updateStreamState(BluetoothDevice device, int state,
            int audioType) {
        StreamAudioService service = StreamAudioService.getStreamAudioService();
        if (service == null) {
            Log.d(TAG, "Stream audio service is null");
            return;
        }
        service.onStreamStateChange(device, state, audioType);
    }

    private String getFrameDuration() {
        BluetoothCodecConfig mCodecConfig = mVoiceCodecStatus.getCodecConfig();
        long cs1 = mCodecConfig.getCodecSpecific1() >> 32 & 0xff;
        if (cs1 == 0x00) {
            return "7500";
        } else {
            return "10000";
        }
    }

    private String getLc3BlocksPerSdu() {
        BluetoothCodecConfig mCodecConfig = mVoiceCodecStatus.getCodecConfig();
        long cs1 = mCodecConfig.getCodecSpecific1() >> 40 & 0xff;
        return Integer.toString((int)cs1);
    }

    private String getCodectype() {
        BluetoothCodecConfig mCodecConfig = mVoiceCodecStatus.getCodecConfig();
        int codec_type = mCodecConfig.getCodecType();
        if (codec_type == APTX_LEA_CODEC)
          return "APTX_LE";
        if (codec_type == APTX_R4_CODEC)
          return "APTX_R4";
        long cs3 = (mCodecConfig.getCodecSpecific3() >>
                (CS_PARAM_NUM_BITS * CS_PARAM_1ST_INDEX)) & CS_PARAM_IND_MASK;
        if (cs3 == CODEC_TYPE_LC3Q) {
          return "LC3Q";
        } else {
          return "LC3";
        }
    }

    private String getLc3qValues() {
        BluetoothCodecConfig mCodecConfig = mVoiceCodecStatus.getCodecConfig();
        long cs3 = mCodecConfig.getCodecSpecific3();
        long cs4 = mCodecConfig.getCodecSpecific4();

        String vsMetaDataLc3qVal = String.join(",", new String[]{
               String.format("%02X", ((cs4 >>
                (CS_PARAM_NUM_BITS * CS_PARAM_8TH_INDEX)) & CS_PARAM_IND_MASK)),
               String.format("%02X", ((cs4 >>
                (CS_PARAM_NUM_BITS * CS_PARAM_7TH_INDEX)) & CS_PARAM_IND_MASK)),
               String.format("%02X", ((cs4 >>
                (CS_PARAM_NUM_BITS * CS_PARAM_6TH_INDEX)) & CS_PARAM_IND_MASK)),
               String.format("%02X", ((cs4 >>
                (CS_PARAM_NUM_BITS * CS_PARAM_5TH_INDEX)) & CS_PARAM_IND_MASK)),
               String.format("%02X", ((cs4 >>
                (CS_PARAM_NUM_BITS * CS_PARAM_4TH_INDEX)) & CS_PARAM_IND_MASK)),
               String.format("%02X", ((cs4 >>
                (CS_PARAM_NUM_BITS * CS_PARAM_3RD_INDEX)) & CS_PARAM_IND_MASK)),
               String.format("%02X", ((cs4 >>
                (CS_PARAM_NUM_BITS * CS_PARAM_2ND_INDEX)) & CS_PARAM_IND_MASK)),
               String.format("%02X", ((cs4 >>
                (CS_PARAM_NUM_BITS * CS_PARAM_1ST_INDEX)) & CS_PARAM_IND_MASK)),
               String.format("%02X", ((cs3 >>
                (CS_PARAM_NUM_BITS * CS_PARAM_8TH_INDEX)) & CS_PARAM_IND_MASK)),
               String.format("%02X", (((cs3 >>
                (CS_PARAM_NUM_BITS * CS_PARAM_2ND_INDEX)) & CS_PARAM_IND_MASK) & 0x01)),
               String.format("%02X", ((cs3 >>
                (CS_PARAM_NUM_BITS * CS_PARAM_1ST_INDEX)) & CS_PARAM_IND_MASK)),
               String.format("%02X", ((cs3 >>
                (CS_PARAM_NUM_BITS * CS_PARAM_7TH_INDEX)) & CS_PARAM_IND_MASK)),
               String.format("%02X", ((cs3 >>
                (CS_PARAM_NUM_BITS * CS_PARAM_6TH_INDEX)) & CS_PARAM_IND_MASK)),
               String.format("%02X", ((cs3 >>
                (CS_PARAM_NUM_BITS * CS_PARAM_5TH_INDEX)) & CS_PARAM_IND_MASK)),
               String.format("%02X", ((cs3 >>
                (CS_PARAM_NUM_BITS * CS_PARAM_4TH_INDEX)) & CS_PARAM_IND_MASK)),
               String.format("%02X", ((cs3 >>
                (CS_PARAM_NUM_BITS * CS_PARAM_3RD_INDEX)) & CS_PARAM_IND_MASK))
          });
        Log.i(TAG, "getLc3qValues() for " + mDevice + ": " + vsMetaDataLc3qVal);
        return vsMetaDataLc3qVal;
    }

    private String getRxTxConfigIndex() {
        BluetoothCodecConfig mCodecConfig = mVoiceCodecStatus.getCodecConfig();
        int sampleRate = mCodecConfig.getSampleRate();
        long cs1 = mCodecConfig.getCodecSpecific1() >> 32 & 0xff;
        if (sampleRate == BluetoothCodecConfig.SAMPLE_RATE_8000) {
            if (cs1 == 0x01) {
                return "1";
            } else {
                return "0";
            }
        } else if (sampleRate == BluetoothCodecConfig.SAMPLE_RATE_16000) {
            if (cs1 == 0x01) {
                return "3";
            } else {
                return "2";
            }
        } else if (sampleRate == BluetoothCodecConfig.SAMPLE_RATE_32000) {
            if (cs1 == 0x01) {
                return "5";
            } else {
                return "4";
            }
        }
        return "0";
    }

    private void setVoiceParameters(boolean on) {
        if (!ApmConst.getQtiLeAudioEnabled()) {
            Log.i(TAG, "setVoiceParameters(): Qti LE-A not enabled, return");
            return;
        }

        String keyValuePairs;
        if (on && (mVoiceCodecStatus != null)) {
            keyValuePairs = String.join(";", new String[]{
                    BLE_AUDIO + "=" + BLE_AUDIO_FEATURE_ON,
                    CODEC_NAME + "=" + "LC3",
                    STREAM_MAP + "=" + "(0, 0, M, 0, 1, L),(1, 0, M, 1, 1, R)",
                    FRAME_DURATION + "=" + getFrameDuration(),
                    SDU_BLOCK + "=" + getLc3BlocksPerSdu(),
                    RXCONFIG_INDX + "=" + getRxTxConfigIndex(),
                    TXCONFIG_INDX + "=" + getRxTxConfigIndex(),
                    VERSION + "=" + "21",
                    VENDOR_META_DATA + "=" + getLc3qValues()
            });
        } else {
            keyValuePairs = String.join(";", new String[]{
                    BLE_AUDIO + "=" + BLE_AUDIO_FEATURE_OFF
            });
        }
        Log.i(TAG, "setVoiceParameters for " + mDevice + ": " + keyValuePairs);
        StreamAudioService service = StreamAudioService.getStreamAudioService();
        if (service != null) {
            service.setCallAudioParam(keyValuePairs);
        }
    }

    private void setCallAudioOn(boolean on) {
        if (!ApmConst.getQtiLeAudioEnabled()) {
            Log.i(TAG, "setCallAudioOn(): Qti LE-A not enabled, return");
            return;
        }

        Log.i(TAG, "set Call Audio On: " + on);
        StreamAudioService service = StreamAudioService.getStreamAudioService();
        if (service != null) {
            service.setCallAudioOn(on);
        }
    }

    int getConnectionState() {
        return mConnectionState;
    }

    int getCsipConnectionState() {
        return mCsipConnectionState;
    }

    BluetoothDevice getDevice() {
        return mDevice;
    }

    BluetoothDevice getPeerDevice() {
        BluetoothDevice d = null;
        List<BluetoothDevice> members = mAcmService.getCsipManager().getSetMembers(mSetId);
        if (members == null) {
            Log.d(TAG, "No set member found");
            return d;
        }
        Iterator<BluetoothDevice> i = members.iterator();
        if (i != null) {
            while (i.hasNext()) {
                d = i.next();
                if (!(Objects.equals(d, mDevice))) {
                    Log.d(TAG, "Device: " + d);
                    break;
                }
            }
        }
        return d;
    }

    void removeDevicefromBgWL() {
        Log.d(TAG, "remove device from BG WL");
        synchronized (mBluetoothGattLock) {
          if (mBluetoothGatt != null && mIsDeviceWhitelisted) {
              mIsDeviceWhitelisted = false;
              mBluetoothGatt.disconnect();
          }
        }
    }

    void addDeviceToBgWL() {
        Log.d(TAG,"addDeviceToBgWL");
        synchronized (mBluetoothGattLock) {
          if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
          }
          mBluetoothGatt = mDevice.connectGatt(mAcmService, true, mGattCallback,
                  BluetoothDevice.TRANSPORT_LE, false, (BluetoothDevice.PHY_LE_1M_MASK |
                  BluetoothDevice.PHY_LE_2M_MASK | BluetoothDevice.PHY_LE_CODED_MASK),
                  null, true);
        }
        mIsDeviceWhitelisted = true;
    }
    boolean isConnected() {
        synchronized (this) {
            return (getConnectionState() == BluetoothProfile.STATE_CONNECTED);
        }
    }

    boolean isCsipLockRequested() {
        synchronized (this) {
            return mCsipLockRequested;
        }
    }

    boolean isMusicPlaying() {
        synchronized (this) {
            return mIsMusicPlaying;
        }
    }

    boolean isVoicePlaying() {
        synchronized (this) {
            return mIsVoicePlaying;
        }
    }

    int getMusicConnectionState() {
        Log.d(TAG, "mMusicConnectionState: " + mMusicConnectionState);
        return mMusicConnectionState;
    }

    int getVoiceConnectionState() {
        Log.d(TAG, "mVoiceConnectionState: " + mVoiceConnectionState);
        return mVoiceConnectionState;
    }

    public @SourceCodecType int getLeAudioCodecType() {
        return BluetoothLeAudioCodecConfig.SOURCE_CODEC_TYPE_LC3;
    }

    public @CodecPriority int getLeAudioCodecPriority() {
        return BluetoothLeAudioCodecConfig.CODEC_PRIORITY_HIGHEST;
    }

    public @ChannelCount int getLeAudioChannelCount() {
        return BluetoothLeAudioCodecConfig.CHANNEL_COUNT_1;
    }

    public @FrameDuration int getLeAudioFrameDuration(BluetoothCodecStatus newCodecStatus) {
        BluetoothCodecConfig mCodecConfig = newCodecStatus.getCodecConfig();
        int codec_type = mCodecConfig.getCodecType();
        if (codec_type == APTX_LEA_CODEC) // aptx_le
          return BluetoothLeAudioCodecConfig.FRAME_DURATION_15000;
        if (codec_type == APTX_R4_CODEC)
          return BluetoothLeAudioCodecConfig.FRAME_DURATION_10000;

        long cs1 = mCodecConfig.getCodecSpecific1() >> 32 & 0xff;
        int frameDuration = BluetoothLeAudioCodecConfig.FRAME_DURATION_NONE;

        if (cs1 == 0x00) {
            frameDuration = BluetoothLeAudioCodecConfig.FRAME_DURATION_7500;
        } else {
            frameDuration = BluetoothLeAudioCodecConfig.FRAME_DURATION_10000;
        }
        return frameDuration;
    }

    public List<BluetoothLeAudioCodecConfig> getCodecsLocalCapabilities(
                                            BluetoothCodecStatus newCodecStatus) {
        List<BluetoothLeAudioCodecConfig> local_caps = new ArrayList<>();
        List<BluetoothCodecConfig> local_capability = newCodecStatus.getCodecsLocalCapabilities();

        for (BluetoothCodecConfig caps : local_capability) {
            long cs3 = caps.getCodecSpecific3();
            int frame_duration = BluetoothLeAudioCodecConfig.FRAME_DURATION_10000;
            int codec_type = BluetoothLeAudioCodecConfig.SOURCE_CODEC_TYPE_LC3;
            if (caps.getCodecType() == APTX_LEA_CODEC) {
              codec_type = BluetoothLeAudioCodecConfig.SOURCE_CODEC_TYPE_APTX_ADAPTIVE_LE;
              cs3 |= QHS_SUPPORT_AVAILABLE;
              frame_duration = BluetoothLeAudioCodecConfig.FRAME_DURATION_15000;
            } else if (caps.getCodecType() == APTX_R4_CODEC) {
              codec_type = FWK_SOURCE_CODEC_TYPE_APTX_ADAPTIVE_R4;//Temp. changes
              cs3 |= QHS_SUPPORT_AVAILABLE;
            }

            int octetsPerFrame = (int)(caps.getCodecSpecific2() >> 32 & 0xffff);

            int minOctetsPerFrame =
                 (int)((caps.getCodecSpecific2() & 0xffffffff) & 0xFFFF);

            int maxOctetsPerFrame =
                 (int)(((caps.getCodecSpecific2() & 0xffffffff) & 0xFFFF0000) >> 16);

            BluetoothLeAudioCodecConfig leCaps =
                   new BluetoothLeAudioCodecConfig.Builder()
                      .setCodecType(codec_type)
                      .setCodecPriority(BluetoothLeAudioCodecConfig.CODEC_PRIORITY_DEFAULT)
                      .setSampleRate(FreqToLeSampleRate(caps.getSampleRate()))
                      .setBitsPerSample(BluetoothLeAudioCodecConfig.BITS_PER_SAMPLE_24)
                      .setChannelCount(caps.getChannelMode())
                      .setFrameDuration(frame_duration)
                      .setOctetsPerFrame(octetsPerFrame)
                      .setMinOctetsPerFrame(minOctetsPerFrame)
                      .setMaxOctetsPerFrame(maxOctetsPerFrame)
                      .setCodecSpecific1(0)
                      .setCodecSpecific2(0)
                      .setCodecSpecific3(cs3)
                      .setCodecSpecific4(0)
                      .build();

            local_caps.add(leCaps);
        }
        return local_caps;
   }

   public List<BluetoothLeAudioCodecConfig> getCodecsSelectableCapabilities(
                                            BluetoothCodecStatus newCodecStatus) {
        List<BluetoothLeAudioCodecConfig> selectableCaps = new ArrayList<>();
        List<BluetoothCodecConfig> selectable_caps = newCodecStatus.getCodecsSelectableCapabilities();

        for (BluetoothCodecConfig caps: selectable_caps) {
            long cs3 = caps.getCodecSpecific3();
            int frame_duration = BluetoothLeAudioCodecConfig.FRAME_DURATION_10000;
            int codec_type = BluetoothLeAudioCodecConfig.SOURCE_CODEC_TYPE_LC3;
            if (caps.getCodecType() == APTX_LEA_CODEC) {
              codec_type = BluetoothLeAudioCodecConfig.SOURCE_CODEC_TYPE_APTX_ADAPTIVE_LE;
              cs3 |= QHS_SUPPORT_AVAILABLE;
              frame_duration = BluetoothLeAudioCodecConfig.FRAME_DURATION_15000;
            } else if (caps.getCodecType() == APTX_R4_CODEC) {
              codec_type = FWK_SOURCE_CODEC_TYPE_APTX_ADAPTIVE_R4;//Temp. changes
              cs3 |= QHS_SUPPORT_AVAILABLE;
            }

            int octetsPerFrame = (int)(caps.getCodecSpecific2() >> 32 & 0xffff);

            int minOctetsPerFrame =
                 (int)((caps.getCodecSpecific2() & 0xffffffff) & 0xFFFF);

            int maxOctetsPerFrame =
                 (int)(((caps.getCodecSpecific2() & 0xffffffff) & 0xFFFF0000) >> 16);

            BluetoothLeAudioCodecConfig leCaps =
                   new BluetoothLeAudioCodecConfig.Builder()
                      .setCodecType(codec_type)
                      .setCodecPriority(BluetoothLeAudioCodecConfig.CODEC_PRIORITY_DEFAULT)
                      .setSampleRate(FreqToLeSampleRate(caps.getSampleRate()))
                      .setBitsPerSample(BluetoothLeAudioCodecConfig.BITS_PER_SAMPLE_24)
                      .setChannelCount(caps.getChannelMode())
                      .setFrameDuration(frame_duration)
                      .setOctetsPerFrame(octetsPerFrame)
                      .setMinOctetsPerFrame(minOctetsPerFrame)
                      .setMaxOctetsPerFrame(maxOctetsPerFrame)
                      .setCodecSpecific1(0)
                      .setCodecSpecific2(0)
                      .setCodecSpecific3(cs3)
                      .setCodecSpecific4(0)
                      .build();

            selectableCaps.add(leCaps);
        }
        return selectableCaps;
   }

   public BluetoothLeAudioCodecConfig GetInputCodecCapabilities(
                                                   BluetoothCodecStatus newCodecStatus) {
        BluetoothCodecConfig mCodecConfig = newCodecStatus.getCodecConfig();
        long cs3 = mCodecConfig.getCodecSpecific3();

        BluetoothLeAudioCodecConfig inputleCaps =
               new BluetoothLeAudioCodecConfig.Builder()
                  .setCodecType(BluetoothLeAudioCodecConfig.SOURCE_CODEC_TYPE_LC3)
                  .setCodecPriority(BluetoothLeAudioCodecConfig.CODEC_PRIORITY_DEFAULT)
                  .setSampleRate(BluetoothLeAudioCodecConfig.SAMPLE_RATE_48000)
                  .setBitsPerSample(BluetoothLeAudioCodecConfig.BITS_PER_SAMPLE_24)
                  .setChannelCount(BluetoothLeAudioCodecConfig.CHANNEL_COUNT_1)
                  .setFrameDuration(BluetoothLeAudioCodecConfig.FRAME_DURATION_10000)
                  .setOctetsPerFrame(100)
                  .setMinOctetsPerFrame(20)
                  .setMaxOctetsPerFrame(155)
                  .setCodecSpecific1(0)
                  .setCodecSpecific2(0)
                  .setCodecSpecific3(cs3)
                  .setCodecSpecific4(0)
                  .build();
        return inputleCaps;
   }

   public BluetoothLeAudioCodecConfig GetInputAAR4CodecCapabilities() {
        long cs3 = 0;
        cs3 |= LE_AUDIO_AVAILABLE_LICENSED;
        BluetoothLeAudioCodecConfig inputleCaps =
               new BluetoothLeAudioCodecConfig.Builder()
                  .setCodecType(FWK_SOURCE_CODEC_TYPE_APTX_ADAPTIVE_R4)/*Temp. changes*/
                  .setCodecPriority(BluetoothLeAudioCodecConfig.CODEC_PRIORITY_DEFAULT)
                  .setSampleRate(BluetoothLeAudioCodecConfig.SAMPLE_RATE_48000)
                  .setBitsPerSample(BluetoothLeAudioCodecConfig.BITS_PER_SAMPLE_24)
                  .setChannelCount(BluetoothLeAudioCodecConfig.CHANNEL_COUNT_1)
                  .setFrameDuration(BluetoothLeAudioCodecConfig.FRAME_DURATION_10000)
                  .setOctetsPerFrame(100)
                  .setMinOctetsPerFrame(20)
                  .setMaxOctetsPerFrame(155)
                  .setCodecSpecific1(0)
                  .setCodecSpecific2(0)
                  .setCodecSpecific3(cs3)
                  .setCodecSpecific4(0)
                  .build();
        return inputleCaps;
   }

   public List<BluetoothLeAudioCodecConfig> PrepareInputCodecCapabilities(
                                             BluetoothCodecStatus newCodecStatus) {
        List<BluetoothLeAudioCodecConfig> inputCaps = new ArrayList<>();
        BluetoothLeAudioCodecConfig leCaps = GetInputCodecCapabilities(newCodecStatus);
        inputCaps.add(leCaps);
        inputCaps.add(GetInputAAR4CodecCapabilities());
        return inputCaps;
   }

    private void processTransitionContextState(IState prevState, IState nextState, int contextType) {
        int pState = AcmStateToBluetoothProfileState(prevState);
        int nState = AcmStateToBluetoothProfileState(nextState);
        if (contextType == CONTEXT_TYPE_MUSIC) {
            mLastMusicConnectionState = pState;
            mMusicConnectionState = nState;
        } else if (contextType == CONTEXT_TYPE_VOICE) {
            mLastVoiceConnectionState = pState;
            mVoiceConnectionState = nState;
        } else if (contextType == CONTEXT_TYPE_MUSIC_VOICE) {
            mLastMusicConnectionState = pState;
            mLastVoiceConnectionState = pState;
            mMusicConnectionState = nState;
            mVoiceConnectionState = nState;
        }
    }

    // NOTE: This event is processed in any state
    @VisibleForTesting
    void processCodecConfigEvent(BluetoothCodecStatus newCodecStatus, int contextType) {
        Log.d(TAG,"ProcessCodecConfigEvent: context type :" + contextType);
        boolean isQtiLeAudioEnabled = ApmConst.getQtiLeAudioEnabled();
        if (contextType == CONTEXT_TYPE_MUSIC) {
            BluetoothCodecConfig mCodecConfig = newCodecStatus.getCodecConfig();
            long cs3 = mCodecConfig.getCodecSpecific3();
            if ((mCodecConfig.getCodecType() == APTX_LEA_CODEC) ||
                (mCodecConfig.getCodecType() == APTX_R4_CODEC)) {
              cs3 |= QHS_SUPPORT_AVAILABLE;
            }
            BluetoothCodecConfig mCodecConfigLc3 = new BluetoothCodecConfig(
                                                       mCodecConfig.getCodecType(),
                                                       mCodecConfig.getCodecPriority(),
                                                       mCodecConfig.getSampleRate(),
                                                       mCodecConfig.getBitsPerSample(),
                                                       mCodecConfig.getChannelMode(),
                                                       mCodecConfig.getCodecSpecific1(),
                                                       mCodecConfig.getCodecSpecific2(),
                                                       cs3,
                                                       mCodecConfig.getCodecSpecific4());

            mMusicCodecStatus = new BluetoothCodecStatus(
                                mCodecConfigLc3,
                                newCodecStatus.getCodecsLocalCapabilities(),
                                newCodecStatus.getCodecsSelectableCapabilities());

            StreamAudioService service = StreamAudioService.getStreamAudioService();
            if (!isQtiLeAudioEnabled) {
                int octetsPerFrame = (int)(mCodecConfig.getCodecSpecific2() >> 32 & 0xffff);

                int minOctetsPerFrame =
                     (int)((mCodecConfig.getCodecSpecific2() & 0xffffffff) & 0xFFFF);

                int maxOctetsPerFrame =
                     (int)(((mCodecConfig.getCodecSpecific2() & 0xffffffff) & 0xFFFF0000) >> 16);

                int codec_type = BluetoothLeAudioCodecConfig.SOURCE_CODEC_TYPE_LC3;
                if (mCodecConfig.getCodecType() == APTX_LEA_CODEC)
                  codec_type = BluetoothLeAudioCodecConfig.SOURCE_CODEC_TYPE_APTX_ADAPTIVE_LE;
                if (mCodecConfig.getCodecType() == APTX_R4_CODEC)
                  codec_type = FWK_SOURCE_CODEC_TYPE_APTX_ADAPTIVE_R4;//Temp. changes

                BluetoothLeAudioCodecConfig mLeAudioOuptutCodecConfig =
                       new BluetoothLeAudioCodecConfig.Builder()
                           .setCodecType(codec_type)
                           .setCodecPriority(BluetoothLeAudioCodecConfig.CODEC_PRIORITY_HIGHEST)
                           .setSampleRate(FreqToLeSampleRate(mCodecConfig.getSampleRate()))
                           .setBitsPerSample(mCodecConfig.getBitsPerSample())
                           .setChannelCount(BluetoothLeAudioCodecConfig.CHANNEL_COUNT_1)
                           .setFrameDuration(getLeAudioFrameDuration(newCodecStatus))
                           .setOctetsPerFrame(octetsPerFrame)
                           .setMinOctetsPerFrame(minOctetsPerFrame)
                           .setMaxOctetsPerFrame(maxOctetsPerFrame)
                           .setCodecSpecific1(mCodecConfig.getCodecSpecific1())
                           .setCodecSpecific2(mCodecConfig.getCodecSpecific2())
                           .setCodecSpecific3(cs3)
                           .setCodecSpecific4(mCodecConfig.getCodecSpecific4())
                           .build();

                BluetoothLeAudioCodecConfig mLeAudioInputCodecConfig =
                           GetInputCodecCapabilities(newCodecStatus);

                mLeAudioCodecStatus = new BluetoothLeAudioCodecStatus(
                                mLeAudioInputCodecConfig,
                                mLeAudioOuptutCodecConfig,
                                PrepareInputCodecCapabilities(newCodecStatus),
                                getCodecsLocalCapabilities(newCodecStatus),
                                PrepareInputCodecCapabilities(newCodecStatus),
                                getCodecsSelectableCapabilities(newCodecStatus));
                Log.d(TAG,"ProcessCodecConfigEvent: mLeAudioCodecStatus :" + mLeAudioCodecStatus);
                if (service != null) {
                    service.onLeCodecConfigChange(mDevice, mLeAudioCodecStatus, contextType);
                }
            }
            if (service != null) {
                service.onMediaCodecConfigChange(mDevice, mMusicCodecStatus, contextType);
            }
        } else if (contextType == CONTEXT_TYPE_VOICE) {
            mVoiceCodecStatus = newCodecStatus;
        } else if (contextType == CONTEXT_TYPE_MUSIC_VOICE) {
            BluetoothCodecConfig mCodecConfig = newCodecStatus.getCodecConfig();
            long cs3 = mCodecConfig.getCodecSpecific3();
            if (mCodecConfig.getCodecType() == APTX_R4_CODEC) {
              cs3 |= QHS_SUPPORT_AVAILABLE;
            }
            StreamAudioService service = StreamAudioService.getStreamAudioService();

            if (!isQtiLeAudioEnabled) {
                int octetsPerFrame = (int)(mCodecConfig.getCodecSpecific2() >> 32 & 0xffff);

                int minOctetsPerFrame =
                     (int)((mCodecConfig.getCodecSpecific2() & 0xffffffff) & 0xFFFF);

                int maxOctetsPerFrame =
                     (int)(((mCodecConfig.getCodecSpecific2() & 0xffffffff) & 0xFFFF0000) >> 16);

                int codec_type = BluetoothLeAudioCodecConfig.SOURCE_CODEC_TYPE_LC3;
                if (mCodecConfig.getCodecType() == APTX_R4_CODEC)
                  codec_type = FWK_SOURCE_CODEC_TYPE_APTX_ADAPTIVE_R4;//Temp. changes


                BluetoothLeAudioCodecConfig mLeAudioInputCodecConfig =
                       new BluetoothLeAudioCodecConfig.Builder()
                           .setCodecType(codec_type)
                           .setCodecPriority(BluetoothLeAudioCodecConfig.CODEC_PRIORITY_HIGHEST)
                           .setSampleRate(FreqToLeSampleRate(mCodecConfig.getSampleRate()))
                           .setBitsPerSample(mCodecConfig.getBitsPerSample())
                           .setChannelCount(BluetoothLeAudioCodecConfig.CHANNEL_COUNT_1)
                           .setFrameDuration(getLeAudioFrameDuration(newCodecStatus))
                           .setOctetsPerFrame(octetsPerFrame)
                           .setMinOctetsPerFrame(minOctetsPerFrame)
                           .setMaxOctetsPerFrame(maxOctetsPerFrame)
                           .setCodecSpecific1(mCodecConfig.getCodecSpecific1())
                           .setCodecSpecific2(mCodecConfig.getCodecSpecific2())
                           .setCodecSpecific3(cs3)
                           .setCodecSpecific4(mCodecConfig.getCodecSpecific4())
                           .build();

                mLeAudioCodecStatus = new BluetoothLeAudioCodecStatus(
                                mLeAudioInputCodecConfig,
                                mLeAudioCodecStatus.getOutputCodecConfig(),
                                PrepareInputCodecCapabilities(newCodecStatus),
                                mLeAudioCodecStatus.getOutputCodecLocalCapabilities(),
                                PrepareInputCodecCapabilities(newCodecStatus),
                                mLeAudioCodecStatus.getOutputCodecSelectableCapabilities());
                Log.d(TAG,"ProcessCodecConfigEvent: mLeAudioCodecStatus :" + mLeAudioCodecStatus);
                service.onLeCodecConfigChange(mGroupAddress, mLeAudioCodecStatus, contextType);
            }
        }
    }

    public BluetoothLeAudioCodecStatus getLeAudioCodecStatus() {
        return mLeAudioCodecStatus;
    }

    @Override
    protected String getLogRecString(Message msg) {
        StringBuilder builder = new StringBuilder();
        builder.append(messageWhatToString(msg.what));
        builder.append(": ");
        builder.append("arg1=")
                .append(msg.arg1)
                .append(", arg2=")
                .append(msg.arg2)
                .append(", obj=")
                .append(msg.obj);
        return builder.toString();
    }

    private static boolean sameSelectableCodec(BluetoothCodecStatus prevCodecStatus,
            BluetoothCodecStatus newCodecStatus) {
        if (prevCodecStatus == null) {
            return false;
        }
        List<BluetoothCodecConfig> c1 = prevCodecStatus.getCodecsSelectableCapabilities();
        List<BluetoothCodecConfig> c2 = newCodecStatus.getCodecsSelectableCapabilities();
        if (c1 == null) {
            return (c2 == null);
        }
        if (c2 == null) {
            return false;
        }
        if (c1.size() != c2.size()) {
            return false;
        }
        return c1.containsAll(c2);
    }

    private static String messageWhatToString(int what) {
        switch (what) {
            case CONNECT:
                return "CONNECT";
            case DISCONNECT:
                return "DISCONNECT";
            case STACK_EVENT:
                return "STACK_EVENT";
            case CONNECT_TIMEOUT:
                return "CONNECT_TIMEOUT";
            default:
                break;
        }
        return Integer.toString(what);
    }

    private static String contextTypeToString(int contextType) {
        switch (contextType) {
            case CONTEXT_TYPE_UNKNOWN:
                return "UNKNOWN";
            case CONTEXT_TYPE_MUSIC:
                return "MEDIA";
            case CONTEXT_TYPE_VOICE:
                return "CONVERSATIONAL";
            case CONTEXT_TYPE_MUSIC_VOICE:
                return "MEDIA+CONVERSATIONAL";
            default:
                break;
        }
        return Integer.toString(contextType);
    }

    private static String profileStateToString(int state) {
        switch (state) {
            case BluetoothProfile.STATE_DISCONNECTED:
                return "DISCONNECTED";
            case BluetoothProfile.STATE_CONNECTING:
                return "CONNECTING";
            case BluetoothProfile.STATE_CONNECTED:
                return "CONNECTED";
            case BluetoothProfile.STATE_DISCONNECTING:
                return "DISCONNECTING";
            default:
                break;
        }
        return Integer.toString(state);
    }

    /*private static String musicAudioStateToString(int state) {
        switch (state) {
            case BluetoothA2dp.STATE_PLAYING:
                return "PLAYING";
            case BluetoothA2dp.STATE_NOT_PLAYING:
                return "NOT_PLAYING";
            default:
                break;
        }
        return Integer.toString(state);
    }*/

    int FreqToLeSampleRate(int sample_rate) {
        int le_freq = 0;
        int freq = 1;
        while (sample_rate != 0) {
          if ((sample_rate & 0x1) != 0) {
            le_freq |= ConvertToLEfreq(freq);
          }
          sample_rate >>= 1;
          freq = freq << 1;
        }
        Log.e(TAG, "le_freq " + le_freq);
        return le_freq;
    }

    int ConvertToLEfreq (int freq) {
        switch (freq) {
          case BluetoothCodecConfig.SAMPLE_RATE_44100:
            return BluetoothLeAudioCodecConfig.SAMPLE_RATE_44100;
          case BluetoothCodecConfig.SAMPLE_RATE_48000:
            return BluetoothLeAudioCodecConfig.SAMPLE_RATE_48000;
          case BluetoothCodecConfig.SAMPLE_RATE_96000:
            return BluetoothLeAudioCodecConfig.SAMPLE_RATE_96000;
          case BluetoothCodecConfig.SAMPLE_RATE_16000:
            return BluetoothLeAudioCodecConfig.SAMPLE_RATE_16000;
          case BluetoothCodecConfig.SAMPLE_RATE_24000:
            return BluetoothLeAudioCodecConfig.SAMPLE_RATE_24000;
          case BluetoothCodecConfig.SAMPLE_RATE_32000:
            return BluetoothLeAudioCodecConfig.SAMPLE_RATE_32000;
          case BluetoothCodecConfig.SAMPLE_RATE_8000:
            return BluetoothLeAudioCodecConfig.SAMPLE_RATE_8000;
          case BluetoothCodecConfig.SAMPLE_RATE_192000:
            return FWK_SAMPLE_RATE_192000;// Temp. changes
          default:
            return BluetoothLeAudioCodecConfig.SAMPLE_RATE_NONE;
        }
    }

    private static String voiceAudioStateToString(int state) {
        switch (state) {
          case BluetoothHeadset.STATE_AUDIO_DISCONNECTED:
            return "AUDIO_DISCONNECTED";
          case BluetoothHeadset.STATE_AUDIO_CONNECTING:
            return "AUDIO_CONNECTING";
          case BluetoothHeadset.STATE_AUDIO_CONNECTED:
            return "AUDIO_CONNECTED";
          case BluetoothHeadset.STATE_AUDIO_DISCONNECTING:
            return "AUDIO_DISCONNECTING";
          default:
            break;
        }
        return Integer.toString(state);
    }

    private static int AcmStateToBluetoothProfileState(IState state) {
        if (state instanceof Disconnected) {
            return BluetoothProfile.STATE_DISCONNECTED;
        } else if (state instanceof Connecting) {
            return BluetoothProfile.STATE_CONNECTING;
        } else if (state instanceof Connected) {
            return BluetoothProfile.STATE_CONNECTED;
        } else if (state instanceof Disconnecting) {
            return BluetoothProfile.STATE_DISCONNECTING;
        }
        Log.w(TAG, "Unknown State");
        return BluetoothProfile.STATE_DISCONNECTED;
    }

    public void dump(StringBuilder sb) {
        ProfileService.println(sb, "mDevice: " + mDevice);
        ProfileService.println(sb, "  StateMachine: " + this.toString());
        ProfileService.println(sb, "  mIsMusicPlaying: " + mIsMusicPlaying);
        synchronized (this) {
            if (mVoiceCodecStatus != null) {
                ProfileService.println(sb, " Voice mCodecConfig: " + mVoiceCodecStatus.getCodecConfig());
            }
            if (mMusicCodecStatus != null) {
                ProfileService.println(sb, " Music mCodecConfig: " + mMusicCodecStatus.getCodecConfig());
            }
        }
        // Dump the state machine logs
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        super.dump(new FileDescriptor(), printWriter, new String[]{});
        printWriter.flush();
        stringWriter.flush();
        ProfileService.println(sb, "  StateMachineLog:");
        Scanner scanner = new Scanner(stringWriter.toString());
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            ProfileService.println(sb, "    " + line);
        }
        scanner.close();
    }

    @Override
    protected void log(String msg) {
        if (DBG) {
            super.log(msg);
        }
    }
}
