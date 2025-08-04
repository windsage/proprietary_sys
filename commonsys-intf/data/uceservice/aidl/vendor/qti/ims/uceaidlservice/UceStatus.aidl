/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.ims.uceaidlservice;

import vendor.qti.ims.uceaidlservice.UceStatusCode;

@VintfStability
parcelable UceStatus {
    UceStatusCode status;
    String description;
}
