/**
 * Copyright (c) 2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.wifi.wifilearner;

/**
 * Byte array representing a Mac Address. Use when we need to
 * pass an array of Mac Addresses to a method, as variable-sized
 * 2D arrays are not supported in AIDL.
 */
@VintfStability
parcelable MacAddress {
    /**
     * Peer mac address.
     */
    byte[6] data;
}
