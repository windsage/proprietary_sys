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

import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Retention;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.annotation.IntDef;
import java.util.Map;
import java.lang.String;
import java.lang.Integer;
import java.util.List;



/**
 * Bluetooth LE Broadcast Scan Assistance related callbacks, used to deliver result of
 * Broadcast Assist operations performed using {@link BleBroadcastAudioScanAssistManager}
 *
 * @hide
 * @see BleBroadcastAudioScanAssistManager
 */
public abstract class BleBroadcastAudioScanAssistCallback {

    /** @hide */
    @IntDef(prefix = "BASS_STATUS_", value = {
              BASS_STATUS_SUCCESS,
              BASS_STATUS_FAILURE,
              BASS_STATUS_FATAL,
              BASS_STATUS_TXN_TIMEOUT,
              BASS_STATUS_INVALID_SOURCE_ID,
              BASS_STATUS_COLOCATED_SRC_UNAVAILABLE,
              BASS_STATUS_INVALID_SOURCE_SELECTED,
              BASS_STATUS_SOURCE_UNAVAILABLE,
              BASS_STATUS_DUPLICATE_ADDITION,
    })

    @Retention(RetentionPolicy.SOURCE)
    public @interface Bass_Status {}

    public static final int BASS_STATUS_SUCCESS = 0x00;
    public static final int BASS_STATUS_FAILURE = 0x01;
    public static final int BASS_STATUS_FATAL = 0x02;
    public static final int BASS_STATUS_TXN_TIMEOUT = 0x03;

    public static final int BASS_STATUS_INVALID_SOURCE_ID = 0x04;
    public static final int BASS_STATUS_COLOCATED_SRC_UNAVAILABLE = 0x05;
    public static final int BASS_STATUS_INVALID_SOURCE_SELECTED = 0x06;
    public static final int BASS_STATUS_SOURCE_UNAVAILABLE = 0x07;
    public static final int BASS_STATUS_DUPLICATE_ADDITION = 0x08;
    public static final int BASS_STATUS_NO_EMPTY_SLOT = 0x09;
    public static final int BASS_STATUS_INVALID_GROUP_OP = 0x10;

    /**
     * Callback when BLE broadcast audio source found.
     * result of {@link BleBroadcastAudioScanAssistManager#searchforLeAudioBroadcasters} will be
     * delivered through this callback
     *
     * @param scanres {@link ScanResult} object of the scanned result
     */
    public void onBleBroadcastSourceFound(ScanResult scanres) {
    };


    /**
     * Callback when BLE broadcast audio source found.
     * result of {@link BleBroadcastAudioScanAssistManager#searchforLeAudioBroadcasters} will be
     * delivered through this callback
     *
     * @param status Status of the Broadcast source selection.
     * @param broadcastSourceChannels {@link BleBroadcastSourceChannel} List
     * containing avaiable broadcast source channels that are being broadcasted from the selected
     * broadcast source
     *
     */
    public void onBleBroadcastSourceSelected(BluetoothDevice device,
                                                         @Bass_Status int status,
                                List<BleBroadcastSourceChannel> broadcastSourceChannels) {
    };

    /**
     * Callback when BLE broadcast audio source is been successfully added to the remote Scan delegator.
     * result of {@link BleBroadcastAudioScanAssistManager#addBroadcastSource} will be
     * delivered through this callback
     *
     * This callback is an acknowledgement confirming the source information added
     * to the Scan delegator. Actual updated source Information values of resulting Broadcast Source Information
     * will be notified using {@link BleBroadcastAudioScanAssistManager#ACTION_BROADCAST_RECEIVER_STATE} intent
     *
     * @param device remote scan delegator for which Source is been added.
     * @param srcId source Id of the Broadcast source information added
     * @param status true on succesful addition of source Information, false otherwise.
     *
     */
   public void onBleBroadcastAudioSourceAdded(BluetoothDevice device,
                                             byte srcId,
                                             @Bass_Status int status) {
    };

    /**
     * Callback when BLE broadcast audio source Information is been updated to the remote Scan delegator.
     * result of {@link BleBroadcastAudioScanAssistManager#updateBroadcastSource} will be
     * delivered through this callback
     *
     * This callback is an acknowledgement confirming the source information update request is succesfully
     * written on the Scan delegator. Actual updated source Information values of resulting Broadcast Source Information
     * will be notified using {@link BleBroadcastAudioScanAssistManager#ACTION_BROADCAST_RECEIVER_STATE} intent
     *
     * @param device remote scan delegator for which Source is been updated.
     * @param srcId source Id of the Broadcast source information updated.
     * @param status true on succesful updating of source Information, false otherwise.
     *
     */
    public void onBleBroadcastAudioSourceUpdated(BluetoothDevice device,
                                             byte srcId,
                                             @Bass_Status int status) {
    };

    /**
     * Callback when BLE broadcast audio source Information is updated with broadcast PIN code to the remote Scan delegator.
     * result of {@link BleBroadcastAudioScanAssistManager#setBroadcastCode} will be
     * delivered through this callback
     *
     * This callback is an acknowledgement confirming the Broadcast PIN update request is succesfully
     * written to the Scan delegator. Actual updated source Information values of resulting Broadcast Source Information
     * will be notified using {@link BleBroadcastAudioScanAssistManager#ACTION_BROADCAST_RECEIVER_STATE} intent.
     * Encryption status from the {@link BleBroadcastAudioScanAssistManager#ACTION_BROADCAST_RECEIVER_STATE} will
     * confirm the succesfull Broadcast PIN code and resulting decryption of the Broadcast data at the reciver side.
     *
     * @param device remote scan delegator for which Source is been updated.
     * @param srcId source Id of the Broadcast PIN updated.
     * @param status true on succesful updating of source Information, false otherwise.
     *
     */
    public void onBleBroadcastPinUpdated(BluetoothDevice rcvr,
                                                byte srcId,
                                                @Bass_Status int status) {
    };

    /**
     * Callback when BLE broadcast audio source Information is removed from the remote Scan delegator.
     * result of {@link BleBroadcastAudioScanAssistManager#removeBroadcastSource} will be
     * delivered through this callback
     *
     * This callback is an acknowledgement confirming the Broadcast source infor removal request is succesfully
     * written to the Scan delegator. Actual removal of source Information values of resulting Broadcast Source Information
     * will be notified using {@link BleBroadcastAudioScanAssistManager#ACTION_BROADCAST_RECEIVER_STATE} intent.
     * Deletion of source Information will result is setting all the source information attributes to ZERO other than
     * source Id
     *
     * @param device remote scan delegator for which Source is removed.
     * @param srcId source Id of the Broadcast source information removed.
     * @param status true on succesful updating of source Information, false otherwise.
     *
     */
    public void onBleBroadcastAudioSourceRemoved(BluetoothDevice rcvr,
                                             byte srcId,
                                             @Bass_Status int status) {
    };
}
