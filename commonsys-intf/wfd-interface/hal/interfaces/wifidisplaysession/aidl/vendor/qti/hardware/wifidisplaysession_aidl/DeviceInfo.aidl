/* ==============================================================================
 * DeviceInfo.aidl
 *
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
============================================================================== */


package vendor.qti.hardware.wifidisplaysession_aidl;

import vendor.qti.hardware.wifidisplaysession_aidl.NetIFType;

@VintfStability
parcelable DeviceInfo {
    String macAddr;
    String ipAddr;
    NetIFType netType;
    int wfdDeviceInfoBitmap;
    int sessionMngtControlPort;
    int decoderLatency;
    int extSupport;
    int maxThroughput;
    byte coupleSinkStatusBitmap;
}
