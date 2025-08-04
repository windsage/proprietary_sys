/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.ims.configaidlservice;

/**
 * Status codes for updateAppTokenFetchStatus API
 */
@VintfStability
@Backing(type="int")
enum StatusCode {
    /**
     * Operation succeeded
     */
    SUCCESS = 0,
    /**
     * Operation failed
     */
    FAILED,
    /**
     * Operation initiated and in progress
     */
    IN_PROGRESS,
    /**
     * Client is not authorised to perform operation
     */
    SECURITY_FAILURE,
    /**
     * Invalid/unknown uri/parameter provided for operation
     */
    ILLEGAL_PARAM,
    /**
     * Client permissions or other internal error condition
     */
    ILLEGAL_STATE,
    /**
     * Indicates the Login Engine is not installed/available
     */
    NULL_CURSOR,
    /**
     * Oem Client not available
     */
    APP_NOT_AVAILABLE,
}
