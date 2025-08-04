/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.internal.telephony;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.PersistableBundle;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.sysprop.TelephonyProperties;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.ims.FeatureConnector;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;

import org.codeaurora.ims.QtiImsExtConnector;
import org.codeaurora.ims.QtiImsExtManager;
import org.codeaurora.ims.QtiImsException;
import org.codeaurora.ims.QtiImsExtListenerBaseImpl;

import com.qti.extphone.ExtTelephonyManager;

/**
 * This class handles the logic to cache the Sms Callback Mode per subId.
 * It receives the SCBM intent from vendor IMS service and provides APIs for clients to query.
 * This class is not thread safe.
 */
public class ScbmHandler extends Handler {
    private static final String LOG_TAG = "ScbmHandler";
    private Context mContext;
    public static final int RESTART_SCM_TIMER = 0; // restart Scm timer
    public static final int CANCEL_SCM_TIMER = 1; // cancel Scm timer
    private static ScbmHandler sScbmHandler = null;
    // mScbmExitRespRegistrant is informed after the phone has been exited
    private Registrant mScbmExitRespRegistrant;
    // mScmTimerResetRegistrants are informed after Scm timer is canceled or re-started
    private final RegistrantList mScmTimerResetRegistrants = new RegistrantList();
    //This intent would be broadcasted when modem enters/exits SCBM
    private final String ACTION_SMS_CALLBACK_MODE =
            "org.codeaurora.intent.action.SMS_CALLBACK_MODE";
    private final String EXTRA_SMS_CALLBACK_MODE = "sms_callback_mode";
    private final String EXTRA_SMS_PERMISSION =
            "com.qti.permission.RECEIVE_SMS_CALLBACK_MODE";

    private SubscriptionManager mSubscriptionManager;
    private int mScbmPhoneId = SubscriptionManager.INVALID_PHONE_INDEX;
    private boolean mIsPhoneInScbmState = false;
    private QtiImsExtConnector mQtiImsExtConnector;
    private QtiImsExtManager mQtiImsExtManager;

    private boolean mScmCanceledForEmergency = false;
    private boolean mIsExitScbmFeatureRetrieved = false;
    private boolean mIsExitScbmFeatureSupported = false;

    //Constructor
    private ScbmHandler(Context context){
        mContext = context;
        mContext.registerReceiver(mScbmReceiver, new IntentFilter(ACTION_SMS_CALLBACK_MODE),
                EXTRA_SMS_PERMISSION, null);
        mSubscriptionManager = (SubscriptionManager) mContext
                .getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        if(SystemProperties.getBoolean("ril.inscbm", false)) {
            setIsInScbm(0, false);
            handleExitScbm();
        }

        if (ImsManager.isImsSupportedOnDevice(mContext)) {
            TelephonyManager tm = (TelephonyManager) mContext.getSystemService(
                    Context.TELEPHONY_SERVICE);
            final int numPhones = tm.getActiveModemCount();
            for (int phoneId = 0; phoneId < numPhones; phoneId++) {
                new ImsFeatureConnector(phoneId);
            }
        }
    }

    private class ImsFeatureConnector implements FeatureConnector.Listener<ImsManager> {
        private FeatureConnector<ImsManager> mFeatureConnector;
        private static final int CONNECTOR_RETRY_DELAY_MS = 5000; // 5 seconds.

        public ImsFeatureConnector(int phoneId) {
            mFeatureConnector = ImsManager.getConnector(mContext, phoneId, LOG_TAG, this,
                    mContext.getMainExecutor());
            mFeatureConnector.connect();
        }

        @Override
        public void connectionReady(ImsManager manager, int subId) throws ImsException {
            Log.d(LOG_TAG, "connectionReady");
            if (mQtiImsExtConnector == null) {
                Log.d(LOG_TAG, "connectionReady createQtiImsExtConnector");
                createQtiImsExtConnector(mContext);
                mQtiImsExtConnector.connect();
            }
        }

        @Override
        public void connectionUnavailable(int reason) {
            Log.d(LOG_TAG, "connectionUnavailable");
            if (reason == FeatureConnector.UNAVAILABLE_REASON_SERVER_UNAVAILABLE &&
                    mQtiImsExtConnector == null) {
                removeCallbacks(mFeatureConnectorRunnable);
                postDelayed(mFeatureConnectorRunnable, CONNECTOR_RETRY_DELAY_MS);
            }
        }

        private Runnable mFeatureConnectorRunnable = new Runnable() {
            @Override
            public void run() {
                mFeatureConnector.connect();
            }
        };
    }

