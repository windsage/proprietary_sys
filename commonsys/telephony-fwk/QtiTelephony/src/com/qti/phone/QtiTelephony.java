/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qti.phone;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

public class QtiTelephony {
    private static final String LOG_TAG = "QtiTelephony";

    private Context mContext;
    private QtiRadioProxy mQtiRadioProxy;
    QtiTelephonyCallback mQtiTelephonyCallback;
    private Handler mHandler;

    /* Event Constants */
    private static final int EVENT_MCFG_REFRESH_IND = 1;

    public QtiTelephony(Context context, QtiRadioProxy radioProxy) {
        mQtiRadioProxy = radioProxy;
        mContext = context;

        HandlerThread headlerThread = new HandlerThread(LOG_TAG);
        headlerThread.start();
        mHandler = new QtiTelephonyHandler(headlerThread.getLooper());

        TelephonyManager tm = (TelephonyManager) mContext.
                getSystemService(Context.TELEPHONY_SERVICE);

        mQtiTelephonyCallback = new QtiTelephonyCallback();
        mQtiRadioProxy.registerInternalCallback(mQtiTelephonyCallback);

    }

    private final class QtiTelephonyHandler extends Handler {
        QtiTelephonyHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case EVENT_MCFG_REFRESH_IND: {
                    logd("EVENT_MCFG_REFRESH_IND ");
                    handleMcfgRefreshInfo((QtiMcfgRefreshInfo)msg.obj);
                    break;
                }

                default: {
                    logd("invalid message " + msg.what);
                    break;
                }
            }
        }
    }

    class QtiTelephonyCallback extends QtiRadioProxy.IQtiRadioInternalCallback {
        @Override
        public void onMcfgRefresh(QtiMcfgRefreshInfo refreshInfo) {
            logi("onMcfgRefresh");
            mHandler.sendMessage(mHandler.obtainMessage(EVENT_MCFG_REFRESH_IND, -1, -1,
                    refreshInfo));
        }
    }

    private void handleMcfgRefreshInfo(QtiMcfgRefreshInfo refreshInfo) {
        if (refreshInfo == null) return;
        logd("handleMcfgRefreshInfo refreshInfo: " + refreshInfo);
        int simState = TelephonyManager.getSimStateForSlotIndex(refreshInfo.getSubId());
        logd("handleMcfgRefreshInfo refreshInfo SimState: " + simState);
        if ((simState == TelephonyManager.SIM_STATE_LOADED) &&
                (refreshInfo.getMcfgState() == QtiMcfgRefreshInfo.MCFG_REFRESH_COMPLETE)) {
            logd("handleMcfgRefreshInfo Update carrier config");
            // Request the carrier config manager to update the config for mPhoneId
            CarrierConfigManager configManager =
                    (CarrierConfigManager) mContext.getSystemService(
                    Context.CARRIER_CONFIG_SERVICE);
            configManager.updateConfigForPhoneId(refreshInfo.getSubId(),
                    Intent.SIM_STATE_LOADED);
        } else {
            logd("handleMcfgRefreshInfo carrier config update not required");
        }
    }

    public void destroy() {
        logi("destroy");
        mQtiRadioProxy.unRegisterInternalCallback(mQtiTelephonyCallback);
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
