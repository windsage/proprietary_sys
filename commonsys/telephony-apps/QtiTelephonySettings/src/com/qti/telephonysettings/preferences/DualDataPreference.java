/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qti.telephonysettings.preferences;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Toast;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.qti.extphone.Client;
import com.qti.extphone.ExtPhoneCallbackListener;
import com.qti.extphone.ExtTelephonyManager;
import com.qti.extphone.ServiceCallback;
import com.qti.extphone.Status;
import com.qti.extphone.Token;
import com.qti.telephonysettings.R;
import com.qti.telephonysettings.SubscriptionsChangeListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DualDataPreference extends SwitchPreference implements
       Preference.OnPreferenceChangeListener,
       SubscriptionsChangeListener.SubscriptionsChangeListenerClient {

    private static final String TAG = "DualDataPreference";

    private static final String DUAL_DATA_PREFERENCE = "dual_data_preference";
    private static final Uri DUAL_DATA_PREFERENCE_URI =
            Settings.Global.getUriFor(DUAL_DATA_PREFERENCE);

    private static final int SETTING_VALUE_ON = 1;
    private static final int SETTING_VALUE_OFF = 0;

    private static final int EVENT_ON_DUAL_DATA_CAPABILITY_CHANGED = 1;
    private static final int EVENT_SET_DUAL_DATA_USER_PREFERENCE_RESPONSE = 2;

    private Client mClient;
    private Context mContext;
    private String mPackageName;
    private SubscriptionManager mSubscriptionManager;
    private TelephonyManager mTelephonyManager;
    private ExtTelephonyManager mExtTelephonyManager;
    private SubscriptionsChangeListener mChangeListener;
    private int mDefaultDataSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    private Map<Integer, TelephonyCallback> mSubIdTelephonyCallbackMap = new HashMap<>();
    private int mPhoneCount;
    private final ContentObserver mContentObserver;

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "mBroadcastReceiver: action= " + action);
            if (action.equals(TelephonyManager.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED)) {
                if (mDefaultDataSubId == getDefaultDataSubscriptionId()) {
                    Log.d(TAG, "DDS is not changed");
                    return;
                }

                for (int subId : mSubIdTelephonyCallbackMap.keySet()) {
                    TelephonyCallback oldCallback = mSubIdTelephonyCallbackMap.get(subId);
                    if (oldCallback != null) {
                        mTelephonyManager.unregisterTelephonyCallback(oldCallback);
                    }
                    mSubIdTelephonyCallbackMap.remove(subId);
                }

                mDefaultDataSubId = getDefaultDataSubscriptionId();
                listenForNonDdsCallStateChange();
            }
        }
    };

    @Override
    public void onAirplaneModeChanged(boolean airplaneModeEnabled) {
        Log.d(TAG, "onAirplaneModeChanged: No operation");
    }

    @Override
    public void onSubscriptionsChanged() {
        Log.d(TAG, "onSubscriptionsChanged()");
        handleOnSubscriptionsChanged();
    }

    private void handleOnSubscriptionsChanged() {
        int activeModemCount = mTelephonyManager.getActiveModemCount();
        if (mPhoneCount == activeModemCount) {
            Log.d(TAG, "no change in active SIM count");
            return;
        }
        mPhoneCount = activeModemCount;
        if (mPhoneCount > 1) {
            if (mExtTelephonyManager != null) {
                updateUiPreference(mExtTelephonyManager.getDualDataCapability(), false);
            }
        } else {
            updateUiPreference(false, true);
        }
    }

    private int getDefaultDataSubscriptionId() {
        return mSubscriptionManager.getDefaultDataSubscriptionId();
    }

    private void listenForNonDdsCallStateChange() {
        List<SubscriptionInfo> subInfos =
                mSubscriptionManager.getActiveSubscriptionInfoList();
        if (subInfos != null) {
            for (SubscriptionInfo subInfo : subInfos) {
                if (subInfo == null) continue;
                if (subInfo.getSubscriptionId() != mDefaultDataSubId) {
                    DualDataTelephonyCallback callback = new DualDataTelephonyCallback(
                            subInfo.getSubscriptionId());
                    mTelephonyManager = mTelephonyManager.
                            createForSubscriptionId(subInfo.getSubscriptionId());
                    mTelephonyManager.
                            registerTelephonyCallback(mContext.getMainExecutor(), callback);
                    mSubIdTelephonyCallbackMap.put(subInfo.getSubscriptionId(), callback);
                }
            }
        }
    }

    public DualDataPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.d(TAG, "Constructor");

        mContext = context.getApplicationContext();
        mPackageName = mContext.getPackageName();
        mSubscriptionManager = mContext.getSystemService(SubscriptionManager.class);
        if (mSubscriptionManager != null) {
            mSubscriptionManager = mSubscriptionManager.createForAllUserProfiles();
        }
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class);
        mPhoneCount = mTelephonyManager.getActiveModemCount();
        updateUiPreference(false, true);
        mDefaultDataSubId = getDefaultDataSubscriptionId();
        mExtTelephonyManager = ExtTelephonyManager.getInstance(mContext);
        mExtTelephonyManager.connectService(mExtTelManagerServiceCallback);
        listenForNonDdsCallStateChange();
        IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyManager.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED);
        mContext.registerReceiver(mBroadcastReceiver, filter);
        mChangeListener = new SubscriptionsChangeListener(mContext.getApplicationContext(), this);
        mChangeListener.start();
        setOnPreferenceChangeListener(this);
        mContentObserver = new ContentObserver(mHandler) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                Log.d(TAG, "onChange: URI = " + uri);
                if (DUAL_DATA_PREFERENCE_URI.equals(uri)) {
                    updateUiPreference(mExtTelephonyManager.getDualDataCapability(), true);
                }
            }
        };
        mContext.getContentResolver()
                .registerContentObserver(DUAL_DATA_PREFERENCE_URI, false, mContentObserver);
    }

    public DualDataPreference(Context context) {
        this(context, null);
    }

    public void cleanUp() {
        Log.d(TAG, "Disconnecting ExtTelephonyService");
        mExtTelephonyManager.unregisterCallback(mExtPhoneCallbackListener);
        mExtTelephonyManager.disconnectService();
        for (int subId : mSubIdTelephonyCallbackMap.keySet()) {
            TelephonyCallback oldCallback = mSubIdTelephonyCallbackMap.get(subId);
            if (oldCallback != null) {
                mTelephonyManager.unregisterTelephonyCallback(oldCallback);
            }
        }
        mSubIdTelephonyCallbackMap.clear();
        mContext.unregisterReceiver(mBroadcastReceiver);
        mChangeListener.stop();
        mContext.getContentResolver().unregisterContentObserver(mContentObserver);
    }

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "msg.what: " + msg.what);
            Result result = (Result) msg.obj;
            if (result == null) return;
            Status status;
            switch (msg.what) {
                case EVENT_ON_DUAL_DATA_CAPABILITY_CHANGED:
                    status = (Status) result.mStatus;
                    boolean support = (boolean) result.mData;
                    if (status.get() != Status.SUCCESS) return;
                    if (support) {
                        updateUiPreference(true, false);
                    } else {
                        updateUiPreference(false, true);
                    }
                    break;
                case EVENT_SET_DUAL_DATA_USER_PREFERENCE_RESPONSE:
                    status = (Status) result.mStatus;
                    int currentValue = (isChecked() ? SETTING_VALUE_ON : SETTING_VALUE_OFF);
                    if (status.get() == Status.SUCCESS) {
                        Log.d(TAG, "Update preference: " + currentValue + " in database");
                        updateUserPreferenceToDb(currentValue);
                    } else {
                        Toast.makeText(mContext, R.string.dual_data_preference_selection_fail,
                                Toast.LENGTH_LONG).show();
                        //Revert the user selection
                        int previousValue = getUserPreferenceFromDb();
                        Log.d(TAG, "previousValue: " + previousValue +
                                " currentValue: " + currentValue);
                        if (previousValue != currentValue) {
                            Log.d(TAG, "Revert User selection");
                            setChecked((previousValue == 1) ? true : false);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private ExtPhoneCallbackListener mExtPhoneCallbackListener = new ExtPhoneCallbackListener() {
        @Override
        public void onDualDataCapabilityChanged(Token token, Status status, boolean support)
                throws RemoteException {
            Log.d(TAG, "onDualDataCapabilityChanged :  " + support);
            mHandler.sendMessage(mHandler.obtainMessage(
                    DualDataPreference.EVENT_ON_DUAL_DATA_CAPABILITY_CHANGED,
                    new Result(token, status, support)));
        }

        @Override
        public void setDualDataUserPreferenceResponse(Token token, Status status)
                throws RemoteException {
            Log.d(TAG, "setDualDataUserPreferenceResponse: token = " +
                    token + " Status = " + status);
            mHandler.sendMessage(mHandler.obtainMessage(
                    DualDataPreference.EVENT_SET_DUAL_DATA_USER_PREFERENCE_RESPONSE,
                    new Result(token, status, null)));
        }
    };

    private ServiceCallback mExtTelManagerServiceCallback = new ServiceCallback() {
        @Override
        public void onConnected() {
            Log.d(TAG, "mExtTelManagerServiceCallback: service connected");
            updateUiPreference(mExtTelephonyManager.getDualDataCapability(), false);
            int[] events = new int[] {
                    ExtPhoneCallbackListener.EVENT_ON_DUAL_DATA_CAPABILITY_CHANGED};
            mClient = mExtTelephonyManager.registerCallbackWithEvents(
                    mPackageName, mExtPhoneCallbackListener, events);
            Log.d(TAG, "Client = " + mClient);
        }

        @Override
        public void onDisconnected() {
            Log.d(TAG, "mExtTelManagerServiceCallback: service disconnected");
            mExtTelephonyManager.unregisterCallback(mExtPhoneCallbackListener);
            mClient = null;
            updateUiPreference(false, true);
        }
    };

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.d(TAG, "onPreferenceChange : " + (Boolean) newValue);
        if (mExtTelephonyManager != null) {
            try {
                mExtTelephonyManager.setDualDataUserPreference(mClient, (Boolean) newValue);
            } catch (Exception e) {
                Log.e(TAG, "Exception " + e);
            }
        }
        return true;
    }

    private void updateUiPreference(boolean enable, boolean forceUpdate) {
        if (mTelephonyManager.getActiveModemCount() > 1 || forceUpdate) {
            Log.d(TAG, "Preference enable: " + enable);
            setEnabled(enable);
            setChecked(getUserPreferenceFromDb() == 1 ? true : false);
        }
    }

    private void updateUserPreferenceToDb(int value) {
        Settings.Global.putInt(mContext.getContentResolver(), DUAL_DATA_PREFERENCE, value);
    }

    private int getUserPreferenceFromDb() {
        return Settings.Global.getInt(mContext.getContentResolver(), DUAL_DATA_PREFERENCE,
                SETTING_VALUE_OFF);
    }

    private class DualDataTelephonyCallback extends TelephonyCallback implements
            TelephonyCallback.CallStateListener {
        private int mSubId;
        public DualDataTelephonyCallback(int subId) {
            mSubId = subId;
        }

        @Override
        public void onCallStateChanged(int state) {
            Log.d(TAG, "onCallStateChanged: state: " + state + " for subId: " + mSubId);

            final boolean enable = state == TelephonyManager.CALL_STATE_IDLE
                    && mExtTelephonyManager != null
                    && mExtTelephonyManager.getDualDataCapability();

            Log.d(TAG, "onCallStateChanged: Enable Preference: " + enable);

            setEnabled(enable);
        }
    }

    class Result {
        Token mToken;
        Status mStatus;
        Object mData;

        public Result(Token mToken, Status mStatus, Object mData) {
            this.mToken = mToken;
            this.mStatus = mStatus;
            this.mData = mData;
        }

        @Override
        public String toString() {
            return "Result{" + "mToken=" + mToken + ", mStatus=" + mStatus + ", mData=" + mData +
                    '}';
        }
    }
}
