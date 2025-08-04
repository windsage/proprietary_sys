/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
  * Not a Contribution.
  *
  * Copyright (C) 2013 The Android Open Source Project
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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.ims.ImsReasonInfo;

import androidx.annotation.VisibleForTesting;

import com.qualcomm.ims.utils.Log;
import com.qualcomm.ims.vt.ImsMedia;

import org.codeaurora.telephony.utils.AsyncResult;

/* This class is responsible to handle all conference related requests, responses and indications.
 * Keep track of conference states and notifies the conference state to all clients.
 */
public class ImsConferenceController {
    private static final String LOG_TAG = "ImsConferenceController";
    private ImsSenderRxr mCi;
    private Context mContext;
    private List<Listener> mListeners = new CopyOnWriteArrayList<>();
    private Handler mHandler;
    private ImsServiceClassTracker mImsServiceClassTracker;
    private ImsCallSessionCallbackHandler mMergeHostListener;
    private ConferenceResult mConferenceResult;
    private ConferenceState mConferenceState = ConferenceState.IDLE;
    private boolean mIsConferenceCallStateCompleted = false;
    private boolean mIsConferenceResponseReceived = false;
    private ImsReasonInfo mConferenceResponseError = null;

    public enum ConferenceState {
        IDLE, PROGRESS, COMPLETED
    };

    private static final int EVENT_CONFERENCE = 1;
    private static final int EVENT_REFRESH_CONF_INFO = 2;
    private static final int EVENT_CONFERENCE_CALL_STATE_COMPLETED = 3;
    private static final int EVENT_RESUME_NETWORK_HELD_PARTICIPANT = 4;
    private static final int EVENT_HANGUP_NETWORK_HELD_PARTICIPANT = 5;
    private static final int EVENT_ABORT_CONFERENCE = 6;

    public interface Listener {
        /**
         * Method that reports the conference states.
         * @param conferenceState, @see ImsConferenceController#ConferenceState.
         * @param isSuccess, true if conference is success.
         *                   false if conference is failure.
         *                   This is valid only for ConferenceState#COMPLETED to
         *                    know conference is success or failure.
         */
        public void onConferenceStateChanged(ConferenceState conferenceState,
                final boolean isSuccess);
        /* To update that the remote party call converted to multiparty call.
         * @param isMultiParty, true if remote call converted to multiparty.
         */
        public void onConferenceParticipantStateChanged(final boolean isMultiParty);
        /**
         * Method to update the client about abort conference status
         */
        public void onAbortConferenceCompleted(final boolean shouldAllowPendingDialRequest);
    }

