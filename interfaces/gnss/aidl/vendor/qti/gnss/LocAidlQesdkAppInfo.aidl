/*
* Copyright (c) 2024 Qualcomm Technologies, Inc.
* All Rights Reserved.
* Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package vendor.qti.gnss;

import vendor.qti.gnss.LocAidlQesdkSessionType;
import vendor.qti.gnss.LocAidlQesdkSessionPrecision;

@VintfStability
parcelable LocAidlQesdkAppInfo {
    int pid;
    int uid;
    String appHash;
    String appPackageName;
    String appCookie;
    String appQwesLicenseId;
    LocAidlQesdkSessionType sessionType;
    LocAidlQesdkSessionPrecision sessionPrecision;
}

