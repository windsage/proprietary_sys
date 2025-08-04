/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.quicinc.voice.assist.sdk.utility;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;

import java.util.List;

import static com.quicinc.voice.assist.sdk.utility.Constants.QVA_PACKAGE_NAME;

public class PermissionUtils {
    private static final String TAG = PermissionUtils.class.getSimpleName();
    public static final String QVA_PERMISSION_ACTIVITY =
            "com.quicinc.voice.activation.activity.PermissionActivity";
    public static final String QVA_PERMISSION_ACTION =
            "com.quicinc.voice.activation.PERMISSION_LAUNCHER";

    /**
     * Check whether QVA has {@code Manifest.permission.RECORD_AUDIO} permission or not.
     * @param context The context used to query permission
     * @return {@code true} has permission, {@code false} otherwise.
     */
    public static boolean hasQVARecordAudioPermission(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo qvaInfo = pm.getPackageInfo(QVA_PACKAGE_NAME, 0 );
            if (qvaInfo != null) {
                return PackageManager.PERMISSION_GRANTED ==
                        pm.checkPermission(Manifest.permission.RECORD_AUDIO, QVA_PACKAGE_NAME);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "can't find qva application");
        }
        return false;
    }

    /**
     * Method that help QVA request audio record permission before voice enrollment or recognition.
     *
     * @param context A <code>Context</code> that will be used to get PackageManager and start QVA's
     *                permission Activity.
     * @param requestCode The request code used to receive the result of QVA permission request.
     *
     * @return <code>true</code> need help qva to request Audio permission, otherwise qva is not
     *         exist or already have permission.
     */
    public static boolean requestQVAPermission(Activity context, int requestCode) {
        PackageManager pm = context.getPackageManager();
        Intent permissionIntent = new Intent(QVA_PERMISSION_ACTION)
                .setPackage(QVA_PACKAGE_NAME)
                .setComponent(new ComponentName(QVA_PACKAGE_NAME,
                        QVA_PERMISSION_ACTIVITY));

        List<ResolveInfo> per = pm.queryIntentActivities(permissionIntent, 0);
        if (!per.isEmpty()) {
            Log.d(TAG,
                    "need request audio record permission");
            UIThreadExecutor.execute(() -> {
                context.startActivityForResult(permissionIntent, requestCode);
            });
            return true;
        }
        return false;
    }
}
