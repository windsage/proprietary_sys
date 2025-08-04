/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qti.phone;

import static java.util.Arrays.copyOf;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.telephony.SubscriptionManager;
import android.telephony.UiccCardInfo;
import android.telephony.UiccSlotMapping;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.qti.extphone.Client;
import com.qti.extphone.QtiSimType;
import com.qti.extphone.Status;
import com.qti.extphone.Token;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import vendor.qti.hardware.radio.qtiradioconfig.SimTypeInfo;
import vendor.qti.hardware.radio.qtiradioconfig.SimType;

public class QtiUiccSwitcher {
    private static final String LOG_TAG = "QtiUiccSwitcher";

    private Context mContext;
    private Handler mHandler;
    private int mPhoneCount;
    private Token mGetRequestToken = null;
    private Token mSetRequestToken = null;
    private QtiRadioConfigProxy mQtiRadioConfigProxy;
    QtiUiccSwitcherCallback mQtiUiccSwitcherCallback;
    private Object mPSimSwitchInProgress = null;
    private long mMinDeadline = 0;

    private boolean[] mIsRadioUnavailable;
    private QtiSimType[] mCurrentSimType;
    private QtiSimType[] mSupportedSimTypes;

    /* Event Constants */
    private static final int EVENT_GET_SIM_TYPE = 1;
    private static final int EVENT_GET_SIM_TYPE_RESPONSE = 2;
    private static final int EVENT_RADIO_STATE_CHANGED = 3;
    private static final int EVENT_SET_SIM_TYPE = 4;

    private static final int GLPA_SIM_TYPE_INVALID = -1;
    private static final int GLPA_SIM_TYPE_PHYSICAL = 0;
    private static final int GLPA_SIM_TYPE_ESIM = 1;

    private static final int MAX_DELAY_INTERVAL = 5000;
    private static final int MIN_DELAY_INTERVAL = 1000;

    private static final String EXTRA_STATE = "state";
    private static final String ACTION_RADIO_POWER_STATE_CHANGED =
            "org.codeaurora.intent.action.RADIO_POWER_STATE";
    private static final String ACTION_SIM_TYPE_SWITCH_REQ =
            "com.android.euicc.service.SIM_TYPE_UPDATE_ACTION";
    private static final String INTENT_EXTRA_SLOT_ID = "com.android.euicc.service.extra_slot_id";
    private static final String INTENT_EXTRA_SIM_TYPE = "com.android.euicc.service.extra_sim_type";

    public QtiUiccSwitcher(Context context, QtiRadioConfigProxy radioProxy) {
        mQtiRadioConfigProxy = radioProxy;
        mContext = context;

        TelephonyManager tm = (TelephonyManager) mContext.
                getSystemService(Context.TELEPHONY_SERVICE);
        mPhoneCount = tm.getActiveModemCount();

        mCurrentSimType = new QtiSimType[mPhoneCount];
        mSupportedSimTypes = new QtiSimType[mPhoneCount];
        mIsRadioUnavailable = new boolean[mPhoneCount];

        for (int index = 0; index < mPhoneCount; index++) {
            mCurrentSimType[index] = new QtiSimType(QtiSimType.SIM_TYPE_INVALID);
            mSupportedSimTypes[index] = new QtiSimType(QtiSimType.SIM_TYPE_INVALID);
            mIsRadioUnavailable[index] = true;
        }

        HandlerThread headlerThread = new HandlerThread(LOG_TAG);
        headlerThread.start();
        mHandler = new QtiSimTypeHandler(headlerThread.getLooper());

        mQtiUiccSwitcherCallback = new QtiUiccSwitcherCallback();
        mQtiRadioConfigProxy.registerInternalCallback(mQtiUiccSwitcherCallback);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_RADIO_POWER_STATE_CHANGED);
        intentFilter.addAction(TelephonyManager.ACTION_SIM_SLOT_STATUS_CHANGED);
        intentFilter.addAction(ACTION_SIM_TYPE_SWITCH_REQ);
        mContext.registerReceiver(mBroadcastReceiver, intentFilter, Context.RECEIVER_EXPORTED);

