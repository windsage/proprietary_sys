/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
*/
/*
 * Copyright (c) 2020, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *    * Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above
 *      copyright notice, this list of conditions and the following
 *      disclaimer in the documentation and/or other materials provided
 *      with the distribution.
 *    * Neither the name of The Linux Foundation nor the names of its
 *      contributors may be used to endorse or promote products derived
 *      from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.android.bluetooth.bc;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import java.util.UUID;
import java.util.Collection;
import android.os.UserHandle;

import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import java.nio.charset.StandardCharsets;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Scanner;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.lang.String;
import java.lang.StringBuffer;
import java.lang.Integer;

import java.nio.ByteBuffer;
import java.lang.Byte;
import java.util.stream.IntStream;
import java.util.NoSuchElementException;

import android.bluetooth.BluetoothLeAudioCodecConfigMetadata;
import android.bluetooth.BluetoothLeAudioContentMetadata;
import android.bluetooth.BluetoothLeBroadcastChannel;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastSubgroup;
import android.bluetooth.BluetoothStatusCodes;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.BluetoothLeScanner;
import java.util.UUID;
import android.os.Handler;
import android.os.ParcelUuid;
import android.os.SystemProperties;
import android.os.RemoteException;

import android.bluetooth.BleBroadcastSourceInfo;
import android.bluetooth.BleBroadcastSourceChannel;
//import android.bluetooth.BluetoothBroadcast;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import com.android.bluetooth.btservice.ServiceFactory;
///*_BMS
import com.android.bluetooth.broadcast.BroadcastService;
//_BMS*/
import com.android.bluetooth.lebroadcast.BassClientService;
import android.bluetooth.BluetoothCodecConfig;
/*_PACS
import com.android.bluetooth.pacsclient.PacsClientService;
_PACS*/
import android.bluetooth.IBleBroadcastAudioScanAssistCallback;

/**
 * Bass Utility functions
 */

final class BassUtils {
        private static final String TAG = "BassUtils";
        /*LE Scan related members*/
        private boolean mBroadcastersAround = false;
        private BluetoothAdapter mBluetoothAdapter = null;
        private BluetoothLeScanner mLeScanner = null;
        private BCService mBCService = null;

        ///*_BMS
        private BroadcastService mBAService = null;
        //_BMS*/
        public static final String BAAS_UUID = "00001852-0000-1000-8000-00805F9B34FB";
        public static final String PUBLIC_BROADCAST_AUDIO_UUID = "00001856-0000-1000-8000-00805F9B34FB";
        private boolean mIsLocalBMSNotified = false;
        private ServiceFactory mFactory = new ServiceFactory();
        //Using ArrayList as KEY to hashmap. May be not risk
        //in this case as It is used to track the callback to cancel Scanning later
        private final Map<ArrayList<IBleBroadcastAudioScanAssistCallback>, ScanCallback> mLeAudioSourceScanCallbacks;
        private final Map<BluetoothDevice, ScanCallback> mBassAutoAssist;
        private ScanCallback mScanCallback;
        private final Map<Integer, ScanResult> mScanBroadcasts = new HashMap<>();

        private static final int AA_START_SCAN = 1;
        private static final int AA_SCAN_SUCCESS = 2;
        private static final int AA_SCAN_FAILURE = 3;
        private static final int AA_SCAN_TIMEOUT = 4;
        //timeout for internal scan
        private static final int AA_SCAN_TIMEOUT_MS = 1000;
        private boolean mIsPublicBroadcastSource = false;
        private static final int DATA_TYPE_PBP_NAME_AD_TYPE =0x30;
        private static final int PBP_PTS_ENABLED = 1;

        String mPbsName = null;

        /**
         * Stanadard Codec param types
         */
        static final  int LOCATION = 3;
        //sample rate
        static final int SAMPLE_RATE = 1;
        //frame duration
        static final int FRAME_DURATION = 2;
        //Octets per frame
        static final int OCTETS_PER_FRAME = 8;
        /*_PACS
        private PacsClientService mPacsClientService = PacsClientService.getPacsClientService();
        _PACS*/
        BassUtils (BCService service) {
            mBCService = service;
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            mLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
            mLeAudioSourceScanCallbacks = new HashMap<ArrayList<IBleBroadcastAudioScanAssistCallback>, ScanCallback>();
            mBassAutoAssist = new HashMap<BluetoothDevice, ScanCallback>();
            ///*_BMS
            mBAService = BroadcastService.getBroadcastService();
            //_BMS*/
        }

