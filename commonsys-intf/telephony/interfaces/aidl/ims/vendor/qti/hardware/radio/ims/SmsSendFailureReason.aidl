/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

@VintfStability
@Backing(type="int")
enum SmsSendFailureReason {
    INVALID,
    NONE,
    /**
     * Generic failure cause
     */
    GENERIC_FAILURE,
    /**
     * Failed because radio was explicitly turned off
     */
    RADIO_OFF,
    /**
     * Failed because no pdu provided
     */
    NULL_PDU,
    /**
     * Failed because service is currently unavailable
     */
    NO_SERVICE,
    /**
     * Failed because we reached the sending queue limit.
     */
    LIMIT_EXCEEDED,
    /**
     * Failed because user denied the sending of this short code.
     */
    SHORT_CODE_NOT_ALLOWED,
    /**
     * Failed because the user has denied this app
     * ever send premium short codes.
     */
    SHORT_CODE_NEVER_ALLOWED,
    /**
     * Failed because FDN is enabled.
     */
    FDN_CHECK_FAILURE,
    /**
     * Failed because the radio was not available.
     */
    RADIO_NOT_AVAILABLE,
    /**
     * Failed because of network rejection.
     */
    NETWORK_REJECT,
    /**
     * Failed because of invalid arguments.
     */
    INVALID_ARGUMENTS,
    /**
     * Failed because of an invalid state.
     */
    INVALID_STATE,
    /**
     * Failed because there is no memory.
     */
    NO_MEMORY,
    /**
     * Failed because the sms format is not valid.
     */
    INVALID_SMS_FORMAT,
    /**
     * Failed because of a system error.
     */
    SYSTEM_ERROR,
    /**
     * Failed because of a modem error.
     */
    MODEM_ERROR,
    /**
     * Failed because of a network error.
     */
    NETWORK_ERROR,
    /**
     * Failed because of an encoding error.
     */
    ENCODING_ERROR,
    /**
     * Failed because of an invalid smsc address.
     */
    INVALID_SMSC_ADDRESS,
    /**
     * Failed because the operation is not allowed.
     */
    OPERATION_NOT_ALLOWED,
    /*
     * Failed because of an internal error.
     */
    INTERNAL_ERROR,
    /**
     * Failed because there are no resources.
     */
    NO_RESOURCES,
    /**
     * Failed because the operation was cancelled.
     */
    CANCELLED,
    /**
     * Failed because the request is not supported.
     */
    REQUEST_NOT_SUPPORTED,
}
