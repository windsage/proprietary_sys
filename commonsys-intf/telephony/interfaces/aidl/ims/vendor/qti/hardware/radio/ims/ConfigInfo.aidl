/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.ConfigFailureCause;
import vendor.qti.hardware.radio.ims.ConfigItem;

/**
 * Data structure to store configInfo item and related details.
 * Lower layer will process ConfigInfo if ConfigInfo#ConfigItem is not
 * ConfigItem#INVALID.
 */
@VintfStability
parcelable ConfigInfo {
    ConfigItem item = ConfigItem.INVALID;
    boolean hasBoolValue;
    boolean boolValue;
    int intValue;
    String stringValue;
    ConfigFailureCause errorCause = ConfigFailureCause.INVALID;
}