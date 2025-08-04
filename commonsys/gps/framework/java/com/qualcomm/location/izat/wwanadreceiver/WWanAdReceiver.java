/*====*====*====*====*====*====*====*====*====*====*====*====*====*====*====*
  Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
=============================================================================*/

package com.qualcomm.location.izat.wwanadreceiver;

import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.RemoteException;
import android.util.Log;
import com.qti.wwanadreceiver.*;
import java.util.Arrays;

import com.qualcomm.location.izat.IzatService;
import com.qualcomm.location.idlclient.LocIDLClientBase.*;
import com.qualcomm.location.idlclient.*;
import vendor.qti.gnss.ILocAidlGnss;
import vendor.qti.gnss.ILocAidlWWANAdRequestListener;
import vendor.qti.gnss.ILocAidlWWANAdReceiver;
import vendor.qti.gnss.ILocAidlWWANAdReceiver.LocAidlWWANADType;

public class WWanAdReceiver implements IzatService.ISystemEventListener {
    private static final String TAG = "WWanAdReceiver";
    private static final boolean VERBOSE_DBG = Log.isLoggable(TAG, Log.VERBOSE);

    private static final Object sCallBacksLock = new Object();
    private final Context mContext;
    private volatile IWWANAdRequestListener mReqListener = null;
    private WWanAdReceiverIdlClient mIdlClient = null;
    private String mPackageName;

    private static WWanAdReceiver sInstance = null;
    public static WWanAdReceiver getInstance(Context ctx) {
        if (sInstance == null) {
            sInstance = new WWanAdReceiver(ctx);
        }
        return sInstance;
    }
    private WWanAdReceiver(Context ctx) {
        if (VERBOSE_DBG) {
            Log.d(TAG, "WWanAdReceiver construction");
        }

        mContext = ctx;
        if (LocIDLClientBase.getIDLServiceVersion().compareTo(IDLServiceVersion.V_AIDL) >= 0) {
            mIdlClient = new WWanAdReceiverIdlClient(this);
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
                mReqListener = null;
            }
        }
    }

    /* Remote binder */
    private final IWWANAdReceiver.Stub mBinder = new IWWANAdReceiver.Stub() {
        public boolean registerRequestListener(IWWANAdRequestListener callback) {
            if (callback == null) {
                Log.e(TAG, "callback is null");
                return false;
            }
            synchronized (sCallBacksLock) {
                if (null != mReqListener) {
                    Log.e(TAG, "Request listener already provided.");
                    return false;
                }
                mReqListener = callback;
            }

            mPackageName = mContext.getPackageManager().getNameForUid(Binder.getCallingUid());
            return true;
        }

        public void pushWWANAssistanceData(int requestId, boolean status, int respType,
                byte[] respPayload) {
            Log.d(TAG, "in IWWANAdReceiver.Stub: pushWWANAssistanceData: requestId: " +
                    requestId + ", status: " + status + ", respType: " + respType +
                    ", payload length:" + respPayload.length);
            if (null != mIdlClient) {
                mIdlClient.pushWWANAssistanceData(requestId, status, respType, respPayload);
            }
        }

        public void removeResponseListener(IWWANAdRequestListener callback) {
            if (callback == null) {
                Log.e(TAG, "callback is null");
                return;
            }
            synchronized (sCallBacksLock) {
                mReqListener = null;
            }
            mPackageName = null;
        }

    };

    private void onWWANAdRequest(int requestId, byte[] reqPayload) {
        synchronized (sCallBacksLock) {
            try {
                if (null != mReqListener) {
                    mReqListener.onWWANAdRequest(requestId, reqPayload);
                }
            } catch (RemoteException e) {
                Log.w(TAG, "onWWANAdRequest remote exception");
            }
        }
    }

    public IWWANAdReceiver getWWanAdReceiverBinder() {
        return mBinder;
    }

    // ======================================================================
    // AIDL client
    // ======================================================================
    static class WWanAdReceiverIdlClient extends LocIDLClientBase
            implements LocIDLClientBase.IServiceDeathCb {
        private final String TAG = "WWanAdReceiverIdlClient";
        private final boolean VERBOSE = Log.isLoggable(TAG, Log.VERBOSE);

        private LocAidlWWanAdRequestListener mLocAidlReqListener;
        private ILocAidlWWANAdReceiver mLocAidlWWanAdReceiver;

        public WWanAdReceiverIdlClient(WWanAdReceiver receiver) {
            mLocAidlReqListener = new LocAidlWWanAdRequestListener(receiver);
            getWWanAdReceiverIface();
            if (null != mLocAidlWWanAdReceiver) {
                try {
                    mLocAidlWWanAdReceiver.init(mLocAidlReqListener);
                    registerServiceDiedCb(this);
                } catch (RemoteException e) {
                    Log.e(TAG, "Exception on provider init: " + e);
                }
            }
        }

        private void getWWanAdReceiverIface() {
            Log.i(TAG, "getWWanAdReceiverIface");
            ILocAidlGnss gnssService = (vendor.qti.gnss.ILocAidlGnss) getGnssAidlService();

            if (null != gnssService) {
                try {
                    mLocAidlWWanAdReceiver = gnssService.getExtensionLocAidlWWANAdReceiver();
                } catch (RemoteException e) {
                    throw new RuntimeException("Exception getting wwan ad receiver: " + e);
                }
            }
        }

        @Override
        public void onServiceDied() {
            mLocAidlWWanAdReceiver = null;
            getWWanAdReceiverIface();
            if (null != mLocAidlWWanAdReceiver) {
                try {
                    mLocAidlWWanAdReceiver.init(mLocAidlReqListener);
                } catch (RemoteException e) {
                    Log.e(TAG, "Exception on provider init: " + e);
                }
            }
        }

        public void pushWWANAssistanceData(int requestId, boolean status,
                int respType, byte[] respPayload) {
            if (null != mLocAidlWWanAdReceiver) {
                try {
                    mLocAidlWWanAdReceiver.pushWWANAssistanceData(requestId, status,
                            respType, respPayload);
                } catch (RemoteException e) {
                    Log.e(TAG, "Exception on provider init: " + e);
                }
            }
        }

        class LocAidlWWanAdRequestListener extends ILocAidlWWANAdRequestListener.Stub {
            private WWanAdReceiver mReceiver;

            public LocAidlWWanAdRequestListener(WWanAdReceiver receiver) {
                mReceiver = receiver;
            }

            public void onWWANAdRequest(int requestId, byte[] reqPayload) {
                Log.d(TAG, "onWWANAdRequest: request Id " + requestId +
                        ", reqPayload length " + reqPayload.length);
                mReceiver.onWWANAdRequest(requestId, reqPayload);
            }
            @Override
            public final int getInterfaceVersion() {
                return ILocAidlWWANAdRequestListener.VERSION;
            }
            @Override
            public final String getInterfaceHash() {
                return ILocAidlWWANAdRequestListener.HASH;
            }
        }
    }
}
