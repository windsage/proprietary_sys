/*
* Copyright (c) 2022 Qualcomm Technologies, Inc.
* All Rights Reserved.
* Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package com.qti.telephonysettings.preferences;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import androidx.preference.Preference;

public class SimSelectionPreference extends Preference {

    private static final String TAG = "SimSelectionPreference";
    private Context mContext;

    public SimSelectionPreference(Context context) {
        this(context, null);
    }

    public SimSelectionPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;
        Log.d(TAG, "Constructor");
    }

}
