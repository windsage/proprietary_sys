/*====*====*====*====*====*====*====*====*====*====*====*====*====*====*====*
  Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
  All rights reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.

  Not a Contribution, Apache license notifications and
  license are retained for attribution purposes only.
=============================================================================*/
/*
 * Copyright (C) 2012 The Android Open Source Project
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
package com.qualcomm.location.izatprovider;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.WorkSource;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import java.util.*;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import android.location.provider.LocationProviderBase;
import android.location.provider.ProviderRequest;
import android.location.provider.ProviderProperties;

import com.qualcomm.location.izat.flp.FlpServiceProvider;
import com.qualcomm.location.izat.altitudereceiver.*;
import com.qualcomm.location.izat.esstatusreceiver.EsStatusReceiver;
import com.qualcomm.location.izat.IzatService;
import com.qualcomm.location.LocationService;
import com.qti.flp.IFlpService;
import com.qti.flp.ILocationCallback;

import java.util.concurrent.atomic.AtomicBoolean;
import com.qualcomm.location.utils.IZatServiceContext;
import static com.qualcomm.location.utils.IZatServiceContext.MSG_IZAT_PROVIDER_BASE;

import com.qualcomm.location.idlclient.*;
import com.qualcomm.location.idlclient.LocIDLClientBase.*;
import vendor.qti.gnss.ILocAidlGnss;
import vendor.qti.gnss.ILocAidlIzatProvider;
import vendor.qti.gnss.ILocAidlIzatProviderCallback;
import vendor.qti.gnss.LocAidlIzatLocation;
import vendor.qti.gnss.LocAidlIzatRequest;
import vendor.qti.gnss.LocAidlIzatStreamType;
import vendor.qti.gnss.LocAidlIzatHorizontalAccuracy;
import vendor.qti.gnss.LocAidlNetworkPositionSourceType;
import vendor.qti.gnss.LocAidlAltitudeRefType;
import vendor.qti.gnss.LocAidlIzatRequestType;
import vendor.qti.gnss.LocAidlWwanAppInfo;

import com.qualcomm.location.policy.SessionRequest;
import com.qualcomm.location.izat.UserConsentManager;
import com.qualcomm.location.policy.SessionRequest.RequestPrecision;
import com.qualcomm.location.policy.SessionRequest.SessionType;
import com.qualcomm.location.izat.UserConsentManager.WwanAppInfo;


public class IzatProvider extends LocationProviderBase implements Handler.Callback {
    // member constants
    private static final int MSG_ENABLE_PROVIDER      = MSG_IZAT_PROVIDER_BASE + 1;
    private static final int MSG_DISABLE_PROVIDER     = MSG_IZAT_PROVIDER_BASE + 2;
    private static final int MSG_SET_REQUEST          = MSG_IZAT_PROVIDER_BASE + 3;
    private static final int MSG_LOCATION_CHANGED     = MSG_IZAT_PROVIDER_BASE + 4;
    private static final int MSG_STATUS_CHANGED       = MSG_IZAT_PROVIDER_BASE + 5;
    private static final int MSG_DESTROY_ENGINE       = MSG_IZAT_PROVIDER_BASE + 6;
    private static final int MSG_INIT                 = MSG_IZAT_PROVIDER_BASE + 7;
    private static final int MSG_SET_SESSION_REQUEST  = MSG_IZAT_PROVIDER_BASE + 8;
    private static final int MSG_SET_MULTIPLEX_REQUEST  = MSG_IZAT_PROVIDER_BASE + 9;

    private static final int GNSS_SESSION_BEGIN         = 4;
    private static final int GNSS_SESSION_END           = 5;
    private static final int GNSS_ENGINE_ON             = 6;
    private static final int GNSS_ENGINE_OFF            = 7;

    static final String NLP_TYPE_KEY                    = "networkLocationType";
    static final String ALT_REF_TYPE_KEY                = "altitudeRefType";

    // These get set in JNI at the time of class laoding.
    private static int IZAT_STREAM_FUSED = LocAidlIzatStreamType.FUSED;
    private static int IZAT_STREAM_NETWORK = LocAidlIzatStreamType.NETWORK;

    private static IzatProvider sNetwkProvider;
    private static IzatProvider sFusedProvider;

    private IzatSessionMultiplexer mWifiMultiplexer;
    private IzatSessionMultiplexer mWwanMultiplexer;
    private IZatServiceContext mIZatServiceCtx;
    private final boolean DBG;
    private final String TAG;
    private final String NAME;
    private final boolean mHighPowerCapable;
    private final int mIzatProviderType;
    private Handler mHandler;
    private AppOpsManager mAppOpsMgr;
    private WorkSource mActiveWorkSource;
    private boolean mNavigating;

    private IzatProviderClient mProviderClient;
    private final Context mAppContext;
    private static Context sContext;
    private AtomicBoolean mUserConsent = new AtomicBoolean(false);
    private static int sFusedRequestCnt;

    private WwanAppInfo mLastSentAppInfo;

    // Wwan App Info Multiplex result Listener Interface
    public interface WwanAppInfoMultiplexResultListener {
        // WWAN App Info Multiplex completed, handle result
        public void onWwanAppInfoListMultiplexed(WwanAppInfo multiplexedAppInfo);
    }
    private final List<WwanAppInfoMultiplexResultListener> mWwanAppInfoMultiplexListeners =
            new ArrayList<WwanAppInfoMultiplexResultListener>();

    // Register a listener for multiplex result
    public void registerWwanAppInfoMultiplexResultListener(
            WwanAppInfoMultiplexResultListener listener) {
        if (!mWwanAppInfoMultiplexListeners.contains(listener)) {
            mWwanAppInfoMultiplexListeners.add(listener);
        }
    }
    private void notifyWwanAppInfoMultiplexResultListeners(WwanAppInfo multiplexedAppInfo) {

        for (WwanAppInfoMultiplexResultListener listener: mWwanAppInfoMultiplexListeners) {
            listener.onWwanAppInfoListMultiplexed(multiplexedAppInfo);
        }
    }

    public void onLoad() {
        Log.d(TAG,"location provider loaded");
        mHandler.obtainMessage(MSG_ENABLE_PROVIDER).sendToTarget();
    }

    public void onUnload() {
        Log.d(TAG,"location provider unloaded");
        mHandler.obtainMessage(MSG_DISABLE_PROVIDER).sendToTarget();
    }

    @Override
    public void onSetRequest(ProviderRequest request) {
        mHandler.obtainMessage(MSG_SET_REQUEST, request).sendToTarget();
    }

    public void onSetSessionRequest(SessionRequest request) {
        mHandler.obtainMessage(MSG_SET_SESSION_REQUEST, request).sendToTarget();
    }

    public void onSetMultiplexRequest(List<WwanAppInfo> appInfoList) {
        mHandler.obtainMessage(MSG_SET_MULTIPLEX_REQUEST, appInfoList).sendToTarget();
    }

    @Override
    public void onSendExtraCommand(String command, Bundle extras) {}
    @Override
    public void onFlush(OnFlushCompleteCallback callback) {}

    // called from aidl client
    private void onLocationChanged(Location[] location) {
        for (Location loc: location) {
            logLocation("onLocationChanged:", loc);
        }
        mHandler.obtainMessage(MSG_LOCATION_CHANGED, location).sendToTarget();
    }

    private void logLocation(String msg, Location location) {

        String emptyStr = "";
        log(msg + " UTC Time: " + location.getTime() +
            "; Elapsed Real Time Nanos: " + location.getElapsedRealtimeNanos() +
            "; Latitude: " + location.getLatitude() +"; Longitude: " + location.getLongitude() +
            "; Accuracy: " + (location.hasAccuracy() ? location.getAccuracy() : emptyStr) +
            "; Altitude: " + (location.hasAltitude() ? location.getAltitude() : emptyStr) +
            "; Vertical Unc: " +
            (location.hasVerticalAccuracy() ? location.getVerticalAccuracyMeters(): emptyStr) +
            "; Speed: " + (location.hasSpeed() ? location.getSpeed() : emptyStr) +
            "; Speed Unc " +
            (location.hasSpeedAccuracy() ? location.getSpeedAccuracyMetersPerSecond(): emptyStr) +
            "; Bearing: " + (location.hasBearing() ? location.getBearing() : emptyStr) +
            "; Bearing Unc " +
            (location.hasBearingAccuracy() ? location.getBearingAccuracyDegrees(): emptyStr) +
            "; positionSource: " + emptyStr +
            "; providerName: " + NAME);
    }

    private Location toLocation(long utcTime,
                                long elapsedRealTimeNanos, double latitude,
                                double longitude, boolean hasAltitude,
                                double altitude, boolean hasVerticalUnc,
                                float verticalUnc, boolean hasSpeed,
                                float speed,boolean hasSpeedUnc,
                                float speedUnc, boolean hasBearing,
                                float bearing, boolean hasBearingUnc,
                                float bearingUnc, boolean hasAccuracy,
                                float accuracy, short positionSource,
                                boolean hasAltRefType, short altRefType) {

        Location location = new Location(NAME);

        location.setTime(utcTime);
        location.setElapsedRealtimeNanos(elapsedRealTimeNanos);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        if (hasAltitude) location.setAltitude(altitude);
        if (hasSpeed) location.setSpeed(speed);
        if (hasBearing) location.setBearing(bearing);
        if (hasAccuracy) location.setAccuracy(accuracy);
        if (hasVerticalUnc) location.setVerticalAccuracyMeters(verticalUnc);
        if (hasSpeedUnc) location.setSpeedAccuracyMetersPerSecond(speedUnc);
        if (hasBearingUnc) location.setBearingAccuracyDegrees(bearingUnc);
        location.makeComplete();

        Bundle extras = new Bundle();
        switch (positionSource) {
        case 0:
            extras.putString(NLP_TYPE_KEY, "wwan");
            break;
        case 1:
            extras.putString(NLP_TYPE_KEY, "wifi_rssi");
            break;
        case 2:
            extras.putString(NLP_TYPE_KEY, "wifi_rtt_server");
            break;
        case 3:
            extras.putString(NLP_TYPE_KEY, "wifi_rtt_ftm");
            break;
        }

        if (hasAltRefType) {
            switch (altRefType) {
                case 0:
                    extras.putString(ALT_REF_TYPE_KEY, "unknown");
                    break;
                case 1:
                    extras.putString(ALT_REF_TYPE_KEY, "ref_wgs84");
                    break;
                case 2:
                    extras.putString(ALT_REF_TYPE_KEY, "ref_msl");
                    break;
                case 3:
                    extras.putString(ALT_REF_TYPE_KEY, "ref_agl");
                    break;
                case 4:
                    extras.putString(ALT_REF_TYPE_KEY, "ref_floor_level");
                    break;
                default:
                    Log.e(TAG, "Invalid type");
                }
        }
        location.setExtras(extras);
        if (mIzatProviderType == IZAT_STREAM_NETWORK) {
            // Network locations must have a noGPSLocation extra parameter
            Bundle extras1 = location.getExtras();
            if (extras1 == null) {
                extras1 = new Bundle();
            }
            extras1.putParcelable(Location.EXTRA_NO_GPS_LOCATION, new Location(location));
            location.setExtras(extras1);
        } else {
            // else we need add an extras bundle to this location. It is a little wasting this
            // way because Location makes a copy of the pass in bundle. So this Bundle is to
            // be dereferenced as soon as the call is done.
            location.setExtras(new Bundle());
        }

        return location;
    }

    // called from idlclient
    private void onStatusChanged (int status) {
        mHandler.obtainMessage(MSG_STATUS_CHANGED, status).sendToTarget();
    }

    public static synchronized boolean hasNetworkProvider() {
        return sNetwkProvider != null;
    }

    public static IzatProvider getNetworkProvider(Context ctx) {
        Log.d("IzatProvider", "getNetworkProvider");
        synchronized(IzatProvider.class) {
            if (sNetwkProvider == null) {
                ProviderProperties properties = new ProviderProperties.Builder()
                        .setHasNetworkRequirement(true)
                        .setHasAltitudeSupport(true)
                        .setHasSpeedSupport(true)
                        .setHasBearingSupport(true)
                        .setPowerUsage(ProviderProperties.POWER_USAGE_LOW)
                        .setAccuracy(ProviderProperties.ACCURACY_COARSE)
                        .build();
                sNetwkProvider = new IzatProvider(
                        ctx, IZAT_STREAM_NETWORK, LocationManager.NETWORK_PROVIDER, properties);
                Log.d("IzatProvider", "getNetworkProvider finish");
            }
        }
        return sNetwkProvider;
    }
    public static IzatProvider getFusedProvider(Context ctx) {
        Log.d("IzatProvider", "getFusedProvider");
        synchronized(IzatProvider.class) {
            if (sFusedProvider == null) {
                ProviderProperties properties = new ProviderProperties.Builder()
                        .setHasNetworkRequirement(true)
                        .setHasAltitudeSupport(true)
                        .setHasSpeedSupport(true)
                        .setHasBearingSupport(true)
                        .setPowerUsage(ProviderProperties.POWER_USAGE_LOW)
                        .setAccuracy(ProviderProperties.ACCURACY_FINE)
                        .build();
                sFusedProvider = new IzatProvider(
                        ctx, IZAT_STREAM_FUSED, LocationManager.FUSED_PROVIDER, properties);
                Log.d("IzatProvider", "getFusedProvider finish");
            }
        }
        return sFusedProvider;
    }

    private IzatProvider(Context ctx, int providerType, String providerName,
                         ProviderProperties properties) {
        super(ctx, "IzatProvider_"+providerName, properties);
        boolean firstInit = false;
        TAG = "IzatProvider_"+providerName;
        DBG = Log.isLoggable(TAG, Log.DEBUG);
        NAME = providerName;
        mIzatProviderType = providerType;
        mHighPowerCapable = (mIzatProviderType == IZAT_STREAM_FUSED);
        sContext = ctx;
        mAppContext = ctx.getApplicationContext();
        mHandler = new Handler(IZatServiceContext.getInstance(sContext).getLooper(), this);
        mHandler.obtainMessage(MSG_INIT).sendToTarget();
        mIZatServiceCtx = IZatServiceContext.getInstance(sContext);
    }

    @Override
    public boolean handleMessage (Message msg) {
        int msgID = msg.what;
        log("handleMessage what - " + msgID);

        switch (msgID) {
        case MSG_INIT:
            mAppOpsMgr = mAppContext.getSystemService(AppOpsManager.class);
            mActiveWorkSource = new WorkSource();
            mNavigating = false;

            if (mIzatProviderType == IZAT_STREAM_FUSED) {
                mProviderClient = new FusedProviderClient();
            } else {
                if (LocIDLClientBase.getIDLServiceVersion().compareTo(IDLServiceVersion.V_AIDL)
                        >= 0) {
                    mProviderClient = new IzatProviderIdlClient(mIzatProviderType);
                    if (mWifiMultiplexer == null) {
                        mWifiMultiplexer = new IzatSessionMultiplexer(
                            this, mProviderClient, UserConsentManager.getInstance(sContext));
                    }
                    if (mWwanMultiplexer == null) {
                        mWwanMultiplexer = new IzatSessionMultiplexer(
                            this, mProviderClient, UserConsentManager.getInstance(sContext));
                    }
                } else {
                    Log.e(TAG, "ILoc AIDL is not supported!");
                    return true;
                }
            }
            break;
        case MSG_ENABLE_PROVIDER:
            mProviderClient.onEnableProvider();
            break;
        case MSG_DISABLE_PROVIDER:
            mProviderClient.onDisableProvider();
            break;
        case MSG_SET_REQUEST:
            ProviderRequest params = (ProviderRequest)msg.obj;
            handleSetRequest(params);
            break;
        case MSG_SET_SESSION_REQUEST:
            SessionRequest request = (SessionRequest)msg.obj;
            handleSetSessionRequest(request);
            break;
        case MSG_SET_MULTIPLEX_REQUEST:
            List<WwanAppInfo> appInfoList = (List<WwanAppInfo>)msg.obj;
            handleMultiplexRequest(appInfoList);
            break;
        case MSG_LOCATION_CHANGED:
            Location[] location = (Location[])msg.obj;
            if (!mHighPowerCapable) {
                Bundle extra = location[0].getExtras();
                if ("wwan".equals(extra.getString(NLP_TYPE_KEY))) {
                    mWwanMultiplexer.onLocationChanged(location[0]);
                } else {
                    mWifiMultiplexer.onLocationChanged(location[0]);
                }
            } else {
                reportLocation(location[0]);
            }
            break;
        case MSG_STATUS_CHANGED:
            int status = (int)msg.obj;
            log("MSG_STATUS_CHANGED: Status: " + status);
            boolean wasNavigating = mNavigating;
            if (status == GNSS_SESSION_BEGIN) {
                mNavigating = true;
            } else if (status ==  GNSS_SESSION_END ||
                       status ==  GNSS_ENGINE_OFF) {
                mNavigating = false;
            }

            if (wasNavigating != mNavigating) {
                // Send an intent to toggle the GPS icon
                updateHighPowerLocationMonitoringOnClientUids(
                        mActiveWorkSource, mNavigating);
            }
            break;
        }

        return true;
    }

    private void handleSetRequest(ProviderRequest request) {
        log("handleSetRequest");

        try {
            if (request.getWorkSource() != null) {
                // Fused Location Request, trigger Time Based Tracking session.
                WorkSource[] changes = mActiveWorkSource.setReturningDiffs(request.getWorkSource());
                if (null != changes && mNavigating && mHighPowerCapable) {
                    // start for new work, true for start
                    updateHighPowerLocationMonitoringOnClientUids(changes[0], true);
                    // finish for gone work, false for stop
                    updateHighPowerLocationMonitoringOnClientUids(changes[1], false);
                }
            }

            //Multiplexer is only needed for Network location provider
            if (!mHighPowerCapable) {
                mWifiMultiplexer.setProviderRequest(request);
            } else {
                boolean isEmergency = request.isLocationSettingsIgnored();
                if (request.isActive()) {
                    mProviderClient.OnAddRequest(Integer.MAX_VALUE,
                            request.getIntervalMillis(),
                            0,
                            LocAidlIzatHorizontalAccuracy.BLOCK,
                            isEmergency,
                            SessionType.SESSION_TYPE_GTP.ordinal(),
                            0);
                } else {
                    mProviderClient.OnRemoveRequest(Integer.MAX_VALUE,
                            request.getIntervalMillis(),
                            0,
                            LocAidlIzatHorizontalAccuracy.BLOCK,
                            isEmergency,
                            SessionType.SESSION_TYPE_GTP.ordinal(),
                            0);
                }
            }

        } catch (Exception e) {
            Log.w(TAG, "Exception ", e);
        }
    }

    private void handleSetSessionRequest(SessionRequest request) {
        log("handleSetSessionRequest");
        if (request.mIdentity.sessionType == SessionType.SESSION_TYPE_GTP) {
            mWifiMultiplexer.setSessionRequest(request);
        } else if (request.mIdentity.sessionType == SessionType.SESSION_TYPE_WWAN) {
            mWwanMultiplexer.setSessionRequest(request);
        }
    }

    public void handleMultiplexRequest(List<WwanAppInfo> appInfoList) {

        UserConsentManager cm = UserConsentManager.getInstance(sContext);
        WwanAppInfo p1App = null; // Priority 1
        WwanAppInfo p2App = null; // Priority 2
        WwanAppInfo p3App = null; // Priority 3
        WwanAppInfo p4App = null; // Priority 4
        WwanAppInfo selectedApp = null;
        WwanAppInfo lastSentApp = null;

        for (WwanAppInfo appInfo: appInfoList) {

            boolean fine = appInfo.mHasFineLocationPermission;
            boolean bg = appInfo.mHasBgLocationPermission;
            boolean userConsent = cm.getPackageUserConsent(appInfo.mPackageName, appInfo.mUserId);
            boolean aospConsent = cm.getAospUserConsent();

            if (fine && bg && userConsent) {
                if (p1App == null) {
                    p1App = appInfo;
                    break;
                }
            } else if (fine && bg && !userConsent && aospConsent) {
                if (p2App == null) {
                    p2App = appInfo;
                }
            } else if (fine && !bg && userConsent) {
                if (p3App == null) {
                    p3App = appInfo;
                }
            } else if (fine && !bg && !userConsent && aospConsent) {
                if (p4App == null) {
                    p4App = appInfo;
                }
            }

            if (appInfo.equals(mLastSentAppInfo)) {
                lastSentApp = appInfo;
            }
        }

        if (p1App != null) {
            selectedApp = p1App;
        } else if (p2App != null) {
            selectedApp = p2App;
        } else if (p3App != null) {
            selectedApp = p3App;
        } else if (p4App != null) {
            selectedApp = p4App;
        }

        boolean lastSentAppHasUserConsent = (lastSentApp != null &&
            cm.getPackageUserConsent(lastSentApp.mPackageName, lastSentApp.mUserId));

        if ((selectedApp == null && lastSentApp == null) ||
                (selectedApp != null && selectedApp.comparePermissions(lastSentApp) &&
                    (lastSentAppHasUserConsent || cm.getAospUserConsent()))) {
            if (lastSentApp == null) {
                log("Last sent app info still valid, pkg: null");
            } else {
                log("Last sent app info still valid, pkg: " + lastSentApp.mPackageName);
            }
        } else {
            mProviderClient.notifyWwanAppInfo(selectedApp);
            notifyWwanAppInfoMultiplexResultListeners(selectedApp);
            if (selectedApp == null) {
                mLastSentAppInfo = null;
            } else {
                mLastSentAppInfo = new WwanAppInfo(selectedApp);
            }
        }
    }

    // FLP specific API
    private void updateHighPowerLocationMonitoringOnClientUids(WorkSource newWork, boolean start) {
        if (newWork != null) {
            for (int i=0; i<newWork.size(); i++) {
                int uid = newWork.getUid(i);
                String packageName = newWork.getPackageName(i);
                if (start) {
                    mAppOpsMgr.startOpNoThrow(AppOpsManager.OP_GPS, uid, packageName, false, null,
                            null);
                } else {
                    mAppOpsMgr.finishOp(AppOpsManager.OP_GPS, uid, packageName);
                }
                log(String.format("%sOp - uid: %d; packageName: %s",
                                  (start ? "start" : "finish"), uid, packageName));
            }
        }
    }

    // NLP specific API
    public void setUserConsent(boolean consentAccepted) {
        // For OSnp only this can't happen (opt-in not shown)
        if (mUserConsent.compareAndSet(!consentAccepted, consentAccepted)) {
            setAllowed(consentAccepted);
        }
    }

    public boolean getUserConsent() {
        return mUserConsent.get();
    }

    private void log(String log) {
        if (DBG) Log.d(TAG, log);
    }

    public void registerEsStatusUpdate() {
        FlpServiceProvider.getInstance(sContext).registerEsStatusUpdate();
    }

    //=============================================================
    // Izat Session Multiplexer
    //=============================================================
    private static class RequestData {
        long mLastLocationReportTimestamp = 0;
    }
    // All Multiplexer method must be invoked on Handler thread
    private static class IzatSessionMultiplexer implements EsStatusReceiver.IEsStatusListener {

        private final String TAG = "IzatSessionMultiplexer";
        private final boolean VERBOSE = Log.isLoggable(TAG, Log.VERBOSE);

        private IzatProvider mProvider;
        private IzatProviderClient mAidlClient;
        private UserConsentManager mUserConsentManager;

        private List<SessionRequest> mSessionRequests;
        private ProviderRequest mAospProviderRequest = null;
        private boolean mDeviceInEmergency = false;

        // Holds additional data for above session requests and provider request
        private Map<Object, RequestData> mRequestDataMap;

        private boolean mIsAidlSessionRunning = false;
        private long mCurrentAidlSessionInterval = 0;
        private RequestPrecision mCurrentAidlSessionAccuracy =
                RequestPrecision.REQUEST_PRECISION_COARSE;
        private SessionType mCurrentAidlSessionType = SessionType.SESSION_TYPE_GTP;
        private int mCurrentUid = -1;

        private void logv(String log) {
            if (VERBOSE) Log.v(TAG, log);
        }
        private void logi(String log) {
            Log.i(TAG, log);
        }
        private void loge(String log) {
            Log.e(TAG, log);
        }

        public IzatSessionMultiplexer(IzatProvider provider, IzatProviderClient client,
                UserConsentManager consentMgr) {
            mProvider = provider;
            mAidlClient = client;
            mUserConsentManager = consentMgr;
            mSessionRequests = new ArrayList<SessionRequest>();
            mRequestDataMap = new HashMap<Object, RequestData>();
            EsStatusReceiver.getInstance(sContext).registerEsStatusListener(this);
            mUserConsentManager.registerUserConsentListener(() -> { multiplex(); });
        }

        @Override
        public void onStatusChanged(boolean isEmergencyMode) {
            Log.d(TAG, "Emergency mode changed from " + mDeviceInEmergency +
                    " to : " + isEmergencyMode);
            mDeviceInEmergency = isEmergencyMode;
        }

        public void setSessionRequest(SessionRequest request) {

            if (!request.isValid()) {
                loge("Invalid request");
                return;
            }

            switch (request.mIdentity.requestType) {
            case REQUEST_LOCATION_UPDATES:
            case REQUEST_PASSIVE_LOCATION_UPDATES:
                requestLocationUpdates(request);
                break;

            case REMOVE_LOCATION_UPDATES:
            case REMOVE_PASSIVE_LOCATION_UPDATES:
                removeLocationUpdates(request);
                break;

            default:
                loge("Invalid request type");
                break;
            }
        }

        public void setProviderRequest(ProviderRequest request) {
            logv("Multiplexer: addProviderRequest isActive: " + request.isActive() +
                    " minIntervalMillis: " + request.getIntervalMillis());

            if (request.isActive()) {
                requestLocationUpdates(request);
            } else {
                removeLocationUpdates(request);
            }
        }

        public void onLocationChanged(Location location) {
            Log.d(TAG, "ENTERING onLocationChanged...");

            Location wifiNoneFtm = null;
            Location wifiFtm = null;
            Location wwanFix = null;
            Location fixToRpt = null;

            Log.v(TAG, "MSG_LOCATION_CHANGED: " + location.getProvider());
            Bundle extra = location.getExtras();
            if ("wifi_rssi".equals(extra.getString(NLP_TYPE_KEY))) {
                wifiNoneFtm = location;
                Log.d(TAG, "receive wifi rssi position");
            } else if ("wifi_rtt_server".equals(extra.getString(NLP_TYPE_KEY))) {
                wifiNoneFtm = location;
                Log.d(TAG, "receive wifi rtt server position");
            } else if ("wifi_rtt_ftm".equals(extra.getString(NLP_TYPE_KEY))) {
                wifiFtm = location;
                Log.d(TAG, "receive wifi rtt ftm position");
            } else if ("wwan".equals(extra.getString(NLP_TYPE_KEY))) {
                wwanFix = location;
                Log.d(TAG, "receive wwan position");
            } else {
                Log.e(TAG, "receive no valid location");
                return;
            }
            boolean hasValidwifiNoneFtm = (wifiNoneFtm != null);
            boolean hasValidwifiFtm = (wifiFtm != null);
            boolean hasValidwwanFix = (wwanFix != null);

            Log.d(TAG, "size of mSessionRequests = " + mSessionRequests.size());
            for (SessionRequest request : mSessionRequests) {
                fixToRpt = null;
                boolean userConsentGranted = mUserConsentManager.getPackageUserConsent(
                        request.mIdentity.packageName, request.mIdentity.uid);
                Log.d(TAG, "SessionRequest = " + request.mIdentity.sessionType +
                           ", userConsentGranted = " + userConsentGranted +
                           ", uid = " + request.mIdentity.uid);
                if (request.mIdentity.sessionType == SessionType.SESSION_TYPE_WWAN) {
                    if (hasValidwwanFix) {
                        fixToRpt = wwanFix;
                        Log.d(TAG, "report wwan position");
                    } else {
                        Log.e(TAG, "no valid wwan position");
                        continue;
                    }
                } else if (request.mIdentity.sessionType == SessionType.SESSION_TYPE_GTP) {
                    if (userConsentGranted) {
                        if (hasValidwifiNoneFtm) {
                            fixToRpt = wifiNoneFtm;
                            Log.d(TAG, "report wifi none ftm position");
                        } else if (hasValidwifiFtm) {
                            fixToRpt = wifiFtm;
                            Log.d(TAG, "report wifi ftm position");
                        } else {
                            Log.e(TAG, "no valid position");
                            continue;
                        }
                    } else {
                        if (hasValidwifiFtm) {
                            fixToRpt = wifiFtm;
                            Log.d(TAG, "report wifi ftm position");
                        } else {
                            Log.e(TAG, "user consent not granted, no valid wifi fix");
                            continue;
                        }
                    }
                } else {
                    Log.e(TAG, "no valid request session type, current type: " +
                            request.mIdentity.sessionType);
                    continue;
                }
                if (fixToRpt != null && request.mParams.callback != null) {
                    request.mParams.callback.onLocationAvailable(fixToRpt);
                } else {
                    Log.e(TAG, "no valid position to report or no valid Location SDK session");
                    continue;
                }
                logi("request type: " + request.mIdentity.requestType + ", request QoS: " +
                        request.mParams.precision +  ", userConsentGranted: " +
                        userConsentGranted + ", hasValidwifiNoneFtm: " + hasValidwifiNoneFtm +
                        ", hasValidwifiFtm: " + hasValidwifiFtm + ", hasValidwwanFix: " +
                        hasValidwwanFix + ", wifiNoneFtm: " +
                        (wifiNoneFtm == null ? "": wifiNoneFtm) + ", wifiFtm: " +
                        (wifiFtm == null ? "": wifiFtm) + ", wwanFix: " +
                        (wwanFix == null ? "": wwanFix + ", fix to report: "));
            }

            if (mAospProviderRequest != null) {
                Log.i(TAG, "Reporting location to aosp request.");
                mProvider.reportLocation(location);
            }
        }

        private void requestLocationUpdates(SessionRequest request) {

            // Look for any existing request
            SessionRequest existingRequest = null;
            Iterator<SessionRequest> iterator = mSessionRequests.iterator();
            while (iterator.hasNext()) {
                SessionRequest r = iterator.next();
                if (r.getUniqueId() == request.getUniqueId() &&
                        r.mIdentity.requestType == request.mIdentity.requestType) {
                    Log.d(TAG, "Found Existing Request matching pid/uid");
                    existingRequest = r;
                    break;
                }
            }

            boolean multiplexNeeded = false;
            if (existingRequest != null) {

                if (existingRequest.isValidUpdate(request)) {
                    if ((existingRequest.mParams.minIntervalMillis !=
                            request.mParams.minIntervalMillis ||
                            existingRequest.mParams.precision != request.mParams.precision ||
                            existingRequest.mIdentity.sessionType !=
                            request.mIdentity.sessionType) &&
                            existingRequest.mIdentity.requestType !=
                            SessionRequest.RequestType.REQUEST_PASSIVE_LOCATION_UPDATES) {
                        existingRequest.mParams.minIntervalMillis =
                                request.mParams.minIntervalMillis;
                        existingRequest.mParams.precision = request.mParams.precision;
                        existingRequest.mIdentity.sessionType = request.mIdentity.sessionType;
                        multiplexNeeded = true;
                        //Since the request already exist, replace the existing request using
                        //new request
                        iterator.remove();
                        mSessionRequests.add(request);
                    } else {
                        logv("Min interval unchanged");
                    }

                } else {
                    loge("Invalid update to existing request: " + existingRequest);
                }
            } else {
                Log.d(TAG, "request.mIdentity.sessionType = " + request.mIdentity.sessionType +
                    "request.mIdentity.requestType = " + request.mIdentity.requestType);
                mSessionRequests.add(request);
                if (request.mIdentity.requestType !=
                        SessionRequest.RequestType.REQUEST_PASSIVE_LOCATION_UPDATES) {
                    multiplexNeeded = true;
                }
            }

            if (multiplexNeeded) {
                mCurrentAidlSessionType = request.mIdentity.sessionType;
                multiplex();
            }
        }

        private void requestLocationUpdates(ProviderRequest request) {

            if (mAospProviderRequest == null || !mAospProviderRequest.isActive() ||
                    mAospProviderRequest.getIntervalMillis() != request.getIntervalMillis()) {
                mAospProviderRequest = request;
                multiplex();
            }
        }

        private void removeLocationUpdates(SessionRequest request) {
            Log.d(TAG, "ENtering removeLocationUpdates");
            // Look for any existing request
            SessionRequest existingRequest = null;
            for (SessionRequest r : mSessionRequests) {
                Log.d(TAG, "before remove: size of mSessionRequests = " + mSessionRequests.size());
                if (r.getUniqueId() == request.getUniqueId() &&
                        r.isPassiveRequestType() == request.isPassiveRequestType()) {
                    existingRequest = r;
                }
            }

            if (existingRequest != null) {

                mSessionRequests.remove(existingRequest);
                Log.d(TAG, "after remove: size of mSessionRequests = " + mSessionRequests.size());
                // No need to re-multiplex for passive request
                if (request.mIdentity.requestType !=
                        SessionRequest.RequestType.REMOVE_PASSIVE_LOCATION_UPDATES) {
                    Log.d(TAG, "requestType != REMOVE_PASSIVE_LOCATION_UPDATES");
                    multiplex();
                }
            } else {
                loge("Request not found");
            }
        }

        private void removeLocationUpdates(ProviderRequest request) {

            if (mAospProviderRequest != null && mAospProviderRequest.isActive()) {

                mAospProviderRequest = request;
                multiplex();
            }
        }

        private void multiplex() {
            Log.v(TAG, "Entering multiplex...");
            // Look at the list of sessions, and extract all minIntervals
            List<Long> minIntervals = new ArrayList<Long>();
            List<RequestPrecision> accuracies = new ArrayList<>();
            int uid = -1;
            if (mSessionRequests.size() != 0) {
                uid = mSessionRequests.get(0).mIdentity.uid;
            }
            for (SessionRequest r : mSessionRequests) {
                if (r.mIdentity.requestType ==
                        SessionRequest.RequestType.REQUEST_LOCATION_UPDATES &&
                        r.mParams.minIntervalMillis > 0) {
                    minIntervals.add(r.mParams.minIntervalMillis);
                    accuracies.add(r.mParams.precision);
                }
            }
            boolean isEmergency = false;
            if (mAospProviderRequest != null) {
                isEmergency = mAospProviderRequest.isLocationSettingsIgnored();
            }

            if (mAospProviderRequest != null && mAospProviderRequest.isActive()) {
                Log.d(TAG, "mAospProviderRequest: " + mAospProviderRequest.toString());
                minIntervals.add(mAospProviderRequest.getIntervalMillis());
                accuracies.add(RequestPrecision.REQUEST_PRECISION_COARSE);
            }

            // Send request down to AIDL service
            if (minIntervals.size() == 0) {
                Log.d(TAG, "minIntervals size is 0.");

                if (mIsAidlSessionRunning) {
                    // stop session on AIDL service
                    mAidlClient.OnRemoveRequest(
                        Integer.MAX_VALUE, // numFixes
                        0,                 // interval
                        0,                 // displacement
                        LocAidlIzatHorizontalAccuracy.BLOCK, // accuracy
                        isEmergency,                        // isEmergency
                        mCurrentAidlSessionType.ordinal(),
                        mCurrentUid);
                    mIsAidlSessionRunning = false;
                }
                mCurrentAidlSessionInterval = 0;
                mCurrentAidlSessionAccuracy = RequestPrecision.REQUEST_PRECISION_COARSE;
                mCurrentUid = -1;
            } else {
                long gcdIntervalMillis = 0;

                // Get Greatest Common Denominator of intervals
                if (minIntervals.size() > 1) {
                    gcdIntervalMillis = getMultiplexedInterval(minIntervals);
                } else {
                    gcdIntervalMillis = minIntervals.get(0);
                }
                RequestPrecision accuracy = RequestPrecision.REQUEST_PRECISION_COARSE;
                // Consolidated all active sessions QoS, if atleast one high request,
                // send high accuracy request to QNP, else low
                if (accuracies.size() > 0) {
                    for (RequestPrecision accu: accuracies) {
                        if (accu == RequestPrecision.REQUEST_PRECISION_FINE) {
                            accuracy = accu;
                            break;
                        }
                    }
                    Log.d(TAG, "gcdIntervalMillis: " + gcdIntervalMillis +
                            ", consolidated accuracy: " + accuracy +
                            ", mCurrentAidlSessionType: " + mCurrentAidlSessionType +
                            ", uid: " + uid);

                    // Run session on AIDL if gcd is different than current session interval
                    if (gcdIntervalMillis != mCurrentAidlSessionInterval ||
                            !mIsAidlSessionRunning ||
                            accuracy != mCurrentAidlSessionAccuracy) {

                        // trigger session on AIDL service
                        mAidlClient.OnAddRequest(
                                Integer.MAX_VALUE,  // numFixes
                                gcdIntervalMillis,  // interval
                                0,                  // displacement
                                accuracy.ordinal(), //requestPrecision
                                isEmergency,   // isEmergency
                                mCurrentAidlSessionType.ordinal(),
                                uid);

                        mIsAidlSessionRunning = true;
                        mCurrentAidlSessionInterval = gcdIntervalMillis;
                        mCurrentAidlSessionAccuracy = accuracy;
                        mCurrentUid = uid;
                    } else {
                        logi("Current session already running with required interval: " +
                                gcdIntervalMillis);
                    }
                }
            }
        }

        // Get the multiplexed interval
        private long getMultiplexedInterval(List<Long> values) {

            long intervalMillis = 0;
            for (long value : values) {
                if (intervalMillis == 0 || value < intervalMillis) {
                    intervalMillis = value;
                }
            }
            return intervalMillis > 0 ? intervalMillis : 1000;
        }
    }

    //=============================================================
    //Java IDL client interface
    //=============================================================
    interface IzatProviderClient {
        public void onEnableProvider();
        public void onDisableProvider();
        public void OnAddRequest(int numFixes, long tbf, float displacement,
                int accuracy, boolean isEmergency, int source, int uid);
        public void OnRemoveRequest(int numFixes, long tbf, float displacement,
                int accuracy, boolean isEmergency, int source, int uid);
        public void notifyWwanAppInfo(WwanAppInfo appInfo);
    }

    private class FusedProviderClient implements IzatProviderClient,
            AltitudeReceiver.IAltitudeReportCb, EsStatusReceiver.IEsStatusListener {
        private IFlpService mFlpBinder;
        private FusedLocationCallback mFusedProviderCb;
        private AltitudeReceiver mAltReceiver;
        private volatile boolean mIsUserEmergency;
        private boolean mIsInSession;
        private IzatProvider mProvider;

        public boolean getUserEsStatus() {
            return mIsUserEmergency;
        }

        private FusedProviderClient() {
            mFlpBinder = FlpServiceProvider.getInstance(sContext).getFlpBinder();
            mAltReceiver = AltitudeReceiver.getInstance(sContext);
            mFusedProviderCb = new FusedLocationCallback();
            mProvider = IzatProvider.this;
            EsStatusReceiver.getInstance(sContext).registerEsStatusListener(this);
            //retrieve LocAidlIzatProvider once to update this ALE Flp type
            try {
                ILocAidlGnss serviceAidl = LocIDLClientBase.getGnssAidlService();
                if (null != serviceAidl) {
                    serviceAidl.getExtensionLocAidlIzatFusedProvider();
                }
            } catch (Exception e) {
                Log.e(TAG,"Exception: " + e);
            }
        }

        @Override
        public void onEnableProvider(){}
        @Override
        public void onDisableProvider(){}
        @Override
        public void OnAddRequest(int numFixes, long tbf, float displacement,
                int accuracy, boolean isEmergency, int source, int uid) {
            Log.d(TAG, "OnAddRequest, mIsUserEmergency: " + mIsUserEmergency +
                    ", isEmergency: " + isEmergency);
            //there is ongoing session
            if (mIsInSession) {
                try {
                    mFlpBinder.updateFlpSession(sFusedRequestCnt, 2, tbf, 0, 0);
                } catch(Exception e) {}
            } else {
                int hwId = ++sFusedRequestCnt;
                try {
                    mFlpBinder.registerCallback(FlpServiceProvider.FLP_SESSION_FOREGROUND, hwId,
                            mFusedProviderCb, System.currentTimeMillis());
                    mFlpBinder.startFlpSessionWithPower(hwId, 2, tbf,
                            0, 0, FlpServiceProvider.POWER_MODE_INVALID, 0);
                } catch(Exception e) {}
                mIsInSession = true;
            }
            //Start altituder query
            mAltReceiver.startAltitudeQuery(this);
        }

        @Override
        public void OnRemoveRequest(int numFixes, long tbf, float displacement,
                int accuracy, boolean isEmergency, int source, int uid) {
            if (!mIsUserEmergency) {
                try {
                    mFlpBinder.unregisterCallback(FlpServiceProvider.FLP_SESSION_FOREGROUND,
                            mFusedProviderCb);
                    mFlpBinder.stopFlpSession(sFusedRequestCnt);
                } catch(Exception e) {}
                mIsInSession = false;
            }
            // If device is in emergency call, stop FLP session when emergency call end
            // Just set Altitude callback to null to stop altitude query
            mAltReceiver.stopAltitudeQuery();
        }

        @Override
        public void onLocationReportWithAlt(Location location, boolean isAltitudeAvail) {
            Log.d(TAG, "onLocationReportWithAlt: " + location.toString());
            Location[] loc = {toLocation(location.getTime(),
                    location.getElapsedRealtimeNanos(), location.getLatitude(),
                    location.getLongitude(),
                    location.hasAltitude(), location.getAltitude(),
                    location.hasVerticalAccuracy(),
                    location.getVerticalAccuracyMeters(),
                    location.hasSpeed(), location.getSpeed(),
                    location.hasSpeedAccuracy(),
                    location.getSpeedAccuracyMetersPerSecond(),
                    location.hasBearing(),
                    location.getBearing(),
                    location.hasBearingAccuracy(), location.getBearingAccuracyDegrees(),
                    location.hasAccuracy(), location.getAccuracy(), (short)-1, false, (short)0)};
            mProvider.onLocationChanged(loc);
        }

        @Override
        public void onStatusChanged(boolean isEmergencyMode) {
            Log.d(TAG, "Emergency mode changed from : " + mIsUserEmergency
                    + " to : " + isEmergencyMode);
            if (!mIsUserEmergency && isEmergencyMode) {
                //Start network location provider in emergency mode
                ProviderRequest.Builder requestBuilder =
                        new ProviderRequest.Builder();
                requestBuilder.setIntervalMillis(1000);
                requestBuilder.setLocationSettingsIgnored(true);
                //Indicate this is an emergency NLP request
                ProviderRequest request = requestBuilder.build();
                getNetworkProvider(sContext).onSetRequest(request);
            } else if (mIsUserEmergency && !isEmergencyMode) {
                ProviderRequest.Builder requestBuilder =
                        new ProviderRequest.Builder();
                requestBuilder.setIntervalMillis(Long.MAX_VALUE);
                requestBuilder.setLocationSettingsIgnored(true);
                ProviderRequest request = requestBuilder.build();
                getNetworkProvider(sContext).onSetRequest(request);
                if (mIsInSession) {
                    try {
                        mFlpBinder.unregisterCallback(FlpServiceProvider.FLP_SESSION_FOREGROUND,
                                mFusedProviderCb);
                        mFlpBinder.stopFlpSession(sFusedRequestCnt);
                    } catch(Exception e) {}
                    mIsInSession = false;
                    mAltReceiver.stopAltitudeQuery();
                }
            }
            mIsUserEmergency = isEmergencyMode;
        }

        @Override
        public void notifyWwanAppInfo(WwanAppInfo appInfo) {
            Log.d(TAG, "FusedProviderClient.notifyWwanAppInfo - noop");
        }

        private class FusedLocationCallback extends ILocationCallback.Stub {
            @Override
            public void onLocationAvailable(Location[] locations) {
                Location[] locArr = new Location[locations.length];
                int i = 0;
                for (Location location : locations) {
                    Log.d(TAG, "onLocationAvailable: " + location.toString());
                    if (location.getAccuracy() >= 25000) {
                        Log.e(TAG, "Location accuracy is larger than 25km, ignore");
                        continue;
                    }
                    //Comment out getUserEsStatus() for now for test purpose
                    if (getUserEsStatus() && mAltReceiver.isPresent()) {
                        Log.d(TAG, "In Emergency and Z Provider present, query Z");
                        mAltReceiver.getAltitudeFromLocation(location, true);
                    } else {
                        Log.d(TAG, "Not in Emergency or Z Provider not present, report location");
                        locArr[i++] = toLocation(location.getTime(),
                                location.getElapsedRealtimeNanos(), location.getLatitude(),
                                location.getLongitude(),
                                location.hasAltitude(), location.getAltitude(),
                                location.hasVerticalAccuracy(),
                                location.getVerticalAccuracyMeters(),
                                location.hasSpeed(), location.getSpeed(),
                                location.hasSpeedAccuracy(),
                                location.getSpeedAccuracyMetersPerSecond(),
                                location.hasBearing(),
                                location.getBearing(),
                                location.hasBearingAccuracy(), location.getBearingAccuracyDegrees(),
                                location.hasAccuracy(), location.getAccuracy(), (short)-1, false,
                                (short)0);
                    }
                }
                if (i > 0) {
                    mProvider.onLocationChanged(locArr);
                }
                Log.d(TAG, "onLocationAvailable, exit");
            }
        }
    }

    //=============================================================
    //IzatProvider Java AIDL client
    //=============================================================
    private class IzatProviderIdlClient extends LocIDLClientBase
            implements LocIDLClientBase.IServiceDeathCb, IzatProviderClient {

        private final String TAG = "IzatProviderIdlClient";
        private ILocAidlIzatProvider mIzatProviderIface;
        private IzatProviderCb mIzatProviderCb;
        private int mProviderType;
        private volatile LocAidlIzatRequest mReq = null;

        private IzatProviderIdlClient(int providerType) {
            mProviderType = providerType;
            getIzatProviderIface();
            if (null != mIzatProviderIface) {
                try {
                    mIzatProviderCb = new IzatProviderCb(IzatProvider.this);
                    mIzatProviderIface.init(mIzatProviderCb);
                    registerServiceDiedCb(this);
                } catch (RemoteException e) {
                }
            }
        }

        private void getIzatProviderIface() {
            if (null == mIzatProviderIface) {
                ILocAidlGnss service = (ILocAidlGnss)getGnssAidlService();

                if (null != service) {
                    try {
                        mIzatProviderIface = service.getExtensionLocAidlIzatNetworkProvider();
                    } catch (RemoteException e) {
                    }
                }
            }
        }

        @Override
        public void onServiceDied() {
            mIzatProviderIface = null;
            getIzatProviderIface();
            if (null != mIzatProviderIface) {
                try {
                    mIzatProviderIface.init(mIzatProviderCb);
                    mIzatProviderIface.onEnable();
                    if (mReq != null) {
                        mIzatProviderIface.onAddRequest(mReq);
                    }
                } catch (RemoteException e) {
                }
            }
        }

        public class IzatProviderCb extends ILocAidlIzatProviderCallback.Stub {
            private IzatProvider mProvider;

            public IzatProviderCb(IzatProvider provider) {
                mProvider = provider;
            }

            @Override
            public void onLocationChanged(LocAidlIzatLocation location) {}
            @Override
            public void onLocationsChanged(LocAidlIzatLocation[] location) {
                IDLClientUtils.fromIDLService(TAG);
                int len = location.length;
                Location[] locArr = new Location[len];
                for (int i=0; i<len; ++i) {
                    long utcTime = location[i].utcTimestampInMsec;
                    long elapsedRealTimeNanos = location[i].elapsedRealTimeInNanoSecs;
                    double latitude = location[i].latitude;
                    double longitude = location[i].longitude;
                    boolean hasAltitude = location[i].hasAltitudeWrtEllipsoid;
                    double altitude = location[i].altitudeWrtEllipsoid;
                    boolean hasVerticalUnc = location[i].hasVertUnc;
                    float verticalUnc = location[i].vertUnc;
                    boolean hasSpeed = location[i].hasSpeed;
                    float speed = location[i].speed;
                    boolean hasSpeedUnc = location[i].hasSpeedUnc;
                    float speedUnc = location[i].speedUnc;
                    boolean hasBearing = location[i].hasBearing;
                    float bearing = location[i].bearing;
                    boolean hasBearingUnc = location[i].hasBearingUnc;
                    float bearingUnc = location[i].bearingUnc;
                    boolean hasAccuracy = location[i].hasHorizontalAccuracy;
                    float accuracy = location[i].horizontalAccuracy;
                    short positionSource = -1;

                    if (location[i].hasNetworkPositionSource) {
                        switch (location[i].networkPositionSource) {
                            case LocAidlNetworkPositionSourceType.CELL:
                                positionSource = 0;
                                break;
                            case LocAidlNetworkPositionSourceType.WIFI:
                                positionSource = 1;
                                break;
                            case LocAidlNetworkPositionSourceType.WIFI_RTT_SERVER:
                                positionSource = 2;
                                break;
                            case LocAidlNetworkPositionSourceType.WIFI_RTT_FTM:
                                positionSource = 3;
                                break;
                        }
                    }
                    boolean hasAltRefType = location[i].hasAltitudeRefType;
                    short altRefType = 0;
                    if (hasAltRefType) {
                        switch (location[i].altitudeRefType) {
                            case LocAidlAltitudeRefType.ALT_REF_UNKNOWN:
                                altRefType = 0;
                                break;
                            case LocAidlAltitudeRefType.ALT_REF_WGS84:
                                altRefType = 1;
                                break;
                            case LocAidlAltitudeRefType.ALT_REF_MSL:
                                altRefType = 2;
                                break;
                            case LocAidlAltitudeRefType.ALT_REF_AGL:
                                altRefType = 3;
                                break;
                            case LocAidlAltitudeRefType.ALT_REF_FLOOR_LEVEL:
                                altRefType = 4;
                                break;
                            default:
                                Log.e(TAG, "Invalid type");
                        }
                    }
                    locArr[i] = toLocation(utcTime, elapsedRealTimeNanos, latitude, longitude,
                            hasAltitude, altitude, hasVerticalUnc, verticalUnc, hasSpeed, speed,
                            hasSpeedUnc, speedUnc, hasBearing, bearing, hasBearingUnc, bearingUnc,
                            hasAccuracy, accuracy, positionSource, hasAltRefType, altRefType);
                }

                mProvider.onLocationChanged(locArr);
            }

            @Override
            public void onStatusChanged(int status) {
                IDLClientUtils.fromIDLService(TAG);
                mProvider.onStatusChanged(status);
            }
            @Override
            public final int getInterfaceVersion() {
                return ILocAidlIzatProviderCallback.VERSION;
            }
            @Override
            public final String getInterfaceHash() {
                return ILocAidlIzatProviderCallback.HASH;
            }
        }

        @Override
        public void onEnableProvider() {
            if (null != mIzatProviderIface) {
                try {
                    IDLClientUtils.toIDLService(TAG);
                    mIzatProviderIface.onEnable();
                } catch (RemoteException e) {
                }
            }
        }

        @Override
        public void onDisableProvider() {
            if (null != mIzatProviderIface) {
                try {
                    IDLClientUtils.toIDLService(TAG);
                    mIzatProviderIface.onDisable();
                } catch (RemoteException e) {
                }
            }
        }

        @Override
        public void OnAddRequest(int numFixes, long tbf, float displacement,
                int accuracy, boolean isEmergency, int source, int uid) {
            LocAidlIzatRequest req = new LocAidlIzatRequest();
            req.provider = mProviderType;
            req.numUpdates = numFixes;
            req.timeIntervalBetweenFixes = tbf;
            req.smallestDistanceBetweenFixes = displacement;
            //convert from RequestPrecision to LocAidl accuracy
            req.suggestedHorizontalAccuracy = accuracy - 1;
            req.uid = uid;
            if (source == SessionType.SESSION_TYPE_GTP.ordinal()) {
                if (accuracy == RequestPrecision.REQUEST_PRECISION_FINE.ordinal()) {
                    req.requestType = LocAidlIzatRequestType.WIFI_PREMIUM;
                } else {
                    req.requestType = LocAidlIzatRequestType.WIFI_BASIC;
                }
            } else if (source == SessionType.SESSION_TYPE_WWAN.ordinal()) {
                if (accuracy == RequestPrecision.REQUEST_PRECISION_FINE.ordinal()) {
                    req.requestType = LocAidlIzatRequestType.WWAN_PREMIUM;
                } else {
                    req.requestType = LocAidlIzatRequestType.WWAN_BASIC;
                }
            }
            //Use LocAidlIzatStreamType::GNSS to indicate this is a emergency NLP request
            //We can have a better name when next time AIDL upgrade
            if (isEmergency) {
                req.provider = LocAidlIzatStreamType.GNSS;
            }

            if (null != mIzatProviderIface) {
                try {
                    IDLClientUtils.toIDLService(TAG);
                    mIzatProviderIface.onAddRequest(req);
                    mReq = req;
                } catch (RemoteException e) {
                }
            }
        }

        @Override
        public void OnRemoveRequest(int numFixes, long tbf, float displacement,
                int accuracy, boolean isEmergency, int source, int uid) {
            LocAidlIzatRequest req = new LocAidlIzatRequest();
            req.provider = mProviderType;
            req.numUpdates = numFixes;
            req.timeIntervalBetweenFixes = tbf;
            req.smallestDistanceBetweenFixes = displacement;
            req.suggestedHorizontalAccuracy = accuracy;
            req.requestType = source;
            req.uid = uid;
            if (source == SessionType.SESSION_TYPE_GTP.ordinal()) {
                if (accuracy == RequestPrecision.REQUEST_PRECISION_FINE.ordinal()) {
                    req.requestType = LocAidlIzatRequestType.WIFI_PREMIUM;
                } else {
                    req.requestType = LocAidlIzatRequestType.WIFI_BASIC;
                }
            } else if (source == SessionType.SESSION_TYPE_WWAN.ordinal()) {
                if (accuracy == RequestPrecision.REQUEST_PRECISION_FINE.ordinal()) {
                    req.requestType = LocAidlIzatRequestType.WWAN_PREMIUM;
                } else {
                    req.requestType = LocAidlIzatRequestType.WWAN_BASIC;
                }
            }
            //Use LocAidlIzatStreamType::GNSS to indicate this is a emergency NLP request
            //We can have a better name when next time AIDL upgrade
            if (isEmergency) {
                req.provider = LocAidlIzatStreamType.GNSS;
            }

            if (null != mIzatProviderIface) {
                try {
                    IDLClientUtils.toIDLService(TAG);
                    mReq = null;
                    mIzatProviderIface.onRemoveRequest(req);
                } catch (RemoteException e) {
                }
            }
        }

        @Override
        public void notifyWwanAppInfo(WwanAppInfo appInfo) {
            if (null != mIzatProviderIface) {

                LocAidlWwanAppInfo aidlAppInfo = new LocAidlWwanAppInfo();
                if (appInfo != null) {
                    aidlAppInfo.pid = 0; // Should not be required, this is a variable field.
                    aidlAppInfo.uid = appInfo.mUserId;
                    aidlAppInfo.appHash = appInfo.mAppHash;
                    aidlAppInfo.appPackageName = appInfo.mPackageName;
                    aidlAppInfo.appCookie = appInfo.mCookie;
                    aidlAppInfo.hasFinePermission = appInfo.mHasFineLocationPermission;
                    aidlAppInfo.hasCoarsePermission = appInfo.mHasCoarseLocationPermission;
                    aidlAppInfo.hasBackgroundPermission = appInfo.mHasBgLocationPermission;
                    aidlAppInfo.appQwesLicenseId = "";
                    aidlAppInfo.hasPremiumLicense = appInfo.mHasPreciseLicense;
                    aidlAppInfo.hasStandardLicense = appInfo.mHasStandardLicense;
                } else {
                    aidlAppInfo.pid = 0;
                    aidlAppInfo.uid = 0;
                    aidlAppInfo.appHash = "";
                    aidlAppInfo.appPackageName = "";
                    aidlAppInfo.appCookie = "";
                    aidlAppInfo.hasFinePermission = false;
                    aidlAppInfo.hasCoarsePermission = false;
                    aidlAppInfo.hasBackgroundPermission = false;
                    aidlAppInfo.appQwesLicenseId = "";
                    aidlAppInfo.hasPremiumLicense = false;
                    aidlAppInfo.hasStandardLicense = false;
                }

                try {
                    IDLClientUtils.toIDLService(TAG);
                    mIzatProviderIface.notifyWwanAppInfo(aidlAppInfo);
                } catch (RemoteException e) {
                    Log.e(TAG, "Failed to notifyWwanAppInfo");
                }
            }
        }
    }
}
