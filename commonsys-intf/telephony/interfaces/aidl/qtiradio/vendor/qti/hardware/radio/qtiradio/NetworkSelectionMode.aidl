/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.qtiradio;

import vendor.qti.hardware.radio.qtiradio.AccessMode;

@VintfStability
@JavaDerive(toString=true)
parcelable NetworkSelectionMode {

    /**
     * Current AccessMode {PLMN or SNPN}
     */
    AccessMode accessMode;

    /**
     * Specifies whether network selection mode is manual or not.
     */
    boolean isManual;
}
