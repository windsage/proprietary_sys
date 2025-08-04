/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package org.codeaurora.ims;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;

import com.qualcomm.ims.utils.Log;

import java.util.List;

import org.codeaurora.ims.ImsArManager;
import org.codeaurora.ims.internal.IImsArListener;

public class ImsArControllerImpl extends ImsArControllerBase {
    private ImsServiceSub mServiceSub;
    private Context mContext;
    private volatile IImsArListener mListener;

    private final IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            onClientDeath();
        }
    };

    public ImsArControllerImpl(ImsServiceSub serviceSub, Context context) {
        super(context.getMainExecutor(), context);
        mServiceSub = serviceSub;
        mContext = context;
    }

    //AR Call Client Process Death
    private void onClientDeath() {
        mListener = null;
    }

    @Override
    public void onSetListener(IImsArListener listener)
            throws RemoteException {
        ImsCallSessionImpl session = getArSession();
        if (session == null) {
            Log.d(this, "onSetArListener: no call is available");
            return;
        }

        IImsArListener intf = mListener;
        if (intf != null) {
            IBinder binder = intf.asBinder();
            binder.unlinkToDeath(mDeathRecipient, 0);
        }

        //Client can set a NULL listener.
        if (listener != null) {
            IBinder binder = listener.asBinder();
            binder.linkToDeath(mDeathRecipient, 0);
        } else {
            Log.w(this, "Trying to set a NULL listener.");
        }
        mListener = listener;

        session.setArListener(listener);
    }

    @Override
    public void onEnableArMode(String cameraId) {
        ImsCallSessionImpl session = getArSession();
        if (session == null) {
            Log.d(this, "enableArMode: no call is available");
            return;
        }
        session.enableArMode(cameraId);
    }

    @Override
    public void onSetLocalRenderingDelay(int delay) {
        ImsCallSessionImpl session = getArSession();
        if (session == null) {
            Log.d(this, "onSetLocalRenderingDelay no call is available");
            return;
        }
        session.setLocalRenderingDelay(delay);
    }

    private ImsCallSessionImpl getArSession() {
        List<ImsCallSessionImpl> sessionList =
            mServiceSub.getCallSessionByState(DriverCallIms.State.ACTIVE);
        if (sessionList.isEmpty()) {
            Log.d(this, "getArSession no call is available");
            return null;
        }
        return sessionList.get(0);
    }
}
