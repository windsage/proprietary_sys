/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.ims.uceaidlservice;

import vendor.qti.ims.uceaidlservice.OptionsCapabilityInfo;
import vendor.qti.ims.uceaidlservice.OptionsCmdStatus;
import vendor.qti.ims.uceaidlservice.OptionsSipResponse;
import vendor.qti.ims.uceaidlservice.UceStatusCode;
import vendor.qti.ims.uceaidlservice.OptionsCapabilityInfo;
import vendor.qti.ims.uceaidlservice.OptionsCmdStatus;
import vendor.qti.ims.uceaidlservice.OptionsCapabilityInfo;
import vendor.qti.ims.uceaidlservice.OptionsCmdStatus;
import vendor.qti.ims.uceaidlservice.OptionsCapabilityInfo;
import vendor.qti.ims.uceaidlservice.OptionsCmdStatus;
import vendor.qti.ims.uceaidlservice.OptionsSipResponse;

@VintfStability
interface IOptionsListener {
    /**
     * Callback function to be invoked to inform the client of the status of an asynchronous call.
     *
     * @param commandStatus     OptionsCmdStatus status of the request placed.
     *
     */
    oneway void cmdStatus(in OptionsCmdStatus commandStatus);

    /**
     * Callback function invoked to inform the client of an incoming Options request
     * from the network.
     *
     * @param uri        URI of the remote entity received.
     *                   This will be either a SIP URI or Tel URI as supported on network.
     * @param capInfo    OptionsCapabilityInfo of the remote entity.
     * @param tID        transation ID of the request received from network.
     */
    oneway void incomingOptions(in String uri, in OptionsCapabilityInfo capInfo, in char tId);

    /**
     * Callback invoked for IUceService.createOptionsService() API Call
     * @param serviceHandle     a token to identify the client.
     *                          -1 indicates invalid handle
     *
     */
    oneway void onOptionsCreated(in long serviceHandle);

    /**
     * Callback function to be invoked by the Options service to notify the listener of service
     * availability. This indicates the service is ready to use.
     * @param status       as service availability.
     *
     */
    oneway void serviceAvailable(in UceStatusCode status);

    /**
     * Callback function to be invoked by the Options service to notify the listener of service
     * unavailability. Indicates the service is not ready to
     *
     * @param status       as service unavailability.
     *
     */
    oneway void serviceUnAvailable(in UceStatusCode status);

    /**
     * Callback function invoked to inform the client when the response for a Sip Options request
     * has been received.
     *
     * @param uri             URI of the remote entity received in network response.
     *                        This will be either a SIP URI or Tel URI as supported on network.
     * @param sipResponse     OptionsSipResponse of the network response received.
     * @param capInfo         OptionsCapabilityInfo of the remote entity received.
     *
     */
    oneway void sipResponseReceived(in String uri, in OptionsSipResponse sipResponse,
        in OptionsCapabilityInfo capInfo);
}
