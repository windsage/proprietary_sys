/*****************************************************************
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 ******************************************************************/

package vendor.qti.bluetooth.xpan;

import android.bluetooth.BluetoothDevice;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import android.content.Intent;
import android.net.MacAddress;
import android.net.wifi.ScanResult;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import vendor.qti.bluetooth.xpan.XpanApStateMachine.ApCallback;
import vendor.qti.bluetooth.xpan.XpanNsdHelper.MdnsCallback;
import vendor.qti.bluetooth.xpan.XpanNsdHelper.NsdData;
import vendor.qti.bluetooth.xpan.XpanNsdHelper.NsdMsg;
import vendor.qti.bluetooth.xpan.XpanProviderClient.OpCode;
import vendor.qti.bluetooth.xpan.XpanGattStateMachine.GattResponseCallback;
import vendor.qti.bluetooth.xpan.XpanConstants.Requestor;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Collection;
import android.os.UserHandle;
import android.text.TextUtils;
import java.lang.IllegalArgumentException;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.State;
import com.android.internal.util.IState;
import com.android.internal.util.StateMachine;

import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.lang.String;
import java.lang.StringBuffer;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.lang.Integer;
import java.nio.ByteBuffer;

/**
 * Bluetooth Xpan Connection State Machine
 *
 *                        (Disconnected)<--------------|
 *                           |       ^                 |
 *                   CONNECT |       | DISCONNECTED    |
 *                           v       |                 |
 *                 (Connecting)<--->(Disconnecting)<---|
 *                           |       ^                 |
 *                 CONNECTED |       | DISCONNECT      |
 *                           v       |                 |
 *                          (Connected)                |
 *                               |                     |
 *                               |                     |
 *                               v                     |
 *                        (TwtConfigured) -------------|
 *                               |                     |
 *                               |                     |
 *                               v                     |
 *                       (XpanBearerActive)------------|
 */

final class XpanConnectionStateMachine extends StateMachine {
    private final String TAG;
    private static final boolean DBG = XpanUtils.DBG;
    private static final boolean VDBG = XpanUtils.VDBG;

    /* States */
    static final int DISCONNECTED = 0;
    static final int CONNECTING = 1;
    static final int DISCONNECTING = 2;
    static final int CONNECTED = 3;
    static final int TWT_CONFIGURED = 4;
    static final int BEARER_ACTIVE = 5;
    static final int LOW_POWER = 6;
    static final int AIRPLANE_MODE = 7;
    static final int BEARER_AP = 8;

    /* State machine messages */
    private final int CONNECT_XPAN = 0;
    private final int DISCONNECT_XPAN = 1;
    private final int ENABLE_SAP = 2;
    private final int DISABLE_SAP = 3;
    private final int CONNECT_SSID = 4;
    private final int DISCONNECT_SSID = 5;
    private final int PREPARE_AUDIO_BEARER = 6;
    private final int MOVE_TO_LOW_POWER = 7;
    private final int REQUESTED_SAP_POWER_STATE = 8;
    private final int XPAN_EVENT = 9;
    private final int NEW_BEARER_SWITCH_REQUEST = 10;
    private final int SAP_CONNECTION_TIMEOUT = 11;
    private final int FREQUENCY_UPDATED = 12;
    private final int BEARER_SWITCHED_INDICATION = 13;
    private final int REQ_CLEAR_TO_SEND = 14;
    private final int REQ_BEARER_PREFERENCE = 15;
    private final int AIRPLANEMODE_ENABLED = 16;
    private final int AIRPLANEMODE_DISABLED = 17;
    private final int SSR_WIFI_STARTED = 18;
    private final int SSR_WIFI_COMPLETED = 19;
    private final int CONNECTED_SSID = 20;
    private final int REQ_WIFI_SCAN_RESULTS = 21;
    private final int UPDATE_WIFI_SCAN_RESULTS = 22;
    private final int ON_CSS = 23;
    private final int EB_IPV4_NOTIFICATION = 24;
    private final int EB_IPV6_NOTIFICATION = 25;
    private final int UPDATE_UDP_PORT = 26;
    private final int UPDATE_L2CAP_TCP_PORT = 27;
    private final int UPDATE_BEACON_INTERVAL = 28;
    private final int START_FILTERED_SCAN = 29;
    private final int CB_SCAN_RESULT_BT = 30;
    private final int MDNS_QUERY = 31;
    private final int MDNS_REGISTER_UNREGISTER = 32;
    private final int CB_CURRENT_TRANSPORT = 33;
    private final int CB_LE_LINK = 34;
    private final int UPDATE_UDP_SYNC_PORT = 35;
    private final int UPDATE_USECASE_IDENTIFIER = 36;
    private final int SAP_POWER_STATE_RES = 37;
    private final int RES_MDNS_DATA = 38;
    private final int TWT_EMPTY = 39;
    private final int TWT_CONF = 40;
    private final int DISCONNECT_SSID_AND_GATT = 41;
    private final int WIFI_CHANNEL_SWITCH_REQ = 42;
    private final int ON_SAP_IF_UPDATE = 43;
    private final int UPDATE_SAP_STATE = 44;
    private final int CB_BEARER_SWITCH_FAILED = 45;
    private final int CHANGE_STATE = 46;

    private int mUseCase;
    private final Disconnected mDisconnected;
    private final Connected mConnected;
    private final Connecting mConnecting;
    private final Disconnecting mDisconnecting;
    private final LowPower mLowPower;
    private final TwtConfigured mTwtConfigured;
    private final BearerActive mBearerActive;
    private final AirplaneMode mAirplaneMode;
    private final BearerAP mBearerAp;

    private final BluetoothDevice mDevice;
    private XpanProfileService mService;
    private XpanNsdHelper mNsdHelper;
    private XpanGattStateMachine mGattSm;
    private XpanSapStateMachine mSapSm;
    private XpanApStateMachine mApSm;
    private int mTwtSetupId = 0;
    private boolean mPendingTwtConf = false;
    private XpanUtils mXpanUtils;
    private List<RemoteMacAddress> mListRemoteMac;
    private XpanEb mEb;
    private int mBearer;
    private WifiUtils mWifiUtils;
    private boolean mBearerPrepared = false;
    private XpanProviderClient mProviderClient;
    private int mPlayingState;
    private boolean mIsRequestedLowPower = false, mReqPowerState = false;
    private boolean mIsExitLowPower = false;
    private boolean mIsInterMediateLowPower = false;
    private boolean mIsIdle, mReadComplected;
    private int mVbcPeriodicity;
    private int mRightOffset;
    private LeAudioProxy mLeAudioProxy;
    private boolean mIsAirplaneMode = false;
    private XpanDataParser mDataParser;
    private GattCallback mGattResCallback;
    private ApDetails mApDetails;
    private boolean mSSRMode = false, mReqPowerStatePen = false;
    private int mRemoteReqBearer = XpanConstants.BEARER_DEFAULT;
    private int mTempBearer = -1;
    private boolean mFilterScan = false;
    private int mCurrentTransport = XpanConstants.BEARER_LE;
    private int mSetId = -1, mState;
    private MdnsCallbackListener mMdnsCbListener;
    private boolean mLeLinkEstablished = false;
    private byte mBeacondata[] = {};
    private boolean mSendBeacon = false;
    private final XpanDatabaseManager mDatabaseManager;

    RemoteMacAddress getRemoteMacAddressFromMacAddress(MacAddress macAddress) {
        for (RemoteMacAddress mac : mListRemoteMac) {
            if (mac.getMacAddress().equals(macAddress)) {
                return mac;
            }
        }
        return null; // Should not reach here
    }

    boolean isMacAddressPresent(MacAddress macAddress) {
        boolean found = false;
        for (RemoteMacAddress mac : mListRemoteMac) {
            if (mac.getMacAddress().equals(macAddress)) {
                found = true;
                break;
            }
        }
        return found;
    }

    private class MdnsCallbackListener implements MdnsCallback {

        @Override
        public void onMdnsFound(NsdData data) {
            sendMessage(RES_MDNS_DATA, data);
        }

        @Override
        public void onMdnsStatusChanged(int state, int status) {

        }
    }

    XpanConnectionStateMachine(BluetoothDevice device, XpanProfileService svc,
            Looper looper, XpanGattStateMachine gattSm, XpanSapStateMachine sapSm,
            XpanApStateMachine apSm, String tag) {
        super(tag, looper);

        if (XpanUtils.DBGSM) {
            setDbg(true);
        }

        TAG = tag;
        mDevice = device;
        mService = svc;
        mGattSm = gattSm;
        mSapSm = sapSm;
        mApSm = apSm;
        mApSm.registerApCallBack(mDevice, new ApSmCallBack());
        mApDetails = mApSm.getApDetails();
        mDisconnected = new Disconnected();
        mDisconnecting = new Disconnecting();
        mConnected = new Connected();
        mConnecting = new Connecting();
        mLowPower = new LowPower();
        mTwtConfigured = new TwtConfigured();
        mBearerActive = new BearerActive();
        mAirplaneMode = new AirplaneMode();
        mBearerAp = new BearerAP();
        mXpanUtils = mService.getXpanUtils();
        mLeAudioProxy = mService.getLeAudioProxy();
        addState(mDisconnected);
        addState(mDisconnecting);
        addState(mConnected);
        addState(mConnecting);
        addState(mLowPower);
        addState(mTwtConfigured);
        addState(mBearerActive);
        addState(mAirplaneMode);
        addState(mBearerAp);
        mVbcPeriodicity = mGattSm.getVbcPeriodicity();
        mProviderClient = mService.getProviderClient();
        mDatabaseManager = mService.getDatabaseManager();

        BluetoothDevice playingDevice = mService.getPlayingDevice();
        if (playingDevice != null && playingDevice.equals(device)) {
            mPlayingState = XpanConstants.GROUP_STREAM_STATUS_STREAMING;
        } else {
            mPlayingState = XpanConstants.GROUP_STREAM_STATUS_IDLE;
        }
        mEb = mGattSm.getEb();
        mListRemoteMac = mEb.getRemoteMacList();
        if (VDBG) {
            for (RemoteMacAddress remoteMac : mListRemoteMac) {
                Log.v(TAG, remoteMac.toString());
            }
        }
        mWifiUtils = WifiUtils.getInstance(mService.getApplicationContext());
        mDataParser = new XpanDataParser();
        mGattResCallback = new GattCallback();
        mGattSm.registerGattResponseCallback(mGattResCallback);
        mNsdHelper = mService.getNsdHelper();
        mSetId = mXpanUtils.getSetId(mDevice);
        mMdnsCbListener = new MdnsCallbackListener();
        mNsdHelper.registerMdnsCallback(mDevice, mMdnsCbListener);
        // set initial state
        setInitialState(mDisconnected);
    }

    static XpanConnectionStateMachine make(BluetoothDevice device, XpanProfileService svc,
            Looper looper, XpanGattStateMachine gattSm, XpanSapStateMachine sapSm,
            XpanApStateMachine apSm) {
        String tag = "XpanConn" + "_"
                + device.getAddress().replaceAll(":", "").substring(6);
        if (DBG)
            Log.d(tag, "make " + device);

        XpanConnectionStateMachine xpanConnSm = new XpanConnectionStateMachine(device,
                svc, looper, gattSm, sapSm, apSm, tag);
        xpanConnSm.start();
        return xpanConnSm;
    }

    private void init() {
        mTwtSetupId = 0;
        mPendingTwtConf = false;
        mBearer = XpanConstants.BEARER_DEFAULT;
        mBearerPrepared = false;
        mIsRequestedLowPower = false;
        mIsExitLowPower = true;
        mIsInterMediateLowPower = false;
        mRightOffset = 0;
        mTempBearer = -1;
        mReqPowerState = false;
        mState = DISCONNECTED;
        mUseCase = mService.getCurrentUseCase();
        mSendBeacon = false;
        mReqPowerStatePen = false;
    }

    XpanEb getEb() {
        return mEb;
    }

    class Disconnected extends State {

        @Override
        public void enter() {
            log("Enter Disconnected ");
            init();
            for (RemoteMacAddress addr : mListRemoteMac) {
                updateRemoteMacState(addr.getMacAddress(),
                    XpanConstants.MAC_DSICONNECTED, null);
            }
        }

