/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.uimremoteclient;

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

import vendor.qti.hardware.radio.uim_remote_client.V1_0.IUimRemoteServiceClient;
import vendor.qti.hardware.radio.uim_remote_client.V1_0.IUimRemoteServiceClientResponse;
import vendor.qti.hardware.radio.uim_remote_client.V1_0.IUimRemoteServiceClientIndication;
import vendor.qti.hardware.radio.uim_remote_client.V1_0.UimRemoteEventReqType;

public class UimRemoteClientHidl implements IUimRemoteClientInterface{

    private final String LOG_TAG = "UimRemoteClientHidl";
    private IUimRemoteServiceClient mUimRemoteClient;
    private int mSlotId;
    private Context mContext;
    private IBinder mBinder;
    private static final int EVENT_REMOTE_CLIENT_SERVICE_DEAD = 1;
    private static final int DEFAULT_REINIT_TIMEOUT_MS = 3000;
    private UimRemoteClientHandler mRemoteHandler;
    private final String mInstanceName;
    private final ServiceNotification mServiceNotification =
                    new ServiceNotification();
    private final AtomicLong mUimRemoteClientCookie = new AtomicLong(0);
    private IUimRemoteServiceClientResponse mUimRemoteClientResp;
    private IUimRemoteServiceClientIndication mUimRemoteClientInd;
    private UimRemoteClientDeathRecipient mDeathRecipient;
    private boolean mIsDisposed = false;
    private int simSlots = 0;
    private static Handler mRespHdlr;
    private final Object mLock = new Object();

    public UimRemoteClientHidl(int slotId, Context context) {
        mSlotId = slotId;
        mContext = context;
        mInstanceName = "uimRemoteClient" + slotId;
        try {
            boolean ret = IServiceManager.getService().registerForNotifications
                    ("vendor.qti.hardware.radio.uim_remote_client@1.0::IUimRemoteServiceClient",
                    mInstanceName, mServiceNotification);
            if (!ret) {
                Log.d(LOG_TAG, "Unable to register service start notification: ret" + ret);
            }
        } catch(RemoteException ex) {
             Log.e(LOG_TAG, "registerForServiceNotifications: exception" + ex);
        }
        mDeathRecipient = new UimRemoteClientDeathRecipient();
        mRemoteHandler = new UimRemoteClientHandler();
    }

    private final class ServiceNotification extends IServiceNotification.Stub {
        @Override
        public void onRegistration(String fqName, String name, boolean preexisting) {
            if (!mInstanceName.equals(name) || mIsDisposed) {
                Log.d(LOG_TAG, "onRegistration: Ignoring.");
                return;
            }
            Log.d(LOG_TAG, "ServiceNotification - onRegistration");
            initUimRemoteClientHidl();
        }
    }

    private synchronized void initUimRemoteClientHidl() {
        Log.d(LOG_TAG, "initUimRemoteClientHidl");
        try {
            vendor.qti.hardware.radio.uim_remote_client.V1_0.
                    IUimRemoteServiceClient uimRemoteClient;
            uimRemoteClient = vendor.qti.hardware.radio.uim_remote_client.V1_0.
                    IUimRemoteServiceClient.getService(mInstanceName);
            if (uimRemoteClient == null) {
                Log.e(LOG_TAG, "initUimRemoteClientHidl: uimRemoteClient == null");
                return;
            }
            uimRemoteClient.linkToDeath(mDeathRecipient,
                                    mUimRemoteClientCookie.incrementAndGet());
            mUimRemoteClientResp = new UimRemoteClientResponse(mSlotId);
            mUimRemoteClientInd = new UimRemoteClientIndication(mSlotId);
            uimRemoteClient.setCallback(mUimRemoteClientResp, mUimRemoteClientInd);
            synchronized (mLock) {
                mUimRemoteClient = uimRemoteClient;
            }
        } catch(Exception e) {
            Log.e(LOG_TAG, "initUimRemoteClientHidl: Exception=" + e);
        }
    }

    final class UimRemoteClientDeathRecipient implements HwBinder.DeathRecipient {
        @Override
        public void serviceDied(long cookie) {
            Log.d(LOG_TAG, "serviceDied");
            resetServiceAndRequestList();
            mRemoteHandler.sendMessageDelayed(mRemoteHandler.obtainMessage(
                        EVENT_REMOTE_CLIENT_SERVICE_DEAD), DEFAULT_REINIT_TIMEOUT_MS);
        }
    }

