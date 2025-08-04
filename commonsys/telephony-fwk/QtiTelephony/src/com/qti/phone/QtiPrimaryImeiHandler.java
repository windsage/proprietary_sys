/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qti.phone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import com.qti.extphone.QtiImeiInfo;
import com.qti.extphone.Status;
import com.qti.extphone.Token;

import org.codeaurora.telephony.utils.Log;

public class QtiPrimaryImeiHandler {
    private static final String LOG_TAG = "QtiPrimaryImeiHandler";

    private Context mContext;
    private QtiRadioProxy mQtiRadioProxy;
    PrimaryImeiCallback mPrimaryImeiCallback;
    private Handler mHandler;
    private int mPhoneCount;
    private int mReceivedImeiCount = 0;

    private boolean[] mIsRadioUnAvailable;
    private QtiImeiInfo[] mImeiInfo;
    private QtiImeiInfo[] mNewImeiInfo;
    private Token[] mRequestToken;

    /* Event Constants */
    private static final int EVENT_GET_IMEI = 1;
    private static final int EVENT_GET_IMEI_RESPONSE = 2;
    private static final int EVENT_IMEI_IND = 3;
    private static final int EVENT_RADIO_STATE_CHANGE = 4;

    private static final String EXTRA_STATE = "state";
    private static final String ACTION_RADIO_POWER_STATE_CHANGED =
            "org.codeaurora.intent.action.RADIO_POWER_STATE";

