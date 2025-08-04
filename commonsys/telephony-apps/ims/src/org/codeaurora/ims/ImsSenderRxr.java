/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 * Not a Contribution.
 * Apache license notifications and license are retained
 * for attribution purposes only.
 *
 * Copyright (C) 2006 The Android Open Source Project
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
 *
 */

package org.codeaurora.ims;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.location.Address;
import android.os.Handler;
import android.os.HwBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.PowerManager.WakeLock;
import android.sysprop.TelephonyProperties;
import android.telephony.ims.ImsManager;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.ImsSsData;
import android.telephony.ims.ImsStreamMediaProfile;
import android.telephony.SubscriptionManager;

import androidx.annotation.VisibleForTesting;

import com.qualcomm.ims.utils.Log;
import org.codeaurora.telephony.utils.AsyncResult;
import org.codeaurora.telephony.utils.Registrant;
import org.codeaurora.telephony.utils.RegistrantList;
import org.codeaurora.telephony.utils.SomeArgs;
import org.codeaurora.ims.sms.SmsResponse;
import org.codeaurora.ims.sms.StatusReport;
import org.codeaurora.ims.sms.IncomingSms;
import org.codeaurora.ims.CallComposerInfo;
import org.codeaurora.ims.VosActionInfo;

import java.lang.ClassCastException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

import org.codeaurora.ims.QtiCallConstants;
import org.codeaurora.ims.IQtiRadioConfig;
import org.codeaurora.ims.utils.CallComposerInfoUtils;
import org.codeaurora.ims.utils.QtiImsExtUtils;
/**
 * {@hide}
 */
class IFRequest {

    // ***** Class Variables
    static int sNextSerial = 0;
    static Object sSerialMonitor = new Object();
    private static Object sPoolSync = new Object();
    private static IFRequest sPool = null;
    private static int sPoolSize = 0;
    private static final int MAX_POOL_SIZE = 4;

    // ***** Instance Variables
    int mSerial;
    int mRequest;
    Message mResult;
    // FIXME delete parcel
    // Parcel mp;
    IFRequest mNext;
    byte[] mData;

    /**
     * Retrieves a new IFRequest instance from the pool.
     *
     * @param request MessageId.REQUEST_*
     * @param result sent when operation completes
     * @return a IFRequest instance from the pool.
     */
    static IFRequest obtain(int request, Message result) {
        IFRequest rr = null;

        synchronized (sPoolSync) {
            if (sPool != null) {
                rr = sPool;
                sPool = rr.mNext;
                rr.mNext = null;
                sPoolSize--;
            }
        }

        if (rr == null) {
            rr = new IFRequest();
        }

        synchronized (sSerialMonitor) {
            rr.mSerial = sNextSerial++;
        }
        rr.mRequest = request;
        rr.mResult = result;

        if (result != null && result.getTarget() == null) {
            throw new NullPointerException("Message target must not be null");
        }

        return rr;
    }

    void setResult(Message newMsg) {
        mResult = newMsg;
    }

    /**
     * Returns a IFRequest instance to the pool. Note: This should only be
     * called once per use.
     */
    void release() {
        synchronized (sPoolSync) {
            if (sPoolSize < MAX_POOL_SIZE) {
                this.mNext = sPool;
                sPool = this;
                sPoolSize++;
                mResult = null;
            }
        }
    }

    private IFRequest() {
    }

    static void resetSerial() {
        synchronized (sSerialMonitor) {
            sNextSerial = 0;
        }
    }

    String serialString() {
        // Cheesy way to do %04d
        StringBuilder sb = new StringBuilder(8);
        String sn;

        sn = Integer.toString(mSerial);

        // sb.append("J[");
        sb.append('[');
        for (int i = 0, s = sn.length(); i < 4 - s; i++) {
            sb.append('0');
        }

        sb.append(sn);
        sb.append(']');
        return sb.toString();
    }

    void onError(int error, Object ret) {
        RuntimeException ex;
        String errorMsg;

        if (error == ImsErrorCode.SUCCESS) {
            ex = null;
        } else {
            errorMsg = ImsSenderRxr.errorIdToString(error);
            ex = new ImsRilException(error, errorMsg);
        }

        Log.i(this, serialString() + "< "
                + ImsSenderRxr.msgIdToString(mRequest)
                + " error: " + error);

        if (mResult != null && mResult.getTarget() != null) {
            AsyncResult.forMessage(mResult, ret, ex);
            mResult.sendToTarget();
        }
    }
}

/**
 * IMS implementation of the CommandsInterface. {@hide}
 */
public class ImsSenderRxr extends ImsPhoneBaseCommands {
    static final String LOG_TAG = "ImsSenderRxr";

    /**
     * Wake lock timeout should be longer than the longest timeout in the vendor
     */
    private static final int DEFAULT_WAKE_LOCK_TIMEOUT = 60000;
    private static final int PDU_LENGTH_OFFSET = 4;
    private static final int MSG_TAG_LENGTH = 1;

    // ***** Events
    static final int EVENT_WAKE_LOCK_TIMEOUT = 1;
    private static final int EVENT_QTI_RADIO_CONFIG_SERVICE_UP = 2;

    // ***** Instance Variables
    WakeLock mWakeLock;
    int mWakeLockTimeout;

    final ImsRadioHandler mImsRadioHandler;

    // The number of requests sent out but waiting for response. It increases
    // while
    // sending request and decreases while handling response. It should match
    // mRequestList.size() unless there are requests no replied while
    // WAKE_LOCK_TIMEOUT occurs.
    int mRequestMessagesWaiting;

    /* Variable caching the Phone ID */
    private Integer mPhoneId;

    /* Variable caching the presence of UNSOL call list indication */
    private boolean mIsUnsolCallListPresent = false;

    private boolean mWfcRoamingConfigurationSupport = false;

    /*
       This is a static configuration to check if RIL supports the AIDL reordering feature.
       RIL is queried when QtiRadioConfig service comes up. Given the current requirement
       that static features supported in RIL will not change when it dies and comes back up,
       this flag will not be cleared when onServiceDown is called.
    */
    private boolean mIsAidlReorderingSupported = false;

    private boolean mIsCrbtSupported = false;

    // I'd rather this be LinkedList or something
    ArrayList<IFRequest> mRequestsList = new ArrayList<IFRequest>();

    // When we are testing emergency calls
    AtomicBoolean mTestingEmergencyCall = new AtomicBoolean(false);

    // ***** Constants
    static final int ZERO_SECONDS = 0;

    // Names of the Ims Radio services appended with slot id (for multiple sims)
    static final String[] IIMS_RADIO_SERVICE_NAME = {"imsradio0", "imsradio1", "imsradio2"};

    private Registrant mIncomingSmsRegistrant;
    private Registrant mSendSmsStatusReportRegistrant;
    private RegistrantList mHandoverStatusRegistrants;
    private RegistrantList mRefreshConfInfoRegistrations;
    private RegistrantList mSrvStatusRegistrations;
    private RegistrantList mTtyStatusRegistrants;
    private RegistrantList mRadioStateRegistrations;
    private RegistrantList mGeolocationRegistrants;
    private RegistrantList mVoWiFiCallQualityRegistrants;
    private RegistrantList mSsaccRegistrants;
    private RegistrantList mVopsRegistrants;
    private RegistrantList mParticipantStatusRegistrants;
    private RegistrantList mImsSubConfigChangeRegistrants;
    private RegistrantList mRegistrationBlockStatusRegistrants;
    protected RegistrantList mModifyCallRegistrants;
    protected RegistrantList mMwiRegistrants;
    private RegistrantList mRttMessageRegistrants;
    private RegistrantList mRttModifyRegistrants;
    protected Registrant mSsnRegistrant;
    protected Registrant mSsIndicationRegistrant;
    private RegistrantList mAutoRejectRegistrants;
    private RegistrantList mVoiceInfoStatusRegistrants;
    private RegistrantList mMultiIdentityStatusChangeRegistrants;
    private RegistrantList mMultiIdentityInfoPendingRegistrants;
    private RegistrantList mWfcRoamingModeConfigRegistrants;
    private RegistrantList mUssdInfoRegistrants;
    private RegistrantList mGeoLocationDataStatusRegistrants;
    private RegistrantList mSipDtmfInfoRegistrants;
    private RegistrantList mSrvDomainChangedRegistrants;
    private RegistrantList mConferenceCallStateCompletedRegistrants;
    private RegistrantList mSmsCallbackModeChangedRegistrants;
    private RegistrantList mIncomingDtmfStartRegistrants;
    private RegistrantList mIncomingDtmfStopRegistrants;
    private RegistrantList mMultiSimVoiceCapabilityChangedRegistrants;
    private RegistrantList mCiWlanNotificationRegistrants;
    private RegistrantList mSrtpEncryptionUpdateRegistrants;
    private RegistrantList mImsServiceUpRegistrants;
    private RegistrantList mImsServiceDownRegistrants;

    private ImsRadioResponse mImsResponse = new ImsRadioResponse();
    private ImsRadioIndication mImsIndication = new ImsRadioIndication();
    private IImsRadio mImsRadioHal;
    private IQtiRadioConfig mQtiRadioConfigHal;
    private IQtiRadioConfigIndication mQtiRadioConfigIndication = new QtiRadioConfigIndication();

    public void dispose() {
        clearRegistrants();
        if (mImsRadioHal == null) {
            return;
        }
        mImsRadioHal.dispose();
    }

    private void clearRegistrants() {
        unSetIncomingImsSms(null);
        unSetImsSmsStatusReport(null);
        unSetOnSuppServiceNotification(null);
        unregisterForSuppServiceIndication(null);
        mHandoverStatusRegistrants.clear();
        mRefreshConfInfoRegistrations.clear();
        mSrvStatusRegistrations.clear();
        mTtyStatusRegistrants.clear();
        mRadioStateRegistrations.clear();
        mGeolocationRegistrants.clear();
        mVoWiFiCallQualityRegistrants.clear();
        mSsaccRegistrants.clear();
        mVopsRegistrants.clear();
        mParticipantStatusRegistrants.clear();
        mImsSubConfigChangeRegistrants.clear();
        mRegistrationBlockStatusRegistrants.clear();
        mModifyCallRegistrants.clear();
        mMwiRegistrants.clear();
        mRttMessageRegistrants.clear();
        mRttModifyRegistrants.clear();
        mAutoRejectRegistrants.clear();
        mVoiceInfoStatusRegistrants.clear();
        mMultiIdentityStatusChangeRegistrants.clear();
        mMultiIdentityInfoPendingRegistrants.clear();
        mWfcRoamingModeConfigRegistrants.clear();
        mUssdInfoRegistrants.clear();
        mGeoLocationDataStatusRegistrants.clear();
        mSipDtmfInfoRegistrants.clear();
        mSrvDomainChangedRegistrants.clear();
        mConferenceCallStateCompletedRegistrants.clear();
        mSmsCallbackModeChangedRegistrants.clear();
        mIncomingDtmfStartRegistrants.clear();
        mIncomingDtmfStopRegistrants.clear();
        mMultiSimVoiceCapabilityChangedRegistrants.clear();
        mCiWlanNotificationRegistrants.clear();
        mSrtpEncryptionUpdateRegistrants.clear();
        mImsServiceUpRegistrants.clear();
        mImsServiceDownRegistrants.clear();
    }

    private void notifyServiceUp(String instanceName) {
        Log.i(this, "notifyServiceUp : " + instanceName);
        mImsServiceUpRegistrants.notifyRegistrants(
                new AsyncResult(null, mPhoneId, null));
    }

    private void notifyServiceDown(String instanceName) {
        Log.i(this, "notifyServiceDown : " + instanceName);
        mImsServiceDownRegistrants.notifyRegistrants(
                new AsyncResult(null, mPhoneId, null));
    }

    // send message to handle EVENT_QTI_RADIO_CONFIG_SERVICE_UP on the main thread
    private void handleQtiRadioConfigUp() {
        Message msg = mImsRadioHandler.obtainMessage(EVENT_QTI_RADIO_CONFIG_SERVICE_UP);
        mImsRadioHandler.sendMessage(msg);
    }

    public boolean isAidlReorderingSupported() {
        return mIsAidlReorderingSupported;
    }

    public boolean isCrbtSupported() {
        return mIsCrbtSupported;
    }

    public boolean isSmsSupported() {
        return mImsRadioHal.isFeatureSupported(Feature.SMS);
    }

    public boolean isCrsSupported() {
        return mImsRadioHal.isFeatureSupported(Feature.CRS);
    }

    // For future features, use QtiRadioConfig for feature supported
    // check. ImsRadio will return true for legacy features only.
    public boolean isFeatureSupported(int feature) {
        // Initialization of ImsRadio and QtiRadioConfig occurs on the main thread
        // and both instances should be available when clients attempt to query
        // for isFeatureSupported. Adding null checks due to crashes seen
        // when running UTs when the setup is run on a different thread
        if (mImsRadioHal == null || mQtiRadioConfigHal == null) {
            return false;
        }
        try {
            return (mImsRadioHal.isFeatureSupported(feature) ||
                    mQtiRadioConfigHal.isFeatureSupported(feature));
        } catch(RemoteException ex) {
            Log.e(LOG_TAG, "isFeatureSupported Failed", ex);
            return false;
        }
    }

