/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 * Not a Contribution.
 *
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qualcomm.qti.internal.telephony.data;

import static com.qti.extphone.ExtTelephonyManager.FEATURE_SMART_TEMP_DDS_VIA_RADIO_CONFIG;

import android.annotation.NonNull;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.telephony.AccessNetworkConstants;
import android.telephony.Annotation.DataFailureCause;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.TelephonyManager.SimState;
import android.telephony.data.ApnSetting;
import android.telephony.data.DataCallResponse;
import android.telephony.data.DataProfile;
import android.telephony.ims.ImsException;
import android.telephony.ims.ImsManager;
import android.telephony.ims.ImsMmTelManager;
import android.util.Pair;
import android.util.SparseArray;

import com.android.internal.telephony.data.DataEvaluation.DataEvaluationReason;
import com.android.internal.telephony.DctConstants;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.data.DataEvaluation;
import com.android.internal.telephony.data.DataEvaluation.DataDisallowedReason;
import com.android.internal.telephony.data.DataNetwork;
import com.android.internal.telephony.data.DataNetwork.TearDownReason;
import com.android.internal.telephony.data.DataNetworkController;
import com.android.internal.telephony.data.DataServiceManager;
import com.android.internal.telephony.data.PhoneSwitcher;
import com.android.internal.telephony.data.TelephonyNetworkRequest;
import com.android.internal.telephony.flags.FeatureFlags;
import com.android.internal.telephony.subscription.SubscriptionManagerService;
import com.qti.extphone.Client;
import com.qti.extphone.ExtTelephonyManager;
import com.qti.extphone.ExtPhoneCallbackListener;
import com.qti.extphone.IExtPhoneCallback;
import com.qti.extphone.ServiceCallback;
import com.qti.extphone.Status;
import com.qti.extphone.Token;

import org.codeaurora.telephony.utils.EnhancedRadioCapabilityResponse;

public class QtiDataNetworkController extends DataNetworkController {

    private static final int EVENT_RECONNECT_QTI_EXT_TELEPHONY_SERVICE = DctConstants.BASE + 200;
    private static final int EVENT_QTI_DISCONNECT_DEFAULT_PDN          = DctConstants.BASE + 201;
    private static final int EVENT_QOS_PARAMETERS_CHANGED              = DctConstants.BASE + 202;
    private static final int EVENT_QTI_PHONE_READY                     = DctConstants.BASE + 203;
    private static final int INVALID_CID    = -1;
    private static final int INVALID_INDEX  = -1;
    private static final int RECONNECT_EXT_TELEPHONY_SERVICE_DELAY_MILLISECOND = 2000;

    private Client mClient;
    private ExtTelephonyManager mExtTelephonyManager;
    private EnhancedRadioCapabilityResponse mEnhancedRadioCapability;
    private boolean mIsCiwlanFeatureEnabledByPlatform;
    private boolean mIsCiwlanFeatureEnabledByUser;
    private ContentResolver mResolver;
    private long mCiwlanTimer = -1;
    private Uri mCrossSimCallingUri;
    private boolean mIsMobileDataEnabled;
    private int mEnabledChangedReason;
    private boolean mIsDataRoamingEnabled;
    private boolean mPendingDataOff;
    private boolean mPendingRoamingOff;
    private boolean mIsInSecureMode = false;

    // Whether essential SIM records are loaded.
    private boolean mIsEssentialRecordsLoaded = false;

    private QtiDataConfigManager mQtiDataConfigManager;
    private SmartTempDdsSwitchController mSmartTempDdsSwitchController;
    private boolean mIsUsingRadioConfigForSmartTempDds;
    private int mDataRegState = NetworkRegistrationInfo.REGISTRATION_STATE_UNKNOWN;
    private DataRegStateListener mDataRegStateListener;

    @NonNull
    private final FeatureFlags mFeatureFlags;

