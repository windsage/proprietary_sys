/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.c2pa;

/**
 * Response for C2PA Enroll
 */
@VintfStability
@Backing(type="int")
enum EnrollResponse {
    ENROLL_RESPONSE_SUCCESS = 0,
    ENROLL_RESPONSE_FAILED,
    ENROLL_RESPONSE_INVALID_PARAM,
    ENROLL_RESPONSE_NETWORK_ERROR,
    ENROLL_RESPONSE_CUSTOM = (1 << 8),
}