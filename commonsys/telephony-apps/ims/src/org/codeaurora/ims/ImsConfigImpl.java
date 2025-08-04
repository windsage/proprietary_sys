/*
 * Copyright (c) 2015, 2016, 2018-2020, 2022-2023, 2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package org.codeaurora.ims;

import java.lang.ClassCastException;
import java.util.ArrayList;
import android.Manifest;

import androidx.annotation.VisibleForTesting;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.ims.stub.ImsConfigImplBase;
import android.telephony.ims.ImsMmTelManager;
import android.telephony.ims.ProvisioningManager;
import com.qualcomm.ims.utils.Log;
import org.codeaurora.ims.QtiCallConstants;
import org.codeaurora.telephony.utils.SomeArgs;
import org.codeaurora.telephony.utils.AsyncResult;

/*
  AOSP calls to this class will execute on Binder (Runnable::run) threads
  due to blocking API(s) from Framework -> QC IMS Service -> QCRIL
    Applicable API(s): setConfig(int, int), setConfig(int, String)
                       getConfigInt, getConfigString
*/
public class ImsConfigImpl extends ImsConfigImplBase {
    private static final int EVENT_ACCESS_PROVISIONED_VAL = 1;
    private static final int EVENT_SET_FEATURE_VALUE = 2;

    private ImsServiceSub mServiceSub;
    private ImsSenderRxr mCi;
    private Handler mHandler;
    private Context mContext;
    private int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    private SubscriptionManager mSubscriptionManager = null;
    private Uri mWfcModeUri;
    private Uri mWfcRoamingModeUri;
    private ImsMmTelManager mImsMmTelManager = null;
    private boolean mIsContentObserversRegistered = false;
    private boolean mIsCarrierConfigLoaded = false;
    // Permissions used by class.
    private final String MODIFY_PHONE_STATE = Manifest.permission.MODIFY_PHONE_STATE;
    private final String READ_PRIVILEGED_PHONE_STATE =
            Manifest.permission.READ_PRIVILEGED_PHONE_STATE;
    private final String READ_PHONE_STATE = Manifest.permission.READ_PHONE_STATE;

