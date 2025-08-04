/*****************************************************************
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 ******************************************************************/

package vendor.qti.bluetooth.xpan;

import static android.Manifest.permission.BLUETOOTH_CONNECT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeAudio;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.MacAddress;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import vendor.qti.bluetooth.xpan.LeAudioProxy.LeProxyListener;
import vendor.qti.bluetooth.xpan.XpanConstants.Requestor;
import vendor.qti.bluetooth.xpan.XpanNsdHelper.NsdMsg;

/**
 * Provides Xpan Profile Service, as a service in the Xpan APK.
 *
 * @hide
 */

public class XpanProfileService extends Service implements LeProxyListener {

    private static final String TAG = "XpanProfile";
    private static boolean VDBG = XpanUtils.VDBG;
    private static boolean DBG = XpanUtils.DBG;

    private XpanProviderClient mProviderClient;

    private Context mCtx;

    // Upper limit on XPAN devices: Bonded & Connected
    private final int MAX_XPAN_STATE_MACHINES = 10;

    private final int MSG_XPAN_SERVICE_INIT = 1;
    private final int MSG_HANDLE_UNBONDED_DEVICE = 2;
    private final int MSG_HANDLE_LE_AUDIO_CONNECTED = 3;
    private final int MSG_HANDLE_LE_AUDIO_DISCONNECTED = 4;
    private final int MSG_HANDLE_DUT_CODEC_R4_NOT_SUPPORTED = 5;
    static final int CB_UPDATE_USECASE = 6;
    static final int CB_PREPARE_AUDIO_BEARER = 7;
    static final int CB_TWT_SESSION = 8;
    static final int CB_ACS_UPDATE = 9;
    static final int CB_SAP_BEACON_INTERVALS = 10;
    static final int CB_UPDATE_SAP_INTERFACE = 11;
    static final int MSG_STREAM_STATE_CHANGED = 12;
    static final int CB_XPAN_DEVICE_FOUND = 13;
    static final int CB_BEARER_SWITCH_INDICATION = 14;
    static final int CB_BEARER_PREFERENCE_RES = 15;
    private final int MSG_INIT_PENDING_CONNECTIONS = 16;
    private static final int MSG_STATE_OFF = 17;
    private final int MSG_STATE_ON = 18;
    private final int MSG_XPAN_SERVICE_CLEAR = 19;
    private final int MSG_UPDATE_CODEC_DEFAULT = 20;
    static final int CB_WIFI_TRANSPORT_PREF = 21;
    static final int CB_SSR_WIFI = 22;
    static final int CB_MDNS_QUERY = 23;
    static final int CB_MDNS_REGISTER_UNREGISTER = 24;
    static final int CB_START_FILTERED_SCAN = 25;
    static final int CB_LE_LINK_ESTABLISHED = 26;
    static final int CB_PORT_NUMBERS = 27;
    static final int CB_CURRENT_TRANSPORT = 28;
    static final int CB_ROLE_SWITCH_INDICATION = 29;
    static final int CB_INIT_FAILED = 30;
    static final int CB_SCAN_RESULT_BT = 31;
    static final int CB_POWER_STATE_RES = 32;
    static final int CB_CSS = 33;
    static final int CB_WIFI_DRIVER_STATUS = 34;
    static final int CB_COUNTRY_CODE_UPDATE = 35;
    static final int CB_SAP_IF_UPDATE = 36;
    static final int SAP_IF_UPDATE = 37;
    private final int MSG_AIRPLANE_STATE_CHANGE = 38;
    static final int CB_BEARER_SWITCH_FAILED = 39;

    private XpanHandler mXpanHandler;

    private HandlerThread mXpanHandlerThread;
    private boolean mLeProxyConnected = false;
    private boolean mIsStart = false, mPendingWifiDriverInit = false;
    private boolean mIsWSsrInProgress = false;

    private final ConcurrentMap<BluetoothDevice, XpanGattStateMachine> mGattStateMachines =
            new ConcurrentHashMap<>();

    private final ConcurrentMap<BluetoothDevice, XpanConnectionStateMachine> mConnStateMachines
            = new ConcurrentHashMap<>();

    private final List<BluetoothDevice> mListQllSupport = new ArrayList<BluetoothDevice>();

    private HashMap<BluetoothDevice, BluetoothDevice> mIdetityDevicesMap =
            new HashMap<BluetoothDevice, BluetoothDevice>();

    private CopyOnWriteArrayList<BluetoothDevice> mPendingPrepareAudioBearer =
            new CopyOnWriteArrayList<BluetoothDevice>();

    private List<BluetoothDevice> mPreviousXpanDevices =
            new ArrayList<BluetoothDevice>();

    private XpanSapStateMachine mXpanSapSm;

    private XpanApStateMachine mApSm;

    private XpanUtils mXpanUtils;

    private LeAudioProxy mLeAudioProxy;

    private static XpanProfileService sService;

    private int mUsecase = XpanConstants.USECASE_NONE;

    private BluetoothDevice mDevicePlaying;

    private XpanScanner mScanner;

    private XpanNsdHelper mNsdHelper;
    private Looper mLooper = null;

    private XpanDatabaseManager mDatabaseManager;
    public XpanDatabaseManager getDatabaseManager() {
        return mDatabaseManager;
    }

