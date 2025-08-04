/*====================================================================
*  Copyright (c) 2024  Qualcomm Technologies, Inc.
*  All Rights Reserved.
*  Confidential and Proprietary - Qualcomm Technologies, Inc.
*====================================================================
*/

package vendor.qti.hardware.capabilityconfigstore;

import vendor.qti.hardware.capabilityconfigstore.Result;

@VintfStability
parcelable CommandResult {
    Result result_type;
    String value;
}
