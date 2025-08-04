/******************************************************************************
 *
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 ******************************************************************************/
package com.qualcomm.qtil.aptxacu;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class aptxacuService extends Service {
  private static final String TAG = "aptxacuService";
  private static final boolean DBG = false;

  private static final String APTXACU_SERVICE = "com.qualcomm.qtil.aptxacu.aptxacuService";
  Messenger mMessenger;

  class IncomingHandler extends Handler {
    private Context applicationContext;

    IncomingHandler(Context context) {
      Log.i(TAG, "IncomingHandler");
      applicationContext = context.getApplicationContext();
    }

    @Override
    public void handleMessage(Message msg) {
      Context context = getApplicationContext();
      aptxacuApplication app = (aptxacuApplication) context;
      Intent intent = (Intent) msg.obj;
      final String action = intent.getAction();

      switch (action) {
        case aptxacuALSDefs.ACTION_ALS_PREFERENCES_UPDATED:
          Log.i(TAG, "handleMessage ACTION_ALS_PREFERENCES_UPDATED");
          app.alsPreferencesUpdated(intent);
          break;
        default:
          Log.i(TAG, "handleMessage unknown action: " + action);
          super.handleMessage(msg);
      }
    }
  }

  @Override
  public IBinder onBind(Intent intent) {
    Log.i(TAG, "onBind: " + intent);
    if (APTXACU_SERVICE.equals(intent.getAction())) {
      mMessenger = new Messenger(new IncomingHandler(this));
      return mMessenger.getBinder();
    }
    return null;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    if (DBG) Log.d(TAG, "onCreate");
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (DBG) Log.d(TAG, "onDestroy");
  }

  /** Produce full dumpsys output: adb shell dumpsys activity service aptxacuService */
  @Override
  protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
    aptxacuApplication app = (aptxacuApplication) getApplicationContext();
    app.dump(writer);
    writer.flush();
    writer.close();
    Log.i(TAG, "dump complete");
    return;
  }
}
