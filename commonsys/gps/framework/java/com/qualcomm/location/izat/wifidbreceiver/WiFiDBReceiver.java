/*====*====*====*====*====*====*====*====*====*====*====*====*====*====*====*
  Copyright (c) 2017, 2020-2021, 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
=============================================================================*/
package com.qualcomm.location.izat.wifidbreceiver;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.RemoteException;
import android.util.Log;

import com.qti.debugreport.*;
import com.qti.wifidbreceiver.*;

import com.qualcomm.location.idlclient.LocIDLClientBase;
import com.qualcomm.location.idlclient.IDLClientUtils;
import com.qualcomm.location.izat.GTPClientHelper;
import com.qualcomm.location.izat.IzatService;
import com.qualcomm.location.izat.esstatusreceiver.EsStatusReceiver;

import java.util.ArrayList;
import java.util.List;

import com.qualcomm.location.idlclient.LocIDLClientBase.*;
import com.qualcomm.location.idlclient.*;
import vendor.qti.gnss.ILocAidlGnss;
import vendor.qti.gnss.ILocAidlWiFiDBReceiver;
import vendor.qti.gnss.ILocAidlWiFiDBReceiverCallback;
import vendor.qti.gnss.LocAidlIzatLocation;
import vendor.qti.gnss.LocAidlApInfo;
import vendor.qti.gnss.LocAidlApSpecialInfo;
import vendor.qti.gnss.LocAidlApLocationData;
import vendor.qti.gnss.LocAidlUlpLocation;


public class WiFiDBReceiver implements IzatService.ISystemEventListener {
    private static final String TAG = "WiFiDBReceiver";
    private static final boolean VERBOSE = Log.isLoggable(TAG, Log.VERBOSE);

    // As defined in LocAidlWifiDBListStatus & GTP
    private static final int LOOKUP_STATUS = 3;

    private static final Object sCallBacksLock = new Object();
    private final Context mContext;
    // In case and old SDK client < 7.0 is used
    private boolean mIsLegacySDKClient = false;

    //Indicate whether the device is in emergency mode
    private boolean mIsEmergency = false;

    private volatile IWiFiDBReceiverResponseListener mWiFiDBReceiverResponseListener = null;
    private PendingIntent mListenerIntent = null;
    private WiFiDBReceiverIdlClient mIdlClient = null;

    private String mPackageName;

    private static WiFiDBReceiver sInstance = null;
    public static WiFiDBReceiver getInstance(Context ctx) {
        if (sInstance == null) {
            sInstance = new WiFiDBReceiver(ctx);
        }
        return sInstance;
    }

