/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 */

package com.qualcomm.qti.poweroffalarm;

import android.os.RemoteException;

public class AlarmAIDLAdapter implements IAlarmBaseAdapter {
    private vendor.qti.hardware.alarm.IAlarm mProxy;
    public AlarmAIDLAdapter(vendor.qti.hardware.alarm.IAlarm proxy) {
        mProxy = proxy;
    }

    @Override
    public int cancelAlarm() throws RemoteException{
        return mProxy.cancelAlarm();
    }

    @Override
    public long getAlarm() throws RemoteException{
        return mProxy.getAlarm();
    }

    @Override
    public long getRtcTime() throws RemoteException{
        return mProxy.getRtcTime();
    }

    @Override
    public int setAlarm(long time) throws RemoteException{
        return mProxy.setAlarm(time);
    }
}
