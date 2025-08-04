/******************************************************************************
 *
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 ******************************************************************************/
package com.qualcomm.qtil.aptxui;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.IBinder;
import android.os.Messenger;
import android.os.UserManager;
import android.util.Log;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class aptxuiApplication extends Application {
  private static final String TAG = "aptxuiApplication";
  private static final boolean DBG = false;
  private static final boolean VDBG = false;

  Messenger mUIService = null;
  boolean mUIBound = false;
  private String mUIVersionName = "Version not found";
  private String mUIPackageName = "not available";

  private aptxuiBTControl mBTControl = null;
  private boolean mQssEnabled = false;

  private ServiceConnection mUIConnection =
      new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
          Log.i(TAG, "onServiceConnected: " + className);
          mUIService = new Messenger(service);
          mUIBound = true;
        }

        public void onServiceDisconnected(ComponentName className) {
          Log.i(TAG, "onServiceDisconnected: " + className);
          mUIService = null;
          mUIBound = false;
        }
      };

  @Override
  public void onCreate() {
    super.onCreate();
    Log.i(TAG, "onCreate");

    try {
      mUIPackageName = getPackageName();
      mUIVersionName = getPackageManager().getPackageInfo(mUIPackageName, 0).versionName;
      Log.i(TAG, "aptxui Version Name: " + mUIVersionName);
      Log.i(TAG, "aptxui Package Name: " + mUIPackageName);
    } catch (NameNotFoundException e) {
      Log.e(TAG, "Exception requesting versions: " + e.getLocalizedMessage());
    }

    try {
      if (!checkPermissions()) {
        throw new RuntimeException("Insufficient permissions to function.");
      }

      String QssEnabled = getResources().getString(R.string.qss_enabled);
      mQssEnabled = QssEnabled.equalsIgnoreCase("true");
      Log.i(TAG, "mQssEnabled: " + mQssEnabled);

      mBTControl = new aptxuiBTControl(this);
      if (mBTControl == null) {
        throw new InstantiationException("Failed to instantiate aptxuiBTControl.");
      }
      mBTControl.start();

      // Bind to UI service.
      final Intent mainServiceIntent = new Intent(this, aptxuiService.class);
      mainServiceIntent.setAction(aptxuiService.class.getName());
      bindService(mainServiceIntent, mUIConnection, BIND_AUTO_CREATE);

    } catch (Exception e) {
      Log.e(TAG, "Exception: " + e.toString());
    }
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

  /** Check if Snapdragon Sound support is enabled. */
  public boolean isQssEnabled() {
    return mQssEnabled;
  }

  /** Check if user is running in an "unlocked" state. */
  private static boolean isUserUnlocked(Context context) {
    UserManager userManager = context.getSystemService(UserManager.class);
    return userManager.isUserUnlocked();
  }

  /** Debugging information. */
  public void dump(PrintWriter pw) {
    synchronized (this) {
      pw.println("[" + TAG + "]");
      pw.println("aptxui Version Name: " + mUIVersionName);
      pw.println("aptxui Package Name: " + mUIPackageName);
      pw.println("aptxui Bound: " + mUIBound);

      if (mUIBound && isUserUnlocked(this)) {
        pw.println("");
        pw.println("Settings.");
        pw.println("QSS enabled: " + mQssEnabled);
        pw.println("");

        if (mBTControl != null) mBTControl.dump(pw);
      }
      pw.flush();
    }
  }
}
