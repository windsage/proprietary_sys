/*
*  Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
*  All rights reserved.
*  Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package vendor.qti.gnss;

import vendor.qti.gnss.LocAidlLocation;

@VintfStability
interface ILocAidlPrecisePositionServiceCallback {

    @VintfStability
    @Backing(type="int")
    enum LocAidlPrecisePositionSessionResponse {
        SUCCESS = 0,
        UNKNOWN_FAILURE = 1,
        NOT_SUPPORTED = 2,
        NO_VALID_LICENSE = 3,
        NO_CORRECTION = 4,
        ANOTHER_PRECISE_SESSION_RUNNING = 5,
    }

    void trackingCb(in LocAidlLocation location);

    void responseCb(in LocAidlPrecisePositionSessionResponse response);
}
