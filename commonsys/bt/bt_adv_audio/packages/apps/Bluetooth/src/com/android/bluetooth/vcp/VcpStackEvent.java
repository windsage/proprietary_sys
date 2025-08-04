/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
*/
/*
 *Copyright (c) 2020, The Linux Foundation. All rights reserved.
 *Not a contribution
 */

/*
 * Copyright 2018 The Android Open Source Project
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

package com.android.bluetooth.vcp;

import android.bluetooth.BluetoothDevice;

/**
 * Stack event sent via a callback from JNI to Java, or generated
 * internally by the VCP State Machine.
 */
public class VcpStackEvent {
    // Event types for STACK_EVENT message (coming from native)
    private static final int EVENT_TYPE_NONE = 0;
    public static final int EVENT_TYPE_CONNECTION_STATE_CHANGED = 1;
    public static final int EVENT_TYPE_VOLUME_STATE_CHANGED = 2;
    public static final int EVENT_TYPE_VOLUME_FLAGS_CHANGED = 3;

    // Do not modify without updating the HAL bt_vcp.h files.
    // Match up with enum class ConnectionState of bt_vcp_controller.h.
    static final int CONNECTION_STATE_DISCONNECTED = 0;
    static final int CONNECTION_STATE_CONNECTING = 1;
    static final int CONNECTION_STATE_CONNECTED = 2;
    static final int CONNECTION_STATE_DISCONNECTING = 3;

    public int type;
    public BluetoothDevice device;
    public int valueInt1;
    public int valueInt2;

    VcpStackEvent(int type) {
        this.type = type;
    }

    @Override
    public String toString() {
        // event dump
        StringBuilder result = new StringBuilder();
        result.append("VcpStackEvent {type:" + eventTypeToString(type));
        result.append(", device:" + device);
        result.append(", value1:" + valueInt1);
        result.append(", value2:" + valueInt2);
        result.append("}");
        return result.toString();
    }

    private static String eventTypeToString(int type) {
        switch (type) {
            case EVENT_TYPE_NONE:
                return "EVENT_TYPE_NONE";
            case EVENT_TYPE_CONNECTION_STATE_CHANGED:
                return "EVENT_TYPE_CONNECTION_STATE_CHANGED";
            case EVENT_TYPE_VOLUME_STATE_CHANGED:
                return "EVENT_TYPE_VOLUME_STATE_CHANGED";
            case EVENT_TYPE_VOLUME_FLAGS_CHANGED:
                return "EVENT_TYPE_VOLUME_FLAGS_CHANGED";
            default:
                return "EVENT_TYPE_UNKNOWN:" + type;
        }
    }
}

