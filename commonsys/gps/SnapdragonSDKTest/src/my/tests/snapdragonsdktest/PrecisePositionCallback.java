/*====*====*====*====*====*====*====*====*====*====*====*====*====*====*====*
  Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
  All rights reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
=============================================================================*/

package my.tests.snapdragonsdktest;

import android.util.Log;
import android.location.Location;
import com.qti.location.sdk.IZatPrecisePositioningService;
import com.qti.location.sdk.IZatPrecisePositioningService.IZatLocationResponse;
import com.qti.location.sdk.IZatPrecisePositioningService.IZatLocationQuality;

public class PrecisePositionCallback implements
        IZatPrecisePositioningService.IZatPrecisePositioningCallback {
    private static String TAG = "PPCallbackInApp";
    private static PrecisePositionActivity mPpActivityObj;
    private static final Object sCallBacksLock = new Object();

    public PrecisePositionCallback(PrecisePositionActivity ppActivityObj) {
        mPpActivityObj = ppActivityObj;
    }

    public void onLocationAvailable(Location location) {
        synchronized(sCallBacksLock) {
            IZatLocationQuality qualityType;
            int locationTechMask;
            qualityType = IZatLocationQuality.fromInt(location.getExtras().getInt("Quality_type"));
            locationTechMask = location.getExtras().getInt("Tech_mask");
            String result = "Time:" + location.getTime() + ", latitude:" + location.getLatitude() +
                    ", longitude:" + location.getLongitude() + ", altitude:" +
                    location.getAltitude() + ", qualityType:" + qualityType + ", locationTechMask:"
                    + locationTechMask;
            Log.d(TAG, result);
            mPpActivityObj.showResultsOnUI(result);
        }
    }

    public void onResponseCallback(IZatLocationResponse response) {
        synchronized(sCallBacksLock) {
            Log.d(TAG, "response:" + response);
            String result = "response:" + response;
            mPpActivityObj.showResultsOnUI(result);
        }
    }
}
