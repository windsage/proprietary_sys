/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qti.phone;

import com.qti.extphone.DualDataRecommendation;
import com.qti.extphone.Status;

import vendor.qti.hardware.radio.RadioError;

public class QtiRadioConfigUtils {

    static Status convertHalErrorcode(int rilErrorCode) {
        return new Status((rilErrorCode == 0) ? Status.SUCCESS : Status.FAILURE);
    }

    static CiwlanCapability convertHalCiwlanCapability(int capability, int rilErrorCode) {
        if (rilErrorCode != RadioError.NONE) {
            return new CiwlanCapability(CiwlanCapability.INVALID);
        }

        switch (capability) {
            case vendor.qti.hardware.radio.qtiradioconfig.CiwlanCapability.NONE:
                return new CiwlanCapability(CiwlanCapability.NONE);
            case vendor.qti.hardware.radio.qtiradioconfig.CiwlanCapability.DDS:
                return new CiwlanCapability(CiwlanCapability.DDS);
            case vendor.qti.hardware.radio.qtiradioconfig.CiwlanCapability.BOTH:
                return new CiwlanCapability(CiwlanCapability.BOTH);
            default:
                return new CiwlanCapability(CiwlanCapability.INVALID);
        }
    }

    static DualDataRecommendation convertHalDualDataRecommendation(
            vendor.qti.hardware.radio.qtiradioconfig.DualDataRecommendation rec) {
        return new DualDataRecommendation(rec.sub, rec.action);
    }
}
