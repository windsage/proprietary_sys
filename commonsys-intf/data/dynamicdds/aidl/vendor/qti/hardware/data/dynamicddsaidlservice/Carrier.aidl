/*===========================================================================

  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.

===========================================================================*/

package vendor.qti.hardware.data.dynamicddsaidlservice;

@VintfStability
parcelable Carrier {
    String iin;
    /**
     * preference for specified carrier, range from 1 to 10
     *  with 1 being the higest preference
     */
    byte preference;
}
