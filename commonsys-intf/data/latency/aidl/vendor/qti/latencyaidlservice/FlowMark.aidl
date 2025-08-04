/******************************************************************************  
Copyright (c) 2023 Qualcomm Technologies, Inc.  
All Rights Reserved.  
Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.latencyaidlservice;

import vendor.qti.latencyaidlservice.Tos;
import vendor.qti.latencyaidlservice.V6Mark;

/**
 * Union so only IPv4 or IPv6 filter marking can be used per filter.
 */
@VintfStability
union FlowMark {
    /**
     * TOS is only for IPv4 flows.
     */
    Tos tos;
    /**
     * IPv6 can have traffic class and/or filter label.
     */
    V6Mark v6;
}
