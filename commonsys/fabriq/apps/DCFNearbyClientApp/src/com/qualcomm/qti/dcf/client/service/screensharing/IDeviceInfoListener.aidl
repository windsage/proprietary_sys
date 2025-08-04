/**
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.dcf.client.service.screensharing;

interface IDeviceInfoListener {

    /**
     * Obtains the own device address and advertise to nearby devices for screen sharing.
     */
    void onDeviceAddressAvailable(String ownDeviceAddress);
}