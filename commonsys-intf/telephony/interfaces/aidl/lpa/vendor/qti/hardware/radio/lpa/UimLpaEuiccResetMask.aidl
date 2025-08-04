/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 */

package vendor.qti.hardware.radio.lpa;

@VintfStability
@Backing(type="int")
enum UimLpaEuiccResetMask {
    UIM_LPA_EUICC_RESET_TEST_PROFILES = 1,
    UIM_LPA_EUICC_RESET_OPERATIONAL_PROFILES = 2,
    UIM_LPA_EUICC_RESET_TO_DEFAULT_SMDP_ADDRESS = 4,
}
