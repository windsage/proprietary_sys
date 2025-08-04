/*===========================================================================
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
===========================================================================*/

package vendor.qti.hardware.data.flowaidlservice;

/**
 * Possible Flow Status values.
 */
@VintfStability
@Backing(type="long")
enum FlowStatus {
    FLOW_INACTIVE,
    FLOW_ACTIVE,
    FLOW_INACTIVITY_TIMEOUT,
    FLOW_DELETED,
    ERROR_DUPLICATE_FLOW,
    ERROR_FLOW_LIMIT_REACHED,
    ERROR_INTERNAL,
    ERROR_INVALID_ARGS,
    ERROR_IP_TYPE_MISMATCH,
    ERROR_INACTIVITY_TIMEOUT_INVALID,
    ERROR_DST_PORT_INVALID,
    ERROR_SRC_PORT_INVALID,
    ERROR_DST_IP_INVALID,
    ERROR_SRC_IP_INVALID,
}
