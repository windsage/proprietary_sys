/*====*====*====*====*====*====*====*====*====*====*====*====*====*====*====*
  Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
  All rights reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
=============================================================================*/
package com.qualcomm.location.izat.gtp;

import android.content.Context;
import android.location.Location;
import android.os.Binder;
import android.os.RemoteException;
import android.util.Log;
import java.util.List;
import com.qti.gtp.IGTPService;
import com.qti.gtp.IGTPServiceCallback;
import com.qti.gtp.GtpRequestData;
import com.qti.gtp.GTPAccuracy;
import android.content.pm.PackageManager;

import com.qualcomm.location.izat.CallbackData;
import com.qualcomm.location.izat.UserConsentManager;
import com.qualcomm.location.izat.UserConsentManager.WwanAppInfoListener;
import com.qualcomm.location.izat.UserConsentManager.WwanAppInfo;
import com.qualcomm.location.izatprovider.IzatProvider;
import com.qualcomm.location.policy.ISessionOwner;
import com.qualcomm.location.policy.Policy;
import com.qualcomm.location.policy.SessionPolicyManager;
import com.qualcomm.location.policy.SessionRequest;
import com.qualcomm.location.izat.IzatService;
import android.os.Handler;
import android.os.Message;
import com.qualcomm.location.utils.IZatServiceContext;
import static com.qualcomm.location.utils.IZatServiceContext.MSG_IZAT_PROVIDER_BASE;

import static com.qualcomm.location.policy.SessionRequest.RequestPrecision;
import static com.qualcomm.location.policy.SessionRequest.RequestPrecision.REQUEST_PRECISION_COARSE;
import static com.qualcomm.location.policy.SessionRequest.RequestPrecision.REQUEST_PRECISION_FINE;
import static com.qualcomm.location.policy.SessionRequest.RequestType.*;
import static com.qualcomm.location.policy.SessionRequest.SessionType.*;



