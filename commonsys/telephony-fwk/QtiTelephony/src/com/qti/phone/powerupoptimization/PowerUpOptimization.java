/*
 * Copyright (c) 2021, 2023-2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qti.phone.powerupoptimization;

import static java.util.Arrays.copyOf;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.UiccSlotInfo;
import android.telephony.ims.ImsMmTelManager;
import android.telephony.ims.feature.ImsFeature;
import android.text.TextUtils;
import android.util.Log;

import com.qti.phone.QtiMsgTunnelClient;
import com.qti.phone.QtiMsgTunnelClient.InternalOemHookCallback;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PowerUpOptimization {
    private static final String TAG = "PowerUpOptimization";

    private int mNumPhones;
    private boolean mIsImsSupported;
    private final Object mIsAtelReadySentLock = new Object();
    private Map<Integer, ImsMmTelManager> mImsMmTelManagers;
    private Set<Integer> mAvailableSubs = new HashSet<>();

    private Context mContext;
    private QtiMsgTunnelClient mQtiMsgTunnelClient;
    private TelephonyManager mTelephonyManager;
    private SubscriptionManager mSubscriptionManager;

    // Indicates that IMS stack is up
    private boolean[] mIsImsStackUpForSlot;

    // Indicates that connection to IRadio HAL has been established
    private boolean[] mIsRilConnectedForSlot;

    // Indicates that connection to IQtiOemHook HAL has been established
    private boolean mIsOemHookConnected;

    private boolean[] mIsAtelReadySentForSlot;

    private boolean[] mIsGetImsStateInProgress;

    private static final int READY = 1;

    // Timeout for consecutive IMS state query
    private static final int TIMEOUT_MILLIS = 1000;
    // Try checking for IMS stack up for at most 2 minutes
    // from TIMEOUT_MILLIS * MAX_RETRY_COUNT
    private static final int MAX_RETRY_COUNT = 60 * 2 - 1;
    private static final int RETRY_COUNT_INITIALIZER = -1;

    // Events for the handler
    private static final int GET_IMS_STATE = 1;
    private static final int EVENT_SIM_DISABLED = 2;
    private static final int EVENT_IMS_STACK_UP = 3;
    private static final int EVENT_RIL_CRASH = 4;
    private static final int EVENT_MULTI_SIM_CONFIG_CHANGED = 5;
    private static final int EVENT_RADIO_POWER_STATE_CHANGED = 6;
    private static final int EVENT_SIM_STATE_ABSENT = 7;
    private static final int EVENT_SIM_STATE_LOADED = 8;
    private static final int EVENT_IMS_NOT_SUPPORTED = 9;

    private static final String EXTRA_STATE = "state";
    private static final String INTENT_KEY_ICC_STATE = "ss";
    private static final String INTENT_VALUE_ICC_NOT_READY = "NOT_READY";

    private static final String ACTION_RADIO_POWER_STATE_CHANGED =
            "org.codeaurora.intent.action.RADIO_POWER_STATE";
    private static final String ACTION_SIM_STATE_CHANGED =
            "android.intent.action.SIM_STATE_CHANGED";

    private static final String ACTION_ESSENTIAL_RECORDS_LOADED =
            "org.codeaurora.intent.action.ESSENTIAL_RECORDS_LOADED";

    private boolean mIsIntentRegistered = false;
    private int mPowerUpOptimizationPropVal = 0;
    private boolean mIsBootup = true;

    private PowerUpOptHandler mPowerUpOptHandler;
    private static PowerUpOptimization mPowerUpOptimization;

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    public static PowerUpOptimization getInstance(Context context,int propVal) {
        if (mPowerUpOptimization == null) {
            mPowerUpOptimization = new PowerUpOptimization(context, propVal);
        }
        return mPowerUpOptimization;
    }

    public static PowerUpOptimization getInstance() {
        return mPowerUpOptimization;
    }

    private PowerUpOptimization(Context context, int propVal) {
        log("PowerUpOptimization started");
        mContext = context;
        mPowerUpOptimizationPropVal = propVal;

        mTelephonyManager = mContext.getSystemService(TelephonyManager.class);
        mSubscriptionManager = mContext.getSystemService(SubscriptionManager.class);
        if (mSubscriptionManager != null) {
            mSubscriptionManager = mSubscriptionManager.createForAllUserProfiles();
        }

        mNumPhones = mTelephonyManager.getActiveModemCount();
        mIsRilConnectedForSlot = new boolean[mNumPhones];
        mIsImsStackUpForSlot = new boolean[mNumPhones];
        mIsAtelReadySentForSlot = new boolean[mNumPhones];
        mIsGetImsStateInProgress = new boolean[mNumPhones];
        mImsMmTelManagers = new HashMap<>();

        HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        mPowerUpOptHandler = new PowerUpOptHandler(thread.getLooper());
        mQtiMsgTunnelClient = QtiMsgTunnelClient.getInstance();
        if (mQtiMsgTunnelClient == null) {
            return;
        }
        mQtiMsgTunnelClient.registerOemHookCallback(mOemHookCallback);

        if (Looper.myLooper() == null) {
            Log.e(TAG, "Preparing Looper");
            Looper.prepare();
        }

        mIsImsSupported =
                mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY_IMS);
        if (!mIsImsSupported) {
            mPowerUpOptHandler.obtainMessage(EVENT_IMS_NOT_SUPPORTED).sendToTarget();
        }
        registerForIntents();

    }

    private void onImsNotSupported() {
        log("IMS is not supported");
        // Set IMS stack up bit for all slots
        for (int slot = 0 ; slot < mNumPhones; slot++) {
            mIsImsStackUpForSlot[slot] = true;
        }
        trySendPhoneReadyForAllSlots();
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            String action = intent.getAction();
            log("onReceive: " + action);
            if (ACTION_RADIO_POWER_STATE_CHANGED.equals(action)) {
                int slotIdExtra = intent.getIntExtra(SubscriptionManager.EXTRA_SLOT_INDEX,
                        SubscriptionManager.INVALID_SIM_SLOT_INDEX);
                int radioStateExtra = intent.getIntExtra(EXTRA_STATE,
                        TelephonyManager.RADIO_POWER_UNAVAILABLE);
                mPowerUpOptHandler.obtainMessage(EVENT_RADIO_POWER_STATE_CHANGED, slotIdExtra,
                        radioStateExtra).sendToTarget();
            } else if (TelephonyManager.ACTION_SIM_APPLICATION_STATE_CHANGED.equals(action)
                    || TelephonyManager.ACTION_SIM_CARD_STATE_CHANGED.equals(action)) {
                final int simState = intent.getIntExtra(TelephonyManager.EXTRA_SIM_STATE,
                        TelephonyManager.SIM_STATE_UNKNOWN);
                final int slotId = intent.getIntExtra(SubscriptionManager.EXTRA_SLOT_INDEX,
                        SubscriptionManager.INVALID_SIM_SLOT_INDEX);
                log("SIM intent: simState= " + simState + " slotId= " + slotId + " mIsBootup =" +
                        mIsBootup);
                if (isValidSlotIndex(slotId)) {
                    if (simState == TelephonyManager.SIM_STATE_ABSENT
                            || (simState == TelephonyManager.SIM_STATE_UNKNOWN &&
                                    mIsBootup)
                            || simState == TelephonyManager.SIM_STATE_CARD_RESTRICTED
                            || simState == TelephonyManager.SIM_STATE_CARD_IO_ERROR) {
                        mPowerUpOptHandler.obtainMessage(EVENT_SIM_STATE_ABSENT,
                                slotId).sendToTarget();
                    } else if (simState == TelephonyManager.SIM_STATE_LOADED
                            || isSimLocked(simState)) {
                        mPowerUpOptHandler.obtainMessage(EVENT_SIM_STATE_LOADED,
                                slotId).sendToTarget();
                    }
                } else {
                    log("invalid slot id: " + slotId);
                }
            } else if (ACTION_SIM_STATE_CHANGED.equals(action)) {
                String iccState =  intent.getStringExtra(INTENT_KEY_ICC_STATE);
                final int slotId = intent.getIntExtra(SubscriptionManager.EXTRA_SLOT_INDEX,
                        SubscriptionManager.INVALID_SIM_SLOT_INDEX);

                log("SIM_STATE_CHANGED: iccState= " + iccState + " slotId= " + slotId);

                if (INTENT_VALUE_ICC_NOT_READY.equals(iccState) && isSubDeactivated(slotId)) {
                    mPowerUpOptHandler.sendMessageDelayed(mPowerUpOptHandler.
                            obtainMessage(EVENT_SIM_DISABLED, slotId), TIMEOUT_MILLIS);
                }
            } else if (ACTION_ESSENTIAL_RECORDS_LOADED.equals(action)) {
                final int slotId = intent.getIntExtra(CarrierConfigManager.EXTRA_SLOT_INDEX,
                        SubscriptionManager.INVALID_SIM_SLOT_INDEX);
                mPowerUpOptHandler.obtainMessage(EVENT_SIM_STATE_LOADED,
                        slotId).sendToTarget();
            } else {
                log("received unknown intent: " + action);
            }
        }
    };

    public void onMultiSimConfigChanged() {
        log("onMultiSimConfigChanged");
        mPowerUpOptHandler.sendMessage(mPowerUpOptHandler.
                obtainMessage(EVENT_MULTI_SIM_CONFIG_CHANGED));
    }

    private boolean isValidSlotIndex(int slotId) {
        return slotId >= 0 && slotId < mNumPhones;
    }

    private boolean isSimLocked(int simState) {
        return simState == TelephonyManager.SIM_STATE_PIN_REQUIRED
                || simState == TelephonyManager.SIM_STATE_PUK_REQUIRED
                || simState == TelephonyManager.SIM_STATE_NETWORK_LOCKED
                || simState == TelephonyManager.SIM_STATE_PERM_DISABLED;
    }

    private final InternalOemHookCallback mOemHookCallback = new InternalOemHookCallback() {
        @Override
        public String getCallBackName() {
            return "PowerUpOptService";
        }

        @Override
        public void onOemHookConnected() {
            log("QcRilHook Service ready");
            mIsOemHookConnected = true;
            trySendPhoneReadyForAllSlots();
        }

        @Override
        public void onOemHookDisconnected() {
            log("QcRilHook Service disconnected");
            mIsOemHookConnected = false;
        }
    };

    private void handleRadioPowerStateChanged(int slotId, int radioState) {
        if (isValidSlotIndex(slotId)) {
            if (radioState == TelephonyManager.RADIO_POWER_UNAVAILABLE) {
                log("radio is unavailable for slot: " + slotId);
                mIsRilConnectedForSlot[slotId] = false;
                mIsAtelReadySentForSlot[slotId] = false;
                if (mIsImsSupported) {
                    mIsImsStackUpForSlot[slotId] = false;
                }
            } else {
                log("radio is available for slot: " + slotId);
                mIsRilConnectedForSlot[slotId] = true;
                trySendPhoneReadyForSlot(slotId);
            }
        }
    }

    private void onSimLoadedOrLocked(int slotId) {
        log("SIM is loaded or locked on slot: " + slotId);
        if (isValidSlotIndex(slotId)) {
            SubscriptionInfo subInfo =
                    mSubscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(slotId);
            if (subInfo != null) {
                log("subInfo: " + subInfo);
                int subId = subInfo.getSubscriptionId();
                if (mAvailableSubs.contains(subId) && mIsAtelReadySentForSlot[slotId]) {
                    log("This sub was handled");
                    return;
                }
                if (!mAvailableSubs.contains(subId)) {
                    mAvailableSubs.add(subId);
                }
                if (mIsImsSupported && !mIsImsStackUpForSlot[slotId] &&
                        !mIsGetImsStateInProgress[slotId]) {
                    // fetch IMS state for this subscription
                    mImsMmTelManagers.put(subId, ImsMmTelManager.createForSubscriptionId(subId));
                    getImsState(subId, slotId, RETRY_COUNT_INITIALIZER);
                }
            } else {
                Log.d(TAG, "subInfo is null for slot: " + slotId);
            }
        }
    }

    private void onSimAbsent(int slotId) {
        log("SIM is absent on slot: " + slotId);
        if (isValidSlotIndex(slotId)) {
            mIsImsStackUpForSlot[slotId] = true;
            mIsRilConnectedForSlot[slotId] = true;
            trySendPhoneReadyForSlot(slotId);
        }
    }

    void checkImsState(int subId, int slotId, int retryCount) {
        if (isValidSlotIndex(slotId)) {
            try {
                final IntegerConsumer intResult = new IntegerConsumer();

                mImsMmTelManagers.get(subId).getFeatureState(mExecutor, intResult);

                if (intResult.get(TIMEOUT_MILLIS) != ImsFeature.STATE_READY) {
                    log("IMS state not ready, calling the method with 1000 ms timeout");
                    mIsImsStackUpForSlot[slotId] = false;
                    // Fetch IMS state after TIMEOUT_MILLIS
                    getImsState(subId, slotId, retryCount);
                } else {
                    log("IMS state ready for sub: " + subId);
                    mPowerUpOptHandler.sendMessage(mPowerUpOptHandler.obtainMessage(
                            EVENT_IMS_STACK_UP, slotId));
                }
            } catch (Exception ex) {
                Log.e(TAG,"Exception in checkImsState", ex);
            }
        }
    }

    private void getImsState(int subId, int slotId, int retryCount) {
        log("getImsState: slotId=" + slotId + "retryCount=" + retryCount);
        if (isValidSlotIndex(slotId)) {
            mIsGetImsStateInProgress[slotId] = true;
            if (retryCount > MAX_RETRY_COUNT) {
                log("Reach the max retry time: " + retryCount + " for slot: " + slotId);
                mPowerUpOptHandler.sendMessage(mPowerUpOptHandler.obtainMessage(
                        EVENT_IMS_STACK_UP, slotId));
                return;
            }
            final int interval = retryCount > 0 ? TIMEOUT_MILLIS : 0;
            ImsStackCheck checker = new ImsStackCheck();
            checker.subId = subId;
            checker.slotId = slotId;
            checker.retryCount = retryCount + 1;
            mPowerUpOptHandler.sendMessageDelayed(
                    mPowerUpOptHandler.obtainMessage(GET_IMS_STATE, checker), interval);
        }
    }

    private void onImsStackReadyForSlot(int slotId) {
        if (isValidSlotIndex(slotId)) {
            mIsGetImsStateInProgress[slotId] = false;
            mIsImsStackUpForSlot[slotId] = true;
            log("mIsImsStackUpForSlot: " + Arrays.toString(mIsImsStackUpForSlot));

            // try sending phone state ready for the given slotId
            trySendPhoneReadyForSlot(slotId);
        }
    }

    public void handleRadioDied(int slotId) {
        log("handleRadioDied()...");
        mPowerUpOptHandler.sendMessage(mPowerUpOptHandler.obtainMessage(EVENT_RIL_CRASH, slotId));

    }

    private void clearAtelReadySent() {
        Log.d(TAG, "clearAtelReadySent()...");
        for (int slotId = 0; slotId < mNumPhones; slotId++) {
            mIsAtelReadySentForSlot[slotId] = false;
        }
    }

    private void trySendPhoneReadyForAllSlots() {
        for (int slot = 0; slot < mNumPhones; slot++) {
            trySendPhoneReadyForSlot(slot);
        }
    }

    private void trySendPhoneReadyForSlot(int slotId) {
        log("trySendPhoneReady for slot: " + slotId);

        synchronized (mIsAtelReadySentLock) {
            /*
             To send ready to RIL, we need to wait for three events:
             1. RIL is connected
             2. OEM HOOK is ready
             3. IMS stack is ready (if IMS is supported)
             */

            if (mIsOemHookConnected
                    && mIsRilConnectedForSlot[slotId]
                    && mIsImsStackUpForSlot[slotId]
                    && !mIsAtelReadySentForSlot[slotId]) {

                mIsAtelReadySentForSlot[slotId] = true;
                mIsBootup = false;

                Thread powerUpOptServiceThread = new Thread(() -> {
                    log("Sending ATEL Ready to RIL for slot: " + slotId);
                    mQtiMsgTunnelClient.sendAtelReadyStatus(READY, slotId);

                });
                powerUpOptServiceThread.start();
            } else {
                log("Not sending ATEL ready: " + dumpStates(slotId));
            }
        }
    }

    private String dumpStates(int slot) {
        return "States: [" + slot +
                ": {" + mIsOemHookConnected +
                ", " + mIsRilConnectedForSlot[slot] +
                ", " + mIsImsStackUpForSlot[slot] +
                ", " + mIsAtelReadySentForSlot[slot] +
                "}]";
    }

    public void cleanUp() {
        Log.e(TAG, "cleanUp");
        try {
            if (mQtiMsgTunnelClient != null) {
                mQtiMsgTunnelClient.unregisterOemHookCallback(mOemHookCallback);
            }
            if (mIsIntentRegistered) {
                mContext.unregisterReceiver(mBroadcastReceiver);
                mIsIntentRegistered = false;
            }
            mPowerUpOptHandler.getLooper().quit();
        } catch (Exception ex) {
            Log.e(TAG, "cleanUp exception", ex);
        }
    }

    private void registerForIntents() {
        if (!mIsIntentRegistered) {
            Log.d(TAG, "Registering for Intents");

            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ACTION_RADIO_POWER_STATE_CHANGED);
            intentFilter.addAction(ACTION_SIM_STATE_CHANGED);
            intentFilter.addAction(TelephonyManager.ACTION_SIM_CARD_STATE_CHANGED);
            intentFilter.addAction(TelephonyManager.ACTION_SIM_APPLICATION_STATE_CHANGED);
            intentFilter.addAction(ACTION_ESSENTIAL_RECORDS_LOADED);
            mContext.registerReceiver(mBroadcastReceiver, intentFilter, Context.RECEIVER_EXPORTED);
            mIsIntentRegistered = true;
        }
    }

    private boolean isSubDeactivated(int slotId) {
        List<SubscriptionInfo>  availableSubInfos =
                mSubscriptionManager.getAvailableSubscriptionInfoList();
        UiccSlotInfo[] slotsInfo = mTelephonyManager.getUiccSlotsInfo();

        if (slotsInfo == null || availableSubInfos == null) return false;

        UiccSlotInfo slotInfo = slotsInfo[slotId];
        for (SubscriptionInfo info : availableSubInfos) {
             if (info == null || slotInfo == null) continue;
            if (TextUtils.equals(stripTrailingFs(info.getCardString()),
                    stripTrailingFs(slotInfo.getCardId()))) {
                return !info.areUiccApplicationsEnabled();
            }
        }
        return false;
    }

    private String stripTrailingFs(String s) {
        return s == null ? null : s.replaceAll("(?i)f*$", "");
    }

    private void handleSendPhoneReadyOnSimDisabled(int slotId) {
        log("handleSendPhoneReadyOnSimDisabled:  slotId= " + slotId);
        SubscriptionInfo subInfo =
                mSubscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(slotId);
        if (subInfo == null) onSimAbsent(slotId);
    }

    private void handleSendPhoneReadyOnRildCrash(int slotId) {
        log("handleSendPhoneReadyOnRildCrash:  slotId= " + slotId);
        if (isValidSlotIndex(slotId)) {
            mIsBootup = false;
            clearAtelReadySent();
            mIsRilConnectedForSlot[slotId] = false;

            if (mIsImsSupported) {
                mIsImsStackUpForSlot[slotId] = false;
            }

            trySendPhoneReadyForAllSlots();
        } else {
            log("invalid slot id: " + slotId);
        }
    }

    private void handleMultiSimConfigChanged() {
        int activeModemCount = mTelephonyManager.getActiveModemCount();
        log("handleMultiSimConfigChanged activeModemCount = " + activeModemCount);
        if (activeModemCount == mNumPhones) {
            return;
        }

        mNumPhones = activeModemCount;
        mIsRilConnectedForSlot = copyOf(mIsRilConnectedForSlot, mNumPhones);
        mIsImsStackUpForSlot = copyOf(mIsImsStackUpForSlot, mNumPhones);
        mIsAtelReadySentForSlot = copyOf(mIsAtelReadySentForSlot, mNumPhones);
        mIsGetImsStateInProgress = copyOf(mIsGetImsStateInProgress, mNumPhones);

        if (!mIsImsSupported) {
            onImsNotSupported();
        }
    }

    public class PowerUpOptHandler extends Handler {

        PowerUpOptHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            log("handleMessage what = " + msg.what);
            switch (msg.what) {
                case EVENT_SIM_DISABLED: {
                    handleSendPhoneReadyOnSimDisabled((int) msg.obj);
                    break;
                }

                case GET_IMS_STATE: {
                    ImsStackCheck checker = (ImsStackCheck) msg.obj;
                    log("GET_IMS_STATE, slot " + checker.slotId + ", sub: " + checker.subId
                                + ", retry: " + checker.retryCount);
                    checkImsState(checker.subId, checker.slotId, checker.retryCount);
                    break;
                }

                 case EVENT_IMS_STACK_UP: {
                    onImsStackReadyForSlot((int) msg.obj);
                    break;
                 }

                 case EVENT_RIL_CRASH: {
                    handleSendPhoneReadyOnRildCrash((int) msg.obj);
                    break;
                 }

                case EVENT_MULTI_SIM_CONFIG_CHANGED: {
                    handleMultiSimConfigChanged();
                    break;
                }

                case EVENT_RADIO_POWER_STATE_CHANGED: {
                    handleRadioPowerStateChanged(msg.arg1, msg.arg2);
                    break;
                }

                case EVENT_SIM_STATE_ABSENT: {
                    onSimAbsent((int) msg.obj);
                    break;
                }

                case EVENT_SIM_STATE_LOADED: {
                    onSimLoadedOrLocked((int) msg.obj);
                    break;
                }

                case EVENT_IMS_NOT_SUPPORTED: {
                    onImsNotSupported();
                    break;
                }

                default: {
                    log("invalid message " + msg.what);
                    break;
                }
            }
        }
    }

    private void log(String msg) {
        Log.d(TAG, msg);
    }

    private static class ImsStackCheck {
        int subId;
        int slotId;
        int retryCount;
    }
}
