/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.am;

@VintfStability
@Backing(type="int")
enum AudioError {
    STATUS_OK = 0,
    GENERIC_FAILURE = 1,
    STATUS_SERVER_DIED = 2,
}
