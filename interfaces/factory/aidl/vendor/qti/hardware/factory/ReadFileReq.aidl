/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

// FIXME: license file, or use the -l option to generate the files with the header.

package vendor.qti.hardware.factory;

/**
 * READ FILE REQUEST
 */
@VintfStability
parcelable ReadFileReq {
    int offset;
    char max_size;
    String file_name;
}
