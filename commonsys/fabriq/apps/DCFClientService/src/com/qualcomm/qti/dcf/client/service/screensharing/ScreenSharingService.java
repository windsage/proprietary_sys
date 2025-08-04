/**
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.dcf.client.service.screensharing;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class ScreenSharingService extends Service {

    private static final String TAG = "ScreenSharingService";

    public @interface WfdState {
        int UNKNOWN = 0;
        int IDLE = 1;
        int BUSY = 2;
    }

    public @interface WfdType {
        int SOURCE = 0;
        int SINK = 1;
    }

    //WIFI P2P errors are 0 or positive values, to diff, we define our errors as negative values
    public @interface Error {
        int NO_SCREEN_SHARING_SERVICE = -1;
        int BINDER_CALL_FAILED = -2;
        int P2P_DISCOVERY_TIMEOUT = -3;
        int P2P_CONNECTING_TIMEOUT = -4;
        int NO_WFD_SERVICE = -5;
        int CREATE_SESSION_FAILED = -6;
        int START_SESSION_FAILED = -7;
        int START_SESSION_TIMEOUT = -8;
    }

    private IScreenSharingService.Stub mBinder;

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind()");
        if (mBinder == null) {
            Context c = getApplicationContext();
            mBinder = new ScreenSharingServiceImpl(c);
            // Start self service here to prevent the service process be killed immediately when
            // user unbind service or client process be killed which will cause abnormal
            // disconnection of wfd session and p2p.
            startSelf();
        }
        return mBinder;
    }

    private void startSelf() {
        Intent screenSharing = new Intent(this, this.getClass());
        startService(screenSharing);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
        if (mBinder != null && mBinder instanceof ScreenSharingServiceImpl) {
            Log.d(TAG, "Unregister callbacks");
            ((ScreenSharingServiceImpl) mBinder).destroyService();
            mBinder = null;
        }
    }

}