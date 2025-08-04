/*
*  Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
*  All rights reserved.
*  Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package vendor.qti.gnss;
import vendor.qti.gnss.ILocAidlPrecisePositionServiceCallback;

@VintfStability
interface ILocAidlPrecisePositionService {

    @VintfStability
    @Backing(type="int")
    enum LocAidlPrecisePositionCorrectionType {
        TYPE_DEFAULT = 1,
        TYPE_RTCM = 2,
        TYPE_3GPP = 3,
    }

    @VintfStability
    @Backing(type="int")
    enum LocAidlPrecisePositionType {
        UNKNOWN = 0,
        MLP = 1,
        DLP = 2,
        MLP_WOCS = 3,
    }

    int setCallback(in ILocAidlPrecisePositionServiceCallback callback);

    void startSession(in int id, in long tbfMsec, in LocAidlPrecisePositionType preciseType,
            in LocAidlPrecisePositionCorrectionType correctionType);

    void stopSession(in int id);
}


