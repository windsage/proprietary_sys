/******************************************************************************  
Copyright (c) 2023 Qualcomm Technologies, Inc.  
All Rights Reserved.  
Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.latencyaidlservice;

/**
 * Return values for ILatencyService requests
 */
@VintfStability
@Backing(type="long")
enum StatusCode {
    OK,
    INVALID_ARGUMENTS,
    SET_ERROR,
    INVALID_STATE,
    CALLBACK_NOT_SET,
}
