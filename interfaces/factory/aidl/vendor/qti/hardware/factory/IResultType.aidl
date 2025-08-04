/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

// FIXME: license file, or use the -l option to generate the files with the header.

package vendor.qti.hardware.factory;

import vendor.qti.hardware.factory.IResultType;

/**
 * List of Result types supported.
 */
@VintfStability
@Backing(type="int")
enum IResultType {
    FAILED = -1,
    SUCCESS = 0,
    ERROR = 1,
    OPEN_ERROR = 2,
    NOT_EXIST_ERR = 3,
    WRONG_PARAMS = 4,
}
