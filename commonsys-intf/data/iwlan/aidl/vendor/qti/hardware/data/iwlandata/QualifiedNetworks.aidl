/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.hardware.data.iwlandata;

import android.hardware.radio.data.ApnTypes;

@VintfStability
parcelable QualifiedNetworks {
    ApnTypes apnType;
    int[] networks;
}
