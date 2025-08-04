/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

@VintfStability
/**
 * Data structure to store sms related information when send sms
 * request is sent from telephony.
 */

parcelable SmsSendRequest {
    int messageRef = -1;
    String format;
    String smsc;
    boolean shallRetry;
    byte[] pdu;
}

