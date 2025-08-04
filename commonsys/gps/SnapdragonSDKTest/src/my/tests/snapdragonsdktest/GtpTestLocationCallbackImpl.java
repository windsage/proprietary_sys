/*====*====*====*====*====*====*====*====*====*====*====*====*====*====*====*
  Copyright (c) 2022, 2024 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
=============================================================================*/
package my.tests.snapdragonsdktest;

import android.app.Activity;
import android.location.Location;
import android.util.Log;
import com.qti.location.sdk.IZatGtpService;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;

public class GtpTestLocationCallbackImpl implements IZatGtpService.IZatGtpServiceCallback {
    private static final String TAG = "GTPTestCallbackInApp";

    private Handler mHandler;
    private String sessionName;
    private int fixCounter;

    private static final int MSG_UPDATE_LOC_REPORT = 1;

    public GtpTestLocationCallbackImpl(String sessionName, Handler handler) {
        this.sessionName = sessionName;
        this.mHandler = handler;
        fixCounter = 0;
    }

    @Override
    public void onLocationAvailable(Location loc) {

        Bundle extras = loc.getExtras();

        if  (extras == null) {
            extras = new Bundle();
        }
        extras.putString("sessionName", sessionName);
        loc.setExtras(extras);
        Log.d(TAG, "reportLocation " + loc.toString());
        mHandler.obtainMessage(MSG_UPDATE_LOC_REPORT, fixCounter, 0, loc).sendToTarget();
        fixCounter++;
    }

}
