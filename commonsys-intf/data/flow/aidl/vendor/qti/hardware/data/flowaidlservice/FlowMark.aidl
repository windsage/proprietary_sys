/*===========================================================================
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
===========================================================================*/

package vendor.qti.hardware.data.flowaidlservice;

import vendor.qti.hardware.data.flowaidlservice.Tos;
import vendor.qti.hardware.data.flowaidlservice.V6Mark;

/**
 * Union so only IPv4 or IPv6 flow marking can be used per flow.
 */
@VintfStability
union FlowMark {
    /**
     * TOS is only for IPv4 flows.
     */
    Tos tos;
    /**
     * IPv6 can have traffic class and/or flow label.
     */
    V6Mark v6;
}
