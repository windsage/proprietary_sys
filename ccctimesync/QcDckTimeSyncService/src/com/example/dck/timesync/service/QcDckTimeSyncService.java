
/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package vendor.qti.bluetooth.qcdcktimesync;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import java.io.InputStream;
import android.content.ComponentName;
import android.content.PermissionChecker;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.IBinder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.*;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;
import java.util.function.Supplier;


import org.carconnectivity.android.digitalkey.timesync.BleTimestamp;
import org.carconnectivity.android.digitalkey.timesync.CccDkTimeSync;
import org.carconnectivity.android.digitalkey.timesync.CccDkTimeSync.BleLmpEvent;
import org.carconnectivity.android.digitalkey.timesync.CccDkTimeSync.Direction;
import org.carconnectivity.android.digitalkey.timesync.IBleLmpEventListener;
import org.carconnectivity.android.digitalkey.timesync.ICccDkTimeSync;
import org.carconnectivity.android.digitalkey.timesync.IEventCallback;
import org.carconnectivity.android.digitalkey.timesync.IVersionListener;
import org.carconnectivity.android.digitalkey.timesync.Version;

import android.hardware.bluetooth.lmp_event.IBluetoothLmpEvent;
import android.hardware.bluetooth.lmp_event.IBluetoothLmpEventCallback;
import android.hardware.bluetooth.lmp_event.AddressType;
import android.hardware.bluetooth.lmp_event.LmpEventId;
//import android.hardware.bluetooth.lmp_event.Direction;
import android.hardware.bluetooth.lmp_event.Timestamp;

import java.io.*;
import java.security.Permission;
import java.util.*;
import java.util.logging.*;
import java.lang.Long;
import android.os.Handler;
import android.os.HandlerThread;


public class QcDckTimeSyncService extends Service {
    private static final String TAG = "QcDckTimeSyncService";
    public static boolean START_STATE = false;

    public static int LOG_LEVEL = 3;

    public boolean bServerStarted;
    private HashMap<byte[], IEventCallback> mAddressToCallbackMap;
    private HashMap<byte[], IBleLmpEventListener> mAddressToBleEventListenerMap;

    private Context mContext;
    private NotificationManager mNotificationManager;
    private byte[] mPendingAddress;
    private IBleLmpEventListener mPendingEventListener;

    private Supplier<IBluetoothLmpEvent> mLmpEventServiceSupplier;
    private IBluetoothLmpEvent mLmpEventService;
    private IBluetoothLmpEventCallback mBluetoothLmpEventCallbackImpl;

    private class BluetoothLmpEventCallbackImpl extends IBluetoothLmpEventCallback.Stub {
           @Override
          public void onEventGenerated(
              android.hardware.bluetooth.lmp_event.Timestamp timestamp,
              byte addressType, byte[] address, byte direction, byte lmpEventId,
              char connEventCounter) throws RemoteException {
              Log.i(TAG, "onEventGenerated: timestamp: " + timestamp +
                          "address: <" + address +
                          "> addressType: " + addressType +
                          "diretion: " + direction +
                          "lmpEventId: " + lmpEventId +
                          "ConnectionEventCounter: " + connEventCounter);
              IBleLmpEventListener cb = mAddressToBleEventListenerMap.get(address);
              int dir =  (direction == android.hardware.bluetooth.lmp_event.Direction.TX) ? 1 : 0;
              BleTimestamp ts = new BleTimestamp(timestamp.systemTimeUs, timestamp.bluetoothTimeUs, 0, 0, false);
              int event = (lmpEventId == LmpEventId.CONNECT_IND) ? 0 : 1;
              int eventCounter = (int)connEventCounter;
              if (cb != null) {
                  cb.onTimestamp(address, ts, dir, event, eventCounter);
              }
          }
          @Override
          public void onRegistered(boolean status) throws RemoteException  {
                Log.i(TAG, "onRegistered: " + status);
                IEventCallback cb = mAddressToCallbackMap.get(mPendingAddress);
                if (cb != null) {
                    if (status == true) {
                        cb.onRegisterSuccess();
                        mAddressToBleEventListenerMap.put(mPendingAddress, mPendingEventListener);
                    } else {
                        cb.onRegisterFailure();
                    }
                    mPendingAddress = null;
                    mPendingEventListener = null;
                }
          }

          @Override
          public int getInterfaceVersion() {
            Log.i(TAG, "getInterfaceVersion");
            return super.VERSION;
          }

