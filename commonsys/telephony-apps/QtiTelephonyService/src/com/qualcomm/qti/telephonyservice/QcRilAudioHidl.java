/*
 * Copyright (c) 2017, 2021-2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.telephonyservice;

import android.media.AudioManager;
import android.os.IBinder;
import android.os.HwBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import java.util.concurrent.atomic.AtomicLong;
import vendor.qti.hardware.radio.am.V1_0.AudioError;
import vendor.qti.hardware.radio.am.V1_0.IQcRilAudioCallback;

import android.hidl.manager.V1_0.IServiceManager;
import android.hidl.manager.V1_0.IServiceNotification;

public class QcRilAudioHidl implements IAudioControllerCallback {
    final static String TAG = "QcRilAudioHidl";

    private final String mInstanceName;
    private final String mLogSuffix;
    private final AtomicLong mRilAudioCookie = new AtomicLong(0);

    private final AudioProxyDeathRecipient mRilAudioDeathRecipient = new AudioProxyDeathRecipient();
    private final ServiceNotification mServiceNotification = new ServiceNotification();
    private final IQcRilAudioCallback mQcRilAudioCallback = new QcRilAudioCallbackHidl();
    private IAudioController mAudioController;
    private vendor.qti.hardware.radio.am.V1_0.IQcRilAudio mRilAudio;
    private boolean mIsDisposed = false;
    private int mAudioServerStatus = AudioController.AUDIO_STATUS_UNKNOWN;
    // Synchronization object of HAL interfaces.
    private final Object mLock = new Object();

    private vendor.qti.hardware.radio.am.V1_0.IQcRilAudio rilAudio() {
        vendor.qti.hardware.radio.am.V1_0.IQcRilAudio rilAudio = null;
        synchronized (mLock) {
            rilAudio = mRilAudio;
        }
        return rilAudio;
    }

    private IAudioController audioController() {
        IAudioController audioController = null;
        synchronized (mLock) {
            audioController = mAudioController;
        }
        return audioController;
    }

    private class QcRilAudioCallbackHidl extends IQcRilAudioCallback.Stub {
        @Override
        public int setParameters(String keyValuePairs) {
            IAudioController audioController = audioController();
            if (audioController == null) {
                Log.e(TAG, "setParameters - mAudioController is null, returning.");
                return AudioError.AUDIO_GENERIC_FAILURE;
            }
            return convertAudioErrorToHidl(audioController.setParameters(keyValuePairs));
        }

        @Override
        public String getParameters(String key) {
            IAudioController audioController = audioController();
            if (audioController == null) {
                Log.e(TAG, "getParameters - mAudioController is null, returning.");
                return "";
            }
            return audioController.getParameters(key);
        }
    }

    private class ServiceNotification extends IServiceNotification.Stub {
        @Override
        public void onRegistration(String fqName, String name, boolean preexisting) {
            Log.d(TAG, "onRegistration: fqName=" + fqName + " name=" + name);

            if (!mInstanceName.equals(name) || isDisposed()) {
                Log.d(TAG, "onRegistration: Ignoring.");
                dump("onRegistration");
                return;
            }

            initHal();
            if (mAudioServerStatus != AudioController.AUDIO_STATUS_UNKNOWN) {
                setError(mAudioServerStatus);
            }
        }
    }

    private class AudioProxyDeathRecipient implements HwBinder.DeathRecipient {
        @Override
        public void serviceDied(long cookie) {
            if (isDisposed()) {
                Log.d(TAG, "serviceDied: Ignoring.");
                dump("serviceDied");
                return;
            }

            final long current = mRilAudioCookie.get();
            if (cookie != current) {
                Log.v(TAG, "serviceDied: Ignoring. provided=" + cookie + " expected=" + current);
                return;
            }

            Log.e(TAG, "IQcRilAudio service died" + mLogSuffix);
            resetService();
        }

    }

    public QcRilAudioHidl(int slotId, IAudioController audioController) {
        mInstanceName = "slot" + slotId;
        mLogSuffix = "[" + mInstanceName + "]";
        mAudioController = audioController;
        try {
            boolean ret = IServiceManager.getService()
                    .registerForNotifications("vendor.qti.hardware.radio.am@1.0::IQcRilAudio",
                    mInstanceName, mServiceNotification);
            if (!ret) {
                Log.e(TAG, "Unable to register service start notification: ret = " + ret);
            }
        } catch (RemoteException ex) {
            Log.e(TAG, "Unable to register service start notification");
        }
    }

    @Override
    public void onDispose() {
        if (isDisposed()) {
            return;
        }
        mIsDisposed = true;
        vendor.qti.hardware.radio.am.V1_0.IQcRilAudio rilAudio = rilAudio();
        try {
            if (rilAudio != null) {
                rilAudio.unlinkToDeath(mRilAudioDeathRecipient);
                rilAudio = null;
            }
        } catch(RemoteException e) {
            Log.d(TAG, "dispose: Exception=" + e );
        }
    }

    public boolean isDisposed() {
        return mIsDisposed;
    }

    private void initHal() {
        Log.d(TAG, "initHal");
        vendor.qti.hardware.radio.am.V1_0.IQcRilAudio rilAudio;
        try {
            rilAudio = vendor.qti.hardware.radio.am.V1_0.IQcRilAudio.getService(mInstanceName);
            if (rilAudio == null) {
                Log.e(TAG, "initHal: mRilAudio == null");
                return;
            }
            rilAudio.linkToDeath(mRilAudioDeathRecipient,
                    mRilAudioCookie.incrementAndGet());
            rilAudio.setCallback(mQcRilAudioCallback);
            synchronized (mLock) {
                mRilAudio = rilAudio;
            }
        } catch (Exception e) {
                Log.e(TAG, "initHal: Exception: " + e);
        }
    }

    @Override
    public void onAudioStatusChanged(int status) {
        if (mAudioServerStatus == status) {
            return;
        }
        mAudioServerStatus = status;
        setError(mAudioServerStatus);
    }

    private void setError(int errorCode) {
        vendor.qti.hardware.radio.am.V1_0.IQcRilAudio rilAudio = rilAudio();
        if (rilAudio == null) {
            Log.w(TAG, "setError - mRilAudio is null, returning." + mLogSuffix);
            return;
        }

        try {
            rilAudio.setError(convertAudioErrorToHidl(errorCode));
            Log.d(TAG, "setError." + mLogSuffix);
        } catch (Exception e) {
            Log.e(TAG, "setError request to IQcRilAudio: " + mInstanceName + " Exception: " + e);
        }
    }

    private void resetService() {
        synchronized (mLock) {
            mRilAudio = null;
        }
    }

    private void dump(String fn) {
        Log.d(TAG, fn + ": InstanceName=" + mInstanceName);
        Log.d(TAG, fn + ": isDisposed=" + isDisposed());
    }

    private int convertAudioErrorToHidl(int errorCode) {
        switch (errorCode) {
            case AudioController.AUDIO_STATUS_OK :
                return AudioError.AUDIO_STATUS_OK;
            case AudioController.AUDIO_STATUS_SERVER_DIED :
                return AudioError.AUDIO_STATUS_SERVER_DIED;
            default :
                return AudioError.AUDIO_GENERIC_FAILURE;
        }
    }
}
