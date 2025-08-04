/*===========================================================================

  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.

===========================================================================*/

package vendor.qti.hardware.data.dynamicddsaidlservice;

/**
 * Replied status.
 */
@VintfStability
@Backing(type="int")
enum StatusCode {
    OK = 0,
    INVALID_ARGUMENTS = 1,
    NOT_SUPPORTED = 2,
    FAILED = 3,
}
