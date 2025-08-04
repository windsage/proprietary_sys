/*****************************************************************
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 ******************************************************************/

package vendor.qti.bluetooth.xpan;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

import com.android.internal.util.IState;
import com.android.internal.util.State;

import com.android.internal.util.StateMachine;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.net.MacAddress;
import android.net.wifi.SoftApCapability;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.SoftApInfo;
import android.net.wifi.WifiSsid;
import android.net.wifi.WifiClient;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.LocalOnlyHotspotReservation;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;

/**
 * Bluetooth Xpan SAP Connection State Machine
 *
 *                      (SapDisabled)
 *                           ^
 *                           |
 *               SAP_ENABLED | SAP_DISABLED
 *                           v
 *                      (SapEnabled)
 *                           ^
 *                           |
 *             SAP_CONNECTED | SAP_DISCONNECTED
 *                           v
 *                     (SapConnected)
 *                           ^
 *                           |
 *         SAP_MODE_STANDBY  | SAP_MODE_ACTIVE
 *                           v
 *                     (SapLowPower)
 */

final class XpanSapStateMachine extends StateMachine {
    private static final String TAG = "XpanSap";
    private final boolean VDBG = XpanUtils.VDBG;
    private static final boolean DBG = XpanUtils.DBG;

    /* SAP Connection States */
    static final int SAP_AIRPLANE = 0;
    static final int SAP_DISABLED = 1;
    static final int SAP_ENABLING = 2;
    static final int SAP_DISABLING = 3;
    static final int SAP_ENABLED = 4;
    static final int SAP_CONNECTED = 5;
    static final int SAP_LOWPOWER = 6;

    /* SAP State machine messages */
    private final int ENABLE_SAP = 1;
    private final int DISABLE_SAP = 2;
    private final int SAP_STATE_CHANGED = 3;
    private final int SAP_STATION_CONNECTED = 4;
    private final int SAP_STATION_DISCONNECTED = 5;
    private final int RE_CREATE_SAP_INTERFACE = 6;
    private final int TIMEOUT_LOWPOWER = 7;
    private final int SAP_STATION_ACTIVE = 8;
    static final int SAP_FREQUENCE_UPDATED = 9;
    private final int SAP_INTERFACE_READY = 10;
    private final int CREATE_SAP_INTERFACE = 11;
    private final int ENABLE_OR_DISABLE_LOW_POWER = 12;
    private final int ON_BEACON_INTERVALS_RECEIVED = 13;
    private final int AIRPLANEMODE_ENABLED = 14;
    private final int AIRPLANEMODE_DISABLED = 15;
    private final int SSR_WIFI_STARTED = 16;
    private final int SSR_WIFI_COMPLETED = 17;
    private final int ON_CSS = 18;
    private final int ON_IF_UPDATE = 19;
    private final int CAN_IF_CREATE = 20;

    /* XpanSapStateMachine states */
    private final SapDisabled mSapDisabled;
    private final SapEnabling mSapEnabling;
    private final SapEnabled mSapEnabled;
    private final SapDisabling mSapDisabling;
    private final SapConnected mSapConnected;
    private final SapLowPower mSapLowPower;
    private final AirplaneMode mSapAirplane;

    private XpanProfileService mService;

    private WifiManager mWifiManager;
    private LocalOnlyHotspotReservation mReservation;

    private HandlerExecutor mExecutor;
    private Handler mHandler;

    private int mCurrentState = SAP_DISABLED;
    private int mFrequency = 0;
    private int mDialogId = 0, mWifiStatus = -1;
    private boolean mSapIfCreated = false;
    private boolean mSsrCbPending = false, mSsrStarted = false;
    private boolean mLohsClosePending = false, mIsSapFailed= false;
    private boolean mIsRecreate = false, mIsCountryUpdated = false, mSapLocalDisable = false;
    private boolean mIfCrPen = false, mIfDelPen = false, mLohsCbPen = false, mCbLpPending = false;

    private ConcurrentHashMap<BluetoothDevice, XpanSapCallback> xpanSapCb =
            new ConcurrentHashMap<>();

    private CopyOnWriteArrayList<MacAddress> mConnectedClients =
            new CopyOnWriteArrayList<>();

    private ConcurrentHashMap<Integer, DeviceLP> mMapDialog =
            new ConcurrentHashMap<>();

    private XpanProviderClient mProviderClient;

    private WifiUtils mWifiUtils;
    private XpanUtils mUtils;
    private String mCurrentSsid = "";
    private MacAddress mCurrentBssid;
    private PowerManager.WakeLock mWakeLock = null;
    private Context mContext;

    BiConsumer<Boolean, Set<WifiManager.InterfaceCreationImpact>> bioConsumer
                = new BiConsumer<Boolean, Set<WifiManager.InterfaceCreationImpact>>() {
        @Override
        public void accept(Boolean create, Set<WifiManager.InterfaceCreationImpact> impacts) {
            if (VDBG)
                Log.v(TAG, "accept " + create + " " + impacts + " "
                        + mWifiUtils.isWifiEnabled() + " " + mWifiUtils.isWifiApEnabled());

            if ( (create && impacts != null && impacts.size() > 0)
                    || (!create && (mWifiUtils.isWifiEnabled() || mWifiUtils.isWifiApEnabled()))) {
                onSapStateChanged(SAP_DISABLED);
                if (DBG)
                    Log.d(TAG, "CREATE_SAP_INTERFACE Ignore");
            } else {
                if (DBG)
                    Log.d(TAG, "CREATE_SAP_INTERFACE");
                sendMessage(CREATE_SAP_INTERFACE);
            }
        }

    };

    XpanSapStateMachine(XpanProfileService svc) {
        super(TAG, svc.getLooper());
        if (XpanUtils.DBGSM) {
            setDbg(true);
        }
        mService = svc;
        mHandler = svc.getXpanHandler();
        mContext = svc.getApplicationContext();
        mWifiManager = svc.getSystemService(WifiManager.class);
        mProviderClient = mService.getProviderClient();
        mWifiUtils = WifiUtils.getInstance(mContext);
        mUtils = XpanUtils.getInstance(mContext);

        mSapDisabled = new SapDisabled();
        mSapEnabling = new SapEnabling();
        mSapEnabled = new SapEnabled();
        mSapConnected = new SapConnected();
        mSapLowPower = new SapLowPower();
        mSapDisabling = new SapDisabling();
        mSapAirplane = new AirplaneMode();

        addState(mSapAirplane);
        addState(mSapDisabled);
        addState(mSapEnabling);
        addState(mSapEnabled);
        addState(mSapConnected);
        addState(mSapLowPower);
        addState(mSapDisabling);

        setInitialState(mSapDisabled);

        mExecutor = new HandlerExecutor(mHandler);
        mWifiManager.registerLocalOnlyHotspotSoftApCallback(mExecutor, mSoftApCallback);
        if (DBG) Log.d(TAG, TAG);
    }

