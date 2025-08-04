/*******************************************************************************
@file    QcrilMsgTunnelService.java
@brief   Communicates with QCRIL for OEM specific reqs/responses

---------------------------------------------------------------------------
Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
All rights reserved.
Confidential and Proprietary - Qualcomm Technologies, Inc.
---------------------------------------------------------------------------
 ******************************************************************************/

package com.qualcomm.qcrilmsgtunnel;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;

public class QcrilMsgTunnelService extends Service {

    private static final String TAG = "QcrilMsgTunnelService";

    /**
     * <p>Broadcast Action: It indicates the an RIL_UNSOL_OEM_HOOK_RAW message was received
     * <p class="note">.
     * This is to indicate OEM applications that an unsolicited OEM message was received.
     *
     * <p class="note">This is a protected intent that can only be sent
     * by the system.
     */
    public static final String ACTION_UNSOL_RESPONSE_OEM_HOOK_RAW =
            "com.qualcomm.intent.action.ACTION_UNSOL_RESPONSE_OEM_HOOK_RAW";

    private static Context mContext;
    private static QcrilMsgTunnelIfaceManager mTunnelIface;

    public static Context getContext() {
        return mContext;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Returning mHookIface for QcrilMsgTunnelService binding.");
        return mTunnelIface;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate method");
        mContext = getBaseContext();
        mTunnelIface = new QcrilMsgTunnelIfaceManager(this);
        mContext.registerReceiver(mBroadcastReceiver,
                new IntentFilter(TelephonyManager.ACTION_MULTI_SIM_CONFIG_CHANGED),
                        Context.RECEIVER_EXPORTED);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "QcrilMsgTunnelService Destroyed Successfully...");
        mContext.unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(TelephonyManager.ACTION_MULTI_SIM_CONFIG_CHANGED)) {
                int activeSimCount = intent.getIntExtra(
                        TelephonyManager.EXTRA_ACTIVE_SIM_SUPPORTED_COUNT, 1);
                mTunnelIface.handleOnMultiSimConfigChanged(activeSimCount);
            }
        }
    };
}
