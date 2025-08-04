/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 */

package vendor.qti.hardware.radio.lpa;

import vendor.qti.hardware.radio.lpa.UimLpaAddProfileProgressInd;
import vendor.qti.hardware.radio.lpa.UimLpaHttpTransactionInd;
import vendor.qti.hardware.radio.lpa.UimLpaUserConsentType;

@VintfStability
interface IUimLpaIndication {

    /**
     * UIM_LPA_ADD_PROFILE_PROGRESS_IND
     *
     * @param progressInd profile download progress indication.
     */
    oneway void uimLpaAddProfileProgressIndication(
        in UimLpaAddProfileProgressInd progressInd);

    /**
     * UIM_LPA_END_USER_CONSENT_IND
     *
     * @param UimLpaUserConsentType
     */
    oneway void uimLpaEndUserConsentIndication(in UimLpaUserConsentType userConsent);

    /**
     * UIM_LPA_HTTP_TXN_IND
     *
     * @param txnInd profile download http transaction indication
     */
    oneway void uimLpaHttpTxnIndication(in UimLpaHttpTransactionInd txnInd);
}