    public void registerForImsSubConfigChanged(Handler h, int what, Object obj) {
        mImsSubConfigChangeRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForImsSubConfigChanged(Handler h) {
        mImsSubConfigChangeRegistrants.remove(h);
    }

    public void registerForSsacStatusChanged(Handler h, int what, Object obj) {
        Registrant r = new WakeLockRegistrant(h, what, obj, mNotifyWakeLock);
        mSsaccRegistrants.add(r);
    }

    public void unregisterForSsacStatusChanged(Handler h) {
        mSsaccRegistrants.remove(h);
    }

    public void registerForVopsStatusChanged(Handler h, int what, Object obj) {
        Registrant r = new WakeLockRegistrant(h, what, obj, mNotifyWakeLock);
        mVopsRegistrants.add(r);
    }

    public void unregisterForVopsStatusChanged(Handler h) {
        mVopsRegistrants.remove(h);
    }

    public void registerForWfcRoamingModeFeatureSupport(Handler h, int what, Object obj) {
        Registrant r = new WakeLockRegistrant(h, what, obj, mNotifyWakeLock);
        mWfcRoamingModeConfigRegistrants.add(r);
        mWfcRoamingModeConfigRegistrants.notifyRegistrants(new AsyncResult(null,
                mWfcRoamingConfigurationSupport, null));
    }

    public void unregisterForWfcRoamingModeFeatureSupport(Handler h) {
        mWfcRoamingModeConfigRegistrants.remove(h);
    }

    public void registerForHandoverStatusChanged(Handler h, int what, Object obj) {
        Registrant r = new WakeLockRegistrant(h, what, obj, mNotifyWakeLock);
        mHandoverStatusRegistrants.add(r);
    }

    public void unregisterForHandoverStatusChanged(Handler h) {
        mHandoverStatusRegistrants.remove(h);
    }

    public void registerForRefreshConfInfo(Handler h, int what, Object obj) {
        Registrant r = new WakeLockRegistrant(h, what, obj, mNotifyWakeLock);
        mRefreshConfInfoRegistrations.add(r);
    }

    public void unregisterForRefreshConfInfo(Handler h) {
        mRefreshConfInfoRegistrations.remove(h);
    }

    public void registerForSrvStatusUpdate(Handler h, int what, Object obj) {
        Registrant r = new WakeLockRegistrant(h, what, obj, mNotifyWakeLock);
        mSrvStatusRegistrations.add(r);
    }

    public void unregisterForSrvStatusUpdate(Handler h) {
        mSrvStatusRegistrations.remove(h);
    }

    public void registerForTtyStatusChanged(Handler h, int what, Object obj) {
        Registrant r = new WakeLockRegistrant(h, what, obj, mNotifyWakeLock);
        mTtyStatusRegistrants.add(r);
    }

    public void unregisterForTtyStatusChanged(Handler h) {
        mTtyStatusRegistrants.remove(h);
    }

    public void registerForGeolocationRequest(Handler h, int what, Object obj) {
        Registrant r = new WakeLockRegistrant(h, what, obj, mNotifyWakeLock);
        mGeolocationRegistrants.add(r);
    }

    public void unregisterForGeolocationRequest(Handler h) {
        mGeolocationRegistrants.remove(h);
    }

    public void registerForVoWiFiCallQualityUpdate(Handler h, int what, Object obj) {
        Registrant r = new WakeLockRegistrant(h, what, obj, mNotifyWakeLock);
        mVoWiFiCallQualityRegistrants.add(r);
    }

    public void unregisterForVoWiFiCallQualityUpdate(Handler h) {
        mVoWiFiCallQualityRegistrants.remove(h);
    }

    public void registerForParticipantStatusInfo(Handler h, int what, Object obj) {
        Registrant r = new WakeLockRegistrant(h, what, obj, mNotifyWakeLock);
        mParticipantStatusRegistrants.add(r);
    }

    public void unregisterForParticipantStatusInfo(Handler h) {
        mParticipantStatusRegistrants.remove(h);
    }

    public void registerForRegistrationBlockStatus(Handler h, int what, Object obj) {
        Registrant r = new WakeLockRegistrant(h, what, obj, mNotifyWakeLock);
        mRegistrationBlockStatusRegistrants.add(r);
    }

    public void unregisterForRegistrationBlockStatus(Handler h) {
        mRegistrationBlockStatusRegistrants.remove(h);
    }

    public void setOnSuppServiceNotification(Handler h, int what, Object obj) {
        mSsnRegistrant = new WakeLockRegistrant(h, what, obj, mNotifyWakeLock);
    }

    public void unSetOnSuppServiceNotification(Handler h) {
        if (mSsnRegistrant != null) {
            mSsnRegistrant.clear();
            mSsnRegistrant = null;
        }
    }

    public void registerForSuppServiceIndication(Handler h, int what, Object obj) {
        mSsIndicationRegistrant = new WakeLockRegistrant(h, what, obj, mNotifyWakeLock);
    }

    public void unregisterForSuppServiceIndication(Handler h) {
        if (mSsIndicationRegistrant != null) {
            mSsIndicationRegistrant.clear();
            mSsIndicationRegistrant = null;
        }
    }

    public void registerForUssdInfo(Handler h, int what, Object obj) {
        Registrant r = new WakeLockRegistrant(h, what, obj, mNotifyWakeLock);
        mUssdInfoRegistrants.add(r);
    }

    public void unregisterForUssdInfo(Handler h) {
        mUssdInfoRegistrants.remove(h);
    }

    public void registerForSipDtmfInfo(Handler h, int what, Object obj) {
        Registrant r = new WakeLockRegistrant(h, what, obj, mNotifyWakeLock);
        mSipDtmfInfoRegistrants.add(r);
    }

    public void unregisterForSipDtmfInfo(Handler h) {
        mSipDtmfInfoRegistrants.remove(h);
    }

    public void setOnIncomingImsSms(Handler h, int what, Object obj){
        mIncomingSmsRegistrant = new WakeLockRegistrant(h, what, obj, mNotifyWakeLock);
    }

    public void unSetIncomingImsSms(Handler h){
        if (mIncomingSmsRegistrant != null) {
            mIncomingSmsRegistrant.clear();
            mIncomingSmsRegistrant = null;
        }
    }

    public void setOnImsSmsStatusReport(Handler h, int what, Object obj){
        mSendSmsStatusReportRegistrant = new WakeLockRegistrant(h, what, obj, mNotifyWakeLock);
    }

    public void unSetImsSmsStatusReport(Handler h){
        if (mSendSmsStatusReportRegistrant != null) {
            mSendSmsStatusReportRegistrant.clear();
            mSendSmsStatusReportRegistrant = null;
        }
    }

    public void registerForCallAutoRejection(Handler h, int what, Object obj) {
        Registrant r = new WakeLockRegistrant(h, what, obj, mNotifyWakeLock);
        mAutoRejectRegistrants.add(r);
    }

    public void unregisterForCallAutoRejection(Handler h) {
        mAutoRejectRegistrants.remove(h);
    }

    public void registerForMultiIdentityRegistrationStatusChange(
            Handler h, int what, Object obj) {
        Registrant r = new WakeLockRegistrant(h, what, obj, mNotifyWakeLock);
        mMultiIdentityStatusChangeRegistrants.add(r);
    }

    public void registerForMultiIdentityInfoPendingIndication(
            Handler h, int what, Object obj) {
        Registrant r = new WakeLockRegistrant(h, what, obj, mNotifyWakeLock);
        mMultiIdentityInfoPendingRegistrants.add(r);
    }

    public void unregisterForMultiIdentityRegistrationStatusChange(Handler h) {
        mMultiIdentityStatusChangeRegistrants.remove(h);
    }

    public void unregisterForMultiIdentityPendingInfoRequest(Handler h) {
        mMultiIdentityInfoPendingRegistrants.remove(h);
    }

    public void registerForGeoLocationDataStatus(Handler h, int what, Object obj) {
        Registrant r = new WakeLockRegistrant(h, what, obj, mNotifyWakeLock);
        mGeoLocationDataStatusRegistrants.add(r);
    }

    public void unregisterForGeoLocationDataStatus(Handler h) {
        mGeoLocationDataStatusRegistrants.remove(h);
    }

    public void registerForSrvDomainChanged(Handler h, int what, Object obj) {
        Registrant r = new WakeLockRegistrant(h, what, obj, mNotifyWakeLock);
        mSrvDomainChangedRegistrants.add(r);
    }

    public void unregisterForSrvDomainChanged(Handler h) {
        mSrvDomainChangedRegistrants.remove(h);
    }

    public void registerForConferenceCallStateCompleted(Handler h, int what, Object obj) {
        Registrant r = new WakeLockRegistrant(h, what, obj, mNotifyWakeLock);
        mConferenceCallStateCompletedRegistrants.add(r);
    }

    public void unregisterForConferenceCallStateCompleted(Handler h) {
        mConferenceCallStateCompletedRegistrants.remove(h);
    }

    public void registerForSmsCallbackModeChanged(Handler h, int what, Object obj) {
        Registrant r = new WakeLockRegistrant(h, what, obj, mNotifyWakeLock);
        mSmsCallbackModeChangedRegistrants.add(r);
    }

    public void unregisterForSmsCallbackModeChanged(Handler h) {
        mSmsCallbackModeChangedRegistrants.remove(h);
    }

    public void registerForIncomingDtmfStart(Handler h, int what, Object obj) {
        Registrant r = new WakeLockRegistrant(h, what, obj, mNotifyWakeLock);
        mIncomingDtmfStartRegistrants.add(r);
    }

    public void unregisterForIncomingDtmfStart(Handler h) {
        mIncomingDtmfStartRegistrants.remove(h);
    }

    public void registerForIncomingDtmfStop(Handler h, int what, Object obj) {
        Registrant r = new WakeLockRegistrant(h, what, obj, mNotifyWakeLock);
        mIncomingDtmfStopRegistrants.add(r);
    }

    public void unregisterForIncomingDtmfStop(Handler h) {
        mIncomingDtmfStopRegistrants.remove(h);
    }

    public void registerForMultiSimVoiceCapabilityChanged(Handler h, int what, Object obj) {
        mMultiSimVoiceCapabilityChangedRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForMultiSimVoiceCapabilityChanged(Handler h) {
        mMultiSimVoiceCapabilityChangedRegistrants.remove(h);
    }

    public void registerForCiwlanNotification(Handler h, int what, Object obj) {
        mCiWlanNotificationRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForCiwlanNotification(Handler h) {
        mCiWlanNotificationRegistrants.remove(h);
    }

    public void registerForSrtpEncryptionUpdate(Handler h, int what, Object obj) {
        Registrant r = new WakeLockRegistrant(h, what, obj, mNotifyWakeLock);
        mSrtpEncryptionUpdateRegistrants.add(r);
    }

    public void unregisterForSrtpEncryptionUpdate(Handler h) {
        mSrtpEncryptionUpdateRegistrants.remove(h);
    }

    public void registerForImsServiceUp(Handler h, int what, Object obj) {
        mImsServiceUpRegistrants.addUnique(h, what, obj);
        if (mImsRadioHal != null && mImsRadioHal.isAlive()) {
            notifyServiceUp(IIMS_RADIO_SERVICE_NAME[mPhoneId]);
        }
    }

    public void unregisterForImsServiceUp(Handler h) {
        mImsServiceUpRegistrants.remove(h);
    }

    public void registerForImsServiceDown(Handler h, int what, Object obj) {
        mImsServiceDownRegistrants.addUnique(h, what, obj);
        if (mImsRadioHal == null || !mImsRadioHal.isAlive()) {
            notifyServiceDown(IIMS_RADIO_SERVICE_NAME[mPhoneId]);
        }
    }

    public void unregisterForImsServiceDown(Handler h) {
        mImsServiceDownRegistrants.remove(h);
    }

    @VisibleForTesting
    public Registrant getIncomingSmsRegistrant(){
        return mIncomingSmsRegistrant;
    }

    @VisibleForTesting
    public Registrant getSendSmsStatusReportRegistrant(){
        return mSendSmsStatusReportRegistrant;
    }

    class ImsRadioHandler extends Handler {
        ImsRadioHandler() {
            this(Looper.getMainLooper());
        }

        ImsRadioHandler(Looper looper) {
            super(looper);
        }

        // ***** Handler implementation
        @Override
        public void handleMessage(Message msg) {
            IFRequest rr = (IFRequest) (msg.obj);

            switch (msg.what) {
                case EVENT_WAKE_LOCK_TIMEOUT:
                    /**
                     * Haven't heard back from the last request. Assume we're not getting a
                     * response and release the wake lock.
                     * The timer of WAKE_LOCK_TIMEOUT is reset with each new send request. So when
                     * WAKE_LOCK_TIMEOUT occurs all requests in mRequestList already waited at
                     * least DEFAULT_WAKE_LOCK_TIMEOUT but no response.
                     * Reset mRequestMessagesWaiting to enable releaseWakeLockIfDone().
                     *
                     * Note: Keep mRequestList so that delayed response can still be handled when
                     * response finally comes.
                     */
                    synchronized (mWakeLock) {
                        if (mWakeLock.isHeld()) {

                            if (mRequestMessagesWaiting != 0) {
                                Log.i(this, "Number of messages still waiting for response "
                                        + mRequestMessagesWaiting + " at TIMEOUT. Reset to 0");

                                mRequestMessagesWaiting = 0;

                                synchronized (mRequestsList) {
                                    int count = mRequestsList.size();
                                    Log.i(this, "WAKE_LOCK_TIMEOUT " +
                                            " mRequestList=" + count);

                                    for (int i = 0; i < count; i++) {
                                        rr = mRequestsList.get(i);
                                        Log.i(this, i + ": [" + rr.mSerial + "] "
                                                + msgIdToString(rr.mRequest));
                                    }
                                }
                            }
                            mWakeLock.release();
                        }
                    }
                    break;
                case EVENT_QTI_RADIO_CONFIG_SERVICE_UP:
                    Log.d(this, "EVENT_QTI_RADIO_CONFIG_SERVICE_UP received");
                    mIsAidlReorderingSupported =
                            isFeatureSupported(Feature.INTERNAL_AIDL_REORDERING);
                    mIsCrbtSupported = isFeatureSupported(Feature.UVS_CRBT_CALL);
                    break;
            }
        }
    }

    private void notifyUssdInfo(UssdInfo info, ImsRilException ex) {
        if (mUssdInfoRegistrants != null) {
            mUssdInfoRegistrants.notifyRegistrants(new AsyncResult(null, info, ex));
         }
    }

    private void notifySipDtmfInfo(String configCode) {
        if (mSipDtmfInfoRegistrants != null) {
            mSipDtmfInfoRegistrants.notifyRegistrants(new AsyncResult(null, configCode, null));
         }
    }

    /* Overloading function with default argument. */
    private void removeFromQueueAndSendResponse(int token) {
        removeFromQueueAndSendResponse(token, ImsErrorCode.GENERIC_FAILURE);
    }

    private void removeFromQueueAndSendResponse(int token, Object ret) {
        removeFromQueueAndSendResponse(token, ImsErrorCode.GENERIC_FAILURE, ret);
    }

    /**
     * Removes the request matching the token id from the request list and sends the response
     * to the client
     *
     * @param token to match request/response. Response must include same token as request
     * @param errorCode of type ImsErrorCode.Error send back from RIL for the dial request
     * @param ret Result object of the request to be passed back to the client
     *
     */
    private void removeFromQueueAndSendResponse(int token, int errorCode) {
        removeFromQueueAndSendResponse(token, errorCode, null);
    }

    private void removeFromQueueAndSendResponse(int token, int errorCode, Object ret) {
        IFRequest rr = findAndRemoveRequestFromList(token);

        if (rr == null) {
            Log.w(this, "Unexpected solicited response or Invalid token id! token: "
                        + token + " error: " + errorCode);
            return;
        }
        sendResponse(rr, errorCode, ret);
    }

    /**
     * Sends the response to the client corresponding to the IFRequest passed in as a param and
     * returns any error send to the client along with the result object
     *
     * @param rr IFRequest containing the token id, request type, request id, etc.
     * @param errorCode of type ImsErrorCode.Error send back from RIL for the dial request
     * @param ret Result object of the request to be passed back to the client
     *
     */
    private void sendResponse(IFRequest rr, int error, Object ret) {
        if (error != ImsErrorCode.SUCCESS) {
            rr.onError(error, ret);
            rr.release();
            releaseWakeLockIfDone();
            return;
        }

        log(rr.serialString() + "< " + msgIdToString(rr.mRequest)
                + " " + retToString(rr.mRequest, ret));

        if (rr.mResult != null) {
            AsyncResult.forMessage(rr.mResult, ret, null);
            rr.mResult.sendToTarget();
        }

        rr.release();
        releaseWakeLockIfDone();
    }

    public ImsSenderRxr(Context context, int phoneId) {
        super(context);

        mPhoneId = phoneId;
        initNotifyRegistrants();

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LOG_TAG);
        mWakeLock.setReferenceCounted(false);
        mWakeLockTimeout = TelephonyProperties.wake_lock_timeout()
                .orElse(DEFAULT_WAKE_LOCK_TIMEOUT);
        mImsRadioHandler = new ImsRadioHandler();
        mRequestMessagesWaiting = 0;

        mQtiRadioConfigHal = new QtiRadioConfigAidl(context, mQtiRadioConfigIndication);
        // Call factory to invoke HIDL or AIDL
        mImsRadioHal = ImsRadioHalFactory.newImsRadioHal(mImsResponse, mImsIndication, mPhoneId,
                context);
    }

    private void initNotifyRegistrants() {
        mHandoverStatusRegistrants = new WakeLockRegistrantList(mNotifyWakeLock);
        mRefreshConfInfoRegistrations = new WakeLockRegistrantList(mNotifyWakeLock);
        mSrvStatusRegistrations = new WakeLockRegistrantList(mNotifyWakeLock);
        mTtyStatusRegistrants = new WakeLockRegistrantList(mNotifyWakeLock);
        mRadioStateRegistrations = new WakeLockRegistrantList(mNotifyWakeLock);
        mGeolocationRegistrants = new WakeLockRegistrantList(mNotifyWakeLock);
        mVoWiFiCallQualityRegistrants = new WakeLockRegistrantList(mNotifyWakeLock);
        mSsaccRegistrants = new WakeLockRegistrantList(mNotifyWakeLock);
        mVopsRegistrants = new WakeLockRegistrantList(mNotifyWakeLock);
        mParticipantStatusRegistrants = new WakeLockRegistrantList(mNotifyWakeLock);
        mImsSubConfigChangeRegistrants = new WakeLockRegistrantList(mNotifyWakeLock);
        mRegistrationBlockStatusRegistrants = new WakeLockRegistrantList(mNotifyWakeLock);
        mModifyCallRegistrants = new WakeLockRegistrantList(mNotifyWakeLock);
        mMwiRegistrants = new WakeLockRegistrantList(mNotifyWakeLock);
        mRttMessageRegistrants = new WakeLockRegistrantList(mNotifyWakeLock);
        mRttModifyRegistrants = new WakeLockRegistrantList(mNotifyWakeLock);
        mAutoRejectRegistrants = new WakeLockRegistrantList(mNotifyWakeLock);
        mVoiceInfoStatusRegistrants = new WakeLockRegistrantList(mNotifyWakeLock);
        mMultiIdentityStatusChangeRegistrants = new WakeLockRegistrantList(mNotifyWakeLock);
        mMultiIdentityInfoPendingRegistrants = new WakeLockRegistrantList(mNotifyWakeLock);
        mWfcRoamingModeConfigRegistrants = new WakeLockRegistrantList(mNotifyWakeLock);
        mUssdInfoRegistrants = new WakeLockRegistrantList(mNotifyWakeLock);
        mGeoLocationDataStatusRegistrants = new WakeLockRegistrantList(mNotifyWakeLock);
        mSipDtmfInfoRegistrants = new WakeLockRegistrantList(mNotifyWakeLock);
        mSrvDomainChangedRegistrants = new WakeLockRegistrantList(mNotifyWakeLock);
        mConferenceCallStateCompletedRegistrants = new WakeLockRegistrantList(mNotifyWakeLock);
        mSmsCallbackModeChangedRegistrants = new WakeLockRegistrantList(mNotifyWakeLock);
        mIncomingDtmfStartRegistrants = new WakeLockRegistrantList(mNotifyWakeLock);
        mIncomingDtmfStopRegistrants = new WakeLockRegistrantList(mNotifyWakeLock);
        mMultiSimVoiceCapabilityChangedRegistrants = new WakeLockRegistrantList(mNotifyWakeLock);
        mCiWlanNotificationRegistrants = new WakeLockRegistrantList(mNotifyWakeLock);
        mSrtpEncryptionUpdateRegistrants = new WakeLockRegistrantList(mNotifyWakeLock);
        mImsServiceUpRegistrants = new WakeLockRegistrantList(mNotifyWakeLock);
        mImsServiceDownRegistrants = new WakeLockRegistrantList(mNotifyWakeLock);
    }

    /**
     * Holds a PARTIAL_WAKE_LOCK whenever a) There is outstanding request sent
     * to the interface and no replied b) There is a request pending to be sent
     * out. There is a WAKE_LOCK_TIMEOUT to release the lock, though it
     * shouldn't happen often.
     */

    private void acquireWakeLock() {
        synchronized (mWakeLock) {
            mWakeLock.acquire();

            mImsRadioHandler.removeMessages(EVENT_WAKE_LOCK_TIMEOUT);
            Message msg = mImsRadioHandler.obtainMessage(EVENT_WAKE_LOCK_TIMEOUT);
            mImsRadioHandler.sendMessageDelayed(msg, mWakeLockTimeout);
        }
    }

    private void releaseWakeLockIfDone() {
        synchronized (mWakeLock) {
            if (mWakeLock.isHeld() &&
                    (mRequestMessagesWaiting == 0)) {
                mWakeLock.release();
            }
        }
    }

    /**
     * Acquires the wake lock, queues the IFRequest in the request list so that when the response
     * comes we can match it with the request.
     *
     * @param rr IFRequest containing the token id, request type, request id, etc.
     */
    private void queueRequest(IFRequest rr) {
        acquireWakeLock();

       synchronized (mRequestsList) {
           mRequestsList.add(rr);
           mRequestMessagesWaiting++;
        }
    }

    /**
     * Release each request in mRequestsList then clear the list
     *
     * @param error is the ImsErrorCode.Error sent back
     * @param loggable true means to print all requests in mRequestslist
     */
    private void clearRequestsList(int error, boolean loggable) {
        IFRequest rr;
        synchronized (mRequestsList) {
            int count = mRequestsList.size();
            if (loggable) {
                Log.i(this,"clearRequestsList: mRequestList=" + count);
            }

            for (int i = 0; i < count; i++) {
                rr = mRequestsList.get(i);
                if (loggable) {
                    Log.i(this, i + ": [" + rr.mSerial + "] " +
                            msgIdToString(rr.mRequest));
                }
                rr.onError(error, null);
                rr.release();
            }
            mRequestsList.clear();
            mRequestMessagesWaiting = 0;
            releaseWakeLockIfDone();
        }

        /* Clear the existing calls also */
        if (mIsUnsolCallListPresent) {
            mIsUnsolCallListPresent = false;
            mCallStateRegistrants
                    .notifyRegistrants(new AsyncResult(null, null,
                    new RuntimeException(ImsSenderRxr.errorIdToString(error))));
        }
    }

    private IFRequest findAndRemoveRequestFromList(int serial) {
        synchronized (mRequestsList) {
            for (int i = 0, s = mRequestsList.size(); i < s; i++) {
                IFRequest rr = mRequestsList.get(i);

                if (rr.mSerial == serial) {
                    mRequestsList.remove(i);
                    if (mRequestMessagesWaiting > 0)
                        mRequestMessagesWaiting--;
                    return rr;
                }
            }
        }

        return null;
    }

    private String retToString(int req, Object ret) {

        if (ret == null)
            return "";

        StringBuilder sb;
        String s;
        int length;
        if (ret instanceof int[]) {
            int[] intArray = (int[]) ret;
            length = intArray.length;
            sb = new StringBuilder("{");
            if (length > 0) {
                int i = 0;
                sb.append(intArray[i++]);
                while (i < length) {
                    sb.append(", ").append(intArray[i++]);
                }
            }
            sb.append("}");
            s = sb.toString();
        } else if (ret instanceof String[]) {
            String[] strings = (String[]) ret;
            length = strings.length;
            sb = new StringBuilder("{");
            if (length > 0) {
                int i = 0;
                sb.append(strings[i++]);
                while (i < length) {
                    sb.append(", ").append(strings[i++]);
                }
            }
            sb.append("}");
            s = sb.toString();
        } else if (req == MessageId.UNSOL_RESPONSE_CALL_STATE_CHANGED
                || req == MessageId.REQUEST_GET_CURRENT_CALLS ) {
            ArrayList<DriverCallIms> calls = (ArrayList<DriverCallIms>) ret;
            sb = new StringBuilder(" ");
            for (DriverCallIms dc : calls) {
                sb.append("[").append(dc).append("] ");
            }
            s = sb.toString();
        } else {
            s = ret.toString();
        }
        return s;
    }

    public void registerForModifyCall(Handler h, int what, Object obj) {
        Registrant r = new WakeLockRegistrant(h, what, obj, mNotifyWakeLock);
        mModifyCallRegistrants.add(r);
    }

    public void unregisterForModifyCall(Handler h) {
        mModifyCallRegistrants.remove(h);
    }

    public void registerForMwi(Handler h, int what, Object obj) {
        Registrant r = new WakeLockRegistrant(h, what, obj, mNotifyWakeLock);
        mMwiRegistrants.add(r);
    }

    public void unregisterForMwi(Handler h) {
        mMwiRegistrants.remove(h);
    }

    static String errorIdToString(int request) {
        String errorMsg;
        switch (request) {
            case ImsErrorCode.SUCCESS:
                return "SUCCESS";
            case ImsErrorCode.RADIO_NOT_AVAILABLE:
                return "E_RADIO_NOT_AVAILABLE";
            case ImsErrorCode.GENERIC_FAILURE:
                return "E_GENERIC_FAILURE";
            case ImsErrorCode.PASSWORD_INCORRECT:
                return "E_PASSWORD_INCORRECT";
            case ImsErrorCode.REQUEST_NOT_SUPPORTED:
                return "E_REQUEST_NOT_SUPPORTED";
            case ImsErrorCode.CANCELLED:
                return "E_CANCELLED";
            case ImsErrorCode.UNUSED:
                return "E_UNUSED";
            case ImsErrorCode.INVALID_PARAMETER:
                return "E_INVALID_PARAMETER";
            case ImsErrorCode.REJECTED_BY_REMOTE:
                return "E_REJECTED_BY_REMOTE";
            case ImsErrorCode.NETWORK_NOT_SUPPORTED:
                return "E_NETWORK_NOT_SUPPORTED";
            case ImsErrorCode.FDN_CHECK_FAILURE:
                return "E_FDN_CHECK_FAILURE";
            case ImsErrorCode.SS_MODIFIED_TO_DIAL:
                return "E_SS_MODIFIED_TO_DIAL";
            case ImsErrorCode.SS_MODIFIED_TO_USSD:
                return "E_SS_MODIFIED_TO_USSD";
            case ImsErrorCode.SS_MODIFIED_TO_SS:
                return "E_SS_MODIFIED_TO_SS";
            case ImsErrorCode.SS_MODIFIED_TO_DIAL_VIDEO:
                return "E_SS_MODIFIED_TO_DIAL_VIDEO";
            case ImsErrorCode.USSD_CS_FALLBACK:
                return "E_USSD_CS_FALLBACK";
            case ImsErrorCode.SEND_SIP_DTMF_FAILED:
                return "E_SEND_SIP_DTMF_FAILED";
            case ImsErrorCode.CF_SERVICE_NOT_REGISTERED:
                return "E_CF_SERVICE_NOT_REGISTERED";
            case ImsErrorCode.RIL_FAILED_INTERNAL:
                return "E_RIL_FAILED_INTERNAL";
            default:
                return "E_UNKNOWN";
        }
    }

    static String msgIdToString(int request) {
        // TODO - check all supported messages are covered
        switch (request) {
            case MessageId.REQUEST_GET_CURRENT_CALLS:
                return "GET_CURRENT_CALLS";
            case MessageId.REQUEST_DIAL:
                return "DIAL";
            case MessageId.REQUEST_ANSWER:
                return "REQUEST_ANSWER";
            case MessageId.REQUEST_DEFLECT_CALL:
                return "REQUEST_DEFLECT_CALL";
            case MessageId.REQUEST_ADD_PARTICIPANT:
                return "REQUEST_ADD_PARTICIPANT";
            case MessageId.REQUEST_HANGUP:
                return "HANGUP";
            case MessageId.REQUEST_HANGUP_WAITING_OR_BACKGROUND:
                return "HANGUP_WAITING_OR_BACKGROUND";
            case MessageId.REQUEST_HANGUP_FOREGROUND_RESUME_BACKGROUND:
                return "HANGUP_FOREGROUND_RESUME_BACKGROUND";
            case MessageId.REQUEST_SWITCH_WAITING_OR_HOLDING_AND_ACTIVE:
                return "MessageId.REQUEST_SWITCH_WAITING_OR_HOLDING_AND_ACTIVE";
            case MessageId.REQUEST_CONFERENCE:
                return "CONFERENCE";
            case MessageId.REQUEST_UDUB:
                return "UDUB";
            case MessageId.REQUEST_SEND_UI_TTY_MODE:
                return "REQUEST_SEND_UI_TTY_MODE";
            case MessageId.REQUEST_MODIFY_CALL_INITIATE:
                return "MODIFY_CALL_INITIATE";
            case MessageId.REQUEST_MODIFY_CALL_CONFIRM:
                return "MODIFY_CALL_CONFIRM";
            case MessageId.UNSOL_MODIFY_CALL:
                return "UNSOL_MODIFY_CALL";
            case MessageId.REQUEST_LAST_CALL_FAIL_CAUSE:
                return "LAST_CALL_FAIL_CAUSE";
            case MessageId.REQUEST_DTMF:
                return "DTMF";
            case MessageId.REQUEST_DTMF_START:
                return "DTMF_START";
            case MessageId.REQUEST_DTMF_STOP:
                return "DTMF_STOP";
            case MessageId.REQUEST_EXPLICIT_CALL_TRANSFER:
                return "REQUEST_EXPLICIT_CALL_TRANSFER";
            case MessageId.REQUEST_EXIT_EMERGENCY_CALLBACK_MODE:
                return "REQUEST_EXIT_EMERGENCY_CALLBACK_MODE";
            case MessageId.REQUEST_IMS_REGISTRATION_STATE:
                return "REQUEST_IMS_REGISTRATION_STATE";
            case MessageId.REQUEST_QUERY_CLIP:
                return "REQUEST_QUERY_CLIP";
            case MessageId.REQUEST_QUERY_SERVICE_STATUS:
                return "REQUEST_QUERY_SERVICE_STATUS";
            case MessageId.REQUEST_SET_SERVICE_STATUS:
                return "REQUEST_SET_SERVICE_STATUS";
            case MessageId.REQUEST_GET_CLIR:
                return "REQUEST_GET_CLIR";
            case MessageId.REQUEST_SET_CLIR:
                return "REQUEST_SET_CLIR";
            case MessageId.REQUEST_QUERY_CALL_FORWARD_STATUS:
                return "REQUEST_QUERY_CALL_FORWARD_STATUS";
            case MessageId.REQUEST_SET_CALL_FORWARD_STATUS:
                return "REQUEST_SET_CALL_FORWARD_STATUS";
            case MessageId.REQUEST_QUERY_CALL_WAITING:
                return "REQUEST_QUERY_CALL_WAITING";
            case MessageId.REQUEST_SET_CALL_WAITING:
                return "REQUEST_SET_CALL_WAITING";
            case MessageId.REQUEST_SET_SUPP_SVC_NOTIFICATION:
                return "REQUEST_SET_SUPP_SVC_NOTIFICATION";
            case MessageId.REQUEST_SUPP_SVC_STATUS:
                return "REQUEST_SUPP_SVC_STATUS";
            case MessageId.REQUEST_GET_RTP_STATISTICS:
                return "REQUEST_GET_RTP_STATISTICS";
            case MessageId.REQUEST_GET_RTP_ERROR_STATISTICS:
                return "REQUEST_GET_RTP_ERROR_STATISTICS";
            case MessageId.REQUEST_GET_WIFI_CALLING_STATUS:
                return "REQUEST_GET_WIFI_CALLING_STATUS";
            case MessageId.REQUEST_SET_WIFI_CALLING_STATUS:
                return "REQUEST_SET_WIFI_CALLING_STATUS";
            case MessageId.REQUEST_GET_COLR:
                return "REQUEST_GET_COLR";
            case MessageId.REQUEST_SET_COLR:
                return "REQUEST_SET_COLR";
            case MessageId.REQUEST_HOLD:
                return "REQUEST_HOLD";
            case MessageId.REQUEST_RESUME:
                return "REQUEST_RESUME";
            case MessageId.REQUEST_SET_IMS_CONFIG:
                return "REQUEST_SET_IMS_CONFIG";
            case MessageId.REQUEST_GET_IMS_CONFIG:
                return "REQUEST_GET_IMS_CONFIG";
            case MessageId.REQUEST_SEND_GEOLOCATION_INFO:
                return "REQUEST_SEND_GEOLOCATION_INFO";
            case MessageId.REQUEST_GET_VOPS_INFO:
                return "REQUEST_GET_VOPS_INFO";
            case MessageId.REQUEST_GET_SSAC_INFO:
                return "REQUEST_GET_SSAC_INFO";
            case MessageId.REQUEST_SET_VOLTE_PREF:
                return "REQUEST_SET_VOLTE_PREF";
            case MessageId.REQUEST_GET_VOLTE_PREF:
                return "REQUEST_GET_VOLTE_PREF";
            case MessageId.REQUEST_GET_HANDOVER_CONFIG:
                return "REQUEST_GET_HANDOVER_CONFIG";
            case MessageId.REQUEST_SET_HANDOVER_CONFIG:
                return "REQUEST_SET_HANDOVER_CONFIG";
            case MessageId.REQUEST_GET_IMS_SUB_CONFIG:
                return "REQUEST_GET_IMS_SUB_CONFIG";
            case MessageId.REQUEST_SEND_RTT_MSG:
                return "REQUEST_SEND_RTT_MSG";
            case MessageId.REQUEST_CANCEL_MODIFY_CALL:
                return "REQUEST_CANCEL_MODIFY_CALL";
            case MessageId.REQUEST_SEND_IMS_SMS:
                return "REQUEST_SEND_IMS_SMS";
            case MessageId.REQUEST_ACK_IMS_SMS:
                return "REQUEST_ACK_IMS_SMS";
            case MessageId.REQUEST_ACK_IMS_SMS_STATUS_REPORT:
                return "REQUEST_ACK_IMS_SMS_STATUS_REPORT";
            case MessageId.REQUEST_REGISTER_MULTI_IDENTITY_LINES:
                return "REQUEST_REGISTER_MULTI_IDENTITY_LINES";
            case MessageId.REQUEST_QUERY_VIRTUAL_LINE_INFO:
                return "REQUEST_QUERY_VIRTUAL_LINE_INFO";
            case MessageId.REQUEST_EMERGENCY_DIAL:
                return "REQUEST_EMERGENCY_DIAL";
            case MessageId.REQUEST_CALL_COMPOSER_DIAL:
                return "CALL_COMPOSER_DIAL";
            case MessageId.REQUEST_USSD:
                return "REQUEST_USSD";
            case MessageId.REQUEST_CANCEL_USSD:
                return "REQUEST_CANCEL_USSD";
            case MessageId.REQUEST_SIP_DTMF:
                return "REQUEST_SIP_DTMF";
            case MessageId.REQUEST_SET_MEDIA_CONFIG:
                return "REQUEST_SET_MEDIA_CONFIG";
            case MessageId.REQUEST_QUERY_MULTI_SIM_VOICE_CAPABILITY:
                return "REQUEST_QUERY_MULTI_SIM_VOICE_CAPABILITY";
            case MessageId.REQUEST_EXIT_SCBM:
                return "REQUEST_EXIT_SCBM";
            case MessageId.REQUEST_SEND_VOS_SUPPORT_STATUS:
                return "REQUEST_SEND_VOS_SUPPORT_STATUS";
            case MessageId.REQUEST_SEND_VOS_ACTION_INFO:
                return "REQUEST_SEND_VOS_ACTION_INFO";
            case MessageId.REQUEST_SET_GLASSES_FREE_3D_VIDEO_CAPABILITY:
                return "REQUEST_SET_GLASSES_FREE_3D_VIDEO_CAPABILITY";
            case MessageId.REQUEST_ABORT_CONFERENCE:
                return "REQUEST_ABORT_CONFERENCE";
            case MessageId.UNSOL_RESPONSE_IMS_NETWORK_STATE_CHANGED:
                return "UNSOL_RESPONSE_IMS_NETWORK_STATE_CHANGED";
            case MessageId.UNSOL_RESPONSE_CALL_STATE_CHANGED:
                return "UNSOL_RESPONSE_CALL_STATE_CHANGED";
            case MessageId.UNSOL_CALL_RING:
                return "UNSOL_CALL_RING";
            case MessageId.UNSOL_ENTER_EMERGENCY_CALLBACK_MODE:
                return "UNSOL_ENTER_EMERGENCY_CALLBACK_MODE";
            case MessageId.UNSOL_RINGBACK_TONE:
                return "UNSOL_RINGBACK_TONE";
            case MessageId.UNSOL_EXIT_EMERGENCY_CALLBACK_MODE:
                return "UNSOL_EXIT_EMERGENCY_CALLBACK_MODE";
            case MessageId.REQUEST_IMS_REG_STATE_CHANGE:
                return "REQUEST_IMS_REG_STATE_CHANGE";
            case MessageId.UNSOL_RESPONSE_HANDOVER:
                return "UNSOL_RESPONSE_HANDOVER";
            case MessageId.UNSOL_REFRESH_CONF_INFO:
                return "UNSOL_REFRESH_CONF_INFO";
            case MessageId.UNSOL_SRV_STATUS_UPDATE:
                return "UNSOL_SRV_STATUS_UPDATE";
            case MessageId.UNSOL_SUPP_SVC_NOTIFICATION:
                return "UNSOL_SUPP_SVC_NOTIFICATION";
            case MessageId.UNSOL_TTY_NOTIFICATION:
                return "UNSOL_TTY_NOTIFICATION";
            case MessageId.UNSOL_RADIO_STATE_CHANGED:
                return "UNSOL_RADIO_STATE_CHANGED";
            case MessageId.UNSOL_MWI:
                return "UNSOL_MWI";
            case MessageId.UNSOL_REQUEST_GEOLOCATION:
                return "UNSOL_REQUEST_GEOLOCATION";
            case MessageId.UNSOL_REFRESH_VICE_INFO:
                return "UNSOL_REFRESH_VICE_INFO";
            case MessageId.UNSOL_VOWIFI_CALL_QUALITY:
                return "UNSOL_VOWIFI_CALL_QUALITY";
            case MessageId.UNSOL_VOPS_CHANGED:
                return "UNSOL_VOPS_CHANGED";
            case MessageId.UNSOL_SSAC_CHANGED:
                return "UNSOL_SSAC_CHANGED";
            case MessageId.UNSOL_PARTICIPANT_STATUS_INFO:
                return "UNSOL_PARTICIPANT_STATUS_INFO";
            case MessageId.UNSOL_IMS_SUB_CONFIG_CHANGED:
                return "UNSOL_IMS_SUB_CONFIG_CHANGED";
            case MessageId.UNSOL_RESPONSE_REGISTRATION_BLOCK_STATUS:
                return "UNSOL_RESPONSE_REGISTRATION_BLOCK_STATUS";
            case MessageId.UNSOL_RESPONSE_RTT_MSG_RECEIVED:
                return "UNSOL_RESPONSE_RTT_MSG_RECEIVED";
            case MessageId.UNSOL_ON_SS:
                return "UNSOL_ON_SS";
            case MessageId.UNSOL_IMS_SMS_STATUS_REPORT:
                return "UNSOL_IMS_SMS_STATUS_REPORT";
            case MessageId.UNSOL_INCOMING_IMS_SMS:
                return "UNSOL_INCOMING_IMS_SMS";
            case MessageId.UNSOL_AUTO_CALL_REJECTION_IND:
                return "UNSOL_AUTO_CALL_REJECTION_IND";
            case MessageId.UNSOL_VOICE_INFO:
                return "UNSOL_VOICE_INFO";
            case MessageId.UNSOL_MULTI_IDENTITY_REGISTRATION_STATUS_CHANGE:
                return "UNSOL_MULTI_IDENTITY_REGISTRATION_STATUS_CHANGE";
            case MessageId.UNSOL_MULTI_IDENTITY_INFO_PENDING:
                return "UNSOL_MULTI_IDENTITY_INFO_PENDING";
            case MessageId.UNSOL_MODEM_SUPPORTS_WFC_ROAMING_MODE:
                return "UNSOL_MODEM_SUPPORTS_WFC_ROAMING_MODE";
            case MessageId.UNSOL_USSD_FAILED:
                return "UNSOL_USSD_FAILED";
            case MessageId.UNSOL_AUTO_CALL_COMPOSER_CALL_REJECTION_IND:
                return "UNSOL_AUTO_CALL_COMPOSER_CALL_REJECTION_IND";
            case MessageId.UNSOL_CALL_COMPOSER_INFO_AVAILABLE_IND:
                return "UNSOL_CALL_COMPOSER_INFO_AVAILABLE_IND";
            case MessageId.UNSOL_RETRIEVE_GEO_LOCATION_DATA_STATUS:
                return "UNSOL_RETRIEVE_GEO_LOCATION_DATA_STATUS";
            case MessageId.UNSOL_USSD_RECEIVED:
                return "UNSOL_USSD_RECEIVED";
            case MessageId.UNSOL_SIP_DTMF_RECEIVED:
                return "UNSOL_SIP_DTMF_RECEIVED";
            case MessageId.UNSOL_SERVICE_DOMAIN_CHANGED:
                return "UNSOL_SERVICE_DOMAIN_CHANGED";
            case MessageId.UNSOL_CONFERENCE_CALL_STATE_COMPLETED:
                return "UNSOL_CONFERENCE_CALL_STATE_COMPLETED";
            case MessageId.UNSOL_SCBM_UPDATE_IND:
                return "UNSOL_SCBM_UPDATE_IND";
            case MessageId.UNSOL_INCOMING_DTMF_START:
                return "UNSOL_INCOMING_DTMF_START";
            case MessageId.UNSOL_INCOMING_DTMF_STOP:
                return "UNSOL_INCOMING_DTMF_STOP";
            case MessageId.UNSOL_MULTI_SIM_VOICE_CAPABILITY_CHANGED:
                return "UNSOL_MULTI_SIM_VOICE_CAPABILITY_CHANGED";
            case MessageId.UNSOL_INCOMING_CALL_AUTO_REJECTED:
                return "UNSOL_INCOMING_CALL_AUTO_REJECTED";
            case MessageId.UNSOL_PRE_ALERTING_CALL_INFO_AVAILABLE:
                return "UNSOL_PRE_ALERTING_CALL_INFO_AVAILABLE";
            case MessageId.UNSOL_C_IWLAN_NOTIFICATION:
                return "UNSOL_C_IWLAN_NOTIFICATION";
            case MessageId.UNSOL_SRTP_ENCRYPTION_UPDATE:
                return "UNSOL_SRTP_ENCRYPTION_UPDATE";
            default:
                return "<unknown message>";
        }
    }

    public void log(String msg) {
        Log.i(this, msg + "[SUB" + mPhoneId + "]");
    }

    public void logv(String msg) {
        Log.v(this, msg + "[SUB" + mPhoneId + "]");
    }

    /**
     * Use this only for unimplemented methods. Prints stack trace if the
     * unimplemented method is ever called
     */
    public void logUnimplemented() {
        try {
            Exception e = new Exception();
            throw e;
        } catch (Exception e) {
            Log.i(this, "Unimplemented method. Stack trace: ");
            e.printStackTrace();
        }
    }

    public void unsljLog(int response) {
        log("[UNSL]< " + msgIdToString(response));
    }

    public void unsljLogMore(int response, String more) {
        log("[UNSL]< " + msgIdToString(response) + " " + more);
    }

    public void unsljLogRet(int response, Object ret) {
        log("[UNSL]< " + msgIdToString(response) + " " + retToString(response, ret));
    }

    public void unsljLogvRet(int response, Object ret) {
        logv("[UNSL]< " + msgIdToString(response) + " " + retToString(response, ret));
    }

    public void logSolicitedRequest(IFRequest rr) {
        log(rr.serialString() + "> " + msgIdToString(rr.mRequest) + " ");
    }

    @Override
    public void setPhoneType(int phoneType) { // Called by Phone constructor
        log("setPhoneType=" + phoneType + " old value=" + mPhoneType);
        mPhoneType = phoneType;
    }

    public void addParticipant(String address, int clirMode, CallDetails callDetails,
            Message result) {
        log("addParticipant address = " + Log.pii(address) + " clirMode = " + clirMode
                + " callDetails = " + callDetails);
        final int msgId = MessageId.REQUEST_ADD_PARTICIPANT;
        final String msgIdString = msgIdToString(msgId);
        IFRequest rr = IFRequest.obtain(msgId, result);

        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        queueRequest(rr);

        try {
            logSolicitedRequest(rr);
            mImsRadioHal.addParticipant(rr.mSerial, address, clirMode, callDetails);
        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, msgIdString + " to IImsRadio: Exception: " + ex);
        }
    }

    private boolean sendErrorOnImsRadioDown(IFRequest rr, String msgIdString) {
        return sendErrorOnImsRadioDown(rr, msgIdString, null);
    }

    /**
     * If the Ims Radio service is down, send the error to clients and release the IFRequest
     *
     * @param rr IFRequest containing the token id, request type, request id, etc.
     * @return boolean - true if error was sent on Ims Radio service down, false if Service is up.
     * @param ret Result object of the request to be passed back to the client
     */
    private boolean sendErrorOnImsRadioDown(IFRequest rr, String msgIdString, Object ret) {
        if (mImsRadioHal != null && mImsRadioHal.isAlive()) {
            return false;
        }
        Log.e(this, "ImsRadio HAL is not available. Can't send " +
                    msgIdString + " to QCRIL");
        rr.onError(ImsErrorCode.RADIO_NOT_AVAILABLE, ret);
        rr.release();
        return true;
    }

    public void
    dial(String address, EmergencyCallInfo eInfo, int clirMode, CallDetails callDetails,
            boolean isEncrypted, Message result) {
        dial(address, eInfo, clirMode, callDetails, isEncrypted, null, null, result);
    }

    public void
    dial(String address, EmergencyCallInfo eInfo, int clirMode, CallDetails callDetails,
            boolean isEncrypted, CallComposerInfo ccInfo, RedialInfo redialInfo, Message result) {
        log("Dial Request - address= " + Log.pii(address) + "clirMode= " + clirMode
                + " callDetails= " + callDetails + " isEncrypted= " + isEncrypted
                + " redialInfo " + redialInfo);
        int msgId = MessageId.REQUEST_DIAL;
        if (eInfo != null && mImsRadioHal.isFeatureSupported(Feature.EMERGENCY_DIAL)) {
            msgId = MessageId.REQUEST_EMERGENCY_DIAL;
        } else if (ccInfo != null && mImsRadioHal.isFeatureSupported(Feature.CALL_COMPOSER_DIAL)) {
            msgId = MessageId.REQUEST_CALL_COMPOSER_DIAL;
        }
        final String msgIdString = msgIdToString(msgId);
        IFRequest rr = IFRequest.obtain(msgId, result);

        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        queueRequest(rr);
        try {
            logSolicitedRequest(rr);
            Log.v(this, msgIdString + " request to IImsRadio - token = " + rr.mSerial);
            mImsRadioHal.dial(rr.mSerial, address, eInfo, clirMode, callDetails, isEncrypted,
                    ccInfo, redialInfo);

        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, msgIdString + "to IImsRadio: Exception: " + ex);
        }
    }

