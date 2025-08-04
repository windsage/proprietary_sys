/* Copyright (c) 2015-2017, 2019-2021, 2023 Qualcomm Technologies, Inc.
  * All Rights Reserved.
  * Confidential and Proprietary - Qualcomm Technologies, Inc.
  */
package com.qualcomm.ims.vt;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.telephony.ims.ImsReasonInfo;
import android.util.Log;

import java.util.List;

import org.codeaurora.ims.DriverCallIms;
import org.codeaurora.ims.ICallListListener;
import org.codeaurora.ims.ImsCallSessionImpl;
import org.codeaurora.ims.ImsCallUtils;
import org.codeaurora.ims.ImsServiceSub;
import org.codeaurora.ims.ImsUssdSessionImpl;
import org.codeaurora.ims.QtiCallConstants;
import org.codeaurora.ims.utils.QtiImsExtUtils;

/**
 * This class listens for battery status changes and if device is in
 * low battery, for SKT operator this class shall:
 * 1. disconnect all active VT calls.
 * 2. disconnect MO/MT VT calls
 * 3. reject upgrade requests
 * For RJIL operator, calls will not be disconnected but this class informs
 * all the alive sessions that battery is low
 */
public class LowBatteryHandler implements ICallListListener {

    private static String TAG = "VideoCall_LowBattery";
    private static LowBatteryHandler sInstance;
    private Context mContext;
    private List<ImsServiceSub> mServiceSubs;
    private boolean mIsLowBattery = false;
    private final boolean isCarrierOneSupported = ImsCallUtils.isCarrierOneSupported();

    private LowBatteryHandler(List<ImsServiceSub> serviceSubs, Context context) {
        mContext = context;
        mServiceSubs = serviceSubs;
        mContext.registerReceiver(mBatteryLevel,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    /**
     * Creates an instance of the Low battery handler class.
     *
     * @param serviceSubs list of command interface instances
     * @param context context of this class
     *
     * @return Low battery handler instance
     */
    public static LowBatteryHandler init(List<ImsServiceSub> serviceSubs,
            Context context) {
        if (sInstance == null) {
            sInstance = new LowBatteryHandler(serviceSubs, context);
        } else {
            throw new RuntimeException("LowBatteryHandler: Multiple initialization");
        }
        return sInstance;
    }

    public void dispose() {
        Log.i(TAG, "dispose()");
        mContext.unregisterReceiver(mBatteryLevel);
        sInstance = null;
    }

    /**
     * Get instance of Low battery handler.
     * @return Low battery handler instance
     */
    public static LowBatteryHandler getInstance() {
        if (sInstance != null) {
            return sInstance;
        } else {
            throw new RuntimeException("LowBatteryHandler: Not initialized");
        }
    }

    /**
     * Brodcast receiver to handle battery events.
     */
    private BroadcastReceiver mBatteryLevel = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                mIsLowBattery = intent.getBooleanExtra(BatteryManager.EXTRA_BATTERY_LOW, false);
                for (ImsServiceSub serviceSub : mServiceSubs) {
                    boolean allowVideoCallsInLowBattery = QtiImsExtUtils.
                        allowVideoCallsInLowBattery(serviceSub.getPhoneId(), mContext);
                    if (mIsLowBattery && !allowVideoCallsInLowBattery) {
                        // Disconnect video calls only for SKT.
                        Log.d(TAG, "disconnectVideoCalls on low battery");
                        disconnectVideoCalls(serviceSub);
                    }
                    if (!allowVideoCallsInLowBattery || isCarrierOneSupported) {
                        // Update low battery status for both RJIL and SKT.
                        serviceSub.updateLowBatteryStatus();
                    }
                }
            }
        }
    };

    @Override
    public void onSessionAdded(ImsCallSessionImpl callSession) {
        Log.d(TAG, "onSessionAdded callSession = " + callSession);

        if (!isCarrierOneSupported) {
            return;
        }

        if (mIsLowBattery && ImsCallUtils.isIncomingCall(callSession)) {
            for (ImsServiceSub serviceSub : mServiceSubs) {
                List<ImsCallSessionImpl> sessionList =
                        serviceSub.getCallSessionByState(DriverCallIms.State.DIALING);
                if (!sessionList.isEmpty()) {
                    /*
                     * There can be a possibility to receive incoming call when MO low
                     * battery video call is deferred waiting to get user input whether
                     * to continue the call as Video call or convert to voice call.
                     * In such cases, allow the MT call by terminating the pending
                     * MO low battery video call.
                     */
                    sessionList.get(0).terminate(ImsReasonInfo.CODE_USER_TERMINATED);
                }
            }
        }
    }

    @Override
    public void onSessionRemoved(ImsCallSessionImpl callSession) {
        //Dummy
    }

    @Override
    public void onSessionAdded(ImsUssdSessionImpl ussdSession) {
        //Dummy
    }

    @Override
    public void onSessionRemoved(ImsUssdSessionImpl ussdSession) {
        //Dummy
    }

    /**
     * To know current battery status of device.
     * @return boolean, true if device battery status is LOW
     *                  false if device battery status is OK
     */
    public boolean isLowBattery(int phoneId) {
        if (isCarrierOneSupported ||
                !(QtiImsExtUtils.allowVideoCallsInLowBattery(phoneId, mContext))) {
            return mIsLowBattery;
        }
        return false;
    }

    /**
     * To disconnect all active and hold video calls
     */
    private void disconnectVideoCalls(ImsServiceSub serviceSub) {
        Log.d(TAG, "disconnectVideoCalls ");
        disconnectVideoCalls(serviceSub, DriverCallIms.State.ACTIVE);
        disconnectVideoCalls(serviceSub, DriverCallIms.State.HOLDING);
    }

    /**
     * Disconnect all video calls depends on state.
     */
    private void disconnectVideoCalls(ImsServiceSub serviceSub, DriverCallIms.State state) {
        for (ImsCallSessionImpl session : serviceSub.getCallSessionByState(state)) {
            Log.d(TAG, "disconnectVideoCalls session : " + session);
            if (ImsCallUtils.isVideoCall(session.getInternalCallType())) {
                session.terminate(ImsReasonInfo.CODE_LOW_BATTERY);
            }
        }
    }
}
