/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qti.phone;

import static com.qti.extphone.ExtTelephonyManager.FEATURE_SMART_TEMP_DDS_VIA_RADIO_CONFIG;
import static com.qti.extphone.ExtTelephonyManager.FEATURE_EMERGENCY_ENHANCEMENT;

import android.content.Context;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Log;

import com.qti.extphone.DualDataRecommendation;
import com.qti.extphone.ExtTelephonyManager;
import com.qti.extphone.MsimPreference;
import com.qti.extphone.QtiSimType;
import com.qti.extphone.Status;
import com.qti.extphone.Token;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import vendor.qti.hardware.radio.RadioError;
import vendor.qti.hardware.radio.RadioResponseInfo;
import vendor.qti.hardware.radio.qtiradioconfig.IQtiRadioConfig;
import vendor.qti.hardware.radio.qtiradioconfig.IQtiRadioConfigIndication;
import vendor.qti.hardware.radio.qtiradioconfig.IQtiRadioConfigResponse;
import vendor.qti.hardware.radio.qtiradioconfig.SimType;
import vendor.qti.hardware.radio.qtiradioconfig.SimTypeInfo;

public class QtiRadioConfigAidl implements IQtiRadioConfigConnectionInterface {
    private final String LOG_TAG = "QtiRadioConfigAidl";
    private IQtiRadioConfigConnectionCallback mCallback;
    private Context mContext;

    private final Token UNSOL = new Token(-1);
    private ConcurrentHashMap<Integer, Token> mInflightRequests = new ConcurrentHashMap<Integer,
            Token>();

    // Synchronization object of HAL interfaces
    private final Object mHalSync = new Object();

    static final String QTI_RADIO_CONFIG_SERVICE_NAME =
            "vendor.qti.hardware.radio.qtiradioconfig.IQtiRadioConfig/default";
    private IBinder mQtiRadioConfigBinder;
    private QtiRadioConfigDeathRecipient mQtiRadioConfigDeathRecipient;
    private IQtiRadioConfig mQtiRadioConfig;
    private IQtiRadioConfigResponse mQtiRadioConfigResponse;
    private IQtiRadioConfigIndication mQtiRadioConfigIndication;

    // List of features for isSupportedFeature API
    private static final int INVALID_FEATURE = -1;
    // private static final INTERNAL_AIDL_REORDERING = 1;
    // private static final DSDS_TRANSITION = 2;
    private static final int SMART_TEMP_DDS_VIA_RADIO_CONFIG = 3;
    private static final int EMERGENCY_ENHANCEMENT = 4;
    // private static final int TDSCDMA_SUPPORTED = 5;

    public QtiRadioConfigAidl(Context context) {
        mContext = context;
        mQtiRadioConfigDeathRecipient = new QtiRadioConfigDeathRecipient();
        initQtiRadioConfig();
    }

    private void initQtiRadioConfig() {
        Log.i(LOG_TAG,"initQtiRadioConfig");
        mQtiRadioConfigBinder = Binder.allowBlocking(
                ServiceManager.waitForDeclaredService(QTI_RADIO_CONFIG_SERVICE_NAME));
        if (mQtiRadioConfigBinder == null) {
            Log.e(LOG_TAG, "initQtiRadioConfig failed");
            return;
        }

        IQtiRadioConfig qtiRadioConfig =
                vendor.qti.hardware.radio.qtiradioconfig.IQtiRadioConfig.Stub.asInterface(
                        mQtiRadioConfigBinder);
        if (qtiRadioConfig == null) {
            Log.e(LOG_TAG,"Get binder for IQtiRadioConfig stable AIDL failed");
            return;
        }
        Log.i(LOG_TAG,"Get binder for IQtiRadioConfig stable AIDL is successful");

        try {
            mQtiRadioConfigBinder.linkToDeath(mQtiRadioConfigDeathRecipient, 0 /* Not Used */);
        } catch (RemoteException ex) {
            Log.e(LOG_TAG, "initQtiRadioConfig: Failed to link to death recipient", ex);
        }

        synchronized (mHalSync) {
            mQtiRadioConfigResponse = new QtiRadioConfigResponseAidl();
            mQtiRadioConfigIndication = new QtiRadioConfigIndicationAidl();
            try {
                qtiRadioConfig.setCallbacks(mQtiRadioConfigResponse,
                        mQtiRadioConfigIndication);
            } catch (RemoteException ex) {
                Log.e(LOG_TAG, "initQtiRadioConfig: setCallbacks failed", ex);
            }
            mQtiRadioConfig = qtiRadioConfig;
        }
    }

