/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
*/
/******************************************************************************
 *  Copyright (c) 2020-2021, The Linux Foundation. All rights reserved.
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


package com.android.bluetooth.mcp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import com.android.bluetooth.Utils;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;

/**
 * Mcp Native Interface to/from JNI.
 */
public class McpNativeInterface {
    private static final String TAG = "McpNativeInterface";
    private static final boolean DBG = true;
    private BluetoothAdapter mAdapter;
    @GuardedBy("INSTANCE_LOCK")
    private static McpNativeInterface sInstance;
    private static final Object INSTANCE_LOCK = new Object();

    static {
        classInitNative();
    }

    private McpNativeInterface() {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mAdapter == null) {
            Log.wtfStack(TAG, "No Bluetooth Adapter Available");
        }
    }

    /**
     * Get singleton instance.
     */
    public static McpNativeInterface getInstance() {
        synchronized (INSTANCE_LOCK) {
            if (sInstance == null) {
                sInstance = new McpNativeInterface();
            }
            return sInstance;
        }
    }

    /**
     * Initializes the native interface.
     *
     * priorities to configure.
     */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public void init() {
        initNative();
    }

    /**
     * Cleanup the native interface.
     */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public void cleanup() {
        cleanupNative();
    }


    /**
     * update MCP media supported feature
     * @param feature
     */
  @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public boolean mediaControlPointOpcodeSupported(int feature) {
        return mediaControlPointOpcodeSupportedNative(feature);
    }

    /**
     * update MCP media supported feature current value
     * @param value
     */
  @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public boolean mediaControlPoint(int value, int status) {
        Log.d(TAG, " mediaControlPoint: " + value + " " + status);
        return mediaControlPointNative(value, status);
    }

  /**
     * Sets the Mcp media state
     * @param state
     */
  @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public boolean mediaState(int state) {
        return mediaStateNative(state);
    }

  /**
     * update MCP media player name
     * @param player name
     */
  @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public boolean mediaPlayerName(String playeName) {
        return mediaPlayerNameNative(playeName);
    }
  /**
     * update track change notification
     * @param track id
     */
  @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public boolean trackChanged() {
        return trackChangedNative();
    }
  /**
     * update MCP track position
     * @param position
     */
  @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public boolean trackPosition(int position) {
        return trackPositionNative(position);
    }

  /**
     * update MCP track duration
     * @param duration
     */
  @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public boolean trackDuration(int duration) {
        return trackDurationNative(duration);
    }
  /**
     * update MCP track title
     * @param title
     */
  @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public boolean trackTitle(String title) {
        return trackTitleNative(title);
    }
  /**
     * update playing order support of media
     * @param order
     */
  @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public boolean playingOrderSupported(int order) {
        return playingOrderSupportedNative(order);
    }
  /**
     * update playing order value of media
     * @param value
     */
  @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public boolean playingOrder(int value) {
        return playingOrderNative(value);
    }
  /**
     * update active device
     * @param device
     * @param setId
     */
  @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public boolean setActiveDevice(BluetoothDevice device, int setId,
                                   int profile, int is_csip) {
        return setActiveDeviceNative(profile, setId, getByteAddress(device), is_csip);
    }
    /**
     * Sets Mcp media content control id
     * @param ccid
     */
  @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public boolean contentControlId(int ccid) {

        return contentControlIdNative(ccid);
    }
    /**
     * Disconnect Mcp disconnect device
     * @param device
     */
  @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public boolean disconnectMcp(BluetoothDevice device) {
        return disconnectMcpNative(getByteAddress(device));
    }

    /**
     * Disconnect Mcp disconnect device
     * @param device
     */
  @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public boolean bondStateChange(BluetoothDevice device, int state) {
        return bondStateChangeNative(state, getByteAddress(device));
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

    // Callbacks from the native stack back into the Java framework.
    // All callbacks are routed via the Service which will disambiguate which
    private void OnConnectionStateChanged(int state, byte[] address) {
        if (DBG) {
            Log.d(TAG, "OnConnectionStateChanged: " + state);
        }
        BluetoothDevice device = getDevice(address);

        McpService service = McpService.getMcpService();
        if (service != null)
            service.onConnectionStateChanged(device, state);
    }

    private void MediaControlPointChangedRequest(int state, byte[] address) {
        BluetoothDevice device = getDevice(address);
        if (DBG) {
            Log.d(TAG, "MediaControlPointChangedReq: " + state);
        }
        McpService service = McpService.getMcpService();
        if (service != null)
            service.onMediaControlPointChangeReq(device, state);
    }

    private void TrackPositionChangedRequest(int position) {
        if (DBG) {
           Log.d(TAG, "TrackPositionChangedRequest: " + position);
        }
        McpService service = McpService.getMcpService();
        if (service != null)
            service.onTrackPositionChangeReq(position);
    }

    private void PlayingOrderChangedRequest(int order) {
        if (DBG) {
           Log.d(TAG, "PlayingOrderChangedRequest: " + order);
        }
        McpService service = McpService.getMcpService();
        if (service != null)
            service.onPlayingOrderChangeReq(order);
    }

    // Callbacks from the native stack back into the Java framework
    private void McpServerInitializedCallback(int status) {
        if (DBG) {
           Log.d(TAG, "McpServerInitializedCallback status: " + status);
        }
        McpService service = McpService.getMcpService();
        if (service != null)
            service.onMcpServerInitialized(status);
    }

    // Native methods that call into the JNI interface
    private static native void classInitNative();
    private native void initNative();
    private native void cleanupNative();
    private native boolean mediaControlPointOpcodeSupportedNative(int feature);
    private native boolean mediaControlPointNative(int value, int status);
    private native boolean mediaStateNative(int state);
    private native boolean mediaPlayerNameNative(String playerName);
    private native boolean trackChangedNative();
    private native boolean trackPositionNative(int position);
    private native boolean trackDurationNative(int duration);
    private native boolean trackTitleNative(String title);
    private native boolean playingOrderSupportedNative(int order);
    private native boolean playingOrderNative(int value);
    private native boolean setActiveDeviceNative(int profile, int setId,
                                                 byte[] address, int is_csip);
    private native boolean contentControlIdNative(int ccid);
    private native boolean disconnectMcpNative(byte[] address);
    private native boolean bondStateChangeNative(int state, byte[] address);
}