    private ServiceCallback mServiceCallback = new ServiceCallback() {
        @Override
        public void onConnected() {
            log("ExtTelephonyService connected");
            int[] events = new int[] {
                    ExtPhoneCallbackListener.EVENT_ON_DDS_SWITCH_CAPABILITY_CHANGE,
                    ExtPhoneCallbackListener.EVENT_ON_DATA_DEACTIVATE_DELAY_TIME,
                    ExtPhoneCallbackListener.EVENT_ON_EPDG_OVER_CELLULAR_DATA_SUPPORTED,
                    ExtPhoneCallbackListener.EVENT_ON_SECURE_MODE_STATUS_CHANGE};
            String packageName = mPhone.getContext().getPackageName();
            mClient = mExtTelephonyManager.registerCallbackWithEvents(
                    packageName, mExtPhoneCallbackListener, events);
            log("Client = " + mClient);
            try {
                mExtTelephonyManager.getQtiRadioCapability(mPhone.getPhoneId(), mClient);
            } catch (RemoteException e) {
                loge("getQtiRadioCapability, remote exception " + e);
            }
            mIsUsingRadioConfigForSmartTempDds = mExtTelephonyManager.
                    isFeatureSupported(FEATURE_SMART_TEMP_DDS_VIA_RADIO_CONFIG);
            log("mIsUsingRadioConfigForSmartTempDds: " + mIsUsingRadioConfigForSmartTempDds);
            if (!mIsUsingRadioConfigForSmartTempDds) {
                mExtTelephonyManager.getDdsSwitchCapability(mPhone.getPhoneId(), mClient);
            }
            try {
                mIsCiwlanFeatureEnabledByPlatform = mExtTelephonyManager.
                         isEpdgOverCellularDataSupported(mPhone.getPhoneId());
                log("isCiwlanFeatureEnabledByPlatform: " + mIsCiwlanFeatureEnabledByPlatform);
            } catch (RemoteException ex) {
                log("isEpdgOverCellularDataSupported Exception " + ex);
            }
            try {
                mExtTelephonyManager.getSecureModeStatus(mClient);
            } catch (RemoteException ex) {
                loge("getSecureModeStatus, remote exception " + ex);
            }
        }

        @Override
        public void onDisconnected() {
            log("ExtTelephonyService disconnected. Client = " + mClient);
            mExtTelephonyManager.unregisterCallback(mExtPhoneCallbackListener);
            mIsCiwlanFeatureEnabledByPlatform = false;
            mClient = null;
            Message msg = obtainMessage(EVENT_RECONNECT_QTI_EXT_TELEPHONY_SERVICE);
            sendMessageDelayed(msg, RECONNECT_EXT_TELEPHONY_SERVICE_DELAY_MILLISECOND);
        }
    };

    protected final ExtPhoneCallbackListener mExtPhoneCallbackListener;
    protected ContentObserver mCiwlanContentObserver;

    protected final @NonNull SparseArray<QtiDataServiceManager> mQtiDataServiceManagers =
            new SparseArray<>();

    public QtiDataNetworkController(@NonNull Phone phone, @NonNull Looper looper,
            @NonNull FeatureFlags featureFlags) {
        super(phone, looper, featureFlags);
        log("QtiDataNetworkController: constructor");
        mFeatureFlags = featureFlags;
        mEnhancedRadioCapability = new EnhancedRadioCapabilityResponse();
        mExtPhoneCallbackListener = new PhoneExtPhoneEventObserver(looper);
        mExtTelephonyManager = ExtTelephonyManager.getInstance(mPhone.getContext());
        mExtTelephonyManager.connectService(mServiceCallback);
        mQtiDataConfigManager = (QtiDataConfigManager) mDataConfigManager;
        mSmartTempDdsSwitchController =
                SmartTempDdsSwitchController.getInstance(mPhone.getContext(), looper);

        for (int transport : mAccessNetworksManager.getAvailableTransports()) {
            mQtiDataServiceManagers.put(transport,
                    (QtiDataServiceManager) mDataServiceManagers.get(transport));
        }

        mQtiDataServiceManagers
                .get(AccessNetworkConstants.TRANSPORT_TYPE_WWAN)
                .registerForQosParametersChanged(this, EVENT_QOS_PARAMETERS_CHANGED);
        mDataRegState = phone.getServiceState().getNetworkRegistrationInfo(
                NetworkRegistrationInfo.DOMAIN_PS,
                AccessNetworkConstants.TRANSPORT_TYPE_WWAN).getRegistrationState();
    }

    public EnhancedRadioCapabilityResponse getEnhancedRadioCapabilityResponse() {
        return mEnhancedRadioCapability;
    }

    @Override
    public void handleMessage (Message msg) {
        log("handleMessage, msg.what = " + msg.what);
        switch (msg.what) {
            case EVENT_RECONNECT_QTI_EXT_TELEPHONY_SERVICE:
                log("EVENT_RECONNECT_QTI_EXT_TELEPHONY_SERVICE");
                mExtTelephonyManager.connectService(mServiceCallback);
                break;
            case EVENT_QTI_DISCONNECT_DEFAULT_PDN:
                log("EVENT_QTI_DISCONNECT_DEFAULT_PDN");
                if (mPendingDataOff) {
                    mPendingDataOff = false;
                    super.onDataEnabledChanged(mIsMobileDataEnabled, mEnabledChangedReason);
                }

                if (mPendingRoamingOff) {
                    mPendingRoamingOff = false;
                    super.onDataRoamingEnabledChanged(mIsDataRoamingEnabled);
                }
                break;
            case EVENT_QOS_PARAMETERS_CHANGED:
                log("EVENT_QOS_PARAMETERS_CHANGED");
                AsyncResult ar = (AsyncResult) msg.obj;
                Pair<Integer, FrameworkQosParameters> resultPair =
                        (Pair<Integer, FrameworkQosParameters>) ar.result;
                if (ar.exception == null && resultPair != null) {
                    onQosParametersChanged(resultPair.first, resultPair.second);
                } else {
                    log("EVENT_QOS_PARAMETERS_CHANGED: exception, ignore.");
                }
                break;
            default:
                super.handleMessage(msg);
        }
    }


