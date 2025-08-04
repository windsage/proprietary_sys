/*===========================================================================
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
===========================================================================*/

package vendor.qti.hardware.data.flowaidlservice;

/**
 * Possible values for Flow application type.
 */
@VintfStability
@Backing(type="int")
enum AppType {
    UNSPECIFIED,
    PRIORITY,
    VOICE,
    LOW_LATENCY_APP,
    VIDEO,
}