    static XpanSapStateMachine make(XpanProfileService svc) {
        if (DBG) Log.d(TAG, "make");
        XpanSapStateMachine xpanSapSm = new XpanSapStateMachine(svc);
        xpanSapSm.start();
        return xpanSapSm;
    }

    private void init() {
        mFrequency = 0;
        mDialogId =0;
        mCurrentSsid = null;
        mCurrentBssid = null;
        mMapDialog.clear();
        mConnectedClients.clear();
        mCbLpPending = false;
    }

    void doQuit() {
        logDebug("doQuit ");
        if (mReservation != null && !mLohsClosePending) {
            handleDisableSap();
        } else if (mSapIfCreated && !mLohsClosePending) {
            deleteInterface();
            mSapIfCreated = false;
        }
        xpanSapCb.clear();
        mWifiManager.unregisterLocalOnlyHotspotSoftApCallback(mSoftApCallback);
        mWifiUtils.close();
        mIfDelPen = false;
        mIfCrPen = false;
        mLohsCbPen = false;
    }

    class SapDisabled extends State {

        @Override
        public void enter() {
            init();
            mCurrentState = SAP_DISABLED;
            log( "Enter SapDisabled");
            if (!isWifiDrvierReady()) {
                mProviderClient.getWifiDriverStatus();
            }
            updateSapState(XpanConstants.SAP_STATE_DISABLED);
        }

        @Override
        public void exit() {
            if (mIsRecreate) {
                mIsRecreate = false;
            }
            mSapLocalDisable = false;
        }

        @Override
        public boolean processMessage(Message message) {
            log("SapDisabled processMessage " + messageToString(
                    message.what));

            switch (message.what) {
                case CAN_IF_CREATE :
                    reportCreateInterfaceImpact();
                    break;
                case CREATE_SAP_INTERFACE :
                    createInterface();
                    break;

                case RE_CREATE_SAP_INTERFACE :
                    mIsRecreate = true;
                    deleteInterface();
                    break;

                case ENABLE_SAP:
                    handleEnableSap();
                    transitionTo(mSapEnabling);
                    break;

                case DISABLE_SAP:
                    Log.w(TAG, "Already in SapDisabled state.");
                    break;

                case SAP_STATE_CHANGED:
                    logV("SAP_STATE_CHANGED " + stateToString(message.arg1));
                    if (message.arg1 == SAP_ENABLED) {
                        handleSapEnabled((LocalOnlyHotspotReservation)message.obj);
                        transitionTo(mSapEnabled);
                    }
                    break;

                case ON_IF_UPDATE:
                    sendSapIfUpdate();
                    break;

                default:
                    log("SapDisabled not handled " + message.what);
                    return NOT_HANDLED;
            }
            return HANDLED;
        }
    }

    class SapEnabling extends State {

        @Override
        public void enter() {
            mCurrentState = SAP_ENABLING;
            xpanSapCb.forEach((k,v)->v.onSapStateChanged(mCurrentSsid, SAP_ENABLING));
            log( "Enter SapEnabling");
        }

        @Override
        public boolean processMessage(Message message) {
            log("SapEnabling processMessage " + messageToString(
                    message.what));

            switch (message.what) {
                case ENABLE_SAP:
                    Log.w(TAG, "SAP is already enabling");
                    break;

                case DISABLE_SAP:
                    handleDisableSap();
                    break;

                case SAP_STATE_CHANGED:
                    logV("SAP_STATE_CHANGED " + stateToString(message.arg1));
                    switch (message.arg1) {
                        case SAP_ENABLED:
                            handleSapEnabled((LocalOnlyHotspotReservation)message.obj);
                            transitionTo(mSapEnabled);
                            break;

                        case SAP_DISABLED:
                            handleSapDisabled();
                            transitionTo(mSapDisabled);
                            break;

                        default:
                            Log.w(TAG, "Unanticipated state transition");
                    }
                    break;

                case SAP_FREQUENCE_UPDATED:
                    Log.w(TAG, "SapEnabling, deferMessage SAP_FREQUENCE_UPDATED");
                    deferMessage(message);
                    break;

                case SSR_WIFI_STARTED:
                    updateWifiSSrOrAirplaeMode(SSR_WIFI_STARTED);
                    break;

                case AIRPLANEMODE_ENABLED:
                    updateWifiSSrOrAirplaeMode(AIRPLANEMODE_ENABLED);
                    break;

                case ON_IF_UPDATE:
                    sendSapIfUpdate();
                    break;

                default:
                    log("SapEnabling not handled " + message.what);
                    return NOT_HANDLED;
            }
            return HANDLED;
        }
    }

    class SapEnabled extends State {

         @Override
         public void enter() {
             mCurrentState = SAP_ENABLED;
             xpanSapCb.forEach((k,v)->v.onSapStateChanged(mCurrentSsid, SAP_ENABLED));
             log( "Enter SapEnabled");
             updateSapState(XpanConstants.SAP_STATE_ENABLED);
             if (mUtils.isLowpowerEnable()) {
                 // acquire the wakelock
                 acquireWakeLock();
                 sendMessageDelayed(TIMEOUT_LOWPOWER,
                         mUtils.secToMillieSec(XpanConstants.SAP_LOW_POWER_DURATION));
             }
         }

         @Override
         public void exit() {
             if (mUtils.isLowpowerEnable()) {
                 removeMessages(TIMEOUT_LOWPOWER);
                 // release the wakelock
                 releaseWakeLock();
             }
         }

