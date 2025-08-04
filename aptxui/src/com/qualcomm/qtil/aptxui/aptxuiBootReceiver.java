/******************************************************************************
 *
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 ******************************************************************************/
package com.qualcomm.qtil.aptxui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class aptxuiBootReceiver extends BroadcastReceiver {
  private static final String TAG = "aptxuiBootReceiver";

  @Override
  public void onReceive(Context context, Intent intent) {
    String action = intent.getAction();
    Log.i(TAG, "onReceive action: " + action);

    switch (action) {
      case Intent.ACTION_BOOT_COMPLETED:
      case Intent.ACTION_LOCKED_BOOT_COMPLETED:
        // Subsequently, application will now get launched automatically
        break;
      default:
        Log.e(TAG, "Received unexpected action:" + action);
        break;
    }
  }
}