public class GtpServiceProvider implements IzatService.ISystemEventListener, ISessionOwner,
         WwanAppInfoListener, Handler.Callback {
    private static final String TAG = "GtpServiceProvider";
    private static final boolean VERBOSE = Log.isLoggable(TAG, Log.VERBOSE);
    private static final String ACCESS_FINE_LOCATION =
            "android.permission.ACCESS_FINE_LOCATION";
    private static final String ACCESS_BACKGROUND_LOCATION =
            "android.permission.ACCESS_BACKGROUND_LOCATION";

    private final SessionPolicyManager mSessionPolicyManager;
    private final UserConsentManager mUserConsentManager;

    private static final int MSG_STOP_SESSION = MSG_IZAT_PROVIDER_BASE + 9;
    private static final int MSG_SET_APPINFO = MSG_IZAT_PROVIDER_BASE + 10;

    private final IzatProvider mProvider;
    private static GtpServiceProvider sInstance = null;
    private final Context mContext;
    private IZatServiceContext mIZatServiceCtx = null;
    private Handler mHandler;

    public static GtpServiceProvider getInstance(
            Context ctx,
            SessionPolicyManager sessionPolicyManager) {
        if (null == sInstance) {
            sInstance = new GtpServiceProvider(ctx, sessionPolicyManager);
        }
        return sInstance;
    }

    private GtpServiceProvider(Context ctx,
                               SessionPolicyManager sessionPolicyManager) {
        this.mSessionPolicyManager = sessionPolicyManager;
        this.mProvider = IzatProvider.getNetworkProvider(ctx);
        this.mProvider.onLoad();
        this.mContext = ctx;
        this.mUserConsentManager = UserConsentManager.getInstance(ctx);
        mIZatServiceCtx = IZatServiceContext.getInstance(ctx);
        mHandler = new Handler(mIZatServiceCtx.getLooper(), this);
        IzatService.AidlClientDeathNotifier.getInstance().registerAidlClientDeathCb(this);
        mUserConsentManager.registerWwanAppInfoListener(this);
    }

    public IGTPService getGtpBinder() {
        return mBinder;
    }

    @Override
    public void onAidlClientDied(String packageName, int pid, int uid) {
        Log.d(TAG, "onAidlClientDied - package " + packageName);
        mSessionPolicyManager.stopSessionsByPiduid(pid, uid);
    }

    private ISessionOwner sessionOwner() {
        return this;
    }

    @Override
    public void onWwanAppInfoListUpdated(List<WwanAppInfo> appInfoList) {
        mProvider.onSetMultiplexRequest(appInfoList);
    }

    @Override
    public boolean handleMessage (Message msg) {
        int msgID = msg.what;
        switch (msgID) {
            case MSG_STOP_SESSION:
                Log.d(TAG, "MSG_STOP_SESSION");
                SessionRequest sessionRequest = new SessionRequest(
                        msg.arg1,
                        msg.arg2,
                        (String)msg.obj,
                        SESSION_TYPE_WWAN,
                        REMOVE_LOCATION_UPDATES,
                        sessionOwner());

                sessionRequest.mParams.precision = REQUEST_PRECISION_COARSE;

                Log.d(TAG, "removeWwanLocationUpdates: calling SPM.process with" +
                        " session ID: " + sessionRequest.getUniqueId());
                mSessionPolicyManager.process(sessionRequest);
                break;
            case MSG_SET_APPINFO:
                int pid = msg.arg1;
                int uid = msg.arg2;
                String packageName = mContext.getPackageManager().getNameForUid(uid);
                String appHash = mIZatServiceCtx.getLicenseeHash(pid, uid);
                mUserConsentManager.setWwanAppInfo(packageName, pid, uid, appHash,
                        SESSION_TYPE_INVALID, RequestPrecision.REQUEST_PRECISION_INVALID);
                break;
            default:
                break;
        }
        return true;
    }

    private final IGTPService.Stub mBinder = new IGTPService.Stub() {

        @Override
        public void requestLocationUpdates(
                IGTPServiceCallback gtpServiceCallback,
                GtpRequestData gtpReq) {
            Log.d(TAG, "requestLocationUpdates: "
                    + "mMinIntervalMillis: " + gtpReq.getMinIntervalMillis() + ",mAccuracy: " +
                    gtpReq.getAccuracy()  + ", Has ACCESS_FINE_LOCATION permission: " +
                    mContext.checkCallingPermission(ACCESS_FINE_LOCATION));
            if (mContext.checkCallingPermission(ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED) {
                throw new SecurityException("Requires ACCESS_FINE_LOCATION permission");
            }
            SessionRequest sessionRequest = new SessionRequest(
                    Binder.getCallingPid(),
                    Binder.getCallingUid(),
                    mContext.getPackageManager()
                            .getNameForUid(Binder.getCallingUid()),
                    SESSION_TYPE_GTP,
                    REQUEST_LOCATION_UPDATES,
                    sessionOwner());

            sessionRequest.mParams.minIntervalMillis = gtpReq.getMinIntervalMillis();
            sessionRequest.mParams.callback
                    = new GtpCallbackData(gtpServiceCallback);
            sessionRequest.mParams.precision = (gtpReq.getAccuracy() == GTPAccuracy.HIGH) ?
                    REQUEST_PRECISION_FINE : REQUEST_PRECISION_COARSE;

            Log.v(TAG, "requestLocationUpdates: calling SPM.process with" +
                    " session ID: " + sessionRequest.getUniqueId());
            mSessionPolicyManager.process(sessionRequest);
        }

        @Override
        public void requestWwanLocationUpdates(
                IGTPServiceCallback gtpServiceCallback,
                GtpRequestData gtpReq) {
            Log.v(TAG, "requestWwanLocationUpdates: package name: " +
                    mContext.getPackageManager().getNameForUid(Binder.getCallingUid()) +
                    ", userId: " + Binder.getCallingUid() + "requestWwanLocationUpdates: "
                    + "mMinIntervalMillis: " + gtpReq.getMinIntervalMillis() + ", mAccuracy: " +
                    gtpReq.getAccuracy()  + ", Has ACCESS_FINE_LOCATION permission: " +
                    mContext.checkCallingPermission(ACCESS_FINE_LOCATION) +
                    ", Has ACCESS_BACKGROUND_LOCATION permission: " +
                    mContext.checkCallingPermission(ACCESS_BACKGROUND_LOCATION) +
                    ", Has User Consent permission: " + (mUserConsentManager.getAospUserConsent() ||
                    mUserConsentManager.getPackageUserConsent(
                            mContext.getPackageManager().getNameForUid(Binder.getCallingUid()),
                            Binder.getCallingUid())));
            if (mContext.checkCallingPermission(ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Requires ACCESS_FINE_LOCATION permission");
                throw new SecurityException("Requires ACCESS_FINE_LOCATION permission");
            } else if (mContext.checkCallingPermission(ACCESS_BACKGROUND_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Requires ACCESS_BACKGROUND_LOCATION permission");
                throw new SecurityException("Requires ACCESS_BACKGROUND_LOCATION permission");
            } else if (!mUserConsentManager.getAospUserConsent() &&
                    !mUserConsentManager.getPackageUserConsent(
                            mContext.getPackageManager().getNameForUid(Binder.getCallingUid()),
                            Binder.getCallingUid())) {
                Log.e(TAG, "Requires USER_CONSENT permission, AOSPUserConsent: " +
                        mUserConsentManager.getAospUserConsent() + ", PackageUserConsent: " +
                        mUserConsentManager.getPackageUserConsent(
                            mContext.getPackageManager().getNameForUid(Binder.getCallingUid()),
                            Binder.getCallingUid()));
                throw new SecurityException("Requires USER_CONSENT permission");
            }
            SessionRequest sessionRequest = new SessionRequest(
                    Binder.getCallingPid(),
                    Binder.getCallingUid(),
                    mContext.getPackageManager()
                            .getNameForUid(Binder.getCallingUid()),
                    SESSION_TYPE_WWAN,
                    REQUEST_LOCATION_UPDATES,
                    sessionOwner());

            sessionRequest.mParams.minIntervalMillis = gtpReq.getMinIntervalMillis();
            GtpCallbackData Cb = new GtpCallbackData(gtpServiceCallback);
            Cb.pid = Binder.getCallingPid();
            Cb.uid = Binder.getCallingUid();
            Cb.pkgName = mContext.getPackageManager().getNameForUid(Binder.getCallingUid());
            sessionRequest.mParams.callback = Cb;
            sessionRequest.mParams.precision = (gtpReq.getAccuracy() == GTPAccuracy.HIGH) ?
                    REQUEST_PRECISION_FINE : REQUEST_PRECISION_COARSE;

            Log.v(TAG, "requestWwanLocationUpdates: calling SPM.process with" +
                    " session ID: " + sessionRequest.getUniqueId());
            mSessionPolicyManager.process(sessionRequest);
        }

        @Override
        public void removeLocationUpdates() {
            SessionRequest sessionRequest = new SessionRequest(
                    Binder.getCallingPid(),
                    Binder.getCallingUid(),
                    mContext.getPackageManager()
                            .getNameForUid(Binder.getCallingUid()),
                    SESSION_TYPE_GTP,
                    REMOVE_LOCATION_UPDATES,
                    sessionOwner());

            sessionRequest.mParams.precision = REQUEST_PRECISION_COARSE;

            Log.v(TAG, "removeLocationUpdates: calling SPM.process with" +
                    " session ID: " + sessionRequest.getUniqueId());

            mSessionPolicyManager.process(sessionRequest);
        }

        @Override
        public void removeWwanLocationUpdates() {
            SessionRequest sessionRequest = new SessionRequest(
                    Binder.getCallingPid(),
                    Binder.getCallingUid(),
                    mContext.getPackageManager()
                            .getNameForUid(Binder.getCallingUid()),
                    SESSION_TYPE_WWAN,
                    REMOVE_LOCATION_UPDATES,
                    sessionOwner());

            sessionRequest.mParams.precision = REQUEST_PRECISION_COARSE;

            Log.v(TAG, "removeWwanLocationUpdates: calling SPM.process with" +
                    " session ID: " + sessionRequest.getUniqueId());

            mSessionPolicyManager.process(sessionRequest);
        }

        @Override
        public void requestPassiveLocationUpdates(
                IGTPServiceCallback gtpServiceCallback) {
            Log.d(TAG, "requestPassiveLocationUpdates: "
                    + ", Has ACCESS_FINE_LOCATION permission: " +
                    mContext.checkCallingPermission(ACCESS_FINE_LOCATION));
            if (mContext.checkCallingPermission(ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED) {
                throw new SecurityException("Requires ACCESS_FINE_LOCATION permission");
            }
            SessionRequest sessionRequest = new SessionRequest(
                    Binder.getCallingPid(),
                    Binder.getCallingUid(),
                    mContext.getPackageManager()
                            .getNameForUid(Binder.getCallingUid()),
                    SESSION_TYPE_GTP,
                    REQUEST_PASSIVE_LOCATION_UPDATES,
                    sessionOwner());

            sessionRequest.mParams.callback
                    = new GtpCallbackData(gtpServiceCallback);
            sessionRequest.mParams.precision = REQUEST_PRECISION_COARSE;

            Log.d(TAG, "requestPassiveLocationUpdates: calling SPM.process" +
                    " with session ID: " + sessionRequest.getUniqueId());

            mSessionPolicyManager.process(sessionRequest);
        }

        @Override
        public void requestPassiveWwanLocationUpdates(
                IGTPServiceCallback gtpServiceCallback) {
            Log.v(TAG, "requestPassiveWwanLocationUpdates: package name: " +
                    mContext.getPackageManager().getNameForUid(Binder.getCallingUid()) +
                    ", userId: " + Binder.getCallingUid() + "requestPassiveWwanLocationUpdates: "
                    + ", Has ACCESS_FINE_LOCATION permission: " +
                    mContext.checkCallingPermission(ACCESS_FINE_LOCATION) +
                    ", Has ACCESS_BACKGROUND_LOCATION permission: " +
                    mContext.checkCallingPermission(ACCESS_BACKGROUND_LOCATION) +
                    ", Has User Consent permission: " + (mUserConsentManager.getAospUserConsent() ||
                    mUserConsentManager.getPackageUserConsent(
                            mContext.getPackageManager().getNameForUid(Binder.getCallingUid()),
                            Binder.getCallingUid())));
            if (mContext.checkCallingPermission(ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Requires ACCESS_FINE_LOCATION permission");
                throw new SecurityException("Requires ACCESS_FINE_LOCATION permission");
            } else if (mContext.checkCallingPermission(ACCESS_BACKGROUND_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Requires ACCESS_BACKGROUND_LOCATION permission");
                throw new SecurityException("Requires ACCESS_BACKGROUND_LOCATION permission");
            } else if (!mUserConsentManager.getAospUserConsent() &&
                    !mUserConsentManager.getPackageUserConsent(
                            mContext.getPackageManager().getNameForUid(Binder.getCallingUid()),
                            Binder.getCallingUid())) {
                Log.e(TAG, "Requires USER_CONSENT permission");
                throw new SecurityException("Requires USER_CONSENT permission");
            }
            SessionRequest sessionRequest = new SessionRequest(
                    Binder.getCallingPid(),
                    Binder.getCallingUid(),
                    mContext.getPackageManager()
                            .getNameForUid(Binder.getCallingUid()),
                    SESSION_TYPE_WWAN,
                    REQUEST_PASSIVE_LOCATION_UPDATES,
                    sessionOwner());

            sessionRequest.mParams.callback
                    = new GtpCallbackData(gtpServiceCallback);
            sessionRequest.mParams.precision = REQUEST_PRECISION_COARSE;

            Log.d(TAG, "requestPassiveWwanLocationUpdates: calling SPM.process" +
                    " with session ID: " + sessionRequest.getUniqueId());

            mSessionPolicyManager.process(sessionRequest);
        }

        @Override
        public void removePassiveLocationUpdates() {

            SessionRequest sessionRequest = new SessionRequest(
                    Binder.getCallingPid(),
                    Binder.getCallingUid(),
                    mContext.getPackageManager()
                            .getNameForUid(Binder.getCallingUid()),
                    SESSION_TYPE_GTP,
                    REMOVE_PASSIVE_LOCATION_UPDATES,
                    sessionOwner());
            sessionRequest.mParams.precision = REQUEST_PRECISION_COARSE;

            Log.d(TAG, "removePassiveLocationUpdates: calling SPM.process" +
                    " with session ID: " + sessionRequest.getUniqueId());

            mSessionPolicyManager.process(sessionRequest);
        }

        @Override
        public void removePassiveWwanLocationUpdates() {
            SessionRequest sessionRequest = new SessionRequest(
                    Binder.getCallingPid(),
                    Binder.getCallingUid(),
                    mContext.getPackageManager()
                            .getNameForUid(Binder.getCallingUid()),
                    SESSION_TYPE_WWAN,
                    REMOVE_PASSIVE_LOCATION_UPDATES,
                    sessionOwner());
            sessionRequest.mParams.precision = REQUEST_PRECISION_COARSE;

            Log.d(TAG, "removePassiveWwanLocationUpdates: calling SPM.process" +
                    " with session ID: " + sessionRequest.getUniqueId());

            mSessionPolicyManager.process(sessionRequest);
        }

        @Override
        public void setUserConsent(boolean b) {

            Log.d(TAG, "Calling mUserConsentManager.setPackageUserConsent: " + b);
            int callingUid = Binder.getCallingUid();
            mUserConsentManager.setPackageUserConsent(
                    b,
                    mContext.getPackageManager().getNameForUid(callingUid),
                    callingUid);
        }
    };

    @Override
    public void handle(SessionRequest sessionRequest) {

        Log.d(TAG, "handle:  " + sessionRequest.mIdentity.requestType.name()
                + " session id : " + sessionRequest.getUniqueId());

        mProvider.onSetSessionRequest(sessionRequest);
    }

    @Override
    public boolean isPolicyApplicable(Policy.PolicyName policyName, SessionRequest request) {
        switch (policyName) {

            case POLICY_NAME_USER_PROFILE_CHANGE:
            case POLICY_NAME_POWER_SAVE_MODE:
            case POLICY_NAME_PERMISSION_CHANGE:
            case POLICY_NAME_BACKGROUND_THROTTLING:
            case POLICY_NAME_LOCATION_SETTING_CHANGE:
            case POLICY_NAME_POWER_BLAME_REPORTING:
            case POLICY_NAME_APPOPS_CHANGE:
                return true;

            default:
                return false;
        }
    }

    public void setWwanAppInfo(int pid, int uid) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_APPINFO, pid, uid));
    }


    private class GtpCallbackData
            extends CallbackData implements SessionRequest.SessionCallback {

        public boolean isSingleShot;
        public int pid;
        public int uid;
        public String pkgName;

        public GtpCallbackData(IGTPServiceCallback callback) {
            isSingleShot = false;
            super.mCallback = callback;
        }

        @Override
        public void onLocationAvailable(Location location) {

            try {
                Log.d(TAG, "GtpCallbackData onLocationAvailable");
                ((IGTPServiceCallback) mCallback).onLocationAvailable(location);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
            if (isSingleShot) {
                mHandler.obtainMessage(MSG_STOP_SESSION, pid, uid, pkgName).sendToTarget();
            }
        }
    }
}
