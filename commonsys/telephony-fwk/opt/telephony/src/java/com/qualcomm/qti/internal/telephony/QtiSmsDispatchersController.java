/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.internal.telephony;

import static android.telephony.AccessNetworkConstants.TRANSPORT_TYPE_WWAN;
import static android.telephony.NetworkRegistrationInfo.DOMAIN_PS;

import android.annotation.NonNull;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Binder;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.telephony.CarrierConfigManager;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.ServiceState;
import android.telephony.SmsManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.ims.ImsException;
import android.telephony.ims.ImsManager;
import android.telephony.ims.ImsMmTelManager;

import com.android.internal.telephony.flags.FeatureFlags;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.SMSDispatcher;
import com.android.internal.telephony.SmsDispatchersController;
import com.android.internal.telephony.SmsNumberUtils;
import com.android.internal.telephony.SmsStorageMonitor;
import com.android.internal.telephony.SmsUsageMonitor;
import com.android.telephony.Rlog;

import com.qti.extphone.CiwlanConfig;
import com.qti.extphone.Client;
import com.qti.extphone.ExtTelephonyManager;
import com.qti.extphone.ExtPhoneCallbackListener;
import com.qti.extphone.ServiceCallback;
import com.qti.extphone.Status;
import com.qti.extphone.Token;

import java.util.ArrayList;

public class QtiSmsDispatchersController extends SmsDispatchersController {

    private static final String LOG_TAG = "QtiSmsDispatchersController";

    private Client mClient;
    private String mPackageName;
    private TelephonyManager mTelephonyManager;
    private ExtTelephonyManager mExtTelephonyManager;
    private Phone mPhone;
    private int mSlotId;
    private Context mContext;
    private boolean mIsInSecureMode = false;
    private CiwlanConfig mCiwlanConfig = null;
    private boolean mCsSmsAllowedInCiwlanOnly = false;

