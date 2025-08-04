/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.hardware.data.iwlandata;

import android.hardware.radio.network.RegState;

@VintfStability
parcelable IWlanDataRegStateResult {
    RegState regState;
    int reasonForDenial;
}