    @Override
    public int getHalVersion() {
        try {
            return getQtiRadioConfig().getInterfaceVersion();
        } catch (RemoteException ex) {
            Log.e(LOG_TAG, "getInterfaceVersion Failed.", ex);
            return -1;
        }
    }

    @Override
    public void getSecureModeStatus(Token token) {
        int serial = token.get();
        Log.d(LOG_TAG, "getSecureModeStatus: serial = " + serial);
        mInflightRequests.put(serial, token);

        try {
            getQtiRadioConfig().getSecureModeStatus(serial);
        } catch (RemoteException ex) {
            mInflightRequests.remove(serial, token);
            Log.e(LOG_TAG, "getSecureModeStatus Failed.", ex);
        }
    }

    @Override
    public void setMsimPreference(Token token, MsimPreference pref) {
        int serial = token.get();
        Log.d(LOG_TAG, "setMsimPreference: serial = " + serial);
        mInflightRequests.put(serial, token);

        try {
            getQtiRadioConfig().setMsimPreference(serial, pref.get());
        } catch (RemoteException ex) {
            mInflightRequests.remove(serial, token);
            Log.e(LOG_TAG, "setMsimPreference Failed.", ex);
        }
    }

    @Override
    public void getSimTypeInfo(Token token) {
        int serial = token.get();
        Log.d(LOG_TAG, "getSimTypeInfo: serial = " + serial);
        mInflightRequests.put(serial, token);

        try {
            getQtiRadioConfig().getSimTypeInfo(serial);
        } catch (RemoteException ex) {
            mInflightRequests.remove(serial, token);
            Log.e(LOG_TAG, "getSimTypeInfo Failed.", ex);
        }
    }

    @Override
    public void getDualDataCapability(Token token) {
        int serial = token.get();
        Log.d(LOG_TAG, "getDualDataCapability: serial = " + serial);
        mInflightRequests.put(serial, token);

        try {
            getQtiRadioConfig().getDualDataCapability(serial);
        } catch (RemoteException ex) {
            mInflightRequests.remove(serial, token);
            Log.e(LOG_TAG, "getDualDataCapability Failed.", ex);
        }
    }

    @Override
    public void setSimType(Token token, QtiSimType[] qtiSimType) {
        int serial = token.get();
        Log.d(LOG_TAG, "setSimType: serial = " + serial);
        mInflightRequests.put(serial, token);

        int length = qtiSimType.length;
        int[] simType = new int[length];

        // Convert QtiSimType enums to sAidl SimType enums
        for (int index = 0; index < length; index++) {
            Log.d(LOG_TAG, "setSimType: type = " + qtiSimType[index]);
            if (qtiSimType[index].get() == QtiSimType.SIM_TYPE_PHYSICAL) {
                simType[index] = SimType.SIM_TYPE_PHYSICAL;
            } else if (qtiSimType[index].get() == QtiSimType.SIM_TYPE_IUICC) {
                simType[index] = SimType.SIM_TYPE_INTEGRATED;
            } else if (qtiSimType[index].get() == QtiSimType.SIM_TYPE_ESIM) {
                simType[index] = SimType.SIM_TYPE_ESIM;
            } else {
                Log.e(LOG_TAG, "setSimType Invalid SimType " + qtiSimType[index].get());
            }
        }

        try {
            getQtiRadioConfig().setSimType(serial, simType);
        } catch (RemoteException ex) {
            mInflightRequests.remove(serial, token);
            Log.e(LOG_TAG, "setSimType Failed.", ex);
        }
    }

