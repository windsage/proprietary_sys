/** @file IZatManager.java
*/

/* ======================================================================
*  Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
*  All Rights Reserved.
*  Confidential and Proprietary - Qualcomm Technologies, Inc.
*  ====================================================================*/

package com.qti.location.sdk;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import com.qti.altitudereceiver.IAltitudeReceiver;
import com.qti.altitudereceiver.IAltitudeReceiverResponseListener;
import com.qti.debugreport.IDebugReportCallback;
import com.qti.debugreport.IXtraStatusCallback;
import com.qti.debugreport.IDebugReportService;
import com.qti.flp.*;
import com.qti.geofence.GeofenceData;
import com.qti.geofence.IGeofenceCallback;
import com.qti.geofence.IGeofenceService;
import com.qti.gnssconfig.IGnssConfigCallback;
import com.qti.gnssconfig.IGnssConfigService;
import com.qti.gnssconfig.NtripConfigData;
import com.qti.gnssconfig.RLConfigData;
import com.qti.gtp.IGTPService;
import com.qti.gtp.GTPAccuracy;
import com.qti.gtp.IGTPServiceCallback;
import com.qti.gtp.GtpRequestData;
import com.qti.wwanadreceiver.*;
import com.qti.preciseposition.IPrecisePositionService;
import com.qti.preciseposition.IPrecisePositionCallback;
import com.qti.izat.IIzatService;
import com.qti.location.sdk.IZatAltitudeReceiver.IZatAltitudeReceiverResponseListener;
import com.qti.location.sdk.IZatDBCommon.IZatAPSSIDInfo;
import com.qti.location.sdk.IZatDBCommon.IZatApBsListStatus;
import com.qti.location.sdk.IZatDBCommon.IZatCellInfo;
import com.qti.location.sdk.IZatDBCommon.IZatRangingBandWidth;
import com.qti.location.sdk.IZatGnssConfigServices.IZatRobustLocationConfigService;
import com.qti.location.sdk.IZatGnssConfigServices.IZatSvConfigService;
import com.qti.location.sdk.IZatGnssConfigServices.IZatNtnConfigService.IZatNtnStatusCallback;
import com.qti.location.sdk.IZatWWANDBProvider.IZatBSObsLocationData;
import com.qti.location.sdk.IZatWWANDBProvider.IZatWWANDBProviderResponseListener;
import com.qti.location.sdk.IZatWWANDBReceiver.*;
import com.qti.location.sdk.IZatWWANDBReceiver.IZatBSLocationDataBase.IZatReliablityTypes;
import com.qti.location.sdk.IZatWiFiDBProvider.IZatAPScan.IzatApServiceStatus;
import com.qti.location.sdk.IZatWiFiDBProvider.*;
import com.qti.location.sdk.IZatWiFiDBReceiver.IZatAPInfo;
import com.qti.location.sdk.IZatWiFiDBReceiver.IZatAPInfoExtra;
import com.qti.location.sdk.IZatWiFiDBReceiver.IZatWiFiDBReceiverResponseListener;
import com.qti.location.sdk.IZatWiFiDBReceiver.IZatWiFiDBReceiverResponseListenerExt;
import com.qti.location.sdk.IZatWiFiDBReceiver.IZatAPLocationData;
import com.qti.location.sdk.utils.IZatDataValidation;
import com.qti.location.sdk.utils.IZatValidationResults.IZatDataTypes;
import com.qti.location.sdk.IZatWWANAdReceiver;
import com.qti.location.sdk.IZatWWANAdReceiver.IZatWWANAdRequestListener;
import com.qti.location.sdk.IZatPrecisePositioningService.IZatLocationResponse;
import com.qti.location.sdk.IZatPrecisePositioningService.IZatPrecisePositioningRequest;
import com.qti.location.sdk.IZatPrecisePositioningService.IZatPrecisePositioningCallback;
import com.qti.debugreport.IZatXTRAStatus;
import com.qti.location.sdk.IZatDebugReportingService.IZatXtraStatusCallback;
import com.qti.location.sdk.IZatDebugReportingService.IZatXtraStatus;
import com.qti.location.sdk.IZatDebugReportingService.IZatXtraDataStatus;
import com.qti.wifidbprovider.APObsLocData;
import com.qti.wifidbprovider.APScan;
import com.qti.wifidbprovider.IWiFiDBProvider;
import com.qti.wifidbprovider.IWiFiDBProviderResponseListener;
import com.qti.wifidbprovider.*;
import com.qti.wifidbreceiver.*;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.Executor;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collections;


/** @addtogroup IZatManager
    @{ */

/**
 * The IZatManager class is the entry point to Qualcomm Location services.
 * It requires the permission of android.permission.ACCESS_FINE_LOCATION.
 *
 * <pre>
 * <code>
 *    // Sample Code
 *
 *    import android.app.Activity;
 *    import android.os.Bundle;
 *    import android.view.View;
 *    import android.widget.Button;
 *    import com.qti.location.sdk.IZatManager;
 *    import com.qti.location.sdk.IZatFlpService;
 *
 *    public class FullscreenActivity extends Activity {
 *
 *       private IZatManager mIzatMgr = null;
 *       private IZatFlpService mFlpSevice = null;
 *
 *       &#64Override
 *       protected void onCreate(Bundle savedInstanceState) {
 *
 *            ...
 *            // get the instance of IZatManager
 *            mIzatMgr = IZatManager.getInstance(getApplicationContext());
 *
 *            ...
 *            final Button connectButton = (Button)findViewById(R.id.connectButton);
 *            connectButton.setOnClickListener(new View.OnClickListener() {
 *                &#64Override
 *                public void onClick(View v) {
 *
 *                    // connect to IZatFlpService through IZatManager
 *                    if (mIzatMgr != null) {
 *                        <b>mFlpSevice = mIzatMgr.connectFlpService();</b>
 *                    }
 *                }
 *            });
 *            ...
 *        }
 *    }
 * </code>
 * </pre>
 */
public class IZatManager {
    private static String TAG = "IZatManager";
    private static final boolean VERBOSE = Log.isLoggable(TAG, Log.VERBOSE);
    private static final String SDK_Version = "12.0.1";

    private static final int UNINITIALIZED_MASK = -1;
    private int mFlpFeatureMasks = UNINITIALIZED_MASK;
    private final int FEATURE_BIT_TIME_BASED_BATCHING_IS_SUPPORTED = 0x1;
    private final int FEATURE_BIT_DISTANCE_BASED_TRACKING_IS_SUPPORTED = 0x2;
    private final int FEATURE_BIT_ADAPTIVE_BATCHING_IS_SUPPORTED = 0x4;
    private final int FEATURE_BIT_DISTANCE_BASED_BATCHING_IS_SUPPORTED = 0x8;
    private final int FEATURE_BIT_OUTDOOR_TRIP_BATCHING_IS_SUPPORTED = 0x10;
    private final int FEATURE_BIT_AGPM_IS_SUPPORTED = 0x20;
    private final int FEATURE_BIT_CONSTELLATION_ENABLEMENT_IS_SUPPORTED = 0x40;
    private final int FEATURE_BIT_CONFORMITY_INDEX_IS_SUPPORTED = 0x80;
    private final int FEATURE_BIT_PRECISE_LOCATION_IS_SUPPORTED = 0x100;
    private final int FEATURE_BIT_GTP_LOCATION_IS_SUPPORTED = 0x200;
    private final int FEATURE_BIT_QESDK_GTP_LOCATION_IS_SUPPORTED = 0x400;
    private final int FEATURE_BIT_HW_RSSI_IS_SUPPORTED = 0x800;
    private final int FEATURE_BIT_HW_RTT_IS_SUPPORTED = 0x1000;
    private final int FEATURE_BIT_QWES_GTP_RSSI_IS_SUPPORTED = 0x2000;
    private final int FEATURE_BIT_QWES_GTP_RTT_IS_SUPPORTED = 0x4000;
    private final int FEATURE_BIT_QWES_WWAN_STANDARD_IS_SUPPORTED = 0x8000;
    private final int FEATURE_BIT_QWES_WWAN_PREMIUM_IS_SUPPORTED = 0x10000;

    private Map<IZatFlpServiceImpl.IZatSessionHandlerImpl, FlpRequestMapItem> mFlpRequestsMap
        = createIdMap();
    private Map<IZatFlpServiceImpl.IZatSessionHandlerImpl, FlpRequestMapItem> createIdMap() {
        Map<IZatFlpServiceImpl.IZatSessionHandlerImpl, FlpRequestMapItem> result =
            new HashMap<IZatFlpServiceImpl.IZatSessionHandlerImpl, FlpRequestMapItem>();
        return Collections.synchronizedMap(result);
    }

    private Map<IZatFlpService.IFlpLocationCallback,
                LocationCallbackWrapper> mFlpPassiveCbMap = createPassiveCbMap();
    private Map<IZatFlpService.IFlpLocationCallback,
                LocationCallbackWrapper> createPassiveCbMap() {
        Map<IZatFlpService.IFlpLocationCallback, LocationCallbackWrapper> result =
            new HashMap<IZatFlpService.IFlpLocationCallback, LocationCallbackWrapper>();
        return Collections.synchronizedMap(result);
    }

    private Map<IZatTestService.IFlpMaxPowerAllocatedCallback,
                MaxPowerAllocatedCallbackWrapper> mFlpMaxPowerCbMap = createMaxPowerCbMap();
    private Map<IZatTestService.IFlpMaxPowerAllocatedCallback,
                MaxPowerAllocatedCallbackWrapper> createMaxPowerCbMap() {
        Map<IZatTestService.IFlpMaxPowerAllocatedCallback,
            MaxPowerAllocatedCallbackWrapper> result =
            new HashMap<IZatTestService.IFlpMaxPowerAllocatedCallback,
                        MaxPowerAllocatedCallbackWrapper>();
        return Collections.synchronizedMap(result);
    }

    private Map<IZatGeofenceServiceImpl,
                IZatGeofenceService.IZatGeofenceCallback> mGeofenceClientCallbackMap
        = createGeofenceClientCallbackMap();
    private Map<IZatGeofenceServiceImpl,
                IZatGeofenceService.IZatGeofenceCallback> createGeofenceClientCallbackMap() {
        Map<IZatGeofenceServiceImpl, IZatGeofenceService.IZatGeofenceCallback> result =
            new HashMap<IZatGeofenceServiceImpl, IZatGeofenceService.IZatGeofenceCallback>();
        return Collections.synchronizedMap(result);
    }

    private Map<IZatGeofenceServiceImpl.IZatGeofenceHandleImpl,
                GeofenceMapItem> mGeofencesMap = createGeofencesMap();
    private Map<IZatGeofenceServiceImpl.IZatGeofenceHandleImpl,
                GeofenceMapItem> createGeofencesMap() {
        Map<IZatGeofenceServiceImpl.IZatGeofenceHandleImpl,
            GeofenceMapItem> result =
            new HashMap<IZatGeofenceServiceImpl.IZatGeofenceHandleImpl,
                        GeofenceMapItem>();
        return Collections.synchronizedMap(result);
    }

    private PendingIntent mGeofenceIntent;

    private Map<IZatDebugReportingServiceImpl,
                IZatDebugReportingService.IZatDebugReportCallback> mDebugReportClientCallbackMap
        = createDebugReportClientCallbackMap();
    private Map<IZatDebugReportingServiceImpl,
                IZatDebugReportingService.IZatDebugReportCallback>
        createDebugReportClientCallbackMap() {
            Map<IZatDebugReportingServiceImpl,
                IZatDebugReportingService.IZatDebugReportCallback> result =
                new HashMap<IZatDebugReportingServiceImpl,
                            IZatDebugReportingService.IZatDebugReportCallback>();
            return Collections.synchronizedMap(result);
    }

    private int mActiveGtpTbf;
    private int mActiveGtpWwanTbf;
    private GTPAccuracy mActiveGtpAccuracy;
    private GTPAccuracy mActiveGtpWwanAccuracy;
    private IZatGtpService.IZatGtpServiceCallback mActiveGtpCallback;
    private IZatGtpService.IZatGtpServiceCallback mPassiveGtpCallback;
    private IZatGtpService.IZatGtpServiceCallback mActiveGtpWwanCallback;
    private IZatGtpService.IZatGtpServiceCallback mPassiveGtpWwanCallback;


    private IZatNtnStatusCallback mntnStatusCallback;
    private IZatRobustLocationConfigService.IZatRLConfigCallback mRLConfigCallback;

    private IZatPrecisePositioningCallback mPrecisePositionCallback;
    private IZatPrecisePositioningCallbackWrapper mPrecisePositionCbWrapper;
    private IZatPrecisePositioningRequest mPrecisePositionRequest;

    private volatile IZatWiFiDBReceiverImpl mWiFiDBRecImpl;
    private volatile IZatWiFiDBProviderImpl mWiFiDBProvImpl;
    private volatile IZatFlpServiceImpl mFlpServiceImpl;
    private volatile IZatGeofenceServiceImpl mGeofenceServiceImpl;
    private volatile IZatTestServiceImpl mTestServiceImpl;
    private volatile IZatDebugReportingServiceImpl mDebugReportingServiceImpl;
    private volatile IZatGnssConfigServicesImpl mGnssConfigServicesImpl;
    private volatile IZatAltitudeReceiverImpl mAltitudeRecImpl;
    private volatile IZatGtpServiceImpl mGtpServiceImpl;
    private volatile IZatWWANAdReceiverImpl mWWANAdReceiverImpl;
    private volatile IZatPrecisePositioningServiceImpl mPrecisePositionServiceImpl;

    private static final int FLP_PASSIVE_LISTENER_HW_ID = -1;
    private static final int FLP_RESULT_SUCCESS = 0;

    private static final int FLP_SEESION_BACKGROUND = 1;
    private static final int FLP_SEESION_FOREGROUND = 2;
    private static final int FLP_SEESION_PASSIVE = 4;

    private static volatile int sFlpRequestsCnt = 0;
    private static final Object sFlpServiceLock = new Object();
    private static final Object sTestServiceLock = new Object();
    private static final Object sGeofenceServiceLock = new Object();
    private static final Object sGeofencesMapLock = new Object();
    private static final Object sGeofenceClientCallbackMapLock = new Object();
    private static final Object sDebugReportServiceLock = new Object();
    private static final Object sDebugReportCallbackMapLock = new Object();
    private static final Object sWiFiDBReceiverLock = new Object();
    private static final Object sWiFiDBProviderLock = new Object();
    private static final Object sGnssConfigServiceLock = new Object();
    private static final Object sntnStatusCallbackLock = new Object();
    private static final Object sRLConfigCallbackMapLock = new Object();
    private static final Object sAltitudeReceiverLock = new Object();
    private static final Object sGtpServiceLock = new Object();
    private static final Object sWWANAdReceiverLock = new Object();
    private static final Object sPrecisePositionServiceLock = new Object();

    private static final String REMOTE_IZAT_SERVICE_NAME = "com.qualcomm.location.izat.IzatService";

    private Context mContext;
    private static IZatManager sInstance = null;
    private static volatile IIzatService sIzatService = null;
    private GeofenceStatusCallbackWrapper mGeofenceCbWrapper = new GeofenceStatusCallbackWrapper();
    private DebugReportCallbackWrapper mDebugReportCbWrapper = new DebugReportCallbackWrapper();
    private WiFiDBReceiverRespListenerWrapper mWiFiDBReceiverRespListenerWrapper =
            new WiFiDBReceiverRespListenerWrapper();
    private WiFiDBProviderRespListenerWrapper mWiFiDBProviderRespListenerWrapper =
            new WiFiDBProviderRespListenerWrapper();
    private GnssConfigCallbackWrapper mGnssConfigCbWrapper =
            new GnssConfigCallbackWrapper();
    private AltitudeReceiverRespListenerWrapper mAltitudeReceiverRespListenerWrapper =
            new AltitudeReceiverRespListenerWrapper();
    private WWANAdRequestListenerWrapper mWWANAdRequestListener =
            new WWANAdRequestListenerWrapper();

