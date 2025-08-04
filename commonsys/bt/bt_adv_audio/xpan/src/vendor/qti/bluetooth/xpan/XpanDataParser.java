/*****************************************************************
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 ******************************************************************/
package vendor.qti.bluetooth.xpan;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import android.net.MacAddress;
import android.net.wifi.ScanResult;
import android.text.TextUtils;
import android.util.Log;

public class XpanDataParser {

    private static final String TAG = "XpanDataParser";
    private static final boolean DBG = XpanUtils.DBG;
    private static final boolean VDBG = XpanUtils.VDBG;

    private final byte DATA_16[] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    private final byte DATA_4[] = { 0, 0, 0, 0 };

    /*
     * Client Feature bit mask
     */
    private int getClientFeature() {
        int clientfeature = XpanConstants.XPAN_5_GHZ_BEARER | XpanConstants.XPAN_6_GHZ_BEARER
            | XpanConstants.XPAN_POINT_TO_POINT_TOPOLOGY | XpanConstants.XPAN_P2P_AUDIO_BEARER
            | XpanConstants.AUDIO_BEARER_SWITCH_BW_BT_AND_SAP
            | XpanConstants.APTX_ADAPTIVE_CODEC_R4;
        if (VDBG) Log.v("getClientFeature", clientfeature + "");
        return clientfeature;
    }

    byte[] getConnectSsidBytes(String ssid, MacAddress bssid,
            String passphrase, int frequency, int secMode, String countryString) {

        int ssidLen = ssid.getBytes().length;
        int passphraseLen = passphrase.getBytes().length;
        int opcodeLen = 1;
        int requestLen = opcodeLen + ssidLen + passphraseLen
                + 1 /*ssid length*/
                + 1 /*passphrase length*/
                + 6 /* bssid Mac Address length*/
                + 2 /*center frequency*/
                + 1 /*Security mode*/
                + 3 /* Country String */
                + 32 /* PMK */;
        if (passphraseLen == 0) {
            requestLen -= 32;
        }
        if (VDBG)
            Log.v(TAG, " getConnectSsidBytes " + requestLen);
        ByteBuffer buffer = getByteBuffer(requestLen);
        buffer.put((byte)XpanConstants.OPCODE_CP_CONNECT_SSID); // connect ssid opcode
        buffer.put((byte)(0xFF & ssidLen));
        buffer.put(ssid.getBytes());
        buffer.put((byte)(0xFF & passphraseLen));
        buffer.put(passphrase.getBytes());
        buffer.put(bssid.toByteArray());
        buffer.put((byte)(0xFF & frequency));
        buffer.put((byte)(0xFF & frequency>>8));
        buffer.put((byte)(0xFF & secMode));
        buffer.put(countryString.getBytes());
        if (passphraseLen != 0) {
            buffer.put(getPmk(ssid, passphrase));
        }
        return buffer.array();
    }

    byte[] getBeaconParameterBytes(long nextTbttTsf, int bIMultiplier, int primaryFrequency) {
        if (VDBG)
            Log.v(TAG, "getBeaconParameterBytes " + nextTbttTsf
                    + " " + bIMultiplier + " " + primaryFrequency);

        if (bIMultiplier > Short.MAX_VALUE || primaryFrequency > Short.MAX_VALUE) {
            Log.e(TAG, "getBeaconParameterBytes incorrect values");
        }
        ByteBuffer buffer = getByteBuffer(13);
        buffer.put((byte)XpanConstants.OPCODE_CP_UPDATE_BEACON_PARAMETERS);
        buffer.putLong(nextTbttTsf);
        buffer.putShort(Integer.valueOf(bIMultiplier).shortValue());
        buffer.putShort(Integer.valueOf(primaryFrequency).shortValue());
        return buffer.array();
    }

    byte[] getDisConnectBytes(String ssid) {
        if (VDBG) Log.v(TAG, "getDisConnectBytes " + ssid);
        int ssidLen = ssid.length();
        int index = 0;
        byte value[] = new byte[ssidLen + 2];
        value[index++] = (byte)XpanConstants.OPCODE_CP_DISCONNECT_SSID; // disconnect ssid opcode
        value[index++] = (byte)(ssidLen & 0xFF);
        System.arraycopy(ssid.getBytes(), 0, value, index, ssidLen);
        return value;
    }