    /**
     * Sends USSD request to RIL via the IImsRadio interface.
     * @param address for USSD request
     * Response will be received by sendUssdResponse()
     */
    public void sendUssd(String address, Message result) {
        log("USSD Request - address- " + Log.pii(address));
        final int msgId = MessageId.REQUEST_USSD;
        final String msgIdString = msgIdToString(msgId);
        IFRequest rr = IFRequest.obtain(msgId, result);

        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        if (!mImsRadioHal.isFeatureSupported(Feature.USSD)) {
            sendResponse(rr, ImsErrorCode.USSD_CS_FALLBACK, new ImsReasonInfo());
            return;
        }

        queueRequest(rr);
        try {
            logSolicitedRequest(rr);
            mImsRadioHal.sendUssd(rr.mSerial, address);
        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, msgIdString + " to IImsRadio: Exception: " + ex);
        }
    }

    /**
      * Sends SIP DTMF string to RIL
      * @param requestCode for SIP DTMF request
      * Response will be received by sendSipDtmfResponse()
      */
    public void sendSipDtmf(String requestCode, Message result) {
        log("Send Sip Dtmf Request - requestCode- " + Log.pii(requestCode));
        final int msgId = MessageId.REQUEST_SIP_DTMF;
        final String msgIdString = msgIdToString(msgId);
        IFRequest rr = IFRequest.obtain(msgId, result);
        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        if (!mImsRadioHal.isFeatureSupported(Feature.SIP_DTMF)) {
            sendResponse(rr, ImsErrorCode.SEND_SIP_DTMF_FAILED, null);
            return;
        }

        queueRequest(rr);
        try {
            logSolicitedRequest(rr);
            mImsRadioHal.sendSipDtmf(rr.mSerial, requestCode);
        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, msgIdString + " to IImsRadio: Exception: " + ex);
        }
    }

    /**
     * Sends cancel USSD request to RIL via the IImsRadio interface.
     * Response will be received by cancelPendingUssdResponse()
     */
    public void cancelPendingUssd(Message result) {
        log("Cancel pending USSD");
        final int msgId = MessageId.REQUEST_CANCEL_USSD;
        final String msgIdString = msgIdToString(msgId);
        IFRequest rr = IFRequest.obtain(msgId, result);

        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        queueRequest(rr);
        try {
            logSolicitedRequest(rr);
            mImsRadioHal.cancelPendingUssd(rr.mSerial);
        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, msgIdString + " to IImsRadio: Exception: " + ex);
        }
    }

    public void
    acceptCall(Message result, int callType) {
        acceptCall(result, callType, ImsStreamMediaProfile.RTT_MODE_DISABLED);
    }

    public void
    acceptCall(Message result, int callType, int rttMode) {
        acceptCall(result, callType, QtiImsExtUtils.QTI_IMS_TIR_PRESENTATION_DEFAULT, rttMode);
    }

    public void
    acceptCall(Message result, int callType, int ipPresentation, int rttMode) {
        final int msgId = MessageId.REQUEST_ANSWER;
        final String msgIdString = msgIdToString(msgId);

        IFRequest rr = IFRequest.obtain(msgId, result);

        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        queueRequest(rr);
        try {
            logSolicitedRequest(rr);
            mImsRadioHal.answer(rr.mSerial, callType, ipPresentation, rttMode);
            Log.i(this, "rtt mode : " + rttMode + " and ipPresentation: " +
                    ipPresentation + " to HAL");
        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, msgIdString + "request to IImsRadio: Exception: " + ex);
        }
    }

    public void deflectCall(int index, String number, Message result) {
        logv("deflect call to: " + number + "connid:" + index);
        final int msgId = MessageId.REQUEST_DEFLECT_CALL;
        final String msgIdString = msgIdToString(msgId);
        IFRequest rr = IFRequest.obtain(msgId, result);

        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        queueRequest(rr);
        try {
            logSolicitedRequest(rr);
            mImsRadioHal.deflectCall(rr.mSerial, index, number);
        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, msgIdString + "request to IImsRadio: Exception: " + ex);
        }
    }

    public void sendSms(int messageRef, String format, String smsc,
                        boolean isRetry, byte[] pdu, Message result){
        Log.i(this,"sendSms over IImsRadio with format:" + format);

        final int msgId = MessageId.REQUEST_SEND_IMS_SMS;
        final String msgIdString = msgIdToString(msgId);
        IFRequest rr = IFRequest.obtain(msgId, result);

        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        queueRequest(rr);
        try {
            logSolicitedRequest(rr);
            mImsRadioHal.sendSms(rr.mSerial, messageRef, format, smsc, isRetry, pdu);
        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, msgIdToString(msgId) + "to ImsRadioV12: Exception: " + ex.getMessage());
        }
    }

    public void acknowledgeSms(int messageRef, int result, Message target){
        Log.i(this,"acknowledgeSms: messageRef: " + messageRef + " result: " + result);

        final int msgId = MessageId.REQUEST_ACK_IMS_SMS;
        final String msgIdString = msgIdToString(msgId);
        // Don't queue this IFRequest in the request list as the response is not
        //sent by the hidl service.
        IFRequest rr = IFRequest.obtain(msgId, target);
        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        try {
            logSolicitedRequest(rr);
            mImsRadioHal.acknowledgeSms(rr.mSerial, messageRef, result);
        } catch (Exception ex) {
            Log.e(this, msgIdToString(msgId) + "request to ImsRadio: Exception: " + ex);
        }
        rr.release();
    }

    public void acknowledgeSmsReport(int messageRef, int result, Message target){
        Log.i(this,"acknowledgeSmsReport: messageRef: " + messageRef + " result: " + result);

        final int msgId = MessageId.REQUEST_ACK_IMS_SMS_STATUS_REPORT ;
        final String msgIdString = msgIdToString(msgId);
        //Don't queue this IFRequest in the request list as the response is not
        //sent by the hidl service.
        IFRequest rr = IFRequest.obtain(msgId, target);

        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        try {
            logSolicitedRequest(rr);
            mImsRadioHal.acknowledgeSmsReport(rr.mSerial, messageRef, result);
        } catch (Exception ex) {
            Log.e(this, msgIdToString(msgId) + "to ImsRadioV12: Exception: " + ex);
        }
        rr.release();
    }

    public String getSmsFormat(){
        try {
            return mImsRadioHal.getSmsFormat();
        } catch (Exception ex) {
            Log.e(this, "Failed to getSmsFormat. Exception " + ex);
        }
        return null;
    }

    public synchronized void sendGeolocationInfo(double lat, double lon,
            Address address, Message result) {
        Log.i(this,"sendGeolocationInfo: lat: " + lat + " lon: " + lon + " address: " + address);
        final int msgId = MessageId.REQUEST_SEND_GEOLOCATION_INFO;
        final String msgIdString = msgIdToString(msgId);
        IFRequest rr = IFRequest.obtain(msgId, result);
        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }
        queueRequest(rr);

        try {
            logSolicitedRequest(rr);
            mImsRadioHal.sendGeolocationInfo(rr.mSerial, lat, lon, address);
        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, msgIdString + "to IImsRadio: Exception: " + ex);
        }
    }

    public void
    hangupConnection(int index, Message result) {
            hangupWithReason(index, null, null, false, Integer.MAX_VALUE, null, result);
    }

    public void
    hangupWithReason(int connectionId, String userUri, String confUri,
            boolean mpty, int failCause, String errorInfo, Message result) {
        final int msgId = MessageId.REQUEST_HANGUP;
        final String msgIdString = msgIdToString(msgId);
        IFRequest rr = IFRequest.obtain(msgId, result);

        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        queueRequest(rr);
        try {
            logSolicitedRequest(rr);
            mImsRadioHal.hangup(rr.mSerial, connectionId, userUri, confUri, mpty, failCause,
                    errorInfo);
        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, msgIdString + "request to IImsRadio: Exception: " + ex);
        }
    }

    public void queryServiceStatus(Message result) {
        final int msgId = MessageId.REQUEST_QUERY_SERVICE_STATUS;
        final String msgIdString = msgIdToString(msgId);
        IFRequest rr = IFRequest.obtain(msgId, result);

        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        queueRequest(rr);
        try {
            logSolicitedRequest(rr);
            mImsRadioHal.queryServiceStatus(rr.mSerial);
        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, "Serivce status query request to IImsRadio: Exception: " + ex);
        }
    }

    public void setServiceStatus(Message result, ArrayList<CapabilityStatus> capabilityStatusList,
            int restrictCause) {
        final int msgId = MessageId.REQUEST_SET_SERVICE_STATUS;
        final String msgIdString = msgIdToString(msgId);

        if (mImsRadioHal.isFeatureSupported(Feature.CONSOLIDATED_SET_SERVICE_STATUS)) {
            IFRequest rr = IFRequest.obtain(msgId, result);
            if (sendErrorOnImsRadioDown(rr, msgIdString, capabilityStatusList)) {
                return;
            }
            /* Pack required information into SomeArgs.
             * This data will be retrieved in corresponding response callback.
             * Note that we are also packing Message object passed to
             * this function which will be used to reply back to the sender.
             */
            SomeArgs setCapArgs = SomeArgs.obtain();
            // pack original message
            setCapArgs.arg1 = Message.obtain(result);
            // pack the capability list
            setCapArgs.arg2 = capabilityStatusList;
            // create a new message such that it holds setCapArgs object
            Message newMsg = Message.obtain();
            newMsg.obj = setCapArgs;

            /* We are ready to send the request to RIL. Set new msg as the result
             * in IFRequest and in the corresponding response callback, data from
             * new message will be retrived and will be used to reply back to sender.
             */
            rr.setResult(newMsg);
            queueRequest(rr);
            logSolicitedRequest(rr);
            Log.i(this, msgIdString + " to ImsRadio: token -" + rr.mSerial +
                    " RestrictCause:" + restrictCause);
            try {
                mImsRadioHal.setServiceStatus(rr.mSerial, capabilityStatusList, restrictCause);
            } catch (Exception ex) {
                // replace with original message that sender is expecting
                rr.setResult(result);
                removeFromQueueAndSendResponse(rr.mSerial, capabilityStatusList);
                Log.e(this, "SetServiceStatus request to IImsRadio: Exception: " + ex);
            }
        } else {
            for (CapabilityStatus capabilityStatus : capabilityStatusList) {
                IFRequest rr = IFRequest.obtain(msgId, result);
                if (sendErrorOnImsRadioDown(rr, msgIdString, capabilityStatusList)) {
                    return;
                }

                /* Pack required information into SomeArgs.
                 * This data will be retrieved in corresponding response callback.
                 * Note that we are also packing Message object passed to
                 * this function which will be used to reply back to the sender.
                 */
                SomeArgs setCapArgs = SomeArgs.obtain();
                // pack original message
                setCapArgs.arg1 = Message.obtain(result);

                // create a list containing only the capability that we are sending to lower layers
                ArrayList<CapabilityStatus> newCapabilityStatusList = new ArrayList<>();
                newCapabilityStatusList.add(capabilityStatus);
                // pack the new capability list
                setCapArgs.arg2 = newCapabilityStatusList;

                // create a new message such that it holds setCapArgs object
                Message newMsg = Message.obtain();
                newMsg.obj = setCapArgs;

                /* We are ready to send the request to RIL. Set new msg as the result
                 * in IFRequest and in the corresponding response callback, data from
                 * new message will be retrived and will be used to reply back to sender.
                 */
                rr.setResult(newMsg);
                queueRequest(rr);
                Log.v(this, "SetServiceStatus capabilityStatus: " + capabilityStatus);
                logSolicitedRequest(rr);
                Log.i(this, msgIdString + " to ImsRadio: token -" + rr.mSerial);

                try {
                    mImsRadioHal.setServiceStatus(rr.mSerial, newCapabilityStatusList,
                            restrictCause);
                } catch (Exception ex) {
                    // replace with original message that sender is expecting
                    rr.setResult(result);
                    removeFromQueueAndSendResponse(rr.mSerial, capabilityStatusList);
                    Log.e(this, "SetServiceStatus request to IImsRadio: Exception: " + ex);
                    // message object will be recycled once response is sent to target.
                    // So return from here to avoid null pointer exception.
                    return;
                }
            }
        }
    }

    public void getImsRegistrationState(Message result) {
        final int msgId = MessageId.REQUEST_IMS_REGISTRATION_STATE;
        final String msgIdString = msgIdToString(msgId);
        IFRequest rr = IFRequest.obtain(msgId, result);

        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        queueRequest(rr);
        try {
            logSolicitedRequest(rr);
            mImsRadioHal.getImsRegistrationState(rr.mSerial);
        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, msgIdString + " request to IImsRadio: Exception: " + ex);
        }
    }

    public void sendImsRegistrationState(int imsRegState, Message result) {
        final int msgId = MessageId.REQUEST_IMS_REG_STATE_CHANGE;
        final String msgIdString = msgIdToString(msgId);
        IFRequest rr = IFRequest.obtain(msgId, result);

        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        queueRequest(rr);
        try {
            logSolicitedRequest(rr);
            Log.i(this, msgIdString + " request to ImsRadio - token:" + rr.mSerial + " RegState" +
                    imsRegState);
            mImsRadioHal.requestRegistrationChange(rr.mSerial, imsRegState);
        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, msgIdString + " request to IImsRadio: Exception: " + ex);
        }
   }

    public void modifyCallInitiate(Message result, CallModify callModify) {
        logv("modifyCallInitiate callModify= " + callModify);
        final int msgId = MessageId.REQUEST_MODIFY_CALL_INITIATE;
        final String msgIdString = msgIdToString(msgId);
        IFRequest rr = IFRequest.obtain(msgId, result);

        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        queueRequest(rr);
        try {
            logSolicitedRequest(rr);
            mImsRadioHal.modifyCallInitiate(rr.mSerial, callModify);
        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, msgIdString + "request to IImsRadio: Exception: " + ex);
        }
    }

    public void cancelModifyCall(Message result, int callId) {
        logv("cancelModifyCall");
        final int msgId = MessageId.REQUEST_CANCEL_MODIFY_CALL;
        final String msgIdString = msgIdToString(msgId);
        IFRequest rr = IFRequest.obtain(msgId, result);

        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        queueRequest(rr);
        try {
            logSolicitedRequest(rr);
            Log.i(this, msgIdString + " request to ImsRadio - " + rr.mSerial + " callId:" +
                    callId);
            mImsRadioHal.cancelModifyCall(rr.mSerial, callId);
        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, msgIdString + "request to IImsRadio: Exception: " + ex);
        }
    }

    public void modifyCallConfirm(Message result, CallModify callModify) {
        logv("modifyCallConfirm callModify= " + callModify);
        final int msgId = MessageId.REQUEST_MODIFY_CALL_CONFIRM;
        final String msgIdString = msgIdToString(msgId);
        IFRequest rr = IFRequest.obtain(msgId, result);

        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        queueRequest(rr);
        try {
            logSolicitedRequest(rr);
            mImsRadioHal.modifyCallConfirm(rr.mSerial, callModify);
        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, msgIdString + "request to IImsRadio: Exception: " + ex);
        }
    }

    public void hold(Message result, int callId) {
        final int msgId = MessageId.REQUEST_HOLD;
        final String msgIdString = msgIdToString(msgId);
        IFRequest rr = IFRequest.obtain(msgId, result);

        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        queueRequest(rr);
        try {
            logSolicitedRequest(rr);
            Log.i(this, msgIdString + " request to ImsRadio - " + rr.mSerial + " callId:" +
                    callId);
            mImsRadioHal.hold(rr.mSerial, callId);
        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, msgIdString + " request to IImsRadio: Exception: " + ex);
        }
    }

    public void resume(Message result, int callId) {
        final int msgId = MessageId.REQUEST_RESUME;
        final String msgIdString = msgIdToString(msgId);
        IFRequest rr = IFRequest.obtain(msgId, result);

        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        queueRequest(rr);
        try {
            logSolicitedRequest(rr);
            Log.i(this, msgIdString + " request to ImsRadio - " + rr.mSerial + " callId:" +
                    callId);
            mImsRadioHal.resume(rr.mSerial, callId);
        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, msgIdString + " request to IImsRadio: Exception: " + ex);
        }
    }

    public void conference(Message result) {
        final int msgId = MessageId.REQUEST_CONFERENCE;
        final String msgIdString = msgIdToString(msgId);
        IFRequest rr = IFRequest.obtain(msgId, result);

        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        queueRequest(rr);
        try {
            logSolicitedRequest(rr);
            mImsRadioHal.conference(rr.mSerial);
        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, msgIdString + "request to IImsRadio: Exception: " + ex);
        }
    }

    public void abortConference(Message result, int conferenceAbortReason) {
        final int msgId = MessageId.REQUEST_ABORT_CONFERENCE;
        final String msgIdString = msgIdToString(msgId);
        IFRequest rr = IFRequest.obtain(msgId, result);

        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        if (!isFeatureSupported(Feature.CONCURRENT_CONFERENCE_EMERGENCY_CALL)) {
            sendResponse(rr, ImsErrorCode.REQUEST_NOT_SUPPORTED, null);
            return;
        }

        queueRequest(rr);
        try {
            logSolicitedRequest(rr);
            mImsRadioHal.abortConference(rr.mSerial, conferenceAbortReason);
        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, msgIdString + "request to IImsRadio: Exception: " + ex);
        }
    }

    public void explicitCallTransfer(int srcCallId, int type, String number,
            int destCallId, Message result) {
        logv("explicitCallTransfer srcCallId= " + srcCallId + " type= "+ type + " number= "+
                number + "destCallId = " + destCallId);
        final int msgId = MessageId.REQUEST_EXPLICIT_CALL_TRANSFER;
        final String msgIdString = msgIdToString(msgId);
        IFRequest rr = IFRequest.obtain(msgId, result);

        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        queueRequest(rr);
        try {
            logSolicitedRequest(rr);
            mImsRadioHal.explicitCallTransfer(rr.mSerial, srcCallId, type, number,
                    destCallId);
        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, msgIdString + "request to IImsRadio: Exception: " + ex);
        }
    }

    public void sendConfigRequest(int requestType, int item, boolean boolValue,
        int intValue, String strValue, int errorCause, Message result) {
        final String msgIdString = msgIdToString(requestType);
        IFRequest rr = IFRequest.obtain(requestType, result);

        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        queueRequest(rr);
        try {
            logSolicitedRequest(rr);
            Log.i(this, msgIdString + " request to ImsRadio: token " + rr.mSerial +
                    " request type: " + requestType);
            if (requestType == MessageId.REQUEST_GET_IMS_CONFIG) {
                mImsRadioHal.getConfig(rr.mSerial, item, boolValue, intValue, strValue, errorCause);
            } else {
                mImsRadioHal.setConfig(rr.mSerial, item, boolValue, intValue, strValue, errorCause);
            }
        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, msgIdString + " request to IImsRadio: Exception: " + ex);
        }
    }

    public void sendDtmf(int callId, char c, Message result) {
        final int msgId = MessageId.REQUEST_DTMF;
        final String msgIdString = msgIdToString(msgId);
        IFRequest rr = IFRequest.obtain(msgId, result);

        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        queueRequest(rr);
        try {
            logSolicitedRequest(rr);
            mImsRadioHal.sendDtmf(rr.mSerial, callId, c);
        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, msgIdString + "to IImsRadio: Exception: " + ex);
        }
    }

    public void startDtmf(int callId, char c, Message result) {
        final int msgId = MessageId.REQUEST_DTMF_START;
        final String msgIdString = msgIdToString(msgId);
        IFRequest rr = IFRequest.obtain(msgId, result);

        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        queueRequest(rr);
        try {
            logSolicitedRequest(rr);
            mImsRadioHal.startDtmf(rr.mSerial, callId, c);
        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, msgIdToString(msgId) + "to IImsRadio: Exception: " + ex);
        }
    }

    public void stopDtmf(int callId, Message result) {
        final int msgId = MessageId.REQUEST_DTMF_STOP;
        final String msgIdString = msgIdToString(msgId);
        IFRequest rr = IFRequest.obtain(msgId, result);

        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        queueRequest(rr);
        try {
            logSolicitedRequest(rr);
            mImsRadioHal.stopDtmf(rr.mSerial, callId);
        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, msgIdToString(msgId) + "to IImsRadio: Exception: " + ex);
        }
    }

    private void disableSrvStatus() {
        Log.v(this, "disableSrvStatus");
        if (mSrvStatusRegistrations != null) {
            ImsRilException ex = new ImsRilException(ImsErrorCode.RADIO_NOT_AVAILABLE, null);
            mSrvStatusRegistrations
                    .notifyRegistrants(new AsyncResult(null, null, ex));
        }
    }

    public void setSuppServiceNotifications(boolean enable, Message result) {
        logv("setSuppServiceNotifications enable = " + enable);
        final int msgId = MessageId.REQUEST_SET_SUPP_SVC_NOTIFICATION;
        final String msgIdString = msgIdToString(msgId);

        IFRequest rr = IFRequest.obtain(msgId, result);

        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        queueRequest(rr);
        try {
            logSolicitedRequest(rr);
            mImsRadioHal.setSuppServiceNotification(rr.mSerial, enable);
        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, msgIdString + "request to IImsRadio: Exception: " + ex);
        }
    }

    public void getCLIR(Message result) {
        logv("getCLIR");
        final int msgId = MessageId.REQUEST_GET_CLIR;
        final String msgIdString = msgIdToString(msgId);
        IFRequest rr = IFRequest.obtain(msgId, result);

        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        queueRequest(rr);
        try {
            logSolicitedRequest(rr);
            mImsRadioHal.getClir(rr.mSerial);
        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, "GetClir request to IImsRadio: Exception: " + ex);
        }
    }

    public void setCLIR(int clirMode, Message result) {
        logv("setCLIR clirmode = " + clirMode);
        final int msgId = MessageId.REQUEST_SET_CLIR;
        final String msgIdString = msgIdToString(msgId);
        IFRequest rr = IFRequest.obtain(msgId, result);

        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        queueRequest(rr);
        try {
            logSolicitedRequest(rr);
            mImsRadioHal.setClir(rr.mSerial, clirMode);
        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, msgIdToString(msgId) + "to IImsRadio: Exception: " + ex);
        }
    }

    public void queryCallWaiting(int serviceClass, Message response) {
        logv("queryCallWaiting serviceClass = " + serviceClass);
        final int msgId = MessageId.REQUEST_QUERY_CALL_WAITING;
        final String msgIdString = msgIdToString(msgId);

        IFRequest rr = IFRequest.obtain(msgId, response);

        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        queueRequest(rr);
        try {
            logSolicitedRequest(rr);
            mImsRadioHal.getCallWaiting(rr.mSerial, serviceClass);
        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, msgIdString + "request to IImsRadio: Exception: " + ex);
        }
    }

    public void setCallWaiting(boolean enable, int serviceClass,
            Message response) {
        logv("setCallWaiting enable = " + enable + "serviceClass = "
                + serviceClass);
        final int msgId = MessageId.REQUEST_SET_CALL_WAITING;
        final String msgIdString = msgIdToString(msgId);

        IFRequest rr = IFRequest.obtain(msgId, response);

        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        queueRequest(rr);
        try {
            logSolicitedRequest(rr);
            mImsRadioHal.setCallWaiting(rr.mSerial, enable, serviceClass);
        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, msgIdString + "request to IImsRadio: Exception: " + ex);
        }
    }

    public void queryIncomingCallBarring(String facility, int serviceClass, Message response) {
        suppSvcStatus(SuppSvcResponse.QUERY, facilityStringToInt(facility),
                             null, serviceClass, response);
    }

    public void setIncomingCallBarring(int operation, String facility, String[] icbNum,
            int serviceClass, Message response) {
        suppSvcStatus(operation, facilityStringToInt(facility), icbNum, serviceClass, response);
    }

    public void setCallForward(int action, int cfReason, int serviceClass,
            String number, int timeSeconds, Message response) {
        logv("setCallForward cfReason= " + cfReason + " serviceClass = "
                + serviceClass + "number = " + number + "timeSeconds = "
                + timeSeconds);
        setCallForwardInternal(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE,
                Integer.MAX_VALUE, action, cfReason, serviceClass, number, timeSeconds, response);
    }

    public void setCallForwardUncondTimer(int startHour, int startMinute, int endHour,
            int endMinute, int action, int cfReason, int serviceClass, String number,
            Message response) {
        setCallForwardInternal(startHour, startMinute, endHour, endMinute, action, cfReason,
                serviceClass, number, ZERO_SECONDS, response);
    }

    private void setCallForwardInternal(int startHour, int startMinute, int endHour,
            int endMinute, int action, int cfReason, int serviceClass, String number,
            int timeSeconds, Message response) {
        logv("setCallForwardInternal cfReason= " + cfReason + " serviceClass = " +
                serviceClass + "number = " + number + "startHour = " + startHour +
                "startMinute = " + startMinute + "endHour = " + endHour + "endMin = " +
                endMinute);
        final int msgId = MessageId.REQUEST_SET_CALL_FORWARD_STATUS;
        final String msgIdString = msgIdToString(msgId);
        IFRequest rr = IFRequest.obtain(msgId, response);

        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        queueRequest(rr);
        try {
            logSolicitedRequest(rr);
            mImsRadioHal.setCallForwardStatus(rr.mSerial, startHour, startMinute, endHour,
                    endMinute, action, cfReason, serviceClass, number, timeSeconds);
        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, msgIdString + "to IImsRadio: Exception: " + ex);
        }
    }

    public void queryCallForwardStatus(int cfReason, int serviceClass,
            String number, Message response) {
        queryCallForwardStatus(cfReason, serviceClass, number, response,
                false /*expectMore*/);
    }

    public void queryCallForwardStatus(int cfReason, int serviceClass,
            String number, Message response, boolean expectMore) {
        logv("queryCallForwardStatus cfReason= " + cfReason
                + " serviceClass = " + serviceClass + "number = " + number
                + " expectMore = " + expectMore);
        final int msgId = MessageId.REQUEST_QUERY_CALL_FORWARD_STATUS;
        final String msgIdString = msgIdToString(msgId);
        IFRequest rr = IFRequest.obtain(msgId, response);

        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        queueRequest(rr);
        try {
            logSolicitedRequest(rr);
            mImsRadioHal.queryCallForwardStatus(rr.mSerial, cfReason, serviceClass, number,
                    expectMore);
        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, msgIdString + "to IImsRadio: Exception: " + ex);
        }
    }

    public void queryCLIP(Message response) {
        logv("queryClip");
        final int msgId = MessageId.REQUEST_QUERY_CLIP;
        final String msgIdString = msgIdToString(msgId);
        IFRequest rr = IFRequest.obtain(msgId, response);

        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        queueRequest(rr);
        try {
            logSolicitedRequest(rr);
            mImsRadioHal.getClip(rr.mSerial);
        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, "GetClip request to IImsRadio: Exception: " + ex);
        }
    }

    public void setUiTTYMode(int uiTtyMode, Message response) {
        logv("setUiTTYMode uittyMode=" + uiTtyMode);
        final int msgId = MessageId.REQUEST_SEND_UI_TTY_MODE;
        final String msgIdString = msgIdToString(msgId);
        IFRequest rr = IFRequest.obtain(msgId, response);

        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        queueRequest(rr);
        try {
            logSolicitedRequest(rr);
            mImsRadioHal.setUiTtyMode(rr.mSerial, uiTtyMode);
        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, msgIdToString(msgId) + "to IImsRadio: Exception: " + ex);
        }
    }

    public void exitEmergencyCallbackMode(Message response) {
        logv("exitEmergencyCallbackMode");
        final int msgId = MessageId.REQUEST_EXIT_EMERGENCY_CALLBACK_MODE;
        final String msgIdString = msgIdToString(msgId);
        IFRequest rr = IFRequest.obtain(msgId, response);

        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        queueRequest(rr);
        try {
            logSolicitedRequest(rr);
            mImsRadioHal.exitEmergencyCallbackMode(rr.mSerial);
        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, msgIdString + "request to IImsRadio: Exception: " + ex);
        }
    }

    public void exitScbm(Message response) {
        logv("exitScbm");
        final int msgId = MessageId.REQUEST_EXIT_SCBM;
        final String msgIdString = msgIdToString(msgId);
        IFRequest rr = IFRequest.obtain(msgId, response);

        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        if (!isFeatureSupported(Feature.EXIT_SCBM)) {
            sendResponse(rr, ImsErrorCode.REQUEST_NOT_SUPPORTED, null);
            return;
        }

        queueRequest(rr);
        try {
            logSolicitedRequest(rr);
            mImsRadioHal.exitSmsCallBackMode(rr.mSerial);
        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, msgIdString + "request to IImsRadio: Exception: " + ex);
        }
    }

    @Override
    public void queryFacilityLock(String facility, String password,
            int serviceClass, Message response) {
        suppSvcStatus(SuppSvcResponse.QUERY, facilityStringToInt(facility), response);
    }

    @Override
    public void setFacilityLock(String facility, boolean lockState,
            String password, int serviceClass, Message response) {
        int operation = lockState ? SuppSvcResponse.ACTIVATE : SuppSvcResponse.DEACTIVATE;
        suppSvcStatus(operation, facilityStringToInt(facility), response);
    }

    public void getSuppSvc(String facility, Message response) {
        suppSvcStatus(SuppSvcResponse.QUERY, facilityStringToInt(facility), response);
    }

    public void setSuppSvc(String facility, boolean lockState, Message response) {
        int operation = lockState ? SuppSvcResponse.ACTIVATE : SuppSvcResponse.DEACTIVATE;
        suppSvcStatus(operation, facilityStringToInt(facility), response);
    }

    public void suppSvcStatus(int operationType, int facility, String[] icbNum,
            int serviceClassValue, Message response) {
        logv("suppSvcStatus operationType = " + operationType + " facility = "
                + facility + "serviceClassValue = " + serviceClassValue);
        suppSvcStatusInternal(operationType, facility, icbNum, null, serviceClassValue, response);
    }

    public void suppSvcStatus(int operationType, int facility, Message response) {
        logv("suppSvcStatus operationType = " + operationType + " facility = "
                + facility);
        suppSvcStatusInternal(operationType, facility, null, null, Integer.MAX_VALUE, response);
    }

    public void suppSvcStatus(int operationType, int facility, String[] icbNum, String password,
            int serviceClassValue, Message response) {
        logv("suppSvcStatus operationType = " + operationType + " facility = " + facility
                + "serviceClassValue = " + serviceClassValue);
        suppSvcStatusInternal(operationType, facility, icbNum, password, serviceClassValue,
                response);
    }

    private void suppSvcStatusInternal(int operationType, int facility, String[] inCbNumList,
            String password, int serviceClass, Message response) {
        suppSvcStatus(operationType, facility, inCbNumList, password, serviceClass, response,
                false /*expectMore*/);
    }

    public void suppSvcStatus(int operationType, int facility, String[] inCbNumList,
            String password, int serviceClass, Message response, boolean expectMore) {
        final int msgId = MessageId.REQUEST_SUPP_SVC_STATUS;
        final String msgIdString = msgIdToString(msgId);
        IFRequest rr = IFRequest.obtain(msgId, response);

        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        queueRequest(rr);
        try {
            logSolicitedRequest(rr);
            mImsRadioHal.suppServiceStatus(rr.mSerial, operationType, facility, inCbNumList,
                    password, serviceClass, expectMore);
        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, msgIdString + "to IImsRadio: Exception: " + ex);
        }
    }

    public void getCOLR(Message result) {
        logv("getCOLR");
        final int msgId = MessageId.REQUEST_GET_COLR;
        final String msgIdString = msgIdToString(msgId);
        IFRequest rr = IFRequest.obtain(msgId, result);

        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        queueRequest(rr);
        try {
            logSolicitedRequest(rr);
            mImsRadioHal.getColr(rr.mSerial);
        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, "GetColr request to IImsRadio: Exception: " + ex);
        }
    }

    public void setCOLR(int presentationValue, Message result) {
        logv("setCOLR presentationValue = " + presentationValue);
        final int msgId = MessageId.REQUEST_SET_COLR;
        final String msgIdString = msgIdToString(msgId);
        IFRequest rr = IFRequest.obtain(msgId, result);

        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        queueRequest(rr);
        try {
            logSolicitedRequest(rr);
            mImsRadioHal.setColr(rr.mSerial, presentationValue);
        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, msgIdToString(msgId) + "to IImsRadio: Exception: " + ex);
        }
    }

    static int facilityStringToInt(String sc) {
        if (sc == null) {
            throw new RuntimeException ("invalid supplementary service");
        }

        if (sc.equals("CLIP")) {
            return SuppSvcResponse.FACILITY_CLIP;
        }
        else if (sc.equals("COLP")) {
            return SuppSvcResponse.FACILITY_COLP;
        }
        return 0;
    }

    public void getPacketCount(Message response) {
        logv("getPacketCount");
        final int msgId = MessageId.REQUEST_GET_RTP_STATISTICS;
        final String msgIdString = msgIdToString(msgId);
        IFRequest rr = IFRequest.obtain(msgId, response);

        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        queueRequest(rr);
        try {
            logSolicitedRequest(rr);
            mImsRadioHal.getRtpStatistics(rr.mSerial);
        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, msgIdString + "request to IImsRadio: Exception: " + ex);
        }
    }

    public void getPacketErrorCount(Message response) {
        logv("getPacketErrorCount");
        final int msgId = MessageId.REQUEST_GET_RTP_ERROR_STATISTICS;
        final String msgIdString = msgIdToString(msgId);
        IFRequest rr = IFRequest.obtain(msgId, response);

        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        queueRequest(rr);
        try {
            logSolicitedRequest(rr);
            mImsRadioHal.getRtpErrorStatistics(rr.mSerial);
        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, msgIdString + "request to IImsRadio: Exception: " + ex);
        }
    }

    public void getImsSubConfig(Message response) {
        logv("getImsSubConfig");
        final int msgId = MessageId.REQUEST_GET_IMS_SUB_CONFIG;
        final String msgIdString = msgIdToString(msgId);
        IFRequest rr = IFRequest.obtain(msgId, response);

        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        queueRequest(rr);
        try {
            logSolicitedRequest(rr);
            mImsRadioHal.getImsSubConfig(rr.mSerial);
        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, msgIdString + "request to IImsRadio: Exception: " + ex);
        }
    }

    public void queryMultiSimVoiceCapability(Message response) {
        logv("queryMultiSimVoiceCapability");
        final int msgId = MessageId.REQUEST_QUERY_MULTI_SIM_VOICE_CAPABILITY;
        final String msgIdString = msgIdToString(msgId);
        IFRequest rr = IFRequest.obtain(msgId, response);

        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        if (!isFeatureSupported(Feature.MULTI_SIM_VOICE_CAPABILITY)) {
            sendResponse(rr, ImsErrorCode.REQUEST_NOT_SUPPORTED, null);
            return;
        }

        queueRequest(rr);
        try {
            logSolicitedRequest(rr);
            mImsRadioHal.queryMultiSimVoiceCapability(rr.mSerial);
        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, msgIdString + "request to IImsRadio: Exception: " + ex);
        }
    }

    public void getWifiCallingPreference(Message response) {
        Log.i(this, "getWifiCallingPreference : Not supported");
    }

    public void setWifiCallingPreference(int wifiCallingStatus, int wifiCallingPreference,
            Message response) {
        Log.i(this, "setWifiCallingPreference : Not supported");
    }

    public void getHandoverConfig(Message response) {
        Log.i(LOG_TAG, "getHandoverConfig : Not supported");
    }

    public void setHandoverConfig(int hoConfig, Message response) {
        Log.i(LOG_TAG, "setHandoverConfig : Not supported");
    }

    public void queryVopsStatus(Message response) {
        Log.i(this, "queryVopsStatus : Not supported");
    }

    public void querySsacStatus(Message response) {
        Log.i(this, "querySsacStatus : Not supported");
    }

    public void updateVoltePref(int preference, Message response) {
        Log.i(this, "updateVoltePref : Not supported");
    }

    public void queryVoltePref(Message response) {
        Log.i(this, "queryVoltePref : Not supported");
    }

    /* ImsPhoneCommandsInterface API's */
    public void rejectCall(Message result) {
        Log.i(this, "rejectCall : Not supported");
    }

    public void
    getLastCallFailCause(Message result) {
        Log.i(this, "getLastCallFailCause : Not supported");
    }

    public void hangupWaitingOrBackground(Message result) {
        Log.i(this, "hangupWaitingOrBackground : Not supported");
    }

    public void getCurrentCalls(Message result) {
        Log.i(this, "getCurrentCalls : Not supported");
    }

    public void switchWaitingOrHoldingAndActive(Message result) {
        Log.i(this, "switchWaitingOrHoldingAndActive : Not supported");
    }

    public void explicitCallTransfer(Message result) {
        Log.i(this, "explicitCallTransfer : Not supported");
    }

    public void hangupForegroundResumeBackground(Message result) {
        Log.i(this, "hangupForegroundResumeBackground : Not supported");
    }


    public void registerForRttMessage(Handler h, int what, Object obj) {
        mRttMessageRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForRttMessage(Handler h) {
        mRttMessageRegistrants.remove(h);
    }

    public void registerForVoiceInfo(Handler h, int what, Object obj) {
        mVoiceInfoStatusRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForVoiceInfo(Handler h) {
        mVoiceInfoStatusRegistrants.remove(h);
    }

    public void sendRttMessage(String message, Message response) {
        Log.i(this, "RTT: sendRttMessage msg = " + message);
        final int msgId = MessageId.REQUEST_SEND_RTT_MSG;
        final String msgIdString = msgIdToString(msgId);
        IFRequest rr = IFRequest.obtain(msgId, response);

        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        queueRequest(rr);
        try {
            logSolicitedRequest(rr);
            mImsRadioHal.sendRttMessage(rr.mSerial, message);
        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, msgIdToString(msgId) + "to IImsRadio: Exception: " + ex);
        }
    }

    public void queryVirtualLineInfo(String msisdn, Message response) {
        Log.i(this, "queryVirtualLineInfo = " + msisdn);
        final int msgId = MessageId.REQUEST_QUERY_VIRTUAL_LINE_INFO;
        final String msgIdString = msgIdToString(msgId);
        IFRequest rr = IFRequest.obtain(msgId, response);

        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }
        queueRequest(rr);
        try {
            logSolicitedRequest(rr);
            mImsRadioHal.queryVirtualLineInfo(rr.mSerial, msisdn);
        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, msgIdToString(msgId) + "to IImsRadio V1.4: Exception: " + ex);
        }
    }

    public void requestMultiIdentityLinesRegistration(Collection<MultiIdentityLineInfo> linesInfo,
            Message response) {
        Log.i(this, "registerMultiIdentityLines = " + linesInfo);
        final int msgId = MessageId.REQUEST_REGISTER_MULTI_IDENTITY_LINES;
        final String msgIdString = msgIdToString(msgId);
        IFRequest rr = IFRequest.obtain(msgId, response);

        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        queueRequest(rr);
        try {
            logSolicitedRequest(rr);
            mImsRadioHal.registerMultiIdentityLines(rr.mSerial, linesInfo);
        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, msgIdToString(msgId) + "to IImsRadio: Exception: " + ex);
        }
    }

    public void setMediaConfigurationRequest(Point screenSize, Point avcSize, Point hevcSize,
            Message result) {
        final int msgId = MessageId.REQUEST_SET_MEDIA_CONFIG;
        final String msgIdString = msgIdToString(msgId);
        IFRequest rr = IFRequest.obtain(msgId, result);
        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        if (!isFeatureSupported(Feature.SET_MEDIA_CONFIG)) {
            sendResponse(rr, ImsErrorCode.REQUEST_NOT_SUPPORTED, null);
            return;
        }

        queueRequest(rr);
        try {
            logSolicitedRequest(rr);
            mImsRadioHal.setMediaConfiguration(rr.mSerial, screenSize, avcSize, hevcSize);
        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, msgIdToString(msgId) + "to IImsRadio: Exception: " + ex);
        }

    }

    /**
     * Send VOS Support Status to RIL
     * @param isVosSupported for Vos Support request
     * Response will be received by sendVosSupportStatusResponse()
     */
    public void sendVosSupportStatus(boolean isVosSupported, Message result) {
        final int msgId = MessageId.REQUEST_SEND_VOS_SUPPORT_STATUS;
        final String msgIdString = msgIdToString(msgId);
        IFRequest rr = IFRequest.obtain(msgId, result);
        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        if (!isFeatureSupported(Feature.VIDEO_ONLINE_SERVICE)) {
            sendResponse(rr, ImsErrorCode.REQUEST_NOT_SUPPORTED, null);
            return;
        }

        queueRequest(rr);
        try {
            logSolicitedRequest(rr);
            mImsRadioHal.sendVosSupportStatus(rr.mSerial, isVosSupported);
        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, msgIdToString(msgId) + "to IImsRadio: Exception: " + ex);
        }

    }

    /**
     * Send VOS Action Info to RIL
     * @param vosActionInfo for Vos Action request
     * Response will be received by sendVosActionInfoResponse()
     */
    public void sendVosActionInfo(VosActionInfo vosActionInfo, Message result) {
        final int msgId = MessageId.REQUEST_SEND_VOS_ACTION_INFO;
        final String msgIdString = msgIdToString(msgId);
        IFRequest rr = IFRequest.obtain(msgId, result);
        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        if (!isFeatureSupported(Feature.VIDEO_ONLINE_SERVICE)) {
            sendResponse(rr, ImsErrorCode.REQUEST_NOT_SUPPORTED, null);
            return;
        }

        queueRequest(rr);
        try {
            logSolicitedRequest(rr);
            mImsRadioHal.sendVosActionInfo(rr.mSerial, vosActionInfo);
        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, msgIdToString(msgId) + "to IImsRadio: Exception: " + ex);
        }

    }

    /**
     * Send enable glasses free 3d video capability request to RIL
     * @param enable3DVideo informs device supports glasses free 3d video,
     * modem will use this flag to negotiate with network during MO call setup.
     * Response will be received by setGlassesFree3dVideoCapabilityResponse()
     */
    public void setGlassesFree3dVideoCapability(boolean enable3dVideo, Message result) {
        Log.i(this, "setGlassesFree3dVideoCapability = " + enable3dVideo);
        final int msgId = MessageId.REQUEST_SET_GLASSES_FREE_3D_VIDEO_CAPABILITY;
        final String msgIdString = msgIdToString(msgId);
        IFRequest rr = IFRequest.obtain(msgId, result);
        if (sendErrorOnImsRadioDown(rr, msgIdString)) {
            return;
        }

        if (!isFeatureSupported(Feature.GLASSES_FREE_3D_VIDEO)) {
            sendResponse(rr, ImsErrorCode.REQUEST_NOT_SUPPORTED, null);
            return;
        }

        queueRequest(rr);
        try {
            logSolicitedRequest(rr);
            mImsRadioHal.setGlassesFree3dVideoCapability(rr.mSerial, enable3dVideo);
        } catch (Exception ex) {
            removeFromQueueAndSendResponse(rr.mSerial);
            Log.e(this, msgIdToString(msgId) + "to IImsRadio: Exception: " + ex);
        }
    }

    class ImsRadioIndication implements IImsRadioIndication {

        @Override
        public void onServiceUp() {
            Log.i(this, "onServiceUp: HAL Service available");
            notifyServiceUp(IIMS_RADIO_SERVICE_NAME[mPhoneId]);
        }

        @Override
        public void onServiceDown() {
            Log.i(this, "onServiceDown: HAL Service not available");
            notifyServiceDown(IIMS_RADIO_SERVICE_NAME[mPhoneId]);
            clearRequestsList(ImsErrorCode.RADIO_NOT_AVAILABLE, false);
        }

        @Override
        public void onCallStateChanged(List<DriverCallIms> driverCallImsList) {
            Collections.sort(driverCallImsList);
            mIsUnsolCallListPresent = true;
            unsljLogRet(MessageId.UNSOL_RESPONSE_CALL_STATE_CHANGED, driverCallImsList);

            for (DriverCallIms dc : driverCallImsList) {
                // Check for an error message from the network.
                // If network sends a "Forbidden - Not authorized for service" string,
                // throw an intent. This intent is expected to be processed by OMA-DM
                // applications.
                if (dc.callFailCause.getExtraMessage() != null &&
                        dc.callFailCause.getExtraMessage().equalsIgnoreCase(
                        ImsReasonInfo.EXTRA_MSG_SERVICE_NOT_AUTHORIZED)) {
                    int subId = QtiImsExtUtils.getSubscriptionIdFromPhoneId(mContext, mPhoneId);
                    log("Throwing ACTION_FORBIDDEN_NO_SERVICE_AUTHORIZATION intent " +
                            "for subId " + subId);
                    Intent intent = new Intent(
                            ImsManager.ACTION_FORBIDDEN_NO_SERVICE_AUTHORIZATION);
                    intent.putExtra(SubscriptionManager.EXTRA_SUBSCRIPTION_INDEX, subId);
                    intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
                    mContext.sendBroadcast(intent);
                }
                if (dc.isVoicePrivacy) {
                    mVoicePrivacyOnRegistrants.notifyRegistrants();
                    log("InCall VoicePrivacy is enabled");
                } else {
                    mVoicePrivacyOffRegistrants.notifyRegistrants();
                    log("InCall VoicePrivacy is disabled");
                }
            }
            mCallStateRegistrants.notifyRegistrants(new AsyncResult(null, driverCallImsList, null));
        }

        @Override
        public void onImsSmsStatusReport(StatusReport smsStatusReport) {
            unsljLog(MessageId.UNSOL_IMS_SMS_STATUS_REPORT);

            if (mSendSmsStatusReportRegistrant != null) {
                mSendSmsStatusReportRegistrant.notifyRegistrant(new AsyncResult(null,
                        smsStatusReport, null));
            }
        }

        @Override
        public void onIncomingImsSms(IncomingSms imsSms) {
             unsljLog(MessageId.UNSOL_INCOMING_IMS_SMS);

             if (mIncomingSmsRegistrant != null) {
                 mIncomingSmsRegistrant.notifyRegistrant(new AsyncResult(null, imsSms, null));
             }
        }

        @Override
        public void onRing() {
            unsljLogRet(MessageId.UNSOL_CALL_RING, null);

            if (mRingRegistrant != null) {
                mRingRegistrant.notifyRegistrant(new AsyncResult(null, null, null));
            }
        }

        @Override
        public void onRingbackTone(boolean tone) {
            unsljLogRet(MessageId.UNSOL_RINGBACK_TONE, tone);

            if (mRingbackToneRegistrants != null) {
                mRingbackToneRegistrants.notifyRegistrants(new AsyncResult(null, tone, null));
            }
        }

        @Override
        public void onRegistrationChanged(ImsRegistrationInfo registrationInfo) {
            unsljLog(MessageId.UNSOL_RESPONSE_IMS_NETWORK_STATE_CHANGED);

            mImsNetworkStateChangedRegistrants.notifyRegistrants(
                    new AsyncResult(null, registrationInfo, null));
        }

        @Override
        public void onHandover(HoInfo hoInfo) {
            unsljLogRet(MessageId.UNSOL_RESPONSE_HANDOVER, hoInfo);
            if (hoInfo == null) {
                Log.e(this, "onHandover: hoInfo is null.");
                return ;
            }
            mHandoverStatusRegistrants.notifyRegistrants(
                    new AsyncResult(null, hoInfo, null));

        }

        @Override
        public void onServiceStatusChanged(List<ServiceStatus> srvStatusList) {
            unsljLogRet(MessageId.UNSOL_SRV_STATUS_UPDATE, srvStatusList);

            if (srvStatusList != null) {
                mSrvStatusRegistrations.notifyRegistrants(new AsyncResult(null, srvStatusList,
                        null));
            }
        }

        @Override
        public void onRadioStateChanged(RadioState radioState) {
            unsljLogRet(MessageId.UNSOL_RADIO_STATE_CHANGED, radioState);

            setRadioState(radioState);
        }

        @Override
        public void onEnterEmergencyCallBackMode() {
            unsljLog(MessageId.UNSOL_ENTER_EMERGENCY_CALLBACK_MODE);

            if (mEmergencyCallbackModeRegistrant != null) {
                mEmergencyCallbackModeRegistrant.notifyRegistrant();
            }
        }

        @Override
        public void onExitEmergencyCallBackMode() {
            unsljLog(MessageId.UNSOL_EXIT_EMERGENCY_CALLBACK_MODE);

            if (mExitEmergencyCallbackModeRegistrants != null) {
                mExitEmergencyCallbackModeRegistrants.notifyRegistrants(
                    new AsyncResult(null, null, null));
            }
        }

        @Override
        public void onTtyNotification(int[] mode) {
            unsljLogRet(MessageId.UNSOL_TTY_NOTIFICATION, mode);
            if (mode != null && mTtyStatusRegistrants != null) {
                mTtyStatusRegistrants.notifyRegistrants(new AsyncResult(null, mode, null));
            }
        }

        @Override
        public void onRefreshConferenceInfo(ConfInfo info) {
            unsljLogRet(MessageId.UNSOL_REFRESH_CONF_INFO, info);
            if (info == null) {
                Log.e(this, "onRefreshConferenceInfo: Data is null.");
                return;
            }
            mRefreshConfInfoRegistrations.notifyRegistrants(
                new AsyncResult(null, info, null));

        }

        @Override
        public void onRefreshViceInfo(ViceUriInfo viceInfo) {
            unsljLogRet(MessageId.UNSOL_REFRESH_VICE_INFO, viceInfo);
            if (viceInfo == null) {
                Log.e(this, "onRefreshViceInfo: Data is null.");
                return;
            }
            mRefreshViceInfoRegistrants
                .notifyRegistrants(new AsyncResult(null, viceInfo, null));

        }

        @Override
        public void onModifyCall(CallModify callModifyInfo) {
            unsljLogRet(MessageId.UNSOL_MODIFY_CALL, callModifyInfo);
            mModifyCallRegistrants
                    .notifyRegistrants(new AsyncResult(null, callModifyInfo, null));
        }

        @Override
        public void onSuppServiceNotification(SuppNotifyInfo suppServiceNotifInfo) {
            unsljLogRet(MessageId.UNSOL_SUPP_SVC_NOTIFICATION, suppServiceNotifInfo);

            if (mSsnRegistrant != null) {
                mSsnRegistrant.notifyRegistrant(new AsyncResult (null, suppServiceNotifInfo, null));
            }
        }

        @Override
        public void onMessageWaiting(Mwi mwiIndication) {
            unsljLogRet(MessageId.UNSOL_MWI, mwiIndication);

            if (mwiIndication == null) {
                Log.e(this, "onMessageWaiting: Data is null");
                return;
            }

            mMwiRegistrants.notifyRegistrants(new AsyncResult (null, mwiIndication, null));
        }

        @Override
        public void onGeolocationInfoRequested(GeoLocationInfo geoLocationInfo) {
            unsljLogRet(MessageId.UNSOL_REQUEST_GEOLOCATION, geoLocationInfo);
            if (geoLocationInfo == null) {
                Log.e(this, "onGeolocationInfoRequested: Null location data!");
                return;
            }
            mGeolocationRegistrants.notifyRegistrants(new
                    AsyncResult (null, geoLocationInfo, null));
        }

        @Override
        public void onIncomingCallAutoRejected(DriverCallIms driverCallIms) {
            if (driverCallIms.getCallComposerInfo() == null && driverCallIms.getEcnamInfo() == null
                    && !driverCallIms.getIsDcCall())
                unsljLogRet(MessageId.UNSOL_AUTO_CALL_REJECTION_IND, driverCallIms);
            else if (driverCallIms.getEcnamInfo() == null && !driverCallIms.getIsDcCall())
                unsljLogRet(MessageId.UNSOL_AUTO_CALL_COMPOSER_CALL_REJECTION_IND,
                        driverCallIms);
            else
                unsljLogRet(MessageId.UNSOL_INCOMING_CALL_AUTO_REJECTED, driverCallIms);
            mAutoRejectRegistrants.notifyRegistrants(new AsyncResult(null, driverCallIms, null));
        }

        @Override
        public void onImsSubConfigChanged(ImsSubConfigDetails configDetails) {
            unsljLogRet(MessageId.UNSOL_IMS_SUB_CONFIG_CHANGED, configDetails);

            if (configDetails == null) {
                Log.e(this, "onImsSubConfigChanged: Data is null.");
                return;
            }

            mImsSubConfigChangeRegistrants.
                    notifyRegistrants(new AsyncResult(null, configDetails, null));
        }

        @Override
        public void onParticipantStatusInfo(
                ParticipantStatusDetails participantStatusInfo) {
            unsljLogRet(MessageId.UNSOL_PARTICIPANT_STATUS_INFO, participantStatusInfo);
            if (participantStatusInfo == null) {
                Log.e(this, "onParticipantStatusInfo: Participant status info is null");
                return;
            }
            mParticipantStatusRegistrants
                    .notifyRegistrants(new AsyncResult(null, participantStatusInfo, null));
        }

        @Override
        public void onRegistrationBlockStatus(
                RegistrationBlockStatusInfo registrationBlockStatusInfo) {
            unsljLogRet(MessageId.UNSOL_RESPONSE_REGISTRATION_BLOCK_STATUS,
                    registrationBlockStatusInfo);

            if (registrationBlockStatusInfo == null) {
                Log.e(this, "onRegistrationBlockStatus: Data is null.");
                return;
            }

            mRegistrationBlockStatusRegistrants.notifyRegistrants(
                    new AsyncResult (null, registrationBlockStatusInfo, null));
        }

        @Override
        public void onRttMessageReceived(String msg) {
            if (msg == null) {
                Log.e(this, "onRttMessageReceived: msg is null.");
                return;
            }

            Log.v(this, "onRttMessageReceived: msg = " + msg);
            unsljLogRet(MessageId.UNSOL_RESPONSE_RTT_MSG_RECEIVED, msg);
            mRttMessageRegistrants.notifyRegistrants(new AsyncResult(null, msg, null));
        }

        @Override
        public void onVoiceInfoChanged(int voiceInfo) {
            Log.v(this, "onVoiceInfoChanged: VoiceInfo = " + voiceInfo);
            unsljLogRet(MessageId.UNSOL_VOICE_INFO, voiceInfo);
            mVoiceInfoStatusRegistrants.notifyRegistrants(new AsyncResult(null, voiceInfo, null));
        }

        @Override
        public void onVoWiFiCallQuality(int[] voWiFiCallQuality) {
            unsljLogRet(MessageId.UNSOL_VOWIFI_CALL_QUALITY, voWiFiCallQuality);
            if (mVoWiFiCallQualityRegistrants != null) {
                mVoWiFiCallQualityRegistrants.notifyRegistrants(
                        new AsyncResult (null, voWiFiCallQuality, null));
            }
        }

        @Override
        public void onSupplementaryServiceIndication(ImsSsData ssData) {
            unsljLogRet(MessageId.UNSOL_ON_SS, ssData);
            if (ssData == null) {
                Log.e(this, "onSupplementaryServiceIndication : Data is null.");
                return;
            }

            if (mSsIndicationRegistrant != null) {
                mSsIndicationRegistrant.notifyRegistrant(new AsyncResult(null, ssData, null));
            }
        }

        @Override
        public void onVopsChanged(boolean isVopsEnabled) {
            unsljLogRet(MessageId.UNSOL_VOPS_CHANGED, isVopsEnabled);
            if (mVopsRegistrants != null) {
                mVopsRegistrants.notifyRegistrants(new AsyncResult(null, isVopsEnabled, null));
            }
        }

        @Override
        public void onMultiIdentityRegistrationStatusChange(
                List<MultiIdentityLineInfo> linesInfo) {
            unsljLogRet(MessageId.UNSOL_MULTI_IDENTITY_REGISTRATION_STATUS_CHANGE, linesInfo);
            mMultiIdentityStatusChangeRegistrants.notifyRegistrants(
                    new AsyncResult(null, linesInfo, null));
        }

        @Override
        public void onMultiIdentityInfoPending() {
            unsljLogRet(MessageId.UNSOL_MULTI_IDENTITY_INFO_PENDING, null);
            mMultiIdentityInfoPendingRegistrants.notifyRegistrants(
                    new AsyncResult(null, null, null));
        }

        @Override
        public void onModemSupportsWfcRoamingModeConfiguration(
                boolean wfcRoamingConfigurationSupport) {
            unsljLogRet(MessageId.UNSOL_MODEM_SUPPORTS_WFC_ROAMING_MODE,
                    wfcRoamingConfigurationSupport);
            mWfcRoamingConfigurationSupport = wfcRoamingConfigurationSupport;
            if (mWfcRoamingModeConfigRegistrants != null) {
                mWfcRoamingModeConfigRegistrants
                        .notifyRegistrants(new AsyncResult(null, wfcRoamingConfigurationSupport,
                        null));
            }
        }

        @Override
        public void onUssdMessageFailed(UssdInfo ussdInfo) {
            unsljLogRet(MessageId.UNSOL_USSD_FAILED, ussdInfo);
            ImsRilException ex = new ImsRilException(ussdInfo.getErrorCode(),
                                                     ussdInfo.getErrorMessage());
            notifyUssdInfo(ussdInfo, ex);
        }

        @Override
        public void onUssdReceived(UssdInfo ussdInfo) {
            unsljLogRet(MessageId.UNSOL_USSD_RECEIVED, ussdInfo);
            notifyUssdInfo(ussdInfo, null /* ImsRilException */);
        }

        @Override
        public void onCallComposerInfoAvailable(
                int callId, CallComposerInfo callComposerInfo) {
            unsljLogRet(MessageId.UNSOL_CALL_COMPOSER_INFO_AVAILABLE_IND, callComposerInfo);
            mPreAlertingCallInfoRegistrants.notifyRegistrants(
                    new AsyncResult(null, new PreAlertingCallInfo(callId, callComposerInfo), null));
        }

        @Override
        public void onRetrievingGeoLocationDataStatus(int geoLocationDataStatus) {
            unsljLogRet(MessageId.UNSOL_RETRIEVE_GEO_LOCATION_DATA_STATUS, geoLocationDataStatus);
            if (mGeoLocationDataStatusRegistrants != null) {
                mGeoLocationDataStatusRegistrants.notifyRegistrants(new AsyncResult(null,
                geoLocationDataStatus, null));
            }
        }

        @Override
        public void onSipDtmfReceived(String configCode) {
            unsljLogRet(MessageId.UNSOL_SIP_DTMF_RECEIVED, configCode);
            notifySipDtmfInfo(configCode);
        }

        @Override
        public void onServiceDomainChanged(int domain) {
            unsljLogRet(MessageId.UNSOL_SERVICE_DOMAIN_CHANGED, domain);
            if (mSrvDomainChangedRegistrants != null) {
                mSrvDomainChangedRegistrants.notifyRegistrants(new AsyncResult(null,
                        domain, null));
            }
        }

        @Override
        public void onSmsCallBackModeChanged(int mode) {
            unsljLogRet(MessageId.UNSOL_SCBM_UPDATE_IND, mode);
            if (mSmsCallbackModeChangedRegistrants != null) {
                mSmsCallbackModeChangedRegistrants.notifyRegistrants(new AsyncResult(null,
                        mode, null));
            }
        }

        @Override
        public void onConferenceCallStateCompleted() {
            unsljLogRet(MessageId.UNSOL_CONFERENCE_CALL_STATE_COMPLETED, null);
            if (mConferenceCallStateCompletedRegistrants != null) {
                mConferenceCallStateCompletedRegistrants.notifyRegistrants(
                        new AsyncResult(null, null, null));
            }
        }

        @Override
        public void onIncomingDtmfStart(int callId, String dtmf) {
            unsljLogRet(MessageId.UNSOL_INCOMING_DTMF_START, dtmf);
            SomeArgs args = SomeArgs.obtain();
            args.argi1 = callId;
            args.arg1 = dtmf;
            if (mIncomingDtmfStartRegistrants != null) {
                mIncomingDtmfStartRegistrants.notifyRegistrants(
                      new AsyncResult(null, args, null));
            }
        }

        @Override
        public void onIncomingDtmfStop(int callId, String dtmf) {
            unsljLogRet(MessageId.UNSOL_INCOMING_DTMF_STOP, dtmf);
            SomeArgs args = SomeArgs.obtain();
            args.argi1 = callId;
            args.arg1 = dtmf;
            if (mIncomingDtmfStopRegistrants != null) {
                mIncomingDtmfStopRegistrants.notifyRegistrants(
                      new AsyncResult(null, args, null));
            }
        }

        @Override
        public void onMultiSimVoiceCapabilityChanged(int voiceCapability) {
            unsljLogRet(MessageId.UNSOL_MULTI_SIM_VOICE_CAPABILITY_CHANGED, voiceCapability);

            if (mMultiSimVoiceCapabilityChangedRegistrants != null) {
                mMultiSimVoiceCapabilityChangedRegistrants.
                        notifyRegistrants(new AsyncResult(null, voiceCapability, null));
            }
        }

        @Override
        public void onPreAlertingCallInfoAvailable(PreAlertingCallInfo info) {
            unsljLogRet(MessageId.UNSOL_PRE_ALERTING_CALL_INFO_AVAILABLE, info);
            mPreAlertingCallInfoRegistrants.notifyRegistrants(new AsyncResult(null, info, null));
        }

        @Override
        public void onCiWlanNotification(boolean show) {
            unsljLogRet(MessageId.UNSOL_C_IWLAN_NOTIFICATION, show);
            mCiWlanNotificationRegistrants.notifyRegistrants(new AsyncResult(null, show, null));
        }

        @Override
        public void onSrtpEncryptionInfo(int callId, int category) {
            unsljLogRet(MessageId.UNSOL_SRTP_ENCRYPTION_UPDATE, category);
            SomeArgs args = SomeArgs.obtain();
            args.argi1 = callId;
            args.argi2 = category;
            if (mSrtpEncryptionUpdateRegistrants != null) {
                mSrtpEncryptionUpdateRegistrants.notifyRegistrants(
                    new AsyncResult(null, args, null));
            }
        }
    }

    class QtiRadioConfigIndication implements IQtiRadioConfigIndication {

        @Override
        public void onServiceUp() {
            Log.i(this, "onServiceUp: HAL Service available for phoneId: " + mPhoneId);
            handleQtiRadioConfigUp();
        }

        @Override
        public void onServiceDown() {
            Log.i(this, "onServiceDown: HAL Service not available for phoneId: " + mPhoneId);
        }
    }

    class ImsRadioResponse implements IImsRadioResponse {

        @Override
        public void onDialResponse(int token, int errorCode) {
            // TODO: Map proto error codes to IImsRadio error codes to be used by the interface.
            // Change usage of errors of type ImsErrorCode.Error to some proprietary error code
            // and return that to clients.
            removeFromQueueAndSendResponse(token, errorCode);
        }

        @Override
        public void onSendImsSmsResponse(int token, SmsResponse response) {
            IFRequest rr = findAndRemoveRequestFromList(token);
            if (rr == null) {
                Log.e(this, "onSendImsSmsResponse rr is NULL");
                return;
            }
            sendResponse(rr,ImsErrorCode.SUCCESS,response);
        }

        @Override
        public void onGetConfigResponse(int token, int errorCode, Object ret) {
            IFRequest rr = findAndRemoveRequestFromList(token);
            if (rr == null) {
                Log.e(this, "onGetConfigResponse rr is NULL");
                return;
            }

            sendResponse(rr, errorCode, ret);
        }

        @Override
        public void onSetConfigResponse(int token, int errorCode, Object ret) {
            IFRequest rr = findAndRemoveRequestFromList(token);
            if (rr == null) {
                Log.e(this, "onSetConfigResponse rr is NULL");
                return;
            }

            sendResponse(rr, errorCode, ret);
        }

        @Override
        public void onQueryServiceStatusResponse(int token, int errorCode,
                List<ServiceStatus> serviceStatusInfoList) {
            IFRequest rr = findAndRemoveRequestFromList(token);
            if (rr == null) {
                Log.e(this, "onQueryServiceStatusResponse rr is NULL");
                return;
            }

            sendResponse(rr, errorCode, serviceStatusInfoList);
        }

        @Override
        public void onSetServiceStatusResponse(int token, int errorCode) {
            IFRequest rr = findAndRemoveRequestFromList(token);
            if (rr == null) {
                Log.e(this, "onSetServiceStatusResponse  rr is NULL");
                return;
            }

            try {
                SomeArgs setCapArgs = (SomeArgs)rr.mResult.obj;
                Message orgMsg = (Message)setCapArgs.arg1;
                /* On getting response from RIL, retreive the data stored in IFRequest#mResult,
                 * and reply back to sender with retreived data and with original message that
                 * sender is expecting
                 */
                rr.setResult(orgMsg);
                sendResponse(rr, errorCode, (ArrayList<CapabilityStatus>) setCapArgs.arg2);
            } catch (ClassCastException ex) {
                Log.e(this, "onSetServiceStatusResponse exception = " + ex);
            }
        }

        private void sendImsReasonInfo(int token, int errorCode, ImsReasonInfo errorInfo) {
            IFRequest rr = findAndRemoveRequestFromList(token);
            if (rr == null) {
                Log.e(this, "sendImsReasonInfo rr is NULL for token: " + token);
                return;
            }
            Log.i(this, msgIdToString(rr.mRequest) + "Response: errorCode = " + errorCode
                    + " errorInfo = " + errorInfo);
            sendResponse(rr, errorCode, errorInfo);
        }

        @Override
        public void onResumeResponse(int token, int errorCode, ImsReasonInfo errorInfo) {
            sendImsReasonInfo(token, errorCode, errorInfo);
        }

        @Override
        public void onHoldResponse(int token, int errorCode, ImsReasonInfo errorInfo) {
            sendImsReasonInfo(token, errorCode, errorInfo);
        }

        @Override
        public void onHangupResponse(int token, int errorCode) {
            removeFromQueueAndSendResponse(token, errorCode);
        }

        @Override
        public void onAnswerResponse(int token, int errorCode) {
            removeFromQueueAndSendResponse(token, errorCode);
        }

        @Override
        public void onRequestRegistrationChangeResponse(int token, int errorCode) {
            removeFromQueueAndSendResponse(token, errorCode);
        }

        @Override
        public void onGetRegistrationResponse(int token, int errorCode,
                                              ImsRegistrationInfo registration) {
            IFRequest rr = findAndRemoveRequestFromList(token);
            if (rr == null) {
                Log.e(this, "onGetRegistrationResponse rr is NULL");
                return;
            }

            sendResponse(rr, errorCode, registration);
        }

        @Override
        public void onSuppServiceStatusResponse(int token, int errorCode,
                                                SuppSvcResponse suppSvcResponse) {
            IFRequest rr = findAndRemoveRequestFromList(token);
            if (rr == null) {
                Log.e(this, "onSuppServiceStatusResponse rr is NULL");
                return;
            }

            if (rr.mRequest == MessageId.REQUEST_SET_CALL_FORWARD_STATUS) {
                CallForwardStatusInfo cfStatusInfo = new CallForwardStatusInfo(
                        suppSvcResponse.getErrorDetails(), null);
                sendResponse(rr, errorCode, cfStatusInfo);
            } else {
                sendResponse(rr, errorCode, suppSvcResponse);
            }
        }

        @Override
        public void onConferenceResponse(int token, int errorCode, ImsReasonInfo errorInfo) {
            sendImsReasonInfo(token, errorCode, errorInfo);
        }

        @Override
        public void onAbortConferenceResponse(int token, int errorCode) {
            removeFromQueueAndSendResponse(token, errorCode);
        }

        @Override
        public void onGetClipResponse(int token, int errorCode,
                SuppService clipProvisionStatus) {
            IFRequest rr = findAndRemoveRequestFromList(token);
            if (rr == null) {
                Log.e(this, "onGetClipResponse rr is NULL");
                return;
            }

            sendResponse(rr, errorCode, clipProvisionStatus);
        }

        @Override
        public void onGetClirResponse(int token, int errorCode, int[] clirInfo) {
            IFRequest rr = findAndRemoveRequestFromList(token);
            if (rr == null) {
                Log.e(this, "onGetClirResponse rr is NULL");
                return;
            }

            sendResponse(rr, errorCode, clirInfo);
        }

        @Override
        public void onQueryCallForwardStatusResponse(int token, int errorCode,
                ImsCallForwardTimerInfo timerInfo[]) {
            IFRequest rr = findAndRemoveRequestFromList(token);
            if (rr == null) {
                Log.e(this, "onQueryCallForwardStatusResponse rr is NULL");
                return;
            }

            sendResponse(rr, errorCode, timerInfo);
        }

        @Override
        public void onGetCallWaitingResponse(int token, int errorCode, int[] response) {
            IFRequest rr = findAndRemoveRequestFromList(token);
            if (rr == null) {
                Log.e(this, "onGetCallWaitingResponse rr is NULL");
                return;
            }

            sendResponse(rr, errorCode, response);
        }

        @Override
        public void onSetClirResponse(int token, int errorCode) {
            removeFromQueueAndSendResponse(token, errorCode);
        }

        @Override
        public void onGetColrResponse(int token, int errorCode, SuppService colrInfo) {
            IFRequest rr = findAndRemoveRequestFromList(token);
            if (rr == null) {
                Log.e(this, "onGetColrResponse rr is NULL");
                return;
            }

            sendResponse(rr, errorCode, colrInfo);
        }

        @Override
        public void onExitEmergencyCallbackModeResponse(int token, int errorCode) {
            removeFromQueueAndSendResponse(token, errorCode);
        }

        @Override
        public void onSendDtmfResponse(int token, int errorCode) {
            removeFromQueueAndSendResponse(token, errorCode);
        }

        @Override
        public void onStartDtmfResponse(int token, int errorCode) {
            removeFromQueueAndSendResponse(token, errorCode);
        }

        @Override
        public void onStopDtmfResponse(int token, int errorCode) {
            removeFromQueueAndSendResponse(token, errorCode);
        }

        @Override
        public void onSetUiTTYModeResponse(int token, int errorCode) {
            removeFromQueueAndSendResponse(token, errorCode);
        }

        @Override
        public void onModifyCallInitiateResponse(int token, int errorCode) {
            removeFromQueueAndSendResponse(token, errorCode);
        }

        @Override
        public void onCancelModifyCallResponse(int token, int errorCode) {
            removeFromQueueAndSendResponse(token, errorCode);
        }

        @Override
        public void onModifyCallConfirmResponse(int token, int errorCode) {
            removeFromQueueAndSendResponse(token, errorCode);
        }

        @Override
        public void onExplicitCallTransferResponse(int token, int errorCode,
                                                   ImsReasonInfo errorInfo) {
            sendImsReasonInfo(token, errorCode, errorInfo);
        }

        @Override
        public void onSetSuppServiceNotificationResponse(int token, int errorCode,
                int serviceStatusClass) {
            removeFromQueueAndSendResponse(token, errorCode);
        }

        @Override
        public void onGetRtpStatisticsResponse(int token, int errorCode,
                                               long packetCount) {
            IFRequest rr = findAndRemoveRequestFromList(token);
            if (rr == null) {
                Log.e(this, "onGetRtpStatisticsResponse rr is NULL");
                return;
            }
            sendResponse(rr, errorCode, packetCount);
        }

        @Override
        public void onGetRtpErrorStatisticsResponse(int token, int errorCode,
                                                    long packetErrorCount) {
            IFRequest rr = findAndRemoveRequestFromList(token);
            if (rr == null) {
                Log.e(this, "onGetRtpErrorStatisticsResponse rr is NULL");
                return;
            }
            sendResponse(rr, errorCode, packetErrorCount);
        }

        @Override
        public void onAddParticipantResponse(int token, int errorCode) {
            removeFromQueueAndSendResponse(token, errorCode);
        }

        @Override
        public void onDeflectCallResponse(int token, int errorCode) {
            removeFromQueueAndSendResponse(token, errorCode);
        }

        @Override
        public void onSendGeolocationInfoResponse(int token, int errorCode) {
            removeFromQueueAndSendResponse(token, errorCode);
        }

        @Override
        public void onGetImsSubConfigResponse(int token, int errorCode,
                ImsSubConfigDetails subConfigInfo) {
            IFRequest rr = findAndRemoveRequestFromList(token);
            if (rr == null) {
                Log.e(this, "onGetImsSubConfigResponse rr is NULL");
                return;
            }

            sendResponse(rr, errorCode, subConfigInfo);
        }

        @Override
        public void onSendUssdResponse(int token, int errorCode, ImsReasonInfo errorInfo) {
            sendImsReasonInfo(token, errorCode, errorInfo);
        }

        @Override
        public void onSendSipDtmfResponse(int token, int errorCode) {
            IFRequest rr = findAndRemoveRequestFromList(token);
            if (rr == null) {
                Log.e(this, "onSendSipDtmfResponse rr is NULL");
                return;
            }
            sendResponse(rr, errorCode, null);
        }

        @Override
        public void onCancelPendingUssdResponse(int token, int errorCode, ImsReasonInfo errorInfo) {
            sendImsReasonInfo(token, errorCode, errorInfo);
        }

        @Override
        public void onRegisterMultiIdentityLinesResponse(int token, int errorCode) {
            removeFromQueueAndSendResponse(token, errorCode);
        }

        @Override
        public void onSendRttMessageResponse(int token, int errorCode) {
            removeFromQueueAndSendResponse(token, errorCode);
        }

        @Override
        public void onQueryVirtualLineInfoResponse(int token, String msisdn,
                                                   VirtualLineInfo virtualLineInfo) {
            IFRequest rr = findAndRemoveRequestFromList(token);
            if (rr == null) {
                Log.e(this, "onQueryVirtualLineInfoResponse rr is NULL");
                return;
            }
            sendResponse(rr, ImsErrorCode.SUCCESS, virtualLineInfo);
        }

        @Override
        public void onSetCallForwardStatusResponse(int token, int errorCode,
                CallForwardStatusInfo callForwardStatusInfo) {
            IFRequest rr = findAndRemoveRequestFromList(token);
            if (rr == null) {
                Log.e(this, "onSetCallForwardStatusResponse rr is NULL");
                return;
            }

            Log.i(this, "onSetCallForwardStatusResponse:: " + callForwardStatusInfo);
            sendResponse(rr, errorCode, callForwardStatusInfo);
        }

        @Override
        public void onSetMediaConfigurationResponse(int token, int errorCode) {
            IFRequest rr = findAndRemoveRequestFromList(token);
            if (rr == null) {
                Log.e(this, "onSetMediaConfigurationResponse rr is NULL");
                return;
            }
            sendResponse(rr, errorCode, null);
        }

        @Override
        public void onQueryMultiSimVoiceCapabilityResponse(int token, int errorCode,
                int voiceCapability) {
            IFRequest rr = findAndRemoveRequestFromList(token);
            if (rr == null) {
                Log.e(this, "onQueryMultiSimVoiceCapabilityResponse rr is NULL");
                return;
            }
            sendResponse(rr, errorCode, voiceCapability);
        }

        @Override
        public void exitSmsCallBackModeResponse(int token, int errorCode) {
            IFRequest rr = findAndRemoveRequestFromList(token);
            if (rr == null) {
                Log.e(this, "exitSmsCallBackModeResponse rr is NULL");
                return;
            }
            sendResponse(rr, errorCode, null);
        }

        @Override
        public void onSendVosSupportStatusResponse(int token, int errorCode) {
            IFRequest rr = findAndRemoveRequestFromList(token);
            if (rr == null) {
                Log.e(this, "onSendVosSupportStatusResponse rr is NULL");
                return;
            }
            sendResponse(rr, errorCode, null);
        }

        @Override
        public void onSendVosActionInfoResponse(int token, int errorCode) {
            IFRequest rr = findAndRemoveRequestFromList(token);
            if (rr == null) {
                Log.e(this, "onSendVosActionInfoResponse rr is NULL");
                return;
            }
            sendResponse(rr, errorCode, null);
        }

        @Override
        public void onSetGlassesFree3dVideoCapabilityResponse(int token, int errorCode) {
            IFRequest rr = findAndRemoveRequestFromList(token);
            if (rr == null) {
                Log.e(this, "onSetGlassesFree3dVideoCapabilityResponse rr is NULL");
                return;
            }
            sendResponse(rr, errorCode, null);
        }
    }
}
