/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.ims.imscmaidlservice;

import vendor.qti.ims.imscmaidlservice.ConnectionEventData;
import vendor.qti.ims.imscmaidlservice.IncomingMessage;

// Interface inherits from vendor.qti.ims.imscmservice@2.0::IImsCMConnectionListener but AIDL does not support interface inheritance (methods have been flattened).
@VintfStability
interface IImsCMConnectionListener {
    /**
     * Callback used to indicate the incoming message to the client.
     *
     * @param[in] message   Incoming message params.
     *
     * @return
     *   None.
     *
     * @dependencies
     *   None.
     */
    oneway void handleIncomingMessage(in IncomingMessage data);

    /**
     * Callback used to indicate the status of the command that
     * has been triggered.
     *
     * @param[in] status    Status as defined in StatusCode enum
     * @param[in] userdata  User data corresponding to the command.
     *
     * @return
     *   None.
     *
     * @dependencies
     *   None.
     */
    oneway void onCommandStatus(in int status, in int userdata);

    /**
     * Callback function to inform clients about a registration status
     * change, changes in service allowed by the policy manager due to
     * a RAT change, and any forceful terminations of the connection
     * object by the QTI framework due to PDP status changes.
     *
     * @param[in] event Connection event datastructure.
     *
     * @return
     *   None.
     *
     * @dependencies
     *   None.
     */
    oneway void onEventReceived(in ConnectionEventData event);
}
