/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.uimremoteserver;

import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;

/*
 * Default HAL class that is invoked when no IUimRemoteServiceServer HAL is available.
 * Typical use case for this is when the target does not support telephony/ril.
 */
public class UimRemoteServerNotSupportedHal implements IUimRemoteServerInterface {

    private final String LOG_TAG = "UimRemoteServerNotSupportedHal";

    private void fail() throws RemoteException {
        throw new RemoteException("Radio is not supported");
    }

    public void registerResponseHandler(Handler responseHandler) {
    }

    public void uimRemoteServerConnectReq(int token, int maxMessageSize)
            throws RemoteException {
        fail();
    }

    public void uimRemoteServerDisconnectReq(int token) throws RemoteException {
        fail();
    }

    public void uimRemoteServerApduReq(int token, byte[] cmd) throws RemoteException {
        fail();
    }

    public void uimRemoteServerTransferAtrReq(int token) throws RemoteException {
        fail();
    }

    public void uimRemoteServerPowerReq(int token, boolean state) throws RemoteException {
        fail();
    }

    public void uimRemoteServerResetSimReq(int token) throws RemoteException {
        fail();
    }
}
