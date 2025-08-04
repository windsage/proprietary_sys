/*
 * Copyright (c) 2020 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 * Not a Contribution.
 * Apache license notifications and license are retained
 * for attribution purposes only.
 *
 * Copyright (C) 2017-2018 The Android Open Source Project
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
 *
 */

package org.codeaurora.ims;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import android.annotation.IntDef;

import android.telephony.ims.feature.MmTelFeature;
import android.telephony.emergency.EmergencyNumber;
import android.telephony.ims.stub.ImsRegistrationImplBase;
import android.telephony.ims.feature.ImsFeature;


/**
 * This class contains annotations copied here from AOSP to
 * be re-used in order to replace the hidden API usage.
 */
public class ImsAnnotations {

    /**
     * Integer values that define the capability type.
     */
    @IntDef(flag = true,
            value = {
                    MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VOICE,
                    MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VIDEO,
                    MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_UT,
                    MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_SMS
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface MmTelCapability {}

     /**
     * The routing to tell how to handle the call for the corresponding emergency number.
     */
    @IntDef(flag = false, prefix = { "EMERGENCY_CALL_ROUTING_" }, value = {
            EmergencyNumber.EMERGENCY_CALL_ROUTING_UNKNOWN,
            EmergencyNumber.EMERGENCY_CALL_ROUTING_EMERGENCY,
            EmergencyNumber.EMERGENCY_CALL_ROUTING_NORMAL
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface EmergencyCallRouting {}

    /**
     * Defining Emergency Service Category as follows:
     *  - General emergency call, all categories;
     *  - Police;
     *  - Ambulance;
     *  - Fire Brigade;
     *  - Marine Guard;
     *  - Mountain Rescue;
     *  - Manually Initiated eCall (MIeC);
     *  - Automatically Initiated eCall (AIeC);
     *
     * Category UNSPECIFIED (General emergency call, all categories) indicates that no specific
     * services are associated with this emergency number; if the emergency number is specified,
     * it has one or more defined emergency service categories.
     *
     * Reference: 3gpp 22.101, Section 10 - Emergency Calls
     */
    @IntDef(flag = true, prefix = { "EMERGENCY_SERVICE_CATEGORY_" }, value = {
            EmergencyNumber.EMERGENCY_SERVICE_CATEGORY_UNSPECIFIED,
            EmergencyNumber.EMERGENCY_SERVICE_CATEGORY_POLICE,
            EmergencyNumber.EMERGENCY_SERVICE_CATEGORY_AMBULANCE,
            EmergencyNumber.EMERGENCY_SERVICE_CATEGORY_FIRE_BRIGADE,
            EmergencyNumber.EMERGENCY_SERVICE_CATEGORY_MARINE_GUARD,
            EmergencyNumber.EMERGENCY_SERVICE_CATEGORY_MOUNTAIN_RESCUE,
            EmergencyNumber.EMERGENCY_SERVICE_CATEGORY_MIEC,
            EmergencyNumber.EMERGENCY_SERVICE_CATEGORY_AIEC
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface EmergencyServiceCategories {}

    /**
     * Defines the underlying radio technology type that we have registered for IMS over.
     */
    @IntDef(flag = true,
            value = {
                    ImsRegistrationImplBase.REGISTRATION_TECH_NONE,
                    ImsRegistrationImplBase.REGISTRATION_TECH_LTE,
                    ImsRegistrationImplBase.REGISTRATION_TECH_IWLAN
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ImsRegistrationTech {}

    /**
     * Integer values defining the result codes that should be returned from
     * {@link #changeEnabledCapabilities} when the framework tries to set a feature's capability.
     */
    @IntDef(flag = true,
            value = {
                    ImsFeature.CAPABILITY_ERROR_GENERIC,
                    ImsFeature.CAPABILITY_SUCCESS
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ImsCapabilityError {}
}
