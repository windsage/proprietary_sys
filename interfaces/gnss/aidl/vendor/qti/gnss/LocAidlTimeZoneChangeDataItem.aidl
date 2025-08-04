/*
* Copyright (c) 2021 Qualcomm Technologies, Inc.
* All Rights Reserved.
* Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package vendor.qti.gnss;

@VintfStability
parcelable LocAidlTimeZoneChangeDataItem {
    long curTimeMillis;
    int rawOffset;
    int dstOffset;
}

