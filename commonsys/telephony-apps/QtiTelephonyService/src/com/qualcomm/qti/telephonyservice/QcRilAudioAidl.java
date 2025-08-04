/*
 * Copyright (c) 2021-2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.telephonyservice;

import android.os.IBinder;
import android.util.Log;
import android.os.Binder;
import android.os.ServiceManager;
import android.os.RemoteException;
import vendor.qti.hardware.radio.am.AudioError;

public class QcRilAudioAidl implements IAudioControllerCallback {

    private final static String TAG = "QcRilAudioAidl";
    private static final String QCRIL_AUDIO_SERVICE_NAME =
            "vendor.qti.hardware.radio.am.IQcRilAudio/slot";
    private vendor.qti.hardware.radio.am.IQcRilAudio mRilAudio;
    private final String mInstanceName;
    private final String mServiceInstance;
    private QcRilAudioDeathRecipient mDeathRecipient;
    private vendor.qti.hardware.radio.am.IQcRilAudioRequest mRequestInterface =
            new QcRilAudioRequest();
    private vendor.qti.hardware.radio.am.IQcRilAudioResponse mResponseInterface;
    private IAudioController mAudioController;
    private IBinder mBinder;
    // Synchronization object of HAL interfaces.
    private final Object mLock = new Object();
    private int mAudioServerStatus = AudioController.AUDIO_STATUS_UNKNOWN;

    public QcRilAudioAidl(int slotId, IAudioController audioController) {
        mInstanceName = "slot" + slotId;
        mServiceInstance = QCRIL_AUDIO_SERVICE_NAME + slotId;
        mAudioController = audioController;
        mDeathRecipient = new QcRilAudioDeathRecipient();
        initQcRilAudio();
    }

    public static boolean isAidlAvailable(int slotId) {
        try {
            return ServiceManager.isDeclared(QCRIL_AUDIO_SERVICE_NAME + slotId);
        } catch (SecurityException e) {
            // Security exception will be thrown if sepolicy for AIDL is not present
            Log.e(TAG, "Security exception while call into AIDL");
        }
        return false;
    }

    /**
     * Class that implements the binder death recipient to be notified when
     * IQcRilAudio service dies.
     */
    final class QcRilAudioDeathRecipient implements IBinder.DeathRecipient {
        /**
         * Callback that gets called when the service has died
         */
        @Override
        public void binderDied() {
            Log.e(TAG, "QcRilAudio died.");
            resetService();
            initQcRilAudio();
            setError(mAudioServerStatus);
        }
    }

    private void resetService() {
        IBinder binder = null;
        synchronized (mLock) {
            mRilAudio = null;
            mResponseInterface = null;
            binder = mBinder;
            mBinder = null;
        }
        try {
            boolean result = binder.unlinkToDeath(mDeathRecipient, 0 /* Not used */);
        } catch (Exception ex) {
            Log.e(TAG, "QcRilAudio binder is null " + ex);
        }
    }

    private vendor.qti.hardware.radio.am.IQcRilAudio rilAudio() {
        vendor.qti.hardware.radio.am.IQcRilAudio rilAudio = null;
        synchronized (mLock) {
            rilAudio = mRilAudio;
        }
        return rilAudio;
    }

    private vendor.qti.hardware.radio.am.IQcRilAudioResponse responseInterface() {
        vendor.qti.hardware.radio.am.IQcRilAudioResponse responseInterface = null;
        synchronized (mLock) {
            responseInterface = mResponseInterface;
        }
        return responseInterface;
    }

    private IAudioController audioController() {
        IAudioController audioController = null;
        synchronized (mLock) {
            audioController = mAudioController;
        }
        return audioController;
    }

    private void initQcRilAudio() {
        try {
            if (rilAudio() == null) {
                IBinder binder = Binder.allowBlocking(
                       ServiceManager.waitForDeclaredService(mServiceInstance));
                if (binder != null) {
                    binder.linkToDeath(mDeathRecipient, 0 /* Not Used */);
                    Log.i(TAG, "initQcRilAudio: Stable AIDL is used.");
                    vendor.qti.hardware.radio.am.IQcRilAudio qcRilAudio =
                            vendor.qti.hardware.radio.am.IQcRilAudio.Stub.asInterface(binder);
                    if (qcRilAudio != null) {
                        Log.i(TAG, "initQcRilAudio: setRequestInterface");
                        synchronized (mLock) {
                            mBinder = binder;
                            mRilAudio = qcRilAudio;
                            mResponseInterface = qcRilAudio.setRequestInterface(mRequestInterface);
                        }
                    } else {
                        Log.e(TAG, "initQcRilAudio: mRilAudio is null");
                    }
                } else {
                    Log.e(TAG, "initQcRilAudio: Stable AIDL is NOT used.");
                }
            }
        } catch (Exception ex) {
             Log.e(TAG, "initQcRilAudio: Exception: " + ex);
        }
    }

    private class QcRilAudioRequest extends vendor.qti.hardware.radio.am.IQcRilAudioRequest.Stub {
        @Override
        public void setParameters(int token, String params) {
            IAudioController ac = audioController();
            if (ac == null) {
                Log.e(TAG, "setParameters - mAudioController is null, returning.");
                return;
            }
            int ret = convertAudioErrorToAidl(ac.setParameters(params));
            vendor.qti.hardware.radio.am.IQcRilAudioResponse respInterface = responseInterface();
            if (respInterface == null) {
                Log.e(TAG, "setParameters - response interface is null, returning.");
                return;
            }
            try {
                respInterface.setParametersResponse(token, ret);
            } catch (Exception ex) {
                Log.e(TAG, "setParametersResponse: Exception: " + ex);
            }
        }

        @Override
        public void queryParameters(int token, String params) {
            IAudioController ac = audioController();
            if (ac == null) {
                Log.e(TAG, "queryParameters - mAudioController is null, returning.");
                return;
            }
            String ret = ac.getParameters(params);
            vendor.qti.hardware.radio.am.IQcRilAudioResponse respInterface = responseInterface();
            if (respInterface == null) {
                Log.e(TAG, "queryParameters - response interface is null, returning.");
                return;
            }
            try {
                respInterface.queryParametersResponse(token, ret);
            } catch (Exception ex) {
                Log.e(TAG, "getParametersResponse: Exception: " + ex);
            }
        }

        @Override
        public String getInterfaceHash() {
            return vendor.qti.hardware.radio.am.IQcRilAudioRequest.HASH;
        }

        @Override
        public final int getInterfaceVersion() {
            return vendor.qti.hardware.radio.am.IQcRilAudioRequest.VERSION;
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
        vendor.qti.hardware.radio.am.IQcRilAudio rilAudio = rilAudio();
        if (rilAudio == null) {
            Log.e(TAG, "setError - mRilAudio is null, returning.");
            return;
        }
        try {
            rilAudio.setError(convertAudioErrorToAidl(errorCode));
        } catch (Exception e) {
            Log.e(TAG, "setError request to IQcRilAudio: " + mInstanceName + " Exception: " + e);
        }
    }

    @Override
    public void onDispose() {
        resetService();
        synchronized (mLock) {
            mRequestInterface = null;
            mAudioController = null;
        }
    }

    private int convertAudioErrorToAidl(int errorCode) {
        switch (errorCode) {
            case AudioController.AUDIO_STATUS_OK:
                return AudioError.STATUS_OK;
            case AudioController.AUDIO_STATUS_SERVER_DIED:
                return AudioError.STATUS_SERVER_DIED;
            default:
                return AudioError.GENERIC_FAILURE;
        }
    }
}
