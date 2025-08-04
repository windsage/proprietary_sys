/*****************************************************************
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 ******************************************************************/

package vendor.qti.bluetooth.xpan;

import static android.net.wifi.WifiManager.WIFI_AP_STATE_DISABLED;
import static android.net.wifi.WifiManager.WIFI_AP_STATE_DISABLING;
import static android.net.wifi.WifiManager.WIFI_AP_STATE_ENABLED;
import static android.net.wifi.WifiManager.WIFI_AP_STATE_ENABLING;
import static android.net.wifi.WifiManager.WIFI_AP_STATE_FAILED;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothProfile;

public class XpanConstants {

    static final int XPAN_5_GHZ_BEARER = 0x01 << 1;
    static final int XPAN_6_GHZ_BEARER = 0x01 << 2;
    static final int XPAN_POINT_TO_POINT_TOPOLOGY = 0x01 << 3;
    static final int XPAN_P2P_AUDIO_BEARER = 0x01 << 5;
    static final int AUDIO_BEARER_SWITCH_BW_BT_AND_SAP = 0x01 << 9;
    static final int APTX_ADAPTIVE_CODEC_R4 = 0x01 << 17;
    // Transport reason
    static final int TP_REASON_CONNECTED = 0x00;
    static final int TP_REASON_REMOTE_R4_NOT_SUPPORT = 0x01;
    static final int TP_REASON_DUT_R4_NOT_SUPPORT = 0x02;
    static final int TP_REASON_REMOTE_SAP_NOTSUPPORT = 0x03;
    static final int TP_REASON_CONNECTION_FAILURE = 0x04;
    static final int TP_REASON_DISCONNECT = 0x05;
    static final int TP_REASON_UNPAIRED = 0x06;
    static final int TP_REASON_AIRPLANE_ENABLED = 0x07;
    static final int TP_REASON_SSR_STARTED = 0x08;

    static final int TP_SWITCH_REASON_UNSPECIFIED = 0;
    static final int TP_SWITCH_REASON_TERMINATING = 1;

    /* Reason why SAP turned off */
    static final int SAP_TD_REASON_CONCURRENCY = 0;
    static final int SAP_TD_REASON_AP = 1;
    static final int SAP_TD_REASON_UNKNOWN = 2;

    // Bearer status
    static final int BEARER_STATUS_SUCESS = 0x00;
    static final int BEARER_STATUS_FAILURE = 0x01;

    // BT or MDNS Scan START / STOP
    static final int DISCOVERY_START = 0x01;
    static final int DISCOVERY_STOP = 0x00;

    // MDNS Register/Unregister
    static final int MDNS_REGISTER = 0x01;
    static final int MDNS_UNREGISTER = 0x00;

    static final String KEY_UUID_LOCAL = "uuid_local";
    static final String KEY_MDNS = "mDNSUUID";

    // LE Link Success or Failure
    static final int SUCCESS = 0x00;
    static final int FAILURE = 0x01;

    // Use case
    static final int USECASE_NONE = 0x00;
    static final int USECASE_LOSSLESS_MUSIC = 0x01;
    static final int USECASE_GAMING = 0x02;
    static final int USECASE_VBC = 0X03;
    static final int USECASE_AUDIO_STREAMING = 0X05;
    static final int USECASE_VOICE_CALL = 0X07;
    static final int USECASE_AP_VOICE_CALL = 0X08;

    // TWT event type
    static final int TWT_SETUP = 0x00;
    static final int TWT_TERMINATE = 0x01;
    static final int TWT_SUSPEND = 0x02;
    static final int TWT_RESUME = 0x03;

    // Acs
    static final int ACS_STARTED = 0x00;
    static final int ACS_COMPLETED = 0x01;
    static final int ACS_FAILED = 0x02;

    // Beacon Intervals
    static final int TBT_INTERVAL_TYPE_LOWER = 0x01;
    static final int TBT_INTERVAL_TYPE_AGGRESSIVE = 0x02;

    /* Control point OP code */