        sendGetSimTypeInfoRequest();
        logd("constructor " + mPhoneCount);
    }

    public void destroy() {
        logi("destroy");
        mQtiRadioConfigProxy.unRegisterInternalCallback(mQtiUiccSwitcherCallback);
        mContext.unregisterReceiver(mBroadcastReceiver);
        mHandler.getLooper().quit();
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_RADIO_POWER_STATE_CHANGED.equals(action)) {
                int phoneId = intent.getIntExtra(SubscriptionManager.EXTRA_SLOT_INDEX,
                        SubscriptionManager.INVALID_SIM_SLOT_INDEX);
                int radioState = intent.getIntExtra(EXTRA_STATE,
                        TelephonyManager.RADIO_POWER_UNAVAILABLE);

                if (phoneId >= 0 && phoneId < mPhoneCount) {
                    mHandler.obtainMessage(EVENT_RADIO_STATE_CHANGED,
                            phoneId, radioState).sendToTarget();
                } else {
                    loge("received invalid phoneId: " + phoneId);
                }
            } else if (TelephonyManager.ACTION_SIM_SLOT_STATUS_CHANGED.equals(action)) {
                logd("received slot status change indication");
                if (mPSimSwitchInProgress != null) {
                    // Wait for 1sec before releasing the thread as per below layer requirement
                    long now = SystemClock.elapsedRealtime();
                    if ((now > mMinDeadline) && !isBothLogicalSlotsMappedToSamePhysicalSlot()) {
                        mPSimSwitchInProgress.notifyAll();
                        mPSimSwitchInProgress = null;
                    }
                }
                sendGetSimTypeInfoRequest();
            } else if (ACTION_SIM_TYPE_SWITCH_REQ.equals(action)) {
                int logicalSlotId = intent.getIntExtra(INTENT_EXTRA_SLOT_ID,
                        SubscriptionManager.INVALID_SIM_SLOT_INDEX);
                int simType = intent.getIntExtra(INTENT_EXTRA_SIM_TYPE, GLPA_SIM_TYPE_INVALID);
                logd("received sim type switch request from gLPA, slotId = "
                        + logicalSlotId + " simType = " + simType);

                // This Intent would be received from gLPA module, the requirement here is to
                // perform SIM type switch to make eSIM/pSIM active
                if (simType != GLPA_SIM_TYPE_INVALID) {
                    mHandler.obtainMessage(EVENT_SET_SIM_TYPE,
                            logicalSlotId, simType).sendToTarget();
                }
            } else {
                loge("received invalid action: " + action);
            }
        }
    };

    private void handleRadioPowerStateChanged(int phoneId, int radioState) {
        logd("handleRadioPowerStateChanged, state[" + phoneId + "]=" + radioState);
        boolean oldRadioState = mIsRadioUnavailable[phoneId];
        boolean isRadioAvailableOnAllSims = true;

        mIsRadioUnavailable[phoneId] =
                (radioState == TelephonyManager.RADIO_POWER_UNAVAILABLE) ? true : false;

        for (int i = 0; i < mIsRadioUnavailable.length; i++) {
            if (mIsRadioUnavailable[i]) {
                isRadioAvailableOnAllSims = false;
            }
        }

        // If radio moves from UNAVAILABLE to AVAILABLE query latest SimType information
        if (isRadioAvailableOnAllSims && (oldRadioState != mIsRadioUnavailable[phoneId])) {
            logd("radio available, send getSimType request");
            sendGetSimTypeInfoRequest();
        }
    }

    private final class QtiSimTypeHandler extends Handler {
        QtiSimTypeHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_GET_SIM_TYPE: {
                    try {
                        mGetRequestToken = mQtiRadioConfigProxy.getSimTypeInfo();
                    } catch (RemoteException | NullPointerException ex) {
                        loge("Exception: " + ex);
                    }
                    logd("EVENT_GET_SIM_TYPE " + mGetRequestToken);
                    break;
                }

                case EVENT_GET_SIM_TYPE_RESPONSE: {
                    logd("EVENT_GET_SIM_TYPE_RESPONSE: status " + msg.arg1);
                    if (mGetRequestToken == null || (msg.arg2 != mGetRequestToken.get())) {
                        logd("getSimTypeInfoResponse, ignore " + mGetRequestToken);
                        return;
                    }

                    mGetRequestToken = null;

                    // If it received success response then only update the SimType cache
                    if (msg.arg1 == Status.SUCCESS) {
                        updateSimTypeInfo((SimTypeInfo[])msg.obj);
                    }
                    break;
                }

                case EVENT_RADIO_STATE_CHANGED: {
                    handleRadioPowerStateChanged(msg.arg1, msg.arg2);
                    break;
                }

                case EVENT_SET_SIM_TYPE: {
                    processSetSimTypeRequest(msg.arg1, msg.arg2);
                    break;
                }

                default: {
                    logd("invalid message " + msg.what);
                    break;
                }
            }
        }
    }

    private void sendGetSimTypeInfoRequest() {
        mHandler.sendMessage(mHandler.obtainMessage(EVENT_GET_SIM_TYPE));
    }

    // Once receive the SimType information from below layer store it in local cache
    private void updateSimTypeInfo(SimTypeInfo[] simTypeInfo) {
       boolean isCurrentSimTypeChanged = false;
       int length = simTypeInfo.length;

        logi("updateSimTypeInfo , SimTypeInfo length : " + length);

        if (length > mPhoneCount) {
            mCurrentSimType = copyOf(mCurrentSimType, length);
            mSupportedSimTypes = copyOf(mSupportedSimTypes, length);
            for (int i = mPhoneCount; i < length; i++) {
                mCurrentSimType[mPhoneCount] = new QtiSimType(QtiSimType.SIM_TYPE_INVALID);
                mSupportedSimTypes[mPhoneCount] = new QtiSimType(QtiSimType.SIM_TYPE_INVALID);
            }
        }

        for (int index = 0; index < length; index++) {
            int currentSimType = convertToQtiSimType(simTypeInfo[index].currentSimType);

            synchronized (mCurrentSimType) {
                logi("updateSimTypeInfo, current SimType=" + mCurrentSimType[index]
                        + " new SimType=" + simTypeInfo[index].currentSimType);

                if (mCurrentSimType[index].get() != currentSimType) {
                    isCurrentSimTypeChanged = true;
                }
                mCurrentSimType[index] = new QtiSimType(currentSimType);
            }

            int supportedSimType = convertToQtiSimType(simTypeInfo[index].supportedSimTypes);

            synchronized (mSupportedSimTypes) {
                logi("updateSimTypeInfo, supported SimType=" + mSupportedSimTypes[index]
                        + " new supported SimType=" + simTypeInfo[index].supportedSimTypes);

                mSupportedSimTypes[index] = new QtiSimType(supportedSimType);
            }
            logi("supported=" + supportedSimType + " current " + currentSimType);
        }

        if (isCurrentSimTypeChanged) {
            // If any pending set request exists send the success status
            if (mSetRequestToken != null) {
                mQtiRadioConfigProxy.sendSetSimTypeResponse(
                        mSetRequestToken, new Status(Status.SUCCESS));
                mSetRequestToken = null;
            }

            // If current SIM Type is changed, notify to the all registered clients
            mQtiRadioConfigProxy.sendSimTypeChangeInd(mCurrentSimType);
        }
    }

    // Utility to convert sAIDL SimType values to AIDL QtiSimType values
    int convertToQtiSimType(int simType) {
        int type = 0;
        if (simType == (SimType.SIM_TYPE_PHYSICAL | SimType.SIM_TYPE_INTEGRATED)) {
            type = QtiSimType.SIM_TYPE_PHYSICAL_IUICC;
        } else if (simType == SimType.SIM_TYPE_PHYSICAL) {
            type = QtiSimType.SIM_TYPE_PHYSICAL;
        } else if (simType == SimType.SIM_TYPE_INTEGRATED) {
            type = QtiSimType.SIM_TYPE_IUICC;
        } else if (simType == SimType.SIM_TYPE_ESIM) {
            type = QtiSimType.SIM_TYPE_ESIM;
        } else if (simType == (SimType.SIM_TYPE_PHYSICAL | SimType.SIM_TYPE_ESIM)) {
            type = QtiSimType.SIM_TYPE_PHYSICAL_ESIM;
        } else if (simType == (SimType.SIM_TYPE_INTEGRATED | SimType.SIM_TYPE_ESIM)) {
            type = QtiSimType.SIM_TYPE_ESIM_IUICC;
        } else if (simType == (SimType.SIM_TYPE_PHYSICAL | SimType.SIM_TYPE_INTEGRATED |
                SimType.SIM_TYPE_ESIM)) {
            type = QtiSimType.SIM_TYPE_PHYSICAL_ESIM_IUICC;
        }
        logv("received type=" + simType + " sent type " + type);
        return type;
    }

    class QtiUiccSwitcherCallback extends QtiRadioConfigProxy.IQtiRadioConfigInternalCallback {

        @Override
        public void getSimTypeInfoResponse(Token token, Status status, SimTypeInfo[] simType) {
            logi("getSimTypeInfoResponse " + Arrays.toString(simType)
                    + " token=" + token);
            mHandler.sendMessage(mHandler.obtainMessage(
                    EVENT_GET_SIM_TYPE_RESPONSE, status.get(), token.get(), simType));
        }

        @Override
        public void setSimTypeResponse(Token token, Status status) {
            if (mSetRequestToken == null || !mSetRequestToken.equals(token)) {
                logd("setSimTypeResponse, ignore " + mSetRequestToken
                        + " received Token = " + token);
                return;
            }

            // When setSimType request received Failure status, inform caller immediately, in
            // success case the operation completion takes time so wait to send response until
            // Sim Type switching request completed.
            if (status.get() != Status.SUCCESS) {
                mQtiRadioConfigProxy.sendSetSimTypeResponse(token, status);
                mSetRequestToken = null;
            }
            logi("setSimTypeResponse, token " + mSetRequestToken);
        }
    }

    public QtiSimType[] getCurrentSimType() {
        logd("getCurrentSimType" + Arrays.toString(mCurrentSimType));

        synchronized (mCurrentSimType) {
            return mCurrentSimType;
        }
    }

    public QtiSimType[] getSupportedSimTypes() {
        logd("getSupportedSimTypes " + Arrays.toString(mSupportedSimTypes));

        synchronized (mSupportedSimTypes) {
            return mSupportedSimTypes;
        }
    }

    // Send request to QtiRadioConfigProxy and store the Token value, this Token would be
    // used to send response back to client when setSimType request completed.
    public Token setSimType(Client client, QtiSimType[] simType) throws RemoteException {
        logd("setSimType, " + Arrays.toString(simType));

        // If below all conditions met, handle the setSimType request later
        // 1. Device supports MEP
        // 2. Both logical slots mapped to same physical slot
        // 3. Received request to switch slot from eSIM to pSIM
        if (activatepSimIfNeeded(simType)) {
            logd("psim activation in progress");
            mPSimSwitchInProgress = new Object();

            // Wait for at least timeoutInMs before returning null request result
            long now = SystemClock.elapsedRealtime();
            long deadline = now + MAX_DELAY_INTERVAL;
            mMinDeadline = now + MIN_DELAY_INTERVAL;
            synchronized (mPSimSwitchInProgress) {
                while ((mPSimSwitchInProgress != null) && (now < deadline)) {
                    try {
                        mPSimSwitchInProgress.wait(deadline - now);
                    } catch (InterruptedException e) {
                        // Do nothing, go back and check if request is completed or timeout
                    } finally {
                        now = SystemClock.elapsedRealtime();
                    }
                }
                mPSimSwitchInProgress = null;
            }
        }

        mSetRequestToken = mQtiRadioConfigProxy.setSimType(client, simType);
        return mSetRequestToken;
    }

    private void processSetSimTypeRequest(int slotId, int simType) {
        int eSimSupportedSlotId = geteSimTypeSupportedSlotId();
        Token token = null;
        QtiSimType[] newSimType = getCurrentSimType();

        if (eSimSupportedSlotId == SubscriptionManager.INVALID_SIM_SLOT_INDEX) {
            logi("eSIM not supported on any slot, simType " + simType + " on slot " + slotId);
            return;
        }

        // Update the received simType and send request to below layers
        if (simType == GLPA_SIM_TYPE_PHYSICAL) {
            newSimType[eSimSupportedSlotId] = new QtiSimType(QtiSimType.SIM_TYPE_PHYSICAL);
        } else if (simType == GLPA_SIM_TYPE_ESIM) {
            newSimType[eSimSupportedSlotId] = new QtiSimType(QtiSimType.SIM_TYPE_ESIM);
        }

        try {
            token = mQtiRadioConfigProxy.setSimType(newSimType);
        } catch (RemoteException ex) {
            loge("processSetSimTypeRequest, Exception: " + ex);
        }
        logi("setSimType, simType = " + simType + " slotId = " + slotId + " token = " + token);
    }

    // This method finds the slotId where eSIM slot is supported
    private int geteSimTypeSupportedSlotId() {
        int eSimSupportedSlotId = SubscriptionManager.INVALID_SIM_SLOT_INDEX;
        QtiSimType[] supportedSimTypes = getSupportedSimTypes();

        for (int i = 0; i < supportedSimTypes.length; i++) {
            logi("geteSimTypeSupportedSlotId, " + supportedSimTypes[i].get());
            if ((supportedSimTypes[i].get() == QtiSimType.SIM_TYPE_PHYSICAL_ESIM) ||
                    (supportedSimTypes[i].get() == QtiSimType.SIM_TYPE_PHYSICAL_ESIM_IUICC )) {
                eSimSupportedSlotId = i;
                break;
            }
        }

        return eSimSupportedSlotId;
    }

    private boolean activatepSimIfNeeded(QtiSimType[] simType) {
        if (!isMEPSupported()) {
            logd("device does not support mep");
            return false;
        }
        if (!isBothLogicalSlotsMappedToSamePhysicalSlot()) {
            logd("both logical slots not mapped to same physical slots");
            return false;
        }

        return activatePsim();
    }

    private boolean isMEPSupported() {
        TelephonyManager tm = mContext.getSystemService(TelephonyManager.class);
        List<UiccCardInfo> cardsInfo = ((tm != null) ? tm.getUiccCardsInfo() : null);
        if (cardsInfo == null) {
            logi("isMEPSupported, UiccCardInfo null");
            return false;
        }
        return cardsInfo.stream().anyMatch(
                cardInfo -> cardInfo.isMultipleEnabledProfilesSupported());
    }

    private boolean isBothLogicalSlotsMappedToSamePhysicalSlot() {
        int physicalSlotId = -1;
        boolean bothSlotsMappedToSamePhysicalSlot = false;
        TelephonyManager telMgr = mContext.getSystemService(TelephonyManager.class);
        Collection<UiccSlotMapping> uiccSlotMappings =
                ((telMgr != null) ? telMgr.getSimSlotMapping() : null);

        logd("UiccSlotMappings = " + uiccSlotMappings);
        for (UiccSlotMapping slotMapping : uiccSlotMappings) {
            if ((physicalSlotId != -1) && (physicalSlotId == slotMapping.getPhysicalSlotIndex())) {
                bothSlotsMappedToSamePhysicalSlot = true;
                break;
            }
            physicalSlotId = slotMapping.getPhysicalSlotIndex();
        }
        return bothSlotsMappedToSamePhysicalSlot;
    }

    // The requirement here is to activate the physical slot by replacing it a MEP port
    private boolean activatePsim() {
        TelephonyManager tm = mContext.getSystemService(TelephonyManager.class);
        if (tm == null) {
            loge("activatePsim tm null");
            return false;
        }

        List<UiccSlotMapping> slotMappingList = new ArrayList<UiccSlotMapping>();

        for (int index = 0; index < mPhoneCount; index++) {
            slotMappingList.add(
                    new UiccSlotMapping(0/* portId */, index/* physicalslotId */, index));
        }
        logi("setSimSlotMapping " + slotMappingList);

        try {
            tm.setSimSlotMapping(slotMappingList);
        } catch (IllegalStateException e) {
            loge("setSimSlotMapping failed with " + e);
            return false;
        }
        return true;
    }

    private void logv(String string) {
        Log.v(LOG_TAG, string);
    }

    private void logd(String string) {
        Log.d(LOG_TAG, string);
    }

    private void logi(String string) {
        Log.i(LOG_TAG, string);
    }

    private void loge(String string) {
        Log.e(LOG_TAG, string);
    }
}
