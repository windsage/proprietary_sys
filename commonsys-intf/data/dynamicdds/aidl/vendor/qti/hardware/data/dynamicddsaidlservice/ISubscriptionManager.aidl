/*===========================================================================

  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.

===========================================================================*/

package vendor.qti.hardware.data.dynamicddsaidlservice;

import vendor.qti.hardware.data.dynamicddsaidlservice.IDddsCallback;
import vendor.qti.hardware.data.dynamicddsaidlservice.IGetAppPreferencesCallback;
import vendor.qti.hardware.data.dynamicddsaidlservice.ISetAppPreferencesCallback;
import vendor.qti.hardware.data.dynamicddsaidlservice.StatusCode;
import vendor.qti.hardware.data.dynamicddsaidlservice.SubscriptionConfig;

/**
 * IDynamicSubscriptionManager is an interface used for managing dynamic
 * designated data subscription switch functionality and managing application
 * preference upon which designated data subscription switch decision is made.
 */
@VintfStability
interface ISubscriptionManager {
    /**
     * Clear application preference
     *
     * @param out  status     OK if successful, other StatusCode otherwise
     */
    StatusCode clearAppPreferences();

    /**
     * Get current application preference.
     *
     * @param subConfig Subscription configuration
     * @param   cb         IGetAppPreferencesCallback to listen to result of
     *                     getAppPreferences
     * @param out  status     OK if successful, NOT_SUPPORTED if dynamic designated
     *                     data subscription switch function is not enabled,
     *
     */
    StatusCode getAppPreferences(in IGetAppPreferencesCallback cb);

    /**
     * Register IDynamicDdsCallback to monitor Dynamic Subscription
     * change feature status and designated data subscription status
     *
     * @param   cb IDddsCallback
     * @param out  status   OK if successful
     *                   other StatusCode otherwise
     */
    StatusCode registerForDynamicSubChanges(in IDddsCallback cb);

    /**
     * Set application preference. New preference
     * will override previous effective preference if successful and previous
     * preference remain unchanged if failed. Successfully set preference is
     * presist.
     *
     * @param   preference subscription preference
     * @param   cb         ISetAppPreferencesCallback to listen to result of
     *                     setAppPreferences
     * @param out  status     OK if successful, NOT_SUPPORT if dynamic designated
     *                     data subscription switch function is not enabled,
     *                     REQUEST_IN_PROCESS if setAppPreferences or getAppPreferences
     *                     has not received corresponding callback.
     */
    StatusCode setAppPreferences(in SubscriptionConfig[] preference,
        in ISetAppPreferencesCallback cb);

    /**
     * Enable or Disable Dynamic DDS Switch Feature.
     * Dynamic DDS Feature needs to be enabled for any application
     * preference changes.
     *
     * @param   enable   true enables dynamic DDS Switch Feature
     *                   and false disables dynamic DDS Switch Feature
     * @param out  status   OK if successful
     *                   other StatusCode otherwise
     */
    StatusCode setDynamicSubscriptionChange(in boolean enable);
}
