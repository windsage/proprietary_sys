/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qti.phone;

import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.telephony.CellInfo;
import android.telephony.ImsiEncryptionInfo;
import android.telephony.NetworkScanRequest;
import android.telephony.PhoneNumberUtils;
import android.telephony.RadioAccessSpecifier;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.NetworkScanResult;
import static com.android.internal.telephony.RILConstants.REQUEST_NOT_SUPPORTED;

import com.qti.extphone.CellularRoamingPreference;
import com.qti.extphone.CiwlanConfig;
import com.qti.extphone.ExtTelephonyManager;
import com.qti.extphone.NetworkSelectionMode;
import com.qti.extphone.NrConfig;
import com.qti.extphone.NrIcon;
import com.qti.extphone.NrIconType;
import com.qti.extphone.QtiPersoUnlockStatus;
import com.qti.extphone.QtiCallForwardInfo;
import com.qti.extphone.QtiImeiInfo;
import com.qti.extphone.QtiSetNetworkSelectionMode;
import com.qti.extphone.Status;
import com.qti.extphone.Token;
import com.qti.phone.powerupoptimization.PowerUpOptimization;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import vendor.qti.hardware.radio.qtiradio.CallForwardInfo;
import vendor.qti.hardware.radio.qtiradio.FacilityLockInfo;
import vendor.qti.hardware.radio.qtiradio.ImeiInfo;
import vendor.qti.hardware.radio.qtiradio.IQtiRadio;
import vendor.qti.hardware.radio.qtiradio.IQtiRadioIndication;
import vendor.qti.hardware.radio.qtiradio.IQtiRadioResponse;
import vendor.qti.hardware.radio.qtiradio.NrUwbIconBandInfo;
import vendor.qti.hardware.radio.qtiradio.NrUwbIconBandwidthInfo;
import vendor.qti.hardware.radio.qtiradio.NrUwbIconRefreshTime;
import vendor.qti.hardware.radio.qtiradio.QtiNetworkScanRequest;
import vendor.qti.hardware.radio.qtiradio.SetNetworkSelectionMode;

import static org.codeaurora.telephony.utils.Log.pii;

public class QtiRadioAidl implements IQtiRadioConnectionInterface {
    private final String LOG_TAG = "QtiRadioAidl";
    private IQtiRadioConnectionCallback mCallback;
    private int mSlotId;
    private Context mContext;

    private final Token UNSOL = new Token(-1);
    private ConcurrentHashMap<Integer, Token> mInflightRequests = new ConcurrentHashMap<Integer,
            Token>();

    // Synchronization object of HAL interfaces.
    private final Object mHalSync = new Object();
    private IBinder mBinder;
    // The death recepient object which gets notified when IQtiRadio service dies.
    private QtiRadioDeathRecipient mDeathRecipient;
    private final String IQTI_RADIO_STABLE_AIDL_SERVICE_INSTANCE = "slot";
    private String mServiceInstance;

    private IQtiRadio mQtiRadio;
    private IQtiRadioResponse mQtiRadioResponseAidl;
    private IQtiRadioIndication mQtiRadioIndicationAidl;

    private int mCurrentVersion = -1;
    private final int VERSION_ONE = 1;
    private final int VERSION_TEN = 10;
    private final int VERSION_ELEVEN = 11;
    private final int VERSION_TWELVE = 12;
    private final int VERSION_THIRTEEN = 13;

    // List of features for isFeatureSupported API
    private static final int INVALID_FEATURE = -1;
    private static final int BACK_BACK_SS_REQ_FEATURE = 1;
    private static final int PERSO_UNLOCK_TEMP_FEATURE = 2;
    private static final int CIWLAN_CONFIG_FEATURE = 3;
    private static final int CELLULAR_ROAMING_FEATURE = 4;
    private static final int CIWLAN_USER_PREFERENCE_FEATURE = 5;
    private static final int NR_ICON_6RX_FEATURE = 6;

    //Maximum time in millisecs for telephony should wait to deactivate data call
    //when user turns off mobile data or data roaming during CIWLAN.
    // This is configurable from resource overlays.
    private long mMaxDataDeactivateDelayTime = 7000;

    private static Optional<Boolean> mIsPersoUnlockTempFeatureEnabled = Optional.empty();
    private static final String PROPERTY_PERSO_UNLOCK_TEMP_FEATURE =
            "persist.vendor.radio.temp_unlock_feature";

    private boolean mQueryNrIconCalledWithout6RxSupport = false;

    public QtiRadioAidl(int slotId, Context context) {
        mContext = context;
        mSlotId = slotId;
        mServiceInstance = IQTI_RADIO_STABLE_AIDL_SERVICE_INSTANCE + (mSlotId + 1);
        mDeathRecipient = new QtiRadioDeathRecipient();
        mMaxDataDeactivateDelayTime = QtiRadioUtils.getMaxDataDeactivateDelayTime(context);
    }

    public void initQtiRadio() {
        Log.i(LOG_TAG,"initQtiRadio mSlotId: " + mSlotId);
        synchronized (mHalSync) {
            if (!SubscriptionManager.isValidSlotIndex(mSlotId)) {
                return;
            }
            mBinder = Binder.allowBlocking(
                    ServiceManager.waitForDeclaredService(
                            "vendor.qti.hardware.radio.qtiradio.IQtiRadioStable/" +
                            mServiceInstance));
            if (mBinder == null) {
                Log.e(LOG_TAG, "initQtiRadio failed");
                return;
            }

            vendor.qti.hardware.radio.qtiradio.IQtiRadio qtiRadio =
                    vendor.qti.hardware.radio.qtiradio.IQtiRadio.Stub.asInterface(mBinder);
            if(qtiRadio == null) {
                Log.e(LOG_TAG,"Get binder for QtiRadio StableAIDL failed");
                return;
            }
            Log.i(LOG_TAG,"Get binder for QtiRadio StableAIDL is successful");

            try {
                mBinder.linkToDeath(mDeathRecipient, 0 /* Not Used */);
            } catch (android.os.RemoteException ex) {
            }

            mQtiRadioResponseAidl = new QtiRadioResponseAidl();
            mQtiRadioIndicationAidl = new QtiRadioIndicationAidl();
            try {
                qtiRadio.setCallbacks(mQtiRadioResponseAidl, mQtiRadioIndicationAidl);
            } catch (android.os.RemoteException ex) {
                Log.e(LOG_TAG, "Failed to call setCallbacks stable AIDL API" + ex);
            }
            mQtiRadio = qtiRadio;

            try {
                mCurrentVersion = mQtiRadio.getInterfaceVersion();
            } catch (android.os.RemoteException ex) {
                Log.e(LOG_TAG, "Exception for getInterfaceVersion()" + ex);
            }
        }
    }

    /**
     * Class that implements the binder death recipient to be notified when
     * IImsRadio service dies.
     */
    final class QtiRadioDeathRecipient implements IBinder.DeathRecipient {
        /**
         * Callback that gets called when the service has died
         */
        @Override
        public void binderDied() {
            Log.e(LOG_TAG, "IQtiRadio Died mSlotId : " + mSlotId);
            resetHalInterfaces();
            initQtiRadio();
            PowerUpOptimization powerUpOptimization = PowerUpOptimization.getInstance();
            if (powerUpOptimization != null) {
                powerUpOptimization.handleRadioDied(mSlotId);
            }
        }
    }

    private void resetHalInterfaces() {
        Log.d(LOG_TAG, "resetHalInterfaces: Resetting HAL interfaces.");
        if (mBinder != null) {
            try {
                boolean result = mBinder.unlinkToDeath(mDeathRecipient, 0 /* Not used */);
                mBinder = null;
            } catch (Exception ex) {}
        }
        synchronized (mHalSync) {
            mQtiRadio = null;
            mQtiRadioResponseAidl = null;
            mQtiRadioIndicationAidl = null;
        }
    }

    private IQtiRadio getQtiRadio() throws RemoteException {
        synchronized (mHalSync) {
            if (mQtiRadio != null) {
                return mQtiRadio;
            } else {
                throw new RemoteException("mQtiRadio is null");
            }
        }
    }

    private Status convertHalErrorcode(int rilErrorCode) {
        return new Status((rilErrorCode == 0) ? Status.SUCCESS : Status.FAILURE);
    }

    private NrConfig convertHalNrConfig(int nrConfig) {
        return new NrConfig(nrConfig);
    }

    private QtiCallForwardInfo[] convertHidlCallForwardInfo2Aidl(
            CallForwardInfo[] callForwardInfos) {
        if (callForwardInfos == null) return null;
        int size = callForwardInfos.length;
        QtiCallForwardInfo[] ret = new QtiCallForwardInfo[size];
        for (int i = 0; i < size; i++) {
            ret[i] = new QtiCallForwardInfo();
            CallForwardInfo cfInfo = callForwardInfos[i];
            ret[i].status = cfInfo.status;
            ret[i].reason = cfInfo.reason;
            ret[i].serviceClass = cfInfo.serviceClass;
            ret[i].toa = cfInfo.toa;
            ret[i].number = cfInfo.number;
            ret[i].timeSeconds = cfInfo.timeSeconds;
        }
        return ret;
    }

