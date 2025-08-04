/*****************************************************************
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 ******************************************************************/

package vendor.qti.bluetooth.xpan;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.android.net.module.util.MacAddressUtils;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.MacAddress;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.SoftApInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.LocalOnlyHotspotReservation;
import android.net.wifi.WifiSsid;
import android.text.TextUtils;
import android.util.Log;

public class WifiUtils {

    private static final String TAG = "XpanWifi";
    private static final boolean DBG = XpanUtils.DBG;
    private static final boolean VDBG = XpanUtils.VDBG;

    private final String CACHE_WIFI = "wifi_cache";
    private final String KEY_CHANNELS_5G = "channels_5g";
    private final String KEY_CHANNELS_2G = "channels_2g";
    private final String KEY_CHANNELS_6G = "channels_6g";
    private final String KEY_ETHER_TYPE = "ether_type";
    private final String BAND_2GHZ ="2.4";
    static final String BAND_5GHZ ="5";
    private final String BAND_6GHZ ="6";
    private String mKeyChannel = KEY_CHANNELS_5G;
    private String mCountryCode;
    private String mSsid = "";
    private MacAddress mBssid;
    private String mPassphrase;
    private int mSecMode;
    private int mSecType;

    private Context mCtx;
    private BluetoothAdapter mBtAdapter;

    private static WifiUtils sInstance;
    private static final Object INSTANCE_LOCK = new Object();
    private final int DEFAULT_ETHER_TYPE = 0x8151;
    private final String PASS_PHARSE = "Xpan1234";
    private static int mEtherType = 0x8151;
    private int mBand  = SoftApConfiguration.BAND_5GHZ;
    private int mFrequencies [];
    private XpanUtils mUtils;
    private WifiManager mWifiManager;
    private boolean mConfigLoaded = false;

    private enum XPAN_WIFI_CACHE {
        CHANNEL, ETHERTYPE
    }

    private WifiUtils(Context ctx) {
        mCtx = ctx;
        mUtils = XpanUtils.getInstance(mCtx);
        mBtAdapter = mUtils.getBluetoothAdapter();
        mWifiManager = mCtx.getSystemService(WifiManager.class);
    }

    void loadConfig() {
        String freq = mUtils.getBand();
        boolean isLohsOpen = mUtils.isLohsOpen();
        mSecMode = XpanConstants.SM_WPA2;
        mSecType = SoftApConfiguration.SECURITY_TYPE_WPA2_PSK;
        if (isLohsOpen) {
            mSecMode = XpanConstants.SM_OPEN;
            mSecType = SoftApConfiguration.SECURITY_TYPE_OPEN;
        }
        switch (freq) {
        case BAND_2GHZ:
            mBand = SoftApConfiguration.BAND_2GHZ;
            mKeyChannel = KEY_CHANNELS_2G;
            break;

        case BAND_6GHZ:
            mBand = SoftApConfiguration.BAND_6GHZ;
            mKeyChannel = KEY_CHANNELS_6G;
            mSecMode = XpanConstants.SM_WPA3;
            mSecType = SoftApConfiguration.SECURITY_TYPE_WPA3_SAE;
            break;
        }
        if (VDBG)
            Log.v(TAG, "loadConfig " + freq + " " + mBand + " " + mSecMode);
        mConfigLoaded = true;
    }

    /**
     * Get singleton instance.
     */
    static WifiUtils getInstance(Context ctx) {
        synchronized (INSTANCE_LOCK) {
            if (sInstance == null) {
                sInstance = new WifiUtils(ctx);
            }
            return sInstance;
        }
    }

    void close() {
        if (DBG)
            Log.d(TAG, "close");
        sInstance = null;
        mConfigLoaded = false;
    }

    private void setSoftApConfiguration() {

        if (DBG)
            Log.d(TAG, "setSoftApConfiguration");

        String address = mBtAdapter.getAddress();
        mBssid = getBssid();
        if (TextUtils.isEmpty(address)) {
            address = "00000000";
        }
        address = address.replaceAll(":", "");
        if (address.length() > 32) {
            address = address.substring(0, 31);
        }
        mSsid = address + "Xpan";
        mPassphrase = PASS_PHARSE + address;
        if (mSecMode == XpanConstants.SM_OPEN) {
            mPassphrase = "";
        }
    }

