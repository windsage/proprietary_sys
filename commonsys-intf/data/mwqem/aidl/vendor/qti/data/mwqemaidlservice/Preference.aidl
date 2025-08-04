/*===========================================================================
 Copyright (c) 2023 Qualcomm Technologies, Inc.
 All Rights Reserved.
 Confidential and Proprietary - Qualcomm Technologies, Inc.
===========================================================================*/

package vendor.qti.data.mwqemaidlservice;

/**
 * Enum which holds the MWQEM Preference
 * OPTIMIZE_LATENCY
 * OPTIMIZE_TPUT
 */
@VintfStability
@Backing(type="byte")
enum Preference {
    OPTIMIZE_LATENCY = 1,
    OPTIMIZE_TPUT = 2,
}
