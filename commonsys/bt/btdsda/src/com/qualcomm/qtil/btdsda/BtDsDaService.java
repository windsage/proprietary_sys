/* Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 * Not a contribution.
 */

/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.qualcomm.qtil.btdsda;

import static android.Manifest.permission.BLUETOOTH_CONNECT;

import android.annotation.RequiresPermission;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.content.BroadcastReceiver;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.ComponentName;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.telecom.InCallService;
import android.telecom.Call;
import android.telecom.Connection;
import android.telecom.CallAudioState;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telecom.VideoProfile;
import android.telephony.TelephonyManager;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.text.TextUtils;

import com.android.internal.telephony.PhoneConstants;
import com.qualcomm.qtil.btdsda.BluetoothHeadsetProxy;




import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;


public class BtDsDaService extends InCallService {
  private static final String TAG = "BtDsDaService";
  // match up with bthf_call_state_t of bt_hf.h
  private static final int CALL_STATE_ACTIVE = 0;
  private static final int CALL_STATE_HELD = 1;
  private static final int CALL_STATE_DIALING = 2;
  private static final int CALL_STATE_ALERTING = 3;
  private static final int CALL_STATE_INCOMING = 4;
  private static final int CALL_STATE_WAITING = 5;
  private static final int CALL_STATE_IDLE = 6;
  private static final int CALL_STATE_DISCONNECTED = 7;

  //function calls for DsDa
  private static final int CALL_INCOMING_ENDED = 0;
  private static final int CALL_INCOMING_ACTIVE = 1;
  private static final int CALL_INCOMING_RINGING_SETUP = 2;
  private static final int CALL_INCOMING_ACTIVE_SETUP = 3;
  private static final int CALL_ACTIVE_ENDED = 4;
  private static final int ADD_HELDCALLS_TO_ACTIVE = 5;
  private static final int MOVE_ACTIVE_TO_HELD = 6;
  private static final int CALL_MADE_ACTIVE = 7;
  private static final int INCOMING_SETUP_OUTGOING = 8;

  public int mLastBtHeadsetState = CALL_STATE_IDLE;

  //count variables for held and active calls for dsda
  public int mDsdaTotalcalls = 0;
  public int mDsdaActiveCalls = 0;
  public int mDsdaIncomingCalls = 0;
  public int mDsDaHeldCalls = 0;
  public int mDsDaOutgoingCalls = 0;
  public int mDsDaCallState = CALL_STATE_IDLE;
  public String mDsDaRingingAddress = null;
  public int mDsDaRingingAddressType = DEFAULT_RINGING_ADDRESS_TYPE;
  public String mDsDaRingingName = null;

    //flag for newCall Not updated
  private int mDsDaTwoIncomingCallsFlag = 0;
  private int mdsDaSelectPhoneAccountFlag = 0;
  private int mDelayOutgoingUpdate = 0;
  private int mDsDaHighDefCallFlag = 0;
  private int mCallSwapPending = 0;
  private int conferenceCallInitiated = 0;

  public String mFirstIncomingCallId = null;

  public String mSecondIncomingCallId = null;

  public String mSelectPhoneAccountId = null;

  public String mDialingCallId = null;

  // match up with bthf_call_state_t of bt_hf.h
  // Terminate all held or set UDUB("busy") to a waiting call
  private static final int CHLD_TYPE_RELEASEHELD = 0;
  // Terminate all active calls and accepts a waiting/held call
  private static final int CHLD_TYPE_RELEASEACTIVE_ACCEPTHELD = 1;
  // Hold all active calls and accepts a waiting/held call
  private static final int CHLD_TYPE_HOLDACTIVE_ACCEPTHELD = 2;
  // Add all held calls to a conference
  private static final int CHLD_TYPE_ADDHELDTOCONF = 3;

  // Indicates that no BluetoothCall is ringing
  private static final int DEFAULT_RINGING_ADDRESS_TYPE = 128;

  private int mNumActiveCalls = 0;
  private int mNumHeldCalls = 0;
  private int mNumChildrenOfActiveCall = 0;
  private int mBluetoothCallState = CALL_STATE_IDLE;
  private String mRingingAddress = "";
  private int mRingingAddressType = DEFAULT_RINGING_ADDRESS_TYPE;
  private BluetoothCall mOldHeldCall = null;
  private boolean mHeadsetUpdatedRecently = false;
  private boolean mIsDisconnectedTonePlaying = false;

  private static final Object LOCK = new Object();
  private BluetoothHeadsetProxy mBluetoothHeadset;
  private boolean mHeadsetConnectFlag = false;

  private static final String ACTION_DSDA_CALL_STATE_CHANGE =
      "android.bluetooth.dsda.action.DSDA_CALL_STATE_CHANGED";

  public static final int INVALID_DSDA_STATE = -1;
  public static final int INVALID_CHLD_REQUEST = -1;
  public static final int ANSWER_CALL = 1;
  public static final int HANGUP_CALL = 2;
  public static final int PROCESS_CHLD= 3;
  public static final int HELD_CALL   = 4;
  public static final int LIST_CLCC   = 5;
  public static final int QUERY_PHONE_STATE = 6;
  public static final int CLEAN_UP = 7;

  public TelephonyManager mTelephonyManager;
  public TelecomManager mTelecomManager;

  public final HashMap<String, CallStateCallback> mCallbacks = new HashMap<>();
  public final HashMap<String, BluetoothCall> mBluetoothDsDaCallHashMap = new HashMap<>();
  // A map from Calls to indexes used to identify calls for CLCC (C* List Current Calls).
  private final Map<BluetoothCall, Integer> mClccIndexMap = new HashMap<>();

  public CallInfo mCallInfo = new CallInfo();
  private static BtDsDaService sInstance;
  private static final String ACTION_MSIM_VOICE_CAPABILITY_CHANGED =
   "org.codeaurora.intent.action.MSIM_VOICE_CAPABILITY_CHANGED";

  /**
   * Listens to connections and disconnections of bluetooth headsets.  We need to save the current
   * bluetooth headset so that we know where to send BluetoothCall updates.
   */
  public BluetoothProfile.ServiceListener mProfileListener =
      new BluetoothProfile.ServiceListener() {

      @Override
      public void onServiceConnected(int profile, BluetoothProfile proxy) {
          synchronized (LOCK) {
            setBluetoothHeadset(new BluetoothHeadsetProxy((BluetoothHeadset) proxy));
            Log.e(TAG, " onServiceConnected " + profile);
            updateHeadsetWithCallState(true /* force */);
            }
      }

      @Override
      public void onServiceDisconnected(int profile) {
           synchronized (LOCK) {
             Log.e(TAG, " onServiceDisConnected " + profile);
             if (mBluetoothHeadset != null && sInstance != null) {
                 Log.d(TAG, "closeBluetoothHeadsetProxy");
                 mBluetoothHeadset.closeBluetoothHeadsetProxy(sInstance);
             }
             setBluetoothHeadset(null);
             mHeadsetConnectFlag = false;
           }
      }
  };
  public class BtDsDaStateReceiver extends BroadcastReceiver {

      @Override
      public void onReceive(Context context, Intent intent) {
          String action = intent.getAction();
          Log.d(TAG, "my action is" + action);
          if (action.equals(ACTION_DSDA_CALL_STATE_CHANGE)) {
              int state = intent.getIntExtra("state", INVALID_DSDA_STATE);
              if (state == ANSWER_CALL) {
                 Log.d(TAG, "AnswerCall Intent");
                 answerCall();
              }
              else if (state == HANGUP_CALL) {
                 Log.d(TAG, "Hangup Call Intent");
                 hangupCall();
              }
              else if (state == LIST_CLCC) {
                 Log.d(TAG, "CLCC intent");
                 listCurrentCalls();
              }
              else if (state == PROCESS_CHLD) {
                 int chld = intent.getIntExtra("chld", INVALID_CHLD_REQUEST);
                 Log.d(TAG, "process chld intent");
                 processChld(chld);
              }
              else if (state == QUERY_PHONE_STATE) {
                 Log.d(TAG, "QUERY Phone State intent");
                 updateHeadsetWithCallState(true);
              }
              else if (state == CLEAN_UP) {
                 Log.d(TAG, "HashMap CLEAN_UP intent");
                 mBluetoothDsDaCallHashMap.clear();
              }
          }
          else if (action.equals(ACTION_MSIM_VOICE_CAPABILITY_CHANGED)) {
             Log.d(TAG, "ACTION_MSIM_VOICE_CAPABILITY_CHANGED intent received");
             if (mTelephonyManager != null) {
                 if (mTelephonyManager.isDsdaOrDsdsTransitionMode()){
                    Log.w(TAG, "In DSDA to DSDS or viceversa transition mode");
                 }
                 else {
                       if (mTelephonyManager.isConcurrentCallsPossible()) {
                        Log.w(TAG, "In DSDA mode");
                    }
                    else {
                       Log.w(TAG, "In DSDS mode");
                    }
                 }
             }//null check
             else {
                  Log.e(TAG, "mTelephonyManager is null when "
                            +"ACTION_MSIM_VOICE_CAPABILITY_CHANGED intent received");
             }
          }
      }
  };
  public BtDsDaStateReceiver mBluetoothAdapterReceiver = null;
  public BtDsDaStateReceiver mVoiceCapabilityChangeReceiver = null;