    static final int OPCODE_CP_CONNECT_SSID = 0x01;
    static final int OPCODE_CP_DISCONNECT_SSID = 0x02;
    static final int OPCODE_CP_CONNECTED_SSID = 0x03;
    static final int OPCODE_CP_AVAILABLE_SSID = 0x04;
    static final int OPCODE_CP_RELATED_SSID = 0x05;
    static final int OPCODE_CP_MDNS_SRV_UUID = 0x06;
    static final int OPCODE_CP_REMOVE_SSID = 0x07;
    static final int OPCODE_CP_L2CAP_TCP_PORT = 0x08;
    static final int OPCODE_CP_UDP_PORT = 0x09;
    static final int OPCODE_CP_ETHER_TYPE = 0x0A;
    static final int OPCODE_CP_MAC_ADDRESS = 0x0B;
    static final int OPCODE_CP_BEARER_PREFRENCE_RES = 0x0C;
    static final int OPCODE_CP_UPDATE_BEACON_PARAMETERS = 0x0D;
    static final int OPCODE_CP_SAP_POWER_STATE_RESPONSE = 0x0E;
    static final int OPCODE_CP_CLIENT_FEATURES = 0x0F;
    static final int OPCODE_CP_AUDIO_BEARER_SWITCH_REQ = 0x10;
    static final int OPCODE_CP_WIFI_SCAN_RESULTS = 0x11;
    static final int OPCODE_CP_CSA = 0x12;
    static final int OPCODE_CP_UDP_SYNC_PORT = 0x13;
    static final int OPCODE_CP_ROAMING_REQUEST = 0x14;
    static final int OPCODE_CP_MULTICAST_MAC_ADDRESS = 0x15;
    static final int OPCODE_CP_USECASE_IDENTIFIER = 0x16;
    static final int OPCODE_CP_BEARER_SWITCH_FAILED = 0x19;
    static final int OPCODE_CP_SAP_STATE = 0x1B;

    static final int BEARER_DEFAULT = 0x00;
    static final int BEARER_BR_EDR = 0x01;
    static final int BEARER_LE = 0x02;
    static final int BEARER_AP = 0x03;
    static final int BEARER_P2P = 0x04;
    static final int BEARER_AP_PREP = 0x05;
    static final int BEARER_AP_ASSIST = 0x06;
    static final int BEARER_NON_XPAN = 0x07;
    static final int BEARER_P2P_PREP = 0x08;
    static final int BEARER_NONE = 0x10;

    // Remote Bearer Preference response
    static final int BEARER_ACCEPTED = 0x00;
    static final int BEARER_REJECTED = 0x01;

    // Response for Clear to send request
    public static final int CTS_NOT_READY = 0x00; // Not ready to accept audio data over XPAN
    public static final int CTS_READY = 0x01; // Ready to accept audio data over XPAN

    /* TWT Parameters flags */
    static final int TWT_NEGOTIATION_TYPE_INDIVIDUAL = 0;
    static final int TWT_NEGOTIATION_TYPE_BROADCAST = 1;
    static final int TWT_FLOW_TYPE_ANNOUNCED = 0;
    static final int TWT_FLOW_TYPE_UNANNOUNCED = 1 << 1;
    static final int TWT_TRIGGER_TYPE_NONTRIGGERED = 0;
    static final int TWT_TRIGGER_TYPE_TRIGGERED = 1 << 2;

    /* Timeout intervals */
    static int SAP_LOW_POWER_DURATION = 12; // Seconds
    static int GENERAL_BEACON_INT = 1;
    static int INTERMEDIATE_ACTIVE_DURATION = 3;
    static int CONNECTION_WAITING_TIMEOUT = 60 * 60 * 1000; //  60 min timeout Todo
    public static final int SCAN_TIMEOUT_DURATION = 15; // seconds
    static int INTERMEDIATE_ACTIVE_DURATION_MIN = 1500; // 1.5 seconds
    static final int DEFAULT_LP_INTERVAL = 10; // 10 seconds
    static final boolean DEFAULT_LP_ENABLE = true; // Lowpower enable
    static int DURATION_SAP_CONCURRENCY = 2000; // 2 seconds
    static final int DURATION_ONE_SECOND = 1000; // 1 second

