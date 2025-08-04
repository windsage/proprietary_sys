/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.ims.configaidlservice;

import vendor.qti.ims.configaidlservice.SettingsId;
import vendor.qti.ims.configaidlservice.SettingsValues;

@VintfStability
parcelable SettingsData {
    SettingsId settingsId;
    SettingsValues settingsValues;
}
