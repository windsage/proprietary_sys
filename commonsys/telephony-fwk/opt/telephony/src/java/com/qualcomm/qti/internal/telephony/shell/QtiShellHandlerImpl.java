/*
 * Copyright (c) 2023-2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.internal.telephony.shell;

import android.content.Context;
import android.os.RemoteException;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import com.qti.extphone.Client;
import com.qti.extphone.ExtPhoneCallbackListener;
import com.qti.extphone.ExtTelephonyManager;
import com.qti.extphone.ServiceCallback;
import com.qti.extphone.Status;
import com.qti.extphone.Token;

import java.util.List;

public class QtiShellHandlerImpl implements ShellHandlerDelegate {
    private static final String LOG_TAG = "QtiShellHandlerImpl";
    private static final String DUAL_DATA_PREFERENCE = "dual_data_preference";
    private static final String SMART_DDS_SWITCH = "smart_dds_switch";

    private static final int SETTING_VALUE_OFF = 0;
    private static final int SETTING_VALUE_ON = 1;

    private Context mContext;
    private ExtTelephonyManager mExtTelephonyManager;
    private Client mClient;
    private TelephonyManager mTelephonyManager;
    private SubscriptionManager mSubscriptionManager;

    private ExtPhoneCallbackListener mExtPhoneListener = new ExtPhoneCallbackListener() {
        @Override
        public void onDualDataCapabilityChanged(Token token, Status status, boolean support)
                throws RemoteException {
            log("onDualDataCapabilityChanged support: " + support);
        }

        @Override
        public void setDualDataUserPreferenceResponse(Token token, Status status) {
            log("setDualDataUserPreferenceResponse status: " + status);
        }

        @Override
        public void setSmartDdsSwitchToggleResponse(Token token, boolean result)
                throws RemoteException {
            log("setSmartDdsSwitchToggleResponse result: " + result);
        }
    };

    private ServiceCallback mServiceCallback = new ServiceCallback() {
         @Override
         public void onConnected() {
             log("Ext service is connected");
             final int[] events = new int[] {
                     ExtPhoneCallbackListener.EVENT_ON_DUAL_DATA_CAPABILITY_CHANGED,
                     ExtPhoneCallbackListener.EVENT_SET_DUAL_DATA_USER_PREFERENCE_RESPONSE,
                     ExtPhoneCallbackListener.EVENT_SET_SMART_DDS_SWITCH_TOGGLE_RESPONSE};
             mClient = mExtTelephonyManager.registerCallbackWithEvents(
                     mContext.getPackageName(), mExtPhoneListener, events);
             if (mClient == null) {
                 log("client is null");
             }
         }

         @Override
         public void onDisconnected() {
             log("Ext service is disconnected");
             mClient = null;
         }
    };

    public QtiShellHandlerImpl(Context context) {
        mContext = context;
        mExtTelephonyManager = ExtTelephonyManager.getInstance(context);
        mExtTelephonyManager.connectService(mServiceCallback);
        mTelephonyManager = getTelephonyManager();
        mSubscriptionManager = getSubscriptionManager();
        log("constructor done");
    }

    private TelephonyManager getTelephonyManager() {
        if (mTelephonyManager == null) {
            mTelephonyManager = mContext.getSystemService(TelephonyManager.class);
        }
        return mTelephonyManager;
    }

    private SubscriptionManager getSubscriptionManager() {
        if (mSubscriptionManager == null) {
            mSubscriptionManager = mContext.getSystemService(SubscriptionManager.class);
        }
        return mSubscriptionManager;
    }

    @Override
    public String getDualDataEnableState() {
        log("getDualDataEnableState");
        if (isDualDataCapable()) {
            final int value = getSettingsValue(DUAL_DATA_PREFERENCE);
            return String.valueOf(value);

        }
        return String.valueOf(SETTING_VALUE_OFF);
    }

    @Override
    public boolean enableDualData() {
        log("enableDualData");
        return performDualDataToggle(true);
    }

    @Override
    public boolean disableDualData() {
        log("disableDualData");
        return performDualDataToggle(false);
    }

    private boolean isDualDataCapable() {
        return mExtTelephonyManager != null && mExtTelephonyManager.getDualDataCapability();
    }

    private boolean performDualDataToggle(boolean enable) {
        if (isDualDataCapable()) {
            try {
                mExtTelephonyManager.setDualDataUserPreference(mClient, enable);
                updateSettingsDb(DUAL_DATA_PREFERENCE, enable);
                return true;
            } catch (RemoteException e) {
                log("performDualData failed with an exception");
            }
        }
        return false;
    }

    @Override
    public String getTempDdsEnableState() {
        if (mContext != null) {
            TelephonyManager telephonyManager = getTelephonyManager();
            SubscriptionManager subscriptionManager = getSubscriptionManager();
            final int nddsSubId = getNonDdsSubId(subscriptionManager);
            if (SubscriptionManager.isUsableSubscriptionId(nddsSubId)) {
                final boolean isEnabled = telephonyManager.createForSubscriptionId(nddsSubId)
                        .isMobileDataPolicyEnabled(TelephonyManager
                        .MOBILE_DATA_POLICY_DATA_ON_NON_DEFAULT_DURING_VOICE_CALL);
                log("getTempDdsEnableState nDDS SUB ID " + nddsSubId + " enable " + isEnabled);
                return String.valueOf(isEnabled);
            }
        }
        log("getTempDdsEnableState false");
        return "false";
    }

    @Override
    public boolean enableTempDdsSwitch() {
        log("enableTempDdsSwitch");
        return performTempDdsToggle(true);
    }

    @Override
    public boolean disableTempDdsSwitch() {
        log("disableTempDdsSwitch");
        return performTempDdsToggle(false);
    }

    @Override
    public String getSmartPermDdsEnableState() {
        log("getSmartPermDdsEnableState");
        if (isSmartPermDdsAvailable()) {
            final int value = getSettingsValue(SMART_DDS_SWITCH);
            return String.valueOf(value);
        }
        return String.valueOf(0);
    }

    @Override
    public boolean enableSmartPermDdsSwitch() {
        log("enableSmartPermDdsSwitch");
        return performSmartPermDdsToggle(true);
    }

    @Override
    public  boolean disableSmartPermDdsSwitch() {
        log("disableSmartPermDdsSwitch");
        return performSmartPermDdsToggle(false);
    }

    private int getNonDdsSubId(SubscriptionManager subscriptionManager) {
        if (subscriptionManager != null) {
            final int defaultDataSub = SubscriptionManager.getDefaultDataSubscriptionId();
            if (!SubscriptionManager.isUsableSubscriptionId(defaultDataSub)) {
                return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
            }
            List<SubscriptionInfo> subInfoList =
                    subscriptionManager.getActiveSubscriptionInfoList();
            if (subInfoList != null) {
                for (SubscriptionInfo subInfo : subInfoList) {
                    if (subInfo.getSubscriptionId() != defaultDataSub) {
                        return subInfo.getSubscriptionId();
                    }
                }
            }
        }
        return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    }

    private boolean performTempDdsToggle(boolean enable) {
        if (mContext != null) {
            TelephonyManager telephonyManager = getTelephonyManager();
            SubscriptionManager subscriptionManager = getSubscriptionManager();
            final int nddsSubId = getNonDdsSubId(subscriptionManager);
            if (SubscriptionManager.isUsableSubscriptionId(nddsSubId)) {
                telephonyManager.createForSubscriptionId(nddsSubId)
                        .setMobileDataPolicyEnabled(TelephonyManager
                        .MOBILE_DATA_POLICY_DATA_ON_NON_DEFAULT_DURING_VOICE_CALL, enable);
                log("performTempDdsEnablement nDDS SUB ID " + nddsSubId + " enable " + enable);
                return true;
            }
        }
        return false;
    }

    private boolean isSmartPermDdsAvailable() {
        try {
            return mExtTelephonyManager.isSmartDdsSwitchFeatureAvailable();
        } catch (RemoteException e) {
            log("isSmartPermDdsAvailable failed with an exception");
        }
        return false;
    }

    private boolean performSmartPermDdsToggle(boolean enable) {
        if (isSmartPermDdsAvailable() && canSmartPermDdsBePerformed()) {
            try {
                mExtTelephonyManager.setSmartDdsSwitchToggle(enable, mClient);
                updateSettingsDb(SMART_DDS_SWITCH, enable);
                return true;
            } catch (RemoteException e) {
                log("performSmartPermDdsEnablement failed with an exception");
            }
        }
        return false;
    }

    private void updateSettingsDb(String name, boolean enable) {
        Settings.Global.putInt(mContext.getContentResolver(), name,
                enable ? SETTING_VALUE_ON : SETTING_VALUE_OFF);
    }

    private int getSettingsValue(String name) {
        return Settings.Global.getInt(mContext.getContentResolver(), name, SETTING_VALUE_OFF);
    }

    private boolean canSmartPermDdsBePerformed() {
        // To align with Qti Settings, check if APM is on.
        if (Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0) {
            return false;
        }

        return getSubscriptionManager() != null ?
                getSubscriptionManager().getActiveSubscriptionInfoCount() > 1
                : false;
    }

    private void log(String str) {
        android.util.Log.d(LOG_TAG, str);
    }
}