    SoftApConfiguration getSoftApConfiguration() {
        if (VDBG)
            Log.v(TAG, "getSoftApConfiguration");
        if (!mConfigLoaded) {
            loadConfig();
        }

        if (TextUtils.isEmpty(mSsid)) {
            setSoftApConfiguration();
        }

        int channels[] = getCacheChannels();
        if (channels == null) {
            channels = new int[0];
            Log.e(TAG," should not reach here");
        }
        if (mUtils.isFixedFreq() && channels.length != 0 ) {
            ArrayList<int[]> list = getFixedChannel(channels);
            channels = list.get(0);
            mFrequencies = list.get(1);
        }

        if (mSecMode == XpanConstants.SM_OPEN) {
            return getSoftApConfigurationOpen();
        }

        if (TextUtils.isEmpty(mSsid) || TextUtils.isEmpty(mPassphrase)) {
            Log.w(TAG, "getSoftApConfiguration " + mSsid + " " + mPassphrase);
            return null;
        }
        final SoftApConfiguration sapConfig = new SoftApConfiguration.Builder()
                .setBand(mBand)
                .setWifiSsid(WifiSsid.fromBytes(mSsid.getBytes(StandardCharsets.UTF_8)))
                .setAllowedAcsChannels(mBand, channels)
                .setBssid(mBssid)
                .setMacRandomizationSetting(SoftApConfiguration.RANDOMIZATION_NONE)
                .setPassphrase(mPassphrase, mSecType)
                .setMaxChannelBandwidth(SoftApInfo.CHANNEL_WIDTH_20MHZ)
                .setAutoShutdownEnabled(false)
                .setHiddenSsid(true)
                .setIeee80211beEnabled(false)
                .build();
        return sapConfig;
    }

    private SoftApConfiguration getSoftApConfigurationOpen() {
        final SoftApConfiguration sapConfig = new SoftApConfiguration.Builder()
                  .setBand(mBand)
                  .setWifiSsid(WifiSsid.fromBytes(mSsid.getBytes(StandardCharsets.UTF_8)))
                  .setAllowedAcsChannels(mBand, getCacheChannels())
                  .setBssid(mBssid)
                  .setMacRandomizationSetting(SoftApConfiguration.RANDOMIZATION_NONE)
                  .setMaxChannelBandwidth(SoftApInfo.CHANNEL_WIDTH_20MHZ)
                  .setAutoShutdownEnabled(false)
                  .setHiddenSsid(true)
                  .build();
        if (VDBG)
            Log.v(TAG, "getSoftApConfigurationOpen " + sapConfig);
        return sapConfig;
    }

    String getPassPharse() {
        return mPassphrase;
    }

    String getSsid() {
        return mSsid;
    }

    int getBand() {
        return mBand;
    }

    MacAddress getBssid() {
        if (mBssid == null) {
            setBssid();
        }
        return mBssid;
    }

    public int getEtherType() {
        if (VDBG)
            Log.v(TAG, "getEtherType " + mEtherType);

        return mEtherType;
    }

    void setBssid() {
        if (mBssid == null) {
            mBssid = MacAddressUtils.createRandomUnicastAddress
                (MacAddress.fromString(mBtAdapter.getAddress()), new SecureRandom());
        }
    }

    void setCoutryCode(String country) {
        if (TextUtils.isEmpty(country) && TextUtils.isEmpty(mCountryCode)) {
            WifiManager wifiManager = mCtx.getSystemService(WifiManager.class);
            try {
                country = wifiManager.getCountryCode();
            } catch (SecurityException se) {
                Log.w(TAG, "setCoutryCode " + se.toString());
            }
            country = (TextUtils.isEmpty(country) ? XpanConstants.DEFAULT_COUTRY : country);
        }
        /* add ASCII space for any environment in the country as per dot11CountryString */
        mCountryCode = new String(country);
        if (DBG)
            Log.d(TAG, "setCoutryCode " + mCountryCode);
    }

