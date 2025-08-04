/**
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.dcf.client.service.screensharing.p2p;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pWfdInfo;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.qualcomm.qti.dcf.client.service.screensharing.ScreenSharingService;

public class WifiP2pManagerHelper {
    private final String TAG = "WifiP2pManagerHelper";

    private static final int DISCOVERY_DEVICES_TIMEOUT = 10000;
    // Default RTSP Control Port
    public static final int DEFAULT_SESSION_MGMT_CTRL_PORT = 7236;
    // Following value is set based on 802.11n rates, in multiples of 1Mbps
    public static final int WIFI_MAX_THROUGHPUT = 54;

    public @interface P2pCmd {
        int CMD_ENABLE_FEATURE = 0;
        int CMD_DISABLE_FEATURE = 1;
        int CMD_DISCOVER_PEERS = 2;
        int CMD_PEER_DEVICES_CHANGED = 3;
        int CMD_DISCOVER_PEERS_FAILED = 4;
        int CMD_CONNECT = 5;
        int CMD_CONNECT_FAILED = 6;
        int CMD_CONNECTED = 7;
        int CMD_DISCONNECT = 8;
        int CMD_DISCONNECTED = 9;
        int CMD_UPDATE_DEVICE_TYPE = 10;
        int CMD_CREATE_GROUP_FOR_SINK = 11;
        int CMD_SET_MIRACAST_MODE = 12;
        int CMD_REQUEST_DEVICE_INFO = 13;
        int CMD_SET_WFD_INFO = 14;
    }

    private static final int INVALID_DEVICE_TYPE = -1;

    private final Handler mHandler = new Handler();
    private final Context mContext;
    private final P2pStateMachine mP2pStateMachine;
    private P2pStatusListener mP2pStatusListener;
    private final WifiP2pBroadcastReceiver mReceiver;
    private final IntentFilter mFilter;
    private WifiP2pManager mWifiP2pManager;
    private WifiP2pManager.Channel mChannel;
    private P2pActionListener mActionListener;
    private String mPeerAddress;
    private int mWfdType = INVALID_DEVICE_TYPE;
    private WifiP2pDevice mPeerDevice;
    private WifiP2pDevice mLocalDevice;
    private WifiP2pInfo mWifiP2pInfo;
    private int mP2pState;

    private final Runnable mNotifyDiscoveryFailure = new Runnable() {
        @Override
        public void run() {
            mP2pStateMachine.sendMessage(P2pCmd.CMD_DISCOVER_PEERS_FAILED,
                    ScreenSharingService.Error.P2P_DISCOVERY_TIMEOUT);
        }
    };

    public WifiP2pManagerHelper(Context context) {
        this.mContext = context;
        mP2pStateMachine = new P2pStateMachine();
        mReceiver = new WifiP2pBroadcastReceiver();
        mFilter = new IntentFilter();
        mFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        mFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
    }

    public void registerListener(P2pStatusListener listener) {
        mP2pStatusListener = listener;
    }

    public void unregisterListener(P2pStatusListener listener) {
        mP2pStatusListener = null;
    }

    public void initialize() {
        mP2pStateMachine.sendMessage(P2pCmd.CMD_ENABLE_FEATURE);
    }

    private boolean initializeP2p() {
        // Device capability definition check
        if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)) {
            Log.e(TAG, "Wi-Fi Direct is not supported by this device.");
            return false;
        }

        // Hardware capability check
        WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            Log.e(TAG, "Cannot get Wi-Fi system service.");
            return false;
        }

        mWifiP2pManager = (WifiP2pManager) mContext.getSystemService(Context.WIFI_P2P_SERVICE);
        if (mWifiP2pManager == null) {
            Log.e(TAG, "Cannot get Wi-Fi Direct system service.");
            return false;
        }

        mChannel = mWifiP2pManager.initialize(mContext, mContext.getMainLooper(), null);
        if (mChannel == null) {
            Log.e(TAG, "Cannot initialize Wi-Fi Direct.");
            return false;
        }
        mContext.registerReceiver(mReceiver, mFilter);
        return true;
    }

    public void deinit() {
        mP2pStateMachine.sendMessage(P2pCmd.CMD_DISABLE_FEATURE);
    }

    private void deinitInternal() {
        if (mChannel != null) {
            mContext.unregisterReceiver(mReceiver);
            mChannel.close();
            mChannel = null;
        }
    }

    public void setMiracastMode(int mode) {
        mP2pStateMachine.sendMessage(P2pCmd.CMD_SET_MIRACAST_MODE, mode);
    }

    private void setMiracastModeInternal(int mode) {
        mWifiP2pManager.setMiracastMode(mode);
    }

    public void setWfdDeviceType(int wfdType) {
        if (mWfdType != wfdType) {
            mWfdType = wfdType;
            mP2pStateMachine.sendMessage(P2pCmd.CMD_UPDATE_DEVICE_TYPE);
        }
    }

    private void updateDeviceType() {
        setWfdInfo();
        mP2pStateMachine.sendMessage(P2pCmd.CMD_CREATE_GROUP_FOR_SINK);
    }

    private void setWfdInfo() {
        WifiP2pWfdInfo wfdInfo = new WifiP2pWfdInfo();
        wfdInfo.setEnabled(true);
        wfdInfo.setDeviceType(mWfdType);
        wfdInfo.setControlPort(DEFAULT_SESSION_MGMT_CTRL_PORT);
        wfdInfo.setMaxThroughput(WIFI_MAX_THROUGHPUT);
        wfdInfo.setContentProtectionSupported(true);
        wfdInfo.setSessionAvailable(true);
        mWifiP2pManager.setWfdInfo(mChannel, wfdInfo, null);
    }

    private void createP2pGroupForSinkDevice() {
        if (mWfdType == ScreenSharingService.WfdType.SINK) {
            createGroup();
        }
    }

    private void createGroup() {
        mWifiP2pManager.createGroup(mChannel, new WifiP2pManager
                .ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "onSuccess: createGroup");
            }

            @Override
            public void onFailure(int reason) {
                Log.w(TAG, "onFailure: createGroup reason = " + reason);
            }
        });
    }

    private void removeGroup() {
        mWifiP2pManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.w(TAG, "onSuccess: removeGroup");
            }

            @Override
            public void onFailure(int reason) {
                Log.w(TAG, "onFailure: removeGroup reason = " + reason);
            }
        });
    }

    public void requestLocalDeviceInfo(P2pDeviceAddressListener listener) {
        mP2pStateMachine.sendMessage(P2pCmd.CMD_REQUEST_DEVICE_INFO, listener);
    }

    private void requestLocalDeviceInfoInternal(P2pDeviceAddressListener listener) {
        mWifiP2pManager.requestDeviceInfo(mChannel, wifiP2pDevice -> {
            Log.i(TAG, "onDeviceInfoAvailable: wifiP2pDevice=" + wifiP2pDevice);
            if (wifiP2pDevice != null) {
                listener.onDeviceAddressAvailable(wifiP2pDevice.deviceAddress);
            }
        });
    }

    public void establishP2pConnection(String address, P2pActionListener listener) {
        mPeerAddress = address;
        mActionListener = listener;
        mP2pStateMachine.sendMessage(P2pCmd.CMD_DISCOVER_PEERS);
    }

    public WifiP2pDevice getLocalWifiP2pDevice() {
        return mLocalDevice;
    }

    public WifiP2pDevice getPeerWifiP2pDevice() {
        return mPeerDevice;
    }

    public WifiP2pInfo getConnectionInfo() {
        return mWifiP2pInfo;
    }

    public boolean isP2pStateEnabled() {
        return mP2pState == WifiP2pManager.WIFI_P2P_STATE_ENABLED;
    }

    private void onP2pActionSuccess() {
        if (mActionListener != null) {
            mActionListener.onSuccess();
            mActionListener = null;
        }
    }

    private void onP2pActionFailure(int reasonCode) {
        if (mActionListener != null) {
            mActionListener.onFailure(reasonCode);
            mActionListener = null;
        }
    }

    public void teardownP2pConnection(P2pActionListener listener) {
        mActionListener = listener;
        mP2pStateMachine.sendMessage(P2pCmd.CMD_DISCONNECT);
    }

    private void discoverPeers() {
        mWifiP2pManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "onSuccess: discoverPeers");
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "onFailure: discoverPeers");
                mP2pStateMachine.sendMessage(P2pCmd.CMD_DISCOVER_PEERS_FAILED, reason);
            }
        });
        Log.e(TAG, "discovery the peer device, waiting until 5s timeout");
        mHandler.removeCallbacks(mNotifyDiscoveryFailure);
        mHandler.postDelayed(mNotifyDiscoveryFailure, DISCOVERY_DEVICES_TIMEOUT);
    }

    private void requestPeers() {
        mWifiP2pManager.requestPeers(mChannel, peers -> {
            Log.d(TAG, "mPeerAddress = " + mPeerAddress);
            if (!TextUtils.isEmpty(mPeerAddress) && peers.get(mPeerAddress) != null) {
                mPeerDevice = peers.get(mPeerAddress);
                mHandler.removeCallbacks(mNotifyDiscoveryFailure);
                mP2pStateMachine.sendMessage(P2pCmd.CMD_CONNECT);
            } else {
                Log.d(TAG, "not found peer device");
            }
        });
    }

    private void connectToPeerDevice() {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = mPeerAddress;
        mWifiP2pManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "onSuccess: connecting to peer");
            }

            @Override
            public void onFailure(int reason) {
                mP2pStateMachine.sendMessage(P2pCmd.CMD_CONNECT_FAILED, reason);
            }
        });
    }

    private void cancelConnect() {
        //our use case, only source device connects to sink device.
        //sink device only call remove group to disconnect and source device is be only initiator.
        removeGroup();
    }

    private void onP2pStateChanged(int p2pState) {
        if (p2pState == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
            // WFD Info will be clear due to Wifi off, thus
            // recovery WFD info when Wifi on
            mP2pStateMachine.sendMessage(P2pCmd.CMD_SET_WFD_INFO);
            mP2pStateMachine.sendMessage(P2pCmd.CMD_CREATE_GROUP_FOR_SINK);
        }
        mP2pState = p2pState;
        if (mP2pStatusListener != null) {
            mP2pStatusListener.onP2pStateChanged();
        }
    }

    private void obtainDeviceInfo() {
        mWifiP2pManager.requestDeviceInfo(mChannel, wifiP2pDevice -> {
            if (wifiP2pDevice == null) return;

            mLocalDevice = wifiP2pDevice;
            WifiP2pWfdInfo wfdInfo = wifiP2pDevice.getWfdInfo();
            Log.d(TAG, "obtainDeviceInfo mWfdType = " + mWfdType + " status = "
                    + wifiP2pDevice.status + " wfdInfo = " + wfdInfo);
            //p2p connected and remove app from recent page.
            if (wifiP2pDevice.status == WifiP2pDevice.CONNECTED) {
                if (mWfdType == INVALID_DEVICE_TYPE && wfdInfo != null) {
                    mWfdType = wfdInfo.getDeviceType();
                }
                mP2pStateMachine.sendMessage(P2pCmd.CMD_CONNECTED);
            } else {
                if (mWifiP2pInfo != null && !mWifiP2pInfo.groupFormed) {
                    mP2pStateMachine.sendMessage(P2pCmd.CMD_CREATE_GROUP_FOR_SINK);
                }
            }

            //the type invalid means the helper instance is created just now
            //to ensure the service device type is same as local
            if (mWfdType != INVALID_DEVICE_TYPE) {
                if (wfdInfo == null || wfdInfo.getDeviceType() != mWfdType) {
                    mP2pStateMachine.sendMessage(P2pCmd.CMD_UPDATE_DEVICE_TYPE);
                }
            }
        });
    }

    private void onP2pConnected(WifiP2pGroup wifiP2pGroup) {
        if (mP2pStatusListener != null) {
            mP2pStatusListener.onConnected(wifiP2pGroup);
        }
        mP2pStateMachine.sendMessage(P2pCmd.CMD_CONNECTED);
    }

    private void onP2pDisconnected() {
        if (mP2pStatusListener != null) {
            mP2pStatusListener.onDisconnected();
        }
        mP2pStateMachine.sendMessage(P2pCmd.CMD_DISCONNECTED);
    }

    private class WifiP2pBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                Log.d(TAG, "WIFI_P2P_STATE_CHANGED_ACTION state = " + state);
                onP2pStateChanged(state);
            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                Log.d(TAG, "WIFI_P2P_PEERS_CHANGED_ACTION");
                mP2pStateMachine.sendMessage(P2pCmd.CMD_PEER_DEVICES_CHANGED);
            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                Log.d(TAG, "WIFI_P2P_CONNECTION_CHANGED_ACTION");
                NetworkInfo networkInfo = intent.getParcelableExtra(
                        WifiP2pManager.EXTRA_NETWORK_INFO);
                mWifiP2pInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);
                if (networkInfo.isConnected()) {
                    WifiP2pGroup wifiP2pGroup = null;
                    if (mWifiP2pInfo.groupFormed) {
                        wifiP2pGroup = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP);
                    }
                    onP2pConnected(wifiP2pGroup);
                } else if (networkInfo.getState() == NetworkInfo.State.DISCONNECTED){
                    onP2pDisconnected();
                }
            } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                Log.d(TAG, "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");
                mLocalDevice = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            }
        }
    }

    private class P2pStateMachine {

        private State mCurrentP2pState;
        private final State mDefaultState;
        private final State mIdleState;
        private final State mDiscoveringState;
        private final State mConnectingState;
        private final State mConnectedState;

        public P2pStateMachine() {
            mDefaultState = new DefaultState();
            mIdleState = new IdleState();
            mDiscoveringState = new DiscoveringState();
            mConnectingState = new ConnectingState();
            mConnectedState = new ConnectedState();
            transitionTo(mDefaultState);
        }

        private class DefaultState implements State {

            @Override
            public void enter() {
                deinitInternal();
            }

            @Override
            public void processMessage(int message, Object arg) {
                if (message == P2pCmd.CMD_ENABLE_FEATURE) {
                    if (initializeP2p()) {
                        transitionTo(mIdleState);
                    }
                } else {
                    LogUnhandledCmd(message);
                }
            }
        }

        private class IdleState implements State {

            @Override
            public void enter() {
                //obtain existing device info and reset.
                obtainDeviceInfo();
            }

            @Override
            public void processMessage(int message, Object arg) {
                switch (message) {
                    case P2pCmd.CMD_DISCOVER_PEERS: {
                        transitionTo(mDiscoveringState);
                        break;
                    }

                    case P2pCmd.CMD_DISCONNECT: {
                        onP2pActionSuccess();
                        break;
                    }

                    case P2pCmd.CMD_CONNECTED: {
                        transitionTo(mConnectedState);
                        break;
                    }

                    case P2pCmd.CMD_DISABLE_FEATURE: {
                        transitionTo(mDefaultState);
                        break;
                    }

                    case P2pCmd.CMD_UPDATE_DEVICE_TYPE: {
                        updateDeviceType();
                        break;
                    }

                    case P2pCmd.CMD_CREATE_GROUP_FOR_SINK: {
                        createP2pGroupForSinkDevice();
                        break;
                    }

                    case P2pCmd.CMD_SET_MIRACAST_MODE: {
                        setMiracastModeInternal((int)arg);
                        break;
                    }

                    case P2pCmd.CMD_REQUEST_DEVICE_INFO: {
                        requestLocalDeviceInfoInternal((P2pDeviceAddressListener)arg);
                        break;
                    }

                    case P2pCmd.CMD_SET_WFD_INFO: {
                        setWfdInfo();
                        break;
                    }

                    default:
                        LogUnhandledCmd(message);
                        break;
                }
            }
        }

        private class DiscoveringState implements State {

            @Override
            public void enter() {
                discoverPeers();
            }

            @Override
            public void processMessage(int message, Object arg) {
                switch (message) {

                    case P2pCmd.CMD_DISCOVER_PEERS_FAILED: {
                        onP2pActionFailure((int)arg);
                        transitionTo(mIdleState);
                        break;
                    }

                    case P2pCmd.CMD_PEER_DEVICES_CHANGED: {
                        requestPeers();
                        break;
                    }

                    case P2pCmd.CMD_CONNECT: {
                        transitionTo(mConnectingState);
                        break;
                    }

                    case P2pCmd.CMD_REQUEST_DEVICE_INFO: {
                        requestLocalDeviceInfoInternal((P2pDeviceAddressListener)arg);
                        break;
                    }

                    default:
                        LogUnhandledCmd(message);
                        break;
                }
            }
        }

        private class ConnectingState implements State {

            @Override
            public void enter() {
                connectToPeerDevice();
            }

            @Override
            public void processMessage(int message, Object arg) {
                switch (message) {

                    case P2pCmd.CMD_CONNECT_FAILED: {
                        onP2pActionFailure((int)arg);
                        transitionTo(mIdleState);
                        break;
                    }

                    case P2pCmd.CMD_CONNECTED: {
                        onP2pActionSuccess();
                        transitionTo(mConnectedState);
                        break;
                    }

                    case P2pCmd.CMD_REQUEST_DEVICE_INFO: {
                        requestLocalDeviceInfoInternal((P2pDeviceAddressListener)arg);
                        break;
                    }

                    case P2pCmd.CMD_DISCONNECTED: {
                        onP2pActionFailure(ScreenSharingService.Error.P2P_CONNECTING_TIMEOUT);
                        transitionTo(mIdleState);
                        break;
                    }

                    default:
                        LogUnhandledCmd(message);
                        break;
                }
            }
        }

        private class ConnectedState implements State {

            @Override
            public void enter() {
                Log.d(TAG, "enter");
            }

            @Override
            public void processMessage(int message, Object arg) {
                switch (message) {
                    case P2pCmd.CMD_DISABLE_FEATURE: {
                        cancelConnect();
                        transitionTo(mIdleState);
                        sendMessage(P2pCmd.CMD_DISABLE_FEATURE);
                        break;
                    }

                    case P2pCmd.CMD_UPDATE_DEVICE_TYPE:
                    case P2pCmd.CMD_DISCONNECT: {
                        cancelConnect();
                        break;
                    }

                    case P2pCmd.CMD_DISCONNECTED: {
                        onP2pActionSuccess();
                        transitionTo(mIdleState);
                        break;
                    }

                    case P2pCmd.CMD_SET_MIRACAST_MODE: {
                        setMiracastModeInternal((int)arg);
                        break;
                    }

                    case P2pCmd.CMD_REQUEST_DEVICE_INFO: {
                        requestLocalDeviceInfoInternal((P2pDeviceAddressListener)arg);
                        break;
                    }

                    default:
                        LogUnhandledCmd(message);
                        break;
                }
            }
        }

        private void sendMessage(int message) {
            sendMessage(message, null);
        }

        private void sendMessage(int message, Object arg) {
            mCurrentP2pState.processMessage(message, arg);
        }

        private void transitionTo(State state) {
            if (mCurrentP2pState != state) {
                mCurrentP2pState = state;
                mCurrentP2pState.enter();
                Log.d(TAG, "transition to State = " +
                        mCurrentP2pState.getClass().getSimpleName());
            }
        }

        private void LogUnhandledCmd(int cmd) {
            Log.w(TAG, "LogUnhandledCmd: current state=" + mCurrentP2pState.getClass()
                    .getSimpleName() + ", unhandled cmd=" + cmd);
        }
    }

    private interface State {
        void enter();
        void processMessage(int message, Object arg);
    }

    public interface P2pDeviceAddressListener {
        void onDeviceAddressAvailable(String address);
    }

    public interface P2pActionListener {
        void onSuccess();

        void onFailure(int reasonCode);
    }

    public interface P2pStatusListener {
        void onConnected(WifiP2pGroup wifiP2pGroup);
        void onDisconnected();
        void onP2pStateChanged();
    }
}
