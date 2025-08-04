/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 * Not a Contribution.
 *
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qualcomm.qti.internal.telephony.data;

import android.annotation.NonNull;
import android.app.AlertDialog;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.AccessNetworkConstants;
import android.telephony.AccessNetworkConstants.TransportType;
import android.telephony.Annotation.DataFailureCause;
import android.telephony.DataFailCause;
import android.telephony.TelephonyManager;
import android.telephony.data.ApnSetting;
import android.telephony.data.DataCallResponse;
import android.telephony.data.DataProfile;
import android.util.SparseArray;
import android.view.WindowManager;

import com.android.internal.telephony.data.DataEvaluation.DataEvaluationReason;
import com.android.internal.telephony.data.DataNetworkController;
import com.android.internal.telephony.data.DataNetworkController.NetworkRequestList;
import com.android.internal.telephony.data.DataRetryManager;
import com.android.internal.telephony.data.DataRetryManager.DataRetryManagerCallback;
import com.android.internal.telephony.data.DataServiceManager;
import com.android.internal.telephony.data.DataUtils;
import com.android.internal.telephony.flags.FeatureFlags;
import com.android.internal.telephony.Phone;

import com.qualcomm.qti.internal.telephony.data.QtiDataResetEventTracker;

import java.util.HashSet;
import java.util.Set;

public class QtiDataRetryManager extends DataRetryManager {

    // Maximum data reject count
    public static final int MAX_PDP_REJECT_COUNT = 3;
    // Data reset event tracker to know reset events.
    private QtiDataResetEventTracker mQtiDataResetEventTracker = null;
    // data reject dialog, made static because only one dialog object can be
    // used between multiple dataconnection objects.
    protected static AlertDialog mDataRejectDialog = null;
    // Store data reject cause for comparison
    private String mDataRejectReason = "NONE";
    // Store data reject count
    private int mDataRejectCount = 0;
    // Store data reject cause code
    private int mPdpRejectCauseCode = 0;

    private QtiDataConfigManager mQtiDataConfigManager;

    /**
     * Reasons for which throttling and retry entries should not be cleared when PDP reject
     * feature is enabled.
     */
    private final Set<Integer> mNoResetReasons = new HashSet<>() {{
        add(RESET_REASON_DATA_PROFILES_CHANGED);
        add(RESET_REASON_DATA_CONFIG_CHANGED);
    }};

    /**
     * Constructor
     *
     * @param phone The phone instance.
     * @param dataNetworkController Data network controller.
     * @param dataServiceManagers Data Service Manager instances for WWAN and WLAN transports.
     * It is used to receive unthrottle event per APN from RIL.
     * @param looper The looper to be used by the handler. Currently the handler thread is the
     * phone process's main thread.
     * @param dataRetryManagerCallback Data retry callback.
     */
    public QtiDataRetryManager(@NonNull Phone phone,
            @NonNull DataNetworkController dataNetworkController,
            @NonNull SparseArray<DataServiceManager> dataServiceManagers,
            @NonNull Looper looper, @NonNull FeatureFlags featureFlags,
            @NonNull DataRetryManager.DataRetryManagerCallback dataRetryManagerCallback) {
        super(phone, dataNetworkController, dataServiceManagers, looper, featureFlags,
                dataRetryManagerCallback);
        log("QtiDataRetryManager: constructor");
        mQtiDataConfigManager = (QtiDataConfigManager) mDataConfigManager;
    }


    /**
     * DataRetryManager#onReset() is designed to cancel all retry and throttling entries.
     * For the PDP reject feature on WCDMA, expectation is to continue with the existing
     * throttling and retry entries if APNs or data configs are changed.
     */
    protected void onReset(@RetryResetReason int reason) {
        if (mQtiDataConfigManager.isPdpRejectConfigEnabled()
                && mDataRejectCount > 0
                && mNoResetReasons.contains(reason)) {
            log("Skipping reset for " + resetReasonToString(reason));
            return;
        }
        super.onReset(reason);
    }

    /**
     * Check if the specified data profile corresponds to default type
     *
     * @param dataProfile The data profile to check.
     * @return {@code true} if the data profile has a TYPE_DEFAULT bitmask set.
     */
    boolean isDataProfileOfTypeDefault(DataProfile dataProfile) {
        if (dataProfile != null
                && dataProfile.getApnSetting() != null
                && (dataProfile.getApnSetting().getApnTypeBitmask() & ApnSetting.TYPE_DEFAULT)
                == ApnSetting.TYPE_DEFAULT) {
            log("isDataProfileOfTypeDefault: true, dataProfile: " + dataProfile);
            return true;
        }
        return false;
    }

    /**
     * @return {@code true} if PDP reject retry is in progress
     */
    boolean isPdpRejectRetryOngoing() {
        if (mQtiDataConfigManager.isPdpRejectConfigEnabled()
                && mDataRejectCount > 0) {
            log("isPdpRejectRetryOngoing: true");
            return true;
        }
        return false;
    }

    /**
     * Reset data reject params on data call success
     */
    @Override
    public void handlePdpRejectCauseSuccess() {
        if (mDataRejectCount > 0) {
            log("handlePdpRejectCauseSuccess: reset reject count");
            resetDataRejectCounter();
        }
    }

    /**
     * returns true if data reject cause matches errors listed
     */
    private boolean isMatchingPdpRejectCause(int dataFailCause) {
        return dataFailCause == DataFailCause.USER_AUTHENTICATION
                || dataFailCause == DataFailCause.SERVICE_OPTION_NOT_SUBSCRIBED
                || dataFailCause == DataFailCause.MULTI_CONN_TO_SAME_PDN_NOT_ALLOWED;
    }

