/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
*/
/*
 * Copyright (c) 2020 The Linux Foundation. All rights reserved.
 * Not a contribution.
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.bluetooth.bc;

import static android.Manifest.permission.BLUETOOTH_CONNECT;

import android.content.AttributionSource;
import android.os.UserHandle;
import android.app.ActivityManager;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeAudioContentMetadata;
import android.bluetooth.BluetoothLeBroadcastChannel;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.bluetooth.BluetoothLeBroadcastSubgroup;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSyncHelper;
import android.bluetooth.BluetoothBroadcast;
import android.bluetooth.BluetoothStatusCodes;
import android.bluetooth.BleBroadcastSourceInfo;
import android.bluetooth.BleBroadcastSourceChannel;
import android.bluetooth.BleBroadcastAudioScanAssistManager;
import android.bluetooth.BleBroadcastAudioScanAssistCallback;
import android.bluetooth.IBluetoothSyncHelper;
import android.bluetooth.IBluetoothManager;
import android.bluetooth.IBleBroadcastAudioScanAssistCallback;
import android.bluetooth.le.ScanFilter;

import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.HandlerThread;
import android.util.Log;
import android.os.ParcelUuid;
import android.bluetooth.BluetoothUuid;
import java.util.ArrayList;
import android.os.ServiceManager;
import java.nio.charset.StandardCharsets;

import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.PeriodicAdvertisingCallback;
import android.bluetooth.le.PeriodicAdvertisingManager;
import android.bluetooth.le.PeriodicAdvertisingReport;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

import com.android.bluetooth.BluetoothMetricsProto;
import com.android.bluetooth.CsipWrapper;
import com.android.bluetooth.Utils;
import com.android.bluetooth.broadcast.BroadcastService;
import com.android.bluetooth.btservice.MetricsLogger;
import com.android.bluetooth.btservice.ProfileService;
import com.android.bluetooth.btservice.ServiceFactory;
import com.android.bluetooth.a2dp.A2dpService;
import com.android.internal.annotations.VisibleForTesting;
import com.android.bluetooth.btservice.AdapterService;
import com.android.bluetooth.apm.ApmConst;
import com.android.bluetooth.apm.ActiveDeviceManagerService;
import com.android.bluetooth.le_audio.LeAudioService;
import com.android.bluetooth.lebroadcast.BassClientService;
import com.android.bluetooth.lebroadcast.LeBroadcastAssistantServIntf;
//*_CSIP
//CSIP related imports
import com.android.bluetooth.groupclient.GroupService;
import android.bluetooth.BluetoothGroupCallback;
import android.bluetooth.DeviceGroup;
//_CSIP*/

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.UUID;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.NoSuchElementException;
import android.os.SystemProperties;


import com.android.internal.util.ArrayUtils;
/** @hide */
public class BCService extends ProfileService {
    private static final boolean DBG = true;
    private static final String TAG = BCService.class.getSimpleName();

    public static final ParcelUuid CAP_UUID =
                ParcelUuid.fromString("00001853-0000-1000-8000-00805F9B34FB");

    public static final String BC_ID = "0000184F-0000-1000-8000-00805F9B34FB";
    public static final String BS_ID = "00001852-0000-1000-8000-00805F9B34FB";
    private static BCService sBCService;
    private static final int MAX_BASS_CLIENT_STATE_MACHINES = 10;
    private static final int MAX_BASS_CLIENT_CSET_MEMBERS = 10;
    private static final int MAX_BROADCAST_CODE_LENGTH = 16;
    private final Map<BluetoothDevice, BassClientStateMachine> mStateMachines =
             new HashMap<>();
    private HandlerThread mStateMachinesThread;
    private final Map<Integer, BassCsetManager> mSetManagers =
             new HashMap<>();
    private HandlerThread mSetManagerThread;

    private AdapterService mAdapterService;
    private BassClientService mBassClientService;
    private BluetoothLeBroadcastMetadata currentMetadata;

    private Map<BluetoothDevice, ArrayList<IBleBroadcastAudioScanAssistCallback>> mAppCallbackMap =
             new HashMap<BluetoothDevice, ArrayList<IBleBroadcastAudioScanAssistCallback>>();

    private BassUtils bassUtils = null;
    public static final int INVALID_SYNC_HANDLE = -1;
    public static final int INVALID_ADV_SID = -1;
    public static final int INVALID_ADV_ADDRESS_TYPE = -1;
    public static final int INVALID_ADV_INTERVAL = -1;
    public static final int INVALID_BROADCAST_ID = -1;

    private boolean mAutoLocalSourceAddForActiveDeviceEnabled = false;
    private Map<BluetoothDevice, ArrayList<BluetoothDevice>> mActiveSourceMap;
    private ArrayList<BluetoothDevice> mStartScanOffloadDevices;
    private boolean mPendingPANotificationToBCast = false;

    //*_CSIP
    //CSET interfaces
    private CsipWrapper mCsipWrapper = CsipWrapper.getInstance();
    public int mCsipAppId = -1;
    private int mQueuedOps = 0;
    //_CSIP*/

    /*Caching the PAresults from Broadcast source*/
    /*This is stored at service so that each device state machine can access
    and use it as needed. Once the periodic sync in cancelled, this data will bre
    removed to ensure stable data won't used*/
    /*broadcastSrcDevice, syncHandle*/
    private Map<BluetoothDevice, Integer> mSyncHandleMap;
    /*syncHandle, parsed BaseData data*/
    private Map<Integer, BaseData> mSyncHandleVsBaseInfo;
    /*bcastSrcDevice, corresponding PAResultsMap*/
    private Map<BluetoothDevice, PAResults> mPAResultsMap;
    // The CSIP group which requires group operation or single operation for addSource, modifySource, removeSource
    private Map<Integer, Boolean> mGroupOpMap;
    public class PAResults {
        public BluetoothDevice mDevice;
        public int mAddressType;
        public int mAdvSid;
        public int mSyncHandle;
        public int metaDataLength;
        public byte[] metaData;
        public byte mFeatures;
        public int mPAInterval;
        public int mBroadcastId;
        public boolean mIsNotified;
        public String mPublicBroadcastName;
        public boolean mIsPublicBroadcast;

        private static final int PBP_SERVICE_DATA_MIN_LEN = 2;
        private static final int PBP_FEATURES_ENCRYPTION_BIT = 0x1;
        private static final int PBP_FEATURES_STANDARD_QUALITY_BIT = 0x2;
        private static final int PBP_FEATURES_HIGH_QUALITY_BIT = 0x04;

        PAResults(BluetoothDevice device, int addressType,
                         int syncHandle, int advSid, int paInterval, int broadcastId,
                         String broadcastName) {
            mDevice = device;
            mAddressType = addressType;
            mAdvSid = advSid;
            mSyncHandle = syncHandle;
            mPAInterval = paInterval;
            mBroadcastId = broadcastId;
            mIsNotified = false;
            mPublicBroadcastName = broadcastName;
        }

        public void updateSyncHandle(int syncHandle) {
            mSyncHandle = syncHandle;
        }

        public void updateAdvSid(int advSid) {
            mAdvSid = advSid;
        }

        public void updateAddressType(int addressType) {
            mAddressType = addressType;
        }

        public void updateAdvInterval(int advInterval) {
            mPAInterval = advInterval;
        }

        public void updateBroadcastId(int broadcastId) {
            mBroadcastId = broadcastId;
        }

        public boolean getIsNotified() {
            synchronized (this) {
                return mIsNotified;
            }
        }

        public void updateIsNotified(boolean notified) {
            synchronized (this) {
                mIsNotified = notified;
            }
        }

        public void updatePublicBroadcastData(byte[] serviceData) {
            mFeatures = 0;
            metaDataLength = 0;
            metaData = null;
            mIsPublicBroadcast = false;
            if (serviceData != null) {
                log("Public broadcast service data: " +
                        Arrays.toString(serviceData));
            }
            if (serviceData == null ||
                    serviceData.length < PBP_SERVICE_DATA_MIN_LEN) {
                return;
            }
            int offset = 0;
            byte features = serviceData[offset++];
            int length = serviceData[offset++] & 0xff;
            if (serviceData.length != (length + PBP_SERVICE_DATA_MIN_LEN)) {
                log("Invalid public broadcast service data length");
                return;
            }
            mIsPublicBroadcast = true;
            mFeatures = features;
            metaDataLength = length;
            if (metaDataLength > 0) {
                metaData = new byte[metaDataLength];
                System.arraycopy(serviceData, offset,
                        metaData, 0, metaDataLength);
                offset += metaDataLength;
            }
        }

        public void updateBroadcastName(String broadcastName) {
            mPublicBroadcastName = broadcastName;
        }

        public String getBroadcastName() {
            return mPublicBroadcastName;
        }

        public boolean isPublicBroadcast() {
            return mIsPublicBroadcast;
        }

        public boolean isEncrypted() {
            return (mFeatures & PBP_FEATURES_ENCRYPTION_BIT) != 0;
        }

        public boolean isStandardQuality() {
            return (mFeatures & PBP_FEATURES_STANDARD_QUALITY_BIT) != 0;
        }

        public boolean isHighQuality() {
            return (mFeatures & PBP_FEATURES_HIGH_QUALITY_BIT) != 0;
        }

        public int getAudioQuality() {
            int audioQuality = BluetoothLeBroadcastMetadata.AUDIO_CONFIG_QUALITY_NONE;
            if (isStandardQuality()) {
                audioQuality |= BluetoothLeBroadcastMetadata.AUDIO_CONFIG_QUALITY_STANDARD;
            }
            if (isHighQuality()) {
                audioQuality |= BluetoothLeBroadcastMetadata.AUDIO_CONFIG_QUALITY_HIGH;
            }
            return audioQuality;
        }

        public byte[] getMetaData() {
            return metaData;
        }

        public void print() {
            log("-- PAResults --");
            log("mDevice:" + mDevice);
            log("mAddressType:" + mAddressType);
            log("mAdvSid:" + mAdvSid);
            log("mSyncHandle:" + mSyncHandle);
            log("mPAInterval:" + mPAInterval);
            log("mBroadcastId:" + mBroadcastId);
            log("mIsNotified: " + mIsNotified);
            log("mIsPublicBroadcast: " + mIsPublicBroadcast);
            if (mIsPublicBroadcast) {
                log("mPublicBroadcastName: " + mPublicBroadcastName);
                log("mFeatures: " + mFeatures);
                if (metaData != null) {
                    log("metaData: " + Arrays.toString(metaData));
                }
            }
            log("-- END: PAResults --");
        }
    };

    public synchronized void updatePAResultsMap(BluetoothDevice device, int addressType, int syncHandle, int advSid,
            int advInterval, int bId, String broadcastName, byte[] serviceData) {
          log("updatePAResultsMap: device: " + device);
          log("updatePAResultsMap: syncHandle: " + syncHandle);
          log("updatePAResultsMap: advSid: " + advSid);
          log("updatePAResultsMap: addressType: " + addressType);
          log("updatePAResultsMap: advInterval: " + advInterval);
          log("updatePAResultsMap: broadcastId: " + bId);
          log("mSyncHandleMap" + mSyncHandleMap);
          log("mPAResultsMap" + mPAResultsMap);
          //Cache the SyncHandle
          if (mSyncHandleMap != null) {
              Integer i = new Integer(syncHandle);
              mSyncHandleMap.put(device, i);
          }
          if (mPAResultsMap != null) {
              PAResults paRes = mPAResultsMap.get(device);
              if (paRes == null) {
                  log("PAResmap: add >>>");
                  paRes = new PAResults (device, addressType,
                              syncHandle, advSid, advInterval, bId, broadcastName);
                  if (paRes != null) {
                      paRes.updatePublicBroadcastData(serviceData);
                      paRes.print();
                      mPAResultsMap.put(device, paRes);
                  }
              } else {
                  if (advSid != INVALID_ADV_SID) {
                      paRes.updateAdvSid(advSid);
                  }
                  if (syncHandle != INVALID_SYNC_HANDLE) {
                      paRes.updateSyncHandle(syncHandle);
                  }
                  if (addressType != INVALID_ADV_ADDRESS_TYPE) {
                      paRes.updateAddressType(addressType);
                  }
                  if (advInterval != INVALID_ADV_INTERVAL) {
                      paRes.updateAdvInterval(advInterval);
                  }
                  if (bId != INVALID_BROADCAST_ID) {
                      paRes.updateBroadcastId(bId);
                  }
                  if (broadcastName != null) {
                      paRes.updateBroadcastName(broadcastName);
                  }
                  if (serviceData != null) {
                      paRes.updatePublicBroadcastData(serviceData);
                  }
                  log("PAResmap: update >>>");
                  paRes.print();
                  mPAResultsMap.replace(device, paRes);
              }
          }
          log(">>mPAResultsMap" + mPAResultsMap);
      }

      public PAResults getPAResults(BluetoothDevice device) {
          PAResults res = null;
          if (mPAResultsMap != null) {
            res = mPAResultsMap.get(device);
          } else {
            Log.e(TAG, "getPAResults: mPAResultsMap is null");
        }
        return res;
      }
      public PAResults clearPAResults(BluetoothDevice device) {
          PAResults res = null;
          Log.d(TAG, "clearPAResults for srcDevice: " + device);
          if (mSyncHandleMap != null) {
              mSyncHandleMap.remove(device);
          }
          if (mPAResultsMap != null) {
            res = mPAResultsMap.remove(device);
          } else {
            Log.e(TAG, "getPAResults: mPAResultsMap is null");
        }
        return res;
      }

      public void clearAllNotifiedFlags() {
          synchronized(this) {
              if (mPAResultsMap != null) {
                  for (PAResults res: mPAResultsMap.values()) {
                      res.mIsNotified = false;
                  }
              }
          }
      }

      public void updateBASE(int syncHandlemap, BaseData base) {
         if (mSyncHandleVsBaseInfo != null) {
             log("updateBASE : mSyncHandleVsBaseInfo>>");
             mSyncHandleVsBaseInfo.put(syncHandlemap, base);
         } else {
             Log.e(TAG, "updateBASE: mSyncHandleVsBaseInfo is null");
         }
      }

