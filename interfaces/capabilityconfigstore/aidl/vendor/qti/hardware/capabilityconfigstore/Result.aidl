/*====================================================================
*  Copyright (c) 2024  Qualcomm Technologies, Inc.
*  All Rights Reserved.
*  Confidential and Proprietary - Qualcomm Technologies, Inc.
*====================================================================
*/
package vendor.qti.hardware.capabilityconfigstore;

/*
 * Type enumerating  various  result  codes returned from ICapabilityConfigStore method.
 */
@VintfStability
@Backing(type="int")
enum Result {
    SUCCESS = 0,
    NOT_FOUND = -1,
}
