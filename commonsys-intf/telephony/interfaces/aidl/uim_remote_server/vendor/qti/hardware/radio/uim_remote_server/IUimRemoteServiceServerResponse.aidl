/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.uim_remote_server;

import vendor.qti.hardware.radio.uim_remote_server.UimRemoteServiceServerConnectRsp;
import vendor.qti.hardware.radio.uim_remote_server.UimRemoteServiceServerResultCode;

@VintfStability
interface IUimRemoteServiceServerResponse {
    /**
     * TRANSFER_APDU_RESP
     *
     * @param token Id to match req-resp. Value must match the one in req.
     * @param resultCode ResultCode to indicate if command was processed correctly
     *        Possible values:
     *        UimRemoteServiceServerResultCode:UIM_REMOTE_SERVICE_SERVER_SUCCESS,
     *        UimRemoteServiceServerResultCode:UIM_REMOTE_SERVICE_SERVER_GENERIC_FAILURE,
     *        UimRemoteServiceServerResultCode:UIM_REMOTE_SERVICE_SERVER_CARD_NOT_ACCESSSIBLE,
     *        UimRemoteServiceServerResultCode:UIM_REMOTE_SERVICE_SERVER_CARD_ALREADY_POWERED_OFF,
     *        UimRemoteServiceServerResultCode:UIM_REMOTE_SERVICE_SERVER_CARD_REMOVED
     * @param apduRsp APDU Response. Valid only if command was processed correctly and no error
     *        occurred.
     */
    oneway void uimRemoteServiceServerApduResponse(in int token,
        in UimRemoteServiceServerResultCode resultCode, in byte[] apduRsp);

    /**
     * CONNECT_RESP
     *
     * @param token Id to match req-resp. Value must match the one in req.
     * @param sapConnectRsp Connection Status
     * @param maxMsgSize MaxMsgSize supported by server if request cannot be fulfilled.
     *        Valid only if connectResponse is UimRemoteServiceServerConnectRsp:MSG_SIZE_TOO_LARGE.
     */
    oneway void uimRemoteServiceServerConnectResponse(in int token,
        in UimRemoteServiceServerConnectRsp sapConnectRsp, in int maxMsgSize);

    /**
     * DISCONNECT_RESP
     *
     * @param token Id to match req-resp. Value must match the one in req.
     */
    oneway void uimRemoteServiceServerDisconnectResponse(in int token);

    /**
     * ERROR_RESP
     *
     * @param token Id to match req-resp. Value must match the one in req.
     */
    oneway void uimRemoteServiceServerErrorResponse(in int token);

    /**
     * POWER_SIM_OFF_RESP and POWER_SIM_ON_RESP
     *
     * @param token Id to match req-resp. Value must match the one in req.
     * @param resultCode ResultCode to indicate if command was processed correctly
     *        Possible values:
     *        UimRemoteServiceServerResultCode:UIM_REMOTE_SERVICE_SERVER_SUCCESS,
     *        UimRemoteServiceServerResultCode:UIM_REMOTE_SERVICE_SERVER_GENERIC_FAILURE,
     *        UimRemoteServiceServerResultCode:UIM_REMOTE_SERVICE_SERVER_CARD_NOT_ACCESSSIBLE,
     *        (possible only for power on req)
     *        UimRemoteServiceServerResultCode:UIM_REMOTE_SERVICE_SERVER_CARD_ALREADY_POWERED_OFF,
     *        (possible only for power off req)
     *        UimRemoteServiceServerResultCode:UIM_REMOTE_SERVICE_SERVER_CARD_REMOVED,
     *        UimRemoteServiceServerResultCode:UIM_REMOTE_SERVICE_SERVER_CARD_ALREADY_POWERED_ON
     *        (possible only for power on req)
     */
    oneway void uimRemoteServiceServerPowerResponse(in int token,
        in UimRemoteServiceServerResultCode resultCode);

    /**
     * RESET_SIM_RESP
     *
     * @param token Id to match req-resp. Value must match the one in req.
     * @param resultCode ResultCode to indicate if command was processed correctly
     *        Possible values:
     *        UimRemoteServiceServerResultCode:UIM_REMOTE_SERVICE_SERVER_SUCCESS,
     *        UimRemoteServiceServerResultCode:UIM_REMOTE_SERVICE_SERVER_GENERIC_FAILURE,
     *        UimRemoteServiceServerResultCode:UIM_REMOTE_SERVICE_SERVER_CARD_NOT_ACCESSSIBLE,
     *        UimRemoteServiceServerResultCode:UIM_REMOTE_SERVICE_SERVER_CARD_ALREADY_POWERED_OFF,
     *        UimRemoteServiceServerResultCode:UIM_REMOTE_SERVICE_SERVER_CARD_REMOVED
     */
    oneway void uimRemoteServiceServerResetSimResponse(in int token,
        in UimRemoteServiceServerResultCode resultCode);

    /**
     * TRANSFER_ATR_RESP
     *
     * @param token Id to match req-resp. Value must match the one in req.
     * @param resultCode ResultCode to indicate if command was processed correctly
     *        Possible values:
     *        UimRemoteServiceServerResultCode:UIM_REMOTE_SERVICE_SERVER_SUCCESS,
     *        UimRemoteServiceServerResultCode:UIM_REMOTE_SERVICE_SERVER_GENERIC_FAILURE,
     *        UimRemoteServiceServerResultCode:UIM_REMOTE_SERVICE_SERVER_CARD_ALREADY_POWERED_OFF,
     *        UimRemoteServiceServerResultCode:UIM_REMOTE_SERVICE_SERVER_CARD_REMOVED,
     *        UimRemoteServiceServerResultCode:UIM_REMOTE_SERVICE_SERVER_DATA_NOT_AVAILABLE
     * @param atr Answer to Reset from the subscription module. Included only if no error occurred
     *        otherwise empty.
     */
    oneway void uimRemoteServiceServerTransferAtrResponse(in int token,
        in UimRemoteServiceServerResultCode resultCode, in byte[] atr);

    /**
     * TRANSFER_CARD_READER_STATUS_REQ
     *
     * @param token Id to match req-resp. Value must match the one in req.
     * @param resultCode ResultCode to indicate if command was processed correctly
     *        Possible values:
     *        UimRemoteServiceServerResultCode:UIM_REMOTE_SERVICE_SERVER_SUCCESS,
     *        UimRemoteServiceServerResultCode:UIM_REMOTE_SERVICE_SERVER_GENERIC_FAILURE
     *        UimRemoteServiceServerResultCode:UIM_REMOTE_SERVICE_SERVER_DATA_NOT_AVAILABLE
     * @param cardReaderStatus Card Reader Status coded as described in 3GPP TS 11.14 Section
     *        12.33 and TS 31.111 Section 8.33
     */
    oneway void uimRemoteServiceServerTransferCardReaderStatusResponse(in int token,
        in UimRemoteServiceServerResultCode resultCode, in int cardReaderStatus);

    /**
     * SET_TRANSPORT_PROTOCOL_RESP
     *
     * @param token Id to match req-resp. Value must match the one in req.
     * @param resultCode ResultCode to indicate if command was processed correctly
     *        Possible values:
     *        UimRemoteServiceServerResultCode:UIM_REMOTE_SERVICE_SERVER_SUCCESS
     *        UimRemoteServiceServerResultCode:UIM_REMOTE_SERVICE_SERVER_NOT_SUPPORTED
     */
    oneway void uimRemoteServiceServerTransferProtocolResponse(in int token,
        in UimRemoteServiceServerResultCode resultCode);
}
