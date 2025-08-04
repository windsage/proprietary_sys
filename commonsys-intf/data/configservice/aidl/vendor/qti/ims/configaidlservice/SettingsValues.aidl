/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.ims.configaidlservice;

import vendor.qti.ims.configaidlservice.KeyValuePairTypeBool;
import vendor.qti.ims.configaidlservice.KeyValuePairTypeInt;
import vendor.qti.ims.configaidlservice.KeyValuePairTypeString;

@VintfStability
parcelable SettingsValues {
    KeyValuePairTypeBool[] boolData;
    KeyValuePairTypeInt[] intData;
    KeyValuePairTypeString[] stringData;
}
