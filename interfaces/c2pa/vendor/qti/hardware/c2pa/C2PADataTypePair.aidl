/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.c2pa;

import vendor.qti.hardware.c2pa.C2PADataType;

/**
 * C2PADataTypePair where "key" is of type string and "value" is of type C2PADataType.
 */
@VintfStability
parcelable C2PADataTypePair {
    String key;
    C2PADataType value;
}