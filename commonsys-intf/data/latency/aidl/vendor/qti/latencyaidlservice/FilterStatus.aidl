/******************************************************************************  
Copyright (c) 2023 Qualcomm Technologies, Inc.  
All Rights Reserved.  
Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.latencyaidlservice;

/**
 * Possible Filter Status values.
 */
@VintfStability
@Backing(type="long")
enum FilterStatus {
    FILTER_INACTIVE,
    FILTER_ACTIVE,
    FILTER_INACTIVITY_TIMEOUT,
    FILTER_DELETED,
    ERROR_DUPLICATE_FILTER,
    ERROR_FILTER_LIMIT_REACHED,
    ERROR_INTERNAL,
    ERROR_INVALID_ARGS,
    ERROR_IP_TYPE_MISMATCH,
    ERROR_INACTIVITY_TIMEOUT_INVALID,
    ERROR_DST_PORT_INVALID,
    ERROR_SRC_PORT_INVALID,
    ERROR_DST_IP_INVALID,
    ERROR_SRC_IP_INVALID,
}