        @Override
        public boolean processMessage(Message message) {
            log("Disconnected processMessage " + messageToString(message.what));

            switch (message.what) {
            case CONNECT_XPAN:
                if (mCurrentTransport == XpanConstants.BEARER_AP) {
                    transitionTo(mBearerAp);
                } else {
                    /*
                     * Steps: 1. Enable SAP 2. Send Connect SSID Params
                     */
                    transitionTo(mConnecting);
                    // If transport is P2P
                    sendMessage(ENABLE_SAP);
                }
                break;

              case ENABLE_SAP:
                transitionTo(mConnecting);
                mSapSm.enableSap(mDevice, sapCb);
                break;

            case REQUESTED_SAP_POWER_STATE :
                if (mSapSm.isLowPower()) {
                    mReqPowerState = true;
                    mSapSm.setActiveMode();
                } else {
                    updatePowerStateRequest();
                }
                break;

            case SAP_POWER_STATE_RES:
                sendData(SAP_POWER_STATE_RES, message);
                break;

            case REQ_BEARER_PREFERENCE :
                handleBearerPreference(message);
                break;

            case REQ_CLEAR_TO_SEND:
                updateClearToSendReq(message.arg1);
                break;

            case PREPARE_AUDIO_BEARER:
                mBearer = message.arg1;
                mBearerPrepared = false;
                int status = XpanConstants.BEARER_REJECTED;
                if (mBearer == XpanConstants.BEARER_LE) {
                    sendAudioBearerSwitchReq(mBearer);
                    break;
                }
                if (mBearer == XpanConstants.BEARER_NONE) {
                    status = XpanConstants.BEARER_ACCEPTED;
                } else if (mBearer == XpanConstants.BEARER_AP) {
                    processBearerAp();
                    break;
                }
                updatePrepareAudioBearer(mBearer, status);
                break;

            case EB_IPV4_NOTIFICATION:
            case EB_IPV6_NOTIFICATION:
                sendIpNotificationUpdate(message);
                break;

            case START_FILTERED_SCAN:
                startOrStopScan(message.arg1);
                break;

            case MDNS_QUERY:
                updateMdnsQuery(message);
                break;

            case MDNS_REGISTER_UNREGISTER:
                updateMdnsRegisterUnregister(message);
                break;

            case RES_MDNS_DATA:
                updateApDetailsRemote(message);
                break;

            case REQ_WIFI_SCAN_RESULTS:
                startWifiScan();
                break;

            case UPDATE_WIFI_SCAN_RESULTS:
                sendData(UPDATE_WIFI_SCAN_RESULTS, message);
                break;

            case CB_LE_LINK:
                handleLeLinkStatus(message.arg1);
                break;

            case CB_SCAN_RESULT_BT:
                connectLeLink();
                break;

            case CONNECT_SSID :
                sendData(CONNECT_SSID, null);
                break;

            case CONNECTED_SSID :
                sendData(CONNECTED_SSID, null);
                break;

            case UPDATE_USECASE_IDENTIFIER:
                sendData(UPDATE_USECASE_IDENTIFIER, null);
                break;

            case UPDATE_L2CAP_TCP_PORT:
                sendData(UPDATE_L2CAP_TCP_PORT, null);
                break;

            case UPDATE_UDP_PORT:
                sendData(UPDATE_UDP_PORT, null);
                break;

            case UPDATE_UDP_SYNC_PORT:
                sendData(UPDATE_UDP_SYNC_PORT, null);
                break;

            case BEARER_SWITCHED_INDICATION:
                int bearerType = message.arg1;
                int bearerStatus = message.arg2;
                if (bearerType == XpanConstants.BEARER_AP
                        && bearerStatus == XpanConstants.BEARER_STATUS_SUCESS) {
                    transitionTo(mBearerAp);
                }
                break;

            case AIRPLANEMODE_DISABLED:
            case SSR_WIFI_COMPLETED:
                // Enable SAP , do not change to Conn SM state to avoid ssid request
                if (mGattSm.isConnected()) {
                    int sapState = mSapSm.enableSap(mDevice, sapCb);
                    if (DBG)
                        Log.d(TAG, "Disconnected sapState" + sapState);
                    setCodecConfigPreference(LeAudioProxy.CODEC_TYPE_APTX_ADAPTIVE_R4);
                }
                break;

            case AIRPLANEMODE_ENABLED:
                if (mGattSm.isConnected()) {
                    setCodecConfigPreference(LeAudioProxy.CODEC_TYPE_DEFAULT);
                }
                break;

            case FREQUENCY_UPDATED:
                sendData(UPDATE_SAP_STATE, mXpanUtils.getMessage(XpanConstants.SAP_STATE_ON,
                        XpanConstants.SAP_TD_REASON_UNKNOWN));
                break;

            case CHANGE_STATE:
                transitionToState(message.arg1);
                break;

            case XPAN_EVENT:
                XpanEvent event = (XpanEvent) message.obj;
                if (VDBG) Log.v(TAG, "Disconnected XPAN_EVENT " + event);

                switch (event.getEventType()) {
                case XpanEvent.SSID_CONN_STATE_CHANGE:
                    handleSsidConnectionChange(event);
                    break;

                case XpanEvent.BEARER_PREFERENCE_RES:
                    sendBearerPreferenceRes(event);
                    break;

                case XpanEvent.UPDATE_BEACON_INTERVALS:
                    int powerSaveBi = event.getArg1();
                    if (mReqPowerState) {
                        mReqPowerState = false;
                        if (powerSaveBi == XpanConstants.GENERAL_BEACON_INT) {
                            message.arg1 = XpanConstants.POWER_STATE_RES_ACCEPT;
                        } else {
                            message.arg1 = XpanConstants.POWER_STATE_RES_REJECT;
                        }
                        sendData(SAP_POWER_STATE_RES, message);
                    }
                    if (powerSaveBi == XpanConstants.GENERAL_BEACON_INT) {
                        mSendBeacon = true;
                        mBeacondata = mDataParser.getBeaconParameterBytes(event.getTbtTsf(),
                                event.getArg1(), mSapSm.getFrequency());
                    } else  {
                        transitionTo(mLowPower);
                    }
                    break;

                case XpanEvent.BEARER_SWITCH:
                    status = event.getArg2();
                    updatePrepareAudioBearer(event.getArg1(), status);
                    if (status != XpanConstants.AUDIO_BEARER_READY) {
                        if (VDBG)
                            Log.v(TAG, "BEARER_SWITCH_EVT AUDIO_BEARER_FAIL");
                        break;
                    }
                    break;

                default:
                    logDebug("Disconnected XPAN_EVENT not handled " + event);
                    break;
                }
                break;

            case ON_SAP_IF_UPDATE:
                handleSapIfUpdate();
                break;

            default:
                log("Disconnected Unhandled Message " + message.what);
                return NOT_HANDLED;
            }
            return HANDLED;
        }
    }

    /**
     * Tasks in Connecting State for completing connection
     * 1. Enable Required transport
     *    i) For P2P: Enable SAP (Phase 1)
     *    ii) For AP, Enable Wifi and connect to AP (Phase 2)
     * 2. Ask remote to connect to SAP or AP.
     * 3. Wait for remote connected event (wifi Callback)
     *    Move to Connected state once remote connected to SAP.
     */

    class Connecting extends State {

        @Override
        public void enter() {
            mState = CONNECTING;
            int timeout = mXpanUtils.getLowPowerInterval() * 2;
            log("Enter Connecting " + timeout);
            mSapSm.acquireWakeLock();
            sendMessageDelayed(SAP_CONNECTION_TIMEOUT, mXpanUtils.secToMillieSec(timeout));
        }

        @Override
        public void exit() {
            removeMessages(SAP_CONNECTION_TIMEOUT);
            mSapSm.releaseWakeLock();
        }

        @Override
        public boolean processMessage(Message message) {
            log("Connecting processMessage " + messageToString(message.what));

            switch (message.what) {
            case CONNECT_XPAN:
                logDebug("Connecting Xpan Connection already initiated for " + mDevice);
                break;

            case DISCONNECT_XPAN:
                sendData(DISCONNECT_SSID_AND_GATT, null);
                transitionTo(mDisconnected);
                break;

            case ENABLE_SAP:
                int sapState = mSapSm.enableSap(mDevice, sapCb);
                boolean connectionFailed = false;

                /* If SAP is already enabled, trigger ConnectSsid */
                if (sapState == XpanSapStateMachine.SAP_ENABLED) {
                    if (!sendData(CONNECT_SSID, null)) {
                        connectionFailed = true;
                    }

                    /* If SAP can not be enabled */
                } else if (sapState == XpanSapStateMachine.SAP_DISABLED) {
                    connectionFailed = true;
                } else if (sapState == XpanSapStateMachine.SAP_AIRPLANE) {
                    transitionTo(mAirplaneMode);
                }

                if (connectionFailed) {
                    transitionTo(mDisconnected);
                }
                break;

            case CONNECT_SSID:
                sendData(CONNECT_SSID, null);
                break;

            case PREPARE_AUDIO_BEARER:
                mBearer = message.arg1;
                mBearerPrepared = false;
                int bearerStatus = XpanConstants.BEARER_REJECTED;
                if (mBearer == XpanConstants.BEARER_LE) {
                    sendAudioBearerSwitchReq(mBearer);
                    break;
                } else if (mBearer == XpanConstants.BEARER_NONE) {
                    bearerStatus = XpanConstants.BEARER_ACCEPTED;
                }
                updatePrepareAudioBearer(mBearer, bearerStatus);
                break;

            case CONNECTED_SSID :
                sendData(CONNECTED_SSID, null);
                break;

            case REQUESTED_SAP_POWER_STATE :
                updatePowerStateRequest();
                break;

            case SAP_POWER_STATE_RES:
                sendData(SAP_POWER_STATE_RES, message);
                break;

            case SAP_CONNECTION_TIMEOUT:
                sendData(DISCONNECT_SSID, null);
                transitionTo(mDisconnected);
                mSapSm.releaseWakeLock();
                break;

            case REQ_BEARER_PREFERENCE:
                handleBearerPreference(message);
                break;

            case REQ_CLEAR_TO_SEND:
                updateClearToSendReq(message.arg1);
                break;

            case AIRPLANEMODE_ENABLED:
            case SSR_WIFI_STARTED:
                transitionTo(mAirplaneMode);
                break;

            case EB_IPV4_NOTIFICATION:
            case EB_IPV6_NOTIFICATION:
                sendIpNotificationUpdate(message);
                if(mEb.isConnected()) {
                    transitionTo(mConnected);
                }
                break;

            case FREQUENCY_UPDATED:
                sendMessage(CONNECT_SSID);
                break;

            case REQ_WIFI_SCAN_RESULTS:
                startWifiScan();
                break;

            case UPDATE_WIFI_SCAN_RESULTS:
                sendData(UPDATE_WIFI_SCAN_RESULTS, message);
                break;

            case UPDATE_USECASE_IDENTIFIER:
                sendData(UPDATE_USECASE_IDENTIFIER, null);
                break;

            case UPDATE_UDP_SYNC_PORT:
                sendData(UPDATE_UDP_SYNC_PORT, null);
                break;

            case UPDATE_L2CAP_TCP_PORT:
                sendData(UPDATE_L2CAP_TCP_PORT, null);
                break;

            case UPDATE_UDP_PORT:
                sendData(UPDATE_UDP_PORT, null);
                break;

            case CHANGE_STATE:
                transitionToState(message.arg1);
                break;

            case XPAN_EVENT:
                XpanEvent event = (XpanEvent) message.obj;
                if (VDBG) Log.v(TAG, "Connecting XPAN_EVENT " + event);

                switch (event.getEventType()) {
                case XpanEvent.SAP_STATE_CHANGE:
                    int p2pState = event.getArg1();
                    if (p2pState == XpanSapStateMachine.SAP_FREQUENCE_UPDATED) {
                        sendMessage(CONNECT_SSID);
                    } else if (p2pState == XpanSapStateMachine.SAP_DISABLED) {
                        transitionTo(mDisconnected);
                    }
                    break;

                case XpanEvent.SSID_CONN_STATE_CHANGE:
                    handleSsidConnectionChange(event);
                    break;

                case XpanEvent.BEARER_PREFERENCE_RES:
                    sendBearerPreferenceRes(event);
                    break;

                default:
                    logDebug("Connecting XPAN_EVENT not handled message " + event.getEventType());
                    break;
                }
                break;
            default:
                logDebug("Connecting not handled message " + messageToString(message.what));
                return NOT_HANDLED;
            }
            return HANDLED;
        }
    }

    class Connected extends State {

        @Override
        public void enter() {
            mState = CONNECTED;
            startLPTimer();
            log("Enter Connected");
        }

        @Override
        public void exit() {
            removeMessages(MOVE_TO_LOW_POWER);
            mSapSm.releaseWakeLock();
        }

        @Override
        public boolean processMessage(Message message) {
            log("Connected processMessage " + messageToString(message.what));

            switch (message.what) {
            case DISCONNECT_XPAN:
                sendData(DISCONNECT_SSID_AND_GATT, null);
                if (!mEb.isEbConnectedToSap()) {
                    transitionTo(mDisconnected);
                }
                break;

            case ENABLE_SAP:
                mSapSm.enableSap(mDevice, sapCb);
                break;

            case PREPARE_AUDIO_BEARER:
                mBearer = message.arg1;
                mBearerPrepared = false;
                if (mBearer == XpanConstants.BEARER_LE) {
                    sendAudioBearerSwitchReq(mBearer);
                    break;
                } else if (mBearer == XpanConstants.BEARER_NONE) {
                    sendData(TWT_EMPTY, message);
                    break;
                } else if (mBearer == XpanConstants.BEARER_AP) {
                    processBearerAp();
                } else if (mBearer == XpanConstants.BEARER_P2P) {
                    startLPTimer();
                    sendData(TWT_CONF, message);
                }
                break;

            case UPDATE_L2CAP_TCP_PORT:
                sendData(UPDATE_L2CAP_TCP_PORT, null);
                break;

            case UPDATE_UDP_PORT:
                sendData(UPDATE_UDP_PORT, null);
                break;

            case UPDATE_UDP_SYNC_PORT:
                sendData(UPDATE_UDP_SYNC_PORT, null);
                break;

            case MOVE_TO_LOW_POWER:
                mIsRequestedLowPower = true;
                mIsExitLowPower = false;
                mSapSm.enableLowPower(mDevice, XpanConstants.SAP_ENABLE_LOW_POWER);
                mSapSm.releaseWakeLock();
                break;

            case REQUESTED_SAP_POWER_STATE :
                updatePowerStateRequest();
                break;

            case SAP_POWER_STATE_RES:
                sendData(SAP_POWER_STATE_RES, message);
                break;

            case BEARER_SWITCHED_INDICATION:
                int bearer = message.arg1;
                int status = message.arg2;
                if (bearer == XpanConstants.BEARER_P2P
                        && status == XpanConstants.STATUS_FAILURE) {
                    sendData(TWT_EMPTY, message);
                } else if (mBearer == XpanConstants.BEARER_AP
                        && status == XpanConstants.STATUS_SUCESS) {
                    transitionTo(mBearerAp);
                }
                break;

            case REQ_BEARER_PREFERENCE:
                handleBearerPreference(message);
                break;

            case REQ_CLEAR_TO_SEND:
                updateClearToSendReq(message.arg1);
                break;

            case AIRPLANEMODE_ENABLED:
            case SSR_WIFI_STARTED:
                transitionTo(mAirplaneMode);
                break;

            case CONNECT_SSID :
                sendData(CONNECT_SSID, null);
                break;

            case CONNECTED_SSID:
                sendData(CONNECTED_SSID, null);
                break;

            case REQ_WIFI_SCAN_RESULTS:
                startWifiScan();
                break;

            case UPDATE_WIFI_SCAN_RESULTS:
                sendData(UPDATE_WIFI_SCAN_RESULTS, message);
                break;

            case ON_CSS:
                sendData(ON_CSS, message);
                break;

            case EB_IPV4_NOTIFICATION:
            case EB_IPV6_NOTIFICATION:
                sendIpNotificationUpdate(message);
                if(!mEb.isConnected()) {
                    transitionTo(mDisconnected);
                }
                break;

            case CB_CURRENT_TRANSPORT:
                if(mCurrentTransport == XpanConstants.BEARER_AP) {
                    transitionTo(mBearerAp);
                }
                break;

            case UPDATE_USECASE_IDENTIFIER:
                sendData(UPDATE_USECASE_IDENTIFIER, null);
                break;

            case WIFI_CHANNEL_SWITCH_REQ:
                updateWifiChannelSwitchReq(message);
                break;

            case MDNS_QUERY:
                updateMdnsQuery(message);
                break;

            case MDNS_REGISTER_UNREGISTER:
                updateMdnsRegisterUnregister(message);
                break;

            case CB_LE_LINK:
                handleLeLinkStatus(message.arg1);
                break;

            case CB_SCAN_RESULT_BT:
                connectLeLink();
                break;

            case RES_MDNS_DATA:
                Log.v(TAG, "Connected, deferMessage RES_MDNS_DATA");
                deferMessage(message);
                break;

            case CHANGE_STATE:
                transitionToState(message.arg1);
                break;

            case XPAN_EVENT:
                XpanEvent event = (XpanEvent) message.obj;
                if (VDBG) Log.v(TAG, "Connected XPAN_EVENT " + event);

                switch (event.getEventType()) {
                case XpanEvent.SAP_STATE_CHANGE:
                    int p2pState = event.getArg1();
                    if (p2pState == XpanSapStateMachine.SAP_DISABLED) {
                        transitionTo(mDisconnected);
                    }
                    break;

                case XpanEvent.SSID_CONN_STATE_CHANGE:
                    handleSsidConnectionChange(event);
                    break;

                case XpanEvent.TWT_SESSION:
                    int twtType = event.getArg3();
                    if (twtType == XpanConstants.TWT_TERMINATE) {
                        logDebug("Ignore TWT_EVT_TERMINATE in Connected state");
                        event.setArg3(XpanEvent.TWT_TERMINATE_IN_CONNECTED_STATE);
                        updateRemoteMacState(event.getRemoteMacAddr(), -1, event);
                        break;
                    } else if (!mPendingTwtConf) {
                        if (DBG)
                            Log.d(TAG, "Connected ingore Twt Conf");
                        return HANDLED;
                    }
                    updateRemoteMacState(event.getRemoteMacAddr(), -1, event);
                    transitionTo(mTwtConfigured);
                    // send PREPARE_AUDIO_BEARER in TwtConfigured State
                    sendMessage(PREPARE_AUDIO_BEARER, XpanConstants.BEARER_P2P,
                            XpanEvent.TWT_SESSION, mDevice);
                    break;

                case XpanEvent.BEARER_SWITCH:
                    status = event.getArg2();
                    updatePrepareAudioBearer(event.getArg1(), status);
                    if (status != XpanConstants.AUDIO_BEARER_READY) {
                        if (VDBG)
                            Log.v(TAG, "BEARER_SWITCH_EVT AUDIO_BEARER_FAIL");
                        break;
                    }
                    break;

                case XpanEvent.UPDATE_BEACON_INTERVALS:
                    int powerSaveBi = event.getArg1();
                    sendData(UPDATE_BEACON_INTERVAL, message);
                    if (powerSaveBi != XpanConstants.GENERAL_BEACON_INT) {
                        if (mReqPowerStatePen) {
                            mReqPowerStatePen = false;
                            deferMessage(mXpanUtils.getMessageWithWhat(REQUESTED_SAP_POWER_STATE,
                                    REQUESTED_SAP_POWER_STATE));
                        }
                        transitionTo(mLowPower);
                    } else if (mReqPowerStatePen) {
                        mReqPowerStatePen = false;
                        sendMessage(REQUESTED_SAP_POWER_STATE);
                    }
                    break;

                case XpanEvent.BEARER_PREFERENCE_RES:
                    sendBearerPreferenceRes(event);
                    break;

                default:
                    logDebug("Connected XPAN_EVENT not handled message " + event.getEventType());
                    break;
                }
                break;
            default:
                logDebug("Connected not handled message " + messageToString(message.what));
                return NOT_HANDLED;
            }
            return HANDLED;
        }
    }