    private QtiImeiInfo convertHidlImeiInfo2Aidl(ImeiInfo imeiInfo) {
        return new QtiImeiInfo(mSlotId, imeiInfo.imei, imeiInfo.type);
    }

    class QtiRadioResponseAidl extends vendor.qti.hardware.radio.qtiradio.
            IQtiRadioResponse.Stub {
        @Override
        public final int getInterfaceVersion() {
            return vendor.qti.hardware.radio.qtiradio.IQtiRadioResponse.VERSION;
        }

        @Override
        public String getInterfaceHash() {
            return vendor.qti.hardware.radio.qtiradio.IQtiRadioResponse.HASH;
        }

        /**
         * Response to IQtiRadio.queryNrIconType
         *
         * @param serial - Number to match the request with the response. The response must include
         *                 the same serial number as the request.
         * @param errorCode - Error code as per types.hal returned from RIL
         * @param iconType - NR icon type as per NrIconType.aidl to indicate the 5G icon shown on
         *                   the UI. One of NONE (Non-5G), 5G BASIC, 5G UWB, or 5G++.
         */
        @Override
        public void onNrIconTypeResponse(int serial, int errorCode, int iconType) {
            Log.d(LOG_TAG, "onNrIconTypeResponse: serial = " + serial + ", slotId = " + mSlotId
                    + ", errorCode = " + errorCode + " iconType = " + iconType);
            if (mInflightRequests.containsKey(serial)) {
                Token token = mInflightRequests.get(serial);
                if (mQueryNrIconCalledWithout6RxSupport) {
                    mCallback.onNrIconResponse(mSlotId, token, convertHalErrorcode(errorCode),
                            new NrIcon(iconType, -1));
                    mQueryNrIconCalledWithout6RxSupport = false;
                } else {
                    mCallback.onNrIconType(mSlotId, token, convertHalErrorcode(errorCode),
                            new NrIconType(iconType));
                }
                mInflightRequests.remove(serial);
            } else {
                Log.d(LOG_TAG, "onNrIconTypeResponse: No previous request found for serial = "
                        + serial);
            }
        }

        /**
         * Response to IQtiRadio.enableEndc
         *^M
         * @param serial to match request/response. Response must inclue same serial as request.
         * @param errorCode - errorCode as per types.hal returned from RIL.
         * @param status SUCCESS/FAILURE of the request.
         */
        @Override
        public void onEnableEndcResponse(int serial, int errorCode, int status)
        {
            Log.d(LOG_TAG, "onEnableEndcResponse:slotId ="+ mSlotId + "serial = " + serial +
                    " errorCode = " + errorCode + " " + "status = " + status);
            if (mInflightRequests.containsKey(serial)) {
                Token token = mInflightRequests.get(serial);
                mCallback.onEnableEndc(mSlotId, token, convertHalErrorcode(errorCode));
                mInflightRequests.remove(serial);
            } else {
                Log.d(LOG_TAG, "onEnableEndcResponse: No previous request found for serial = " +
                        serial);
            }
        }
        /**
         * Response to IQtiRadio.queryEndcStatus
         *
         * @param serial to match request/response. Response must inclue same serial as request.
         * @param errorCode - errorCode as per types.hal returned from RIL.
         * @param endcStatus values as per types.hal to indicate ENDC is enabled/disabled.
         */
        @Override
        public void onEndcStatusResponse(int serial, int errorCode, int endcStatus)
        {
            Log.d(LOG_TAG, "onEndcStatusResponse:slotId ="+ mSlotId + "serial = " + serial +
                    " errorCode = " + errorCode + " " + "enabled = " + endcStatus);
            if (mInflightRequests.containsKey(serial)) {
                Token token = mInflightRequests.get(serial);

                boolean isEnabled = (endcStatus == vendor.qti.hardware.radio.qtiradio
                        .EndcStatus.ENABLED);
                mCallback.onEndcStatus(mSlotId, token, convertHalErrorcode(errorCode), isEnabled);
                mInflightRequests.remove(serial);
            } else {
                Log.d(LOG_TAG, "onEndcStatusResponse: No previous request found for serial = " +
                        serial);
            }
        }
        /**
         * Response to IQtiRadio.SetNrConfig
         *
         * @param serial to match request/response. Response must inclue same serial as request.
         * @param errorCode - errorCode as per types.hal returned from RIL.
         * @param status SUCCESS/FAILURE of the request.
         */
        @Override
        public void setNrConfigResponse(int serial, int errorCode, int status)
        {
            Log.d(LOG_TAG,"setNrConfigResponse:slotId ="+ mSlotId + " serial = " + serial +
                    " errorCode = " + errorCode + " status = " + status);
            if (mInflightRequests.containsKey(serial)) {
                Token token = mInflightRequests.get(serial);
                mCallback.onSetNrConfig(mSlotId, token, convertHalErrorcode(errorCode));
                mInflightRequests.remove(serial);
            } else {
                Log.d(LOG_TAG, "setNrConfigResponse: No previous request found for serial = " +
                        serial);
            }
        }
        /**
         * Response to IQtiRadio.queryNrConfig
         *
         * @param serial to match request/response. Response must inclue same serial as request.
         * @param errorCode - errorCode as per types.hal returned from RIL.
         * @param enabled values as per NrConfig.aidl to indicate status of NrConfig.
         */
        @Override
        public void onNrConfigResponse(int serial, int errorCode, int nrConfig)
        {
            Log.d(LOG_TAG, "onNrConfigResponse:slotId ="+ mSlotId + "serial = " + serial +
                    " errorCode = " + errorCode + " nrConfig = " + nrConfig);
            if (mInflightRequests.containsKey(serial)) {
                Token token = mInflightRequests.get(serial);
                NrConfig config = convertHalNrConfig(nrConfig);
                mCallback.onNrConfigStatus(mSlotId, token, convertHalErrorcode(errorCode), config);
                mInflightRequests.remove(serial);
            } else {
                Log.d(LOG_TAG, "onNrConfigResponse: No previous request found for serial = " +
                        serial);
            }
        }

        /**
         * Response to IQtiRadio.getQtiRadioCapability
         *
         * @param serial to match request/response. Response must inclue same serial as request.
         * @param errorCode - errorCode as per types.hal returned from RIL.
         * @param enabled values as per NrConfig.aidl to indicate status of NrConfig.
         */
        @Override
        public void getQtiRadioCapabilityResponse(int serial, int errorCode, int radioAccessFamily)
        {
            Log.d(LOG_TAG, "getQtiRadioCapabilityResponse:slotId ="+ mSlotId + "serial = " +
                    serial + " errorCode = " + errorCode + " raf = " + radioAccessFamily);
            if (mInflightRequests.containsKey(serial)) {
                Token token = mInflightRequests.get(serial);
                mCallback.getQtiRadioCapabilityResponse(mSlotId, token,
                        convertHalErrorcode(errorCode),
                        QtiRadioUtils.convertToQtiNetworkTypeBitMask(radioAccessFamily));
                mInflightRequests.remove(serial);
            } else {
                Log.d(LOG_TAG, "getQtiRadioCapabilityResponse: No previous request" +
                        "found for serial = " + serial);
            }
        }

        /**
        * Response to IQtiRadio.getCallForwardStatus
        * @param serial to match request/response. Response must include same serial as request.
        * @param errorCode - errorCode as per types.hal returned from RIL.
        * @param callInfoForwardInfoList list of call forward status information for different
        * service classes.
        */
        @Override
        public void getCallForwardStatusResponse(int serial, int errorCode, CallForwardInfo[]
                callForwardInfoList) {
            Log.d(LOG_TAG, "getCallForwardStatusResponse:slotId ="+ mSlotId + "serial = " +
                    serial + " errorCode = " + errorCode);
            if (mInflightRequests.containsKey(serial)) {
                Token token = mInflightRequests.get(serial);
                mCallback.getCallForwardStatusResponse(mSlotId, token,
                        convertHalErrorcode(errorCode),
                        convertHidlCallForwardInfo2Aidl(callForwardInfoList));
                mInflightRequests.remove(serial);
            } else {
                Log.d(LOG_TAG, "getCallForwardStatusResponse: No previous request" +
                        "found for serial = " + serial);
            }
        }

        /**
        * Response to IQtiRadio.getFacilityLockForApp
        * @param serial to match request/response. Response must include same serial as request.
        * @param errorCode - errorCode as per types.hal returned from RIL.
        * @param response 0 is the TS 27.007 service class bit vector of services for which the
        *        specified barring facility is active. "0" means "disabled for all"
        */
        @Override
        public void getFacilityLockForAppResponse(int serial, int errorCode, int response) {
            Log.d(LOG_TAG, "getFacilityLockForAppResponse:slotId ="+ mSlotId + "serial = " +
                    serial + " errorCode = " + errorCode);
            if (mInflightRequests.containsKey(serial)) {
                Token token = mInflightRequests.get(serial);

                //Lock status enabled or disabled
                int[] ret = new int[1];
                ret[0] = response;

                mCallback.getFacilityLockForAppResponse(mSlotId, token,
                        convertHalErrorcode(errorCode), ret);
                mInflightRequests.remove(serial);
            } else {
                Log.d(LOG_TAG, "getFacilityLockForAppResponse: No previous request" +
                        "found for serial = " + serial);
            }
        }

        /**
        * Response to IQtiRadio.getImei
        * @param serial to match request/response. Response must include same serial as request.
        * @param errorCode - errorCode as per types.hal returned from RIL.
        * @param imeiInfo - provides current slot IMEI, its type as Primary, Secondary or Invalid
        */
        @Override
        public void getImeiResponse(int serial, int errorCode, ImeiInfo imeiInfo) {
            Log.d(LOG_TAG, "getImeiResponse: slotId ="+ mSlotId + "serial = " +
                    serial + " errorCode = " + errorCode + " imeitype " + imeiInfo.type);
            if (mInflightRequests.containsKey(serial)) {
                Token token = mInflightRequests.get(serial);

                mCallback.getImeiResponse(mSlotId, token,
                        convertHalErrorcode(errorCode), convertHidlImeiInfo2Aidl(imeiInfo));
                mInflightRequests.remove(serial);
            } else {
                Log.d(LOG_TAG, "getImeiResponse: No previous request" +
                        "found for serial = " + serial);
            }
        }

        /* Response to IQtiRadio.getDdsSwitchCapability
         *
         * @param serial to match request/response. Response must include same serial as request.
         * @param errorCode - errorCode as per RIL_Errno part of
         *                  hardware/ril/include/telephony/ril.h.
         * @param support true/false if smart dds switch capability is supported or not.
         */
        public void getDdsSwitchCapabilityResponse(int serial,
                int errorCode, boolean support) {
            Log.d(LOG_TAG, "getDdsSwitchCapabilityResponse:slotId = "+ mSlotId +
                    " serial = " + serial + " errorCode = " + errorCode + " support = " + support);
            if (mInflightRequests.containsKey(serial)) {
                Token token = mInflightRequests.get(serial);
                mCallback.onDdsSwitchCapabilityChange(mSlotId, token,
                        convertHalErrorcode(errorCode), support);
                mInflightRequests.remove(serial);
            } else {
                Log.d(LOG_TAG, "getDdsSwitchCapabilityResponse:" +
                        "No previous request found for serial = " + serial);
            }
        }

        /**
         * Response to IQtiRadio.sendUserPreferenceForDataDuringVoiceCall
         *
         * @param serial to match request/response. Response must include same serial as request.
         * @param errorCode - errorCode as per RIL_Errno part of
         *                  hardware/ril/include/telephony/ril.h.
         * @param status SUCCESS/FAILURE of the request.
         */
        public void sendUserPreferenceForDataDuringVoiceCallResponse(int serial,
                int errorCode) {
            Log.d(LOG_TAG, "sendUserPreferenceForDataDuringVoiceCallResponse:slotId = "
                    + mSlotId + " serial = " + serial + " errorCode = " + errorCode);
            if (mInflightRequests.containsKey(serial)) {
                Token token = mInflightRequests.get(serial);
                mCallback.onSendUserPreferenceForDataDuringVoiceCall(mSlotId, token,
                        convertHalErrorcode(errorCode));
                mInflightRequests.remove(serial);
            } else {
                Log.d(LOG_TAG, "sendUserPreferenceForDataDuringVoiceCallResponse:" +
                        "No previous request found for serial = " + serial);
            }
        }

        /**
         * Response to IQtiRadio.setNrUltraWidebandIconConfig
         *
         * @param serial - Serial number to match the request with the response
         * @param errorCode - Error code as per RIL_Errno part of
         *                  hardware/ril/include/telephony/ril.h
         */
        public void setNrUltraWidebandIconConfigResponse(int serial, int errorCode) {
            Log.d(LOG_TAG, "setNrUltraWidebandIconConfigResponse: slotId = " + mSlotId +
                    " serial = " + serial + " errorCode = " + errorCode);
            if (mInflightRequests.containsKey(serial)) {
                Token token = mInflightRequests.get(serial);
                mCallback.onSetNrUltraWidebandIconConfigResponse(mSlotId, token,
                        convertHalErrorcode(errorCode));
                mInflightRequests.remove(serial);
            } else {
                Log.d(LOG_TAG, "setNrUltraWidebandIconConfigResponse: No previous request found " +
                        "for serial = " + serial);
            }
        }

        /**
        * Response to IQtiRadio.startNetworkScan
        * @param serial to match request/response. Response must include same serial as request.
        * @param errorCode - Error code
        */
        @Override
        public void startNetworkScanResponse(int serial, int errorCode) {
            Log.d(LOG_TAG, "startNetworkScanResponse: slotId ="+ mSlotId + "serial = " +
                    serial);
            if (mInflightRequests.containsKey(serial)) {
                Token token = mInflightRequests.get(serial);

                mCallback.startNetworkScanResponse(mSlotId, token,
                        errorCode);
                mInflightRequests.remove(serial);
            } else {
                Log.d(LOG_TAG, "startNetworkScanResponse: No previous request" +
                        "found for serial = " + serial);
            }
        }

        /**
        * Response to IQtiRadio.stopNetworkScan
        * @param serial to match request/response. Response must include same serial as request.
        * @param errorCode - Error code
        */
        @Override
        public void stopNetworkScanResponse(int serial, int errorCode) {
            Log.d(LOG_TAG, "stopNetworkScanResponse: slotId ="+ mSlotId + "serial = " +
                    serial);
            if (mInflightRequests.containsKey(serial)) {
                Token token = mInflightRequests.get(serial);

                mCallback.stopNetworkScanResponse(mSlotId, token,
                        errorCode);
                mInflightRequests.remove(serial);
            } else {
                Log.d(LOG_TAG, "stopNetworkScanResponse: No previous request" +
                        "found for serial = " + serial);
            }
        }

        /**
        * Response to IQtiRadio.setNetworkSelectionModeManual
        * @param serial to match request/response. Response must include same serial as request.
        * @param errorCode - Error code
        */
        @Override
        public void setNetworkSelectionModeManualResponse(int serial, int errorCode) {
            Log.d(LOG_TAG, "setNetworkSelectionModeManualResponse: slotId ="+ mSlotId +
                    "serial = " + serial);
            if (mInflightRequests.containsKey(serial)) {
                Token token = mInflightRequests.get(serial);

                mCallback.setNetworkSelectionModeManualResponse(mSlotId, token,
                        errorCode);
                mInflightRequests.remove(serial);
            } else {
                Log.d(LOG_TAG, "setNetworkSelectionModeManualResponse: No previous request" +
                        "found for serial = " + serial);
            }
        }

        /**
        * Response to IQtiRadio.setNetworkSelectionModeAutomatic
        * @param serial to match request/response. Response must include same serial as request.
        * @param errorCode - Error code
        */
        @Override
        public void setNetworkSelectionModeAutomaticResponse(int serial, int errorCode) {
            Log.d(LOG_TAG, "setNetworkSelectionModeAutomaticResponse: slotId =" + mSlotId +
                    "serial = " + serial);
            if (mInflightRequests.containsKey(serial)) {
                Token token = mInflightRequests.get(serial);

                mCallback.setNetworkSelectionModeAutomaticResponse(mSlotId, token,
                        errorCode);
                mInflightRequests.remove(serial);
            } else {
                Log.d(LOG_TAG, "setNetworkSelectionModeAutomaticResponse: No previous request" +
                        "found for serial = " + serial);
            }
        }

        /**
        * Response to IQtiRadio.setNetworkSelectionModeAutomatic
        * @param serial to match request/response. Response must include same serial as request.
        * @param errorCode - Error code
        * @param mode - Network selection mode
        */
        @Override
        public void getNetworkSelectionModeResponse(int serial, int errorCode,
                vendor.qti.hardware.radio.qtiradio.NetworkSelectionMode mode) {
            Log.d(LOG_TAG, "getNetworkSelectionModeResponse: slotId =" + mSlotId +
                    "serial = " + serial);
            if (mInflightRequests.containsKey(serial)) {
                Token token = mInflightRequests.get(serial);

                NetworkSelectionMode nsm = new NetworkSelectionMode(mode.accessMode,
                        mode.isManual);

                mCallback.getNetworkSelectionModeResponse(mSlotId, token,
                        convertHalErrorcode(errorCode), nsm);
                mInflightRequests.remove(serial);
            } else {
                Log.d(LOG_TAG, "getNetworkSelectionModeResponse: No previous request" +
                        "found for serial = " + serial);
            }
        }

        /**
         * Response to IQtiRadio.setCiwlanModeUserPreference
         * @param serial ID to match request/response. Response must include same serial
         * as request.
         * @param errorCode Error code
        */
        @Override
        public void setCiwlanModeUserPreferenceResponse(int serial, int errorCode) {
            Log.d(LOG_TAG, "setCiwlanModeUserPreferenceResponse: slotId =" + mSlotId +
                    " serial = " + serial + " errorCode = " + errorCode);
            if (mInflightRequests.containsKey(serial)) {
                Token token = mInflightRequests.get(serial);
                mCallback.setCiwlanModeUserPreferenceResponse(mSlotId, token,
                        convertHalErrorcode(errorCode));
                mInflightRequests.remove(serial);
            } else {
                Log.d(LOG_TAG, "setCiwlanModeUserPreferenceResponse: No previous request" +
                        "found for serial = " + serial);
            }
        }

        /**
         * Response to IQtiRadio.setCellularRoamingPreference
         *
         * @param serial - To match request/response. Response must include same serial as request.
         * @param errorCode - Error code from modem
         */
        @Override
        public void setCellularRoamingPreferenceResponse(int serial, int errorCode) {
            Log.d(LOG_TAG, "setCellularRoamingPreferenceResponse: slotId = " + mSlotId +
                    ", serial = " + serial);
            if (mInflightRequests.containsKey(serial)) {
                Token token = mInflightRequests.get(serial);
                mCallback.setCellularRoamingPreferenceResponse(mSlotId, token,
                        convertHalErrorcode(errorCode));
                mInflightRequests.remove(serial);
            } else {
                Log.d(LOG_TAG, "setCellularRoamingPreferenceResponse: No previous request found " +
                        "for serial = " + serial);
            }
        }

        /**
         * Response to IQtiRadio.queryNrIcon
         *
         * @param serial - Number to match the request with the response. The response must include
         *                 the same serial number as the request.
         * @param errorCode - Error code as per types.hal returned from RIL
         * @param icon - NR icon type as per NrIconType.aidl and additional information such as the
         *               Rx count
         */
        @Override
        public void onNrIconResponse(int serial, int errorCode,
                vendor.qti.hardware.radio.qtiradio.NrIcon icon) {
            Log.d(LOG_TAG, "onNrIconResponse: serial = " + serial + ", slotId = " + mSlotId
                    + ", icon = " + icon);
            if (mInflightRequests.containsKey(serial)) {
                Token token = mInflightRequests.get(serial);
                mCallback.onNrIconResponse(mSlotId, token, convertHalErrorcode(errorCode),
                        new NrIcon(icon.type, icon.rxCount));
                mInflightRequests.remove(serial);
            } else {
                Log.d(LOG_TAG, "onNrIconResponse: No previous request found for serial = "
                        + serial);
            }
        }

        @Override
        public void sendAllEsimProfilesResponse(int serial, int errorCode) {
            Log.d(LOG_TAG, "sendAllEsimProfilesResponse: slotId = " + mSlotId +
                    ", serial = " + serial);
            if (mInflightRequests.containsKey(serial)) {
                Token token = mInflightRequests.get(serial);
                mCallback.onSendAllEsimProfilesResponse(token);
                mInflightRequests.remove(serial);
            } else {
                Log.d(LOG_TAG, "sendAllEsimProfilesResponse:  No previous request" +
                        "found for serial = " + serial);
            }
        }

        @Override
        public void notifyEnableProfileStatusResponse(int serial, int errorCode) {
            Log.d(LOG_TAG, "notifyEnableProfileStatusResponse: slotId = " + mSlotId +
                    ", serial = " + serial);
            if (mInflightRequests.containsKey(serial)) {
                Token token = mInflightRequests.get(serial);
                mCallback.onNotifyEnableProfileStatusResponse(token);
                mInflightRequests.remove(serial);
            } else {
                Log.d(LOG_TAG, "onNotifyEnableProfileStatusResponse:  No previous request" +
                        "found for serial = " + serial);
            }
        }

        @Override
        public void notifyDisableProfileStatusResponse(int serial, int errorCode) {
            Log.d(LOG_TAG, "notifyDisableProfileStatusResponse: slotId = " + mSlotId +
                    ", serial = " + serial);
            if (mInflightRequests.containsKey(serial)) {
                Token token = mInflightRequests.get(serial);
                mCallback.onNotifyDisableProfileStatusResponse(token);
                mInflightRequests.remove(serial);
            } else {
                Log.d(LOG_TAG, "onNotifyDisableProfileStatusResponse:  No previous request" +
                        "found for serial = " + serial);
            }
        }
    }

