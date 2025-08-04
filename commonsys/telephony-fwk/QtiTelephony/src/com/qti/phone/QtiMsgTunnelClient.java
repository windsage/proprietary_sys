/*
 * Copyright (c) 2020-2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qti.phone;

import android.content.Context;
import android.util.Log;
import android.os.Message;
import android.os.RemoteException;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.qualcomm.qcrilhook.OemHookCallback;
import com.qualcomm.qcrilhook.QcRilHook;
import com.qualcomm.qcrilhook.QcRilHookCallback;
import com.qti.extphone.IDepersoResCallback;

public class QtiMsgTunnelClient {

    private static final String LOG_TAG = "QtiMsgTunnelClient";
    private static Context mContext;
    private static QtiMsgTunnelClient mInstance;
    private QcRilHook mQcRilHook;
    private boolean mQcRilHookReady = false;
    private ArrayList<InternalOemHookCallback> mInternalOemHookCallbacks;

    private final static int BYTE_SIZE = 1;
    private static final char NULL_TERMINATOR = '\0';
    private static final int NULL_TERMINATOR_LENGTH = BYTE_SIZE;

    /**
     * Callbacks interface to inform about QcRilOemHook status.
     */
    public interface InternalOemHookCallback {
        /**
         * @return the class name of the callback
         */
        String getCallBackName();

        /**
         * Called when a connection between QcRilHook and QcRilMsgTunnel has been established.
         */
        void onOemHookConnected();

        /**
         * Called when the connection between QcRilHook and QcRilMsgTunnel gets broken.
         */
        void onOemHookDisconnected();
    }

    private QtiMsgTunnelClient(Context context) {
        mContext = context;
        mInternalOemHookCallbacks = new ArrayList<>();
        mQcRilHook = new QcRilHook(mContext, mQcrilHookCb);
    }

    public void registerOemHookCallback(InternalOemHookCallback callback) {
        mInternalOemHookCallbacks.add(callback);
        notifyOemHookCallbacks();
    }

    public void unregisterOemHookCallback(InternalOemHookCallback callback) {
        mInternalOemHookCallbacks.remove(callback);
    }

    private void notifyOemHookCallbacks() {
        for (InternalOemHookCallback callback : mInternalOemHookCallbacks) {
            Log.d(LOG_TAG, "notifying OemHook status to " + callback.getCallBackName());
            if (mQcRilHookReady) {
                callback.onOemHookConnected();
            } else {
                callback.onOemHookDisconnected();
            }
        }
    }

    /**
    * This method returns the singleton instance of QtiMsgTunnelClient object
    */
    public static synchronized QtiMsgTunnelClient getInstance() {
        synchronized (QtiMsgTunnelClient.class) {
            if (mInstance == null) {
                Log.d(LOG_TAG, "QtiMsgTunnelClient instance null !!! ");
            }
            return mInstance;
        }
    }

    public static void init(Context context) {
        synchronized (QtiMsgTunnelClient.class) {
            if (mInstance == null) {
                mInstance = new QtiMsgTunnelClient(context);
            }
        }
    }

    /**
     * Called when connection to QcrilMsgTunnelService has been established.
     */
    private QcRilHookCallback mQcrilHookCb = new QcRilHookCallback() {
        public void onQcRilHookReady() {
            mQcRilHookReady = true;
            Log.d(LOG_TAG, "QcRilHook Service ready, notifying registrants");
            notifyOemHookCallbacks();
        }

        public synchronized void onQcRilHookDisconnected() {
            mQcRilHookReady = false;
            Log.d(LOG_TAG, "QcRilHook Service disconnected, notifying registrants");
            notifyOemHookCallbacks();
        }
    };

    public boolean performIncrementalScan(int slotId) {
        if (!mQcRilHookReady) {
            Log.e(LOG_TAG, "QcRilhook not ready. Bail out");
            return false;
        }
        if (QtiRadioConfigProxy.isInSecureMode()) {
            Log.d(LOG_TAG, "performIncrementalScan not allowed in Secure Mode");
            return false;
        }
        Log.d(LOG_TAG, "performIncrementalScan started...");
        return mQcRilHook.qcRilPerformIncrManualScan(slotId);
    }

    public boolean abortIncrementalScan(int slotId) {
        if (!mQcRilHookReady) {
            Log.e(LOG_TAG, "QcRilhook not ready. Bail out");
            return false;
        }
        Log.d(LOG_TAG, "abortIncrementalScan started...");
        return mQcRilHook.qcRilAbortNetworkScan(slotId);
    }

    public void supplyIccDepersonalization(String netpin, String type,
                IDepersoResCallback callback, int phoneId) {
        if (!mQcRilHookReady) {
            Log.e(LOG_TAG, "QcRilhook not ready. Bail out");
            return;
        }
        Message msg = Message.obtain();
        byte[] payload = null;
        // type + null character + netpin + null character
        int payloadLength  = type.length() + NULL_TERMINATOR_LENGTH +
                (netpin == null ? NULL_TERMINATOR_LENGTH
                : netpin.length() + NULL_TERMINATOR_LENGTH);

        payload = new byte[payloadLength];
        ByteBuffer buf = mQcRilHook.createBufferWithNativeByteOrder(payload);
        // type
        buf.put(type.getBytes());
        buf.put((byte)NULL_TERMINATOR); // null character
        // pin
        if (netpin != null) buf.put(netpin.getBytes());
        buf.put((byte)NULL_TERMINATOR); // null character

        OemHookCallback oemHookCb = new DepersoCallback(callback, msg);
        mQcRilHook.sendQcRilHookMsgAsync(
                QcRilHook.QCRIL_EVT_HOOK_ENTER_DEPERSONALIZATION_CODE, payload,
                oemHookCb, phoneId);

    }

    public void sendAtelReadyStatus(int isReady, int phoneId) {
        Log.d(LOG_TAG, "sendAtelReadyStatus, isReady: " + isReady + ", phoneId: " + phoneId);

        if (!mQcRilHookReady) {
            Log.e(LOG_TAG, "QcRilhook not ready. Bail out");
            return;
        }
        byte[] payload = new byte[BYTE_SIZE];
        ByteBuffer reqBuffer = QcRilHook.createBufferWithNativeByteOrder(payload);
        reqBuffer.put((byte) isReady);

        mQcRilHook.sendQcRilHookMsg(
                QcRilHook.QCRIL_EVT_HOOK_SET_ATEL_UI_STATUS, payload, phoneId);
    }

    private class DepersoCallback extends OemHookCallback {
        IDepersoResCallback depersoCallBack;
        int SUCCESS = 0;
        int ERROR = 1;

        public DepersoCallback(IDepersoResCallback callback, Message msg) {
            super(msg);
            depersoCallBack = callback;
        }

        @Override
        public void onOemHookResponse(byte[] response, int phoneId) throws RemoteException {
            if (response != null) {
                Log.d(LOG_TAG, "DepersoResult SUCCESS");
                depersoCallBack.onDepersoResult(SUCCESS, phoneId);
            } else {
                Log.d(LOG_TAG, "DepersoResult ERROR");
                depersoCallBack.onDepersoResult(ERROR, phoneId);
            }
        }

        @Override
        public void onOemHookException(int phoneId) throws RemoteException  {
            Log.d(LOG_TAG, "DepersoResult ERROR");
            depersoCallBack.onDepersoResult(ERROR, phoneId);
        }
    }
}
