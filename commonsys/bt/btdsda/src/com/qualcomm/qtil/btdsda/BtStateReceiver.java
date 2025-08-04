/* Copyright (c) 2021-2023 Qualcomm Technologies, Inc.
 *All Rights Reserved.
 *Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package com.qualcomm.qtil.btdsda;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.content.pm.PackageManager;
import android.content.ComponentName;
import android.os.UserManager;

public class BtStateReceiver extends BroadcastReceiver {
  private static final String TAG = "BtStateReceiver";
  static final ComponentName BLUETOOTH_DSDA_SERVICE_COMPONENT = 
    new ComponentName("com.qualcomm.qtil.btdsda",
            BtDsDaService.class.getCanonicalName());

  @Override
  public void onReceive(Context context, Intent intent) {

    String action = intent.getAction();
    if(action == null) return;
    BtDsDaService DsDaServiceInstance = BtDsDaService.getInstance();
    UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
    if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
        int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter
                        .ERROR);
        if (state == BluetoothAdapter.STATE_OFF) {
            Log.d(TAG, "BtStateReceiver received BT off state intent");
            if (DsDaServiceInstance != null) {
               Log.e(TAG, "BtStateReceiver called cleanup");
               DsDaServiceInstance.CleanUp();
            }
            context.stopService(new Intent(context, BtDsDaService.class));

            context.getPackageManager().setComponentEnabledSetting(
                  BLUETOOTH_DSDA_SERVICE_COMPONENT,
                  PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                  PackageManager.DONT_KILL_APP);
        }
        else if(state == BluetoothAdapter.STATE_ON) {
            Log.d(TAG, "BtStateReceiver received BT On state intent");
            if (DsDaServiceInstance == null) {
               if (um.isUserForeground()) {
                  Log.d(TAG, "ForeGround User Turn On intent received");
                  context.getPackageManager().setComponentEnabledSetting(
                        BLUETOOTH_DSDA_SERVICE_COMPONENT,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP);
                  context.startService(new Intent(context, BtDsDaService.class));
               } else {
                 Log.d(TAG, "Not Active Foreground User.Exiting its Process");
                 System.exit(0);
               }
            }
        }
    }
    else if (action.equals(Intent.ACTION_LOCKED_BOOT_COMPLETED)) {
       Log.d(TAG, "ACTION_LOCKED_BOOT_COMPLETED intent received");
       BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
       if ((DsDaServiceInstance == null) &&
          ((bluetoothAdapter != null) &&
          (bluetoothAdapter.getState() == BluetoothAdapter.STATE_ON))) {
            Log.d(TAG, "LOCKED_BOOT_COMPLETED & BT On with No DsDa Instance");
            if (um.isUserForeground()) {
               Log.d(TAG, "ForeGround User Locked Boot intent after bt on");
               context.getPackageManager().setComponentEnabledSetting(
                     BLUETOOTH_DSDA_SERVICE_COMPONENT,
                     PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                     PackageManager.DONT_KILL_APP);
               context.startService(new Intent(context, BtDsDaService.class));
            } else {
               Log.d(TAG, "Not Active ForeGround User.Exiting its Process");
               System.exit(0);
            }
       }
    }
  }
}
