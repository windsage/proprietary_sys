/**
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.hardware.wifi.wifimyftm;

import vendor.qti.hardware.wifi.wifimyftm.MyFtmCmdStatus;

@VintfStability
parcelable MyFtmStatus {
   /**
    * Refer enum MyFtmCmdStatus for more info on the status codes.
    */
    MyFtmCmdStatus cmdStatus;
   /**
    * This output string informs the client the output response/error from
    * myftm.
    */
    String outputString;
}