    public BaseData getBASE(int syncHandlemap) {
        BaseData base = null;
        if (mSyncHandleVsBaseInfo != null) {
            log("getBASE : syncHandlemap::" + syncHandlemap);
            base = mSyncHandleVsBaseInfo.get(syncHandlemap);
        } else {
            Log.e(TAG, "getBASE: mSyncHandleVsBaseInfo is null");
        }
        log("getBASE returns" + base);
        return base;
    }

    public void clearBASE(int syncHandlemap) {
        if (mSyncHandleVsBaseInfo != null) {
            log("clearBASE : mSyncHandleVsBaseInfo>>");
            mSyncHandleVsBaseInfo.remove(syncHandlemap);
        } else {
            Log.e(TAG, "updateBASE: mSyncHandleVsBaseInfo is null");
        }
    }

    public String getProgramInfoFromBASE(int syncHandle) {
        String program = null;
        BaseData base = getBASE(syncHandle);
        if(base == null) {
            log("getProgramInfoFromBASE : BASE is null");
            return null;
        }

        int numSubGroups = base.getNumberOfSubgroupsofBIG();
        for (int i=0; i<numSubGroups; i++) {
            if (base.getMetadata(i) != null) {
                BluetoothLeAudioContentMetadata data =
                        BluetoothLeAudioContentMetadata.fromRawBytes(base.getMetadata(i));
                program = data.getProgramInfo();
                if (program != null) {
                    break;
                }
            }
        }
        log("getProgramInfoFromBASE : program " + program);
        return program;
    }

    public void setActiveSyncedSource(BluetoothDevice scanDelegator, BluetoothDevice sourceDevice) {
        if (mActiveSourceMap == null) {
            Log.w(TAG, "setActiveSyncedSource:  mActiveSourceMap is null");
            return;
        }
        log("setActiveSyncedSource: scanDelegator" + scanDelegator + ":: sourceDevice:" + sourceDevice);
        ArrayList<BluetoothDevice> activeSyncSources = mActiveSourceMap.get(scanDelegator);
        if (activeSyncSources == null) {
            Log.i(TAG, "setActiveSyncedSource: entry not exists");
            activeSyncSources = new ArrayList<BluetoothDevice>();
        }
        activeSyncSources.add(sourceDevice);
        mActiveSourceMap.put(scanDelegator, activeSyncSources);
    }

    public ArrayList<BluetoothDevice> getActiveSyncedSource(BluetoothDevice scanDelegator) {
        if (mActiveSourceMap == null) {
            Log.w(TAG, "getActiveSyncedSource:  mActiveSourceMap is null");
            return null;
        }
        ArrayList<BluetoothDevice> currentSource =  mActiveSourceMap.get(scanDelegator);
        log("getActiveSyncedSource: scanDelegator" + scanDelegator + "returning " + currentSource);
        return currentSource;
    }

    public void removeActiveSyncedSource(BluetoothDevice scanDelegator, BluetoothDevice sourceDevice) {
        if (mActiveSourceMap == null) {
            Log.w(TAG, "removeActiveSyncedSource:  mActiveSourceMap is null");
            return;
        }
        log("removeActiveSyncedSource: scanDelegator" + scanDelegator + ":: sourceDevice:" + sourceDevice);
        ArrayList<BluetoothDevice> activeSyncSources = mActiveSourceMap.get(scanDelegator);
        if (activeSyncSources == null) {
            Log.i(TAG, "removeActiveSyncedSource: activeSyncSources list is null");
            return;
        } else {
           boolean ret = activeSyncSources.remove(sourceDevice);
           Log.i(TAG, "removeActiveSyncedSource: ret value of removal from list:" + ret);
        }
        if (activeSyncSources.size() != 0) {
            mActiveSourceMap.replace(scanDelegator, activeSyncSources);
        } else {
            Log.i(TAG, "removeActiveSyncedSource: Remove the complete entry");
            mActiveSourceMap.remove(scanDelegator);
        }
        return;
    }

    public boolean isRegisteredSyncSource(BluetoothDevice sourceDevice) {
        boolean isRegistered = false;
        synchronized (mStateMachines) {
            for (BassClientStateMachine sm : mStateMachines.values()) {
                if (sm.getConnectionState() == BluetoothProfile.STATE_CONNECTED &&
                        sm.isRegisteredSyncSource(sourceDevice)) {
                    isRegistered = true;
                    break;
                }
            }
        }
        log("isRegisteredSyncSource return " + isRegistered);
        return isRegistered;
    }

    public boolean isPublicAddrForSrc() {
        boolean defaultValue = !ApmConst.getAospLeaEnabled();
        return SystemProperties.getBoolean(
                "persist.vendor.service.bt.pAddrForcSource", defaultValue);
    }

    public BluetoothDevice getLocalBroadcastSource() {
        BluetoothDevice address = BluetoothAdapter.getDefaultAdapter().
                getRemoteDevice(BluetoothAdapter.getDefaultAdapter().getAddress());
        BroadcastService baService = BroadcastService.getBroadcastService();
        if (baService != null && !isPublicAddrForSrc() && baService.BroadcastGetAdvAddress() != null) {
            address = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(
                    baService.BroadcastGetAdvAddress());
        }
        return address;
    }

    public int getLocalBroadcastSourceType() {
        BroadcastService baService = BroadcastService.getBroadcastService();
        if (baService == null || isPublicAddrForSrc()) {
            return BluetoothDevice.ADDRESS_TYPE_PUBLIC;
        }
        return baService.BroadcastGetAdvAddrType();
    }

    public boolean isAddSrcForConnectedPeerOnly() {
        return SystemProperties.getBoolean(
                "persist.vendor.service.bt.addSrcForConnectedPeer", false);
    }

     @Override
     protected IProfileServiceBinder initBinder() {
         return new BluetoothSyncHelperBinder(this);
     }

     //*_CSIP
     private BluetoothGroupCallback mBluetoothGroupCallback = new BluetoothGroupCallback() {
          public void onGroupClientAppRegistered(int status, int appId) {
              log("onCsipAppRegistered:" + status + "appId: " + appId);
              if (status == 0) {
                  mCsipAppId = appId;
              } else {
                  Log.e(TAG, "Csip registeration failed, status:" + status);
              }
          }

          public void onConnectionStateChanged (int state, BluetoothDevice device) {
              log("onConnectionStateChanged: Device: " + device + "state: " + state);
                  //notify the statemachine about CSIP connection
                  synchronized (mStateMachines) {
                      BassClientStateMachine stateMachine = getOrCreateStateMachine(device);
                      Message m = stateMachine.obtainMessage(BassClientStateMachine.CSIP_CONNECTION_STATE_CHANGED);
                      m.obj = state;
                      stateMachine.sendMessage(m);
                  }
          }

          public void onNewGroupFound (int setId,  BluetoothDevice device, UUID uuid) {     }
          public void onGroupDiscoveryStatusChanged (int setId, int status, int reason) {    }
          public void onGroupDeviceFound (int setId, BluetoothDevice device) {    }
          public void onExclusiveAccessChanged (int setId, int value, int status, List<BluetoothDevice> devices) {
              log("onLockStatusChanged: setId" + setId + devices + "status:" + status);
              BassCsetManager setMgr = null;
              setMgr = getOrCreateCSetManager(setId, null);
              if (setMgr == null) {
                      return;
              }
              log ("sending Lock status to setId:" + setId);
              Message m = setMgr.obtainMessage(BassCsetManager.LOCK_STATE_CHANGED);
              m.obj = devices;
              m.arg1 = value;
              setMgr.sendMessage(m);
          }
          public void onExclusiveAccessStatusFetched (int setId, int lockStatus) {    }
          public void onExclusiveAccessAvailable (int setId, BluetoothDevice device) {    }
     };
     //_CSIP*/

     @Override
     protected boolean start() {
        if (DBG) {
            Log.d(TAG, "start()");
        }
        mAdapterService = Objects.requireNonNull(AdapterService.getAdapterService(),
                 "AdapterService cannot be null when BCService starts");
         mStateMachines.clear();
         mStateMachinesThread = new HandlerThread("BCService.StateMachines");
         mStateMachinesThread.start();

         mSetManagers.clear();
         mSetManagerThread = new HandlerThread("BCService.SetManagers");
         mSetManagerThread.start();

         setBCService(this);
         LeBroadcastAssistantServIntf.init(sBCService);

         bassUtils = new BassUtils(this);
         mAppCallbackMap = new HashMap<BluetoothDevice,
                 ArrayList<IBleBroadcastAudioScanAssistCallback>>();
         //Saving PSync stuff for future addition
         mSyncHandleMap = new HashMap<BluetoothDevice, Integer>();
         mPAResultsMap = new HashMap<BluetoothDevice, PAResults>();
         mSyncHandleVsBaseInfo = new HashMap<Integer, BaseData>();
         mActiveSourceMap = new HashMap<BluetoothDevice, ArrayList<BluetoothDevice>>();
         mGroupOpMap = new HashMap<Integer, Boolean>();
         mStartScanOffloadDevices = new ArrayList<>();

         //*_CSIP
         mCsipWrapper.registerGroupClientModule(mBluetoothGroupCallback);
         //_CSIP*/
         /*_PACS
         mPacsClientService = PacsClientService.getPacsClientService();
         _PACS*/

         ///*_GAP
         //GAP registeration for Bass UUID notification
         if (mAdapterService != null) {
             log("register for BASS UUID notif");
            ParcelUuid bassUuid = new ParcelUuid(BassClientStateMachine.BASS_UUID);
            mAdapterService.registerUuidSrvcDisc(bassUuid);
         }
         //_GAP*/
         mAutoLocalSourceAddForActiveDeviceEnabled = SystemProperties.getBoolean("persist.vendor.service.bt.mAutoLocalAdd", false);
         return true;
     }

    @Override
    protected boolean stop() {
        if (DBG) {
            Log.d(TAG, "stop()");
        }

        synchronized (mStateMachines) {
             for (BassClientStateMachine sm : mStateMachines.values()) {
                 sm.doQuit();
                 sm.cleanup();
             }
             mStateMachines.clear();
        }

        if (mStateMachinesThread != null) {
             mStateMachinesThread.quitSafely();
             mStateMachinesThread = null;
        }

        if (mSetManagerThread != null) {
             mSetManagerThread.quitSafely();
             mSetManagerThread = null;
        }

        setBCService(null);

        if (mAppCallbackMap != null) {
            mAppCallbackMap.clear();
            mAppCallbackMap = null;
        }

        if (mSyncHandleMap != null) {
            mSyncHandleMap.clear();
            mSyncHandleMap = null;
        }

        if (mPAResultsMap != null) {
            mPAResultsMap.clear();
            mPAResultsMap = null;
        }

        if (mActiveSourceMap != null) {
            mActiveSourceMap.clear();
            mActiveSourceMap = null;
        }

        if (mGroupOpMap != null) {
            mGroupOpMap.clear();
            mGroupOpMap = null;
        }

        if (mStartScanOffloadDevices != null) {
            mStartScanOffloadDevices.clear();
            mStartScanOffloadDevices = null;
        }
        //*_CSIP
        if (mCsipAppId != -1) {
           //mSetCoordinator.unregisterGroupClientModule(mCsipAppId);
        }
        //_CSIP*/
        return true;
     }

     @Override
     public boolean onUnbind(Intent intent) {
        Log.d(TAG, "Need to unregister app");
        //unregisterApp();
        return super.onUnbind(intent);
    }

    /**
     * Get the BCService instance
     * @return BCService instance
     */
    public static synchronized BCService getBCService() {
        if (sBCService == null) {
            Log.w(TAG, "getBCService(): service is NULL");
            return null;
        }

        if (!sBCService.isAvailable()) {
            Log.w(TAG, "getBCService(): service is not available");
            return null;
        }
        return sBCService;
    }

    public BassUtils getBassUtils() {
        return bassUtils;
    }

    public boolean isAutoLocalSourceAddForActiveDeviceEnabled(BluetoothDevice dev) {
        boolean ret = false;
        ServiceFactory sFactory = new ServiceFactory();
        ActiveDeviceManagerService activeDevMgrService = ActiveDeviceManagerService.get();
        BluetoothDevice a2dpActiveDevice = null;
        if (activeDevMgrService != null) {
            a2dpActiveDevice = activeDevMgrService.getActiveDevice(ApmConst.AudioFeatures.MEDIA_AUDIO);
        }
        if (Objects.equals(dev,a2dpActiveDevice) && mAutoLocalSourceAddForActiveDeviceEnabled) {
            ret = true;
        }
        return ret;
    }

    private byte isLocalSourceAvailable(BluetoothDevice dev) {
            List<BleBroadcastSourceInfo> currentSourceInfos =
                getAllBroadcastSourceInformation(dev);
            String localBroadcasterAddr = getLocalBroadcastSource().getAddress();
            boolean isLocalSrcAvailable = false;
            byte srcId = BassClientStateMachine.INVALID_SRC_ID;
            if (currentSourceInfos == null) {
                log("currentSourceInfos is null for " + dev);
                return srcId;
            }
            for (int i=0; i<currentSourceInfos.size(); i++) {
                BleBroadcastSourceInfo sI = currentSourceInfos.get(i);
                if (sI.isEmptyEntry()) {
                    continue;
                }
                BluetoothDevice sIDevice = sI.getSourceDevice();
                if (localBroadcasterAddr.equals(sIDevice.getAddress())) {
                    srcId = sI.getSourceId();
                    Log.e(TAG, "isLocalSourceAvailable: Local src present");
                    isLocalSrcAvailable = true;
                    break;
                }
           }
           return srcId;
    }

