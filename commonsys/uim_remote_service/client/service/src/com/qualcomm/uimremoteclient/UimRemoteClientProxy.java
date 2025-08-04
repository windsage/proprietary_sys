/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.uimremoteclient;

import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import java.util.concurrent.atomic.AtomicInteger;

import vendor.qti.hardware.radio.uim_remote_client.UimRemoteClientCardInitStatusType;

public class UimRemoteClientProxy {

    private static final String LOG_TAG = "UimRemoteClientProxy";
    private Context mContext;
    private IUimRemoteClientInterface[] mUimRemoteClient;
    private final AtomicInteger mToken = new AtomicInteger(0);

    public UimRemoteClientProxy(Context context) {
        mContext = context;
        mUimRemoteClient = UimRemoteClientFactory.makeUimRemoteClient(context);
        Log.d(LOG_TAG, "Made mUimRemoteClient - length = " +
                mUimRemoteClient.length);
    }

    public void registerResponseHandler(Handler responseHandler, int slotId) {
        mUimRemoteClient[slotId].registerResponseHandler(responseHandler);
    }

    public int uimRemoteEvent(int slotId, int event, byte[] atr, int errCode,
                    boolean has_transport, int transport, boolean has_usage, int usage,
                    boolean has_apdu_timeout,int apdu_timeout,
                    boolean has_disable_all_polling, int disable_all_polling,
                    boolean has_poll_timer, int poll_timer)
                    throws RemoteException {
        Log.d(LOG_TAG, "uimRemoteEvent: slotId = " + slotId);
        if (!isSlotIdValid(slotId)) {
            return -1;
        }
        int token = getNextToken();
        mUimRemoteClient[slotId].uimRemoteEvent(token, event, atr, errCode,
                has_transport, transport, has_usage, usage, has_apdu_timeout,
                apdu_timeout, has_disable_all_polling, disable_all_polling,
                has_poll_timer, poll_timer);
        return token;
    }

    public int uimRemoteApdu(int slotId, int apduStatus, byte[] apduResp
            ) throws RemoteException {
        Log.d(LOG_TAG, "uimRemoteApdu: slotId = " + slotId);
        if (!isSlotIdValid(slotId)) {
            return -1;
        }
        int token = getNextToken();
        mUimRemoteClient[slotId].uimRemoteApdu(token, apduStatus, apduResp);
        return token;
    }

    class ClientBinderDeathRecipient implements IBinder.DeathRecipient {
        IUimRemoteClientServiceCallback mCallback;

        public ClientBinderDeathRecipient(IUimRemoteClientServiceCallback callback) {
            Log.d(LOG_TAG, "registering for client cb = " + callback + " binder = " +
                    callback.asBinder() + " death notification");
            mCallback = callback;
        }

        @Override
        public void binderDied() {
            Log.d(LOG_TAG, "Client callback = " + mCallback + " binder = " +
                    mCallback.asBinder() + "died");
            IBinder binder = mCallback.asBinder();
            binder.unlinkToDeath(this, 0);
        }
    }

    private boolean isSlotIdValid(int slotId) {
        if (slotId >= 0 && slotId < mUimRemoteClient.length) {
            return true;
        }
        Log.d(LOG_TAG, "Invalid slotId = " + slotId + ", mUimRemoteClient length= "
                + mUimRemoteClient.length + " skipping request!");
        return false;
    }

    int getNextToken() {
        return mToken.incrementAndGet();
    }
}
