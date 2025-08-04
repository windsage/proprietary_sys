/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.uimremoteserver;

import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import java.util.concurrent.atomic.AtomicInteger;

import vendor.qti.hardware.radio.uim_remote_server.V1_0.UimRemoteServiceServerResultCode;

public class UimRemoteServerProxy {

    private static final String LOG_TAG = "UimRemoteServerProxy";
    private IUimRemoteServerInterface[] mUimRemoteServer;
    private final AtomicInteger mToken = new AtomicInteger(0);
    private static final int INVALID_TOKEN = -1;

    public UimRemoteServerProxy(Context context) {
        mUimRemoteServer = UimRemoteServerFactory.makeUimRemoteServer(context);
        Log.d(LOG_TAG, "Made mUimRemoteServer - length = " +
                mUimRemoteServer.length);
    }

    public void registerResponseHandler(Handler responseHandler, int slotId) {
        mUimRemoteServer[slotId].registerResponseHandler(responseHandler);
    }

    public int uimRemoteServerConnectReq(int slotId, int maxMessageSize)
            throws RemoteException {
        if (!isSlotIdValid(slotId)) {
            return INVALID_TOKEN;
        }
        int token = getNextToken();
        mUimRemoteServer[slotId].uimRemoteServerConnectReq(token, maxMessageSize);
        return token;
    }

    public int uimRemoteServerDisconnectReq(int slotId) throws RemoteException {
        if (!isSlotIdValid(slotId)) {
            return INVALID_TOKEN;
        }
        int token = getNextToken();
        mUimRemoteServer[slotId].uimRemoteServerDisconnectReq(token);
        return token;
    }

    public int uimRemoteServerApduReq(int slotId, byte[] cmd) throws RemoteException {
        if (!isSlotIdValid(slotId)) {
            return INVALID_TOKEN;
        }
        int token = getNextToken();
        mUimRemoteServer[slotId].uimRemoteServerApduReq(token, cmd);
        return token;
    }

    public int uimRemoteServerTransferAtrReq(int slotId) throws RemoteException {
        if (!isSlotIdValid(slotId)) {
            return INVALID_TOKEN;
        }
        int token = getNextToken();
        mUimRemoteServer[slotId].uimRemoteServerTransferAtrReq(token);
        return token;
    }

    public int uimRemoteServerPowerReq(int slotId, boolean state) throws RemoteException {
        if (!isSlotIdValid(slotId)) {
            return INVALID_TOKEN;
        }
        int token = getNextToken();
        mUimRemoteServer[slotId].uimRemoteServerPowerReq(token, state);
        return token;
    }

    public int uimRemoteServerResetSimReq(int slotId) throws RemoteException {
        if (!isSlotIdValid(slotId)) {
            return INVALID_TOKEN;
        }
        int token = getNextToken();
        mUimRemoteServer[slotId].uimRemoteServerResetSimReq(token);
        return token;
    }

    class ClientBinderDeathRecipient implements IBinder.DeathRecipient {
        IUimRemoteServerServiceCallback mCallback;

        public ClientBinderDeathRecipient(IUimRemoteServerServiceCallback callback) {
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
        if (slotId >= 0 && slotId < mUimRemoteServer.length) {
            return true;
        }
        Log.d(LOG_TAG, "Invalid slotId = " + slotId + ", mUimRemoteServer length= "
                + mUimRemoteServer.length + " skipping request!");
        return false;
    }

    int getNextToken() {
        return mToken.incrementAndGet();
    }
}
