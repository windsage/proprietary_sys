/*===========================================================================

  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.

===========================================================================*/

package vendor.qti.hardware.data.lceaidlservice;

import vendor.qti.hardware.data.lceaidlservice.Report;

/**
 * Interface declaring unsolicited link capacity estimate indications.
 */
@VintfStability
interface ILceIndication {
    /**
     * Indicates current link capacity estimate.
     *
     * This indication is sent whenever the reporting criteria, as set by
     * ILceService.setLinkCapacityReportingCriteria, are met.
     *
     * @param report struct having the accessNetwork, rate, level, dir, queueSize to the link capacity estimation
     */
    oneway void linkEstimation(in Report report);
}
