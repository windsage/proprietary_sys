/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.qtiradioconfig;

import vendor.qti.hardware.radio.qtiradioconfig.DualDataAction;
import vendor.qti.hardware.radio.qtiradioconfig.DualDataSubscription;

@VintfStability
parcelable DualDataRecommendation {
    /**
     * Subscription to which the recommendation applies.
     * Values DDS = 1, NON_DDS = 2.
     */
    DualDataSubscription sub;

    /**
     * Recommended action applicable to the recommendation sub.
     * Values DATA_DISALLOW = 0, DATA_ALLOW = 1;
     */
    DualDataAction action;
}
