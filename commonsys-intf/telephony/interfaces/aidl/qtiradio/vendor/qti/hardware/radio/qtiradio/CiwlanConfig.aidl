/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.qtiradio;

import vendor.qti.hardware.radio.qtiradio.CiwlanMode;

@VintfStability
@JavaDerive(toString = true)
parcelable CiwlanConfig {
    CiwlanMode homeMode;
    CiwlanMode roamMode;
}