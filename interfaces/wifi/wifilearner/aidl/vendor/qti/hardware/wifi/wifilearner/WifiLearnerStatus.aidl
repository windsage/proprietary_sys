/**
 * Copyright (c) 2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.wifi.wifilearner;

import vendor.qti.hardware.wifi.wifilearner.WifiLearnerStatusCode;

@VintfStability
parcelable WifiLearnerStatus {
    /**
     * Status code when application queries for peer statistics.
     */
    WifiLearnerStatusCode code;
    /**
     * Additional debug message for more information if any.
     */
    String debugMessage;
}