          @Override
          public String getInterfaceHash() {
             Log.i(TAG, "getInterfaceHash");
             return super.HASH;
          }
    };

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind");
        return new CccDkTimeSyncBinder(this);
    }

    void initBleLMPServiceConnection() {
         Log.i(TAG, "initBleLMPServiceConnection");
         mLmpEventServiceSupplier =  new VintfHalCache();
         if (mLmpEventServiceSupplier == null) {
             Log.i(TAG, "No LMP Events AIDL Service here");
         } else {
             mLmpEventService = mLmpEventServiceSupplier.get();
         }
    }

    //Service lifecycle methods
    @Override
    public void onCreate() {
        if(QcDckTimeSyncService.LOG_LEVEL >= 2)
            Log.d(TAG, "Dck time Sync service was created.");

         mContext = this;
         mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        mAddressToCallbackMap = new HashMap<byte[], IEventCallback> ();
        mAddressToBleEventListenerMap = new HashMap<byte[], IBleLmpEventListener> ();
        mBluetoothLmpEventCallbackImpl = new BluetoothLmpEventCallbackImpl();
        Log.i(TAG, "initBleLMPServiceConnection: ");
        initBleLMPServiceConnection();
        mPendingAddress = null;
        mPendingEventListener = null;
    }

    public void registerBleLmpEventListener(byte[] address, IBleLmpEventListener callback) throws RemoteException {
        Log.i(TAG, "registerBleLmpEventListener: " + address + "callback: " + callback);
        byte[] lmpEventIds = {LmpEventId.CONNECT_IND, LmpEventId.LL_PHY_UPDATE_IND};
        IEventCallback cb = mAddressToCallbackMap.get(address);
        if (mLmpEventService != null) {
            try {
                Log.i(TAG, "registerBleLmpEventListener: " + address + " mBluetoothLmpEventCallbackImpl: " + mBluetoothLmpEventCallbackImpl);
                mLmpEventService.registerForLmpEvents(mBluetoothLmpEventCallbackImpl, AddressType.RANDOM, address, lmpEventIds);
                mPendingAddress = address;
                mPendingEventListener = callback;
            } catch (RemoteException e) {
                if (cb != null) {
                    cb.onRegisterFailure();
                }
            }
        } else {
            Log.e(TAG, "LmpEventService is not available");
            cb.onRegisterFailure();
        }
    }

    public void registerEventCallback(byte[] address, IEventCallback callback) {
        Log.i(TAG, "registerEventCallback: " + address + "callback: " + callback);
        mAddressToCallbackMap.put(address, callback);
    }
    public void unregisterEventCallback(byte[] address) {
        Log.i(TAG, "unregisterEventCallback: " + address);
        if (mLmpEventService != null) {
            try {
                mLmpEventService.unregisterLmpEvents(AddressType.RANDOM, address);
                mAddressToBleEventListenerMap.remove(address);
                mAddressToCallbackMap.remove(address);
            } catch (RemoteException e) {
                Log.e(TAG, "unregisterBleLmpEventListener: RemoteException");
            }
        } else {
            Log.e(TAG, "LmpEventService is not available");
        }
    }

    private void reset(){
        mContext = this;
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
     }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        START_STATE = true;
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        if(QcDckTimeSyncService.LOG_LEVEL >= 1)
            Log.d(TAG, "User swiped to remove process. Service storing connected devices will be restarted and lose state information.");
        stopSelf();
    }

    public QcDckTimeSyncService() {
        Log.d(TAG, "default constructor");
        mBluetoothLmpEventCallbackImpl = new BluetoothLmpEventCallbackImpl();
    }

     public boolean startServer(){
        if(QcDckTimeSyncService.LOG_LEVEL >= 2)
        Log.d(TAG, "StartServer");
        return true;
    }

    public boolean stopServer(){
        if(QcDckTimeSyncService.LOG_LEVEL >= 2)
            Log.d(TAG, "StopServer");
        return true;
    }

    private static class VintfHalCache implements Supplier<IBluetoothLmpEvent>, IBinder.DeathRecipient {
        private IBluetoothLmpEvent mInstance = null;
        @Override
        public synchronized IBluetoothLmpEvent get() {
            if (mInstance == null) {
                IBinder binder = Binder.allowBlocking(ServiceManager.waitForDeclaredService(
                        "android.hardware.bluetooth.lmp_event.IBluetoothLmpEvent/default"));
                if (binder != null) {
                    mInstance = IBluetoothLmpEvent.Stub.asInterface(binder);
                    try {
                        binder.linkToDeath(this, 0);
                    } catch (RemoteException e) {
                        Log.i(TAG, "Unable to register DeathRecipient for " + mInstance);
                    }
                }
            }
            return mInstance;
        }
        @Override
        public synchronized void binderDied() {
            mInstance = null;
        }
    }

    static class CccDkTimeSyncBinder extends ICccDkTimeSync.Stub implements IBinder {
        private QcDckTimeSyncService mDckService;
        private List<Version> SUPPORTED_VERSIONS;

        CccDkTimeSyncBinder (QcDckTimeSyncService svc) {
            SUPPORTED_VERSIONS = new ArrayList<Version> ();
            mDckService = svc;
            Version v11 = new Version((byte)1, (byte)1);
            Version v10 = new Version((byte)1, (byte)0);
            //SUPPORTED_VERSIONS.add(v11);
            SUPPORTED_VERSIONS.add(v10);
            Log.i(TAG, "CccDkTimeSyncBinder instantiated");
        }
        @Override
        public void getApiVersion(Version versionMin, Version versionMax, IVersionListener callback) throws RemoteException {
            Log.d(TAG, "getApiversion");
            for (Version version : SUPPORTED_VERSIONS) {
                if (versionMin.isGreaterThan(version) || versionMax.isLessThan(version)) {
                    continue;
                }
                Log.i(TAG, "API Version" + version.getMajor() + "." + version.getMinor());
                callback.onVersion(version);
                return;
            }
            Log.e(TAG, "API Version Unsupported");
            callback.onVersion(CccDkTimeSync.VERSION_UNSUPPORTED);
        }

        @Override
        public void registerBleLmpEventListener(byte[] address, IBleLmpEventListener callback) throws RemoteException {
            if (mDckService != null) {
                mDckService.registerBleLmpEventListener(address, callback);
            }
        }

        @Override
        public void registerEventCallback(byte[] address, IEventCallback callback) {
            if (mDckService != null) {
                mDckService.registerEventCallback(address, callback);
            }
        }

        @Override
        public void unregisterEventCallback(byte[] address) {
            if (mDckService != null) {
                mDckService.unregisterEventCallback(address);
            }
        }
    }
}