    class QtiRadioIndicationAidl extends vendor.qti.hardware.radio.qtiradio.
            IQtiRadioIndication.Stub {
        @Override
        public final int getInterfaceVersion() {
            return vendor.qti.hardware.radio.qtiradio.IQtiRadioIndication.VERSION;
        }

        @Override
        public String getInterfaceHash() {
            return vendor.qti.hardware.radio.qtiradio.IQtiRadioIndication.HASH;
        }

        /*
         * Unsol msg to indicate incremental network scan results.
         *
         * @param result the result of the network scan
         *
         */
        @Override
        public void networkScanResult(
                vendor.qti.hardware.radio.qtiradio.QtiNetworkScanResult result) {
            Log.d(LOG_TAG, "networkScanResult: slotId = " + mSlotId + "NetworkScanResult = " +
                    result);
            ArrayList<CellInfo> cellInfos = QtiRadioUtils.convertHalCellInfoList(
                    result.networkInfos);
            NetworkScanResult nsr = new NetworkScanResult(result.status,
                    result.error, cellInfos);
            mCallback.networkScanResult(mSlotId, UNSOL, nsr);
        }

        /**
         * Unsol msg to indicate changes to 5G icon type
         *
         * @param iconType - NR icon type as per NrIconType.aidl to indicate the 5G icon shown on
         *                   the UI. One of NONE (Non-5G), 5G BASIC, 5G UWB, or 5G++.
         */
        @Override
        public void onNrIconTypeChange(int iconType) {
            Log.d(LOG_TAG, "onNrIconTypeChange: slotId = " + mSlotId + ", type = " + iconType);
            mCallback.onNrIconType(mSlotId, UNSOL, new Status(Status.SUCCESS),
                    new NrIconType(iconType));
        }

        /*
         * Unsol msg to indicate change in NR Config.
         *
         * @param NrConfig as per types.hal to indicate NSA/SA/NSA+SA.
         *
         */
        @Override
        public void onNrConfigChange(int config)
        {
        }

        /*
         * Unsol msg to indicate change in Primary IMEI mapping.
         *
         * @param imeiInfo, IMEI value and its Type, Primary/Secondary/Invalid.
         */
        @Override
        public void onImeiChange(ImeiInfo imeiInfo) {
            Log.d(LOG_TAG, "onImeiChange: slotId = " + mSlotId + "Imei = " +
                    pii(imeiInfo.imei) + " type: " + imeiInfo.type);
            QtiImeiInfo qtiImeiInfo = convertHidlImeiInfo2Aidl(imeiInfo);
            mCallback.onImeiChange(mSlotId, qtiImeiInfo);
        }

        /* Unsol msg to inform HLOS that smart DDS switch capability changed.
         * Upon receiving this unsol, HLOS has to inform modem if user has enabled
         * temp DDS switch from UI or not.
         *
         */
        public void onDdsSwitchCapabilityChange() {
            Log.d(LOG_TAG, "onDdsSwitchCapabilityChange: slotId = " + mSlotId);
            mCallback.onDdsSwitchCapabilityChange(mSlotId, UNSOL,
                    new Status(Status.SUCCESS), true);
        }

        /*
         * Unsol msg to indicate if telephony has to enable/disable its temp DDS switch logic
         * If telephony temp DDS switch is disabled, then telephony will wait for
         * modem recommendations in seperate indication to perform temp DDS switch.
         *
         * @param telephonyDdsSwitch true/false based on telephony temp DDS switch
         *          logic should be enabled/disabled.
         */
        public void onDdsSwitchCriteriaChange(boolean telephonyDdsSwitch) {
            Log.d(LOG_TAG, "onDdsSwitchCriteriaChange: slotId = " + mSlotId +
                    "telephonyDdsSwitch = " + telephonyDdsSwitch);
            mCallback.onDdsSwitchCriteriaChange (mSlotId, UNSOL, telephonyDdsSwitch);
        }

        /*
         * Unsol msg to indicate modem recommendation for temp DDS switch.
         *
         * @param recommendedSlotId slot ID to which DDS has to be switched.
         *
         */
        public void onDdsSwitchRecommendation(int recommendedSlotId) {
            Log.d(LOG_TAG, "onDdsSwitchRecommendation: slotId = " + mSlotId +
                    "recommendedSlotId = " + recommendedSlotId);
            mCallback.onDdsSwitchRecommendation(mSlotId, UNSOL, recommendedSlotId);
        }

        /**
         * Unsol msg indicates the delay time to deactivate default data pdn
         * when cellular IWLAN feature is ON.
         * @param - delayTimeMilliSecs delayTimeMilliSecs > 0 indicates one or more pdns
           are established over Cellular IWLAN and wait for delayTimeMilliSecs
           to deactivate default data pdn if required.
           delayTimeMilliSecs <= 0 indicates no pdns are established over Cellular IWLAN.
         */
        public void onDataDeactivateDelayTime(long delayTimeMilliSecs) {
            Log.d(LOG_TAG, "onDataDeactivateDelayTime: slotId = " + mSlotId +
                    ", delayTimeMilliSecs = " + delayTimeMilliSecs);
            if (delayTimeMilliSecs < 0) return;

            if (delayTimeMilliSecs > mMaxDataDeactivateDelayTime) {
                delayTimeMilliSecs = mMaxDataDeactivateDelayTime;
            }
            mCallback.onDataDeactivateDelayTime(mSlotId, UNSOL, delayTimeMilliSecs);
        }

        /**
         * Unsol msg indicates epdg over cellular data
         * (cellular IWLAN) feature is supported or not.
         *
         * @param - support indicates if the feature is supported or not.
         */
        public void onEpdgOverCellularDataSupported(boolean support) {
            Log.d(LOG_TAG, "onEpdgOverCellularDataSupported: slotId = " + mSlotId +
                    ", support = " + support);
            mCallback.onEpdgOverCellularDataSupported(mSlotId, UNSOL, support);
        }

        /*
         * Unsol msg to indicate Mcfg refresh state.
         *
         * @param refreshState, Mcfg refresh state.
         * @param subscriptionId, on which subscription refresh happend.
         */
         @Override
        public void onMcfgRefresh(int refreshState, int subscriptionId) {
            Log.d(LOG_TAG, "onMcfgRefresh: subscriptionId = " + subscriptionId +
                    " refreshState = " + refreshState);
            QtiMcfgRefreshInfo qtiRefreshInfo = new QtiMcfgRefreshInfo(subscriptionId,
                    refreshState);
            mCallback.onMcfgRefresh(UNSOL, qtiRefreshInfo);
        }

        /*
         * Unsol msg to indicate Perso Unlock Status Change.
         *
         * @param persoUnlockStatus, which can be temporary or permanent.
         * @param slotId, on which perso substate changed.
         */
        @Override
        public void onSimPersoUnlockStatusChange(int persoUnlockStatus) {
            Log.d(LOG_TAG, "onSimPersoUnlockStatusChange: slotId = " + mSlotId +
                    " status = " + persoUnlockStatus);
            QtiPersoUnlockStatus qtiPersoUnlockStatus = new QtiPersoUnlockStatus(persoUnlockStatus);
            mCallback.onSimPersoUnlockStatusChange(mSlotId, qtiPersoUnlockStatus);
        }

        /*
         * This indication is based on various conditions like internet PDN is established
         * on DDS over LTE/NR RATs, CIWLAN is supported in home/roaming etc..
         * This is different from existing API IQtiRadioAidl#onEpdgOverCellularDataSupported()
         * which indicates if modem supports the CIWLAN feature based on static
         * configuration in modem.
         *
         * @param ciwlanAvailable true indicates C_IWLAN RAT is available, false otherwise.
         */
        @Override
        public void onCiwlanAvailable(boolean ciwlanAvailable) {
            Log.d(LOG_TAG, "onCiwlanAvailable: slotId = " + mSlotId +
                    " ciwlanAvailable = " + ciwlanAvailable);
            mCallback.onCiwlanAvailable(mSlotId, ciwlanAvailable);
        }

        /*
         * Unsol msg to indicate the C_IWLAN mode(only vs preferred) for home and roaming.
         *
         * @param ciwlanConfig C_IWLAN configuration (only vs preferred) for home and roaming.
         */
        @Override
        public void onCiwlanConfigChange(
                vendor.qti.hardware.radio.qtiradio.CiwlanConfig config) {
            Log.d(LOG_TAG, "onCiwlanConfigChange: slotId = " + mSlotId +
                    " home = " + config.homeMode + " roaming = " + config.roamMode);
            CiwlanConfig ciwlanConfig = new CiwlanConfig(config.homeMode, config.roamMode);
            mCallback.onCiwlanConfigChange(mSlotId, ciwlanConfig);
        }

        /**
         * Unsol msg to indicate changes to the NR icon
         *
         * @param icon - NR icon type as per NrIconType.aidl and additional information such as the
         *               Rx count
         */
        @Override
        public void onNrIconChange(vendor.qti.hardware.radio.qtiradio.NrIcon icon) {
            Log.d(LOG_TAG, "onNrIconChange: slotId = " + mSlotId + ", icon = " + icon);
            mCallback.onNrIconChange(mSlotId, new NrIcon(icon.type, icon.rxCount));
        }

        /**
         * Indication from modem to telephony to fetch the ICCID of the available eSIM profiles.
         *
         * @param refNum - Reference Number received from the modem which needs to be sent in the
         *                 response
         */
        @Override
        public void onGetAllEsimProfilesReq(int refNum) {
            Log.d(LOG_TAG, "onGetAllEsimProfilesReq:  " + refNum);
            mCallback.onAllAvailableProfilesReq(mSlotId, refNum);
        }

        /**
         * Indication from modem to telephony to activate the given eSIM profile.
         *
         * @param refNum - Reference Number received from the modem which needs to be sent in the
         *                 response once activation is done.
         * @param iccId - IccId of the eSIM profile which needs to be activated.
         */
        @Override
        public void onEnableProfileReq(int refNum, String iccId) {
            Log.d(LOG_TAG, "onEnableProfileReq: " + iccId);
            mCallback.onEnableProfileReq(mSlotId, refNum, iccId);
        }

        /**
         * Indication from modem to telephony to deactivate the given eSIM profile.
         *
         * @param refNum - Reference Number received from the modem which needs to be sent in the
         *                 response once deactivation is done.
         * @param iccId - ICCID of the eSIM profile which needs to be deactivated.
         */
        @Override
        public void onDisableProfileReq(int refNum, String iccId) {
            Log.d(LOG_TAG, "onDisableProfileReq: " + iccId);
            mCallback.onDisableProfileReq(mSlotId, refNum, iccId);
        }
    }