    private volatile boolean mIsReConnecting = false;

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG,"onServiceConnected");
            mIsReConnecting = false;
            sIzatService = IIzatService.Stub.asInterface(service);
            try {
                sIzatService.registerProcessDeath(new Binder());
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException: " + e);
            }
        }
        @Override
        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG,"onServiceDisconnected");
            IzatServiceRecover();
        }
        @Override
        public void onBindingDied (ComponentName name) {
            Log.d(TAG,"onBindingDied");
            IzatServiceRecover();
        }

        private void IzatServiceRecover() {
            sIzatService = null;
            mIsReConnecting = true;
            if (sIzatService == null) {
                connectIzatService();
            }
            if (mFlpServiceImpl != null) {
                connectFlpService();
                restoreFlpSessions();
                restorePassiveListeners();
            }
            if (mGeofenceServiceImpl != null) {
                connectGeofenceService();
                restoreGeofenceSessions();
            }
            if (mDebugReportingServiceImpl != null) {
                connectDebugReportingService();
                restoreDebugReportService();
            }
            if (mTestServiceImpl != null) {
                connectTestService();
                restoreMaxPowerAllocatedCbs();
            }
            if (mGnssConfigServicesImpl != null) {
                connectGnssConfigServices();
            }
            if (mWiFiDBRecImpl != null) {
                try {
                    IWiFiDBReceiver wifiDBReceiver =
                            (IWiFiDBReceiver)(sIzatService.getWiFiDBReceiver());
                    // register the callbacks
                    synchronized (sWiFiDBReceiverLock) {
                        if (mWiFiDBRecImpl.mResponseListenerExt != null) {
                            wifiDBReceiver.registerResponseListenerExt(
                                    mWiFiDBReceiverRespListenerWrapper,
                                    mWiFiDBRecImpl.mWiFiReceiverIntent);
                        } else {
                            wifiDBReceiver.registerResponseListener(
                                    mWiFiDBReceiverRespListenerWrapper);
                        }
                    }
                    mWiFiDBRecImpl.mReceiver = wifiDBReceiver;
                } catch (RemoteException e) {
                }
            }
            if (mWiFiDBProvImpl != null) {
                try {
                    IWiFiDBProvider wifiDBProvider =
                            (IWiFiDBProvider)(sIzatService.getWiFiDBProvider());
                    // register the callbacks
                    synchronized (sWiFiDBProviderLock) {
                        wifiDBProvider.registerResponseListener(mWiFiDBProviderRespListenerWrapper,
                                mWiFiDBProvImpl.mWiFiProviderIntent);
                    }
                    mWiFiDBProvImpl.mProvider = wifiDBProvider;
                } catch (RemoteException e) {
                }
            }
            if (mAltitudeRecImpl != null) {
                connectToAltitudeReceiver(mAltitudeRecImpl.mResponseListener,
                        mAltitudeRecImpl.mAltReceiverIntent);
            }
            if (mGtpServiceImpl != null) {
                connectToGtpService();
                restoreGtpSessions();
            }
            if (mWWANAdReceiverImpl != null) {
                connectToWWANAdReceiver(mWWANAdReceiverImpl.mReqListener);
            }
            if (mPrecisePositionServiceImpl != null) {
                connectToPrecisePositioningService();
                restorePreciseSession();
            }
        }
    };

    /**
     * Returns an instance of IZatManager.
     *
     * @param context Context object.
     *
     * @throws IZatIllegalArgumentException The context is NULL.
     * @return
     * The instance of IZatManager.
     */
    public static synchronized IZatManager getInstance(Context context)
        throws IZatIllegalArgumentException {
        if (null == context) {
            throw new IZatIllegalArgumentException("null argument");
        }

        if (null == sInstance) {
            sInstance = new IZatManager(context);
        }

        return sInstance;
    }

    private IZatManager(Context context) {
        // To avoid collisions with other process, use thread id + 1 byte for counter
        sFlpRequestsCnt = android.os.Process.myTid() << 8;
        if (VERBOSE) {
            Log.v(TAG, "IZatManager ctor sFlpRequestsCnt=" + sFlpRequestsCnt);
        }
        mContext = context;
    }

    /**
     * Gets the Qualcomm Location SDK version and the IZatService version.
     * <p>
     * This API requires that IZatService is present, otherwise
     * an {@link IZatServiceUnavailableException} is thrown.
     *
     * This returns version information in format X.Y.Z:A.B.C,
     * where XYZ are major, minor, and revision IDs for the SDK
     * and ABC are major, minor, and revision IDs for the service.
     * This API may take as long as several seconds to execute.
     * It is recommended not to call this API in the main thread.
     * </p>
     * @throws IZatServiceUnavailableException  The IZatService is not present.
     * @return
     * The versions of the SDK and the service.
     */
    public String getVersion()
        throws IZatServiceUnavailableException {
        String service_version;

        if (null == sIzatService) {
            connectIzatService();
        }

        try {
            service_version = sIzatService.getVersion();
        } catch (RemoteException e) {
            throw new RuntimeException("Failed to get IzatService version", e);
        }

        if (null == service_version) {
            service_version = "1.0.0";
        }

        String version = SDK_Version + ":" + service_version;

        return version;
    }

    private void setFeatureMask(IFlpService flpService) {
        if (null == flpService) {
            throw new IZatServiceUnavailableException("FLP Service is not available.");
        }
        synchronized (sFlpServiceLock) {
            if (UNINITIALIZED_MASK == mFlpFeatureMasks) {
                try {
                    for (int retries = 0; retries < 10; retries++) {
                        mFlpFeatureMasks = flpService.getAllSupportedFeatures();
                        if (UNINITIALIZED_MASK != mFlpFeatureMasks) {
                            break;
                        }
                        Thread.sleep(200);
                    }
                } catch (InterruptedException | RemoteException ignored) {
                }
            }
        }
        Log.d(TAG, "got mFlpFeatureMasks: " + mFlpFeatureMasks);
    }

    private synchronized void connectIzatService() {
        if (null == sIzatService) {
            if (VERBOSE) {
                Log.d(TAG, "Connecting to Izat service by name [" +
                      REMOTE_IZAT_SERVICE_NAME + "]");
            }

            if (!mIsReConnecting) {
                // check if IZat service installed
                PackageManager pm = mContext.getPackageManager();
                ResolveInfo ri = pm.resolveService(new Intent(REMOTE_IZAT_SERVICE_NAME), 0);
                if (null == ri) {
                    Log.e(TAG, "Izat service (" + REMOTE_IZAT_SERVICE_NAME + ") not installed");
                    throw new IZatServiceUnavailableException("Izat service unavailable.");
                }

                Intent intent = new Intent(REMOTE_IZAT_SERVICE_NAME);
                intent.setPackage("com.qualcomm.location");
                mContext.bindService(intent, Context.BIND_AUTO_CREATE, new Executor() {
                            @Override
                            public void execute(Runnable command){
                                command.run();
                            }
                        },
                        mServiceConnection);
            }
            for (int i = 0; i < 10; i++) {
                if (null != sIzatService) {
                    return;
                }
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ie){
                }
            }
            throw new IZatServiceUnavailableException("connectIzatService time out.");
        }
    }


    /**
     * Gets an IZatFlpService interface.
     *
     * This API may take several seconds to execute.
     * Qualcomm Technologies, Inc. recommends not to call this API in the main thread.
     *
     * Permission required: com.qualcomm.permission.ACCESS_COARSE_LOCATION </p>
     * @throws IZatServiceUnavailableException The FLP service is not present.
     * @throws SecurityException           The SDK client is not a system App.
     *
     * @return
     * An IZatFlpService interface.
     *
     * @dependencies
     * This API requires that the IZat FLP service is present, otherwise
     * an IZatServiceUnavailableException is thrown.
     */
    public IZatFlpService connectFlpService()
        throws IZatServiceUnavailableException {

        if (null == sIzatService) {
            connectIzatService();
        }

        try {
            IFlpService flpService = (IFlpService)(sIzatService.getFlpService());
            if (null == flpService) {
                throw new IZatServiceUnavailableException("FLP Service is not available.");
            }
            setFeatureMask(flpService);

            if ((mFlpFeatureMasks & FEATURE_BIT_DISTANCE_BASED_TRACKING_IS_SUPPORTED)>0) {
                if (mFlpServiceImpl == null) {
                    mFlpServiceImpl = new IZatFlpServiceImpl(flpService);
                } else {
                    mFlpServiceImpl.mService = flpService;
                }
            } else {
                Log.e(TAG, "Izat FLP positioning is not supported on this device.");
                return null;
            }
        } catch (RemoteException e) {
            throw new RuntimeException("Failed to get IFlpService", e);
        }
        return mFlpServiceImpl;
    }

    /**
     * Disconnects form the specified IZatFlpService interface.
     *
     * @param service Handle object that is returned from
     * the IZatManager#connectFlpService().
     * @throws IZatIllegalArgumentException The input service is NULL.
     * @return
     * None.
     */
    public void disconnectFlpService(IZatFlpService service)
        throws IZatIllegalArgumentException {
        if (null == service || !(service instanceof IZatFlpServiceImpl))
            throw new IZatIllegalArgumentException();

        // stop all the undergoing sessions before disconnecting
        try {
            synchronized (sFlpServiceLock) {
                IFlpService flpService =
                   (IFlpService)(sIzatService.getFlpService());
                if (null == flpService) {
                    throw new IZatServiceUnavailableException("FLP Service is not available.");
                }
                FlpRequestMapItem mapItem = null;
                for (IZatFlpServiceImpl.IZatSessionHandlerImpl key : mFlpRequestsMap.keySet()) {
                    mapItem = mFlpRequestsMap.get(key);
                    // stop session
                    if (flpService.stopFlpSession(mapItem.getHwId()) !=
                            FLP_RESULT_SUCCESS) {
                        Log.e(TAG, "stopFlpSession failed in disconnecting");
                        return;
                    }
                    if ((mFlpFeatureMasks & FEATURE_BIT_DISTANCE_BASED_BATCHING_IS_SUPPORTED)>0) {
                        // un-register cb
                        if (NotificationType.NOTIFICATION_WHEN_BUFFER_IS_FULL ==
                                mapItem.getNotifyType()) {
                            flpService.unregisterCallback(FLP_SEESION_BACKGROUND,
                                                          mapItem.getCbWrapper());
                        }
                        if (NotificationType.NOTIFICATION_ON_EVERY_LOCATION_FIX ==
                                mapItem.getNotifyType()) {
                            flpService.unregisterCallback(FLP_SEESION_FOREGROUND,
                                                          mapItem.getCbWrapper());
                        }
                    } else {
                        flpService.unregisterCallback(FLP_SEESION_PASSIVE,
                                                      mapItem.getCbWrapper());
                    }

                    // unregister if any session status handlers are registered
                    if (mapItem.getStatusCallback() != null) {
                        flpService.unregisterForSessionStatus(mapItem.getStatusCallback());
                    }
                }
                mFlpRequestsMap.clear();

                // remove all passive listeners.
                LocationCallbackWrapper cbWrapper = null;
                for (IZatFlpService.IFlpLocationCallback key : mFlpPassiveCbMap.keySet()) {
                    cbWrapper = mFlpPassiveCbMap.get(key);
                    flpService.unregisterCallback(FLP_SEESION_PASSIVE,
                                                  cbWrapper);
                }
                mFlpPassiveCbMap.clear();
                mFlpServiceImpl = null;
            }
        } catch (RemoteException e) {
            throw new RuntimeException("Failed stop all flp sessions", e);
        }
    }

    /**
     * Gets an {@link IZatTestService} interface.
     *
     * Permission required: com.qualcomm.permission.ACCESS_FINE_LOCATION </p>
     *                      com.qualcomm.permission.BIND_DEVICE_ADMIN </p>
     * <p>
     * This API requires that the IZatTest service is present, otherwise
     * an {@link IZatServiceUnavailableException} is thrown.<br/>
     * This API may take as long as several seconds to execute.
     * It is recommended not to call this API in main thread.
     * Only a system application is allowed to call this API.
     * </p>
     *
     * @throws IZatServiceUnavailableException The Test service is not present.
     * @throws SecurityException           The SDK client is not a system App.
     * @return An {@link IZatTestService} interface.
     */
    public IZatTestService connectTestService()
        throws IZatServiceUnavailableException {

        if (null == sIzatService) {
            connectIzatService();
        }

        try {
            ITestService testService = (ITestService)(sIzatService.getTestService());
            if (null == testService) {
                throw new IZatServiceUnavailableException("Test Service is not available.");
            }
            if (mTestServiceImpl == null) {
                mTestServiceImpl = new IZatTestServiceImpl(testService);
            } else {
                mTestServiceImpl.mService = testService;
            }
        } catch (RemoteException e) {
            throw new RuntimeException("Failed to get ITestService", e);
        }
        return mTestServiceImpl;
    }

    /**
     * Disconnects from the specified {@link IZatTestService} interface.
     *
     * @param service Handle object that is returned from
     * {@link IZatManager#connectTestService()}.
     * @throws IZatIllegalArgumentException The input service is NULL.
     */
    public void disconnectTestService(IZatTestService service)
        throws IZatIllegalArgumentException {
        if (null == service || !(service instanceof IZatTestServiceImpl))
            throw new IZatIllegalArgumentException();

        synchronized (sTestServiceLock) {
            for (IZatTestService.IFlpMaxPowerAllocatedCallback key :
                     mFlpMaxPowerCbMap.keySet()) {
                service.deregisterForMaxPowerAllocatedChange(key);
            }
            mFlpMaxPowerCbMap.clear();
            mTestServiceImpl = null;
        }
    }

    /**
     * Gets an {@link IZatGeofenceService} interface.
     *
     * Permission required: com.qualcomm.permission.ACCESS_FINE_LOCATION </p>
     * <p>
     * This API requires that the Geofence service is present, otherwise
     * an {@link IZatServiceUnavailableException} is thrown.<br/>
     * This API may take as long as several seconds to execute.
     * It is recommended not to call this API in the main thread.
     * </p>
     *
     * @throws IZatServiceUnavailableException The Geofence service is not present.
     * @throws SecurityException           The SDK client is not a system App.
     * @return An {@link IZatGeofenceService} interface.
     */
    public IZatGeofenceService connectGeofenceService()
        throws IZatServiceUnavailableException {
        if (null == sIzatService) {
            connectIzatService();
        }

        try {
            IGeofenceService geofenceService =
                (IGeofenceService)(sIzatService.getGeofenceService());
            if (null == geofenceService) {
                throw new IZatServiceUnavailableException("Geofence Service is not available.");
            }
            // register the geofence callback
            synchronized (sGeofenceServiceLock) {
                geofenceService.registerCallback(mGeofenceCbWrapper);
            }
            if (mGeofenceServiceImpl == null) {
                mGeofenceServiceImpl = new IZatGeofenceServiceImpl(geofenceService);
            } else {
                mGeofenceServiceImpl.mService = geofenceService;
            }
        } catch (RemoteException e) {
            throw new RuntimeException("Failed to get IGeofenceService", e);
        }
        return mGeofenceServiceImpl;
    }

   /**
     * Disconnects from the specified {@link IZatGeofenceService} interface.
     *
     * @param service Handle object that is returned from
     * {@link IZatManager#connectGeofenceService()}.
     * @throws IZatIllegalArgumentException The input service is NULL.
     */
    public void disconnectGeofenceService(IZatGeofenceService service)
        throws IZatIllegalArgumentException {
        if (null == service || !(service instanceof IZatGeofenceServiceImpl))
            throw new IZatIllegalArgumentException();

        try {
            synchronized (sGeofenceServiceLock) {
                IGeofenceService geofenceService =
                   (IGeofenceService)(sIzatService.getGeofenceService());
                if (null == geofenceService) {
                    throw new IZatServiceUnavailableException("Geofence Service is not available.");
                }
                // remove all geofence added.
                GeofenceMapItem mapItem = null;
                for (IZatGeofenceServiceImpl.IZatGeofenceHandleImpl key :
                         mGeofencesMap.keySet()) {
                    mapItem = mGeofencesMap.get(key);
                    geofenceService.removeGeofence(mapItem.getHWGeofenceId());
                }
                // un-register the geofence callback
                geofenceService.unregisterCallback(mGeofenceCbWrapper);
            }
            synchronized (sGeofencesMapLock) {
                mGeofencesMap.clear();
            }
            // delete the client callback
            synchronized (sGeofenceClientCallbackMapLock) {
                for (IZatGeofenceServiceImpl key : mGeofenceClientCallbackMap.keySet()) {
                    if (service == key) {
                        mGeofenceClientCallbackMap.remove(key);
                        break;
                    }
                }
            }
            mGeofenceServiceImpl = null;
        } catch (RemoteException e) {
            throw new RuntimeException("Failed to remove all geofence added", e);
        }
    }

   /**
    * Gets an {@link IZatDebugReportingService} interface.
    *
    * Permission required: com.qualcomm.permission.ACCESS_FINE_LOCATION </p>
    *                      com.qualcomm.permission.BIND_DEVICE_ADMIN </p>
    * <p>
    * This API requires that the IZat debug reporting service is present, otherwise
    * an {@link IZatServiceUnavailableException} is thrown.<br/>
    * This API may take as long as several seconds to execute.
    * It is recommended not to call this API in the main thread.
    * </p>
    *
    * @throws IZatServiceUnavailableException The Debug Reporting Service is not present.
    * @throws SecurityException           The SDK client is not a system App.
    * @return An {@link IZatDebugReportingService} interface.
    */
   public IZatDebugReportingService connectDebugReportingService()
        throws IZatServiceUnavailableException {
        if (null == sIzatService) {
            connectIzatService();
        }

        try {
            IDebugReportService debugReportService =
                (IDebugReportService)(sIzatService.getDebugReportService());
            if (null == debugReportService) {
                throw new IZatServiceUnavailableException("Debug Reporting Service is not available.");
            }
            synchronized (sDebugReportServiceLock) {
                debugReportService.registerForDebugReporting(mDebugReportCbWrapper);
            }
            if (mDebugReportingServiceImpl == null) {
                mDebugReportingServiceImpl = new IZatDebugReportingServiceImpl(debugReportService);
            } else {
                mDebugReportingServiceImpl.mService = debugReportService;
            }
        } catch (RemoteException e) {
            throw new RuntimeException("Failed to get IDebugReportService", e);
        }
        return mDebugReportingServiceImpl;
   }

   /**
     * Disconnects from the specified {@link IZatDebugReportingService} interface.
     *
     * @param service Handle object that is returned from
     * {@link IZatManager#connectDebugReportingService()}.
     * @throws IZatIllegalArgumentException The input service is NULL.
     */
   public void disconnectDebugReportingService(IZatDebugReportingService service) {
       if (null == service || !(service instanceof IZatDebugReportingServiceImpl)) {
            throw new IZatIllegalArgumentException();
       }

       try {
           IDebugReportService debugReportService =
               (IDebugReportService)(sIzatService.getDebugReportService());
            if (null == debugReportService) {
                throw new IZatServiceUnavailableException("Debug Report Service is not available.");
            }
            synchronized (sDebugReportServiceLock) {
               debugReportService.unregisterForDebugReporting(mDebugReportCbWrapper);
            }
            synchronized (sDebugReportCallbackMapLock) {
                for(IZatDebugReportingServiceImpl key :
                        mDebugReportClientCallbackMap.keySet()) {
                    if (service == key) {
                        mDebugReportClientCallbackMap.remove(key);
                        break;
                    }
                }
            }
            mDebugReportingServiceImpl = null;
        } catch (RemoteException e) {
            throw new RuntimeException("Failed to disconnect DebugReportService", e);
        }
   }

    /**
     * Gets an {@link IZatWiFiDBReceiver} interface.
     *
     * Permission required: com.qualcomm.permission.GTPWIFI_PERMISSION </p>
     * <p>
     * This API requires that the Wi-Fi DB receiver is present, otherwise
     * an {@link IZatServiceUnavailableException} is thrown.<br/>
     * This API may take as long as several seconds to execute.
     * It is recommended not to call this API in the main thread.
     * </p>
     *
     * @param listener Handle object that is returned from the {@link IZatWiFiDBReceiver} service.
     *                 Can not be null, or a IZatIllegalArgumentException will be thrown.
     * @param WiFiReceiverIntent Pending intent to handle onServiceRequest() calls from SDK.
     *                 Can not be null, or a IZatIllegalArgumentException will be thrown.
     * @throws IZatServiceUnavailableException The Wi-Fi DB
     *                                         receiver is not
     *                                         present.
     * @throws IZatIllegalArgumentException    One of the arguments is null.
     * @throws SecurityException           The SDK client is not a system App.
     * @return an {@link IZatWiFiDBReceiver} interface.
     */
    @Deprecated
    public IZatWiFiDBReceiver connectToWiFiDBReceiver(
            IZatWiFiDBReceiverResponseListener listener)
            throws IZatServiceUnavailableException {
        if (null == sIzatService) {
            connectIzatService();
        }

        if (null == listener) {
            throw new IZatIllegalArgumentException("Listener can not be null.");
        }

        try {
            if(null == mWiFiDBRecImpl) {
                IWiFiDBReceiver wifiDBReceiver =
                        (IWiFiDBReceiver)(sIzatService.getWiFiDBReceiver());
                // register the callbacks
                synchronized (sWiFiDBReceiverLock) {
                    wifiDBReceiver.registerResponseListener(mWiFiDBReceiverRespListenerWrapper);
                }
                mWiFiDBRecImpl = new IZatWiFiDBReceiverImpl(wifiDBReceiver, listener);
            }
            return mWiFiDBRecImpl;

        } catch (RemoteException e) {
            throw new RuntimeException("Failed to get IWiFiDBReceiver", e);
        }
    }

    /**
     * Gets an {@link IZatWiFiDBReceiver} interface.
     *
     * Permission required: com.qualcomm.permission.GTPWIFI_PERMISSION </p>
     * <p>
     * This API requires that the Wi-Fi DB receiver is present, otherwise
     * an {@link IZatServiceUnavailableException} is thrown.<br/>
     * This API may take as long as several seconds to execute.
     * It is recommended not to call this API in the main thread.
     * </p>
     *
     * @param listener Handle object that is returned from the {@link IZatWiFiDBReceiver} service.
     *                 Can not be null, or a IZatIllegalArgumentException will be thrown.
     * @param WiFiReceiverIntent Pending intent to handle onServiceRequest() calls from SDK.
     *                 Can not be null, or a IZatIllegalArgumentException will be thrown.
     * @throws IZatServiceUnavailableException The Wi-Fi DB
     *                                         receiver is not
     *                                         present.
     * @throws IZatIllegalArgumentException    One of the arguments is null.
     * @throws SecurityException           The SDK client is not a system App.
     * @return an {@link IZatWiFiDBReceiver} interface.
     */
    public IZatWiFiDBReceiver connectToWiFiDBReceiver(
                IZatWiFiDBReceiverResponseListenerExt listener, PendingIntent WiFiReceiverIntent)
                throws IZatServiceUnavailableException, IZatIllegalArgumentException {

        if (null == sIzatService) {
            connectIzatService();
        }

        if (null == listener) {
            throw new IZatIllegalArgumentException("Listener can not be null.");
        }

        if (null == WiFiReceiverIntent) {
            throw new IZatIllegalArgumentException("Pending intent can not be null.");
        }

        try {
            if(null == mWiFiDBRecImpl) {
                IWiFiDBReceiver wifiDBReceiver =
                    (IWiFiDBReceiver)(sIzatService.getWiFiDBReceiver());
                // register the callbacks
                synchronized (sWiFiDBReceiverLock) {
                    wifiDBReceiver.registerResponseListenerExt(mWiFiDBReceiverRespListenerWrapper,
                            WiFiReceiverIntent);
                }
                mWiFiDBRecImpl = new IZatWiFiDBReceiverImpl(wifiDBReceiver, listener);
                mWiFiDBRecImpl.mWiFiReceiverIntent = WiFiReceiverIntent;
            }
            return mWiFiDBRecImpl;

        } catch (RemoteException e) {
            throw new RuntimeException("Failed to get IWiFiDBReceiver", e);
        }
    }

    /**
     * Disconnects the specified {@link IZatWiFiDBReceiver}
     * interface.
     *
     * @param receiver Handle object that is returned from
     * {@link IZatManager#connectToWiFiDBReceiver()}.
     * @throws IZatIllegalArgumentException The input receiver is NULL.
     *
     */
    public void disconnectFromWiFiDBReceiver(IZatWiFiDBReceiver receiver)
        throws IZatIllegalArgumentException {
        if (null == receiver || !(receiver instanceof IZatWiFiDBReceiverImpl))
            throw new IZatIllegalArgumentException();
        try {
            IWiFiDBReceiver wifiDBReceiver =
                    (IWiFiDBReceiver)(sIzatService.getWiFiDBReceiver());
            synchronized (sWiFiDBReceiverLock) {
                wifiDBReceiver.removeResponseListener(mWiFiDBReceiverRespListenerWrapper);
            }
            mWiFiDBRecImpl = null;
        } catch (RemoteException e) {
            throw new RuntimeException("Failed to disconnect WiFiDBReceiver", e);
        }
    }

    /**
     * Gets an {@link IZatWWANDBReceiver} interface.
     * <p>
     * This API requires that WWAN DB Receiver is present, otherwise
     * an {@link IZatServiceUnavailableException}
     * is thrown.<br/>
     *
     * This API may take as long as several seconds to execute.
     * It is recommended not to call this API in the main thread.
     * </p>
     *
     * @param listener Handle object that is returned from
     *                     the {@link IZatWWANDBReceiver} service.
     *                 Can not be null, or a IZatIllegalArgumentException will be thrown.
     * @throws IZatServiceUnavailableException The WWAN DB
     *                                         Receiver is not
     *                                         present.
     * @throws SecurityException           The SDK client is not a system App.
     * @return an {@link IZatWWANDBReceiver} interface.
     */
    public IZatWWANDBReceiver connectToWWANDBReceiver(
            IZatWWANDBReceiverResponseListener listener)
            throws IZatServiceUnavailableException{
        return null;
    }

    /**
     * Gets an {@link IZatWWANDBReceiver} interface.
     * <p>
     * This API requires that WWAN DB Receiver is present, otherwise
     * an {@link IZatServiceUnavailableException}
     * is thrown.<br/>
     *
     * This API may take as long as several seconds to execute.
     * It is recommended not to call this API in the main thread.
     * </p>
     *
     * @param listener Handle object that is returned from
     *                     the {@link IZatWWANDBReceiver} service.
     *                 Can not be null, or a IZatIllegalArgumentException will be thrown.
     * @param WWANReceiverIntent Pending intent to handle onServiceRequest() calls from SDK.
     *                 Can not be null, or a IZatIllegalArgumentException will be thrown.
     * @throws IZatServiceUnavailableException The WWAN DB
     *                                         Receiver is not
     *                                         present.
     * @throws IZatIllegalArgumentException    One of the arguments is null.
     * @throws SecurityException           The SDK client is not a system App.
     * @return an {@link IZatWWANDBReceiver} interface.
     */
    public IZatWWANDBReceiver connectToWWANDBReceiver(
            IZatWWANDBReceiverResponseListenerExt listener, PendingIntent WWANReceiverIntent)
            throws IZatServiceUnavailableException, IZatIllegalArgumentException {
        return null;
    }

    /**
     * Disconnects the specified {@link IZatWWANDBReceiver}
     * interface.
     *
     * @param receiver Handle object that is returned from
     * {@link IZatManager#connectToWWANDBReceiver()}.
     *
     * @throws IZatIllegalArgumentException The input receiver is NULL.
     */
    public void disconnectFromWWANDBReceiver(IZatWWANDBReceiver receiver)
            throws IZatIllegalArgumentException {
    }

    /**
     * Gets an {@link IZatWiFiDBProvider} interface.
     * <p>
     * This API requires that the Wi-Fi DB provider is present, otherwise
     * an {@link IZatServiceUnavailableException} is thrown.<br/>
     * This API may take as long as several seconds to execute.
     * It is recommended not to call this API in the main thread.
     * </p>
     * Note that usage of this API implicitly means that the end user
     * accepts any license agreement required.
     * </p>
     *
     * Permission required: com.qualcomm.permission.GTPWIFI_CROWDSOURCING_PERMISSION </p>
     * @param listener Handle object that is returned from the {@link IZatWiFiDBProvider} service.
     *                 Can not be null, or a IZatIllegalArgumentException will be thrown.
     * @param WiFiProviderIntent Pending intent to handle onServiceRequest() calls from SDK.
     *                 Can not be null, or a IZatIllegalArgumentException will be thrown.
     * @throws IZatServiceUnavailableException The Wi-Fi DB
     *                                         provider is not
     *                                         present.
     * @throws IZatIllegalArgumentException    One of the arguments is null.
     * @throws SecurityException           The SDK client is not a system App.
     *
     * @return an {@link IZatWiFiDBProvider} interface.
     */
    public IZatWiFiDBProvider connectToWiFiDBProvider(
            IZatWiFiDBProviderResponseListener listener, PendingIntent WiFiProviderIntent)
            throws IZatServiceUnavailableException, IZatIllegalArgumentException {

        if (null == sIzatService) {
            connectIzatService();
        }
        if (null == listener) {
            throw new IZatIllegalArgumentException("Listener can not be null.");
        }

        if (null == WiFiProviderIntent) {
            throw new IZatIllegalArgumentException("Pending intent can not be null.");
        }

        try {
            if(null == mWiFiDBProvImpl) {
                IWiFiDBProvider wifiDBProvider =
                        (IWiFiDBProvider)(sIzatService.getWiFiDBProvider());
                // register the callbacks
                synchronized (sWiFiDBProviderLock) {
                    wifiDBProvider.registerResponseListener(mWiFiDBProviderRespListenerWrapper,
                            WiFiProviderIntent);
                }
                mWiFiDBProvImpl = new IZatWiFiDBProviderImpl(wifiDBProvider, listener);
                mWiFiDBProvImpl.mWiFiProviderIntent = WiFiProviderIntent;
            }
            return mWiFiDBProvImpl;

        } catch (RemoteException e) {
            throw new RuntimeException("Failed to get IWiFiDBReceiver", e);
        }
    }

    /**
     * Disconnects the specified {@link IZatWiFiDBProvider}
     * interface.
     *
     * Permission required: com.qualcomm.permission.GTPWIFI_CROWDSOURCING_PERMISSION </p>
     * @param provider Handle object that is returned from
     * {@link IZatManager#connectToWiFiDBProvider()}.
     * @throws IZatIllegalArgumentException The input provider is NULL.
     *
     */
    public void disconnectFromWiFiDBProvider(IZatWiFiDBProvider provider)
            throws IZatIllegalArgumentException {
        if (null == provider || !(provider instanceof IZatWiFiDBProviderImpl))
            throw new IZatIllegalArgumentException();
        try {
            IWiFiDBProvider wifiDBProvider =
                    (IWiFiDBProvider)(sIzatService.getWiFiDBProvider());
            synchronized (sWiFiDBReceiverLock) {
                wifiDBProvider.removeResponseListener(mWiFiDBProviderRespListenerWrapper);
            }
            mWiFiDBProvImpl = null;
        } catch (RemoteException e) {
            throw new RuntimeException("Failed to disconnect WifiDBProvider", e);
        }
    }

    /**
     * Gets an {@link IZatWWANDBProvider} interface.
     * <p>
     * This API requires that WWAN DB Provider is present, otherwise
     * an {@link IZatServiceUnavailableException}
     * is thrown.<br/>
     *
     * This API may take as long as several seconds to execute.
     * It is recommended not to call this API in the main thread.
     * </p>
     * Note that usage of this API implicitly means that the end user
     * accepts any license agreement required.
     * </p>
     *
     * Permission required: com.qualcomm.permission.GTPWIFI_CROWDSOURCING_PERMISSION </p>
     * @param listener Handle object that is returned from
     *                 the {@link IZatWWANDBProvider} service.
     *                 Can not be null, or a IZatIllegalArgumentException will be thrown.
     * @param WWANProviderIntent Pending intent to handle onServiceRequest() calls from SDK.
     *                 Can not be null, or a IZatIllegalArgumentException will be thrown.
     * @throws IZatServiceUnavailableException The WWAN DB
     *                                         Provider is not
     *                                         present.
     * @throws IZatIllegalArgumentException    One of the arguments is null.
     * @throws SecurityException           The SDK client is not a system App.
     * @return an {@link IZatWWANDBProvider} interface.
     */
    public IZatWWANDBProvider connectToWWANDBProvider(
            IZatWWANDBProviderResponseListener listener, PendingIntent WWANProviderIntent)
            throws IZatServiceUnavailableException, IZatIllegalArgumentException {
        return null;
    }

    /**
     * Disconnects the specified {@link IZatWWANDBProvider}
     * interface.
     *
     * @param provider Handle object that is returned from
     * {@link IZatManager#connectToWWANDBProvider()}.
     *
     * @throws IZatIllegalArgumentException The input provider is NULL.
     */
    public void disconnectFromWWANDBProvider(IZatWWANDBProvider provider)
            throws IZatIllegalArgumentException {
    }

    /**
     * Gets an {@link IZatAltitudeReceiver} interface.
     * <p>
     * This API requires that Altitude Receiver is present, otherwise
     * an {@link IZatServiceUnavailableException}
     * is thrown.<br/>
     *
     * This API may take as long as several seconds to execute.
     * It is recommended not to call this API in the main thread.
     * </p>
     * Note that usage of this API implicitly means that the end user
     * accepts any license agreement required.
     * </p>
     *
     * Permission required: com.qualcomm.permission.ACCESS_FINE_LOCATION </p>
     *                      com.qualcomm.permission.ALTITUDEPROVIDER_PERMISSION </p>
     * @param listener Handle object that is returned from
     *                 the {@link IZatAltitudeReceiver} service.
     *                 Can not be null, or a IZatIllegalArgumentException will be thrown.
     * @param altitudeReceiverIntent Pending intent to handle altitude lookup calls from SDK.
     *                 Can not be null, or a IZatIllegalArgumentException will be thrown.
     * @throws IZatServiceUnavailableException The Altitude Receiver is not present.
     * @throws IZatIllegalArgumentException    One of the arguments is null.
     * @throws SecurityException           The SDK client is not a system App.
     * @return an {@link IZatWWANDBProvider} interface.
     */
    public IZatAltitudeReceiver connectToAltitudeReceiver(
            IZatAltitudeReceiverResponseListener listener, PendingIntent altitudeReceiverIntent)
            throws IZatServiceUnavailableException, IZatIllegalArgumentException {

        if (sIzatService == null) {
            connectIzatService();
        }
        if (null == listener) {
            throw new IZatIllegalArgumentException("Listener can not be null.");
        }

        if (null == altitudeReceiverIntent) {
            throw new IZatIllegalArgumentException("Pending intent can not be null.");
        }

        try {
            if(null == mAltitudeRecImpl) {
                IAltitudeReceiver altitudeReceiver =
                        (IAltitudeReceiver)(sIzatService.getAltitudeReceiver());
                if (null == altitudeReceiver) {
                    throw new IZatServiceUnavailableException("Altitude Receiver is not available");
                }
                // register the callbacks
                synchronized (sAltitudeReceiverLock) {
                    altitudeReceiver.registerResponseListener(mAltitudeReceiverRespListenerWrapper,
                            altitudeReceiverIntent);
                }
                mAltitudeRecImpl = new IZatAltitudeReceiverImpl(altitudeReceiver, listener);
                mAltitudeRecImpl.mAltReceiverIntent = altitudeReceiverIntent;
            }
            return mAltitudeRecImpl;
        } catch (RemoteException e) {
            throw new RuntimeException("Failed to get IAltitudeReceiver", e);
        }

    }

    /**
     * Disconnects the specified {@link IZatAltitudeReceiver}
     * interface.
     *
     * @param provider Handle object that is returned from
     * {@link IZatManager#connectToAltitudeReceiver()}.
     *
     * @throws IZatIllegalArgumentException The input provider is NULL.
     */
    public void disconnectFromAltitudeReceiver(IZatAltitudeReceiver receiver)
            throws IZatIllegalArgumentException {
        if (null == receiver || !(receiver instanceof IZatAltitudeReceiver))
            throw new IZatIllegalArgumentException();
        try {
            IAltitudeReceiver altitudeReceiver =
                    (IAltitudeReceiver)(sIzatService.getAltitudeReceiver());
            synchronized (sAltitudeReceiverLock) {
                altitudeReceiver.removeResponseListener(mAltitudeReceiverRespListenerWrapper);
            }
            mAltitudeRecImpl = null;
        } catch (RemoteException e) {
            throw new RuntimeException("Failed to disconnect AltitudeReceiver", e);
        }
    }

    /**
     * Gets an {@link IZatGnssConfigServices} interface.
     * <p>
     * This API requires that the IZat Gnss Config services are present, otherwise
     * an {@link IZatServiceUnavailableException} is thrown.<br/>
     * This API may take as long as several seconds to execute.
     * It is recommended not to call this API in the main thread.
     * </p>
     *
     * @throws IZatServiceUnavailableException The Gnss Config Services are not present.
     * @throws SecurityException           The SDK client is not a system App.
     * @return An {@link IZatGnssConfigServices} interface.
     */
    public IZatGnssConfigServices connectGnssConfigServices()
            throws IZatServiceUnavailableException {

        if (null == sIzatService) {
            connectIzatService();
        }

        try {
            setFeatureMask((IFlpService)(sIzatService.getFlpService()));
            IGnssConfigService gnssConfigService =
                (IGnssConfigService)(sIzatService.getGnssConfigService());
            if (null == gnssConfigService) {
                throw new IZatServiceUnavailableException("Gnss Config Service is not available.");
            }
            if (mGnssConfigServicesImpl == null) {
                mGnssConfigServicesImpl = new IZatGnssConfigServicesImpl(gnssConfigService);
            } else {
                mGnssConfigServicesImpl.mService = gnssConfigService;
            }
            return mGnssConfigServicesImpl;
        } catch (RemoteException e) {
            throw new RuntimeException("Failed to get IGnssConfigService", e);
        }
    }

    /**
      * Disconnects from the specified {@link IZatGnssConfigServices} interface.
      *
      * @param service Handle object that is returned from
      * {@link IZatManager#connectGnssConfigServices()}.
      * @throws IZatIllegalArgumentException The input service is NULL.
      */
    public void disconnectGnssConfigServices(IZatGnssConfigServices service) {
        if (null == service || !(service instanceof IZatGnssConfigServicesImpl))
             throw new IZatIllegalArgumentException();

        try {
            IGnssConfigService gnssConfigService =
                (IGnssConfigService)(sIzatService.getGnssConfigService());
            if (null == gnssConfigService) {
                throw new IZatServiceUnavailableException("Gnss Config Service is not available.");
            }
            synchronized (sRLConfigCallbackMapLock) {
                mRLConfigCallback = null;
            }
            synchronized (sntnStatusCallbackLock) {
                mntnStatusCallback = null;
            }
            mGnssConfigServicesImpl = null;
        } catch (RemoteException e) {
            throw new RuntimeException("Failed to disconnect GnssConfigService", e);
        }
    }

    /**
     * Gets an instance of IZatGtpService.
     * This API may take several seconds to execute. Qualcomm Technologies,
     * Inc. recommends not calling this API in the main thread.
     *
     * Permission required: com.qualcomm.permission.USER_CONSENT_PERMISSION </p>
     * @throws IZatServiceUnavailableException The GTP service is not available.
     *
     * @return
     * An instance of the IZatGtpService interface.
     *
     * @dependencies
     * This API requires that the IZat GTP service is present, otherwise
     * an IZatServiceUnavailableException is thrown.
     */
    public IZatGtpService connectToGtpService()
            throws IZatServiceUnavailableException
    {

        if (null == sIzatService) {
            connectIzatService();
        }

        try {
            setFeatureMask(sIzatService.getFlpService());

            if ((mFlpFeatureMasks & FEATURE_BIT_GTP_LOCATION_IS_SUPPORTED) == 0) {
                throw new IZatFeatureNotSupportedException(
                        "GTP Positioning is not supported on this device.");
            }

            if ((mFlpFeatureMasks & FEATURE_BIT_QESDK_GTP_LOCATION_IS_SUPPORTED) != 0) {
                throw new IZatFeatureNotSupportedException(
                        "GTP Positioning is not supported on this device.");
            }

            IGTPService gtpService = sIzatService.getGTPService();

            if (null == gtpService) {
                throw new IZatServiceUnavailableException(
                        "GTP Service is not available.");
            }
            if (null == mGtpServiceImpl) {
                mGtpServiceImpl = new IZatGtpServiceImpl(gtpService);
            } else {
                mGtpServiceImpl.mService = gtpService;
            }
        } catch (RemoteException e) {
            throw new RuntimeException("Failed to get IGTPService", e);
        }

        Log.d(TAG, "connectToGtpService: connected to service");
        return mGtpServiceImpl;
    }

    /**
     * Disconnects from {@link IZatGtpService}. Removes any active or passive sessions.
     * {@link IZatManager#disconnectGtpService(IZatGtpService)} ()}.
     */
    public void disconnectGtpService(IZatGtpService gtpService) {

            if (null == gtpService) {
                throw new IZatIllegalArgumentException();
            }
            synchronized (sGtpServiceLock) {
                mActiveGtpCallback = null;
                mPassiveGtpCallback = null;
                mActiveGtpWwanCallback = null;
                mPassiveGtpWwanCallback = null;
                mActiveGtpTbf = -1;
                mActiveGtpWwanTbf = -1;
                mActiveGtpAccuracy = GTPAccuracy.NOMINAL;
                mActiveGtpWwanAccuracy = GTPAccuracy.NOMINAL;
                gtpService.removeLocationUpdates();
                gtpService.removePassiveLocationUpdates();
            }
    }

    /**
     *
     * Gets an {@link IZatWWANAdReceiver} interface.
     * <p>
     * This API requires that the WWAN AD Receiver is present, otherwise
     * an {@link IZatServiceUnavailableException} is thrown.<br/>
     * This API may take as long as several seconds to execute.
     * It is recommended not to call this API in the main thread.
     * </p>
     * Note that usage of this API implicitly means that the end user
     * accepts any license agreement required.
     * </p>
     *
     * Permission required: com.qualcomm.permission.IZAT </p>
     * @param listener Handle WWAN AD request from the {@link IZatWWANAdReceiver} service.
     *                 Can not be null, or a IZatIllegalArgumentException will be thrown.
     * @throws IZatServiceUnavailableException The WWANAdReceiver is not present.
     * @throws IZatIllegalArgumentException    One of the arguments is null.
     * @throws SecurityException           The SDK client is not a system App.
     *
     * @return an {@link IZatWWANAdReceiver} interface.
     */
    public IZatWWANAdReceiver connectToWWANAdReceiver(IZatWWANAdRequestListener listener)
            throws IZatServiceUnavailableException, IZatIllegalArgumentException {

        if (null == sIzatService) {
            connectIzatService();
        }
        if (null == listener) {
            throw new IZatIllegalArgumentException("Listener can not be null.");
        }

        try {
            IWWANAdReceiver wwanAdReceiver =
                    (IWWANAdReceiver)(sIzatService.getWWANAdReceiver());
            // register the callbacks
            synchronized (sWWANAdReceiverLock) {
                wwanAdReceiver.registerRequestListener(mWWANAdRequestListener);
            }
            if (null == mWWANAdReceiverImpl) {
                mWWANAdReceiverImpl = new IZatWWANAdReceiverImpl(wwanAdReceiver, listener);
            } else {
                mWWANAdReceiverImpl.mWWANAdReceiver = wwanAdReceiver;
                mWWANAdReceiverImpl.mReqListener = listener;
            }
            return mWWANAdReceiverImpl;
        } catch (RemoteException e) {
            throw new RuntimeException("Failed to get IWWANAdReceiver", e);
        }
    }

    /**
     * Disconnects the specified {@link IZatWWANAdReceiver}
     * interface.
     *
     * Permission required: com.qualcomm.permission.IZAT </p>
     * @param provider Handle object that is returned from
     * {@link IZatManager#connectToWWANAdReceiver()}.
     * @throws IZatIllegalArgumentException The input provider is NULL.
     *
     */
    public void disconnectFromWWANAdReceiver(IZatWWANAdReceiver receiver)
            throws IZatIllegalArgumentException {
        if (null == receiver || !(receiver instanceof IZatWWANAdReceiver))
            throw new IZatIllegalArgumentException();
        try {
            IWWANAdReceiver wwanAdReceiver =
                    (IWWANAdReceiver)(sIzatService.getWWANAdReceiver());
            // register the callbacks
            synchronized (sWWANAdReceiverLock) {
                wwanAdReceiver.removeResponseListener(mWWANAdRequestListener);
            }
            mWWANAdReceiverImpl = null;
        } catch (RemoteException e) {
            throw new RuntimeException("Failed to disconnect WWANAdReceiver", e);
        }
    }
    /**
     * Gets an IZatPrecisePositioningService interface.
     *
     * This API may take several seconds to execute.
     * Qualcomm Technologies, Inc. recommends not to call this API in the main thread.
     *
     * Permission required: com.qualcomm.permission.ACCESS_FINE_LOCATION </p>
     * @throws IZatServiceUnavailableException The precise positioning service is not present.
     * @throws SecurityException           The SDK client is not a system App.
     *
     * @return
     * An IZatPrecisePositioningService interface.
     *
     * @dependencies
     * This API requires that the IZat Precise Positioning service is present, otherwise
     * an IZatServiceUnavailableException is thrown.
     */
    public IZatPrecisePositioningService connectToPrecisePositioningService()
            throws IZatServiceUnavailableException {
        if (null == sIzatService) {
            connectIzatService();
        }
        try {
            setFeatureMask(sIzatService.getFlpService());
            if ((mFlpFeatureMasks & FEATURE_BIT_PRECISE_LOCATION_IS_SUPPORTED) == 0) {
                throw new IZatFeatureNotSupportedException(
                        "precise Positioning is not supported on this device.");
            }
            IPrecisePositionService ppService = sIzatService.getPrecisePositionService();
            if (null == ppService) {
                throw new IZatServiceUnavailableException(
                        "Precise Positioning Service is not available.");
            }
            if (null == mPrecisePositionServiceImpl) {
                mPrecisePositionServiceImpl = new IZatPrecisePositioningServiceImpl(ppService);
            } else {
                mPrecisePositionServiceImpl.mService = ppService;
            }

        } catch (RemoteException e) {
            throw new RuntimeException("Failed to get PrecisePositioningervice", e);
        }
        Log.d(TAG, "connectToPrecisePositioningService: connected to service");
        return mPrecisePositionServiceImpl;
    }
    /**
     * Disconnects form the specified IZatPrecisePositioningService interface.
     *
     * @param service Handle object that is returned from
     * the IZatManager#connectPrecisePositioningService().
     * @throws IZatIllegalArgumentException The input service is NULL.
     * @return
     * None.
     */
    public void disconnectPrecisePositioningService(IZatPrecisePositioningService ppService) {
        if (null == ppService) {
            throw new IZatIllegalArgumentException();
        }
        synchronized (sPrecisePositionServiceLock) {
            mPrecisePositionCallback = null;
            ppService.stopPrecisePositioningSession();
        }
    }

    private void restorePreciseSession() {
        try {
            synchronized (sPrecisePositionServiceLock) {
                mPrecisePositionServiceImpl.mService.startPrecisePositioningSession(
                        mPrecisePositionCbWrapper, mPrecisePositionRequest.mMinTimeInterval,
                        mPrecisePositionRequest.mPreciseType.getValue(),
                        mPrecisePositionRequest.mCorrectionType.getValue());
            }
        } catch (RemoteException e) {
            throw new RuntimeException("Failed start precise session", e);
        }
    }

    private void restoreFlpSessions() {
        try {
            synchronized (sFlpServiceLock) {
                for (FlpRequestMapItem mapItem: mFlpRequestsMap.values()) {
                    if ((mFlpFeatureMasks & FEATURE_BIT_DISTANCE_BASED_BATCHING_IS_SUPPORTED)>0) {
                        if (mapItem.getNotifyType() ==
                                NotificationType.NOTIFICATION_WHEN_TRIP_IS_COMPLETED ||
                            mapItem.getNotifyType() ==
                                NotificationType.NOTIFICATION_WHEN_BUFFER_IS_FULL) {
                            mFlpServiceImpl.mService.registerCallback(FLP_SEESION_BACKGROUND,
                                    mapItem.mHwId,
                                    mapItem.mCbWrapper,
                                    mapItem.getSessionStartTime());
                        } else {
                            mFlpServiceImpl.mService.registerCallback(FLP_SEESION_FOREGROUND,
                                    mapItem.mHwId,
                                    mapItem.mCbWrapper,
                                    mapItem.getSessionStartTime());
                        }
                    } else {
                        mFlpServiceImpl.mService.registerCallback(FLP_SEESION_PASSIVE,
                                mapItem.mHwId,
                                mapItem.mCbWrapper,
                                mapItem.getSessionStartTime());
                    }
                    mFlpServiceImpl.mService.startFlpSessionWithPower(mapItem.mHwId,
                            mapItem.getNotifyType().getCode(),
                            mapItem.getTimeInterval(),
                            mapItem.getDistanceInterval(),
                            mapItem.getTripDistance(),
                            mapItem.getPowerMode(),
                            mapItem.getTbmMillis());
                }
            }
        } catch (RemoteException e) {
            throw new RuntimeException("Failed startFlpSession", e);
        }
    }

    private void restoreGeofenceSessions() {
        try {
            for (GeofenceMapItem mapItem: mGeofencesMap.values()) {
                IZatGeofenceService.IzatGeofence geofence = mapItem.getGeofence();
                Object context = mapItem.getContext();
                double latitude = geofence.getLatitude();
                double longitude = geofence.getLongitude();
                double radius = geofence.getRadius();
                int transitionType = geofence.getTransitionTypes().getValue();
                int responsiveness = geofence.getNotifyResponsiveness();
                int confidence = geofence.getConfidence().getValue();

                int dwellTime = 0;
                int dwellType = 0;
                if (geofence.getDwellNotify() != null) {
                    dwellTime = geofence.getDwellNotify().getDwellTime();
                    dwellType = geofence.getDwellNotify().getDwellType();
                }

                int geofenceId;
                synchronized (sGeofenceServiceLock) {
                    if ((context != null) &&
                            ((context instanceof String) || (context instanceof Bundle))) {

                        String appTextData =
                                ((context instanceof String) ? context.toString() : null);
                        Bundle bundleObj =
                                ((context instanceof Bundle) ? (Bundle) context : null);

                        geofenceId = mGeofenceServiceImpl.mService.addGeofenceObj(
                                new GeofenceData(responsiveness,
                                                 latitude,
                                                 longitude,
                                                 radius,
                                                 transitionType,
                                                 confidence,
                                                 dwellType,
                                                 dwellTime,
                                                 appTextData,
                                                 bundleObj,
                                                 -1));
                    } else {
                        geofenceId = mGeofenceServiceImpl.mService.addGeofence(latitude,
                                                                               longitude,
                                                                               radius,
                                                                               transitionType,
                                                                               responsiveness,
                                                                               confidence,
                                                                               dwellTime,
                                                                               dwellType);
                    }
                    if (mapItem.mIsPaused) {
                        mGeofenceServiceImpl.mService.pauseGeofence(geofenceId);
                    }
                }
                synchronized (sGeofencesMapLock) {
                    mapItem.setHWGeofenceId(geofenceId);
                }
            }
        } catch (RemoteException e) {
            throw new RuntimeException("Failed add geofence", e);
        }
        //Restore the pending intent if has
        if (mGeofenceIntent != null) {
            try {
                synchronized (sGeofenceServiceLock) {
                    mGeofenceServiceImpl.mService.registerPendingIntent(mGeofenceIntent);
                }
            } catch (RemoteException e) {
                throw new RuntimeException("Failed add geofence", e);
            }
        }
    }

    private void restorePassiveListeners() {
        try {
            synchronized (sFlpServiceLock) {
                for (LocationCallbackWrapper CbItem: mFlpPassiveCbMap.values()) {
                    mFlpServiceImpl.mService.registerCallback(FLP_SEESION_PASSIVE,
                                                              FLP_PASSIVE_LISTENER_HW_ID,
                                                              CbItem,
                                                              System.currentTimeMillis());
                }
            }
        } catch (RemoteException e) {
            throw new RuntimeException("Failed registerForPassiveLocations", e);
        }
    }

    private void restoreMaxPowerAllocatedCbs() {
        try {
            synchronized (sTestServiceLock) {
                for (MaxPowerAllocatedCallbackWrapper CbItem: mFlpMaxPowerCbMap.values()) {
                    mTestServiceImpl.mService.registerMaxPowerChangeCallback(CbItem);
                }
            }
        } catch (RemoteException e) {
            throw new RuntimeException("Failed registerForMaxPowerAllocatedChange", e);
        }
    }

    private void restoreDebugReportService() {
        if (!mDebugReportClientCallbackMap.isEmpty()) {
            try {
                mDebugReportingServiceImpl.mService.startReporting();
            } catch (RemoteException e) {
                throw new RuntimeException("Failed to register for debug reports");
            }
        }
    }

    private void restoreGtpSessions() {
        synchronized (sGtpServiceLock) {
            if (null != mActiveGtpCallback && mActiveGtpTbf > -1) {
                Log.d(TAG, "restoreGtpSessions: restoring active session");
                try {
                    mGtpServiceImpl.mService.removeLocationUpdates();
                    mGtpServiceImpl.mService.requestLocationUpdates(
                            new GtpServiceCallbackWrapper(mActiveGtpCallback),
                            new GtpRequestData(mActiveGtpTbf, mActiveGtpAccuracy));
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            }
            if (null != mPassiveGtpCallback) {
                Log.d(TAG, "restoreGtpSessions: restoring passive session");
                try {
                    mGtpServiceImpl.mService.removePassiveLocationUpdates();
                    mGtpServiceImpl.mService.requestPassiveLocationUpdates(
                            new GtpServiceCallbackWrapper(mPassiveGtpCallback));
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            }
            if (null != mActiveGtpWwanCallback && mActiveGtpWwanTbf > -1) {
                Log.d(TAG, "restoreGtpSessions: restoring active Wwan session");
                try {
                    mGtpServiceImpl.mService.removeWwanLocationUpdates();
                    mGtpServiceImpl.mService.requestWwanLocationUpdates(
                            new GtpServiceCallbackWrapper(mActiveGtpWwanCallback),
                            new GtpRequestData(mActiveGtpWwanTbf, mActiveGtpWwanAccuracy));
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            }
            if (null != mPassiveGtpWwanCallback) {
                Log.d(TAG, "restoreGtpSessions: restoring passive session");
                try {
                    mGtpServiceImpl.mService.removePassiveWwanLocationUpdates();
                    mGtpServiceImpl.mService.requestPassiveWwanLocationUpdates(
                            new GtpServiceCallbackWrapper(mPassiveGtpWwanCallback));
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

/** @cond */

    /*
     * Implementation of interface IZatFlpService. (FOR INTERNAL USE ONLY)
     *
     * @hide
     */
    private class IZatFlpServiceImpl implements IZatFlpService {

        IFlpService mService;
        public IZatFlpServiceImpl(IFlpService service) {
            mService = service;
        }

        @Override
        public IZatFlpSessionHandle startFlpSession(IFlpLocationCallback callback,
                                                    IzatFlpRequest flpRequest)
                                                    throws IZatIllegalArgumentException {
            if (null == callback || null == flpRequest) {
                throw new IZatIllegalArgumentException("invalid input parameter");
            }
            if ((flpRequest.getTimeInterval() <= 0) &&
                (flpRequest.getDistanceInterval() <= 0) &&
                (flpRequest.getTripDistance() <= 0)) {
                throw new IZatIllegalArgumentException("Atleast one of the parameters " +
                        "in time, distance interval and trip distance must be valid");
            }
            IZatDataValidation.dataValidate(flpRequest);
            try {
                synchronized (sFlpServiceLock) {
                    // first to check if this request is already started.
                    NotificationType notifType =
                            NotificationType.NOTIFICATION_ON_EVERY_LOCATION_FIX;
                    if (flpRequest.mIsRunningInBackground) {

                        notifType = NotificationType.NOTIFICATION_WHEN_BUFFER_IS_FULL;

                        if (flpRequest instanceof IzatFlpBgRequest) {
                            // get the active mode
                            IzatFlpBgRequest.IzatFlpBgRequestMode activeMode =
                                    ((IzatFlpBgRequest)flpRequest).getActiveMode();

                            if (IzatFlpBgRequest.IzatFlpBgRequestMode.TRIP_MODE == activeMode) {
                                if (0 == (mFlpFeatureMasks &
                                          FEATURE_BIT_OUTDOOR_TRIP_BATCHING_IS_SUPPORTED)) {
                                    throw new IZatFeatureNotSupportedException (
                                            "Outdoor trip mode is not supported.");
                                } else if (flpRequest.getTripDistance() > 0) {
                                    notifType =
                                            NotificationType.NOTIFICATION_WHEN_TRIP_IS_COMPLETED;
                                }
                            }
                        }
                    }

                    for (FlpRequestMapItem mapItem: mFlpRequestsMap.values()) {
                        if ((mapItem.getCallback() == callback) &&
                            (mapItem.getNotifyType() == notifType) &&
                            (mapItem.getTimeInterval() == flpRequest.getTimeInterval()) &&
                            (mapItem.getDistanceInterval() == flpRequest.getDistanceInterval()) &&
                                    (mapItem.getTripDistance() == flpRequest.getTripDistance())){
                            throw new IZatIllegalArgumentException("this session" +
                                                                   " started already.");
                        }
                    }
                    int hwId = sFlpRequestsCnt++;
                    long sessionStartTime = System.currentTimeMillis();
                    // register the cb
                    LocationCallbackWrapper cbWrapper = new LocationCallbackWrapper(callback);

                    if ((mFlpFeatureMasks & FEATURE_BIT_DISTANCE_BASED_BATCHING_IS_SUPPORTED)>0) {
                        /* If the modem support LB2.0, then the batched fixes and live fix will be
                          returned independently to clients based on the request issued by
                          each client. */
                        if (flpRequest.mIsRunningInBackground) {
                            mService.registerCallback(FLP_SEESION_BACKGROUND,
                                                      hwId,
                                                      cbWrapper,
                                                      sessionStartTime);
                        } else {
                            mService.registerCallback(FLP_SEESION_FOREGROUND,
                                                      hwId,
                                                      cbWrapper,
                                                      sessionStartTime);
                        }
                    } else {
                            /* If the modem does not support LB2.0, then the batching client
                               will also receive the live fix, which is the legacy behaviour.*/
                            mService.registerCallback(FLP_SEESION_PASSIVE,
                                                      hwId,
                                                      cbWrapper,
                                                      sessionStartTime);
                    }
                    // start flp session
                    int result = mService.startFlpSessionWithPower(hwId,
                                    notifType.getCode(),
                                    flpRequest.getTimeInterval(),
                                    flpRequest.getDistanceInterval(),
                                    flpRequest.getTripDistance(),
                                    flpRequest.getPowerMode(),
                                    flpRequest.getTbmMillis());
                    if (VERBOSE) {
                        Log.v(TAG, "startFlpSession() returning : " + result);
                    }
                    if (FLP_RESULT_SUCCESS == result) {
                        IZatSessionHandlerImpl handle = new IZatSessionHandlerImpl();
                        mFlpRequestsMap.put(handle,
                                new FlpRequestMapItem(callback,
                                                      notifType,
                                                      flpRequest.getTimeInterval(),
                                                      flpRequest.getDistanceInterval(),
                                                      flpRequest.getTripDistance(),
                                                      cbWrapper,
                                                      hwId,
                                                      sessionStartTime,
                                                      flpRequest.getPowerMode(),
                                                      flpRequest.getTbmMillis()));
                        return handle;
                    } else {
                        if (0 < (mFlpFeatureMasks &
                                 FEATURE_BIT_DISTANCE_BASED_BATCHING_IS_SUPPORTED)) {
                            if (flpRequest.mIsRunningInBackground) {
                                mService.unregisterCallback(FLP_SEESION_BACKGROUND,
                                                            cbWrapper);
                            } else {
                                mService.unregisterCallback(FLP_SEESION_FOREGROUND,
                                                            cbWrapper);
                            }
                        } else {
                                mService.unregisterCallback(FLP_SEESION_PASSIVE,
                                                            cbWrapper);
                        }
                        sFlpRequestsCnt--;
                        return null;
                    }
                }
            } catch (RemoteException e) {
                throw new RuntimeException("Failed startFlpSession", e);
            }
        }

        @Override
        public void stopFlpSession(IZatFlpSessionHandle handler)
            throws IZatIllegalArgumentException {

            if (null == handler || !(handler instanceof IZatFlpServiceImpl.IZatSessionHandlerImpl)) {
                throw new IZatIllegalArgumentException("invalid input parameter");
            }
            try {
                synchronized (sFlpServiceLock) {
                    // first to check if this request is already started.
                    FlpRequestMapItem mapItem = mFlpRequestsMap.get(handler);
                    if (null == mapItem) {
                        Log.e(TAG, "this request ID is unknown.");
                        return;
                    }
                    if (mService.stopFlpSession(mapItem.getHwId()) != FLP_RESULT_SUCCESS){
                        Log.e(TAG, "stopFlpSession() failed. ");
                        return;
                    }
                    if ((mFlpFeatureMasks & FEATURE_BIT_DISTANCE_BASED_BATCHING_IS_SUPPORTED)>0) {
                        if ((NotificationType.NOTIFICATION_WHEN_BUFFER_IS_FULL ==
                                mapItem.getNotifyType()) ||
                            (NotificationType.NOTIFICATION_WHEN_TRIP_IS_COMPLETED ==
                                    mapItem.getNotifyType())) {
                            mService.unregisterCallback(FLP_SEESION_BACKGROUND,
                                                        mapItem.getCbWrapper());
                        } else if (NotificationType.NOTIFICATION_ON_EVERY_LOCATION_FIX == mapItem.getNotifyType()) {
                            mService.unregisterCallback(FLP_SEESION_FOREGROUND,
                                                        mapItem.getCbWrapper());
                        }
                    } else {
                        mService.unregisterCallback(FLP_SEESION_PASSIVE,
                                                    mapItem.getCbWrapper());
                    }
                    mFlpRequestsMap.remove(handler);
                }
            } catch (RemoteException e) {
                throw new RuntimeException("Failed stopFlpSession", e);
            }
        }

        @Override
        public void registerForPassiveLocations(IZatFlpService.IFlpLocationCallback callback)
            throws IZatIllegalArgumentException {
            if (null == callback) {
                throw new IZatIllegalArgumentException("invalid input parameter");
            }
            try {
                synchronized (sFlpServiceLock) {
                    if (null == mFlpPassiveCbMap.get(callback)) {
                        LocationCallbackWrapper cbWrapper = new LocationCallbackWrapper(callback);
                        mFlpPassiveCbMap.put(callback, cbWrapper);
                        mService.registerCallback(FLP_SEESION_PASSIVE,
                                                  FLP_PASSIVE_LISTENER_HW_ID,
                                                  cbWrapper,
                                                  System.currentTimeMillis());
                    } else {
                        Log.w(TAG, "this passive callback is already registered.");
                    }
                }
            } catch (RemoteException e) {
                throw new RuntimeException("Failed registerForPassiveLocations", e);
            }
        }

        @Override
        public void deregisterForPassiveLocations(IZatFlpService.IFlpLocationCallback callback)
            throws IZatIllegalArgumentException {
            if (null == callback) {
                throw new IZatIllegalArgumentException("invalid input parameter");
            }
            try {
                synchronized (sFlpServiceLock) {
                    LocationCallbackWrapper cbWrapper = mFlpPassiveCbMap.get(callback);
                    if (null == cbWrapper) {
                        Log.w(TAG, "this passive callback is not registered.");
                    } else {
                        mService.unregisterCallback(FLP_SEESION_PASSIVE,
                                                    cbWrapper);
                        mFlpPassiveCbMap.remove(callback);
                    }
                }
            } catch (RemoteException e) {
                throw new RuntimeException("Failed deregisterForPassiveLocations", e);
            }
        }

        private class IZatSessionHandlerImpl implements IZatFlpService.IZatFlpSessionHandle {
            @Override
            public void pullLocations() {

                try {
                    synchronized (sFlpServiceLock) {
                        FlpRequestMapItem mapItem = mFlpRequestsMap.get(this);
                        if (null == mapItem) {
                            Log.w(TAG, "no flp session undergoing");
                            return;
                        }
                        if (null == mapItem.getCbWrapper()) {
                            Log.w(TAG, "no available callback");
                            return;
                        }
                        int result = mService.pullLocations(mapItem.getCbWrapper(),
                                                            mapItem.getSessionStartTime(),
                                                            mapItem.getHwId());
                        if (FLP_RESULT_SUCCESS == result) {
                            mapItem.setSessionStartTime(System.currentTimeMillis());
                        }
                        if (VERBOSE) {
                            Log.v(TAG, "pullLocations() returning : " + result);
                        }
                    }
                } catch (RemoteException e) {
                    throw new RuntimeException("Failed pullLocations", e);
                }
            }

            @Override
            public void setToForeground() {
                try {
                    synchronized (sFlpServiceLock) {
                        FlpRequestMapItem mapItem = mFlpRequestsMap.get(this);
                        if (null == mapItem) {
                            Log.w(TAG, "no flp session undergoing");
                            return;
                        }
                        if (null == mapItem.getCbWrapper()) {
                            Log.w(TAG, "no available callback");
                            return;
                        }
                        int result = FLP_RESULT_SUCCESS;
                        if (NotificationType.NOTIFICATION_ON_EVERY_LOCATION_FIX !=
                                mapItem.getNotifyType()) {
                            result = mService.updateFlpSessionWithPower(mapItem.getHwId(),
                                NotificationType.NOTIFICATION_ON_EVERY_LOCATION_FIX.getCode(),
                                mapItem.getTimeInterval(),
                                mapItem.getDistanceInterval(), 0,
                                mapItem.getPowerMode(), mapItem.getTbmMillis());
                            if (FLP_RESULT_SUCCESS == result) {
                                // update the item flag in map
                                mapItem.updateNotifyType(
                                    NotificationType.NOTIFICATION_ON_EVERY_LOCATION_FIX);
                                mFlpRequestsMap.put(this, mapItem);
                                // move the callback form bg to fg
                                if ((mFlpFeatureMasks &
                                    FEATURE_BIT_DISTANCE_BASED_BATCHING_IS_SUPPORTED)>0) {
                                    mService.unregisterCallback(FLP_SEESION_BACKGROUND,
                                                                mapItem.getCbWrapper());
                                    mService.registerCallback(FLP_SEESION_FOREGROUND,
                                                              mapItem.getHwId(),
                                                              mapItem.getCbWrapper(),
                                                              mapItem.getSessionStartTime());
                                }
                            } else {
                                Log.v(TAG, "mService.updateFlpSession failed.");
                            }
                        }
                        if (VERBOSE)  {
                            Log.v(TAG, "setToForeground() returning : " + result);
                        }
                    }
                } catch (RemoteException e) {
                    throw new RuntimeException("Failed setToForeground", e);
                }
            }

            @Override
            public void setToBackground() {
                try {
                    synchronized (sFlpServiceLock) {
                        FlpRequestMapItem mapItem = mFlpRequestsMap.get(this);
                        if (null == mapItem) {
                            Log.w(TAG, "no flp session undergoing");
                            return;
                        }
                        if (null == mapItem.getCbWrapper()) {
                            Log.w(TAG, "no available callback");
                            return;
                        }
                        int result = FLP_RESULT_SUCCESS;
                        if (NotificationType.NOTIFICATION_WHEN_BUFFER_IS_FULL !=
                                mapItem.getNotifyType()) {
                            result = mService.updateFlpSessionWithPower(mapItem.getHwId(),
                                NotificationType.NOTIFICATION_WHEN_BUFFER_IS_FULL.getCode(),
                                mapItem.getTimeInterval(),
                                mapItem.getDistanceInterval(), 0,
                                mapItem.getPowerMode(), mapItem.getTbmMillis());
                            if (FLP_RESULT_SUCCESS == result) {
                                // update the item flag in map
                                mapItem.updateNotifyType(
                                    NotificationType.NOTIFICATION_WHEN_BUFFER_IS_FULL);
                                mFlpRequestsMap.put(this, mapItem);
                                if ((mFlpFeatureMasks &
                                    FEATURE_BIT_DISTANCE_BASED_BATCHING_IS_SUPPORTED)>0) {
                                    // move the callback form fg to bg
                                    mService.unregisterCallback(FLP_SEESION_FOREGROUND,
                                                                mapItem.getCbWrapper());
                                    mService.registerCallback(FLP_SEESION_BACKGROUND,
                                                              mapItem.getHwId(),
                                                              mapItem.getCbWrapper(),
                                                              mapItem.getSessionStartTime());
                                }
                            } else {
                                Log.v(TAG, "mService.updateFlpSession failed.");
                            }
                        }
                        if (VERBOSE) {
                            Log.v(TAG, "setToBackground() returning : " + result);
                        }
                    }
                } catch (RemoteException e) {
                    throw new RuntimeException("Failed setToBackground", e);
                }
            }

            @Override
            public void setToTripMode() throws
                    IZatFeatureNotSupportedException {
                if (0 == (mFlpFeatureMasks & FEATURE_BIT_OUTDOOR_TRIP_BATCHING_IS_SUPPORTED)) {
                    throw new IZatFeatureNotSupportedException (
                            "Outdoor trip mode is not supported.");
                }

                try {
                    synchronized (sFlpServiceLock) {
                        FlpRequestMapItem mapItem = mFlpRequestsMap.get(this);
                        if (null == mapItem) {
                            Log.w(TAG, "no flp session undergoing");
                            return;
                        }
                        if (null == mapItem.getCbWrapper()) {
                            Log.w(TAG, "no available callback");
                            return;
                        }

                        if (0 == (mFlpFeatureMasks &
                                  FEATURE_BIT_OUTDOOR_TRIP_BATCHING_IS_SUPPORTED)) {
                            Log.w(TAG, "Outdoor Trip mode not supported");
                            return;
                        }

                        int result = FLP_RESULT_SUCCESS;
                        if (NotificationType.NOTIFICATION_WHEN_TRIP_IS_COMPLETED !=
                                mapItem.getNotifyType()) {
                            result = mService.updateFlpSessionWithPower(mapItem.getHwId(),
                                    NotificationType.NOTIFICATION_WHEN_TRIP_IS_COMPLETED.getCode(),
                                    mapItem.getTimeInterval(),
                                    mapItem.getDistanceInterval(), mapItem.getTripDistance(),
                                    mapItem.getPowerMode(), mapItem.getTbmMillis());
                            if (FLP_RESULT_SUCCESS == result) {
                                // update the item flag in map
                                mapItem.updateNotifyType(
                                        NotificationType.NOTIFICATION_WHEN_TRIP_IS_COMPLETED);

                                // remember to switch back to previous state once
                                // trip is completed.
                                mapItem.setRestartOnTripCompleted(true);
                                mFlpRequestsMap.put(this, mapItem);
                                if ((mFlpFeatureMasks &
                                    FEATURE_BIT_DISTANCE_BASED_BATCHING_IS_SUPPORTED)>0) {
                                    if (NotificationType.NOTIFICATION_ON_EVERY_LOCATION_FIX ==
                                            mapItem.getPreviousNotifyType()) {
                                        // switching from foreground to trip mode
                                        mService.unregisterCallback(FLP_SEESION_FOREGROUND,
                                                                    mapItem.getCbWrapper());
                                        mService.registerCallback(FLP_SEESION_BACKGROUND,
                                                                  mapItem.getHwId(),
                                                                  mapItem.getCbWrapper(),
                                                                  mapItem.getSessionStartTime());
                                    }
                                }
                            } else {
                                Log.v(TAG, "mService.updateFlpSession failed.");
                            }
                        }
                        if (VERBOSE)  {
                            Log.v(TAG, "setToTripMode() returning : " + result);
                        }
                    }
                } catch (RemoteException e) {
                    throw new RuntimeException("Failed setToTripMode", e);
                }
            }

            @Override
            public void registerForSessionStatus(IFlpStatusCallback callback)
                    throws IZatIllegalArgumentException, IZatFeatureNotSupportedException {
                if (0 == (mFlpFeatureMasks & FEATURE_BIT_OUTDOOR_TRIP_BATCHING_IS_SUPPORTED)) {
                    throw new IZatFeatureNotSupportedException (
                            "Session status not supported.");
                }

                if (null == callback) {
                    throw new IZatIllegalArgumentException("invalid input parameter");
                }

                try {
                    synchronized (sFlpServiceLock) {
                        FlpRequestMapItem mapItem = mFlpRequestsMap.get(this);
                        if (null == mapItem) {
                            Log.w(TAG, "no flp session undergoing");
                            return;
                        }

                        FlpStatusCallbackWrapper cbWrapper =
                            new FlpStatusCallbackWrapper(callback, mService);
                        mapItem.setStatusCallback(cbWrapper);
                        mService.registerForSessionStatus(mapItem.getHwId(), cbWrapper);

                    }
                } catch (RemoteException e) {
                    throw new RuntimeException("Failed registerForSessionStatus", e);
                }
            }

            @Override
            public void unregisterForSessionStatus()
                    throws  IZatIllegalArgumentException, IZatFeatureNotSupportedException {

                    if (0 == (mFlpFeatureMasks & FEATURE_BIT_OUTDOOR_TRIP_BATCHING_IS_SUPPORTED)) {
                    throw new IZatFeatureNotSupportedException (
                            "Session status not supported.");
                }

                try {
                    synchronized (sFlpServiceLock) {
                        FlpRequestMapItem mapItem = mFlpRequestsMap.get(this);
                        if (null == mapItem) {
                            Log.w(TAG, "no flp session undergoing");
                            return;
                        }

                        FlpStatusCallbackWrapper cbWrapper = mapItem.getStatusCallback();
                        if (null == cbWrapper) {
                            Log.w(TAG, "no status callback wrapper is registered.");
                        } else {
                            mService.unregisterForSessionStatus(cbWrapper);
                            mapItem.setStatusCallback(null);
                        }
                    }
                } catch (RemoteException e) {
                    throw new RuntimeException("Failed unregisterForSessionStatus", e);
                }
        }
    }
  }

    /*
     * Implementation of interface IZatPrecisePositioningService.
     *
     */
    private class IZatPrecisePositioningServiceImpl implements IZatPrecisePositioningService {

        IPrecisePositionService mService;
        private boolean mIsSessionStarted = false;
        public IZatPrecisePositioningServiceImpl(IPrecisePositionService service) {
            mService = service;
            mIsSessionStarted = false;
        }

        @Override
        public void startPrecisePositioningSession(IZatPrecisePositioningCallback callback,
                                                   IZatPrecisePositioningRequest pPRequest)
                                                   throws IZatIllegalArgumentException {
            if (null == callback || null == pPRequest) {
                throw new IZatIllegalArgumentException("invalid input parameter");
            }
            if (false == mIsSessionStarted) {
                if (pPRequest.getTimeInterval() <= 0) {
                    throw new IZatIllegalArgumentException("time interval invalid");
                }
                try {
                    synchronized (sPrecisePositionServiceLock) {
                        mPrecisePositionCallback = callback;
                        mPrecisePositionCbWrapper =
                                new IZatPrecisePositioningCallbackWrapper(mPrecisePositionCallback);
                        mPrecisePositionRequest = pPRequest;
                        mService.startPrecisePositioningSession(
                            mPrecisePositionCbWrapper,
                            mPrecisePositionRequest.mMinTimeInterval,
                            mPrecisePositionRequest.mPreciseType.getValue(),
                            mPrecisePositionRequest.mCorrectionType.getValue());
                        mIsSessionStarted = true;
                    }
                } catch (RemoteException e) {
                    throw new RuntimeException("Failed startPrecisePositioningSession", e);
                }
            } else {
                Log.w(TAG, "precise session is already started");
                if (null != callback) {
                    callback.onResponseCallback(
                        IZatLocationResponse.LOCATION_RESPONSE_ANOTHER_PRECISE_SESSION_RUNNING);
                }
            }
        }

        public void stopPrecisePositioningSession() {
            try {
                synchronized (sPrecisePositionServiceLock) {
                    mPrecisePositionCallback = null;
                    mService.stopPrecisePositioningSession();
                    mIsSessionStarted = false;
            }
            } catch (RemoteException e) {
                throw new RuntimeException("Failed stopPrecisePositioningSession", e);
            }
        }
    }

    /**
     * Implementation of interface IZatTestService. (FOR INTERNAL USE ONLY)
     *
     * @hide
     */
    private class IZatTestServiceImpl implements IZatTestService {

        ITestService mService;
        public IZatTestServiceImpl(ITestService service) {
            mService = service;
        }

        @Override
        public void deleteAidingData(long flags)
            throws IZatIllegalArgumentException {
            if (0 == flags) {
                throw new IZatIllegalArgumentException("invalid input parameter." +
                                                       " flags must be filled");
            }
            try {
                mService.deleteAidingData(flags);
            } catch (RemoteException e) {
                throw new RuntimeException("Failed deregisterForPassiveLocations", e);
            }
        }

        @Override
        public void updateXtraThrottle(boolean enabled) {
            try {
                mService.updateXtraThrottle(enabled);
            } catch (RemoteException e) {
                throw new RuntimeException("Failed updateXtraThrottle", e);
            }
        }

        @Override
        public void registerForMaxPowerAllocatedChange(
            IZatTestService.IFlpMaxPowerAllocatedCallback callback)
                throws IZatIllegalArgumentException {

            if (null == callback) {
                throw new IZatIllegalArgumentException("invalid input parameter");
            }
            try {
                synchronized (sTestServiceLock) {
                    if (null == mFlpMaxPowerCbMap.get(callback)) {
                        MaxPowerAllocatedCallbackWrapper cbWrapper =
                                    new MaxPowerAllocatedCallbackWrapper(callback);
                        mFlpMaxPowerCbMap.put(callback, cbWrapper);
                        mService.registerMaxPowerChangeCallback(cbWrapper);
                    } else {
                        Log.w(TAG, "this max power callback is already registered.");
                    }
                }
            } catch (RemoteException e) {
                throw new RuntimeException("Failed registerForMaxPowerAllocatedChange", e);
            }
        }

        @Override
        public void deregisterForMaxPowerAllocatedChange(
            IZatTestService.IFlpMaxPowerAllocatedCallback callback)
                throws IZatIllegalArgumentException {

            if (null == callback) {
                throw new IZatIllegalArgumentException("invalid input parameter");
            }
            try {
                synchronized (sTestServiceLock) {
                    MaxPowerAllocatedCallbackWrapper cbWrapper = mFlpMaxPowerCbMap.get(callback);
                    if (null == cbWrapper) {
                        Log.w(TAG, "this passive callback is not registered.");
                    } else {
                        mService.unregisterMaxPowerChangeCallback(cbWrapper);
                        mFlpMaxPowerCbMap.remove(callback);
                    }
                }
            } catch (RemoteException e) {
                throw new RuntimeException("Failed deregisterForMaxPowerAllocatedChange", e);
            }
        }
    }

    private class IZatWiFiDBReceiverImpl extends IZatWiFiDBReceiver {
        IWiFiDBReceiver mReceiver;
        PendingIntent mWiFiReceiverIntent;
        public IZatWiFiDBReceiverImpl(IWiFiDBReceiver receiver,
                                      Object listener)
            throws IZatIllegalArgumentException {
            super(listener);
            if(null == receiver || null == listener) {
                throw new IZatIllegalArgumentException("IZatWiFiDBReceiverImpl:" +
                                                       " null receiver / listener");
            }
            mReceiver = receiver;
        }

        @Override
        public void requestAPList(int expire_in_days) {
            try {
                IZatDataValidation.dataValidate(expire_in_days, IZatDataTypes.WIFI_EXPIRE_DAYS);
                mReceiver.requestAPList(expire_in_days);
            } catch (RemoteException e) {
                throw new RuntimeException("Failed to request AP LIst", e);
            }
        }

        @Override
        public void requestScanList() {
            try {
                mReceiver.requestScanList();
            } catch (RemoteException e) {
                throw new RuntimeException("Failed to request AP LIst", e);
            }
        }

        @Override
        public void pushWiFiDB(List<IZatAPLocationData> izatLocationDataList,
                               List<IZatAPSpecialInfo> izatSpecialInfoList,
                               int days_valid) {

            if (IZatWiFiDBReceiver.MAXIMUM_INJECTION_ELEMENTS < izatLocationDataList.size()) {
                throw new RuntimeException(
                        "Exceeded maximum number of APs in izatLocationDataList: " +
                         IZatWiFiDBReceiver.MAXIMUM_INJECTION_ELEMENTS);
            }

            if (IZatWiFiDBReceiver.MAXIMUM_INJECTION_ELEMENTS < izatSpecialInfoList.size()) {
                throw new RuntimeException(
                        "Exceeded maximum number of APs in izatSpecialInfoList: " +
                        IZatWiFiDBReceiver.MAXIMUM_INJECTION_ELEMENTS);
            }

            // Per IZatWWANDBReceiver documentation:
            // Defaults to 15 days if 0 is provided.
            if (0 == days_valid) {
                Log.i(TAG, "days valid set to 0, use default of 15");
                days_valid = 15;
            }

            List<APLocationData> apLocationDataList = new ArrayList<APLocationData>();
            List<APSpecialInfo> apSpecialInfoList = new ArrayList<APSpecialInfo>();

            for(IZatAPLocationData izatLocationData: izatLocationDataList) {
                if(null != izatLocationData) {
                    IZatDataValidation.dataValidate(izatLocationData);
                    APLocationData apLocationData = new APLocationData();
                    apLocationData.mMacAddress = izatLocationData.getMacAddress();
                    apLocationData.mLatitude = izatLocationData.getLatitude();
                    apLocationData.mLongitude = izatLocationData.getLongitude();
                    apLocationData.mValidBits = apLocationData.AP_LOC_WITH_LAT_LON;
                    try{
                        apLocationData.mMaxAntenaRange = izatLocationData.getMaxAntenaRange();
                        apLocationData.mValidBits |= apLocationData.AP_LOC_MAR_VALID;
                    }catch (IZatStaleDataException e){
                        Log.e(TAG, "MAR exception ");
                        //do nothing
                    }

                    try{
                        apLocationData.mHorizontalError = izatLocationData.getHorizontalError();
                        apLocationData.mValidBits |= apLocationData.AP_LOC_HORIZONTAL_ERR_VALID;
                    }catch (IZatStaleDataException e){
                        Log.e(TAG, "HE exception ");
                        //do nothing
                    }

                    try{
                        apLocationData.mReliability = izatLocationData.getReliability().ordinal();
                        apLocationData.mValidBits |= apLocationData.AP_LOC_RELIABILITY_VALID;
                    }catch (IZatStaleDataException e){
                        Log.e(TAG, "REL exception ");
                        //do nothing
                    }

                    apLocationDataList.add(apLocationData);
                }
            }

            for(IZatAPSpecialInfo spl_info: izatSpecialInfoList) {
                if(null != spl_info) {
                    IZatDataValidation.dataValidate(spl_info);
                    APSpecialInfo info = new APSpecialInfo();
                    info.mMacAddress = spl_info.mMacAddress;
                    info.mInfo = spl_info.mInfo.ordinal();
                    apSpecialInfoList.add(info);
                }
            }

            try {
                mReceiver.pushWiFiDB(apLocationDataList, apSpecialInfoList, days_valid);
            } catch (RemoteException e) {
                throw new RuntimeException("Failed to push WiFi DB", e);
            }
        }

        @Override
        public void pushLookupResult(List<IZatAPLocationData> izatLocationDataList,
                                     List<IZatAPSpecialInfo> izatSpecialInfoList) {
            List<APLocationData> apLocationDataList = new ArrayList<APLocationData>();
            List<APSpecialInfo> apSpecialInfoList = new ArrayList<APSpecialInfo>();

            for(IZatAPLocationData izatLocationData: izatLocationDataList) {
                if(null != izatLocationData) {
                    IZatDataValidation.dataValidate(izatLocationData);
                    APLocationData apLocationData = new APLocationData();
                    apLocationData.mMacAddress = izatLocationData.getMacAddress();
                    apLocationData.mLatitude = izatLocationData.getLatitude();
                    apLocationData.mLongitude = izatLocationData.getLongitude();
                    apLocationData.mValidBits = apLocationData.AP_LOC_WITH_LAT_LON;
                    try{
                        apLocationData.mMaxAntenaRange = izatLocationData.getMaxAntenaRange();
                        apLocationData.mValidBits |= apLocationData.AP_LOC_MAR_VALID;
                    }catch (IZatStaleDataException e){
                        Log.e(TAG, "MAR exception ");
                        //do nothing
                    }

                    try{
                        apLocationData.mHorizontalError = izatLocationData.getHorizontalError();
                        apLocationData.mValidBits |= apLocationData.AP_LOC_HORIZONTAL_ERR_VALID;
                    }catch (IZatStaleDataException e){
                        Log.e(TAG, "HE exception ");
                        //do nothing
                    }

                    try{
                        apLocationData.mReliability = izatLocationData.getReliability().ordinal();
                        apLocationData.mValidBits |= apLocationData.AP_LOC_RELIABILITY_VALID;
                    }catch (IZatStaleDataException e){
                        Log.e(TAG, "REL exception ");
                        //do nothing
                    }

                    if (izatLocationData.isRttParametersValid()) {
                        apLocationData.mValidBits |= (apLocationData.AP_LOC_ALT_VALID |
                                apLocationData.AP_LOC_POSITION_QUALITY_VALID |
                                apLocationData.AP_LOC_RTT_CAPABILITY_VALID |
                                apLocationData.AP_LOC_RTT_BIAS_VALID);
                        try {
                            apLocationData.mRttCapability =
                                    izatLocationData.getRttCapability().ordinal();
                            apLocationData.mPositionQuality =
                                    izatLocationData.getApPositionQuality().ordinal();
                            apLocationData.mAltRefType =
                                    izatLocationData.getAltitudeRefType().ordinal();
                            apLocationData.mAltitude = izatLocationData.getAltitude();
                            apLocationData.mRttRangeBiasInMm =
                                    izatLocationData.getRttRangeBiasInMm();
                        } catch (IZatStaleDataException e) {
                            Log.e(TAG, "Fill RTT parameters fail!");
                        }
                    }
                    apLocationDataList.add(apLocationData);
                }
            }

            for(IZatAPSpecialInfo spl_info: izatSpecialInfoList) {
                if(null != spl_info) {
                    IZatDataValidation.dataValidate(spl_info);
                    APSpecialInfo info = new APSpecialInfo();
                    info.mMacAddress = spl_info.mMacAddress;
                    info.mInfo = spl_info.mInfo.ordinal();
                    apSpecialInfoList.add(info);
                }
            }

            try {
                mReceiver.pushLookupResult(apLocationDataList, apSpecialInfoList);
            } catch (RemoteException e) {
                throw new RuntimeException("Failed to push WiFi DB", e);
            }
        }
    }

    private class IZatWiFiDBProviderImpl extends IZatWiFiDBProvider {
        IWiFiDBProvider mProvider;
        PendingIntent mWiFiProviderIntent;
        public IZatWiFiDBProviderImpl(IWiFiDBProvider provider,
                                      IZatWiFiDBProviderResponseListener listener)
                throws IZatIllegalArgumentException {
            super(listener);
            if(null == provider || null == listener) {
                throw new IZatIllegalArgumentException("IZatWiFiDBProviderImpl:" +
                        " null receiver / listener");
            }
            mProvider = provider;
        }

        @Override
        public void requestAPObsLocData() {
            try {
                mProvider.requestAPObsLocData();
            } catch (RemoteException e) {
                throw new RuntimeException("Failed to request AP Observation data", e);
            }
        }
    }

    private class IZatAltitudeReceiverImpl extends IZatAltitudeReceiver {
        IAltitudeReceiver mReceiver;
        PendingIntent mAltReceiverIntent;
        public IZatAltitudeReceiverImpl(IAltitudeReceiver receiver,
                                        IZatAltitudeReceiverResponseListener listener)
            throws IZatIllegalArgumentException {
            super(listener);
            if(null == receiver || null == listener) {
                throw new IZatIllegalArgumentException("IZatAltitudeReceiverImpl:" +
                                                       " null receiver / listener");
            }
            mReceiver = receiver;
        }

        @Override
        public void pushAltitude(Location location) {
            try {
                mReceiver.pushAltitude(location);
            } catch (RemoteException e) {
                throw new RuntimeException("Failed to push altitude ", e);
            }
        }
    }

    private class IZatGeofenceServiceImpl implements IZatGeofenceService {
        IGeofenceService mService;
        public IZatGeofenceServiceImpl(IGeofenceService service) {
            mService = service;
        }

        @Override
        public void registerForGeofenceCallbacks(IZatGeofenceCallback statusCb)
                                                 throws IZatIllegalArgumentException {
            if (null == statusCb) {
                throw new IZatIllegalArgumentException("invalid input parameter");
            }
            /* save the callbacks bonding with the caller. If there is already a
               callback associated with the caller, then the new cb will override
               the existing cb. */
            synchronized (sGeofenceClientCallbackMapLock) {
                mGeofenceClientCallbackMap.put(this, statusCb);
            }
        }

        @Override
        public void deregisterForGeofenceCallbacks(IZatGeofenceCallback statusCb)
                                                   throws IZatIllegalArgumentException {
            if (null == statusCb) {
                throw new IZatIllegalArgumentException("invalid input parameter");
            }
            synchronized (sGeofenceClientCallbackMapLock) {
                mGeofenceClientCallbackMap.remove(this);
            }
        }

        @Override
        public void registerPendingIntent(PendingIntent geofenceIntent)
                                throws IZatIllegalArgumentException {
            if (null == geofenceIntent) {
                throw new IZatIllegalArgumentException("invalid input parameter");
            }

            synchronized (sGeofenceServiceLock) {
                try {
                    mService.registerPendingIntent(geofenceIntent);
                    mGeofenceIntent = geofenceIntent;
                } catch (RemoteException e) {
                    throw new RuntimeException("Failed registerPendingIntent");
                }
            }
        }

        @Override
        public void unregisterPendingIntent(PendingIntent geofenceIntent)
                                throws IZatIllegalArgumentException {
            if (null == geofenceIntent) {
                throw new IZatIllegalArgumentException("invalid input parameter");
            }

            synchronized (sGeofenceServiceLock) {
                try {
                    mService.unregisterPendingIntent(geofenceIntent);
                } catch (RemoteException e) {
                    throw new RuntimeException("Failed unregisterPendingIntent");
                }
            }
            mGeofenceIntent = null;
        }

        @Override
        public Map<IZatGeofenceHandle, IzatGeofence> recoverGeofences() {
            IZatGeofenceCallback cb = mGeofenceClientCallbackMap.get(this);
            if (null == cb) {
                Log.e(TAG, "callback is not registered");
                return null;
            }

            Map<IZatGeofenceHandle, IzatGeofence> gfHandleDataMap =
                new HashMap<> ();
            ArrayList<GeofenceData> gfList = new ArrayList<GeofenceData> ();

            try {
                synchronized(sGeofenceServiceLock) {
                    mService.recoverGeofences(gfList);
                }
            } catch (RemoteException e) {
                throw new RuntimeException("Failed to recover geofences", e);
            }
            synchronized (sGeofencesMapLock) {
                for (GeofenceData geofence : gfList) {
                    // create a new IzatGeofence object to be returned
                    IZatGeofenceService.IzatGeofence gfObj =
                            getIzatGeofenceFromGeofenceData(geofence);

                    // check if IZatGeofenceHandle for this geofence id exists
                    // Update gfHandleDataMap
                    boolean handleExists = false;
                    for (Map.Entry<IZatGeofenceServiceImpl.IZatGeofenceHandleImpl,
                            GeofenceMapItem> entry :
                        mGeofencesMap.entrySet()) {
                        if (entry.getValue().getHWGeofenceId() == geofence.getGeofenceId()) {
                            handleExists = true;
                            gfHandleDataMap.put(entry.getKey(), gfObj);
                            break;
                        }
                    }

                    if (false == handleExists) {
                        IZatGeofenceHandleImpl handle = new IZatGeofenceHandleImpl();
                        String appTextData = geofence.getAppTextData();
                        Object appData;
                        if (appTextData != null) {
                            appData = appTextData;
                        } else {
                            appData = geofence.getAppBundleData();
                        }
                        mGeofencesMap.put(handle,
                            new GeofenceMapItem(appData,
                            geofence.getGeofenceId(), cb, gfObj));
                        gfHandleDataMap.put(handle, gfObj);
                    }
                }
            }
            return gfHandleDataMap;
        }

        @Override
        public IZatGeofenceHandle addGeofence(Object context, IzatGeofence geofence)
                                              throws IZatIllegalArgumentException {
            if (null == geofence) {
                throw new IZatIllegalArgumentException("invalid null geofence");
            }
            if (geofence.getLatitude() < -90 || geofence.getLatitude() > 90) {
                throw new IZatIllegalArgumentException("invalid geofence latitude." +
                                                       " Expected in range -90..90.");
            }
            if (geofence.getLongitude() < -180 || geofence.getLongitude() > 180) {
                throw new IZatIllegalArgumentException("invalid geofence longitude." +
                                                       " Expected in range -180..180.");
            }
            IZatDataValidation.dataValidate(geofence);
            IZatGeofenceCallback cb = mGeofenceClientCallbackMap.get(this);
            int geofenceId;

            if (null == cb) {
                Log.e(TAG, "callback is not registered.");
                return null;
            }
            try {
                synchronized (sGeofenceServiceLock) {
                    double latitude = geofence.getLatitude();
                    double longitude = geofence.getLongitude();
                    double radius = geofence.getRadius();
                    int transitionType = geofence.getTransitionTypes().getValue();
                    int responsiveness = geofence.getNotifyResponsiveness();
                    int confidence = geofence.getConfidence().getValue();

                    int dwellTime = 0;
                    int dwellType = 0;
                    if (geofence.getDwellNotify() != null) {
                        dwellTime = geofence.getDwellNotify().getDwellTime();
                        dwellType = geofence.getDwellNotify().getDwellType();
                    }

                    if ((context != null) &&
                            ((context instanceof String) || (context instanceof Bundle))) {

                        String appTextData =
                                ((context instanceof String) ? context.toString() : null);
                        Bundle bundleObj =
                                ((context instanceof Bundle) ? (Bundle) context : null);

                        geofenceId = mService.addGeofenceObj(new GeofenceData(responsiveness,
                                                                              latitude,
                                                                              longitude,
                                                                              radius,
                                                                              transitionType,
                                                                              confidence,
                                                                              dwellType,
                                                                              dwellTime,
                                                                              appTextData,
                                                                              bundleObj,
                                                                              -1));
                    } else {
                        geofenceId = mService.addGeofence(latitude,
                                                          longitude,
                                                          radius,
                                                          transitionType,
                                                          responsiveness,
                                                          confidence,
                                                          dwellTime,
                                                          dwellType);
                    }
                }
            } catch (RemoteException e) {
                throw new RuntimeException("Failed addGeofence", e);
            }
            IZatGeofenceHandleImpl handle = new IZatGeofenceHandleImpl();
            synchronized (sGeofencesMapLock) {
                mGeofencesMap.put(handle,
                                  new GeofenceMapItem(context, geofenceId, cb, geofence));
            }
            return handle;
        }

        @Override
        public void removeGeofence(IZatGeofenceHandle handler)
                                   throws IZatIllegalArgumentException {
            if (null == handler ||
                !(handler instanceof IZatGeofenceServiceImpl.IZatGeofenceHandleImpl)) {
                throw new IZatIllegalArgumentException("invalid input parameter");
            }
            try {
                synchronized (sGeofenceServiceLock) {
                    GeofenceMapItem mapItem = mGeofencesMap.get(handler);
                    if (null == mapItem) {
                        Log.e(TAG, "this request ID is unknown.");
                        IZatGeofenceCallback cb =
                                mGeofenceClientCallbackMap.get(this);
                        if (cb != null) {
                            cb.onRequestFailed(handler,
                                    GEOFENCE_REQUEST_TYPE_REMOVE,
                                    GEOFENCE_RESULT_ERROR_ID_UNKNOWN);
                        } else {
                            throw new IZatIllegalArgumentException(
                                    "Invalid Geofence handle");
                        }
                        return;
                    }
                    mService.removeGeofence(mapItem.getHWGeofenceId());
                }
            } catch (RemoteException e) {
                throw new RuntimeException("Failed removeGeofence", e);
            }
            synchronized (sGeofencesMapLock) {
                mGeofencesMap.remove(handler);
            }
        }

        private GeofenceData recoverGeofenceDataById(int geofenceId)
                throws IZatIllegalArgumentException {
            ArrayList<GeofenceData> gfList = new ArrayList<GeofenceData> ();
            try {
                synchronized(sGeofenceServiceLock) {
                    mService.recoverGeofences(gfList);

                    for (GeofenceData geofence : gfList) {
                        if (geofenceId == geofence.getGeofenceId()) {
                            return geofence;
                        }
                    }
                    throw new IZatIllegalArgumentException("Invalid geofence id");
                }
            } catch (RemoteException e) {
                throw new RuntimeException("Failed to recover geofence", e);
            }
        }

        private IzatGeofence getIzatGeofenceFromGeofenceData(GeofenceData geofence) {
            IZatGeofenceService.IzatGeofence gfObj =
                    new IZatGeofenceService.IzatGeofence(geofence.getLatitude(),
                    geofence.getLongitude(), geofence.getRadius());
                    gfObj.setNotifyResponsiveness(geofence.getNotifyResponsiveness());
                    gfObj.setTransitionTypes(
                            IZatGeofenceService.IzatGeofenceTransitionTypes.values()[
                            geofence.getTransitionType().getValue()]);
                    gfObj.setConfidence(
                            IZatGeofenceService.IzatGeofenceConfidence.values()[
                            geofence.getConfidence().getValue() - 1]);

                    IZatGeofenceService.IzatDwellNotify dwellNotify =
                            new IZatGeofenceService.IzatDwellNotify(
                                    geofence.getDwellTime(),
                                    geofence.getDwellType().getValue());
                    gfObj.setDwellNotify(dwellNotify);

            return gfObj;
        }

        private class IZatGeofenceHandleImpl implements IZatGeofenceService.IZatGeofenceHandle {

            @Override
            public Object getContext() {
                GeofenceMapItem mapItem = mGeofencesMap.get(this);
                if (mapItem != null) {
                    return mapItem.getContext();
                }
                return null;
            }

            @Override
            @Deprecated
            public void update(IzatGeofenceTransitionTypes transitionTypes,
                               int notifyResponsiveness)
                               throws IZatIllegalArgumentException {
                if (null == transitionTypes || 0 >= notifyResponsiveness) {
                    throw new IZatIllegalArgumentException("invalid input parameter");
                }

                IzatGeofence gf = new IZatGeofenceService.IzatGeofence(null, null, null);
                gf.setTransitionTypes(transitionTypes);
                gf.setNotifyResponsiveness(notifyResponsiveness);

                update(gf);
            }

            @Override
            public void update(IzatGeofence geofence) throws IZatIllegalArgumentException {
                try {
                    GeofenceMapItem mapItem = mGeofencesMap.get(this);
                    synchronized (sGeofenceServiceLock) {
                        if (null == mapItem) {
                            Log.e(TAG, "this request ID is unknown.");
                            IZatGeofenceCallback cb =
                                    mGeofenceClientCallbackMap.get(this);
                            if (cb != null) {
                                cb.onRequestFailed(this,
                                        GEOFENCE_REQUEST_TYPE_UPDATE,
                                        GEOFENCE_RESULT_ERROR_ID_UNKNOWN);
                            } else {
                                throw new IZatIllegalArgumentException(
                                        "Invalid Geofence handle");
                            }
                            return;
                        }
                        mService.updateGeofenceData(mapItem.getHWGeofenceId(),
                                                    geofence.getLatitude(),
                                                    geofence.getLongitude(),
                                                    geofence.getRadius(),
                                                    geofence.getTransitionTypes().getValue(),
                                                    geofence.getNotifyResponsiveness(),
                                                    geofence.getConfidence().getValue(),
                                                    geofence.getDwellNotify().getDwellTime(),
                                                    geofence.getDwellNotify().getDwellType(),
                                                    geofence.getUpdatedFieldsMask());
                        geofence.resetUpdatedFieldsMask();
                    }
                    synchronized (sGeofencesMapLock) {
                        mapItem.updateGeofence(geofence);
                    }
                } catch (RemoteException e) {
                    throw new RuntimeException("Failed update", e);
                }
            }

            @Override
            public void pause() throws IZatIllegalArgumentException {
                try {
                    GeofenceMapItem mapItem = mGeofencesMap.get(this);
                    synchronized (sGeofenceServiceLock) {
                        if (null == mapItem) {
                            Log.e(TAG, "this request ID is unknown.");
                            IZatGeofenceCallback cb =
                                    mGeofenceClientCallbackMap.get(this);
                            if (cb != null) {
                                cb.onRequestFailed(this,
                                        GEOFENCE_REQUEST_TYPE_PAUSE,
                                        GEOFENCE_RESULT_ERROR_ID_UNKNOWN);
                            } else {
                                throw new IZatIllegalArgumentException(
                                        "Invalid Geofence handle");
                            }
                            return;
                        }
                        mService.pauseGeofence(mapItem.getHWGeofenceId());
                    }
                    synchronized (sGeofencesMapLock) {
                        mapItem.mIsPaused = true;
                    }
                } catch (RemoteException e) {
                    throw new RuntimeException("Failed pause", e);
                }
            }

            @Override
            public void resume() throws IZatIllegalArgumentException {
                try {
                    GeofenceMapItem mapItem = mGeofencesMap.get(this);
                    synchronized (sGeofenceServiceLock) {
                        if (null == mapItem) {
                            Log.e(TAG, "this request ID is unknown.");
                            IZatGeofenceCallback cb =
                                    mGeofenceClientCallbackMap.get(this);
                            if (cb != null) {
                                cb.onRequestFailed(this,
                                        GEOFENCE_REQUEST_TYPE_RESUME,
                                        GEOFENCE_RESULT_ERROR_ID_UNKNOWN);
                            } else {
                                throw new IZatIllegalArgumentException(
                                        "Invalid Geofence handle");
                            }
                            return;
                        }
                        mService.resumeGeofence(mapItem.getHWGeofenceId());
                    }
                    synchronized (sGeofencesMapLock) {
                        mapItem.mIsPaused = false;
                    }
                } catch (RemoteException e) {
                    throw new RuntimeException("Failed resume", e);
                }
            }

            @Override
            public boolean isPaused() throws IZatIllegalArgumentException {
                synchronized (sGeofenceServiceLock) {
                    GeofenceMapItem mapItem = mGeofencesMap.get(this);
                    if (null == mapItem) {
                        Log.e(TAG, "this request ID is unknown.");
                        throw new IZatIllegalArgumentException(
                                "Invalid Geofence handle");
                    }
                    return recoverGeofenceDataById(mapItem.mHWGeofenceId).isPaused();
                }
            }
       }
    }

    private class IZatGtpServiceImpl implements IZatGtpService {

        IGTPService mService;

        public IZatGtpServiceImpl(IGTPService service) {
            mService = service;
        }

        @Override
        public void requestLocationUpdates(IZatGtpServiceCallback callback, int minIntervalMillis) {
            requestLocationUpdates(callback, minIntervalMillis, IzatGtpAccuracy.NOMINAL);
        }

        @Override
        public void requestLocationUpdates(IZatGtpServiceCallback callback, int minIntervalMillis,
                IzatGtpAccuracy accuracy) {
            if (accuracy == IzatGtpAccuracy.NOMINAL) {
                if ((mFlpFeatureMasks & FEATURE_BIT_HW_RSSI_IS_SUPPORTED) == 0 ||
                        (mFlpFeatureMasks & FEATURE_BIT_QWES_GTP_RSSI_IS_SUPPORTED) == 0) {
                    throw new RuntimeException("NOMINAL accuracy request not supported");
                }
            } else if (accuracy == IzatGtpAccuracy.HIGH) {
                if ((mFlpFeatureMasks & FEATURE_BIT_HW_RTT_IS_SUPPORTED) == 0 ||
                        (mFlpFeatureMasks & FEATURE_BIT_QWES_GTP_RTT_IS_SUPPORTED) == 0) {
                    throw new RuntimeException("HIGH accuracy request not supported");
                }
            }
            synchronized (sGtpServiceLock) {
                try {
                    Log.d(TAG,"requestLocationUpdates minIntervalMillis: " + minIntervalMillis);
                    if (minIntervalMillis < 1000) {
                        Log.d(TAG, "Interval provided too small. Adjusting to 1 second.");
                        minIntervalMillis = 1000;
                    }
                    if (null != mActiveGtpCallback) {
                        Log.d(TAG, "removing existing active session");
                        mService.removeLocationUpdates();
                    }
                    mActiveGtpCallback = callback;
                    mActiveGtpTbf = minIntervalMillis;
                    mActiveGtpAccuracy = GTPAccuracy.values()[accuracy.getValue()];
                    mService.requestLocationUpdates(
                            new GtpServiceCallbackWrapper(mActiveGtpCallback),
                            new GtpRequestData(minIntervalMillis, mActiveGtpAccuracy));
                } catch (RemoteException e) {
                    throw new RuntimeException("Failed to start network positioning");
                }
            }
        }

        @Override
        public void requestWwanLocationUpdates(IZatGtpServiceCallback callback,
                int minIntervalMillis, IzatGtpAccuracy accuracy) {

            if (accuracy == IzatGtpAccuracy.NOMINAL) {
                if ((mFlpFeatureMasks & FEATURE_BIT_QWES_WWAN_STANDARD_IS_SUPPORTED) == 0) {
                    throw new RuntimeException("WWAN standard positioning not supported");
                }
            } else if (accuracy == IzatGtpAccuracy.HIGH) {
                if ((mFlpFeatureMasks & FEATURE_BIT_QWES_WWAN_PREMIUM_IS_SUPPORTED) == 0) {
                    throw new RuntimeException("HIGH premium positioning not supported");
                }
            }

            synchronized (sGtpServiceLock) {
                try {
                    Log.d(TAG,"requestWwanLocationUpdates minIntervalMillis: " + minIntervalMillis);
                    if (minIntervalMillis < 1000) {
                        Log.d(TAG, "Interval provided too small. Adjusting to 1 second.");
                        minIntervalMillis = 1000 * 3600;
                    }
                    mActiveGtpWwanCallback = callback;
                    mActiveGtpWwanTbf = minIntervalMillis;
                    mActiveGtpWwanAccuracy = GTPAccuracy.values()[accuracy.getValue()];
                    mService.requestWwanLocationUpdates(
                            new GtpServiceCallbackWrapper(mActiveGtpWwanCallback),
                            new GtpRequestData(minIntervalMillis, mActiveGtpWwanAccuracy));
                } catch (RemoteException e) {
                    throw new RuntimeException("Failed to start network positioning");
                }
            }
        }

        @Override
        public void removeLocationUpdates() {
            synchronized (sGtpServiceLock) {
                try {
                    Log.d(TAG,"removeLocationUpdates");
                    mActiveGtpCallback = null;
                    mActiveGtpTbf = -1;
                    mActiveGtpAccuracy = GTPAccuracy.NOMINAL;
                    mService.removeLocationUpdates();
                } catch (RemoteException e) {
                    throw new RuntimeException("Failed to stop network positioning");
                }
            }
        }

        @Override
        public void removeWwanLocationUpdates() {
            Log.d(TAG,"entering removeWwanLocationUpdates...");
            synchronized (sGtpServiceLock) {
                try {
                    Log.d(TAG,"removeWwanLocationUpdates");
                    mActiveGtpWwanCallback = null;
                    mActiveGtpWwanTbf = -1;
                    mActiveGtpWwanAccuracy = GTPAccuracy.NOMINAL;
                    mService.removeWwanLocationUpdates();
                } catch (RemoteException e) {
                    throw new RuntimeException("Failed to stop wwan positioning");
                }
            }
        }

        @Override
        public void requestPassiveLocationUpdates(IZatGtpServiceCallback callback) {
            synchronized (sGtpServiceLock) {
                try {
                    Log.d(TAG,"requestPassiveLocationUpdates");
                    if (null != mPassiveGtpCallback) {
                        Log.d(TAG, "removing existing passive session");
                        mService.removePassiveLocationUpdates();
                    }
                    mPassiveGtpCallback = callback;
                    mService.requestPassiveLocationUpdates(
                            new GtpServiceCallbackWrapper(mPassiveGtpCallback));
                } catch (RemoteException e) {
                    throw new RuntimeException("Failed to stop passive positioning");
                }
            }
        }

        @Override
        public void requestPassiveWwanLocationUpdates(IZatGtpServiceCallback callback) {
            synchronized (sGtpServiceLock) {
                try {
                    Log.d(TAG,"requestPassiveWwanLocationUpdates");
                    mPassiveGtpWwanCallback = callback;
                    mService.requestPassiveWwanLocationUpdates(
                            new GtpServiceCallbackWrapper(mPassiveGtpWwanCallback));
                } catch (RemoteException e) {
                    throw new RuntimeException("Failed to stop passive wwan positioning");
                }
            }
        }

        @Override
        public void removePassiveLocationUpdates() {
            synchronized (sGtpServiceLock) {
                try {
                    Log.d(TAG,"removePassiveLocationUpdates");
                    mPassiveGtpCallback = null;
                    mService.removePassiveLocationUpdates();
                } catch (RemoteException e) {
                    throw new RuntimeException("Failed to stop passive wwan positioning");
                }
            }
        }

        @Override
        public void removePassiveWwanLocationUpdates() {
            synchronized (sGtpServiceLock) {
                try {
                    Log.d(TAG,"removePassiveWwanLocationUpdates");
                    mPassiveGtpWwanCallback = null;
                    mService.removePassiveWwanLocationUpdates();
                } catch (RemoteException e) {
                    throw new RuntimeException("Failed to stop passive wwan positioning");
                }
            }
        }

        @Override
        public void setUserConsent(boolean userConsent) {
            synchronized (sGtpServiceLock) {
                try {
                    Log.d(TAG,"setUserConsent:  " + userConsent);
                    mService.setUserConsent(userConsent);
                } catch (RemoteException e) {
                    throw new RuntimeException("Failed to set user consent");
                }
            }
        }
    }

    private class IZatWWANAdReceiverImpl extends IZatWWANAdReceiver {
        IWWANAdReceiver mWWANAdReceiver;

        public IZatWWANAdReceiverImpl(IWWANAdReceiver wwanAdReceiver,
                IZatWWANAdRequestListener listener) {
            super(listener);
            if(null == wwanAdReceiver || null == listener) {
                throw new IZatIllegalArgumentException("IZatWWANAdReceiverImpl:" +
                                                      " null receiver / listener");
            }
            mWWANAdReceiver = wwanAdReceiver;
        }

        @Override
        public void pushWWANAssistanceData(int requestId, boolean status,
                IZatWWANADType respType, byte[] respPayload) {
            try {
                mWWANAdReceiver.pushWWANAssistanceData(requestId, status, respType.ordinal(),
                        respPayload);
            } catch (RemoteException e) {
                throw new RuntimeException("Failed to pushWWANAssistanceData");
            }
        }
    }

    private class WWANAdRequestListenerWrapper extends IWWANAdRequestListener.Stub {

        @Override
        public void onWWANAdRequest(int requestId, byte[] reqPayload) {
            if (null != mWWANAdReceiverImpl.mReqListener) {
                mWWANAdReceiverImpl.mReqListener.onWWANAdRequest(requestId, reqPayload);
            }
        }
    }

    private static class GtpServiceCallbackWrapper extends IGTPServiceCallback.Stub {
        private final IZatGtpService.IZatGtpServiceCallback callback;

        public GtpServiceCallbackWrapper(IZatGtpService.IZatGtpServiceCallback callback) {
            this.callback = callback;
        }

        @Override
        public void onLocationAvailable(Location location) {
            callback.onLocationAvailable(location);
        }
    }

    public class IZatDebugReportingServiceImpl implements IZatDebugReportingService {
        IDebugReportService mService;
        XtraStatusCallbackWrapper mXtraStatusCbWrapper;

        public IZatDebugReportingServiceImpl(IDebugReportService service) {
            mService = service;
        }

        @Override
        public void registerForDebugReports(IZatDebugReportCallback reportCb)
                                                 throws IZatIllegalArgumentException {
            if (null == reportCb) {
                throw new IZatIllegalArgumentException("invalid input parameter");
            }

            synchronized (sDebugReportCallbackMapLock) {
                mDebugReportClientCallbackMap.put(this, reportCb);
            }

            synchronized(sDebugReportServiceLock) {
                if (!mDebugReportClientCallbackMap.isEmpty()) {
                    try {
                        mService.startReporting();
                    } catch (RemoteException e) {
                        throw new RuntimeException("Failed to register for debug reports");
                    }
                }
            }
        }

        @Override
        public void deregisterForDebugReports(IZatDebugReportCallback reportCb)
                                                   throws IZatIllegalArgumentException {
            if (null == reportCb) {
                throw new IZatIllegalArgumentException("invalid input parameter");
            }

            synchronized (sDebugReportCallbackMapLock) {
                IZatDebugReportCallback cb = mDebugReportClientCallbackMap.get(this);
                if (cb != null) {
                    mDebugReportClientCallbackMap.remove(this);
                }
            }

            synchronized(sDebugReportServiceLock) {
                if (mDebugReportClientCallbackMap.isEmpty()) {
                    try {
                        mService.stopReporting();
                    } catch (RemoteException e) {
                        throw new RuntimeException("Failed to deregister for debug reports");
                    }
                }
            }
        }

       @Override
       public Bundle getDebugReport()
           throws IZatIllegalArgumentException {
           synchronized(sDebugReportServiceLock) {
                Bundle bdlReportObj = null;

                try {
                    bdlReportObj = mService.getDebugReport();
                } catch (RemoteException e) {
                    throw new RuntimeException("Failed to get debug report");
                }

                return bdlReportObj;
           }
       }
       @Override
        public void registerXtraStatusListener(IZatXtraStatusCallback listener) {
            if (null == listener) {
                throw new IZatIllegalArgumentException("invalid input parameter");
            }
            mXtraStatusCbWrapper = new XtraStatusCallbackWrapper(listener);
            synchronized(sDebugReportServiceLock) {
                try {
                    mService.registerXtraStatusListener(mXtraStatusCbWrapper);
                } catch (RemoteException e) {
                    throw new RuntimeException("Failed to register for xtra status");
                }
            }
        }
       @Override
        public void unregisterXtraStatusListener() {
            synchronized(sDebugReportServiceLock) {
                try {
                    mService.unregisterXtraStatusListener();
                } catch (RemoteException e) {
                    throw new RuntimeException("Failed to unregister for xtra status");
                }
            }
        }
       @Override
        public void getXtraStatus() {
            if (null == mXtraStatusCbWrapper) {
                Log.e(TAG, "xtra status cb not registered yet!");
                return;
            }
            synchronized(sDebugReportServiceLock) {
                try {
                    mService.getXtraStatus();
                } catch (RemoteException e) {
                    throw new RuntimeException("Failed to get xtra status");
                }
            }
        }
    }

    public class IZatGnssConfigServicesImpl implements IZatGnssConfigServices {
        IGnssConfigService mService;
        IZatRobustLocationConfigService mRobustLocationService;
        IZatPreciseLocationConfigService mPreciseLocationConfigService;
        IZatNtnConfigService mNtnConfigService;

        public IZatGnssConfigServicesImpl(IGnssConfigService service) {
            mService = service;
        }
        @Deprecated
        public synchronized IZatSvConfigService getSvConfigService() {
            Log.e(TAG, "Izat Gnss SV constellation config is not supported on this device.");
            throw new IZatFeatureNotSupportedException(
                    "Izat Gnss SV constellation config is not supported on this device.");
        }

        public synchronized IZatRobustLocationConfigService getRobustLocationConfigService() {
            if ((mFlpFeatureMasks & FEATURE_BIT_CONFORMITY_INDEX_IS_SUPPORTED)>0) {
                if (mRobustLocationService == null) {
                    mRobustLocationService = new IZatRobustLocationConfigServiceImpl();
                }
            } else {
                Log.e(TAG, "Izat Robust Location is not supported on this device");
                throw new IZatServiceUnavailableException(
                        "Izat Robust Location is not supported on this device.");
            }
            return mRobustLocationService;
        }

        public synchronized IZatPreciseLocationConfigService getPreciseLocationConfigService() {
            if ((mFlpFeatureMasks & FEATURE_BIT_PRECISE_LOCATION_IS_SUPPORTED)>0) {
                if (mPreciseLocationConfigService == null) {
                    mPreciseLocationConfigService = new IZatPreciseLocationConfigServiceImpl();
                }
            } else {
                Log.e(TAG, "Izat Precise Location is not supported on this device.");
                throw new IZatServiceUnavailableException(
                        "Izat Precise Location is not supported on this device.");
            }

            return mPreciseLocationConfigService;
        }

        @Override
        public synchronized IZatNtnConfigService getNtnConfigService() {
            if (mNtnConfigService == null) {
                mNtnConfigService = new IZatNtnConfigServiceImpl();
            }
            return mNtnConfigService;
        }

        @Override
        public void setNetworkLocationUserConsent(boolean hasUserConsent) {
            try {
                mService.setNetworkLocationUserConsent(hasUserConsent);
            } catch (RemoteException e) {
                throw new RuntimeException("Failed to set network location user consent");
            }
        }

        @Override
        public void injectSuplCert(int suplCertId, byte[] suplCertData) {
            if (suplCertId < 0 || suplCertId > 9 || suplCertData.length > 2000) {
                Log.e(TAG, "suplCertId out of range [0, 9] or suplCertData exceeds 2000 bytes!");
                throw new IZatIllegalArgumentException("Invalid supl cert");
            }
            try {
                mService.injectSuplCert(suplCertId, suplCertData);
            } catch (RemoteException e) {
                throw new RuntimeException("Failed to inject supl cert");
            }
        }

        private class IZatRobustLocationConfigServiceImpl
                implements IZatGnssConfigServices.IZatRobustLocationConfigService {
            @Override
            public void getRobustLocationConfig(IZatRLConfigCallback rlConfigCb) {
                if (null == rlConfigCb) {
                    throw new IZatIllegalArgumentException("Invalid robust location config cb");
                }

                synchronized (sRLConfigCallbackMapLock) {
                    mRLConfigCallback = rlConfigCb;
                }

                synchronized(sGnssConfigServiceLock) {
                    if (mRLConfigCallback != null) {
                        try {
                            if (!mGnssConfigCbWrapper.mRegisterStatus) {
                                mService.registerCallback(mGnssConfigCbWrapper);
                                mGnssConfigCbWrapper.mRegisterStatus = true;
                            }
                            mService.getRobustLocationConfig();
                        } catch (RemoteException e) {
                            throw new RuntimeException("Failed to get gnss sv type config");
                        }
                    }
                }
            }

            @Override
            public void setRobustLocationConfig(boolean enable, boolean enableForE911) {
                synchronized(sGnssConfigServiceLock) {
                    try {
                        mService.setRobustLocationConfig(enable, enableForE911);
                    } catch (RemoteException e) {
                        throw new RuntimeException("Failed to set Robust location config");
                    }
                }
            }
            @Override
            public boolean configMerkleTree(String merkleTreeXml, int xmlSize) {
                boolean retVal = false;
                synchronized(sGnssConfigServiceLock) {
                    try {
                        retVal = mService.configMerkleTree(merkleTreeXml, xmlSize);
                    } catch (RemoteException e) {
                        throw new RuntimeException("Failed to config Merkle Tree");
                    }
                }
                return retVal;
            }

            @Override
            public boolean configOsnmaEnablement(boolean isEnabled) {
                boolean retVal = false;
                synchronized(sGnssConfigServiceLock) {
                    try {
                        retVal = mService.configOsnmaEnablement(isEnabled);
                    } catch (RemoteException e) {
                        throw new RuntimeException("Failed to set Osnma enablement");
                    }
                }
                return retVal;
            }
        }

        private class IZatPreciseLocationConfigServiceImpl
                implements IZatGnssConfigServices.IZatPreciseLocationConfigService {

            private boolean mOptIn = false;
            @Override
            public void setPreciseLocationOptIn(IZatPreciseLocationOptIn optIn) {
                try {
                    boolean bOptIn =
                            (optIn == IZatPreciseLocationOptIn.OPTED_IN_FOR_LOCATION_REPORT);
                    mService.updateNtripGgaConsent(bOptIn);
                    mOptIn = bOptIn;
                    } catch (RemoteException e) {
                    throw new RuntimeException("Failed to set enable precise location config");
                }
            }

            @Override
            public void enablePreciseLocation(IZatPreciseLocationNTRIPSettings ntripSettings,
                    boolean requiresInitialNMEA) throws IZatIllegalArgumentException {
                synchronized(sGnssConfigServiceLock) {
                    if (requiresInitialNMEA && !mOptIn) {
                        throw new IZatIllegalArgumentException(
                                "NMEA required but optIn was not set.");
                    }

                    NtripConfigData data = new NtripConfigData();
                    data.mHostNameOrIP = ntripSettings.mHostNameOrIP;
                    data.mMountPointName = ntripSettings.mMountPointName;
                    data.mPort = ntripSettings.mPort;
                    data.mUserName = ntripSettings.mUserName;
                    data.mPassword = ntripSettings.mPassword;
                    data.mRequiresInitialNMEA = requiresInitialNMEA;
                    data.mUseSSL = ntripSettings.mUseSSL;
                    data.mNmeaUpdateInterval = ntripSettings.mNmeaUpdateInterval;

                    try {
                        mService.enablePreciseLocation(data);
                    } catch (RemoteException e) {
                        throw new RuntimeException("Failed to set enable precise location config");
                    }
                }
            }

            @Override
            public void disablePreciseLocation() {
                try {
                    mService.disablePreciseLocation();
                } catch (RemoteException e) {
                    throw new RuntimeException("Failed to set enable precise location config");
                }
            }
        }

        private class IZatNtnConfigServiceImpl
                implements IZatGnssConfigServices.IZatNtnConfigService {

            @Override
            public void registerNtnStatusCallback(IZatNtnStatusCallback callback) {
                if (null == callback) {
                    throw new IZatIllegalArgumentException("Invalid ntnStatusCallback!");
                }

                try {
                    if (!mGnssConfigCbWrapper.mRegisterStatus) {
                        mService.registerCallback(mGnssConfigCbWrapper);
                        mGnssConfigCbWrapper.mRegisterStatus = true;
                    }
                } catch (RemoteException e) {
                    throw new RuntimeException("Failed to registerNtnStatusCallback");
                }
                synchronized (sntnStatusCallbackLock) {
                    mntnStatusCallback = callback;
                }
            }

            @Override
            public void set3rdPartyNtnCapability(boolean isCapable) {
                synchronized(sGnssConfigServiceLock) {
                    try {
                        mService.set3rdPartyNtnCapability(isCapable);
                    } catch (RemoteException e) {
                        throw new RuntimeException("Failed to set3rdPartyNtnCapability");
                    }
                }
            }

            @Override
            public void getNtnConfigSignalMask() {
                if (mntnStatusCallback == null) {
                    throw new IZatIllegalArgumentException("ntnStatusCallback was not set.");
                }
                synchronized(sGnssConfigServiceLock) {
                    try {
                        mService.getNtnConfigSignalMask();
                    } catch (RemoteException e) {
                        throw new RuntimeException("Failed to getNtnConfigSignalMask");
                    }
                }
            }

            @Override
            public void setNtnConfigSignalMask(int gpsSignalTypeConfigMask) {
                if (mntnStatusCallback == null) {
                    throw new IZatIllegalArgumentException("ntnStatusCallback was not set.");
                }
                synchronized(sGnssConfigServiceLock) {
                    try {
                        mService.setNtnConfigSignalMask(gpsSignalTypeConfigMask);
                    } catch (RemoteException e) {
                        throw new RuntimeException("Failed to setNtnConfigSignalMask");
                    }
                }
            }
        }
    }

    /**
     * Implementation of callback object. (FOR INTERNAL USE ONLY)
     *
     * @hide
     */
    private class LocationCallbackWrapper extends ILocationCallback.Stub {
        IZatFlpService.IFlpLocationCallback mCallback;
        public LocationCallbackWrapper(IZatFlpService.IFlpLocationCallback callback) {
            mCallback = callback;
        }
        public void onLocationAvailable(Location[] locations) {
            if (null == mCallback) {
                Log.w(TAG, "mCallback is NULL in LocationCallbackWrapper");
                return;
            }
            IZatDataValidation.dataValidate(locations);
            mCallback.onLocationAvailable(locations);
        }
    }

    /* Implementation of callback object. (FOR INTERNAL USE ONLY)
     *
     * @hide
     */
    private class FlpStatusCallbackWrapper extends ISessionStatusCallback.Stub {
        IZatFlpService.IFlpStatusCallback mCallback;
        IFlpService mService;

        public FlpStatusCallbackWrapper(IZatFlpService.IFlpStatusCallback callback,
            IFlpService flpService) {
            mCallback = callback;
            mService = flpService;
        }

        public void onBatchingStatusCb(int status) {
            if (null == mCallback) {
                Log.w(TAG, "mCallback is NULL in FlpStatusCallbackWrapper");
            }
            IZatDataValidation.dataValidate(status, IZatDataTypes.FLP_STATUS);

            try {
                synchronized(sFlpServiceLock) {
                    IZatFlpService.IzatFlpStatus batchStatus =
                            IZatFlpService.IzatFlpStatus.values()[status];

                    if (batchStatus != IZatFlpService.IzatFlpStatus.OUTDOOR_TRIP_COMPLETED) {
                        mCallback.onBatchingStatusCb(batchStatus);
                        return;
                    }

                    FlpRequestMapItem mapItem = null;
                    IZatFlpServiceImpl.IZatSessionHandlerImpl sessionHandler = null;
                    for (IZatFlpServiceImpl.IZatSessionHandlerImpl key : mFlpRequestsMap.keySet()) {
                        mapItem = mFlpRequestsMap.get(key);
                        if (mapItem.getStatusCallback() == this) {
                            sessionHandler = key;
                            break;
                        }
                    }

                    if (null == mapItem) {
                        Log.w(TAG, "no flp session undergoing");
                        return;
                    }

                    NotificationType notifType = mapItem.getNotifyType();
                    if ((IZatFlpService.IzatFlpStatus.OUTDOOR_TRIP_COMPLETED == batchStatus) &&
                        (NotificationType.NOTIFICATION_WHEN_TRIP_IS_COMPLETED == notifType)) {
                        mCallback.onBatchingStatusCb(batchStatus);

                        // update the FLP session previous notification mode
                        if (mapItem.getRestartOnTripCompleted()) {
                            int result = FLP_RESULT_SUCCESS;
                            result = mService.startFlpSession(mapItem.getHwId(),
                                    mapItem.getPreviousNotifyType().getCode(),
                                    mapItem.getTimeInterval(),
                                    mapItem.getDistanceInterval(), mapItem.getTripDistance());

                            if (FLP_RESULT_SUCCESS == result) {
                                // update the item flag in map
                                mapItem.updateNotifyType(mapItem.getPreviousNotifyType());
                                mapItem.setRestartOnTripCompleted(false);

                                if ((mFlpFeatureMasks &
                                    FEATURE_BIT_DISTANCE_BASED_BATCHING_IS_SUPPORTED)>0) {
                                    if (NotificationType.NOTIFICATION_ON_EVERY_LOCATION_FIX ==
                                            mapItem.getNotifyType()) {
                                        // switching from trip mode to foreground mode
                                        mService.unregisterCallback(FLP_SEESION_BACKGROUND,
                                                                    mapItem.getCbWrapper());
                                        mService.registerCallback(FLP_SEESION_FOREGROUND,
                                                                  mapItem.getHwId(),
                                                                  mapItem.getCbWrapper(),
                                                                  mapItem.getSessionStartTime());
                                    }
                                }
                            } else {
                                Log.v(TAG, "mService.updateFlpSession on trip completed failed.");
                            }
                        } else {
                            // remove the session handler from the cache as the trip ahs completed
                            mService.unregisterCallback(FLP_SEESION_BACKGROUND,
                                                        mapItem.getCbWrapper());
                            sessionHandler.unregisterForSessionStatus();
                            mFlpRequestsMap.remove(sessionHandler);
                        }
                    }
                }
            } catch (RemoteException e) {
                   throw new RuntimeException("Failed to handle onBatchingStatusCb for status:" +
                           status);
            }
        }
    }

    private enum NotificationType {
        NOTIFICATION_WHEN_BUFFER_IS_FULL(1),
        NOTIFICATION_ON_EVERY_LOCATION_FIX(2),
        NOTIFICATION_WHEN_TRIP_IS_COMPLETED(3);
        private final int mCode;
        private NotificationType(int c) {
            mCode = c;
        }

        public int getCode() {
            return mCode;
        }
    }

    private class FlpRequestMapItem {
        public IZatFlpService.IFlpLocationCallback mCallback = null;
        public NotificationType mNotiType = null;
        public LocationCallbackWrapper mCbWrapper = null;
        public FlpStatusCallbackWrapper mStatusCbWrapper = null;
        public long mMaxTime = -1;
        public int mMaxDistance = -1;
        public long mTripDistance = -1;
        public int mHwId = -1;
        private long mSessionStartTime = -1;
        private boolean mRestartOnTripCompleted = false;
        private NotificationType mPreviousNotifType = null;
        private int mPowerMode = 0;
        private long mTbmMillis = 0;
        public FlpRequestMapItem(IZatFlpService.IFlpLocationCallback callback,
                                 NotificationType notiType,
                                 long maxTime,
                                 int maxDistance,
                                 long tripDistance,
                                 LocationCallbackWrapper cbWrapper,
                                 int hwId,
                                 long sessionStartTime,
                                 int powerMode,
                                 long tbmMillis) {
            mCallback = callback;
            mNotiType = notiType;
            mPreviousNotifType = notiType;
            mMaxTime = maxTime;
            mMaxDistance = maxDistance;
            mTripDistance = tripDistance;
            mCbWrapper = cbWrapper;
            mHwId = hwId;
            mSessionStartTime = sessionStartTime;
            mRestartOnTripCompleted = false;
            mStatusCbWrapper = null;
            mPowerMode = powerMode;
            mTbmMillis = tbmMillis;
        }
        public IZatFlpService.IFlpLocationCallback getCallback() {
            return mCallback;
        }
        public void updateNotifyType(NotificationType type) {
            mNotiType = type;

            if (NotificationType.NOTIFICATION_WHEN_TRIP_IS_COMPLETED != type) {
                mPreviousNotifType = type;
            }
        }
        public NotificationType getNotifyType() {
            return mNotiType;
        }

        public NotificationType getPreviousNotifyType() {
            return mPreviousNotifType;
        }

        public long getTimeInterval() {
            return mMaxTime;
        }
        public int getDistanceInterval() {
            return mMaxDistance;
        }

        public long getTripDistance() {
            return mTripDistance;
        }

        public LocationCallbackWrapper getCbWrapper() {
            return mCbWrapper;
        }
        public int getHwId() {
            return mHwId;
        }
        public long getSessionStartTime() {
            return mSessionStartTime;
        }
        public void setSessionStartTime(long sessionStartTime) {
            mSessionStartTime = sessionStartTime;
        }

        public void setRestartOnTripCompleted(boolean restart) {
            mRestartOnTripCompleted = restart;
        }

        public boolean getRestartOnTripCompleted() {
            return mRestartOnTripCompleted;
        }

        public void setStatusCallback(FlpStatusCallbackWrapper cbWrapper) {
            mStatusCbWrapper = cbWrapper;
        }

        public FlpStatusCallbackWrapper getStatusCallback() {
            return mStatusCbWrapper;
        }

        public void setPowerMode(int powerMode) {
            if (0== (mFlpFeatureMasks & FEATURE_BIT_AGPM_IS_SUPPORTED)) {
                throw new IZatFeatureNotSupportedException ("AGPM is not supported");
            }
            mPowerMode = powerMode;
        }
        public int getPowerMode() {
            return mPowerMode;
        }

        public void setTbmMillis(long tbmMillis) {
            if (0== (mFlpFeatureMasks & FEATURE_BIT_AGPM_IS_SUPPORTED)) {
                throw new IZatFeatureNotSupportedException ("AGPM is not supported");
            }
            mTbmMillis = tbmMillis;
        }
        public long getTbmMillis() {
            return mTbmMillis;
        }
    }

    private class GeofenceMapItem {
        Object mContext;
        int mHWGeofenceId;
        IZatGeofenceService.IZatGeofenceCallback mCallback;
        IZatGeofenceService.IzatGeofence mGeofence;
        volatile boolean mIsPaused;

        public GeofenceMapItem(Object context,
                               int geofenceId,
                               IZatGeofenceService.IZatGeofenceCallback callback,
                               IZatGeofenceService.IzatGeofence geofence)
        {
            mContext = context;
            mHWGeofenceId = geofenceId;
            mCallback = callback;
            mGeofence = geofence;
            mIsPaused = false;
        }

        public Object getContext() {
            return mContext;
        }
        public void setHWGeofenceId(int id) {
            mHWGeofenceId = id;
        }
        public int getHWGeofenceId() {
            return mHWGeofenceId;
        }
        public IZatGeofenceService.IZatGeofenceCallback getCallback() {
            return mCallback;
        }
        public void updateGeofence(IZatGeofenceService.IzatGeofence geofence) {
            mGeofence = geofence;
        }
        public IZatGeofenceService.IzatGeofence getGeofence() {
            return mGeofence;
        }
    }

    private class MaxPowerAllocatedCallbackWrapper
        extends IMaxPowerAllocatedCallback.Stub {

        IZatTestService.IFlpMaxPowerAllocatedCallback mCallback;
        public MaxPowerAllocatedCallbackWrapper(
            IZatTestService.IFlpMaxPowerAllocatedCallback callback) {
            mCallback = callback;
        }
        public void onMaxPowerAllocatedChanged(double power_mW) {
            if (null == mCallback) {
                Log.w(TAG, "mCallback is NULL in MaxPowerAllocatedCallbackWrapper");
                return;
            }
            mCallback.onMaxPowerAllocatedChanged(power_mW);
        }
    }

    private class GeofenceStatusCallbackWrapper
        extends IGeofenceCallback.Stub {

        public void onTransitionEvent(int geofenceHwId,
                                      int event,
                                      Location location) {
            if (VERBOSE) {
                Log.d(TAG, "onTransitionEvent - geofenceHwId is " +
                      geofenceHwId + "; event is " + event);
            }
            IZatDataValidation.dataValidate(location);
            IZatDataValidation.dataValidate(event, IZatDataTypes.GEO_EVENT);
            // find the callback through geofence hw id
            synchronized (sGeofencesMapLock) {
                GeofenceMapItem mapItem = null;
                for (IZatGeofenceServiceImpl.IZatGeofenceHandleImpl key :
                        mGeofencesMap.keySet()) {
                    mapItem = mGeofencesMap.get(key);
                    if (mapItem.getHWGeofenceId() == geofenceHwId) {
                        mapItem.getCallback().onTransitionEvent(key, event, location);
                        return;
                    }
                }
            }
        }

        public void onRequestResultReturned(int geofenceHwId,
                                            int requestType,
                                            int result) {
            if (VERBOSE) {
                Log.d(TAG, "onRequestResultReturned - geofenceHwId is " +
                      geofenceHwId + "; requestType is " + requestType +
                      "; result is " + result);
            }
            IZatDataValidation.dataValidate(requestType, IZatDataTypes.GEO_REQUEST_TYPE);
            IZatDataValidation.dataValidate(result, IZatDataTypes.GEO_ERROR_CODE);
            if (result == IZatGeofenceService.GEOFENCE_RESULT_SUCCESS) {
                return;
            }
            // find the callback through geofence hw id
            synchronized (sGeofencesMapLock) {
                GeofenceMapItem mapItem = null;
                for (IZatGeofenceServiceImpl.IZatGeofenceHandleImpl key :
                        mGeofencesMap.keySet()) {
                    mapItem = mGeofencesMap.get(key);
                    if (mapItem.getHWGeofenceId() == geofenceHwId) {
                        if (IZatGeofenceService.GEOFENCE_REQUEST_TYPE_ADD == requestType) {
                            mGeofencesMap.remove(key);
                        }
                        mapItem.getCallback().onRequestFailed(key, requestType, result);
                        return;
                    }
                }
            }
        }

        public void onEngineReportStatus(int status,
                                         Location location) {
            if (VERBOSE) {
                Log.d(TAG, "onEngineReportStatus - status is " + status);
            }
            IZatDataValidation.dataValidate(location);
            IZatDataValidation.dataValidate(status, IZatDataTypes.GEO_ENGINE_STATUS);
            // broadcast to all clients
            synchronized (sGeofenceClientCallbackMapLock) {
                for (IZatGeofenceServiceImpl key : mGeofenceClientCallbackMap.keySet()) {
                    mGeofenceClientCallbackMap.get(key).onEngineReportStatus(status,
                                                                             location);
                }
            }
        }
    }

    private class DebugReportCallbackWrapper
        extends IDebugReportCallback.Stub {

        public void onDebugDataAvailable(Bundle debugReport) {
            if (VERBOSE) {
                Log.v(TAG, "onDebugDataAvailable");
            }
            synchronized(sDebugReportCallbackMapLock) {
                // broadcast to all clients
                for (IZatDebugReportingServiceImpl key : mDebugReportClientCallbackMap.keySet()) {
                    mDebugReportClientCallbackMap.get(key).onDebugReportAvailable(debugReport);
                }
            }
        }
    }

    private class XtraStatusCallbackWrapper extends IXtraStatusCallback.Stub {
        IZatXtraStatusCallback mCallback;
        public XtraStatusCallbackWrapper(IZatXtraStatusCallback cb) {
            mCallback = cb;
        }
        public void onXtraStatusChanged(IZatXTRAStatus xtraStatus) {
            if (VERBOSE) {
                Log.v(TAG, "onXtraStatusChanged");
            }
            if (mCallback != null) {
                IZatXtraStatus xtra = new IZatXtraStatus();
                xtra.mEnabled = xtraStatus.getEnabled();
                xtra.mXtraDataStatus = IZatXtraDataStatus.fromInt(xtraStatus.getXtraDataStatus());
                xtra.mValidityHrs = xtraStatus.getValidityHrs();
                xtra.mLastDownloadStatus = xtraStatus.getLastDownloadStatus();
                mCallback.onXtraStatusChanged(xtra);
            }
        }
    }

    private class WiFiDBReceiverRespListenerWrapper extends IWiFiDBReceiverResponseListener.Stub {

        // Decprecated, for backwards compatibility with legacy SDK
        public void onAPListAvailable(List<APInfo> ap_info) {
            if (VERBOSE) {
                Log.d(TAG, "onAPListAvailable");
            }
            if(null != mWiFiDBRecImpl) {
                List<IZatAPInfo> apInfo = new ArrayList<IZatAPInfo>();
                for(APInfo ap: ap_info) {
                    IZatCellInfo cellInfo = null;

                    if (!(ap.mCellRegionID1 == 0 && ap.mCellRegionID2 == 0 && ap.mCellRegionID3 == 0
                            && ap.mCellRegionID4 == 0)) {
                        cellInfo = new IZatCellInfo(ap.mCellRegionID1,
                                ap.mCellRegionID2,
                                ap.mCellRegionID3,
                                ap.mCellRegionID4,
                                ap.mCellType);
                    }
                    IZatAPSSIDInfo ssidInfo = null;
                    if(null != ap.mSSID && ap.mSSID.length > 0) {
                        ssidInfo = new IZatAPSSIDInfo(ap.mSSID, (short)ap.mSSID.length);
                    }
                    apInfo.add(new IZatAPInfo(ap.mMacAddress, 0,
                            new IZatAPInfoExtra(cellInfo, ssidInfo)));
                }

                if (VERBOSE) {
                    log(apInfo, null, null);
                }
                for(IZatAPInfo item : apInfo) {
                    IZatDataValidation.dataValidate(item);
                }

                if (null != mWiFiDBRecImpl.mResponseListener) {
                    mWiFiDBRecImpl.mResponseListener.onAPListAvailable(apInfo);
                }
            }
        }

        private IZatAPInfo iZatAPInfoFromAPInfoExt(APInfoExt ap) {
            IZatCellInfo cellInfo = null;

            if (!(ap.mCellRegionID1 == 0 && ap.mCellRegionID2 == 0 && ap.mCellRegionID3 == 0
                    && ap.mCellRegionID4 == 0)) {
                cellInfo = new IZatCellInfo(ap.mCellRegionID1,
                        ap.mCellRegionID2,
                        ap.mCellRegionID3,
                        ap.mCellRegionID4,
                        ap.mCellType);
            }
            IZatAPSSIDInfo ssidInfo = null;
            if (null != ap.mSSID && ap.mSSID.length > 0) {
                ssidInfo = new IZatAPSSIDInfo(ap.mSSID, (short)ap.mSSID.length);
            }

            IZatAPInfoExtra extra = null;
            if (null != cellInfo && null != ssidInfo) {
               extra = new IZatAPInfoExtra(cellInfo, ssidInfo);
            }

            return new IZatAPInfo(ap.mMacAddress, ap.mTimestamp, extra);
        }

        public void onAPListAvailableExt(List<APInfoExt> ap_info, int status, Location location) {
            if (VERBOSE) {
                Log.d(TAG, "onAPListAvailable");
            }
            if(null != mWiFiDBRecImpl) {
                List<IZatAPInfo> iZatApInfoList = new ArrayList<IZatAPInfo>();
                for(APInfoExt ap: ap_info) {
                    IZatAPInfo iZatAPInfo = iZatAPInfoFromAPInfoExt(ap);
                    iZatApInfoList.add(iZatAPInfo);
                }

                if (VERBOSE) {
                    log(iZatApInfoList, IZatApBsListStatus.fromInt(status), location);
                }
                for(IZatAPInfo item : iZatApInfoList) {
                    IZatDataValidation.dataValidate(item);
                }

                if (null != mWiFiDBRecImpl.mResponseListenerExt) {
                    mWiFiDBRecImpl.mResponseListenerExt.onAPListAvailable(iZatApInfoList,
                            IZatApBsListStatus.fromInt(status), location);
                }
            }
        }

        @Deprecated
        public void onLookupRequest(List<APInfoExt> ap_info, Location location) {
            if (VERBOSE) {
                Log.d(TAG, "onLookupRequest");
            }
            if(null != mWiFiDBRecImpl) {
                List<IZatAPInfo> iZatApInfoList = new ArrayList<IZatAPInfo>();
                for(APInfoExt ap: ap_info) {
                    IZatAPInfo iZatAPInfo = iZatAPInfoFromAPInfoExt(ap);
                    if (VERBOSE) {
                        Log.d(TAG, "onLookupRequest ap.mFdalStatus: " + ap.mFdalStatus);
                    }
                    iZatAPInfo.setFdalStatus(ap.mFdalStatus);
                    iZatApInfoList.add(iZatAPInfo);
                }

                if (VERBOSE) {
                    log(iZatApInfoList, IZatApBsListStatus.STD_FINAL, location);
                }
                for(IZatAPInfo item : iZatApInfoList) {
                    IZatDataValidation.dataValidate(item);
                }
                IZatDataValidation.dataValidate(location);
                if (null != mWiFiDBRecImpl.mResponseListenerExt) {
                    mWiFiDBRecImpl.mResponseListenerExt.onLookupRequest(iZatApInfoList, location);
                }
            }
        }

        public void onStatusUpdate(boolean is_success, String error) {
            if (VERBOSE) {
                Log.d(TAG, "onStatusUpdate");
            }
            if(null != mWiFiDBRecImpl) {
                if (null != mWiFiDBRecImpl.mResponseListenerExt) {
                    mWiFiDBRecImpl.mResponseListenerExt.onStatusUpdate(is_success, error);
                } else if (null != mWiFiDBRecImpl.mResponseListener) {
                    mWiFiDBRecImpl.mResponseListener.onStatusUpdate(is_success, error);
                }

            }
        }

        @Deprecated
        public void onServiceRequest() {
            if (VERBOSE) {
                Log.d(TAG, "onServiceRequest");
            }
            if(null != mWiFiDBRecImpl) {
                if (null != mWiFiDBRecImpl.mResponseListenerExt) {
                    mWiFiDBRecImpl.mResponseListenerExt.onServiceRequest();
                } else if (null != mWiFiDBRecImpl.mResponseListener) {
                    mWiFiDBRecImpl.mResponseListener.onServiceRequest();
                }
            }
        }

        public boolean onServiceRequestES(boolean isEmergency) {
            if (VERBOSE) {
                Log.d(TAG, "onServiceRequestES: " + isEmergency);
            }
            if(null != mWiFiDBRecImpl) {
                if (null != mWiFiDBRecImpl.mResponseListenerExt) {
                    try {
                        mWiFiDBRecImpl.mResponseListenerExt.onServiceRequest(isEmergency);
                    } catch (AbstractMethodError e) {
                        mWiFiDBRecImpl.mResponseListenerExt.onServiceRequest();
                    }
                } else if (null != mWiFiDBRecImpl.mResponseListener) {
                    mWiFiDBRecImpl.mResponseListener.onServiceRequest();
                }
            }
            return true;
        }

        public boolean onLookupRequestES(List<APInfoExt> ap_info, Location location,
                boolean isEmergency) {
            if (VERBOSE) {
                Log.d(TAG, "onLookupRequestES: " + isEmergency);
            }
            if(null != mWiFiDBRecImpl) {
                List<IZatAPInfo> iZatApInfoList = new ArrayList<IZatAPInfo>();
                for(APInfoExt ap: ap_info) {
                    IZatAPInfo iZatAPInfo = iZatAPInfoFromAPInfoExt(ap);
                    if (VERBOSE) {
                        Log.d(TAG, "onLookupRequest ap.mFdalStatus: " + ap.mFdalStatus);
                    }
                    iZatAPInfo.setFdalStatus(ap.mFdalStatus);
                    iZatApInfoList.add(iZatAPInfo);
                }

                if (VERBOSE) {
                    log(iZatApInfoList, IZatApBsListStatus.STD_FINAL, location);
                }
                for(IZatAPInfo item : iZatApInfoList) {
                    IZatDataValidation.dataValidate(item);
                }
                IZatDataValidation.dataValidate(location);
                if (null != mWiFiDBRecImpl.mResponseListenerExt) {
                    try {
                        mWiFiDBRecImpl.mResponseListenerExt.onLookupRequest(
                                iZatApInfoList, location, isEmergency);
                    } catch (AbstractMethodError e) {
                        mWiFiDBRecImpl.mResponseListenerExt.onLookupRequest(
                                iZatApInfoList, location);
                    }
                }
            }
            return true;
        }

    }

    private class WiFiDBProviderRespListenerWrapper extends IWiFiDBProviderResponseListener.Stub {

        public void onApObsLocDataAvailable(List<APObsLocData> apObsList, int apStatus) {
            if (VERBOSE) {
                Log.d(TAG, "onApObsLocDataAvailable");
            }
            if(null != mWiFiDBProvImpl) {
                List<IZatAPObsLocData> iZatAPObsList = new ArrayList<IZatAPObsLocData>();

                for(APObsLocData apObs: apObsList) {
                    List<IZatAPScan> apScanList = null;
                    List<IZatAPRttScan> apRttScanList = null;
                    if (apObs.mScanList != null) {
                        Log.d(TAG, "onApObsLocDataAvailable, discovery scan");
                        apScanList = new ArrayList<IZatAPScan>();
                        for (APScan apScan: apObs.mScanList) {
                            IZatAPSSIDInfo ssidInfo = null;
                            if(null != apScan.mSSID && apScan.mSSID.length > 0) {
                                ssidInfo =
                                    new IZatAPSSIDInfo(apScan.mSSID, (short)apScan.mSSID.length);
                            }
                            apScanList.add(new IZatAPScan(apScan.mMacAddress,
                                    apScan.mRssi,
                                    apScan.mDeltaTime,
                                    ssidInfo,
                                    apScan.mChannelNumber,
                                    IzatApServiceStatus.values()[apScan.mIsServing],
                                    apScan.mFrequency,
                                    IZatRangingBandWidth.values()[apScan.mBandWidth]));
                        }
                    }

                    if (apObs.mRttScanList != null) {
                        Log.d(TAG, "onApObsLocDataAvailable, rtt scan");
                        apRttScanList = new ArrayList<IZatAPRttScan>();
                        for (APRttScan rttScan: apObs.mRttScanList) {
                            List<IZatRangingMeasurement> rangingMeas =
                                    new ArrayList<IZatRangingMeasurement>();
                            for (APRangingMeasurement measurement: rttScan.mRangingMeasurements) {
                                rangingMeas.add(new IZatRangingMeasurement(
                                        measurement.mDistanceInMm,
                                        measurement.mRssi,
                                        IZatRangingBandWidth.values()[measurement.mTxBandWidth],
                                        IZatRangingBandWidth.values()[measurement.mRxBandWidth],
                                        measurement.mChainNumber));
                            }
                            apRttScanList.add(new IZatAPRttScan(rttScan.mMacAdress,
                                    rttScan.mDeltaTime,
                                    rttScan.mNumAttempted,
                                    rangingMeas));
                        }

                    }
                    IZatCellInfo cellInfo = null;

                    if (!(apObs.mCellInfo.mCellRegionID1 == 0 && apObs.mCellInfo.mCellRegionID2 == 0
                            && apObs.mCellInfo.mCellRegionID3 == 0
                            && apObs.mCellInfo.mCellRegionID4 == 0)) {
                        cellInfo = new IZatCellInfo(apObs.mCellInfo.mCellRegionID1,
                                                    apObs.mCellInfo.mCellRegionID2,
                                                    apObs.mCellInfo.mCellRegionID3,
                                                    apObs.mCellInfo.mCellRegionID4,
                                                    apObs.mCellInfo.mCellType);
                    }
                    if (apScanList != null) {
                        iZatAPObsList.add(new IZatAPObsLocData(apObs.mLocation,
                                                               cellInfo,
                                                               apObs.mScanTimestamp,
                                                               apScanList));
                    }
                    if (apRttScanList != null) {
                        iZatAPObsList.add(new IZatAPObsLocData(apRttScanList,
                                                               apObs.mLocation,
                                                               cellInfo,
                                                               apObs.mScanTimestamp));
                    }
                }

                if (VERBOSE) {
                    logAp(iZatAPObsList, IZatApBsListStatus.values()[apStatus]);
                }
                for(IZatAPObsLocData item : iZatAPObsList) {
                    IZatDataValidation.dataValidate(item);
                }

                mWiFiDBProvImpl.mResponseListener.onApObsLocDataAvailable(iZatAPObsList,
                        IZatApBsListStatus.values()[apStatus]);
            }
        }
        public void onServiceRequest() {
            if (VERBOSE) {
                Log.d(TAG, "onServiceRequest");
            }
            if(null != mWiFiDBProvImpl) {
                mWiFiDBProvImpl.mResponseListener.onServiceRequest();
            }
        }

    }

    private class GnssConfigCallbackWrapper
                  extends IGnssConfigCallback.Stub {
        public boolean mRegisterStatus = false;

        public void getRobustLocationConfigCb(RLConfigData rlConfigData) {
            IZatRobustLocationConfigService.IzatRLConfigInfo izatRLInfo =
                new IZatRobustLocationConfigService.IzatRLConfigInfo();
            izatRLInfo.mValidMask = rlConfigData.validMask;
            izatRLInfo.mEnableStatus = rlConfigData.enableStatus;
            izatRLInfo.mEnableForE911Status = rlConfigData.enableStatusForE911;
            izatRLInfo.major = rlConfigData.major;
            izatRLInfo.minor = rlConfigData.minor;

            synchronized(sRLConfigCallbackMapLock) {
                mRLConfigCallback.getRobustLocationConfigCb(izatRLInfo);
            }
        }

        public void ntnConfigSignalMaskResponse(boolean isSuccess, int gpsSignalTypeConfigMask) {
            synchronized(sntnStatusCallbackLock) {
                if (mntnStatusCallback != null) {
                    mntnStatusCallback.ntnConfigSignalMaskResponse(isSuccess,
                            gpsSignalTypeConfigMask);
                }
            }
        }

        public void ntnConfigSignalMaskChanged(int gpsSignalTypeConfigMask) {
            synchronized(sntnStatusCallbackLock) {
                if (mntnStatusCallback != null) {
                    mntnStatusCallback.ntnConfigSignalMaskChanged(gpsSignalTypeConfigMask);
                }
            }
        }
    }

    private class AltitudeReceiverRespListenerWrapper
            extends IAltitudeReceiverResponseListener.Stub {
        @Override
        public void onAltitudeLookupRequest(Location location, boolean isEmergency)
                throws RemoteException {
            if (VERBOSE) {
                Log.d(TAG, "onAltitudeLookupRequest");
            }
            if(null != mAltitudeRecImpl) {
                if (null != mAltitudeRecImpl.mResponseListener) {
                    mAltitudeRecImpl.mResponseListener
                            .onAltitudeLookupRequest(location, isEmergency);
                }
            }
        }
    }

    private class IZatPrecisePositioningCallbackWrapper extends IPrecisePositionCallback.Stub {
        private final IZatPrecisePositioningService.IZatPrecisePositioningCallback mCallback;

        public IZatPrecisePositioningCallbackWrapper(
                IZatPrecisePositioningService.IZatPrecisePositioningCallback callback) {
            this.mCallback = callback;
        }
        @Override
        public void onLocationAvailable(Location location) {
            mCallback.onLocationAvailable(location);
        }
        @Override
        public void onResponseCallback(int response) {
            mCallback.onResponseCallback(IZatLocationResponse.fromInt(response));
        }
    }

    /* ******************************************************************************** */
    /* Log helper functions                                                             */
    /* ******************************************************************************** */
    private void logAp(List<IZatAPObsLocData> ap_obs_list,
                       IZatApBsListStatus ap_obs_status) {
        Log.v(TAG, "Ap ObsLocData Available size: " + ap_obs_list.size() + " status:" +
                ap_obs_status);

        int counter = 1;
        for (IZatAPObsLocData ap_obs: ap_obs_list) {
            Log.v(TAG, "Entry IZatAPObsLocData num. " + counter + " Location data:");
            log(ap_obs.getLocation());
            Log.v(TAG, "Cell data: " + ap_obs.getCellInfo());
            log(ap_obs.getCellInfo());
            if (ap_obs.getApScanList() != null) {
                Log.v(TAG, "Scan timestamp: " + ap_obs.getScanTimestamp() +
                        "Number of APScan entries: " + ap_obs.getApScanList().size());
                log(ap_obs.getApScanList());
            }
            if (ap_obs.getApRttList() != null) {
                Log.v(TAG, "Scan timestamp: " + ap_obs.getScanTimestamp() +
                        "Number of APScan entries: " + ap_obs.getApRttList().size());
                logRtt(ap_obs.getApRttList());
            }
            counter++;
        }
    }

    private void log(List<IZatAPInfo> ap_list,
                     IZatApBsListStatus ap_status,
                     Location location) {

        Log.v(TAG, "AP List size:" + ap_list.size() + " status: " + ap_status);
        log(location);

        for(IZatWiFiDBReceiver.IZatAPInfo ap_info : ap_list) {
            StringBuilder s = new StringBuilder("AP Info mac: ").append(ap_info.getMacAddress()).
                    append("AP Info timestamp: ").append(ap_info.getTimestamp());
            if (ap_info.isExtraAvailable())
            {
                IZatWiFiDBReceiver.IZatAPInfoExtra ap_extra = ap_info.getExtra();
                if (null != ap_extra.getSSID()) {
                    try {
                        s.append("SSID utf8: ").append(new String(ap_extra.getSSID().mSSID, "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        s.append("SSID utf8: invalid utf8");
                    }
                } else {
                    s.append("SSID utf8: null");
                }
                log(ap_extra.getCellInfo());
            } else {
                s.append("AP info no extra data available.");
            }
            Log.v(TAG, s.toString());
        }
    }

    private void log(Location location) {
        if (null == location) {
            Log.v(TAG, "Location is null");
        } else {
            Log.v(TAG, "Provider: " + location.getProvider() +
                       "Longitude: " + location.getLongitude() +
                       "Latitude: " + location.getLatitude() +
                       "Altitude: " + (location.hasAltitude() ? location.getAltitude() : "EMPTY") +
                       "Accuracy: " + (location.hasAccuracy() ? location.getAccuracy() : "EMPTY") +
                       "loc. Timestamp: " + location.getTime() +
                       "VerticalAccuracy: " + (location.hasVerticalAccuracy() ?
                        location.getVerticalAccuracyMeters() : "EMPTY") );
        }
    }

    private void log(IZatCellInfo cellInfo) {
        if (null == cellInfo) {
            Log.v(TAG, "cellInfo is null");
        } else {
            StringBuilder s = new StringBuilder("RegionID1: ").append(cellInfo.getRegionID1()).
                    append("RegionID2: ").append(cellInfo.getRegionID2()).
                    append("RegionID3: ").append(cellInfo.getRegionID3()).
                    append("RegionID4: ").append(cellInfo.getRegionID4()).
                    append("Type:      ").append(cellInfo.getType());
            try {
                s.append(cellInfo.getCellLocalTimestamp());
            } catch (IZatStaleDataException e) {
                s.append("Timestamp: invalid");
            }

            Log.v(TAG, s.toString());
        }
    }

    private void log(List<IZatAPScan> ap_scan_list) {
        int counter = 1;
        for (IZatAPScan ap_scan: ap_scan_list) {

            StringBuilder s = new StringBuilder("===  Entry IZatAPScan num. ").append(counter).
                    append("Mac address: ").append(ap_scan.getMacAddress()).
                    append("Rssi: ").append(ap_scan.getRssi());
            if (null != ap_scan.getSSID()) {
                try {
                    s.append("SSID utf8: ").append(new String(ap_scan.getSSID().mSSID, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    s.append("SSID utf8: invalid utf8");
                }
            } else {
                s.append("SSID utf8: null");
            }
            s.append("Channel number: ").append(ap_scan.getAPChannelNumber());
            s.append("Delta time:  ").append(ap_scan.getDeltatime());

            Log.v(TAG, s.toString());
            counter++;
        }
    }

    private void logRtt(List<IZatAPRttScan> rttList) {
        int counter = 1;
        for (IZatAPRttScan rttScan: rttList) {
            StringBuilder s = new StringBuilder("===  Entry IZatAPRttScan num. ").append(counter).
                    append(",Mac address: ").append(rttScan.getMacAddress()).
                    append(",deltaTime: ").append(rttScan.getDeltatime()).
                    append(",Num attempted: ").append(rttScan.getNumAttempted());
            Log.v(TAG, s.toString());
            List<IZatRangingMeasurement> measList = rttScan.getApRangingList();
            if (measList != null) {
                for (IZatRangingMeasurement rttMeas : measList) {
                    StringBuilder ss = new StringBuilder("Ranging Measurement:").
                        append(",distanceMm: ").append(rttMeas.mDistanceInMM).
                        append(",rssi: ").append(rttMeas.mRSSI).
                        append(",txBandWidth: ").append(rttMeas.mTxBandWidth).
                        append(",rxBandWidth: ").append(rttMeas.mRxBandWidth).
                        append(",chain Number: ").append(rttMeas.mChainNumber);
                    Log.v(TAG, ss.toString());
                }
            }
        }
    }

/** @endcond */
}
/** @} */  /* end_addtogroup IZatManager */
