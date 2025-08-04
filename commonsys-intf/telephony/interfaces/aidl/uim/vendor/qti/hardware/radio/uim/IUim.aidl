/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.uim;

import vendor.qti.hardware.radio.uim.IUimIndication;
import vendor.qti.hardware.radio.uim.IUimResponse;
import vendor.qti.hardware.radio.uim.UimRemoteSimlockOperationType;
import vendor.qti.hardware.radio.uim.UimRemoteSimlockOperationType;
import vendor.qti.hardware.radio.uim.IUimResponse;
import vendor.qti.hardware.radio.uim.UimApplicationType;

@VintfStability
interface IUim {
    /**
     * UIM_REMOTE_SIMLOCK_REQUEST
     *
     * @param token Id to match req-resp. Resp must include same token.
     * @param simLockOp simlock request type
     * @param simlockData simlock data
     */
    oneway void uimRemoteSimlockRequest(in int token, in UimRemoteSimlockOperationType simlockOp,
        in byte[] simlockData);

    /**
     * Set callback for uim lpa requests and indications
     *
     * @param responseCallback Object containing response callback functions
     * @param indicationCallback Object containing uim remote server indication callback functions
     */
    void setCallback(in IUimResponse responseCallback, in IUimIndication indicationCallback);

    /**
     * setGbaCallback to set the callbacks for handling
     * responses related to GBA requests
     */
    void setGbaCallback(in IUimResponse responseCallback);

    /**
     * UIM_GBA_INIT to perform bootstrapping procedures(GBA)
     * and generate generate keys
     *
     * @param token
     *   It is to match req-resp.
     *   Response must include same token.
     *
     * @param securityProtocol
     *   Security protocol identifier NULL terminated string of 5 octets.
     *   See 3GPP TS 33.220 Annex H.
     *
     * @param nafFullyQualifiedDomainName
     *   NAF fully qualified domain name with maximum size of 255 bytes.
     *
     * @param appType
     *   Card Application type
     *
     * @forceBootStrapping
     *   true=force bootstrapping, false=do not force bootstrapping
     *
     * @apn
     *   Access Point name required for BootStrapping procedure.
     *   This is optional i.e. If empty string is passed, then
     *   APN is selected based on NV configuration or other policies
     *   in the modem.
     */
    oneway void uimGbaInit(in int token, in byte[] securityProtocol,
        in String nafFullyQualifiedDomainName, in UimApplicationType appType,
        in boolean forceBootStrapping, in String apn);

    /**
     * UIM_GET_IMPI to obtain IMPI value
     *
     * @param token
     *   It is used to match a request-resp
     *   Response will include the same token
     *
     * @param appType
     *   Card Application type
     *
     * @param secure
     *   If true, encrypted IMPI value is returned
     *   If false, plain text IMPI value is returned
     */
    oneway void uimGetImpi(in int token, in UimApplicationType appType, in boolean secure);
}
