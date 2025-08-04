/**
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.dcf.client.service;

interface IDeviceInfoService {

    String getBleAddress();

    String getWifiFactoryAddress();
}