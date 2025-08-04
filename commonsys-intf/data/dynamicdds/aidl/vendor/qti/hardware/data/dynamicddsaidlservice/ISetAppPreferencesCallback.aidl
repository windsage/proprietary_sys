/*===========================================================================

  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.

===========================================================================*/

package vendor.qti.hardware.data.dynamicddsaidlservice;

import vendor.qti.hardware.data.dynamicddsaidlservice.StatusCode;

/**
 * Callback providing result of setAppPreferences
 */
@VintfStability
interface ISetAppPreferencesCallback {
    /**
     * Called when setAppPreferences has result
     * @param   status       OK if application preferences are got succcessfully,
     *                       other StatusCode otherwise
     * @param   reason       detailed reason if failed.
     */
    oneway void onResult(in StatusCode status, in String reason);
}
