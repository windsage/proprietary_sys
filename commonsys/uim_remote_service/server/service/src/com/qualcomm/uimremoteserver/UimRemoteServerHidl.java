/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.uimremoteserver;

import android.content.Context;
import android.os.Binder;
import android.os.Handler;
import android.os.HwBinder;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import android.hidl.manager.V1_0.IServiceManager;
import android.hidl.manager.V1_0.IServiceNotification;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;

import vendor.qti.hardware.radio.uim_remote_server.V1_0.IUimRemoteServiceServer;
import vendor.qti.hardware.radio.uim_remote_server.V1_0.IUimRemoteServiceServerResponse;
import vendor.qti.hardware.radio.uim_remote_server.V1_0.IUimRemoteServiceServerIndication;
import vendor.qti.hardware.radio.uim_remote_server.V1_0.UimRemoteServiceServerApduType;

public class UimRemoteServerHidl implements IUimRemoteServerInterface {

    private final String LOG_TAG = "UimRemoteServerHidl";
    private IUimRemoteServiceServer mUimRemoteServer;
    private int mSlotId;
    private Context mContext;
    private IBinder mBinder;
    private static Handler mRespHdlr;
    private static final int EVENT_REMOTE_SERVER_SERVICE_DEAD = 1;
    private static final int DEFAULT_REINIT_TIMEOUT_MS = 3000;
    private UimRemoteServerHandler mRemoteHandler;
    private final String mInstanceName;
    private final ServiceNotification mServiceNotification =
                    new ServiceNotification();
    private final AtomicLong mUimRemoteServerCookie = new AtomicLong(0);
    private IUimRemoteServiceServerResponse mUimRemoteServerResp;
    private IUimRemoteServiceServerIndication mUimRemoteServerInd;
    private UimRemoteServerDeathRecipient mDeathRecipient;
    private boolean mIsDisposed = false;
    private ConcurrentHashMap<Integer, Integer> mInflightRequests = new ConcurrentHashMap<Integer,
                    Integer>();
    private final Object mLock = new Object();

    public UimRemoteServerHidl(int slotId) {
        mSlotId = slotId;
        mInstanceName = "uimRemoteServer" + slotId;
        try {
            boolean ret = IServiceManager.getService().registerForNotifications
                    ("vendor.qti.hardware.radio.uim_remote_server@1.0::IUimRemoteServiceServer",
                    mInstanceName, mServiceNotification);
            if (!ret) {
                Log.d(LOG_TAG, "Unable to register service start notification: ret" + ret);
            }
        } catch(RemoteException ex) {
             Log.e(LOG_TAG, "registerForServiceNotifications: exception" + ex);
        }
        mDeathRecipient = new UimRemoteServerDeathRecipient();
        mRemoteHandler = new UimRemoteServerHandler();
    }

    private final class ServiceNotification extends IServiceNotification.Stub {
        @Override
        public void onRegistration(String fqName, String name, boolean preexisting) {
            if (!mInstanceName.equals(name) || mIsDisposed) {
                Log.d(LOG_TAG, "onRegistration: Ignoring.");
                return;
            }
            Log.d(LOG_TAG, "ServiceNotification - onRegistration");
            initUimRemoteServerHidl();
        }
    }

    private synchronized void initUimRemoteServerHidl() {
        Log.d(LOG_TAG, "initUimRemoteServerHidl");
        try {
            vendor.qti.hardware.radio.uim_remote_server.V1_0.
                    IUimRemoteServiceServer uimRemoteServer;
            uimRemoteServer = vendor.qti.hardware.radio.uim_remote_server.V1_0.
                    IUimRemoteServiceServer.getService(mInstanceName);
            if (uimRemoteServer == null) {
                Log.e(LOG_TAG, "initUimRemoteServerHidl: uimRemoteServer == null");
                return;
            }
            uimRemoteServer.linkToDeath(mDeathRecipient,
                                    mUimRemoteServerCookie.incrementAndGet());
            mUimRemoteServerResp = new UimRemoteServerResponse(mSlotId);
            mUimRemoteServerInd = new UimRemoteServerIndication(mSlotId);
            uimRemoteServer.setCallback(mUimRemoteServerResp, mUimRemoteServerInd);
            synchronized (mLock) {
                mUimRemoteServer = uimRemoteServer;
            }
        } catch(RemoteException e) {
            Log.e(LOG_TAG, "initUimRemoteServerHidl: Exception=" + e);
        }
    }

