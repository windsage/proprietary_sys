/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.fingerprint;

/**
 * @brief: Structure for Authentication token
 */
@VintfStability
parcelable AuthToken {
    byte version;
    long challengeId;
    long userId;
    long authenticatorId;
    int  authenticatorType;
    long timestamp;
    byte[] hmac;
}
