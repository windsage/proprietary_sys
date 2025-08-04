/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.uim_remote_client;

import vendor.qti.hardware.radio.uim_remote_client.UimRemoteClientApduRsp;
import vendor.qti.hardware.radio.uim_remote_client.UimRemoteClientEventRsp;

@VintfStability
interface IUimRemoteServiceClientResponse {
    /**
     * Uim remote client APDU response
     *
     * @param token Id to match req-resp. Resp must include same token.
     * @param apduResp remote UIM APDU response
     */
    oneway void uimRemoteServiceClientApduResp(in int token, in UimRemoteClientApduRsp apduResp);

    /**
     * Uim remote client event response
     *
     * @param token Id to match req-resp. Resp must include same token.
     * @param eventResp remote UIM event response
     */
    oneway void uimRemoteServiceClientEventResp(in int token,
        in UimRemoteClientEventRsp eventResp);
}
