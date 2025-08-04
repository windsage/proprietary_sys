/*====*====*====*====*====*====*====*====*====*====*====*====*====*====*====*
  Copyright (c) 2021 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
=============================================================================*/
package com.qualcomm.location.idlclient;

import android.util.Log;
import android.os.RemoteException;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.HashSet;
import android.os.IBinder;
import vendor.qti.gnss.ILocAidlGnss;
import android.os.ServiceManager;

abstract public class LocIDLClientBase {
    public static final String LOCAIDL_SERVICE_NAME = "vendor.qti.gnss.ILocAidlGnss/default";
    protected static vendor.qti.gnss.ILocAidlGnss mGnssAidlService = null;
    protected static IBinder mGnssAidlBinder = null;
    protected static IDLServiceVersion mIDLServiceVer = IDLServiceVersion.V0_0;
    protected static Runnable mGetGnssService = null;
    protected static CountDownLatch mCountDownLatch;
    protected static final String TAG = "LocIDLClientBase";

    //Izat modules wait for loc aidl service maximum 30s
    protected static final long WAIT_GETSERVICE_TIME_MS = 30000;
    protected static final long WAIT_GNSS_SERVICE_INIT_TIME_MS = 1000;
    protected static HashSet<IServiceDeathCb> mServiceDiedCbs = new HashSet<IServiceDeathCb>();
    private static LocAidlDeathRecipient mAidlDeathRecipient = null;

    public interface IServiceDeathCb {
        void onServiceDied();
    }

    public static enum IDLServiceVersion {
        V0_0, //AIDL not supported
        V_AIDL,
        V_AIDL_2_0
    }

    static {
        mCountDownLatch = new CountDownLatch(1);
        mGetGnssService = new Runnable() {
            @Override
            public void run() {
                do {
                    Log.d(TAG, "try to get LOCAIDL service");
                    try {
                        mGnssAidlBinder =
                            ServiceManager.waitForDeclaredService(LOCAIDL_SERVICE_NAME);
                    } catch (Exception e) {
                        Log.e(TAG, "failed to start LOCAIDL service via service manager");
                        mGnssAidlBinder = null;
                    }
                    if (null != mGnssAidlBinder) {
                        Log.d(TAG, "LOCAIDL service available");
                        mGnssAidlService = ILocAidlGnss.Stub.asInterface(mGnssAidlBinder);
                    } else {
                        Log.d(TAG, "LOCAIDL service not available");
                    }
                    if (mGnssAidlService != null) {
                        try {
                            mIDLServiceVer = IDLServiceVersion.V_AIDL;
                            if (mGnssAidlService.getInterfaceVersion() > 1) {
                                mIDLServiceVer = IDLServiceVersion.V_AIDL_2_0;
                            }
                        } catch(RemoteException e) {
                            Log.e(TAG, "RemoteException: " + e);
                        }
                        break;
                    }
                } while (false);

                try {
                    if (mGnssAidlBinder != null) {
                        mAidlDeathRecipient = new LocAidlDeathRecipient();
                        mGnssAidlBinder.linkToDeath(mAidlDeathRecipient, 0);
                    }

                } catch (RemoteException e) {
                    Log.d(TAG, "RemoteException: " + e);
                } catch (NoSuchElementException e) {
                    Log.d(TAG, "NoSuchElementException: " + e);
                }
                mCountDownLatch.countDown();
            }
        };
        new Thread(mGetGnssService).start();
    }

    public static IDLServiceVersion getIDLServiceVersion() {
        try {
            mCountDownLatch.await(WAIT_GETSERVICE_TIME_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
        }
        return mIDLServiceVer;
    }

    protected static void binderDiedReset() {
            mGnssAidlService = null;
            mIDLServiceVer = IDLServiceVersion.V0_0;
            mCountDownLatch = new CountDownLatch(1);

            new Thread(mGetGnssService).start();
            try {
                mCountDownLatch.await(WAIT_GNSS_SERVICE_INIT_TIME_MS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {}

            for (IServiceDeathCb item : mServiceDiedCbs) {
                item.onServiceDied();
            }
    }

    protected void registerServiceDiedCb(IServiceDeathCb cb) {
            mServiceDiedCbs.add(cb);
    }

    protected void unregisterServiceDiedCb(IServiceDeathCb cb) {
            mServiceDiedCbs.remove(cb);
    }

    public static vendor.qti.gnss.ILocAidlGnss getGnssAidlService() {
        try {
            mCountDownLatch.await(WAIT_GETSERVICE_TIME_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
        }
        return mGnssAidlService;
    }

    static final class LocAidlDeathRecipient implements IBinder.DeathRecipient {
        @Override
        public void binderDied() {
            Log.d(TAG, "LocAidlDeathRecipient.binderDied()");
            binderDiedReset();
        }
    }
}
