/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.ims.imscmaidlservice;

import vendor.qti.ims.imscmaidlservice.ConfigType;
import vendor.qti.ims.imscmaidlservice.IImsCMConnection;
import vendor.qti.ims.imscmaidlservice.IImsCMConnectionListener;
import vendor.qti.ims.imscmaidlservice.IImsCmServiceListener;
import vendor.qti.ims.imscmaidlservice.MethodResponseData;
import vendor.qti.ims.imscmaidlservice.IImsCmServiceListener;
import vendor.qti.ims.imscmaidlservice.AutoconfigTriggerReason;
import vendor.qti.ims.imscmaidlservice.IImsCmServiceListener;
import vendor.qti.ims.imscmaidlservice.ServiceListenerToken;
import vendor.qti.ims.imscmaidlservice.StatusCode;
import vendor.qti.ims.imscmaidlservice.ConnectionInfo;

@VintfStability
interface IImsCmService {

    /**
     * Initialize Service to work with. This returns a status code.
     * Onsuccessful initialize, OnServiceReady() is triggered with a
     * Service handle.
     *
     * @param[in] iccId       iccId of the subscription the service is requested.
     * @param[in] cmListener  IImsCmServiceListener object for the service
     * @param[in] userData    user data/request ID.
     * @param[out] listenerId unique id for cmListener
     *
     * @return
     *   status               code. See #StatusCode.
     *
     * @dependencies
     *   None.
     */
    StatusCode InitializeService(in String iccId, in IImsCmServiceListener cmListener, in int userData,
         out ServiceListenerToken listenerId);

    /**
     * Adds a listener containing the list of
     * function pointers to be invoked to notify clients of the
     * various events from the connection manager.
     *
     * @param[in] connectionManager    Handle Received in OnServiceReady()
     * @param[in] cmListener           Structure with the list of callbacks.
     * @param[out] listenerId          unique id for cmListener.
     *
     * @return
     *   status                        code. See #StatusCode.
     *
     * @dependencies
     *   None.
     */
    StatusCode addListener(in long connectionManager, in IImsCmServiceListener cmListener,
        out ServiceListenerToken listenerId);

    // Adding return type to method instead of out param int status since there is only one return value.
    /**
     * Closes the connection and triggers deregistration of the associated URI.
     *
     * @param[in] connectionManager    Handle Received in OnServiceReady()
     * @param[in] connectionHandle     Handle to the IMSConnection interface object.
     *
     * @return
     *   status                        code. See #StatusCode.
     *
     * @dependencies
     *   None.
     */
    StatusCode closeConnection(in long connectionManager, in long connectionHandle);

    // Adding return type to method instead of out param int status since there is only one return value.
    /**
     * Closes the connection manager. Closing the
     * manager forces pending connection objects to be
     * immediately deleted, regardless of what state they are in.
     *
     * @param[in] connectionManager    Handle Received in OnServiceReady()
     *
     * @return
     *   status                        code. See #StatusCode.
     *
     * @dependencies
     *   None.
     */
    StatusCode closeService(in long connectionManager);

    /**
     * Creates a new IMS connection object.
     *
     * There must be a corresponding connection object for each feature tag.
     * The URI passed as part of this call is validated with the standard
     * RCS 5.1 URIs, and the connection objects are created only for supported
     * URIs. Registration for the URI is triggered after this API is invoked.
     *
     * <b>URI format for a single URI:</b>
     *
     * @indent FeatureTagName="FeatureTagValue"
     *
     *   Example
     *   @code
     *   +g.3gpp.icsi-ref="urn%3Aurn-7%3A3gpp-service.ims.icsi.oma.cpm.session" @endcode
     *   @newpage
     * <b>URI format for multiple URIs:</b>
     *
     *   @indent FeatureTagName1="FeatureTagValue1"; FeatureTagName2="FeatureTagValue2"
     *
     *   Example
     *   @code
     *   "+g.oma.sip-im";"+g.3gpp.cs-voice";"+g.3gpp.iari-ref="urn%3Aurn-7%3A3gpp-
     *   application.ims.iari.gsma-is"";"+g.3gpp.iari-ref="urn%3Aurn-7%3A3gpp-
     *   application.ims.iari.rcse.ft""" @endcode
     *
     *   @note1hang FeatureTagName and FeatureTagValue are defined in the RCS 5.1
     *   Specification.
     *
     *   Upon successful Connection Creation
     *   connectionEvent::SERVICE_CREATED is called on the
     *   IImsCMConnectionListener::onEventReceived()
     *
     * @param[in] cmConnListener          listener for the connection object**
     * @param[in] pIMSConnectionManager   Handle to the IMSConnectionManager
     *                                    interface object.
     * @param[in] uriStr                  NULL-terminated Uniform Resource Identifier (URI)
     *                                    associated with a particular application.
     * @param[out] connInfo               ConnectionInfo object with connection related info
     * 
     *
     * @dependencies
     *  None.
     */
    void createConnection(in long connectionManager, in IImsCMConnectionListener cmConnListener,
        in String uriStr,
        out ConnectionInfo connInfo);

