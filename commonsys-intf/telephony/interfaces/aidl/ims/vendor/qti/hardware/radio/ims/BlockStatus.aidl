/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.BlockReasonDetails;
import vendor.qti.hardware.radio.ims.BlockReasonType;

/**
 * BlockStatus to indicate registration block status.
 * Telephony will process BlockStatus only if BlockStatus#blockReason is not
 *     BlockReasonType#INVALID
 */
@VintfStability
parcelable BlockStatus {
    BlockReasonType blockReason = BlockReasonType.INVALID;
    BlockReasonDetails blockReasonDetails;
}

