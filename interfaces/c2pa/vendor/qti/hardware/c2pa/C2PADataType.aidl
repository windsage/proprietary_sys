/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.c2pa;

import android.hardware.common.Ashmem;

/**
 * Customized C2PA data type used for input and output parameters.
 */
@VintfStability
union C2PADataType {
   byte byteValue;
   int intValue;
   long longValue;
   double doubleValue;
   String stringValue;
   byte[] arrayValue;
   Ashmem fdValue;
}
