/*====================================================================
*  Copyright (c) 2024  Qualcomm Technologies, Inc.
*  All Rights Reserved.
*  Confidential and Proprietary - Qualcomm Technologies, Inc.
*====================================================================
*/
package vendor.qti.hardware.capabilityconfigstore;

import vendor.qti.hardware.capabilityconfigstore.CommandResult;

@VintfStability
interface ICapabilityConfigStore {
    // Adding return type to method instead of out param CommandResult result since there is only one return value.
    /**
     * query the value via area  and key which is any string
     * @param area, key
     * @param out value: return value as string, return type SUCESS if value is returned else NOT_FOUND
     */
    CommandResult getConfig(in String area, in String key);
}
