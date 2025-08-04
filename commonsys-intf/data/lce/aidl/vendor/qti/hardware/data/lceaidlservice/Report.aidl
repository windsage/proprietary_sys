/*===========================================================================

  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.

===========================================================================*/

package vendor.qti.hardware.data.lceaidlservice;

import vendor.qti.hardware.data.lceaidlservice.AccessNetwork;
import vendor.qti.hardware.data.lceaidlservice.LinkDirection;
import vendor.qti.hardware.data.lceaidlservice.ReportType;

/**
 * Data structure passed to linkEstimation().
 * @field accessNetwork The network type for the estimation report.
 * @field rate Rate in kbps.
 * @field level Level of accuracy at which the throughput information is generated on a scale
 *   of 0 through 7. 0 indicates the least
 * @field dir LinkDirection.
 * @field queueSize Uplink queue size in bytes.
 * @field type If the report came from setLinkCapacityReportingCriteria() or
 *   getLastEstimationReport().
 */
@VintfStability
parcelable Report {
    AccessNetwork accessNetwork;
    int rate;
    int level;
    LinkDirection dir;
    int queueSize;
    ReportType type;
}
