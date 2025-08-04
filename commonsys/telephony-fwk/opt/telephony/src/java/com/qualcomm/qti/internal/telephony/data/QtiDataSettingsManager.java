/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 * Not a Contribution.
 *
 * Copyright (C) 2021 The Android Open Source Project
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

package com.qualcomm.qti.internal.telephony.data;

import android.annotation.NonNull;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.data.ApnSetting;
import android.telephony.data.ApnSetting.ApnType;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.os.Looper;
import android.os.Message;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.data.DataSettingsManager;
import com.android.internal.telephony.data.DataNetworkController;
import com.android.internal.telephony.data.PhoneSwitcher;
import com.android.internal.telephony.data.PhoneSwitcher.PhoneSwitcherCallback;
import com.android.internal.telephony.data.TelephonyNetworkProvider;
import com.android.internal.telephony.flags.FeatureFlags;
import com.android.telephony.Rlog;

import com.qti.extphone.DualDataRecommendation;

public class QtiDataSettingsManager extends DataSettingsManager {

    // Redefines events from the parent class
    private static final int EVENT_SUBSCRIPTIONS_CHANGED = 4;
    private static final int EVENT_INITIALIZE = 11;

    // Defines private events using to itself
    private static final int EVENT_ACTIVE_PHONE_SWITCH = 100;
    private static final int EVENT_DEFAULT_DATA_SUBSCRIPTIONS_CHANGED = 12;

    private int mSubId;
    private int mPhoneId;
    private String mLogTag;
    private QtiDataNetworkController mQtiDataNetworkController;

