/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.uim_remote_client;

import vendor.qti.hardware.radio.uim_remote_client.UimRemoteClientAppState;
import vendor.qti.hardware.radio.uim_remote_client.UimRemoteClientAppType;

@VintfStability
parcelable UimRemoteClientAppInfo {
    /**
     * Enumeration for app type
     */
    UimRemoteClientAppType appType;
    /**
     * Enumeration for app state
     */
    UimRemoteClientAppState appState;
}
