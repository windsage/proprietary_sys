/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 */

package vendor.qti.hardware.radio.lpa;

@VintfStability
@Backing(type="int")
enum UimLpaUserConsentType {
    UIM_LPA_NO_CONFIRMATION_REQD = 0,
    UIM_LPA_SIMPLE_CONFIRMATION_REQD = 1,
    UIM_LPA_STRONG_CONFIRMATION_REQD = 2,
}
