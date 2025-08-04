/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.AddressInfo;
import vendor.qti.hardware.radio.ims.BlockStatus;

/**
 * RegistrationBlockStatusInfo to indicate registration block status.
 * Telephony will process RegistrationBlockStatusInfo if BlockStatus#blockReason is not
 *     BlockReasonType#INVALID
 */
@VintfStability
parcelable RegistrationBlockStatusInfo {
    BlockStatus blockStatusOnWwan;
    BlockStatus blockStatusOnWlan;
}