    private static final String ACTION_RADIO_POWER_STATE_CHANGED =
            "org.codeaurora.intent.action.RADIO_POWER_STATE";
    private static final String RADIO_POWER_STATE = "state";

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED:
                    logd("ACTION_CARRIER_CONFIG_CHANGED");
                    int subId = intent.getIntExtra(CarrierConfigManager.EXTRA_SUBSCRIPTION_INDEX,
                            SubscriptionManager.INVALID_SUBSCRIPTION_ID);
                    handleCarrierConfigChanged(subId);
                    break;
                case ACTION_RADIO_POWER_STATE_CHANGED:
                    int slotId = intent.getIntExtra(SubscriptionManager.EXTRA_SLOT_INDEX,
                            SubscriptionManager.INVALID_SIM_SLOT_INDEX);
                    int radioState = intent.getIntExtra(RADIO_POWER_STATE,
                            TelephonyManager.RADIO_POWER_UNAVAILABLE);
                    handleRadioPowerStateChanged(slotId, radioState);
                    break;
                default:
                    loge("onReceive: Unsupported action");
            }
        }
    };

    public QtiSmsDispatchersController(Phone phone, SmsStorageMonitor storageMonitor,
            SmsUsageMonitor usageMonitor, @NonNull FeatureFlags featureFlags) {
        super(phone, storageMonitor, usageMonitor, featureFlags);
        logd("Constructor");
        mPhone = phone;
        mSlotId = phone.getPhoneId();
        mContext = phone.getContext();
        mPackageName = phone.getContext().getPackageName();
        mTelephonyManager = phone.getContext().getSystemService(TelephonyManager.class);
        mExtTelephonyManager = ExtTelephonyManager.getInstance(mContext);
        mExtTelephonyManager.connectService(mServiceCallback);
        IntentFilter filter = new IntentFilter(CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED);
        filter.addAction(ACTION_RADIO_POWER_STATE_CHANGED);
        mContext.registerReceiver(mBroadcastReceiver, filter);
    }

    private ServiceCallback mServiceCallback = new ServiceCallback() {
        @Override
        public void onConnected() {
            logd("ExtTelephonyService connected");
            int[] events = new int[] {
                    ExtPhoneCallbackListener.EVENT_ON_SECURE_MODE_STATUS_CHANGE,
                    ExtPhoneCallbackListener.EVENT_ON_CIWLAN_CONFIG_CHANGE};
            mClient = mExtTelephonyManager.registerCallbackWithEvents(mPackageName,
                    mExtPhoneCallbackListener, events);
            logd("mClient = " + mClient);
            try {
                mExtTelephonyManager.getSecureModeStatus(mClient);
            } catch (RemoteException ex) {
                loge("getSecureModeStatus, remote exception " + ex);
            }
            try {
                // Query the C_IWLAN config
                mCiwlanConfig = mExtTelephonyManager.getCiwlanConfig(mSlotId);
            } catch (RemoteException ex) {
                loge("getCiwlanConfig, remote exception " + ex);
            }
        }

        @Override
        public void onDisconnected() {
            logd("ExtTelephonyService disconnected mClient = " + mClient);
            mExtTelephonyManager.unregisterCallback(mExtPhoneCallbackListener);
            mClient = null;
        }
    };

    private ExtPhoneCallbackListener mExtPhoneCallbackListener = new ExtPhoneCallbackListener() {
        @Override
        public void onCiwlanConfigChange(int slotId, CiwlanConfig ciwlanConfig) {
            logd("onCiwlanConfigChange: slotId = " + slotId + ", config = " + ciwlanConfig);
            if (slotId == mSlotId) {
                mCiwlanConfig = ciwlanConfig;
            }
        }

        @Override
        public void onSecureModeStatusChange(boolean enabled) throws RemoteException {
            logd("onSecureModeStatusChange: enabled = " + enabled);
            mIsInSecureMode = enabled;
        }

        @Override
        public void getSecureModeStatusResponse(Token token, Status status, boolean enableStatus) {
            logd("getSecureModeStatusResponse: enabled = " + enableStatus);
            mIsInSecureMode = enableStatus;
        }
    };

    private boolean isSmsAllowed(String funcName, String destAddr, boolean... usesImsService) {
        boolean isEmergency = mTelephonyManager.isEmergencyNumber(filterDestAddress(destAddr));
        boolean isImsAvailable = mImsSmsDispatcher.isAvailable();
        boolean isEmergencySmsSupported = mImsSmsDispatcher.isEmergencySmsSupport(destAddr);
        boolean isCiwlanEnabled = isCiwlanEnabled();
        boolean isInCiwlanOnlyMode = isInCiwlanOnlyMode();
        boolean isCiwlanEnabledAndInOnlyMode =  isCiwlanEnabled && isInCiwlanOnlyMode;
        logd(funcName + ": mIsInSecureMode = " + mIsInSecureMode + ", isEmergency = "
                + isEmergency + ", isImsAvailable = " + isImsAvailable
                + (usesImsService.length != 0 ? ", usesImsService = " + usesImsService[0] : "")
                + ", isEmergencySmsSupported = " + isEmergencySmsSupported + ", isCiwlanEnabled = "
                + isCiwlanEnabled + ", isInCiwlanOnlyMode = " + isInCiwlanOnlyMode
                + ", csSmsAllowedInCiwlanOnly = " + mCsSmsAllowedInCiwlanOnly);
        // In secure mode, regular SMS is not allowed, and emergency SMS is only allowed when IMS
        // or limited service is available
        if (!mIsInSecureMode || (isEmergency && (isImsAvailable || isEmergencySmsSupported))) {
            // In C_IWLAN-only mode, emergency and regular SMS are allowed if IMS is available or
            // based on KEY_CS_SMS_IN_CIWLAN_ONLY_MODE carrier config.
            // The following conditions all have to be true for isImsAvailable to be true:
            // 1) IMS service being up 2) IMS being registered 3) IMS being SMS capable
            if (isCiwlanEnabledAndInOnlyMode) {
                return (isImsAvailable
                        && (usesImsService.length != 0 ? !usesImsService[0] : true))
                        || mCsSmsAllowedInCiwlanOnly;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    @Override
    protected void sendData(String callingPackage, int callingUser, String destAddr, String scAddr,
            int destPort, byte[] data, PendingIntent sentIntent, PendingIntent deliveryIntent,
            boolean isForVvm) {
        if (isSmsAllowed("sendData", destAddr)) {
            super.sendData(callingPackage, callingUser, destAddr, scAddr, destPort, data,
                    sentIntent, deliveryIntent, isForVvm);
        } else {
            triggerSentIntentForFailure(sentIntent);
        }
    }

    @Override
    public void sendText(String destAddr, String scAddr, String text, PendingIntent sentIntent,
            PendingIntent deliveryIntent, Uri messageUri, String callingPkg, int callingUser,
            boolean persistMessage, int priority, boolean expectMore, int validityPeriod,
            boolean isForVvm, long messageId) {
        if (isSmsAllowed("sendText", destAddr)) {
            super.sendText(destAddr, scAddr, text, sentIntent, deliveryIntent, messageUri,
                    callingPkg, callingUser, persistMessage, priority, expectMore, validityPeriod,
                    isForVvm, messageId);
        } else {
            triggerSentIntentForFailure(sentIntent);
        }
    }

    @Override
    public void sendText(String destAddr, String scAddr, String text, PendingIntent sentIntent,
            PendingIntent deliveryIntent, Uri messageUri, String callingPkg, int callingUser,
            boolean persistMessage, int priority, boolean expectMore, int validityPeriod,
            boolean isForVvm, long messageId, boolean skipShortCodeCheck) {
        if (isSmsAllowed("sendText", destAddr)) {
            super.sendText(destAddr, scAddr, text, sentIntent, deliveryIntent, messageUri,
                    callingPkg, callingUser, persistMessage, priority, expectMore, validityPeriod,
                    isForVvm, messageId, skipShortCodeCheck);
        } else {
            triggerSentIntentForFailure(sentIntent);
        }
    }

    @Override
    protected void sendMultipartText(String destAddr, String scAddr,
            ArrayList<String> parts, ArrayList<PendingIntent> sentIntents,
            ArrayList<PendingIntent> deliveryIntents, Uri messageUri, String callingPkg,
            int callingUser, boolean persistMessage, int priority, boolean expectMore,
            int validityPeriod, long messageId) {
        if (isSmsAllowed("sendMultipartText", destAddr)) {
            super.sendMultipartText(destAddr, scAddr, parts, sentIntents, deliveryIntents,
                    messageUri, callingPkg, callingUser, persistMessage, priority, expectMore,
                    validityPeriod, messageId);
        } else {
            triggerSentIntentForFailure(sentIntents);
        }
    }

    @Override
    public void sendRetrySms(SMSDispatcher.SmsTracker tracker) {
        if (isSmsAllowed("sendRetrySms", tracker.mDestAddress, tracker.mUsesImsServiceForIms)) {
            super.sendRetrySms(tracker);
        } else {
            triggerSentIntentForFailure(tracker.mSentIntent);
        }
    }

    private void triggerSentIntentForFailure(PendingIntent sentIntent) {
        if (sentIntent != null) {
            try {
                sentIntent.send(SmsManager.RESULT_ERROR_GENERIC_FAILURE);
            } catch (CanceledException ex) {
                loge("Intent has been canceled!");
            }
        }
    }

    private void triggerSentIntentForFailure(ArrayList<PendingIntent> sentIntents) {
        if (sentIntents == null) {
            return;
        }
        for (PendingIntent sentIntent : sentIntents) {
            triggerSentIntentForFailure(sentIntent);
        }
    }

    private String filterDestAddress(String destAddr) {
        String result = SmsNumberUtils.filterDestAddr(mContext, mPhone.getSubId(), destAddr);
        return result != null ? result : destAddr;
    }

    private boolean isCiwlanEnabled() {
        final long identity = Binder.clearCallingIdentity();
        try {
            ImsMmTelManager imsMmTelMgr = getImsMmTelManager();
            if (imsMmTelMgr == null) {
                return false;
            }
            return imsMmTelMgr.isCrossSimCallingEnabled();
        } catch (ImsException exception) {
            loge("Failed to get C_IWLAN toggle status: " + exception);
            return false;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private ImsMmTelManager getImsMmTelManager() {
        int subId = SubscriptionManager.getSubscriptionId(mSlotId);
        if (!SubscriptionManager.isUsableSubscriptionId(subId)) {
            logd("getImsMmTelManager: subId unusable");
            return null;
        }
        if (mContext != null) {
            ImsManager imsMgr = mContext.getSystemService(ImsManager.class);
            return (imsMgr == null) ? null : imsMgr.getImsMmTelManager(subId);
        } else {
            logd("getImsMmTelManager: context null");
            return null;
        }
    }

    private boolean isInCiwlanOnlyMode() {
        if (mCiwlanConfig == null) {
            logd("C_IWLAN config null");
            return false;
        }
        if (isRoaming()) {
            return mCiwlanConfig.isCiwlanOnlyInRoam();
        }
        return mCiwlanConfig.isCiwlanOnlyInHome();
    }

    private boolean isRoaming() {
        if (mTelephonyManager == null) {
            logd("isRoaming: TelephonyManager null");
            return false;
        }
        boolean nriRoaming = false;
        ServiceState serviceState = null;
        final long identity = Binder.clearCallingIdentity();
        try {
            serviceState = mTelephonyManager.getServiceState();
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
        if (serviceState != null) {
            NetworkRegistrationInfo nri =
                    serviceState.getNetworkRegistrationInfo(DOMAIN_PS, TRANSPORT_TYPE_WWAN);
            if (nri != null) {
                nriRoaming = nri.isNetworkRoaming();
            } else {
                logd("isRoaming: network registration info null");
            }
        } else {
            logd("isRoaming: service state null");
        }
        return nriRoaming;
    }

    private boolean isCsSmsAllowedInCiwlanOnlyMode() {
        CarrierConfigManager carrierConfigMgr = (CarrierConfigManager) mContext.getSystemService(
                Context.CARRIER_CONFIG_SERVICE);
        if (carrierConfigMgr != null) {
            // Get the carrier config manager specific to this sub
            int subId = mPhone.getSubId();
            PersistableBundle config = carrierConfigMgr.getConfigForSubId(subId);
            if (config != null) {
                return config.getBoolean(
                        CarrierConfigManager.KEY_CS_SMS_IN_CIWLAN_ONLY_MODE, false);
            } else {
                logd("Carrier config null for subId = " + subId);
            }
        } else {
            logd("Carrier config manager null");
        }
        return false;
    }

    private void handleCarrierConfigChanged(int subId) {
        if (subId != mPhone.getSubId()) {
            return;
        }
        mCsSmsAllowedInCiwlanOnly = isCsSmsAllowedInCiwlanOnlyMode();
    }

    private void handleRadioPowerStateChanged(int slotId, int radioState) {
        // Retrieve the C_IWLAN config when radio becomes available, e.g., after modem SSR.
        if (slotId == mSlotId && radioState == TelephonyManager.RADIO_POWER_ON) {
            if (mExtTelephonyManager.isServiceConnected()) {
                try {
                    logd("Retrieving C_IWLAN config");
                    mCiwlanConfig = mExtTelephonyManager.getCiwlanConfig(mSlotId);
                } catch (RemoteException ex) {
                    loge("getCiwlanConfig, remote exception " + ex);
                }
            }
        }
    }

    private void logd(String msg) {
        Rlog.d(LOG_TAG, msg);
    }

    private void loge(String msg) {
        Rlog.e(LOG_TAG, msg);
    }
}
