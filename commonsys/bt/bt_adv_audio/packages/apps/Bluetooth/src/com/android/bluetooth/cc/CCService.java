/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
*/
/*
 *Copyright (c) 2020-2021, The Linux Foundation. All rights reserved.
 *Not a contribution
 */

/*
 * Copyright 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.bluetooth.cc;

import static android.Manifest.permission.BLUETOOTH_CONNECT;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.content.SharedPreferences;
import android.util.Log;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Queue;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

import android.os.Message;
import android.os.Binder;
import android.os.IBinder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemProperties;
import android.os.Bundle;
import android.util.Log;

import com.android.internal.util.ArrayUtils;
import com.android.bluetooth.Utils;
import com.android.bluetooth.btservice.AdapterService;
import com.android.bluetooth.btservice.ProfileService;
import com.android.bluetooth.acm.AcmService;
import com.android.bluetooth.apm.ApmConst;
import com.android.bluetooth.apm.ApmConstIntf;
import com.android.bluetooth.apm.CallAudio;
import com.android.bluetooth.apm.CallControl;
import com.android.bluetooth.apm.ActiveDeviceManagerService;
import com.android.bluetooth.le_audio.LeAudioService;
import com.android.bluetooth.hfp.HeadsetService;
import com.android.bluetooth.CsipWrapper;

import java.util.Objects;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Iterator;


/**
 * Provides Bluetooth CC profile as a service in the Bluetooth application.
 * @hide
 */
public class CCService extends ProfileService {

    private static final String TAG = "CCService";
    private static final String DISABLE_INBAND_RINGING_PROPERTY =
             "persist.bluetooth.lea.disableinbandringing";
    private static final String CC_PTS_ENABLED_PROPERTY =
             "persist.vendor.btstack.cc.certification";
    private static final boolean DBG = true;
    private static CCService sCCService;
    private BroadcastReceiver mBondStateChangedReceiver;

    private int mCCId = 0xFF;
    private BluetoothDevice mActiveDevice;
    private boolean mCallControlInitialized = false;
    private BluetoothDevice mCallOriginatedDevice = null;
    private AdapterService mAdapterService;
    private CCNativeInterface mNativeInterface;
    private CallAudio mCallAudio = null;
    private ActiveDeviceManagerService mActiveDevMgrService = null;
    private Context mContext  = null;;
    private CcsMessageHandler mHandler;
    private int mMaxConnectedAudioDevices = 1;
    private boolean  InBandRingtoneSupport = false;
    private boolean mVirtualCallStarted = false;
    private boolean mStarted;
    private boolean mCreated;
    private boolean mIsPendingAnswerCall = false;
    private static int mLatestActiveCallIndex = 0;
    private static int mLatestHeldCallIndex = 0;
    private CallControlState mPrevTelephonyState = null;
    private CallControlState mCurrentTelephonyState = null;
    private HashMap<Integer, CallControlState> mCallStateList = null;
    private HashMap<Integer, CallControlState> mPrevCallStateList = null;
    private ConcurrentLinkedQueue<ArrayList<CallControlState>> mPendingCallStates =
                                 new ConcurrentLinkedQueue<ArrayList<CallControlState>>();
    private Queue<Integer> mLccTobeQueued = null;
    private Queue<Integer> mLccWaitForResponseQ = null;

    private static final int FLAGS_DIRECTION_BIT = 0x0001;
    private static final int CC_SIGNAL_STRENGTH_FACTOR = 20;

    private static final int CC_CONTENT_CONTROL_ID = 77;
    private static final int CC_OPTIONAL_LOCAL_HOLD_FEAT = 0x01;
    private static final int CC_OPTIONAL_JOIN_FEAT = 0x02;
    private static final int CALL_CONTROL_OPTIONAL_FEATURES =
                        CC_OPTIONAL_LOCAL_HOLD_FEAT|CC_OPTIONAL_JOIN_FEAT;
    //native event
    static final int EVENT_TYPE_CONNECTION_STATE_CHANGED = 1;
    static final int EVENT_TYPE_CALL_CONTROL_POINT_CHANGED = 2;
    //CC to JNI update
    static final int UPDATE_BEARER_NAME = 3;
    static final int UPDATE_BEARER_TECH = 4;
    static final int UPDATE_STATUS_FLAGS = 5;
    static final int UPDATE_SIGNAL_STRENGTH = 6;
    static final int UPDATE_BEARERLIST_SUPPORTED = 7;
    static final int UPDATE_CONTENT_CONTROL_ID = 8;
    static final int UPDATE_CALL_STATE = 9;
    static final int UPDATE_CALL_CONTROL_OPCODES_SUPPORTED = 10;
    static final int UPDATE_CALL_CONTROL_RESPONSE = 11;
    static final int UPDATE_INCOMING_CALL = 12;
    static final int PROCESS_CALL_STATE = 13;
    static final int PROCESS_PHONE_STATE_CHANGED = 14;
    static final int ACTIVE_DEVICE_CHANGED = 15;
    static final int ACTIVE_PROFILE_UPDATED = 16;
    static final int PROCESS_CALL_CONTROL_OPS = 17;

    @Override
    protected IProfileServiceBinder initBinder() {
        return new CcBinder(this);
    }

    @Override
    protected void create() {
        Log.i(TAG, "create()");
        if (mCreated) {
            throw new IllegalStateException("create() called twice");
        }
        mCreated = true;
    }

    @Override
    protected void cleanup() {
        Log.i(TAG, "cleanup()");
        if (mNativeInterface != null) {
           if (mCallControlInitialized == true) {
              mNativeInterface.cleanup();
           }
        }
        while (mPendingCallStates.isEmpty() != true)
        {
           mPendingCallStates.clear();
        }

    }