    class LowPower extends State {

        Message prepareBearerMessage;

        @Override
        public void enter() {
            prepareBearerMessage = null;
            log("Enter LowPower");
            mState = LOW_POWER;
        }

        @Override
        public boolean processMessage(Message message) {
            log("LowPower processMessage " + messageToString(message.what));

            switch (message.what) {
            case DISCONNECT_XPAN:
                sendData(DISCONNECT_SSID_AND_GATT, null);
                if (!mEb.isEbConnectedToSap()) {
                    transitionTo(mDisconnected);
                }
                break;

            case PREPARE_AUDIO_BEARER:
                mBearer = message.arg1;
                mBearerPrepared = false;
                if (mBearer == XpanConstants.BEARER_NONE) {
                    updatePrepareAudioBearer(mBearer, XpanConstants.BEARER_ACCEPTED);
                } if (mBearer == XpanConstants.BEARER_LE) {
                    sendAudioBearerSwitchReq(mBearer);
                } else if (mBearer == XpanConstants.BEARER_AP) {
                    processBearerAp();
                } else if (mBearer == XpanConstants.BEARER_P2P) {
                    mIsRequestedLowPower = false;
                    mIsExitLowPower = true;
                    mIsInterMediateLowPower = false;
                    mSapSm.enableLowPower(mDevice, XpanConstants.SAP_DISABLE_LOW_POWER);
                    prepareBearerMessage = Message.obtain(message);
                    mBearer = message.arg1;
                }
                break;

            case MOVE_TO_LOW_POWER:
                if(DBG) Log.d(TAG, "Already in low power, ignore");
                mSapSm.releaseWakeLock();
                break;

            case REQUESTED_SAP_POWER_STATE:
                mIsInterMediateLowPower = true;
                mIsRequestedLowPower = false;
                mIsExitLowPower = true;
                mSapSm.enableLowPower(mDevice, XpanConstants.SAP_DISABLE_LOW_POWER);
                break;

            case BEARER_SWITCHED_INDICATION :
                int bearerType = message.arg1;
                int status = message.arg2;
                if (bearerType == XpanConstants.BEARER_P2P
                        && status == XpanConstants.STATUS_FAILURE) {
                    sendData(TWT_EMPTY, message);
                } else if (bearerType == XpanConstants.BEARER_LE
                        && status == XpanConstants.STATUS_SUCESS) {
                    if (prepareBearerMessage != null) {
                        prepareBearerMessage = null;
                    }
                } else if (mBearer == XpanConstants.BEARER_AP
                        && status == XpanConstants.STATUS_SUCESS) {
                    transitionTo(mBearerAp);
                }
                break;

            case REQ_BEARER_PREFERENCE:
                handleBearerPreference(message);
                break;

            case REQ_CLEAR_TO_SEND:
                updateClearToSendReq(message.arg1);
                break;

            case AIRPLANEMODE_ENABLED:
            case SSR_WIFI_STARTED:
                transitionTo(mAirplaneMode);
                break;

            case ON_CSS:
                sendData(ON_CSS, message);
                break;

            case CONNECT_SSID :
                sendData(CONNECT_SSID, null);
                break;

            case CONNECTED_SSID:
                sendData(CONNECTED_SSID, null);
                break;

            case REQ_WIFI_SCAN_RESULTS:
                startWifiScan();
                break;

            case UPDATE_WIFI_SCAN_RESULTS:
                sendData(UPDATE_WIFI_SCAN_RESULTS, message);
                break;

           case EB_IPV4_NOTIFICATION:
           case EB_IPV6_NOTIFICATION:
               sendIpNotificationUpdate(message);
                if(!mEb.isConnected()) {
                    transitionTo(mDisconnected);
                }
                break;

           case START_FILTERED_SCAN:
               startOrStopScan(message.arg1);
               break;

           case CB_SCAN_RESULT_BT:
               connectLeLink();
               break;

           case UPDATE_USECASE_IDENTIFIER:
               sendData(UPDATE_USECASE_IDENTIFIER, null);
               break;

           case UPDATE_L2CAP_TCP_PORT:
               sendData(UPDATE_L2CAP_TCP_PORT, null);
               break;

           case UPDATE_UDP_PORT:
               sendData(UPDATE_UDP_PORT, null);
               break;

           case UPDATE_UDP_SYNC_PORT:
               sendData(UPDATE_UDP_SYNC_PORT, null);
               break;

           case WIFI_CHANNEL_SWITCH_REQ:
               updateWifiChannelSwitchReq(message);
               break;

           case MDNS_QUERY:
               updateMdnsQuery(message);
               break;

           case MDNS_REGISTER_UNREGISTER:
               updateMdnsRegisterUnregister(message);
               break;

           case RES_MDNS_DATA:
               Log.v(TAG, "LowPower, deferMessage RES_MDNS_DATA");
               deferMessage(message);
               break;

            case XPAN_EVENT:
                XpanEvent event = (XpanEvent) message.obj;
                if (VDBG)
                    Log.v(TAG, "LowPower XPAN_EVENT " + event);
                switch (event.getEventType()) {

                case XpanEvent.SAP_STATE_CHANGE:
                    int p2pState = event.getArg1();
                    if (p2pState == XpanSapStateMachine.SAP_DISABLED) {
                        transitionTo(mDisconnected);
                    }
                    break;

                case XpanEvent.SSID_CONN_STATE_CHANGE:
                    handleSsidConnectionChange(event);
                    break;

                case XpanEvent.TWT_SESSION:
                    mSendBeacon = true;
                    mBeacondata = mDataParser.getBeaconParameterBytes(0,
                            XpanConstants.GENERAL_BEACON_INT, mSapSm.getFrequency());
                    sendData(UPDATE_BEACON_INTERVAL, null);
                    mSendBeacon = false;
                    transitionTo(mConnected);
                    sendMessage(XPAN_EVENT, event);
                    break;

                case XpanEvent.UPDATE_BEACON_INTERVALS:
                    int powerSaveBi = event.getArg1();
                    if (mIsInterMediateLowPower) {
                        sendData(SAP_POWER_STATE_RES,
                                mXpanUtils.getMessage(XpanConstants.POWER_STATE_RES_ACCEPT));
                    }
                    if(powerSaveBi == XpanConstants.GENERAL_BEACON_INT) {
                        mSendBeacon = true;
                        mBeacondata = mDataParser.getBeaconParameterBytes(event.getTbtTsf(),
                                event.getArg1(), mSapSm.getFrequency());
                        if (!mIsInterMediateLowPower) {
                            sendData(UPDATE_BEACON_INTERVAL, null);
                            mSendBeacon = false;
                        }
                        if (prepareBearerMessage != null) {
                            if (VDBG)
                                Log.v(TAG, "send prepareBearerMessage");
                            deferMessage(prepareBearerMessage);
                        }
                       transitionTo(mConnected);
                    }
                    break;

                case XpanEvent.BEARER_SWITCH:
                    status = event.getArg2();
                    updatePrepareAudioBearer(event.getArg1(), status);
                    if (status != XpanConstants.AUDIO_BEARER_READY) {
                        if (VDBG)
                            Log.v(TAG, "BEARER_SWITCH_EVT AUDIO_BEARER_FAIL ");
                        break;
                    }
                    if (mBearer == XpanConstants.BEARER_P2P) {
                        transitionTo(mBearerActive);
                    } else {
                        transitionTo(mConnected);
                    }
                    break;

                case XpanEvent.BEARER_PREFERENCE_RES:
                    sendBearerPreferenceRes(event);
                    break;

                default:
                    logDebug("LowPower XPAN_EVENT not handled " + event.getEventType());
                    break;
                }
                break;
            default:
                log("LowPower not handled message " + messageToString(message.what));
                return NOT_HANDLED;
            }
            return HANDLED;
        }
    }

    class Disconnecting extends State {

        @Override
        public void enter() {
            log("Enter Disconnecting ");
            mState = DISCONNECTING;;
        }

        @Override
        public boolean processMessage(Message message) {
            log("Disconnecting processMessage " + messageToString(message.what));

            switch (message.what) {
            case CONNECT_XPAN:
                log("defer connecting to " + mDevice + " till pending disconnection");
                deferMessage(message);
                break;

            case DISCONNECT_XPAN:
                Log.w(TAG, "Already disconnecting: DISCONNECT ignored: " + mDevice);
                break;

            case REQUESTED_SAP_POWER_STATE :
                updatePowerStateRequest();
                break;

            case SAP_POWER_STATE_RES:
                sendData(SAP_POWER_STATE_RES, message);
                break;


            case REQ_BEARER_PREFERENCE:
                handleBearerPreference(message);
                break;

            case REQ_CLEAR_TO_SEND:
                updateClearToSendReq(message.arg1);
                break;

            case REQ_WIFI_SCAN_RESULTS:
                startWifiScan();
                break;

            case BEARER_SWITCHED_INDICATION:
                int bearerType = message.arg1;
                int bearerStatus = message.arg2;
                if (bearerType == XpanConstants.BEARER_AP
                        && bearerStatus == XpanConstants.BEARER_STATUS_SUCESS) {
                    transitionTo(mBearerAp);
                }
                break;

            case UPDATE_UDP_SYNC_PORT:
                sendData(UPDATE_UDP_SYNC_PORT, null);
                break;

            case CHANGE_STATE:
                transitionToState(message.arg1);
                break;

            case XPAN_EVENT:
                XpanEvent event = (XpanEvent) message.obj;

                switch (event.getEventType()) {
                case XpanEvent.SSID_CONN_STATE_CHANGE:
                    handleSsidConnectionChange(event);
                    break;

                case XpanEvent.SAP_STATE_CHANGE:
                    int p2pState = event.getArg1();
                    if (p2pState == XpanSapStateMachine.SAP_DISABLED) {
                        transitionTo(mDisconnected);
                    }
                    break;

                case XpanEvent.BEARER_PREFERENCE_RES:
                    sendBearerPreferenceRes(event);
                    break;

                default:
                    logDebug("Disconnecting XPAN_EVENT not handled " + event.getEventType());
                    break;
                }
                break;

            default:
                logDebug("Disconnecting not handled message " + messageToString(message.what));
                return NOT_HANDLED;
            }

            return HANDLED;
        }
    }

    class TwtConfigured extends State {

        @Override
        public void enter() {
            log("Enter TwtConfigured");
            mState = TWT_CONFIGURED;
        }

