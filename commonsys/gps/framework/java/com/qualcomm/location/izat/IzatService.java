/*====*====*====*====*====*====*====*====*====*====*====*====*====*====*====*
  Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
  All rights reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
=============================================================================*/
package com.qualcomm.location.izat;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.app.PendingIntent;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.Binder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.Handler;
import android.util.Log;

import com.qti.gtp.IGTPService;
import com.qualcomm.location.izat.gtp.GtpServiceProvider;
import com.qualcomm.location.izat.flp.FlpServiceProvider;
import com.qti.flp.IFlpService;
import com.qti.flp.ITestService;
import com.qualcomm.location.izat.geofence.GeofenceServiceProvider;
import com.qti.geofence.IGeofenceService;
import com.qualcomm.location.izat.debugreport.DebugReportService;
import com.qti.debugreport.IDebugReportService;
import com.qti.izat.IIzatService;
import com.qti.preciseposition.IPrecisePositionService;
import com.qualcomm.location.izat.preciseposition.PrecisePositionServiceProvider;
import com.qti.wifidbreceiver.IWiFiDBReceiver;
import com.qualcomm.location.izat.wifidbreceiver.WiFiDBReceiver;
import com.qti.gnssconfig.IGnssConfigService;
import com.qualcomm.location.izat.gnssconfig.GnssConfigService;
import com.qti.wifidbprovider.IWiFiDBProvider;
import com.qualcomm.location.izat.wifidbprovider.WiFiDBProvider;
import com.qti.altitudereceiver.IAltitudeReceiver;
import com.qualcomm.location.izat.altitudereceiver.AltitudeReceiver;
import com.qualcomm.location.izat.wwanadreceiver.WWanAdReceiver;
import com.qti.wwanadreceiver.IWWANAdReceiver;
import com.qualcomm.location.utils.IZatServiceContext;
import com.qualcomm.location.LocationService;
import android.content.SharedPreferences;
import java.util.Set;
import java.util.HashSet;
import java.lang.Runnable;
import java.lang.Thread;
import org.json.JSONObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import com.qualcomm.location.policy.SessionPolicyManager;
import android.location.LocationManager;
import android.os.UserHandle;

public class IzatService extends Service {
    private static final String TAG = "IzatService";
    private static final String Service_Version = "12.0.0";
    private static final boolean VERBOSE = Log.isLoggable(TAG, Log.VERBOSE);
    private static final String IZAT_SERVICE_NAME = "com.qualcomm.location.izat.IzatService";
    private static final String ACCESS_FINE_LOCATION =
            android.Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String ACCESS_COARSE_LOCATION =
            Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final String BIND_DEVICE_ADMIN = android.Manifest.permission.BIND_DEVICE_ADMIN;
    private static final String GTPWIFI_PERMISSION = "com.qualcomm.permission.ACCESS_GTPWIFI_API";
    private static final String GTPWIFI_CROWDSOURCING_PERMISSION =
            "com.qualcomm.permission.ACCESS_GTPWIFI_CROWDSOURCING_API";
    private static final String ALTITUDEPROVIDER_PERMISSION =
            "com.qualcomm.permission.ALTITUDE_PROVIDER";
    private static final String USER_CONSENT_PERMISSION =
            "com.qualcomm.permission.ACCESS_USER_CONSENT_API";
    private static final String IZAT_PERMISSION = "com.qualcomm.permission.IZAT";

    private Context mContext;
    private IZatServiceContext mIZatServiceCtx;
    private static Handler sHandler;

    private static final boolean sIsDeviceUnlocked =
            SystemProperties.get("ro.boot.flash.locked").equals("0");

    private SessionPolicyManager mSessionPolicyManager;
    private static IzatServiceShutdownHandler mShutdownHandler;
    public static boolean sIsRunning = false;

