/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.ims.uceaidlservice;

import vendor.qti.ims.uceaidlservice.CapabilityInfo;
import vendor.qti.ims.uceaidlservice.IOptionsListener;
import vendor.qti.ims.uceaidlservice.OptionsCapabilityInfo;
import vendor.qti.ims.uceaidlservice.UceStatus;
import vendor.qti.ims.uceaidlservice.CapabilityInfo;
import vendor.qti.ims.uceaidlservice.IOptionsListener;
import vendor.qti.ims.uceaidlservice.OptionsCapabilityInfo;
import vendor.qti.ims.uceaidlservice.CapabilityInfo;
import vendor.qti.ims.uceaidlservice.IOptionsListener;
import vendor.qti.ims.uceaidlservice.OptionsCapabilityInfo;
import vendor.qti.ims.uceaidlservice.CapabilityInfo;
import vendor.qti.ims.uceaidlservice.IOptionsListener;
import vendor.qti.ims.uceaidlservice.OptionsCapabilityInfo;

@VintfStability
interface IOptionsService {

    /**
     * Adds a listener to the Options service.
     *
     * @param serviceHandle          received in IOptionsListener.OnOptionsCreated() callback.
     * @param listener               IOptionsListener object.
     * @param clientHandle           token from the client.
     * @return                status of the request placed.
     *
     */
    UceStatus addListener(in long serviceHandle, in IOptionsListener listener,
        in long clientHandle);

    /**
     * Gets the current capabilities information stored in service.
     * The Capability information is received in IOptionsListener.cmdStatus() callback
     * @param serviceHandle     received in IOptionsListener.OnOptionsCreated() callback.
     *
     * @param userData          client token for the request
     * @return                  status of the request placed.
     */
    UceStatus getCapabilityInfo(in long serviceHandle, in long userData);

    /**
     * Requests the capabilities information of a remote URI.
     * the remote party capability is received in IOptionsListener.sipResponseReceived() callback.
     *
     * @param serviceHandle     received in IOptionsListener.OnOptionsCreated() callback.
     * @param remoteUri         URI of the remote contact.
     *                          This has to to be a SIP URI or TEL uri of the remote contact
     * @param userData          client token for the request
     * @return                  status of the request placed.
     */
    UceStatus getContactCapability(in long serviceHandle, in String remoteUri, in long userData);

    /**
     * Requests the capabilities information of specified contacts.
     * For each remote party capability is received in IOptionsListener.sipResponseReceived() callback.
     * @param serviceHandle     received in IOptionsListener.OnOptionsCreated() callback.
     * @param remoteUriList     list of remote contact URI's.
     *                          the URI should be in format of  SIP URI or TEL uri of the remote contact.
     * @param userData          client token for the request.
     * @return                  status of the request placed.
     */
    UceStatus getContactListCapability(in long serviceHandle, in String[] remoteUriList,
        in long userData);

    /**
     * Removes a listener from the Options service.
     * @param serviceHandle    received in IOptionsListener.OnOptionsCreated() callback.
     * @param clientHandle     token from the client used in addListener() API.
     * @return                 status of the request placed.
     */
    UceStatus removeListener(in long serviceHandle, in long clientHandle);

    /**
     * Generates a Network response with the capabilites to share with remote contact
     * Incoming Options request is received in IOptionsListener.incomingOptions() callback.
     * The client needs to share device capabilites currently supported using this network respose request.
     *
     *
     * @param serviceHandle         received in IOptionsListener.OnOptionsCreated() callback.
     * @param tId                   transaction ID received in IOptionsListener.incomingOptions() callback.
     * @param sipResonseCode        SIP response code the UE needs to share to network.
     *                              The response code compliant to RFC-3261
     * @param reasonPhrase          response phrase corresponding to the response code.
     *                              The response phrase compliant to RFC-3261
     * @param reasonHeader          response header corresponding to the response code.
     *                              The response header compliant to RFC-3261
     * @param capInfo               OptionsCapabilityInfo to share in the resonse to network.
     * @param isContactinBlackList  1 if the contact is blacklisted, else 0.
     * @return                      status of the request placed.
     */
    UceStatus responseIncomingOptions(in long serviceHandle, in int tId, in char sipResonseCode,
        in String reasonPhrase, in String reasonHeader, in OptionsCapabilityInfo capInfo,
        in byte isContactinBlackList);

    /**
     * Sets the capabilities information of the self device.
     * The final status of the call is received in IOptionsListener.cmdStatus() callback.
     * 
     *
     * @param serviceHandle     received in IOptionsListener.OnOptionsCreated() callback.
     * @param capinfo           CapabilityInfo to store.
     * @param userData          client token for the request
     * @return                  status of the request placed.
     */
    UceStatus setCapabilityInfo(in long serviceHandle, in CapabilityInfo capinfo,
        in long userData);
}
