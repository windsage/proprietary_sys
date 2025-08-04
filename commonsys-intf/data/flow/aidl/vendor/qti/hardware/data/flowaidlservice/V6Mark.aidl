/*===========================================================================
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
===========================================================================*/

package vendor.qti.hardware.data.flowaidlservice;

import vendor.qti.hardware.data.flowaidlservice.TrafficClass;

/**
 * IPv6 flow marking for traffic class and flow label.
 */
@VintfStability
parcelable V6Mark {
    TrafficClass trafficClass;
    int flowLabel;
}