    String getCountryCode() {
        return mCountryCode;
    }

    String getFormatedCountryCode() {
        if (TextUtils.isEmpty(mCountryCode)) {
            return XpanConstants.DEFAULT_COUTRY + "O";
        }
        return mCountryCode + "O";
    }

    int getSecMode() {
        return mSecMode;
    }

    int[] channelToFrequency(int channel[]) {
        int fre[] = new int[channel.length];
        switch (mBand) {
        case SoftApConfiguration.BAND_2GHZ:
            for (int i = 0; i < channel.length; i++) {
                int ch = channel[i];
                int fr = ch;
                if (ch >= 1 && ch <= 13) {
                    fr = 2407 + 5 * ch;
                } else if (ch == 14) {
                    fr = 2484;
                } else {
                    Log.d(TAG,
                            "channelToFrequency for BAND_2GHZ " + ch + " is not valid ");
                }
                fre[i] = fr;
            }
            break;
        case SoftApConfiguration.BAND_5GHZ:
            for (int i = 0; i < channel.length; i++) {
                fre[i] = 5000 + 5 * channel[i];
            }
            break;
        case SoftApConfiguration.BAND_6GHZ:
            for (int i = 0; i < channel.length; i++) {
                int ch = channel[i];
                int fr = ch;
                if (ch == 2) {
                    fr = 5935;
                } else {
                    fr = 5950 + 5 * ch;
                }
                fre[i] = fr;
            }
            break;
        default:
            Log.d(TAG, "channelToFrequency not valid band  " + mBand);
            break;
        }
        return fre;
    }

    void updateChannels(int[] channels) {
        if (channels != null && channels.length != 0) {
            cacheValue(XPAN_WIFI_CACHE.CHANNEL, null, channels);
        } else {
            if(DBG) Log.w(TAG,"updateChannels empty");
        }
    }

    private int[] getCacheChannels() {
        SharedPreferences pref = mUtils.getSharedPreferences(CACHE_WIFI);
        if (pref == null) {
            Log.w(TAG, "getCacheChannels " + pref);
            return new int[0];
        }
        Set<String> set = pref.getStringSet(mKeyChannel, null);
        if (set == null) {
            if (DBG) Log.d(TAG, "getCacheChannels " + mKeyChannel + " empty");
            return new int[0];
        }
        int listChannel[] = new int[set.size()];
        int index = 0;
        for (String channel : set) {
            listChannel[index++] = Integer.parseInt(channel);
        }
        logChannels("getCacheChannels", mKeyChannel, listChannel);
        mFrequencies = channelToFrequency(listChannel);
        return listChannel;
    }

    void cacheValue(XPAN_WIFI_CACHE cache, String key, int channels[]) {
        SharedPreferences pref = mUtils.getSharedPreferences(CACHE_WIFI);
        if (pref == null) {
            Log.w(TAG, "cacheValue Ignore" + cache + " key " + key + " " + channels);
            return;
        }
        SharedPreferences.Editor editor = pref.edit();

        if (cache == XPAN_WIFI_CACHE.ETHERTYPE) {
            int ethertype = pref.getInt(KEY_ETHER_TYPE, -1);
            if (ethertype == -1) {
                editor.putInt(KEY_ETHER_TYPE, DEFAULT_ETHER_TYPE);
                ethertype = DEFAULT_ETHER_TYPE;
            }
            mEtherType = ethertype;
            if (VDBG)
                Log.v(TAG, "cacheValue ethertype " + ethertype);

        } else if (cache == XPAN_WIFI_CACHE.CHANNEL) {
            if (channels.length == 0) {
                Log.w(TAG, "cacheValue empty " + mKeyChannel);
                return;
            }
            logChannels("cacheValue", mKeyChannel, channels);

            Set<String> setchannel = new HashSet<>();
            for (int chanel : channels) {
                setchannel.add(chanel + "");
            }
            editor.putStringSet(mKeyChannel, setchannel);

        } else {
            Log.w(TAG, "cacheValue invalid " + cache);
        }
        editor.apply();
    }

