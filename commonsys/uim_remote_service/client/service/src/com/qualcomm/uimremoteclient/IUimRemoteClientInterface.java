/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.uimremoteclient;

import android.os.Handler;
import android.os.RemoteException;

public interface IUimRemoteClientInterface {

    public void uimRemoteEvent(int token, int event, byte[] atr, int errCode, boolean has_transport,
                    int transport, boolean has_usage, int usage, boolean has_apdu_timeout,
                    int apdu_timeout, boolean has_disable_all_polling, int disable_all_polling,
                    boolean has_poll_timer, int poll_timer) throws RemoteException;

    public void uimRemoteApdu(int token, int apduStatus, byte[] apduResp) throws RemoteException;

    /**
     * registerResponseHandler , registering the Response
     * Handler with AIDL/HIDL classes.
     * IDL classes can send the response/Indication to
     * Service using this Handler.
     */
    public void registerResponseHandler(Handler handler);
}
