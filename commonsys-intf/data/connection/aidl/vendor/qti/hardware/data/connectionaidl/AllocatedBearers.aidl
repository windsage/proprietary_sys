/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.hardware.data.connectionaidl;

import vendor.qti.hardware.data.connectionaidl.BearerInfo;

@VintfStability
parcelable AllocatedBearers {
    int cid;
    String apn;
    /**
     * List of allocated bearers for this call
     */
    BearerInfo[] bearers;
}
