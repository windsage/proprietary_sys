/******************************************************************************
 *
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 ******************************************************************************/
package com.qualcomm.qtil.aptxacu;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public class aptxacuSettingsActivity extends AppCompatActivity {
  private static final String TAG = "aptxacuSettingsActivity";
  private static final boolean DBG = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.i(TAG, "onCreate");

    // Set the system bar insets to be ignored by the window decor.
    setupSystemBarInsets();

    Context context = getApplicationContext();
    String title = context.getString(R.string.acu_label);
    setTitle(title);

    // Display the fragment as the main content.
    getSupportFragmentManager()
        .beginTransaction()
        .replace(android.R.id.content, new aptxacuSettingsFragment())
        .commit();
  }

  @Override
  public void onStart() {
    super.onStart();
    if (DBG) Log.d(TAG, "onStart");
  }

  @Override
  public void onRestart() {
    super.onRestart();
    if (DBG) Log.d(TAG, "onRestart");
  }

  @Override
  public void onResume() {
    super.onResume();
    if (DBG) Log.d(TAG, "onResume");
  }

  @Override
  public void onPause() {
    super.onPause();
    if (DBG) Log.d(TAG, "onPause");
  }

  @Override
  public void onStop() {
    super.onStop();
    if (DBG) Log.d(TAG, "onStop");
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (DBG) Log.d(TAG, "onDestroy");
  }

  /**
   * Setup the system bar insets.
   *
   * <p>This method is called in onCreate() to set the system bar insets to be ignored by the window
   * decor.
   */
  private void setupSystemBarInsets() {
    // Tell the window decor to ignore system bars (e.g. status bar, navigation bar)
    WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

    // Set a listener to apply window insets to the content view
    ViewCompat.setOnApplyWindowInsetsListener(
        findViewById(android.R.id.content),
        (v, windowInsets) -> {
          // Get the system bar insets (e.g. status bar, navigation bar)
          Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
          // Apply the insets as padding to the content view
          v.setPadding(insets.left, insets.top, insets.right, insets.bottom);

          // Consume the insets to prevent them from being applied to other views
          return WindowInsetsCompat.CONSUMED;
        });
  }
}
