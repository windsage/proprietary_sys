/******************************************************************************  
Copyright (c) 2023 Qualcomm Technologies, Inc.  
All Rights Reserved.  
Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.latencyaidlservice;

import vendor.qti.latencyaidlservice.Level;
import vendor.qti.latencyaidlservice.FlowMark;
import vendor.qti.latencyaidlservice.IpAddress;
import vendor.qti.latencyaidlservice.OodStatus;
import vendor.qti.latencyaidlservice.Protocol;

/**
 * Data structure for Filter parameters.
 *
 * @field srcIp source IpAddress.
 * @field srcPort source port.
 * @field dstIp destination IpAddress.
 * @field dstPort destination port.
 * @field protocol Uplink queue size in bytes.
 * @field mark union for v4 or v6 marking.
 * @field uplink_level uplink latency level.
 * @field downlink_level downlink latency level.
 * @field pdcp_timer pdcp discard timer in millisecs.
 * @field ood OOD Status.
 */
@VintfStability
parcelable Filter {
    IpAddress srcIp;
    char srcPort;
    IpAddress dstIp;
    char dstPort;
    Protocol protocol;
    FlowMark mark;
    Level uplink_level;
    Level downlink_level;
    long pdcp_timer;
    OodStatus ood;
}
