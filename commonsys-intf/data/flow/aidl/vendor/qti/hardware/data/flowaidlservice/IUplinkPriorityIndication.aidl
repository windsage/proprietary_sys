/*===========================================================================
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
===========================================================================*/

package vendor.qti.hardware.data.flowaidlservice;

import vendor.qti.hardware.data.flowaidlservice.FlowInfo;

/**
 * Interface declaring solicited and unsolicited flow indications updates.
 */
@VintfStability
interface IUplinkPriorityIndication {
    /**
     * Indicates that a Flow has changed Status.
     *
     * This indication is sent whenever a Flow is updated.
     *
     * @param flow Struct tying the status and flow ID to the new flow.
     */
    oneway void flowStatus(in FlowInfo flow);
}