         @Override
         public boolean processMessage(Message message) {
             log("SapEnabled processMessage " + messageToString(
                     message.what));

             switch (message.what) {
                 case ENABLE_SAP:
                     log("SAP already enabled. SSID " + mCurrentSsid);
                     break;

                 case DISABLE_SAP:
                     handleDisableSap();
                     break;

                 case SAP_STATE_CHANGED:
                    int newState = message.arg1;
                    logV("SAP_STATE_CHANGED " + stateToString(newState));
                    if (newState == SAP_DISABLED) {
                        handleSapDisabled();
                        transitionTo(mSapDisabled);
                    } else if(newState == SAP_ENABLED) {
                        if(message.obj instanceof LocalOnlyHotspotReservation) {
                            //Validate SAP configuration
                            handleSapEnabled((LocalOnlyHotspotReservation)message.obj);
                        }
                    }
                    break;

                 case SAP_FREQUENCE_UPDATED:
                     sendFrequncyUpdated();
                     break;

                case SAP_STATION_CONNECTED:
                    transitionTo(mSapConnected);
                    break;

                case SAP_STATION_DISCONNECTED:
                    log("SapEnabled SAP_STATION_DISCONNECTED");
                    break;

                case SSR_WIFI_STARTED:
                    updateWifiSSrOrAirplaeMode(SSR_WIFI_STARTED);
                    break;

                case AIRPLANEMODE_ENABLED:
                    updateWifiSSrOrAirplaeMode(AIRPLANEMODE_ENABLED);
                    break;

                case TIMEOUT_LOWPOWER:
                    enableSapLowPower(null, XpanConstants.SAP_ENABLE_LOW_POWER);
                    // release the wakelock
                    releaseWakeLock();
                    break;

                case ON_BEACON_INTERVALS_RECEIVED:
                    updatedBeaconIntervals(message);
                    break;

                case ENABLE_OR_DISABLE_LOW_POWER:
                    int powerState = message.arg1;
                    BluetoothDevice device = (BluetoothDevice) message.obj;
                    enableSapLowPower(device, powerState);
                    break;

                case ON_IF_UPDATE:
                    sendSapIfUpdate();
                    break;

                 default:
                      log("SapEnabled not handled " + message.what);
                     return NOT_HANDLED;
             }
             return HANDLED;
         }
     }

    class SapDisabling extends State {

         @Override
         public void enter() {
             mCurrentState = SAP_DISABLING;
             xpanSapCb.forEach((k,v)->v.onSapStateChanged(mCurrentSsid, SAP_DISABLING));
             log( "Enter SapDisabling");
             releaseWakeLock();
         }

         @Override
         public boolean processMessage(Message message) {
             log("SapDisabling processMessage " + messageToString(
                     message.what));

             switch (message.what) {
                 case ENABLE_SAP:
                     log("Enable Sap in DISABLED State");
                     deferMessage(message);
                     break;

                 case DISABLE_SAP:
                     Log.w(TAG, "SAP is already disabling. Ignore.");
                     break;

                 case SAP_STATE_CHANGED:
                    int newState = message.arg1;
                    logV("SAP_STATE_CHANGED " + stateToString(newState));
                    if (newState == SAP_DISABLED) {
                        handleSapDisabled();
                        transitionTo(mSapDisabled);
                    }  if (newState == SAP_ENABLED) {
                        handleSapEnabled((LocalOnlyHotspotReservation)message.obj);
                        transitionTo(mSapEnabled);
                    }
                    break;

                 default:
                      log("SapDisabling not handled " + message.what);
                     return NOT_HANDLED;
             }
             return HANDLED;
         }
     }

    class SapConnected extends State {

        @Override
        public void enter() {
            mCurrentState = SAP_CONNECTED;
            log( "Enter SapConnected ");
        }

        @Override
        public boolean processMessage(Message message) {
            log("SapConnected processMessage "
                    + messageToString(message.what));

            switch (message.what) {
                case DISABLE_SAP:
                    handleDisableSap();
                    break;

                case SAP_STATE_CHANGED:
                    int newState = message.arg1;
                    logV("SAP_STATE_CHANGED " + stateToString(newState));
                    if (newState == SAP_DISABLED) {
                        handleSapDisabled();
                        transitionTo(mSapDisabled);
                    }
                    break;

                case SAP_STATION_CONNECTED:
                    log("Total Connected clients  " + mConnectedClients.size());
                    break;

                case SAP_STATION_DISCONNECTED:
                    if (mConnectedClients.size() == 0) {
                        transitionTo(mSapEnabled);
                    }
                    break;

                case ENABLE_OR_DISABLE_LOW_POWER:
                    int powerState = message.arg1;
                    BluetoothDevice device = (BluetoothDevice) message.obj;
                    enableSapLowPower(device, powerState);
                    break;

                case ON_BEACON_INTERVALS_RECEIVED:
                    updatedBeaconIntervals(message);
                    break;

                case SAP_STATION_ACTIVE:
                    log("SapConnected Already Active state not ignore");
                    break;

                case SSR_WIFI_STARTED:
                    updateWifiSSrOrAirplaeMode(SSR_WIFI_STARTED);
                    break;

                case AIRPLANEMODE_ENABLED:
                    updateWifiSSrOrAirplaeMode(AIRPLANEMODE_ENABLED);
                    break;

                case ON_CSS:
                      updateCsa(message);
                    break;

                case ON_IF_UPDATE:
                    sendSapIfUpdate();
                    break;

                default:
                    log("SapConnected not handled " + message.what);
                    return NOT_HANDLED;
            }
            return HANDLED;
        }
    }

    class SapLowPower extends State {

        @Override
        public void enter() {
            mCurrentState = SAP_LOWPOWER;
            log( "Enter SapLowPower");
        }

        @Override
        public void exit() {
            int what = getCurrentMessage().what;
            if (what == ENABLE_SAP || what == SAP_STATION_ACTIVE) {
                enableSapLowPower(null, XpanConstants.SAP_DISABLE_LOW_POWER);
            }
        }

        @Override
        public boolean processMessage(Message message) {
            log("SapLowPower processMessage "
                    + messageToString(message.what));

            switch (message.what) {
                case DISABLE_SAP:
                    handleDisableSap();
                    break;

                case SAP_STATE_CHANGED:
                    int newState = message.arg1;
                    logV("SAP_STATE_CHANGED " + stateToString(newState));
                    if (newState == SAP_DISABLED) {
                        handleSapDisabled();
                        transitionTo(mSapDisabled);
                    }
                    break;

                case SAP_STATION_CONNECTED:
                    log("Total Connected clients  " + mConnectedClients.size());
                    break;

                case ENABLE_SAP:
                case SAP_STATION_ACTIVE:
                    if (mConnectedClients.size() > 0) {
                        transitionTo(mSapConnected);
                    } else {
                        transitionTo(mSapEnabled);
                    }
                    break;

                case ENABLE_OR_DISABLE_LOW_POWER:
                    int powerState = message.arg1;
                    BluetoothDevice device =(BluetoothDevice)message.obj;
                    enableSapLowPower(device, powerState);
                    break;

                case ON_BEACON_INTERVALS_RECEIVED:
                    updatedBeaconIntervals(message);
                    break;

                case SSR_WIFI_STARTED:
                    updateWifiSSrOrAirplaeMode(SSR_WIFI_STARTED);
                    break;

                case AIRPLANEMODE_ENABLED:
                    updateWifiSSrOrAirplaeMode(AIRPLANEMODE_ENABLED);
                    break;

                case ON_CSS:
                    updateCsa(message);
                  break;

                case ON_IF_UPDATE:
                    sendSapIfUpdate();
                    break;

                default:
                    log("SapLowPower not handled " + message.what);
                    return NOT_HANDLED;
            }
            return HANDLED;
        }
    }

