/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qti.phone;

import static com.qti.extphone.ExtPhoneCallbackListener.EVENT_SET_CELLULAR_ROAMING_PREFERENCE_RESPONSE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.provider.Settings;

import com.qti.extphone.CellularRoamingPreference;
import com.qti.extphone.Client;
import com.qti.extphone.ExtPhoneCallbackListener;
import com.qti.extphone.ExtTelephonyManager;
import com.qti.extphone.ServiceCallback;
import com.qti.extphone.Status;
import com.qti.extphone.Token;

public class QtiCellularRoamingPreferenceController extends Handler {
    private final static String TAG = QtiCellularRoamingPreferenceController.class.getSimpleName();

    private static final String SYNC_STATE_CACHE_KEY = "CELLULAR_ROAMING_SYNC_KEY";
    private static final String TELEPHONY_STATE_CACHE_KEY = "CELLULAR_ROAMING_STATE_KEY";
    private static final String CELLULAR_ROAMING_SUPPORT_CACHE_KEY = "CELLULAR_ROAMING_SUPPORT_KEY";

    private static final int RETRY_COUNT = 3;
    private static final int RETRY_DELAY_MS = 2000;

    private static final int EVENT_MODEM_RESET = 1;

    private static final int INVALID = CellularRoamingPreference.INVALID;
    private static final int DISABLED = CellularRoamingPreference.DISABLED;
    private static final int ENABLED = CellularRoamingPreference.ENABLED;
    private static final int TRUE = ENABLED;
    private static final int FALSE = DISABLED;

    private static final String EXTRA_STATE = "state";
    private static final String ACTION_RADIO_POWER_STATE_CHANGED =
            "org.codeaurora.intent.action.RADIO_POWER_STATE";

    private final Context mContext;
    private final QtiRadioProxy mQtiRadioProxy;
    private int mActiveModemCount;
    private int mNumProcessedSlots = 0;
    private int[] mRetryCount = {0, 0};
    private boolean[] mRadioAvailable = {false, false};
    private String mPackageName;
    private Client mClient;
    private ExtTelephonyManager mExtTelephonyManager;
    private QtiCellularRoamingCallback mQtiCellularRoamingCallback;
    private CellularRoamingPreference mCellularRoamingPref;

    private ServiceCallback mExtTelManagerServiceCallback = new ServiceCallback() {
        @Override
        public void onConnected() {
            logd("ExtTelephonyService connected");
            int[] events = new int[] {EVENT_SET_CELLULAR_ROAMING_PREFERENCE_RESPONSE};
            mClient = mExtTelephonyManager.registerCallbackWithEvents(mPackageName,
                    new ExtPhoneCallbackListener(), events);
            logd("Client = " + mClient);
            boolean cellularRoamingSupported = mExtTelephonyManager.isFeatureSupported(
                    ExtTelephonyManager.FEATURE_CELLULAR_ROAMING);
            Settings.Global.putInt(mContext.getContentResolver(),
                    CELLULAR_ROAMING_SUPPORT_CACHE_KEY, cellularRoamingSupported ? TRUE : FALSE);
            if (cellularRoamingSupported) {
                registerReceivers();
                getModemPreference();
            } else {
                logd("Cellular roaming not supported");
            }
        }

        @Override
        public void onDisconnected() {
            logd("ExtTelephonyService disconnected");
            mClient = null;
        }
    };

    public QtiCellularRoamingPreferenceController(Context context, Looper looper,
            QtiRadioProxy radioProxy) {
        super(looper);
        mContext = context;
        mQtiRadioProxy = radioProxy;
        mPackageName = this.getClass().getPackage().getName();
        mQtiCellularRoamingCallback = new QtiCellularRoamingCallback();
        mQtiRadioProxy.registerInternalCallback(mQtiCellularRoamingCallback);
        TelephonyManager tm = mContext.getSystemService(TelephonyManager.class);
        if (tm != null) {
            mActiveModemCount = tm.getActiveModemCount();
        }
        mExtTelephonyManager = ExtTelephonyManager.getInstance(context);
        mExtTelephonyManager.connectService(mExtTelManagerServiceCallback);
    }

