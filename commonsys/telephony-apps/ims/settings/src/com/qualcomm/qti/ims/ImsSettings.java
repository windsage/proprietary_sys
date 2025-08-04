/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 * Not a Contribution.
 * Apache license notifications and license are retained
 * for attribution purposes only.
 *
 * Copyright (c) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qualcomm.qti.ims;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.MediaStore;
import android.provider.Settings;
import android.telephony.ims.ProvisioningManager;
import android.telephony.ims.stub.ImsConfigImplBase;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import org.codeaurora.ims.QtiCallConstants;
import org.codeaurora.ims.QtiImsException;
import org.codeaurora.ims.utils.QtiCallUtils;
import org.codeaurora.ims.utils.QtiCarrierConfigHelper;
import org.codeaurora.ims.utils.QtiImsExtUtils;
import org.codeaurora.telephony.utils.Preconditions;

/**
 * The activity class for editing a new or existing IMS profile.
 */
public class ImsSettings extends PreferenceActivity
        implements Preference.OnPreferenceChangeListener {

    private static final String TAG = ImsSettings.class.getSimpleName();
    public static final String SUB_ID_EXTRA =
            "com.android.phone.settings.SubscriptionInfoHelper.SubscriptionId";
    private static final int LOAD_IMAGE = 1;

    /* Request code for activity that handles intent defined in selectImage API.
       This code will be returned in onActivityResult() when activity exits */
    public static final int RESULT_SELECT_IMAGE = 0;
    public static final String BUTTON_SET_AUTO_REJECT_MODE_KEY= "ims_ar_mode";
    public static final String BUTTON_SET_STATIC_IMAGE_KEY = "ims_vt_call_static_image";
    public static final String BUTTON_VT_CALL_QUALITY_KEY = "ims_vt_call_quality";
    public static final String BUTTON_RTT_OPERATION_MODE_KEY = "ims_rtt_operation_mode";
    public static final String KEY_TOGGLE_RTT = "toggle_rtt";
    public static final String KEY_TOGGLE_RTT_VISIBILITY = "rtt_visibility";
    public static final String KEY_TOGGLE_RTT_CALL_TYPE = "toggle_rtt_call_type";
    public static final String KEY_TOGGLE_CALL_COMPOSER = "toggle_call_composer";
    public static final String KEY_TOGGLE_B2C_ENRICHED_CALL = "toggle_b2c_enriched_call";
    public static final String KEY_TOGGLE_DATA_CHANNEL = "toggle_data_channel";

    private ListPreference mVideoCallQuality;
    private ListPreference mAutoRejectMode = null;
    private ListPreference mRttOperationMode;

    private PreferenceScreen mScreen = null;
    private Preference mStaticImagePreference = null;

    private EditTextPreference mDeflectNum = null;
    private SwitchPreference mButtonCsRetry = null;
    private int[] mCallState = null;
    private PhoneStateListener[] mPhoneStateListener = null;
    int mPhoneId = QtiCallConstants.INVALID_PHONE_ID;

    private SwitchPreference mRttModePreference = null;
    private SwitchPreference mRttVisibilityPreference = null;
    private SwitchPreference mRttCallTypePreference = null;
    private SwitchPreference mCallComposerPreference = null;
    private SwitchPreference mB2cEnrichedCallPreference = null;
    private SwitchPreference mDataChannelPreference = null;

    private CarrierConfigManager mConfigManager;
    private ProvisioningManager mProvisionManager = null;

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        registerPhoneStateListeners();
        displayVideoQualityOptions();
        displayDeflectNumEditor();
        displayCsRetryOpions();
        displayStaticImageOptions();
        displayAutoRejectModeOptions();
        displayCallComposerModeOptions();
        displayB2CEnrichedCallSupportOptions();
        displayDataChannelSupportOptions();
        removeRttPreferences();

        if (QtiImsExtUtils.isRttSupported(mPhoneId, getApplicationContext())) {
            if (QtiImsExtUtils.shallShowRttVisibilitySetting(mPhoneId, this)) {
                displayRttVisibilityOption();
                displayRttCallTypeOption();
            } else {
                displayRttOption();
                displayRttOperationOption();
            }
        } else {
            Log.d(TAG, "RTT: not supported");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        unRegisterPhoneStateListeners();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        QtiCarrierConfigHelper.getInstance().teardown();
        mStaticImagePreference = null;
        mDeflectNum = null;
        mScreen = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "start ImsSettings");
        super.onCreate(savedInstanceState);
        setupEdgeToEdge();

        addPreferencesFromResource(R.xml.ims_settings);
        mScreen = getPreferenceScreen();

        if (mScreen == null) {
            Log.e(TAG, "PreferenceScreen is invalid");
            return;
        }

        mPhoneId = getIntent().getIntExtra(QtiCallConstants.EXTRA_PHONE_ID,
                QtiCallConstants.INVALID_PHONE_ID);
        Log.d(TAG, "ImsSetting mPhoneId = " + mPhoneId);
        mConfigManager = (CarrierConfigManager)
                getSystemService(Context.CARRIER_CONFIG_SERVICE);
        QtiCarrierConfigHelper.getInstance().setup(
                getApplicationContext());
        mVideoCallQuality = (ListPreference) mScreen
                .findPreference(BUTTON_VT_CALL_QUALITY_KEY);

        mStaticImagePreference = (Preference) mScreen.
                findPreference(BUTTON_SET_STATIC_IMAGE_KEY);

        mButtonCsRetry = (SwitchPreference)mScreen.findPreference(
                getString(R.string.ims_to_cs_retry));

        mRttModePreference = (SwitchPreference) findPreference(KEY_TOGGLE_RTT);
        mRttVisibilityPreference = (SwitchPreference) findPreference(KEY_TOGGLE_RTT_VISIBILITY);
        mRttCallTypePreference = (SwitchPreference) findPreference(KEY_TOGGLE_RTT_CALL_TYPE);
        mAutoRejectMode = (ListPreference) mScreen.findPreference(BUTTON_SET_AUTO_REJECT_MODE_KEY);
        mCallComposerPreference = (SwitchPreference) mScreen.findPreference(
                KEY_TOGGLE_CALL_COMPOSER);
        mRttOperationMode = (ListPreference) mScreen.findPreference(BUTTON_RTT_OPERATION_MODE_KEY);
        mB2cEnrichedCallPreference = (SwitchPreference) mScreen.findPreference(
                KEY_TOGGLE_B2C_ENRICHED_CALL);
        mDataChannelPreference = (SwitchPreference) mScreen.findPreference(
                KEY_TOGGLE_DATA_CHANNEL);

        try {
            mProvisionManager = ProvisioningManager.createForSubscriptionId(getSubscriptionId());
        } catch (IllegalArgumentException e) {
            mProvisionManager = null;
            Log.e(TAG, "ProvisioningManager is not available");
        }

        TelephonyManager tm = (TelephonyManager) getBaseContext().getSystemService(
                Context.TELEPHONY_SERVICE);
        mPhoneStateListener = new PhoneStateListener[tm.getPhoneCount()];
        mCallState = new int[mPhoneStateListener.length];

        mDeflectNum = (EditTextPreference) mScreen.findPreference(getString(
                R.string.qti_ims_call_deflect));
        mDeflectNum.setOnPreferenceChangeListener(this);

        if (tm.getPhoneCount() > 1) {
            initAutoRejectModeOptions();
        } else {
            Log.d(TAG, "Call modes for single sim are not supported");
        }

        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void registerPhoneStateListeners() {
        TelephonyManager tm =
                (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        SubscriptionManager subMgr = (SubscriptionManager) getSystemService(
                Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        if (tm == null || subMgr == null) {
            Log.e(TAG, "TelephonyManager or SubscriptionManager is null");
            return;
        }
        subMgr = subMgr.createForAllUserProfiles();

        for (int i = 0; i < mPhoneStateListener.length; i++) {
            final SubscriptionInfo subInfo =
                    subMgr.getActiveSubscriptionInfoForSimSlotIndex(i);
            if (subInfo == null) {
                Log.e(TAG, "registerPhoneStateListener subInfo : " + subInfo +
                        " for phone Id: " + i);
                continue;
            }

            tm = tm.createForSubscriptionId(subInfo.getSubscriptionId());

            final int phoneId = i;
            mPhoneStateListener[i]  = new PhoneStateListener() {
                /*
                * Enable/disable the "set static image" and "VT call quality" button
                * when in/out of a call
                * @see android.telephony.PhoneStateListener#onCallStateChanged(int,
                * java.lang.String)
                */
                @Override
                public void onCallStateChanged(int state, String incomingNumber) {
                    Log.d(TAG, "PhoneStateListener onCallStateChanged: state is " + state +
                            " SubId: " + subInfo.getSubscriptionId());
                    mCallState[phoneId] = state;
                    Preference pref = getPreferenceScreen().
                            findPreference(BUTTON_SET_STATIC_IMAGE_KEY);
                    if (pref != null) {
                        pref.setEnabled(isCallStateIdle());
                    }
                    Preference vtQualityPref = getPreferenceScreen().
                            findPreference(BUTTON_VT_CALL_QUALITY_KEY);
                    if (vtQualityPref != null) {
                        vtQualityPref.setEnabled(isCallStateIdle());
                    }
                }
            };
            Log.d(TAG, "Register for call state change for phone Id: " + i);
            tm.listen(mPhoneStateListener[i], PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    private void unRegisterPhoneStateListeners() {
        TelephonyManager tm =
                (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        SubscriptionManager subMgr = (SubscriptionManager) getSystemService(
                Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        if (tm == null || subMgr == null) {
            Log.e(TAG, "TelephonyManager or SubscriptionManager is null");
            return;
        }
        subMgr = subMgr.createForAllUserProfiles();

        for (int i = 0; i < mPhoneStateListener.length; i++) {
            if (mPhoneStateListener[i] != null) {
                final SubscriptionInfo subInfo =
                        subMgr.getActiveSubscriptionInfoForSimSlotIndex(i);
                if (subInfo == null) {
                    Log.e(TAG, "registerPhoneStateListener subInfo : " + subInfo +
                            " for phone Id: " + i);
                    continue;
                }

                tm = tm.createForSubscriptionId(subInfo.getSubscriptionId());
                Log.d(TAG, "unRegister for call state change for phone Id: " + i);
                tm.listen(mPhoneStateListener[i], PhoneStateListener.LISTEN_NONE);
                mPhoneStateListener[i] = null;
            }
        }
    }

    private void displayVideoQualityOptions() {
        if (mScreen == null || mVideoCallQuality == null) {
            Log.e(TAG, "displayVideoQualityOptions PreferenceScreen/VideoQuality is invalid");
            return;
        }

        //Remove the preference by default and add only if supported
        mScreen.removePreference(mVideoCallQuality);
        mVideoCallQuality.setOnPreferenceChangeListener(null);

        int videoQuality = getVideoQuality();
        //Show the video quality prefernce only if modem reported valid video quality and
        //config video quality UI is enabled.
        if (QtiImsExtUtils.shallShowVideoQuality(mPhoneId, this) &&
                videoQuality != ImsConfigImplBase.CONFIG_RESULT_UNKNOWN) {
            mScreen.addPreference(mVideoCallQuality);
            mVideoCallQuality.setOnPreferenceChangeListener(this);
            /* If High video quality is not supported, then remove High option */
            if (!(CameraUtil.getInstance().isHighVideoQualitySupported(this))) {
                mVideoCallQuality.setEntries(
                        R.array.ims_vt_call_quality_entries_high_not_supported);
                mVideoCallQuality.setEntryValues(
                        R.array.ims_vt_call_quality_values_high_not_supported);
            }
            loadVideoCallQualityPrefs(videoQuality);
        }
    }

    private void displayCsRetryOpions() {
        if (mScreen == null || mButtonCsRetry == null) {
            Log.e(TAG, "displayCsRetryOptions PreferenceScreen is invalid");
            return;
        }

        //remove the references by default and add them only when config is enabled
        mScreen.removePreference(mButtonCsRetry);
        mButtonCsRetry.setOnPreferenceChangeListener(null);

        //Enable CS Retry settings depending on CS Retry Config
        if (QtiImsExtUtils.isCsRetryConfigEnabled(mPhoneId, getApplicationContext())) {
            mScreen.addPreference(mButtonCsRetry);
            mButtonCsRetry.setOnPreferenceChangeListener(this);
            mButtonCsRetry.setChecked(QtiCallUtils.isCsRetryEnabledByUser(
                    getApplicationContext()));
        }
    }

    private void displayStaticImageOptions() {
        if (mScreen == null || mStaticImagePreference == null) {
            Log.e(TAG, "displayStaticImageOptions PreferenceScreen is invalid");
            return;
        }

        //remove the references by default and add them only when config is enabled
        mScreen.removePreference(mStaticImagePreference);
        mStaticImagePreference.setOnPreferenceClickListener(null);

        //Enable static image options if static image config is enabled
        if (QtiImsExtUtils.shallShowStaticImageUi(mPhoneId, getApplicationContext())) {
            mScreen.addPreference(mStaticImagePreference);
            mStaticImagePreference.setOnPreferenceClickListener(prefClickListener);
        }
    }

    private void displayCallComposerModeOptions() {
        if (mScreen == null || mCallComposerPreference == null) {
            Log.e(TAG, "displayCallComposerMode Preference screen is invalid");
            return;
        }

        // remove the reference by default and add them only when config is enabled
        mScreen.removePreference(mCallComposerPreference);
        mCallComposerPreference.setOnPreferenceChangeListener(null);

        if (QtiImsExtUtils.isCallComposerSupported(mPhoneId, getApplicationContext())) {
            mScreen.addPreference(mCallComposerPreference);
            mCallComposerPreference.setOnPreferenceChangeListener(this);
            // Enable call composer settings based on config per phone id
            boolean isChecked = QtiImsExtUtils.getCallComposerMode(
                    getApplicationContext().getContentResolver(),
                    mPhoneId) == QtiCallConstants.CALL_COMPOSER_ENABLED;
            mCallComposerPreference.setChecked(isChecked);
            Log.d(TAG, "CallComposer mode: UI Option = " + mCallComposerPreference.isChecked());
        }
    }

    private void displayB2CEnrichedCallSupportOptions() {
        if (mScreen == null || mB2cEnrichedCallPreference == null) {
            Log.e(TAG, "displayB2CEnrichedCallSupport Preference screen is invalid");
            return;
        }

        // remove the reference by default and add them only when config is enabled
        mScreen.removePreference(mB2cEnrichedCallPreference);
        mB2cEnrichedCallPreference.setOnPreferenceChangeListener(null);

        if (QtiImsExtUtils.isB2cEnrichedCallingSupported(mPhoneId, getApplicationContext())) {
            mScreen.addPreference(mB2cEnrichedCallPreference);
            mB2cEnrichedCallPreference.setOnPreferenceChangeListener(this);
            // Enable b2c enriched call settings based on config per phone id
            boolean isChecked = QtiImsExtUtils.getB2cEnrichedCallingMode(
                    getApplicationContext().getContentResolver(),
                    mPhoneId) == QtiCallConstants.B2C_ENRICHED_CALLING_ENABLED;
            mB2cEnrichedCallPreference.setChecked(isChecked);
            Log.d(TAG, "B2cEnrichedCall mode: UI Option = " +
                    mB2cEnrichedCallPreference.isChecked());
        }
    }

    private void displayDataChannelSupportOptions() {
        if (mScreen == null || mDataChannelPreference == null) {
            Log.e(TAG, "displayDataChannelSupportOptions Preference screen is invalid");
            return;
        }

        // remove the reference by default and add them only when config is enabled
        mScreen.removePreference(mDataChannelPreference);
        mDataChannelPreference.setOnPreferenceChangeListener(null);

        if (QtiImsExtUtils.isDataChannelSupported(mPhoneId, getApplicationContext())) {
            mScreen.addPreference(mDataChannelPreference);
            mDataChannelPreference.setOnPreferenceChangeListener(this);
            // Data Channel settings based on config per phone id
            boolean isChecked = QtiImsExtUtils.getDataChannelUserPreference(
                    getApplicationContext().getContentResolver(),
                    mPhoneId) == QtiCallConstants.DATA_CHANNEL_ENABLED;
            mDataChannelPreference.setChecked(isChecked);
            Log.d(TAG, "Data Channel mode: UI Option = " +
                    mDataChannelPreference.isChecked());
        }
    }

    private void onCallComposerPreferenceChanged(SwitchPreference callComposerPref) {
        // handle user selecting the SwitchPreference for call composer pref
        boolean isEnabled = !callComposerPref.isChecked();
        callComposerPref.setChecked(isEnabled);

        Log.d(TAG, "onCallComposerPreferenceChanged isEnabled = " + isEnabled);
        int result = ImsConfigImplBase.CONFIG_RESULT_FAILED;
        // update database to reflect UI setting
        QtiImsExtUtils.setCallComposerMode(
                getApplicationContext().getContentResolver(), mPhoneId, isEnabled);

        try {
            int value = isEnabled ? QtiCallConstants.CALL_COMPOSER_ENABLED :
                    QtiCallConstants.CALL_COMPOSER_DISABLED;
            if (mProvisionManager != null) {
                result = mProvisionManager.setProvisioningIntValue(
                        QtiCallConstants.CALL_COMPOSER_MODE, value);
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "setCallComposerMode failed. Exception = " + e);
            result = ImsConfigImplBase.CONFIG_RESULT_FAILED;
        }
        if (hasRequestFailed(result)) {
            // reset UI and database for set config failure
            mCallComposerPreference.setChecked(!isEnabled);
            QtiImsExtUtils.setCallComposerMode(
                getApplicationContext().getContentResolver(), mPhoneId, !isEnabled);
            displayToast("Setting call composer user setting failed.");
        }
    }

    private void onB2cEnrichedCallPreferenceChanged(SwitchPreference b2cEnrichedCallPref) {
        // handle user selecting the SwitchPreference for b2c enriched call pref
        boolean isEnabled = !b2cEnrichedCallPref.isChecked();
        b2cEnrichedCallPref.setChecked(isEnabled);

        Log.d(TAG, "onB2cEnrichedCallPreferenceChanged isEnabled = " + isEnabled);
        int result = ImsConfigImplBase.CONFIG_RESULT_FAILED;
        // update database to reflect UI setting
        QtiImsExtUtils.setB2cEnrichedCallingMode(
                getApplicationContext().getContentResolver(), mPhoneId, isEnabled);

        try {
            int value = isEnabled ? QtiCallConstants.B2C_ENRICHED_CALLING_ENABLED :
                    QtiCallConstants.B2C_ENRICHED_CALLING_DISABLED;
            if (mProvisionManager != null) {
                result = mProvisionManager.setProvisioningIntValue(
                        QtiCallConstants.B2C_ENRICHED_CALLING_MODE, value);
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "setB2cEnrichedCallingMode failed. Exception = " + e);
            result = ImsConfigImplBase.CONFIG_RESULT_FAILED;
        }
        if (hasRequestFailed(result)) {
            // reset UI and database for set config failure
            mB2cEnrichedCallPreference.setChecked(!isEnabled);
            QtiImsExtUtils.setB2cEnrichedCallingMode(
                getApplicationContext().getContentResolver(), mPhoneId, !isEnabled);
            displayToast("Setting b2c enriched call user setting failed.");
        }
    }

    private void onDataChannelPreferenceChanged(SwitchPreference dataChannelPref) {
        // handle user selecting the SwitchPreference for data channel call pref
        boolean isEnabled = !dataChannelPref.isChecked();
        dataChannelPref.setChecked(isEnabled);

        Log.d(TAG, "onDataChannelPreferenceChanged isEnabled = " + isEnabled);
        int result = ImsConfigImplBase.CONFIG_RESULT_FAILED;
        // update database to reflect UI setting
        QtiImsExtUtils.setDataChannelUserPreference(
                getApplicationContext().getContentResolver(), mPhoneId, isEnabled);

        try {
            int value = isEnabled ? QtiCallConstants.DATA_CHANNEL_ENABLED :
                    QtiCallConstants.DATA_CHANNEL_DISABLED;
            if (mProvisionManager != null) {
                result = mProvisionManager.setProvisioningIntValue(
                        QtiCallConstants.DATA_CHANNEL_MODE, value);
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "setDataChannelUserPreference failed. Exception = " + e);
            result = ImsConfigImplBase.CONFIG_RESULT_FAILED;
        }
        if (hasRequestFailed(result)) {
            // reset UI and database for set config failure
            mDataChannelPreference.setChecked(!isEnabled);
            QtiImsExtUtils.setDataChannelUserPreference(
                getApplicationContext().getContentResolver(), mPhoneId, !isEnabled);
            displayToast("Setting data channel user setting failed.");
        }
    }

    private void initAutoRejectModeOptions() {
        mAutoRejectMode.setPersistent(true);
        mAutoRejectMode.setOnPreferenceChangeListener(this);
    }

    private void displayAutoRejectModeOptions() {
        if (mScreen == null || mAutoRejectMode == null) {
            Log.e(TAG, "displayAutoRejectModeOptions Preference screen is invalid");
            return;
        }

        // remove preference and only add if msim enabled
        mScreen.removePreference(mAutoRejectMode);
        mAutoRejectMode.setOnPreferenceChangeListener(null);

        TelephonyManager tm =
                (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (tm.getPhoneCount() > 1) {
            mScreen.addPreference(mAutoRejectMode);
            mAutoRejectMode.setOnPreferenceChangeListener(this);
            mAutoRejectMode.setEntries(R.array.ims_ar_mode_entries);
            mAutoRejectMode.setEntryValues(R.array.ims_ar_mode_values);
            // retrieve auto reject mode  per phone id
            int value = QtiImsExtUtils.getAutoRejectMode(
                    getApplicationContext().getContentResolver(),
                    mPhoneId);
            loadAutoRejectModePrefs(value);
            Log.d(TAG, "AutoReject call mode: UI Option = " + value);
        }
    }

    private String autoRejectModeToString(int callMode) {
        switch(callMode) {
            case QtiCallConstants.AR_MODE_ALLOW_INCOMING:
                return getString(R.string.ims_ar_mode_allow_incoming);
            case QtiCallConstants.AR_MODE_AUTO_REJECT:
                return getString(R.string.ims_ar_mode_auto_reject);
            case QtiCallConstants.AR_MODE_ALLOW_ALERTING:
                return getString(R.string.ims_ar_mode_allow_alerting);
            default:
                return getString(R.string.ims_ar_mode_unknown);
        }
    }

    private boolean onAutoRejectModePreferenceChange(int value) {
        boolean success = setAutoRejectMode(value);
        if (success) {
            loadAutoRejectModePrefs(value);
        }
        return success;

    }

    private void loadAutoRejectModePrefs(int vqValue) {
        Log.d(TAG, "loadAutoRejectModePrefs, vqValue = " + vqValue);
        final String arMode = autoRejectModeToString(vqValue);
        mAutoRejectMode.setValue(String.valueOf(vqValue));
        mAutoRejectMode.setSummary(arMode);
    }

    private boolean setAutoRejectMode(int arMode) {
        int result = ImsConfigImplBase.CONFIG_RESULT_FAILED;

        try {
            if (mProvisionManager != null) {
                result = mProvisionManager.setProvisioningIntValue(
                        QtiCallConstants.AUTO_REJECT_CALL_MODE, arMode);
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "setAutoRejectMode failed. Exception = " + e);
            result = ImsConfigImplBase.CONFIG_RESULT_FAILED;
        }
        boolean isFailed = hasRequestFailed(result);
        if (!isFailed) {
            // update database to reflect UI setting
            QtiImsExtUtils.setAutoRejectMode(
                    getApplicationContext().getContentResolver(), mPhoneId, arMode);
        } else if (result == QtiCallConstants.CONFIG_RESULT_NOT_SUPPORTED) {
            displayToast("This auto reject  mode option is not supported");
        }
        return !isFailed;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {  // See ActionBar#setDisplayHomeAsUpEnabled()
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setCsRetry(boolean turnOn) {
        final int value = (turnOn) ? 1:0;
        android.provider.Settings.Global.putInt(
                  getApplicationContext().getContentResolver(),
                  QtiCallConstants.IMS_TO_CS_RETRY_ENABLED, value);
    }

    private void displayToast(String displayStr) {
        Toast.makeText(getApplicationContext(), displayStr, Toast.LENGTH_SHORT).show();
    }

    /* This API prepares an intent with which an activity is launched.
       User then can select the image from that activity */
    private void selectImage() {
        Log.d(TAG, "selectImage");

        final String[] ACCEPT_MIME_TYPES = {
            "image/jpeg",
            "image/jpg",
            "image/png"
        };

        Intent intent = new Intent(
                Intent.ACTION_GET_CONTENT,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        /* allow the user to select data that is already on the device
           not requiring it be downloaded from a remote service when opened */
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        // restrict activity to show images of only ACCEPT_MIME_TYPES
        intent.putExtra(Intent.EXTRA_MIME_TYPES, ACCEPT_MIME_TYPES);

        // check if there is any activity that can handle this intent
        ComponentName cName = intent.resolveActivity(getPackageManager());
        Log.d(TAG, "selectImage cName : " + cName);
        if (cName != null) {
            startActivityForResult(intent, RESULT_SELECT_IMAGE);
        } else {
            Log.w(TAG, "UE cannot handle this intent");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult requestCode = " + requestCode);
        if (requestCode == RESULT_SELECT_IMAGE) {
            if (data == null) {
                Log.w(TAG, "possibly user didn't select any image");
                return;
            }

            TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            if (tm.getCallState() != TelephonyManager.CALL_STATE_IDLE) {
                Log.w(TAG, "call state is not idle so ignore user selected image");
                Toast.makeText(this, getResources().getString(
                        R.string.ims_vt_call_static_image_ignore).toString(),
                        Toast.LENGTH_SHORT).show();
                return;
            }

            Uri uri = data.getData();
            getApplicationContext().grantUriPermission("org.codeaurora.dialer", uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
            getApplicationContext().grantUriPermission("org.codeaurora.ims", uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);

            String uriStr = uri != null ? uri.toString() : null;
            Log.d(TAG, "uri = " + uri + " uriStr = " + uriStr);

            if (uriStr != null) {
                //store the imageUriStr in DB
                saveStaticImageUriStr(getContentResolver(), uriStr);
                Toast.makeText(this, getResources().getString(
                        R.string.ims_vt_call_static_image_okay).toString(),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getResources().getString(
                        R.string.ims_vt_call_static_image_error).toString(),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    /* Saves the file path of static image selected by the user in DB */
    private void saveStaticImageUriStr(ContentResolver contentResolver, String uri) {
        android.provider.Settings.Global.putString(contentResolver,
                QtiImsExtUtils.QTI_IMS_STATIC_IMAGE_SETTING, uri);
    }

    Preference.OnPreferenceClickListener prefClickListener =
            new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference pref) {
            Log.d(TAG, "onPreferenceClick");
            if (pref.equals(mStaticImagePreference)) {
                selectImage();
            }
            return true;
        }
    };

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        if (pref.equals(mVideoCallQuality)) {
            if (newValue == null) {
                Log.e(TAG, "onPreferenceChange invalid value received");
            } else {
                final int quality = Integer.parseInt(newValue.toString());
                boolean result = setVideoQuality(quality);
                if (result) {
                    loadVideoCallQualityPrefs(quality);
                }
                return result;
            }
        } else if (pref.equals(mDeflectNum)) {
            if (newValue == null) {
                Log.e(TAG, "onPreferenceChange Deflect number invalid value received");
            } else {
                QtiImsExtUtils.setCallDeflectNumber(
                        getApplicationContext().getContentResolver(), newValue.toString());
                displayDeflectNumEditor();
            }
        } else if (pref.equals(mButtonCsRetry)) {
            SwitchPreference csRetryPref = (SwitchPreference)pref;
            csRetryPref.setChecked(!csRetryPref.isChecked());
            setCsRetry(csRetryPref.isChecked());
        } else if (pref.equals(mRttModePreference)) {
            SwitchPreference rttPref = (SwitchPreference)pref;
            rttPref.setChecked(!rttPref.isChecked());
            setRttMode(rttPref.isChecked());
            Log.d(TAG, "RTT: onPreferenceChange mode = " + rttPref.isChecked());
        } else if (pref.equals(mRttVisibilityPreference)) {
            SwitchPreference rttPref = (SwitchPreference)pref;
            rttPref.setChecked(!rttPref.isChecked());
            QtiImsExtUtils.setRttVisibility(rttPref.isChecked(), getApplicationContext(),
                    mPhoneId);
            Log.d(TAG, "RTT: onPreferenceChange visibility setting = " + rttPref.isChecked());
        } else if (pref.equals(mRttCallTypePreference)) {
            SwitchPreference rttPref = (SwitchPreference)pref;
            rttPref.setChecked(!rttPref.isChecked());
            QtiImsExtUtils.setCanStartRttCall(rttPref.isChecked(), getApplicationContext(),
                    mPhoneId);
            Log.d(TAG, "RTT: onPreferenceChange rtt call type = " + rttPref.isChecked());
        } else if (pref.equals(mAutoRejectMode)) {
            if (newValue == null) {
                Log.e(TAG, "AutoRejectMode: onPreferenceChange invalid value received");
            } else {
                final int value = Integer.parseInt(newValue.toString());
                return onAutoRejectModePreferenceChange(value);
            }
        } else if (pref.equals(mCallComposerPreference)) {
            SwitchPreference callComposerPref = (SwitchPreference) pref;
            onCallComposerPreferenceChanged(callComposerPref);
            Log.d(TAG, "CallComposer: onPreferenceChange setting = " +
                    callComposerPref.isChecked());
        } else if (pref.equals(mRttOperationMode)) {
            if (newValue == null) {
                Log.e(TAG, "RttOperationMode: onPreferenceChange invalid value received");
            } else {
                final int value = Integer.parseInt(newValue.toString());
                onRttOperationModePreferenceChange(value);
            }
        } else if (pref.equals(mB2cEnrichedCallPreference)) {
            SwitchPreference b2cEnrichedCallPref = (SwitchPreference) pref;
            onB2cEnrichedCallPreferenceChanged(b2cEnrichedCallPref);
            Log.d(TAG, "B2cEnrichedCall: onPreferenceChange setting = " +
                    b2cEnrichedCallPref.isChecked());
        } else if (pref.equals(mDataChannelPreference)) {
            SwitchPreference dataChannelPref = (SwitchPreference) pref;
            onDataChannelPreferenceChanged(dataChannelPref);
            Log.d(TAG, "Data Channel: onPreferenceChange setting = " +
                    dataChannelPref.isChecked());
        }
        return true;
    }

    private void loadVideoCallQualityPrefs(int vqValue) {
        Log.d(TAG, "loadVideoCallQualityPrefs, vqValue = " + vqValue);
        final String videoQuality = videoQualityToString(vqValue);
        mVideoCallQuality.setValue(String.valueOf(vqValue));
        mVideoCallQuality.setSummary(videoQuality);
    }

    private int getVideoQuality() {
        int videoQuality = ImsConfigImplBase.CONFIG_RESULT_UNKNOWN;
        try {
            if (mProvisionManager != null) {
                videoQuality = mProvisionManager.getProvisioningIntValue(
                        ProvisioningManager.KEY_VIDEO_QUALITY);
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "getVideoQuality failed. Exception = " + e);
        }
        return videoQuality;
    }

    private boolean setVideoQuality(int quality) {
        int result = ImsConfigImplBase.CONFIG_RESULT_FAILED;
        try {
            if (mProvisionManager != null) {
                result = mProvisionManager.setProvisioningIntValue(
                        ProvisioningManager.KEY_VIDEO_QUALITY, quality);
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "setVideoQuality failed. Exception = " + e);
        }
        Log.d(TAG, "setVideoQuality, result = " + result);
        return !hasRequestFailed(result);
    }

    private static boolean hasRequestFailed(int result) {
        return (result != ImsConfigImplBase.CONFIG_RESULT_SUCCESS);
    }

    private String videoQualityToString(int quality) {
        switch (quality) {
            case QtiImsExtUtils.VideoQualityFeatureValuesConstants.HIGH:
                return getString(R.string.ims_vt_call_quality_high);
            case QtiImsExtUtils.VideoQualityFeatureValuesConstants.MEDIUM:
                return getString(R.string.ims_vt_call_quality_medium);
            case QtiImsExtUtils.VideoQualityFeatureValuesConstants.LOW:
                return getString(R.string.ims_vt_call_quality_low);
            case ImsConfigImplBase.CONFIG_RESULT_UNKNOWN:
            default:
                return getString(R.string.ims_vt_call_quality_unknown);
        }
    }

    private boolean isCallDeflectionOrTransferEnabled() {
        return isCallDeflectionSupported() || isCallTranferedSupported();
    }

    private boolean isCallTranferedSupported() {
        if (mConfigManager == null) {
            return false;
        }
        PersistableBundle b = mConfigManager.getConfigForSubId(getSubscriptionId());
        return b != null && b.getBoolean(
            CarrierConfigManager.KEY_CARRIER_ALLOW_TRANSFER_IMS_CALL_BOOL);
    }

    private boolean isCallDeflectionSupported() {
        boolean isCallDeflectionSupported = false;
        if (mConfigManager != null) {
            PersistableBundle b = mConfigManager.getConfigForSubId(getSubscriptionId());
            if (b != null) {
                isCallDeflectionSupported = b.getBoolean(
                        CarrierConfigManager.KEY_CARRIER_ALLOW_DEFLECT_IMS_CALL_BOOL);
            }
        }
        return isCallDeflectionSupported;
    }

    private int getSubscriptionId() {
        SubscriptionManager subscriptionManager = (SubscriptionManager) getSystemService(
                Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        if (subscriptionManager == null) {
            return subscriptionManager.INVALID_SUBSCRIPTION_ID;
        }
        subscriptionManager = subscriptionManager.createForAllUserProfiles();
        SubscriptionInfo subInfo = subscriptionManager.
                getActiveSubscriptionInfoForSimSlotIndex(mPhoneId);
        if (subInfo == null) {
            return subscriptionManager.INVALID_SUBSCRIPTION_ID;
        }
        return subInfo.getSubscriptionId();
    }

    private void displayDeflectNumEditor() {
        if (mScreen == null || mDeflectNum == null) {
            Log.e(TAG, "displayDeflectNumEditor PreferenceScreen is invalid");
            return;
        }

        //remove the references by default and add them only when config is enabled
        mScreen.removePreference(mDeflectNum);
        mDeflectNum.setOnPreferenceChangeListener(null);

        //Enable Call deflection editor depending on platform support
        if (isCallDeflectionOrTransferEnabled()) {
            mScreen.addPreference(mDeflectNum);
            mDeflectNum.setOnPreferenceChangeListener(this);

            String number = QtiImsExtUtils.getCallDeflectNumber(
                    getApplicationContext().getContentResolver());
            if (number != null) {
                mDeflectNum.setText(number);
                mDeflectNum.setSummary(number);
            } else {
                mDeflectNum.setText("");
                mDeflectNum.setSummary(getString(R.string.qti_ims_number_not_set));
            }
        }
    }

    private void removeRttPreferences() {
        if (mScreen == null) {
            Log.e(TAG, "removeRttPreferences mScreen is invalid");
            return;
        }
        if (mRttModePreference != null) {
            mScreen.removePreference(mRttModePreference);
            mRttModePreference.setOnPreferenceChangeListener(null);
        }
        if (mRttVisibilityPreference != null) {
            mScreen.removePreference(mRttVisibilityPreference);
            mRttVisibilityPreference.setOnPreferenceChangeListener(null);
        }
        if (mRttOperationMode != null) {
            mScreen.removePreference(mRttOperationMode);
            mRttOperationMode.setOnPreferenceChangeListener(null);
        }
        if (mRttCallTypePreference != null) {
            mScreen.removePreference(mRttCallTypePreference);
            mRttCallTypePreference.setOnPreferenceChangeListener(null);
        }
    }

    private void displayRttOption() {
        if (mRttModePreference == null) {
            Log.e(TAG, "displayRttOption PreferenceScreen is invalid");
            return;
        }
        mRttModePreference.setPersistent(true);
        mScreen.addPreference(mRttModePreference);
        mRttModePreference.setOnPreferenceChangeListener(this);

        //Enable RTT settings depending on RTT Config
        mRttModePreference.setChecked(QtiImsExtUtils.isRttOn(getApplicationContext(), mPhoneId));
        Log.d(TAG, "RTT: UI Option = " + mRttModePreference.isChecked());
    }

    private void displayRttOperationOption() {
        if (mScreen == null || mRttOperationMode == null) {
            Log.e(TAG, "displayRttOperationOption Preference screen is invalid");
            return;
        }

        mRttOperationMode.setPersistent(true);
        mScreen.addPreference(mRttOperationMode);
        mRttOperationMode.setOnPreferenceChangeListener(this);

        int value = QtiImsExtUtils.getRttOperatingMode(getApplicationContext(), mPhoneId);
        loadRttOperationModePrefs(value);
        Log.d(TAG, "RttOperation mode: UI Option = " + value);
    }

    private void displayRttCallTypeOption() {
        if (mScreen == null || mRttCallTypePreference == null) {
            Log.e(TAG, "displayRttCallTypeOption Preference screen is invalid");
            return;
        }

        mRttCallTypePreference.setPersistent(true);
        mScreen.addPreference(mRttCallTypePreference);
        mRttCallTypePreference.setOnPreferenceChangeListener(this);

        mRttCallTypePreference.setChecked(!QtiImsExtUtils.canStartRttCall(
                getApplicationContext(), mPhoneId));
        Log.d(TAG, "RttCall type: UI Option = " + mRttCallTypePreference.isChecked());
    }

    private void loadRttOperationModePrefs(int value) {
        Log.d(TAG, "loadRttOperationModePrefs, value = " + value);
        final String rttOpMode = getSummaryText(value);
        mRttOperationMode.setValue(String.valueOf(value));
        mRttOperationMode.setSummary(rttOpMode);
    }

    private String getSummaryText(int rttMode) {
        switch(rttMode) {
            case QtiCallConstants.RTT_UPON_REQUEST_MODE:
                return getString(R.string.ims_rtt_upon_request_mode);
            case QtiCallConstants.RTT_AUTOMATIC_MODE:
                return getString(R.string.ims_rtt_automatic_mode);
            default:
                return getString(R.string.ims_rtt_unknown_mode);
        }
    }

    private void onRttOperationModePreferenceChange(int value) {
        QtiImsExtUtils.setRttOperatingMode(getApplicationContext().getContentResolver(),
                mPhoneId, value);
        loadRttOperationModePrefs(value);
    }

    private void displayRttVisibilityOption() {
        if (mRttVisibilityPreference == null) {
            Log.e(TAG, "displayRttVisibilityOption PreferenceScreen is invalid");
            return;
        }
        mRttVisibilityPreference.setPersistent(true);
        mScreen.addPreference(mRttVisibilityPreference);
        mRttVisibilityPreference.setOnPreferenceChangeListener(this);

        //Enable RTT visibility settings depending on RTT Config
        mRttVisibilityPreference.setChecked(
                QtiImsExtUtils.isRttVisibilityOn(getApplicationContext(), mPhoneId));
        Log.d(TAG, "RTT: Visibility UI Option = " + mRttVisibilityPreference.isChecked());
    }

    // Update the RTT mode in required places
    private void setRttMode(boolean enabling) {
        Log.d(TAG, "RTT: setRttMode value = " + enabling);

        // Update the system setting
        QtiImsExtUtils.setRttMode(enabling, getApplicationContext(), mPhoneId);

        // Update the UI to reflect system setting
        mRttModePreference.setChecked(enabling);

        // Update the NV via config (only if rtt always enabled is false)
        if(!isRttAlwaysEnabled()) {
            try {
                Preconditions.checkNotNull(mProvisionManager);
                mProvisionManager.setProvisioningIntValue(
                        ProvisioningManager.KEY_RTT_ENABLED, enabling ?
                        ProvisioningManager.PROVISIONING_VALUE_ENABLED :
                        ProvisioningManager.PROVISIONING_VALUE_DISABLED);
            } catch (RuntimeException e) {
                Log.e(TAG, "RTT: setRttMode failed. Exception = " + e);
                //Re-set this back due to failure
                mRttModePreference.setChecked(!enabling);
            }
        }
    }

    private boolean isCallStateIdle() {
        for (int i = 0; i < mCallState.length; i++) {
            if (TelephonyManager.CALL_STATE_IDLE != mCallState[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean isRttAlwaysEnabled() {
        if (mConfigManager == null) {
            return false;
        }
        PersistableBundle b = mConfigManager.getConfigForSubId(getSubscriptionId());
        return b != null &&
                b.getBoolean(CarrierConfigManager.KEY_IGNORE_RTT_MODE_SETTING_BOOL);
    }

    private void setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content),
                (v, windowInsets) -> {
                    Insets insets = windowInsets.getInsets(
                            WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime()
                                    | WindowInsetsCompat.Type.displayCutout());
                    // Apply the insets paddings to the view.
                    v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
                    // Return CONSUMED if you don't want the window insets to keep being
                    // passed down to descendant views.
                    return WindowInsetsCompat.CONSUMED;
                });
    }

}
