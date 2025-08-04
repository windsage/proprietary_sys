/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.ims.imscmaidlservice;

@VintfStability
@Backing(type="int")
enum MethodResponseDataKeys {
    method = 3001,
    /*
     * SIP Method for which we got error response or timer B/F is fired
     * accepts a string type value
     */
    responseCode = 3002,
}
