/*
 * Copyright (c) 2016, 2019-2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 * Not a Contribution, Apache license notifications and license are retained
 * for attribution purposes only.
 *
 * Copyright (C) 2006 The Android Open Source Project
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


package com.qualcomm.qti.internal.telephony;

import static android.telephony.TelephonyManager.HAL_SERVICE_MODEM;

import static com.android.internal.telephony.RIL.RADIO_HAL_VERSION_2_2;

import android.annotation.NonNull;
import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.ImsiEncryptionInfo;
import android.telephony.NetworkScanRequest;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.flags.FeatureFlags;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneNotifier;
import com.android.internal.telephony.RadioCapability;
import com.android.internal.telephony.RIL;
import com.android.internal.telephony.RILConstants;
import com.android.internal.telephony.TelephonyComponentFactory;
import com.android.internal.telephony.CarrierInfoManager;
import com.android.internal.telephony.subscription.SubscriptionInfoInternal;
import com.android.internal.telephony.uicc.IccCardStatus;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccPort;
import com.android.internal.telephony.uicc.UiccSlot;

import com.qti.extphone.Client;
import com.qti.extphone.ExtTelephonyManager;
import com.qti.extphone.ExtPhoneCallbackListener;
import com.qti.extphone.IExtPhoneCallback;
import com.qti.extphone.NetworkSelectionMode;
import com.qti.extphone.QtiImeiInfo;
import com.qti.extphone.ServiceCallback;
import com.qti.extphone.Status;
import com.qti.extphone.Token;

import java.util.function.Consumer;
import java.util.HashMap;

public class QtiGsmCdmaPhone extends GsmCdmaPhone {
    private static final String LOG_TAG = "QtiGsmCdmaPhone";
    private static final int PROP_EVENT_START = EVENT_LAST;
    private static final int EVENT_RESET_CARRIER_KEY_IMSI_ENCRYPTION = PROP_EVENT_START + 1;
    private static final int EVENT_ESSENTIAL_SIM_RECORDS_LOADED      = PROP_EVENT_START + 2;
    private static final int EVENT_QUERY_VONR_ENABLED_DONE           = PROP_EVENT_START + 3;
    private static final int EVENT_IMEI_TYPE_CHANGED                 = PROP_EVENT_START + 4;

    private BaseRilInterface mQtiRilInterface;
    private int imsiToken = 0;
    private Context mContext;
    private Client mClient;
    private TelephonyManager mTelephonyManager;
    private ExtTelephonyManager mExtTelephonyManager;
    private boolean mIsInSecureMode = false;
    private Object mLock = new Object();
    private final HashMap<Integer, Message> mPendingRequests = new HashMap<>();
    private long mCiwlanTimer = -1;

    public QtiGsmCdmaPhone(Context context,
            CommandsInterface ci, PhoneNotifier notifier, int phoneId,
            int precisePhoneType, TelephonyComponentFactory telephonyComponentFactory,
            @NonNull FeatureFlags featureFlags) {
        this(context, ci, notifier, false, phoneId, precisePhoneType,
                telephonyComponentFactory, featureFlags);
    }

    public QtiGsmCdmaPhone(Context context,
            CommandsInterface ci, PhoneNotifier notifier, boolean unitTestMode, int phoneId,
            int precisePhoneType, TelephonyComponentFactory telephonyComponentFactory,
            @NonNull FeatureFlags featureFlags) {
        super(context, ci, notifier, unitTestMode, phoneId, precisePhoneType,
                telephonyComponentFactory, featureFlags);
        mContext = context;
        logd("Constructor");
        mQtiRilInterface = getQtiRilInterface();
        mCi.registerForCarrierInfoForImsiEncryption(this,
                EVENT_RESET_CARRIER_KEY_IMSI_ENCRYPTION, null);
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class);
        mExtTelephonyManager = ExtTelephonyManager.getInstance(mContext);
        mExtTelephonyManager.connectService(mServiceCallback);
    }

    private ServiceCallback mServiceCallback = new ServiceCallback() {
        @Override
        public void onConnected() {
            logd("ExtTelephonyService connected");
            int[] events = new int[] {
                    ExtPhoneCallbackListener.EVENT_ON_SECURE_MODE_STATUS_CHANGE,
                    ExtPhoneCallbackListener.EVENT_ON_IMEI_TYPE_CHANGED};
            mClient = mExtTelephonyManager.registerCallbackWithEvents(
                    mContext.getPackageName(), mExtPhoneCallbackListener, events);
            logd("mClient = " + mClient);
            try {
                mExtTelephonyManager.getSecureModeStatus(mClient);
            } catch (RemoteException ex) {
                loge("getSecureModeStatus Exception " + ex);
            }
        }

        @Override
        public void onDisconnected() {
            logd("ExtTelephonyService disconnected. mClient = " + mClient);
            mExtTelephonyManager.unregisterCallback(mExtPhoneCallbackListener);
            mClient = null;
        }
    };

    private ExtPhoneCallbackListener mExtPhoneCallbackListener = new ExtPhoneCallbackListener() {
        @Override
        public void onSecureModeStatusChange(boolean enabled) throws RemoteException {
            logd("ExtPhoneCallback: onSecureModeStatusChange enabled: " + enabled);
            mIsInSecureMode = enabled;
        }

        @Override
        public void getSecureModeStatusResponse(Token token, Status status, boolean enableStatus) {
            logd("ExtPhoneCallback: getSecureModeStatusResponse enabled: " + enableStatus);
            mIsInSecureMode = enableStatus;
        }

        @Override
        public void getNetworkSelectionModeResponse(int slotId, Token token, Status status,
                NetworkSelectionMode modes) {
            logd("ExtPhoneCallback: getNetworkSelectionModeResponse");
            Message msg = null;
            synchronized (mPendingRequests) {
                msg =  mPendingRequests.get(token.get());
            }
            onCheckForNetworkSelectionModeAutomatic(slotId, msg, status, modes);
            synchronized (mPendingRequests) {
                mPendingRequests.remove(token.get());
            }
        }

        @Override
        public void onImeiTypeChanged(QtiImeiInfo[] imeiInfo) throws RemoteException {
            if (getHalVersion(HAL_SERVICE_MODEM).less(RADIO_HAL_VERSION_2_2)) {
                if (imeiInfo == null) {
                    loge("Invalid IMEI Info");
                    return;
                }
                sendMessage(obtainMessage(EVENT_IMEI_TYPE_CHANGED, imeiInfo));
            }
        }
    };

    public boolean setLocalCallHold(boolean enable) {
        if (!mQtiRilInterface.isServiceReady()) {
            loge("mQtiRilInterface is not ready yet");
            return false;
        }
        return mQtiRilInterface.setLocalCallHold(mPhoneId, enable);
    }

    @Override
    public void dispose() {
        mQtiRilInterface = null;
        super.dispose();
    }

    @Override
    public void handleMessage(Message msg) {
        logd("handleMessage: Event: " + msg.what);
        AsyncResult ar;
        switch(msg.what) {

            case EVENT_SIM_RECORDS_LOADED:
                if(isPhoneTypeGsm()) {
                    logd("notify call forward indication, phone id:" + mPhoneId);
                    notifyCallForwardingIndicator();
                }
                super.handleMessage(msg);
                break;

            case EVENT_NV_READY:{
                logd("Event EVENT_NV_READY Received");
                mSST.pollState();
                // Notify voicemails.
                logd("notifyMessageWaitingChanged");
                mNotifier.notifyMessageWaitingChanged(this);
                updateVoiceMail();
                // Do not call super.handleMessage().
                // AOSP do not handle EVENT_NV_READY.
                break;
            }

            case EVENT_RESET_CARRIER_KEY_IMSI_ENCRYPTION:
                logd("Event EVENT_RESET_CARRIER_KEY_IMSI_ENCRYPTION");
                super.resetCarrierKeysForImsiEncryption();
                break;

            case EVENT_ESSENTIAL_SIM_RECORDS_LOADED:
                logd("Event EVENT_ESSENTIAL_SIM_RECORDS_LOADED Received");
                handleEssentialRecordsLoaded();
                break;

            case EVENT_MODEM_RESET:
                Rlog.d(LOG_TAG,"Event EVENT_MODEM_RESET Received");
                ScbmHandler.getInstance().handleModemReset();
                super.handleMessage(msg);
                break;
            case EVENT_QUERY_VONR_ENABLED_DONE:
                ar = (AsyncResult)msg.obj;
                if (ar == null || ar.exception != null) {
                    break;
                }

                boolean enabled = (boolean) ar.result;
                Rlog.d(LOG_TAG,"Event EVENT_QUERY_VONR_ENABLED_DONE Received: " + enabled);
                break;

            case EVENT_IMEI_TYPE_CHANGED:
                QtiImeiInfo[] imeiInfo = (QtiImeiInfo[])msg.obj;
                for (int i = 0; i < imeiInfo.length; i++) {
                    if (imeiInfo[i] != null && imeiInfo[i].getSlotId() == mPhoneId) {
                        logd("onImeiTypeChanged: " + imeiInfo[i].toString());
                        mImei = imeiInfo[i].getImei();
                        mImeiType = imeiInfo[i].getImeiType();
                        break;
                    }
                }
                break;

            default: {
                super.handleMessage(msg);
            }

        }
    }

    private BaseRilInterface getQtiRilInterface() {
        BaseRilInterface qtiRilInterface;
        if (getUnitTestMode()) {
            logd("getQtiRilInterface, unitTestMode = true");
            qtiRilInterface = SimulatedQtiRilInterface.getInstance(mContext);
        } else {
            qtiRilInterface = QtiRilInterface.getInstance(mContext);
        }
        return qtiRilInterface;
    }


    @Override
    public void setCarrierInfoForImsiEncryption(ImsiEncryptionInfo imsiEncryptionInfo) {
        Phone phone = PhoneFactory.getPhone(mPhoneId);
        if (phone != null && phone.getHalVersion().greaterOrEqual(RIL.RADIO_HAL_VERSION_1_6)) {
            super.setCarrierInfoForImsiEncryption(imsiEncryptionInfo);
        } else {
            CarrierInfoManager.setCarrierInfoForImsiEncryption(imsiEncryptionInfo,
                    mContext, mPhoneId);
            QtiTelephonyComponentFactory.getInstance().getRil(mPhoneId)
                    .setCarrierInfoForImsiEncryption(++imsiToken, imsiEncryptionInfo);
        }
    }
    /**
     * Retrieves the full serial number of the ICC (including hex digits), if applicable.
     */
    @Override
    public String getFullIccSerialNumber() {
        String iccId = super.getFullIccSerialNumber();

        if (TextUtils.isEmpty(iccId)) {
            UiccPort port = mUiccController.getUiccPort(mPhoneId);
            iccId = (port == null) ? null : port.getIccId();
        }
        return iccId;
    }

    private void handleEssentialRecordsLoaded() {
        logd("handleEssentialRecordsLoaded, mPhoneId: " + mPhoneId);

        // Resolve the carrierId
        resolveSubscriptionCarrierId(IccCardConstants.INTENT_VALUE_ICC_LOADED);

        // Request the carrier config manager to update the config for mPhoneId
        CarrierConfigManager configManager =
                (CarrierConfigManager) mContext.getSystemService(Context.CARRIER_CONFIG_SERVICE);
        configManager.updateConfigForPhoneId(mPhoneId,
                ExtTelephonyManager.SIM_STATE_ESSENTIAL_RECORDS_LOADED);

        // Set essential records loaded state of DataNetworkController/DcTracker to true
        getDataNetworkController().setEssentialRecordsLoaded(true);
    }

    public void onCarrierConfigLoadedForEssentialRecords() {
        logd("onCarrierConfigLoadedForEssentialRecords");

        // We are here because CarrierConfigs have been fetched after essential records were loaded
        // Inform DataNetworkController/DcTracker so that data setup can begin early
        getDataNetworkController().onCarrierConfigLoadedForEssentialRecords();
    }

    @Override
    protected void registerForIccRecordEvents() {
        QtiServiceStateTracker qtiSst = (QtiServiceStateTracker) mSST;
        IccRecords r = mIccRecords.get();

        if (r != null) {
            logd("registerForEssentialRecordsLoaded");
            r.registerForEssentialRecordsLoaded(this, EVENT_ESSENTIAL_SIM_RECORDS_LOADED, null);
        }
        super.registerForIccRecordEvents();
    }

    @Override
    protected void unregisterForIccRecordEvents() {
        QtiServiceStateTracker qtiSst = (QtiServiceStateTracker) mSST;
        IccRecords r = mIccRecords.get();

        if (r != null) {
            logd("unregisterForEssentialRecordsLoaded");
            r.unregisterForEssentialRecordsLoaded(this);
        }
        super.unregisterForIccRecordEvents();
    }

    @Override
    public void setSmartTempDdsSwitchSupported(boolean smartDdsSwitch) {
        mSmartTempDdsSwitchSupported = smartDdsSwitch;
    }

    @Override
    public boolean getSmartTempDdsSwitchSupported() {
        return mSmartTempDdsSwitchSupported;
    }

    @Override
    public void setTelephonyTempDdsSwitch(boolean telephonyDdsSwitch) {
        mTelephonyTempDdsSwitch = telephonyDdsSwitch;
    }

    @Override
    public boolean getTelephonyTempDdsSwitch() {
        return mTelephonyTempDdsSwitch;
    }

    @Override
    protected void updateVoNrSettings(PersistableBundle config) {
        UiccSlot slot = mUiccController.getUiccSlotForPhone(mPhoneId);

        // If no card is present, do nothing.
        if (slot == null || slot.getCardState() != IccCardStatus.CardState.CARDSTATE_PRESENT) {
            return;
        }

        if (config == null) {
            loge("didn't get the vonr_enabled_bool from the carrier config.");
            return;
        }

        boolean mIsVonrEnabledByCarrier =
                config.getBoolean(CarrierConfigManager.KEY_VONR_ENABLED_BOOL);

        int setting = -1;
        SubscriptionInfoInternal subInfo = mSubscriptionManagerService
                .getSubscriptionInfoInternal(getSubId());
        if (subInfo != null) {
            setting = subInfo.getNrAdvancedCallingEnabled();
        }

        logd("VoNR setting from telephony.db:"
                + setting
                + " ,vonr_enabled_bool:"
                + mIsVonrEnabledByCarrier);

        // if user never changed vonr through UI option, setting will be -1, AP
        // only need to query NV74233 that indicates current vonr capability.
        // For other scenarios, we need to align AP and MP settings.
        if (setting == -1) {
            mCi.isVoNrEnabled(obtainMessage(EVENT_QUERY_VONR_ENABLED_DONE), null);
        } else if (setting == 1) {
            mCi.setVoNrEnabled(true, obtainMessage(EVENT_SET_VONR_ENABLED_DONE), null);
        } else if (setting == 0) {
            mCi.setVoNrEnabled(false, obtainMessage(EVENT_SET_VONR_ENABLED_DONE), null);
        }
    }

    public boolean isInScbm() {
        return ScbmHandler.getInstance().isInScbm();
    }

    public void exitScbm() {
        ScbmHandler.getInstance().exitScbm();
    }

    public void setOnScbmExitResponse(Handler h, int what, Object obj) {
        ScbmHandler.getInstance().setOnScbmExitResponse(h, what, obj);
    }

    public void unsetOnScbmExitResponse(Handler h) {
        ScbmHandler.getInstance().unsetOnScbmExitResponse(h);
    }

    public void registerForScbmTimerReset(Handler h, int what, Object obj) {
        ScbmHandler.getInstance().registerForScbmTimerReset(h, what, obj);
    }

    public void unregisterForScbmTimerReset(Handler h) {
        ScbmHandler.getInstance().unregisterForScbmTimerReset(h);
    }

    public boolean isInScbm(int subId) {
        return ScbmHandler.getInstance().isInScbm(subId);
    }

    public boolean isExitScbmFeatureSupported() {
        return ScbmHandler.getInstance().isExitScbmFeatureSupported();
    }

    public boolean isScbmTimerCanceledForEmergency() {
        return ScbmHandler.getInstance().isScbmTimerCanceledForEmergency();
    }

    // Query IMEI info when first time a SIM card inserted on device in a power-up
    void notifySubInfoAdded() {
        logd("notifySubInfoAdded, query IMEI");
        mCi.getDeviceIdentity(obtainMessage(EVENT_GET_DEVICE_IDENTITY_DONE));
    }

    /**
     * @return {@code true} if using the new telephony data stack.
     */
    public boolean isUsingNewDataStack() {
        return true;
    }

    /**
     * Set C_IWLAN timer use to delay deactivating data call when mobile data UI is off or
     * roaming UI is off when device is in roaming.
     */
    @Override
    public void setCiwlanTimerToDealayDeactivateDataCall(long ciwlanTimer) {
        mCiwlanTimer = ciwlanTimer;
    }

    /**
     * Get C_IWLAN timer use to delay deactivating data call when mobile data UI is off or
     * roaming UI is off when device is in roaming.
     */
    @Override
    public long getCiwlanTimerToDealayDeactivateDataCall() {
        return mCiwlanTimer;
    }

    @Override
    public void acceptCall(int videoState) throws CallStateException {
        boolean isInEcbm = mTelephonyManager.getEmergencyCallbackMode();
        logd("acceptCall: mIsInSecureMode = " + mIsInSecureMode + ", isInEcbm = " + isInEcbm);
        // Only allow accepting the call if device is not in Secure Mode or it is in ECbM
        if (!mIsInSecureMode || isInEcbm) {
            super.acceptCall(videoState);
        } else {
            throw new CallStateException(CallStateException.ERROR_DEVICE_IN_SECURE_MODE,
                    "Secure Mode");
        }
    }

    @Override
    public Connection dial(String dialString, @NonNull DialArgs dialArgs,
            Consumer<Phone> chosenPhoneConsumer) throws CallStateException {
        boolean isEmergency = getEmergencyNumberTracker().isEmergencyNumber(dialString);
        logd("dial: mIsInSecureMode = " + mIsInSecureMode + ", isEmergency = " + isEmergency);
        // Only allow dialing if device is not in Secure Mode or the number is emergency
        if (!mIsInSecureMode || isEmergency) {
            return super.dial(dialString, dialArgs, chosenPhoneConsumer);
        } else {
            throw new CallStateException(CallStateException.ERROR_DEVICE_IN_SECURE_MODE,
                    "Secure Mode");
        }
    }

    @Override
    public void startNetworkScan(NetworkScanRequest nsr, Message response) {
        logd("startNetworkScan: mIsInSecureMode = " + mIsInSecureMode);
        if (!mIsInSecureMode) {
            super.startNetworkScan(nsr, response);
        } else {
            AsyncResult.forMessage(response, null,
                    CommandException.fromRilErrno(RILConstants.OPERATION_NOT_ALLOWED));
            response.sendToTarget();
        }
    }

    @Override
    public boolean handleUssdRequest(String ussdRequest, ResultReceiver wrappedCallback) {
    logd("handleUssdRequest: mIsInSecureMode = " + mIsInSecureMode);
        if (!mIsInSecureMode) {
            return super.handleUssdRequest(ussdRequest, wrappedCallback);
        } else {
            sendUssdResponse(ussdRequest, null, TelephonyManager.USSD_RETURN_FAILURE,
                    wrappedCallback);
            return false;
        }
    }

    @Override
    public void setNetworkSelectionModeAutomatic(Message response) {
        // we don't want to do this unecesarily - it acutally causes
        // the radio to repeate network selection and is costly
        // first check if we're already in automatic mode
        logd("setNetworkSelectionModeAutomatic, querying current mode");
        if(QtiTelephonyComponentFactory.getInstance().getRil(mPhoneId).isCagSnpnEnabled()) {
            synchronized (mLock) {
                int serial = 0;

                synchronized (mPendingRequests) {
                    try {
                        Token token = mExtTelephonyManager.getNetworkSelectionMode(mPhoneId,
                                mClient);
                        serial = token.get();
                    } catch (RuntimeException e) {
                        logd("Exception getNetworkSelectionMode " + e);
                    }
                    mPendingRequests.put(serial, response);
                }
                try {
                    mLock.wait();
                } catch (Exception e) {
                    logd("Exception :" + e);
                }
            }
        } else {
            super.setNetworkSelectionModeAutomatic(response);
        }
    }

    private void onCheckForNetworkSelectionModeAutomatic(int slotId, Message response,
            Status status, NetworkSelectionMode modes) {
        AsyncResult ar = new AsyncResult(response, modes, null);
        boolean doAutomatic = true;
        int accessMode = QtiTelephonyComponentFactory.getInstance().getRil(mPhoneId)
                .getAccessMode();
        if (status.get() == Status.SUCCESS) {
            try {
                if (modes.getIsManual() == false && modes.getAccessMode() == accessMode) {
                    // already confirmed to be in automatic mode - don't resend
                    doAutomatic = false;
                }
            } catch (Exception e) {
                // send the setting on error
            }
        }
        synchronized (mLock) {
            mLock.notify();
        }
        // wrap the response message in our own message along with
        // an empty string (to indicate automatic selection) for the
        // operator's id.
        NetworkSelectMessage nsm = new NetworkSelectMessage();
        nsm.message = response;
        nsm.operatorNumeric = "";
        nsm.operatorAlphaLong = "";
        nsm.operatorAlphaShort = "";

        if (doAutomatic) {
            Message msg = obtainMessage(EVENT_SET_NETWORK_AUTOMATIC_COMPLETE, nsm);
            mCi.setNetworkSelectionModeAutomatic(msg);
        } else {
            Rlog.d(LOG_TAG, "setNetworkSelectionModeAutomatic - already auto, ignoring");
            // let the calling application know that the we are ignoring automatic mode switch.
            if (nsm.message != null) {
                nsm.message.arg1 = ALREADY_IN_AUTO_SELECTION;
            }

            ar.userObj = nsm;
            handleSetSelectNetwork(ar);
        }

        updateSavedNetworkOperator(nsm);
    }

    private void logd(String msg) {
        Rlog.d(LOG_TAG, "[" + mPhoneId +" ] " + msg);
    }

    private void loge(String msg) {
        Rlog.e(LOG_TAG, "[" + mPhoneId +" ] " + msg);
    }
}