    boolean validateSapParams(LocalOnlyHotspotReservation res) {
        boolean valid = true;
        SoftApConfiguration softApConfig = res.getSoftApConfiguration();
        if (softApConfig == null) {
            Log.w(TAG, "Invalid SoftApConfiguration received");
            valid = false;
        }
        if (!mSsid.equals(softApConfig.getSsid())) {
            Log.w(TAG, "Invalid ssid " + softApConfig.getSsid());
            valid = false;
        }
        if (!mBssid.equals(softApConfig.getBssid())) {
            Log.w(TAG, "Invalid bssid " + softApConfig.getBssid());
            valid = false;
        }
        if (mSecMode == XpanConstants.SM_WPA2 &&
                !mPassphrase.equals(softApConfig.getPassphrase())) {
            Log.w(TAG, "Invalid pass phrase " + softApConfig.getPassphrase());
            valid = false;
        }
        if (VDBG) {
            Log.v(TAG, "validateSapParams ssid " + mSsid + ", bssid " + mBssid +
                ", passphrase " + mPassphrase + ", Country " + mCountryCode+" valid "+valid);
        }
        return valid;
    }

    private void logChannels(String msg, String key, int channels[]) {
        if (!VDBG) {
            return;
        }
        StringBuilder builder = new StringBuilder();
        builder.append(msg + " " + key);
        if (channels != null) {
            for (int i : channels) {
                builder.append(" " + i);
            }
        } else {
            builder.append("Empty channels");
        }
        Log.v(TAG, builder.toString());
    }

    int[] getFrequencies() {
        if (mFrequencies == null) {
            Log.e(TAG, "getFrequencies empty");
            mFrequencies = new int[0];
        }
        return mFrequencies;
    }

    ArrayList<int []> getFixedChannel(int channels[]) {
        int channel = mUtils.getFixedChannel();
        if (VDBG)
            Log.v(TAG, "getFixedChannel channel  " + channel);
        if(channel == 0) {
            channel = channels[0];
        } else {
            boolean found = false;
            for (int ch : channels) {
                if (ch == channel) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                if (VDBG)
                    Log.v(TAG, "getFixedChannel " + channel + " Channel not found setting default "
                            + channels[0]);
                channel = channels[0];
            }
        }
        int singleTonChannel[] = {channel};
        int singleTonFrequency[] = channelToFrequency(singleTonChannel);
        if (VDBG)
            Log.v(TAG, "getFixedChannel " + singleTonChannel + " freq " + singleTonFrequency);
        for(int i : singleTonChannel) {
            Log.d(TAG,"getFixedChannel channel -> "+i);
        }
        for(int i : singleTonFrequency) {
            Log.d(TAG,"getFixedChannel freq -> "+i);
        }

        ArrayList<int []> list = new ArrayList<>();
        list.add(singleTonChannel);
        list.add(singleTonFrequency);
        return list;
    }

