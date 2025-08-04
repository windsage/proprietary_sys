/*===========================================================================
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
===========================================================================*/

package vendor.qti.hardware.data.flowaidlservice;

/**
 * Return values for IUplinkPriorityService requests.
 */
@VintfStability
@Backing(type="long")
enum StatusCode {
    OK,
    INVALID_ARGUMENTS,
    INVALID_STATE,
    CALLBACK_NOT_SET,
    UNKNOWN_ERROR,
}
