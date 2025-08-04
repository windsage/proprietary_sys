/*
 * Copyright (c) 2020-2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qti.phone;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.util.Log;

public class QtiTelephonyBootReceiver extends BroadcastReceiver {
    private static final String TAG = "QtiTelephonyBootReceiver";
    private static final boolean DBG = true;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (DBG)
            Log.d(TAG, "onReceive: " + intent.toString());
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)
                || intent.getAction().equals(Intent.ACTION_LOCKED_BOOT_COMPLETED)) {
            if (UserHandle.myUserId() != 0) {
                Log.d(TAG, "not primary user, ignore start ExtTelephonyService");
                return;
            }
            Intent serviceIntent = new Intent(context, ExtTelephonyService.class);
            ComponentName serviceComponent = context.startService(serviceIntent);
            if (serviceComponent == null) {
                Log.d(TAG, "Could not start ExtTelephonyService");
            } else {
                Log.d(TAG, "Successfully started ExtTelephonyService");
            }
        } else {
            Log.e(TAG, "Received unsupported intent");
        }
    }
}
