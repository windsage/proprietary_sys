/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.hardware.data.connectionaidl;

import vendor.qti.hardware.data.connectionaidl.RatType;

@VintfStability
parcelable BearerInfo {
    int bearerId;
    RatType uplink;
    RatType downlink;
}