    private final BroadcastReceiver mScbmReceiver = new  BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_SMS_CALLBACK_MODE)) {
                int phoneId = intent.getIntExtra(SubscriptionManager.EXTRA_SLOT_INDEX,
                        SubscriptionManager.INVALID_PHONE_INDEX);
                if (!SubscriptionManager.isValidPhoneId(phoneId)) {
                    Log.d(LOG_TAG, "Invalid phoneID");
                    return;
                }
                boolean scbmStatus = intent.getBooleanExtra(EXTRA_SMS_CALLBACK_MODE, false);
                Log.i(LOG_TAG, "ACTION_SMS_CALLBACK_MODE intent received scbmStatus: "
                        + scbmStatus + " phoneId: " + phoneId);
                Phone phone = PhoneFactory.getPhone(phoneId);
                int subId = phone.getSubId();
                if(!isCarrierConfigEnabledScbm(subId) && scbmStatus) {
                    Log.d(LOG_TAG, " SCBM feature not enabled for phoneId: " + phoneId +
                            " sbId: " + subId);
                    return;
                }
                boolean currentStatus = isInScbm();
                setScbmPhoneId(phoneId);
                setIsInScbm(phoneId, scbmStatus);
                if (currentStatus != scbmStatus && isExitScbmFeatureSupported()) {
                  if (scbmStatus) {
                      handleEnterScbm();
                  } else {
                      handleExitScbm();
                  }
                }
            }
        }
    };

    /**
    * To check whether the carrier config KEY_USE_SMS_CALLBACK_MODE_BOOL is enabled.
    * returns true if enabled, false otherwise.
    */
    private boolean isCarrierConfigEnabledScbm(int subId) {
        CarrierConfigManager configManager = (CarrierConfigManager) mContext
                .getSystemService(Context.CARRIER_CONFIG_SERVICE);
        if (configManager == null) {
            return false;
        }
        PersistableBundle b = configManager.getConfigForSubId(subId);
        return (b != null) && (b.getBoolean(CarrierConfigManager.KEY_USE_SMS_CALLBACK_MODE_BOOL));
    }

    private void handleEnterScbm() {
        Log.i(LOG_TAG, "handleEnterScbm");
        // notify change
        sendSmsCallbackModeChange();
    }

    private void sendSmsCallbackModeChange() {
        //Send an Intent
        Intent intent = new Intent(ExtTelephonyManager.ACTION_SMS_CALLBACK_MODE_CHANGED);
        intent.putExtra(ExtTelephonyManager.EXTRA_PHONE_IN_SCM_STATE, isInScbm());
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent, getScbmPhoneId());
        ActivityManager.broadcastStickyIntent(intent, UserHandle.USER_ALL);
        Log.i(LOG_TAG, "sendSmsCallbackModeChange");
    }

    private void handleExitScbm() {
        if (mScbmExitRespRegistrant != null) {
            mScbmExitRespRegistrant.notifyRegistrant();
        }
        Log.i(LOG_TAG, "handleExitScbm");
        sendSmsCallbackModeChange();
        setScbmPhoneId(SubscriptionManager.INVALID_PHONE_INDEX);
    }

    /**
    * To check if the device is in sms callback mode or not.
    */
    public boolean isInScbm() {
        boolean isPhoneInScbmState = false;
        synchronized(this) {
            isPhoneInScbmState = mIsPhoneInScbmState;
        }
        return isPhoneInScbmState ;
    }

    /**
    * To check if the modem is in sms callback mode for PhoneId.
    * Return true if the phone is in SCBM state.
    */
    public boolean isInScbm(int phoneId) {
        boolean status = false;
        synchronized (this) {
         status = (mIsPhoneInScbmState && mScbmPhoneId == phoneId);
        }
        return status;
    }

    private void setIsInScbm(int phoneId, boolean isInScbm) {
        SystemProperties.set("ril.inscbm", String.valueOf(isInScbm));
        synchronized (this) {
            mIsPhoneInScbmState = isInScbm;
            mScbmPhoneId = phoneId;
        }
    }

    private void setScbmPhoneId(int phoneId) {
        synchronized (this) {
            mScbmPhoneId = phoneId;
        }
    }

    private int getScbmPhoneId() {
        int phoneId = SubscriptionManager.INVALID_PHONE_INDEX;
        synchronized (this) {
            phoneId = mScbmPhoneId;
        }
        return phoneId;
    }

    /**
    * To initialize instance of ScbmHandler only if carrier config
    * is enabled to use SCBM feaure.
    */
    public static void init(Context context) {
        if (sScbmHandler != null) {
            Log.d(LOG_TAG, "Scbm Handler already initialized.");
            return;
        }
        sScbmHandler = new ScbmHandler(context);
        Log.i(LOG_TAG, "init(): ScbmHandler initialized.");
    }

    //To get the single instance of ScbmHandler.
    public static ScbmHandler getInstance() {
        if (sScbmHandler == null) {
            throw new RuntimeException("ScbmHandler was not initialized!");
        }
        return sScbmHandler;
    }

    public void setOnScbmExitResponse(Handler h, int what, Object obj) {
         mScbmExitRespRegistrant = new Registrant(h, what, obj);
    }

    public void unsetOnScbmExitResponse(Handler h) {
        mScbmExitRespRegistrant.clear();
    }

    private QtiImsExtListenerBaseImpl imsInterfaceListener =
            new QtiImsExtListenerBaseImpl() {

        @Override
        public void onScbmExited(boolean status) {
            if (!status) {
               Log.d(LOG_TAG, "Exit scbm failed");
               return;
            }
            setIsInScbm(getScbmPhoneId(), false);
            handleExitScbm();
        }
   };

    private void createQtiImsExtConnector(Context context) {
        try {
            mQtiImsExtConnector = new QtiImsExtConnector(context,
                    new QtiImsExtConnector.IListener() {
                @Override
                public void onConnectionAvailable(QtiImsExtManager qtiImsExtManager) {
                    Log.d(LOG_TAG, "onConnectionAvailable");
                    mQtiImsExtManager = qtiImsExtManager;
                    synchronized (this) {
                        if (!mIsExitScbmFeatureRetrieved && mQtiImsExtManager != null) {
                            Phone phone = PhoneFactory.getDefaultPhone();
                            if (phone != null) {
                                try {
                                    mIsExitScbmFeatureSupported =
                                            mQtiImsExtManager.isExitScbmFeatureSupported(
                                                    phone.getPhoneId());
                                    Log.d(LOG_TAG, "isExitScbmFeatureSupported: " +
                                            mIsExitScbmFeatureSupported);
                                    mIsExitScbmFeatureRetrieved = true;
                                } catch (QtiImsException e) {
                                    Log.w(LOG_TAG, "isExitScbmFeatureSupported exception!" +e);
                                }
                            }
                        }
                    }
                    // To handle IMS process crash case.
                    // If QtiImsExtService took time to start after IMS process crash and
                    // FEATUE_READY reported by that time then cannot exit SCBM from
                    // ImsPhoneCallTracker. Hence exit SCBM from here once side car connected.
                    if (isInScbm()) {
                        exitScbm();
                    }
                }

                @Override
                public void onConnectionUnavailable() {
                    mQtiImsExtManager = null;
                }
            });
        } catch (QtiImsException e) {
            Log.e("createQtiImsExtConnector",
                "Unable to create QtiImsExtConnector");
        }
    }

    public void exitScbm() {
        if (mQtiImsExtManager == null ||
                getScbmPhoneId() == SubscriptionManager.INVALID_PHONE_INDEX) {
            Log.d(LOG_TAG, "mQtiImsExtManager is null or SCBM phoneId invalid.");
            return;
        }
        try {
            mQtiImsExtManager.exitScbm(getScbmPhoneId(), imsInterfaceListener);
        } catch (QtiImsException e) {
           Log.w(LOG_TAG, "exitScbm exception!" +e);
        }
    }

    public void handleModemReset() {
        if(isInScbm()) {
            setIsInScbm(getScbmPhoneId(), false);
            handleExitScbm();
        }
    }

    /**
     * Registration point for Scm timer reset
     *
     * @param h handler to notify
     * @param what User-defined message code
     * @param obj placed in Message.obj
     */
    public void registerForScbmTimerReset(Handler h, int what, Object obj) {
        mScmTimerResetRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForScbmTimerReset(Handler h) {
        mScmTimerResetRegistrants.remove(h);
    }

    public boolean isExitScbmFeatureSupported() {
        synchronized (this) {
            if(mIsExitScbmFeatureRetrieved) {
                  return mIsExitScbmFeatureSupported;
            }
        }

        Phone phone = PhoneFactory.getPhone(getScbmPhoneId());
        if (phone != null && mQtiImsExtManager != null) {
            try {
                mIsExitScbmFeatureSupported = mQtiImsExtManager.isExitScbmFeatureSupported(
                        phone.getPhoneId());
                Log.e(LOG_TAG, "isExitScbmFeatureSupported: " + mIsExitScbmFeatureSupported);
                mIsExitScbmFeatureRetrieved = true;
                return mIsExitScbmFeatureSupported;
            } catch (QtiImsException e) {
                Log.w(LOG_TAG, "isExitScbmFeatureSupported exception!" +e);
            }
        }
        return false;
    }

    public boolean isScbmTimerCanceledForEmergency() {
        return mScmCanceledForEmergency;
    }

    private void setScbmTimerCanceledForEmergency(boolean isCanceled) {
        mScmCanceledForEmergency = isCanceled;
    }
}