    @java.lang.Override
    public int getPropertyValueInt(String property, int def) throws RemoteException {
        Log.d(LOG_TAG, "getPropertyValueInt: property = " + property + "default = " + def);
        try {
            return Integer.parseInt(getQtiRadio().getPropertyValue(property, String.valueOf(def)));
        } catch(android.os.RemoteException ex) {
            Log.e(LOG_TAG, "getPropertyValue Failed" + ex);
            return SystemProperties.getInt(property, def);
        }
    }

    @java.lang.Override
    public boolean getPropertyValueBool(String property, boolean def) throws RemoteException {
        Log.d(LOG_TAG, "getPropertyValueBool: property = " + property + "default = " + def);
        try {
            return Boolean.parseBoolean(getQtiRadio()
                    .getPropertyValue(property, String.valueOf(def)));
        } catch(android.os.RemoteException ex) {
            Log.e(LOG_TAG, "getPropertyValue Failed" + ex);
            return SystemProperties.getBoolean(property, def);
        }
    }

    @java.lang.Override
    public String getPropertyValueString(String property, String def) throws RemoteException {
        Log.d(LOG_TAG, "getPropertyValueString: property = " + property + "default = " + def);
        try {
            return getQtiRadio().getPropertyValue(property, def);
        } catch(android.os.RemoteException ex) {
            Log.e(LOG_TAG, "getPropertyValue Failed" + ex);
            return SystemProperties.get(property, def);
        }
    }