    public boolean updateWifiParams(ConnectivityManager cm, NetworkCapabilities nc,
            Network nw, ApDetails params) {
        if (!nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            Log.d(TAG, "updateWifiParams not TRANSPORT_WIFI");
            return false;
        }
        WifiConfiguration configurarion = mWifiManager.getPrivilegedConnectedNetwork();
        if (configurarion == null) {
            if (DBG)
                Log.d(TAG, "updateWifiParams configurarion null");
            return false;
        }
        String ssid = configurarion.SSID;
        String ssidNc = nc.getSsid();
        if (TextUtils.isEmpty(ssid)|| TextUtils.isEmpty(ssidNc)) {
            Log.w(TAG, "updateWifiParams not valid " + ssid + " ssidNc " + ssidNc);
            return false;
        }
        if(!ssid.equals(ssidNc)) {
            Log.w(TAG, "updateWifiParams ssid's not match");
            return false;
        }
        String preSharedKey = configurarion.preSharedKey;
        if(DBG) Log.d(TAG, "updateWifiParams preSharedKey " + preSharedKey);
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        if (wifiInfo == null) {
            Log.d(TAG, "updateWifiParams wifiInfo null");
            return false;
        }
        boolean isEnterprise = isEnterpriseAp(wifiInfo);

        int frequency = wifiInfo.getFrequency();
        int secType = wifiInfo.getCurrentSecurityType();
        MacAddress bssid = MacAddress.fromString(wifiInfo.getBSSID());
        MacAddress macAddress = MacAddress.fromString(wifiInfo.getMacAddress());
        LinkProperties lProperties = cm.getLinkProperties(nw);
        InetAddress ipv4Address = null;
        InetAddress ipv6Address = null;
        if (lProperties != null) {
            List<LinkAddress> lAddress = lProperties.getLinkAddresses();
            for (LinkAddress la : lAddress) {
                if (la.isIpv4()) {
                    ipv4Address = la.getAddress();
                    if(DBG) Log.d(TAG,"updateWifiParams ipv4Address "+ipv4Address);
                } else if (la.isIpv6()) {
                    ipv6Address = la.getAddress();
                    if(DBG) Log.d(TAG,"updateWifiParams ipv6Address "+ipv6Address);
                }
            }
        }
        params = new ApDetails(ssid, bssid, frequency, ipv4Address, ipv6Address,
                macAddress , preSharedKey, secType, isEnterprise);
        return true;
    }

    public ApDetails getWifiParams(ConnectivityManager cm) {
        WifiConfiguration configurarion = mWifiManager.getPrivilegedConnectedNetwork();
        ApDetails params = null;
        if (configurarion == null) {
            if (DBG)
                Log.d(TAG, "getWifiParams configurarion null");
            return params;
        }
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        if (wifiInfo == null) {
            Log.d(TAG, "getWifiParams wifiInfo null");
            return params;
        }
        boolean isEnterprise = isEnterpriseAp(wifiInfo);

        InetAddress ipv4Address = null;
        InetAddress ipv6Address = null;
        // TODO: WAR remove later
        Network nww[] = cm.getAllNetworks();
        for (Network n : nww) {
            LinkProperties lProperties = cm.getLinkProperties(n);
            if (DBG)
                Log.d(TAG, "getWifiParams " + n + " " + lProperties);
            if (lProperties != null) {
                String ifName = lProperties.getInterfaceName();
                if (!TextUtils.isEmpty(ifName) && ifName.startsWith(XpanConstants.IF_WLAN)) {
                    List<LinkAddress> lAddress = lProperties.getLinkAddresses();
                    if (VDBG)
                        Log.v(TAG, "getWifiParams lAddress " + lAddress);
                    for (LinkAddress la : lAddress) {
                        if (la.isIpv4()) {
                            ipv4Address = la.getAddress();
                        } else if (la.isIpv6()) {
                            ipv6Address = la.getAddress();
                        }
                    }
                }
            }
        }
        String ssid = configurarion.SSID.replace("\"", "");
        String pwd = configurarion.preSharedKey;
        if (VDBG)
            Log.v(TAG, "getWifiParams " + ssid + " " + pwd);
        if (TextUtils.isEmpty(pwd)) {
            pwd = "";
        }
        if (pwd.startsWith("\"")) {
            pwd = pwd.substring(1, pwd.length());
        }
        if (pwd.endsWith("\"")) {
            pwd = pwd.substring(0, pwd.length() - 1);
        }
        String preSharedKey = new String(pwd);
        long redactions = wifiInfo.getApplicableRedactions();
        if (VDBG)
            Log.v(TAG, "getWifiParams redactions " + redactions + " wifiInfo " + wifiInfo);
        int frequency = wifiInfo.getFrequency();
        int secType = wifiInfo.getCurrentSecurityType();
        String bssidStr = wifiInfo.getBSSID();
        String macAddr = wifiInfo.getMacAddress();
        if (TextUtils.isEmpty(bssidStr) || TextUtils.isEmpty(macAddr)) {
            if (DBG)
                Log.w(TAG, "getWifiParams not valid " + bssidStr + " " + macAddr);
            return params;
        }
        MacAddress bssid = MacAddress.fromString(bssidStr);
        MacAddress macAddress = MacAddress.fromString(macAddr);
        params = new ApDetails(ssid, bssid, frequency, ipv4Address, ipv6Address,
                macAddress, preSharedKey, secType, isEnterprise);
        return params;
    }

