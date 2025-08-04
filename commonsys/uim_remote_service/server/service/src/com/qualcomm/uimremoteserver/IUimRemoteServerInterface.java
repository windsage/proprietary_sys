/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.uimremoteserver;

import android.os.Handler;
import android.os.RemoteException;

public interface IUimRemoteServerInterface {

    public void uimRemoteServerConnectReq(int token, int maxMessageSize)
            throws RemoteException;

    public void uimRemoteServerDisconnectReq(int token) throws RemoteException;

    public void uimRemoteServerApduReq(int token, byte[] cmd) throws RemoteException;

    public void uimRemoteServerTransferAtrReq(int token) throws RemoteException;

    public void uimRemoteServerPowerReq(int token, boolean state) throws RemoteException;

    public void uimRemoteServerResetSimReq(int token) throws RemoteException;

    /**
     * registerResponseHandler , registering the Response
     * Handler with AIDL/HIDL classes.
     * IDL classes can send the response/Indication to
     * Service using this Handler.
     */
    public void registerResponseHandler(Handler handler);
}
