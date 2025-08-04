/*====*====*====*====*====*====*====*====*====*====*====*====*====*====*====*
  Copyright (c) 2021,2024 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
=============================================================================*/
package com.qualcomm.location.izat.altitudereceiver;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.RemoteException;
import android.util.Log;
import android.os.Handler;
import android.os.Message;

import com.qti.altitudereceiver.*;

import com.qualcomm.location.izat.IzatService;
import com.qualcomm.location.izat.GTPClientHelper;
import com.qualcomm.location.utils.IZatServiceContext;

import java.util.LinkedList;
import java.util.List;

public class AltitudeReceiver implements IzatService.ISystemEventListener, Handler.Callback {
    private static final String TAG = "AltitudeReceiver";
    private static final boolean VERBOSE = Log.isLoggable(TAG, Log.VERBOSE);

    private final Context mContext;
    private Handler mHandler;

    private static final int ALTITUDE_LOOK_UP = IZatServiceContext.MSG_ALTITUDE_RECEIVER +1;
    private static final int REPORT_ALTITUDE = IZatServiceContext.MSG_ALTITUDE_RECEIVER +2;
    private static final int ALTITUDE_LOOK_UP_TIME_OUT = IZatServiceContext.MSG_ALTITUDE_RECEIVER +3;
    private static final int SET_ALTITUDE_CB = IZatServiceContext.MSG_ALTITUDE_RECEIVER +4;

    // If Z Provider doesn't response in 5 sencods, return latest fix to FLP
    private static final int ALTITUDE_LOOK_UP_TIMEOUT_IN_MS = 5000;
    // Modem E-911 callflow will not accept CPI injection with horizontal unc
    // more than 200 meters of 90% confidence, which is 136.0 meters of 68% confidence
    private static final float LOC_ACCURACY_THRESHOLD = 136.0f;

    private volatile IAltitudeReceiverResponseListener mAltitudeReceiverResponseListener = null;
    private PendingIntent mListenerIntent = null;
    private static final Object sCallBacksLock = new Object();

    private IAltitudeReportCb mAltitudeReportCb;
    private Location mLocationUsedInQuery = null;
    private Location mLatestLocation = null;

    private static AltitudeReceiver sInstance = null;
    public synchronized static AltitudeReceiver getInstance(Context ctx) {
        if (sInstance == null) {
            sInstance = new AltitudeReceiver(ctx);
        }
        return sInstance;
    }

    private AltitudeReceiver(Context ctx) {
        if (VERBOSE) {
            Log.d(TAG, "AltitudeReceiver construction");
        }
        mContext = ctx;
        mHandler = new Handler(IZatServiceContext.getInstance(mContext).getLooper(), this);
    }