    class AirplaneMode extends State {

        @Override
        public void enter() {
            mCurrentState = SAP_AIRPLANE;
            int what = getCurrentMessage().what;
            log("Enter AirplaneMode");
            if(what == SSR_WIFI_STARTED) {
                mSsrStarted = true;
            } else  {
                handleDisableSap();
            }
            updateSapState(XpanConstants.SAP_STATE_DISABLED);
        }

        @Override
        public boolean processMessage(Message msg) {
            log("AirplaneMode processMessage " + messageToString(msg.what));

            switch (msg.what) {

            case DISABLE_SAP:
                handleDisableSap();
                transitionTo(mSapDisabled);
                if (mSsrStarted) {
                    mSapIfCreated = false;
                    mSsrStarted = false;
                }

                break;

            case SSR_WIFI_COMPLETED:
                transitionTo(mSapDisabled);
                if (handleDisableSap()) {
                    mSsrCbPending = true;
                } else {
                    updateWifiSSrOrAirplaeMode(SSR_WIFI_COMPLETED);
                }
                break;

            case AIRPLANEMODE_DISABLED:
                updateWifiSSrOrAirplaeMode(AIRPLANEMODE_DISABLED);
                break;

            case ENABLE_SAP:
                if (DBG)
                    Log.d(TAG, "Ignore ENABLE_SAP");
                break;

            case SAP_STATION_DISCONNECTED:
                if (DBG)
                    Log.d(TAG, "SAP_STATION_DISCONNECTED");
                break;

            case CREATE_SAP_INTERFACE:
                if (DBG)
                    Log.d(TAG, "Ignore CREATE_SAP_INTERFACE");
                break;

            case SAP_FREQUENCE_UPDATED:
                sendFrequncyUpdated();
                break;

            case SAP_STATE_CHANGED:
                int state = msg.arg1;
                logV("SAP_STATE_CHANGED " + stateToString(state));
                switch (state) {
                case SAP_DISABLED:
                    handleSapDisabled();
                    break;
                case SAP_ENABLED:
                    if(msg.obj instanceof LocalOnlyHotspotReservation) {
                        handleSapEnabled((LocalOnlyHotspotReservation)msg.obj);
                    }
                    break;
                }
                break;
            default:
                log("AirplaneMode not handled " + msg.what);
                return NOT_HANDLED;
            }
            return HANDLED;
        }

    }

    private void updateWifiSSrOrAirplaeMode(int msg) {
        if (DBG)
            Log.d(TAG, "updateWifiSSrOrAirplaeMode " + msg);
        switch (msg) {
        case SSR_WIFI_COMPLETED:
            xpanSapCb.forEach((k, v) -> v.onWifiSsr(XpanConstants.WIFI_SSR_COMPLETED));
            break;
        case SSR_WIFI_STARTED:
            transitionTo(mSapAirplane);
            xpanSapCb.forEach((k, v) -> v.onWifiSsr(XpanConstants.WIFI_SSR_STARTED));
            break;
        case AIRPLANEMODE_DISABLED:
            transitionTo(mSapDisabled);
            xpanSapCb.forEach((k, v) -> v.onAirplanemodeEnabled(false));
            break;
        case AIRPLANEMODE_ENABLED:
            transitionTo(mSapAirplane);
            xpanSapCb.forEach((k, v) -> v.onAirplanemodeEnabled(true));
            break;
            default:
                Log.w(TAG,"updateWifiSSrOrAirplaeMode not handled");
                break;
        }
    }

    private void enableSapLowPower(BluetoothDevice device, int powerState) {

        boolean enable = ((powerState == XpanConstants.SAP_ENABLE_LOW_POWER) ? true : false);

        if (enable && mService.isStreamingOnSap(device)) {
            /*
             * Based on QBCESPEC-1686,
             * HS should not update beacons if streaming active with other EB SET
             */
            if (DBG)
                Log.d(TAG, "enableSapLowPower ignore");
            return;
        }

        DeviceLP deviceLp = new DeviceLP(getDialogId(), device, powerState);
        mMapDialog.put(mDialogId, deviceLp);
        if (VDBG) {
            Log.v(TAG, "enableSapLowPower " + deviceLp);
        }
        mCbLpPending = true;
        mProviderClient.enableSapLowPower(mDialogId, enable);
    }

    /* SAP Callbacks to be received by other Xpan modules */
    public interface XpanSapCallback {
        public void onSapStateChanged(String ssid, int state);
        public void onUpdatedBeaconIntervals(int powerSaveBi, long nextTsf);
        public void onAirplanemodeEnabled(boolean isEnable);
        public void onWifiSsr(int state);
        public void onCsaUpdate(XpanEvent event);
        public void onFrequencyUpdated(int frequency);
        public void onSapIfStatusUpdate();
    }

    /* LocalOnlyHotspot Callbacks received from WIFI Framework */
    private WifiManager.LocalOnlyHotspotCallback lohsCb =
            new WifiManager.LocalOnlyHotspotCallback() {

        /* LocalOnlyHotspot start succeeded. */
        @Override
        public void onStarted(LocalOnlyHotspotReservation reservation) {
            mLohsCbPen = false;
            logDebug("onStarted");
            if (mReservation != null) {
                Log.w(TAG, "onStarted reservation not closed");
            }
            mReservation = reservation;
            sendMessage(SAP_STATE_CHANGED, SAP_ENABLED, 0, reservation);
        }

        /* LocalOnlyHotspot stop succeeded. */
        @Override
        public void onStopped() {
            mLohsCbPen = false;
            boolean isAirplanemode = mUtils.isAirplaneModeOn();
            logDebug("onStopped " + (isAirplanemode ? true : ""));
            if (isAirplanemode && mCurrentState != SAP_AIRPLANE) {
                updateWifiSSrOrAirplaeMode(AIRPLANEMODE_ENABLED);
            } else {
                if (mReservation != null) {
                    if (!mLohsClosePending) {
                        logDebug("onStopped close reservation");
                        mReservation.close();
                    }
                    mReservation = null;
                } else {
                    logDebug("onStopped mReservation null ");
                }
                sendMessage(SAP_STATE_CHANGED, SAP_DISABLED);
            }
        }

        /* LocalOnlyHotspot failed to start. */
        @Override
        public void onFailed(int reason) {
            mLohsCbPen = false;
            mReservation = null;
            boolean isAirplanemode = mUtils.isAirplaneModeOn();
            if (!isAirplanemode) {
                isAirplanemode = mService.isPendingApStateChange();
            }
            logDebug("onFailed " + reason + " " + (isAirplanemode ? true : ""));
            if (isAirplanemode && mCurrentState != SAP_AIRPLANE) {
                deferMessage(mUtils.getMessageWithWhat(SAP_STATE_CHANGED,SAP_DISABLED));
                updateWifiSSrOrAirplaeMode(AIRPLANEMODE_ENABLED);
            } else {
                sendMessage(SAP_STATE_CHANGED, SAP_DISABLED);
                mIsSapFailed = true;
            }
            updateSapState(XpanConstants.SAP_STATE_FAILED);
        }
    };

