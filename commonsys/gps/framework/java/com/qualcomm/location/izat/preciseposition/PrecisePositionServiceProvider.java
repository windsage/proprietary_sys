/*====*====*====*====*====*====*====*====*====*====*====*====*====*====*====*
  Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
  All rights reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
=============================================================================*/

package com.qualcomm.location.izat.preciseposition;

import android.location.Location;
import android.location.LocationManager;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Handler;
import android.content.Context;
import android.util.Log;
import android.os.RemoteException;
import android.os.Bundle;

import com.qualcomm.location.izat.IzatService;
import com.qualcomm.location.policy.ISessionOwner;
import com.qualcomm.location.policy.SessionPolicyManager;
import com.qualcomm.location.policy.SessionRequest;
import com.qualcomm.location.policy.SessionRequest.SessionType;
import com.qualcomm.location.policy.SessionRequest.RequestType;
import com.qualcomm.location.policy.SessionRequest.RequestPrecision;
import com.qualcomm.location.policy.SessionRequest.CorrectionType;
import com.qualcomm.location.policy.Policy;
import com.qualcomm.location.idlclient.LocIDLClientBase.*;
import com.qualcomm.location.idlclient.*;
import com.qti.preciseposition.IPrecisePositionService;
import com.qti.preciseposition.IPrecisePositionCallback;

import vendor.qti.gnss.ILocAidlPrecisePositionServiceCallback;
import vendor.qti.gnss.ILocAidlPrecisePositionService;
import vendor.qti.gnss.ILocAidlPrecisePositionServiceCallback.LocAidlPrecisePositionSessionResponse;
import vendor.qti.gnss.LocAidlLocationFlagsBits;
import vendor.qti.gnss.LocAidlLocation;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

