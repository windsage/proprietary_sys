/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.hardware.data.iwlandata;
import android.hardware.radio.RadioError;

@VintfStability
parcelable IWlanResponseInfo {
    int serial;
    RadioError error;
}
