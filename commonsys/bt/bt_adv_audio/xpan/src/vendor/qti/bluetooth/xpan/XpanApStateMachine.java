/*****************************************************************
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 ******************************************************************/
package vendor.qti.bluetooth.xpan;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.android.internal.util.State;
import com.android.internal.util.StateMachine;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/**
 * Xpan Ap State Machine
 *
 *          (DisConnected)
 *               ^
 *  AP_CONNECTED | AP_DISCONNECTED
 *               v
 *          (Connected)
 */
public class XpanApStateMachine extends StateMachine {

    private static final String TAG = "XpanApSm";
    private static final boolean DBG = XpanUtils.DBG;
    private static final boolean VDBG = XpanUtils.VDBG;

    /* State Machine Messages */
    private final int CONNECTED = 0;
    private final int DISCONNECTED = 1;
    private final int CONNECTION_STATE_CHANGED = 3;
    private final int INITIATE_SCAN = 4;
    private final int SCAN_STOP = 5;
    private final int SCAN_TIMEOUT = 6;
    private final int SCAN_RESULTS = 7;

    private WifiUtils mWifiUtils;
    private XpanUtils mUtils;
    private WifiStateReceiver mWifiStateReceiver;
    private WifiScanReceiver mWifiScanReceiver = null;
    private XpanProviderClient mProviderClient;
    private Context mCtx;
    private ApDetails mApDetails;
    private ConcurrentHashMap<BluetoothDevice, ApCallback> mAapCb =
            new ConcurrentHashMap<BluetoothDevice, ApCallback>();
    private CopyOnWriteArrayList<BluetoothDevice> mRequestScanList =
            new CopyOnWriteArrayList<BluetoothDevice>();
    private WifiManager mWifiManager;
    private ConnectivityManager mConnectivityManager;
    private Connected mConnected;
    private Disconnected mDisConnected;
    private int mL2capTcpPort = 0;
    private int mUdpPortAudio  = 0, mUdpSyncPort = 0 , mUdpPortReports = 0;
    private boolean mScanInitiated = false;
    private boolean mUpdateApDetails;
    private XpanNsdHelper mNsdHelper;

    /* AP Callback to be received by other modules */
    public interface ApCallback {
        public void onApConnectionStateChanged(ApDetails params);
        public void onWifiScanResults(List<ScanResult> scanResult);
    }

