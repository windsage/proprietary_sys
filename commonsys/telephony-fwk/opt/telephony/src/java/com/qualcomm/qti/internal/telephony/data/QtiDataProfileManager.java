/*
 * Copyright (c) 2022-2023 Qualcomm Technologies, Inc.
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
import android.telephony.data.ApnSetting;
import com.android.internal.telephony.data.DataConfigManager;
import com.android.internal.telephony.data.DataNetworkController;
import com.android.internal.telephony.data.DataProfileManager;
import com.android.internal.telephony.data.DataProfileManager.DataProfileManagerCallback;
import com.android.internal.telephony.data.DataServiceManager;
import com.android.internal.telephony.flags.FeatureFlags;
import com.android.internal.telephony.Phone;

import org.codeaurora.telephony.utils.EnhancedRadioCapabilityResponse;

import java.util.ArrayList;

public class QtiDataProfileManager extends DataProfileManager {

    private static final int GID = 0;
    private static final int APN_TYPE = 1;
    private static final int DEVICE_CAPABILITY = 2;
    private static final int APN_NAME = 3;
    private static final int KEY_MULTI_APN_ARRAY_FOR_SAME_GID_ENTRY_LENGTH = 4;

    private QtiDataConfigManager mQtiDataConfigManager;
    private QtiDataNetworkController mQtiDataNetworkController;

    /**
     * Constructor
     *
     * @param phone The phone instance.
     * @param dataNetworkController Data network controller.
     * @param dataServiceManager WWAN data service manager.
     * @param looper The looper to be used by the handler. Currently the handler thread is the
     * phone process's main thread.
     * @param featureFlags Feature flags controlling which feature is enabled.
     * @param callback Data profile manager callback.
     */
    public QtiDataProfileManager(@NonNull Phone phone,
            @NonNull DataNetworkController dataNetworkController,
            @NonNull DataServiceManager dataServiceManager, @NonNull Looper looper,
            @NonNull FeatureFlags featureFlags,
            @NonNull DataProfileManager.DataProfileManagerCallback callback) {
        super(phone, dataNetworkController, dataServiceManager, looper, featureFlags, callback);
        log("QtiDataProfileManager: constructor");
        mQtiDataNetworkController = (QtiDataNetworkController) mDataNetworkController;
        mQtiDataConfigManager = (QtiDataConfigManager) mDataConfigManager;
    }

    /**
     * Filters out multipe apns based on radio capability if the APN's GID value is listed in
     * CarrierConfigManager#KEY_MULTI_APN_ARRAY_FOR_SAME_GID as per the operator requirement.
     */
    @Override
    protected void filterApnSettingsWithRadioCapability(ArrayList<ApnSetting> allApnSettings) {
        if (!mQtiDataConfigManager.isApnFilteringWithRadioCapabilityRequired()) return;

        log("filterApnSettingsWithRadioCapability start: allApnSettings: " + allApnSettings);
        int i = 0;
        while (i < allApnSettings.size()) {
            ApnSetting apn = allApnSettings.get(i);
            String apnType = ApnSetting.getApnTypesStringFromBitmask(apn.getApnTypeBitmask());
            if (apn.hasMvnoParams() && (apn.getMvnoType() == ApnSetting.MVNO_TYPE_GID) &&
                    isApnFilteringRequired(apn.getMvnoMatchData(), apnType)) {
                String apnName = getApnBasedOnRadioCapability(apn.getMvnoMatchData(),
                        apnType, mQtiDataNetworkController.getEnhancedRadioCapabilityResponse()
                        .getEnhancedRadioCapability());
                if (apnName != null && !apnName.equals(apn.getApnName())) {
                    allApnSettings.remove(i);
                    log("filterApnSettingsWithRadioCapability: removed not supported apn:" + apn);
                } else {
                    i++;
                }
            } else {
                i++;
            }
        }
        log("filterApnSettingsWithRadioCapability: end: allApnSettings:" + allApnSettings);
    }

     /**
     * Check if APN filtering is required
     *
     * @param gid gid value of the apn.
     * @param apnType  apn type.
     * @return True if multipe apns present in CarrierConfigManager#KEY_MULTI_APN_ARRAY_FOR_SAME_GID
               for this gid .
     */
    private boolean isApnFilteringRequired(String gid, String apnType) {
        final String[] apnConfig = mQtiDataConfigManager.getApnsWithSameGID();
        for (String apnEntry: apnConfig) {
            String[] split = apnEntry.split(":");
            if (split.length == KEY_MULTI_APN_ARRAY_FOR_SAME_GID_ENTRY_LENGTH) {
                if (gid.equals(split[GID]) && apnType.equals(split[APN_TYPE])) {
                    return true;
                }
            }
        }

        return false;
    }

     /**
     * Return apn name based on the device capability if the corresponding
     * entry present in CarrierConfigManager#KEY_MULTI_APN_ARRAY_FOR_SAME_GID
     *
     * @param gid gid value of the apn.
     * @param apnType  apn type.
     * @param deviceCapability  device capability, Ex: SA, NSA, LTE etc..
     * @return apn name from the entry matches with all the above three params.
     */
    private String getApnBasedOnRadioCapability(String gid, String apnType,
            String deviceCapability) {
        if (deviceCapability == null) {
            loge("getApnBasedOnRadioCapability: deviceCapability is null");
            return null;
        }
        final String[] apnConfig = mQtiDataConfigManager.getApnsWithSameGID();
        for (String apnEntry: apnConfig) {
            String[] split = apnEntry.split(":");
            if (split.length == KEY_MULTI_APN_ARRAY_FOR_SAME_GID_ENTRY_LENGTH &&
                    deviceCapability != null) {
                if (gid.equals(split[GID]) && apnType.equals(split[APN_TYPE]) &&
                        deviceCapability.equals(split[DEVICE_CAPABILITY])) {
                    return split[APN_NAME];
                }
            }
        }
        return null;
    }
}