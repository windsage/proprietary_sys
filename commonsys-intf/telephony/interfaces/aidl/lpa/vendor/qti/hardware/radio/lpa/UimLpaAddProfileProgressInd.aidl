/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 */

package vendor.qti.hardware.radio.lpa;

import vendor.qti.hardware.radio.lpa.UimLpaAddProfileFailureCause;
import vendor.qti.hardware.radio.lpa.UimLpaAddProfileStatus;
import vendor.qti.hardware.radio.lpa.UimLpaProfilePolicyMask;
import vendor.qti.hardware.radio.lpa.UimLpaUserConsentType;

@VintfStability
@JavaDerive(toString=true)
parcelable UimLpaAddProfileProgressInd {

    /**
     *
     * LPA Profile Status
     */
    UimLpaAddProfileStatus status;

    /**
     *
     * LPA AddProfile Failure Cause
     */
    UimLpaAddProfileFailureCause cause;

    /**
     *
     * Add Profile Progress
     */
    int progress;

    /**
     *
     * LPA Profile Policy Mask
     */
    UimLpaProfilePolicyMask policyMask;

    /**
     *
     * userConsentRequired information
     */
    boolean userConsentRequired;

    /**
     *
     * profile Name
     */
    String profileName;

    /**
     *
     * Lpa UserConsentType
     */
    UimLpaUserConsentType user_consent_type;
}