    private final class WifiStateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
            case WifiManager.NETWORK_STATE_CHANGED_ACTION:
                NetworkInfo nwInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (VDBG)
                    Log.v(TAG, "NETWORK_STATE_CHANGED_ACTION " + nwInfo.getState());
                if (NetworkInfo.State.CONNECTED.equals(nwInfo.getState())) {
                    sendMessage(CONNECTION_STATE_CHANGED, CONNECTED);
                } else {
                    sendMessage(CONNECTION_STATE_CHANGED, DISCONNECTED);
                }
                break;
            }
        }
    }

    private final class WifiScanReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (VDBG) Log.v(TAG, "WifiScanReceiver " + action +" "+mScanInitiated);
            switch (action) {
            case WifiManager.SCAN_RESULTS_AVAILABLE_ACTION:
                if (mScanInitiated) {
                    sendMessage(SCAN_RESULTS);
                }
                break;
            }
        }
    }

    static XpanApStateMachine make(XpanProfileService service, Looper looper) {
        if (DBG)
            Log.d(TAG, TAG);
        XpanApStateMachine apSm = new XpanApStateMachine(service, looper);
        apSm.start();
        return apSm;
    }

    private XpanApStateMachine(XpanProfileService service, Looper looper) {
        super(TAG, looper);
        mCtx = service.getApplicationContext();
        mWifiUtils = WifiUtils.getInstance(mCtx);
        mUtils = service.getXpanUtils();
        mWifiManager = mWifiUtils.getWifiManager();
        mConnectivityManager = mCtx.getSystemService(ConnectivityManager.class);
        registerWifiReceiver();
        mConnected = new Connected();
        mDisConnected = new Disconnected();
        mNsdHelper = service.getNsdHelper();
        mProviderClient = service.getProviderClient();
        addState(mDisConnected);
        addState(mConnected);
        setInitialState(mDisConnected);
    }

    private void init() {
        mL2capTcpPort = 0;
        mUdpPortAudio = 0;
        mUdpSyncPort = 0;
        mApDetails = null;
    }

    private final class Disconnected extends State {

        @Override
        public void enter() {
            init();
            if (DBG)
                Log.d(TAG, "Enter Disconnected");
            mAapCb.forEach((device,apcallback)->apcallback.onApConnectionStateChanged(null));
        }

        @Override
        public boolean processMessage(Message msg) {
            if (VDBG)
                Log.v(TAG, "Disconnected processMessage " + msg.what);
            if (msg.what == CONNECTION_STATE_CHANGED) {
                int state = msg.arg1;
                if (VDBG)
                    Log.v(TAG, "Disconnected CONNECTION_STATE_CHANGED state " + state);
                if (state == CONNECTED) {
                    transitionTo(mConnected);
                }
                return HANDLED;
            } else if(msg.what == INITIATE_SCAN) {
                updateScanResult();
            }
            return NOT_HANDLED;
        }
    }

    private final class Connected extends State {

        @Override
        public void enter() {
            if (DBG)
                Log.d(TAG, "Enter Connected");
            if (!getWifiParams()) {
                if (DBG)
                    Log.w(TAG, "Connected params not valid");
                transitionTo(mDisConnected);
            }
        }

        @Override
        public boolean processMessage(Message msg) {
            if (VDBG)
                Log.v(TAG, "Connected processMessage " + msg.what);
            switch (msg.what) {
            case INITIATE_SCAN:
                if (!initiateScan((BluetoothDevice) msg.obj)) {
                    updateScanResult();
                    stopScan();
                }
                break;

            case SCAN_STOP:
                stopScan();
                break;

            case SCAN_TIMEOUT:
                stopScan();
                break;

            case SCAN_RESULTS:
                updateScanResult();
                stopScan();
                break;

            case CONNECTION_STATE_CHANGED:
                int state = msg.arg1;
                if (DBG)
                    Log.d(TAG, "Connected CONNECTION_STATE_CHANGED state " + state);
                if (state != CONNECTED) {
                    transitionTo(mDisConnected);
                } else {
                    getWifiParams();
                }
                break;

            default:
                return NOT_HANDLED;
            }

            return HANDLED;
        }

        @Override
        public void exit() {
            if (mApDetails != null) {
                mApDetails.clear();
                mUpdateApDetails = true;
                updateApDetailsLocal();
                mApDetails = null;
            }

        }
    }

    private boolean initiateScan(BluetoothDevice device) {
        if (mScanInitiated) {
            Log.d(TAG, "Scan Requested already");
            return true;
        }
        if (mWifiScanReceiver == null) {
            mWifiScanReceiver = new WifiScanReceiver();
            IntentFilter intentFilter = new IntentFilter(
                    WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            mCtx.registerReceiver(mWifiScanReceiver, intentFilter);
            mScanInitiated = mWifiManager.startScan();
            if (VDBG)
                Log.v(TAG, "initiateScan " + mScanInitiated);
        } else {
            Log.w(TAG, "initiateScan ignore");
        }
        return mScanInitiated;
    }

    void startScan(BluetoothDevice device) {
        if (VDBG)
            Log.v(TAG, "startScan " + device +" " + mRequestScanList);
        if (!mRequestScanList.contains(device)) {
            mRequestScanList.add(device);
            if (VDBG)
                Log.v(TAG, "startScan " + device + " added " + mRequestScanList);
        }
        if (isConnected()) {
            sendMessage(INITIATE_SCAN, device);
        } else {
            updateScanResult();
        }
    }

    void updateScanResult() {
        if (mRequestScanList.size() == 0) {
            Log.w(TAG, "Empty mRequestScanList");
            return;
        }
        List<ScanResult> scanResultConnected = new ArrayList<ScanResult>();
        if (mApDetails != null) {
            List<ScanResult> scanResult = mWifiManager.getScanResults();
            String ssid = mApDetails.getSsid();
            for (ScanResult result : scanResult) {
                if (ssid.equalsIgnoreCase(result.SSID)) {
                    scanResultConnected.add(result);
                }
            }
        }
        if(VDBG) {
            Log.v(TAG, "updateScanResult mRequestScanList " + mRequestScanList
                    + "\nmAapCb " + mAapCb
                    + "\nscanResultConnected size " + scanResultConnected.size());
        }
        for (BluetoothDevice device : mRequestScanList) {
            ApCallback apCallback = mAapCb.get(device);
            apCallback.onWifiScanResults(scanResultConnected);
        }
        mRequestScanList.clear();
    }

    private void stopScan() {
        if (mWifiScanReceiver != null) {
            if (DBG)
                Log.d(TAG, "stopScan");
            mCtx.unregisterReceiver(mWifiScanReceiver);
            mWifiScanReceiver = null;
            mScanInitiated = false;
            mRequestScanList.clear();
        } else {
            Log.w(TAG, "stopScan ignore");
        }
    }

    void doQuit() {
        if (DBG)
            Log.d(TAG, "doQuit ");
        stopScan();
        unRegisterWifiReceiver();
        quitNow();
    }

    private void registerWifiReceiver() {
        if (mWifiStateReceiver != null) {
            Log.w(TAG, "registerWifiReceiver ignore");
            return;
        }
        if (DBG)
            Log.d(TAG, "registerWifiReceiver");
        mWifiStateReceiver = new WifiStateReceiver();
        IntentFilter intentFilter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mCtx.registerReceiver(mWifiStateReceiver, intentFilter);
    }

    private void unRegisterWifiReceiver() {
        if (mWifiStateReceiver == null) {
            Log.w(TAG, "unRegisterWifiReceiver ignore");
            return;
        }
        if (DBG)
            Log.d(TAG, "unRegisterWifiReceiver");
        mCtx.unregisterReceiver(mWifiStateReceiver);
    }

    boolean isConnected() {
        return mApDetails != null;
    }

    boolean isDisConnected() {
        return mApDetails == null;
    }

    private boolean getWifiParams() {
        ApDetails details = mWifiUtils.getWifiParams(mConnectivityManager);
        if (VDBG)
            Log.v(TAG, "getWifiParams " + details);
        boolean isUpdated = false;
        if (mApDetails == null) {
            mApDetails = details;
            mUpdateApDetails = true;
        } else if (mApDetails.isUpdated(details)) {
            mApDetails = details;
            mUpdateApDetails = true;
        }
        if (mApDetails != null && !mApDetails.isValidIpv4()) {
            mUpdateApDetails = false;
        }
        isUpdated = mUpdateApDetails;
        updateApDetailsLocal();
        return isUpdated;
    }

    ApDetails getApDetails() {
        return mApDetails;
    }

    void onPortNumbers(XpanEvent event) {
        if (VDBG)
            Log.v(TAG, "onPortNumbers " + event);
        /*
         * FixMe: Spec  Says 4 port numbers required
         * i) L2CAP TCP Port
         * ii) UDP port Audio
         * iii) UDP port Reports (Not Required)
         * iv) UDP Sync port
         */
        mL2capTcpPort = event.getArg1();
        mUdpPortAudio = event.getArg2();
        mUdpSyncPort = event.getArg3();
        mNsdHelper.setPort(mL2capTcpPort);
    }

    int getL2capTcpPort() {
        return mL2capTcpPort;
    }

    /**
     * @return the mUdpPortAudio
     */
    int getUdpPortAudio() {
        return mUdpPortAudio;
    }

    /**
     * @return the mUdpPortAudio
     */
    int getUdpPortReports() {
        return mUdpPortReports;
    }

    /**
     * @return the mUdpSyncPort
     */
    int getUdpSyncPort() {
        return mUdpSyncPort;
    }

    void registerApCallBack(BluetoothDevice device, ApCallback cb) {
        if (!mAapCb.containsKey(device)) {
            logDebug("registerApCallBack " + device);
            mAapCb.put(device, cb);
        } else {
            logDebug("registerApCallBack " + device + " Already registered");
        }
    }

    void unRegisterApCallBack(BluetoothDevice device) {
        if (mAapCb.containsKey(device)) {
            logDebug("unRegisterApCallBack " + device);
            mAapCb.remove(device);
        } else {
            logDebug("unRegisterApCallBack " + device + " Not present");
        }
    }

    private void logDebug(String msg) {
        if (DBG) {
            Log.d(TAG, msg);
        }
    }

    void setEbConnected(boolean isConnected) {
        if (isConnected != mUpdateApDetails) {
            mUpdateApDetails = isConnected;
            updateApDetailsLocal();
        }
    }

    private void updateApDetailsLocal() {
        if (VDBG)
            Log.v(TAG, "updateApDetailsLocal " + mUpdateApDetails + " mApDetails " + mApDetails);
        if (mUpdateApDetails && mApDetails != null && mProviderClient != null) {
            mProviderClient.updateApDetailsLocal(mApDetails);
            mAapCb.forEach((device,apcallback)->apcallback.onApConnectionStateChanged(mApDetails));
            mUpdateApDetails = false;
        }
    }

}