    @java.lang.Override
    public void enableEndc(boolean enable, Token token) throws RemoteException {
        int serial = token.get();
        Log.d(LOG_TAG, "enableEndc: serial = " + serial + "enable = " + enable);
        mInflightRequests.put(serial, token);
        try {
            getQtiRadio().enableEndc(serial, enable);
        } catch(android.os.RemoteException ex) {
            mInflightRequests.remove(serial, token);
            Log.e(LOG_TAG, "enableEndc Failed." + ex);
            throw ex;
        }
    }

    @java.lang.Override
    public void queryNrIconType(Token token) throws RemoteException {
        int serial = token.get();
        Log.d(LOG_TAG, "queryNrIconType: serial = " + serial);
        mInflightRequests.put(serial, token);
        try {
            getQtiRadio().queryNrIconType(serial);
        } catch(android.os.RemoteException ex) {
            mInflightRequests.remove(serial, token);
            Log.e(LOG_TAG, "queryNrIconType Failed." + ex);
            throw ex;
        }
    }

    @java.lang.Override
    public void queryEndcStatus(Token token) throws RemoteException {
        int serial = token.get();
        Log.d(LOG_TAG, "queryEndcStatus: serial = " + serial);
        mInflightRequests.put(serial, token);
        try {
            getQtiRadio().queryEndcStatus(serial);
        } catch(android.os.RemoteException ex) {
            mInflightRequests.remove(serial, token);
            Log.e(LOG_TAG, "queryEndcStatus Failed." + ex);
            throw ex;
        }
    }

