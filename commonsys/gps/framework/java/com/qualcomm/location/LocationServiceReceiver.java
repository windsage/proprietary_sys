/*
 *                     Location Service Reciever
 *
 * GENERAL DESCRIPTION
 *   This file is the receiver for the ACTION SHUTDOWN
 *
 * Copyright (c) 2013-2014 Qualcomm Atheros, Inc.
 * All Rights Reserved.
 * Qualcomm Atheros Confidential and Proprietary.
 *
 *  Copyright (c) 2012-2015, 2022, 2024 Qualcomm Technologies, Inc.
 *  All Rights Reserved.
 *  Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.location;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.os.UserHandle;
import com.qualcomm.location.izat.IzatService;
import android.location.LocationManager;
import android.content.IntentFilter;

public class LocationServiceReceiver extends BroadcastReceiver {
    private static final String TAG = "LocationServiceReceiver";
    private static boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);
    private boolean mLocationEnabledStatus;

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            String intentAction = intent.getAction();
            
            if (intentAction != null) {
                if(DEBUG) {
                    Log.d(TAG, "Received: " + intentAction);
                }
                switch (intentAction) {
                    case Intent.ACTION_LOCKED_BOOT_COMPLETED:
                        setLocationBootupStatus(context);
                        installModeChangeReceiver(context);
                    case Intent.ACTION_BOOT_COMPLETED:
                        Intent intentLocationService = new Intent(context, LocationService.class);
                        Log.d(TAG, "to launchIzatService: ");
                        launchIzatService(context);
                        Log.d(TAG, "to startLocationService: ");
                        context.startServiceAsUser(intentLocationService, UserHandle.SYSTEM);
                        break;
                    case LocationManager.MODE_CHANGED_ACTION:
                        mLocationEnabledStatus = intent.getBooleanExtra
                            (LocationManager.EXTRA_LOCATION_ENABLED, false);
                        Log.d(TAG, "location mode changed to: " + mLocationEnabledStatus +
                                ", izatService running: " + IzatService.sIsRunning);
                        launchIzatService(context);
                        break;
                    default:
                        Log.d(TAG, "unknown action: " + intentAction);

                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setLocationBootupStatus(Context context) {
        LocationManager locMgr = (LocationManager)context.
            getSystemService(Context.LOCATION_SERVICE);
        mLocationEnabledStatus = locMgr.isLocationEnabledForUser(UserHandle.SYSTEM);
        Log.i(TAG, "Bootup Location status: " + mLocationEnabledStatus);
    }
    private void installModeChangeReceiver(Context context) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LocationManager.MODE_CHANGED_ACTION);
        context.getApplicationContext().registerReceiver(this, intentFilter, null, null);
        Log.i(TAG, "Location change intent registered");
    }
    private void launchIzatService(Context context) {
        Log.i(TAG, "Location Enabled: " + mLocationEnabledStatus + ", IzatService.sIsRunning: " +
                IzatService.sIsRunning);
        if (mLocationEnabledStatus && !IzatService.sIsRunning) {
            Intent intentIzatService = new Intent(context, IzatService.class);
            intentIzatService.setAction("com.qualcomm.location.izat.IzatService");
            context.startServiceAsUser(intentIzatService, UserHandle.SYSTEM);
        }
    }
}
