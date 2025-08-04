/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

@VintfStability
@Backing(type="int")
enum StatusType {
    INVALID,
    DISABLED,
    PARTIALLY_ENABLED,
    ENABLED,
    NOT_SUPPORTED,
}
