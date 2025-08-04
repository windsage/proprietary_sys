/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.ims.imscmaidlservice;

import vendor.qti.ims.imscmaidlservice.IImsCMConnectionListener;
import vendor.qti.ims.imscmaidlservice.StatusCode;
import vendor.qti.ims.imscmaidlservice.OutgoingMessage;
import vendor.qti.ims.imscmaidlservice.ServiceListenerToken;

@VintfStability
interface IImsCMConnection {
    /**
     * Adds a listener containing the list of
     * function pointers to be invoked to notify clients of the
     * various events from the connection.
     *
     * @param[in] pConnectionListener  Structure with the list of
     *                                 callbacks.
     * @param[out] listenerToken       unique id for connListener
     *
     * @return
     *   Status code.                  See #StatusCode.
     *  
     *
     * @dependencies
     *   None.
     */
    StatusCode addListener(in IImsCMConnectionListener connlistener,
        out ServiceListenerToken listenerToken);

    // Adding return type to method instead of out param int status since there is only one return value.
    /**
     * Terminates all transactions being handled by the connection object.
     *
     * @param[in] userdata       Command  user data.
     *
     * @return
     *   Status code.            See #StatusCode.
     *
     * @dependencies
     *   None.
     */
    StatusCode closeAllTransactions(in int userdata);

    // Adding return type to method instead of out param int status since there is only one return value.
    /**
     * Terminates a SIP transaction with a specified call ID.
     *
     * @param[in] pCallId        Call ID.
     * @param[in] userdata       Command  user data.
     *
     * @return
     *   Status code.            See #StatusCode.
     *
     * @dependencies
     *   None.
     */
    StatusCode closeTransaction(in String callID, in int userdata);

    // Adding return type to method instead of out param int status since there is only one return value.
    /**
     * Removes a listener from the IMS connection.
     *
     * @param[in]  listenerToken     unique id provided in addListener()
     *                               or createConnection() API.
     *
     * @return
     *   Status code.                See #StatusCode.
     *
     * @dependencies
     *   None.
     */
    StatusCode removeListener(in long listenerToken);

    // Adding return type to method instead of out param int status since there is only one return value.
    /**
     * Sends a SIP message with a unique call ID.
     *
     * @param[in] data       Structure consisting of outgoing
     *                       message parameters.
     * @param[in] userdata   Command  user data.
     *
     * @return
     *   Status code.        See #StatusCode.
     *
     * @dependencies
     *   None.
     */
    StatusCode sendMessage(in OutgoingMessage data, in int userdata);

    // Adding return type to method instead of out param int statusCode since there is only one return value.
    /**
     * Set the current status of the service(Feature Tag of the service)
     * which is session based and using the current connection.
     *
     * @param[in] pFeatureTag     FeatureTag/Service.
     * @param[in] status          Status of the session.
     *
     * @return
     *   Status code.             See #StatusCode.
     *
     * @dependencies
     *   None.
     */
    StatusCode setStatus(in String featureTag, in int status);
}