        @Override
        public boolean processMessage(Message message) {
            log("TwtConfigured processMessage " + messageToString(message.what));

            switch (message.what) {
            case DISCONNECT_XPAN:
                sendData(DISCONNECT_SSID_AND_GATT, null);
                break;

            case PREPARE_AUDIO_BEARER:
                mBearer = message.arg1;
                int bsState = message.arg2;
                mBearerPrepared = false;
                if (VDBG)
                    Log.v(TAG, "TwtConfigured  " + mBearer + " bsState " + bsState);
                if (mBearer == XpanConstants.BEARER_LE) {
                    sendAudioBearerSwitchReq(mBearer);
                    break;
                } else if (mBearer == XpanConstants.BEARER_NONE) {
                    sendData(TWT_EMPTY, message);
                    break;
                } else if (bsState == XpanEvent.TWT_SESSION) {
                    sendAudioBearerSwitchReq(mBearer);
                } else if (bsState == NEW_BEARER_SWITCH_REQUEST) {
                    /* For switching bearer to XPAN */
                    if (DBG) Log.v(TAG, "TwtConfigured  NEW_BEARER_SWITCH_REQUEST ");
                    sendData(TWT_CONF, message);
                    updateRemoteMacState(null, XpanConstants.MAC_FORCE_SEND_TWT, null);
                    sendAudioBearerSwitchReq(mBearer);
                } else if (mBearer == XpanConstants.BEARER_AP) {
                    Log.w(TAG, "TwtConfigured Invalid Bearer AP");
                }
                break;

            case REQUESTED_SAP_POWER_STATE :
                updatePowerStateRequest();
                break;

            case SAP_POWER_STATE_RES:
                sendData(SAP_POWER_STATE_RES, message);
                break;

            case BEARER_SWITCHED_INDICATION :
                int bearerType = message.arg1;
                int status = message.arg2;
                if (bearerType == XpanConstants.BEARER_P2P
                        && status == XpanConstants.STATUS_FAILURE) {
                    sendData(TWT_EMPTY, message);
                }
                break;

            case REQ_BEARER_PREFERENCE:
                handleBearerPreference(message);
                break;

            case REQ_CLEAR_TO_SEND:
                updateClearToSendReq(message.arg1);
                break;

            case AIRPLANEMODE_ENABLED:
            case SSR_WIFI_STARTED:
                transitionTo(mAirplaneMode);
                break;

            case ON_CSS:
                sendData(ON_CSS, message);
                break;

            case REQ_WIFI_SCAN_RESULTS:
                startWifiScan();
                break;

            case CONNECTED_SSID:
                sendData(CONNECTED_SSID, null);
                break;

            case UPDATE_WIFI_SCAN_RESULTS:
                sendData(UPDATE_WIFI_SCAN_RESULTS, message);
                break;

            case UPDATE_USECASE_IDENTIFIER:
                sendData(UPDATE_USECASE_IDENTIFIER, null);
                break;

            case UPDATE_UDP_SYNC_PORT:
                sendData(UPDATE_UDP_SYNC_PORT, null);
                break;

            case UPDATE_L2CAP_TCP_PORT:
                sendData(UPDATE_L2CAP_TCP_PORT, null);
                break;

            case UPDATE_UDP_PORT:
                sendData(UPDATE_UDP_PORT, null);
                break;

            case WIFI_CHANNEL_SWITCH_REQ:
                updateWifiChannelSwitchReq(message);
                break;

            case CHANGE_STATE:
                transitionToState(message.arg1);
                break;

            case XPAN_EVENT:
                XpanEvent event = (XpanEvent) message.obj;

                switch (event.getEventType()) {
                case XpanEvent.TWT_SESSION:
                    int twtType = event.getArg3();
                    updateRemoteMacState(event.getRemoteMacAddr(), -1, event);
                    if (twtType == XpanConstants.TWT_TERMINATE) {
                        if (!isTwtConfigured()) {
                            transitionTo(mConnected);
                        }
                    }
                    break;

                case XpanEvent.BEARER_SWITCH:
                    status = event.getArg2();
                    updatePrepareAudioBearer(event.getArg1(), status);
                    if (status != XpanConstants.AUDIO_BEARER_READY) {
                        if (VDBG)
                            Log.v(TAG, "BEARER_SWITCH_EVT AUDIO_BEARER_FAIL ");
                        break;
                    }
                    if (mBearer == XpanConstants.BEARER_P2P) {
                        transitionTo(mBearerActive);
                    } else {
                        if (DBG) Log.d(TAG, "TwtConfigured BEARER_SWITCH_EVT " + mBearer
                                + " TWT not terminated yet");
                    }
                    break;

                case XpanEvent.SSID_CONN_STATE_CHANGE:
                    handleSsidConnectionChange(event);
                    break;

                case XpanEvent.SAP_STATE_CHANGE:
                    int p2pState = event.getArg1();
                    if (p2pState == XpanSapStateMachine.SAP_DISABLED) {
                        transitionTo(mDisconnected);
                    }
                    break;

                case XpanEvent.BEARER_PREFERENCE_RES:
                    sendBearerPreferenceRes(event);
                    break;

                case ON_SAP_IF_UPDATE:
                    handleSapIfUpdate();
                    break;

                default:
                    logDebug("TwtConfigured XPAN_EVENT not handled " + event.getEventType());
                    break;
                }
                break;

            default:
                logDebug("TwtConfigured not handled message " + messageToString(message.what));
                return NOT_HANDLED;
            }
            return HANDLED;
        }
    }

    class BearerActive extends State {

        @Override
        public void enter() {
            log("Enter BearerActive");
            mState = BEARER_ACTIVE;
            mDatabaseManager.updateDeviceBearerToDb(mDevice, XpanConstants.BEARER_P2P);
        }

        @Override
        public boolean processMessage(Message message) {
            log("BearerActive processMessage " + messageToString(message.what));

            switch (message.what) {
            case DISCONNECT_XPAN:
                sendData(DISCONNECT_SSID_AND_GATT, null);
                break;

            case CONNECT_XPAN:
                Log.w(TAG, "ignored: " + mDevice);
                break;

            case PREPARE_AUDIO_BEARER:
                mBearer = message.arg1;
                mBearerPrepared = false;
                if (mBearer == XpanConstants.BEARER_LE) {
                    sendAudioBearerSwitchReq(mBearer);
                    break;
                } else if (mBearer == XpanConstants.BEARER_NONE) {
                    sendData(TWT_EMPTY, message);
                    transitionTo(mTwtConfigured);
                    break;
                } else if (mBearer == XpanConstants.BEARER_P2P) {
                    /* For switching bearer to XPAN */
                    if (DBG)
                        Log.d(TAG, "Already streaming on P2P");
                    updatePrepareAudioBearer(mBearer, XpanConstants.BEARER_STATUS_FAILURE);
                    break;
                } else {
                    Log.w(TAG,"Invalid PREPARE_AUDIO_BEARER " + mBearer);
                }
                break;

            case REQUESTED_SAP_POWER_STATE :
                updatePowerStateRequest();
                break;

            case SAP_POWER_STATE_RES:
                sendData(SAP_POWER_STATE_RES, message);
                break;

            case BEARER_SWITCHED_INDICATION:
                int bearerType = message.arg1;
                int bearerStatus = message.arg2;
                if (bearerType == XpanConstants.BEARER_LE
                        && bearerStatus == XpanConstants.BEARER_STATUS_SUCESS) {
                    sendData(TWT_EMPTY, message);
                    transitionTo(mTwtConfigured);
                } else if (bearerType == XpanConstants.BEARER_P2P
                        && bearerStatus == XpanConstants.BEARER_STATUS_FAILURE) {
                    sendData(TWT_EMPTY, message);
                    transitionTo(mTwtConfigured);
                    mRemoteReqBearer = XpanConstants.BEARER_LE;
                }
                break;

            case REQ_BEARER_PREFERENCE:
                handleBearerPreference(message);
                break;

            case REQ_CLEAR_TO_SEND:
                updateClearToSendReq(message.arg1);
                break;

            case AIRPLANEMODE_ENABLED:
            case SSR_WIFI_STARTED:
                transitionTo(mAirplaneMode);
                break;

            case ON_CSS:
                sendData(ON_CSS, message);
                break;

            case UPDATE_USECASE_IDENTIFIER:
                sendData(UPDATE_USECASE_IDENTIFIER, null);
                break;

            case REQ_WIFI_SCAN_RESULTS:
                startWifiScan();
                break;

            case CONNECTED_SSID:
                sendData(CONNECTED_SSID, null);
                break;

            case UPDATE_WIFI_SCAN_RESULTS:
                sendData(UPDATE_WIFI_SCAN_RESULTS, message);
                break;

            case UPDATE_L2CAP_TCP_PORT:
                sendData(UPDATE_L2CAP_TCP_PORT, null);
                break;

            case UPDATE_UDP_PORT:
                sendData(UPDATE_UDP_PORT, null);
                break;

            case UPDATE_UDP_SYNC_PORT:
                sendData(UPDATE_UDP_SYNC_PORT, null);
                break;

            case WIFI_CHANNEL_SWITCH_REQ:
                updateWifiChannelSwitchReq(message);
                break;

            case XPAN_EVENT:
                XpanEvent event = (XpanEvent) message.obj;

                switch (event.getEventType()) {
                case XpanEvent.BEARER_SWITCH:
                    int changedBearer = event.getArg1();
                    int status = event.getArg2();
                    if (VDBG)
                        Log.v(TAG, "BearerActive BEARER_SWITCH_EVT " +
                                changedBearer + " status " + status);
                    updatePrepareAudioBearer(changedBearer, status);
                    if (status != XpanConstants.AUDIO_BEARER_READY) {
                        if (VDBG)
                            Log.v(TAG, "BEARER_SWITCH_EVT AUDIO_BEARER_FAIL ");
                        break;
                    }
                    break;

                case XpanEvent.SSID_CONN_STATE_CHANGE:
                    handleSsidConnectionChange(event);
                    break;

                case XpanEvent.SAP_STATE_CHANGE:
                    break;

                case XpanEvent.TWT_SESSION:
                    updateRemoteMacState(event.getRemoteMacAddr(), -1, event);
                    if (event.getArg3() == XpanConstants.TWT_TERMINATE) {
                        if (!isTwtConfigured()) {
                            transitionTo(mConnected);
                        }
                    }
                    break;

                case XpanEvent.BEARER_PREFERENCE_RES:
                    sendBearerPreferenceRes(event);
                    break;

                default:
                    logDebug("BearerActive not handled message " + messageToString(message.what));
                    break;
                }
                break;

            case ON_SAP_IF_UPDATE:
                handleSapIfUpdate();
                break;

            default:
                logDebug("BearerActive not handled message " + messageToString(message.what));
                return NOT_HANDLED;
            }
            return HANDLED;
        }
    }

    class AirplaneMode extends State {

        @Override
        public void enter() {
            log("Enter AirplaneMode");
            mState = AIRPLANE_MODE;
            if (mIsAirplaneMode) {
                updateTransport(false, XpanConstants.TP_REASON_AIRPLANE_ENABLED);
                setCodecConfigPreference(LeAudioProxy.CODEC_TYPE_DEFAULT);
            }
            sendData(UPDATE_SAP_STATE, mXpanUtils.getMessage(XpanConstants.SAP_STATE_OFF,
                    XpanConstants.SAP_TD_REASON_AP));
            mDatabaseManager.updateDeviceBearerToDb(mDevice, XpanConstants.BEARER_LE);
        }

        @Override
        public void exit() {
            if (mIsAirplaneMode) {
                mIsAirplaneMode = false;
                setCodecConfigPreference(LeAudioProxy.CODEC_TYPE_APTX_ADAPTIVE_R4);
            }
        }

        @Override
        public boolean processMessage(Message msg) {
            log("AirplaneMode processMessage " + messageToString(msg.what));

            switch (msg.what) {
            case DISCONNECT_XPAN:
                if (mIsAirplaneMode) {
                    mIsAirplaneMode = false;
                    mXpanUtils.cacheDeviceCodec(mDevice, LeAudioProxy.CODEC_TYPE_APTX_ADAPTIVE_R4);
                }
                sendData(DISCONNECT_SSID_AND_GATT, null);
                transitionTo(mDisconnected);
                break;

            case PREPARE_AUDIO_BEARER:
                mBearer = msg.arg1;
                int bsState = msg.arg2;
                if (DBG)
                    Log.d(TAG, "PREPARE_AUDIO_BEARER " + mBearer);
                if (mBearer == XpanConstants.BEARER_LE) {
                    sendAudioBearerSwitchReq(mBearer);
                    break;
                } else if (mBearer == XpanConstants.BEARER_NONE) {
                    sendData(TWT_EMPTY, msg);
                    break;
                } else {
                    if (DBG)
                        Log.d(TAG, " Ignore PREPARE_AUDIO_BEARER  " + mBearer);
                }
                break;

            case REQ_BEARER_PREFERENCE:
                handleBearerPreference(msg);
                break;

            case REQ_CLEAR_TO_SEND:
                updateClearToSendReq(msg.arg1);
                break;

            case AIRPLANEMODE_DISABLED:
            case SSR_WIFI_COMPLETED:
                transitionTo(mDisconnected);
                connectXpan();
                break;

            case REQUESTED_SAP_POWER_STATE :
            case SAP_POWER_STATE_RES:
                sendData(SAP_POWER_STATE_RES,
                        mXpanUtils.getMessage(XpanConstants.POWER_STATE_RES_REJECT));
                break;

            case EB_IPV4_NOTIFICATION:
            case EB_IPV6_NOTIFICATION:
                sendIpNotificationUpdate(msg);
                if (mEb.isConnected()) {
                    transitionTo(mConnected);
                }
                break;

            case UPDATE_WIFI_SCAN_RESULTS:
                sendData(UPDATE_WIFI_SCAN_RESULTS, msg);
                break;

            case XPAN_EVENT:
                XpanEvent event = (XpanEvent) msg.obj;
                if (VDBG) Log.v(TAG, "AirplaneMode XPAN_EVENT " +event);
                switch (event.getEventType()) {
                case XpanEvent.SSID_CONN_STATE_CHANGE:
                    handleSsidConnectionChange(event);
                    break;

                case XpanEvent.SAP_STATE_CHANGE:
                    break;

                case XpanEvent.TWT_SESSION:
                    updateRemoteMacState(event.getRemoteMacAddr(), -1, event);
                    break;

                case XpanEvent.BEARER_PREFERENCE_RES:
                    sendBearerPreferenceRes(event);
                    break;

                default:
                    logDebug("AirplaneMode Xpan Event not handled");
                    break;
                }
                break;

            default:
                logDebug("AirplaneMode not handled message " + messageToString(msg.what));
                return NOT_HANDLED;
            }
            return HANDLED;
        }
    }

    class BearerAP extends State {

        @Override
        public void enter() {
            logDebug("Enter BearerAP");
            mState = BEARER_AP;
            mDatabaseManager.updateDeviceBearerToDb(mDevice, XpanConstants.BEARER_AP);
        }

        @Override
        public void exit() {
            if (mFilterScan) {
                startOrStopScan(XpanConstants.DISCOVERY_STOP);
            }
        }

