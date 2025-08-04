/*****************************************************************
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 ******************************************************************/

package vendor.qti.bluetooth.xpan;

import java.io.Serializable;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

import com.android.internal.util.State;
import com.android.internal.util.IState;
import com.android.internal.util.StateMachine;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.net.MacAddress;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import vendor.qti.bluetooth.xpan.XpanConstants.Requestor;
import vendor.qti.bluetooth.xpan.XpanProviderClient.OpCode;

/**
 * Bluetooth Xpan Gatt Connection State Machine
 *
 *                        (Disconnected)
 *                           |       ^
 *                   CONNECT |       | DISCONNECTED
 *                           V       |
 *                 (Connecting)<--->(Disconnecting)
 *                           |       ^
 *                 CONNECTED |       | DISCONNECT
 *                           V       |
 *                          (Connected)
 */

final class XpanGattStateMachine extends StateMachine {
    private final String TAG;
    private static final boolean VDBG = XpanUtils.VDBG;
    private static final boolean DBG = XpanUtils.DBG;

    public static final int CONNECT = 1;
    public static final int DISCONNECT = 2;
    private final int CONNECTION_STATE_CHANGED = 3;
    private final int READ_GATT_CHARS = 4;
    static final int CONNECT_SSID = 5;
    static final int DISCONNECT_SSID = 6;
    static final int TWT_CONFIG = 7;
    static final int UPDATE_BEACON_INTERVAL = 8;
    private final int SEND_ETHER_TYPE = 9;
    private final int SEND_MAC_ADDRESS = 10;
    static final int POWER_STATE_RESPONSE = 11;
    private final int BEARER_PREFERENCE_RESPONSE = 12;
    private final int AUDIO_BEARER_SWITCH_REQ = 13;
    private final int SEND_CLIENT_FEATURES = 14;
    static final int CONNECTED_SSID = 15;
    static final int CSA = 16;
    static final int WIFI_SCAN_RESULT = 17;
    private final int MDNS_SRV_UUID = 18;
    static final int UPDATE_UDP_PORT = 19;
    static final int UPDATE_L2CAP_TCP_PORT = 20;
    static final int UPDATE_UDP_SYNC_PORT = 21;
    static final int UPDATE_USECASE_IDENTIFIER = 22;
    static final int TWT_EMPTY = 23;
    static final int DISCONNECT_SSID_AND_GATT = 24;
    private final int REGISTER_NOTIFICATION = 25;
    private final int CONNECT_XPAN = 26;
    static final int UPDATE_SAP_STATE = 27;
    static final int READ_BREARER_PREF = 28;
    static final int UPDATE_BEARER_SWITCH_FAILED = 29;

    private final int XPAN_MTU = 512;

    private final Disconnected mDisconnected;
    private final Connected mConnected;
    private final Connecting mConnecting;
    private final Disconnecting mDisconnecting;

    /* States */
    private final int DISCONNECTED = BluetoothProfile.STATE_DISCONNECTED;
    private final int CONNECTING = BluetoothProfile.STATE_CONNECTING;
    static final int CONNECTED = BluetoothProfile.STATE_CONNECTED;
    private final int DISCONNECTING = BluetoothProfile.STATE_DISCONNECTING;

    private final BluetoothDevice mDevice;
    private XpanDevice mXpanDevice;
    private BluetoothGatt mBluetoothGatt;
    private XpanProfileService mService;
    private Queue<BluetoothGattCharacteristic> charQueue =
            new ArrayDeque<BluetoothGattCharacteristic>();
    private Queue<XpanTask> xpanTaskQueue =
            new ArrayDeque<XpanTask>();
    private Queue<GattCharData> mWriteQueue = new ArrayDeque<GattCharData>();
    private XpanUtils mXpanUtils;
    private List<RemoteMacAddress> mListMacAddresses= new ArrayList<RemoteMacAddress>();
    private int mPeriodicity;
    private int mBearer;
    private WifiUtils mWifiUtils;
    private int mConnState = -1;
    private int mBearerPreference = XpanConstants.BEARER_DEFAULT;
    private int mPortL2capTcp;
    private int mPortUdpAudio, mPortUdpReports;
    private XpanEb mEb = null;
    private XpanDataParser mDataParser;
    private GattResponseCallback mGattResCallback;
    private XpanApStateMachine mApSm;
    private boolean mEtherTypeSent = false, mWritePending = false, mConnectSsidSent = false,
            mDisconnectGatt = false, mConfig = false, mIsPortUpdatePending = false,
            mSerDiscovered = false;

    private XpanGattStateMachine(BluetoothDevice device, XpanProfileService svc,
            Looper looper, String tag) {
        super(tag, looper);
        if (XpanUtils.DBGSM) {
            setDbg(true);
        }
        TAG = tag;
        if (DBG) Log.d(TAG, "XpanGattStateMachine " + device);
        mDevice = device;
        mService = svc;
        mXpanDevice = new XpanDevice();
        mDataParser = new XpanDataParser();
        mXpanUtils = XpanUtils.getInstance(mService);
        mWifiUtils = WifiUtils.getInstance(mService.getApplicationContext());
        mEb = new XpanEb(mDevice);
        mDisconnected = new Disconnected();
        mDisconnecting = new Disconnecting();
        mConnected = new Connected();
        mConnecting = new Connecting();
        mXpanUtils = XpanUtils.getInstance(mService);
        mApSm = svc.getApSm();
        addState(mDisconnected);
        addState(mDisconnecting);
        addState(mConnected);
        addState(mConnecting);
        setInitialState(mDisconnected);

    }

    static XpanGattStateMachine make(BluetoothDevice device,
                                      XpanProfileService svc,Looper looper) {
        String tag = "XpanGatt"+"_"
                + device.getAddress().replaceAll(":","").substring(6);
        XpanGattStateMachine xpanGattSm = new XpanGattStateMachine(device, svc, looper, tag);
        xpanGattSm.start();
        return xpanGattSm;
    }

    private void init() {
        mEtherTypeSent = false;
        charQueue.clear();
        xpanTaskQueue.clear();
        mPeriodicity = -1;
        mBearer = XpanConstants.BEARER_DEFAULT;
        mConnectSsidSent = false;
        mConnState = -1;
        mBearerPreference = XpanConstants.BEARER_DEFAULT;
        mDisconnectGatt = false;
        mConfig = false;
        mSerDiscovered = false;
    }

    class Disconnected extends State {

        @Override
        public void enter() {
            log("Enter Disconnected ");
            mConnState = DISCONNECTED;
            init();
            mGattResCallback.onGattReadStatus(false, mBearerPreference);
        }

