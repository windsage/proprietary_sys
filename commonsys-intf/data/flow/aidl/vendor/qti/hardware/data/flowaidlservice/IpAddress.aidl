/*===========================================================================
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
===========================================================================*/

package vendor.qti.hardware.data.flowaidlservice;

/**
 * IPv4/v6 address union.
 */
@VintfStability
union IpAddress {
    /**
     * 0.0.0.0 is invalid. should be ignored.
     */
    @nullable byte[4] v4;
    /**
     * 0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0 is invalid. should be ignored.
     */
    byte[16] v6;
}
