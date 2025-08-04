/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

@VintfStability
@Backing(type="int")
enum SmsSendStatus {
    INVALID,
    /**
     * Message was sent successfully.
     */
    OK,
    /**
     * IMS provider failed to send the message and platform
     * should not retry falling back to sending the message
     * using the radio.
     */
    ERROR,
    /**
     * IMS provider failed to send the message and platform
     * should retry again after setting TP-RD
     */
    ERROR_RETRY,
    /**
     * IMS provider failed to send the message and platform
     * should retry falling back to sending the message
     * using the radio.
     */
    ERROR_FALLBACK,
}
