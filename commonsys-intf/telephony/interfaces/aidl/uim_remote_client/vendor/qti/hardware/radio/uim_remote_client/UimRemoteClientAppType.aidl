/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.uim_remote_client;

@VintfStability
@Backing(type="int")
enum UimRemoteClientAppType {
    /**
     * Un-known app type
     */
    UIM_RMT_APP_UNKNOWN = 0,
    /**
     * SIM app type
     */
    UIM_RMT_APP_SIM = 1,
    /**
     * USIM app type
     */
    UIM_RMT_APP_USIM = 2,
    /**
     * RUIM app type
     */
    UIM_RMT_APP_RUIM = 3,
    /**
     * CSIM app type
     */
    UIM_RMT_APP_CSIM = 4,
}
