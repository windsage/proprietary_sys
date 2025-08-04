/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

@VintfStability
@Backing(type="int")
enum Codec {
    INVALID,
    QCELP13K,
    EVRC,
    EVRC_B,
    EVRC_WB,
    EVRC_NW,
    AMR_NB,
    AMR_WB,
    GSM_EFR,
    GSM_FR,
    GSM_HR,
    G711U,
    G723,
    G711A,
    G722,
    G711AB,
    G729,
    EVS_NB,
    EVS_WB,
    EVS_SWB,
    EVS_FB,
}
