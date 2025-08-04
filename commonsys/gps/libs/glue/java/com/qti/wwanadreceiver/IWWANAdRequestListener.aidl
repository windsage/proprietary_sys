/* ======================================================================
*  Copyright (c) 2018 Qualcomm Technologies, Inc.
*  All Rights Reserved.
*  Confidential and Proprietary - Qualcomm Technologies, Inc.
*  ====================================================================*/

package com.qti.wwanadreceiver;

oneway interface IWWANAdRequestListener {
    void onWWANAdRequest(in int requestId, in byte[] reqPayload);
}
