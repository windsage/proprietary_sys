/* ======================================================================
*  Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
*  All Rights Reserved.
*  Confidential and Proprietary - Qualcomm Technologies, Inc.
*  ====================================================================*/

package com.qti.wwanadreceiver;

import com.qti.wwanadreceiver.IWWANAdRequestListener;

interface IWWANAdReceiver {

    boolean registerRequestListener(in IWWANAdRequestListener callback);

    void pushWWANAssistanceData(int requestId, boolean status, int respType, in byte[] respPayload);

    void removeResponseListener(in IWWANAdRequestListener callback);
}