    /**
     * Reset data reject count and reason
     */
    private void resetDataRejectCounter() {
        mDataRejectCount = 0;
        mDataRejectReason = "NONE";
    }

    /**
     * Data reset event listener. onResetEvent is called
     * whenever any data reset event occurs
     */
    private QtiDataResetEventTracker.ResetEventListener mResetEventListener =
           new QtiDataResetEventTracker.ResetEventListener() {
        @Override
        public void onResetEvent(boolean retry, String reason) {
            log("onResetEvent: retry=" + retry);
            // Dismiss dialog.
            if (mDataRejectDialog != null && mDataRejectDialog.isShowing()) {
                log("onResetEvent: Dismiss dialog");
                mDataRejectDialog.dismiss();
            }
            mQtiDataResetEventTracker.stopResetEventTracker();
            resetDataRejectCounter();
            DataEvaluationReason dataEvaluationReason = DataEvaluationReason.DATA_RETRY;
            if (reason.equalsIgnoreCase("DATA_ENABLED_CHANGED")) {
                onReset(RESET_REASON_DATA_ENABLED_CHANGED);
                dataEvaluationReason = DataEvaluationReason.DATA_ENABLED_CHANGED;
            }
            if (retry) {
                mDataNetworkController.sendMessage(mDataNetworkController
                        .obtainMessage(mDataNetworkController
                        .EVENT_REEVALUATE_UNSATISFIED_NETWORK_REQUESTS,
                        dataEvaluationReason));
            }
        }
    };

    /**
     * returns true if networkType is UTRAN, else false
     */
    private boolean isWcdma(int networkType) {
        return DataUtils.networkTypeToAccessNetworkType(networkType)
                == AccessNetworkConstants.AccessNetworkType.UTRAN;
    }

    protected void onEvaluateDataSetupRetry(@NonNull DataProfile dataProfile,
            @TransportType int transport, @NonNull NetworkRequestList requestList,
            @DataFailureCause int cause, long retryDelayMillis) {
        if (mQtiDataConfigManager.isPdpRejectConfigEnabled()
                && requestList.stream()
                .filter(r -> r.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
                .findAny().isPresent()) {
            String reason = DataFailCause.toString(cause);
            if (isMatchingPdpRejectCause(cause)
                    && isWcdma(mDataNetworkController.getDataNetworkType(transport))) {
                if (mQtiDataResetEventTracker == null) {
                    mQtiDataResetEventTracker = new QtiDataResetEventTracker(transport,
                            mPhone, mResetEventListener);
                }

                // If previously rejected code is not same as current data reject reason,
                // then reset the count and reset the reject reason.
                if (!reason.equalsIgnoreCase(mDataRejectReason)) {
                    resetDataRejectCounter();
                }
                if (mDataRejectCount == 0) {
                    mQtiDataResetEventTracker.startResetEventTracker();
                }
                mDataRejectCount++;
                mDataRejectReason = reason;
                log("RejectCount: " + mDataRejectCount + ", RejectReason: " + mDataRejectReason);
                // If MAX Reject count reached, display pop-up to user.
                if (MAX_PDP_REJECT_COUNT <= mDataRejectCount) {
                    log("onEvaluateDataSetupRetry: reached max retry count");
                    displayPdpRejectPopup(cause);
                    retryDelayMillis = Long.MAX_VALUE;
                } else if (retryDelayMillis == DataCallResponse.RETRY_DURATION_UNDEFINED) {
                    retryDelayMillis = mQtiDataConfigManager.getPdpRejectRetryDelay();
                    log("onEvaluateDataSetupRetry: delay from config: " + retryDelayMillis);
                }
            } else {
                log("onDataSetupCompleteError: reset reject count");
                resetDataRejectCounter();
            }
        }
        super.onEvaluateDataSetupRetry(dataProfile, transport, requestList,
            cause, retryDelayMillis);
    }

    /**
     * This function will display the pdp reject message
     */
    private void displayPdpRejectPopup(int dataFailCause) {
        log("displayPdpRejectPopup : " + DataFailCause.toString(dataFailCause));
        String title = mQtiDataConfigManager.getPdpRejectDialogTitle();
        String message = null;

        switch (dataFailCause) {
            case DataFailCause.USER_AUTHENTICATION:
                message = mQtiDataConfigManager.getPdpRejectCauseUserAuthentication();
                break;
            case DataFailCause.SERVICE_OPTION_NOT_SUBSCRIBED:
                message = mQtiDataConfigManager.getPdpRejectCauseServiceNotSubscribed();
                break;
            case DataFailCause.MULTI_CONN_TO_SAME_PDN_NOT_ALLOWED:
                message = mQtiDataConfigManager.getPdpRejectCauseSamePdnNotAllowed();
                break;
            default:
                loge("displayPdpRejectPopup: invalid dataFailCause: " + dataFailCause);
                return;
        }

        if (mDataRejectDialog == null || !mDataRejectDialog.isShowing()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(
                    mPhone.getContext());
            builder.setPositiveButton(android.R.string.ok, null);
            mDataRejectDialog = builder.create();
        }

        mDataRejectDialog.setMessage(message);
        mDataRejectDialog.setCanceledOnTouchOutside(false);
        mDataRejectDialog.setTitle(title);
        mDataRejectDialog.getWindow().setType(
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        mDataRejectDialog.show();
    }
}
