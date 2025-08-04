/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qti.phone.primarycard;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;

public class SettingsUserDataPreferenceContentObserver extends ContentObserver {

    private OnUserDataPreferenceChangedListener mListener;

    public SettingsUserDataPreferenceContentObserver(Handler handler) {
        super(handler);
    }

    public void register(Context context) {
        Uri uri = Settings.Global.getUriFor(PrimaryCardUtils.SETTING_USER_PREF_DATA_SUB);
        context.getContentResolver().registerContentObserver(uri, false, this);
    }

    public void unregister(Context context) {
        context.getContentResolver().unregisterContentObserver(this);
    }

    public void setOnDataPreferenceChangedListener(OnUserDataPreferenceChangedListener listener) {
        mListener = listener;
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        if (mListener != null) {
            mListener.onUserDataPreferenceChanged();
        }
    }

    public interface OnUserDataPreferenceChangedListener {
        void onUserDataPreferenceChanged();
    }
}
