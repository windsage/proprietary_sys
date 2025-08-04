/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.uimremoteclient;

import android.content.Context;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import com.qualcomm.qti.servicelib.ServiceLib;

import vendor.qti.hardware.radio.uim_remote_client.UimRemoteClientApduStatus;
import vendor.qti.hardware.radio.uim_remote_client.UimRemoteEventReqType;
import vendor.qti.hardware.radio.uim_remote_client.UimRemoteClientCardInitStatusType;

public class UimRemoteClientAidl implements IUimRemoteClientInterface {

    private final String LOG_TAG = "UimRemoteClientAidl";
    private int mSlotId;
    private Context mContext;

    // Synchronization object of HAL interfaces.
    private final Object mHalSync = new Object();
    private IBinder mBinder;
    // The death recepient object which gets notified when
    // IUimRemoteServiceClient service dies.
    private UimRemoteClientDeathRecipient mDeathRecipient;
    private final String AIDL_SERVICE_INSTANCE = "uimRemoteClient";
    private String mServiceInstance;
    private int simSlots = 0;
    private static Handler mRespHdlr;

    private vendor.qti.hardware.radio.uim_remote_client.IUimRemoteServiceClient
            mUimRemoteClient;
    private vendor.qti.hardware.radio.uim_remote_client.IUimRemoteServiceClientResponse
            mUimRemoteClientResponseAidl;
    private vendor.qti.hardware.radio.uim_remote_client.IUimRemoteServiceClientIndication
            mUimRemoteClientIndicationAidl;

    public UimRemoteClientAidl(int slotId, Context context) {
        mContext = context;
        mSlotId = slotId;
        mServiceInstance = AIDL_SERVICE_INSTANCE + (mSlotId);
        mDeathRecipient = new UimRemoteClientDeathRecipient();
        initUimRemoteClient();
    }

    private void initUimRemoteClient() {
        Log.i(LOG_TAG,"initUimRemoteClient");
        ServiceLib serviceLibObj = new ServiceLib();
        mBinder = serviceLibObj.waitForDeclaredService(
                        "vendor.qti.hardware.radio.uim_remote_client."
                        + "IUimRemoteServiceClient/" + mServiceInstance);
        if (mBinder == null) {
            Log.e(LOG_TAG, "initUimRemoteClient failed");
            return;
        }

        vendor.qti.hardware.radio.uim_remote_client.IUimRemoteServiceClient
                uimRemoteClient = vendor.qti.hardware.radio.uim_remote_client.
                IUimRemoteServiceClient.Stub.asInterface(mBinder);
        if(uimRemoteClient == null) {
            Log.e(LOG_TAG,"Get binder for UimRemoteServiceClient StableAIDL failed");
            return;
        }
        Log.i(LOG_TAG,"Get binder for UimRemoteServiceClient StableAIDL is successful");

        try {
            mBinder.linkToDeath(mDeathRecipient, 0 /* Not Used */);
        } catch (RemoteException ex) {
        }

        synchronized (mHalSync) {
            mUimRemoteClientResponseAidl = new UimRemoteClientResponseAidl();
            mUimRemoteClientIndicationAidl = new UimRemoteClientIndicationAidl();
            try {
                uimRemoteClient.setCallback(mUimRemoteClientResponseAidl,
                        mUimRemoteClientIndicationAidl);
            } catch (RemoteException ex) {
                Log.e(LOG_TAG, "Failed to call setCallbacks stable AIDL API" + ex);
            }
            mUimRemoteClient = uimRemoteClient;
        }
    }

    /**
     * Class that implements the binder death recipient to be notified when
     * IUimRemoteServiceClient service dies.
     */
    final class UimRemoteClientDeathRecipient implements IBinder.DeathRecipient {
        /**
         * Callback that gets called when the service has died
         */
        @Override
        public void binderDied() {
            Log.e(LOG_TAG, "IUimRemoteServiceClient Died");
            resetHalInterfaces();
            initUimRemoteClient();
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
            mUimRemoteClient = null;
            mUimRemoteClientResponseAidl = null;
            mUimRemoteClientIndicationAidl = null;
        }
    }

    class UimRemoteClientResponseAidl extends vendor.qti.hardware.radio.
            uim_remote_client.IUimRemoteServiceClientResponse.Stub {
        @Override
        public final int getInterfaceVersion() {
            return vendor.qti.hardware.radio.uim_remote_client.
                    IUimRemoteServiceClientResponse.VERSION;
        }

        @Override
        public String getInterfaceHash() {
            return vendor.qti.hardware.radio.uim_remote_client.
                    IUimRemoteServiceClientResponse.HASH;
        }

        @Override
        public void uimRemoteServiceClientEventResp(int serial, int eventResp) {
            Log.d(LOG_TAG, "uimRemoteServiceClientEventResp: slotId=" + mSlotId +
                    "serial = " + serial + " eventResp = " + eventResp);
            mRespHdlr.sendMessage(mRespHdlr.obtainMessage
                    (UimRemoteClientService.EVENT_REMOTE_EVENT_RESPONSE, mSlotId,
                    serial, eventResp));
        }

        @Override
        public void uimRemoteServiceClientApduResp(int serial, int apduResp) {
            Log.d(LOG_TAG, "uimRemoteServiceClientApduResp: slotId=" + mSlotId +
                    "serial = " + serial + " apduResp = " + apduResp);
            mRespHdlr.sendMessage(mRespHdlr.obtainMessage
                    (UimRemoteClientService.EVENT_REMOTE_APDU_RESPONSE, mSlotId,
                    serial, apduResp));
        }
    }

