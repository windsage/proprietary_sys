/*===========================================================================
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
===========================================================================*/

package vendor.qti.hardware.data.flowaidlservice;

/**
 * Enum values for Flow protocols.
 */
@VintfStability
@Backing(type="long")
enum Protocol {
    NONE,
    ICMP,
    TCP,
    UDP,
    ESP,
    AH,
    ICMP6,
    TCPUDP,
}
