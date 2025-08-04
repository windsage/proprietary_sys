/*
* Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
* All Rights Reserved.
* Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package vendor.qti.gnss;

import vendor.qti.gnss.ILocAidlWWANAdRequestListener;

@VintfStability
interface ILocAidlWWANAdReceiver {

    @VintfStability
    @Backing(type="int")
    enum LocAidlWWANADType {
        LOW_ACCURACY = 0,
        HIGH_ACCURACY = 1,
    }

    boolean init(in ILocAidlWWANAdRequestListener listener);
    void pushWWANAssistanceData(int requestId, boolean status, LocAidlWWANADType respType,
        in byte[] respPayload);
}