    @java.lang.Override
    public void setNrConfig(NrConfig config, Token token) throws RemoteException {
        int serial = token.get();
        Log.d(LOG_TAG, "setNrConfig: serial = " + serial +
                "NrConfig= " + config.get());
        mInflightRequests.put(serial, token);
        try {
            getQtiRadio().setNrConfig(serial, config.get());
        } catch(android.os.RemoteException ex) {
            mInflightRequests.remove(serial, token);
            Log.e(LOG_TAG, "setNrConfig Failed." + ex);
            throw ex;
        }
    }

    @java.lang.Override
    public void setNetworkSelectionModeAutomatic(int accessType,
            Token token) throws RemoteException {
        int serial = token.get();
        Log.d(LOG_TAG, "setNetworkSelectionModeAutomatic: serial = " + serial +
                "accessType= " + accessType);
        mInflightRequests.put(serial, token);

        try {
            getQtiRadio().setNetworkSelectionModeAutomatic(serial, accessType);
        } catch(android.os.RemoteException ex) {
            mInflightRequests.remove(serial, token);
            Log.e(LOG_TAG, "setNetworkSelectionModeAutomatic Failed." + ex);
            throw ex;
        }
    }

    @java.lang.Override
    public void getNetworkSelectionMode(Token token) throws RemoteException {
        int serial = token.get();
        Log.d(LOG_TAG, "getNetworkSelectionMode: serial = " + serial);
        mInflightRequests.put(serial, token);

        try {
            getQtiRadio().getNetworkSelectionMode(serial);
        } catch(android.os.RemoteException ex) {
            mInflightRequests.remove(serial, token);
            Log.e(LOG_TAG, "getNetworkSelectionMode Failed." + ex);
            throw ex;
        }
    }

    @java.lang.Override
    public void queryNrConfig(Token token) throws RemoteException {
        int serial = token.get();
        Log.d(LOG_TAG, "queryNrConfig: serial = " + serial);
        mInflightRequests.put(serial, token);
        try {
            getQtiRadio().queryNrConfig(serial);
        } catch(android.os.RemoteException ex) {
            mInflightRequests.remove(serial, token);
            Log.e(LOG_TAG, "queryNrConfig Failed." + ex);
            throw ex;
        }
    }

    @java.lang.Override
    public void startNetworkScan(NetworkScanRequest networkScanRequest,
            Token token) throws RemoteException {
        int serial = token.get();
        Log.d(LOG_TAG, "startNetworkScan: serial = " + serial +
                "networkScanRequest= " + networkScanRequest);
        mInflightRequests.put(serial, token);

        QtiNetworkScanRequest qnsr = new QtiNetworkScanRequest();
        vendor.qti.hardware.radio.qtiradio.NetworkScanRequest nsr =
                new vendor.qti.hardware.radio.qtiradio.NetworkScanRequest();
        nsr.type = networkScanRequest.getScanType();
        nsr.interval = networkScanRequest.getSearchPeriodicity();

        ArrayList<vendor.qti.hardware.radio.qtiradio.RadioAccessSpecifier> specifiers =
                new ArrayList<>();
        for (RadioAccessSpecifier ras : networkScanRequest.getSpecifiers()) {
            vendor.qti.hardware.radio.qtiradio.RadioAccessSpecifier rasInHalFormat =
                    QtiRadioUtils.convertToHalRadioAccessSpecifierAidl(ras);
            if (rasInHalFormat == null) {
                mQtiRadioResponseAidl.startNetworkScanResponse(serial, REQUEST_NOT_SUPPORTED);
                return;
            }
            specifiers.add(rasInHalFormat);
        }
        nsr.specifiers = specifiers.stream().toArray(
                vendor.qti.hardware.radio.qtiradio.RadioAccessSpecifier[]::new);
        nsr.maxSearchTime = networkScanRequest.getMaxSearchTime();
        nsr.incrementalResults = networkScanRequest.getIncrementalResults();
        nsr.incrementalResultsPeriodicity =
                networkScanRequest.getIncrementalResultsPeriodicity();
        nsr.mccMncs = networkScanRequest.getPlmns().stream().toArray(String[]::new);
        qnsr.nsr = nsr;
        qnsr.accessMode = QtiRadioUtils.getAccessMode(networkScanRequest);
        qnsr.searchType = QtiRadioUtils.getSearchType(networkScanRequest);
        try {
            getQtiRadio().startNetworkScan(serial, qnsr);
        } catch(android.os.RemoteException ex) {
            mInflightRequests.remove(serial, token);
            Log.e(LOG_TAG, "startNetworkScan Failed." + ex);
            throw ex;
        }
    }

    @java.lang.Override
    public void stopNetworkScan(Token token) throws RemoteException {
        int serial = token.get();
        Log.d(LOG_TAG, "stopNetworkScan: serial = " + serial);
        mInflightRequests.put(serial, token);
        try {
            getQtiRadio().stopNetworkScan(serial);
        } catch(android.os.RemoteException ex) {
            mInflightRequests.remove(serial, token);
            Log.e(LOG_TAG, "stopNetworkScan Failed." + ex);
            throw ex;
        }
    }

    @java.lang.Override
    public void setNetworkSelectionModeManual(QtiSetNetworkSelectionMode mode,
            Token token) throws RemoteException {
        int serial = token.get();
        Log.d(LOG_TAG, "setNetworkSelectionModeManual: serial = " + serial +
                "mode= " + mode);
        mInflightRequests.put(serial, token);
        SetNetworkSelectionMode snsm = new SetNetworkSelectionMode();
        snsm.operatorNumeric = mode.getOperatorNumeric();
        snsm.ran = QtiRadioUtils.convertToHalAccessNetworkAidl(mode.getRan());
        snsm.accessMode = mode.getAccessMode();
        if (mode.getCagId() > -1) {
            vendor.qti.hardware.radio.qtiradio.CagInfo cagInfo =
                    new vendor.qti.hardware.radio.qtiradio.CagInfo();
            cagInfo.cagId = mode.getCagId();
            cagInfo.cagName = "";
            snsm.cagInfo = cagInfo;
        }
        if (mode.getNid() != null) {
            snsm.snpnNid = mode.getNid();
        } else {
            snsm.snpnNid = new byte[0];
        }
        try {
            getQtiRadio().setNetworkSelectionModeManual(serial, snsm);
        } catch(android.os.RemoteException ex) {
            mInflightRequests.remove(serial, token);
            Log.e(LOG_TAG, "startNetworkScan Failed." + ex);
            throw ex;
        }
    }

    @java.lang.Override
    public void getQtiRadioCapability(Token token) throws RemoteException {
        int serial = token.get();
        Log.d(LOG_TAG, "getQtiRadioCapability: serial = " + serial);
        mInflightRequests.put(serial, token);
        try {
            getQtiRadio().getQtiRadioCapability(serial);
        } catch(android.os.RemoteException ex) {
            mInflightRequests.remove(serial, token);
            Log.e(LOG_TAG, "getQtiRadioCapability Failed." + ex);
            throw ex;
        }
    }

    @Override
    public void setCarrierInfoForImsiEncryption(Token token,
            ImsiEncryptionInfo imsiEncryptionInfo) {
        Log.d(LOG_TAG, "Not Supported");
    }

    @java.lang.Override
    public void enable5g(Token token) throws RemoteException {
        Log.d(LOG_TAG, "Not Supported");
    }

    @java.lang.Override
    public void disable5g(Token token) throws RemoteException {
        Log.d(LOG_TAG, "Not Supported");
    }

    @java.lang.Override
    public void queryNrBearerAllocation(Token token) throws RemoteException {
        Log.d(LOG_TAG, "Not Supported");
    }

    @java.lang.Override
    public void enable5gOnly(Token token) throws RemoteException {
        Log.d(LOG_TAG, "Not Supported");
    }

    @java.lang.Override
    public void query5gStatus(Token token) throws RemoteException {
        Log.d(LOG_TAG, "Not Supported");
    }

    @java.lang.Override
    public void queryNrDcParam(Token token) throws RemoteException {
        Log.d(LOG_TAG, "Not Supported");
    }

