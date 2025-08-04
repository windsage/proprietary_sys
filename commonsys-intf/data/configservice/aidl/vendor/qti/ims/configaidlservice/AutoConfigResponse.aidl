/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.ims.configaidlservice;

@VintfStability
parcelable AutoConfigResponse {
    /**
     * SIP/HTTP response code
     */
    char statusCode;
    /**
     * SIP/HTTP response reason phrase
     */
    String reasonPhrase;
}