        @Override
        public boolean processMessage(Message message) {
            log("BearerAP processMessage " + messageToString(message.what));

            switch (message.what) {
            case DISCONNECT_XPAN:
                sendData(DISCONNECT_SSID_AND_GATT, null);
                transitionTo(mDisconnected);
                log("BearerAP move to disconnected");
                break;

            case CONNECT_XPAN:
                Log.w(TAG, "Ignore CONNECT_XPAN");
                break;

            case ENABLE_SAP:
                mSapSm.enableSap(mDevice, sapCb);
                break;

            case PREPARE_AUDIO_BEARER:
                mBearer = message.arg1;
                mBearerPrepared = false;
                if (mBearer == XpanConstants.BEARER_LE || mBearer == XpanConstants.BEARER_NONE
                        || mBearer == XpanConstants.BEARER_AP_PREP
                        || mBearer == XpanConstants.BEARER_AP) {
                    updatePrepareAudioBearer(mBearer, XpanConstants.BEARER_STATUS_SUCESS);
                    if (mBearer == XpanConstants.BEARER_NONE) {
                        transitionTo(mConnected);
                    }
                }
                break;

            case REQUESTED_SAP_POWER_STATE :
                updatePowerStateRequest();
                break;

            case SAP_POWER_STATE_RES:
                sendData(SAP_POWER_STATE_RES, message);
                break;

            case REQ_BEARER_PREFERENCE:
                handleBearerPreference(message);
                break;

            case REQ_CLEAR_TO_SEND:
                updateClearToSendReq(message.arg1);
                break;

            case AIRPLANEMODE_ENABLED:
            case SSR_WIFI_STARTED:
                transitionTo(mAirplaneMode);
                break;

            case EB_IPV4_NOTIFICATION:
            case EB_IPV6_NOTIFICATION:
                sendIpNotificationUpdate(message);
                break;

            case START_FILTERED_SCAN:
                startOrStopScan(message.arg1);
                break;

            case CB_SCAN_RESULT_BT:
                connectLeLink();
                break;

            case CB_LE_LINK:
                handleLeLinkStatus(message.arg1);
                break;

            case REQ_WIFI_SCAN_RESULTS:
                startWifiScan();
                break;

            case CONNECTED_SSID:
                sendData(CONNECTED_SSID, null);
                break;

            case UPDATE_WIFI_SCAN_RESULTS:
                sendData(UPDATE_WIFI_SCAN_RESULTS, message);
                break;

            case MDNS_QUERY:
                updateMdnsQuery(message);
                break;

            case UPDATE_USECASE_IDENTIFIER:
                sendData(UPDATE_USECASE_IDENTIFIER, null);
                break;

            case UPDATE_L2CAP_TCP_PORT:
                sendData(UPDATE_L2CAP_TCP_PORT, null);
                break;

            case UPDATE_UDP_PORT:
                sendData(UPDATE_UDP_PORT, null);
                break;

            case UPDATE_UDP_SYNC_PORT:
                sendData(UPDATE_UDP_SYNC_PORT, null);
                break;

            case WIFI_CHANNEL_SWITCH_REQ:
                updateWifiChannelSwitchReq(message);
                break;

            case MDNS_REGISTER_UNREGISTER:
                updateMdnsRegisterUnregister(message);
                break;

            case RES_MDNS_DATA:
                updateApDetailsRemote(message);
                break;

            case XPAN_EVENT:
                XpanEvent event = (XpanEvent) message.obj;
                if (VDBG) Log.v(TAG, "BearerAP XPAN_EVENT " + event);

                switch (event.getEventType()) {
                case XpanEvent.BEARER_SWITCH:
                    int changedBearer = event.getArg1();
                    updatePrepareAudioBearer(changedBearer, event.getArg2());
                    // ToDo: re-check prepare bearer
                    if (changedBearer != XpanConstants.BEARER_P2P) {
                        transitionTo(mConnected);
                    }
                    break;

                case XpanEvent.SSID_CONN_STATE_CHANGE:
                    handleSsidConnectionChange(event);
                    break;

                case XpanEvent.SAP_STATE_CHANGE:
                    break;

                case XpanEvent.TWT_SESSION:
                    updateRemoteMacState(event.getRemoteMacAddr(), -1, event);
                    break;

                case XpanEvent.BEARER_PREFERENCE_RES:
                    sendBearerPreferenceRes(event);
                    break;

                default:
                    logDebug("BearerAP not handled message XPAN_EVENT " + messageToString(message.what));
                    break;
                }
                break;
            default:
                logDebug("BearerAP not handled message " + messageToString(message.what));
                return NOT_HANDLED;
            }
            return HANDLED;
        }

    }

    private void sendIpNotificationUpdate(Message msg) {
        List<RemoteMacAddress> listMac = (List<RemoteMacAddress>) msg.obj;
        //String ssidAp = mApDetails.getSsid();
        boolean connected = false;
        for (RemoteMacAddress remoteMac : listMac) {
            String ssid = remoteMac.getSsid();
            RemoteMacAddress remoteMacInfo = mEb.getMac(remoteMac.getMacAddress());
            if (remoteMac.isIpv4()) {
                remoteMacInfo.init(remoteMac.getSsid(), remoteMac.getBssid(),
                        remoteMac.getIpv4Address(), remoteMac.getAudioLocation(),
                        remoteMac.getRole(), remoteMac.getStatus());
            } else if (remoteMac.isIpv6()) {
                remoteMacInfo.init(remoteMac.getSsid(), remoteMac.getBssid(),
                        remoteMac.getIpv6Address(), remoteMac.getAudioLocation(),
                        remoteMac.getRole(), remoteMac.getStatus());
            }
            if (!connected) {
                connected = (remoteMac.getStatus() == XpanConstants.EB_CONNECTED) ? true : false;
            }
        }
        if (VDBG)
            Log.v(TAG, "sendIpNotificationUpdate " + connected);
        if (connected && mApSm.isConnected()) {
            if (mState < CONNECTED) {
                if (mCurrentTransport == XpanConstants.BEARER_AP
                        && getState() != BEARER_AP) {
                    transitionTo(mBearerAp);
                } else {
                    transitionTo(mConnected);
                }
            }
        } else if (mState >= CONNECTED) {
            transitionTo(mDisconnected);
        }
        mProviderClient.updateApDetailsRemote(mEb);
    }

    private void updateWifiChannelSwitchReq(Message msg) {
        mProviderClient.updateWifiChannelSwitchReq(mDevice, msg.arg1);
    }

    /* Xpan Connection State Machine API's */
    void connectXpan() {
        logDebug("connectXpan");
        sendMessage(CONNECT_XPAN);
    }

    void disconnectXpan() {
        logDebug("disconnectXpan");

        if (mState == DISCONNECTED) {
            if (DBG)
                Log.d(TAG, "Already in disconnected state");
            return;
        }

        sendMessage(DISCONNECT_XPAN);
    }

    void onPrepareAudioBearer(int bearer) {
        logDebug("onPrepareAudioBearer :" + XpanConstants.getBearerString(bearer) +
                " " + XpanConstants.getBearerString(mRemoteReqBearer));
        if (bearer != XpanConstants.BEARER_NONE
                && mPlayingState != XpanConstants.GROUP_STREAM_STATUS_STREAMING) {
            mPlayingState = XpanConstants.GROUP_STREAM_STATUS_STREAMING;
            logDebug("onPrepareAudioBearer GROUP_STREAM_STATUS_STREAMING");
        }
        if (mRemoteReqBearer == XpanConstants.BEARER_P2P
                && mCurrentTransport == XpanConstants.BEARER_AP) {
            sendMessage(PREPARE_AUDIO_BEARER, bearer, NEW_BEARER_SWITCH_REQUEST);
        } else if (mRemoteReqBearer != XpanConstants.BEARER_DEFAULT && mRemoteReqBearer != bearer
                && bearer != XpanConstants.BEARER_NONE) {
            mBearerPrepared = false;
            updatePrepareAudioBearer(bearer, XpanConstants.BEARER_REJECTED);
        } else {
            /*
             * Steps (bearer = XPAN):
             * 1. Get TWT Parameter as per Audio Mode and send it to remote device.
             * 2. Receive TWT parameter updated callback from Xpan Manager.
             *    Move to TWT_CONFIGURED State.
             * 3. Send Bearer Switch Request to remote.
             * 4. ON receiving Bearer Switched Callback, move to XPAN_BEARER_ACTIVE_STATE.
             * 5. Send updateBearerPrepared response to Xpan Manager.
             *
             * Steps (bearer = BT)
             * 1. Send Bearer switch request to Xpan Manager and wait for callback.
             * 2. Receive callback for bearer switched and then move to CONNECTED State.
             * 3. Start SAP Low power timer if transport is P2P.
             */
            sendMessage(PREPARE_AUDIO_BEARER, bearer, NEW_BEARER_SWITCH_REQUEST);
        }
    }

    void onBearerSwitchIndication(int bearer, int status) {
        if (DBG)
            Log.d(TAG, "onBearerSwitchIndication " + bearer + " status " + status);
        if (mIsIdle && status == XpanConstants.BEARER_STATUS_SUCESS) {
            resetIdle();
        }
        if (status == XpanConstants.BEARER_STATUS_SUCESS) {
            mDatabaseManager.updateDeviceBearerToDb(mDevice, bearer);
        }
        sendMessage(BEARER_SWITCHED_INDICATION, bearer, status);
    }

    void onUseCaseUpdate(int useCase) {
        if (VDBG)
            Log.v(TAG, "onUseCaseUpdate " + mUseCase + " " + useCase);
        mUseCase = useCase;
        if (mCurrentTransport == XpanConstants.BEARER_AP) {
            sendMessage(UPDATE_USECASE_IDENTIFIER);
        }
    }

    void onPortNumbers() {
        if (!mGattSm.isConfigured()) {
            mGattSm.SetPortUpdatePending(true);
            if (VDBG)
                Log.v(TAG, "onPortNumbers ignore");
            return;
        }
        if (VDBG)
            Log.v(TAG, "onPortNumbers");
        if (mApSm.getL2capTcpPort() != 0) {
            sendMessage(UPDATE_L2CAP_TCP_PORT);
        } else {
            sendMessage(UPDATE_UDP_PORT);
        }
    }

    void onBearerSwitchFailed(int reason, int role) {
        if (VDBG)
            Log.v(TAG, "onBearerSwitchFailed " + reason + " " + role);
        sendData(CB_BEARER_SWITCH_FAILED, mXpanUtils.getMessage(reason, role));
    }

    /* SAP Callbacks */
    private XpanSapStateMachine.XpanSapCallback sapCb = new XpanSapStateMachine.XpanSapCallback() {

        @Override
        public void onSapStateChanged(String ssid, int state) {
            logDebug("onSapStateChanged " + state);

            if (state == XpanSapStateMachine.SAP_DISABLED) {
                if(!mSapSm.isSapLocalDisable()) {
                    sendData(UPDATE_SAP_STATE, mXpanUtils.getMessage(XpanConstants.SAP_STATE_OFF,
                            XpanConstants.SAP_TD_REASON_CONCURRENCY));
                }
            }
            XpanEvent event = new XpanEvent(XpanEvent.SAP_STATE_CHANGE);
            event.setArg1(state);
            event.setObject(ssid);
            sendMessage(XPAN_EVENT, event);
        }

        @Override
        public void onUpdatedBeaconIntervals(int powerSaveBi, long nextTsf) {
            logDebug("onUpdatedBeaconIntervals powerSaveBi " + powerSaveBi + " nextTsf "
                + nextTsf);
            if (mIsRequestedLowPower || mIsExitLowPower || mIsInterMediateLowPower
                    || mReqPowerState) {
                updatedBeaconIntervals(powerSaveBi, nextTsf);
                if (mIsExitLowPower) {
                    mIsExitLowPower = false;
                } else if(mIsRequestedLowPower) {
                    mIsRequestedLowPower = false;
                }
            } else {
                Log.w(TAG, "Ignore Beacon interval update");
                return;
            }
        }

        public void onAirplanemodeEnabled(boolean isEnable) {
            logDebug("onAirplanemodeEnabled " + isEnable);
            int msg = AIRPLANEMODE_DISABLED;
            if (isEnable) {
                mIsAirplaneMode = true;
                msg = AIRPLANEMODE_ENABLED;
            }
            sendMessage(msg);
        }

        public void onWifiSsr(int state) {
            logDebug("onWifiSsr " + state);
            mSSRMode = false;
            int msg = SSR_WIFI_COMPLETED;
            if (state == XpanConstants.WIFI_SSR_STARTED) {
                mSSRMode = true;
                msg = SSR_WIFI_STARTED;
            }
            sendMessage(msg);
        }

        @Override
        public void onCsaUpdate(XpanEvent event) {
            sendMessage(ON_CSS, event);
        }

        @Override
        public void onFrequencyUpdated(int frequency) {
            sendMessage(FREQUENCY_UPDATED, frequency);
        }

        @Override
        public void onSapIfStatusUpdate() {
            sendMessage(ON_SAP_IF_UPDATE);
        }
    };

    private void updatedBeaconIntervals(int powerSaveBi, long nextTsf) {
        XpanEvent event = new XpanEvent(XpanEvent.UPDATE_BEACON_INTERVALS);
        event.setArg1(powerSaveBi);
        event.setTbtTsf(nextTsf);
        sendMessage(XPAN_EVENT, event);
    }

    void sendMacStateChanged(MacAddress macAddress, boolean connected) {
        boolean update = false;
        int status = -1;
        int macState = -1;
        RemoteMacAddress remoteMac = null;
        for (RemoteMacAddress mac : mListRemoteMac) {
            if (macAddress.equals(mac.getMacAddress())) {
                remoteMac = mac;
            }
        }
        if (remoteMac == null) {
            logDebug("sendMacStateChanged ignore " + macAddress
                    + " mListRemoteMac " + mListRemoteMac.size());
            return;
        }
        logDebug(remoteMac.toString());
        if (connected && remoteMac.isDisConnected()) {
            update = true;
            status = XpanConstants.EB_CONNECTED;
            macState = XpanConstants.MAC_CONNECTED;
            logDebug("Updated state to connected for " + remoteMac.getMacAddress());
        } else if (!connected && remoteMac.isConnected()) {
            update = true;
            status = XpanConstants.EB_DISCONNECTED;
            macState = XpanConstants.MAC_DSICONNECTED;
            logDebug("Updated state to disconnected for " + remoteMac.getMacAddress());
        } else {
            update = false;
            status = -1;
            logDebug("no need to update state");
        }
        if (update) {
            XpanEvent event = new XpanEvent(XpanEvent.SSID_CONN_STATE_CHANGE);
            event.setArg1(status);
            event.setRemoteMacAddr(macAddress);
            handleSsidConnectionChange(event);
        }
    }

