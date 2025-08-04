/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
*/
/*
 * Copyright (c) 2020, The Linux Foundation. All rights reserved.

 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.

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

package com.android.bluetooth.broadcast;

import android.bluetooth.BluetoothCodecStatus;
import android.bluetooth.BluetoothBroadcast;
/**
 * Stack event sent via a callback from JNI to Java, or generated.
 */
public class BroadcastStackEvent {
    // Event types for STACK_EVENT message (coming from native)
    private static final int EVENT_TYPE_NONE = 0;
    public static final int EVENT_TYPE_BROADCAST_STATE_CHANGED = 1;
    public static final int EVENT_TYPE_BROADCAST_AUDIO_STATE_CHANGED = 2;
    public static final int EVENT_TYPE_ENC_KEY_GENERATED = 3;
    public static final int EVENT_TYPE_CODEC_CONFIG_CHANGED = 4;
    public static final int EVENT_TYPE_SETUP_BIG = 5;
    public static final int EVENT_TYPE_BROADCAST_ID_GENERATED = 6;

    public static final int STATE_IDLE = 0;
    public static final int STATE_CONFIGURED = 1;
    public static final int STATE_STREAMING = 2;

    public static final int STATE_STOPPED = 0;
    public static final int STATE_STARTED = 1;

    public int type = EVENT_TYPE_NONE;
    public int advHandle = 0;
    public int valueInt = 0;
    public int bigHandle = 0;
    public int NumBises = 0;
    public int[] BisHandles;
    public byte[] BroadcastId = new byte[3];
    public String key;
    public BluetoothCodecStatus codecStatus;

    BroadcastStackEvent(int type) {
        this.type = type;
    }

    @Override
    public String toString() {
        // event dump
        StringBuilder result = new StringBuilder();
        result.append("BroadcastStackEvent {type:" + eventTypeToString(type));
        result.append(", value1:" + eventTypeValueIntToString(type, valueInt));
        if (codecStatus != null) {
            result.append(", codecStatus:" + codecStatus);
        }
        result.append("}");
        return result.toString();
    }

    private static String eventTypeToString(int type) {
        switch (type) {
            case EVENT_TYPE_NONE:
                return "EVENT_TYPE_NONE";
            case EVENT_TYPE_BROADCAST_STATE_CHANGED:
                return "EVENT_TYPE_BROADCAST_STATE_CHANGED";
            case EVENT_TYPE_BROADCAST_AUDIO_STATE_CHANGED:
                return "EVENT_TYPE_BROADCAST_AUDIO_STATE_CHANGED";
            case EVENT_TYPE_ENC_KEY_GENERATED:
                return "EVENT_TYPE_ENC_KEY_GENERATED";
            case EVENT_TYPE_CODEC_CONFIG_CHANGED:
                return "EVENT_TYPE_CODEC_CONFIG_CHANGED";
            case EVENT_TYPE_SETUP_BIG:
                return "EVENT_TYPE_SETUP_BIG";
            default:
                return "EVENT_TYPE_UNKNOWN:" + type;
        }
    }

    private static String eventTypeValueIntToString(int type, int value) {
        switch (type) {
            case EVENT_TYPE_BROADCAST_STATE_CHANGED:
                switch (value) {
                    case BluetoothBroadcast.STATE_DISABLED:
                        return "DISABLED";
                    case BluetoothBroadcast.STATE_ENABLING:
                        return "ENABLING";
                    case BluetoothBroadcast.STATE_ENABLED:
                        return "CONFIGURED";
                    case BluetoothBroadcast.STATE_STREAMING:
                        return "STREAMING";
                    default:
                        break;
                }
                break;
            case EVENT_TYPE_BROADCAST_AUDIO_STATE_CHANGED:
                switch(value) {
                  case BluetoothBroadcast.STATE_PLAYING:
                      return "PLAYING";
                  case BluetoothBroadcast.STATE_NOT_PLAYING:
                      return "NOT PLAYING";
                  default:
                      break;
                }
            default:
                break;
        }
        return Integer.toString(value);
    }
}

