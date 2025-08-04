/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.ims.configaidlservice;

/*
 * Default SMS app type
 */
@VintfStability
@Backing(type="int")
enum DefaultSMSApp {
    /**
     * No default
     */
    DEFAULT_NONE = 0,
    /**
     * Use RCS as default SMS app
     */
    DEFAULT_RCS = 1,
    /**
     * Use SMS as default SMS app
     */
    DEFAULT_SMS = 2,
    /**
     * MAX APPTYPE
     */
    APPTYPE_MAX = 3,

}
