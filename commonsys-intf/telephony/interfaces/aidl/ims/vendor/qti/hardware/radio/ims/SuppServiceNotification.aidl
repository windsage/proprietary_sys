/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.NotificationType;

/**
 * Data structure containing details of the supplementary service notification.
 * Telephony will process SuppServiceNotification based on
 * individual default parameters.
 */

@VintfStability
parcelable SuppServiceNotification {
    NotificationType notificationType = NotificationType.INVALID;
    int code;
    int index;
    int type;
    String number;
    int connId;
    String historyInfo;
    boolean hasHoldTone;
    boolean holdTone;
}

