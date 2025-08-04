/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.RadioTechType;

@VintfStability
@Backing(type="int")
enum RadioTechType {
    INVALID,
    ANY,
    UNKNOWN,
    GPRS,
    EDGE,
    UMTS,
    IS95A,
    IS95B,
    RTT_1X,
    EVDO_0,
    EVDO_A,
    HSDPA,
    HSUPA,
    HSPA,
    EVDO_B,
    EHRPD,
    LTE,
    HSPAP,
    GSM,
    TD_SCDMA,
    WIFI,
    IWLAN,
    NR5G,
    C_IWLAN,
}
