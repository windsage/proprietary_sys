/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.qasr;

/** Service specific error returned for the {@link IQasr} APIs . */

@Backing(type="int") @VintfStability
enum QasrStatus {
    SUCCESS = 0,
    /** Invalid input. */
    INVALID = -1,
    /**
     * Failure due to resource contention, such as a high priority use case
     * has pre-empted ASR.
     */
    RESOURCE_CONTENTION = - 2,
    /** Operation is not supported in this implementation. */
    OPERATION_NOT_SUPPORTED = -3,
    /** Unexpected internal failure. */
    FAILURE = -4,
}
