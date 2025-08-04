/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.uim_remote_client;

import vendor.qti.hardware.radio.uim_remote_client.IUimRemoteServiceClientIndication;
import vendor.qti.hardware.radio.uim_remote_client.IUimRemoteServiceClientResponse;
import vendor.qti.hardware.radio.uim_remote_client.UimRemoteClientApduStatus;
import vendor.qti.hardware.radio.uim_remote_client.UimRemoteEventReqType;

@VintfStability
interface IUimRemoteServiceClient {
    /**
     * Uim remote client APDU request
     *
     * @param token Id to match req-resp. Resp must include same token.
     * @param apduStatus UIM APDU status
     * @param apduResponse APDU response
     */
    oneway void uimRemoteServiceClientApduReq(in int token,
        in UimRemoteClientApduStatus apduStatus, in byte[] apduResponse);

    /**
     * Uim remote client event requests
     *
     * @param token Id to match req-resp. Resp must include same token.
     * @param eventReq remote UIM event parameters
     */
    oneway void uimRemoteServiceClientEventReq(in int token, in UimRemoteEventReqType eventReq);

    /**
     * Set callback for uim remote service client requests and indications
     *
     * @param responseCallback Object containing response callback functions
     * @param indicationCallback Object containing uim remote client indication callback functions
     */
    void setCallback(in IUimRemoteServiceClientResponse responseCallback,
        in IUimRemoteServiceClientIndication indicationCallback);
}
