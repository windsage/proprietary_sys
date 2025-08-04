/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.ims.configaidlservice;

/*
 * User Agent app types
 */
@VintfStability
@Backing(type="int")
enum AppType {
    /**
     * Default APPTYPE
     */
    APPTYPE_DEFAULT = 0,
    /**
     * SMS APPTYPE to SMS specific UA
     */
    APPTYPE_SMS = 1,
    /**
     * RCS APPTYPE to RCS specific UA
     */
    APPTYPE_RCS = 2,
    /**
     * MAX APPTYPE
     */
    APPTYPE_MAX = 3,

}
