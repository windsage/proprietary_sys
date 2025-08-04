/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.ims.callcapabilityaidlservice;

import vendor.qti.ims.callcapabilityaidlservice.KeyValuePairBoolType;
import vendor.qti.ims.callcapabilityaidlservice.KeyValuePairStringType;

@VintfStability
parcelable CallCapabilityInfo {
    String contactNumber;
    KeyValuePairBoolType[] boolData;
    KeyValuePairStringType[] stringData;
}
