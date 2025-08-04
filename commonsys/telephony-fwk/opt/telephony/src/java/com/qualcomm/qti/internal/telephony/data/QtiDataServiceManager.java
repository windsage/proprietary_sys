/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 * Not a Contribution.
 */

/*
 * Copyright 2021 The Android Open Source Project
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
import android.annotation.Nullable;
import android.net.LinkProperties;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.RegistrantList;
import android.os.RemoteException;
import android.telephony.AccessNetworkConstants;
import android.telephony.AccessNetworkConstants.TransportType;
import android.telephony.data.ApnSetting;
import android.telephony.data.DataCallResponse;
import android.telephony.data.DataProfile;
import android.telephony.data.DataServiceCallback;
import android.telephony.data.NetworkSliceInfo;
import android.telephony.data.Qos;
import android.telephony.data.TrafficDescriptor;
import android.util.Pair;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.data.DataServiceManager;
import com.android.telephony.Rlog;
import com.qualcomm.qti.internal.telephony.QtiPhoneUtils;
import com.qualcomm.qti.internal.telephony.QtiTelephonyComponentFactory;

import java.util.HashMap;
import java.util.Map;

public class QtiDataServiceManager extends DataServiceManager {

    private static final int EVENT_GET_QOS_PARAMETERS_DONE = 101;
    private static final int EVENT_QOS_PARAMETERS_CHANGED  = 102;
    private static final int EVENT_RADIO_OFF_OR_NOT_AVAILABLE = 103;
    private static final int EVENT_MODEM_RESET = 104;
    private static final int EVENT_RADIO_UNAVAILABLE = 105;
    private static final int DEFAULT_PHONE_INDEX = 0;
    private static final String PROPERTY_FETCH_QOS_PARAMETERS = "persist.vendor.radio.fetchqos";
    public static final String DATA_CALL_RESPONSE = "data_call_response";

    /* Registrant list used to receive QoS parameters changed indications */
    private final RegistrantList mQosParametersChangedRegistrants = new RegistrantList();

    private boolean mIsFetchQosPropertyEnabled = false;

    /**
     * HashMap of cid to FrameworkQosParameters
     * This stores the latest QoS information available for a particular cid.
     */
    Map<Integer, FrameworkQosParameters> mQosParamsbyCid = new HashMap<>();

    public QtiDataServiceManager(@NonNull Phone phone, @NonNull Looper looper,
            @TransportType int transportType) {
        super(phone, looper, transportType);
        mTag = "Qti" + mTag;
        updateQosPropertyValue();
        if (shouldFetchQosParameters()) {
            registerReceivers();
            registerListenerForQtiPhoneReady();
        }
    }

    /**
     * Read the value of property "persist.vendor.radio.fetchqos" from vendor RIL.
     */
    private void updateQosPropertyValue() {
        try {
            mIsFetchQosPropertyEnabled = QtiTelephonyComponentFactory
                    .getInstance()
                    .getRil(DEFAULT_PHONE_INDEX)
                    .getPropertyValueBool(PROPERTY_FETCH_QOS_PARAMETERS, false);
            log("mIsFetchQosPropertyEnabled: " + mIsFetchQosPropertyEnabled);
        } catch (RemoteException | NullPointerException ex) {
            loge("Exception while reading Qos property: " + ex);
        }
    }

    /**
     * QoS parameters need to be explicitly fetched only when the property
     * "persist.vendor.radio.fetchqos" is true and the current transport type is for cellular.
     * Package visible so that it is accessible to QtiDcController as well.
     */
    boolean shouldFetchQosParameters() {
        return mIsFetchQosPropertyEnabled
                && getTransportType() == AccessNetworkConstants.TRANSPORT_TYPE_WWAN;
    }

    /**
     * Register for modem unavailable/off/reset events.
     * These will be used as triggers to clear {@link mQosParamsbyCid} map.
     */
    private void registerReceivers() {
        log("Register modem events");
        // Detect APM enable
        mPhone.mCi.registerForOffOrNotAvailable(this, EVENT_RADIO_OFF_OR_NOT_AVAILABLE, null);

        // Detect modem SSR
        mPhone.mCi.registerForModemReset(this, EVENT_MODEM_RESET, null);

        // Detect RILD reset
        mPhone.mCi.registerForNotAvailable(this, EVENT_RADIO_UNAVAILABLE, null);
    }

    /**
     * Register a listener with QtiPhoneUtils to know when it gets connected to ExtTelephonyService
     */
    void registerListenerForQtiPhoneReady() {
        log("registerListenerForQtiPhoneReady");
        QtiPhoneUtils.addOnQtiPhoneReadyListener(this::onQtiPhoneReady);
    }

    /**
     * Called when QtiPhoneUtils is created and it has connected to ExtTelephonyService
     * This will be called once during boot when qti phone process starts,
     * and later whenever qti phone process recovers after a crash.
     */
    private void onQtiPhoneReady() {
        // QtiPhoneUtils is now up. Attempt registering QoS related callback.
        log("onQtiPhoneReady");
        registerQosCallback();
    }

    /**
     * This registers a callback with QtiPhoneUtils to receive Qos Parameters changed indication.
     */
    private void registerQosCallback() {
        try {
            log("Registering QoS callback");
            QtiPhoneUtils qtiPhoneUtils = QtiPhoneUtils.getInstance();
            QtiCellularDataServiceCallback callback =
                    new QtiCellularDataServiceCallback("onQosParametersChanged");
            qtiPhoneUtils.registerDataServiceCallbackForQos(mPhone.getPhoneId(), callback);
        } catch (RuntimeException ex) {
            loge("registerQosCallback: Error connecting to qti phone.");
        }
    }

    /**
     * Overridden from the superclass to accommodate fetching of QoS parameters
     *
     * Setup a data connection. The data service provider must implement this method to support
     * establishing a packet data connection. When completed or error, the service must invoke
     * the provided callback to notify the platform.
     *
     * @param accessNetworkType Access network type that the data call will be established on.
     *        Must be one of {@link AccessNetworkConstants.AccessNetworkType}.
     * @param dataProfile Data profile used for data call setup. See {@link DataProfile}
     * @param isRoaming True if the device is data roaming.
     * @param allowRoaming True if data roaming is allowed by the user.
     * @param reason The reason for data setup. Must be {@link DataService#REQUEST_REASON_NORMAL} or
     *        {@link DataService#REQUEST_REASON_HANDOVER}.
     * @param linkProperties If {@code reason} is {@link DataService#REQUEST_REASON_HANDOVER}, this
     *        is the link properties of the existing data connection, otherwise null.
     * @param pduSessionId The pdu session id to be used for this data call.  A value of -1 means
     *                     no pdu session id was attached to this call.
     *                     Reference: 3GPP TS 24.007 Section 11.2.3.1b
     * @param sliceInfo The slice that represents S-NSSAI.
     *                  Reference: 3GPP TS 24.501
     * @param trafficDescriptor The traffic descriptor for this data call, used for URSP matching.
     *                          Reference: 3GPP TS TS 24.526 Section 5.2
     * @param matchAllRuleAllowed True if using the default match-all URSP rule for this request is
     *                            allowed.
     * @param onCompleteMessage The result message for this request. Null if the client does not
     *        care about the result.
     */
    @Override
    public void setupDataCall(int accessNetworkType, DataProfile dataProfile, boolean isRoaming,
            boolean allowRoaming, int reason, LinkProperties linkProperties, int pduSessionId,
            @Nullable NetworkSliceInfo sliceInfo, @Nullable TrafficDescriptor trafficDescriptor,
            boolean matchAllRuleAllowed, Message onCompleteMessage) {
        if (DBG) log("setupDataCall from QtiDataServiceManager");
        if (!mBound) {
            loge("setupDataCall: Data service not bound.");
            sendCompleteMessage(onCompleteMessage, DataServiceCallback.RESULT_ERROR_ILLEGAL_STATE);
            return;
        }

        QtiCellularDataServiceCallback callback =
                new QtiCellularDataServiceCallback("setupDataCall");

        if (onCompleteMessage != null) {
            if (shouldFetchQosParameters()) {
                // If the current data call request is for IMS apn, store this in the message.
                // This will be used to determine if QoS parameters need to be fetched while
                // handling the response of this request.
                boolean isIms =
                        (dataProfile.getSupportedApnTypesBitmask() & ApnSetting.TYPE_IMS) != 0;
                onCompleteMessage.arg1 = isIms ? 1 : 0;
                log("setupDataCall request"
                        + ", apnmask: " + dataProfile.getSupportedApnTypesBitmask()
                        + ", isIms: " + isIms);
            }
            mMessageMap.put(callback.asBinder(), onCompleteMessage);
        }
        try {
            mIDataService.setupDataCall(mPhone.getPhoneId(), accessNetworkType, dataProfile,
                    isRoaming, allowRoaming, reason, linkProperties, pduSessionId, sliceInfo,
                    trafficDescriptor, matchAllRuleAllowed, callback);
        } catch (RemoteException e) {
            loge("setupDataCall: Cannot invoke setupDataCall on data service.");
            mMessageMap.remove(callback.asBinder());
            sendCompleteMessage(onCompleteMessage, DataServiceCallback.RESULT_ERROR_ILLEGAL_STATE);
        }
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case EVENT_GET_QOS_PARAMETERS_DONE:
                log("EVENT_GET_QOS_PARAMETERS_DONE");
                onGetQosParametersResponse(msg);
                break;
            case EVENT_QOS_PARAMETERS_CHANGED:
                log("EVENT_QOS_PARAMETERS_CHANGED");
                onQosParametersChanged(msg);
                break;
            case EVENT_RADIO_OFF_OR_NOT_AVAILABLE:
                // fallthrough
            case EVENT_MODEM_RESET:
                // fallthrough
            case EVENT_RADIO_UNAVAILABLE:
                resetQosParamsCache(msg.what);
                break;
            default:
                super.handleMessage(msg);
        }
    }

    /**
     * Requests for QoS parameters from the vendor RIL.
     * For applicable cases, this is called immediately after response to setupDataCall() request
     * is received from the lower layers. If there is any exception met while querying for the QoS
     * parameters, the setupDataCallonCompleteMessage will be sent with the original result from
     * the setupDataCall response.
     *
     * @param cid Connection id of the data call for which Qos parameters need to be fetched
     * @param setupDataCallResultCode Result code received in the setupDataCall
     *                                response for this data call
     * @param setupDataCallonCompleteMessage The result message for the setupDataCall request that
     *                                       originated this request.
     */
    private void requestQosParameters(int cid, int setupDataCallResultCode,
                                      Message setupDataCallonCompleteMessage) {
        if (DBG) log("Request QoS params for cid: " + cid);
        try {
            QtiPhoneUtils qtiPhoneUtils = QtiPhoneUtils.getInstance();
            Message qosParamsMsg = obtainMessage(EVENT_GET_QOS_PARAMETERS_DONE,
                    cid, setupDataCallResultCode, setupDataCallonCompleteMessage);
            qtiPhoneUtils.getQosParameters(mPhone.getPhoneId(), cid, qosParamsMsg);
        } catch (RuntimeException ex) {
            Rlog.e(mTag, "requestQosParameters, caught error while fetching parameters." + ex);
            sendCompleteMessage(setupDataCallonCompleteMessage, setupDataCallResultCode);
            return;
        } catch (RemoteException ex) {
            Rlog.e(mTag, "requestQosParameters, caught error while connecting to qtiphone.", ex);
            sendCompleteMessage(setupDataCallonCompleteMessage, setupDataCallResultCode);
            return;
        }
    }

    /**
     * Handles the response of {@link QtiPhoneUtils#getQosParameters} request initiated
     * from {@link requestQosParameters}.
     *
     * @param qosParamsMsg Message containing the QoS parameters.
     * Message:
     *  arg1  cid
     *  arg2  setupDataCallResultCode
     *  obj   AsyncResult:
     *          userObj: onCompleteMessage from setupDataCall,
     *          result: FrameworkQosParameters or null
     *          exception
     */
    private void onGetQosParametersResponse(Message qosParamsMsg) {
        AsyncResult ar = (AsyncResult) qosParamsMsg.obj;
        if (ar == null) {
            // This is really unexpected, and we are left in a limbo state
            // Simply return from here as there is nothing that can be done.
            return;
        }

        Message onCompleteMessage = (Message) ar.userObj;
        if (onCompleteMessage == null || onCompleteMessage.getData() == null) {
            loge("Unable to find the message for setup data call response.");
            return;
        }

        int setupDataCallResultCode = qosParamsMsg.arg2;
        if (ar.exception != null) {
            // Exception while fetching Qos parameters. Send response of setupDataCall as it is.
            Rlog.e(mTag, "onGetQosParametersResponse, caught exception.", ar.exception);
            sendCompleteMessage(onCompleteMessage, setupDataCallResultCode);
        }

        // Get the QoS parameters from the response message
        FrameworkQosParameters qosParams = (FrameworkQosParameters) ar.result;
        if (qosParams == null) {
            // For some reason, we received a null value for the Qos parameters
            // Send response of setupDataCall as it is.
            sendCompleteMessage(onCompleteMessage, setupDataCallResultCode);
        }

        // Retrieve DataCallResponse for which these QoS parameters were queried
        DataCallResponse dataCallResponse = onCompleteMessage
                .getData().getParcelable(DATA_CALL_RESPONSE);

        // Save the Qos parameters in our cache
        addToQosParamsMap(dataCallResponse.getId(), qosParams);

        // Append QoS parameters to this DataCallResponse
        DataCallResponse modifiedDataCallResponse =
                appendQosParamsToDataCallResponse(dataCallResponse, qosParams);

        // Replace the DataCallResponse object in the message
        onCompleteMessage.getData().putParcelable(DATA_CALL_RESPONSE, modifiedDataCallResponse);

        // Send the combined response to the caller of the setupDataCall request
        sendCompleteMessage(onCompleteMessage, setupDataCallResultCode);
    }

    /**
     * This method appends the QoS parameters to an instance of DataCallResponse
     *
     * @param oldDataCallResponse DataCallResponse received from RIL
     * @param qosParams QoS parameters that need to be appended to oldDataCallResponse
     */
    DataCallResponse appendQosParamsToDataCallResponse(DataCallResponse oldDataCallResponse,
                                                       FrameworkQosParameters qosParams) {
        // If oldDataCallResponse or qosParams is null, there is no need to append anything
        if (oldDataCallResponse == null || qosParams == null) {
            return oldDataCallResponse;
        }

        log("appendQosParamsToDataCallResponse:"
                + ", DataCallResponse: " + oldDataCallResponse
                + ", QoS parameters: " + qosParams);

        return new DataCallResponse.Builder()
                .setCause(oldDataCallResponse.getCause())
                .setRetryDurationMillis(oldDataCallResponse.getRetryDurationMillis())
                .setId(oldDataCallResponse.getId())
                .setLinkStatus(oldDataCallResponse.getLinkStatus())
                .setProtocolType(oldDataCallResponse.getProtocolType())
                .setInterfaceName(oldDataCallResponse.getInterfaceName())
                .setAddresses(oldDataCallResponse.getAddresses())
                .setDnsAddresses(oldDataCallResponse.getDnsAddresses())
                .setGatewayAddresses(oldDataCallResponse.getGatewayAddresses())
                .setPcscfAddresses(oldDataCallResponse.getPcscfAddresses())
                .setMtu(oldDataCallResponse.getMtu())
                .setMtuV4(oldDataCallResponse.getMtuV4())
                .setMtuV6(oldDataCallResponse.getMtuV6())
                .setHandoverFailureMode(oldDataCallResponse.getHandoverFailureMode())
                .setPduSessionId(oldDataCallResponse.getPduSessionId())
                .setDefaultQos(qosParams.getDefaultQos())
                .setQosBearerSessions(qosParams.getQosBearerSessions())
                .setSliceInfo(oldDataCallResponse.getSliceInfo())
                .setTrafficDescriptors(oldDataCallResponse.getTrafficDescriptors())
                .build();
    }

    /**
     * Append QoS parameters from cache to the provided DataCallResponse if needed.
     * Overridden from super class.
     *
     * @param cid The connection id
     * @param dataProfile The {@link DataProfile} for the network
     * @param sourceDataCallResponse the {@link DataCallResponse} that needs to be modified
     */
    public DataCallResponse appendQosParamsToDataCallResponseIfNeeded(int cid,
            DataProfile dataProfile, DataCallResponse sourceDataCallResponse) {
        if (dataProfile == null
                || sourceDataCallResponse == null
                || !shouldFetchQosParameters()
                || (dataProfile.getSupportedApnTypesBitmask()    // IMS APN check
                        & ApnSetting.TYPE_IMS) == 0) {
            return sourceDataCallResponse;
        }

        log("appendQosParamsToDataCallResponseIfNeeded:"
                + ", cid: " + cid
                + ", DataProfile: " + dataProfile
                + ", DataCallResponse: " + sourceDataCallResponse);

        return appendQosParamsToDataCallResponse(
            sourceDataCallResponse, getQosParamsFromMap(cid));
    }

    public class QtiCellularDataServiceCallback extends DataServiceCallbackWrapper {

        QtiCellularDataServiceCallback(String tag) {
            super(tag);
        }

        @Override
        public void onSetupDataCallComplete(@DataServiceCallback.ResultCode int resultCode,
                                            DataCallResponse response) {
            if (DBG) {
                log("QtiDataServiceManager: onSetupDataCallComplete"
                        + ", resultCode = " + resultCode
                        + ", response = " + response);
            }
            Message msg = mMessageMap.remove(asBinder());

            if (msg != null) {
                msg.getData().putParcelable(DATA_CALL_RESPONSE, response);

                boolean isIms = msg.arg1 == 1 ? true : false;
                log("response isIms: " + isIms);

                if (shouldFetchQosParameters() && isIms) {
                    requestQosParameters(response.getId(), resultCode, msg);
                } else {
                    sendCompleteMessage(msg, resultCode);
                }
            } else {
                loge("Unable to find the message for setup data call response.");
            }
        }

        /**
         * This callback is invoked from QtiPhoneUtils when QoS Parameters Changed
         * indication is received from lower layers
         *
         * @param cid Connection Id for the data call whose Qos parameters were changed
         * @param qosParams the new QoS parameters
         */
        public void onQosParametersChanged(int cid, FrameworkQosParameters qosParams) {
            if (qosParams == null) {
                log("onQosParametersChanged, qosParams is null");
                // TODO: maybe this will also be the case when QoS session is brought down.
                // See if this needs to be handled.
                return;
            }
            log("onQosParametersChanged, cid: " + cid + ", qosParams: " + qosParams);
            sendMessage(obtainMessage(EVENT_QOS_PARAMETERS_CHANGED, cid, -1, qosParams));
        }
    }

    /**
     * Informs QtiDcController that QoS parameters have changed for a cid
     */
    private void onQosParametersChanged(Message msg) {
        int cid = msg.arg1;
        FrameworkQosParameters qosParams = (FrameworkQosParameters) msg.obj;
        mQosParametersChangedRegistrants.notifyRegistrants(
                new AsyncResult(null,
                new Pair<Integer, FrameworkQosParameters>(cid, qosParams), null));
    }

    /*
     * Adds an entry to QoS parameters cache map {@link mQosParamsbyCid}.
     *
     * Package visible
     */
    void addToQosParamsMap(int cid, FrameworkQosParameters qosParams) {
        synchronized(mQosParamsbyCid) {
            mQosParamsbyCid.put(cid, qosParams);
        }
        log("Added QosParameters for cid " + cid + " into the map."
                + " QoS parameters: " + ((qosParams == null) ? "null" : qosParams));
    }

    /*
     * Removes an entry from QoS parameters cache map {@link mQosParamsbyCid}.
     *
     * Package visible
     */
    void removeFromQosParamsMap(int cid) {
        synchronized(mQosParamsbyCid) {
            if (mQosParamsbyCid.containsKey(cid)) {
                mQosParamsbyCid.remove(cid);
                log("Removed QosParameters for cid " + cid + " from map");
            } else {
                log("QosParameters for cid " + cid + " not present in map");
            }
        }
    }

    /*
     * Retrieves an entry from QoS parameters cache map {@link mQosParamsbyCid}.
     * Returns null if the entry is not present in the map.
     *
     * Package visible
     */
    @Nullable FrameworkQosParameters getQosParamsFromMap(int cid) {
        FrameworkQosParameters result = null;
        synchronized(mQosParamsbyCid) {
            if (!mQosParamsbyCid.containsKey(cid)) {
                log("No QosParameters available for cid " + cid + " in the map.");
                return null;
            }
            result = mQosParamsbyCid.get(cid);
        }
        return result;
    }

    /*
     * Clears all entries in QoS parameters cache map {@link mQosParamsbyCid}.
     * This is called when we want to reset our slate after modem's state
     * changes to unavailable/off/reset.
     */
    private void resetQosParamsCache(int what) {
        log("Resetting QoS parameters cache due to " + messageToString(what));
        synchronized(mQosParamsbyCid) {
            mQosParamsbyCid.clear();
        }
    }

    private String messageToString(int what) {
        switch(what) {
            case EVENT_RADIO_OFF_OR_NOT_AVAILABLE:
                return "EVENT_RADIO_OFF_OR_NOT_AVAILABLE";
            case EVENT_MODEM_RESET:
                return "EVENT_MODEM_RESET";
            case EVENT_RADIO_UNAVAILABLE:
                return "EVENT_RADIO_UNAVAILABLE";
            default:
                return Integer.toString(what);
        }
    }

    /**
     * Register for Qos parameters changed event.
     *
     * @param h The target to post the event message to.
     * @param what The event.
     */
    public void registerForQosParametersChanged(Handler h, int what) {
        if (h != null) {
            mQosParametersChangedRegistrants.addUnique(h, what, null);
        }
    }

    /**
     * Unregister for Qos parameters changed event.
     *
     * @param h The handler
     */
    public void unregisterForQosParametersChanged(Handler h) {
        if (h != null) {
            mQosParametersChangedRegistrants.remove(h);
        }
    }
}
