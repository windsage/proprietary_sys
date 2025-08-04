/*****************************************************************
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 ******************************************************************/
package vendor.qti.bluetooth.xpan;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.net.MacAddress;
import android.util.Log;

public class XpanEb implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final boolean DBG = XpanUtils.DBG;
    private static final boolean VDBG = XpanUtils.VDBG;
    private static final String TAG = "XpanEb";

    private BluetoothDevice device;
    private UUID uuidRemote;
    private List<RemoteMacAddress> remoteMacList;
    private int currentTransport;
    private int portL2capTcp = 0;
    private int portUdpAudio = 0;
    private int portUdpReports = 0;

    public XpanEb(BluetoothDevice device) {
        this.device = device;
        currentTransport = -1;
        remoteMacList = new ArrayList<RemoteMacAddress>();
    }

    /**
     * @return the portL2capTcp
     */
    int getPortL2capTcp() {
        return portL2capTcp;
    }

    /**
     * @param portL2capTcp the portL2capTcp to set
     */
    void setPortL2capTcp(int portL2capTcp) {
        this.portL2capTcp = portL2capTcp;
    }

    /**
     * @portUdp the portUdp for Audio data
     */
    int getPortUdpAudio() {
        return portUdpAudio;
    }

    /**
     * @param setPortUdpAudio the setPortUdpAudio to set
     */
    void setPortUdpAudio(int portUdpAudio) {
        this.portUdpAudio = portUdpAudio;
    }

    /**
     * @return the portUdpReports
     */
    int getPortUdpReports() {
        return portUdpReports;
    }

    /**
     * @param portUdpReports the portUdpReports to set
     */
    void setPortUdpReports(int portUdpReports) {
        this.portUdpReports = portUdpReports;
    }

    RemoteMacAddress getMac(MacAddress mac) {
        RemoteMacAddress remoteMacAddress = null;
        for (RemoteMacAddress remoteMac : remoteMacList) {
            if (remoteMac.getMacAddress().equals(mac)) {
                remoteMacAddress = remoteMac;
                break;
            }
        }
        return remoteMacAddress;
    }

    /**
     * @return the device
     */
    BluetoothDevice getDevice() {
        return device;
    }

    /**
     * @param device the device to set
     */
    void setDevice(BluetoothDevice device) {
        this.device = device;
    }

    /**
     * @return the uuidRemote
     */
    UUID getUuidRemote() {
        return uuidRemote;
    }

    /**
     * @param uuidRemote the uuidRemote to set
     */
    void setUuidRemote(UUID uuidRemote) {
        this.uuidRemote = uuidRemote;
    }

   /**
     * @return the remoteMacList
     */
    List<RemoteMacAddress> getRemoteMacList() {
        return remoteMacList;
    }

    /**
     * @return the currentTransport
     */
    int getCurrentTransport() {
        return currentTransport;
    }

    /**
     * @param currentTransport the currentTransport to set
     */
    void setCurrentTransport(int currentTransport) {
        this.currentTransport = currentTransport;
    }

    void addMac(BluetoothDevice device, MacAddress macAddress, int audioLocation) {
        if (getMac(macAddress) != null) {
            Log.w(TAG, "Mac address already added " + macAddress);
            return;
        } else {
            remoteMacList.add(new RemoteMacAddress(device, macAddress, audioLocation));
        }
    }

    boolean isConnected() {
        boolean connected = false;
        for (RemoteMacAddress macaddress : remoteMacList) {
            if (macaddress.getRole() == XpanConstants.ROLE_PRIMARY
                    && macaddress.getStatus() == XpanConstants.EB_CONNECTED) {
                connected = true;
            }
        }
        return connected;
    }

    /*
     * Return true if any of EB connected Xsap
     */
    boolean isEbConnectedToSap() {
        boolean connected = false;
        for (RemoteMacAddress mac : remoteMacList) {
            if (mac.isConnected()) {
                connected = true;
                break;
            }
        }
        return connected;
    }

    void reset() {
        remoteMacList.forEach((m) -> m.reset());
    }

    MacAddress getMac(int location) {
        MacAddress addr = null;
        for (RemoteMacAddress remoteMac : remoteMacList) {
            if (remoteMac.getAudioLocation() == location) {
                addr = remoteMac.getMacAddress();
                break;
            }
        }
        return addr;
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        if (VDBG) {
            buffer.append(TAG + " " + device + ", uuidRemote " + uuidRemote
                    + " currentTransport " + currentTransport);
            if (remoteMacList != null) {
                for (RemoteMacAddress macaddr : remoteMacList) {
                    buffer.append("\n" + macaddr);
                }
            }
        } else if (DBG) {
            buffer.append(TAG + " " + device + ", uuidRemote " + uuidRemote);
        } else {
            buffer.append(device);
        }
        return buffer.toString();
    }

    String getEbSSID() {
        String ssid = "";
        for (RemoteMacAddress macaddress : remoteMacList) {
            if (macaddress.getRole() == XpanConstants.ROLE_PRIMARY
                    && macaddress.getStatus() == XpanConstants.EB_CONNECTED) {
                ssid = macaddress.getSsid();
            }
        }
        return ssid;
    }

}
