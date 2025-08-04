/*
 * Copyright (c) 2021-2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 * Not a Contribution.
 *
 * Copyright (C) 2017 The Android Open Source Project
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
 * limitations under the License
 *
 */

package org.codeaurora.ims;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.HashSet;
import java.util.List;

import org.codeaurora.telephony.utils.AsyncResult;

import android.content.Intent;
import android.content.Context;

import android.net.Uri;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.os.UserHandle;

import android.telephony.ims.ImsReasonInfo;

import com.qualcomm.ims.utils.Log;

/**
 *
 * This class acts as a controller for Registration related
 * events.
 *.
 */
public class ImsRegistrationController implements Handler.Callback {

    private Handler mHandler;
    private Context mContext;
    private ImsSenderRxr mCi = null;
    private HashSet<Uri> mSelfIndentityUris = null;
    private List<Listener> mListeners = new CopyOnWriteArrayList<>();

    private int mSrvDomain = ImsRegistrationUtils.NO_SRV;
    private int mRegistrationStatus = ImsRegistrationInfo.NOT_REGISTERED;
    private int mErrorCode = ImsReasonInfo.CODE_UNSPECIFIED;
    private ImsReasonInfo mDeregisteredInfo = null;

    //This is the error that we get from the modem
    //for IKEv2 Authentication failure(24).
    private static final int IKEv2_AUTH_FAILURE = 5;

    private final int EVENT_REGISTRATION_BLOCK_STATUS = 1;
    private final int EVENT_GEO_LOCATION_DATA_STATUS = 2;
    private final int EVENT_IMS_STATE_CHANGED = 3;
    private final int EVENT_IMS_STATE_DONE = 4;
    private final int EVENT_SRV_DOMAIN_CHANGED = 5;
    private final int EVENT_QUERY_IMS_REG_STATE = 6;

    // To decide whether to ignore the response of get registration state
    // due to a new unsol registration state change event.
    private boolean mIsQueryingRegState = false;

    public interface Listener {
        default void onRegistered(int registrationState,
            ImsReasonInfo imsReasonInfo, int imsRadioTech, boolean isBroadcast) {}
        default void onRegistering(int registrationState,
            ImsReasonInfo imsReasonInfo, int imsRadioTech, boolean isBroadcast) {}
        default void onDeregistered(int registrationState,
            ImsReasonInfo imsReasonInfo, int imsRadioTech, boolean isBroadcast) {}
        default void onTechnologyChangeFailed(int imsRadioTech, ImsReasonInfo info) {}
        default void onSubscriberAssociatedUriChanged(Uri[] uris) {}
    }

    public ImsRegistrationController(ImsSenderRxr imsSenderRxr, Context context) {
        this(imsSenderRxr, context, Looper.getMainLooper());
    }

    public ImsRegistrationController(ImsSenderRxr imsSenderRxr, Context context,
            Looper looper) {
        mCi = imsSenderRxr;
        mHandler = new Handler(looper, this);
        mContext = context;
        mCi.registerForRegistrationBlockStatus(mHandler, EVENT_REGISTRATION_BLOCK_STATUS, null);
        mCi.registerForGeoLocationDataStatus(mHandler, EVENT_GEO_LOCATION_DATA_STATUS, null);
        mCi.registerForImsNetworkStateChanged(mHandler, EVENT_IMS_STATE_CHANGED, null);
        mCi.registerForSrvDomainChanged(mHandler, EVENT_SRV_DOMAIN_CHANGED, null);
    }

