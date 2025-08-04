/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.uim_remote_client;

import vendor.qti.hardware.radio.uim_remote_client.UimRemoteClientErrorCauseType;
import vendor.qti.hardware.radio.uim_remote_client.UimRemoteClientEventStatusType;
import vendor.qti.hardware.radio.uim_remote_client.UimRemoteClientTransportType;
import vendor.qti.hardware.radio.uim_remote_client.UimRemoteClientUsageType;

@VintfStability
parcelable UimRemoteEventReqType {
    UimRemoteClientEventStatusType event;
    byte[] atr;
    boolean has_wakeupSupport;
    boolean wakeupSupport;
    boolean has_errorCode;
    UimRemoteClientErrorCauseType errorCode;
    boolean has_transport;
    UimRemoteClientTransportType transport;
    boolean has_usage;
    UimRemoteClientUsageType usage;
    boolean has_apdu_timeout;
    int apduTimeout;
    boolean has_disable_all_polling;
    int disableAllPolling;
    boolean has_poll_timer;
    int pollTimer;
}
