/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.ims.uceaidlservice;

import vendor.qti.ims.uceaidlservice.PresCmdStatus;
import vendor.qti.ims.uceaidlservice.PresPublishTriggerType;
import vendor.qti.ims.uceaidlservice.PresResInfo;
import vendor.qti.ims.uceaidlservice.PresRlmiInfo;
import vendor.qti.ims.uceaidlservice.PresSipResponse;
import vendor.qti.ims.uceaidlservice.PresTupleInfo;
import vendor.qti.ims.uceaidlservice.UceStatusCode;
import vendor.qti.ims.uceaidlservice.PresPublishTriggerType;
import vendor.qti.ims.uceaidlservice.PresResInfo;
import vendor.qti.ims.uceaidlservice.PresSipResponse;
import vendor.qti.ims.uceaidlservice.PresTupleInfo;

@VintfStability
interface IPresenceListener {
    /**
     * Callback function to be invoked to inform the client of the status of an asynchronous call.
     * @param commandStatus     command status of the request placed.
     */
    oneway void cmdStatus(in PresCmdStatus commandStatus);

    /**
     * Callback function to be invoked to inform the client when the NOTIFY message carrying a
     * single contact's capabilities information is received.
     * @param presentityUri     URI of the remote entity the request was placed.
     *                          This will be either a SIP URI or Tel URI as supported on network.
     * @param tupleinfoArr      array of PresTubleInfo which carries
     *                          capability information the remote entity supports.
     *
     */
    oneway void onCapInfoReceived(in String presentityUri, in PresTupleInfo[] tupleinfoArr);

    /**
     * Callback function to be invoked to inform the client when the NOTIFY message carrying
     * contact's capabilities information is received.
     * @param rlmiInfo      resource infomation received from network.
     * @param resInfoArr    array of PresResInfo which carries
     *                      capabilities received from network for the list of
     *                      remore URI.
     */
    oneway void onListCapInfoReceived(in PresRlmiInfo rlmiInfo, in PresResInfo[] resInfoArr);

    /**
     * Callback invoked for IUceService.createPresenceService() API Call
     * @param serviceHandle     a token to identify the client.
     *
     */
    oneway void onPresenceCreated(in long serviceHandle);

    /**
     * Callback function to be invoked by the Presence service to notify the client to send a
     * publish request.
     *
     * @param triggerType      PresPublishTriggerType for the network being supported.
     *
     */
    oneway void onPublishTrigger(in PresPublishTriggerType triggerType);

    /**
     * Callback function to be invoked to inform the client when the response for a SIP message,
     * such as PUBLISH or SUBSCRIBE, has been received.
     * @param sipResponse   PresSipResponse object which carries network
     *                      response received for the request placed.
     *
     */
    oneway void onSipResponseReceived(in PresSipResponse sipResponse);

    /**
     * Callback function to be invoked to inform the client when Unpublish message
     * is sent to network.
     */
    oneway void onUnpublishSent();

    /**
     * Callback function to be invoked by the Presence service to notify the listener of service
     * availability.
     * @param status     as service availability.
     *
     */
    oneway void serviceAvailable(in UceStatusCode status);

    /**
     * Callback function to be invoked by the Presence service to notify the listener of service
     * unavailability.
     * @param status       as service unAvailability.
     *
     */
    oneway void serviceUnAvailable(in UceStatusCode status);
}
