/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.RegFailureReasonType;

/**
 * BlockReasonDetails to indicate registration block reason.
 * Telephony will process BlockReasonDetails based on valid values of
 *     BlockReasonDetails#regFailureReasonType and BlockReasonDetails#regFailureReason.
 */
@VintfStability
parcelable BlockReasonDetails {
    RegFailureReasonType regFailureReasonType = RegFailureReasonType.INVALID;
    int regFailureReason;
}

