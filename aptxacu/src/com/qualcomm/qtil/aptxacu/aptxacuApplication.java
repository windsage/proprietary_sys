/******************************************************************************
 *
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 ******************************************************************************/
package com.qualcomm.qtil.aptxacu;

import android.app.Activity;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.UserManager;
import android.preference.PreferenceManager;
import android.util.Log;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class aptxacuApplication extends Application {
  private static final String TAG = "aptxacuApplication";
  private static final boolean DBG = false;

  Messenger mACUService = null;
  Messenger mALSService = null;
  boolean mACUBound = false;
  boolean mALSBound = false;

  private String mALSAppPackageName = "com.qualcomm.qtil.aptxals";
  private String mALSAppServiceClass = "aptxalsService";
  private String mVersionName = "";
  private String mPackageName = "";

  private static final int EVENT_ACU_APTX_AND_APTX_HD_PRIORITY = 2;
  private static final int EVENT_ACU_APTX_ADAPTIVE_96KHZ_SAMPLE_RATE = 3;
  private static final int EVENT_ACU_AUDIO_PROFILE_OVERRIDE = 4;
  private static final int EVENT_ACU_APP_AUDIO_PROFILE_PREFERENCE_LIST = 5;
  private static final int EVENT_ALS_PREFERENCES_UPDATED = 6;
  private static final int ALS_RETRY_ATTEMPTS = 5;
  private static final int ALS_RETRY_DELAY_MS = 1000;

  private final aptxacuApplicationHandler mHandler = new aptxacuApplicationHandler();
  private List<OnStateChangedListener> mListener = new ArrayList<OnStateChangedListener>();

  private final ActivityLifecycleCallbacks mCallbacks =
      new ActivityLifecycleCallbacks() {

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
          if (DBG) Log.d(TAG, "onActivityCreated: " + activity.getComponentName().getClassName());
        }

        @Override
        public void onActivityStarted(Activity activity) {
          if (DBG) Log.d(TAG, "onActivityStarted: " + activity.getComponentName().getClassName());
          // Bind to service when activity is started.
          doBindService();
        }

        @Override
        public void onActivityResumed(Activity activity) {
          if (DBG) Log.d(TAG, "onActivityResumed: " + activity.getComponentName().getClassName());
        }

        @Override
        public void onActivityPaused(Activity activity) {
          if (DBG) Log.d(TAG, "onActivityPaused: " + activity.getComponentName().getClassName());
        }

        @Override
        public void onActivityStopped(Activity activity) {
          if (DBG) Log.d(TAG, "onActivityStopped: " + activity.getComponentName().getClassName());
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
          if (DBG)
            Log.d(
                TAG, "onActivitySaveInstanceState: " + activity.getComponentName().getClassName());
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
          if (DBG) Log.d(TAG, "onActivityDestroyed: " + activity.getComponentName().getClassName());
          // Unbind to service when activity is destroyed.
          doUnbindService();
        }
      };

  // ACU service connection implementation.
  private ServiceConnection mACUServiceConnection =
      new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
          Log.i(TAG, "onServiceConnected: " + className);
          mACUService = new Messenger(service);
          mACUBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
          Log.i(TAG, "onServiceDisconnected: " + className);
          mACUService = null;
          mACUBound = false;
        }
      };

  // ALS service connection implementation.
  private ServiceConnection mALSConnection =
      new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
          Log.i(TAG, "onServiceConnected: " + className);
          mALSService = new Messenger(service);
          mALSBound = true;
        }

        public void onServiceDisconnected(ComponentName className) {
          Log.i(TAG, "onServiceDisconnected: " + className);
          mALSService = null;
          mALSBound = false;
        }
      };

  @Override
  public void onCreate() {
    super.onCreate();
    Log.i(TAG, "onCreate");

    // Print app application info.
    printVersionInfo();

    try {
      if (!checkPermissions()) {
        throw new RuntimeException("Insufficient permissions to function.");
      }
    } catch (Exception e) {
      Log.e(TAG, "Exception: " + e.toString());
    }

    // Bind to ACU service.
    try {
      final Intent mainServiceIntent = new Intent(this, aptxacuService.class);
      mainServiceIntent.setAction(aptxacuService.class.getName());
      bindService(mainServiceIntent, mACUServiceConnection, BIND_AUTO_CREATE);
    } catch (SecurityException e) {
      Log.e(TAG, "Can't bind to aptxacuService");
    }

    // Register activity lifecycle callbacks.
    registerActivityLifecycleCallbacks(mCallbacks);
  }

  /** Check that all permissions listed in manifest file are granted. */
  private boolean checkPermissions() {
    List<String> notGrantedList = new ArrayList<>();
    try {
      PackageInfo pkgInfo =
          getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_PERMISSIONS);
      for (String perm : pkgInfo.requestedPermissions) {
        if (DBG) Log.d(TAG, "Checking granting for " + perm);
        if (checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) {
          notGrantedList.add(perm);
        }
      }
    } catch (PackageManager.NameNotFoundException e) {
      Log.e(TAG, "Fail to get requestedPermissions " + e.getMessage());
    }

    if (!notGrantedList.isEmpty()) {
      for (String perm : notGrantedList) {
        Log.w(TAG, "Not granted for " + perm);
      }
      return false;
    }
    return true;
  }

  private void printVersionInfo() {
    PackageInfo packageInfo;
    try {
      packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
      if (packageInfo != null) {
        mPackageName = packageInfo.packageName;
        mVersionName = packageInfo.versionName;
        Log.i(
            TAG, "printVersionInfo packageName: " + mPackageName + " versionName: " + mVersionName);
      }
    } catch (NameNotFoundException e) {
      Log.e(TAG, "NameNotFoundException occurred while retrieving packageInfo: " + e.getMessage());
    }
  }

  private static boolean isUserUnlocked(Context context) {
    UserManager userManager = context.getSystemService(UserManager.class);
    return userManager.isUserUnlocked();
  }

  private void doBindService() {
    if (!mALSBound) {
      try {
        Log.i(TAG, "doBindService bind to " + mALSAppPackageName + "." + mALSAppServiceClass);
        Intent intent = new Intent();
        intent.setAction(mALSAppPackageName + "." + mALSAppServiceClass);
        intent.setComponent(
            new ComponentName(mALSAppPackageName, mALSAppPackageName + "." + mALSAppServiceClass));
        bindService(intent, mALSConnection, Context.BIND_AUTO_CREATE);
      } catch (SecurityException e) {
        Log.e(TAG, "doBindService can't bind to " + mALSAppPackageName + "." + mALSAppServiceClass);
      }
    }
  }

  private void doUnbindService() {
    if (mALSBound) {
      Log.i(TAG, "doUnbindService mALSConnection");
      // Unbind existing ALS connection.
      unbindService(mALSConnection);
      mALSService = null;
      mALSBound = false;
    }
  }

  public String GetAptxAndAptxHdPriority() {
    String value = "DEFAULT";
    try {
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
      value = prefs.getString(aptxacuALSDefs.APTX_AND_APTX_HD_PRIORITY, "DEFAULT");
    } catch (Exception e) {
      Log.e(TAG, "Exception: " + e);
    }
    return value;
  }

  public String GetAptxAdaptive96KHzSampleRate() {
    String value = "OFF";
    try {
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
      value = prefs.getString(aptxacuALSDefs.APTX_ADAPTIVE_96KHZ_SAMPLE_RATE, "OFF");
    } catch (Exception e) {
      Log.e(TAG, "Exception: " + e);
    }
    return value;
  }

  public String GetAudioProfileOverride() {
    String value = "AUTO_ADJUST";
    try {
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
      value = prefs.getString(aptxacuALSDefs.AUDIO_PROFILE_OVERRIDE, "AUTO_ADJUST");
    } catch (Exception e) {
      Log.e(TAG, "Exception: " + e);
    }
    return value;
  }

  public String GetAppAudioProfilePreferenceList() {
    String value = "";
    try {
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
      value = prefs.getString(aptxacuALSDefs.APP_AUDIO_PROFILE_PREFERENCE_LIST, "");
    } catch (Exception e) {
      Log.e(TAG, "Exception: " + e);
    }
    return value;
  }

  public void SetAptxAndAptxHdPriority(String value) {
    try {
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
      Editor editor = prefs.edit();
      editor.putString(aptxacuALSDefs.APTX_AND_APTX_HD_PRIORITY, value);
      editor.commit();
    } catch (Exception e) {
      Log.e(TAG, "Exception: " + e);
    }
  }

  public void SetAptxAdaptive96KHzSampleRate(String value) {
    try {
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
      Editor editor = prefs.edit();
      editor.putString(aptxacuALSDefs.APTX_ADAPTIVE_96KHZ_SAMPLE_RATE, value);
      editor.commit();
    } catch (Exception e) {
      Log.e(TAG, "Exception: " + e);
    }
  }

  public void SetAudioProfileOverride(String value) {
    try {
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
      Editor editor = prefs.edit();
      editor.putString(aptxacuALSDefs.AUDIO_PROFILE_OVERRIDE, value);
      editor.commit();
    } catch (Exception e) {
      Log.e(TAG, "Exception: " + e);
    }
  }

  public void SetAppAudioProfilePreferenceList(String value) {
    try {
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
      Editor editor = prefs.edit();
      editor.putString(aptxacuALSDefs.APP_AUDIO_PROFILE_PREFERENCE_LIST, value);
      editor.commit();
      acuAudioProfilePreferenceListUpdated();
    } catch (Exception e) {
      Log.e(TAG, "Exception: " + e);
    }
  }

  public void alsPreferencesUpdated(Intent intent) {
    intent.setComponent(
        new ComponentName(mALSAppPackageName, mALSAppPackageName + "." + mALSAppServiceClass));
    Message msg = mHandler.obtainMessage(EVENT_ALS_PREFERENCES_UPDATED);
    msg.obj = intent;
    mHandler.sendMessage(msg);
  }

  public void acuAptxAndAptxHdPriority() {
    Message msg = mHandler.obtainMessage(EVENT_ACU_APTX_AND_APTX_HD_PRIORITY);
    mHandler.sendMessage(msg);
  }

  public void acuAptxAdaptive96KHzSampleRate() {
    Message msg = mHandler.obtainMessage(EVENT_ACU_APTX_ADAPTIVE_96KHZ_SAMPLE_RATE);
    mHandler.sendMessage(msg);
  }

  public void acuAudioProfileOverride() {
    Message msg = mHandler.obtainMessage(EVENT_ACU_AUDIO_PROFILE_OVERRIDE);
    mHandler.sendMessage(msg);
  }

  public void acuAudioProfilePreferenceListUpdated() {
    Message msg = mHandler.obtainMessage(EVENT_ACU_APP_AUDIO_PROFILE_PREFERENCE_LIST);
    mHandler.sendMessage(msg);
  }

  public void registerOnStateChangedListener(OnStateChangedListener listener) {
    mListener.add(listener);
  }

  public void unregisterOnStateChangedListener(OnStateChangedListener settingsScreen) {
    mListener.remove(settingsScreen);
  }

  private void updateListeners() {
    for (OnStateChangedListener settingsScreen : mListener) {
      settingsScreen.onStateChanged(this);
    }
  }

  public interface OnStateChangedListener {
    public void onStateChanged(aptxacuApplication app);
  }

  private class aptxacuApplicationHandler extends Handler {
    @Override
    public void handleMessage(Message msg) {
      Intent intent = (Intent) msg.obj;
      Context context = getApplicationContext();

      switch (msg.what) {
        case EVENT_ACU_APTX_AND_APTX_HD_PRIORITY:
          {
            if (mALSBound) {
              Log.i(TAG, "EVENT_ACU_APTX_AND_APTX_HD_PRIORITY mALSBound: " + mALSBound);
              Intent intentALS = new Intent(context, aptxacuService.class);
              intentALS.setAction(aptxacuALSDefs.ACTION_ACU_APTX_AND_APTX_HD_PRIORITY);
              intentALS.putExtra(
                  aptxacuALSDefs.APTX_AND_APTX_HD_PRIORITY, GetAptxAndAptxHdPriority());

              Message msgALS = obtainMessage(aptxacuALSDefs.MESSAGE_TYPE_ALS);
              msgALS.obj = intentALS;
              try {
                mALSService.send(msgALS);
              } catch (RemoteException e) {
                e.printStackTrace();
              }
            } else {
              int attempt = msg.arg1;
              if (attempt <= ALS_RETRY_ATTEMPTS) {
                doBindService();
                removeMessages(EVENT_ACU_APTX_AND_APTX_HD_PRIORITY);

                Message msg_delayed = obtainMessage(EVENT_ACU_APTX_AND_APTX_HD_PRIORITY);
                msg_delayed.arg1 = attempt + 1;
                Log.i(TAG, "EVENT_ACU_APTX_AND_APTX_HD_PRIORITY msg_delayed: " + msg_delayed);
                sendMessageDelayed(msg_delayed, ALS_RETRY_DELAY_MS);
              } else {
                Log.e(
                    TAG,
                    "Failed to bind to "
                        + mALSAppPackageName
                        + "."
                        + mALSAppServiceClass
                        + " after "
                        + ALS_RETRY_ATTEMPTS
                        + " attempts");
              }
            }
            break;
          }

        case EVENT_ACU_APTX_ADAPTIVE_96KHZ_SAMPLE_RATE:
          {
            if (mALSBound) {
              Log.i(TAG, "EVENT_ACU_APTX_ADAPTIVE_96KHZ_SAMPLE_RATE mALSBound: " + mALSBound);
              Intent intentALS = new Intent(context, aptxacuService.class);
              intentALS.setAction(aptxacuALSDefs.ACTION_ACU_APTX_ADAPTIVE_96KHZ_SAMPLE_RATE);
              intentALS.putExtra(
                  aptxacuALSDefs.APTX_ADAPTIVE_96KHZ_SAMPLE_RATE, GetAptxAdaptive96KHzSampleRate());

              Message msgALS = obtainMessage(aptxacuALSDefs.MESSAGE_TYPE_ALS);
              msgALS.obj = intentALS;
              try {
                mALSService.send(msgALS);
              } catch (RemoteException e) {
                e.printStackTrace();
              }
            } else {
              int attempt = msg.arg1;
              if (attempt <= ALS_RETRY_ATTEMPTS) {
                doBindService();
                removeMessages(EVENT_ACU_APTX_ADAPTIVE_96KHZ_SAMPLE_RATE);

                Message msg_delayed = obtainMessage(EVENT_ACU_APTX_ADAPTIVE_96KHZ_SAMPLE_RATE);
                msg_delayed.arg1 = attempt + 1;
                Log.i(TAG, "EVENT_ACU_APTX_ADAPTIVE_96KHZ_SAMPLE_RATE msg_delayed: " + msg_delayed);
                sendMessageDelayed(msg_delayed, ALS_RETRY_DELAY_MS);
              } else {
                Log.e(
                    TAG,
                    "Failed to bind to "
                        + mALSAppPackageName
                        + "."
                        + mALSAppServiceClass
                        + " after "
                        + ALS_RETRY_ATTEMPTS
                        + " attempts");
              }
            }
            break;
          }

        case EVENT_ACU_AUDIO_PROFILE_OVERRIDE:
          {
            if (mALSBound) {
              Log.i(TAG, "EVENT_ACU_AUDIO_PROFILE_OVERRIDE mALSBound: " + mALSBound);
              Intent intentALS = new Intent(context, aptxacuService.class);
              intentALS.setAction(aptxacuALSDefs.ACTION_ACU_AUDIO_PROFILE_OVERRIDE);
              intentALS.putExtra(aptxacuALSDefs.AUDIO_PROFILE_OVERRIDE, GetAudioProfileOverride());

              Message msgALS = obtainMessage(aptxacuALSDefs.MESSAGE_TYPE_ALS);
              msgALS.obj = intentALS;
              try {
                mALSService.send(msgALS);
              } catch (RemoteException e) {
                e.printStackTrace();
              }
            } else {
              int attempt = msg.arg1;
              if (attempt <= ALS_RETRY_ATTEMPTS) {
                doBindService();
                removeMessages(EVENT_ACU_AUDIO_PROFILE_OVERRIDE);
                Message msg_delayed = obtainMessage(EVENT_ACU_AUDIO_PROFILE_OVERRIDE);
                msg_delayed.arg1 = attempt + 1;
                Log.i(TAG, "EVENT_ACU_AUDIO_PROFILE_OVERRIDE msg_delayed: " + msg_delayed);
                sendMessageDelayed(msg_delayed, ALS_RETRY_DELAY_MS);
              } else {
                Log.e(
                    TAG,
                    "Failed to bind to "
                        + mALSAppPackageName
                        + "."
                        + mALSAppServiceClass
                        + " after "
                        + ALS_RETRY_ATTEMPTS
                        + " attempts");
              }
            }
            break;
          }

        case EVENT_ACU_APP_AUDIO_PROFILE_PREFERENCE_LIST:
          {
            if (mALSBound) {
              Log.i(TAG, "EVENT_ACU_APP_AUDIO_PROFILE_PREFERENCE_LIST mALSBound: " + mALSBound);
              Intent intentALS = new Intent(context, aptxacuService.class);
              intentALS.setAction(aptxacuALSDefs.ACTION_ACU_APP_AUDIO_PROFILE_PREFERENCE_LIST);
              intentALS.putExtra(
                  aptxacuALSDefs.APP_AUDIO_PROFILE_PREFERENCE_LIST,
                  GetAppAudioProfilePreferenceList());
              Message msgALS = obtainMessage(aptxacuALSDefs.MESSAGE_TYPE_ALS);
              msgALS.obj = intentALS;
              try {
                mALSService.send(msgALS);
              } catch (RemoteException e) {
                e.printStackTrace();
              }
            } else {
              int attempt = msg.arg1;
              if (attempt <= ALS_RETRY_ATTEMPTS) {
                doBindService();
                removeMessages(EVENT_ACU_APP_AUDIO_PROFILE_PREFERENCE_LIST);
                Message msg_delayed = obtainMessage(EVENT_ACU_APP_AUDIO_PROFILE_PREFERENCE_LIST);
                msg_delayed.arg1 = attempt + 1;
                Log.i(
                    TAG, "EVENT_ACU_APP_AUDIO_PROFILE_PREFERENCE_LIST msg_delayed: " + msg_delayed);
                sendMessageDelayed(msg_delayed, ALS_RETRY_DELAY_MS);
              } else {
                Log.e(
                    TAG,
                    "Failed to bind to "
                        + mALSAppPackageName
                        + "."
                        + mALSAppServiceClass
                        + " after "
                        + ALS_RETRY_ATTEMPTS
                        + " attempts");
              }
            }
            break;
          }

        case EVENT_ALS_PREFERENCES_UPDATED:
          {
            if (intent == null) {
              Log.e(TAG, "missing intent");
              return;
            }

            String aptx_and_aptx_hd_priority =
                intent.getStringExtra(aptxacuALSDefs.APTX_AND_APTX_HD_PRIORITY);
            String aptx_adaptive_96khz_sample_rate =
                intent.getStringExtra(aptxacuALSDefs.APTX_ADAPTIVE_96KHZ_SAMPLE_RATE);
            String audio_profile_override =
                intent.getStringExtra(aptxacuALSDefs.AUDIO_PROFILE_OVERRIDE);

            SetAptxAndAptxHdPriority(aptx_and_aptx_hd_priority);
            SetAptxAdaptive96KHzSampleRate(aptx_adaptive_96khz_sample_rate);
            SetAudioProfileOverride(audio_profile_override);

            // Setting Fragment, if shown will have registered to receive updates.
            updateListeners();
            break;
          }

        default:
          break;
      }
    }
  }

  /** Debugging information. */
  public void dump(PrintWriter pw) {
    synchronized (this) {
      pw.println("[" + TAG + "]");
      pw.println("aptxacu packageName: " + mPackageName + " versionName: " + mVersionName);
      pw.println("aptxacu Bound: " + mACUBound);

      if (mACUBound && isUserUnlocked(this)) {
        pw.println("");
        pw.println("User Preferences.");
        pw.println("Set aptX and aptX HD priority: " + GetAptxAndAptxHdPriority());
        pw.println("Enable use 96KHz sample rate: " + GetAptxAdaptive96KHzSampleRate());
        pw.println("Set audio profile override: " + GetAudioProfileOverride());
        pw.println("Get app audio profile preference list: " + GetAppAudioProfilePreferenceList());
      }
      pw.flush();
    }
  }
}