    @Override
    public void onAidlClientDied(String packageName, int pid, int uid) {
        Log.d(TAG, "aidl client crash: " + packageName);
        synchronized (sCallBacksLock) {
            mAltitudeReceiverResponseListener = null;
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        int msgID = msg.what;
        Location loc;

        if (mAltitudeReceiverResponseListener == null) {
            Log.d(TAG, "mAltitudeReceiverResponseListener null, ignore msg " + msgID);
            return false;
        }

        switch(msgID) {
            case ALTITUDE_LOOK_UP:
                loc = (Location)msg.obj;
                Log.d(TAG, "Altitude look up request, location: " + loc.toString()  +
                           ", loc utc timestamp: " + loc.getTime() +
                           ", Previous query in progress: " + (mLocationUsedInQuery != null));

                if (mAltitudeReportCb != null && loc.hasAccuracy() &&
                        (loc.getAccuracy() <= LOC_ACCURACY_THRESHOLD)) {
                    mLatestLocation = loc;
                    // Fire altitude look up request if no ongoing request and loc accuracy is good
                    // if accuracy is good, E-911 will not accept the injection
                    // mLocationUsedInQuery indicates there is no ongoing request
                    if (mLocationUsedInQuery == null) {
                        mLocationUsedInQuery = loc;
                        //Remove the altitude info before sending to 3rd party Z-Provider
                        mLocationUsedInQuery.removeAltitude();
                        mLocationUsedInQuery.removeVerticalAccuracy();
                        boolean isEmergency = (msg.arg1 == 1);

                        synchronized (sCallBacksLock) {
                            try {
                                mAltitudeReceiverResponseListener
                                        .onAltitudeLookupRequest(mLocationUsedInQuery, isEmergency);
                            } catch (RemoteException e) {
                                Log.e(TAG, "error happens when getting altitude, sending intent");
                                GTPClientHelper.SendPendingIntent(mContext, mListenerIntent,
                                                                  "AltitudeReceiver");
                            }
                        }

                        //5s timer to handle potential time out of this request
                        mHandler.sendMessageDelayed(
                            mHandler.obtainMessage(ALTITUDE_LOOK_UP_TIME_OUT, loc),
                            ALTITUDE_LOOK_UP_TIMEOUT_IN_MS);
                        }
                    }
                    break;
            case REPORT_ALTITUDE:
                loc = (Location)msg.obj;
                //If mLocationUsedInQuery is null or timestamp of loc returned from ZProvider
                //is different from mLocationUsedInQuery, just ignore;
                //Else if ZProvider returned loc with valid altitude and altitude accuracy
                //set altitude and altitude accuracy to mLocationUsedInQuery and report
                //mLocationUsedInQuery to FLP;
                //Else if ZProvider returned loc without valid altitude
                //report mLatestLocation to FLP.
                if (mLocationUsedInQuery == null || mLocationUsedInQuery.getElapsedRealtimeNanos()
                        != loc.getElapsedRealtimeNanos()) {
                    Log.d(TAG, "timestamp of loc returned from ZProvider mismatch!");
                } else if (loc.hasAltitude() && loc.hasVerticalAccuracy()) {
                    mLocationUsedInQuery.setAltitude(loc.getAltitude());
                    mLocationUsedInQuery.setVerticalAccuracyMeters(loc.getVerticalAccuracyMeters());
                    reportAltitude(mLocationUsedInQuery, true);
                } else if (mLatestLocation != null) {
                    reportAltitude(mLatestLocation, false);
                }
                break;
            case ALTITUDE_LOOK_UP_TIME_OUT:
                loc = (Location)msg.obj;
                // report mLatestLocation to FLP when time out
                // Compare timestamp of loc and mLocationUsedInQuery to check if
                // It is a real time out case, as the timeout timer is not cancelled
                // for successful lookup case
                if ((mLocationUsedInQuery != null) &&
                        (mLocationUsedInQuery.getElapsedRealtimeNanos() ==
                            loc.getElapsedRealtimeNanos()) &&
                         (mLatestLocation != null)) {
                    Log.d(TAG, "Altitude look up request time out, report latest location");
                    reportAltitude(mLatestLocation, false);
                }
                break;
            case SET_ALTITUDE_CB:
                mLatestLocation = null;
                mLocationUsedInQuery = null;
                mAltitudeReportCb = (IAltitudeReportCb)msg.obj;
                if (mAltitudeReportCb != null) {
                    Log.d(TAG, "AltitudeReceiver SetCallback to none-null");
                } else {
                    Log.d(TAG, "AltitudeReceiver SetCallback to null");
                }
                break;
            default:
                break;
        }
        return true;
    }

    //If 3rd party Z-Provider returns invalid altitude or timeout
    //for 10 continuous fixes, stop to query Altitude in 60s and then recover
    private void reportAltitude(Location loc, boolean status) {
        Log.d(TAG, "reportAltitude, location: " + loc.toString()  +
                   "loc timestamp: " + loc.getTime() + "loc altitude: " + loc.getAltitude() +
                   ", status: " + status);

        if (mAltitudeReportCb != null) {
            mAltitudeReportCb.onLocationReportWithAlt(loc, status);
        } else {
            Log.d(TAG, "mAltitudeReportCb is null");
        }

        mLocationUsedInQuery = null;
    }

    public interface IAltitudeReportCb {
        void onLocationReportWithAlt(Location location, boolean isAltitudeAvail);
    }

    public void startAltitudeQuery(IAltitudeReportCb callback) {
        mHandler.obtainMessage(SET_ALTITUDE_CB, callback).sendToTarget();
    }

    public void stopAltitudeQuery() {
        mHandler.obtainMessage(SET_ALTITUDE_CB, null).sendToTarget();
    }

    public boolean isPresent() {
        return !(mAltitudeReceiverResponseListener == null);
    }

    public void getAltitudeFromLocation(Location loc, boolean isEmergency) {
        if (mAltitudeReportCb != null) {
            mHandler.obtainMessage(ALTITUDE_LOOK_UP, isEmergency ? 1:0, 0, loc).sendToTarget();
        }
    }

    /* Remote binder */
    private final IAltitudeReceiver.Stub mBinder = new IAltitudeReceiver.Stub() {

        @Override
        public boolean registerResponseListener(IAltitudeReceiverResponseListener callback,
                android.app.PendingIntent notifyIntent) throws android.os.RemoteException {
            Log.d(TAG, "registerResponseListener");
            if (callback == null) {
                Log.e(TAG, "callback is null");
                return false;
            }

            if (notifyIntent == null) {
                Log.w(TAG, "notifyIntent is null");
            }

            synchronized (sCallBacksLock) {
                if (null != mAltitudeReceiverResponseListener) {
                    Log.e(TAG, "Response listener already provided.");
                    return false;
                }
                mAltitudeReceiverResponseListener = callback;
                mListenerIntent = notifyIntent;
            }
            return true;
        }

        @Override
        public boolean removeResponseListener(IAltitudeReceiverResponseListener callback)
                throws android.os.RemoteException {
            Log.d(TAG, "removeResponseListener");
            synchronized (sCallBacksLock) {
                mAltitudeReceiverResponseListener = null;
                mListenerIntent = null;
            }
            return true;
        }

        @Override
        public void pushAltitude(Location location) throws android.os.RemoteException {
            Log.d(TAG, "pushAltitude, location: " + location.toString());
            mHandler.obtainMessage(REPORT_ALTITUDE, location).sendToTarget();
        }
    };


    public IAltitudeReceiver getAltitudeReceiverBinder() {
        return mBinder;
    }
}
