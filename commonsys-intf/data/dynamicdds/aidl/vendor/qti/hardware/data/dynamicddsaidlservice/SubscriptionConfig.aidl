/*===========================================================================

  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.

===========================================================================*/

package vendor.qti.hardware.data.dynamicddsaidlservice;

import vendor.qti.hardware.data.dynamicddsaidlservice.Carrier;

@VintfStability
parcelable SubscriptionConfig {
    /**
     * package name of application
     */
    String appName;
    Carrier[] carriers;
    /**
     * priority of the application, range from 1 to 10
     *  with 1 being the highest priority
     */
    byte priority;
}