    /* Audio Bearer switch request */
    static final int NEW_AUDIO_BEARER_REQ_BT = 0x00;
    static final int NEW_AUDIO_BEARER_REQ_P2P = 0x01;

    /* Audio Bearer switch response */
    public static final int AUDIO_BEARER_READY = 0x00;
    static final int AUDIO_BEARER_FAIL = 0x01;

    /* Requested SAP power state */
    static final int REQ_SAP_POWER_STATE_DEEP_SLEEP = 0x00;
    static final int REQ_SAP_POWER_STATE_ACTIVE = 0x01;

    /* Requested SAP power state response*/
    static final int POWER_STATE_RES_ACCEPT = 0x00;
    static final int POWER_STATE_RES_REJECT = 0x01;

    // GATT error codes
    static final int GATT_DISCONNECT_PEER = 0x16; //22
    static final int GATT_OUT_OF_RANGE = 0xFF; //255
    static final int GATT_VALUE_NOT_ALLOWED = 0x13; // 19
    static final int GATT_ERROR = 0x85; // 133

    // GATT Connection error codes
    static final int GATT_CONN_OK = 0;
    static final int GATT_CONN_L2C_FAILURE = 1;
    static final int GATT_CONN_TIMEOUT = 0x08;
    static final int GATT_CONN_TERMINATE_PEER = 0x13;
    static final int GATT_CONN_TERMINATE_LOCAL = 0x16;
    static final int GATT_CONN_LMP_TIMEOUT = 0x22;
    static final int GATT_CONN_FAILED_ESTABLISHMENT = 0x3E;
    static final int GATT_CONN_TERMINATED_POWER_OFF = 0x15;
    static final int GATT_CONN_NONE = 0x0101;

    // GATT operation codes
    static final int GATT_SUCCESS = BluetoothGatt.GATT_SUCCESS;
    static final int GATT_READ_NOT_PERMITTED = BluetoothGatt.GATT_READ_NOT_PERMITTED;
    static final int GATT_WRITE_NOT_PERMITTED = BluetoothGatt.GATT_WRITE_NOT_PERMITTED;
    static final int GATT_INSUFFICIENT_AUTHENTICATION
            = BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION;
    static final int GATT_REQUEST_NOT_SUPPORTED = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED;
    static final int GATT_INSUFFICIENT_ENCRYPTION = BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION;
    static final int GATT_INVALID_OFFSET = BluetoothGatt.GATT_INVALID_OFFSET;
    static final int GATT_INSUFFICIENT_AUTHORIZATION = BluetoothGatt.GATT_INSUFFICIENT_AUTHORIZATION;
    static final int GATT_INVALID_ATTRIBUTE_LENGTH = BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH;
    static final int GATT_CONNECTION_CONGESTED = BluetoothGatt.GATT_CONNECTION_CONGESTED;
    static final int GATT_FAILURE = BluetoothGatt.GATT_FAILURE;

    // Aidl Init Failed
    static final int AIDL_FAIL_REASON = 0x1; // Default

    //XPAN  SAP State
    static final int  SAP_STATE_ENABLED = WIFI_AP_STATE_ENABLED;
    static final int  SAP_STATE_ENABLING = WIFI_AP_STATE_ENABLING;// onStarted()
    static final int  SAP_STATE_DISABLED = WIFI_AP_STATE_DISABLED; // onStopped() -> XPAN_SAP_STATE_DISABLED
    static final int  SAP_STATE_FAILED = WIFI_AP_STATE_FAILED; // onFailed() -> XPAN_SAP_STATE_FAILED
    static final int  SAP_STATE_DISABLING = WIFI_AP_STATE_DISABLING;

    static final int STATE_DISCONNECTED = BluetoothProfile.STATE_DISCONNECTED;
    static final int STATE_CONNECTING = BluetoothProfile.STATE_CONNECTING;
    static final int STATE_CONNECTED = BluetoothProfile.STATE_CONNECTED;
    static final int STATE_DISCONNECTING = BluetoothProfile.STATE_DISCONNECTING;

