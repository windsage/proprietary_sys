/*
 * Copyright (c) 2015, 2016, 2018-2021, 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 * Not a Contribution.
 * Apache license notifications and license are retained
 * for attribution purposes only.
 *
 * Copyright (c) 2006 The Android Open Source Project
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

package org.codeaurora.ims;

import com.qualcomm.ims.utils.Log;
import org.codeaurora.telephony.utils.AsyncResult;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.ims.ImsCallForwardInfo;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.ImsSsData;
import android.telephony.ims.ImsSsInfo;
import android.telephony.ims.ImsUtListener;
import android.telephony.ims.stub.ImsUtImplBase;

import androidx.annotation.VisibleForTesting;

import java.util.List;
import java.util.ArrayList;

import org.codeaurora.telephony.utils.CallForwardInfo;
import org.codeaurora.ims.QtiCallConstants;

public class ImsUtImpl extends ImsUtImplBase {

    private static final String LOG_TAG = "ImsUtImpl";
    private static final int MAX_REQUESTS_PENDING = 50; // TODO: Verify and set proper value!

    // Supplementary Service Events
    private static final int EVENT_QUERY_CF    = 1;
    private static final int EVENT_UPDATE_CF   = 2;
    private static final int EVENT_QUERY_CW    = 3;
    private static final int EVENT_UPDATE_CW   = 4;
    private static final int EVENT_QUERY_CLIR  = 5;
    private static final int EVENT_UPDATE_CLIR = 6;
    private static final int EVENT_QUERY_CLIP  = 7;
    private static final int EVENT_UPDATE_CLIP = 8;
    private static final int EVENT_QUERY_COLR  = 9;
    private static final int EVENT_UPDATE_COLR = 10;
    private static final int EVENT_QUERY_COLP  = 11;
    private static final int EVENT_UPDATE_COLP = 12;
    private static final int EVENT_QUERY_CB    = 13;
    private static final int EVENT_UPDATE_CB   = 14;
    private static final int EVENT_UNSOL_ON_SS = 15;

    // Used for various supp. services APIs.
    // See 27.007 +CCFC or +CLCK
    static final int SERVICE_CLASS_NONE       = 0; // no user input
    static final int SERVICE_CLASS_VOICE      = (1 << 0);
    static final int SERVICE_CLASS_DATA       = (1 << 1); //synonym for 16+32+64+128
    static final int SERVICE_CLASS_FAX        = (1 << 2);
    static final int SERVICE_CLASS_SMS        = (1 << 3);
    static final int SERVICE_CLASS_DATA_SYNC  = (1 << 4);
    static final int SERVICE_CLASS_DATA_ASYNC = (1 << 5);
    static final int SERVICE_CLASS_PACKET     = (1 << 6);
    static final int SERVICE_CLASS_PAD        = (1 << 7);
    static final int SERVICE_CLASS_MAX        = (1 << 7); // Max SERVICE_CLASS value

    // Call forwarding 'reason' values.
    static final int CF_REASON_UNCONDITIONAL    = 0;
    static final int CF_REASON_BUSY             = 1;
    static final int CF_REASON_NO_REPLY         = 2;
    static final int CF_REASON_NOT_REACHABLE    = 3;
    static final int CF_REASON_ALL              = 4;
    static final int CF_REASON_ALL_CONDITIONAL  = 5;
    static final int CF_REASON_NOT_LOGGED_IN    = 6;

    // UT Error strings the lower layers can send as part of response exception.
    static final String UT_ERROR_GENERIC             = "E_GENERIC_FAILURE";
    static final String UT_PASSWORD_INCORRECT        = "E_PASSWORD_INCORRECT";
    static final String UT_ERROR_NETWORK_UNSUPPORTED = "E_NETWORK_NOT_SUPPORTED";
    static final String UT_ERROR_FDN_FAILURE = "E_FDN_CHECK_FAILURE";
    static final String UT_ERROR_SS_MODIFIED_TO_DIAL = "E_SS_MODIFIED_TO_DIAL";
    static final String UT_ERROR_SS_MODIFIED_TO_USSD = "E_SS_MODIFIED_TO_USSD";
    static final String UT_ERROR_SS_MODIFIED_TO_SS = "E_SS_MODIFIED_TO_SS";
    static final String UT_ERROR_SS_MODIFIED_TO_DIAL_VIDEO = "E_SS_MODIFIED_TO_DIAL_VIDEO";

    // Permissions used by class.
    private final String MODIFY_PHONE_STATE = Manifest.permission.MODIFY_PHONE_STATE;
    private final String READ_PRIVILEGED_PHONE_STATE = Manifest.permission.READ_PRIVILEGED_PHONE_STATE;
    private final String READ_PHONE_STATE = Manifest.permission.READ_PHONE_STATE;

    private ImsServiceSub mServiceSub;
    private ImsSenderRxr mCi;
    private Context mContext;
    private final ImsUtListenerImpl mListenerImpl = new ImsUtListenerImpl();
    private final Handler mHandler = new ImsUtImplHandler();
    private static int requestId = -1;
    //Callforward status
    private static final int CF_DISABLED = 0;
    private static final int CF_ENABLED = 1;
    // Objects to handle synchronization and notification of closure
    private IOnCloseListener mOnCloseListener;
    private final Object mLock = new Object();
    private boolean mIsDisposed = false;

    public ImsUtImpl(ImsServiceSub serviceSub, ImsSenderRxr senderRxr, Context context) {
        mServiceSub = serviceSub;
        mCi = senderRxr;
        mContext = context;
        mCi.registerForSuppServiceIndication(mHandler, EVENT_UNSOL_ON_SS, null);
    }

    private void enforceReadPhoneState(String fn) {
        if (mContext.checkCallingOrSelfPermission(READ_PRIVILEGED_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED
                && mContext.checkCallingOrSelfPermission(READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            mContext.enforceCallingOrSelfPermission(READ_PHONE_STATE, fn);
        }
    }

    @VisibleForTesting
    public Handler getHandler() {
        return mHandler;
    }

    interface IOnCloseListener {
        public void onClosed(ImsUtImpl obj);
    }

    void setOnClosedListener(IOnCloseListener listener) {
        mOnCloseListener = listener;
    }

    private boolean isDisposed() {
        return mIsDisposed;
    }

    /**
     * Closes the object. This object is not usable after being closed.
     */
    @Override
    public void close() {
        mContext.enforceCallingOrSelfPermission(MODIFY_PHONE_STATE, "close");
        mIsDisposed = true;
        if (mCi != null) {
            mCi.unregisterForSuppServiceIndication(mHandler);
            mCi = null;
        }
        if (mOnCloseListener != null) {
            mOnCloseListener.onClosed(this);
            mOnCloseListener = null;
        }
    }

    /**
     * Retrieves the configuration of the call barring.
     */
    @Override
    public int queryCallBarring(int cbType) {
        return queryCallBarringForServiceClass(cbType, SERVICE_CLASS_NONE);
    }

    /**
     * Retrieves the configuration of the call barring for specified service class.
     */
    @Override
    public int queryCallBarringForServiceClass(int cbType, int serviceClass) {
        enforceReadPhoneState("queryCallBarring");
        int id = getIdForRequest();
        if (id < 0) {
            Log.e(this, "Invalid request id for queryCallBarring.");
            // ImsUt.java treats ids < 0 as an error.
            return -1;
        }

        return queryCallBarringForServiceClass(cbType, serviceClass, false /*expectMore*/,
                        null /*password*/,
                        mHandler.obtainMessage(EVENT_QUERY_CB, id, 0, this)) ? id : -1;
    }

    /**
     * Retrieves the configuration of the call barring for specified service class
     *  with expectMore information
     */
    public boolean queryCallBarringForServiceClass(int cbType, int serviceClass,
            boolean expectMore, String password, Message msg) {
        enforceReadPhoneState("queryCallBarring");
        if (isDisposed() || mCi == null) {
            dumpw("queryCallBarringForServiceClass");
            return false;
        }

        int facility = getFacilityFromCbType(cbType);
        if (facility == -1) {
            Log.e(this, "Unsupported call barring facility code in queryCallBarring.");
            return false;
        }
        if (facility == SuppSvcResponse.FACILITY_BS_MT) {
            mCi.suppSvcStatus(SuppSvcResponse.QUERY,
                    facility,
                    null,
                    password,
                    SERVICE_CLASS_VOICE,
                    msg, expectMore);
        } else {
            mCi.suppSvcStatus(SuppSvcResponse.QUERY,
                    facility, null, password, serviceClass,
                    msg, expectMore);
        }
        return true;
    }

    private int getFacilityFromCbType(int cbType) {
        // NOTE: Refer to getCBTypeFromFacility in ImsPhone.java. All codes
        //       there are converted to appropriate ImsQmiIF codes.
        if (cbType == ImsUtImplBase.CALL_BARRING_ALL_OUTGOING) {
            return SuppSvcResponse.FACILITY_BAOC;
        }
        else if (cbType == ImsUtImplBase.CALL_BARRING_OUTGOING_INTL) {
            return SuppSvcResponse.FACILITY_BAOIC;
        }
        else if (cbType == ImsUtImplBase.CALL_BARRING_OUTGOING_INTL_EXCL_HOME) {
            return SuppSvcResponse.FACILITY_BAOICxH;
        }
        else if (cbType == ImsUtImplBase.CALL_BARRING_ALL_INCOMING) {
            return SuppSvcResponse.FACILITY_BAIC;
        }
        else if (cbType == ImsUtImplBase.CALL_BLOCKING_INCOMING_WHEN_ROAMING) {
            return SuppSvcResponse.FACILITY_BAICr;
        }
        else if (cbType == ImsUtImplBase.CALL_BARRING_ALL) {
            return SuppSvcResponse.FACILITY_BA_ALL;
        }
        else if (cbType == ImsUtImplBase.CALL_BARRING_OUTGOING_ALL_SERVICES) {
            return SuppSvcResponse.FACILITY_BA_MO;
        }
        else if (cbType == ImsUtImplBase.CALL_BARRING_INCOMING_ALL_SERVICES) {
            return SuppSvcResponse.FACILITY_BA_MT;
        }
        else if (cbType == ImsUtImplBase.CALL_BARRING_SPECIFIC_INCOMING_CALLS) {
            return SuppSvcResponse.FACILITY_BS_MT;
        }
        else if (cbType == ImsUtImplBase.CALL_BARRING_ANONYMOUS_INCOMING) {
            return SuppSvcResponse.FACILITY_BAICa;
        }
        else { // Unsupported Call Barring Code
            return -1;
        }
    }

    public ImsCallForwardInfo[] toImsCallForwardInfo(ImsCallForwardTimerInfo[] cfInfoList) {
        ImsCallForwardInfo[] callForwardInfoList
                = new ImsCallForwardInfo[cfInfoList.length];
        int callForwardStatus = CF_DISABLED;
        int condition = ImsUtImplBase.INVALID_RESULT;
        int timeSeconds = 0;
        for (int i = 0; i < cfInfoList.length; i++) {
            ImsCallForwardTimerInfo cfInfo = cfInfoList[i];

            if (cfInfo.status == CF_ENABLED) {
                callForwardStatus = CF_ENABLED; // Enabled
            } else if (cfInfo.status == CF_DISABLED) {
                callForwardStatus = CF_DISABLED; // Disabled
            } else {
                Log.e(this, "Bad status in Query CF response.");
                return null;
            }

            if (cfInfo.reason == CF_REASON_UNCONDITIONAL) {
                condition = ImsCallForwardInfo.CDIV_CF_REASON_UNCONDITIONAL;
            } else if (cfInfo.reason == CF_REASON_BUSY) {
               condition = ImsCallForwardInfo.CDIV_CF_REASON_BUSY;
            } else if (cfInfo.reason == CF_REASON_NO_REPLY) {
                condition = ImsCallForwardInfo.CDIV_CF_REASON_NO_REPLY;
                // Time present only in this case.
                timeSeconds = cfInfo.timeSeconds;
            } else if (cfInfo.reason == CF_REASON_NOT_REACHABLE) {
                condition = ImsCallForwardInfo.CDIV_CF_REASON_NOT_REACHABLE;
            } else if (cfInfo.reason == CF_REASON_ALL) {
                condition = ImsCallForwardInfo.CDIV_CF_REASON_ALL;
            } else if (cfInfo.reason == CF_REASON_ALL_CONDITIONAL) {
                condition = ImsCallForwardInfo.CDIV_CF_REASON_ALL_CONDITIONAL;
            } else if (cfInfo.reason == CF_REASON_NOT_LOGGED_IN) {
                condition = ImsCallForwardInfo.CDIV_CF_REASON_NOT_LOGGED_IN;
            } else {
                Log.e(this, "Bad reason in Query CF response.");
                return null;
            }
            callForwardInfoList[i] = new ImsCallForwardInfo(condition,
                    callForwardStatus, cfInfo.toa, cfInfo.serviceClass,
                    new String(cfInfo.number), timeSeconds);
        }
        return callForwardInfoList;
    }

    /**
     * Retrieves the configuration of the call forward.
     */
    @Override
    public int queryCallForward(int condition, String number) {
        return queryCFForServiceClass(condition, number, SERVICE_CLASS_VOICE);
    }

    /**
     * Retrieves the configuration of the call forward for specified service class.
     */
    public int queryCFForServiceClass(int condition, String number, int serviceClass) {
        enforceReadPhoneState("queryCallForward");
        if (isDisposed() || mCi == null) {
            dumpw("queryCFForServiceClass");
            return -1;
        }
        int reason = -1;

        int id = getIdForRequest();
        if (id < 0) {
            Log.e(this, "Invalid request id for queryCallForward.");
            // ImsUt.java treats ids < 0 as an error.
            return -1;
        }

        if (condition == ImsCallForwardInfo.CDIV_CF_REASON_UNCONDITIONAL) {
            reason = CF_REASON_UNCONDITIONAL;
        }
        else if (condition == ImsCallForwardInfo.CDIV_CF_REASON_BUSY) {
            reason = CF_REASON_BUSY;
        }
        else if (condition == ImsCallForwardInfo.CDIV_CF_REASON_NO_REPLY) {
            reason = CF_REASON_NO_REPLY;
        }
        else if (condition == ImsCallForwardInfo.CDIV_CF_REASON_NOT_REACHABLE) {
            reason = CF_REASON_NOT_REACHABLE;
        }
        else if (condition == ImsCallForwardInfo.CDIV_CF_REASON_ALL) {
            reason = CF_REASON_ALL;
        }
        else if (condition == ImsCallForwardInfo.CDIV_CF_REASON_ALL_CONDITIONAL) {
            reason = CF_REASON_ALL_CONDITIONAL;
        }
        else if (condition == ImsCallForwardInfo.CDIV_CF_REASON_NOT_LOGGED_IN) {
            reason = CF_REASON_NOT_LOGGED_IN;
        }
        else {
            Log.e(this, "Invalid condition for queryCallForward.");
            return -1;
        }
        mCi.queryCallForwardStatus(reason,
                                   serviceClass,
                                   number,
                                   mHandler.obtainMessage(EVENT_QUERY_CF, id, 0, this));
        return id;
    }

    /**
     * Retrieves the configuration of the call waiting.
     */
    @Override
    public int queryCallWaiting() {
        enforceReadPhoneState("queryCallWaiting");
        if (isDisposed() || mCi == null) {
            dumpw("queryCallWaiting");
            return -1;
        }

        int id = getIdForRequest();
        if (id < 0) {
            Log.e(this, "Invalid request id for queryCallWaiting.");
            return -1;
        }
        mCi.queryCallWaiting(SERVICE_CLASS_NONE,
                             mHandler.obtainMessage(EVENT_QUERY_CW, id, 0, this));
        return id;
    }

    /**
     * Retrieves the default CLIR setting.
     */
    @Override
    public int queryClir() {
        enforceReadPhoneState("queryCLIR");
        if (isDisposed() || mCi == null) {
            dumpw("queryClir");
            return -1;
        }
        int id = getIdForRequest();
        if (id < 0) {
            Log.e(this, "Invalid request id for queryCLIR.");
            return -1;
        }
        mCi.getCLIR(mHandler.obtainMessage(EVENT_QUERY_CLIR, id, 0, this));
        return id;
    }

    /**
     * Retrieves the CLIP call setting.
     */
    @Override
    public int queryClip() {
        enforceReadPhoneState("queryCLIP");
        if (isDisposed() || mCi == null) {
            dumpw("queryClip");
            return -1;
        }
        int id = getIdForRequest();
        if (id < 0) {
            Log.e(this, "Invalid request id for queryCLIP.");
            return -1;
        }
        mCi.queryCLIP(mHandler.obtainMessage(EVENT_QUERY_CLIP, id, 0, this));
        return id;
    }

    /**
     * Retrieves the COLR call setting.
     */
    @Override
    public int queryColr() {
        enforceReadPhoneState("queryCOLR");
        if (isDisposed() || mCi == null) {
            dumpw("queryColr");
            return -1;
        }
        int id = getIdForRequest();
        if (id < 0) {
            Log.e(this, "Invalid request id for queryCOLR.");
            return -1;
        }
        mCi.getCOLR(mHandler.obtainMessage(EVENT_QUERY_COLR, id, 0, this));
        return id;
    }

    /**
     * Retrieves the COLP call setting.
     */
    @Override
    public int queryColp() {
        enforceReadPhoneState("queryCOLP");
        if (isDisposed() || mCi == null) {
            dumpw("queryColp");
            return -1;
        }
        int id = getIdForRequest();
        if (id < 0) {
            Log.e(this, "Invalid request id for queryCLIP.");
            return -1;
        }
        mCi.getSuppSvc("COLP", //TODO: String argument required. Use like this or define somewhere?
                          mHandler.obtainMessage(EVENT_QUERY_COLP, id, 0, this));
        return id;
    }

    /*
     * we have reused CF actions because CF and CB actions are used for
     * same purpose.However,We are updating CB actions here as per proto
     * file to be in sync with lower layer.
     */
    public static int getIcbAction(int action) {
        if(action == ImsPhoneCommandsInterface.CF_ACTION_DISABLE){
            return SuppSvcResponse.DEACTIVATE;
        } else if(action == ImsPhoneCommandsInterface.CF_ACTION_ENABLE) {
            return  SuppSvcResponse.ACTIVATE;
        } else if (action == ImsPhoneCommandsInterface.CF_ACTION_ERASURE) {
            return SuppSvcResponse.ERASURE;
        } else if(action == ImsPhoneCommandsInterface.CF_ACTION_REGISTRATION) {
            return SuppSvcResponse.REGISTER;
        }
        return ImsUtImplBase.INVALID_RESULT;
    }

    /**
     * Updates the configuration of the call barring.
     */
    @Override
    public int updateCallBarring(int cbType, int action, String[] barrList) {
        return updateCallBarringForServiceClass(cbType, action, barrList, SERVICE_CLASS_NONE);
    }

    /**
     * Updates the configuration of the call barring for specified service class.
     */
    @Override
    public int updateCallBarringForServiceClass(int cbType, int action,
            String[] barrList, int serviceClass) {
        return updateCallBarringWithPassword(cbType, action, barrList, serviceClass, "");
    }

    /**
     * Updates the configuration of the call barring for specified service class with password.
     */
    @Override
    public int updateCallBarringWithPassword(int cbType, int action,
            String[] barrList, int serviceClass, String password) {
        mContext.enforceCallingOrSelfPermission(MODIFY_PHONE_STATE, "updateCallBarring");
        if (isDisposed() || mCi == null) {
            dumpw("updateCallBarringWithPassword");
            return -1;
        }
        int id = getIdForRequest();
        if (id < 0) {
            Log.e(this, "Invalid request id for updateCallBarring.");
            // ImsUt.java treats ids < 0 as an error.
            return -1;
        }

        int facility = getFacilityFromCbType(cbType);
        if (facility == -1) {
            Log.e(this, "Unsupported call barring facility code in updateCallBarring.");
            return -1;
        }

        int cbAction = getIcbAction(action);
        // Check for ICB case.
        if (facility == SuppSvcResponse.FACILITY_BS_MT) {
            mCi.suppSvcStatus(cbAction,
                    facility,
                    barrList,
                    password,
                    SERVICE_CLASS_VOICE,
                    mHandler.obtainMessage(EVENT_UPDATE_CB, id, 0, this));
        } else {
            mCi.suppSvcStatus(cbAction,
                    facility, null, password, serviceClass,
                    mHandler.obtainMessage(EVENT_UPDATE_CB, id, 0, this));
        }

        return id;
    }

    /**
     * Updates the configuration of the call forward.
     */
    @Override
    public int updateCallForward(int action, int condition, String number, int serviceClass,
            int timeSeconds) {
        mContext.enforceCallingOrSelfPermission(MODIFY_PHONE_STATE, "updateCallForward");
        if (isDisposed() || mCi == null) {
            dumpw("updateCallForward");
            return -1;
        }
        int id = getIdForRequest();
        if (id < 0) {
            Log.e(this, "Invalid request id for updateCallForward.");
            // ImsUt.java treats ids < 0 as an error.
            return -1;
        }
        mCi.setCallForward(action,
                           condition,
                           serviceClass,
                           number,
                           timeSeconds,
                           mHandler.obtainMessage(EVENT_UPDATE_CF, id, 0, this));
        return id;
    }

    /**
     * Updates the configuration of the call forward Unconditional Timer.
     */
    public int updateCallForwardUncondTimer(int startHour, int startMinute, int endHour,
            int endMinute, int action, int condition, String number) {
        if (isDisposed() || mCi == null) {
            dumpw("updateCallForwardUncondTimer");
            return -1;
        }
        int id = getIdForRequest();
        if (id < 0) {
            Log.e(this, "Invalid request id for updateCallForwardUncondTimer.");
            // ImsUt.java treats ids < 0 as an error.
            return -1;
        }
        mCi.setCallForwardUncondTimer(startHour, startMinute, endHour, endMinute,
                           action, condition,
                           ImsSsData.SERVICE_CLASS_VOICE,
                           number,
                           mHandler.obtainMessage(EVENT_UPDATE_CF, id, 0, this));
        return id;
    }

    /**
     * Updates the configuration of the call waiting.
     */
    @Override
    public int updateCallWaiting(boolean enable, int serviceClass) {
        mContext.enforceCallingOrSelfPermission(MODIFY_PHONE_STATE, "updateCallWaiting");
        if (isDisposed() || mCi == null) {
            dumpw("updateCallWaiting");
            return -1;
        }
        int id = getIdForRequest();
        if (id < 0) {
            Log.e(this, "Invalid request id for updateCallForward.");
            return -1;
        }
        mCi.setCallWaiting(enable,
                           serviceClass,
                           mHandler.obtainMessage(EVENT_UPDATE_CW, id, 0, this));
        return id;
    }

    /**
     * Updates the configuration of the CLIR supplementary service.
     */
    @Override
    public int updateClir(int clirMode) {
        mContext.enforceCallingOrSelfPermission(MODIFY_PHONE_STATE, "updateCLIR");
        if (isDisposed() || mCi == null) {
            dumpw("updateClir");
            return -1;
        }
        int id = getIdForRequest();
        if (id < 0) {
            Log.e(this, "Invalid request id for updateCLIR.");
            return -1;
        }
        mCi.setCLIR(clirMode,
                    mHandler.obtainMessage(EVENT_UPDATE_CLIR, id, 0, this));
        return id;
    }

    /**
     * Updates the configuration of the CLIP supplementary service.
     */
    @Override
    public int updateClip(boolean enable) {
        mContext.enforceCallingOrSelfPermission(MODIFY_PHONE_STATE, "updateCLIP");
        if (isDisposed() || mCi == null) {
            dumpw("updateClip");
            return -1;
        }
        int id = getIdForRequest();
        if (id < 0) {
            Log.e(this, "Invalid request id for updateCLIP.");
            return -1;
        }
        mCi.setSuppSvc("CLIP",
                       enable,
                       mHandler.obtainMessage(EVENT_UPDATE_CLIP, id, 0, this));
        return id;
    }

    /**
     * Updates the configuration of the COLR supplementary service.
     */
    @Override
    public int updateColr(int presentation) {
        mContext.enforceCallingOrSelfPermission(MODIFY_PHONE_STATE, "updateCOLR");
        if (isDisposed() || mCi == null) {
            dumpw("updateColr");
            return -1;
        }
        int id = getIdForRequest();
        if (id < 0) {
            Log.e(this, "Invalid request id for updateCOLR.");
            return -1;
        }
        mCi.setCOLR(presentation,
                mHandler.obtainMessage(EVENT_UPDATE_COLR, id, 0, this));
        return id;
    }

    /**
     * Updates the configuration of the COLP supplementary service.
     */
    @Override
    public int updateColp(boolean enable) {
        mContext.enforceCallingOrSelfPermission(MODIFY_PHONE_STATE, "updateCOLP");
        if (isDisposed() || mCi == null) {
            dumpw("updateColp");
            return -1;
        }
        int id = getIdForRequest();
        if (id < 0) {
            Log.e(this, "Invalid request id for updateCOLP.");
            return -1;
        }
        mCi.setSuppSvc("COLP",
                       enable,
                       mHandler.obtainMessage(EVENT_UPDATE_COLP, id, 0, this));
        return id;
    }

    /**
     * Sets the listener.
     */
    @Override
    public void setListener(ImsUtListener listener) {
        Log.d(this, "setListener");
        mContext.enforceCallingOrSelfPermission(MODIFY_PHONE_STATE, "setListener");
        if (isDisposed()) {
            Log.w(this, "setListener - isDisposed");
            return;
        }
        mListenerImpl.mListener = listener;
    }

    /**
     * Method to get a request id for a request.
     * @return requestId
     */
    private int getIdForRequest() {
        // Note: This logic is in place to handle multiple UT requests at
        //       the same time. Currently, UI supports only one request.
        synchronized(mLock) {
            requestId++;
            if (requestId >= MAX_REQUESTS_PENDING) {
                requestId = 0;
            }
            return requestId;
        }
    }

    @VisibleForTesting
    public void setIdForRequest(int id) {
        synchronized(mLock) {
            requestId = id;
        }
    }

    /**
     * Method to parse the exception string within an AsyncResult exception
     * and return the correct IMS reason for failure.
     * @param ar The (AsyncResult) response message from lower layers.
     * @return The ImsReasonInfo with appropriate error code.
     */
    public static ImsReasonInfo getImsReasonInfoFromResponseError(AsyncResult ar) {
        if (ar == null) {
            Log.i("ImsUtImpl", "getImsReasonInfoFromResponseError :: Null AsyncResult!");
            return null;
        }
        if (ar.exception == null) {
            Log.i("ImsUtImpl", "getImsReasonInfoFromResponseError :: "
                    + "No exception in AsyncResult!");
            return null;
        }
        // ImsSenderRxr packs a RuntimeException with the AyncResult.
        // This is the only type we are handling currently.
        if (!(ar.exception instanceof RuntimeException)) {
            Log.i("ImsUtImpl", "getImsReasonInfoFromResponseError :: "
                    + "Improper exception type in AsyncResult!");
            return null;
        }

        int code = ImsReasonInfo.CODE_UNSPECIFIED;
        ImsRilException ex = (ImsRilException)ar.exception;
        String error = ex.getMessage();
        if (error == null) {
            Log.i("ImsUtImpl", "getImsReasonInfoFromResponseError :: "
                    + "Null message string in exception!");
            return new ImsReasonInfo(code, ImsReasonInfo.CODE_UNSPECIFIED, null);
        }

        code = toImsReasonInfoCode(ex.getErrorCode());

        Log.i(LOG_TAG, "getImsReasonInfoFromResponseError :: "
                + "ImsReasonInfo code=" + code);
        return new ImsReasonInfo(code, ImsReasonInfo.CODE_UNSPECIFIED, null);
    }

    /** Check the error code propagated with the exception.
    * Map it to the appropriate ImsReasonInfo code.
    */
    private static int toImsReasonInfoCode(int imsErrorCode) {
        switch(imsErrorCode) {
            case ImsErrorCode.GENERIC_FAILURE:
                return ImsReasonInfo.CODE_UNSPECIFIED;
            case ImsErrorCode.PASSWORD_INCORRECT:
                return ImsReasonInfo.CODE_UT_CB_PASSWORD_MISMATCH;
            case ImsErrorCode.NETWORK_NOT_SUPPORTED:
                return ImsReasonInfo.CODE_UT_SERVICE_UNAVAILABLE;
            case ImsErrorCode.FDN_CHECK_FAILURE:
                return ImsReasonInfo.CODE_FDN_BLOCKED;
            case ImsErrorCode.SS_MODIFIED_TO_DIAL:
                return ImsReasonInfo.CODE_UT_SS_MODIFIED_TO_DIAL;
            case ImsErrorCode.SS_MODIFIED_TO_USSD:
                return ImsReasonInfo.CODE_UT_SS_MODIFIED_TO_USSD;
            case ImsErrorCode.SS_MODIFIED_TO_SS:
                return ImsReasonInfo.CODE_UT_SS_MODIFIED_TO_SS;
            case ImsErrorCode.SS_MODIFIED_TO_DIAL_VIDEO:
                return ImsReasonInfo.CODE_UT_SS_MODIFIED_TO_DIAL_VIDEO;
            case ImsErrorCode.CF_SERVICE_NOT_REGISTERED:
                return QtiCallConstants.CODE_UT_CF_SERVICE_NOT_REGISTERED;
            default:
                Log.i("ImsUtImpl", "getImsReasonInfoFromResponseError :: "
                        + "Unrecognized exception message string!");
                return ImsReasonInfo.CODE_UNSPECIFIED;
        }
    }

    private void dumpw(String msgPrefix) {
        Log.w(this, msgPrefix + ": isDisposed= " + isDisposed());
        Log.w(this, msgPrefix + ": mCi= " + mCi);
        Log.w(this, msgPrefix + ": mListenerImpl= " + mListenerImpl);
    }

    //Handler for tracking requests sent to ImsSenderRxr.
    private class ImsUtImplHandler extends Handler {
        ImsUtImplHandler() {
            this(Looper.getMainLooper());
        }

        ImsUtImplHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.i(this, "Message received: what = " + msg.what);
            if (isDisposed() || mListenerImpl == null) {
                dumpw("handleMessage");
                return;
            }
            AsyncResult ar;

            switch (msg.what) {

                case EVENT_QUERY_CB:
                    ar = (AsyncResult) msg.obj;
                    if (ar != null) {
                        if (msg.arg1 < 0) {
                            Log.e(this, "Invalid message id received in handleMessage.");
                            return;
                        }

                        if (ar.exception != null) {
                            Log.e(this, "Query CB error");

                            if (ar.userObj != null) {
                                mListenerImpl.utConfigurationQueryFailed(msg.arg1,
                                        getImsReasonInfoFromResponseError(ar));
                            }
                        }
                        else if (ar.result != null) {
                            SuppSvcResponse response = (SuppSvcResponse) ar.result;
                            final ImsReasonInfo sipError = response.getErrorDetails();
                            if (sipError != null) {
                                Log.e(this, "SuppSvcResponse has failure for CB query.");
                                mListenerImpl.utConfigurationQueryFailed(msg.arg1,
                                    new ImsReasonInfo(ImsReasonInfo.CODE_UT_NETWORK_ERROR,
                                    sipError.getExtraCode(), sipError.getExtraMessage()));
                                return;
                            } else if (response.getFailureCause().length() > 0) {
                                ImsReasonInfo error = new ImsReasonInfo(ImsReasonInfo.
                                        CODE_UT_NETWORK_ERROR, ImsReasonInfo.CODE_UNSPECIFIED,
                                        response.getFailureCause());
                                Log.e(LOG_TAG, "CB query failed with error = " + error);
                                mListenerImpl.utConfigurationQueryFailed(msg.arg1, error);
                                return;
                            }
                            if (response.getStatus() == SuppSvcResponse.INVALID) {
                                Log.e(this, "No service status info in response for CB query.");
                                mListenerImpl.utConfigurationQueryFailed(msg.arg1,
                                        new ImsReasonInfo(ImsReasonInfo.CODE_UT_NETWORK_ERROR,
                                        ImsReasonInfo.CODE_UNSPECIFIED, null));
                            }
                            else {
                                if(!response.getBarredLines().isEmpty()) {
                                    List<ImsSsInfo> ssInfoArray = new ArrayList<>();
                                    List<SuppSvcResponse.BarredLines> barredLines =
                                            response.getBarredLines();
                                    for (SuppSvcResponse.BarredLines lines : barredLines) {
                                        if (!barredLines.isEmpty()) {
                                            List<SuppSvcResponse.LineStatus> line =
                                                    lines.getLines();
                                            for (SuppSvcResponse.LineStatus lineStatus : line) {
                                                ImsSsInfo.Builder ssInfoBuilder =
                                                        new ImsSsInfo.Builder(
                                                        lineStatus.getStatus());
                                                ssInfoBuilder.setIncomingCommunicationBarringNumber(
                                                        lineStatus.getNumber());
                                                ssInfoArray.add(ssInfoBuilder.build());
                                            }
                                        }
                                    }
                                    mListenerImpl.utConfigurationCallBarringQueried(
                                            msg.arg1, ssInfoArray.toArray(
                                            new ImsSsInfo[ssInfoArray.size()]));
                                } else {
                                    ImsSsInfo[] ssInfoStatus = new ImsSsInfo[1];
                                    int status = ImsSsInfo.DISABLED;
                                    if (response.getStatus() == ImsSsInfo.ENABLED) {
                                        status = ImsSsInfo.ENABLED;
                                    }
                                    ImsSsInfo.Builder imsSsInfoBuilder =
                                            new ImsSsInfo.Builder(status);
                                    ImsSsInfo ssInfo = imsSsInfoBuilder.build();
                                    ssInfoStatus[0] = ssInfo;
                                    Log.i(this, "success callback Query Anonymous CB, status= "
                                            + ssInfo.getStatus());
                                    mListenerImpl.utConfigurationCallBarringQueried(msg.arg1,
                                            ssInfoStatus);
                                }
                            }
                        }
                        else {
                            Log.e(this, "Null response received for Query CB!");
                            mListenerImpl.utConfigurationQueryFailed(msg.arg1, new ImsReasonInfo(
                                    ImsReasonInfo.CODE_UT_NETWORK_ERROR,
                                    ImsReasonInfo.CODE_UNSPECIFIED, null));
                        }
                    }
                    break;

                case EVENT_UPDATE_CB:
                    ar = (AsyncResult) msg.obj;
                    if (ar != null) {
                        if (msg.arg1 < 0) {
                            Log.e(this, "Invalid message id received in handleMessage.");
                            return;
                        }

                        if (ar.exception != null) {
                            Log.e(this, "Update CB error");
                            if (ar.result != null) {
                               SuppSvcResponse response = (SuppSvcResponse) ar.result;
                               final ImsReasonInfo sipError = response.getErrorDetails();
                               if (sipError != null) {
                                   Log.e(this, "SuppSvcResponse has failure for CB update.");
                                   mListenerImpl.utConfigurationUpdateFailed(msg.arg1,
                                           new ImsReasonInfo(ImsReasonInfo.CODE_UT_NETWORK_ERROR,
                                           sipError.getExtraCode(), sipError.getExtraMessage()));
                                   return;
                                } else if (response.getFailureCause().length() > 0) {
                                    ImsReasonInfo error = new ImsReasonInfo(ImsReasonInfo.
                                            CODE_UT_NETWORK_ERROR, ImsReasonInfo.CODE_UNSPECIFIED,
                                            response.getFailureCause());
                                    Log.e(LOG_TAG, "CB update failed with error = " + error);
                                    mListenerImpl.utConfigurationUpdateFailed(msg.arg1,
                                            error);
                                    return;
                                } else {
                                    Log.e(LOG_TAG, "SuppSvcResponse failure with neither"
                                            + " errordetails nor failurecause");
                                    mListenerImpl.utConfigurationUpdateFailed(msg.arg1,
                                            getImsReasonInfoFromResponseError(ar));
                                }
                            } else {
                                mListenerImpl.utConfigurationUpdateFailed(msg.arg1,
                                        getImsReasonInfoFromResponseError(ar));
                            }
                        }
                        else {
                            // Null response from RIL is a valid success scenario here.
                            mListenerImpl.utConfigurationUpdated(msg.arg1);
                        }
                    }
                    break;

                case EVENT_UPDATE_CW:
                    ar = (AsyncResult) msg.obj;
                    if (ar != null) {
                        if (msg.arg1 < 0) {
                            Log.e(this, "Invalid message id received in handleMessage.");
                            return;
                        }
                        if (ar.exception != null) {
                            Log.e(this, "Update CW error");

                            if (ar.result != null) {
                                // Update CW response has failure cause information.
                                // Check for it to determine request's success of failure.
                                SuppSvcResponse response
                                    = (SuppSvcResponse) ar.result;
                                final ImsReasonInfo sipError = response.getErrorDetails();
                                if (sipError != null) {
                                    Log.d(this, "SuppSvcResponse has failure for msg.what= "
                                            + msg.what);
                                    mListenerImpl.utConfigurationUpdateFailed(msg.arg1,
                                             new ImsReasonInfo(ImsReasonInfo.CODE_UT_NETWORK_ERROR,
                                             sipError.getExtraCode(), sipError.getExtraMessage()));
                                } else if (response.getFailureCause().length() > 0) {
                                    ImsReasonInfo error = new ImsReasonInfo(
                                            ImsReasonInfo.CODE_UT_NETWORK_ERROR,
                                            ImsReasonInfo.CODE_UNSPECIFIED,
                                            response.getFailureCause());
                                    Log.e(LOG_TAG, "SuppSvc " + msg.what + " failed with error = "
                                            + error);
                                    mListenerImpl.utConfigurationUpdateFailed(msg.arg1,
                                            error);
                                } else {
                                    Log.e(LOG_TAG, "SuppSvcResponse failure with neither"
                                            + " errordetails nor failurecause");
                                    mListenerImpl.utConfigurationUpdateFailed(msg.arg1,
                                            getImsReasonInfoFromResponseError(ar));
                                }
                            }
                            else if (ar.userObj != null) {
                                mListenerImpl.utConfigurationUpdateFailed(msg.arg1,
                                        getImsReasonInfoFromResponseError(ar));
                            } else {
                                // Nothing to pass to frameworks for this request's response.
                                Log.e(LOG_TAG, "SuppSvcResponse failure with neither ar.result"
                                         + " nor userObj");
                                mListenerImpl.utConfigurationUpdateFailed(msg.arg1,
                                        getImsReasonInfoFromResponseError(ar));
                            }
                        }
                        else {
                            Log.i(this, "Success callback called for msg.what= "
                                    + msg.what);
                            mListenerImpl.utConfigurationUpdated(msg.arg1);
                        }
                    }
                    break;

                case EVENT_UPDATE_CF:
                    ar = (AsyncResult) msg.obj;
                    if (ar == null || msg.arg1 < 0) {
                        Log.e(this, "Invalid response: ar = " +  ar + " msgId = " + msg.arg1);
                        return;
                    }

                    if (ar.exception != null) {
                        Log.e(this, "Update CF error");

                        CallForwardStatusInfo cfStatusInfo = (CallForwardStatusInfo) ar.result;
                        final ImsReasonInfo sipError = cfStatusInfo != null ?
                                cfStatusInfo.getSipErrorInfo() : null;
                        String failCause = sipError != null ? sipError.getExtraMessage() : null;
                        if (failCause != null && !(failCause.isEmpty())) {
                            // Update CF response has failure cause information.
                            mListenerImpl.utConfigurationUpdateFailed(msg.arg1,
                                    new ImsReasonInfo(ImsReasonInfo.CODE_UT_NETWORK_ERROR,
                                    sipError.getExtraCode(), failCause));
                        } else {
                            Log.e(LOG_TAG, "Update CF failure: ar = " + ar + " sipError: " +
                                    sipError);
                            mListenerImpl.utConfigurationUpdateFailed(msg.arg1,
                                    getImsReasonInfoFromResponseError(ar));
                        }
                    } else {
                        Log.i(this, "Success callback called for msg.what= " + msg.what);
                        mListenerImpl.utConfigurationUpdated(msg.arg1);
                    }
                    break;

                case EVENT_QUERY_CF:
                    ar = (AsyncResult) msg.obj;
                    if (ar != null) {
                        if (msg.arg1 < 0) {
                            Log.e(this, "Invalid message id received in handleMessage.");
                            return;
                        }
                        if (ar.exception != null) {
                            Log.e(this, "Query CF error");
                            if (ar.userObj != null) {
                                mListenerImpl.utConfigurationQueryFailed(msg.arg1,
                                        getImsReasonInfoFromResponseError(ar));
                            }
                        }
                        else if (ar.result != null) {
                            ImsCallForwardTimerInfo cfInfoList[] =
                                    (ImsCallForwardTimerInfo[]) ar.result;

                            if (cfInfoList.length < 1) {
                                Log.e(this, "CallForwardInfo list has no elements!");
                                mListenerImpl.utConfigurationQueryFailed(msg.arg1,
                                        new ImsReasonInfo(ImsReasonInfo.CODE_UT_NETWORK_ERROR,
                                        ImsReasonInfo.CODE_UNSPECIFIED, null));
                                return;
                            }

                            ImsCallForwardInfo[] callForwardInfoList =
                                    toImsCallForwardInfo(cfInfoList);

                            if (callForwardInfoList == null) {
                                mListenerImpl.utConfigurationQueryFailed(
                                        msg.arg1, new ImsReasonInfo(ImsReasonInfo.
                                        CODE_UT_NETWORK_ERROR, ImsReasonInfo.CODE_UNSPECIFIED,
                                        null));
                                return;
                            }

                            mListenerImpl.utConfigurationCallForwardQueried(
                                    msg.arg1,
                                    callForwardInfoList);
                        }
                        else {
                            Log.e(this, "Null response received for Query CF!");
                            mListenerImpl.utConfigurationQueryFailed(
                                    msg.arg1, new ImsReasonInfo(ImsReasonInfo.CODE_UT_NETWORK_ERROR,
                                    ImsReasonInfo.CODE_UNSPECIFIED, null));
                        }
                    }
                    break;

                case EVENT_QUERY_CW:
                    ar = (AsyncResult) msg.obj;
                    if (ar != null) {
                        if (msg.arg1 < 0) {
                            Log.e(this, "Invalid message id received in handleMessage.");
                            return;
                        }
                        if (ar.exception != null) {
                            Log.e(this, "Query CW error");
                            if (ar.userObj != null) {
                                mListenerImpl.utConfigurationQueryFailed(
                                        msg.arg1,
                                        getImsReasonInfoFromResponseError(ar));
                            }
                        }
                        else if (ar.result != null) {
                            int[] cwResponse = (int[]) ar.result;

                            ImsSsInfo[] callWaitingInfoList = new ImsSsInfo[1];

                            int status = ImsSsInfo.DISABLED;
                            if (cwResponse[0] == SuppSvcResponse.ENABLED) {
                                if ((cwResponse[1] & SERVICE_CLASS_VOICE) == SERVICE_CLASS_VOICE) {
                                    status = ImsSsInfo.ENABLED;
                                } else {
                                    status = ImsSsInfo.DISABLED;
                                }
                            }
                            else if (cwResponse[0] == SuppSvcResponse.DISABLED) {
                                status = ImsSsInfo.DISABLED;
                            }
                            else {
                                Log.e(this, "No service status received for CallWaitingInfo.");
                                mListenerImpl.utConfigurationQueryFailed(
                                        msg.arg1, new ImsReasonInfo(ImsReasonInfo.
                                        CODE_UT_NETWORK_ERROR, ImsReasonInfo.CODE_UNSPECIFIED,
                                        null));
                                return;
                            }

                            //NOTE: Service status is VOICE by default, hence not checked.
                            ImsSsInfo.Builder imsSsInfoBuilder = new ImsSsInfo.Builder(status);
                            callWaitingInfoList[0] = imsSsInfoBuilder.build();

                            mListenerImpl.utConfigurationCallWaitingQueried(
                                    msg.arg1,
                                    callWaitingInfoList);
                        }
                        else {
                            Log.e(this, "Null response received for Query CW!");
                            mListenerImpl.utConfigurationQueryFailed(
                                    msg.arg1, new ImsReasonInfo(ImsReasonInfo.CODE_UT_NETWORK_ERROR,
                                    ImsReasonInfo.CODE_UNSPECIFIED, null));
                        }
                    }
                    break;

                case EVENT_QUERY_CLIR:
                    ar = (AsyncResult) msg.obj;
                    if (ar != null) {
                        if (msg.arg1 < 0) {
                            Log.e(this, "Invalid message id received in handleMessage.");
                            return;
                        }
                        if (ar.exception != null) {
                            if (msg.what == EVENT_QUERY_CLIR) {
                                Log.e(this, "Query CLIR error");
                            }

                            if (ar.userObj != null) {
                                mListenerImpl.utConfigurationQueryFailed(
                                        msg.arg1,
                                        getImsReasonInfoFromResponseError(ar));
                            }
                        }
                        else if (ar.result != null) {
                            int[] clirResp = (int[]) ar.result;
                            if (clirResp != null && clirResp.length ==
                                    ImsCallUtils.CLIR_RESPONSE_LENGTH) {
                                ImsSsInfo info = new ImsSsInfo.Builder(ImsSsInfo.NOT_REGISTERED)
                                        .setClirOutgoingState(clirResp[0])
                                        .setClirInterrogationStatus(clirResp[1]).build();
                                Log.i(this, "Calling success callback for Query CLIR.");
                                mListenerImpl.lineIdentificationSupplementaryServiceResponse(
                                        msg.arg1, info);
                            } else {
                                Log.e(this, "Received invalid response for Query CLIR.");
                            }
                        }
                    }
                    break;

                case EVENT_QUERY_CLIP:
                    ar = (AsyncResult) msg.obj;
                    SuppService clipStatus = (SuppService) ar.result;
                    if (ar != null) {
                        if (msg.arg1 < 0) {
                            Log.e(this, "Invalid message id received in handleMessage.");
                            return;
                        }
                        if (ar.exception != null) {
                            Log.e(this, "Error for Query Event= " + msg.what);
                            if (ar.userObj != null) {
                                ImsReasonInfo sipError = clipStatus.getErrorDetails();
                                if (sipError != null) {
                                    mListenerImpl.utConfigurationQueryFailed(
                                            msg.arg1, new ImsReasonInfo(
                                                          ImsReasonInfo.CODE_UT_NETWORK_ERROR,
                                                          sipError.getExtraCode(),
                                                          sipError.getExtraMessage()));
                                } else {
                                    mListenerImpl.utConfigurationQueryFailed(
                                            msg.arg1,
                                            getImsReasonInfoFromResponseError(ar));
                                }
                            }
                        } else if (clipStatus != null) {
                            ImsSsInfo.Builder imsSsInfoBuilder =
                                    new ImsSsInfo.Builder(clipStatus.getStatus());
                            Log.d(this, "Success callback on Query event= " + msg.what);
                            mListenerImpl.lineIdentificationSupplementaryServiceResponse(
                                    msg.arg1, imsSsInfoBuilder.build());
                        }
                    }
                    break;

                case EVENT_QUERY_COLR:
                    ar = (AsyncResult) msg.obj;
                    SuppService colr = (SuppService) ar.result;
                    if (ar != null) {
                        if (msg.arg1 < 0) {
                            Log.e(this, "Invalid message id received in handleMessage.");
                            return;
                        }
                        if (ar.exception != null) {
                            Log.e(this, "Error for Query Event= " + msg.what);

                            if (ar.userObj != null) {
                                ImsReasonInfo sipError = colr.getErrorDetails();
                                if (sipError != null) {
                                    mListenerImpl.utConfigurationQueryFailed(
                                             msg.arg1, new ImsReasonInfo(
                                                          ImsReasonInfo.CODE_UT_NETWORK_ERROR,
                                                          sipError.getExtraCode(),
                                                          sipError.getExtraMessage()));
                                } else {
                                    mListenerImpl.utConfigurationQueryFailed(
                                            msg.arg1,
                                            getImsReasonInfoFromResponseError(ar));
                                }
                            }
                        } else if (colr != null) {
                            ImsSsInfo.Builder ssInfoBuilder =
                                    new ImsSsInfo.Builder(colr.getStatus());
                            ImsSsInfo ssInfo = ssInfoBuilder.build();
                            Log.i(this, "Service= " + msg.what + " status= "
                                + ssInfo.getStatus());

                            ssInfoBuilder.setProvisionStatus(colr.getProvisionStatus());
                            Log.i(this, "Service= " + msg.what + " Provision Status= "
                                + ssInfo.getProvisionStatus());

                            Log.d(this, "Success callback on Query event= " + msg.what);
                            mListenerImpl.lineIdentificationSupplementaryServiceResponse(
                                    msg.arg1, ssInfo);
                        }
                    }
                    break;

                case EVENT_QUERY_COLP:
                    ar = (AsyncResult) msg.obj;
                    if (ar != null) {
                        if (msg.arg1 < 0) {
                            Log.e(this, "Invalid message id received in handleMessage.");
                            return;
                        }
                        if (ar.exception != null) {
                            Log.e(this, "Query COLP error");

                            if (ar.userObj != null) {
                                mListenerImpl.utConfigurationQueryFailed(
                                        msg.arg1,
                                        getImsReasonInfoFromResponseError(ar));
                            }
                        }
                        else if (ar.result != null) {
                            // COLP response has failure cause information.
                            // Check for it to determiqne request's success of failure.
                            SuppSvcResponse response = (SuppSvcResponse) ar.result;
                            final ImsReasonInfo sipError = response.getErrorDetails();
                            if(sipError != null) {
                                Log.e(this, "SuppSvcResponse has failure for COLP query.");
                                mListenerImpl.utConfigurationQueryFailed(msg.arg1,
                                        new ImsReasonInfo(ImsReasonInfo.CODE_UT_NETWORK_ERROR,
                                        sipError.getExtraCode(), sipError.getExtraMessage()));
                            } else if (response.getFailureCause().length() > 0) {
                                ImsReasonInfo error = new ImsReasonInfo(ImsReasonInfo.
                                        CODE_UT_NETWORK_ERROR, ImsReasonInfo.CODE_UNSPECIFIED,
                                        response.getFailureCause());
                                Log.e(LOG_TAG, "COLP query failed with error = " + error);
                                mListenerImpl.utConfigurationQueryFailed(
                                        msg.arg1,
                                        error);
                            }
                            else {
                                response = (SuppSvcResponse) ar.result;
                                ImsSsInfo.Builder ssInfoBuilder =
                                        new ImsSsInfo.Builder(response.getStatus());
                                ImsSsInfo ssInfo = ssInfoBuilder.build();
                                ssInfoBuilder.setProvisionStatus(response.getProvisionStatus());
                                Log.i(this, "Service= " + msg.what + " provision Status= "
                                        + ssInfo.getProvisionStatus() + " status = "
                                        + ssInfo.getStatus());

                                Log.i(this, "Success callback called for Query COLP.");
                                mListenerImpl.lineIdentificationSupplementaryServiceResponse(
                                        msg.arg1, ssInfo);
                            }
                        }
                    }
                    break;

                case EVENT_UPDATE_CLIR:
                case EVENT_UPDATE_CLIP:
                case EVENT_UPDATE_COLR:
                case EVENT_UPDATE_COLP:

                    ar = (AsyncResult) msg.obj;
                    if (ar != null) {
                        if (msg.arg1 < 0) {
                            Log.e(this, "Invalid message id received in handleMessage.");
                            return;
                        }
                        if (ar.exception != null) {
                            if (msg.what == EVENT_UPDATE_CLIR) {
                                Log.e(this, "Update CLIR error");
                            }
                            else if (msg.what == EVENT_UPDATE_CLIP) {
                                Log.e(this, "Update CLIP error");
                            }
                            else if (msg.what == EVENT_UPDATE_COLR) {
                                Log.e(this, "Update COLR error");
                            }
                            else if (msg.what == EVENT_UPDATE_COLP) {
                                Log.e(this, "Update COLP error");
                            }
                            if (ar.result != null) {
                                // CLIP and COLP query responses have failure cause information.
                                // Check for it to determine request's success of failure.
                                SuppSvcResponse response =
                                        (SuppSvcResponse) ar.result;
                                final ImsReasonInfo sipError = response.getErrorDetails();
                                if (sipError != null) {
                                    Log.e(this,
                                            "SuppSvcResponse has failure for CLIP/COLP update");
                                    mListenerImpl.utConfigurationUpdateFailed(
                                            msg.arg1,
                                            new ImsReasonInfo(ImsReasonInfo.CODE_UT_NETWORK_ERROR,
                                            sipError.getExtraCode(), sipError.getExtraMessage()));
                                } else if (!response.getFailureCause().isEmpty()) {
                                    ImsReasonInfo error = new ImsReasonInfo(ImsReasonInfo.
                                            CODE_UT_NETWORK_ERROR, ImsReasonInfo.CODE_UNSPECIFIED,
                                            response.getFailureCause());
                                    Log.e(LOG_TAG, "SuppSvc " + msg.what + " failed, error: "
                                            + error);
                                    mListenerImpl.utConfigurationUpdateFailed(
                                             msg.arg1, error);
                                } else {
                                    Log.e(LOG_TAG, "SuppSvcResponse failure with neither"
                                            + " errordetails nor failurecause");
                                    mListenerImpl.utConfigurationUpdateFailed(
                                            msg.arg1,
                                            getImsReasonInfoFromResponseError(ar));
                                }
                            } else if (ar.userObj != null) {
                                    Log.e(LOG_TAG, "SuppSvcResponse failure with valid userObj");
                                    mListenerImpl.utConfigurationUpdateFailed(
                                            msg.arg1,
                                            getImsReasonInfoFromResponseError(ar));
                            } else {
                                // Nothing to pass to frameworks for this request's response.
                                Log.e(LOG_TAG, "SuppSvcResponse failure with neither ar.result"
                                         + " nor userObj");
                                mListenerImpl.utConfigurationUpdateFailed(
                                        msg.arg1,
                                        getImsReasonInfoFromResponseError(ar));
                            }
                        } else {
                            Log.i(this, "Success callback called for msg.what= "
                                    + msg.what);
                            mListenerImpl.utConfigurationUpdated(msg.arg1);
                        }
                    }
                    break;
                 case EVENT_UNSOL_ON_SS:
                    ar = (AsyncResult) msg.obj;
                    if (ar.exception == null) {
                        ImsSsData ssData = (ImsSsData) ar.result;
                        mListenerImpl.onSupplementaryServiceIndication(ssData);
                    } else {
                        Log.e(this, "exception in handling UNSOL_ON_SS");
                    }
                    break;
            }

        }

    }
}
