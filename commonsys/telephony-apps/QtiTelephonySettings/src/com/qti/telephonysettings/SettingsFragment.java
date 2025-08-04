/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qti.telephonysettings;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserManager;
import android.util.Log;
import android.util.Pair;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.qti.telephonysettings.preferences.CellularRoamingPreference;
import com.qti.telephonysettings.preferences.DualDataPreference;
import com.qti.telephonysettings.preferences.MsimPreference;
import com.qti.telephonysettings.preferences.NrModePreference;
import com.qti.telephonysettings.preferences.PortSelectionPreference;
import com.qti.telephonysettings.preferences.SmartDdsSwitchPreference;
import com.qti.telephonysettings.preferences.ImsSettingsPreference;

public class SettingsFragment extends PreferenceFragmentCompat implements
        SubscriptionsChangeListener.SubscriptionsChangeListenerClient {

    private static final String TAG = "SettingsFragment";

    private CellularRoamingPreference mCellularRoamingPreference;
    private NrModePreference mNrModePreference;
    private SmartDdsSwitchPreference mSmartDdsSwitchPreference;
    private MsimPreference mMsimPreference;
    private DualDataPreference mDualDataPreference;
    private ImsSettingsPreference mImsSettingsPreference;
    private PortSelectionPreference mPortSelectionPreference;
    private SubscriptionsChangeListener mChangeListener;
    private SubscriptionManager mSubscriptionManager;
    private TelephonyManager mTelMgr;
    private UserManager mUserManager;
    private Context mContext;

    private static final String KEY_NR_MODE = "preferred_5G_nr_mode";
    private static final String KEY_SMART_DDS_SWITCH = "smart_dds_switch";
    private static final String KEY_MSIM_PREFERENCE = "msim_preference";
    private static final String KEY_DUAL_DATA_PREFERENCE = "dual_data";
    private static final String KEY_CELLULAR_ROAMING_PREFERENCE = "cellular_roaming";
    private static final String BUTTON_IMS_SETTINGS_KEY = "ims_settings_key";
    private static final String KEY_PORT_SELECTION_PREFERENCE = "port_selection";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings_preference, rootKey);

        initPreferences();

        mContext = getActivity().getApplicationContext();
        mChangeListener = new SubscriptionsChangeListener(mContext, this);
        mTelMgr = mContext.getSystemService(TelephonyManager.class);
        mSubscriptionManager = mContext.getSystemService(SubscriptionManager.class);
        if (mSubscriptionManager != null) {
            mSubscriptionManager = mSubscriptionManager.createForAllUserProfiles();
        }
        mUserManager = mContext.getSystemService(UserManager.class);
    }

    @Override
    public void onResume() {
        super.onResume();

        mChangeListener.start();
        String title = mContext.getResources().getString(R.string.title_activity_main);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        ActionBar actionBar = activity != null ? activity.getSupportActionBar() : null;
        if (actionBar != null) actionBar.setTitle(title);
        updatePreferences();

        Log.d(TAG, "onResume()");
    }

    private void initPreferences() {
        mSmartDdsSwitchPreference =
                (SmartDdsSwitchPreference) findPreference(KEY_SMART_DDS_SWITCH);
        mNrModePreference = (NrModePreference) findPreference(KEY_NR_MODE);
        mMsimPreference = (MsimPreference) findPreference(KEY_MSIM_PREFERENCE);
        mDualDataPreference = (DualDataPreference) findPreference(KEY_DUAL_DATA_PREFERENCE);
        mCellularRoamingPreference =
                (CellularRoamingPreference) findPreference(KEY_CELLULAR_ROAMING_PREFERENCE);
        mImsSettingsPreference = (ImsSettingsPreference) findPreference(BUTTON_IMS_SETTINGS_KEY);
        mPortSelectionPreference =
                (PortSelectionPreference) findPreference(KEY_PORT_SELECTION_PREFERENCE);
    }

    private int makeRadioVersion(int major, int minor){
        if (major < 0 || minor < 0) return 0;
        return major * 100 + minor;
    }

    private boolean isAidlHalAvailable() {
        final int RADIO_HAL_VERSION_1_6 = makeRadioVersion(1, 6); // Radio HAL Version S

        TelephonyManager telephonyManager = mContext.getSystemService(TelephonyManager.class);
        if (telephonyManager == null) {
            Log.e(TAG, "TelephonyManger is NULL");
            return false;
        }
        Pair<Integer, Integer> radioVersion = telephonyManager.getRadioHalVersion();
        int halVersion = makeRadioVersion(radioVersion.first, radioVersion.second);

        Log.d(TAG, "isAidlHalAvailable: halVersion = " + halVersion);
        if (halVersion >= RADIO_HAL_VERSION_1_6) {
            return true;
        }
        return false;
    }

    private void updatePreferences() {

        if (mImsSettingsPreference != null ) {
            Log.d(TAG, "updatePreferences()" + " IMS Settings preference");
            mImsSettingsPreference.update();
        }

        if (mTelMgr == null || mUserManager == null || mNrModePreference == null) return;

        if (!mTelMgr.isDataCapable() || !mUserManager.isAdminUser()) {
            mNrModePreference.setVisible(false);
        }

        mNrModePreference.setEnabled(!mChangeListener.isAirplaneModeOn());
        mNrModePreference.update();
        int apiLevel = SystemProperties.getInt("ro.board.api_level", 0);
        if (((apiLevel >= Build.VERSION_CODES.S) ||
                ((apiLevel == 0) && isAidlHalAvailable()))
                && mTelMgr.getActiveModemCount() > 1
                && mSubscriptionManager.getActiveSubscriptionInfoCount() > 1 ) {
            mMsimPreference.setVisible(true);
        }
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
        if (mSmartDdsSwitchPreference != null) mSmartDdsSwitchPreference.cleanUp();
        if (mDualDataPreference != null) mDualDataPreference.cleanUp();
        if (mMsimPreference != null) mMsimPreference.cleanUp();
        if (mPortSelectionPreference != null) mPortSelectionPreference.cleanUp();
    }

    @Override
    public void onSubscriptionsChanged() {
        Log.d(TAG, "onSubscriptionsChanged()");
        updatePreferences();
    }

    @Override
    public void onAirplaneModeChanged(boolean airplaneModeEnabled) {
        Log.d(TAG, "onAirplaneModeChanged()");
        updatePreferences();
    }
}
