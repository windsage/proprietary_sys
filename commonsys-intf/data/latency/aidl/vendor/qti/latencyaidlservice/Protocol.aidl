/******************************************************************************  
Copyright (c) 2023 Qualcomm Technologies, Inc.  
All Rights Reserved.  
Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.latencyaidlservice;

/**
 * Enum values for filter protocols.
 */
@VintfStability
@Backing(type="long")
enum Protocol {
    NONE,
    TCP,
    UDP,
    TCPUDP,
}
