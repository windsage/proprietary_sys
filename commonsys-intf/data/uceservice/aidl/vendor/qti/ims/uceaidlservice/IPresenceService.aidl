/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.ims.uceaidlservice;

import vendor.qti.ims.uceaidlservice.IPresenceListener;
import vendor.qti.ims.uceaidlservice.PresServiceInfo;
import vendor.qti.ims.uceaidlservice.PresenceCapabilityInfo;
import vendor.qti.ims.uceaidlservice.RcsFeatureTag;
import vendor.qti.ims.uceaidlservice.UceStatus;
import vendor.qti.ims.uceaidlservice.IPresenceListener;
import vendor.qti.ims.uceaidlservice.PresenceCapabilityInfo;
import vendor.qti.ims.uceaidlservice.PresenceCapabilityInfo;
import vendor.qti.ims.uceaidlservice.FeatureDesc;
import vendor.qti.ims.uceaidlservice.IPresenceListener;
import vendor.qti.ims.uceaidlservice.PresenceCapabilityInfo;

@VintfStability
interface IPresenceService {

    /**
     * Adds a listener to the Presence service.
     *
     * @param serviceHandle    received in IPresenceListener.onPresenceCreated() callback.
     * @param listener         IPresenceListener Object.
     * @param clientHandle     token from the client.
     *
     * @return                 status of the request placed.
     */
    UceStatus addListener(in long serviceHandle, in IPresenceListener listener,
        in long clientHandle);

    /**
     * Retrieves the capability information for a single contact. Clients receive the requested
     * information via the listener callback function IPresenceListener.onCapInfoReceived() callback.
     *
     * @param serviceHandle     received in IPresenceListener.onPresenceCreated() callback.
     * @param remoteUri         remote contact URI
     *                          This has to to be a SIP URI or TEL uri of the remote contact
     * @param userData          client token for the request.
     * @return                  status of the request placed.
     */
    UceStatus getContactCapability(in long serviceHandle, in String remoteUri, in long userData);

    /**
     * Retrieves the capability information for a list of contacts. Clients receive the requested
     * information via the listener callback function IPresenceListener.onListCapInfoReceived() callback.
     *
     * @param serviceHandle     received in IPresenceListener.onPresenceCreated() callback.
     * @param contactList       list of remote contact URI's.
     *                          the URI should be in format of  SIP URI or TEL uri of the remote contact.
     * @param userData          client token for the request.
     * @return                  status of the request placed.
     */
    UceStatus getContactListCapability(in long serviceHandle, in String[] contactList,
        in long userData);

    /**
     * Sends a request to publish current device capabilities.
     * The network response is notifed in IPresenceListener.onSipResponseReceived() callback.
     * If status of API is returned as NOT_SUPPORTED then it means capabilities are
     * not supported by the server.
     *
     * @param serviceHandle     received in IPresenceListener.onPresenceCreated() callback.
     * @param myCapInfo         PresenceCapabilityInfo to share to network.
     * @param userData          client token for the request.
     * @return                  status of the request placed.
     */
    UceStatus publishCapability(in long serviceHandle, in PresenceCapabilityInfo capInfo,
        in long userData);

    /**
     * Re-enables the Presence service from disabled state.
     *
     * The Presence Service moves to disabled state in runtime only if the network
     * sends a response for "489 Bad event" SIP response for any Presence event.
     *
     * If the client intends to resume Presence service, The client must call this API
     * before calling any new presence API requests.
     *
     * The final status of this request is notified in IPresenceListener.cmdStatus() callback.
     *
     * @param serviceHandle     received in IPresenceListener.onPresenceCreated() callback.
     * @param userData          client token for the request
     * @return                  status of the request placed.
     */
    UceStatus reEnableService(in long serviceHandle, in long userData);

    /**
     * Removes a listener from the Presence service.
     *
     * @param serviceHandle    received in IPresenceListener.onPresenceCreated() callback.
     * @param clientHandle     token provided in IUceService.createPresenceService() or
     *                         IPresenceService.addListener().
     * @return                 status of the request placed.
     */
    UceStatus removeListener(in long serviceHandle, in long clientHandle);

    /**
     * Sets the mapping between a new/custom feature tag and the corresponding service tuple information
     * to be included in the published document.
     * The status of this call is received in IPresenceListener.cmdStatus() callback.
     * 
     * Please refer to GSMA RCS 5.3 documentation on the format of custom feature tag
     * and service tuple information.
     *
     * @param serviceHandle     received in IPresenceListener.onPresenceCreated() callback.
     * @param featureTag        feature to be supported.
     * @param serviceInfo       service information describing the featureTag.
     * @param userData          client token for the request.
     * @return                  status of the request placed.
     */
    UceStatus setNewFeatureTag(in long serviceHandle, in FeatureDesc featureTag,
        in PresServiceInfo serviceInfo, in long userData);
}
