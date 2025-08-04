/*====*====*====*====*====*====*====*====*====*====*====*====*====*====*====*
  Copyright (c) 2017, 2020-2022, 2024 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
=============================================================================*/
package com.qualcomm.location.izat.debugreport;

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;
import android.os.Handler;
import android.os.Message;

import com.qti.debugreport.*;
import com.qualcomm.location.idlclient.IDLClientUtils;
import com.qualcomm.location.izat.CallbackData;
import com.qualcomm.location.izat.DataPerPackageAndUser;
import com.qualcomm.location.izat.IzatService;
import com.qualcomm.location.utils.IZatServiceContext;
import java.lang.IndexOutOfBoundsException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.qualcomm.location.idlclient.*;
import com.qualcomm.location.idlclient.LocIDLClientBase.*;
import vendor.qti.gnss.ILocAidlGnss;
import vendor.qti.gnss.ILocAidlDebugReportService;
import vendor.qti.gnss.ILocAidlDebugReportServiceCallback;
import vendor.qti.gnss.LocAidlXtraStatus;
import vendor.qti.gnss.LocAidlSystemStatusReports;
import vendor.qti.gnss.LocAidlSystemStatusXoState;
import vendor.qti.gnss.LocAidlSystemStatusBestPosition;
import vendor.qti.gnss.LocAidlSystemStatusErrRecovery;
import vendor.qti.gnss.LocAidlSystemStatusSvHealth;
import vendor.qti.gnss.LocAidlSystemStatusEphemeris;
import vendor.qti.gnss.LocAidlSystemStatusPdr;
import vendor.qti.gnss.LocAidlSystemStatusPositionFailure;
import vendor.qti.gnss.LocAidlSystemStatusXtra;
import vendor.qti.gnss.LocAidlSystemStatusTimeAndClock;
import vendor.qti.gnss.LocAidlSystemStatusInjectedPosition;
import vendor.qti.gnss.LocAidlSystemStatusRfAndParams;

public class DebugReportService implements IzatService.ISystemEventListener, Handler.Callback {
    private static final String TAG = "DebugReportService";
    private static final boolean VERBOSE = Log.isLoggable(TAG, Log.VERBOSE);

    private static final Object sCallBacksLock = new Object();
    private static final Object sLock = new Object();
    private RemoteCallbackList<IDebugReportCallback> mDebugReportCallbacks
            = new RemoteCallbackList<IDebugReportCallback>();
    private RemoteCallbackList<IXtraStatusCallback> mXtraStatusCallbacks
            = new RemoteCallbackList<IXtraStatusCallback>();
    private final Context mContext;
    private Handler mHandler;
    Timer mDebugReportTimer;

    private DebugReportServiceIDLClient mDebugReportServiceIDLClient;

    private class ClientDebugReportData extends CallbackData {
        private IDebugReportCallback mCallback;
        private boolean mReportPeriodic;

        public ClientDebugReportData(IDebugReportCallback callback) {
            mCallback = callback;
            super.mCallback = callback;
            mReportPeriodic = false;
        }
    }

    private class ClientXtraStatusData extends CallbackData {
        private IXtraStatusCallback mCallback;

        public ClientXtraStatusData(IXtraStatusCallback callback) {
            mCallback = callback;
            super.mCallback = callback;
        }
    }
    private DataPerPackageAndUser<ClientDebugReportData> mDataPerPackageAndUser;
    private DataPerPackageAndUser<ClientXtraStatusData> mXtraDataPerPackageAndUser;

    // DebugReport Data classes
    ArrayList<IZatEphmerisDebugReport> mListOfEphmerisReports =
            new ArrayList<IZatEphmerisDebugReport>();
    ArrayList<IZatFixStatusDebugReport> mListOfFixStatusReports =
            new ArrayList<IZatFixStatusDebugReport>();
    ArrayList<IZatLocationReport> mListOfBestLocationReports = new ArrayList<IZatLocationReport>();
    ArrayList<IZatLocationReport> mListOfEPIReports = new ArrayList<IZatLocationReport>();
    ArrayList<IZatGpsTimeDebugReport> mListGpsTimeOfReports =
            new ArrayList<IZatGpsTimeDebugReport>();
    ArrayList<IZatXoStateDebugReport> mListXoStateOfReports =
            new ArrayList<IZatXoStateDebugReport>();
    ArrayList<IZatRfStateDebugReport> mListRfStateOfReports =
            new ArrayList<IZatRfStateDebugReport>();
    ArrayList<IZatErrorRecoveryReport> mListOfErrorRecoveries =
            new ArrayList<IZatErrorRecoveryReport>();
    ArrayList<IZatPDRDebugReport> mListOfPDRReports = new ArrayList<IZatPDRDebugReport>();
    ArrayList<IZatSVHealthDebugReport> mListOfSVHealthReports =
            new ArrayList<IZatSVHealthDebugReport>();
    ArrayList<IZatXTRADebugReport> mListOfXTRAReports = new ArrayList<IZatXTRADebugReport>();

    private static DebugReportService sInstance = null;
    public static DebugReportService getInstance(Context ctx) {
        synchronized (sLock) {
            if (sInstance == null) {
                sInstance = new DebugReportService(ctx);
            }
        }
        return sInstance;
    }