  @Override
  public IBinder onBind(Intent intent) {
      Log.d(TAG, "BtDsDaService onBind is called");
      BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
      if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
          Log.i(TAG, "Bluetooth is off");
          ComponentName componentName = BtStateReceiver.BLUETOOTH_DSDA_SERVICE_COMPONENT;
          getPackageManager().setComponentEnabledSetting(
                  componentName,
                  PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                  PackageManager.DONT_KILL_APP);
          return null;
      }
      IBinder binder = super.onBind(intent);
      mTelephonyManager = getSystemService(TelephonyManager.class);
      return binder;
   }

  @Override
  public boolean onUnbind(Intent intent) {
      Log.i(TAG, "onUnbind. Intent: " + intent);
      mDsdaActiveCalls = 0;
      mDsDaCallState = CALL_STATE_IDLE;
      mDsdaIncomingCalls = 0;
      mDsDaTwoIncomingCallsFlag = 0;
      mDsDaOutgoingCalls = 0;
      mDsDaRingingAddress = null;
      mDsDaRingingAddressType = DEFAULT_RINGING_ADDRESS_TYPE;
      mDsDaHeldCalls = 0;
      mDsDaRingingName = null;
      mDsdaTotalcalls = 0;
      return super.onUnbind(intent);
   }

  @Override
  public void onCreate() {
      Log.d(TAG, "BtDsDaService is created from onCreate()");
      super.onCreate();
      if (!mHeadsetConnectFlag) {
          Log.d(TAG, "onCreate(): getProfileProxy");
          BluetoothAdapter.getDefaultAdapter()
                  .getProfileProxy(this, mProfileListener, BluetoothProfile.HEADSET);
          mHeadsetConnectFlag = true;
      }
      if (mBluetoothAdapterReceiver == null) {
          Log.d(TAG, "onCreate(): registerReceiver");
          mBluetoothAdapterReceiver = new BtDsDaStateReceiver();
          IntentFilter intentFilter = new IntentFilter();
          intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
          intentFilter.addAction(ACTION_DSDA_CALL_STATE_CHANGE);
          registerReceiver(mBluetoothAdapterReceiver, intentFilter, Context.RECEIVER_EXPORTED);
      }
      if (mVoiceCapabilityChangeReceiver == null) {
          Log.d(TAG, "onCreate(): mVoiceCapabilityChangeReceiver ");
          mVoiceCapabilityChangeReceiver = new BtDsDaStateReceiver();
          IntentFilter intentFilter = new IntentFilter(
                 TelecomManager.ACTION_TTY_PREFERRED_MODE_CHANGED);
          intentFilter.addAction(ACTION_MSIM_VOICE_CAPABILITY_CHANGED);
          registerReceiver(mVoiceCapabilityChangeReceiver, intentFilter,
                android.Manifest.permission.MODIFY_PHONE_STATE, null, Context.RECEIVER_EXPORTED);
      }
  }

   @Override
   public int onStartCommand(Intent intent, int flags, int startId) {
      Log.d(TAG, "BtDsDaService is created from onStartCommand()");
      if (!mHeadsetConnectFlag) {
          Log.d(TAG, "onStartCommand(): getProfileProxy");
          BluetoothAdapter.getDefaultAdapter()
                  .getProfileProxy(this, mProfileListener, BluetoothProfile.HEADSET);
          mHeadsetConnectFlag = true;
      }
      if (mBluetoothAdapterReceiver == null) {
          Log.d(TAG, "onStartCommand(): registerReceiver");
          mBluetoothAdapterReceiver = new BtDsDaStateReceiver();
          IntentFilter intentFilter = new IntentFilter();
          intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
          intentFilter.addAction(ACTION_DSDA_CALL_STATE_CHANGE);
          registerReceiver(mBluetoothAdapterReceiver, intentFilter, Context.RECEIVER_EXPORTED);
      }
      if (mVoiceCapabilityChangeReceiver == null) {
          Log.d(TAG, "onStartCommand(): mVoiceCapabilityChangeReceiver");
          mVoiceCapabilityChangeReceiver = new BtDsDaStateReceiver();
          IntentFilter intentFilter = new IntentFilter(
                 TelecomManager.ACTION_TTY_PREFERRED_MODE_CHANGED);
          intentFilter.addAction(ACTION_MSIM_VOICE_CAPABILITY_CHANGED);
          registerReceiver(mVoiceCapabilityChangeReceiver, intentFilter,
                android.Manifest.permission.MODIFY_PHONE_STATE, null, Context.RECEIVER_EXPORTED);
      }
      sInstance = this;
      return START_NOT_STICKY;
   }

   @Override
   public void onDestroy() {
    Log.d(TAG, "BtDsDaService's on Destroy() is called");
    synchronized (LOCK) {
     if (mBluetoothAdapterReceiver != null) {
        Log.d(TAG, "onDestroy(): unregisterReceiver");
        unregisterReceiver(mBluetoothAdapterReceiver);
        mBluetoothAdapterReceiver = null;
     }
    if (mBluetoothHeadset != null) {
        Log.d(TAG, "onDestroy(): closeBluetoothHeadsetProxy");
        mBluetoothHeadset.closeBluetoothHeadsetProxy(this);
        mBluetoothHeadset = null;
        mHeadsetConnectFlag = false;
     }
     if ( mVoiceCapabilityChangeReceiver != null) {
        Log.d(TAG, "onDestroy(): unregister mVoiceCapabilityChangeReceiver");
        unregisterReceiver(mVoiceCapabilityChangeReceiver);
        mVoiceCapabilityChangeReceiver = null;
     }
    }//synchronized (LOCK){
      mBluetoothDsDaCallHashMap.clear();
      sInstance = null;
      super.onDestroy();
   }
   public BtDsDaService() {
      Log.i(TAG, "BtDsDaService is created");
      sInstance = this;
   }
   public static BtDsDaService getInstance() {
       return sInstance;
   }

   public void CleanUp() {
       Log.d(TAG, "BtDsDaService's cleanup is called");
       if (mBluetoothAdapterReceiver != null) {
           Log.d(TAG, "CleanUp(): unregisterReceiver");
           unregisterReceiver(mBluetoothAdapterReceiver);
           mBluetoothAdapterReceiver = null;
       }
       if (mVoiceCapabilityChangeReceiver != null) {
           Log.d(TAG, "CleanUp(): mVoiceCapabilityChangeReceiver");
           unregisterReceiver(mVoiceCapabilityChangeReceiver);
           mVoiceCapabilityChangeReceiver = null;
       }
       if (mBluetoothHeadset != null) {
           Log.d(TAG, "CleanUp(): closeBluetoothHeadsetProxy");
           mBluetoothHeadset.closeBluetoothHeadsetProxy(this);
           mBluetoothHeadset = null;
           mHeadsetConnectFlag = false;
       }
       mBluetoothDsDaCallHashMap.clear();
       sInstance = null;
   }
       /**
    * Receives events for global state changes of the bluetooth adapter.
    */

   public class CallStateCallback extends Call.Callback {
     public int mLastState;

     public CallStateCallback(int initialState) {
         mLastState = initialState;
     }

     public int getLastState() {
         return mLastState;
     }

     public void onStateChanged(BluetoothCall call, int state) {
         Log.i(TAG, "StateChanged call: " + call + "state: " + state);
         if (mCallInfo.isNullCall(call)) {
             return;
         }
         if (call.isExternalCall()) {
             return;
         }

         Log.e(TAG, "onStateChanged: state is " + state);

         // If a BluetoothCall is being put on hold because of a new connecting call, ignore the
         // CONNECTING since the BT state update needs to send out the numHeld = 1 + dialing
         // state atomically.
         // When the BluetoothCall later transitions to DIALING/DISCONNECTED we will then
         // send out the aggregated update.
         if (getLastState() == Call.STATE_ACTIVE && state == Call.STATE_HOLDING) {
             for (BluetoothCall otherCall : mCallInfo.getBluetoothCalls()) {
                 if (otherCall.getState() == Call.STATE_CONNECTING) {
                     mLastState = state;
                     return;
                 }
             }
         }

         // To have an active BluetoothCall and another dialing at the same time is an invalid BT
         // state. We can assume that the active BluetoothCall will be automatically held
         // which will send another update at which point we will be in the right state.
         BluetoothCall activeCall = mCallInfo.getActiveCall();
         if (!mCallInfo.isNullCall(activeCall)
                 && getLastState() == Call.STATE_CONNECTING
                 && (state == Call.STATE_DIALING || state == Call.STATE_PULLING_CALL)) {
             mLastState = state;
             return;
         }
         mLastState = state;
         processOnStateChanged(call);
     }

     @Override
     public void onStateChanged(Call call, int state) {
         super.onStateChanged(call, state);
         onStateChanged(getBluetoothCallById(call.getDetails().getTelecomCallId()), state);
     }

     public void onDetailsChanged(BluetoothCall call, Call.Details details) {
         Log.i(TAG, "onDetailsChanged call: " + call + "details: " + details);
         if (mCallInfo.isNullCall(call)) {
             return;
         }

         if (call.isExternalCall()) {
            Log.e(TAG, "onDetailsChanged : calling onCallRemoved");
             onCallRemoved(call);
         } else {
            if (!mBluetoothDsDaCallHashMap.containsKey(call.getTelecomCallId())) {
                onCallAdded(call);
             }else{
               Log.i(TAG, "onDetailsChanged call was already added");
               if (details.getState() == Call.STATE_DISCONNECTING) {
                 Log.i(TAG, "Ignore Call STATE_DISCONNECTING");
               }
               if ((mDsDaHighDefCallFlag == 1) && ((mDialingCallId != null) &&
                  (mDialingCallId.equals(call.getTelecomCallId())))) {
                 Log.i(TAG, "onDetailsChanged call extras: " + call.getDetails().getExtras());
                 if ((call.getState() == Call.STATE_DIALING ||
                      call.getState() == Call.STATE_CONNECTING ||
                      call.getState() == Call.STATE_PULLING_CALL ) &&
                     (((null != call.getDetails().getExtras()) &&
                     (0 != call.getDetails().getExtras().getInt(TelecomManager.EXTRA_CALL_TECHNOLOGY_TYPE)))
                         ||call.getDetails().hasProperty(Call.Details.PROPERTY_VOIP_AUDIO_MODE))) {
                   mDialingCallId = null;
                   mDsDaHighDefCallFlag = 0;
                   updateHeadsetWithCallState(true /* force */);
                 }
              }
            }
         }
     }

     @Override
     public void onDetailsChanged(Call call, Call.Details details) {
         super.onDetailsChanged(call, details);
         onDetailsChanged(getBluetoothCallById(call.getDetails().getTelecomCallId()), details);
     }

     public void onParentChanged(BluetoothCall call) {
         Log.e(TAG, "onParentChanged call: " + call);
         if (mCallInfo.isNullCall(call)) {
             return;
         }

         if (call.isExternalCall()) {
             return;
         }
            Log.e(TAG, "onParentChanged ");
         if (call.getParentId() != null) {
             // If this BluetoothCall is newly conferenced, ignore the callback.
             // We only care about the one sent for the parent conference call.
             Log.d(TAG,
                     "Ignoring onIsConferenceChanged from child BluetoothCall with new parent");
             return;
         }
         processConferenceCall(); //use bool parameter as required
     }

     @Override
     public void onParentChanged(Call call, Call parent) {
         super.onParentChanged(call, parent);
         onParentChanged(
                 getBluetoothCallById(call.getDetails().getTelecomCallId()));
     }

     public void onChildrenChanged(BluetoothCall call, List<BluetoothCall> children) {
         Log.e(TAG, "onChildrenChanged call: " + call);
         if (mCallInfo.isNullCall(call)) {
             return;
         }

         if (call.isExternalCall()) {
             return;
         }

         if (call.getChildrenIds().size() == 1) {
             // If this is a parent BluetoothCall with only one child,
             // ignore the callback as well since the minimum number of child calls to
             // start a conference BluetoothCall is 2. We expect this to be called again
             // when the parent BluetoothCall has another child BluetoothCall added.
             Log.d(TAG,
                     "Ignoring onIsConferenceChanged from parent with only one child call");
             return;
         }
         processConferenceCall(); //use bool parameter as required
     }

     @Override
     public void onChildrenChanged(Call call, List<Call> children) {
         super.onChildrenChanged(call, children);
         onChildrenChanged(
                 getBluetoothCallById(call.getDetails().getTelecomCallId()),
                 getBluetoothCallsByIds(BluetoothCall.getIds(children)));
     }
   }

   @RequiresPermission(android.Manifest.permission.MODIFY_PHONE_STATE)
   protected void enforceModifyPermission() {
       enforceCallingOrSelfPermission(android.Manifest.permission.MODIFY_PHONE_STATE, null);
   }

   @RequiresPermission(android.Manifest.permission.MODIFY_PHONE_STATE)
   public boolean answerCall() {
       synchronized (LOCK) {
           enforceModifyPermission();
           Log.i(TAG, "BT - answering call");
           BluetoothCall call = mCallInfo.getRingingOrSimulatedRingingCall();
           BluetoothCall mTempCall = null;
           if (mDsDaTwoIncomingCallsFlag == 1) {
              mTempCall = getBluetoothCallById(mFirstIncomingCallId);
              if (mCallInfo.isNullCall(mTempCall)) {
                 call = mTempCall;
              }
           }
           if (mCallInfo.isNullCall(call)) {
               return false;
           }
           call.answer(VideoProfile.STATE_AUDIO_ONLY);
           return true;
       }
   }

   @RequiresPermission(android.Manifest.permission.MODIFY_PHONE_STATE)
   public boolean hangupCall() {
       synchronized (LOCK) {
           enforceModifyPermission();
           Log.i(TAG, "BT - hanging up call");
           BluetoothCall call = mCallInfo.getForegroundCall();
           BluetoothCall mTempCall = null;
           if (mCallInfo.isNullCall(call)) {
               return false;
           }
           // release the parent if there is a conference call
           BluetoothCall conferenceCall = getBluetoothCallById(call.getParentId());
           if (!mCallInfo.isNullCall(conferenceCall)
                   && conferenceCall.getState() == Call.STATE_ACTIVE) {
               Log.i(TAG, "BT - hanging up conference call");
               call = conferenceCall;
           }
           if (call.getState() == Call.STATE_RINGING) {
              if (mDsDaTwoIncomingCallsFlag == 1) {
                 mTempCall = getBluetoothCallById(mFirstIncomingCallId);
                 if (mCallInfo.isNullCall(mTempCall)) {
                    call = mTempCall;
                 }
               }
               call.reject(false, "");
           } else {
               call.disconnect();
           }
           return true;
       }
   }

   @RequiresPermission(android.Manifest.permission.MODIFY_PHONE_STATE)
   public String getSubscriberNumber() {
       synchronized (LOCK) {
           enforceModifyPermission();
           Log.i(TAG, "getSubscriberNumber");
           String address = null;
           if (mTelecomManager != null) {
               PhoneAccount account = mCallInfo.getBestPhoneAccount();
               if (account != null) {
                   Uri addressUri = account.getAddress();
                   if (addressUri != null) {
                       address = addressUri.getSchemeSpecificPart();
                   }
               }
           }
           if (TextUtils.isEmpty(address)) {
               address = mTelephonyManager.getLine1Number();
               if (address == null) address = "";
           }
           return address;
       }
   }

   @RequiresPermission(android.Manifest.permission.MODIFY_PHONE_STATE)
   public boolean listCurrentCalls() {
       synchronized (LOCK) {
           Log.e(TAG, "listCurrentCalls ");
           enforceModifyPermission();
           // only log if it is after we recently updated the headset state or else it can
           // clog the android log since this can be queried every second.
           boolean logQuery = mHeadsetUpdatedRecently;
           mHeadsetUpdatedRecently = false;

           if (logQuery) {
               Log.i(TAG, "listcurrentCalls");
           }

           sendListOfCalls(logQuery);
           return true;
       }
   }

   @RequiresPermission(android.Manifest.permission.MODIFY_PHONE_STATE)
   public boolean processChld(int chld) {
       synchronized (LOCK) {
           enforceModifyPermission();
           long token = Binder.clearCallingIdentity();
           Log.i(TAG, "processChld " + chld);
           return _processChld(chld);
       }
   }

   public void onCallAdded(BluetoothCall call) {
       Log.d(TAG, "onCallAdded:" + call);
       if (call.isExternalCall()) {
           return;
       }
       if (!mBluetoothDsDaCallHashMap.containsKey(call.getTelecomCallId())) {
           Log.d(TAG, "onCallAdded");
           CallStateCallback callback = new CallStateCallback(call.getState());
           mCallbacks.put(call.getTelecomCallId(), callback);
           call.registerCallback(callback);

           mBluetoothDsDaCallHashMap.put(call.getTelecomCallId(), call);
           processOnCallAdded(call);
       }
   }

   @Override
    public void onCallAdded(Call call) {
        Log.d(TAG, "BtDsDaService oncalladded is called");
        super.onCallAdded(call);
        onCallAdded(new BluetoothCall(call));
    }

    public void onCallRemoved(BluetoothCall call) {
        if (call.isExternalCall()) {
        return;
        }
        Log.d(TAG, "onCallRemoved:" + call);
        CallStateCallback callback = getCallback(call);
        if (callback != null) {
           call.unregisterCallback(callback);
        }
        if (mBluetoothDsDaCallHashMap.containsKey(call.getTelecomCallId())) {
           mBluetoothDsDaCallHashMap.remove(call.getTelecomCallId());
        }
        mClccIndexMap.remove(call);
        processOnCallRemoved(call);
        //HandleDsDaUseCases(CALL_REMOVED, call);
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);
        BluetoothCall bluetoothCall = getBluetoothCallById(call.getDetails().getTelecomCallId());
        if (bluetoothCall == null) {
           Log.w(TAG, "onCallRemoved, BluetoothCall is removed before registered");
           return;
        }
        onCallRemoved(bluetoothCall);
    }

    @Override
    public void onCallAudioStateChanged(CallAudioState audioState) {
        super.onCallAudioStateChanged(audioState);
        Log.e(TAG, "onCallAudioStateChanged, audioState == " + audioState);
    }

    private void sendListOfCalls(boolean shouldLog) {
        Collection<BluetoothCall> calls = mCallInfo.getBluetoothCalls();
        Log.d(TAG, "sendListOfCalls");
        int mNumRingingCalls = mCallInfo.getNumRingingCalls();
        for (BluetoothCall call : calls) {
        // We don't send the parent conference BluetoothCall to the bluetooth device.
        // We do, however want to send conferences that have no children to the bluetooth
        // device (e.g. IMS Conference).
           if (!call.isConference()
               || (call.isConference()
               && call.can(Connection.CAPABILITY_CONFERENCE_HAS_NO_CHILDREN))) {
               //if multiple ringing calls are present, in clcc we send
               //only ringing call which is updated and if outgoingCalls are
               //present, we do not send any of the incoming call update in clcc
               if (Call.STATE_RINGING == call.getState()) {
                  if (mDsDaOutgoingCalls == 0) {
                     if ((mFirstIncomingCallId != null) &&
                          (mFirstIncomingCallId.equals(call.getTelecomCallId()))) {
                        sendClccForCall(call, shouldLog);
                     }
                     else if ((mSecondIncomingCallId != null) &&
                               (mSecondIncomingCallId.equals(call.getTelecomCallId()))) {
                        //ignoring this 2nd incoming call update
                        continue;
                     }
                  }
               }
               else {
                  sendClccForCall(call, shouldLog);
               }
            }
        }
        sendClccEndMarker();
    }

    private void sendClccEndMarker() {
        // End marker is recognized with an index value of 0. All other parameters are ignored.
        if (mBluetoothHeadset != null) {
        Log.d(TAG, "sendClccEndMarker");
        mBluetoothHeadset.clccResponseDsDa(0 /* index */, 0, 0, 0, false, null, 0);
        }
    }
    /**
     * Sends a single clcc (C* List Current Calls) event for the specified call.
     */
    private void sendClccForCall(BluetoothCall call, boolean shouldLog) {
        Log.d(TAG, "sendClccForCall: call: " + call);
        boolean isForeground = mCallInfo.getForegroundCall() == call;
        int state = getBtCallState(call, isForeground);
        boolean isPartOfConference = false;
        boolean isConferenceWithNoChildren = call.isConference()
               && call.can(Connection.CAPABILITY_CONFERENCE_HAS_NO_CHILDREN);

        if (state == CALL_STATE_IDLE) {
           Log.e(TAG, "sendClccForCall, call state is idle, returning");
           return;
        }

        BluetoothCall conferenceCall = getBluetoothCallById(call.getParentId());
        if (!mCallInfo.isNullCall(conferenceCall)) {
           isPartOfConference = true;

           // Run some alternative states for Conference-level merge/swap support.
           // Basically, if BluetoothCall supports swapping or merging at the conference-level,
           // then we need to expose the calls as having distinct states
           // (ACTIVE vs CAPABILITY_HOLD) or
           // the functionality won't show up on the bluetooth device.

           // Before doing any special logic, ensure that we are dealing with an
           // ACTIVE BluetoothCall and that the conference itself has a notion of
           // the current "active" child call.
           BluetoothCall activeChild = getBluetoothCallById(
                   conferenceCall.getGenericConferenceActiveChildCallId());
           if (state == CALL_STATE_ACTIVE && !mCallInfo.isNullCall(activeChild)) {
           // Reevaluate state if we can MERGE or if we can SWAP without previously having
           // MERGED.
             boolean shouldReevaluateState =
               conferenceCall.can(Connection.CAPABILITY_MERGE_CONFERENCE)
                   || (conferenceCall.can(Connection.CAPABILITY_SWAP_CONFERENCE)
                   && !conferenceCall.wasConferencePreviouslyMerged());


                if (shouldReevaluateState) {
                   isPartOfConference = false;
                   if (call == activeChild) {
                      state = CALL_STATE_ACTIVE;
                   } else {
                    // At this point we know there is an "active" child and we know that it is
                    // not this call, so set it to HELD instead.
                    state = CALL_STATE_HELD;
                    }
                }
                Log.e(TAG, "sendClccForCall: shouldReevaluateState is " + shouldReevaluateState +
                            " state is " + state);
           }
            if (conferenceCall.getState() == Call.STATE_HOLDING
                    && conferenceCall.can(Connection.CAPABILITY_MANAGE_CONFERENCE)) {
                // If the parent IMS CEP conference BluetoothCall is on hold, we should mark
                // this BluetoothCall as being on hold regardless of what the other
                // children are doing.
                state = CALL_STATE_HELD;
                Log.e(TAG, "sendClccForCall: call state is holding, has cap manage conference");
            }
        }
        else if (isConferenceWithNoChildren) {
            // Handle the special case of an IMS conference BluetoothCall without conference
            // event package support.
            // The BluetoothCall will be marked as a conference, but the conference will not have
            // child calls where conference event packages are not used by the carrier.
            isPartOfConference = true;
        }

        int index = getIndexForCall(call);
        int direction = call.isIncoming() ? 1 : 0;
        final Uri addressUri;
        if (call.getGatewayInfo() != null) {
            addressUri = call.getGatewayInfo().getOriginalAddress();
        } else {
            addressUri = call.getHandle();
        }

        String address = addressUri == null ? null : addressUri.getSchemeSpecificPart();
        if (address != null) {
           address = PhoneNumberUtils.stripSeparators(address);
        }

        // Don't send host call information when IMS calls are conferenced
        String subsNum = getSubscriberNumber();
        if (subsNum != null && address != null) {
           if (subsNum.contains(address)) {
              Log.w(TAG, "return without sending host call in CLCC");
              return;
           }
        }

        int addressType = address == null ? -1 : PhoneNumberUtils.toaFromString(address);

        if (shouldLog) {
           Log.i(TAG, "sending clcc for BluetoothCall "
                        + index + ", "
                        + direction + ", "
                        + state + ", "
                        + isPartOfConference + ", "
                        + addressType);
        }
        int heldcalls = mCallInfo.getNumHeldCalls();
        if (isPartOfConference == false) {
           if (state == CALL_STATE_HELD) {
              if ((heldcalls > 1) && (mCallSwapPending != 1)) {
                isPartOfConference = true;
               }
            }
        }
        if (mBluetoothHeadset != null) {
           mBluetoothHeadset.clccResponseDsDa(
             index, direction, state, 0, isPartOfConference, address, addressType);
        }
    }

    /**
     * Returns the caches index for the specified call.  If no such index exists, then an index is
     * given (smallest number starting from 1 that isn't already taken).
     */
    private int getIndexForCall(BluetoothCall call) {
        Log.d(TAG, "getIndexForCall: call:" + call);
        if (mClccIndexMap.containsKey(call)) {
           return mClccIndexMap.get(call);
        }

        int i = 1;	// Indexes for bluetooth clcc are 1-based.
        while (mClccIndexMap.containsValue(i)) {
            i++;
        }

        // NOTE: Indexes are removed in {@link #onCallRemoved}.
        mClccIndexMap.put(call, i);
        return i;
    }

    private boolean _processChld(int chld) {
        BluetoothCall activeCall = mCallInfo.getActiveCall();
        BluetoothCall ringingCall = mCallInfo.getRingingOrSimulatedRingingCall();
        int num_ringingCalls = mCallInfo.getNumRingingCalls();
        if (num_ringingCalls >1) {
           Log.i(TAG, "more than 1 ringing call is present");
           ringingCall = getBluetoothCallById(mFirstIncomingCallId);
        }
        if (ringingCall == null) {
           Log.i(TAG, "asdf ringingCall null");
        } else {
            Log.i(TAG, "asdf ringingCall not null " + ringingCall.hashCode());
        }

        BluetoothCall heldCall = mCallInfo.getHeldCall();

        Log.i(TAG, "Active: " + activeCall
                + " Ringing: " + ringingCall
                + " Held: " + heldCall);
        Log.i(TAG, "asdf chld " + chld);

        if (chld == CHLD_TYPE_RELEASEHELD) {
           Log.i(TAG, "asdf CHLD_TYPE_RELEASEHELD");
           if (!mCallInfo.isNullCall(ringingCall)) {
           Log.i(TAG, "asdf reject " + ringingCall.hashCode());
           ringingCall.reject(false, null);
           return true;
           } else if (!mCallInfo.isNullCall(heldCall)) {
                heldCall.disconnect();
                return true;
             }
           } else if (chld == CHLD_TYPE_RELEASEACTIVE_ACCEPTHELD) {
                if (mCallInfo.isNullCall(activeCall)
                    && mCallInfo.isNullCall(ringingCall)
                    && mCallInfo.isNullCall(heldCall)) {
                    return false;
                }
                if (!mCallInfo.isNullCall(activeCall)) {
                    BluetoothCall conferenceCall = getBluetoothCallById(activeCall.getParentId());
                    if (!mCallInfo.isNullCall(conferenceCall)
                        && conferenceCall.getState() == Call.STATE_ACTIVE) {
                       Log.i(TAG, "CHLD: disconnect conference call");
                       conferenceCall.disconnect();
                    } else {
                        activeCall.disconnect();
                    }
                }
                if (!mCallInfo.isNullCall(ringingCall)) {
                   ringingCall.answer(ringingCall.getVideoState());
                } else if (!mCallInfo.isNullCall(heldCall)) {
                    heldCall.unhold();
                }
                return true;
            }else if (chld == CHLD_TYPE_HOLDACTIVE_ACCEPTHELD) {
                if (!mCallInfo.isNullCall(activeCall)
                        && activeCall.can(Connection.CAPABILITY_SWAP_CONFERENCE)) {
                    activeCall.swapConference();
                    Log.i(TAG, "CDMA calls in conference swapped, updating headset");
                    updateHeadsetWithCallState(true /* force */);
                    return true;
                } else if (!mCallInfo.isNullCall(ringingCall)) {
                    ringingCall.answer(VideoProfile.STATE_AUDIO_ONLY);
                    return true;
                } else if (!mCallInfo.isNullCall(heldCall)) {
                    // CallsManager will hold any active calls when unhold() is called on a
                    // currently-held call.
                    heldCall.unhold();
                    return true;
                } else if (!mCallInfo.isNullCall(activeCall)
                      && (activeCall.can(Connection.CAPABILITY_HOLD)
                      ||  activeCall.can(Connection.CAPABILITY_SUPPORT_HOLD))) {
                    activeCall.hold();
                    return true;
                } else if (!mCallInfo.isNullCall(activeCall)) {
                   BluetoothCall conferenceCall = getBluetoothCallById(activeCall.getParentId());
                   if (!mCallInfo.isNullCall(conferenceCall)
                    && (conferenceCall.can(Connection.CAPABILITY_HOLD)
                      || conferenceCall.can(Connection.CAPABILITY_SUPPORT_HOLD))) {
                      Log.i(TAG, "Hold conference call");
                      conferenceCall.hold();
                      return true;
                   }
                }
            } else if (chld == CHLD_TYPE_ADDHELDTOCONF) {
                if (!mCallInfo.isNullCall(activeCall)) {
                   if (activeCall.can(Connection.CAPABILITY_MERGE_CONFERENCE)) {
                      Log.e(TAG, "active call has capability merge conference");
                      activeCall.mergeConference();
                      return true;
                    } else {
                    List<BluetoothCall> conferenceable = getBluetoothCallsByIds(
                        activeCall.getConferenceableCalls());
                        if (!conferenceable.isEmpty()) {
                          Log.i(TAG, "conferencible is not empty");
                          activeCall.conference(conferenceable.get(0));
                          return true;
                        }
                    }
                }
            }
        return false;
    }

    /**
     * Sends an update of the current BluetoothCall state to the current Headset.
     *
     * @param force {@code true} if the headset state should be sent regardless if no changes to
     * the state have occurred, {@code false} if the state should only be sent if the state
     * has changed.
     */
    private void updateHeadsetWithCallState(boolean force) {

        BluetoothCall activeCall = mCallInfo.getActiveCall();
        Log.i(TAG, "UHWCS Active call info call: " + activeCall);
        BluetoothCall ringingCall = mCallInfo.getRingingOrSimulatedRingingCall();
        BluetoothCall heldCall = mCallInfo.getHeldCall();

        int bluetoothCallState = getBluetoothCallStateForUpdate();

        String ringingAddress = null;
        int ringingAddressType = DEFAULT_RINGING_ADDRESS_TYPE;
        String ringingName = null;
        if (!mCallInfo.isNullCall(ringingCall) && ringingCall.getHandle() != null
                && !ringingCall.isSilentRingingRequested()) {
            ringingAddress = ringingCall.getHandle().getSchemeSpecificPart();
            if (ringingAddress != null) {
               ringingAddressType = PhoneNumberUtils.toaFromString(ringingAddress);
            }
            ringingName = ringingCall.getCallerDisplayName();
            if (TextUtils.isEmpty(ringingName)) {
            ringingName = ringingCall.getContactDisplayName();
            }
        }
        if (ringingAddress == null) {
           ringingAddress = "";
        }

        int numActiveCalls = mCallInfo.isNullCall(activeCall) ? 0 : 1;
        int numHeldCalls = mCallInfo.getNumHeldCalls();
        if (mTelephonyManager != null) {
           if (mTelephonyManager.isConcurrentCallsPossible()
                 || mTelephonyManager.isDsdaOrDsdsTransitionMode()) {
              Log.i(TAG, "Concurrent Calls Possible: DSDA ");
              if (numHeldCalls > 1) {
                 mDsDaHeldCalls = numHeldCalls;
                 numHeldCalls = 1;
              }
           }
        }
        int numChildrenOfActiveCall =
            mCallInfo.isNullCall(activeCall) ? 0 : activeCall.getChildrenIds().size();
        Log.i(TAG, "UHWCS numChildrenOfActiveCall: " + numChildrenOfActiveCall);

        Log.e(TAG, "updateHeadsetWithCallState: numActiveCall :" + numActiveCalls + " numHeldCalls : "
                   + numHeldCalls + " numChildrenOfActiveCall " + numChildrenOfActiveCall +
                  " bluetoothCallState " + bluetoothCallState);

        // Intermediate state for GSM calls which are in the process of being swapped.
        // TODO: Should we be hardcoding this value to 2 or should we check if all top level calls
        //		 are held?
        boolean callsPendingSwitch = (numHeldCalls == 2);

        // For conference calls which support swapping the active BluetoothCall within the
        // conference (namely CDMA calls) we need to expose that as a held BluetoothCall
        // in order for the BT device to show "swap" and "merge" functionality.
        boolean ignoreHeldCallChange = false;
        if (!mCallInfo.isNullCall(activeCall) && activeCall.isConference()
                && !activeCall.can(Connection.CAPABILITY_CONFERENCE_HAS_NO_CHILDREN)) {
          Log.i(TAG, "UHWCS main if: ");
        if (activeCall.can(Connection.CAPABILITY_SWAP_CONFERENCE)) {
          Log.i(TAG, "UHWCS SWAP COnference check ");
            // Indicate that BT device should show SWAP command by indicating that there is a
            // BluetoothCall on hold, but only if the conference wasn't previously merged.
            numHeldCalls = activeCall.wasConferencePreviouslyMerged() ? 0 : 1;
            Log.e(TAG, "updateHeadsetWithCallState: active call has swap conference cap");
        } else if (activeCall.can(Connection.CAPABILITY_MERGE_CONFERENCE)) {
            Log.i(TAG, "UHWCS merge conference check " );
            numHeldCalls = 1;  // Merge is available, so expose via numHeldCalls.
            Log.e(TAG, "updateHeadsetWithCallState: active call has merge conference cap");
          }

        for (String id : activeCall.getChildrenIds()) {
            // Held BluetoothCall has changed due to it being combined into a CDMA conference.
            // Keep track of this and ignore any future update since it doesn't really count
            // as a BluetoothCall change.
            if (mOldHeldCall != null && mOldHeldCall.getTelecomCallId() == id) {
               ignoreHeldCallChange = true;
               Log.e(TAG, "updateHeadsetWithCallState: ignoreHeldCallChange is true");
               break;
            }
         }
        }
        synchronized (LOCK) {
         if (mBluetoothHeadset != null
            && (force
            || (!callsPendingSwitch
            && (numActiveCalls != mNumActiveCalls
            || numChildrenOfActiveCall != mNumChildrenOfActiveCall
            || numHeldCalls != mNumHeldCalls
            || bluetoothCallState != mBluetoothCallState
            || !TextUtils.equals(ringingAddress, mRingingAddress)
            || ringingAddressType != mRingingAddressType
            || (heldCall != mOldHeldCall && !ignoreHeldCallChange))))) {

            // If the BluetoothCall is transitioning into the alerting state, send DIALING first.
            // Some devices expect to see a DIALING state prior to seeing an ALERTING state
            // so we need to send it first.
            boolean sendDialingFirst = mBluetoothCallState != bluetoothCallState
                && bluetoothCallState == CALL_STATE_ALERTING;

            mOldHeldCall = heldCall;
            mNumActiveCalls = numActiveCalls;
            mNumChildrenOfActiveCall = numChildrenOfActiveCall;
            mNumHeldCalls = numHeldCalls;
            mBluetoothCallState = bluetoothCallState;
            mRingingAddress = ringingAddress;
            mRingingAddressType = ringingAddressType;
            mDsdaActiveCalls = numActiveCalls;
            mDsDaCallState = bluetoothCallState;
            mDsDaRingingAddress = ringingAddress;
            mDsDaRingingAddressType = ringingAddressType;

            if (sendDialingFirst) {
               // Log in full to make logs easier to debug.
               Log.i(TAG, "updateHeadsetWithCallState "
                                + "numActive " + mNumActiveCalls + ", "
                                + "numHeld " + mNumHeldCalls + ", "
                                + "callState " + CALL_STATE_DIALING + ", "
                                + "ringing type " + mRingingAddressType);
               mBluetoothHeadset.phoneStateChangedDsDa(
                     mNumActiveCalls,
                     mNumHeldCalls,
                     CALL_STATE_DIALING,
                     mRingingAddress,
                     mRingingAddressType,
                     ringingName);
                mDsDaCallState = CALL_STATE_DIALING;
            }

            Log.i(TAG, "updateHeadsetWithCallState "
                            + "numActive " + mNumActiveCalls + ", "
                            + "numHeld " + mNumHeldCalls + ", "
                            + "callState " + mBluetoothCallState + ", "
                            + "ringing type " + mRingingAddressType);

            mBluetoothHeadset.phoneStateChangedDsDa(
                  mNumActiveCalls,
                  mNumHeldCalls,
                  mBluetoothCallState,
                  mRingingAddress,
                  mRingingAddressType,
                  ringingName);

            mHeadsetUpdatedRecently = true;
            mLastBtHeadsetState = mBluetoothCallState;
          }
        }//synchronized (LOCK){
   }
   private void processOnCallAdded(BluetoothCall call) {

     int numRingingCalls = mCallInfo.getNumRingingCalls();
     BluetoothCall activeCall = mCallInfo.getActiveCall();

     Log.d(TAG, "processOnCallAdded Events");
     //Outgoing call can be initiated anytime except
     //incoming call is not present
     if ((call.getState() == Call.STATE_CONNECTING) ||
        (call.getState() == Call.STATE_DIALING)) {
        Log.d(TAG, "New outgoing call Added");
        if ((activeCall != null && mDsdaActiveCalls == 1)
          && (mDsDaHeldCalls != 0)) {
            //This check is to make sure the new call comes
            //only after active call becomes held
            mDelayOutgoingUpdate = 1;
            Log.d(TAG, "Delaying the Outgoing call update");
            return;
        }
        mDsDaOutgoingCalls++;
        mDsdaTotalcalls++;
        updateHeadsetWithCallState(false /* force */);
     }
     else if ((call.getState() == Call.STATE_RINGING) ||
             (call.getState() == Call.STATE_SIMULATED_RINGING)) {
       //2 incoming calls can be possible
       //so, need to bookmark the incoming calls info
       //incoming call can also come when outgoing call
       //is in progress. Ignore updating incoming call in such case
       if (mDsdaIncomingCalls == 0) {
         Log.d(TAG, "New 1st Incoming call Added");
         mFirstIncomingCallId = call.getTelecomCallId();
         mDsdaIncomingCalls++;
         mDsdaTotalcalls++;
         if (mDsDaOutgoingCalls == 0) {
           updateHeadsetWithCallState(false /* force */);
         }
         else {
           Log.d(TAG, "outgoing call is alerting," +
                        "Not sending incoming call update");
         }
       }
       else if (mDsdaIncomingCalls > 0) {
         //when 2 incoming calls are present, we
         //don't update 2nd call and bookmark this as well
         //as we don't know which call can get accepted
         Log.d(TAG, "New 2nd Incoming call Added");
         if (numRingingCalls == 2) {
           mDsdaIncomingCalls++;
           mDsdaTotalcalls++;
           mDsDaTwoIncomingCallsFlag = 1;
           mSecondIncomingCallId = call.getTelecomCallId();
           Log.d(TAG, "Not updating 2nd Incoming call");
         }
       }
     }
     else if ((call.getState() == Call.STATE_ACTIVE)) {
       if (mDsdaActiveCalls == 0) {
          //if bluetooth headset is connected after active call is ongoing.
          Log.d(TAG, "Call active came when no active calls are present");
          updateHeadsetWithCallState(false /* force */);
          mDsdaActiveCalls = 1;
       }
       else if (mDsdaActiveCalls == 1) {
          Log.d(TAG, "Active call update came when already active call is present");
          if (call.getTelecomCallId().equals(activeCall.getTelecomCallId())) {
            //no need to update this state to remote
            //already existing active call event
            return;
          }
          else {
            //conference call would have been initiated
            conferenceCallInitiated = 1;
            processConferenceCall();
            return;
          }
       }
       if (conferenceCallInitiated == 1) {
        //directing all the next call added events to the ProcessConferenceCall
        processConferenceCall();
       }
     }
     else if ((call.getState() == Call.STATE_HOLDING)) {
       if (mDsDaHeldCalls == 0) {
          Log.d(TAG, "Call held came when no calls are present");
          updateHeadsetWithCallState(false /* force */);
          mDsDaHeldCalls = 1;
       }
       else if (mDsDaHeldCalls == 1) {
          //multiple dsda held calls are present before headset is connected
          //conference fake need not be updated in this case
          Log.d(TAG, "multiple held calls during headset connection"
                      + "no need to fake in this case");
          mDsDaHeldCalls++;
       }
     }
     else if ((call.getState() == Call.STATE_SELECT_PHONE_ACCOUNT)) {
       //When BLDN is triggered and sim needs to be selected,
       //SELECT_PHONE_ACCOUNT will be triggered. We dont send any
       //update over BT until its state turned to connecting or dialing
       mDsdaTotalcalls++;
       mdsDaSelectPhoneAccountFlag = 1;
       mSelectPhoneAccountId = call.getTelecomCallId();
     }
   }

   private void processOnCallRemoved(BluetoothCall call) {

     BluetoothCall dailingCall = mCallInfo.getOutgoingCall();
     int numOutgoingCalls = mCallInfo.isNullCall(dailingCall) ? 0 : 1;
     BluetoothCall activeCall = mCallInfo.getActiveCall();
     int numActiveCalls = mCallInfo.isNullCall(activeCall) ? 0 : 1;
     int numHeldCalls = mCallInfo.getNumHeldCalls();
     int numRingingCalls = mCallInfo.getNumRingingCalls();
     Log.d(TAG, "processOnCallRemoved Events");

     if (numOutgoingCalls != mDsDaOutgoingCalls) {
       // outgoing call ended before answered
       //if incoming call is possible in this scenario
       //need to handle differently
       Log.d (TAG, "outgoing call ended before answered");
       mDsDaOutgoingCalls--;
       updateHeadsetWithCallState(false);
     }
     else if (mDsdaIncomingCalls != numRingingCalls) {
       if ((mSecondIncomingCallId != null) &&
           (mSecondIncomingCallId.equals(call.getTelecomCallId()))) {
         Log.d(TAG, "Non Updated Incoming call is ended");
         mDsdaIncomingCalls--;
         mDsdaTotalcalls--;
         mSecondIncomingCallId = null;
         mDsDaTwoIncomingCallsFlag = 0;
       }
       else if ((mFirstIncomingCallId != null) &&
               (mFirstIncomingCallId.equals(call.getTelecomCallId()))) {
         Log.d(TAG, "Updated incoming call is ended");
         mDsdaIncomingCalls--;
         mDsdaTotalcalls--;
         mFirstIncomingCallId = mSecondIncomingCallId;
         mSecondIncomingCallId = null;
         if (mDsDaOutgoingCalls == 0) {
           updateHeadsetWithCallState(false /*force*/);
         }
         else {
           Log.d(TAG, "incoming call ended while outgoing call is present" +
                       "Not updating this state");
         }
         mDsDaTwoIncomingCallsFlag = 0;
       }
     }
     else if ((mDsdaActiveCalls > 0) || (mDsDaHeldCalls > 0)) {
       Log.d(TAG, "active or held call might have been removed");
       if (mDsDaHeldCalls > numHeldCalls) {
         Log.d(TAG, "mismatch in held calls mDsDaHeldCalls:" +
         mDsDaHeldCalls + "numHeldCalls:" + numHeldCalls);
         if (numHeldCalls == 0) {
           //only one held call earlier.
           //so, need to update this as this is normal scenario
           Log.d(TAG, "only one held call present earlier, updating");
           mDsDaHeldCalls = 0;
           mDsdaTotalcalls--;
           updateHeadsetWithCallState(false /*force */);
         }
         else {
           // no need to update this state..but, need to update clcc.
           mDsDaHeldCalls--;
           mDsdaTotalcalls--;
           Log.d(TAG, "one of the multiple calls removed, not updating");
         }
       }
       if ((mDsdaActiveCalls == 1) && (numActiveCalls == 0)) {
         //this means, active call is ended
         Log.d(TAG, "active call is removed");
         mDsdaActiveCalls = 0;
         mDsdaTotalcalls--;
         updateHeadsetWithCallState(false /*force */);
       }
     }
     else if ((mdsDaSelectPhoneAccountFlag == 1) &&
         (mSelectPhoneAccountId.equals(call.getTelecomCallId()))) {
        //this call state will not be updated if call removed
        //from the select Phone account state before dialing
        Log.d(TAG, "Call removed from SelectPhoneAccount State");
        mdsDaSelectPhoneAccountFlag = 0;
        mSelectPhoneAccountId = null;
     }
     else {
       mDsdaTotalcalls--;
       updateHeadsetWithCallState(false /* force */);
     }
   }

   private void processOnStateChanged(BluetoothCall call) {

     int bluetoothLastState = mLastBtHeadsetState;
     int numRingingCalls = mCallInfo.getNumRingingCalls();
     int numHeldCalls = mCallInfo.getNumHeldCalls();
     BluetoothCall activeCall = mCallInfo.getActiveCall();
     int numActiveCalls = mCallInfo.isNullCall(activeCall) ? 0 : 1;
     BluetoothCall dailingCall = mCallInfo.getOutgoingCall();
     int numOutgoingCalls = mCallInfo.isNullCall(dailingCall) ? 0 : 1;
     int btCallState = getBtCallState(call, false);
     Log.d(TAG, "ProcessOnStateChanged events");

     if ((mdsDaSelectPhoneAccountFlag == 1) &&
         (mSelectPhoneAccountId.equals(call.getTelecomCallId()))) {
        if ((call.getState() == Call.STATE_CONNECTING) ||
           (call.getState() == Call.STATE_DIALING)) {
          Log.d(TAG, "Dialing state update After SelectPhoneAccount");
          mdsDaSelectPhoneAccountFlag = 0;
          mSelectPhoneAccountId = null;
          mDsDaOutgoingCalls++;
          mDsdaTotalcalls++;
          updateHeadsetWithCallState(false /*force*/);
        }
        else {
          //If not call moved to Dialing or connecting, Call would
          //Have been disconnected before selecting the sim.
          //Will remove after the call removed callback.
          Log.d(TAG, "Call Disconnect for SelectPhoneAccount");
        }
        return;
     }
     switch (bluetoothLastState) {
        case CALL_STATE_ALERTING:
        case CALL_STATE_INCOMING:
          if ((btCallState == CALL_STATE_ALERTING) ||
              (btCallState == CALL_STATE_INCOMING) ||
              (btCallState == CALL_STATE_WAITING)) {
             //already updated incoming call no need to update
          }
          else if (btCallState == CALL_STATE_ACTIVE) {
            //either incoming call/alerting call moved to active

            if (mDsDaOutgoingCalls > numOutgoingCalls) {
               //outgoing call is made active
               if (mDsdaIncomingCalls == 0) {
                  Log.d(TAG, "outgoing call became active. DSDS");
                  mDsDaOutgoingCalls--;
                  mDsdaActiveCalls = 1;
                  updateHeadsetWithCallState(false);
               }
               else {
                  Log.d(TAG, "outgoing call became active.when incoming call is present");
                  boolean mOutAct = processPhoneStateChangeParams(CALL_MADE_ACTIVE);
                  mDsdaActiveCalls = 1;
                  mDsDaOutgoingCalls--;
                  Log.d(TAG, "sending waiting call update after 60ms delay");
                  try {
                      Log.d(TAG, "wait for 60Msecs");
                      Thread.sleep(60);
                  } catch (InterruptedException e) {
                      Log.e(TAG, "DsDa Thread was interrupted", e);
                  }
                  if (mOutAct) {
                    Log.d(TAG, "incoming call waiting params during outgoing active");
                    boolean mIncWaitSetup =
                              processPhoneStateChangeParams(CALL_INCOMING_RINGING_SETUP);
                  }
               }
            }
            else if (mDsdaIncomingCalls > numRingingCalls) {
              if ((numRingingCalls == 0) && (mDsdaIncomingCalls == 1)) {
                 if (numOutgoingCalls == 1) {
                    boolean mIncSetupOut =
                              processPhoneStateChangeParams(INCOMING_SETUP_OUTGOING);
                 }
                 Log.d(TAG, "ringing call moved to active. DSDS");
                 mDsdaIncomingCalls--;
                 mDsdaActiveCalls = 1;
                 updateHeadsetWithCallState(false /* force */);
              }
              else if((numRingingCalls == 1) && (mDsdaIncomingCalls == 2)) {
                 Log.d(TAG, "multiple ringing calls, 1 ringing moved to active");
                 if ((mFirstIncomingCallId != null) &&
                      (mFirstIncomingCallId.equals(call.getTelecomCallId()))) {
                   Log.d(TAG, "updated incoming call moved to active");
                   boolean incAct = processPhoneStateChangeParams(CALL_MADE_ACTIVE);
                   mDsdaIncomingCalls--;
                   mDsdaActiveCalls = 1;
                   mFirstIncomingCallId = mSecondIncomingCallId;
                   mSecondIncomingCallId = null;
                   mDsDaTwoIncomingCallsFlag = 0;
                   if (incAct) {
                     Log.d(TAG, "updated 2nd incoming call after 1st moved to active");
                     boolean incWait = processPhoneStateChangeParams(CALL_INCOMING_RINGING_SETUP);
                   }
                 }
                 else if ((mSecondIncomingCallId != null) &&
                         (mSecondIncomingCallId.equals(call.getTelecomCallId()))) {
                    Log.d(TAG, "Un-Updated incoming call made active");
                    mDsdaIncomingCalls--;
                    mSecondIncomingCallId = null;
                    mDsDaTwoIncomingCallsFlag = 0;
                    Log.d(TAG, "sending fake incoming setup and active params for 2nd incoming call");
                    boolean mIncEnded =
                             processPhoneStateChangeParams(CALL_INCOMING_ENDED);
                    if (mIncEnded) {
                      boolean mIncSetup =
                               processPhoneStateChangeParams(CALL_INCOMING_ACTIVE_SETUP);
                      if (mIncSetup) {
                        boolean mIncAct =
                                 processPhoneStateChangeParams(CALL_MADE_ACTIVE);
                        mDsdaActiveCalls = 1;
                        if (mIncAct) {
                          updateHeadsetWithCallState(false /* force */);}}
                    }
                 }
              }
            }
            else if (mDsDaHeldCalls > numHeldCalls) {
               if ((mDsDaHeldCalls == 1) && (numHeldCalls == 0)) {
                 Log.d(TAG, "only 1 held call present, moving it to active");
                 mDsDaHeldCalls = 0;
                 mDsdaActiveCalls = 1;
                 updateHeadsetWithCallState(false /*force*/);
               }
               else if ((mDsDaHeldCalls > 1) && (numHeldCalls > 0)) {
                  Log.d(TAG, "held call made active");
                  //one of the held call made active
                  mDsDaHeldCalls--;
                  mDsdaActiveCalls = 1;
                  updateHeadsetWithCallState(false /* force */);
               }
            }
          }
          else if (btCallState == CALL_STATE_HELD) {
            if ((numActiveCalls == 0) && (mDsdaActiveCalls == 1)) {
              Log.d(TAG, "active call is made to held");
              if (numHeldCalls > mDsDaHeldCalls) {
                if (numHeldCalls >= 2) {
                  Log.d(TAG, "multiple held calls now");
                  //held call to active,
                  //active to held
                  //noheldcalls
                  Log.d(TAG, "faking multiple held conference params without active call");
                  boolean mhelAct =
                            processPhoneStateChangeParams(ADD_HELDCALLS_TO_ACTIVE);
                  if (mhelAct) {
                    boolean mActHel =
                            processPhoneStateChangeParams(MOVE_ACTIVE_TO_HELD);
                  }
                }
                else {
                    updateHeadsetWithCallState(false);
                    Log.d(TAG, "1st held call from active");
                }
              }
              mDsDaHeldCalls++;
              mDsdaActiveCalls = 0;
            }
         }
         else if (btCallState == CALL_STATE_IDLE) {
           if ((call.getState() == Call.STATE_DISCONNECTED) || 
               (call.getState() == Call.STATE_DISCONNECTING)) {
             Log.d(TAG, "StateChange: received Disconnected event");
             if (mDsdaIncomingCalls > numRingingCalls) {
             Log.d(TAG, "disconnect event for ringing call");
                if ((numRingingCalls == 0) && (mDsdaIncomingCalls == 1)) {
                   Log.d(TAG, "normal dsds scenario ringing call ended, lets handle in callremoved");
                    //updateHeadsetWithCallState(false /* force */);
                    break;
                }
                if ((mFirstIncomingCallId != null) &&
                    (mFirstIncomingCallId.equals(call.getTelecomCallId()))) {
                    if (mDsDaTwoIncomingCallsFlag == 1) {
                       Log.d(TAG, "fake indicators_updated Incoming call ended");
                       boolean incomingended =
                                processPhoneStateChangeParams(CALL_INCOMING_ENDED);
                       if (incomingended) {
                          boolean CallSetupInd =
                                  processPhoneStateChangeParams(CALL_INCOMING_RINGING_SETUP);
                       }
                    }
                }
                else if ((mSecondIncomingCallId != null) &&
                      (mSecondIncomingCallId.equals(call.getTelecomCallId()))) {
                  if (mDsDaTwoIncomingCallsFlag == 1) {
                    Log.d(TAG, "fake indicators_non-updated Incoming call ended");
                    Log.d(TAG, "no need to update this state");
                  }
                }
             }
             else if (mDsDaHeldCalls > numHeldCalls) {
               Log.d(TAG, "one of the or the only held call might have been disconnected");
               if ((mDsDaHeldCalls >= 1) && (numHeldCalls == 0)) {
                 Log.d(TAG, "only existing held call/all held calls are disconnected");
                 updateHeadsetWithCallState(false /* force */);
               }
               else if ((mDsDaHeldCalls > 1) && (numHeldCalls < mDsDaHeldCalls)
                                          && (numHeldCalls > 0)) {
                 Log.d(TAG, "one of the multiple held calls are disconencted");
                 Log.d(TAG, "Lets not update anything here");
                 //need to see if it impacts anything if mDsDaHeldCalls state is updated
                 //if not updated, if any of the other state change event comes prior to
                 //call removed, it should not vary the functionality.
                 //clcc response will be taken care in sendclcc function
                 mDsDaHeldCalls = numHeldCalls;
               }
             }
             else if ((mDsdaActiveCalls == 1) && (numActiveCalls == 0)) {
               Log.d(TAG, "Means the active call is ended");
               updateHeadsetWithCallState(false /* force */);
             }
             else if ((mDsDaOutgoingCalls == 1) && (numOutgoingCalls == 0)) {
               if (mDsdaIncomingCalls == 0) {
                 //updateHeadsetWithCallState(false);
                 //will be handled in call removed
                 Log.d(TAG, "outgoing call ended before active while no incoming.Handling in Call removed");
               }
               else {
                 Log.d(TAG, "outgoing call ended while incoming call is also there. sending fake incoming params");
                 mDsDaOutgoingCalls --;
                 boolean Outgoing_ended = processPhoneStateChangeParams(CALL_INCOMING_ENDED);
                 Log.d(TAG, "sending waiting call update after 50ms delay");
                 try {
                     Log.d(TAG, "wait for 50Msecs");
                     Thread.sleep(50);
                  } catch (InterruptedException e) {
                      Log.e(TAG, "DsDa Thread was interrupted", e);
                  }
                 if (Outgoing_ended) {
                   boolean Inc_setup = processPhoneStateChangeParams(CALL_INCOMING_RINGING_SETUP);
                 }
               }
             }
           }
         }
       break;
       case CALL_STATE_IDLE:
          //so it can have active or held or no calls.cannot have incoming/alerting call
          Log.d(TAG, "previous bt state is idle:" + bluetoothLastState);

          if (btCallState == CALL_STATE_HELD) { //received call event state
            Log.d(TAG, "recevied call event as held event");
            if (mDsDaHeldCalls < numHeldCalls) {
              Log.d(TAG, "new held call is received from active");
              if ((numActiveCalls == 0) && (mDsdaActiveCalls == 1)) {
                //ideally here the active call should be none
                Log.d(TAG, "active calls 0 and held calls more than 0");
              }
              if ((mDsDaHeldCalls > 0) && (numHeldCalls>1)) {
                Log.d(TAG, "Multiple held event came");
                if ((mDsDaHeldCalls == 1) && (numHeldCalls == 2)) {
                   if (mTelephonyManager != null) {
                      if (!(mTelephonyManager.isConcurrentCallsPossible()
                            || mTelephonyManager.isDsdaOrDsdsTransitionMode())) {
                         Log.i(TAG, "Concurrent Calls Not Possible: Not DSDA ");
                         Log.i(TAG, "Call swapping is in progress ");
                         mCallSwapPending = 1;
                         updateHeadsetWithCallState(false);
                         return;
                      }
                   }
                }
                Log.d(TAG, "sending fake params call_active_ended");
                boolean mHelAct =
                        processPhoneStateChangeParams(ADD_HELDCALLS_TO_ACTIVE); //no held calls
                if (mHelAct) {
                  boolean mActHel =
                          processPhoneStateChangeParams(MOVE_ACTIVE_TO_HELD);
                  mDsdaActiveCalls=0;
                  mDsDaHeldCalls++;
                }
                if (mDelayOutgoingUpdate == 1) {
                    Log.d(TAG, "sending delayed outgoing update after prev call moved to held");
                    mDsDaOutgoingCalls++;
                    mDsdaTotalcalls++;
                    updateHeadsetWithCallState(false);
                    mDelayOutgoingUpdate = 0;
                }
              }
              else if ((mDsDaHeldCalls == 0) && (numHeldCalls ==1)) {
                Log.d(TAG, "when only 1 active call and moved to held call");
                mDsdaActiveCalls = 0;
                mDsDaHeldCalls++;
                updateHeadsetWithCallState(false);
              }
            }
          }
          else if (btCallState == CALL_STATE_IDLE) {
             //this means, received call state event can be in ringing state or disconnected
             //will be handled in call removed
             //this might be one of the held call would have been ended
             //check the behaviour of remote update and change accoordingly.
             //this need not be handled here.. would have been handled in oncalladded event or oncallremoved event
             if ((call.getState() == Call.STATE_NEW) ||
                 (call.getState() == Call.STATE_AUDIO_PROCESSING) ||
                 (call.getState() == Call.STATE_SIMULATED_RINGING)) {
               Log.d(TAG, "ignoring these call state events");
             }
             else if ((call.getState() == Call.STATE_DISCONNECTED) ||
                      (call.getState() == Call.STATE_DISCONNECTING)) {
               Log.d(TAG, "this event can come for either held or active call");
               if ((numActiveCalls == 0) && (mDsdaActiveCalls == 1)) {
                 Log.d(TAG, "active call ended event is received");
                 updateHeadsetWithCallState(false /* force */);
                 mDsdaActiveCalls = 0;
               }
               else if (numHeldCalls < mDsDaHeldCalls) {
                 if ((numHeldCalls > 0) && (mDsDaHeldCalls > 1)) {
                   Log.d(TAG, "multiple held calls..one of the held is ended");
                   //no update here
                 }
                 else if ((numHeldCalls == 0) && (mDsDaHeldCalls == 1)) {
                   Log.d(TAG, "only 1 held call present and is ended");
                   updateHeadsetWithCallState(false /* force */);
                 }
               }
             }
             else if (call.getState() == Call.STATE_RINGING) {
                 //this should not come because call added should come 1st. so
                 //ignore this command
                 Log.d(TAG, "this ringing should not come will handle only after oncall added");
             }
         }
         else if (btCallState == CALL_STATE_ACTIVE) {
             if (mCallSwapPending == 1) {
                updateHeadsetWithCallState(false /* force */);
                mCallSwapPending = 0;
                return;
             }
             if (numHeldCalls < mDsDaHeldCalls) {
                  Log.d(TAG, "one of the held is moved to active");
                 //for this to work, active should have been none earlier
                 if ((numActiveCalls == 1) && (mDsdaActiveCalls == 0)) {
                     if ((mDsDaHeldCalls == 1) && (numHeldCalls == 0)) {
                         Log.d(TAG, "only held call moved to active");
                         mDsdaActiveCalls = 1;
                         mDsDaHeldCalls--;
                         updateHeadsetWithCallState(false /*force */);
                     }
                     else {
                         //As per the stack implementation, Active call
                         //can come either from held state, incoming or
                         //from outgoing. so, for multiple held to one active
                         //we will send as if held is made as active and
                         //then update the held call info
                         Log.d(TAG, "multiple held, one moved to active");
                         boolean mNoHeld =
                                  processPhoneStateChangeParams(ADD_HELDCALLS_TO_ACTIVE);
                         mDsDaHeldCalls--;
                         mDsdaActiveCalls = 1;
                         if (mNoHeld) {
                            updateHeadsetWithCallState(false);
                         }
                     }
                 } else {
                      Log.d(TAG, "conference call event came");
                      updateHeadsetWithCallState(false);
                 }
             } else {
                  Log.d(TAG, "silent ringing call moved to active");
                  mDsdaIncomingCalls--;
                  mFirstIncomingCallId = null;
                  updateHeadsetWithCallState(false);
             }
         } else if (btCallState == CALL_STATE_DISCONNECTED) {
             Log.d(TAG, "call disconnected during idle state. handling in call removed");
             //will be handled in call_removed
         }
         else if ((btCallState == CALL_STATE_INCOMING) ||
                 (btCallState == CALL_STATE_WAITING) ||
                 (btCallState == CALL_STATE_ALERTING)) {
             Log.d(TAG, "incoming/alerting during idle state. would have been updated in oncalladded");
             //this would have been handled in oncallAdded state
             //we can call updateheadsetwithcallstate based on restricting details it gets
             //updateHeadsetWithCallState(false);
         }
       break;
       }
    }
   private void processConferenceCall() {
       int numHeldCalls = mCallInfo.getNumHeldCalls();
       Log.d(TAG, "process conference call");
       if (conferenceCallInitiated == 1) {
        //Probably no need to update headset in this case
        //confirmation can be done during testing
        Log.d(TAG, "not updating conference call events");
       }
   }
  private boolean processPhoneStateChangeParams(int event) {
    boolean ret = false;
    int held_calls = 0;
    int active_calls = 0;
    if (mBluetoothHeadset == null) {
       Log.e(TAG, " bluetoothHeadset is null ");
       ret = false;
    }
    switch (event) {
      case CALL_INCOMING_ENDED:
         mDsDaCallState = CALL_STATE_IDLE;
         if (mDsDaHeldCalls != 0) {
            held_calls = 1;
         }
         if (mDsdaActiveCalls != 0) {
            active_calls = 1;
         }
         Log.d(TAG, "processPhoneStateChangeParams: CALL_INCOMING_ENDED");
         if (mBluetoothHeadset != null) {
           mBluetoothHeadset.phoneStateChangedDsDa(
           active_calls,
           held_calls,
           mDsDaCallState,
           null,
           mDsDaRingingAddressType,
           null);
         }
         mLastBtHeadsetState = mDsDaCallState;
         ret = true;
       break;
     case CALL_INCOMING_ACTIVE_SETUP:
     case CALL_INCOMING_RINGING_SETUP:
         BluetoothCall ringingCall = null;
         if (event == CALL_INCOMING_RINGING_SETUP) {
            ringingCall = mCallInfo.getRingingOrSimulatedRingingCall();
         }
         else if (event == CALL_INCOMING_ACTIVE_SETUP) {
              ringingCall = mCallInfo.getActiveCall();
         }
         String ringingAddress = null;
         int ringingAddressType = DEFAULT_RINGING_ADDRESS_TYPE;
         String ringingName = null;
         if (!mCallInfo.isNullCall(ringingCall) && ringingCall.getHandle() != null
                    && !ringingCall.isSilentRingingRequested()) {
            ringingAddress = ringingCall.getHandle().getSchemeSpecificPart();
            if (ringingAddress != null) {
               ringingAddressType = PhoneNumberUtils.toaFromString(ringingAddress);
            }
            ringingName = ringingCall.getCallerDisplayName();
            if (TextUtils.isEmpty(ringingName)) {
               ringingName = ringingCall.getContactDisplayName();
            }
        }
        if (ringingAddress == null) {
           ringingAddress = "";
        }
        mDsDaCallState = CALL_STATE_INCOMING;
        if (mDsdaActiveCalls != 0) {
           active_calls = 1;
        }
        if (mDsDaHeldCalls != 0) {
           held_calls = 1;
        }
        Log.d(TAG, "processPhoneStateChangeParams: CALL_STATE_INCOMING");
        if (mBluetoothHeadset != null) {
          mBluetoothHeadset.phoneStateChangedDsDa(
          active_calls,
          held_calls,
          mDsDaCallState,
          ringingAddress,
          ringingAddressType,
          ringingName);
        }
        mLastBtHeadsetState = mDsDaCallState;
        ret = true;
      break;
     case CALL_ACTIVE_ENDED:
        mDsDaCallState = CALL_STATE_IDLE;
        active_calls = 0;
        if (mDsDaHeldCalls != 0) {
           held_calls = 1;
        }
        Log.d(TAG, "processPhoneStateChangeParams: CALL_STATE_IDLE");
        if (mBluetoothHeadset != null) {
          mBluetoothHeadset.phoneStateChangedDsDa(
          active_calls,
          held_calls,
          mDsDaCallState,
          null,
          mDsDaRingingAddressType,
          null);
        }
        mLastBtHeadsetState = mDsDaCallState;
        ret = true;
      break;
     case ADD_HELDCALLS_TO_ACTIVE:
        if (mDsdaIncomingCalls > 0) {
           mDsDaCallState = CALL_STATE_INCOMING;
        }
        else {
           mDsDaCallState = CALL_STATE_IDLE;
        }
        held_calls = 0;
        active_calls = 1;
        Log.d(TAG, "processPhoneStateChangeParams: ADD_HELDCALLS_TO_ACTIVE");
        if (mBluetoothHeadset != null) {
          mBluetoothHeadset.phoneStateChangedDsDa(
          active_calls,
          held_calls,
          mDsDaCallState,
          null,
          mDsDaRingingAddressType,
          null);
        }
        mLastBtHeadsetState = mDsDaCallState;
        ret = true;
      break;
     case MOVE_ACTIVE_TO_HELD:
        if (mDsdaIncomingCalls > 0) {
           mDsDaCallState = CALL_STATE_INCOMING;
        }
        else {
           mDsDaCallState = CALL_STATE_IDLE;
        }
        held_calls = 1;
        active_calls = 0;
        Log.d(TAG, "processPhoneStateChangeParams: MOVE_ACTIVE_TO_HELD");
        if (mBluetoothHeadset != null) {
          mBluetoothHeadset.phoneStateChangedDsDa(
          active_calls,
          held_calls,
          mDsDaCallState,
          null,
          mDsDaRingingAddressType,
          null);
        }
        mLastBtHeadsetState = mDsDaCallState;
        ret = true;
      break;
     case CALL_MADE_ACTIVE:
        active_calls = 1;
        mDsDaCallState = CALL_STATE_IDLE;
        mDsDaRingingAddress = null;
        mDsDaRingingAddressType = DEFAULT_RINGING_ADDRESS_TYPE;
        mDsDaRingingName = null;
        if (mDsDaHeldCalls != 0) {
           held_calls = 1;
        }
        Log.d(TAG, "processPhoneStateChangeParams: CALL_MADE_ACTIVE");
        if (mBluetoothHeadset != null) {
          mBluetoothHeadset.phoneStateChangedDsDa(
          active_calls,
          mDsDaHeldCalls,
          mDsDaCallState,
          mDsDaRingingAddress,
          mDsDaRingingAddressType,
          mDsDaRingingName);
        }
        mLastBtHeadsetState = mDsDaCallState;
        ret = true;
      break;
     case INCOMING_SETUP_OUTGOING:
        BluetoothCall incRingingCall = getBluetoothCallById(mFirstIncomingCallId);
        String incRingingAddress = null;
        int incRingingAddressType = DEFAULT_RINGING_ADDRESS_TYPE;
        String incRingingName = null;
        if (!mCallInfo.isNullCall(incRingingCall) && incRingingCall.getHandle() != null
                   && !incRingingCall.isSilentRingingRequested()) {
           incRingingAddress = incRingingCall.getHandle().getSchemeSpecificPart();
           if (incRingingAddress != null) {
              incRingingAddressType = PhoneNumberUtils.toaFromString(incRingingAddress);
           }
           incRingingName = incRingingCall.getCallerDisplayName();
           if (TextUtils.isEmpty(incRingingName)) {
              incRingingName = incRingingCall.getContactDisplayName();
           }
       }
       if (incRingingAddress == null) {
          incRingingAddress = "";
       }
       mDsDaCallState = CALL_STATE_INCOMING;
       if (mDsdaActiveCalls != 0) {
          active_calls = 1;
       }
       if (mDsDaHeldCalls != 0) {
          held_calls = 1;
       }
       Log.d(TAG, "processPhoneStateChangeParams: CALL_STATE_INCOMING");
       if (mBluetoothHeadset != null) {
         mBluetoothHeadset.phoneStateChangedDsDa(
         active_calls,
         held_calls,
         mDsDaCallState,
         incRingingAddress,
         incRingingAddressType,
         incRingingName);
       }
       mLastBtHeadsetState = mDsDaCallState;
       ret = true;
     break;
     }
    return ret;
  }

  private int getBluetoothCallStateForUpdate() {
     BluetoothCall ringingCall = mCallInfo.getRingingOrSimulatedRingingCall();
     BluetoothCall dialingCall = mCallInfo.getOutgoingCall();
     boolean hasOnlyDisconnectedCalls = mCallInfo.hasOnlyDisconnectedCalls();

     if ((mDsdaIncomingCalls > 0) && (mDsDaOutgoingCalls > 0)) {
        Log.d(TAG, "when both outgoing and ringing are present" +
                    "ignore ringing call state update");
        ringingCall = null;
     }
     if ((mDsDaOutgoingCalls > 0) && (mDsdaIncomingCalls == 0)
          && (mDsdaActiveCalls == 1)) {
         Log.d(TAG, "incoming became active when dailing present" +
                    "ignore outgoing call state");
        dialingCall = null;
     }
     //
     // !! WARNING !!
     // You will note that CALL_STATE_WAITING, CALL_STATE_HELD, and CALL_STATE_ACTIVE are not
     // used in this version of the BluetoothCall state mappings.  This is on purpose.
     // phone_state_change() in btif_hf.c is not written to handle these states. Only with the
     // listCalls*() method are WAITING and ACTIVE used.
     // Using the unsupported states here caused problems with inconsistent state in some
     // bluetooth devices (like not getting out of ringing state after answering a call).
     //
     int bluetoothCallState = CALL_STATE_IDLE;
     if (!mCallInfo.isNullCall(ringingCall) && !ringingCall.isSilentRingingRequested()) {
        bluetoothCallState = CALL_STATE_INCOMING;
     } else if ((!mCallInfo.isNullCall(dialingCall))
                &&( (dialingCall.getState() == Call.STATE_DIALING) ||
                     (dialingCall.getState() == Call.STATE_CONNECTING) ||
                     (dialingCall.getState() == Call.STATE_PULLING_CALL) )) {
          Log.i(TAG, "getBluetoothCallStateForUpdate, getExtras:  " + dialingCall.getDetails().getExtras());
         if (( null != dialingCall.getDetails().getExtras() &&
              0 != dialingCall.getDetails().getExtras().getInt(TelecomManager.EXTRA_CALL_TECHNOLOGY_TYPE))
            || dialingCall.getDetails().hasProperty(Call.Details.PROPERTY_VOIP_AUDIO_MODE))  {
            bluetoothCallState = CALL_STATE_ALERTING;
            Log.i(TAG, "updateHeadsetWithCallState CALL_STATE_ALERTING");
         } else {
            Log.i(TAG, "Not Updating alerting state as PhoneType is 0");
            mDsDaHighDefCallFlag = 1;
            mDialingCallId = dialingCall.getTelecomCallId();
         }
     } else if (hasOnlyDisconnectedCalls || mIsDisconnectedTonePlaying) {
        // Keep the DISCONNECTED state until the disconnect tone's playback is done
        bluetoothCallState = CALL_STATE_DISCONNECTED;
     }
     return bluetoothCallState;
  }

  private int getBtCallState(BluetoothCall call, boolean isForeground) {
      switch (call.getState()) {
        case Call.STATE_NEW:
        case Call.STATE_DISCONNECTED:
        case Call.STATE_AUDIO_PROCESSING:
            return CALL_STATE_IDLE;

        case Call.STATE_ACTIVE:
            return CALL_STATE_ACTIVE;

        case Call.STATE_CONNECTING:
        case Call.STATE_SELECT_PHONE_ACCOUNT:
        case Call.STATE_DIALING:
        case Call.STATE_PULLING_CALL:
            // Yes, this is correctly returning ALERTING.
            // "Dialing" for BT means that we have sent information to the service provider
            // to place the BluetoothCall but there is no confirmation that the BluetoothCall
            // is going through. When there finally is confirmation, the ringback is
            // played which is referred to as an "alert" tone, thus, ALERTING.
            // TODO: We should consider using the ALERTING terms in Telecom because that
            // seems to be more industry-standard.
            return CALL_STATE_ALERTING;

        case Call.STATE_HOLDING:
            return CALL_STATE_HELD;

        case Call.STATE_RINGING:
        case Call.STATE_SIMULATED_RINGING:
            if (call.isSilentRingingRequested()) {
                return CALL_STATE_IDLE;
            } else if (isForeground) {
                return CALL_STATE_INCOMING;
            } else {
                return CALL_STATE_WAITING;
            }
      }
      return CALL_STATE_IDLE;
  }

  public CallStateCallback getCallback(BluetoothCall call) {
    Log.d(TAG, "getCallback");
    return mCallbacks.get(call.getTelecomCallId());
  }

  public void setBluetoothHeadset(BluetoothHeadsetProxy bluetoothHeadset) {
    mBluetoothHeadset = bluetoothHeadset;
  }

  public BluetoothCall getBluetoothCallById(String id) {
    if (mBluetoothDsDaCallHashMap.containsKey(id)) {
        return mBluetoothDsDaCallHashMap.get(id);
    }
    return null;
  }

  public List<BluetoothCall> getBluetoothCallsByIds(List<String> ids) {
    List<BluetoothCall> calls = new ArrayList<>();
    for (String id : ids) {
        BluetoothCall call = getBluetoothCallById(id);
        if (!mCallInfo.isNullCall(call)) {
            calls.add(call);
        }
    }
    return calls;
  }

  // extract call information functions out into this part, so we can mock it in testing
  public class CallInfo {

      public BluetoothCall getForegroundCall() {
          LinkedHashSet<Integer> states = new LinkedHashSet<Integer>();
          BluetoothCall foregroundCall;

          states.add(Call.STATE_CONNECTING);
          foregroundCall = getCallByStates(states);
          if (!mCallInfo.isNullCall(foregroundCall)) {
            return foregroundCall;
          }

          states.clear();
          states.add(Call.STATE_ACTIVE);
          states.add(Call.STATE_DIALING);
          states.add(Call.STATE_PULLING_CALL);
          foregroundCall = getCallByStates(states);
          if (!mCallInfo.isNullCall(foregroundCall)) {
            return foregroundCall;
          }

          states.clear();
          states.add(Call.STATE_RINGING);
          foregroundCall = getCallByStates(states);
          if (!mCallInfo.isNullCall(foregroundCall)) {
            return foregroundCall;
          }

          return null;
      }

      public BluetoothCall getCallByStates(LinkedHashSet<Integer> states) {
          List<BluetoothCall> calls = getBluetoothCalls();
          for (BluetoothCall call : calls) {
            if (states.contains(call.getState())) {
                return call;
            }
          }
          return null;
      }

      public BluetoothCall getCallByState(int state) {
          List<BluetoothCall> calls = getBluetoothCalls();
          for (BluetoothCall call : calls) {
            if (state == call.getState()) {
                return call;
            }
          }
          return null;
      }

      public int getNumHeldCalls() {
          int number = 0;
          List<BluetoothCall> calls = getBluetoothCalls();
          for (BluetoothCall call : calls) {
            if (call.getState() == Call.STATE_HOLDING) {
                number++;
            }
          }
          return number;
      }

      public int getNumRingingCalls() {
          int number = 0;
          List<BluetoothCall> calls = getBluetoothCalls();
          for (BluetoothCall call : calls) {
            if (call.getState() == Call.STATE_RINGING) {
                number++;
            }
          }
          return number;
      }

      public boolean hasOnlyDisconnectedCalls() {
          List<BluetoothCall> calls = getBluetoothCalls();
          if (calls.size() == 0) {
              return false;
          }
          for (BluetoothCall call : calls) {
            if (call.getState() != Call.STATE_DISCONNECTED) {
                return false;
            }
          }
          return true;
      }

      public List<BluetoothCall> getBluetoothCalls() {
          return getBluetoothCallsByIds(BluetoothCall.getIds(getCalls()));
      }

      public BluetoothCall getOutgoingCall() {
          LinkedHashSet<Integer> states = new LinkedHashSet<Integer>();
          states.add(Call.STATE_CONNECTING);
          states.add(Call.STATE_DIALING);
          states.add(Call.STATE_PULLING_CALL);
          return getCallByStates(states);
      }

      public BluetoothCall getRingingOrSimulatedRingingCall() {
          LinkedHashSet<Integer> states = new LinkedHashSet<Integer>();
          states.add(Call.STATE_RINGING);
          states.add(Call.STATE_SIMULATED_RINGING);

          if ((mDsdaIncomingCalls > 0) && (mDsDaOutgoingCalls > 0)) {
             Log.d(TAG, "when both outgoing and ringing are present" +
                         "ignore ringing call state update");
             return null;
          }
          return getCallByStates(states);
      }

      public BluetoothCall getActiveCall() {
          return getCallByState(Call.STATE_ACTIVE);
      }

      public BluetoothCall getHeldCall() {
          return getCallByState(Call.STATE_HOLDING);
      }

      public BluetoothCall getRingingCall() {
          return getCallByState(Call.STATE_RINGING);
      }

    /**
     * Returns the best phone account to use for the given state of all calls.
     * First, tries to return the phone account for the foreground call, second the default
     * phone account for PhoneAccount.SCHEME_TEL.
     */
      public PhoneAccount getBestPhoneAccount() {
          BluetoothCall call = getForegroundCall();

          PhoneAccount account = null;
          if (!mCallInfo.isNullCall(call)) {
            PhoneAccountHandle handle = call.getAccountHandle();
            if (handle != null) {
                // First try to get the network name of the foreground call.
                account = mTelecomManager.getPhoneAccount(handle);
            }
          }

          if (account == null) {
            // Second, Try to get the label for the default Phone Account.
            List<PhoneAccountHandle> handles =
                mTelecomManager.getPhoneAccountsSupportingScheme(PhoneAccount.SCHEME_TEL);
            while (handles.iterator().hasNext()) {
                account = mTelecomManager.getPhoneAccount(handles.iterator().next());
                if (account != null) {
                    return account;
                }
            }
          }
          return null;
      }

      public boolean isNullCall(BluetoothCall call) {
          return call == null || call.getCall() == null;
      }
  };
};
