/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.uim_remote_client;

@VintfStability
@Backing(type="int")
enum UimRemoteClientAppState {
    /**
     * Un-known app state
     */
    UIM_RMT_APP_STATE_UNKNOWN = 0,
    /**
     * Detected app state
     */
    UIM_RMT_APP_STATE_DETECTED = 1,
    /**
     * Waiting-On-User app state
     */
    UIM_RMT_APP_STATE_WAITING_ON_USER = 2,
    /**
     * Halted app state
     */
    UIM_RMT_APP_STATE_HALTED = 3,
    /**
     * Ready app state
     */
    UIM_RMT_APP_STATE_READY = 4,
}