    private WiFiDBReceiver(Context ctx) {
        if (VERBOSE) {
            Log.d(TAG, "WiFiDBReceiver construction");
        }

        mContext = ctx;
        if (LocIDLClientBase.getIDLServiceVersion().compareTo(IDLServiceVersion.V_AIDL) >= 0) {
            mIdlClient = new WiFiDBReceiverIdlClient(this);
            EsStatusReceiver.getInstance(ctx).registerEsStatusListener(
                    new EsStatusReceiver.IEsStatusListener() {
                    public void onStatusChanged(boolean isEmergencyMode) {
                    Log.d(TAG, "Emergency mode changed to : " + isEmergencyMode);
                    mIsEmergency = isEmergencyMode;
                    }});

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
                mWiFiDBReceiverResponseListener = null;
            }
        }
    }

    /* Remote binder */
    private final IWiFiDBReceiver.Stub mBinder = new IWiFiDBReceiver.Stub() {

        @Deprecated
        // For backwards compatibility with possible static-linked SDK
        public boolean registerResponseListener(final IWiFiDBReceiverResponseListener callback) {
            mIsLegacySDKClient = true;
            return registerResponseListenerExt(callback, null);
        }

        // For backwards compatibility with possible static-linked SDK
        public boolean registerResponseListenerExt(final IWiFiDBReceiverResponseListener callback,
                                                   PendingIntent notifyIntent) {
            mIsLegacySDKClient = false;
            if (callback == null) {
                Log.e(TAG, "callback is null");
                return false;
            }

            if (notifyIntent == null) {
                Log.w(TAG, "notifyIntent is null");
            }

            synchronized (sCallBacksLock) {
                if (null != mWiFiDBReceiverResponseListener) {
                    Log.e(TAG, "Response listener already provided.");
                    return false;
                }

                mWiFiDBReceiverResponseListener = callback;
                mListenerIntent = notifyIntent;
            }

            mPackageName = mContext.getPackageManager().getNameForUid(Binder.getCallingUid());

            return true;
        }

        public void removeResponseListener(final IWiFiDBReceiverResponseListener callback) {
            if (callback == null) {
                Log.e(TAG, "callback is null");
                return;
            }
            synchronized (sCallBacksLock) {
                mWiFiDBReceiverResponseListener = null;
                mListenerIntent = null;
            }

            mPackageName = null;
        }


        public void requestAPList(int expireInDays) {
            if (VERBOSE) {
                Log.d(TAG, "in IWiFiDBReceiver.Stub(): requestAPList()");
            }

            mIdlClient.sendAPListRequest(expireInDays);
        }

        public void requestScanList() {
            if (VERBOSE) {
                Log.d(TAG, "in IWiFiDBReceiver.Stub(): requestScanList()");
            }

            mIdlClient.sendScanListRequest();
        }

        public void pushWiFiDB(List<APLocationData> locData,
                              List<APSpecialInfo> splData,
                              int daysValid) {
            // Legacy SDK used reverse MAC as 445566112233 like original WiFi module
            if (mIsLegacySDKClient) {
                for (APLocationData apLocationData: locData) {
                    apLocationData.mMacAddress = reverseLo24Hi24(apLocationData.mMacAddress);
                }
                for (APSpecialInfo apSpecialInfo: splData) {
                    apSpecialInfo.mMacAddress = reverseLo24Hi24(apSpecialInfo.mMacAddress);
                }
            }

            mIdlClient.pushAPWiFiDB(locData, splData, daysValid, false);
        }

        public void pushLookupResult(List<APLocationData> locData,
                              List<APSpecialInfo> splData) {
            mIdlClient.pushAPWiFiDB(locData, splData, 0, true);
        }
    };

    protected void onAPListAvailable(ArrayList<APInfoExt> apInfoExtList, int status,
            Location location) {
        if (VERBOSE) {
            Log.d(TAG, "onAPListAvailable status: " + status);
        }
        synchronized (sCallBacksLock) {
            if (null != mWiFiDBReceiverResponseListener) {
                try {
                    if (mIsLegacySDKClient) {
                        List<APInfo> apInfoList = new ArrayList<APInfo>();
                        for (APInfoExt apInfoExt: apInfoExtList) {
                            APInfo apInfo = new APInfo();
                            apInfo.mMacAddress = apInfoExt.mMacAddress;
                            // Legacy SDK used reverse MAC as 445566112233 like original WiFi module
                            apInfo.mMacAddress = reverseLo24Hi24(apInfo.mMacAddress);
                            apInfo.mSSID = apInfoExt.mSSID;
                            apInfo.mCellType = apInfoExt.mCellType;
                            apInfo.mCellRegionID1 = apInfoExt.mCellRegionID1;
                            apInfo.mCellRegionID2 = apInfoExt.mCellRegionID2;
                            apInfo.mCellRegionID3 = apInfoExt.mCellRegionID3;
                            apInfo.mCellRegionID4 = apInfoExt.mCellRegionID4;

                            apInfoList.add(apInfo);
                        }

                        mWiFiDBReceiverResponseListener.onAPListAvailable(
                                apInfoList);
                    } else {
                        // Check special status to indicate it is the new onLookupRequest
                        if (LOOKUP_STATUS == status) {
                            if (!mWiFiDBReceiverResponseListener.onLookupRequestES(
                                    apInfoExtList, location, mIsEmergency)) {
                                Log.d(TAG, "Fall back to legacy onLookupRequest!");
                                mWiFiDBReceiverResponseListener.onLookupRequest(
                                        apInfoExtList, location);
                            }
                        } else {
                            mWiFiDBReceiverResponseListener.onAPListAvailableExt(
                                    apInfoExtList, status, location);
                        }
                    }
                } catch (RemoteException e) {
                    Log.w(TAG, "onAPListAvailable remote exception, sending intent");
                    GTPClientHelper.SendPendingIntent(mContext, mListenerIntent, "WiFiDBReceiver");
               }
            }
        }
    }

    protected void onStatusUpdate(boolean isSuccess, String error) {
        if (VERBOSE) {
            Log.d(TAG, "onStatusUpdate");
        }
        synchronized (sCallBacksLock) {
            if (null != mWiFiDBReceiverResponseListener) {
                try {
                    mWiFiDBReceiverResponseListener.onStatusUpdate(isSuccess,
                                                                                        error);
                    if (VERBOSE) {
                        Log.d(TAG, "onStatusUpdate: send update to listener");
                    }
                } catch (RemoteException e) {
                    Log.w(TAG, "onStatusUpdate remote exception, sending intent");
                    GTPClientHelper.SendPendingIntent(mContext, mListenerIntent, "WiFiDBReceiver");
                }
            }
        }
    }

    protected void onServiceRequest() {
        if (VERBOSE) {
            Log.d(TAG, "onServiceRequest");
        }
        synchronized (sCallBacksLock) {
            if (null != mWiFiDBReceiverResponseListener) {
                try {
                        if (!mWiFiDBReceiverResponseListener.onServiceRequestES(mIsEmergency)) {
                            Log.d(TAG, "Fall back to legacy onServiceRequest!");
                            mWiFiDBReceiverResponseListener.onServiceRequest();
                        }
                } catch (RemoteException e) {
                    Log.w(TAG, "onServiceRequest remote exception, sending intent");
                    GTPClientHelper.SendPendingIntent(mContext, mListenerIntent,
                            "WiFiDBReceiver");
                }
            }
        }
    }

    public IWiFiDBReceiver getWiFiDBReceiverBinder() {
        return mBinder;
    }

    private static String reverseLo24Hi24(String mac) {
        String res = mac;
        if (null != mac && mac.length() == 12) {
            res = mac.substring(6, 12) + mac.substring(0, 6);
        }
        return res;
    }

    // ======================================================================
    // WiFiDBReceiver Java AIDL client
    // ======================================================================
    static class WiFiDBReceiverIdlClient extends LocIDLClientBase
            implements LocIDLClientBase.IServiceDeathCb {
        private final String TAG = "WiFiDBReceiverIdlClient";
        private final boolean VERBOSE = Log.isLoggable(TAG, Log.VERBOSE);

        private ILocAidlWiFiDBReceiver mWiFiDBReceiverIface;

        private final int HAS_HORIZONTAL_COMPONENT = 1;
        private final int HAS_VERTICAL_COMPONENT = 2;
        private final int HAS_QUALITY = 4;

        private LocAidlWiFiDBReceiverCallback mLocAidlWiFiDBReceiverCallback;

        private void getWiFiDBReceiverIface() {
            if (null == mWiFiDBReceiverIface) {
                ILocAidlGnss service = (ILocAidlGnss)getGnssAidlService();

                if (null != service) {
                    try {
                        mWiFiDBReceiverIface = service.getExtensionLocAidlWiFiDBReceiver();
                    } catch (RemoteException e) {
                        Log.e(TAG, "Exception getting wifi db wrapper: " + e);
                        mWiFiDBReceiverIface = null;
                    }
                }
            }
        }

        @Override
        public void onServiceDied() {
            mWiFiDBReceiverIface = null;
            getWiFiDBReceiverIface();
            if (null != mWiFiDBReceiverIface) {
                try {
                    mWiFiDBReceiverIface.init(mLocAidlWiFiDBReceiverCallback);
                    mWiFiDBReceiverIface.registerWiFiDBUpdater(mLocAidlWiFiDBReceiverCallback);
                } catch (RemoteException e) {
                    Log.e(TAG, "Exception on receiver init: " + e);
                }
            }
        }

        private WiFiDBReceiverIdlClient(WiFiDBReceiver receiver) {
            getWiFiDBReceiverIface();

            mLocAidlWiFiDBReceiverCallback = new LocAidlWiFiDBReceiverCallback(receiver);

            try {
                Log.d(TAG, "WiFiDBReceiverIdlClient aidl init");
                if (null != mWiFiDBReceiverIface) {
                    mWiFiDBReceiverIface.init(mLocAidlWiFiDBReceiverCallback);
                    mWiFiDBReceiverIface.registerWiFiDBUpdater(mLocAidlWiFiDBReceiverCallback);
                    registerServiceDiedCb(this);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Exception on receiver init: " + e);
            }
        }

        public void sendAPListRequest(int expire_in_days) {
            try {
                if (null != mWiFiDBReceiverIface) {
                    mWiFiDBReceiverIface.sendAPListRequest(expire_in_days);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Exception in sendAPListRequest: " + e);
            }
        }

        public void sendScanListRequest() {
            try {
                if (null != mWiFiDBReceiverIface) {
                    mWiFiDBReceiverIface.sendScanListRequest();
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Exception in sendScanListRequest: " + e);
            }
        }

        public int pushAPWiFiDB(List<APLocationData> locData, List<APSpecialInfo> splData,
                int daysValid, boolean isLookup) {

            int result = 0;

            LocAidlApLocationData[] apLocList = new LocAidlApLocationData[locData.size()];
            int i = 0;
            for (APLocationData loc: locData) {
                LocAidlApLocationData apLoc = new LocAidlApLocationData();
                apLoc.mac_R48b = IDLClientUtils.hexMacToLong(loc.mMacAddress);
                apLoc.latitude = loc.mLatitude;
                apLoc.longitude = loc.mLongitude;
                apLoc.max_antenna_range = loc.mMaxAntenaRange;
                apLoc.horizontal_error = loc.mHorizontalError;
                apLoc.reliability = (byte) loc.mReliability;
                apLoc.valid_bits = (byte) loc.mValidBits;
                apLoc.altitude = loc.mAltitude;
                apLoc.altRefType = loc.mAltRefType;
                apLoc.rttCapability = loc.mRttCapability;
                apLoc.positionQuality =  loc.mPositionQuality;
                apLoc.rttRangeBiasInMm = loc.mRttRangeBiasInMm;
                apLocList[i] = apLoc;
                i++;
            }

            LocAidlApSpecialInfo[] splList = new LocAidlApSpecialInfo[splData.size()];
            i = 0;
            for (APSpecialInfo sp: splData) {
                LocAidlApSpecialInfo spl = new LocAidlApSpecialInfo();
                spl.mac_R48b = IDLClientUtils.hexMacToLong(sp.mMacAddress);
                spl.info = (byte) sp.mInfo;

                splList[i] = spl;
                i++;
            }

            try {
                if (null != mWiFiDBReceiverIface) {
                    mWiFiDBReceiverIface.pushAPWiFiDB(apLocList, apLocList.length,
                            splList, splList.length, daysValid, isLookup);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Exception pushing wifidb: " + e);
            }

            return result;
        }

        protected void finalize() throws Throwable {
            if (null != mWiFiDBReceiverIface) {
                mWiFiDBReceiverIface.unregisterWiFiDBUpdater();
            }
        }

        // ======================================================================
        // Callbacks
        // ======================================================================

        class LocAidlWiFiDBReceiverCallback extends ILocAidlWiFiDBReceiverCallback.Stub {

            private WiFiDBReceiver mWiFiDBReceiver;

            private LocAidlWiFiDBReceiverCallback(WiFiDBReceiver wiFiDBReceiver) {
                mWiFiDBReceiver = wiFiDBReceiver;
            }

            public void attachVmOnCallback() {
                // ???
            }

            public void serviceRequestCallback() {
                mWiFiDBReceiver.onServiceRequest();
            }

            public void statusUpdateCallback(boolean status, String reason) {
                mWiFiDBReceiver.onStatusUpdate(status, reason);
            }

            public void apListUpdateCallback(LocAidlApInfo[] apInfoList,
                    int apListSize, int apListStatus, LocAidlUlpLocation ulpLocation,
                    boolean ulpLocationValid) {

                ArrayList<APInfoExt> apInfoExtList = new ArrayList<APInfoExt>();

                for (LocAidlApInfo apInfoAidl: apInfoList) {
                    APInfoExt apInfo = new APInfoExt();
                    apInfo.mMacAddress =
                            IDLClientUtils.longMacToHex(apInfoAidl.mac_R48b);
                    apInfo.mCellType = apInfoAidl.cell_type;
                    apInfo.mCellRegionID1 = apInfoAidl.cell_id1;
                    apInfo.mCellRegionID2 = apInfoAidl.cell_id2;
                    apInfo.mCellRegionID3 = apInfoAidl.cell_id3;
                    apInfo.mCellRegionID4 = apInfoAidl.cell_id4;
                    apInfo.mSSID = apInfoAidl.ssid.getBytes();
                    apInfo.mTimestamp = (int) apInfoAidl.utc_time;
                    apInfo.mFdalStatus = apInfoAidl.fdal_status;

                    apInfoExtList.add(apInfo);
                }

                mWiFiDBReceiver.onAPListAvailable(apInfoExtList, apListStatus,
                        IDLClientUtils.translateAidlLocation(ulpLocation.gpsLocation));
            }
            @Override
            public final int getInterfaceVersion() {
                return ILocAidlWiFiDBReceiverCallback.VERSION;
            }
            @Override
            public final String getInterfaceHash() {
                return ILocAidlWiFiDBReceiverCallback.HASH;
            }
        }
    }
}
