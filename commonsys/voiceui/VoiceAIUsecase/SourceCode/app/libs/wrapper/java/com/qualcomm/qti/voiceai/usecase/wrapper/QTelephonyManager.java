/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.voiceai.usecase.wrapper;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.telephony.SubscriptionManager;

public class QTelephonyManager {
    private Context mContext;
    private TelephonyManager mTelephonyManager;
    private TelephonyManager mSub1TelephonyManager;
    private TelephonyManager mSub2TelephonyManager;
    private SubscriptionManager mSubscriptionManager;

    public QTelephonyManager(Context context) {
        mContext = context;
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class);
        mSubscriptionManager = mContext.getSystemService(SubscriptionManager.class);
        int subscriptionId1 = mSubscriptionManager.getSubscriptionId(0);
        int subscriptionId2 = mSubscriptionManager.getSubscriptionId(1);
        mSub1TelephonyManager = mTelephonyManager.createForSubscriptionId(subscriptionId1);
        mSub2TelephonyManager = mTelephonyManager.createForSubscriptionId(subscriptionId2);
    }

    public void registerTelephonyCallback(PhoneCallStateCallbackWrapper wrapper) {
        mSub1TelephonyManager.registerTelephonyCallback(mContext.getMainExecutor(), wrapper.mCallback);
    }

    public void unregisterTelephonyCallback(PhoneCallStateCallbackWrapper wrapper) {
        mSub1TelephonyManager.unregisterTelephonyCallback(wrapper.mCallback);
    }

    public void registerTelephonyCallbackForSub2(PhoneCallStateCallbackWrapper wrapper) {
        mSub2TelephonyManager.registerTelephonyCallback(mContext.getMainExecutor(), wrapper.mCallback);
    }

    public void unregisterTelephonyCallbackForSub2(PhoneCallStateCallbackWrapper wrapper) {
        mSub2TelephonyManager.unregisterTelephonyCallback(wrapper.mCallback);
    }
}
