/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
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

import android.annotation.NonNull;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Looper;
import android.telephony.Rlog;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.UiccSlotInfo;
import android.util.Log;

import com.android.internal.telephony.flags.FeatureFlags;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.RIL;
import com.android.internal.telephony.subscription.SubscriptionManagerService;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppState;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.PersoSubState;
import com.android.internal.telephony.uicc.IccCardStatus.PinState;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;


import java.util.Arrays;

public class QtiSubscriptionManagerService extends SubscriptionManagerService {
    static final private String LOG_TAG = "QtiSubscriptionManagerService";

    private static final String PROPERTY_SUBSIDY_DEVICE  = "persist.vendor.radio.subsidydevice";

    private static QtiSubscriptionManagerService sInstance;
    private static Context mContext;
    private boolean mNotifyAddSubInfo = true;

    public static QtiSubscriptionManagerService init(Context c, Looper looper,
            @NonNull FeatureFlags featureFlags) {
        synchronized (QtiSubscriptionManagerService.class) {
            if (sInstance == null) {
                sInstance = new QtiSubscriptionManagerService(c, looper, featureFlags);
            } else {
                Log.wtf(LOG_TAG, "init() called multiple times! sInstance = " + sInstance);
            }
            return (QtiSubscriptionManagerService)sInstance;
        }
    }

    public static QtiSubscriptionManagerService getInstance() {
        if (sInstance == null) {
            Log.wtf(LOG_TAG, "getInstance null");
        }

        return (QtiSubscriptionManagerService)sInstance;
    }

    private QtiSubscriptionManagerService(Context c, Looper looper,
            @NonNull FeatureFlags featureFlags) {
        super(c, looper, featureFlags);
        mContext = c;
        logd("init by context");
    }

