/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.ims.configaidlservice;

import vendor.qti.ims.configaidlservice.ConfigData;

/*
 * AutoConfig Data Type
 */
@VintfStability
parcelable AutoConfig {
    /*
     * AutoConfigRequestType
     */
    int autoConfigRequestType;
    /*
     * configXml sent with compressed status
     */
    ConfigData configData;
}