    @java.lang.Override
    public void queryNrSignalStrength(Token token) throws RemoteException {
        Log.d(LOG_TAG, "Not Supported");
    }

    @java.lang.Override
    public void queryUpperLayerIndInfo(Token token) throws RemoteException {
        Log.d(LOG_TAG, "Not Supported");
    }

    @java.lang.Override
    public void query5gConfigInfo(Token token) throws RemoteException {
        Log.d(LOG_TAG, "Not Supported");
    }

    @Override
    public void sendCdmaSms(byte[] pdu, boolean expectMore, Token token) {
        Log.d(LOG_TAG, "Not Supported");
    }

    @Override
    public boolean isFeatureSupported(int feature) {
        switch (toAidlFeature(feature)) {
            case BACK_BACK_SS_REQ_FEATURE:
                if (mCurrentVersion > VERSION_ONE) {
                    Log.d(LOG_TAG, "BACK_BACK_SS_REQ_FEATURE supported");
                    return true;
                }
                break;
            case CIWLAN_CONFIG_FEATURE:
                if (mCurrentVersion >= VERSION_TEN) {
                    Log.d(LOG_TAG, "CIWLAN_CONFIG_FEATURE supported");
                    return true;
                }
                break;
            case PERSO_UNLOCK_TEMP_FEATURE:
                if (mCurrentVersion >= VERSION_ELEVEN) {
                    Log.d(LOG_TAG, "PERSO_UNLOCK_TEMP_FEATURE supported");
                    return true;
                }
                break;
            case CELLULAR_ROAMING_FEATURE:
                if (mCurrentVersion >= VERSION_TWELVE) {
                    Log.d(LOG_TAG, "CELLULAR_ROAMING_FEATURE supported");
                    return true;
                }
                break;
            case CIWLAN_USER_PREFERENCE_FEATURE:
                if (mCurrentVersion >= VERSION_TWELVE) {
                    Log.d(LOG_TAG, "CIWLAN_USER_PREFERENCE_FEATURE supported");
                    return true;
                }
                break;
            case NR_ICON_6RX_FEATURE:
                if (mCurrentVersion >= VERSION_THIRTEEN) {
                    Log.d(LOG_TAG, "NR_ICON_6RX_FEATURE supported");
                    return true;
                }
                break;
        }
        return false;
    }

    private int toAidlFeature(int feature) {
        switch (feature) {
            case ExtTelephonyManager.FEATURE_BACK_TO_BACK_SUPPLEMENTARY_SERVICE_REQ:
                return BACK_BACK_SS_REQ_FEATURE;
            case ExtTelephonyManager.FEATURE_PERSO_UNLOCK_TEMP:
                return PERSO_UNLOCK_TEMP_FEATURE;
            case ExtTelephonyManager.FEATURE_GET_CIWLAN_CONFIG:
                return CIWLAN_CONFIG_FEATURE;
            case ExtTelephonyManager.FEATURE_CELLULAR_ROAMING:
                return CELLULAR_ROAMING_FEATURE;
            case ExtTelephonyManager.FEATURE_CIWLAN_MODE_PREFERENCE:
                return CIWLAN_USER_PREFERENCE_FEATURE;
        }
        return INVALID_FEATURE;
    }

    @Override
    public void queryCallForwardStatus(Token token, int cfReason, int serviceClass,
            String number, boolean expectMore) throws RemoteException {
        int serial = token.get();
        Log.d(LOG_TAG, "queryCallForwardStatus: serial = " + serial);
        mInflightRequests.put(serial, token);

        CallForwardInfo cfInfo = new CallForwardInfo();
        cfInfo.reason = cfReason;
        cfInfo.serviceClass = serviceClass;
        cfInfo.toa = PhoneNumberUtils.toaFromString(number);
        cfInfo.number = convertNullToEmptyString(number);
        cfInfo.timeSeconds = 0;
        cfInfo.expectMore = expectMore;

        try {
            getQtiRadio().getCallForwardStatus(serial, cfInfo);
        } catch(android.os.RemoteException ex) {
            mInflightRequests.remove(serial, token);
            Log.e(LOG_TAG, "queryCallForwardStatus Failed." + ex);
            throw ex;
        }
    }

    @Override
    public void getFacilityLockForApp(Token token, String facility, String password,
                int serviceClass, String appId, boolean expectMore) throws RemoteException {
        int serial = token.get();
        Log.d(LOG_TAG, "getFacilityLockForApp: serial = " + serial);
        mInflightRequests.put(serial, token);

        FacilityLockInfo facLockInfo = new FacilityLockInfo();
        facLockInfo.facility = convertNullToEmptyString(facility);
        facLockInfo.password = convertNullToEmptyString(password);
        facLockInfo.serviceClass = serviceClass;
        facLockInfo.appId = convertNullToEmptyString(appId);
        facLockInfo.expectMore = expectMore;

        try {
            getQtiRadio().getFacilityLockForApp(serial, facLockInfo);
        } catch(android.os.RemoteException ex) {
            mInflightRequests.remove(serial, token);
            Log.e(LOG_TAG, "getFacilityLockForApp Failed." + ex);
            throw ex;
        }
    }

    @Override
    public void getImei(Token token) throws RemoteException {
        int serial = token.get();
        Log.d(LOG_TAG, "getImei: serial = " + serial);
        mInflightRequests.put(serial, token);
        try {
            getQtiRadio().getImei(serial);
        } catch (android.os.RemoteException ex) {
            mInflightRequests.remove(serial, token);
            Log.e(LOG_TAG, "getImei Failed." + ex);
            throw ex;
        }
    }

    @Override
    public void getDdsSwitchCapability(Token token) {
        int serial = token.get();
        Log.d(LOG_TAG, "getDdsSwitchCapability: serial = " + serial);
        mInflightRequests.put(serial, token);

        try {
            getQtiRadio().getDdsSwitchCapability(serial);
        } catch(android.os.RemoteException ex) {
            mInflightRequests.remove(serial, token);
            Log.e(LOG_TAG, "getDdsSwitchCapability Failed." + ex);
        }
    }

    @Override
    public void getQosParameters(Token token, int cid) {
        Log.e(LOG_TAG, "getQosParameters not supported in AIDL");
    }

    @Override
    public void sendUserPreferenceForDataDuringVoiceCall(Token token,
            boolean userPreference) {
        int serial = token.get();
        Log.d(LOG_TAG, "sendUserPreferenceForDataDuringVoiceCall: serial = " + serial +
                " slotId = "+ mSlotId + " userPreference: " + userPreference);
        mInflightRequests.put(serial, token);

        try {
            getQtiRadio().sendUserPreferenceForDataDuringVoiceCall(serial, userPreference);
        } catch(android.os.RemoteException ex) {
            mInflightRequests.remove(serial, token);
            Log.e(LOG_TAG, "sendUserPreferenceForDataDuringVoiceCall Failed." + ex);
        }
    }

    @Override
    public boolean isEpdgOverCellularDataSupported() throws RemoteException {
        Log.d(LOG_TAG, "isEpdgOverCellularDataSupported()");
        try {
            return getQtiRadio().isEpdgOverCellularDataSupported();
        } catch(RemoteException ex) {
            Log.e(LOG_TAG, "isEpdgOverCellularDataSupported Failed" + ex);
            return false;
        }
    }

    @Override
    public void setNrUltraWidebandIconConfig(Token token, int sib2Value,
            NrUwbIconBandInfo saBandInfo, NrUwbIconBandInfo nsaBandInfo,
            ArrayList<NrUwbIconRefreshTime> refreshTimes,
            NrUwbIconBandwidthInfo bandwidthInfo) throws RemoteException {
        final int serial = token.get();
        Log.d(LOG_TAG, "setNrUltraWidebandIconConfig: serial = " + serial);
        mInflightRequests.put(serial, token);
        try {
            mQtiRadio.setNrUltraWidebandIconConfig(serial, sib2Value, saBandInfo,
                    nsaBandInfo, refreshTimes, bandwidthInfo);
        } catch (RemoteException ex) {
            Log.e(LOG_TAG, "setNrUltraWidebandIconConfig failed", ex);
            mInflightRequests.remove(serial);
            throw ex;
        }
    }

    @Override
    public CiwlanConfig getCiwlanConfig() {
        if (!isFeatureSupported(CIWLAN_CONFIG_FEATURE)) {
            return new CiwlanConfig(CiwlanConfig.UNSUPPORTED, CiwlanConfig.UNSUPPORTED);
        }
        try {
            vendor.qti.hardware.radio.qtiradio.CiwlanConfig config =
                    getQtiRadio().getCiwlanConfig();
            CiwlanConfig ciwlanConfig =
                    (config == null) ? null : new CiwlanConfig(config.homeMode, config.roamMode);
            Log.d(LOG_TAG, "getCiwlanConfig: " + ciwlanConfig);
            return ciwlanConfig;
        } catch (RemoteException ex) {
            Log.e(LOG_TAG, "getCiwlanConfig failed.", ex);
            return null;
        }
    }

