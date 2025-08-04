/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.ims.imscmaidlservice;

@VintfStability
@Backing(type="int")
enum IncomingMessageKeys {
    /**
     * Message contents ( complete received SIP message).
     * accepts a buffer type value
     */
    Message = 5001,
    /**
     * Address from where the SIP message is received.
     * accepts a string type value
     */
    recdAddr = 5002,
}
