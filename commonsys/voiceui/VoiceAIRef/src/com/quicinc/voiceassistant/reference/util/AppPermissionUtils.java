/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.quicinc.voiceassistant.reference.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import com.quicinc.voiceassistant.reference.PermissionActivity;
import com.quicinc.voice.assist.sdk.utility.PermissionUtils;

import java.util.ArrayList;

public class AppPermissionUtils {
    public final static String[] RUNTIME_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
    };
    public static final String EXTRA_KEY_REQUIRED_PERMISSIONS = "required_permissions";
    public static final int REQUEST_CODE = 1000;
    public static final int QVA_REQUEST_CODE = 1001;

    public static boolean requestRuntimePermissions(Activity activity) {
        if (PermissionUtils.hasQVARecordAudioPermission(activity)) {
            if (!AppPermissionUtils.isRuntimePermissionsGranted(activity)) {
                AppPermissionUtils.requestClientPermission(activity);
            } else {
                return false;
            }
        } else {
            PermissionUtils.requestQVAPermission(activity, QVA_REQUEST_CODE);
        }
        return true;
    }

    public static boolean isRuntimePermissionsGranted(Context context) {
        String[] requestPermissions = filterRequestPermissions(context);
        return requestPermissions.length == 0;
    }

    private static String[] filterRequestPermissions(Context context) {
        boolean isPermissionGranted;

        ArrayList<String> permissionList = new ArrayList<>();
        for (String item : RUNTIME_PERMISSIONS) {
            isPermissionGranted = (PackageManager.PERMISSION_GRANTED ==
                    context.checkSelfPermission(item));
            if (!isPermissionGranted) {
                permissionList.add(item);
            }
        }

        String[] requestPermissions = new String[permissionList.size()];
        permissionList.toArray(requestPermissions);
        return requestPermissions;
    }

    private static void requestClientPermission(Activity activity) {
        String[] requestPermissions = filterRequestPermissions(activity);
        if (requestPermissions.length > 0) {
            Intent intent = new Intent(activity, PermissionActivity.class);
            intent.putExtra(EXTRA_KEY_REQUIRED_PERMISSIONS, RUNTIME_PERMISSIONS);
            activity.startActivityForResult(intent, REQUEST_CODE);
        }
    }
}
