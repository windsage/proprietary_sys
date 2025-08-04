/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.MultiIdentityLineType;
import vendor.qti.hardware.radio.ims.MultiIdentityRegistrationStatus;

/**
 * MultiIdentityLineInfo is used to support MultiIdentity line registration.
 * Lower layers will process MultiIdentityLineInfo only if msisdn is not empty.
 */
@VintfStability
parcelable MultiIdentityLineInfo {
    String msisdn;
    MultiIdentityRegistrationStatus registrationStatus = MultiIdentityRegistrationStatus.UNKNOWN;
    MultiIdentityLineType lineType = MultiIdentityLineType.UNKNOWN;
}
