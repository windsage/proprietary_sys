/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.quicinc.voice.assist.sdk.configuration;

import android.os.Bundle;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;

import com.quicinc.voice.activation.aidl.IResultCallback;
import com.quicinc.voice.activation.aidl.IWakewordSettingsService;
import com.quicinc.voice.assist.sdk.utility.Constants;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A package class that implement the basic operation get/set parameters for settings
 */
public class WakewordSettingsManager {
    private static final int CONNECT_TIMEOUT_SEC = 3;
    public static final String TAG = WakewordSettingsManager.class.getSimpleName();

    public static Bundle getParams(ConfigurationConnector connector, Bundle bundle) {
        IWakewordSettingsService service = checkAndGetSettingsService(connector);
        if (service == null) {
            return createFailureResponse("service is not connected");
        }
        AtomicReference<Bundle> result = new AtomicReference<>(new Bundle());
        CountDownLatch countDownLatch = new CountDownLatch(1);
        try {
            service.getParams(bundle, new IResultCallback.Stub() {
                @Override
                public void onSuccess(Bundle returnValues) {
                    result.set(returnValues);
                    countDownLatch.countDown();
                }

                @Override
                public void onFailure(Bundle params) {
                    result.set(params);
                    countDownLatch.countDown();
                }
            });
        } catch (Exception e) {
            countDownLatch.countDown();
            Log.d(TAG,
                    "call getParams error, " + e.getLocalizedMessage());
        }
        boolean returned = false;
        try {
            returned = countDownLatch.await(CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG,
                    "CountDownLatch await Interrupted error when getParams, " +
                            e.getLocalizedMessage());
        }
        return returned? result.get(): createFailureResponse( "getParams timeout");
    }

    private static Bundle createFailureResponse(String msg) {
        Bundle response = new Bundle();
        response.putInt(Constants.KEY_ERROR_CODE, -1);
        response.putString(Constants.KEY_ERROR_MESSAGE, msg);
        return response;
    }

    public static boolean setParams(ConfigurationConnector connector, Bundle params) {
        IWakewordSettingsService service = checkAndGetSettingsService(connector);
        if (service == null || params == null || params.isEmpty()) return false;

        CountDownLatch countDownLatch = new CountDownLatch(1);
        AtomicBoolean atomicBoolean = new AtomicBoolean(false);
        try {
            service.setParams(params, new IResultCallback.Stub() {
                @Override
                public void onSuccess(Bundle returnValues) {
                    atomicBoolean.set(true);
                    countDownLatch.countDown();
                }

                @Override
                public void onFailure(Bundle params) {
                    atomicBoolean.set(false);
                    countDownLatch.countDown();

                }
            });
        } catch (Exception e) {
            countDownLatch.countDown();
            Log.e(TAG,
                    "call setParams error, " + e.getLocalizedMessage());
        }

        boolean returned = false;
        try {
            returned = countDownLatch.await(CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG,
                    "CountDownLatch await Interrupted error when setParams, " +
                            e.getLocalizedMessage());
        }
        return returned && atomicBoolean.get();
    }

    private static IWakewordSettingsService checkAndGetSettingsService(
            ConfigurationConnector connector) {
        if (connector != null) return connector.getSettingsService();
        return null;
    }
}
