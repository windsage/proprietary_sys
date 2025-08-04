/******************************************************************************  
Copyright (c) 2023 Qualcomm Technologies, Inc.  
All Rights Reserved.  
Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.latencyaidlservice;

/**
 * Enum values for OOD status.
 */
@VintfStability
@Backing(type="long")
enum OodStatus {
    UNSPECIFIED = 0,
    ENABLED,
    DISABLED,
}
