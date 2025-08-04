/*===========================================================================
 Copyright (c) 2023 Qualcomm Technologies, Inc.
 All Rights Reserved.
 Confidential and Proprietary - Qualcomm Technologies, Inc.
===========================================================================*/

package vendor.qti.data.mwqemaidlservice;

/**
 * Return values for IMwqemService requests
 */
@VintfStability
@Backing(type="long")
enum ReturnCode {
    OK,
    INVALID_ARGUMENTS,
    UNKNOWN_ERROR,
}
