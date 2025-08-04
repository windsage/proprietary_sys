/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.ims.uceaidlservice;

import vendor.qti.ims.uceaidlservice.OptionsCmdId;

@VintfStability
parcelable OptionsSipResponse {
    /*
     * Command ID
     */
    OptionsCmdId cmdId;
    /*
     * request ID to identify API Request
     */
    int requestId;
    /**
     * network generated error code
     *  this is compliant to  RFC 3261
     */
    char sipResponseCode;
    /**
     * time to retry in secs
     *  this is also network generated
     */
    char retryAfter;
    /**
     * network generated phrase in combination to error code
     *  this is compliant to  RFC 3261
     */
    String reasonPhrase;
    /**
     * network generated header in combination to error code
     *  this is compliant to  RFC 3261
     */
    String reasonHeader;
}
