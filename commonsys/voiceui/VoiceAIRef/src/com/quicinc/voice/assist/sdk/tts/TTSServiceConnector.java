/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.quicinc.voice.assist.sdk.tts;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.quicinc.voice.activation.aidl.IResultCallback;
import com.quicinc.voice.activation.aidl.ISpeakProgressCallback;
import com.quicinc.voice.activation.aidl.ITTSService;
import com.quicinc.voice.assist.sdk.utility.Constants;

import java.lang.ref.WeakReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TTSServiceConnector implements ServiceConnection {
    private final static String TAG = TTSServiceConnector.class.getSimpleName();
    public final static String TTS_SERVICE_CLASS_NAME =
            "com.quicinc.voice.activation.tts.TTSService";
    private final Intent mServiceIntent = new Intent()
            .setClassName(Constants.QVA_PACKAGE_NAME, TTS_SERVICE_CLASS_NAME);

    private final Context mContext;
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
            Log.d(TAG, "binderDied, try to reconnect");
            myHandler.postDelayed(()->{
                mContext.bindService(mServiceIntent, Context.BIND_AUTO_CREATE | Context.BIND_INCLUDE_CAPABILITIES,
                        mExecutor, TTSServiceConnector.this);
            }, 200);

        }
    };

    public TTSServiceConnector(Context context) {
        mContext = context;
        isBound = false;
        myHandler = new Handler(context.getMainLooper());
        mExecutor = Executors.newSingleThreadExecutor();
        mCountDownLatch = new CountDownLatch(1);
    }

    ITTSService getService() {
        if (isBound) {
            return ITTSService.Stub.asInterface(mIntentIBinder);
        } else {
            return null;
        }
    }

    public void initTTSEngine(String language, IResultCallback callback) {
        Log.d(TAG, "initTTSEngine ");
        if (callback == null) {
            callback = new IResultCallback.Stub() {
                @Override
                public void onSuccess(Bundle returnValues) throws RemoteException {
                    Log.d(TAG, "TTS onSuccess " + returnValues.toString());
                }

                @Override
                public void onFailure(Bundle params) throws RemoteException {
                    Log.d(TAG, "TTS onFailure " + params.toString());
                }
            };
        }
        try {
            if (isBound) {
                getService().initTTSEngine(language, callback);
            } else {
                boolean res = mCountDownLatch.await(200, TimeUnit.MILLISECONDS);
                if (res) getService().initTTSEngine(language, callback);
            }
        } catch (Exception e) {
            Log.e("tts provider", "initTTSEngine error, no connect");
        }
    }

    public void startSpeak(Bundle params, WeakReference<ISpeakProgressCallback> callbackRef){
        Log.d(TAG, "startSpeak " + params.toString());
        ISpeakProgressCallback callback = callbackRef.get();
        if (callback == null) {
            Log.e(TAG, "startSpeak call back is null");
            return;
        }
        try {
            if (isBound) {
                getService().startSpeak(params, callback);
            } else {
                boolean res = mCountDownLatch.await(200, TimeUnit.MILLISECONDS);
                if (res)  getService().startSpeak(params, callback);
            }
        } catch (Exception e) {
            Log.e("tts provider", "startSpeak error, no connect");
        }
    }
    public void stopSpeak(){
        Log.d(TAG, "stopSpeak " );
        try {
            if (isBound) {
                getService().stopSpeak();
            } else {
                boolean res = mCountDownLatch.await(200, TimeUnit.MILLISECONDS);
                if (res)  getService().stopSpeak();
            }
        } catch (Exception e) {
            Log.e("tts provider", "stopSpeak error, no connect");
        }
    }
    public void deInitTTSEngine(){
        Log.d(TAG, "deInitTTSEngine " );
        try {
            if (isBound) {
                getService().deinitTTSEngine();
            } else {
                boolean res = mCountDownLatch.await(200, TimeUnit.MILLISECONDS);
                if (res)  getService().deinitTTSEngine();
            }
        } catch (Exception e) {
            Log.e("tts provider", "deinitTTSEngine error, no connect");
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Log.d(TAG, "onServiceConnected " + name);
        isBound = true;
        mIntentIBinder = service;
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
            isBound = false;
            if (mIntentIBinder != null) {
                Log.d(TAG, "unlinkToDeath");
                mIntentIBinder.unlinkToDeath(mDeathRecipient, 0);
            }
        }
    }
}