    /**
     * Called when QoS parameters change event is triggered.
     *
     * Steps:
     * 1. Check if the cid is valid, i.e., it corresponds to an existing IMS DataNetwork
     * 2. Add the cid and the QoS parameters to our cache
     * 3. Update DataNetwork object for the received cid with the new QoS parameters.
     *
     * @param cid Connection Id of the data call whose QoS parameters were changed
     * @param qosParams the new QoS parameters
     */
    private void onQosParametersChanged(int cid, FrameworkQosParameters qosParams) {
        log("onQosParametersChanged"
                + ", cid: " + cid
                + ", qosParams: " + (qosParams == null ? "null" : qosParams));

        if (qosParams == null) {
            return;
        }

        // 1. Check if the cid is valid, i.e., it corresponds to an existing DataNetwork
        DataNetwork dataNetworkForReceivedCid = mDataNetworkList.stream()
                .filter(dataNetwork -> dataNetwork.getId() == cid)
                .findFirst()
                .orElse(null);

        if (dataNetworkForReceivedCid == null
                || dataNetworkForReceivedCid.getDataProfile() == null) {
            // There is no DataNetwork instance with a valid DataProfile corresponding to the
            // received cid.
            loge("onQosParametersChanged: no associated DataNetwork yet, ignore");
            return;
        }

        log("onQosParametersChanged: dataNetworkForReceivedCid: " + dataNetworkForReceivedCid);

        if ((dataNetworkForReceivedCid.getDataProfile().getSupportedApnTypesBitmask()
                & ApnSetting.TYPE_IMS) == 0) {
            // The DataNetwork instance corresponding to the received cid does not have IMS APN
            loge("onQosParametersChanged: associated DataNetwork not for IMS, ignore");
            return;
        }

        // 2. Add the cid and FrameworkQosParameters to our cache
        mQtiDataServiceManagers
                .get(AccessNetworkConstants.TRANSPORT_TYPE_WWAN)
                .addToQosParamsMap(cid, qosParams);

        // 3. Update DataNetwork object for the received cid with the new QoS parameters
        dataNetworkForReceivedCid.updateQosBearerSessions(qosParams.getQosBearerSessions());
    }

    @Override
    protected void onDataDuringVoiceCallChanged(boolean enabled) {
        log("onDataDuringVoiceCallChanged: " + enabled);
        evaluateAndSendDataDuringVoiceCallInfo(mPhone.getPhoneId(),
                SmartTempDdsSwitchController.REASON_DATA_DURING_VOICE_CALL_CHANGED);
    }

    /** Called when subscription info changed. */
    @Override
    protected void onSubscriptionChanged() {
        if (mSubId != mPhone.getSubId()) {
            if (SubscriptionManager.isValidSubscriptionId(mSubId)) {
                unregisterCiwlanContentObserver();
            }
            if (SubscriptionManager.isValidSubscriptionId(mPhone.getSubId())) {
                registerCiwlanContentObserverAndStartTimer();
            }
        }
        super.onSubscriptionChanged();
    }

    private boolean isCrossSimCallingEnabledByUser() {
        if (!SubscriptionManager.isValidSubscriptionId(mPhone.getSubId())) {
            return false;
        }

        ImsMmTelManager imsMmTelMgr = getImsMmTelManager();
        if (imsMmTelMgr == null) {
            return false;
        }

        try {
            return imsMmTelMgr.isCrossSimCallingEnabled();
        } catch (ImsException exception) {
            log("fail to get cross SIM calling configuration " + exception);
        }
        return false;
    }

    private void sendCrossSimCallingEnabled() {
        if (!SubscriptionManager.isValidSubscriptionId(mPhone.getSubId())) {
            return;
        }

        ImsMmTelManager imsMmTelMgr = getImsMmTelManager();
        if (imsMmTelMgr == null) {
            return;
        }
        mIsCiwlanFeatureEnabledByUser = isCrossSimCallingEnabledByUser();
        log("setCrossSimCallingEnabled: " + mIsCiwlanFeatureEnabledByUser);
        try {
            imsMmTelMgr.setCrossSimCallingEnabled(mIsCiwlanFeatureEnabledByUser);
        } catch (ImsException exception) {
            log("fail to send cross SIM calling configuration: " +
                    mIsCiwlanFeatureEnabledByUser + " Exception: " + exception);
        }
    }

