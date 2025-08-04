/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qti.phone;

import android.annotation.NonNull;
import android.app.AppOpsManager;
import android.content.Context;
import android.hidl.manager.V1_0.IServiceManager;
import android.hidl.manager.V1_0.IServiceNotification;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IHwBinder;
import android.os.IServiceCallback;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.ImsiEncryptionInfo;
import android.telephony.NetworkScanRequest;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.NetworkScanResult;

import com.qti.extphone.BearerAllocationStatus;
import com.qti.extphone.CellularRoamingPreference;
import com.qti.extphone.CiwlanConfig;
import com.qti.extphone.Client;
import com.qti.extphone.ExtPhoneCallbackListener;
import com.qti.extphone.IExtPhoneCallback;
import com.qti.extphone.DcParam;
import com.qti.extphone.IExtPhoneCallback;
import com.qti.extphone.NetworkSelectionMode;
import com.qti.extphone.NrConfig;
import com.qti.extphone.NrConfigType;
import com.qti.extphone.NrIcon;
import com.qti.extphone.NrIconType;
import com.qti.extphone.QRadioResponseInfo;
import com.qti.extphone.QosParametersResult;
import com.qti.extphone.QtiCallForwardInfo;
import com.qti.extphone.QtiImeiInfo;
import com.qti.extphone.QtiPersoUnlockStatus;
import com.qti.extphone.QtiSetNetworkSelectionMode;
import com.qti.extphone.SignalStrength;
import com.qti.extphone.SmsResult;
import com.qti.extphone.Status;
import com.qti.extphone.Token;
import com.qti.extphone.UpperLayerIndInfo;

import com.qti.phone.QtiNtnProfileHelper.QtiNtnProfileCallback;

import vendor.qti.hardware.data.dynamicdds.V1_0.ISubscriptionManager;
import vendor.qti.hardware.data.dynamicdds.V1_0.IToken;
import vendor.qti.hardware.data.dynamicdds.V1_0.StatusCode;
import vendor.qti.hardware.radio.qtiradio.NrUwbIconBandInfo;
import vendor.qti.hardware.radio.qtiradio.NrUwbIconBandwidthInfo;
import vendor.qti.hardware.radio.qtiradio.NrUwbIconRefreshTime;

import static java.util.Arrays.copyOf;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

public class QtiRadioProxy {

    private static final String TAG = "QtiRadioProxy";

    private static final int DEFAULT_PHONE_INDEX = 0;
    private static final int EVENT_ON_NR_ICON_TYPE = 1;
    private static final int EVENT_ON_ENABLE_ENDC = 2;
    private static final int EVENT_ON_ENDC_STATUS = 3;
    private static final int EVENT_ON_SET_NR_CONFIG = 4;
    private static final int EVENT_ON_NR_CONFIG_STATUS = 5;
    private static final int EVENT_ON_BEARER_ALLOCATION_CHANGE_IND = 6;
    private static final int EVENT_ON_5G_ENABLE_STATUS_CHANGE_IND = 7;
    private static final int EVENT_ON_NR_DC_PARAM = 8;
    private static final int EVENT_ON_UPPER_LAYER_IND_INFO = 9;
    private static final int EVENT_ON_5G_CONFIG_INFO = 10;
    private static final int EVENT_ON_SIGNAL_STRENGTH = 11;
    private static final int EVENT_QTI_RADIO_CAPABILITY_RESPONSE = 12;
    private static final int EVENT_SEND_CDMA_SMS_RESPONSE = 13;
    private static final int EVENT_SEND_CARRIER_INFO_RESPONSE = 14;
    private static final int EVENT_CALL_FORWARD_QUERY_RESPONSE = 15;
    private static final int EVENT_FACILITY_LOCK_QUERY_RESPONSE = 16;
    private static final int EVENT_SMART_DDS_SWITCH_TOGGLE = 17;
    private static final int EVENT_ON_SMART_DDS_SWITCH_TOGGLE_RESPONSE = 18;
    private static final int EVENT_GET_IMEI_RESPONSE = 19;
    private static final int EVENT_IMEI_CHANGE_IND_INFO = 20;
    private static final int EVENT_ON_ALLOW_MODEM_RECOMMENDATION_FOR_DATA_DURING_CALL = 21;
    private static final int EVENT_ON_DDS_SWITCH_CAPABILITY_CHANGE = 22;
    private static final int EVENT_ON_AUTO_DDS_SWITCH_CHANGE = 23;
    private static final int EVENT_ON_DDS_SWITCH_RECOMMENDATION = 24;
    private static final int EVENT_ON_DATA_DEACTIVATE_DELAY_TIME = 25;
    private static final int EVENT_ON_EPDG_OVER_CELLULAR_DATA_SUPPORTED = 26;
    private static final int EVENT_ON_MCFG_REFRESH = 27;
    private static final int EVENT_ON_SET_NR_ULTRA_WIDEBAND_CONFIG_RESPONSE = 28;
    private static final int EVENT_START_NETWORK_SCAN_RESPONSE = 29;
    private static final int EVENT_NETWORK_SCAN_RESULT = 30;
    private static final int EVENT_STOP_NETWORK_SCAN_RESPONSE = 31;
    private static final int EVENT_START_NETWORK_SELECTION_MODE_MANUAL_RESPONSE = 32;
    private static final int EVENT_START_NETWORK_SELECTION_MODE_AUTOMATIC_RESPONSE = 33;
    private static final int EVENT_GET_NETWORK_SELECTION_MODE_RESPONSE = 34;
    private static final int EVENT_GET_QOS_PARAMETERS_RESPONSE = 35;
    private static final int EVENT_ON_QOS_PARAMETERS_CHANGED = 36;
    private static final int EVENT_SIM_PERSO_UNLOCK_STATUS_CHANGE = 37;
    private static final int EVENT_ON_CIWLAN_AVAILABLE = 38;
    private static final int EVENT_SET_CIWLAN_MODE_USER_PREFERENCE_RESPONSE = 39;
    private static final int EVENT_ON_CIWLAN_CONFIG_CHANGE =  40;
    private static final int EVENT_SET_CELLULAR_ROAMING_PREFERENCE_RESPONSE =  41;
    private static final int EVENT_ON_NR_ICON_CHANGE = 42;
    private static final int EVENT_QUERY_NR_ICON_RESPONSE = 43;
    private static final int EVENT_GET_ALL_ESIM_PROFILES = 44;
    private static final int EVENT_ENABLE_ESIM_PROFILE = 45;
    private static final int EVENT_DISABLE_ESIM_PROFILE = 46;
    private static final int EVENT_GET_ALL_ESIM_PROFILES_RESPONSE = 47;
    private static final int EVENT_DISABLE_ESIM_PROFILE_RESPONSE = 48;
    private static final int EVENT_ENABLE_ESIM_PROFILE_RESPONSE = 49;

    private final int SMART_DDS_SWITCH_OFF = 0;
    private final int SMART_DDS_SWITCH_ON = 1;

    private boolean SUCCESS = true;
    private boolean FAILED = false;

    private final String CNE_FACTORY_SERVICE_NAME = "vendor.qti.data.factory@2.4::IFactory";
    private final String CNE_FACTORY_SERVICE_INSTANCE_NAME = "default";

    private boolean mIsFactoryAidlAvailable = false;
    private static vendor.qti.hardware.data.dynamicddsaidlservice.
            ISubscriptionManager sAidlDynamicSubscriptionManager;

    private static Context mContext;
    private IQtiRadioConnectionInterface[] mQtiRadio;
    private static volatile int mSerial = -1;
    private AppOpsManager mAppOpsManager;
    private HandlerThread mWorkerThread = new HandlerThread(TAG + "BgThread");
    private Handler mWorkerThreadHandler;
    private static final long mDeathBinderCookie = Integer.MAX_VALUE;
    private static ISubscriptionManager sDynamicSubscriptionManager;
    private static boolean mCneDataFactoryAvailable = false;
    private final ServiceNotification mServiceNotification = new ServiceNotification();
    private ConcurrentHashMap<Integer, Transaction> mInflightRequests = new
            ConcurrentHashMap<Integer, Transaction>();
    private ArrayList<IQtiRadioInternalCallback> mInternalCallbackList = new
            ArrayList<IQtiRadioInternalCallback>();
    private ExtTelephonyServiceImpl mExtTelephonyServiceImpl;
    private int mEsimProfileIndCache[] = new int[] {-1, -1};

    private final IServiceCallback mServiceCallback = new ServiceCallback();
    private static final String CNE_FACTORY_SAIDL_SERVICE_NAME =
            "vendor.qti.data.factoryservice.IFactory/default";

    // this property is used only for test purpose.
    private static final String PROPERTY_SMART_DDS_SWITCH = "persist.vendor.radio.smart_dds_switch";

    private class WorkerHandler extends Handler {
        private static final String TAG = QtiRadioProxy.TAG + "Handler: ";