    @Override
    public void setDualDataUserPreference(Token token, boolean enable) {
        int serial = token.get();
        Log.d(LOG_TAG, "setDualDataUserPreference: serial = " + serial
                + " preference = " + enable);
        mInflightRequests.put(serial, token);

        try {
            getQtiRadioConfig().setDualDataUserPreference(serial, enable);
        } catch (RemoteException ex) {
            mInflightRequests.remove(serial, token);
            Log.e(LOG_TAG, "getSecureModeStatus Failed.", ex);
        }
    }

    @Override
    public void getCiwlanCapability(Token token) {
        int serial = token.get();
        Log.d(LOG_TAG, "getCiwlanCapability: serial = " + serial);
        mInflightRequests.put(serial, token);

        try {
            getQtiRadioConfig().getCiwlanCapability(serial);
        } catch (RemoteException ex) {
            mInflightRequests.remove(serial, token);
            Log.e(LOG_TAG, "getCiwlanCapability Failed.", ex);
        }
    }

    @Override
    public void getDdsSwitchCapability(Token token) {
        int serial = token.get();
        Log.d(LOG_TAG, "getDdsSwitchCapability: serial = " + serial);
        mInflightRequests.put(serial, token);

        try {
            getQtiRadioConfig().getDdsSwitchCapability(serial);
        } catch (RemoteException ex) {
            mInflightRequests.remove(serial, token);
            Log.e(LOG_TAG, "getDdsSwitchCapability Failed.", ex);
        }
    }

    @Override
    public void sendUserPreferenceForDataDuringVoiceCall(Token token, boolean[] isAllowedOnSlot) {
        int serial = token.get();
        Log.d(LOG_TAG, "sendUserPreferenceForDataDuringVoiceCall"
                + ", serial = " + serial
                + ", isAllowedOnSlot = " + Arrays.toString(isAllowedOnSlot));
        mInflightRequests.put(serial, token);

        try {
            getQtiRadioConfig().sendUserPreferenceForDataDuringVoiceCall(serial, isAllowedOnSlot);
        } catch (RemoteException ex) {
            mInflightRequests.remove(serial, token);
            Log.e(LOG_TAG, "sendUserPreferenceForDataDuringVoiceCall Failed.", ex);
        }
    }

    class QtiRadioConfigResponseAidl extends IQtiRadioConfigResponse.Stub {
        @Override
        public final int getInterfaceVersion() {
            return vendor.qti.hardware.radio.qtiradioconfig.IQtiRadioConfigResponse.VERSION;
        }

        @Override
        public String getInterfaceHash() {
            return vendor.qti.hardware.radio.qtiradioconfig.IQtiRadioConfigResponse.HASH;
        }

        /**
         * Response to IQtiRadioConfig.getSecureModeStatus
         *
         * @param serial to match request/response. Response must include same serial as request.
         * @param errorCode as per types.hal returned from RIL.
         * @param status the Secure Mode status - true: enabled, false: disabled
         */
        @Override
        public void getSecureModeStatusResponse(int serial, int errorCode, boolean status) {
            Log.d(LOG_TAG, "getSecureModeStatusResponse: " + "serial = " + serial + " errorCode = "
                    + errorCode + " status = " + status);
            if (mInflightRequests.containsKey(serial)) {
                Token token = mInflightRequests.get(serial);
                Status error = QtiRadioConfigUtils.convertHalErrorcode(errorCode);
                mCallback.getSecureModeStatusResponse(token, error, status);
                mInflightRequests.remove(serial);
            } else {
                Log.d(LOG_TAG, "getSecureModeStatusResponse: No previous request found for " +
                        "serial = " + serial);
            }
        }

        /**
         * Response to IQtiRadioConfig.setMsimPreference
         *
         * @param serial to match request/response. Response must include same serial as request.
         * @param errorCode as per types.hal returned from RIL.
         */
        @Override
        public void setMsimPreferenceResponse(int serial, int errorCode) {
            Log.d(LOG_TAG, "setMsimPreferenceResponse: " + "serial = " + serial + " errorCode = "
                    + errorCode);
            if (mInflightRequests.containsKey(serial)) {
                Token token = mInflightRequests.get(serial);
                Status status = QtiRadioConfigUtils.convertHalErrorcode(errorCode);
                mCallback.setMsimPreferenceResponse(token, status);
                mInflightRequests.remove(serial);
            } else {
                Log.d(LOG_TAG, "setMsimPreferenceResponse: No previous request found for " +
                        "serial = " + serial);
            }
        }

