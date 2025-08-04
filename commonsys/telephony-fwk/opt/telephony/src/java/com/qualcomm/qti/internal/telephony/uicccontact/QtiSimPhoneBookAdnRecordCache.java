/*
 * Copyright (c) 2017 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.internal.telephony.uicccontact;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.text.TextUtils;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.uicc.AdnRecord;
import com.android.internal.telephony.uicc.IccConstants;
import com.android.internal.telephony.uicc.IccRefreshResponse;
import com.qualcomm.qti.internal.telephony.QtiRilInterface;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.List;

/**
 * {@hide}
 */
public class QtiSimPhoneBookAdnRecordCache extends Handler {
    //***** Instance Variables
    private static final String LOG_TAG = "QtiSimPhoneBookAdnRecordCache";
    private static final boolean DBG = true;
    // member variables
    protected final CommandsInterface mCi;
    protected int mPhoneId;
    protected Context mContext;
    //max adn count
    private int mAdnCount = 0;
    //valid adn count
    private int mValidAdnCount = 0;
    private int mEmailCount = 0;
    private int mValidEmailCount = 0;
    private int mAddNumCount = 0;
    private int mValidAddNumCount = 0;
    private int mMaxNameLen = 0;
    private int mMaxNumberLen = 0;
    private int mMaxEmailLen = 0;
    private int mMaxAnrLen = 0;
    private int mRecCount = 0;
    private Object mLock = new Object();
    private final ConcurrentSkipListMap<Integer, AdnRecord> mSimPbRecords =
            new ConcurrentSkipListMap<Integer, AdnRecord>();
    private boolean mRefreshAdnCache = false;
    private QtiRilInterface mQtiRilInterface;
    private String mTag;

    // People waiting for ADN-like files to be loaded
    ArrayList<Message> mAdnLoadingWaiters = new ArrayList<Message>();

    // People waiting for adn record to be updated
    Message mAdnUpdatingWaiter = null;

    //***** Event Constants
    static final int EVENT_INIT_ADN_DONE = 1;
    static final int EVENT_QUERY_ADN_RECORD_DONE = 2;
    static final int EVENT_LOAD_ADN_RECORD_DONE = 3;
    static final int EVENT_LOAD_ALL_ADN_LIKE_DONE = 4;
    static final int EVENT_UPDATE_ADN_RECORD_DONE = 5;
    static final int EVENT_SIM_REFRESH = 6;