    byte[] getAudioBearerSwitchRequestBytes(int bearer) {
        if (VDBG) Log.v(TAG, "getAudioBearerSwitchRequestBytes " + bearer);
        if (bearer == XpanConstants.BEARER_P2P) {
            bearer = XpanConstants.NEW_AUDIO_BEARER_REQ_P2P;
        } else if (bearer == XpanConstants.BEARER_LE) {
            bearer = XpanConstants.NEW_AUDIO_BEARER_REQ_BT;
        } else {
            if (VDBG)
                Log.v(TAG, "getAudioBearerSwitchRequestBytes invalid " + bearer);
            return null;
        }
        byte [] value = new byte[2];
        value[0] = (byte) XpanConstants.OPCODE_CP_AUDIO_BEARER_SWITCH_REQ;
        value[1] = (byte)(bearer & 0xFF);
        return value;
    }

    byte[] getEtherTypeBytes(int etherType) {
        if (VDBG)
            Log.v(TAG, "getEtherTypeBytes " + etherType);
        byte [] value = new byte[3];
        value[0] = (byte)XpanConstants.OPCODE_CP_ETHER_TYPE;
        value[1] = (byte)(etherType & 0xFF);
        value[2] = (byte)((etherType >> 8) & 0xFF);
        return value;

    }

    byte[] getPowerStateResBytes(int response) {
        if (VDBG)
            Log.v(TAG, "getPowerStateResBytes " + response);
        byte []value = new byte[2];
        value[0] = (byte)(XpanConstants.OPCODE_CP_SAP_POWER_STATE_RESPONSE);
        value[1] = (byte)(response & 0xFF);
        return value;
    }

    byte[] getBearerPreferenceResponseBytes(int response) {
        if (VDBG)
            Log.v(TAG, "getBearerPreferenceResponseBytes " + response);
        byte []value = new byte[2];
        value[0] = (byte)(XpanConstants.OPCODE_CP_BEARER_PREFRENCE_RES);
        value[1] = (byte)(response & 0xFF);
        return value;
    }

    byte[] getSapStateBytes(int state, int reason) {
        if (VDBG)
            Log.v(TAG, "getSapStateBytes " + state + " " + reason);
        byte []value = new byte[3];
        value[0] = (byte)(XpanConstants.OPCODE_CP_SAP_STATE);
        value[1] = (byte)(state & 0xFF);
        value[2] = (byte)(reason & 0xFF);
        return value;
    }

    byte[] getMacAddressBytes(MacAddress mac) {
        if (VDBG) Log.v(TAG, "getMacAddressBytes " + mac);
        ByteBuffer buffer = getByteBuffer(7);
        buffer.put((byte)XpanConstants.OPCODE_CP_MAC_ADDRESS);
        buffer.put(mac.toByteArray());
        return buffer.array();
    }

    byte[] getTwtParamBytes(TwtWakeParams twtWakeParams, int setupid) {
        int typeFlags = XpanConstants.TWT_NEGOTIATION_TYPE_INDIVIDUAL
                | XpanConstants.TWT_FLOW_TYPE_UNANNOUNCED
                | XpanConstants.TWT_TRIGGER_TYPE_NONTRIGGERED;
        int sp = twtWakeParams.getSp();
        int si = twtWakeParams.getSi();
        long tsfPrimary = 0;
        int tsfSecondaryOffset = twtWakeParams.getRightoffset();
        ByteBuffer buffer = getByteBuffer(22);
        buffer.put((byte) (setupid & 0xFF));
        buffer.put((byte) (typeFlags & 0xFF));
        buffer.putInt(sp);
        buffer.putInt(si);
        buffer.putLong(tsfPrimary);
        buffer.putInt(tsfSecondaryOffset);
        if (VDBG)
            Log.v(TAG,    "getTwtParamBytes setupid " + setupid + " typeFlags " + typeFlags
                    + "sp " + sp + " si " + si + " tsfPrimary " + tsfPrimary
                    + " tsfSecondaryOffset " + tsfSecondaryOffset);
        return buffer.array();
    }

    byte[] getEmptyByteData() {
        return getByteBuffer(0).array();
    }

