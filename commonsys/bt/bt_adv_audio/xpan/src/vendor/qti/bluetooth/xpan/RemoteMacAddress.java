/*****************************************************************
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 ******************************************************************/

package vendor.qti.bluetooth.xpan;

import java.io.Serializable;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;

import android.bluetooth.BluetoothDevice;
import android.net.MacAddress;

public class RemoteMacAddress implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final boolean DBG = XpanUtils.DBG;
    private static final boolean VDBG = XpanUtils.DBG;
    private MacAddress macAddress;
    private int audioLocation;
    private int si;
    private int sp;
    private boolean twtConfigured;
    private boolean connected;
    private BluetoothDevice mDevice;

    // For XPAN_AP
    private String ssid;
    private MacAddress bssid;
    private int role;
    private int status = -1;
    private Inet4Address ipv4Address;
    private Inet6Address ipv6Address;
    private boolean ipv4 = false;
    private boolean ipv6 = false;

    RemoteMacAddress(BluetoothDevice device, MacAddress macAddress, int audioLocation) {
        mDevice = device;
        this.macAddress = macAddress;
        this.audioLocation = audioLocation;
    }

    void init(String ssid, MacAddress bssid, Inet4Address ipv4Address, int audioLocation,
            int role, int status) {
        this.ipv4Address = ipv4Address;
        ipv4 = true;
        init(ssid, bssid, audioLocation, role, status);
    }

    void init(String ssid, MacAddress bssid, Inet6Address ipv6Address, int audioLocation,
            int role, int status) {
        this.ipv6Address = ipv6Address;
        ipv6 = true;
        init(ssid, bssid, audioLocation, role, status);
    }

    private void init(String ssid, MacAddress bssid, int audioLocation, int role, int status) {
        this.ssid = ssid;
        this.bssid = bssid;
        this.audioLocation = audioLocation;
        this.role = role;
        this.status = status;
    }

    void setTwtConfigured(boolean twtConfigured, int si, int sp) {
        this.twtConfigured = twtConfigured;
        setSi(si);
        setSp(sp);
    }

    void setConnected(boolean connected) {
        this.connected = connected;
        if (!connected) {
            setTwtConfigured(false, 0, 0);
        }
    }

    MacAddress getMacAddress() {
        return macAddress;
    }

    boolean isTwtConfigured() {
        return twtConfigured;
    }

    boolean isConnected() {
        return connected;
    }

    boolean isDisConnected() {
        return !connected;
    }

    BluetoothDevice getDevice() {
        return mDevice;
    }

    int getSi() {
        return si;
    }

    private void setSi(int si) {
        this.si = si;
    }

    int getSp() {
        return sp;
    }

    private void setSp(int sp) {
        this.sp = sp;
    }

    String getSsid() {
        return ssid;
    }

    MacAddress getBssid() {
        return bssid;
    }

    int getAudioLocation() {
        return audioLocation;
    }

    int getRole() {
        return role;
    }

    int getStatus() {
        return status;
    }

    boolean isIpv4() {
        return ipv4;
    }

    boolean isIpv6() {
        return ipv6;
    }

    Inet4Address getIpv4Address() {
        return ipv4Address;
    }

    Inet6Address getIpv6Address() {
        return ipv6Address;
    }

    InetAddress getIpAddress() {
        if (isIpv4()) {
            return ipv4Address;
        } else {
            return ipv6Address;
        }
    }

    boolean isUpdated(Object obj) {
        if (this == obj) {
            return false;
        }
        if (obj == null) {
            return true;
        }
        if (getClass() != obj.getClass()) {
            return true;
        }
        RemoteMacAddress other = (RemoteMacAddress) obj;
        if (audioLocation != other.audioLocation) {
            return true;
        }
        if (bssid == null) {
            if (other.bssid != null) {
                return true;
            }
        } else if (!bssid.equals(other.bssid)) {
            return true;
        }
        if (ipv4Address == null) {
            if (other.ipv4Address != null) {
                return true;
            }
        } else if (!ipv4Address.equals(other.ipv4Address)) {
            return true;
        }
        if (ipv6Address == null) {
            if (other.ipv6Address != null) {
                return true;
            }
        } else if (!ipv6Address.equals(other.ipv6Address)) {
            return true;
        }
        if (macAddress == null) {
            if (other.macAddress != null) {
                return true;
            }
        } else if (!macAddress.equals(other.macAddress)) {
            return true;
        }
        if (role != other.role) {
            return true;
        }
        if (ssid == null) {
            if (other.ssid != null) {
                return true;
            }
        } else if (!ssid.equals(other.ssid)) {
            return true;
        }
        if (status != other.status) {
            return true;
        }
        return false;
    }

    void reset() {
        si = 0;
        sp = 0;
        twtConfigured = false;
        connected = false;
        ssid = "";
        bssid = null;
        role = -1;
        status = -1;
        ipv4Address = null;
        ipv6Address = null;
        ipv4 = false;
        ipv6 = false;
    }

    @Override
    public String toString() {
        if (VDBG) {
            StringBuilder builder = new StringBuilder();
            if (macAddress != null) {
                builder.append("" + macAddress);
            }
            if (audioLocation != -1) {
                builder.append(" location=" + audioLocation);
            }
            if (si > 0) {
                builder.append(" si=" + si);
            }
            if (sp > 0) {
                builder.append(" sp=" + sp);
            }
            if (ssid != null && ssid.length() > 0) {
                builder.append(" ssid=" + ssid);
            }
            if (bssid != null) {
                builder.append(" bssid=" + bssid);
            }
            if (role >= 0) {
                builder.append(" role=" + role);
            }
            if (status >= 0) {
                builder.append(" status=" + status);
            }
            if (ipv4Address != null) {
                builder.append(" ipv4Addr=" + ipv4Address);
            }
            if (ipv6Address != null) {
                builder.append(" ipv6Addr=" + ipv6Address);
            }
            if (ipv4) {
                builder.append(" ipv4=" + true);
            }
            if (ipv6) {
                builder.append(" ipv6=" + true);
            }
            builder.append(" twt = " + twtConfigured);
            builder.append(" connected = " + connected);
            return builder.toString();
        } else if (DBG) {
            return "mac=" + macAddress + " location=" + audioLocation;
        } else {
            return "";
        }
    }

}
