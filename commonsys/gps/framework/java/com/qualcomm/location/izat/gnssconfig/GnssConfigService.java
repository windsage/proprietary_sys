/*====*====*====*====*====*====*====*====*====*====*====*====*====*====*====*
  Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
=============================================================================*/

package com.qualcomm.location.izat.gnssconfig;

import android.content.Context;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.content.pm.PackageManager;

import android.util.Log;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import com.qti.gnssconfig.*;
import com.qualcomm.location.izat.*;
import com.qualcomm.location.utils.IZatServiceContext;

import com.qualcomm.location.idlclient.LocIDLClientBase.*;
import com.qualcomm.location.idlclient.*;
import com.qualcomm.location.izat.IzatService;
import com.qualcomm.location.izat.IzatService.*;
import vendor.qti.gnss.ILocAidlGnss;
import vendor.qti.gnss.ILocAidlGnssConfigService;
import vendor.qti.gnss.ILocAidlGnssConfigServiceCallback;
import vendor.qti.gnss.LocAidlNtripConnectionParams;
import vendor.qti.gnss.LocAidlRobustLocationInfo;

public class GnssConfigService implements ISsrNotifier, ISystemEventListener {
    private static final String TAG = "GnssConfigService";
    private static final boolean VERBOSE = Log.isLoggable(TAG, Log.VERBOSE);

    private static final Object sCallBacksLock = new Object();
    private RemoteCallbackList<IGnssConfigCallback> mGnssConfigCallbacks
        = new RemoteCallbackList<IGnssConfigCallback>();
    private final Context mContext;
    private IZatServiceContext mIZatServiceCtx;
    private final UserConsentManager mUserConsentManager;

    private static final String ACCESS_SV_CONFIG_API =
            "com.qualcomm.qti.permission.ACCESS_SV_CONFIG_API";
    private static final String ACCESS_ROBUST_LOCATION =
            "com.qualcomm.qti.permission.ACCESS_ROBUST_LOCATION_API";
    private static final String ACCESS_PRECISE_LOCATION_API =
            "com.qualcomm.qti.permission.ACCESS_PRECISE_LOCATION_API";
    private static final String USER_CONSENT_PERMISSION =
            "com.qualcomm.permission.ACCESS_USER_CONSENT_API";
    private static final String IZAT_PERMISSION = "com.qualcomm.permission.IZAT";

    private ConfigData mConfigData = new ConfigData();
    private class ClientGnssConfigData extends CallbackData {
        private IGnssConfigCallback mCallback;

        public ClientGnssConfigData(IGnssConfigCallback callback) {
            mCallback = callback;
            super.mCallback = callback;
        }
    }

    private DataPerPackageAndUser<ClientGnssConfigData> mDataPerPackageAndUser;

    private boolean isNtnApiSessionOngoing = false;

    private static GnssConfigService sInstance = null;
    public static GnssConfigService getInstance(Context ctx) {
        if (sInstance == null) {
            sInstance = new GnssConfigService(ctx);
        }
        return sInstance;
    }
    private GnssConfigServiceIdlClient mRemoteServiceClient;
    private GnssConfigService(Context ctx) {
        if (VERBOSE) {
            Log.d(TAG, "GnssConfigService construction");
        }

        mContext = ctx;
        mDataPerPackageAndUser = new DataPerPackageAndUser<ClientGnssConfigData>(mContext,
                new UserChanged());
        mUserConsentManager = UserConsentManager.getInstance(mContext);
        if (LocIDLClientBase.getIDLServiceVersion().compareTo(IDLServiceVersion.V_AIDL) >= 0) {
            mRemoteServiceClient = new GnssConfigServiceIdlClient(this);
            IzatService.AidlClientDeathNotifier.getInstance().registerAidlClientDeathCb(this);
            mIZatServiceCtx = IZatServiceContext.getInstance(mContext);
        } else {
            Log.e(TAG, "ILoc AIDL is not supported!");
        }
    }

