/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.uimremoteclient;

import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;

/*
 * Default HAL class that is invoked when no IQtiRadioConfig HAL is available.
 * Typical use case for this is when the target does not support telephony/ril.
 */
public class UimRemoteClientNotSupportedHal implements IUimRemoteClientInterface {

    private final String LOG_TAG = "UimRemoteClientNotSupportedHal";

    private void fail() throws RemoteException {
        throw new RemoteException("Radio is not supported");
    }

    @Override
    public void uimRemoteEvent(int token, int event, byte[] atr, int errCode, boolean has_transport,
                    int transport, boolean has_usage, int usage, boolean has_apdu_timeout,
                    int apdu_timeout, boolean has_disable_all_polling, int disable_all_polling,
                    boolean has_poll_timer, int poll_timer) throws RemoteException {
        fail();
    }

    @Override
    public void uimRemoteApdu(int token, int apduStatus, byte[] apduResp) throws RemoteException {
        fail();
    }

    public void registerResponseHandler(Handler responseHandler) {
    }
}
