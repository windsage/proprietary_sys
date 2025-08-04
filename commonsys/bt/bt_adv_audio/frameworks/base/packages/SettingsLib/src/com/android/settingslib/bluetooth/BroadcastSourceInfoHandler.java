/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
*/
/*
 * Copyright (c) 2020-2021, The Linux Foundation. All rights reserved.
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

package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;
import android.bluetooth.BleBroadcastAudioScanAssistManager;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import android.content.Intent;
import android.bluetooth.BleBroadcastSourceInfo;
import android.os.Handler;

public class BroadcastSourceInfoHandler implements BluetoothEventManager.Handler {
        private static final String TAG = "BroadcastSourceInfoHandler";
        private static final boolean V = Log.isLoggable(TAG, Log.VERBOSE);
        private final CachedBluetoothDeviceManager mDeviceManager;
        BroadcastSourceInfoHandler(CachedBluetoothDeviceManager deviceManager
            ) {
            mDeviceManager = deviceManager;
        }
        @Override
        public void onReceive(Context context, Intent intent, BluetoothDevice device) {
            if (device == null) {
                Log.w(TAG, "BroadcastSourceInfoHandler: device is null");
                return;
            }

            final String action = intent.getAction();
            if (action == null) {
                Log.w(TAG, "BroadcastSourceInfoHandler: action is null");
                return;
            }
            BleBroadcastSourceInfo sourceInfo = intent.getParcelableExtra(
                              BleBroadcastSourceInfo.EXTRA_SOURCE_INFO);

            int sourceInfoIdx = intent.getIntExtra(
                              BleBroadcastSourceInfo.EXTRA_SOURCE_INFO_INDEX,
                              BluetoothAdapter.ERROR);

            int maxNumOfsrcInfo = intent.getIntExtra(
                              BleBroadcastSourceInfo.EXTRA_MAX_NUM_SOURCE_INFOS,
                              BluetoothAdapter.ERROR);
            if (V) {
                Log.d(TAG, "Rcved :BCAST_RECEIVER_STATE Intent for : " + device);
                Log.d(TAG, "Rcvd BroadcastSourceInfo index=" + sourceInfoIdx);
                Log.d(TAG, "Rcvd max num of source Info=" + maxNumOfsrcInfo);
                Log.d(TAG, "Rcvd BroadcastSourceInfo=" + sourceInfo);
            }
            CachedBluetoothDevice cachedDevice = mDeviceManager.findDevice(device);
            VendorCachedBluetoothDevice vDevice =
            VendorCachedBluetoothDevice.getVendorCachedBluetoothDevice(cachedDevice, null);
            if (vDevice != null) {
                vDevice.onBroadcastReceiverStateChanged(sourceInfo,
                                     sourceInfoIdx, maxNumOfsrcInfo);
                cachedDevice.dispatchAttributesChanged();
            } else {
                Log.e(TAG, "No vCachedDevice created for this Device");
            }
        }
};