    // Adding return type to method instead of out param int status since there is only one return value.
    /**
     * Queries the configuration structure
     * consisting of various parameters the client will be interested in
     * to form the SIP messages.Some of these configuration parameters
     * are populated once the UE is successfully registered, so
     * clients should wait for successful registration of at least one
     * of the URIs before calling this API.
     *
     * The configuration is asynchronously provided through
     * IImsCmServiceListener::onConfigurationChange()
     *
     * @param[in] connectionManager    Handle Received in OnServiceReady()
     * @param[in] configType           Configuration type.
     * @param[in] userdata             Command  user data.
     *
     * @return
     *   status                        code. See #StatusCode.
     *
     * @dependencies
     *   None.
     */
    StatusCode getConfiguration(in long connectionManager, in ConfigType configType, in int userdata);

    // Adding return type to method instead of out param int status since there is only one return value.
    /**
     * Provides support for triggering Registration Restoration, this method should be called in case
     * we receive 403 kind of permanent error for non-registration sip transactions
     * and also in case timer B/F is fired in the SIP stack used.
     * Triggers registration based on the provided method name and error codes of non-registration sip messages.
     * the success of this request is posted in onCommandStatus()
     *
     * @param[in] connectionManager    Handle Received in OnServiceReady().
     * @param[in] data                 method response data structure.
     * @param[in] userdata             Command  user data.
     *
     * @return
     *   status                        code. See #StatusCode.
     *
     * @dependencies
     *   None.
     */
    StatusCode methodResponse(in long connectionManager, in MethodResponseData data, in int userdata);

    // Adding return type to method instead of out param int status since there is only one return value.
    /**
     * Removes a listener from the IMS connection manager. Status of
     * the request is returned in QIMSCM_COMMAND_STATUS
     *
     * @param[in] connectionManager    Handle Received in OnServiceReady()
     * @param[in] listenerId           unique Id generated by addlistener()
     *                                 or InitializeService() api.
     *
     * @return
     *   status                        code. See #StatusCode.
     *
     * @dependencies
     *   None.
     */
    StatusCode removeListener(in long connectionManager, in long listenerId);

    // Adding return type to method instead of out param int status since there is only one return value.
    /**
     * Triggers the Request to Modem to generate AutoConfig configurations
     *
     * @param[in] connectionManager    Handle Received in OnServiceReady().
     * @param[in] autoConfigReasonType AutoConfig Reason type for triggering the request to modem
     * @param[in] userdata             Command  user data.
     *
     * @return
     *   status                        code. See #StatusCode.
     *
     * @dependencies
     *   None.
     */
    StatusCode triggerACSRequest(in long connectionManager,
        in AutoconfigTriggerReason autoConfigReasonType, in int userdata);

    // Adding return type to method instead of out param int status since there is only one return value.
    /**
     * Used to trigger deregistration. This method will trigger a de-registration on all
     * the feature tags. This method then does a pdn release followed by pdn bringup.
     *
     * @param[in] connectionManager    Handle Received in OnServiceReady()
     * @param[in] userdata             Command  user data.
     *
     * @return
     *    status                       code. See #StatusCode.
     *
     * @dependencies
     *    None.
     */
    StatusCode triggerDeRegistration(in long connectionManager, in int userdata);

    // Adding return type to method instead of out param int status since there is only one return value.
    /**
     * Used to trigger registration. This must be
     * done once all the connections are created so that
     * trigger registration with all the required FTs is done at one time.
     *
     * @param[in] connectionManager    Handle Received in OnServiceReady()
     * @param[in] userdata             Command  user data.
     *
     * @return
     *   status                        code. See #StatusCode.
     *
     * @dependencies
     *   None.
     */
    StatusCode triggerRegistration(in long connectionManager, in int userdata);
}