        private ScanCallback mPaSyncScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                log( "onScanResult:" + result);
            }
        };

        void cleanUp () {

              if (mLeAudioSourceScanCallbacks != null) {
                  mLeAudioSourceScanCallbacks.clear();
              }

              if (mBassAutoAssist != null) {
                  mBassAutoAssist.clear();
              }
        }

        boolean leScanControl(boolean on) {
            log("leScanControl:" + on);
            mLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
            if (mLeScanner == null) {
                Log.e(TAG, "LeScan handle not available");
                return false;
            }

            if (on) {
                mLeScanner.startScan(mPaSyncScanCallback);
            } else {
                mLeScanner.stopScan(mPaSyncScanCallback);
            }

            return true;
         }

         public boolean isPublicBroadcastSourceEnabled() {
            boolean status  = false;
            if (mBAService == null) {
                mBAService = BroadcastService.getBroadcastService();
            }

            if (mBAService != null) {
                status = mBAService.is_pbs_enabled();
                log("isPublicBroadcastSourceEnabled:" + status);
            }
            return status;
         }

        /* private helper to check if the Local BLE Broadcast happening Or not */
         public boolean isLocalLEAudioBroadcasting() {
             boolean ret = false;
             /*String localLeABroadcast = SystemProperties.get("persist.vendor.btstack.isLocalLeAB");
             if (!localLeABroadcast.isEmpty() && "true".equals(localLeABroadcast)) {
                 ret = true;
             }
             log("property isLocalLEAudioBroadcasting returning " + ret);*/
             ///*_Broadcast
             if (mBAService == null) {
                 mBAService = BroadcastService.getBroadcastService();
             }
             if (mBAService != null) {
                 ret = mBAService.isBroadcastActive();
                 //ret = mBAService.isBroadcastStreaming();
                log("local broadcast streaming:" + ret);
             } else {
                log("BroadcastService is Null");
             }
             //_Broadcast*/
             log("isLocalLEAudioBroadcasting returning " + ret);
             return ret;
         }

        Handler mAutoAssistScanHandler = new Handler() {
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case AA_START_SCAN:
                        BluetoothDevice dev = (BluetoothDevice) msg.obj;
                        Message m = obtainMessage(AA_SCAN_TIMEOUT);
                        m.obj = dev;
                        sendMessageDelayed(m, AA_SCAN_TIMEOUT_MS);
                        searchforLeAudioBroadcasters(dev, null, null);
                        break;
                    case AA_SCAN_SUCCESS:
                        //Able to find to desired desired Source Device
                        ScanResult scanRes = (ScanResult) msg.obj;
                        dev = scanRes.getDevice();
                        stopSearchforLeAudioBroadcasters(dev,null);
                        mBCService.selectBroadcastSource(dev, scanRes, false, true);
                        break;
                    case AA_SCAN_FAILURE:
                        //Not able to find the given source
                        //ignore
                        break;
                    case AA_SCAN_TIMEOUT:
                        dev = (BluetoothDevice)msg.obj;
                        stopSearchforLeAudioBroadcasters(dev, null);
                        break;
                }
            }
        };

        public ScanResult getScanBroadcast(int broadcastId) {
            log("getScanBroadcast: broadcastId = " + broadcastId);
            synchronized (mScanBroadcasts) {
                if (mScanBroadcasts != null) {
                    return mScanBroadcasts.get(broadcastId);
                }
                return null;
            }
        }

        private boolean ptsForPbpEnabled() {
            return (SystemProperties.getInt("persist.vendor.btstack.pbp_pts_enable", 0) == PBP_PTS_ENABLED);
        }

        public BluetoothLeBroadcastMetadata getBroadcastMetadata(BluetoothDevice device,
                int broadcastId, String program, BaseData base) {
            final long AUDIO_LOCATION_FRONT_LEFT = 0x01;
            String program_info = "program info";
            final String LANGUAGE = "eng";
            final long CODEC_ID = 0x0000000006;

            if (program != null) {
                program_info = program;

                if (mIsPublicBroadcastSource) {
                    if (ptsForPbpEnabled() && (mPbsName != null)) {
                        program_info = mPbsName;
                    }
                }
            }
            log("broadcastId: " + broadcastId + " program: " + program_info);

            BluetoothLeBroadcastMetadata.Builder metaData =
                    new BluetoothLeBroadcastMetadata.Builder();
            if (base == null) {
                BluetoothLeAudioContentMetadata audioMetaData =
                        new BluetoothLeAudioContentMetadata.Builder()
                                .setProgramInfo(program_info)
                                .setLanguage(LANGUAGE)
                                .build();
                BluetoothLeAudioCodecConfigMetadata audioCodecConfig =
                        new BluetoothLeAudioCodecConfigMetadata.Builder()
                                .setAudioLocation(AUDIO_LOCATION_FRONT_LEFT)
                                .build();
                BluetoothLeBroadcastChannel channel =
                        new BluetoothLeBroadcastChannel.Builder()
                                .setChannelIndex(0)
                                .setSelected(false)
                                .setCodecMetadata(audioCodecConfig)
                                .build();
                BluetoothLeBroadcastSubgroup subGroup =
                        new BluetoothLeBroadcastSubgroup.Builder()
                                .setCodecId(CODEC_ID)
                                .setCodecSpecificConfig(audioCodecConfig)
                                .setContentMetadata(audioMetaData)
                                .addChannel(channel)
                                .build();
                metaData.setSourceDevice(device, device.getAddressType())
                        .setPresentationDelayMicros(0)
                        .addSubgroup(subGroup)
                        .setBroadcastId(broadcastId);
            } else {
                log("Get metadata from Base");
                int numSubGroups = (int) base.getNumberOfSubgroupsofBIG();
                for (int i = 0; i < numSubGroups; i++) {
                    ArrayList<BaseData.BaseInformation> bisInfos =
                            base.getBISIndexInfosGroupNo(i);
                    BluetoothLeBroadcastSubgroup.Builder subGroupBuilder =
                            new BluetoothLeBroadcastSubgroup.Builder();
                    log("subgroup: " + i + " - codec info:");
                    printByteArray(base.getCodecInfo(i));
                    BluetoothLeAudioCodecConfigMetadata subGroupCodec;
                    if (base.getCodecInfo(i) != null) {
                        subGroupCodec =
                                BluetoothLeAudioCodecConfigMetadata.fromRawBytes(
                                        base.getCodecInfo(i));
                    } else {
                        subGroupCodec =
                                new BluetoothLeAudioCodecConfigMetadata.Builder()
                                .setAudioLocation(AUDIO_LOCATION_FRONT_LEFT)
                                .build();
                    }
                    log("subgroup: " + i + " - metadata:");
                    printByteArray(base.getMetadata(i));
                    BluetoothLeAudioContentMetadata subGroupmMetadata;
                    if (base.getMetadata(i) != null) {
                        subGroupmMetadata =
                                BluetoothLeAudioContentMetadata.fromRawBytes(
                                        base.getMetadata(i));
                    } else {
                        subGroupmMetadata =
                                new BluetoothLeAudioContentMetadata.Builder()
                                .setProgramInfo(program_info)
                                .setLanguage(LANGUAGE)
                                .build();
                    }
                    subGroupmMetadata = new BluetoothLeAudioContentMetadata.Builder(
                            subGroupmMetadata).setProgramInfo(program_info).build();
                    subGroupBuilder.setCodecId(CODEC_ID)
                                   .setCodecSpecificConfig(subGroupCodec)
                                   .setContentMetadata(subGroupmMetadata);
                    for (BaseData.BaseInformation b: bisInfos) {
                        BluetoothLeBroadcastChannel.Builder channelBuilder =
                                new BluetoothLeBroadcastChannel.Builder();
                        log("subGroup/bisIndex: " + i + "/" + b.index + " - codec info:");
                        printByteArray(base.getCodecInfo(i, (int) b.index));
                        BluetoothLeAudioCodecConfigMetadata bisCodec;
                        if (base.getCodecInfo(i, (int) b.index) != null) {
                            bisCodec = BluetoothLeAudioCodecConfigMetadata.fromRawBytes(
                                    base.getCodecInfo(i, (int) b.index));
                        } else {
                            bisCodec = new BluetoothLeAudioCodecConfigMetadata.Builder()
                                    .setAudioLocation(AUDIO_LOCATION_FRONT_LEFT)
                                    .build();
                        }
                        channelBuilder.setChannelIndex(b.index)
                                      .setCodecMetadata(bisCodec)
                                      .setSelected(false);
                        subGroupBuilder.addChannel(channelBuilder.build());
                    }
                    metaData.addSubgroup(subGroupBuilder.build());
                }
                byte[] arrayPresentationDelay = base.getLevelOne().presentationDelay;
                int presentationDelay = (int) ((arrayPresentationDelay[2] & 0xff) << 16
                        | (arrayPresentationDelay[1] & 0xff)
                        | (arrayPresentationDelay[0] & 0xff));
                log("presentationDelay: " + presentationDelay);
                metaData.setSourceDevice(device, device.getAddressType())
                        .setPresentationDelayMicros(presentationDelay)
                        .setBroadcastId(broadcastId);
            }

            String localAddress = null;
            if (mBCService.isPublicAddrForSrc()) {
                localAddress = BluetoothAdapter.getDefaultAdapter().getAddress();
            } else {
                if (mBAService == null) {
                    mBAService = BroadcastService.getBroadcastService();
                }
                if (mBAService != null && mBAService.isBroadcastActive()) {
                    localAddress = mBAService.BroadcastGetAdvAddress();
                }
            }
            if (localAddress != null && localAddress.equals(device.getAddress())) {
                log("collocated source case");
                if (isPublicBroadcastSourceEnabled()) {
                    log("Public broadcast source");
                    byte[] serviceData = mBAService.getPublicBroadcastServiceData();
                    if (serviceData != null && serviceData.length > 1) {
                        log("Length of servicedata: " + serviceData.length);
                        byte feature = serviceData[0];
                        byte length = serviceData[1];
                        int audioQuality = BluetoothLeBroadcastMetadata.AUDIO_CONFIG_QUALITY_NONE;
                        if ((feature & 0x2) != 0) {
                            audioQuality |= BluetoothLeBroadcastMetadata.AUDIO_CONFIG_QUALITY_STANDARD;
                        }
                        if ((feature & 0x4) != 0) {
                            audioQuality |= BluetoothLeBroadcastMetadata.AUDIO_CONFIG_QUALITY_HIGH;
                        }
                        metaData.setAudioConfigQuality(audioQuality);
                        if (serviceData.length == ((int) length + 2)) {
                            if (length > 0) {
                                byte[] meta = new byte[(int) length];
                                System.arraycopy(serviceData, 2, meta, 0, length);
                                metaData.setPublicBroadcastMetadata(BluetoothLeAudioContentMetadata
                                        .fromRawBytes(meta));
                            }
                        }
                    }
                    metaData.setPublicBroadcast(true);
                    metaData.setBroadcastName(mBAService.getBroadcastName());
                    log("Broadcast name: " + mBAService.getBroadcastName());
                }
            } else {
                log("non-collocated source case");
                BCService.PAResults res = mBCService.getPAResults(device);
                if (res != null) {
                    metaData.setPublicBroadcast(res.isPublicBroadcast());
                    if (res.isPublicBroadcast()) {
                        log("Public broadcast source");
                        metaData.setAudioConfigQuality(res.getAudioQuality());
                        if (res.getMetaData() != null) {
                            metaData.setPublicBroadcastMetadata(BluetoothLeAudioContentMetadata
                                    .fromRawBytes(res.getMetaData()));
                        }
                        String broadcastName = res.getBroadcastName();
                        if (broadcastName != null) {
                            metaData.setBroadcastName(broadcastName);
                        }
                        log("Broadcast name: " + broadcastName);
                    }
                }
            }
            return metaData.build();
        }

        public void notifyLocalBroadcastSourceFound(ArrayList<IBleBroadcastAudioScanAssistCallback> cbs) {
            BluetoothDevice localDev = mBCService.getLocalBroadcastSource();
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
            if (cbs != null) {
                for (IBleBroadcastAudioScanAssistCallback cb : cbs) {
                    try {
                          cb.onBleBroadcastSourceFound(scanRes);
                    } catch (RemoteException e)  {
                          Log.e(TAG, "Exception while calling onBleBroadcastSourceFound");
                    }
                }
            }
            BassClientService bassClientService = mBCService.getBassClientService();
            if (bassClientService != null) {
                if (mBAService == null) {
                    mBAService = BroadcastService.getBroadcastService();
                }
                if(!mBCService.isAddedSource(localDev)) {
                    String programInfo = null;
                    int broadcastId = 0;
                    if (mBAService != null) {
                        byte[] broadcast_id = mBAService.getBroadcastId();
                        broadcastId = (0x00FF0000 & (broadcast_id[2] << 16));
                        broadcastId |= (0x0000FF00 & (broadcast_id[1] << 8));
                        broadcastId |= (0x000000FF & broadcast_id[0]);
                        programInfo = mBAService.getProgramInfo();
                    }
                    BaseData base = null;
                    if (mBAService != null) {
                        base = new BaseData(mBAService.getNumSubGroups(),
                                mBAService.BroadcastGetBisInfo(),
                                mBAService.BroadcastGetMetaInfo(),
                                mBAService.getPresentationDelay());
                        base.printConsolidated();
                        mBCService.updateBASE(mBAService.BroadcatGetAdvHandle(), base);
                    }
                    bassClientService.getCallbacks().
                            notifySourceFound(getBroadcastMetadata(localDev, broadcastId, programInfo, base));
                    mBCService.selectLocalSources();
                }
            }
        }

        private static byte[] extractPublicBroadcastNameBytesFromScanRecord(byte[] scanRecord, int start, int length) {
            byte[] bytes = new byte[length];
            System.arraycopy(scanRecord, start, bytes, 0, length);
            return bytes;
        }

        static String getPublicBroadcastNameFromScanRecordBytes(byte[] scanRecord) {
            if (scanRecord == null) {
                Log.e(TAG, " Scan record is NULL ");
                return null;
            }
            int currentPos = 0;
            int advertiseFlag = -1;
            String publicbroadcastsName = null;
            log( "getPublicBroadcastNameFromScanRecordBytes");
            try {
                while (currentPos < scanRecord.length) {
                    int length = scanRecord[currentPos++] & 0xFF;
                    if (length == 0) {
                        break;
                    }
                    int dataLength = length - 1;
                    int fieldType = scanRecord[currentPos++] & 0xFF;
                    switch (fieldType) {
                        case DATA_TYPE_PBP_NAME_AD_TYPE:
                            publicbroadcastsName = new String(extractPublicBroadcastNameBytesFromScanRecord(scanRecord, currentPos, dataLength));
                            log( "PBS Name " + publicbroadcastsName);
                        default:
                            break;
                    }
                        currentPos += dataLength;
                }
            } catch (Exception e) {
                    Log.e(TAG, "unable to parse scan record: " + Arrays.toString(scanRecord));
            }
            return publicbroadcastsName;
        }

        public boolean searchforLeAudioBroadcasters (BluetoothDevice srcDevice,
                                       ArrayList<IBleBroadcastAudioScanAssistCallback> cbs,
                                       List<ScanFilter> filters) {
           log( "searchforLeAudioBroadcasters: ");
           mIsPublicBroadcastSource = false;
           BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();
           mIsLocalBMSNotified = false;
           if (scanner == null) {
                Log.e(TAG, "startLeScan: cannot get BluetoothLeScanner");
                return false;
           }
           synchronized (mLeAudioSourceScanCallbacks) {
                if (mLeAudioSourceScanCallbacks.containsKey(cbs)) {
                    Log.e(TAG, "LE Scan has already started");
                    return false;
                }
                ScanCallback scanCallback = new ScanCallback() {
                   @Override
                    public void onScanResult(int callbackType, ScanResult result) {
                        log( "onScanResult:" + result);
                        if (callbackType != ScanSettings.CALLBACK_TYPE_ALL_MATCHES) {
                            // Should not happen.
                            Log.e(TAG, "LE Scan has already started");
                            return;
                        }
                        ScanRecord scanRecord = result.getScanRecord();
                        //int pInterval = result.getPeriodicAdvertisingInterval();
                        if (scanRecord != null) {
                            Map<ParcelUuid, byte[]> listOfUuids = scanRecord.getServiceData();
                            if (listOfUuids != null) {
                                //ParcelUuid bmsUuid = new ParcelUuid(BroadcastService.BAAS_UUID);
                                //boolean isBroadcastSource = listOfUuids.containsKey(bmsUuid);
                                boolean isBroadcastSource = listOfUuids.containsKey(ParcelUuid.fromString(BAAS_UUID));
                                if (isPublicBroadcastSourceEnabled()) {
                                    mIsPublicBroadcastSource = listOfUuids.containsKey(ParcelUuid.fromString(PUBLIC_BROADCAST_AUDIO_UUID));
                                    log( "mIsPublicBroadcastSource:" + mIsPublicBroadcastSource);
                                    if (mIsPublicBroadcastSource) {
                                        mPbsName = getPublicBroadcastNameFromScanRecordBytes(scanRecord.getBytes());
                                    }
                                }
                                log( "isBroadcastSource:" + isBroadcastSource);
                                if (isBroadcastSource) {
                                    log( "Broadcast Source Found:" + result.getDevice());
                                    if (cbs != null) {
                                        for (IBleBroadcastAudioScanAssistCallback cb : cbs) {
                                           try {
                                               cb.onBleBroadcastSourceFound(result);
                                           } catch (RemoteException e)  {
                                               Log.e(TAG, "Exception while calling onBleBroadcastSourceFound");
                                           }
                                        }
                                    } else {
                                        if (srcDevice != null && srcDevice.equals(result.getDevice())) {
                                            log("matching src Device found");
                                            Message msg = mAutoAssistScanHandler.obtainMessage(AA_SCAN_SUCCESS);
                                            msg.obj = result;
                                            mAutoAssistScanHandler.sendMessage(msg);
                                        }
                                    }

                                    if (srcDevice == null) {
                                        BassClientService bassClientService = mBCService.getBassClientService();
                                        if (bassClientService != null) {
                                            if (!mBCService.isAddedSource(result.getDevice())) {
                                                byte[] broadcastIdArray = listOfUuids.get(ParcelUuid.fromString(BAAS_UUID));
                                                int broadcastId = (0x00FF0000 & (broadcastIdArray[2] << 16));
                                                        broadcastId |= (0x0000FF00 & (broadcastIdArray[1] << 8));
                                                        broadcastId |= (0x000000FF & broadcastIdArray[0]);
                                                synchronized (mScanBroadcasts) {
                                                    if (mScanBroadcasts.get(broadcastId) == null) {
                                                        log("selectBroadcastSources for scanned sources");
                                                        mScanBroadcasts.put(broadcastId, result);
                                                        mBCService.selectBroadcastSources(result);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    log( "Broadcast Source UUID not preset, ignore");
                                }
                            } else {
                                Log.e(TAG, "Ignore no UUID");
                                return;
                            }
                        } else {
                            Log.e(TAG, "Scan record is null, ignoring this Scan res");
                            return;
                        }
                        //Before starting LE Scan, Call local APIs to find out if the local device
                        //is Broadcaster, then generate callback for Local device
                        if (!mIsLocalBMSNotified && isLocalLEAudioBroadcasting()) {
                        //Create a DUMMY scan result for colocated case
                            notifyLocalBroadcastSourceFound(cbs);
                            mIsLocalBMSNotified = true;
                        }
                       }

                     public void onScanFailed(int errorCode) {
                         Log.e(TAG, "Scan Failure:" + errorCode);
                     }
                };
         if (mBluetoothAdapter != null) {
             if (cbs != null) {
                 mLeAudioSourceScanCallbacks.put(cbs, scanCallback);
             } else if (srcDevice != null){
                 //internal auto assist trigger remember it
                 //based on device
                 mBassAutoAssist.put(srcDevice, scanCallback);
             } else {
                 mScanCallback = scanCallback;
             }

             synchronized (mScanBroadcasts) {
                 mScanBroadcasts.clear();
             }
             ScanSettings settings = new ScanSettings.Builder().setCallbackType(
                 ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                 .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                 .setLegacy(false)
                 .build();

                 if (filters == null) {
                     filters = new ArrayList<ScanFilter>();
                 }
                if (!BassUtils.containUuid(filters, ParcelUuid.fromString(BAAS_UUID))) {
                    byte[] serviceData = {0x00, 0x00 ,0x00}; // Broadcast_ID
                    byte[] serviceDataMask = {0x00, 0x00, 0x00};
                    filters.add(new ScanFilter.Builder()
                            .setServiceData(ParcelUuid.fromString(BAAS_UUID), serviceData, serviceDataMask).build());
                }

                 if (!mIsLocalBMSNotified && isLocalLEAudioBroadcasting()) {
                    if (isPublicBroadcastSourceEnabled()) {
                        mIsPublicBroadcastSource = true;
                    } else {
                        mIsPublicBroadcastSource = false;
                    }
                    notifyLocalBroadcastSourceFound(cbs);
                    mIsLocalBMSNotified = true;
                 }
                 scanner.startScan(filters, settings, scanCallback);
                 BassClientService bassClientService = mBCService.getBassClientService();
                 if (bassClientService != null) {
                     bassClientService.getCallbacks().
                             notifySearchStarted(BluetoothStatusCodes.REASON_LOCAL_APP_REQUEST);
                 }
                 return true;
             } else {
                 Log.e(TAG, "searchforLeAudioBroadcasters: Adapter is NULL");
                 return false;
             }
         }
    }
    public boolean stopSearchforLeAudioBroadcasters(BluetoothDevice srcDev,
                                                     ArrayList<IBleBroadcastAudioScanAssistCallback> cbs) {
        log( "stopSearchforLeAudioBroadcasters()");
        BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();
        if (scanner == null) {
            return false;
        }
        ScanCallback scanCallback = null;
        if (cbs != null) {
            scanCallback = mLeAudioSourceScanCallbacks.remove(cbs);
        } else {
            if (srcDev != null) {
                scanCallback = mBassAutoAssist.remove(srcDev);
            } else {
                scanCallback = mScanCallback;
                mScanCallback = null;
            }
        }

        if (scanCallback == null) {
            log( "scan not started yet");
            return false;
        }
        scanner.stopScan(scanCallback);
        BassClientService bassClientService = mBCService.getBassClientService();
        if (bassClientService != null) {
            bassClientService.getCallbacks().
                    notifySearchStopped(BluetoothStatusCodes.REASON_LOCAL_APP_REQUEST);
        }
        return true;
    }

    public boolean isSearchInProgress() {
        return !mLeAudioSourceScanCallbacks.isEmpty() ||
                !mBassAutoAssist.isEmpty() ||
                mScanCallback != null;
    }

    private static boolean containUuid(List<ScanFilter> filters, ParcelUuid uuid) {
        for (ScanFilter filter: filters) {
            if (filter.getServiceUuid().equals(uuid)) {
                return true;
            }
        }
        return false;
    }

    private int convertConfigurationSRToCapabilitySR(byte sampleRate) {
        int ret = BluetoothCodecConfig.SAMPLE_RATE_NONE;
        switch (sampleRate) {
            case 1:
                ret = BluetoothCodecConfig.SAMPLE_RATE_NONE; break;
            case 2:
                ret = BluetoothCodecConfig.SAMPLE_RATE_NONE; break;
            case 3:
                ret = BluetoothCodecConfig.SAMPLE_RATE_NONE; break;
            case 4:
                //ret = BluetoothCodecConfig.SAMPLE_RATE_32000; break;
            case 5:
                ret = BluetoothCodecConfig.SAMPLE_RATE_44100; break;
            case 6:
                ret = BluetoothCodecConfig.SAMPLE_RATE_48000; break;
            }
        log("convertConfigurationSRToCapabilitySR returns:" + ret);
        return ret;
    }

    private boolean isSampleRateSupported(BluetoothDevice device, byte sampleRate) {
        boolean ret = false;
        /*_PACS
        BluetoothCodecConfig[]  supportedConfigs = mPacsClientService.getSinkPacs(device);
        int actualSampleRate = convertConfigurationSRToCapabilitySR(sampleRate);

        if (actualSampleRate == BluetoothCodecConfig.SAMPLE_RATE_NONE) {
            return false;
        }

        for (int i=0; i<supportedConfigs.length; i++) {
            if (actualSampleRate == supportedConfigs[i].getSampleRate()) {
                ret = true;
            }
        }

        log("isSampleRateSupported returns:" + ret);
        _PACS*/
        return ret;
    }
    public List<BleBroadcastSourceChannel> selectBises(BluetoothDevice device,
                                                 BleBroadcastSourceInfo srcInfo, BaseData base)  {
        boolean noPref = SystemProperties.getBoolean("persist.vendor.service.bt.bass_no_pref", false);
        if (noPref) {
            log("No pref selected");
            return null;
        } else {
        /*_PACS
        mPacsClientService = PacsClientService.getPacsClientService();
        List<BleBroadcastSourceChannel> bChannels = new ArrayList<BleBroadcastSourceChannel>();
        //if (mPacsClientService == null) {
            log("selectBises: Pacs Service is null, pick BISes apropriately");
            //Pacs not available
            if (base != null) {
                bChannels = base.pickAllBroadcastChannels();
            } else {
                bChannels = null;
            }
            return bChannels;
        //}
        if (mPacsClientService != null) {
            int supportedLocations = 1/*mPacsClientService.getSinkLocations(device);
            ArrayList<BaseData.BaseInformation> broadcastedCodecInfo = base.getBISIndexInfos();
            if (broadcastedCodecInfo != null) {
                for (int i=0; i<broadcastedCodecInfo.size(); i++) {
                    HashMap<Integer, String> consolidatedUniqueCodecInfo = broadcastedCodecInfo.get(i).consolidatedUniqueCodecInfo;
                    byte index = broadcastedCodecInfo.get(i).index;
                    if (consolidatedUniqueCodecInfo != null) {


                        byte[] bisChannelLocation = consolidatedUniqueCodecInfo.get(LOCATION).getBytes();
                        byte[] locationValue = new byte[4];
                        System.arraycopy(bisChannelLocation, 2, locationValue, 0, 4);
                        log ("locationValue>>> ");
                        printByteArray(locationValue);
                        ByteBuffer wrapped = ByteBuffer.wrap(locationValue);
                        int bisLocation = wrapped.getInt();
                        log("bisLocation: " + bisLocation);
                        int reversebisLoc = Integer.reverseBytes(bisLocation);
                        log("reversebisLoc: " + reversebisLoc);

                        byte[] bisSampleRate = consolidatedUniqueCodecInfo.get(SAMPLE_RATE).getBytes();
                        byte bisSR = bisSampleRate[2];

                        //using bitwise operand as Location can be bitmask
                        if (isSampleRateSupported(device, bisSR) && (reversebisLoc & supportedLocations) == supportedLocations) {
                             log("matching location: bisLocation " + reversebisLoc + ":: " + supportedLocations);
                             BleBroadcastSourceChannel bc = new BleBroadcastSourceChannel(index, String.valueOf(index), true);
                             bChannels.add(bc);
                        }
                     }
                }
            }
        }

        if (bChannels != null && bChannels.size() == 0) {
            log("selectBises: no channel are selected");
            bChannels = null;

        }
        return bChannels;
        _PACS*/
      }
      return null;
    }

    public void triggerAutoAssist (BleBroadcastSourceInfo srcInfo) {
        //searchforLeAudioBroadcasters (srcInfo.getSourceDevice(), null, AUTO_ASSIST_SCAN_TIMEOUT);
        BluetoothDevice dev = srcInfo.getSourceDevice();

        Message msg = mAutoAssistScanHandler.obtainMessage(AA_START_SCAN);
        msg.obj = srcInfo.getSourceDevice();
        mAutoAssistScanHandler.sendMessage(msg);
    }

    static void log(String msg) {
        if (BassClientStateMachine.BASS_DBG) {
           Log.d(TAG, msg);
        }
    }

    static void printByteArray(byte[] array) {
        if (array == null) {
            log("array is null");
            return;
        }
        log("Entire byte Array as string: " + Arrays.toString(array));
        log("printitng byte by bte");
        for (int i=0; i<array.length; i++) {
             log( "array[" + i + "] :" + Byte.toUnsignedInt(array[i]));
        }
    }
}
