/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
/*
 * Not a Contribution, Apache license notifications and license are retained
 * for attribution purposes only.
 *
 * Copyright (C) 2015 The Android Open Source Project
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

import static android.telephony.SubscriptionManager.DEFAULT_SUBSCRIPTION_ID;
import static android.telephony.SubscriptionManager.INVALID_PHONE_INDEX;
import static android.telephony.SubscriptionManager.INVALID_SUBSCRIPTION_ID;
import static android.telephony.TelephonyManager.RADIO_POWER_UNAVAILABLE;
import static android.telephony.ims.stub.ImsRegistrationImplBase.REGISTRATION_TECH_CROSS_SIM;
import static android.telephony.ims.stub.ImsRegistrationImplBase.REGISTRATION_TECH_IWLAN;
import static com.qti.extphone.ExtTelephonyManager.FEATURE_SMART_TEMP_DDS_VIA_RADIO_CONFIG;

import android.annotation.NonNull;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.RemoteException;
import android.provider.Settings;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.data.AutoDataSwitchController;
import com.android.internal.telephony.data.PhoneSwitcher;
import com.android.internal.telephony.data.TelephonyNetworkRequest;
import com.android.internal.telephony.flags.FeatureFlags;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.nano.TelephonyProto.TelephonyEvent.DataSwitch;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.subscription.SubscriptionManagerService;

import com.qti.extphone.Client;
import com.qti.extphone.DualDataRecommendation;
import com.qti.extphone.ExtPhoneCallbackListener;
import com.qti.extphone.ExtTelephonyManager;
import com.qti.extphone.IExtPhoneCallback;
import com.qti.extphone.ServiceCallback;
import com.qti.extphone.Status;
import com.qti.extphone.Token;
import com.qualcomm.qcrilhook.QcRilHook;
import com.qualcomm.qti.internal.telephony.QtiRilInterface;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class QtiPhoneSwitcher extends PhoneSwitcher {

    private static final String DUAL_DATA_PREFERENCE = "dual_data_preference";
    private static final Uri DUAL_DATA_USER_PREFERENCE = Settings
            .Global.getUriFor(DUAL_DATA_PREFERENCE);
    //Enable dual data user preference by default.
    private static final int DEFAULT_DUAL_DATA_USER_PREFERENCE = 1;
    private int mDualDataUserPreference = -1;

    private Context mContext;
    private QtiRilInterface mQtiRilInterface;

    private static final int SETTING_VALUE_OFF = 0;
    private static final int USER_INITIATED_SWITCH = 0;
    private static final int NONUSER_INITIATED_SWITCH = 1;
    private static final int DEFAULT_PHONE_INDEX = 0;
    private static final int RECONNECT_EXT_TELEPHONY_SERVICE_DELAY_MILLISECOND = 2000;

    // Event definition for smart dds switch evalution with bad RF condition.
    private static final int EVENT_DATA_REG_STATE_CHANGED = 200;

    private Client mClient;
    private final RegistrantList mDualDataRecommendationRegistrants;

    private ExtTelephonyManager mExtTelephonyManager;
    protected final ExtPhoneCallbackListener mExtPhoneCallbackListener;

    /** Indicates whether Smart DDS switch feature is available on the device.
        In the absense of Smart DDS Switch feature, we fallback to auto data switch mechanism. */
    private boolean mIsSmartDdsSwitchFeatureAvailable;

    private boolean mNonDdsInternetNotAllowed = true;
    private ConcurrentHashMap<Integer, Boolean> mPendingDdsSwitch = new ConcurrentHashMap<>();
    private boolean mPendingBroadcastDualDataRecommendation = false;
    private DualDataRecommendation mDualDataRecommendation;

    private SmartTempDdsSwitchController mSmartTempDdsSwitchController;
    private boolean mIsUsingRadioConfigForSmartTempDdsSwitch;

    public QtiPhoneSwitcher(int maxActivePhones, Context context, Looper looper,
            @NonNull FeatureFlags featureFlags) {
        super (maxActivePhones, context, looper, featureFlags);
        mContext = context;
        mQtiRilInterface = QtiRilInterface.getInstance(context);
        mQtiRilInterface.registerForUnsol(this, EVENT_UNSOL_MAX_DATA_ALLOWED_CHANGED, null);
        mDualDataRecommendationRegistrants = new RegistrantList();
        mExtPhoneCallbackListener = new QtiPhoneSwitcherExtPhoneCallbackListener(looper);
        mExtTelephonyManager = ExtTelephonyManager.getInstance(mContext);
        mExtTelephonyManager.connectService(mServiceCallback);
        registerForDualDataPreferenceChange(looper);
        mSmartTempDdsSwitchController = SmartTempDdsSwitchController.getInstance(context, looper);
    }

    public static QtiPhoneSwitcher make(int maxActivePhones, Context context, Looper looper,
            FeatureFlags featureFlags) {
        if (sPhoneSwitcher == null) {
            sPhoneSwitcher = new QtiPhoneSwitcher(maxActivePhones, context, looper, featureFlags);
        }

        return (QtiPhoneSwitcher)sPhoneSwitcher;
    }

    /**
     * If dual data recommendation comes from RIL, registrants will be notified.
     */
    public void registerForDualDataRecommendation(Handler h, int what, Object o) {
        Registrant r = new Registrant(h, what, o);
        mDualDataRecommendationRegistrants.add(r);
    }

    public void unregisterForDualDataRecommendation(Handler h) {
        mDualDataRecommendationRegistrants.remove(h);
    }

    public void unregisterForActivePhoneSwitch(Handler h) {
        mActivePhoneRegistrants.remove(h);
    }

    private boolean isDdsSwitchPending() {
        for (Boolean value: mPendingDdsSwitch.values()) {
            if (value.booleanValue()) return true;
        }
        return false;
    }

    private void updateAndNotify(DualDataRecommendation rec) {
        mDualDataRecommendation = rec;
        if (mDualDataRecommendation.getRecommendedSub() == DualDataRecommendation.NON_DDS) {
            mNonDdsInternetNotAllowed = (mDualDataRecommendation.getAction()
                    == DualDataRecommendation. ACTION_DATA_ALLOW) ? false : true;
        }
        if (isDdsSwitchPending()) {
            logl("Pending DDS switch. Can not notify DualDataRecommendation: " +
                    mDualDataRecommendation);
            mPendingBroadcastDualDataRecommendation = true;
            return;
        }
        logl("notify DualDataRecommendation: " + mDualDataRecommendation);
        mDualDataRecommendationRegistrants.notifyRegistrants(
                new AsyncResult(null, mDualDataRecommendation, null));
    }

    private void sendDualDataUserPreference() {
        if (mExtTelephonyManager == null || !mExtTelephonyManager.getDualDataCapability()) {
            return;
        }

        mDualDataUserPreference = Settings.Global.getInt(mContext.getContentResolver(),
                DUAL_DATA_PREFERENCE, DEFAULT_DUAL_DATA_USER_PREFERENCE);
        try {
            mExtTelephonyManager.setDualDataUserPreference(mClient,
                    mDualDataUserPreference == 1 ? true : false);
        } catch (Exception e) {
            Rlog.e(LOG_TAG, "Exception: ", e);
        }
    }

    private ServiceCallback mServiceCallback = new ServiceCallback() {
        @Override
        public void onConnected() {
            // KEYSTONE(I08ff87bd8eced5a9df3c80fc1a7ef75dd59300fd,b/257537199)
            logl("ExtTelephony Service connected");
            int[] events = new int[] {
                    ExtPhoneCallbackListener.EVENT_ON_DDS_SWITCH_CRITERIA_CHANGE,
                    ExtPhoneCallbackListener.EVENT_ON_DDS_SWITCH_RECOMMENDATION,
                    ExtPhoneCallbackListener.EVENT_ON_DDS_SWITCH_CONFIG_RECOMMENDATION,
                    ExtPhoneCallbackListener.EVENT_ON_DUAL_DATA_CAPABILITY_CHANGED,
                    ExtPhoneCallbackListener.EVENT_ON_DUAL_DATA_RECOMMENDATION};
            mClient = mExtTelephonyManager.registerCallbackWithEvents(
                    mContext.getPackageName(), mExtPhoneCallbackListener, events);
            logl("Client = " + mClient);
            try {
                mIsUsingRadioConfigForSmartTempDdsSwitch = mExtTelephonyManager
                        .isFeatureSupported(FEATURE_SMART_TEMP_DDS_VIA_RADIO_CONFIG);
                mIsSmartDdsSwitchFeatureAvailable =
                        mExtTelephonyManager.isSmartDdsSwitchFeatureAvailable();
                logl("mIsSmartDdsSwitchFeatureAvailable: "
                        + mIsSmartDdsSwitchFeatureAvailable
                        + ", mIsUsingRadioConfigForSmartTempDdsSwitch: "
                        + mIsUsingRadioConfigForSmartTempDdsSwitch);
            } catch (RemoteException ex) {
                logl("isSmartDdsSwitchFeatureAvailable exception " + ex);
            }
            // If Smart DDS feature is not available, evaluate auto data switch
            if (!mIsSmartDdsSwitchFeatureAvailable) {
                // While reconnecting, evaluate it by reason registration state changed.
                mAutoDataSwitchController.evaluateAutoDataSwitch(
                        AutoDataSwitchController.EVALUATION_REASON_REGISTRATION_STATE_CHANGED);
            }
            sendDualDataUserPreference();
        }
        @Override
        public void onDisconnected() {
            logl("ExtTelephony Service disconnected...");
            mExtTelephonyManager.unregisterCallback(mExtPhoneCallbackListener);
            mClient = null;
            Message msg = obtainMessage(EVENT_RECONNECT_EXT_TELEPHONY_SERVICE);
            sendMessageDelayed(msg, RECONNECT_EXT_TELEPHONY_SERVICE_DELAY_MILLISECOND);
        }
    };

    protected class QtiPhoneSwitcherExtPhoneCallbackListener extends ExtPhoneCallbackListener {
        public QtiPhoneSwitcherExtPhoneCallbackListener(Looper looper) {
            super(looper);
        }

        @Override
        public void onDdsSwitchCriteriaChange(int slotId,
                boolean telephonyDdsSwitch) throws RemoteException {
            logl("ExtPhoneCallback: onDdsSwitchCriteriaChange: " + telephonyDdsSwitch +
                    " slotId: " + slotId);
            if (!SubscriptionManager.isValidPhoneId(slotId)) {
                return;
            }
            Phone phone = PhoneFactory.getPhone(slotId);
            if (phone != null) {
                // Whether telephony temporary DDS feature is enabled or not is relying
                // on the data during call setting.
                boolean isTelTempDdsEnabled = telephonyDdsSwitch
                        && phone.getDataSettingsManager().isMobileDataPolicyEnabled(
                        TelephonyManager
                        .MOBILE_DATA_POLICY_DATA_ON_NON_DEFAULT_DURING_VOICE_CALL)
                        && isTelTempDdsSwitchSatisfiedWithDdsSubSituation();
                logl("Enable telephony temp DDS " + isTelTempDdsEnabled + " on slot " + slotId);
                phone.setTelephonyTempDdsSwitch(isTelTempDdsEnabled);
            }
        }

        @Override
        public void onDdsSwitchRecommendation(int slotId,
                int recommendedSlotId) throws RemoteException {
            logl("ExtPhoneCallback: onDdsSwitchRecommendation" +
                    ", slotId: " + slotId +
                    ", recommendedSlotId: " + recommendedSlotId);
            onDdsSwitchConfigRecommendation(recommendedSlotId);
        }

        @Override
        public void onDdsSwitchConfigRecommendation(int recommendedSlotId) throws RemoteException {
            logl("ExtPhoneCallback: onDdsSwitchConfigRecommendation" +
                    ", recommendedSlotId: " + recommendedSlotId);
            if (!SubscriptionManager.isValidPhoneId(recommendedSlotId)) {
                return;
            }
            if (getPrimaryDataPhoneId() == recommendedSlotId) {
                mPhoneIdInVoiceCall = SubscriptionManager.INVALID_PHONE_INDEX;
                if (mEmergencyOverride != null) {
                    logl("Precise call state simulates");
                    sendMessage(obtainMessage(EVENT_PRECISE_CALL_STATE_CHANGED));
                }
            } else {
                if (!isTempDdsSwitchForReccomendationAllowed(recommendedSlotId)) {
                    logl("Temp DDS switch for recommendation is skipped");
                    return;
                }
                mPhoneIdInVoiceCall = recommendedSlotId;
            }
            evaluateIfImmediateDataSwitchIsNeeded("recommendation",
                    DataSwitch.Reason.DATA_SWITCH_REASON_IN_CALL);
        }

        @Override
        public void onDualDataCapabilityChanged(Token token, Status status, boolean support)
                throws RemoteException {
            logl("onDualDataCapabilityChanged :  " + support);
            if (support) {
                sendDualDataUserPreference();
            }
        }

        @Override
        public void setDualDataUserPreferenceResponse(Token token, Status status)
                throws RemoteException {
            logl("setDualDataUserPreferenceResponse: token = " +
                    token + " Status = " + status);
            if (status.get() == Status.SUCCESS) {
                Settings.Global.putInt(mContext.getContentResolver(),
                        DUAL_DATA_PREFERENCE, mDualDataUserPreference);
            }
        }

        @Override
        public void onDualDataRecommendation(DualDataRecommendation rec) {
            logl("ExtPhoneCallback: onDualDataRecommendation " + rec);
            updateAndNotify(rec);
        }
    };

    private int getPhoneCount() {
        TelephonyManager tm = (TelephonyManager) mContext.
                getSystemService(Context.TELEPHONY_SERVICE);
        return tm.getActiveModemCount();
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
                logl("getNonDdsPhoneId: " + phoneId);
                return phoneId;
            }
        }
        return SubscriptionManager.INVALID_PHONE_INDEX;
    }

    private boolean isTempDdsSwitchForReccomendationAllowed(int recommendedSlotId) {
        Phone nDdsPhone = PhoneFactory.getPhone(getNonDdsPhoneId());
        boolean isEnabled = false;

        if (nDdsPhone != null) {
            isEnabled = nDdsPhone.getDataSettingsManager().isMobileDataPolicyEnabled(
                    TelephonyManager.MOBILE_DATA_POLICY_DATA_ON_NON_DEFAULT_DURING_VOICE_CALL);
        }

        logl("isTempDdsSwitchForReccomendationAllowed : User preferred DataDuringCall value is: "
                + isEnabled + " on NonDdsPhoneId: " + getNonDdsPhoneId()
                + " and the recommendedSlotId = " + recommendedSlotId);

        return isEnabled;
    }

    /*
     * Whether there is a case that modem actively enables telephony temp DDS switch
     * but Telephony unwants? It is like, Telephony enables modem data during call,
     * modem wants Telephony temp DDS switch work.
     * To continuously consider this case, need to align with Telephony modem data during
     * call evaluation.
     */
    private boolean isTelTempDdsSwitchSatisfiedWithDdsSubSituation() {
        Phone phone = PhoneFactory.getPhone(getPrimaryDataPhoneId());
        if (phone != null) {
            boolean isDataRoaming = phone.getServiceState().getDataRoaming();
            boolean isRoamingDataEnabled = phone.getDataRoamingEnabled();
            boolean isDataEnabled = phone.getDataSettingsManager()
                    .isDataEnabledForReason(TelephonyManager.DATA_ENABLED_REASON_USER);
            logl("DDS SUB: isDataEnabled = " + isDataEnabled + " isDataRoaming = " + isDataRoaming
                    + " isRoamingDataEnabled = " + isRoamingDataEnabled);
            // If Telephony supresses data during call sliently,
            // Telephony temp DDS switch shouldn't work also.
            if (!isDataEnabled || (isDataRoaming && !isRoamingDataEnabled)) {
                return false;
            }
        }
        return true;
    }

    private void queryMaxDataAllowed() {
        mMaxDataAttachModemCount = mQtiRilInterface.getMaxDataAllowed();
    }

    private void handleUnsolMaxDataAllowedChange(Message msg) {
        if (msg == null ||  msg.obj == null) {
            logl("Null data received in handleUnsolMaxDataAllowedChange");
            return;
        }
        ByteBuffer payload = ByteBuffer.wrap((byte[])msg.obj);
        payload.order(ByteOrder.nativeOrder());
        int rspId = payload.getInt();
        if ((rspId == QcRilHook.QCRILHOOK_UNSOL_MAX_DATA_ALLOWED_CHANGED)) {
            int response_size = payload.getInt();
            if (response_size < 0 ) {
                logl("Response size is Invalid " + response_size);
                return;
            }
            mMaxDataAttachModemCount = payload.get();
            logl(" Unsol Max Data Changed to: " + mMaxDataAttachModemCount);
        }
    }

    @Override
    public void handleMessage(Message msg) {
        logl("handle event - " + msg.what);
        AsyncResult ar = null;
        switch (msg.what) {
            case EVENT_RADIO_ON: {
                if (mQtiRilInterface.isServiceReady()) {
                    queryMaxDataAllowed();
                } else {
                    logl("Oem hook service is not ready");
                }
                sendDualDataUserPreference();
                super.handleMessage(msg);
                break;
            }
            case EVENT_UNSOL_MAX_DATA_ALLOWED_CHANGED: {
                org.codeaurora.telephony.utils.AsyncResult asyncresult =
                        (org.codeaurora.telephony.utils.AsyncResult) msg.obj;
                if (asyncresult.result != null) {
                    handleUnsolMaxDataAllowedChange((Message) asyncresult.result);
                } else {
                    logl("Error: empty result, EVENT_UNSOL_MAX_DATA_ALLOWED_CHANGED");
                }
                break;
            }
            case EVENT_RECONNECT_EXT_TELEPHONY_SERVICE: {
                logl("EVENT_RECONNECT_EXT_TELEPHONY_SERVICE");
                mExtTelephonyManager.connectService(mServiceCallback);
                break;
            }
            case EVENT_DATA_REG_STATE_CHANGED: {
                onEvaluate(REQUESTS_UNCHANGED, "EVENT_DATA_REG_STATE_CHANGED");
                break;
            }
            case EVENT_IMS_RADIO_TECH_CHANGED: {
                // register for radio tech change to listen to radio tech handover in case previous
                // attempt was not successful
                if (!mFlags.changeMethodOfObtainingImsRegistrationRadioTech()) {
                    registerForImsRadioTechChange();
                } else {
                    if (msg.obj == null) {
                        logl("EVENT_IMS_RADIO_TECH_CHANGED but parameter is not available");
                        break;
                    }
                    if (!onImsRadioTechChanged((AsyncResult) (msg.obj))) {
                        break;
                    }
                }

                // When smart temp DDS switch is honored, evaluating data phone usage isn't
                // needed for IMS radio tech changed.
                if (isTelephonyTempDdsSwitchEnabled()) {
                    // if voice call state changes or in voice call didn't change
                    // but RAT changes(e.g. Iwlan -> cross sim), reevaluate for data switch.
                    if (updatesIfPhoneInVoiceCallChanged() || isAnyVoiceCallActiveOnDevice()) {
                        evaluateIfImmediateDataSwitchIsNeeded("Ims radio tech changed",
                                DataSwitch.Reason.DATA_SWITCH_REASON_IN_CALL);
                    }
                }
                break;
            }
            default:
                super.handleMessage(msg);
        }
    }

    private int getPrimaryDataPhoneId() {
        int primaryDataPhoneId = SubscriptionManager.INVALID_PHONE_INDEX;
        if (SubscriptionManager.isValidSubscriptionId(mPrimaryDataSubId)) {
            primaryDataPhoneId =
                    SubscriptionManagerService.getInstance().getPhoneId(mPrimaryDataSubId);
        }
        return primaryDataPhoneId;
    }

    @Override
    protected boolean onEvaluate(boolean requestsChanged, String reason) {
        if (!updateHalCommandToUse()) {
            logl("Wait for HAL command update");
            return false;
        }

        return super.onEvaluate(requestsChanged, reason);
    }

    @Override
    protected void sendRilCommands(int phoneId) {
        if (!updateHalCommandToUse()) {
            logl("Wait for HAL command update");
            return;
        }

        super.sendRilCommands(phoneId);
        if (SubscriptionManager.isValidPhoneId(phoneId)
                && phoneId == mPreferredDataPhoneId
                && (mHalCommandToUse == HAL_COMMAND_PREFERRED_DATA)) {
            logl("DDS switch starts");
            mPendingDdsSwitch.put(phoneId, true);
        }
    }

    @Override
    protected void onDdsSwitchResponse(AsyncResult ar) {
        boolean commandSuccess = ar != null && ar.exception == null;
        int phoneId = (int) ar.userObj;
        if (!SubscriptionManager.isValidPhoneId(phoneId)) {
            logl("onDdsSwitchResponse: Invalid phoneId = " + phoneId);
            return;
        }
        if (mEmergencyOverride != null) {
            logl("Emergency override result sent = " + commandSuccess);
            mEmergencyOverride.sendOverrideCompleteCallbackResultAndClear(commandSuccess);
            // Do not retry , as we do not allow changes in onEvaluate during an emergency
            // call. When the call ends, we will start the countdown to remove the override.
        } else if (!commandSuccess) {
            logl("onDdsSwitchResponse: DDS switch failed. with exception " + ar.exception);
            //Remove this check once these errors are passed through dds hal response
            if (isAnyVoiceCallActiveOnDevice()) {
                mCurrentDdsSwitchFailure.get(phoneId).add(CommandException.Error.
                        OP_NOT_ALLOWED_DURING_VOICE_CALL);
                logl("onDdsSwitchResponse: Wait for call end indication");
                return;
            } else if (!isSimApplicationReady(phoneId)) {
               /* If there is a attach failure due to sim not ready then
                  hold the retry until sim gets ready */
                mCurrentDdsSwitchFailure.get(phoneId).add(CommandException.Error.INVALID_SIM_STATE);
                logl("onDdsSwitchResponse: Wait for SIM to get READY");
                return;
            }
            logl("onDdsSwitchResponse: Scheduling DDS switch retry");
            sendMessageDelayed(Message.obtain(this, EVENT_MODEM_COMMAND_RETRY,
                    phoneId), MODEM_COMMAND_RETRY_PERIOD_MS);
            return;
        }
        if (commandSuccess) {
            logl("onDdsSwitchResponse: DDS switch success on phoneId = " + phoneId);
            mAutoDataSwitchController.displayAutoDataSwitchNotification(phoneId,
                    mLastSwitchPreferredDataReason == DataSwitch.Reason.DATA_SWITCH_REASON_AUTO);
        }
        mCurrentDdsSwitchFailure.get(phoneId).clear();
        // Notify all registrants
        mActivePhoneRegistrants.notifyRegistrants();

        mPendingDdsSwitch.clear();
        if (mPendingBroadcastDualDataRecommendation && !isDdsSwitchPending()) {
            logl("notify DualDataRecommendation: " + mDualDataRecommendation);
            mDualDataRecommendationRegistrants.notifyRegistrants(
                    new AsyncResult(null, mDualDataRecommendation, null));
            mPendingBroadcastDualDataRecommendation = false;
        }
        notifyPreferredDataSubIdChanged();
        mPhoneSwitcherCallbacks.forEach(callback -> callback.invokeFromExecutor(
                () -> callback.onPreferredDataPhoneIdChanged(phoneId)));
    }

    private boolean isSmartTempDdsSwitchSupported() {
        if (isUsingRadioConfigForSmartTempDds()) {
            return mSmartTempDdsSwitchController.isSmartTempDdsSwitchCapable();
        }
        for (int i = 0; i < mActiveModemCount; i++) {
            if (!PhoneFactory.getPhone(i).getSmartTempDdsSwitchSupported()) return false;
        }
        return true;
    }

    private boolean updateHalCommandToUse() {
        if (mHalCommandToUse == HAL_COMMAND_UNKNOWN) {
            boolean isRadioAvailable = true;
            for (int i = 0; i < mActiveModemCount; i++) {
                isRadioAvailable &= PhoneFactory.getPhone(i).mCi.getRadioState()
                                != RADIO_POWER_UNAVAILABLE;
            }
            if (isRadioAvailable) {
                logl("update HAL command");
                mHalCommandToUse = mRadioConfig.isSetPreferredDataCommandSupported()
                        ? HAL_COMMAND_PREFERRED_DATA : HAL_COMMAND_ALLOW_DATA;
                return true;
            } else {
                logl("radio is unavailable");
            }
        }
        return mHalCommandToUse != HAL_COMMAND_UNKNOWN;
    }

    @Override
    protected boolean isPhoneInVoiceCall(Phone phone) {
        if (phone ==  null) {
            return false;
        }
        // A phone in voice call might trigger data being switched to it.
        // We only report true if its precise call state is ACTIVE, ALERTING or HOLDING,
        // INCOMING, WAITING and DISCONNECTING.
        // The reason is data switching is interrupting, so we only switch when necessary and
        // acknowledged by the users
        // For outgoing call we don't switch until call is connected in network
        // (DIALING -> ALERTING).
        Call.State foregroundCallState = phone.getForegroundCall().getState();
        return (!phone.getBackgroundCall().isIdle()
                || foregroundCallState == Call.State.ACTIVE
                || foregroundCallState == Call.State.ALERTING
                || foregroundCallState == Call.State.DISCONNECTING
                || phone.getRingingCall().isRinging()
                || phone.getRingingCall().getState() == Call.State.DISCONNECTING);
    }

    @Override
    protected int phoneIdForRequest(TelephonyNetworkRequest networkRequest) {
        NetworkRequest netRequest = networkRequest.getNativeNetworkRequest();
        int subId = getSubIdFromNetworkSpecifier(netRequest.getNetworkSpecifier());

        if (subId == DEFAULT_SUBSCRIPTION_ID) return mPreferredDataPhoneId;
        if (subId == INVALID_SUBSCRIPTION_ID) return INVALID_PHONE_INDEX;

        int preferredDataSubId = (mPreferredDataPhoneId >= 0
                && mPreferredDataPhoneId < mActiveModemCount)
                ? mPhoneSubscriptions[mPreferredDataPhoneId] : INVALID_SUBSCRIPTION_ID;

        // Allow Internet PDN connections on both SIMs simultaneously when device is supported.
        // Some multi-SIM devices will only support one Internet PDN connection. So
        // If Internet PDN is established on the non-preferred phone, it will interrupt
        // Internet connection on the preferred phone. So we only accept Internet request with
        // preferred data subscription or no specified subscription.
        // One exception is, if it's restricted request (doesn't have NET_CAPABILITY_NOT_RESTRICTED)
        // it will be accepted, which is used temporary data usage from system.
        if (mNonDdsInternetNotAllowed
                && netRequest.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && netRequest.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                && subId != preferredDataSubId && subId != mValidator.getSubIdInValidation()) {
            // Returning INVALID_PHONE_INDEX will result in netRequest not being handled.
            return INVALID_PHONE_INDEX;
        }

        // Try to find matching phone ID. If it doesn't exist, we'll end up returning INVALID.
        int phoneId = INVALID_PHONE_INDEX;
        for (int i = 0; i < mActiveModemCount; i++) {
            if (mPhoneSubscriptions[i] == subId) {
                phoneId = i;
                break;
            }
        }
        return phoneId;
    }

    //Register for a change in dual data UI preference. When it is toggled off,
    //notify clients that internet pdn is not allowed on nDDS.
    private void registerForDualDataPreferenceChange(Looper looper) {
        mContext.getContentResolver().registerContentObserver(DUAL_DATA_USER_PREFERENCE, false,
                new ContentObserver(new Handler(looper)) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                if (DUAL_DATA_USER_PREFERENCE.equals(uri)) {
                    boolean userSelection = Settings.Global.getInt(mContext.getContentResolver(),
                            DUAL_DATA_PREFERENCE, SETTING_VALUE_OFF) == 1 ? true : false;
                    logl("Dual Data Preference UI changed: " + userSelection);
                    if (!userSelection) {
                        DualDataRecommendation rec = new DualDataRecommendation(
                                DualDataRecommendation.NON_DDS,
                                DualDataRecommendation.ACTION_DATA_NOT_ALLOW);
                        updateAndNotify(rec);
                    }
                }
            }
        });
    }

    @Override
    protected void updatePreferredDataPhoneId() {
        if (mEmergencyOverride != null && findPhoneById(mEmergencyOverride.mPhoneId) != null) {
            super.updatePreferredDataPhoneId();
        } else {
            if (isAnyVoiceCallActiveOnDevice()) {
                // When call is ongoing, keep watching if temp DDS is needed
                final int imsRegTech = mImsRegTechProvider.get(mContext, mPhoneIdInVoiceCall);
                if (imsRegTech != REGISTRATION_TECH_IWLAN
                        && imsRegTech != REGISTRATION_TECH_CROSS_SIM) {
                    internalUpdatePreferredDataPhoneId();
                } else {
                    // Do nothings because DDS can't be revoked till call ends
                    // after going out cellular voice call.
                    logl("IMS call on IWLAN or cross SIM. Call will be ignored for DDS switch");
                }
            } else {
                // After call ends, revokes DDS actively.
                mPreferredDataPhoneId = getFallbackDataPhoneIdForInternetRequests();
            }
            logl("updatePreferredDataPhoneId mPreferredDataPhoneId " + mPreferredDataPhoneId);
            mPreferredDataSubId.set(SubscriptionManager.getSubscriptionId(mPreferredDataPhoneId));
        }
    }

    private void internalUpdatePreferredDataPhoneId() {
        // Suppose temp DDS happens already, DDS can't be revoked till call ends.
        if (mPhoneIdInVoiceCall != mPreferredDataPhoneId) {
            boolean isAllowed = true;
            Phone preferredDataPhone = findPhoneById(mPreferredDataPhoneId);
            if (preferredDataPhone != null
                    && SubscriptionManager.isValidSubscriptionId(preferredDataPhone.getSubId())) {
                // Check if the primary data phone has overall data enabled
                isAllowed = preferredDataPhone.getDataSettingsManager().isDataEnabled();
                logl("DDS mobile data check: allowed " + isAllowed);
                // when DDS is in roaming, and data roaming for DDS is turned off,
                if (preferredDataPhone.getServiceState().getDataRoaming()) {
                     isAllowed = isAllowed && preferredDataPhone.getDataRoamingEnabled();
                     logl("DDS roaming data check: allowed " + isAllowed);
                }

            }
            // Check if data is allowed during a call on non-DDS
            Phone voicePhone = findPhoneById(mPhoneIdInVoiceCall);
            if (isAllowed && voicePhone != null) {
                // Check if voice phone is satisfied to establish internet PDN.
                // This will consider both 'Data during calls' and 'Auto data switch'
                // during evaluation
                isAllowed = isAllowed && voicePhone.isDataAllowed();
                logl("internalUpdatePreferredDataPhoneId: internet pdn is allowed " + isAllowed);
            }

            if (isAllowed) {
                mPreferredDataPhoneId = mPhoneIdInVoiceCall;
            }
        }
    }

    public boolean hasTempDdsSwitched() {
        return mPrimaryDataSubId != getActiveDataSubId();
    }

    @Override
    protected boolean isTelephonyTempDdsSwitchEnabled() {
        if (isUsingRadioConfigForSmartTempDds()) {
            boolean isTelephonyTempDdsSwitchEnabled =
                    ((mSmartTempDdsSwitchController.isSmartTempDdsSwitchCapable()
                    || !mSmartTempDdsSwitchController.isDataAllowedDuringCall())
                    ? false : true );
            logl("isTelephonyTempDdsSwitchEnabled() = " + isTelephonyTempDdsSwitchEnabled);
            return isTelephonyTempDdsSwitchEnabled;
        }

        for (int i = 0; i < mActiveModemCount; i++) {
            Phone phone = PhoneFactory.getPhone(i);
            if(phone == null) {
                continue;
            }
            int subId = phone.getSubId();
            if (SubscriptionManager.isValidSubscriptionId(subId)
                    && subId != mPrimaryDataSubId && !getTelephonyTempDdsSwitch()) {
                logl("isTelephonyTempDdsSwitchEnabled() : " + false);
                return false;
            }
        }
        logl("isTelephonyTempDdsSwitchEnabled() : " + true);
        return true;
    }

    private boolean getTelephonyTempDdsSwitch() {
        Phone nDdsPhone = PhoneFactory.getPhone(getNonDdsPhoneId());

        boolean isDataDuringCallEnabled =
                nDdsPhone.getDataSettingsManager().isMobileDataPolicyEnabled(
                TelephonyManager.MOBILE_DATA_POLICY_DATA_ON_NON_DEFAULT_DURING_VOICE_CALL);

        boolean isTelTempDdsSwitchSatisfiedWithDdsSubSituation =
                isTelTempDdsSwitchSatisfiedWithDdsSubSituation();

        boolean isTelephonyTempDdsSwitchAllowed = nDdsPhone.getTelephonyTempDdsSwitch()
                && isDataDuringCallEnabled && isTelTempDdsSwitchSatisfiedWithDdsSubSituation;

        logl("getTelephonyTempDdsSwitch() = " + isTelephonyTempDdsSwitchAllowed
                + ", isDataDuringCallEnabled = " + isDataDuringCallEnabled
                + ", isTelTempDdsSwitchSatisfiedWithDdsSubSituation = "
                + isTelTempDdsSwitchSatisfiedWithDdsSubSituation);

        return isTelephonyTempDdsSwitchAllowed;
    }

    private boolean isUsingRadioConfigForSmartTempDds() {
        return mIsUsingRadioConfigForSmartTempDdsSwitch
                && mSmartTempDdsSwitchController != null;
    }

    public void sendDataRegStateChanged() {
        obtainMessage(EVENT_DATA_REG_STATE_CHANGED).sendToTarget();
    }

    @Override
    protected void onDataEnabledChanged() {
         logl("QTI : onDataEnabledChanged");
         if (!isUsingRadioConfigForSmartTempDds()) {
             super.onDataEnabledChanged();
         } else {
             evaluateTelephonyTempDdsIfRequried("user changed data settings during call",
                     DataSwitch.Reason.DATA_SWITCH_REASON_IN_CALL);
         }
    }
}
