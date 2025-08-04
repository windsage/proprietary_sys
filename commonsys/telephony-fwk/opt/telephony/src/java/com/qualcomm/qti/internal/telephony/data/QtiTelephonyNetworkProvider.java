/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.internal.telephony.data;

import android.annotation.NonNull;
import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.data.PhoneSwitcher;
import com.android.internal.telephony.data.TelephonyNetworkProvider;
import com.android.internal.telephony.flags.FeatureFlags;
import com.android.telephony.Rlog;

import com.qti.extphone.DualDataRecommendation;

import java.util.Map;

public class QtiTelephonyNetworkProvider extends TelephonyNetworkProvider {

    private String LOG_TAG = "QtiTNP";

    private static final int EVENT_DUAL_DATA_RECOMMENDATION = 101;

    private Handler mHandler;
    private DualDataRecommendation mDualDataRecommendation;

    /**
     * Constructor
     *
     * @param looper The looper for event handling
     * @param context The context
     * @param featureFlags Android feature flags
     */
    public QtiTelephonyNetworkProvider(@NonNull Looper looper, @NonNull Context context,
            @NonNull FeatureFlags featureFlags) {
        super(looper, context, featureFlags);

        PhoneSwitcher phoneSwitcher = PhoneSwitcher.getInstance();
        if (phoneSwitcher != null && phoneSwitcher instanceof QtiPhoneSwitcher) {
            QtiPhoneSwitcher qtiPhoneSwitcher = (QtiPhoneSwitcher) phoneSwitcher;
            mHandler = new QtiTelephonyNetworkProviderInternalHandler(looper);
            for (Phone phone : PhoneFactory.getPhones()) {
                qtiPhoneSwitcher.registerForDualDataRecommendation(mHandler,
                        EVENT_DUAL_DATA_RECOMMENDATION, phone.getPhoneId());
            }
        }
    }

    public class QtiTelephonyNetworkProviderInternalHandler extends Handler {

        QtiTelephonyNetworkProviderInternalHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            AsyncResult ar;
            switch (msg.what) {
                case EVENT_DUAL_DATA_RECOMMENDATION: {
                    Rlog.d(LOG_TAG, "EVENT_DUAL_DATA_RECOMMENDATION");
                    ar = (AsyncResult)msg.obj;
                    if (ar.exception != null) {
                        Rlog.d(LOG_TAG, "EVENT_DUAL_DATA_RECOMMENDATION, Exception: "
                                + ar.exception);
                        ar.result = null;
                    }
                    if (ar.result != null) {
                        onDualDataRecommendation((DualDataRecommendation)(ar.result));
                    }
                    break;
                }
            }
        }
    }

    private void onDualDataRecommendation(@NonNull DualDataRecommendation rec) {
        mDualDataRecommendation = rec;
        if (rec.getRecommendedSub() == DualDataRecommendation.NON_DDS) {
            reevaluateNetworkRequests("Dual Data Recommendation");
        }
    }

    public DualDataRecommendation getDualDataRecommendation() {
        return mDualDataRecommendation;
    }
}