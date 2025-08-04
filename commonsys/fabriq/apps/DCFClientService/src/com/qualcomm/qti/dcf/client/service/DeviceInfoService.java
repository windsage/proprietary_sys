/**
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.dcf.client.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class DeviceInfoService extends Service {

    private static final String TAG  = "DeviceInfoService";
    private static final String QESDK_DCF_CLIENT_PACKAGE_NAME = "com.qualcomm.qti.dcf.qesdkclient";
    private static final String BT_CONNECT_PERMISSION =
            android.Manifest.permission.BLUETOOTH_CONNECT;
    private static final String LOCAL_ADDRESS_PERMISSION =
            android.Manifest.permission.LOCAL_MAC_ADDRESS;
    // wlan0 is the default wifi interface
    private static final String DEFAULT_INTERFACE = "wlan0";

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind()");
        return mBinder;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
    }

    private final IDeviceInfoService.Stub mBinder = new IDeviceInfoService.Stub() {
        @Override
        public String getBleAddress() {
            checkPermission(BT_CONNECT_PERMISSION);
            checkPermission(LOCAL_ADDRESS_PERMISSION);
            BluetoothAdapter adapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
            if (adapter != null && adapter.isEnabled()) {
                return adapter.getAddress();
            }
            return null;
        }

        @Override
        public String getWifiFactoryAddress() {
            try {
                List<NetworkInterface> interfaces = Collections.list(NetworkInterface
                        .getNetworkInterfaces());
                for (NetworkInterface intf : interfaces) {
                    if (intf.getName().equalsIgnoreCase(DEFAULT_INTERFACE)) {
                        byte[] bytes = intf.getHardwareAddress();
                        if (bytes == null) {
                            return null;
                        }

                        StringBuilder builder = new StringBuilder();
                        for (byte b : bytes) {
                            builder.append(String.format("%02x", b));
                        }

                        if (builder.length() > 0) {
                            builder.deleteCharAt(builder.length() - 1);
                        }
                        return builder.toString();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "getWifiFactoryAddress Exception: " + e.getMessage());
            }

            return null;
        }
    };

    private void checkPermission(String permission) {
        String callingPackage = getPackageManager().getNameForUid(Binder.getCallingUid());
        if (QESDK_DCF_CLIENT_PACKAGE_NAME.equals(callingPackage)) return;

        // Otherwise,explicitly check for caller permission ...
        if (checkCallingOrSelfPermission(permission) != PERMISSION_GRANTED) {
            Log.w(TAG, "Caller needs permission '" + permission + "' " + callingPackage);
            throw new SecurityException("Access denied to process: " + Binder.getCallingPid()
                    + ", must have permission " + permission);
        }
    }
}
