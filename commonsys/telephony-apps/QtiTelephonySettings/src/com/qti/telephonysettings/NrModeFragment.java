/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qti.telephonysettings;

import android.content.Context;
import android.content.Intent;
import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.ArrayMap;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.ListPreference;

import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;

import com.qti.extphone.Client;
import com.qti.extphone.ExtTelephonyManager;
import com.qti.extphone.ExtPhoneCallbackListener;
import com.qti.extphone.NrConfig;
import com.qti.extphone.Status;
import com.qti.extphone.ServiceCallback;
import com.qti.extphone.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NrModeFragment extends PreferenceFragmentCompat implements
        ListPreference.OnPreferenceChangeListener,
        SubscriptionsChangeListener.SubscriptionsChangeListenerClient {

    private static final String TAG = "NrModeFragment";

    private static final int NR_MODE_NSA_SA = 0;
    private static final int NR_MODE_NSA = 1;
    private static final int NR_MODE_SA = 2;
    private static final int EVENT_SET_NR_CONFIG_STATUS = 101;
    private static final int EVENT_GET_NR_CONFIG_STATUS = 102;

    private Client mClient;
    private Context mContext;
    private String mPackageName;
    private SubscriptionsChangeListener mChangeListener;
    private PreferenceScreen mPreferenceScreen;
    private Map<Integer, ListPreference> mPreferences;
    private Map<Integer, TelephonyCallbackCallStateListener> mCallStateListeners;
    private TelephonyManager mTelephonyManager;
    private SharedPreferences mSharedPreferences;
    private SubscriptionManager mSubscriptionManager;

    private List<ListPreference> mPreferenceList;

    private int mUserPrefNrConfig;
    private ExtTelephonyManager mExtTelephonyManager;
    private boolean mServiceConnected;
    private boolean mRetry;
    private boolean mAirplaneModeEnabled;

    private ExtPhoneCallbackListener mExtPhoneCallbackListener = new ExtPhoneCallbackListener() {
        @Override
        public void onSetNrConfig(int slotId, Token token, Status status) throws
                RemoteException {
            Log.d(TAG, "onSetNrConfig: slotId = " + slotId + " token = " + token + " status = " +
                    status);
            if (status.get() == Status.SUCCESS) {
                updateSharedPreference(slotId, mUserPrefNrConfig);
            }
            mMainThreadHandler.sendMessage(mMainThreadHandler
                    .obtainMessage(EVENT_SET_NR_CONFIG_STATUS, slotId, -1));
        }

        @Override
        public void onNrConfigStatus(int slotId, Token token, Status status, NrConfig nrConfig)
                throws RemoteException {
            Log.d(TAG, "onNrConfigStatus: slotId = " + slotId + " token = " + token + " status = " +
                    status + " nrConfig = " + nrConfig);
            if (status.get() == Status.SUCCESS) {
                updateSharedPreference(slotId, nrConfig.get());
                mMainThreadHandler.sendMessage(mMainThreadHandler
                        .obtainMessage(EVENT_GET_NR_CONFIG_STATUS, slotId, -1));
            }
        }
    };

    private Handler mMainThreadHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "handleMessage msg.what = " + msg.what);
            switch (msg.what) {
                case EVENT_SET_NR_CONFIG_STATUS:
                case EVENT_GET_NR_CONFIG_STATUS: {
                    int slotId = msg.arg1;
                    if (mPreferenceScreen != null) {
                        updatePreferenceForSlot(slotId);
                    }
                    break;
                }
            }
        }
    };

    private void updateSharedPreference(int slotId, int nrConfig) {
        mSharedPreferences.edit().putInt("nr_mode_" + slotId, nrConfig).apply();
    }

    private ServiceCallback mServiceCallback = new ServiceCallback() {
        @Override
        public void onConnected() {
            Log.d(TAG, "ExtTelephonyService connected");
            mServiceConnected = true;
            int[] events = new int[] {};
            mClient = mExtTelephonyManager.registerCallbackWithEvents(
                    mPackageName, mExtPhoneCallbackListener, events);
            Log.d(TAG, "Client = " + mClient);
            if (mRetry) {
                mRetry = false;
                for (int slotId = 0; slotId < mTelephonyManager.getActiveModemCount();
                        slotId++) {
                    Token token = mExtTelephonyManager.queryNrConfig(slotId, mClient);
                    Log.d(TAG, "queryNrConfig: " + token);
                }
            }
        }

        @Override
        public void onDisconnected() {
            Log.d(TAG, "ExtTelephonyService disconnected...");
            if (mServiceConnected) {
                mExtTelephonyManager.unregisterCallback(mExtPhoneCallbackListener);
                mServiceConnected = false;
                mClient = null;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        mContext = getActivity();
        mSharedPreferences = mContext.getSharedPreferences(mContext.getPackageName(),
                mContext.MODE_PRIVATE);
        mSubscriptionManager = mContext.getSystemService(SubscriptionManager.class);
        if (mSubscriptionManager != null) {
            mSubscriptionManager = mSubscriptionManager.createForAllUserProfiles();
        }
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class);
        mPreferences = new ArrayMap<>();
        mCallStateListeners = new ArrayMap<>();
        mChangeListener =
                new SubscriptionsChangeListener(mContext.getApplicationContext(), this);
        mExtTelephonyManager = ExtTelephonyManager.getInstance(mContext.getApplicationContext());

        Log.d(TAG, "Connect to ExtTelephony bound service...");
        mExtTelephonyManager.connectService(mServiceCallback);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        mPreferenceScreen = getPreferenceScreen();
        if (mPreferenceScreen != null) {
            mPreferenceScreen.removeAll();
        }

        setPreferencesFromResource(R.xml.nrmode_preference, rootKey);
        mPreferenceScreen = getPreferenceScreen();
    }

    @Override
    public void onResume() {
        super.onResume();

        mChangeListener.start();

        String title = mContext.getResources().getString(R.string.preferred_5G_nr_mode_title);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        ActionBar actionBar = activity != null ? activity.getSupportActionBar() : null;
        if (actionBar != null) actionBar.setTitle(title);
        update();

        Log.d(TAG, "onResume()");
    }

    @Override
    public void onPause() {
        super.onPause();
        mChangeListener.stop();
        Log.d(TAG, "onPause()");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mExtTelephonyManager.unregisterCallback(mExtPhoneCallbackListener);
        mExtTelephonyManager.disconnectService();
        unregisterTelephonyCallbackCallStateListener();
        Log.d(TAG, "onDestroy()");
    }

    private void unregisterTelephonyCallbackCallStateListener() {
        if (mCallStateListeners != null) {

            for (var entry : mCallStateListeners.entrySet()) {
                entry.getValue().unregister();
            }
            mCallStateListeners.clear();
        }
    }

    private void update() {
        if (mPreferenceScreen == null) {
            return;
        }

        Log.d(TAG, "update()");

        // Since we may already have created some preferences previously, we first grab the list of
        // those, then go through the current available subscriptions making sure they are all
        // present in the screen, and finally remove any now-outdated ones.
        final Map<Integer, ListPreference> existingPreferences = mPreferences;
        mPreferences = new ArrayMap<>();
        mPreferenceList = new ArrayList<>();

        unregisterTelephonyCallbackCallStateListener();
        for (int slotId = 0; slotId < mTelephonyManager.getActiveModemCount(); slotId++) {
            SubscriptionInfo info = mSubscriptionManager.
                    getActiveSubscriptionInfoForSimSlotIndex(slotId);

            if (info != null) {
                final int subId = info.getSubscriptionId();
                ListPreference pref = existingPreferences.remove(subId);
                if (pref == null) {
                    pref = new ListPreference(mPreferenceScreen.getContext());
                    mPreferenceScreen.addPreference(pref);
                }

                pref.setKey("key_" +slotId);
                pref.setIconSpaceReserved(false);
                pref.setTitle(info.getDisplayName());
                pref.setOrder(slotId);
                pref.setOnPreferenceClickListener(null);
                pref.setDialogTitle("Select NR Mode For Slot " + slotId);

                pref.setOnPreferenceChangeListener(this);
                pref.setEntries(R.array.preferred_5g_network_mode_choices);
                pref.setEntryValues(R.array.preferred_5g_network_mode_values);

                if (mServiceConnected && mClient != null) {
                    mRetry = false;
                    Token token = mExtTelephonyManager.queryNrConfig(slotId, mClient);
                    Log.d(TAG, "queryNrConfig: " + token);
                } else {
                    mRetry = true;
                }

                mPreferences.put(subId, pref);
                mPreferenceList.add(pref);

                Log.d(TAG, "Adding CallStateListener for subId... " + subId);
                mCallStateListeners.put(subId, new TelephonyCallbackCallStateListener(subId));
                mCallStateListeners.get(subId).register(mContext);
            } else {
                Log.d(TAG, "sub info is null, add null preference for slot: " + slotId);
                mPreferenceList.add(slotId, null);
            }
        }
        for (Preference pref : existingPreferences.values()) {
            mPreferenceScreen.removePreference(pref);
        }
    }

    private void updatePreferenceForSlot(int slotId) {
        int nrConfig = mSharedPreferences.getInt("nr_mode_" + slotId,
                NrConfig.NR_CONFIG_COMBINED_SA_NSA);
        String text = mContext.getString(getSummaryResId(nrConfig));

        Log.d(TAG, "updatePreferenceForSlot for " + slotId + " ,nr mode is " + nrConfig +
                " , set summary to " + text);

        ListPreference pref = mPreferenceList.get(slotId);
        if (pref != null) {
            pref.setSummary(text);
            pref.setValue(Integer.toString(nrConfig));
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object object) {
        final int newNrMode = Integer.parseInt((String) object);
        final int slotId = mPreferenceList.indexOf(preference);

        Log.i(TAG, "onPreferenceChange for slot: " + slotId + ", setNrConfig: " + newNrMode);

        if (mSubscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(slotId) == null
                || mAirplaneModeEnabled) {
            Log.d(TAG, "onPreferenceChange: airplane mode : " + mAirplaneModeEnabled);
            Toast.makeText(mContext, mAirplaneModeEnabled ? R.string.airplaneModeEnabled
                    : R.string.non_active_subscription, Toast.LENGTH_LONG).show();
            return true;
        }

        mUserPrefNrConfig = newNrMode;
        if (mServiceConnected && mClient != null) {
            Token token = mExtTelephonyManager.setNrConfig(
                    slotId, new NrConfig(newNrMode), mClient);
            Log.d(TAG, "setNrConfig: " + token);
        }
        final ListPreference listPreference = (ListPreference) preference;
        String summary = mContext.getString(getSummaryResId(newNrMode));
        listPreference.setSummary(summary);
        return true;
    }

    private int getSummaryResId(int nrMode) {
        if (nrMode == NrConfig.NR_CONFIG_COMBINED_SA_NSA) {
            return R.string.nr_nsa_sa;
        } else if (nrMode == NrConfig.NR_CONFIG_NSA) {
            return R.string.nr_nsa;
        } else if (nrMode == NrConfig.NR_CONFIG_SA) {
            return R.string.nr_sa;
        } else {
            return R.string.nr_nsa_sa;
        }
    }

    @Override
    public void onAirplaneModeChanged(boolean airplaneModeEnabled) {
        mAirplaneModeEnabled = airplaneModeEnabled;
        update();
    }

    @Override
    public void onSubscriptionsChanged() {
        Log.d(TAG, "onSubscriptionsChanged()");
        update();
    }

    private class TelephonyCallbackCallStateListener extends TelephonyCallback implements
            TelephonyCallback.CallStateListener {
        private int mSubId;
        private TelephonyManager mTelephonyManager;

        TelephonyCallbackCallStateListener(int subId) {
            super();
            Log.d(TAG, "TelephonyCallbackCallStateListener ... cons()");
            mSubId = subId;
        }

        @Override
        public void onCallStateChanged(int state) {
            // use this mSubId to disable
            if (mPreferences != null) {
                Log.d(TAG, "TelephonyCallbackCallStateListener ... onCallStateChanged : mSubId : "
                        +mSubId);
                Preference pref = mPreferences.get(mSubId);
                if (pref != null) {
                    pref.setEnabled(state == TelephonyManager.CALL_STATE_IDLE);
                }
            }
        }

        public void register(Context context) {
            Log.d(TAG, "TelephonyCallbackCallStateListener ... register");
            if (SubscriptionManager.isValidSubscriptionId(mSubId)) {
                mTelephonyManager = context.getSystemService(TelephonyManager.class).
                        createForSubscriptionId(mSubId);
                mTelephonyManager.registerTelephonyCallback(mContext.getMainExecutor(),
                    mCallStateListeners.get(mSubId));
            }
        }

        public void unregister() {
            Log.d(TAG, "TelephonyCallbackCallStateListener ... unregister");
            mTelephonyManager.unregisterTelephonyCallback(this);
        }
    }
}
