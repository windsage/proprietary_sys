/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

@VintfStability
@Backing(type="int")
enum SmsCallBackMode {
    INVALID = 0, /* Invalid */
    EXIT = 1,    /* Modem leaves Sms callback mode */
    ENTER = 2,   /* Modem enters Sms callback mode */
}