    private ImsMmTelManager getImsMmTelManager() {
        if (!SubscriptionManager.isValidSubscriptionId(mPhone.getSubId())) {
            return null;
        }
        ImsManager imsManager = mPhone.getContext().getSystemService(ImsManager.class);
        return (imsManager == null) ? null : imsManager.getImsMmTelManager(mPhone.getSubId());
    }

    private long getCiwlanTimer() {
        long maxTimer = -1;
        for (int phoneId = 0; phoneId < getPhoneCount(); phoneId++) {
            Phone phone = null;
            try {
                phone = PhoneFactory.getPhone(phoneId);
            } catch (IllegalStateException e) {
                loge("getCiwlanTimer " + e.getMessage());
            }
            if (phone == null) {
                loge("getCiwlanTimer, phone null for phoneId: " + phoneId);
                continue;
            }
            long timer = phone.getCiwlanTimerToDealayDeactivateDataCall();
            if (timer > maxTimer) {
                maxTimer = timer;
            }
        }
        log("getCiwlanTimer(): " + maxTimer);
        return maxTimer;
    }

    private void registerCiwlanContentObserverAndStartTimer() {
        // Register for CrossSimCalling setting uri
        mCrossSimCallingUri =
                Uri.withAppendedPath(SubscriptionManager.CROSS_SIM_ENABLED_CONTENT_URI,
                String.valueOf(mPhone.getSubId()));
        if (mCiwlanContentObserver == null) {
            mCiwlanContentObserver = new ContentObserver(new Handler()) {
                    @Override
                    public void onChange(boolean selfChange, Uri uri) {
                        if (mCrossSimCallingUri.equals(uri)) {
                            log("CIWLAN UI preference changed");
                            int subId;
                            String uriString = uri.getPath();
                            try {
                                subId = Integer.parseInt(
                                        uriString.substring(
                                                uriString.lastIndexOf('/') + 1));
                            } catch (NumberFormatException e) {
                                log("NumberFormatException on " + uriString);
                                return;
                            }

                            if (!SubscriptionManager.isValidSubscriptionId(subId)
                                    || (subId != mPhone.getSubId())) {
                                log("Invalid subId : " + subId);
                                return;
                            }

                            mIsCiwlanFeatureEnabledByUser = isCrossSimCallingEnabledByUser();
                            long ciwlanTimer = getCiwlanTimer();
                            if (!mIsCiwlanFeatureEnabledByUser && (ciwlanTimer > 0)) {
                                sendMessageDelayed(obtainMessage(
                                        EVENT_QTI_DISCONNECT_DEFAULT_PDN),
                                        ciwlanTimer);
                            }
                        }
                    }
                };
        }
        mPhone.getContext().getContentResolver().registerContentObserver(
                mCrossSimCallingUri, false, mCiwlanContentObserver);
    }

    private void unregisterCiwlanContentObserver() {
        if (mCiwlanContentObserver != null) {
            mPhone.getContext().getContentResolver()
                    .unregisterContentObserver(mCiwlanContentObserver);
        }
    }

    @Override
    protected void onDataEnabledChanged(boolean enabled,
            @TelephonyManager.DataEnabledChangedReason int reason) {
        log("QtiDNC onDataEnabledChanged: enabled = " + enabled + " reason = " + reason);

        boolean isDds = isCurrentSubDds(mPhone.getSubId());
        if (isDds) {
            // Mobile data of DDS has changed.
            // Evaluate if data during calls is allowed for nDDS.
            evaluateAndSendDataDuringVoiceCallInfo(getNonDdsPhoneId(),
                    SmartTempDdsSwitchController.REASON_DDS_DATA_ENABLED_CHANGED);
        }

        long ciwlanTimer = getCiwlanTimer();
        if (isDds && !enabled && (ciwlanTimer > 0)) {
            if (!hasMessages(EVENT_QTI_DISCONNECT_DEFAULT_PDN)) {
                mIsMobileDataEnabled = enabled;
                mEnabledChangedReason = reason;
                sendMessageDelayed(obtainMessage(EVENT_QTI_DISCONNECT_DEFAULT_PDN),
                        ciwlanTimer);
            }
            mPendingDataOff = true;
        } else {
            super.onDataEnabledChanged(enabled, reason);
        }
    }

