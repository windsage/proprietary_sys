/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 * Not a Contribution
 *
 * Copyright (C) 2014 The Android Open Source Project
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


package com.qualcomm.qti.internal.telephony;

import android.content.Context;
import android.os.RemoteException;
import android.provider.Settings;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telephony.Rlog;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telecom.TelecomManager;
import android.telephony.TelephonyRegistryManager;
import android.util.Log;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;

import java.util.Iterator;
import java.util.List;

/*
* This class is mainly to update the user preferences once subinfo
* records are available on legacy targets which uses IRadio HAL 1.4
*/
public class QtiUiccCardProvisionerHelper {
    static final String LOG_TAG = "QtiUiccCardProvisionerHelper";

    private static final int PROVISIONED = 1;
    private static final int NOT_PROVISIONED = 0;

    private static final String APM_SIM_NOT_PWDN_PROPERTY = "persist.vendor.radio.apm_sim_not_pwdn";
    private static final String SETTING_USER_PREF_DATA_SUB = "user_preferred_data_sub";

    private Context mContext;
    private static QtiUiccCardProvisionerHelper sInstance = null;
    private int sNumPhones;
    private TelecomManager mTelecomManager;
    private TelephonyManager mTelephonyManager;

    private static final int DEFAULT_PHONE_INDEX = 0;

    public static QtiUiccCardProvisionerHelper init(Context c) {
        synchronized (QtiUiccCardProvisionerHelper.class) {
            if (sInstance == null) {
                sInstance = new QtiUiccCardProvisionerHelper(c);
            } else {
                Log.wtf(LOG_TAG, "init() called multiple times!  sInstance = " + sInstance);
            }
            return (QtiUiccCardProvisionerHelper)sInstance;
        }
    }

    public static QtiUiccCardProvisionerHelper getInstance() {
        if (sInstance == null) {
           Log.wtf(LOG_TAG, "getInstance null");
        }

        return (QtiUiccCardProvisionerHelper)sInstance;
    }

    private QtiUiccCardProvisionerHelper(Context c) {
        mContext = c;
        mTelecomManager = (TelecomManager) mContext.getSystemService(Context.TELECOM_SERVICE);
        mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        sNumPhones = mTelephonyManager.getActiveModemCount();
        logd(" init by Context");
    }

    private boolean isRadioInValidState() {
        int simNotPwrDown = 0;
        try {
            simNotPwrDown = QtiTelephonyComponentFactory.getInstance().getRil(DEFAULT_PHONE_INDEX).
                    getPropertyValueInt(APM_SIM_NOT_PWDN_PROPERTY, 0);
        } catch (RemoteException|NullPointerException ex) {
            loge("Exception: " + ex);
        }
        boolean isApmSimNotPwrDown = (simNotPwrDown == 1);
        int isAPMOn = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0);

        // Do not updateUserPrefs, SIM is pwdn due to APM, as apm_sim_not_pwdn flag is not set.
        if ((isAPMOn == 1) && (!isApmSimNotPwrDown)) {
            logd("isRadioInValidState, isApmSimNotPwrDown = " + isApmSimNotPwrDown
                    + ", isAPMOn:" + isAPMOn);
            return false;
        }

        // Radio Unavailable, do not updateUserPrefs. As this may happened due to SSR or RIL Crash.
        if (!isRadioAvailableOnAllSubs()) {
            logd(" isRadioInValidState, radio not available");
            return false;
        }

