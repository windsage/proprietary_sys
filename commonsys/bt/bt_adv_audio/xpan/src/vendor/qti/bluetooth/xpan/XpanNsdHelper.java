/*****************************************************************
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 ******************************************************************/

package vendor.qti.bluetooth.xpan;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.text.TextUtils;
import android.util.Log;
import vendor.qti.bluetooth.xpan.XpanNsdHelper.NsdData;

public class XpanNsdHelper {

    private static final String TAG = "XpanNsd";
    private static final boolean DBG = XpanUtils.DBG;
    private static final boolean VDBG = XpanUtils.VDBG;

    private final String SERVICE_NAME = "XPAN";
    private final String SERVICE_TYPE = "_xpan._tcp";
    private final String SERVICE_NAME_EB = "XPAN_EB";

    private final NsdManager mNsdManager;
    private NsdServiceInfo mServiceInfo;
    private RegstrationListener mRegListener;
    private DiscoveryListener mDiscoveryListener;
    private InfoListener mInfoListener;
    private XpanUtils mUtils;
    private Handler mHandler;
    private List<BluetoothDevice> mDeviceListRegister = new ArrayList<BluetoothDevice>();
    private List<BluetoothDevice> mDeviceListDiscover = new ArrayList<BluetoothDevice>();
    private ConcurrentHashMap<BluetoothDevice, MdnsCallback> mMdnsCb =
            new ConcurrentHashMap<>();
    private XpanDataParser mDataParser;
    private int mPort = 0;
    private BluetoothDevice mDevice = null;
    private XpanProviderClient mProviderClient = null;


    static enum NsdMsg {
        DISCOVERY_START, DISCOVERY_STOP, REGISTER, UNREGISTER, CLOSE,
    }

    private class RegstrationListener implements NsdManager.RegistrationListener {

        @Override
        public void onRegistrationFailed(NsdServiceInfo nsdServiceInfo, int errorcode) {
            if (DBG)
                Log.d(TAG, "onRegistrationFailed " + nsdServiceInfo + " " + errorcode);
        }

        @Override
        public void onUnregistrationFailed(NsdServiceInfo nsdServiceInfo, int errorcode) {
            if (DBG)
                Log.d(TAG, "onUnregistrationFailed " + nsdServiceInfo + " " + errorcode);
        }

        @Override
        public void onServiceRegistered(NsdServiceInfo nsdServiceInfo) {
            if (DBG)
                Log.d(TAG, "onServiceRegistered " + nsdServiceInfo);

        }

        @Override
        public void onServiceUnregistered(NsdServiceInfo nsdServiceInfo) {
            if (DBG)
                Log.d(TAG, "onServiceUnregistered " + nsdServiceInfo);

        }
    }

    private class DiscoveryListener implements NsdManager.DiscoveryListener {

        @Override
        public void onStartDiscoveryFailed(String serviceType, int errorCode) {
            if (DBG)
                Log.d(TAG, "onStartDiscoveryFailed " + serviceType + "  " + errorCode);
            updateMdnsDiscoveryStatus(XpanConstants.DISCOVERY_START, XpanConstants.FAILURE);
            mNsdManager.stopServiceDiscovery(mDiscoveryListener);
        }

        @Override
        public void onStopDiscoveryFailed(String serviceType, int errorCode) {
            if (DBG)
                Log.d(TAG, "onStopDiscoveryFailed " + serviceType + " " + errorCode);
            updateMdnsDiscoveryStatus(XpanConstants.DISCOVERY_STOP, XpanConstants.FAILURE);
            mNsdManager.stopServiceDiscovery(mDiscoveryListener);
        }

        @Override
        public void onDiscoveryStarted(String serviceType) {
            if (DBG)
                Log.d(TAG, "onDiscoveryStarted " + serviceType);
            updateMdnsDiscoveryStatus(XpanConstants.DISCOVERY_START, XpanConstants.SUCCESS);
        }

