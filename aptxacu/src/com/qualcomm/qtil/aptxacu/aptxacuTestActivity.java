/******************************************************************************
 *
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 ******************************************************************************/
package com.qualcomm.qtil.aptxacu;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;

/** Support ability for testing from via Android adb shell command. */

/*
// Set aptX Adaptive 96KHz sample rate preference.
adb shell am start -n com.qualcomm.qtil.aptxacu/.aptxacuTestActivity --es "SetAptxAdaptive96KHzSampleRate" "ON"
adb shell am start -n com.qualcomm.qtil.aptxacu/.aptxacuTestActivity --es "SetAptxAdaptive96KHzSampleRate" "OFF"
*/

/*
// Set aptX and aptX HD priority preference.
adb shell am start -n com.qualcomm.qtil.aptxacu/.aptxacuTestActivity --es "SetAptxAndAptxHdPriority" "DEFAULT"
adb shell am start -n com.qualcomm.qtil.aptxacu/.aptxacuTestActivity --es "SetAptxAndAptxHdPriority" "APTX"
adb shell am start -n com.qualcomm.qtil.aptxacu/.aptxacuTestActivity --es "SetAptxAndAptxHdPriority" "APTX_HD"
*/

/*
// Set audio profile override preference.
adb shell am start -n com.qualcomm.qtil.aptxacu/.aptxacuTestActivity --es "SetAudioProfileOverride" "AUTO_ADJUST"
adb shell am start -n com.qualcomm.qtil.aptxacu/.aptxacuTestActivity --es "SetAudioProfileOverride" "HIGH_QUALITY"
adb shell am start -n com.qualcomm.qtil.aptxacu/.aptxacuTestActivity --es "SetAudioProfileOverride" "GAMING_MODE"
*/

public class aptxacuTestActivity extends Activity {
  private static final String TAG = "aptxacuTestActivity";
  private aptxacuApplication mApp = null;

  private static boolean isUserUnlocked(Context context) {
    UserManager userManager = context.getSystemService(UserManager.class);
    return userManager.isUserUnlocked();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    try {
      // Check unlocked state
      if (isUserUnlocked(this)) {
        mApp = (aptxacuApplication) getApplicationContext();

        // Set aptX Adaptive 96KHz sample rate preference
        String setAptxAdaptive96KHzSampleRateKey = "SetAptxAdaptive96KHzSampleRate";
        String setAptxAdaptive96KHzSampleRateVal =
            getIntent().getExtras().getString(setAptxAdaptive96KHzSampleRateKey);
        String ON = "ON";
        String OFF = "OFF";

        if (!TextUtils.isEmpty(setAptxAdaptive96KHzSampleRateVal)) {
          if (setAptxAdaptive96KHzSampleRateVal.compareToIgnoreCase(ON) == 0) {
            Log.i(TAG, "SetAptxAdaptive96KHzSampleRate: " + setAptxAdaptive96KHzSampleRateVal);
            mApp.SetAptxAdaptive96KHzSampleRate(ON);
          } else if (setAptxAdaptive96KHzSampleRateVal.compareToIgnoreCase(OFF) == 0) {
            Log.i(TAG, "SetAptxAdaptive96KHzSampleRate: " + setAptxAdaptive96KHzSampleRateVal);
            mApp.SetAptxAdaptive96KHzSampleRate(OFF);
          } else {
            Log.e(
                TAG,
                "invalid SetAptxAdaptive96KHzSampleRate: " + setAptxAdaptive96KHzSampleRateVal);
          }
          mApp.acuAptxAdaptive96KHzSampleRate();
        }

        // Set aptX and aptX HD priority preference
        String setAptxAndAptxHdPriorityKey = "SetAptxAndAptxHdPriority";
        String setAptxAndAptxHdPriorityVal =
            getIntent().getExtras().getString(setAptxAndAptxHdPriorityKey);
        String DEFAULT = "DEFAULT";
        String APTX = "APTX";
        String APTX_HD = "APTX_HD";

        if (!TextUtils.isEmpty(setAptxAndAptxHdPriorityVal)) {
          if (setAptxAndAptxHdPriorityVal.compareToIgnoreCase(DEFAULT) == 0) {
            Log.i(TAG, "SetAptxAndAptxHdPriority: " + setAptxAndAptxHdPriorityVal);
            mApp.SetAptxAndAptxHdPriority(DEFAULT);
          } else if (setAptxAndAptxHdPriorityVal.compareToIgnoreCase(APTX) == 0) {
            Log.i(TAG, "SetAptxAndAptxHdPriority: " + setAptxAndAptxHdPriorityVal);
            mApp.SetAptxAndAptxHdPriority(APTX);
          } else if (setAptxAndAptxHdPriorityVal.compareToIgnoreCase(APTX_HD) == 0) {
            Log.i(TAG, "SetAptxAndAptxHdPriority: " + setAptxAndAptxHdPriorityVal);
            mApp.SetAptxAndAptxHdPriority(APTX_HD);
          } else {
            Log.e(TAG, "invalid SetAptxAndAptxHdPriority: " + setAptxAndAptxHdPriorityVal);
          }
          mApp.acuAptxAndAptxHdPriority();
        }

        // Set audio profile override preference
        String setAudioProfileOverrideKey = "SetAudioProfileOverride";
        String setAudioProfileOverrideVal =
            getIntent().getExtras().getString(setAudioProfileOverrideKey);
        String AUTO_ADJUST = "AUTO_ADJUST";
        String GAMING_MODE = "GAMING_MODE";
        String HIGH_QUALITY = "HIGH_QUALITY";

        if (!TextUtils.isEmpty(setAudioProfileOverrideVal)) {
          if (setAudioProfileOverrideVal.compareToIgnoreCase(AUTO_ADJUST) == 0) {
            Log.i(TAG, "SetAudioProfileOverride: " + setAudioProfileOverrideVal);
            mApp.SetAudioProfileOverride(AUTO_ADJUST);
          } else if (setAudioProfileOverrideVal.compareToIgnoreCase(GAMING_MODE) == 0) {
            Log.i(TAG, "SetAudioProfileOverride: " + setAudioProfileOverrideVal);
            mApp.SetAudioProfileOverride(GAMING_MODE);
          } else if (setAudioProfileOverrideVal.compareToIgnoreCase(HIGH_QUALITY) == 0) {
            Log.i(TAG, "SetAudioProfileOverride: " + setAudioProfileOverrideVal);
            mApp.SetAudioProfileOverride(HIGH_QUALITY);
          } else {
            Log.e(TAG, "invalid SetAudioProfileOverride: " + setAudioProfileOverrideVal);
          }
          mApp.acuAudioProfileOverride();
        }
      }
    } catch (Exception e) {
      Log.e(TAG, "Exception: " + e);
    }
    this.finish();
  }
}
