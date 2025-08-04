/*****************************************************************
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 ******************************************************************/

package vendor.qti.bluetooth.xpan;

import android.bluetooth.BluetoothDevice;
import android.net.MacAddress;

public class XpanEvent {

    // Events
    static final int SAP_STATE_CHANGE = 1;
    static final int SSID_CONN_STATE_CHANGE = 2;
    static final int TWT_SESSION = 3;
    static final int BEARER_SWITCH = 4;
    static final int BEACON_INTERVAL_RECEIVED = 5;
    static final int UPDATE_BEACON_INTERVALS = 6;
    static final int TWT_TERMINATE_IN_CONNECTED_STATE = 7;
    static final int BEARER_PREFERENCE_RES = 8;
    static final int CB_CSS = 9;
    static final int BEARER_PREFERENCE = 10;

    private BluetoothDevice device;
    private int eventType, arg1, arg2, arg3, urgency;
    private long tbtTsf;
    private Object obj;
    private MacAddress remoteMacAddr;

    XpanEvent(int eventType) {
        this.eventType = eventType;
        arg1 = -1;
        arg2 = -1;
        arg3 = -1;
        tbtTsf = -1;
        urgency = 0;
    }

    XpanEvent setDevice(BluetoothDevice device) {
        this.device = device;
        return this;
    }

    void setArg1(int arg1) {
        this.arg1 = arg1;
    }

    void setArg2(int arg2) {
        this.arg2 = arg2;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    int getEventType() {
        return eventType;
    }

    int getArg1() {
        return arg1;
    }

    int getArg2() {
        return arg2;
    }

    int getArg3() {
        return arg3;
    }

    Object getObj() {
        return obj;
    }

    void setArg3(int arg3) {
        this.arg3 = arg3;
    }

    void setObject(Object obj) {
        this.obj = obj;
    }

    public MacAddress getRemoteMacAddr() {
        return remoteMacAddr;
    }

    void setRemoteMacAddr(MacAddress remoteMacAddr) {
        this.remoteMacAddr = remoteMacAddr;
    }

    void setTbtTsf(long tbtTsf) {
        this.tbtTsf = tbtTsf;
    }

    long getTbtTsf() {
        return tbtTsf;
    }

    int getUrgency() {
        return urgency;
    }

    void setUrgency(int urgency) {
        this.urgency = urgency;
    }

    @Override
    public String toString() {

        if (XpanUtils.VDBG) {

            StringBuilder builder = new StringBuilder();
            if (device != null) {
                builder.append(" " + device);
            }
            if (eventType != -1) {
                builder.append(" " + getEventMsg());
            }
            if (arg1 != -1) {
                builder.append(" arg1:" + arg1);
            }
            if (arg2 != -1) {
                builder.append(" arg2:" + arg2);
            }
            if (arg3 != -1) {
                builder.append(" arg3:" + arg3);
            }
            if (tbtTsf != -1) {
                builder.append(" tbtTsf:" + tbtTsf);
            }
            if (obj != null) {
                builder.append(" obj:" + obj);
            }
            if (remoteMacAddr != null) {
                builder.append(" remoteMacAddr:" + remoteMacAddr);
            }
            return builder.toString();
        } else {
            return eventType + "";
        }
    }

    private String getEventMsg() {
        String type = "";
        switch (eventType) {
        case SAP_STATE_CHANGE:
            type = "SAP_STATE_CHANGE";
            break;
        case SSID_CONN_STATE_CHANGE:
            type = "SSID_CONN_STATE_CHANGE";
            break;
        case TWT_SESSION:
            type = "TWT_SESSON";
            break;
        case BEARER_SWITCH:
            type = "BEARER_SWITCH";
            break;
        case BEACON_INTERVAL_RECEIVED:
            type = "BEACON_INTERVAL_RECEIVED";
            break;
        case UPDATE_BEACON_INTERVALS:
            type = "UPDATE_BEACON_INTERVALS";
            break;
        case TWT_TERMINATE_IN_CONNECTED_STATE:
            type = "TWT_TERMINATE_IN_CONNECTED_STATE";
            break;
        case BEARER_PREFERENCE_RES:
            type = "BEARER_PREFERENCE_RES";
            break;
        case CB_CSS:
            type = "CB_CSS";
            break;
        default:
            type = eventType + " Unknown";
            break;
        }
        return type;
    }
}