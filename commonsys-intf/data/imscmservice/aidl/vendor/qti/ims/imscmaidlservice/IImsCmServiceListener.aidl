/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.ims.imscmaidlservice;

import vendor.qti.ims.imscmaidlservice.ConfigData;
import vendor.qti.ims.imscmaidlservice.AutoconfigRequestStatus;
import vendor.qti.ims.imscmaidlservice.AutoConfigResponse;

@VintfStability
interface IImsCmServiceListener {

    /**
     * Callback function to notify the clients about the current status of
     * autoconfig request placed.
     *
     * @param[in] status of the autoconfig request, as defined in autoconfigRequestStatus enum
     * @param[in] Userdata/request ID passed while placing a request|
     *
     * @return
     *   None.
     *
     * @dependencies
     *   None.
     */
    oneway void onAcsConnectionStatusChange(in AutoconfigRequestStatus autoConfigReqStatus,
        in int userdata);

    /**
     * Unsolicited indication to client when service receives a response
     * for Auto Config request to server that requires additional
     * handling from the client.
     *
     * @param[in] sip response of the ACS request
     *
     * @return
     *   None.
     *
     * @dependencies
     *   None.
     */
    oneway void onAutoConfigResponse(in AutoConfigResponse acsResponse);

    /**
     * Callback function to notify the clients the status of request
     * placed.
     *
     * @param[in] Userdata/request ID passed while placing a request
     * @param[in] status of the request, as defined in StatuCode enum.
     *
     * @return
     *   None.
     *
     * @dependencies
     *   None.
     */
    oneway void onCommandStatus(in int userdata, in int status);

    // Changing method name from onConfigurationChange_2_2 to onConfigurationChange
    /**
     * Callback function to notify the clients of a configuration
     * change event.
     *
     * @param[in] config data structure (User or device config)
     * @param[in] userData    user data/request ID per client
     *
     * @return
     *   None.
     *
     * @dependencies
     *   None.
     */
    oneway void onConfigurationChange(in ConfigData config, in int userdata);

    /**
     * Callback function to notify the client upon successful service
     * creation.
     *
     * @param[in] connectionManager     Service Handle
     * @param[in] userdata              request ID passed in InitializeService()
     * @param[in] eStatus               STATUS_SUCCESS if Subsciption supported,
     *                                  else STATUS_SERVICE_NOT_SUPPORTED
     *
     * @return
     *   None.
     *
     * @dependencies
     *   None.
     */
    oneway void onServiceReady(in long connectionManager, in int userdata, in int eStatus);

    /**
     * Callback function to return the status of the connection manager.
     *
     * @param[in] eStatus Status as defined in StatusCode enum.
     *
     * @return
     *   None.
     *
     * @dependencies
     *   None.
     */
    oneway void onStatusChange(in int eStatus);
}
