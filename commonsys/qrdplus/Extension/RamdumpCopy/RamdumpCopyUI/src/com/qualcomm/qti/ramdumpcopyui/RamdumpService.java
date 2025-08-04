/**
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.ramdumpcopyui;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.ForegroundServiceStartNotAllowedException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;

import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import java.util.HashMap;

import static com.qualcomm.qti.ramdumpcopyui.Constants.*;

public class RamdumpService extends Service {
    private static final String TAG = "RamdumpUIService";

    public static final int RETRY_TIMES = 3;

    public static final String CALL_BACK = "receiver";
    public static final String ACTION = "com.qualcomm.qti.ramdumpcopyui.UPDATE";
    public static final String ACTION_DAEMON = "com.qualcomm.qti.ramdump.DAEMON";

    public static final String TAG_CURRENT_INDEX = "index";
    public static final String TAG_TOTAL_COUNT = "count";
    public static final String TAG_FILE_NAME = "name";
    public static final String TAG_PATH = "path";
    public static final String TAG_ERROR = "err";

    private static final String EXCEPTION_FINISH = "copy_finish";

    private static final int TICK_TIME = 1000;
    private static final int DEFAULT_COUNT = 50;

    private static final String CHANNEL_ID = "notification_channel";
    private static final int NOTIFICATION_PROGRESS = 233;
    private static final int NOTIFICATION_DONE = 234;
    Notification.Builder mBuilder;

    private HashMap<Integer, String> mDumpInfo = new HashMap();
    private Handler mHandler;
    private boolean mConnected = false;
    private CopyStatusReceiver myReceiver = new CopyStatusReceiver();

    public RamdumpService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!mConnected) {
                    Bundle bundle = new Bundle();
                    sendUpdate(CMD_CONNECT_REFUSED, bundle);
                    updateNotification(0, "Copy error!", "Connection refused!", true);
                    Log.e(TAG, errorToString(ECONNREFUSED));
                }
            }
        }, 60000);
        try {
            showProgressNotification(this);
        } catch (ForegroundServiceStartNotAllowedException e) {
            Log.d(TAG, "ForegroundServiceStartNotAllowedException, CMD_CONNECT_REFUSED");
            Bundle bundle = new Bundle();
            sendUpdate(CMD_CONNECT_REFUSED, bundle);
        }
        registerReceiver(myReceiver, new IntentFilter(ACTION_DAEMON), Context.RECEIVER_EXPORTED);
        Log.d(TAG, "registerReceiver");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler(Looper.getMainLooper(), null);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "unregisterReceiver");
        unregisterReceiver(myReceiver);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void showProgressNotification(Context context) {
        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel notificationChannel =
                new NotificationChannel(CHANNEL_ID,
                        "Copy progress",
                        NotificationManager.IMPORTANCE_HIGH);
        nm.createNotificationChannel(notificationChannel);
        int PROGRESS_MAX = 100;
        int PROGRESS_CURRENT = 0;
        mBuilder = new Notification.Builder(context, CHANNEL_ID);
        mBuilder.setWhen(
                System.currentTimeMillis())
                .setContentTitle("Ramdump copying")
                .setProgress(PROGRESS_MAX, PROGRESS_CURRENT, false)
                .setSmallIcon(android.R.drawable.star_on)
                .setColor(context.getResources().getColor(R.color.colorPrimary, null))
                .setLocalOnly(true);

        Notification notification = mBuilder.build();
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.flags |= Notification.FLAG_ONLY_ALERT_ONCE;
        startForeground(NOTIFICATION_PROGRESS, notification);
    }

    private void updateNotification(int index, String title, String content, boolean dismiss) {
        Log.d(TAG, "updateNotification index=" + index + " title=" + title +
                " content=" + content + " dismiss=" + dismiss);
        if (dismiss) {
            mBuilder.setProgress(0, 0, false);
        } else {
            mBuilder.setContentText("File:" + content);
            mBuilder.setProgress(100, index, false);
        }
        Notification notification = mBuilder.build();
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(
                Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(NOTIFICATION_PROGRESS, notification);

        if (dismiss) {
            mBuilder.setContentTitle(title);
            mBuilder.setContentText(content);
            Notification notiDone = mBuilder.build();
            mNotificationManager.notify(NOTIFICATION_DONE, notiDone);
        }
    }

    private void sendUpdate(int code, Bundle bundle) {
        Intent intent = new Intent(ACTION);
        bundle.putInt(CALL_BACK, code);
        intent.putExtras(bundle);
        LocalBroadcastManager.getInstance(RamdumpService.this).sendBroadcast(intent);
    }

    private void parseCmd(String cmd) {
        Log.d(TAG, "parseCmd:" + cmd);
        if (TextUtils.isEmpty(cmd)) {
            return;
        }
        String cmds[] = cmd.split(":");
        if (TextUtils.equals(String.valueOf(FROM_SERVER), cmds[0])) {
            final int command = Integer.parseInt(cmds[1]);
            if (TextUtils.equals(String.valueOf(ENOREADY), cmds[2])) {
                Log.e(TAG, errorToString(ENOREADY));
                mDumpInfo.put(command, String.valueOf(ENOREADY));
                return;
            }
            Bundle bundle = new Bundle();
            switch (command) {
                case CMD_COPY_TYPE:
                    //FROM_SERVER:CMD_COPY_TYPE:1
                    Log.d(TAG, "CMD_COPY_TYPE " + (TextUtils.equals(
                            String.valueOf(TYPE_MULTIPLE), cmds[2]) ? "Multiple" : "Combined"));
                    mDumpInfo.put(command, cmds[2]);
                    break;
                case CMD_VALIDATED:
                    Log.d(TAG, "CMD_VALIDATED");
                    //FROM_SERVER:CMD_VALIDATED:status
                    mDumpInfo.put(command, cmds[2]);
                    final int dumpStatus = Integer.parseInt(cmds[2]);
                    if (dumpStatus != STATUS_OK) {
                        updateNotification(0, "Copy error!",
                                errorToString(dumpStatus), true);
                        bundle.putInt(TAG_ERROR, dumpStatus);
                        sendUpdate(CMD_VALIDATED, bundle);
                    }
                    break;
                case CMD_TOTAL_SIZE:
                    Log.d(TAG, "CMD_TOTAL_SIZE:" + cmds[2]);
                    //FROM_SERVER:CMD_TOTAL_SIZE:$dump_size:Byte
                    mDumpInfo.put(command, cmds[2]);
                    if (TextUtils.equals(cmds[2], String.valueOf(ENOSPC))) {
                        bundle.putInt(TAG_ERROR, ENOSPC);
                        updateNotification(0, "Copy error!",
                                errorToString(ENOSPC), true);
                        sendUpdate(CMD_TOTAL_SIZE, bundle);
                    }
                    break;
                case CMD_TOTAL_COUNT:
                    Log.d(TAG, "CMD_TOTAL_COUNT:" + cmds[2]);
                    //FROM_SERVER:CMD_TOTAL_COUNT:$section_cnt
                    mDumpInfo.put(command, cmds[2]);
                    break;
                case CMD_COPY_UPDATE:
                    Log.d(TAG, "CMD_COPY_UPDATE " + cmds[3]);
                    //FROM_SERVER:CMD_COPY_UPDATE:$curr_section:$section_name:
                    // $section_size:$STATUS_COPY:$section_offset
                    String cntTmp = mDumpInfo.get(CMD_TOTAL_COUNT);
                    int cnt = DEFAULT_COUNT;
                    if (cntTmp != null) {
                        cnt = Integer.parseInt(cntTmp);
                    }
                    int idx = Integer.parseInt(cmds[2]);
                    int percent = idx * 100 / cnt;
                    final int status = Integer.parseInt(cmds[5]);
                    switch (status) {
                        case STATUS_COPYING:
                            bundle.putString(TAG_FILE_NAME, "Copying: " + cmds[3]);
                            break;
                        case STATUS_DONE:
                            bundle.putString(TAG_FILE_NAME, "Done copy: " + cmds[3]);
                            break;
                        default:
                            break;
                    }
                    mDumpInfo.put(command, cmds[2]);
                    bundle.putInt(TAG_CURRENT_INDEX, Integer.parseInt(cmds[2]));
                    bundle.putInt(TAG_TOTAL_COUNT, cnt);
                    updateNotification(percent, null, cmds[3], false);
                    sendUpdate(CMD_COPY_UPDATE, bundle);
                    break;
                case CMD_COPY_FINISHED:
                    Log.d(TAG, "CMD_COPY_FINISHED ret: " + cmds[2]);
                    /*$FROM_SERVER:$CMD_COPY_FINISHED:$STATUS_OK:$TARGET_FOLDER
                    $FROM_SERVER:$CMD_COPY_FINISHED:$EIO:$err_code */
                    final int stat = Integer.parseInt(cmds[2]);
                    switch (stat) {
                        case STATUS_OK:
                            mDumpInfo.put(command, cmds[3]);
                            bundle.putString(RamdumpService.TAG_PATH, cmds[3]);
                            updateNotification(0, "Copy successfully!",
                                    "Path:" + cmds[3], true);
                            sendUpdate(CMD_COPY_FINISHED, bundle);
                            break;
                        case EIO:
                            updateNotification(0, "Copy error!",
                                    errorToString(EIO) + " err:" + cmds[3], true);
                            bundle.putInt(TAG_ERROR, EIO);
                            sendUpdate(CMD_COPY_FINISHED, bundle);
                            break;
                        default:
                            break;
                    }
                    return;

                default:
                    break;
            }
        } else {
            Log.e(TAG, "Unknown cmd:" + cmd);
        }
    }

    public class CopyStatusReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, intent.toString());
            String cmds = intent.getStringExtra("cmd");
            mConnected = true;
            parseCmd(cmds);
        }
    }

}
