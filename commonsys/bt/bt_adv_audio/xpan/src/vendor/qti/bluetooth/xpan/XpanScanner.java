/*****************************************************************
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 ******************************************************************/
package vendor.qti.bluetooth.xpan;

import java.util.ArrayList;
import java.util.List;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class XpanScanner {

    private final String TAG = "XpanScanner";
    private final boolean DBG = true;
    private final boolean VDBG = true;

    private List<BluetoothDevice> mDeviceList = new ArrayList<BluetoothDevice>();
    private BluetoothLeScanner mScanner;
    private XpanScanCallBack mCallBack;
    private ScanHandler mScanHandler;
    private XpanUtils mUtils;
    private XpanProfileService mService;
    private boolean mScanResolution = false;

    private enum ScannerMsg {
        HANDLE_LE_RESULT, START_SCAN, STOP_SCAN
    }

    private class XpanScanCallBack extends ScanCallback {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (VDBG)
                Log.d(TAG, "onScanResult " + result + " type " + callbackType);
            mScanHandler.sendMessage(
                    mScanHandler.obtainMessage(parse(ScannerMsg.HANDLE_LE_RESULT), result));
        }

        @Override
        public void onScanFailed(int errorCode) {
            if (VDBG)
                Log.d(TAG, "onScanFailed " + errorCode);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            if (VDBG)
                Log.d(TAG, "onBatchScanResults " + results);
        }
    }

    private class ScanHandler extends Handler {
        ScanHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            ScannerMsg opcode = ScannerMsg.values()[msg.what];
            if (VDBG)
                Log.v(TAG, "handleMessage " + opcode);
            switch (opcode) {
            case START_SCAN:
                handelStartScan((BluetoothDevice) msg.obj);
                break;
            case STOP_SCAN:
                Object obj = msg.obj;
                handleStopScan(obj);
                break;
            case HANDLE_LE_RESULT:
                ScanResult result = (ScanResult) msg.obj;
                deviceFound(result);
                break;
            default:
                break;
            }
        }
    }

    XpanScanner(XpanProfileService service) {
        mService = service;
        mUtils = service.getXpanUtils();
        mCallBack = new XpanScanCallBack();
        mScanHandler = new ScanHandler(service.getLooper());
        mScanner = mUtils.getBluetoothAdapter().getBluetoothLeScanner();
        if (VDBG)
            Log.v(TAG, TAG);
    }

    boolean startScan(BluetoothDevice device) {
        if (DBG)
            Log.d(TAG, "startScan " + device);
        if (mScanResolution) {
            if (VDBG)
                Log.v(TAG, "startScan already in progress " + device);
            return false;
        }
        return mScanHandler.sendMessage(mScanHandler.obtainMessage(parse(ScannerMsg.START_SCAN), device));
    }

    boolean stopScan(BluetoothDevice device) {
        if (DBG)
            Log.d(TAG, "stopScan " + device);
        if (!mScanResolution) {
            if (VDBG)
                Log.v(TAG, "stopScan already in progress " + device);
            return false;
        }
        return mScanHandler.sendMessage(mScanHandler.obtainMessage(parse(ScannerMsg.STOP_SCAN), device));
    }

    private void handelStartScan(BluetoothDevice device) {
        if (!mScanResolution) {
            mScanResolution = true;
        }
        if (mDeviceList.contains(device)) {
            if (DBG)
                Log.d(TAG, device + " scan already in progress");
            return;
        } else {
            mDeviceList.add(device);
        }
        if (VDBG)
            Log.v(TAG, "handelStartScan " + mDeviceList);
        if (mScanner != null) {
            mScanner.stopScan(mCallBack);
        }
        ScanSettings scanSettings = new ScanSettings.Builder().setLegacy(false)
                .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES).build();

        List<ScanFilter> filters = new ArrayList<ScanFilter>();
        for (BluetoothDevice dev : mDeviceList) {
            filters.add(new ScanFilter.Builder().setDeviceAddress(dev.getAddress()).build());
        }
        mScanner.startScan(filters, scanSettings, mCallBack);
    }

    private void handleStopScan(Object obj) {
        if (obj == null) {
            mDeviceList.clear();
        } else if (obj instanceof BluetoothDevice) {
            BluetoothDevice dev = (BluetoothDevice) obj;
            if (mDeviceList.contains(dev)) {
                mDeviceList.remove(dev);
            }
        }
        if (mDeviceList.size() == 0) {
            mScanner.stopScan(mCallBack);
            mScanResolution = false;
        }
    }

    private void deviceFound(ScanResult result) {
        BluetoothDevice device = result.getDevice();
        if (mDeviceList.contains(device)) {
            mDeviceList.remove(device);
            mService.onScanResult(device);
            handleStopScan(device);
        }
    }

    private int parse(ScannerMsg code) {
        return code.ordinal();
    }

    boolean isScanInProgress() {
        return mScanResolution;
    }

}
