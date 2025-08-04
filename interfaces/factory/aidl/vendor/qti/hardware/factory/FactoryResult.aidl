/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

// FIXME: license file, or use the -l option to generate the files with the header.

package vendor.qti.hardware.factory;

import vendor.qti.hardware.factory.IResultType;

/**
 * FTM Commnand Result.
 */
@VintfStability
parcelable FactoryResult {
    IResultType result;
    String data;
}
