/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

@VintfStability
@Backing(type="int")
enum EmergencyServiceCategory {
    // General emergency call, all categories
    UNSPECIFIED = 0,
    POLICE = 1 << 0,
    AMBULANCE = 1 << 1,
    FIRE_BRIGADE = 1 << 2,
    MARINE_GUARD = 1 << 3,
    MOUNTAIN_RESCUE = 1 << 4,
    // Manually Initiated eCall (MIeC)
    MIEC = 1 << 5,
    // Automatically Initiated eCall (AIeC)
    AIEC = 1 << 6,
}