    byte[] getClientFeatureBytes() {
        ByteBuffer buffer = getByteBuffer(9);
        buffer.put((byte)XpanConstants.OPCODE_CP_CLIENT_FEATURES);
        buffer.putLong(getClientFeature());
        return buffer.array();
    }

    ByteBuffer getByteBuffer(int size) {
        ByteBuffer bytebuffer = ByteBuffer.allocate(size);
        bytebuffer.order(ByteOrder.LITTLE_ENDIAN);
        return bytebuffer;
    }

    ByteBuffer getByteBuffer(byte [] data) {
        ByteBuffer bytebuffer = ByteBuffer.wrap(data);
        bytebuffer.order(ByteOrder.LITTLE_ENDIAN);
        return bytebuffer;
    }

    void updateMacAddress(byte[] data, XpanEb eb) {
        if (data == null || data.length == 0 || data.length <= 10) {
            Log.e(TAG, "updateMacAddress invalid " + ((data == null) ? null : data.length));
            return;
        }
        int numdevices = (int) data[0];
        if (VDBG)
            Log.v(TAG, "updateMacAddress " + numdevices);
        if (numdevices <= 0) {
            return;
        }
        int parsed = 1;
        for (int i = 0; i < numdevices; i++) {
            try {
                byte[] macBytes = new byte[6];
                System.arraycopy(data, parsed, macBytes, 0, 6);
                parsed = parsed + 6;

                MacAddress macAddress = MacAddress.fromBytes(macBytes);

                byte[] audioBytes = new byte[4];
                System.arraycopy(data, parsed, audioBytes, 0, 4);
                parsed  += 4;
                ByteBuffer bufferr = getByteBuffer(audioBytes);
                int location = bufferr.getInt();
                if (DBG)
                    Log.d(TAG, "updateMacAddress " + macAddress + " " + location);
                eb.addMac(eb.getDevice(), macAddress, location);
            } catch (Exception e) {
                Log.d(TAG, "updateMacAddress " + e.toString());
            }
        }
        if (numdevices != eb.getRemoteMacList().size()) {
            Log.w(TAG, "updateMacAddress invalid");
        }

    }

    byte[] getConnectedSsidBytes(WifiUtils wifiUtils, ApDetails obj) {
        if (!obj.isValid()) {
            Log.w(TAG, "getConnectedSsidBytes invalid " + obj);
            return null;
        }
        int opcodeLen = 1;
        int passphraseLen = obj.getPassPhrase().getBytes().length;
        int ssidLength = obj.getSsid().getBytes().length;
        int length = opcodeLen + 1 /* ssidLength */
                + ssidLength /* SSID value length */
                + 6 /* Bssid */
                + 2 /* Primary Frequency */
                + 4 /* IPv4 address */
                + 16 /* IPv6 address */
                + 6 /* Mac address */
                + 1 /* Pass phare length */
                + passphraseLen /* Pass phrase value */
                + 1 /* Security mode */
                + 3 /* Country string */
                + 32 /* PMK */;
        if (passphraseLen == 0) {
            length -= 32;
        }
        if (VDBG)
            Log.v(TAG, "getConnectedSsidBytes " + obj);
        ByteBuffer buffer = getByteBuffer(length);
        buffer.put((byte) XpanConstants.OPCODE_CP_CONNECTED_SSID);
        buffer.put((byte) (0xFF & ssidLength));
        buffer.put(obj.getSsid().getBytes());
        buffer.put(obj.getBssid().toByteArray());
        buffer.put(shortToByteArray(obj.getPrimaryFrequency()));
        if (obj.getIpv4Address() != null) {
            buffer.put(obj.getIpv4Address().getAddress());
        } else {
            buffer.put(DATA_4);
        }
        if (obj.getIpv6Address() != null) {
            buffer.put(obj.getIpv6Address().getAddress());
        } else {
            buffer.put(DATA_16);
        }
        buffer.put(obj.getMacAddress().toByteArray());
        buffer.put((byte) (0xFF & passphraseLen));
        buffer.put(obj.getPassPhrase().getBytes());
        buffer.put((byte) (0xFF & wifiUtils.getSecurityMode(obj.getSecMode())));
        buffer.put(wifiUtils.getFormatedCountryCode().getBytes());
        if (passphraseLen != 0) {
            buffer.put(getPmk(obj.getSsid(), obj.getPassPhrase()));
        }
        return buffer.array();
    }