    @Override
    protected boolean start() {
        Log.i(TAG, "start()");
        if (sCCService != null) {
            Log.w(TAG, "CCService is already running");
            return true;
        }
        if (DBG) {
            Log.d(TAG, "Create CCService Instance");
        }

        mContext = this;
        mAdapterService = Objects.requireNonNull(AdapterService.getAdapterService(),
                "AdapterService cannot be null when CCService starts");
        mNativeInterface = Objects.requireNonNull(CCNativeInterface.getInstance(),
                "CcNativeInterface cannot be null when CcService starts");
        // Step 2: Get maximum number of connected audio devices
        mMaxConnectedAudioDevices = mAdapterService.getMaxConnectedAudioDevices();
        Log.i(TAG, "Max connected audio devices set to " + mMaxConnectedAudioDevices);

        if (mHandler != null) {
           mHandler = null;
        }
        HandlerThread thread = new HandlerThread("BluetoothCCSHandler");
        thread.start();
        Looper looper = thread.getLooper();
        mHandler = new CcsMessageHandler(looper);
        //APM's CallControl and CallAudio initialization
        CallControl.init(mContext);
        mCallAudio = CallAudio.init(mContext);
        setCCService(this);
        mCallControlInitialized = false;
        mNativeInterface.init(mMaxConnectedAudioDevices,isInbandRingingEnabled());
        Log.d(TAG, "cc native init done");
        IntentFilter filter = new IntentFilter();
        //mSystemInterface = HeadsetService.getSystemInterfaceObj();
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        mBondStateChangedReceiver = new BondStateChangedReceiver();
        mContext.registerReceiver(mBondStateChangedReceiver, filter, Context.RECEIVER_EXPORTED);
        mCallStateList = new HashMap<> ();
        mPrevCallStateList = new HashMap<> ();
        mLccWaitForResponseQ = new LinkedList<> ();
        mLccTobeQueued = new LinkedList<> ();
        mActiveDevMgrService = ActiveDeviceManagerService.get();
        return true;
    }

    @Override
    protected boolean stop() {
        Log.i(TAG, "stop()");
        if (sCCService == null) {
           Log.w(TAG, "stop() called before start()");
           return true;
        }
        setCCService(null);
        // Cleanup native interface
        if (mCallControlInitialized == true) {
           mNativeInterface.cleanup();
           mNativeInterface = null;
        }
        mContext.unregisterReceiver(mBondStateChangedReceiver);
        // Clear AdapterService
        mAdapterService = null;
        mMaxConnectedAudioDevices = 1;
        mCallOriginatedDevice = null;
        CallControl.listenForPhoneState(PhoneStateListener.LISTEN_NONE);
        mCallControlInitialized = false;
        return true;
    }

    private static synchronized void setCCService(CCService instance) {
        if (DBG) {
            Log.d(TAG, "setCCService(): set to: " + instance);
        }
        sCCService = instance;
    }

    public static synchronized CCService getCCService() {
        if (sCCService == null) {
            Log.w(TAG, "getCCService(): service is null");
            return null;
        }
        return sCCService;
    }

    public boolean updateBearerProviderName(String name) {
       Message msg = mHandler.obtainMessage();
       msg.what = UPDATE_BEARER_NAME;
       msg.obj = name;
       mHandler.sendMessage(msg);
       return true;
    }

    public void HandleCcOps(int op, int[] call_indices, int count,
                               byte[] dialNumber, byte[] address) {
        Log.d(TAG, "HandleCcOps(): op: " + op);
        Bundle data = new Bundle();
        Message msg = mHandler.obtainMessage(PROCESS_CALL_CONTROL_OPS);
        data.putInt("op", op);
        data.putIntegerArrayList("call_indices",
                      IntStream.of(call_indices)
                      .boxed()
                      .collect(Collectors.toCollection(ArrayList::new)));
        data.putInt("count", count);
        data.putByteArray("dialNumber", dialNumber);
        data.putByteArray("BdAddress", address);
        msg.setData(data);
        mHandler.sendMessage(msg);
        Log.d(TAG, "HandleCcOps(): Exit: ");
    }

