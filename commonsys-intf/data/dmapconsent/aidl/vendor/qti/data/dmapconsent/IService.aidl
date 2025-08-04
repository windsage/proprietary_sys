/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.data.dmapconsent;

import vendor.qti.data.dmapconsent.ConsentType;
import vendor.qti.data.dmapconsent.ConsentOptin;
import vendor.qti.data.dmapconsent.ConsentLogging;

import vendor.qti.data.dmapconsent.IServiceInd;
import vendor.qti.data.dmapconsent.IStatusCb;

@VintfStability
interface IService {

    /**
     * Registers for service status indications.  Must be called before init.
     *
     * @param[in] ind,           Indication method to which service updates will be sent. The
     *                           result of the registration request will also be sent to this
     *                           method.
     *
     * @return                   None.
     */
    void registerForServiceInd(IServiceInd ind);

    /**
     * Initializes a session with a tag and verifies the license. Must be called after
     * registerForServiceInd and before initAppIdentifier is called.
     * If the init_v2 API is used, this API cannot be used.
     *
     * @param[in] tag,           Tag string.
     * @param[in] apnName,       APN name.
     * @param[in] apnType,       APN type.
     * @param[in] cb,            Callback method to which the result of the API call will be sent.
     *
     * @return transactionId,    ID of the transaction that identifies the result of the API call.
     *                           A return value of zero indicates the sanity of the input failed,
     *                           and a callback should not be expected. The given cb will be called
     *                           with this ID and the result of the API call if the transactionId
     *                           is not zero.
     */
    long init(String tag, String apnName, String apnType, IStatusCb cb);

    /**
     * Initializes a session with an application identifier. Must be called before calling update.
     *
     * @param[in] appId,         appId string (null-terminated).
     * @param[in] cb,            Callback method to which the result of the API call will be sent.
     *
     * @return transactionId,    ID of the transaction that identifies the result of the API call.
     *                           A return value of zero indicates the sanity of the input failed,
     *                           and a callback should not be expected. The given cb will be called
     *                           with this ID and the result of the API call if the transactionId
     *                           is not zero.
     */
    long initAppIdentifier(String appId, IStatusCb cb);

    /**
     * Updates the session tag.  Must be called after initAppIdentifier.
     *
     * @param[in] tag,           Tag string.
     * @param[in] cb,            Callback method to which the result of the API call will be sent.
     *
     * @return transactionId,    ID of the transaction that identifies the result of the API call.
     *                           A return value of zero indicates the sanity of the input failed,
     *                           and a callback should not be expected. The given cb will be called
     *                           with this ID and the result of the API call if the transactionId
     *                           is not zero.
     */
    long updateTag(String tag, IStatusCb cb);

    /**
     * Updates fine, coarse, or general consent. Can only be called after initAppIdentifier has
     * been called.
     *
     * @param[in] type,          Consent type (CONSENT_FINE_LOCATION, CONSENT_COARSE_LOCATION,
     *                           CONSENT_GENERAL).
     * @param[in] optin,         Opt in type (CONSENT_PERSISTENT_OPT_IN, CONSENT_OPT_OUT).
     * @param[in] cb,            Callback method to which the result of the API call will be sent.
     *
     * @return transactionId,    ID of the transaction that identifies the result of the API call.
     *                           A return value of zero indicates the sanity of the input failed,
     *                           and a callback should not be expected. The given cb will be called
     *                           with this ID and the result of the API call if the transactionId
     *                           is not zero.
     */
    long updateConsent(ConsentType type, ConsentOptin optin, IStatusCb cb);

    /**
     * Sets the logging level.
     *
     * @param[in] logging,       Logging level (DISABLE, LOCAL, REMOTE)
     * @param[in] cb,            Callback method to which the result of the API call will be sent.
     *
     * @return transactionId,    ID of the transaction that identifies the result of the API call.
     *                           A return value of zero indicates the sanity of the input failed,
     *                           and a callback should not be expected. The given cb will be called
     *                           with this ID and the result of the API call if the transactionId
     *                           is not zero.
     */
    long enableLogging(ConsentLogging logging, IStatusCb cb);

    /**
     * Deinitializes the existing session. After calling no service status updates will be sent.
     *
     * @param[in]                None.
     *
     * @return                   None.
     */
    void deregister();

    /**
     * Initializes a session with a tag and verifies the license. Must be called after
     * registerForServiceInd and before initAppIdentifier is called.
     * This API allows the clients to pass a unique token string identifier.
     * If the init API is used, this API cannot be used.
     *
     * @param[in] tag,           Tag string.
     * @param[in] apnName,       APN name.
     * @param[in] apnType,       APN type.
     * @param[in] token,         Token string.
     * @param[in] cb,            Callback method to which the result of the API call will be sent.
     *
     * @return transactionId,    ID of the transaction that identifies the result of the API call.
     *                           A return value of zero indicates the sanity of the input failed,
     *                           and a callback should not be expected. The given cb will be called
     *                           with this ID and the result of the API call if the transactionId
     *                           is not zero.
     */
    long init_v2(String tag, String apnName, String apnType, String token, IStatusCb cb);

    /**
     * Update the APN details.
     *
     * @param[in] apnName,       APN name.
     * @param[in] apnType,       APN type.
     * @param[in] cb,            Callback method to which the result of the API call will be sent.
     *
     * @return transactionId,    ID of the transaction that identifies the result of the API call.
     *                           A return value of zero indicates the sanity of the input failed,
     *                           and a callback should not be expected. The given cb will be called
     *                           with this ID and the result of the API call if the transactionId
     *                           is not zero.
     */
    long updateApn(String apnName, String apnType, IStatusCb cb);
}
