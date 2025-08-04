/*====*====*====*====*====*====*====*====*====*====*====*====*====*====*====*
  Copyright (c) 2018, 2020-2021, 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
=============================================================================*/

package com.qualcomm.location.izat.wifidbprovider;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import com.qti.wifidbprovider.*;
import com.qti.wifidbreceiver.BSInfo;
import com.qualcomm.location.idlclient.LocIDLClientBase;
import com.qualcomm.location.idlclient.IDLClientUtils;
import com.qualcomm.location.izat.GTPClientHelper;
import com.qualcomm.location.izat.IzatService;

import com.qualcomm.location.idlclient.LocIDLClientBase.*;
import com.qualcomm.location.idlclient.*;
import vendor.qti.gnss.ILocAidlGnss;
import vendor.qti.gnss.ILocAidlWiFiDBProvider;
import vendor.qti.gnss.ILocAidlWiFiDBProviderCallback;
import vendor.qti.gnss.LocAidlApObsData;
import vendor.qti.gnss.LocAidlLocation;
import vendor.qti.gnss.LocAidlApScanData;
import vendor.qti.gnss.LocAidlApRangingData;
import vendor.qti.gnss.LocAidlApRangingScanResult;

import static android.location.LocationManager.FUSED_PROVIDER;
import static android.location.LocationManager.GPS_PROVIDER;

public class WiFiDBProvider implements IzatService.ISystemEventListener {
    private static final String TAG = "WiFiDBProvider";
    private static final boolean VERBOSE_DBG = Log.isLoggable(TAG, Log.VERBOSE);

    private static final Object sCallBacksLock = new Object();
    private final Context mContext;

    private volatile IWiFiDBProviderResponseListener mWiFiDBProviderResponseListener = null;

    private PendingIntent mListenerIntent = null;
    private WiFiDBProviderIdlClient mIdlClient = null;
    private String mPackageName;

    private static WiFiDBProvider sInstance = null;
    public static WiFiDBProvider getInstance(Context ctx) {
        if (sInstance == null) {
            sInstance = new WiFiDBProvider(ctx);
        }
        return sInstance;
    }

    private WiFiDBProvider(Context ctx) {
        if (VERBOSE_DBG) {
            Log.d(TAG, "WiFiDBProvider construction");
        }

        mContext = ctx;
        if (LocIDLClientBase.getIDLServiceVersion().compareTo(IDLServiceVersion.V_AIDL) >= 0) {
            mIdlClient = new WiFiDBProviderIdlClient(this);
            IzatService.AidlClientDeathNotifier.getInstance().registerAidlClientDeathCb(this);
        } else {
            Log.e(TAG, "ILoc AIDL is not supported!");
        }
    }

    @Override
    public void onAidlClientDied(String packageName, int pid, int uid) {
        if (mPackageName != null && mPackageName.equals(packageName)) {
            Log.d(TAG, "aidl client crash: " + packageName);
            synchronized (sCallBacksLock) {
                mWiFiDBProviderResponseListener = null;
            }
        }
    }

    /* Remote binder */
    private final IWiFiDBProvider.Stub mBinder = new IWiFiDBProvider.Stub() {

        public boolean registerResponseListener(final IWiFiDBProviderResponseListener callback,
                                                PendingIntent notifyIntent) {
            if (VERBOSE_DBG) {
                Log.d(TAG, "WiFiDBProvider registerResponseListener");
            }

            if (callback == null) {
                Log.e(TAG, "callback is null");
                return false;
            }

            if (notifyIntent == null) {
                Log.w(TAG, "notifyIntent is null");
            }

            synchronized (sCallBacksLock) {
                if (null != mWiFiDBProviderResponseListener) {
                    Log.e(TAG, "Response listener already provided.");
                    return false;
                }
                mWiFiDBProviderResponseListener = callback;
                mListenerIntent = notifyIntent;
            }

            mPackageName = mContext.getPackageManager().getNameForUid(Binder.getCallingUid());

            return true;
        }

        public void removeResponseListener(final IWiFiDBProviderResponseListener callback) {
            if (callback == null) {
                Log.e(TAG, "callback is null");
                return;
            }
            synchronized (sCallBacksLock) {
                mWiFiDBProviderResponseListener = null;
                mListenerIntent = null;
            }
            mPackageName = null;
        }


        public void requestAPObsLocData() {
            if (VERBOSE_DBG) {
                Log.d(TAG, "in IWiFiDBProvider.Stub(): requestAPObsLocData()");
            }

            mIdlClient.requestApObsData();
        }

    };

    private void onApObsLocDataAvailable(List<APObsLocData> obsDataList, int status) {
        if (VERBOSE_DBG) {
            Log.d(TAG, "onApObsLocDataAvailable status: " + status);
        }

        Log.d(TAG, "onApObsLocDataAvailable status: " + status);
        Log.d(TAG, "onApObsLocDataAvailable obsDataList size: " + obsDataList.size());
        synchronized (sCallBacksLock) {
            if (null != mWiFiDBProviderResponseListener) {
                try {
                    mWiFiDBProviderResponseListener.onApObsLocDataAvailable(obsDataList, status);
                } catch (RemoteException e) {
                    Log.w(TAG, "onApObsLocDataAvailable remote exception, sending intent");
                    GTPClientHelper.SendPendingIntent(mContext, mListenerIntent, "WiFiDBProvider");
                }
            }
        }
    }

