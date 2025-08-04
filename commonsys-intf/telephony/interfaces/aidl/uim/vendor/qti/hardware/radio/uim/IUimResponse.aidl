/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.uim;

import vendor.qti.hardware.radio.uim.UimRemoteSimlockOperationType;
import vendor.qti.hardware.radio.uim.UimRemoteSimlockResponseType;
import vendor.qti.hardware.radio.uim.UimRemoteSimlockStatus;
import vendor.qti.hardware.radio.uim.UimRemoteSimlockVersion;
import vendor.qti.hardware.radio.uim.UimRemoteSimlockOperationType;
import vendor.qti.hardware.radio.uim.UimGbaStatus;

@VintfStability
interface IUimResponse {
    /**
     * UIM_REMOTE_SIMLOCK_RESPONSE
     *
     * @param token Id to match req-resp. Value must match the one in req.
     * @param response Remote simlock response
     * @param simlockOp remote simlock requested operation
     * @param simlockRspData simlock response data valid for requested operation
     *        Encrypted Key for UIM_REMOTE_SIMLOCK_GENERATE_ENCRYPTED_KEY
     *        Simlock response BLOB for UIM_REMOTE_SIMLOCK_PROCESS_SIMLOCK_DATA
     *        HMAC data for UIM_REMOTE_SIMLOCK_GENERATE_HMAC
     * @param version Simlock blob highest version supported valid for requested
     * operation UIM_REMOTE_SIMLOCK_GET_MAX_SUPPORTED_VERSION
     * @param status Status of the simlock valid for requested operation
     * UIM_REMOTE_SIMLOCK_GET_STATUS
     */
    oneway void uimRemoteSimlockResponse(in int token, in UimRemoteSimlockResponseType response,
        in UimRemoteSimlockOperationType simlockOp, in byte[] simlockRspData,
        in UimRemoteSimlockVersion version, in UimRemoteSimlockStatus status);

    /**
     * UIM_GET_IMPI_RESPONSE
     *
     * @param token Id
     *   It matches req-resp. Value must match the one in req.
     *
     * @param response Remote simlock response
     *
     * @param gbaStatus
     *   It tells the status of the request
     *   Futher information of the response makes sense only when
     *   status is success
     *
     * @param isImpiEncrypted
     *   Tells whether IMPI value is encrypted
     *
     * @param impi
     *   IMPI value
     */
    oneway void uimGbaGetImpiResponse(in int token, in UimGbaStatus gbaStatus,
        in boolean isImpiEncrypted, in byte[] impi);

    /**
     * UIM_GBA_INIT_RESPONSE
     *
     * @param token
     *   It matches req-resp. Value must match the one in req.
     *
     * @param gbaStatus
     *   It tells the status of the request
     *   Futher information of the response makes sense only when
     *   status is success
     *
     * @param isKsNafEncrypted
     *   Tells whether KsNaf is in encrypted state
     *
     * @param ksNaf
     *   KsNaf value
     *
     * @param b_Tid
     *   B_TID value
     *
     * @param key_lifetime
     *   key_lifetime value
     */
    oneway void uimGbaInitResponse(in int token, in UimGbaStatus gbaStatus,
        in boolean isKsNafEncrypted, in byte[] ksNaf, in String b_Tid, in String key_lifetime);

    /**
     * UIM_REMOTE_SIMLOCK_TIMER_RESPONSE
     *
     * @param token Id to match req-resp. Value must match the one in req.
     * @param response Remote simlock response
     * @param timerValue timer value started by simlock engine
     */
    oneway void uimRemoteSimlockTimerResponse(in int token,
        in UimRemoteSimlockResponseType response, in int timerValue);
}
