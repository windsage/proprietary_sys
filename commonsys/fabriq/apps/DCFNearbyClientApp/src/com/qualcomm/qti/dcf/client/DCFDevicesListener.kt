/**
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.dcf.client

interface DCFDevicesListener {

    /**
     *  this callback will run on Executor, not main-thread.
     */
    fun onDevicesChanged(devices: List<DCFDevice>)
}