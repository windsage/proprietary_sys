/*====*====*====*====*====*====*====*====*====*====*====*====*====*====*====*
  Copyright (c) 2021-2022 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
=============================================================================*/

package com.qti.services.zprovidertest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.lang.Runnable;
import java.lang.Thread;

import com.qti.location.sdk.*;
import com.qti.location.sdk.IZatAltitudeReceiver;
import com.qti.location.sdk.IZatAltitudeReceiver.IZatAltitudeReceiverResponseListener;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.util.Log;
import android.os.Environment;
import android.os.SystemClock;

public class ZProviderService extends Service {
    private IZatManager mIZM;
    private IZatAltitudeReceiver mAltReceiver;
    private ZProviderListener mAltReqListener;

    public static final String TAG = "ZProviderTest";

    private static class testData {
        float altitude;
        int processTimeInMs;
    }

    private ArrayList<testData> testDataList = new ArrayList<testData>();
    private int dataCursor = 0;

    @Override
    public void onCreate() {
        Log.v(TAG, "onBSNewCreate");
        mIZM = IZatManager.getInstance(getApplicationContext());

        Intent activitIntent = new Intent(getApplicationContext(), ZProviderService.class);
        PendingIntent zproviderServiceIntent = PendingIntent.getService(getApplicationContext(), 0,
                activitIntent, PendingIntent.FLAG_MUTABLE);
        mAltReqListener = new ZProviderListener();
        mAltReceiver = mIZM.connectToAltitudeReceiver(mAltReqListener, zproviderServiceIntent);
        String path = getExternalFilesDir(null).getAbsolutePath() + "/zaxis.txt";
        readFile(path);
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy");
        mIZM.disconnectFromAltitudeReceiver(mAltReceiver);
    }

    @Override
    public int onStartCommand (Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            Log.v(TAG, "onStartCommand with intent action: " + action);
        } else {
            Log.v(TAG, "onStartCommand NO intent");
        }

        return Service.START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void readFile(String fileName) {
        try {
            File f = new File(fileName);
            FileInputStream fileIS = new FileInputStream(f);
            BufferedReader buf = new BufferedReader(new InputStreamReader(fileIS));
            String readString = new String();

            // begin file reading
            while((readString = buf.readLine()) != null){
                String[] parts = readString.split("\\s+");
                testData data = new testData();
                data.altitude = Float.valueOf(parts[0]);
                data.processTimeInMs = Integer.valueOf(parts[1]);
                testDataList.add(data);
            }
            Log.d(TAG, testDataList.size() + " Read lines");
        } catch (Exception e){
            Log.e(TAG, "Error: " + e.getMessage());
        }

    }

    class ZProviderListener implements IZatAltitudeReceiverResponseListener {
        @Override
        public void onAltitudeLookupRequest(Location location, boolean isEmergency) {
            Log.d(TAG, "onAltitudeLookupRequest: Location [" + location.toString()
                    + "], isEmergency " + isEmergency);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (testDataList.size() > 0) {
                            testData data = testDataList.get(dataCursor);
                            Log.d(TAG, "altitude = " + data.altitude
                                    + " processTime = " + data.processTimeInMs);
                            dataCursor = (dataCursor + 1) % testDataList.size();
                            float altitude = data.altitude;
                            int processTime = data.processTimeInMs;
                            if (altitude != 0.0f && processTime != 0) {
                                //Case 1, normal case
                                try {
                                    Thread.sleep(processTime);
                                } catch (InterruptedException e) {}
                                location.setAltitude(altitude);
                                // hard coded vertical accuracy
                                location.setVerticalAccuracyMeters(2.6f);
                                Log.d(TAG, "PushAltitude: " + altitude + " processTime:"
                                        + processTime + " location: " + location.toString());
                                mAltReceiver.pushAltitude(location);
                            } else if (altitude == 0.0f && processTime != 0) {
                                //Case 2, altitude invalid, push the original location
                                try {
                                    Thread.sleep(processTime);
                                } catch (InterruptedException e) {}
                                Log.d(TAG, "Push invalid Altitude, processTime:"
                                        + processTime + " location: " + location.toString());
                                location.setElapsedRealtimeNanos(
                                        SystemClock.elapsedRealtimeNanos());
                                mAltReceiver.pushAltitude(location);
                            } else if (processTime == 0) {
                                //Case 3, processTime = 0, do nothing
                                Log.d(TAG, "No response for altitude request");
                            }
                        }
                    }}).start();
        }
    }

};

