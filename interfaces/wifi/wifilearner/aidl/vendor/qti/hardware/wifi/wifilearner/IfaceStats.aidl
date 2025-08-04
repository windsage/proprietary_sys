/**
 * Copyright (c) 2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.wifi.wifilearner;

@VintfStability
parcelable IfaceStats {
    /**
     * Rate mask (enum IfaceStatsInfoMask) of the tx/rx rates.
     */
    long rateMask;
    /**
     * Current reception rate.
     */
    long rxRate;
    /**
     * Current transmission rate.
     */
    long txRate;
}
