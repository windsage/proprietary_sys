/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.c2pa;

/**
 * Response for C2PA Validation
 */
@VintfStability
@Backing(type="int")
enum ValidateResponse {
    VALIDATION_RESPONSE_VALID = 0,
    VALIDATION_RESPONSE_INVALID,
    VALIDATION_RESPONSE_MANIFEST_CORRUPTED,
    VALIDATION_RESPONSE_FAILED,
    VALIDATION_RESPONSE_CUSTOM = (1 << 8),
}