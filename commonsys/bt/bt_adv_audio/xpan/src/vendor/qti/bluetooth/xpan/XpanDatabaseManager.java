/*****************************************************************
 * Copyright (c) 2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 ******************************************************************/

package vendor.qti.bluetooth.xpan;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ContentValues;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import vendor.qti.bluetooth.xpan.XpanDatabaseContract.DeviceData;

import java.util.Set;

public class XpanDatabaseManager {
    private static final String TAG = "XpanDatabaseManager";
    private static final boolean VDBG = XpanUtils.VDBG;
    private final XpanProfileService mService;
    private BluetoothAdapter mBtAdapter;

    public XpanDatabaseManager(XpanProfileService service) {
        mService = service;
        mBtAdapter = service.getXpanUtils().getBluetoothAdapter();
    }

    private boolean isDeviceBonded(BluetoothDevice device) {
        Set<BluetoothDevice> bondedDevices = mBtAdapter.getBondedDevices();
        boolean bonded = bondedDevices != null && bondedDevices.contains(device);
        if(VDBG) {
            Log.v(TAG, "isDeviceBonded: device=" + device + ", bonded=" + bonded);
        }
        return bonded;
    }

    public void updateDeviceBearerToDb(BluetoothDevice device, int bearer) {
        if (VDBG) {
            Log.v(TAG, "updateDeviceBearerToDb: device=" + device + ", bearerInt=" + bearer);
        }
        String bearerStr = XpanDatabaseContract.DeviceData.bearer2Str(bearer);
        if (bearerStr != null) {
            updateDeviceBearerToDb(device, bearerStr);
        }
    }

    public void updateDeviceBearerToDb(BluetoothDevice device, String bearer) {
        if (VDBG) {
            Log.v(TAG, "updateDeviceBearerToDb: device=" + device + ", bearer=" + bearer);
        }
        if (!isDeviceBonded(device))
            return;

        String address = device.getAddress();
        Uri uri = XpanDatabaseContract.CONTENT_URI_DEVICE_DATA;
        Uri uriRet = null;

        ContentValues values = new ContentValues();
        values.put(DeviceData.Columns.ADDRESS, address);
        values.put(DeviceData.Columns.BEARER, bearer);
        int count = mService.getContentResolver().update(uri,
                values, DeviceData.Columns.ADDRESS + "=?", new String[]{address});
        if (count == 0) {
            uriRet = mService.getContentResolver().insert(uri, values);
        }
        if (VDBG) {
            Log.d(TAG, "updateDeviceBearerToDb: count=" + count + ", uriRet=" + uriRet);
        }
    }

    public void removeDeviceFromDb(BluetoothDevice device) {
        String address = device.getAddress();
        Uri uri = XpanDatabaseContract.CONTENT_URI_DEVICE_DATA;

        int count = mService.getContentResolver().delete(uri,
                DeviceData.Columns.ADDRESS + "=?", new String[]{address});
        if (VDBG) {
            Log.d(TAG, "removeDeviceFromDb " + device + "  removed " + count);
        }
    }

    public void updateDeviceWhcSupportedToDb(BluetoothDevice device, boolean supported) {
        if (VDBG) {
            Log.v(TAG, "updateDeviceWhcSupportedToDb: device=" + device +
                    ", supported=" + supported);
        }

        if (!isDeviceBonded(device))
            return;

        String address = device.getAddress();
        Uri uri = XpanDatabaseContract.CONTENT_URI_DEVICE_DATA;
        Uri uriRet = null;

        ContentValues values = new ContentValues();
        values.put(DeviceData.Columns.ADDRESS, address);
        values.put(DeviceData.Columns.WHC_SUPPORTED,
                supported ? DeviceData.WHC_SUPPORTED_YES : DeviceData.WHC_SUPPORTED_NO);
        int count = mService.getContentResolver().update(uri,
                values, DeviceData.Columns.ADDRESS + "=?", new String[]{address});
        if (count == 0) {
            uriRet = mService.getContentResolver().insert(uri, values);
        }

        if (VDBG) {
            Log.v(TAG, "updateDeviceWhcSupportedToDb: count=" + count + ", uriRet=" + uriRet);
        }
    }
}
