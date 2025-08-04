/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.snapdragonServices.qape;

import vendor.qti.snapdragonServices.qape.ThermalZone;

@VintfStability
interface IThermalZoneCallback {
    void thermalZoneCallback(in ThermalZone zone);
}
