/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.quicinc.voice.assist.sdk.enrollment;

/**
 * An enumeration of all known reason an user voice enrollment operation can fail.
 */
public enum EnrollmentFailedReason {
    /**
     * Used to indicate failure with unknown reason.
     */
    INIT_FAILURE_UNKNOWN(-1),
    /**
     * Used to indicate that the remote service is not connected.
     */
    INIT_FAILURE_SERVICE_NOT_CONNECTED(-2),
    /**
     * Used to indicate the base model is illegal for the enrollment leads to failure.
     */
    INIT_FAILURE_ILLEGAL_MODEL(-3),
    /**
     * Used to indicate model creation failed with unknown reason.
     */
    CREATION_FAILURE_UNKNOWN(-4);

    private final int mErrorCode;

    EnrollmentFailedReason(int errorCode) {
        mErrorCode = errorCode;
    }

    static EnrollmentFailedReason getFailedReason(int errorCode) {
        for (EnrollmentFailedReason error : EnrollmentFailedReason.values()) {
            if (errorCode == error.mErrorCode) {
                return error;
            }
        }
        return INIT_FAILURE_UNKNOWN;
    }
}
