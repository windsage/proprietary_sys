/******************************************************************************  
Copyright (c) 2023 Qualcomm Technologies, Inc.  
All Rights Reserved.  
Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.latencyaidlservice;

/**
 * IPv6 traffic class struct for value and mask.
 */
@VintfStability
parcelable TrafficClass {
    byte val;
    byte mask;
}
