/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

// FIXME: license file, or use the -l option to generate the files with the header.

package vendor.qti.hardware.factory;

/**
 * WRITE FILE REQUEST
 */
@VintfStability
parcelable WriteFileReq {
    byte append_data;
    char i_size;
    String data;
    String file_name;
}
