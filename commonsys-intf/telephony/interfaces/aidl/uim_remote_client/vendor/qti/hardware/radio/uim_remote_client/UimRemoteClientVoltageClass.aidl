/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.uim_remote_client;

@VintfStability
@Backing(type="int")
enum UimRemoteClientVoltageClass {
    UIM_REMOTE_VOLTAGE_CLASS_C_LOW = 0,
    UIM_REMOTE_VOLTAGE_CLASS_C = 1,
    UIM_REMOTE_VOLTAGE_CLASS_C_HIGH = 2,
    UIM_REMOTE_VOLTAGE_CLASS_B_LOW = 3,
    UIM_REMOTE_VOLTAGE_CLASS_B = 4,
    UIM_REMOTE_VOLTAGE_CLASS_B_HIGH = 5,
}
