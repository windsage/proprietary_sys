/*  Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 *  All Rights Reserved.
 *  Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qti.gnssconfig;

import com.qti.gnssconfig.RLConfigData;

oneway interface IGnssConfigCallback
{
    void getRobustLocationConfigCb(in RLConfigData rlConfigData);
    void ntnConfigSignalMaskResponse(boolean isSuccess, int gpsSignalTypeConfigMask);
    void ntnConfigSignalMaskChanged(int gpsSignalTypeConfigMask);
}