    @Override
    public void onAidlClientDied(String packageName, int pid, int uid) {
        Log.d(TAG, "aidl client crash: " + packageName);
        synchronized (sCallBacksLock) {
            ClientGnssConfigData clData =
                    mDataPerPackageAndUser.getDataByPkgName(packageName);

            if (null != clData) {
                if (VERBOSE) {
                    Log.d(TAG, "Package died: " + clData.mPackageName);
                }
                mGnssConfigCallbacks.unregister(clData.mCallback);
            }

        }
    }

    enum PrecisePositionOptInStatus {
        UNKNOWN, OFF, ON
    }
    enum RobustLocationStatus {
        UNKNOWN, OFF, LOW, HIGH
    }
    /* =================================================
     *   retrieve config when SSR or boot up
     *  ================================================*/
    private class ConfigData extends JSONizable {
        public RobustLocationStatus rlStatus = RobustLocationStatus.UNKNOWN;

        public PrecisePositionOptInStatus ppOptInStatus = PrecisePositionOptInStatus.UNKNOWN;
        public boolean ppEnabled = false;   // true when precise position param configured
        public String ppHostNameOrIP = "";
        public String ppMountPointName = "";
        public int ppPort = 0;
        public String ppUserName = "";
        public String ppPassword = "";
        public boolean ppRequiresInitialNMEA = false;
        public boolean ppUseSSL = false;
        public int ppNmeaUpdateInterval = 0;
    }

    private void SaveConfigData() {
        SsrHandler.get().registerDataForSSREvents(mContext,
                GnssConfigService.class.getName(), mConfigData.toJSON());
    }

    public void restoreConfigData() {
        if (mConfigData.rlStatus != RobustLocationStatus.UNKNOWN) {
            // RobustLocationStatus <--> rlEnable,rlForE911Enable:
            // OFF <--> F, F;   LOW <--> T, F;   HIGH <--> T, T
            boolean rlEnable = (mConfigData.rlStatus != RobustLocationStatus.OFF);
            boolean rlForE911Enable = (mConfigData.rlStatus == RobustLocationStatus.HIGH);
            mRemoteServiceClient.setRobustLocationConfig(rlEnable, rlForE911Enable);
        }

        if (mConfigData.ppOptInStatus != PrecisePositionOptInStatus.UNKNOWN) {
            boolean ppOptIn = (mConfigData.ppOptInStatus == PrecisePositionOptInStatus.ON);
            mRemoteServiceClient.updateNtripGgaConsent(ppOptIn);
        }
        if (mIZatServiceCtx.isPreciseLocationSupported() && mConfigData.ppEnabled) {
            NtripConfigData data = new NtripConfigData();
            data.mHostNameOrIP = mConfigData.ppHostNameOrIP;
            data.mMountPointName = mConfigData.ppMountPointName;
            data.mPort = mConfigData.ppPort;
            data.mUserName = mConfigData.ppUserName;
            data.mPassword = mConfigData.ppPassword;
            data.mRequiresInitialNMEA = mConfigData.ppRequiresInitialNMEA;
            data.mUseSSL = mConfigData.ppUseSSL;
            data.mNmeaUpdateInterval = mConfigData.ppNmeaUpdateInterval;

            mRemoteServiceClient.enablePPENtripStream(data);
        } else {
            mRemoteServiceClient.disablePPENtripStream();
        }
    }

    @Override
    public void bootupAndSsrNotifier(String jsonStr) {
        Log.d(TAG, "bootupAndSsrNotifier");
        mConfigData.fromJSON(jsonStr);
        restoreConfigData();
    }


