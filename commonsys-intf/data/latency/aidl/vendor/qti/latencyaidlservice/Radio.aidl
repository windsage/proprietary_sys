/******************************************************************************  
Copyright (c) 2023 Qualcomm Technologies, Inc.  
All Rights Reserved.  
Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.latencyaidlservice;

/**
 * Enum describing radio types
 */
@VintfStability
@Backing(type="long")
enum Radio {
    WWAN,
    WLAN,
}
