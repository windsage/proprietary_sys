/*
* Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
* All Rights Reserved.
* Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package vendor.qti.gnss;

import vendor.qti.gnss.LocAidlIzatRequestType;

@VintfStability
parcelable LocAidlWwanAppInfo {
    int pid;
    int uid;
    String appHash;
    String appPackageName;
    String appCookie;
    boolean hasFinePermission;
    boolean hasCoarsePermission;
    boolean hasBackgroundPermission;
    //Below two fields are only valid for QESDK app
    String appQwesLicenseId;
    boolean hasPremiumLicense;
    boolean hasStandardLicense;
}
