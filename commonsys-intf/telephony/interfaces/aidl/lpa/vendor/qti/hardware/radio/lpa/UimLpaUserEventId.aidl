/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 */

package vendor.qti.hardware.radio.lpa;

@VintfStability
@Backing(type="int")
enum UimLpaUserEventId {
    UIM_LPA_UNKNOWN_EVENT_ID = 0,
    UIM_LPA_ADD_PROFILE = 1,
    UIM_LPA_ENABLE_PROFILE = 2,
    UIM_LPA_DISABLE_PROFILE = 3,
    UIM_LPA_DELETE_PROFILE = 4,
    UIM_LPA_EUICC_MEMORY_RESET = 5,
    UIM_LPA_GET_PROFILE = 6,
    UIM_LPA_UPDATE_NICKNAME = 7,
    UIM_LPA_GET_EID = 8,
    UIM_LPA_USER_CONSENT = 9,
    UIM_LPA_SRV_ADDR_OPERATION = 10,
    UIM_LPA_CONFIRM_CODE = 11,
    UIM_LPA_EUICC_INFO2 = 12,
}
