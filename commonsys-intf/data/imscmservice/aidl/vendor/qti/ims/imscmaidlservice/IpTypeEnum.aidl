/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.ims.imscmaidlservice;

/*
 * Supported IP type Enums
 */
@VintfStability
@Backing(type="int")
enum IpTypeEnum {
    /**
     * Unknown IP type.
     */
    UNKNOWN,
    /**
     * IPv4.
     */
    IPV4,
    /**
     * IPv6.
     */
    IPV6,
}