    public boolean updateBearerProviderTechnology (int network_type) {
        int tech_type;
        switch(network_type) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_GSM:
                 tech_type = 8;
                 break;
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
               tech_type = 1;
               break;
            case TelephonyManager.NETWORK_TYPE_LTE:
            case TelephonyManager.NETWORK_TYPE_LTE_CA:
               tech_type =2;
               break;
            case TelephonyManager.NETWORK_TYPE_NR:
               tech_type = 5;
               break;
            default:
              tech_type = 2;
        }
        Log.d(TAG, "Bearer technology set to: " + tech_type);
        Message msg = mHandler.obtainMessage();
        msg.what = UPDATE_BEARER_TECH;
        msg.arg1 = tech_type;
        mHandler.sendMessage(msg);
        return true;
    }

    public boolean updateSignalStrength(int signal) {
        Message msg = mHandler.obtainMessage();
        msg.what = UPDATE_SIGNAL_STRENGTH;
        msg.arg1 = signal*CC_SIGNAL_STRENGTH_FACTOR;
        mHandler.sendMessage(msg);
        return true;
    }

    public boolean updateSupportedBearerList(String supportedBearers) {
        Message msg = mHandler.obtainMessage();
        msg.what = UPDATE_BEARERLIST_SUPPORTED ;
        msg.obj = supportedBearers;
        mHandler.sendMessage(msg);
        return true;
    }

    public void updateOriginateResult(BluetoothDevice device, int event, int res) {
        if (mCallOriginatedDevice == null || device != mCallOriginatedDevice) {
            Log.e(TAG, "Originate resp ignored, as there is no Orginate req");
            return;
        }
        if (res != 1) {
            mCallOriginatedDevice = null;
            updateCallControlResponse(CCHalConstants.BTCC_OP_ORIGINATE,
                                      CCHalConstants.BTCC_DEF_INDEX_FOR_FAILURES,
                                      CCHalConstants.BTCC_OP_NOT_POSSIBLE, device);
        }
    }

    public boolean updateContentControlID(int ccid) {
        Message msg = mHandler.obtainMessage();
        msg.what = UPDATE_CONTENT_CONTROL_ID;
        msg.arg1 = ccid;
        mHandler.sendMessage(msg);
        mCCId = ccid;
        return true;
    }

    public boolean updateStatusFlags(int statusFlags) {
        Message msg = mHandler.obtainMessage();
        msg.what = UPDATE_STATUS_FLAGS;
        msg.arg1 = statusFlags;
        mHandler.sendMessage(msg);
        return true;
    }

    public boolean updateCallControlOptionalFeatures(int feature) {
        Message msg = mHandler.obtainMessage();
        msg.what = UPDATE_CALL_CONTROL_OPCODES_SUPPORTED;
        msg.arg1 = feature;
        mHandler.sendMessage(msg);
        return true;
    }

    public boolean updateCallControlResponse(int op, int index, int status,
                                             BluetoothDevice device) {
        Message msg = mHandler.obtainMessage();
        msg.what = UPDATE_CALL_CONTROL_RESPONSE ;
        msg.arg1 = op;
        msg.arg2 = index;
        msg.obj = status;
        mHandler.sendMessage(msg);
        return true;
    }

    private boolean updateIncomingCall(int index, String uri)  {
        Message msg = mHandler.obtainMessage();
        msg.what = UPDATE_INCOMING_CALL ;
        msg.arg1 = index;
        msg.obj = uri;
        mHandler.sendMessage(msg);
        return true;
    }

    boolean isVirtualCallStarted() {
        return mVirtualCallStarted;
    }

    public void setVirtualCallActive(boolean state) {
        Log.i(TAG, "setVirtualCallActive: " + state);
        if (state == true) {
          startScoUsingVirtualVoiceCall();
        } else {
            stopScoUsingVirtualVoiceCall();
        }
    }

    private void disaptchFakeCallState (CallControlState state) {
        if (state != null) {
            mCallStateList.put(state.mIndex, state);
        }
        Message msg = mHandler.obtainMessage();
        msg.what = PROCESS_CALL_STATE;
        Collection<CallControlState> values = mCallStateList.values();
        ArrayList<CallControlState> listOfValues = new ArrayList<>(values);
        msg.obj = listOfValues;
        mHandler.sendMessage(msg);
    }

    boolean startScoUsingVirtualVoiceCall() {
        Log.i(TAG, "startScoUsingVirtualVoiceCall: " + Utils.getUidPidString());
        mVirtualCallStarted = true;
        // Send fake call states to mimic outgoing calls

        if (ApmConst.getQtiLeAudioEnabled() ||
                !(mCallAudio != null && mCallAudio.isVoipLeaWarEnabled())) {
            mCallStateList.clear();
            CallControlState alertingState = new CallControlState(1,
                                         CCHalConstants.CALL_STATE_ALERTING,
                                         FLAGS_DIRECTION_BIT);
            disaptchFakeCallState(alertingState);

            CallControlState activeState = new CallControlState(1,
                                         CCHalConstants.CALL_STATE_ACTIVE,
                                         FLAGS_DIRECTION_BIT);
            disaptchFakeCallState(activeState);
        }
        return true;
    }

    boolean stopScoUsingVirtualVoiceCall() {
        Log.i(TAG, "stopScoUsingVirtualVoiceCall: " + Utils.getUidPidString());
        // 1. Check if virtual call has already started
        if (!mVirtualCallStarted) {
            Log.w(TAG, "stopScoUsingVirtualVoiceCall: virtual call not started");
            return false;
        }
        mVirtualCallStarted = false;

        if (ApmConst.getQtiLeAudioEnabled() ||
                !(mCallAudio != null && mCallAudio.isVoipLeaWarEnabled())) {
            // 2. Send fake call states to mimic it ias outgoing calls
            mCallStateList.clear();
            CallControlState disConnectedState =
                    new CallControlState(1, CCHalConstants.CCS_STATE_DISCONNECTED,
                                         FLAGS_DIRECTION_BIT);
            disaptchFakeCallState(disConnectedState);
        }
        return true;
    }

    private void updateCallState(ArrayList<CallControlState> listOfValues) {
        Log.d(TAG, "updateCallState");
        Message msg = mHandler.obtainMessage();
        msg.what = UPDATE_CALL_STATE;
        msg.obj = listOfValues;
        mHandler.sendMessage(msg);
    }

    public boolean processAndUpdateCallState(ArrayList<CallControlState> listOfValues) {
        AcmService mAcmService = AcmService.getAcmService();
        Log.d(TAG, " processAndUpdateCallState");
        BluetoothDevice activeDevice = mActiveDevMgrService!=null?
                                   mActiveDevMgrService.getActiveAbsoluteDevice(ApmConstIntf.AudioFeatures.MEDIA_AUDIO):null;
        int flags = 0;
        //processUnicastState(listOfValues);
        if (mAcmService != null && activeDevice != null &&
            mAcmService.isAcmPlayingMusic(activeDevice)) {
            Log.d(TAG, " processAndUpdateCallState: caching call state");
            mPendingCallStates.add(listOfValues);
            return true;
        } else {
            for (CallControlState state : listOfValues) {
                Log.i(TAG, "processAndUpdateCallState: direction" + state.mDirection);
                if (state.mDirection == 1) {
                    //Incoming call: off the direction bit
                   flags = (flags & (~FLAGS_DIRECTION_BIT));
                } else {
                   //Outgoing call: on the direction bit
                   flags = (flags | FLAGS_DIRECTION_BIT);
                }
                state.mFlags = flags;
                String uri = "";
                String uri_str = "tel:";
                Log.i(TAG, "processAndUpdateCallState: index = " + state.mIndex);
                if (state.mState == CCHalConstants.CALL_STATE_ACTIVE) {
                    mLatestActiveCallIndex = state.mIndex;
                } else if (state.mState == CCHalConstants.CALL_STATE_HELD) {
                    mLatestHeldCallIndex = state.mIndex;
                }
                Log.i(TAG, "processAndUpdateCallState: call state = " + state.mState);
                if ((state.mState == CCHalConstants.CALL_STATE_INCOMING)||
                    (state.mState == CCHalConstants.CALL_STATE_WAITING)) {
                    if (state.mNumber != null) {
                        uri = uri_str.concat(state.mNumber);
                    }
                    Log.i(TAG, "processAndUpdateCallState: inc uri = " + uri);
                    updateIncomingCall(state.mIndex, uri);
                }
            }
            updateCallState(listOfValues);
        }
        return true;
    }

    private void compareAndUpdateWithPrevCallList(
                      HashMap<Integer, CallControlState> currentCallStateList) {
         Log.d(TAG, "compareAndUpdateWithPrevCallList");
         for (Integer key: mPrevCallStateList.keySet()) {
             if (currentCallStateList.containsKey(key) == false) {
             //create a fake disconnected for that index
                 if (mPrevCallStateList.get(key).mState !=
                      CCHalConstants.CALL_STATE_DISCONNECTED) {
                     Log.d(TAG, "inserting DISC state fake!");
                     CallControlState fakeDiscForDisappeared =
                         new CallControlState(key,
                                             CCHalConstants.CALL_STATE_DISCONNECTED,
                                             mPrevCallStateList.get(key).mFlags);
                         mCallStateList.put(key, fakeDiscForDisappeared);
                 }
             }
         }
         mPrevCallStateList.putAll(mCallStateList);
    }

    public void clccResponse(int index, int direction, int call_status,
                             int mode, boolean mpty, String number, int type) {
        Log.d(TAG, "clccResponse");
        if (index != 0) {
            CallControlState state = new CallControlState(index, direction,
                                                          call_status, number);
            mCallStateList.put(index, state);
        } else {
            //update the call state to stack as 0 indicates end of call list
            compareAndUpdateWithPrevCallList(mCallStateList);
            Message msg = mHandler.obtainMessage();
            msg.what = PROCESS_CALL_STATE;
            Collection<CallControlState> values = mCallStateList.values();
              ArrayList<CallControlState> listOfValues = new ArrayList<>(values);
            msg.obj = listOfValues;
            mHandler.sendMessage(msg);
            if (!mLccWaitForResponseQ.isEmpty()) {
                mLccWaitForResponseQ.remove();
            }
            if (!mLccTobeQueued.isEmpty()) {
                mLccTobeQueued.remove();
                getBlcc();
            }
        }
    }

    private void getBlcc() {
        Log.d(TAG, "getBlcc");
        if (mLccTobeQueued.isEmpty()) {
            mCallStateList.clear();
            if (CallControl.listCurrentCalls() == true) {
                mLccWaitForResponseQ.add(1);
                Log.d(TAG, "getBlcc: successfully sent");
                //telephony should always respond with clccresponse
            }
        } else {
            mLccTobeQueued.add(1);
        }
    }

    private boolean processCallStateChange(CallControlState state) {
        Message msg = mHandler.obtainMessage();
        msg.what = PROCESS_PHONE_STATE_CHANGED;
        msg.obj = state;
        mHandler.sendMessage(msg);
        return true;
    }

    public boolean isInbandRingingEnabled() {
        boolean returnVal;
        if (ApmConstIntf.getAospLeaEnabled()) {
            returnVal =
                !SystemProperties.getBoolean(DISABLE_INBAND_RINGING_PROPERTY, false);
        } else {
            returnVal =
                !SystemProperties.getBoolean(DISABLE_INBAND_RINGING_PROPERTY, true);
        }
        Log.d(TAG, "isInbandRingingEnabled returning: " + returnVal);
        return returnVal;
    }

    boolean isCallAudioNeeded(CallControlState state) {
        boolean ret = false;
        if (isInbandRingingEnabled() &&
            state.mState == CCHalConstants.CALL_STATE_INCOMING) {
            ret = true;
        } else if (mCallAudio != null && mCallAudio.isAudioOn() == false &&
                   (state.mState == CCHalConstants.CALL_STATE_ALERTING ||
                    mPrevTelephonyState != null &&
                    mPrevTelephonyState.mNumActive == 0 &&
                    state.mNumActive == 1 &&
                    mPrevTelephonyState.mState != CCHalConstants.CALL_STATE_ALERTING)) {

            ret = true;
        }
        return ret;
    }

    public boolean shouldCallAudioBeActive() {
        boolean ret = false;
        if (mCurrentTelephonyState == null) {
            return false;
        }
        Log.d(TAG, "shouldCallAudioBeActive: " +
                   ", callState: " + mCurrentTelephonyState.mState +
                   ", numHeld:" + mCurrentTelephonyState.mNumHeld +
                   ", numActive:" + mCurrentTelephonyState.mNumActive);
        if (isInbandRingingEnabled() &&
            mCurrentTelephonyState.mState == CCHalConstants.CALL_STATE_INCOMING) {
            ret = true;
        } else if (mCurrentTelephonyState.mNumActive > 0 ||
                   mCurrentTelephonyState.mNumHeld > 0 ||
                   (mCurrentTelephonyState.mState !=
                            CCHalConstants.CALL_STATE_IDLE &&
                    mCurrentTelephonyState.mState !=
                            CCHalConstants.CALL_STATE_DISCONNECTED &&
                    mCurrentTelephonyState.mState !=
                            CCHalConstants.CALL_STATE_INCOMING)) {
            ret = true;
        }
        Log.d(TAG, "shouldCallAudioBeActive returning: " + ret);
        return ret;
    }

    public  boolean  phoneStateChanged(int numActive, int numHeld,
                                       int callState, String number, int type,
                                       String name, boolean isVirtualCall) {
        Log.d(TAG, "phoneStateChanged: " + " callState: " + callState +
                   ", number:" + number + ", numActive:" + numActive +
                   ", isVirtualCall:" + isVirtualCall);
        mCurrentTelephonyState = new CallControlState(numActive, numHeld,
                                                      callState, number, type, name);
        int profileID =
          mActiveDevMgrService.getActiveProfile(ApmConst.AudioFeatures.CALL_AUDIO);

        if (callState == CCHalConstants.CALL_STATE_DIALING) {
            //ignore this as it is fake Telephony event
            return true;
        }

        // Should stop all other audio mode in this case
        if ((numActive + numHeld) > 0 || callState != CCHalConstants.CALL_STATE_IDLE) {
            if (!isVirtualCall && mVirtualCallStarted) {
                // stop virtual voice call if there is an incoming Telecom call update
                mCallAudio.stopScoUsingVirtualVoiceCall();
            }
            if (ApmConst.getAospLeaEnabled() && mCallAudio != null &&
                    mCallAudio.isVoipLeaWarEnabled() &&
                    isInbandRingingEnabled() && callState == CCHalConstants.CALL_STATE_INCOMING) {
                if (!isVirtualCall && mVirtualCallStarted) {
                    Log.i(TAG, "Ignore CS incomig state update if inband-ring enabled");
                }
            } else {
                if (!isVirtualCall && mVirtualCallStarted) {
                    // stop virtual voice call if there is an incoming Telecom call update
                    mCallAudio.stopScoUsingVirtualVoiceCall();
                }
            }
            processCallStateChange(mCurrentTelephonyState);
        } else {
            // ignore CS non-call state update when virtual call started
            if (!isVirtualCall && mVirtualCallStarted) {
              Log.i(TAG, "Ignore CS non-call state update");
            }
        }

        if (isCallAudioNeeded(mCurrentTelephonyState)) {
            if (mCallAudio != null && ApmConst.getQtiLeAudioEnabled()) {
                if ((profileID == ApmConst.AudioProfiles.BAP_CALL) ||
                    (profileID == ApmConst.AudioProfiles.TMAP_CALL)) {
                    Log.d(TAG, "connect BAP call ");
                    mCallAudio.connectAudio();
                }
            } else {
                Log.e(TAG, "no CallAudio handle");
            }
        }

        if (mPrevTelephonyState != null &&
            mPrevTelephonyState.mNumActive == 1 &&
            mCurrentTelephonyState.mNumActive == 0 &&
            mCurrentTelephonyState.mNumHeld == 0 ||
            mCurrentTelephonyState.mState == CCHalConstants.CCS_STATE_DISCONNECTED) {
            if (mPrevTelephonyState.mNumHeld == 0 &&
                mCurrentTelephonyState.mNumHeld == 1) {
               Log.d(TAG, "special case where Active call moved to HOLD");
            } else {
                if (mCallAudio != null && mCallAudio.isAudioOn()
                                        && ApmConst.getQtiLeAudioEnabled()) {
                    if ((profileID == ApmConst.AudioProfiles.BAP_CALL) ||
                       (profileID == ApmConst.AudioProfiles.TMAP_CALL)) {
                        Log.d(TAG, "disconnect BAP call ");
                        mCallAudio.disconnectAudio();
                    }
                } else {
                    Log.e(TAG, "no CallAudio handle OR no Audio up");
                }
            }
        }
        if ((numActive + numHeld) > 0 || callState != CCHalConstants.CALL_STATE_IDLE) {
            mPrevTelephonyState =
                    new CallControlState(numActive, numHeld,callState, number, type, name);
        }
        return true;
   }

    public BluetoothDevice getActiveDevice() {
        return mActiveDevice;
    }

    public int getContentControlID() {
        return mCCId;
    }

    public boolean setActiveDevice(BluetoothDevice device) {
        Message msg = mHandler.obtainMessage();
        msg.what = ACTIVE_DEVICE_CHANGED;
        msg.obj = device;
        mHandler.sendMessage(msg);
        return true;
    }

    public boolean updateActiveProfile(BluetoothDevice device, int state) {
        Message msg = mHandler.obtainMessage();
        msg.what = ACTIVE_PROFILE_UPDATED;
        msg.arg1 = state;
        msg.obj = device;
        mHandler.sendMessage(msg);
        return true;
    }

    private boolean setActiveDeviceRemoteTrigger(BluetoothDevice device) {
        boolean ret = false;
        if (ApmConst.getQtiLeAudioEnabled()) {
            mActiveDevMgrService = ActiveDeviceManagerService.get();
            if (mActiveDevMgrService != null) {
                ret = mActiveDevMgrService.setActiveDeviceBlocking(device,
                                                   ApmConst.AudioFeatures.CALL_AUDIO);
            }
        } else {
            Log.d(TAG, "setActiveDeviceRemoteTrigger(): Calling LeAudioService setActive");
            LeAudioService leaService = LeAudioService.getLeAudioService();
            if (leaService != null) {
                ret = leaService.setActiveDeviceBlocking(device);
            } else {
                Log.e(TAG, "setActiveDeviceRemoteTrigger(): lea service unavailable");
            }
        }
        Log.d(TAG, "setActiveDeviceRemoteTrigger(): returns: " + ret);
        return ret;
    }

    private boolean isActiveDevice(BluetoothDevice device) {
        boolean ret = false;
        mActiveDevMgrService = mActiveDevMgrService = ActiveDeviceManagerService.get();
        AcmService mAcmService = AcmService.getAcmService();
        BluetoothDevice mGroupDevice;
        if (mAcmService != null) {
            mGroupDevice = mAcmService.getGroup(device);
        } else {
            mGroupDevice = device;
        }

        if (mActiveDevMgrService != null) {
            ret = Objects.equals(mGroupDevice,
                                 mActiveDevMgrService.getActiveDevice(
                                             ApmConst.AudioFeatures.CALL_AUDIO));
        }
        Log.d(TAG, "isActiveDevice returns" + ret);
        return ret;
    }

    public boolean handleCallControlOps(int op, int[]call_indices, int count,
                                        byte[] dialNumber, byte[] address) {
        Log.d(TAG, "handleCallControlOps(): " + op);
        HandleCcOps(op, call_indices, count, dialNumber, address);
        return true;
    }

    public void handleAnswerCall(BluetoothDevice device) {
        Log.d(TAG, "handleAnswerCall: device: " + device +
                   ", mPendingAnswerCall: " + mIsPendingAnswerCall);
        if (mIsPendingAnswerCall) {
            CallControl.answerCall(device);
            mIsPendingAnswerCall = false;
        }
    }

    public boolean onCallControlPointChangedRequest(int op, int[] call_indices,
                                                    int count, String dialNumber,
                                                    BluetoothDevice device ) {
        Log.d(TAG, "onCallControlPointChangedRequest: opcode  : " +
                                  CCHalConstants.operationToString(op)) ;
        switch(op) {
            case CCHalConstants.BTCC_OP_ACCEPT: {
                if (!SystemProperties.getBoolean(CC_PTS_ENABLED_PROPERTY, false)) {
                    if (isActiveDevice(device) != true) {
                        Log.d(TAG, "call set active device");
                        setActiveDeviceRemoteTrigger (device);
                        mIsPendingAnswerCall = true;
                    }
                }
                Log.d(TAG, "onCallControlPointChangedRequest: " +
                           "mIsPendingAnswerCall: " + mIsPendingAnswerCall);

                boolean mediaUpdatedAlready = false;
                boolean voiceUpdatedAlready = false;
                mActiveDevMgrService = ActiveDeviceManagerService.get();
                if (mActiveDevMgrService != null) {
                    mediaUpdatedAlready = mActiveDevMgrService.isUpdatedPreviouselyToMM(
                                                   ApmConst.AudioFeatures.MEDIA_AUDIO);
                    voiceUpdatedAlready = mActiveDevMgrService.isUpdatedPreviouselyToMM(
                                                   ApmConst.AudioFeatures.CALL_AUDIO);
                    mActiveDevMgrService.resetUpdatedPreviouselyToMM(ApmConst.AudioFeatures.MEDIA_AUDIO);
                    mActiveDevMgrService.resetUpdatedPreviouselyToMM(ApmConst.AudioFeatures.CALL_AUDIO);
                }
                Log.d(TAG, "onCallControlPointChangedRequest: " +
                           "mediaUpdatedAlready: " + mediaUpdatedAlready +
                           "voiceUpdatedAlready: " + voiceUpdatedAlready);

                if (!mIsPendingAnswerCall ||
                    (mediaUpdatedAlready && voiceUpdatedAlready)) {
                    CallControl.answerCall(device);
                }
                break;
            }

            case CCHalConstants.BTCC_OP_TERMINATE: {
                int callIndex = call_indices[0];
                int res;
                int idx = CCHalConstants.BTCC_DEF_INDEX_FOR_FAILURES;
                Log.d(TAG, "callIndex: " + callIndex);
                if (isActiveDevice(device) == true) {
                    CallControl.terminateCall(device, callIndex);
                    res = CCHalConstants.BTCC_OP_SUCCESS;
                    idx = call_indices[0];
                } else {
                    res = CCHalConstants.BTCC_OP_NOT_POSSIBLE;
                    idx = CCHalConstants.BTCC_DEF_INDEX_FOR_FAILURES;
                }
                updateCallControlResponse(op, idx, res, device);
                break;
            }

            case CCHalConstants.BTCC_OP_LOCAL_HLD:{
                int callIndex = call_indices[0];
                int res;
                int idx = CCHalConstants.BTCC_DEF_INDEX_FOR_FAILURES;
                Log.d(TAG, "callIndex: " + callIndex);
                if (isActiveDevice(device) &&
                    CallControl.holdCall(device, callIndex) == true) {
                    res = CCHalConstants.BTCC_OP_SUCCESS;
                    idx = callIndex;
                } else {
                    res = CCHalConstants.BTCC_OP_NOT_POSSIBLE;
                    idx = CCHalConstants.BTCC_DEF_INDEX_FOR_FAILURES;
                }
                updateCallControlResponse(op, idx, res, device);
                break;
            }

            case CCHalConstants.BTCC_OP_LOCAL_RETRIEVE: {
                //Analogus to SWAP as stack would have
                //already validated the input index is in HELD state
                int chld = 2;
                int res;
                int idx = CCHalConstants.BTCC_DEF_INDEX_FOR_FAILURES;
                if (isActiveDevice(device) &&
                    CallControl.processChld(device, chld) == true) {
                    res = CCHalConstants.BTCC_OP_SUCCESS;
                    idx = call_indices[0];
                } else {
                    res = CCHalConstants.BTCC_OP_NOT_POSSIBLE;
                    idx = CCHalConstants.BTCC_DEF_INDEX_FOR_FAILURES;
                }
                updateCallControlResponse(op, idx, res, device);
                break;
            }

            case CCHalConstants.BTCC_OP_ORIGINATE: {
                Log.d(TAG, "Orignate: from Device: " + device +
                           ", dialString: " + dialNumber);
                if (dialNumber == null) {
                    Log.e(TAG, "null dial string");
                    break;
                }
                if (mCallOriginatedDevice != null) {
                    Log.d(TAG, "Originate is pending from device: " +
                                                           mCallOriginatedDevice);
                    updateCallControlResponse(op,
                                        CCHalConstants.BTCC_DEF_INDEX_FOR_FAILURES,
                                        CCHalConstants.BTCC_OP_NOT_POSSIBLE, device);
                    break;
                } else {
                    if (!SystemProperties.getBoolean(CC_PTS_ENABLED_PROPERTY, false)) {
                        if (isActiveDevice(device) != true) {
                            Log.d(TAG, "call set active device");
                            setActiveDeviceRemoteTrigger(device);
                        }
                    }
                    String[] result = dialNumber.split(":");
                    if (CallControl.dialOutgoingCall(device, result[1]) == true) {
                        mCallOriginatedDevice = device;
                    } else {
                        updateCallControlResponse(op,
                                        CCHalConstants.BTCC_DEF_INDEX_FOR_FAILURES,
                                        CCHalConstants.BTCC_OP_NOT_POSSIBLE, device);
                    }
                }
                break;
            }

            case CCHalConstants.BTCC_OP_JOIN: {
                //Stack would have validate to ensure the input indicies
                //are valid candidates for JOIN op
                int chld = 3;
                int res;
                int idx = CCHalConstants.BTCC_DEF_INDEX_FOR_FAILURES;
                if (CallControl.processChld(device, chld) == true) {
                    res = CCHalConstants.BTCC_OP_SUCCESS;
                    idx = call_indices[0];
                } else {
                    res = CCHalConstants.BTCC_OP_NOT_POSSIBLE;
                    idx = CCHalConstants.BTCC_DEF_INDEX_FOR_FAILURES;
                }
                updateCallControlResponse(op, idx, res, device);
                break;
            }
        }
        return true;
    }

    public void onCallControlInitialized(int status) {
        Log.v(TAG, "CallControlInitializedCallback: status=" + status);
        if (status == 0)  {
            //Initialize Telephony and APM related Initialization
            mCallControlInitialized = true;
            CallControl.listenForPhoneState(
                             PhoneStateListener.LISTEN_SERVICE_STATE|
                             PhoneStateListener.LISTEN_SERVICE_STATE);
            updateContentControlID(CC_CONTENT_CONTROL_ID);
            updateSupportedBearerList("tel");
            updateCallControlOptionalFeatures(CALL_CONTROL_OPTIONAL_FEATURES);
        }
    }

    public void onAcmConnectionStateChanged(BluetoothDevice device, int prevState, int newState) {
        Log.v(TAG, "onConnectionStateChanged: address=" + device.toString() +
                   " prevState: " + prevState + " -> " + newState);
        onConnectionStateChanged(device, newState);
    }

    public void onConnectionStateChanged(BluetoothDevice device, int state) {
        Log.v(TAG, "onConnectionStateChanged: address=" + device.toString() +
                   " state: " + state);
        List<BluetoothDevice> hfconnectedDevices = new ArrayList<>(0);
        List<BluetoothDevice> leAudioConnectedDevices = new ArrayList<>(0);
        List<BluetoothDevice> leAudioGroupLeadDevices = new ArrayList<>(0);
        HeadsetService headsetService = HeadsetService.getHeadsetService();
        AcmService acmService = AcmService.getAcmService();
        LeAudioService leAudioService = LeAudioService.getLeAudioService();
        if (acmService != null) {
           leAudioConnectedDevices = acmService.getConnectedDevices();
           Log.v(TAG, "onConnectionStateChanged: leAudioConnectedDevices = "
                                                 + leAudioConnectedDevices.size());
        }
        if (headsetService != null) {
           hfconnectedDevices = headsetService.getConnectedDevices();
           Log.v(TAG, "onConnectionStateChanged: hfconnectedDevices = "
                                                 + hfconnectedDevices.size());
        }

        if (leAudioService != null) {
            leAudioGroupLeadDevices = leAudioService.getConnectedGroupLeadDevices();
            Log.v(TAG, "onConnectionStateChanged: leAudioGroupLeadDevices = "
                                                 + leAudioGroupLeadDevices.size());
        }

        if (state == 0) {
            if (leAudioGroupLeadDevices.size() <= 1 &&
                isInbandRingingEnabled()) {
                Log.i(TAG,"enable Inband Ringtone for CC if only one device is connected");
                if (hfconnectedDevices.size() < 1) {
                    updateStatusFlags(1);
                }
            }
            return;
        }
        if ((leAudioGroupLeadDevices.size() > 1) ||
            (hfconnectedDevices.size() >= 1)) {
            Log.d(TAG, "disable inband ringtone support if enabled");
            if (isInbandRingingEnabled()) {
               updateStatusFlags(0);
            }
        }

        Message msg = mHandler.obtainMessage();
        msg.what = EVENT_TYPE_CONNECTION_STATE_CHANGED;
        msg.obj = device;
        msg.arg2 = state;
        mHandler.sendMessage(msg);
        return;
    }

    public void onUnicastStateUpdate(Integer playState) {
        Log.d(TAG, "onUnicastStateUpdate: " + playState);
        if (playState == BluetoothA2dp.STATE_NOT_PLAYING) {
            Log.d(TAG," onUnicastStateUpdate, not playing check pending call state");
            if (mPendingCallStates.size() > 0) {
                Iterator<ArrayList<CallControlState>> it = mPendingCallStates.iterator();
                if (it != null) {
                    while (it.hasNext()) {
                        ArrayList<CallControlState> listOfItems = it.next();
                        it.remove();
                        if (processAndUpdateCallState(listOfItems)) {
                            Log.d(TAG,"cached call state dispatched");
                        }
                    }
                    Log.d(TAG,"onUnicastStateUpdate: mPendingCallStates size: " + mPendingCallStates.size());
                }
            } else {
                Log.d(TAG," No pending call state");
            }
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

    void bondStateChanged(BluetoothDevice device, int bondState) {
        if (DBG) {
            Log.d(TAG, "Bond state changed for device: " + device + " state: " + bondState);
        }
        // Remove state machine if the bonding for a device is removed
        if (bondState != BluetoothDevice.BOND_NONE) {
            return;
        }
    }

    private boolean callListContainsDialingCall(ArrayList<CallControlState> listOfValues) {
        boolean ret = false;
        for (CallControlState state : listOfValues) {
            if (state.mState == CCHalConstants.CALL_STATE_DIALING
                || state.mState == CCHalConstants.CALL_STATE_ALERTING) {
                ret = true;
                break;
            }
        }
        return ret;
    }

     /** Handles CCS messages. */
    private final class CcsMessageHandler extends Handler {
        private CcsMessageHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (DBG) Log.v(TAG, "CcsMessageHandler: received message=" +
                                                 messageWhatToString(msg.what));
            ArrayList<CallControlState> listOfValues = null;
            BluetoothDevice device;
            if (mCallControlInitialized == false) {
                Log.d(TAG, "ignore telephony events as CC not initialized yet");
                return;
            }
            switch (msg.what) {
                case UPDATE_BEARER_NAME:
                    String bName = (String)msg.obj;
                    mNativeInterface.updateBearerProviderName(bName);
                    break;

                case UPDATE_BEARER_TECH:
                    int tech_type = (int)msg.arg1;
                    mNativeInterface.updateBearerTechnology(tech_type);
                    break;

                case UPDATE_SIGNAL_STRENGTH:
                    int signal = (int)msg.arg1;
                    mNativeInterface.updateSignalStrength(signal);
                    break;

                case UPDATE_STATUS_FLAGS:
                    int statusFlags = (int)msg.arg1;
                    mNativeInterface.updateStatusFlags(statusFlags);
                    break;

                case UPDATE_BEARERLIST_SUPPORTED :
                    String bSList = (String)msg.obj;
                    mNativeInterface.updateSupportedBearerList(bSList);
                    break;

                case UPDATE_CONTENT_CONTROL_ID:
                    int ccid = (int)msg.arg1;
                    mNativeInterface.contentControlId(ccid);
                    break;

                case UPDATE_CALL_STATE:
                    listOfValues = (ArrayList<CallControlState>)msg.obj;
                    Log.d(TAG, "Call list size : " + listOfValues.size());
                    boolean status = mNativeInterface.callState(listOfValues);
                    if (mCallOriginatedDevice != null &&
                        callListContainsDialingCall(listOfValues)) {
                        Log.e(TAG, "push the pending Originate response");
                        //Stack will pick the right index
                        updateCallControlResponse(CCHalConstants.BTCC_OP_ORIGINATE,
                                        CCHalConstants.BTCC_DEF_INDEX_FOR_FAILURES,
                                        CCHalConstants.BTCC_OP_SUCCESS,
                                        mCallOriginatedDevice);
                        mCallOriginatedDevice = null;
                    }
                    break;

                case UPDATE_CALL_CONTROL_OPCODES_SUPPORTED :
                    int feature = (int)msg.arg1;
                    mNativeInterface.callControlOptionalFeatures(feature);
                    break;

                case UPDATE_CALL_CONTROL_RESPONSE :
                    int op = (int)msg.arg1;
                    int ind = (int)msg.arg2;
                    int st = (int)msg.obj;
                    mNativeInterface.callControlResponse(op, ind, st, null);
                    break;

                case UPDATE_INCOMING_CALL :
                    int index = (int)msg.arg1;
                    String uri = (String)msg.obj;
                    mNativeInterface.updateIncomingCall(index, uri);
                    break;

                case PROCESS_PHONE_STATE_CHANGED:
                    getBlcc();
                    break;

                case PROCESS_CALL_STATE:
                    listOfValues = (ArrayList<CallControlState>)msg.obj;
                    processAndUpdateCallState(listOfValues);
                    break;

                case ACTIVE_DEVICE_CHANGED:
                    device = (BluetoothDevice)msg.obj;
                    mNativeInterface.setActiveDevice(device,-1);
                    if (device == null) {
                       if (mPrevTelephonyState != null) {
                          Log.d(TAG, "active device null, clear mNumActive");
                          mPrevTelephonyState.mNumActive = 0;
                       }
                    }
                    break;

                case ACTIVE_PROFILE_UPDATED:
                    device = (BluetoothDevice)msg.obj;
                    int profile_state =(int)msg.arg1;
                    mNativeInterface.updateActiveProfile(device, profile_state);
                    if (profile_state == 0 && mPrevTelephonyState != null) {
                       Log.d(TAG, "cc profile disconnected, clear mNumActive");
                       mPrevTelephonyState.mNumActive = 0;
                       break;
                    }
                    Log.d(TAG, "Active profile update: query phone state");
                    // Query phone state for initial setup
                    CallControl.queryPhoneState();
                    break;

                case EVENT_TYPE_CONNECTION_STATE_CHANGED:
                    Log.d(TAG, "cc connection updated: query phone state");
                     // Query phone state for initial setup
                     CallControl.queryPhoneState();
                    break;

                case PROCESS_CALL_CONTROL_OPS:
                    Log.d(TAG, "Process Call control operations.");
                    Bundle data = msg.getData();
                    int op_code = data.getInt("op");
                    ArrayList<Integer> arrlist = data.getIntegerArrayList("call_indices");
                    Integer[] i = {};
                    if (arrlist != null) {
                        i = arrlist.stream().toArray(Integer[]::new);
                    }
                    int[] call_indices = Arrays.stream(i)
                                           .mapToInt(Integer::intValue).toArray();
                    int count = data.getInt("count");
                    byte[] dialnumber = data.getByteArray("dialNumber");
                    byte[] bdaddr = data.getByteArray("BdAddress");

                    String dialUri = new String(dialnumber, StandardCharsets.UTF_8);
                    BluetoothDevice dev = getDevice(bdaddr);
                    onCallControlPointChangedRequest(op_code, call_indices,
                                                     count, dialUri, dev);
                    break;

                default:
                    Log.e(TAG, "unknown message! msg.what=" + messageWhatToString(msg.what));
                    break;
           }
           Log.v(TAG, "Exit handleMessage");
       }
   }

    public static String messageWhatToString(int what) {
        switch (what) {
            case UPDATE_BEARER_NAME :
                return "UPDATE_BEARER_NAME";
            case UPDATE_BEARER_TECH :
                return "UPDATE_BEARER_TECH";
            case UPDATE_SIGNAL_STRENGTH :
                return "UPDATE_SIGNAL_STRENGTH";
            case UPDATE_BEARERLIST_SUPPORTED :
                return "UPDATE_BEARERLIST_SUPPORTED";
            case UPDATE_CONTENT_CONTROL_ID :
                return "UPDATE_CONTENT_CONTROL_ID";
            case UPDATE_CALL_STATE :
                return "UPDATE_CALL_STATE";
            case UPDATE_CALL_CONTROL_OPCODES_SUPPORTED :
                return "UPDATE_CALL_CONTROL_OPCODES_SUPPORTED ";
            case UPDATE_CALL_CONTROL_RESPONSE :
                return "UPDATE_CALL_CONTROL_RESPONSE";
            case UPDATE_INCOMING_CALL  :
                return "UPDATE_INCOMING_CALL";
            case PROCESS_CALL_STATE :
                return "PROCESS_CALL_STATE";
            case UPDATE_STATUS_FLAGS:
                return "UPDATE_STATUS_FLAGS";
            default:
                break;
        }
        return Integer.toString(what);
    }

    /**
     * Binder object: must be a static class or memory leak may occur.
     */
    static class CcBinder extends Binder implements IProfileServiceBinder {
        private CCService mService;

        private CCService getService() {
            if (!Utils.checkCallerIsSystemOrActiveUser(TAG)) {
                return null;
            }

            if (mService != null && mService.isAvailable()) {
                return mService;
            }
            return null;
        }

        CcBinder(CCService svc) {
            mService = svc;
        }

        @Override
        public void cleanup() {
            mService = null;
        }
    }
}
