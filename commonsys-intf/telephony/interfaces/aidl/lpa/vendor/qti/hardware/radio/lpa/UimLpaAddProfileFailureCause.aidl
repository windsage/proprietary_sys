/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 */

package vendor.qti.hardware.radio.lpa;

@VintfStability
@Backing(type="int")
enum UimLpaAddProfileFailureCause {
    UIM_LPA_ADD_PROFILE_FAILURE_CAUSE_NONE = 0,
    UIM_LPA_ADD_PROFILE_FAILURE_CAUSE_GENERIC = 1,
    UIM_LPA_ADD_PROFILE_FAILURE_CAUSE_SIM = 2,
    UIM_LPA_ADD_PROFILE_FAILURE_CAUSE_NETWORK = 3,
    UIM_LPA_ADD_PROFILE_FAILURE_CAUSE_MEMORY = 4,
}
