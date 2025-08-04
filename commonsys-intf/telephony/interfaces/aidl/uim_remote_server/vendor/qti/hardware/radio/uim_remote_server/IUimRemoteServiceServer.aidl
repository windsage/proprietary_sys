/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.uim_remote_server;

import vendor.qti.hardware.radio.uim_remote_server.IUimRemoteServiceServerIndication;
import vendor.qti.hardware.radio.uim_remote_server.IUimRemoteServiceServerResponse;
import vendor.qti.hardware.radio.uim_remote_server.UimRemoteServiceServerApduType;
import vendor.qti.hardware.radio.uim_remote_server.UimRemoteServiceServerTransferProtocol;

@VintfStability
interface IUimRemoteServiceServer {
    /**
     * Set callback for uim remote service server requests and indications
     *
     * @param responseCallback Object containing response callback functions
     * @param indicationCallback Object containing uim remote server indication callback functions
     */
    void setCallback(in IUimRemoteServiceServerResponse responseCallback,
        in IUimRemoteServiceServerIndication indicationCallback);

    /**
     * TRANSFER_APDU_REQ
     *
     * @param token Id to match req-resp. Resp must include same token.
     * @param type APDU command type
     * @param command CommandAPDU/CommandAPDU7816 parameter depending on type
     */
    oneway void uimRemoteServiceServerApduReq(in int token, in UimRemoteServiceServerApduType type,
        in byte[] command);

    /**
     * CONNECT_REQ
     *
     * @param token Id to match req-resp. Resp must include same token.
     * @param maxMsgSize MaxMsgSize to be used for SIM Access Profile connection
     */
    oneway void uimRemoteServiceServerConnectReq(in int token, in int maxMsgSize);

    /**
     * DISCONNECT_REQ
     *
     * @param token Id to match req-resp. Resp must include same token.
     */
    oneway void uimRemoteServiceServerDisconnectReq(in int token);

    /**
     * POWER_SIM_OFF_REQ and POWER_SIM_ON_REQ
     *
     * @param token Id to match req-resp. Resp must include same token.
     * @param state true for on, false for off
     */
    oneway void uimRemoteServiceServerPowerReq(in int token, in boolean state);

    /**
     * RESET_SIM_REQ
     *
     * @param token Id to match req-resp. Resp must include same token.
     */
    oneway void uimRemoteServiceServerResetSimReq(in int token);

    /**
     * SET_TRANSPORT_PROTOCOL_REQ
     *
     * @param token Id to match req-resp. Resp must include same token.
     * @param transferProtocol Transport Protocol
     */
    oneway void uimRemoteServiceServerSetTransferProtocolReq(in int token,
        in UimRemoteServiceServerTransferProtocol transferProtocol);

    /**
     * TRANSFER_ATR_REQ
     *
     * @param token Id to match req-resp. Resp must include same token.
     */
    oneway void uimRemoteServiceServerTransferAtrReq(in int token);

    /**
     * TRANSFER_CARD_READER_STATUS_REQ
     *
     * @param token Id to match req-resp. Resp must include same token.
     */
    oneway void uimRemoteServiceServerTransferCardReaderStatusReq(in int token);
}
