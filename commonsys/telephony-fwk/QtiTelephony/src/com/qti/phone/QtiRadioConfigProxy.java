/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qti.phone;

import android.R.drawable;
import static com.qti.extphone.ExtTelephonyManager.FEATURE_SMART_TEMP_DDS_VIA_RADIO_CONFIG;
import static com.qti.extphone.ExtTelephonyManager.FEATURE_EMERGENCY_ENHANCEMENT;

import android.app.StatusBarManager;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;

import com.qti.extphone.Client;
import com.qti.extphone.DualDataRecommendation;
import com.qti.extphone.ExtPhoneCallbackListener;
import com.qti.extphone.IExtPhoneCallback;
import com.qti.extphone.MsimPreference;
import com.qti.extphone.QtiSimType;
import com.qti.extphone.Status;
import com.qti.extphone.Token;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import vendor.qti.hardware.radio.qtiradioconfig.SimTypeInfo;

public class QtiRadioConfigProxy {

    private static final String TAG = "QtiRadioConfigProxy";

    private final int EVENT_GET_SECURE_MODE_STATUS_RESPONSE = 1;
    private final int EVENT_ON_SECURE_MODE_STATUS_CHANGE = 2;
    private final int EVENT_ON_MSIM_PREFERENCE_RESPONSE = 3;
    private final int EVENT_ON_GET_SIM_TYPE_INFO_RESPONSE = 4;
    private final int EVENT_ON_SET_SIM_TYPE_RESPONSE = 5;
    private final int EVENT_ON_CIWLAN_CAPABILITY_CHANGE = 6;
    public  final int EVENT_ON_DUAL_DATA_CAPABILITY_CHANGED = 7;
    public  final int EVENT_SET_DUAL_DATA_USER_PREFERENCE_RESPONSE = 8;
    public  final int EVENT_ON_DUAL_DATA_RECOMMENDATION = 9;
    public  final int EVENT_ON_DDS_SWITCH_CAPABILITY_CHANGED = 10;
    public  final int EVENT_ON_DDS_SWITCH_CRITERIA_CHANGED = 11;
    public  final int EVENT_ON_DDS_SWITCH_RECOMMENDATION = 12;
    public  final int EVENT_ON_ALLOW_MODEM_RECOMMENDATION_FOR_DATA_DURING_CALL = 13;

    private final String SECURE_MODE_STATUS_BAR_ICON = "secure_mode";

    private static Context mContext;
    private StatusBarManager mStatusBarManager;
    private IQtiRadioConfigConnectionInterface mQtiRadioConfig;
    private HandlerThread mWorkerThread = new HandlerThread(TAG + "BgThread");
    private Handler mWorkerThreadHandler;
    private ConcurrentHashMap<Integer, Transaction> mInflightRequests =
            new ConcurrentHashMap<Integer, Transaction>();
    private volatile int mSerial = -1;
    private static boolean mIsInSecureMode = false;
    private ArrayList<IQtiRadioConfigInternalCallback> mInternalCallbackList = new
            ArrayList<IQtiRadioConfigInternalCallback>();
    private ExtTelephonyServiceImpl mExtTelephonyServiceImpl;
    private CiwlanCapability mCiwlanCapability;
    private SubscriptionManager mSubscriptionManager;
    private TelephonyManager mTelephonyManager;
    private AtomicBoolean mDualDataCapability = new AtomicBoolean(false);

    private boolean SUCCESS = true;
    private boolean FAILED = false;

    public QtiRadioConfigProxy(Context context, ExtTelephonyServiceImpl extTelephonyServiceImpl) {
        this(context);
        setExtTelephonyServiceImpl(extTelephonyServiceImpl);
    }

    /**
     * This constructor should only be used for unit tests.
     * If using this constructor, invoke setExtTelephonyServiceImpl
     * to set ExtTelephonyServiceImpl object.
     */
    @VisibleForTesting
    public QtiRadioConfigProxy(Context context) {
        mContext = context;
        mQtiRadioConfig = QtiRadioConfigFactory.makeQtiRadioConfig(context);
        mQtiRadioConfig.registerCallback(mQtiRadioConfigCallback);
        mStatusBarManager = context.getSystemService(StatusBarManager.class);
        mSubscriptionManager = context.getSystemService(SubscriptionManager.class);
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        mWorkerThread.start();
        setLooper(mWorkerThread.getLooper());
    }

