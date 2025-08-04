/*
* Copyright (c) 2024 Qualcomm Technologies, Inc.
* All Rights Reserved.
* Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package com.qti.telephonysettings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.ArrayMap;
import android.util.Log;

import android.telephony.ims.feature.ImsFeature;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.android.ims.ImsException;
import com.android.ims.ImsManager;

import java.util.Map;

public class ImsSettingsFragment extends PreferenceFragmentCompat implements
        SubscriptionsChangeListener.SubscriptionsChangeListenerClient {

    private static final String TAG = "ImsSettingsFragment";
    private Context mContext;
    private SubscriptionsChangeListener mChangeListener;
    private PreferenceScreen mPreferenceScreen;
    private Map<Integer, PreferenceScreen> mPreferences;
    private TelephonyManager mTelephonyManager;
    private SubscriptionManager mSubscriptionManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        mContext = getActivity();
        mSubscriptionManager = mContext.getSystemService(SubscriptionManager.class);
        if (mSubscriptionManager != null) {
            mSubscriptionManager = mSubscriptionManager.createForAllUserProfiles();
        }
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class);
        mPreferences = new ArrayMap<>();
        mChangeListener =
                new SubscriptionsChangeListener(mContext.getApplicationContext(), this);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        mPreferenceScreen = getPreferenceScreen();
        if (mPreferenceScreen != null) {
            mPreferenceScreen.removeAll();
        }

        setPreferencesFromResource(R.xml.ims_settings_preference, rootKey);
        mPreferenceScreen = getPreferenceScreen();
    }

    @Override
    public void onResume() {
        super.onResume();

        mChangeListener.start();

        String title = mContext.getResources().getString(R.string.ims_settings_title);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        ActionBar actionBar = activity != null ? activity.getSupportActionBar() : null;
        if (actionBar != null) {
            actionBar.setTitle(title);
        }
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
        Log.d(TAG, "onDestroy()");
    }

    private void update() {
        if (mPreferenceScreen == null) {
            return;
        }

        // Since we may already have created some preferences previously, we first grab the list of
        // those, then go through the current available subscriptions making sure they are all
        // present in the screen, and finally remove any now-outdated ones.
        final Map<Integer, PreferenceScreen> existingPreferences = mPreferences;
        mPreferences = new ArrayMap<>();

        for (int slotId = 0; slotId < mTelephonyManager.getActiveModemCount(); slotId++) {
            SubscriptionInfo info = mSubscriptionManager.
                    getActiveSubscriptionInfoForSimSlotIndex(slotId);

            if (info != null) {
                final int subId = info.getSubscriptionId();
                PreferenceScreen pref = existingPreferences.remove(subId);
                if (pref == null) {
                    pref = new PreferenceScreen(mPreferenceScreen.getContext(), null);

                    ImsManager imsManager = ImsManager.getInstance(
                            mPreferenceScreen.getContext(),
                            SubscriptionManager.getPhoneId(subId));
                    try {
                        if ((imsManager.isVolteEnabledByPlatform() ||
                                imsManager.isVtEnabledByPlatform() ||
                                imsManager.isWfcEnabledByPlatform()) &&
                                imsManager.getImsServiceState() == ImsFeature.STATE_READY) {
                            mPreferenceScreen.addPreference(pref);
                        }
                    } catch (ImsException ex) {
                        Log.e(TAG, "Exception when trying to get ImsServiceStatus: " + ex);
                    }
                }
                pref.setKey("key_" +slotId);
                pref.setIconSpaceReserved(false);
                pref.setTitle(info.getDisplayName());
                pref.setOrder(slotId);
                pref.setOnPreferenceClickListener(
                    new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent intent = new Intent("org.codeaurora.IMS_SETTINGS");
                        intent.putExtra("phoneId", SubscriptionManager.getPhoneId(subId));
                        startActivity(intent);
                        return true;
                    }
                });
                mPreferences.put(subId, pref);

            } else {
                Log.d(TAG, "sub info is null for slot: " + slotId);
            }
        }

        for (PreferenceScreen pref : existingPreferences.values()) {
            mPreferenceScreen.removePreference(pref);
        }
        Log.d(TAG, "update()");
    }

    @Override
    public void onSubscriptionsChanged() {
        Log.d(TAG, "onSubscriptionsChanged()");
        update();
    }

    @Override
    public void onAirplaneModeChanged(boolean airplaneModeEnabled) {
        //no-op
        Log.d(TAG, "onAirplaneModeChanged()");
    }
}
