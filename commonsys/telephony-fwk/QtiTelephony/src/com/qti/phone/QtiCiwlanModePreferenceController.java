/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qti.phone;

import static com.qti.extphone.ExtPhoneCallbackListener.EVENT_SET_CIWLAN_MODE_USER_PREFERENCE_RESPONSE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.qti.extphone.CiwlanConfig;
import com.qti.extphone.Client;
import com.qti.extphone.ExtPhoneCallbackListener;
import com.qti.extphone.ExtTelephonyManager;
import com.qti.extphone.ServiceCallback;
import com.qti.extphone.Status;
import com.qti.extphone.Token;

/*
 * This class makes sure the user selected C_IWLAN mode preferences are enforced on boot if they
 * differ from what's on the modem side.
 */
public class QtiCiwlanModePreferenceController extends Handler {
    private final static String TAG = QtiCiwlanModePreferenceController.class.getSimpleName();

    private static final String SYNC_STATE_CACHE_KEY = "CELLULAR_I_WLAN_SYNC_KEY";
    private static final String TELEPHONY_STATE_CACHE_KEY = "CELLULAR_I_WLAN_PREFERENCE_STATE_KEY";
    private static final String CIWLAN_MODE_PREF_SUPPORT_CACHE_KEY = "CIWLAN_MODE_PREF_SUPPORT_KEY";

    private static final int RETRY_COUNT = 3;
    private static final int RETRY_DELAY_MS = 2000;

    private static final int EVENT_MODEM_RESET = 1;

    private static final int INVALID = CiwlanConfig.INVALID;
    private static final int ONLY = CiwlanConfig.ONLY;
    private static final int PREFERRED = CiwlanConfig.PREFERRED;
    private static final String DEFAULT_PREFS = PREFERRED + "," + ONLY;
    private static final int TRUE = 1;
    private static final int FALSE = 0;

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
    private QtiCiwlanModePrefCallback mQtiCiwlanModePrefCallback;
    private CiwlanConfig mModemCiwlanModePref;

    private ServiceCallback mExtTelManagerServiceCallback = new ServiceCallback() {
        @Override
        public void onConnected() {
            logd("ExtTelephonyService connected");
            int[] events = new int[] {EVENT_SET_CIWLAN_MODE_USER_PREFERENCE_RESPONSE};
            mClient = mExtTelephonyManager.registerCallbackWithEvents(mPackageName,
                    new ExtPhoneCallbackListener(), events);
            logd("Client = " + mClient);
            boolean ciwlanModePrefSupported = mExtTelephonyManager.isFeatureSupported(
                    ExtTelephonyManager.FEATURE_CIWLAN_MODE_PREFERENCE);
            Settings.Global.putInt(mContext.getContentResolver(),
                    CIWLAN_MODE_PREF_SUPPORT_CACHE_KEY, ciwlanModePrefSupported ? TRUE : FALSE);
            if (ciwlanModePrefSupported) {
                registerReceivers();
                getModemPreference();
            } else {
                logd("C_IWLAN mode preference not supported");
            }
        }

        @Override
        public void onDisconnected() {
            logd("ExtTelephonyService disconnected");
            mClient = null;
        }
    };

    public QtiCiwlanModePreferenceController(Context context, Looper looper,
            QtiRadioProxy radioProxy) {
        super(looper);
        mContext = context;
        mQtiRadioProxy = radioProxy;
        mPackageName = this.getClass().getPackage().getName();
        mQtiCiwlanModePrefCallback = new QtiCiwlanModePrefCallback();
        mQtiRadioProxy.registerInternalCallback(mQtiCiwlanModePrefCallback);
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

    private final class QtiCiwlanModePrefCallback extends QtiRadioProxy.IQtiRadioInternalCallback {
        @Override
        public void setCiwlanModeUserPreferenceResponse(int slotId, Token token, Status status) {
            obtainMessage(EVENT_SET_CIWLAN_MODE_USER_PREFERENCE_RESPONSE, slotId, -1,
                    new QtiRadioProxy.Result(token, status, null)).sendToTarget();
        }
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case EVENT_SET_CIWLAN_MODE_USER_PREFERENCE_RESPONSE:
                logd("EVENT_SET_CIWLAN_MODE_USER_PREFERENCE_RESPONSE");
                int status = ((QtiRadioProxy.Result) msg.obj).mStatus.get();
                handleSetModePreferenceResponse(msg.arg1, status);
                break;
            case EVENT_MODEM_RESET:
                handleRadioPowerStateChanged(msg.arg1, msg.arg2);
                break;
            default:
                logd("Unexpected event: " + msg.what);
        }
    }

    private void handleSetModePreferenceResponse(int slotId, int status) {
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
            mModemCiwlanModePref = null;
            if (slotIdFromRadioAvailable.length != 0 && slotIdFromRadioAvailable[0] != slot) {
                continue;
            }
            try {
                mModemCiwlanModePref = mExtTelephonyManager.getCiwlanModeUserPreference(slot);
            } catch (Exception e) {
                loge("getModemPreference: exception " + e);
            }
            if (mModemCiwlanModePref != null) {
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
        final String modemPrefs = mModemCiwlanModePref != null ?
                mModemCiwlanModePref.getCiwlanHomeMode() + "," +
                mModemCiwlanModePref.getCiwlanRoamMode() : INVALID + "," + INVALID;
        final String cachedPrefs = (String) readFromCache(TELEPHONY_STATE_CACHE_KEY, slotId);
        logd("slot " + slotId + " - cached prefs " + cachedPrefs + ", modem prefs " + modemPrefs);
        if (modemPrefs.contains(String.valueOf(INVALID))) {
            logd("comparePreferences: invalid modem pref");
            return;
        }
        if (cachedPrefs != modemPrefs) {
            try {
                int homePref = Integer.valueOf(cachedPrefs.split(",")[0]);
                int roamPref = Integer.valueOf(cachedPrefs.split(",")[1]);
                CiwlanConfig config = new CiwlanConfig(homePref, roamPref);
                mExtTelephonyManager.setCiwlanModeUserPreference(slotId, mClient, config);
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
                Settings.Global.putInt(mContext.getContentResolver(), id + slotId, (Integer) obj);
                break;
            default:
                loge("writeToCache: unsupported id");
        }
    }

    private Object readFromCache(String id, int slotId) {
        switch (id) {
            case TELEPHONY_STATE_CACHE_KEY:
                String cache = Settings.Global.getString(mContext.getContentResolver(),
                        id + slotId);
                cache = (cache == null) ? DEFAULT_PREFS : cache;
                return cache;
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
            mQtiRadioProxy.unRegisterInternalCallback(mQtiCiwlanModePrefCallback);
        }
    }

    private static void logd(String msg) {
        Log.d(TAG, msg);
    }

    private static void loge(String msg) {
        Log.e(TAG, msg);
    }
}
