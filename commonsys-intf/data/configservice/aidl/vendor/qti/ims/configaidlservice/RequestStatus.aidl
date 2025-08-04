/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.ims.configaidlservice;

/**
 * Status code for client commands and APIs
 */
@VintfStability
@Backing(type="int")
enum RequestStatus {
    /**
     * Client command UNSUPPORTED
     */
    UNSUPPORTED = -1,
    /**
     * Client command succeeded
     */
    OK = 0,
    /**
     * Decompression of config failed
     */
    DECODING_ERROR = 1,
    /**
     * Configuration XML format Invalid
     */
    INVALID_CONTENT = 2,
    /**
     * Client command Failed
     */
    FAIL = 3,
    /**
     * Client command status is in progress
     */
    IN_PROGRESS = 4,
    /**
     * Client command retry attempts maxed out
     */
    RETRY_ATTEMPTS_MAXED_OUT = 5,
}
