/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qti.telephonysettings.preferences;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;

import androidx.preference.Preference;

import com.qti.telephonysettings.SubscriptionsChangeListener;

public class PortSelectionPreference extends Preference
        implements SubscriptionsChangeListener.SubscriptionsChangeListenerClient {

    private static final String TAG = "PortSelectionPreference";

    private TelephonyManager mTelephonyManager;
    private SubscriptionsChangeListener mChangeListener;
    private int mActiveModemCount;

    public PortSelectionPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mTelephonyManager = context.getSystemService(TelephonyManager.class);
        mChangeListener = new SubscriptionsChangeListener(context.getApplicationContext(), this);
        mChangeListener.start();
        updatePreference();
    }

    @Override
    public void onSubscriptionsChanged() {
        updatePreference();
    }

    private void updatePreference() {
        int phoneCount = mTelephonyManager.getActiveModemCount();
        if (mActiveModemCount == phoneCount) {
            return;
        }
        setEnabled(phoneCount == 2);
        Log.d(TAG, "updatePreference phoneCount: " + phoneCount);
        mActiveModemCount = phoneCount;
    }

    @Override
    public void onAirplaneModeChanged(boolean airplaneModeEnabled) {
        Log.d(TAG, "onAirplaneModeChanged: No operation");
    }

    public void cleanUp() {
        mChangeListener.stop();
    }
}