    public class UimRemoteClientHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_REMOTE_CLIENT_SERVICE_DEAD:
                    Log.d(LOG_TAG, "EVENT_REMOTE_CLIENT_SERVICE_DEAD reinitialize ...");
                    initUimRemoteClientHidl();
                    break;
            }
        }
    }

    private final class UimRemoteClientResponse extends
                    IUimRemoteServiceClientResponse.Stub {
        int mSlotId;

        public UimRemoteClientResponse(int slotId) {
            Log.d(LOG_TAG, "[" + slotId + "] Constructor: ");
            mSlotId = slotId;
        }

        @Override
        public void UimRemoteServiceClientEventResp(int serial, int eventResp) {
            if (mIsDisposed) {
               Log.e(LOG_TAG, "Uim Remote Client Response not exist");
               return;
            }
            mRespHdlr.sendMessage(mRespHdlr.obtainMessage
                    (UimRemoteClientService.EVENT_REMOTE_EVENT_RESPONSE, mSlotId,
                    serial, eventResp));
        }

        @Override
        public void UimRemoteServiceClientApduResp(int serial, int apduResp) {
            if (mIsDisposed) {
               Log.e(LOG_TAG, "Uim Remote Client Response not exist");
               return;
            }
            mRespHdlr.sendMessage(mRespHdlr.obtainMessage
                    (UimRemoteClientService.EVENT_REMOTE_APDU_RESPONSE, mSlotId,
                    serial, apduResp));
        }
    }

    private final class UimRemoteClientIndication extends
                        IUimRemoteServiceClientIndication.Stub {
        int mSlotId;

        public UimRemoteClientIndication(int slotId) {
            Log.d(LOG_TAG, "[" + slotId + "] Constructor: ");
            mSlotId = slotId;
        }

        @Override
        public void UimRemoteServiceClientApduInd(java.util.ArrayList<Byte> apduInd) {
            if (mIsDisposed) {
               Log.e(LOG_TAG, "Uim Remote Client Indication not exist");
               return;
            }
            Log.d(LOG_TAG, "UimRemoteServiceClientApduInd");
            mRespHdlr.sendMessage(mRespHdlr.obtainMessage
                    (UimRemoteClientService.EVENT_REMOTE_APDU_INDICATION, mSlotId,
                    -1, arrayListToPrimitiveArray(apduInd)));
        }

        @Override
        public void UimRemoteServiceClientConnectInd() {
            if (mIsDisposed) {
                Log.e(LOG_TAG, "Uim Remote Client Indication not exist");
                return;
            }
            Log.d(LOG_TAG, "UimRemoteServiceClientConnectInd");
            mRespHdlr.sendMessage(mRespHdlr.obtainMessage
                    (UimRemoteClientService.EVENT_REMOTE_CONNECT_INDICATION, mSlotId,
                    -1, null));
        }

        @Override
        public void UimRemoteServiceClientDisconnectInd() {
            if (mIsDisposed) {
                Log.e(LOG_TAG, "Uim Remote Client Indication not exist");
                return;
            }
            Log.d(LOG_TAG, "UimRemoteServiceClientDisconnectInd");
            mRespHdlr.sendMessage(mRespHdlr.obtainMessage
                    (UimRemoteClientService.EVENT_REMOTE_DISCONNECT_INDICATION, mSlotId,
                    -1, null));
        }

        @Override
        public void UimRemoteServiceClientPowerUpInd(boolean hasTimeOut, int timeOut,
                            boolean hasVoltageClass, int powerUpVoltageClass) {
            if (mIsDisposed) {
                Log.e(LOG_TAG, "Uim Remote Client Indication not exist");
                return;
            }
            Log.d(LOG_TAG, "UimRemoteServiceClientPowerUpInd");
            mRespHdlr.sendMessage(mRespHdlr.obtainMessage
                    (UimRemoteClientService.EVENT_REMOTE_POWERUP_INDICATION, mSlotId,
                    -1, null));
        }

        @Override
        public void UimRemoteServiceClientPowerDownInd(boolean hasPowerDownMode,
                            int powerDownMode) {
            if (mIsDisposed) {
                Log.e(LOG_TAG, "Uim Remote Client Indication not exist");
                return;
            }
            Log.d(LOG_TAG, "UimRemoteServiceClientPowerDownInd");
            mRespHdlr.sendMessage(mRespHdlr.obtainMessage
                    (UimRemoteClientService.EVENT_REMOTE_POWERDOWN_INDICATION, mSlotId,
                    -1, null));
        }

        @Override
        public void UimRemoteServiceClientResetInd() {
            if (mIsDisposed) {
                Log.e(LOG_TAG, "Uim Remote Client Indication not exist");
                return;
            }
            Log.d(LOG_TAG, "UimRemoteServiceClientResetInd");
            mRespHdlr.sendMessage(mRespHdlr.obtainMessage
                    (UimRemoteClientService.EVENT_REMOTE_RESET_INDICATION, mSlotId,
                    -1, null));
        }
    }

    @Override
    public void uimRemoteEvent(int token, int event, byte[] atr, int errCode, boolean has_transport,
                    int transport, boolean has_usage, int usage, boolean has_apdu_timeout,
                    int apdu_timeout, boolean has_disable_all_polling, int disable_all_polling,
                    boolean has_poll_timer, int poll_timer) throws RemoteException {
        Log.d(LOG_TAG, "uimRemoteEvent: token = " + token);
        UimRemoteEventReqType mEventType = new UimRemoteEventReqType();
        mEventType.event = event;
        mEventType.atr.addAll(primitiveArrayToArrayList(atr));
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
            mUimRemoteClient.UimRemoteServiceClientEventReq(token, mEventType);
        } catch(RemoteException ex) {
            Log.e(LOG_TAG, "uimRemoteEvent Failed." + ex);
        }
    }

    @Override
    public void uimRemoteApdu(int token, int apduStatus, byte[] apduResp) throws RemoteException {
        Log.d(LOG_TAG, "uimRemoteApdu: token = " + token);
        try {
            mUimRemoteClient.UimRemoteServiceClientApduReq(token, apduStatus,
                    primitiveArrayToArrayList(apduResp));
        } catch(RemoteException ex) {
            Log.e(LOG_TAG, "uimRemoteApdu Failed." + ex);
        }
    }

    @Override
    public void registerResponseHandler(Handler responseHandler) {
        mRespHdlr = responseHandler;
    }

    private void resetServiceAndRequestList() {
        if (mIsDisposed) {
            return;
        }
        mIsDisposed = true;
        try {
            if (mUimRemoteClient != null) {
                mUimRemoteClient.unlinkToDeath(mDeathRecipient);
                mUimRemoteClient = null;
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
