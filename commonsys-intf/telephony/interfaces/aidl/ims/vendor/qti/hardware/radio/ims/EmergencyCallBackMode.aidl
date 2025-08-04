/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

@VintfStability
@Backing(type="int")
enum EmergencyCallBackMode {
    INVALID = 0, /* Invalid emergency callback mode */
    EXIT = 1,    /* Modem leaves emergency callback mode */
    ENTER = 2,   /* Modem enters emergency callback mode */
}
