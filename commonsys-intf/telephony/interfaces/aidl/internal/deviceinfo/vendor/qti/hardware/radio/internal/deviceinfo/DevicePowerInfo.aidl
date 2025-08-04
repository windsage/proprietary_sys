/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.internal.deviceinfo;

@VintfStability
parcelable DevicePowerInfo {
    /**
     * Bit mask containing the type of charger connected to the device
     * corresponding to values of the "plugged" field in the
     * ACTION_BATTERY_CHANGED intent
     * Will have values from enum ChargerType in the bit mask
     */
    long chargingMode;

    // Total battery capacity in microampere-hours
    int totalBatteryCapacity;

    // Current battery level as a percentage (0-100)
    byte batteryLevel;

    // Current battery level index in the range 0-10.
    // If threshold is 20, 50, 80, these would map to level index
    // 0 (0-20), 1 (21-50), 2 (51-80), 3 (81-100) etc.
    byte batteryLevelIndex;

    /**
     * True if the device is currently in power save mode, false otherwise.
     * Monitored via PowerManager#ACTION_POWER_SAVE_MODE_CHANGED
     */
    boolean powerSaveMode;
}
