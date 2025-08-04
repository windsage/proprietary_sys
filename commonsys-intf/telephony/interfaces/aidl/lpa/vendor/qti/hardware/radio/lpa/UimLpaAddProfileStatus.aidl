/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 */

package vendor.qti.hardware.radio.lpa;

@VintfStability
@Backing(type="int")
enum UimLpaAddProfileStatus {
    UIM_LPA_ADD_PROFILE_STATUS_NONE = 0,
    UIM_LPA_ADD_PROFILE_STATUS_ERROR = 1,
    UIM_LPA_ADD_PROFILE_STATUS_DOWNLOAD_PROGRESS = 2,
    UIM_LPA_ADD_PROFILE_STATUS_INSTALLATION_PROGRESS = 3,
    UIM_LPA_ADD_PROFILE_STATUS_INSTALLATION_COMPLETE = 4,
    UIM_LPA_ADD_PROFILE_STATUS_GET_USER_CONSENT = 5,
    UIM_LPA_ADD_PROFILE_STATUS_SEND_CONF_CODE_REQ = 6,
}
