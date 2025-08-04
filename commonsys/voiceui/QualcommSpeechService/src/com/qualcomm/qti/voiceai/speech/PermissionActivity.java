/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.voiceai.speech;

import android.Manifest;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;

public class PermissionActivity extends Activity {

    public final static String[] REQUEST_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
    };

    public static final String RECORD_AUDIO = Manifest.permission.RECORD_AUDIO;

    private static final int REQUEST_CODE = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermissions(REQUEST_PERMISSIONS, REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        setResultAndFinish();
    }

    private void setResultAndFinish() {
        setResult(RESULT_OK, null);
        finish();
    }

}
