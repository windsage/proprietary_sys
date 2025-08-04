/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
 
package vendor.qti.snapdragonServices.qape;

@VintfStability
@Backing(type="int")
enum ThermalZone {
    FARAWAY_FROM_MITIGATION = 0,
    MITIGATION_HEADROOM_75P = 1,
    MITIGATION_HEADROOM_50P = 2,
    MITIGATION_HEADROOM_30P = 3,
    MITIGATION_HEADROOM_10P = 4,
    MITIGATION_HEADROOM_5P = 5,
    MITIGATED_DONE = 6,
}