    private ContentObserver mVoWiFiModeObserver =
            new ContentObserver(new Handler(Looper.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange) {
                    updateWFCMode(false /*isRoamingMode*/);
                }
            };
    private ContentObserver mVoWiFiRoamingModeObserver =
            new ContentObserver(new Handler(Looper.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange) {
                    updateWFCMode(true /*isRoamingMode*/);
                }
            };

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getExtras() == null) {
                Log.e(ImsConfigImpl.this, "onReceive: null extras");
                return;
            }
            int subId = intent.getExtras().getInt(SubscriptionManager.EXTRA_SUBSCRIPTION_INDEX,
                    SubscriptionManager.INVALID_SUBSCRIPTION_ID);
            String action = intent.getAction();
            Log.i(ImsConfigImpl.this, "mBroadcastReceiver: action " + action +
                     " sub = " + subId +  " mSubId = " + mSubId);
            if (!SubscriptionManager.isValidSubscriptionId(subId)) {
                return;
            }
            if ((CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED.equals(action) ||
                    QtiCallConstants.ACTION_ESSENTIAL_RECORDS_LOADED.equals(action)) &&
                    subId == mSubId) {
                mIsCarrierConfigLoaded = true;
                updateWfcModeConfigurationsToModem();
            }
        }
    };

    /**
     * Creates the Ims Config interface object for a sub.
     * @param senderRxr
     */
    public ImsConfigImpl(ImsServiceSub serviceSub, ImsSenderRxr senderRxr, Context context) {
        super(Runnable::run);
        mServiceSub = serviceSub;
        mCi = senderRxr;
        mContext = context;
        mHandler = new ImsConfigImplHandler(mContext.getMainLooper());
        // Listen for subscription changed to get valid subscription Id.
        mSubscriptionManager = (SubscriptionManager) mContext.getSystemService(
                Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        if (mSubscriptionManager != null) {
            mSubscriptionManager = mSubscriptionManager.createForAllUserProfiles();
            mSubscriptionManager.addOnSubscriptionsChangedListener(mOnSubscriptionsChangeListener);
        } else {
            Log.e(this, "unable to listen for subscription changed due to " +
                    "subscriptionManager is null");
        }
        IntentFilter filter = new IntentFilter(
                CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED);
        filter.addAction(QtiCallConstants.ACTION_ESSENTIAL_RECORDS_LOADED);
        mContext.registerReceiver(mBroadcastReceiver, filter, Context.RECEIVER_EXPORTED);
    }

    @VisibleForTesting
    public Handler getHandler() {
        return mHandler;
    }

    public void dispose() {
        try {
            mContext.unregisterReceiver(mBroadcastReceiver);
        } catch(IllegalArgumentException e) {
            Log.e(this, "dispose :: Exception " + e);
        }

        if (mSubscriptionManager != null) {
            mSubscriptionManager.removeOnSubscriptionsChangedListener(
                    mOnSubscriptionsChangeListener);
        }
    }

    private final SubscriptionManager.OnSubscriptionsChangedListener mOnSubscriptionsChangeListener
            = new SubscriptionManager.OnSubscriptionsChangedListener() {
        @Override
        public void onSubscriptionsChanged() {
            final SubscriptionInfo subInfo = mSubscriptionManager.
                    getActiveSubscriptionInfoForSimSlotIndex(mServiceSub.getPhoneId());
            if (subInfo == null) {
                Log.e(ImsConfigImpl.this, "onSubscriptionsChanged unable to process due to " +
                        "SubscriptionInfo is null");
                clearSubscriptionCache();
                return;
            }

            final int subId = subInfo.getSubscriptionId();
            Log.d(ImsConfigImpl.this, "onSubscriptionsChanged oldSubId = " + mSubId
                    + " new SubId " + subId);
            if (mSubId != subId) {
                clearSubscriptionCache();
                mSubId = subId;
                if (!SubscriptionManager.isValidSubscriptionId(subId)
                        || !mSubscriptionManager.isActiveSubscriptionId(subId)) {
                    Log.e(ImsConfigImpl.this, "Invalid subscription id = " + subId);
                    return;
                }
                mImsMmTelManager = ImsMmTelManager.createForSubscriptionId(mSubId);
                mWfcModeUri = Uri.withAppendedPath(SubscriptionManager.WFC_MODE_CONTENT_URI,
                        "" + mSubId);
                mWfcRoamingModeUri = Uri.withAppendedPath(SubscriptionManager.
                        WFC_ROAMING_MODE_CONTENT_URI, "" + mSubId);
                mContext.getContentResolver().registerContentObserver(mWfcModeUri, true,
                        mVoWiFiModeObserver);
                mContext.getContentResolver().registerContentObserver(mWfcRoamingModeUri, true,
                        mVoWiFiRoamingModeObserver);
                mIsContentObserversRegistered = true;
            }
        }
    };

    private void clearSubscriptionCache() {
        // The function clears the previous subscription cache.
        mIsCarrierConfigLoaded = false;
        mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        if (mIsContentObserversRegistered) {
            mContext.getContentResolver().unregisterContentObserver(
                    mVoWiFiModeObserver);
            mContext.getContentResolver().unregisterContentObserver(
                    mVoWiFiRoamingModeObserver);
            mIsContentObserversRegistered = false;
        }
         mImsMmTelManager = null;
         mWfcModeUri = null;
         mWfcRoamingModeUri = null;
    }

    public void updateWfcModeConfigurationsToModem() {
        // The function sends async request.
        Log.d(this, "updateWfcModeConfigurationsToModem: mIsCarrierConfigLoaded = "
                + mIsCarrierConfigLoaded);
        if (mServiceSub.IsWfcRoamingConfigurationSupportedByModem() && mImsMmTelManager != null &&
                mIsCarrierConfigLoaded) {
            updateWFCMode(false /*isRoamingMode*/);
            updateWFCMode(true /*isRoamingMode*/);
        }
    }

    @VisibleForTesting
    public int updateWFCMode(boolean isRoamingMode) {
        Log.d(this, "updateWFCMode: sub = " + mSubId + " isRoamingMode = " + isRoamingMode);
        mContext.enforceCallingOrSelfPermission(MODIFY_PHONE_STATE, "updateWFCMode");
        enforceReadPhoneState("updateWFCMode");
        //If subscription becomes invalid, catch the exception and return CONFIG_RESULT_FAILED
        try {
            if (mSubscriptionManager == null ||
                    !mSubscriptionManager.isActiveSubscriptionId(mSubId)) {
                Log.w(this, "updateWFCMode: invalid sub = " + mSubId);
                return ImsConfigImplBase.CONFIG_RESULT_FAILED;
            }

            if (mImsMmTelManager == null) {
                Log.w(this, "updateWFCMode: mImsMmTelManager null");
                return ImsConfigImplBase.CONFIG_RESULT_FAILED;
            }

            if (!mServiceSub.IsWfcRoamingConfigurationSupportedByModem()) {
                Log.i(this, "Modem do not support WFC roaming config");
                return ImsConfigImplBase.CONFIG_RESULT_FAILED;
            }

            int item, value;
            if (isRoamingMode) {
                item = QtiCallConstants.QTI_CONFIG_VOWIFI_ROAMING_MODE_PREFERENCE;
                value = mImsMmTelManager.getVoWiFiRoamingModeSetting();
            } else {
                item = ProvisioningManager.KEY_VOICE_OVER_WIFI_MODE_OVERRIDE;
                value = mImsMmTelManager.getVoWiFiModeSetting();
            }
            sendSetConfigRequestAsync(item, value);
        } catch (Exception e) {
            Log.w(this, "Could not update WFCMode: invalid sub = " + mSubId);
            return ImsConfigImplBase.CONFIG_RESULT_FAILED;
        }
        return ImsConfigImplBase.CONFIG_RESULT_UNKNOWN;
    }

    public enum SetCapabilityFailCause {
        ERROR_GENERIC,
        ERROR_SUCCESS;

        @Override
        public String toString() {
            switch(this) {
                case ERROR_GENERIC:
                    return "ERROR_GENERIC";
                case ERROR_SUCCESS:
                    return "ERROR_SUCCESS";
            }
            return "";
        }
    }

    public interface SetCapabilityValueListener {
        public void onSetCapabilityValueSuccess(int capability, int radioTech, int value);
        public void onSetCapabilityValueFailure(int capability, int radioTech,
                                                SetCapabilityFailCause reason);
    }

    //Handler for tracking requests sent to ImsSenderRxr.
    private class ImsConfigImplHandler extends Handler {
        public ImsConfigImplHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.i(this, "Message received: what = " + msg.what);
            AsyncResult ar = (AsyncResult) msg.obj;

            switch (msg.what) {
                case EVENT_SET_FEATURE_VALUE:
                    onSetFeatureResponseDone(ar);
                    break;
                case EVENT_ACCESS_PROVISIONED_VAL:
                    onAccessProvisionedValDone(ar, msg.what);
                    break;
                default:
                    Log.e(this, "handleMessage: unhandled message");
            }
        }
    }

    private void onAccessProvisionedValDone(AsyncResult ar, int accessType) {
        SomeArgs response = (SomeArgs)ar.userObj;
        response.arg2 = getOperationStatus(response.arg1, ar.exception);
        response.arg3 = ar.result;
        synchronized(response) {
            Log.i(this, "Notifyall");
            response.notifyAll();
        }
    }

    private void onSetFeatureResponseDone(AsyncResult ar) {
        if (ar == null ) {
            Log.e(this, "onSetFeatureResponseDone :: AsyncResult is null.");
            return;
        }

        SetCapabilityValueListener listener = null;
        ArrayList<CapabilityStatus> capabilityStatusList = null;

        try {
            capabilityStatusList = (ArrayList<CapabilityStatus>)ar.result;
            listener = (SetCapabilityValueListener) ar.userObj;
        } catch (ClassCastException ex) {
            Log.e(this, "onSetFeatureResponseDone :: Exception " + ex);
        }

        if (listener == null) {
            Log.i(this, "onSetFeatureResponseDone :: listener is null");
            return;
        }

        if (capabilityStatusList == null) {
            Log.i(this, "onSetFeatureResponseDone :: capabilityStatusList is null");
            return;
        }

        for (CapabilityStatus capabilityStatus : capabilityStatusList) {
            if (ar.exception == null) {
                listener.onSetCapabilityValueSuccess(capabilityStatus.getCapability(),
                        capabilityStatus.getRadioTech(),
                        capabilityStatus.getStatus());
            } else {
                listener.onSetCapabilityValueFailure(capabilityStatus.getCapability(),
                        capabilityStatus.getRadioTech(),
                        SetCapabilityFailCause.ERROR_GENERIC);
            }
        }
    }

    // CONFIG_RESULT_NOT_SUPPORTED is currently only supported for AUTO_REJECT
    private int getOperationStatus(Object item, Throwable ex) {
        Log.d(this, "getOperationStatus: " + ex);
        if (ex == null) {
            return ImsConfigImplBase.CONFIG_RESULT_SUCCESS;
        }
        if (item == null || !(item instanceof Integer) ||
                !(ex instanceof ImsRilException)) {
            return ImsConfigImplBase.CONFIG_RESULT_FAILED;
        }
        int config = (Integer) item;
        if (config != QtiCallConstants.AUTO_REJECT_CALL_MODE) {
            return ImsConfigImplBase.CONFIG_RESULT_FAILED;
        }
        ImsRilException imsRilException = (ImsRilException) ex;
        if (imsRilException.getErrorCode() == ImsErrorCode.REQUEST_NOT_SUPPORTED) {
            return QtiCallConstants.CONFIG_RESULT_NOT_SUPPORTED;
        }
        return ImsConfigImplBase.CONFIG_RESULT_FAILED;
    }

    private void enforceReadPhoneState(String fn) {
        if (mContext.checkCallingOrSelfPermission(READ_PRIVILEGED_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED
                && mContext.checkCallingOrSelfPermission(READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            mContext.enforceCallingOrSelfPermission(READ_PHONE_STATE, fn);
        }
    }

    protected Object sendRequest(int requestType, int item, int event, boolean boolVal,
            int intVal, String strVal) {
        //SomeArgs semantics
        //arg1 - item
        //arg2 - result of operation - pass/fail
        //arg3 - value returned by function for get operation
        SomeArgs request = SomeArgs.obtain();
        try {
            request.arg1 = item;
            mCi.sendConfigRequest(requestType, ImsCallUtils.convertImsConfigToImsConfigItem(item),
                    boolVal, intVal, strVal, ImsConfigItem.NO_ERR,
                    mHandler.obtainMessage(event, request));

            synchronized(request) {
                while(request.arg2 == null) {
                    try {
                        request.wait();
                    } catch (InterruptedException e) {
                        //Do Nothing and wait
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            request.arg2 = ImsConfigImplBase.CONFIG_RESULT_UNKNOWN;
        } finally {
            return request;
        }
    }

    /**
     * Gets the currently stored value configuration value from the ImsService for {@code item}.
     *
     * @param item an integer key.
     * @return configuration value, stored in integer format or {@link #CONFIG_RESULT_UNKNOWN} if
     * unavailable.
     */
    @Override
    public int getConfigInt(int item) {
        Log.d(this, "getConfigInt :: item=" + item);
        enforceReadPhoneState("getConfigInt");
        if (!ImsCallUtils.isConfigRequestValid(item, ImsCallUtils.CONFIG_TYPE_INT)) {
            Log.e(this, "Invalid API request for item");
            return ImsConfigImplBase.CONFIG_RESULT_UNKNOWN;
        }
        SomeArgs result = (SomeArgs)sendRequest(MessageId.REQUEST_GET_IMS_CONFIG, item,
                EVENT_ACCESS_PROVISIONED_VAL, false, 0, null);
        int retVal = ((int)result.arg2 != ImsConfigImplBase.CONFIG_RESULT_SUCCESS ?
                    ImsConfigImplBase.CONFIG_RESULT_UNKNOWN : (int)result.arg3);
        result.recycle();
        return retVal;
    }

    /**
     * Gets the currently stored value configuration value from the ImsService for {@code item}.
     *
     * @param item an integer key.
     * @return configuration value, stored in String format or {@code null} if unavailable.
     */
    @Override
    public String getConfigString(int item) {
        Log.d(this, "getConfigString :: item=" + item);
        enforceReadPhoneState("getConfigString");
        if (!ImsCallUtils.isConfigRequestValid(item, ImsCallUtils.CONFIG_TYPE_STRING)) {
            Log.e(this, "Invalid API request for item");
            return null;
        }
        SomeArgs result = (SomeArgs)sendRequest(MessageId.REQUEST_GET_IMS_CONFIG, item,
                EVENT_ACCESS_PROVISIONED_VAL, false, 0, null);
        String retVal = ((int)result.arg2 != ImsConfigImplBase.CONFIG_RESULT_SUCCESS ?
                    null : (String)result.arg3);
        result.recycle();
        return retVal;
    }

    private int sendSetConfigRequest(int item, int value) {
        SomeArgs result = (SomeArgs)sendRequest(MessageId.REQUEST_SET_IMS_CONFIG, item,
                EVENT_ACCESS_PROVISIONED_VAL, false, value, null);
        int retVal = (int)result.arg2;
        result.recycle();
        return retVal;
    }

    protected void sendSetConfigRequestAsync(int item, int value) {
        Log.d(this, "sendSetConfigRequestAsync :: item=" + item + " value=" + value);
        mCi.sendConfigRequest(MessageId.REQUEST_SET_IMS_CONFIG,
                ImsCallUtils.convertImsConfigToImsConfigItem(item),
                false, value, null, ImsConfigItem.NO_ERR, null);
    }
    /**
     * Sets the configuration value for this ImsService.
     *
     * @param item an integer key.
     * @param value an integer containing the configuration value.
     * @return the result of setting the configuration value, defined as either
     * {@link #CONFIG_RESULT_FAILED} or {@link #CONFIG_RESULT_SUCCESS}.
     */
    @Override
    public int setConfig(int item, int value) {
        Log.d(this, "setConfig :: item=" + item + " value=" + value);
        mContext.enforceCallingOrSelfPermission(MODIFY_PHONE_STATE, "setConfig");
        // Remove the below if block after Google bug 109936682 is fixed in AOSP.
        // With this bug, Google will update carrier_default_wfc_ims_roaming_enabled_bool
        // for supported carriers. Now it is always updated as false which is disabling
        // VoWiFi Roaming NV in modem.
        if (item == ProvisioningManager.KEY_VOICE_OVER_WIFI_ROAMING_ENABLED_OVERRIDE) {
            Log.w(this, "Ignore VoWiFi Roaming enable/disable");
            return ImsConfigImplBase.CONFIG_RESULT_FAILED;
        }
        if (!ImsCallUtils.isConfigRequestValid(item, ImsCallUtils.CONFIG_TYPE_INT)) {
            Log.e(this, "Invalid API request for item");
            return ImsConfigImplBase.CONFIG_RESULT_FAILED;
        }
        // To provide backward compatibility, AOSP will continue to update the Voice over Wifi mode
        // config with the roaming mode preference value whenever device moves to roaming.
        // So for the modems, that support separate NV configuration for home/roam preference, this
        // config update should be ignored to prevent overriding of home preference with roam
        // preference. This can be removed in future android releases once AOSP removes the logic
        // that update WFC mode when device moves from home to roam or viceverse.
        // QC IMS service listens to URI changes to update Home/Roam call mode preferences to modem
        if (item == ProvisioningManager.KEY_VOICE_OVER_WIFI_MODE_OVERRIDE &&
                 mServiceSub.IsWfcRoamingConfigurationSupportedByModem()) {
            Log.w(this, "VoWiFi mode: config update is done only when mode preference"
                    + " is changed or when sim is loaded");
            return ImsConfigImplBase.CONFIG_RESULT_FAILED;
        }
        return sendSetConfigRequest(item, value);
    }

    /**
     * Sets the configuration value for this ImsService.
     *
     * @param item an integer key.
     * @param value a String containing the new configuration value.
     * @return Result of setting the configuration value, defined as either
     * {@link #CONFIG_RESULT_FAILED} or {@link #CONFIG_RESULT_SUCCESS}.
     */
    @Override
    public int setConfig(int item, String value) {
        Log.d(this, "setConfig :: item=" + item + " value=" + value);
        mContext.enforceCallingOrSelfPermission(MODIFY_PHONE_STATE, "setConfig");
        if (!ImsCallUtils.isConfigRequestValid(item, ImsCallUtils.CONFIG_TYPE_STRING)) {
            Log.e(this, "Invalid API request for item");
            return ImsConfigImplBase.CONFIG_RESULT_FAILED;
        }
        SomeArgs result = (SomeArgs)sendRequest(MessageId.REQUEST_SET_IMS_CONFIG, item,
                EVENT_ACCESS_PROVISIONED_VAL, false, 0, value);
        int retVal = (int)result.arg2;
        result.recycle();
        return retVal;
    }

    public void setCapabilityValue(ArrayList<CapabilityStatus> capabilityStatusList,
            SetCapabilityValueListener listener) {
        mContext.enforceCallingOrSelfPermission(MODIFY_PHONE_STATE, "setCapabilityValue");
        mCi.setServiceStatus(mHandler.obtainMessage(EVENT_SET_FEATURE_VALUE, listener),
                capabilityStatusList, CallDetails.CALL_RESTRICT_CAUSE_NONE);
    }

    @VisibleForTesting
    public SubscriptionManager.OnSubscriptionsChangedListener getSubscriptionsChangeListener() {
        return mOnSubscriptionsChangeListener;
    }
}
