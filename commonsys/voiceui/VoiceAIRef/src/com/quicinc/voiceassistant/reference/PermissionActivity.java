/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.quicinc.voiceassistant.reference;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;

import com.quicinc.voiceassistant.reference.util.AppPermissionUtils;

final public class PermissionActivity extends Activity {
    private final static String TAG = PermissionActivity.class.getSimpleName();
    private final static int REQUEST_CODE = 10001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        String[] requiredPermissions = getIntent().getStringArrayExtra(
                AppPermissionUtils.EXTRA_KEY_REQUIRED_PERMISSIONS);
        if (AppPermissionUtils.isRuntimePermissionsGranted(this)) {
            setResultAndFinish();
        } else {
            requestPermissions(requiredPermissions, REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,  String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        setResultAndFinish();
    }

    private void setResultAndFinish(){
        setResult(REQUEST_CODE);
        finish();
    }
}
