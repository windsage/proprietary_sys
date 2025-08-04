/**
 * Copyright (c) 2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.wifi.wifilearner;

@VintfStability
@Backing(type="long")
enum IfaceStatsInfoMask {
    STATS_HAS_RX_RATE = 1 << 0,
    STATS_HAS_TX_RATE = 1 << 1,
}