    public WifiManager getWifiManager() {
        return mWifiManager;
    }

    int getSecurityMode(int sec) {
        int mode = XpanConstants.SM_UNKNOWN;
        switch (sec) {
        case WifiInfo.SECURITY_TYPE_OPEN:
            mode =  XpanConstants.SM_OPEN;
            break;
        case WifiInfo.SECURITY_TYPE_WEP:
            mode =  XpanConstants.SM_WEP;
            break;
        case WifiInfo.SECURITY_TYPE_PSK:
            mode =  XpanConstants.SM_WPA2;
            break;
        /*case WifiInfo.SECURITY_TYPE_SAE:
            mode =  XpanConstants.SM_MIXED_MODE;
            break;*/
        case WifiInfo.SECURITY_TYPE_SAE:
            mode =  XpanConstants.SM_WPA3;
            break;
            default:
                Log.w(TAG,"getSecurityMode not matched");
                break;
        }
        if (VDBG)
            Log.v(TAG, "getSecurityMode " + sec + " mode " + mode);
        return mode;
    }

    int getSecurityMode(int securities[]) {
        int secMode = 0;
        for (int sec : securities) {
            int mode = -1;
            switch (sec) {
            case WifiInfo.SECURITY_TYPE_OPEN:
                mode = XpanConstants.SM_OPEN;
                break;
            case WifiInfo.SECURITY_TYPE_WEP:
                mode = XpanConstants.SM_WEP;
                break;
            case WifiInfo.SECURITY_TYPE_PSK:
                mode = XpanConstants.SM_WPA2;
                break;
            case WifiInfo.SECURITY_TYPE_SAE:
                mode = XpanConstants.SM_WPA3;
                break;
            }
            if (mode != -1) {
                secMode = secMode | mode;
            }
        }
        if (secMode == 0) {
            secMode = XpanConstants.SM_UNKNOWN;
        }
        if (VDBG)
            Log.v(TAG, "getSecurityMode " + secMode);
        return secMode;
    }

    boolean isWifiEnabled() {
        return mWifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED;
    }

    boolean isWifiApEnabled() {
        return mWifiManager.getWifiApState() == WifiManager.WIFI_AP_STATE_ENABLED;
    }

    boolean isUpdateStationFrequency(ApDetails details) {
        boolean updated = false;
        WifiConfiguration configurarion = mWifiManager.getPrivilegedConnectedNetwork();
        ApDetails params = null;
        if (configurarion == null) {
            if (DBG)
                Log.d(TAG, "isStationFrequencyUpdated configurarion null");
            return updated;
        }
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        if (wifiInfo == null) {
            Log.d(TAG, "isStationFrequencyUpdated wifiInfo null");
            return updated;
        }
        int curFreq = wifiInfo.getFrequency();
        int prevFreq = details.getPrimaryFrequency();
        if (curFreq > 0 && curFreq != prevFreq) {
            if (VDBG)
                Log.v(TAG,
                        "isStationFrequencyUpdated " + prevFreq+ " " + curFreq);
            details.setPrimaryFrequency(curFreq);
            updated = true;
        }
        return updated;
    }

    private boolean isEnterpriseAp(WifiInfo wifiInfo) {
        if (wifiInfo == null) {
            Log.d(TAG, "isEnterpriseAp wifiInfo null");
            return false;
        }

        int secType = wifiInfo.getCurrentSecurityType();
        boolean isEnterpriseAp = (secType == wifiInfo.SECURITY_TYPE_EAP ||
                secType == wifiInfo.SECURITY_TYPE_EAP_WPA3_ENTERPRISE_192_BIT ||
                secType == wifiInfo.SECURITY_TYPE_EAP_WPA3_ENTERPRISE);
        if (DBG)
            Log.d(TAG, "isEnterpriseAp " + isEnterpriseAp + ", secType=" + secType);
        return isEnterpriseAp;
    }
}
