/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.bluetooth.xpan;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.net.MacAddress;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.text.TextUtils;
import android.util.Log;
import vendor.qti.bluetooth.xpan.XpanNsdHelper.NsdData;
import vendor.qti.hardware.bluetooth.xpanprovider.IXpanProvider;
import vendor.qti.hardware.bluetooth.xpanprovider.IXpanProviderCallback;

/**
 * XpanProviderClient binds with vendor XpanProvider Service for XPAN use cases.
 * This class calls required API's in XpanManager and receives callbacks.
 * @hide
 */

public class XpanProviderClient {
    private static final String TAG = "XpanProvider";
    private static boolean VDBG = XpanUtils.VDBG;
    private static boolean DBG = XpanUtils.DBG;

    private XpanUtils mXpanUtils;

    private static final int OPCODE_INDEX = 0;
    private static final int BD_ADDR_LEN = 6;

    private Handler mHandler;

    /* Opcodes for commands sent from Profile to Xpan Manager */
    enum OpCode {
        UPDATE_BONDED_XPAN_DEVICES,
        UPDATE_TRANSPORT_STATUS,
        ENABLE_SAP_ACS,
        UPDATE_PREPARED_AUDIO_BEARER,
        UPDATE_TWT_SESSION_PARAMS,
        UPDATE_BEARER_SWITCHED,
        UPDATE_HOST_PARAMS,
        UPDATE_SAP_LOWPOWER,
        UPDATE_SAP_STATE,
        UPDATE_VBC_PERIODICITY,
        CREATE_SAP_INTERFACE,
        UPDATE_WIFI_SCAN_STARTED,
        UPDATE_BEARER_PREFERENCE,
        UPDATE_REMOTE_CTS,
        UPDATE_AIRPLANE_MODE_ENABLED,
        UPDATE_WIFI_TRANSPORT_PREF,
        CONNECT_LE_LINK,
        UPDATE_AP_DETAILS_LOCAL,
        UPDATE_AP_DETAILS_REMOTE,
        GET_PORT_NUMBERS,
        UPDATE_MDNS_STATUS,
        UPDATE_DEVICE_BOND_STATE,
        UPDATE_MDNS_RECONRD,
        UPDATE_POWER_STATE_REQ,
        UPDATE_CONNECTED_SAP_CLIENTS,
        UPDATE_WIFI_CHANNEL_SWITCH_REQ,
        GET_WIFI_DRIVER_STATUS,
        DISCONNECT_SAP_CLIENTS,
        TERMINATE_TWT,
    }

    /* Opcodes for Events received from Xpan Manager.*/
    enum OpCodeCb {
        CB_XPAN_DEVICE_FOUND,
        CB_USECASE_UPDATE,
        CB_ACS_UPDATE,
        CB_PREPARE_AUDIO_BEARER,
        CB_TWT_SESSION,
        CB_SAP_LOW_POWER_MODE_UPDATE,
        CB_BEARER_SWITCH_INDICATION,
        CB_SAP_INTERFACE,
        CB_BEARER_PREFERENCE_RES,
        CB_SSR_WIFI,
        CB_WIFI_TRANSPORT_PREF,
        CB_MDNS_QUERY,
        CB_MDNS_REGISTER_UNREGISTER,
        CB_START_FILTERED_SCAN,
        CB_ESTABLISHED_LE_LINK,
        CB_PORT_NUMBERS,
        CB_CURRENT_TRANSPORT,
        CB_ROLE_SWITCH_INDICATION,
        CB_POWER_STATE_RES,
        CB_CHANNEL_SWITCH_STARTED,
        CB_VEN_DISABLED,
        CB_WIFI_DRIVER_STATUS,
        CB_COUNTRY_CODE_UPDATE,
        CB_SAP_INTERFACE_STATE,
        CB_BEARER_SWITCH_FAILED,
    }

    private IXpanProvider mXpanProvider;

    /* Xpan Provider client stub for callback API */
    private IXpanProviderCallback mXpanCallbacks =
            new IXpanProviderCallback.Stub() {
        /**
         * Events received from XpanProvider Service.
         */
        @Override
        public void xpanEventReceivedCb(byte[] event) {
            parseXpanEvents(event);
        }

        @Override
        public final String getInterfaceHash() {
            return IXpanProviderCallback.HASH;
        }

        @Override
        public final int getInterfaceVersion() {
            return IXpanProviderCallback.VERSION;
        }
    };

    XpanProviderClient(Handler handler, XpanUtils xpanUtils) {
        mHandler = handler;
        mXpanUtils = xpanUtils;
        if (!init()) {
            updateInitFail();
        }
    }

    void close() {
        if (VDBG)
            Log.v(TAG, "close");
        if (mHandler != null) {
            mHandler = null;
        }
    }

    private boolean init() {
        boolean isDeclared =
            ServiceManager.isDeclared(IXpanProvider.DESCRIPTOR + "/default");

        if (DBG) Log.d(TAG, "init isDeclared " + isDeclared);

        if (!isDeclared) {
            Log.e(TAG, "XpanProviderService is not declared");
            return false;
        }

        IBinder binder = Binder.allowBlocking(ServiceManager.waitForDeclaredService(
                IXpanProvider.DESCRIPTOR + "/default"));
        if (binder == null) {
            Log.e(TAG, "Failed to obtain XpanProviderService");
            return false;
        }

        mXpanProvider = IXpanProvider.Stub.asInterface(binder);
        try {
            binder.linkToDeath(deathRecipient, 0);
            mXpanProvider.registerXpanCallbacks(mXpanCallbacks);
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to register DeathRecipient for " + mXpanProvider
                      + " or callback registration failed : " + e);
            return false;
        }

