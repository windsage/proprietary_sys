/**
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.dcf.client.service.screensharing;

interface IWfdStatusListener {
    /**
     * The screen sharing state will be notified to recipients once changed.
     * it contains 'unknown', 'idle', 'busy'.
     */
    void onWfdStateChanged(int state);

    /**
     * The device type of screen sharing will be notified to recipients once changed.
     * it contains 'source', 'sink'.
     */
    void onWfdDeviceTypeChanged(int wfdDeviceType);
}