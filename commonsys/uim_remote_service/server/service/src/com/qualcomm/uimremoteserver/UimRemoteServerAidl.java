/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.uimremoteserver;

import android.content.Context;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.qualcomm.qti.servicelib.ServiceLib;

import java.util.concurrent.ConcurrentHashMap;

import vendor.qti.hardware.radio.uim_remote_server.UimRemoteServiceServerApduType;

public class UimRemoteServerAidl implements IUimRemoteServerInterface {

    private final String LOG_TAG = "UimRemoteServerAidl";
    private int mSlotId;
    private static Handler mRespHdlr;

    // Synchronization object of HAL interfaces.
    private final Object mHalSync = new Object();
    private IBinder mBinder;
    // The death recepient object which gets notified when
    // IUimRemoteServiceClient service dies.
    private UimRemoteServerDeathRecipient mDeathRecipient;
    private final String AIDL_SERVICE_INSTANCE = "uimRemoteServer";
    private String mServiceInstance;
    private ConcurrentHashMap<Integer, Integer> mInflightRequests =
            new ConcurrentHashMap<Integer, Integer>();

    private vendor.qti.hardware.radio.uim_remote_server.IUimRemoteServiceServer
            mUimRemoteServer;
    private vendor.qti.hardware.radio.uim_remote_server.IUimRemoteServiceServerResponse
            mUimRemoteServerResponseAidl;
    private vendor.qti.hardware.radio.uim_remote_server.IUimRemoteServiceServerIndication
            mUimRemoteServerIndicationAidl;

    public UimRemoteServerAidl(int slotId, Context context) {
        mSlotId = slotId;
        mServiceInstance = AIDL_SERVICE_INSTANCE + (mSlotId);
        mDeathRecipient = new UimRemoteServerDeathRecipient();
        initUimRemoteServer();
    }

    private void initUimRemoteServer() {
        Log.i(LOG_TAG,"initUimRemoteServer");
        ServiceLib serviceLibObj = new ServiceLib();
        mBinder = serviceLibObj.waitForDeclaredService(
                    "vendor.qti.hardware.radio.uim_remote_server." +
                    "IUimRemoteServiceServer/"+mServiceInstance);
        if (mBinder == null) {
            Log.e(LOG_TAG, "initUimRemoteServer failed");
            return;
        }

        vendor.qti.hardware.radio.uim_remote_server.
                IUimRemoteServiceServer uimRemoteServer = vendor.qti.hardware.radio.
                uim_remote_server.IUimRemoteServiceServer.Stub.asInterface(mBinder);
        if(uimRemoteServer == null) {
            Log.e(LOG_TAG,"Get binder for UimRemoteServiceServer StableAIDL failed");
            return;
        }
        Log.i(LOG_TAG,"Get binder for UimRemoteServiceServer StableAIDL is successful");

        try {
            mBinder.linkToDeath(mDeathRecipient, 0 /* Not Used */);
        } catch (android.os.RemoteException ex) {
        }

        synchronized (mHalSync) {
            mUimRemoteServerResponseAidl = new UimRemoteServerResponseAidl(mSlotId);
            mUimRemoteServerIndicationAidl = new UimRemoteServerIndicationAidl(mSlotId);
            try {
                uimRemoteServer.setCallback(
                        mUimRemoteServerResponseAidl, mUimRemoteServerIndicationAidl);
            } catch (android.os.RemoteException ex) {
                Log.e(LOG_TAG, "Failed to call setCallbacks stable AIDL API" + ex);
            }
            mUimRemoteServer = uimRemoteServer;
        }
    }

    /**
     * Class that implements the binder death recipient to be notified when
     * IUimRemoteServiceClient service dies.
     */
    final class UimRemoteServerDeathRecipient implements IBinder.DeathRecipient {
        /**
         * Callback that gets called when the service has died
         */
        @Override
        public void binderDied() {
            Log.e(LOG_TAG, "IUimRemoteServiceClient Died");
            resetHalInterfaces();
            initUimRemoteServer();
        }
    }