    /* SoftAp Callbacks received from WIFI framework */
    private WifiManager.SoftApCallback mSoftApCallback = new WifiManager.SoftApCallback() {

        @Override
        public void onStateChanged(int state, int reason) {
            logDebug("onStateChanged " + state + " reason " + reason);
            if (state == WifiManager.WIFI_AP_STATE_DISABLED) {
                mLohsCbPen = false;
                if (mReservation != null) {
                    if (!mLohsClosePending) {
                        logDebug("onStateChanged close reservation");
                        mReservation.close();
                    }
                    mReservation = null;
                    if (mSsrCbPending) {
                        if (mSapIfCreated) {
                            deleteInterface();
                        } else {
                            updateWifiSSrOrAirplaeMode(SSR_WIFI_COMPLETED);
                            mSsrCbPending = false;
                        }
                    }
                    if (mSsrCbPending && !mSapIfCreated) {
                        updateWifiSSrOrAirplaeMode(SSR_WIFI_COMPLETED);
                        mSsrCbPending = false;
                    }
                    sendMessage(SAP_STATE_CHANGED, SAP_DISABLED);
                } else {
                    logDebug("onStateChanged " + state + " mReservation null ");
                }
                mLohsClosePending = false;
            }
        }

        @Override
        public void onConnectedClientsChanged(SoftApInfo info, List<WifiClient> clients) {
            logDebug("onConnectedClientsChanged " + info);
            logV("onConnectedClientsChanged " + clients);

            ArrayList<MacAddress> connectedStations = new ArrayList<>();
            MacAddress bssid = info.getBssid();
            if (bssid == null || !bssid.equals(mCurrentBssid)) {
                Log.w(TAG, "onConnectedClientsChanged ssid not match " + bssid + " "
                        + mCurrentBssid);
                return;
            }

            for (WifiClient clt: clients) {
                MacAddress staAddress = clt.getMacAddress();
                logDebug("onConnected " + staAddress);
                connectedStations.add(staAddress);

                // station connected
                if (staAddress != null && !mConnectedClients.contains(staAddress)) {
                    mConnectedClients.add(staAddress);
                    sendMessage(SAP_STATION_CONNECTED, staAddress);
                    sendMacAddressStateUpdate(staAddress, true /* connected */);
                }
            }

            List<MacAddress> prevConnectedClients = new ArrayList<>(mConnectedClients);
            if (prevConnectedClients.removeAll(connectedStations)) {
                for (MacAddress mac: prevConnectedClients) {
                    mConnectedClients.remove(mac);
                    sendMessage(SAP_STATION_DISCONNECTED, mac);
                    sendMacAddressStateUpdate(mac, false /* disconnected */);
                }
            }

            if (clients.size() == 0) {
                for (MacAddress mac : prevConnectedClients) {
                    mConnectedClients.remove(mac);
                    sendMessage(SAP_STATION_DISCONNECTED, mac);
                    sendMacAddressStateUpdate(mac, false /* disconnected */);
                }
            }
        }

        // Get frequency and pass to EB if frequency is differ from last sent
        // softApInfoList is always one in Xpan profile case
        @Override
        public void onInfoChanged(List<SoftApInfo> softApInfoList) {

            logV("onInfoChanged " + softApInfoList);

            SoftApInfo softapinfo = null;
            for (SoftApInfo info : softApInfoList) {
                MacAddress sofpmacaddress = info.getBssid();
                if (sofpmacaddress.equals(mWifiUtils.getBssid())) {
                    softapinfo = info;
                    break;
                }
            }

            if (softapinfo == null) {
                logDebug("onInfoChanged invalid softapinfo");
                return;
            }

            int updatedFreq = softapinfo.getFrequency();

            if (updatedFreq > 0 && mFrequency != updatedFreq) {
                onFrequencyUpdated(updatedFreq);
            }
        }

        @Override
        public void onCapabilityChanged(SoftApCapability softApCapability) {
            logDebug("onCapabilityChanged " + softApCapability);
            mWifiUtils.updateChannels(
                    softApCapability.getSupportedChannelList(mWifiUtils.getBand()));
        }

    };

    private void handleSapEnabled(LocalOnlyHotspotReservation res) {

        if (VDBG)
            Log.v(TAG, "handleSapEnabled");

        if (res == null) {
            Log.e(TAG, "Invalid LocalOnlyHotspotReservation");
            return;
        }

        boolean isValid = mWifiUtils.validateSapParams(res);

        if (!isValid) {
            Log.e(TAG, "handleSapEnabled SAP config error");
            return;
        }

        mCurrentSsid = mWifiUtils.getSsid();
        mCurrentBssid = mWifiUtils.getBssid();

    }

    private void handleSapDisabled() {
        logDebug("handleSapDisabled");

        xpanSapCb.forEach((k,v)->v.onSapStateChanged(mCurrentSsid, SAP_DISABLED));
        if(mCurrentState != SAP_AIRPLANE && !mIsCountryUpdated) {
            //xpanSapCb.clear();
            logDebug("handleSapDisabled clear callbacks ignore");
        }
        if (mSapIfCreated) {
            deleteInterface();
        }
        if (mReservation != null) {
            Log.w(TAG, "handleSapDisabled reservation not closed");
        }

    }

