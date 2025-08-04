/**
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.dcf.client.contextsync;

import static android.app.Notification.EXTRA_TEXT;

import static com.qualcomm.qti.dcf.client.DCFDataElementKt.*;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;

import com.qualcomm.qti.dcf.client.DCFAdvertiseManager;
import com.qualcomm.qti.dcf.client.DCFDataElement;
import com.qualcomm.qti.dcf.client.DCFDevice;
import com.qualcomm.qti.dcf.client.DCFDevicesListener;
import com.qualcomm.qti.dcf.client.DCFScanManager;
import com.qualcomm.qti.dcf.nearbyclient.R;
import com.qualcomm.qti.dcf.client.UserSettings;

import java.util.ArrayList;
import java.util.List;

public class ContextSyncController implements UserSettings.OnSettingsChangedListener,
        DCFDevicesListener {
    private static final String TAG = "ContextSyncController";

    public static final int DEFAULT_INTERVAL = 10;

    private static final String TIMER_ACTION = "com.dcf.intent.action.TIMER_ACTION";
    private static final String NOTIFICATION_CHANNEL_ID = "com.dcf.intent.NOTIFICATION_CHANNEL_ID";
    private static final String NOTIFICATION_CHANNEL_NAME = "Dcf_Notification";
    private static final int NOTIFICATION_ID = 10000;
    private static final String NOTIFICATION_TEXT = "This is a message from dcf client.";

    public @interface UserState {
        int ACTIVE = 0;
        int INACTIVE = 1;
        int UNKNOWN = 2;
    }

    public @interface NotificationFlag {
        int YES = 0;
        int NO = 1;
        int UNKNOWN = 2;
    }

    private Context mContext;
    private boolean mEnabled;
    private boolean mHasDCFDeviceActive;
    @UserState private int mUserState;
    @NotificationFlag private int mNotificationFlag;
    private int mNotificationInterval = DEFAULT_INTERVAL;

    private ContextSyncBroadcastReceiver mContextSyncReceiver;
    private IntentFilter mContextSyncFilter;
    private AlarmManager mAlarmManager;
    private PendingIntent mAlarmIntent;
    private NotificationManager mNotificationManager;
    private NotificationChannel mNotificationChannel;
    private Notification mNotification;

    List<DCFDataElement> mDataElementList = new ArrayList<DCFDataElement>();
    DCFDataElement mDataElement;

    private static class Instance {
        public static ContextSyncController sInstance = new ContextSyncController();
    }

    private ContextSyncController() {}

    public static ContextSyncController getInstance() {
        return Instance.sInstance;
    }

    @Override
    public void onSettingsChanged(SharedPreferences sharedPreferences, String key) {
        if (UserSettings.KEY_CONTEXT_SYNC_ENABLE.equals(key)) {
            enableContextSyncCapability(UserSettings.INSTANCE.getContextSyncEnable());
        } else if (UserSettings.KEY_SEND_NOTIFICATION_INTERVAL.equals(key)) {
            onNotificationIntervalChanged(UserSettings.INSTANCE.getNotificationInterval());
        }
    }

    @Override
    public void onDevicesChanged(List<DCFDevice> devices) {
        mHasDCFDeviceActive = hasDCFDeviceActive(devices);
        onNotificationFlagChanged();
    }

    private boolean hasDCFDeviceActive(List<DCFDevice> devices) {
        boolean hasDeviceActive = false;
        for (DCFDevice device : devices) {
            for (DCFDataElement element : device.getDataElements()) {
                if (element.getTag() == DE_TAG_USER_ACTIVITY) {
                    byte userState = element.getData()[0];
                    if (userState == (byte) (UserState.ACTIVE & 0xFF)) {
                        hasDeviceActive = true;
                        break;
                    }
                }
            }

            if (hasDeviceActive) {
                break;
            }
        }
        return hasDeviceActive;
    }

    private void onNotificationFlagChanged() {
        switch (mUserState) {
            case UserState.ACTIVE: {
                setShowNotificationFlag(NotificationFlag.YES);
                break;
            }

            case UserState.INACTIVE: {
                setShowNotificationFlag(mHasDCFDeviceActive ?
                        NotificationFlag.NO : NotificationFlag.UNKNOWN);
                break;
            }

            case UserState.UNKNOWN: {
                setShowNotificationFlag(mHasDCFDeviceActive ?
                        NotificationFlag.NO : NotificationFlag.UNKNOWN);
                break;
            }

            default:
                break;
        }
    }

    public void init(Context context) {
        mContext = context;
        mAlarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        mNotificationManager = (NotificationManager) mContext
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mContextSyncReceiver = new ContextSyncBroadcastReceiver();
        mContextSyncFilter = new IntentFilter();
        mContextSyncFilter.addAction(Intent.ACTION_SCREEN_ON);
        mContextSyncFilter.addAction(Intent.ACTION_SCREEN_OFF);
        mContextSyncFilter.addAction(TIMER_ACTION);
        mAlarmIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(TIMER_ACTION),
                PendingIntent.FLAG_IMMUTABLE);
        mNotificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
        mNotification = new Notification.Builder(mContext, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(NOTIFICATION_CHANNEL_NAME)
                .setSmallIcon(R.drawable.ic_baseline_notifications_24)
                .setDefaults(Notification.DEFAULT_ALL)
                .setPriority(Notification.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build();
        mDataElement = buildContextSyncDE();
        mDataElementList.add(mDataElement);

        setNotificationInterval(UserSettings.INSTANCE.getNotificationInterval());
        enableContextSyncCapability(UserSettings.INSTANCE.getContextSyncEnable());
        UserSettings.INSTANCE.addOnSettingsChangedListener(this);
    }

    private void enableContextSyncCapability(boolean enable) {
        Log.i(TAG, "enableContextSyncCapability: enable=" + enable);
        if (mEnabled == enable) {
            return;
        }

        mEnabled = enable;
        if (mEnabled) {
            setShowNotificationFlag(NotificationFlag.YES);
            advertiseContextSync();
            registerContextSyncReceiver();
            mNotificationManager.createNotificationChannel(mNotificationChannel);
            mAlarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + mNotificationInterval * 1000,
                    mAlarmIntent);
            DCFScanManager.getInstance().registerDCFDevicesListener(this);
        } else {
            setShowNotificationFlag(NotificationFlag.UNKNOWN);
            stopAdvertiseContextSync();
            unregisterContextSyncReceiver();
            mNotificationManager.deleteNotificationChannel(NOTIFICATION_CHANNEL_ID);
            mAlarmManager.cancel(mAlarmIntent);
            DCFScanManager.getInstance().unregisterDCFDevicesListener(this);
        }
    }

    private void setShowNotificationFlag(@NotificationFlag int flag) {
        Log.d(TAG, "setShowNotificationFlag: flag=" + flag);
        mNotificationFlag = flag;
    }

    private void advertiseContextSync() {
        DCFAdvertiseManager.getInstance().addDataElements(mDataElementList);
    }

    private void stopAdvertiseContextSync() {
        DCFAdvertiseManager.getInstance().removeDataElements(mDataElementList);
    }

    private void onUserStateChanged(@UserState int state) {
        mUserState = state;
        Log.i(TAG, "onUserStateChanged: UserState=" + mUserState);
        mDataElement.getData()[0] = (byte) (mUserState & 0xFF);
    }

    private void onNotificationIntervalChanged(int newInterval) {
        setNotificationInterval(newInterval);
        if (mEnabled) {
            resetAlarm();
        }
    }

    private void setNotificationInterval(int interval) {
        Log.d(TAG, "onNotificationIntervalChanged: interval=" + interval);
        mNotificationInterval = interval;
    }

    private void resetAlarm() {
        mAlarmManager.cancel(mAlarmIntent);
        mAlarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + mNotificationInterval * 1000,
                mAlarmIntent);
    }

    private void registerContextSyncReceiver() {
        mContext.registerReceiver(mContextSyncReceiver, mContextSyncFilter,
                Context.RECEIVER_EXPORTED);
    }

    private void unregisterContextSyncReceiver() {
        mContext.unregisterReceiver(mContextSyncReceiver);
    }

    private void sendNotification() {
        if (mNotificationFlag == NotificationFlag.YES ||
                mNotificationFlag == NotificationFlag.UNKNOWN) {
            mNotification.extras.putCharSequence(EXTRA_TEXT, System.currentTimeMillis() + ": "
                    + NOTIFICATION_TEXT);
            mNotificationManager.notify(NOTIFICATION_ID, mNotification);
        } else if (mNotificationFlag == NotificationFlag.NO) {
            Log.i(TAG, "onReceive: skip send notification");
        }
    }

    private class ContextSyncBroadcastReceiver extends BroadcastReceiver {
        private static final String TAG = "ContextSyncReceiver";
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "onReceive: action=" + action);
            if (Intent.ACTION_SCREEN_ON.equals(action)) {
                onUserStateChanged(UserState.ACTIVE);
                onNotificationFlagChanged();
                advertiseContextSync();
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                onUserStateChanged(UserState.INACTIVE);
                onNotificationFlagChanged();
                advertiseContextSync();
            } else if (ContextSyncController.TIMER_ACTION.equals(action)) {
                sendNotification();
                if (mEnabled) {
                    mAlarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                            SystemClock.elapsedRealtime() + mNotificationInterval * 1000,
                            mAlarmIntent);
                }
            }
        }
    }

    private DCFDataElement buildContextSyncDE() {
        return new DCFDataElement(DE_TAG_USER_ACTIVITY, new byte[] {(byte) (mUserState & 0xFF)});
    }
}
