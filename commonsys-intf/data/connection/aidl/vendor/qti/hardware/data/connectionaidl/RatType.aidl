/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.hardware.data.connectionaidl;

@VintfStability
@Backing(type="int")
enum RatType {
    UNSPECIFIED = 0,
    RAT_4G = 1 << 0,
    RAT_5G = 1 << 1,
}