    private void handleEnableSap() {
        logDebug("handleEnableSap");

        SoftApConfiguration sapConfig = mWifiUtils.getSoftApConfiguration();

        if (mWifiManager == null) {
            Log.e(TAG, "WifiService is not obtained. Return.");
            return;
        }

        if (sapConfig == null) {
            Log.e(TAG, "Invalid SoftApConfiguration. Return.");
            return;
        }

        mProviderClient.updateHostParams(mWifiUtils.getBssid().toString(),
                mWifiUtils.getEtherType());
        /*mProviderClient.enableSapAcs(mWifiUtils.getFrequencies(),
                mWifiUtils.getFrequencies().length);*/
            if(!mLohsCbPen) {
                try {
                    mLohsCbPen = true;
                    mWifiManager.startLocalOnlyHotspot(sapConfig, mExecutor, lohsCb);
                }
                catch (IllegalStateException e) {
                    if (DBG)
                        Log.w(TAG, "handleEnableSap " + e.toString());
                    handleDisableSap();
                }
            } else {
                if(DBG) Log.w(TAG,"handleEnableSap ignore starLohs");
            }
    }

    private boolean handleDisableSap() {
        logDebug("handleDisableSap");
        boolean disableInitiated  = false;
        if (mReservation != null) {
            mLohsClosePending = true;
            mReservation.close();
            disableInitiated = true;
        } else if (mLohsCbPen) {
            mLohsClosePending = true;
            cancelLocalOnlyHotspotRequest();
            disableInitiated = true;
        }
        return disableInitiated;
    }

    boolean isSapEnabled() {
        return (mCurrentState == SAP_ENABLED);
    }

    private boolean isAirplaneMode() {
        return (mCurrentState == SAP_AIRPLANE);
    }

    boolean isSapDisabled() {
        return (mCurrentState == SAP_DISABLED);
    }

    boolean isLowPower() {
        return (mCurrentState == SAP_LOWPOWER);
    }

    private boolean isConnected() {
        return mCurrentState == SAP_CONNECTED;
    }

