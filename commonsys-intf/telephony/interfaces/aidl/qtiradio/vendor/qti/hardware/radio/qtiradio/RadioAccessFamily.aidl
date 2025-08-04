/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.qtiradio;

import vendor.qti.hardware.radio.qtiradio.RadioTechnology;

@VintfStability
@Backing(type="int")
enum RadioAccessFamily {
    UNKNOWN = 1 << RadioTechnology.UNKNOWN,
    GPRS = 1 << RadioTechnology.GPRS,
    EDGE = 1 << RadioTechnology.EDGE,
    UMTS = 1 << RadioTechnology.UMTS,
    IS95A = 1 << RadioTechnology.IS95A,
    IS95B = 1 << RadioTechnology.IS95B,
    ONE_X_RTT = 1 << RadioTechnology.ONE_X_RTT,
    EVDO_0 = 1 << RadioTechnology.EVDO_0,
    EVDO_A = 1 << RadioTechnology.EVDO_A,
    HSDPA = 1 << RadioTechnology.HSDPA,
    HSUPA = 1 << RadioTechnology.HSUPA,
    HSPA = 1 << RadioTechnology.HSPA,
    EVDO_B = 1 << RadioTechnology.EVDO_B,
    EHRPD = 1 << RadioTechnology.EHRPD,
    LTE = 1 << RadioTechnology.LTE,
    HSPAP = 1 << RadioTechnology.HSPAP,
    GSM = 1 << RadioTechnology.GSM,
    TD_SCDMA = 1 << RadioTechnology.TD_SCDMA,
    LTE_CA = 1 << RadioTechnology.LTE_CA,
    NR_NSA = 1 << RadioTechnology.NR_NSA,
    NR_SA = 1 << RadioTechnology.NR_SA,
}
