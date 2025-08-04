/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.voiceai.usecase.voicecall;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.IBinder;

import com.qualcomm.qti.voiceai.usecase.voicecall.ui.VoiceCallTranslationActivity;
import com.qualcomm.qti.voiceai.usecase.wrapper.QTelephonyManager;
import com.qualcomm.qti.voiceai.usecase.wrapper.PhoneCallStateCallbackWrapper;

import androidx.core.app.NotificationCompat;

public class VoiceCallTranslationService extends Service {

    private static final String TAG = VoiceCallTranslationService.class.getSimpleName();
    private static final String ACTION_ENABLE_VOICE_CALL_TRANSLATION_FEATURE = "com.qualcomm.qti.voiceai.usecase.voicecall.enable_feature";
    private static final String ACTION_ENTER_TRANSLATION_MODE = "com.qualcomm.qti.voiceai.usecase.voicecall.enter_mode";
    public static final String KEY_ENABLE_FEATURE = "enable";
    public static final String KEY_ENTER_MODE = "mode";
    public static final int STATE_INIT = 1;
    public static final int STATE_START = 2;
    public static final int STATE_STOP = 3;
    public static final int STATE_DEINIT = 4;

    private static final int DEFAULT_CALL_STATE = -1;
    private VoiceCallFloatWindow mFloatWindow;
    private VoiceCallTranslationManager mManager;
    private PhoneCallStateCallbackWrapper mCallback;
    private PhoneCallStateCallbackWrapper mSub2Callback;
    private QTelephonyManager mQTelephonyManager;
    private int mPhoneCallState;
    private boolean mIsPhoneCallActive;
    private boolean mIsInTranslationMode;
    private boolean isFeatureEnabled;

    public static void enableFeature(Context context, boolean enable) {
        Intent serviceIntent = new Intent(ACTION_ENABLE_VOICE_CALL_TRANSLATION_FEATURE);
        serviceIntent.setClassName(context.getPackageName(), VoiceCallTranslationService.class.getName());
        serviceIntent.putExtra(KEY_ENABLE_FEATURE, enable);
        context.startService(serviceIntent);
    }

