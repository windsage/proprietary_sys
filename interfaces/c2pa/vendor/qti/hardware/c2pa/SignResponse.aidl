/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.c2pa;

/**
 * Response for C2PA Sign
 */
@VintfStability
@Backing(type="int")
enum SignResponse {
    SIGN_RESPONSE_SUCCESS = 0,
    SIGN_RESPONSE_FAILED,
    SIGN_RESPONSE_INVALID_MEDIA,
    SIGN_RESPONSE_INVALID_PARAM,
    SIGN_RESPONSE_NOT_ENROLLED,
    SIGN_RESPONSE_CUSTOM = (1 << 8),
}
