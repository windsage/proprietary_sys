/*
* Copyright (c) 2023-2024 Qualcomm Technologies, Inc.
* All Rights Reserved.
* Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package com.qti.telephonysettings.preferences;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import androidx.preference.Preference;

import com.qti.extphone.ExtTelephonyManager;
import com.qti.extphone.ServiceCallback;
import com.qti.telephonysettings.R;

public class CellularRoamingPreference extends Preference {

    private static final String TAG = "CellularRoamingPreference";
    private Context mContext;
    private ExtTelephonyManager mExtTelephonyManager;
    private ServiceCallback mExtTelManagerServiceCallback = new ServiceCallback() {
        @Override
        public void onConnected() {
            Log.d(TAG, "ExtTelephonyService connected");
            checkFeatureAvailability();
        }

        @Override
        public void onDisconnected() {
            Log.d(TAG, "ExtTelephonyService disconnected");
        }
    };

    public CellularRoamingPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        Log.d(TAG, "Constructor");
        mExtTelephonyManager = ExtTelephonyManager.getInstance(context);
        mExtTelephonyManager.connectService(mExtTelManagerServiceCallback);
    }

    private void checkFeatureAvailability() {
        if (mExtTelephonyManager.isFeatureSupported(ExtTelephonyManager.FEATURE_CELLULAR_ROAMING)) {
            setEnabled(true);
        } else {
            Log.d(TAG, "checkFeatureAvailability: feature unavailable");
            setSummary(mContext.getResources().getString(R.string.feature_unavailable));
        }
    }
}