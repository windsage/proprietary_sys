/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
*/
/******************************************************************************
 *  Copyright (c) 2020, The Linux Foundation. All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are
 *  met:
 *      * Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *      * Redistributions in binary form must reproduce the above
 *        copyright notice, this list of conditions and the following
 *        disclaimer in the documentation and/or other materials provided
 *        with the distribution.
 *      * Neither the name of The Linux Foundation nor the names of its
 *        contributors may be used to endorse or promote products derived
 *        from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 *  ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 *  BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 *  BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 *  WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 *  OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 *  IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *****************************************************************************/

package com.android.bluetooth.acm;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothCodecConfig;
import android.bluetooth.BluetoothCodecStatus;
import android.bluetooth.BluetoothDevice;
import android.util.Log;
import java.util.List;
import java.util.Arrays;
import java.util.Objects;

import com.android.bluetooth.btservice.AdapterService;
import com.android.bluetooth.btservice.storage.DatabaseManager;
import com.android.bluetooth.Utils;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;

/**
 * ACM Native Interface to/from JNI.
 */
public class AcmNativeInterface {
    private static final String TAG = "AcmNativeInterface";
    private static final boolean DBG = true;
    private BluetoothAdapter mAdapter;
    static final int CONTEXT_TYPE_UNKNOWN = 0;
    static final int CONTEXT_TYPE_MUSIC = 1;
    static final int CONTEXT_TYPE_VOICE = 2;
    static final int CONTEXT_TYPE_MUSIC_VOICE = 3;
    @GuardedBy("INSTANCE_LOCK")
    private static AcmNativeInterface sInstance;
    private static final Object INSTANCE_LOCK = new Object();
    static final DatabaseManager mDatabaseManager = Objects.requireNonNull(AdapterService.getAdapterService().getDatabase(),
                 "DatabaseManager cannot be null when AcmNativeInterface starts");
    static {
        classInitNative();
    }