    public QtiPrimaryImeiHandler(Context context, QtiRadioProxy radioProxy) {
        mQtiRadioProxy = radioProxy;
        mContext = context;

        TelephonyManager tm = (TelephonyManager) mContext.
                getSystemService(Context.TELEPHONY_SERVICE);
        mPhoneCount = tm.getActiveModemCount();
        mImeiInfo = new QtiImeiInfo[mPhoneCount];
        mNewImeiInfo = new QtiImeiInfo[mPhoneCount];
        mRequestToken = new Token[mPhoneCount];
        mIsRadioUnAvailable = new boolean[mPhoneCount];

        for (int i = 0; i < mPhoneCount; i++) {
            mImeiInfo[i] = null;
            mNewImeiInfo[i] = null;
            mRequestToken[i] = null;
            mIsRadioUnAvailable[i] = true;
        }

        HandlerThread headlerThread = new HandlerThread(LOG_TAG);
        headlerThread.start();
        mHandler = new PrimaryImeiHandler(headlerThread.getLooper());

        mPrimaryImeiCallback = new PrimaryImeiCallback();
        mQtiRadioProxy.registerInternalCallback(mPrimaryImeiCallback);

        mContext.registerReceiver(mBroadcastReceiver,
                new IntentFilter(ACTION_RADIO_POWER_STATE_CHANGED));
        sendGetImeiRequest();
        logd("QtiPrimaryImeiHandler " + mPhoneCount);
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            logd("onReceive: " + action);
            if (ACTION_RADIO_POWER_STATE_CHANGED.equals(action)) {
                int slotId = intent.getIntExtra(SubscriptionManager.EXTRA_SLOT_INDEX,
                        SubscriptionManager.INVALID_SIM_SLOT_INDEX);
                int radioState = intent.getIntExtra(EXTRA_STATE,
                        TelephonyManager.RADIO_POWER_UNAVAILABLE);

                mHandler.obtainMessage(EVENT_RADIO_STATE_CHANGE, slotId, radioState).sendToTarget();
            }
        }
    };

    private void handleRadioPowerStateChanged(int slotId, int radioState) {
        logd(" handleRadioPowerStateChanged, state[" + slotId + "]=" + radioState);
        boolean oldRadioState = mIsRadioUnAvailable[slotId];
        boolean isRadioAvailableOnAllSims = true;

        mIsRadioUnAvailable[slotId] =
                (radioState == TelephonyManager.RADIO_POWER_UNAVAILABLE) ? true : false;

        for (int i = 0; i < mIsRadioUnAvailable.length; i++) {
            if (mIsRadioUnAvailable[i]) {
                isRadioAvailableOnAllSims = false;
            }
        }

        if (isRadioAvailableOnAllSims && (oldRadioState != mIsRadioUnAvailable[slotId])) {
            sendGetImeiRequest();
        }
    }

    private final class PrimaryImeiHandler extends Handler {
        PrimaryImeiHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case EVENT_GET_IMEI: {
                    logd("EVENT_GET_IMEI");
                    mReceivedImeiCount = 0;
                    for (int i = 0; i < mPhoneCount; i++) {
                        try {
                            mRequestToken[i] = mQtiRadioProxy.getImei(i, null);
                        } catch (RemoteException | NullPointerException ex) {
                            loge("Exception: " + ex);
                        }
                    }
                    break;
                }

                case EVENT_GET_IMEI_RESPONSE: {
                    logd("EVENT_GET_IMEI_RESPONSE: " + msg.arg1 + " status " + msg.arg2);
                    mRequestToken[msg.arg1] = null;
                    updateImeiInfo(msg.arg1,(QtiImeiInfo)msg.obj);
                    break;
                }

                case EVENT_IMEI_IND: {
                    logd("EVENT_IMEI_IND: " + msg.arg1);

                    // Clear and igore if any get request ongoing
                    for (int i = 0; i < mPhoneCount; i++) {
                        if (mRequestToken[i] != null) {
                            logd(" clearing token" + mRequestToken[i] + " of slot " + i);
                            mReceivedImeiCount = 0;
                            mRequestToken[i] = null;
                        }
                    }
                    updateImeiInfo(msg.arg1, (QtiImeiInfo)msg.obj);
                    break;
                }

                case EVENT_RADIO_STATE_CHANGE: {
                    logd(" EVENT_RADIO_STATE_CHANGE ");
                    handleRadioPowerStateChanged(msg.arg1, msg.arg2);
                    break;
                }

                default: {
                    logd("invalid message " + msg.what);
                    break;
                }
            }
        }
    }

    private void updateImeiInfo(int slotId, QtiImeiInfo imeiInfo) {
        boolean isInfoChanged = false;
        if (imeiInfo == null || (slotId < 0 || slotId >= mPhoneCount)) {
            loge(" invalid slotID " + slotId + " null imei " + imeiInfo);
            return;
        }

        mReceivedImeiCount++;
        mNewImeiInfo[slotId] = imeiInfo;

        logd(" updateImeiInfo, " + imeiInfo + " mReceivedImeiCount=" +
                mReceivedImeiCount);

        // Update to mImeiInfo cache only after all slot IMEI information received
        if (mReceivedImeiCount == mPhoneCount) {
            synchronized (mImeiInfo) {
                for (int i = 0; i < mPhoneCount; i++) {
                    if (mNewImeiInfo[i] != null && !mNewImeiInfo[i].equals(mImeiInfo[i])) {
                        isInfoChanged = true;
                        mImeiInfo[i] = mNewImeiInfo[i];
                    }
                    mNewImeiInfo[i]  = null;
                }
                mReceivedImeiCount = 0;
            }
            logi("sendGetImeiRequest,  isInfoChanged=" + isInfoChanged);
        }

        // Inform clients when IMEI information updated/changed
        if (isInfoChanged) {
            mQtiRadioProxy.sendImeiInfoInd(mImeiInfo);
        }
    }

    private void sendGetImeiRequest() {
        logd("sendGetImeiRequest ");

        mHandler.sendMessage(mHandler.obtainMessage(EVENT_GET_IMEI));
    }

    public QtiImeiInfo[] getImeiInfo() {
        logd("getImeiInfo");

        synchronized (mImeiInfo) {
            return mImeiInfo;
        }
    }

    class PrimaryImeiCallback extends QtiRadioProxy.IQtiRadioInternalCallback {
        @Override
        public void getImeiResponse(int slotId, Token token, Status status, QtiImeiInfo imeiInfo) {
            if (mRequestToken[slotId] == null || !mRequestToken[slotId].equals(token)) {
                logd(" getImeiResponse, ignore " + mRequestToken[slotId]);
                return;
            }
            logi(" getImeiResponse, imei[" + slotId + "]=" + Log.pii(imeiInfo.getImei()));
            mHandler.sendMessage(mHandler.obtainMessage(
                    EVENT_GET_IMEI_RESPONSE, slotId, status.get(), imeiInfo));
        }

        @Override
        public void onImeiChanged(int slotId, QtiImeiInfo imeiInfo) {
            logi("onImeiChanged, imei[" + slotId + "]=" + Log.pii(imeiInfo.getImei()));
            mHandler.sendMessage(mHandler.obtainMessage(EVENT_IMEI_IND, slotId, -1, imeiInfo));
        }
    }

    public void destroy() {
        logi("destroy");
        mQtiRadioProxy.unRegisterInternalCallback(mPrimaryImeiCallback);
        mContext.unregisterReceiver(mBroadcastReceiver);
        mHandler.getLooper().quit();
    }

    private void logd(String string) {
        Log.i(LOG_TAG, string);
    }

    private void logi(String string) {
        Log.i(LOG_TAG, string);
    }

    private void loge(String string) {
        Log.e(LOG_TAG, string);
    }
}
