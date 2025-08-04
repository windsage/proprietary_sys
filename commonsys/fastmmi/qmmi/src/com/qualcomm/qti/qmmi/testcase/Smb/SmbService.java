/*
 * Copyright (c) 2017, 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.qmmi.testcase.Smb;

import android.content.Intent;
import android.content.res.Resources;

import java.util.Locale;

import com.qualcomm.qti.qmmi.R;
import com.qualcomm.qti.qmmi.bean.TestCase;
import com.qualcomm.qti.qmmi.framework.BaseService;
import com.qualcomm.qti.qmmi.model.HidlManager;
import com.qualcomm.qti.qmmi.model.AidlManager;
import com.qualcomm.qti.qmmi.utils.LogUtils;

import vendor.qti.hardware.factory.V1_0.FactoryResult; //hidl
//import FactoryResultAidl = vendor.qti.hardware.factory.FactoryResult;

public class SmbService extends BaseService {
    private HidlManager hidlManager = null;
    private AidlManager aidlManager = null;

    public int onStartCommand(Intent intent, int flags, int startId) {
        aidlManager = AidlManager.getInstance();
        if (aidlManager == null) {
            LogUtils.loge("No AidlManager service here");
            hidlManager = HidlManager.getInstance();
            if (hidlManager == null) {
                LogUtils.loge("No HidlManager service here");
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void register() {
    }

    @Override
    public int stop(TestCase testCase) {
        return 0;
    }

    @Override
    public int run(TestCase testCase) {

        if (aidlManager != null) {
            vendor.qti.hardware.factory.FactoryResult resultAidl = aidlManager.getSmbStatus();

            StringBuffer sb = new StringBuffer();
            sb.append(getString(R.string.smb_status));
            if (resultAidl != null && resultAidl.result == 0) {
                testCase.addTestData("smb status", getEnString(R.string.smb_ready));
                sb.append(getString(R.string.smb_ready));
                updateResultForCase(testCase.getName(), TestCase.STATE_PASS);
            }else {
                testCase.addTestData("smb status", getEnString(R.string.smb_not_ready));
                sb.append(getString(R.string.smb_not_ready));
                updateResultForCase(testCase.getName(), TestCase.STATE_FAIL);
            }

            updateView(testCase.getName(), sb.toString());
        } else if(hidlManager != null) {
            FactoryResult resultHidl = hidlManager.getSmbStatus();

            StringBuffer sb = new StringBuffer();
            sb.append(getString(R.string.smb_status));
            if (resultHidl != null && resultHidl.result == 0) {
                testCase.addTestData("smb status", getEnString(R.string.smb_ready));
                sb.append(getString(R.string.smb_ready));
                updateResultForCase(testCase.getName(), TestCase.STATE_PASS);
            }else {
                testCase.addTestData("smb status", getEnString(R.string.smb_not_ready));
                sb.append(getString(R.string.smb_not_ready));
                updateResultForCase(testCase.getName(), TestCase.STATE_FAIL);
            }

            updateView(testCase.getName(), sb.toString());
        }
        return 0;
    }

    private String getEnString(int result) {
        Resources currentRes = this.getResources();
        android.content.res.AssetManager asserts = currentRes.getAssets();
        android.util.DisplayMetrics metrics = currentRes.getDisplayMetrics();
        android.content.res.Configuration config = new android.content.res.Configuration(currentRes.getConfiguration());
        config.locale = Locale.ENGLISH;
        Resources enRes = this.createConfigurationContext(config).getResources();
        return enRes.getString(result);
    }
}
