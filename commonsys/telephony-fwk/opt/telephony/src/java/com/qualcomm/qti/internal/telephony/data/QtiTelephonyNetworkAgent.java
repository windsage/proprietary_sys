/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 * Not a Contribution.
 *
 * Copyright (C) 2019 The Android Open Source Project
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

import static android.telephony.ims.stub.ImsRegistrationImplBase.REGISTRATION_TECH_CROSS_SIM;

import android.annotation.NonNull;
import android.content.Context;
import android.net.NetworkAgentConfig;
import android.net.NetworkCapabilities;
import android.net.NetworkProvider;
import android.net.NetworkScore;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import com.android.ims.ImsManager;
import com.android.internal.telephony.data.DataNetwork;
import com.android.internal.telephony.data.PhoneSwitcher;
import com.android.internal.telephony.data.PhoneSwitcher.PhoneSwitcherCallback;
import com.android.internal.telephony.data.TelephonyNetworkAgent;
import com.android.internal.telephony.data.TelephonyNetworkAgent.TelephonyNetworkAgentCallback;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.subscription.SubscriptionManagerService;

import java.time.Duration;

public class QtiTelephonyNetworkAgent extends TelephonyNetworkAgent {
    private static final long NETWORK_LINGER_TIME_NON_DDS = 2000;      // In milliseconds
    private static final long NETWORK_LINGER_TIME_DDS = 30000;         // In milliseconds
    private static final int TEARDOWN_DELAY_TIMEOUT_NON_DDS = 3000;    // In milliseconds
    private static final int TEARDOWN_DELAY_TIMEOUT_NON_DDS_CIWLAN = 5000;    // In milliseconds
    private static final int TEARDOWN_DELAY_TIMEOUT_DDS = 0;           // In milliseconds

    private static final int EVENT_ACTIVE_PHONE_SWITCH = 1;

    private int mScore;
    private final Phone mPhone;
    private PhoneSwitcher mPhoneSwitcher;
    private Handler mInternalHandler;
    private final SubscriptionManagerService mSubscriptionManagerService;
    private PhoneSwitcherCallback mPhoneSwitcherCallback;

    /**
     * Constructor
     *
     * @param phone The phone instance.
     * @param looper The looper to be used by the handler. Currently, the handler thread is the
     * phone process's main thread.
     * @param dataNetwork The data network which owns this network agent.
     * @param score The initial score of the network.
     * @param config The network agent config.
     * @param provider The network provider.
     * @param callback TelephonyNetworkAgent callback.
     */
    public QtiTelephonyNetworkAgent(@NonNull Phone phone, @NonNull Looper looper,
            @NonNull DataNetwork dataNetwork, @NonNull NetworkScore score,
            @NonNull NetworkAgentConfig config, @NonNull NetworkProvider provider,
            @NonNull TelephonyNetworkAgentCallback callback) {
        super(phone, looper, dataNetwork, score, config, provider, callback);

        mLogTag = "QtiTNA-" + mId + "-" + phone.getPhoneId();
        mScore = score.getLegacyInt();
        mPhone = phone;
        mPhoneSwitcher = PhoneSwitcher.getInstance();
        mInternalHandler = new InternalHandler(looper);
        // Register for preferred data changed event
        mPhoneSwitcherCallback = new PhoneSwitcherCallback(mInternalHandler::post) {
            @Override
            public void onPreferredDataPhoneIdChanged(int phoneId) {
                 log("Preferred data sub phone id changed to " + phoneId);
                 Message msg = mInternalHandler.obtainMessage(EVENT_ACTIVE_PHONE_SWITCH);
                 mInternalHandler.sendMessage(msg);
            }
        };
        PhoneSwitcher.getInstance().registerCallback(mPhoneSwitcherCallback);

        mSubscriptionManagerService = SubscriptionManagerService.getInstance();
        log("Constructor");
    }

    private class InternalHandler extends Handler {
        public InternalHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case EVENT_ACTIVE_PHONE_SWITCH:
                    if (mDataNetwork.getNetworkCapabilities().hasCapability(
                            NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                        onActivePhoneSwitch();
                    }
                    break;
                default:
                    loge("Unknown message = " + msg.what);
            }
        }
    }

    private int getPhoneCount() {
        TelephonyManager tm = (TelephonyManager) mPhone.getContext().
                getSystemService(Context.TELEPHONY_SERVICE);
        return tm.getActiveModemCount();
    }

    private boolean isCiwlanRegisteredOnAnySub() {
        for (int phoneId = 0; phoneId < getPhoneCount(); phoneId++) {
            Phone phone = null;
            try {
                phone = PhoneFactory.getPhone(phoneId);
            } catch (IllegalStateException e) {
                loge("isCiwlanRegisteredOnAnySub " + e.getMessage());
            }
            if (phone == null) {
                loge("isCiwlanRegisteredOnAnySub, phone null for phoneId: " + phoneId);
                continue;
            }
            int imsRegTech = ImsManager.getInstance(phone.getContext(), phoneId)
                    .getRegistrationTech();
            log("isCiwlanRegisteredOnAnySub: imsRegTech: " + imsRegTech
                    + " for Phone Id: " + phoneId);
            if (imsRegTech == REGISTRATION_TECH_CROSS_SIM) {
                return true;
            }
        }
        return false;
    }

    private void onActivePhoneSwitch() {
        long lingerTimer;
        int timeoutDelay;
        boolean isPrimary = false;
        int acitveDataSubId = SubscriptionManager.getActiveDataSubscriptionId();
        if (!SubscriptionManager.isValidSubscriptionId(acitveDataSubId)) return;

        if (mPhone.getSubId() == acitveDataSubId) {
            lingerTimer = NETWORK_LINGER_TIME_DDS;
            timeoutDelay = TEARDOWN_DELAY_TIMEOUT_DDS;
            isPrimary = true;
        } else {
            lingerTimer = NETWORK_LINGER_TIME_NON_DDS;
            if (isCiwlanRegisteredOnAnySub()) {
                timeoutDelay = TEARDOWN_DELAY_TIMEOUT_NON_DDS_CIWLAN;
            } else {
                timeoutDelay = TEARDOWN_DELAY_TIMEOUT_NON_DDS;
            }
        }
        setLingerDuration(Duration.ofMillis(lingerTimer));
        setTeardownDelayMillis(timeoutDelay);
        log("onActivePhoneSwitch: setTransportPrimary = " + isPrimary +
                ", setTeardownDelayMillis = " + timeoutDelay);
        sendNetworkScore(new NetworkScore.Builder().setLegacyInt(mScore)
                .setTransportPrimary(isPrimary).build());
    }

    /**
     * Called when connectivity service has indicated they no longer want this network.
     */
    @Override
    public void onNetworkUnwanted() {
        log("onNetworkUnwanted - no-op");
    }

    @Override
    public void onNetworkDestroyed() {
        log("onNetworkDestroyed - tearing down");
        PhoneSwitcher.getInstance().unregisterCallback(mPhoneSwitcherCallback);
        mInternalHandler = null;
        if (mAbandoned) {
            log("The agent is already abandoned. Ignored to tear down");
            return;
        }
        mDataNetwork.tearDown(DataNetwork.TEAR_DOWN_REASON_CONNECTIVITY_SERVICE_UNWANTED);
    }
}