    static final int BOND_NONE = BluetoothDevice.BOND_NONE;
    static final int BOND_BONDING = BluetoothDevice.BOND_BONDING;
    static final int BOND_BONDED  = BluetoothDevice.BOND_BONDED;

    // SAP or AP Security Mode
    static final int  SM_OPEN = 0x00; // SECURITY_TYPE_OPEN = 0;
    static final int  SM_WEP = 0x01; // SECURITY_TYPE_WEP = 1;
    static final int  SM_WPA = 0x02; // deprecated
    static final int  SM_WPA2 = 0x03; //SECURITY_TYPE_PSK = 2;
    static final int  SM_MIXED_MODE = 0x4; // SECURITY_TYPE_PSK SECURITY_TYPE_SAE
    static final int  SM_WPA3 = 0x5; //SECURITY_TYPE_WPA3_SAE SECURITY_TYPE_SAE = 4;
    static final int  SM_WPA3_TRANSITION = 0x06; // Later
    static final int  SM_ANY = 0x07; // Not required
    static final int  SM_UNKNOWN = 0x08; // All invalid

    // MAC Connection states
    static final int MAC_CONNECTED = 0;
    static final int MAC_DSICONNECTED = 1;
    static final int MAC_FORCE_SEND_TWT = 2;

    static final int SAP_IF_CREATE = 1;
    static final int SAP_IF_DELETE = 0;
    static final int SAP_IF_CREATE_SUCCESS = 0;

    static final int SAP_STATE_OFF = 0;
    static final int SAP_STATE_ON = 1;

    static final int SAP_ENABLE_LOW_POWER = 0;
    static final int SAP_DISABLE_LOW_POWER = 1;

    static final int STATUS_SUCESS = 0x00;
    static final int STATUS_FAILURE = 0x01;
    static final int STATUS_ALREADY_PRESENT = 0x02;

    static final int TRUE = 1;
    static final int FALSE = 0;

    static final int WIFI_SSR_STARTED =  0x00;
    static final int WIFI_SSR_COMPLETED =  0x01;

    // Role associated with IP Address
    public static final int ROLE_PRIMARY = 0x00;
    public static final int ROLE_SECONDARY = 0x01;

    // Bearer Preference Request
    public static final int BEARER_REQ_EB = 0x00;
    public static final int BEARER_REQ_PROFILE = 0x01;
    public static final int BEARER_REQ_WIFI = 0x02;
    public static final int BEARER_REQ_OTHER = 0x03;

    // IP Type
    public static final int TYPE_IPV4 = 0;
    public static final int TYPE_IPV6 = 1;
    public static final int SIZE_IPV4 = 4;
    public static final int SIZE_IPV6 = 16;

    static final String MAC_DEFAULT = "00:00:00:00:00:00";
    static byte DEFAULT_IP [] =  new byte[] {00, 00, 00, 00 };

    static final int LOC_LEFT = 0x00000001;
    static final int LOC_RIGHT = 0x00000002;

    static final int  GROUP_STREAM_STATUS_IDLE  = 0;
    static final int GROUP_STREAM_STATUS_STREAMING = 1;

    public static final int PROP_VALUE_MAX_LEN = 80;
    // Properties
    static final String PROP_LOWPOWER_INTERVAL = "persist.vendor.service.bt.xpan_lowpower_interval";
    static final String PROP_LOWPOWER_ENABLE = "persist.vendor.service.bt.xpan_lowpower_enable";
    static final String PROP_LOHS_OPEN = "persist.vendor.service.bt.xpan_lohs_open";
    // TO capture Sniffer logs
    static final String PROP_SNIFFER = "persist.vendor.service.bt.xpan_sniffer";
    static final String PROP_FIXED_CHANNEL = "persist.vendor.service.bt.xpan_fixed_chan";

    // QLL Support
    static final int QLL_NOT_SUPPORT = -1;
    static final int QLL_SUPPORT = 0;