        /**
         * Response to IQtiRadioConfig.getSimTypeInfo
         *
         * @param serial to match request/response. Response must include same serial as request.
         * @param errorCode as per types.hal returned from RIL.
         * @param simTypeInfo the current active/supported SimType info received from modem.
         */
        @Override
        public void getSimTypeInfoResponse(int serial, int errorCode, SimTypeInfo[] simTypeInfo) {
            Log.d(LOG_TAG, "getSimTypeInfoResponse: " + "serial = " + serial + " errorCode = "
                    + errorCode);
            if (mInflightRequests.containsKey(serial)) {
                Token token = mInflightRequests.get(serial);
                Status status = QtiRadioConfigUtils.convertHalErrorcode(errorCode);

                for (int index = 0; index < simTypeInfo.length; index++) {
                    Log.d(LOG_TAG, "SimTypeInfo, current " + simTypeInfo[index].currentSimType +
                            " supported " + simTypeInfo[index].supportedSimTypes);
                }
                mCallback.getSimTypeInfoResponse(token, status, simTypeInfo);
                mInflightRequests.remove(serial);
            } else {
                Log.d(LOG_TAG, "getSimTypeInfoResponse: No previous request found for " +
                        "serial = " + serial);
            }
        }

        /**
         * Response to IQtiRadioConfig.setSimType
         *
         * @param serial to match request/response. Response must include same serial as request.
         * @param errorCode as per types.hal returned from RIL.
         */
        @Override
        public void setSimTypeResponse(int serial, int errorCode) {
            Log.d(LOG_TAG, "setSimTypeResponse: " + "serial = " + serial + " errorCode = "
                    + errorCode);
            if (mInflightRequests.containsKey(serial)) {
                Token token = mInflightRequests.get(serial);
                Status status = QtiRadioConfigUtils.convertHalErrorcode(errorCode);
                mCallback.setSimTypeResponse(token, status);
                mInflightRequests.remove(serial);
            } else {
                Log.d(LOG_TAG, "setSimTypeResponse: No previous request found for " +
                        "serial = " + serial);
            }
        }

        /**
         * Response to IQtiRadioConfig.getDualDataCapability
         *
         * @param info Response info struct containing response type, serial no. and error.
         * @param support True if modem supports dual data feature.
         */
        @Override
        public void getDualDataCapabilityResponse(RadioResponseInfo info, boolean support) {
            Log.d(LOG_TAG, "getDualDataCapabilityResponse: serial = "
                    + info.serial + " errorCode = " + info.error + " capability = " + support);
            if (mInflightRequests.containsKey(info.serial)) {
                Token token = mInflightRequests.get(info.serial);
                Status status = QtiRadioConfigUtils.convertHalErrorcode(info.error);
                mCallback.onDualDataCapabilityChanged(token, status, support);
                mInflightRequests.remove(info.serial);
            } else {
                Log.d(LOG_TAG, "getDualDataCapabilityResponse: No previous request found for " +
                        "serial = " + info.serial);
            }
        }

        /**
         * Response to IQtiRadioConfig.setDualDataUserPreference
         *
         * @param info Response info struct containing response type, serial no. and error.
         */
        @Override
        public void setDualDataUserPreferenceResponse(RadioResponseInfo info) {
            Log.d(LOG_TAG, "setDualDataUserPreferenceResponse: " +
                    "serial = " + info.serial + " errorCode = " + info.error);
            if (mInflightRequests.containsKey(info.serial)) {
                Token token = mInflightRequests.get(info.serial);
                Status status = QtiRadioConfigUtils.convertHalErrorcode(info.error);
                mCallback.setDualDataUserPreferenceResponse(token, status);
                mInflightRequests.remove(info.serial);
            } else {
                Log.d(LOG_TAG, "setDualDataUserPreferenceResponse: No previous request " +
                        "found for " + "serial = " + info.serial);
            }
        }