    void acquireWakeLock() {
        if (mWakeLock == null) {
            // initialize the wakelock
            PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "XpanConnectionStateTimer");
            mWakeLock.setReferenceCounted(false);
            mWakeLock.acquire();
            if (VDBG)
                Log.v(TAG, "acquireWakeLock");
        }
    }

    void releaseWakeLock() {
        if (mWakeLock != null) {
            mWakeLock.release();
            if (VDBG)
                Log.v(TAG, "releaseWakeLock");
            mWakeLock = null;
        }
    }

    boolean isLohsStarted() {
        return ((mCurrentState >= SAP_ENABLED) && (mFrequency > 0));
    }

    /**
     * This API is called when XPan device is connected. When first XPan supporting
     * device is connected, SAP would be enabled using this API. When subsequence devices
     * are connected, this API would return immidiatly with SAP_ENABLED status.
     * @param device BluetoothDevice instance.
     * @param cb XpanSapCallback to be given to the calling module.
     * @return SAP_ENABLED if SAP is already enabled,
     *         SAP_DISABLED if SAP can not be enale because of the error condition,
     *         SAP_ENABLING if SAP turn on procedure is initiated.
     */
    int enableSap(BluetoothDevice device, XpanSapCallback cb) {
        logDebug("enableSap " + device + " mSapIfCreated " + mSapIfCreated);

        if (cb == null) {
            Log.e(TAG, "Should pass valid callback instance");
            return SAP_DISABLED;
        }

        if (!xpanSapCb.containsKey(device)) {
            logDebug("enableSap registering cb " + device);
            xpanSapCb.put(device, cb);
        } else {
            logDebug("enableSap Updated cb " + device);
            xpanSapCb.put(device, cb);
        }

        if (isSapEnabled() || isConnected()) {
            logDebug("SAP is already in enabled state.");
            //callback
            return SAP_ENABLED;
        } else if (isAirplaneMode()) {
            logDebug("SAP in AirplaneMode");
            return SAP_AIRPLANE;
        } else if (isLowPower()) {
            sendMessage(ENABLE_SAP);
            return SAP_ENABLED;
        }
        if (mSapIfCreated) {
            sendMessage(ENABLE_SAP);
        } else {
            sendMessage(CAN_IF_CREATE);
        }
        return SAP_ENABLING;
    }

    /**
     * This API is called when XPan device is disconnected. When last connected XPan
     * device is disconnected, SAP would be disabled using this API. Any previous call
     * to disconnect would not disable SAP unless its last disconnected device.
     * @param device BluetoothDevice instance.
     * @return SAP_DISABLED if SAP is
     *         SAP_DISABLED if SAP can not be enale because of the error condition,
     *         SAP_ENABLING if SAP turn on procedure is initiated.
     */
    int disableSap(BluetoothDevice device) {

        /* If device is not in the map for maintaining SAP in ENABLED state */
        if (!xpanSapCb.containsKey(device)) {
            Log.w(TAG, "callbacks already deregistered.");
            return SAP_DISABLED;
        }

        if (xpanSapCb.size() > 1) {
            xpanSapCb.remove(device);
            return SAP_DISABLED;
        }
        mSapLocalDisable = true;
        sendMessage(DISABLE_SAP);
        return SAP_DISABLING;
    }

    int getFrequency() {
        return mFrequency;
    }

    void onFrequencyUpdated(int frequency) {
        if (DBG)
            Log.d(TAG, "onFrequencyUpdated " + mFrequency + " " + frequency);

        if (frequency > 0 && mFrequency != frequency) {
            mFrequency = frequency;
            sendMessage(SAP_FREQUENCE_UPDATED);
        }
    }

    void onUpdateSapInterface(int reqState, int status) {
        if (DBG)
            Log.d(TAG, "onUpdateSapInterface " + XpanConstants.getSapReqStr(reqState)
                    + " " + XpanConstants.getSapStateStr(status) + " " + mIsRecreate);
        mIfDelPen = false;
        mIfCrPen = false;
        mSapIfCreated = false;
        if (reqState == XpanConstants.SAP_IF_CREATE
                && status == XpanConstants.STATUS_SUCESS) {
            mSapIfCreated = true;
            sendMessage(ENABLE_SAP);
        } else if (reqState == XpanConstants.SAP_IF_CREATE
                && status == XpanConstants.STATUS_ALREADY_PRESENT) {
            sendMessage(RE_CREATE_SAP_INTERFACE);
        } else if (mIsRecreate && reqState == XpanConstants.SAP_IF_DELETE
                && status == XpanConstants.STATUS_SUCESS) {
            sendMessage(CREATE_SAP_INTERFACE);
            mIsRecreate = false;
        } else if (reqState == XpanConstants.SAP_IF_DELETE
                && status == XpanConstants.STATUS_SUCESS) {
            // SAP Interface deleted
            if (isRetrySap()) {
                if (DBG)
                    Log.d(TAG, "CREATE_SAP_INTERFACE CC Updated ");
                sendMessage(CREATE_SAP_INTERFACE);
            }
        } else {
            sendMessage(DISABLE_SAP);
            Log.w(TAG, "onUpdateSapInterface failed");
        }
        if (mSsrCbPending && !mLohsClosePending) {
            updateWifiSSrOrAirplaeMode(SSR_WIFI_COMPLETED);
            mSsrCbPending = false;
        }
    }

    private void sendMacAddressStateUpdate(MacAddress macAddress, boolean isConnected) {
        logDebug("sendMacAddressStateUpdate " + macAddress + " " + isConnected);
        mService.sendMacAddressStateUpdate(macAddress, isConnected);
    }

    void setActiveMode() {
        if (mCurrentState == SAP_LOWPOWER) {
            sendMessage(SAP_STATION_ACTIVE);
        }
    }

    void enableLowPower(BluetoothDevice device, int powerState) {
        if (mCurrentState >= SAP_ENABLED) {
            sendMessage(ENABLE_OR_DISABLE_LOW_POWER, powerState, -1, device);
        } else {
            if (DBG) Log.d(TAG,"enableLowPower ignore " + mCurrentState);
        }
    }

    void onBeaconIntervalsReceived(int id, int powerSaveBi, long nextTsf) {

        DeviceLP deviceLp = mMapDialog.remove(id);
        mCbLpPending = false;
        if (deviceLp == null) {
            Log.w(TAG, "onBeaconIntervalsReceived ignore deviceLp null for " + id);
            return;
        } else if (mCurrentState < SAP_ENABLED) {
            if (DBG)
                Log.d(TAG, "onBeaconIntervalsReceived ignored");
        }

        XpanEvent event = new XpanEvent(XpanEvent.BEACON_INTERVAL_RECEIVED);
        event.setObject(deviceLp);
        event.setArg1(powerSaveBi);
        event.setTbtTsf(nextTsf);
        sendMessage(ON_BEACON_INTERVALS_RECEIVED, event);
    }

    private void updateSapState(int state) {
        mProviderClient.updateSapState(state);
    }

    private void sendAirPlaneModeEnabled(int state) {
        mProviderClient.updateAirPlaneModeChange(state);
    }

    void onAirPlaneModeEnabled(boolean isEnabled) {
        if (VDBG)
            Log.v(TAG, "onAirPlaneModeEnabled " + isEnabled);
        int msg = (isEnabled) ? AIRPLANEMODE_ENABLED : AIRPLANEMODE_DISABLED;
        sendMessage(msg);
    }

    void onSsrWifi(int state) {
        if (VDBG)
            Log.v(TAG, "onSsrWifi " + state);
        int msg = (state == XpanConstants.WIFI_SSR_STARTED) ? SSR_WIFI_STARTED
                : SSR_WIFI_COMPLETED;
        sendMessage(msg);
    }

    void onSapChannelSwitchStarted(XpanEvent event) {
        if (VDBG)
            Log.v(TAG, "onSapChannelSwitchStarted " + event);
        sendMessage(ON_CSS, event);
    }

    void onWifiDrvStatus(int status) {
        if (VDBG)
            Log.v(TAG, "onWifiDrvStatus " + status);
        mWifiStatus = status;
    }

    void onCountryCodeUpdate(String country) {
        if (VDBG)
            Log.v(TAG, "onCountryCodeUpdate " + country);
        String prevContry = mWifiUtils.getCountryCode();
        if (TextUtils.isEmpty(prevContry)) {
            mIsCountryUpdated = false;
        } else if (!prevContry.equalsIgnoreCase(country)) {
            mIsCountryUpdated = true;
        }
        mWifiUtils.setCoutryCode(country);
    }

    void onSapInterfaceUpdate() {
        if (VDBG)
            Log.v(TAG, "onSapInterfaceUpdate");
        sendMessage(ON_IF_UPDATE);
    }

    private String messageToString(int what) {
        switch (what) {
            case ENABLE_SAP:
                return "ENABLE_SAP";
            case DISABLE_SAP:
                return "DISABLE_SAP";
            case SAP_STATE_CHANGED:
                return "SAP_STATE_CHANGED";
            case SAP_STATION_CONNECTED:
                return "SAP_STATION_CONNECTED";
            case SAP_STATION_DISCONNECTED:
                return "SAP_STATION_DISCONNECTED";
            case RE_CREATE_SAP_INTERFACE:
                return "RE_CREATE_SAP_INTERFACE";
            case TIMEOUT_LOWPOWER:
                return "TIMEOUT_LOWPOWER";
            case SAP_STATION_ACTIVE:
                return "SAP_STATION_ACTIVE";
            case SAP_FREQUENCE_UPDATED:
                return "SAP_FREQUENCE_UPDATED";
            case SAP_INTERFACE_READY:
                return "SAP_INTERFACE_READY";
            case CREATE_SAP_INTERFACE:
                return "CREATE_SAP_INTERFACE";
            case ENABLE_OR_DISABLE_LOW_POWER:
                return "ENABLE_OR_DISABLE_LOW_POWER";
            case ON_BEACON_INTERVALS_RECEIVED:
                return "ON_BEACON_INTERVALS_RECEIVED";
            case AIRPLANEMODE_ENABLED:
                return "AIRPLANEMODE_ENABLED";
            case AIRPLANEMODE_DISABLED:
                return "AIRPLANEMODE_DISABLED";
            case SSR_WIFI_STARTED:
                return "SSR_WIFI_STARTED";
            case SSR_WIFI_COMPLETED:
                return "SSR_WIFI_COMPLETED";
            case ON_CSS:
                return "ON_CSS";
            case ON_IF_UPDATE:
                return "ON_IF_UPDATE";
            case CAN_IF_CREATE:
                return "CAN_IF_CREATE";
            default:
                break;
        }
        return Integer.toString(what);
    }

    private String stateToString(int state) {
        switch (state) {
            case SAP_DISABLED:
                return "DISABLED";
            case SAP_ENABLING:
                return "ENABLING";
            case SAP_ENABLED:
                return "ENABLED";
            case SAP_DISABLING:
                return "DISABLING";
            case SAP_CONNECTED:
                return "CONNECTED";
            case SAP_LOWPOWER:
                return "LOWPOWER";
            case SAP_AIRPLANE:
                return "AIRPLANE";
            default:
                return "UN_KNOWN " + state;
        }
    }

    private void logDebug(String msg) {
        if (DBG) {
            Log.d(TAG, msg);
        }
    }

    private void logV(String msg) {
        if (VDBG) {
            Log.v(TAG, msg);
        }
    }

    private void updateCsa(Message message) {
        XpanEvent event = (XpanEvent) message.obj;
        if (VDBG)
            Log.v(TAG, "updateCsa " + event + " " + mFrequency);
        int freq = event.getArg1();
        if (freq > 0 && mFrequency > 0 && freq != mFrequency) {
            mFrequency = freq;
            xpanSapCb.forEach((k, v) -> v.onCsaUpdate(event));
        }
    }

    private void sendFrequncyUpdated() {
        xpanSapCb.forEach((k, v) -> v.onFrequencyUpdated(mFrequency));
    }

    private void sendSapIfUpdate() {
        xpanSapCb.forEach((k, v) -> v.onSapIfStatusUpdate());
    }

    private class DeviceLP implements Serializable {

        private int dialogId;
        private BluetoothDevice device;
        private int lowPwoerState;

        DeviceLP(int dialogId, BluetoothDevice device, int lowPwoerState) {
            this.dialogId = dialogId;
            this.device = device;
            this.lowPwoerState = lowPwoerState;
        }

        int getDialogId() {
            return dialogId;
        }

        BluetoothDevice getDevice() {
            return device;
        }

        boolean isLowPowerENable() {
            return lowPwoerState == XpanConstants.SAP_ENABLE_LOW_POWER;
        }

        boolean isLowPowerDisable() {
            return lowPwoerState == XpanConstants.SAP_DISABLE_LOW_POWER;
        }

        @Override
        public String toString() {
            return "DeviceLP [" + dialogId + " " + device + " " + getPowerState() + "]";
        }

        private String getPowerState() {
            String state = " Unknown";
            if (lowPwoerState == XpanConstants.SAP_ENABLE_LOW_POWER) {
                state = " Enable";
            } else if (lowPwoerState == XpanConstants.SAP_DISABLE_LOW_POWER) {
                state = " Disable";
            }
            return state;
        }

    }

    private void updatedBeaconIntervals(Message msg) {
        XpanEvent event = (XpanEvent) msg.obj;
        int powerSaveBi = event.getArg1();
        long nextTbtTsf = event.getTbtTsf();
        DeviceLP lp = (DeviceLP) event.getObj();
        if (DBG)
            Log.d(TAG, "updatedBeaconIntervals " + event);
        xpanSapCb.forEach(
                (k, v) -> v.onUpdatedBeaconIntervals(powerSaveBi, nextTbtTsf));

        if (powerSaveBi != XpanConstants.GENERAL_BEACON_INT
                && (mCurrentState == SAP_ENABLED || mCurrentState == SAP_CONNECTED)) {
            transitionTo(mSapLowPower);
        } else if (powerSaveBi == XpanConstants.GENERAL_BEACON_INT
                && mCurrentState == SAP_LOWPOWER) {
            if (mConnectedClients.size() > 0) {
                transitionTo(mSapConnected);
            } else if (mConnectedClients.size() == 0) {
                transitionTo(mSapEnabled);
            }
        } else {
            Log.w(TAG, "updatedBeaconIntervals " + event);
        }
    }

    private int getDialogId() {
        if (mDialogId > (Byte.MAX_VALUE - 10)) {
            mDialogId = 0;
        }
       return ++mDialogId;
    }

    boolean isWifiDrvierReady() {
        return mWifiStatus == XpanConstants.WIFI_DRV_ACTIVE;
    }

    private boolean isRetrySap() {
        if (DBG)
            Log.d(TAG, "isRetrySap " + mIsCountryUpdated + " " + mIsSapFailed);
        boolean isRetry = mIsCountryUpdated && mIsSapFailed;
        if (isRetry) {
            mIsCountryUpdated = false;
            mIsSapFailed = false;
        }
        return isRetry;
    }

    void reportCreateInterfaceImpact() {
        if (VDBG)
            Log.d(TAG, "reportCreateInterfaceImpact");
        mWifiUtils.getWifiManager().reportCreateInterfaceImpact(WifiManager.WIFI_INTERFACE_TYPE_AP,
                true, Executors.newSingleThreadExecutor(), bioConsumer);
    }

    boolean isSapLocalDisable() {
        return mSapLocalDisable;
    }

    private void onSapStateChanged(int state) {
        xpanSapCb.forEach((k,v)->v.onSapStateChanged(mCurrentSsid, state));
    }

    private void createInterface() {
        if (VDBG)
            Log.v(TAG, "createInterface " + mIfCrPen);
        mProviderClient.createSapInterface(true);
        mIfCrPen = true;
    }

    private void deleteInterface() {
        if(mIfDelPen) {
            if(VDBG) Log.v(TAG,"deleteInterface Ignore");
            return;
        }
        mProviderClient.createSapInterface(false);
        mIfDelPen = true;
    }

    private void cancelLocalOnlyHotspotRequest() {
        try {
            Class[] cArgs = null;
            ((mWifiManager.getClass()).getDeclaredMethod("cancelLocalOnlyHotspotRequest", cArgs))
                    .invoke((Object) mWifiManager);
            if (DBG)
                Log.d(TAG, "cancelLocalOnlyHotspotRequest");
        } catch (Exception e) {
            Log.e(TAG, "cancelLocalOnlyHotspotRequest " + e);
        }
    }

    boolean isPendingApiCb() {
        if (DBG)
            Log.d(TAG, "isPendingApiCb mLohsCbPen " + mLohsCbPen + " mLohsClosePending "
                    + mLohsClosePending +" mIfCrPen "+mIfCrPen+" mIfDelPen "+mIfDelPen);
        return mLohsCbPen || mLohsClosePending || mIfCrPen || mIfDelPen;
    }

    boolean isCbLpPending() {
        return mCbLpPending;
    }

    boolean isClientConnected(MacAddress addr) {
        return mConnectedClients.contains(addr);
    }

    void resetLpTimer() {
        if (hasMessages(TIMEOUT_LOWPOWER)) {
            if (VDBG)
                Log.v(TAG, "resetLpTimer");
            removeMessages(TIMEOUT_LOWPOWER);
            if (VDBG)
                Log.v(TAG, "resetLpTimer done ");
            sendMessageDelayed(TIMEOUT_LOWPOWER,
                    mUtils.secToMillieSec(XpanConstants.SAP_LOW_POWER_DURATION));
        } else {
            if (VDBG)
                Log.v(TAG, "resetLpTimer ignore");
        }
    }

}
