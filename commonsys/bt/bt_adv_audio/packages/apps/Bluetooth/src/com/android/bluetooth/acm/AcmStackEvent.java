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

import android.bluetooth.BluetoothCodecStatus;
import android.bluetooth.BluetoothDevice;

/**
 * Stack event sent via a callback from JNI to Java, or generated
 * internally by the ACM State Machine.
 */
public class AcmStackEvent {
    // Event types for STACK_EVENT message (coming from native)
    private static final int EVENT_TYPE_NONE = 0;
    public static final int EVENT_TYPE_CONNECTION_STATE_CHANGED = 1;
    public static final int EVENT_TYPE_AUDIO_STATE_CHANGED = 2;
    public static final int EVENT_TYPE_CODEC_CONFIG_CHANGED = 3;
    public static final int EVENT_TYPE_META_DATA_CHANGED = 4;

    // Do not modify without updating the HAL bt_acm.h files.
    // Match up with btacm_connection_state_t enum of bt_acm.h
    static final int CONNECTION_STATE_DISCONNECTED = 0;
    static final int CONNECTION_STATE_CONNECTING = 1;
    static final int CONNECTION_STATE_CONNECTED = 2;
    static final int CONNECTION_STATE_DISCONNECTING = 3;
    // Match up with btacm_audio_state_t enum of bt_acm.h
    static final int AUDIO_STATE_REMOTE_SUSPEND = 0;
    static final int AUDIO_STATE_STOPPED = 1;
    static final int AUDIO_STATE_STARTED = 2;

    // Match up with btacm_audio_state_t enum of bt_acm.h
    static final int CONTEXT_TYPE_UNKNOWN = 0;
    static final int CONTEXT_TYPE_MUSIC = 1;
    static final int CONTEXT_TYPE_VOICE = 2;
    static final int CONTEXT_TYPE_MUSIC_VOICE = 3;

     // Match up with btacm_audio_state_t enum of bt_acm.h
    static final int PROFILE_TYPE_NONE = 0;

    public int type = EVENT_TYPE_NONE;
    public BluetoothDevice device;
    public int valueInt1 = 0;
    public int valueInt2 = 0;
    public BluetoothCodecStatus codecStatus;

    AcmStackEvent(int type) {
        this.type = type;
    }

    @Override
    public String toString() {
        // event dump
        StringBuilder result = new StringBuilder();
        result.append("AcmStackEvent {type:" + eventTypeToString(type));
        result.append(", device:" + device);
        result.append(", state:" + eventTypeValueIntToString(type, valueInt1));
        result.append(", context type:" + contextTypeValueIntToString(valueInt2));
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
            case EVENT_TYPE_CONNECTION_STATE_CHANGED:
                return "EVENT_TYPE_CONNECTION_STATE_CHANGED";
            case EVENT_TYPE_AUDIO_STATE_CHANGED:
                return "EVENT_TYPE_AUDIO_STATE_CHANGED";
            case EVENT_TYPE_CODEC_CONFIG_CHANGED:
                return "EVENT_TYPE_CODEC_CONFIG_CHANGED";
            case EVENT_TYPE_META_DATA_CHANGED:
                return "EVENT_TYPE_META_DATA_CHANGED";
            default:
                return "EVENT_TYPE_UNKNOWN:" + type;
        }
    }

    private static String eventTypeValueIntToString(int type, int value) {
        switch (type) {
            case EVENT_TYPE_CONNECTION_STATE_CHANGED:
                switch (value) {
                    case CONNECTION_STATE_DISCONNECTED:
                        return "DISCONNECTED";
                    case CONNECTION_STATE_CONNECTING:
                        return "CONNECTING";
                    case CONNECTION_STATE_CONNECTED:
                        return "CONNECTED";
                    case CONNECTION_STATE_DISCONNECTING:
                        return "DISCONNECTING";
                    default:
                        break;
                }
                break;
            case EVENT_TYPE_AUDIO_STATE_CHANGED:
                switch (value) {
                    case AUDIO_STATE_REMOTE_SUSPEND:
                        return "REMOTE_SUSPEND";
                    case AUDIO_STATE_STOPPED:
                        return "STOPPED";
                    case AUDIO_STATE_STARTED:
                        return "STARTED";
                    default:
                        break;
                }
                break;
            default:
                break;
        }
        return Integer.toString(value);
    }

    private static String contextTypeValueIntToString(int value) {
        switch (value) {
            case CONTEXT_TYPE_UNKNOWN:
                return "UNKNOWN";
            case CONTEXT_TYPE_MUSIC:
                return "MEDIA";
            case CONTEXT_TYPE_VOICE:
                return "CONVERSATIONAL";
            case CONTEXT_TYPE_MUSIC_VOICE:
                return "MEDIA+CONVERSATIONAL";
            default:
                return "UNKNOWN:" + value;
        }
    }
}