    private void selectLocalSource(BluetoothDevice device) {
        BluetoothDevice localDev = getLocalBroadcastSource();
        String localName = BluetoothAdapter.getDefaultAdapter().getName();
        ScanRecord record = null;
        if (localName != null) {
            byte name_len = (byte)localName.length();
            byte[] bd_name = localName.getBytes(StandardCharsets.US_ASCII);
            byte[] name_key = new byte[] {++name_len, 0x09 }; //0x09 TYPE:Name
            byte[] scan_r = new byte[name_key.length + bd_name.length];
            System.arraycopy(name_key, 0, scan_r, 0, name_key.length);
            System.arraycopy(bd_name, 0, scan_r, name_key.length, bd_name.length);
            record = ScanRecord.parseFromBytes(scan_r);
            log ("Local name populated in fake Scan res:" + record.getDeviceName());
        }
        ScanResult scanRes = new ScanResult(localDev,
                1, 1, 1,2, 0, 0, 0, record, 0);
        selectBroadcastSource(device, scanRes, false, false);
    }

    public void forceUpdateLocalBroadcastSourceToA2DPActiveDevice() {
        ServiceFactory sFactory = new ServiceFactory();
        BluetoothDevice dummyDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice("FA:CE:FA:CE:FA:CE");
        ActiveDeviceManagerService activeDevMgrService = ActiveDeviceManagerService.get();
        BluetoothDevice activeStreamingDevice = null;
        if (activeDevMgrService != null) {
            activeStreamingDevice = activeDevMgrService.getActiveDevice(ApmConst.AudioFeatures.MEDIA_AUDIO);
        }
        if (activeStreamingDevice == null || Objects.equals(activeStreamingDevice, dummyDevice)) {
            log("Either no Active A2DP device Or Broadcast is active, ignore");
            return;
        }
        BluetoothDevice localDev = getLocalBroadcastSource();
        BleBroadcastSourceInfo srcInfo = new BleBroadcastSourceInfo(
                                localDev,
                                BassClientStateMachine.INVALID_SRC_ID,
                                (byte)INVALID_ADV_SID,
                                BleBroadcastSourceInfo.BROADCASTER_ID_INVALID,
                                BleBroadcastSourceInfo.BROADCAST_ASSIST_ADDRESS_TYPE_PUBLIC,
                                BleBroadcastSourceInfo.BROADCAST_ASSIST_PA_SYNC_STATE_IN_SYNC,
                                BleBroadcastSourceInfo.BROADCAST_ASSIST_ENC_STATE_UNENCRYPTED,
                                null,
                                (byte)0,
                                BleBroadcastSourceInfo.BROADCAST_ASSIST_AUDIO_SYNC_STATE_NOT_SYNCHRONIZED,
                                null,
                                null
                                );
        byte srcId = isLocalSourceAvailable(activeStreamingDevice);
        if (srcId != BassClientStateMachine.INVALID_SRC_ID) {
            log("local src is already present, remove");
            removeBroadcastSourceInternal(activeStreamingDevice, srcId, false);
        }
        selectLocalSource(activeStreamingDevice);
        if (addBroadcastSourceInternal(activeStreamingDevice, srcInfo, true) == false) {
            log("auto source addition failure");
        } else {
            mPendingPANotificationToBCast = true;
        }
    }

    public boolean isPendingPASyncNotificationtoBA() {
        return mPendingPANotificationToBCast;
    }

    public void setPASyncNotified() {
       //clearoff pending flag
        mPendingPANotificationToBCast = false;
    }

    public boolean isAutoLocalSourceAddEnabled() {
        Log.d(TAG, "isAutoLocalSourceAddEnabled(): returns: " + mAutoLocalSourceAddForActiveDeviceEnabled);
        return mAutoLocalSourceAddForActiveDeviceEnabled;
    }

    public void onBroadcastStateChanged(int state) {
        if (mAutoLocalSourceAddForActiveDeviceEnabled == true) {
            if (state == BluetoothBroadcast.STATE_ENABLED) {
                forceUpdateLocalBroadcastSourceToA2DPActiveDevice();
            }
        }
    }

    public BluetoothDevice getDeviceForSyncHandle(int syncHandle) {
        BluetoothDevice dev = null;
        if (mSyncHandleMap != null) {
            for (Map.Entry<BluetoothDevice, Integer> entry : mSyncHandleMap.entrySet()) {
                Integer value = entry.getValue();
                if (value == syncHandle) {
                    dev = entry.getKey();
                }
            }
        }
        return dev;
    }

    private static synchronized void setBCService(BCService instance) {
        if (DBG) {
            Log.d(TAG, "setBCService(): set to: " + instance);
        }
        sBCService = instance;
    }

    /**
     * Connects the bass profile to the passed in device
     *
     * @param device is the device with which we will connect the Bass profile
     * @return true if BAss profile successfully connected, false otherwise
     */
    public boolean connect(BluetoothDevice device) {
        if (DBG) {
            Log.d(TAG, "connect(): " + device);
        }
        if (device == null) {
            return false;
        }

        if (getConnectionPolicy(device) == BluetoothProfile.CONNECTION_POLICY_UNKNOWN) {
            return false;
        }

        LeAudioService leAudioService = LeAudioService.getLeAudioService();
        if (leAudioService != null &&
            leAudioService.getConnectionPolicy(device) ==
                        BluetoothProfile.CONNECTION_POLICY_FORBIDDEN) {
            Log.d(TAG,"unicast connection forbidden, ignore BC connect");
            return false;
        }
        synchronized (mStateMachines) {
            BassClientStateMachine stateMachine = getOrCreateStateMachine(device);

            stateMachine.sendMessage(BassClientStateMachine.CONNECT);
        }
        return true;
    }

  /**
     * Disconnects Bassclient profile for the passed in device
     *
     * @param device is the device with which we want to disconnected the BAss client profile
     * @return true if Bass client profile successfully disconnected, false otherwise
     */
    public boolean disconnect(BluetoothDevice device) {

        if (DBG) {
            Log.d(TAG, "disconnect(): " + device);
        }
        if (device == null) {
            return false;
        }

      synchronized (mStateMachines) {
          BassClientStateMachine stateMachine = getOrCreateStateMachine(device);

          stateMachine.sendMessage(BassClientStateMachine.DISCONNECT);
        }
        return true;
    }

    public List<BluetoothDevice> getConnectedDevices() {

        synchronized (mStateMachines) {
            List<BluetoothDevice> devices = new ArrayList<>();
            for (BassClientStateMachine sm : mStateMachines.values()) {
                if (sm.isConnected()) {
                    devices.add(sm.getDevice());
                }
            }
            log("getConnectedDevices: " + devices);
            return devices;
        }
    }

    /**
     * Check whether can connect to a peer device.
     * The check considers a number of factors during the evaluation.
     *
     * @param device the peer device to connect to
     * @return true if connection is allowed, otherwise false
     */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public boolean okToConnect(BluetoothDevice device) {
        // Check if this is an incoming connection in Quiet mode.
        if (mAdapterService.isQuietModeEnabled()) {
            Log.e(TAG, "okToConnect: cannot connect to " + device + " : quiet mode enabled");
            return false;
        }
        // Check connection policy and accept or reject the connection.
        int connectionPolicy = getConnectionPolicy(device);
        int bondState = mAdapterService.getBondState(device);
        // Allow this connection only if the device is bonded. Any attempt to connect while
        // bonding would potentially lead to an unauthorized connection.
        if (bondState != BluetoothDevice.BOND_BONDED) {
            Log.w(TAG, "okToConnect: return false, bondState=" + bondState);
            return false;
        } else if (connectionPolicy != BluetoothProfile.CONNECTION_POLICY_UNKNOWN
                && connectionPolicy != BluetoothProfile.CONNECTION_POLICY_ALLOWED) {
            // Otherwise, reject the connection if connectionPolicy is not valid.
            Log.w(TAG, "okToConnect: return false, connectionPolicy=" + connectionPolicy);
            return false;
        }
        return true;
    }

    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {

        ArrayList<BluetoothDevice> devices = new ArrayList<>();
        if (states == null) {
            return devices;
        }
        final BluetoothDevice[] bondedDevices = mAdapterService.getBondedDevices();
        if (bondedDevices == null) {
            return devices;
        }
        synchronized (mStateMachines) {
            for (BluetoothDevice device : bondedDevices) {
                final ParcelUuid[] featureUuids = device.getUuids();
                if (!ArrayUtils.contains(featureUuids, new ParcelUuid(BassClientStateMachine.BASS_UUID))) {
                    continue;
                }
                int connectionState = BluetoothProfile.STATE_DISCONNECTED;
                BassClientStateMachine sm = getOrCreateStateMachine(device);
                if (sm != null) {
                    connectionState = sm.getConnectionState();
                }
                for (int state : states) {
                    if (connectionState == state) {
                        devices.add(device);
                        break;
                    }
                }
            }
            return devices;
        }
    }

    public int getConnectionState(BluetoothDevice device) {
        synchronized (mStateMachines) {
            BassClientStateMachine sm = getOrCreateStateMachine(device);
            if (sm == null) {
                log("getConnectionState returns STATE_DISC");
                return BluetoothProfile.STATE_DISCONNECTED;
            }
            return sm.getConnectionState();
        }
    }

    /**
     * Set the connectionPolicy of the Hearing Aid profile.
     *
     * @param device the remote device
     * @param connectionPolicy the connection policy of the profile
     * @return true on success, otherwise false
     */
    public boolean setConnectionPolicy(BluetoothDevice device, int connectionPolicy) {

        if (DBG) {
            Log.d(TAG, "Saved connectionPolicy " + device + " = " + connectionPolicy);
        }
        boolean setSuccessfully;
        setSuccessfully = mAdapterService.getDatabase()
                .setProfileConnectionPolicy(device, BluetoothProfile.BC_PROFILE, connectionPolicy);
        if (setSuccessfully && connectionPolicy == BluetoothProfile.CONNECTION_POLICY_ALLOWED) {
            connect(device);
        } else if (setSuccessfully
                && connectionPolicy == BluetoothProfile.CONNECTION_POLICY_FORBIDDEN) {
            disconnect(device);
        }
        return setSuccessfully;
    }

    /**
     * Get the connection policy of the profile.
     *
     * <p> The connection policy can be any of:
     * {@link BluetoothProfile#CONNECTION_POLICY_ALLOWED},
     * {@link BluetoothProfile#CONNECTION_POLICY_FORBIDDEN},
     * {@link BluetoothProfile#CONNECTION_POLICY_UNKNOWN}
     *
     * @param device Bluetooth device
     * @return connection policy of the device
     * @hide
     */
    public int getConnectionPolicy(BluetoothDevice device) {

        return mAdapterService.getDatabase()
                .getProfileConnectionPolicy(device, BluetoothProfile.BC_PROFILE);
    }

    public void sendBroadcastSourceSelectedCallback(BluetoothDevice device, List<BleBroadcastSourceChannel> bChannels, int status){
        if (mAppCallbackMap == null) {
            Log.w(TAG, "sendBroadcastSourceSelectedCallback: mAppCallbackMap is null");
            return;
        }
        ArrayList<IBleBroadcastAudioScanAssistCallback> cbs = mAppCallbackMap.get(device);
        if (cbs == null) {
            Log.e(TAG, "no App callback for this device" + device);
            return;
        }
        for (IBleBroadcastAudioScanAssistCallback cb : cbs) {
            try {
               cb.onBleBroadcastAudioSourceSelected(device, status, bChannels);
            } catch (RemoteException e)  {
               Log.e(TAG, "Exception while calling sendBroadcastSourceSelectedCallback");
            }
        }
    }

    public void sendAddBroadcastSourceCallback(BluetoothDevice device, byte srcId, int status){
        Log.d(TAG, "sendAddBroadcastSourceCallback");
        if (mBassClientService != null) {
            if (status == BleBroadcastAudioScanAssistCallback.BASS_STATUS_SUCCESS
                    || status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "notifySourceAdded: srcId = " + srcId);
                mBassClientService.getCallbacks().notifySourceAdded(device,
                        srcId, BluetoothStatusCodes.REASON_LOCAL_APP_REQUEST);
            } else {
                Log.d(TAG, "notifySourceAddedFailed");
                mBassClientService.getCallbacks().notifySourceAddFailed(
                        device, currentMetadata,
                        BluetoothStatusCodes.ERROR_UNKNOWN);
            }
        }
        if (mAppCallbackMap == null) {
            Log.w(TAG, "sendAddBroadcastSourceCallback: mAppCallbackMap is null");
            return;
        }
        ArrayList<IBleBroadcastAudioScanAssistCallback> cbs = mAppCallbackMap.get(device);
        if (cbs == null) {
            Log.e(TAG, "no App callback for this device" + device);
            return;
        }

