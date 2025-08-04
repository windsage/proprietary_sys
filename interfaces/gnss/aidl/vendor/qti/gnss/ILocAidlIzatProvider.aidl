/*
* Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
* All Rights Reserved.
* Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package vendor.qti.gnss;

import vendor.qti.gnss.ILocAidlIzatProviderCallback;
import vendor.qti.gnss.LocAidlIzatRequest;
import vendor.qti.gnss.LocAidlWwanAppInfo;

@VintfStability
interface ILocAidlIzatProvider {
    void deinit();

    boolean init(in ILocAidlIzatProviderCallback callback);

    boolean onAddRequest(in LocAidlIzatRequest request);

    boolean onDisable();

    boolean onEnable();

    boolean onRemoveRequest(in LocAidlIzatRequest request);

    void notifyWwanAppInfo(in LocAidlWwanAppInfo appInfo);
}
