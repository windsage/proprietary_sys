/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.ims.imscmaidlservice;

/*
 * Message Type
 */
@VintfStability
@Backing(type="int")
enum MessageType {
    TYPE_REQUEST,
    /*
     * SIP REQUEST MESSAGE IDENTIFIER
     */
    TYPE_RESPONSE,
    /*
     * SIP RESPONSE MESSAGE IDENTIFIER
     */
    TYPE_INVALID_MAX,
}
