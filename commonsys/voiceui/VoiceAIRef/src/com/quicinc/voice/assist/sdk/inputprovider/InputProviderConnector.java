/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.quicinc.voice.assist.sdk.inputprovider;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.quicinc.voiceassistant.reference.data.Settings;
import com.quicinc.voice.activation.aidl.IInputProviderService;
import com.quicinc.voice.activation.aidl.IInputReceiverCallback;
import com.quicinc.voice.activation.aidl.IResultCallback;
import com.quicinc.voice.assist.sdk.utility.Constants;
import com.quicinc.voiceassistant.reference.views.ASRViewModel;

import java.lang.ref.WeakReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class InputProviderConnector implements ServiceConnection {
    private final static String TAG = InputProviderConnector.class.getSimpleName();
    public final static String INPUT_PROVIDER_SERVICE_NAME =
            "com.quicinc.voice.activation.llm.InputProviderService";
    private final Intent mServiceIntent = new Intent()
            .setClassName(Constants.QVA_PACKAGE_NAME, INPUT_PROVIDER_SERVICE_NAME);

    private Context mContext;
    private IBinder mIntentIBinder;

    private Executor mExecutor;

    private Handler myHandler;

    private volatile boolean isBound;
    private WeakReference<Runnable> mRunOnConnect;

    private CountDownLatch mCountDownLatch;

    private final IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() {

        @Override
        public void binderDied() {
            if (mIntentIBinder != null) {
                mIntentIBinder.unlinkToDeath(mDeathRecipient, 0);
            }
            Log.d(TAG, "binderDied, try to rebind service");
            ASRViewModel asrViewModel = new ViewModelProvider(
                    (AppCompatActivity) mContext).get(ASRViewModel.class);
            if (asrViewModel != null && asrViewModel.getASRListening().getValue()) {
                myHandler.post(() -> {
                    asrViewModel.setASRListening(false);
                });
            }
            myHandler.postDelayed(()->{
                mContext.bindService(mServiceIntent, Context.BIND_AUTO_CREATE | Context.BIND_INCLUDE_CAPABILITIES,
                        mExecutor, InputProviderConnector.this);
            }, 200);

        }
    };

    IResultCallback mIResultCallback = new IResultCallback.Stub() {

        @Override
        public void onSuccess(Bundle returnValues) throws RemoteException {
            Log.d(TAG, "mIResultCallback onSuccess");
        }

        @Override
        public void onFailure(Bundle params) throws RemoteException {
            Log.d(TAG, "mIResultCallback onFailure");
        }
    };

    public InputProviderConnector(Context context) {
        mContext = context;
        isBound = false;
        myHandler = new Handler(context.getMainLooper());
        mExecutor = Executors.newSingleThreadExecutor();
        mCountDownLatch = new CountDownLatch(1);
    }

    IInputProviderService getService() {
        if (isBound) {
            return IInputProviderService.Stub.asInterface(mIntentIBinder);
        } else {
            return null;
        }
    }

    public void registerClient(Bundle params) {
        Log.d(TAG, "registerClient :" + params);
        try {
            if (isBound) {
                getService().registerClient(params, mIResultCallback);
            } else {
                try {
                    boolean res = mCountDownLatch.await(200, TimeUnit.MILLISECONDS);
                    if (res) getService().registerClient(params, mIResultCallback);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                Log.e("input provider", "registerClient error, no connect");
            }
        } catch (Exception e) {
        }
    }

    public void unregisterClient(Bundle params) {
        Log.d(TAG, "unregisterClient :" + params);
        try {
            if (isBound) {
                getService().unregisterClient(params);
            } else {
                try {
                    boolean res = mCountDownLatch.await(200, TimeUnit.MILLISECONDS);
                    if (res) getService().unregisterClient(params);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                Log.e("input provider", "unregisterClient error, no connect");
            }
        } catch (Exception e) {
        }
    }

    public void getParams(Bundle params, IResultCallback callback) {
        Log.d(TAG, "getParams :" + params);
        try {
            if (isBound) {
                getService().getParams(params, callback);
            } else {
                boolean res = mCountDownLatch.await(200, TimeUnit.MILLISECONDS);
                if (res) getService().getParams(params, callback);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setParams(Bundle params, IResultCallback callback) {
        Log.d(TAG, "setParams :" + params);
        if (callback == null) callback = mIResultCallback;
        try {
            if (isBound) {
                getService().setParams(params, callback);
            } else {
                boolean res = mCountDownLatch.await(200, TimeUnit.MILLISECONDS);
                if (res) getService().setParams(params, callback);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startRecording(Bundle params, IInputReceiverCallback callback) {
        Log.d(TAG, "startRecording :" + params);
        try {
            if (isBound) {
                getService().startRecording(params, callback);
            } else {
                boolean res = mCountDownLatch.await(200, TimeUnit.MILLISECONDS);
                if (res) getService().startRecording(params, callback);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopRecording(Bundle params) {
        Log.d(TAG, "stopRecording :" + params);
        try {
            if (isBound) {
                getService().stopRecording(params);
            } else {
                boolean res = mCountDownLatch.await(200, TimeUnit.MILLISECONDS);
                if (res) getService().stopRecording(params);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void registerInputReceiverCallback(IInputReceiverCallback resultCallback) {
        Log.d(TAG, "registerInputReceiverCallback ");
        try {
            if (isBound) {
                getService().registerInputReceiverCallback(resultCallback);
            } else {
                try {
                    boolean res = mCountDownLatch.await(200, TimeUnit.MILLISECONDS);
                    if (res) getService().registerInputReceiverCallback(resultCallback);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

            }
        } catch (Exception e) {
        }
    }

    public void onResponseReceived(Bundle params) {
        Log.d(TAG, "onResponseReceived " + params);
        try {
            if (isBound) {
                getService().onResponseReceived(params);
            } else {
                try {
                    boolean res = mCountDownLatch.await(200, TimeUnit.MILLISECONDS);
                    if (res) getService().onResponseReceived(params);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                Log.e("input provider", "onResponseReceived error, no connect");
            }
        } catch (Exception e) {
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.d(TAG, "onServiceConnected " + name);
        isBound = true;
        mIntentIBinder = service;
        String asrLanguage = Settings.getASRLanguage(mContext);
        if (!TextUtils.isEmpty(asrLanguage)) {
            Bundle bundle = new Bundle();
            bundle.putString("request.language", asrLanguage);
            setParams(bundle, null);
        }
        try {
            service.linkToDeath(mDeathRecipient, 0);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        Runnable runnable = mRunOnConnect.get();
        if (runnable != null) {
            myHandler.post(runnable);
        }
        mCountDownLatch.countDown();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.d(TAG, "onServiceDisconnected " + name);
        isBound = false;
        mCountDownLatch = new CountDownLatch(1);
    }

    public synchronized void connect(Runnable runOnConnect) {
        Log.d(TAG, "connect ");
        mRunOnConnect = new WeakReference<>(runOnConnect);
        if (isBound) {
            runOnConnect.run();
            return;
        }

        mContext.bindService(mServiceIntent, Context.BIND_AUTO_CREATE | Context.BIND_INCLUDE_CAPABILITIES,
                mExecutor, this);
    }

    public synchronized void disconnect() {
        Log.d(TAG, "disconnect ");
        if (isBound) {
            mContext.unbindService(this);
            mContext = null;
            isBound = false;
            if (mIntentIBinder != null) {
                Log.d(TAG, "unlinkToDeath");
                mIntentIBinder.unlinkToDeath(mDeathRecipient, 0);
            }
        }
    }
}
