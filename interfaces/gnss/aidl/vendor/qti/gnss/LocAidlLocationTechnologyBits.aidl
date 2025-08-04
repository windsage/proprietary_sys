/*
*  Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
*  All rights reserved.
*  Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package vendor.qti.gnss;

@VintfStability
@Backing(type="int")
enum LocAidlLocationTechnologyBits {
    GNSS_BIT = (1 << 0),
    CELL_BIT = (1 << 1),
    WIFI_BIT = (1 << 2),
    SENSORS_BIT = (1 << 3),
    PPE_BIT = (1 << 8),
    DGNSS_BIT = (1 << 11),
    SBAS_BIT = (1 << 15),
}
