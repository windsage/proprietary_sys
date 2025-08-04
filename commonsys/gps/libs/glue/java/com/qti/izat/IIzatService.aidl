/*====*====*====*====*====*====*====*====*====*====*====*====*====*====*====*
  Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
  All rights reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
=============================================================================*/

package com.qti.izat;

import android.os.IBinder;
import com.qti.flp.IFlpService;
import com.qti.flp.ITestService;
import com.qti.geofence.IGeofenceService;
import com.qti.debugreport.IDebugReportService;
import com.qti.wifidbreceiver.IWiFiDBReceiver;
import com.qti.gnssconfig.IGnssConfigService;
import com.qti.wifidbprovider.IWiFiDBProvider;
import com.qti.altitudereceiver.IAltitudeReceiver;
import com.qti.gtp.IGTPService;
import com.qti.wwanadreceiver.IWWANAdReceiver;
import com.qti.preciseposition.IPrecisePositionService;

interface IIzatService {
    IFlpService getFlpService();
    ITestService getTestService();
    IGeofenceService getGeofenceService();
    String getVersion();
    IDebugReportService getDebugReportService();
    IWiFiDBReceiver getWiFiDBReceiver();
    IGnssConfigService getGnssConfigService();
    IWiFiDBProvider getWiFiDBProvider();
    void registerProcessDeath(in IBinder clientDeathListener);
    IAltitudeReceiver getAltitudeReceiver();
    IGTPService getGTPService();
    IWWANAdReceiver getWWANAdReceiver();
    IPrecisePositionService getPrecisePositionService();
}
