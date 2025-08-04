/**
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.dcf.client.service.screensharing;

interface IActionListener {

    void onSuccess();

    void onFailure(int reasonCode);
}