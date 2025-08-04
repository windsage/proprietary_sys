/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

@VintfStability
@Backing(type="int")
enum SsServiceType {
    INVALID,
    CFU,
    CF_BUSY,
    CF_NO_REPLY,
    CF_NOT_REACHABLE,
    CF_ALL,
    CF_ALL_CONDITIONAL,
    CFUT,
    CLIP,
    CLIR,
    COLP,
    COLR,
    CNAP,
    WAIT,
    BAOC,
    BAOIC,
    BAOIC_EXC_HOME,
    BAIC,
    BAIC_ROAMING,
    ALL_BARRING,
    OUTGOING_BARRING,
    INCOMING_BARRING,
    INCOMING_BARRING_DN,
    INCOMING_BARRING_ANONYMOUS,
}