    byte[] getCsaBytes(XpanEvent event) {

        int length = 1 /* op code */ + 2 /* primaryFreq */ + 8 /* targettsf */
                + 1 /* band width */;
        if (DBG)
            Log.d(TAG, "getCsaBytes " + event);
        ByteBuffer buffer = getByteBuffer(length);
        buffer.put((byte) XpanConstants.OPCODE_CP_CSA);
        buffer.put(shortToByteArray(event.getArg1()));
        buffer.put(longToByteArray(event.getTbtTsf()));
        buffer.put((byte) (0xFF & event.getArg2()));
        return buffer.array();
    }

    byte[] getUdpPortBytes(int udpAudio, int udpReport) {

        int length = 1 /* op code */ + 2 /* Udo Audio */ + 2 /* Udp report */;
        if (VDBG)
            Log.v(TAG, "getUdpPortBytes " + udpAudio + "  " + udpReport);
        ByteBuffer buffer = getByteBuffer(length);
        buffer.put((byte) XpanConstants.OPCODE_CP_UDP_PORT);
        buffer.put(shortToByteArray(udpAudio));
        buffer.put(shortToByteArray(udpReport));
        return buffer.array();
    }

    byte[] getL2capTcpPortBytes(int l2capTcpPort) {

        int length = 1 /* op code */ + 2 /* L2cap TCP port */;
        if (VDBG)
            Log.v(TAG, "getL2capTcpPortBytes " + l2capTcpPort);
        ByteBuffer buffer = getByteBuffer(length);
        buffer.put((byte) XpanConstants.OPCODE_CP_L2CAP_TCP_PORT);
        buffer.put(shortToByteArray(l2capTcpPort));
        return buffer.array();
    }

    byte[] getUdpSyncPortBytes(int port) {

        int length = 1 /* op code */ + 2 /* UDP Sync Port */;
        if (VDBG)
            Log.v(TAG, "getUdpSyncPortBytes " + port);
        ByteBuffer buffer = getByteBuffer(length);
        buffer.put((byte) XpanConstants.OPCODE_CP_UDP_SYNC_PORT);
        buffer.put(shortToByteArray(port));
        return buffer.array();
    }

    byte[] getUseCaseIdentifierBytes(int usecase, int periodicity) {

        int length = 1 /* op code */ + 1 /* usecase */ +2 /*(periodicity) */;
        if (DBG)
            Log.d(TAG, "getUseCaseIdentifierBytes " + usecase + " " + periodicity);
        ByteBuffer buffer = getByteBuffer(length);
        buffer.put((byte) XpanConstants.OPCODE_CP_USECASE_IDENTIFIER);
        buffer.put((byte) getUsecaseIdentifier(usecase));
        buffer.put(shortToByteArray(periodicity));
        return buffer.array();
    }

    private int getUsecaseIdentifier(int usecase) {
        int identifier = XpanConstants.USECASE_IDENTIFIER_AUDIO_STREAMING;
        switch (usecase) {
        case XpanConstants.USECASE_AUDIO_STREAMING:
        case XpanConstants.USECASE_LOSSLESS_MUSIC:
            identifier = XpanConstants.USECASE_IDENTIFIER_AUDIO_STREAMING;
            break;
        case XpanConstants.USECASE_VOICE_CALL:
        case XpanConstants.USECASE_AP_VOICE_CALL:
            identifier = XpanConstants.USECASE_IDENTIFIER_VOICE_CALL;
            break;

        default:
            break;
        }
        return identifier;
    }

