/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
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
import android.os.Looper;
import android.os.Message;
import android.telephony.CarrierConfigManager;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.data.DataConfigManager;
import com.android.internal.telephony.flags.FeatureFlags;
import com.qualcomm.qti.internal.telephony.QtiPhoneUtils;

public class QtiDataConfigManager extends DataConfigManager {
    /** Event for carrier config changed derived from its parent class*/
    private static final int EVENT_CARRIER_CONFIG_CHANGED = 1;

    protected String[] mApnsWithSameGID;
    protected boolean mIsApnFilteringRequired;
    private boolean mIsSmartDdsSwitchFeatureAvailable = true;  // default to true

    /**
     * Constructor
     *
     * @param phone The phone instance.
     * @param looper The looper to be used by the handler. Currently the handler thread is the
     * phone process's main thread.
     */
    public QtiDataConfigManager(@NonNull Phone phone, @NonNull Looper looper,
            @NonNull FeatureFlags featureFlags) {
        super(phone, looper, featureFlags);
        log("QtiDataConfigManager: constructor");
        registerListenerForQtiPhoneReady();
    }

    /**
     * Register a listener with QtiPhoneUtils to know when it gets connected to ExtTelephonyService
     */
    void registerListenerForQtiPhoneReady() {
        log("registerListenerForQtiPhoneReady");
        QtiPhoneUtils.addOnQtiPhoneReadyListener(this::onQtiPhoneReady);
    }

    /**
     * Called when QtiPhoneUtils is created and it has connected to ExtTelephonyService
     */
    private void onQtiPhoneReady() {
        // QtiPhoneUtils is now up.
        log("onQtiPhoneReady");
        try {
            mIsSmartDdsSwitchFeatureAvailable =
                    QtiPhoneUtils.getInstance().isSmartDdsSwitchFeatureAvailable();
        } catch (RuntimeException ex) {
            loge("onQtiPhoneReady: Error connecting to qti phone.");
        }
    }

    public void onCarrierConfigLoadedForEssentialRecords() {
        log("QtiDCM onCarrierConfigLoadedForEssentialRecords");
        sendEmptyMessage(EVENT_CARRIER_CONFIG_CHANGED);
    }

    /**
     * This can be overridden by vendors classes to load other configs.
     */
    protected void updateOtherConfigs() {
        mApnsWithSameGID = mCarrierConfig.getStringArray(
                CarrierConfigManager.KEY_MULTI_APN_ARRAY_FOR_SAME_GID);
        mIsApnFilteringRequired = mCarrierConfig.getBoolean(
                CarrierConfigManager.KEY_REQUIRE_APN_FILTERING_WITH_RADIO_CAPABILITY);
    }

    public boolean isApnFilteringWithRadioCapabilityRequired() {
        return mIsApnFilteringRequired;
    }

    public String[] getApnsWithSameGID() {
        return mApnsWithSameGID;
    }

    /**
     * @return {@code true} if carrier specific PDP reject configuration
     * is enabled.
     */
    public boolean isPdpRejectConfigEnabled() {
        return mResources.getBoolean(com.android.internal.R.bool
                .config_pdp_reject_enable_retry);
    }

    /**
     * @return The PDP reject retry delay in milliseconds.
     */
    public long getPdpRejectRetryDelay() {
        return mResources.getInteger(
                com.android.internal.R.integer.config_pdp_reject_retry_delay_ms);
    }

    /**
     * @return The PDP reject dialog title string.
     */
    public @NonNull String getPdpRejectDialogTitle() {
        return mResources.getString(com.android.internal.R.string.config_pdp_reject_dialog_title);
    }

    /**
     * @return The PDP reject cause user authentication string.
     */
    public @NonNull String getPdpRejectCauseUserAuthentication() {
        return mResources.getString(com.android.internal.R.
                string.config_pdp_reject_user_authentication_failed);
    }

    /**
     * @return The PDP reject cause service option not
     * subscribed string.
     */
    public @NonNull String getPdpRejectCauseServiceNotSubscribed() {
        return mResources.getString(com.android.internal.R.
                string.config_pdp_reject_service_not_subscribed);
    }

    /**
     * @return The PDP reject cause multiple connections to same
     * pdn not allowed string.
     */
    public @NonNull String getPdpRejectCauseSamePdnNotAllowed() {
        return mResources.getString(com.android.internal.R.
                string.config_pdp_reject_multi_conn_to_same_pdn_not_allowed);
    }

    public boolean isSmartDdsSwitchFeatureAvailable() {
        return mIsSmartDdsSwitchFeatureAvailable;
    }

    /**
     * @return Time threshold in ms to define a internet connection status to be stable
     * (e.g. out of service, in service, wifi is the default active network.etc), while -1 indicates
     * auto switch feature disabled.
     */
    @Override
    public long getAutoDataSwitchAvailabilityStabilityTimeThreshold() {
        if (mIsSmartDdsSwitchFeatureAvailable) return -1;
        return super.getAutoDataSwitchAvailabilityStabilityTimeThreshold();
    }
}
