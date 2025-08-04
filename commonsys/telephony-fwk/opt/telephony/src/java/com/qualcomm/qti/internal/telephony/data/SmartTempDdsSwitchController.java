/*
 * Copyright (c) 2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.internal.telephony.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.qti.extphone.Client;
import com.qti.extphone.ExtTelephonyManager;
import com.qti.extphone.ExtPhoneCallbackListener;
import com.qti.extphone.ServiceCallback;
import com.qti.extphone.Status;
import com.qti.extphone.Token;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.subscription.SubscriptionManagerService;

import java.util.Arrays;

public class SmartTempDdsSwitchController {
    private static final String TAG = "SmartTempDdsSwitchController";
    private static SmartTempDdsSwitchController sInstance;
    private Client mClient;
    private Context mContext;
    private ExtTelephonyManager mExtTelephonyManager;
    private SubscriptionManager mSubscriptionManager;
    private LocalHandler mHandler;
    private final ExtPhoneCallbackListener mExtPhoneCallbackListener;

    private final Object mCapabilityLock = new Object();
    private final Object mIsTelephonyTempDdsSwitchInUseLock = new Object();

    private boolean mIsTempSmartDdsSwitchCapable;
    private boolean mIsTelephonyTempDdsSwitchInUse;
    private boolean[] mIsTempDdsSwitchAllowedOnSlot; // Size is 2 always after initiated.

    private static final int EVENT_RECONNECT_QTI_EXT_TELEPHONY_SERVICE = 100;
    private static final int EVENT_EVALUATE_AND_SEND_DATA_DURING_CALLS_INFO = 101;
    private static final int RETRY_DELAY_TWO_SECONDS = 2000;

    /* package visible */
    static final String REASON_DDS_ROAMING_STATE_CHANGED = "DDS_ROAMING_STATE_CHANGED";
    static final String REASON_DATA_DURING_VOICE_CALL_CHANGED =
            "DATA_DURING_VOICE_CALL_CHANGED";
    static final String REASON_DDS_DATA_ENABLED_CHANGED = "DDS_DATA_ENABLED_CHANGED";
    static final String REASON_DDS_DATA_ROAMING_ENABLED_CHANGED =
            "DDS_DATA_ROAMING_ENABLED_CHANGED";
    static final String REASON_SUBSCRIPTION_CHANGED = "SUBSCRIPTION_CHANGED";
    static final String REASON_DDS_SWITCH_CAPABILITY_CHANGED =
            "DDS_SWITCH_CAPABILITY_CHANGED";
    static final String REASON_ACTIVE_PHONE_SWITCH =
            "ACTIVE_PHONE_SWITCH";

    public static SmartTempDdsSwitchController getInstance(Context context, Looper looper) {
        synchronized (SmartTempDdsSwitchController.class) {
            if (sInstance == null) {
                sInstance = new SmartTempDdsSwitchController(context, looper);
            }
            return sInstance;
        }
    }

    private SmartTempDdsSwitchController(Context context, Looper looper) {
        mContext = context;
        mExtTelephonyManager = ExtTelephonyManager.getInstance(context);
        mSubscriptionManager = (SubscriptionManager) context.getSystemService(
                Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        mExtPhoneCallbackListener = new MyExtPhoneCallbackListener(looper);
        mExtTelephonyManager.connectService(mServiceCallback);
        mHandler = new LocalHandler(looper);
    }

    class LocalHandler extends Handler {
        LocalHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_RECONNECT_QTI_EXT_TELEPHONY_SERVICE: {
                    log("EVENT_RECONNECT_QTI_EXT_TELEPHONY_SERVICE");
                    mExtTelephonyManager.connectService(mServiceCallback);
                    break;
                }

                case EVENT_EVALUATE_AND_SEND_DATA_DURING_CALLS_INFO: {
                    String reason = (String) msg.obj;
                    onEvaluateAndSendDataDuringCallsInfo(reason);
                    break;
                }
            }
        }
    }

    /**
     * Returns whether the device is capable of performing Smart Temp DDS Switch.
     * This reflects the value received via {@link IQtiRadioConfig#onDdsSwitchCapabilityChanged}.
     *
     * @return true if the device is capable of Smart Temp DDS Switch functionality,
     *         false otherwise.
     */
    public boolean isSmartTempDdsSwitchCapable() {
        boolean isCapable = false;
        synchronized(mCapabilityLock) {
            isCapable = mIsTempSmartDdsSwitchCapable;
        }
        return isCapable;
    }

    /**
     * Returns whether telephony-initiated Temp DDS switch is currently being used.
     * This reflects the value received via {@link IQtiRadioConfig#onDdsSwitchCriteriaChanged}.
     *
     * @return true/false if telephony-initiated Temp DDS switch is enabled
     */
    public boolean isTelephonyTempDdsSwitchInUse() {
        boolean isInUse = false;
        synchronized(mIsTelephonyTempDdsSwitchInUseLock) {
            isInUse = mIsTelephonyTempDdsSwitchInUse
                    && mIsTempDdsSwitchAllowedOnSlot != null
                    && (mIsTempDdsSwitchAllowedOnSlot[0] || mIsTempDdsSwitchAllowedOnSlot[1]);
        }
        return isInUse;
    }

    /**
     * Evaluates the data during calls status for both slots and sends it to the lower layers.
     *
     * This is triggered from {@link QtiDataNetworkController} whenever any event occurs that can
     * result in a change of the data during calls state, namely:
     * a. DDS mobile data enabled state changes,
     * b. DDS enters/leaves roaming area,
     * c. DDS data roaming enabled state changes.
     *
     * @param reason Reason for the evaluation
     */
    public void evaluateAndSendDataDuringCallsInfo(String reason) {
        Message msg = mHandler.obtainMessage(
                EVENT_EVALUATE_AND_SEND_DATA_DURING_CALLS_INFO, reason);
        mHandler.sendMessage(msg);
    }

    private void onEvaluateAndSendDataDuringCallsInfo(String reason) {
        boolean isSuccessfulEvaluation = evaluateDataDuringCallsInfo(reason);
        if (isSuccessfulEvaluation
                && mExtTelephonyManager != null
                && isSmartTempDdsSwitchCapable()) {
            log("Evaluated data during voice call: "
                    + Arrays.toString(mIsTempDdsSwitchAllowedOnSlot)
                    + " reason: " + reason);
            mExtTelephonyManager.sendUserPreferenceConfigForDataDuringVoiceCall(
                    mIsTempDdsSwitchAllowedOnSlot, mClient);
        }
    }

    /**
     * This method evaluates if temp DDS switch is allowed, and stores
     * the latest evaluation in {@link mIsTempDdsSwitchAllowedOnSlot}
     *
     * @param reason Reason for the evaluation
     * @return true if we should send the evaluation results to the lower layers or not.
     */
    private boolean evaluateDataDuringCallsInfo(String reason) {
        // if we are on single sim configuration, return false
        int activeModemCount = TelephonyManager.getDefault().getActiveModemCount();
        if (activeModemCount < 2) {
            loge("evaluateAndSendDataDuringCallsInfo, activeModemCount: " + activeModemCount);
            return false;
        }

        boolean[] userPreferences = new boolean[activeModemCount];
        for (int phoneId = 0; phoneId < activeModemCount; phoneId++) {
            Phone phone;
            try {
                phone = PhoneFactory.getPhone(phoneId);
            } catch (IllegalStateException e) {
                loge("evaluateAndSendDataDuringCallsInfo " + e.getMessage());
                return false;
            }

            if (phone == null) {
                loge("evaluateAndSendDataDuringCallsInfo, phone null for phoneId: " + phoneId);
                return false;
            }

            if (!SubscriptionManager.isValidSubscriptionId(phone.getSubId())) {
                // If the subId becomes valid at a later point in time, we will be notified of this
                // change via the OnSubscriptionsChangedListener, and will evaluate the data during
                // calls info at that time.
                loge("evaluateAndSendDataDuringCallsInfo, invalid subId, ignore");
                return false;
            }

            boolean isDataAllowedDuringCall = false;

            if (phoneId == getDefaultDataPhoneId()) {
                // This phoneId corresponds to the DDS
                // Always send false for the DDS
                log("evaluateAndSendDataDuringCallsInfo: DDS is on : " + phoneId);
                isDataAllowedDuringCall = false;
            } else {
                // For non DDS, send false if
                // 1. Data during calls UI switch is turned off, or
                // 2. mobile data on DDS is disabled, or
                // 3. DDS is in roaming and user has disabled data roaming
                boolean dataDuringCallsUiSwitchStatus = phone.getDataSettingsManager()
                        .isMobileDataPolicyEnabled(TelephonyManager
                        .MOBILE_DATA_POLICY_DATA_ON_NON_DEFAULT_DURING_VOICE_CALL);
                boolean isDataEnabledOnDds = isDataEnabledOnDds();
                boolean isDdsRoamingCriteriaSatisfiedForTempSwitch =
                        isDdsRoamingCriteriaSatisfiedForTempSwitch();

                isDataAllowedDuringCall =
                        dataDuringCallsUiSwitchStatus
                        && isDataEnabledOnDds
                        && isDdsRoamingCriteriaSatisfiedForTempSwitch;

                log("isDataAllowedDuringCall: " + isDataAllowedDuringCall
                        + ", dataDuringCallsUiSwitchStatus: " + dataDuringCallsUiSwitchStatus
                        + ", isDataEnabledOnDds: " + isDataEnabledOnDds
                        + ", isDdsRoamingSatisfied: " + isDdsRoamingCriteriaSatisfiedForTempSwitch
                        + ", phoneId: " + phoneId
                        + ", reason: " + reason);
            }

            userPreferences[phoneId] = isDataAllowedDuringCall;
        }

        synchronized(mIsTelephonyTempDdsSwitchInUseLock) {
            mIsTempDdsSwitchAllowedOnSlot = userPreferences;
        }
        return true;
    }

    public boolean isDataAllowedDuringCall() {
        boolean isDataAllowedDuringCall = false;
        boolean isSuccessfulEvaluation = evaluateDataDuringCallsInfo("IS_DATA_ALLOWED_DURING_CALL");
        if(!isSuccessfulEvaluation) {
            log("isDataAllowedDuringCall() : isSuccessfulEvaluation = " + isSuccessfulEvaluation);
            return false;
        }
        synchronized(mIsTelephonyTempDdsSwitchInUseLock) {
            isDataAllowedDuringCall =
                    (mIsTempDdsSwitchAllowedOnSlot[0] || mIsTempDdsSwitchAllowedOnSlot[1]);
        }
        log("isDataAllowedDuringCall() : isDataAllowedDuringCall = " + isDataAllowedDuringCall
                + ", isSuccessfulEvaluation = " + isSuccessfulEvaluation);
        return isDataAllowedDuringCall;
    }

    private int getDefaultDataPhoneId() {
        SubscriptionManagerService subMgrService = SubscriptionManagerService.getInstance();
        int defaultDataSubId = subMgrService.getDefaultDataSubId();
        int defaultDataPhoneId = SubscriptionManager.INVALID_PHONE_INDEX;
        if (SubscriptionManager.isValidSubscriptionId(defaultDataSubId)) {
            defaultDataPhoneId = subMgrService.getPhoneId(defaultDataSubId);
        }
        return defaultDataPhoneId;
    }

    private Phone getDefaultDataPhone() {
        int defaultDataPhoneId = getDefaultDataPhoneId();
        Phone defaultDataPhone = null;
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

    class MyExtPhoneCallbackListener extends ExtPhoneCallbackListener {
        public MyExtPhoneCallbackListener(Looper looper) {
            super(looper);
        }

        @Override
        public void onDdsSwitchConfigCapabilityChanged(Token token, Status status,
                boolean isCapable) throws RemoteException {
            log("ExtPhoneCallback: onDdsSwitchConfigCapabilityChanged: " + isCapable);

            synchronized(mCapabilityLock) {
                mIsTempSmartDdsSwitchCapable = isCapable;
            }

            // Send data during calls info to the modem when it sends the DDS switch
            // capability indication
            evaluateAndSendDataDuringCallsInfo(REASON_DDS_SWITCH_CAPABILITY_CHANGED);
        }

        @Override
        public void onDdsSwitchConfigCriteriaChanged(boolean telephonyDdsSwitch)
                throws RemoteException {
            log("ExtPhoneCallback: onDdsSwitchConfigCriteriaChanged: " + telephonyDdsSwitch);
            synchronized(mIsTelephonyTempDdsSwitchInUseLock) {
                mIsTelephonyTempDdsSwitchInUse = telephonyDdsSwitch;
            }
        }
    }

    private ServiceCallback mServiceCallback = new ServiceCallback() {
        @Override
        public void onConnected() {
            log("ExtTelephonyService connected");
            int[] events = new int[] {
                    ExtPhoneCallbackListener.EVENT_ON_DDS_SWITCH_CONFIG_CAPABILITY_CHANGED,
                    ExtPhoneCallbackListener.EVENT_ON_DDS_SWITCH_CONFIG_CRITERIA_CHANGED};
            String packageName = mContext.getPackageName();
            mClient = mExtTelephonyManager.registerCallbackWithEvents(
                    packageName, mExtPhoneCallbackListener, events);
            log("Client = " + mClient);
            mExtTelephonyManager.getDdsSwitchConfigCapability(mClient);
        }

        @Override
        public void onDisconnected() {
            log("ExtTelephonyService disconnected. Client = " + mClient);
            mExtTelephonyManager.unregisterCallback(mExtPhoneCallbackListener);
            mClient = null;
            Message msg = mHandler.obtainMessage(EVENT_RECONNECT_QTI_EXT_TELEPHONY_SERVICE);
            mHandler.sendMessageDelayed(msg, RETRY_DELAY_TWO_SECONDS);
        }
    };

    private void loge(String message) {
        Log.e(TAG, message);
    }

    private void log(String message) {
        Log.d(TAG, message);
    }
}
