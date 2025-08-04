/*
 * Copyright (c) 2014, 2016-2019, 2021-2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.services.systemhelper;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.Handler;
import android.util.Log;
import android.os.PowerManager;
import android.telephony.TelephonyManager;
import android.provider.Settings;
import android.content.res.Resources;
import android.content.res.Configuration;

/* system helper service gathers events from the Android framework
 * and forwards them to the clients.
 *
 * This service is started on BOOT_COMPLETED
 */

public class SysHelperService extends Service implements Runnable {
  public static final String TAG = "SysHelperService";
  public static final String INTENT_CATEGORY = "android.intent.category.HOME";

  private native int init();
  private native int initAIDL();
  private native int terminate();

  public static native int sendEventNotification(int eventId);
  public static native int sendEventNotificationAIDL(int eventId);
  public native String getMessage();
  public native void sendConfirmation();
  public native void setOrientationPortrait(boolean state);

  private boolean running = false;
  private static final Object LOCK = new Object();
  private static PowerManager.WakeLock screen_wl;
  private boolean wl_init = false;
  private int sys_rotation = 0;
  private int user_rotation = 0;

  public static int PHONE_STATE_RINGING = 1;
  public static int PHONE_STATE_OFF_HOOK = 2;
  public static int PHONE_STATE_IDLE = 4;
  public static int ACTION_SCREEN_OFF = 8;
  public static int ACTION_SCREEN_ON = 16;
  public static int ACTION_SHUTDOWN = 32;
  public static int ACTION_USER_PRESENT = 64;

  static { System.loadLibrary("systemhelper_jni"); }

  public static Context context;

  public static void notifyEvent(int eventId) {
    sendEventNotification(eventId);
    sendEventNotificationAIDL(eventId);
  }

  @Override
  public void run() {
    int ret = 0, ret2 = 0;

    ret = init();
    if (ret != 0) {
        Log.e(TAG, "failed to initialise HIDL service, exiting...");
    }

    ret2 = initAIDL();
    if (ret2 != 0) {
        Log.e(TAG, "failed to initialise AIDL service, exiting...");
    }

    // Check if either AIDL or HIDL systemhelper service instances are started on service bootup.
    if (ret != 0 && ret2 != 0) {
        return;
    }

    running = true;

    CallReceiver mCallReceiver = new CallReceiver();
    IntentFilter filter2 =
        new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
    filter2.addCategory(INTENT_CATEGORY);
    registerReceiver(mCallReceiver, filter2);

    PowerReceiver mPowerReceiver = new PowerReceiver();
    IntentFilter filter1 = new IntentFilter(Intent.ACTION_SHUTDOWN);
    filter1.addCategory(INTENT_CATEGORY);
    registerReceiver(mPowerReceiver, filter1);

    ScreenReceiver mScreenReceiver = new ScreenReceiver();
    IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
    filter.addAction(Intent.ACTION_SCREEN_ON);
    filter.addAction(Intent.ACTION_USER_PRESENT);
    registerReceiver(mScreenReceiver, filter);

    UserSwitchReceiver mUserSwitchReceiver = new UserSwitchReceiver();
    IntentFilter filter3 = new IntentFilter(Intent.ACTION_USER_FOREGROUND);
    filter3.addAction(Intent.ACTION_USER_BACKGROUND);
    filter3.addAction(Intent.ACTION_USER_SWITCHED);
    registerReceiver(mUserSwitchReceiver, filter3);

    while (true) {
      String str = getMessage();

      if (!wl_init) {
        // Initialize the wake lock.
        // The wakelock can be used to prevent android from dimming
        // or turning off the dispay during secure UI session.
        // The responsibility can be moved into application/other service as needed
        PowerManager pm =
          (PowerManager)context.getSystemService(Context.POWER_SERVICE);
          screen_wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
                                     PowerManager.ON_AFTER_RELEASE, TAG);
        wl_init = true;
      }

      switch (str) {
      case "WL_AQUIRE":
        screen_wl.acquire();
        break;

      case "WL_RELEASE":
        screen_wl.release();
        break;

      case "ROT_LOCK":
        lockOrientation(true);
        break;

      case "ROT_UNLOCK":
        lockOrientation(false);
        break;

      default:
        Log.e(TAG, "requested resource is invalid");
      }
      sendConfirmation();
    }
  }

  public void lockOrientation(boolean lock) {
    if (lock == true) {
      try {
        sys_rotation = Settings.System.getInt(
            getContentResolver(), Settings.System.ACCELEROMETER_ROTATION);
        user_rotation = Settings.System.getInt(
            getContentResolver(), Settings.System.USER_ROTATION);
        Settings.System.putInt(getContentResolver(),
                               Settings.System.ACCELEROMETER_ROTATION, 0);
        Settings.System.putInt(getContentResolver(),
                               Settings.System.USER_ROTATION, 0);
      } catch (Settings.SettingNotFoundException ex) {
        Log.e(TAG, "Orientation setting not found");
      }
    } else {
      Settings.System.putInt(getContentResolver(),
                             Settings.System.ACCELEROMETER_ROTATION,
                             sys_rotation);
      Settings.System.putInt(getContentResolver(),
                             Settings.System.USER_ROTATION,
                             user_rotation);
    }
  }

  @Override
  public void onCreate() {
    super.onCreate();
    SysHelperService.context = this;
    if (!running) {
      new Thread(this).start();
    }
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.d(TAG, "Service started flags " + flags + " startId " + startId);

    // We want this service to continue running until it is explicitly
    // stopped, so return sticky.
    return START_STICKY;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    screen_wl.release();
    wl_init = false;
    terminate();
    Log.d(TAG, "onDestroy");
  }

  @Override
  public IBinder onBind(Intent intent) {
    Log.d(TAG, "onBind");
    return null;
  }

  public void onConfigurationChanged(Configuration configuration) {
    super.onConfigurationChanged(configuration);
    int currentDeviceRotation = getResources().getConfiguration().orientation;
    if (currentDeviceRotation == Configuration.ORIENTATION_PORTRAIT) {
        setOrientationPortrait(true);
    } else if (currentDeviceRotation == Configuration.ORIENTATION_LANDSCAPE) {
        setOrientationPortrait(false);
    }
  }
}
