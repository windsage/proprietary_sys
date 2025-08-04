/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/
package vendor.qti.ims.imscmaidlservice;

import vendor.qti.ims.imscmaidlservice.AutoConfig;
import vendor.qti.ims.imscmaidlservice.DeviceConfig;
import vendor.qti.ims.imscmaidlservice.UserConfig;

/*
 * Configuration Data Type
 * object used in onConfigurationChange()
 * callback
 */
@VintfStability
parcelable ConfigData {
    UserConfig userConfigData;
    DeviceConfig deviceConfigData;
    AutoConfig autoConfigData;
}