    private void updateRemoteMacState(MacAddress addr, int macState, XpanEvent event) {

        if (DBG)
            Log.d(TAG, "updateRemoteMacState " + addr + " macState " + macState
                    + " event " + event);
        boolean updateTwt = false;
        boolean isTwtTerminate = false;
        boolean isConnUpdated = false;

        for (RemoteMacAddress remotemac : mListRemoteMac) {
            if (macState == XpanConstants.MAC_FORCE_SEND_TWT) {
                updateTwt = true;
                break;
            } else if (remotemac.getMacAddress().equals(addr)) {
                if (macState == XpanConstants.MAC_CONNECTED) {

                    if (remotemac.isDisConnected()) {
                        remotemac.setConnected(true);
                        isConnUpdated = true;
                    } else {
                        if (VDBG) Log.v(TAG, "updateRemoteMacState already connected " + addr);
                    }
                } else if (macState == XpanConstants.MAC_DSICONNECTED) {
                    if (remotemac.isConnected()) {
                        if (!updateTwt) {
                            updateTwt = remotemac.isTwtConfigured();
                        }
                        remotemac.setConnected(false);
                        isConnUpdated = true;
                    } else {
                        if (VDBG) Log.v(TAG, "updateRemoteMacState already disconnected " + addr);
                    }
                } else if (event != null) {
                    int twtType = event.getArg3();
                    int si = event.getArg1();
                    int sp = event.getArg2();

                    if (twtType == XpanConstants.TWT_SETUP) {
                        if (remotemac.isTwtConfigured()) {
                            if (VDBG) Log.v(TAG, "updateRemoteMacState already Configured " + addr);
                        } else {
                            remotemac.setTwtConfigured(true, si, sp);
                            if (!updateTwt) {
                                updateTwt = true;
                            }
                        }
                    } else if (twtType == XpanConstants.TWT_TERMINATE) {
                        if (remotemac.isTwtConfigured()) {
                            remotemac.setTwtConfigured(false, 0, 0);
                            if (!updateTwt) {
                                updateTwt = true;
                            }
                            if (event.getArg3() == XpanEvent.TWT_TERMINATE_IN_CONNECTED_STATE
                                    && updateTwt) {
                                updateTwt = false;
                                if (VDBG) Log.v(TAG, "Ignore updateTwt");
                            }
                            isTwtTerminate = true;
                        } else {
                            if (VDBG) Log.v(TAG, "updateRemoteMacState  TWT Terminated " + addr);
                        }
                    }
                } else {
                    Log.w(TAG, "Invalid updateRemoteMacState " + addr + " macState " + macState
                            + " event " + event);
                }
            }
        }
        if (VDBG) Log.v(TAG, "updateRemoteMacState " + updateTwt + " " + isConnUpdated);
        if(isConnUpdated) {
            updateConnectedEbDetails();
        }
        if (!updateTwt) {
            return;
        }

        List<String> mListMac = new ArrayList<String>();
        List<Integer> mListAudioLocation = new ArrayList<Integer>();
        List<Integer> mListSi = new ArrayList<Integer>();
        List<Integer> mListSp = new ArrayList<Integer>();
        List<Boolean> mListTwtConfigured = new ArrayList<Boolean>();

        for (RemoteMacAddress remoteMac : mListRemoteMac) {
            if (remoteMac.isConnected() && remoteMac.isTwtConfigured()) {
                mListMac.add(remoteMac.getMacAddress().toString());
                mListAudioLocation.add(remoteMac.getAudioLocation());
                mListSi.add(remoteMac.getSi());
                mListSp.add(remoteMac.getSp());
                mListTwtConfigured.add(true);
            }
        }
        if (mListMac.size() > 0) {
            updateTwtSessionParams(mRightOffset, mVbcPeriodicity, mListMac, mListAudioLocation,
                    mListTwtConfigured, mListSi, mListSp);
        } else if (isTwtTerminate) {
            updateTwtSessionParams(0, mVbcPeriodicity, mListMac, mListAudioLocation,
                    mListTwtConfigured, mListSi, mListSp);
        }
    }

    private void handleSsidConnectionChange(XpanEvent event) {
        int status = event.getArg1();
        MacAddress macAddress = event.getRemoteMacAddr();
        if (VDBG)
            Log.v(TAG, "handleSsidConnectionChange " + macAddress);

        if (status == XpanConstants.EB_CONNECTED) {
            updateRemoteMacState(macAddress,
                    XpanConstants.MAC_CONNECTED, null);
            if (mState == DISCONNECTED || mState == CONNECTING || mState == DISCONNECTING) {
                updateTransport(true, XpanConstants.TP_REASON_CONNECTED);
                sendMessage(CHANGE_STATE, CONNECTED);
            } else if (mState == LOW_POWER || mState == CONNECTED) {
                updateTransport(true, XpanConstants.TP_REASON_CONNECTED);
            }
        } else if (status == XpanConstants.EB_DISCONNECTED) {

            if (mSapSm.isClientConnected(macAddress)) {
                if (DBG)
                    Log.w(TAG, "handleSsidConnectionChange ignore");
                return;
            }
            updateRemoteMacState(macAddress,
                    XpanConstants.MAC_DSICONNECTED, null);
            boolean disconnect = true;
            for (RemoteMacAddress remoteMac : mListRemoteMac) {
                if (remoteMac.isConnected()) {
                    disconnect = false;
                    break;
                }
            }

            if (disconnect && (mState != DISCONNECTING || mState!= DISCONNECTED)) {
                if (mState != AIRPLANE_MODE) {
                    sendMessage(CHANGE_STATE, DISCONNECTED);
                }
                updateTransport(false, XpanConstants.TP_REASON_DISCONNECT);

            } else {
                Log.w(TAG, "handleSsidConnectionChange EARBUD_DISCONNECTED_FROM_SSID ignore");
            }
        } else {
            Log.w(TAG, "handleSsidConnectionChange invalid");
        }
    }

    /* XPan Manager Callbacks */
    void onTwtSession(MacAddress macAddress, int sp, int si, int type) {
        logDebug("onTwtSession " + macAddress + " sp " + sp
                + " si " + si + " " + XpanConstants.getTwtTypeString(type));

        RemoteMacAddress remoteMac = getRemoteMacAddressFromMacAddress(macAddress);
        if (remoteMac.isDisConnected()) {
            if (type == XpanConstants.TWT_SETUP) {
                if(DBG) Log.d(TAG,"onTwtSession TWT SETUP for non connected MAC "
                    + macAddress + " deferring to check");
                XpanEvent event = new XpanEvent(XpanEvent.TWT_SESSION);
                event.setArg1(si);
                event.setArg2(sp);
                event.setArg3(type);
                event.setRemoteMacAddr(macAddress);
                sendMessageDelayed(XPAN_EVENT, event, 50);
            } else {
                if(DBG) Log.d(TAG,"onTwtSession TWT_TERMINATE for non connected MAC "
                    + macAddress + " ignoring");
            }
            return;
        } else if (type == XpanConstants.TWT_SETUP && remoteMac.isTwtConfigured()) {
            if(DBG) Log.d(TAG,"onTwtSession Twt configured already " + macAddress);
            return;
        } else if (type == XpanConstants.TWT_TERMINATE && !remoteMac.isTwtConfigured()) {
            if(DBG) Log.d(TAG,"onTwtSession Twt not configured to terminate "
                    + macAddress);
            return;
        }
        XpanEvent event = new XpanEvent(XpanEvent.TWT_SESSION);
        event.setArg1(si);
        event.setArg2(sp);
        event.setArg3(type);
        event.setRemoteMacAddr(macAddress);
        sendMessage(XPAN_EVENT, event);
    }

    void updatePowerStateRequest() {
        if (VDBG) {
            Log.v(TAG, "updatePowerStateRequest");
        }
        if (mSapSm.isCbLpPending() && mState == CONNECTED) {
            if (VDBG) {
                Log.v(TAG, "updatePowerStateRequest CbLpPending");
            }
            mReqPowerStatePen = true;
        } else if (mSapSm.isLohsStarted()) {
            mProviderClient.updatePowerStateRequest(mDevice,
                    XpanConstants.INTERMEDIATE_ACTIVE_DURATION_MIN);
        } else {
            sendData(SAP_POWER_STATE_RES,
                    mXpanUtils.getMessage(XpanConstants.POWER_STATE_RES_REJECT));
        }
    }

    /*
     * Received power state response from Wifi Vendor
     */
    void onPowerStateRes(int status) {
        if (VDBG) Log.v(TAG, "onPowerStateRes " + status);
        sendMessage(SAP_POWER_STATE_RES, status);
    }

    private void sendAudioBearerSwitchReq(int bearer) {
        boolean isSendSwitchReq = ((mPlayingState == XpanConstants.GROUP_STREAM_STATUS_STREAMING)
                && !mIsIdle);
        logDebug("sendAudioBearerSwitchReq " + bearer + " isSendSwitchReq "
            + isSendSwitchReq + " Idle " + mIsIdle);
        if (!mIsIdle  && isSendSwitchReq && bearer != XpanConstants.BEARER_NONE) {
            mGattSm.sendAudioBearerSwitchReq(bearer);
        } else {
            updatePrepareAudioBearer(bearer, XpanConstants.BEARER_STATUS_SUCESS);
            onAudioBearerSwitchResponse(bearer,XpanConstants.BEARER_STATUS_SUCESS);
        }
    }

    void onAudioBearerSwitchResponse(int bearer, int status) {
        logDebug("onAudioBearerSwitchResponse " + bearer + ", status " + status);
        XpanEvent event = new XpanEvent(XpanEvent.BEARER_SWITCH);
        event.setArg1(bearer);
        event.setArg2(status);
        sendMessage(XPAN_EVENT, event);
    }

    private boolean isTwtConfigured() {
        boolean twtConfigured = false;
        for (RemoteMacAddress mac : mListRemoteMac) {
            if (mac.isConnected() && mac.isTwtConfigured()) {
                twtConfigured = true;
                break;
            }
        }
        return twtConfigured;
    }

    /*
     * Received Clear To Send from peer
     */
    void reqClearToSend(int ctsReq) {
        if (VDBG) Log.v(TAG, "reqClearToSend " + ctsReq);
        sendMessage(REQ_CLEAR_TO_SEND, ctsReq);
    }

    /*
     * Received bearer preference from peer or Wifi Vendor
     */
    void onBearerPreference(Requestor requester, int bearer, int reason) {
        if (VDBG) Log.v(TAG, "onBearerPreference " + requester
                +"  " + XpanConstants.getBearerString(bearer) + " " + reason);
        XpanEvent event = getBearerPrefEvent(XpanEvent.BEARER_PREFERENCE, requester.ordinal(), bearer,
                reason, 0 /* Xm should ignore this for for NON AP cases  */);
        sendMessage(REQ_BEARER_PREFERENCE, event);
        if (bearer == XpanConstants.BEARER_LE
                && reason == XpanConstants.TP_SWITCH_REASON_TERMINATING) {
            sendData(UPDATE_SAP_STATE, mXpanUtils.getMessage(XpanConstants.SAP_STATE_OFF,
                    XpanConstants.SAP_TD_REASON_CONCURRENCY));
        }
    }

    /*
     * Received bearer preference response from XM
     */
    void onBearerPreferenceRes(XpanEvent event) {
        int requester = event.getArg1();
        int bearer = event.getArg2();
        int status = event.getArg3();
        if (VDBG)
            Log.d(TAG, "onBearerPreferenceRes " + XpanConstants.getBearerResString(status));
        if (mTempBearer != bearer) {
            Log.w(TAG, "onBearerPreferenceRes " + mTempBearer + " " + bearer);
        }
        if (status == XpanConstants.BEARER_ACCEPTED) {
            mRemoteReqBearer = bearer;
            mTempBearer = -1;
        }
        sendMessage(XPAN_EVENT, event);
    }

    private void sendBearerPreferenceRes(XpanEvent event) {
        int requester = event.getArg1();
        int bearer = event.getArg2();
        int response = event.getArg3();
        if (VDBG)
            Log.v(TAG, "sendBearerPreferenceRes " + event);
        if (requester == Requestor.EB.ordinal()) {
            mGattSm.sendBearerPreferenceResponse(bearer, response);
        } else if (requester == Requestor.WIFI_VENDOR.ordinal()) {
            mProviderClient.updateWifiTransportPreference(bearer, response);
        }
    }

    private String messageToString(int what) {
        switch (what) {
        case CONNECT_XPAN:
            return "CONNECT_XPAN";
        case DISCONNECT_XPAN:
            return "DISCONNECT_XPAN";
        case ENABLE_SAP:
            return "ENABLE_SAP";
        case DISABLE_SAP:
            return "DISABLE_SAP";
        case CONNECT_SSID:
            return "CONNECT_SSID";
        case CONNECTED_SSID:
            return "CONNECTED_SSID";
        case DISCONNECT_SSID:
            return "DISCONNECT_SSID";
        case DISCONNECT_SSID_AND_GATT:
            return "DISCONNECT_SSID_AND_GATT";
        case PREPARE_AUDIO_BEARER:
            return "PREPARE_AUDIO_BEARER";
        case MOVE_TO_LOW_POWER:
            return "MOVE_TO_LOW_POWER";
        case REQUESTED_SAP_POWER_STATE :
            return "REQUESTED_SAP_POWER_STATE";
        case XPAN_EVENT:
            return "XPAN_EVENT";
        case NEW_BEARER_SWITCH_REQUEST:
            return "NEW_BEARER_SWITCH_REQUEST";
        case SAP_CONNECTION_TIMEOUT:
            return "SAP_CONNECTION_TIMEOUT";
        case BEARER_SWITCHED_INDICATION:
            return "BEARER_SWITCHED_INDICATION";
        case REQ_CLEAR_TO_SEND :
            return "REQ_CLEAR_TO_SEND ";
        case REQ_BEARER_PREFERENCE :
            return "REQ_BEARER_PREFERENCE ";
        case AIRPLANEMODE_DISABLED:
            return "AIRPLANEMODE_DISABLED";
        case AIRPLANEMODE_ENABLED:
            return "AIRPLANEMODE_ENABLED";
        case SSR_WIFI_COMPLETED:
            return "SSR_WIFI_COMPLETED";
        case SSR_WIFI_STARTED:
            return "SSR_WIFI_STARTED";
        case EB_IPV4_NOTIFICATION:
            return "EB_IPV4_NOTIFICATION";
        case REQ_WIFI_SCAN_RESULTS:
            return "REQ_WIFI_SCAN_RESULTS";
        case UPDATE_WIFI_SCAN_RESULTS:
            return "UPDATE_WIFI_SCAN_RESULTS";
        case UPDATE_UDP_PORT:
            return "UPDATE_UDP_PORT";
        case UPDATE_UDP_SYNC_PORT:
            return "UPDATE_UDP_SYNC_PORT";
        case UPDATE_USECASE_IDENTIFIER:
            return "UPDATE_USECASE_IDENTIFIER";
        case SAP_POWER_STATE_RES:
            return "SAP_POWER_STATE_RES";
        case MDNS_QUERY:
            return "MDNS_QUERY";
        case MDNS_REGISTER_UNREGISTER:
            return "MDNS_REGISTER_UNREGISTER";
        case CHANGE_STATE:
            return "CHANGE_STATE";
        default:
            return Integer.toString(what);
        }
    }