    /* Remote binder */
    private final IGnssConfigService.Stub mBinder = new IGnssConfigService.Stub() {

        /**
         *
         * Register Callback
         *
         */
        public void registerCallback(final IGnssConfigCallback callback) {
            if (!(mContext.checkCallingPermission(ACCESS_SV_CONFIG_API) ==
                    PackageManager.PERMISSION_GRANTED ||
                    mContext.checkCallingPermission(ACCESS_ROBUST_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED ||
                    mContext.checkCallingPermission(ACCESS_PRECISE_LOCATION_API) ==
                    PackageManager.PERMISSION_GRANTED)) {
                throw new SecurityException("Requires ACCESS_SV_CONFIG_API or" +
                        "ACCESS_ROBUST_LOCATION or ACCESS_PRECISE_LOCATION_API permission");
            }

            if (callback == null) {
                Log.e(TAG, "callback is null");
                return;
            }

            synchronized (sCallBacksLock) {
                if (VERBOSE) {
                    Log.d(TAG, "getGnssSvTypeConfig: " +
                            mDataPerPackageAndUser.getPackageName(null));
                }


                ClientGnssConfigData clData = mDataPerPackageAndUser.getData();
                if (null == clData) {
                    clData = new ClientGnssConfigData(callback);
                    mDataPerPackageAndUser.setData(clData);
                } else {
                    if (null != clData.mCallback) {
                        mGnssConfigCallbacks.unregister(clData.mCallback);
                    }
                    clData.mCallback = callback;
                }

                mGnssConfigCallbacks.register(callback);
            }
        }

        /**
         *
         * Robust location config
         *
         */
        public void getRobustLocationConfig() {
            Log.d(TAG, "getRobustLocationConfig: Has ACCESS_ROBUST_LOCATION permission: " +
                    mContext.checkCallingPermission(ACCESS_ROBUST_LOCATION));
            if (mContext.checkCallingPermission(ACCESS_ROBUST_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED) {
                throw new SecurityException("Requires ACCESS_ROBUST_LOCATION_API permission");
            }
            if (IzatService.isDeviceOEMUnlocked(mContext)) {
                throw new SecurityException(
                        "Robust location only supported on bootloader locked device!");
            }

            synchronized(sCallBacksLock) {
                if (VERBOSE) {
                    Log.d(TAG, "getRobustLocationConfig: " +
                                mDataPerPackageAndUser.getPackageName(null));
                }
                mRemoteServiceClient.getRobustLocationConfig();
            }
        }

        public void setRobustLocationConfig(boolean enable, boolean enableForE911) {
            Log.d(TAG, "setRobustLocationConfig: " + "enable: " + enable + "enableForE911: " +
                    enableForE911 + "Has ACCESS_ROBUST_LOCATION permission: " +
                    mContext.checkCallingPermission(ACCESS_ROBUST_LOCATION));
            if (mContext.checkCallingPermission(ACCESS_ROBUST_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED) {
                throw new SecurityException("Requires ACCESS_ROBUST_LOCATION_API permission");
            }
            if (IzatService.isDeviceOEMUnlocked(mContext)) {
                throw new SecurityException(
                        "Robust location only supported on bootloader locked device!");
            }

            synchronized(sCallBacksLock) {
                if (VERBOSE) {
                    Log.d(TAG, "setRobustLocationConfig: " +
                                mDataPerPackageAndUser.getPackageName(null));
                }
                mRemoteServiceClient.setRobustLocationConfig(enable, enableForE911);
            }
            if (enableForE911) { // enableForE911 == true implies enable == true
                mConfigData.rlStatus = RobustLocationStatus.HIGH;
            } else if (enable) {
                mConfigData.rlStatus = RobustLocationStatus.LOW;
            } else { // !enable && !enableForE911
                mConfigData.rlStatus = RobustLocationStatus.OFF;
            }
            SaveConfigData();
        }

        @Override
        public boolean configMerkleTree(String merkleTreeXml, int xmlSize) {
            Log.v(TAG, "configMerkleTree, size: " + xmlSize + ", merkleTreeXml: " + merkleTreeXml +
                    "Has ACCESS_ROBUST_LOCATION permission: " +
                    mContext.checkCallingPermission(ACCESS_ROBUST_LOCATION));
            if (mContext.checkCallingPermission(ACCESS_ROBUST_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED) {
                throw new SecurityException("Requires ACCESS_ROBUST_LOCATION_API permission");
            }
            boolean retVal = false;
            synchronized(sCallBacksLock) {
                retVal = mRemoteServiceClient.configMerkleTree(merkleTreeXml, xmlSize);
            }
            return retVal;
        }

        @Override
        public boolean configOsnmaEnablement(boolean isEnabled) {
            Log.v(TAG, "configOsnmaEnablement: " + isEnabled +
                    "Has ACCESS_ROBUST_LOCATION permission: " +
                    mContext.checkCallingPermission(ACCESS_ROBUST_LOCATION));
            if (mContext.checkCallingPermission(ACCESS_ROBUST_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED) {
                throw new SecurityException("Requires ACCESS_ROBUST_LOCATION_API permission");
            }
            boolean retVal = false;
            synchronized(sCallBacksLock) {
                retVal = mRemoteServiceClient.configOsnmaEnablement(isEnabled);
            }
            return retVal;
        }

        /**
         *
         * Precise location config
         *
         */
        public void enablePreciseLocation(NtripConfigData data) {
            Log.d(TAG, "enablePreciseLocation: Has ACCESS_PRECISE_LOCATION_API permission: " +
                    mContext.checkCallingPermission(ACCESS_PRECISE_LOCATION_API));
            if (mContext.checkCallingPermission(ACCESS_PRECISE_LOCATION_API) !=
                    PackageManager.PERMISSION_GRANTED) {
                throw new SecurityException("Requires ACCESS_PRECISE_LOCATION_API permission");
            }
            if (!mIZatServiceCtx.isPreciseLocationSupported()) {
                Log.e(TAG, "Izat Precise Location is not supported on this device.");
                throw new RuntimeException(
                        "Izat Precise Location is not supported on this device.");
            }
            mConfigData.ppEnabled = true;
            mConfigData.ppHostNameOrIP = data.mHostNameOrIP;
            mConfigData.ppMountPointName = data.mMountPointName;
            mConfigData.ppPort = data.mPort;
            mConfigData.ppUserName = data.mUserName;
            mConfigData.ppPassword = data.mPassword;
            mConfigData.ppRequiresInitialNMEA = data.mRequiresInitialNMEA;
            mConfigData.ppUseSSL = data.mUseSSL;
            mConfigData.ppNmeaUpdateInterval = data.mNmeaUpdateInterval;

            SaveConfigData();
            mRemoteServiceClient.enablePPENtripStream(data);
        }

        public void disablePreciseLocation() {
            Log.d(TAG, "disablePreciseLocation: Has ACCESS_PRECISE_LOCATION_API permission: " +
                    mContext.checkCallingPermission(ACCESS_PRECISE_LOCATION_API));
            if (mContext.checkCallingPermission(ACCESS_PRECISE_LOCATION_API) !=
                    PackageManager.PERMISSION_GRANTED) {
                throw new SecurityException("Requires ACCESS_PRECISE_LOCATION_API permission");
            }

            mConfigData.ppEnabled = true;
            mConfigData.ppHostNameOrIP = null;
            mConfigData.ppMountPointName = null;
            mConfigData.ppPort = 0;
            mConfigData.ppUserName = null;
            mConfigData.ppPassword = null;
            mConfigData.ppRequiresInitialNMEA = false;
            mConfigData.ppUseSSL = false;
            mConfigData.ppNmeaUpdateInterval = 0;

            SaveConfigData();
            mRemoteServiceClient.disablePPENtripStream();
        }

        public void updateNtripGgaConsent(boolean optIn) {
            Log.d(TAG, "updateNtripGgaConsent: " + "optIn: " + optIn +
                    "Has ACCESS_PRECISE_LOCATION_API permission: " +
                    mContext.checkCallingPermission(ACCESS_PRECISE_LOCATION_API));
            if (mContext.checkCallingPermission(ACCESS_PRECISE_LOCATION_API) !=
                    PackageManager.PERMISSION_GRANTED) {
                throw new SecurityException("Requires ACCESS_PRECISE_LOCATION_API permission");
            }
            Log.d(TAG, "EDGNSS updateNtripGgaConsent: " + optIn);

            mConfigData.ppOptInStatus = optIn? PrecisePositionOptInStatus.ON:
                    PrecisePositionOptInStatus.OFF;
            SaveConfigData();
            mRemoteServiceClient.updateNtripGgaConsent(optIn);
        }

        @Override
        public void setNetworkLocationUserConsent(boolean b) {
            Log.d(TAG, "setNetworkLocationUserConsent: " + b + "Has ACCESS_USER_CONSENT_API: " +
                    mContext.checkCallingPermission(USER_CONSENT_PERMISSION));
            if (mContext.checkCallingPermission(USER_CONSENT_PERMISSION) !=
                    PackageManager.PERMISSION_GRANTED) {
                throw new SecurityException("Requires ACCESS_USER_CONSENT_API");
            } else {
                mUserConsentManager.setAospUserConsent(b);
            }
        }

        @Override
        public void injectSuplCert(int suplCertId, byte[] suplCertData) {
            Log.d(TAG, "injectSuplCert: " + suplCertId + ", Has IZAT permission: " +
                    mContext.checkCallingPermission(IZAT_PERMISSION));
            if (mContext.checkCallingPermission(IZAT_PERMISSION) !=
                    PackageManager.PERMISSION_GRANTED) {
                throw new SecurityException("Requires ACCESS_USER_CONSENT_API");
            } else {
                mRemoteServiceClient.injectSuplCert(suplCertId, suplCertData);
            }
        }

        /**
         *
         * NTN status config
         *
         */
        public void set3rdPartyNtnCapability(boolean isCapable) {
            if (mContext.checkCallingPermission(ACCESS_SV_CONFIG_API) !=
                    PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "set3rdPartyNtnCapability requires ACCESS_SV_CONFIG_API permission");
                throw new SecurityException("Requires ACCESS_SV_CONFIG_API permission");
            }

            if (VERBOSE) {
                Log.d(TAG, "set3rdPartyNtnCapability: "
                        + mDataPerPackageAndUser.getPackageName(null) + ", isCapable: " +
                        isCapable);
            }
            mRemoteServiceClient.set3rdPartyNtnCapability(isCapable);
        }

        public void getNtnConfigSignalMask() {
            Log.d(TAG, "getNtnConfigSignalMask: " + mDataPerPackageAndUser.getPackageName(null)
                    + ", Has ACCESS_SV_CONFIG_API permission: " +
                    mContext.checkCallingPermission(ACCESS_SV_CONFIG_API));
            if (mContext.checkCallingPermission(ACCESS_SV_CONFIG_API) !=
                    PackageManager.PERMISSION_GRANTED) {
                throw new SecurityException("Requires ACCESS_SV_CONFIG_API permission");
            }

            synchronized(sCallBacksLock) {
                if (!isNtnApiSessionOngoing) {
                    isNtnApiSessionOngoing = true;
                    mRemoteServiceClient.getNtnConfigSignalMask();
                } else {
                    throw new RuntimeException("there is already one NTN API ongoing!");
                }
            }
        }

        public void setNtnConfigSignalMask(int gpsSignalTypeConfigMask) {
            Log.d(TAG, "setNtnConfigSignalMask: "
                    + mDataPerPackageAndUser.getPackageName(null) + ", Mask: " +
                    gpsSignalTypeConfigMask + ", Has ACCESS_SV_CONFIG_API permission: " +
                    mContext.checkCallingPermission(ACCESS_SV_CONFIG_API));

            if (mContext.checkCallingPermission(ACCESS_SV_CONFIG_API) !=
                    PackageManager.PERMISSION_GRANTED) {
                throw new SecurityException("Requires ACCESS_SV_CONFIG_API permission");
            }

            synchronized(sCallBacksLock) {
                if (!isNtnApiSessionOngoing) {
                    isNtnApiSessionOngoing = true;
                    mRemoteServiceClient.setNtnConfigSignalMask(gpsSignalTypeConfigMask);
                } else {
                    throw new RuntimeException("there is already one NTN API ongoing!");
                }
            }

        }
    };

    private void onRobustLocationConfigCb(RLConfigData rlConfigData) {

        synchronized (sCallBacksLock) {

            Log.d(TAG, "onRobustLocationConfigCb: " + mDataPerPackageAndUser.getPackageName(null));

            for (ClientGnssConfigData clData : mDataPerPackageAndUser.getAllData()) {
                if (VERBOSE) {
                    Log.d(TAG, "Invoking cb for client: " + clData.mPackageName);
                }
                try {
                    clData.mCallback.getRobustLocationConfigCb(rlConfigData);
                } catch (RemoteException e) {
                    Log.e(TAG, "onRobustLocationConfigCb: Failed to invoke cb");
                }
            }
        }
    }

    private void onNtnConfigSignalMaskResp(boolean isSuccess, int gpsSignalTypeConfigMask) {
        synchronized (sCallBacksLock) {
            Log.d(TAG, "ntnConfigSignalMaskResponse: " +
                    mDataPerPackageAndUser.getPackageName(null) + ", isSuccess: " +
                    isSuccess + ", mask: " + gpsSignalTypeConfigMask);

            for (ClientGnssConfigData clData : mDataPerPackageAndUser.getAllData()) {
                if (VERBOSE) {
                    Log.d(TAG, "Invoking cb for client: " + clData.mPackageName);
                }
                try {
                    clData.mCallback.ntnConfigSignalMaskResponse(isSuccess,
                            gpsSignalTypeConfigMask);
                } catch (RemoteException e) {
                    Log.e(TAG, "ntnConfigSignalMaskResponse: Failed to invoke cb");
                }
            }
            isNtnApiSessionOngoing = false;
        }
    }

    private void onNtnConfigSignalMaskChanged(int gpsSignalTypeConfigMask) {
        synchronized (sCallBacksLock) {
            Log.d(TAG, "ntnConfigSignalMaskChanged: " +
                    mDataPerPackageAndUser.getPackageName(null) + ", mask: " +
                    gpsSignalTypeConfigMask);

            for (ClientGnssConfigData clData : mDataPerPackageAndUser.getAllData()) {
                if (VERBOSE) {
                    Log.d(TAG, "Invoking cb for client: " + clData.mPackageName);
                }
                try {
                    clData.mCallback.ntnConfigSignalMaskChanged(gpsSignalTypeConfigMask);
                } catch (RemoteException e) {
                    Log.e(TAG, "ntnConfigSignalMaskChanged: Failed to invoke cb");
                }
            }
        }
    }

    class UserChanged implements DataPerPackageAndUser.UserChangeListener<ClientGnssConfigData> {
        @Override
        public void onUserChange(Map<String, ClientGnssConfigData> prevUserData,
                                 Map<String, ClientGnssConfigData> currentUserData) {
            if (VERBOSE) {
                Log.d(TAG, "Active user has changed, updating gnssConfig callbacks...");
            }

            synchronized (sCallBacksLock) {
                // Remove prevUser callbacks
                for (ClientGnssConfigData gnssConfigData: prevUserData.values()) {
                    mGnssConfigCallbacks.unregister(gnssConfigData.mCallback);
                }

                // Add back current user callbacks
                for (ClientGnssConfigData gnssConfigData: currentUserData.values()) {
                    mGnssConfigCallbacks.register(gnssConfigData.mCallback);
                }
            }
        }
    }

    public IGnssConfigService getGnssConfigBinder() {
        return mBinder;
    }

    /* =================================================
     *   AIDL Client
     * =================================================*/
    static private class GnssConfigServiceIdlClient extends LocIDLClientBase
            implements LocIDLClientBase.IServiceDeathCb {
        private GnssConfigServiceCallback mGnssCfgServiceCallback;
        private ILocAidlGnssConfigService mGnssConfigServiceIface;
        private GnssConfigService mService;

        private GnssConfigServiceIdlClient(GnssConfigService service) {
            getGnssConfigServiceIface();
            mGnssCfgServiceCallback = new GnssConfigServiceCallback();
            mService = service;
            try {
                if (null != mGnssConfigServiceIface) {
                    mGnssConfigServiceIface.init(mGnssCfgServiceCallback);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Exception on GnssConfigService init: " + e);
            }
            registerServiceDiedCb(this);
        }

        private void getGnssConfigServiceIface() {
            if (null == mGnssConfigServiceIface) {
                ILocAidlGnss gnssService = (ILocAidlGnss) getGnssAidlService();
                if (null != gnssService) {
                    try {
                        mGnssConfigServiceIface = gnssService.getExtensionLocAidlGnssConfigService();
                    } catch (RemoteException e) {
                        Log.e(TAG, "Exception getting gnss config service: " + e);
                        mGnssConfigServiceIface = null;
                    }
                }
            }
        }

        @Override
        public void onServiceDied() {
            Log.e(TAG, "ILocAidlGnssConfigService died");
            mGnssConfigServiceIface = null;
            getGnssConfigServiceIface();
            try {
                if (null != mGnssConfigServiceIface) {
                    mGnssConfigServiceIface.init(mGnssCfgServiceCallback);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Exception on GnssConfig Service init: " + e);
            }
            mService.restoreConfigData();
            mService.isNtnApiSessionOngoing = false;
        }

        public void getRobustLocationConfig() {
            try {
                if (null != mGnssConfigServiceIface) {
                    mGnssConfigServiceIface.getRobustLocationConfig();
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Exception getRobustLocationConfig: " + e);
            }
        }

        public void setRobustLocationConfig(boolean enable, boolean enableForE911) {
            IDLClientUtils.toIDLService(TAG);
            try {
                if (null != mGnssConfigServiceIface) {
                    mGnssConfigServiceIface.setRobustLocationConfig(enable, enableForE911);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Exception setRobustLocationConfig: " + e);
            }
        }

        public void updateNtripGgaConsent(boolean consentAccepted) {
            try {
                Log.d(TAG, "EDGNSS updateNtripGgaConsent: " + consentAccepted);
                if (null != mGnssConfigServiceIface) {
                    mGnssConfigServiceIface.updateNTRIPGGAConsent(consentAccepted);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Exception updateNtripGgaConsent: " + e);
            }
        }

        public void enablePPENtripStream(NtripConfigData data) {
            try {
                LocAidlNtripConnectionParams params = new LocAidlNtripConnectionParams();
                params.requiresNmeaLocation = data.mRequiresInitialNMEA;
                params.hostNameOrIp = data.mHostNameOrIP;
                params.mountPoint = data.mMountPointName;
                params.username = data.mUserName;
                params.password = data.mPassword;
                params.port = data.mPort;
                params.useSSL = data.mUseSSL;
                params.nmeaUpdateInterval = data.mNmeaUpdateInterval;
                if (null != mGnssConfigServiceIface) {
                    mGnssConfigServiceIface.enablePPENtripStream(params, false);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Exception enablePPENtripStream: " + e);
            }
        }

        public void disablePPENtripStream() {
            IDLClientUtils.toIDLService(TAG);
            try {
                if (null != mGnssConfigServiceIface) {
                    mGnssConfigServiceIface.disablePPENtripStream();
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Exception disablePPENtripStream: " + e);
            }
        }

        public boolean configMerkleTree(String merkleTreeXml, int xmlSize) {
            IDLClientUtils.toIDLService(TAG);
            try {
                if (null != mGnssConfigServiceIface) {
                    return mGnssConfigServiceIface.configMerkleTree(merkleTreeXml, xmlSize);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Exception configMerkleTree: " + e);
            }
            return false;
        }

        public boolean configOsnmaEnablement(boolean isEnabled) {
            IDLClientUtils.toIDLService(TAG);
            try {
                if (null != mGnssConfigServiceIface) {
                    return mGnssConfigServiceIface.configOsnmaEnablement(isEnabled);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Exception configOsnmaEnablement: " + e);
            }
            return false;
        }

        public void set3rdPartyNtnCapability(boolean isCapable) {
            IDLClientUtils.toIDLService(TAG);
            try {
                if (null != mGnssConfigServiceIface) {
                    mGnssConfigServiceIface.set3rdPartyNtnCapability(isCapable);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Exception getNtnConfigSignalMask: " + e);
            }
        }

        public void getNtnConfigSignalMask() {
            IDLClientUtils.toIDLService(TAG);
            try {
                if (null != mGnssConfigServiceIface) {
                    mGnssConfigServiceIface.getNtnConfigSignalMask();
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Exception getNtnConfigSignalMask: " + e);
                mService.isNtnApiSessionOngoing = false;
            }
        }

        public void setNtnConfigSignalMask(int gpsSignalTypeConfigMask) {
            IDLClientUtils.toIDLService(TAG);
            try {
                if (null != mGnssConfigServiceIface) {
                    mGnssConfigServiceIface.setNtnConfigSignalMask(gpsSignalTypeConfigMask);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Exception setNtnConfigSignalMask: " + e);
                mService.isNtnApiSessionOngoing = false;
            }
        }

        public void injectSuplCert(int suplCertId, byte[] suplCertData) {
            IDLClientUtils.toIDLService(TAG);
            try {
                if (null != mGnssConfigServiceIface) {
                    mGnssConfigServiceIface.injectSuplCert(suplCertId, suplCertData);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Exception injectSuplCert: " + e);
            }
        }
        /* =================================================
        *   AIDL Callback
        * =================================================*/
        private class GnssConfigServiceCallback extends ILocAidlGnssConfigServiceCallback.Stub {

            public GnssConfigServiceCallback() {
            }
            @Override
            public void getGnssSvTypeConfigCb(byte[] disabledSvTypeList)
                    throws android.os.RemoteException {
            }

            public void getRobustLocationConfigCb(LocAidlRobustLocationInfo info) {
                RLConfigData rlConfigData = new RLConfigData();
                rlConfigData.validMask = info.validMask;
                rlConfigData.enableStatus = info.enable;
                rlConfigData.enableStatusForE911 = info.enableForE911;
                rlConfigData.major = info.major;
                rlConfigData.minor = info.minor;

                mService.onRobustLocationConfigCb(rlConfigData);
            }

            @Override
            public final int getInterfaceVersion() {
                return ILocAidlGnssConfigServiceCallback.VERSION;
            }
            @Override
            public final String getInterfaceHash() {
                return ILocAidlGnssConfigServiceCallback.HASH;
            }

            public void ntnConfigSignalMaskResponse(boolean isSuccess,
                    int gpsSignalTypeConfigMask) {
                IDLClientUtils.fromIDLService(TAG);
                mService.onNtnConfigSignalMaskResp(isSuccess, gpsSignalTypeConfigMask);
            }

            public void ntnConfigSignalMaskChanged(int gpsSignalTypeConfigMask) {
                IDLClientUtils.fromIDLService(TAG);
                mService.onNtnConfigSignalMaskChanged(gpsSignalTypeConfigMask);
            }
        }
    }
}
