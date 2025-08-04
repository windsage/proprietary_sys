/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.fingerprint;

import vendor.qti.hardware.fingerprint.Status;

/**
 * @brief: Structure for enrollment record
 */
@VintfStability
parcelable EnrollRecord {
    Status status;
    String enrolleeId;
    long enrollmentDate;
    String[] fingers;
    int dbStatus;
}