    private void onServiceRequest() {
        if (VERBOSE_DBG) {
            Log.d(TAG, "onServiceRequest");
        }
        synchronized (sCallBacksLock) {
            if (null != mWiFiDBProviderResponseListener) {
                try {
                    mWiFiDBProviderResponseListener.onServiceRequest();
                } catch (RemoteException e) {
                    Log.w(TAG, "onServiceRequest remote exception, sending intent");
                    GTPClientHelper.SendPendingIntent(mContext, mListenerIntent, "WiFiDBProvider");
                }
            }
        }
    }

    public IWiFiDBProvider getWiFiDBProviderBinder() {
        return mBinder;
    }

    // ======================================================================
    // AIDL client
    // ======================================================================
    static class WiFiDBProviderIdlClient extends LocIDLClientBase
            implements LocIDLClientBase.IServiceDeathCb {
        private final String TAG = "WiFiDBProviderIdlClient";
        private final boolean VERBOSE = Log.isLoggable(TAG, Log.VERBOSE);

        private LocAidlWiFiDBProviderCallback mLocAidlWiFiDBProvicerCallback;
        private ILocAidlWiFiDBProvider mLocAidlWiFiDBProvider;

        public WiFiDBProviderIdlClient(WiFiDBProvider provider) {
            getWiFiDBProviderIface();
            mLocAidlWiFiDBProvicerCallback = new LocAidlWiFiDBProviderCallback(provider);

            if (null != mLocAidlWiFiDBProvider) {
                try {
                    mLocAidlWiFiDBProvider.init(mLocAidlWiFiDBProvicerCallback);
                    mLocAidlWiFiDBProvider.registerWiFiDBProvider(mLocAidlWiFiDBProvicerCallback);
                    registerServiceDiedCb(this);
                } catch (RemoteException e) {
                    Log.e(TAG, "Exception on provider init: " + e);
                }
            }
        }

        private void getWiFiDBProviderIface() {
            Log.i(TAG, "getWiFiDBProviderIface");
            ILocAidlGnss gnssService = (vendor.qti.gnss.ILocAidlGnss) getGnssAidlService();

            if (null != gnssService) {
                try {
                    mLocAidlWiFiDBProvider = gnssService.getExtensionLocAidlWiFiDBProvider();
                } catch (RemoteException e) {
                    throw new RuntimeException("Exception getting wifidb provider: " + e);
                }
            }
        }

        @Override
        public void onServiceDied() {
            mLocAidlWiFiDBProvider = null;
            getWiFiDBProviderIface();
            if (null != mLocAidlWiFiDBProvider) {
                try {
                    mLocAidlWiFiDBProvider.init(mLocAidlWiFiDBProvicerCallback);
                    mLocAidlWiFiDBProvider.registerWiFiDBProvider(mLocAidlWiFiDBProvicerCallback);
                } catch (RemoteException e) {
                    Log.e(TAG, "Exception on provider init: " + e);
                }
            }
        }

        public void requestApObsData() {
            if (null != mLocAidlWiFiDBProvider) {
                try {
                    mLocAidlWiFiDBProvider.sendAPObsLocDataRequest();
                } catch (RemoteException e) {
                    throw new RuntimeException("Exception on sendAPObsLocDataRequest: " + e);
                }
            } else {
                throw new RuntimeException("mLocAidlWiFiDBProvider is null!");
            }
        }

        // ======================================================================
        // Callbacks
        // ======================================================================

        class LocAidlWiFiDBProviderCallback extends ILocAidlWiFiDBProviderCallback.Stub {

            private WiFiDBProvider mWiFiDBProvider;

            private LocAidlWiFiDBProviderCallback(WiFiDBProvider wiFiDBProvider) {
                mWiFiDBProvider = wiFiDBProvider;
            }

            public void attachVmOnCallback() {
                // ???
            }

            public void serviceRequestCallback() {
                mWiFiDBProvider.onServiceRequest();
            }

            public void apObsLocDataUpdateCallback(LocAidlApObsData[] apObsLocDataList,
                    int apObsLocDataListSize, int apListStatus) {

                ArrayList<APObsLocData> aPObsLocDataList = new ArrayList<APObsLocData>();

                for (LocAidlApObsData apObsData: apObsLocDataList) {
                    APObsLocData apObsLocData = new APObsLocData();
                    apObsLocData.mScanTimestamp = (int) (apObsData.scanTimestamp_ms / 1000);
                    apObsLocData.mLocation = IDLClientUtils.translateAidlLocation(
                            apObsData.gpsLoc.gpsLocation);
                    if (apObsData.gpsLoc.position_source == 2) {
                        //This is a GNSS position report
                        apObsLocData.mLocation.setProvider(GPS_PROVIDER);
                    } else {
                        //Mark it as fused provider if it is not a GNSS position report
                        apObsLocData.mLocation.setProvider(FUSED_PROVIDER);
                    }

                    BSInfo bsInfo = new BSInfo();
                    bsInfo.mCellType = RiltoIZatCellTypes(apObsData.cellInfo.cell_type);
                    bsInfo.mCellRegionID1 = apObsData.cellInfo.cell_id1;
                    bsInfo.mCellRegionID2 = apObsData.cellInfo.cell_id2;
                    bsInfo.mCellRegionID3 = apObsData.cellInfo.cell_id3;
                    bsInfo.mCellRegionID4 = apObsData.cellInfo.cell_id4;

                    apObsLocData.mCellInfo = bsInfo;

                    if (apObsData.ap_scan_info_size > 0) {
                        ArrayList<APScan> apScanList = new ArrayList<APScan>();
                        for (LocAidlApScanData apAidlScan: apObsData.ap_scan_info) {
                            APScan apScan = new APScan();
                            apScan.mMacAddress = IDLClientUtils.longMacToHex(apAidlScan.mac_R48b);
                            apScan.mRssi = apAidlScan.rssi;
                            apScan.mDeltaTime = (int) apAidlScan.age_usec;
                            apScan.mChannelNumber = apAidlScan.channel_id >= 0 ?
                                    apAidlScan.channel_id : (256 + apAidlScan.channel_id);
                            apScan.mSSID = apAidlScan.ssid.getBytes();;
                            apScan.mIsServing = apAidlScan.isServing;
                            apScan.mFrequency = apAidlScan.frequency;
                            apScan.mBandWidth = apAidlScan.rxBandWidth;
                            apScanList.add(apScan);
                        }

                        APScan[] apScanArray = new APScan[apScanList.size()];
                        apScanList.toArray(apScanArray);
                        apObsLocData.mScanList = apScanArray;
                    } else {
                        apObsLocData.mScanList = null;
                    }

                    if (apObsData.ap_ranging_info_size > 0) {
                        ArrayList<APRttScan> apRttScanList = new ArrayList<APRttScan>();
                        for (LocAidlApRangingData rttAidlScan : apObsData.ap_ranging_info) {
                            APRttScan rttScan = new APRttScan();
                            rttScan.mMacAdress = IDLClientUtils.longMacToHex(rttAidlScan.mac_R48b);
                            rttScan.mDeltaTime = (int) rttAidlScan.age_usec;
                            rttScan.mNumAttempted = rttAidlScan.num_attempted;
                            ArrayList<APRangingMeasurement> measurements =
                                    new ArrayList<APRangingMeasurement>();
                            for (LocAidlApRangingScanResult rttMeas :
                                    rttAidlScan.ap_ranging_scan_info) {
                                APRangingMeasurement meas = new APRangingMeasurement();
                                meas.mDistanceInMm = rttMeas.distanceMm;
                                meas.mRssi = rttMeas.rssi;
                                meas.mTxBandWidth = rttMeas.txBandWidth;
                                meas.mRxBandWidth = rttMeas.rxBandWidth;
                                meas.mChainNumber = rttMeas.chainNo;
                                measurements.add(meas);
                            }
                            APRangingMeasurement[] measArray =
                                    new APRangingMeasurement[measurements.size()];
                            measurements.toArray(measArray);
                            rttScan.mRangingMeasurements = measArray;
                            apRttScanList.add(rttScan);
                        }

                        APRttScan[] rttScanArray = new APRttScan[apRttScanList.size()];
                        apRttScanList.toArray(rttScanArray);
                        apObsLocData.mRttScanList = rttScanArray;
                    } else {
                        apObsLocData.mRttScanList = null;
                    }

                    aPObsLocDataList.add(apObsLocData);
                }

                mWiFiDBProvider.onApObsLocDataAvailable(aPObsLocDataList, apListStatus);
            }
            @Override
            public final int getInterfaceVersion() {
                return ILocAidlWiFiDBProviderCallback.VERSION;
            }
            @Override
            public final String getInterfaceHash() {
                return ILocAidlWiFiDBProviderCallback.HASH;
            }
        }
    }

    public static int RiltoIZatCellTypes(int rilCellType) {
        int res = -1;
        final int LOC_RIL_TECH_CDMA = 0x1;
        final int LOC_RIL_TECH_GSM = 0x2;
        final int LOC_RIL_TECH_WCDMA = 0x4;
        final int LOC_RIL_TECH_LTE = 0x8;

        final int GSM = 0;
        final int WCDMA = 1;
        final int CDMA = 2;
        final int LTE = 3;


        if (rilCellType == LOC_RIL_TECH_CDMA) {
            res = CDMA;
        }

        if (rilCellType == LOC_RIL_TECH_GSM) {
            res = GSM;
        }

        if (rilCellType == LOC_RIL_TECH_WCDMA) {
            res = WCDMA;
        }

        if (rilCellType == LOC_RIL_TECH_LTE) {
            res = LTE;
        }

        return res;
    }
}
