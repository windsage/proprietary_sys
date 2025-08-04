/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

/**
 * Data structure containing details regarding status report of the sent message.
 * Telephony will process SmsSendStatusReport if SmsSendStatusReport#pdu is not null.
 */
@VintfStability
parcelable SmsSendStatusReport {
    int messageRef = -1;
    String format;
    byte[] pdu;
}