    static final int EB_CONNECTED = 0;
    static final int EB_DISCONNECTED = 3;

    static final int IP_ADDRESS_OBTAINED = EB_CONNECTED;
    static final int IP_ADDRESS_OBTAIN_FAILED = 0x01;
    static final int FAILED_TO_CONNECT_SSID = 0x02;

    static final int PERIODICITY_LOSSLESS = 70; // To prevent audio gaps over AP
    static final int PERIODICITY_CALL = 30;

    static enum Requestor {
        EB, WIFI_VENDOR
    };

    static final int AP_AVAILABILITY_STARTED = 0;
    static final int AP_AVAILABILITY_COMPLETED = 1;
    static final int AP_AVAILABILITY_CANCELLED = 2;

    static final int USECASE_IDENTIFIER_AUDIO_STREAMING = 0x00;
    static final int USECASE_IDENTIFIER_VOICE_CALL = 0x01;
    static final int USECASE_IDENTIFIER_GAMING_WITHOUT_VB = 0x02;
    static final int USECASE_IDENTIFIER_GAMING_WITH_VB = 0x03;
    static final int USECASE_IDENTIFIER_STREO_RECORDING = 0x04;

    /* Wifi Driver Status */
    /*
     * Wifi Driver booted up successfully and xpan is supported by wifi.
     */
    static final int WIFI_DRV_ACTIVE = 0x00;

    /*
     * Wi-Fi driver is not ready, XM sends
     * OnWifiActive callback to profile when Wi-Fi is ready.
     */
    static final int WIFI_DRV_NOT_ACTIVE = 0x01;

    /*
     * Wi-Fi driver is active, but xpan is not supported by Wi-Fi
     */
    static final int WIFI_DRV_NOT_SUPPORTING_XPAN = 0x02;

    static final String DEFAULT_COUTRY ="IN";

    static final String IF_WLAN ="wlan";

    static String getBearerString(int bearer) {
        switch (bearer) {
            case BEARER_DEFAULT :
                return "DEFAULT";
            case BEARER_BR_EDR :
                return "BR_EDR";
            case BEARER_LE :
                return "LE";
            case BEARER_AP :
                return "AP";
            case BEARER_P2P :
                return "P2P";
            case BEARER_AP_PREP :
                return "AP_PREP";
            case BEARER_P2P_PREP :
                return "P2P_PREP";
            case BEARER_NONE :
                return "NONE";
            default:
                return bearer + " Unknown";
        }
    }

    static String getBearerResString(int status) {
        switch (status) {
            case BEARER_ACCEPTED :
                return "Accepted";
            case BEARER_REJECTED :
                return "Rejected";
            default:
                return status + " Unknown";
        }
    }

    static String getAcsString(int status) {
        switch (status) {
            case ACS_COMPLETED :
                return "Completed";
            case ACS_STARTED :
                return "Started";
            case ACS_FAILED :
                return "Failed";
            default:
                return status + " Unknown";
        }
    }

    static String getTwtTypeString(int type) {
        switch (type) {
            case TWT_SETUP:
                return "Setup";

            case TWT_TERMINATE:
                return "Terminate";

            case TWT_SUSPEND:
                return "Suspend";

            case TWT_RESUME:
                return "Resume";

            default:
                return type + " Unknown";
        }
    }

    static String getSapReqStr(int req) {
        switch (req) {
        case SAP_IF_CREATE:
            return "Create";
        case SAP_IF_DELETE:
            return "Delete";
        default:
            return req + " Unknown";
        }
    }

    static String getSapStateStr(int state) {
        switch (state) {
        case SAP_IF_CREATE_SUCCESS:
            return "Success";
        case STATUS_FAILURE:
            return "Fail";
        case STATUS_ALREADY_PRESENT:
            return "Present";
        default:
            return state + " Unknown";
        }
    }

    static String getWifiSsrStr(int state) {
        switch (state) {
        case WIFI_SSR_STARTED:
            return "Started";
        case WIFI_SSR_COMPLETED:
            return "Completed";
        default:
            return state + " Unknown";
        }
    }