    int getState() {
        return mState;
    }

    void logDebug(String msg) {
        if (DBG) {
            Log.d(TAG, msg);
        }
    }

    void updatePlayState(int playState) {
        if (DBG) Log.d(TAG, "updatePlayState " + playState);
        mPlayingState = playState;
    }

    // Provider client APIs

    void updateApDetailsRemote(XpanEb eb) {
        if (DBG)
            Log.d(TAG, "updateApDetailsRemote " + eb);
        int size = eb.getRemoteMacList().size();
        if (size == 0) {
            Log.w(TAG, " ignore updateApDetailsRemote size is 0");
            return;
        }
        mProviderClient.updateApDetailsRemote(eb);
    }

    void updateApDetailsRemote(Message msg) {
        NsdData data = (NsdData) msg.obj;
        if (DBG)
            Log.d(TAG, "updateApDetailsRemote " + data);
        if (data != null) {
            mProviderClient.updateApDetailsRemote(data);
        }
    }

    private boolean updateTwtSessionParams(int rightOffset, int vbc, List<String> listMac,
            List<Integer> listAudioLocation, List<Boolean> listTwtConfigured,
            List<Integer> listSi, List<Integer> listSp) {
        if (VDBG)
            Log.v(TAG, "updateTwtSessionParams");
        return mProviderClient.updateTwtSessionParams(rightOffset, vbc, listMac,
                listAudioLocation, listTwtConfigured, listSi, listSp);
    }

    private boolean updateTransport(boolean isEnabled, int reason) {
        int updatedReason = reason;
        if (mSSRMode) {
            updatedReason = XpanConstants.TP_REASON_SSR_STARTED;
        }
        return mProviderClient.updateTransport(mDevice, isEnabled, updatedReason);
    }

    private boolean updatePrepareAudioBearer(int bearer, int status) {
        if (VDBG) {
            Log.v(TAG, "updatePrepareAudioBearer " + bearer + " " + status + " " + mBearerPrepared
                    + " " + mIsIdle);
        }
        if (mBearerPrepared) {
            if (DBG)
                Log.d(TAG, "Already sent updatePrepareAudioBearer");
            return false;
        }
        mProviderClient.updatePrepareAudioBearer(mDevice, bearer, status);
        if (mIsIdle && bearer == XpanConstants.BEARER_P2P
                && status == XpanConstants.BEARER_ACCEPTED) {
            updateBearerSwitched(bearer, status);
        }
        if (bearer == XpanConstants.BEARER_NONE && !mIsIdle) {
            setIdle();
        } else if (mIsIdle && bearer != XpanConstants.BEARER_AP) {
            resetIdle();
        }
        mBearerPrepared = true;
        return mBearerPrepared;
    }

    void updateBearerSwitched(int bearer, int status) {
        if (VDBG) {
            Log.v(TAG, "updateBearerSwitched " + bearer + " status " + status);
        }
        mProviderClient.updateBearerSwitched(mDevice, bearer, status);
    }

    /*
     * Send Clear to send request to XM
     */
    void updateClearToSendReq(int ctsReq) {
        if (VDBG)
            Log.v(TAG, "updateClearToSendReq " + ctsReq);
        mProviderClient.updateClearToSendReq(mDevice, ctsReq);
    }

    private void connectLeLink() {
        if (VDBG)
            Log.v(TAG, "connectLeLink");
        mProviderClient.connectLeLink(mDevice);
    }

    private class ApSmCallBack implements ApCallback {

        @Override
        public void onApConnectionStateChanged(ApDetails params) {
            if (VDBG) {
                Log.v(TAG, "onApConnectionStateChanged " + params);
            }
            mApDetails = params;
            if (mApDetails != null &&  mReadComplected) {
                sendData(CONNECTED_SSID, null);
            }

            mDatabaseManager.updateDeviceWhcSupportedToDb(mDevice,
                    mApDetails != null && mApDetails.isValid() && !mApDetails.isEnterprise());
        }

        @Override
        public void onWifiScanResults(List<ScanResult> scanResult) {
            if (VDBG)
                Log.v(TAG, "onWifiScanResults");
            sendMessage(UPDATE_WIFI_SCAN_RESULTS, scanResult);
        }
    }

    private void setCodecConfigPreference(int codecType) {
        if (DBG)
            Log.d(TAG, "setCodecConfigPreference " + codecType);
        boolean isCodecSet = mLeAudioProxy.setCodecConfigPreference(mDevice, codecType);
        if (VDBG)
            Log.v(TAG, "setCodecConfigPreference " + isCodecSet);
        if (!isCodecSet && codecType == LeAudioProxy.CODEC_TYPE_APTX_ADAPTIVE_R4) {
            mXpanUtils.cacheDeviceCodec(mDevice, codecType);
        }
    }

    private void handleBearerPreference(Message msg) {
        XpanEvent prefEvent = (XpanEvent) msg.obj;
        int requester, bearer, reason;
        requester = prefEvent.getArg1();
        bearer = prefEvent.getArg2();
        reason = prefEvent.getArg3();

        if (VDBG)
            Log.v(TAG, "handleBearerPreference " + requester + " " + bearer + " " + mPlayingState);
        XpanEvent event = new XpanEvent(XpanEvent.BEARER_PREFERENCE_RES);
        event.setArg1(requester);
        event.setArg2(bearer);
        if (bearer == XpanConstants.BEARER_NON_XPAN || bearer == XpanConstants.BEARER_AP_ASSIST
                || bearer > XpanConstants.BEARER_P2P_PREP) {
            event.setArg3(XpanConstants.BEARER_REJECTED);
            sendBearerPreferenceRes(event);
            if (VDBG)
                Log.v(TAG, "Rejected not supported bearer");
        } else if (bearer == XpanConstants.BEARER_P2P_PREP && mSapSm.isLohsStarted()) {
            event.setArg3(XpanConstants.BEARER_ACCEPTED);
            sendBearerPreferenceRes(event);
            sendMessage(CONNECT_SSID);
            mSapSm.resetLpTimer();
        } else if ((bearer == XpanConstants.BEARER_AP && mApSm.isDisConnected())
                || (bearer == XpanConstants.BEARER_AP
                        && mCurrentTransport == XpanConstants.BEARER_LE && !mEb.isConnected())
                || (mState == AIRPLANE_MODE )
                || (bearer == XpanConstants.BEARER_P2P
                        && (mUseCase == XpanConstants.USECASE_VOICE_CALL ||
                        mUseCase == XpanConstants.USECASE_AP_VOICE_CALL))
                || bearer == XpanConstants.BEARER_AP && !isHsEbConnectedtoAp()) {
            if (VDBG)
                Log.v(TAG, "Rejected");
            event.setArg3(XpanConstants.BEARER_REJECTED);
            sendBearerPreferenceRes(event);
        } else if (!mSapSm.isLohsStarted() && bearer == XpanConstants.BEARER_P2P_PREP) {
            if (VDBG)
                Log.v(TAG, "Rejected Lohs not enabled ");
            event.setArg3(XpanConstants.BEARER_REJECTED);
            sendBearerPreferenceRes(event);
        } else if (bearer == XpanConstants.BEARER_P2P && !mEb.isEbConnectedToSap()) {
            if (VDBG)
                Log.v(TAG, "Rejected Eb not connected to SAP");
            event.setArg3(XpanConstants.BEARER_REJECTED);
            sendBearerPreferenceRes(event);
        } else if ((bearer != XpanConstants.BEARER_AP
                && mCurrentTransport != XpanConstants.BEARER_AP)
                && (((mPlayingState == XpanConstants.GROUP_STREAM_STATUS_IDLE)
                        || (mService.getCurrentUseCase() == XpanConstants.USECASE_NONE))
                        || ((mState == AIRPLANE_MODE) && (bearer == XpanConstants.BEARER_LE)))) {
            if (requester != Requestor.WIFI_VENDOR.ordinal()) {
                mRemoteReqBearer = bearer;
            }
            if (VDBG)
                Log.v(TAG, "Accepted");
            event.setArg3(XpanConstants.BEARER_ACCEPTED);
            sendBearerPreferenceRes(event);
            mDatabaseManager.updateDeviceBearerToDb(mDevice, bearer);
        } else {
            mTempBearer = bearer;
            mProviderClient.updateBearerPreferenceReq(mDevice, requester, bearer, reason, prefEvent.getUrgency());
        }
    }

    void onStartFilterScan(int state) {
        sendMessage(START_FILTERED_SCAN, state);
    }

    void onMdnsQuerry(int state) {
        if (VDBG)
            Log.v(TAG, "onMdnsQuerry " + state);
        sendMessage(MDNS_QUERY, state);
    }

    void onMdnsRegisterUnregister(int state) {
        if (VDBG)
            Log.v(TAG, "onMdnsRegisterUnregister " + state);
        sendMessage(MDNS_REGISTER_UNREGISTER, state);
    }

    void onLinkEstablished(int state) {
        if(VDBG) Log.v(TAG, "onLinkEstablished " + state);
        mLeLinkEstablished = (state == XpanConstants.SUCCESS);
        sendMessage(CB_LE_LINK, state);
    }

    void onCurrentTransport(int transport) {
        mCurrentTransport = ((transport == XpanConstants.BEARER_AP) ? XpanConstants.BEARER_AP
                : XpanConstants.BEARER_LE);
        if (mState == CONNECTED) {
            sendMessage(CB_CURRENT_TRANSPORT, transport);
        }
        if (VDBG)
            Log.v(TAG, "onCurrentTransport " + mCurrentTransport);

        mDatabaseManager.updateDeviceBearerToDb(mDevice, mCurrentTransport);
    }

    private boolean sendData(int code, Message msg) {
        int opcode = -1;
        byte data[] = null;
        boolean isSend = false;

        switch (code) {
        case UPDATE_WIFI_SCAN_RESULTS:
            opcode = XpanGattStateMachine.WIFI_SCAN_RESULT;
            List<ScanResult> scanResult = (List<ScanResult>) msg.obj;
            data = mDataParser.getWifiScanResultBytes(mWifiUtils, scanResult);
            break;

        case ON_CSS:
            opcode = XpanGattStateMachine.CSA;
            XpanEvent event = (XpanEvent) msg.obj;
            data = mDataParser.getCsaBytes(event);
            break;

        case UPDATE_UDP_PORT:
            opcode = XpanGattStateMachine.UPDATE_UDP_PORT;
            data = mDataParser.getUdpPortBytes(mApSm.getUdpPortAudio(), mApSm.getUdpPortReports());
            break;

        case UPDATE_L2CAP_TCP_PORT:
            opcode = XpanGattStateMachine.UPDATE_L2CAP_TCP_PORT;
            data = mDataParser.getL2capTcpPortBytes(mApSm.getL2capTcpPort());
            break;

        case UPDATE_UDP_SYNC_PORT:
            opcode = XpanGattStateMachine.UPDATE_UDP_SYNC_PORT;
            data = mDataParser.getUdpSyncPortBytes(mApSm.getUdpSyncPort());
            break;

        case UPDATE_USECASE_IDENTIFIER:
            opcode = XpanGattStateMachine.UPDATE_USECASE_IDENTIFIER;
            if (mUseCase == XpanConstants.USECASE_VOICE_CALL
                    || mUseCase == XpanConstants.USECASE_AP_VOICE_CALL
                    || mUseCase == XpanConstants.USECASE_AUDIO_STREAMING
                    || mUseCase == XpanConstants.USECASE_LOSSLESS_MUSIC) {
                data = mDataParser.getUseCaseIdentifierBytes(mUseCase,
                        mXpanUtils.getPeriodicity(mUseCase));
            } else {
                /* if no usecase available, assume music streaming */
                data = mDataParser.getUseCaseIdentifierBytes(XpanConstants.USECASE_AUDIO_STREAMING,
                        mXpanUtils.getPeriodicity(XpanConstants.USECASE_AUDIO_STREAMING));
            }
            break;

        case TWT_CONF:
            opcode = XpanGattStateMachine.TWT_CONFIG;
            mUseCase = mService.getCurrentUseCase();
            if (mTwtSetupId > 250) {
                mTwtSetupId = 0;
            }
            data = mDataParser.getTwtParamBytes(
                    mXpanUtils.getTwtParam(mUseCase), ++mTwtSetupId);
            mPendingTwtConf = true;
            mBearerPrepared = false;
            mRightOffset = mXpanUtils.getRightOffSet(mUseCase);
            break;

        case TWT_EMPTY:
            opcode = XpanGattStateMachine.TWT_EMPTY;
            if (mPendingTwtConf) {
                mPendingTwtConf = false;
                data = mDataParser.getEmptyByteData();
            }
            int bearer = msg.arg1;
            mBearerPrepared = false;
            if (bearer == XpanConstants.BEARER_NONE) {
                updatePrepareAudioBearer(bearer, XpanConstants.BEARER_STATUS_SUCESS);
            }
            break;

        case CONNECT_SSID:
            opcode = XpanGattStateMachine.CONNECT_SSID;
            String ssid = mWifiUtils.getSsid();
            MacAddress bssid = mWifiUtils.getBssid();
            String passphrase = mWifiUtils.getPassPharse();
            int frequency = mSapSm.getFrequency();
            int secMode = mWifiUtils.getSecMode();
            String countryString = mWifiUtils.getFormatedCountryCode();
            if (ssid == null || bssid == null || passphrase == null || frequency == 0) {
                Log.w(TAG, "sendData CONNECT_SSID Incorrect config " + ssid + " bssid " + bssid
                        + " passphrase " + passphrase + " frequency " + frequency);
                return false;
            }
            if (VDBG) Log.v(TAG, "sendData CONNECT_SSID " + ssid + " " + bssid
                    + " " + passphrase + " " + frequency
                    + " " + countryString + " " + secMode);

            data = mDataParser.getConnectSsidBytes(ssid, bssid, passphrase,
                    frequency, secMode, countryString);
            break;

        case CONNECTED_SSID:
            opcode = XpanGattStateMachine.CONNECTED_SSID;
            if (mApDetails != null && mApDetails.isValid()) {
                mWifiUtils.isUpdateStationFrequency(mApDetails);
                data = mDataParser.getConnectedSsidBytes(mWifiUtils, mApDetails);
            } else {
                logDebug("CONNECTED_SSID ignore " + mApDetails);
            }
            break;

        case DISCONNECT_SSID_AND_GATT:
            opcode = XpanGattStateMachine.DISCONNECT_SSID_AND_GATT;
            if (mGattSm.isConnSsidSent() && !TextUtils.isEmpty(getCurrentSsid())) {
                data = mDataParser.getDisConnectBytes(getCurrentSsid());
            } else {
                mGattSm.disconnect();
            }
            break;

        case DISCONNECT_SSID:
            opcode = XpanGattStateMachine.DISCONNECT_SSID;
            if (mGattSm.isConnSsidSent() && !TextUtils.isEmpty(getCurrentSsid())) {
                data = mDataParser.getDisConnectBytes(getCurrentSsid());
            }
            break;

        case SAP_POWER_STATE_RES:
            opcode = XpanGattStateMachine.POWER_STATE_RESPONSE;
            data = mDataParser.getPowerStateResBytes(msg.arg1);
            break;

        case UPDATE_BEACON_INTERVAL:
            opcode = XpanGattStateMachine.UPDATE_BEACON_INTERVAL;
            if (msg != null) {
                XpanEvent beaconData = (XpanEvent) msg.obj;
                data = mDataParser.getBeaconParameterBytes(beaconData.getTbtTsf(),
                        beaconData.getArg1(), mSapSm.getFrequency());
            } else if (mSendBeacon && mBeacondata != null) {
                data = mBeacondata;
            } else {
                Log.w(TAG," Ignore UPDATE_BEACON_INTERVAL");
            }
            break;

        case UPDATE_SAP_STATE:
            opcode = XpanGattStateMachine.UPDATE_SAP_STATE;
            data = mDataParser.getSapStateBytes(msg.arg1, msg.arg2);
            break;

        case CB_BEARER_SWITCH_FAILED:
            opcode = XpanGattStateMachine.UPDATE_BEARER_SWITCH_FAILED;
            data = mDataParser.getBearerSwitchFailedBytes(msg.arg1, msg.arg2);
            break;

        }
        if (data != null) {
            isSend = true;
            mGattSm.sendData(opcode, data);
        } else {
            Log.w(TAG, "sendData ignore " + opcode);
        }
        return isSend;
    }