        //Do not updateUserPrefs when Shutdown is in progress
        if (isShuttingDown()) {
            logd(" isRadioInValidState: device shutdown in progress ");
            return false;
        }
        return true;
    }

    private boolean isRadioAvailableOnAllSubs() {
        for (int i = 0; i < sNumPhones; i++) {
            if (PhoneFactory.getPhone(i).mCi != null && PhoneFactory.getPhone(i).mCi.getRadioState()
                    == TelephonyManager.RADIO_POWER_UNAVAILABLE) {
                return false;
            }
        }
        return true;
    }

    private boolean isShuttingDown() {
        for (int i = 0; i < sNumPhones; i++) {
            if (PhoneFactory.getPhone(i) != null &&
                    PhoneFactory.getPhone(i).isShuttingDown()) return true;
        }
        return false;
    }

    private boolean isNonSimAccountFound() {
        final Iterator<PhoneAccountHandle> phoneAccounts =
                mTelecomManager.getCallCapablePhoneAccounts().listIterator();

        while (phoneAccounts.hasNext()) {
            final PhoneAccountHandle phoneAccountHandle = phoneAccounts.next();
            final PhoneAccount phoneAccount = mTelecomManager.getPhoneAccount(phoneAccountHandle);
            if (mTelephonyManager.getSubIdForPhoneAccount(phoneAccount) ==
                    SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                logi("Other than SIM account found. ");
                return true;
            }
        }
        logi("Other than SIM account not found ");
        return false;
    }

    private PhoneAccountHandle subscriptionIdToPhoneAccountHandle(final int subId) {
        final Iterator<PhoneAccountHandle> phoneAccounts =
                mTelecomManager.getCallCapablePhoneAccounts().listIterator();

        while (phoneAccounts.hasNext()) {
            final PhoneAccountHandle phoneAccountHandle = phoneAccounts.next();
            final PhoneAccount phoneAccount = mTelecomManager.getPhoneAccount(phoneAccountHandle);
            if (subId == mTelephonyManager.getSubIdForPhoneAccount(phoneAccount)) {
                return phoneAccountHandle;
            }
        }

        return null;
    }

    synchronized void updateUserPreferences() {
        logi("updateUserPreferences");
        QtiSubscriptionManagerService qtiSubMgrSer = QtiSubscriptionManagerService.getInstance();
        if (qtiSubMgrSer == null || !qtiSubMgrSer.areAllSubscriptionsLoaded()) {
            logd("Subscriptions not yet loaded");
            return;
        }

        SubscriptionInfo mNextActivatedSub = null;
        int activeCount = 0;
        if (!isRadioInValidState()) {
            logd("Radio is in Invalid state, Ignore Updating User Preference!!!");
            return;
        }

        SubscriptionManager subMgr = (SubscriptionManager)
                mContext.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        List<SubscriptionInfo> sil = subMgr.getActiveSubscriptionInfoList();
        // If list of active subscriptions empty OR non of the SIM provisioned
        // clear defaults preference of voice/sms/data.
        if (sil == null || sil.size() < 1) {
            logi("updateUserPreferences: Subscription list is empty");
            return;
        }

        final int defaultVoiceSubId = SubscriptionManager.getDefaultVoiceSubscriptionId();
        final int defaultDataSubId = SubscriptionManager.getDefaultDataSubscriptionId();
        final int defaultSmsSubId = SubscriptionManager.getDefaultSmsSubscriptionId();

        //Get num of activated Subs and next available activated sub info.
        for (SubscriptionInfo subInfo : sil) {
            if (isSubProvisioned(SubscriptionManager.getSubscriptionId(
                    subInfo.getSimSlotIndex()))) {
                activeCount++;
                if (mNextActivatedSub == null) mNextActivatedSub = subInfo;
            }
        }
        logd("updateUserPreferences:: active sub count = " + activeCount + " dds = "
                + defaultDataSubId + " voice = " + defaultVoiceSubId +
                " sms = " + defaultSmsSubId);

        // in Single SIM case or if there are no activated subs available, no need to update. EXIT.
        if ((mNextActivatedSub == null) || (subMgr.getActiveSubscriptionInfoCountMax() == 1)) {
            return;
        }

        handleDataPreference(mNextActivatedSub.getSubscriptionId());

        if ((defaultSmsSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID || activeCount == 1)
                && !isSubProvisioned(defaultSmsSubId) && !qtiSubMgrSer.isSubIdCreationPending()) {
            subMgr.setDefaultSmsSubId(mNextActivatedSub.getSubscriptionId());
        }

        if ((defaultVoiceSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID || activeCount == 1)
                && !isSubProvisioned(defaultVoiceSubId) && !qtiSubMgrSer.isSubIdCreationPending()) {
            subMgr.setDefaultVoiceSubscriptionId(mNextActivatedSub.getSubscriptionId());
        }

        // voice preference is handled in such a way that
        // 1. Whenever current Sub is deactivated or removed It fall backs to
        //    next available Sub.
        // 2. When device is flashed for the first time, initial voice preference
        //    would be set to always ask.
        if (!isNonSimAccountFound() && activeCount == 1 && !qtiSubMgrSer.isSubIdCreationPending()) {
            final int subId = mNextActivatedSub.getSubscriptionId();
            PhoneAccountHandle phoneAccountHandle = subscriptionIdToPhoneAccountHandle(subId);
            logi("set default phoneaccount to  " + subId);
            mTelecomManager.setUserSelectedOutgoingPhoneAccount(phoneAccountHandle);
        }

        qtiSubMgrSer.updateDefaultSubId();

        TelephonyRegistryManager telephonyRegistryManager =
                mContext.getSystemService(TelephonyRegistryManager.class);
        if (telephonyRegistryManager != null) {
            telephonyRegistryManager.notifySubscriptionInfoChanged();
        }
        logd("updateUserPreferences: after currentDds = " +
                SubscriptionManager.getDefaultDataSubscriptionId() + " voice = " +
                SubscriptionManager.getDefaultVoiceSubscriptionId() + " sms = " +
                SubscriptionManager.getDefaultSmsSubscriptionId());
    }

    private void handleDataPreference(int nextActiveSubId) {
        SubscriptionManager subMgr = (SubscriptionManager)
                mContext.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        List<SubscriptionInfo> subInfoList = subMgr.getActiveSubscriptionInfoList();
        if (subInfoList == null) {
            return;
        }
        logd(" handleDataPreference ");

        int userPrefDataSubId = getUserPrefDataSubIdFromDB();
        int currentDataSubId = SubscriptionManager.getDefaultDataSubscriptionId();
        boolean userPrefSubValid = false;

        for (SubscriptionInfo subInfo : subInfoList) {
            if (subInfo.getSubscriptionId() == userPrefDataSubId) {
                userPrefSubValid = true;
            }
        }
        logd("havePrefSub = " + userPrefSubValid + " user pref subId = "
                + userPrefDataSubId + " current dds " + currentDataSubId
                + " next active subId " + nextActiveSubId);

        // If earlier user selected DDS is now available, set that as DDS subId.
        if (userPrefSubValid && isSubProvisioned(userPrefDataSubId) &&
                (currentDataSubId != userPrefDataSubId)) {
            subMgr.setDefaultDataSubId(userPrefDataSubId);
        } else if (!isSubProvisioned(currentDataSubId)) {
            subMgr.setDefaultDataSubId(nextActiveSubId);
        }

        // Check and set DDS after sub activation
        QtiUiccCardProvisioner uiccCardProvisioner = QtiUiccCardProvisioner.getInstance();
        uiccCardProvisioner.setDdsIfRequired();
    }

    protected int getUserPrefDataSubIdFromDB() {
        return android.provider.Settings.Global.getInt(mContext.getContentResolver(),
                SETTING_USER_PREF_DATA_SUB, SubscriptionManager.INVALID_SUBSCRIPTION_ID);
    }

    private int getUiccProvisionStatus(int slotId) {
        QtiPhoneUtils qtiPhoneUtils = QtiPhoneUtils.getInstance();
        if (qtiPhoneUtils != null) {
            return qtiPhoneUtils.getCurrentUiccCardProvisioningStatus(slotId);
        } else {
            return NOT_PROVISIONED;
        }
    }

    // This method returns true if subId and corresponding slotId is in valid
    // range and the Uicc card corresponds to this slot is provisioned.
    private boolean isSubProvisioned(int subId) {
        boolean isSubIdUsable = SubscriptionManager.isUsableSubIdValue(subId);

        if (isSubIdUsable) {
            int slotId = SubscriptionManager.getSlotIndex(subId);
            if (!SubscriptionManager.isValidSlotIndex(slotId)) {
                loge(" Invalid slotId " + slotId + " or subId = " + subId);
                isSubIdUsable = false;
            } else {
                if (getUiccProvisionStatus(slotId) != PROVISIONED) {
                    isSubIdUsable = false;
                }
                loge("isSubProvisioned, state = " + isSubIdUsable + " subId = " + subId);
            }
        }
        return isSubIdUsable;
    }

    private void logd(String string) {
        Rlog.d(LOG_TAG, string);
    }

    private void logi(String string) {
        Rlog.i(LOG_TAG, string);
    }

    private void loge(String string) {
        Rlog.e(LOG_TAG, string);
    }
}
