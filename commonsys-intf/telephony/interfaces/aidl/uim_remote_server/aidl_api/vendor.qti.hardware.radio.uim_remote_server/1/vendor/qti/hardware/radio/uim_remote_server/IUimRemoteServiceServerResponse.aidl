/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
///////////////////////////////////////////////////////////////////////////////
// THIS FILE IS IMMUTABLE. DO NOT EDIT IN ANY CASE.                          //
///////////////////////////////////////////////////////////////////////////////

// This file is a snapshot of an AIDL file. Do not edit it manually. There are
// two cases:
// 1). this is a frozen version file - do not edit this in any case.
// 2). this is a 'current' file. If you make a backwards compatible change to
//     the interface (from the latest frozen version), the build system will
//     prompt you to update this file with `m <name>-update-api`.
//
// You must not make a backward incompatible change to any AIDL file built
// with the aidl_interface module type with versions property set. The module
// type is used to build AIDL files in a way that they can be used across
// independently updatable components of the system. If a device is shipped
// with such a backward incompatible change, it has a high risk of breaking
// later when a module using the interface is updated, e.g., Mainline modules.

package vendor.qti.hardware.radio.uim_remote_server;
@VintfStability
interface IUimRemoteServiceServerResponse {
  oneway void uimRemoteServiceServerApduResponse(in int token, in vendor.qti.hardware.radio.uim_remote_server.UimRemoteServiceServerResultCode resultCode, in byte[] apduRsp);
  oneway void uimRemoteServiceServerConnectResponse(in int token, in vendor.qti.hardware.radio.uim_remote_server.UimRemoteServiceServerConnectRsp sapConnectRsp, in int maxMsgSize);
  oneway void uimRemoteServiceServerDisconnectResponse(in int token);
  oneway void uimRemoteServiceServerErrorResponse(in int token);
  oneway void uimRemoteServiceServerPowerResponse(in int token, in vendor.qti.hardware.radio.uim_remote_server.UimRemoteServiceServerResultCode resultCode);
  oneway void uimRemoteServiceServerResetSimResponse(in int token, in vendor.qti.hardware.radio.uim_remote_server.UimRemoteServiceServerResultCode resultCode);
  oneway void uimRemoteServiceServerTransferAtrResponse(in int token, in vendor.qti.hardware.radio.uim_remote_server.UimRemoteServiceServerResultCode resultCode, in byte[] atr);
  oneway void uimRemoteServiceServerTransferCardReaderStatusResponse(in int token, in vendor.qti.hardware.radio.uim_remote_server.UimRemoteServiceServerResultCode resultCode, in int cardReaderStatus);
  oneway void uimRemoteServiceServerTransferProtocolResponse(in int token, in vendor.qti.hardware.radio.uim_remote_server.UimRemoteServiceServerResultCode resultCode);
}
