/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.ims.configaidlservice;

import vendor.qti.ims.configaidlservice.AutoConfigTriggerReason;
import vendor.qti.ims.configaidlservice.ConfigData;
import vendor.qti.ims.configaidlservice.SettingsData;
import vendor.qti.ims.configaidlservice.SettingsId;
import vendor.qti.ims.configaidlservice.StatusCode;
import vendor.qti.ims.configaidlservice.TokenType;
import vendor.qti.ims.configaidlservice.UceCapabilityInfo;
import vendor.qti.ims.configaidlservice.SettingsData;
import vendor.qti.ims.configaidlservice.RcsStatus;
import vendor.qti.ims.configaidlservice.AppType;

@VintfStability
interface IConfigService {
    /**
     * Deregisters for callback with modem for any settings value update on modem
     * previously registered for using registerForSettingsChange().
     *
     * @param   userData           user data/request ID.
     *
     * @return  status             status returned as per RequestStatus
     *
     * The response status for request is provided through
     * IConfigServiceListener::onCommandStatus()
     */
    int deregisterForSettingsChange(in int userData);

    /**
     * Queries the auto configuration xml structure from modem if its available.
     * The configuration would be provided through
     * IConfigServiceListener::onAutoConfigurationReceived()
     *
     * @param   userData           user data/request ID.
     *
     * @return
     *          status             status return as per RequestStatus.
     *
     * The response status for request is provided through
     * IConfigServiceListener::onCommandStatus()
     *
     */
    int getAcsConfiguration(in int userData);

    /**
     * Queries the modem to retrieve the Rcs Capabilities set on modem.
     * If modem is up and status of RCSService capabilities are available then
     * rcsStatus would carry the rcsenabled status and status would be returned
     *  as SUCCESS else status can be returned as FAILED or IN_PROGRESS.
     *
     * @param out  rcsStatus       rcsStatus notifying rcsenabled status.
     *
     * @return     status          status code as per RequestStatus
     */

    int getRcsServiceStatus(out RcsStatus rcsStatus);

    /**
     * Queries the modem to retrieve value of all the properties set on modem
     * of a particular type.
     *
     * @param   settingsId         integer value representing the type of SettingsId
     *                             to query
     *          userData           user data/request ID.
     * @return
     *          status             status code as per RequestStatus
     *
     * The response status for request is provided through
     * IConfigServiceListener::onGetSettingsResponse()
     */
    int getSettingsValue(in SettingsId settingsId, in int userData);

    /**
     * Queries the modem to retrieve the Uce Capabilities set on modem.
     * If modem is up and status of UCE capabilities are available then uceCapInfo
     * would carry the capability status and status would be returned as SUCCESS
     * else status can be returned as FAILED or IN_PROGRESS.
     *
     * @param out  uceCapInfo         capinfo notifying the UCE capabilities on modem
     *
     * @return     status             status returned as per RequestStatus
     */
    int getUceStatus(out UceCapabilityInfo uceCapInfo);

    /**
     * Registers for callback with modem to listen for any settings value update on
     * modem. Any change in settings value from modem would be provided by
     * IConfigServiceListener::onGetUpdatedSettings callback.
     *
     * @param   userData           user data/request ID.
     *
     * @return  status             status of the register request as per RequestStatus
     *
     * The response status for request is provided through
     * IConfigServiceListener::onCommandStatus()
     */
    int registerForSettingsChange(in int userData);

    /**
     * Updates token when it is available after fetch request is successful.
     * It can be updated when requested by onTokenFetchRequest callback or
     * whenever token is refreshed by client.
     *
     * @param   token         plain-Text app-token, must be base-64 encoded
     *          userData      user data/request ID.
     *
     * @return  status        status returned as per RequestStatus
     * The response status for request is provided through
     * IConfigServiceListener::onCommandStatus()
     */
    int setAppToken(in String token, in int userData);

    // Adding return type to method instead of out param int status since there is only one return value.
    /**
     * Sends RCS configuration data to modem
     *
     * @param   configData    the new configuration in ConfigData container
     *          userData      user data/request ID.
     *
     * @return
     *          status        status code as per RequestStatus
     *
     * The response status for request is provided through
     * IConfigServiceListener::onCommandStatus()
     */
    int setConfig(in ConfigData configData, in int userData);

    /**
     * Sends RCS settings data to modem
     *
     * @param   settingsData       struct corresponding to SettingsData, containing type of
     *                             SettingsId and key value pair for specifying individual
     *                             SettingsKeys and their values.
     *          userData           user data/request ID.
     * @return
     *          status             status code as per RequestStatus
     *
     * The response status for request is provided through
     * IConfigServiceListener::onCommandStatus()
     */
    int setSettingsValue(in SettingsData settingsData, in int userData);

    /**
     * Triggers the Request to Modem to generate AutoConfig configurations
     *
     * @param autoConfigReasonType          AutoConfig Reason type for triggering the request to modem
     *        userData                      user data/request ID.
     * @return
     *        status                        status code  as per RequestStatus.
     *
     * The response status for request is provided through
     * IConfigServiceListener::onCommandStatus()
     */
    int triggerAcsRequest(in AutoConfigTriggerReason autoConfigReasonType, in int userData);

    /**
     * Update the status of token fetch request as triggered by the client
     * as and when status changes
     *
     * @param   requestId   unique Id received as part of onTokenFetchRequest callback
     * @param   tokenType   type of token as defined in TokenType enum
     * @param   status      status of the token fetch request as defined in StatusCode enum
     * @param   userData    user data/request ID.
     *
     * @return  status      status returned as per #RequestStatus
     *
     * The response status for request is provided through
     * IConfigServiceListener::onCommandStatus()
     *
     */
    int updateTokenFetchStatus(in int requestId, in TokenType tokenType, in StatusCode status,
        in int userData);

    /**
     * Fetch UserAgent from modem User Agent Manager for given app type
     *
     * @param   appType     app type used by modem to construct appropriate user agent string
     * @param   userData    user data/request ID.
     *
     * @return  status      status code as per RequestStatus
     *
     * The user agent string response is provided through
     * IConfigServiceListener::onUserAgentReceived()
     */
    int getUserAgent(in AppType appType);

    /**
     * Sends SMS version to modem
     *
     * @param   version     SMS version string requested by modem to construct user agent strings
     * @param   userData    user data/request ID.
     *
     * @return  status      status code as per RequestStatus
     */
    int setSmsVersion(in String version);
}