    public static void enterTranslationMode(Context context, boolean enter) {
        Intent serviceIntent = new Intent(ACTION_ENTER_TRANSLATION_MODE);
        serviceIntent.setClassName(context.getPackageName(), VoiceCallTranslationService.class.getName());
        serviceIntent.putExtra(KEY_ENTER_MODE, enter);
        context.startService(serviceIntent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        QLog.VCLogD(TAG, "onCreate");
        mManager = new VoiceCallTranslationManager(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_ENABLE_VOICE_CALL_TRANSLATION_FEATURE.equals(intent.getAction())) {
            boolean enable = intent.getBooleanExtra(KEY_ENABLE_FEATURE, false);
            QLog.VCLogD(TAG, "voice call translation feature is changed to " + (enable? "enable": "disable"));
            triggerFeatureOn(enable);
            return enable ? START_STICKY : START_NOT_STICKY;
        } else if (intent != null && ACTION_ENTER_TRANSLATION_MODE.equals(intent.getAction())) {
            boolean enterMode = intent.getBooleanExtra(KEY_ENTER_MODE, false);
            QLog.VCLogD(TAG, "voice call translation mode is changed to " + enterMode);
            updateTranslationMode(enterMode);
            return START_STICKY;
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        QLog.VCLogD(TAG, "onDestroy");
        unregisterTelephonyCallback();
        mManager.clearThread();
        mManager = null;
        mFloatWindow = null;
    }

    private void showFloatWindow() {
        if(mFloatWindow == null) {
            mFloatWindow = new VoiceCallFloatWindow(this);
        }
        mFloatWindow.show();
    }

    private void hideFloatWindow() {
        if(mFloatWindow != null) {
            mFloatWindow.hide();
        }
    }

    private void triggerFeatureOn(boolean enable) {
        if(isFeatureEnabled ^ enable) {
            isFeatureEnabled = enable;
            if (enable) {
                registerTelephonyCallback();
                updateForegroundState(true);
            } else {
                updateForegroundState(false);
                unregisterTelephonyCallback();
                hideFloatWindow();
                mManager.stop();
                mManager.deinit();
                mIsInTranslationMode = false;
                stopSelf();
            }
        }
    }

    private void registerTelephonyCallback() {
        QLog.VC_PhoneCallLogD("registerTelephonyCallback");
        mPhoneCallState = DEFAULT_CALL_STATE;
        if (mQTelephonyManager == null) {
            mQTelephonyManager = new QTelephonyManager(this);
        }
        if (mCallback == null) {
            mCallback = new PhoneCallStateCallbackWrapper(this, new PhoneCallStateCallbackWrapper.Listener() {
                @Override
                public void onIdle() {
                    if (mPhoneCallState != DEFAULT_CALL_STATE) {
                        updateTranslationState(STATE_STOP);
                        updateTranslationState(STATE_DEINIT);
                    }
                }

                @Override
                public void onRinging() {
                    updateTranslationState(STATE_INIT);
                }

                @Override
                public void onAlerting() {
                    updateTranslationState(STATE_INIT);
                }

                @Override
                public void onActive() {
                    updateTranslationState(STATE_START);
                }
            });
        }

        if(mSub2Callback == null) {
            mSub2Callback = new PhoneCallStateCallbackWrapper(this, new PhoneCallStateCallbackWrapper.Listener() {
                @Override
                public void onIdle() {
                    QLog.VC_PhoneCallLogD("mCallbackSub2 onIdle");
                    if (mPhoneCallState != DEFAULT_CALL_STATE) {
                        updateTranslationState(STATE_STOP);
                        updateTranslationState(STATE_DEINIT);
                    }
                }

                @Override
                public void onRinging() {
                    QLog.VC_PhoneCallLogD("mCallbackSub2 onRinging");
                    updateTranslationState(STATE_INIT);
                }

                @Override
                public void onAlerting() {
                    QLog.VC_PhoneCallLogD("mCallbackSub2 onAlerting");
                    updateTranslationState(STATE_INIT);
                }

                @Override
                public void onActive() {
                    QLog.VC_PhoneCallLogD("mCallbackSub2 onActive");
                    updateTranslationState(STATE_START);
                }
            });
        }

        mQTelephonyManager.registerTelephonyCallback(mCallback);
        mQTelephonyManager.registerTelephonyCallbackForSub2(mSub2Callback);
    }

    private void unregisterTelephonyCallback() {
        QLog.VC_PhoneCallLogD("unregisterTelephonyCallback");
        if (mCallback != null) {
            mQTelephonyManager.unregisterTelephonyCallback(mCallback);
            mQTelephonyManager.unregisterTelephonyCallbackForSub2(mSub2Callback);
            mCallback = null;
            mSub2Callback = null;
        }
        mQTelephonyManager = null;
    }

    private void updateTranslationState(int pendingState) {
        mPhoneCallState = pendingState;
        mIsPhoneCallActive = pendingState == STATE_START;
        QLog.VC_PhoneCallLogD("voice call translation pending state is changed to " + pendingState);
        if (pendingState == STATE_INIT) {
            showFloatWindow();
            mManager.init();
        } else if (pendingState == STATE_START) {
            showFloatWindow();
            tryToStartTranslation();
        } else if (pendingState == STATE_STOP) {
            mManager.stop();
        } else if (pendingState == STATE_DEINIT) {
            hideFloatWindow();
            mManager.deinit();
            mIsInTranslationMode = false;
            destroyTranslationActivityWhenCallDisconnected();
        } else {
            QLog.VC_PhoneCallLogE("error state " + pendingState);
        }
    }

    private void updateTranslationMode(boolean enter) {
        mIsInTranslationMode = enter;
        if (mIsInTranslationMode) {
            tryToStartTranslation();
        } else {
            mManager.stop();
        }
    }

    private void tryToStartTranslation() {
        QLog.VC_PhoneCallLogD("isPhoneCallActive=" + mIsPhoneCallActive
                + ", inTranslationMode=" + mIsInTranslationMode);
        if (mIsPhoneCallActive && mIsInTranslationMode) {
            mManager.start();
        }
    }

    private void destroyTranslationActivityWhenCallDisconnected() {
        QLog.VCLogD(TAG, "destroyTranslationActivityWhenCallDisconnected");
        Intent activityIntent = new Intent();
        activityIntent.putExtra("call_state", STATE_DEINIT);
        activityIntent.setClassName(getPackageName(), VoiceCallTranslationActivity.class.getName());
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(activityIntent);
    }

    private void updateForegroundState(boolean foreground) {
        if (foreground) {
            NotificationChannel channel = new NotificationChannel("Foreground_tx_channel",
                    "VoiceCallTXChannel", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("voicecall tx channel for foreground service notification");

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(this, "Foreground_tx_channel")
                    .setSmallIcon(android.R.drawable.sym_def_app_icon)
                    .build();
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    | ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            stopForeground(STOP_FOREGROUND_REMOVE);
        }
    }
}