    byte[] getWifiScanResultBytes(WifiUtils wifiUtils, List<ScanResult> scanResults) {
        int length = 1 /* op code */ + 8 /* timestamp */ + 1 /* numbssid */
                + 1 /* ssid Length */
                + 1 /* ssid Lengthvalue */
                + 6 /* Bssid */
                + 2 /* Channel */
                + 1 /* Sec Mode */
                + 1; /* Rssi */
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int size = scanResults.size();
        long timeStamp = Calendar.getInstance().getTimeInMillis();
        if (DBG)
            Log.d(TAG, "getWifiScanResultBytes length " + length + " scanResults " + size
                    + " timeStamp " + timeStamp);
        buffer.write((byte) XpanConstants.OPCODE_CP_WIFI_SCAN_RESULTS);
        try {
            buffer.write(longToByteArray(timeStamp));
            buffer.write((byte) size);
            for (ScanResult result : scanResults) {
                buffer.write((byte) result.SSID.getBytes().length);
                buffer.write(result.SSID.getBytes());
                MacAddress macAddress = MacAddress.fromString(result.BSSID);
                buffer.write(macAddress.toByteArray());
                buffer.write(shortToByteArray(result.frequency)); // Channel
                buffer.write((byte) wifiUtils.getSecurityMode(result.getSecurityTypes())); // Sec Mode
                buffer.write((byte) result.level); // Rssi
                String data = result.SSID + " " + result.BSSID + " freq " + result.frequency
                        + " level " + result.level;
                if (VDBG)
                    Log.d(TAG, data);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.w(TAG, "getWifiScanResultBytes " + e.toString());
        }
        return buffer.toByteArray();
    }

    byte[] getMdnsSrvBytes(UUID uuid) {

        int length = 1 /* op code */ + 16 ;
        if (DBG)
            Log.d(TAG, "getMdnsSrvBytes " + uuid);
        ByteBuffer buffer = getByteBuffer(length);
        buffer.put((byte) XpanConstants.OPCODE_CP_MDNS_SRV_UUID);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return buffer.array();
    }

    UUID getMdnsSrvuUID(byte data[]) {
        ByteBuffer buffer = getByteBuffer(data);
        long mostBits = buffer.getLong();
        long leastBits = buffer.getLong();
        return new UUID(mostBits, leastBits);
    }

    String getMdnsUuidString(UUID uuid) {
        int length = 16;
        ByteBuffer buffer = getByteBuffer(length);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        byte array[] = buffer.array();
        StringBuilder sb = new StringBuilder();
        for (byte b : array) {
            sb.append(String.format("%02x", b));
        }
        if (DBG)
            Log.d(TAG, "getMdnsUuidString " + sb.toString());
        return sb.toString();
    }

    byte[] getBearerSwitchFailedBytes(int reason, int role) {
        if (VDBG)
            Log.v(TAG, "getBearerSwitchFailedBytes " + reason + " " + role);
        byte[] value = new byte[3];
        value[0] = (byte) (XpanConstants.OPCODE_CP_BEARER_SWITCH_FAILED);
        value[1] = (byte) (reason & 0xFF);
        value[2] = (byte) (role & 0xFF);
        return value;
    }

    /**
     * Converts integer value to byte array.
     */
    byte[] shortToByteArray(int val) {
        return (new byte[] { (byte) ((val) & 0xFF), (byte) ((val >> 8) & 0xFF), });
    }

    /**
     * Converts 2 byte array to integer value
     */
    int byteArrayToIntShort(byte[] val) {
        return (((val[1] & 0xFF) << 8) | ((val[0] & 0xFF)));
    }

    /**
     * Converts 4 byte array to integer value
     */
    int byteArrayToInt(byte[] val) {
        return (((val[3] & 0xFF) << 24) | ((val[2] & 0xFF) << 16) | ((val[1] & 0xFF) << 8)
                | ((val[0] & 0xFF)));
    }

    /**
     * Converts long value to byte array.
     */
    private byte[] longToByteArray(long val) {
        return (new byte[] { (byte) ((val) & 0xFF), (byte) ((val >> 8) & 0xFF),
                (byte) ((val >> 16) & 0xFF), (byte) ((val >> 24) & 0xFF),
                (byte) ((val >> 32) & 0xFF), (byte) ((val >> 40) & 0xFF),
                (byte) ((val >> 48) & 0xFF), (byte) ((val >> 56) & 0xFF) });
    }

    private byte[] getPmk(String ssid, String passPharse) {
        byte pmk[] = new byte[32];
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WITHHMACSHA1");
            PBEKeySpec spec = new PBEKeySpec(passPharse.toCharArray(), ssid.getBytes(), 4096,
                    32 * Byte.SIZE);
            pmk = skf.generateSecret(spec).getEncoded();
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            Log.e(TAG, "getPmk " + e.toString());
            if (VDBG) {
                e.printStackTrace();
            }
            for (int i = 0; i < pmk.length; i++) {
                pmk[i] = 0;
            }
        }
        return pmk;
    }

}