        @Override
        public void onDiscoveryStopped(String serviceType) {
            if (DBG)
                Log.d(TAG, "onDiscoveryStopped " + serviceType);
            updateMdnsDiscoveryStatus(XpanConstants.DISCOVERY_STOP, XpanConstants.SUCCESS);
        }

        @Override
        public void onServiceFound(NsdServiceInfo nsdServiceInfo) {
            boolean isSame = nsdServiceInfo.getServiceType().contains(SERVICE_TYPE);
            if (DBG)
                Log.d(TAG, "onServiceFound : " + nsdServiceInfo + "  " + isSame);
            String serviceName = nsdServiceInfo.getServiceName();
            if (!isSame) {
                // Service type is containing the protocol and transport
                Log.w(TAG, "Unknown Service " + nsdServiceInfo.getServiceType());
            } else if (serviceName.contains(SERVICE_NAME_EB)) {
                unregisterServiceInfoCallback();
                mInfoListener = new InfoListener();
                if (VDBG)
                    Log.v(TAG, "onServiceFound registerServiceInfoCallback");
                mNsdManager.registerServiceInfoCallback(nsdServiceInfo,
                        Executors.newSingleThreadExecutor(), mInfoListener);
             }
        }

        @Override
        public void onServiceLost(NsdServiceInfo nsdServiceInfo) {
            if (DBG)
                Log.d(TAG, "onServiceLost " + nsdServiceInfo);
        }
    }

    private class InfoListener implements NsdManager.ServiceInfoCallback {

        public void onServiceInfoCallbackRegistrationFailed(int errorCode) {

            if (DBG)
                Log.d(TAG, "onServiceInfoCallbackRegistrationFailed " + errorCode);
            mInfoListener = null;
        }

        public void onServiceUpdated(NsdServiceInfo serviceInfo) {
            NsdData data = getNsdData(serviceInfo);
            if (DBG)
                Log.d(TAG, "onServiceUpdated " + serviceInfo + " \n " + data);

            if (data != null) {
                BluetoothDevice device = data.getDevice();
                MdnsCallback cb = mMdnsCb.get(device);
                if (cb != null) {
                    updateMessage(device, NsdMsg.DISCOVERY_STOP);
                    updateMessage(device, NsdMsg.UNREGISTER);
                    cb.onMdnsFound(data);
                } else {
                    Log.w(TAG, "onServiceUpdated MdnsCallback not found " + device);
                }

            } else {
                Log.w(TAG, "onServiceUpdated service null");
            }
        }

        public void onServiceLost() {
            if (DBG)
                Log.d(TAG, "onServiceLost ");

        }

        public void onServiceInfoCallbackUnregistered() {
            if (DBG)
                Log.d(TAG, "onServiceInfoCallbackUnregistered ");
            mInfoListener = null;

        }
    }

    /* Callbacks to be received by Connection State machine */
    public interface MdnsCallback {
        public void onMdnsFound(NsdData data);
        public void onMdnsStatusChanged(int state, int status);
    }

    XpanNsdHelper(XpanProfileService service) {
        if (DBG)
            Log.d(TAG, TAG);
        mNsdManager = (NsdManager) service.getApplicationContext()
                .getSystemService(Context.NSD_SERVICE);
        mUtils = service.getXpanUtils();
        mDataParser = new XpanDataParser();
        mProviderClient = service.getProviderClient();
    }

    void registerMdnsCallback(BluetoothDevice device, MdnsCallback callback) {
        if (VDBG)
            Log.d(TAG, "registerMdnsCallback " + device);
        if (mMdnsCb.containsKey(device)) {
            Log.w(TAG, "registerMdnsCallback present " + device);
            return;
        }
        mMdnsCb.put(device, callback);
    }

    /**
     * @param port the mPort to set
     */
    void setPort(int port) {
        mPort = port;
        if (VDBG)
            Log.v(TAG, "setPort " + mPort);
    }

