/**
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.dcf.client

import android.app.Application
import com.qualcomm.qti.dcf.client.contextsync.ContextSyncController
import com.qualcomm.qti.dcf.client.screensharing.ScreenSharingController

const val TAG = "DCFClientApp"

class DCFClientApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(DCFScanManager.activityLifecycleCallbacks)

        UserSettings.init(applicationContext)
        readCredentialInfo(this)
        DCFScanManager.init(applicationContext)
        DCFAdvertiseManager.init(applicationContext)

        ContextSyncController.getInstance().init(applicationContext)
        ScreenSharingController.getInstance().init(applicationContext)
    }
}