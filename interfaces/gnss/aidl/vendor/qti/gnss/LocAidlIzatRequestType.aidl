/*
* Copyright (c) 2023 Qualcomm Technologies, Inc.
* All Rights Reserved.
* Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package vendor.qti.gnss;

@VintfStability
@Backing(type="int")
enum LocAidlIzatRequestType {
    WIFI_BASIC = 0,
    WIFI_PREMIUM = 1,
    WWAN_BASIC = 2,
    WWAN_PREMIUM = 3,
}
