/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

@VintfStability
@Backing(type="int")
enum CallState {
    INVALID,
    ACTIVE,
    HOLDING,
    DIALING,
    ALERTING,
    INCOMING,
    WAITING,
    END,
}