    private void registerReceivers() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_RADIO_POWER_STATE_CHANGED);
        mContext.registerReceiver(mBroadcastReceiver, intentFilter, Context.RECEIVER_EXPORTED);
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (ACTION_RADIO_POWER_STATE_CHANGED.equals(intent.getAction())) {
                int slotIdExtra = intent.getIntExtra(SubscriptionManager.EXTRA_SLOT_INDEX,
                        SubscriptionManager.INVALID_SIM_SLOT_INDEX);
                int radioStateExtra = intent.getIntExtra(EXTRA_STATE,
                        TelephonyManager.RADIO_POWER_UNAVAILABLE);
                obtainMessage(EVENT_MODEM_RESET, slotIdExtra, radioStateExtra).sendToTarget();
            }
        }
    };

    private final class QtiCellularRoamingCallback extends QtiRadioProxy.IQtiRadioInternalCallback {
        @Override
        public void setCellularRoamingPreferenceResponse(int slotId, Token token, Status status) {
            obtainMessage(EVENT_SET_CELLULAR_ROAMING_PREFERENCE_RESPONSE, slotId, -1,
                    new QtiRadioProxy.Result(token, status, null)).sendToTarget();
        }
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case EVENT_SET_CELLULAR_ROAMING_PREFERENCE_RESPONSE:
                logd("EVENT_SET_CELLULAR_ROAMING_PREFERENCE_RESPONSE");
                int status = ((QtiRadioProxy.Result) msg.obj).mStatus.get();
                handleSetPreferenceResponse(msg.arg1, status);
                break;
            case EVENT_MODEM_RESET:
                handleRadioPowerStateChanged(msg.arg1, msg.arg2);
                break;
            default:
                logd("Unexpected event: " + msg.what);
        }
    }

    private void handleSetPreferenceResponse(int slotId, int status) {
        if (mRetryCount[slotId] < RETRY_COUNT) {
            if (status == Status.SUCCESS) {
                writeToCache(SYNC_STATE_CACHE_KEY, slotId, TRUE);
                cleanUp(slotId, status);
            } else {
                mRetryCount[slotId]++;
                postDelayed(() -> comparePreferences(slotId), RETRY_DELAY_MS);
            }
        } else {
            writeToCache(SYNC_STATE_CACHE_KEY, slotId, FALSE);
            cleanUp(slotId, status);
        }
    }

    private void handleRadioPowerStateChanged(int slotId, int radioState) {
        if (radioState == TelephonyManager.RADIO_POWER_ON) {
            logd("Radio is available for slot " + slotId);
            mRadioAvailable[slotId] = true;
            getModemPreference(slotId);
        } else {
            mRadioAvailable[slotId] = false;
        }
    }

    private void getModemPreference(int... slotIdFromRadioAvailable) {
        for (int slot = 0; slot < mActiveModemCount; slot++) {
            mCellularRoamingPref = null;
            if (slotIdFromRadioAvailable.length != 0 && slotIdFromRadioAvailable[0] != slot) {
                continue;
            }
            try {
                mCellularRoamingPref = mExtTelephonyManager.getCellularRoamingPreference(slot);
            } catch (Exception e) {
                loge("getModemPreference: exception " + e);
            }
            if (mCellularRoamingPref != null) {
                comparePreferences(slot);
            } else {
                logd("getModemPreference: pref null");
            }
        }
    }

    private void comparePreferences(int slotId) {
        // Don't do any comparisons if radio is not available
        if (!mRadioAvailable[slotId]) {
            logd("comparePreferences: radio unavailable. bail out!");
            return;
        }
        final int modemState = mCellularRoamingPref != null ?
                mCellularRoamingPref.getInternationalCellularRoamingPref() : INVALID;
        final int cachedState = (Integer) readFromCache(TELEPHONY_STATE_CACHE_KEY, slotId);
        logd("slot " + slotId + " - cached state " + cachedState + ", modem state " + modemState);
        if (modemState == INVALID) {
            logd("comparePreferences: invalid modem state");
            return;
        }
        if (cachedState != modemState) {
            try {
                // Passing INVALID for the domestic cellular roaming pref so as to not modify it
                // on the modem side
                mQtiRadioProxy.setCellularRoamingPreference(mClient, slotId,
                        new CellularRoamingPreference(cachedState, INVALID));
            } catch (Exception e) {
                loge("comparePreferences: exception " + e);
            }
        } else {
            writeToCache(SYNC_STATE_CACHE_KEY, slotId, TRUE);
            cleanUp(slotId, Status.SUCCESS);
        }
    }

    private void writeToCache(String id, int slotId, Object obj) {
        switch (id) {
            case SYNC_STATE_CACHE_KEY:
                Settings.Global.putInt(mContext.getContentResolver(), SYNC_STATE_CACHE_KEY + slotId,
                        (Integer) obj);
                break;
            default:
                loge("writeToCache: unsupported id");
        }
    }

    private Object readFromCache(String id, int slotId) {
        switch (id) {
            case TELEPHONY_STATE_CACHE_KEY:
                return Settings.Global.getInt(mContext.getContentResolver(),
                        TELEPHONY_STATE_CACHE_KEY + slotId, ENABLED);
            default:
                loge("readFromCache: unsupported id");
                return null;
        }
    }

    private void cleanUp(int slotId, int status) {
        // Perform a cleanup when either of these conditions apply:
        // 1) The cached and the modem states already match for both slots
        // 2) The maximum number of retries has been reached for both slots
        mNumProcessedSlots++;
        if (mNumProcessedSlots == mActiveModemCount &&
                (status == Status.SUCCESS || mRetryCount[slotId] >= RETRY_COUNT)) {
            mQtiRadioProxy.unRegisterInternalCallback(mQtiCellularRoamingCallback);
        }
    }

    private static void logd(String msg) {
        Log.d(TAG, msg);
    }

    private static void loge(String msg) {
        Log.e(TAG, msg);
    }
}
