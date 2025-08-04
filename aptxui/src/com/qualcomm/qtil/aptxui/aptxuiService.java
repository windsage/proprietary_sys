/******************************************************************************
 *
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 ******************************************************************************/
package com.qualcomm.qtil.aptxui;

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

public class aptxuiService extends Service {

  private static final String TAG = "aptxuiService";
  private static final String APTXUI_SERVICE = "com.qualcomm.qtil.aptxui.aptxuiService";
  Messenger mMessenger;

  // TODO: Possibly remove Handler as it not used.
  class IncomingHandler extends Handler {
    private Context applicationContext;

    IncomingHandler(Context context) {
      applicationContext = context.getApplicationContext();
    }

    // TODO: Possibly remove Handler as it not used.
    @Override
    public void handleMessage(Message msg) {
      Context context = getApplicationContext();
      aptxuiApplication app = (aptxuiApplication) context;
      Intent intent = (Intent) msg.obj;
      final String action = intent.getAction();
      Log.i(TAG, "action: " + action);

      switch (action) {
        default:
          super.handleMessage(msg);
      }
    }
  }

  @Override
  public IBinder onBind(Intent intent) {
    Log.i(TAG, "onBind: " + intent);
    if (APTXUI_SERVICE.equals(intent.getAction())) {
      mMessenger = new Messenger(new IncomingHandler(this));
      return mMessenger.getBinder();
    }
    return null;
  }

  /** Produce full dumpsys output: adb shell dumpsys activity service aptxuiService */
  @Override
  protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
    aptxuiApplication app = (aptxuiApplication) getApplicationContext();
    app.dump(writer);
    writer.flush();
    writer.close();
    Log.i(TAG, "dump complete");
    return;
  }
}
