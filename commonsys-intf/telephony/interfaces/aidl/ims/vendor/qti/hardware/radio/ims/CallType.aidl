/*
 * Copyright (c) 2021-2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.CallType;

@VintfStability
@Backing(type="int")
enum CallType {
    UNKNOWN,
    VOICE,
    VT_TX,
    VT_RX,
    VT,
    VT_NODIR,
    CS_VS_TX,
    CS_VS_RX,
    PS_VS_TX,
    PS_VS_RX,
    SMS,
    UT,
    USSD,
    CALLCOMPOSER,
    DC,
}