    @NonNull
    private final FeatureFlags mFeatureFlags;

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            log("mBroadcastReceiver: action= " + action);
            if (action.equals(TelephonyManager.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED)) {
                if (!SubscriptionManager.isValidSubscriptionId(mSubId)) {
                    log("SUBID is invalid");
                    return;
                }
                if (mSubId != SubscriptionManager.getDefaultDataSubscriptionId()) {
                    log("SUBID is nDDS sub");
                    sendMessage(obtainMessage(EVENT_DEFAULT_DATA_SUBSCRIPTIONS_CHANGED));
                }
            }
        }
    };

    /**
     * Constructor
     *
     * @param phone The phone instance.
     * @param dataNetworkController Data network controller.
     * @param looper The looper to be used by the handler. Currently the handler thread is the
     * phone process's main thread.
     * @param callback Data settings manager callback.
     */
    public QtiDataSettingsManager(@NonNull Phone phone,
            @NonNull DataNetworkController dataNetworkController,
            @NonNull FeatureFlags featureFlags, @NonNull Looper looper,
            @NonNull DataSettingsManagerCallback callback) {
        super(phone, dataNetworkController, featureFlags, looper, callback);
        mFeatureFlags = featureFlags;
        mLogTag = "QtiDSMGR-" + phone.getPhoneId();
        mQtiDataNetworkController = (QtiDataNetworkController) dataNetworkController;
        mSubId = phone.getSubId();
        mPhoneId = phone.getPhoneId();
        IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyManager.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED);
        phone.getContext().registerReceiver(mBroadcastReceiver, filter);
        log("Constructor ends");
    }

    @Override
    public void handleMessage(Message msg) {
        log("handleMessage: " + msg.what);
        switch (msg.what) {
            case EVENT_INITIALIZE:
                super.handleMessage(msg);
                onInit();
                break;
            case EVENT_SUBSCRIPTIONS_CHANGED:
                super.handleMessage(msg);
                final int subId = (int) msg.obj;
                if (mSubId != subId) {
                    mSubId = subId;
                    sendDataDuringCall(SmartTempDdsSwitchController.REASON_SUBSCRIPTION_CHANGED);
                }
                break;
            case EVENT_ACTIVE_PHONE_SWITCH:
                sendDataDuringCall(SmartTempDdsSwitchController.REASON_ACTIVE_PHONE_SWITCH);
                // Suppose temporary DDS switch is triggered after voice call is active,
                // the call state changed event won't drive data enabled state update
                // until call ends. In this case, need to peer into whether data enabled
                // state is overridden.
                if (mPhone.getState() != PhoneConstants.State.IDLE) {
                    updateDataEnabledAndNotify(TelephonyManager.DATA_ENABLED_REASON_OVERRIDE);
                }
                break;
            case EVENT_DEFAULT_DATA_SUBSCRIPTIONS_CHANGED:
                log("EVENT_DEFAULT_DATA_SUBSCRIPTIONS_CHANGED");
                // The Active Data subId is not the primary DDS sub suggesting temporary DDS switch
                // has been already triggered, so update overall data state of the Temp DDS sub.
                if (mSubId == PhoneSwitcher.getInstance().getActiveDataSubId()
                        && mPhone.getState() != PhoneConstants.State.IDLE) {
                    log("Update overall data enabled state of the Temporary DDS");
                    updateDataEnabledAndNotify(TelephonyManager.DATA_ENABLED_REASON_OVERRIDE);
                }
                break;
            default:
                super.handleMessage(msg);
        }
    }

    private void onInit() {
        log("onInit...");
        // Register for preferred data changed event
        PhoneSwitcher.getInstance().registerCallback(new PhoneSwitcherCallback(this::post) {
            @Override
            public void onPreferredDataPhoneIdChanged(int phoneId) {
                log("Preferred data sub phone id changed to " + phoneId);
                sendMessage(obtainMessage(EVENT_ACTIVE_PHONE_SWITCH));
            }
        });
    }

    private void sendDataDuringCall(String reason) {
        if (!SubscriptionManager.isValidSubscriptionId(mSubId)) {
            log("sendDataDuringCall SUB ID is invalid");
            return;
        }
        if (mQtiDataNetworkController == null) {
            log("sendDataDuringCall mQtiDataNetworkController is null");
            return;
        }
        log("sendDataDuringCall mPhoneId = " + mPhoneId + " + mSubId = " + mSubId);
        mQtiDataNetworkController.evaluateAndSendDataDuringVoiceCallInfo(mPhoneId, reason);
    }

    @Override
    public boolean isDataEnabled(@ApnType int apnType) {
        // If temp dds switch has happened, consider isDataAllowedInVoiceCall only, otherwise,
        // consider isDataAllowedOverriddenByPlatform prior to isDataAllowedInVoiceCall.
        return super.isDataEnabled(apnType)
                && (hasTempDdsSwitched() ? isDataAllowedInVoiceCall(apnType)
                : (isDataAllowedOverriddenByPlatform(apnType)
                || isDataAllowedInVoiceCall(apnType)));
    }

    private boolean hasTempDdsSwitched() {
        final PhoneSwitcher phoneSwitcher = PhoneSwitcher.getInstance();
        if (phoneSwitcher != null && phoneSwitcher instanceof QtiPhoneSwitcher) {
            QtiPhoneSwitcher qtiPhoneSwitcher = (QtiPhoneSwitcher) phoneSwitcher;
            final boolean hasTempDdsSwitched = qtiPhoneSwitcher.hasTempDdsSwitched();
            log("hasTempDdsSwitched = " + hasTempDdsSwitched);
            return hasTempDdsSwitched;
        }
        return false;
    }

    /**
     * Check if the platform data is allowed by APN
     *
     * When it is a internet APN, need to check if allowed for nDDS SUB additionally, otherwise,
     * it is allowed always.
     */
    private boolean isDataAllowedOverriddenByPlatform(@ApnType int apnType) {
        if (!isInternetApn(apnType)) {
            // MMS APN and others are allowed on nDDS SUB.
            return true;
        }

        // If APN is capable of internet, check if it is allowed by platform.
        // For DDS, internet data is allowed by default if there is no temp DDS switch
        if (mSubId == SubscriptionManager.getDefaultDataSubscriptionId()
                && mPhoneId == PhoneSwitcher.getInstance().getPreferredDataPhoneId()) {
            return true;
        }

        boolean isInternetDataAllowed = false;
        final TelephonyNetworkProvider networkProvider = PhoneFactory.getNetworkProvider();
        if (networkProvider != null
                && networkProvider instanceof QtiTelephonyNetworkProvider) {
            QtiTelephonyNetworkProvider qtiNetworkProvider =
                    (QtiTelephonyNetworkProvider) networkProvider;
            DualDataRecommendation recommendation = qtiNetworkProvider
                    .getDualDataRecommendation();

            // For Non-DDS, we check if the modem allows internet data.
            if (recommendation != null) {
                isInternetDataAllowed =
                        (recommendation.getRecommendedSub() == DualDataRecommendation.NON_DDS)
                        && (recommendation.getAction()
                        == DualDataRecommendation.ACTION_DATA_ALLOW);
            }
            log("isDataAllowedOverriddenByPlatform from QtiTelephonyNetworkProvider = "
                    + isInternetDataAllowed);
        }

        return isInternetDataAllowed;
    }

    /**
     * Check if internet data is allowed for data during call or auto DDS switch
     *
     * When internet data isn't allowed on nDDS, need to check if it is allowed for data during call
     * or auto DDS switch.
     */
    private boolean isDataAllowedInVoiceCall(@ApnType int apnType) {
        // No matter whether nDDS mobile is off or on, allowing internet data in voice call is
        // determined by data during call or auto dds switch status for eliminating gaps where data
        // during call or auto dds option is out of control when mobile data is on.
        if (isInternetApn(apnType)
                && mSubId != SubscriptionManager.getDefaultDataSubscriptionId()
                && mPhone.getState() != PhoneConstants.State.IDLE) {
            final boolean isDataAllowedInVoiceCall =
                    (isMobileDataPolicyEnabled(TelephonyManager
                    .MOBILE_DATA_POLICY_DATA_ON_NON_DEFAULT_DURING_VOICE_CALL)
                    || isMobileDataPolicyEnabled(TelephonyManager
                    .MOBILE_DATA_POLICY_AUTO_DATA_SWITCH));
            log("isDataAllowedInVoiceCall = " + isDataAllowedInVoiceCall);
            return isDataAllowedInVoiceCall;
        }
        // Data is always allowed on the DDS SUB regardless of call state.
        return true;
    }

    // DNC just takes of the highest APN from capabilities, so to eliminate the impact of the
    // overall data state, check if it is an internet APN exactly.
    private boolean isInternetApn(@ApnType int apnType) {
        return apnType == ApnSetting.TYPE_DEFAULT;
    }

    private void log(@NonNull String str) {
        Rlog.d(mLogTag, str);
    }
}
