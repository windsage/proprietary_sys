/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qti.telephonysettings.preferences;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.qti.telephonysettings.R;
import com.qti.telephonysettings.SubscriptionsChangeListener;

import com.qti.extphone.Client;
import com.qti.extphone.ExtPhoneCallbackListener;
import com.qti.extphone.ExtTelephonyManager;
import com.qti.extphone.ServiceCallback;
import com.qti.extphone.Token;

/**
 * Whether to allow modem to intelligently switch DDS without user direction
 * (0 = disabled, 1 = enabled)
 */
public class SmartDdsSwitchPreference extends SwitchPreference implements
        Preference.OnPreferenceChangeListener,
        SubscriptionsChangeListener.SubscriptionsChangeListenerClient {

    private final static String TAG = "SmartDdsSwitchPreference";

    private final static int EVENT_SET_DEFAULT_TOGGLE_STATE_RESPONSE = 1;

    private final static int SETTING_VALUE_ON = 1;
    private final static int SETTING_VALUE_OFF = 0;

    private static final String SMART_DDS_SWITCH = "smart_dds_switch";
    private static final Uri SMART_DDS_SWITCH_URI =
            Settings.Global.getUriFor(SMART_DDS_SWITCH);

    private Client mClient;
    private Context mContext;
    private String mPackageName;
    private final TelephonyManager mTelephonyManager;
    private ExtTelephonyManager mExtTelephonyManager;
    private SubscriptionsChangeListener mChangeListener;
    private boolean mFeatureAvailable = false;
    private boolean mServiceConnected = false;
    private boolean mSwitchEnabled = false;
    private int mCurrentState = SETTING_VALUE_OFF;
    private final ContentObserver mContentObserver;

    public SmartDdsSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.d(TAG, "Constructor");

        mContext = context.getApplicationContext();
        mCurrentState = getSwitchValue();
        mChangeListener = new SubscriptionsChangeListener(mContext, this);
        mChangeListener.start();
        mPackageName = mContext.getPackageName();
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class);
        mExtTelephonyManager = ExtTelephonyManager.getInstance(mContext);
        mExtTelephonyManager.connectService(mExtTelManagerServiceCallback);

        setOnPreferenceChangeListener(this);
        mContentObserver = new ContentObserver(mHandler) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                Log.d(TAG, "onChange: URI = " + uri);
                if (SMART_DDS_SWITCH_URI.equals(uri)) {
                    updateState(false);
                }
            }
        };
        mContext.getContentResolver()
                .registerContentObserver(SMART_DDS_SWITCH_URI, false, mContentObserver);
    }

    public SmartDdsSwitchPreference(Context context) {
        this(context, null);
    }

    public void cleanUp() {
        Log.d(TAG, "Disconnecting ExtTelephonyService");
        mExtTelephonyManager.unregisterCallback(mExtPhoneCallbackListener);
        mExtTelephonyManager.disconnectService();
        mChangeListener.stop();
        mContext.getContentResolver().unregisterContentObserver(mContentObserver);
    }

    private ServiceCallback mExtTelManagerServiceCallback = new ServiceCallback() {
        @Override
        public void onConnected() {
            Log.d(TAG, "mExtTelManagerServiceCallback: service connected");
            mServiceConnected = true;
            int[] events = new int[] {};
            mClient = mExtTelephonyManager.registerCallbackWithEvents(
                    mPackageName, mExtPhoneCallbackListener, events);
            updateState(true);
            Log.d(TAG, "Client = " + mClient);
        }

        @Override
        public void onDisconnected() {
            Log.d(TAG, "mExtTelManagerServiceCallback: service disconnected");
            if (mServiceConnected) {
                mExtTelephonyManager.unregisterCallback(mExtPhoneCallbackListener);
                mServiceConnected = false;
                mClient = null;
            }
            updateState(true);
        }
    };

    private ExtPhoneCallbackListener mExtPhoneCallbackListener = new ExtPhoneCallbackListener() {
        @Override
        public void setSmartDdsSwitchToggleResponse(Token token, boolean result) throws
                RemoteException {
            Log.d(TAG, "setSmartDdsSwitchToggleResponse: token = " + token + " result = " + result);
            if (result) {
                mHandler.sendMessage(mHandler.obtainMessage(
                        EVENT_SET_DEFAULT_TOGGLE_STATE_RESPONSE));
            }
        }
    };

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case EVENT_SET_DEFAULT_TOGGLE_STATE_RESPONSE: {
                    Log.d(TAG, "EVENT_SET_DEFAULT_TOGGLE_STATE_RESPONSE");
                    String defaultSummary = mContext.getResources().getString(
                            R.string.smart_dds_switch_summary);
                    updateUi(defaultSummary, true);
                    putSwitchValue(mSwitchEnabled ? SETTING_VALUE_ON : SETTING_VALUE_OFF);
                    break;
                }
                default:
                    Log.e(TAG, "Unsupported action");
            }
        }
    };

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        mSwitchEnabled = (Boolean) newValue;

        Log.d(TAG, "onPreferenceChange: isEnabled = " + mSwitchEnabled);

        // Temporarily update the text and disable the button until the response is received
        String waitSummary = mContext.getResources().getString(R.string.wait_summary);
        updateUi(waitSummary, false);
        if (mServiceConnected && mClient != null) {
            try {
                mExtTelephonyManager.setSmartDdsSwitchToggle(mSwitchEnabled, mClient);
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Exception " + e);
                return false;
            }
        } else {
            Log.e(TAG, "ExtTelephonyService not connected");
            return false;
        }
    }

    public void updateState(boolean forced) {
        final int smartDdsSwitch = getSwitchValue();
        if (!forced && mCurrentState == smartDdsSwitch) {
            return;
        }
        mCurrentState = smartDdsSwitch;
        setChecked(smartDdsSwitch != SETTING_VALUE_OFF);

        if (mServiceConnected) {
            try {
                mFeatureAvailable = mExtTelephonyManager.isSmartDdsSwitchFeatureAvailable();
            } catch (Exception e) {
                Log.e(TAG, "Exception " + e);
            }
            Log.d(TAG, "mFeatureAvailable: " + mFeatureAvailable);
            if (mFeatureAvailable) {
                String defaultSummary = mContext.getResources().getString(
                        R.string.smart_dds_switch_summary);
                updateUi(defaultSummary, isAvailable());
            } else {
                Log.d(TAG, "Feature unavailable");
                setVisible(false);
            }
        } else {
            Log.d(TAG, "Service not connected");
        }
    }

    private void updateUi(String summary, boolean enable) {
        Log.d(TAG, "updateUi enable: " + enable);
        setVisible(true);
        setSummary(summary);
        setEnabled(enable);
    }

    private boolean isAvailable() {
        // Only show the toggle if 1) APM is off and 2) more than one subscription is active
        SubscriptionManager subscriptionManager = mContext.getSystemService(
                SubscriptionManager.class);
        if (subscriptionManager != null) {
            subscriptionManager = subscriptionManager.createForAllUserProfiles();
            int numActiveSubscriptionInfoCount =
                    subscriptionManager.getActiveSubscriptionInfoCount();
            Log.d(TAG, "numActiveSubscriptionInfoCount: " + numActiveSubscriptionInfoCount);
            return !mChangeListener.isAirplaneModeOn() && (numActiveSubscriptionInfoCount > 1);
        }
        return false;
    }

    @Override
    public void onSubscriptionsChanged() {
        Log.d(TAG, "onSubscriptionsChanged()");
        updateState(true);
    }

    @Override
    public void onAirplaneModeChanged(boolean airplaneModeEnabled) {
        Log.d(TAG, "onAirplaneModeChanged()");
        updateState(true);
    }

    private void putSwitchValue(int state) {
        Settings.Global.putInt(mContext.getContentResolver(), SMART_DDS_SWITCH, state);
    }

    private int getSwitchValue() {
        return Settings.Global.getInt(mContext.getContentResolver(), SMART_DDS_SWITCH,
                SETTING_VALUE_OFF);
    }
}
