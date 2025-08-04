/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

@VintfStability
@Backing(type="int")
enum SystemServiceDomain {
    INVALID = 0,      /* Invalid */
    NO_SRV = 1,       /* No service */
    CS_ONLY = 2,      /* Circuit-switched only */
    PS_ONLY = 3,      /* Packet-switched only */
    CS_PS = 4,        /* Circuit-switched and packet-switched */
    CAMPED = 5,       /* Camped */
}
