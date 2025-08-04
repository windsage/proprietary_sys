/*****************************************************************
 * Copyright (c) 2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 ******************************************************************/

package vendor.qti.bluetooth.xpan;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

public class XpanApplication extends Application {

    private final static boolean DBG = true;
    private String TAG = "XpanApp";

    @Override
    public void onCreate() {
        super.onCreate();
        if (DBG)
            Log.d(TAG, TAG);

        Thread.setDefaultUncaughtExceptionHandler(new AppHandler());

    }

    private class AppHandler implements Thread.UncaughtExceptionHandler {

        @Override
        public void uncaughtException(Thread th, Throwable e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
            if (XpanProfileService.getService() != null) {
                stopService(new Intent(getApplicationContext(), XpanProfileService.class));
            }
        }
    }

}