        /**
         * Response to IQtiRadioConfig.getCiwlanCapability
         *
         * @param info Response info struct containing response type, serial no. and error.
         * @param capability <CiwlanCapability> info of CIWLAN feature support by modem
         *        for a subscription.
         */
        @Override
        public void getCiwlanCapabilityResponse(RadioResponseInfo info, int capability) {
            Log.d(LOG_TAG, "getCiwlanCapabilityResponse: serial = "
                    + info.serial + " errorCode = "+ info.error + " capability = " + capability);
            if (mInflightRequests.containsKey(info.serial)) {
                Token token = mInflightRequests.get(info.serial);
                Status status = QtiRadioConfigUtils.convertHalErrorcode(info.error);
                mCallback.onCiwlanCapabilityChanged(token, status,
                        QtiRadioConfigUtils.convertHalCiwlanCapability(capability, info.error));
                mInflightRequests.remove(info.serial);
            } else {
                Log.d(LOG_TAG, "getCiwlanCapabilityResponse: No previous request found for " +
                        "serial = " + info.serial);
            }
        }

        /**
         * Response to IQtiRadioConfig.getDdsSwitchCapability()
         *
         * @param info Response info struct containing serial number of the request
         *        and the error code
         * @param isCapable true/false based on whether Smart Temp DDS switch capability
         *        is supported by the modem or not.
        */
        @Override
        public void getDdsSwitchCapabilityResponse(RadioResponseInfo info, boolean isCapable) {
            Log.d(LOG_TAG, "getDdsSwitchCapabilityResponse:"
                    + " serial = " + info.serial
                    + " errorCode = "+ info.error
                    + " isCapable = " + isCapable);
            if (mInflightRequests.containsKey(info.serial)) {
                Token token = mInflightRequests.get(info.serial);
                Status status = QtiRadioConfigUtils.convertHalErrorcode(info.error);
                mCallback.onDdsSwitchCapabilityChanged(token, status, isCapable);
                mInflightRequests.remove(info.serial);
            } else {
                Log.d(LOG_TAG, "getDdsSwitchCapabilityResponse: No previous request found for " +
                        "serial = " + info.serial);
            }
        }

        /**
         * Response to IQtiRadioConfig.sendUserPreferenceForDataDuringVoiceCall()
         *
         * @param info Response info struct containing serial number of the request
         *        and the error code
         */
        @Override
        public void sendUserPreferenceForDataDuringVoiceCallResponse(RadioResponseInfo info) {
            Log.d(LOG_TAG, "sendUserPreferenceForDataDuringVoiceCallResponse: "
                    + " serial = " + info.serial
                    + " errorCode = "+ info.error);
            if (mInflightRequests.containsKey(info.serial)) {
                Token token = mInflightRequests.get(info.serial);
                Status status = QtiRadioConfigUtils.convertHalErrorcode(info.error);
                mCallback.onSendUserPreferenceForDataDuringVoiceCall(token, status);
                mInflightRequests.remove(info.serial);
            } else {
                Log.d(LOG_TAG, "sendUserPreferenceForDataDuringVoiceCallResponse: No previous "
                        + "request found for serial = " + info.serial);
            }
        }
    }

    class QtiRadioConfigIndicationAidl extends IQtiRadioConfigIndication.Stub {
        @Override
        public final int getInterfaceVersion() {
            return vendor.qti.hardware.radio.qtiradioconfig.IQtiRadioConfigIndication.VERSION;
        }

        @Override
        public String getInterfaceHash() {
            return vendor.qti.hardware.radio.qtiradioconfig.IQtiRadioConfigIndication.HASH;
        }

        /**
         * Unsol msg to inform HLOS that the device has entered/exited Secure Mode
         *
         * @param enabled indicating whether Secure Mode is on or off - true: on, false: off
         */
        @Override
        public void onSecureModeStatusChange(boolean enabled) {
            Log.d(LOG_TAG, "onSecureModeStatusChange: enabled = " + enabled);
            mCallback.onSecureModeStatusChange(UNSOL, enabled);
        }

