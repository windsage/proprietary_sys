/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

@VintfStability
@Backing(type="int")
enum HandoverType {
    INVALID,
    START,
    COMPLETE_SUCCESS,
    COMPLETE_FAIL,
    CANCEL,
    NOT_TRIGGERED,
    NOT_TRIGGERED_MOBILE_DATA_OFF,
}
