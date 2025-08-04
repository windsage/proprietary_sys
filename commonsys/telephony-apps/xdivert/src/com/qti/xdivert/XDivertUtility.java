/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qti.xdivert;

import static java.util.Arrays.copyOf;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telecom.TelecomManager;
import android.telephony.CallForwardingInfo;
import android.telephony.ims.feature.MmTelFeature;
import android.telephony.ims.ImsManager;
import android.telephony.ims.ImsMmTelManager;
import android.telephony.ims.stub.ImsRegistrationImplBase;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.codeaurora.telephony.utils.RegistrantList;

import java.util.function.Consumer;
import java.util.List;

public class XDivertUtility extends BroadcastReceiver {
    static final String LOG_TAG = "XDivertUtility";
    private static final int SLOT_ONE = 0;
    private static final int SLOT_TWO = 1;
    private static final int SIM_RECORDS_LOADED = 1;
    private static final int MESSAGE_SET_CFNR = 2;
    private static final int MESSAGE_SET_CFB = 3;
    private static final int TIME = 20;
    static final int CALL_FORWARD_XDIVERT = 22;
    static final int SINGLE_SIM = 1;
    static final int DUAL_SIM = 2;

    private static final String ID_CHANNEL = "xdivert_channel";
    private static final String SIM_IMSI = "sim_imsi_key";
    private static final String SIM_NUMBER = "sim_number_key";
    private static final String SLOT_KEY  = "slot";

    public static final String LINE1_NUMBERS = "Line1Numbers";
    private static final String XDIVERT_STATUS = "xdivert_status_key";
    private static final RegistrantList sMSimConfigChangeRegistrants = new RegistrantList();

    private static Context mContext;
    protected static XDivertUtility sMe;
    protected NotificationManager mNotificationManager;

    private String[] mImsiFromSim;
    private String[] mStoredImsi;
    private String[] mStoredOldImsi;
    private String[] mLineNumber;
    private String[] mOldLineNumber;

    private int mNumPhones = 0;
    private boolean[] mHasImsiChanged;

    private Handler mXdivHandler;
    private HandlerExecutor mHandlerExecutor;
    private Looper mLooper;
    private TelephonyManager mTelephonyManager;
    private SubscriptionManager mSubscriptionManager;

    public XDivertUtility() {
        sMe = this;
    }

    static XDivertUtility init(Context context) {
        synchronized (XDivertUtility.class) {
            Log.d(LOG_TAG, "init...");
            if (sMe == null) {
                sMe = new XDivertUtility(context);
            } else {
                Log.w(LOG_TAG, "init() called multiple times!  sInstance = " + sMe);
            }
            return sMe;
        }
    }

    private XDivertUtility(Context context) {
        Log.d(LOG_TAG, "onCreate()...");

        mContext = context;
        HandlerThread thread = new HandlerThread("XdivUtilHandlerThread");
        thread.start();
        mLooper = thread.getLooper();
        mXdivHandler = new XdivHandler(mLooper);
        mHandlerExecutor = new HandlerExecutor(mXdivHandler);

        mTelephonyManager = (TelephonyManager) mContext
                .getSystemService(Context.TELEPHONY_SERVICE);
        mSubscriptionManager = mContext.getSystemService(SubscriptionManager.class);
        mNotificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        // if the DB value is not set.
        int status =
                Settings.Secure.getInt(context.getContentResolver(), XDIVERT_STATUS, -1);
        if (status == -1) {
            Settings.Secure.putInt(context.getContentResolver(), XDIVERT_STATUS,
                    getXDivertStatus() ? 1 : 0);
        }

        mNumPhones = mTelephonyManager.getActiveModemCount();
        mImsiFromSim = new String[mNumPhones];
        mStoredImsi = new String[mNumPhones];
        mStoredOldImsi = new String[mNumPhones];
        mLineNumber = new String[mNumPhones];
        mOldLineNumber = new String[mNumPhones];
        mHasImsiChanged = new boolean[mNumPhones];
        if (mNumPhones != SINGLE_SIM) {
            checkIfSimRecordsAreLoaded();
            registerForSubscriptionsChangedListener();
        }
    }

    static XDivertUtility getInstance() {
        return sMe;
    }

    private void registerForSubscriptionsChangedListener() {
        mSubscriptionManager.addOnSubscriptionsChangedListener(mOnSubscriptionsChangeListener);
    }

