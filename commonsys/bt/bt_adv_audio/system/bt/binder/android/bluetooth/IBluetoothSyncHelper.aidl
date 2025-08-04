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
package android.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BleBroadcastSourceInfo;
import android.bluetooth.IBleBroadcastAudioScanAssistCallback;
import android.bluetooth.le.ScanResult;
import android.content.AttributionSource;

/**
 * APIs for Bluetooth Bluetooth Scan offloader service
 *
 * @hide
 */
interface IBluetoothSyncHelper {
    // Public API
    @RequiresNoPermission
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    boolean connect(in BluetoothDevice device,in AttributionSource attributionSource);

    @RequiresNoPermission
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    boolean disconnect(in BluetoothDevice device,in AttributionSource attributionSource);

    @RequiresNoPermission
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    List<BluetoothDevice> getConnectedDevices(in AttributionSource attributionSource);

    @RequiresNoPermission
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    List<BluetoothDevice> getDevicesMatchingConnectionStates(in int[] states, in AttributionSource attributionSource);

    @RequiresNoPermission
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    int getConnectionState(in BluetoothDevice device, in AttributionSource attributionSource);

    @RequiresNoPermission
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    boolean setConnectionPolicy(in BluetoothDevice device, int connectionPolicy, in AttributionSource attributionSource);

    @RequiresNoPermission
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    int getConnectionPolicy(in BluetoothDevice device, in AttributionSource attributionSource);

    @RequiresNoPermission
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    boolean startScanOffload (in BluetoothDevice device,
                              in boolean groupOp,
                              in AttributionSource attributionSource);

    @RequiresNoPermission
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    boolean stopScanOffload (in BluetoothDevice device,
                             in boolean groupOp,
                             in AttributionSource attributionSource);

    @RequiresNoPermission
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    void registerAppCallback(in BluetoothDevice device,
                             in IBleBroadcastAudioScanAssistCallback cb, in AttributionSource attributionSource);

    @RequiresNoPermission
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    void unregisterAppCallback(in BluetoothDevice device,
                               in IBleBroadcastAudioScanAssistCallback cb, in AttributionSource attributionSource);

    @RequiresNoPermission
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_SCAN)")
    boolean searchforLeAudioBroadcasters (in BluetoothDevice device, in AttributionSource attributionSource);

    @RequiresNoPermission
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_SCAN)")
    boolean stopSearchforLeAudioBroadcasters(in BluetoothDevice device, in AttributionSource attributionSource);

    @RequiresNoPermission
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    boolean addBroadcastSource(in BluetoothDevice device,
                               in BleBroadcastSourceInfo srcInfo,
                               in boolean groupOp,
                               in AttributionSource attributionSource
                            );
    @RequiresNoPermission
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    boolean selectBroadcastSource(in BluetoothDevice device,
                                  in ScanResult scanRes,
                                  in boolean groupOp,
                                  in AttributionSource attributionSource
                                  );

    @RequiresNoPermission
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    boolean updateBroadcastSource(in BluetoothDevice device,
                                  in BleBroadcastSourceInfo srcInfo,
                                  in boolean groupOp,
                                  in AttributionSource attributionSource
                                  );

    @RequiresNoPermission
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    boolean setBroadcastCode (in BluetoothDevice device,
                              in BleBroadcastSourceInfo srcInfo,
                              in boolean groupOp,
                              in AttributionSource attributionSource
                              );

    @RequiresNoPermission
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    boolean removeBroadcastSource (in BluetoothDevice device,
                                   in byte SourceId,
                                   in boolean groupOp,
                                   in AttributionSource attributionSource
                                   );

    @RequiresNoPermission
    @JavaPassthrough(annotation="@android.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)")
    List<BleBroadcastSourceInfo> getAllBroadcastSourceInformation(
                                             in BluetoothDevice device,
                                             in AttributionSource attributionSource);
}
