/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.ims.imscmaidlservice;

/*
 * Supported Protocol Type
 */
@VintfStability
@Backing(type="int")
enum SipProtocolType {
    UDP,
    /*
     * SIP message sent over UDP/via header has UDP.
     */
    TCP,
    /*
     * SIP message sent over TCP/via header has TCP.
     */
    INVALID_MAX,
}
