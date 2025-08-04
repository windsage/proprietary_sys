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
       * Redistributions of source code must retain the above copyright
         notice, this list of conditions and the following disclaimer.
       * Redistributions in binary form must reproduce the above
         copyright notice, this list of conditions and the following
         disclaimer in the documentation and/or other materials provided
         with the distribution.
       * Neither the name of The Linux Foundation nor the names of its
         contributors may be used to endorse or promote products derived
         from this software without specific prior written permission.
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
 **************************************************************************/

package com.android.bluetooth.apm;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;
import java.util.List;

import com.android.bluetooth.Utils;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;

/**
 * A2DP Native Interface to/from JNI.
 */
public class ApmNativeInterface {
    private static final String TAG = "ApmNativeInterface";
    private static final boolean DBG = true;

    @GuardedBy("INSTANCE_LOCK")
    private static ApmNativeInterface sInstance;
    private BluetoothAdapter mAdapter;
    private static final Object INSTANCE_LOCK = new Object();

    static {
        classInitNative();
    }

    @VisibleForTesting
    private ApmNativeInterface() {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mAdapter == null) {
            Log.w(TAG, "No Bluetooth Adapter Available");
        }
    }

    /**
     * Get singleton instance.
     */
    public static ApmNativeInterface getInstance() {
        synchronized (INSTANCE_LOCK) {
            if (sInstance == null) {
                sInstance = new ApmNativeInterface();
            }
            return sInstance;
        }
    }

    /**
     * Initializes the native interface.
     */
    public void init() {
        initNative();
    }

    /**
     * Cleanup the native interface.
     */
    public void cleanup() {
        cleanupNative();
    }

    /**
     * Report new active device to stack.
     *
     * @param device: new active device
     * @param profile: new active profile
     * @return true on success, otherwise false.
     */
    public boolean activeDeviceUpdate(BluetoothDevice device, int profile, int audioType) {
        return activeDeviceUpdateNative(getByteAddress(device), profile, audioType);
    }

    /**
     * Report Content Control ID to stack.
     *
     * @param id: Content Control ID
     * @param profile: content control profile
     * @return true on success, otherwise false.
     */
    public boolean setContentControl(int id, int profile) {
        return setContentControlNative(id, profile);
    }

    // Get QC Hal enabled information from stack
    public boolean isQcLeaEnabled() {
         return isQcLeaEnabledNative();
   }

    // Get AOSP Hal enabled information from stack
    public boolean isAospLeaEnabled() {
         return isAospLeaEnabledNative();
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

    //Current logic is implemented only for CALL_AUDIO
    //Proper Audio_type needs to be sent to device profile map
    //for other audio features
    private int getActiveProfile(byte[] address, int audio_type) {
        DeviceProfileMap dpm = DeviceProfileMap.getDeviceProfileMapInstance();
        BluetoothDevice device = getDevice(address);
        int profile = dpm.getProfile(device, ApmConst.AudioFeatures.CALL_AUDIO);
        StreamAudioService streamAudioService = StreamAudioService.getStreamAudioService();
        BluetoothDevice groupDevice = streamAudioService.getDeviceGroup(device);
        if(!device.equals(groupDevice)) {
            profile = dpm.getProfile(groupDevice, ApmConst.AudioFeatures.CALL_AUDIO);
        }
        return profile;
    }

    private void updateMetadataA2dp(int context_type) {
        Log.w(TAG, "Callback registered in JAVA layer");
        Log.w(TAG, "Context Type: " + context_type);
        StreamAudioService streamAudioService = StreamAudioService.getStreamAudioService();
        if (streamAudioService != null) {
            streamAudioService.setMetadataContext(context_type);
        }

    }

    private void setLatencyMode(boolean isLowLatency) {
        Log.d(TAG, "Callback registered in JAVA layer");
        Log.d(TAG, " isLowLatency: " + isLowLatency);
        StreamAudioService streamAudioService = StreamAudioService.getStreamAudioService();
        if (streamAudioService != null) {
            streamAudioService.setLatencyMode(isLowLatency);
        }
    }

    // Native methods that call into the JNI interface
    private static native void classInitNative();
    private native void initNative();
    private native void cleanupNative();
    private native boolean activeDeviceUpdateNative(byte[] address, int profile, int audioType);
    private native boolean setContentControlNative(int id, int profile);
    private native boolean isQcLeaEnabledNative();
    private native boolean isAospLeaEnabledNative();
}

