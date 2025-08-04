/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 */

package vendor.qti.hardware.radio.lpa;

import vendor.qti.hardware.radio.lpa.UimLpaSrvAddrOpReq;
import vendor.qti.hardware.radio.lpa.UimLpaUserEventId;
import vendor.qti.hardware.radio.lpa.UimLpaUserConsentType;

@VintfStability
@JavaDerive(toString=true)
parcelable UimLpaUserReq {

    /**
     *
     * UimLpaUser EventId
     */
    UimLpaUserEventId event;

    /**
     *
     * UIM_LPA_ADD_PROFILE
     */
    String activationCode;

    /**
     *
     * UIM_LPA_ADD_PROFILE
     */
    String confirmationCode;

    /*
     *
     * UIM_LPA_UPDATE_NICKNAME
     */
    String nickname;

    /*
     *
     * UIM_LPA_ENABLE_PROFILE
     * UIM_LPA_DISABLE_PROFILE
     * UIM_LPA_DELETE_PROFILE
     * UIM_LPA_UPDATE_NICKNAME
     */
    byte[] iccid;

    /*
     *
     * UIM_LPA_EUICC_MEMORY_RESET
     */
    int resetMask;

    /*
     * UIM_LPA_USER_CONSENT
     */
    boolean userOk;

    /*
     * UIM_LPA_SRV_ADDR_OPERATION
     *
     */
    UimLpaSrvAddrOpReq srvOpReq;

    /*
     *
     * UIM_USER_NOK_REASON
     */
    int nok_reason;

    /*
     *
     * UimLpa Consent Type
     */
    UimLpaUserConsentType user_consent_type;
}
