/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.qtiradio;

@VintfStability
@Backing(type="int")
enum RadioTechnology {
    UNKNOWN = 0,
    GPRS = 1,
    EDGE = 2,
    UMTS = 3,
    IS95A = 4,
    IS95B = 5,
    ONE_X_RTT = 6,
    EVDO_0 = 7,
    EVDO_A = 8,
    HSDPA = 9,
    HSUPA = 10,
    HSPA = 11,
    EVDO_B = 12,
    EHRPD = 13,
    LTE = 14,
    HSPAP = 15,
    GSM = 16,
    TD_SCDMA = 17,
    IWLAN = 18,
    LTE_CA = 19,
    NR_NSA = 20,
    NR_SA = 21,
}
