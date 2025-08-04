/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

@VintfStability
@Backing(type="int")
enum SmsDeliverStatus {
    /**
     * Default value.
     */
    INVALID,
    /**
     * Message was not delivered.
     */
    ERROR,
    /**
     * Message was delivered successfully.
     */
    OK,
    /**
     * Message was not delivered due to lack of memory.
     */
    ERROR_NO_MEMORY,
    /**
     * Message was not delivered as the request is not supported.
     */
    ERROR_REQUEST_NOT_SUPPORTED,
}
