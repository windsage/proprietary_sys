/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.uim_remote_client;

import vendor.qti.hardware.radio.uim_remote_client.UimRemoteClientAppInfo;

@VintfStability
parcelable UimRemoteClientCardInitStatusType {
    /**
     * Num of active SIM slots in device
     */
    byte numOfActiveSlots;
    /**
     * Num of Apps for a slot
     */
    byte numOfApps;
    /**
     * App-Info for each App of a slot
     */
    UimRemoteClientAppInfo[] appInfo;
}
