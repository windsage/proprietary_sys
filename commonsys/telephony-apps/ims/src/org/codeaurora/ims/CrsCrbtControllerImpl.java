/*
 * Copyright (c) 2021, 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package org.codeaurora.ims;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;

import com.qualcomm.ims.utils.Log;

import java.util.HashMap;
import java.util.List;

import org.codeaurora.ims.CrsCrbtManager;
import org.codeaurora.ims.internal.ICrsCrbtListener;

public class CrsCrbtControllerImpl extends CrsCrbtControllerBase {
    private ImsServiceSub mServiceSub;
    private Context mContext;
    private final HashMap<IBinder, CrsCrbtDeathRecipient> mClients =
        new HashMap<IBinder, CrsCrbtDeathRecipient>();

    private final class CrsCrbtDeathRecipient implements IBinder.DeathRecipient {
        ICrsCrbtListener mListener;
        ImsCallSessionImpl mSession;
        public CrsCrbtDeathRecipient(ICrsCrbtListener listener, ImsCallSessionImpl session) {
            mListener = listener;
            mSession = session;
        }
        public void linkToDeath() throws RemoteException {
            mListener.asBinder().linkToDeath(this, 0);
        }

        public void close() {
            mListener.asBinder().unlinkToDeath(this, 0);
            synchronized (mClients) {
                mClients.remove(mListener.asBinder());
            }
            if (mSession != null) {
                mSession.setCrsCrbtListener(null);
            }
            mListener = null;
            mSession = null;
        }
        @Override
        public void binderDied() {
            Log.d(this, "Client died: " + this);
            close();
        }
        @Override
        public String toString() {
            return new StringBuilder(128)
                .append("Client{")
                .append(",ICrsCrbtListener:")
                .append(mListener)
                .append(",ImsCallSessionImpl:")
                .append(mSession)
                .append("}")
                .toString();
        };

    }

    public CrsCrbtControllerImpl(ImsServiceSub serviceSub, Context context) {
        super(context.getMainExecutor(), context);
        mServiceSub = serviceSub;
        mContext = context;
    }

    @Override
    public void onSetCrsCrbtListener(ICrsCrbtListener listener)
            throws RemoteException {
        Log.d(this, "onSetCrsCrbtListener");

        if (listener == null) {
            Log.w(this, "onSetCrsCrbtListener : listener is null");
            return;
        }

        ImsCallSessionImpl session = getIncomingOrOutgoingCallSession();
        if (session == null ) {
            Log.d(this, "onSetCrsCrbtListener : no incoming/outgoing call is available");
            return;
        }
        synchronized (mClients) {
            CrsCrbtDeathRecipient client = mClients.get(listener.asBinder());
            if (client != null) {
                client.close();
                mClients.remove(client);
            }
            client = new CrsCrbtDeathRecipient(listener, session);
            client.linkToDeath();
            session.setCrsCrbtListener(listener);
            mClients.put(listener.asBinder(), client);
        }
    }

    @Override
    public void onRemoveCrsCrbtListener(ICrsCrbtListener listener) {
        if (listener == null) {
            Log.w(this, "onRemoveCrsCrbtListener : listener is null");
            return;
        }
        synchronized (mClients) {
            CrsCrbtDeathRecipient client = mClients.get(listener.asBinder());
            if (client != null) {
                client.close();
                mClients.remove(client);
            }
        }
    }

    @Override
    public void onSendSipDtmf(String requestCode) {
        ImsCallSessionImpl session = getIncomingOrOutgoingCallSession();
        if (session == null) {
            Log.d(this, "onSendSipDtmf : no incoming/outgoing call is available");
            return;
        }
        session.sendSipDtmf(requestCode);
    }

    @Override
    public boolean onIsPreparatorySession(String callId) {
        ImsCallSessionImpl session = getIncomingOrOutgoingCallSession();
        return (session == null) ?
            false : session.isPreparatorySession(callId);
    }

    private ImsCallSessionImpl getIncomingOrOutgoingCallSession() {
        List<ImsCallSessionImpl> sessionList =
                mServiceSub.getCallSessionByState(DriverCallIms.State.INCOMING);

        if (sessionList.isEmpty()) {
            sessionList =  mServiceSub.getCallSessionByState(DriverCallIms.State.DIALING);
        }

        if (sessionList.isEmpty()) {
            sessionList =  mServiceSub.getCallSessionByState(DriverCallIms.State.ALERTING);
        }
        return sessionList.isEmpty() ? null : sessionList.get(0);
    }
}
