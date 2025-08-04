/* ==============================================================================
 * NetIFType.aidl
 *
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
============================================================================== */


package vendor.qti.hardware.wifidisplaysession_aidl;

@VintfStability
@Backing(type="int")
enum NetIFType {
    UNKNOWN_NET,
    WIFI_P2P,
    WIGIG_P2P,
    LAN,
}