        if (DBG) Log.d(TAG, "XpanProvider binding successful");
        return true;
    }

    /**
     * DeathReceipient handler to binder service disconnection.
     */
    IBinder.DeathRecipient deathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            if (DBG) {
                Log.d(TAG, "binderDied");
            }

            mXpanProvider = null;
            /* Bind to vendor service again since profile service wont restart
             in this case */
            // TODO : Should this be run in separate thread.
            init();
        }
    };

    /**
     * Sends Xpan Command to XPan Provider Service. Xpan Manager handles
     * these commands or sends it QHCI.
     */
    private boolean sendXpanCommand(byte[] cmd) {

        if (mXpanProvider == null) {
            Log.e(TAG, "Xpan Command " + cmd[OPCODE_INDEX]
                    + " cant be sent. Service not bound.");
            return false;
        }

        try {
            mXpanProvider.sendXpanCommand(cmd);
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to send Xpan Command : " + e);
            return false;
        }

        return true;
    }

    /**
     * Parses the Xpan callback from XpanManager to appropriate Xpan Event.
     */
    private void parseXpanEvents(byte[] event) {
        if (event == null || event.length == 0 || mHandler == null) {
            Log.e(TAG, "Invalid callback event " + event + " " + mHandler);
            return;
        }

        int code = (int)event[OPCODE_INDEX];
        if (code >= OpCodeCb.values().length) {
            Log.e(TAG, "parseXpanEvents unknown " + code);
            return;
        }
        OpCodeCb opcode = OpCodeCb.values()[code];
        byte[] data = null;
        if (event.length > 1) {
            data = Arrays.copyOfRange(event, 1, event.length);
        }

        switch(opcode) {

        case CB_VEN_DISABLED:
            onVenDisabled(data);
            break;

            case CB_XPAN_DEVICE_FOUND:
                onXpanDeviceFound(data);
                break;

            case CB_USECASE_UPDATE:
                onUsecaseUpdate(data);
                break;

            case CB_ACS_UPDATE:
                onAcsUpdate(data);
                break;

            case CB_PREPARE_AUDIO_BEARER:
                onPrepareAudioBearer(data);
                break;

            case CB_TWT_SESSION:
                onTwtSession(data);
                break;

            case CB_SAP_LOW_POWER_MODE_UPDATE :
                onBeaconIntervalsReceived(data);
                break;

            case CB_BEARER_SWITCH_INDICATION:
                onBearerSwitchIndication(data);
                break;

            case CB_SAP_INTERFACE:
                onUpdateSapInterface(data);
                break;

            case CB_BEARER_PREFERENCE_RES:
                onBearerPreferenceRes(data);
                break;

            case CB_SSR_WIFI :
                onSsrWifi(data);
                break;

            case CB_WIFI_TRANSPORT_PREF:
                onWifiTransportPref(data);
                break;
            
            case CB_MDNS_QUERY:
                onMdnsQuery(data);
                break;

            case CB_MDNS_REGISTER_UNREGISTER:
                onMdnsRegisterUnregister(data);
                break;

            case CB_START_FILTERED_SCAN:
                onStartFilterScan(data);
                break;

            case CB_ESTABLISHED_LE_LINK:
                onLeLinkEstablished(data);
                break;

            case CB_PORT_NUMBERS:
                onPortNumbers(data);
                break;

            case CB_CURRENT_TRANSPORT:
                onCurrentTransport(data);
                break;

            case CB_ROLE_SWITCH_INDICATION:
                onRoleSwitchInd(data);
                break;

            case CB_POWER_STATE_RES:
                onPowerStateRes(data);
                break;

            case CB_CHANNEL_SWITCH_STARTED:
                onSapChannelSwitchStarted(data);
                break;

            case CB_WIFI_DRIVER_STATUS:
                onWifiDriverStatus(data);
                break;

            case CB_COUNTRY_CODE_UPDATE:
                onCountryCodeUpdate(data);
                break;

            case CB_SAP_INTERFACE_STATE:
                onSapInterfaceUpdate();
                break;

            case CB_BEARER_SWITCH_FAILED:
                onBearerSwitchFailed(data);
                break;

            default:
                Log.w(TAG, "Unhandled Event " + code);
        }
    }

    /* Xpan Command API's */
    /**
     * To updates Bonded Xpan Devices to QHCI
     */
    boolean updateXpanBondedDevices (List<BluetoothDevice> devices) {
        if (VDBG) {
            Log.v(TAG, "updateXpanBondedDevices " + devices);
        }
        if (devices == null || devices.size() == 0) {
            return false;
        }

        // Total bytes = opcode byte (1) + numOfDevices (1) + numOfDevices * 6
        ByteBuffer buffer = getByteBuffer(2 + (devices.size() * 6));
        buffer.put((byte)(0xFF & parse(OpCode.UPDATE_BONDED_XPAN_DEVICES)));
        buffer.put((byte)(0xFF & devices.size()));
        for (BluetoothDevice device: devices) {
            buffer.put(getAddressBytes(device.getAddress()));
        }

        return sendXpanCommand(buffer.array());
    }

    /**
     * This updates that a given transport is enabled and requirements to
     * use the transport are met.
     */
    boolean updateTransport(BluetoothDevice device,
            boolean isEnabled, int reason) {
        if (VDBG) {
            Log.v(TAG, "updateTransport " + device
                    + " " + isEnabled
                    + " " + XpanConstants.getTransPortStr(reason));
        }

        /* totalBytes = opcode byte (1) + address(6) + transport(1)
                        + isEnabled(1) + reason (1) */
        final int totalBytes = 10;

        ByteBuffer buffer = getByteBuffer(totalBytes);
        buffer.put((byte)(0xFF & parse(OpCode.UPDATE_TRANSPORT_STATUS)));
        buffer.put(getAddressBytes(device.getAddress()));
        buffer.put((byte)(0xFF & XpanConstants.BEARER_P2P));
        buffer.put((byte)(0xFF & (isEnabled ? 1 : 0)));
        buffer.put((byte)(0xFF & reason));

        return sendXpanCommand(buffer.array());
    }

    /**
     * To enable SAP ACS algorithm.
     */
    boolean enableSapAcs (int[] freqList, int freqListSize) {
        if (VDBG) {
            Log.v(TAG, "enableSapAcs " + freqListSize);
        }

        /* totalBytes = opcode byte (1) + freqListSize(1) + (4*list_size)  */
        final int totalBytes = 1 + 4 + (4 * freqList.length);

        ByteBuffer buffer = getByteBuffer(totalBytes);
        buffer.put((byte)(0xFF & parse(OpCode.ENABLE_SAP_ACS)));
        buffer.put(intToByteArray(freqListSize));
        for (int i = 0; i < freqListSize; i++) {
            buffer.put(intToByteArray(freqList[i]));
        }

        return sendXpanCommand(buffer.array());
    }

    /**
     * To update that the bearer has been switched to mentioned bearer.
     */
    boolean updateBearerSwitched (BluetoothDevice device, int bearer,
            int status) {
        if (VDBG) {
            Log.v(TAG, "updateBearerSwitched  " + device + " bearer "
                    + bearer + " status = " + status);
        }

        /* totalBytes = opcode byte (1) + address(6) + bearer(1) */
        final int totalBytes = 9;

        ByteBuffer buffer = getByteBuffer(totalBytes);
        buffer.put((byte)(0xFF & parse(OpCode.UPDATE_BEARER_SWITCHED)));
        buffer.put(getAddressBytes(device.getAddress()));
        buffer.put((byte)(0xFF & bearer));
        buffer.put((byte)(0xFF & status));

        return sendXpanCommand(buffer.array());
    }

    /**
     * To updates TWT Session related params to Xpan Manager.
     */
    boolean updateTwtSessionParams(int rightOffset, int vbc, List<String> macaddr,
        List<Integer> audioLocation, List<Boolean> established, List<Integer> si,
        List<Integer> sp) {

        int numDevices = macaddr.size();

        /*
         * TWT size (24 ) - rightoffset (4) + vbc (1) + macAddr (6) + location(4) + established (1) + si (4) + sp
         * (4) totalBytes = opcode byte (1) + numDevices + (TWT_size * numDevices );
         */

        final int totalBytes =  7 + (numDevices * 19);

        ByteBuffer buffer = getByteBuffer(totalBytes);
        buffer.put((byte) (0xFF & parse(OpCode.UPDATE_TWT_SESSION_PARAMS)));
        buffer.put(intToByteArray(rightOffset));
        buffer.put((byte) (0xFF & vbc));
        buffer.put((byte) (0xFF & numDevices));
        if (VDBG) {
            Log.v(TAG, "updateTwtSessionParams " + rightOffset +
                    " vbc " + vbc + " numDevices " + numDevices);
        }
        for (int i = 0; i < numDevices; i++) {
            if (VDBG) {
                Log.v(TAG, "updateTwtSessionParams " + macaddr.get(i)
                        + ", location " + audioLocation.get(i) + " isEstablished "
                        + established.get(i) + " si " + si.get(i) + " sp " + sp.get(i));
            }

            buffer.put(getAddressBytes(macaddr.get(i).toUpperCase()));
            buffer.put(intToByteArray( audioLocation.get(i)));
            buffer.put((byte) (0xFF & ( established.get(i) ? 1 : 0)));
            buffer.put(intToByteArray(si.get(i)));
            buffer.put(intToByteArray(sp.get(i)));
        }
        return sendXpanCommand(buffer.array());
    }

    /**
     * To indicate that the bearer is prepared after prepare bearer call.
     */
    boolean updatePrepareAudioBearer(BluetoothDevice device, int bearer, int status) {
        if (VDBG) {
            Log.v(TAG, "updatePrepareAudioBearer  " + device + " "
                    + bearer + " " + status);
        }

        /* totalBytes = opcode byte (1) + address(6) + bearer(1) + status(1)*/
        final int totalBytes = 9;

        ByteBuffer buffer = getByteBuffer(totalBytes);
        buffer.put((byte)(0xFF & parse(OpCode.UPDATE_PREPARED_AUDIO_BEARER)));
        buffer.put(getAddressBytes(device.getAddress()));
        buffer.put((byte)(0xFF & bearer));
        buffer.put((byte)(0xFF & status));

        return sendXpanCommand(buffer.array());
    }

    /**
     * To updates SoftAp Configuration MacAddress and Ether Type to Xpan Manager.
     */
    boolean updateHostParams(String macaddress, int ethertype) {
        if (VDBG) {
            Log.v(TAG, "updateSoftApMacAddress " + macaddress
                    + "  "+ ethertype);
        }

        /* totalBytes = opcode byte (1) + address(6) + ether type (2) */
        final int totalBytes = 9;

        ByteBuffer buffer = getByteBuffer(totalBytes);
        buffer.put((byte)(0xFF & parse(OpCode.UPDATE_HOST_PARAMS)));
        buffer.put(getAddressBytes(macaddress.toUpperCase()));
        buffer.put(shortToByteArray(ethertype));

        return sendXpanCommand(buffer.array());
    }

    /**
     * To update SAP Low Power mode.
     */
    boolean enableSapLowPower(int dialogId, boolean mode) {

        if (VDBG) {
            Log.v(TAG, "enableSapLowpower " + dialogId + "  " + mode);
        }

        /* totalBytes = opcode byte (1) + mode (1) + dialogid (1) */
        final int totalBytes = 3;

        ByteBuffer buffer = getByteBuffer(totalBytes);
        buffer.put((byte)(0xFF & parse(OpCode.UPDATE_SAP_LOWPOWER)));
        buffer.put((byte)(0xFF & dialogId));
        buffer.put((byte)(0xFF & (mode ? 1 : 0)));

        return sendXpanCommand(buffer.array());
    }

    /**
     * To update SAP State to Xpan Manager.
     */
    boolean updateSapState(int state) {
        if (VDBG) {
            Log.v(TAG, "updateSapState " + state);
        }

        /* totalBytes = opcode byte (1) + state (1)*/
        final int totalBytes = 2;

        ByteBuffer buffer = getByteBuffer(totalBytes);
        buffer.put((byte)(0xFF & parse(OpCode.UPDATE_SAP_STATE)));
        buffer.put((byte)(0xFF & state));
        return sendXpanCommand(buffer.array());
    }

    /**
     * This API updates VNC periodicity to Xpan Manager
     */
    boolean updateVBCPeriodicity(int periodicity, BluetoothDevice device) {

        if (VDBG) {
            Log.v(TAG, "updateVBCPeriodicity " + device + " " + periodicity);
        }

        /* totalBytes = opcode byte (1) + periodicity(1)  + device (6) */
        final int totalBytes = 8;

        ByteBuffer buffer = getByteBuffer(totalBytes);
        buffer.put((byte)(0xFF & parse(OpCode.UPDATE_VBC_PERIODICITY)));
        buffer.put((byte)(0xFF & periodicity));
        buffer.put(getAddressBytes(device.getAddress()));

        return sendXpanCommand(buffer.array());
    }

    /**
     * This API Creates SAP Interface
     */
    boolean createSapInterface(boolean isCreate) {
        if (VDBG) {
            Log.v(TAG, "createSapInterface " + ((isCreate) ? "Create" : "Delete"));
        }
        /* totalBytes = opcode byte (1)*/
        final int totalBytes = 2;
        ByteBuffer buffer = getByteBuffer(totalBytes);
        buffer.put((byte)(0xFF & parse(OpCode.CREATE_SAP_INTERFACE)));
        buffer.put((byte)(0xFF & ((isCreate) ?
                XpanConstants.SAP_IF_CREATE : XpanConstants.SAP_IF_DELETE)));
        return sendXpanCommand(buffer.array());
    }

    /**
     * This This API updates Remote Bearer preference to XM
     */
    boolean updateBearerPreferenceReq(BluetoothDevice device, int requester, int bearer,
            int reason, int urgency) {
        if (VDBG) {
            Log.v(TAG, "updateBearerPreferenceReq " + device
                    + " bearer " + XpanConstants.getBearerString(bearer) + " " + requester
                    + "  " + reason + " " + urgency);
        }
        /* totalBytes = opcode (1) + address(6) + requester(1) + bearer(1) + reason(1) + urgency (1)*/
        final int totalBytes = 11;
        ByteBuffer buffer = getByteBuffer(totalBytes);
        buffer.put((byte)(0xFF & parse(OpCode.UPDATE_BEARER_PREFERENCE)));
        buffer.put(getAddressBytes(device.getAddress()));
        buffer.put((byte)(0xFF & requester));
        buffer.put((byte)(0xFF & bearer));
        buffer.put((byte)(0xFF & reason));
        buffer.put((byte)(0xFF & ((urgency == 1) ? 1 : 0)));
        return sendXpanCommand(buffer.array());
    }

    /**
     * This This API updates Request clear to send to XM
     */
    boolean updateClearToSendReq(BluetoothDevice device, int ctsReq) {
        if (VDBG) {
            Log.v(TAG, "updateClearToSendReq " + device + " " + ctsReq);
        }
        /* totalBytes = opcode (1) + address(6) + bearer(1) */
        final int totalBytes = 8;
        ByteBuffer buffer = getByteBuffer(totalBytes);
        buffer.put((byte)(0xFF & parse(OpCode.UPDATE_REMOTE_CTS)));
        buffer.put(getAddressBytes(device.getAddress()));
        buffer.put((byte)(0xFF & ctsReq));
        return sendXpanCommand(buffer.array());
    }

    /**
     * This This API Send Airplane mode indication to XM
     */
    boolean updateAirPlaneModeChange(int state) {
        if (VDBG) {
            Log.v(TAG, "onAirPlaneModeChange " + state);
        }
        /* totalBytes = opcode (1) + state(1) */
        final int totalBytes = 2;
        ByteBuffer buffer = getByteBuffer(totalBytes);
        buffer.put((byte) (0xFF & parse(OpCode.UPDATE_AIRPLANE_MODE_ENABLED)));
        buffer.put((byte) (0xFF & state));
        return sendXpanCommand(buffer.array());
    }

    boolean connectLeLink(BluetoothDevice device) {
        if (VDBG) {
            Log.v(TAG, "connectLeLink " + device);
        }
        /* totalBytes = opcode (1) + address(6) */
        final int totalBytes = 7;
        ByteBuffer buffer = getByteBuffer(totalBytes);
        buffer.put((byte) (0xFF & parse(OpCode.CONNECT_LE_LINK)));
        buffer.put(getAddressBytes(device.getAddress()));
        return sendXpanCommand(buffer.array());
    }

    boolean updateApDetailsLocal(ApDetails apDetails) {
        if (apDetails == null) {
            Log.w(TAG, "updateApDetailsLocal ignore " + apDetails);
            return false;
        }
        /*
         * totalBytes = opcode(1) + bssid (6) + macAddress(6) + iptype(1) + ipAddr_size +
         * UUID (16) + freq (4)
         */
        int totalBytes = 1 + 6 + 6 + 1 + 16 + 4;
        int iptype = -1;
        int ipSize = -1;
        if (apDetails.isValidIpv4()) {
            iptype = XpanConstants.TYPE_IPV4;
            ipSize = XpanConstants.SIZE_IPV4;
        } else if (apDetails.isValidIpv6()) {
            iptype = XpanConstants.TYPE_IPV6;
            ipSize = XpanConstants.SIZE_IPV6;
        }
        boolean disconnected = false;
        if (iptype == -1) {
            disconnected = true;
            iptype = XpanConstants.TYPE_IPV4;
            ipSize = XpanConstants.SIZE_IPV4;
        }
        totalBytes += ipSize;
        if (VDBG)
            Log.v(TAG, "updateApDetailsLocal " + totalBytes + " " + apDetails);

        ByteBuffer buffer = getByteBuffer(totalBytes);
        buffer.put((byte) (0xFF & parse(OpCode.UPDATE_AP_DETAILS_LOCAL)));
        buffer.put(getAddressBytes(apDetails.getBssid().toString()));
        buffer.put(getAddressBytes(apDetails.getMacAddress().toString()));
        buffer.put((byte) (0xFF & iptype));
        if (disconnected) {
            buffer.put(XpanConstants.DEFAULT_IP);
        } else {
            buffer.put(apDetails.getIpv4Address().getAddress());
        }
        buffer.put(getUuidInBytes(mXpanUtils.getUuidLocal()));
        buffer.put(intToByteArray(apDetails.getPrimaryFrequency()));
        return sendXpanCommand(buffer.array());
    }

    boolean updateApDetailsRemote(XpanEb eb) {
        try {
        List<RemoteMacAddress> listRemotemac = eb.getRemoteMacList();
        int size = 0;
        if (VDBG) {
            Log.v(TAG, "updateApDetailsRemote " + eb);
        }
        if (eb.getDevice() == null) {
            return false;
        }
        /*
         * totalBytes = opcode (1) + BtAddr(6) + Mdns (1) + Mdns uuid (16) + l2capTcpPort (2)
         * + udpPortAudio(2)
         * + udpPortReports(2) + numdevices (1) + bssid (6) +  iptye (1) + ip (4 or
         * 16)  + Audiolocation (1) + role (1) +  mac (6)
         */
        int totalBytes = 31;
        for (RemoteMacAddress remMac : listRemotemac) {
            if (remMac.getStatus() == XpanConstants.IP_ADDRESS_OBTAINED) {
                size++;
                if (remMac.isIpv6()) {
                    totalBytes += 12;
                }
            }
        }
        totalBytes += + (size * 19);
        BluetoothDevice device = eb.getDevice();
        ByteBuffer buffer = getByteBuffer(totalBytes);
        buffer.put((byte) (0xFF & parse(OpCode.UPDATE_AP_DETAILS_REMOTE)));
        buffer.put(getAddressBytes(device.getAddress()));
        buffer.put((byte) (0xFF & XpanConstants.FALSE)); // Remove Later
        buffer.put(getUuidInBytes(eb.getUuidRemote()));
        buffer.put(shortToByteArray(eb.getPortL2capTcp()));
        buffer.put(shortToByteArray(eb.getPortUdpAudio()));
        buffer.put(shortToByteArray(eb.getPortUdpReports()));
        buffer.put((byte) (0XFF & size));
        if (size != 0) {
            for (RemoteMacAddress remMac : listRemotemac) {
                if (remMac.getStatus() != XpanConstants.IP_ADDRESS_OBTAINED) {
                    continue;
                }
                buffer.put(getAddressBytes(remMac.getBssid().toString()));
                buffer.put((byte) (0XFF & ((remMac.isIpv6()) ? XpanConstants.TYPE_IPV6
                        : XpanConstants.TYPE_IPV4)));
                buffer.put(remMac.getIpAddress().getAddress());
                buffer.put((byte) (0xFF & remMac.getAudioLocation()));
                buffer.put((byte) (0xFF & remMac.getRole()));
                buffer.put(getAddressBytes(remMac.getMacAddress().toString()));
            }
        }
        return sendXpanCommand(buffer.array());
        } catch (Exception e) {
            e.printStackTrace();
            Log.w(TAG, e.toString());
            return false;
        }
    }

    boolean updateApDetailsRemote(NsdData data) {

        if (VDBG) {
            Log.v(TAG,
                    "updateApDetailsRemote " + data);
        }

        String ip = data.getInetAddress().toString();
        byte ipAddress[] = XpanConstants.MAC_DEFAULT.getBytes();
        int tcpPort = data.getPort();
        int udpPort = 0;
        String macval = XpanConstants.MAC_DEFAULT;
        int totalBytes = 31;
        totalBytes += + (1 * 19);
        int mdns  = XpanConstants.TRUE;
        if (VDBG)
            Log.v(TAG,
                    "updateApDetailsRemote " + totalBytes + " ip " + ip + " port " + tcpPort);
        if (!TextUtils.isEmpty(ip)) {
            ipAddress = data.getInetAddress().getAddress();
        }
        ByteBuffer buffer = getByteBuffer(totalBytes);
        buffer.put((byte) (0xFF & parse(OpCode.UPDATE_AP_DETAILS_REMOTE)));
        buffer.put(getAddressBytes(data.getDevice().getAddress()));
        buffer.put((byte) (0xFF & mdns));
        buffer.put(getUuidInBytes(data.getMdnsUuid()));
        buffer.put(shortToByteArray(tcpPort));
        buffer.put(shortToByteArray(udpPort));
        buffer.put(shortToByteArray(tcpPort));
        buffer.put((byte) (0XFF & 1));
        MacAddress mac = MacAddress.fromString(macval);
        buffer.put(getAddressBytes(mac.toString()));
        buffer.put((byte) (0XFF & XpanConstants.TYPE_IPV4));
        buffer.put(ipAddress);
        buffer.put((byte) (0xFF & 0));
        buffer.put((byte) (0xFF & 0));
        buffer.put(getAddressBytes(mac.toString()));
        return sendXpanCommand(buffer.array());
    }

    boolean getPortDetails() {
        if (VDBG) {
            Log.v(TAG, "getPortDetails");
        }
        /* totalBytes = opcode(1) */
        final int totalBytes = 1;
        ByteBuffer buffer = getByteBuffer(totalBytes);
        buffer.put((byte) (0xFF & parse(OpCode.GET_PORT_NUMBERS)));
        return sendXpanCommand(buffer.array());
    }

    boolean updateMdnsDiscoveryStatus(BluetoothDevice device, UUID uuidRemote, int state,
            int status) {
        if (VDBG) {
            Log.v(TAG, "updateMdnsDiscoveryStatus " + device + "  " + uuidRemote + " state "
                    + state + "  status " + status);
        }
        /*
         * totalBytes = opcode (1) + address(6) + uuidRemote(16) + state(1) + status(1)
         */
        final int totalBytes = 25;
        ByteBuffer buffer = getByteBuffer(totalBytes);
        buffer.put((byte) (0xFF & parse(OpCode.UPDATE_MDNS_STATUS)));
        buffer.put(getAddressBytes(device.getAddress()));
        buffer.put(getUuidInBytes(uuidRemote));
        buffer.put((byte) (0xFF & state));
        buffer.put((byte) (0xFF & status));
        return sendXpanCommand(buffer.array());
    }

    /**
     * Update Bluetooth device Bond state to QHCI
     */
    boolean updateBondState(BluetoothDevice device, int state) {
        if (VDBG) {
            Log.v(TAG, "updateBondState " + device + " " + state);
        }
        if (device == null) {
            return false;
        }

        // Total bytes = opcode byte (1) + BluetoothDevice (6) + BondState (1)
        final int totalBytes = 8;
        ByteBuffer buffer = getByteBuffer(totalBytes);
        buffer.put((byte) (0xFF & parse(OpCode.UPDATE_DEVICE_BOND_STATE)));
        buffer.put(getAddressBytes(device.getAddress()));
        buffer.put((byte) (0xFF & state));
        return sendXpanCommand(buffer.array());
    }

    /**
     * This This API send response for bearer preference request from wifi module
     */
    boolean updateWifiTransportPreference(int bearer, int reponse) {
        if (VDBG) {
            Log.v(TAG, "updateWifiTransportPreference " + bearer + "  " + reponse);
        }
        /* totalBytes = opcode (1) + bearer(1) + requestor(1) */
        final int totalBytes = 3;
        ByteBuffer buffer = getByteBuffer(totalBytes);
        buffer.put((byte)(0xFF & parse(OpCode.UPDATE_WIFI_TRANSPORT_PREF)));
        buffer.put((byte)(0xFF & bearer));
        buffer.put((byte)(0xFF & reponse));
        return sendXpanCommand(buffer.array());
    }

    /**
     * This This API send Power state request received by EB
     */
    boolean updatePowerStateRequest(BluetoothDevice device, int duration) {
        if (VDBG) {
            Log.v(TAG, "updatePowerStateRequest " + device + "  " + duration);
        }
        /* totalBytes = opcode (1) + device(6) + duration in m seconds (4) */
        final int totalBytes = 11;
        ByteBuffer buffer = getByteBuffer(totalBytes);
        buffer.put((byte)(0xFF & parse(OpCode.UPDATE_POWER_STATE_REQ)));
        buffer.put(getAddressBytes(device.getAddress()));
        buffer.put((intToByteArray(duration)));
        return sendXpanCommand(buffer.array());
    }

    boolean updateConnectedEbDetails(int setId, String left, String right) {
        if (VDBG) {
            Log.v(TAG, "updateConnectedEbDetails " + setId + " " + left + " " + right);
        }
        /* totalBytes = opcode (1) + setid(1) + mac_left (6) + mac_right(6)*/
        final int totalBytes = 1 + 1 + 6 + 6;
        ByteBuffer buffer = getByteBuffer(totalBytes);
        buffer.put((byte) (0xFF & parse(OpCode.UPDATE_CONNECTED_SAP_CLIENTS)));
        buffer.put((byte) (0xFF & setId));
        buffer.put(getAddressBytes(left));
        buffer.put(getAddressBytes(right));
        return sendXpanCommand(buffer.array());
    }

    boolean updateWifiScanStarted(BluetoothDevice device, int state) {
        if (VDBG) {
            Log.v(TAG, "updateWifiScanStarted " + device + " " + state);
        }
        /* totalBytes = opcode (1) + BluetoothDevice(6) + state (1)*/
        final int totalBytes = 1 + 6 + 1;
        ByteBuffer buffer = getByteBuffer(totalBytes);
        buffer.put((byte) (0xFF & parse(OpCode.UPDATE_WIFI_SCAN_STARTED)));
        buffer.put(getAddressBytes(device.getAddress()));
        buffer.put((byte) (0xFF & state));
        return sendXpanCommand(buffer.array());
    }

    boolean updateWifiChannelSwitchReq(BluetoothDevice device, int channel) {
        if (VDBG) {
            Log.v(TAG, "updateWifiChannelSwitchReq " + device + " " + channel);
        }
        /* totalBytes = opcode (1) + BluetoothDevice(6) + state (1) */
        final int totalBytes = 1 + 6 + 1;
        ByteBuffer buffer = getByteBuffer(totalBytes);
        buffer.put((byte) (0xFF & parse(OpCode.UPDATE_WIFI_CHANNEL_SWITCH_REQ)));
        buffer.put(getAddressBytes(device.getAddress()));
        buffer.put(shortToByteArray(channel));
        return sendXpanCommand(buffer.array());
    }

    boolean getWifiDriverStatus() {
        if (VDBG) {
            Log.v(TAG, "getWifiDriverStatus");
        }
        int totalBytes = 1; /* opcode */
        ByteBuffer buffer = getByteBuffer(totalBytes);
        buffer.put((byte) (0xFF & parse(OpCode.GET_WIFI_DRIVER_STATUS)));
        return sendXpanCommand(buffer.array());
    }

    boolean updateSapClientsStateChange(List<MacAddress> listmac, OpCode type) {
        if (VDBG) {
            Log.v(TAG, "updateSapClientsStateChange " + type + " " + listmac);
        }
        if (listmac == null || listmac.size() == 0) {
            return false;
        }
        /* totalBytes = opcode (1) + (MacAddress(6) * sizeListMac) */
        final int totalBytes = 1 + (6 * listmac.size());
        ByteBuffer buffer = getByteBuffer(totalBytes);
        buffer.put((byte) (0xFF & parse(type)));
        for (MacAddress mac : listmac) {
            buffer.put(mac.toByteArray());
        }
        return sendXpanCommand(buffer.array());
    }

    /* Xpan Provider Callbacks */

    /**
     * Callback received if Xpan Manager interface not enabled
     */
    private void onVenDisabled(byte[] data) {
        if (VDBG) {
            Log.v(TAG, "onVenDisabled ");
        }
        updateInitFail();
    }

    /**
     * Callback received when new device is connected and it supports xpan
     */
    private void onXpanDeviceFound(byte[] data) {
        if (data == null || data.length != BD_ADDR_LEN) {
            Log.e(TAG, "onXpanDeviceFound Incorrect data length "
                    + (data != null ? data.length:0));
            return;
        }

        BluetoothDevice device = byteArrayToBluetoothDevice(data);
        if (VDBG) {
            Log.v(TAG, "onXpanDeviceFound " + device);
        }
        if (mHandler != null) {
            mHandler.sendMessage(mHandler
                    .obtainMessage(XpanProfileService.CB_XPAN_DEVICE_FOUND, device));
        }
    }

    /**
     * Callback received when a new use case is started.
     */
    private void onUsecaseUpdate(byte[] data) {
        /* expectedEventLen = bdAddr(6) + useCase(1) */
        int expectedEventLen = 7;
        int bytesParsed = 0;

        if (data == null || data.length != expectedEventLen) {
            Log.e(TAG, "onUsecaseUpdate Incorrect data length "
                    + (data != null ? data.length:0));
            return;
        }

        BluetoothDevice device = byteArrayToBluetoothDevice(
                Arrays.copyOfRange(data, 0, BD_ADDR_LEN));
        bytesParsed += BD_ADDR_LEN;
        int usecase = (int)(0xFF & data[bytesParsed]);
        if (VDBG) {
            Log.v(TAG, "onUsecaseUpdate " + device + " " + usecase);
        }
        if (mHandler != null) {
            mHandler.sendMessage(mHandler
                    .obtainMessage(XpanProfileService.CB_UPDATE_USECASE,
                        usecase,-1, device));
        }

    }

    /**
     * Callback received when Xpan Profile needs to prepare bearer.
     */
    private void onPrepareAudioBearer(byte[] data) {
        /* expectedEventLen = bdAddr(6) + bearer(1) */
        int expectedEventLen = 7;
        int bytesParsed = 0;

        if (data == null || data.length != expectedEventLen) {
            Log.e(TAG, "onPrepareAudioBearer Incorrect data length "
                    + (data != null ? data.length:0));
            return;
        }

        BluetoothDevice device = byteArrayToBluetoothDevice(
                Arrays.copyOfRange(data, 0, BD_ADDR_LEN));
        bytesParsed += BD_ADDR_LEN;
        int bearer = (int)(0xFF & data[bytesParsed]);

        if (VDBG) {
            Log.v(TAG, "onPrepareAudioBearer " + device
                    + " " + XpanConstants.getBearerString(bearer));
        }

        if (mHandler != null) {
            mHandler.sendMessage(mHandler
                    .obtainMessage(XpanProfileService.CB_PREPARE_AUDIO_BEARER,
                        bearer,-1, device));
        }
    }

    /**
     * Callback received when ACS has finshed and operating frequency is determined.
     */
    private void onAcsUpdate(byte[] data) {
        /* expectedEventLen = status(1) + frequency(4) */
        int expectedEventLen = 5;
        int bytesParsed = 0;

        if (data == null || data.length != expectedEventLen) {
            Log.e(TAG, "onAcsUpdate Incorrect data length = "
                    + (data != null ? data.length:0));
            return;
        }

        int status = (int)(0xFF & data[bytesParsed++]);
        int frequency = byteArrayToInt(Arrays.copyOfRange(
                data, bytesParsed, bytesParsed + 4));

        if (VDBG) {
            Log.v(TAG, "onAcsUpdate  ignore" + XpanConstants.getAcsString(status)
                    + " " + frequency);
        }
        if (status != XpanConstants.ACS_COMPLETED && status != XpanConstants.ACS_FAILED) {
            return;
        }
        if (mHandler != null) {
            //mHandler.sendMessage(mHandler.obtainMessage(XpanProfileService.CB_ACS_UPDATE,
               //     frequency, status, null));
        }
    }

    /**
     * Callback received when TWT session is established/Terminated with earbud
     */
    private void onTwtSession(byte[] data) {
        /* expectedEventLen = macAddress(6) + sp(4) + si(4) + eventType(1) */
        int expectedEventLen = 15;
        int bytesParsed = 0;

        if (data == null || data.length != expectedEventLen) {
            Log.e(TAG, "onTwtSession Incorrect data length = "
                    + (data != null ? data.length:0));
            return;
        }

        byte[] macAddress = Arrays.copyOfRange(data, 0, BD_ADDR_LEN);
        bytesParsed += BD_ADDR_LEN;
        int sp = byteArrayToInt(Arrays.copyOfRange(data, bytesParsed, bytesParsed + 4));
        bytesParsed += 4;
        int si = byteArrayToInt(Arrays.copyOfRange(data, bytesParsed, bytesParsed + 4));
        bytesParsed += 4;
        int eventType = (int)(0xFF & data[bytesParsed++]);
        MacAddress mac = MacAddress.fromBytes(macAddress);
        if (VDBG) {
            Log.v(TAG, "onTwtSession " + mac
                    + " sp " + sp + " si " + si
                    + " " + XpanConstants.getTwtTypeString(eventType));
        }
        if (eventType != XpanConstants.TWT_SETUP
                && eventType != XpanConstants.TWT_TERMINATE) {
            Log.w(TAG, "Ignore onTwtSession invalid TWT type");
            return;
        }

        if (mHandler != null) {
            XpanEvent event = new XpanEvent(XpanEvent.TWT_SESSION);
            event.setRemoteMacAddr(mac);
            event.setArg1(sp);
            event.setArg2(si);
            event.setArg3(eventType);
            mHandler.sendMessage(mHandler
                .obtainMessage(XpanProfileService.CB_TWT_SESSION, event));
        }

    }

    /**
     * Bearer switched indication event from Xpan Manager
     */
    private void onBearerSwitchIndication(byte[] data) {
        /* expectedEventLen = bdAddr(6) + bearertype(1) + bearerstatus(1) */
        int expectedEventLen = 8;
        int bytesParsed = 0;

        if (data == null || data.length != expectedEventLen) {
            Log.e(TAG, "onBearerSwitchIndication Incorrect data length "
                    + (data != null ? data.length : 0));
            return;
        }
        BluetoothDevice device = byteArrayToBluetoothDevice(
                Arrays.copyOfRange(data, 0, BD_ADDR_LEN));
        bytesParsed += BD_ADDR_LEN;
        int bearer = (int) (0xFF & data[bytesParsed++]);
        int status = (int) (0xFF & data[bytesParsed++]);
        if (VDBG) {
            Log.v(TAG, "onBearerSwitchIndication " + device
                    + "  " + XpanConstants.getBearerString(bearer)
                + " " + status);
        }
        if (device == null) {
            Log.w(TAG, "onBearerSwitchIndication device null");
            return;
        }
        if (status != XpanConstants.BEARER_STATUS_SUCESS
                && status != XpanConstants.BEARER_STATUS_FAILURE) {
            status = XpanConstants.BEARER_STATUS_FAILURE;
            Log.w(TAG, "onBearerSwitchIndication updated");
        }
        if (mHandler != null) {
            mHandler.sendMessage(mHandler.obtainMessage(
                    XpanProfileService.CB_BEARER_SWITCH_INDICATION,
                    bearer, status, device));
        }
    }

    /**
     * Callback Event from Xpan Manager when low power mode is
     * updated. This event sends required parameters like Beacon
     * interval (Regular / Power Saving) and next TSF.
     */
    private void onBeaconIntervalsReceived(byte[] data) {

        /* expectedEventLen = dialogid(1) +  powersavebi(2) + nexttsf(8) */
        int expectedEventLen = 11;
        int bytesParsed = 0;

        if (data == null || data.length != expectedEventLen) {
            Log.e(TAG, "onBeaconIntervalsReceived Incorrect data length "
                    + (data != null ? data.length : 0));
            return;
        }

        int dialogId = (int)(0xFF & data[bytesParsed++]);
        int powerSaveBi =
                byteArrayToIntShort(Arrays.copyOfRange(data, bytesParsed, bytesParsed + 2));
        bytesParsed += 2;
        long nextTsf = byteArrayToLong(Arrays.copyOfRange(data, bytesParsed, bytesParsed + 8));
        if (VDBG) {
            Log.v(TAG, "onBeaconIntervalsReceived " + dialogId
                    + " powerSaveBi " + powerSaveBi + " nextTsf " + nextTsf);
        }
        if (mHandler != null) {
            XpanEvent event = new XpanEvent(XpanEvent.BEACON_INTERVAL_RECEIVED);
            event.setArg1(dialogId);
            event.setArg2(powerSaveBi);
            event.setTbtTsf(nextTsf);
            mHandler.sendMessage(mHandler
                .obtainMessage(XpanProfileService.CB_SAP_BEACON_INTERVALS, event));
        }

    }

    /**
     * Callback Event from Xpan Manager when SAP interface created
     */
    private void onUpdateSapInterface(byte[] data) {

        /* expectedEventLen = reqState (1) + status(1)*/
        int expectedEventLen = 2;
        int bytesParsed = 0;

        if (data == null || data.length != expectedEventLen) {
            Log.e(TAG, "onUpdateSapInterface Incorrect data length "
                    + (data != null ? data.length : 0));
            return;
        }

        int reqState = (int)(0xFF & data[bytesParsed++]);
        int status = (int)(0xFF & data[bytesParsed++]);
        if (VDBG)
            Log.v(TAG, "onUpdateSapInterface " + XpanConstants.getSapReqStr(reqState)
                    + " " + XpanConstants.getSapStateStr(status));

        if(status != XpanConstants.STATUS_SUCESS && status != XpanConstants.STATUS_FAILURE
                && status != XpanConstants.STATUS_ALREADY_PRESENT) {
            status = XpanConstants.STATUS_FAILURE;
            if (DBG) Log.d(TAG, "onUpdateSapInterface status updated");
        }
        if (mHandler != null) {
            mHandler.sendMessage(mHandler
                    .obtainMessage(XpanProfileService.CB_UPDATE_SAP_INTERFACE, reqState, status));
        }

    }

    /**
     * Callback Event from Xpan Manager for Bearer Preference Response
     */
    private void onBearerPreferenceRes(byte[] data) {

         /* expectedEventLen = bdAddr(6) + requester(1) + bearer(1) + status(1) */
         int expectedEventLen = 9;
         int bytesParsed = 0;
         if (data == null || data.length != expectedEventLen) {
             Log.e(TAG, "onBearerPreferenceRes Incorrect data length "
                     + (data != null ? data.length:0));
             return;
         }
         BluetoothDevice device = byteArrayToBluetoothDevice(
                 Arrays.copyOfRange(data, 0, BD_ADDR_LEN));
         bytesParsed += BD_ADDR_LEN;
         int requester = (int)(0xFF & data[bytesParsed++]);
        int bearer = (int)(0xFF & data[bytesParsed++]);
        int status = (int)(0xFF & data[bytesParsed]);
        if (VDBG) {
            Log.v(TAG, "onBearerPreferenceRes " + device
                    + " " + requester
                    + " " + bearer
                    + " " + XpanConstants.getBearerResString(status));
        }
        if (device == null) {
            Log.w(TAG, "onBearerPreferenceRes device null");
            return;
        }

        if (status != XpanConstants.BEARER_ACCEPTED && status != XpanConstants.BEARER_REJECTED) {
            status = XpanConstants.BEARER_REJECTED;
            if (DBG)
                Log.d(TAG, "onBearerPreferenceRes status updated");
        }
        XpanEvent event = new XpanEvent(XpanEvent.BEARER_PREFERENCE_RES);
        event.setDevice(device);
        event.setArg1(requester);
        event.setArg2(bearer);
        event.setArg3(status);
        if (mHandler != null) {
            mHandler.sendMessage(
                    mHandler.obtainMessage(XpanProfileService.CB_BEARER_PREFERENCE_RES, event));
        }
    }

    /**
     * Callback Event from Xpan Manager for WIFI SSR
     */
    private void onSsrWifi(byte[] data) {

        /* expectedEventLen = state(1) */
        int expectedEventLen = 1;
        if (data == null || data.length != expectedEventLen) {
            Log.e(TAG, "onSsrWifi Incorrect data length " + (data != null ? data.length : 0));
            return;
        }
        int state = (int) (0xFF & data[0]);
        if (VDBG) {
            Log.v(TAG, "onSsrWifi " + XpanConstants.getWifiSsrStr(state));
        }
        if (mHandler != null) {
            mHandler.sendMessage(
                    mHandler.obtainMessage(XpanProfileService.CB_SSR_WIFI, state, -1));
        }
    }

    /**
     * Callback Event from Xpan Manager for Bearer Preference request
     */
    private void onWifiTransportPref(byte[] data) {
         /* expectedEventLen = bearer(1) + reason (1) */
         int expectedEventLen = 2;
         int bytesParsed = 0;
         if (data == null || data.length != expectedEventLen) {
             Log.e(TAG, "onWifiTransportPref Incorrect data length "
                     + (data != null ? data.length:0));
             return;
         }
        int bearer = (int)(0xFF & data[bytesParsed++]);
        int reason = (int)(0xFF & data[bytesParsed]);
        if (VDBG) {
            Log.v(TAG, "onWifiTransportPref " + bearer + " " + reason);
        }
        if (mHandler != null) {
            mHandler.sendMessage(mHandler.obtainMessage(
                    XpanProfileService.CB_WIFI_TRANSPORT_PREF, bearer, reason));
        }
    }

    void onMdnsQuery(byte data[]) {
        /* expectedEventLen = bdAddr(6) + state(1) */
        int expectedEventLen = 7;
        int bytesParsed = 0;
        if (data == null || data.length != expectedEventLen) {
            Log.e(TAG, "onMdnsQuery Incorrect data length " + (data != null ? data.length : 0));
            return;
        }
        BluetoothDevice device = byteArrayToBluetoothDevice(
                Arrays.copyOfRange(data, 0, BD_ADDR_LEN));
        bytesParsed += BD_ADDR_LEN;
        int state = (int) (0xFF & data[bytesParsed]);
        if (VDBG) {
            Log.v(TAG, "onMdnsQuery " + device + " " + XpanConstants.getMdnsStrFromQueryState(state));
        }
        if (device == null) {
            Log.w(TAG, "onMdnsQuery device null");
            return;
        }
        if (mHandler != null) {
            mHandler.sendMessage(
                    mHandler.obtainMessage(XpanProfileService.CB_MDNS_QUERY, state, -1, device));
        }
    }

    void onMdnsRegisterUnregister(byte data[]) {
        /* expectedEventLen = bdAddr(6) + state(1) */
        int expectedEventLen = 7;
        int bytesParsed = 0;
        if (data == null || data.length != expectedEventLen) {
            Log.e(TAG, "onMdnsRegisterUnregister Incorrect data length " + (data != null ? data.length : 0));
            return;
        }
        BluetoothDevice device = byteArrayToBluetoothDevice(
                Arrays.copyOfRange(data, 0, BD_ADDR_LEN));
        bytesParsed += BD_ADDR_LEN;
        int state = (int) (0xFF & data[bytesParsed]);
        if (VDBG) {
            Log.v(TAG, "onMdnsRegisterUnregister " + device + " " + XpanConstants.getMdnsStrFromState(state));
        }
        if (device == null) {
            Log.w(TAG, "onMdnsRegisterUnregister device null");
            return;
        }
        if (mHandler != null) {
            mHandler.sendMessage(
                    mHandler.obtainMessage(XpanProfileService.CB_MDNS_REGISTER_UNREGISTER, state, -1, device));
        }
    }

    void onStartFilterScan(byte data[]) {
        /* expectedEventLen = bdAddr(6) + state(1) */
        int expectedEventLen = 7;
        int bytesParsed = 0;
        if (data == null || data.length != expectedEventLen) {
            Log.e(TAG, "onStartFilterScan Incorrect data length " + (data != null ? data.length : 0));
            return;
        }
        BluetoothDevice device = byteArrayToBluetoothDevice(
                Arrays.copyOfRange(data, 0, BD_ADDR_LEN));
        bytesParsed += BD_ADDR_LEN;
        int status = (int) (0xFF & data[bytesParsed]);
        if (VDBG) {
            Log.v(TAG, "onStartFilterScan " + device + " " + status);
        }
        if (device == null) {
            Log.w(TAG, "onStartFilterScan device null");
            return;
        }
        if (mHandler != null) {
            mHandler.sendMessage(
                    mHandler.obtainMessage(XpanProfileService.CB_START_FILTERED_SCAN, status, -1, device));
        }

    }

    void onLeLinkEstablished(byte data[]) {
        /* expectedEventLen = bdAddr(6) + state(1) */
        int expectedEventLen = 7;
        int bytesParsed = 0;
        if (data == null || data.length != expectedEventLen) {
            Log.e(TAG, "onLeLinkEstablished Incorrect data length "
                    + (data != null ? data.length : 0));
            return;
        }
        BluetoothDevice device = byteArrayToBluetoothDevice(
                Arrays.copyOfRange(data, 0, BD_ADDR_LEN));
        bytesParsed += BD_ADDR_LEN;
        int status = (int) (0xFF & data[bytesParsed]);
        if (VDBG) {
            Log.v(TAG, "onLeLinkEstablished " + device + " status " + status);
        }
        if (device == null) {
            Log.w(TAG, "onLeLinkEstablished device null");
            return;
        }
        if (mHandler != null) {
            mHandler.sendMessage(mHandler.obtainMessage(XpanProfileService.CB_LE_LINK_ESTABLISHED,
                    status, -1, device));
        }

    }

    void onPortNumbers(byte data[]) {
        /* expectedEventLen = l2capTcpPort(2) +  udpPort audio(2) + udp Sync Port(2) */
        int expectedEventLen = 6;
        int bytesParsed = 0;
        if (data == null || data.length != expectedEventLen) {
            Log.e(TAG, "onPortNumbers Incorrect data length " + (data != null ? data.length : 0));
            return;
        }

        int l2capTcpPort = byteArrayToIntShort(Arrays.copyOfRange(data, bytesParsed, bytesParsed+2));
        bytesParsed += 2;
        int udpPortAudio =  byteArrayToIntShort(Arrays.copyOfRange(data, bytesParsed, bytesParsed+2));
        bytesParsed += 2;
        int udpSyncPort =  byteArrayToIntShort(Arrays.copyOfRange(data, bytesParsed, bytesParsed+2));

        if (VDBG) {
            Log.v(TAG, "onPortNumbers " + l2capTcpPort + " " + udpPortAudio +" "+udpSyncPort);
        }
        XpanEvent event = new XpanEvent(-1);
        event.setArg1(l2capTcpPort);
        event.setArg2(udpPortAudio);
        event.setArg3(udpSyncPort);
        if (mHandler != null) {
            mHandler.sendMessage(
                    mHandler.obtainMessage(XpanProfileService.CB_PORT_NUMBERS,event));
        }
    }

    void onCurrentTransport(byte data[]) {
        /* expectedEventLen = transport(1) + Device (6) */
        int expectedEventLen = 7;
        int bytesParsed = 0;
        if (data == null || data.length != expectedEventLen) {
            Log.e(TAG, "onCurrentTransport Incorrect data length "
                    + (data != null ? data.length : 0));
            return;
        }
        BluetoothDevice device = byteArrayToBluetoothDevice(
                Arrays.copyOfRange(data, 0, BD_ADDR_LEN));
        bytesParsed += BD_ADDR_LEN;
        int transport = (int) (0xFF & data[bytesParsed]);
        if (VDBG) {
            Log.v(TAG, "onCurrentTransport " + device + " " + transport);
        }
        if (mHandler != null) {
            mHandler.sendMessage(
                    mHandler.obtainMessage(XpanProfileService.CB_CURRENT_TRANSPORT, transport,
                            -1, device));
        }
    }

    void onRoleSwitchInd(byte data[]) {
        /* expectedEventLen = bdAddr(6) + mac_addr_primary(6) + mac_addr_secondary(6) */
        int expectedEventLen = 18;
        int bytesParsed = 0;
        if (data == null || data.length != expectedEventLen) {
            Log.e(TAG,
                    "onRoleSwitchInd Incorrect data length " + (data != null ? data.length : 0));
            return;
        }
        BluetoothDevice device = byteArrayToBluetoothDevice(
                Arrays.copyOfRange(data, 0, BD_ADDR_LEN));
        bytesParsed += BD_ADDR_LEN;
        byte[] addressPrimary = Arrays.copyOfRange(data, bytesParsed, BD_ADDR_LEN);
        bytesParsed += BD_ADDR_LEN;
        byte[] addressSecondary = Arrays.copyOfRange(data, bytesParsed, BD_ADDR_LEN);
        MacAddress macPrimary = MacAddress.fromBytes(addressPrimary);
        MacAddress macSecondary = MacAddress.fromBytes(addressSecondary);
        if (VDBG) {
            Log.v(TAG, "onRoleSwitchInd " + device + "  " + macPrimary + " "
                    + macSecondary);
        }
        if (mHandler != null) {
            mHandler.sendMessage(
                    mHandler.obtainMessage(XpanProfileService.CB_ROLE_SWITCH_INDICATION, device));
        }
    }

    private void onPowerStateRes(byte[] data) {
        /* expectedEventLen = BluetoothDevice(6) + powerState(1) */
        int expectedEventLen = 7;
        int bytesParsed = 0;
        if (data == null || data.length != expectedEventLen) {
            Log.e(TAG, "onPowerStateRes Incorrect data length "
                    + (data != null ? data.length:0));
            return;
        }
        BluetoothDevice device = byteArrayToBluetoothDevice(
                Arrays.copyOfRange(data, 0, BD_ADDR_LEN));
        bytesParsed += BD_ADDR_LEN;
       int status = (int)(0xFF & data[bytesParsed++]);
       if (VDBG) {
           Log.v(TAG, "onPowerStateRes " + device + " " + status);
       }
       if (status == XpanConstants.AP_AVAILABILITY_COMPLETED) {
           return;
       }
       if (status != XpanConstants.AP_AVAILABILITY_STARTED) {
           status = XpanConstants.POWER_STATE_RES_REJECT;
           if (DBG)
               Log.w(TAG, "onPowerStateRes updated");
       }
       if (mHandler != null) {
           mHandler.sendMessage(
                   mHandler.obtainMessage(XpanProfileService.CB_POWER_STATE_RES,
                           status, -1, device));
       }
    }

    private void onSapChannelSwitchStarted(byte[] data) {
        /* expectedEventLen = freq(2) + tsf(8) + bw(1) */
        int expectedEventLen = 11;
        int bytesParsed = 0;
        if (data == null || data.length != expectedEventLen) {
            Log.e(TAG, "onSapChannelSwitchStarted Incorrect data length "
                   + (data != null ? data.length:0));
           return;
        }
        int freq = byteArrayToIntShort(Arrays.copyOfRange(data, bytesParsed, bytesParsed + 2));
        bytesParsed += 2;
        long tsf = byteArrayToLong(Arrays.copyOfRange(data, bytesParsed, bytesParsed + 8));
        bytesParsed += 8;
        int bw = (int)(0xFF & data[bytesParsed++]);
        if (VDBG) {
            Log.v(TAG, "onSapChannelSwitchStarted " + freq + " " + tsf + " " + bw);
       }
       XpanEvent event = new XpanEvent(XpanEvent.CB_CSS);
       event.setArg1(freq);
       event.setArg2(bw);
       event.setTbtTsf(tsf);
       if (mHandler != null) {
           mHandler.sendMessage(
                   mHandler.obtainMessage(XpanProfileService.CB_CSS, event));
       }
    }

    private void onWifiDriverStatus(byte[] data) {
        int expectedEventLen = 1;
        int bytesParsed = 0;
        if (data == null || data.length != expectedEventLen) {
            Log.e(TAG, "onWifiDriverStatus Incorrect data length "
                   + (data != null ? data.length:0));
           return;
        }
        int status = (int)(0xFF & data[bytesParsed]);
        if (VDBG) {
            Log.v(TAG, "onWifiDriverStatus " + status);
        }
        if (mHandler != null) {
            mHandler.sendMessage(
                    mHandler.obtainMessage(XpanProfileService.CB_WIFI_DRIVER_STATUS, status, -1));
        }
    }

    private void onCountryCodeUpdate(byte[] data) {
        int expectedEventLen = 2;
        int bytesParsed = 0;
        if (data == null || data.length != expectedEventLen) {
            Log.e(TAG, "onCountryCodeUpdate Incorrect data length "
                    + (data != null ? data.length : 0));
            return;
        }
        String country = new String(data);
        if (VDBG) {
            Log.v(TAG, "onCountryCodeUpdate " + country);
        }
        if (mHandler != null) {
            mHandler.sendMessage(
                    mHandler.obtainMessage(XpanProfileService.CB_COUNTRY_CODE_UPDATE, country));
        }
    }

    private void onSapInterfaceUpdate() {
        if (VDBG) {
            Log.v(TAG, "onSapInterfaceUpdate ");
        }
        if (mHandler != null) {
            mHandler.sendMessage(mHandler.obtainMessage(XpanProfileService.CB_SAP_IF_UPDATE));
        }

    }

    private void onBearerSwitchFailed(byte[] data) {
        /* expectedEventLen = BluetoothDevice(6) + Reason(1) + Role(1) */
        int expectedEventLen = 8;
        int bytesParsed = 0;
        if (data == null || data.length != expectedEventLen) {
            Log.e(TAG, "onBearerSwitchFailed Incorrect data length "
                    + (data != null ? data.length : 0));
            return;
        }
        BluetoothDevice device = byteArrayToBluetoothDevice(
                Arrays.copyOfRange(data, 0, BD_ADDR_LEN));
        bytesParsed += BD_ADDR_LEN;
        int reason = (int) (0xFF & data[bytesParsed++]);
        int role = (int) (0xFF & data[bytesParsed++]);
        if (VDBG) {
            Log.v(TAG, "onBearerSwitchFailed " + device + " " + reason + " " + role);
        }
        if (mHandler != null) {
            mHandler.sendMessage(mHandler.obtainMessage(XpanProfileService.CB_BEARER_SWITCH_FAILED,
                    reason, role, device));
        }
    }

    /**
     * Converts a given address string to byte array of 6 bytes.
     * 0th byte corresponds to least significant octet and 5th byte
     * corresponds to most significant octet of Bluetooth address.
     *
     * @param address Remote device bluetooth address in string format
     *        eg. 11:22:33:44:55:66
     * @return Bluetooth address in byte format
     */
    private byte[] getAddressBytes (String address) {
        byte[] addrBytes = new byte[6]; // for 6 byte bd address
        String[] addrTokens = address.split(":");
        int i = 0;

        for (String addrToken : addrTokens) {
            addrBytes[addrBytes.length - i - 1] =
                    (byte)(0xFF & Integer.parseInt(addrToken, 16));
            i++;
        }

        return addrBytes;
    }

    /**
     * Coverts a given address bytes to BluetoothDevice instance.
     *
     * @param address Remote device bluetooth address in byte array
     *        eg. 11:22:33:44:55:66 received as {66, 55, 44, 33, 22, 11}
     * @return BluetoothDevice instance for the address.
     */
    private BluetoothDevice byteArrayToBluetoothDevice (byte[] addrBytes) {
        StringBuilder hexString = new StringBuilder();
        for (int i = addrBytes.length -1; i >= 0; i--) {
            hexString.append(String.format("%02x", addrBytes[i]));
            if (i != 0) {
                hexString.append(":");
            }
        }
        String address = hexString.toString().toUpperCase();
        return mXpanUtils.getBluetoothAdapter().getRemoteDevice(address);
    }

    /**
     * Converts 8 byte array to Long value
     */
    private long byteArrayToLong(byte[] val) {
        return ((long)(val[7] & 0xFF) << 56) |
                ((long)(val[6] & 0xFF) << 48) |
                ((long)(val[5] & 0xFF) << 40) |
                ((long)(val[4] & 0xFF) << 32) |
                ((long)(val[3] & 0xFF) << 24) |
                ((long)(val[2] & 0xFF) << 16) |
                ((long)(val[1] & 0xFF) << 8) |
                ((long)(val[0] & 0xFF));
    }

    /**
     * Converts 4 byte array to integer value
     */
    private int byteArrayToInt(byte[] val) {
        return (((val[3] & 0xFF) << 24) |
                ((val[2] & 0xFF) << 16) |
                ((val[1] & 0xFF) << 8) |
                ((val[0] & 0xFF)));
    }

    /**
     * Converts integer value to 4 byte array.
     */
    private byte[] intToByteArray (int val) {
        return (new byte[] {
                (byte)((val) & 0xFF),
                (byte)((val >> 8) & 0xFF),
                (byte)((val >> 16) & 0xFF),
                (byte)((val >> 24) & 0xFF)
            });
    }

    /**
     * Converts integer value to 2 byte array.
     */
    private byte[] shortToByteArray (int val) {
        return (new byte[] {
                (byte)((val) & 0xFF),
                (byte)((val >> 8) & 0xFF),
            });
    }

    /**
     * Converts 2 byte array to integer value
     */
    private int byteArrayToIntShort(byte[] val) {
        return (((val[1] & 0xFF) << 8) |
                ((val[0] & 0xFF)));
    }

    ByteBuffer getByteBuffer(int size) {
        return ByteBuffer.allocate(size);
    }

    private int parse(OpCode code) {
        return code.ordinal();
    }

    byte[] getUuidInBytes(UUID uuid) {

        int length = 16 ;
        if (DBG)
            Log.d(TAG, "getUuidInBytes " + uuid);
        ByteBuffer buffer = getByteBuffer(length);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return buffer.array();
    }

    private void updateInitFail() {
        if (DBG)
            Log.w(TAG, "updateInitFail");
        mHandler.sendMessage(mHandler.obtainMessage(XpanProfileService.CB_INIT_FAILED,
                XpanConstants.AIDL_FAIL_REASON, -1));
    }
}
