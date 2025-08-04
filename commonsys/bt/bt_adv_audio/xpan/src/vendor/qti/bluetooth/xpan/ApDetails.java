/*****************************************************************
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 ******************************************************************/
package vendor.qti.bluetooth.xpan;

import java.io.Serializable;
import java.net.InetAddress;

import android.net.MacAddress;
import android.text.TextUtils;
import android.util.Log;

public class ApDetails implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final String TAG = "XpanParams";
    private static final boolean DBG = XpanUtils.DBG;
    private static final boolean VDBG = XpanUtils.VDBG;

    private String ssid;
    private MacAddress bssid; // Hydra MAC
    private int primaryFrequency; // 2 octets
    private InetAddress ipv4Address; // 4 octets
    private InetAddress ipv6Address; // 16 octets
    private MacAddress macAddress; // MAC Address of the client
    private String passPhrase;
    private int secMode; // 1 Octet
    private boolean enterprise;

    ApDetails(String ssid, MacAddress bssid, int primaryFrequency, InetAddress ipv4Address,
            InetAddress ipv6Address, MacAddress macAddress, String passPhrase, int secMode,
            boolean enterprise) {
        this.ssid = ssid;
        this.bssid = bssid;
        this.primaryFrequency = primaryFrequency;
        this.ipv4Address = ipv4Address;
        this.ipv6Address = ipv6Address;
        this.macAddress = macAddress;
        this.passPhrase = passPhrase;
        this.secMode = secMode;
        this.enterprise = enterprise;
    }

    void clear() {
        ssid = "";
        passPhrase = "";
        primaryFrequency = 0;
        secMode = 0;
        bssid = MacAddress.fromString(XpanConstants.MAC_DEFAULT);
        ipv4Address = null;
        ipv6Address = null;
        macAddress = MacAddress.fromString(XpanConstants.MAC_DEFAULT);
        enterprise = false;
    }

    String getSsid() {
        return ssid;
    }

    MacAddress getBssid() {
        return bssid;
    }

    String getPassPhrase() {
        return passPhrase;
    }

    void setPrimaryFrequency(int freq) {
        primaryFrequency = freq;
    }

    int getPrimaryFrequency() {
        return primaryFrequency;
    }

    InetAddress getIpv4Address() {
        return ipv4Address;
    }

    boolean isValidIpv4() {
        return ipv4Address != null;
    }

    InetAddress getIpv6Address() {
        return ipv6Address;
    }

    boolean isValidIpv6() {
        return ipv6Address != null;
    }

    InetAddress getIpAddr() {
        InetAddress addr = null;
        if (isValidIpv4()) {
            addr = ipv4Address;
        } else if (isValidIpv6()) {
            addr = ipv6Address;
        }
        return addr;
    }

    MacAddress getMacAddress() {
        return macAddress;
    }

    int getSecMode() {
        return secMode;
    }

    boolean isUpdated(Object obj) {
        if (this == obj)
            return false;
        if (obj == null)
            return true;
        if (getClass() != obj.getClass())
            return true;
        ApDetails other = (ApDetails) obj;
        if (bssid == null) {
            if (other.bssid != null)
                return false;
        } else if (!bssid.equals(other.bssid))
            return true;
        if (ipv4Address == null) {
            if (other.ipv4Address != null)
                return true;
        } else if (!ipv4Address.equals(other.ipv4Address))
            return true;
        if (ipv6Address == null) {
            if (other.ipv6Address != null)
                return true;
        } else if (!ipv6Address.equals(other.ipv6Address))
            return true;
        if (macAddress == null) {
            if (other.macAddress != null)
                return true;
        } else if (!macAddress.equals(other.macAddress))
            return false;
        if (passPhrase == null) {
            if (other.passPhrase != null)
                return false;
        } else if (!passPhrase.equals(other.passPhrase))
            return true;
        if (primaryFrequency != other.primaryFrequency)
            return true;
        if (secMode != other.secMode)
            return true;
        if (ssid == null) {
            if (other.ssid != null)
                return true;
        } else if (!ssid.equals(other.ssid))
            return true;
        if (enterprise != other.enterprise)
            return true;
        return false;
    }

    boolean isValid() {
        boolean valid = false;
        valid = (!TextUtils.isEmpty(ssid) && bssid != null && primaryFrequency > 0
                && (isValidIpv4() || isValidIpv6()));
        if (VDBG)
            Log.v(TAG, "isValid " + valid);
        return valid;
    }

    boolean isEnterprise() {
        return enterprise;
    }

    @Override
    public String toString() {
        if (VDBG) {
            return "ApDetails [ssid " + ssid + " bssid " + bssid + " Mac " + macAddress + " frequency "
                    + primaryFrequency + " ipv4Address " + ipv4Address + " ipv6Address "
                    + ipv6Address + " sec " + secMode + " entreprise " + enterprise + "]";
        } else if (DBG) {
            return "ApDetails [ssid=" + ssid + ", bssid=" + bssid + ", primaryFrequency="
                    + primaryFrequency + "]";
        } else {
            return super.toString();
        }
    }
}
