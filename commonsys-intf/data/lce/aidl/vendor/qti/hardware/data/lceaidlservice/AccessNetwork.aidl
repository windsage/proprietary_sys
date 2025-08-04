/*===========================================================================

  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.

===========================================================================*/

package vendor.qti.hardware.data.lceaidlservice;

/**
 * Access network for which the api from ILceService should apply to.
 */
@VintfStability
@Backing(type="int")
enum AccessNetwork {
    NONE = 0,
    WCDMA = 1,
    LTE = 2,
    NR5G = 3,
}