    private class GattCallback implements GattResponseCallback {

        @Override
        public void onAudioBearerSwitchResponse(int bearer, int status) {
            logDebug("onAudioBearerSwitchResponse " + bearer + ", status " + status);
            XpanEvent event = new XpanEvent(XpanEvent.BEARER_SWITCH);
            event.setArg1(bearer);
            event.setArg2(status);
            sendMessage(XPAN_EVENT, event);
        }

        @Override
        public void onIpv4NotificationReceived(List<RemoteMacAddress> listMac) {
            sendMessage(EB_IPV4_NOTIFICATION, listMac);
        }

        @Override
        public void onIpv6NotificationReceived(List<RemoteMacAddress> listMac) {
            sendMessage(EB_IPV6_NOTIFICATION, listMac);
        }

        @Override
        public void onRequestedSapPowerState(int powerstate) {
            logDebug("onPowerStateRequest " + powerstate);
            if (powerstate != XpanConstants.REQ_SAP_POWER_STATE_ACTIVE) {
                sendData(SAP_POWER_STATE_RES,
                        mXpanUtils.getMessage(XpanConstants.POWER_STATE_RES_REJECT));
                return;
            }
            sendMessage(REQUESTED_SAP_POWER_STATE, powerstate);
        }

        /*
         * Received bearer preference from peer
         */
        @Override
        public void onReqBearerPreference(Requestor requester, int bearer, int urgency) {
            if (VDBG) Log.v(TAG, "onReqBearerPreference "
                    + XpanConstants.getBearerString(bearer));
            XpanEvent event = getBearerPrefEvent(XpanEvent.BEARER_PREFERENCE,
                    requester.ordinal(), bearer, XpanConstants.TP_SWITCH_REASON_UNSPECIFIED, urgency);
            sendMessage(REQ_BEARER_PREFERENCE, event);
        }

        /*
         * Received Clear To Send from peer
         */
        @Override
        public void onReqClearToSend(int ctsReq) {
            if (VDBG)
                Log.v(TAG, "reqClearToSend " + ctsReq);
            sendMessage(REQ_CLEAR_TO_SEND, ctsReq);
        }

        @Override
        public void onReqWifiScanResult() {
            if (VDBG)
                Log.v(TAG, "onReqWifiScanResult ");
            sendMessage(REQ_WIFI_SCAN_RESULTS);
        }

        @Override
        public void onWifiScanResultUpdated() {
            if (DBG)
                Log.d(TAG, "onWifiScanResultUpdated " + mApDetails);
            if (mApDetails != null) {
                sendMessage(CONNECTED_SSID);
            }
        }

        @Override
        public void onL2capTcpPortUpdated() {
            sendMessage(UPDATE_UDP_PORT);
        }

        @Override
        public void onUdpPortUpdated() {
            sendMessage(UPDATE_UDP_SYNC_PORT);
        }

        @Override
        public void onPortNumbersReceived(int portL2cap, int portUdpAudio, int portUdpReports) {
            if (VDBG)
                Log.v(TAG, "onPortNumbersReceived " + portL2cap + " " + portUdpAudio + " "
                        + portUdpReports);
            mEb.setPortL2capTcp(portL2cap);
            mEb.setPortUdpAudio(portUdpAudio);
            mEb.setPortUdpReports(portUdpReports);
        }

        @Override
        public void onBearerPreferenceResponseSent(int bearer) {
            if (bearer == XpanConstants.BEARER_AP &&
                mPlayingState == XpanConstants.GROUP_STREAM_STATUS_STREAMING) {
                sendMessage(UPDATE_USECASE_IDENTIFIER);
            }
        }

        @Override
        public void onPowerStateResponseUpdated() {
            if(mSendBeacon) {
                sendData(UPDATE_BEACON_INTERVAL, null);
                mSendBeacon = false;
            }
        }

        @Override
        public void onWifiChannelSwitchReq(int channel) {
            sendMessage(WIFI_CHANNEL_SWITCH_REQ, channel);
        }

        @Override
        public void onSendPortNumbers() {
            if (VDBG)
                Log.v(TAG, "onSendPortNumbers");
            onPortNumbers();
        }

        @Override
        public void onGattConnnected() {
            setIdle();
        }

        @Override
        public void onGattReadStatus(boolean status, int bearerPref) {
            mRemoteReqBearer = bearerPref;
            mReadComplected = status;
            if (VDBG) {
                Log.v(TAG, "onGattReadStatus " + status + " " + mRemoteReqBearer);
            }
            if (status && mApSm.isConnected()) {
                sendData(CONNECTED_SSID, null);

                mDatabaseManager.updateDeviceWhcSupportedToDb(mDevice,
                    mApDetails != null && mApDetails.isValid() && !mApDetails.isEnterprise());
            }
        }

        @Override
        public void onSapClientsStateChange(int loc, OpCode type) {

            MacAddress mac;
            List<MacAddress> list = new ArrayList<MacAddress>();
            if ((loc & XpanConstants.LOC_LEFT) != XpanConstants.LOC_LEFT) {
                mac = mEb.getMac(XpanConstants.LOC_LEFT);
                if (mac != null) {
                    RemoteMacAddress macAddr = mEb.getMac(mac);
                    if ((type == XpanProviderClient.OpCode.DISCONNECT_SAP_CLIENTS
                            && macAddr.isConnected())) {
                        macAddr.setConnected(false);
                        list.add(mac);
                    }
                    if (type == XpanProviderClient.OpCode.TERMINATE_TWT
                                    && macAddr.isTwtConfigured()) {
                        macAddr.setTwtConfigured(false, 0, 0);
                        list.add(mac);
                    } else {
                        if (VDBG)
                            Log.v(TAG, "onSapClientsStateChange ignore " + macAddr);
                    }
                }
            }
            if ((loc & XpanConstants.LOC_RIGHT) != XpanConstants.LOC_RIGHT) {
                mac = mEb.getMac(XpanConstants.LOC_RIGHT);
                if (mac != null) {
                    RemoteMacAddress macAddr = mEb.getMac(mac);
                    if ((type == XpanProviderClient.OpCode.DISCONNECT_SAP_CLIENTS
                            && macAddr.isConnected())) {
                        macAddr.setConnected(false);
                        list.add(mac);
                    }
                    if (type == XpanProviderClient.OpCode.TERMINATE_TWT
                                    && macAddr.isTwtConfigured()) {
                        macAddr.setTwtConfigured(false, 0, 0);
                        list.add(mac);
                    } else {
                        if (VDBG)
                            Log.v(TAG, "onSapClientsStateChange ignore " + macAddr);
                    }
                }

            }
            if (list.size() == 1) {
                list.add(MacAddress.fromString(XpanConstants.MAC_DEFAULT));
                if (VDBG) {
                    Log.v(TAG, "onSapClientsStateChange updated ");
                }

            }
            if (VDBG) {
                Log.v(TAG, "onSapClientsStateChange " + list);
            }

            if (list.size() > 0) {
                mProviderClient.updateSapClientsStateChange(list, type);
                boolean disconnect = true;
                for (RemoteMacAddress remoteMac : mListRemoteMac) {
                    if (remoteMac.isConnected()) {
                        disconnect = false;
                        break;
                    }
                }

                if (disconnect && type == XpanProviderClient.OpCode.DISCONNECT_SAP_CLIENTS)
                    updateTransport(false, XpanConstants.TP_REASON_DISCONNECT);
            }
        }

        @Override
        public void onPortCofigEnableSapOnAp() {
            if (mSapSm.isSapDisabled()) {
                sendMessage(ENABLE_SAP);
            }
        }

        @Override
        public void onReqSendPortUpdates() {
            sendMessage(UPDATE_UDP_PORT);
        }
    }

    private void startOrStopScan(int type) {
        if (VDBG)
            Log.v(TAG, "startOrStopScan " + type);
        if (type == XpanConstants.DISCOVERY_START) {
            mService.startScan(mDevice);
        } else if (type == XpanConstants.DISCOVERY_STOP) {
            mService.stopScan(mDevice);
        }
    }

    void onScanResult() {
        if (DBG)
            Log.d(TAG, "onScanResult ");
        sendMessage(CB_SCAN_RESULT_BT);
    }

    private void updateMdnsQuery(Message message) {
        NsdMsg msgQuery = (message.arg1 == XpanConstants.DISCOVERY_START) ? NsdMsg.DISCOVERY_START
                : NsdMsg.DISCOVERY_STOP;
        mNsdHelper.updateMessage(mDevice, msgQuery);
    }

    private void updateMdnsRegisterUnregister(Message message) {
        NsdMsg msg = (message.arg1 == XpanConstants.MDNS_REGISTER) ? NsdMsg.REGISTER
                : NsdMsg.UNREGISTER;
        mNsdHelper.updateMessage(mDevice, msg);
    }

    private void processBearerAp() {
        if (mApDetails == null || mApSm.getUdpSyncPort() <= 0) {
            updatePrepareAudioBearer(mBearer, XpanConstants.BEARER_STATUS_FAILURE);
        } else {
            updatePrepareAudioBearer(mBearer, XpanConstants.BEARER_STATUS_SUCESS);
        }
    }

    /**
     * @return the mListRemoteMac
     */
    List<RemoteMacAddress> getListRemoteMac() {
        return mListRemoteMac;
    }

    private void updateConnectedEbDetails() {
        String left = XpanConstants.MAC_DEFAULT;
        String right = XpanConstants.MAC_DEFAULT;
        for (RemoteMacAddress mac : mListRemoteMac) {
            if (mac.isDisConnected()) {
                continue;
            }
            if (mac.getAudioLocation() == XpanConstants.LOC_LEFT) {
                left = mac.getMacAddress().toString().toUpperCase();
            } else if (mac.getAudioLocation() == XpanConstants.LOC_RIGHT) {
                right = mac.getMacAddress().toString().toUpperCase();
            }
        }
        mProviderClient.updateConnectedEbDetails(mSetId, left, right);
    }

    boolean isStreamingOnSap() {
        return (mState == TWT_CONFIGURED || mState == BEARER_ACTIVE);
    }

    private void startWifiScan() {
        if (mCurrentTransport != XpanConstants.BEARER_AP && mApSm.isConnected()) {
            mProviderClient.updateWifiScanStarted(mDevice, XpanConstants.SUCCESS);
        }
        mApSm.startScan(mDevice);
    }

    private void startLPTimer() {
        if (!mXpanUtils.isLowpowerEnable()) {
            Log.w(TAG, "Ignore startLPTimer");
            return;
        }
        removeMessages(MOVE_TO_LOW_POWER);
        int timeOut;
        if (mIsInterMediateLowPower) {
            timeOut = XpanConstants.INTERMEDIATE_ACTIVE_DURATION;
            mIsInterMediateLowPower = false;
        } else {
            timeOut = mXpanUtils.getLowPowerInterval();
        }
        mSapSm.acquireWakeLock();
        sendMessageDelayed(MOVE_TO_LOW_POWER, mXpanUtils.secToMillieSec(timeOut));
        if (VDBG)
            Log.v(TAG, "startLPTimer " + timeOut);
    }

    private void handleLeLinkStatus(int status) {
        if (status == XpanConstants.FAILURE && mCurrentTransport == XpanConstants.BEARER_AP) {
            startOrStopScan(XpanConstants.DISCOVERY_START);
        }
    }

    void handleSapIfUpdate() {
        if (VDBG)
            Log.v(TAG, "handleSapIfUpdate ");
        if (mGattSm.isConnected()) {
            if (hasMessages(ENABLE_SAP)) {
                removeMessages(ENABLE_SAP);
            }
            connectXpan();
        }
    }

    XpanEvent getBearerPrefEvent(int type, int arg1, int arg2, int arg3, int urgency) {
        XpanEvent event = new XpanEvent(type);
        event.setArg1(arg1);
        event.setArg2(arg2);
        event.setArg3(arg3);
        event.setUrgency(urgency);
        return event;
    }

    private String getCurrentSsid() {
        return mWifiUtils.getSsid();
    }

    private void setIdle() {
        if (VDBG)
            Log.v(TAG, "setIdle");
        mIsIdle = true;
    }

    private void resetIdle() {
        if (VDBG)
            Log.v(TAG, "resetIdle");
        mIsIdle = false;
    }

    private boolean isHsEbConnectedtoAp() {
        return mApDetails != null && mEb.isConnected()
                && mApDetails.getSsid().equals(mEb.getEbSSID());
    }

    private void transitionToState(int state) {
        if (DBG)
            Log.d(TAG, "transitionToState " + mState + " -> " + state);
        if (state == CONNECTED
                && (mState == DISCONNECTED || mState == CONNECTING || mState == DISCONNECTING)) {
            if (mSapSm.isLowPower()) {
                transitionTo(mLowPower);
            } else {
                transitionTo(mConnected);
            }
        } else if (state == DISCONNECTED
                && (mState == BEARER_ACTIVE || mState == TWT_CONFIGURED || mState == CONNECTED
                || mState == LOW_POWER)) {
            transitionTo(mDisconnected);
        }
    }

  }