    static String getTransPortStr(int reason) {
        switch (reason) {
        case TP_REASON_CONNECTED:
            return "Connected";
        case TP_REASON_REMOTE_R4_NOT_SUPPORT:
            return "DUT R4 not support";
        case TP_REASON_REMOTE_SAP_NOTSUPPORT:
            return "Remote SAP not support";
        case TP_REASON_CONNECTION_FAILURE:
            return "Connection Failure";
        case TP_REASON_DISCONNECT:
            return "Disconnect";
        case TP_REASON_UNPAIRED:
            return "Unpaired";
        case TP_REASON_AIRPLANE_ENABLED:
            return "Airplane Mode Enabled";
        case TP_REASON_SSR_STARTED:
            return "SSR Started";
        default:
            return reason + " Unknown";
        }
    }

	static String getMdnsStrFromQueryState(int state) {
        String str = "Unknown";
        switch (state) {
        case DISCOVERY_START:
            str = "Start";
            break;
        case DISCOVERY_STOP:
            str = "Stop";
            break;
        }
        return str;
    }

    static String getMdnsStrFromState(int state) {
        String str = "Unknown";
        switch (state) {
        case MDNS_REGISTER:
            str = "Register";
            break;
        case MDNS_UNREGISTER:
            str = "UnRegister";
            break;
        }
        return str;
    }

    static String getGattDiscStrFromStatus(int status) {
        String str = "";
        switch (status) {
        case GATT_CONN_OK:
            str = "OK";
            break;
        case GATT_CONN_L2C_FAILURE:
            str = "L2C_FAILURE";
            break;
        case GATT_CONN_TIMEOUT:
            str = "Timeout";
            break;
        case GATT_CONN_TERMINATE_PEER:
            str = "TERMINATE_PEER";
            break;
        case GATT_CONN_TERMINATE_LOCAL:
            str = "TERMINATE_LOCAL";
            break;
        case GATT_CONN_LMP_TIMEOUT:
            str = "LMP_TIMEOUT";
            break;
        case GATT_CONN_FAILED_ESTABLISHMENT:
            str = "FAILED_ESTABLISHMENT";
            break;
        case GATT_CONN_TERMINATED_POWER_OFF:
            str = "TERMINATED_POWER_OFF";
            break;
        case GATT_OUT_OF_RANGE:
            str = "OUT_OF_RANGE";
            break;
        case GATT_CONN_NONE:
            str = "NONE";
            break;
        default:
            str = "Unknown " + status;
            break;
        }
        return str;
    }

    static String getConnStrFromState(int state) {
        String str = "";
        switch (state) {
        case STATE_DISCONNECTED:
            str = "DISCONNECTED";
            break;
        case STATE_CONNECTED:
            str = "CONNECTED";
            break;
        case STATE_CONNECTING:
            str = "CONNECTING";
            break;
        case STATE_DISCONNECTING:
            str = "DISCONNECTING";
            break;
        default:
            str = "Unknown "+state;
            break;
        }
        return str;
    }

    static String getBondStrFromState(int state) {
        String str = "";
        switch (state) {
        case BOND_NONE:
            str = "NONE";
            break;
        case BOND_BONDED:
            str = "BONDED";
            break;
        case BOND_BONDING:
            str = "BONDING";
            break;
        default:
            str = "Unknown "+state;
            break;
        }
        return str;
    }

    static String getStreamingString(int state) {
        String str = "";
        switch (state) {
        case GROUP_STREAM_STATUS_STREAMING:
            str = "Streaming";
            break;
        case GROUP_STREAM_STATUS_IDLE:
            str = "Idle";
            break;
        default:
            str = "Unknown " + state;
            break;
        }
        return str;
    }

