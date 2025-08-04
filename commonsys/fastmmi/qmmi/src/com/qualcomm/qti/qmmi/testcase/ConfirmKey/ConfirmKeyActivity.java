/*
 * Copyright (c) 2020, 2023, Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.qmmi.testcase.ConfirmKey;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.TextView;

import com.qualcomm.qti.qmmi.R;
import com.qualcomm.qti.qmmi.framework.BaseActivity;
import com.qualcomm.qti.qmmi.utils.LogUtils;
import com.qualcomm.qti.qmmi.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.android.internal.widget.LockPatternUtils;

public class ConfirmKeyActivity extends BaseActivity {

    private HashMap<Integer, String> mKeyMap = new HashMap<Integer, String>();

    {
        mKeyMap.put(KeyEvent.KEYCODE_UNKNOWN, "confirm_key"); // Maybe KEYCODE_ENTER
    }

    /**
     * Can't directly add into TestCase.TestData.
     * When test power/home key, will call BaseActivity.onResume() and will clear TestCase.TestData.
     */
    private Map<String, String> mKeysData = new HashMap<String, String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        for (Integer in : mKeyMap.keySet()) {
            mKeysData.put(mKeyMap.get(in), "not detected");
        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.confirm_key_act;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // TODO Auto-generated method stub
        LogUtils.logi( "dispatchKeyEvent -- keyCode:" + event.getKeyCode());
        TextView keyText = null;
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_UNKNOWN:
                keyText = (TextView) findViewById(R.id.confirm_key);
                mKeysData.put(mKeyMap.get(event.getKeyCode()), "detected");
                break;
        }

        if ( null != keyText) {
            setKeyPass(keyText);
            return true;
        }

        return super.dispatchKeyEvent(event);
    }

    private void setKeyPass(TextView keyText) {
        if (null != keyText) {
            keyText.setBackgroundResource(R.drawable.pass);
        }
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub

        //Write TestCase.TestData
        for (String key : mKeysData.keySet()) {
            mTestCase.addTestData(key, mKeysData.get(key));
            LogUtils.logi( "TestData: " + key + " : " + mKeysData.get(key));
        }

        super.onPause();
    }

    @Override
    protected void onStop() {
        // TODO Auto-generated method stub
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
    }
}
