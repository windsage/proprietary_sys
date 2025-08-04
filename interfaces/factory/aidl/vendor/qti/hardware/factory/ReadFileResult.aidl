/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

// FIXME: license file, or use the -l option to generate the files with the header.

package vendor.qti.hardware.factory;

import vendor.qti.hardware.factory.IResultType;

/**
 * READ FILE RESULT
 */
@VintfStability
parcelable ReadFileResult {
    IResultType result_type;
    int file_size;
    int offset;
    char size;
    byte[] data;
}