        /**
         * Received when dual data capability changes.
         *
         * @param support True if modem supports dual data feature.
         */
        @Override
        public void onDualDataCapabilityChanged(boolean support) {
            Log.d(LOG_TAG, "onDualDataCapabilityChanged: capability = " + support);
            mCallback.onDualDataCapabilityChanged(UNSOL, new Status(Status.SUCCESS), support);
        }

       /**
        * Received in the following conditions to allow/disallow internet pdn on nDDS
        * after dual data user preference is sent as true
        * to modem through IQtiRadio#setDualDataUserPreference().
        * Condition to send onDualDataRecommendation(NON_DDS and DATA_ALLOW):
        *    1)UE is in DSDA sub-mode and in full concurrent condition
        * Conditions to send onDualDataRecommendation(NON_DDS and DATA_DISALLOW):
        *    1)UE is in DSDS sub-mode
        *    2)UE is in TX sharing condition
        *    3)IRAT is initiated on nDDS when UE is in L+NR RAT combo
        *    4)nDDS is OOS
        *
        * @param token to match request/response. Response must include same token as in request,
        *        otherwise token is set to -1.
        * @param rec <DualDataRecommendation> to allow/disallow internet pdn on nDDS.
        */
        @Override
        public void onDualDataRecommendation(
                vendor.qti.hardware.radio.qtiradioconfig.DualDataRecommendation rec) {
            Log.d(LOG_TAG, "onDualDataRecommendation: Sub = " +
                    rec.sub + " Action = " + rec.action);
            mCallback.onDualDataRecommendation(UNSOL,
                    QtiRadioConfigUtils.convertHalDualDataRecommendation(rec));
        }

        /**
         * Received when CIWLAN capability changes.
         *
         * @param capability <CiwlanCapability> info of CIWLAN feature support by modem
         *        for a subscription.
         */
        @Override
        public void onCiwlanCapabilityChanged(int capability) {
            Log.d(LOG_TAG, "onCiwlanCapabilityChanged: capability = " + capability);

            mCallback.onCiwlanCapabilityChanged(UNSOL, new Status(Status.SUCCESS),
                    QtiRadioConfigUtils.convertHalCiwlanCapability(capability, RadioError.NONE));
        }

        /**
         * Indicates that modem capability of Smart Temp DDS Switch has changed.
         *
         * Upon receiving this indication, HLOS must inform the modem the user’s preference
         * for enabling temp DDS switch.
         *
         * @param isCapable true/false based on whether the device is capable of performing
         *        Smart Temp DDS switch
         */
        @Override
        public void onDdsSwitchCapabilityChanged(boolean isCapable) {
            Log.d(LOG_TAG, "onDdsSwitchCapabilityChanged");
            mCallback.onDdsSwitchCapabilityChanged(UNSOL, new Status(Status.SUCCESS), isCapable);
        }

        /**
         * Indicates that Temp DDS Switch criteria has changed.
         *
         * The boolean contained in this indication determines whether the modem-initiated
         * Smart Temp DDS Switch is to be used, or the telephony-initiated legacy Temp DDS
         * Switch logic is to be used. If telephony temp DDS switch logic is disabled, then
         * telephony must wait for modem recommendations to perform the Temp DDS switch.
         *
         * @param telephonyDdsSwitch true/false based on whether telephony temp DDS switch
         *        logic should be enabled or disabled
         */
        @Override
        public void onDdsSwitchCriteriaChanged(boolean telephonyDdsSwitch) {
            Log.d(LOG_TAG, "onDdsSwitchCriteriaChanged, telephonyDdsSwitch: "
                    + telephonyDdsSwitch);
            mCallback.onDdsSwitchCriteriaChanged(UNSOL, telephonyDdsSwitch);
        }

