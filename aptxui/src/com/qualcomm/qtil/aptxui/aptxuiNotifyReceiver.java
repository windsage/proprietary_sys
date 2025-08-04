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

public class aptxuiNotifyReceiver extends BroadcastReceiver {
  private static final String TAG = "aptxuiNotifyReceiver";

  @Override
  public void onReceive(Context context, Intent intent) {
    intent.setClass(context, aptxuiNotify.class);
    aptxuiNotify.enqueueWork(context, intent);
  }
}
