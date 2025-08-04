/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.qtiradioconfig;

@VintfStability
@Backing(type="int")
enum SimType {
    SIM_TYPE_UNKNOWN = 0,
    SIM_TYPE_PHYSICAL = 1 << 0,
    SIM_TYPE_INTEGRATED = 1 << 1,
    /**
     * As a placeholder added eSIM type but currently this is not used.
     * At present, Physical type returned for both Physical and eSIM cards.
     */
    SIM_TYPE_ESIM = 1 << 2,
}