        for (IBleBroadcastAudioScanAssistCallback cb : cbs) {
            try {
               cb.onBleBroadcastAudioSourceAdded(device, srcId, status);
            } catch (RemoteException e)  {
               Log.e(TAG, "Exception while calling onBleBroadcastAudioSourceAdded");
            }
        }
    }

    public void sendUpdateBroadcastSourceCallback(BluetoothDevice device, byte sourceId, int status){
        Log.d(TAG, "sendUpdateBroadcastSourceCallback");
        if (mBassClientService != null) {
            if (status == BleBroadcastAudioScanAssistCallback.BASS_STATUS_SUCCESS
                    || status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "notifySourceModified: sourceId = " + sourceId);
                mBassClientService.getCallbacks().notifySourceModified(device,
                        sourceId, BluetoothStatusCodes.REASON_LOCAL_APP_REQUEST);
            } else {
                Log.d(TAG, "notifySourceModifyFailed");
                mBassClientService.getCallbacks().notifySourceModifyFailed(device,
                        sourceId, BluetoothStatusCodes.ERROR_UNKNOWN);
            }
        }
        if (mAppCallbackMap == null) {
            Log.w(TAG, "sendUpdateBroadcastSourceCallback: mAppCallbackMap is null");
            return;
        }
        ArrayList<IBleBroadcastAudioScanAssistCallback> cbs = mAppCallbackMap.get(device);
        if (cbs == null) {
            Log.e(TAG, "no App callback for this device" + device);
            return;
        }

        for (IBleBroadcastAudioScanAssistCallback cb : cbs) {
            try {
                cb.onBleBroadcastAudioSourceUpdated(device, sourceId, status);
            } catch (RemoteException e)  {
                Log.e(TAG, "Exception while calling onBleBroadcastAudioSourceUpdated");
            }
        }
    }
    public void sendRemoveBroadcastSourceCallback(BluetoothDevice device, byte sourceId, int status){
        Log.d(TAG, "sendRemoveBroadcastSourceCallback");
        if (mBassClientService != null) {
            if (status == BleBroadcastAudioScanAssistCallback.BASS_STATUS_SUCCESS
                    || status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "notifySourceRemoved: sourceId = " + sourceId);
                int setId = getCsetId(device);
                if (mGroupOpMap != null && mGroupOpMap.containsKey(setId)) {
                    mGroupOpMap.remove(setId);
                }
                mBassClientService.getCallbacks().notifySourceRemoved(device,
                        sourceId, BluetoothStatusCodes.REASON_LOCAL_APP_REQUEST);
            } else {
                Log.d(TAG, "notifySourceRemoveFailed");
                mBassClientService.getCallbacks().notifySourceRemoveFailed(device,
                        sourceId, BluetoothStatusCodes.ERROR_UNKNOWN);
            }
        }
        if (mAppCallbackMap == null) {
            Log.w(TAG, "sendRemoveBroadcastSourceCallback: mAppCallbackMap is null");
            return;
        }
        ArrayList<IBleBroadcastAudioScanAssistCallback> cbs = mAppCallbackMap.get(device);
        if (cbs == null) {
            Log.e(TAG, "no App callback for this device" + device);
            return;
        }

        for (IBleBroadcastAudioScanAssistCallback cb : cbs) {
            try {
                cb.onBleBroadcastAudioSourceRemoved(device, sourceId, status);
            } catch (RemoteException e)  {
                Log.e(TAG, "Exception while calling onBleBroadcastAudioSourceRemoved");
            }
        }
    }
    public void sendSetBroadcastPINupdatedCallback(BluetoothDevice device, byte sourceId, int status){
        if (mAppCallbackMap == null) {
            Log.w(TAG, "sendSetBroadcastPINupdatedCallback: mAppCallbackMap is null");
            return;
        }
        ArrayList<IBleBroadcastAudioScanAssistCallback> cbs = mAppCallbackMap.get(device);
        if (cbs == null) {
            Log.e(TAG, "no App callback for this device" + device);
            return;
        }

        for (IBleBroadcastAudioScanAssistCallback cb : cbs) {
            try {
                cb.onBleBroadcastPinUpdated(device, sourceId, status);
            } catch (RemoteException e)  {
                Log.e(TAG, "Exception while calling onBleBroadcastPinUpdated");
            }
        }
    }

    public void registerAppCallback (BluetoothDevice device, IBleBroadcastAudioScanAssistCallback cb) {
        Log.i(TAG, "registerAppCallback" + device);
        if (mAppCallbackMap == null) {
            Log.w(TAG, "registerAppCallback: mAppCallbackMap is null");
            return;
        }
        ArrayList<IBleBroadcastAudioScanAssistCallback> cbs = mAppCallbackMap.get(device);
        if (cbs == null) {
            Log.i(TAG, "registerAppCallback: entry exists");
            cbs = new ArrayList<IBleBroadcastAudioScanAssistCallback>();
        }
        cbs.add(cb);
        mAppCallbackMap.put(device, cbs);

        return;
    }

    public void unregisterAppCallback (BluetoothDevice device, IBleBroadcastAudioScanAssistCallback cb) {
        Log.i(TAG, "unregisterAppCallback" + device);
        if (mAppCallbackMap == null) {
            Log.w(TAG, "unregisterAppCallback: mAppCallbackMap is null");
            return;
        }
        ArrayList<IBleBroadcastAudioScanAssistCallback> cbs = mAppCallbackMap.get(device);
        if (cbs == null) {
            Log.i(TAG, "unregisterAppCallback: cb list is null");
            return;
        } else {
           boolean ret = cbs.remove(cb);
           Log.i(TAG, "unregisterAppCallback: ret value of removal from list:" + ret);
        }
        if (cbs.size() != 0) {
            mAppCallbackMap.replace(device, cbs);
        } else {
            Log.i(TAG, "unregisterAppCallback: Remove the complete entry");
            mAppCallbackMap.remove(device);
        }
        return;
    }

    public boolean searchforLeAudioBroadcasters (BluetoothDevice device) {
        Log.i(TAG, "searchforLeAudioBroadcasters on behalf of" + device);
        if (mAppCallbackMap == null) {
            Log.w(TAG, "searchforLeAudioBroadcasters: mAppCallbackMap is null");
            return false;
        }
        ArrayList<IBleBroadcastAudioScanAssistCallback> cbs = mAppCallbackMap.get(device);
        if (cbs == null) {
            Log.e(TAG, "no App callback for this device" + device);
            return false;
        }
        boolean ret = false;
        if (bassUtils != null) {
            ret = bassUtils.searchforLeAudioBroadcasters(device, cbs, null);
        } else {
            Log.e(TAG, "searchforLeAudioBroadcasters :Null Bass Util Handle" + device);
            ret = false;
        }
        return ret;
    }

    public boolean stopSearchforLeAudioBroadcasters (BluetoothDevice device) {
        Log.i(TAG, "stopsearchforLeAudioBroadcasters on behalf of" + device);
        if (mAppCallbackMap == null) {
            Log.w(TAG, "stopSearchforLeAudioBroadcasters: mAppCallbackMap is null");
            return false;
        }
        ArrayList<IBleBroadcastAudioScanAssistCallback> cbs = mAppCallbackMap.get(device);
        if (cbs == null) {
            Log.e(TAG, "no App callback for this device" + device);
        }
        boolean ret = false;
        if (bassUtils != null) {
            ret = bassUtils.stopSearchforLeAudioBroadcasters(device, cbs);
        } else {
            Log.e(TAG, "stopsearchforLeAudioBroadcasters :Null Bass Util Handle" + device);
            ret = false;
        }
        return ret;
    }

    BluetoothDevice getActiveLeAudioDevice() {
        LeAudioService leAudioService = LeAudioService.getLeAudioService();
        if (leAudioService == null) {
            log("leAudioService is null");
            return null;
        }
        List<BluetoothDevice> activeDevices = leAudioService.getActiveDevices();
        BluetoothDevice activeDevice = activeDevices.get(0);
        if (getConnectionState(activeDevice) != BluetoothProfile.STATE_CONNECTED) {
            log("First active member is not connected, fetch 2nd member");
            activeDevice = activeDevices.get(1);
        }
        log("Current active media device is: " + activeDevice);
        return activeDevice;
    }

    void selectBroadcastSources(ScanResult result) {
        log("selectBroadcastSources");
        BluetoothDevice activeDevice = getActiveLeAudioDevice();
        if (activeDevice != null) {
            boolean isGroupOpDevice = false;
            int setId = getCsetId(activeDevice);
            DeviceGroup devGrp = mCsipWrapper.getCoordinatedSet(setId);
            if (devGrp != null) {
                if (mGroupOpMap != null && mGroupOpMap.containsKey(setId)) {
                    isGroupOpDevice = mGroupOpMap.get(setId);
                } else {
                    log("treat it as group device operation for CSIP device: " + activeDevice);
                    isGroupOpDevice = true;
                }
            }
            startScanOffloadInternal(activeDevice, isGroupOpDevice);
            synchronized (mStateMachines) {
                BassClientStateMachine sm = getOrCreateStateMachine(activeDevice);
                if (sm == null || sm.getConnectionState() != BluetoothProfile.STATE_CONNECTED) {
                    log("BassStateMachine not in connected state");
                    return;
                }
                Message m = sm.obtainMessage(BassClientStateMachine.SELECT_BCAST_SOURCE);
                m.obj = result;
                m.arg1 = sm.USER;
                m.arg2 = isGroupOpDevice ? sm.GROUP_OP : sm.NON_GROUP_OP;
                sm.sendMessage(m);
            }
        } else {
            log("Current leaudio active device is null");
            //Select broadcast sources for all connected sinks if current active device is null
            synchronized (mStateMachines) {
                for (BassClientStateMachine sm : mStateMachines.values()) {
                    if (sm.getConnectionState() == BluetoothProfile.STATE_CONNECTED) {
                        boolean isGroupOpDevice = false;
                        int setId = getCsetId(sm.getDevice());
                        DeviceGroup devGrp = mCsipWrapper.getCoordinatedSet(setId);
                        if (devGrp != null) {
                            if (mGroupOpMap != null && mGroupOpMap.containsKey(setId)) {
                                isGroupOpDevice = mGroupOpMap.get(setId);
                            } else {
                                log("treat it as group device operation for CSIP device: " + sm.getDevice());
                                isGroupOpDevice = true;
                            }
                        }
                        startScanOffloadInternal(activeDevice, isGroupOpDevice);
                        Message m = sm.obtainMessage(BassClientStateMachine.SELECT_BCAST_SOURCE);
                        m.obj = result;
                        m.arg1 = sm.USER;
                        m.arg2 = isGroupOpDevice ? sm.GROUP_OP : sm.NON_GROUP_OP;
                        sm.sendMessage(m);
                    }
                }
            }
        }
    }

    void selectLocalSources() {
        synchronized (mStateMachines) {
            for (BassClientStateMachine sm : mStateMachines.values()) {
                if (sm.isConnected()) {
                    selectLocalSource(sm.getDevice());
                }
            }
        }
    }

    public boolean selectBroadcastSource (BluetoothDevice device, ScanResult scanRes, boolean isGroupOp, boolean auto) {

        Log.i(TAG, "selectBroadcastSource for " + device + "isGroupOp:" + isGroupOp);
        Log.i(TAG, "ScanResult " + scanRes);

        if (scanRes == null) {
            Log.e(TAG, "selectBroadcastSource: null Scan results");
            return false;
        }
        List<BluetoothDevice> listOfDevices = new ArrayList<BluetoothDevice>();
        if (isGroupOp) {
            int setId = getCsetId(device);
            DeviceGroup devGrp = mCsipWrapper.getCoordinatedSet(setId);
            if (devGrp != null) {
                List<BluetoothDevice> grpMembers = devGrp.getDeviceGroupMembers();
                if (isAddSrcForConnectedPeerOnly()) {
                    for (BluetoothDevice dev : grpMembers) {
                        if (getConnectionState(dev) == BluetoothProfile.STATE_CONNECTED) {
                            listOfDevices.add(dev);
                        }
                    }
                } else {
                    listOfDevices = grpMembers;
                }
            } else {
                Log.d(TAG, "devGrp is NULL");
                listOfDevices.add(device);
            }
        } else {
            listOfDevices.add(device);
        }
        if (isRoomForBroadcastSourceAddition(listOfDevices) == false) {
            sendBroadcastSourceSelectedCallback(device, null,
                BleBroadcastAudioScanAssistCallback.BASS_STATUS_NO_EMPTY_SLOT);
            return false;
        }
        //dummy BleSourceInfo from scanRes
        BleBroadcastSourceInfo scanResSI = new BleBroadcastSourceInfo (scanRes.getDevice(),
                                                            BassClientStateMachine.INVALID_SRC_ID,
                                                            (byte)scanRes.getAdvertisingSid(),
                                                            BleBroadcastSourceInfo.BROADCASTER_ID_INVALID,
                                                            scanRes.getAddressType(),
                                                            BleBroadcastSourceInfo.BROADCAST_ASSIST_PA_SYNC_STATE_INVALID,
                                                            BleBroadcastSourceInfo.BROADCAST_ASSIST_ENC_STATE_INVALID,
                                                            null,
                                                            (byte)0,
                                                            BleBroadcastSourceInfo.BROADCAST_ASSIST_AUDIO_SYNC_STATE_NOT_SYNCHRONIZED,
                                                            null,
                                                            null);
        if (isValidBroadcastSourceAddition(listOfDevices, scanResSI) == false) {
            sendBroadcastSourceSelectedCallback(device,
                null, BleBroadcastAudioScanAssistCallback.BASS_STATUS_DUPLICATE_ADDITION);
            return false;
        }
        startScanOffloadInternal(device, isGroupOp);
        synchronized (mStateMachines) {
            BassClientStateMachine sm = getOrCreateStateMachine(device);
            if (sm == null) {
                return false;
            }
            Message m = sm.obtainMessage(BassClientStateMachine.SELECT_BCAST_SOURCE);
            m.obj = scanRes;
            if (auto) {
                m.arg1 = sm.AUTO;
            } else {
                m.arg1 = sm.USER;
            }
            if (isGroupOp) {
                m.arg2 = sm.GROUP_OP;
            } else {
                m.arg2 = sm.NON_GROUP_OP;
            }
            sm.sendMessage(m);
        }
        return true;
    }

    public synchronized void notifyOperationCompletion(BluetoothDevice device, int pendingOperation) {
        log("notifyOperationCompletion: " + device + "pendingOperation: " +
            BassClientStateMachine.messageWhatToString(pendingOperation));
        //synchronized (mStateMachines) {
            switch (pendingOperation) {
                case BassClientStateMachine.START_SCAN_OFFLOAD:
                case BassClientStateMachine.STOP_SCAN_OFFLOAD:
                case BassClientStateMachine.ADD_BCAST_SOURCE:
                case BassClientStateMachine.UPDATE_BCAST_SOURCE:
                case BassClientStateMachine.REMOVE_BCAST_SOURCE:
                case BassClientStateMachine.SET_BCAST_CODE:
                    if (mQueuedOps > 0) {
                        mQueuedOps = mQueuedOps - 1;
                    } else {
                        log("not a queued op, Internal op");
                        return;
                    }
                break;
                default:
                     {
                         log("notifyOperationCompletion: unhandled case");
                         return;
                     }
              }
        //}
          if (mQueuedOps == 0) {
              log("notifyOperationCompletion: all ops are done!");
          }

    }

    public synchronized boolean startScanOffload (BluetoothDevice masterDevice, List<BluetoothDevice> devices) {

        Log.i(TAG, "startScanOffload for " + devices);
        for (BluetoothDevice dev : devices) {
            BassClientStateMachine stateMachine = getOrCreateStateMachine(dev);
            if (stateMachine == null || mStartScanOffloadDevices == null) {
                continue;
            }
            if (mStartScanOffloadDevices.contains(dev)) {
                Log.i(TAG, "startScanOffload has informed device " + dev);
                continue;
            }
            mStartScanOffloadDevices.add(dev);
            stateMachine.sendMessage(BassClientStateMachine.START_SCAN_OFFLOAD);
            mQueuedOps = mQueuedOps + 1;
        }
        return true;
    }

    public synchronized boolean stopScanOffload (BluetoothDevice masterDevice, List<BluetoothDevice> devices) {

        Log.i(TAG, "stopScanOffload for " + devices);
        for (BluetoothDevice dev : devices) {
            BassClientStateMachine stateMachine = getOrCreateStateMachine(dev);
            if (stateMachine == null || mStartScanOffloadDevices == null) {
                continue;
            }
            if (!mStartScanOffloadDevices.contains(dev)) {
                Log.w(TAG, "mStartScanOffloadDevices does not contain dev " + dev);
                continue;
            }
            Log.i(TAG, "remove " + dev + " from startScanOffloadDevices List");
            mStartScanOffloadDevices.remove(dev);
            stateMachine.sendMessage(BassClientStateMachine.STOP_SCAN_OFFLOAD);
            mQueuedOps = mQueuedOps + 1;
        }

        return true;
    }

    public boolean isLocalBroadcasting() {
        return bassUtils.isLocalLEAudioBroadcasting();
    }

    private boolean isValidBroadcastSourceAddition(List<BluetoothDevice> devices,
                                                BleBroadcastSourceInfo srcInfo) {
        boolean ret = true;

        //run through all the device, if it is not valid
        //to even one device to add this source, return failure
        for (BluetoothDevice dev : devices) {
            List<BleBroadcastSourceInfo> currentSourceInfos =
                getAllBroadcastSourceInformation(dev);
            if (currentSourceInfos == null) {
                log("currentSourceInfos is null for " + dev);
                continue;
            }
            for (int i=0; i<currentSourceInfos.size(); i++) {
                if (srcInfo.matches(currentSourceInfos.get(i))) {
                   ret = false;
                   Log.e(TAG, "isValidBroadcastSourceAddition: fails for: " + dev + "&srcInfo" + srcInfo);
                   break;
                }
            }
        }

        log("isValidBroadcastSourceInfo returns: " + ret);
        return ret;
    }

    private boolean isRoomForBroadcastSourceAddition(List<BluetoothDevice> devices) {
        boolean isRoomAvail = false;

        //run through all the device, if it is not valid
        //to even one device to add this source, return failure
        for (BluetoothDevice dev : devices) {
            isRoomAvail = false;
            List<BleBroadcastSourceInfo> currentSourceInfos =
                getAllBroadcastSourceInformation(dev);
            for (int i=0; i<currentSourceInfos.size(); i++) {
                BleBroadcastSourceInfo srcInfo = currentSourceInfos.get(i);
                if (srcInfo.isEmptyEntry()) {
                   isRoomAvail = true;
                   continue;
                }
            }
            if (isRoomAvail == false) {
                Log.e(TAG, "isRoomForBroadcastSourceAddition: fails for: " + dev);
                break;
            }
        }

        log("isRoomForBroadcastSourceAddition returns: " + isRoomAvail);
        return isRoomAvail;
    }

    public synchronized boolean addBroadcastSource (BluetoothDevice masterDevice,
            List<BluetoothDevice> devices, BleBroadcastSourceInfo srcInfo) {

        Log.i(TAG, "addBroadcastSource for " + devices +
                   "SourceInfo " + srcInfo);
        if (srcInfo == null) {
            Log.e(TAG, "addBroadcastSource: null SrcInfo");
            return false;
        }
        if (getConnectionState(masterDevice) != BluetoothProfile.STATE_CONNECTED &&
                !isAddSrcForConnectedPeerOnly()) {
            Log.i(TAG, "Device is not connected");
            sendAddBroadcastSourceCallback(masterDevice, srcInfo.getSourceId(),
                    BleBroadcastAudioScanAssistCallback.BASS_STATUS_FATAL);
            if (mBassClientService != null) {
                mBassClientService.getCallbacks().notifySourceAddFailed(masterDevice, currentMetadata,
                        BluetoothStatusCodes.ERROR_REMOTE_LINK_ERROR);
            }
            return false;
        }
        List<BluetoothDevice> listOfValidDevices = new ArrayList<BluetoothDevice>();
        if (isAddSrcForConnectedPeerOnly()) {
            for (BluetoothDevice dev : devices) {
                if (getConnectionState(dev) != BluetoothProfile.STATE_CONNECTED) {
                    Log.i(TAG, "memberDevice: " + dev + " is invalid for adding source ");
                } else {
                    Log.i(TAG, "memberDevice: " + dev + " is valid for adding source ");
                    listOfValidDevices.add(dev);
                }
            }
        } else {
            listOfValidDevices = devices;
        }

        if (isRoomForBroadcastSourceAddition(listOfValidDevices) == false) {
            sendAddBroadcastSourceCallback(masterDevice,
                BassClientStateMachine.INVALID_SRC_ID, BleBroadcastAudioScanAssistCallback.BASS_STATUS_NO_EMPTY_SLOT);
            if (mBassClientService != null) {
                mBassClientService.getCallbacks().notifySourceAddFailed(masterDevice, currentMetadata,
                        BluetoothStatusCodes.ERROR_REMOTE_NOT_ENOUGH_RESOURCES);
            }
            return false;
        }

        if (isValidBroadcastSourceAddition(listOfValidDevices, srcInfo) == false) {
            sendAddBroadcastSourceCallback(masterDevice,
                BassClientStateMachine.INVALID_SRC_ID, BleBroadcastAudioScanAssistCallback.BASS_STATUS_DUPLICATE_ADDITION);
            if (mBassClientService != null) {
                mBassClientService.getCallbacks().notifySourceAddFailed(masterDevice, currentMetadata,
                        BluetoothStatusCodes.ERROR_REMOTE_NOT_ENOUGH_RESOURCES);
            }
            return false;
        }
        for (BluetoothDevice dev : listOfValidDevices) {
            BassClientStateMachine stateMachine = getOrCreateStateMachine(dev);
            if (stateMachine == null) {
                Log.w(TAG, "addBroadcastSource: device seem to be not avaiable, proceed");
                continue;
            }
            Message m = stateMachine.obtainMessage(BassClientStateMachine.ADD_BCAST_SOURCE);
            m.obj = srcInfo;
            stateMachine.sendMessage(m);
            mQueuedOps = mQueuedOps + 1;
        }
        return true;
    }

    private byte getSrcIdForCSMember(BluetoothDevice masterDevice, BluetoothDevice memberDevice, byte masterSrcId) {
        byte targetSrcId = -1;
        List<BleBroadcastSourceInfo> masterSrcInfos = getAllBroadcastSourceInformation(masterDevice);
        List<BleBroadcastSourceInfo> memberSrcInfos = getAllBroadcastSourceInformation(memberDevice);
        if (isAddSrcForConnectedPeerOnly() &&
                (memberSrcInfos != null && memberSrcInfos.size() != 0)) {
            for (int i=0; i<memberSrcInfos.size(); i++) {
                if (memberSrcInfos.get(i).getSourceId() == masterSrcId) {
                    Log.d(TAG, "member source id is matched");
                    return masterSrcId;
                }
            }
        }

        if (masterSrcInfos == null || masterSrcInfos.size() == 0 ||
            memberSrcInfos == null || memberSrcInfos.size() == 0) {
            Log.e(TAG, "master or member source Infos not available");
            return targetSrcId;
        }
        if (masterDevice.equals(memberDevice)) {
            log("master: " + masterDevice + "member:memberDevice");
            return masterSrcId;
        }
        BluetoothDevice masterSrcDevice = null;
        for (int i=0; i<masterSrcInfos.size(); i++) {
            if (masterSrcInfos.get(i).getSourceId() == masterSrcId) {
                masterSrcDevice = masterSrcInfos.get(i).getSourceDevice();
                break;
            }
        }
        if (masterSrcDevice == null) {
            Log.e(TAG, "No matching SRC Id for the operation in masterDevice");
            return targetSrcId;
        }

        //look for this srcAddress in member to retrieve the srcId
        for (int i=0; i<memberSrcInfos.size(); i++) {
            if (masterSrcDevice.equals(memberSrcInfos.get(i).getSourceDevice())) {
                targetSrcId = masterSrcInfos.get(i).getSourceId();
                break;
            }
        }
        if (targetSrcId == -1) {
            Log.e(TAG, "No matching SRC Address in the member Src Infos");
        }
        return targetSrcId;
    }

    public synchronized boolean updateBroadcastSource (BluetoothDevice masterDevice, List<BluetoothDevice> devices,
                                    BleBroadcastSourceInfo srcInfo
                                 ) {

         int status = BleBroadcastAudioScanAssistCallback.BASS_STATUS_INVALID_SOURCE_ID;
         Log.i(TAG, "updateBroadcastSource for " + devices +
                     "masterDevice " + masterDevice +
                     "SourceInfo " + srcInfo);

         if (srcInfo == null) {
             Log.e(TAG, "updateBroadcastSource: null SrcInfo");
             return false;
         }
         if (getConnectionState(masterDevice) != BluetoothProfile.STATE_CONNECTED &&
                 !isAddSrcForConnectedPeerOnly()) {
            Log.i(TAG, "Device is not connected");
            sendUpdateBroadcastSourceCallback(masterDevice, srcInfo.getSourceId(),
                    BleBroadcastAudioScanAssistCallback.BASS_STATUS_FATAL);
            if (mBassClientService != null) {
                mBassClientService.getCallbacks().notifySourceModifyFailed(masterDevice,
                        srcInfo.getSourceId(), BluetoothStatusCodes.ERROR_REMOTE_LINK_ERROR);
            }
            return false;
        }

         List<BluetoothDevice> listOfValidDevices = new ArrayList<BluetoothDevice>();
         for (BluetoothDevice dev : devices) {
             if (getSrcIdForCSMember(masterDevice, dev, srcInfo.getSourceId()) == -1) {
                 Log.i(TAG, "memberDevice: " + dev + " is invalid for updating source");
             } else {
                 Log.i(TAG, "memberDevice: " + dev + " is valid for updating source");
                 listOfValidDevices.add(dev);
             }
         }

         if (listOfValidDevices.size() == 0) {
             Log.i(TAG, "masterDevice: " + masterDevice + " is invalid for updating source");
             if (devices.size() > 1) {
                 status = BleBroadcastAudioScanAssistCallback.BASS_STATUS_INVALID_GROUP_OP;
             } else {
                 status = BleBroadcastAudioScanAssistCallback.BASS_STATUS_INVALID_SOURCE_ID;
             }
             sendRemoveBroadcastSourceCallback(masterDevice, BassClientStateMachine.INVALID_SRC_ID,
                     status);
             if (mBassClientService != null) {
                 mBassClientService.getCallbacks().notifySourceModifyFailed(masterDevice,
                         srcInfo.getSourceId(), BluetoothStatusCodes.ERROR_BAD_PARAMETERS);
             }
             return false;
         }

         for (BluetoothDevice dev : listOfValidDevices) {
             BassClientStateMachine stateMachine = getOrCreateStateMachine(dev);
             if (stateMachine == null) {
                 Log.w(TAG, "updateBroadcastSource: Device seem to be not avaiable");
                 continue;
             }
             byte targetSrcId = getSrcIdForCSMember(masterDevice, dev, srcInfo.getSourceId());
             srcInfo.setSourceId(targetSrcId);

             Message m = stateMachine.obtainMessage(BassClientStateMachine.UPDATE_BCAST_SOURCE);
             m.obj = srcInfo;
             m.arg1 = stateMachine.USER;
             stateMachine.sendMessage(m);
             mQueuedOps = mQueuedOps + 1;
         }

        return true;
    }

    public synchronized boolean setBroadcastCode(BluetoothDevice masterDevice, List<BluetoothDevice> devices,
                                      BleBroadcastSourceInfo srcInfo, boolean noCheckSourceId) {

        int status = BleBroadcastAudioScanAssistCallback.BASS_STATUS_INVALID_SOURCE_ID;
        Log.i(TAG, "setBroadcastCode for " + devices +
                   "masterDevice" + masterDevice +
                   "Broadcast PIN" + srcInfo.getBroadcastCode());

        if (srcInfo == null) {
            Log.e(TAG, "setBroadcastCode: null SrcInfo");
            return false;
        }

        if (!noCheckSourceId) {
            for (BluetoothDevice dev : devices) {
                if (getSrcIdForCSMember(masterDevice, dev, srcInfo.getSourceId()) == -1) {
                    if (devices.size() > 1) {
                        status = BleBroadcastAudioScanAssistCallback.BASS_STATUS_INVALID_GROUP_OP;
                    } else {
                        status = BleBroadcastAudioScanAssistCallback.BASS_STATUS_INVALID_SOURCE_ID;
                    }
                    sendRemoveBroadcastSourceCallback(masterDevice, BassClientStateMachine.INVALID_SRC_ID,
                            status);
                    return false;
                }
            }
        }

        for (BluetoothDevice dev : devices) {
            BassClientStateMachine stateMachine = getOrCreateStateMachine(dev);
            if (stateMachine == null) {
                 Log.w(TAG, "setBroadcastCode: Device seem to be not avaiable");
                 continue;
            }
            if (!noCheckSourceId) {
                byte targetSrcId = getSrcIdForCSMember(masterDevice, dev, srcInfo.getSourceId());
                srcInfo.setSourceId(targetSrcId);
            }
            Message m = stateMachine.obtainMessage(BassClientStateMachine.SET_BCAST_CODE);
            m.obj = srcInfo;
            m.arg1 = stateMachine.FRESH;
            stateMachine.sendMessage(m);
            mQueuedOps = mQueuedOps + 1;
        }

        return true;
    }

    private boolean isBroadcastSourceInfoPASyncOn (BleBroadcastSourceInfo srcInfo) {
        boolean ret = false;
        if (srcInfo != null && srcInfo.getMetadataSyncState() ==
                 BleBroadcastSourceInfo.BROADCAST_ASSIST_PA_SYNC_STATE_IN_SYNC) {
            ret = true;
        }
        return ret;
    }

    public synchronized boolean removeBroadcastSource (BluetoothDevice masterDevice, List<BluetoothDevice> devices,
                                    byte sourceId
                                 ) {

        int status = BleBroadcastAudioScanAssistCallback.BASS_STATUS_INVALID_SOURCE_ID;
        Log.i(TAG,  "removeBroadcastSource for " + devices +
                   "masterDevice " + masterDevice +
                    "removeBroadcastSource: sourceId:" + sourceId);

        if (sourceId == BassClientStateMachine.INVALID_SRC_ID) {
            Log.e(TAG, "removeBroadcastSource: Invalid source Id");
            return false;
        }
        if (getConnectionState(masterDevice) != BluetoothProfile.STATE_CONNECTED &&
                !isAddSrcForConnectedPeerOnly()) {
            Log.i(TAG, "Device is not connected");
            sendRemoveBroadcastSourceCallback(masterDevice, sourceId,
                    BleBroadcastAudioScanAssistCallback.BASS_STATUS_FATAL);
            if (mBassClientService != null) {
                mBassClientService.getCallbacks().notifySourceRemoveFailed(masterDevice,
                        sourceId, BluetoothStatusCodes.ERROR_REMOTE_LINK_ERROR);
            }
            return false;
        }
        List<BluetoothDevice> listOfValidDevices = new ArrayList<BluetoothDevice>();
        for (BluetoothDevice dev : devices) {
            if (getSrcIdForCSMember(masterDevice, dev, sourceId) == -1) {
                Log.i(TAG, "memberDevice: " + dev + " is invalid for removing source " + sourceId);
            } else {
                Log.i(TAG, "memberDevice: " + dev + " is valid for removing source " + sourceId);
                listOfValidDevices.add(dev);
            }
        }

        if (listOfValidDevices.size() == 0) {
             Log.i(TAG, "masterDevice: " + masterDevice + " is invalid for removing source " + sourceId);
             if (devices.size() > 1) {
                 status = BleBroadcastAudioScanAssistCallback.BASS_STATUS_INVALID_GROUP_OP;
             } else {
                 status = BleBroadcastAudioScanAssistCallback.BASS_STATUS_INVALID_SOURCE_ID;
             }
             sendRemoveBroadcastSourceCallback(masterDevice, BassClientStateMachine.INVALID_SRC_ID,
                     status);
             if (mBassClientService != null) {
                 mBassClientService.getCallbacks().notifySourceRemoveFailed(masterDevice, sourceId,
                     BluetoothStatusCodes.ERROR_BAD_PARAMETERS);
             }
             return false;
        }

        for (BluetoothDevice dev : listOfValidDevices) {
            BassClientStateMachine stateMachine = getOrCreateStateMachine(dev);
            if (stateMachine == null) {
                Log.w(TAG, "setBroadcastCode: Device seem to be not avaiable");
                continue;
            }

            Message m = stateMachine.obtainMessage(BassClientStateMachine.REMOVE_BCAST_SOURCE);
            m.arg1 = getSrcIdForCSMember(masterDevice, dev, sourceId);
            log("removeBroadcastSource: send message to SM " + dev);
            int removeSrcDelay = 0;
            if (isBroadcastSourceInfoPASyncOn(stateMachine.getBroadcastSourceInfoForSourceId(m.arg1))) {
                log("delay remove src to ensure pending update src completes");
                removeSrcDelay = 120;
            }
            stateMachine.sendMessageDelayed(m, removeSrcDelay);
            mQueuedOps = mQueuedOps + 1;
        }

        return true;
    }

    public List<BleBroadcastSourceInfo> getAllBroadcastSourceInformation (BluetoothDevice device
                                    ) {
        Log.i(TAG, "getAllBroadcastSourceInformation for " + device);
        synchronized (mStateMachines) {
            BassClientStateMachine sm = getOrCreateStateMachine(device);
            if (sm == null) {
                return null;
            }
            return sm.getAllBroadcastSourceInformation();
        }
    }

    private BassClientStateMachine getOrCreateStateMachine(BluetoothDevice device) {
        if (device == null) {
            Log.e(TAG, "getOrCreateStateMachine failed: device cannot be null");
            return null;
        }
        synchronized (mStateMachines) {
            BassClientStateMachine sm = mStateMachines.get(device);
            if (sm != null) {
                return sm;
            }
            // Limit the maximum number of state machines to avoid DoS attack
            if (mStateMachines.size() >= MAX_BASS_CLIENT_STATE_MACHINES) {
                Log.e(TAG, "Maximum number of Bassclient state machines reached: "
                        + MAX_BASS_CLIENT_STATE_MACHINES);
                return null;
            }
            if (DBG) {
                Log.d(TAG, "Creating a new state machine for " + device);
            }
            sm = BassClientStateMachine.make(device, this,
                    mStateMachinesThread.getLooper());
            mStateMachines.put(device, sm);
            return sm;
        }
    }

    private BassCsetManager getOrCreateCSetManager(int setId, BluetoothDevice masterDevice) {
        if (setId == -1) {
            Log.e(TAG, "getOrCreateCSetManager failed: invalid setId");
            return null;
        }
        synchronized (mSetManagers) {
            BassCsetManager sm = mSetManagers.get(setId);
            log("getOrCreateCSetManager: hashmap Entry:" + sm);
            if (sm != null) {
                return sm;
            }
            // Limit the maximum number of set manager state machines
            if (mStateMachines.size() >= MAX_BASS_CLIENT_CSET_MEMBERS) {
                Log.e(TAG, "Maximum number of Bassclient cset members reached: "
                        + MAX_BASS_CLIENT_CSET_MEMBERS);
                return null;
            }
            if (DBG) {
                Log.d(TAG, "Creating a new set Manager for " + setId);
            }
            sm = BassCsetManager.make(setId, masterDevice, this,
                    mSetManagerThread.getLooper());
            mSetManagers.put(setId, sm);
            return sm;
        }
    }

    public boolean isLockSupportAvailable(BluetoothDevice device) {
        boolean isLockAvail = false;
        boolean forceNoCsip = SystemProperties.getBoolean("persist.vendor.service.bt.forceNoCsip", true);
        if (forceNoCsip) {
            log("forceNoCsip is set");
            return isLockAvail;
        }
        //*_CSIP
        isLockAvail = mAdapterService.isGroupExclAccessSupport(device);
        //_CSIP*/

        log("isLockSupportAvailable for:" + device + "returns " + isLockAvail);
        return isLockAvail;
    }

    private int getCsetId(BluetoothDevice device) {
        int setId = 1;
        //*_CSIP
        ParcelUuid uuid = null;
        if (mCsipWrapper != null &&
            mCsipWrapper.checkIncludingServiceForDeviceGroup(device, CAP_UUID)) {
            uuid = CAP_UUID;
        }
        setId = mCsipWrapper.getRemoteDeviceGroupId(device, uuid);
        //_CSIP*/
        log("getCsetId return:" + setId);
        return setId;
    }
    public boolean stopScanOffloadInternal (BluetoothDevice device, boolean isGroupOp) {
        boolean ret = false;
        log("stopScanOffloadInternal: device: " + device
             + "isGroupOp" + isGroupOp);
        List<BluetoothDevice> listOfDevices = new ArrayList<BluetoothDevice>();
        /* Even If the request is for Group, If Lock support not avaiable
         * for that device, go ahead and treat this as single device operation
         */
        if (isGroupOp) {
            int setId = getCsetId(device);
            DeviceGroup devGrp = mCsipWrapper.getCoordinatedSet(setId);
            if (devGrp != null) {
                listOfDevices = devGrp.getDeviceGroupMembers();
            }
        } else {
            listOfDevices.add(device);
        }
        ret = stopScanOffload(device, listOfDevices);
        return ret;
    }

    public boolean startScanOffloadInternal (BluetoothDevice device, boolean isGroupOp) {
        boolean ret = false;
        log("startScanOffloadInternal: device: " + device
             + "isGroupOp" + isGroupOp);
        List<BluetoothDevice> listOfDevices = new ArrayList<BluetoothDevice>();
        /* Even If the request is for Group, If Lock support not avaiable
         * for that device, go ahead and treat this as single device operation
         */
        if (isGroupOp) {
            int setId = getCsetId(device);
            DeviceGroup devGrp = mCsipWrapper.getCoordinatedSet(setId);
            if (devGrp != null) {
                listOfDevices = devGrp.getDeviceGroupMembers();
            }
        } else {
           listOfDevices.add(device);
        }
        ret = startScanOffload(device, listOfDevices);
        return ret;
    }

    public boolean addBroadcastSourceInternal (BluetoothDevice device, BleBroadcastSourceInfo srcInfo,
                                      boolean isGroupOp) {
        boolean ret = false;
        log("addBroadcastSourceInternal: device: " + device
            + "srcInfo" + srcInfo
            + "isGroupOp" + isGroupOp);
        List<BluetoothDevice> listOfDevices = new ArrayList<BluetoothDevice>();
        /* Even If the request is for Group, If Lock support not avaiable
         * for that device, go ahead and treat this as single device operation
         */
        if (isGroupOp) {
            int setId = getCsetId(device);
            DeviceGroup devGrp = mCsipWrapper.getCoordinatedSet(setId);
            if (devGrp != null) {
                listOfDevices = devGrp.getDeviceGroupMembers();
            }
        } else {
           listOfDevices.add(device);
        }
        ret = addBroadcastSource(device, listOfDevices, srcInfo);
        return ret;
    }

    public boolean updateBroadcastSourceInternal (BluetoothDevice device, BleBroadcastSourceInfo srcInfo,
                                                          boolean isGroupOp
                                      ) {
        boolean ret = false;
        log("updateBroadcastSourceInternal: device: " + device
            + "srcInfo" + srcInfo
            + "isGroupOp" + isGroupOp);
        List<BluetoothDevice> listOfDevices = new ArrayList<BluetoothDevice>();
        /* Even If the request is for Group, If Lock support not avaiable
         * for that device, go ahead and treat this as single device operation
         */
        if (isGroupOp) {
            int setId = getCsetId(device);
            DeviceGroup devGrp = mCsipWrapper.getCoordinatedSet(setId);
            if (devGrp != null) {
                listOfDevices = devGrp.getDeviceGroupMembers();
            }
        } else {
           listOfDevices.add(device);
        }
        ret = updateBroadcastSource(device, listOfDevices, srcInfo);
        return ret;
    }

    protected boolean setBroadcastCodeInternal (BluetoothDevice device, BleBroadcastSourceInfo srcInfo,
            boolean isGroupOp) {
        return setBroadcastCodeInternal(device, srcInfo, isGroupOp, false);
    }

    protected boolean setBroadcastCodeInternal (BluetoothDevice device, BleBroadcastSourceInfo srcInfo,
                                                   boolean isGroupOp, boolean noCheckSourceId) {
        boolean ret = false;
        log("setBroadcastCodeInternal: device: " + device
            + "srcInfo" + srcInfo
            + "isGroupOp" + isGroupOp);
        List<BluetoothDevice> listOfDevices = new ArrayList<BluetoothDevice>();
        /* Even If the request is for Group, If Lock support not avaiable
         * for that device, go ahead and treat this as single device operation
         */
        if (isGroupOp) {
            int setId = getCsetId(device);
            DeviceGroup devGrp = mCsipWrapper.getCoordinatedSet(setId);
            if (devGrp != null) {
                listOfDevices = devGrp.getDeviceGroupMembers();
            }
        } else {
           listOfDevices.add(device);
        }
        ret = setBroadcastCode(device, listOfDevices, srcInfo, noCheckSourceId);
        return ret;
    }

    public boolean removeBroadcastSourceInternal (BluetoothDevice device, byte sourceId, boolean isGroupOp
                                       ) {
         boolean ret = false;
         List<BluetoothDevice> listOfDevices = new ArrayList<BluetoothDevice>();
         /* Even If the request is for Group, If Lock support not avaiable
          * for that device, go ahead and treat this as single device operation
          */
         if (isGroupOp) {
             int setId = getCsetId(device);
             DeviceGroup devGrp = mCsipWrapper.getCoordinatedSet(setId);
             if (devGrp != null) {
                 listOfDevices = devGrp.getDeviceGroupMembers();
             }
         } else {
            listOfDevices.add(device);
         }
         ret = removeBroadcastSource(device, listOfDevices, sourceId);
         return ret;
     }

    public boolean selectBroadcastSourceInternal (BluetoothDevice device,
            ScanResult scanRes, boolean isGroupOp, boolean auto) {
        boolean ret = false;

        Log.i(TAG, "selectBroadcastSourceInternal for " + device + "isGroupOp:" + isGroupOp);
        Log.i(TAG, "ScanResult " + scanRes);
        List<BluetoothDevice> listOfDevices = new ArrayList<BluetoothDevice>();
        if (isGroupOp) {
            int setId = getCsetId(device);
            DeviceGroup devGrp = mCsipWrapper.getCoordinatedSet(setId);
            if (devGrp != null) {
                listOfDevices = devGrp.getDeviceGroupMembers();
            } else {
                listOfDevices.add(device);
            }
        } else {
            listOfDevices.add(device);
        }
        for (BluetoothDevice dev : listOfDevices) {
            if (isAddSrcForConnectedPeerOnly()) {
                if (getConnectionState(dev) != BluetoothProfile.STATE_CONNECTED) {
                    continue;
                }
            }
            ret = selectBroadcastSource(dev, scanRes, isGroupOp, auto);
            if (!ret) {
                Log.d(TAG, "selectBroadcastSource fail");
                break;
            }
        }
        return ret;
    }

    static void log(String msg) {
        if (BassClientStateMachine.BASS_DBG) {
           Log.d(TAG, msg);
        }
    }

    public void setBassClientSevice(BassClientService bassService) {
        Log.d(TAG, "setBassClientSevice: " + bassService);
        mBassClientService = bassService;
    }

    public BassClientService getBassClientService() {
        Log.d(TAG, "getBassClientService: " + mBassClientService);
        return mBassClientService;
    }

    public void startSearchingForSources(List<ScanFilter> filters) {
        if (filters == null) {
            Log.d(TAG, "startSearchingForSources: filters is null");
            return;
        }
        Log.d(TAG, "Reset all mIsNotified flags");
        clearAllNotifiedFlags();
        if (bassUtils != null) {
            bassUtils.searchforLeAudioBroadcasters(null, null, filters);
        } else {
            Log.e(TAG, "searchforLeAudioBroadcasters :Null Bass Util Handle");
        }
    }

    public void stopSearchingForSources() {
        if (bassUtils != null) {
            bassUtils.stopSearchforLeAudioBroadcasters(null, null);
        } else {
            Log.e(TAG, "searchforLeAudioBroadcasters :Null Bass Util Handle");
        }
    }

    public boolean isSearchInProgress() {
        boolean ret = false;
        if (bassUtils != null) {
            ret = bassUtils.isSearchInProgress();
        } else {
            Log.e(TAG, "searchforLeAudioBroadcasters :Null Bass Util Handle");
            ret = false;
        }
        return ret;
    }

    BleBroadcastSourceInfo convertFromMetadata(BluetoothLeBroadcastMetadata metadata) {
        List<BluetoothLeBroadcastSubgroup> subGroups = metadata.getSubgroups();
        Map<Integer, Integer> bisIndexList = new HashMap<Integer, Integer>();
        Map<Integer, byte[]> metadataList = new HashMap<Integer, byte[]>();

        for (int i = 0; i < subGroups.size(); i ++) {
            List<BluetoothLeBroadcastChannel> chans = subGroups.get(i).getChannels();
            int audioBisIndex = 0;
            for(int j = 0; j < chans.size(); j ++) {
                audioBisIndex = audioBisIndex | (1 << chans.get(j).getChannelIndex());
            }
            bisIndexList.put(i, audioBisIndex);
            metadataList.put(i, subGroups.get(i).getContentMetadata().getRawMetadata());
        }

        return new BleBroadcastSourceInfo(
                metadata.getSourceDevice(),
                BleBroadcastSourceInfo.BROADCAST_ASSIST_INVALID_SOURCE_ID,
                (byte) metadata.getSourceAdvertisingSid(),
                metadata.getBroadcastId(),
                metadata.getSourceAddressType(),
                BleBroadcastSourceInfo.BROADCAST_ASSIST_PA_SYNC_STATE_IN_SYNC,
                BleBroadcastSourceInfo.BROADCAST_ASSIST_AUDIO_SYNC_STATE_INVALID,
                metadata.isEncrypted() ? BleBroadcastSourceInfo.BROADCAST_ASSIST_ENC_STATE_PIN_NEEDED :
                        BleBroadcastSourceInfo.BROADCAST_ASSIST_ENC_STATE_UNENCRYPTED,
                metadata.getBroadcastCode() != null ? new String(metadata.getBroadcastCode()) : null,
                null, // badCode
                (byte)metadata.getSubgroups().size(),
                bisIndexList,
                metadataList
        );
    }

    BleBroadcastSourceInfo convertFromMetadata(BluetoothLeBroadcastMetadata metadata,
            int sourceId) {
        BleBroadcastSourceInfo srcInfo;
        srcInfo = convertFromMetadata(metadata);
        srcInfo.setSourceId((byte) sourceId);
        return srcInfo;
    }

    BleBroadcastSourceInfo convertFromMetadata(BluetoothLeBroadcastMetadata metadata,
            int sourceId, int syncState) {
        BleBroadcastSourceInfo srcInfo;
        srcInfo = convertFromMetadata(metadata);
        srcInfo.setSourceId((byte) sourceId);
        srcInfo.setMetadataSyncState(syncState);
        return srcInfo;
    }

    BluetoothLeBroadcastReceiveState convertFromSourceInfo(BleBroadcastSourceInfo srcInfo) {
        List<BluetoothLeAudioContentMetadata> metadata = new ArrayList<BluetoothLeAudioContentMetadata>();
        List<byte[]> metaList = new ArrayList(srcInfo.getMetadataList().values());
        List<Integer> bisIndexList = new ArrayList(srcInfo.getBisIndexList().values());
        List<Long> bisSyncState = new ArrayList<Long>();
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

        for(int i = 0; i < metaList.size(); i ++) {
            BluetoothLeAudioContentMetadata data =
                    BluetoothLeAudioContentMetadata.fromRawBytes(metaList.get(i));
            String program = data.getProgramInfo();
            BluetoothDevice device = srcInfo.getSourceDevice();
            if ((program == null || program.length() == 0) &&
                    device != null) {
                String name = device.getName();
                BluetoothDevice localDev = getLocalBroadcastSource();
                // collocated case
                if (device.equals(localDev)) {
                    BroadcastService baService = BroadcastService.getBroadcastService();
                    if (baService != null && baService.getProgramInfo() != null) {
                        name = baService.getProgramInfo();
                    } else {
                        name = BluetoothAdapter.getDefaultAdapter().getName();
                    }
                }
                log("Update program with device name: " + name);
                data = new BluetoothLeAudioContentMetadata.Builder(data)
                        .setProgramInfo(name)
                        .build();
            }
            metadata.add(data);
        }
        for(int i = 0; i < bisIndexList.size(); i ++) {
            bisSyncState.add(new Long(bisIndexList.get(i)));
        }
        String emptyBluetoothDevice = "00:00:00:00:00:00";
        BluetoothDevice bluetoothDevice = srcInfo.getSourceDevice();
        int addressType = srcInfo.getAdvAddressType();
        if (bluetoothDevice == null) {
            bluetoothDevice = btAdapter.getRemoteDevice(emptyBluetoothDevice);
            addressType = BluetoothDevice.ADDRESS_TYPE_PUBLIC;
        }
        return new BluetoothLeBroadcastReceiveState(
                (int) srcInfo.getSourceId(),
                addressType,
                bluetoothDevice,
                (int) srcInfo.getAdvertisingSid(),
                srcInfo.getBroadcasterId(),
                srcInfo.getMetadataSyncState(),
                srcInfo.getEncryptionStatus(),
                srcInfo.getBadBroadcastCode(),
                (int) srcInfo.getNumberOfSubGroups(),
                bisSyncState,
                metadata
        );
    }

    public boolean isAddedSource(BluetoothDevice device) {
        boolean isPresent = false;
        BluetoothDevice activeDevice = getActiveLeAudioDevice();
        log("Current active media device is: " + activeDevice);
        synchronized (mStateMachines) {
            for (BassClientStateMachine sm : mStateMachines.values()) {
                if (sm.isConnected() && sm.getDevice().equals(activeDevice)) {
                    List<BleBroadcastSourceInfo> sourceList =
                            getAllBroadcastSourceInformation(sm.getDevice());
                    if (sourceList == null) {
                        continue;
                    }
                    for (int j = 0; j < sourceList.size(); j ++) {
                        BleBroadcastSourceInfo srcInfo = sourceList.get(j);
                        if (!srcInfo.isEmptyEntry() &&
                                srcInfo.getSourceDevice().equals(device)) {
                            isPresent = true;
                            log("Already added");
                            break;
                        }
                    }
                }
            }
        }
        return isPresent;
    }

    public void addSource(BluetoothDevice sink, BluetoothLeBroadcastMetadata sourceMetadata,
                          boolean isGroupOp) {
        if (sourceMetadata == null) {
            Log.e(TAG, "addSource: sourceMetadata is null");
            return;
        }
        log("addSource: isEncrypted = " + sourceMetadata.isEncrypted() + " isGroupOp = " + isGroupOp);
        if (sourceMetadata.isEncrypted()) {
            if (sourceMetadata.getBroadcastCode() != null) {
                log("Broadcast code length: " + sourceMetadata.getBroadcastCode().length);
                BassClientStateMachine.logByteArray("Broadcast code: ",
                        sourceMetadata.getBroadcastCode(), 0,
                        sourceMetadata.getBroadcastCode().length);
                if (sourceMetadata.getBroadcastCode().length > MAX_BROADCAST_CODE_LENGTH) {
                    if (mBassClientService != null) {
                        mBassClientService.getCallbacks().notifySourceAddFailed(
                                sink, sourceMetadata, BluetoothStatusCodes.ERROR_LE_BROADCAST_INVALID_CODE);
                    }
                    log("Invalid broadcast code length");
                    return;
                }
            } else {
                log("getBroadcastCode is null");
            }
        }
        boolean isGroupOpDevice = false;
        int setId = getCsetId(sink);
        DeviceGroup devGrp = mCsipWrapper.getCoordinatedSet(setId);
        if (devGrp != null) {
            isGroupOpDevice = isGroupOp;
        } else {
            log("addSource: treat it as single device operation for non-CSIP device: " + sink);
        }
        if (mGroupOpMap != null) {
            mGroupOpMap.put(setId, isGroupOpDevice);
        }
        currentMetadata = sourceMetadata;
        BleBroadcastSourceInfo srcInfo = convertFromMetadata(sourceMetadata);
        int broadcastId = srcInfo.getBroadcasterId();
        BluetoothDevice srcAddress = srcInfo.getSourceDevice();
        PAResults paRes = getPAResults(srcAddress);
        ArrayList<BluetoothDevice> activeSyncedSrcList = getActiveSyncedSource(sink);
        if ((paRes == null || getBASE(paRes.mSyncHandle) == null
                || activeSyncedSrcList == null || !activeSyncedSrcList.contains(srcAddress))
                && bassUtils != null) {
            log("PA not synced, try to sync");
            ScanResult result = bassUtils.getScanBroadcast(broadcastId);
            if (result == null) {
                log("Cannot find scan result, fake a scan result");
                int sid = sourceMetadata.getSourceAdvertisingSid();
                if (sid == -1) {
                    sid = 0; // advertising set id 0 by default
                }
                BluetoothDevice source = sourceMetadata.getSourceDevice();
                int addressType = sourceMetadata.getSourceAddressType();
                int bId = sourceMetadata.getBroadcastId();
                byte[] advData = {6, 0x16, 0x52, 0x18, (byte)(bId & 0xFF),
                        (byte)((bId >> 8) & 0xFF), (byte)((bId >> 16) & 0xFF)};
                ScanRecord record = ScanRecord.parseFromBytes(advData);
                result = new ScanResult(source, addressType, 0x1 /* eventType */,
                        0x1 /* primaryPhy */, 0x2 /* secondaryPhy */, sid, 0 /* txPower */,
                        0 /* rssi */, 0 /* periodicAdvertisingInterval */, record,
                        0 /* timestampNanos */);
            }
            if (result != null) {
                log("select broadcast source");
                selectBroadcastSourceInternal(sink, result, isGroupOpDevice, false);
            }
        } else {
            startScanOffloadInternal(sink, isGroupOpDevice);
        }
        boolean ret = addBroadcastSourceInternal(sink, srcInfo, isGroupOpDevice);
        String broadcastCode = srcInfo.getBroadcastCode();
        if (sourceMetadata.isEncrypted() && ret == true && broadcastCode != null && broadcastCode.length() > 0) {
            log("setBroadcastCodeInternal: broadcastCode = " + broadcastCode);
            setBroadcastCodeInternal(sink, srcInfo, isGroupOpDevice, true);
        }
    }

    public void modifySource(BluetoothDevice sink, int sourceId,
                             BluetoothLeBroadcastMetadata updatedMetadata) {
        if (updatedMetadata == null) {
            Log.e(TAG, "modifySource: updatedMetadata is null");
            return;
        }

        boolean isGroupOpDevice = false;
        int setId = getCsetId(sink);
        DeviceGroup devGrp = mCsipWrapper.getCoordinatedSet(setId);
        if (devGrp != null) {
            if (mGroupOpMap != null && mGroupOpMap.containsKey(setId)) {
                isGroupOpDevice = mGroupOpMap.get(setId);
            } else {
                log("modifySource: treat it as group device operation for CSIP device: " + sink);
                isGroupOpDevice = true;
            }
        }
        Log.i(TAG, "modifySource: isGroupOpDevice = " + isGroupOpDevice);
        log("modifySource: isEncrypted = " + updatedMetadata.isEncrypted());
        if (updatedMetadata.isEncrypted()) {
            if (updatedMetadata.getBroadcastCode() != null) {
                log("Broadcast code length: " + updatedMetadata.getBroadcastCode().length);
                BassClientStateMachine.logByteArray("Broadcast code: ",
                        updatedMetadata.getBroadcastCode(), 0,
                        updatedMetadata.getBroadcastCode().length);
                if (updatedMetadata.getBroadcastCode().length > MAX_BROADCAST_CODE_LENGTH) {
                    if (mBassClientService != null) {
                        mBassClientService.getCallbacks().notifySourceModifyFailed(
                                sink, sourceId, BluetoothStatusCodes.ERROR_LE_BROADCAST_INVALID_CODE);
                    }
                    log("Invalid broadcast code length");
                    return;
                }
            } else {
                log("getBroadcastCode is null");
            }
        }
        currentMetadata = updatedMetadata;
        BleBroadcastSourceInfo srcInfo = convertFromMetadata(updatedMetadata, sourceId);
        String broadcastCode = srcInfo.getBroadcastCode();
        boolean ret = updateBroadcastSourceInternal(sink, srcInfo, isGroupOpDevice);
        if (updatedMetadata.isEncrypted() && ret == true && broadcastCode != null && broadcastCode.length() > 0) {
            log("setBroadcastCodeInternal: broadcastCode = " + broadcastCode);
            setBroadcastCodeInternal(sink, srcInfo, isGroupOpDevice);
        }
    }

    public BluetoothLeBroadcastMetadata getCurrentMetadata() {
        return currentMetadata;
    }

    public void removeSource(BluetoothDevice sink, int sourceId) {
        if (getConnectionState(sink) != BluetoothProfile.STATE_CONNECTED) {
            Log.i(TAG, "Device is not connected");
            if (mBassClientService != null) {
                mBassClientService.getCallbacks().notifySourceRemoveFailed(sink,
                        sourceId, BluetoothStatusCodes.ERROR_REMOTE_LINK_ERROR);
            }
        }

        boolean isGroupOpDevice = false;
        int setId = getCsetId(sink);
        DeviceGroup devGrp = mCsipWrapper.getCoordinatedSet(setId);
        if (devGrp != null) {
            if (mGroupOpMap != null && mGroupOpMap.containsKey(setId)) {
                isGroupOpDevice = mGroupOpMap.get(setId);
            } else {
                log("removeSource: treat it as group device operation for CSIP device: " + sink);
                isGroupOpDevice = true;
            }
        }
        Log.i(TAG, "removeSource: sink " + sink + " isGroupOpDevice = " + isGroupOpDevice);

        boolean isFound = false;
        if (isGroupOpDevice) {
            List<BluetoothDevice> grpMembers = devGrp.getDeviceGroupMembers();
            for (BluetoothDevice memberDevice: grpMembers) {
                List<BleBroadcastSourceInfo> sourceInfo = getAllBroadcastSourceInformation(memberDevice);
                for(int i = 0; i < sourceInfo.size(); i ++) {
                    BleBroadcastSourceInfo srcInfo = sourceInfo.get(i);
                    if (srcInfo.getSourceId() == (byte) sourceId &&
                        srcInfo.getSourceDevice() != null) {
                        isFound = true;
                        int metaSyncState = srcInfo.getMetadataSyncState();
                        int audioSyncState = srcInfo.getAudioSyncState();
                        Log.d(TAG, "metaSyncState: " + metaSyncState +
                                " audioSyncState: " + audioSyncState);
                        if (metaSyncState == BleBroadcastSourceInfo.BROADCAST_ASSIST_PA_SYNC_STATE_IN_SYNC ||
                                audioSyncState == BleBroadcastSourceInfo.BROADCAST_ASSIST_AUDIO_SYNC_STATE_SYNCHRONIZED) {
                            Log.d(TAG, "Update source with PA and audio sync off");
                            srcInfo.setMetadataSyncState(
                                    BleBroadcastSourceInfo.BROADCAST_ASSIST_PA_SYNC_STATE_IDLE);
                            srcInfo.setAudioSyncState(
                                    BleBroadcastSourceInfo.BROADCAST_ASSIST_AUDIO_SYNC_STATE_NOT_SYNCHRONIZED);
                            updateBroadcastSourceInternal(memberDevice, srcInfo, false);
                        }
                   }
                }
            }
        } else {
            List<BleBroadcastSourceInfo> sourceInfo = getAllBroadcastSourceInformation(sink);
            for(int i = 0; i < sourceInfo.size(); i ++) {
                BleBroadcastSourceInfo srcInfo = sourceInfo.get(i);
                if (srcInfo.getSourceId() == (byte) sourceId &&
                    srcInfo.getSourceDevice() != null) {
                    isFound = true;
                    int metaSyncState = srcInfo.getMetadataSyncState();
                    int audioSyncState = srcInfo.getAudioSyncState();
                    Log.d(TAG, "metaSyncState: " + metaSyncState +
                            " audioSyncState: " + audioSyncState);
                    if (metaSyncState == BleBroadcastSourceInfo.BROADCAST_ASSIST_PA_SYNC_STATE_IN_SYNC ||
                            audioSyncState == BleBroadcastSourceInfo.BROADCAST_ASSIST_AUDIO_SYNC_STATE_SYNCHRONIZED) {
                        Log.d(TAG, "Update source with PA and audio sync off");
                        srcInfo.setMetadataSyncState(
                                BleBroadcastSourceInfo.BROADCAST_ASSIST_PA_SYNC_STATE_IDLE);
                        srcInfo.setAudioSyncState(
                                BleBroadcastSourceInfo.BROADCAST_ASSIST_AUDIO_SYNC_STATE_NOT_SYNCHRONIZED);
                        updateBroadcastSourceInternal(sink, srcInfo, false);
                    }
               }
            }
        }

        if (isFound) {
            removeBroadcastSourceInternal(sink, (byte) sourceId, isGroupOpDevice);
        } else {
            Log.d(TAG, "SourceId is not found");
            if (mBassClientService != null) {
                mBassClientService.getCallbacks().notifySourceRemoveFailed(sink, sourceId,
                        BluetoothStatusCodes.ERROR_BAD_PARAMETERS);
            }
        }
    }

    public List<BluetoothLeBroadcastReceiveState> getAllSources(BluetoothDevice sink) {
        log("getAllSources for " + sink);
        int setId = getCsetId(sink);
        DeviceGroup devGrp = mCsipWrapper.getCoordinatedSet(setId);
        boolean isGroupOpDevice = false;
        if (devGrp != null) {
            if (mGroupOpMap != null && mGroupOpMap.containsKey(setId)) {
                isGroupOpDevice = mGroupOpMap.get(setId);
            } else {
                log("getAllSources: treat it as group device operation for CSIP device: " + sink);
                isGroupOpDevice = true;
            }
        }

        List<BluetoothDevice> listOfDevices = new ArrayList<BluetoothDevice>();
        if (isAddSrcForConnectedPeerOnly() && isGroupOpDevice) {
            listOfDevices = devGrp.getDeviceGroupMembers();
        } else {
            listOfDevices.add(sink);
        }

        List<BluetoothLeBroadcastReceiveState> recvState = new ArrayList<BluetoothLeBroadcastReceiveState>();
        for (BluetoothDevice dev : listOfDevices) {
            log("getAllSources for member " + dev);
            List<BleBroadcastSourceInfo> sourceInfo = getAllBroadcastSourceInformation(dev);
            if (sourceInfo == null) {
                log("sourceInfo is null for dev " + dev);
                continue;
            }
            for(int i = 0; i < sourceInfo.size(); i ++) {
                BleBroadcastSourceInfo srcInfo = sourceInfo.get(i);
                if (!srcInfo.isEmptyEntry()) {
                    log("source info (" + i + "): sourceId = " + srcInfo.getSourceId());
                    recvState.add(convertFromSourceInfo(srcInfo));
                }
            }
            // return recvState if it is not empty
            if (recvState.size() > 0) {
                break;
            }
        }
        return recvState;
    }

    int getMaximumSourceCapacity(BluetoothDevice sink) {
        log("getMaximumSourceCapacity: device = " + sink);
        BassClientStateMachine stateMachine = getOrCreateStateMachine(sink);
        if (stateMachine == null) {
            log("stateMachine is null");
            return 0;
        }
        return stateMachine.getMaximumSourceCapacity();
    }

    /**
     * Binder object: must be a static class or memory leak may occur
     */
    @VisibleForTesting
    static class BluetoothSyncHelperBinder extends IBluetoothSyncHelper.Stub
            implements IProfileServiceBinder {
        private BCService mService;

        private BCService getService() {
            if (!Utils.checkCallerIsSystemOrActiveUser(TAG)) {
                return null;
            }

            if (mService != null && mService.isAvailable()) {
                return mService;
            }
            return null;
        }

        BluetoothSyncHelperBinder(BCService svc) {
            mService = svc;
        }

        @Override
        public void cleanup() {
            mService = null;
        }

        @Override
        public boolean connect(BluetoothDevice device, AttributionSource source) {
            BCService service = getService();
            if (service == null|| !Utils.checkConnectPermissionForDataDelivery(
                                   service, source, TAG)) {
                return false;
            }
            return service.connect(device);
        }

        @Override
        public boolean disconnect(BluetoothDevice device, AttributionSource source) {
            BCService service = getService();
            if (service == null|| !Utils.checkConnectPermissionForDataDelivery(
                                   service, source, TAG)) {
                return false;
            }
            return service.disconnect(device);
        }

        @Override
        public List<BluetoothDevice> getConnectedDevices(AttributionSource source) {
            BCService service = getService();
            if (service == null|| !Utils.checkConnectPermissionForDataDelivery(
                                   service, source, TAG)) {
                return new ArrayList<>();
            }
            return service.getConnectedDevices();
        }

        @Override
        public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states, AttributionSource source) {
            BCService service = getService();
            if (service == null|| !Utils.checkConnectPermissionForDataDelivery(
                                   service, source, TAG)) {
                return new ArrayList<>();
            }
            return service.getDevicesMatchingConnectionStates(states);
        }

        @Override
        public int getConnectionState(BluetoothDevice device, AttributionSource source) {
            BCService service = getService();
            if (service == null|| !Utils.checkConnectPermissionForDataDelivery(
                                   service, source, TAG)) {
                return BluetoothProfile.STATE_DISCONNECTED;
            }
            return service.getConnectionState(device);
        }

        @Override
        public boolean setConnectionPolicy(BluetoothDevice device, int connectionPolicy, AttributionSource source) {
            BCService service = getService();
            if (service == null|| !Utils.checkConnectPermissionForDataDelivery(
                                   service, source, TAG)) {
                return false;
            }
           return service.setConnectionPolicy(device, connectionPolicy);
        }

        @Override
        public int getConnectionPolicy(BluetoothDevice device, AttributionSource source) {
            BCService service = getService();
            if (service == null|| !Utils.checkConnectPermissionForDataDelivery(
                                   service, source, TAG)) {
                return BluetoothProfile.CONNECTION_POLICY_UNKNOWN;
            }
            return service.getConnectionPolicy(device);
        }
        @Override
        public boolean searchforLeAudioBroadcasters (BluetoothDevice device, AttributionSource source) {
            BCService service = getService();
            if (service == null||
                  !Utils.checkScanPermissionForDataDelivery(service, source, TAG) ||
                  !Utils.checkCallerHasCoarseOrFineLocation(
                         service, source, UserHandle.of(UserHandle.getCallingUserId()))) {
                return false;
            }
            return service.searchforLeAudioBroadcasters(device);
        }

        @Override
        public boolean stopSearchforLeAudioBroadcasters (BluetoothDevice device, AttributionSource source) {
            BCService service = getService();
            if (service == null||
                  !Utils.checkScanPermissionForDataDelivery(service, source, TAG) ||
                  !Utils.checkCallerHasCoarseOrFineLocation(
                         service, source, UserHandle.of(UserHandle.getCallingUserId()))) {
                return false;
            }
            return service.stopSearchforLeAudioBroadcasters(device);
        }

        @Override
        public boolean selectBroadcastSource (BluetoothDevice device, ScanResult scanRes, boolean isGroupOp, AttributionSource source) {
            BCService service = getService();
            if (service == null|| !Utils.checkConnectPermissionForDataDelivery(
                                   service, source, TAG)) {
                return false;
            }
            return service.selectBroadcastSource(device, scanRes, isGroupOp, false);
        }

        @Override
        public void registerAppCallback(BluetoothDevice device, IBleBroadcastAudioScanAssistCallback cb, AttributionSource source) {
            BCService service = getService();
            if (service == null|| !Utils.checkConnectPermissionForDataDelivery(
                                   service, source, TAG)) {
                return;
            }
            service.registerAppCallback(device, cb);
        }

        @Override
        public void unregisterAppCallback(BluetoothDevice device, IBleBroadcastAudioScanAssistCallback cb, AttributionSource source) {
            BCService service = getService();
            if (service == null|| !Utils.checkConnectPermissionForDataDelivery(
                                   service, source, TAG)) {
                return;
            }
            service.unregisterAppCallback(device, cb);
        }

        @Override
        public boolean startScanOffload(BluetoothDevice device, boolean isGroupOp, AttributionSource source) {
            BCService service = getService();
            if (service == null|| !Utils.checkConnectPermissionForDataDelivery(
                                   service, source, TAG)) {
                return false;
            }
            return service.startScanOffloadInternal(device, isGroupOp);
        }

        @Override
        public boolean stopScanOffload(BluetoothDevice device, boolean isGroupOp, AttributionSource source) {
            BCService service = getService();
            if (service == null|| !Utils.checkConnectPermissionForDataDelivery(
                                   service, source, TAG)) {
                return false;
            }
            return service.stopScanOffloadInternal(device, isGroupOp);
        }

        @Override
        public boolean addBroadcastSource(BluetoothDevice device, BleBroadcastSourceInfo srcInfo
                                      , boolean isGroupOp, AttributionSource source) {
            BCService service = getService();
            if (service == null|| !Utils.checkConnectPermissionForDataDelivery(
                                   service, source, TAG)) {
                return false;
            }
            return service.addBroadcastSourceInternal(device, srcInfo, isGroupOp);
        }

        @Override
        public boolean updateBroadcastSource (BluetoothDevice device,
                                    BleBroadcastSourceInfo srcInfo,
                                    boolean isGroupOp, AttributionSource source) {
            BCService service = getService();
            if (service == null|| !Utils.checkConnectPermissionForDataDelivery(
                                   service, source, TAG)) {
                return false;
            }
            return service.updateBroadcastSourceInternal(device, srcInfo, isGroupOp);
        }

        @Override
        public boolean setBroadcastCode (BluetoothDevice device,
                                    BleBroadcastSourceInfo srcInfo,
                                    boolean isGroupOp, AttributionSource source) {
            BCService service = getService();
            if (service == null|| !Utils.checkConnectPermissionForDataDelivery(
                                   service, source, TAG)) {
                return false;
            }
            return service.setBroadcastCodeInternal(device, srcInfo, isGroupOp);
        }

        @Override
        public boolean removeBroadcastSource (BluetoothDevice device,
                                    byte sourceId,
                                    boolean isGroupOp, AttributionSource source) {
            BCService service = getService();
            if (service == null|| !Utils.checkConnectPermissionForDataDelivery(
                                   service, source, TAG)) {
                return false;
            }
            return service.removeBroadcastSourceInternal(device, sourceId, isGroupOp);
        }
        @Override
        public List<BleBroadcastSourceInfo> getAllBroadcastSourceInformation (BluetoothDevice device,
                                    AttributionSource source) {
            BCService service = getService();
            if (service == null|| !Utils.checkConnectPermissionForDataDelivery(
                                   service, source, TAG)) {
                return null;
            }
            return service.getAllBroadcastSourceInformation(device);
       }
   }
}