    private void resetHalInterfaces() {
        Log.d(LOG_TAG, "resetHalInterfaces: Resetting HAL interfaces.");
        if (mBinder != null) {
            try {
                boolean result = mBinder.unlinkToDeath(mDeathRecipient, 0 /* Not used */);
                mBinder = null;
            } catch (Exception ex) {}
        }
        synchronized (mHalSync) {
            mUimRemoteServer = null;
            mUimRemoteServerResponseAidl = null;
            mUimRemoteServerIndicationAidl = null;
        }
    }

    class UimRemoteServerResponseAidl extends vendor.qti.hardware.radio.
            uim_remote_server.IUimRemoteServiceServerResponse.Stub {

        int mSlotId;

        public UimRemoteServerResponseAidl(int slotId) {
            Log.d(LOG_TAG, "[" + slotId + "] Constructor: ");
            mSlotId = slotId;
        }

        @Override
        public final int getInterfaceVersion() {
            return vendor.qti.hardware.radio.uim_remote_server.
                    IUimRemoteServiceServerResponse.VERSION;
        }

        @Override
        public String getInterfaceHash() {
            return vendor.qti.hardware.radio.uim_remote_server.
                    IUimRemoteServiceServerResponse.HASH;
        }

        @Override
        public void uimRemoteServiceServerConnectResponse(int serial,
                int sapConnectRsp, int maxMsgSize) {
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
                byte[] apduRsp) {
            UimRemoteServerResult resultObj = new UimRemoteServerResult();
            resultObj.setResultCode(resultCode);
            resultObj.setApduRsp(apduRsp);
            mRespHdlr.sendMessage(mRespHdlr.obtainMessage
                    (UimRemoteServerService.EVENT_SERVER_APDU_RESPONSE, mSlotId,
                    serial, resultObj));
        }

        @Override
        public void uimRemoteServiceServerTransferAtrResponse(int serial,
                int resultCode, byte[] atr) {
            UimRemoteServerResult resultObj = new UimRemoteServerResult();
            resultObj.setResultCode(resultCode);
            resultObj.setAtr(atr);
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
            Log.e(LOG_TAG, "uimRemoteServiceServerTransferCardReaderStatusResponse " +
                    "Not Supported, dummy function");
        }

        @Override
        public void uimRemoteServiceServerErrorResponse(int serial) {
            Log.e(LOG_TAG, "uimRemoteServiceServerErrorResponse " +
                    "Not Supported, dummy function");
        }

        @Override
        public void uimRemoteServiceServerTransferProtocolResponse(
                int serial, int resultCode) {
            Log.e(LOG_TAG, "uimRemoteServiceServerTransferProtocolResponse " +
                    "Not Supported, dummy function");
        }
    }

    class UimRemoteServerIndicationAidl extends vendor.qti.hardware.radio.
            uim_remote_server.IUimRemoteServiceServerIndication.Stub {

        int mSlotId;

        public UimRemoteServerIndicationAidl(int slotId) {
            Log.d(LOG_TAG, "[" + slotId + "] Constructor: ");
            mSlotId = slotId;
        }

        @Override
        public final int getInterfaceVersion() {
            return vendor.qti.hardware.radio.uim_remote_server.
                    IUimRemoteServiceServerIndication.VERSION;
        }

        @Override
        public String getInterfaceHash() {
            return vendor.qti.hardware.radio.uim_remote_server.
                    IUimRemoteServiceServerIndication.HASH;
        }

        @Override
        public void uimRemoteServiceServerDisconnectIndication(int disconnectType) {
            Log.d(LOG_TAG, "DisconnectIndication: slotId = " + mSlotId );
            UimRemoteServerResult resultObj = new UimRemoteServerResult();
            resultObj.setDisconnectType(disconnectType);
            mRespHdlr.sendMessage(mRespHdlr.obtainMessage
                    (UimRemoteServerService.EVENT_SERVER_DISCONNECT_INDICATION, mSlotId,
                    0, resultObj));
        }

        @Override
        public void uimRemoteServiceServerStatusIndication(int status) {
            Log.d(LOG_TAG, "StatusIndication: slotId = " + mSlotId );
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
                UimRemoteServiceServerApduType.UIM_REMOTE_SERVICE_SERVER_APDU, cmd);
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
}
