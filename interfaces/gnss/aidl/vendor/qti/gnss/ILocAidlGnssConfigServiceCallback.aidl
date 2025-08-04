/*
* Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
* All Rights Reserved.
* Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package vendor.qti.gnss;

import vendor.qti.gnss.LocAidlGnssConstellationType;
import vendor.qti.gnss.LocAidlRobustLocationInfo;

@VintfStability
interface ILocAidlGnssConfigServiceCallback {

    void getGnssSvTypeConfigCb(
        in LocAidlGnssConstellationType[] disabledSvTypeList);

    void getRobustLocationConfigCb(in LocAidlRobustLocationInfo info);
    void ntnConfigSignalMaskResponse(in boolean isSuccess, in int gpsSignalTypeConfigMask);
    void ntnConfigSignalMaskChanged(in int gpsSignalTypeConfigMask);
}