    /**
     * Broadcast receiver in XpanProfileService to handle Bluetooth Events.
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TextUtils.isEmpty(action)) {
                if (DBG)
                    Log.d(TAG, "action empty " + action);
                return;
            }
            BluetoothDevice device = intent
                    .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            switch (action) {
            case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
                        BluetoothDevice.ERROR);
                if (VDBG)
                    Log.v(TAG, "bondState " + XpanConstants.getBondStrFromState(bondState)
                        + " device " + device);
               if (bondState == BluetoothDevice.BOND_NONE) {
                    mXpanHandler.sendMessage(mXpanHandler
                            .obtainMessage(MSG_HANDLE_UNBONDED_DEVICE,
                                XpanConstants.TP_REASON_UNPAIRED,-1, device));
                }
                break;
            case BluetoothLeAudio.ACTION_LE_AUDIO_CONNECTION_STATE_CHANGED:
                int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
                if (DBG)
                    Log.d(TAG, "LE_AUDIO_CONNECTION_STATE_CHANGED "
                            + XpanConstants.getConnStrFromState(state));
                if (state == BluetoothProfile.STATE_CONNECTED && !mIsWSsrInProgress) {
                    mXpanHandler.sendMessage(mXpanHandler
                            .obtainMessage(MSG_HANDLE_LE_AUDIO_CONNECTED, device));
                } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                    mXpanHandler.sendMessage(mXpanHandler
                            .obtainMessage(MSG_HANDLE_LE_AUDIO_DISCONNECTED,
                                    XpanConstants.TP_REASON_CONNECTION_FAILURE, -1,device));
                    mDatabaseManager.updateDeviceBearerToDb(device, null);
                }
                break;

            case Intent.ACTION_AIRPLANE_MODE_CHANGED :
                boolean airplaneModeOn = mXpanUtils.isAirplaneModeOn();
                if (DBG) {
                    boolean airplaneMode = intent.getBooleanExtra("state", false);
                    Log.d(TAG, "ACTION_AIRPLANE_MODE_CHANGED " + airplaneMode + " "
                            + airplaneModeOn);
                }
                mXpanHandler.sendMessage(mXpanHandler.obtainMessage(MSG_AIRPLANE_STATE_CHANGE));

                break;

            case BluetoothAdapter.ACTION_STATE_CHANGED:
                int btstate = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                if (DBG)
                    Log.d(TAG, "ACTION_STATE_CHANGED " + btstate);
                if (btstate == BluetoothAdapter.STATE_TURNING_OFF) {
                    mXpanHandler.sendMessage(mXpanHandler.obtainMessage(MSG_STATE_OFF));
                } else if(btstate == BluetoothAdapter.STATE_ON) {
                    sendOnState();
                }
                break;

            case Intent.ACTION_USER_SWITCHED:
                int userId = intent.getIntExtra(Intent.EXTRA_USER_HANDLE, -1);
                if (DBG)
                    Log.d(TAG, "ACTION_USER_SWITCHED " + userId);
                break;

            case BluetoothLeAudio.ACTION_LE_AUDIO_ACTIVE_DEVICE_CHANGED:
                if (DBG)
                    Log.d(TAG, "LE_AUDIO_ACTIVE_DEVICE_CHANGED " + device);
                if (mLeAudioProxy != null) {
                    mLeAudioProxy.setActiveDevice(device);
                }
                if (device == null) {
                    return;
                }
                if (isQllSupport(device)) {
                    if (mXpanUtils.isAirplaneModeOn()) {
                        mXpanHandler.sendMessage(
                                mXpanHandler.obtainMessage(MSG_UPDATE_CODEC_DEFAULT, device));
                    } else {
                        updateCodecToR4(device);
                    }
                }
                break;

            default:
                if (DBG)
                    Log.d(TAG, action + " not handled ");
                break;
            }
        }
    };

    /**
     * XpanHandler to handle tasks in Xpan thread context
     */
    private class XpanHandler extends Handler {
        private XpanHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (DBG)
                Log.d(TAG, "handleMessage " + msg.what);
            BluetoothDevice device = null;

