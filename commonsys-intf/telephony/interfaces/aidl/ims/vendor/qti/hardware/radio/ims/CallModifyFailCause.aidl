/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

@VintfStability
@Backing(type="int")
enum CallModifyFailCause {
    E_INVALID,
    E_SUCCESS,
    E_RADIO_NOT_AVAILABLE,
    E_GENERIC_FAILURE,
    E_REQUEST_NOT_SUPPORTED,
    E_CANCELLED,
    E_UNUSED,
    E_INVALID_PARAMETER,
    E_REJECTED_BY_REMOTE,
    E_IMS_DEREGISTERED,
    E_NETWORK_NOT_SUPPORTED,
    E_HOLD_RESUME_FAILED,
    E_HOLD_RESUME_CANCELED,
    E_REINVITE_COLLISION,
}
