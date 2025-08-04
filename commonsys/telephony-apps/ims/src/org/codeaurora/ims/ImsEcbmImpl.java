/* Copyright (c) 2014, 2016, 2020, 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package org.codeaurora.ims;

import com.qualcomm.ims.utils.Log;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.telephony.ims.stub.ImsEcbmImplBase;

import androidx.annotation.VisibleForTesting;

public class ImsEcbmImpl extends ImsEcbmImplBase {

    private final int EVENT_ENTER_EMERGENCY_CALLBACK_MODE = 1;
    private final int EVENT_EXIT_EMERGENCY_CALLBACK_MODE = 2;

    private ImsServiceSub mServiceSub;
    private ImsSenderRxr mCi;
    private Handler mHandler = new ImsEcbmImplHandler();

    public ImsEcbmImpl(ImsServiceSub serviceSub, ImsSenderRxr ci) {
        mCi = ci;
        mCi.setEmergencyCallbackMode(mHandler, EVENT_ENTER_EMERGENCY_CALLBACK_MODE, null);
        mCi.registerForExitEmergencyCallbackMode(mHandler, EVENT_EXIT_EMERGENCY_CALLBACK_MODE,
                null);
    }

    public void exitEmergencyCallbackMode() {
        mCi.exitEmergencyCallbackMode(Message.obtain(mHandler, EVENT_EXIT_EMERGENCY_CALLBACK_MODE));
    }

    // Handler for tracking requests/UNSOLs to/from ImsSenderRxr.
    private class ImsEcbmImplHandler extends Handler {
        ImsEcbmImplHandler() {
            this(Looper.getMainLooper());
        }

        ImsEcbmImplHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.i(this, "Message received: what = " + msg.what);

            switch (msg.what) {
                case EVENT_ENTER_EMERGENCY_CALLBACK_MODE:
                    Log.i(this, "EVENT_ENTER_EMERGENCY_CALLBACK_MODE");
                    try {
                        enteredEcbm();
                    } catch (RuntimeException ex) {
                        Log.w(this, "Unable to notify enter ECBM: " + ex);
                    }
                    break;
                case EVENT_EXIT_EMERGENCY_CALLBACK_MODE:
                    Log.i(this, "EVENT_EXIT_EMERGENCY_CALLBACK_MODE");
                    try {
                        exitedEcbm();
                    } catch (RuntimeException ex) {
                        Log.w(this, "Unable to notify exit ECBM: " + ex);
                    }
                    break;
                default:
                    Log.i(this, "Unknown message = " + msg.what);
                    break;
            }
        }
    }

    @VisibleForTesting
    public Handler getHandler() {
        return mHandler;
    }
}
