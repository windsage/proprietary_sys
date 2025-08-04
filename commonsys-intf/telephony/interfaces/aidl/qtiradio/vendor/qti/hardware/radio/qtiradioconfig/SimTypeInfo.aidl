/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.qtiradioconfig;

import vendor.qti.hardware.radio.qtiradioconfig.SimType;

@VintfStability
@JavaDerive(toString=true)
parcelable SimTypeInfo {
    /*
     * Current Sim Type, whether Physical/eSIM or iUICC(INTEGRATED)
     */
    SimType currentSimType;

    /*
     * Its a bitfield of SimType. To provide what all SIM Types supported on a slot,
     * Physical/eSIM or iUICC(INTEGRATED) or both
     */
    int supportedSimTypes;
}