    final class UimRemoteServerDeathRecipient implements HwBinder.DeathRecipient {
        @Override
        public void serviceDied(long cookie) {
            Log.d(LOG_TAG, "serviceDied");
            resetServiceAndRequestList();
            mRemoteHandler.sendMessageDelayed(mRemoteHandler.obtainMessage(
                        EVENT_REMOTE_SERVER_SERVICE_DEAD), DEFAULT_REINIT_TIMEOUT_MS);
        }
    }

    public class UimRemoteServerHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_REMOTE_SERVER_SERVICE_DEAD:
                    Log.d(LOG_TAG, "EVENT_REMOTE_SERVER_SERVICE_DEAD reinitialize ...");
                    initUimRemoteServerHidl();
                    break;
            }
        }
    }

    private final class UimRemoteServerResponse extends
                    IUimRemoteServiceServerResponse.Stub {
        int mSlotId;

        public UimRemoteServerResponse(int slotId) {
            Log.d(LOG_TAG, "[" + slotId + "] Constructor: ");
            mSlotId = slotId;
        }

        @Override
        public void uimRemoteServiceServerConnectResponse(int serial, int sapConnectRsp,
                int maxMsgSize) {
            UimRemoteServerResult resultObj = new UimRemoteServerResult();
            resultObj.setSapConnectRsp(sapConnectRsp);
            resultObj.setMaxMsgSize(maxMsgSize);
            mRespHdlr.sendMessage(mRespHdlr.obtainMessage
                    (UimRemoteServerService.EVENT_SERVER_CONNECT_RESPONSE, mSlotId,
                    serial, resultObj));
        }

        @Override
        public void uimRemoteServiceServerDisconnectResponse(int serial) {
            mRespHdlr.sendMessage(mRespHdlr.obtainMessage
                    (UimRemoteServerService.EVENT_SERVER_DISCONNECT_RESPONSE, mSlotId,
                    serial, null));
        }

        @Override
        public void uimRemoteServiceServerApduResponse(int serial, int resultCode,
                java.util.ArrayList<Byte> apduRsp) {
            UimRemoteServerResult resultObj = new UimRemoteServerResult();
            resultObj.setResultCode(resultCode);
            resultObj.setApduRsp(arrayListToPrimitiveArray(apduRsp));
            mRespHdlr.sendMessage(mRespHdlr.obtainMessage
                    (UimRemoteServerService.EVENT_SERVER_APDU_RESPONSE, mSlotId,
                    serial, resultObj));
        }

        @Override
        public void uimRemoteServiceServerTransferAtrResponse(int serial,
                int resultCode, java.util.ArrayList<Byte> atr) {
            UimRemoteServerResult resultObj = new UimRemoteServerResult();
            resultObj.setResultCode(resultCode);
            resultObj.setAtr(arrayListToPrimitiveArray(atr));
            mRespHdlr.sendMessage(mRespHdlr.obtainMessage
                    (UimRemoteServerService.EVENT_SERVER_TRANSFER_ATR_RESPONSE, mSlotId,
                    serial, resultObj));
        }

        @Override
        public void uimRemoteServiceServerPowerResponse(int serial, int resultCode) {
            UimRemoteServerResult resultObj = new UimRemoteServerResult();
            resultObj.setResultCode(resultCode);
            mRespHdlr.sendMessage(mRespHdlr.obtainMessage
                    (UimRemoteServerService.EVENT_SERVER_POWER_RESPONSE, mSlotId,
                    serial, resultObj));
        }

        @Override
        public void uimRemoteServiceServerResetSimResponse(int serial, int resultCode) {
            UimRemoteServerResult resultObj = new UimRemoteServerResult();
            resultObj.setResultCode(resultCode);
            mRespHdlr.sendMessage(mRespHdlr.obtainMessage
                    (UimRemoteServerService.EVENT_SERVER_RESET_SIM_RESPONSE, mSlotId,
                    serial, resultObj));
        }

        @Override
        public void uimRemoteServiceServerTransferCardReaderStatusResponse(int serial,
                int resultCode, int cardReaderStatus) {
            Log.e(LOG_TAG, "uimRemoteServiceServerTransferCardReaderStatusResponse" +
                " Not Supported, dummy function");
        }

        @Override
        public void uimRemoteServiceServerErrorResponse(int serial) {
            Log.e(LOG_TAG, "uimRemoteServiceServerErrorResponse" +
                " Not Supported, dummy function");
        }

        @Override
        public void uimRemoteServiceServerTransferProtocolResponse(int serial,
                int resultCode) {
            Log.e(LOG_TAG, "uimRemoteServiceServerTransferProtocolResponse" +
                " Not Supported, dummy function");
        }
    }

    private final class UimRemoteServerIndication extends
                    IUimRemoteServiceServerIndication.Stub {
        int mSlotId;

        public UimRemoteServerIndication(int slotId) {
            Log.d(LOG_TAG, "[" + slotId + "] Constructor: ");
            mSlotId = slotId;
        }

        @Override
        public void uimRemoteServiceServerDisconnectIndication(int disconnectType) {
            Log.d(LOG_TAG, "uimRemoteServiceServerDisconnectIndication");
            UimRemoteServerResult resultObj = new UimRemoteServerResult();
            resultObj.setDisconnectType(disconnectType);
            mRespHdlr.sendMessage(mRespHdlr.obtainMessage
                    (UimRemoteServerService.EVENT_SERVER_DISCONNECT_INDICATION, mSlotId,
                    0, resultObj));
        }

        @Override
        public void uimRemoteServiceServerStatusIndication(int status) {
            Log.d(LOG_TAG, "uimRemoteServiceServerStatusIndication");
            UimRemoteServerResult resultObj = new UimRemoteServerResult();
            resultObj.setStatus(status);
            mRespHdlr.sendMessage(mRespHdlr.obtainMessage
                    (UimRemoteServerService.EVENT_SERVER_STATUS_INDICATION, mSlotId,
                    0, resultObj));
        }
    }

    @Override
    public void registerResponseHandler(Handler responseHandler) {
        mRespHdlr = responseHandler;
    }

    @Override
    public void uimRemoteServerConnectReq(int token, int maxMessageSize)
            throws RemoteException {
        mInflightRequests.put(token, token);
        mUimRemoteServer.uimRemoteServiceServerConnectReq(token, maxMessageSize);
    }

    @Override
    public void uimRemoteServerDisconnectReq(int token) throws RemoteException {
        mInflightRequests.put(token, token);
        mUimRemoteServer.uimRemoteServiceServerDisconnectReq(token);
    }

    @Override
    public void uimRemoteServerApduReq(int token, byte[] cmd) throws RemoteException {
        mInflightRequests.put(token, token);
        mUimRemoteServer.uimRemoteServiceServerApduReq(token,
                UimRemoteServiceServerApduType.UIM_REMOTE_SERVICE_SERVER_APDU,
                primitiveArrayToArrayList(cmd));
    }

    @Override
    public void uimRemoteServerTransferAtrReq(int token) throws RemoteException {
        mInflightRequests.put(token, token);
        mUimRemoteServer.uimRemoteServiceServerTransferAtrReq(token);
    }

    @Override
    public void uimRemoteServerPowerReq(int token, boolean state) throws RemoteException {
        mInflightRequests.put(token, token);
        mUimRemoteServer.uimRemoteServiceServerPowerReq(token, state);
    }

    @Override
    public void uimRemoteServerResetSimReq(int token) throws RemoteException {
        mInflightRequests.put(token, token);
        mUimRemoteServer.uimRemoteServiceServerResetSimReq(token);
    }

    private void resetServiceAndRequestList() {
        if (mIsDisposed) {
            return;
        }
        mIsDisposed = true;
        try {
            if (mUimRemoteServer != null) {
                mUimRemoteServer.unlinkToDeath(mDeathRecipient);
                mUimRemoteServer = null;
            }
        } catch(RemoteException e) {
            Log.e(LOG_TAG, "ToDestroy: Exception=" + e);
        }
    }

    /* Utility method to convert byte array to ArrayList of Bytes */
    private static ArrayList<Byte> primitiveArrayToArrayList(byte[] arr) {
        if (arr == null) return null;
        ArrayList<Byte> arrayList = new ArrayList<>(arr.length);
        for (byte b : arr) {
            arrayList.add(b);
        }
        return arrayList;
    }
    /* Utility method to convert ArrayList of Bytes to byte array */
    private static byte[] arrayListToPrimitiveArray(ArrayList<Byte> bytes) {
        if (bytes == null) return null;
        byte[] ret = new byte[bytes.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = bytes.get(i);
        }
        return ret;
    }
}
