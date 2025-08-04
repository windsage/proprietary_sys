/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.uim_remote_server;

import vendor.qti.hardware.radio.uim_remote_server.UimRemoteServiceServerDisconnectType;
import vendor.qti.hardware.radio.uim_remote_server.UimRemoteServiceServerStatus;

@VintfStability
interface IUimRemoteServiceServerIndication {
    /**
     * DISCONNECT_IND
     *
     * @param disconnectType Disconnect Type to indicate if shutdown is graceful or immediate
     */
    oneway void uimRemoteServiceServerDisconnectIndication(
        in UimRemoteServiceServerDisconnectType disconnectType);

    /**
     * STATUS_IND
     *
     * @param status Parameter to indicate reason for the status change.
     */
    oneway void uimRemoteServiceServerStatusIndication(in UimRemoteServiceServerStatus status);
}