    @Override
    public void onCreate() {
        if (VERBOSE) {
            Log.d(TAG, "onCreate");
        }
        mContext = this;
        mIZatServiceCtx = IZatServiceContext.getInstance(mContext.getApplicationContext());
        sHandler = new Handler(mIZatServiceCtx.getLooper());
        mSessionPolicyManager = new SessionPolicyManager(mContext);
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (VERBOSE) {
            Log.d(TAG, "onBind");
        }
        if ("com.qualcomm.location.izat.IzatService".equals(intent.getAction())) {
            if (VERBOSE) {
                Log.d(TAG, "Got a binding request.");
            }
            if (null == mShutdownHandler) {
                mShutdownHandler = new IzatServiceShutdownHandler(
                        IzatServiceShutdownHandler.IZAT_SERVICE_SDK_CLIENT_CONNECTED);
            }
            return mBinder;
        } else {
            return null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SsrHandler.get().clearAllPackages(mContext);
        mSessionPolicyManager.unRegisterIntentReceivers();
        if (mShutdownHandler.getTimerScheduled()) {
            Log.d(TAG, "Removing the Shudown Timer");
            sHandler.removeCallbacksAndMessages(mShutdownHandler.getShutdownTimerToken());
            sHandler = null;
            mIZatServiceCtx.unregisterSystemEventListener(
                    ISystemEventListener.MSG_LOCATION_MODE_CHANGE, mShutdownHandler);
            mShutdownHandler = null;
        }
        Log.d(TAG, "Izat Service is shutting down.");
        sIsRunning = false;
    }

    @Override
    public  int onStartCommand (Intent intent, int flags, int startId) {
        Log.d(TAG, "onStart. intent: " + intent);
        if (intent != null && intent.getBooleanExtra(LocationService.IS_SSR, false)) {
            Log.d(TAG, "onStartCommand with ssr extra field, it is a SSR case");
            SsrHandler.get().setSsrStatus(true, mContext);
            SsrHandler.get().notifyClients(mContext);
        }
        if (null == mShutdownHandler) {
            mShutdownHandler = new IzatServiceShutdownHandler(
                    IzatServiceShutdownHandler.IZAT_SERVICE_LOCATION_SETTINGS_ON);
        }
        //notifyClients both when SSR and first bootup
        SsrHandler.get().notifySsrAndBootup(mContext);
        sIsRunning = true;
        return Service.START_NOT_STICKY;
    }

    /* Remote binder */
    private final IIzatService.Stub mBinder = new IIzatService.Stub() {
        public void registerProcessDeath(final IBinder clientDeathListener) {
            AidlClientDeathNotifier clientNotifier = AidlClientDeathNotifier.getInstance();
            clientNotifier.addAidlClient(mContext, clientDeathListener);
            try {
                clientDeathListener.linkToDeath(new IBinder.DeathRecipient() {
                    @Override
                    public void binderDied() {
                        //Broadcast this event to all modules who registered
                        clientNotifier.broadcastToListeners(clientDeathListener);
                        clientDeathListener.unlinkToDeath (this, 0);
                        clientNotifier.removeAidlClient(clientDeathListener);
                    }
                }, 0);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException: " + e);
            }
        }
        public IFlpService getFlpService() {
            Log.d(TAG, "getFlpService: " + ", Has ACCESS_COARSE_LOCATION permission: " +
                    mContext.checkCallingPermission(ACCESS_COARSE_LOCATION));
            if (mContext.checkCallingPermission(ACCESS_COARSE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED) {
                throw new SecurityException("Requires ACCESS_COARSE_LOCATION permission");
            }
            FlpServiceProvider flpServiceProvider =
                FlpServiceProvider.getInstance(mContext);
            if (null == flpServiceProvider) {
                return null;
            }
            return flpServiceProvider.getFlpBinder();
        }
        public ITestService getTestService() {
            Log.d(TAG, "getTestService: " + ", Has ACCESS_FINE_LOCATION permission: " +
                    mContext.checkCallingPermission(ACCESS_FINE_LOCATION) +
                    ", Has BIND_DEVICE_ADMIN permission: " +
                    mContext.checkCallingPermission(BIND_DEVICE_ADMIN));
            if (mContext.checkCallingPermission(ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED ||
                    mContext.checkCallingPermission(BIND_DEVICE_ADMIN) !=
                    PackageManager.PERMISSION_GRANTED) {
                throw new SecurityException("Requires ACCESS_FINE_LOCATION " +
                        "and BIND_DEVICE_ADMIN permission");
            }
            FlpServiceProvider flpServiceProvider =
                FlpServiceProvider.getInstance(mContext);
            if (null == flpServiceProvider) {
                return null;
            }
            return flpServiceProvider.getFlpTestingBinder();
        }
        public IGeofenceService getGeofenceService() {
            Log.d(TAG, "getGeofenceService: " + "Has ACCESS_FINE_LOCATION permission: " +
                    mContext.checkCallingPermission(ACCESS_FINE_LOCATION));
            if (mContext.checkCallingPermission(ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED) {
                throw new SecurityException("Requires ACCESS_FINE_LOCATION permission");
            }
            GeofenceServiceProvider geofenceServiceProvider =
                GeofenceServiceProvider.getInstance(mContext);
            if (null == geofenceServiceProvider) {
                return null;
            }
            return geofenceServiceProvider.getGeofenceBinder();
        }
        public String getVersion() {
            return Service_Version;
        }
        public IDebugReportService getDebugReportService() {
            Log.d(TAG, "getDebugReportService: " + "Has ACCESS_FINE_LOCATION permission: " +
                    mContext.checkCallingPermission(ACCESS_FINE_LOCATION) +
                    ", Has BIND_DEVICE_ADMIN permission: " +
                    mContext.checkCallingPermission(BIND_DEVICE_ADMIN));
            if (mContext.checkCallingPermission(ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED ||
                    mContext.checkCallingPermission(BIND_DEVICE_ADMIN) !=
                    PackageManager.PERMISSION_GRANTED) {
                throw new SecurityException("Requires ACCESS_FINE_LOCATION " +
                        "and BIND_DEVICE_ADMIN permission");
            }
            DebugReportService debugReportService =
                DebugReportService.getInstance(mContext);
            if (null == debugReportService) {
                return null;
            }
            return debugReportService.getDebugReportBinder();
        }
        public IWiFiDBReceiver getWiFiDBReceiver() {
            Log.d(TAG, "getWiFiDBReceiver: " + "Has GTPWIFI_PERMISSION permission: " +
                    mContext.checkCallingPermission(GTPWIFI_PERMISSION));
            if (mContext.checkCallingPermission(GTPWIFI_PERMISSION) !=
                    PackageManager.PERMISSION_GRANTED) {
                throw new SecurityException("Requires ACCESS_GTPWIFI_API permission");
            }
            WiFiDBReceiver wifiDBReceiver = WiFiDBReceiver.getInstance(mContext);
            return wifiDBReceiver.getWiFiDBReceiverBinder();
        }
        public IGnssConfigService getGnssConfigService() {
            GnssConfigService gnssConfigService =
                    GnssConfigService.getInstance(mContext);
            if (null == gnssConfigService) {
                return null;
            }
            return gnssConfigService.getGnssConfigBinder();
        }
        public IWiFiDBProvider getWiFiDBProvider() {
            Log.d(TAG, "getWiFiDBProvider: " + "Has GTPWIFI_CROWDSOURCING_PERMISSION permission: "
                    + mContext.checkCallingPermission(GTPWIFI_CROWDSOURCING_PERMISSION));
            if (mContext.checkCallingPermission(GTPWIFI_CROWDSOURCING_PERMISSION) !=
                    PackageManager.PERMISSION_GRANTED) {
                throw new SecurityException("Requires ACCESS_GTPWIFI_CROWDSOURCING_API permission");
            }
            WiFiDBProvider wifiDBProvider = WiFiDBProvider.getInstance(mContext);
            return wifiDBProvider.getWiFiDBProviderBinder();
        }
        public IAltitudeReceiver getAltitudeReceiver() {
            Log.d(TAG, "getAltitudeReceiver: " + "Has ACCESS_FINE_LOCATION permission: " +
                    mContext.checkCallingPermission(ACCESS_FINE_LOCATION) +
                    ", Has ALTITUDEPROVIDER_PERMISSION permission: " +
                    mContext.checkCallingPermission(ALTITUDEPROVIDER_PERMISSION));
            if (mContext.checkCallingPermission(ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED ||
                    mContext.checkCallingPermission(ALTITUDEPROVIDER_PERMISSION) !=
                    PackageManager.PERMISSION_GRANTED) {
                throw new SecurityException("Requires ACCESS_FINE_LOCATION " +
                        "and com.qualcomm.permission.ALTITUDE_PROVIDER permission");
            }
            AltitudeReceiver altitudeReceiver = AltitudeReceiver.getInstance(mContext);
            return altitudeReceiver.getAltitudeReceiverBinder();
        }

        @Override
        public IGTPService getGTPService() {
            Log.d(TAG, "getGTPService: " + "Has USER_CONSENT_PERMISSION permission: " +
                    mContext.checkCallingPermission(USER_CONSENT_PERMISSION));
            if (mContext.checkCallingPermission(USER_CONSENT_PERMISSION) !=
                    PackageManager.PERMISSION_GRANTED) {
                throw new SecurityException("Requires ACCESS_USER_CONSENT_API");
            }
            GtpServiceProvider gtpServiceProvider =
                GtpServiceProvider.getInstance(mContext, mSessionPolicyManager);
            int flpFeatureMask = mIZatServiceCtx.getAllSupportedFeatures();
            if ((flpFeatureMask & IZatServiceContext.FEATURE_BIT_QWES_WWAN_STANDARD_IS_SUPPORTED)
                    != 0 || (flpFeatureMask &
                    IZatServiceContext.FEATURE_BIT_QWES_WWAN_PREMIUM_IS_SUPPORTED) != 0) {
                int pid = Binder.getCallingPid();
                int uid = Binder.getCallingUid();
                gtpServiceProvider.setWwanAppInfo(pid, uid);
            }
            return gtpServiceProvider.getGtpBinder();
        }

        @Override
        public IWWANAdReceiver getWWANAdReceiver() {
            Log.d(TAG, "getWWANAdReceiver: " + "Has IZAT permission: " +
                    mContext.checkCallingPermission(IZAT_PERMISSION));
            if (mContext.checkCallingPermission(IZAT_PERMISSION) !=
                    PackageManager.PERMISSION_GRANTED) {
                throw new SecurityException("Requires IZAT permission!");
            }
            WWanAdReceiver recver = WWanAdReceiver.getInstance(mContext);
            return recver.getWWanAdReceiverBinder();
        }

        public IPrecisePositionService getPrecisePositionService() {
            Log.d(TAG, "getPrecisePositionService: " + ", Has ACCESS_FINE_LOCATION permission: " +
                    mContext.checkCallingPermission(ACCESS_FINE_LOCATION));
            if (mContext.checkCallingPermission(ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED) {
                throw new SecurityException("Requires ACCESS_FINE_LOCATION permission");
            }
            PrecisePositionServiceProvider ppServiceProvider =
                PrecisePositionServiceProvider.getInstance(mContext, mSessionPolicyManager);
            if (null == ppServiceProvider) {
                return null;
            }
            return ppServiceProvider.getPrecisePositionBinder();
        }
    };

    public static boolean isDeviceOEMUnlocked(Context ctx) {
        Log.d(TAG, "isDeviceOEMUnlocked " + sIsDeviceUnlocked);
        return sIsDeviceUnlocked;
    }

    /*========================================================
     *  aidl client death notifier
     *======================================================*/

    public static class AidlClientDeathNotifier {

        private class ClientIdentity {
            public String packageName;
            public int pid;
            public int uid;
        }
        private Set<ISystemEventListener> mListeners;
        private HashMap<IBinder, ClientIdentity> mBinders;
        private Object mListenersLock;
        private Object mBindersLock;

        private static final Object mLock = new Object();
        private static AidlClientDeathNotifier mNotifier;

        public static AidlClientDeathNotifier getInstance() {
            synchronized (mLock) {
                if (mNotifier == null) {
                    mNotifier = new AidlClientDeathNotifier();
                }
            }
            return mNotifier;
        }

        private AidlClientDeathNotifier() {
            mListeners = new HashSet<ISystemEventListener>();
            mBinders = new HashMap<IBinder, ClientIdentity>();
            mListenersLock = new Object();
            mBindersLock = new Object();
        }

        private void addAidlClient(Context ctx, IBinder binder) {
            String packageName = ctx.getPackageManager().getNameForUid(Binder.getCallingUid());
            synchronized (mBindersLock) {
                ClientIdentity id = new ClientIdentity();
                id.packageName = packageName;
                id.pid = Binder.getCallingPid();
                id.uid = Binder.getCallingUid();
                mBinders.put(binder, id);
                if (mBinders.size() > 0 && mShutdownHandler != null) {
                    mShutdownHandler.updateIzatShutdowMask(
                        IzatServiceShutdownHandler.IZAT_SERVICE_SDK_CLIENT_CONNECTED, true);
                }
            }
        }

        private void removeAidlClient(IBinder binder) {
            synchronized (mBindersLock) {
                mBinders.remove(binder);
                if (mBinders.size() == 0 && mShutdownHandler != null) {
                    mShutdownHandler.updateIzatShutdowMask(
                        IzatServiceShutdownHandler.IZAT_SERVICE_SDK_CLIENT_CONNECTED, false);
                }
            }
        }

        private void broadcastToListeners(IBinder binder) {
            synchronized(mListenersLock) {
                for (ISystemEventListener listener : mListeners) {
                    ClientIdentity id = mBinders.get(binder);
                    listener.onAidlClientDied(id.packageName, id.pid, id.uid);
                }
            }
        }

        public void registerAidlClientDeathCb(ISystemEventListener listener) {
            synchronized(mListenersLock) {
                mListeners.add(listener);
            }
        }

        public void deRegisterAidClientDeathCb(ISystemEventListener listener) {
            synchronized(mListenersLock) {
                mListeners.remove(listener);
            }
        }
        public HashMap<IBinder, String> getCallingPackage() {
            HashMap<IBinder, String> binderMap = new HashMap<IBinder, String>();
            mBinders.forEach((k, v) -> binderMap.put(k, v.packageName));
            return binderMap;
        }
    }

    /*========================================================
     *  IzatService SSR notifier and handler
     *======================================================*/
    public abstract static class JSONizable {
        public String toJSON() {
            Field[] fields = getClass().getDeclaredFields();
            JSONObject obj = new JSONObject();
            try {
                for (Field field: fields) {
                    if (!"this$0".equals(field.getName())) {
                        Log.v(TAG, "name: " + field.getName() + " value: " + field.get(this));
                        obj.put(field.getName(), field.get(this));
                    }
                }
            } catch (Exception e) {
                Log.v(TAG, "Exception: " + e);
            }
            return obj.toString();
        }

        public void fromJSON(String json_str) {
            Field[] fields = getClass().getDeclaredFields();
            try {
                JSONObject obj = new JSONObject(json_str);
                for (Field field: fields) {
                    if (!"this$0".equals(field.getName())) {
                        field.set(this, obj.get(field.getName()));
                        Log.v(TAG, "name: " + field.getName() + " value: " + field.get(this));
                    }
                }
            } catch (Exception e) {
                Log.v(TAG, "Exception: " + e);
            }
        }
    }

    public interface ISsrNotifier {
        public void bootupAndSsrNotifier(String jsonStr);
    }

    public static class SsrHandler {
        private Set<String> mClientPackages;
        private Set<String> mReconnectedPackages;
        private Object mDataLock;
        private volatile boolean mIsReconnecting = false;
        private volatile boolean mIsNotified = false;

        private static final String SSR_ACTION = "IzatService.restart";
        private static final String CLIENTS_SHARED_PREFS_FILE = "izatclients";
        private static final String SSR_DATA_SHARED_PREFS_FILE = "izatssr_data";
        private static final String IZAT_PACKAGE_KEY = "packages";
        private static final int WAIT_FOR_SERVICE_REBIND_TIME_SEC = 5;
        private static final Object mLock = new Object();
        private static SsrHandler mSsrHandler;

        public static SsrHandler get() {
            synchronized (mLock) {
                if (mSsrHandler == null) {
                    mSsrHandler = new SsrHandler();
                }
            }
            return mSsrHandler;
        }

        private SsrHandler() {
            mClientPackages = new HashSet<String>();
            mReconnectedPackages = new HashSet<String>();
            mDataLock = new Object();
        }

        private SharedPreferences getSharedPref(Context ctx, String filePath) {
            return ctx.createDeviceProtectedStorageContext()
                    .getSharedPreferences(filePath, Context.MODE_PRIVATE);
        }

        private void saveClientNames(Context ctx) {
            SharedPreferences sharedPref = getSharedPref(ctx, CLIENTS_SHARED_PREFS_FILE);
            SharedPreferences.Editor editor = sharedPref.edit();

            if (mClientPackages.size() == 0) {
                editor.clear();
            } else {
                editor.putStringSet(IZAT_PACKAGE_KEY, mClientPackages);
            }
            editor.commit();
        }

        public void clearAllPackages(Context ctx) {
            synchronized (mDataLock) {
                mClientPackages.clear();
                saveClientNames(ctx);
            }
        }

        private void notifyClients(Context ctx) {
            //The logic of SSR handle mechanism is this:
            //1,When connect to SDK service and get its service,
            //  each SDK client who has pendingIntent registered should call
            //  updateClientPackageName(context, pkgName, true) to write its
            //  package name to the sharedpreference;
            //2,When disconnect to SDK service,
            //  each SDK client who has pendingIntent registered should call
            //  updateClientPackageName(context, pkgName, false) to remove its
            //  package name from sharedpreference;
            //3,Once IZatService reboot unexpectly,
            //  3.1 If package list in sharedPref isn't null
            //    3.1.1 Create a new thread to handle SSR asyc;
            //    3.1.2 Wait for clients to re-bind,
            //      3.1.2.1 If client is still alive, it should re-connect in a
            //        quite short time(normally less than 1s), recore its pkg
            //        name in mReconnectedPackages;
            //      3.1.2.2 If client isn't alive, no reconnection will happen;
            //    3.1.3 Timeout, broadcast intent to clients who is in mClientPackages
            //      but not in mReconnectedPackages;
            if (mClientPackages.size() != 0) {
                Log.d(TAG, "onStartCommand - send intent to notify clients izatservice restart");
                sHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            for (int i = 0; i < WAIT_FOR_SERVICE_REBIND_TIME_SEC; ++i) {
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e){
                                }
                                if (mReconnectedPackages.size() == mClientPackages.size()) {
                                    Log.d(TAG, "no client need to be notified");
                                    setSsrStatus(false, ctx);
                                    return;
                                }
                            }
                            synchronized (mDataLock) {
                                for (String packageName : mClientPackages) {
                                    if (!mReconnectedPackages.contains(packageName)) {
                                        Log.d(TAG, "broadcast intent to client: " + packageName);
                                        Intent ssrIntent = new Intent().
                                                setPackage(packageName).setAction(SSR_ACTION);
                                        ctx.sendBroadcast(ssrIntent);
                                    }
                                }
                            }
                            setSsrStatus(false, ctx);
                        }
               });
            } else {
                setSsrStatus(false, ctx);
            }
        }

        private void notifySsrAndBootup(Context ctx) {
            //The logic of notifySsrAndBootup to modules is below:
            //1, Modules who need to handle SSR and boot up need to define their
            //   own type extends JSONizable, and invoke registerDataForSSREvents
            //   to save their own data to sharedpreference when needed, they also
            //   need to implement ISsrNotifier;
            //2, When SSR or boot up, this method will be invoked;
            //   2.1, this method read the data from sharedpreference;
            //   2.2, Retrieve the class name from the keySet ofsharedpreference,
            //        Call bootupAndSsrNotifier to deliver the JSONizable data to
            //        each module;
            //3, Each module will handle this data by themselves in their
            //   bootupAndSsrNotifier method.
            if (mIsNotified) {
                return;
            }
            SharedPreferences sharedPref = getSharedPref(ctx, SSR_DATA_SHARED_PREFS_FILE);
            Map<String, ?> clients = sharedPref.getAll();

            for (String key : clients.keySet()) {
                String jsonStr = (String)clients.get(key);
                try {
                    Class c = Class.forName(key);
                    Method m = c.getDeclaredMethod("getInstance", Context.class);
                    if (m.invoke(c, ctx) instanceof ISsrNotifier) {
                        ((ISsrNotifier)(m.invoke(c, ctx))).bootupAndSsrNotifier(jsonStr);
                    }
                } catch (Exception e) {
                    Log.v(TAG, "Exception: " + e);
                }
            }
            mIsNotified = true;
        }

        private void setSsrStatus(boolean status, Context ctx) {
            mIsReconnecting = status;
            //Restore mClientPackages if it is a SSR case
            if (status) {
                Set<String> clientPackages = getSharedPref(ctx, CLIENTS_SHARED_PREFS_FILE).
                        getStringSet(IZAT_PACKAGE_KEY, new HashSet<String>());
                synchronized (mDataLock) {
                    mClientPackages.clear();
                    mClientPackages.addAll(clientPackages);
                }
            }
        }

        private boolean getSsrStatus() {
            return mIsReconnecting;
        }

        private void updateClientPackageName(Context ctx,
                String packageName, boolean addIfTrueElseRemove) {
            //Put this package to mReconnectedPackages if it is a re-connection case
            if (getSsrStatus()) {
                synchronized (mDataLock) {
                    mReconnectedPackages.add(packageName);
                }
                return;
            }

            synchronized (mDataLock) {
                if (addIfTrueElseRemove != mClientPackages.contains(packageName)) {
                    if (addIfTrueElseRemove) {
                        mClientPackages.add(packageName);
                    } else {
                        mClientPackages.remove(packageName);
                    }
                }
                saveClientNames(ctx);
            }
        }

        public void updateClientPackageName(Context ctx,
                PendingIntent pendingIntent, boolean addIfTrueElseRemove) {
            String packageName = pendingIntent.getCreatorPackage();
            updateClientPackageName(ctx, packageName, addIfTrueElseRemove);
        }

        public void registerDataForSSREvents(Context ctx, String classname, String data) {
            SharedPreferences sharedPref = getSharedPref(ctx, SSR_DATA_SHARED_PREFS_FILE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(classname, data);
            editor.commit();
        }
    }

    public interface ISystemEventListener {
        public static final int MSG_OUTGOING_CALL =                     2;
        public static final int MSG_NET_INITIATED =                     3;
        public static final int MSG_PKG_REMOVED =                       4;
        public static final int MSG_LOCATION_MODE_CHANGE =              5;
        public static final int MSG_USER_SWITCH_ACTION_UPDATE =         6;
        public static final int MSG_UID_IMPORTANCE_CHANGE =             7;

        default void onAidlClientDied(String packageName, int pid, int uid) {}
        default void notify(int msgId, Object... args) {}

    }

    /*========================================================
     *  IzatService shutdown handler
     *======================================================*/
     private class IzatServiceShutdownHandler implements ISystemEventListener {
         private int mIzatServiceShutdownMask;
         private boolean mIsTimerScheduled;
         private static final int IZAT_SERVICE_LOCATION_SETTINGS_ON = 0x1;
         private static final int IZAT_SERVICE_SDK_CLIENT_CONNECTED = 0x2;
         private final Object mShutdownTimerToken = new Object();

         IzatServiceShutdownHandler(int mask) {
             mIzatServiceShutdownMask = mask;
             mIsTimerScheduled = false;
             mIZatServiceCtx.registerSystemEventListener(
                     ISystemEventListener.MSG_LOCATION_MODE_CHANGE, this);
             boolean locationStatus =
                 ((LocationManager)(mContext.getSystemService(Context.LOCATION_SERVICE))).
                 isLocationEnabledForUser(UserHandle.SYSTEM);
             Log.d(TAG, "mask: " + mIzatServiceShutdownMask +
                     ", Izat Service start location status: " + locationStatus);
             updateIzatShutdowMask(IZAT_SERVICE_LOCATION_SETTINGS_ON, locationStatus);
         }

        @Override
        public void notify(int msgId, Object... args) {
            switch (msgId) {
                case MSG_LOCATION_MODE_CHANGE:
                    boolean locationSettingsIsOn = (boolean)args[0];
                    Log.d(TAG, "location settings: " + locationSettingsIsOn);
                    updateIzatShutdowMask(IZAT_SERVICE_LOCATION_SETTINGS_ON,
                            locationSettingsIsOn);
                    break;
                default:
            }
        }

        void updateIzatShutdowMask(int mask, boolean set) {
            Log.d(TAG, "mask: " + mask + " set: " + set +
                    " current shutdown mask: " + mIzatServiceShutdownMask + " TimerScheduled: "
                    + mIsTimerScheduled);
            // to relaunch IzatService when Location Setting toggles ON
            if (mIzatServiceShutdownMask == 0 && mask != 0 && !IzatService.sIsRunning) {
                Intent intentIzatService = new Intent(mContext, IzatService.class);
                intentIzatService.setAction("com.qualcomm.location.izat.IzatService");
                mContext.startServiceAsUser(intentIzatService, UserHandle.SYSTEM);
            }
            synchronized(this) {
                if (set) {
                    mIzatServiceShutdownMask = mIzatServiceShutdownMask | mask;
                } else {
                    mIzatServiceShutdownMask = mIzatServiceShutdownMask & (~mask);
                }
            }
            if (mIzatServiceShutdownMask <= 0) {
                if (!mIsTimerScheduled) {
                    mIsTimerScheduled = true;
                    Log.d(TAG, "Start Izat Service shutdown timer");
                    scheduleShutdownRunnable();
                }
            } else {
                if (mIsTimerScheduled) {
                    Log.d(TAG, "Remove Shutdown timer due updated triggers");
                    sHandler.removeCallbacksAndMessages(mShutdownTimerToken);
                    mIsTimerScheduled = false;
                }
            }
        }

        private void scheduleShutdownRunnable() {
            sHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                    Log.d(TAG, "IzatShutdownMask: " + mIzatServiceShutdownMask);
                    if (mIzatServiceShutdownMask <= 0) {
                    mIsTimerScheduled = false;
                    stopSelf();
                    }
                    }
                    }, mShutdownTimerToken, 300000);
            // 5 mins timer before IzatService can shutdown
        }
        public Object getShutdownTimerToken() { return mShutdownTimerToken;}
        public boolean getTimerScheduled() { return mIsTimerScheduled;}
     }
}
