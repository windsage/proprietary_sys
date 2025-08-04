/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.radio.ims;

import vendor.qti.hardware.radio.ims.CallDetails;
import vendor.qti.hardware.radio.ims.CallModifyFailCause;

/**
 * CallModifyInfo is used to upgrade/downgrade the call.
 * Lower layers will process CallModifyInfo only if callIndex is > 0.
 */
@VintfStability
parcelable CallModifyInfo {
    int callIndex;
    CallDetails callDetails;
    /* CallModifyFailCause is not used for outgoing requests.
     * Default value is CallModifyFailCause.E_INVALID.
     */
    CallModifyFailCause failCause = CallModifyFailCause.E_INVALID;
}
