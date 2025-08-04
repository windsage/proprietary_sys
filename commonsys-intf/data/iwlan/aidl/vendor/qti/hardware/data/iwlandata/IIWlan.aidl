/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.hardware.data.iwlandata;

import vendor.qti.hardware.data.iwlandata.IIWlanIndication;
import vendor.qti.hardware.data.iwlandata.IIWlanResponse;
import android.hardware.radio.AccessNetwork;
import android.hardware.radio.data.DataProfileInfo;
import android.hardware.radio.data.DataRequestReason;
import android.hardware.radio.data.LinkAddress;
import android.hardware.radio.data.SliceInfo;

/**
 * Interface for mobile data service. Used with iWLAN.
 */
@VintfStability
interface IIWlan {

    /**
     *  Deactivate packet data connection.
     *
     * @param serial Serial number of request.
     * @param cid Call id returned in the callback of {@link DataServiceProvider#setupDataCall(
     * int32_t, DataProfile, boolean, boolean, boolean, LinkProperties, DataServiceCallback)}.
     * @param reason request reason for device shut down.
     */
    oneway void deactivateDataCall(in int serial, in int cid,
        in DataRequestReason reason);

    /**
     *  Returns the qualified networks
     *
     * @param serial Serial number of request.
     */
    oneway void getAllQualifiedNetworks(in int serial);

    /**
     *  Returns the data call list.
     *
     * @param serial Serial number of request.
     */
    oneway void getDataCallList(in int serial);

    /**
     *  Returns the network registration state.
     *
     * @param serial Serial number of request.
     */
    oneway void getDataRegistrationState(in int serial);

    /**
     * iWlan has been disabled by framework
     *
     */
    oneway void iwlanDisabled();

    /**
     * When response type received from a IWlan indication
     * acknowledge the receipt of those messages by sending responseAcknowledgement().
     *
     */
    oneway void responseAcknowledgement();

    /**
     *  Returns current transport status.
     *
     * @param dataResponse Object containing data service response functions
     * @param dataIndication Object containing data service indications
     */
    void setResponseFunctions(in IIWlanResponse dataResponse, in IIWlanIndication dataIndication);

    /**
     * Setup a packet data connection. If DataCallResponse.status returns DataCallFailCause:NONE,
     * the data connection must be added to data calls and a unsolDataCallListChanged() must be
     * sent. The call remains until removed by subsequent unsolDataCallIstChanged(). It may be lost
     * due to many factors, including deactivateDataCall() being issued, the radio powered off,
     * reception lost or even transient factors like congestion. This data call list is returned by
     * getDataCallList() and dataCallListChanged().
     * The Radio is expected to:
     * - Create one data call context.
     * - Create and configure a dedicated interface for the context.
     * - The interface must be point to point.
     * - The interface is configured with one or more addresses and is capable of sending and
     *   receiving packets. The format is IP address with optional "/" prefix length (The format is
     *   defined in RFC-4291 section 2.3). For example, "192.0.1.3", "192.0.1.11/16", or
     *   "2001:db8::1/64". Typically one IPv4 or one IPv6 or one of each. If the prefix length is
     *   absent, then the addresses are assumed to be point to point with IPv4 with prefix length 32
     *   or IPv6 with prefix length 128.
     * - Must not modify routing configuration related to this interface; routing management is
     *   exclusively within the purview of the Android OS.
     * - Support simultaneous data call context, with limits defined in the specifications. For LTE,
     *   the max number of data calls is equal to the max number of EPS bearers that can be active.
     *
     * @param serial Serial number of request.
     * @param accessNetwork The access network to setup the data call. If the data connection cannot
     *        be established on the specified access network then this should respond with an error.
     * @param dataProfileInfo Data profile info.
     * @param roamingAllowed Indicates whether or not data roaming is allowed by the user.
     * @param reason The request reason. Must be DataRequestReason:NORMAL or
     *        DataRequestReason:HANDOVER.
     * @param addresses If the reason is DataRequestReason:HANDOVER, this indicates the list of link
     *        addresses of the existing data connection. This parameter must be ignored unless
     *        reason is DataRequestReason:HANDOVER.
     * @param dnses If the reason is DataRequestReason:HANDOVER, this indicates the list of DNS
     *        addresses of the existing data connection. The format is defined in RFC-4291 section
     *        2.2. For example, "192.0.1.3" or "2001:db8::1". This parameter must be ignored unless
     *        reason is DataRequestReason:HANDOVER.
     * @param pduSessionId The pdu session id to be used for this data call. A value of 0 means no
     *        pdu session id was attached to this call. Reference: 3GPP TS 24.007 section 11.2.3.1b
     * @param sliceInfo SliceInfo to be used for the data connection when a handover occurs from
     *        EPDG to 5G. It is valid only when accessNetwork is AccessNetwork:NGRAN. If the slice
     *        passed from EPDG is rejected, then the data failure cause must be
     *        DataCallFailCause:SLICE_REJECTED.
     * @param matchAllRuleAllowed bool to indicate if using default match-all URSP rule for this
     *        request is allowed. If false, this request must not use the match-all URSP rule and if
     *        a non-match-all rule is not found (or if URSP rules are not available) it should
     *        return failure with cause DataCallFailCause:MATCH_ALL_RULE_NOT_ALLOWED. This is needed
     *        as some requests need to have a hard failure if the intention cannot be met, for
     *        example, a zero-rating slice.
     *
     * Response function is IRadioDataResponse.setupDataCallResponse()
     */
    oneway void setupDataCall(in int serial, in AccessNetwork accessNetwork,
                   in DataProfileInfo dataProfileInfo, in boolean roamingAllowed,
                   in DataRequestReason reason, in LinkAddress[]addresses, in String[] dnses,
                   in int pduSessionId, in @nullable SliceInfo sliceInfo,
                   in boolean matchAllRuleAllowed);

}