    @Override
    protected void onDataRoamingEnabledChanged(boolean enabled) {
        log("QtiDNC onDataRoamingEnabledChanged: enabled=" + enabled);

        boolean isDds = isCurrentSubDds(mPhone.getSubId());
        if (isDds) {
            // When roaming switch of the DDS is toggled, temp switch
            // may not be allowed if the roaming criteria is not met.
            // See {@link#isDdsRoamingCriteriaSatisfiedForTempSwitch()}
            evaluateAndSendDataDuringVoiceCallInfo(getNonDdsPhoneId(),
                    SmartTempDdsSwitchController.REASON_DDS_DATA_ROAMING_ENABLED_CHANGED);
        }

        long ciwlanTimer = getCiwlanTimer();
        if (isDds && !enabled && (ciwlanTimer > 0)) {
            if (!hasMessages(EVENT_QTI_DISCONNECT_DEFAULT_PDN)) {
                mIsDataRoamingEnabled = enabled;
                sendMessageDelayed(obtainMessage(EVENT_QTI_DISCONNECT_DEFAULT_PDN),
                        ciwlanTimer);
            }
            mPendingRoamingOff = true;
        } else {
            super.onDataRoamingEnabledChanged(enabled);
        }
    }

    private Phone getDefaultDataPhone() {
        int defaultDataPhoneId = SubscriptionManager.INVALID_PHONE_INDEX;
        Phone defaultDataPhone = null;
        boolean isDataEnableOnDds = false;
        SubscriptionManagerService subMgrService = SubscriptionManagerService.getInstance();

        int defaultDataSubId = subMgrService.getDefaultDataSubId();
        if (SubscriptionManager.isValidSubscriptionId(defaultDataSubId)) {
            defaultDataPhoneId = subMgrService.getPhoneId(defaultDataSubId);
        }

        if (SubscriptionManager.isValidPhoneId(defaultDataPhoneId)) {
            try {
                defaultDataPhone = PhoneFactory.getPhone(defaultDataPhoneId);
            } catch (IllegalStateException ex) {
                loge("getDefaultDataPhone: " + ex.getMessage());
            }
        }

        return defaultDataPhone;
    }

    /**
     * @return {@code true} if mobile data switch of the DDS is turned on.
     */
    private boolean isDataEnabledOnDds() {
        Phone defaultDataPhone = getDefaultDataPhone();
        if (defaultDataPhone != null) {
            return defaultDataPhone.getDataSettingsManager()
                    .isDataEnabledForReason(TelephonyManager.DATA_ENABLED_REASON_USER);
        }

        return false;
    }

    /**
     * @return {@code true} if we should allow temp DDS switch to non-DDS based on the roaming
     * criteria of the DDS:
     * If DDS is in home network, allow temp DDS switch
     * If DDS is in roaming network and data roaming is enabled, allow temp DDS switch
     * If DDS is in roaming network and data roaming is disabled, block temp DDS switch
     */
    private boolean isDdsRoamingCriteriaSatisfiedForTempSwitch() {
        Phone defaultDataPhone = getDefaultDataPhone();
        if (defaultDataPhone == null) {
            return false;
        }

        try {
            // If DDS is in home network, temp DDS switch can proceed
            if (!defaultDataPhone.getServiceState().getDataRoaming()) {
                return true;
            }

            // If DDS is in roaming, check if data roaming is enabled by the user
            return defaultDataPhone.getDataSettingsManager().isDataRoamingEnabled();
        } catch (NullPointerException ex) {
            loge("Exception while checking DDS roaming state: " + ex);
            return false;
        }
    }

    @Override
    protected void onServiceStateChanged() {
        if (isCurrentSubDds(mPhone.getSubId())) {
            handleDdsRoamingStateChangesIfRequired();
        }

        super.onServiceStateChanged();

        // Handle data reg state on nDDS SUB.
        if (mPhone.getSubId() != SubscriptionManager.getDefaultDataSubscriptionId()) {
            int dataRegState = mPhone.getServiceState().getNetworkRegistrationInfo(
                    NetworkRegistrationInfo.DOMAIN_PS,
                    AccessNetworkConstants.TRANSPORT_TYPE_WWAN).getRegistrationState();
            if (mDataRegState != dataRegState) {
                log("onServiceStateChanged data reg state on " + mPhone.getSubId() + " "
                        + NetworkRegistrationInfo.registrationStateToString(mDataRegState)
                        + " -> "
                        + NetworkRegistrationInfo.registrationStateToString(dataRegState));
                mDataRegState = dataRegState;
                PhoneSwitcher phoneSwitcher = PhoneSwitcher.getInstance();
                if (phoneSwitcher != null && phoneSwitcher instanceof QtiPhoneSwitcher) {
                    QtiPhoneSwitcher qtiPhoneSwitcher = (QtiPhoneSwitcher) phoneSwitcher;
                    qtiPhoneSwitcher.sendDataRegStateChanged();
                }
            }
        }
    }

    /**
     * Send the new state of 'data during calls' based on whether the current roaming
     * state of the DDS allows temp DDS switch.
     */
    private void handleDdsRoamingStateChangesIfRequired() {
        try {
            boolean oldRoamingState = mServiceState.getDataRoaming();
            boolean newRoamingState =
                    mPhone.getServiceStateTracker().getServiceState().getDataRoaming();

            if (oldRoamingState != newRoamingState) {
                log("DDS roaming state changed: " + oldRoamingState + " -> " + newRoamingState);
                evaluateAndSendDataDuringVoiceCallInfo(getNonDdsPhoneId(),
                        SmartTempDdsSwitchController.REASON_DDS_ROAMING_STATE_CHANGED);
            }
        } catch (NullPointerException ex) {
            loge("Exception while handling DDs roaming state changes: " + ex);
        }
    }