    private DebugReportService(Context ctx) {
        if (VERBOSE) {
            Log.d(TAG, "DebugReportService construction");
        }

        mContext = ctx;
        if (LocIDLClientBase.getIDLServiceVersion().compareTo(IDLServiceVersion.V_AIDL) >= 0) {
            mDebugReportServiceIDLClient = new DebugReportServiceIDLClient(this);
            mHandler = new Handler(IZatServiceContext.getInstance(mContext).getLooper(), this);
            mDataPerPackageAndUser = new DataPerPackageAndUser<ClientDebugReportData>(mContext,
                    new UserChanged());
            mXtraDataPerPackageAndUser = new DataPerPackageAndUser<ClientXtraStatusData>(mContext,
                    new XtraUserChanged());
            IzatService.AidlClientDeathNotifier.getInstance().registerAidlClientDeathCb(this);
        } else {
            Log.e(TAG, "ILoc AIDL is not supported!");
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        return true;
    }

    @Override
    public void onAidlClientDied(String packageName, int pid, int uid) {
        Log.d(TAG, "aidl client crash: " + packageName);
        synchronized (sCallBacksLock) {
            ClientDebugReportData clData =
                    mDataPerPackageAndUser.getDataByPkgName(packageName);

            if (null != clData) {
                if (VERBOSE) {
                    Log.d(TAG, "Package died: " + clData.mPackageName);
                }
                mDataPerPackageAndUser.removeData(clData);
                mDebugReportCallbacks.unregister(clData.mCallback);
            }
            checkOnPeriodicReporting();

            ClientXtraStatusData clXtraData =
                    mXtraDataPerPackageAndUser.getDataByPkgName(packageName);
            if (null != clXtraData) {
                if (VERBOSE) {
                    Log.d(TAG, "Package died: " + clXtraData.mPackageName);
                }
                mXtraDataPerPackageAndUser.removeData(clXtraData);
                mXtraStatusCallbacks.unregister(clXtraData.mCallback);
                clXtraData.mCallback = null;
            }
        }
    }

    public void reportXtraStatusUpdate(IZatXTRAStatus xtraStatus) {
        for (ClientXtraStatusData clData : mXtraDataPerPackageAndUser.getAllData()) {
            if (VERBOSE) {
                Log.d(TAG, "Sending XtraStatus to " + clData.mPackageName);
            }
            try {
                if (clData.mCallback != null) {
                    clData.mCallback.onXtraStatusChanged(xtraStatus);
                } else {
                    Log.e(TAG, "IXtraStatusCallback is null for " + clData.mPackageName);
                }
            } catch (RemoteException e) {
                // do nothing
            }
        }
    }
    /* Remote binder */
    private final IDebugReportService.Stub mBinder = new IDebugReportService.Stub() {

        public void registerForDebugReporting(final IDebugReportCallback callback) {
            if (callback == null) {
                Log.e(TAG, "callback is null");
                return;
            }

            synchronized (sCallBacksLock) {
                if (VERBOSE) {
                    Log.d(TAG, "registerForDebugReporting: " +
                            mDataPerPackageAndUser.getPackageName(null));
                }


                ClientDebugReportData clData = mDataPerPackageAndUser.getData();
                if (null == clData) {
                    clData = new ClientDebugReportData(callback);
                    mDataPerPackageAndUser.setData(clData);
                } else {
                    if (null != clData.mCallback) {
                        mDebugReportCallbacks.unregister(clData.mCallback);
                    }
                    clData.mCallback = callback;
                }

                mDebugReportCallbacks.register(callback);
            }
        }

        public void unregisterForDebugReporting(IDebugReportCallback callback) {
            if (callback == null) {
                Log.e(TAG, "callback is null");
                return;
            }

            synchronized (sCallBacksLock) {
                 if (VERBOSE) {
                    Log.d(TAG, "unregisterForDebugReporting: " +
                            mDataPerPackageAndUser.getPackageName(null));
                }

                mDataPerPackageAndUser.removeData(mDataPerPackageAndUser.getData());
                mDebugReportCallbacks.unregister(callback);
                checkOnPeriodicReporting();
            }
        }

        public Bundle getDebugReport() {
            if (VERBOSE) {
                Log.d(TAG, "getDebugReport JAVA: " + mDataPerPackageAndUser.getPackageName(null));
            }

            synchronized(sCallBacksLock) {
                mListOfEphmerisReports.clear();
                mListOfFixStatusReports.clear();
                mListOfEPIReports.clear();
                mListOfBestLocationReports.clear();
                mListGpsTimeOfReports.clear();
                mListXoStateOfReports.clear();
                mListRfStateOfReports.clear();
                mListOfErrorRecoveries.clear();
                mListOfPDRReports.clear();
                mListOfSVHealthReports.clear();
                mListOfXTRAReports.clear();

                if (null != mDebugReportServiceIDLClient) {
                    mDebugReportServiceIDLClient.getReport(30, mListOfEphmerisReports,
                                              mListOfFixStatusReports,
                                              mListOfEPIReports,
                                              mListOfBestLocationReports,
                                              mListGpsTimeOfReports,
                                              mListXoStateOfReports,
                                              mListRfStateOfReports,
                                              mListOfErrorRecoveries,
                                              mListOfPDRReports,
                                              mListOfSVHealthReports,
                                              mListOfXTRAReports);
                }

                Bundle bundleDebugReportObj = new Bundle();
                bundleDebugReportObj.putParcelableArrayList(
                        IZatDebugConstants.IZAT_DEBUG_EPH_STATUS_KEY,
                        mListOfEphmerisReports);
                bundleDebugReportObj.putParcelableArrayList(
                        IZatDebugConstants.IZAT_DEBUG_FIX_STATUS_KEY,
                        mListOfFixStatusReports);
                bundleDebugReportObj.putParcelableArrayList(
                        IZatDebugConstants.IZAT_DEBUG_EXTERNAL_POSITION_INJECTION_KEY,
                        mListOfEPIReports);
                bundleDebugReportObj.putParcelableArrayList(
                        IZatDebugConstants.IZAT_DEBUG_BEST_POSITION_KEY,
                        mListOfBestLocationReports);
                bundleDebugReportObj.putParcelableArrayList(
                        IZatDebugConstants.IZAT_DEBUG_GPS_TIME_KEY,
                        mListGpsTimeOfReports);
                bundleDebugReportObj.putParcelableArrayList(
                        IZatDebugConstants.IZAT_DEBUG_RF_STATE_KEY,
                        mListRfStateOfReports);
                bundleDebugReportObj.putParcelableArrayList(
                        IZatDebugConstants.IZAT_DEBUG_XO_STATE_KEY,
                        mListXoStateOfReports);
                bundleDebugReportObj.putParcelableArrayList(
                        IZatDebugConstants.IZAT_DEBUG_LAST_ERROR_RECOVERIES_KEY,
                        mListOfErrorRecoveries);
                bundleDebugReportObj.putParcelableArrayList(
                        IZatDebugConstants.IZAT_DEBUG_PDR_INFO_KEY,
                        mListOfPDRReports);
                bundleDebugReportObj.putParcelableArrayList(
                        IZatDebugConstants.IZAT_DEBUG_SV_HEALTH_KEY,
                        mListOfSVHealthReports);
                bundleDebugReportObj.putParcelableArrayList(
                        IZatDebugConstants.IZAT_DEBUG_XTRA_STATUS_KEY,
                        mListOfXTRAReports);
                return bundleDebugReportObj;
            }
        }

        public void startReporting() {
            if (VERBOSE) {
                Log.d(TAG, "Request to start periodic reporting by package:"
                           + mDataPerPackageAndUser.getPackageName(null) );
            }

            // update ClientGeofenceData for this package
            synchronized(sCallBacksLock) {
                ClientDebugReportData clData = mDataPerPackageAndUser.getData();
                if (null != clData) {
                    clData.mReportPeriodic = true;
                }
            }

            mHandler.post(()-> {
                if (mDebugReportTimer != null) {
                    if (VERBOSE) {
                        Log.d(TAG, "Periodic reporting already in progress");
                    }
                    return;
                }

                mDebugReportTimer = new Timer();
                mDebugReportTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        synchronized (sCallBacksLock) {
                            mListOfEphmerisReports.clear();
                            mListOfFixStatusReports.clear();
                            mListOfEPIReports.clear();
                            mListOfBestLocationReports.clear();
                            mListGpsTimeOfReports.clear();
                            mListXoStateOfReports.clear();
                            mListRfStateOfReports.clear();
                            mListOfErrorRecoveries.clear();
                            mListOfPDRReports.clear();
                            mListOfSVHealthReports.clear();
                            mListOfXTRAReports.clear();

                            if (null != mDebugReportServiceIDLClient) {
                                mDebugReportServiceIDLClient.getReport(1, mListOfEphmerisReports,
                                                         mListOfFixStatusReports,
                                                         mListOfEPIReports,
                                                         mListOfBestLocationReports,
                                                         mListGpsTimeOfReports,
                                                         mListXoStateOfReports,
                                                         mListRfStateOfReports,
                                                         mListOfErrorRecoveries,
                                                         mListOfPDRReports,
                                                         mListOfSVHealthReports,
                                                         mListOfXTRAReports);
                            }

                            if (mListOfEphmerisReports.isEmpty() &&
                                    mListOfFixStatusReports.isEmpty() &&
                                    mListOfEPIReports.isEmpty() &&
                                    mListOfBestLocationReports.isEmpty() &&
                                    mListGpsTimeOfReports.isEmpty() &&
                                    mListXoStateOfReports.isEmpty() &&
                                    mListRfStateOfReports.isEmpty() &&
                                    mListOfErrorRecoveries.isEmpty() &&
                                    mListOfPDRReports.isEmpty() &&
                                    mListOfSVHealthReports.isEmpty() &&
                                    mListOfXTRAReports.isEmpty()) {
                                if (VERBOSE) {
                                    Log.d(TAG, "Empty debug report");
                                }
                                return;
                            }

                            Bundle bundleDebugReportObj = new Bundle();

                            try {
                                bundleDebugReportObj.putParcelable(
                                        IZatDebugConstants.IZAT_DEBUG_EPH_STATUS_KEY,
                                        mListOfEphmerisReports.get(0) );
                            } catch (IndexOutOfBoundsException ioobe) {}

                            try {
                                bundleDebugReportObj.putParcelable(
                                        IZatDebugConstants.IZAT_DEBUG_FIX_STATUS_KEY,
                                        mListOfFixStatusReports.get(0));
                            } catch (IndexOutOfBoundsException ioobe) {}

                            try {
                                bundleDebugReportObj.putParcelable(
                                        IZatDebugConstants
                                            .IZAT_DEBUG_EXTERNAL_POSITION_INJECTION_KEY,
                                        mListOfEPIReports.get(0));
                            } catch (IndexOutOfBoundsException ioobe) {}

                            try {
                                bundleDebugReportObj.putParcelable(
                                        IZatDebugConstants.IZAT_DEBUG_BEST_POSITION_KEY,
                                        mListOfBestLocationReports.get(0));
                            } catch (IndexOutOfBoundsException ioobe) {}

                            try {
                                bundleDebugReportObj.putParcelable(
                                        IZatDebugConstants.IZAT_DEBUG_GPS_TIME_KEY,
                                        mListGpsTimeOfReports.get(0));
                            } catch (IndexOutOfBoundsException ioobe) {}

                            try {
                                bundleDebugReportObj.putParcelable(
                                        IZatDebugConstants.IZAT_DEBUG_RF_STATE_KEY,
                                        mListRfStateOfReports.get(0));
                            } catch (IndexOutOfBoundsException ioobe) {}

                            try {
                                bundleDebugReportObj.putParcelable(
                                        IZatDebugConstants.IZAT_DEBUG_XO_STATE_KEY,
                                        mListXoStateOfReports.get(0));
                            } catch (IndexOutOfBoundsException ioobe) {}

                            try {
                                bundleDebugReportObj.putParcelable(
                                        IZatDebugConstants.IZAT_DEBUG_LAST_ERROR_RECOVERIES_KEY,
                                        mListOfErrorRecoveries.get(0));
                            } catch (IndexOutOfBoundsException ioobe) {}

                            try {
                                bundleDebugReportObj.putParcelable(
                                        IZatDebugConstants.IZAT_DEBUG_PDR_INFO_KEY,
                                        mListOfPDRReports.get(0));
                            } catch (IndexOutOfBoundsException ioobe) {}

                            try {
                                bundleDebugReportObj.putParcelable(
                                        IZatDebugConstants.IZAT_DEBUG_SV_HEALTH_KEY,
                                        mListOfSVHealthReports.get(0));
                            } catch (IndexOutOfBoundsException ioobe) {}

                            try {
                                bundleDebugReportObj.putParcelable(
                                        IZatDebugConstants.IZAT_DEBUG_XTRA_STATUS_KEY,
                                        mListOfXTRAReports.get(0));
                            } catch (IndexOutOfBoundsException ioobe) {}

                            String ownerPackage = null;
                            for (ClientDebugReportData clData :
                                    mDataPerPackageAndUser.getAllData()) {
                                if (true == clData.mReportPeriodic) {
                                    if (VERBOSE) {
                                        Log.d(TAG, "Sending report to " + clData.mPackageName);
                                    }

                                    try {
                                        clData.mCallback.onDebugDataAvailable(
                                                bundleDebugReportObj);
                                    } catch (RemoteException e) {
                                                 // do nothing
                                    }
                                }
                            }
                        }
                    }}, 0, 1000);
                });
            }

        public void stopReporting() {
            if (VERBOSE) {
                Log.d(TAG, "Request to stop periodic reporting by package:"
                           + mDataPerPackageAndUser.getPackageName(null));
            }

            // update ClientGeofenceData for this package
            synchronized (sCallBacksLock) {
                ClientDebugReportData clData = mDataPerPackageAndUser.getData();
                if (null != clData) {
                    clData.mReportPeriodic = false;
                }

                checkOnPeriodicReporting();
            }
        }
        public void registerXtraStatusListener(IXtraStatusCallback callback) {
            if (callback == null) {
                Log.e(TAG, "callback is null");
                return;
            }
                 if (VERBOSE) {
                    Log.d(TAG, "registerXtraStatusListener: " +
                            mXtraDataPerPackageAndUser.getPackageName(null));
                }
            synchronized (sCallBacksLock) {
                ClientXtraStatusData clData = mXtraDataPerPackageAndUser.getData();
                if (null == clData) {
                    clData = new ClientXtraStatusData(callback);
                    mXtraDataPerPackageAndUser.setData(clData);
                } else {
                    if (null != clData.mCallback) {
                        mXtraStatusCallbacks.unregister(clData.mCallback);
                    }
                    clData.mCallback = callback;
                }
                mXtraStatusCallbacks.register(callback);
            }
            if (null != mDebugReportServiceIDLClient) {
                mDebugReportServiceIDLClient.registerXtraStatusListener();
            }
        }

        public void unregisterXtraStatusListener() {
            if (null != mDebugReportServiceIDLClient) {
                mDebugReportServiceIDLClient.unregisterXtraStatusListener();
            }
        }

        public void getXtraStatus() {
            synchronized (sCallBacksLock) {
                ClientXtraStatusData clData = mXtraDataPerPackageAndUser.getData();
                if (null == clData || null == clData.mCallback) {
                    Log.e(TAG, "IXtraStatusCallback not registered for: " +
                            mXtraDataPerPackageAndUser.getPackageName(null));
                    return;
                }
            }
            if (null != mDebugReportServiceIDLClient) {
                mDebugReportServiceIDLClient.getXtraStatus();
            }
        }
    };

    private void checkOnPeriodicReporting() {
        boolean continuePeriodicReporting = false;

        for (ClientDebugReportData clData : mDataPerPackageAndUser.getAllData()) {
            if (clData.mReportPeriodic == true) {
                continuePeriodicReporting = true;
                break;
            }
        }

        if (continuePeriodicReporting == false) {
            if (VERBOSE) {
                Log.d(TAG, "Service is stopping periodic debug reports");
            }

            mHandler.post(()-> {
                if (mDebugReportTimer != null) {
                    mDebugReportTimer.cancel();
                    mDebugReportTimer = null;
                } else {
                    if (VERBOSE) {
                        Log.d(TAG, "No peridoc reporting in progress !!");
                    }
                }
            });
        }
    }

    class UserChanged implements DataPerPackageAndUser.UserChangeListener<ClientDebugReportData> {
        @Override
        public void onUserChange(Map<String, ClientDebugReportData> prevUserData,
                                 Map<String, ClientDebugReportData> currentUserData) {
            if (VERBOSE) {
                Log.d(TAG, "Active user has changed, updating debugReport callbacks...");
            }

            synchronized (sCallBacksLock) {
                // Remove prevUser callbacks
                for (ClientDebugReportData debugReportDataData: prevUserData.values()) {
                    mDebugReportCallbacks.unregister(debugReportDataData.mCallback);
                }

                // Add back current user callbacks
                for (ClientDebugReportData debugReportDataData: currentUserData.values()) {
                    mDebugReportCallbacks.register(debugReportDataData.mCallback);
                }

                checkOnPeriodicReporting();
            }
        }
    }
    class XtraUserChanged implements DataPerPackageAndUser.UserChangeListener<ClientXtraStatusData> {
        @Override
        public void onUserChange(Map<String, ClientXtraStatusData> prevUserData,
                                 Map<String, ClientXtraStatusData> currentUserData) {
            if (VERBOSE) {
                Log.d(TAG, "Active user has changed, updating xtraStatus callbacks...");
            }

            synchronized (sCallBacksLock) {
                // Remove prevUser callbacks
                for (ClientXtraStatusData xtraStatusDataData: prevUserData.values()) {
                    mXtraStatusCallbacks.unregister(xtraStatusDataData.mCallback);
                }

                // Add back current user callbacks
                for (ClientXtraStatusData xtraStatusDataData: currentUserData.values()) {
                    mXtraStatusCallbacks.register(xtraStatusDataData.mCallback);
                }

            }
        }
    }

    public IDebugReportService getDebugReportBinder() {
        return mBinder;
    }

    private class DebugReportServiceIDLClient extends LocIDLClientBase
            implements LocIDLClientBase.IServiceDeathCb {

        private static final String TAG = "DebugReportServiceIDLClient";
        public final int HAS_HORIZONTAL_COMPONENT = 1;
        public final int HAS_VERTICAL_COMPONENT = 2;
        public final int HAS_SOURCE = 4;
        private ILocAidlDebugReportService sDebugService;
        private DebugReportServiceCallback mXtraStatusCb;

        private boolean sInstanceStarted = false;


        void getDebugReportIface() {
            if (null == sDebugService) {
                ILocAidlGnss service = getGnssAidlService();
                if (null != service) {
                    try {
                        sDebugService = service.getExtensionLocAidlDebugReportService();
                        Log.d(TAG, "getDebugReportIface sDebugService=" + sDebugService);
                    } catch (RemoteException e) {
                        Log.e(TAG, "Exception getting debug report extension: " + e);
                    }
                } else {
                    Log.e(TAG, "Debug report service is null!");
                }
            }
        }

        private DebugReportServiceIDLClient(DebugReportService provider) {
            getDebugReportIface();
            if (null != sDebugService) {
                try {
                    Log.d(TAG, "DebugReportServiceIDLClient aidl init");
                    mXtraStatusCb = new DebugReportServiceCallback(provider);
                    sDebugService.init();
                    registerServiceDiedCb(this);
                } catch (RemoteException e) {
                    Log.e(TAG, "Exception getting debug report extension: " + e);
                }
            }
        }

        @Override
        public void onServiceDied() {
            sDebugService = null;
            getDebugReportIface();
            if (null != sDebugService) {
                try {
                    Log.d(TAG, "DebugReportServiceIDLClient init");
                    sDebugService.init();
                } catch (RemoteException e) {
                    Log.e(TAG, "Exception getting debug report extension: " + e);
                }
            }
        }
        public void registerXtraStatusListener() {
            vendor.qti.gnss.ILocAidlDebugReportService iface = sDebugService;

            if (null != iface) {
                Log.d(TAG, "registerXtraStatusListener ...");
                try {
                    iface.registerXtraStatusListener(mXtraStatusCb);
                } catch (RemoteException e) {
                    Log.e(TAG, "Exception when registerXtraStatusListener(): " + e);
                }
            }
        }

        public void unregisterXtraStatusListener() {
            vendor.qti.gnss.ILocAidlDebugReportService iface = sDebugService;

            if (null != iface) {
                Log.d(TAG, "unregisterXtraStatusListener ...");
                try {
                    iface.unregisterXtraStatusListener();
                } catch (RemoteException e) {
                    Log.e(TAG, "Exception when unregisterXtraStatusListener(): " + e);
                }
            }
        }
        public void getXtraStatus() {
            vendor.qti.gnss.ILocAidlDebugReportService iface = sDebugService;

            if (null != iface) {
                Log.d(TAG, "getXtraStatus ...");
                try {
                    iface.getXtraStatus();
                } catch (RemoteException e) {
                    Log.e(TAG, "Exception when getXtraStatus(): " + e);
                }
            }
        }
        public void getReport(int maxReports,
                ArrayList<IZatEphmerisDebugReport> ephmerisReports,
                ArrayList<IZatFixStatusDebugReport> fixStatusReports,
                ArrayList<IZatLocationReport> epiReports,
                ArrayList<IZatLocationReport> bestLocationReports,
                ArrayList<IZatGpsTimeDebugReport> gpsTimeReports,
                ArrayList<IZatXoStateDebugReport> xoStateReports,
                ArrayList<IZatRfStateDebugReport> rfStateReports,
                ArrayList<IZatErrorRecoveryReport> errorRecoveries,
                ArrayList<IZatPDRDebugReport> pdrReports,
                ArrayList<IZatSVHealthDebugReport> svHealthReports,
                ArrayList<IZatXTRADebugReport> xtraReports) {

            vendor.qti.gnss.ILocAidlDebugReportService iface = sDebugService;

            if (null != iface) {
                Log.d(TAG, "getReport ...");
                try {
                    LocAidlSystemStatusReports reports = iface.getReport(maxReports);

                    Log.d(TAG, "getReport success: " + reports.mSuccess);
                    populateV1_0Reports(reports, ephmerisReports, fixStatusReports,
                            epiReports, bestLocationReports, gpsTimeReports, xoStateReports,
                            rfStateReports, errorRecoveries, pdrReports, svHealthReports,
                            xtraReports);

                    for (LocAidlSystemStatusRfAndParams status: reports.mRfAndParamsVec) {
                        IZatRfStateDebugReport rfReport = new IZatRfStateDebugReport(
                                new IZatUtcSpec(status.mUtcTime.tv_sec,
                                        status.mUtcTime.tv_nsec),
                                new IZatUtcSpec(status.mUtcReported.tv_sec,
                                        status.mUtcReported.tv_nsec),
                                status.mPgaGain,
                                status.mAdcI,
                                status.mAdcQ,
                                status.mJammerGps, status.mJammerGlo,
                                status.mJammerBds, status.mJammerGal,
                                status.mGpsBpAmpI, status.mGpsBpAmpQ,
                                status.mGloBpAmpI, status.mGloBpAmpQ,
                                status.mBdsBpAmpI, status.mBdsBpAmpQ,
                                status.mGalBpAmpI, status.mGalBpAmpQ,
                                status.mJammedSignalsMask, status.mJammerInd,
                                status.mAgc);
                        rfStateReports.add(rfReport);
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, "Exception getting report: " + e);
                }
            }
        }

        private void populateV1_0Reports(LocAidlSystemStatusReports reports,
                ArrayList<IZatEphmerisDebugReport> ephmerisReports,
                ArrayList<IZatFixStatusDebugReport> fixStatusReports,
                ArrayList<IZatLocationReport> epiReports,
                ArrayList<IZatLocationReport> bestLocationReports,
                ArrayList<IZatGpsTimeDebugReport> gpsTimeReports,
                ArrayList<IZatXoStateDebugReport> xoStateReports,
                ArrayList<IZatRfStateDebugReport> rfStateReports,
                ArrayList<IZatErrorRecoveryReport> errorRecoveries,
                ArrayList<IZatPDRDebugReport> pdrReports,
                ArrayList<IZatSVHealthDebugReport> svHealthReports,
                ArrayList<IZatXTRADebugReport> xtraReports) {
            Log.d(TAG, "getReport 1.0 success: " + reports.mSuccess);

            populateXtraReportList(xtraReports, reports.mXtraVec);
            populateGpsTimeReportList(gpsTimeReports, reports.mTimeAndClockVec);
            populateXoStateReports(xoStateReports, reports.mXoStateVec);
            populateErrRecoveryReports(errorRecoveries, reports.mErrRecoveryVec);
            populateInjectedPositionReports(epiReports, reports.mInjectedPositionVec);
            populateBestPositionReports(bestLocationReports, reports.mBestPositionVec);
            populateEphemerisReports(ephmerisReports, reports.mEphemerisVec);
            populateSvHealthReports(svHealthReports, reports.mSvHealthVec);
            populatePdrReports(pdrReports, reports.mPdrVec);
            populatePositionFailureReports(fixStatusReports, reports.mPositionFailureVec);
        }

        private void populateXtraReportList(ArrayList<IZatXTRADebugReport> xtraReports,
                LocAidlSystemStatusXtra[] aidlXtraReports ) {
            for (LocAidlSystemStatusXtra status: aidlXtraReports) {
                IZatXTRADebugReport xtraReport = new  IZatXTRADebugReport(
                        new IZatUtcSpec(status.mUtcTime.tv_sec, status.mUtcTime.tv_nsec),
                        new IZatUtcSpec(status.mUtcReported.tv_sec, status.mUtcReported.tv_nsec),
                        status.mXtraValidMask, status.mGpsXtraValid, status.mGpsXtraAge,
                        status.mGloXtraValid, status.mGloXtraAge,
                        status.mBdsXtraValid, status.mBdsXtraAge,
                        status.mGalXtraValid, status.mGalXtraAge,
                        status.mQzssXtraValid, status.mQzssXtraAge);

                xtraReports.add(xtraReport);
            }
        }

        private void populateGpsTimeReportList(ArrayList<IZatGpsTimeDebugReport> izatReports,
                LocAidlSystemStatusTimeAndClock[] aidlReports) {
            for (LocAidlSystemStatusTimeAndClock aidlReport:
                    aidlReports) {
                IZatGpsTimeDebugReport izatReport = new  IZatGpsTimeDebugReport(
                        new IZatUtcSpec(aidlReport.mUtcTime.tv_sec, aidlReport.mUtcTime.tv_nsec),
                        new IZatUtcSpec(aidlReport.mUtcReported.tv_sec,
                                        aidlReport.mUtcReported.tv_nsec),
                        aidlReport.mGpsWeek,
                        aidlReport.mGpsTowMs,
                        aidlReport.mTimeValid == 1,
                        aidlReport.mTimeSource,
                        aidlReport.mTimeUnc,
                        aidlReport.mClockFreqBias,
                        aidlReport.mClockFreqBiasUnc,
                        aidlReport.mLeapSeconds,
                        aidlReport.mLeapSecUnc);

                izatReports.add(izatReport);
            }
        }

        private void populateXoStateReports(ArrayList<IZatXoStateDebugReport> izatReports,
                LocAidlSystemStatusXoState[] aidlReports) {
            for (LocAidlSystemStatusXoState aidlReport: aidlReports) {
                IZatXoStateDebugReport izatReport = new  IZatXoStateDebugReport(
                        new IZatUtcSpec(aidlReport.mUtcTime.tv_sec, aidlReport.mUtcTime.tv_nsec),
                        new IZatUtcSpec(aidlReport.mUtcReported.tv_sec,
                                        aidlReport.mUtcReported.tv_nsec),
                        aidlReport.mXoState);
                izatReports.add(izatReport);
            }
        }

        private void populateErrRecoveryReports(ArrayList<IZatErrorRecoveryReport> izatReports,
                LocAidlSystemStatusErrRecovery[] aidlReports) {
            for (LocAidlSystemStatusErrRecovery aidlReport: aidlReports) {
                IZatErrorRecoveryReport izatReport = new  IZatErrorRecoveryReport(
                        new IZatUtcSpec(aidlReport.mUtcTime.tv_sec, aidlReport.mUtcTime.tv_nsec),
                        new IZatUtcSpec(aidlReport.mUtcReported.tv_sec,
                                        aidlReport.mUtcReported.tv_nsec));
                izatReports.add(izatReport);
            }
        }

        private void populateInjectedPositionReports(ArrayList<IZatLocationReport> izatReports,
                LocAidlSystemStatusInjectedPosition[] aidlReports) {
            for (LocAidlSystemStatusInjectedPosition aidlReport: aidlReports) {
                IZatLocationReport izatReport = new  IZatLocationReport(
                        new IZatUtcSpec(aidlReport.mUtcTime.tv_sec, aidlReport.mUtcTime.tv_nsec),
                        new IZatUtcSpec(aidlReport.mUtcReported.tv_sec,
                                        aidlReport.mUtcReported.tv_nsec),
                        aidlReport.mEpiValidity | HAS_SOURCE,
                        aidlReport.mEpiLat,
                        aidlReport.mEpiLon,
                        aidlReport.mEpiHepe,
                        aidlReport.mEpiAlt,
                        aidlReport.mEpiAltUnc,
                        aidlReport.mEpiSrc);
                izatReports.add(izatReport);
            }
        }

        private void populateBestPositionReports(ArrayList<IZatLocationReport> izatReports,
                LocAidlSystemStatusBestPosition[] aidlReports) {
            for (LocAidlSystemStatusBestPosition aidlReport: aidlReports) {
                IZatLocationReport izatReport = new  IZatLocationReport(
                        new IZatUtcSpec(aidlReport.mUtcTime.tv_sec, aidlReport.mUtcTime.tv_nsec),
                        new IZatUtcSpec(aidlReport.mUtcReported.tv_sec,
                                        aidlReport.mUtcReported.tv_nsec),
                        HAS_HORIZONTAL_COMPONENT | HAS_VERTICAL_COMPONENT,
                        aidlReport.mBestLat,
                        aidlReport.mBestLon,
                        aidlReport.mBestHepe,
                        aidlReport.mBestAlt,
                        aidlReport.mBestAltUnc, 0);
                izatReports.add(izatReport);
            }
        }

        private void populateEphemerisReports(ArrayList<IZatEphmerisDebugReport> izatReports,
                LocAidlSystemStatusEphemeris[] aidlReports) {
            for (LocAidlSystemStatusEphemeris aidlReport: aidlReports) {
                IZatEphmerisDebugReport izatReport = new  IZatEphmerisDebugReport(
                        new IZatUtcSpec(aidlReport.mUtcTime.tv_sec, aidlReport.mUtcTime.tv_nsec),
                        new IZatUtcSpec(aidlReport.mUtcReported.tv_sec,
                                        aidlReport.mUtcReported.tv_nsec),
                        aidlReport.mGpsEpheValid,
                        aidlReport.mGloEpheValid,
                        aidlReport.mBdsEpheValid,
                        aidlReport.mGalEpheValid,
                        aidlReport.mQzssEpheValid);
                izatReports.add(izatReport);
            }
        }

        private void populateSvHealthReports(ArrayList<IZatSVHealthDebugReport> izatReports,
                LocAidlSystemStatusSvHealth[] aidlReports) {
            for (LocAidlSystemStatusSvHealth aidlReport: aidlReports) {
                IZatSVHealthDebugReport izatReport = new  IZatSVHealthDebugReport(
                        new IZatUtcSpec(aidlReport.mUtcTime.tv_sec, aidlReport.mUtcTime.tv_nsec),
                        new IZatUtcSpec(aidlReport.mUtcReported.tv_sec,
                                        aidlReport.mUtcReported.tv_nsec),
                        aidlReport.mGpsGoodMask,
                        aidlReport.mGpsUnknownMask,
                        aidlReport.mGpsBadMask,
                        aidlReport.mGloGoodMask,
                        aidlReport.mGloUnknownMask,
                        aidlReport.mGloBadMask,
                        aidlReport.mBdsGoodMask,
                        aidlReport.mBdsUnknownMask,
                        aidlReport.mBdsBadMask,
                        aidlReport.mGalGoodMask,
                        aidlReport.mGalUnknownMask,
                        aidlReport.mGalBadMask,
                        aidlReport.mQzssGoodMask,
                        aidlReport.mQzssUnknownMask,
                        aidlReport.mQzssBadMask);
                izatReports.add(izatReport);
            }
        }

        private void populatePdrReports(ArrayList<IZatPDRDebugReport> izatReports,
                LocAidlSystemStatusPdr[] aidlReports) {
            for (LocAidlSystemStatusPdr aidlReport: aidlReports) {
                IZatPDRDebugReport izatReport = new  IZatPDRDebugReport(
                        new IZatUtcSpec(aidlReport.mUtcTime.tv_sec, aidlReport.mUtcTime.tv_nsec),
                        new IZatUtcSpec(aidlReport.mUtcReported.tv_sec,
                                        aidlReport.mUtcReported.tv_nsec),
                        aidlReport.mFixInfoMask);
                izatReports.add(izatReport);
            }
        }

        private void populatePositionFailureReports(ArrayList<IZatFixStatusDebugReport> izatReports,
                LocAidlSystemStatusPositionFailure[] aidlReports) {
            for (LocAidlSystemStatusPositionFailure aidlReport: aidlReports) {
                IZatFixStatusDebugReport izatReport = new  IZatFixStatusDebugReport(
                        new IZatUtcSpec(aidlReport.mUtcTime.tv_sec, aidlReport.mUtcTime.tv_nsec),
                        new IZatUtcSpec(aidlReport.mUtcReported.tv_sec,
                                        aidlReport.mUtcReported.tv_nsec),
                        aidlReport.mFixInfoMask, aidlReport.mHepeLimit);
                izatReports.add(izatReport);
            }
        }
        /* =================================================
         *   AIDL Callbacks : ILocAidlDebugReportServiceCallback.hal
         * =================================================*/
        private class DebugReportServiceCallback extends ILocAidlDebugReportServiceCallback.Stub {
            private DebugReportService mProvider;
            public DebugReportServiceCallback(DebugReportService provider) {
                mProvider = provider;
            }
            @Override
            public void onXtraStatusChanged(LocAidlXtraStatus xtra)
                    throws android.os.RemoteException {
                IDLClientUtils.fromIDLService(TAG);
                IZatXTRAStatus xtraStatus = new IZatXTRAStatus(xtra.mEnabled, xtra.mStatus,
                        xtra.mValidityHrs, xtra.mLastDownloadStatus);
                mProvider.reportXtraStatusUpdate(xtraStatus);
            }
            @Override
            public final String getInterfaceHash() {
                return ILocAidlDebugReportServiceCallback.HASH;
            }
            @Override
            public final int getInterfaceVersion() {
                return ILocAidlDebugReportServiceCallback.VERSION;
            }
        }
    }
}
