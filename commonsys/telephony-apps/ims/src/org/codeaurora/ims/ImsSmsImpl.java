/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package org.codeaurora.ims;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.provider.Telephony.Sms;
import android.telephony.ims.stub.ImsSmsImplBase;
import android.telephony.SmsMessage;
import com.qualcomm.ims.utils.Log;

import org.codeaurora.ims.sms.IncomingSms;
import org.codeaurora.ims.sms.SmsResponse;
import org.codeaurora.ims.sms.StatusReport;
import org.codeaurora.ims.utils.QtiImsExtUtils;
import org.codeaurora.telephony.utils.AsyncResult;

import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/*
  AOSP calls to this class will execute on Binder (Runnable::run) threads
  due to blocking API(s) from Framework -> QC IMS Service -> QCRIL
    Applicable API(s): sendSms, acknowledgeSms, acknowledgeSmsReport,
                       getSmsFormat, onReady
*/
public class ImsSmsImpl extends ImsSmsImplBase {
    static final String LOG_TAG = "ImsSmsImpl";
    private Context mContext;
    private ImsSenderRxr mSmsCi; /* Commands Interface */
    private int mPhoneId = 0;
    /* Stores message ref comes from modem in #sendResponseToFramework
       with a token and used to retrive this token to send to
       framework later  when the same messge ref is received
       in status report */
    private HashMap<Integer, Integer> mSmsMap = new HashMap<Integer, Integer>();
    private Handler mHandler;
    private AtomicBoolean mSmsFwkReady = new AtomicBoolean();
    private int mIncomingToken = 0;
    private TelephonyManager mTelephonyManager;

    private final int EVENT_SEND_SMS_RESPONSE = 1;
    private final int EVENT_UNSOL_INCOMING_SMS = 2;
    private final int EVENT_UNSOL_STATUS_REPORT = 3;

    public static final int MT_IMS_STATUS_VALIDATION_NONE = 0;
    public static final int MT_IMS_STATUS_VALIDATION_PASS = 1;
    public static final int MT_IMS_STATUS_VALIDATION_FAIL = 2;

    public ImsSmsImpl(Context context, int phoneId, ImsSenderRxr ci) {
        super(Runnable::run);
        mContext = context;
        mSmsCi = ci;
        mPhoneId = phoneId;
        mHandler = new ImsSmsHandler(mContext.getMainLooper());
        mSmsCi.setOnIncomingImsSms(mHandler, EVENT_UNSOL_INCOMING_SMS, null);
        mSmsCi.setOnImsSmsStatusReport(mHandler, EVENT_UNSOL_STATUS_REPORT, null);
        mTelephonyManager = (TelephonyManager)
                mContext.getSystemService(Context.TELEPHONY_SERVICE);
    }

    private void sendStatusReportErrorToRIL(int msgRef){
        mSmsCi.acknowledgeSmsReport(msgRef, STATUS_REPORT_STATUS_ERROR, null);
    }

    private void sendDeliveryErrorToRIL(){
        mSmsCi.acknowledgeSms(0, DELIVER_STATUS_ERROR_GENERIC, null);
    }

