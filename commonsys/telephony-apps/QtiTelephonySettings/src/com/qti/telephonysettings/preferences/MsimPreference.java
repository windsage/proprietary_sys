/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qti.telephonysettings.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import androidx.preference.Preference;
import androidx.preference.ListPreference;

import java.lang.String;

import com.qti.extphone.Client;
import com.qti.extphone.ExtPhoneCallbackListener;
import com.qti.extphone.ExtTelephonyManager;
import com.qti.extphone.ServiceCallback;
import com.qti.extphone.Status;
import com.qti.extphone.Token;
import com.qti.telephonysettings.R;

public class MsimPreference extends ListPreference implements
       Preference.OnPreferenceChangeListener {

    private final String TAG = "MsimPreference";
    private static final String KEY_MSIM_PREFERENCE = "msim_preference";
    private static final String DSDA = "0";
    private static final String DSDS = "1";
    private static final String NONE = "2";

    private static final int EVENT_SET_MSIM_PREFERENCE_RESPONSE = 101;

    private Client mClient;
    private Context mContext;
    private String mPackageName;
    private ExtTelephonyManager mExtTelephonyManager;
    private boolean mServiceConnected = false;

    public MsimPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.d(TAG, "Constructor");

        mContext = context.getApplicationContext();
        mPackageName = mContext.getPackageName();
        mExtTelephonyManager = ExtTelephonyManager.getInstance(mContext);
        mExtTelephonyManager.connectService(mExtTelManagerServiceCallback);

        setOnPreferenceChangeListener(this);
    }

    public MsimPreference(Context context) {
        this(context, null);
    }

    private ServiceCallback mExtTelManagerServiceCallback = new ServiceCallback() {
        @Override
        public void onConnected() {
            Log.d(TAG, "mExtTelManagerServiceCallback: service connected");
            int[] events = new int[] {};
            mServiceConnected = true;
            mClient = mExtTelephonyManager.registerCallbackWithEvents(
                    mPackageName, mExtPhoneCallbackListener, events);
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
        }
    };

    private ExtPhoneCallbackListener mExtPhoneCallbackListener = new ExtPhoneCallbackListener() {
        @Override
        public void setMsimPreferenceResponse(Token token, Status status) throws
                RemoteException {
            Log.d(TAG, "setMsimPreferenceResponse: token = " + token + " Status = " + status);
            mHandler.sendMessage(mHandler.obtainMessage(
                MsimPreference.EVENT_SET_MSIM_PREFERENCE_RESPONSE, status));

        }
    };

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "msg.what: " + msg.what);
            Status status = (Status) msg.obj;
            switch(msg.what) {
                case EVENT_SET_MSIM_PREFERENCE_RESPONSE:

                    Log.d(TAG, "EVENT_SET_MSIM_PREFERENCE_RESPONSE");

                    if (status.get() == Status.SUCCESS) {
                        updateMsimPrefGlobalSettings(getValue());
                    } else if (getMsimPrefFromGlobalSettings() != null) {
                        /*Revert selection if QMI returns failure right away.*/
                        String selectedPreference = getValue();
                        Log.d(TAG, "selectedPreference: " + selectedPreference);
                        if (selectedPreference.equals(DSDA)) {
                            setValue(DSDS);
                            updateMsimPrefGlobalSettings(DSDS);
                        } else {
                            setValue(DSDA);
                            updateMsimPrefGlobalSettings(DSDA);
                        }
                    } else {
                        /*For the first boot,Ui is shown with no button selected by default.
                          If QMI returns failure after user selects any button, continue
                          showing no button is selected.*/
                        setValue(NONE);
                    }
                    break;
                default:
                    Log.e(TAG, "Unsupported event");
           }
        }
    };

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.d(TAG, "onPreferenceChange: isEnabled = " + newValue);
        int prefChange = Integer.parseInt(String.valueOf(newValue));

        /*If user selects the same button, ignore the selection.*/
        if ((String.valueOf(newValue)).equals(getMsimPrefFromGlobalSettings())) {
            Log.d(TAG, "Ignore the same selection");
            return true;
        }

        if (mServiceConnected && mClient != null) {
            com.qti.extphone.MsimPreference pref = new com.qti.extphone.MsimPreference(prefChange);
            try {
                mExtTelephonyManager.setMsimPreference(mClient, pref);
            } catch (Exception e) {
                Log.e(TAG, "Exception " + e);
            }
        }

        return true;
    }

    public void cleanUp() {
        Log.d(TAG, "Disconnecting ExtTelephonyService");
        mExtTelephonyManager.unregisterCallback(mExtPhoneCallbackListener);
        mExtTelephonyManager.disconnectService();
    }

    private void updateMsimPrefGlobalSettings(String msimPref) {
        Settings.Global.putString(mContext.getContentResolver(), KEY_MSIM_PREFERENCE, msimPref);
    }

    private String getMsimPrefFromGlobalSettings() {
        return Settings.Global.getString(mContext.getContentResolver(), KEY_MSIM_PREFERENCE);
    }
}