    /**
     * Registers a Listener.
     * @param Listener Listener to be registered for conference state updates.
     * @see ImsConferenceController#Listener
     * @throws IllegalArgumentException Will throw an error if Listener is null.
     */
    public void registerListener(Listener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener is null!");
        }

        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
        } else {
            logw("registerListener :: duplicate Listener!");
        }
    }

    /**
     * Unregisters a Listener.
     * @param Listener Listener to unregister
     * @see ImsConferenceController#Listener
     * @throws IllegalArgumentException Will throw an error if listener is null.
     * @return true of listener is removed, false otherwise.
     */
    public boolean unregisterListener(Listener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener is null!");
        }

        return mListeners.remove(listener);
    }

    /**
     * To know the current conference state.
     * @return ConferenceState.
     */
    public ConferenceState getConferenceState() {
        return mConferenceState;
    }

    /**
     * To know the conference is in progress.
     * @return true, if conference is in PROGRESS.
     *         false otherwise.
     */
    public boolean isInProgress() {
        return mConferenceState == ConferenceState.PROGRESS;
    }

    /**
     * To update that the remote party call converted to multiparty call.
     * @param cs, current CallSession.
     * @param isMultiParty, true if remote call converted to multiparty.
     * This will trigger ImsConferenceStateListener#onConferenceParticipantStateChanged to all
     * registered clients if any change in multi party.
     */
    public void mayBeUpdateMultipartyState(ImsCallSessionImpl cs, final boolean isMultiParty) {
        logv("mayBeUpdateMultipartyState : CallSession isMpty: " + cs.isMultipartyCall() +
                " isMultiParty: " + isMultiParty);

        if (cs.isMultipartyCall() == isMultiParty) {
            logv("mayBeUpdateMultipartyState : no change in mpty");
            return;
        }

        for (Listener listener : mListeners) {
            listener.onConferenceParticipantStateChanged(isMultiParty);
        }
    }

    private void notifyConferenceStateChanged(ConferenceState conferenceState, boolean isSuccess) {
        logv("notifyConferenceStateChanged");
        for (Listener listener : mListeners) {
            listener.onConferenceStateChanged(conferenceState, isSuccess);
        }
    }

    private void notifyAbortConferenceCompleted(boolean shouldAllowPendingDialRequest) {
        logv("notifyAbortConferenceCompleted");
        for (Listener listener : mListeners) {
            listener.onAbortConferenceCompleted(shouldAllowPendingDialRequest);
        }
    }

    // Constructor
    public ImsConferenceController(ImsSenderRxr ci, Context context,
            ImsServiceClassTracker serviceClassTracker) {
        mCi = ci;
        mContext = context;
        mImsServiceClassTracker = serviceClassTracker;
        mHandler = new ImsConferenceControllerHandler();
        registerListener(ImsMedia.getInstance());
        mCi.registerForRefreshConfInfo(mHandler, EVENT_REFRESH_CONF_INFO, null);
        mCi.registerForConferenceCallStateCompleted(mHandler,
                EVENT_CONFERENCE_CALL_STATE_COMPLETED, null);
    }

    public void dispose() {
        unregisterListener(ImsMedia.getInstance());
        mCi.unregisterForRefreshConfInfo(mHandler);
        mCi.unregisterForConferenceCallStateCompleted(mHandler);
        mHandler = null;
        mContext = null;
    }

    /**
     * Process new multiparty call and update is it created due to conference process or
     * phantom call.
     * @param cs, current multiparty CallSession.
     * @return true, if conference is not initiated using ImsConferenceController.
     *         false if conference is initiated using ImsConferenceController and having valid data.
     */
    public boolean processNewMptyCall(ImsCallSessionImpl cs) {
        if (mConferenceResult == null || mConferenceResult.confHostCall == null ||
                !isInProgress()) {
            logd("processNewMptyCall: callSession is phantom conference call");
            return true;
        }

        cs.setConfInfo(mConferenceResult.confHostCall.getConfInfo());
        mConferenceResult.confHostCall.reportNewConferenceCallSession(cs);
        return false;
    }

    private class ConferenceResult {
        ImsCallSessionImpl activeCall;
        ImsCallSessionImpl heldCall;
        ImsCallSessionImpl confHostCall;
        boolean shouldHaveTransientSession = true;
    }

    /*package*/ void cleanupConferenceAttempt() {
        mMergeHostListener = null;
        mConferenceResult = null;
        mIsConferenceCallStateCompleted = false;
        mIsConferenceResponseReceived = false;
        mConferenceResponseError = null;
        mConferenceState = ConferenceState.IDLE;
        notifyConferenceStateChanged(ConferenceState.IDLE, false);
    }

    public void handleConferenceResult() {

        if (mConferenceResponseError != null) {
            // If the response is a failure,  we know that both the calls
            // have failed to get merged into the conference call.
            // mConferenceResponseError will be valid if conference is failure.

            // Telephony framework expecting call states before and after conference failure will be
            // same if calls are alive. If call states are not expected then handling here to avoid
            // unwanted behaviors.
            // 1. Check if active call before conference is in HOLD
            // 2. If in HOLD try to resume the call to make sure call states before and after are
            //    same.
            // 3. If not able to RESUME the call then END the call.
            if (getCallSessionDriverCallState(mConferenceResult.activeCall) ==
                    DriverCallIms.State.HOLDING) {
                logd("handleConferenceResult: Resume network HELD call");
                mCi.resume(mHandler.obtainMessage(EVENT_RESUME_NETWORK_HELD_PARTICIPANT, this),
                        mConferenceResult.activeCall.getCallIdInt());
            } else {
                processConferenceFailure();
            }
            return;
        }

        processConferenceResult();
    }

    private void processConferenceFailure() {
        mMergeHostListener.callSessionMergeFailed(mConferenceResponseError);
        mConferenceState = ConferenceState.COMPLETED;
        notifyConferenceStateChanged(ConferenceState.COMPLETED, false);
        cleanupConferenceAttempt();
    }

    private void processConferenceResult() {
        logi("Conference response received. Processing final result.");

        // Check the states of existing calls. By this point of time, the call list
        // should be up to date with the final states of all the call sessions. If
        // sessions have ended as part of the merge process, they will not be present.

        //Get ACTIVE call session
        ImsCallSessionImpl activeCs = getCallSessionByState(DriverCallIms.State.ACTIVE);
        if (activeCs != null) {
            // If the conference host terminates before conference call session is
            // created conference info xml file will be processed with host ConfInfo
            // object which has to be updated to conference call
            if (mConferenceResult.shouldHaveTransientSession &&
                    (mConferenceResult.activeCall != null)) {
                activeCs.setConfInfo(mConferenceResult.activeCall.getConfInfo());
            }

            mConferenceResult.activeCall = activeCs;

            if (mConferenceResult.shouldHaveTransientSession) {
                logv("Setting mIsConferenceHostSession to true");
                activeCs.mIsConferenceHostSession = true;
            }
        } else {
            //TODO: The only possible valid scenario might be all calls ending.
            //      Discussion required on how to handle this scenario.
        }

        // If there are no held calls, it implies both the calls got merged into the
        // conference, and hence ended.
        // NOTE: We cannot figure out genuine call drops here.
        if (getCallSessionByState(DriverCallIms.State.HOLDING) == null) {
            mConferenceResult.heldCall = null;
        }

        // Call the merge success callback with the active call for 3 way conference
        // This active call is used to create transient session
        // For all other scenario pass null as the session would already exist
        if (mConferenceResult.shouldHaveTransientSession) {
            mMergeHostListener.callSessionMergeComplete(mConferenceResult.activeCall);
            // When conference host call is ended as part of merge into conference,
            // the ImsCallSession is not closed from telephony framework. Instead
            // the ImsCallSessiom is replaced with the transient conference call
            // session created. Hence we close the conference host session here
            // explicitly so that the onClosed() listeners can do the required cleanup.
            // For example CameraController can close camera if conf host session is the
            // camera owner.
            if (mConferenceResult.confHostCall != null &&
                    !mConferenceResult.confHostCall.isImsCallSessionAlive()) {
                logi("processConferenceResult: close conf host session");
                mConferenceResult.confHostCall.close();
            }
        } else {
            mMergeHostListener.callSessionMergeComplete(null);
        }

        // Un-mute all call sessions still present.
        mConferenceState = ConferenceState.COMPLETED;
        notifyConferenceStateChanged(ConferenceState.COMPLETED, true);

        // Reset all conference call flow variables.
        cleanupConferenceAttempt();
    }

    public void sendConferenceRequest(ImsCallSessionImpl hostSession) {
        logi("Conference request being requested by session = " + hostSession);

        if (isInProgress()) {
            loge("sendConferenceRequest: in middle of merge process, ignore.");
            return;
        }

        mConferenceResult = new ConferenceResult();
        mConferenceResult.confHostCall = hostSession;

        // Cache the host session's listener before sending the request. If the host
        // session ends as part of the conference call flow, its listener will still
        // be required to call the callSessionMerged callback. Simply having a reference
        // will not work as it will be set to null once the call session ends.
        mMergeHostListener = hostSession.mCallbackHandler;

        // Cache a snapshot of the call sessions. This will be used while processing the final
        // call session states after the conference merge process ends.
        ImsCallSessionImpl cs = getCallSessionByState(DriverCallIms.State.ACTIVE);
        if (cs == null) {
            loge("sendConferenceRequest: there is no ACTIVE call session");
            cleanupConferenceAttempt();
            return;
        }
        mConferenceResult.activeCall = cs;

        // Get HOLDING call session
        cs = getCallSessionByState(DriverCallIms.State.HOLDING);
        if (cs == null) {
            loge("sendConferenceRequest: there is no HOLD call session");
            cleanupConferenceAttempt();
            return;
        }
        mConferenceResult.heldCall = cs;

        //If a call session is a conference host, we should not have a transient session
        if (mConferenceResult.activeCall.mIsConferenceHostSession ||
                mConferenceResult.heldCall.mIsConferenceHostSession) {
            logv("sendConferenceRequest: Setting should have transient session to false ");
            mConferenceResult.shouldHaveTransientSession = false;
        }

        mConferenceState = ConferenceState.PROGRESS;
        // Block the intermediate call state reporting for all the call sessions
        // till conference merge is in progress by notifying conference state.
        notifyConferenceStateChanged(ConferenceState.PROGRESS, false);

        // Send the conference request to lower layers.
        mCi.conference(mHandler.obtainMessage(EVENT_CONFERENCE, this));
    }

    public void sendAbortConferenceRequest(int conferenceAbortReason) {
        // Send the conference request to lower layers.
        mCi.abortConference(mHandler.obtainMessage(EVENT_ABORT_CONFERENCE, this),
                conferenceAbortReason);
    }

    /**
     * Handles Conference refresh Info notified through UNSOL_REFRESH_CONF_INFO message
     * @param ar - the AsyncResult object that contains the refresh Info information
     */
    public void handleRefreshConfInfo(AsyncResult ar) {
        logi( "handleRefreshConfInfo");

        if (ar == null) {
            loge("handleRefreshConfInfo: AsyncResult is null");
            return;
        }

        if (ar.exception != null) {
            loge("handleRefreshConfInfo: " + ar.exception);
            return;
        }

        if (ar.result == null) {
            loge("handleRefreshConfInfo: ConfInfo is null");
            return;
        }

        ConfInfo confRefreshInfo = (ConfInfo) ar.result;
        byte[] confInfoBytes = confRefreshInfo.getConfInfoUri();
        final int confCallState = confRefreshInfo.getConfCallState();
        int state = (confCallState != ConfInfo.INVALID) ? confCallState : ConfInfo.FOREGROUND;
        ImsCallSessionImpl callSession = getCallSessionWithMptyBitSet(state);

        if (confInfoBytes == null || confInfoBytes.length < 1 || callSession == null) {
            loge("handleRefreshConfInfo: confInfoBytes: " + confInfoBytes + " state: " + state +
                    " CallSession: " + callSession);
            return;
        }

        logi("handleRefreshConfInfo: confCallState = " + state + ", callSession = "
                + callSession /*+ ", confInfoBytes: " + confInfoBytes*/);
        /*
         * UE subscribes for conference xml as soon as it establishes session with conference
         * server.Multiparty bit will be updated only through UNSOL_RESPONSE_CALL_STATE_CHANGED
         * after all the participants are merged to the call. So refresh info can be received
         * during the interval in which the conference request is sent and before the conference
         * call reflects in the UNSOL_RESPONSE_CALL_STATE_CHANGED.
         */
        callSession.notifyConfInfo(confInfoBytes);
    }

    private ImsCallSessionImpl getCallSessionWithMptyBitSet(int state) {
        // Use the cached active session as during conference one of the sessions
        // will receive END as part of merge
        if ((mConferenceResult != null) && mConferenceResult.shouldHaveTransientSession) {
            logi("getCallSessionWithMptyBitSet session = " + mConferenceResult.activeCall);
            return mConferenceResult.activeCall;
        }

        return mImsServiceClassTracker.getCallSessionWithMptyBitSet(state);
    }

    @VisibleForTesting
    public ImsCallSessionImpl getCallSessionByState(DriverCallIms.State state) {
        if (mImsServiceClassTracker == null) {
            loge("getCallSessionByState: ImsServiceClassTracker is null");
            return null;
        }

        List<ImsCallSessionImpl> csList = mImsServiceClassTracker.getCallSessionByState(state);

        if (csList.isEmpty()) {
            loge("getCallSessionByState: there are no call sessions");
            return null;
        }

        return csList.get(0);
    }

    private DriverCallIms.State getCallSessionDriverCallState(
            ImsCallSessionImpl activeCallSession) {
        if (activeCallSession == null) {
            loge("getCallSessionDriverCallState: activeCallSession is null");
            return DriverCallIms.State.END;
        }

        // Use latest call state from ServiceClassTracker instead of using the cache session
        // (mConferenceResult.activeCall) as during the process of conference, calls can be ended
        // due to remote end or due to network conditions.
        ImsCallSessionImpl session = getCallSession(activeCallSession.getCallId());
        return session != null ? session.getDriverCallState() : DriverCallIms.State.END;
    }

    private ImsCallSessionImpl getCallSession(String callId) {
        if (mImsServiceClassTracker == null) {
            loge("getCallSession: ImsServiceClassTracker is null");
            return null;
        }

        return mImsServiceClassTracker.getCallSession(callId);
    }

    //Handler for the events on response from ImsSenderRxr
    private class ImsConferenceControllerHandler extends Handler {

        ImsConferenceControllerHandler() {
            this(Looper.getMainLooper());
        }

        ImsConferenceControllerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            logi("Message received: what = " + msg.what);
            AsyncResult ar;

            switch (msg.what) {
                case EVENT_CONFERENCE:
                    ar = (AsyncResult) msg.obj;
                    handleConferenceResponse(ar);
                    break;
                case EVENT_REFRESH_CONF_INFO:
                    ar = (AsyncResult) msg.obj;
                    handleRefreshConfInfo(ar);
                    break;
                case EVENT_CONFERENCE_CALL_STATE_COMPLETED:
                    handleConferenceCompleted();
                    break;
                case EVENT_RESUME_NETWORK_HELD_PARTICIPANT:
                    ar = (AsyncResult) msg.obj;
                    handleResumeResponse(ar);
                    break;
                case EVENT_HANGUP_NETWORK_HELD_PARTICIPANT:
                    ar = (AsyncResult) msg.obj;
                    handleHangupResponse(ar);
                    break;
                case EVENT_ABORT_CONFERENCE:
                    ar = (AsyncResult) msg.obj;
                    handleAbortConferenceResponse(ar);
                    break;
                default:
                    logi("Unknown message = " + msg.what);
            }
        }
    }

    private void handleResumeResponse(AsyncResult ar) {
        if (ar != null && ar.exception != null) {
            // Hangup the call as resume failed.
            // Cross check the call state before ending the call to make sure not trying to end
            // the call which is already ended.
            if (getCallSessionDriverCallState(mConferenceResult.activeCall) ==
                    DriverCallIms.State.HOLDING) {
                logd("handleResumeResponse: Hangup call due to explicit resume operation failed");
                mCi.hangupWithReason(mConferenceResult.activeCall.getCallIdInt(),
                        null, null, false, ImsReasonInfo.CODE_USER_TERMINATED, null,
                        mHandler.obtainMessage(EVENT_HANGUP_NETWORK_HELD_PARTICIPANT, this));
                return;
            }
        }
        processConferenceFailure();
    }

    private void handleHangupResponse(AsyncResult ar) {
        // Mark conference failure is completed for hangup success/failure.
        if (ar != null && ar.exception != null) {
            Log.e(this, "Hangup error: " + ar.exception);
        }
        processConferenceFailure();
    }

    private void handleConferenceResponse(AsyncResult ar) {
        mIsConferenceResponseReceived = true;
        mConferenceResponseError = (ar == null || ar.exception != null) ?
                ImsCallUtils.getImsReasonInfo(ar) : null;

        // If HIDL is not supporting UNSOL_CONFERENCE_CALL_STATE_COMPLETED process immediately
        if (!mCi.isFeatureSupported(Feature.CONFERENCE_CALL_STATE_COMPLETED)) {
            handleConferenceResult();
            return;
        }

        if (!mIsConferenceCallStateCompleted) {
            //Conference call state completed not yet received
            return;
        }

        handleConferenceResult();
    }

    private void handleAbortConferenceResponse(AsyncResult ar) {
        boolean abortConferenceResponseError = (ar == null || ar.exception != null) ?
                true : false;
        /*
         * We need to allow the dial if the error is with request not supported
         * to preserve legacy behavior.
         */
        boolean shouldAllowPendingRequest = !abortConferenceResponseError ||
                (ar != null && ((ImsRilException) ar.exception).getErrorCode() ==
                ImsErrorCode.REQUEST_NOT_SUPPORTED);

        logi("shouldAllowPendingRequest : " + shouldAllowPendingRequest);
        notifyAbortConferenceCompleted(shouldAllowPendingRequest);
    }

    private void handleConferenceCompleted() {
        mIsConferenceCallStateCompleted = true;

        if (!mIsConferenceResponseReceived) {
            //Conference response not yet received
            return;
        }

        handleConferenceResult();
    }

    @VisibleForTesting
    public Handler getHandler() {
        return mHandler;
    }

    private void logi(String s) {
        Log.i(LOG_TAG, s);
    }

    private void logd(String s) {
        Log.d(LOG_TAG, s);
    }

    private void logv(String s) {
        Log.v(LOG_TAG, s);
    }

    private void logw(String s) {
        Log.w(LOG_TAG, s);
    }

    private void loge(String s) {
        Log.e(LOG_TAG, s);
    }
}
