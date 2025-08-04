/*
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 */

package vendor.qti.hardware.radio.lpa;

import vendor.qti.hardware.radio.lpa.UimLpaHttpCustomHeader;

@VintfStability
@JavaDerive(toString=true)
parcelable UimLpaHttpTransactionInd {

    /**
     *
     * Indication tokenId
     */
    int tokenId;

    /**
     *
     * Indication payload
     */
    byte[] payload;

    /**
     *
     * Indication payload contentType
     */
    String contentType;

    /**
     *
     * Indication payload Headers
     */
    UimLpaHttpCustomHeader[] customHeaders;

    /**
     *
     * Indication url.
     */
    String url;
}