    @VisibleForTesting
    private AcmNativeInterface() {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mAdapter == null) {
            Log.wtf(TAG, "No Bluetooth Adapter Available");
        }
    }

    /**
     * Get singleton instance.
     */
    public static AcmNativeInterface getInstance() {
        synchronized (INSTANCE_LOCK) {
            if (sInstance == null) {
                sInstance = new AcmNativeInterface();
            }
            return sInstance;
        }
    }

    /**
     * Initializes the native interface.
     *
     * @param maxConnectedAudioDevices maximum number of A2DP Sink devices that can be connected
     * simultaneously
     * @param codecConfigPriorities an array with the codec configuration
     * priorities to configure.
     */
    public void init(int maxConnectedAudioDevices, BluetoothCodecConfig[] codecConfigPriorities) {
        initNative(maxConnectedAudioDevices, codecConfigPriorities);
    }


    /**
     * Initiates ACM connection to a remote device.
     *
     * @param device the remote device
     * @return true on success, otherwise false.
     */
    public boolean connectAcm(BluetoothDevice device, int contextType, int profileType, int preferredContext) {
        return connectAcmNative(getByteAddress(device), contextType, profileType, preferredContext);
    }

    /**
     * Disconnects ACM from a remote device.
     *
     * @param device the remote device
     * @return true on success, otherwise false.
     */
    public boolean disconnectAcm(BluetoothDevice device, int contextType) {
        return disconnectAcmNative(getByteAddress(device), contextType);
    }

    /**
     * Sets a connected ACM group/remote as active.
     *
     * @param device the remote device
     * @return true on success, otherwise false.
     */
    public boolean setActiveDevice(BluetoothDevice device, int contextType) {
        return setActiveDeviceNative(getByteAddress(device), contextType);
    }

    /**
     * Sends Start stream to remote group/remote for voice call.
     *
     * @param device the remote device
     * @return true on success, otherwise false.
     */
    public boolean startStream(BluetoothDevice device, int contextType) {
        return startStreamNative(getByteAddress(device), contextType);
    }

    /**
     * Sends Stop stream to remote group/remote for voice call.
     *
     * @param device the remote device
     * @return true on success, otherwise false.
     */
    public boolean stopStream(BluetoothDevice device, int contextType) {
        return stopStreamNative(getByteAddress(device), contextType);
    }

    /**
     * Sets the codec configuration preferences.
     *
     * @param device the remote Bluetooth device
     * @param codecConfigArray an array with the codec configurations to
     * configure.
     * @return true on success, otherwise false.
     */
    public boolean setCodecConfigPreference(BluetoothDevice device, BluetoothCodecConfig[] codecConfigArray,
                                                     int contextType, int preferredContext) {
        return setCodecConfigPreferenceNative(getByteAddress(device), codecConfigArray,
                                              contextType, preferredContext);
    }

    public boolean ChangeCodecConfigPreference(BluetoothDevice device,
                                               String message) {
        return ChangeCodecConfigPreferenceNative(getByteAddress(device), message);
    }

    /**
     * Sends hfp call and bap Media synchronization.
     *
     * @param device the remote device
     * @return true on success, otherwise false.
     */
    public void hfpCallBapMediaSync(boolean isBapMediaSuspend) {
        hfpCallBapMediaSyncNative(isBapMediaSuspend);
    }

    /**
     * Cleanup the native interface.
     */
    public void cleanup() {
        cleanupNative();
    }

    private BluetoothDevice getDevice(byte[] address) {
        return mAdapter.getRemoteDevice(address);
    }

    private byte[] getByteAddress(BluetoothDevice device) {
        if (device == null) {
            return Utils.getBytesFromAddress("00:00:00:00:00:00");
        }
        return Utils.getBytesFromAddress(device.getAddress());
    }

    private void sendMessageToService(AcmStackEvent event) {
        AcmService service = AcmService.getAcmService();
        if (service != null) {
            service.messageFromNative(event);
        } else {
            Log.w(TAG, "Event ignored, service not available: " + event);
        }
    }

    // Callbacks from the native stack back into the Java framework.
    // All callbacks are routed via the Service which will disambiguate which
    // state machine the message should be routed to.

    private void onConnectionStateChanged(byte[] address, int state, int contextType) {
        AcmStackEvent event =
                new AcmStackEvent(AcmStackEvent.EVENT_TYPE_CONNECTION_STATE_CHANGED);
        event.device = getDevice(address);
        event.valueInt1 = state;
        event.valueInt2 = contextType;

        if (DBG) {
            Log.d(TAG, "onConnectionStateChanged: " + event);
        }
        sendMessageToService(event);
    }

    private void onAudioStateChanged(byte[] address, int state, int contextType) {
        AcmStackEvent event = new AcmStackEvent(AcmStackEvent.EVENT_TYPE_AUDIO_STATE_CHANGED);

        event.device = getDevice(address);
        event.valueInt1 = state;
        event.valueInt2 = contextType;

        if (DBG) {
            Log.d(TAG, "onAudioStateChanged: " + event);
        }
        sendMessageToService(event);
    }

    private void onCodecConfigChanged(byte[] address,
            BluetoothCodecConfig newCodecConfig,
            BluetoothCodecConfig[] codecsLocalCapabilities,
            BluetoothCodecConfig[] codecsSelectableCapabilities, int contextType) {
        AcmStackEvent event = new AcmStackEvent(AcmStackEvent.EVENT_TYPE_CODEC_CONFIG_CHANGED);
        event.device = getDevice(address);
        event.codecStatus = new BluetoothCodecStatus(newCodecConfig,
                                                     Arrays.asList(codecsLocalCapabilities),
                                                     Arrays.asList(codecsSelectableCapabilities));
        event.valueInt2 = contextType;
        if (DBG) {
            Log.d(TAG, "onCodecConfigChanged: " + event);
        }
        sendMessageToService(event);
    }

    private void onMetaDataChanged(byte[] address, int contextType) {
        AcmStackEvent event = new AcmStackEvent(AcmStackEvent.EVENT_TYPE_META_DATA_CHANGED);
        event.device = getDevice(address);
        event.valueInt2 = contextType;

        if (DBG) {
            Log.d(TAG, "onMetaDataChanged: " + event);
        }
        sendMessageToService(event);
    }

    private boolean getCallStateInfo() {
        AcmService service = AcmService.getAcmService();
        if (service != null) {
            return service.isAcmCallActive();
        }
        return false;
    }

    private boolean getAAR4Status(byte[] address) {
        if (DBG) {
            Log.d(TAG, "getAAR4Status ");
        }
        return mDatabaseManager.getAAR4Status(getDevice(address));
    }
    // Native methods that call into the JNI interface
    private static native void classInitNative();
    private native void initNative(int maxConnectedAudioDevices,
                                   BluetoothCodecConfig[] codecConfigPriorities);
    private native boolean connectAcmNative(byte[] address, int contextType, int profileType, int preferredContext);
    private native boolean disconnectAcmNative(byte[] address, int contextType);
    private native boolean setActiveDeviceNative(byte[] address, int contextType);
    private native boolean startStreamNative(byte[] address, int contextType);
    private native boolean stopStreamNative(byte[] address, int contextType);
    private native boolean setCodecConfigPreferenceNative(byte[] address,
                BluetoothCodecConfig[] codecConfigArray, int contextType, int preferredContext);
    private native boolean ChangeCodecConfigPreferenceNative(byte[] address, String Id);
    private native void cleanupNative();
    private native void hfpCallBapMediaSyncNative(boolean isBapMediaSuspend);
}