    /**
     * Returns the phoneId of the non-DDS
     */
    private int getNonDdsPhoneId() {
        for (int phoneId = 0; phoneId < getPhoneCount(); phoneId++) {
            int[] subIds = SubscriptionManager.getSubId(phoneId);
            if (subIds != null
                    && subIds.length > 0
                    && subIds[0] != SubscriptionManager.getDefaultDataSubscriptionId()) {
                log("getNonDdsPhoneId: " + phoneId);
                return phoneId;
            }
        }
        return SubscriptionManager.INVALID_PHONE_INDEX;
    }

    /**
     * Evaluate whether the modem should send temp DDS switch recommendations. The goal here is to
     * refer to all the conditions that can impact the step of switching data back to the original
     * DDS after call on nDDS ends. The final boolean determined is sent to lower layers via
     * {@link IExtPhone#sendUserPreferenceForDataDuringVoiceCall(int, boolean)}, which will be
     * further used by the modem to send temp DDS switch recommendations.
     *
     * @param phoneId The phoneId for which the evaluation needs to be done
     * @param reason The reason for evaluation
     */
    /*package visible*/ void evaluateAndSendDataDuringVoiceCallInfo(int phoneId, String reason) {
        if (!SubscriptionManager.isValidPhoneId(phoneId)) {
            return;
        }

        if (isUsingRadioConfigForSmartTempDds()) {
            mSmartTempDdsSwitchController.evaluateAndSendDataDuringCallsInfo(reason);
            return;
        }

        Phone phone;

        try {
            phone = PhoneFactory.getPhone(phoneId);
        } catch (IllegalStateException e) {
            loge("evaluateAndSendDataDuringVoiceCallInfo " + e.getMessage());
            return;
        }

        if (phone == null) {
            loge("evaluateAndSendDataDuringVoiceCallInfo, phone null for phoneId: " + phoneId);
            return;
        }

        if (!SubscriptionManager.isValidSubscriptionId(phone.getSubId())) {
            loge("evaluateAndSendDataDuringVoiceCallInfo, sub id is invalid");
            return;
        }

        // Always send false on DDS
        boolean isDataAllowedDuringCall = false;

        if (phoneId == getNonDdsPhoneId()) {
            // For non DDS, send false if
            // 1. Data during calls UI switch is turned off, or
            // 2. mobile data on DDS is disabled, or
            // 3. DDS is in roaming and user has disabled data roaming
            isDataAllowedDuringCall =
                    phone.getDataSettingsManager()
                    .isMobileDataPolicyEnabled(TelephonyManager
                    .MOBILE_DATA_POLICY_DATA_ON_NON_DEFAULT_DURING_VOICE_CALL)
                    && isDataEnabledOnDds()
                    && isDdsRoamingCriteriaSatisfiedForTempSwitch();

            log("isDataAllowedDuringCall: " + isDataAllowedDuringCall
                    + ", data during calls switch: "
                    + phone.getDataSettingsManager().isMobileDataPolicyEnabled(TelephonyManager
                    .MOBILE_DATA_POLICY_DATA_ON_NON_DEFAULT_DURING_VOICE_CALL)
                    + ", isDataEnabledOnDds: " + isDataEnabledOnDds()
                    + ", isDdsRoamingSatisfied: " + isDdsRoamingCriteriaSatisfiedForTempSwitch()
                    + ", phoneId: " + phoneId
                    + ", reason: " + reason);
        }

        if (mExtTelephonyManager != null
                && phone.getSmartTempDdsSwitchSupported()) {
            log("Evaluated data during voice call: " + isDataAllowedDuringCall
                    + " phoneId: " + phoneId);
            mExtTelephonyManager.sendUserPreferenceForDataDuringVoiceCall(
                    phoneId, isDataAllowedDuringCall, mClient);
        }

    }

