/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 */

package vendor.qti.hardware.radio.lpa;

@VintfStability
@Backing(type="int")
enum UimLpaResult {
    UIM_LPA_RESULT_SUCCESS = 0,
    UIM_LPA_RESULT_FAILURE = 1,
    UIM_LPA_RESULT_CONFRM_CODE_MISSING = 2,
}
