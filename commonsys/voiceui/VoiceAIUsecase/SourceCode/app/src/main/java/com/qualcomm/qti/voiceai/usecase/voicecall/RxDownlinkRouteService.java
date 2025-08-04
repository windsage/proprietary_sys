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

import androidx.core.app.NotificationCompat;


public class RxDownlinkRouteService extends Service {

    private static final String TAG = RxDownlinkRouteService.class.getSimpleName();
    private static final String ACTION_RX_INIT = "ACTION_RX_INIT";
    private static final String ACTION_RX_START = "ACTION_RX_START";
    private static final String ACTION_RX_STOP = "ACTION_RX_STOP";
    private static final String ACTION_RX_DEINIT = "ACTION_RX_DEINIT";
    private static final String ACTION_RX_SET_LANGUAGE = "ACTION_RX_SET_LANGUAGE";
    public static final String KEY_START_TIME = "start_time";
    public static final String KEY_ASR_LANGUAGE = "asr_language";
    public static final String KEY_TRANSLATION_LANGUAGE = "translation_language";
    private RxDownlinkRouteManager mManager;

    public static void init(Context context) {
        Intent serviceIntent = new Intent(ACTION_RX_INIT);
        serviceIntent.setClassName(context.getPackageName(), RxDownlinkRouteService.class.getName());
        context.startService(serviceIntent);
    }

    public static void start(Context context, String startTime) {
        Intent serviceIntent = new Intent(ACTION_RX_START);
        serviceIntent.setClassName(context.getPackageName(), RxDownlinkRouteService.class.getName());
        serviceIntent.putExtra(KEY_START_TIME, startTime);
        context.startService(serviceIntent);
    }

    public static void stop(Context context) {
        Intent serviceIntent = new Intent(ACTION_RX_STOP);
        serviceIntent.setClassName(context.getPackageName(), RxDownlinkRouteService.class.getName());
        context.startService(serviceIntent);
    }

    public static void deinit(Context context) {
        Intent serviceIntent = new Intent(ACTION_RX_DEINIT);
        serviceIntent.setClassName(context.getPackageName(), RxDownlinkRouteService.class.getName());
        context.startService(serviceIntent);
    }

    public static void setLanguage(Context context, String asrLanguage, String translationLanguage) {
        Intent serviceIntent = new Intent(ACTION_RX_SET_LANGUAGE);
        serviceIntent.setClassName(context.getPackageName(), RxDownlinkRouteService.class.getName());
        serviceIntent.putExtra(KEY_ASR_LANGUAGE, asrLanguage);
        serviceIntent.putExtra(KEY_TRANSLATION_LANGUAGE, translationLanguage);
        context.startService(serviceIntent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        QLog.VCLogD(TAG, "onCreate");
        mManager = new RxDownlinkRouteManager(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_RX_INIT.equals(intent.getAction())) {
            QLog.VCLogD(TAG, "rx downlink route init");
            updateForegroundState(true);
            mManager.init();
        } else if (intent != null && ACTION_RX_START.equals(intent.getAction())) {
            QLog.VCLogD(TAG, "rx downlink route start");
            String startTime = intent.getStringExtra(KEY_START_TIME);
            Constants.markVoiceCallStartTime(startTime);
            mManager.start();
        } else if (intent != null && ACTION_RX_STOP.equals(intent.getAction())) {
            QLog.VCLogD(TAG, "rx downlink route stop");
            Constants.cleanVoiceCallStartTime();
            mManager.stop();
        }
        else if (intent != null && ACTION_RX_DEINIT.equals(intent.getAction())) {
            QLog.VCLogD(TAG, "rx downlink route deinit");
            updateForegroundState(false);
            mManager.deinit();
            stopSelf();
        }
        else if (intent != null && ACTION_RX_SET_LANGUAGE.equals(intent.getAction())) {
            String asrLanguage = intent.getStringExtra(KEY_ASR_LANGUAGE);
            String translationLanguage = intent.getStringExtra(KEY_TRANSLATION_LANGUAGE);
            mManager.setASRLanguage(asrLanguage);
            mManager.setTranslationLanguage(translationLanguage);
        }
        return START_NOT_STICKY;
    }

    private void updateForegroundState(boolean foreground) {
        if (foreground) {
            NotificationChannel channel = new NotificationChannel("Foreground_rx_channel",
                    "VoiceCallRXChannel", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("voicecall rx channel for foreground service notification");

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(this, "Foreground_rx_channel")
                    .setSmallIcon(android.R.drawable.sym_def_app_icon)
                    .build();
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    | ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            stopForeground(STOP_FOREGROUND_REMOVE);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        QLog.VCLogD(TAG, "onDestroy");
        mManager = null;
    }
}