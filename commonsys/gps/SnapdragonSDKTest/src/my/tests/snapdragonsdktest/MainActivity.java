/*====*====*====*====*====*====*====*====*====*====*====*====*====*====*====*
  Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
=============================================================================*/

package my.tests.snapdragonsdktest;

import android.Manifest;
import android.app.TabActivity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

import java.util.ArrayList;

public class MainActivity extends TabActivity {
    private static String TAG = "SnapdragonSDKTest";
    private static final boolean VERBOSE = Log.isLoggable(TAG, Log.VERBOSE);
    private boolean mIsInitialized = false;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        boolean permissionGranted = false;
        Log.d(TAG, "ininit" + mIsInitialized);

        if (Build.VERSION.SDK_INT >= 23) {
            // if SDK version > 23, check for required runtime permission.
            String[] operationPermissionNames = getOperationPermissionName();

            if (operationPermissionNames == null ||
                        checkOperationPermission(operationPermissionNames)) {
                permissionGranted = true;
            }
            else {
                // if permission not granted, don't continue
                permissionGranted = false;
            }
        }
        else {
            permissionGranted = true;
        }

        if (permissionGranted) {
            doInit();
        }
    }

    private void doInit() {
        if (mIsInitialized == true) {
            return;
        }

        TabHost tabHost=getTabHost();
        // no need to call TabHost.Setup()

        //First Tab
        TabSpec spec1=tabHost.newTabSpec("DebugReport");
        spec1.setIndicator("DebugReport");
        Intent in1=new Intent(this, DebugReportActivity.class);
        spec1.setContent(in1);

        TabSpec spec2=tabHost.newTabSpec("Geofence");
        spec2.setIndicator("Geofence");
        Intent in2=new Intent(this, FullscreenActivity.class);
        spec2.setContent(in2);

        TabSpec spec3=tabHost.newTabSpec("FLP");
        spec3.setIndicator("FLP");
        Intent in3=new Intent(this, FlpActivity.class);
        spec3.setContent(in3);

        TabSpec spec4=tabHost.newTabSpec("XTRA");
        spec4.setIndicator("XTRA");
        Intent in4=new Intent(this, XtraThrottleActivity.class);
        spec4.setContent(in4);

        TabSpec spec5=tabHost.newTabSpec("GTP");
        spec5.setIndicator("GTP");
        Intent in5=new Intent(this, GtpActivity.class);
        spec5.setContent(in5);

        TabSpec spec6=tabHost.newTabSpec("Wwan");
        spec6.setIndicator("Wwan");
        Intent in6=new Intent(this, GtpWwanActivity.class);
        spec6.setContent(in6);

        TabSpec spec7=tabHost.newTabSpec("Gnss");
        spec7.setIndicator("Gnss");
        Intent in7=new Intent(this, GtpOemConsentActivity.class);
        spec7.setContent(in7);

        TabSpec spec8=tabHost.newTabSpec("GnssConfig");
        spec8.setIndicator("GnssConfig");
        Intent in8=new Intent(this, GnssConfigActivity.class);
        spec8.setContent(in8);

        TabSpec spec9=tabHost.newTabSpec("PrecisePosition");
        spec9.setIndicator("PrecisePosition");
        Intent in9=new Intent(this, PrecisePositionActivity.class);
        spec9.setContent(in9);

        tabHost.addTab(spec1);
        tabHost.addTab(spec2);
        tabHost.addTab(spec3);
        tabHost.addTab(spec4);
        tabHost.addTab(spec5);
        tabHost.addTab(spec6);
        tabHost.addTab(spec7);
        tabHost.addTab(spec8);
        tabHost.addTab(spec9);

        mIsInitialized = true;
    }

    private boolean checkOperationPermission(String[] permissionName) {
        ArrayList<String> needRequestPermission = new ArrayList<String>();
        for (String tmp : permissionName) {
            if (!(PackageManager.PERMISSION_GRANTED == checkSelfPermission(tmp))) {
                needRequestPermission.add(tmp);
            }
        }

        if (needRequestPermission.size() == 0) {
            return true;
        } else {
            String[] needRequestPermissionArray = new String[needRequestPermission.size()];
            needRequestPermission.toArray(needRequestPermissionArray);
            requestPermissions(needRequestPermissionArray, 0);
            return false;
        }
    }

    private String[] getOperationPermissionName() {
        return new String[]{ Manifest.permission.ACCESS_COARSE_LOCATION};
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (permissions == null || grantResults == null ||
            permissions.length == 0 || grantResults.length == 0) {
            return;
        }

        boolean bPass = true;
        for (int i = 0 ; i < permissions.length ; i++) {
            Log.d(TAG, "PermissionsResult: " + permissions[i] + ":" + grantResults[i]);
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                bPass = false;
            }
        }
        if(!bPass) {
            finish();
        } else {
            doInit();
        }
    }
}
