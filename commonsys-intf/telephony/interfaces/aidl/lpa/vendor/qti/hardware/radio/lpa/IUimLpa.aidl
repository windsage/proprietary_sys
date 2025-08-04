/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 */

package vendor.qti.hardware.radio.lpa;

import vendor.qti.hardware.radio.lpa.IUimLpaIndication;
import vendor.qti.hardware.radio.lpa.IUimLpaResponse;
import vendor.qti.hardware.radio.lpa.UimLpaHttpCustomHeader;
import vendor.qti.hardware.radio.lpa.UimLpaResult;
import vendor.qti.hardware.radio.lpa.UimLpaUserReq;

@VintfStability
interface IUimLpa {

    /**
     * UIM_LPA_HTTP_TXN_COMPLETED_REQUEST
     *
     * @param token Id to match req-resp. Resp must include same token.
     * @param result http transaction result
     * @param responsePayload response data
     * @param customHeaders http transaction customer headers
     *
     * Response function is IUimLpaResponse.uimLpaHttpTxnCompletedResponse()
     */
    oneway void uimLpaHttpTxnCompletedRequest(in int token, in UimLpaResult result,
        in byte[] responsePayload, in UimLpaHttpCustomHeader[] customHeaders);

    /**
     * UIM_LPA_USER_REQUEST
     *
     * @param token Id to match req-resp. Resp must include same token.
     * @param userReq User request data
     *
     * Response function is IUimLpaResponse.uimLpaUserResponse()
     */
    oneway void uimLpaUserRequest(in int token, in UimLpaUserReq userReq);

    /**
     * Set callback for uim lpa requests and indications
     *
     * @param responseCallback Object containing response callback functions
     * @param indicationCallback Object containing uim remote server indication callback functions
     */
    void setCallback(in IUimLpaResponse responseCallback,
        in IUimLpaIndication indicationCallback);
}
