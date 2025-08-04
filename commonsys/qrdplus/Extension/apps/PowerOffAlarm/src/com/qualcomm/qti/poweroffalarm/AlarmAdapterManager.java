/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 */

package com.qualcomm.qti.poweroffalarm;

import android.os.RemoteException;
import android.util.Log;
import java.util.NoSuchElementException;
import android.os.ServiceManager;

public class AlarmAdapterManager {
    private static final String TAG = "AlarmAdapterManager";

    private static IAlarmBaseAdapter maybeConnectToV1_0() {
        vendor.qti.hardware.alarm.V1_0.IAlarm proxy = null;
        try {
            proxy = vendor.qti.hardware.alarm.V1_0.IAlarm.getService(true /* retry */);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException while attaching to Alarm HAL proxy", e);
        } catch (NoSuchElementException e) {
            Log.i(TAG, "Alarm HAL service not found");
        }
        return (proxy == null) ? null : new AlarmV1_0Adapter(proxy);
    }

    private static IAlarmBaseAdapter maybeConnectToAidl() {
        vendor.qti.hardware.alarm.IAlarm proxy = null;
        final String aidlServiceName =
                vendor.qti.hardware.alarm.IAlarm.class.getCanonicalName() + "/default";
        boolean isDeclared = false;
        try {
            isDeclared = ServiceManager.isDeclared(aidlServiceName);
        } catch (Exception e) {
            Log.e(TAG, "Alarm AIDL service was declared exception : " + e.toString());
            isDeclared = false;
        }
        if (isDeclared) {
            proxy = vendor.qti.hardware.alarm.IAlarm.Stub.asInterface(
                    ServiceManager.waitForService(aidlServiceName));
            if (proxy == null) {
                Log.e(TAG, "Alarm AIDL service was declared but was not found");
            }
        } else {
            Log.d(TAG, "Alarm AIDL service is not declared");
        }
        return (proxy == null) ? null : new AlarmAIDLAdapter(proxy);
    }


    public static IAlarmBaseAdapter getAlarmAdapter() {
        IAlarmBaseAdapter adapter = maybeConnectToAidl();
        if (adapter == null) {
            adapter = maybeConnectToV1_0();
        }
        return adapter;
    }
}
