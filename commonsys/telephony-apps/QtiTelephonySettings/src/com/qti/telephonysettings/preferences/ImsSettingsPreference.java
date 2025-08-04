/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package com.qti.telephonysettings.preferences;

import android.content.Context;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionInfo;

import android.util.AttributeSet;
import android.util.Log;

import androidx.preference.Preference;

import com.qti.telephonysettings.R;

import java.util.List;

public class ImsSettingsPreference extends Preference {

    private static final String TAG = "ImsSettingsPreference";
    private Context mContext;
    private SubscriptionManager mSubscriptionManager;

    public ImsSettingsPreference(Context context) {
        this(context, null);
    }

    public ImsSettingsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;
        mSubscriptionManager = mContext.getSystemService(SubscriptionManager.class);
        if (mSubscriptionManager != null) {
            mSubscriptionManager = mSubscriptionManager.createForAllUserProfiles();
        }
        update();
        Log.d(TAG, "Constructor");
    }


    public void update() {
        final List<SubscriptionInfo> subs =
                mSubscriptionManager.getActiveSubscriptionInfoList();
        if (subs == null || subs.isEmpty()) {
            setEnabled(false);
            return;
        }
        setEnabled(true);
        refreshSummary();
        Log.d(TAG, "update()");
    }

    @Override
    public CharSequence getSummary() {
        final List<SubscriptionInfo> subs =
                mSubscriptionManager.getActiveSubscriptionInfoList();
        if (subs == null || subs.isEmpty()) {
            return null;
        }
        Log.d(TAG, "getSummary()");
        final int count = subs.size();
        return mContext.getResources().getQuantityString(R.plurals.mobile_network_summary_count,
                count, count);
    }

    /**
     * Refresh preference summary with getSummary()
     */
    private void refreshSummary() {
        final CharSequence summary = getSummary();
        if (summary == null) {
            return;
        }
        setSummary(summary);

        Log.d(TAG, "refreshSummary: " + summary);
    }

}
