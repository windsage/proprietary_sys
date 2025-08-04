/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.VerificationStatus;

@VintfStability
parcelable VerstatInfo {
    boolean canMarkUnwantedCall;
    VerificationStatus verificationStatus = VerificationStatus.VALIDATION_NONE;
}