public class PrecisePositionServiceProvider implements IzatService.ISystemEventListener,
                                            ISessionOwner{
    private static final String TAG = "PrecisePositionServiceProvider";
    private static final boolean VERBOSE = Log.isLoggable(TAG, Log.VERBOSE);
    private final Context mContext;
    private PrecisePositionIDLClient mPrecisePositionIDLClient;
    private SessionPolicyManager mSessionPolicyManager;
    private List<SessionRequest> mSessionRequests;
    private static final String ACCESS_FINE_LOCATION = "android.permission.ACCESS_FINE_LOCATION";

    private static PrecisePositionServiceProvider sInstance = null;
    public  static PrecisePositionServiceProvider getInstance(Context ctx,
                           SessionPolicyManager sessionPolicyMgr) {
        if (sInstance == null) {
            sInstance = new PrecisePositionServiceProvider(ctx, sessionPolicyMgr);
        }
        return sInstance;
    }

    private PrecisePositionServiceProvider(Context ctx, SessionPolicyManager sessionPolicyMgr) {
        mContext = ctx;

        if (LocIDLClientBase.getIDLServiceVersion().compareTo(IDLServiceVersion.V_AIDL) >= 0) {
            mPrecisePositionIDLClient = new PrecisePositionIDLClient(this);
            mSessionPolicyManager = sessionPolicyMgr;
            mSessionRequests = new ArrayList<SessionRequest>();
            IzatService.AidlClientDeathNotifier.getInstance().registerAidlClientDeathCb(this);
        } else {
            Log.e(TAG, "Unsupported IDL Service Version.");
        }
    }

    public IPrecisePositionService getPrecisePositionBinder() {
        return mBinder;
    }

    @Override
    public void onAidlClientDied(String packageName, int pid, int uid) {
        Log.d(TAG, "onAidlClientDied - package:" + packageName + ", pid:" + pid + ", uid:" + uid);
        mSessionPolicyManager.stopSessionsByPiduid(pid, uid);
    }

    private ISessionOwner sessionOwner() {
        return this;
    }

    private int sessionTypeToInt(SessionType sessionType) {
        int preciseType = 0;
        switch (sessionType) {
        case SESSION_TYPE_EDGNSS:
            preciseType = 1;
            break;
        case SESSION_TYPE_PPE:
            preciseType = 2;
            break;
        case SESSION_TYPE_WOCS:
            preciseType = 3;
            break;
        default:
            break;
        }
        return preciseType;
    }

    private SessionType sessionTypeFromInt(int type) {
        SessionType sessionType= SessionType.SESSION_TYPE_INVALID;
        switch (type) {
        case 1:
            sessionType = SessionType.SESSION_TYPE_EDGNSS;
            break;
        case 2:
            sessionType = SessionType.SESSION_TYPE_PPE;
            break;
        case 3:
            sessionType = SessionType.SESSION_TYPE_WOCS;
            break;
        default:
            break;
        }
        return sessionType;
    }

    /* Remote binder */
    private final IPrecisePositionService.Stub mBinder = new IPrecisePositionService.Stub() {
        @Override
        public void startPrecisePositioningSession(IPrecisePositionCallback callback, long tbfMsec,
                                    int preciseType, int correctionType) {
             Log.d(TAG, "startPrecisePositionSession: "
                    + "IntervalMillis: " + tbfMsec + ", precise type: " +
                    preciseType  + ", correction type: " + correctionType +
                    ", Has ACCESS_FINE_LOCATION permission: " +
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
                    sessionTypeFromInt(preciseType),
                    RequestType.REQUEST_LOCATION_UPDATES,
                    sessionOwner());

            sessionRequest.mParams.callback = new PrecisePositionCallbackData(callback);
            sessionRequest.mParams.precision = RequestPrecision.REQUEST_PRECISION_FINE;
            sessionRequest.mParams.correctionType = CorrectionType.fromInt(correctionType);
            sessionRequest.mParams.minIntervalMillis = tbfMsec;

            Log.v(TAG, "startPrecisePositionSession: calling with" +
                    " session ID: " + sessionRequest.getUniqueId());
            mSessionPolicyManager.process(sessionRequest);
        }

        @Override
        public void stopPrecisePositioningSession() {
            Log.d(TAG, "stopPrecisePositioningSession: pid:" + Binder.getCallingPid() + ", uid:" +
                    Binder.getCallingUid() + ", packageName:" + mContext.getPackageManager()
                            .getNameForUid(Binder.getCallingUid()));
            SessionRequest existingRequest = getCurrentSessionRequest();
            if (null != existingRequest) {
                SessionRequest removeRequest = new SessionRequest(existingRequest);
                removeRequest.mIdentity.requestType = RequestType.REMOVE_LOCATION_UPDATES;
                Log.v(TAG, "removeLocationUpdates: calling with" +
                    " session ID: " + removeRequest.getUniqueId());
                mSessionPolicyManager.process(removeRequest);
            } else {
                Log.i(TAG, "cannot find session started from this app");
            }
        }
    };
    //=============================================================
    // ISessionOwner methods
    //=============================================================
    @Override
    public void handle(SessionRequest request) {
        switch(request.mIdentity.requestType) {
        case REQUEST_LOCATION_UPDATES:
            startSession(request);
            break;
        case REMOVE_LOCATION_UPDATES:
            stopSession(request);
            break;
        default:
            Log.e(TAG, "Unsupported session request type.");
            break;
        }
    }

     @Override
    public boolean isPolicyApplicable(Policy.PolicyName policyName, SessionRequest request) {
        switch (policyName) {
        case POLICY_NAME_USER_PROFILE_CHANGE:
        case POLICY_NAME_LOCATION_SETTING_CHANGE:
        case POLICY_NAME_BACKGROUND_STARTSTOP:
        case POLICY_NAME_PERMISSION_CHANGE:
        case POLICY_NAME_POWER_SAVE_MODE:
        case POLICY_NAME_POWER_BLAME_REPORTING:
        case POLICY_NAME_APPOPS_CHANGE:
            return true;
        default:
            return false;
        }
    }

    private SessionRequest getCurrentSessionRequest() {
        Log.d(TAG, "session request size:" + mSessionRequests.size() + ", pid:" +
                Binder.getCallingPid() + ", uid:" + Binder.getCallingUid() + "packageName:" +
                mContext.getPackageManager().getNameForUid(Binder.getCallingUid()));
        SessionRequest existingRequest = null;
        for (SessionRequest r : mSessionRequests) {
            Log.d(TAG, "request: pid:" + r.mIdentity.pid + ", uid:" + r.mIdentity.uid +
                  ", packageName:" +
                  mContext.getPackageManager().getNameForUid(Binder.getCallingUid()));
            if ((r.mIdentity.pid == Binder.getCallingPid()) &&
                (r.mIdentity.uid == Binder.getCallingUid()) &&
                (r.mIdentity.packageName.equals(mContext.getPackageManager().getNameForUid(
                     Binder.getCallingUid())))) {
                existingRequest = r;
                break;
            }
        }
        return existingRequest;
    }

    //=============================================================
    // Requests down to Precise Position Vendor Service
    //=============================================================
    private void startSession(SessionRequest request) {
        Log.i(TAG, "Starting session (pid: " + request.mIdentity.pid +
                ", uid: " + request.mIdentity.uid +
                ", sessionType: " + request.mIdentity.sessionType +
                ", requestType: " + request.mIdentity.requestType +
                ", minIntervalMillis: " + request.mParams.minIntervalMillis +
                ", precision: " + request.mParams.precision +
                ", correctionType:" + request.mParams.correctionType + ")");

        // Look for any existing request
        SessionRequest existingRequest = null;
        Iterator<SessionRequest> iterator = mSessionRequests.iterator();
        while (iterator.hasNext()) {
            SessionRequest r = iterator.next();
            if (r.getUniqueId() == request.getUniqueId()) {
                Log.d(TAG, "Found Existing Request matching pid/uid");
                existingRequest = r;
                break;
            }
        }
        if (null == existingRequest) {
            if (request.mIdentity.sessionType == SessionType.SESSION_TYPE_PPE ||
                request.mIdentity.sessionType == SessionType.SESSION_TYPE_EDGNSS ||
                request.mIdentity.sessionType == SessionType.SESSION_TYPE_WOCS) {
                mSessionRequests.add(request);
                // find first request
                SessionRequest firstRequest = mSessionRequests.get(0);
                mPrecisePositionIDLClient.startSession(firstRequest);
            }
        } else {
            Log.i(TAG, "This pid/uid already has a request, drop it");
        }
    }

    private void stopSession(SessionRequest request) {
        Log.i(TAG, "Stopping session (pid: " + request.mIdentity.pid +
                ", uid: " + request.mIdentity.uid +
                ", sessionType: " + request.mIdentity.sessionType +
                ", requestType: " + request.mIdentity.requestType +
                ", minIntervalMillis: " + request.mParams.minIntervalMillis +
                ", precision: " + request.mParams.precision + ")");
         // Look for any existing request
        SessionRequest existingRequest = null;
        if (mSessionRequests.size() > 0) {
            for (SessionRequest r : mSessionRequests) {
                if (r.getUniqueId() == request.getUniqueId()) {
                    Log.d(TAG, "Found Existing Request matching pid/uid");
                    existingRequest = r;
                    break;
                }
            }
        }
        if (null != existingRequest) {
            if (request.mIdentity.sessionType == SessionType.SESSION_TYPE_PPE ||
                request.mIdentity.sessionType == SessionType.SESSION_TYPE_EDGNSS ||
                request.mIdentity.sessionType == SessionType.SESSION_TYPE_WOCS) {
                mSessionRequests.remove(existingRequest);
                mPrecisePositionIDLClient.stopSession(request);
                Log.d(TAG, "after remove: size of mSessionRequests = " + mSessionRequests.size());
                if (mSessionRequests.size() > 0) {
                    // start the first request in the queue
                    SessionRequest firstRequest = mSessionRequests.get(0);
                    mPrecisePositionIDLClient.startSession(firstRequest);
                }
            }
        }
    }

    private void onLocationAvaiable(Location location) {
        Log.v(TAG, "size of mSessionRequests = " + mSessionRequests.size());
        for (SessionRequest request : mSessionRequests) {
            if (null != request.mParams.callback) {
                request.mParams.callback.onLocationAvailable(location);
            }
        }
    }

    private void onResponseCallback(int response) {
        if (mSessionRequests.size() > 0) {
            SessionRequest request = mSessionRequests.get(0);
            if (null != request.mParams.callback) {
                PrecisePositionCallbackData callback =
                        (PrecisePositionCallbackData)request.mParams.callback;
                callback.onResponseCallback(response);
            }
        }
    }

    private class PrecisePositionCallbackData implements SessionRequest.SessionCallback {
        IPrecisePositionCallback mCallback;
        public PrecisePositionCallbackData(IPrecisePositionCallback callback) {
            mCallback = callback;
        }

        @Override
        public void onLocationAvailable(Location location) {
            try {
                Log.v(TAG, "PrecisePositionCallbackData onLocationAvailable");
                if (null != mCallback) {
                    mCallback.onLocationAvailable(location);
                }
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }
        public void onResponseCallback(int response) {
             try {
                Log.v(TAG, "PrecisePositionCallbackData onResponseCallback");
                if (null != mCallback) {
                    mCallback.onResponseCallback(response);
                }
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }
    }

    //=============================================================
    // ILocAidlPrecisePositioning AIDL client
    //=============================================================
    class PrecisePositionIDLClient extends LocIDLClientBase
            implements LocIDLClientBase.IServiceDeathCb {
        private final String TAG = "PrecisePositionIDLClient";
        private PrecisePositionIDLCallback mPrecisePositionCallback = null;
        public vendor.qti.gnss.ILocAidlPrecisePositionService mPrecisePositionIface = null;

        @Override
        public void onServiceDied() {
            Log.d(TAG, "onServiceDied");
            mPrecisePositionIface = null;
            getPrecisePositionIface();
            try {
                if (mPrecisePositionIface != null) {
                    mPrecisePositionIface.setCallback(mPrecisePositionCallback);
                    reportPrecisePositionServiceDied();
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Exception on service died.");
            }
        }

        private PrecisePositionIDLClient(PrecisePositionServiceProvider provider) {
            getPrecisePositionIface();
            try {
                if (mPrecisePositionIface != null) {
                    registerServiceDiedCb(this);
                    mPrecisePositionCallback = new PrecisePositionIDLCallback(provider);
                    mPrecisePositionIface.setCallback(mPrecisePositionCallback);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Exception pp aidl client cons");
            }
        }

        public void getPrecisePositionIface() {
            if (null == mPrecisePositionIface) {
                try {
                    mPrecisePositionIface =
                        getGnssAidlService().getExtensionLocAidlPrecisePositionService();
                } catch (RuntimeException e) {
                    Log.e(TAG, "Exception getting precise position Iface: " + e);
                    mPrecisePositionIface = null;
                } catch (RemoteException e) {
                }
            }
        }

        public void startSession(SessionRequest request) {
            if (null == mPrecisePositionIface) {
                Log.e(TAG, "precise position iface is null");
                return;
            }
            IDLClientUtils.toIDLService(TAG);
            try {
                mPrecisePositionIface.startSession((int)request.getUniqueId(),
                                                 request.mParams.minIntervalMillis,
                                                 sessionTypeToInt(request.mIdentity.sessionType),
                                                 request.mParams.correctionType.getValue());
            } catch (RemoteException e) {
            }
        }

        public void stopSession(SessionRequest request) {
            if (null == mPrecisePositionIface) {
                Log.e(TAG, "precise position iface is null");
                return;
            }
            IDLClientUtils.toIDLService(TAG);
            try {
                mPrecisePositionIface.stopSession((int)request.getUniqueId());
            } catch (RemoteException e) {
            }
        }

        private void reportPrecisePositionServiceDied() {
            Log.i(TAG, "ILocAidlPrecisePosition service died.");
            if (mSessionRequests.size() > 0) {
                SessionRequest currentRequest = mSessionRequests.get(0);
                startSession(currentRequest);
            } else {
                Log.i(TAG, "no precise session is running");
            }
        }
        //==============================================================
        // ILocAidlPrecisePositionCallback Callback Implementation
        //==============================================================
        class PrecisePositionIDLCallback extends ILocAidlPrecisePositionServiceCallback.Stub {
            private PrecisePositionServiceProvider mPrecisePositionServiceProvider;

            public PrecisePositionIDLCallback(PrecisePositionServiceProvider provider) {
                mPrecisePositionServiceProvider = provider;
            }

            public void trackingCb(LocAidlLocation aidlLocation) {
                IDLClientUtils.fromIDLService(TAG);
                Location location = IDLClientUtils.translateAidlLocation(aidlLocation);
                Bundle extras = new Bundle();
                if ((aidlLocation.locationFlagsMask &
                            LocAidlLocationFlagsBits.CONFORMITY_INDEX_BIT) != 0) {
                    extras.putFloat("Conformity_index", aidlLocation.conformityIndex);
                }
                if ((aidlLocation.locationFlagsMask &
                            LocAidlLocationFlagsBits.TECH_MASK_BIT) != 0) {
                    extras.putInt("Tech_mask", aidlLocation.locationTechnologyMask);
                }
                if ((aidlLocation.locationFlagsMask &
                            LocAidlLocationFlagsBits.QUALITY_TYPE_BIT) != 0) {
                    extras.putInt("Quality_type", aidlLocation.qualityType);
                }
                if (extras.size() > 0) {
                    location.setExtras(extras);
                }
                mPrecisePositionServiceProvider.onLocationAvaiable(location);
            }

            public void responseCb(int response) {
                IDLClientUtils.fromIDLService(TAG);
                mPrecisePositionServiceProvider.onResponseCallback(response);

            }

            @Override
            public final int getInterfaceVersion() {
                return ILocAidlPrecisePositionServiceCallback.VERSION;
            }

            @Override
            public final String getInterfaceHash() {
                return ILocAidlPrecisePositionServiceCallback.HASH;
            }
        }
    }
}
