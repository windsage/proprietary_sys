/*===========================================================================
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
===========================================================================*/

package vendor.qti.hardware.data.flowaidlservice;

/**
 * IPv4 type of service value and mask.
 */
@VintfStability
parcelable Tos {
    byte val;
    byte mask;
}
