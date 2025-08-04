/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

@VintfStability
@Backing(type="int")
enum VerificationStatus {
    /**
     * Telephone number is not validated.
     */
    VALIDATION_NONE,
    /**
     * Telephone number validation passed.
     */
    VALIDATION_PASS,
    /**
     * Telephone number validation failed.
     */
    VALIDATION_FAIL,
}
