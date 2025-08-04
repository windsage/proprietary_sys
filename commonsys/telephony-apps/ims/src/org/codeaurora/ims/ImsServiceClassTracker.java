/*
 * Copyright (c) 2015-2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.telephony.ims.feature.MmTelFeature;
import android.telephony.ims.ImsCallProfile;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.ImsSuppServiceNotification;
import android.telephony.ims.stub.ImsCallSessionImplBase;
import android.util.SparseIntArray;

import androidx.annotation.VisibleForTesting;

import com.qualcomm.ims.utils.Log;
import org.codeaurora.telephony.utils.AsyncResult;
import org.codeaurora.telephony.utils.SomeArgs;

import org.codeaurora.ims.utils.CallComposerInfoUtils;
import org.codeaurora.ims.utils.QtiImsExtUtils;

public class ImsServiceClassTracker implements Handler.Callback {
    // Call Id, Dial String --> Call Session
    private final Map<String, ImsCallSessionImpl> mCallList;
    private final ArrayList<ImsCallSessionImpl> mPendingSessionList;
    private ArrayList<ImsUssdSessionImpl> mUssdList;
    private List<ICallListListener> mCallListListeners;
    private SparseIntArray mPreAlertingCallTokenList;

    private ImsSenderRxr mCi = null;
    private Context mContext;

    private boolean mIsVideoSupported = false;
    private boolean mIsVoiceSupported = false;

    private Handler mHandler;
    private ImsServiceSub mServiceSub;

    public static final String CONF_URI_DC_NUMBER = "Conference Call";

    private static int sToken = -1;
    private final int EVENT_INCOMING_DTMF_START = 1;
    private final int EVENT_INCOMING_DTMF_STOP = 2;
    private final int EVENT_CLOSE_ALL_SESSIONS = 3;
    private final int EVENT_SRTP_ENCRYPTION_UPDATE = 4;

    private QImsSessionBase.ListenerBase mCallListener = new QImsSessionBase.ListenerBase() {
        @Override
        public void onClosed(QImsSessionBase s) {
            ImsServiceClassTracker.this.onCallClosed(s);
        }
    };

    private QImsSessionBase.ListenerBase mUssdListener = new QImsSessionBase.ListenerBase() {
        @Override
        public void onClosed(QImsSessionBase s) {
            ImsServiceClassTracker.this.onUssdClosed(s);
        }
    };

    public ImsServiceClassTracker(ImsSenderRxr ci, Context context, ImsServiceSub serviceSub) {
        this(ci, context, serviceSub, Looper.getMainLooper());
    }

    // Constructor
    @VisibleForTesting
    public ImsServiceClassTracker(ImsSenderRxr ci, Context context, ImsServiceSub serviceSub,
            Looper looper) {
        mCi = ci;
        mContext = context;
        mCallList = new HashMap<String, ImsCallSessionImpl>();
        mPendingSessionList = new ArrayList<ImsCallSessionImpl>();
        mUssdList = new ArrayList<ImsUssdSessionImpl>();
        mCallListListeners = new CopyOnWriteArrayList<>();
        mPreAlertingCallTokenList = new SparseIntArray();
        mServiceSub = serviceSub;
        mHandler = new Handler(looper, this);
        mCi.registerForIncomingDtmfStart(mHandler, EVENT_INCOMING_DTMF_START, null);
        mCi.registerForIncomingDtmfStop(mHandler, EVENT_INCOMING_DTMF_STOP, null);
        mCi.registerForSrtpEncryptionUpdate(mHandler, EVENT_SRTP_ENCRYPTION_UPDATE, null);
    }

    /**
     * Get the call Count associated with this tracker object
     * @return call Count
     */
    @VisibleForTesting
    public int getCallCount() {
        return mCallList.size();
    }

    /**
     * Get the ussd Count associated with this tracker object
     * @return ussd Count
     */
    @VisibleForTesting
    public int getUssdCount() {
        return mUssdList.size();
    }

    /**
     * Creates a Bunde for passing incoming call data.
     * @param isUssd - If this is a USSD request
     * @param isUnknown - If this is a Phantom call
     *
     * @return Bundle instance containing the params.
     */
    private Bundle createIncomingCallBundle(boolean isUssd, boolean isUnknown) {
        Bundle extras = new Bundle();
        extras.putBoolean(MmTelFeature.EXTRA_IS_USSD, isUssd);
        extras.putBoolean(MmTelFeature.EXTRA_IS_UNKNOWN_CALL, isUnknown);
        return extras;
    }

    public void updateFeatureCapabilities(boolean isVideoSupported, boolean isVoiceSupported) {
        Log.i(this,"updateFeatureCapabilities video " + isVideoSupported + " voice " + isVoiceSupported);
        mIsVideoSupported = isVideoSupported;
        mIsVoiceSupported = isVoiceSupported;
        for (Map.Entry<String, ImsCallSessionImpl> e : mCallList.entrySet()) {
            final ImsCallSessionImpl session = e.getValue();
            session.updateFeatureCapabilities(isVideoSupported, isVoiceSupported);
        }
    }

    public void updateLowBatteryStatus() {
        for (Map.Entry<String, ImsCallSessionImpl> e : mCallList.entrySet()) {
            final ImsCallSessionImpl session = e.getValue();
            session.updateLowBatteryStatus();
        }
    }

    /**
     * Method to provide the currently available calls related information
     * and explictly marking the individual call state as END.
     */
    public Object getCallsListToClear() {
        ArrayList<DriverCallIms> response = null;
        DriverCallIms dc;

        if (mCallList.size() > 0) {
            response = new ArrayList<DriverCallIms> ();

            for (Iterator<Map.Entry<String, ImsCallSessionImpl>> it =
                    mCallList.entrySet().iterator(); it.hasNext();) {
                Map.Entry<String, ImsCallSessionImpl> e = it.next();
                ImsCallSessionImpl callSession = (ImsCallSessionImpl) e.getValue();
                dc = new DriverCallIms();
                dc.state = DriverCallIms.State.END;
                dc.index = callSession.getCallIdInt();
                dc.callDetails = new CallDetails();
                dc.callDetails.call_type = callSession.getInternalCallType();
                dc.callDetails.call_domain = callSession.getCallDomain();
                dc.callFailCause = new ImsReasonInfo(ImsReasonInfo.
                        CODE_LOCAL_SERVICE_UNAVAILABLE, ImsReasonInfo.CODE_UNSPECIFIED, null);
                response.add(dc);
            }
            Collections.sort(response);
        }

        return response;
    }

    /**
     * Handle a list of calls updated by the IMS stack
     * @param dcList
     */
    public void handleCalls(ArrayList<DriverCallIms> dcList) {
        Map <String, DriverCallIms> dcMap = new HashMap<String, DriverCallIms>();
        // First pass is to look at every call in dc and update the Call Session List
        final boolean disableVideo = shallDisableVideo(dcList);
        for (int i = 0; dcList!= null && i < dcList.size(); i++) {
            ImsCallSessionImpl callSession = null;
            DriverCallIms dc = maybeDisableVideo(dcList.get(i), disableVideo);
            if (mPendingSessionList != null) {
                for (Iterator<ImsCallSessionImpl> it = mPendingSessionList.iterator();
                        it.hasNext();) {
                    ImsCallSessionImpl s = it.next();
                    // Match the DIALING call or END call which is not already added to mCallList
                    if ((dc.state == DriverCallIms.State.DIALING ||
                            dc.state == DriverCallIms.State.END)
                            && mCallList.get(Integer.toString(dc.index)) == null) {
                        // Add to call list as we now have call id, remove from
                        // temp list
                        Log.i(this, "Found match call session in temp list, s = " + s);
                        Log.i(this, "Index in call list is " + dc.index);
                        addCall(dc.index, s, false);
                        // Remove from mPendingSessionList
                        it.remove();
                    }
                }
            }

            callSession = mCallList.get(Integer.toString(dc.index));
            if (callSession != null){
                // Pending MO, active call updates
                // update for a existing call - no callID but MO number for a dial request
                // Call collision scenario
                callSession.updateCall(dc);
            } else {
                boolean isUnknown = false;
                if (dc.state == DriverCallIms.State.END) {
                    //This is an unknown call probably already cleaned up as part of SRVCC
                    //just ignore this dc and continue with the dc list

                    maybeBroadcastPreAlertingCallIntent(dc.index);
                    continue;
                }
                callSession = new ImsCallSessionImpl(dc, mCi, mContext, this, mIsVideoSupported,
                        mServiceSub.getPhoneId(), mServiceSub.getImsConferenceController());
                callSession.addListener(mCallListener);
                callSession.updateFeatureCapabilities(mIsVideoSupported, mIsVoiceSupported);
                // TODO: This functionality of informing ServiceSub of a new call needs to
                // be revisited from a design point of view.
                if (dc.isMT) {
                    Log.i(this, "MT Call creating a new call session");
                    reportIncomingCall(callSession, dc.index, false);
                } else if (dc.isMpty) {
                    Log.i(this, "Conference Call creating a new call session");
                    // setting default executor directly due to Google bug#262800764
                    // can be reverted once this is fixed in AOSP
                    callSession.setDefaultExecutor(mContext.getMainExecutor());
                    isUnknown = mServiceSub.getImsConferenceController().
                            processNewMptyCall(callSession);
                    if (isUnknown) {
                        Log.i(this, "Phantom conference call Scenario");
                    } else {
                        addCall(dc.index, callSession, true);
                    }
                    callSession.updateConfSession(dc);
                } else if (dc.state != DriverCallIms.State.END) {
                   Log.i(this, "MO phantom Call Scenario. State = " + dc.state);
                   isUnknown = true;
                }
                if (isUnknown) {
                    reportIncomingCall(callSession, dc.index, true);
                }
            }
            // If state is not END then add call to list of active calls
            if (dc.state != DriverCallIms.State.END) {
                dcMap.put(Integer.toString(dc.index), dc);
            }
            if (dc.state == DriverCallIms.State.END) {
                maybeBroadcastPreAlertingCallIntent(dc.index);
            }
        }

        // Second pass to check if all Call Sessions are still active, dc will not contain
        // a disconnected call in the dc List, remove any call sessions that are not present
        // in dc List.
        for (Iterator<Map.Entry<String, ImsCallSessionImpl>> it =
                mCallList.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, ImsCallSessionImpl> e = it.next();
            if (dcMap.get(e.getKey()) == null) { // Call Dropped!
                // CallStartFailed/CallTerminated are triggered during updateCall
                // when call state is END. Also callsession close is triggered by the
                // component that creates the session.
                // Here just remove the session from tracker.
                Log.i(this, "handleCalls: removing dropped/ended call id:" +
                        e.getKey());
                it.remove();
                notifyCallRemoved(e.getValue());
            }
        }
    }

    /**
     * Handle USSD message received by the IMS stack
     * @param profile that is passed to AOSP.
     * @param info that contains USSD related information.
     */
    public void onUssdMessageReceived(UssdInfo info) {
        //In case of empty message from lower layer, treat
        //it like an error case.
        boolean isErrorCase = info.getMessage().isEmpty();

        //As in this case we have an ongoing USSD session,
        //we prevent creation of another.
        if (!mUssdList.isEmpty() && !isErrorCase) {
            Log.d(this, "onUssdMessageReceived: ongoing USSD session");
            return;
        }

        if (isErrorCase) {
            Log.d(this, "onUssdMessageReceived: received empty message");
            return;
        }

        ImsCallProfile profile = new ImsCallProfile(ImsCallProfile.SERVICE_TYPE_NORMAL,
                                                    ImsCallProfile.CALL_TYPE_VOICE);
        mServiceSub.handleNotifyIncomingCall(createCallSession(profile, info),
            createIncomingCallBundle(true, false));
    }

    private boolean shallDisableVideo(ArrayList<DriverCallIms> dcList) {
        if (QtiImsExtUtils.canHoldVideoCall(getPhoneId(), mContext)) {
            return false;
        }
        boolean hasActiveVoiceCall = false;
        boolean hasHeldVoiceCall = false;
        ImsCallSessionImpl callSession = null;

        for (DriverCallIms dc : dcList ) {
            hasActiveVoiceCall |= dc.state == DriverCallIms.State.ACTIVE &&
                    ImsCallUtils.isVoiceCall(dc.callDetails.call_type);
            hasHeldVoiceCall |= dc.state == DriverCallIms.State.HOLDING &&
                    ImsCallUtils.isVoiceCall(dc.callDetails.call_type);
        }

        return (hasActiveVoiceCall && hasHeldVoiceCall) ||
                mServiceSub.getImsConferenceController().isInProgress();
    }

    private DriverCallIms maybeDisableVideo(DriverCallIms dc, final boolean disableVideo) {
        if (!disableVideo || dc == null || dc.callDetails == null ||
                dc.callDetails.localAbility == null) {
            return dc;
        }

        boolean isVideoDisabled = !ImsCallSessionImpl.isServiceAllowed(
                CallDetails.CALL_TYPE_VT, dc.callDetails.localAbility);
        if (isVideoDisabled == disableVideo) {
            return dc;
        }

        for (ServiceStatus srv : dc.callDetails.localAbility) {
            if (srv.type == CallDetails.CALL_TYPE_VT) {
                srv.status = ServiceStatus.STATUS_DISABLED;
            }
        }
        return dc;
    }

    public void onCallClosed(QImsSessionBase session) {
        ImsCallSessionImpl callSession = (ImsCallSessionImpl)session;
        if (callSession.getState() != ImsCallSessionImplBase.State.TERMINATED) {
            Log.i(this, "onCallClosed: call session not in terminated state. Ignore.");
            return;
        }

        // Remove the terminated session if present in pending list.
        // If a pending session is closed by AOSP framework due to dial error,
        // we remove the session from the list here.
        if (mPendingSessionList.remove(callSession)) {
            Log.i(this, "Removing pending session on close " + callSession);
            notifyCallRemoved(callSession);
        }
    }

    private void removeAndNotifySessions(
            Collection <? extends QImsSessionBase> list, boolean needToClose, String listName) {
        if (list == null) {
            return;
        }
        for (Iterator<? extends QImsSessionBase> it = list.iterator(); it.hasNext(); ) {
            QImsSessionBase session = it.next();
            Log.i(this, "removeAndNotifySessions : " + session + " from " + listName);
            if (needToClose) {
                session.terminate(ImsReasonInfo.CODE_LOCAL_INTERNAL_ERROR);
                session.close();
            }
            it.remove();
            if (session instanceof ImsCallSessionImpl) {
                notifyCallRemoved((ImsCallSessionImpl)session);
            } else if (session instanceof ImsUssdSessionImpl) {
                notifyUssdRemoved((ImsUssdSessionImpl)session);
            }
        }
    }

    private void removeCallSessionsAfterSrvcc() {
        Log.i(this, "removeCallSessionsAfterSrvcc");
        // For all the states after DIALING state is reported by RIL
        // Do not close the sessions here. ATEL fwk will close it.
        removeAndNotifySessions(mCallList.values(), false, "call list");

        // When there is a pending session waiting for a call object with DIALING state
        removeAndNotifySessions(mPendingSessionList, false, "pending list");
    }

    public void onUssdClosed(QImsSessionBase session) {
        Log.i(this, "onUssdClosed for session " + session);
        if (mUssdList != null) {
            if (mUssdList.remove((ImsUssdSessionImpl)session)) {
                Log.i(this, "Removing session on close " + session);
                notifyUssdRemoved((ImsUssdSessionImpl)session);
            }
        }
    }

    public void handleModifyCallRequest(CallModify cm) {
        if (cm != null) {
            ImsCallSessionImpl callSession = null;
            callSession = mCallList.get(Integer.toString(cm.call_index));
            if (callSession != null) {
                callSession.onReceivedModifyCall(cm);
            } else {
                Log.e(this,"handleModifyCallRequest Error: callSession is null");
            }
        } else {
            Log.e(this,"handleModifyCallRequest Error: Null Call Modify request ");
        }
    }

    /**
     * Create a call session
     * @param profile - ImsCallProfile associated with this call
     * @param listener - listner for the call session
     * @return IImsCallSession object or null on failure
     */
    public ImsCallSessionImplBase createCallSession(ImsCallProfile profile,
            UssdInfo info) {
        QImsSessionBase session =
            ((profile.getCallExtraInt(ImsCallProfile.EXTRA_DIALSTRING) ==
                      ImsCallProfile.DIALSTRING_USSD) || (info != null)) ?
            createUssdSession(profile, info) :
            createCallSession(profile);
        return (ImsCallSessionImplBase)session;
    }

    private QImsSessionBase createCallSession(ImsCallProfile profile) {
        ImsCallSessionImpl session = new ImsCallSessionImpl(profile, mCi, mContext,
                this, mIsVideoSupported, mServiceSub.getPhoneId(),
                mServiceSub.getImsConferenceController());
        session.addListener(mCallListener);
        session.updateFeatureCapabilities(mIsVideoSupported, mIsVoiceSupported);
        mPendingSessionList.add(session);
        notifyCallAdded(session);
        return session;
    }

    private QImsSessionBase createUssdSession(ImsCallProfile profile, UssdInfo info) {
        ImsUssdSessionImpl session = new ImsUssdSessionImpl(profile, mContext,
                mCi, mServiceSub.getPhoneId(), this, info);
        session.addListener(mUssdListener);
        session.updateFeatureCapabilities(mIsVideoSupported, mIsVoiceSupported);
        mUssdList.add(session);
        notifyUssdAdded(session);
        return session;
    }

    /**
     * Get a call session associated with the callId
     * @param callId
     * @return IImsCallSession object
     */
    public ImsCallSessionImpl getCallSession(String callId) {
        ImsCallSessionImpl session = null;
        session = mCallList.get(callId);
        return session;
    }

    /**
     * Handle the call state changes for incoming (MT) Hold/Resume as part of
     * the UNSOL_SUPP_SVC_NOTIFICATION message.
     * TODO: Handle other supp_svc info here?
     *
     * @param info
     */
    public void handleSuppSvcUnsol(SuppNotifyInfo info) {
        Log.i(this, "handleSuppSvcUnsol connId= " + info.getConnId());
        ImsCallSessionImpl callSession =
                callSession = mCallList.get(Integer.toString(info.getConnId()));
        if (callSession != null) {
            boolean startOnHoldLocalTone = false;
            final String forwardedCallHistory = info.getHistoryInfo();
            String[] callHistory = null;
            if (forwardedCallHistory != null && !forwardedCallHistory.isEmpty() ) {
                callHistory = forwardedCallHistory.split("\r\n");
            }
            ImsSuppServiceNotification suppServiceInfo = new ImsSuppServiceNotification(
                    info.getNotificationType(), info.getCode(), info.getIndex(),
                    info.getNotificationType(), info.getNumber(), callHistory);

            if (info.hasHoldTone()) {
                startOnHoldLocalTone = info.getHoldTone();
            }
            Log.i(this, "handleSuppSvcUnsol suppNotification= " + suppServiceInfo +
                  " startOnHoldLocalTone = " + startOnHoldLocalTone);
            callSession.updateSuppServiceInfo(suppServiceInfo, startOnHoldLocalTone);
        } else {
            Log.e(this, "No call session found for number: "/* + info.getNumber()*/);
        }
    }

    /**
     * Handle the TTY mode changes as part of the UNSOL_TTY_NOTIFICATION message.
     *
     * @param mode the mode informed via the indication
     */
    public void handleTtyModeChangeUnsol(int mode) {
        ImsCallSessionImpl callSession = null;

        // Check if any call session is active.
        for (Iterator<Map.Entry<String, ImsCallSessionImpl>> it =
                mCallList.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, ImsCallSessionImpl> e = it.next();
            // Call active
            if (e.getValue().getState() == ImsCallSessionImplBase.State.ESTABLISHED) {
                callSession = (ImsCallSessionImpl) e.getValue();
                callSession.notifyTtyModeChange(mode);
                break;
            }
        }

        if (callSession == null) {
            Log.e(this, "No Active call session found for TTY mode change");
        }
    }

    public ImsCallSessionImpl getCallSessionWithMptyBitSet(int state) {
        for (Iterator<Map.Entry<String, ImsCallSessionImpl>> it = mCallList.entrySet()
                .iterator(); it.hasNext();) {
            Map.Entry<String, ImsCallSessionImpl> e = it.next();
            ImsCallSessionImpl session = (ImsCallSessionImpl) e.getValue();
            DriverCallIms.State dcState = session.getDriverCallState();
            Log.i(this, "getCallSessionWithMptyBitSet:: ImsCallSession state = "
                    + session.getState() + ", isMultiparty = " + session.isMultiparty());

            if (session.isMultiparty() == true) {
                Log.i(this, "ImsCallSession found with Multiparty bit set");
                if ((dcState == DriverCallIms.State.DIALING ||
                        dcState == DriverCallIms.State.ALERTING ||
                        dcState == DriverCallIms.State.ACTIVE)
                        && (state == ConfInfo.FOREGROUND)) {
                    Log.i(this, "Foreground Conference callSession found");
                    return session;
                } else if ((dcState == DriverCallIms.State.HOLDING)
                        && (state == ConfInfo.BACKGROUND)) {
                    Log.i(this, "Background Conference callSession found");
                    return session;
                } else if ((dcState == DriverCallIms.State.INCOMING ||
                        dcState == DriverCallIms.State.WAITING)
                        && (state == ConfInfo.RINGING)) {
                    Log.i(this, "Ringing Conference callSession found");
                    return session;
                }
            }
        }

        return null;
    }

    /**
     * Gets list of call sessions that are in the given state.
     * @param state State of the call.
     * @return List of call session objects that have {@code state}
     */
    public List<ImsCallSessionImpl> getCallSessionByState(DriverCallIms.State state) {
        List<ImsCallSessionImpl> sessionList = new ArrayList<ImsCallSessionImpl>();
        if (state == null) return sessionList;

        for (ImsCallSessionImpl session : mPendingSessionList) {
            if (session.getInternalState() == state) {
                sessionList.add(session);
            }
        }

        for (Map.Entry<String, ImsCallSessionImpl> e : mCallList.entrySet()) {
            final ImsCallSessionImpl session = e.getValue();
            if (session.getInternalState() == state) {
                sessionList.add(session);
            }
        }
        return sessionList;
    }

    /**
     * Gets a call session with give media id.
     * @param mediaId Media id of the session to be searched.
     * @return Call session with {@code mediaId}
     */
    public ImsCallSessionImpl findSessionByMediaId(int mediaId) {
        for (Map.Entry<String, ImsCallSessionImpl> e : mCallList.entrySet()) {
            final ImsCallSessionImpl session = e.getValue();
            if (session.getMediaId() == mediaId) {
                return session;
            }
        }
        return null;
    }

    // For SRVCC indication source and target RATs will be invalid.
    private static boolean isSrvcc(HoInfo hoInfo) {
        return hoInfo.getSrcTech() == RadioTech.RADIO_TECH_INVALID &&
                hoInfo.getTargetTech() == RadioTech.RADIO_TECH_INVALID;
    }

    public void handleHandover(HoInfo handover) {
        Log.i(this, "in handleHandover");
        if (isSrvcc(handover)) {
            if (handover.getType() == HoInfo.COMPLETE_SUCCESS) {
                removeCallSessionsAfterSrvcc();
            }
            // srvcc indications are not notified to ImsCallSessionImpl
            return;
        }

        Log.i(this, "hoState: " + handover.getType() +
                " srcTech: " + handover.getSrcTech() +
                " tarTech: " + handover.getTargetTech() +
                " extraType: " + handover.getExtraType() +
                " extraInfo: " + handover.getExtraInfo() +
                " ErrorCode: " + handover.getErrorCode() +
                " errorMessage: " + handover.getErrorMessage());

        boolean showHandoverToast = false;
        for (Iterator<Map.Entry<String, ImsCallSessionImpl>> it = mCallList.entrySet()
                .iterator(); it.hasNext();) {
            Map.Entry<String, ImsCallSessionImpl> e = it.next();
            ImsCallSessionImpl callSession = (ImsCallSessionImpl) e.getValue();
            if (callSession == null) {
                Log.i(this, "handleHandover: null callsession. Key = " + e.getKey());
                continue;
            }
            callSession.handleHandover(handover.getType(), handover.getSrcTech(),
                    handover.getTargetTech(), handover.getExtraType(),
                    handover.getExtraInfo(),
                    handover.getErrorCode(), handover.getErrorMessage());
            if (!showHandoverToast && callSession.isInCall()) {
                showHandoverToast = true;
            }
        }

        if (showHandoverToast &&
                handover.getType() == HoInfo.COMPLETE_SUCCESS &&
                (handover.getSrcTech() == RadioTech.RADIO_TECH_WIFI ||
                 handover.getSrcTech() == RadioTech.RADIO_TECH_IWLAN) &&
                 (handover.getTargetTech() == RadioTech.RADIO_TECH_LTE ||
                   handover.getTargetTech() == RadioTech.RADIO_TECH_NR5G)) {
            Log.i(this, "Switching to LTE network for better quality");
        }
    }

    /**
     * Registers call list listener.
     * @param listener Listener to registered
     * @see ICallListListener
     */
    /* package */void addListener(ICallListListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("addListener error: listener is null.");
        }

        // Note: This will do linear search, O(N).
        // This is acceptable since arrays size is small.
        if (!mCallListListeners.contains(listener)) {
            mCallListListeners.add(listener);
        } else {
            Log.i(this,"addListener: Listener already added, " + listener);
        }
    }

    /**
     * Unregisters call list listener. Note: Only {@code ImsServiceClass.MMTEL} is supported.
     * @param listener Listener to unregistered
     * @see ICallListListener
     */
    /* package */void removeListener(ICallListListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("removeListener error: listener is null.");
        }

        // Note: This will do linear search, O(N).
        // This is acceptable since arrays size is small.
        if (mCallListListeners != null && mCallListListeners.contains(listener)) {
            mCallListListeners.remove(listener);
        } else {
            Log.e(this,"removeListener error: Listener not found, " + listener);
        }
    }

    // TODO Create CallList class and hide listeners, registration, notification in that class.
    private void notifyCallAdded(ImsCallSessionImpl session) {
        for (ICallListListener listener : mCallListListeners) {
            listener.onSessionAdded(session);
        }
    }

    private void addCall(Integer id, ImsCallSessionImpl session, boolean notify) {
        mCallList.put(id.toString(), session);
        if (notify) {
            notifyCallAdded(session);
        }
    }

    private void notifyCallRemoved(ImsCallSessionImpl session) {
        for (ICallListListener listener : mCallListListeners) {
            listener.onSessionRemoved(session);
        }
    }

    private void notifyUssdAdded(ImsUssdSessionImpl session) {
        mCallListListeners.forEach(listener -> {
            listener.onSessionAdded(session);
        });
    }

    private void notifyUssdRemoved(ImsUssdSessionImpl session) {
        mCallListListeners.forEach(listener -> {
            listener.onSessionRemoved(session);
        });
    }

    public void reportIncomingCall(ImsCallSessionImpl session, int index, boolean isUnknown) {
        Log.d(this, "reportIncomingCall :: session=" + session + " index=" + index
                + " isUnknown=" + isUnknown);
        mServiceSub.handleNotifyIncomingCall((ImsCallSessionImplBase)session,
                createIncomingCallBundle(false, isUnknown));
        addCall(index, session, true);
    }

    /**
     * Update the WiFi quality indication to call sessions
     *
     * @param quality the quality value received via the indication
     */
    public void updateVoWiFiCallQuality(int quality) {
        ImsCallSessionImpl callSession = null;

        /* Update the existing call sessions with quality value */
        for (Iterator<Map.Entry<String, ImsCallSessionImpl>> it =
                mCallList.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, ImsCallSessionImpl> e = it.next();
            callSession = (ImsCallSessionImpl) e.getValue();
            callSession.updateVoWiFiCallQuality(quality);
        }
    }

    public boolean isVoiceSupportedOverWifi() {
        return mServiceSub.isVoiceSupportedOverWifi();
    }

    public boolean isVideoSupportedOverWifi() {
        return mServiceSub.isVideoSupportedOverWifi();
    }

    public boolean isUssdSupported() {
        return mServiceSub.isUssdSupported();
    }

    public int getPhoneId() {
        return mServiceSub.getPhoneId();
    }

    public Executor getExecutor() {
        return mServiceSub.getExecutor();
    }

    @VisibleForTesting
    public SparseIntArray getPreAlertingCallTokenList() {
        return mPreAlertingCallTokenList;
    }

    private static int getToken() {
        sToken = (sToken == Integer.MAX_VALUE) ? 0 : sToken + 1;
        return sToken;
    }

    public void onPreAlertingCallDataAvailable(PreAlertingCallInfo info) {
        int token = getToken();
        mPreAlertingCallTokenList.put(info.getCallId(), token);
        broadcastPreAlertingCallIntent(info, token);
    }

    // Utility function to broadcast pre alerting call end intent if token valid
    private void maybeBroadcastPreAlertingCallIntent(int callId) {
        int token = mPreAlertingCallTokenList.get(callId, QtiCallConstants.INVALID_TOKEN);
        if (token != QtiCallConstants.INVALID_TOKEN) {
            broadcastPreAlertingCallIntent(token);
            mPreAlertingCallTokenList.delete(callId);
        }
    }

    // utility function to broadcast intent when pre alerting call ends
    private void broadcastPreAlertingCallIntent(int token) {
        broadcastPreAlertingCallIntent(null, token);
    }

    private void broadcastPreAlertingCallIntent(PreAlertingCallInfo info, int token) {
        Intent intent = new Intent(QtiCallConstants.ACTION_PRE_ALERTING_CALL_INFO);
        intent.putExtra(QtiCallConstants.EXTRA_PRE_ALERTING_CALL_TOKEN, token);
        intent.putExtra(QtiCallConstants.EXTRA_PRE_ALERTING_CALL_PHONE_ID, getPhoneId());

        if (info == null) {
            Log.d(this, "broadcastPreAlertingCallIntent for end state");
            intent.putExtra(QtiCallConstants.EXTRA_PRE_ALERTING_CALL_ENDED, true);
        } else {
            // Get call composer bundle
            Bundle ccExtra = CallComposerInfoUtils.toBundle(info.getCallComposerInfo());
            if (ccExtra != null) {
                intent.putExtra(QtiCallConstants.EXTRA_CALL_COMPOSER_INFO, ccExtra);
            }

            // Get ecnam bundle
            Bundle ecnamExtra = info.getEcnamInfo() == null ? null : info.getEcnamInfo().toBundle();
            if (ecnamExtra != null) {
                intent.putExtra(QtiCallConstants.EXTRA_CALL_ECNAM, ecnamExtra);
            }
            intent.putExtra(QtiCallConstants.EXTRA_DATA_CHANNEL_MODEM_CALL_ID,
                    info.getModemCallId());
            intent.putExtra(QtiCallConstants.EXTRA_IS_DATA_CHANNEL_CALL, info.getIsDcCall());
        }

        mContext.sendBroadcast(intent, "com.qti.permission.RECEIVE_PRE_ALERTING_CALL_INFO");
    }

    @Override
    public boolean handleMessage(Message msg) {
        Log.i (this, "Message received: what = " + msg.what);
        AsyncResult ar;
        try {
            switch (msg.what) {
                case EVENT_INCOMING_DTMF_START:
                    ar = (AsyncResult) msg.obj;
                    handleIncomingDtmf(ar, true/*isStart*/);
                    break;
                case EVENT_INCOMING_DTMF_STOP:
                    ar = (AsyncResult) msg.obj;
                    handleIncomingDtmf(ar, false/*isStart*/);
                    break;
                case EVENT_CLOSE_ALL_SESSIONS:
                    handleCloseAllSessions();
                    break;
                case EVENT_SRTP_ENCRYPTION_UPDATE:
                    ar = (AsyncResult) msg.obj;
                    handleSrtpEncryptionInfo(ar);
                    break;
                default:
                    Log.i(this, "Unknown message = " + msg.what);
            }
        }  catch (Exception ex) {
            Log.e(this, "handleMessage: Exception: " + ex);
        }
        return true;
    }

    private void handleIncomingDtmf(AsyncResult ar, boolean isStart) {
        if (ar == null || ar.result == null) {
            Log.e(this, "handleIncomingDtmf exception");
            return;
        }
        final SomeArgs args = (SomeArgs) ar.result;
        if (ar.exception != null) {
            Log.e(this, "handleIncomingDtmf ar.exception not null");
            args.recycle();
            return;
        }
        String dtmf = (String) args.arg1;
        ImsCallSessionImpl callSession = getCallSession(Integer.toString(args.argi1));
        if(callSession == null) {
            Log.e(this, "handleIncomingDtmf Error: callSession is null");
            return;
        }
        callSession.notifyIncomingDtmf(isStart, dtmf);
        args.recycle();
    }

    @VisibleForTesting
    public Handler getHandler() {
        return mHandler;
    }

    public void closeAllSessions() {
        mHandler.sendEmptyMessage(EVENT_CLOSE_ALL_SESSIONS);
    }

    private void handleCloseAllSessions() {
        Log.i(this, "handleCloseAllSessions");
        removeAndNotifySessions(mCallList.values(), true, "call list");

        removeAndNotifySessions(mPendingSessionList, true, "pending call list");

        removeAndNotifySessions(mUssdList, true, "ussd list");
    }

    private void handleSrtpEncryptionInfo(AsyncResult ar) {
        if (ar == null || ar.result == null) {
            Log.e(this, " handleSrtpEncryptionInfo exception");
            return;
        }
        if (ar.exception != null) {
            Log.e(this, "handleSrtpEncryptionInfo ar.exception not null");
            return;
        }
        final SomeArgs args = (SomeArgs) ar.result;
        int callId = args.argi1;
        int category = args.argi2;
        ImsCallSessionImpl callSession = getCallSession(Integer.toString(callId));
        if (callSession == null) {
             Log.e(this, "handleSrtpEncryptionInfo Error: callSession is not found");
             args.recycle();
             return;
        }
        callSession.notifySrtpEncryptionUpdate(category);
        args.recycle();
    }
}
