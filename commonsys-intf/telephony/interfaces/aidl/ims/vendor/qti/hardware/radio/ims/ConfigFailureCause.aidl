/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

@VintfStability
@Backing(type="int")
enum ConfigFailureCause {
    INVALID,
    NO_ERR,
    IMS_NOT_READY,
    FILE_NOT_AVAILABLE,
    READ_FAILED,
    WRITE_FAILED,
    OTHER_INTERNAL_ERR,
}