    void updateMessage(BluetoothDevice device, NsdMsg msg) {
        try {
            if (VDBG)
                Log.d(TAG, "updateMessage : " + device + " msg " + msg);
            switch (msg) {
            case DISCOVERY_STOP:
                if (mDiscoveryListener == null) {
                    Log.w(TAG, "Ignore discoveryStop");
                    break;
                }
                if (!mDeviceListDiscover.contains(device)) {
                    Log.w(TAG, "No Device discovery present for " + device);
                    return;
                }
                mNsdManager.stopServiceDiscovery(mDiscoveryListener);
                mDiscoveryListener = null;
                unregisterServiceInfoCallback();
                mDeviceListDiscover.remove(device);
                break;

            case DISCOVERY_START:
                if (mDeviceListDiscover.contains(device)) {
                    Log.w(TAG, "Discovery already started for " + device);
                    return;
                }
                if (mDiscoveryListener == null) {
                    mDiscoveryListener = new DiscoveryListener();
                }
                mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD,
                        mDiscoveryListener);
                mDeviceListDiscover.add(device);
                mDevice = device;
                break;

            case REGISTER:
                if (mPort <= 0) {
                    Log.w(TAG, "Ignore register " + mPort + " " + device);
                    return;
                }
                if (mDeviceListRegister.contains(device)) {
                    Log.w(TAG, "Service registred already for " + device);
                    return;
                }
                mDeviceListRegister.add(device);
                if (mRegListener == null) {
                    mRegListener = new RegstrationListener();
                    // Create the NsdServiceInfo instance & populate it.
                    mServiceInfo = new NsdServiceInfo();
                    // Add Unique name
                    mServiceInfo.setServiceName(SERVICE_NAME);
                    mServiceInfo.setServiceType(SERVICE_TYPE);
                    mServiceInfo.setPort(mPort);
                    mServiceInfo.setAttribute(XpanConstants.KEY_MDNS,
                            mDataParser.getMdnsUuidString(mUtils.getUuidLocal()));
                    mNsdManager.registerService(mServiceInfo, NsdManager.PROTOCOL_DNS_SD,
                            mRegListener);
                    if (VDBG)
                        Log.v(TAG, "registerService " + device + " " + mServiceInfo);
                } else {
                    if (VDBG)
                        Log.v(TAG, "registerService " + device + " added ");
                }
                break;

            case UNREGISTER:
                if (DBG)
                    Log.d(TAG, "unRegisterService");
                if (mNsdManager == null || mRegListener == null) {
                    Log.w(TAG, "unRegisterService " + mNsdManager + " " + mRegListener);
                    return;
                }
                if (mDeviceListRegister.contains(device)) {
                    mDeviceListRegister.remove(device);
                    if (mDeviceListRegister.size() == 0) {
                        unregisterService(mRegListener);
                        mRegListener = null;
                    }
                }
                break;

            case CLOSE:
                if (mRegListener != null) {
                    unregisterService(mRegListener);
                }
                if (mDiscoveryListener != null) {
                    mNsdManager.stopServiceDiscovery(mDiscoveryListener);
                    mDiscoveryListener = null;
                }
                if (mInfoListener != null) {
                    mNsdManager.unregisterServiceInfoCallback(mInfoListener);
                }
                mDeviceListRegister.clear();
                mDeviceListDiscover.clear();
                mMdnsCb.clear();
                mPort = 0;
                mDevice = null;
                break;

            default:
                Log.w(TAG, "discovery " + device + " msg " + msg + " not handled");
                break;
            }
        } catch (Exception e) {
            Log.w(TAG, e.toString());
        }
    }

    public class NsdData {

        private BluetoothDevice device;
        private int port;
        private UUID mdnsUuid;
        private InetAddress inetAddress;

        public NsdData(BluetoothDevice device, int port, UUID mdnsUuid, InetAddress inetAddress) {
            super();
            this.device = device;
            this.port = port;
            this.mdnsUuid = mdnsUuid;
            this.inetAddress = inetAddress;
        }

        /**
         * @return the device
         */
        BluetoothDevice getDevice() {
            return device;
        }

        /**
         * @return the port
         */
        int getPort() {
            return port;
        }

        /**
         * @return the mdnsUuid
         */
        UUID getMdnsUuid() {
            return mdnsUuid;
        }

        /**
         * @return the inetAddress
         */
        InetAddress getInetAddress() {
            return inetAddress;
        }

        @Override
        public String toString() {
            return "NsdData [device=" + device + ", port=" + port + ", mdnsUuid=" + mdnsUuid
                    + ", inetAddress=" + inetAddress + "]";
        }
    }

    NsdData getNsdData(NsdServiceInfo serviceInfo) {
        if (VDBG)
            Log.d(TAG, "getNsdData " + serviceInfo);
        String name = serviceInfo.getServiceName();
        String type = serviceInfo.getServiceType();
        List<InetAddress> listIpAddress = serviceInfo.getHostAddresses();
        int port = serviceInfo.getPort();
        NsdData data = null;
        Map<String, byte[]> attributes = serviceInfo.getAttributes();
        if (VDBG)
            Log.d(TAG, "getNsdData name " + name + " type " + type + " listIpAddress "
                    + listIpAddress + " port " + port + " attr " + attributes);

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(type) || listIpAddress == null
                || listIpAddress.size() == 0 || port <= 0 || attributes == null
                || attributes.size() == 0) {
            Log.w(TAG, "getNsdData not valid");
            return data;
        }
        byte uuidBytes[] = attributes.get(XpanConstants.KEY_MDNS);
        if (uuidBytes == null || uuidBytes.length != 16) {

            Log.w(TAG, "getNsdData uuid invalid " +( (uuidBytes != null) ? uuidBytes.length
                    : "uuid null"));
        }
        UUID uuid = mDataParser.getMdnsSrvuUID(uuidBytes);
        if (VDBG)
            Log.d(TAG, "getNsdData uuid " + uuid);

        if (uuid == null) {
            Log.w(TAG, "getNsdData not valid uuid");
            return data;
        }
        String btAddress = mUtils.getMdnsUuidAddress(uuid);
        BluetoothAdapter btAdapter = mUtils.getBluetoothAdapter();
        if (!TextUtils.isEmpty(btAddress) && btAdapter != null) {
            data = new NsdData(btAdapter.getRemoteDevice(btAddress), port, uuid,
                    listIpAddress.get(0));
        } else {
            if (DBG)
                Log.w(TAG, "getNsdData btAddress " + btAddress + " btAdapter " + btAdapter);
        }
        if (VDBG)
            Log.d(TAG, "getNsdData " + data);

        return data;
    }

    private void unregisterService(NsdManager.RegistrationListener listener) {
        try {
            mNsdManager.unregisterService(listener);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "unregisterService " + listener + " " + e.toString());
        }
    }

    private void updateMdnsDiscoveryStatus(int state, int status) {

        if (mDevice == null) {
            Log.w(TAG, "updateMdnsDiscoveryStatus Ignore mDevice null");
            return;
        }
        String uuid = mUtils.getMdnsUuid(mDevice);
        if (TextUtils.isEmpty(uuid)) {
            Log.w(TAG, "updateMdnsDiscoveryStatus Ignore uuid null");
            return;
        }
        UUID uuidRemote = UUID.fromString(uuid);
        mProviderClient.updateMdnsDiscoveryStatus(mDevice, uuidRemote, state, status);
    }

    private void unregisterServiceInfoCallback() {
        if (mInfoListener != null) {
            mNsdManager.unregisterServiceInfoCallback(mInfoListener);
        }
    }
}
