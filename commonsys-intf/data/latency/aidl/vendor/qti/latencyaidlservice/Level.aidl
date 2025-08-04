/******************************************************************************  
Copyright (c) 2023 Qualcomm Technologies, Inc.  
All Rights Reserved.  
Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.latencyaidlservice;

/**
 * Enum which holds the latency levels
 * As the L* value increases, the latency will decrease.
 * the higher Level has the lowest latency.
 */
@VintfStability
@Backing(type="long")
enum Level {
    L1 = 1,
    L2 = 2,
    L3 = 3,
    L4 = 4,
    L5 = 5,
    L6 = 6,
}
