/******************************************************************************  
Copyright (c) 2023 Qualcomm Technologies, Inc.  
All Rights Reserved.  
Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.latencyaidlservice;

import vendor.qti.latencyaidlservice.FilterInfo;

/**
 * Interface declaring solicited and unsolicited filter indications updates.
 */
@VintfStability
interface ILinkLatencyIndication {
    /**
     * Indicates that a Filter has changed Status.
     *
     * This indication is sent whenever a Filter is updated.
     *
     * @param filter Struct tying the status and filter ID to the new filter.
     */
    oneway void filterStatus(in FilterInfo filter);
}
