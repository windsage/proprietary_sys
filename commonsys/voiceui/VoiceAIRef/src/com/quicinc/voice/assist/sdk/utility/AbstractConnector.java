/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.quicinc.voice.assist.sdk.utility;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public abstract class AbstractConnector {
    private final Context mContext;
    private final Intent mServiceIntent;
    private IBinder mIntentIBinder;
    private final ServiceConnection mIntentServiceConnection;

    private ServiceConnectListener mConnectListener;

    protected AbstractConnector(Context context, Intent intent) {
        mContext = context;
        mServiceIntent = intent;
        mIntentIBinder = null;
        mIntentServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mIntentIBinder = service;
                if (mConnectListener != null) {
                    mConnectListener.onServiceConnected();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mIntentIBinder = null;
                if (mConnectListener != null) {
                    mConnectListener.onServiceDisConnected(name);
                }
            }
        };
    }

    protected IBinder getBinder() {
        return mIntentIBinder;
    }

    public boolean connect(ServiceConnectListener listener) {
        mConnectListener = listener;
        if (mIntentIBinder != null) return true;
        return mContext.bindService(mServiceIntent, mIntentServiceConnection,
                Context.BIND_AUTO_CREATE | Context.BIND_INCLUDE_CAPABILITIES);
    }

    public void disconnect() {
        if(mIntentIBinder !=null) {
            mContext.unbindService(mIntentServiceConnection);
        }
        mConnectListener = null;
    }

    public interface ServiceConnectListener {
        void onServiceConnected();

        void onServiceDisConnected(ComponentName name);
    }
}
