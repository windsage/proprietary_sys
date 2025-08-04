/*===========================================================================

  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.

===========================================================================*/

package vendor.qti.hardware.data.dynamicddsaidlservice;

import vendor.qti.hardware.data.dynamicddsaidlservice.StatusCode;
import vendor.qti.hardware.data.dynamicddsaidlservice.SubscriptionConfig;

/**
 * Callback providing result of getAppPreferences
 */
@VintfStability
interface IGetAppPreferencesCallback {
    /**
     * Called when getAppPreferences has result
     * @param   status       OK if application preferences are got succcessfully,
     *                       other StatusCode otherwise
     * @param   preferences   application preferences if successful
     */
    oneway void onResult(in StatusCode status, in SubscriptionConfig[] preferences);
}
