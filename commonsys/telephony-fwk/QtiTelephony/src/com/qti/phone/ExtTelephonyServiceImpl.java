/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qti.phone;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.ImsiEncryptionInfo;
import android.telephony.PinResult;
import android.telephony.RadioAccessFamily;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.NetworkScanRequest;
import android.util.Log;
import android.util.Pair;

import java.io.FileDescriptor;
import java.lang.Integer;
import java.lang.NumberFormatException;
import java.lang.String;
import java.lang.Thread;
import java.util.Optional;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.OperatorInfo;

import com.qti.extphone.CellularRoamingPreference;
import com.qti.extphone.CiwlanConfig;
import com.qti.extphone.Client;
import com.qti.extphone.ExtPhoneCallbackListener;
import com.qti.extphone.IDepersoResCallback;
import com.qti.extphone.IExtPhone;
import com.qti.extphone.IExtPhoneCallback;
import com.qti.extphone.MsimPreference;
import com.qti.extphone.NrConfig;
import com.qti.extphone.QtiImeiInfo;
import com.qti.extphone.QtiPersoUnlockStatus;
import com.qti.extphone.QtiSetNetworkSelectionMode;
import com.qti.extphone.QtiSimType;
import com.qti.extphone.Token;
import com.qti.phone.esimosupdate.EsimOsUpdateAgent;
import com.qti.phone.nruwbicon.NrUwbConfigsController;
import com.qti.phone.powerupoptimization.PowerUpOptimization;
import com.qti.phone.primarycard.PrimaryCardService;
import com.qti.phone.subsidylock.SubsidyDeviceController;
import com.qti.phone.subsidylock.SubsidyLockUtils;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ExtTelephonyServiceImpl {

    private static final String LOG_TAG = "ExtTelephonyServiceImpl";
    private static Context mContext;
    private static int mNumPhones;
    private static final int DEFAULT_RIL_INSTANCE_ID = 0;
    private static final int SLOT_INVALID = -1;
    private static final int ACTIVE_SIM_SUPPORTED_SINGLE = 1;
    private static final String CONFIG_CURRENT_PRIMARY_SUB = "config_current_primary_sub";
    private static final String MULTI_SIM_SMS_PROMPT = "multi_sim_sms_prompt";
    private static final int NOT_ENABLED = 0;
    private static final int DEFAULT_PHONE_INDEX = 0;
    private static final String PROPERTY_POWER_UP_OPTIMIZATION = "persist.vendor.radio.poweron_opt";
    private static final String PROPERTY_PRIMARY_CARD = "persist.vendor.radio.primarycard";
    private static final String SUBSIDY_DEVICE_PROPERTY_NAME =
            "persist.vendor.radio.subsidydevice";
    private static final String KEY_MSIM_PREFERENCE = "msim_preference";
    private int mClientIndex = -1;
    private static final String EXTRA_STATE = "state";
    private static final String ACTION_RADIO_POWER_STATE_CHANGED =
            "org.codeaurora.intent.action.RADIO_POWER_STATE";
    private static final int QTI_RADIO_CONFIG_HAL_CIWLAN_SUPPORT_VERSION = 4;
    private static final String PROPERTY_DSDS_TO_SS = "persist.vendor.radio.dsds_to_ss";
    private static final String PROPERTY_DSDS_TO_SS_TIMER = "persist.vendor.radio.dsds_to_ss_timer";
    private static final int EVENT_MULTI_SIM_CONFIG_CHANGED = 1;
    private static final int DELAY_MILLIS = 500;
    private static final String PROPERTY_OS_UPDATE_AGENT = "persist.vendor.esim_os_update_agent";
    private HandlerThread mWorkerThread = new HandlerThread(LOG_TAG + "BgThread");
    private Handler mWorkerThreadHandler;

    private boolean mRegisterReceiver = false;
    private final boolean SUCCESS = true;
    private final boolean FAILED = false;

    private int mPowerUpOptimizationPropVal = 0;
    private PowerUpOptimization mPowerUpOptimization;

    private QtiRadioProxy mQtiRadioProxy;
    private QtiRadioConfigProxy mQtiRadioConfigProxy;
    private QtiPrimaryImeiHandler mPrimaryImeiHandler = null;
    private SubscriptionManager mSubscriptionManager;
    private SubsidyDeviceController mSubsidyDevController;
    private TelephonyManager mTelephonyManager;
    private QtiTelephony mQtiTelephony;
    private QtiUiccSwitcher mQtiUiccSwitcher = null;
    private QtiNtnProfileHelper mQtiNtnProfileHelper;
    private final HashMap<IExtPhoneCallback, Set<Integer>> mCallbackList = new HashMap<>();
    private EsimOsUpdateAgent mEsimOsUpdateAgent = null;
    private QtiDeviceConfigController mQtiDeviceConfigController = null;
    private final ConcurrentHashMap<Integer, CiwlanConfig> mCiwlanConfigCache =
            new ConcurrentHashMap<>(2);
    private final ConcurrentHashMap<Integer, Boolean> mCiwlanAvailableCache =
            new ConcurrentHashMap<>(2);
    private NrUwbConfigsController mNrUwbConfigsController;
    private QtiCellularRoamingPreferenceController mQtiCellularRoamingPreferenceController;
    private QtiCiwlanModePreferenceController mQtiCiwlanModePreferenceController;

    public ExtTelephonyServiceImpl(Context context) {
        this(context, null);
    }

    /**
     * Constructor
     *
     * This constructor should only be used for unit tests
     *
     * @param context
     * @param qtiRadioProxy
     */
    @VisibleForTesting
    public ExtTelephonyServiceImpl(Context context, QtiRadioProxy qtiRadioProxy) {
        Log.d(LOG_TAG, "Constructor - Enter");
        mContext = context;
        createWorkerThreadHandler();
        mNumPhones = getPhoneCount();
        QtiMsgTunnelClient.init(mContext);
        if (qtiRadioProxy == null) {
            mQtiRadioProxy = new QtiRadioProxy(mContext, this);
        } else {
            // This else block is executed only during unit tests.
            mQtiRadioProxy = qtiRadioProxy;
            mQtiRadioProxy.init(this);
        }
        mQtiRadioConfigProxy = new QtiRadioConfigProxy(mContext);
        mQtiRadioConfigProxy.setExtTelephonyServiceImpl(this);
        mSubscriptionManager = mContext.getSystemService(SubscriptionManager.class);
        if (mSubscriptionManager != null) {
            mSubscriptionManager = mSubscriptionManager.createForAllUserProfiles();
        }
        mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        mNrUwbConfigsController =
                new NrUwbConfigsController(mContext, Looper.myLooper(), mQtiRadioProxy);
        mQtiCellularRoamingPreferenceController = new QtiCellularRoamingPreferenceController(
                mContext, Looper.myLooper(), mQtiRadioProxy);
        mQtiCiwlanModePreferenceController = new QtiCiwlanModePreferenceController(mContext,
                Looper.myLooper(), mQtiRadioProxy);
        ExtTelephonyThread extTelephonyThread = new ExtTelephonyThread();
        extTelephonyThread.start();
        Log.d(LOG_TAG, "Constructor - Exit");
    }

    protected void cleanUp() {
        if (mPowerUpOptimizationPropVal != NOT_ENABLED && mPowerUpOptimization != null) {
            mPowerUpOptimization.cleanUp();
        }
        if(mRegisterReceiver) {
            mContext.unregisterReceiver(mBroadcastReceiver);
            mRegisterReceiver = false;
        }
        if (mQtiTelephony != null) {
            mQtiTelephony.destroy();
            mQtiTelephony = null;
        }

        if (mNrUwbConfigsController != null) {
            mNrUwbConfigsController.dispose();
        }

        if (mQtiNtnProfileHelper != null) {
            mQtiNtnProfileHelper.cleanUp();
            mQtiNtnProfileHelper = null;
        }
    }

    class ExtTelephonyThread extends Thread {
        public void run() {
            startPowerUpOptimizationIfRequired();
            getCiwlanCapability();
            requestForDualDataCapability();
            initQtiNtnProfileHelper();
            startPrimaryCardServiceIfRequired();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ACTION_RADIO_POWER_STATE_CHANGED);
            intentFilter.addAction(TelephonyManager.ACTION_MULTI_SIM_CONFIG_CHANGED);
            intentFilter.addAction(TelephonyManager.ACTION_SIM_APPLICATION_STATE_CHANGED);
            mContext.registerReceiver(mBroadcastReceiver,intentFilter, Context.RECEIVER_EXPORTED);
            mRegisterReceiver = true;
            makeQtiPrimaryImeiHandler();
            initSubsidyDeviceController();
            makeQtiTelephony();
            makeQtiUiccSwitcher();
            makeQtiDeviceConfigController();
            initCiwlanConfig();
            initCiwlanAvailable();
            makeEsimOsUpdateAgent();
        }
    }

    /**
     * Starts PowerUpOptimization if required.
     * This PowerUpOptimization should be started only when {@link #PROPERTY_POWER_UP_OPTIMIZATION}
     * is enabled.
     */
    private void startPowerUpOptimizationIfRequired() {
        mPowerUpOptimizationPropVal = NOT_ENABLED;
        try {
            mPowerUpOptimizationPropVal = mQtiRadioProxy.
                    getPropertyValueInt(PROPERTY_POWER_UP_OPTIMIZATION, NOT_ENABLED);
        } catch (RemoteException | NullPointerException ex) {
            Log.e(LOG_TAG, "Exception: ", ex);
        }

        if (mPowerUpOptimizationPropVal == NOT_ENABLED) {
            Log.d(LOG_TAG, "PowerUpOptimization is not enabled.");
            return;
        }

        mPowerUpOptimization = PowerUpOptimization.getInstance(mContext,
                mPowerUpOptimizationPropVal);
    }

    /**
     * Starts PrimaryCardService if required.
     * This service should be started only when {@link #PROPERTY_PRIMARY_CARD} is
     * enabled, and the device supports multi-sim configuration.
     */
    private void startPrimaryCardServiceIfRequired() {
        // Check multi-sim configuration
        if (mNumPhones < 2) {
            Log.d(LOG_TAG, "Device is not multi-sim. PrimaryCard is not supported.");
            return;
        }

        // check the value of PROPERTY_PRIMARY_CARD
        boolean isPrimaryCardEnabled = false;
        try {
            isPrimaryCardEnabled = mQtiRadioProxy.
                    getPropertyValueBool(PROPERTY_PRIMARY_CARD, false);
        } catch (RemoteException | NullPointerException ex) {
            Log.e(LOG_TAG, "Exception: ", ex);
        }

        if (!isPrimaryCardEnabled) {
            Log.d(LOG_TAG, "PrimaryCard feature is not enabled.");
            return;
        }

        // Start PrimaryCardService
        Intent serviceIntent = new Intent(mContext, PrimaryCardService.class);
        ComponentName serviceComponent = mContext.startService(serviceIntent);
        if (serviceComponent == null) {
            Log.e(LOG_TAG, "Could not start PrimaryCardService");
        } else {
            Log.d(LOG_TAG, "Successfully started PrimaryCardService");
        }
    }

    private void initSubsidyDeviceController() {
        if (mNumPhones < 2) {
           Log.d(LOG_TAG, "Device should be multi-sim for Subsidyfeature to be supported.");
           return;
        }

        if (!isSubsidyFeatureEnabled()) {
            Log.d(LOG_TAG, "Subsidylock feature is not enabled");
            return;
        }

        if (mSubsidyDevController == null) {
            mSubsidyDevController = new SubsidyDeviceController(mContext);
        }
    }

    boolean isSubsidyFeatureEnabled() {
        boolean isFeatureEnabled = false;
        try {
            isFeatureEnabled = mQtiRadioProxy.getPropertyValueBool(
                    SUBSIDY_DEVICE_PROPERTY_NAME, false);
        } catch (Exception ex) {
            Log.e(LOG_TAG, "Exception: ", ex);
        }
        return isFeatureEnabled;
    }

    private void disposeSubsidyDeviceController() {
        if (mSubsidyDevController != null) {
            mSubsidyDevController.dispose();
            mSubsidyDevController = null;
        }
    }

    public boolean isPrimaryCarrierSlotId(int slotId) {
        return SubsidyLockUtils.isPrimaryCapableSimCard(mContext, slotId);
    }

    private int makeRadioVersion(int major, int minor) {
        if (major < 0 || minor < 0) return 0;
        return major * 100 + minor;
    }

    private boolean isAidlHalAvailable(Context context) {
        final int RADIO_HAL_VERSION_1_6 = makeRadioVersion(1, 6); // Radio HAL Version S

        if (context == null) {
            Log.e(LOG_TAG, "Context is NULL");
            return false;
        }

        TelephonyManager telephonyManager = context.getSystemService(TelephonyManager.class);
        if (telephonyManager == null) {
            Log.e(LOG_TAG, "TelephonyManger is NULL");
            return false;
        }
        Pair<Integer, Integer> radioVersion = telephonyManager.getRadioHalVersion();
        int halVersion = makeRadioVersion(radioVersion.first, radioVersion.second);

        Log.d(LOG_TAG, "isAidlHalAvailable: halVersion = " + halVersion);
        if (halVersion >= RADIO_HAL_VERSION_1_6) {
            return true;
        }
        return false;
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (TelephonyManager.ACTION_MULTI_SIM_CONFIG_CHANGED.equals(intent.getAction())) {
                mNumPhones =
                        intent.getIntExtra(TelephonyManager.EXTRA_ACTIVE_SIM_SUPPORTED_COUNT, 1);
                Log.d(LOG_TAG, "Received ACTION_MULTI_SIM_CONFIG_CHANGED, mNumPhones: " +
                        mNumPhones);

                mQtiRadioProxy.onMultiSimConfigChanged(mNumPhones);

                if (mPowerUpOptimization != null) {
                    mPowerUpOptimization.onMultiSimConfigChanged();
                }
                if (mQtiDeviceConfigController != null) {
                    mQtiDeviceConfigController.onMultiSimConfigChanged(mNumPhones);
                }
                initCiwlanConfig();
                initCiwlanAvailable();
                mWorkerThreadHandler.sendMessageDelayed(mWorkerThreadHandler.
                        obtainMessage(EVENT_MULTI_SIM_CONFIG_CHANGED), DELAY_MILLIS);
            } else if (TelephonyManager.ACTION_SIM_APPLICATION_STATE_CHANGED
                    .equals(intent.getAction())) {
                final int simState = intent.getIntExtra(TelephonyManager.EXTRA_SIM_STATE,
                        TelephonyManager.SIM_STATE_UNKNOWN);
                int apiLevel = SystemProperties.getInt("ro.board.api_level", 0);
                if ((simState == TelephonyManager.SIM_STATE_LOADED)
                        && ((apiLevel >= Build.VERSION_CODES.S) ||
                        ((apiLevel == 0) && isAidlHalAvailable(context)))
                        && mTelephonyManager.getActiveModemCount() > 1
                        && mSubscriptionManager.getActiveSubscriptionInfoCount() > 1 ) {
                    String msimPref = Settings.Global.getString(mContext.getContentResolver(),
                            KEY_MSIM_PREFERENCE);
                    if (msimPref != null) {
                        try {
                            MsimPreference msimPreference = new MsimPreference(
                                    Integer.parseInt(String.valueOf(msimPref)));
                            mQtiRadioConfigProxy.setMsimPreference(msimPreference);
                        } catch (NumberFormatException | RemoteException e) {
                            Log.e(LOG_TAG, "Exception " + e);
                        }
                    }
                }
            } else if (ACTION_RADIO_POWER_STATE_CHANGED.equals(intent.getAction())) {
                int slotIdExtra = intent.getIntExtra(SubscriptionManager.EXTRA_SLOT_INDEX,
                        SubscriptionManager.INVALID_SIM_SLOT_INDEX);
                int radioStateExtra = intent.getIntExtra(EXTRA_STATE,
                        TelephonyManager.RADIO_POWER_UNAVAILABLE);
                handleRadioPowerStateChanged(slotIdExtra, radioStateExtra);
            }
        }
    };

    private void handleRadioPowerStateChanged(int slotId, int radioState) {
        final boolean handleOnce = slotId == DEFAULT_RIL_INSTANCE_ID;
        if (radioState == TelephonyManager.RADIO_POWER_UNAVAILABLE) {
            Log.d(LOG_TAG, "Radio is unavailable for slot " + slotId);
            if (handleOnce) {
                mQtiRadioConfigProxy.invalidateCiwlanCapabilityCache();
                mQtiRadioConfigProxy.invalidateDualDataCapabilityCache();
            }
            mCiwlanConfigCache.clear();
            mCiwlanAvailableCache.clear();
        } else if (radioState == TelephonyManager.RADIO_POWER_ON) {
            Log.d(LOG_TAG, "Radio is available for slot " + slotId);
            if (handleOnce) {
                getCiwlanCapability();
                requestForDualDataCapability();
            }
            loadCiwlanConfig(slotId);
            loadCiwlanAvailable(slotId);
        }
    }

    private boolean isValidSlotIndex(int slotId) {
        return slotId >= 0 && mTelephonyManager != null
                && slotId < mTelephonyManager.getActiveModemCount();
    }

    private void makeQtiPrimaryImeiHandler() {
        Log.d(LOG_TAG, "makeQtiPrimaryImeiHandler " + mPrimaryImeiHandler);
        if (mPrimaryImeiHandler == null && mNumPhones > 1 && QtiRadioFactory.isAidlAvailable()) {
            mPrimaryImeiHandler = new QtiPrimaryImeiHandler(mContext, mQtiRadioProxy);
        }
    }

   private void makeQtiTelephony() {
        Log.d(LOG_TAG, "QtiTelephony ");
        if (mQtiTelephony == null && QtiRadioFactory.isAidlAvailable()) {
            mQtiTelephony = new QtiTelephony(mContext, mQtiRadioProxy);
        }
    }

    private void makeQtiUiccSwitcher() {
        Log.d(LOG_TAG, "makeQtiUiccSwitcher " + mQtiUiccSwitcher);
        if (mQtiUiccSwitcher == null && QtiRadioFactory.isAidlAvailable()) {
            mQtiUiccSwitcher = new QtiUiccSwitcher(mContext, mQtiRadioConfigProxy);
        }
    }

    private boolean isEsimOsUpdateAgentEnabled() {
        int value = 0;
        try {
            value = mQtiRadioProxy.getPropertyValueInt(PROPERTY_OS_UPDATE_AGENT, value);
        } catch (RemoteException ex) {
            Log.e(LOG_TAG, "Exception: ", ex);
        }
        return value == 1;
    }

    private void makeEsimOsUpdateAgent() {
        boolean isEsimOSUpdateAgentEnabled = isEsimOsUpdateAgentEnabled();
        Log.i(LOG_TAG, "EsimOsUpdateAgent " + mEsimOsUpdateAgent + " value = " +
                isEsimOSUpdateAgentEnabled);
        if (isEsimOSUpdateAgentEnabled && mEsimOsUpdateAgent == null) {
            mEsimOsUpdateAgent = new EsimOsUpdateAgent(mContext, mQtiRadioProxy);
        }
    }

    private void makeQtiDeviceConfigController() {
        Log.d(LOG_TAG, "mQtiDeviceConfigController " + mQtiDeviceConfigController);
        int dsdsToSsFeatureValue = getDsdsToSsConfigMode();
        if ((dsdsToSsFeatureValue != 0) && (mQtiDeviceConfigController == null)) {

            int timerValue = -1;
            try {
                timerValue = mQtiRadioProxy.getPropertyValueInt(PROPERTY_DSDS_TO_SS_TIMER, -1);
            } catch (RemoteException | NullPointerException ex) {
                Log.e(LOG_TAG, "Exception: ", ex);
            }
            mQtiDeviceConfigController =
                    new QtiDeviceConfigController(mContext, dsdsToSsFeatureValue, timerValue);
        }
    }

    private int getDsdsToSsConfigMode() {
        int getDsdsToSsConfigMode = 0;
        try {
            getDsdsToSsConfigMode = mQtiRadioProxy.
                    getPropertyValueInt(PROPERTY_DSDS_TO_SS, getDsdsToSsConfigMode);
        } catch (RemoteException | NullPointerException ex) {
            Log.e(LOG_TAG, "Exception: ", ex);
        }
        return getDsdsToSsConfigMode;
    }

    private static boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager =
                (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    private int getPhoneCount() {
        TelephonyManager tm = (TelephonyManager) mContext.
                getSystemService(Context.TELEPHONY_SERVICE);
        return tm.getActiveModemCount();
    }

    private void getCiwlanCapability() {
        try {
            mQtiRadioConfigProxy.getCiwlanCapability();
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "Exception " + e);
        }
    }

    private void requestForDualDataCapability() {
        try {
            mQtiRadioConfigProxy.requestForDualDataCapability();
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "Exception " + e);
        }
    }

    public int getPropertyValueInt(String property, int def) throws RemoteException {
        return mQtiRadioProxy.getPropertyValueInt(property, def);
    }

    public boolean getPropertyValueBool(String property, boolean def) throws RemoteException {
        return mQtiRadioProxy.getPropertyValueBool(property, def);
    }

    public String getPropertyValueString(String property, String def) throws RemoteException {
        return mQtiRadioProxy.getPropertyValueString(property, def);
    }

    public int getCurrentPrimaryCardSlotId() {
        int slotId = Settings.Global.getInt(mContext.getContentResolver(),
                CONFIG_CURRENT_PRIMARY_SUB, SLOT_INVALID);
        Log.d(LOG_TAG, "getCurrentPrimaryCardSlotId slotId="+slotId);
        return slotId;
    }

    public boolean performIncrementalScan(int slotId) {
        return QtiMsgTunnelClient.getInstance().performIncrementalScan(slotId);
    }

    public boolean abortIncrementalScan(int slotId) {
        return QtiMsgTunnelClient.getInstance().abortIncrementalScan(slotId);
    }

    public boolean isSMSPromptEnabled() {
        boolean prompt = false;
        int value = 0;
        try {
            value = Settings.Global.getInt(mContext.getContentResolver(),
                    MULTI_SIM_SMS_PROMPT);
        } catch (SettingNotFoundException snfe) {
            Log.d(LOG_TAG, "Exception Reading Dual Sim SMS Prompt Values");
        }
        prompt = (value == 0) ? false : true ;
        Log.d(LOG_TAG, "isSMSPromptEnabled: SMS Prompt option:" + prompt);
        return prompt;
    }

    public void setSMSPromptEnabled(boolean enabled) {
        int value = (enabled == false) ? 0 : 1;
        Settings.Global.putInt(mContext.getContentResolver(),
                MULTI_SIM_SMS_PROMPT, value);
        Log.d(LOG_TAG, "setSMSPromptEnabled to " + enabled + " Done");
    }

    public void supplyIccDepersonalization(String netpin, String type,
                                           IDepersoResCallback callback, int phoneId) {
        QtiMsgTunnelClient.getInstance().
                supplyIccDepersonalization(netpin, type, callback, phoneId);
    }

    public Token enableEndc(int slot, boolean enable, Client client) throws RemoteException {
        return mQtiRadioProxy.enableEndc(slot, enable, client);
    }

    public Token queryNrIconType(int slot, Client client) throws RemoteException {
        return mQtiRadioProxy.queryNrIconType(slot, client);
    }

    public Token queryEndcStatus(int slot, Client client) throws RemoteException {
        return mQtiRadioProxy.queryEndcStatus(slot, client);
    }

    public Token setNrConfig(int slot, NrConfig config, Client client) throws RemoteException {
        return mQtiRadioProxy.setNrConfig(slot, config, client);
    }

    public Token setNetworkSelectionModeAutomatic(int slot, int accessType, Client client)
            throws RemoteException {
        return mQtiRadioProxy.setNetworkSelectionModeAutomatic(slot, accessType, client);
    }

    public Token getNetworkSelectionMode(int slot, Client client)
            throws RemoteException {
        return mQtiRadioProxy.getNetworkSelectionMode(slot, client);
    }

    public Token queryNrConfig(int slot, Client client) throws RemoteException {
        return mQtiRadioProxy.queryNrConfig(slot, client);
    }

    public Token sendCdmaSms(int slot, byte[] pdu,
                             boolean expectMore, Client client) throws RemoteException {
        return mQtiRadioProxy.sendCdmaSms(slot, pdu, expectMore, client);
    }

    public Token startNetworkScan(int slot, NetworkScanRequest networkScanRequest,
                Client client) throws RemoteException {
        return mQtiRadioProxy.startNetworkScan(slot, networkScanRequest, client);
    }

    public Token stopNetworkScan(int slot, Client client) throws RemoteException {
        return mQtiRadioProxy.stopNetworkScan(slot, client);
    }

    public Token setNetworkSelectionModeManual(int slot, QtiSetNetworkSelectionMode mode,
            Client client) throws RemoteException {
        return mQtiRadioProxy.setNetworkSelectionModeManual(slot, mode, client);
    }

    public Token enable5g(int slot, Client client) throws RemoteException {
        return mQtiRadioProxy.enable5g(slot, client);
    }

    public Token disable5g(int slot, Client client) throws RemoteException {
        return mQtiRadioProxy.disable5g(slot, client);
    }

    public Token queryNrBearerAllocation(int slot, Client client) throws RemoteException {
        return mQtiRadioProxy.queryNrBearerAllocation(slot, client);
    }

    public Token getQtiRadioCapability(int slot, Client client) throws RemoteException {
        return mQtiRadioProxy.getQtiRadioCapability(slot, client);
    }

    public Token enable5gOnly(int slot, Client client) throws RemoteException {
        return mQtiRadioProxy.enable5gOnly(slot, client);
    }

    public Token query5gStatus(int slot, Client client) throws RemoteException {
        return mQtiRadioProxy.query5gStatus(slot, client);
    }

    public Token queryNrDcParam(int slot, Client client) throws RemoteException {
        return mQtiRadioProxy.queryNrDcParam(slot, client);
    }

    public Token queryNrSignalStrength(int slot, Client client) throws RemoteException {
        return mQtiRadioProxy.queryNrSignalStrength(slot, client);
    }

    public Token queryUpperLayerIndInfo(int slot, Client client) throws RemoteException {
        return mQtiRadioProxy.queryUpperLayerIndInfo(slot, client);
    }

    public Token query5gConfigInfo(int slot, Client client) throws RemoteException {
        return mQtiRadioProxy.query5gConfigInfo(slot, client);
    }

    public Token setCarrierInfoForImsiEncryption(int slot, ImsiEncryptionInfo info,
                                                 Client client) throws RemoteException {
        return mQtiRadioProxy.setCarrierInfoForImsiEncryption(slot, info, client);
    }

    public void queryCallForwardStatus(int slotId, int cfReason, int serviceClass,
                String number, boolean expectMore, Client client) throws RemoteException {
        mQtiRadioProxy.queryCallForwardStatus(slotId, cfReason, serviceClass, number,
                expectMore, client);
    }

    public void getFacilityLockForApp(int slotId, String facility, String password,
                int serviceClass, String appId, boolean expectMore, Client client)
                throws RemoteException {
        mQtiRadioProxy.getFacilityLockForApp(slotId, facility, password, serviceClass,
                appId, expectMore, client);
    }

    public QtiImeiInfo[] getImeiInfo() throws RemoteException {
        if (mPrimaryImeiHandler == null) {
            Log.e(LOG_TAG, "getImeiInfo, not supported");
            return null;
        }
        return mPrimaryImeiHandler.getImeiInfo();
    }

    public boolean isSmartDdsSwitchFeatureAvailable() {
        return mQtiRadioProxy.isSmartDdsSwitchFeatureAvailable();
    }

    public void setSmartDdsSwitchToggle(boolean isEnabled, Client client) throws RemoteException {
        mQtiRadioProxy.setSmartDdsSwitchToggle(isEnabled, client);
    }

    public boolean setAirplaneMode(boolean on) {
        boolean result = mTelephonyManager.setRadioPower(!on);
        Log.d(LOG_TAG, "setAirplaneMode result: " + result);
        if (result) {
            // This is so that the UI also reflects the status of the APM
            Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON,
                    on ? 1 : 0);
            final Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
            intent.putExtra("state", on);
            mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        }
        return result;
    }

    public boolean getAirplaneMode() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    public boolean checkSimPinLockStatus(int subId) {
        TelephonyManager tm = mTelephonyManager.createForSubscriptionId(subId);
        return tm.isIccLockEnabled();
    }

    public boolean toggleSimPinLock(int subId, boolean enabled, String pin) {
        TelephonyManager tm = mTelephonyManager.createForSubscriptionId(subId);
        PinResult pinResult = tm.setIccLockEnabled(enabled, pin);
        Log.d(LOG_TAG, "toggleSimPinLock pinResult: " + pinResult);
        return pinResult.getResult() == PinResult.PIN_RESULT_TYPE_SUCCESS;
    }

    public boolean verifySimPin(int subId, String pin) {
        TelephonyManager tm = mTelephonyManager.createForSubscriptionId(subId);
        // If the SIM is pin locked, the supplyPin API returns true when an empty string is passed
        // to it. This check is to return false in that case.
        if (tm.isIccLockEnabled() && pin.isEmpty()) {
            return false;
        }
        boolean result = tm.supplyPin(pin);
        Log.d(LOG_TAG, "verifySimPin result: " + result);
        return result;
    }

    public boolean verifySimPukChangePin(int subId, String puk, String newPin) {
        TelephonyManager tm = mTelephonyManager.createForSubscriptionId(subId);
        boolean result = tm.supplyPuk(puk, newPin);
        Log.d(LOG_TAG, "verifySimPukChangePin result: " + result);
        return result;
    }

    public boolean isFeatureSupported(int feature) {
        return mQtiRadioProxy.isFeatureSupported(feature)
                || mQtiRadioConfigProxy.isFeatureSupported(feature);
    }

    public Token sendUserPreferenceForDataDuringVoiceCall(int slotId,
            boolean userPreference, Client client) throws RemoteException {
        return mQtiRadioProxy.sendUserPreferenceForDataDuringVoiceCall(
                slotId, userPreference, client);
    }

    public Token sendUserPreferenceConfigForDataDuringVoiceCall(boolean[] isAllowedOnSlot,
            Client client) throws RemoteException {
        return mQtiRadioConfigProxy.sendUserPreferenceForDataDuringVoiceCall(
                isAllowedOnSlot, client);
    }

    public Token getDdsSwitchCapability(int slotId, Client client) throws RemoteException {
        return mQtiRadioProxy.getDdsSwitchCapability(slotId, client);
    }

    public Token getDdsSwitchConfigCapability(Client client) throws RemoteException {
        return mQtiRadioConfigProxy.getDdsSwitchCapability(client);
    }

    public boolean isEpdgOverCellularDataSupported(int slotId)
            throws RemoteException {
        //QtiRadioConfig V4 introduces new API to get CIWLAN capability
        if (mQtiRadioConfigProxy.getHalVersion() >=
                QTI_RADIO_CONFIG_HAL_CIWLAN_SUPPORT_VERSION) {
            Optional<Integer> optionalCap = mQtiRadioConfigProxy.getCiwlanCapabilityFromCache();
            if (optionalCap.isPresent()) {
                int capability = optionalCap.get().intValue();
                if (capability == CiwlanCapability.BOTH) return true;
                if (capability == CiwlanCapability.DDS) {
                    final int ddsPhoneId = mSubscriptionManager.getPhoneId(
                            mSubscriptionManager.getDefaultDataSubscriptionId());
                    if (ddsPhoneId != SubscriptionManager.INVALID_PHONE_INDEX
                            && ddsPhoneId == slotId) {
                        return true;
                    }
                }
            } else {
                //Cache is invalid so request modem for future use and return false.
                //Clients will be updated with indication from modem if any change in capability.
                Log.d(LOG_TAG, "Ciwlan Cache is invalid");
                getCiwlanCapability();
            }
            return false;
        } else {
            return mQtiRadioProxy.isEpdgOverCellularDataSupported(slotId);
        }
    }

    public Token getQosParameters(int slotId, int cid, Client client) throws RemoteException {
        return mQtiRadioProxy.getQosParameters(slotId, cid, client);
    }

    public Token getSecureModeStatus(Client client) throws RemoteException {
        return mQtiRadioConfigProxy.getSecureModeStatus(client);
    }

    public Token setMsimPreference(Client client, MsimPreference pref)
            throws RemoteException {
        return mQtiRadioConfigProxy.setMsimPreference(client, pref);
    }

    public QtiSimType[] getCurrentSimType() throws RemoteException {
        if (mQtiUiccSwitcher == null) {
            Log.e(LOG_TAG, "getCurrentSimType, not supported");
            return null;
        }
        return mQtiUiccSwitcher.getCurrentSimType();
    }

    public QtiSimType[] getSupportedSimTypes() throws RemoteException {
        if (mQtiUiccSwitcher == null) {
            Log.e(LOG_TAG, "getSupportedSimTypes, not supported");
            return null;
        }
        return mQtiUiccSwitcher.getSupportedSimTypes();
    }

    public Token setSimType(Client client, QtiSimType[] simType) throws RemoteException {
        if (mQtiUiccSwitcher == null) {
            Log.e(LOG_TAG, "setSimType, not supported");
            return null;
        }
        return mQtiUiccSwitcher.setSimType(client, simType);
    }

    public CiwlanConfig getCiwlanConfig(int slotId) throws RemoteException {
        return getCiwlanConfigfromCache(slotId);
    }

    private CiwlanConfig getCiwlanConfigfromCache(int slotId) {
        if (!mCiwlanConfigCache.containsKey(slotId)) {
            loadCiwlanConfig(slotId);
        }

        if (mCiwlanConfigCache.containsKey(slotId)) {
            return mCiwlanConfigCache.get(slotId);
        } else {
            return null;
        }
    }

    private void loadCiwlanConfig(int slotId) {
        CiwlanConfig ciwlanConfig = null;
        try {
            ciwlanConfig = mQtiRadioProxy.getCiwlanConfig(slotId);
        } catch (RemoteException ex) {
            Log.e(LOG_TAG, "No exception is passed down");
        }
        if (ciwlanConfig != null) {
            Log.d(LOG_TAG, "Cache C_IWLAN config on slot " + slotId);
            mCiwlanConfigCache.put(slotId, ciwlanConfig);
        }
    }

    public void updateCiwlanConfigCache(int slotId, CiwlanConfig ciwlanConfig) {
        if (ciwlanConfig != null) {
            mCiwlanConfigCache.put(slotId, ciwlanConfig);
        }
    }

    private void initCiwlanConfig() {
        mCiwlanConfigCache.clear();
        Log.d(LOG_TAG, "initCiwlanConfig: phoneCount = " + mNumPhones);
        IntStream.range(0, mNumPhones).forEach(i -> loadCiwlanConfig(i));
    }

    public boolean getDualDataCapability() {
        return mQtiRadioConfigProxy.getDualDataCapabilityFromCache();
    }

    public Token setDualDataUserPreference(Client client, boolean preference)
            throws RemoteException {
        return mQtiRadioConfigProxy.setDualDataUserPreference(client, preference);
    }

    public QtiPersoUnlockStatus getSimPersoUnlockStatus(int slotId) throws RemoteException {
        return mQtiRadioProxy.getSimPersoUnlockStatus(slotId);
    }

    public boolean isCiwlanAvailable(int slotId) throws RemoteException {
        if (mCiwlanAvailableCache.containsKey(slotId)) {
            return mCiwlanAvailableCache.get(slotId).booleanValue();
        } else {
            return loadCiwlanAvailable(slotId);
        }
    }

    public Token setCiwlanModeUserPreference(int slotId, Client client, CiwlanConfig ciwlanConfig)
            throws RemoteException {
        return mQtiRadioProxy.setCiwlanModeUserPreference(slotId, client, ciwlanConfig);
    }

    public CiwlanConfig getCiwlanModeUserPreference(int slotId) throws RemoteException {
        return mQtiRadioProxy.getCiwlanModeUserPreference(slotId);
    }

    private boolean loadCiwlanAvailable(int slotId) {
        boolean ciwlanAvailable = false;
        try {
            ciwlanAvailable = mQtiRadioProxy.isCiwlanAvailable(slotId);
            updateCiwlanAvailableCache(slotId, ciwlanAvailable);
        } catch (RemoteException ex) {
            Log.e(LOG_TAG, "RemoteException " + ex);
        }
        return ciwlanAvailable;
    }

    public void updateCiwlanAvailableCache(int slotId, boolean ciwlanAvailable) {
        mCiwlanAvailableCache.put(slotId, Boolean.valueOf(ciwlanAvailable));
    }

    private void initCiwlanAvailable() {
        mCiwlanAvailableCache.clear();
        Log.d(LOG_TAG, "initCiwlanAvailable: phoneCount = " + mNumPhones);
        IntStream.range(0, mNumPhones).forEach(i -> loadCiwlanAvailable(i));
    }

    public boolean isEmcSupported(int slotId) {
        try {
            return mQtiRadioProxy.isEmcSupported(slotId);
        } catch (RemoteException ex) {
            Log.e(LOG_TAG, "RemoteException " + ex);
        }
        return false;
    }

    public boolean isEmfSupported(int slotId) {
        try {
            return mQtiRadioProxy.isEmfSupported(slotId);
        } catch (RemoteException ex) {
            Log.e(LOG_TAG, "RemoteException " + ex);
        }
        return false;
    }

    public CellularRoamingPreference getCellularRoamingPreference(int slotId)
            throws RemoteException {
        return mQtiRadioProxy.getCellularRoamingPreference(slotId);
    }

    public Token setCellularRoamingPreference(Client client, int slotId,
            CellularRoamingPreference pref) throws RemoteException {
        return mQtiRadioProxy.setCellularRoamingPreference(client, slotId, pref);
    }

    public Token queryNrIcon(int slotId, Client client) throws RemoteException {
        return mQtiRadioProxy.queryNrIcon(slotId, client);
    }

    private void initQtiNtnProfileHelper() {
        if (mQtiNtnProfileHelper == null) {
            mQtiNtnProfileHelper = new QtiNtnProfileHelper(mContext, mQtiRadioProxy);
        }
    }

    public Client registerCallbackWithEvents(String packageName, IExtPhoneCallback callback,
            int[] events) throws RemoteException {
        Client client = null;
        IBinder binder = callback.asBinder();

        binder.linkToDeath(new ClientBinderDeathRecipient(callback), 0);

        int uid = Binder.getCallingUid();
        String callerPackageName = mContext.getPackageManager().getNameForUid(uid);
        Log.d(LOG_TAG, "registerCallbackWithEvents: uid = " + uid + " callerPackage=" +
                callerPackageName + "callback = " + callback + "binder = " + binder);
        Set<Integer> eventList = Arrays.stream(events).boxed().collect(Collectors.toSet());

        if (addCallback(callback, eventList) == SUCCESS) {
            client = new Client(++mClientIndex, uid, packageName, callback);
            Log.d(LOG_TAG, "registerCallbackWithEvents: client = " + client);

        } else {
            Log.d(LOG_TAG, "registerCallbackWithEvents: callback could not be added.");
        }
        return client;
    }

    public void unregisterCallback(IExtPhoneCallback callback) throws RemoteException {
        removeCallback(callback);
        mQtiRadioProxy.removeClientFromInflightRequests(callback);
        mQtiRadioConfigProxy.removeClientFromInflightRequests(callback);
    }

    public boolean addCallback(IExtPhoneCallback callback, Set<Integer> eventList) {
        IBinder binder = callback.asBinder();
        synchronized (mCallbackList) {
            for (IExtPhoneCallback it : mCallbackList.keySet()) {
                if (it.asBinder().equals(binder)) {
                    // Found an existing callback with same binder
                    Log.e(LOG_TAG, "Found an existing callback with the same binder: " + callback);
                    return FAILED;
                }
            }
            Log.d(LOG_TAG, "adding callback = " + callback + " eventlist = " + eventList);
            mCallbackList.put(callback, eventList);
        }
        return SUCCESS;
    }

    public void removeCallback(IExtPhoneCallback callback) {
        IBinder binder = callback.asBinder();
        Log.d(LOG_TAG, "removeCallback: callback = " + callback + " binder = " + binder);
        synchronized (mCallbackList) {
            for (IExtPhoneCallback it : mCallbackList.keySet()) {
                if (it.asBinder().equals(binder)) {
                    Log.d(LOG_TAG, "removing callback = " + it);
                    mCallbackList.remove(it);
                    return;
                }
            }
        }
    }

    public boolean isClientValid(Client client) {
        if (client == null || client.getCallback() == null) {
            Log.e(LOG_TAG, "Invalid client");
            return false;
        }
        synchronized (mCallbackList) {
            for (IExtPhoneCallback it : mCallbackList.keySet()) {
                if (it.asBinder().equals(client.getCallback().asBinder())) {
                    return true;
                }
            }
        }
        Log.d(LOG_TAG, "This client is unregistered: " + client);
        return false;
    }

    public ArrayList<IExtPhoneCallback> retrieveCallbacksWithEvent(int tokenKey, int event) {
        ArrayList<IExtPhoneCallback> list = new ArrayList<IExtPhoneCallback>();
        synchronized (mCallbackList) {
            if (tokenKey == Token.UNSOL) {
                for (IExtPhoneCallback it : mCallbackList.keySet()) {
                    if (mCallbackList.get(it).contains(ExtPhoneCallbackListener.EVENT_ALL) ||
                            mCallbackList.get(it).contains(event)) {
                        list.add(it);
                    }
                }
            } else {
                Log.d(LOG_TAG, "This is not an indication");
            }
        }
        Log.d(LOG_TAG, "Retrieved callbacks for event = " + event + " list = " + list);
        return list;
    }

    public void dump(FileDescriptor fd, PrintWriter printwriter, String[] args) {
        PrintWriter pw = printwriter;
        pw.println("AIDL clients : ");
        synchronized (mCallbackList) {
            for (IExtPhoneCallback callback : mCallbackList.keySet()) {
                IBinder binder = callback.asBinder();
                pw.println("Callback = " + callback + "-> Binder = " + binder);
            }
        }
        pw.flush();
        mQtiRadioProxy.dump(fd, pw, args);
        mQtiRadioConfigProxy.dump(fd, pw, args);
    }

    public int getAidlClientsCount() {
        synchronized (mCallbackList) {
            return mCallbackList.size();
        }
    }

    private class ClientBinderDeathRecipient implements IBinder.DeathRecipient {
        IExtPhoneCallback mCallback;

        public ClientBinderDeathRecipient(IExtPhoneCallback callback) {
            Log.d(LOG_TAG, "registering for client cb = " + callback + " binder = "
                    + callback.asBinder() + " death notification");
            mCallback = callback;
        }

        @Override
        public void binderDied() {
            Log.d(LOG_TAG, "Client callback = " + mCallback +" binder = " + mCallback.asBinder() +
                    "died");

            IBinder binder = mCallback.asBinder();
            binder.unlinkToDeath(this, 0);

            try {
                unregisterCallback(mCallback);
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "Exception while unregistering callback = " + mCallback +
                        " binder = " + mCallback.asBinder());
            }
        }
    }

    private void createWorkerThreadHandler() {
        mWorkerThread.start();
        mWorkerThreadHandler = new ExtTelephonyServiceImpl.WorkerHandler(mWorkerThread.getLooper());
    }

    private class WorkerHandler extends Handler {
        private static final String TAG = ExtTelephonyServiceImpl.LOG_TAG + "Handler: ";

        public WorkerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "handleMessage msg.what = " + msg.what);
            switch (msg.what) {
                case EVENT_MULTI_SIM_CONFIG_CHANGED: {
                    Log.d(TAG, "EVENT_MULTI_SIM_CONFIG_CHANGED");
                    if (mNumPhones == ACTIVE_SIM_SUPPORTED_SINGLE) {
                        // The device is no longer multi-sim. Stop PrimaryCardService.
                        if (isServiceRunning(PrimaryCardService.class)) {
                            Log.d(TAG, "Stopping PrimaryCardService");
                            mContext.stopService(new Intent(mContext, PrimaryCardService.class));
                        }
                        if (mPrimaryImeiHandler != null) {
                            mPrimaryImeiHandler.destroy();
                            mPrimaryImeiHandler = null;
                        }
                        disposeSubsidyDeviceController();
                    } else {
                        // Config has changed to multi-sim, which could be 2 or 3
                        if (!isServiceRunning(PrimaryCardService.class)) {
                            startPrimaryCardServiceIfRequired();
                        }
                        makeQtiPrimaryImeiHandler();
                        initSubsidyDeviceController();
                    }
                    break;
                }

                default: {
                    Log.d(TAG, "invalid message = " + msg.what);
                    break;
                }
            }
        }
    }
}
