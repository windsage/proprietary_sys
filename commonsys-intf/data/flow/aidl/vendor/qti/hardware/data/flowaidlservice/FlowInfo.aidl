/*===========================================================================
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
===========================================================================*/

package vendor.qti.hardware.data.flowaidlservice;

import vendor.qti.hardware.data.flowaidlservice.FlowStatus;

/**
 * Data structure used by IUplinkPriorityIndication.flowStatus() and
 * IUplinkPriorityService.getFlows().
 *
 * @field flowId Id associated with the given flow.
 * @field status Current FlowStatus for the flow.
 */
@VintfStability
parcelable FlowInfo {
    int flowId;
    FlowStatus status;
}
