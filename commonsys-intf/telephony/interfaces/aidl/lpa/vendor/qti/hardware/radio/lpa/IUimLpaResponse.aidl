/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 */

package vendor.qti.hardware.radio.lpa;

import vendor.qti.hardware.radio.lpa.UimLpaResult;
import vendor.qti.hardware.radio.lpa.UimLpaUserResp;

@VintfStability
interface IUimLpaResponse {

    /**
     * Response to IUimLpa.uimLpaHttpTxnCompletedRequest
     *
     * UIM_LPA_HTTP_TXN_COMPLETED_REPONSE
     *
     * @param token Id to match req-resp. Value must match the one in req.
     * @param result http transaction completion request result
     *
     */
    oneway void uimLpaHttpTxnCompletedResponse(in int token, in UimLpaResult result);

    /**
     * Response to IUimLpa.uimLpaHttpTxnCompletedRequest
     *
     * UIM_LPA_USER_RESPONSE
     *
     * @param token Id to match req-resp. Value must match the one in req.
     * @param response LPA user request response
     *
     */
    oneway void uimLpaUserResponse(in int token, in UimLpaUserResp response);
}
