/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.ims.imscmaidlservice;

@VintfStability
@Backing(type="int")
enum OutgoingMessageKeys {
    /**
     * Outbound proxy name.
     * accepts a string type value
     */
    OutboundProxy = 4001,
    /**
     * Remote port where the message has to be sent
     * accepts a string type value
     */
    RemotePort = 4002,
    /**
     * Protocol used in the SIP message.
     * accepts a string type value
     */
    Protocol = 4003,
    /**
     * Message type
     * accepts a string type value
     */
    MessageType = 4004,
    /**
     * Call ID.
     * accepts a string type value
     */
    CallId = 4005,
    /**
     * Message content.
     * accepts a buffer type value
     */
    Message = 4006,
}
