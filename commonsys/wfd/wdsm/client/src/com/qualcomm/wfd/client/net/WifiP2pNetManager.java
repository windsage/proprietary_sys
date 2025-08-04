/*
* Copyright (c) 2012-2013, 2016-2022 Qualcomm Technologies, Inc.
* All Rights Reserved.
* Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package com.qualcomm.wfd.client.net;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.Manifest;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.DeviceInfoListener;
import android.net.wifi.p2p.WifiP2pWfdInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.qualcomm.wfd.WfdDevice;
import com.qualcomm.wfd.WfdEnums.NetType;
import com.qualcomm.wfd.WfdEnums.WFDDeviceType;
import com.qualcomm.wfd.WfdEnums.WFDR2DeviceType;
import com.qualcomm.wfd.client.Whisperer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import java.lang.reflect.Method;

import com.qualcomm.wfd.client.net.WifiP2pNetDevice.WifiP2pWfdInfoHelper;

public class WifiP2pNetManager implements NetManagerIF {
    static final String TAG = "WifiP2pNetManager";

    private Whisperer whisperer = Whisperer.getInstance();

    private Handler mEventHandler;
    WifiP2pManager manager;
    Channel channel;
    private HandlerThread processThread;

    private final WifiP2pNetDevice localDevice = new WifiP2pNetDevice(null);
    private volatile WifiP2pInfo localConnectionInfo;
    private volatile boolean mEnabled;
    private volatile boolean mConnected;

    private final Collection<WifiP2pNetDevice> peers = new ArrayList<>() ;
    private final Collection<WifiP2pNetDevice> connectedPeers = new ArrayList<>() ;

    private final IntentFilter intentFilter = new IntentFilter();
    private BroadcastReceiver mReceiver;

    static WifiP2pNetManager sInstance;

    public WifiP2pNetManager(Handler handler) {
        if (sInstance != null) {
            throw new RuntimeException("Can create only one instance");
        }
        mEventHandler = handler;
        if(checkLocationPerms()) {
            init();
        }
    }

    private void init() {
        sInstance = this;
        processThread = new HandlerThread("WifiP2pNetManager");
        processThread.start();
        manager = (WifiP2pManager) whisperer.getActivity()
                .getSystemService(Context.WIFI_P2P_SERVICE);
        //channel = manager.initialize(context, processThread.getLooper(), null);

        channel = manager.initialize(whisperer.getActivity(), processThread.getLooper(), new WifiP2pManager.ChannelListener() {
            private boolean retryChannel = false;
            @Override
            public void onChannelDisconnected() {
                Log.d(TAG, "onChannelDisconnected() called");
                // we will try once more
                if (manager != null && !retryChannel) {
                    mEventHandler.obtainMessage(WIFI_UTIL_RETRY_CHANNEL, getNetType().ordinal(), 0).sendToTarget();
                    retryChannel = true;
                    channel = manager.initialize(whisperer.getActivity(), processThread.getLooper(), this);
                } else {
                    peers.clear();
                    connectedPeers.clear();
                    mEventHandler.obtainMessage(WIFI_UTIL_CHANNAL_LOST_PERMANENTLY, getNetType().ordinal(), 0).sendToTarget();
                }
            }
        });

        discoverServices();
        requestDeviceInfo();

        // add necessary intent values to be matched.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);

        mReceiver = new WifiDirectBroadcastReceiver(manager);
    }

    @Override
    public void destroy() {
        if(sInstance == null) {
            return;
        }
        //whisperer.activity.unregisterReceiver(mReceiver);
        stopPeerDiscovery(ResultListener.NullListener);
        processThread.quitSafely();
        sInstance = null;
    }

    /**
     * Method to convert a WifiP2PDevice to a WFDDevice
     *
     * @param device
     *            the WifiP2pDevice to be converted to WfdDevice
     * @return WfdDevice the converted WfdDevice
     */
    WfdDevice convertToWfdDevice(WifiP2pNetDevice device) {
        // another parameter: boolean parseLeaseFile
        WifiP2pDevice wifiP2pDevice = device.getDevice();

        if (wifiP2pDevice == null) {
            Log.e(TAG, "convertToWfdDevice Something amiss!! wifiP2pDevice is null");
            return null;
        }
        WifiP2pWfdInfo wfdInfo = wifiP2pDevice.getWfdInfo();
        if (wfdInfo == null) {
            Log.e(TAG, "convertToWfdDevice Something fishy!! WFDInfo is null for device");
            return null;
        }
        WifiP2pWfdInfoHelper wfdInfoHelper = WifiP2pWfdInfoHelper.wrap(wfdInfo);
        WfdDevice wfdDevice = new WfdDevice();
        wfdDevice.deviceType = wfdInfo.getDeviceType();
        wfdDevice.netType = getNetType().ordinal();
        wfdDevice.macAddress = wifiP2pDevice.deviceAddress;
        Log.d(TAG, "convertToWfdDevice: device macAddress= "
                + wfdDevice.macAddress);
        wfdDevice.deviceName = wifiP2pDevice.deviceName;
        //P2pWfdDeviceInfo extraWfdInfo = null;

        wfdDevice.ipAddress = device.getIp();
        Log.d(TAG, "convertToWfdDevice:wfdinfo" + wfdInfo.toString());
        if (wfdInfoHelper.isWfdR2Supported())
            wfdDevice.extSupport = 1;
        else
            wfdDevice.extSupport = 0;
        /*
        if (!parseLeaseFile) {
            if (info != null) {
                wfdDevice.ipAddress = info.groupOwnerAddress.getHostAddress();
                Log.d(TAG, "convertToWfdDevice- IP: " + wfdDevice.ipAddress);
            }
        } else {
            Log.d(TAG, "convertToWfdDevice parsing lease file");
            wfdDevice.ipAddress = getPeerIP(wfdDevice.macAddress);
            Log.d(TAG, "convertToWfdDevice- ipAddress: " + wfdDevice.ipAddress);
        }
        */

        if (wfdDevice.ipAddress == null) {
            Log.e(TAG, "convertToWfdDevice- no ipAddress was found");
        }

        wfdDevice.rtspPort = wfdInfo.getControlPort();
        wfdDevice.isAvailableForSession = wfdInfo.isSessionAvailable();
        wfdDevice.addressOfAP = null;
        wfdDevice.coupledSinkStatus = 0;
        wfdDevice.preferredConnectivity = 0;

        return wfdDevice;
    }

    @Override
    public void send_wfd_set(boolean isAvailableForSession, WFDDeviceType type, boolean extSupport) {
        if(sInstance == null) {
            return;
        }

        Log.d(TAG, "send_wfd_set");
        int wfdDeviceInfo = 0;
        int wfdR2DeviceInfo = WFDR2DeviceType.R2_UNSUPPORTED.getCode();

        if (type == WFDDeviceType.SOURCE) {
            wfdDeviceInfo |= P2pWfdDeviceInfo.DEVICETYPE_SOURCE;
            wfdR2DeviceInfo = (extSupport==true)?WFDR2DeviceType.R2_SOURCE.getCode():WFDR2DeviceType.R2_UNSUPPORTED.getCode();
        } else if(type == WFDDeviceType.PRIMARY_SINK) {
            wfdDeviceInfo |= P2pWfdDeviceInfo.DEVICETYPE_PRIMARYSINK;
            wfdR2DeviceInfo = (extSupport==true)?WFDR2DeviceType.R2_SINK.getCode():WFDR2DeviceType.R2_UNSUPPORTED.getCode();
        }
        else if(type == WFDDeviceType.SECONDARY_SINK)
            wfdDeviceInfo |= P2pWfdDeviceInfo.DEVICETYPE_SECONDARYSINK;
        else if(type == WFDDeviceType.SOURCE_PRIMARY_SINK) {
            wfdDeviceInfo |= P2pWfdDeviceInfo.DEVICETYPE_SOURCE_PRIMARYSINK;
            wfdR2DeviceInfo = (extSupport==true)?WFDR2DeviceType.R2_SOURCE_SINK.getCode():WFDR2DeviceType.R2_UNSUPPORTED.getCode();
        }

        // Needs to be enabled when service discovery is supported
        //wfdDeviceInfo |= SERVICE_DISCOVERY_SUPPORTED;

        //wfdDeviceInfo |= PREFERRED_CONNECTIVITY_TDLS;

        wfdDeviceInfo |= P2pWfdDeviceInfo.CP_SUPPORTED;

        //wfdDeviceInfo |= TIME_SYNC_SUPPORTED;

        //if(type == WFDDeviceType.PRIMARY_SINK ||
        //type == WFDDeviceType.SECONDARY_SINK ||
        //type == WFDDeviceType.SOURCE_PRIMARY_SINK) {

        //    wfdDeviceInfo |= AUDIO_NOT_SUPPORTED_AT_PSINK;
        //}

        if ( type == WFDDeviceType.SOURCE ||
            type == WFDDeviceType.SOURCE_PRIMARY_SINK ) {
            wfdDeviceInfo |= P2pWfdDeviceInfo.AUDIO_ONLY_SUPPORTED_AT_SOURCE;
        }

        //wfdDeviceInfo |= TDLS_PERSISTENT_GROUP;

        //wfdDeviceInfo |= TDLS_PERSISTENT_GROUP_REINVOKE;

        WifiP2pWfdInfo wfdP2pInfo = WifiP2pWfdInfoHelper.createWifiP2pWfdInfo(wfdDeviceInfo,
                    P2pWfdDeviceInfo.DEFAULT_SESSION_MGMT_CTRL_PORT,
                    P2pWfdDeviceInfo.WIFI_MAX_THROUGHPUT);
        if (wfdP2pInfo == null) {
            Log.e(TAG, "Cannot create WifiP2pWfdInfo instance");
            return;
        }
        WifiP2pWfdInfoHelper wfdP2pInfoHelper = WifiP2pWfdInfoHelper.wrap(wfdP2pInfo);
        wfdP2pInfo.setEnabled(true);
        wfdP2pInfo.setSessionAvailable(isAvailableForSession);
        Log.w(TAG, "wfdR2DeviceInfo = " + wfdR2DeviceInfo + ", extSupport = " + extSupport);
        wfdP2pInfoHelper.setWfdR2Device(wfdR2DeviceInfo);
        if ((type == WFDDeviceType.SOURCE ||
            type == WFDDeviceType.SOURCE_PRIMARY_SINK )) {
                //wfdP2pInfo.setCoupledSinkSupportAtSource(false);
        }

        if ( (type == WFDDeviceType.PRIMARY_SINK ||
            type == WFDDeviceType.SECONDARY_SINK ||
            type == WFDDeviceType.SOURCE_PRIMARY_SINK)) {

            //wfdP2pInfo.setCoupledSinkSupportAtSink(true);
        }

        WifiP2pManagerHelper managerHelper = WifiP2pManagerHelper.wrap(manager);
        managerHelper.setWfdInfo(channel, wfdP2pInfo, new ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG, "Successfully set WFD IE Params");
            }

            @Override
            public void onFailure(int error) {
                Log.d(TAG, "Failed to set WFD IE Params: " + error + ".");
            }

        });
    }

    public static String getPeerIP(String peerMac) {
        Log.d(TAG, "getPeerIP():  peerMac= " + peerMac);

        String ip = null;

        /* Try ARP table lookup */
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader("/proc/net/arp"));
            String line;
            if (br != null) {
                while ((line = br.readLine()) != null) {
                    Log.d(TAG, "line in /proc/net/arp is " + line);
                    String[] splitted = null;
                    if (line != null) {
                        splitted = line.split(" +");
                    }

                    // consider it as a match if 5 out of 6 bytes of the mac
                    // address match
                    // ARP output is in the format
                    // <IP address> <HW type> <Flags> <HW address> <Mask Device>

                    if (splitted != null && splitted.length >= 4) {
                        String[] peerMacBytes = peerMac.split(":");
                        String[] arpMacBytes = splitted[3].split(":");

                        if (arpMacBytes == null) {
                            continue;
                        }

                        int matchCount = 0;
                        for (int i = 0; i < arpMacBytes.length; i++) {
                            if (peerMacBytes[i]
                                    .equalsIgnoreCase(arpMacBytes[i])) {
                                matchCount++;
                            }
                        }

                        if (matchCount >= 5) {
                            ip = splitted[0];
                            // Perfect match!
                            if (matchCount == 6) {
                                // Perfect match!
                                return ip;
                            }
                        }
                    }
                }
            } else {
                Log.e(TAG, "Unable to open /proc/net/arp");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null)
                    br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return ip;
    }

    /**
     * @return the local IP if found on p2p interface
     */
    /*
    public static String getLocalIp() {
        Enumeration<NetworkInterface> netIntfList, virtualIntfList;
        Enumeration<InetAddress> phyAddresses, virtualAddresses;
        try {
            netIntfList = NetworkInterface.getNetworkInterfaces();
            while (netIntfList.hasMoreElements()) {
                NetworkInterface netIntf = netIntfList.nextElement();
                if (netIntf.isUp() && netIntf.getName().contains("p2p")) {
                    virtualIntfList = netIntf.getSubInterfaces();
                    while (virtualIntfList.hasMoreElements()) {
                        NetworkInterface virtualNetIntf = virtualIntfList
                                .nextElement();
                        virtualAddresses = virtualNetIntf.getInetAddresses();
                        while (virtualAddresses.hasMoreElements()) {
                            InetAddress virtualIPAddress = virtualAddresses
                                    .nextElement();
                            if (virtualIPAddress instanceof Inet4Address) {
                                Log.e(TAG,
                                        "IP address of device on virtual interface "
                                                + netIntf.getName()
                                                + " is "
                                                + virtualIPAddress
                                                        .getHostAddress());
                                return virtualIPAddress.getHostAddress();
                            }
                        }
                    }
                    phyAddresses = netIntf.getInetAddresses();
                    while (phyAddresses.hasMoreElements()) {
                        InetAddress phyIPAddress = phyAddresses.nextElement();
                        if (phyIPAddress instanceof Inet4Address) {
                            Log.e(TAG,
                                    "IP address of device on physical interface "
                                + netIntf.getName() + " is "
                                            + phyIPAddress.getHostAddress());
                            return phyIPAddress.getHostAddress();
                        }
                    }
                }
            }
        } catch (SocketException e) {
            Log.e(TAG, "SocketException");
        }
        return null;
    }
    */

    @Override
    public void disconnect(final ResultListener rl) {
        if(sInstance == null) {
            return;
        }
        Log.d(TAG, "disconnect() called");
        if (!whisperer.isMultiSinkMode()) {
            manager.removeGroup(channel, wrapResultListener(rl));
        } else {
            Log.e(TAG,"Ignoring call to remove group since Auto-GO mode is enabled");
        }
    }

    @Override
    public NetType getNetType() {
        return NetType.WIFI_P2P;
    }

    /* Callback Starts */
    @Override
    public void onMainActivityResume() {
        if(sInstance == null) {
            return;
        }
        Log.v(TAG, "onMainActivityResume");
        whisperer.getActivity().registerReceiver(mReceiver, intentFilter);
    }

    private void discoverServices(){
        Log.d(TAG, "Calling discoverSevices");
        if(channel != null){
            manager.discoverServices(channel, null);
        }
    }
    private void requestDeviceInfo(){
        Log.d(TAG, "Calling requestDeviceInfo");
        if(channel != null && manager != null){
            manager.requestDeviceInfo(channel, new WifiP2pManager.DeviceInfoListener(){
                @Override
                public void onDeviceInfoAvailable(WifiP2pDevice wifiP2pDevice){
                    Log.d(TAG, "onDeviceinfo for "+ wifiP2pDevice);
                    if (null != wifiP2pDevice)
                        onLocalDeviceChanged(wifiP2pDevice);
                }
            });
        }
    }

    @Override
    public void onMainActivityPause() {
        if(sInstance == null) {
            return;
        }
        Log.v(TAG, "onMainActivityPause");
        if (!whisperer.isMultiSinkMode()) {// TODO: MITask Fix Me!
            whisperer.getActivity().unregisterReceiver(mReceiver);
        }
    }

    void onDisconnected() {
        Log.v(TAG, "onDisconnected");
        peers.clear();
        connectedPeers.clear();
        mConnected = false;
        mEventHandler.obtainMessage(WIFI_UTIL_DISCONNECTED, getNetType().ordinal(), 0).sendToTarget();
    }

    void onConnectionInfoAvailable(WifiP2pInfo info) {
        Log.v(TAG, "onConnectionInfoAvailable " + info);
        //localDevice.mWifiP2pInfo = info;
        localConnectionInfo = info;
        mConnected = true;
        mEventHandler.obtainMessage(WIFI_UTIL_CONNECTION_INFO, getNetType().ordinal(), 0,
                new WifiWigigP2pConnectionInfo(info)).sendToTarget();
    }

    void onPeerListUpdate(Collection<WifiP2pDevice> devices) {
        Log.v(TAG, "onPeerListUpdate");
        peers.clear();
        for (WifiP2pDevice dev: devices) {
            peers.add(new WifiP2pNetDevice(dev));
        }
        mEventHandler.obtainMessage(WIFI_UTIL_PEERS_CHANGED, getNetType().ordinal(), 0, peers).sendToTarget();
    }

    void onConnectedListUpdate(Collection<WifiP2pDevice> devices) {
        Log.v(TAG, "onConnectedListUpdate");
        connectedPeers.clear();
        for (WifiP2pDevice device: devices) {
            connectedPeers.add(new WifiP2pNetDevice(device));
        }
        mEventHandler.obtainMessage(WIFI_UTIL_CONNECTED_PEERS_CHANGED, getNetType().ordinal(), 0, connectedPeers).sendToTarget();
    }

    void onLocalDeviceChanged(WifiP2pDevice device) {
        Log.v(TAG, "onLocalDeviceChanged");
        localDevice.setDevice(device);
        mEventHandler.obtainMessage(WIFI_UTIL_LOCAL_DEVICE_CHANGED, getNetType().ordinal(), 0, localDevice).sendToTarget();
    }

    void onStateChanged(int state) {
        Log.v(TAG, "onStateChanged " + state);
        mEnabled = (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED);
        mEventHandler.obtainMessage(NetManagerIF.WIFI_UTIL_P2P_STATE_CHANGED, getNetType().ordinal(), state).sendToTarget();
    }
    /* Callback Ends */

    @Override
    public void setMiracastMode(int mode) {
        if(sInstance == null) {
            return;
        }
        manager.setMiracastMode(mode);
    }

    @Override
    public void createGroup(final ResultListener rl) {
        if(sInstance == null) {
            return;
        }
        manager.createGroup(channel, wrapResultListener(rl));
    }

    @Override
    public void removeGroup(final ResultListener rl) {
        if(sInstance == null) {
            return;
        }
        manager.removeGroup(channel, wrapResultListener(rl));
    }

    @Override
    public void discoveryPeers(final ResultListener rl) {
        if(sInstance == null) {
            return;
        }
        manager.discoverPeers(channel, wrapResultListener(rl));
    }

    @Override
    public void stopPeerDiscovery(final ResultListener rl) {
        if(sInstance == null) {
            return;
        }
        manager.stopPeerDiscovery(channel, wrapResultListener(rl));
    }

    @Override
    public void cancelConnect(final ResultListener rl) {
        if(sInstance == null) {
            return;
        }
        if (localDevice.hasDeviceInfo() && localDevice.getDevice().status == WifiP2pDevice.CONNECTED) {
            disconnect(rl);
        } else {
            manager.cancelConnect(channel, wrapResultListener(rl));
        }
    }

    @Override
    public boolean isEnabled() {
        return mEnabled;
    }

    @Override
    public boolean isConnected() {
        return mConnected;
    }

    @Override
    public WifiP2pNetDevice getLocalDevice() {
        return localDevice;
    }

    @Override
    public ConnectionInfoIF getLocalConnectionInfo() {
        if(sInstance == null) {
            return null;
        }
        return localConnectionInfo != null? new WifiWigigP2pConnectionInfo(localConnectionInfo) : null;
    }

    @Override
    public Collection<NetDeviceIF> getPeers() {
        if(sInstance == null) {
            return null;
        }
        return Collections.unmodifiableCollection((Collection)peers);
    }

    @Override
    public Collection<NetDeviceIF> getConnectedPeers() {
        if(sInstance == null) {
            return null;
        }
        return Collections.unmodifiableCollection((Collection)connectedPeers);
    }

    @Override
    public void updateSystemPermissions(int requestCode,
            String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 'L': {
                int i = 0;
                while(i < grantResults.length) {
                    if (((permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION))
                        || (permissions[i].equals(Manifest.permission.NEARBY_WIFI_DEVICES)))
                        && (grantResults[i] == PackageManager.PERMISSION_GRANTED)) {
                        // Nothing to do, continue checking other permissions
                    } else {
                        return;
                    }
                    i++;
                }
                if (sInstance == null)
                   init();
                break;
            }
        }
    }

    public boolean checkPerms(int requestCode, String permissions[]) {
        boolean requestPerms = false;
        // If NetManager requests permissions in a group of which atleast one
        // is not (yet) granted, request the entire batch.
        for(String perm: permissions) {
            if (whisperer.getActivity().checkSelfPermission(perm)!= PackageManager.PERMISSION_GRANTED) {
                requestPerms = true;
                break;
            }
        }
        if(requestPerms)
            whisperer.getActivity().requestPermissions(permissions, requestCode);
        return !requestPerms;
    }

    private static ActionListener wrapResultListener(final ResultListener rl) {
        return new ActionListener() {
            @Override
            public void onSuccess() {
                rl.onSuccess(null);
            }

            @Override
            public void onFailure(int i) {
                rl.onFailure(i);
            }
        };
    }

    private boolean checkLocationPerms() {
        return checkPerms('L',
                new String[] {Manifest.permission.NEARBY_WIFI_DEVICES, Manifest.permission.ACCESS_FINE_LOCATION});
    }

    static class WifiP2pManagerHelper {
        static final String TAG = "WifiP2pManagerHelper";

        static final Method sMethod_setWfdInfo; // void setWfdInfo(Channel, WifiP2pWfdInfo, ActionListener)

        static {
            Method method_setWfdInfo = null;
            try {
                method_setWfdInfo = WifiP2pManager.class.getMethod("setWfdInfo", Channel.class, WifiP2pWfdInfo.class, ActionListener.class);
            } catch (Exception e) {
                Log.e(TAG, "Failed to get WifiP2pManager.setWfdInfo method ", e);
            }
            sMethod_setWfdInfo = method_setWfdInfo;

            Log.d(TAG, "WifiP2pManagerHelper initialized");
        }

        private final WifiP2pManager mManager;

        static WifiP2pManagerHelper wrap(WifiP2pManager manager) {
            return new WifiP2pManagerHelper(manager);
        }

        private WifiP2pManagerHelper(WifiP2pManager manager) {
            mManager = manager;
        }

        public void setWfdInfo(Channel c, WifiP2pWfdInfo wfdInfo, ActionListener listener) {
            Log.v(TAG, "setWfdInfo");
            if (mManager == null) {
                Log.e(TAG, "mManager is null");
                return;
            }

            if (sMethod_setWfdInfo == null) {
                Log.e(TAG, "sMethod_setWfdInfo is null");
                return;
            }

            try {
                sMethod_setWfdInfo.invoke(mManager, c, wfdInfo, listener);
                return;
            } catch (Exception e) {
                Log.e(TAG, "Failed to call WifiP2pManager.setWfdInfo() by reflection", e);
                return;
            }
        }
    }
}