    private void enforcePermissions(String message, String ...permissions) {
        for (String permission : permissions) {
            if (mContext.checkCallingOrSelfPermission(permission) ==
                    PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }
        throw new SecurityException(message + ". Does not have permissions for "
                + Arrays.toString(permissions));
    }

    @Override
    public void setUiccApplicationsEnabled(boolean enabled, int subId) {
        logd("[setUiccApplicationsEnabled]+ enabled: " + enabled + " subId: " + subId);

        Phone phone = PhoneFactory.getPhone(SubscriptionManager.getPhoneId(subId));

        if (phone != null && phone.getHalVersion().greaterOrEqual(RIL.RADIO_HAL_VERSION_1_5)) {
            logd("setUiccApplicationsEnabled HAL is 1.5 or above");
            super.setUiccApplicationsEnabled(enabled, subId);
        } else {
            enforcePermissions("setUiccApplicationsEnabled",
                    android.Manifest.permission.MODIFY_PHONE_STATE);

            long identity = Binder.clearCallingIdentity();
            try {
                int slotId = getSlotIdForSubId(subId);

                if (enabled) {
                    logd("setUiccApplicationsEnabled: using legacy api activateUiccCard");
                    QtiUiccCardProvisioner.getInstance().activateUiccCard(slotId);
                } else {
                    logd("setUiccApplicationsEnabled: uisng legacy api deactivateUiccCard");
                    QtiUiccCardProvisioner.getInstance().deactivateUiccCard(slotId);
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    private int getSlotIdForSubId(int subId) {
        SubscriptionManager subMgr = mContext.getSystemService(SubscriptionManager.class);
        if (subMgr == null) {
            return SubscriptionManager.INVALID_SIM_SLOT_INDEX;
        }

        if (subMgr.isActiveSubscriptionId(subId)) {
            return getSlotIndex(subId);
        }
        return getSlotIdForDeactivatedSub(subId);
    }

    private int getSlotIdForDeactivatedSub(int subId) {
        SubscriptionInfo subInfo = getSubscriptionInfo(subId);

        TelephonyManager telephonyMgr = mContext.getSystemService(TelephonyManager.class);
        UiccSlotInfo[] slotsInfo = telephonyMgr.getUiccSlotsInfo();
        if (slotsInfo == null) {
            return SubscriptionManager.INVALID_SIM_SLOT_INDEX;
        }

        for (int index = 0; index < slotsInfo.length; index++) {
            UiccSlotInfo slotInfo = slotsInfo[index];
            if (subInfo.getIccId().equals(slotInfo.getCardId())) {
                logd("getSlotIdForDeactivatedSub: slotId found: " + index);
                return index;
            }
        }
        return SubscriptionManager.INVALID_SIM_SLOT_INDEX;
    }

    @Override
    public void updateSubscription(int phoneId) {
        super.updateSubscription(phoneId);
        Phone phone = PhoneFactory.getPhone(phoneId);
        if (phone != null && phone.getHalVersion().less(RIL.RADIO_HAL_VERSION_1_5)) {
            QtiUiccCardProvisionerHelper.getInstance().updateUserPreferences();
        }
    }

    @Override
    public boolean areUiccAppsEnabledOnCard(int phoneId) {
        Phone phone = PhoneFactory.getPhone(phoneId);
        if (phone == null || phone.getHalVersion().greaterOrEqual(RIL.RADIO_HAL_VERSION_1_5)) {
            return super.areUiccAppsEnabledOnCard(phoneId);
        }

        TelephonyManager telMgr = mContext.getSystemService(TelephonyManager.class);
        if (QtiPhoneUtils.getInstance() == null || telMgr == null) {
            Rlog.d(LOG_TAG, "QtiPhoneUtils or TelephonyManager instance is null");
            return true;
        }

        return (QtiPhoneUtils.getInstance().getCurrentUiccCardProvisioningStatus(phoneId)
                == QtiUiccCardProvisioner.UiccProvisionStatus.PROVISIONED
                && telMgr.getSimState(phoneId) == TelephonyManager.SIM_STATE_READY);
    }

    @Override
    public int addSubInfo(String uniqueId, String displayName, int slotIndex,
            int subscriptionType) {
        int retValue = super.addSubInfo(uniqueId, displayName, slotIndex, subscriptionType);
        int phoneCount = TelephonyManager.getDefault().getActiveModemCount();

        if (phoneCount > 1 && mNotifyAddSubInfo) {
            for (int phoneId = 0; phoneId < phoneCount; phoneId++) {
                try {
                    QtiGsmCdmaPhone phone = (QtiGsmCdmaPhone)PhoneFactory.getPhone(phoneId);
                    phone.notifySubInfoAdded();
                } catch (ClassCastException e) {
                    loge("addSubInfo, exception " + e);
                    continue;
                }
            }
            mNotifyAddSubInfo = false;
        }
        return retValue;
    }

    private boolean isSimLocked(Phone phone) {
        int family = (phone.getPhoneType() == PhoneConstants.PHONE_TYPE_GSM) ?
                UiccController.APP_FAM_3GPP : UiccController.APP_FAM_3GPP2;
        UiccCardApplication uiccCardApp =
                UiccController.getInstance().getUiccCardApplication(phone.getPhoneId(), family);
        if (uiccCardApp == null) return false;

        AppState appState = uiccCardApp.getState();

        return (uiccCardApp.getPin1State() == PinState.PINSTATE_ENABLED_PERM_BLOCKED
                || appState == AppState.APPSTATE_PIN
                || appState == AppState.APPSTATE_PUK
                || (appState == AppState.APPSTATE_SUBSCRIPTION_PERSO &&
                PersoSubState.isPersoLocked(uiccCardApp.getPersoSubState())));
    }

    @Override
    public boolean isSubIdCreationPending() {
        boolean subsidyDevicePropVal = false;

        try {
            subsidyDevicePropVal = QtiPhoneUtils.getInstance().
                    getPropertyValueBool(PROPERTY_SUBSIDY_DEVICE, false);
        } catch (RuntimeException e) {
            loge("isSubIdCreationPending, exception" + e);
        }

        if (subsidyDevicePropVal) {
            Phone[] phones = PhoneFactory.getPhones();
            for (Phone phone : phones) {
                SubscriptionInfo subInfo = getSubscriptionInfo(phone.getSubId());
                if (isSimLocked(phone) && (subInfo != null && !subInfo.isActive())) {
                    logi("SubId Creation is Pending for slot : " + phone.getPhoneId());
                    return true;
                }
            }
        }
        logd("isSubIdCreationPending()..." + subsidyDevicePropVal);
        return false;
    }

    @Override
    public boolean isDsdsToSsConfigEnabled() {
        boolean dsdsToSsConfigStatus = false;
        try {
            dsdsToSsConfigStatus = QtiPhoneUtils.getInstance().
                    isDsdsToSsConfigEnabled();
        } catch (RuntimeException e) {
            loge("isDsdsToSsConfigEnabled, exception " + e);
        }
        return dsdsToSsConfigStatus;
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