        public WorkerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "handleMessage msg.what = " + msg.what);
            switch (msg.what) {
                case EVENT_SMART_DDS_SWITCH_TOGGLE: {
                    Log.d(TAG, "EVENT_SMART_DDS_SWITCH_TOGGLE mIsFactoryAidlAvailable: "
                            + mIsFactoryAidlAvailable);
                    Result result = (Result) msg.obj;
                    Token token = result.mToken;
                    boolean isEnabled = (int) result.mData == SMART_DDS_SWITCH_ON;
                    if (mIsFactoryAidlAvailable) {
                        setAidlDynamicSubscriptionChange(token, isEnabled);
                    } else {
                        setDynamicSubscriptionChange(token, isEnabled);
                    }
                    break;
                }

                case EVENT_ON_SMART_DDS_SWITCH_TOGGLE_RESPONSE: {
                    Log.d(TAG, "EVENT_ON_SMART_DDS_SWITCH_TOGGLE_RESPONSE");
                    Token token = (Token) msg.obj;
                    int status =  msg.arg1;
                    int toggleValue = msg.arg2;
                    setSmartDdsSwitchToggleResponse(token, status, toggleValue);
                    break;
                }

                case EVENT_ON_NR_ICON_TYPE: {
                    Log.d(TAG, "EVENT_ON_NR_ICON_TYPE");
                    int slotId = msg.arg1;
                    Result result = (Result) msg.obj;
                    onNrIconType(slotId, result.mToken, result.mStatus,
                            (NrIconType) result.mData);
                    break;
                }

                case EVENT_ON_ENABLE_ENDC: {
                    Log.d(TAG, "EVENT_ON_ENABLE_ENDC");
                    int slotId = msg.arg1;
                    Result result = (Result) msg.obj;
                    onEnableEndc(slotId, result.mToken, result.mStatus);
                    break;
                }

                case EVENT_ON_ENDC_STATUS: {
                    Log.d(TAG, "EVENT_ON_ENDC_STATUS");
                    int slotId = msg.arg1;
                    Result result = (Result) msg.obj;
                    onEndcStatus(slotId, result.mToken, result.mStatus, (boolean) result.mData);
                    break;
                }

                case EVENT_ON_SET_NR_CONFIG: {
                    Log.d(TAG, "EVENT_ON_SET_NR_CONFIG");
                    int slotId = msg.arg1;
                    Result result = (Result) msg.obj;
                    onSetNrConfig(slotId, result.mToken, result.mStatus);
                    break;
                }

                case EVENT_ON_NR_CONFIG_STATUS: {
                    Log.d(TAG, "EVENT_ON_NR_CONFIG_STATUS");
                    int slotId = msg.arg1;
                    Result result = (Result) msg.obj;
                    onNrConfigStatus(slotId, result.mToken, result.mStatus,
                            (NrConfig) result.mData);
                    break;
                }

                case EVENT_ON_5G_ENABLE_STATUS_CHANGE_IND: {
                    Log.d(TAG, "EVENT_ON_5G_ENABLE_STATUS");
                    int slotId = msg.arg1;
                    Result result = (Result) msg.obj;
                    on5gStatus(slotId, result.mToken, result.mStatus, (boolean) result.mData);
                    break;
                }

                case EVENT_ON_BEARER_ALLOCATION_CHANGE_IND: {
                    Log.d(TAG, "EVENT_ON_BEARER_ALLOCATION_CHANGE_IND");
                    int slotId = msg.arg1;
                    Result result = (Result) msg.obj;
                    onAnyNrBearerAllocation(slotId, result.mToken, result.mStatus,
                            (BearerAllocationStatus) result.mData);
                    break;
                }

                case EVENT_ON_NR_DC_PARAM: {
                    Log.d(TAG, "EVENT_ON_NR_DC_PARAM");
                    int slotId = msg.arg1;
                    Result result = (Result) msg.obj;
                    onNrDcParam(slotId, result.mToken, result.mStatus,
                            (DcParam) result.mData);
                    break;
                }

                case EVENT_ON_UPPER_LAYER_IND_INFO: {
                    Log.d(TAG, "EVENT_ON_UPPER_LAYER_IND_INFO");
                    int slotId = msg.arg1;
                    Result result = (Result) msg.obj;
                    onUpperLayerIndInfo(slotId, result.mToken, result.mStatus,
                            (UpperLayerIndInfo) result.mData);
                    break;
                }

                case EVENT_ON_5G_CONFIG_INFO: {
                    Log.d(TAG, "EVENT_ON_5G_CONFIG_INFO");
                    int slotId = msg.arg1;
                    Result result = (Result) msg.obj;
                    on5gConfigInfo(slotId, result.mToken, result.mStatus,
                            (NrConfigType) result.mData);
                    break;
                }

                case EVENT_ON_SIGNAL_STRENGTH: {
                    Log.d(TAG, "EVENT_ON_SIGNAL_STRENGTH");
                    int slotId = msg.arg1;
                    Result result = (Result) msg.obj;
                    onSignalStrength(slotId, result.mToken, result.mStatus,
                            (SignalStrength) result.mData);
                    break;
                }

                case EVENT_QTI_RADIO_CAPABILITY_RESPONSE: {
                    Log.d(TAG, "EVENT_QTI_RADIO_CAPABILITY_RESPONSE");
                    int slotId = msg.arg1;
                    Result result = (Result) msg.obj;
                    getQtiRadioCapabilityResponse(slotId, result.mToken, result.mStatus,
                            (int) result.mData);
                    break;
                }

                case EVENT_SEND_CDMA_SMS_RESPONSE: {
                    Log.d(TAG, "EVENT_SEND_CDMA_SMS_RESPONSE");
                    int slotId = msg.arg1;
                    Result result = (Result) msg.obj;
                    sendCdmaSmsResponse(slotId, result.mToken, result.mStatus,
                            (SmsResult) result.mData);
                    break;
                }

                case EVENT_SEND_CARRIER_INFO_RESPONSE: {
                    Log.d(TAG, "EVENT_SEND_CARRIER_INFO_RESPONSE");
                    int slotId = msg.arg1;
                    Result result = (Result) msg.obj;
                    setCarrierInfoForImsiEncryptionResponse(slotId, result.mToken, result.mStatus,
                            (QRadioResponseInfo) result.mData);
                    break;
                }

                case EVENT_CALL_FORWARD_QUERY_RESPONSE: {
                    Log.d(TAG, "EVENT_CALL_FORWARD_QUERY_RESPONSE");
                    Result result = (Result) msg.obj;
                    sendcallforwardqueryResponse(result.mToken, result.mStatus,
                            (QtiCallForwardInfo[]) result.mData);
                    break;
                }

                case EVENT_FACILITY_LOCK_QUERY_RESPONSE: {
                    Log.d(TAG, "EVENT_FACILITY_LOCK_RESPONSE");
                    Result result = (Result) msg.obj;
                    sendfacilityLockResponse(result.mToken, result.mStatus, (int[]) result.mData);
                    break;
                }

                case EVENT_GET_IMEI_RESPONSE: {
                    Log.d(TAG, "EVENT_GET_IMEI_RESPONSE");
                    int slotId = msg.arg1;
                    Result result = (Result) msg.obj;
                    sendImeiInfoResponse(slotId, result.mToken, result.mStatus,
                            (QtiImeiInfo) result.mData);
                    break;
                }

                case EVENT_IMEI_CHANGE_IND_INFO: {
                    Log.d(TAG, "EVENT_IMEI_CHANGE_IND_INFO");
                    int slotId = msg.arg1;
                    Result result = (Result) msg.obj;
                    sendImeiInfoIndInternal(slotId, (QtiImeiInfo) result.mData);
                    break;
                }

                case EVENT_ON_ALLOW_MODEM_RECOMMENDATION_FOR_DATA_DURING_CALL: {
                    int slotId = msg.arg1;
                    Result result = (Result) msg.obj;
                    onSendUserPreferenceForDataDuringVoiceCall(slotId, result.mToken,
                            result.mStatus);
                    break;
                }

                case EVENT_ON_DDS_SWITCH_CAPABILITY_CHANGE: {
                    int slotId = msg.arg1;
                    Result result = (Result) msg.obj;
                    onDdsSwitchCapabilityChange(slotId, result.mToken, result.mStatus,
                            (boolean) result.mData);
                    break;
                }

                case EVENT_ON_AUTO_DDS_SWITCH_CHANGE: {
                    int slotId = msg.arg1;
                    Result result = (Result) msg.obj;
                    onDdsSwitchCriteriaChange(slotId, result.mToken,
                            (boolean) result.mData);
                    break;
                }

                case EVENT_ON_DDS_SWITCH_RECOMMENDATION: {
                    int slotId = msg.arg1;
                    Result result = (Result) msg.obj;
                    onDdsSwitchRecommendation(slotId, result.mToken,
                            (int) result.mData);
                    break;
                }

                case EVENT_ON_DATA_DEACTIVATE_DELAY_TIME: {
                    int slotId = msg.arg1;
                    Result result = (Result) msg.obj;
                    onDataDeactivateDelayTime(slotId, result.mToken,
                            (long) result.mData);
                    break;
                }

                case EVENT_ON_EPDG_OVER_CELLULAR_DATA_SUPPORTED: {
                    int slotId = msg.arg1;
                    Result result = (Result) msg.obj;
                    onEpdgOverCellularDataSupported(slotId, result.mToken,
                            (boolean) result.mData);
                    break;
                }

                case EVENT_ON_MCFG_REFRESH: {
                    Log.d(TAG, "EVENT_ON_MCFG_REFRESH");
                    Result result = (Result) msg.obj;
                    sendMcfgRefreshInfo(result.mToken, (QtiMcfgRefreshInfo) result.mData);
                    break;
                }

                case EVENT_ON_SET_NR_ULTRA_WIDEBAND_CONFIG_RESPONSE: {
                    Log.d(TAG, "EVENT_ON_SET_NR_ULTRA_WIDEBAND_CONFIG_RESPONSE");
                    Result result = (Result) msg.obj;
                    onSetNrUltraWidebandIconConfigResponse(msg.arg1, result.mToken);
                    break;
                }

                case EVENT_START_NETWORK_SCAN_RESPONSE: {
                    Log.d(TAG, "EVENT_START_NETWORK_SCAN_RESPONSE");
                    int slotId = msg.arg1;
                    Result result = (Result) msg.obj;
                    startNetworkScanResponse(slotId, result.mToken, (int) result.mData);
                    break;
                }

                case EVENT_STOP_NETWORK_SCAN_RESPONSE: {
                    Log.d(TAG, "EVENT_STOP_NETWORK_SCAN_RESPONSE");
                    int slotId = msg.arg1;
                    Result result = (Result) msg.obj;
                    stopNetworkScanResponse(slotId, result.mToken, (int) result.mData);
                    break;
                }

                case EVENT_START_NETWORK_SELECTION_MODE_MANUAL_RESPONSE: {
                    Log.d(TAG, "EVENT_START_NETWORK_SELECTION_MODE_MANUAL_RESPONSE");
                    int slotId = msg.arg1;
                    Result result = (Result) msg.obj;
                    setNetworkSelectionModeManualResponse(slotId, result.mToken,
                            (int) result.mData);
                    break;
                }

                case EVENT_START_NETWORK_SELECTION_MODE_AUTOMATIC_RESPONSE: {
                    Log.d(TAG, "EVENT_START_NETWORK_SELECTION_MODE_AUTOMATIC_RESPONSE");
                    int slotId = msg.arg1;
                    Result result = (Result) msg.obj;
                    setNetworkSelectionModeAutomaticResponse(slotId, result.mToken,
                            (int) result.mData);
                    break;
                }

                case EVENT_GET_NETWORK_SELECTION_MODE_RESPONSE: {
                    Log.d(TAG, "EVENT_GET_NETWORK_SELECTION_MODE_RESPONSE");
                    int slotId = msg.arg1;
                    Result result = (Result) msg.obj;
                    getNetworkSelectionModeResponse(slotId, result.mToken, result.mStatus,
                            (NetworkSelectionMode) result.mData);
                    break;
                }

                case EVENT_NETWORK_SCAN_RESULT: {
                    Log.d(TAG, "EVENT_NETWORK_SCAN_RESULT");
                    int slotId = msg.arg1;
                    Result result = (Result) msg.obj;
                    networkScanResult(slotId, result.mToken, (NetworkScanResult) result.mData);
                    break;
                }

                case EVENT_GET_QOS_PARAMETERS_RESPONSE: {
                    Log.d(TAG, "EVENT_GET_QOS_PARAMETERS_RESPONSE");
                    int slotId = msg.arg1;
                    Result result = (Result) msg.obj;
                    getQosParametersResponse(slotId, result.mToken, result.mStatus,
                            (QosParametersResult) result.mData);
                    break;
                }

                case EVENT_ON_QOS_PARAMETERS_CHANGED: {
                    Log.d(TAG, "EVENT_ON_QOS_PARAMETERS_CHANGED");
                    int slotId = msg.arg1;
                    int cid = msg.arg2;
                    Result result = (Result) msg.obj;
                    onQosParametersChanged(slotId, cid, (QosParametersResult) result.mData);
                    break;
                }

                case EVENT_SIM_PERSO_UNLOCK_STATUS_CHANGE: {
                    Log.d(TAG, "EVENT_SIM_PERSO_UNLOCK_STATUS_CHANGE");
                    int slotId = msg.arg1;
                    Result result = (Result) msg.obj;
                    sendSimPersoUnlockStatusChange(slotId, (QtiPersoUnlockStatus) result.mData);
                    break;
                }

                case EVENT_ON_CIWLAN_AVAILABLE: {
                    Log.d(TAG, "EVENT_ON_CIWLAN_AVAILABLE");
                    int slotId = msg.arg1;
                    Result result = (Result) msg.obj;
                    onCiwlanAvailable(slotId, (boolean) result.mData);
                    break;
                }

                case EVENT_SET_CIWLAN_MODE_USER_PREFERENCE_RESPONSE: {
                    Log.d(TAG, "EVENT_SET_CIWLAN_MODE_USER_PREFERENCE_RESPONSE");
                    int slotId = msg.arg1;
                    Result result = (Result) msg.obj;
                    setCiwlanModeUserPreferenceResponse(slotId, result.mToken, result.mStatus);
                    break;
                }

                case EVENT_ON_CIWLAN_CONFIG_CHANGE: {
                    Log.d(TAG, "EVENT_ON_CIWLAN_CONFIG_CHANGE");
                    int slotId = msg.arg1;
                    Result result = (Result) msg.obj;
                    onCiwlanConfigChange(slotId, (CiwlanConfig) result.mData);
                    break;
                }

                case EVENT_SET_CELLULAR_ROAMING_PREFERENCE_RESPONSE: {
                    Log.d(TAG, "EVENT_SET_CELLULAR_ROAMING_PREFERENCE_RESPONSE");
                    Result result = (Result) msg.obj;
                    setCellularRoamingPreferenceResponse(msg.arg1, result.mToken, result.mStatus);
                    break;
                }

                case EVENT_ON_NR_ICON_CHANGE: {
                    Log.d(TAG, "EVENT_ON_NR_ICON_CHANGE");
                    Result result = (Result) msg.obj;
                    onNrIconChange(msg.arg1, (NrIcon) result.mData);
                    break;
                }

                case EVENT_QUERY_NR_ICON_RESPONSE: {
                    Log.d(TAG, "EVENT_QUERY_NR_ICON_RESPONSE");
                    Result result = (Result) msg.obj;
                    onNrIconResponse(msg.arg1, result.mToken, result.mStatus,
                            (NrIcon) result.mData);
                    break;
                }

                case EVENT_GET_ALL_ESIM_PROFILES: {
                    Log.d(TAG, "EVENT_GET_ALL_ESIM_PROFILES");
                    onGetAllEsimProfiles(msg.arg1 /* slotId */, msg.arg2 /* refNum */);
                    break;
                }

                case EVENT_ENABLE_ESIM_PROFILE: {
                    Log.d(TAG, "EVENT_ENABLE_ESIM_PROFILE");
                    Result res = (Result) msg.obj;
                    onEnableEsimProfile(msg.arg1 /* slotId */, msg.arg2 /* refNum */,
                            (String) res.mData /* iccid */);
                    break;
                }

                case EVENT_DISABLE_ESIM_PROFILE: {
                    Log.d(TAG, "EVENT_DISABLE_ESIM_PROFILE");
                    Result res = (Result) msg.obj;
                    onDisableEsimProfile(msg.arg1 /* slotId */, msg.arg2 /* refNum */,
                            (String) res.mData /* iccid */);
                    break;
                }

                case EVENT_GET_ALL_ESIM_PROFILES_RESPONSE:
                case EVENT_DISABLE_ESIM_PROFILE_RESPONSE:
                case EVENT_ENABLE_ESIM_PROFILE_RESPONSE: {
                    Result result = (Result) msg.obj;
                    Log.d(TAG, "tokenkey = " + result.mToken.get());
                    mInflightRequests.remove(result.mToken.get());
                    break;
                }
            }
        }
    }

    static class Result {
        Token mToken;
        Status mStatus;
        Object mData;

        public Result(Token mToken, Status mStatus, Object mData) {
            this.mToken = mToken;
            this.mStatus = mStatus;
            this.mData = mData;
        }

        @Override
        public String toString() {
            return "Result{" + "mToken=" + mToken + ", mStatus=" + mStatus + ", mData=" + mData +
                    '}';
        }
    }

    class Transaction {
        Token mToken;
        String mName;
        Client mClient;

        public Transaction(Token token, String name, Client client) {
            mToken = token;
            mName = name;
            mClient = client;
        }

        @Override
        public String toString() {
            return "Transaction{" + "mToken=" + mToken + ", mName='" + mName + '\'' + ", mClient="
                    + mClient + '}';
        }
    }

    static Token getNextToken() {
        return new Token(++mSerial);
    }

    IQtiRadioConnectionCallback mQtiRadioCallback = new IQtiRadioConnectionCallback() {
        @Override
        public void onNrIconType(int slotId, Token token, Status status,
                                 NrIconType nrIconType) {
            Log.d(TAG, "onNrIconType slotId = " + slotId + " NrIconType = " + nrIconType);
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_ON_NR_ICON_TYPE, slotId, -1, new Result(token, status,
                            nrIconType)));
        }

        @Override
        public void onEnableEndc(int slotId, Token token, Status status) {
            Log.d(TAG, "onEnableEndc slotId = " + slotId + " token = " + token + " status = " +
                    status);

            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_ON_ENABLE_ENDC, slotId, -1, new Result(token, status, null)));
        }

        @Override
        public void onEndcStatus(int slotId, Token token, Status status, boolean enableStatus) {
            Log.d(TAG, "onEndcStatus slotId = " + slotId + " token = " + token + " status = " +
                    status + " enable = " + enableStatus);

            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_ON_ENDC_STATUS, slotId, -1, new Result(token, status,
                            enableStatus)));
        }

        @Override
        public void onSetNrConfig(int slotId, Token token, Status status) {
            Log.d(TAG,"setNrConfigStatus: slotId = " + slotId + " token = " + token + " status= " +
                    status);
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_ON_SET_NR_CONFIG, slotId, -1, new Result(token, status, null)));
        }

        @Override
        public void onNrConfigStatus(int slotId, Token token, Status status, NrConfig nrConfig) {
            Log.d(TAG, "onNrConfigStatus: slotId = " + slotId + " token = " + token + " status= " +
                    status + " nrConfig = " + nrConfig);
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_ON_NR_CONFIG_STATUS, slotId, -1, new Result(token, status, nrConfig)));
        }

        @Override
        public void setCarrierInfoForImsiEncryptionResponse(int slotId,
                Token token, Status status, QRadioResponseInfo info) {
            Log.d(TAG, "setCarrierInfoForImsiEncryptionResponse: slotId = " + slotId +
                    " info = " + info);
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_SEND_CARRIER_INFO_RESPONSE, slotId, -1,
                            new Result(token, status, info)));
        }

        @Override
        public void on5gStatus(int slotId, Token token, Status status, boolean enableStatus) {
            Log.d(TAG, "on5gStatus slotId = " + slotId + " token = " + token + " status = " +
                    status + " enableStatus = " + enableStatus);
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_ON_5G_ENABLE_STATUS_CHANGE_IND, slotId, -1, new Result(token, status,
                            enableStatus)));
        }

        @Override
        public void onAnyNrBearerAllocation(int slotId, Token token, Status status,
                                            BearerAllocationStatus bearerStatus) {
            Log.d(TAG, "onAnyNrBearerAllocation slotId = " + slotId +
                    " bearerStatus = " + bearerStatus);
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_ON_BEARER_ALLOCATION_CHANGE_IND, slotId, -1, new Result(token, status,
                            bearerStatus)));
        }

        @Override
        public void onNrDcParam(int slotId, Token token, Status status,
                                            DcParam dcParam) {
            Log.d(TAG, "onNrDcParam slotId = " + slotId +
                    " dcParam = " + dcParam);
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_ON_NR_DC_PARAM, slotId, -1, new Result(token, status,
                            dcParam)));
        }

        @Override
        public void onUpperLayerIndInfo(int slotId, Token token, Status status,
                                            UpperLayerIndInfo upperLayerInfo) {
            Log.d(TAG, "onUpperLayerIndInfo slotId = " + slotId +
                    " upperLayerInfo = " + upperLayerInfo);
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_ON_UPPER_LAYER_IND_INFO, slotId, -1, new Result(token, status,
                            upperLayerInfo)));
        }

        @Override
        public void on5gConfigInfo(int slotId, Token token, Status status,
                                            NrConfigType nrConfigType) {
            Log.d(TAG, "on5gConfigInfo slotId = " + slotId +
                    " nrConfigType = " + nrConfigType);
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_ON_5G_CONFIG_INFO, slotId, -1, new Result(token, status,
                            nrConfigType)));
        }

        @Override
        public void onSignalStrength(int slotId, Token token, Status status,
                                            SignalStrength signalStrength) {
            Log.d(TAG, "onSignalStrength slotId = " + slotId +
                    " signalStrength = " + signalStrength);
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_ON_SIGNAL_STRENGTH, slotId, -1, new Result(token, status,
                            signalStrength)));
        }

        @Override
        public void getQtiRadioCapabilityResponse(int slotId, Token token, Status status,
                                     int raf) {
            Log.d(TAG, "getQtiRadioCapabilityResponse slotId = " + slotId +
                    " raf = " + raf);
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_QTI_RADIO_CAPABILITY_RESPONSE, slotId, -1, new Result(token, status,
                            raf)));
        }

        @Override
        public void sendCdmaSmsResponse(int slotId, Token token, Status status, SmsResult sms) {
            Log.d(TAG, "sendCdmaSmsResponse slotId = " + slotId +
                    " status = " + status);
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_SEND_CDMA_SMS_RESPONSE, slotId, -1, new Result(token, status, sms)));
        }

        @Override
        public void getCallForwardStatusResponse(int slotId, Token token, Status status,
                QtiCallForwardInfo[] callForwardInfoList) {
            Log.d(TAG, "getCallForwardStatusResponse slotId = " + slotId +
                    " status = " + status);
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_CALL_FORWARD_QUERY_RESPONSE, slotId, -1, new Result(token, status,
                    callForwardInfoList)));
        }

        @Override
        public void getFacilityLockForAppResponse(int slotId, Token token, Status status,
                int[] result) {
            Log.d(TAG, "getFacilityLockForAppResponse slotId = " + slotId +
                    " status = " + status);
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_FACILITY_LOCK_QUERY_RESPONSE, slotId, -1,
                    new Result(token, status, result)));
        }

        @Override
        public void getImeiResponse(int slotId, Token token, Status status, QtiImeiInfo imeiInfo) {
            Log.d(TAG, "getImeiResponse slotId = " + slotId +
                    " status = " + status + " imeiInfo = " + imeiInfo);
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_GET_IMEI_RESPONSE, slotId, -1,
                    new Result(token, status, imeiInfo)));
        }

        @Override
        public void onImeiChange(int slotId, QtiImeiInfo imeiInfo) {
            Log.d(TAG, "onImeiChange slotId = " + slotId +
                    " imeiInfo = " + imeiInfo);
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_IMEI_CHANGE_IND_INFO, slotId, -1,
                    new Result(null, null, imeiInfo)));
        }

        @Override
        public void onSendUserPreferenceForDataDuringVoiceCall(int slotId, Token token,
                Status status) {
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_ON_ALLOW_MODEM_RECOMMENDATION_FOR_DATA_DURING_CALL,
                    slotId, -1, new Result(token, status, null)));
        }

        @Override
        public void onDdsSwitchCapabilityChange(int slotId, Token token,
                Status status, boolean support) {
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_ON_DDS_SWITCH_CAPABILITY_CHANGE, slotId, -1,
                    new Result(token, status, support)));
        }

        @Override
        public void onDdsSwitchCriteriaChange(int slotId, Token token, boolean telephonyDdsSwitch) {
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_ON_AUTO_DDS_SWITCH_CHANGE, slotId, -1,
                    new Result(token, null, telephonyDdsSwitch)));
        }

        @Override
        public void onDdsSwitchRecommendation(int slotId, Token token, int recommendedSlotId) {
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_ON_DDS_SWITCH_RECOMMENDATION, slotId, -1,
                    new Result(token, null, recommendedSlotId)));
        }

        @Override
        public void onDataDeactivateDelayTime(int slotId, Token token, long delayTimeMilliSecs) {
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_ON_DATA_DEACTIVATE_DELAY_TIME, slotId, -1,
                    new Result(token, null, delayTimeMilliSecs)));
        }

        @Override
        public void onEpdgOverCellularDataSupported(int slotId, Token token, boolean support) {
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_ON_EPDG_OVER_CELLULAR_DATA_SUPPORTED, slotId, -1,
                    new Result(token, null, support)));
        }

        @Override
        public void onMcfgRefresh(Token token, QtiMcfgRefreshInfo qtiRefreshInfo) {
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_ON_MCFG_REFRESH, -1, -1,
                    new Result(token, null, qtiRefreshInfo)));
        }

        @Override
        public void onSetNrUltraWidebandIconConfigResponse(int slotId, Token token, Status status) {
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage(
                    EVENT_ON_SET_NR_ULTRA_WIDEBAND_CONFIG_RESPONSE, slotId, -1,
                    new Result(token, status, null)));
        }

        @Override
        public void startNetworkScanResponse(int slotId, Token token, int errorCode) {
            Log.d(TAG, "startNetworkScanResponse slotId = " + slotId + " errorCode = " + errorCode);
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_START_NETWORK_SCAN_RESPONSE, slotId, -1, new Result(token, null,
                            errorCode)));
        }

        @Override
        public void stopNetworkScanResponse(int slotId, Token token, int errorCode) {
            Log.d(TAG, "stopNetworkScanResponse slotId = " + slotId + " errorCode = " + errorCode);
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_STOP_NETWORK_SCAN_RESPONSE, slotId, -1, new Result(token, null,
                            errorCode)));
        }

        @Override
        public void setNetworkSelectionModeManualResponse(int slotId, Token token,
                int errorCode) {
            Log.d(TAG, "setNetworkSelectionModeManualResponse slotId = " + slotId +
                    " errorCode = " + errorCode);
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_START_NETWORK_SELECTION_MODE_MANUAL_RESPONSE, slotId, -1,
                    new Result(token, null, errorCode)));
        }

        @Override
        public void setNetworkSelectionModeAutomaticResponse(int slotId, Token token,
                int errorCode) {
            Log.d(TAG, "setNetworkSelectionModeAutomaticResponse slotId = " + slotId +
                    " errorCode = " + errorCode);
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_START_NETWORK_SELECTION_MODE_AUTOMATIC_RESPONSE, slotId, -1,
                    new Result(token, null, errorCode)));
        }

        @Override
        public void getNetworkSelectionModeResponse(int slotId, Token token,
                Status status, NetworkSelectionMode modes) {
            Log.d(TAG, "getNetworkSelectionModeResponse slotId = " + slotId +
                    "AccessMode = " + modes.getAccessMode() + "IsManual = " + modes.getIsManual());
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_GET_NETWORK_SELECTION_MODE_RESPONSE, slotId, -1,
                    new Result(token, status, modes)));
        }

        @Override
        public void networkScanResult(int slotId, Token token, NetworkScanResult nsr) {
            Log.d(TAG, "networkScanResult slotId = " + slotId + " NetworkScanResult = " + nsr);
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_NETWORK_SCAN_RESULT, slotId, -1, new Result(token, null,
                            nsr)));
        }

        @Override
        public void getQosParametersResponse(int slotId, Token token, Status status,
                                             QosParametersResult result) {
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_GET_QOS_PARAMETERS_RESPONSE, slotId, -1,
                    new Result(token, status, result)));
        }

        @Override
        public void onQosParametersChanged(int slotId, int cid, QosParametersResult qosParams) {
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_ON_QOS_PARAMETERS_CHANGED, slotId, cid,
                    new Result(null, null, qosParams)));
        }

        @Override
        public void onSimPersoUnlockStatusChange(int slotId,
                QtiPersoUnlockStatus persoUnlockStatus) {
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_SIM_PERSO_UNLOCK_STATUS_CHANGE, slotId, -1,
                            new Result(null, null, persoUnlockStatus)));
        }

        @Override
        public void onCiwlanAvailable(int slotId, boolean ciwlanAvailable) {
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_ON_CIWLAN_AVAILABLE, slotId, -1,
                    new Result(null, null, ciwlanAvailable)));
        }

        @Override
        public void setCiwlanModeUserPreferenceResponse(int slotId, Token token,
                Status status) {
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_SET_CIWLAN_MODE_USER_PREFERENCE_RESPONSE, slotId, -1,
                    new Result(token, status, null)));
        }

        @Override
        public void onCiwlanConfigChange(int slotId, CiwlanConfig ciwlanConfig) {
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_ON_CIWLAN_CONFIG_CHANGE, slotId, -1,
                    new Result(null, null, ciwlanConfig)));
        }

        @Override
        public void setCellularRoamingPreferenceResponse(int slotId, Token token, Status status) {
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage(
                    EVENT_SET_CELLULAR_ROAMING_PREFERENCE_RESPONSE, slotId, -1,
                    new Result(token, status, null)));
        }

        @Override
        public void onNrIconChange(int slotId, NrIcon icon) {
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage(
                    EVENT_ON_NR_ICON_CHANGE, slotId, -1, new Result(null, null, icon)));
        }

        @Override
        public void onNrIconResponse(int slotId, Token token, Status status, NrIcon icon) {
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage(
                    EVENT_QUERY_NR_ICON_RESPONSE, slotId, -1, new Result(token, status, icon)));
        }

        public void onAllAvailableProfilesReq(int slotId, int refNum) {
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage(
                    EVENT_GET_ALL_ESIM_PROFILES, slotId, refNum));
        }

        @Override
        public  void onEnableProfileReq(int slotId, int refNum, String iccId) {
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage(
                    EVENT_ENABLE_ESIM_PROFILE, slotId, refNum, new Result(null, null, iccId)));
        }

        @Override
        public  void onDisableProfileReq(int slotId, int refNum, String iccId) {
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage(
                    EVENT_DISABLE_ESIM_PROFILE, slotId, refNum, new Result(null, null, iccId)));
        }

        @Override
        public void onSendAllEsimProfilesResponse(Token token) {
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage(
                    EVENT_GET_ALL_ESIM_PROFILES_RESPONSE, new Result(token, null, null)));
        }

        @Override
        public void onNotifyDisableProfileStatusResponse(Token token) {
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage(
                    EVENT_DISABLE_ESIM_PROFILE_RESPONSE, new Result(token, null, null)));
        }

        @Override
        public void onNotifyEnableProfileStatusResponse(Token token) {
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage(
                    EVENT_ENABLE_ESIM_PROFILE_RESPONSE, new Result(token, null, null)));
        }
    };

    public QtiRadioProxy(Context context, ExtTelephonyServiceImpl extTelephonyServiceImpl) {
        this(context);
        init(extTelephonyServiceImpl);
    }

    /**
     * This constructor should only be used for unit tests.
     * If using this constructor, invoke init(ExtTelephonyServiceImpl extTelephonyServiceImpl)
     * to set ExtTelephonyServiceImpl object and to register for callbacks.
     */
    @VisibleForTesting
    public QtiRadioProxy(Context context) {
        mContext = context;
        mQtiRadio = QtiRadioFactory.makeQtiRadio(mContext);
        Log.d(TAG, "Made mQtiRadio - length = " + mQtiRadio.length);
        mWorkerThread.start();
        setLooper(mWorkerThread.getLooper());
        if (QtiRadioUtils.isCneAidlAvailable()) {
            Log.d(TAG, "QtiRadioProxy() isCneAidlAvailable: init AIDL ... ");
            mIsFactoryAidlAvailable = true;
            mCneDataFactoryAvailable = QtiRadioUtils.initIFactoryAidl();
            QtiRadioUtils.registerForNotifications(CNE_FACTORY_SAIDL_SERVICE_NAME,
                    mServiceCallback);
        } else {
            Log.d(TAG, "QtiRadioProxy() init HIDL ... ");
            initIFactoryHidl();
        }
    }

    private void initIFactoryHidl() {
        try {
            if (!IServiceManager.getService().registerForNotifications(CNE_FACTORY_SERVICE_NAME,
                    CNE_FACTORY_SERVICE_INSTANCE_NAME, mServiceNotification)) {
                Log.e(TAG, "Failed to register for service start notification");
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to register for service start notification", e);
        }
    }

    private class ServiceCallback extends IServiceCallback.Stub {
        @Override
        public void onRegistration(String name, @NonNull final IBinder newBinder)
                throws RemoteException {
            Log.d(TAG, "onRegistration: name = " + name);
            if (!CNE_FACTORY_SAIDL_SERVICE_NAME.equals(name)) {
                Log.d(TAG, "onRegistration: Ignoring.");
                return;
            }
            Log.d(TAG, "onRegistration: SUCCESS");
            mCneDataFactoryAvailable = true;
            callDynamicDdsSwitchOnDemand();
        }
    }

    public void onMultiSimConfigChanged(int activeModemCount) {
        Log.d(TAG, "onMultiSimConfigChanged activeModemCount = " + activeModemCount);

        // No change.
        if (mQtiRadio.length == activeModemCount) {
            return;
        }

        int oldActiveModemCount = mQtiRadio.length;

        // Dual SIM -> Single SIM switch.
        for (int phoneId = oldActiveModemCount - 1; phoneId >= activeModemCount; phoneId--) {
            mQtiRadio[phoneId].unRegisterCallback(mQtiRadioCallback);
            mQtiRadio[phoneId] = null;
        }

        mQtiRadio = copyOf(mQtiRadio, activeModemCount);

        // Single SIM -> Dual SIM switch.
        for (int phoneId = oldActiveModemCount; phoneId < activeModemCount; phoneId++) {
            mQtiRadio[phoneId] = QtiRadioFactory.makeQtiRadio(mContext, phoneId);
            registerCallback(phoneId);
            QtiRadioFactory.initQtiRadioAidl(phoneId);
        }
    }

    @VisibleForTesting
    void init(ExtTelephonyServiceImpl extTelephonyServiceImpl) {
        Log.d(TAG, "init: set ExtTelephonyServiceImpl");
        mExtTelephonyServiceImpl = extTelephonyServiceImpl;
        IntStream.range(0, mQtiRadio.length).forEach(i -> registerCallback(i));
        // QtiRadioProxy callbacks to be registered first followed by QtiRadioStable AIDL client
        // registration with RIL, otherwise indication can get dropped at QtiRadioAidl as there is
        // no QtiRadioProxy callbacks available. Below initQtiRadioAidl() will register the
        // QtiRadioStable AIDL callbacks with RIL.
        QtiRadioFactory.initQtiRadioAidl();
        callDynamicDdsSwitchOnDemand();
    }

    private void setLooper(Looper workerLooper) {
        mWorkerThreadHandler = new QtiRadioProxy.WorkerHandler(workerLooper);
    }

    private void registerCallback(int slotId) {
        if (isSlotIdValid(slotId)) {
            mQtiRadio[slotId].registerCallback(mQtiRadioCallback);
        }
    }

    public int getPropertyValueInt(String property, int def) throws RemoteException {
        return mQtiRadio[DEFAULT_PHONE_INDEX].getPropertyValueInt(property, def);
    }

    public boolean getPropertyValueBool(String property, boolean def) throws RemoteException {
        return mQtiRadio[DEFAULT_PHONE_INDEX].getPropertyValueBool(property, def);
    }

    public String getPropertyValueString(String property, String def) throws RemoteException {
        return mQtiRadio[DEFAULT_PHONE_INDEX].getPropertyValueString(property, def);
    }

    public Token queryNrIconType(int slotId, Client client) throws RemoteException {
        Log.d(TAG, "queryNrIconType: slotId = " + slotId);
        if (!isSlotIdValid(slotId) || !mExtTelephonyServiceImpl.isClientValid(client)) {
            return null;
        }
        Token token = getNextToken();
        mInflightRequests.put(token.get(), new Transaction(token, "queryNrIconType",
                client));
        mQtiRadio[slotId].queryNrIconType(token);
        return token;
    }

    public Token enableEndc(int slotId, boolean enabled, Client client) throws RemoteException {
        Log.d(TAG, "enableEndc: slotId = " + slotId);
        if (!isSlotIdValid(slotId) || !mExtTelephonyServiceImpl.isClientValid(client)) {
            return null;
        }
        Token token = getNextToken();
        mInflightRequests.put(token.get(), new Transaction(token, "enableEndc",
                client));
        mQtiRadio[slotId].enableEndc(enabled, token);
        return token;
    }

    public Token queryEndcStatus(int slotId, Client client) throws RemoteException {
        Log.d(TAG, "queryEndcStatus: slotId = " + slotId);
        if (!isSlotIdValid(slotId) || !mExtTelephonyServiceImpl.isClientValid(client)) {
            return null;
        }
        Token token = getNextToken();
        mInflightRequests.put(token.get(), new Transaction(token, "queryEndcStatus",
                client));
        mQtiRadio[slotId].queryEndcStatus(token);
        return token;
    }

    public Token setNrConfig(int slotId, NrConfig config, Client client) throws RemoteException {
        int uid = Binder.getCallingUid();
        String packageName = mContext.getPackageManager().getNameForUid(uid);
        Log.d(TAG, "setNrConfig: slotId = " + slotId  + " config = " + config
                + " uid = " + uid + " package=" + packageName);
        if (!isSlotIdValid(slotId) || !mExtTelephonyServiceImpl.isClientValid(client)) {
            return null;
        }
        Token token = getNextToken();
        mInflightRequests.put(token.get(), new Transaction(token, "setNrConfig", client));
        mQtiRadio[slotId].setNrConfig(config, token);
        return token;
    }

    public Token setNetworkSelectionModeAutomatic(int slotId, int accessType, Client client)
            throws RemoteException {
        Log.d(TAG, "setNetworkSelectionModeAutomatic: slotId = " + slotId  +
                " accessType = " + accessType);
        if (!isSlotIdValid(slotId) || !mExtTelephonyServiceImpl.isClientValid(client)) {
            return null;
        }
        Token token = getNextToken();
        mInflightRequests.put(token.get(), new Transaction(token,
                "setNetworkSelectionModeAutomatic", client));
        mQtiRadio[slotId].setNetworkSelectionModeAutomatic(accessType, token);
        return token;
    }

    public Token getNetworkSelectionMode(int slotId, Client client)
            throws RemoteException {
        Log.d(TAG, "getNetworkSelectionMode: slotId = " + slotId);
        if (!isSlotIdValid(slotId) || !mExtTelephonyServiceImpl.isClientValid(client)) {
            return null;
        }
        Token token = getNextToken();
        mInflightRequests.put(token.get(), new Transaction(token,
                "getNetworkSelectionMode", client));
        mQtiRadio[slotId].getNetworkSelectionMode(token);
        return token;
    }

    public Token queryNrConfig(int slotId, Client client) throws RemoteException {
        Log.d(TAG, "queryNrConfig: slotId = " + slotId);
        if (!isSlotIdValid(slotId) || !mExtTelephonyServiceImpl.isClientValid(client)) {
            return null;
        }
        Token token = getNextToken();
        mInflightRequests.put(token.get(), new Transaction(token, "queryNrConfig", client));
        mQtiRadio[slotId].queryNrConfig(token);
        return token;
    }

    public Token enable5g(int slotId, Client client) throws RemoteException {
        Log.d(TAG, "enable5g: slotId = " + slotId);
        if (!isSlotIdValid(slotId) || !mExtTelephonyServiceImpl.isClientValid(client)) {
            return null;
        }
        Token token = getNextToken();
        mInflightRequests.put(token.get(), new Transaction(token, "enable5g", client));
        mQtiRadio[slotId].enable5g(token);
        return token;
    }

    public Token disable5g(int slotId, Client client) throws RemoteException {
        Log.d(TAG, "disable5g: slotId = " + slotId);
        if (!isSlotIdValid(slotId) || !mExtTelephonyServiceImpl.isClientValid(client)) {
            return null;
        }
        Token token = getNextToken();
        mInflightRequests.put(token.get(), new Transaction(token, "disable5g", client));
        mQtiRadio[slotId].disable5g(token);
        return token;
    }

    public Token queryNrBearerAllocation(int slotId, Client client) throws RemoteException {
        Log.d(TAG, "queryNrBearerAllocation: slotId = " + slotId);
        if (!isSlotIdValid(slotId) || !mExtTelephonyServiceImpl.isClientValid(client)) {
            return null;
        }
        Token token = getNextToken();
        mInflightRequests.put(token.get(), new Transaction(token, "queryNrBearerAllocation",
                client));
        mQtiRadio[slotId].queryNrBearerAllocation(token);
        return token;
    }

    public Token enable5gOnly(int slotId, Client client) throws RemoteException {
        Log.d(TAG, "enable5gOnly: slotId = " + slotId);
        if (!isSlotIdValid(slotId) || !mExtTelephonyServiceImpl.isClientValid(client)) {
            return null;
        }
        Token token = getNextToken();
        mInflightRequests.put(token.get(), new Transaction(token, "enable5gOnly",
                client));
        mQtiRadio[slotId].enable5gOnly(token);
        return token;
    }

    public Token query5gStatus(int slotId, Client client) throws RemoteException {
        Log.d(TAG, "query5gStatus: slotId = " + slotId);
        if (!isSlotIdValid(slotId) || !mExtTelephonyServiceImpl.isClientValid(client)) {
            return null;
        }
        Token token = getNextToken();
        mInflightRequests.put(token.get(), new Transaction(token, "query5gStatus",
                client));
        mQtiRadio[slotId].query5gStatus(token);
        return token;
    }

    public Token queryNrDcParam(int slotId, Client client) throws RemoteException {
        Log.d(TAG, "queryNrDcParam: slotId = " + slotId);
        if (!isSlotIdValid(slotId) || !mExtTelephonyServiceImpl.isClientValid(client)) {
            return null;
        }
        Token token = getNextToken();
        mInflightRequests.put(token.get(), new Transaction(token, "queryNrDcParam",
                client));
        mQtiRadio[slotId].queryNrDcParam(token);
        return token;
    }

    public Token queryNrSignalStrength(int slotId, Client client) throws RemoteException {
        Log.d(TAG, "queryNrSignalStrength: slotId = " + slotId);
        if (!isSlotIdValid(slotId) || !mExtTelephonyServiceImpl.isClientValid(client)) {
            return null;
        }
        Token token = getNextToken();
        mInflightRequests.put(token.get(), new Transaction(token, "queryNrSignalStrength",
                client));
        mQtiRadio[slotId].queryNrSignalStrength(token);
        return token;
    }

    public Token queryUpperLayerIndInfo(int slotId, Client client) throws RemoteException {
        Log.d(TAG, "queryUpperLayerIndInfo: slotId = " + slotId);
        if (!isSlotIdValid(slotId) || !mExtTelephonyServiceImpl.isClientValid(client)) {
            return null;
        }
        Token token = getNextToken();
        mInflightRequests.put(token.get(), new Transaction(token, "queryUpperLayerIndInfo",
                client));
        mQtiRadio[slotId].queryUpperLayerIndInfo(token);
        return token;
    }

    public Token query5gConfigInfo(int slotId, Client client) throws RemoteException {
        Log.d(TAG, "query5gConfigInfo: slotId = " + slotId);
        if (!isSlotIdValid(slotId) || !mExtTelephonyServiceImpl.isClientValid(client)) {
            return null;
        }
        Token token = getNextToken();
        mInflightRequests.put(token.get(), new Transaction(token, "query5gConfigInfo",
                client));
        mQtiRadio[slotId].query5gConfigInfo(token);
        return token;
    }

    public Token getQtiRadioCapability(int slotId, Client client) throws RemoteException {
        Log.d(TAG, "getQtiRadioCapability: slotId = " + slotId);
        if (!isSlotIdValid(slotId) || !mExtTelephonyServiceImpl.isClientValid(client)) {
            return null;
        }
        Token token = getNextToken();
        mInflightRequests.put(token.get(), new Transaction(token, "getQtiRadioCapability",
                client));
        mQtiRadio[slotId].getQtiRadioCapability(token);
        return token;
    }

    public Token sendCdmaSms(int slotId, byte[] pdu, boolean expectMore,
            Client client) throws RemoteException {
        Log.d(TAG, "sendCdmaSms: slotId = " + slotId);
        if (!isSlotIdValid(slotId) || !mExtTelephonyServiceImpl.isClientValid(client)) {
            return null;
        }
        Token token = getNextToken();
        mInflightRequests.put(token.get(), new Transaction(token, "sendCdmaSms",
                client));
        mQtiRadio[slotId].sendCdmaSms(pdu, expectMore, token);
        return token;
    }

    public Token startNetworkScan(int slotId, NetworkScanRequest networkScanRequest,
            Client client) throws RemoteException {
        Log.d(TAG, "startNetworkScan: slotId = " + slotId);
        if (!isSlotIdValid(slotId) || !mExtTelephonyServiceImpl.isClientValid(client)) {
            return null;
        }
        Token token = getNextToken();
        mInflightRequests.put(token.get(), new Transaction(token, "startNetworkScan",
                client));
        mQtiRadio[slotId].startNetworkScan(networkScanRequest, token);
        return token;
    }

    public Token stopNetworkScan(int slotId, Client client) throws RemoteException {
        Log.d(TAG, "stopNetworkScan: slotId = " + slotId);
        if (!isSlotIdValid(slotId) || !mExtTelephonyServiceImpl.isClientValid(client)) {
            return null;
        }
        Token token = getNextToken();
        mInflightRequests.put(token.get(), new Transaction(token, "stopNetworkScan",
                client));
        mQtiRadio[slotId].stopNetworkScan(token);
        return token;
    }

    public Token setNetworkSelectionModeManual(int slotId, QtiSetNetworkSelectionMode mode,
            Client client) throws RemoteException {
        Log.d(TAG, "setNetworkSelectionModeManual: slotId = " + slotId);
        if (!isSlotIdValid(slotId) || !mExtTelephonyServiceImpl.isClientValid(client)) {
            return null;
        }
        Token token = getNextToken();
        mInflightRequests.put(token.get(), new Transaction(token, "setNetworkSelectionModeManual",
                client));
        mQtiRadio[slotId].setNetworkSelectionModeManual(mode, token);
        return token;
    }

    public Token setCarrierInfoForImsiEncryption(int slotId, ImsiEncryptionInfo info,
                Client client) throws RemoteException {
        Log.d(TAG, "setCarrierInfoForImsiEncryption: slotId = " + slotId);
        if (!isSlotIdValid(slotId) || !mExtTelephonyServiceImpl.isClientValid(client)) {
            return null;
        }
        Token token = getNextToken();
        mInflightRequests.put(token.get(), new Transaction(token, "setCarrierInfoForImsiEncryption",
                client));
        mQtiRadio[slotId].setCarrierInfoForImsiEncryption(token, info);
        return token;
    }

    public void queryCallForwardStatus(int slotId, int cfReason, int serviceClass, String number,
            boolean expectMore, Client client) throws RemoteException {
        Log.d(TAG, "queryCallForwardStatus: slotId = " + slotId);
        if (!isSlotIdValid(slotId) || !mExtTelephonyServiceImpl.isClientValid(client)) {
            return;
        }
        Token token = getNextToken();
        mInflightRequests.put(token.get(), new Transaction(token, "queryCallForwardStatus",
                client));
        mQtiRadio[slotId].queryCallForwardStatus(token, cfReason, serviceClass, number, expectMore);
    }

    public void getFacilityLockForApp(int slotId, String facility, String password,
            int serviceClass, String appId, boolean expectMore, Client client)
            throws RemoteException {
        Log.d(TAG, "getFacilityLockForApp: slotId = " + slotId);
        if (!isSlotIdValid(slotId) || !mExtTelephonyServiceImpl.isClientValid(client)) {
            return;
        }
        Token token = getNextToken();
        mInflightRequests.put(token.get(), new Transaction(token, "getFacilityLockForApp",
                client));
        mQtiRadio[slotId].getFacilityLockForApp(token, facility, password, serviceClass, appId,
                expectMore);
    }

    // Note: Client can be Null. Do not add isClientValid check here
    public Token getImei(int slotId, Client client) throws RemoteException {
        Log.d(TAG, "getImei: slotId = " + slotId);
        if (!isSlotIdValid(slotId)) {
            return null;
        }
        Token token = getNextToken();
        mInflightRequests.put(token.get(), new Transaction(token, "getImei",
                client));
        mQtiRadio[slotId].getImei(token);
        return token;
    }

    public boolean isSmartDdsSwitchFeatureAvailable() {
        // isSmartDdsSwitchEnable is true by default, it is used for test purpose where Smart
        // DDS feature can be disabled by setting property PROPERTY_SMART_DDS_SWITCH as false,
        // to test Google Auto-DDS feature.
        boolean isSmartDdsSwitchEnable = true;
        try {
            isSmartDdsSwitchEnable = getPropertyValueBool(PROPERTY_SMART_DDS_SWITCH, true);
        } catch (RemoteException ex) {
            Log.d(TAG, "RemoteException while reading Smart DDS property: " + ex);
        }
        Log.d(TAG, "isSmartDdsSwitchFeatureAvailable, isSmartDdsSwitchEnable = "
                + isSmartDdsSwitchEnable + ", mCneDataFactoryAvailable = "
                + mCneDataFactoryAvailable);
        return mCneDataFactoryAvailable && isSmartDdsSwitchEnable;
    }

    public void setSmartDdsSwitchToggle(boolean isEnabled, Client client) throws RemoteException {
        Log.d(TAG, "setSmartDdsSwitchToggle: isEnabled = " + isEnabled);
        if (!mExtTelephonyServiceImpl.isClientValid(client)) return;
        Token token = getNextToken();
        mInflightRequests.put(token.get(), new Transaction(token, "setSmartDdsSwitchToggle",
                client));
        int toggleValue = isEnabled ? SMART_DDS_SWITCH_ON : SMART_DDS_SWITCH_OFF;
        mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage(
                EVENT_SMART_DDS_SWITCH_TOGGLE, new Result(token, null, toggleValue)));
    }

    public boolean isFeatureSupported(int feature) {
        return mQtiRadio[DEFAULT_PHONE_INDEX].isFeatureSupported(feature);
    }

    public Token sendUserPreferenceForDataDuringVoiceCall(int slotId, boolean userPreference,
            Client client) throws RemoteException {
        Log.d(TAG, "sendUserPreferenceForDataDuringVoiceCall: slotId = " + slotId);
        if (!isSlotIdValid(slotId) || !mExtTelephonyServiceImpl.isClientValid(client)) {
            return null;
        }
        Token token = getNextToken();
        mInflightRequests.put(token.get(), new Transaction(token,
                "sendUserPreferenceForDataDuringVoiceCall", client));
        mQtiRadio[slotId].sendUserPreferenceForDataDuringVoiceCall(token, userPreference);
        return token;
    }

    public Token getDdsSwitchCapability(int slotId, Client client) throws RemoteException {
        Log.d(TAG, "getDdsSwitchCapability: slotId = " + slotId);
        if (!isSlotIdValid(slotId) || !mExtTelephonyServiceImpl.isClientValid(client)) {
            return null;
        }
        Token token = getNextToken();
        mInflightRequests.put(token.get(), new Transaction(token, "getDdsSwitchCapability",
                client));
        mQtiRadio[slotId].getDdsSwitchCapability(token);
        return token;
    }

    public boolean isEpdgOverCellularDataSupported(int slotId) throws RemoteException {
        if (isSlotIdValid(slotId)) {
            return mQtiRadio[slotId].isEpdgOverCellularDataSupported();
        }
        return false;
    }

    public Token getQosParameters(int slotId, int cid, Client client) throws RemoteException {
        Log.d(TAG, "getQosParameters: slotId = " + slotId);
        if (!isSlotIdValid(slotId) || !mExtTelephonyServiceImpl.isClientValid(client)) {
            return null;
        }
        Token token = getNextToken();
        mInflightRequests.put(token.get(), new Transaction(token, "getQosParameters", client));
        mQtiRadio[slotId].getQosParameters(token, cid);
        return token;
    }

    public CiwlanConfig getCiwlanConfig(int slotId) throws RemoteException {
        Log.d(TAG, "getCiwlanConfig: slotId = " + slotId);
        if (!isSlotIdValid(slotId)) {
            return null;
        }
        return mQtiRadio[slotId].getCiwlanConfig();
    }

    public QtiPersoUnlockStatus getSimPersoUnlockStatus(int slotId) throws RemoteException {
        Log.d(TAG, "getSimPersoUnlockStatus: slotId = " + slotId);
        if (!isSlotIdValid(slotId)) {
            return null;
        }
        return mQtiRadio[slotId].getSimPersoUnlockStatus();
    }

    public boolean isCiwlanAvailable(int slotId) throws RemoteException {
        if (isSlotIdValid(slotId)) {
            return mQtiRadio[slotId].isCiwlanAvailable();
        }
        return false;
    }

    public Token setCiwlanModeUserPreference(int slotId, Client client, CiwlanConfig ciwlanConfig)
            throws RemoteException {
        Log.d(TAG, "setCiwlanModeUserPreference: slotId = " + slotId);
        if (!isSlotIdValid(slotId) || !mExtTelephonyServiceImpl.isClientValid(client)) {
            return null;
        }
        Token token = getNextToken();
        mInflightRequests.put(token.get(), new Transaction(
                token, "setCiwlanModeUserPreference", client));
        mQtiRadio[slotId].setCiwlanModeUserPreference(token, ciwlanConfig);
        return token;
    }

    public CiwlanConfig getCiwlanModeUserPreference(int slotId) throws RemoteException {
        return mQtiRadio[slotId].getCiwlanModeUserPreference();
    }

    public CellularRoamingPreference getCellularRoamingPreference(int slotId)
            throws RemoteException {
        Log.d(TAG, "getCellularRoamingPreference: slotId = " + slotId);
        if (!isSlotIdValid(slotId)) {
            return null;
        }
        return mQtiRadio[slotId].getCellularRoamingPreference();
    }

    public Token setCellularRoamingPreference(Client client, int slotId,
            CellularRoamingPreference pref) throws RemoteException {
        Log.d(TAG, "setCellularRoamingPreference: slotId = " + slotId + ", internationalPref = " +
                pref.getInternationalCellularRoamingPref() + ", domesticPref = " +
                pref.getDomesticCellularRoamingPref());
        if (!isSlotIdValid(slotId) || !mExtTelephonyServiceImpl.isClientValid(client)) {
            return null;
        }
        Token token = getNextToken();
        mInflightRequests.put(token.get(), new Transaction(token, "setCellularRoamingPreference",
                client));
        mQtiRadio[slotId].setCellularRoamingPreference(token, pref);
        return token;
    }

    public boolean isEmcSupported(int slotId) throws RemoteException {
        if (isSlotIdValid(slotId)) {
            return mQtiRadio[slotId].isEmcSupported();
        }
        return false;
    }

    public boolean isEmfSupported(int slotId) throws RemoteException {
        if (isSlotIdValid(slotId)) {
            return mQtiRadio[slotId].isEmfSupported();
        }
        return false;
    }

    public Token queryNrIcon(int slotId, Client client) throws RemoteException {
        Log.d(TAG, "queryNrIcon: slotId = " + slotId);
        if (!isSlotIdValid(slotId) || !mExtTelephonyServiceImpl.isClientValid(client)) {
            return null;
        }
        Token token = getNextToken();
        mInflightRequests.put(token.get(), new Transaction(token, "queryNrIcon", client));
        mQtiRadio[slotId].queryNrIcon(token);
        return token;
    }

    public Token sendAllEsimProfiles(int slotId, boolean status, int refNum, List<String> iccIdList)
            throws RemoteException  {
        Log.d(TAG, "sendAllEsimProfiles: slotId = " + slotId + " status = " + status  + " refNum = "
                + refNum + " iccIdList  = " + iccIdList);

        if (!isSlotIdValid(slotId)) {
            Log.d(TAG, "sendAllEsimProfiles: invalid slotId");
            return null;
        }
        Token token = getNextToken();
        mInflightRequests.put(token.get(), new Transaction(token, "sendAllEsimProfiles", null));
        mQtiRadio[slotId].sendAllEsimProfiles(token, status, refNum, iccIdList);
        return token;
    }

    public Token notifyEnableProfileStatus(int slotId, int refNum, boolean result)
            throws RemoteException  {
        Log.d(TAG, "notifyEnableProfileStatus: slotId = " + slotId + " refNum = " + refNum +
                " result  = " + result);

        if (!isSlotIdValid(slotId)) {
            Log.d(TAG, "notifyEnableProfileStatus: invalid slotId");
            return null;
        }
        Token token = getNextToken();
        mInflightRequests.put(token.get(),
                new Transaction(token, "notifyEnableProfileStatus", null));
        mQtiRadio[slotId].notifyEnableProfileStatus(token, refNum, result);
        return token;
    }

    public Token notifyDisableProfileStatus(int slotId, int refNum, boolean result)
            throws RemoteException  {
        Log.d(TAG, "notifyDisableProfileStatus: slotId = " + slotId + " refNum = " + refNum +
                " result  = " + result);

        if (!isSlotIdValid(slotId)) {
            Log.d(TAG, "notifyDisableProfileStatus: invalid slotId");
            return null;
        }
        Token token = getNextToken();
        mInflightRequests.put(token.get(),
                new Transaction(token, "notifyDisableProfileStatus", null));
        mQtiRadio[slotId].notifyDisableProfileStatus(token, refNum, result);
        return token;
    }

    private static IHwBinder.DeathRecipient mDeathRecipient = new IHwBinder.DeathRecipient() {
        @Override
        public void serviceDied(long cookie) {
            Log.d(TAG, "CnE ISubscriptionManager service is down");
            if (sDynamicSubscriptionManager != null) {
                try {
                    sDynamicSubscriptionManager.unlinkToDeath(mDeathRecipient);
                } catch (RemoteException e) {
                    Log.e(TAG, "Failed to unlink the death recipient", e);
                }
            }
            mCneDataFactoryAvailable = false;
            sDynamicSubscriptionManager = null;
        }
    };

    private IBinder.DeathRecipient mAidlDeathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            Log.d(TAG, "vendor.qti.hardware.data.dynamicddsaidlservice."
                    + "ISubscriptionManager service is down");
            if (sAidlDynamicSubscriptionManager != null) {
                mCneDataFactoryAvailable = false;
                sAidlDynamicSubscriptionManager = null;
            }
        }
    };

    private class ServiceNotification extends IServiceNotification.Stub {
        @Override
        public void onRegistration(String fqName, String name, boolean preexisting) {
            Log.d(TAG, "onRegistration: fqName = " + fqName + " name = " + name);
            if (!CNE_FACTORY_SERVICE_INSTANCE_NAME.equals(name)) {
                Log.d(TAG, "onRegistration: Ignoring.");
                return;
            }
            mCneDataFactoryAvailable = true;
            callDynamicDdsSwitchOnDemand();
        }
    }

    private void callDynamicDdsSwitchOnDemand() {
        int savedSmartDdsSwitchValue = Settings.Global.getInt(mContext.getContentResolver(),
                QtiRadioUtils.getSmartDdsSwitchKeyName(), SMART_DDS_SWITCH_OFF);
        Log.d(TAG, "savedSmartDdsSwitchValue: " + savedSmartDdsSwitchValue);
        Token token = getNextToken();
        mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage(
                EVENT_SMART_DDS_SWITCH_TOGGLE, new Result(token, null, savedSmartDdsSwitchValue)));
    }

    private void setDynamicSubscriptionChange(Token token, boolean isEnabled) {
        int status = StatusCode.FAILED;
        if (sDynamicSubscriptionManager == null) {
            sDynamicSubscriptionManager = getDynamicSubscriptionManager();
            if (sDynamicSubscriptionManager == null) {
                Log.e(TAG, "getDynamicSubscriptionManager returned null");
                return;
            }
        }
        try {
            status = sDynamicSubscriptionManager.setDynamicSubscriptionChange(isEnabled);
        } catch (RemoteException | NullPointerException e) {
            Log.e(TAG, "setDynamicSubscriptionChange exception", e);
        } finally {
            int toggleValue = isEnabled ? SMART_DDS_SWITCH_ON : SMART_DDS_SWITCH_OFF;
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage(
                    EVENT_ON_SMART_DDS_SWITCH_TOGGLE_RESPONSE, status, toggleValue, token));
        }
    }

    private void setAidlDynamicSubscriptionChange(Token token, boolean isEnabled) {
        int status = StatusCode.FAILED;
        if (sAidlDynamicSubscriptionManager == null) {
            sAidlDynamicSubscriptionManager = createAidlDynamicSubscriptionManager();
            if (sAidlDynamicSubscriptionManager == null) {
                Log.e(TAG, "createAidlDynamicSubscriptionManager returned null");
                return;
            }
        }
        try {
            Log.d(TAG, "Send setDynamicSubscriptionChange req isEnabled:" + isEnabled);
            status = sAidlDynamicSubscriptionManager.setDynamicSubscriptionChange(isEnabled);
        } catch (RemoteException | NullPointerException e) {
            Log.e(TAG, "AIDL: setDynamicSubscriptionChange exception", e);
        } finally {
            int toggleValue = isEnabled ? SMART_DDS_SWITCH_ON : SMART_DDS_SWITCH_OFF;
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage(
                    EVENT_ON_SMART_DDS_SWITCH_TOGGLE_RESPONSE, status, toggleValue, token));
        }
    }

    public static ISubscriptionManager getDynamicSubscriptionManager() {
        ISubscriptionManager manager = null;
        try {
            manager = QtiRadioUtils.getDynamicSubscriptionManager();
            Log.d(TAG, "createDynamicddsISubscriptionManager " + manager);
            if (manager != null && !manager.linkToDeath(mDeathRecipient, mDeathBinderCookie)) {
                Log.e(TAG, "Failed to link to death recipient");
            }
        } catch (RemoteException e) {
            Log.e(TAG, "createDynamicddsISubscriptionManager exception", e);
        }
        return manager;
    }

    private vendor.qti.hardware.data.dynamicddsaidlservice.
            ISubscriptionManager createAidlDynamicSubscriptionManager() {
        try {
            Log.d(TAG, "Call createAidlDynamicSubscriptionManager");
            vendor.qti.hardware.data.dynamicddsaidlservice.ISubscriptionManager manager;
            manager = QtiRadioUtils.getDynamicddsISubscriptionManager();
            if (manager != null) { //Success
                Log.d(TAG, "createAidlDynamicSubscriptionManager success");
                sAidlDynamicSubscriptionManager = manager;
                sAidlDynamicSubscriptionManager.asBinder().linkToDeath(mAidlDeathRecipient, 0);
                return sAidlDynamicSubscriptionManager;
            } else {
                Log.d(TAG, "createAidlDynamicSubscriptionManager failed ");
            }
        } catch (RemoteException e) {
            Log.e(TAG, "createDynamicddsISubscriptionManager exception", e);
        }
        return null;
    }

    private void setSmartDdsSwitchToggleResponse(Token token, int status, int toggleValue) {
        Log.d(TAG, "setSmartDdsSwitchToggleResponse status = " + status);
        boolean result = status == StatusCode.OK;
        try {
            int tokenKey = token.get();
            for (IExtPhoneCallback callback : retrieveCallbacks(tokenKey,
                    ExtPhoneCallbackListener.EVENT_SET_SMART_DDS_SWITCH_TOGGLE_RESPONSE)) {
                Log.d(TAG, "setSmartDdsSwitchToggleResponse: Responding back for transaction = "
                        + mInflightRequests.get(tokenKey));
                callback.setSmartDdsSwitchToggleResponse(token, result);
                mInflightRequests.remove(tokenKey);
        }
        } catch (RemoteException e) {
            Log.d(TAG, "setSmartDdsSwitchToggleResponse: Exception = " + e);
        }
    }

    public void removeClientFromInflightRequests(IExtPhoneCallback callback) {
        for(int key : mInflightRequests.keySet()) {
            Transaction txn = mInflightRequests.get(key);
            if (mExtTelephonyServiceImpl.isClientValid(txn.mClient)
                    && txn.mClient.getCallback().asBinder() == callback.asBinder()) {
                Log.d(TAG, "removeClientFromInflightRequests: Token = " + key + " => " +
                        mInflightRequests.get(key));
                mInflightRequests.remove(key);
            }
        }
    }

    private boolean isSlotIdValid(int slotId) {
        if (slotId >= 0 && slotId < mQtiRadio.length) {
            return true;
        }
        Log.d(TAG, "Invalid slotId = " + slotId + ", mQtiRadio length = " + mQtiRadio.length +
                " skipping request!");
        return false;
    }

    ArrayList<IExtPhoneCallback> retrieveCallbacks(int tokenKey, int event) {
        ArrayList<IExtPhoneCallback> list = new ArrayList<IExtPhoneCallback>();
        if (tokenKey != Token.UNSOL) {
            if (mInflightRequests.containsKey(tokenKey)) {
                Transaction txn = mInflightRequests.get(tokenKey);
                Client client = txn.mClient;
                if (mExtTelephonyServiceImpl.isClientValid(client)) {
                    list.add(client.getCallback());
                } else {
                    Log.e(TAG, "This client is invalid now: " + client);
                }
            }
        } else {
            list = mExtTelephonyServiceImpl.retrieveCallbacksWithEvent(tokenKey, event);
        }

        return list;
    }

    /* Private delegates */
    private void onNrIconType(int slotId, Token token, Status status,
                              NrIconType nrIconType) {
        try {
            int tokenKey = token.get();
            for (IExtPhoneCallback callback : retrieveCallbacks(tokenKey,
                    ExtPhoneCallbackListener.EVENT_ON_NR_ICON_TYPE)) {
                Log.d(TAG, "onNrIconType: Responding back for transaction = " +
                        mInflightRequests.get(tokenKey));
                callback.onNrIconType(slotId, token, status, nrIconType);
                mInflightRequests.remove(tokenKey);
            }
        } catch (RemoteException e) {
            Log.d(TAG, "onNrIconType: Exception = " + e);
        }
    }

    private void onEnableEndc(int slotId, Token token, Status status) {
        try {
            int tokenKey = token.get();
            for (IExtPhoneCallback callback : retrieveCallbacks(tokenKey,
                    ExtPhoneCallbackListener.EVENT_ON_ENABLE_ENDC)) {
                Log.d(TAG, "onEnableEndc: Responding back for transaction = " +
                        mInflightRequests.get(tokenKey));
                callback.onEnableEndc(slotId, token, status);
                mInflightRequests.remove(tokenKey);
            }
        } catch (RemoteException e) {
            Log.d(TAG, "onEnableEndc: Exception = " + e);
        }
    }

    private void onEndcStatus(int slotId, Token token, Status status, boolean enableStatus) {
        try {
            int tokenKey = token.get();
            for (IExtPhoneCallback callback : retrieveCallbacks(tokenKey,
                    ExtPhoneCallbackListener.EVENT_ON_ENDC_STATUS)) {
                Log.d(TAG, "onEndcStatus: Responding back for transaction = " +
                        mInflightRequests.get(tokenKey));
                callback.onEndcStatus(slotId, token, status, enableStatus);
                mInflightRequests.remove(tokenKey);
            }
        } catch (RemoteException e) {
            Log.d(TAG, "onEndcStatus: Exception = " + e);
        }
    }

    public void onSetNrConfig(int slotId, Token token, Status status) {
        try {
            int tokenKey = token.get();
            for (IExtPhoneCallback callback : retrieveCallbacks(tokenKey,
                    ExtPhoneCallbackListener.EVENT_ON_SET_NR_CONFIG)) {
                Log.d(TAG, "onSetNrConfig: Responding back for transaction = " +
                        mInflightRequests.get(tokenKey));
                callback.onSetNrConfig(slotId, token, status);
                mInflightRequests.remove(tokenKey);
            }
        } catch (RemoteException e) {
            Log.d(TAG, "onSetNrConfig: Exception = " + e);
        }
    }

    public void onNrConfigStatus(int slotId, Token token, Status status, NrConfig nrConfig) {
        try {
            int tokenKey = token.get();
            for (IExtPhoneCallback callback : retrieveCallbacks(tokenKey,
                    ExtPhoneCallbackListener.EVENT_ON_NR_CONFIG_STATUS)) {
                Log.d(TAG, "onNrConfigStatus: Responding back for transaction = " +
                        mInflightRequests.get(tokenKey));
                callback.onNrConfigStatus(slotId, token, status, nrConfig);
                mInflightRequests.remove(tokenKey);
            }
        } catch (RemoteException e) {
            Log.d(TAG, "onNrConfigStatus: Exception = " + e);
        }
    }

    private void on5gStatus(int slotId, Token token, Status status, boolean enableStatus) {
        try {
            int tokenKey = token.get();
            for (IExtPhoneCallback callback : retrieveCallbacks(tokenKey,
                    ExtPhoneCallbackListener.EVENT_ON_5G_STATUS)) {
                Log.d(TAG, "on5gStatus: Responding back for transaction = " +
                        mInflightRequests.get(tokenKey));
                callback.on5gStatus(slotId, token, status, enableStatus);
                mInflightRequests.remove(tokenKey);
            }
        } catch (RemoteException e) {
            Log.d(TAG, "on5gStatus: Exception = " + e);
        }
    }

    private void onAnyNrBearerAllocation(int slotId, Token token, Status status,
                                         BearerAllocationStatus bearerStatus) {
        try {
            int tokenKey = token.get();
            for (IExtPhoneCallback callback : retrieveCallbacks(tokenKey,
                    ExtPhoneCallbackListener.EVENT_ON_ANY_NR_BEARER_ALLOCATION)) {
                Log.d(TAG, "onAnyNrBearerAllocation: Responding back for transaction = " +
                        mInflightRequests.get(tokenKey));
                callback.onAnyNrBearerAllocation(slotId, token, status, bearerStatus);
                mInflightRequests.remove(tokenKey);
            }
        } catch (RemoteException e) {
            Log.d(TAG, "onAnyNrBearerAllocation: Exception = " + e);
        }
    }

    private void onNrDcParam(int slotId, Token token, Status status,
                                        DcParam dcParam) {
        try {
            int tokenKey = token.get();
            for (IExtPhoneCallback callback : retrieveCallbacks(tokenKey,
                    ExtPhoneCallbackListener.EVENT_ON_NR_DC_PARAM)) {
                Log.d(TAG, "onNrDcParam: Responding back for transaction = " +
                        mInflightRequests.get(tokenKey));
                callback.onNrDcParam(slotId, token, status, dcParam);
                mInflightRequests.remove(tokenKey);
            }
        } catch (RemoteException e) {
            Log.d(TAG, "onNrDcParam: Exception = " + e);
        }
    }

    private void onUpperLayerIndInfo(int slotId, Token token, Status status,
                                        UpperLayerIndInfo upperLayerInfo) {
        try {
            int tokenKey = token.get();
            for (IExtPhoneCallback callback : retrieveCallbacks(tokenKey,
                    ExtPhoneCallbackListener.EVENT_ON_UPPER_LAYER_IND_INFO)) {
                Log.d(TAG, "onUpperLayerIndInfo: Responding back for transaction = " +
                        mInflightRequests.get(tokenKey));
                callback.onUpperLayerIndInfo(slotId, token, status, upperLayerInfo);
                mInflightRequests.remove(tokenKey);
            }
        } catch (RemoteException e) {
            Log.d(TAG, "onUpperLayerIndInfo: Exception = " + e);
        }
    }

    private void on5gConfigInfo(int slotId, Token token, Status status,
                                        NrConfigType nrConfigType) {
        try {
            int tokenKey = token.get();
            for (IExtPhoneCallback callback : retrieveCallbacks(tokenKey,
                    ExtPhoneCallbackListener.EVENT_ON_5G_CONFIG_INFO)) {
                Log.d(TAG, "on5gConfigInfo: Responding back for transaction = " +
                        mInflightRequests.get(tokenKey));
                callback.on5gConfigInfo(slotId, token, status, nrConfigType);
                mInflightRequests.remove(tokenKey);
            }
        } catch (RemoteException e) {
            Log.d(TAG, "on5gConfigInfo: Exception = " + e);
        }
    }

    private void onSignalStrength(int slotId, Token token, Status status,
                                        SignalStrength signalStrength) {
        try {
            int tokenKey = token.get();
            for (IExtPhoneCallback callback : retrieveCallbacks(tokenKey,
                    ExtPhoneCallbackListener.EVENT_ON_SIGNAL_STRENGTH)) {
                Log.d(TAG, "onSignalStrength: Responding back for transaction = " +
                        mInflightRequests.get(tokenKey));
                callback.onSignalStrength(slotId, token, status, signalStrength);
                mInflightRequests.remove(tokenKey);
            }
        } catch (RemoteException e) {
            Log.d(TAG, "onSignalStrength: Exception = " + e);
        }
    }

    private void getQtiRadioCapabilityResponse(int slotId, Token token, Status status,
                                int raf) {
        try {
            int tokenKey = token.get();
            for (IExtPhoneCallback callback : retrieveCallbacks(tokenKey,
                    ExtPhoneCallbackListener.EVENT_GET_QTIRADIO_CAPABILITY_RESPONSE)) {
                Log.d(TAG, "getQtiRadioCapabilityResponse: Responding back" +
                        "for transaction = " + mInflightRequests.get(tokenKey));
                callback.getQtiRadioCapabilityResponse(slotId, token, status, raf);
                mInflightRequests.remove(tokenKey);
            }
        } catch (RemoteException e) {
            Log.d(TAG, "getQtiRadioCapabilityResponse: Exception = " + e);
        }
    }

    private void sendCdmaSmsResponse(int slotId, Token token, Status status,
                        SmsResult sms) {
        try {
            int tokenKey = token.get();
            for (IExtPhoneCallback callback : retrieveCallbacks(tokenKey,
                    ExtPhoneCallbackListener.EVENT_SEND_CDMA_SMS_RESPONSE)) {
                Log.d(TAG, "sendCdmaSmsResponse: Responding back for transaction = " +
                        mInflightRequests.get(tokenKey));
                callback.sendCdmaSmsResponse(slotId, token, status, sms);
                mInflightRequests.remove(tokenKey);
            }
        } catch (RemoteException e) {
            Log.d(TAG, "sendCdmaSmsResponse: Exception = " + e);
        }
    }

    private void startNetworkScanResponse(int slotId, Token token, int errorCode) {
        try {
            int tokenKey = token.get();
            for (IExtPhoneCallback callback : retrieveCallbacks(tokenKey,
                    ExtPhoneCallbackListener.EVENT_START_NETWORK_SCAN_RESPONSE)) {
                Log.d(TAG, "startNetworkScanResponse: Responding back for transaction = " +
                        mInflightRequests.get(tokenKey));
                callback.startNetworkScanResponse(slotId, token, errorCode);
                mInflightRequests.remove(tokenKey);
            }
        } catch (RemoteException e) {
            Log.d(TAG, "startNetworkScanResponse: Exception = " + e);
        }
    }

    private void stopNetworkScanResponse(int slotId, Token token, int errorCode) {
        try {
            int tokenKey = token.get();
            for (IExtPhoneCallback callback : retrieveCallbacks(tokenKey,
                    ExtPhoneCallbackListener.EVENT_STOP_NETWORK_SCAN_RESPONSE)) {
                Log.d(TAG, "stopNetworkScanResponse: Responding back for transaction = " +
                        mInflightRequests.get(tokenKey));
                callback.stopNetworkScanResponse(slotId, token, errorCode);
                mInflightRequests.remove(tokenKey);
            }
        } catch (RemoteException e) {
            Log.d(TAG, "stopNetworkScanResponse: Exception = " + e);
        }
    }

    private void setNetworkSelectionModeManualResponse(int slotId, Token token, int errorCode) {
        try {
            int tokenKey = token.get();
            for (IExtPhoneCallback callback : retrieveCallbacks(tokenKey,
                    ExtPhoneCallbackListener.
                    EVENT_SET_NETWORK_SELECTION_MODE_MANUAL_RESPONSE)) {
                Log.d(TAG, "setNetworkSelectionModeManualResponse: " +
                        "Responding back for transaction = " + mInflightRequests.get(tokenKey));
                callback.setNetworkSelectionModeManualResponse(slotId, token, errorCode);
                mInflightRequests.remove(tokenKey);
            }
        } catch (RemoteException e) {
            Log.d(TAG, "setNetworkSelectionModeManualResponse: Exception = " + e);
        }
    }

    private void setNetworkSelectionModeAutomaticResponse(int slotId, Token token, int errorCode) {
        try {
            int tokenKey = token.get();
            for (IExtPhoneCallback callback : retrieveCallbacks(tokenKey,
                    ExtPhoneCallbackListener.
                    EVENT_SET_NETWORK_SELECTION_MODE_AUTOMATIC_RESPONSE)) {
                Log.d(TAG, "setNetworkSelectionModeAutomaticResponse: " +
                        "Responding back for transaction = " + mInflightRequests.get(tokenKey));
                callback.setNetworkSelectionModeAutomaticResponse(slotId, token, errorCode);
                mInflightRequests.remove(tokenKey);
            }
        } catch (RemoteException e) {
            Log.d(TAG, "setNetworkSelectionModeAutomaticResponse: Exception = " + e);
        }
    }

    private void getNetworkSelectionModeResponse(int slotId, Token token, Status status,
            NetworkSelectionMode modes) {
        try {
            int tokenKey = token.get();
            for (IExtPhoneCallback callback : retrieveCallbacks(tokenKey,
                    ExtPhoneCallbackListener.EVENT_GET_NETWORK_SELECTION_MODE_RESPONSE)) {
                Log.d(TAG, "getNetworkSelectionModeResponse: " +
                        "Responding back for transaction = " + mInflightRequests.get(tokenKey));
                callback.getNetworkSelectionModeResponse(slotId, token, status, modes);
                mInflightRequests.remove(tokenKey);
            }
        } catch (RemoteException e) {
            Log.d(TAG, "getNetworkSelectionModeResponse: Exception = " + e);
        }
    }

    private void networkScanResult(int slotId, Token token, NetworkScanResult nsr) {
        try {
            for (IExtPhoneCallback callback : retrieveCallbacks(Token.UNSOL,
                    ExtPhoneCallbackListener.EVENT_NETWORK_SCAN_RESULT)) {
                Log.d(TAG, "networkScanResult: = " + nsr);
                callback.networkScanResult(slotId, token, nsr.scanStatus, nsr.scanError,
                        nsr.networkInfos);
            }
        } catch (RemoteException e) {
            Log.d(TAG, "networkScanResult: Exception = " + e);
        }
    }

    private void setCarrierInfoForImsiEncryptionResponse(int slotId, Token token, Status status,
                        QRadioResponseInfo info) {
        try {
            int tokenKey = info.getSerial();
            for (IExtPhoneCallback callback : retrieveCallbacks(tokenKey,
                    ExtPhoneCallbackListener.
                    EVENT_SET_CARRIER_INFO_FOR_IMSI_ENCRYPTION_RESPONSE)) {
                Log.d(TAG, "setCarrierInfoForImsiEncryptionResponse: Responding back for" +
                        " transaction = " + mInflightRequests.get(tokenKey));
                callback.setCarrierInfoForImsiEncryptionResponse(slotId, token, info);
                mInflightRequests.remove(tokenKey);
            }
        } catch (RemoteException e) {
            Log.d(TAG, "setCarrierInfoForImsiEncryptionResponse: Exception = " + e);
        }
    }

    private void sendcallforwardqueryResponse(Token token, Status status, QtiCallForwardInfo[]
            callForwardInfoList) {
        try {
            int tokenKey = token.get();
            for (IExtPhoneCallback callback : retrieveCallbacks(tokenKey,
                    ExtPhoneCallbackListener.EVENT_QUERY_CALL_FORWARD_STATUS_RESPONSE)) {
                Log.d(TAG, "sendcallforwardqueryResponse: Responding back for transaction = " +
                        mInflightRequests.get(tokenKey));
                callback.queryCallForwardStatusResponse(status, callForwardInfoList);
                mInflightRequests.remove(tokenKey);
            }
        } catch (RemoteException e) {
            Log.d(TAG, "sendcallforwardqueryResponse: Exception = " + e);
        }
    }

    private void sendfacilityLockResponse(Token token, Status status, int[] result) {
        try {
            int tokenKey = token.get();
            for (IExtPhoneCallback callback : retrieveCallbacks(tokenKey,
                    ExtPhoneCallbackListener.EVENT_GET_FACILITY_LOCK_FOR_APP_RESPONSE)) {
                Log.d(TAG, "sendfacilityLockResponse: Responding back for transaction = " +
                        mInflightRequests.get(tokenKey));
                callback.getFacilityLockForAppResponse(status, result);
                mInflightRequests.remove(tokenKey);
            }
        } catch (RemoteException e) {
            Log.d(TAG, "sendfacilityLockResponse: Exception = " + e);
        }
    }

    // inform IMEI change indication to registered external clients
    void sendImeiInfoInd(QtiImeiInfo[] imeiInfo) {
        try {
            for (IExtPhoneCallback callback : retrieveCallbacks(Token.UNSOL,
                    ExtPhoneCallbackListener.EVENT_ON_IMEI_TYPE_CHANGED)) {
                Log.d(TAG, "sendImeiInfoInd: = " + imeiInfo);
                callback.onImeiTypeChanged(imeiInfo);
            }
        } catch (RemoteException e) {
            Log.d(TAG, "sendImeiInfoInd: Exception = " + e);
        }
    }

    private void sendImeiInfoResponse(int slotId,
            Token token, Status status, QtiImeiInfo imeiInfo) {
        int tokenKey = token.get();
        synchronized (mInternalCallbackList) {
            for (IQtiRadioInternalCallback callback : mInternalCallbackList) {
                Log.d(TAG, "sendImeiInfoResponse: Responding back for transaction = " +
                        mInflightRequests.get(tokenKey));
                callback.getImeiResponse(slotId, token, status, imeiInfo);
                mInflightRequests.remove(tokenKey);
            }
        }
    }

    private void sendImeiInfoIndInternal(int slotId, QtiImeiInfo imeiInfo) {
        synchronized (mInternalCallbackList) {
            for (IQtiRadioInternalCallback callback : mInternalCallbackList) {
                Log.d(TAG, "sendImeiInfoIndInternal: slotId = " + slotId);
                callback.onImeiChanged(slotId, imeiInfo);
            }
        }
    }

    private void onGetAllEsimProfiles(int slotId, int refNum) {
        boolean ntnCallbackAvailable = false;
        synchronized (mInternalCallbackList) {
            for (IQtiRadioInternalCallback callback : mInternalCallbackList) {
                callback.onGetAllEsimProfiles(slotId, refNum);
                if (callback instanceof QtiNtnProfileCallback) {
                    ntnCallbackAvailable = true;
                }
            }
            Log.d(TAG, "onGetAllEsimProfiles callback available: " + ntnCallbackAvailable);
        }
        if (!ntnCallbackAvailable) {
            mEsimProfileIndCache[0] = slotId;
            mEsimProfileIndCache[1] = refNum;
        }
    }

    private void onEnableEsimProfile(int slotId, int refNum, String iccId) {
        synchronized (mInternalCallbackList) {
            for (IQtiRadioInternalCallback callback : mInternalCallbackList) {
                Log.d(TAG, "onEnableEsimProfile:  " + iccId);
                callback.onEnableEsimProfile(slotId, refNum, iccId);
            }
        }
    }

    private void onDisableEsimProfile(int slotId, int refNum, String iccId) {
        synchronized (mInternalCallbackList) {
            for (IQtiRadioInternalCallback callback : mInternalCallbackList) {
                Log.d(TAG, "onDisableEsimProfile: " + iccId);
                callback.onDisableEsimProfile(slotId, refNum, iccId);
            }
        }
    }

    void registerInternalCallback(IQtiRadioInternalCallback callback) {
        synchronized (mInternalCallbackList) {
            Log.d(TAG, "add internal callback = " + callback);
            mInternalCallbackList.add(callback);
        }
        if (callback instanceof QtiNtnProfileCallback) {
            // check if pending esimProfiles indication is present
            if (mEsimProfileIndCache[0] != -1) {
                mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage(
                        EVENT_GET_ALL_ESIM_PROFILES, mEsimProfileIndCache[0],
                        mEsimProfileIndCache[1]));
                Arrays.fill(mEsimProfileIndCache, -1);
            }
        }
    }

    void unRegisterInternalCallback(IQtiRadioInternalCallback callback) {
        synchronized (mInternalCallbackList) {
            Log.d(TAG, "remove internal callback = " + callback);
            mInternalCallbackList.remove(callback);
        }
    }

    public static class IQtiRadioInternalCallback {

        public void getImeiResponse(int slotId, Token token, Status status, QtiImeiInfo imeiInfo) {
             // do nothing
        }

        public void onImeiChanged(int slotId, QtiImeiInfo imeiInfo) {
             // do nothing
        }

        public void onMcfgRefresh(QtiMcfgRefreshInfo refreshInfo) {
            // do nothing
        }

        public void setCellularRoamingPreferenceResponse(int slotId, Token token, Status status) {
            // do nothing
        }

        public void setCiwlanModeUserPreferenceResponse(int slotId, Token token, Status status) {
            // do nothing
        }

        public void onGetAllEsimProfiles(int slotId, int refNum) {
            // do nothing
        }

        public void onEnableEsimProfile(int slotId, int refNum, String iccId) {
            // do nothing
        }

        public void onDisableEsimProfile(int slotId, int refNum, String iccId) {
            // do nothing
        }
    }

    private void onSendUserPreferenceForDataDuringVoiceCall(int slotId,
            Token token, Status status) {
        int tokenKey = token.get();
        try {
            for (IExtPhoneCallback callback : retrieveCallbacks(tokenKey,
                    ExtPhoneCallbackListener.
                    EVENT_ON_SEND_USER_PREFERENCE_FOR_DATA_DURING_VOICE_CALL)) {
                Log.d(TAG, "onSendUserPreferenceForDataDuringVoiceCall:" +
                        " Responding back for transaction = " +
                        mInflightRequests.get(tokenKey));
                callback.onSendUserPreferenceForDataDuringVoiceCall(slotId,
                        token, status);
            }
        } catch (RemoteException e) {
            Log.d(TAG, "onSendUserPreferenceForDataDuringVoiceCall: Exception = " + e);
        }
        mInflightRequests.remove(tokenKey);
    }

    private void onDdsSwitchCapabilityChange(int slotId, Token token,
            Status status, boolean support) {
        int tokenKey = token.get();
        try {
            for (IExtPhoneCallback callback : retrieveCallbacks(tokenKey,
                    ExtPhoneCallbackListener.EVENT_ON_DDS_SWITCH_CAPABILITY_CHANGE)) {
                Log.d(TAG, "onDdsSwitchCapabilityChange: " +
                        " Responding back for transaction = " +
                        mInflightRequests.get(tokenKey));
                callback.onDdsSwitchCapabilityChange(slotId, token, status, support);
            }
        } catch (RemoteException e) {
            Log.d(TAG, "onDdsSwitchCapabilityChange: Exception = " + e);
        }
        mInflightRequests.remove(tokenKey);
    }

    private void onDdsSwitchCriteriaChange(int slotId, Token token, boolean telephonyDdsSwitch) {
        int tokenKey = token.get();
        try {
            for (IExtPhoneCallback callback : retrieveCallbacks(tokenKey,
                    ExtPhoneCallbackListener.EVENT_ON_DDS_SWITCH_CRITERIA_CHANGE)) {
                Log.d(TAG, "onDdsSwitchCriteriaChange:" +
                        " Responding back for transaction = " +
                        mInflightRequests.get(tokenKey));
                callback.onDdsSwitchCriteriaChange(slotId, telephonyDdsSwitch);
            }
        } catch (RemoteException e) {
            Log.d(TAG, "onDdsSwitchCriteriaChange: Exception = " + e);
        }
        mInflightRequests.remove(tokenKey);
    }

    private void onDdsSwitchRecommendation(int slotId, Token token, int recommendedSlotId) {
        int tokenKey = token.get();
        try {
            for (IExtPhoneCallback callback : retrieveCallbacks(tokenKey,
                    ExtPhoneCallbackListener.EVENT_ON_DDS_SWITCH_RECOMMENDATION)) {
                Log.d(TAG, "onDdsSwitchRecommendation:" +
                        " Responding back for transaction = " +
                        mInflightRequests.get(tokenKey));
                callback.onDdsSwitchRecommendation(slotId, recommendedSlotId);
            }
        } catch (RemoteException e) {
            Log.d(TAG, "onDdsSwitchRecommendation: Exception = " + e);
        }
        mInflightRequests.remove(tokenKey);
    }

    private void onDataDeactivateDelayTime(int slotId, Token token, long delayTimeMilliSecs) {
        int tokenKey = token.get();
        try {
            for (IExtPhoneCallback callback : retrieveCallbacks(tokenKey,
                    ExtPhoneCallbackListener.EVENT_ON_DATA_DEACTIVATE_DELAY_TIME)) {
                Log.d(TAG, "onDataDeactivateDelayTime:" +
                        " Responding back for transaction = " +
                        mInflightRequests.get(tokenKey));
                callback.onDataDeactivateDelayTime(slotId, delayTimeMilliSecs);
            }
        } catch (RemoteException e) {
            Log.d(TAG, "onDataDeactivateDelayTime: Exception = " + e);
        }
        mInflightRequests.remove(tokenKey);
    }

    private void onEpdgOverCellularDataSupported(int slotId, Token token, boolean support) {
        int tokenKey = token.get();
        try {
            for (IExtPhoneCallback callback : retrieveCallbacks(tokenKey,
                    ExtPhoneCallbackListener.EVENT_ON_EPDG_OVER_CELLULAR_DATA_SUPPORTED)) {
                Log.d(TAG, "onEpdgOverCellularDataSupported:" +
                        " Responding back for transaction = " +
                        mInflightRequests.get(tokenKey));
                callback.onEpdgOverCellularDataSupported(slotId, support);
            }
        } catch (RemoteException e) {
            Log.d(TAG, "onEpdgOverCellularDataSupported: Exception = " + e);
        }
        mInflightRequests.remove(tokenKey);
    }

    void sendMcfgRefreshInfo(Token token, QtiMcfgRefreshInfo refreshInfo) {
        synchronized (mInternalCallbackList) {
            for (IQtiRadioInternalCallback callback : mInternalCallbackList) {
                Log.d(TAG, "sendMcfgRefreshInfo");
                callback.onMcfgRefresh(refreshInfo);
            }
        }
    }

    private void getQosParametersResponse(int slotId, Token token,
            Status status, QosParametersResult qosParams) {
        int tokenKey = token.get();
        try {
            for (IExtPhoneCallback callback : retrieveCallbacks(tokenKey,
                    ExtPhoneCallbackListener.EVENT_GET_QOS_PARAMETERS_RESPONSE)) {
                Log.d(TAG, "getQosParametersResponse: " +
                        " Responding back for transaction = " +
                        mInflightRequests.get(tokenKey));
                callback.getQosParametersResponse(slotId, token, status, qosParams);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "getQosParametersResponse: caught remote exception", e);
        }
        mInflightRequests.remove(tokenKey);
    }

    private void onQosParametersChanged(int slotId, int cid, QosParametersResult qosParams) {
        Log.d(TAG, "onQosParametersChanged: slotId: " + slotId
                + ", cid: " + cid
                + ", qosParams: " + qosParams);
        try {
            for (IExtPhoneCallback callback : retrieveCallbacks(Token.UNSOL,
                    ExtPhoneCallbackListener.EVENT_ON_QOS_PARAMETERS_CHANGED)) {
                callback.onQosParametersChanged(slotId, cid, qosParams);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "onQosParametersChanged: caught remote exception", e);
        }
    }

    private void sendSimPersoUnlockStatusChange(int slotId,
            QtiPersoUnlockStatus persoUnlockStatus) {
        try {
            for (IExtPhoneCallback callback : retrieveCallbacks(Token.UNSOL,
                    ExtPhoneCallbackListener.EVENT_ON_SIM_PERSO_UNLOCK_STATUS_CHANGE)) {
                Log.d(TAG, "sendSimPersoUnlockStatusChange: = " + persoUnlockStatus);
                callback.onSimPersoUnlockStatusChange(slotId, persoUnlockStatus);
            }
        } catch (RemoteException e) {
            Log.d(TAG, "sendSimPersoUnlockStatusChange: Exception = " + e);
        }
    }

    private void onCiwlanAvailable(int slotId, boolean ciwlanAvailable) {
        mExtTelephonyServiceImpl.updateCiwlanAvailableCache(slotId, ciwlanAvailable);
        try {
            for (IExtPhoneCallback callback : retrieveCallbacks(Token.UNSOL,
                    ExtPhoneCallbackListener.EVENT_ON_CIWLAN_AVAILABLE)) {
                callback.onCiwlanAvailable(slotId, ciwlanAvailable);
            }
        } catch (RemoteException e) {
            Log.d(TAG, "onCiwlanAvailable: Exception = " + e);
        }
    }

    private void setCiwlanModeUserPreferenceResponse(int slotId,
            Token token, Status status) {
        int tokenKey = token.get();
        try {
            for (IExtPhoneCallback callback : retrieveCallbacks(tokenKey,
                    ExtPhoneCallbackListener.EVENT_SET_CIWLAN_MODE_USER_PREFERENCE_RESPONSE)) {
                Log.d(TAG, "setCiwlanModeUserPreferenceResponse: " +
                        " Responding back for transaction = " +
                        mInflightRequests.get(tokenKey));
                callback.setCiwlanModeUserPreferenceResponse(slotId, token, status);
            }
        } catch (RemoteException e) {
            Log.d(TAG, "setCiwlanModeUserPreferenceResponse: Exception = " + e);
        }
        mInflightRequests.remove(tokenKey);
    }

    private void onCiwlanConfigChange(int slotId, CiwlanConfig ciwlanConfig) {
        mExtTelephonyServiceImpl.updateCiwlanConfigCache(slotId, ciwlanConfig);
        try {
            for (IExtPhoneCallback callback : retrieveCallbacks(Token.UNSOL,
                    ExtPhoneCallbackListener.EVENT_ON_CIWLAN_CONFIG_CHANGE)) {
                callback.onCiwlanConfigChange(slotId, ciwlanConfig);
            }
        } catch (RemoteException e) {
            Log.d(TAG, "onCiwlanConfigChange: Exception = " + e);
        }
    }

    public Token setNrUltraWidebandIconConfig(int slotId, int sib2Value,
            NrUwbIconBandInfo saBandInfo, NrUwbIconBandInfo nsaBandInfo,
            ArrayList<NrUwbIconRefreshTime> refreshTimes,
            NrUwbIconBandwidthInfo bandwidthInfo) throws RemoteException {
        if (!isSlotIdValid(slotId)) {
            Log.d(TAG, "setNrUltraWidebandIconConfig: invalid slotId");
            return null;
        }
        Token token = getNextToken();
        mInflightRequests.put(token.get(),
            new Transaction(token, "setNrUltraWidebandIconConfig", null));
        Log.d(TAG, "setNrUltraWidebandIconConfig: slotId = " + slotId + " token = " + token);
        try {
            mQtiRadio[slotId].setNrUltraWidebandIconConfig(token, sib2Value, saBandInfo,
                    nsaBandInfo, refreshTimes, bandwidthInfo);
        } catch (RemoteException e) {
            mInflightRequests.remove(token.get());
            throw e;
        }
        return token;
    }

    private void onSetNrUltraWidebandIconConfigResponse(int slotId, Token token) {
        int tokenKey = token.get();
        Log.d(TAG, "onSetNrUltraWidebandIconConfigResponse: token = " + tokenKey);
        mInflightRequests.remove(tokenKey);
    }

    public int getInflightRequestsCount() {
        return mInflightRequests.size();
    }

    private void setCellularRoamingPreferenceResponse(int slotId, Token token, Status status) {
        int tokenKey = token.get();
        try {
            for (IExtPhoneCallback callback : retrieveCallbacks(tokenKey,
                    ExtPhoneCallbackListener.EVENT_SET_CELLULAR_ROAMING_PREFERENCE_RESPONSE)) {
                Log.d(TAG, "setCellularRoamingPreferenceResponse: Responding back for " +
                        "transaction = " + mInflightRequests.get(tokenKey));
                callback.setCellularRoamingPreferenceResponse(slotId, token, status);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "setCellularRoamingPreferenceResponse: caught remote exception", e);
        }
        synchronized (mInternalCallbackList) {
            for (IQtiRadioInternalCallback callback : mInternalCallbackList) {
                Log.d(TAG, "setCellularRoamingPreferenceResponse: Responding back for " +
                        "transaction = " + mInflightRequests.get(tokenKey));
                callback.setCellularRoamingPreferenceResponse(slotId, token, status);
            }
        }
        mInflightRequests.remove(tokenKey);
    }

    private void onNrIconChange(int slotId, NrIcon icon) {
        try {
            for (IExtPhoneCallback callback : retrieveCallbacks(Token.UNSOL,
                    ExtPhoneCallbackListener.EVENT_ON_NR_ICON_CHANGE)) {
                Log.d(TAG, "onNrIconChange: icon = " + icon);
                callback.onNrIconChange(slotId, icon);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "onNrIconChange: Exception = " + e);
        }
    }

    private void onNrIconResponse(int slotId, Token token, Status status, NrIcon icon) {
        int tokenKey = token.get();
        try {
            for (IExtPhoneCallback callback : retrieveCallbacks(tokenKey,
                    ExtPhoneCallbackListener.EVENT_QUERY_NR_ICON_RESPONSE)) {
                Log.d(TAG, "onNrIconResponse: Responding back for transaction = "
                        + mInflightRequests.get(tokenKey));
                callback.onNrIconResponse(slotId, token, status, icon);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "onNrIconResponse: Exception = " + e);
        }
    }


    private void dumpInflightRequests(PrintWriter pw){
        for(Integer key : mInflightRequests.keySet()) {
            pw.println("Token = " + key + " => " + mInflightRequests.get(key));
        }
    }

    // Dump service.
    public void dump(FileDescriptor fd, PrintWriter printwriter, String[] args) {
        PrintWriter pw = printwriter;
        pw.println("5G-Middleware:");
        pw.println("mQtiRadio = " + mQtiRadio);
        pw.flush();

        pw.println("Inflight requests : ");
        dumpInflightRequests(pw);
        pw.flush();
    }

}
