/**
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.dcf.client.service.screensharing;

import com.qualcomm.qti.dcf.client.service.screensharing.IActionListener;
import com.qualcomm.qti.dcf.client.service.screensharing.IDeviceInfoListener;
import com.qualcomm.qti.dcf.client.service.screensharing.IWfdStatusListener;

interface IScreenSharingService {

    void requestDeviceInfo(IDeviceInfoListener listener);

    void setWfdDeviceType(int deviceType);

    int getWfdDeviceType();

    int getWfdState();

    String getConnectedDeviceAddress();

    void startScreenSharing(String peerAddress, IActionListener listener);

    void stopScreenSharing(IActionListener listener);

    void registerWfdStatusListener(IWfdStatusListener listener);

    void unregisterWfdStatusListener(IWfdStatusListener listener);
}