/*===========================================================================

  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.

===========================================================================*/

package vendor.qti.hardware.data.lceaidlservice;

/**
 * Return values for ILatencyService requests.
 */
@VintfStability
@Backing(type="long")
enum StatusCode {
    OK,
    INVALID_ARGUMENTS,
    INVALID_STATE,
    REPORTING_CRITERIA_NOT_SET,
    REPORTING_CRITERIA_TYPE_MISMATCH,
    UNKNOWN_ERROR,
}