        /**
         * Indicates the modem's recommendation for the slot on which Temp DDS Switch has to
         * be made.
         *
         * @param recommendedSlotId slot ID to which DDS must be switched
         */
        public void onDdsSwitchRecommendation(int recommendedSlotId) {
            Log.d(LOG_TAG, "onDdsSwitchRecommendation: recommendedSlotId = " + recommendedSlotId);
            mCallback.onDdsSwitchRecommendation(UNSOL, recommendedSlotId);
        }
    }

    /**
     * Class that implements the binder death recipient to be notified when IQtiRadioConfig service
     * dies.
     */
    final class QtiRadioConfigDeathRecipient implements IBinder.DeathRecipient {
        /**
         * Callback that gets called when the service dies
         */
        @Override
        public void binderDied() {
            Log.e(LOG_TAG, "IQtiRadioConfig died");
            resetQtiRadioConfigHalInterfaces();
            initQtiRadioConfig();
        }
    }

    private IQtiRadioConfig getQtiRadioConfig() throws RemoteException {
        synchronized (mHalSync) {
            if (mQtiRadioConfig != null) {
                return mQtiRadioConfig;
            } else {
                throw new RemoteException("mQtiRadioConfig is null");
            }
        }
    }

    private void resetQtiRadioConfigHalInterfaces() {
        Log.d(LOG_TAG, "resetQtiRadioConfigHalInterfaces: Resetting HAL interfaces.");
        if (mQtiRadioConfigBinder != null) {
            try {
                boolean result = mQtiRadioConfigBinder.unlinkToDeath(
                        mQtiRadioConfigDeathRecipient, 0 /* Not used */);
                mQtiRadioConfigBinder = null;
            } catch (Exception ex) {
                Log.e(LOG_TAG, "Failed to unlink IQtiRadioConfig death recipient", ex);
            }
        }
        synchronized (mHalSync) {
            mQtiRadioConfig = null;
            mQtiRadioConfigResponse = null;
            mQtiRadioConfigIndication = null;
        }
    }

    @Override
    public void registerCallback(IQtiRadioConfigConnectionCallback callback) {
        Log.d(LOG_TAG, "registerCallback: callback = " + callback);
        mCallback = callback;
    }

    @Override
    public void unregisterCallback(IQtiRadioConfigConnectionCallback callback) {
        Log.d(LOG_TAG, "unregisterCallback: callback = " + callback);
        if (mCallback == callback) {
            mCallback = null;
        }
    }

    private boolean isVendorApiLevelAtleastV() {
        int boardApiLevel = SystemProperties.getInt("ro.board.api_level", 0);
        int vendorApiLevel = SystemProperties.getInt("ro.vendor.api_level", 0);
        Log.d(LOG_TAG, "isVendorApiLevelAtleastV: bApiLevel = " +  boardApiLevel
                + ", vApiLevel = " + vendorApiLevel);
        return boardApiLevel >= Build.VENDOR_API_2024_Q2
                || vendorApiLevel >= Build.VENDOR_API_2024_Q2;
    }

    private boolean isBoardApiLevelAtMostV() {
        final int boardApiLevel = SystemProperties.getInt("ro.board.api_level", 0);
        return boardApiLevel <= Build.VENDOR_API_2024_Q2;
    }

    @Override
    public boolean isFeatureSupported(int feature) {
        boolean ret = false;
        if (feature == ExtTelephonyManager.FEATURE_SMART_TEMP_DDS_VIA_RADIO_CONFIG) {
            ret = getHalVersion() >= 5 && isVendorApiLevelAtleastV();
        } else if (feature == ExtTelephonyManager.FEATURE_TDSCDMA_SUPPORT) {
            ret = isBoardApiLevelAtMostV();
        } else {
            try {
                ret = getQtiRadioConfig().isFeatureSupported(toAidlFeature(feature));
            } catch (RemoteException ex) {
                Log.e(LOG_TAG, "isFeatureSupported failed.", ex);
            }
        }
        Log.d(LOG_TAG, "isFeatureSupported(" + feature + ") = " + ret);
        return ret;
    }

    private int toAidlFeature(int feature) {
       switch (feature) {
           case FEATURE_EMERGENCY_ENHANCEMENT:
               return EMERGENCY_ENHANCEMENT;
       }
       return INVALID_FEATURE;
    }
}