    public int getHalVersion() {
        return mQtiRadioConfig.getHalVersion();
    }

    @VisibleForTesting
    void setExtTelephonyServiceImpl(ExtTelephonyServiceImpl extTelephonyServiceImpl) {
        Log.d(TAG, "setExtTelephonyServiceImpl");
        mExtTelephonyServiceImpl = extTelephonyServiceImpl;
    }

    private void setLooper(Looper workerLooper) {
        mWorkerThreadHandler = new QtiRadioConfigProxy.WorkerHandler(workerLooper);
    }

    public boolean isFeatureSupported(int feature) {
        return mQtiRadioConfig.isFeatureSupported(feature);
    }

    private class WorkerHandler extends Handler {
        private static final String TAG = QtiRadioConfigProxy.TAG + "Handler";

        public WorkerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "handleMessage msg.what = " + msg.what);
            Result result = (Result) msg.obj;
            switch (msg.what) {
                case EVENT_GET_SECURE_MODE_STATUS_RESPONSE:
                    Log.d(TAG, "EVENT_GET_SECURE_MODE_STATUS_RESPONSE");
                    getSecureModeStatusResponse(result.mToken, result.mStatus,
                            (boolean) result.mData);
                    break;

                case EVENT_ON_SECURE_MODE_STATUS_CHANGE:
                    Log.d(TAG, "EVENT_ON_SECURE_MODE_STATUS_CHANGE");
                    onSecureModeStatusChange(result.mToken, (boolean) result.mData);
                    break;

                case EVENT_ON_MSIM_PREFERENCE_RESPONSE:
                    Log.d(TAG, "EVENT_ON_MSIM_PREFERENCE_RESPONSE");
                    setMsimPreferenceResponse(result.mToken, result.mStatus);
                    break;

                case EVENT_ON_GET_SIM_TYPE_INFO_RESPONSE:
                    Log.d(TAG, "EVENT_ON_GET_SIM_TYPE_INFO_RESPONSE");
                    onGetSimTypeInfoResponse(result.mToken,
                            result.mStatus, (SimTypeInfo[]) result.mData);
                    break;

                case EVENT_ON_SET_SIM_TYPE_RESPONSE:
                    Log.d(TAG, "EVENT_ON_SET_SIM_TYPE_RESPONSE");
                    onSetSimTypeResponse(result.mToken, result.mStatus);
                    break;

                case EVENT_ON_CIWLAN_CAPABILITY_CHANGE:
                    Log.d(TAG, "EVENT_ON_CIWLAN_CAPABILITY_CHANGE");
                    onCiwlanCapabilityChanged(result.mToken, result.mStatus,
                            (CiwlanCapability) result.mData);
                    break;

                case EVENT_ON_DUAL_DATA_CAPABILITY_CHANGED:
                    Log.d(TAG, "EVENT_ON_DUAL_DATA_CAPABILITY_CHANGED");
                    onDualDataCapabilityChanged(result.mToken, result.mStatus,
                            (boolean) result.mData);
                    break;

                case EVENT_SET_DUAL_DATA_USER_PREFERENCE_RESPONSE:
                    Log.d(TAG, "EVENT_SET_DUAL_DATA_USER_PREFERENCE_RESPONSE");
                    setDualDataUserPreferenceResponse(result.mToken, result.mStatus);
                    break;

                case EVENT_ON_DUAL_DATA_RECOMMENDATION:
                    Log.d(TAG, "EVENT_ON_DUAL_DATA_RECOMMENDATION");
                    onDualDataRecommendation(result.mToken,
                            (DualDataRecommendation ) result.mData);
                    break;

                case EVENT_ON_DDS_SWITCH_CAPABILITY_CHANGED:
                    Log.d(TAG, "EVENT_ON_DDS_SWITCH_CAPABILITY_CHANGED");
                    onDdsSwitchCapabilityChanged(result.mToken, result.mStatus,
                            (boolean) result.mData);
                    break;

                case EVENT_ON_DDS_SWITCH_CRITERIA_CHANGED: {
                    Log.d(TAG, "EVENT_ON_DDS_SWITCH_CRITERIA_CHANGED");
                    onDdsSwitchCriteriaChanged(result.mToken, (boolean) result.mData);
                    break;
                }

                case EVENT_ON_DDS_SWITCH_RECOMMENDATION: {
                    Log.d(TAG, "EVENT_ON_DDS_SWITCH_RECOMMENDATION");
                    onDdsSwitchRecommendation(result.mToken, (int) result.mData);
                    break;
                }

                case EVENT_ON_ALLOW_MODEM_RECOMMENDATION_FOR_DATA_DURING_CALL: {
                    Log.d(TAG, "EVENT_ON_ALLOW_MODEM_RECOMMENDATION_FOR_DATA_DURING_CALL");
                    onSendUserPreferenceForDataDuringVoiceCall(result.mToken,
                            result.mStatus);
                    break;
                }
            }
        }
    }

    public Token getSecureModeStatus(Client client) throws RemoteException {
        Log.d(TAG, "getSecureModeStatus");
        if (!mExtTelephonyServiceImpl.isClientValid(client)) return null;
        Token token = getNextToken();
        mInflightRequests.put(token.get(), new Transaction(token, "getSecureModeStatus", client));
        mQtiRadioConfig.getSecureModeStatus(token);
        return token;
    }

    public Token setMsimPreference(Client client, MsimPreference pref)
            throws RemoteException {
        Log.d(TAG, "setMsimPreference: MsimPreference = " + pref.get());
        if (!mExtTelephonyServiceImpl.isClientValid(client)) return null;
        Token token = getNextToken();
        mInflightRequests.put(token.get(), new Transaction(token, "setMsimPreference",
                client));
        mQtiRadioConfig.setMsimPreference(token, pref);
        return token;
    }

    public Token setSimType(Client client, QtiSimType[] simType) throws RemoteException {
        Log.d(TAG, "setSimType: simType = " + simType);
        if (!mExtTelephonyServiceImpl.isClientValid(client)) return null;

        Token token = getNextToken();
        mInflightRequests.put(token.get(), new Transaction(token, "setSimType", client));
        mQtiRadioConfig.setSimType(token, simType);
        return token;
    }

    public void setMsimPreference(MsimPreference pref) throws RemoteException {
        mQtiRadioConfig.setMsimPreference(getNextToken(), pref);
    }

    public Token getSimTypeInfo() throws RemoteException {
        Token token = getNextToken();
        Log.d(TAG, "getSimTypeInfo: token = " + token);

        mQtiRadioConfig.getSimTypeInfo(token);
        return token;
    }

    public Token setSimType(QtiSimType[] simType) throws RemoteException {
        Token token = getNextToken();

        mQtiRadioConfig.setSimType(token, simType);
        return token;
    }

    public Optional<Integer> getCiwlanCapabilityFromCache() {
        if (mCiwlanCapability == null) {
            return Optional.empty();
        }

        return mCiwlanCapability.getCiwlanCapability();
    }

    public void invalidateCiwlanCapabilityCache() {
        mCiwlanCapability = null;
    }

    /**
     * Since "Client" is not passed as a parameter, response can not be sent but stores in
     * the current class and it can be queried with getCiwlanCapabilityFromCache().
     */
    public void getCiwlanCapability() throws RemoteException {
        Token token = getNextToken();
        mQtiRadioConfig.getCiwlanCapability(token);
    }

    /**
     * Since "Client" is not passed as a parameter, response can not be sent but stores in
     * the current class and it can be queried with getCiwlanCapabilityFromCache().
     */
    public void requestForDualDataCapability() throws RemoteException {
        Token token = getNextToken();
        mQtiRadioConfig.getDualDataCapability(token);
    }

    public void invalidateDualDataCapabilityCache() {
        mDualDataCapability.set(false);
    }

    public boolean getDualDataCapabilityFromCache() {
        return mDualDataCapability.get();
    }

    public Token setDualDataUserPreference(Client client, boolean preference)
            throws RemoteException {
        if (!mExtTelephonyServiceImpl.isClientValid(client)) return null;
        Token token = getNextToken();
        mInflightRequests.put(token.get(), new Transaction(token, "setDualDataUserPreference",
                client));
        mQtiRadioConfig.setDualDataUserPreference(token, preference);
        return token;
    }

    public Token getDdsSwitchCapability(Client client) throws RemoteException {
        Log.d(TAG, "getDdsSwitchCapability");
        if (!mExtTelephonyServiceImpl.isClientValid(client)) {
            return null;
        }
        Token token = getNextToken();
        mInflightRequests.put(token.get(), new Transaction(token, "getDdsSwitchCapability",
                client));
        mQtiRadioConfig.getDdsSwitchCapability(token);
        return token;
    }

    public Token sendUserPreferenceForDataDuringVoiceCall(boolean[] isAllowedOnSlot,
            Client client) throws RemoteException {
        Log.d(TAG, "sendUserPreferenceForDataDuringVoiceCall");
        if (!mExtTelephonyServiceImpl.isClientValid(client)) {
            return null;
        }
        Token token = getNextToken();
        mInflightRequests.put(token.get(), new Transaction(token,
                "sendUserPreferenceForDataDuringVoiceCall", client));
        mQtiRadioConfig.sendUserPreferenceForDataDuringVoiceCall(token, isAllowedOnSlot);
        return token;
    }

    class Result {
        Token mToken;
        Status mStatus;
        Object mData;

        public Result(Token mToken, Status mStatus, Object mData) {
            this.mToken = mToken;
            this.mStatus = mStatus;
            this.mData = mData;
        }

        @Override
        public String toString() {
            return "Result{" + "mToken=" + mToken + ", mStatus=" + mStatus + ", mData=" + mData +
                    '}';
        }
    }

    class Transaction {
        Token mToken;
        String mName;
        Client mClient;

        public Transaction(Token token, String name, Client client) {
            mToken = token;
            mName = name;
            mClient = client;
        }

        @Override
        public String toString() {
            return "Transaction{" + "mToken=" + mToken + ", mName='" + mName + '\'' + ", mClient="
                    + mClient + '}';
        }
    }

    private Token getNextToken() {
        return new Token(++mSerial);
    }

    IQtiRadioConfigConnectionCallback mQtiRadioConfigCallback =
            new IQtiRadioConfigConnectionCallback() {

        public void getSecureModeStatusResponse(Token token, Status error, boolean enabled) {
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_GET_SECURE_MODE_STATUS_RESPONSE, new Result(token, error, enabled)));
        }

        @Override
        public void onSecureModeStatusChange(Token token, boolean enabled) {
            Log.d(TAG, "onSecureModeStatusChange enabled = " + enabled);
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_ON_SECURE_MODE_STATUS_CHANGE, new Result(token, null, enabled)));
        }

        @Override
        public void setMsimPreferenceResponse(Token token, Status status) {
            Log.d(TAG, "setMsimPreferenceResponse status = " + status);
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_ON_MSIM_PREFERENCE_RESPONSE, new Result(token, status, null)));
        }

        @Override
        public void getSimTypeInfoResponse(Token token, Status status, SimTypeInfo[] simTypeInfo) {
            Log.d(TAG, "getSimTypeInfoResponse status = " + status);

            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_ON_GET_SIM_TYPE_INFO_RESPONSE, new Result(token, status, simTypeInfo)));
        }

        @Override
        public void setSimTypeResponse(Token token, Status status) {
            Log.d(TAG, "setSimTypeResponse status = " + status);
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_ON_SET_SIM_TYPE_RESPONSE, new Result(token, status, null)));
        }

        @Override
        public void onDualDataCapabilityChanged(Token token, Status status, boolean support) {
            mDualDataCapability.set(support);
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_ON_DUAL_DATA_CAPABILITY_CHANGED, new Result(token, status, support)));
        }

        @Override
        public void setDualDataUserPreferenceResponse(Token token, Status status) {
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_SET_DUAL_DATA_USER_PREFERENCE_RESPONSE,
                    new Result(token, status, null)));
        }

        @Override
        public void onDualDataRecommendation(Token token, DualDataRecommendation rec) {
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_ON_DUAL_DATA_RECOMMENDATION, new Result(token, null, rec)));
        }

        @Override
        public void onCiwlanCapabilityChanged(Token token, Status status,
                CiwlanCapability capability) {
            Log.d(TAG, "onCiwlanCapabilityChanged CiwlanCapability = " + capability);
            mCiwlanCapability = capability;
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_ON_CIWLAN_CAPABILITY_CHANGE, new Result(token, status, capability)));
        }

        @Override
        public void onDdsSwitchCapabilityChanged(Token token, Status status, boolean support) {
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_ON_DDS_SWITCH_CAPABILITY_CHANGED, new Result(token, status, support)));
        }

        @Override
        public void onDdsSwitchCriteriaChanged(Token token, boolean telephonyDdsSwitch) {
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_ON_DDS_SWITCH_CRITERIA_CHANGED,
                    new Result(token, null, telephonyDdsSwitch)));
        }

        @Override
        public void onDdsSwitchRecommendation(Token token, int recommendedSlotId) {
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_ON_DDS_SWITCH_RECOMMENDATION,
                    new Result(token, null, recommendedSlotId)));
        }

        @Override
        public void onSendUserPreferenceForDataDuringVoiceCall(Token token, Status status) {
            mWorkerThreadHandler.sendMessage(mWorkerThreadHandler.obtainMessage
                    (EVENT_ON_ALLOW_MODEM_RECOMMENDATION_FOR_DATA_DURING_CALL,
                    new Result(token, status, null)));
        }
    };

    public void removeClientFromInflightRequests(IExtPhoneCallback callback) {
        for (int key : mInflightRequests.keySet()) {
            Transaction txn = mInflightRequests.get(key);
            if (txn.mClient.getCallback().asBinder() == callback.asBinder()) {
                Log.d(TAG, "removeClientFromInflightRequests: Token = " + key + " => " +
                        mInflightRequests.get(key));
                mInflightRequests.remove(key);
            }
        }
    }

    ArrayList<IExtPhoneCallback> retrieveCallbacks(int tokenKey, int event) {
        ArrayList<IExtPhoneCallback> list = new ArrayList<IExtPhoneCallback>();
        if (tokenKey != Token.UNSOL) {
            if (mInflightRequests.containsKey(tokenKey)) {
                Transaction txn = mInflightRequests.get(tokenKey);
                Client client = txn.mClient;
                if (mExtTelephonyServiceImpl.isClientValid(client)) {
                    list.add(client.getCallback());
                } else {
                    Log.e(TAG, "This client is invalid now: " + client);
                }
            }
        } else {
            list = mExtTelephonyServiceImpl.retrieveCallbacksWithEvent(tokenKey, event);
        }

        return list;
    }

    /* Private delegates */
    private void getSecureModeStatusResponse(Token token, Status error, boolean enabled) {
        try {
            int tokenKey = token.get();
            for (IExtPhoneCallback callback : retrieveCallbacks(tokenKey,
                    ExtPhoneCallbackListener.EVENT_GET_SECURE_MODE_STATUS_RESPONSE)) {
                Log.d(TAG, "getSecureModeStatusResponse: Responding back for transaction = " +
                        mInflightRequests.get(tokenKey));
                callback.getSecureModeStatusResponse(token, error, enabled);
                mInflightRequests.remove(tokenKey);
            }
        } catch (RemoteException ex) {
            Log.e(TAG, "getSecureModeStatusResponse: Exception", ex);
        } finally {
            mIsInSecureMode = enabled;
            toggleSecureModeIcon(enabled);
        }
    }

    private void onSecureModeStatusChange(Token token, boolean enabled) {
        try {
            int tokenKey = token.get();
            for (IExtPhoneCallback callback : retrieveCallbacks(tokenKey,
                    ExtPhoneCallbackListener.EVENT_ON_SECURE_MODE_STATUS_CHANGE)) {
                Log.d(TAG, "onSecureModeStatusChange: Responding back for transaction = " +
                        mInflightRequests.get(tokenKey));
                callback.onSecureModeStatusChange(enabled);
                mInflightRequests.remove(tokenKey);
            }
        } catch (RemoteException ex) {
            Log.e(TAG, "onSecureModeStatusChange: Exception", ex);
        } finally {
            mIsInSecureMode = enabled;
            toggleSecureModeIcon(enabled);
        }
    }

    private void toggleSecureModeIcon(boolean show) {
        boolean canShowIcon = mContext.getResources().getBoolean(R.bool.show_secure_mode_icon);
        Log.d(TAG, "Secure Mode status bar icon disabled from config.");
        if (canShowIcon) {
            if (show) {
                Log.d(TAG, "Showing the Secure Mode icon");
                mStatusBarManager.setIcon(
                        SECURE_MODE_STATUS_BAR_ICON,
                        android.R.drawable.ic_lock_lock,
                        0,  /* iconLevel */
                        mContext.getString(R.string.secure_mode_status_bar_icon_description));
            } else {
                Log.d(TAG, "Removing the Secure Mode icon");
                mStatusBarManager.removeIcon(SECURE_MODE_STATUS_BAR_ICON);
            }
        }
    }

    private void setMsimPreferenceResponse(Token token, Status status) {
        try {
            int tokenKey = token.get();
            for (IExtPhoneCallback callback : retrieveCallbacks(tokenKey,
                    ExtPhoneCallbackListener.EVENT_SET_MSIM_PREFERENCE_RESPONSE)) {
                Log.d(TAG, "setsetMsimPreferenceResponse: Responding back for transaction = " +
                        mInflightRequests.get(tokenKey));
                callback.setMsimPreferenceResponse(token, status);
                mInflightRequests.remove(tokenKey);
            }
        } catch (RemoteException ex) {
            Log.e(TAG, "onSecureModeStatusChange: Exception", ex);
        }
    }

    private void onSetSimTypeResponse(Token token, Status status) {
        int tokenKey = token.get();

        // Do not remove the tokenKey from mInflightRequests here as the response to client
        // is not sent from here.
        // The response to client would be sent from sendSetSimTypeResponse() method,
        // there the tokenKey shall be removed from mInflightRequests.
        synchronized (mInternalCallbackList) {
            for (IQtiRadioConfigInternalCallback callback : mInternalCallbackList) {
                Log.d(TAG, "onSetSimTypeResponse: Responding back for transaction = " +
                        mInflightRequests.get(tokenKey));
                callback.setSimTypeResponse(token, status);
            }
        }
    }

    private void onGetSimTypeInfoResponse(Token token, Status status, SimTypeInfo[] simTypeInfo) {
        int tokenKey = token.get();

        synchronized (mInternalCallbackList) {
            for (IQtiRadioConfigInternalCallback callback : mInternalCallbackList) {
                Log.d(TAG, "onGetSimTypeInfoResponse: Responding back for transaction = " +
                        mInflightRequests.get(tokenKey));
                callback.getSimTypeInfoResponse(token, status, simTypeInfo);
            }
        }
    }

    void sendSetSimTypeResponse(Token token, Status status) {
        try {
            int tokenKey = token.get();

            for (IExtPhoneCallback callback : retrieveCallbacks(tokenKey,
                    ExtPhoneCallbackListener.EVENT_SET_SIM_TYPE_RESPONSE)) {
                Log.d(TAG, "sendSetSimTypeResponse: Responding back for transaction = " +
                        mInflightRequests.get(tokenKey));
                callback.setSimTypeResponse(token, status);
                mInflightRequests.remove(tokenKey);
            }
        } catch (RemoteException ex) {
            Log.e(TAG, "sendSetSimTypeResponse: Exception", ex);
        }
    }

    // Send Sim Type change indication to registered external clients
    void sendSimTypeChangeInd(QtiSimType[] SimType) {
        try {
            for (IExtPhoneCallback callback : retrieveCallbacks(Token.UNSOL,
                    ExtPhoneCallbackListener.EVENT_ON_SIM_TYPE_CHANGED)) {
                Log.d(TAG, "sendSimTypeChangeInd = " + SimType);
                callback.onSimTypeChanged(SimType);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "sendSimTypeChangeInd: Exception = ", e);
        }
    }

    private void onDualDataCapabilityChanged(Token token, Status status, boolean support) {
        try {
            int tokenKey = token.get();
            for (IExtPhoneCallback callback : retrieveCallbacks(tokenKey,
                    ExtPhoneCallbackListener.EVENT_ON_DUAL_DATA_CAPABILITY_CHANGED)) {
                Log.d(TAG, "onDualDataCapabilityChanged: Responding back for transaction = " +
                        mInflightRequests.get(tokenKey));
                callback.onDualDataCapabilityChanged(token, status, support);
                mInflightRequests.remove(tokenKey);
            }
        } catch (RemoteException ex) {
            Log.e(TAG, "onDualDataCapabilityChanged: Exception", ex);
        }
    }

    private void setDualDataUserPreferenceResponse(Token token, Status status) {
        try {
            int tokenKey = token.get();
            for (IExtPhoneCallback callback : retrieveCallbacks(tokenKey,
                    ExtPhoneCallbackListener.EVENT_SET_DUAL_DATA_USER_PREFERENCE_RESPONSE)) {
                Log.d(TAG, "setDualDataUserPreferenceResponse: " +
                        " Responding back for transaction = " +
                        mInflightRequests.get(tokenKey));
                callback.setDualDataUserPreferenceResponse(token, status);
                mInflightRequests.remove(tokenKey);
            }
        } catch (RemoteException ex) {
            Log.e(TAG, "setDualDataUserPreferenceResponse: Exception", ex);
        }
    }

    private void onDualDataRecommendation(Token token, DualDataRecommendation rec) {
        try {
            int tokenKey = token.get();
            for (IExtPhoneCallback callback : retrieveCallbacks(tokenKey,
                    ExtPhoneCallbackListener.EVENT_ON_DUAL_DATA_RECOMMENDATION)) {
                Log.d(TAG, "onDualDataRecommendation: Responding back for transaction = " +
                        mInflightRequests.get(tokenKey));
                callback.onDualDataRecommendation(rec);
                mInflightRequests.remove(tokenKey);
            }
        } catch (RemoteException ex) {
            Log.e(TAG, "onDualDataRecommendation: Exception", ex);
        }
    }

    private void onDdsSwitchCapabilityChanged(Token token, Status status, boolean support) {
        try {
            int tokenKey = token.get();
            for (IExtPhoneCallback callback : retrieveCallbacks(tokenKey,
                    ExtPhoneCallbackListener.EVENT_ON_DDS_SWITCH_CONFIG_CAPABILITY_CHANGED)) {
                Log.d(TAG, "onDdsSwitchCapabilityChanged: Responding back for transaction = " +
                        mInflightRequests.get(tokenKey));
                callback.onDdsSwitchConfigCapabilityChanged(token, status, support);
                mInflightRequests.remove(tokenKey);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "onDdsSwitchCapabilityChanged: Exception = " + e);
        }
    }

    private void onDdsSwitchCriteriaChanged(Token token, boolean telephonyDdsSwitch) {
        try {
            int tokenKey = token.get();
            for (IExtPhoneCallback callback : retrieveCallbacks(tokenKey,
                    ExtPhoneCallbackListener.EVENT_ON_DDS_SWITCH_CONFIG_CRITERIA_CHANGED)) {
                Log.d(TAG, "onDdsSwitchCriteriaChanged:" +
                        " Responding back for transaction = " +
                        mInflightRequests.get(tokenKey));
                callback.onDdsSwitchConfigCriteriaChanged(telephonyDdsSwitch);
                mInflightRequests.remove(tokenKey);
            }
        } catch (RemoteException e) {
            Log.d(TAG, "onDdsSwitchCriteriaChanged: Exception = " + e);
        }
    }

    private void onDdsSwitchRecommendation(Token token, int recommendedSlotId) {
        try {
            int tokenKey = token.get();
            for (IExtPhoneCallback callback : retrieveCallbacks(tokenKey,
                    ExtPhoneCallbackListener.EVENT_ON_DDS_SWITCH_CONFIG_RECOMMENDATION)) {
                Log.d(TAG, "onDdsSwitchRecommendation:" +
                        " Responding back for transaction = " +
                        mInflightRequests.get(tokenKey));
                callback.onDdsSwitchConfigRecommendation(recommendedSlotId);
            }
            mInflightRequests.remove(tokenKey);
        } catch (RemoteException e) {
            Log.d(TAG, "onDdsSwitchRecommendation: Exception = " + e);
        }
    }

    private void onSendUserPreferenceForDataDuringVoiceCall(Token token, Status status) {
        try {
            int tokenKey = token.get();
            for (IExtPhoneCallback callback : retrieveCallbacks(tokenKey,
                    ExtPhoneCallbackListener.
                    EVENT_ON_SEND_USER_PREFERENCE_CONFIG_FOR_DATA_DURING_CALL)) {
                Log.d(TAG, "onSendUserPreferenceForDataDuringVoiceCall:" +
                        " Responding back for transaction = " +
                        mInflightRequests.get(tokenKey));
                callback.onSendUserPreferenceConfigForDataDuringVoiceCall(token, status);
            }
            mInflightRequests.remove(tokenKey);
        } catch (RemoteException e) {
            Log.d(TAG, "onSendUserPreferenceForDataDuringVoiceCall: Exception = " + e);
        }
    }

    void registerInternalCallback(IQtiRadioConfigInternalCallback callback) {
        synchronized (mInternalCallbackList) {
            Log.d(TAG, "add internal callback = " + callback);
            mInternalCallbackList.add(callback);
        }
    }

    void unRegisterInternalCallback(IQtiRadioConfigInternalCallback callback) {
        synchronized (mInternalCallbackList) {
            Log.d(TAG, "remove internal callback = " + callback);
            mInternalCallbackList.remove(callback);
        }
    }

    static class IQtiRadioConfigInternalCallback {

        public void getSimTypeInfoResponse(Token token, Status status, SimTypeInfo[] simType) {
             // do nothing
        }

        public void setSimTypeResponse(Token token, Status status) {
             // do nothing
        }
    }

    /* Private delegates */
    private void onCiwlanCapabilityChanged(Token token, Status error,
            CiwlanCapability capability) {
        try {
            int tokenKey = token.get();
            final int ddsPhoneId = mSubscriptionManager.getPhoneId(
                    mSubscriptionManager.getDefaultDataSubscriptionId());

            Optional<Integer> optionalCap = capability.getCiwlanCapability();
            if (!optionalCap.isPresent()) return;
            int cap = optionalCap.get().intValue();

            int phoneCount = mTelephonyManager.getActiveModemCount();
            for (IExtPhoneCallback callback : retrieveCallbacks(tokenKey,
                    ExtPhoneCallbackListener.EVENT_ON_CIWLAN_CAPABILITY_CHANGE)) {
                for (int phoneId = 0; phoneId < phoneCount; phoneId++) {
                    boolean enable = false;
                    if (cap == CiwlanCapability.BOTH
                            || (cap == CiwlanCapability.DDS && ddsPhoneId == phoneId)) {
                        enable = true;
                    }
                    callback.onEpdgOverCellularDataSupported(phoneId, enable);
                }
            }
        } catch (RemoteException ex) {
            Log.e(TAG, "onCiwlanCapabilityChanged: Exception", ex);
        }
    }

    static boolean isInSecureMode() {
        return mIsInSecureMode;
    }

    public int getInflightRequestsCount() {
        return mInflightRequests.size();
    }

    private void dumpInflightRequests(PrintWriter pw){
        for (Integer key : mInflightRequests.keySet()) {
            pw.println("Token = " + key + " => " + mInflightRequests.get(key));
        }
    }

    // Dump service.
    public void dump(FileDescriptor fd, PrintWriter printwriter, String[] args) {
        PrintWriter pw = printwriter;
        pw.println("5G-Middleware:");
        pw.println("mQtiRadioConfig = " + mQtiRadioConfig);
        pw.flush();

        pw.println("Inflight requests : ");
        dumpInflightRequests(pw);
        pw.flush();
    }
}
