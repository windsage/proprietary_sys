/******************************************************************************  
Copyright (c) 2023 Qualcomm Technologies, Inc.  
All Rights Reserved.  
Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.latencyaidlservice;

/**
 * Enum describing SIM slot Id.
 */
@VintfStability
@Backing(type="long")
enum SlotId {
    SLOT_UNSPECIFIED = -1,
    SLOT_FIRST = 0,
    SLOT_SECOND = 1,
    SLOT_THIRD = 2,
}