            switch (msg.what) {
            case MSG_XPAN_SERVICE_INIT:
                init();
                break;

            case MSG_XPAN_SERVICE_CLEAR:
                clear();
                break;

            case MSG_INIT_PENDING_CONNECTIONS:
               initiatePendingConnections();
                break;

            case MSG_HANDLE_LE_AUDIO_CONNECTED:
                device = (BluetoothDevice) msg.obj;
                openGattConnection(device);
                break;

            case MSG_HANDLE_UNBONDED_DEVICE:
            case MSG_HANDLE_LE_AUDIO_DISCONNECTED:
            case MSG_HANDLE_DUT_CODEC_R4_NOT_SUPPORTED:
              device = (BluetoothDevice) msg.obj;
              int reason = msg.arg1;
                if (msg.what == MSG_HANDLE_UNBONDED_DEVICE) {
                    mDatabaseManager.removeDeviceFromDb(device);
                }
                if (device != null) {
                    closeGattConnection(device, reason);
                }
                break;

            case CB_UPDATE_USECASE:
                device = (BluetoothDevice) msg.obj;
                device = mLeAudioProxy.getActiveDevice();
                mUsecase = msg.arg1;
                if (device == null) {
                    if (DBG)
                        Log.w(TAG, "CB_UPDATE_USECASE no active device");
                    return;
                }
                updateToConnSm(device, mUsecase, CB_UPDATE_USECASE, -1);
                break;

            case CB_PREPARE_AUDIO_BEARER:
                device = (BluetoothDevice) msg.obj;
                //device = getIdentityDevice(device);
                int bearer = msg.arg1;
                updateToConnSm(device, bearer, CB_PREPARE_AUDIO_BEARER, -1);
                break;

            case CB_TWT_SESSION:
                XpanEvent event = (XpanEvent) msg.obj;
                updateTwtSession(event.getRemoteMacAddr(), event.getArg1(),
                        event.getArg2(), event.getArg3());
                break;

            case CB_ACS_UPDATE:
                if (mXpanSapSm != null) {
                    mXpanSapSm.onFrequencyUpdated(msg.arg1);
                }
                break;

            case CB_SAP_BEACON_INTERVALS:
                XpanEvent lowPowerEvent = (XpanEvent) msg.obj;
                int dialogId = lowPowerEvent.getArg1();
                int powerSaveBi = lowPowerEvent.getArg2();
                long nexttbttsf = lowPowerEvent.getTbtTsf();
                if (mXpanSapSm != null) {
                    mXpanSapSm.onBeaconIntervalsReceived(dialogId, powerSaveBi, nexttbttsf);
                }
                break;

            case CB_BEARER_SWITCH_INDICATION:
                device = (BluetoothDevice) msg.obj;
                updateToConnSm(device,  msg.arg2, CB_BEARER_SWITCH_INDICATION, msg.arg1);
                break;

            case CB_UPDATE_SAP_INTERFACE:
                mXpanSapSm.onUpdateSapInterface(msg.arg1, msg.arg2);
                break;

            case MSG_STREAM_STATE_CHANGED:
                int streamState = msg.arg1;
                device = (BluetoothDevice) msg.obj;
                if (DBG)
                    Log.d(TAG, "MSG_STREAM_STATE_CHANGED " + device + " "
                            + XpanConstants.getStreamingString(streamState));
                XpanConnectionStateMachine connSm = getConnectionStateMachine(device);
                if (connSm != null) {
                    updateToConnSm(device, streamState, MSG_STREAM_STATE_CHANGED, -1 );
                } else if (streamState == XpanConstants.GROUP_STREAM_STATUS_STREAMING) {
                    mDevicePlaying = device;
                } else {
                    mDevicePlaying = null;
                }
                break;

            case CB_XPAN_DEVICE_FOUND:
                device = (BluetoothDevice) msg.obj;
                if (!mListQllSupport.contains(device)) {
                    mListQllSupport.add(device);
                    XpanGattStateMachine gattSm = getGattStateMachine(device);
                    if (gattSm == null && mLeAudioProxy.isConnected(device)) {
                        mXpanHandler.sendMessage(mXpanHandler
                                .obtainMessage(MSG_HANDLE_LE_AUDIO_CONNECTED, device));
                    }
                }
                break;

            case CB_BEARER_PREFERENCE_RES:
                event = (XpanEvent) msg.obj;
                onBearerPreferenceRes(event);
                break;

            case CB_SSR_WIFI :
                mIsWSsrInProgress = (msg.arg1 == XpanConstants.WIFI_SSR_STARTED) ? true : false;
                if(mXpanSapSm.isSapDisabled() && !mIsWSsrInProgress) {
                    connectAfterApOrWssrDisable();
                } else {
                    mXpanSapSm.onSsrWifi(msg.arg1);
                }
                break;

            case MSG_STATE_OFF :
                mXpanUtils.stopService(mCtx);
                stopSelf();
                break;

            case MSG_STATE_ON:
                mPreviousXpanDevices = mXpanUtils.loadXpanDevices();
                if (!mPreviousXpanDevices.isEmpty() && mProviderClient != null) {
                    mProviderClient.updateXpanBondedDevices(mPreviousXpanDevices);
                }
                break;

            case MSG_UPDATE_CODEC_DEFAULT:
                device = (BluetoothDevice) msg.obj;
                if (mLeAudioProxy.isR4CodecSupport(device)) {
                    mXpanUtils.cacheDeviceCodec(device, LeAudioProxy.CODEC_TYPE_DEFAULT);
                    mLeAudioProxy.setCodecConfigPreference(device, LeAudioProxy.CODEC_TYPE_DEFAULT);
                }
                break;
              
            case CB_MDNS_QUERY:
                device = (BluetoothDevice) msg.obj;
                updateToConnSm(device, msg.arg1, CB_MDNS_QUERY, -1);
                break;

            case CB_MDNS_REGISTER_UNREGISTER:
                device = (BluetoothDevice) msg.obj;
                updateToConnSm(device, msg.arg1, CB_MDNS_REGISTER_UNREGISTER, -1);
                break;

            case CB_START_FILTERED_SCAN:
                device = (BluetoothDevice) msg.obj;
                updateToConnSm(device, msg.arg1, CB_START_FILTERED_SCAN, -1);
                break;

            case CB_LE_LINK_ESTABLISHED:
                device = (BluetoothDevice) msg.obj;
                updateToConnSm(device, msg.arg1, CB_LE_LINK_ESTABLISHED, -1);
                break;

            case CB_PORT_NUMBERS:
                mApSm.onPortNumbers((XpanEvent) msg.obj);
                mConnStateMachines.forEach((k,v)->v.onPortNumbers());
                break;

            case CB_CURRENT_TRANSPORT:
                device = (BluetoothDevice) msg.obj;
                updateToConnSm(device, msg.arg1, CB_CURRENT_TRANSPORT, -1);
                break;

            case CB_ROLE_SWITCH_INDICATION:
                device = (BluetoothDevice) msg.obj;
                updateToConnSm(device, msg.arg1, CB_ROLE_SWITCH_INDICATION, -1);
                break;

            case CB_INIT_FAILED:
                mXpanUtils.stopService(mCtx);
                stopSelf();
                break;

            case CB_WIFI_TRANSPORT_PREF:
                bearer = msg.arg1;
                device = mLeAudioProxy.getActiveDevice();
                connSm = getConnectionStateMachine(device);
                if (connSm != null) {
                    connSm.onBearerPreference(Requestor.WIFI_VENDOR, bearer, msg.arg2);
                } else {
                    Log.w(TAG, "CB_WIFI_TRANSPORT_PREF ignore");
                }
                break;

            case CB_POWER_STATE_RES:
                device = (BluetoothDevice) msg.obj;
                connSm = getConnectionStateMachine(device);
                if (connSm != null) {
                    connSm.onPowerStateRes(msg.arg1);
                } else {
                    Log.w(TAG, "CB_POWER_STATE_RES ignore");
                }
                break;

            case CB_CSS:
                event = (XpanEvent) msg.obj;
                mXpanSapSm.onSapChannelSwitchStarted(event);
                break;

            case CB_WIFI_DRIVER_STATUS:
                mXpanSapSm.onWifiDrvStatus(msg.arg1);
                if (mPendingWifiDriverInit && mXpanSapSm.isWifiDrvierReady()) {
                    mPendingWifiDriverInit = false;
                    initiatePendingConnections();
                }
                break;

            case CB_COUNTRY_CODE_UPDATE:
                String country = (String) msg.obj;
                mXpanSapSm.onCountryCodeUpdate(country);
                break;

            case CB_SAP_IF_UPDATE:
                device = mLeAudioProxy.getActiveDevice();
                if (device == null) {
                    if (DBG)
                        Log.w(TAG, "CB_SAP_IF_UPDATE ignore");
                    return;
                }
                if (mXpanHandler.hasMessages(SAP_IF_UPDATE)) {
                    mXpanHandler.removeMessages(SAP_IF_UPDATE);
                }
                mXpanHandler.sendMessageDelayed(mXpanHandler.obtainMessage(SAP_IF_UPDATE),
                        XpanConstants.DURATION_SAP_CONCURRENCY);
                break;

            case MSG_AIRPLANE_STATE_CHANGE:
                if (isPendingApStateChange()) {
                    mXpanHandler.removeMessages(MSG_AIRPLANE_STATE_CHANGE);
                }
                if (mXpanSapSm.isPendingApiCb()) {
                    mXpanHandler.sendMessageDelayed(
                            mXpanHandler.obtainMessage(MSG_AIRPLANE_STATE_CHANGE),
                            XpanConstants.DURATION_ONE_SECOND);
                } else {
                    sendAirPlaneModeChange();
                }
                break;

            case SAP_IF_UPDATE:
                mXpanSapSm.onSapInterfaceUpdate();
                break;

            case CB_BEARER_SWITCH_FAILED:
                device = (BluetoothDevice) msg.obj;
                updateToConnSm(device, msg.arg1, CB_BEARER_SWITCH_FAILED, msg.arg2);
                break;

            default:
                if (DBG)
                    Log.d(TAG, "handleMessage not handled ");
                break;
            }
        }
    }

    private void updateToConnSm(BluetoothDevice device, int state, int opCode,
            int bearerType) {
        XpanConnectionStateMachine connSm = getConnectionStateMachine(device);
        if (connSm == null && (opCode == CB_MDNS_QUERY
                || opCode == CB_MDNS_REGISTER_UNREGISTER
                || (opCode == CB_CURRENT_TRANSPORT && state == XpanConstants.BEARER_AP))) {
            getOrCreateGattStateMachine(device);
            connSm = getConnectionStateMachine(device);
        }
        if (connSm == null) {
            Log.w(TAG, "updateToConnSm " + device + " state " + state + " opcode " + opCode);
            return;
        }
        switch (opCode) {
        case CB_MDNS_QUERY:
            connSm.onMdnsQuerry(state);
            break;
        case CB_MDNS_REGISTER_UNREGISTER:
            connSm.onMdnsRegisterUnregister(state);
            break;
        case CB_START_FILTERED_SCAN:
            connSm.onStartFilterScan(state);
            break;
        case CB_LE_LINK_ESTABLISHED:
            connSm.onLinkEstablished(state);
            break;
        case CB_CURRENT_TRANSPORT:
            connSm.onCurrentTransport(state);
            break;
        case CB_PREPARE_AUDIO_BEARER:
            int bearer = state;
            /* remove previous pending update bearer request for same device (if any) */
            if (mPendingPrepareAudioBearer.contains(device)) {
                mPendingPrepareAudioBearer.remove(device);
            }

            int connState = connSm.getState();
            if (bearer == XpanConstants.BEARER_P2P) {
                if (connState == XpanConnectionStateMachine.DISCONNECTED
                        || connState == XpanConnectionStateMachine.DISCONNECTING) {
                    // trigger XPAN Connection and enqueue bearer switch request
                    mPendingPrepareAudioBearer.add(device);
                    connect(device);
                    return;
                }
            }
            connSm.onPrepareAudioBearer(bearer);
            break;
        case CB_BEARER_SWITCH_INDICATION:
            connSm.onBearerSwitchIndication(bearerType, state);
            break;
        case MSG_STREAM_STATE_CHANGED:
            connSm.updatePlayState(state);
            break;
        case CB_SCAN_RESULT_BT:
            connSm.onScanResult();
            break;
        case CB_UPDATE_USECASE:
            connSm.onUseCaseUpdate(state);
            break;

        case CB_BEARER_SWITCH_FAILED:
            connSm.onBearerSwitchFailed(state, bearerType);
            break;

        default :
            if(VDBG) Log.v(TAG, "updateToConnSm Ignore " + opCode);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (DBG)
            Log.d(TAG, TAG);
        mXpanHandlerThread = new HandlerThread("XpanHandlerThread");
        mXpanHandlerThread.start();
        mLooper = mXpanHandlerThread.getLooper();
        mCtx = getApplicationContext();
        mXpanUtils = XpanUtils.getInstance(mCtx);
        mDatabaseManager = new XpanDatabaseManager(this);
        mXpanHandler = new XpanHandler(mLooper);
        setBluetoothXpanService(this);
        mXpanHandler.sendMessage(mXpanHandler.obtainMessage(MSG_XPAN_SERVICE_INIT));
        registerReceivers();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (VDBG)
            Log.v(TAG, "onStartCommand");
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (DBG)
            Log.d(TAG, "onDestroy");
        setBluetoothXpanService(null);
        mProviderClient.close();
        unregisterReceiver(mReceiver);
        if (mLeAudioProxy != null) {
            mLeAudioProxy.close();
        }
        if (mXpanSapSm != null) {
            mXpanSapSm.doQuit();
        }
        if (mApSm != null) {
            mApSm.doQuit();
        }
        mXpanHandler.sendMessage(mXpanHandler.obtainMessage(MSG_XPAN_SERVICE_CLEAR));
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (DBG)
            Log.d(TAG, "onBind");
        return null;
    }

    private void init() {
        mLeAudioProxy = new LeAudioProxy(mCtx, XpanProfileService.this, mXpanHandler);
        mProviderClient = new XpanProviderClient(mXpanHandler, mXpanUtils);
        mXpanSapSm = XpanSapStateMachine.make(sService);
        mNsdHelper = new XpanNsdHelper(sService);
        mApSm = XpanApStateMachine.make(sService, mLooper);
        mUsecase = XpanConstants.USECASE_NONE;
        mIsStart = true;
    }

    private void initiatePendingConnections() {
        BluetoothDevice device = mLeAudioProxy.getConnectedDevices();
        if (DBG)
            Log.d(TAG, "initiatePendingConnections " + ((device != null) ? device : ""));
        if (device != null) {
            mXpanHandler.sendMessage(
                    mXpanHandler.obtainMessage(MSG_HANDLE_LE_AUDIO_CONNECTED, device));
        }
        mIsStart = false;
    }

    private BluetoothDevice getIdentityDevice(BluetoothDevice device) {
        BluetoothDevice identityDevice = mIdetityDevicesMap.get(device);
        if (DBG)
            Log.i(TAG, "getIdentityDevice device "
                    + device + " identityDevice " + identityDevice);
        return (identityDevice != null) ? identityDevice : device;
    }

    /**
     * Get the current instance of {@link XpanProfileService}
     *
     * @return current instance of {@link XpanProfileService}
     */
    static synchronized XpanProfileService getService() {
        if (sService == null) {
            Log.w(TAG, "getService null");
            return null;
        }
        return sService;
    }

    private static synchronized void setBluetoothXpanService(
            XpanProfileService instance) {
        sService = instance;
        if (VDBG) {
            Log.v(TAG, "setBluetoothXpanService " + sService );
        }
    }

    XpanUtils getXpanUtils() {
        return mXpanUtils;
    }

    XpanProviderClient getProviderClient() {
        if (mProviderClient == null) {
            Log.w(TAG, "getProviderClient null");
        }
        return mProviderClient;
    }

    LeAudioProxy getLeAudioProxy() {
        return mLeAudioProxy;
    }

    XpanNsdHelper getNsdHelper() {
        return mNsdHelper;
    }

    private void registerReceivers() {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothLeAudio.ACTION_LE_AUDIO_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothLeAudio.ACTION_LE_AUDIO_ACTIVE_DEVICE_CHANGED);
        filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(Intent.ACTION_USER_SWITCHED);
        registerReceiver(mReceiver, filter);
        int btState = mXpanUtils.getBluetoothAdapter().getState();
        if (VDBG)
            Log.v(TAG, "registerReceivers " + btState);
        if (btState == BluetoothAdapter.STATE_ON) {
            sendOnState();
        }
    }

    /**
     * This function is called when its known that its a XPAN Supporting device.
     */
    private void closeGattConnection(BluetoothDevice device, int disReason) {
        if (VDBG)
            Log.v(TAG, "closeGattConnection " + device);
        XpanGattStateMachine gatSm = getGattStateMachine(device);
        if(gatSm == null) return;
        if (disReason == XpanConstants.TP_REASON_UNPAIRED) {
            mXpanUtils.cacheDevice(device, XpanConstants.QLL_NOT_SUPPORT);
            mXpanUtils.cacheDeviceCodec(device, -1);
            mProviderClient.updateBondState(device, BluetoothDevice.BOND_NONE);
        }
        mProviderClient.updateTransport(device, false, disReason);
        stopScan(device);
        removeStateMachine(device);
    }

    private void openGattConnection(BluetoothDevice device) {
        if (DBG)
            Log.v(TAG, "openGattConnection " + device);
        if (mXpanUtils.isAirplaneModeOn()) {
            if (DBG)
                Log.d(TAG, "Ignore AirplaneMode on");
            return;
        } else if(!mXpanSapSm.isWifiDrvierReady()) {
            if (DBG)
                Log.d(TAG, "Ignore Wifi driver not ready");
            mPendingWifiDriverInit = true;
            return;
        } else if (isQllSupport(device)) {
            mLeAudioProxy.setGroupId(device);
            BluetoothDevice identityDevice = mXpanUtils.getIdentityAddress(device);
            if (!identityDevice.equals(device)) {
                mIdetityDevicesMap.put(device, identityDevice);
            }
            XpanGattStateMachine sm = getOrCreateGattStateMachine(identityDevice);
            if (sm == null) {
                return;
            }
            updateCodecToR4(device);
            mXpanUtils.cacheDevice(device, XpanConstants.QLL_SUPPORT);
            sm.sendMessage(XpanGattStateMachine.CONNECT);
        }
    }

    private void removeStateMachine(BluetoothDevice device) {
        if (DBG)
            Log.d(TAG, "removeStateMachine " + device);
        XpanGattStateMachine gatSm = getGattStateMachine(device);
        XpanConnectionStateMachine conSm = getConnectionStateMachine(device);

        if (conSm != null &&
                conSm.getState() != XpanConnectionStateMachine.DISCONNECTED) {
            conSm.disconnectXpan();
        } else if (gatSm != null && gatSm.getState() == XpanGattStateMachine.CONNECTED) {
            gatSm.sendMessage(XpanGattStateMachine.DISCONNECT);
        } else {
            if (DBG) {
                Log.d(TAG, "removeStateMachine else");
            }
        }

        if(mIdetityDevicesMap.containsKey(device)) {
            mIdetityDevicesMap.remove(device);
        } else {
            Set<BluetoothDevice> set = mIdetityDevicesMap.keySet();
            for(BluetoothDevice dev : set) {
                if(mIdetityDevicesMap.get(dev).equals(device)){
                    mIdetityDevicesMap.remove(dev);
                }
            }
        }
        mXpanSapSm.disableSap(device);
    }

    private void disconnectConnSm(BluetoothDevice device) {
        if (DBG)
            Log.d(TAG, "disconnectConnSm " + device);
        XpanConnectionStateMachine conSm = getConnectionStateMachine(device);
        if (conSm == null) {
            if (DBG)
                Log.d(TAG, "disconnectConnSm conSm null ");
            return;
        }
        conSm.disconnectXpan();
        mXpanSapSm.disableSap(device);
    }

    private XpanGattStateMachine getOrCreateGattStateMachine(BluetoothDevice device) {
        if (DBG)
            Log.d(TAG, "getOrCreateGattStateMachine " + device);

        if (device == null) {
            Log.e(TAG, "Invalid remote device instance.");
            return null;
        }

        synchronized (mGattStateMachines) {
            XpanGattStateMachine sm = mGattStateMachines.get(device);
            if (sm != null) {
                return sm;
            }

            // Limit the maximum number of state machines to avoid DoS attack
            if (mGattStateMachines.size() >= MAX_XPAN_STATE_MACHINES) {
                Log.e(TAG, "Maximum number of XPAN GATT state machines reached: "
                        + mGattStateMachines.size());
                return null;
            }

            sm = XpanGattStateMachine.make(device, this, mLooper);
            mGattStateMachines.put(device, sm);
            getOrCreateXpanConnectionStateMachine(device);
            return sm;
        }
    }

    private XpanConnectionStateMachine getOrCreateXpanConnectionStateMachine(
            BluetoothDevice device) {
        if (DBG)
            Log.d(TAG, "getOrCreateXpanConnectionStateMachine " + device);

        if (device == null) {
            return null;
        }

        synchronized (mConnStateMachines) {
            XpanConnectionStateMachine sm = mConnStateMachines.get(device);
            if (sm != null) {
                return sm;
            }

            // Limit the maximum number of state machines to avoid DoS attack
            if (mConnStateMachines.size() >= MAX_XPAN_STATE_MACHINES) {
                Log.e(TAG, "Maximum number of GATT state machines reached: "
                        + mConnStateMachines.size());
                return null;
            }

            if (DBG)
                Log.d(TAG, "Creating Connection state machine " + device);
            XpanGattStateMachine gattSm = getGattStateMachine(device);
            if (gattSm == null) {
                Log.w(TAG, "Corresponding GATT State Machine not found");
                return null;
            }

            sm = XpanConnectionStateMachine.make(device, this,
                    mLooper, gattSm, mXpanSapSm, mApSm);
            mConnStateMachines.put(device, sm);
            return sm;
        }
    }

    private XpanConnectionStateMachine getConnectionStateMachine(BluetoothDevice device) {
        XpanConnectionStateMachine connSm = null;
        if (device != null) {
            synchronized (mConnStateMachines) {
                connSm =  mConnStateMachines.get(device);
            }
        }
        return connSm;
    }

    private XpanGattStateMachine getGattStateMachine(BluetoothDevice device) {
        if (device == null) {
            return null;
        }

        synchronized (mGattStateMachines) {
            return mGattStateMachines.get(device);
        }
    }

    Handler getXpanHandler() {
        return mXpanHandler;
    }

    void connect(BluetoothDevice device) {
        if (DBG)
            Log.d(TAG, "connect " + device);

        XpanConnectionStateMachine mXpanConnSm = getConnectionStateMachine(device);
        if (mXpanConnSm == null) {
            Log.w(TAG, " Ignore connect connsm not found");
            return;
        }

        int state = mXpanConnSm.getState();
        if (state == XpanConnectionStateMachine.CONNECTED
                || state == XpanConnectionStateMachine.TWT_CONFIGURED
                || state == XpanConnectionStateMachine.BEARER_ACTIVE) {
            if (DBG)
                Log.d(TAG, "XPAN Connection already established");
            // mXpanProfileCallback.onConnectionStateChanged(device,
            // XpanConnectionStateMachine.CONNECTED);
            return;
        }

        if (!mLeAudioProxy.isR4CodecSupport(device)) {
            if(DBG) Log.d(TAG,"R4 codec not support");
            mXpanHandler.sendMessage(mXpanHandler
                    .obtainMessage(MSG_HANDLE_DUT_CODEC_R4_NOT_SUPPORTED,
                            XpanConstants.TP_REASON_DUT_R4_NOT_SUPPORT, -1, device));
            return;
        }
        mXpanConnSm.connectXpan();
    }

    private void onBearerPreferenceRes(XpanEvent event) {
        BluetoothDevice device = event.getDevice();
        if (VDBG)
            Log.v(TAG, "onBearerPreferenceRes " + event);
        XpanConnectionStateMachine connSm = getConnectionStateMachine(device);
        if (connSm == null) {
            Log.w(TAG, "onBearerPreferenceRes Conn SM doesnt exist");
            return;
        }
        connSm.onBearerPreferenceRes(event);
    }

    void updateVBCPeriodicity(int periodicity, BluetoothDevice device) {
        mProviderClient.updateVBCPeriodicity(periodicity, device);
    }


    void sendMacAddressStateUpdate(MacAddress macAddress, boolean isConnected) {
        XpanConnectionStateMachine connSm = getConnSmFromMac(macAddress);
        if (DBG)
            Log.d(TAG, "sendMacAddressStateUpdate " + macAddress + " " + isConnected);
        if (connSm == null) {
            Log.d(TAG, "connSm not found ");
            return;
        }
        connSm.sendMacStateChanged(macAddress, isConnected);
    }

    private void updateTwtSession(MacAddress macAddress, int sp, int si, int type) {
        XpanConnectionStateMachine connSm = getConnSmFromMac(macAddress);
        if (connSm == null) {
            Log.d(TAG, "updateTwtSession ConnSm not found " + macAddress);
            return;
        }
        int state = connSm.getState();
        if (state == XpanConnectionStateMachine.DISCONNECTED
                || state == XpanConnectionStateMachine.DISCONNECTING) {
            Log.d(TAG, "updateTwtSession ConnSm disconnected");
            return;
        }
        connSm.onTwtSession(macAddress, sp, si, type);
    }

    private XpanConnectionStateMachine getConnSmFromMac(MacAddress macAddress) {
        Set<BluetoothDevice> keys = mConnStateMachines.keySet();
        XpanConnectionStateMachine connSm, connSmMac = null;
        BluetoothDevice device = null;
        for (BluetoothDevice dev : keys) {
            connSm = mConnStateMachines.get(dev);
            if (connSm != null && connSm.isMacAddressPresent(macAddress)) {
                connSmMac = connSm;
                break;
            }
        }
        return connSmMac;
    }

    void onGattSmDisConnected(BluetoothDevice device) {
        if (DBG)
            Log.d(TAG, "onGattSmDisConnected " + device);

        disconnectConnSm(device);
    }

    int getCurrentUseCase() {
        return mUsecase;
    }

    private void sendAirPlaneModeChange() {
        boolean isEnabled = mXpanUtils.isAirplaneModeOn();
        if (DBG)
            Log.d(TAG, "sendAirPlaneModeChange " + isEnabled);

        if (mConnStateMachines.size() > 0 && !mXpanSapSm.isSapDisabled()) {
            mXpanSapSm.onAirPlaneModeEnabled(isEnabled);
        } else if (!isEnabled) {
            connectAfterApOrWssrDisable();
        }
    }

    private void onWifiSSRCompleted() {
        if (DBG)
            Log.d(TAG, "onWifiSSRCompleted");
        if(mXpanSapSm.isSapDisabled()) {
            connectAfterApOrWssrDisable();
        }
    }

    boolean isQllSupport(BluetoothDevice device) {
        boolean isSupport = false;
        if (mListQllSupport.contains(device)) {
            isSupport = true;
        } if (mPreviousXpanDevices.contains(device)) {
            isSupport = true;
        }
        if (DBG && !isSupport)
            Log.d(TAG, "isQllSupport " + device + " " + isSupport);
        return isSupport;
    }

    private List<BluetoothDevice> getConnectedDevices() {
        enforceCallingOrSelfPermission(BLUETOOTH_CONNECT, "Need BLUETOOTH_CONNECT permission");
        synchronized (mGattStateMachines) {
            List<BluetoothDevice> devices = new ArrayList<>();
            for (XpanGattStateMachine sm : mGattStateMachines.values()) {
                if (sm.isConnected()) {
                    devices.add(sm.getDevice());
                }
            }
            return devices;
        }
    }

    @Override
    public void onServiceStateUpdated(int state) {
        mLeProxyConnected = (state == XpanConstants.TRUE) ? true : false;
        if (DBG)
            Log.d(TAG, "onServiceStateUpdated " + mLeProxyConnected + " " + mIsStart);
        if (mLeProxyConnected && mIsStart) {
            mXpanHandler.sendMessage(mXpanHandler.obtainMessage(MSG_INIT_PENDING_CONNECTIONS));
        }
    }

    private void connectAfterApOrWssrDisable() {
        if (DBG)
            Log.d(TAG, "connectAfterApOrWssrDisable");
        mXpanHandler.sendMessage(mXpanHandler.obtainMessage(MSG_INIT_PENDING_CONNECTIONS));
    }

    private void clear() {
        if (VDBG)
            Log.v(TAG, "clear");
        mGattStateMachines.forEach((k,v)->v.quitNow());
        mConnStateMachines.forEach((k,v)->v.quitNow());
        mListQllSupport.clear();
        mIdetityDevicesMap.clear();
        mPendingPrepareAudioBearer.clear();
        mPreviousXpanDevices.clear();
        mGattStateMachines.clear();
        mConnStateMachines.clear();
        mIsWSsrInProgress = false;
        mDevicePlaying = null;
        mPendingWifiDriverInit = false;
        if (mXpanHandlerThread != null) {
            mXpanHandlerThread.quit();
        }
        if (mNsdHelper != null) {
            mNsdHelper.updateMessage(null, NsdMsg.CLOSE);
            mNsdHelper = null;
        }
    }


    BluetoothDevice getPlayingDevice() {
        return mDevicePlaying;
    }

    private void updateCodecToR4(BluetoothDevice device) {
        if (VDBG)
            Log.v(TAG, "updateCodecToR4 " + device);

        if (mXpanUtils.isCodecSetPending(device)) {
            if (mLeAudioProxy == null || !mLeAudioProxy.isActiveDevice(device)) {
                if (VDBG)
                    Log.v(TAG, "updateCodecToR4 " + device + " not active");
                return;
            }
            boolean updatedCodec = mLeAudioProxy.setCodecConfigPreference(device,
                    LeAudioProxy.CODEC_TYPE_APTX_ADAPTIVE_R4);
            if (updatedCodec) {
                mXpanUtils.cacheDeviceCodec(device, -1);
            }
        }
    }

    XpanApStateMachine getApSm() {
        return mApSm;
    }

    void startScan(BluetoothDevice device) {
        if (sService == null) {
            Log.w(TAG, "startScan Service not started");
            return;
        }
        if (mScanner == null) {
            mScanner = new XpanScanner(sService);
        }
        mScanner.startScan(device);
    }

    void stopScan(BluetoothDevice device) {
        if (mScanner == null || !mScanner.isScanInProgress()) {
            return;
        }
        mScanner.stopScan(device);
    }

    void onScanResult(BluetoothDevice device) {
        if (DBG)    
            Log.d(TAG, "onScanResult " + device);   
        updateToConnSm(device, -1, CB_SCAN_RESULT_BT, -1);
    }

    private void sendOnState() {
        mXpanHandler.sendMessage(mXpanHandler.obtainMessage(MSG_STATE_ON));
    }

    boolean isStreamingOnSap(BluetoothDevice device) {
        Set<BluetoothDevice> keys = mConnStateMachines.keySet();
        boolean isStreaming = false;
        for (BluetoothDevice dev : keys) {
            if (dev.equals(device)) {
                continue;
            }
            XpanConnectionStateMachine connSm = mConnStateMachines.get(dev);
            if (connSm.isStreamingOnSap()) {
                isStreaming = true;
                break;
            }
        }
        if (DBG)
            Log.d(TAG, "isStreamingOnSap  " + isStreaming);
        return isStreaming;
    }

    Looper getLooper() {
        return mLooper;
    }

    boolean isPendingApStateChange() {
        return mXpanHandler.hasMessages(MSG_AIRPLANE_STATE_CHANGE);
    }

    void enableXpan(BluetoothDevice device, boolean enable) {
        if (DBG)
            Log.d(TAG, "enableXpan  " + device + "  " + enable);
        int codecType = LeAudioProxy.CODEC_TYPE_DEFAULT;
        if (enable) {
            codecType = LeAudioProxy.CODEC_TYPE_APTX_ADAPTIVE_R4;
            openGattConnection(device);
        } else {
            closeGattConnection(device, XpanConstants.TP_REASON_DISCONNECT);
        }
        mLeAudioProxy.setCodecConfigPreference(device, codecType);
    }
}
