/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.data.dmapconsent;

import vendor.qti.data.dmapconsent.ConsentStatus;

@VintfStability
interface IStatusCb {
    /**
     * Gives a status CB for the API call indentified by the given transactionId.
     *
     * @param transactionId      ID of the API call.
     * @param status             Status of the callback.
     *
     * @return                   None.
     */
    oneway void consentStatusCb(in long transactionId, in ConsentStatus status);
}
