/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.MsimAdditionalInfoCode;

/**
 * MsimAdditionalCallInfo is used to notify additional call information as part of concurrent
 * calls scenarios.
 */
@VintfStability
parcelable MsimAdditionalCallInfo {
  MsimAdditionalInfoCode additionalCode = MsimAdditionalInfoCode.NONE;
}
