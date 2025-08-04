/* Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qti.phone;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.telephony.euicc.EuiccManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.UiccCardInfo;
import android.telephony.UiccPortInfo;
import android.telephony.UiccSlotMapping;
import android.util.Log;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class QtiNtnProfileHelper {
    private static final String TAG = "QtiNtnProfileHelper";

    private static final String ACTION_DISABLE_ESIM = "com.android.action.disable_esim";
    private static final String ACTION_ENABLE_ESIM = "com.android.action.enable_esim";
    private static final int REQUEST_CODE = 0;
    private static final int EVENT_DISABLE_ESIM = 0;
    private static final int EVENT_ENABLE_ESIM = 1;
    private static final int EVENT_NOTIFY_ENABLE_ESIM_STATUS = 2;
    private static final int EVENT_NOTIFY_DISABLE_ESIM_STATUS = 3;
    private static final int EVENT_GET_ALL_ESIM_PROFILES = 4;
    private static final int RETRY_LIMIT = 120;

    private Context mContext;
    private Handler mHandler;
    private QtiRadioProxy mQtiRadioProxy;
    private TelephonyManager mTelMgr;
    private SubscriptionManager mSubMgr;
    private EuiccManager mEuiccMgr;
    private HandlerThread mHandlerThread;
    private QtiNtnProfileCallback mQtiNtnProfileCallback;

    private int mRetryCounter;

    public QtiNtnProfileHelper(Context context, QtiRadioProxy qtiRadioProxy) {
        mContext = context;
        mQtiRadioProxy = qtiRadioProxy;
        mSubMgr = mContext.getSystemService(SubscriptionManager.class);
        mEuiccMgr = (EuiccManager) mContext.getSystemService(Context.EUICC_SERVICE);
        mTelMgr = mContext.getSystemService(TelephonyManager.class);

        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();
        mHandler = new QtiNtnProfileHandler(mHandlerThread.getLooper());

        mQtiNtnProfileCallback = new QtiNtnProfileCallback();
        mQtiRadioProxy.registerInternalCallback(mQtiNtnProfileCallback);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_DISABLE_ESIM);
        intentFilter.addAction(ACTION_ENABLE_ESIM);
        mContext.registerReceiver(mReceiver, intentFilter, Context.RECEIVER_EXPORTED);
        Log.i(TAG, "constructor");
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "onReceive: "  + intent.getAction());
            if (ACTION_ENABLE_ESIM.equals(intent.getAction())) {
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_NOTIFY_ENABLE_ESIM_STATUS,
                        intent.getIntExtra("slotId", 0), intent.getIntExtra("refNum", -1),
                        getResultCode()));
            } else if (ACTION_DISABLE_ESIM.equals(intent.getAction())) {
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_NOTIFY_DISABLE_ESIM_STATUS,
                        intent.getIntExtra("slotId", 0), intent.getIntExtra("refNum", -1),
                        getResultCode()));
            }
        }
    };

    private List<String> getEsimProfileIccIds(int slotId, int cardId) {
        List<SubscriptionInfo> allSubs = mSubMgr.getAllSubscriptionInfoList();
        if (allSubs == null || cardId == TelephonyManager.UNSUPPORTED_CARD_ID) {
            Log.i(TAG, "allSubs null or cardId invalid");
            return null;
        }
        List<String> iccIdList = allSubs.stream()
                .filter(subInfo -> cardId == subInfo.getCardId())
                .filter(SubscriptionInfo::isEmbedded)
                .map(subInfo -> subInfo.getIccId())
                .collect(Collectors.toList());
        return iccIdList;
    }

    private int getCardId(int slotId) {
        List<UiccCardInfo> cardInfos = mTelMgr.getUiccCardsInfo();
        if (cardInfos == null || cardInfos.isEmpty()) {
            Log.d(TAG, "Card info not available");
            return TelephonyManager.UNINITIALIZED_CARD_ID;
        }

        for (UiccCardInfo cardInfo : cardInfos) {
            if (cardInfo == null) {
                continue;
            }
            for (UiccPortInfo uiccPortInfo: cardInfo.getPorts()) {
                if (cardInfo.isEuicc() && (uiccPortInfo.getLogicalSlotIndex() == slotId)) {
                    Log.d(TAG, "getCardId: " + cardInfo.getCardId());
                    return cardInfo.getCardId();
                }
            }
        }
        return TelephonyManager.UNSUPPORTED_CARD_ID;
    }

    private int getPortIndex(int slotId) {
        if (mTelMgr == null) return TelephonyManager.INVALID_PORT_INDEX;
        Collection<UiccSlotMapping> simSlotMapping;
        try {
            simSlotMapping = mTelMgr.getSimSlotMapping();
        } catch (Exception e) {
            Log.e(TAG, "Exception during getPortIndex : " + e);
            return TelephonyManager.INVALID_PORT_INDEX;
        }

        UiccSlotMapping uiccSlotMapping = simSlotMapping.stream().filter
               (slotMapping -> slotMapping.getLogicalSlotIndex() == slotId)
               .findFirst().orElse(null);
        if (uiccSlotMapping != null) {
            return uiccSlotMapping.getPortIndex();
        }
        return TelephonyManager.INVALID_PORT_INDEX;
    }

    private void handleDisableProfile(int slotId, int refNum, String iccId) {
        Log.d(TAG, "handleDisableProfile : " + slotId + " refNum: " + refNum + " iccId : " + iccId);
        int cardId = getCardId(slotId);
        if (cardId < 0) {
            notifyDisableProfileStatus(slotId, refNum, false);
            return;
        }

        Intent intent = new Intent(ACTION_DISABLE_ESIM);
        intent.setPackage(mContext.getPackageName());
        intent.putExtra("slotId", slotId);
        intent.putExtra("refNum", refNum);
        int portIndex = getPortIndex(slotId);
        if (portIndex == TelephonyManager.INVALID_PORT_INDEX) {
            portIndex = TelephonyManager.DEFAULT_PORT_INDEX;
        }

        Log.d(TAG, "handleDisableProfile : " + " portIndex : " + portIndex + " cardId: " + cardId);
        try {
            mEuiccMgr.createForCardId(cardId).switchToSubscription
                    (SubscriptionManager.INVALID_SUBSCRIPTION_ID, portIndex,
                    PendingIntent.getBroadcast(mContext, REQUEST_CODE, intent,
                    PendingIntent.FLAG_IMMUTABLE));
        } catch (Exception e) {
            Log.e(TAG, "Exception during switchToSubscription: " + e);
            notifyDisableProfileStatus(slotId, refNum, false);
        }
    }

    private void handleEnableProfile(int slotId, int refNum, String iccId) {
        Log.d(TAG, "handleEnableProfile : " + slotId + " refNum: " + refNum + " iccId : " + iccId);
        List<SubscriptionInfo> allSubs = mSubMgr.getAllSubscriptionInfoList();
        int cardId = getCardId(slotId);
        if (allSubs == null || cardId < 0) {
            Log.i(TAG, "allSubs null or cardId invalid");
            notifyEnableProfileStatus(slotId, refNum, false);
            return;
        }

        int subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        List<SubscriptionInfo> subInfoList =
                allSubs.stream().filter(subInfo -> subInfo.getIccId().equals(iccId)).toList();
        if (subInfoList.size() == 1) {
            subId = subInfoList.get(0).getSubscriptionId();
        }

        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID)  {
            Log.i(TAG, "SubId not found");
            notifyEnableProfileStatus(slotId, refNum, false);
            return;
        }

        Intent intent = new Intent(ACTION_ENABLE_ESIM);
        intent.setPackage(mContext.getPackageName());
        intent.putExtra("slotId", slotId);
        intent.putExtra("refNum", refNum);
        int portIndex = getPortIndex(slotId);
        if (portIndex == TelephonyManager.INVALID_PORT_INDEX) {
            portIndex = TelephonyManager.DEFAULT_PORT_INDEX;
        }

        Log.d(TAG, "handleEnableProfile : " + " portIndex : " + portIndex + " subId: " + subId +
                " cardId: " + cardId);
        try {
            mEuiccMgr.createForCardId(cardId).switchToSubscription(subId, portIndex,
                    PendingIntent.getBroadcast(mContext, REQUEST_CODE, intent,
                    PendingIntent.FLAG_IMMUTABLE));
        } catch (Exception e) {
            Log.e(TAG, "Exception during switchToSubscription: " + e);
            notifyEnableProfileStatus(slotId, refNum, false);
        }
    }

    private void handleGetAllEsimProfiles(int slotId, int refNum) {
        int cardId = getCardId(slotId);
        if (cardId == TelephonyManager.UNINITIALIZED_CARD_ID) {
            // Incase where the getAllEsimProfiles indication received even before the
            // card status indication, we should wait for the card status to be available
            // inorder to send the list of esim profiles.
            // Retry for maximum 120 secs.
            if (mRetryCounter < RETRY_LIMIT) {
                mHandler.sendMessageDelayed(mHandler.obtainMessage(EVENT_GET_ALL_ESIM_PROFILES,
                        slotId, refNum), 1000);
                mRetryCounter++;
            }
            return;
        }
        mRetryCounter = 0;
        List<String> iccidList = getEsimProfileIccIds(slotId, cardId);
        Log.d(TAG, "handleGetAllEsimProfiles: " + iccidList);
        boolean result = (iccidList != null);
        if (mQtiRadioProxy != null) {
            try {
                mQtiRadioProxy.sendAllEsimProfiles(slotId, result, refNum, iccidList);
            } catch (Exception ex) {
                Log.e(TAG, "Exception during sendAllEsimProfiles: " + ex);
            }
        } else {
            Log.d(TAG, "mQtiRadioxProxy is null");
        }
    }

    private void notifyEnableProfileStatus(int slotId, int refNum, boolean result) {
        Log.d(TAG, "notifyEnableProfileStatus: " + result);
        if (mQtiRadioProxy != null) {
            try {
                mQtiRadioProxy.notifyEnableProfileStatus(slotId, refNum, result);
            } catch (RemoteException ex) {
                Log.e(TAG, "Exception during notifyEnableProfileStatus: " + ex);
            }
        } else {
            Log.d(TAG, "mQtiRadioProxy is null");
        }
    }

    private void notifyDisableProfileStatus(int slotId, int refNum, boolean result) {
        Log.d(TAG, "notifyDisableProfileStatus: " + result);
        if (mQtiRadioProxy != null) {
            try {
                mQtiRadioProxy.notifyDisableProfileStatus(slotId, refNum, result);
            } catch (RemoteException ex) {
                Log.e(TAG, "Exception during notifyDisableProfileStatus: " + ex);
            }
        } else {
            Log.d(TAG, "mQtiRadioProxy is null");
        }
    }

    final class QtiNtnProfileCallback extends QtiRadioProxy.IQtiRadioInternalCallback {
        @Override
        public void onGetAllEsimProfiles(int slotId, int refNum) {
            Log.i(TAG, "onGetAllEsimProfiles");
            mHandler.sendMessage(mHandler.obtainMessage(EVENT_GET_ALL_ESIM_PROFILES, slotId,
                    refNum));
        }

        public void onEnableEsimProfile(int slotId, int refNum, String iccId) {
            Log.d(TAG, "onEnableEsimProfile : " + iccId);
            mHandler.sendMessage(mHandler.obtainMessage(EVENT_ENABLE_ESIM, slotId, refNum, iccId));
        }

        public void onDisableEsimProfile(int slotId, int refNum, String iccId) {
            Log.d(TAG, "onDisableEsimProfile : " + iccId);
            mHandler.sendMessage(mHandler.obtainMessage(EVENT_DISABLE_ESIM, slotId, refNum, iccId));
        }
    }

    private final class QtiNtnProfileHandler extends Handler {
        QtiNtnProfileHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.i(TAG, "msg.what: " + msg.what);
            switch(msg.what) {
                case EVENT_GET_ALL_ESIM_PROFILES: {
                    handleGetAllEsimProfiles(msg.arg1 /* slotId */, msg.arg2 /* refNum */);
                    break;
                }

                case EVENT_DISABLE_ESIM: {
                    handleDisableProfile(msg.arg1 /* slotId */, msg.arg2 /* refNum */,
                            (String) msg.obj /* iccid */);
                    break;
                }

                case EVENT_ENABLE_ESIM: {
                    handleEnableProfile(msg.arg1 /* slotId */, msg.arg2 /* refNum */,
                            (String) msg.obj /* iccid */);
                    break;
                }

                case EVENT_NOTIFY_ENABLE_ESIM_STATUS: {
                    notifyEnableProfileStatus(msg.arg1 /* slotId */, msg.arg2 /* refNum */,
                            (int) msg.obj == EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_OK);
                    break;
                }

                case EVENT_NOTIFY_DISABLE_ESIM_STATUS: {
                    notifyDisableProfileStatus(msg.arg1 /* slotId */, msg.arg2 /* refNum */,
                            (int) msg.obj == EuiccManager.EMBEDDED_SUBSCRIPTION_RESULT_OK);
                    break;
                }

                default: {
                    Log.e(TAG, "Unknown event");
                }
            }
        }
    }

    protected void cleanUp() {
        mQtiRadioProxy.unRegisterInternalCallback(mQtiNtnProfileCallback);
        mContext.unregisterReceiver(mReceiver);
        if (mHandlerThread != null) {
            mHandlerThread.quitSafely();
            mHandlerThread = null;
        }
    }
}