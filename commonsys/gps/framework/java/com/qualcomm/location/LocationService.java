
/*====*====*====*====*====*====*====*====*====*====*====*====*====*====*====*
  Copyright (c) 2013, 2018, 2020-2022, 2024 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.

  2013 Qualcomm Atheros, Inc.

  All Rights Reserved.
  Qualcomm Atheros Confidential and Proprietary.

  Not a Contribution, Apache license notifications and
  license are retained for attribution purposes only.
=============================================================================*/

/*
 * Copyright (C) 2008 The Android Open Source Project
 * Copyright (c) 2011,2012, The Linux Foundation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qualcomm.location;

import java.io.IOException;
import java.io.StringReader;
import java.net.InetAddress;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import android.provider.Telephony.Carriers;
import android.database.sqlite.SQLiteException;

import com.qualcomm.location.GpsNetInitiatedHandler;
import com.qualcomm.location.GpsNetInitiatedHandler.GpsNiNotification;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.location.INetInitiatedListener;
import android.telephony.TelephonyManager;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.NetworkInfo;
import android.net.Network;
import android.net.NetworkRequest;
import android.net.NetworkCapabilities;
import android.net.TelephonyNetworkSpecifier;
import android.net.Uri;
import android.net.LinkProperties;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.util.NtpTrustedTime;
import com.qualcomm.location.geocoder.GeocoderProxy;

import com.qualcomm.location.izatprovider.IzatProvider;
import com.qualcomm.location.idlclient.LocIDLClientBase;
import com.qualcomm.location.idlclient.IDLClientUtils;
import com.qualcomm.location.utils.IZatServiceContext;

import com.qualcomm.location.idlclient.*;
import com.qualcomm.location.idlclient.LocIDLClientBase.*;
import vendor.qti.gnss.ILocAidlGnss;
import vendor.qti.gnss.ILocAidlAGnss;
import vendor.qti.gnss.ILocAidlGnssNi;
import vendor.qti.gnss.ILocAidlAGnssCallback;
import vendor.qti.gnss.ILocAidlGnssNiCallback;
import vendor.qti.gnss.LocAidlAGnssSubId;
import android.location.LocationManager;
import android.os.UserHandle;
import com.qualcomm.location.izat.IzatService;

public class LocationService extends Service implements Handler.Callback {
    private static final String TAG = "LocSvc_java";
    private static boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    //Timeout for ConnectivityManager.requestNetwork
    //The modem timeout is 30 seconds. We'll try to send back a response
    //before it times out
    private static final int NETWORK_REQUEST_TIMEOUT_MS = 1000 * 10;
    //Network states passed from network callback
    private static final int NETWORK_UNAVAILABLE = 0;
    private static final int NETWORK_AVAILABLE   = 1;

    // these need to match GpsApgsStatusValue defines in gps.h
    /** AGPS status event values. */
    private static final int GPS_REQUEST_AGPS_DATA_CONN = 1;
    private static final int GPS_RELEASE_AGPS_DATA_CONN = 2;
    private static final int GPS_AGPS_DATA_CONNECTED = 3;
    private static final int GPS_AGPS_DATA_CONN_DONE = 4;
    private static final int GPS_AGPS_DATA_CONN_FAILED = 5;

    // Handler messages
    private static final int ENABLE = IZatServiceContext.MSG_LOCATION_SERVICE_BASE + 2;
    private static final int HANDLE_NETWORK_CALLBACK =
            IZatServiceContext.MSG_LOCATION_SERVICE_BASE + 3;
    private static final int REPORT_AGPS_STATUS = IZatServiceContext.MSG_LOCATION_SERVICE_BASE + 4;

    public static final String IS_SSR = "ssr";

    // true if we are enabled, protected by this
    private AtomicBoolean mEnabled = new AtomicBoolean(false);

    private Context mContext;
    private IZatServiceContext mIZatServiceCtx;

    private GpsNetInitiatedHandler mNIHandler;

    private String mDefaultApn;

    // Wakelocks
    private final static String WAKELOCK_KEY = "LocationService";
    private PowerManager.WakeLock mWakeLock;

    private Handler mHandler;
    private LocationServiceIDLClient mLocationServiceIDLClient;

    private GeocoderProxy mGeocoder;
    //This class is used in sendMessage() to indicate whether or not a wakelock
    //is held when sending the message.
    private static class LocSvcMsgObj {
        Object obj;
        boolean wakeLockHeld;
        public LocSvcMsgObj (Object obj, Boolean wakeLock) {
            this.obj = obj;
            this.wakeLockHeld = wakeLock;
        }
    };

    @Override
    public void onCreate() {
        if (DEBUG) {
            Log.d(TAG, "onCreate ");
        }

        mContext = this;
        mGeocoder = new GeocoderProxy(mContext);
        mIZatServiceCtx = IZatServiceContext.getInstance(mContext);
        mHandler = new Handler(mIZatServiceCtx.getLooper(), this);

        mNIHandler = new GpsNetInitiatedHandler(mContext, mNetInitiatedListener);

        // Create a wake lock
        PowerManager powerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_KEY);
        mWakeLock.setReferenceCounted(true);


        if (LocIDLClientBase.getIDLServiceVersion().compareTo(IDLServiceVersion.V_AIDL) >= 0) {
            mLocationServiceIDLClient = new LocationServiceIDLClient(this);
            enable();
        } else {
            Log.e(TAG, "ILoc AIDL is not supported!");
        }
    }

    @Override
    public  int onStartCommand (Intent intent, int flags, int startId) {
        LocationManager locMgr = (LocationManager)mContext.getSystemService(
                Context.LOCATION_SERVICE);
        // when Location SDK ssr, need to clean ServiceRecord first and then start
        if (intent == null && !IzatService.sIsRunning &&
                locMgr.isLocationEnabledForUser(UserHandle.SYSTEM)) {
            Log.i(TAG, "ssr case, to restart IZatService");
            Intent intentIzatService = new Intent(mContext, IzatService.class);
            stopService(intentIzatService);
            intentIzatService.setAction("com.qualcomm.location.izat.IzatService");
            intentIzatService.putExtra(LocationService.IS_SSR, true);
            mContext.startServiceAsUser(intentIzatService, UserHandle.SYSTEM);
        }
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void sendMessage(int message, int arg1, int arg2, Object obj) {
        // hold a wake lock until this message is delivered
        // note that this assumes the message will not be removed from the queue before
        // it is handled (otherwise the wake lock would be leaked).
        if(DEBUG) {
            Log.d(TAG, "Sending msg: "+ message + " arg1:"+ arg1 +" arg2:"+ arg2);
        }
        mWakeLock.acquire();
        //wakeLockHeld in LocSvcMsgObj is set to true to indicate that
        //a wakelock is held before sending this message
        mHandler.obtainMessage(message, arg1, arg2,
                               new LocSvcMsgObj(obj, true)).sendToTarget();
    }

    private void sendMessage(int message, int arg, Object obj) {
        sendMessage(message, arg, 1, obj);
    }

    @Override
    public boolean handleMessage(Message msg) {
        int message = msg.what;
        LocSvcMsgObj msgObj=null;
        if (msg.obj != null) {
            msgObj = (LocSvcMsgObj)msg.obj;
        }
        switch (message) {
        case ENABLE:
            if (msg.arg1 == 1) {
                handleEnable();
            } else {
                handleDisable();
            }
            break;
        case REPORT_AGPS_STATUS:
            if(msgObj != null) {
                handleReportAgpsStatus((ReportAgpsStatusMessage)msgObj.obj);
            }
            break;
        case HANDLE_NETWORK_CALLBACK:
            if(msgObj != null) {
                handleNetworkCallback(msg.arg1, msg.arg2, (Network)msgObj.obj);
            }
            break;
        }
        // if wakelock was taken for this message, release it
        if((msgObj != null) && (msgObj.wakeLockHeld)) {
            mWakeLock.release();
        }
        return true;
    }

    private void handleNetworkCallback(int state, int connType, Network network) {
        ConnectivityManager connMgr = (ConnectivityManager)mContext.
                getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo info = connMgr.getNetworkInfo(network);

        if (DEBUG) {
            Log.d(TAG, "handleNetworkCallback connTyp: "+ connType + " state: " +state+
                  " network:" +network+ " info:" +info);
        }

        AGpsConnectionInfo agpsConnInfo=getAGpsConnectionInfo(connType);
        if (agpsConnInfo != null) {
            if (agpsConnInfo.mState == AGpsConnectionInfo.STATE_OPENING) {
                if(state == NETWORK_UNAVAILABLE) {
                    if (DEBUG) Log.d(TAG, "call native_agps_data_conn_failed");
                    agpsConnInfo.mAPN = null;
                    agpsConnInfo.release(connType, false);
                    agpsConnInfo.mState = AGpsConnectionInfo.STATE_CLOSED;
                } else {
                    if (DEBUG) Log.d(TAG, "run thread to collect agpsConnInfo");

                    agpsConnInfo.getApn(info);
                    agpsConnInfo.getBearerType(info);
                    Log.d(TAG, "handleAgpsConnOpen mAgpsType: " + agpsConnInfo.mAgpsType +
                        " mAPN: " + agpsConnInfo.mAPN + " mBearerType: "
                        + agpsConnInfo.mBearerType);
                    mLocationServiceIDLClient.agpsDataConnOpen(
                        agpsConnInfo.mAgpsType,
                        agpsConnInfo.mAPN,
                        agpsConnInfo.mBearerType);
                    agpsConnInfo.mState = AGpsConnectionInfo.STATE_OPEN;
                }
            }
            else if(DEBUG) {
                Log.d(TAG, "agpsConnInfo.mState:"+agpsConnInfo.mState);
            }
        }
    }

    public void enable() {
        sendMessage(ENABLE, 1, null);
    }

    private void handleEnable() {
        if (DEBUG) Log.d(TAG, "handleEnable, mEnabled: " + mEnabled.get());
        if (!mEnabled.compareAndSet(false, true)) {
            return;
        }

        boolean enabled = mLocationServiceIDLClient.init();

        if (!enabled) {
            mEnabled.set(false);
            Log.w(TAG, "Failed to enable location provider");
        }
    }

    public void disable() {
        sendMessage(ENABLE, 0, null);
    }

    private void handleDisable() {
        if (DEBUG) Log.d(TAG, "handleDisable, mEnabled: " + mEnabled.get());
        if (!mEnabled.compareAndSet(true, false)) {
            return;
        }

        // do this before releasing wakelock
        mLocationServiceIDLClient.cleanUp();
    }

    public boolean isEnabled() {
        return mEnabled.get();
    }

    /**
     * called from native code to update AGPS status
     */
    public void reportAGpsStatus(int type, int apnTypeMask, int status, int subId) {
        if (DEBUG) Log.d(TAG, "reportAGpsStatus with type = " + type +
                              " apnTypeMask = " + apnTypeMask +
                              " status = " + status +
                              " subId = " + subId);
        ReportAgpsStatusMessage rasm = new ReportAgpsStatusMessage(type,
                                                                   apnTypeMask,
                                                                   status,
                                                                   subId);

        LocSvcMsgObj obj = new LocSvcMsgObj((Object)rasm, false);
        Message msg = new Message();
        msg.what = REPORT_AGPS_STATUS;
        msg.obj = obj;

        mHandler.sendMessage(msg);
    }

    private void handleReportAgpsStatus(ReportAgpsStatusMessage rasm) {
        int type = rasm.type;
        int apnTypeMask = rasm.apnTypeMask;
        int status = rasm.status;
        int subId = rasm.subId;

        if (DEBUG) Log.d(TAG, "handleReportAgpsStatus with type = " + type +
                              " apnTypeMask = " + apnTypeMask +
                              " status = " + status +
                              " subId = " + subId);

        AGpsConnectionInfo agpsConnInfo = getAGpsConnectionInfo(type);
        if (agpsConnInfo == null) {
            if (DEBUG) Log.d(TAG, "reportAGpsStatus agpsConnInfo is null for type "+type);
            // we do not handle this type of connection
            return;
        }

        switch (status) {
            case GPS_REQUEST_AGPS_DATA_CONN:
                if (DEBUG) Log.d(TAG, "GPS_REQUEST_AGPS_DATA_CONN");

                switch (type) {
                case AGpsConnectionInfo.CONNECTION_TYPE_SUPL:
                case AGpsConnectionInfo.CONNECTION_TYPE_C2K:
                case AGpsConnectionInfo.CONNECTION_TYPE_WWAN_ANY:
                case AGpsConnectionInfo.CONNECTION_TYPE_WIFI:
                case AGpsConnectionInfo.CONNECTION_TYPE_SUPL_ES:
                {
                    if (agpsConnInfo.mState == AGpsConnectionInfo.STATE_OPEN) {
                        Log.e(TAG, "Connect in OPEN state! return current conn info directly!");
                        Log.d(TAG, "handleAgpsConnOpen mAgpsType: " + agpsConnInfo.mAgpsType +
                            " mAPN: " + agpsConnInfo.mAPN + " mBearerType: "
                            + agpsConnInfo.mBearerType);
                        mLocationServiceIDLClient.agpsDataConnOpen(
                            agpsConnInfo.mAgpsType,
                            agpsConnInfo.mAPN,
                            agpsConnInfo.mBearerType);
                    } else {
                        if (DEBUG) {
                            Log.d(TAG, "type: "+type);
                        }
                        agpsConnInfo.connect(type, apnTypeMask, subId);
                    }
                    break;
                }
                default:
                    if (DEBUG) Log.e(TAG, "type == unknown");
                    break;
                }
                break;
            case GPS_RELEASE_AGPS_DATA_CONN: {
                if (DEBUG) Log.d(TAG, "GPS_RELEASE_AGPS_DATA_CONN");

                switch (type) {
                case AGpsConnectionInfo.CONNECTION_TYPE_SUPL:
                case AGpsConnectionInfo.CONNECTION_TYPE_C2K:
                case AGpsConnectionInfo.CONNECTION_TYPE_WWAN_ANY:
                case AGpsConnectionInfo.CONNECTION_TYPE_WIFI:
                case AGpsConnectionInfo.CONNECTION_TYPE_SUPL_ES:
                {
                    if (agpsConnInfo.mState == AGpsConnectionInfo.STATE_CLOSED) {
                        Log.e(TAG, "Release in CLOSED state !");
                    } else {
                        agpsConnInfo.release(type, true);
                        agpsConnInfo.mState = AGpsConnectionInfo.STATE_CLOSED;
                    }
                    break;
                }
                default:
                    if (DEBUG) Log.e(TAG, "GPS_RELEASE_AGPS_DATA_CONN but current network state is unknown!");
                    return;
                }
                break;
            }
            case GPS_AGPS_DATA_CONNECTED:
                if (DEBUG) Log.d(TAG, "GPS_AGPS_DATA_CONNECTED");
                break;
            case GPS_AGPS_DATA_CONN_DONE:
                if (DEBUG) Log.d(TAG, "GPS_AGPS_DATA_CONN_DONE");
                break;
            case GPS_AGPS_DATA_CONN_FAILED:
                if (DEBUG) Log.d(TAG, "GPS_AGPS_DATA_CONN_FAILED");
                break;
        }
    }

    //=============================================================
    // NI Client support
    //=============================================================
    private final INetInitiatedListener mNetInitiatedListener = new INetInitiatedListener.Stub() {
        // Sends a response for an NI reqeust to HAL.
        @Override
        public boolean sendNiResponse(int notificationId, int userResponse)
        {
            // TODO Add Permission check

            if (DEBUG) Log.d(TAG, "sendNiResponse, notifId: " + notificationId +
                    ", response: " + userResponse);
            mLocationServiceIDLClient.sendNiResponse(notificationId, userResponse);
            return true;
        }
    };

    // Called by JNI function to report an NI request.
    public void reportNiNotification(
            int notificationId,
            int niType,
            int notifyFlags,
            int timeout,
            int defaultResponse,
            String requestorId,
            String text,
            int requestorIdEncoding,
            int textEncoding,
            String extras,  // Encoded extra data
            boolean esEnabled
        )
    {
        Log.i(TAG, "reportNiNotification: entered");
        Log.i(TAG, "notificationId: " + notificationId +
                ", niType: " + niType +
                ", notifyFlags: " + notifyFlags +
                ", timeout: " + timeout +
                ", defaultResponse: " + defaultResponse);

        Log.i(TAG, "requestorId: " + requestorId +
                ", text: " + text +
                ", requestorIdEncoding: " + requestorIdEncoding +
                ", textEncoding: " + textEncoding);

        GpsNiNotification notification = new GpsNiNotification();

        notification.notificationId = notificationId;
        notification.niType = niType;
        notification.needNotify = (notifyFlags & GpsNetInitiatedHandler.GPS_NI_NEED_NOTIFY) != 0;
        notification.needVerify = (notifyFlags & GpsNetInitiatedHandler.GPS_NI_NEED_VERIFY) != 0;
        notification.privacyOverride = (notifyFlags & GpsNetInitiatedHandler.GPS_NI_PRIVACY_OVERRIDE) != 0;
        notification.timeout = timeout;
        notification.defaultResponse = defaultResponse;
        notification.requestorId = requestorId;
        notification.text = text;
        notification.requestorIdEncoding = requestorIdEncoding;
        notification.textEncoding = textEncoding;

        // Process extras, assuming the format is
        // one of more lines of "key = value"
        Bundle bundle = new Bundle();

        if (extras == null) extras = "";
        Properties extraProp = new Properties();

        try {
            extraProp.load(new StringReader(extras));
        }
        catch (IOException e)
        {
            Log.e(TAG, "reportNiNotification cannot parse extras data: " + extras);
        }

        for (Entry<Object, Object> ent : extraProp.entrySet())
        {
            bundle.putString((String) ent.getKey(), (String) ent.getValue());
        }

        notification.extras = bundle;

        mNIHandler.handleNiNotification(notification, esEnabled);
    }

    private String getDefaultApn() {
        Uri uri = Uri.parse("content://telephony/carriers/preferapn");
        String apn = null;

        try {
            Cursor cursor =
                mContext.getContentResolver().query(uri, new String[] {"apn"},
                                                    null, null, Carriers.DEFAULT_SORT_ORDER);
            if (null != cursor) {
                try {
                    if (cursor.moveToFirst()) {
                        apn = cursor.getString(0);
                    }
                } finally {
                    cursor.close();
                }
            }
        } catch (SQLiteException e) {
            Log.e(TAG, "SQLiteException on mContext.getContentResolver().query");
        } catch (Exception e) {
            Log.e(TAG, "Unknown exception"+e+" on mContext.getContentResolver().query");
        }

        if (apn == null) {
            apn = "dummy-apn";
        }

        if (DEBUG) Log.d(TAG, "getDefaultApn() returns: " + apn);
        return apn;
    }

    private static AGpsConnectionInfo[] mAGpsConnections = new AGpsConnectionInfo[4];
    private AGpsConnectionInfo getAGpsConnectionInfo(int connType) {
        if (DEBUG) Log.d(TAG, "getAGpsConnectionInfo connType - "+connType);
        switch (connType)
        {
        case AGpsConnectionInfo.CONNECTION_TYPE_WWAN_ANY:
        case AGpsConnectionInfo.CONNECTION_TYPE_C2K:
            if (null == mAGpsConnections[0])
                mAGpsConnections[0] = new AGpsConnectionInfo(
                        ConnectivityManager.TYPE_MOBILE, connType);
            return mAGpsConnections[0];
        case AGpsConnectionInfo.CONNECTION_TYPE_SUPL:
            if (null == mAGpsConnections[1])
                mAGpsConnections[1] = new AGpsConnectionInfo(
                        ConnectivityManager.TYPE_MOBILE_SUPL, connType);
            return mAGpsConnections[1];
        case AGpsConnectionInfo.CONNECTION_TYPE_WIFI:
            if (null == mAGpsConnections[2])
                mAGpsConnections[2] = new AGpsConnectionInfo(
                        ConnectivityManager.TYPE_WIFI, connType);
            return mAGpsConnections[2];
        case AGpsConnectionInfo.CONNECTION_TYPE_SUPL_ES:
            if (null == mAGpsConnections[3])
                mAGpsConnections[3] = new AGpsConnectionInfo(
                        ConnectivityManager.TYPE_MOBILE_HIPRI, connType);
            return mAGpsConnections[3];
        default:
            return null;
        }
    }

    private final class LocNetworkCallback extends NetworkCallback {
        private int connType;

        @Override
        public void onAvailable(Network network) {
            super.onAvailable(network);
            if (DEBUG) Log.d(TAG, "OnAvailable for: "+ connType);
            sendMessage(HANDLE_NETWORK_CALLBACK, NETWORK_AVAILABLE, connType, network);
        }

        @Override
        public void onUnavailable() {
            super.onUnavailable();
            if (DEBUG) Log.d(TAG, "OnUnavailable for: "+ connType);
            sendMessage(HANDLE_NETWORK_CALLBACK, NETWORK_UNAVAILABLE, connType, null);
        }

        @Override
        public void onLinkPropertiesChanged(Network network, LinkProperties lp) {
            super.onLinkPropertiesChanged(network,lp);
            if (DEBUG) {
                 Log.d(TAG, "onLinkPropertiesChanged type:" + connType +
                       " IPv4:" + lp.isIpv4Provisioned() + "IPv6:" + lp.isIpv6Provisioned());
            }
        }

        public int getConnType() {
            if (DEBUG) Log.d(TAG, "getConnType for: "+ connType);
            return connType;
        }

        public LocNetworkCallback(int type) {
            super();
            connType = type;
            if (DEBUG) Log.d(TAG, "New LocNetworkCallback for: "+ connType);
        }
    }

    private class AGpsConnectionInfo {
        // these need to match AGpsType enum in gps.h
        private static final int CONNECTION_TYPE_ANY = 0;
        private static final int CONNECTION_TYPE_SUPL = 1;
        private static final int CONNECTION_TYPE_C2K = 2;
        private static final int CONNECTION_TYPE_WWAN_ANY = 3;
        private static final int CONNECTION_TYPE_WIFI = 4;
        private static final int CONNECTION_TYPE_SUPL_ES = 5;

        // this must match the definition of LocAidlApnType
        private static final int BEARER_INVALID = 0;
        private static final int BEARER_IPV4 = 1;
        private static final int BEARER_IPV6 = 2;
        private static final int BEARER_IPV4V6 = 3;

        // for mState
        private static final int STATE_CLOSED = 0;
        private static final int STATE_OPENING = 1;
        private static final int STATE_OPEN = 2;
        private static final int STATE_KEEP_OPEN = 3;

        // SUPL vs ANY (which really is non-SUPL)
        private final int mCMConnType;
        private final int mAgpsType;
        private final String mPHConnFeatureStr;
        private String mAPN;
        private int mIPvVerType;
        private int mState;
        private InetAddress mIpAddr;
        private int mBearerType;
        //Callback to send to ConnectivityManager
        private ConnectivityManager.NetworkCallback mNetworkCallback;

        private AGpsConnectionInfo(int connMgrConnType, int agpsType) {
            mCMConnType = connMgrConnType;
            mAgpsType = agpsType;
            if (ConnectivityManager.TYPE_MOBILE_SUPL == connMgrConnType) {
                mPHConnFeatureStr = "enableSUPL";
            } else {
                mPHConnFeatureStr = "enableHIPRI";
            }
            mAPN = null;
            mState = STATE_CLOSED;
            mIpAddr = null;
            mBearerType = BEARER_INVALID;
        }
        private AGpsConnectionInfo(AGpsConnectionInfo info) {
            this.mCMConnType = info.mCMConnType;
            this.mAgpsType = info.mAgpsType;
            this.mPHConnFeatureStr = info.mPHConnFeatureStr;
            this.mAPN = info.mAPN;
            this.mIPvVerType = info.mIPvVerType;
            this.mState = info.mState;
            this.mIpAddr = info.mIpAddr;
            this.mBearerType = info.mBearerType;
        }
        private int getAgpsType() {
            return mAgpsType;
        }
        private int getCMConnType() {
            return mCMConnType;
        }
        private InetAddress getIpAddr() {
            return mIpAddr;
        }
        private String getApn(NetworkInfo info) {

            if (info != null) {
                mAPN = info.getExtraInfo();
            }
            if (mAPN == null) {
                /* We use the value we read out from the database. That value itself
                   is default to "dummy-apn" if no value from database. */
                mDefaultApn = getDefaultApn();
                mAPN = mDefaultApn;
            }

            if (DEBUG) Log.d(TAG, "getApn(): " + mAPN);
            return mAPN;
        }
        private int getBearerType(NetworkInfo info) {
            if (mAPN == null) {
                mAPN = getApn(info);
            }
            String ipProtocol = null;
            TelephonyManager phone = (TelephonyManager)
                    mContext.getSystemService(Context.TELEPHONY_SERVICE);

            // get IP protocol here
            int networkType = phone.getDataNetworkType();
            String selection = null;
            if (TelephonyManager.NETWORK_TYPE_EHRPD == networkType) {
                selection = "apn = '" + mAPN + "'";
                selection += " and type LIKE '%supl%'";
            } else if (TelephonyManager.NETWORK_TYPE_UNKNOWN == networkType) {
                // UNKNOWN means no sim or not camped, so will not have current = 1
                selection = "apn = '" + mAPN + "'";
                selection += " and carrier_enabled = 1";
                if (CONNECTION_TYPE_SUPL_ES == mAgpsType) {
                    selection += " and type LIKE '%emergency%'";
                }
            } else {
                selection = "current = 1";
                selection += " and apn = '" + mAPN + "'";
                selection += " and carrier_enabled = 1";
            }

            try {
                Cursor cursor =
                    mContext.getContentResolver().query(Carriers.CONTENT_URI,
                                                        new String[] {Carriers.PROTOCOL},
                                                        selection, null,
                                                        Carriers.DEFAULT_SORT_ORDER);
                if (null != cursor) {
                    try {
                        if (cursor.moveToFirst()) {
                            ipProtocol = cursor.getString(0);
                        }
                    } finally {
                        cursor.close();
                    }
                }
            } catch (SQLiteException e) {
                Log.e(TAG, "SQLiteException on mContext.getContentResolver().query");
            } catch (Exception e) {
                Log.e(TAG, "Unknown exception"+e+" on mContext.getContentResolver().query");
            }
            Log.d(TAG, "ipProtocol: " + ipProtocol + " apn: " + mAPN +
                  " networkType: " + phone.getNetworkTypeName() + " state: " + mState);

            if (null == ipProtocol) {
                Log.w(TAG, "ipProtocol not found in db, default to ipv4v6");
                mBearerType = BEARER_IPV4V6;
            } else if (ipProtocol.equals("IPV6")) {
                mBearerType = BEARER_IPV6;
            } else if (ipProtocol.equals("IPV4V6")) {
                mBearerType = BEARER_IPV4V6;
            } else if (ipProtocol.equals("IPV4")) {
                mBearerType = BEARER_IPV4;
            } else {
                Log.e(TAG, "ipProtocol value not expected, default to ipv4v6");
                mBearerType = BEARER_IPV4V6;
            }

            return mBearerType;
        }

        private void connect(int connType, int apnTypeMask, int subId) {
            final ConnectivityManager connMgr =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (DEBUG) {
                Log.d(TAG, "connect() type: " + connType + " apnTypeMask:" +
                      apnTypeMask + " subId " + subId);
            }

            if(mNetworkCallback == null) {
                mNetworkCallback = new LocNetworkCallback(connType);
            }
            NetworkRequest.Builder requestBuilder = new NetworkRequest.Builder();
            requestBuilder.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
            if (((apnTypeMask & LocationServiceIDLClient.APN_TYPE_MASK_DEFAULT) != 0) ||
                ((apnTypeMask & LocationServiceIDLClient.APN_TYPE_MASK_HIPRI) != 0)) {
                requestBuilder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            }
            if ((apnTypeMask & LocationServiceIDLClient.APN_TYPE_MASK_IMS) != 0) {
                requestBuilder.addCapability(NetworkCapabilities.NET_CAPABILITY_IMS);
            }
            if ((apnTypeMask & LocationServiceIDLClient.APN_TYPE_MASK_MMS) != 0) {
                requestBuilder.addCapability(NetworkCapabilities.NET_CAPABILITY_MMS);
            }
            if ((apnTypeMask & LocationServiceIDLClient.APN_TYPE_MASK_DUN) != 0) {
                requestBuilder.addCapability(NetworkCapabilities.NET_CAPABILITY_DUN);
            }
            if ((apnTypeMask & LocationServiceIDLClient.APN_TYPE_MASK_SUPL) != 0) {
                requestBuilder.addCapability(NetworkCapabilities.NET_CAPABILITY_SUPL);
            }
            if ((apnTypeMask & LocationServiceIDLClient.APN_TYPE_MASK_FOTA) != 0) {
                requestBuilder.addCapability(NetworkCapabilities.NET_CAPABILITY_FOTA);
            }
            if ((apnTypeMask & LocationServiceIDLClient.APN_TYPE_MASK_CBS) != 0) {
                requestBuilder.addCapability(NetworkCapabilities.NET_CAPABILITY_CBS);
            }
            if ((apnTypeMask & LocationServiceIDLClient.APN_TYPE_MASK_IA) != 0) {
                requestBuilder.addCapability(NetworkCapabilities.NET_CAPABILITY_IA);
            }
            if ((apnTypeMask & LocationServiceIDLClient.APN_TYPE_MASK_EMERGENCY) != 0) {
                requestBuilder.addCapability(NetworkCapabilities.NET_CAPABILITY_EIMS);
            }

            SubscriptionManager subManager = mContext.getSystemService(SubscriptionManager.class);

            int mySid = -1;
            if (subId > 0) {
                // If subId > 0, indicates subId is specified from lower layer
                mySid = subManager.getSubscriptionId(subId - 1);
            } else {
                // If subId = 0, indicates subId is not specified from lower layer
                // Try to use default data sub ID
                mySid = subManager.getDefaultDataSubscriptionId();
            }

            if (mySid >= 0) {
                TelephonyNetworkSpecifier.Builder netBuilder =
                        new TelephonyNetworkSpecifier.Builder();
                netBuilder.setSubscriptionId(mySid);
                TelephonyNetworkSpecifier netSpecifier = netBuilder.build();
                requestBuilder.setNetworkSpecifier(netSpecifier);
            } else {
                if (DEBUG) Log.d(TAG, "getSubscriptionIds returns null, SIMless device?");
            }
            if (DEBUG) Log.d(TAG, "mySid: " + mySid);

            NetworkRequest request = requestBuilder.build();
            try {
                connMgr.requestNetwork(request, mNetworkCallback,
                                       NETWORK_REQUEST_TIMEOUT_MS);
            } catch (Exception e) {
                Log.e(TAG, "exception getting in requestNetwork: " + e);
                release(mAgpsType, false);
                mState = AGpsConnectionInfo.STATE_CLOSED;
            }
            mState = AGpsConnectionInfo.STATE_OPENING;
        }

        private void release(int connType, boolean success) {
            final ConnectivityManager connMgr =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (DEBUG) Log.d(TAG, "release() type: "+connType);
            if(mNetworkCallback != null) {
                try {
                    connMgr.unregisterNetworkCallback(mNetworkCallback);
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Exception in unregister NetworkCallback !");
                }
                if(success) {
                    mLocationServiceIDLClient.agpsDataConnClosed(connType);
                }
                else {
                    mLocationServiceIDLClient.agpsDataConnFailed(connType);
                }
            }
        }
    }

    private class ReportAgpsStatusMessage {
        int type;
        int apnTypeMask;
        int status;
        int subId;

        public ReportAgpsStatusMessage(int type, int apnTypeMask, int status, int subId) {
            this.type = type;
            this.apnTypeMask = apnTypeMask;
            this.status = status;
            this.subId = subId;
        }
    }

    /* =================================================
     *   AIDL Client
     * =================================================*/
    public class LocationServiceIDLClient extends LocIDLClientBase
            implements LocIDLClientBase.IServiceDeathCb {
        private final String TAG = "LocationServiceIDLClient";
        public static final int APN_TYPE_MASK_DEFAULT =    0x0001;
        public static final int APN_TYPE_MASK_IMS =        0x0002;
        public static final int APN_TYPE_MASK_MMS =        0x0004;
        public static final int APN_TYPE_MASK_DUN =        0x0008;
        public static final int APN_TYPE_MASK_SUPL =       0x0010;
        public static final int APN_TYPE_MASK_HIPRI =      0x0020;
        public static final int APN_TYPE_MASK_FOTA =       0x0040;
        public static final int APN_TYPE_MASK_CBS =        0x0080;
        public static final int APN_TYPE_MASK_IA =         0x0100;
        public static final int APN_TYPE_MASK_EMERGENCY =  0x0200;
        private ILocAidlGnssNi sGnssNi;
        private ILocAidlAGnss sAGnss;
        private LocationService mLocationServiceProvider;
        private GnssNiCallback serviceNiCallback = new GnssNiCallback();
        private LocAGnssCallback mLocAGnssCallback = new LocAGnssCallback();

        private LocationServiceIDLClient(LocationService service) {
            mLocationServiceProvider = service;
            registerServiceDiedCb(this);
        }

        private ILocAidlGnssNi getGnssNiIface() {
            if (null == sGnssNi) {
                ILocAidlGnss service = (ILocAidlGnss)getGnssAidlService();
                if (service == null) {
                    Log.e(TAG, "ILocAidlGnss handle is null");
                    return null;
                }

                try {
                    sGnssNi = service.getExtensionLocAidlGnssNi();
                    if (null == sGnssNi) {
                        Log.e(TAG, "ILocAidlGnssNi handle is null");
                        return null;
                    }
                    Log.d(TAG, "getGnssNiIface, sGnssNi=" + sGnssNi);
                } catch (RemoteException e) {
                    Log.e(TAG, "Exception getting GnssNi service extension: " + e);
                }
            }
            return sGnssNi;
        }

        private ILocAidlAGnss getAGnssIface() {
            ILocAidlGnss service = (ILocAidlGnss)getGnssAidlService();
            if (sAGnss == null) {
                try {
                    if (null == service) {
                        Log.e(TAG, "ILocAidlGnss handle is null");
                    } else {
                        ILocAidlAGnss agnss = service.getExtensionLocAidlAGnss();
                        Log.d(TAG, "getAGnssIface with getExtensionLocAidlAGnss, agnss=" +
                                agnss + " gnss = " + service);
                        if (null == agnss) {
                            Log.e(TAG, "ILocAidlAGnss handle is null");
                        }
                        sAGnss = agnss;
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, "Exception getting AGnss service extension: " + e);
                }
            }
            return sAGnss;
        }

        public boolean init() {
            IDLClientUtils.toIDLService(TAG);
            getGnssAidlService();
            ILocAidlGnssNi niIface = getGnssNiIface();
            if (niIface == null) {
                Log.e(TAG, "NULL GnssNi Iface");
                return false;
            }
            ILocAidlAGnss agnssIface = getAGnssIface();
            if (agnssIface == null) {
                Log.e(TAG, "NULL AGnssVendor Iface");
                return false;
            }
            try {
                if (serviceNiCallback == null) {
                    serviceNiCallback = new GnssNiCallback();
                }
                niIface.setVendorCallback(serviceNiCallback);

                if (sAGnss != null) {
                    if (mLocAGnssCallback == null) {
                        mLocAGnssCallback = new LocAGnssCallback();
                    }
                    sAGnss.setCallbackExt(mLocAGnssCallback);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Exception setting Callback for AGnss/GnssNi: " + e);
            }

            return true;
        }

        public void cleanUp() {
            IDLClientUtils.toIDLService(TAG);
        }

        public void agpsDataConnOpen(int agpsType, String apn, int bearerType) {
            IDLClientUtils.toIDLService(TAG);

            ILocAidlAGnss iface = getAGnssIface();
            if (iface == null) {
                Log.e(TAG, "AGNSS Iface NULL");
                return;
            }
            if (apn == null) {
                Log.e(TAG, "NULL APN");
                throw new IllegalArgumentException( "java/lang/IllegalArgumentException, null Apn");
            }

            try {
                boolean result = iface.dataConnOpenExt(
                        apn, (byte)bearerType, (byte)agpsType);
            } catch (RemoteException e) {
                Log.e(TAG, "Exception dataConnOpenExt: " + e);
            }
        }
        public void agpsDataConnClosed(int agpsType) {
            IDLClientUtils.toIDLService(TAG);

            ILocAidlAGnss iface = getAGnssIface();
            if (iface == null) {
                Log.e(TAG, "AGNSS Iface NULL");
                return;
            }

            try {
                boolean result = iface.dataConnClosedExt((byte)agpsType);
            } catch (RemoteException e) {
                Log.e(TAG, "Exception dataConnClosedExt: " + e);
            }
        }
        public void agpsDataConnFailed(int agpsType) {
            IDLClientUtils.toIDLService(TAG);

            ILocAidlAGnss iface = getAGnssIface();
            if (iface == null) {
                Log.e(TAG, "AGNSS Iface NULL");
                return;
            }

            try {
                boolean result = iface.dataConnFailedExt((byte)agpsType);
            } catch (RemoteException e) {
                Log.e(TAG, "Exception dataConnFailedExt: " + e);
            }
        }
        public void sendNiResponse(int notifId, int response) {
            IDLClientUtils.toIDLService(TAG);

            ILocAidlGnssNi iface = getGnssNiIface();
            if (iface == null) {
                Log.e(TAG, "GNSS NI Iface NULL");
                return;
            }

            try {
                iface.respond(notifId, (byte)response);
            } catch (RemoteException e) {
                Log.e(TAG, "Exception respond: " + e);
            }
        }

        @Override
        public void onServiceDied() {
            Log.e(TAG, "ILocAidlLocationService died");
            sGnssNi = null;
            sAGnss = null;
            getAGnssIface();
            init();
        }

        /* =================================================
         *   AIDL Callbacks : ILocAidlGnssNiCallback.hal
         * =================================================*/
        private class GnssNiCallback extends ILocAidlGnssNiCallback.Stub {
            @Override
            public void niNotifyCbExt(
                    vendor.qti.gnss.LocAidlGnssNiNotification gnssNiNotification)
                    throws android.os.RemoteException {
                IDLClientUtils.fromIDLService(TAG);
                mLocationServiceProvider.reportNiNotification(gnssNiNotification.notificationId,
                        gnssNiNotification.niType, gnssNiNotification.notifyFlags,
                        (int)gnssNiNotification.timeoutSec, gnssNiNotification.defaultResponse,
                        gnssNiNotification.requestorId, gnssNiNotification.notificationMessage,
                        gnssNiNotification.requestorIdEncoding,
                        gnssNiNotification.notificationIdEncoding,
                        gnssNiNotification.extras, gnssNiNotification.esEnabled);

            }

            @Override
            public void gnssCapabilitiesCb(int capabilitiesBitMask)
                    throws android.os.RemoteException {
            }
            @Override
            public final int getInterfaceVersion() {
                return ILocAidlGnssNiCallback.VERSION;
            }
            @Override
            public final String getInterfaceHash() {
                return ILocAidlGnssNiCallback.HASH;
            }
        }


        /* =================================================
         *   AIDL Callbacks : ILocAidlAGnssCallback.hal
         * =================================================*/
        private class LocAGnssCallback extends ILocAidlAGnssCallback.Stub {
            // Methods from ::vendor::qti::gnss::ILocAidlAGnssCallback follow.
            @Override
            public void locAidlAgnssStatusIpV4Cb(vendor.qti.gnss.LocAidlAGnssStatusIpV4 status)
                    throws android.os.RemoteException {
                IDLClientUtils.fromIDLService(TAG);
                mLocationServiceProvider.reportAGpsStatus(status.type, status.apnTypeMask,
                        status.status, status.subId);
            }

            @Override
            public void locAidlAgnssStatusIpV6Cb(vendor.qti.gnss.LocAidlAGnssStatusIpV6 status)
                    throws android.os.RemoteException {
                IDLClientUtils.fromIDLService(TAG);
            }
            @Override
            public final int getInterfaceVersion() {
                return ILocAidlAGnssCallback.VERSION;
            }
            @Override
            public final String getInterfaceHash() {
                return ILocAidlGnssNiCallback.HASH;
            }
        }
    }
}
