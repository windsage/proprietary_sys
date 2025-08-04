/* ======================================================================
*  Copyright (c) 2018, 2020 Qualcomm Technologies, Inc.
*  All Rights Reserved.
*  Confidential and Proprietary - Qualcomm Technologies, Inc.
*  ====================================================================*/

package com.qti.wifidbprovider;

import com.qti.wifidbprovider.IWiFiDBProviderResponseListener;
import android.app.PendingIntent;

interface IWiFiDBProvider {

    boolean registerResponseListener(in IWiFiDBProviderResponseListener callback,
            in PendingIntent notifyIntent);

    void requestAPObsLocData();

    void removeResponseListener(in IWiFiDBProviderResponseListener callback);
}
