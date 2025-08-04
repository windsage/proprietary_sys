/*
* Copyright (c) 2022 Qualcomm Technologies, Inc.
* All Rights Reserved.
* Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package vendor.qti.gnss;

import vendor.qti.gnss.LocAidlQesdkSessionParams;
import vendor.qti.gnss.LocAidlQesdkAppInfo;

@VintfStability
interface ILocAidlQesdkTrackingCallback {

    void requestLocationUpdatesCb(in LocAidlQesdkSessionParams params);
    void removeLocationUpdatesCb(in LocAidlQesdkSessionParams params);
    void setUserConsent(in LocAidlQesdkSessionParams params, in boolean userConsent);
    void setWwanAppInfo(in LocAidlQesdkAppInfo appInfo);
    void removeWwanApp(in String packageName);
}
