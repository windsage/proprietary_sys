 /**
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qcrilmsgtunnel;

import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import vendor.qti.hardware.radio.qcrilhook.IQtiOemHook;
import vendor.qti.hardware.radio.qcrilhook.IQtiOemHookIndication;
import vendor.qti.hardware.radio.qcrilhook.IQtiOemHookResponse;

import static org.codeaurora.telephony.utils.RILConstants.RADIO_NOT_AVAILABLE;

class QtiOemHookAidl {

    private static String TAG = "QtiOemHookAidl";
    private QcrilOemhookMsgTunnel mQcrilOemhookMsgTunnel;
    private QtiOemHookAidlDeathRecipient mAidlDeathRecipient;
    private int mInstanceId;
    private IBinder mBinder;
    private Object mOemHookLock = new Object();
    final Object mLock = new Object();

    IQtiOemHook mQtiOemHook;
    private IQtiOemHookIndication mQtiOemHookIndicationAidl;
    private IQtiOemHookResponse mQtiOemHookResponseAidl;

    public QtiOemHookAidl(QcrilOemhookMsgTunnel qcrilOemhookMsgTunnel, Integer instanceId) {
        mQcrilOemhookMsgTunnel = qcrilOemhookMsgTunnel;
        mInstanceId = instanceId;
        mAidlDeathRecipient = new QtiOemHookAidlDeathRecipient();
    }

    /**
     * Class that implements the binder death recipient to be notified when
     * IQtiOemHook service dies.
     */
    final class QtiOemHookAidlDeathRecipient implements IBinder.DeathRecipient {
        /**
         * Callback that gets called when the service has died
         */
        @Override
        public void binderDied() {
            Log.e(TAG, "Oemhook AIDL service died");
            resetAidlServiceAndRequestList();
            initOemHookAidl();
        }
    }

    /* oemHook Aidl Service */
    public void initOemHookAidl() {
        mBinder = Binder.allowBlocking(
                ServiceManager.waitForDeclaredService(
                QcrilOemhookMsgTunnel.OEMHOOK_AIDL_SERVICE_NAME + mInstanceId));
        if (mBinder == null) {
            Log.e(TAG, "initOemHook failed");
            return;
        }
        mQtiOemHook = IQtiOemHook.Stub.asInterface(mBinder);
        if (mQtiOemHook == null) {
            Log.e(TAG,"Get binder for oemHook StableAIDL failed");
            return;
        }
        Log.d(TAG,"Get binder for oemHook StableAIDL is successful");

        try {
            mBinder.linkToDeath(mAidlDeathRecipient, 0 /* Not Used */);
        } catch (RemoteException ex) {
            Log.e(TAG, "initOemHookAidl: exception" + ex);
        }

        synchronized (mOemHookLock) {
            mQtiOemHookIndicationAidl = new QtiOemHookIndicationAidl();
            mQtiOemHookResponseAidl = new QtiOemHookResponseAidl();
            try {
                mQtiOemHook.setCallback(mQtiOemHookResponseAidl, mQtiOemHookIndicationAidl);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call setCallbacks stable AIDL API" + ex);
            }
        }
    }

    class QtiOemHookResponseAidl extends IQtiOemHookResponse.Stub {

        @Override
        public final int getInterfaceVersion() {
            return IQtiOemHookResponse.VERSION;
        }

        @Override
        public String getInterfaceHash() {
            return IQtiOemHookResponse.HASH;
        }

        @Override
        public void oemHookRawResponse(int serial, int errorCode, byte[] data) {
            Log.i(TAG, "oemHookRawResponse: serial no: " + serial + " error: "
                    + errorCode + "length=" + data.length);
            mQcrilOemhookMsgTunnel.processOemHookSendResponse(serial, errorCode, data);
            mQcrilOemhookMsgTunnel.releaseWakeLockIfDone();
        }

    }

    class QtiOemHookIndicationAidl extends
        vendor.qti.hardware.radio.qcrilhook.IQtiOemHookIndication.Stub {

        @Override
        public final int getInterfaceVersion() {
            return vendor.qti.hardware.radio.qcrilhook.IQtiOemHookIndication.VERSION;
        }

        @Override
        public String getInterfaceHash() {
            return vendor.qti.hardware.radio.qcrilhook.IQtiOemHookIndication.HASH;
        }

        @Override
        public void oemHookRawIndication(byte[] data) {
            Log.d(TAG,"oemHookRawIndication length=" + data.length);
            mQcrilOemhookMsgTunnel.processOemHookSendIndication(data);
        }
    }

    private void resetAidlServiceAndRequestList() {
        synchronized(mLock) {
            if (mBinder != null) {
                try {
                    boolean result = mBinder.unlinkToDeath(mAidlDeathRecipient, 0 /* Not used */);
                    mBinder = null;
                } catch (Exception ex) {}
            }
            mQtiOemHook = null;
            mQtiOemHookResponseAidl = null;
            mQtiOemHookIndicationAidl = null;
        }
        mQcrilOemhookMsgTunnel.clearRequestsList(RADIO_NOT_AVAILABLE);
        mQcrilOemhookMsgTunnel.releaseWakeLockIfDone();
    }
}