    private final SubscriptionManager.OnSubscriptionsChangedListener mOnSubscriptionsChangeListener
            = new SubscriptionManager.OnSubscriptionsChangedListener() {
        @Override
        public void onSubscriptionsChanged() {
            if ((mTelephonyManager.getActiveModemCount() > SINGLE_SIM) && (!isAllSubActive())) {
                Log.d(LOG_TAG, " isAllSubActive false");
                onSubscriptionDeactivated();
            }
        }
    };

    private void checkIfSimRecordsAreLoaded() {
        for (int i = 0; i < mNumPhones; i++) {
            int[] subId = mSubscriptionManager.getSubscriptionIds(i);
            if ((subId != null && subId.length > 0) &&
                    mTelephonyManager.createForSubscriptionId(subId[0]).getSimApplicationState() ==
                    TelephonyManager.SIM_STATE_LOADED) {
                mXdivHandler.sendMessage(mXdivHandler.obtainMessage(SIM_RECORDS_LOADED, i, 0));
            }
            mHasImsiChanged[i] = true;
            mStoredOldImsi[i] = getSimImsi(i);
            mOldLineNumber[i] = getNumber(i);

            Log.d(LOG_TAG, "mStoredOldImsi[" + i + "] = " + mStoredOldImsi[i] +
                    "mOldLineNumber[" + i + "] = " + mOldLineNumber[i]);
        }
    }

