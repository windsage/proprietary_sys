/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 */

package com.qualcomm.qti.poweroffalarm;

import android.os.RemoteException;

public interface IAlarmBaseAdapter {
     int cancelAlarm() throws RemoteException;
     long getAlarm() throws RemoteException;
     long getRtcTime() throws RemoteException;
     int setAlarm(long time) throws RemoteException;
}