    private boolean isPersoUnlockTempFeatureEnabled() {
        if (!mIsPersoUnlockTempFeatureEnabled.isPresent()) {
            try {
                mIsPersoUnlockTempFeatureEnabled =
                        Optional.of(getPropertyValueBool(
                                PROPERTY_PERSO_UNLOCK_TEMP_FEATURE, false));
            } catch (Exception ex) {
                Log.e(LOG_TAG, "isPersoUnlockTempFeatureEnabled: , Exception: ", ex);
            }
        }
        return mIsPersoUnlockTempFeatureEnabled.get();
    }

    @Override
    public QtiPersoUnlockStatus getSimPersoUnlockStatus() {
        QtiPersoUnlockStatus qtiPersoUnlockStatus = null;

        if (!isPersoUnlockTempFeatureEnabled() || !isFeatureSupported(PERSO_UNLOCK_TEMP_FEATURE)) {
            Log.e(LOG_TAG, "Temporary perso substate unlock feature not enabled/supported");
            return null;
        }

        try {
            int persoUnlockStatus = getQtiRadio().getSimPersoUnlockStatus();
            qtiPersoUnlockStatus = new QtiPersoUnlockStatus(persoUnlockStatus);
            Log.d(LOG_TAG, "getSimPersoUnlockStatus: " + qtiPersoUnlockStatus);
        } catch (RemoteException ex) {
            Log.e(LOG_TAG, "getSimPersoUnlockStatus " + ex);
            return null;
        }
        return qtiPersoUnlockStatus;
    }

    @Override
    public boolean isCiwlanAvailable() {
        Log.d(LOG_TAG, "isCiwlanAvailable()");
        try {
            return getQtiRadio().isCiwlanAvailable();
        } catch(RemoteException ex) {
            Log.e(LOG_TAG, "isCiwlanAvailable Failed" + ex);
            return false;
        }
    }

    @Override
    public void setCiwlanModeUserPreference(Token token, CiwlanConfig ciwlanConfig)
            throws RemoteException {
        if (!ciwlanConfig.isValid()) {
            Log.e(LOG_TAG, "setCiwlanModeUserPreference is called with invalid config");
            return;
        }
        final int serial = token.get();
        Log.d(LOG_TAG, "setCiwlanModeUserPreference: serial = " + serial);
        mInflightRequests.put(serial, token);

        try {
            getQtiRadio().setCiwlanModeUserPreference(serial,
                    QtiRadioUtils.converttoHalCiwlanConfig(ciwlanConfig));
        } catch (RemoteException ex) {
            Log.e(LOG_TAG, "setCiwlanModeUserPreference failed", ex);
            mInflightRequests.remove(serial);
            throw ex;
        }
    }

    @Override
    public CiwlanConfig getCiwlanModeUserPreference() {
        try {
            vendor.qti.hardware.radio.qtiradio.CiwlanConfig config =
                    getQtiRadio().getCiwlanModeUserPreference();
            CiwlanConfig ciwlanConfig =
                    (config == null) ? null : new CiwlanConfig(config.homeMode, config.roamMode);
            Log.d(LOG_TAG, "getCiwlanModeUserPreference: " + ciwlanConfig);
            return ciwlanConfig;
        } catch (RemoteException ex) {
            Log.e(LOG_TAG, "getCiwlanModeUserPreference failed.", ex);
            return null;
        }
    }

    @Override
    public CellularRoamingPreference getCellularRoamingPreference() {
        if (!isFeatureSupported(CELLULAR_ROAMING_FEATURE)) {
            return null;
        }
        try {
            vendor.qti.hardware.radio.qtiradio.CellularRoamingPreference pref = null;
            pref = getQtiRadio().getCellularRoamingPreference();
            CellularRoamingPreference cellularRoamingPref = (pref == null) ? null :
                    new CellularRoamingPreference(pref.internationalCellularRoaming,
                            pref.domesticCellularRoaming);
            return cellularRoamingPref;
        } catch (RemoteException ex) {
            Log.e(LOG_TAG, "getCellularRoamingPreference failed." + ex);
            return null;
        }
    }

    @Override
    public void setCellularRoamingPreference(Token token, CellularRoamingPreference pref) {
        int serial = token.get();
        Log.d(LOG_TAG, "setCellularRoamingPreference: serial = " + serial);
        mInflightRequests.put(serial, token);
        try {
            vendor.qti.hardware.radio.qtiradio.CellularRoamingPreference cellularRoamingPref =
                    new vendor.qti.hardware.radio.qtiradio.CellularRoamingPreference();
            cellularRoamingPref.internationalCellularRoaming =
                    pref.getInternationalCellularRoamingPref();
            cellularRoamingPref.domesticCellularRoaming = pref.getDomesticCellularRoamingPref();
            getQtiRadio().setCellularRoamingPreference(serial, cellularRoamingPref);
        } catch (RemoteException ex) {
            mInflightRequests.remove(serial, token);
            Log.e(LOG_TAG, "setCellularRoamingPreference failed." + ex);
        }
    }

    @Override
    public boolean isEmcSupported() {
        Log.d(LOG_TAG, "isEmcSupported()");
        try {
            return getQtiRadio().isEmcSupported();
        } catch(RemoteException ex) {
            Log.e(LOG_TAG, "isEmcSupported Failed" + ex);
            return false;
        }
    }

    @Override
    public boolean isEmfSupported() {
        Log.d(LOG_TAG, "isEmfSupported()");
        try {
            return getQtiRadio().isEmfSupported();
        } catch(RemoteException ex) {
            Log.e(LOG_TAG, "isEmfSupported Failed" + ex);
            return false;
        }
    }

    @Override
    public void queryNrIcon(Token token) throws RemoteException {
        int serial = token.get();
        Log.d(LOG_TAG, "queryNrIcon: serial = " + serial);
        mInflightRequests.put(serial, token);
        try {
            if (isFeatureSupported(NR_ICON_6RX_FEATURE)) {
                getQtiRadio().queryNrIcon(serial);
            } else {
                mQueryNrIconCalledWithout6RxSupport = true;
                getQtiRadio().queryNrIconType(serial);
            }
        } catch (RemoteException ex) {
            mInflightRequests.remove(serial, token);
            Log.e(LOG_TAG, "queryNrIcon failed." + ex);
        }
    }

    @Override
    public void sendAllEsimProfiles(Token token, boolean status, int refNum, List<String> iccIds)
            throws RemoteException {
        Log.d(LOG_TAG,
                "sendAllEsimProfiles: " + iccIds + " status: " + status + " refNum: " + refNum);
        int serial = token.get();
        mInflightRequests.put(serial, token);
        try {
            getQtiRadio().sendAllEsimProfiles(serial, status, iccIds, refNum);
        } catch (RemoteException ex) {
            mInflightRequests.remove(serial, token);
            Log.e(LOG_TAG, "sendAllEsimProfiles failed" + ex);
        }
    }

    @Override
    public void notifyEnableProfileStatus(Token token, int refNum, boolean result)
            throws RemoteException {
        Log.d(LOG_TAG, "notifyEnableProfileStatus result: " + result + " refNum: " + refNum);
        int serial = token.get();
        mInflightRequests.put(serial, token);
        try {
            getQtiRadio().notifyEnableProfileStatus(serial, result, refNum);
        } catch(RemoteException ex) {
            mInflightRequests.remove(serial, token);
            Log.e(LOG_TAG, "notifyEnableProfileStatus failed" + ex);
        }
    }

    @Override
    public void notifyDisableProfileStatus(Token token, int refNum, boolean result)
            throws RemoteException {
        Log.d(LOG_TAG, "notifyDisableProfileStatus result: " + result + " refNum: " + refNum);
        int serial = token.get();
        mInflightRequests.put(serial, token);
        try {
            getQtiRadio().notifyDisableProfileStatus(serial, result, refNum);
        } catch(RemoteException ex) {
            mInflightRequests.remove(serial, token);
            Log.e(LOG_TAG, "notifyDisableProfileStatus failed" + ex);
        }
    }

    private static String convertNullToEmptyString(String string) {
        return string != null ? string : "";
    }

    private void logd(String s) {
        Log.d(LOG_TAG, s);
    }

    private void loge(String s) {
        Log.e(LOG_TAG, s);
    }

    @Override
    public void registerCallback(IQtiRadioConnectionCallback callback) {
        Log.d(LOG_TAG, "registerCallback: callback = " + callback);
        mCallback = callback;
    }

    @Override
    public void unRegisterCallback(IQtiRadioConnectionCallback callback) {
        Log.d(LOG_TAG, "unRegisterCallback: callback = " + callback);
        if (mCallback == callback) {
            mCallback = null;
        }
    }
}