    /**
     * Registers a listener from within IMS Service vendor, for updates.
     * @param listener Listener to registered
     */
    public void addListener(Listener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener is null.");
        }

        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
        } else {
            Log.e(this, "Duplicate listener, " + listener);
        }
    }

    /**
     * Unregisters listeners from within IMS Service vendor modules.
     * @param listener Listener to unregistered
     */
    public void removeListener(Listener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener is null.");
        }

        if (mListeners.contains(listener)) {
            mListeners.remove(listener);
        } else {
            Log.e(this, "Listener not found, " + listener);
        }
    }

    private void notifyOnRegistered(int registrationState,
            ImsReasonInfo imsReasonInfo, int imsRadioTech, boolean isBroadcast) {
        for (Listener l : mListeners) {
            l.onRegistered(registrationState, imsReasonInfo,
                           imsRadioTech, isBroadcast);
        }
    }

    private void notifyOnRegistering(int registrationState,
            ImsReasonInfo imsReasonInfo, int imsRadioTech, boolean isBroadcast) {
        for (Listener l : mListeners) {
            l.onRegistering(registrationState, imsReasonInfo,
                            imsRadioTech, isBroadcast);
        }
    }

    private void notifyOnDeregistered(int registrationState,
            ImsReasonInfo imsReasonInfo, int imsRadioTech, boolean isBroadcast) {
        for (Listener l : mListeners) {
            l.onDeregistered(registrationState, imsReasonInfo,
                             imsRadioTech, isBroadcast);
        }
    }

    private void notifyOnTechnologyChangeFailed(int imsRadioTech, ImsReasonInfo info) {
        for (Listener l : mListeners) {
            l.onTechnologyChangeFailed(imsRadioTech, info);
        }
    }

    private void notifyOnSubscriberAssociatedUriChanged(Uri[] uris) {
        for (Listener l : mListeners) {
            l.onSubscriberAssociatedUriChanged(uris);
        }
    }

    public void requestImsRegistrationState() {
        Log.v(this, "requestImsRegistrationState");
        mHandler.sendEmptyMessage(EVENT_QUERY_IMS_REG_STATE);
    }

    private void handleQueryImsRegistrationState() {
        mCi.getImsRegistrationState(mHandler.obtainMessage(EVENT_IMS_STATE_DONE));
        mIsQueryingRegState = true;
    }

    //Sends deregistarion notificationto AOSP.
    public void reset(ImsReasonInfo info) {
        Log.i(this, "reset");
        mRegistrationStatus = ImsRegistrationInfo.NOT_REGISTERED;
        mErrorCode = info.getCode();
        notifyRegChange(ImsRegistrationInfo.NOT_REGISTERED, info,
                RadioTech.RADIO_TECH_INVALID, false);
    }

    /**
     * Local utility to invoke corresponding listener callbacks.
     */
    private void notifyRegChange(final int registrationState,
            final ImsReasonInfo imsReasonInfo, final int imsRadioTech,
            final boolean isBroadcast) {
        switch (registrationState) {
            case ImsRegistrationInfo.REGISTERED:
                mDeregisteredInfo = null;
                notifyOnRegistered(registrationState, imsReasonInfo,
                        imsRadioTech, isBroadcast);
                break;
            case ImsRegistrationInfo.NOT_REGISTERED:
                mDeregisteredInfo = imsReasonInfo;
                maybeNotifySrvDomainChange(true);
                notifyOnDeregistered(registrationState, imsReasonInfo,
                        imsRadioTech, isBroadcast);
                break;
            case ImsRegistrationInfo.REGISTERING:
                mDeregisteredInfo = null;
                notifyOnRegistering(registrationState, imsReasonInfo,
                        imsRadioTech, isBroadcast);
                break;
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        Log.i(this, "Message received: what = " + msg.what);
        AsyncResult ar;

        switch (msg.what) {
            case EVENT_REGISTRATION_BLOCK_STATUS:
                ar = (AsyncResult) msg.obj;
                handleRegistrationBlockStatus(ar);
                break;
            case EVENT_GEO_LOCATION_DATA_STATUS:
                ar = (AsyncResult) msg.obj;
                handleGeoLocationDataStatus(ar);
                break;
            case EVENT_IMS_STATE_CHANGED:
                ar = (AsyncResult) msg.obj;
                if (ar.result == null) {
                    // Backward compatibility with old RIL API
                    handleQueryImsRegistrationState();
                } else {
                    // Reset this flag so that any pending get reg state response can be ignored.
                    // This is to avoid issues where wrong registration state is notified when
                    // the response for get request and the unsol reg state change events are
                    // received in the wrong order at ImsService.
                    mIsQueryingRegState = false;
                    handleImsStateChanged(ar);
                }
                break;
            case EVENT_IMS_STATE_DONE:
                if (mCi.isAidlReorderingSupported()) {
                    Log.d(this, "EVENT_IMS_STATE_DONE. Ignoring as relying on indication" +
                            " is supported");
                    break;
                }
                ar = (AsyncResult) msg.obj;
                if (mIsQueryingRegState) {
                    mIsQueryingRegState = false;
                    handleImsStateChanged(ar);
                } else {
                    Log.i(this, "EVENT_IMS_STATE_DONE. Ignoring due to new unsol event received");
                }
                break;
            case EVENT_SRV_DOMAIN_CHANGED:
                ar = (AsyncResult) msg.obj;
                handleSrvDomainChanged(ar);
                break;
            case EVENT_QUERY_IMS_REG_STATE:
                handleQueryImsRegistrationState();
                break;
            default:
                Log.i(this, "Unknown message = " + msg.what);
        }
        return true;
    }

    /**
     * Handles registration block status through UNSOL_RESPONSE_REGISTRATION_BLOCK_STATUS message
     * @param ar - the AsyncResult object that contains the registration block status information
     */
    private void handleRegistrationBlockStatus(AsyncResult ar) {
        if (ar == null || ar.exception != null || ar.result == null) {
            Log.e(this, "Async result is null or exception is not null.");
            return;
        }
        RegistrationBlockStatusInfo regBlockStatus = (RegistrationBlockStatusInfo) ar.result;
        // We only check and report wlan failures for now. This can be
        // extended to Wwan when we have an usecase for it.
        if(regBlockStatus.getStatusOnWlan() != null) {
            BlockStatusInfo blockStatus = regBlockStatus.getStatusOnWlan();
            //Check if it has block reason details. Block reason type is
            //ignored for now.
            if(isIKEv2Error(blockStatus)) {
                Log.i( this, "Permanent IWLAN reg failure (IKEv2 auth failure).");
                ImsReasonInfo imsReasonInfo = new ImsReasonInfo(
                        ImsReasonInfo.CODE_EPDG_TUNNEL_ESTABLISH_FAILURE,
                        ImsReasonInfo.CODE_IKEV2_AUTH_FAILURE,
                        null);
                notifyOnTechnologyChangeFailed(RadioTech.RADIO_TECH_IWLAN, imsReasonInfo);
            }
        }
    }

    private static boolean isIKEv2Error(BlockStatusInfo blockStatus) {
        //Check if it has block reason details. Block reason type is
        //ignored for now.
        if(blockStatus.getReasonDetails() != null) {
            BlockReasonDetailsInfo blockReasonDetails = blockStatus.getReasonDetails();
            //Check if it has registration failure reason type
            //and failure reason.
            return ((blockReasonDetails.getRegFailureReasonType() !=
                           BlockReasonDetailsInfo.REG_FAILURE_REASON_TYPE_INVALID) &&
                    (blockReasonDetails.getRegFailureReason() != Integer.MAX_VALUE) &&
                    //Check if there was an IKEv2 authentication failure while
                    //establishing tunnel for IWLAN ims registration.
                    (blockReasonDetails.getRegFailureReasonType() ==
                           BlockReasonDetailsInfo.REG_FAILURE_REASON_TYPE_IWLAN) &&
                    (blockReasonDetails.getRegFailureReason() == IKEv2_AUTH_FAILURE));
        }
        return false;
    }

    private void handleGeoLocationDataStatus(AsyncResult ar) {
        Log.i(this, "handleGeoLocationDataStatus");
        if (ar == null || ar.result == null) {
            Log.e(this, "Error EVENT_GEO_LOCATION_DATA_STATUS AsyncResult ar = " + ar);
            return;
        }

        int geoLocationStatus = (int) ar.result;
        ImsReasonInfo imsReasonInfo = new ImsReasonInfo(ImsReasonInfo.CODE_REGISTRATION_ERROR,
                geoLocationStatus, "");
        mRegistrationStatus = ImsRegistrationInfo.NOT_REGISTERED;
        mErrorCode = ImsReasonInfo.CODE_REGISTRATION_ERROR;
        notifyRegChange(ImsRegistrationInfo.NOT_REGISTERED, imsReasonInfo,
                RadioTech.RADIO_TECH_INVALID, false);
    }

    private void handleImsStateChanged(AsyncResult ar) {
        Log.i(this, "handleImsStateChanged");
        if (ar == null || !(ar.result instanceof ImsRegistrationInfo)) {
            Log.e(this, "handleImsStateChanged error");
            return;
        }
        int errorCode = ImsReasonInfo.CODE_UNSPECIFIED;
        String errorMessage = null;
        String selfIdentityUrisCombined = null;
        int regState = ImsRegistrationInfo.NOT_REGISTERED;
        String[] selfIdentityUriStrings = null;
        Uri[] selfIdentityUris = null;
        ImsRegistrationInfo registration = (ImsRegistrationInfo) ar.result;
        int imsRat = registration.getRadioTech();

        errorCode = registration.getErrorCode();
        errorMessage = registration.getErrorMessage();
        selfIdentityUrisCombined = registration.getPAssociatedUris();
        regState = registration.getState();
        if (selfIdentityUrisCombined != null) {
            selfIdentityUris =
                    ImsRegistrationUtils.extractUrisFromPipeSeparatedUriStrings(
                    selfIdentityUrisCombined);
        }

        ImsReasonInfo imsReasonInfo = new ImsReasonInfo(
                ImsReasonInfo.CODE_REGISTRATION_ERROR,
                errorCode, errorMessage);

        mRegistrationStatus = regState;
        if (regState == ImsRegistrationInfo.NOT_REGISTERED) {
            mErrorCode = ImsReasonInfo.CODE_REGISTRATION_ERROR;
        }
        notifyRegChange(regState, imsReasonInfo, imsRat, true);

        if (ImsRegistrationUtils.areSelfIdentityUrisDiff(
                    mSelfIndentityUris, selfIdentityUris)) {
            updateSelfIdentityUriCache(selfIdentityUris);
            notifyOnSubscriberAssociatedUriChanged(selfIdentityUris);
        }
    }

    /*
     * If IMS is deregistered then reuse the onDeregistered() callback
     * to pass to AOSP if the device is PS attached or not.
     * mDeregisteredInfo is used to pass the latest ImsReasonInfo
     * received from modem to AOSP.
     */
    private void maybeNotifySrvDomainChange(boolean isRegChange) {
        if (mRegistrationStatus != ImsRegistrationInfo.NOT_REGISTERED) {
            Log.i(this, "checkSrvDomainChange IMS not deregistered.");
            return;
        }
        int extraErrCode = ImsRegistrationUtils.convertToPsAttachedCode(mSrvDomain);
        ImsReasonInfo info = new ImsReasonInfo(mErrorCode, extraErrCode, null);
        notifyOnDeregistered(ImsRegistrationInfo.NOT_REGISTERED, info,
                RadioTech.RADIO_TECH_INVALID, false);
        if (!isRegChange) {
            notifyOnDeregistered(ImsRegistrationInfo.NOT_REGISTERED, mDeregisteredInfo,
                    RadioTech.RADIO_TECH_INVALID, false);
        }
    }

    private void updateSelfIdentityUriCache(Uri[] new_uris) {
        if (mSelfIndentityUris == null) {
            mSelfIndentityUris = new HashSet<Uri>();
        } else {
            mSelfIndentityUris.clear();
        }
        if (new_uris == null) {
            Log.d(this, "new_uris is null");
            return;
        }
        for (int i = 0; i < new_uris.length; i++) {
            mSelfIndentityUris.add(new_uris[i]);
            Log.i(this, "updateSelfIdentityUriCache :: "
                     + "new self-identity host URI=" + Log.pii(new_uris[i]));
        }
    }

    private void handleSrvDomainChanged(AsyncResult ar) {
        Log.i(this, "handleSrvDomainChanged");
        if (ar == null || ar.result == null) {
            Log.e(this, "Error EVENT_SRV_DOMAIN_CHANGED AsyncResult ar = " + ar);
            return;
        }

        int srvDomain = (int) ar.result;
        if (mSrvDomain != srvDomain) {
            mSrvDomain = srvDomain;
            maybeNotifySrvDomainChange(false);
        }
    }

    public void dispose() {
        Log.d(this, "dispose");
        mCi.unregisterForRegistrationBlockStatus(mHandler);
        mCi.unregisterForGeoLocationDataStatus(mHandler);
        mCi.unregisterForImsNetworkStateChanged(mHandler);
        mCi.unregisterForSrvDomainChanged(mHandler);
        mCi = null;
        mHandler = null;
        mSelfIndentityUris = null;
        mSrvDomain = ImsRegistrationUtils.NO_SRV;
        mRegistrationStatus = ImsRegistrationInfo.NOT_REGISTERED;
        mDeregisteredInfo = null;
    }
}