    class UimRemoteClientIndicationAidl extends vendor.qti.hardware.radio.
            uim_remote_client.IUimRemoteServiceClientIndication.Stub {
        @Override
        public final int getInterfaceVersion() {
            return vendor.qti.hardware.radio.uim_remote_client.
                    IUimRemoteServiceClientIndication.VERSION;
        }

        @Override
        public String getInterfaceHash() {
            return vendor.qti.hardware.radio.uim_remote_client.
                    IUimRemoteServiceClientIndication.HASH;
        }

        @Override
        public void uimRemoteServiceClientApduInd(byte[] apduInd) {
            Log.d(LOG_TAG, "uimRemoteApduIndication: slotId = " + mSlotId +
                    "apduInd = " + apduInd);
            mRespHdlr.sendMessage(mRespHdlr.obtainMessage
                    (UimRemoteClientService.EVENT_REMOTE_APDU_INDICATION, mSlotId,
                    -1, apduInd));
        }

        @Override
        public void uimRemoteServiceClientCardInitStatusInd(
                UimRemoteClientCardInitStatusType cardInitStatusInd) {
            Log.d(LOG_TAG, "uimRemoteServiceClientCardInitStatusInd: " +
                    "cardInitStatusInd = " + cardInitStatusInd );
        }

        @Override
        public void uimRemoteServiceClientConnectInd() {
            Log.d(LOG_TAG, "uimRemoteServiceClientConnectInd: slotId = " + mSlotId );
            mRespHdlr.sendMessage(mRespHdlr.obtainMessage
                    (UimRemoteClientService.EVENT_REMOTE_CONNECT_INDICATION, mSlotId,
                    -1, null));
        }

        @Override
        public void uimRemoteServiceClientDisconnectInd() {
            Log.d(LOG_TAG, "uimRemoteServiceClientDisconnectInd: slotId = " + mSlotId );
            mRespHdlr.sendMessage(mRespHdlr.obtainMessage
                    (UimRemoteClientService.EVENT_REMOTE_DISCONNECT_INDICATION, mSlotId,
                    -1, null));
        }

        @Override
        public void uimRemoteServiceClientPowerUpInd(boolean hasTimeOut, int timeOut,
                                    boolean hasVoltageClass, int powerUpVoltageClass) {
            Log.d(LOG_TAG, "uimRemoteServiceClientPowerUpInd: slotId = " + mSlotId );
            mRespHdlr.sendMessage(mRespHdlr.obtainMessage
                    (UimRemoteClientService.EVENT_REMOTE_POWERUP_INDICATION, mSlotId,
                    -1, null));
        }

        @Override
        public void uimRemoteServiceClientPowerDownInd(boolean hasPowerDownMode,
                                    int powerDownMode) {
            Log.d(LOG_TAG, "uimRemoteServiceClientPowerDownInd: slotId = " + mSlotId );
            mRespHdlr.sendMessage(mRespHdlr.obtainMessage
                    (UimRemoteClientService.EVENT_REMOTE_POWERDOWN_INDICATION, mSlotId,
                    -1, null));
        }

        @Override
        public void uimRemoteServiceClientResetInd() {
            Log.d(LOG_TAG, "uimRemoteServiceClientResetInd: slotId = " + mSlotId );
            mRespHdlr.sendMessage(mRespHdlr.obtainMessage
                    (UimRemoteClientService.EVENT_REMOTE_RESET_INDICATION, mSlotId,
                    -1, null));
        }

        @Override
        public void uimRemoteServiceClientServiceInd(boolean status) {
            Log.d(LOG_TAG, "uimRemoteServiceClientServiceInd: slotId = " + mSlotId );
        }
    }

    @Override
    public void uimRemoteEvent(int token, int event, byte[] atr, int errCode,
                    boolean has_transport, int transport, boolean has_usage, int usage,
                    boolean has_apdu_timeout,int apdu_timeout,
                    boolean has_disable_all_polling, int disable_all_polling,
                    boolean has_poll_timer, int poll_timer) throws RemoteException {
        Log.d(LOG_TAG, "uimRemoteEvent: token = " + token);
        UimRemoteEventReqType mEventType = new UimRemoteEventReqType();
        mEventType.event = event;
        mEventType.atr = atr;
        mEventType.errorCode = errCode;
        mEventType.has_transport = has_transport;
        mEventType.transport = transport;
        mEventType.has_usage = has_usage;
        mEventType.usage = usage;
        mEventType.has_apdu_timeout = has_apdu_timeout;
        mEventType.apduTimeout = apdu_timeout;
        mEventType.has_disable_all_polling = has_disable_all_polling;
        mEventType.disableAllPolling = disable_all_polling;
        mEventType.has_poll_timer = has_poll_timer;
        mEventType.pollTimer = poll_timer;

        try {
            mUimRemoteClient.uimRemoteServiceClientEventReq(token, mEventType);
        } catch(RemoteException ex) {
            Log.e(LOG_TAG, "uimRemoteEvent Failed." + ex);
        }
    }

    @Override
    public void uimRemoteApdu(int token, int apduStatus, byte[] apduResp)
            throws RemoteException {
        Log.d(LOG_TAG, "uimRemoteApdu: token = " + token);
        try {
            mUimRemoteClient.uimRemoteServiceClientApduReq(token, apduStatus, apduResp);
        } catch(RemoteException ex) {
            Log.e(LOG_TAG, "uimRemoteApdu Failed." + ex);
        }
    }

    @Override
    public void registerResponseHandler(Handler responseHandler) {
        mRespHdlr = responseHandler;
    }
}