    static String getStrFromGatStatus(int status) {
        switch (status) {
        case GATT_SUCCESS:
            return "SUCCESS";
        case GATT_READ_NOT_PERMITTED:
            return "READ_NOT_PERMITTED";
        case GATT_WRITE_NOT_PERMITTED:
            return "WRITE_NOT_PERMITTED";
        case GATT_INSUFFICIENT_AUTHENTICATION:
            return "INSUFFICIENT_AUTHENTICATION";
        case GATT_REQUEST_NOT_SUPPORTED:
            return "REQUEST_NOT_SUPPORTED";
        case GATT_INVALID_OFFSET:
            return "INVALID_OFFSET";
        case GATT_INSUFFICIENT_AUTHORIZATION:
            return "INSUFFICIENT_AUTHORIZATION";
        case GATT_INVALID_ATTRIBUTE_LENGTH:
            return "INVALID_ATTRIBUTE_LENGTH";
        case GATT_INSUFFICIENT_ENCRYPTION:
            return "INSUFFICIENT_ENCRYPTION";
        case GATT_VALUE_NOT_ALLOWED:
            return "VALUE_NOT_ALLOWED";
        case GATT_DISCONNECT_PEER:
            return "DISCONNECT_PEER";
        case GATT_CONN_FAILED_ESTABLISHMENT:
            return "CONN_FAILED_ESTABLISHMENT";
        case GATT_ERROR:
            return "GATT_ERROR";
        case GATT_CONNECTION_CONGESTED:
            return "CONNECTION_CONGESTED";
        case GATT_OUT_OF_RANGE:
            return "OUT_OF_RANGE";
        case GATT_FAILURE:
            return "FAILURE";
        default:
            return "Unknown " + status;
        }
    }

    static String getStrfromOpCode(int opcode) {
        switch (opcode) {
        case OPCODE_CP_CONNECT_SSID:
            return "CONNECT_SSID";
        case OPCODE_CP_DISCONNECT_SSID:
            return "DISCONNECT_SSID";
        case OPCODE_CP_CONNECTED_SSID:
            return "CONNECTED_SSID";
        case OPCODE_CP_AVAILABLE_SSID:
            return "AVAILABLE_SSID";
        case OPCODE_CP_RELATED_SSID:
            return "RELATED_SSID";
        case OPCODE_CP_MDNS_SRV_UUID:
            return "MDNS_SRV_UUID";
        case OPCODE_CP_REMOVE_SSID:
            return "REMOVE_SSID";
        case OPCODE_CP_L2CAP_TCP_PORT:
            return "L2CAP_TCP_PORT";
        case OPCODE_CP_UDP_PORT:
            return "UDP_PORT";
        case OPCODE_CP_ETHER_TYPE:
            return "ETHER_TYPE";
        case OPCODE_CP_MAC_ADDRESS:
            return "MAC_ADDRESS";
        case OPCODE_CP_BEARER_PREFRENCE_RES:
            return "BEARER_PREFRENCE_RES";
        case OPCODE_CP_UPDATE_BEACON_PARAMETERS:
            return "UPDATE_BEACON_PARAMETERS";
        case OPCODE_CP_SAP_POWER_STATE_RESPONSE:
            return "SAP_POWER_STATE_RESPONSE";
        case OPCODE_CP_CLIENT_FEATURES:
            return "CLIENT_FEATURES";
        case OPCODE_CP_AUDIO_BEARER_SWITCH_REQ:
            return "AUDIO_BEARER_SWITCH_REQ";
        case OPCODE_CP_WIFI_SCAN_RESULTS:
            return "WIFI_SCAN_RESULTS";
        case OPCODE_CP_CSA:
            return "CP_CSA";
        case OPCODE_CP_UDP_SYNC_PORT:
            return "UDP_SYNC_PORT";
        case OPCODE_CP_ROAMING_REQUEST:
            return "ROAMING_REQUEST";
        case OPCODE_CP_MULTICAST_MAC_ADDRESS:
            return "MULTICAST_MAC_ADDRESS";
        case OPCODE_CP_USECASE_IDENTIFIER:
            return "USECASE_IDENTIFIER";
        case OPCODE_CP_SAP_STATE:
            return "SAP_STATE";
        case OPCODE_CP_BEARER_SWITCH_FAILED:
            return "BEARER_SWITCH_FAILED";

        default:
            return opcode + " Unknown";
        }
    }
}
