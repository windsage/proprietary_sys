/*
 * Copyright (c) 2016, 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.internal.telephony;

import android.annotation.NonNull;
import android.os.AsyncResult;
import android.os.Message;
import android.telephony.AccessNetworkConstants;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.flags.FeatureFlags;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.ServiceStateTracker;
import com.android.telephony.Rlog;


public class QtiServiceStateTracker extends ServiceStateTracker {
    private static final String LOG_TAG = "QtiServiceStateTracker";

    private boolean mIsImsCallingEnabled;

    /** Used to convert HAL RegStateResult to NetworkRegistrationInfo */
    private QtiCellularNetworkService mQtiCellularNetworkService;

    protected static final int EVENT_POLL_STATE_CS_CELLULAR_REGISTRATION_FROM_CI = 101;
    protected static final int EVENT_POLL_STATE_PS_CELLULAR_REGISTRATION_FROM_CI = 102;

    public QtiServiceStateTracker(GsmCdmaPhone phone, CommandsInterface ci,
            @NonNull FeatureFlags featureFlags) {
        super(phone,ci,featureFlags);
        mIsImsCallingEnabled = isImsCallingEnabled();
        mQtiCellularNetworkService = new QtiCellularNetworkService(phone.getPhoneId());
        log("QtiServiceStateTracker created");
    }

    @Override
    public void handleMessage(Message msg) {
        log("handleMessage: received event " + msg.what);
        switch (msg.what) {

            case EVENT_IMS_CAPABILITY_CHANGED:
                super.handleMessage(msg);

                final boolean oldImsCallingEnabled = mIsImsCallingEnabled;
                mIsImsCallingEnabled = isImsCallingEnabled();

                if (mSS.getState() != ServiceState.STATE_IN_SERVICE
                        && oldImsCallingEnabled != mIsImsCallingEnabled) {
                    log("Notify service state as IMS caps will only affect" +
                            " the merged service state");
                    mPhone.notifyServiceStateChanged(mPhone.getServiceState());
                }
                break;

            case EVENT_POLL_STATE_CS_CELLULAR_REGISTRATION_FROM_CI:  //fallthrough
            case EVENT_POLL_STATE_PS_CELLULAR_REGISTRATION_FROM_CI:
                AsyncResult ar = (AsyncResult) msg.obj;
                handlePollStateResult(msg.what, ar);
                break;

            default:
                super.handleMessage(msg);
                break;
        }
    }

    @Override
    protected void issuePollCommands() {
        // Upon its auxiliary cellular network service instance becomes null, it means
        // the cellular network service of the framework is bound already.
        if (mQtiCellularNetworkService == null || isCellularNetworkServiceConnected()) {

            // We have reached here because connection to CellularNetworkService has now been
            // established in the super class.
            // As such, the local instance of QtiCellularNetworkService is no longer needed.
            if (mQtiCellularNetworkService != null) {
                mQtiCellularNetworkService = null;
                log("QTISST: discarding local service instance");
            }
            super.issuePollCommands();

        } else {

            log("QTISST: issuePollCommands");

            // Issue poll related commands directly from RIL.
            mPollingContext[0]++;
            mCi.getOperator(obtainMessage(EVENT_POLL_STATE_OPERATOR, mPollingContext));

            mPollingContext[0]++;
            mCi.getDataRegistrationState(obtainMessage(
                    EVENT_POLL_STATE_PS_CELLULAR_REGISTRATION_FROM_CI, mPollingContext));

            mPollingContext[0]++;
            mCi.getVoiceRegistrationState(obtainMessage(
                    EVENT_POLL_STATE_CS_CELLULAR_REGISTRATION_FROM_CI, mPollingContext));

            if (mPhone.isPhoneTypeGsm()) {
                mPollingContext[0]++;
                mCi.getNetworkSelectionMode(obtainMessage(
                        EVENT_POLL_STATE_NETWORK_SELECTION_MODE, mPollingContext));
            }
        }
    }

    // Because UE moves into OOS environment, IMS registration state can't get updated in time
    // based on timer's settings from modem as per 3GPP spec, so Telephony has to combine parts of
    // IMS capbilities to determine if service state needs to be notified.
    private boolean isImsCallingEnabled() {
        return mPhone != null
                && (mPhone.isVolteEnabled() || mPhone.isWifiCallingEnabled()
                        || mPhone.isVideoEnabled());
    }

    private boolean isCellularNetworkServiceConnected() {
        // Default to true because if the connection breaks at a later point in time, the local
        // instance of QtiCellularNetworkService will have already been changed to null, and we
        // would end up with no requests sent/received, which can mess up the mPollingContext
        // counter.
        boolean isConnected = true;

        try {
            isConnected = mRegStateManagers.get(AccessNetworkConstants.TRANSPORT_TYPE_WWAN)
                    .isServiceConnected();
        } catch (NullPointerException ex) {
            Rlog.e(LOG_TAG, "Exception checking for cellular network service connection", ex);
        }

        return isConnected;
    }

    protected void handlePollStateResultMessage(int what, AsyncResult ar) {
        log("QTISST: handlePollStateResultMessage processing " + what + messageToString(what));
        switch (what) {

            case EVENT_POLL_STATE_CS_CELLULAR_REGISTRATION_FROM_CI: {

                if (mQtiCellularNetworkService == null) {
                    // This has happened because CellularNetworkService got connected just before
                    // this response, which is now stale and a new set of polling commands will
                    // be issued.
                    super.handlePollStateResultMessage(EVENT_POLL_STATE_CS_CELLULAR_REGISTRATION,
                            new AsyncResult(null, null, new IllegalStateException()));
                } else {
                    NetworkRegistrationInfo networkRegState =
                            mQtiCellularNetworkService.getRegistrationStateFromResult(ar.result,
                                    NetworkRegistrationInfo.DOMAIN_CS);
                    super.handlePollStateResultMessage(EVENT_POLL_STATE_CS_CELLULAR_REGISTRATION,
                            new AsyncResult(null, networkRegState, null));
                }
                break;
            }

            case EVENT_POLL_STATE_PS_CELLULAR_REGISTRATION_FROM_CI: {

                if (mQtiCellularNetworkService == null) {
                    // This has happened because CellularNetworkService got connected just before
                    // this response, which is now stale and a new set of polling commands will
                    // be issued.
                    super.handlePollStateResultMessage(EVENT_POLL_STATE_PS_CELLULAR_REGISTRATION,
                            new AsyncResult(null, null, new IllegalStateException()));
                } else {
                    NetworkRegistrationInfo networkRegState =
                            mQtiCellularNetworkService.getRegistrationStateFromResult(ar.result,
                                    NetworkRegistrationInfo.DOMAIN_PS);
                    super.handlePollStateResultMessage(EVENT_POLL_STATE_PS_CELLULAR_REGISTRATION,
                            new AsyncResult(null, networkRegState, null));
                }
                break;
            }

            default:
                super.handlePollStateResultMessage(what, ar);
        }
    }

    private String messageToString(int what) {
        switch (what) {

            case EVENT_POLL_STATE_PS_CELLULAR_REGISTRATION_FROM_CI :
                return ": EVENT_POLL_STATE_PS_CELLULAR_REGISTRATION_FROM_CI";

            case EVENT_POLL_STATE_CS_CELLULAR_REGISTRATION_FROM_CI :
                return ": EVENT_POLL_STATE_CS_CELLULAR_REGISTRATION_FROM_CI";

            default : return "";
        }
    }
}