        @Override
        public boolean processMessage(Message message) {
            int msg = message.what;
            log("Disconnected processMessage " + messageToString(msg));

            switch (msg) {
                case CONNECT:
                    log("Connecting to " + mDevice);
                    closeGatt();
                    mBluetoothGatt = mDevice.connectGatt(mService, false, mGattCallbacks,
                            BluetoothDevice.TRANSPORT_LE, (BluetoothDevice.PHY_LE_1M_MASK |
                            BluetoothDevice.PHY_LE_2M_MASK | BluetoothDevice.PHY_LE_CODED_MASK),
                            mService.getXpanHandler());
                    if (mBluetoothGatt == null) {
                       Log.e(TAG, "Failed to initiate GATT Connection");
                       return NOT_HANDLED;
                    }
                    transitionTo(mConnecting);
                    break;

                case CONNECTION_STATE_CHANGED:
                    if (VDBG)
                        Log.v(TAG, "CONNECTION_STATE_CHANGED " + message.arg1);
                    break;

                default:
                    log("Not handled message" + msg);
                    return NOT_HANDLED;
            }
            return HANDLED;
        }
    }

    class Connecting extends State {

         @Override
         public void enter() {
             log( "Enter Connecting ");
             mConnState = CONNECTING;
         }

         @Override
         public boolean processMessage(Message message) {
             log("Connecting processMessage " + messageToString(
                     message.what));

             switch (message.what) {
                 case CONNECT:
                      log("Connection already in progress for " + mDevice);
                      break;

                 case DISCONNECT:
                      // Cancel in progress connection
                      disconnectGatt();
                      break;

                 case CONNECTION_STATE_CHANGED:
                      int state = message.arg1;
                      if (state == BluetoothProfile.STATE_CONNECTED) {
                          transitionTo(mConnected);
                      } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                          transitionTo(mDisconnected);
                      }
                      break;

                 default:
                      log("DISCONNECTED: unhandled message:" + message.what);
                     return NOT_HANDLED;
             }
             return HANDLED;
         }
     }

    class Connected extends State {
        @Override
        public void enter() {
            log("Enter Connected");
            mBluetoothGatt.discoverServices();
            mConnState = CONNECTED;
            mWritePending = true;
            mGattResCallback.onGattConnnected();
        }

        @Override
        public void exit() {
            mWriteQueue.clear();
        }

        @Override
        public boolean processMessage(Message message) {
            log("Connected processMessage "
                    + messageToString(message.what));

            switch (message.what) {
                case CONNECT:
                    Log.w(TAG, "Already Connected, ignore msg ");
                    break;

                case DISCONNECT:
                    disconnectGatt();
                    break;

                case CONNECTION_STATE_CHANGED:
                    int state = message.arg1;
                    if (state == BluetoothProfile.STATE_CONNECTED) {
                        transitionTo(mConnected);
                    } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                        transitionTo(mDisconnected);
                    }
                    break;

                case READ_GATT_CHARS:
                    XpanTask task = new XpanTask(XpanTask.GATT_CHARS_READ_OP);
                    xpanTaskQueue.add(task);
                    charQueue.clear();

                    addinQueue(mXpanDevice.mDnsUuid);
                    addinQueue(mXpanDevice.serverFeatures);
                    addinQueue(mXpanDevice.clearToSend);
                    addinQueue(mXpanDevice.macAddress);
                    addinQueue(mXpanDevice.numDevicesPresent);
                    addinQueue(mXpanDevice.voicebackchannelperiodicity);
                    /* WAR: to Handle Bearer read issue in EB
                    addinQueue(mXpanDevice.bearerPreference); */
                    addinQueue(mXpanDevice.l2capTcpPort);
                    addinQueue(mXpanDevice.udpPort);
                    addinQueue(mXpanDevice.ipv4Address);

                    BluetoothGattCharacteristic ch = charQueue.poll();
                    if (ch != null) {
                        readCharacteristicValue(ch);
                    }
                    break;

                case REGISTER_NOTIFICATION:
                    configureXpanGattChar();
                    break;

                case CONNECT_XPAN:
                    mConfig = true;
                    mWritePending = false;
                    connectXpan();
                    break;

                case TWT_CONFIG:
                    byte twtData[] = (byte[]) message.obj;
                    mEtherTypeSent = true;
                    writeCharacteristic(mXpanDevice.twtConfig, twtData);
                    break;

                case TWT_EMPTY:
                    byte twtDataEmpty[] = (byte[]) message.obj;
                    writeCharacteristic(mXpanDevice.twtConfig, twtDataEmpty);
                    break;

                case UPDATE_BEACON_INTERVAL:
                    byte beaconData[] = (byte[]) message.obj;
                    writeCharacteristic(mXpanDevice.xpanControlPoint, beaconData);
                    break;

                case SEND_ETHER_TYPE:
                    int etherType = message.arg1;
                    writeCharacteristic(mXpanDevice.xpanControlPoint,
                            mDataParser.getEtherTypeBytes(etherType));
                    break;

                case SEND_MAC_ADDRESS:
                     MacAddress mac = (MacAddress) message.obj;
                     writeCharacteristic(mXpanDevice.xpanControlPoint,
                             mDataParser.getMacAddressBytes(mac));
                    break;

                case AUDIO_BEARER_SWITCH_REQ :
                    int bearer = message.arg1;
                    mBearer = bearer;
                    byte val [] = mDataParser.getAudioBearerSwitchRequestBytes(bearer);
                    if (val == null) {
                        Log.w(TAG, " Incorrect AUDIO_BEARER_SWITCH_REQ " + bearer);
                        break;
                    }
                    writeCharacteristic(mXpanDevice.xpanControlPoint, val);
                    break;

                case BEARER_PREFERENCE_RESPONSE:
                    int bearerResponse = message.arg1;
                    writeCharacteristic(mXpanDevice.xpanControlPoint,
                            mDataParser.getBearerPreferenceResponseBytes(bearerResponse));
                    break;

                case SEND_CLIENT_FEATURES :
                    writeCharacteristic(mXpanDevice.xpanControlPoint,
                            mDataParser.getClientFeatureBytes());
                    break;

                case POWER_STATE_RESPONSE:
                case DISCONNECT_SSID:
                case MDNS_SRV_UUID:
                case WIFI_SCAN_RESULT:
                case CSA:
                case CONNECTED_SSID:
                case CONNECT_SSID:
                case UPDATE_UDP_PORT:
                case UPDATE_L2CAP_TCP_PORT:
                case UPDATE_UDP_SYNC_PORT:
                case UPDATE_USECASE_IDENTIFIER:
                case UPDATE_SAP_STATE:
                case UPDATE_BEARER_SWITCH_FAILED:
                    byte data[] = (byte[]) message.obj;
                    writeCharacteristic(mXpanDevice.xpanControlPoint, data);
                    break;

                case DISCONNECT_SSID_AND_GATT:
                    byte dataSsid[] = (byte[]) message.obj;
                    writeCharacteristic(mXpanDevice.xpanControlPoint, dataSsid);
                    mDisconnectGatt = true;
                    break;

                case READ_BREARER_PREF:
                    readCharacteristicValue(mXpanDevice.bearerPreference);
                    break;

                default:
                    log("CONNECTED: unhandled message:" + message.what);
                    return NOT_HANDLED;
            }
            return HANDLED;
        }
    }

    class Disconnecting extends State {

        @Override
       public void enter() {
           log( "Enter Disconnecting");
           mConnState = DISCONNECTING;
       }

       @Override
       public boolean processMessage(Message message) {
           log("Disconnecting processMessage " + messageToString(
                   message.what));
           switch (message.what) {
               case CONNECT:
                    log("Disconnecting to " + mDevice);
                    break;
               case DISCONNECT:
                    Log.w(TAG, "Already disconnecting: DISCONNECT ignored: " + mDevice);
                    break;

               case CONNECTION_STATE_CHANGED:
                    int state = message.arg1;
                    if (state == BluetoothProfile.STATE_CONNECTED) {
                        transitionTo(mConnected);
                    } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                        transitionTo(mDisconnected);
                    }

               default:
                   return NOT_HANDLED;
           }
           return HANDLED;
       }
    }

    private void disconnectGatt() {
        if (DBG)
            Log.d(TAG, "disconnectGatt " + mBluetoothGatt);

        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            transitionTo(mDisconnecting);
        } else {
            transitionTo(mDisconnected);
        }
    }

    private String messageToString(int what) {
        switch (what) {
            case CONNECT:
                return "CONNECT";
            case DISCONNECT:
                return "DISCONNECT";
            case CONNECTION_STATE_CHANGED:
                return "CONNECTION_STATE_CHANGED";
            case READ_GATT_CHARS:
                return "READ_GATT_CHARS";
            case CONNECT_SSID:
                return "CONNECT_SSID";
            case DISCONNECT_SSID:
                return "DISCONNECT_SSID";
            case TWT_CONFIG:
                return "TWT_CONFIG";
            case UPDATE_BEACON_INTERVAL:
                return "UPDATE_BEACON_INTERVAL";
            case SEND_ETHER_TYPE:
                return "SEND_ETHER_TYPE";
            case SEND_MAC_ADDRESS:
                return "SEND_MAC_ADDRESS";
            case POWER_STATE_RESPONSE:
                return "POWER_STATE_RESPONSE";
            case BEARER_PREFERENCE_RESPONSE:
                return "BEARER_PREFERENCE_RESPONSE";
            case AUDIO_BEARER_SWITCH_REQ:
                return "AUDIO_BEARER_SWITCH_REQ";
            case SEND_CLIENT_FEATURES:
                return "SEND_CLIENT_FEATURES";
            case CONNECTED_SSID:
                return "CONNECTED_SSID";
            case CSA:
                return "CSA";
            case WIFI_SCAN_RESULT:
                return "WIFI_SCAN_RESULT";
            case MDNS_SRV_UUID:
                return "MDNS_SRV_UUID";
            case UPDATE_UDP_PORT:
                return "UPDATE_UDP_PORT";
            case UPDATE_L2CAP_TCP_PORT:
                return "UPDATE_L2CAP_TCP_PORT";
            case UPDATE_UDP_SYNC_PORT:
                return "UPDATE_UDP_SYNC_PORT";
            case UPDATE_USECASE_IDENTIFIER:
                return "UPDATE_USECASE_IDENTIFIER";
            case TWT_EMPTY:
                return "TWT_EMPTY";
            case XPAN_MTU:
                return "XPAN_MTU";
            case REGISTER_NOTIFICATION:
                return "REGISTER_NOTIFICATION";
            case CONNECT_XPAN:
                return "CONNECT_XPAN";
            case UPDATE_SAP_STATE:
                return "UPDATE_SAP_STATE";
            case READ_BREARER_PREF:
                return "READ_BREARER_PREF";
            default:
                break;
        }
        return Integer.toString(what);
    }

    private final BluetoothGattCallback mGattCallbacks = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int state) {
            if(DBG) Log.d(TAG, "onConnectionStateChange "
                    + XpanConstants.getGattDiscStrFromStatus(status)
                    + " " + XpanConstants.getConnStrFromState(state));

            if (status == BluetoothGatt.GATT_SUCCESS
                    || status == BluetoothGatt.GATT_FAILURE
                    || status == XpanConstants.GATT_OUT_OF_RANGE
                    || status == XpanConstants.GATT_VALUE_NOT_ALLOWED
                    || state == BluetoothProfile.STATE_DISCONNECTED) {
                Message msg = obtainMessage(CONNECTION_STATE_CHANGED);
                msg.arg1 = state;
                sendMessage(msg);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (DBG) Log.d(TAG, "onServicesDiscovered "
                    + XpanConstants.getStrFromGatStatus(status));

            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "Failed to discover services");
                return;
            }
            if (!parseServices()) {
                Log.w(TAG, "xpan not supported");
                disconnect();
                return;
            }
            mSerDiscovered = true;
            if (!mBluetoothGatt.requestMtu(XPAN_MTU)) {
                Log.w(TAG, "requestMtu failed retry");
                // Retry Request MTU
                if (!mBluetoothGatt.requestMtu(XPAN_MTU)) {
                    Log.w(TAG, "requestMtu failed again");
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic ch,
                byte[] value, int status) {
            UUID charUuid = ch.getUuid();
            if (DBG) Log.d(TAG, "onCharacteristicRead " + XpanUuid.uuidToName(ch));
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "onCharacteristicRead failed " + status);
                return;
            }

            XpanTask task = xpanTaskQueue.peek();
            if (task != null) {
                switch (task.taskId) {
                    case XpanTask.GATT_CHARS_READ_OP:
                    if (XpanUuid.IPV4_ADDRESS.equals(charUuid)) {
                        ipv4NotificationReceived(ch);
                        if (xpanTaskQueue.peek() != null)
                           sendClientFeatures();
                    } else if (XpanUuid.IPV6_ADDRESS.equals(charUuid)) {
                        ipv6NotificationReceived(ch);
                    } else if (XpanUuid.L2CAP_TCP_PORT.equals(charUuid)) {
                        updateL2capPort(ch);
                    } else if (XpanUuid.UDP_PORT.equals(charUuid)) {
                        updateUdpPort(ch);
                    } else if (XpanUuid.MDNS_SERVICE_UUID.equals(charUuid)) {
                        updateMdnsUuid(ch);
                    }  else if (XpanUuid.CLEAR_TO_SEND.equals(charUuid)) {
                        updateClearToSend(ch);
                    } else if (XpanUuid.MAC_ADDRESS.equals(charUuid)) {
                        mDataParser.updateMacAddress(value, mEb);
                    } else if (XpanUuid.REQ_SAP_POWER_STATE.equals(charUuid)) {
                        updateRequestedSapPowerState(ch);
                    } else if (XpanUuid.SERVER_FEATURES.equals(charUuid)) {
                        updateServerFeatures(ch);
                    } else if (XpanUuid.BEARER_PREFERENCE.equals(charUuid)) {
                        updateBearerPreferenceRead(ch);
                    } else if (XpanUuid.AUDIO_BEARER_SWITCH_RESPONSE.equals(charUuid)) {
                        updateAudioBearerSwitchRes(ch);
                    } else if (XpanUuid.NUM_DEVICES_PRESENT.equals(charUuid)) {
                        updateNumDevicesPresent(ch);
                    } else if (XpanUuid.VOICE_BACK_CHANNEL_PERIODICITY.equals(charUuid)) {
                        updateVBCPeriodicity(ch);
                    }

                    BluetoothGattCharacteristic nextChar= charQueue.poll();
                    if (nextChar != null) {
                        readCharacteristicValue(nextChar);
                    } else {
                        mWritePending = false;
                        xpanTaskQueue.poll();
                    }
                    break;
                    default :

                    if (DBG)
                        Log.d(TAG, "onCharacteristicRead not handled task id " + task.taskId);
                    break;
                }
            } else if (XpanUuid.BEARER_PREFERENCE.equals(charUuid)) {
                updateBearerPreferenceRead(ch);
                sendMessage(CONNECT_XPAN);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
            BluetoothGattCharacteristic ch, int status) {
            UUID charUuid = ch.getUuid();

            if (VDBG)
                Log.v(TAG, "onCharacteristicWrite " + XpanUuid.uuidToName(ch));
            mWritePending = false;
            if (status != BluetoothGatt.GATT_SUCCESS) {
                mWriteQueue.clear();
                Log.w(TAG, "onCharacteristicWrite fail " + status);
                return;
            }
            if (XpanUuid.TWT_CONFIGURATION.equals(charUuid)) {
                if (!mEtherTypeSent) {
                    sendEtherType();
                }
            } else if (XpanUuid.XPAN_CONTROL_POINT.equals(charUuid)) {
                byte val[] = ch.getValue();
                if (val == null || val.length == 0) {
                    Log.w(TAG, "onCharacteristicWrite no data returned");
                    return;
                }
                int opcode = val[0];
                switch (opcode) {
                case XpanConstants.OPCODE_CP_ETHER_TYPE :
                    mEtherTypeSent = true;
                    break;

                case XpanConstants.OPCODE_CP_MAC_ADDRESS:
                    if (mApSm.getUdpPortAudio() != 0) {
                        mGattResCallback.onReqSendPortUpdates();
                    } else {
                        sendMessage(REGISTER_NOTIFICATION);
                    }
                    break;

                case XpanConstants.OPCODE_CP_UDP_SYNC_PORT:
                    if (!isConfigured()) {
                        mIsPortUpdatePending = false;
                        sendMessage(REGISTER_NOTIFICATION);
                        mGattResCallback.onPortCofigEnableSapOnAp();
                    }
                    break;

                case XpanConstants.OPCODE_CP_MDNS_SRV_UUID:
                    sendMacAddress();
                    break;

                case XpanConstants.OPCODE_CP_CLIENT_FEATURES:
                    sendData(MDNS_SRV_UUID,
                            mDataParser.getMdnsSrvBytes(mXpanUtils.getUuidLocal()));
                    break;

                case XpanConstants.OPCODE_CP_CONNECT_SSID:
                    mConnectSsidSent = true;
                    break;

                case XpanConstants.OPCODE_CP_DISCONNECT_SSID:
                    mConnectSsidSent = false;
                    if (mDisconnectGatt) {
                        disconnectGatt();
                    }
                    break;

                case XpanConstants.OPCODE_CP_BEARER_PREFRENCE_RES:
                    mGattResCallback.onBearerPreferenceResponseSent(mBearerPreference);
                    break;

                case XpanConstants.OPCODE_CP_CONNECTED_SSID:
                case XpanConstants.OPCODE_CP_UPDATE_BEACON_PARAMETERS :
                case XpanConstants.OPCODE_CP_AUDIO_BEARER_SWITCH_REQ:
                case XpanConstants.OPCODE_CP_USECASE_IDENTIFIER:
                case XpanConstants.OPCODE_CP_CSA:
                case XpanConstants.OPCODE_CP_SAP_STATE:
                case XpanConstants.OPCODE_CP_BEARER_SWITCH_FAILED:
                    break;

                case XpanConstants.OPCODE_CP_SAP_POWER_STATE_RESPONSE:
                    mGattResCallback.onPowerStateResponseUpdated();
                    break;

                case XpanConstants.OPCODE_CP_WIFI_SCAN_RESULTS:
                    mGattResCallback.onWifiScanResultUpdated();
                    break;

                case XpanConstants.OPCODE_CP_L2CAP_TCP_PORT:
                    mGattResCallback.onL2capTcpPortUpdated();
                    break;

                case XpanConstants.OPCODE_CP_UDP_PORT:
                    mGattResCallback.onUdpPortUpdated();
                    break;

                default:
                    if(VDBG) Log.v(TAG, "opcode not logged " + opcode);
                    break;
                }
            }
            writePending();
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                BluetoothGattCharacteristic ch, byte [] val) {

            if (mConnState != CONNECTED) {
                return;
            }

            UUID charUuid = ch.getUuid();
            if (DBG) Log.d(TAG, "onCharacteristicChanged "
                    + XpanUuid.uuidToName(ch));
            if (XpanUuid.BEARER_PREFERENCE.equals(charUuid)) {
                updateBearerPreference(ch);
            } else if (XpanUuid.IPV4_ADDRESS.equals(charUuid)) {
                ipv4NotificationReceived(ch);
            } else if (XpanUuid.IPV6_ADDRESS.equals(charUuid)) {
                ipv6NotificationReceived(ch);
            } else if (XpanUuid.L2CAP_TCP_PORT.equals(charUuid)) {
                updateL2capPort(ch);
            } else if (XpanUuid.UDP_PORT.equals(charUuid)) {
                updateUdpPort(ch);
            }  else if (XpanUuid.CLEAR_TO_SEND.equals(charUuid)) {
                updateClearToSend(ch);
            } else if (XpanUuid.REQ_SAP_POWER_STATE.equals(charUuid)) {
                updateRequestedSapPowerState(ch);
            } else if (XpanUuid.SERVER_FEATURES.equals(charUuid)) {
                updateServerFeatures(ch);
            } else if (XpanUuid.AUDIO_BEARER_SWITCH_RESPONSE.equals(charUuid)) {
                updateAudioBearerSwitchRes(ch);
            } else if (XpanUuid.NUM_DEVICES_PRESENT.equals(charUuid)) {
                updateNumDevicesPresent(ch);
            } else if(XpanUuid.VOICE_BACK_CHANNEL_PERIODICITY.equals(charUuid)) {
                updateVBCPeriodicity(ch);
            } else if (XpanUuid.MAC_ADDRESS.equals(charUuid)) {
                mDataParser.updateMacAddress(val, mEb);
            } else if (XpanUuid.REQ_WIFI_SCAN_RESULTS.equals(charUuid)) {
                mGattResCallback.onReqWifiScanResult();
            }  else if (XpanUuid.WIFI_CHANNEL_SWITCH_REQUEST.equals(charUuid)) {
                int channel = ch
                        .getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
                mGattResCallback.onWifiChannelSwitchReq(channel);
            } else if (XpanUuid.TWT_STATUS.equals(charUuid)) {
                int loc = ch.getIntValue(
                        BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                if (VDBG)
                    Log.v(TAG, "TWT_STATUS " + loc);
                mGattResCallback.onSapClientsStateChange(loc,
                        XpanProviderClient.OpCode.TERMINATE_TWT);
            } else if (XpanUuid.SAP_CONN_STATUS.equals(charUuid)) {
                int location = ch.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                if (VDBG)
                    Log.v(TAG, "SAP_CONN_STATUS " + location);
                mGattResCallback.onSapClientsStateChange(location,
                        XpanProviderClient.OpCode.DISCONNECT_SAP_CLIENTS);
            } else {
                if(DBG) Log.d(TAG, "onCharacteristicChanged not handled");
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                int status) {
            if (VDBG) {
                Log.v(TAG, "onDescriptorWrite "
                       + XpanUuid.uuidToName(descriptor.getCharacteristic()));
            }

            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattCharacteristic ch = charQueue.poll();
                if (ch != null) {
                    registerCharForNotification(ch);
                } else {
                    sendMessage(READ_BREARER_PREF);
                }
            } else {
                if (DBG) {
                    Log.w(TAG, "onDescriptorWrite "
                           + XpanUuid.uuidToName(descriptor.getCharacteristic())
                           + " status " + XpanConstants.getStrFromGatStatus(status));
                }
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            if (DBG) Log.d(TAG, "onMtuChanged " + mtu + " "
                    + XpanConstants.getStrFromGatStatus(status));
            // Todo : check MTU fail reason
            if (/* mtu >= XPAN_MTU && */status == BluetoothGatt.GATT_SUCCESS && mSerDiscovered) {
                sendMessage(READ_GATT_CHARS);
            } else {
                if (DBG) {
                    Log.w(TAG, "READ_GATT_CHARS " + mSerDiscovered);
                }
            }
        }

        @Override
        public void onServiceChanged(BluetoothGatt gatt) {
            if (DBG) Log.d(TAG, "onServiceChanged rediscover services");
        }
    };

    private void ipv4NotificationReceived(BluetoothGattCharacteristic ch) {
        byte value[] = ch.getValue();
        if (value == null || value.length <= 0) {
            Log.w(TAG, "ipv4NotificationReceived invalid "
                    + (value != null ? value.length : 0));
            return;
        }
        int bytesParsed = 0;
        int numSsids = ch.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, bytesParsed++);
        if (VDBG)
            Log.v(TAG, "ipv4NotificationReceived " + numSsids + " " + value.length);

        if (numSsids != 0 && value.length < 19) {
            Log.w(TAG, "ipv4NotificationReceived invalid " + value.length);
            return;
        }
        List<RemoteMacAddress> listmac = new ArrayList<RemoteMacAddress>();
        if (numSsids == 0) {
            mEb.reset();
            mGattResCallback.onIpv4NotificationReceived(listmac);
            return;
        }
        mEb.reset();
        try {
            for (int s = 0; s < numSsids; s++) {
                int ssidLength = ch.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8,
                        bytesParsed++);
                String ssid = new String(value, bytesParsed, ssidLength);
                // TODO: should match with SSID of connected AP
                bytesParsed += ssidLength;
                MacAddress bssid = MacAddress
                        .fromBytes(Arrays.copyOfRange(value, bytesParsed, (bytesParsed + 6)));
                bytesParsed += 6;
                int numIpv4 = ch.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8,
                        bytesParsed++);
                if (VDBG)
                    Log.v(TAG, "ipv4NotificationReceived numIpv4 " + numIpv4);
                if (numIpv4 == 0) {
                    return;
                }
                for (int i = 0; i < numIpv4; i++) {
                    int status = ch.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8,
                            bytesParsed++);
                    Inet4Address ip4Address = null;
                    try {
                        ip4Address = (Inet4Address) Inet4Address.getByAddress(
                                Arrays.copyOfRange(value, bytesParsed, (bytesParsed + 4)));
                    } catch (UnknownHostException e) {
                        Log.e(TAG, "ipv4NotificationReceived ip4Address " + e);
                    }
                    bytesParsed += 4;
                    int location = ch.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32,
                            bytesParsed);
                    bytesParsed += 4;
                    int role = ch.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8,
                            bytesParsed++);
                    MacAddress macAddress = MacAddress
                            .fromBytes(Arrays.copyOfRange(value, bytesParsed, (bytesParsed + 6)));
                    bytesParsed += 6;
                    if (VDBG)
                        Log.v(TAG, "ipv4NotificationReceived " + macAddress + " ssid " + ssid
                                + " bssid " + bssid + " ip4Address " + ip4Address + " location "
                                + location + " role " + role + " status " + status);
                    RemoteMacAddress remoteMac = mEb.getMac(macAddress);
                    if (remoteMac == null) {
                        mEb.addMac(mDevice, macAddress, location);
                        remoteMac = mEb.getMac(macAddress);
                        Log.w(TAG, "ipv4NotificationReceived added " + macAddress);
                    }
                    remoteMac.init(ssid, bssid, ip4Address, location, role, status);
                    if (VDBG)
                        Log.v(TAG, "ipv4NotificationReceived remoteMac " + remoteMac);
                    listmac.add(remoteMac);
                }
            }
            // Send IP address notification to ConnectionStateMachine
            mGattResCallback.onIpv4NotificationReceived(listmac);
        } catch (Exception e) {
            e.printStackTrace();
            Log.w(TAG, "ipv4NotificationReceived error " + e.toString());
        }
    }

    private void ipv6NotificationReceived(BluetoothGattCharacteristic ch) {
        byte value[] = ch.getValue();
        if (value == null || value.length < 1) {
            Log.w(TAG, "ipv6NotificationReceived invalid length " + (value != null ? value.length : 0));
            return;
        }
        int bytesParsed = 0;
        int numSsids = ch.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, bytesParsed++);
        if (VDBG)
            Log.v(TAG, "ipv6NotificationReceived " + numSsids);
        if (bytesParsed == 1) {  // ToDo: Remove this check
            if (DBG)
                Log.d(TAG, "Ignore IpV6");
            return;
        }
        mEb.reset();
        List<RemoteMacAddress> listmac = new ArrayList<RemoteMacAddress>();
        if (numSsids == 0) {
            mGattResCallback.onIpv6NotificationReceived(listmac);
            return;
        }

        for (int s = 0; s < numSsids; s++) {
            int ssidLength = ch.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8,
                    bytesParsed++);
            String ssid = new String(value, bytesParsed, ssidLength);
            // TODO: should match with SSID of connected AP
            bytesParsed += ssidLength;
            MacAddress bssid = MacAddress
                    .fromBytes(Arrays.copyOfRange(value, bytesParsed, (bytesParsed + 6)));
            bytesParsed += 6;
            int numIpv6 = ch.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, bytesParsed++);
            for (int i = 0; i < numIpv6; i++) {
                int status = ch.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8,
                        bytesParsed++);
                Inet6Address ip6Address = null;
                try {
                    ip6Address = (Inet6Address) Inet6Address.getByAddress(
                            Arrays.copyOfRange(value, bytesParsed, (bytesParsed + 16)));
                } catch (UnknownHostException e) {
                    Log.e(TAG, "ipv6NotificationReceived " + e);
                }
                bytesParsed += 16;
                int location = ch.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32,
                        bytesParsed);
                bytesParsed += 4;
                int role = ch.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8,
                        bytesParsed++);
                MacAddress macAddress = MacAddress
                        .fromBytes(Arrays.copyOfRange(value, bytesParsed, (bytesParsed + 6)));
                bytesParsed += 6;
                if (VDBG)
                    Log.v(TAG,
                            "ipv6NotificationReceived " + macAddress + " ssid " + ssid + " bssid "
                                    + bssid + " ip6Address " + ip6Address + " location "
                                    + location + " role " + role + " status " + status);
                RemoteMacAddress remoteMac = mEb.getMac(macAddress);
                if (remoteMac == null) {
                    Log.w(TAG,
                            "ipv6NotificationReceived macAddress " + macAddress + " not found ");
                    continue;
                }
                remoteMac.init(ssid, bssid, ip6Address, location, role, status);
                listmac.add(remoteMac);
            }
            // Send IPv6 call back to ConnectionStateMachine
            mGattResCallback.onIpv6NotificationReceived(listmac);
        }
    }

    private void updateMdnsUuid(BluetoothGattCharacteristic ch) {
        byte value[] = ch.getValue();
        if (value == null || value.length != 16) {
            Log.w(TAG, "updateMdnsUuid invalid length " + (value != null ? value.length : 0));
            return;
        }
        UUID uuid = mDataParser.getMdnsSrvuUID(value);
        mEb.setUuidRemote(uuid);
        mXpanUtils.cacheMdnsUuid(mDevice, uuid.toString());
    }

    private void updateL2capPort(BluetoothGattCharacteristic ch) {
        byte value[] = ch.getValue();
        if (value == null || value.length != 2) {
            Log.w(TAG, "updateL2capPort invalid length " + (value != null ? value.length : 0));
            return;
        }
        mPortL2capTcp = ch.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
        if (mPortL2capTcp > 0 && mPortUdpAudio > 0) {
            mGattResCallback.onPortNumbersReceived(mPortL2capTcp, mPortUdpAudio, mPortUdpReports);
        } else {
            if (DBG)
                Log.d(TAG, "updateL2capPort ignore " + mPortL2capTcp + " " + mPortUdpAudio);
        }
    }

    private void updateUdpPort(BluetoothGattCharacteristic ch) {
        byte value[] = ch.getValue();
        if (value == null || value.length !=2) {
            Log.w(TAG, "updateUdpPort invalid length " + (value != null ? value.length : 0));
            return;
        }
        mPortUdpAudio = ch.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
        mPortUdpReports = ch.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
        if (mPortL2capTcp > 0 && mPortUdpAudio > 0) {
            mGattResCallback.onPortNumbersReceived(mPortL2capTcp, mPortUdpAudio, mPortUdpReports);
        } else {
            if (DBG)
                Log.w(TAG, "updateUdpPort " + mPortL2capTcp + " " + mPortUdpReports);
        }
    }

    private void updateRequestedSapPowerState(BluetoothGattCharacteristic ch) {
        byte value[] = ch.getValue();
        if (value == null || value.length != 1) {
            Log.w(TAG, "updateRequestedSapPowerState invalid "
                    + ((value == null) ? null : value.length));
            return;
        }
        int state = ch.getIntValue(
                BluetoothGattCharacteristic.FORMAT_UINT8, 0);

        if (DBG)
            Log.w(TAG, "updateRequestedSapPowerState " + state);

            mGattResCallback.onRequestedSapPowerState(state);
    }

    private void updateBearerPreference(BluetoothGattCharacteristic ch) {
        byte value[] = ch.getValue();
        if (value == null || (value.length != 1 /* Todo Remove Me */ && value.length != 2)) {
            if (DBG)
                Log.w(TAG, "updateBearerPreference Invalid "
                        + ((value == null) ? null : value.length));
            return;
        }
        int bearer = ch.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        int urgency = 0;
        // ToDo : Remove Below check
        if (value.length == 2) {
          urgency = ch.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1);
          if (VDBG)
                Log.v(TAG, "updateBearerPreference urgency " + urgency);
        }
        if (DBG)
            Log.d(TAG, "updateBearerPreference  " + bearer + " " + urgency);

        if (mGattResCallback != null && bearer >= XpanConstants.BEARER_DEFAULT) {
            mGattResCallback.onReqBearerPreference(Requestor.EB, bearer, urgency);
        } else {
            if (DBG) Log.w(TAG, "updateBearerPreference " + XpanConstants.getBearerString(bearer));
        }
    }

    private void updateBearerPreferenceRead(BluetoothGattCharacteristic ch) {
        byte value[] = ch.getValue();
        if (value == null || (value.length != 1 /* Remove me */ && value.length !=2)) {
            Log.w(TAG, "updateBearerPreferenceRead Invalid "
                    + ((value == null) ? value : value.length));
            return;
        }
        mBearerPreference = ch.getIntValue(
                BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        if (DBG)
            Log.d(TAG, "updateBearerPreferenceRead " + mBearerPreference);
    }

    private void updateClearToSend(BluetoothGattCharacteristic ch) {
        byte value[] = ch.getValue();
        if (value == null || value.length != 1) {
            Log.w(TAG, "updateClearToSend Invalid " + ((value == null) ? null : value.length));
            return;
        }
        int ctsReq = ch.getIntValue(
                BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        if (ctsReq != XpanConstants.CTS_NOT_READY
                && ctsReq != XpanConstants.CTS_READY) {
            if (DBG)
                Log.w(TAG, "updateClearToSend ignore " + ctsReq);
            return;
        }
        mGattResCallback.onReqClearToSend(ctsReq);
    }

    private void updateVBCPeriodicity(BluetoothGattCharacteristic ch) {
        if (ch.getValue().length == 0) {
            Log.w(TAG, "updateVBCPeriodicity No data found");
            return;
        }
        int periodicity = ch
                .getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);

        if (mPeriodicity != periodicity) {
            mPeriodicity = periodicity;
            mService.updateVBCPeriodicity(mPeriodicity, mDevice);
        } else {
            if (DBG)
                Log.w(TAG, "updateVBCPeriodicity ignore " + periodicity + " " + mPeriodicity);
        }
    }

    private void updateNumDevicesPresent(BluetoothGattCharacteristic ch) {
        if (ch.getValue().length == 0) {
            Log.w(TAG, "updateNumDevicesPresent No data found");
            return;
        }
        int numDevicesPresent = ch
                .getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        if (DBG)
            Log.d(TAG, "updateNumDevicesPresent " + numDevicesPresent);
    }

    private void updateAudioBearerSwitchRes(BluetoothGattCharacteristic ch) {
        if (ch == null || ch.getValue() == null) {
            if (VDBG)
                Log.v(TAG, "AUDIO_BEARER_SWITCH_RESPONSE invalid " + ch);
            return;
        }
        int bearerswitchresponse = ch
                .getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        mGattResCallback.onAudioBearerSwitchResponse(mBearer, bearerswitchresponse);
    }

    private void updateServerFeatures(BluetoothGattCharacteristic ch) {
        byte data[] = ch.getValue();
        boolean r4CodecSupport = false;
        if (data == null || data.length == 0) {
            Log.w(TAG, "updateServerFeatures No data found");
            r4CodecSupport = false;
        } else {

            ByteBuffer bf = mDataParser.getByteBuffer(data);
            long serverFeatures = bf.getLong();

            if (DBG)
                Log.d(TAG, "updateServerFeatures " + serverFeatures);

            if ((serverFeatures & XpanConstants.APTX_ADAPTIVE_CODEC_R4)
                    == XpanConstants.APTX_ADAPTIVE_CODEC_R4) {
                r4CodecSupport = true;
            }
        }
        if (!r4CodecSupport) {
            Log.e(TAG, "APTX_ADAPTIVE_CODEC_R4 not-support ");
            disconnect();
        }
    }

    private boolean parseServices () {
        boolean xpanSupport = false;
        if (mBluetoothGatt == null) {
            Log.e(TAG, "BluetoothGatt instance is invalid");
            return xpanSupport;
        }

        List<BluetoothGattService> mGattServices = mBluetoothGatt.getServices();

        if (mGattServices == null) {
            Log.w(TAG, "Gatt Services not found");
            return xpanSupport;
        }

        for (BluetoothGattService service: mGattServices) {
            if (XpanUuid.XPAN_SERVICE.equals(service.getUuid())) {
                // store Gatt Characteristics
                mXpanDevice.getXpanGattCharacteristics(service);
                xpanSupport = true;
                break;
            }
        }
        return xpanSupport;
    }

    private void configureXpanGattChar () {
        charQueue.clear();
        addinQueue(mXpanDevice.ipv4Address);
        addinQueue(mXpanDevice.ipv6Address);
        addinQueue(mXpanDevice.l2capTcpPort);
        addinQueue(mXpanDevice.udpPort);
        addinQueue(mXpanDevice.serverFeatures);
        addinQueue(mXpanDevice.audioBearerSwitchResponse);
        addinQueue(mXpanDevice.numDevicesPresent);
        addinQueue(mXpanDevice.voicebackchannelperiodicity);
        addinQueue(mXpanDevice.bearerPreference);
        addinQueue(mXpanDevice.clearToSend);
        addinQueue(mXpanDevice.macAddress);
        addinQueue(mXpanDevice.twtStatus);
        addinQueue(mXpanDevice.sapConnStatus);
        addinQueue(mXpanDevice.reqWifiScanResults);
        addinQueue(mXpanDevice.requestedSapPowerState);
        //addinQueue(mXpanDevice.wifiChannelSwitchReq);

        BluetoothGattCharacteristic ch = charQueue.poll();
        if (ch != null) {
            mWritePending = true;
            registerCharForNotification(ch);
        }
    }

    // Queue do not accept null values
    private void addinQueue(BluetoothGattCharacteristic characterStic) {
        if (characterStic != null) {
            charQueue.add(characterStic);
        } else {
            if (DBG)
                Log.w(TAG, "addinQueue null");
        }
    }

    private void writeCharacteristic(BluetoothGattCharacteristic ch, byte data[]) {
        if (ch != null) {
            GattCharData charData = new GattCharData(ch, data);
            mWriteQueue.add(charData);
        }
        if (mWritePending) {
            String uuid = XpanUuid.uuidToName(ch);
            if (!XpanUuid.TWT_CONFIGURATION.equals(ch.getUuid())
                    && (data != null && data.length > 0)) {
                uuid = XpanConstants.getStrfromOpCode(data[0]);
            }
            if (DBG)
                Log.d(TAG, "writeCharacteristic in queue " + uuid);
            return;
        }
        GattCharData charData = mWriteQueue.poll();
        BluetoothGattCharacteristic characterStic = charData.getCh();
        characterStic.setValue(charData.getData());
        if (VDBG)
            Log.v(TAG, "writeCharacteristic " + XpanUuid.uuidToName(characterStic));
        if (mBluetoothGatt.writeCharacteristic(characterStic)) {
            mWritePending = true;
        } else {
            if (VDBG)
                Log.v(TAG, "writeCharacteristic failed " + XpanUuid.uuidToName(characterStic));
        }
    }

    private void registerCharForNotification (BluetoothGattCharacteristic ch) {
        String uuidName = XpanUuid.uuidToName(ch);
        if (DBG) Log.d(TAG, "registerCharForNotification " + uuidName );

        if (mBluetoothGatt.setCharacteristicNotification(ch, true)) {
            BluetoothGattDescriptor descriptor =
                    ch.getDescriptor(XpanUuid.CLIENT_CHARACTERISTIC_CONFIG);
            if (descriptor != null) {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                boolean result = mBluetoothGatt.writeDescriptor(descriptor);
                if (!result) {
                    Log.w(TAG, "Write Descriptor for " + uuidName);
                }
            }
        }
    }

    private boolean readCharacteristicValue (BluetoothGattCharacteristic ch) {
        if (DBG) Log.d(TAG, "readCharacteristicValue " + XpanUuid.uuidToName(ch));

        if (mBluetoothGatt == null) {
            Log.w(TAG, "readCharacteristicValue - Invalid GattInstance");
            return false;
        }

        return mBluetoothGatt.readCharacteristic(ch);
    }

    private class XpanDevice {

        private BluetoothGattCharacteristic ipv4Address, ipv6Address, l2capTcpPort, udpPort,
                mDnsUuid, twtConfig, xpanControlPoint, macAddress, requestedSapPowerState,
                serverFeatures, audioBearerSwitchResponse, numDevicesPresent,
                voicebackchannelperiodicity, bearerPreference, clearToSend, reqWifiScanResults,
                wifiChannelSwitchReq, twtStatus, sapConnStatus;

        private void getXpanGattCharacteristics(BluetoothGattService service) {
            if (VDBG) {
                Log.v(TAG, "getXpanGattCharacteristics");
            }

            ipv4Address = service.getCharacteristic(XpanUuid.IPV4_ADDRESS);
            ipv6Address = service.getCharacteristic(XpanUuid.IPV6_ADDRESS);
            l2capTcpPort = service.getCharacteristic(XpanUuid.L2CAP_TCP_PORT);
            udpPort = service.getCharacteristic(XpanUuid.UDP_PORT);
            mDnsUuid = service.getCharacteristic(XpanUuid.MDNS_SERVICE_UUID);
            twtConfig = service.getCharacteristic(XpanUuid.TWT_CONFIGURATION);
            xpanControlPoint = service.getCharacteristic(XpanUuid.XPAN_CONTROL_POINT);
            macAddress = service.getCharacteristic(XpanUuid.MAC_ADDRESS);
            requestedSapPowerState = service.getCharacteristic(XpanUuid.REQ_SAP_POWER_STATE);
            serverFeatures = service.getCharacteristic(XpanUuid.SERVER_FEATURES);
            audioBearerSwitchResponse = service.getCharacteristic(
                XpanUuid.AUDIO_BEARER_SWITCH_RESPONSE);
            numDevicesPresent = service.getCharacteristic(XpanUuid.NUM_DEVICES_PRESENT);
            voicebackchannelperiodicity  = service.getCharacteristic(
                    XpanUuid.VOICE_BACK_CHANNEL_PERIODICITY);
            bearerPreference = service.getCharacteristic(XpanUuid.BEARER_PREFERENCE);
            clearToSend = service.getCharacteristic(XpanUuid.CLEAR_TO_SEND);
            reqWifiScanResults = service.getCharacteristic(XpanUuid.REQ_WIFI_SCAN_RESULTS);
            wifiChannelSwitchReq = service.getCharacteristic(XpanUuid.WIFI_CHANNEL_SWITCH_REQUEST);
            twtStatus  = service.getCharacteristic(XpanUuid.TWT_STATUS);
            sapConnStatus = service.getCharacteristic(XpanUuid.SAP_CONN_STATUS);
            boolean isInValid = (ipv4Address == null) || (ipv6Address == null)
                    || (l2capTcpPort == null) || (udpPort == null) || (mDnsUuid == null)
                    || (twtConfig == null) || (xpanControlPoint == null) || (macAddress == null)
                    || (requestedSapPowerState == null) || (serverFeatures == null)
                    || (audioBearerSwitchResponse == null) || (numDevicesPresent == null)
                    || (voicebackchannelperiodicity == null) || (bearerPreference == null)
                    || (clearToSend == null) || (reqWifiScanResults == null)
                    || (wifiChannelSwitchReq == null) || (twtStatus == null)
                    || (sapConnStatus == null);
            if (isInValid) {
                Log.w(TAG, "getXpanGattCharacteristics "
                        + " \nipv4Address " + ipv4Address
                        + " \nipv6Address " + ipv6Address
                        + " \nl2capTcpPort " + l2capTcpPort
                        + " \nudpPort " +  udpPort
                        + " \nmDnsUuid " + mDnsUuid
                        + " \ntwtConfig " + twtConfig
                        + "\nxpanControlPoint " + xpanControlPoint
                        + "\nmacAddress " + macAddress
                        + "\nrequestedSapPowerState " + requestedSapPowerState
                        + "\nserverFeatures " + serverFeatures
                        + "\naudioBearerSwitchResponse "
                        + audioBearerSwitchResponse + "\nnumDevicesPresent "
                        + numDevicesPresent
                        + "\nvoicebackchannelperiodicity " + voicebackchannelperiodicity
                        + "\nbearerPreference " + bearerPreference
                        + "\ncleartoSend " + clearToSend
						+ "\nreqWifiScanResults " + reqWifiScanResults
                        + " \nwifiChannelSwitchReq " + wifiChannelSwitchReq
                        + " \ntwtStatus " + twtStatus
                        + " \nsapConnStatus " + sapConnStatus);
            }
        }

     }

    private class XpanTask {
        private int taskId;

        private static final int GATT_CHARS_READ_OP = 1;
        private static final int CONNECT_SSID_OP = 2;

        private byte[] params;

        public XpanTask(int taskId) {
            this.taskId = taskId;
        }

        private void setTaskParams(byte[] params) {
            this.params = params;
        }
    }

    public void SetPortUpdatePending(boolean pending) {
        mIsPortUpdatePending = pending;
    }

    public boolean GetPortUpdatePending() {
        return mIsPortUpdatePending;
    }

    XpanEb getEb() {
        return mEb;
    }

    private void sendEtherType() {
        sendMessage(SEND_ETHER_TYPE, mWifiUtils.getEtherType());
    }

    void sendBearerPreferenceResponse(int bearer, int response) {
        if (DBG)
            Log.d(TAG, "sendBearerPreferenceResponse " + bearer + " " + response);
        if (response == XpanConstants.BEARER_ACCEPTED && bearer != XpanConstants.BEARER_P2P_PREP) {
            mBearerPreference = bearer;
        }
        sendMessage(BEARER_PREFERENCE_RESPONSE, response);
    }

    private void sendMacAddress() {
        MacAddress macAddress = mWifiUtils.getBssid();
        sendMessage(SEND_MAC_ADDRESS, macAddress);
    }

    private void sendClientFeatures() {
        sendMessage(SEND_CLIENT_FEATURES);
    }

    void sendAudioBearerSwitchReq(int bearer) {
        sendMessage(AUDIO_BEARER_SWITCH_REQ, bearer);
    }

    int getState() {
        IState state = getCurrentState();
        if (state.equals(mDisconnected)) return DISCONNECTED;
        if (state.equals(mConnecting)) return CONNECTING;
        if (state.equals(mConnected)) return CONNECTED;
        if (state.equals(mDisconnecting)) return DISCONNECTING;
        return DISCONNECTED;
    }

    void disconnect() {
        if (DBG)
            Log.d(TAG, "disconnect");
        if (getState() != DISCONNECTED) {
            Message msg = obtainMessage(DISCONNECT);
            sendMessage(msg);
        }
    }

    @Override
    protected void log(String msg) {
        super.log(msg);
    }

    BluetoothDevice getDevice() {
        return mDevice;
    }

    synchronized boolean isConnected() {
        return getState() == CONNECTED;
    }

    int getVbcPeriodicity() {
        return mPeriodicity;
    }

    private void closeGatt() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
    }

    void sendData(int opCode, byte data[]) {
            sendMessage(opCode, data);
    }

    void registerGattResponseCallback(GattResponseCallback callback) {
        mGattResCallback = callback;
    }

    public interface GattResponseCallback {
        public void onAudioBearerSwitchResponse(int bearer, int status);

        public void onIpv4NotificationReceived(List<RemoteMacAddress> listMac);

        public void onIpv6NotificationReceived(List<RemoteMacAddress> listMac);

        public void onRequestedSapPowerState(int powerstate);

        public void onReqBearerPreference(Requestor requester, int bearer, int urgency);

        public void onReqClearToSend(int ctsReq);

        public void onReqWifiScanResult();

        public void onPortCofigEnableSapOnAp();

        public void onReqSendPortUpdates();

        void onWifiScanResultUpdated();

        void onL2capTcpPortUpdated();

        void onUdpPortUpdated();

        void onPortNumbersReceived(int portL2cap, int portUdpAudio, int portUdpAudioReports);

        void onBearerPreferenceResponseSent(int bearer);

        void onPowerStateResponseUpdated();

        void onWifiChannelSwitchReq(int channel);

        void onSendPortNumbers();

        void onGattConnnected();

        void onGattReadStatus(boolean completed, int bearer);

        void onSapClientsStateChange(int loc, OpCode type);
    }

    boolean isConnSsidSent() {
        return mConnectSsidSent;
    }

    private void connectXpan() {
        writePending();
        mService.connect(mDevice);
        mGattResCallback.onGattReadStatus(true, mBearerPreference);
        if (mApSm.getUdpPortAudio() != 0 && mGattResCallback != null && mIsPortUpdatePending) {
            mGattResCallback.onSendPortNumbers();
        } else {
            if (mGattResCallback == null)
                Log.d(TAG, "connectXpan mGatt null");
        }
    }

    private class GattCharData implements Serializable {

        BluetoothGattCharacteristic ch;
        byte data[];

        GattCharData(BluetoothGattCharacteristic ch, byte[] data) {
            this.ch = ch;
            this.data = data;
        }

        BluetoothGattCharacteristic getCh() {
            return ch;
        }

        byte[] getData() {
            return data;
        }
    }

    boolean isConfigured() {
        return mConfig;
    }

    // Process pending queued characteristics
    private void writePending() {
        if (mWriteQueue.size() > 0) {
            writeCharacteristic(null, null);
        }
    }
}