    //***** Constructor
    public QtiSimPhoneBookAdnRecordCache(Context context,  int phoneId,  CommandsInterface ci) {
        mCi = ci;
        mPhoneId = phoneId;
        mContext = context;
        mTag = LOG_TAG + "[" + phoneId + "]";

        mQtiRilInterface = QtiRilInterface.getInstance(context);
        mQtiRilInterface.registerForAdnInitDone(this, EVENT_INIT_ADN_DONE, null);
        mCi.registerForIccRefresh(this, EVENT_SIM_REFRESH, null);

        IntentFilter intentFilter = new IntentFilter(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        context.registerReceiver(sReceiver, intentFilter);
    }

    public void reset() {
        mAdnLoadingWaiters.clear();
        clearUpdatingWriter();

        mSimPbRecords.clear();
        mRecCount = 0;
        mRefreshAdnCache = false;
    }

    private void clearUpdatingWriter() {
        sendErrorResponse(mAdnUpdatingWaiter, "QtiSimPhoneBookAdnRecordCache reset");
        mAdnUpdatingWaiter = null;
    }

    private void sendErrorResponse(Message response, String errString) {
        if (response != null) {
            Exception e = new RuntimeException(errString);
            AsyncResult.forMessage(response).exception = e;
            response.sendToTarget();
        }
    }

    private void
    notifyAndClearWaiters() {
        if (mAdnLoadingWaiters == null) {
            return;
        }

        for (int i = 0, s = mAdnLoadingWaiters.size() ; i < s ; i++) {
            Message response = mAdnLoadingWaiters.get(i);

            if ((response != null)) {
                List<AdnRecord> result = getAdnRecords();
                AsyncResult.forMessage(response, result, null);
                response.sendToTarget();
            }
        }

        mAdnLoadingWaiters.clear();
    }

    @VisibleForTesting
    public List<AdnRecord> getAdnRecords() {
        List<AdnRecord> records = new ArrayList<AdnRecord>(mSimPbRecords.values());
        return records;
    }

    public void queryAdnRecord () {
        mRecCount = 0;
        mAdnCount = 0;
        mValidAdnCount = 0;
        mEmailCount = 0;
        mAddNumCount = 0;
        mRefreshAdnCache = false;

        log("start to queryAdnRecord");

        mQtiRilInterface.registerForAdnRecordsInfo(this, EVENT_LOAD_ADN_RECORD_DONE, null);
        mQtiRilInterface.getAdnRecord(
                obtainMessage(EVENT_QUERY_ADN_RECORD_DONE),
                mPhoneId);

        try {
            mLock.wait();
        } catch (InterruptedException e) {
            Rlog.e(LOG_TAG, "Interrupted Exception in queryAdnRecord");
        }

        mQtiRilInterface.unregisterForAdnRecordsInfo(this);
    }


    public void
    requestLoadAllAdnLike(Message response) {
        if (!mQtiRilInterface.isServiceReady()) {
            log("Oem hook service is not ready yet ");
            sendErrorResponse(response, "Oem hook service is not ready yet");
            return;
        }

        if (!mAdnLoadingWaiters.isEmpty()) {
            mAdnLoadingWaiters.add(response);
            return;
        }

        mAdnLoadingWaiters.add(response);

        synchronized (mLock) {
            if (!mSimPbRecords.isEmpty()) {
                log("ADN cache has already filled in");
                if (mRefreshAdnCache) {
                    refreshAdnCache();
                } else {
                    notifyAndClearWaiters();
                }

                return;
            }

            queryAdnRecord();
        }
    }

    public void updateSimPbAdnByRecordId(AdnRecord newAdn, int recordId, Message response) {
        if (!mQtiRilInterface.isServiceReady()) {
            log("Oem hook service is not ready yet ");
            sendErrorResponse(response, "Oem hook service is not ready yet");
            return;
        }

        synchronized (mLock) {
            if (!mSimPbRecords.isEmpty()) {
                log("ADN cache has already filled in");
                if (mRefreshAdnCache) {
                    refreshAdnCache();
                }
            } else {
                queryAdnRecord();
            }
        }

        if (newAdn == null) {
            sendErrorResponse(response, "invalid adn passed");
            return;
        }
        AdnRecord oldAdn = mSimPbRecords.get(recordId);
        if (oldAdn ==  null) {
            sendErrorResponse(response, "index isn't found");
            return;
        }

        int index = recordId;
        if (oldAdn.isEmpty() && !newAdn.isEmpty()) {
            index = 0;
        }

        QtiSimPhoneBookAdnRecord updateAdn = new QtiSimPhoneBookAdnRecord();
        updateAdn.mRecordIndex = recordId;
        updateAdn.mAlphaTag = newAdn.getAlphaTag();
        updateAdn.mNumber = newAdn.getNumber();
        if(newAdn.getEmails() != null) {
            updateAdn.mEmails = newAdn.getEmails();
            updateAdn.mEmailCount = updateAdn.mEmails.length;
        }
        if(newAdn.getAdditionalNumbers() != null) {
            updateAdn.mAdNumbers = newAdn.getAdditionalNumbers();
            updateAdn.mAdNumCount = updateAdn.mAdNumbers.length;
        }

        if (mAdnUpdatingWaiter != null) {
            sendErrorResponse(response, "Have pending update for Sim PhoneBook Adn");
            return;
        }

        mAdnUpdatingWaiter = response;
        log("recordId = " + recordId + " index = " + index);

        mQtiRilInterface.updateAdnRecord(
                    updateAdn,
                    obtainMessage(EVENT_UPDATE_ADN_RECORD_DONE, index, 0, newAdn),
                    mPhoneId);
    }

    public void updateSimPbAdnBySearch(AdnRecord oldAdn, AdnRecord newAdn, Message response) {
        if (!mQtiRilInterface.isServiceReady()) {
            log("Oem hook service is not ready yet ");
            sendErrorResponse(response, "Oem hook service is not ready yet");
            return;
        }

        synchronized (mLock) {
            if (!mSimPbRecords.isEmpty()) {
                log("ADN cache has already filled in");
                if (mRefreshAdnCache) {
                    refreshAdnCache();
                }
            } else {
                queryAdnRecord();
            }
        }

        int index = -1;
        int recordId = 1;
        if (oldAdn.isEmpty() && !newAdn.isEmpty()) {
            //add contact
            index = 0;
        } else {
            //delete or update contact
            for (AdnRecord adn : mSimPbRecords.values()) {
                if (adn.isEqual(oldAdn)) {
                    recordId = adn.getRecId();
                    index = recordId;
                    break;
                }
            }
        }
        if (index == -1) {
            sendErrorResponse(response, "Sim PhoneBook Adn record don't exist for " + oldAdn);
            return;
        }

        if(index == 0 && mValidAdnCount == mAdnCount) {
            sendErrorResponse(response, "Sim PhoneBook Adn record is full");
            return;
        }

        int recordIndex = (index == 0) ? 0 : recordId;

        QtiSimPhoneBookAdnRecord updateAdn = new QtiSimPhoneBookAdnRecord();
        updateAdn.mRecordIndex = recordIndex;
        updateAdn.mAlphaTag = newAdn.getAlphaTag();
        updateAdn.mNumber = newAdn.getNumber();
        if(newAdn.getEmails() != null) {
            updateAdn.mEmails = newAdn.getEmails();
            updateAdn.mEmailCount = updateAdn.mEmails.length;
        }
        if(newAdn.getAdditionalNumbers() != null) {
            updateAdn.mAdNumbers = newAdn.getAdditionalNumbers();
            updateAdn.mAdNumCount = updateAdn.mAdNumbers.length;
        }

        if (mAdnUpdatingWaiter != null) {
            sendErrorResponse(response, "Have pending update for Sim PhoneBook Adn");
            return;
        }

        mAdnUpdatingWaiter = response;

        mQtiRilInterface.updateAdnRecord(
                    updateAdn,
                    obtainMessage(EVENT_UPDATE_ADN_RECORD_DONE, index, 0, newAdn),
                    mPhoneId);
    }

    private final BroadcastReceiver sReceiver = new  BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED)) {
                int phoneId = intent.getIntExtra(PhoneConstants.PHONE_KEY,
                                            SubscriptionManager.INVALID_SIM_SLOT_INDEX);
                String simStatus = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
                if (IccCardConstants.INTENT_VALUE_ICC_ABSENT.equals(simStatus)
                && mPhoneId == phoneId) {
                    log("ACTION_SIM_STATE_CHANGED intent received simStatus: " + simStatus
                            + "phoneId: " + phoneId);
                    invalidateAdnCache();
                }
            }
        }
    };

    //***** Overridden from Handler

    @Override
    public void
    handleMessage(Message msg) {
        AsyncResult ar = (AsyncResult)msg.obj;
        int efid;

        switch(msg.what) {
            case EVENT_INIT_ADN_DONE:
                ar = (AsyncResult)msg.obj;
                log("Initialized ADN done");
                if (ar.exception == null) {
                    invalidateAdnCache();
                } else {
                    log("Init ADN done Exception: " + ar.exception);
                }

                break;

            case EVENT_QUERY_ADN_RECORD_DONE:
                log("Querying ADN record done");
                if (ar.exception != null) {
                    synchronized (mLock) {
                        mLock.notify();
                    }

                    for (Message response : mAdnLoadingWaiters) {
                        sendErrorResponse(response, "Query adn record failed" + ar.exception);
                    }
                    mAdnLoadingWaiters.clear();
                    break;
                }
                mAdnCount = ((int[]) (ar.result))[0];
                mValidAdnCount = ((int[]) (ar.result))[1];
                mEmailCount = ((int[]) (ar.result))[2];
                mValidEmailCount = ((int[]) (ar.result))[3];
                mAddNumCount = ((int[]) (ar.result))[4];
                mValidAddNumCount = ((int[]) (ar.result))[5];
                mMaxNameLen = ((int[]) (ar.result))[6];
                mMaxNumberLen = ((int[]) (ar.result))[7];
                mMaxEmailLen = ((int[]) (ar.result))[8];
                mMaxAnrLen = ((int[]) (ar.result))[9];
                log("Max ADN count is: " + mAdnCount
                    + ", Valid ADN count is: " + mValidAdnCount
                    + ", Email count is: " + mEmailCount
                    + ", Valid email count is: " + mValidEmailCount
                    + ", Add number count is: " + mAddNumCount
                    + ", Valid add number count is: " + mValidAddNumCount
                    + ", Max name length is: " + mMaxNameLen
                    + ", Max number length is: " + mMaxNumberLen
                    + ", Max email length is: " + mMaxEmailLen
                    + ", Valid anr length is: " + mMaxAnrLen);
                for (int i = 1; i <= mAdnCount; i++) {
                    mSimPbRecords.putIfAbsent(i,
                            new AdnRecord(IccConstants.EF_ADN, i, null, null, null, null));
                }

                if(mValidAdnCount == 0 || mRecCount == mValidAdnCount) {
                    sendMessage(obtainMessage(EVENT_LOAD_ALL_ADN_LIKE_DONE));
                }
                break;

            case EVENT_LOAD_ADN_RECORD_DONE:
                log("Loading ADN record done");
                if (ar.exception != null) {
                    break;
                }

                QtiSimPhoneBookAdnRecord[] AdnRecordsGroup = (QtiSimPhoneBookAdnRecord[])(ar.result);
                if (AdnRecordsGroup == null) {
                    break;
                }

                for (int i = 0 ; i < AdnRecordsGroup.length ; i++) {
                    if (AdnRecordsGroup[i] != null) {
                        mSimPbRecords.replace(AdnRecordsGroup[i].getRecordIndex(),
                                        new AdnRecord(IccConstants.EF_ADN,
                                                AdnRecordsGroup[i].getRecordIndex(),
                                                AdnRecordsGroup[i].getAlphaTag(),
                                                AdnRecordsGroup[i].getNumber(),
                                                AdnRecordsGroup[i].getEmails(),
                                                AdnRecordsGroup[i].getAdNumbers()));
                        mRecCount ++;
                    }
                }

                if(mRecCount == mValidAdnCount) {
                    sendMessage(obtainMessage(EVENT_LOAD_ALL_ADN_LIKE_DONE));
                }
                break;

            case EVENT_LOAD_ALL_ADN_LIKE_DONE:
                log("Loading all ADN records done");
                synchronized (mLock) {
                    mLock.notify();
                }

                notifyAndClearWaiters();
                break;

            case EVENT_UPDATE_ADN_RECORD_DONE:
                log("Update ADN record done");
                Exception e = null;

                if (ar.exception == null) {
                    int index = msg.arg1;
                    AdnRecord adn = (AdnRecord) (ar.userObj);
                    int recordIndex = ((int[]) (ar.result))[0];
                    log("recordIndex = " + recordIndex + " index = " + index);

                    if(index == 0) {
                        //add contact
                        log("Record number for added ADN is " + recordIndex);
                        adn.setRecId(recordIndex);
                        mSimPbRecords.replace(recordIndex, adn);
                        if(adn.getEmails() != null) {
                            int usedEmailCount = mValidEmailCount + adn.getEmails().length;
                            mValidEmailCount =
                                usedEmailCount <= mEmailCount ? usedEmailCount : mEmailCount;
                        }
                        if(adn.getAdditionalNumbers() != null) {
                            int usedAnrCount =
                                mValidAddNumCount + adn.getAdditionalNumbers().length;
                            mValidAddNumCount =
                                usedAnrCount <= mAddNumCount ? usedAnrCount : mAddNumCount;
                        }
                        mValidAdnCount ++;
                    } else if (adn.isEmpty()){
                        //delete contact
                        AdnRecord deletedRecord = mSimPbRecords.get(recordIndex);
                        int adnRecordIndex = deletedRecord.getRecId();
                        log("Record number for deleted ADN is " + adnRecordIndex);
                        if(recordIndex == adnRecordIndex) {
                            if(deletedRecord.getEmails() != null) {
                                int usedEmailCount = mValidEmailCount
                                    - deletedRecord.getEmails().length;
                                mValidEmailCount = usedEmailCount > 0 ? usedEmailCount : 0;
                            }
                            if(deletedRecord.getAdditionalNumbers() != null) {
                                int usedAnrCount = mValidAddNumCount
                                    - deletedRecord.getAdditionalNumbers().length;
                                mValidAddNumCount = usedAnrCount > 0 ? usedAnrCount : 0;
                            }
                            adn.setRecId(recordIndex);
                            mSimPbRecords.replace(recordIndex, adn);
                            mValidAdnCount --;
                        } else {
                            e = new RuntimeException(
                                "The index for deleted ADN record did not match");
                        }
                    } else {
                        //Change contact
                        AdnRecord changedRecord = mSimPbRecords.get(recordIndex);
                        int adnRecordIndex = changedRecord.getRecId();
                        log("Record number for changed ADN is " + adnRecordIndex);
                        if(recordIndex == adnRecordIndex) {
                            int oldEmailCount = changedRecord.getEmails() == null
                                ? 0 : changedRecord.getEmails().length;
                            int newEmailCount =
                                adn.getEmails() == null ? 0 : adn.getEmails().length;
                            int usedEmailCount = mValidEmailCount - oldEmailCount + newEmailCount;
                            mValidEmailCount = usedEmailCount > 0 && usedEmailCount <= mEmailCount
                                ? usedEmailCount : mValidEmailCount;
                            int oldAnrCount = changedRecord.getAdditionalNumbers() == null
                                ? 0 : changedRecord.getAdditionalNumbers().length;
                            int newAnrCount = adn.getAdditionalNumbers() == null
                                ? 0 : adn.getAdditionalNumbers().length;
                            int usedAnrCount = mValidAddNumCount - oldAnrCount + newAnrCount;
                            mValidAddNumCount = usedAnrCount > 0 && usedAnrCount <= mAddNumCount
                                ? usedAnrCount : mValidAddNumCount;
                            adn.setRecId(recordIndex);
                            mSimPbRecords.replace(recordIndex, adn);
                        } else {
                            e = new RuntimeException(
                                "The index for changed ADN record did not match");
                        }
                    }
                } else {
                   e = new RuntimeException("Update adn record failed",
                                ar.exception);
                }

                if (mAdnUpdatingWaiter != null) {
                    AsyncResult.forMessage(mAdnUpdatingWaiter, null, e);
                    mAdnUpdatingWaiter.sendToTarget();
                    mAdnUpdatingWaiter = null;
                }
                break;

            case EVENT_SIM_REFRESH:
                ar = (AsyncResult)msg.obj;
                log("SIM REFRESH occurred");
                if (ar.exception == null) {
                    IccRefreshResponse refreshRsp = (IccRefreshResponse)ar.result;
                    if (refreshRsp == null) {
                        if (DBG) log("IccRefreshResponse received is null");
                        break;
                    }

                    if(refreshRsp.refreshResult == IccRefreshResponse.REFRESH_RESULT_FILE_UPDATE ||
                        refreshRsp.refreshResult == IccRefreshResponse.REFRESH_RESULT_INIT) {
                          invalidateAdnCache();
                    }
                } else {
                    log("SIM refresh Exception: " + ar.exception);
                }
                break;
        }

    }

    public int getAdnCount() {
        return mAdnCount;
    }

    public int getUsedAdnCount() {
        return mValidAdnCount;
    }

    public int getEmailCount() {
        return mEmailCount;
    }

    public int getUsedEmailCount() {
        return mValidEmailCount;
    }

    public int getAnrCount() {
        return mAddNumCount;
    }

    public int getUsedAnrCount() {
        return mValidAddNumCount;
    }

    public int getMaxNameLen() {
        return mMaxNameLen;
    }

    public int getMaxNumberLen() {
        return mMaxNumberLen;
    }

    public int getMaxEmailLen() {
        return mMaxEmailLen;
    }

    public int getMaxAnrLen() {
        return mMaxAnrLen;
    }

    private void log(String msg) {
        if(DBG) Rlog.d(mTag, msg);
    }

    public void invalidateAdnCache() {
        log("invalidateAdnCache");
        mRefreshAdnCache = true;
    }

    private void refreshAdnCache() {
        log("refreshAdnCache");
        mSimPbRecords.clear();
        queryAdnRecord();
    }

    @VisibleForTesting
    public boolean getRefreshAdnCache() {
        return mRefreshAdnCache;
    }
}