    /**
     * Receiver for intent broadcasts the XDivertUtility cares about.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.v(LOG_TAG,"onReceive Action intent recieved:"+action);
        int defaultPhoneId = SubscriptionManager.getSlotIndex(
                SubscriptionManager.getDefaultSubscriptionId());
        //gets the slot information ( "0" or "1")
        int slot = intent.getIntExtra(SLOT_KEY, defaultPhoneId);
        if (action.equals(TelephonyManager.ACTION_SIM_APPLICATION_STATE_CHANGED)) {
            int simState = intent.getIntExtra(TelephonyManager.EXTRA_SIM_STATE,
                    TelephonyManager.SIM_STATE_UNKNOWN);
            if (simState == TelephonyManager.SIM_STATE_LOADED
                    && (mTelephonyManager.getActiveModemCount() > SINGLE_SIM)) {
                handleSimRecordsLoaded(slot);
            }
        } else if(action.equals(TelephonyManager.ACTION_MULTI_SIM_CONFIG_CHANGED)) {
            onMultiSimConfigChanged();
        }
    }

    private void onMultiSimConfigChanged() {
        int activeModemCount = mTelephonyManager.getActiveModemCount();
        if (activeModemCount == SINGLE_SIM || ((activeModemCount == DUAL_SIM)
                && (activeModemCount == mNumPhones))) {
            if (activeModemCount !=  DUAL_SIM) notifyOnMultiSimConfigChanged();
            return;
        }

        Log.v(LOG_TAG,"onMultiSimConfigChanged activeModemCount " + activeModemCount);

        mNumPhones = activeModemCount;
        mImsiFromSim = copyOf(mImsiFromSim, mNumPhones);
        mStoredImsi = copyOf(mStoredImsi, mNumPhones);
        mStoredOldImsi = copyOf(mStoredOldImsi, mNumPhones);
        mLineNumber = copyOf(mLineNumber, mNumPhones);
        mOldLineNumber = copyOf(mOldLineNumber, mNumPhones);
        mHasImsiChanged = copyOf(mHasImsiChanged, mNumPhones);

        checkIfSimRecordsAreLoaded();
        registerForSubscriptionsChangedListener();
    }

    static void registerForMultiSimConfigChange(Handler h, int what, Object obj) {
        sMSimConfigChangeRegistrants.addUnique(h, what, obj);
    }

    static void unregisterForMultiSimConfigChange(Handler h) {
        sMSimConfigChangeRegistrants.remove(h);
    }

    private void notifyOnMultiSimConfigChanged() {
        sMSimConfigChangeRegistrants.notifyRegistrants();
    }

    public boolean isSlotActive(int slotId) {
        int[] subId = mSubscriptionManager.getSubscriptionIds(slotId);
        if (subId != null && subId.length > 0) {
            if (SubscriptionManager.isUsableSubscriptionId(subId[0])) {
                return (mTelephonyManager.createForSubscriptionId(subId[0]).getSimState()
                        == TelephonyManager.SIM_STATE_READY);
            } else {
                return false;
            }
        }
        return false;
    }

    public boolean isAllSubActive() {
        for (int i = 0; i < mNumPhones; i++) {
            if (!isSlotActive(i)) return false;
        }
        Log.d(LOG_TAG, " isAllSubActive true");
        return true;
    }

    protected int getNextPhoneId(int phoneId) {
        int nextPhoneId = ++phoneId;
        if (nextPhoneId >= mNumPhones) {
            nextPhoneId = 0;
        }
        return nextPhoneId;
    }

    private boolean isImsiChanged(int phoneId) {
         Log.d(LOG_TAG, "isImsiChanged  phoneId: " + phoneId);
        if ((mStoredOldImsi[phoneId] != null) &&
                (mImsiFromSim[phoneId] != null) &&
                mImsiFromSim[phoneId].equals(mStoredOldImsi[phoneId])) {
            Log.d(LOG_TAG, "isImsiChanged: not changed ");
            return false;
        }
        int nPhoneId = getNextPhoneId(phoneId);
        if ((mStoredOldImsi[nPhoneId] != null) &&
                (mImsiFromSim[phoneId] != null) &&
                mImsiFromSim[phoneId].equals(mStoredOldImsi[nPhoneId])) {
            Log.d(LOG_TAG, "isImsiChanged: not changed Matched with other slot");
            setSimImsi(mStoredOldImsi[nPhoneId], phoneId);
            storeNumber(mOldLineNumber[nPhoneId], phoneId);
            if (mImsiFromSim[nPhoneId] == null) {
                Log.d(LOG_TAG, "isImsiChanged: updatting other SIM values");
                setSimImsi(mStoredOldImsi[phoneId], nPhoneId);
                storeNumber(mOldLineNumber[phoneId], nPhoneId);
            }

            return false;
        }
        Log.d(LOG_TAG, "isImsiChanged: TRUE ");
       return true;
    }

    void updateOldValues() {
        for (int i = 0; i < mNumPhones; i++) {
            mStoredOldImsi[i] = mImsiFromSim[i];
            mOldLineNumber[i] = mLineNumber[i];

            Log.d(LOG_TAG, "mStoredOldImsi[" + i + "] = " + mStoredOldImsi[i] +
                    " mOldLineNumber[" + i + "] = " + mOldLineNumber[i]);
        }
    }

    void handleSimRecordsLoaded(int slot) {
        boolean status = false;
        int phoneId = slot;
        int nPhoneId = getNextPhoneId(phoneId);
        Log.d(LOG_TAG, "phoneId = " + phoneId);
        Log.d(LOG_TAG, "nPhoneId = " + nPhoneId);
        // Get the Imsi value from the SIM records. Retrieve the stored Imsi
        // value from the shared preference. If both are same, then read the
        // stored phone number from the shared preference, else prompt the
        // user to enter them.
        int[] subID = mSubscriptionManager.getSubscriptionIds(phoneId);
        if (subID != null && subID.length > 0) {
            mImsiFromSim[phoneId] =
                    mTelephonyManager.createForSubscriptionId(subID[0]).getSubscriberId();
        }
        mStoredImsi[phoneId] = getSimImsi(phoneId);
        Log.d(LOG_TAG, "SIM_RECORDS_LOADED mImsiFromSim = " +
                mImsiFromSim[phoneId] + " mStoredImsi = " +
                mStoredImsi[phoneId]);
        if (!isImsiChanged(phoneId)) {
            // Imsi from SIM matches the stored Imsi so get the stored lineNumbers
            mLineNumber[phoneId] = getNumber(phoneId);
            mHasImsiChanged[phoneId] = false;
            Log.d(LOG_TAG, "Stored Line Number = " + mLineNumber[phoneId]);
        } else {
            mHasImsiChanged[phoneId] = true;
        }
        Log.v(LOG_TAG, "mHasImsiChanged[" + phoneId + "]: " + mHasImsiChanged[phoneId]
                + " mHasImsiChanged[" + nPhoneId + "]: " + mHasImsiChanged[nPhoneId]);;
        // Only if Imsi has not changed, query for XDivert status from shared pref
        // and update the notification bar.
        if ((!mHasImsiChanged[phoneId]) && (!mHasImsiChanged[nPhoneId])) {
            updateOldValues();
            status = getXDivertStatus();
            onXDivertChanged(status);
        } else {
            int disablePhoneId = 0;
            boolean disableReq = false;
            boolean updateStatus = false;
            if (isImsiChanged(phoneId)) {
                // Imsi from SIM does not match the stored Imsi.
                // Hence reset the values.
                updateStatus = true;
                setSimImsi(mImsiFromSim[phoneId], phoneId);
                storeNumber(null, phoneId);
            }
            if (!mHasImsiChanged[phoneId] && mImsiFromSim[nPhoneId] != null
                    && mHasImsiChanged[nPhoneId]) {
                disableReq = true;
                disablePhoneId = phoneId;
            }
            if (mHasImsiChanged[phoneId] && mImsiFromSim[nPhoneId] != null
                    && !mHasImsiChanged[nPhoneId]) {
                disableReq = true;
                disablePhoneId = nPhoneId;
            }
            Log.d(LOG_TAG,"Handle next SIM status");
            int state = getSimState(nPhoneId);
                Log.v(LOG_TAG,"Handle next SIM status state: " + state);
            if (state == TelephonyManager.SIM_STATE_ABSENT && !mHasImsiChanged[phoneId]) {
                disableReq = true;
                disablePhoneId = phoneId;
                updateOldValues();
            }

            Log.d(LOG_TAG,"disableReq: " + disableReq + " disablePhoneId: " + disablePhoneId);

            if (disableReq && getXDivertStatus()) {
                updateStatus = true;
                CallForwardingInfo callForwardingInfoasNr = new CallForwardingInfo(false
                        /*enabled*/, CallForwardingInfo.REASON_NOT_REACHABLE,
                        mLineNumber[phoneId], TIME);
                mXdivHandler.sendMessage(mXdivHandler.obtainMessage(MESSAGE_SET_CFNR,
                        disablePhoneId, 0, callForwardingInfoasNr));
            }
            if (updateStatus) {
                setXDivertStatus(false);
            }
            if (mImsiFromSim[phoneId] != null && mImsiFromSim[nPhoneId] != null) {
                updateOldValues();
            }
        }
    }

    class XdivHandler extends Handler {

        XdivHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(LOG_TAG," XdivHandler msg: " +msg.what);
            switch (msg.what) {
                case SIM_RECORDS_LOADED:
                    handleSimRecordsLoaded(msg.arg1);
                    break;
                case MESSAGE_SET_CFNR:
                    Log.d(LOG_TAG, "MESSAGE_SET_CFNR");
                    handleSetCFNRC(msg.arg1, (CallForwardingInfo)msg.obj);
                    break;
                case MESSAGE_SET_CFB:
                    Log.d(LOG_TAG, "MESSAGE_SET_CFB");
                    handleSetCFB(msg.arg1, (CallForwardingInfo)msg.obj);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }

        private void handleSetCFNRC(int phoneId, CallForwardingInfo cfi) {
            int[] subId = mSubscriptionManager.getSubscriptionIds(phoneId);
            if (subId != null && subId.length > 0) {
                TelephonyManager tm = mTelephonyManager.createForSubscriptionId(subId[0]);
                if (tm != null) {
                    tm.setCallForwarding(cfi, mHandlerExecutor, new Consumer<Integer>() {
                            @Override
                            public void accept(Integer result) {
                                if (result == TelephonyManager.CallForwardingInfoCallback.
                                        RESULT_SUCCESS) {
                                    if (isValidImsPhoneId(phoneId)) {
                                        resetCFB(phoneId);
                                        return;
                                    }
                                    setXDivertStatus(false);
                                }
                            }
                    });
                }
            }
        }

        private void handleSetCFB(int phoneId, CallForwardingInfo cfi) {
            int[] subId = mSubscriptionManager.getSubscriptionIds(phoneId);
            if (subId != null && subId.length > 0) {
                TelephonyManager tm = mTelephonyManager.createForSubscriptionId(subId[0]);
                if (tm != null) {
                    tm.setCallForwarding(cfi, mHandlerExecutor, new Consumer<Integer>() {
                            @Override
                            public void accept(Integer result) {
                                if (result == TelephonyManager.CallForwardingInfoCallback.
                                        RESULT_SUCCESS) {
                                    setXDivertStatus(false);
                                }
                            }
                    });
                }
            }
        }
    }


    protected boolean checkImsiReady() {
        for (int i = 0; i < mNumPhones; i++) {
            mStoredImsi[i] = getSimImsi(i);
            int[] subId = mSubscriptionManager.getSubscriptionIds(i);
            if (subId != null && subId.length > 0) {
                mImsiFromSim[i] =
                        mTelephonyManager.createForSubscriptionId(subId[0]).getSubscriberId();
            }
            // if imsi is not yet read, then above api returns ""
            if ((mImsiFromSim[i] == null)  || (mImsiFromSim[i] == "")) {
                return false;
            } else if ((mStoredImsi[i] == null) || ((mImsiFromSim[i] != null)
                    && (!mImsiFromSim[i].equals(mStoredImsi[i])))) {
                // Imsi from SIM does not match the stored Imsi.
                // Hence reset the values.
                setXDivertStatus(false);
                setSimImsi(mImsiFromSim[i], i);
                storeNumber(null, i);
                mHasImsiChanged[i] = true;
            }
        }
        return true;
    }

    // returns the stored Line Numbers
    public String[] getLineNumbers() {
        return mLineNumber;
    }

    // returns the stored Line Numbers
    public String[] setLineNumbers(String[] lineNumbers) {
        return mLineNumber = lineNumbers;
    }

    // returns the stored Imsi from shared preference
    protected String getSimImsi(int subscription) {
        Log.d(LOG_TAG, "getSimImsi sub = " + subscription);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(
                mContext);
        return sp.getString(SIM_IMSI + subscription, null);
    }

    // saves the Imsi to shared preference
    protected void setSimImsi(String imsi, int subscription) {
        Log.d(LOG_TAG, "setSimImsi imsi = " + imsi + "sub = " + subscription);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(SIM_IMSI + subscription, imsi);
        editor.apply();
    }

    int getSimState (int phoneId) {
        return mTelephonyManager.getSimState(phoneId);
    }

    // On Subscription deactivation, clear the Xdivert icon from
    // notification bar
    private void onSubscriptionDeactivated() {
        if (getXDivertStatus()) {
            int phoneId = 0;
            boolean disableReq = false;
            int disablePhoneId = 0;
            int nPhoneId = getNextPhoneId(phoneId);
            int state = getSimState(phoneId);
            int nState = getSimState(nPhoneId);
            Log.d(LOG_TAG,"onSubscriptionDeactivated SIM state: " + state
                    + " nState: " + nState);
            if ((state == TelephonyManager.SIM_STATE_READY ||
                    state == TelephonyManager.SIM_STATE_LOADED)
                    && nState == TelephonyManager.SIM_STATE_ABSENT) {
                disablePhoneId =  phoneId;
                disableReq = true;

            } else if (state == TelephonyManager.SIM_STATE_ABSENT
                    && (nState == TelephonyManager.SIM_STATE_READY ||
                    nState == TelephonyManager.SIM_STATE_LOADED)) {
                disablePhoneId =  nPhoneId;
                disableReq = true;
            }
            if (disableReq) {
                CallForwardingInfo callForwardingInfoasNr = new CallForwardingInfo(false
                        /*enabled*/, CallForwardingInfo.REASON_NOT_REACHABLE,
                        mLineNumber[disablePhoneId], TIME);
                mXdivHandler.sendMessage(mXdivHandler.obtainMessage(MESSAGE_SET_CFNR,
                        disablePhoneId, 0, callForwardingInfoasNr));

                int temp = getNextPhoneId(disablePhoneId);
                mLineNumber[temp] = null;
                mImsiFromSim[temp] = null;
                updateOldValues();
            }
            onXDivertChanged(false);
        }
    }

    // returns the stored Line Numbers from shared preference
    protected String getNumber(int subscription) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(
                mContext);
        return sp.getString(SIM_NUMBER + subscription, null);
    }

    // saves the Line Numbers to shared preference
    protected void storeNumber(String number, int subscription) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(
                mContext);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(SIM_NUMBER + subscription, number);
        editor.apply();

        // Update the lineNumber which will be passed to XDivertPhoneNumbers
        // to populate the number from next time.
        mLineNumber[subscription] = number;
    }
    protected void onXDivertChanged(boolean visible) {
         Log.d(LOG_TAG, "onXDivertChanged(): " + visible);
        updateXDivert(visible);
    }

    // Gets the XDivert Status from shared preference.
    protected boolean getXDivertStatus() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(
                mContext);
        boolean status = sp.getBoolean(XDIVERT_STATUS, false);
        Log.d(LOG_TAG, "getXDivertStatus status = " + status);
        return status;
    }

    // Sets the XDivert Status to shared preference.
    protected void setXDivertStatus(boolean status) {
        Log.d(LOG_TAG, "setXDivertStatus status = " + status);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(
                mContext);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(XDIVERT_STATUS, status);
        editor.apply();
        Settings.Secure.putInt(mContext.getContentResolver(), XDIVERT_STATUS, status ? 1 : 0);
    }
    /**
     * Updates the XDivert indicator notification.
     *
     * @param visible true if XDivert is enabled.
     */
    /* package */ void updateXDivert(boolean visible) {
        Log.d(LOG_TAG, "updateXDivert: " + visible);
        if (visible) {
            Intent intent = new Intent(TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setClassName("com.android.phone",
                    "com.android.phone.settings.PhoneAccountSettingsActivity");
            int resId = R.drawable.stat_sys_phone_call_forward_xdivert;
            PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent,
                PendingIntent.FLAG_IMMUTABLE);
            String title = mContext.getString(R.string.xdivert_title);
            NotificationChannel notificationChannel = new NotificationChannel(ID_CHANNEL, title,
                    NotificationManager.IMPORTANCE_DEFAULT);
            mNotificationManager.createNotificationChannel(notificationChannel);

            Notification notification = new Notification.Builder(mContext)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(resId)
                    .setContentTitle(title)
                    .setContentText(mContext.getString(R.string.sum_xdivert_enabled))
                    .setContentIntent(pendingIntent)
                    .setChannelId(ID_CHANNEL)
                    .setOngoing(true)
                    .build();
            mNotificationManager.notify(CALL_FORWARD_XDIVERT, notification);
        } else {
            mNotificationManager.cancel(CALL_FORWARD_XDIVERT);
        }
    }

    void resetCFB(int phoneId) {
        CallForwardingInfo callForwardingInfobusy = new CallForwardingInfo
                (false /*enabled*/, CallForwardingInfo.REASON_BUSY, null, TIME);
        mXdivHandler.sendMessage(mXdivHandler.obtainMessage(MESSAGE_SET_CFB,
                phoneId, 0, callForwardingInfobusy));
    }

    /**
     * Check is carrier one supported or not
     */
    public static boolean isCarrierOneSupported() {
        String property = SystemProperties.get("persist.vendor.radio.atel.carrier");
        return "405854".equals(property);
    }

    boolean isValidImsPhoneId(int phoneId) {
        if (!isCarrierOneSupported()) {
            return false;
        }

        int[] subId = mSubscriptionManager.getSubscriptionIds(phoneId);

        if (subId != null && subId.length > 0) {
            ImsManager imsManager =
                    (ImsManager) mContext.getSystemService(Context.TELEPHONY_IMS_SERVICE);

            if (imsManager == null) {
                return false;
            }

            ImsMmTelManager mImsMmTelManager = imsManager.getImsMmTelManager(subId[0]);
            if (mImsMmTelManager != null) {
                if (mImsMmTelManager.isAvailable(MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_UT,
                        ImsRegistrationImplBase.REGISTRATION_TECH_LTE)
                        || mImsMmTelManager.isAvailable(MmTelFeature
                                .MmTelCapabilities.CAPABILITY_TYPE_UT,
                        ImsRegistrationImplBase.REGISTRATION_TECH_IWLAN)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void setupEdgeToEdge(Activity activity) {
        if (activity == null) return;
        ViewCompat.setOnApplyWindowInsetsListener(activity.findViewById(android.R.id.content),
                (v, windowInsets) -> {
                    Insets insets = windowInsets.getInsets(
                            WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime()
                            | WindowInsetsCompat.Type.displayCutout());
                    // Apply the insets paddings to the view.
                    v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
                    // Return CONSUMED if you don't want the window insets to keep being
                    // passed down to descendant views.
                    return WindowInsetsCompat.CONSUMED;
                });
    }
}
