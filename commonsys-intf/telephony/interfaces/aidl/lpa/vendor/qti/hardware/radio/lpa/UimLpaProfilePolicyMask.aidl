/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 */

package vendor.qti.hardware.radio.lpa;

@VintfStability
@Backing(type="int")
enum UimLpaProfilePolicyMask {
    UIM_LPA_PROFILE_TYPE_DISABLE_NOT_ALLOWED = 1,
    UIM_LPA_PROFILE_TYPE_DELETE_NOT_ALLOWED = 2,
    UIM_LPA_PROFILE_TYPE_DELETE_ON_DISABLE = 4,
}
