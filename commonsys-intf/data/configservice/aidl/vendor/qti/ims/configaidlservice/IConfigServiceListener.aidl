/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.ims.configaidlservice;

import vendor.qti.ims.configaidlservice.AppType;
import vendor.qti.ims.configaidlservice.AutoConfig;
import vendor.qti.ims.configaidlservice.AutoConfigResponse;
import vendor.qti.ims.configaidlservice.RequestStatus;
import vendor.qti.ims.configaidlservice.SettingsData;
import vendor.qti.ims.configaidlservice.TokenRequestReason;
import vendor.qti.ims.configaidlservice.TokenType;
import vendor.qti.ims.configaidlservice.UceCapabilityInfo;
import vendor.qti.ims.configaidlservice.RequestStatus;
import vendor.qti.ims.configaidlservice.SettingsData;

@VintfStability
interface IConfigServiceListener {
    /**
     * Unsolicited indication to client when IConfigService receives an error
     * response for Auto Config request to server that requires additional
     * handling from the client.
     *
     * @param acsResponse sip response of the ACS request
     *
     * @return
     *   None.
     *
     * @dependencies
     *   None.
     */
    oneway void onAutoConfigErrorSipResponse(in AutoConfigResponse acsResponse);

    /**
     * Callback function to notify clients of the Autoconfiguration xml
     * after successful negotiation between UE and network. It would be
     * triggered when client requests getAcsConfiguration() or when
     * there is a change in autoconfiguration xml on modem which would
     * be notified by this listener.
     *
     * @param acsConfig config data structure having autoconfiguration xml
     *
     * @return
     *   None.
     *
     * @dependencies
     *   None.
     */
    oneway void onAutoConfigurationReceived(in AutoConfig acsConfig);

    /**
     * Callback function to notify the clients the status of request placed.
     *
     * @param status     status of set requests as per RequestStatus.
     *        userData   user data/request ID corresponding to client request.
     */
    oneway void onCommandStatus(in RequestStatus status, in int userData);

    /**
     * Callback triggered upon getting result of getSettingsValue()
     *
     * @param   status    RequestStatus to indicate whether get request to
     *                    modem was success or not
     *          cbdata    struct corresponding to SettingsData, containing the
     *                    SettingsId requested and the corresponding values from modem
     *                    as key-value pair vector, if status is RequestStatus::OK.
     *          userData  user data/request ID corresponding to the client request.
     */
    oneway void onGetSettingsResponse(in RequestStatus status, in SettingsData cbdata,
        in int userData);

    /**
     * Unsolicited indication received from modem to notify HAL client if value of
     * SettingsId has been updated on modem
     *
     *
     * @param   cbdata    struct corresponding to SettingsData, containing the SettingsId
     *                    for which callback is registered and the corresponding values from modem
     *                    as key-value pair vector.
     */
    oneway void onGetUpdatedSettings(in SettingsData cbdata);

    /**
     * Callback triggered notifying the RCS capabilities on modem, triggered upon
     * getting result of getRcsServiceStatus()
     */
    oneway void onRcsServiceStatusUpdate(in boolean isRcsEnabled);

    /**
     * Callback triggered due to service error so reconfiguration is required from the client.
     * On getting this event the client needs to invoke the setConfig API to send
     * configuration xml to modem again.
     */
    oneway void onReconfigNeeded();

    /**
     * Indication triggered when token refresh is requested due to given reasons.
     * On getting this event, client needs to trigger token request and invoke the
     * updateTokenFetchStatus API to update the status of the request where there is
     * a change in it.
     *
     * @param   requestId   unique Id for the request triggered
     * @param   tokenType   type of token as defined in TokenType enum
     * @param   reqReason   reason of the token fetch request as defined in TokenRequestReason enum
     */
    oneway void onTokenFetchRequest(in int requestId, in TokenType tokenType,
        in TokenRequestReason reqReason);

    /**
     * Callback triggered notifying the UCE capabilities on modem, triggered upon
     * getting result of getUceStatus()
     */
    oneway void onUceStatusUpdate(in UceCapabilityInfo capinfo);

    /**
     * Callback to getUserAgent()
     *
     * @param   userAgent   user agent string constructed by modem UAM
     * @param   appType     app type (SMS or RCS) for which user agent string
     *                      was requested
     */
    oneway void onUserAgentReceived(in String userAgent, in AppType appType);
}
