/*===========================================================================
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
===========================================================================*/

package vendor.qti.hardware.data.flowaidlservice;

import vendor.qti.hardware.data.flowaidlservice.AppType;
import vendor.qti.hardware.data.flowaidlservice.FlowMark;
import vendor.qti.hardware.data.flowaidlservice.IpAddress;
import vendor.qti.hardware.data.flowaidlservice.Protocol;
import vendor.qti.hardware.data.flowaidlservice.Direction;

/**
 * Data structure for Flow parameters.
 *
 * All fields from Flow.
 * @field mark union for v4 or v6 marking.
 */
@VintfStability
parcelable Flow {
    IpAddress srcIp;
    char srcPort;
    IpAddress dstIp;
    char dstPort;
    Protocol protocol;
    AppType appType;
    int inactivityTimer;
    FlowMark mark;
    Direction dir;
}
