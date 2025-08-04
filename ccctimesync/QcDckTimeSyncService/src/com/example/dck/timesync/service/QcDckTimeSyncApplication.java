/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.bluetooth.qcdcktimesync;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

/** Starts the QcDckTimeSyncService. */
public class QcDckTimeSyncApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("QcDckTimeSyncApplication:", "OnCreate");
        Intent serviceIntent = new Intent(getApplicationContext(), QcDckTimeSyncService.class);
        startService(serviceIntent);
    }
}