    /**
     * @return {@code true} if the given subId corresponds to the DDS
     */
    private boolean isCurrentSubDds(int subId) {
        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            return subId == SubscriptionManager.getDefaultDataSubscriptionId();
        }
        return false;
    }

    private int getPhoneCount() {
        TelephonyManager tm = (TelephonyManager) mPhone.getContext().
                getSystemService(Context.TELEPHONY_SERVICE);
        return tm.getActiveModemCount();
    }

    /**
     * Called when CarrierConfigs have been fetched after reading the essential SIM records.
     */
    @Override
    public void onCarrierConfigLoadedForEssentialRecords() {
        log("QtiDNC onCarrierConfigLoadedForEssentialRecords");
        getDataConfigManager().onCarrierConfigLoadedForEssentialRecords();
    }

    /**
     * Set the value of {@link mIsEssentialRecordsLoaded} to true or false
     * This is set to true after SIM moves to SIM_STATE_READY and all essential records get loaded
     * It is reset to false when the SIM state moves to any state other than SIM_STATE_LOADED
     */
    public void setEssentialRecordsLoaded(boolean isLoaded) {
        log("QtiDNC setEssentialRecordsLoaded to " + isLoaded);
        mIsEssentialRecordsLoaded = isLoaded;
    }

    /**
     * Return the value of {@link mIsEssentialRecordsLoaded}
     */
    private boolean isEssentialRecordsLoaded() {
        log("QtiDNC isEssentialRecordsLoaded: " + mIsEssentialRecordsLoaded);
        return mIsEssentialRecordsLoaded;
    }

    /**
     * Called when SIM state changes.
     *
     * @param simState SIM state. (Note this is mixed with card state and application state.)
     */
    @Override
    protected void onSimStateChanged(@SimState int simState) {
        resetEssentialRecordsLoadedStateIfRequired(simState);
        super.onSimStateChanged(simState);
    }

    /**
     * Reset the state of flag {@link mIsEssentialRecordsLoaded} to false when the SIM
     * state moves to a state which is not SIM_STATE_LOADED
     */
    private void resetEssentialRecordsLoadedStateIfRequired(@SimState int simState) {
        if (simState != TelephonyManager.SIM_STATE_PRESENT
                && simState != TelephonyManager.SIM_STATE_LOADED) {
            setEssentialRecordsLoaded(false);
        }
    }

    /**
     * Add data disallowed reason when device is in Secure Mode.
     *
     * @param evaluation The evaluation result from
     * {@link super#evaluateDataNetwork(DataNetwork, DataEvaluationReason)} or
     * {@link super#evaluateNetworkRequest(TelephonyNetworkRequest, DataEvaluationReason)}
     */
    @Override
    protected void addDataDisallowedReasonWhenInSecureMode(DataEvaluation evaluation) {
        log("addDataDisallowedReasonWhenInSecureMode: mIsInSecureMode = " + mIsInSecureMode);
        if (mIsInSecureMode) {
            evaluation.addDataDisallowedReason(DataDisallowedReason.DATA_RESTRICTED_BY_SECURE_MODE);
        }
    }

    /**
     * Evaluate if data setup should be allowed with the current SIM state.
     *
     * @param evaluation The evaluation result from
     * {@link super#evaluateDataNetwork(DataNetwork, DataEvaluationReason)} or
     * {@link super#evaluateNetworkRequest(TelephonyNetworkRequest, DataEvaluationReason)}
     */
    @Override
    protected void checkSimStateForDataEvaluation(DataEvaluation evaluation) {
        if (mSimState != TelephonyManager.SIM_STATE_LOADED
                && !isEssentialRecordsLoaded()) {
            log("QtiDCT SIM is neither loaded, nor essentially loaded");
            evaluation.addDataDisallowedReason(DataDisallowedReason.SIM_NOT_READY);
        }
    }

    /**
     * Called when data network is connected.
     *
     * @param dataNetwork The data network.
     */
    @Override
    public void onDataNetworkConnected(@NonNull DataNetwork dataNetwork) {
        if (mQtiDataConfigManager.isPdpRejectConfigEnabled() &&
                dataNetwork.isInternetSupported()) {
            log("QtiDNC onDataNetworkConnected: Reset data reject count ..");
            mDataRetryManager.handlePdpRejectCauseSuccess();
        }
        super.onDataNetworkConnected(dataNetwork);
    }

    /**
     * Called when data network gets disconnected.
     *
     * @param dataNetwork The data network.
     * @param cause The disconnect cause.
     */
    protected void onDataNetworkDisconnected(@NonNull DataNetwork dataNetwork,
            @DataFailureCause int cause, @TearDownReason int tearDownReason) {
        log("QtiDNC onDataNetworkDisconnected: cause: " + cause +
                ", tearDownReason: " + tearDownReason + ", " + dataNetwork);
        mQtiDataServiceManagers
                .get(AccessNetworkConstants.TRANSPORT_TYPE_WWAN)
                .removeFromQosParamsMap(dataNetwork.getId());
        super.onDataNetworkDisconnected(dataNetwork, cause, tearDownReason);
    }

    /**
     * Check if PDP reject retry is in progress
     *
     * @param networkRequest the network request for which the current query is made. PDP reject
     * retry is applicable only if the networkRequest corresponds to internet type.
     *
     * @return {@code true} if the check is successful.
     */
    @Override
    protected boolean isPdpRejectRetryOngoing(TelephonyNetworkRequest networkRequest) {
        int transport = mAccessNetworksManager.getPreferredTransportByNetworkCapability(
                networkRequest.getHighestPriorityApnTypeNetworkCapability());
        DataProfile dataProfile = mDataProfileManager
                .getDataProfileForNetworkRequest(networkRequest, getDataNetworkType(transport),
                mPhone.getServiceState().isUsingNonTerrestrialNetwork(),
                isEsimBootStrapProvisioningActivated(), true);
        QtiDataRetryManager qtiDataRetryManager = (QtiDataRetryManager) mDataRetryManager;

        if (qtiDataRetryManager.isPdpRejectRetryOngoing()
                && qtiDataRetryManager.isDataProfileOfTypeDefault(dataProfile)) {
            log("isPdpRejectRetryOngoing: true");
            return true;
        }
        return false;
    }

    private boolean isUsingRadioConfigForSmartTempDds() {
        return mIsUsingRadioConfigForSmartTempDds
                && mSmartTempDdsSwitchController != null;
    }

    public void setDataRegStateListener(DataRegStateListener listener) {
        mDataRegStateListener = listener;
    }

    protected class PhoneExtPhoneEventObserver extends  ExtPhoneCallbackListener {
        public PhoneExtPhoneEventObserver(Looper looper) {
            super(looper);
        }

        @Override
        public void getQtiRadioCapabilityResponse(int slotId, Token token, Status status, int raf)
                throws RemoteException {
            if (!SubscriptionManager.isValidSlotIndex(slotId) || mPhone.getPhoneId() != slotId) {
                return;
            }

            mEnhancedRadioCapability.updateEnhancedRadioCapability(raf);
        }

        @Override
        public void onDdsSwitchCapabilityChange(int slotId, Token token,
                Status status, boolean smartDdsSupport) throws RemoteException {
            log("ExtPhoneCallback: onDdsSwitchCapabilityChange support: " +
                    smartDdsSupport + " SlotId: " + slotId);

            if (!SubscriptionManager.isValidSlotIndex(slotId)) {
                return;
            }
            if (mPhone.getPhoneId() == slotId) {
                mPhone.setSmartTempDdsSwitchSupported(smartDdsSupport);
                evaluateAndSendDataDuringVoiceCallInfo(mPhone.getPhoneId(),
                        "DDS_SWITCH_CAPABILITY_CHANGED");
            }
        }

        @Override
        public void onDataDeactivateDelayTime(int slotId, long delayTimeMilliSecs)
                throws RemoteException {
            log("ExtPhoneCallback: onDataDeactivateDelayTime  SlotId: " + slotId +
                    " delayTimeMilliSecs " + delayTimeMilliSecs);
            if (slotId != mPhone.getPhoneId()) return;

            //delayTime must always be grater than or equal to zero.
            if (delayTimeMilliSecs < 0) return;

            mPhone.setCiwlanTimerToDealayDeactivateDataCall(delayTimeMilliSecs);

            //Remove existing message already scheduled with delay and schedule new message
            //without delay to disconnect pdn when data is off
            //and onDataDeactivateDelayTime is called before previous mCiwlanTimer expires.
            if (getCiwlanTimer() == 0) {
                //remove if the message is already scheduled with delay
                removeMessages(EVENT_QTI_DISCONNECT_DEFAULT_PDN);
                sendMessage(obtainMessage(EVENT_QTI_DISCONNECT_DEFAULT_PDN));
            }
        }

        @Override
        public void onEpdgOverCellularDataSupported(int slotId, boolean support)
                throws RemoteException {
            log("ExtPhoneCallback: onEpdgOverCellularDataSupported SlotId: " + slotId +
                    " support " + support);
            if (slotId != mPhone.getPhoneId()) return;

            mIsCiwlanFeatureEnabledByPlatform = support;

            if (support) {
                sendCrossSimCallingEnabled();
            }
        }

        @Override
        public void onSecureModeStatusChange(boolean enabled) throws RemoteException {
            log("ExtPhoneCallback: onSecureModeStatusChange enabled: " + enabled);
            mIsInSecureMode = enabled;
            if (!enabled) {
                log("Reevaluating unsatisfied network requests due to Secure Mode exit");
                sendMessage(obtainMessage(EVENT_REEVALUATE_UNSATISFIED_NETWORK_REQUESTS,
                        DataEvaluationReason.SECURE_MODE_STATE_CHANGED));
            }
        }

        @Override
        public void getSecureModeStatusResponse(Token token, Status status, boolean enableStatus) {
            log("ExtPhoneCallback: getSecureModeStatusResponse enabled: " + enableStatus);
            mIsInSecureMode = enableStatus;
        }
    };

    public interface DataRegStateListener {
        public void onDataRegStateChanged(int newState);
    }
}