    private int getSubId(){
        SubscriptionManager subscriptionManager = (SubscriptionManager) mContext.getSystemService(
                Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        if (subscriptionManager == null) {
            return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        }
        subscriptionManager = subscriptionManager.createForAllUserProfiles();
        return subscriptionManager.getSubscriptionId(mPhoneId);
    }

    private boolean isNetworkRoaming() {
        int subId = getSubId();
        if(mTelephonyManager == null ||
            subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            return false;
        }
        return mTelephonyManager.createForSubscriptionId(subId).isNetworkRoaming();
    }

    private int maybeAdjustSendStatus(SmsResponse smsResp) {
        int status = smsResp.getResult();
        int rat = smsResp.getRat();
        int networkRpCause = smsResp.getNetworkErrorCode();
        final int INVALID_RP_CAUSE = -1;

        if (status != ImsSmsImplBase.SEND_STATUS_ERROR_FALLBACK &&
                status != ImsSmsImplBase.SEND_STATUS_ERROR_RETRY) {
            return status;
        }

        if (networkRpCause != INVALID_RP_CAUSE) {
            int[] supportedRpCause = QtiImsExtUtils.getIntArray(mPhoneId, mContext,
                    "supported_sms_rp_cause_values_int_array");
            boolean isSupportedRpCause = supportedRpCause != null &&
                    Arrays.stream(supportedRpCause).anyMatch(value -> value == networkRpCause);

            if (!isSupportedRpCause) {
                return ImsSmsImplBase.SEND_STATUS_ERROR;
            }

            int[] fallbackRpCause = QtiImsExtUtils.getIntArray(mPhoneId, mContext,
                    "sms_rp_cause_values_to_fallback_int_array");
            boolean isFallbackRpCause = fallbackRpCause != null && Arrays.stream(fallbackRpCause).
                    anyMatch(value -> value == networkRpCause);

            if (isFallbackRpCause) {
                // Not returning FALLBACK from here as FALLBACK is not supported for few carriers in
                // roaming.
                status = ImsSmsImplBase.SEND_STATUS_ERROR_FALLBACK;
            }
        }

        /* Requires additional validations if status is SEND_STATUS_ERROR_FALLBACK otherwise return.
         * If status is SEND_STATUS_ERROR_FALLBACK based on operators it can be converted to
         * SEND_STATUS_ERROR_RETRY or SEND_STATUS_ERROR */
        if (status == ImsSmsImplBase.SEND_STATUS_ERROR_RETRY) {
            return status;
        }

        if (QtiImsExtUtils.isCarrierConfigEnabled(mPhoneId, mContext,
                    "config_retry_sms_over_ims")) {
            Log.d(this,"maybeAdjustSendStatus: retry SMS over IMS");
            return ImsSmsImplBase.SEND_STATUS_ERROR_RETRY;
        }

        /* Carrier config "fallback_sms_not_allowed_in_roaming" is for an operator
           to restrict fallback sms in roaming for specific rats. If rat is invalid,
           continue with legacy behavior to support vendor freeze targets */
        if (isNetworkRoaming() && QtiImsExtUtils.isCarrierConfigEnabled(mPhoneId, mContext,
                "fallback_sms_not_allowed_in_roaming")
                && (rat ==  RadioTech.RADIO_TECH_INVALID ||  rat == RadioTech.RADIO_TECH_WIFI
                || rat == RadioTech.RADIO_TECH_C_IWLAN)) {
            Log.d(this,"maybeAdjustSendStatus: Roaming network, fallback not allowed.");
            return ImsSmsImplBase.SEND_STATUS_ERROR;
        }
        return status;
    }

    private void sendResponseToFramework(AsyncResult ar) {
        SmsResponse smsResponse = (SmsResponse) ar.result;
        int token = (int) ar.userObj;
        if (smsResponse == null || !mSmsFwkReady.get()) {
            Log.w(this, "smsResponse =" + smsResponse +
                    " isFrameworkReady=" + mSmsFwkReady.get());
            return;
        }
        final int ref = smsResponse.getMsgRef();
        int result = maybeAdjustSendStatus(smsResponse);
        final int reason = smsResponse.getReason();
        final int networkErrorCode = smsResponse.getNetworkErrorCode();
        Log.i(this,"onSendSmsResult:: token:"+token+
                " smsResponse:"+ smsResponse);

        mSmsMap.put(ref, token);
        try {
            if (result == ImsSmsImplBase.SEND_STATUS_OK) {
               onSendSmsResultSuccess(token, ref);
            } else {
               onSendSmsResultError(token, ref, result, reason, networkErrorCode);
            }
        } catch (RuntimeException ex) {
            Log.e(this, "onSendSmsResult: Ex:" + ex.getMessage());
        }
    }

    private void sendSmsToFramework(AsyncResult ar) {
        IncomingSms incomingSms = (IncomingSms) ar.result;
        if (incomingSms == null || !mSmsFwkReady.get()) {
            Log.w(this, "incomingSms =" + incomingSms +
                    " isFrameworkRead=" + mSmsFwkReady.get());
            sendDeliveryErrorToRIL();
            return;
        }
        if(mIncomingToken == Integer.MAX_VALUE){ mIncomingToken = 0; }

        /**verstat info is applicable only to 3gpp MT Sms.
         * valid values are:
         * MT_IMS_STATUS_VALIDATION_NONE.
         * MT_IMS_STATUS_VALIDATION_PASS.
         * MT_IMS_STATUS_VALIDATION_FAIL.
         *
         * For other formats, ims service always gets MT_IMS_STATUS_VALIDATION_NONE.
         */
        Log.i(this,"onSmsReceived:: token:"+mIncomingToken+
                " incomingSms:" + incomingSms);

        try {
            onSmsReceived(mIncomingToken++, incomingSms.getFormat(), incomingSms.getPdu());
        } catch (RuntimeException ex) {
            Log.e(this, "onSmsReceived: Ex:" + ex.getMessage());
            sendDeliveryErrorToRIL();
        }
    }

    private boolean isSmsDeliverCompleted(StatusReport report) {
        if (report == null) {
            return false;
        }

        // For CDMA status report always return true
        if (SmsMessage.FORMAT_3GPP2.equals(report.getFormat())) {
            return true;
        }

        SmsMessage msg = SmsMessage.createFromPdu(report.getPdu(), report.getFormat());
        if (msg != null) {
            int status = msg.getStatus();
            Log.d(this, "isSmsDeliverCompleted : status = " + status);
            // return true if status is failed or not pending
            return (status >= Sms.STATUS_FAILED || status < Sms.STATUS_PENDING );
        }
        return false;
    }

    private void sendStatusReportToFramework(AsyncResult ar) {
        StatusReport statusReport = (StatusReport) ar.result;
        int token = 0;
        if (statusReport == null || !mSmsFwkReady.get()) {
            Log.w(this, "statusReport =" + statusReport +
                    " isFrameworkRead=" + mSmsFwkReady.get());
            sendStatusReportErrorToRIL(0);
            return;
        }
        final int ref = statusReport.getMsgRef();
        final String format =  statusReport.getFormat();
        Integer tokenObj = mSmsMap.get(ref);
        token = (tokenObj == null) ? -1 : tokenObj.intValue();

        Log.i(this,"onSmsStatusReportReceived:: token: "+token+
                " statusReport:" + statusReport);

        try {
            onSmsStatusReportReceived(token, ref, format, statusReport.getPdu());
        } catch (RuntimeException ex) {
            Log.e(this, "onSmsStatusReportReceived: Ex:" + ex.getMessage());
            sendStatusReportErrorToRIL(ref);
        }

        // Remove token from map if delivery status is not pending
        if (isSmsDeliverCompleted(statusReport)) {
            mSmsMap.remove(ref);
        }
    }

    private class ImsSmsHandler extends Handler {
        public ImsSmsHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.i(this, "Message received: what = " + msg.what);
            AsyncResult ar = (AsyncResult) msg.obj;
            if(ar == null){
                Log.e(this, "msg.obg is null");
                return;
            }
            switch (msg.what) {
                case EVENT_SEND_SMS_RESPONSE:
                    sendResponseToFramework(ar);
                    break;
                case EVENT_UNSOL_INCOMING_SMS:
                    sendSmsToFramework(ar);
                    break;
                case EVENT_UNSOL_STATUS_REPORT:
                    sendStatusReportToFramework(ar);
                    break;
                default:
                    Log.i(LOG_TAG, "Invalid Response");
            }
        }
    }

    @Override
    public void sendSms(int token, int messageRef, String format,
                        String smsc, boolean isRetry, byte[] pdu) {
        Log.i(LOG_TAG,"sendSms:: token:"+token+" msgRef:"+messageRef+
                " format:"+format+" isRetry:"+isRetry);

        mSmsCi.sendSms(messageRef, format, smsc, isRetry, pdu,
                mHandler.obtainMessage(EVENT_SEND_SMS_RESPONSE, token));
    }

    @Override
    public void acknowledgeSms(int token, int messageRef, int result) {
        Log.i(LOG_TAG,"acknowledgeSms:: token:"+token+" msgRef:"+messageRef+
                " result:"+result);

        mSmsCi.acknowledgeSms(messageRef, result, null);
    }

    @Override
    public void acknowledgeSmsReport(int token, int messageRef, int result) {
        Log.i(LOG_TAG,"acknowledgeSmsReport:: token:"+token+" msgRef:"+messageRef+
                " result:"+result);

        mSmsCi.acknowledgeSmsReport(messageRef, result, null);
    }

    @Override
    public String getSmsFormat() {
        return mSmsCi.getSmsFormat();
    }

    @Override
    public void onReady() {
        mSmsFwkReady.set(true);
    }
}
