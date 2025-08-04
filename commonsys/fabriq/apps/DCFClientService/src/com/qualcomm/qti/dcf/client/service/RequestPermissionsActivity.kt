/**
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.dcf.client.service

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager

class RequestPermissionsActivity: Activity() {

    companion object {
        private const val REQUEST_CODE_PERMISSION = 1
        private const val EXTRA_KEY_REQUIRE_PERMISSIONS = "extra_key_require_permissions"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

        requestPermissions(intent.getStringArrayExtra(EXTRA_KEY_REQUIRE_PERMISSIONS)!!,
            REQUEST_CODE_PERMISSION)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSION){
            for (grantResult in grantResults){
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    setResultAndFinish(RESULT_CANCELED)
                    return
                }
            }
            setResultAndFinish(RESULT_OK)
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun setResultAndFinish(resultCode: Int) {
        setResult(resultCode)
        finish()
    }
}